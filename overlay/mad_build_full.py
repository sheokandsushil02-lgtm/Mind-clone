#!/usr/bin/env python3
"""MAD build orchestrator v9 - extraction FIX + 3 report channels.

v9 KEY FIX: ZIP extract + nested-zip unpack + project-root finding ab
yahin hota hai (patch_home me nahi). Pichhli baar app-merged me sirf
overlay chala gaya tha kyunki patch_home ka extractor root miss kar raha tha.
Har step announce() se public annotation chhapta hai (login ke bina dikhta hai).
"""
import base64
import glob
import json
import os
import shutil
import subprocess
import sys
import tempfile
import traceback
import urllib.request
import zipfile

ZIP_NAME = "madgitfileforcloud.zip"
RAW_URL = "https://github.com/sheokandsushil02-lgtm/Mind-clone/raw/main/" + ZIP_NAME
LOG = "/tmp/mad_build.log"
SUMMARY = os.environ.get("GITHUB_STEP_SUMMARY", "")
LINES = []
SKIP = {"build", ".gradle", ".idea", "intermediates", "__pycache__", "node_modules",
        ".git", ".aistudio"}


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


def get_token():
    t = os.environ.get("GITHUB_TOKEN", "")
    if t:
        return t
    try:
        p = subprocess.run(["git", "config", "--get-all",
                            "http.https://github.com/.extraheader"],
                           capture_output=True, text=True)
        for line in (p.stdout or "").splitlines():
            if "basic" in line:
                b64 = line.split()[-1]
                dec = base64.b64decode(b64).decode("utf-8", "replace")
                if ":" in dec:
                    return dec.split(":", 1)[1]
    except Exception:
        pass
    return ""


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
                  ["git", "checkout", "--orphan", "mad-report"],
                  ["git", "add", "-f", "MAD_REPORT.md", "mad_build.log"],
                  ["git", "commit", "-m", "MAD report " + tag],
                  ["git", "push", "origin", "mad-report"]):
            p = subprocess.run(c, cwd=ws, capture_output=True, text=True, timeout=180)
            if p.returncode != 0 and c[:2] == ["git", "push"]:
                log("push fail: " + (p.stderr or "")[:200])
                return
        log("REPORT PUSHED to mad-report branch")
    except Exception as e:
        log("push_report EXC: " + repr(e)[:150])


def post_issue(subject):
    token = get_token()
    repo = os.environ.get("GITHUB_REPOSITORY", "sheokandsushil02-lgtm/Mind-clone")
    if not token:
        log("issue skip: token nahi mila (env ya git config dono khali)")
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
        log("ISSUE #" + str(d.get("number")) + " ban gaya")
    except Exception as e:
        log("issue post fail: " + repr(e)[:200])


def die(msg):
    log("BUILD-FAIL: " + msg)
    announce("BUILD-FAIL: " + msg)
    key = [l for l in LINES if any(k in l for k in
          ("Error", "error", "fail", "FAIL", "nahi", "NAHI", "Syntax", "Exception", "root"))]
    for k in key[-10:]:
        announce(k[:400])
    write_summary("FAIL")
    post_issue("MAD Build Failed: " + msg)
    push_report("FAIL")
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


def find_roots(base):
    strong, weak = [], []
    for dirpath, dirnames, filenames in os.walk(base):
        dirnames[:] = [d for d in dirnames if d not in SKIP]
        has_manifest = os.path.isfile(os.path.join(dirpath, "app", "src", "main",
                                                  "AndroidManifest.xml"))
        has_settings = ("settings.gradle.kts" in filenames) or ("settings.gradle" in filenames)
        if has_settings and has_manifest:
            strong.append(dirpath)
        elif has_manifest:
            weak.append(dirpath)
    return strong, weak


def extract_all(zip_path, dst):
    os.makedirs(dst, exist_ok=True)
    with zipfile.ZipFile(zip_path) as z:
        z.extractall(dst)
    nested = 0
    for _depth in range(5):
        inner = []
        for dirpath, dirnames, filenames in os.walk(dst):
            dirnames[:] = [d for d in dirnames if d not in SKIP]
            for fn in filenames:
                if fn.lower().endswith(".zip"):
                    inner.append(os.path.join(dirpath, fn))
        if not inner:
            break
        for iz in inner:
            try:
                outd = iz[:-4] + "_unzipped"
                with zipfile.ZipFile(iz) as z:
                    z.extractall(outd)
                os.remove(iz)
                nested += 1
                announce("nested ZIP khola: " + os.path.basename(iz)[:150])
            except Exception as e:
                log("nested ZIP fail " + os.path.basename(iz) + ": " + repr(e)[:150])
    return nested


def do_extraction():
    """ZIP laao, sab kuch kholo, asli root dhoondo, app-merged banao."""
    if not os.path.isfile(ZIP_NAME) or os.path.getsize(ZIP_NAME) < 100000:
        run(["git", "fetch", "origin", "main", "--depth=1"], quiet=True)
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
        die("ZIP nahi mili (git + raw url dono fail)")
    log("ZIP size: " + str(os.path.getsize(ZIP_NAME) // (1024 * 1024)) + " MB")

    tmp = tempfile.mkdtemp(prefix="madex_")
    ex = os.path.join(tmp, "ex")
    try:
        with zipfile.ZipFile(ZIP_NAME) as z:
            names = z.namelist()
        announce("ZIP entries: " + str(len(names)))
        shown = 0
        for n in names:
            if shown < 20:
                announce("Z: " + n[:200])
                shown += 1
        nested = extract_all(ZIP_NAME, ex)
        announce("nested zips khole: " + str(nested))

        strong, weak = find_roots(ex)
        announce("strong roots: " + str(len(strong)) + " | weak roots: " + str(len(weak)))
        for r in strong[:4]:
            announce("STRONG: " + os.path.relpath(r, ex)[:200])
        for r in weak[:4]:
            announce("WEAK: " + os.path.relpath(r, ex)[:200])

        root = None
        if strong:
            root = min(strong, key=lambda p: (p.count(os.sep), len(p)))
        elif weak:
            root = min(weak, key=lambda p: (p.count(os.sep), len(p)))
        if not root:
            count = 0
            for dirpath, dirnames, filenames in os.walk(ex):
                dirnames[:] = [d for d in dirnames if d not in SKIP]
                announce("D " + os.path.relpath(dirpath, ex)[:120] + " :: " +
                         ", ".join(sorted(filenames)[:10])[:150])
                count += 1
                if count > 40:
                    break
            die("project root nahi mila (structure upar annotations me hai)")

        announce("ROOT chuna: " + os.path.relpath(root, ex)[:200])
        if os.path.isdir("app-merged"):
            shutil.rmtree("app-merged", ignore_errors=True)
        ignore = shutil.ignore_patterns("build", ".gradle", ".idea", "__pycache__")
        shutil.copytree(root, "app-merged", dirs_exist_ok=True, ignore=ignore)
        announce("app-merged top: " + ", ".join(sorted(os.listdir("app-merged"))[:25])[:250])
    except Exception:
        announce("EXTRACT CRASH: " + traceback.format_exc(limit=3)[-400:])
        die("extraction crash")
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


def main():
    log("SCRIPT-START v9")
    open(LOG, "w").close()

    for s in ("overlay/patch_home.py", "overlay/patch_manifest.py"):
        if run([sys.executable, "-m", "py_compile", s], quiet=True) != 0:
            die(s + " syntax error")
    log("patch scripts compile OK")

    # KEY FIX: extraction yahin, patch_home se pehle
    do_extraction()

    if run([sys.executable, "overlay/patch_home.py", "app-merged"]) != 0:
        die("patch_home.py fail")
    if run([sys.executable, "overlay/patch_manifest.py", "app-merged"]) != 0:
        log("WARN: patch_manifest fail, jaari")
    for rel in ("settings.gradle.kts", "app/build.gradle.kts",
                "app/src/main/AndroidManifest.xml"):
        if not os.path.isfile(os.path.join("app-merged", rel)):
            die("app-merged/" + rel + " nahi mila")
    announce("MERGE+PATCH OK")
    write_summary("STEP3")

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
    log("JAVA_HOME: " + jh)

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

    os.chdir("app-merged")
    if run([gbin, "assembleDebug", "--no-daemon", "--stacktrace"]) != 0:
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
        die("GRADLE BUILD FAIL (errors upar annotations me)")
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
    post_issue("MAD Build CRASHED (uncaught exception)")
    push_report("CRASH")
    sys.exit(1)
