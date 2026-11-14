#!/usr/bin/env bash
# MAD build orchestrator v3 — sab kuch yahin se, aur fail hone par
# khud GitHub issue me poora error report daal deta hai.
set -u

REPO_URL="https://github.com/sheokandsushil02-lgtm/Mind-clone"
LOG="/tmp/mad_build.log"

# ---------- helpers ----------
report_fail() {
  local msg="$1"
  echo "MAD-BUILD-FAIL: $msg"
  local token="${GITHUB_TOKEN:-}"
  local repo="${GITHUB_REPOSITORY:-sheokandsushil02-lgtm/Mind-clone}"
  if [ -n "$token" ]; then
    local body
    body=$(printf 'MAD build fail: %s\n\n--- last 100 log lines ---\n```\n%s\n```\n' "$msg" "$(tail -n 100 "$LOG" 2>/dev/null)")
    local payload
    payload=$(python3 - "$body" <<'PYEOF'
import json, sys
print(json.dumps({"title": "MAD Build Failed - error report", "body": sys.argv[1][:25000]}))
PYEOF
)
    curl -s -X POST \
      -H "Authorization: Bearer $token" \
      -H "Accept: application/vnd.github+json" \
      -d "$payload" \
      "https://api.github.com/repos/$repo/issues" > /dev/null 2>&1 || true
  fi
  exit 1
}

run_logged() {
  "$@" >> "$LOG" 2>&1
}

# ---------- 0) syntax check ----------
: > "$LOG"
run_logged python3 -m py_compile overlay/patch_home.py overlay/patch_manifest.py \
  || report_fail "Python script me syntax error hai (log me traceback hai)"

# ---------- 1) ZIP laao ----------
if [ ! -f madgitfileforcloud.zip ]; then
  git fetch origin main --depth=1 >> "$LOG" 2>&1 || true
  git show origin/main:madgitfileforcloud.zip > madgitfileforcloud.zip 2>>"$LOG" || rm -f madgitfileforcloud.zip
fi
if [ ! -s madgitfileforcloud.zip ]; then
  curl -sSL --retry 3 -o madgitfileforcloud.zip "$REPO_URL/raw/main/madgitfileforcloud.zip" >> "$LOG" 2>&1 \
    || report_fail "ZIP download nahi hui"
fi
[ -s madgitfileforcloud.zip ] || report_fail "ZIP khaali/gawayab hai"
echo "ZIP size: $(du -h madgitfileforcloud.zip | cut -f1)" >> "$LOG"

# ---------- 2) extract + patch ----------
run_logged python3 overlay/patch_home.py app-merged \
  || report_fail "patch_home.py fail hua (log me MAD-PATCH lines + traceback hai)"
run_logged python3 overlay/patch_manifest.py app-merged \
  || echo "WARN: patch_manifest.py fail (jaari)" >> "$LOG"
[ -f app-merged/settings.gradle.kts ] || report_fail "app-merged me settings.gradle.kts nahi mila"
[ -f app-merged/app/build.gradle.kts ] || report_fail "app-merged me app/build.gradle.kts nahi mila"
echo "--- app-merged top ---" >> "$LOG"
ls app-merged >> "$LOG" 2>&1 || true

# ---------- 3) JDK ----------
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/temurin-17-jdk-amd64}"
if [ ! -d "$JAVA_HOME" ]; then
  JAVA_HOME="$(ls -d /usr/lib/jvm/*17* 2>/dev/null | head -1 || true)"
fi
[ -n "$JAVA_HOME" ] && [ -d "$JAVA_HOME" ] || report_fail "JDK 17 nahi mila"
export PATH="$JAVA_HOME/bin:$PATH"
run_logged java -version || report_fail "java nahi chala"

# ---------- 4) gradle ----------
GRADLE_VER="9.3.1"
if ! command -v gradle >/dev/null 2>&1; then
  curl -sSL -o /tmp/gradle.zip "https://services.gradle.org/distributions/gradle-$GRADLE_VER-bin.zip" >> "$LOG" 2>&1 \
    || report_fail "gradle zip download fail"
  run_logged unzip -q /tmp/gradle.zip -d /opt || report_fail "gradle unzip fail"
fi
GRADLE_BIN="$(command -v gradle || echo /opt/gradle-$GRADLE_VER/bin/gradle)"
[ -x "$GRADLE_BIN" ] || report_fail "gradle binary nahi mili"
run_logged "$GRADLE_BIN" --version || report_fail "gradle nahi chala"

# ---------- 5) build ----------
cd app-merged || report_fail "app-merged me entry fail"
run_logged "$GRADLE_BIN" assembleDebug --no-daemon --stacktrace \
  || report_fail "GRADLE BUILD FAIL (log me poora error hai)"
APK="app/build/outputs/apk/debug/app-debug.apk"
[ -f "$APK" ] || report_fail "APK file nahi bani: $APK"
echo "MAD-APK-READY: $(du -h "$APK" | cut -f1)"
