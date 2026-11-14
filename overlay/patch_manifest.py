#!/usr/bin/env python3
"""MAD manifest patch v3 — RECORD_AUDIO permission + MadActivity register.
argv[1] = project root YA manifest path.
v3 fix: permission hamesha <application> se thehle pehle jaati hai (manifest ke andar),
pichle version me XML declaration ke baad galti se bahar chali jaati thi."""
import io, os, sys, glob, traceback

hint = sys.argv[1] if len(sys.argv) > 1 else "app-merged"

target = None
if hint.endswith(".xml") and os.path.isfile(hint):
    target = hint
else:
    root = hint if os.path.isdir(hint) else "app-merged"
    cands = glob.glob(os.path.join(root, "app/src/main/AndroidManifest.xml"))
    if not cands:
        cands = glob.glob(os.path.join(root, "**/src/main/AndroidManifest.xml"), recursive=True)
    if cands:
        target = cands[0]

if not target:
    print("MAD-PATCH-MANIFEST: FATAL: AndroidManifest.xml nahi mila")
    sys.exit(1)

print("MAD-PATCH-MANIFEST: target = " + target)

try:
    src = io.open(target, encoding="utf-8").read()
except Exception:
    traceback.print_exc()
    sys.exit(1)

changed = []

if "RECORD_AUDIO" not in src:
    idx = src.find("<application")
    if idx == -1:
        print("MAD-PATCH-MANIFEST: FATAL: <application> tag nahi mila")
        sys.exit(1)
    perm = '<uses-permission android:name="android.permission.RECORD_AUDIO" />\n\n    '
    src = src[:idx] + perm + src[idx:]
    changed.append("RECORD_AUDIO permission")

if "MadActivity" not in src:
    idx = src.find("</application>")
    if idx == -1:
        print("MAD-PATCH-MANIFEST: FATAL: </application> nahi mila")
        sys.exit(1)
    act = ('<activity android:name="com.example.mad.MadActivity" '
           'android:exported="false" '
           'android:theme="@style/Theme.MyApplication" />')
    src = src[:idx] + "        " + act + "\n    " + src[idx:]
    changed.append("MadActivity")

if changed:
    io.open(target, "w", encoding="utf-8").write(src)
    print("MAD-PATCH-MANIFEST: patched -> " + ", ".join(changed))
else:
    print("MAD-PATCH-MANIFEST: already patched, skip")
