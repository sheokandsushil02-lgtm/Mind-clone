package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mad.MadLauncher
import com.example.ui.components.RemsHeaderBanner
import com.example.ui.theme.*
import com.example.utils.ShareAndPrintUtils
import com.example.viewmodel.AppHub
import com.example.viewmodel.RemsViewModel

@Composable
fun HomeScreen(
    viewModel: RemsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customers by viewModel.allCustomers.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val bills by viewModel.allBills.collectAsState()
    val warranties by viewModel.allWarranties.collectAsState()

    // Calculate business metrics
    var totalDue = 0.0
    var totalAdvance = 0.0
    customers.forEach { cust ->
        val bal = viewModel.getCustomerBalance(cust.id, transactions)
        if (bal > 0) totalDue += bal
        if (bal < 0) totalAdvance += kotlin.math.abs(bal)
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorialBg)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // REMS Grand Header
        RemsHeaderBanner(showContactAction = true)

        Spacer(modifier = Modifier.height(18.dp))

        // Editorial Display Header with Italic Serif Accent
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Your business\n")
                    withStyle(SpanStyle(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal)) {
                        append("creative ")
                    }
                    append("hub.")
                },
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 32.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Light,
                    color = EditorialTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Rajat Electro-Mech Solutions • Uchana Management Suite",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = EditorialTextSecondary,
                    fontSize = 13.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Editorial Next-Up / KPI Highlight Card (Hero container)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = EditorialLavender),
            border = androidx.compose.foundation.BorderStroke(1.dp, EditorialBorderLight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LEDGER OVERVIEW",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EditorialTextOnLavender,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                fontSize = 10.sp
                            )
                        )

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = EditorialSurface.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EditorialBorder.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "LIVE METRICS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = EditorialPrimaryDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "₹${totalDue.toInt()}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = EditorialTextOnLavender,
                            fontSize = 32.sp
                        )
                    )

                    Text(
                        text = "कुल बाकी रकम • ${customers.size} सक्रिय ग्राहक खाते",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = EditorialTextOnLavender.copy(alpha = 0.8f),
                            fontSize = 12.5.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EditorialKpiChip(
                        label = "ग्राहक",
                        value = "${customers.size}",
                        containerColor = EditorialSurface,
                        contentColor = EditorialTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    EditorialKpiChip(
                        label = "बिल & वारंटी",
                        value = "${bills.size + warranties.size}",
                        containerColor = EditorialSurface,
                        contentColor = EditorialTextPrimary,
                        modifier = Modifier.weight(1.2f)
                    )
                    EditorialKpiChip(
                        label = "एडवांस",
                        value = "₹${totalAdvance.toInt()}",
                        containerColor = EditorialSurface,
                        contentColor = EditorialGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3 MAIN EDITORIAL CARDS (Item List, Khata Book, Bill Generate)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Task/Item List Card (Lilac)
            EditorialGridCard(
                title = "Item List",
                subtitle = "${viewModel.selectedOutputItems.size} चयनित",
                emoji = "📋",
                iconContainerColor = EditorialTextOnLilac,
                cardBgColor = EditorialLilac,
                textColor = EditorialTextOnLilac,
                testTag = "hub_item_list_button",
                modifier = Modifier.weight(1f),
                onClick = { viewModel.navigateToHub(AppHub.ITEM_LIST) }
            )

            // Insights / Khata Book Card (Blush)
            EditorialGridCard(
                title = "Khata Pro",
                subtitle = "₹${totalDue.toInt()} बाकी",
                emoji = "📒",
                iconContainerColor = EditorialTextOnBlush,
                cardBgColor = EditorialBlush,
                textColor = EditorialTextOnBlush,
                testTag = "hub_khata_book_button",
                modifier = Modifier.weight(1f),
                onClick = { viewModel.navigateToHub(AppHub.KHATA_BOOK) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Billing & Warranty (Mint / Soft Card)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hub_bill_generate_button")
                .clickable { viewModel.navigateToHub(AppHub.BILL_GENERATE) },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = EditorialMint),
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
                        .background(EditorialTextOnMint),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🧾", fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Billing & Warranty Suite",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = EditorialTextOnMint,
                            fontSize = 16.sp
                        )
                    )
                    Text(
                        text = "पक्का बिल, वारंटी प्रमाण-पत्र एवं इनवॉइस रिकॉर्ड्स",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EditorialTextOnMint.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open",
                    tint = EditorialTextOnMint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

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
                    Text(text = "🎙", fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MAD — Live AI Assistant",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = EditorialTextOnLilac,
                            fontSize = 16.sp
                        )
                    )
                    Text(
                        text = "Bol ke baat karo — Gemini live chat",
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

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Utility Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("quick_call_button")
                    .clickable { ShareAndPrintUtils.dialPhoneNumber(context, "8685058336") },
                shape = RoundedCornerShape(999.dp),
                color = EditorialSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, EditorialBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = EditorialPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call REMS", fontWeight = FontWeight.SemiBold, color = EditorialTextPrimary, fontSize = 13.sp)
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("quick_whatsapp_button")
                    .clickable {
                        ShareAndPrintUtils.shareToWhatsApp(
                            context = context,
                            phoneNumber = "8685058336",
                            message = "नमस्ते, मुझे REMS (Rajat Electro-Mech Solutions) की सेवाओं के बारे में जानकारी चाहिए।"
                        )
                    },
                shape = RoundedCornerShape(999.dp),
                color = EditorialSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, EditorialBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Share, contentDescription = "WhatsApp", tint = EditorialGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp", fontWeight = FontWeight.SemiBold, color = EditorialTextPrimary, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "REMS • Rajat Electro-Mech Solutions • Uchana",
            style = MaterialTheme.typography.bodySmall.copy(
                color = EditorialTextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun EditorialKpiChip(
    label: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, EditorialBorder.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    fontSize = 14.sp
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = EditorialTextSecondary,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
private fun EditorialGridCard(
    title: String,
    subtitle: String,
    emoji: String,
    iconContainerColor: Color,
    cardBgColor: Color,
    textColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, EditorialBorderLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 20.sp)
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        fontSize = 15.sp
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = textColor.copy(alpha = 0.75f),
                        fontSize = 11.5.sp
                    )
                )
            }
        }
    }
}
