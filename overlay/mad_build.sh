#!/usr/bin/env bash
# MAD build helper — ZIP khologa, root dhoondhega, overlay lagayega, patch karega.
# Ye script assistant (Ashna) update kar sakta hai — user ko workflow file baar-baar nahi badalni padegi.
set -uo pipefail

echo "=== [MAD-BUILD] Step 1: original ZIP nikalo ==="
git fetch origin main --depth=1
git show origin/main:madgitfileforcloud.zip > original-app.zip
ls -la original-app.zip

echo "=== [MAD-BUILD] Step 2: unzip ==="
rm -rf extracted app-merged
mkdir -p extracted
unzip -o -q original-app.zip -d extracted
echo "--- extracted (top 2 levels) ---"
find extracted -maxdepth 2 | head -60

echo "=== [MAD-BUILD] Step 3: nested ZIPs bhi khol do (agar hain) ==="
shopt -s globstar nullglob
for inner in extracted/**/*.zip; do
  echo "Nested zip mila: $inner — extracting"
  unzip -o -q "$inner" -d "$(dirname "$inner")/$(basename "$inner" .zip)-inner"
  rm -f "$inner"
done

echo "=== [MAD-BUILD] Step 4: project root dhoondo ==="
SETTING=$(find extracted -name settings.gradle.kts -print -quit)
if [ -z "$SETTING" ]; then
  SETTING=$(find extracted -name build.gradle.kts -print -quit)
fi
if [ -z "$SETTING" ]; then
  echo "FATAL: koi gradle project root nahi mila"
  find extracted -maxdepth 4 -type d | head -50
  exit 1
fi
ROOT=$(dirname "$SETTING")
echo "Project root: $ROOT"

echo "=== [MAD-BUILD] Step 5: project copy karo app-merged me ==="
mkdir -p app-merged
cp -a "$ROOT"/. app-merged/
ls app-merged | head -20

echo "=== [MAD-BUILD] Step 6: overlay MAD files ==="
cp -r overlay/app/. app-merged/app/
cp overlay/patch_home.py overlay/patch_manifest.py app-merged/
find app-merged/app/src/main/java/com/example/mad/ -type f

echo "=== [MAD-BUILD] Step 7: patches ==="
python3 app-merged/patch_home.py app-merged
python3 app-merged/patch_manifest.py app-merged

echo "=== [MAD-BUILD] Done — app-merged ready ==="
