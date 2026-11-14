#!/usr/bin/env python3
"""MAD build wrapper v2.

Bash script (mad_build.sh) ko CRLF line endings se saaf karke chalata hai.
GitHub web editor ya Windows se aayi files me \r aa jata hai jo bash ko
'$'\\r': command not found' dekar maar deta hai.
"""
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SH = os.path.join(HERE, "mad_build.sh")
CLEAN = "/tmp/mad_build_clean.sh"

try:
    with open(SH, "rb") as f:
        raw = f.read()
    with open(CLEAN, "wb") as f:
        f.write(raw.replace(b"\r\n", b"\n").replace(b"\r", b"\n"))
    os.chmod(CLEAN, 0o755)
except Exception as e:
    print("MAD-WRAPPER-FAIL: script saaf nahi hua:", e)
    sys.exit(1)

print("MAD-WRAPPER: mad_build.sh CRLF-cleaned, chala raha hoon...")
r = subprocess.run(["bash", CLEAN] + sys.argv[1:])
sys.exit(r.returncode)
