#!/usr/bin/env python3
"""MAD feature: HomeScreen me MAD ka button add karta hai (build time patch).
Original source file repo me as-it-is rehti hai; sirf build ke waqt button juddta hai."""
import io, sys

path = sys.argv[1] if len(sys.argv) > 1 else "app/src/main/java/com/example/ui/screens/HomeScreen.kt"
src = io.open(path, encoding="utf-8").read()

if "hub_mad_button" in src:
    print("MAD button already present — skip")
    sys.exit(0)

# 1) import add karo
anchor = "import com.example.utils.ShareAndPrintUtils\n"
mad_import = "import com.example.mad.MadLauncher\n"
if anchor in src and mad_import not in src:
    src = src.replace(anchor, anchor + mad_import, 1)

# 2) MAD button — "Quick Utility Actions" wale Row se thehle pehle insert
marker = "        // Quick Utility Actions"
if marker not in src:
    marker = "        Spacer(modifier = Modifier.height(20.dp))"

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

idx = src.find(marker)
if idx == -1:
    print("ERROR: HomeScreen patch marker not found", file=sys.stderr)
    sys.exit(1)

src = src[:idx] + button + "\n" + src[idx:]
io.open(path, "w", encoding="utf-8").write(src)
print("HomeScreen patched with MAD button")
