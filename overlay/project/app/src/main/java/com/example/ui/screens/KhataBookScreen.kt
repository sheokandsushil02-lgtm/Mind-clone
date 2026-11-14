package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.data.db.CustomerEntity
import com.example.ui.theme.*
import com.example.utils.JsonBackupManager
import com.example.utils.ShareAndPrintUtils
import com.example.viewmodel.CustomerSortOption
import com.example.viewmodel.RemsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataBookScreen(
    viewModel: RemsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customers by viewModel.allCustomers.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val selectedCustId by viewModel.selectedCustomerId.collectAsState()

    if (selectedCustId != null) {
        val activeCustomer = customers.firstOrNull { it.id == selectedCustId }
        if (activeCustomer != null) {
            CustomerDetailScreen(
                customer = activeCustomer,
                viewModel = viewModel,
                onBack = { viewModel.selectCustomer(null) }
            )
            return
        }
    }

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showPasteJsonDialog by remember { mutableStateOf(false) }
    var pastedJsonText by remember { mutableStateOf("") }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    var importStatusIsError by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val openKhataJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonString = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (jsonString.isBlank()) {
                    importStatusMessage = "चयनित फ़ाइल खाली है।"
                    importStatusIsError = true
                } else {
                    viewModel.importKhataBookJson(jsonString) { result ->
                        result.onSuccess { info ->
                            importStatusMessage = "सफलतापूर्वक आयात पूर्ण!\n\n${info.message}"
                            importStatusIsError = false
                        }.onFailure { err ->
                            importStatusMessage = "आयात विफल (Import Error):\n${err.message ?: "अमान्य फ़ाइल प्रारूप"}"
                            importStatusIsError = true
                        }
                    }
                }
            } catch (e: Exception) {
                importStatusMessage = "फ़ाइल पढ़ने में त्रुटि: ${e.message}"
                importStatusIsError = true
            }
        }
    }

    var totalDue = 0.0
    var totalAdvance = 0.0
    customers.forEach { cust ->
        val bal = viewModel.getCustomerBalance(cust.id, transactions)
        if (bal > 0) totalDue += bal
        if (bal < 0) totalAdvance += kotlin.math.abs(bal)
    }

    val filteredCustomers = remember(customers, transactions, viewModel.customerSearchQuery, viewModel.customerSortOption) {
        val q = viewModel.customerSearchQuery.trim().lowercase()
        val list = customers.filter {
            it.name.lowercase().contains(q) || it.mobile.contains(q) || it.address.lowercase().contains(q)
        }
        when (viewModel.customerSortOption) {
            CustomerSortOption.ALPHABETICAL -> list.sortedBy { it.name.lowercase() }
            CustomerSortOption.HIGHEST_DUE -> list.sortedByDescending { viewModel.getCustomerBalance(it.id, transactions) }
            CustomerSortOption.DUE_DATE -> list.sortedBy { it.paymentTime ?: "9999-99-99" }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = EditorialBg,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddCustomerDialog = true },
                containerColor = EditorialPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer")
                Spacer(modifier = Modifier.width(8.dp))
                Text("नया ग्राहक जोड़ें", fontWeight = FontWeight.SemiBold)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "खाता बुक Pro",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Normal,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                color = EditorialTextPrimary
                            )
                        )
                        Text(
                            text = "Rajat Electro-Mech Solutions Ledger",
                            style = MaterialTheme.typography.bodySmall.copy(color = EditorialTextSecondary)
                        )
                    }

                    IconButton(
                        onClick = { showBackupDialog = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(EditorialSurface)
                            .border(1.dp, EditorialBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = "Backup", tint = EditorialPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardKpiCard(
                        title = "${customers.size}",
                        subtitle = "कुल ग्राहक",
                        containerColor = EditorialLilac,
                        contentColor = EditorialTextOnLilac,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardKpiCard(
                        title = "₹${totalDue.toInt()}",
                        subtitle = "कुल बाकी",
                        containerColor = EditorialBlush,
                        contentColor = EditorialTextOnBlush,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardKpiCard(
                        title = "₹${totalAdvance.toInt()}",
                        subtitle = "कुल एडवांस",
                        containerColor = EditorialMint,
                        contentColor = EditorialTextOnMint,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = viewModel.customerSearchQuery,
                        onValueChange = { viewModel.customerSearchQuery = it },
                        placeholder = { Text("नाम या नंबर से खोजें...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = EditorialTextSecondary) },
                        trailingIcon = {
                            if (viewModel.customerSearchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.customerSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = EditorialTextSecondary)
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.weight(1f).testTag("customer_search_input"),
                        colors = outlinedFieldColors()
                    )

                    var sortMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { sortMenuExpanded = true },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(EditorialSurface)
                                .border(1.dp, EditorialBorder, RoundedCornerShape(16.dp))
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort", tint = EditorialPrimary)
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("अ से ज्ञ (Alphabetical)") },
                                leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null) },
                                onClick = {
                                    viewModel.customerSortOption = CustomerSortOption.ALPHABETICAL
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("ज्यादा बाकी पहले (Due High)") },
                                leadingIcon = { Icon(Icons.Default.TrendingDown, contentDescription = null) },
                                onClick = {
                                    viewModel.customerSortOption = CustomerSortOption.HIGHEST_DUE
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("पेमेंट तारीख (Payment Date)") },
                                leadingIcon = { Icon(Icons.Default.Event, contentDescription = null) },
                                onClick = {
                                    viewModel.customerSortOption = CustomerSortOption.DUE_DATE
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (filteredCustomers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = RemsNavyCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RemsBorder)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PersonSearch, contentDescription = "Not found", tint = RemsTextMuted, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (viewModel.customerSearchQuery.isNotBlank()) "कोई ग्राहक नहीं मिला" else "खाता बही खाली है। 'नया ग्राहक जोड़ें' बटन दबाएं।",
                                color = RemsTextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredCustomers, key = { it.id }) { customer ->
                    val balance = viewModel.getCustomerBalance(customer.id, transactions)
                    CustomerCard(
                        customer = customer,
                        balance = balance,
                        onCardClick = { viewModel.selectCustomer(customer.id) },
                        onCallClick = { ShareAndPrintUtils.dialPhoneNumber(context, customer.mobile) },
                        onWhatsAppClick = {
                            val msg = buildCustomerReminderMessage(customer.name, balance)
                            ShareAndPrintUtils.shareToWhatsApp(
                                context = context,
                                phoneNumber = customer.mobile,
                                message = msg,
                                customerName = customer.name,
                                documentType = "Payment Reminder"
                            )
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showAddCustomerDialog) {
        var newName by remember { mutableStateOf("") }
        var newMobile by remember { mutableStateOf("") }
        var newAddress by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            title = { Text("नया ग्राहक जोड़ें", color = RemsGold, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("ग्राहक का नाम *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words),
                        colors = outlinedFieldColors(),
                        modifier = Modifier.testTag("new_customer_name")
                    )
                    OutlinedTextField(
                        value = newMobile,
                        onValueChange = { newMobile = it },
                        label = { Text("मोबाइल नंबर") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = outlinedFieldColors(),
                        modifier = Modifier.testTag("new_customer_phone")
                    )
                    OutlinedTextField(
                        value = newAddress,
                        onValueChange = { newAddress = it },
                        label = { Text("पता / गाँव") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences),
                        colors = outlinedFieldColors(),
                        modifier = Modifier.testTag("new_customer_address")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.addCustomer(newName, newMobile, newAddress)
                            showAddCustomerDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RemsGold)
                ) {
                    Text("सेव करें", color = RemsNavyDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerDialog = false }) {
                    Text("रद्द करें", color = RemsTextSecondary)
                }
            }
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Backup, contentDescription = null, tint = EditorialPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("खाता बही बैकअप और डेटा रिस्टोर", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "खाता बही (Khata Book) का सम्पूर्ण डेटा JSON फ़ाइल में सुरक्षित (Export) करें या पहले से बैकअप किया डेटा आयात (Import) करें:",
                        color = EditorialTextSecondary,
                        fontSize = 12.sp
                    )

                    HorizontalDivider(color = EditorialBorderLight)

                    Button(
                        onClick = {
                            val jsonString = viewModel.exportKhataBookJson()
                            JsonBackupManager.exportAndShareJsonFile(
                                context = context,
                                fileName = "KhataBook_Backup.json",
                                jsonString = jsonString
                            )
                            showBackupDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EditorialPrimary)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export JSON (.json फ़ाइल डाउनलोड/शेयर)")
                    }

                    OutlinedButton(
                        onClick = {
                            val jsonString = viewModel.exportKhataBookJson()
                            clipboardManager.setText(AnnotatedString(jsonString))
                            Toast.makeText(context, "खाता बही JSON कोड क्लिपबोर्ड पर कॉपी हो गया!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = EditorialPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy JSON Code (क्लिपबोर्ड)", color = EditorialPrimary, fontSize = 12.sp)
                    }

                    HorizontalDivider(color = EditorialBorderLight)

                    Button(
                        onClick = {
                            showBackupDialog = false
                            try {
                                openKhataJsonLauncher.launch("application/json")
                            } catch (e: Exception) {
                                openKhataJsonLauncher.launch("*/*")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RemsBlue)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import JSON (.json फ़ाइल चुनें)")
                    }

                    OutlinedButton(
                        onClick = {
                            showBackupDialog = false
                            pastedJsonText = ""
                            showPasteJsonDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp), tint = RemsBlue)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste JSON Text (टेक्स्ट पेस्ट करें)", color = RemsNavyDark, fontSize = 12.sp)
                    }

                    HorizontalDivider(color = EditorialBorderLight)

                    OutlinedButton(
                        onClick = {
                            val text = buildFullKhataSummaryText(customers, transactions, viewModel)
                            ShareAndPrintUtils.createAndSharePdf(
                                context = context,
                                customerName = "All_Customers",
                                documentType = "Khata Book Summary",
                                contentText = text
                            )
                            showBackupDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = EditorialTextPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export PDF Summary (प्रिंट / PDF)", color = EditorialTextPrimary, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("बंद करें", color = EditorialPrimary)
                }
            }
        )
    }

    if (showPasteJsonDialog) {
        AlertDialog(
            onDismissRequest = { showPasteJsonDialog = false },
            title = { Text("खाता बही JSON पेस्ट करें", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("कृपया बैकअप किया गया JSON टेक्स्ट यहाँ पेस्ट करें:", fontSize = 12.sp, color = EditorialTextSecondary)
                    OutlinedTextField(
                        value = pastedJsonText,
                        onValueChange = { pastedJsonText = it },
                        placeholder = { Text("{\"fileType\":\"REMS_KHATA_BOOK_BACKUP\", ...}", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                        maxLines = 10
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val txt = pastedJsonText.trim()
                        if (txt.isBlank()) {
                            importStatusMessage = "कृपया JSON टेक्स्ट दर्ज करें।"
                            importStatusIsError = true
                        } else {
                            viewModel.importKhataBookJson(txt) { result ->
                                result.onSuccess { info ->
                                    importStatusMessage = "सफलतापूर्वक आयात पूर्ण!\n\n${info.message}"
                                    importStatusIsError = false
                                    showPasteJsonDialog = false
                                }.onFailure { err ->
                                    importStatusMessage = "आयात विफल (Import Error):\n${err.message ?: "अमान्य JSON प्रारूप"}"
                                    importStatusIsError = true
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EditorialPrimary)
                ) {
                    Text("Import करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteJsonDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    if (importStatusMessage != null) {
        AlertDialog(
            onDismissRequest = { importStatusMessage = null },
            icon = {
                Icon(
                    if (importStatusIsError) Icons.Default.ErrorOutline else Icons.Default.CheckCircleOutline,
                    contentDescription = null,
                    tint = if (importStatusIsError) RemsRed else RemsGreen,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    if (importStatusIsError) "आयात त्रुटि (Import Failed)" else "आयात सफल (Import Success)",
                    fontWeight = FontWeight.Bold,
                    color = if (importStatusIsError) RemsRed else EditorialTextPrimary
                )
            },
            text = {
                Text(importStatusMessage ?: "", fontSize = 13.sp, color = EditorialTextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = { importStatusMessage = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (importStatusIsError) RemsRed else EditorialPrimary
                    )
                ) {
                    Text("ठीक है")
                }
            }
        )
    }
}

@Composable
private fun DashboardKpiCard(
    title: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, EditorialBorderLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    fontSize = 16.sp
                ),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = contentColor.copy(alpha = 0.8f),
                    fontSize = 10.5.sp
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CustomerCard(
    customer: CustomerEntity,
    balance: Double,
    onCardClick: () -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit
) {
    val (statusColor, badgeBg, badgeText) = when {
        balance > 0 -> Triple(EditorialRed, EditorialBlush, "बाकी ₹${balance.toInt()}")
        balance < 0 -> Triple(EditorialBlue, EditorialLilac, "एडवांस ₹${kotlin.math.abs(balance).toInt()}")
        else -> Triple(EditorialGreen, EditorialMint, "भुगतान पूरा")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = EditorialSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, EditorialBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = EditorialTextPrimary,
                            fontSize = 16.sp
                        )
                    )
                    if (customer.mobile.isNotBlank()) {
                        Text(
                            text = customer.mobile,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = EditorialTextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                    if (customer.address.isNotBlank()) {
                        Text(
                            text = customer.address,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = EditorialTextMuted,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = badgeBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = badgeText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (customer.paymentTime != null && balance > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                PaymentReminderBadge(paymentTimeIso = customer.paymentTime)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (customer.mobile.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clickable(onClick = onCallClick),
                        shape = RoundedCornerShape(999.dp),
                        color = EditorialLavender.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EditorialBorderLight)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = EditorialPrimaryDark, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Call", color = EditorialPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clickable(onClick = onWhatsAppClick),
                        shape = RoundedCornerShape(999.dp),
                        color = EditorialMint.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EditorialBorderLight)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "WhatsApp", tint = EditorialGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp", color = EditorialGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentReminderBadge(paymentTimeIso: String) {
    var remainingTimeText by remember { mutableStateOf("") }
    var isOverdue by remember { mutableStateOf(false) }

    LaunchedEffect(paymentTimeIso) {
        while (true) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val target = sdf.parse(paymentTimeIso.replace("T", " "))?.time
                    ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(paymentTimeIso)?.time
                    ?: 0L
                val diff = target - System.currentTimeMillis()
                if (diff > 0) {
                    isOverdue = false
                    val days = diff / (1000 * 60 * 60 * 24)
                    val hours = (diff / (1000 * 60 * 60)) % 24
                    val mins = (diff / (1000 * 60)) % 60
                    val secs = (diff / 1000) % 60
                    remainingTimeText = if (days > 0) "${days}d ${hours}h" else String.format("%02d:%02d:%02d", hours, mins, secs)
                } else {
                    isOverdue = true
                    remainingTimeText = "समय पूरा (Overdue)"
                }
            } catch (_: Exception) {
                remainingTimeText = paymentTimeIso
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isOverdue) RemsRed.copy(alpha = 0.2f * alpha) else RemsAmber.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isOverdue) RemsRed else RemsAmber)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isOverdue) RemsRed else RemsAmber)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "⏱ $remainingTimeText",
                color = if (isOverdue) RemsRed else RemsGoldLight,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun buildCustomerReminderMessage(name: String, balance: Double): String {
    return """
        नमस्ते $name जी,

        Rajat Electro-Mech Solutions (REMS) की तरफ से सादर नमस्कार।

        आपके खाते की वर्तमान बाकी राशि: ₹${balance.toInt()} है।
        कृपया भुगतान निर्धारित समय पर करने की कृपा करें।

        संपर्क: 8685058336 (Sushil Sheokand)
        धन्यवाद!
    """.trimIndent()
}

private fun buildFullKhataSummaryText(
    customers: List<CustomerEntity>,
    transactions: List<com.example.data.db.TransactionEntity>,
    viewModel: RemsViewModel
): String {
    val sb = StringBuilder()
    sb.append("📒 *REMS KHATA BOOK SUMMARY*\n")
    sb.append("Rajat Electro-Mech Solutions • Uchana\n")
    sb.append("Date: ${ShareAndPrintUtils.getCurrentDateIndian()}\n")
    sb.append("----------------------------\n")

    customers.forEachIndexed { idx, cust ->
        val bal = viewModel.getCustomerBalance(cust.id, transactions)
        sb.append("${idx + 1}. ${cust.name}")
        if (cust.mobile.isNotBlank()) sb.append(" (${cust.mobile})")
        sb.append(" : ₹${bal.toInt()}\n")
    }

    sb.append("----------------------------\n")
    sb.append("Total Customers: ${customers.size}\n")
    return sb.toString()
}
