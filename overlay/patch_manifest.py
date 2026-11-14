#!/usr/bin/env python3
"""MAD manifest patch v2 — RECORD_AUDIO permission + MadActivity register.
argv[1] = AndroidManifest.xml ka path YA project root.
Graceful: koi dikkat ho to bhi build nahi rukta (par exit 1 warna APK me mic nahi milega)."""
import io, os, sys, glob, traceback

hint = sys.argv[1] if len(sys.argv) > 1 else "app-merged"

target = None
if hint.endswith(".xml") and os.path.isfile(hint):
    target = hint
else:
    root = hint if os.path.isdir(hint) else "app-merged"
    cands = glob.glob(os.path.join(root, "app/src/main/AndroidManifest.xml"))
    if not cands:
        cands = glob.glob(os.path.join(root, "**/AndroidManifest.xml"), recursive=True)
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
    idx = src.find(">")
    # manifest tag ke closing > ke baad permission
    src = src[:idx + 1] + '\n    <uses-permission android:name="android.permission.RECORD_AUDIO" />' + src[idx + 1:]
    changed.append("RECORD_AUDIO permission")

if "MadActivity" not in src:
    activity_xml = (
        '        <activity\n'
        '            android:name="com.example.mad.MadActivity"\n'
        '            android:exported="false"\n'
        '            android:theme="@style/Theme.MyApplication" />\n'
    )
    pidx = src.find("<provider")
    if pidx == -1:
        pidx = src.find("</application>")
        insert_at = src.rfind("\n", 0, pidx) + 1 if pidx != -1 else len(src)
    else:
        insert_at = src.rfind("\n", 0, pidx) + 1
    src = src[:insert_at] + activity_xml + src[insert_at:]
    changed.append("MadActivity")

if changed:
    io.open(target, "w", encoding="utf-8").write(src)
    print("MAD-PATCH-MANIFEST: patched -> " + ", ".join(changed))
else:
    print("MAD-PATCH-MANIFEST: already patched, skip")
