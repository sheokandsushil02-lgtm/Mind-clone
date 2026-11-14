#!/usr/bin/env bash
# MAD launcher v11: source ZIP multi-route download, then orchestrator.
SRC=f8ed31f0-rems-business.zip
URL="https://d3p662obnq9uz2.cloudfront.net/chat-uploads/chat/user/6af7478f74ac87d004bc3005/6af747bc5ce1b36e2aabca96/f8ed31f0-rems-business.zip"
if [ -s "$SRC" ] && unzip -l "$SRC" >/dev/null 2>&1 && [ "$(unzip -l "$SRC" | tail -n 1 | awk '{print $2}')" -gt 10 ]; then
  echo "MAD: source ZIP present"
else
  UA="Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/128.0 Safari/537.36"
  Q=$(python3 -c "import urllib.parse;print(urllib.parse.quote('$URL',safe=''))")
  curl -sSL --max-time 240 -A "$UA" -H "Referer: https://ashna.ai/" -o "$SRC" "$URL" \
   || curl -sSL --max-time 240 -A "$UA" -o "$SRC" "$URL" \
   || curl -sSL --max-time 300 -o "$SRC" "https://api.allorigins.win/raw?url=$Q" \
   || curl -sSL --max-time 300 -o "$SRC" "https://api.codetabs.com/v1/proxy?quest=$Q" \
   || echo "MAD: all source download routes failed (orchestrator will try fallback)"
fi
ls -la "$SRC" 2>/dev/null || true
exec python3 overlay/mad_build_full.py #
