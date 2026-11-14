#!/usr/bin/env python3
"""MAD build orchestrator v7 - report via git push (checkout credentials)."""
import json
import os
import subprocess
import sys
import traceback
import urllib.request

ZIP_NAME = "madgitfileforcloud.zip"
RAW_URL = "https://github.com/sheokandsushil02-lgtm/Mind-clone/raw/main/" + ZIP_NAME
LOG = "/tmp/mad_build.log"
SUMMARY = os.environ.get("GITHUB_STEP_SUMMARY", "")
LINES = []


def log(msg):
    line = str(msg)
    print("MAD: " + line, flush=True)
    LINES.append(line[:250])


def write_summary(tag):
    if not SUMMARY:
        return
    try:
        f = open(SUMMARY, "a", encoding="utf-8")
        f.write("\n## MAD Build Report [" + tag + "]\n```\n" + "\n".join(LINES) + "\n```\n")
        f.close()
    except Exception:
        pass


def push_report(tag):
    """Checkout ke configured git credentials se mad-report branch pe push."""
    ws = os.environ.get("GITHUB_WORKSPACE", os.getcwd())
    try:
        import shutil
        rf = os.path.join(ws, "MAD_REPORT.md")
        f = open(rf, "w", encoding="utf-8")
        f.write("# MAD Report [" + tag + "]\n\n```\n" + "\n".join(LINES) + "\n```\n")
        f.close()
        try:
            shutil.copyfile(LOG, os.path.join(ws, "mad_build.log"))
        except Exception:
            pass
        cmds = (["git", "config", "user.name", "mad-bot"],
                ["git", "config", "user.email", "mad-bot@users.noreply.github.com"],
                ["git", "checkout", "--orphan", "mad-report"],
                ["git", "add", "-f", "MAD_REPORT.md", "mad_build.log"],
                ["git", "commit", "-m", "MAD report " + tag],
                ["git", "push", "origin", "mad-report"])
        for c in cmds:
            p = subprocess.run(c, cwd=ws, capture_output=True, text=True, timeout=180)
            if p.returncode != 0 and c[:2] == ["git", "push"]:
                log("push fail: " + (p.stderr or "")[:300])
                return
        log("REPORT PUSHED to mad-report branch")
    except Exception as e:
        log("push_report EXC: " + repr(e)[:200])


def die(msg):
    log("BUILD-FAIL: " + msg)
    write_summary("FAIL")
    push_report("FAIL")
    sys.exit(1)


def run(cmd):
    log("$ " + " ".join(cmd))
    try:
        p = subprocess.run(cmd, capture_output=True, text=True, timeout=3600)
    except Exception as e:
        log("EXC: " + repr(e)[:250])
        return -1
    out = (p.stdout or "") + (p.stderr or "")
    if out:
        try:
            lf = open(LOG, "a", encoding="utf-8", errors="replace")
            lf.write(out)
            lf.close()
        except Exception:
            pass
        for ln in out.splitlines():
            if len(LINES) < 120:
                LINES.append(ln.strip()[:250])
    return p.returncode


def main():
    log("SCRIPT-START ok")
    open(LOG, "w").close()

    for s in ("overlay/patch_home.py", "overlay/patch_manifest.py"):
        if run([sys.executable, "-m", "py_compile", s]) != 0:
            die(s + " syntax error")
    log("patch scripts compile OK")

    if not os.path.isfile(ZIP_NAME) or os.path.getsize(ZIP_NAME) < 100000:
        run(["git", "fetch", "origin", "main", "--depth=1"])
        p = subprocess.run(["git", "show", "origin/main:" + ZIP_NAME],
                           stdout=open(ZIP_NAME, "wb"), stderr=subprocess.PIPE)
        if p.returncode != 0 and (not os.path.isfile(ZIP_NAME) or
                                  os.path.getsize(ZIP_NAME) < 100000):
            try:
                urllib.request.urlretrieve(RAW_URL, ZIP_NAME)
                log("ZIP raw URL se aayi")
            except Exception as e:
                log("raw url fail: " + repr(e)[:200])
    if not os.path.isfile(ZIP_NAME) or os.path.getsize(ZIP_NAME) < 100000:
        die("ZIP nahi mili")
    log("ZIP size: " + str(os.path.getsize(ZIP_NAME)))

    if run([sys.executable, "overlay/patch_home.py", "app-merged"]) != 0:
        die("patch_home.py fail")
    if run([sys.executable, "overlay/patch_manifest.py", "app-merged"]) != 0:
        log("WARN: patch_manifest fail, jaari")
    for rel in ("settings.gradle.kts", "app/build.gradle.kts"):
        if not os.path.isfile(os.path.join("app-merged", rel)):
            die("app-merged/" + rel + " nahi mila")
    log("app-merged top: " + ", ".join(sorted(os.listdir("app-merged"))[:20]))
    write_summary("STEP2")

    jh = os.environ.get("JAVA_HOME", "")
    if not os.path.isdir(jh):
        import glob
        c = sorted(glob.glob("/usr/lib/jvm/*17*") + glob.glob("/usr/lib/jvm/*temurin*"))
        jh = c[0] if c else ""
    if not jh or not os.path.isdir(jh):
        die("JDK 17 nahi mila")
    os.environ["JAVA_HOME"] = jh
    os.environ["PATH"] = jh + "/bin:" + os.environ.get("PATH", "")
    if run(["java", "-version"]) != 0:
        die("java nahi chala")
    log("JAVA_HOME: " + jh)

    gbin = ""
    try:
        p = subprocess.run(["bash", "-lc", "command -v gradle"], capture_output=True, text=True)
        if p.returncode == 0:
            gbin = (p.stdout or "").strip()
    except Exception:
        pass
    if gbin and os.path.isfile(gbin):
        log("system gradle: " + gbin)
    else:
        gbin = "/opt/gradle-9.3.1/bin/gradle"
        if not os.path.isfile(gbin):
            gz = "/tmp/gradle.zip"
            try:
                log("gradle download...")
                urllib.request.urlretrieve(
                    "https://services.gradle.org/distributions/gradle-9.3.1-bin.zip", gz)
                run(["unzip", "-q", gz, "-d", "/opt"])
            except Exception as e:
                die("gradle download fail: " + repr(e)[:200])
        if not os.path.isfile(gbin):
            die("gradle binary nahi: " + gbin)
    if run([gbin, "--version"]) != 0:
        die("gradle nahi chala")

    os.chdir("app-merged")
    if run([gbin, "assembleDebug", "--no-daemon", "--stacktrace"]) != 0:
        die("GRADLE BUILD FAIL")
    apk = "app/build/outputs/apk/debug/app-debug.apk"
    if not os.path.isfile(apk):
        die("APK nahi bani: " + apk)
    mb = os.path.getsize(apk) // (1024 * 1024)
    log("APK-READY: " + str(mb) + " MB")
    write_summary("SUCCESS")


try:
    main()
    log("DONE")
except SystemExit:
    raise
except BaseException:
    log("UNCAUGHT EXCEPTION:")
    log(traceback.format_exc()[:2000])
    write_summary("CRASH")
    push_report("CRASH")
    sys.exit(1)
