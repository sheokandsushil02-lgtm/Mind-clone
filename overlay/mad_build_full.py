#!/usr/bin/env python3
"""MAD build orchestrator (pure Python) — v4.

Bash se aane wali saari bimariyon se mukt (CRLF, parse errors).
- ZIP fetch (git ya raw URL), nested unzip, patch scripts chalata hai
- Har failure ka poora reason:
    1) run page ke SUMMARY me chhapti hai (bina login public)
    2) repo me ISSUE ban jati hai
- Fail hone par bhi saaf-saaf batata hai kya hua.
"""
import json
import os
import subprocess
import sys
import urllib.request

ZIP_NAME = "madgitfileforcloud.zip"
RAW_URL = "https://github.com/sheokandsushil02-lgtm/Mind-clone/raw/main/" + ZIP_NAME
LOG = "/tmp/mad_build.log"
STEP_SUMMARY = os.environ.get("GITHUB_STEP_SUMMARY", "")
REPORT = []
LINES = []

def log(msg):
    line = str(msg)
    print("MAD:", line)
    LINES.append(line)

def summary_write(extra=""):
    if not STEP_SUMMARY:
        return
    try:
        with open(STEP_SUMMARY, "a", encoding="utf-8") as f:
            f.write("\n## MAD Build Report\n```\n" + "\n".join(LINES) + "\n```\n" + extra)
    except Exception:
        pass

def post_issue(title):
    token = os.environ.get("GITHUB_TOKEN", "")
    repo = os.environ.get("GITHUB_REPOSITORY", "sheokandsushil02-lgtm/Mind-clone")
    if not token:
        log("issue post skip: GITHUB_TOKEN nahi mila")
        return
    body = "\n".join(LINES)[:24000]
    try:
        tail_log = open(LOG, encoding="utf-8", errors="replace").read()[-8000:]
    except Exception:
        tail_log = "(log file nahi khuli)"
    payload = json.dumps({
        "title": title,
        "body": body + "\n\n--- last log ---\n```\n" + tail_log + "\n```"
    }).encode("utf-8")
    req = urllib.request.Request(
        "https://api.github.com/repos/" + repo + "/issues",
        data=payload,
        headers={
            "Authorization": "Bearer " + token,
            "Accept": "application/vnd.github+json",
            "Content-Type": "application/json",
        })
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            data = json.loads(r.read().decode("utf-8"))
            log("issue ban gaya #" + str(data.get("number")))
    except Exception as e:
        log("issue post fail: " + str(e)[:200])

def fail(msg):
    log("MAD-BUILD-FAIL: " + msg)
    summary_write()
    post_issue("MAD Build Failed: " + msg[:60])
    sys.exit(1)

def run(cmd, **kw):
    """Command chalao, output ko log file + LINES me capture karo."""
    log("$ " + " ".join(cmd))
    try:
        p = subprocess.run(cmd, capture_output=True, text=True, timeout=3600, **kw)
    except Exception as e:
        log("EXCEPTION: " + str(e)[:300])
        return -1, ""
    out = (p.stdout or "") + (p.stderr or "")
    if out:
        with open(LOG, "a", encoding="utf-8", errors="replace") as f:
            f.write(out)
        for ln in out.splitlines():
            if len(LINES) < 120:
                LINES.append(ln.strip()[:250])
    return p.returncode, out

# ---------- 0) syntax check ----------
open(LOG, "w").close()
for s in ("overlay/patch_home.py", "overlay/patch_manifest.py"):
    rc, _ = run([sys.executable, "-m", "py_compile", s])
    if rc != 0:
        fail(s + " me Python syntax error (report me traceback hai)")
log("python scripts compile OK")

# ---------- 1) ZIP ----------
if not os.path.isfile(ZIP_NAME) or os.path.getsize(ZIP_NAME) < 100000:
    run(["git", "fetch", "origin", "main", "--depth=1"])
    with open(ZIP_NAME, "wb") as f:
        p = subprocess.run(["git", "show", "origin/main:" + ZIP_NAME],
                           stdout=f, stderr=subprocess.PIPE)
    if p.returncode != 0 and (not os.path.isfile(ZIP_NAME) or
                              os.path.getsize(ZIP_NAME) < 100000):
        try:
            urllib.request.urlretrieve(RAW_URL, ZIP_NAME)
            log("raw URL se ZIP aayi")
        except Exception as e:
            log("raw URL fail: " + str(e)[:200])
if not os.path.isfile(ZIP_NAME) or os.path.getsize(ZIP_NAME) < 100000:
    fail("ZIP nahi mili (git se bhi nahi, raw URL se bhi nahi)")
log("ZIP size: " + str(os.path.getsize(ZIP_NAME)))

# ---------- 2) patch ----------
rc, _ = run([sys.executable, "overlay/patch_home.py", "app-merged"])
if rc != 0:
    fail("patch_home.py fail (MAD-PATCH lines report me hain)")
rc, _ = run([sys.executable, "overlay/patch_manifest.py", "app-merged"])
if rc != 0:
    log("WARN: patch_manifest.py fail (jaari)")

for rel in ("settings.gradle.kts", "app/build.gradle.kts"):
    if not os.path.isfile(os.path.join("app-merged", rel)):
        fail("app-merged/" + rel + " nahi mila")
log("app-merged top: " + ", ".join(sorted(os.listdir("app-merged"))[:20]))

# ---------- 3) JDK ----------
jh = os.environ.get("JAVA_HOME", "")
if not os.path.isdir(jh):
    import glob as _g
    cands = _g.glob("/usr/lib/jvm/*17*") + _g.glob("/usr/lib/jvm/*temurin*")
    jh = sorted(cands)[0] if cands else ""
if not jh or not os.path.isdir(jh):
    fail("JDK 17 nahi mila")
os.environ["JAVA_HOME"] = jh
os.environ["PATH"] = jh + "/bin:" + os.environ.get("PATH", "")
rc, _ = run(["java", "-version"])
if rc != 0:
    fail("java nahi chala")
log("JAVA_HOME: " + jh)

# ---------- 4) gradle ----------
GRADLE_VER = "9.3.1"
gbin = None
rc, out = run(["bash", "-lc", "command -v gradle"])
if rc == 0 and out.strip():
    gbin = out.strip()
    log("system gradle mila: " + gbin)
else:
    gdir = "/opt/gradle-" + GRADLE_VER
    gbin = gdir + "/bin/gradle"
    if not os.path.isfile(gbin):
        gz = "/tmp/gradle.zip"
        try:
            log("gradle download kar raha hoon...")
            urllib.request.urlretrieve(
                "https://services.gradle.org/distributions/gradle-" + GRADLE_VER + "-bin.zip", gz)
            run(["unzip", "-q", gz, "-d", "/opt"])
        except Exception as e:
            fail("gradle download/unzip fail: " + str(e)[:200])
    if not os.path.isfile(gbin):
        fail("gradle binary nahi mili: " + gbin)
rc, _ = run([gbin, "--version"])
if rc != 0:
    fail("gradle nahi chala")

# ---------- 5) build ----------
os.chdir("app-merged")
rc, _ = run([gbin, "assembleDebug", "--no-daemon", "--stacktrace"])
if rc != 0:
    fail("GRADLE BUILD FAIL — report me poora error stack hai")
APK = "app/build/outputs/apk/debug/app-debug.apk"
if not os.path.isfile(APK):
    fail("APK nahi bani: " + APK)
size = os.path.getsize(APK) // (1024 * 1024)
log("MAD-APK-READY: " + str(size) + " MB")
print("APK_PATH: " + os.path.join(os.getcwd(), APK))
summary_write("\n## MAD APK ban gaya! " + str(size) + " MB\n")
log("SUCCESS")
