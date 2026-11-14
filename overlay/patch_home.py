#!/usr/bin/env python3
"""MAD patch v9 - project overlay/project/ se seedha copy (deterministic).

v9: ZIP/download ka koi bharosa nahi. Poora project ab repo ke
overlay/project/ folder me maujood hai (settings, gradle, sources,
resources, manifest - sab). Ye script bas:
  1. overlay/project/* -> app-merged/ me copy karta hai
  2. MAD .kt files copy karta hai
  3. HomeScreen me MAD button patch karta hai (agar zaroorat ho)
  4. debug.keystore generate karta hai
  5. local.properties aur .env likhta hai
Har failure public annotation + summary me chhapta hai.
"""
import glob
import io
import os
import shutil
import subprocess
import sys
import traceback

ROOT = sys.argv[1] if len(sys.argv) > 1 else "app-merged"
SRC = "overlay/project"
OVERLAY_MAD = os.path.join("overlay", "app", "src", "main", "java", "com", "example", "mad")
SUMMARY_LINES = []


def log(msg):
    line = "MAD-PATCH: " + str(msg)
    print(line, flush=True)
    SUMMARY_LINES.append(line)


def announce(msg):
    safe = str(msg).replace("\n", "%0A").replace("\r", "")[:400]
    print("::error title=MAD-PATCH::" + safe, flush=True)


def flush_summary():
    path = os.environ.get("GITHUB_STEP_SUMMARY")
    if path:
        try:
            with io.open(path, "a", encoding="utf-8") as f:
                f.write("\n## MAD Patch Report\n```\n" + "\n".join(SUMMARY_LINES) + "\n```\n")
        except Exception:
            pass


# ---------- 0) project copy from overlay/project ----------
if not os.path.isdir(SRC):
    log("FATAL: overlay/project folder nahi mila")
    announce("FATAL: overlay/project folder repo me nahi hai")
    flush_summary()
    sys.exit(1)

os.makedirs(ROOT, exist_ok=True)
copied = 0
for dirpath, dirnames, filenames in os.walk(SRC):
    dirnames[:] = [d for d in dirnames if d not in ("build", ".gradle")]
    for fn in filenames:
        s = os.path.join(dirpath, fn)
        rel = os.path.relpath(s, SRC)
        d = os.path.join(ROOT, rel)
        os.makedirs(os.path.dirname(d), exist_ok=True)
        shutil.copy2(s, d)
        copied += 1
log("project files copy: " + str(copied))

for rel in ("settings.gradle.kts", "app/build.gradle.kts",
            "app/src/main/AndroidManifest.xml",
            "app/src/main/java/com/example/MainActivity.kt"):
    state = "HAI  " if os.path.isfile(os.path.join(ROOT, rel)) else "NAHI "
    log(state + rel)
    if state == "NAHI " and rel != "app/src/main/java/com/example/MainActivity.kt":
        announce("FATAL: " + rel + " copy ke baad bhi nahi mila")
        flush_summary()
        sys.exit(1)

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
        if n == 0:
            announce("WARNING: overlay MAD me koi .kt nahi")
    else:
        announce("WARNING: overlay MAD folder nahi mila")
except Exception:
    announce("MAD copy EXC: " + traceback.format_exc()[-200:])

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
    announce("HomeScreen.kt nahi mila")
else:
    try:
        src = io.open(target, encoding="utf-8", errors="replace").read()
        if "hub_mad_button" in src:
            log("MAD button already present (HomeScreen overlay me built-in hai)")
        else:
            anchor = "import com.example.utils.ShareAndPrintUtils"
            if "import com.example.mad.MadLauncher" not in src:
                if anchor in src:
                    src = src.replace(anchor, anchor + "\nimport com.example.mad.MadLauncher", 1)
                else:
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
                announce("HomeScreen me marker nahi mila — button add NAHI hua")
            else:
                io.open(target, "w", encoding="utf-8").write(src)
                log("HomeScreen patch save ho gaya")
    except Exception:
        announce("HomeScreen EXC: " + traceback.format_exc()[-200:])

# ---------- 3) debug.keystore ----------
try:
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
    log("local.properties likhi")
except Exception:
    traceback.print_exc()

# ---------- 5) .env ----------
try:
    envp = os.path.join(ROOT, ".env")
    exp = os.path.join(ROOT, ".env.example")
    if not os.path.isfile(envp) and os.path.isfile(exp):
        shutil.copyfile(exp, envp)
        log(".env bana")
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
    log("gradlew cleanup OK")
except Exception:
    traceback.print_exc()

log("patch phase complete")
flush_summary()
