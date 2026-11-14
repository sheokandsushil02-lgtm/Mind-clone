#!/usr/bin/env bash
# MAD full build orchestrator — workflow se sirf ye ek script call hoti hai.
# Sab kuch yahin se: unzip/merge/patch -> JDK -> gradle -> APK -> report.
set -u

REPO_URL="https://github.com/sheokandsushil02-lgtm/Mind-clone"
BRANCH="app-source"

fail() { echo "MAD-BUILD-FAIL: $1"; exit 1; }

# ---------- 1) ZIP laao (git ya raw URL) ----------
if [ ! -f madgitfileforcloud.zip ]; then
  git fetch origin main --depth=1 || true
  git show origin/main:madgitfileforcloud.zip > madgitfileforcloud.zip 2>/dev/null || rm -f madgitfileforcloud.zip
fi
if [ ! -s madgitfileforcloud.zip ]; then
  curl -sSL --retry 3 -o madgitfileforcloud.zip "$REPO_URL/raw/main/madgitfileforcloud.zip" \
    || fail "ZIP download nahi hui"
fi
[ -s madgitfileforcloud.zip ] || fail "ZIP khaali/gawayab hai"
cp madgitfileforcloud.zip /tmp/madgitfileforcloud.zip

# ---------- 2) extract + patch ----------
python3 overlay/patch_home.py app-merged || fail "patch_home.py fail"
python3 overlay/patch_manifest.py app-merged || echo "WARN: patch_manifest.py ne fail diya (jaari)"
[ -f app-merged/settings.gradle.kts ] || fail "app-merged me settings.gradle.kts nahi mila"
[ -f app-merged/app/build.gradle.kts ] || fail "app-merged me app/build.gradle.kts nahi mila"

# ---------- 3) JDK ----------
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/temurin-17-jdk-amd64}"
[ -d "$JAVA_HOME" ] || export JAVA_HOME="$(ls -d /usr/lib/jvm/*17* 2>/dev/null | head -1 || true)"
if [ -z "${JAVA_HOME:-}" ] || [ ! -d "${JAVA_HOME:-/nonexistent}" ]; then
  fail "JDK 17 nahi mila (JAVA_HOME=$JAVA_HOME)"
fi
export PATH="$JAVA_HOME/bin:$PATH"
java -version || fail "java nahi chala"

# ---------- 4) gradle install (wrapper ke bina) ----------
GRADLE_VER="9.3.1"
GRADLE_HOME="/opt/gradle-$GRADLE_VER"
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  echo "Gradle $GRADLE_VER download kar raha hoon..."
  mkdir -p /opt
  curl -sSL -o /tmp/gradle.zip "https://services.gradle.org/distributions/gradle-$GRADLE_VER-bin.zip" \
    || fail "gradle zip download fail"
  unzip -q /tmp/gradle.zip -d /opt || fail "gradle unzip fail"
  rm -f /tmp/gradle.zip
fi
export PATH="$GRADLE_HOME/bin:$PATH"
gradle --version || fail "gradle nahi chala"

# ---------- 5) build ----------
cd app-merged || fail "app-merged me entry fail"
gradle assembleDebug --no-daemon --stacktrace || { echo "MAD-GRADLE-FAILED"; exit 1; }
APK="app/build/outputs/apk/debug/app-debug.apk"
[ -f "$APK" ] || fail "APK file nahi bani: $APK"
echo "MAD-APK-READY: $(du -h "$APK" | cut -f1)"

# ---------- 6) APK ki jagah batao (artifact upload workflow me ho sakta hai) ----------
echo "APK path: $PWD/$APK"
