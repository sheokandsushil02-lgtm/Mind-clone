#!/usr/bin/env python3
"""MAD patch v7 — extractor + patches + self-diagnostics.

v7 fix (asli bug):
  v6 me pehli khoj par 'app' wala folder mil jata tha (weak match) aur
  nested ZIP kabhi nahi khulti thi. Ab:
    1. Sabse pehle SAARI nested ZIPs recursively khulti hain (depth tak 5)
    2. Tab project root dhoondha jata hai — strong (settings.gradle.kts +
       app manifest) ko tarjeeh, warna weak
  - Poora patch report GitHub ISSUE me post hota hai
  - debug.keystore khud keytool se generate
  - local.properties me sdk.dir
  - ~/.gradle/init.gradle hook — gradle build result bhi issue me
"""
import glob
import io
import json
import os
import shutil
import subprocess
import sys
import tempfile
import traceback
import zipfile

ROOT = sys.argv[1] if len(sys.argv) > 1 else "app-merged"
ZIP_NAME = "madgitfileforcloud.zip"
RAW_URL = "https://github.com/sheokandsushil02-lgtm/Mind-clone/raw/main/" + ZIP_NAME
OVERLAY_MAD = os.path.join("overlay", "app", "src", "main", "java", "com", "example", "mad")
MANIFEST = os.path.join(ROOT, "app", "src", "main", "AndroidManifest.xml")
SUMMARY_LINES = []
ISSUE_NUM_FILE = "/tmp/mad_issue_number.txt"


def log(msg):
    line = "MAD-PATCH: " + str(msg)
    print(line)
    SUMMARY_LINES.append(line)


def flush_summary():
    path = os.environ.get("GITHUB_STEP_SUMMARY")
    if path:
        try:
            with io.open(path, "a", encoding="utf-8") as f:
                f.write("\n## MAD Patch Report\n```\n" + "\n".join(SUMMARY_LINES) + "\n```\n")
        except Exception:
            pass


def gh_api(method, path, payload):
    token = os.environ.get("GITHUB_TOKEN", "")
    repo = os.environ.get("GITHUB_REPOSITORY", "")
    if not token or not repo:
        return None
    cmd = ["curl", "-s", "-X", method,
           "-H", "Authorization: Bearer " + token,
           "-H", "Accept: application/vnd.github+json",
           "-d", json.dumps(payload),
           "https://api.github.com/repos/" + repo + path]
    try:
        r = subprocess.run(cmd, capture_output=True, text=True, timeout=90)
        return json.loads(r.stdout or "{}")
    except Exception:
        return None


def post_patch_report():
    run_id = os.environ.get("GITHUB_RUN_ID", "local")
    title = "MAD Build Diagnostics (run " + run_id + ")"
    body = "Auto-generated patch report — assistant isko padh kar fix karega.\n\n```\n" + \
           "\n".join(SUMMARY_LINES) + "\n```\n"
    if len(body) > 12000:
        body = body[:12000] + "\n... (truncated)\n"
    res = gh_api("POST", "/issues", {"title": title, "body": body})
    if res and res.get("number"):
        log("Diagnostics issue ban gaya #" + str(res["number"]))
        try:
            io.open(ISSUE_NUM_FILE, "w").write(str(res["number"]))
        except Exception:
            pass
        return res["number"]
    if res is None:
        log("Diagnostics issue POST: koi response nahi (token/env missing?)")
    else:
        log("Diagnostics issue POST fail: " + json.dumps(res)[:300])
    return None


SKIP_DIRS = {"build", ".gradle", ".idea", "intermediates", "__pycache__", "node_modules"}


def find_project_root(base):
    strong, weak = [], []
    for dirpath, dirnames, filenames in os.walk(base):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        has_manifest = os.path.isfile(os.path.join(dirpath, "app", "src", "main", "AndroidManifest.xml"))
        has_settings = ("settings.gradle.kts" in filenames) or ("settings.gradle" in filenames)
        if has_settings and has_manifest:
            strong.append(dirpath)
        elif has_manifest:
            weak.append(dirpath)
    pool = strong or weak
    if not pool:
        return None
    root = min(pool, key=lambda p: (p.count(os.sep), len(p)))
    log("root dhoondha: " + os.path.relpath(root, base) + " | strong=" + str(bool(strong)))
    return root


def unzip_to(zip_path, dst):
    os.makedirs(dst, exist_ok=True)
    with zipfile.ZipFile(zip_path) as z:
        z.extractall(dst)


def ensure_zip():
    if os.path.isfile(ZIP_NAME) and os.path.getsize(ZIP_NAME) > 100000:
        log("ZIP maujood hai (" + str(os.path.getsize(ZIP_NAME)) + " bytes)")
        return True
    try:
        r = subprocess.run(["git", "show", "origin/main:" + ZIP_NAME],
                            stdout=open(ZIP_NAME, "wb"), stderr=subprocess.PIPE, timeout=300)
        if r.returncode == 0 and os.path.isfile(ZIP_NAME) and os.path.getsize(ZIP_NAME) > 100000:
            log("git se ZIP aa gayi")
            return True
    except Exception:
        traceback.print_exc()
    try:
        r = subprocess.run(["curl", "-sSL", "--retry", "3", "-o", ZIP_NAME, RAW_URL],
                            stderr=subprocess.PIPE, timeout=600)
        if r.returncode == 0 and os.path.isfile(ZIP_NAME) and os.path.getsize(ZIP_NAME) > 100000:
            log("raw URL se ZIP aa gayi")
            return True
    except Exception:
        traceback.print_exc()
    return False


def extract_nested_zips(outer, max_depth=5):
    """SAARI nested ZIPs pehle hi khulwa do — phir root dhoondo."""
    for depth in range(1, max_depth + 1):
        inner_zips = []
        for dirpath, dirnames, filenames in os.walk(outer):
            dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
            for fn in filenames:
                if fn.lower().endswith(".zip"):
                    inner_zips.append(os.path.join(dirpath, fn))
        if not inner_zips:
            if depth == 1:
                log("koi nested ZIP nahi mili")
            return
        log("nested ZIPs depth " + str(depth) + ": " + str(len(inner_zips)))
        for iz in inner_zips:
            try:
                dstx = os.path.join(os.path.dirname(iz), os.path.basename(iz)[:-4] + "_unzipped")
                unzip_to(iz, dstx)
                os.remove(iz)
                log("nested ZIP khola: " + os.path.basename(iz))
            except Exception:
                log("nested ZIP nahi khuli: " + os.path.basename(iz))


def extract_real_project(dst):
    if not ensure_zip():
        log("ZIP kisi tarah nahi mili")
        return False
    tmp = tempfile.mkdtemp(prefix="madzip_")
    try:
        try:
            with zipfile.ZipFile(ZIP_NAME) as z:
                names = z.namelist()
            log("ZIP me kul " + str(len(names)) + " entries. Pehli 40:")
            for n in names[:40]:
                log("  Z " + n)
        except Exception:
            log("ZIP padhne me dikkat:")
            traceback.print_exc()
            return False
        outer = os.path.join(tmp, "outer")
        unzip_to(ZIP_NAME, outer)
        log("ZIP extract ho gayi")
        # v7: pehle SAARI nested zips kholein
        extract_nested_zips(outer)
        proj = find_project_root(outer)
        if not proj:
            log("PROJECT ROOT NAHI MILA — structure dump:")
            count = 0
            for dirpath, dirnames, filenames in os.walk(outer):
                dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
                rel = os.path.relpath(dirpath, outer)
                log("  D " + rel + " :: " + ", ".join(sorted(filenames)[:12]))
                count += 1
                if count > 60:
                    log("  ... (aur bhi hain)")
                    break
            return False
        log("asli project: " + os.path.relpath(proj, outer))
        ignore = shutil.ignore_patterns("build", ".gradle", ".idea", "*.jks", "*.keystore")
        shutil.copytree(proj, dst, dirs_exist_ok=True, ignore=ignore)
        log("project merge ho gaya app-merged me")
        log("merge ke baad app-merged top: " + ", ".join(sorted(os.listdir(dst))[:25]))
        return True
    except Exception:
        log("extract me dikkat:")
        traceback.print_exc()
        return False
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


# ---------- 0) project nikalo agar app-merged khali/adhura hai ----------
if not os.path.isfile(MANIFEST):
    log("app-merged me manifest nahi — asli project ZIP se nikal raha hoon")
    extract_real_project(ROOT)
else:
    log("app-merged me project maujood hai")

if os.path.isdir(ROOT):
    log("app-merged top-level: " + ", ".join(sorted(os.listdir(ROOT))[:25]))
else:
    log("FATAL: app-merged directory hi nahi hai")

for rel in ["settings.gradle.kts", "app/build.gradle.kts", "gradle.properties",
            "gradle/libs.versions.toml", ".env.example", "app/src/main/AndroidManifest.xml"]:
    state = "HAI  " if os.path.isfile(os.path.join(ROOT, rel)) else "NAHI "
    log(state + rel)

# ---------- 1) MAD overlay .kt files ----------
try:
    if os.path.isdir(OVERLAY_MAD):
        target_mad = os.path.join(ROOT, "app", "src", "main", "java", "com", "example", "mad")
        os.makedirs(target_mad, exist_ok=True)
        n = 0
        for f in glob.glob(os.path.join(OVERLAY_MAD, "*.kt")):
            shutil.copy2(f, target_mad)
            n += 1
        log("MAD .kt files copy: " + str(n))
except Exception:
    traceback.print_exc()

# ---------- 2) HomeScreen MAD button ----------
target = None
cands = glob.glob(os.path.join(ROOT, "app/src/main/java/**/HomeScreen.kt"), recursive=True)
for c in cands:
    if "ui/screens" in c.replace(os.sep, "/"):
        target = c
        break
if not target and cands:
    target = cands[0]

if not target:
    log("FATAL-INFO: HomeScreen.kt nahi mila — MAD button add nahi hua")
else:
    log("HomeScreen target = " + target)
    try:
        src = io.open(target, encoding="utf-8", errors="replace").read()
        if "hub_mad_button" in src:
            log("MAD button already present")
        else:
            anchor = "import com.example.utils.ShareAndPrintUtils"
            if anchor in src and "import com.example.mad.MadLauncher" not in src:
                src = src.replace(anchor, anchor + "\nimport com.example.mad.MadLauncher", 1)
                log("MadLauncher import add hua")
            elif "import com.example.mad.MadLauncher" not in src:
                pkg_end = src.find("\n", src.find("package "))
                if pkg_end != -1:
                    src = src[:pkg_end + 1] + "\nimport com.example.mad.MadLauncher\n" + src[pkg_end + 1:]
                    log("MadLauncher import add hua (fallback)")

            button = '''        Spacer(modifier = Modifier.height(16.dp))

        // === MAD AI BUTTON (naya feature overlay) ===
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hub_mad_button")
                .clickable { MadLauncher.open(context) },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = EditorialLilac),
            border = androidx.compose.foundation.BorderStroke(1.dp, EditorialBorderLight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(EditorialTextOnLilac),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "\U0001F399", fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MAD \u2014 Live AI Assistant",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = EditorialTextOnLilac,
                            fontSize = 16.sp
                        )
                    )
                    Text(
                        text = "Bol ke baat karo \u2014 Gemini live chat",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EditorialTextOnLilac.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open MAD",
                    tint = EditorialTextOnLilac,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
'''
            markers = [
                "        // Quick Utility Actions",
                "        Spacer(modifier = Modifier.height(20.dp))",
                "REMS \u2022 Rajat Electro-Mech Solutions",
            ]
            inserted = False
            for m in markers:
                idx = src.find(m)
                if idx == -1:
                    continue
                line_start = src.rfind("\n", 0, idx) + 1
                src = src[:line_start] + button + "\n" + src[line_start:]
                inserted = True
                log("button insert ho gaya marker se: " + m.strip()[:40])
                break
            if not inserted:
                log("WARNING: koi marker match nahi hua — button add NAHI hua")
            else:
                io.open(target, "w", encoding="utf-8").write(src)
                log("HomeScreen patch save ho gaya")
    except Exception:
        log("HomeScreen patch me dikkat:")
        traceback.print_exc()

# ---------- 3) debug.keystore generate ----------
ks = os.path.join(ROOT, "debug.keystore")
bld = os.path.join(ROOT, "app", "build.gradle.kts")
if os.path.isfile(bld) and not os.path.isfile(ks):
    kt = shutil.which("keytool")
    ok = False
    if kt:
        try:
            subprocess.run([kt, "-genkeypair", "-keystore", ks, "-storepass", "android",
                            "-alias", "androiddebugkey", "-keypass", "android",
                            "-dname", "CN=Android Debug,O=Android,C=US",
                            "-keyalg", "RSA", "-keysize", "2048", "-validity", "9999"],
                           check=True, capture_output=True, timeout=180)
            ok = os.path.isfile(ks)
        except Exception:
            ok = False
    if ok:
        log("debug.keystore generate ho gaya (keytool se) — original signing chalegi")
    else:
        log("keytool se keystore nahi bana — debug signing neutralize karta hoon")
        try:
            g = io.open(bld, encoding="utf-8").read()
            old = 'debug { signingConfig = signingConfigs.getByName("debugConfig") }'
            if old in g:
                io.open(bld, "w", encoding="utf-8").write(
                    g.replace(old, 'debug { /* MAD: default debug signing */ }', 1))
                log("debug signing neutralize ho gaya")
            else:
                log("debugConfig line nahi mili — gradle file as-is")
        except Exception:
            traceback.print_exc()
else:
    log("debug.keystore already hai ya build file missing")

# ---------- 4) local.properties (SDK path) ----------
try:
    sdk = (os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
           or "/usr/local/lib/android/sdk")
    io.open(os.path.join(ROOT, "local.properties"), "w", encoding="utf-8").write(
        "sdk.dir=" + sdk + "\n")
    log("local.properties likha (sdk.dir=" + sdk + ")")
except Exception:
    traceback.print_exc()

# ---------- 5) gradlew cleanup ----------
try:
    for gw in ("gradlew", "gradlew.bat"):
        p = os.path.join(ROOT, gw)
        if os.path.isfile(p):
            os.remove(p)
    log("gradlew hata diya — script-wala gradle use hoga")
except Exception:
    traceback.print_exc()

# ---------- 6) gradle init hook ----------
INIT_GRADLE = r'''
try {
    gradle.buildFinished { result ->
        try {
            def status = (result.failure != null) ? ("FAILED\n\n" + result.failure.toString()) : "SUCCESS - APK ban gaya!"
            def num = null
            def nf = new File("/tmp/mad_issue_number.txt")
            if (nf.exists()) { num = nf.text.trim() }
            def repo = System.getenv("GITHUB_REPOSITORY")
            def token = System.getenv("GITHUB_TOKEN")
            if (repo == null || token == null) { return }
            def msg = "GRADLE RESULT: " + status.take(2500)
            def payload = (num != null && num != "")
                ? groovy.json.JsonOutput.toJson([body: msg])
                : groovy.json.JsonOutput.toJson([title: "MAD Gradle Result", body: msg])
            def path = (num != null && num != "") ? ("/issues/" + num + "/comments") : "/issues"
            def cmd = "curl -s -X POST -H 'Authorization: Bearer " + token + "' -H 'Accept: application/vnd.github+json' -d '" + payload.replace("'", "'\\''") + "' https://api.github.com/repos/" + repo + path
            ["bash", "-c", cmd].execute()
        } catch (Throwable t) {
            println "MAD hook inner: " + t
        }
    }
} catch (Throwable t) {
    println "MAD hook: buildFinished API nahi mili (skip): " + t
}
'''
try:
    gradle_home = os.path.expanduser("~/.gradle")
    os.makedirs(gradle_home, exist_ok=True)
    io.open(os.path.join(gradle_home, "init.gradle"), "w", encoding="utf-8").write(INIT_GRADLE)
    log("gradle init hook lag gaya")
except Exception:
    traceback.print_exc()

log("patch phase complete")
post_patch_report()
flush_summary()
