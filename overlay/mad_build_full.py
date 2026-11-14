#!/usr/bin/env python3
"""MAD build orchestrator v13 FINAL - repo ke overlay/project se seedha build.

- Source of truth: overlay/project/ (repo me committed asli source)
- ZIP/download ka koi bharosa nahi
- Gradle errors public annotations me chhapsein
"""
import base64
import glob
import json
import os
import shutil
import subprocess
import sys
import traceback
import urllib.request

SRC = "overlay/project"
LOG = "/tmp/mad_build.log"
SUMMARY = os.environ.get("GITHUB_STEP_SUMMARY", "")
LINES = []


def log(msg):
    line = str(msg)
    print("MAD: " + line, flush=True)
    LINES.append(line[:250])


def announce(msg):
    safe = str(msg).replace("\n", "%0A").replace("\r", "")
    if safe.strip():
        print("::error title=MAD-BUILD::" + safe[:500], flush=True)


def write_summary(tag):
    if not SUMMARY:
        return
    try:
        f = open(SUMMARY, "a", encoding="utf-8")
        f.write("\n## MAD Build Report [" + tag + "]\n```\n" + "\n".join(LINES) + "\n```\n")
        f.close()
    except Exception:
        pass


def die(msg):
    log("BUILD-FAIL: " + msg)
    announce("BUILD-FAIL: " + msg)
    key = [l for l in LINES if any(k in l for k in
          ("Error", "error", "fail", "FAIL", "not found", "Syntax", "Exception", "root", "e: "))]
    for k in key[-12:]:
        announce(k[:400])
    write_summary("FAIL")
    sys.exit(1)


def run(cmd, quiet=False):
    if not quiet:
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
            if len(LINES) < 150:
                LINES.append(ln.strip()[:250])
    return p.returncode


def main():
    log("SCRIPT-START v13")
    open(LOG, "w").close()

    # 0) syntax check of patch scripts
    for s in ("overlay/patch_home.py", "overlay/patch_manifest.py"):
        if run([sys.executable, "-m", "py_compile", s], quiet=True) != 0:
            die(s + " syntax error")
    log("patch scripts OK")

    # 1) project copy - overlay/project hai ya nahi
    if not os.path.isdir(SRC):
        die("overlay/project folder repo me nahi mila")
    if os.path.isdir("app-merged"):
        shutil.rmtree("app-merged", ignore_errors=True)
    shutil.copytree(SRC, "app-merged", dirs_exist_ok=True,
                    ignore=shutil.ignore_patterns("build", ".gradle"))
    n = sum(len(f) for _, _, f in os.walk("app-merged"))
    announce("project copy OK: " + str(n) + " files")
    for rel in ("settings.gradle.kts", "app/build.gradle.kts",
                "app/src/main/AndroidManifest.xml"):
        if not os.path.isfile(os.path.join("app-merged", rel)):
            die("app-merged/" + rel + " nahi mila")
    log("app-merged top: " + ", ".join(sorted(os.listdir("app-merged"))[:25]))

    # 2) patches
    if run([sys.executable, "overlay/patch_home.py", "app-merged"]) != 0:
        die("patch_home.py fail")
    if run([sys.executable, "overlay/patch_manifest.py", "app-merged"]) != 0:
        log("WARN: patch_manifest fail, jaari")
    if not os.path.isfile(os.path.join("app-merged", "app/src/main/AndroidManifest.xml")):
        die("manifest patch ke baad missing")
    announce("MERGE+PATCH OK")
    write_summary("STEP2")

    # 3) JDK
    jh = os.environ.get("JAVA_HOME", "")
    if not os.path.isdir(jh):
        c = sorted(glob.glob("/usr/lib/jvm/*17*") + glob.glob("/usr/lib/jvm/*temurin*"))
        jh = c[0] if c else ""
    if not jh or not os.path.isdir(jh):
        die("JDK 17 nahi mila")
    os.environ["JAVA_HOME"] = jh
    os.environ["PATH"] = jh + "/bin:" + os.environ.get("PATH", "")
    if run(["java", "-version"]) != 0:
        die("java nahi chala")

    # 4) gradle
    gbin = "/opt/gradle-9.3.1/bin/gradle"
    if not os.path.isfile(gbin):
        gz = "/tmp/gradle.zip"
        try:
            log("gradle download...")
            urllib.request.urlretrieve(
                "https://services.gradle.org/distributions/gradle-9.3.1-bin.zip", gz)
            run(["unzip", "-q", gz, "-d", "/opt"], quiet=True)
        except Exception as e:
            die("gradle download fail: " + repr(e)[:200])
    if not os.path.isfile(gbin):
        die("gradle binary nahi: " + gbin)
    if run([gbin, "--version"]) != 0:
        die("gradle nahi chala")

    # 5) build
    os.chdir("app-merged")
    rc = run([gbin, "assembleDebug", "--no-daemon", "--stacktrace"])
    if rc != 0:
        errs = [ln for ln in LINES if ("error" in ln.lower() or "FAILURE" in ln
                                       or ln.startswith("e: "))]
        shown = 0
        seen = set()
        for ln in errs:
            k = ln[:200]
            if k not in seen and shown < 15:
                seen.add(k)
                announce("GRADLE-ERR: " + ln[:400])
                shown += 1
        if shown == 0:
            for ln in LINES[-8:]:
                announce("LOG: " + ln[:400])
        die("GRADLE BUILD FAIL")

    apk = "app/build/outputs/apk/debug/app-debug.apk"
    if not os.path.isfile(apk):
        die("APK nahi bani: " + apk)
    mb = os.path.getsize(apk) // (1024 * 1024)
    log("APK-READY: " + str(mb) + " MB")
    announce("APK-READY: " + str(mb) + " MB")
    write_summary("SUCCESS")


try:
    main()
    log("DONE")
except SystemExit:
    raise
except BaseException:
    log("UNCAUGHT EXCEPTION:")
    log(traceback.format_exc()[:2000])
    announce("CRASH: " + traceback.format_exc()[-400:])
    write_summary("CRASH")
    sys.exit(1)
