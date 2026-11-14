#!/usr/bin/env python3
"""MAD patch v8 - chhoti ZIP (chat wali) se project fallback.

v8 fix: badi ZIP (madgitfileforcloud.zip) me asli project nahi nikalta
(wrapper folders hain). Isliye ab seedha asli project ki ZIP use hoti hai
jo AI Studio ne banayi thi — ye S3 par hosted hai aur download hoti hai.
"""
import glob
import io
import os
import shutil
import subprocess
import sys
import tempfile
import traceback
import urllib.request
import zipfile

ROOT = sys.argv[1] if len(sys.argv) > 1 else "app-merged"
SMALL_ZIP_URL = ("https://d3p662obnq9uz2.cloudfront.net/chat-uploads/chat/user/"
                 "6af7478f74ac87d004bc3005/6af747bc5ce1b36e2aabca96/"
                 "f8ed31f0-rems-business.zip")
BIG_ZIP_NAME = "madgitfileforcloud.zip"
BIG_ZIP_URL = "https://github.com/sheokandsushil02-lgtm/Mind-clone/raw/main/" + BIG_ZIP_NAME
OVERLAY_MAD = os.path.join("overlay", "app", "src", "main", "java", "com", "example", "mad")
MANIFEST = os.path.join(ROOT, "app", "src", "main", "AndroidManifest.xml")
SUMMARY_LINES = []


def log(msg):
    line = "MAD-PATCH: " + str(msg)
    print(line, flush=True)
    SUMMARY_LINES.append(line)


def flush_summary():
    path = os.environ.get("GITHUB_STEP_SUMMARY")
    if path:
        try:
            with io.open(path, "a", encoding="utf-8") as f:
                f.write("\n## MAD Patch Report\n```\n" + "\n".join(SUMMARY_LINES) + "\n```\n")
        except Exception:
            pass


def announce(msg):
    safe = str(msg).replace("\n", "%0A").replace("\r", "")[:400]
    print("::error title=MAD-PATCH::" + safe, flush=True)


def download(url, dst):
    try:
        urllib.request.urlretrieve(url, dst)
        return os.path.isfile(dst) and os.path.getsize(dst) > 5000
    except Exception as e:
        log("download fail " + url[:60] + ": " + repr(e)[:150])
        return False


def copy_project_from_zip(zip_path, dst):
    try:
        with zipfile.ZipFile(zip_path) as z:
            names = z.namelist()
        log("ZIP me kul " + str(len(names)) + " entries")
        root_prefix = ""
        for n in names:
            parts = n.split("/")
            for i in range(len(parts)):
                cand = "/".join(parts[:i])
                if cand and (cand + "/settings.gradle.kts") in names:
                    root_prefix = cand
                    break
            if root_prefix:
                break
        if "settings.gradle.kts" in names and not root_prefix:
            root_prefix = ""
        log("project root prefix: '" + root_prefix + "'")
        os.makedirs(dst, exist_ok=True)
        with zipfile.ZipFile(zip_path) as z:
            for n in names:
                if n.endswith("/"):
                    continue
                rel = n[len(root_prefix) + 1:] if root_prefix else n
                if not rel or rel.startswith("assets/") or rel.startswith(".aistudio"):
                    continue
                tgt = os.path.join(dst, rel)
                os.makedirs(os.path.dirname(tgt), exist_ok=True)
                with z.open(n) as srcf, open(tgt, "wb") as tf:
                    shutil.copyfileobj(srcf, tf)
        for rel in ("settings.gradle.kts", "app/build.gradle.kts",
                    "app/src/main/AndroidManifest.xml"):
            log(("HAI  " if os.path.isfile(os.path.join(dst, rel)) else "NAHI ") + rel)
        return os.path.isfile(os.path.join(dst, "settings.gradle.kts"))
    except Exception:
        log("project copy me dikkat:")
        announce(traceback.format_exc()[-300:])
        return False


# ---------- 0) project laao ----------
ok = False
if os.path.isfile(MANIFEST) and os.path.isfile(os.path.join(ROOT, "settings.gradle.kts")):
    log("app-merged me project pehle se maujood")
    ok = True

if not ok:
    small = os.path.join(tempfile.gettempdir(), "small_rems.zip")
    if download(SMALL_ZIP_URL, small):
        log("chhoti ZIP (asli project) download hui")
        ok = copy_project_from_zip(small, ROOT)
    else:
        log("chhoti ZIP download fail — badi ZIP try karta hoon")
        big = os.path.join(tempfile.gettempdir(), BIG_ZIP_NAME)
        if not os.path.isfile(BIG_ZIP_NAME):
            download(BIG_ZIP_URL, big)
        srczip = BIG_ZIP_NAME if os.path.isfile(BIG_ZIP_NAME) else big
        if os.path.isfile(srczip):
            tmp = tempfile.mkdtemp(prefix="madbig_")
            with zipfile.ZipFile(srczip) as z:
                z.extractall(tmp)
            inner = [os.path.join(dp, f) for dp, dn, fn in os.walk(tmp)
                     for f in fn if f.lower().endswith(".zip")]
            for iz in inner:
                try:
                    with zipfile.ZipFile(iz) as z:
                        z.extractall(os.path.join(os.path.dirname(iz), "inner"))
                    log("badi ZIP ka nested wrapper khola: " + os.path.basename(iz))
                except Exception:
                    log("nested nahi khula: " + os.path.basename(iz))
            cands = []
            for dp, dn, fn in os.walk(tmp):
                if "settings.gradle.kts" in fn:
                    cands.append(dp)
            if cands:
                proj = min(cands, key=lambda p: p.count(os.sep))
                log("badi ZIP me project mila: " + os.path.relpath(proj, tmp))
                ignore = shutil.ignore_patterns("build", ".gradle", ".idea")
                shutil.copytree(proj, ROOT, dirs_exist_ok=True, ignore=ignore)
                ok = True
            else:
                log("badi ZIP me bhi project nahi mila")
            shutil.rmtree(tmp, ignore_errors=True)

if not ok and not os.path.isfile(os.path.join(ROOT, "settings.gradle.kts")):
    log("FATAL: kisi bhi ZIP se project nahi mila")
    announce("FATAL: project nahi mila (chhoti aur badi dono ZIP fail)")
    flush_summary()
    sys.exit(1)

log("app-merged top: " + ", ".join(sorted(os.listdir(ROOT))[:25]))

# ---------- 1) MAD overlay .kt files ----------
try:
    if os.path.isdir(OVERLAY_MAD):
        t = os.path.join(ROOT, "app", "src", "main", "java", "com", "example", "mad")
        os.makedirs(t, exist_ok=True)
        n = 0
        for f in glob.glob(os.path.join(OVERLAY_MAD, "*.kt")):
            shutil.copy2(f, t)
            n += 1
        log("MAD .kt files copy: " + str(n))
except Exception:
    traceback.print_exc()

# ---------- 2) HomeScreen MAD button ----------
target = None
for c in glob.glob(os.path.join(ROOT, "app/src/main/java/**/HomeScreen.kt"), recursive=True):
    if "ui/screens" in c.replace(os.sep, "/"):
        target = c
        break
if not target:
    cands = glob.glob(os.path.join(ROOT, "app/src/main/java/**/HomeScreen.kt"), recursive=True)
    target = cands[0] if cands else None

if not target:
    log("FATAL-INFO: HomeScreen.kt nahi mila")
    announce("HomeScreen.kt nahi mila — MAD button add nahi hua")
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
            elif "import com.example.mad.MadLauncher" not in src:
                pe = src.find("\n", src.find("package "))
                if pe != -1:
                    src = src[:pe + 1] + "\nimport com.example.mad.MadLauncher\n" + src[pe + 1:]
            button = '''        Spacer(modifier = Modifier.height(16.dp))

        // === MAD AI BUTTON (overlay) ===
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
                ls = src.rfind("\n", 0, idx) + 1
                src = src[:ls] + button + "\n" + src[ls:]
                inserted = True
                log("button inserted @ marker: " + m.strip()[:40])
                break
            if not inserted:
                log("WARNING: koi marker match nahi hua")
                announce("HomeScreen me marker nahi mila — button add NAHI hua")
            else:
                io.open(target, "w", encoding="utf-8").write(src)
                log("HomeScreen patch save ho gaya")
    except Exception:
        log("HomeScreen patch me dikkat:")
        announce(traceback.format_exc()[-300:])

# ---------- 3) debug.keystore ----------
try:
    ks = os.path.join(ROOT, "debug.keystore")
    bld = os.path.join(ROOT, "app", "build.gradle.kts")
    if os.path.isfile(bld) and not os.path.isfile(ks):
        kt = shutil.which("keytool")
        ok2 = False
        if kt:
            try:
                subprocess.run([kt, "-genkeypair", "-keystore", ks, "-storepass", "android",
                                "-alias", "androiddebugkey", "-keypass", "android",
                                "-dname", "CN=Android Debug,O=Android,C=US",
                                "-keyalg", "RSA", "-keysize", "2048", "-validity", "9999"],
                               check=True, capture_output=True, timeout=180)
                ok2 = os.path.isfile(ks)
            except Exception:
                ok2 = False
        if ok2:
            log("debug.keystore generate ho gaya")
        else:
            g = io.open(bld, encoding="utf-8").read()
            old = 'debug { signingConfig = signingConfigs.getByName("debugConfig") }'
            if old in g:
                io.open(bld, "w", encoding="utf-8").write(
                    g.replace(old, 'debug { /* MAD: default debug signing */ }', 1))
                log("debug signing neutralize ho gayi")
            else:
                log("debugConfig line nahi mili")
    else:
        log("keystore step skip")
except Exception:
    traceback.print_exc()

# ---------- 4) local.properties ----------
try:
    sdk = (os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
           or "/usr/local/lib/android/sdk")
    io.open(os.path.join(ROOT, "local.properties"), "w", encoding="utf-8").write(
        "sdk.dir=" + sdk + "\n")
    log("local.properties likhi (sdk.dir=" + sdk + ")")
except Exception:
    traceback.print_exc()

# ---------- 5) .env banao (secrets plugin ke liye) ----------
try:
    envp = os.path.join(ROOT, ".env")
    exp = os.path.join(ROOT, ".env.example")
    if not os.path.isfile(envp) and os.path.isfile(exp):
        shutil.copyfile(exp, envp)
        log(".env bana (.env.example se copy)")
    elif not os.path.isfile(envp):
        io.open(envp, "w", encoding="utf-8").write("")
        log(".env khali banayi")
except Exception:
    traceback.print_exc()

# ---------- 6) gradlew cleanup ----------
try:
    for gw in ("gradlew", "gradlew.bat"):
        p = os.path.join(ROOT, gw)
        if os.path.isfile(p):
            os.remove(p)
    log("gradlew hata diya")
except Exception:
    traceback.print_exc()

log("patch phase complete")
flush_summary()
