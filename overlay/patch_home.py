#!/usr/bin/env python3
"""MAD patch v3 — extractor + HomeScreen patch + build fixes.

argv[1] = merged project dir (default: app-merged)

Order:
  0. AndroidManifest missing? -> madgitfileforcloud.zip se asli project nikalo
     (outer + nested zip dono). Mile to app-merged me merge karo.
  1. MAD overlay .kt files dobara copy (safety).
  2. HomeScreen me MAD button + import.
  3. debug signing fix (debug.keystore missing ho to).
  4. gradlew hata do -> system gradle 9.3.1 use hoga.
Har step graceful — script build nahi rokta, bas summary me sab likhta hai.
"""
import glob
import io
import os
import shutil
import sys
import tempfile
import traceback
import zipfile

ROOT = sys.argv[1] if len(sys.argv) > 1 else "app-merged"
ZIP_NAME = "madgitfileforcloud.zip"
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


# ---------- 0) real project extractor ----------
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
    return min(pool, key=lambda p: (p.count(os.sep), len(p)))


def unzip_to(zip_path, dst):
    os.makedirs(dst, exist_ok=True)
    with zipfile.ZipFile(zip_path) as z:
        z.extractall(dst)


def extract_real_project(dst):
    if not os.path.isfile(ZIP_NAME):
        log("ZIP nahi mila: " + ZIP_NAME)
        return False
    tmp = tempfile.mkdtemp(prefix="madzip_")
    try:
        outer = os.path.join(tmp, "outer")
        unzip_to(ZIP_NAME, outer)
        log("outer ZIP extract ho gaya")
        proj = find_project_root(outer)
        if proj:
            log("project outer ZIP me mila: " + os.path.relpath(proj, outer))
        else:
            log("outer me project nahi — nested zips khol raha hoon")
            inner_id = 0
            for dirpath, dirnames, filenames in os.walk(outer):
                for fn in filenames:
                    if not fn.lower().endswith(".zip"):
                        continue
                    inner_id += 1
                    inner_src = os.path.join(dirpath, fn)
                    inner_dst = os.path.join(tmp, "inner_%d" % inner_id)
                    try:
                        unzip_to(inner_src, inner_dst)
                        log("nested ZIP khola: " + fn)
                    except Exception:
                        log("nested ZIP nahi khula: " + fn)
                        continue
                    proj = find_project_root(inner_dst)
                    if proj:
                        log("project nested ZIP me mila: " + os.path.relpath(proj, inner_dst))
                        break
                if proj:
                    break
        if not proj:
            log("WARNING: ZIP me project root nahi mila — structure dump:")
            for dirpath, dirnames, filenames in os.walk(outer):
                rel = os.path.relpath(dirpath, outer)
                if rel.count(os.sep) <= 2:
                    log("  " + rel + " :: " + ", ".join(sorted(filenames)[:10]))
            return False
        ignore = shutil.ignore_patterns("build", ".gradle", ".idea", "*.jks")
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
    log("FATAL: AndroidManifest.xml ab bhi nahi mila — gradle fail hoga")

# ---------- 1) MAD overlay .kt files ----------
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
    log("FATAL: HomeScreen.kt nahi mila — MAD button add nahi hua")
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

# ---------- 3) debug signing fix ----------
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

# ---------- 4) gradlew removal (system gradle 9.3.1 use hoga) ----------
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
