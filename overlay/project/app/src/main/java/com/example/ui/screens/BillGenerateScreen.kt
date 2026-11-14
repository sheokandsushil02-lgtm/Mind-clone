package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.BillEntity
import com.example.data.db.SavedProductEntity
import com.example.data.model.BillItemRow
import com.example.ui.components.RemsOfficialStamp
import com.example.ui.components.RemsWatermarkOverlay
import com.example.ui.theme.*
import com.example.utils.HindiNumberToWords
import com.example.utils.JsonBackupManager
import com.example.utils.ShareAndPrintUtils
import com.example.viewmodel.RemsViewModel

enum class BillTab {
    NEW_BILL,
    WARRANTY_BILL,
    BILL_HISTORY,
    ITEM_DATABASE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillGenerateScreen(
    viewModel: RemsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(BillTab.NEW_BILL) }

    val bills by viewModel.allBills.collectAsState()
    val savedProducts by viewModel.allSavedProducts.collectAsState()
    val warranties by viewModel.allWarranties.collectAsState()

    var showDiscountBox by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showBillsBackupDialog by remember { mutableStateOf(false) }
    var showPasteBillsJsonDialog by remember { mutableStateOf(false) }
    var pastedBillsJsonText by remember { mutableStateOf("") }
    var billsImportStatusMessage by remember { mutableStateOf<String?>(null) }
    var billsImportStatusIsError by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val openBillsJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonString = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (jsonString.isBlank()) {
                    billsImportStatusMessage = "चयनित फ़ाइल खाली है।"
                    billsImportStatusIsError = true
                } else {
                    viewModel.importBillsJson(jsonString) { result ->
                        result.onSuccess { info ->
                            billsImportStatusMessage = "सफलतापूर्वक बिल आयात पूर्ण!\n\n${info.message}"
                            billsImportStatusIsError = false
                        }.onFailure { err ->
                            billsImportStatusMessage = "बिल आयात विफल (Import Error):\n${err.message ?: "अमान्य फ़ाइल प्रारूप"}"
                            billsImportStatusIsError = true
                        }
                    }
                }
            } catch (e: Exception) {
                billsImportStatusMessage = "फ़ाइल पढ़ने में त्रुटि: ${e.message}"
                billsImportStatusIsError = true
            }
        }
    }

    val totalAmount = viewModel.billItemRows.sumOf { it.amount }
    val discountVal = viewModel.currentBillDiscount.toDoubleOrNull() ?: 0.0
    val netAmount = kotlin.math.max(0.0, totalAmount - discountVal)
    val hindiWords = HindiNumberToWords.convert(netAmount)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RemsNavyDark)
    ) {
        // Tab Switcher
        ScrollableTabRow(
            selectedTabIndex = activeTab.ordinal,
            containerColor = RemsNavyCard,
            contentColor = RemsGold,
            edgePadding = 12.dp
        ) {
            Tab(
                selected = activeTab == BillTab.NEW_BILL,
                onClick = { activeTab = BillTab.NEW_BILL },
                text = { Text("🧾 नया बिल", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == BillTab.WARRANTY_BILL,
                onClick = {
                    viewModel.resetWarrantyForm()
                    activeTab = BillTab.WARRANTY_BILL
                },
                text = { Text("🛡️ वारंटी बिल", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == BillTab.BILL_HISTORY,
                onClick = { activeTab = BillTab.BILL_HISTORY },
                text = { Text("📜 बिल इतिहास (${bills.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == BillTab.ITEM_DATABASE,
                onClick = { activeTab = BillTab.ITEM_DATABASE },
                text = { Text("📦 आइटम डेटाबेस", fontWeight = FontWeight.Bold) }
            )
        }

        when (activeTab) {
            BillTab.NEW_BILL -> {
                NewBillTabContent(
                    viewModel = viewModel,
                    totalAmount = totalAmount,
                    discountVal = discountVal,
                    netAmount = netAmount,
                    hindiWords = hindiWords,
                    showDiscountBox = showDiscountBox,
                    onToggleDiscount = { showDiscountBox = !showDiscountBox },
                    onOpenWarrantyForThisBill = {
                        val currentBill = BillEntity(
                            billNo = viewModel.currentBillNo,
                            date = viewModel.currentBillDate,
                            customerName = viewModel.currentBillCustomerName,
                            customerPhone = viewModel.currentBillCustomerPhone,
                            customerAddress = viewModel.currentBillCustomerAddress,
                            discount = discountVal,
                            totalAmount = totalAmount,
                            netAmount = netAmount,
                            itemsJson = BillItemRow.toJson(viewModel.billItemRows)
                        )
                        viewModel.resetWarrantyForm(currentBill)
                        activeTab = BillTab.WARRANTY_BILL
                    }
                )
            }

            BillTab.WARRANTY_BILL -> {
                WarrantyBillTabContent(viewModel = viewModel)
            }

            BillTab.BILL_HISTORY -> {
                BillHistoryTabContent(
                    viewModel = viewModel,
                    onLoadBill = { bill ->
                        viewModel.loadBill(bill)
                        activeTab = BillTab.NEW_BILL
                    },
                    onOpenBackupDialog = { showBillsBackupDialog = true },
                    onExportJson = {
                        val jsonString = viewModel.exportBillsJson()
                        JsonBackupManager.exportAndShareJsonFile(
                            context = context,
                            fileName = "Bills_Backup.json",
                            jsonString = jsonString
                        )
                    },
                    onImportJson = {
                        try {
                            openBillsJsonLauncher.launch("application/json")
                        } catch (e: Exception) {
                            openBillsJsonLauncher.launch("*/*")
                        }
                    }
                )
            }

            BillTab.ITEM_DATABASE -> {
                ItemDatabaseTabContent(
                    viewModel = viewModel,
                    onAddProductClick = { showAddProductDialog = true },
                    onUseInBill = { product ->
                        viewModel.addBillRow(qty = "1", desc = product.name, rate = product.defaultRate.toString())
                        activeTab = BillTab.NEW_BILL
                    }
                )
            }
        }
    }

    if (showAddProductDialog) {
        var pName by remember { mutableStateOf("") }
        var pRate by remember { mutableStateOf("") }
        var pCat by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text("नया आइटम डेटाबेस में जोड़ें", color = RemsGold, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pName,
                        onValueChange = { pName = it },
                        label = { Text("Item Name *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words),
                        colors = outlinedFieldColors()
                    )
                    OutlinedTextField(
                        value = pRate,
                        onValueChange = { pRate = it },
                        label = { Text("Default Rate (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = outlinedFieldColors()
                    )
                    OutlinedTextField(
                        value = pCat,
                        onValueChange = { pCat = it },
                        label = { Text("Category (उदा. Electrical)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words),
                        colors = outlinedFieldColors()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val r = pRate.toDoubleOrNull() ?: 0.0
                        if (pName.isNotBlank()) {
                            viewModel.saveProduct(pName, r, pCat)
                            showAddProductDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RemsGold)
                ) {
                    Text("Save", color = RemsNavyDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) {
                    Text("Cancel", color = RemsTextSecondary)
                }
            }
        )
    }

    if (showBillsBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBillsBackupDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = RemsGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("बिल डेटा बैकअप और रिस्टोर", fontWeight = FontWeight.Bold, color = RemsGold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "सभी बिल (${bills.size}), सामग्री उत्पाद (${savedProducts.size}) और वारंटी रिकॉर्ड्स (${warranties.size}) को JSON में सुरक्षित (Export) करें या पूर्व बैकअप को आयात (Import) करें:",
                        color = RemsTextSecondary,
                        fontSize = 12.sp
                    )

                    HorizontalDivider(color = RemsBorder)

                    Button(
                        onClick = {
                            val jsonString = viewModel.exportBillsJson()
                            JsonBackupManager.exportAndShareJsonFile(
                                context = context,
                                fileName = "Bills_Backup.json",
                                jsonString = jsonString
                            )
                            showBillsBackupDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RemsGold)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = RemsNavyDark)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Bills JSON (.json फ़ाइल)", color = RemsNavyDark, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val jsonString = viewModel.exportBillsJson()
                            clipboardManager.setText(AnnotatedString(jsonString))
                            Toast.makeText(context, "बिल JSON कोड क्लिपबोर्ड पर कॉपी हो गया!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RemsGold)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = RemsGold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Bills JSON (क्लिपबोर्ड)", fontSize = 12.sp)
                    }

                    HorizontalDivider(color = RemsBorder)

                    Button(
                        onClick = {
                            showBillsBackupDialog = false
                            try {
                                openBillsJsonLauncher.launch("application/json")
                            } catch (e: Exception) {
                                openBillsJsonLauncher.launch("*/*")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RemsBlue)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import Bills JSON (.json फ़ाइल चुनें)")
                    }

                    OutlinedButton(
                        onClick = {
                            showBillsBackupDialog = false
                            pastedBillsJsonText = ""
                            showPasteBillsJsonDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp), tint = RemsBlue)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste Bills JSON (टेक्स्ट पेस्ट करें)", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBillsBackupDialog = false }) {
                    Text("बंद करें", color = RemsGold)
                }
            }
        )
    }

    if (showPasteBillsJsonDialog) {
        AlertDialog(
            onDismissRequest = { showPasteBillsJsonDialog = false },
            title = { Text("बिल JSON टेक्स्ट पेस्ट करें", color = RemsGold, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("कृपया बैकअप किया गया Bills JSON यहाँ पेस्ट करें:", fontSize = 12.sp, color = RemsTextSecondary)
                    OutlinedTextField(
                        value = pastedBillsJsonText,
                        onValueChange = { pastedBillsJsonText = it },
                        placeholder = { Text("{\"fileType\":\"REMS_BILLS_BACKUP\", ...}", fontSize = 11.sp, color = RemsTextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                        maxLines = 10,
                        colors = outlinedFieldColors()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val txt = pastedBillsJsonText.trim()
                        if (txt.isBlank()) {
                            billsImportStatusMessage = "कृपया JSON टेक्स्ट दर्ज करें।"
                            billsImportStatusIsError = true
                        } else {
                            viewModel.importBillsJson(txt) { result ->
                                result.onSuccess { info ->
                                    billsImportStatusMessage = "सफलतापूर्वक आयात पूर्ण!\n\n${info.message}"
                                    billsImportStatusIsError = false
                                    showPasteBillsJsonDialog = false
                                }.onFailure { err ->
                                    billsImportStatusMessage = "बिल आयात विफल (Import Error):\n${err.message ?: "अमान्य JSON प्रारूप"}"
                                    billsImportStatusIsError = true
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RemsGold)
                ) {
                    Text("Import करें", color = RemsNavyDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteBillsJsonDialog = false }) {
                    Text("रद्द करें", color = RemsTextSecondary)
                }
            }
        )
    }

    if (billsImportStatusMessage != null) {
        AlertDialog(
            onDismissRequest = { billsImportStatusMessage = null },
            icon = {
                Icon(
                    if (billsImportStatusIsError) Icons.Default.ErrorOutline else Icons.Default.CheckCircleOutline,
                    contentDescription = null,
                    tint = if (billsImportStatusIsError) RemsRed else RemsGreen,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    if (billsImportStatusIsError) "बिल आयात त्रुटि (Import Failed)" else "बिल आयात सफल (Import Success)",
                    fontWeight = FontWeight.Bold,
                    color = if (billsImportStatusIsError) RemsRed else RemsGold
                )
            },
            text = {
                Text(billsImportStatusMessage ?: "", fontSize = 13.sp, color = Color.White)
            },
            confirmButton = {
                Button(
                    onClick = { billsImportStatusMessage = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (billsImportStatusIsError) RemsRed else RemsGold
                    )
                ) {
                    Text("ठीक है", color = if (billsImportStatusIsError) Color.White else RemsNavyDark)
                }
            }
        )
    }
}

@Composable
private fun NewBillTabContent(
    viewModel: RemsViewModel,
    totalAmount: Double,
    discountVal: Double,
    netAmount: Double,
    hindiWords: String,
    showDiscountBox: Boolean,
    onToggleDiscount: () -> Unit,
    onOpenWarrantyForThisBill: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Bill Form Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = RemsNavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, RemsBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("बिल ग्राहक विवरण", color = RemsGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = viewModel.currentBillNo,
                        onValueChange = { viewModel.currentBillNo = it },
                        label = { Text("Bill No.") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.weight(1f).testTag("bill_no_input"),
                        colors = outlinedFieldColors()
                    )
                    OutlinedTextField(
                        value = viewModel.currentBillDate,
                        onValueChange = {
                            viewModel.currentBillDate = it
                            viewModel.currentBillNo = ShareAndPrintUtils.generateBillNumber(it, (viewModel.allBills.value.size + 1))
                        },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.weight(1f).testTag("bill_date_input"),
                        colors = outlinedFieldColors()
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = viewModel.currentBillCustomerName,
                        onValueChange = { viewModel.currentBillCustomerName = it },
                        label = { Text("ग्राहक का नाम") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.weight(1f).testTag("bill_customer_name"),
                        colors = outlinedFieldColors()
                    )
                    OutlinedTextField(
                        value = viewModel.currentBillCustomerPhone,
                        onValueChange = { viewModel.currentBillCustomerPhone = it },
                        label = { Text("मोबाइल") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f).testTag("bill_customer_phone"),
                        colors = outlinedFieldColors()
                    )
                }

                OutlinedTextField(
                    value = viewModel.currentBillCustomerAddress,
                    onValueChange = { viewModel.currentBillCustomerAddress = it },
                    label = { Text("पता / गाँव") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth().testTag("bill_customer_address"),
                    colors = outlinedFieldColors()
                )
            }
        }

        // Bill Items Table Editor
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = RemsNavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, RemsBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("सामान विवरण (Items)", color = RemsGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = onToggleDiscount,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RemsNavyLight),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("छूट (Discount)", fontSize = 10.sp, color = RemsGold)
                        }

                        Button(
                            onClick = { viewModel.addBillRow() },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RemsGreen),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("+ Row", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                AnimatedVisibility(visible = showDiscountBox) {
                    OutlinedTextField(
                        value = viewModel.currentBillDiscount,
                        onValueChange = { viewModel.currentBillDiscount = it },
                        label = { Text("Discount Amount (₹)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedFieldColors()
                    )
                }

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RemsNavyLight, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("मात्रा", color = RemsTextSecondary, fontSize = 11.sp, modifier = Modifier.width(55.dp), fontWeight = FontWeight.Bold)
                    Text("विवरण (Item)", color = RemsTextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text("दर (₹)", color = RemsTextSecondary, fontSize = 11.sp, modifier = Modifier.width(65.dp), fontWeight = FontWeight.Bold)
                    Text("रुपये", color = RemsTextSecondary, fontSize = 11.sp, modifier = Modifier.width(65.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    Spacer(modifier = Modifier.width(28.dp))
                }

                viewModel.billItemRows.forEachIndexed { index, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedTextField(
                            value = row.qty,
                            onValueChange = {
                                val idx = viewModel.billItemRows.indexOfFirst { it.id == row.id }
                                if (idx != -1) viewModel.billItemRows[idx] = viewModel.billItemRows[idx].copy(qty = it)
                            },
                            placeholder = { Text("Qty", fontSize = 10.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(55.dp),
                            colors = outlinedFieldColors()
                        )

                        OutlinedTextField(
                            value = row.description,
                            onValueChange = { newDesc ->
                                val idx = viewModel.billItemRows.indexOfFirst { it.id == row.id }
                                if (idx != -1) {
                                    val matchedProduct = viewModel.allSavedProducts.value.firstOrNull {
                                        it.name.equals(newDesc.trim(), ignoreCase = true)
                                    }
                                    val newRate = if (matchedProduct != null && row.rate.isBlank()) matchedProduct.defaultRate.toString() else row.rate
                                    viewModel.billItemRows[idx] = viewModel.billItemRows[idx].copy(description = newDesc, rate = newRate)
                                }
                            },
                            placeholder = { Text("Item", fontSize = 10.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.weight(1f),
                            colors = outlinedFieldColors()
                        )

                        OutlinedTextField(
                            value = row.rate,
                            onValueChange = {
                                val idx = viewModel.billItemRows.indexOfFirst { it.id == row.id }
                                if (idx != -1) viewModel.billItemRows[idx] = viewModel.billItemRows[idx].copy(rate = it)
                            },
                            placeholder = { Text("Rate", fontSize = 10.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(65.dp),
                            colors = outlinedFieldColors()
                        )

                        Text(
                            text = "₹${row.amount.toInt()}",
                            color = RemsGold,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(60.dp),
                            textAlign = TextAlign.End
                        )

                        IconButton(
                            onClick = { viewModel.removeBillRow(row.id) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = RemsRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    viewModel.saveCurrentBill()
                    val text = buildBillPlainText(viewModel, totalAmount, discountVal, netAmount, hindiWords)
                    ShareAndPrintUtils.createAndSharePdf(
                        context = context,
                        customerName = viewModel.currentBillCustomerName,
                        documentType = "Bill",
                        contentText = text
                    )
                },
                modifier = Modifier.weight(1f).height(44.dp).testTag("save_bill_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RemsBlue)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Bill", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    val text = buildBillPlainText(viewModel, totalAmount, discountVal, netAmount, hindiWords)
                    ShareAndPrintUtils.shareToWhatsApp(
                        context = context,
                        phoneNumber = viewModel.currentBillCustomerPhone,
                        message = text,
                        customerName = viewModel.currentBillCustomerName,
                        documentType = "Bill"
                    )
                },
                modifier = Modifier.weight(1f).height(44.dp).testTag("whatsapp_bill_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RemsGreen)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("WhatsApp", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = onOpenWarrantyForThisBill,
                modifier = Modifier.weight(1f).height(44.dp).testTag("warranty_bill_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RemsGold)
            ) {
                Text("वारंटी बनाएं", fontWeight = FontWeight.Black, color = RemsNavyDark, fontSize = 11.5.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // --- AUTHENTIC PARCHMENT PAPER INVOICE PREVIEW ---
        Text(
            text = "📄 बिल प्रीव्यू (Official Parchment Style)",
            style = MaterialTheme.typography.labelLarge.copy(
                color = RemsGold,
                fontWeight = FontWeight.Bold
            )
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(6.dp)),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = RemsPaperBg),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, RemsPaperBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 10% REMS Watermark in Background
                RemsWatermarkOverlay(modifier = Modifier.matchParentSize())

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Top line with Bill No and Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "क्रमांक: ${viewModel.currentBillNo.ifBlank { "ZB0001" }}",
                            fontWeight = FontWeight.Bold,
                            color = RemsPaperInk,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "दिनांक: ${ShareAndPrintUtils.formatDateToIndian(viewModel.currentBillDate)}",
                            fontWeight = FontWeight.Bold,
                            color = RemsPaperInk,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "॥ श्री गणेशाय नमः ॥",
                        color = RemsPaperStamp,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Rajat Electro-Mech Solutions",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = RemsPaperInk,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = RemsPaperStamp,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "Electrical • HVAC • Plumbing • Welding • Repair",
                            color = Color(0xFFF8E9BD),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "Saffa Kheri Road, Kharak Bhura, Uchana",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = RemsPaperInk,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Mob.: 8685058336",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = RemsPaperInk,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = RemsPaperBorder)

                    // Customer details
                    Text(
                        text = "श्रीमान: ${viewModel.currentBillCustomerName.ifBlank { "..................................................." }}",
                        fontSize = 13.sp,
                        color = RemsPaperInk,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "पता: ${viewModel.currentBillCustomerAddress.ifBlank { "..................................................." }}",
                        fontSize = 12.sp,
                        color = RemsPaperInk
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Invoice Grid Table
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, RemsPaperBorder)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(RemsPaperBorder.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("मात्रा", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RemsPaperInk, modifier = Modifier.width(45.dp))
                            Text("विवरण (Item)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RemsPaperInk, modifier = Modifier.weight(1f))
                            Text("दर", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RemsPaperInk, modifier = Modifier.width(55.dp), textAlign = TextAlign.End)
                            Text("रुपये", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RemsPaperInk, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
                        }

                        val nonBlankItems = viewModel.billItemRows.filter { it.description.isNotBlank() || it.qty.isNotBlank() }
                        val displayRows = if (nonBlankItems.isEmpty()) viewModel.billItemRows.take(4) else nonBlankItems

                        displayRows.forEach { row ->
                            HorizontalDivider(color = RemsPaperBorder.copy(alpha = 0.5f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(row.qty.ifBlank { "-" }, fontSize = 11.sp, color = RemsPaperInk, modifier = Modifier.width(45.dp))
                                Text(row.description.ifBlank { "-" }, fontSize = 11.sp, color = RemsPaperInk, modifier = Modifier.weight(1f))
                                Text(row.rate.ifBlank { "-" }, fontSize = 11.sp, color = RemsPaperInk, modifier = Modifier.width(55.dp), textAlign = TextAlign.End)
                                Text(if (row.amount > 0) "₹${row.amount.toInt()}" else "-", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RemsPaperInk, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Totals Table on Right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Column(
                            modifier = Modifier
                                .width(180.dp)
                                .border(1.dp, RemsPaperBorder)
                                .padding(6.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("कुल योग:", fontSize = 11.sp, color = RemsPaperInk)
                                Text("₹${totalAmount.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RemsPaperInk)
                            }
                            if (discountVal > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("छूट:", fontSize = 11.sp, color = RemsPaperInk)
                                    Text("-₹${discountVal.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RemsPaperInk)
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = RemsPaperBorder)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("शुद्ध राशि:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RemsPaperInk)
                                Text("₹${netAmount.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = RemsPaperStamp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "राशि शब्दों में: $hindiWords",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = RemsPaperInk
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Footer with received note, Official Stamp and Signature
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "सामान सही-सलामत प्राप्त हुआ।",
                            fontSize = 10.sp,
                            color = RemsPaperInk,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Stamp
                        RemsOfficialStamp(modifier = Modifier.size(90.dp))

                        // Signature
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Sushil Sheokand",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Cursive,
                                color = RemsPaperInk
                            )
                            HorizontalDivider(modifier = Modifier.width(90.dp), color = RemsPaperBorder)
                            Text(
                                text = "हस्ताक्षर / Auth. Sign",
                                fontSize = 9.sp,
                                color = RemsPaperInk
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun WarrantyBillTabContent(
    viewModel: RemsViewModel
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = RemsNavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, RemsBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🛡️ वारंटी विवरण दर्ज करें", color = RemsGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = viewModel.warrantyNo,
                        onValueChange = { viewModel.warrantyNo = it },
                        label = { Text("Warranty No.") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.weight(1f),
                        colors = outlinedFieldColors()
                    )
                    OutlinedTextField(
                        value = viewModel.warrantyOriginalBillNo,
                        onValueChange = { viewModel.warrantyOriginalBillNo = it },
                        label = { Text("Original Bill No.") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.weight(1f),
                        colors = outlinedFieldColors()
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = viewModel.warrantyCustomerName,
                        onValueChange = { viewModel.warrantyCustomerName = it },
                        label = { Text("Customer Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.weight(1f),
                        colors = outlinedFieldColors()
                    )
                    OutlinedTextField(
                        value = viewModel.warrantyCustomerPhone,
                        onValueChange = { viewModel.warrantyCustomerPhone = it },
                        label = { Text("Mobile") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f),
                        colors = outlinedFieldColors()
                    )
                }

                OutlinedTextField(
                    value = viewModel.warrantyItemName,
                    onValueChange = { viewModel.warrantyItemName = it },
                    label = { Text("वस्तु / Item / Product") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedFieldColors()
                )

                // Serial No with Generate Code Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.warrantySerialNumber,
                        onValueChange = { viewModel.warrantySerialNumber = it },
                        label = { Text("Model / Serial No.") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.weight(1f),
                        colors = outlinedFieldColors()
                    )

                    Button(
                        onClick = { viewModel.warrantySerialNumber = ShareAndPrintUtils.generateUniqueSerialCode() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RemsAmber),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("🎲 Generate", fontWeight = FontWeight.Bold, color = RemsNavyDark, fontSize = 11.5.sp)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = viewModel.warrantyPeriod,
                        onValueChange = { viewModel.warrantyPeriod = it },
                        label = { Text("अवधि (Period)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.weight(1f),
                        colors = outlinedFieldColors()
                    )
                    OutlinedTextField(
                        value = viewModel.warrantyTillDate,
                        onValueChange = { viewModel.warrantyTillDate = it },
                        label = { Text("वैध तारीख (Till Date)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.weight(1f),
                        colors = outlinedFieldColors()
                    )
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    viewModel.saveCurrentWarranty()
                    val text = buildWarrantyPlainText(viewModel)
                    ShareAndPrintUtils.shareToWhatsApp(
                        context = context,
                        phoneNumber = viewModel.warrantyCustomerPhone,
                        message = text,
                        customerName = viewModel.warrantyCustomerName,
                        documentType = "Warranty Certificate"
                    )
                },
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RemsGreen)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("WhatsApp Share", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    viewModel.saveCurrentWarranty()
                    val text = buildWarrantyPlainText(viewModel)
                    ShareAndPrintUtils.createAndSharePdf(
                        context = context,
                        customerName = viewModel.warrantyCustomerName,
                        documentType = "Warranty Certificate",
                        contentText = text
                    )
                },
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RemsBlue)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Certificate", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // --- AUTHENTIC WARRANTY CERTIFICATE PREVIEW ---
        Text(
            text = "🛡️ वारंटी प्रमाण-पत्र प्रीव्यू",
            style = MaterialTheme.typography.labelLarge.copy(color = RemsGold, fontWeight = FontWeight.Bold)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(6.dp)),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = RemsPaperBg),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, RemsPaperBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                RemsWatermarkOverlay(modifier = Modifier.matchParentSize())

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("वारंटी क्रमांक: ${viewModel.warrantyNo}", fontWeight = FontWeight.Bold, color = RemsPaperInk, fontSize = 12.sp)
                        Text("दिनांक: ${ShareAndPrintUtils.formatDateToIndian(viewModel.warrantyDate)}", fontWeight = FontWeight.Bold, color = RemsPaperInk, fontSize = 12.sp)
                    }

                    Text(
                        text = "॥ श्री गणेशाय नमः ॥",
                        color = RemsPaperStamp,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Rajat Electro-Mech Solutions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = RemsPaperInk,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Saffa Kheri Road, Kharak Bhura, Uchana • Mob: 8685058336",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RemsPaperInk,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "वारंटी प्रमाण-पत्र / WARRANTY BILL",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = RemsPaperStamp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Warranty Details Grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, RemsPaperBorder)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("ग्राहक: ", fontWeight = FontWeight.Bold, color = RemsPaperInk, fontSize = 11.5.sp)
                            Text(viewModel.warrantyCustomerName.ifBlank { "-" }, color = RemsPaperInk, fontSize = 11.5.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("मोबाइल: ", fontWeight = FontWeight.Bold, color = RemsPaperInk, fontSize = 11.5.sp)
                            Text(viewModel.warrantyCustomerPhone.ifBlank { "-" }, color = RemsPaperInk, fontSize = 11.5.sp)
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("मूल बिल नं: ", fontWeight = FontWeight.Bold, color = RemsPaperInk, fontSize = 11.5.sp)
                            Text(viewModel.warrantyOriginalBillNo.ifBlank { "-" }, color = RemsPaperInk, fontSize = 11.5.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("खरीद दिनांक: ", fontWeight = FontWeight.Bold, color = RemsPaperInk, fontSize = 11.5.sp)
                            Text(ShareAndPrintUtils.formatDateToIndian(viewModel.warrantyPurchaseDate), color = RemsPaperInk, fontSize = 11.5.sp)
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("वस्तु / Item: ", fontWeight = FontWeight.Bold, color = RemsPaperInk, fontSize = 11.5.sp)
                            Text(viewModel.warrantyItemName.ifBlank { "-" }, color = RemsPaperInk, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Model / Serial No: ", fontWeight = FontWeight.Bold, color = RemsPaperInk, fontSize = 11.5.sp)
                            Text(viewModel.warrantySerialNumber.ifBlank { "-" }, color = RemsPaperInk, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("वारंटी अवधि: ", fontWeight = FontWeight.Bold, color = RemsPaperInk, fontSize = 11.5.sp)
                            Text(viewModel.warrantyPeriod, color = RemsPaperInk, fontSize = 11.5.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("वैध दिनांक: ", fontWeight = FontWeight.Bold, color = RemsPaperInk, fontSize = 11.5.sp)
                            Text(ShareAndPrintUtils.formatDateToIndian(viewModel.warrantyTillDate), color = RemsPaperInk, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Warranty Notice Warning Box
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFFFF4E6),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RemsPaperStamp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "महत्वपूर्ण सूचना: यह Guarantee नहीं, Warranty है। Warranty अवधि के दौरान सामान खराब होने पर Repair करवाया जाएगा, बदला / replace नहीं किया जाएगा।",
                            color = Color(0xFF7A2117),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(6.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "शर्तें: " + viewModel.warrantyTerms,
                        fontSize = 8.5.sp,
                        color = RemsPaperInk,
                        lineHeight = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text("ग्राहक हस्ताक्षर: _________", fontSize = 9.5.sp, color = RemsPaperInk)
                        RemsOfficialStamp(modifier = Modifier.size(80.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Sushil Sheokand", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Cursive, color = RemsPaperInk)
                            Text("अधिकृत हस्ताक्षर (REMS)", fontSize = 8.5.sp, color = RemsPaperInk)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun BillHistoryTabContent(
    viewModel: RemsViewModel,
    onLoadBill: (BillEntity) -> Unit,
    onOpenBackupDialog: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit
) {
    val bills by viewModel.allBills.collectAsState()

    val filtered = remember(bills, viewModel.billSearchQuery) {
        val q = viewModel.billSearchQuery.trim().lowercase()
        if (q.isBlank()) bills else bills.filter {
            it.billNo.lowercase().contains(q) || it.customerName.lowercase().contains(q) || it.customerPhone.contains(q)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // TOP BACKUP & RESTORE TOOLBAR FOR BILLS
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = RemsNavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, RemsBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = RemsGold, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "कुल बिल: ${bills.size}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onExportJson,
                        colors = ButtonDefaults.buttonColors(containerColor = RemsGold),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(14.dp), tint = RemsNavyDark)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export JSON", color = RemsNavyDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Button(
                        onClick = onImportJson,
                        colors = ButtonDefaults.buttonColors(containerColor = RemsBlue),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import JSON", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    IconButton(
                        onClick = onOpenBackupDialog,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Backup Options", tint = RemsGold)
                    }
                }
            }
        }

        OutlinedTextField(
            value = viewModel.billSearchQuery,
            onValueChange = { viewModel.billSearchQuery = it },
            placeholder = { Text("Bill No., Customer या Mobile खोजें...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RemsTextSecondary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
            colors = outlinedFieldColors()
        )

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("कोई बिल नहीं मिला", color = RemsTextMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.billNo }) { bill ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = RemsNavyCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RemsBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${bill.billNo} • ${bill.customerName}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${ShareAndPrintUtils.formatDateToIndian(bill.date)} • ₹${bill.netAmount.toInt()}",
                                    color = RemsGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (bill.customerPhone.isNotBlank()) {
                                    Text(
                                        text = bill.customerPhone,
                                        color = RemsTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { onLoadBill(bill) },
                                    colors = ButtonDefaults.buttonColors(containerColor = RemsBlue),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Open", fontSize = 11.sp)
                                }

                                IconButton(
                                    onClick = { viewModel.deleteBill(bill.billNo) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RemsRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemDatabaseTabContent(
    viewModel: RemsViewModel,
    onAddProductClick: () -> Unit,
    onUseInBill: (SavedProductEntity) -> Unit
) {
    val products by viewModel.allSavedProducts.collectAsState()
    var search by remember { mutableStateOf("") }

    val filtered = remember(products, search) {
        val q = search.trim().lowercase()
        if (q.isBlank()) products else products.filter {
            it.name.lowercase().contains(q) || it.category.lowercase().contains(q)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("आइटम डेटाबेस (${products.size})", color = RemsGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)

            Button(
                onClick = onAddProductClick,
                colors = ButtonDefaults.buttonColors(containerColor = RemsGreen),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("नया आइटम", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("आइटम खोजें...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RemsTextSecondary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
            colors = outlinedFieldColors()
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filtered, key = { it.id }) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = RemsNavyCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RemsBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("दर: ₹${product.defaultRate.toInt()}  •  ${product.category}", color = RemsGoldLight, fontSize = 11.5.sp)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { onUseInBill(product) },
                                colors = ButtonDefaults.buttonColors(containerColor = RemsGold),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("बिल में जोड़ें", color = RemsNavyDark, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = { viewModel.deleteProduct(product.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RemsRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildBillPlainText(
    viewModel: RemsViewModel,
    total: Double,
    discount: Double,
    net: Double,
    hindiWords: String
): String {
    val sb = StringBuilder()
    sb.append("🧾 *RAJAT ELECTRO-MECH SOLUTIONS (REMS)*\n")
    sb.append("Saffa Kheri Road, Kharak Bhura, Uchana\n")
    sb.append("Mob.: 8685058336 (Sushil Sheokand)\n")
    sb.append("----------------------------\n")
    sb.append("बिल नं: *${viewModel.currentBillNo}* | दिनांक: ${ShareAndPrintUtils.formatDateToIndian(viewModel.currentBillDate)}\n")
    sb.append("ग्राहक: *${viewModel.currentBillCustomerName}*\n")
    if (viewModel.currentBillCustomerPhone.isNotBlank()) sb.append("मोबाइल: ${viewModel.currentBillCustomerPhone}\n")
    if (viewModel.currentBillCustomerAddress.isNotBlank()) sb.append("पता: ${viewModel.currentBillCustomerAddress}\n")
    sb.append("----------------------------\n")
    sb.append("*ITEMS / सामान:*\n")

    viewModel.billItemRows.filter { it.description.isNotBlank() }.forEachIndexed { idx, row ->
        sb.append("${idx + 1}. ${row.description} (${row.qty}) @ ₹${row.rate} = ₹${row.amount.toInt()}\n")
    }

    sb.append("----------------------------\n")
    sb.append("कुल योग: ₹${total.toInt()}\n")
    if (discount > 0) sb.append("छूट: -₹${discount.toInt()}\n")
    sb.append("*शुद्ध देय राशि: ₹${net.toInt()}*\n")
    sb.append("($hindiWords)\n")
    sb.append("----------------------------\n")
    sb.append("सामान सही-सलामत प्राप्त हुआ। धन्यवाद!")
    return sb.toString()
}

private fun buildWarrantyPlainText(viewModel: RemsViewModel): String {
    return """
        🛡️ *REMS वारंटी प्रमाण-पत्र*
        Rajat Electro-Mech Solutions • Uchana
        Prop: Sushil Sheokand • 8685058336
        ----------------------------
        वारंटी नं: *${viewModel.warrantyNo}*
        दिनांक: ${ShareAndPrintUtils.formatDateToIndian(viewModel.warrantyDate)}
        ग्राहक: *${viewModel.warrantyCustomerName}* (${viewModel.warrantyCustomerPhone})
        सामान: *${viewModel.warrantyItemName}*
        Model / Serial No.: *${viewModel.warrantySerialNumber}*
        वारंटी अवधि: ${viewModel.warrantyPeriod}
        वैध दिनांक: ${ShareAndPrintUtils.formatDateToIndian(viewModel.warrantyTillDate)}
        ----------------------------
        *सूचना:* यह Guarantee नहीं, Warranty है। खराबी होने पर Repair करवाया जाएगा।
        अधिकृत हस्ताक्षर (REMS)
    """.trimIndent()
}
