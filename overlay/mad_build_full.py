#!/usr/bin/env python3
"""MAD build orchestrator v6 - report pushes to mad-report branch."""
import json
import os
import shutil
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
    ws = os.environ.get("GITHUB_WORKSPACE", os.getcwd())
    try:
        rf = os.path.join(ws, "MAD_REPORT.md")
        f = open(rf, "w", encoding="utf-8")
        f.write("# MAD Report [" + tag + "]\n\n```\n" + "\n".join(LINES) + "\n```\n")
        f.close()
        try:
            shutil.copyfile(LOG, os.path.join(ws, "mad_build.log"))
        except Exception:
            pass
        for c in (["git", "config", "user.name", "mad-bot"],
                  ["git", "config", "user.email", "mad-bot@users.noreply.github.com"],
                  ["git", "checkout", "-B", "mad-report"],
                  ["git", "add", "-f", "MAD_REPORT.md", "mad_build.log"],
                  ["git", "commit", "-m", "MAD report " + tag]):
            subprocess.run(c, cwd=ws, capture_output=True, text=True, timeout=120)
        token = os.environ.get("GITHUB_TOKEN", "")
        repo = os.environ.get("GITHUB_REPOSITORY", "")
        if token and repo:
            url = "https://x-access-token:" + token + "@github.com/" + repo + ".git"
            p = subprocess.run(["git", "push", url, "mad-report"], cwd=ws,
                               capture_output=True, text=True, timeout=180)
            if p.returncode == 0:
                log("report mad-report branch pe push ho gaya")
            else:
                log("report push fail: " + (p.stderr or "")[:200])
        else:
            log("report push skip: GITHUB_TOKEN env missing")
    except Exception as e:
        log("push_report EXC: " + repr(e)[:200])


def post_issue(subject):
    token = os.environ.get("GITHUB_TOKEN", "")
    repo = os.environ.get("GITHUB_REPOSITORY", "sheokandsushil02-lgtm/Mind-clone")
    if not token:
        log("issue skip: GITHUB_TOKEN env missing")
        return
    try:
        lf = open(LOG, encoding="utf-8", errors="replace")
        tail = lf.read()[-6000:]
        lf.close()
    except Exception:
        tail = "(no log)"
    body = "\n".join(LINES)[:20000] + "\n--- log tail ---\n" + tail
    data = json.dumps({"title": subject[:70], "body": body[:25000]}).encode("utf-8")
    req = urllib.request.Request("https://api.github.com/repos/" + repo + "/issues", data=data,
                                 headers={"Authorization": "Bearer " + token,
                                          "Accept": "application/vnd.github+json",
                                          "Content-Type": "application/json"})
    try:
        r = urllib.request.urlopen(req, timeout=60)
        d = json.loads(r.read().decode("utf-8"))
        r.close()
        log("issue #" + str(d.get("number")) + " ban gaya")
    except Exception as e:
        log("issue post fail: " + repr(e)[:200])


def die(msg):
    log("BUILD-FAIL: " + msg)
    write_summary("FAIL")
    post_issue("MAD Build Failed: " + msg)
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
            if len(LINES) < 100:
                LINES.append(ln.strip()[:250])
    return p.returncode


def main():
    log("SCRIPT-START ok")
    log("token env: " + ("HAI" if os.environ.get("GITHUB_TOKEN") else "NAHI"))
    open(LOG, "w").close()

    # 0) syntax check of patch scripts
    for s in ("overlay/patch_home.py", "overlay/patch_manifest.py"):
        if run([sys.executable, "-m", "py_compile", s]) != 0:
            die(s + " syntax error")
    log("patch scripts compile OK")
    write_summary("STEP1")

    # 1) ZIP
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

    # 2) patches
    if run([sys.executable, "overlay/patch_home.py", "app-merged"]) != 0:
        die("patch_home.py fail")
    if run([sys.executable, "overlay/patch_manifest.py", "app-merged"]) != 0:
        log("WARN: patch_manifest fail, jaari")
    for rel in ("settings.gradle.kts", "app/build.gradle.kts"):
        if not os.path.isfile(os.path.join("app-merged", rel)):
            die("app-merged/" + rel + " nahi mila")
    log("app-merged top: " + ", ".join(sorted(os.listdir("app-merged"))[:20]))
    write_summary("STEP2")

    # 3) JDK
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

    # 4) gradle
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

    # 5) build
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
    post_issue("MAD Build CRASHED (uncaught exception)")
    push_report("CRASH")
    sys.exit(1)
