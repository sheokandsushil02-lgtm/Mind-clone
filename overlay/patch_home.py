#!/usr/bin/env python3
"""MAD patch v5 — extractor + HomeScreen patch + diagnostics.

argv[1] = merged project dir (default: app-merged)

v5 (CRITICAL FIX): workflow apne step me original-app.zip delete kar deta hai
is script se thehle. Ab script ZIP khud laati hai — pehle file se, warna
`git show origin/main:madgitfileforcloud.zip` se, warna GitHub raw URL se.
Isse build hamesha asli project ke saath aage badhega.
"""
import glob
import io
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


def log(msg):
    line = "MAD-PATCH: " + msg
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


SKIP_DIRS = {"build", ".gradle", ".idea", "intermediates", "__pycache__", "node_modules"}


def ensure_zip():
    """ZIP maujood na ho to khud laao (git se, warna raw URL se)."""
    if os.path.isfile(ZIP_NAME) and os.path.getsize(ZIP_NAME) > 100000:
        log("ZIP maujood hai: " + ZIP_NAME)
        return True
    # 1) git se
    try:
        log("ZIP gawayab — git se laa raha hoon")
        r = subprocess.run(
            ["git", "show", "origin/main:" + ZIP_NAME],
            stdout=open(ZIP_NAME, "wb"), stderr=subprocess.PIPE, timeout=300
        )
        if r.returncode == 0 and os.path.getsize(ZIP_NAME) > 100000:
            log("git se ZIP aa gayi (%d bytes)" % os.path.getsize(ZIP_NAME))
            return True
        log("git se nahi mili: " + r.stderr.decode("utf-8", "replace")[:200])
    except Exception:
        log("git fetch fail:")
        traceback.print_exc()
    # 2) raw URL se
    try:
        log("raw URL se download kar raha hoon")
        r = subprocess.run(
            ["curl", "-sSL", "--retry", "3", "-o", ZIP_NAME, RAW_URL],
            stderr=subprocess.PIPE, timeout=600
        )
        if r.returncode == 0 and os.path.isfile(ZIP_NAME) and os.path.getsize(ZIP_NAME) > 100000:
            log("raw URL se ZIP aa gayi (%d bytes)" % os.path.getsize(ZIP_NAME))
            return True
        log("raw URL se bhi fail")
    except Exception:
        log("curl fail:")
        traceback.print_exc()
    return False


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
    return min(pool, key=lambda p: (p.count(os.sep), len(p)))


def unzip_to(zip_path, dst):
    os.makedirs(dst, exist_ok=True)
    with zipfile.ZipFile(zip_path) as z:
        z.extractall(dst)


def extract_real_project(dst):
    if not ensure_zip():
        log("FATAL-INFO: ZIP kisi tarah se nahi mili — project merge nahi hoga")
        return False
    tmp = tempfile.mkdtemp(prefix="madzip_")
    try:
        try:
            with zipfile.ZipFile(ZIP_NAME) as z:
                names = z.namelist()
            log("ZIP me kul %d files hain. Pehli 300:" % len(names))
            for n in names[:300]:
                log("  Z " + n)
        except Exception:
            log("ZIP padhne me dikkat:")
            traceback.print_exc()
            return False

        outer = os.path.join(tmp, "outer")
        unzip_to(ZIP_NAME, outer)
        log("outer ZIP extract ho gaya")

        proj = find_project_root(outer)

        depth = 0
        while not proj and depth < 5:
            depth += 1
            inner_zips = []
            for dirpath, dirnames, filenames in os.walk(outer):
                dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
                for fn in filenames:
                    if fn.lower().endswith(".zip"):
                        inner_zips.append(os.path.join(dirpath, fn))
            if not inner_zips:
                log("aur koi nested ZIP nahi mili (depth %d)" % depth)
                break
            log("depth %d: %d nested ZIPs mili" % (depth, len(inner_zips)))
            for i, iz in enumerate(inner_zips):
                idst = os.path.join(tmp, "nested_%d_%d" % (depth, i))
                try:
                    unzip_to(iz, idst)
                    log("nested ZIP khola: " + os.path.basename(iz))
                    os.remove(iz)
                except Exception:
                    log("nested ZIP nahi khuli: " + os.path.basename(iz))
                    continue
            proj = find_project_root(outer)

        if not proj:
            log("PROJECT ROOT NAHI MILA. extracted ka structure dump:")
            count = 0
            for dirpath, dirnames, filenames in os.walk(outer):
                dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
                rel = os.path.relpath(dirpath, outer)
                log("  D " + rel + " :: " + ", ".join(sorted(filenames)[:12]))
                count += 1
                if count > 80:
                    log("  ... (aur bhi bahut hain)")
                    break
            return False

        log("project mila: " + os.path.relpath(proj, outer))
        ignore = shutil.ignore_patterns("build", ".gradle", ".idea", "*.jks", "*.keystore")
        shutil.copytree(proj, dst, dirs_exist_ok=True, ignore=ignore)
        log("asli project app-merged me merge ho gaya")
        return True
    except Exception:
        log("extract me dikkat:")
        traceback.print_exc()
        return False
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if not os.path.isfile(MANIFEST):
    log("app-merged me manifest nahi — asli project ZIP se nikal raha hoon")
    extract_real_project(ROOT)
else:
    log("app-merged me project pehle se maujood hai")

if os.path.isfile(MANIFEST):
    log("AndroidManifest.xml OK")
else:
    log("FATAL-INFO: AndroidManifest.xml nahi mila — gradle aage fail hoga, par summary me structure hai")

# MAD overlay .kt files (safety copy)
try:
    if os.path.isdir(OVERLAY_MAD):
        target_mad = os.path.join(ROOT, "app", "src", "main", "java", "com", "example", "mad")
        os.makedirs(target_mad, exist_ok=True)
        n = 0
        for f in glob.glob(os.path.join(OVERLAY_MAD, "*.kt")):
            shutil.copy2(f, target_mad)
            n += 1
        log("MAD .kt files copy: %d" % n)
except Exception:
    traceback.print_exc()

# HomeScreen MAD button
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

# debug signing fix
try:
    ks = os.path.join(ROOT, "debug.keystore")
    bld = os.path.join(ROOT, "app", "build.gradle.kts")
    if os.path.isfile(bld) and not os.path.isfile(ks):
        g = io.open(bld, encoding="utf-8").read()
        old = 'debug { signingConfig = signingConfigs.getByName("debugConfig") }'
        if old in g:
            g = g.replace(old, 'debug { /* MAD: default debug signing */ }', 1)
            io.open(bld, "w", encoding="utf-8").write(g)
            log("debug signing neutralize ho gaya (debug.keystore missing tha)")
        else:
            log("debugConfig line nahi mili — gradle file as-is")
    else:
        log("debug.keystore maujood hai ya build file missing — signing untouched")
except Exception:
    traceback.print_exc()

# gradlew hatao — system gradle use hoga
try:
    for gw in ("gradlew", "gradlew.bat"):
        p = os.path.join(ROOT, gw)
        if os.path.isfile(p):
            os.remove(p)
    log("gradlew hataya gaya — system gradle use hoga")
except Exception:
    traceback.print_exc()

log("done")
flush_summary()
