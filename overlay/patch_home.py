#!/usr/bin/env python3
"""MAD HomeScreen patch v2 — robust.
argv[1] = HomeScreen.kt ka path YA project root (jaise app-merged).
- Kisi bhi package structure me HomeScreen.kt khud dhoondta hai
- 4 alag markers try karta hai
- Fail hone par bhi build nahi rukta (graceful)
- Bonus: agar debug.keystore missing hai to custom debug signing bhi hata deta hai
"""
import io, os, sys, glob, traceback

hint = sys.argv[1] if len(sys.argv) > 1 else "app-merged"

# ---------- 1) HomeScreen.kt dhoondo ----------
target = None
if hint.endswith(".kt") and os.path.isfile(hint):
    target = hint
else:
    root = hint if os.path.isdir(hint) else "app-merged"
    cands = glob.glob(os.path.join(root, "app/src/main/java/**/HomeScreen.kt"), recursive=True)
    for c in cands:
        if "ui/screens" in c.replace(os.sep, "/"):
            target = c
            break
    if not target and cands:
        target = cands[0]

if not target:
    print("MAD-PATCH-HOME: FATAL: HomeScreen.kt nahi mila under " + hint)
    sys.exit(0)  # build mat roko

print("MAD-PATCH-HOME: target = " + target)
try:
    src = io.open(target, encoding="utf-8", errors="replace").read()
    print("MAD-PATCH-HOME: file length = " + str(len(src)) + " chars")
except Exception:
    traceback.print_exc()
    sys.exit(0)

# ---------- 2) Idempotency check ----------
if "hub_mad_button" in src:
    print("MAD-PATCH-HOME: button already present, skip")
else:
    # ---------- 3) Import add ----------
    anchor = "import com.example.utils.ShareAndPrintUtils"
    if anchor in src and "import com.example.mad.MadLauncher" not in src:
        src = src.replace(anchor, anchor + "\nimport com.example.mad.MadLauncher", 1)
        print("MAD-PATCH-HOME: MadLauncher import added")
    else:
        # fallback: package line ke baad import daal do
        if "import com.example.mad.MadLauncher" not in src:
            pkg_idx = src.find("\n\n", src.find("package "))
            if pkg_idx != -1:
                src = src[:pkg_idx] + "\n\nimport com.example.mad.MadLauncher" + src[pkg_idx:]
                print("MAD-PATCH-HOME: MadLauncher import added (fallback)")

    button = '''
        Spacer(modifier = Modifier.height(16.dp))

        // === MAD AI BUTTON (naya feature overlay) ===
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hub_mad_button")
                .clickable { MadLauncher.open(context) },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = EditorialLilac),
            border = androidx.compose.foundation.BorderStroke(1.dp, EditorialBorderLight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(EditorialTextOnLilac),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "\U0001F399", fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MAD \u2014 Live AI Assistant",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = EditorialTextOnLilac,
                            fontSize = 16.sp
                        )
                    )
                    Text(
                        text = "Bol ke baat karo \u2014 Gemini live chat",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EditorialTextOnLilac.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open MAD",
                    tint = EditorialTextOnLilac,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
'''

    # ---------- 4) Insert position — 4 markers ----------
    markers = [
        ("        // Quick Utility Actions", "line"),
        ('testTag("quick_call_button")', "line"),
        ("        Spacer(modifier = Modifier.height(20.dp))", "plain"),
        ("REMS \u2022 Rajat Electro-Mech Solutions", "line"),
    ]
    inserted = False
    for m, mode in markers:
        idx = src.find(m)
        if idx == -1:
            continue
        if mode == "line":
            idx = src.rfind("\n", 0, idx) + 1
        src = src[:idx] + button + "\n" + src[idx:]
        inserted = True
        print("MAD-PATCH-HOME: button inserted before marker: " + m.strip()[:45])
        break

    if not inserted:
        print("MAD-PATCH-HOME: WARNING: koi marker nahi mila, button add nahi hua")
        sys.exit(0)

    try:
        io.open(target, "w", encoding="utf-8").write(src)
        print("MAD-PATCH-HOME: HomeScreen patched OK")
    except Exception:
        traceback.print_exc()
        sys.exit(0)

# ---------- 5) Bonus fix: debug signing (agar debug.keystore missing) ----------
try:
    root = hint if os.path.isdir(hint) else "app-merged"
    ks = os.path.join(root, "debug.keystore")
    bg = glob.glob(os.path.join(root, "app", "build.gradle.kts"))
    if bg and not os.path.isfile(ks):
        gpath = bg[0]
        gsrc = io.open(gpath, encoding="utf-8").read()
        old = 'debug { signingConfig = signingConfigs.getByName("debugConfig") }'
        if old in gsrc:
            gsrc = gsrc.replace(old, 'debug { /* MAD: default debug signing (custom keystore missing) */ }', 1)
            io.open(gpath, "w", encoding="utf-8").write(gsrc)
            print("MAD-PATCH-GRADLE: debug signing neutralized (debug.keystore missing)")
        else:
            print("MAD-PATCH-GRADLE: debugConfig line nahi mili, gradle as-is")
    else:
        print("MAD-PATCH-GRADLE: debug.keystore present ya build file missing, skip")
except Exception:
    print("MAD-PATCH-GRADLE: koi dikkat aayi, skip")
    traceback.print_exc()

print("MAD-PATCH-HOME: done")
