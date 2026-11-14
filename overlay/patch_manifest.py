#!/usr/bin/env python3
"""MAD manifest patch v4 — RECORD_AUDIO permission + MadActivity register.
argv[1] = project root YA manifest path.
v4: kabhi exit 1 nahi — manifest na mile to warning ke saath exit 0,
taaki pipeline aage gradle tak pahunche (asli error wahan saaf dikhega)."""
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
    print("MAD-PATCH-MANIFEST: WARNING: AndroidManifest.xml nahi mila — patch skip")
    sys.exit(0)

print("MAD-PATCH-MANIFEST: target = " + target)

try:
    src = io.open(target, encoding="utf-8").read()
except Exception:
    traceback.print_exc()
    sys.exit(0)

changed = []

try:
    if "RECORD_AUDIO" not in src:
        idx = src.find("<application")
        if idx == -1:
            print("MAD-PATCH-MANIFEST: WARNING: <application> tag nahi mila")
        else:
            perm = '<uses-permission android:name="android.permission.RECORD_AUDIO" />\n\n    '
            src = src[:idx] + perm + src[idx:]
            changed.append("RECORD_AUDIO permission")

    if "MadActivity" not in src:
        idx = src.find("</application>")
        if idx == -1:
            print("MAD-PATCH-MANIFEST: WARNING: </application> nahi mila")
        else:
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
except Exception:
    traceback.print_exc()

sys.exit(0)
