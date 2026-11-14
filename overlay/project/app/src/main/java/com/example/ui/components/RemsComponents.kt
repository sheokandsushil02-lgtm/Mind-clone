package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.utils.ShareAndPrintUtils
import kotlin.math.roundToInt

@Composable
fun RemsHeaderBanner(
    modifier: Modifier = Modifier,
    showContactAction: Boolean = true
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = EditorialLavender
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, EditorialBorderLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = EditorialPrimaryDark,
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Text(
                        text = "REMS BUSINESS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.5.sp,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = EditorialPrimaryLight.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "EST. 2024",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EditorialTextOnLavender,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "RAJAT ELECTRO-MECH SOLUTIONS",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-0.2).sp,
                    color = EditorialTextOnLavender,
                    fontSize = 20.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Prop.: Sushil Sheokand",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = EditorialTextOnLavender.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.5.sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = EditorialSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, EditorialBorder.copy(alpha = 0.5f)),
                modifier = Modifier
                    .clickable(enabled = showContactAction) {
                        if (showContactAction) {
                            ShareAndPrintUtils.dialPhoneNumber(context, "8685058336")
                        }
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Phone",
                        tint = EditorialPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Mob.: 8685058336",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EditorialPrimaryDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Saffa Kheri Road, Kharak Bhura, Uchana (Jind)",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = EditorialTextOnLavender.copy(alpha = 0.7f),
                    fontSize = 11.5.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Date: ${ShareAndPrintUtils.getCurrentDateIndian()}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = EditorialTextOnLavender.copy(alpha = 0.75f),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
fun RemsWatermarkOverlay(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .rotate(-28f)
        ) {
            Text(
                text = "REMS",
                fontSize = 58.sp,
                fontWeight = FontWeight.Light,
                color = EditorialPrimary.copy(alpha = 0.07f),
                letterSpacing = 4.sp,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "RAJAT ELECTRO-MECH SOLUTIONS",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = EditorialPrimary.copy(alpha = 0.07f),
                letterSpacing = 1.sp,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "Sushil Sheokand",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = EditorialPrimary.copy(alpha = 0.07f),
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

@Composable
fun RemsOfficialStamp(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(130.dp)
            .border(2.dp, RemsPaperStamp.copy(alpha = 0.55f), CircleShape)
            .padding(4.dp)
            .border(1.dp, RemsPaperStamp.copy(alpha = 0.4f), CircleShape)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "★ RAJAT ELECTRO-MECH ★",
                color = RemsPaperStamp.copy(alpha = 0.7f),
                fontSize = 6.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "REMS",
                color = RemsPaperStamp.copy(alpha = 0.8f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = "UCHANA (JIND)",
                color = RemsPaperStamp.copy(alpha = 0.7f),
                fontSize = 6.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "VERIFIED SEAL",
                color = RemsPaperStamp.copy(alpha = 0.6f),
                fontSize = 5.5.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun RemsFloatingNavigation(
    currentHub: com.example.viewmodel.AppHub,
    onHubSelected: (com.example.viewmodel.AppHub) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(999.dp),
        color = EditorialSurfaceContainer,
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, EditorialBorder.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorialNavItem(
                title = "Overview",
                emoji = "🏠",
                isSelected = currentHub == com.example.viewmodel.AppHub.HOME,
                onClick = { onHubSelected(com.example.viewmodel.AppHub.HOME) }
            )

            EditorialNavItem(
                title = "Item List",
                emoji = "📋",
                isSelected = currentHub == com.example.viewmodel.AppHub.ITEM_LIST,
                onClick = { onHubSelected(com.example.viewmodel.AppHub.ITEM_LIST) }
            )

            EditorialNavItem(
                title = "Khata Pro",
                emoji = "📒",
                isSelected = currentHub == com.example.viewmodel.AppHub.KHATA_BOOK,
                onClick = { onHubSelected(com.example.viewmodel.AppHub.KHATA_BOOK) }
            )

            EditorialNavItem(
                title = "Billing",
                emoji = "🧾",
                isSelected = currentHub == com.example.viewmodel.AppHub.BILL_GENERATE,
                onClick = { onHubSelected(com.example.viewmodel.AppHub.BILL_GENERATE) }
            )
        }
    }
}

@Composable
private fun EditorialNavItem(
    title: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = if (isSelected) EditorialLilac else Color.Transparent,
            modifier = Modifier
                .width(52.dp)
                .height(30.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = emoji,
                    fontSize = 16.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) EditorialTextPrimary else EditorialTextSecondary,
                fontSize = 10.5.sp
            )
        )
    }
}
