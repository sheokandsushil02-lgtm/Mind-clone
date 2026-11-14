#!/usr/bin/env python3
"""MAD feature: Manifest me RECORD_AUDIO permission + MadActivity register karta hai."""
import io, sys

path = sys.argv[1] if len(sys.argv) > 1 else "app/src/main/AndroidManifest.xml"
src = io.open(path, encoding="utf-8").read()

changed = False

# 1) Mic permission
if "RECORD_AUDIO" not in src:
    src = src.replace(
        'xmlns:tools="http://schemas.android.com/tools">',
        'xmlns:tools="http://schemas.android.com/tools">\n\n    <uses-permission android:name="android.permission.RECORD_AUDIO" />',
        1
    )
    changed = True

# 2) MadActivity register
if "MadActivity" not in src:
    activity_xml = (
        '        <activity\n'
        '            android:name="com.example.mad.MadActivity"\n'
        '            android:exported="false"\n'
        '            android:theme="@style/Theme.MyApplication" />'
    )
    src = src.replace(
        '        <provider',
        activity_xml + '\n\n        <provider',
        1
    )
    changed = True

if changed:
    io.open(path, "w", encoding="utf-8").write(src)
    print("Manifest patched (mic permission + MadActivity)")
else:
    print("Manifest already patched — skip")
