package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.CustomerEntity
import com.example.data.db.TransactionEntity
import com.example.ui.theme.*
import com.example.utils.ShareAndPrintUtils
import com.example.viewmodel.RemsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customer: CustomerEntity,
    viewModel: RemsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transactions by viewModel.currentCustomerTransactions.collectAsState()

    var workDesc by remember { mutableStateOf("") }
    var leneInput by remember { mutableStateOf("") }
    var aayeInput by remember { mutableStateOf("") }
    var discountInput by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("Cash") }

    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPaymentTimeDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    val totalBalance = transactions.sumOf { it.totalDelta }

    val balanceColor = when {
        totalBalance > 0 -> RemsRed
        totalBalance < 0 -> RemsBlue
        else -> RemsGreen
    }

    val balanceTitle = when {
        totalBalance > 0 -> "🔴 कुल बाकी: ₹${totalBalance.toInt()}"
        totalBalance < 0 -> "🔵 कुल एडवांस: ₹${kotlin.math.abs(totalBalance).toInt()}"
        else -> "🟢 भुगतान पूरा (खाता चुकता)"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = RemsNavyDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = customer.name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 17.sp
                        )
                        if (customer.mobile.isNotBlank()) {
                            Text(
                                text = customer.mobile,
                                color = RemsCyan,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (customer.mobile.isNotBlank()) {
                        IconButton(onClick = { ShareAndPrintUtils.dialPhoneNumber(context, customer.mobile) }) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = RemsCyan)
                        }
                    }
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename", tint = RemsGold)
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RemsRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RemsNavyCard)
            )
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
                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = RemsNavyCard),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, balanceColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = balanceTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = balanceColor,
                                fontSize = 18.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val text = buildCustomerLedgerText(customer, transactions, totalBalance)
                                    ShareAndPrintUtils.shareToWhatsApp(
                                        context = context,
                                        phoneNumber = customer.mobile,
                                        message = text,
                                        customerName = customer.name,
                                        documentType = "Payment Receipt"
                                    )
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RemsGreen)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp Share", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val text = buildCustomerLedgerText(customer, transactions, totalBalance)
                                    ShareAndPrintUtils.printCustomerDocument(
                                        context = context,
                                        customerName = customer.name,
                                        documentType = "Payment Receipt",
                                        contentText = text
                                    )
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RemsNavyLight)
                            ) {
                                Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Print Receipt", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D243D)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RemsBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏱ Payment Time",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = RemsGold
                                )
                            )

                            Button(
                                onClick = { showPaymentTimeDialog = true },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RemsAmber),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Time Set करें", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RemsNavyDark)
                            }
                        }

                        if (customer.paymentTime != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = RemsNavyCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, RemsGold.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "निर्धारित समय: ${customer.paymentTime}",
                                        color = RemsGoldLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Time दिया गया: ${customer.paymentTimeCount} बार",
                                        color = RemsTextSecondary,
                                        fontSize = 11.sp
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Button(
                                        onClick = { viewModel.closeCustomerPaymentTime(customer) },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = RemsGreen),
                                        modifier = Modifier.fillMaxWidth().height(34.dp)
                                    ) {
                                        Text("✓ Payment मिल गया — Time Close करें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "अभी कोई Payment Time तय नहीं है।",
                                color = RemsTextMuted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = RemsNavyCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RemsBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "नया लेन-देन दर्ज करें",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = RemsGold
                            )
                        )

                        OutlinedTextField(
                            value = workDesc,
                            onValueChange = { workDesc = it },
                            label = { Text("कार्य / विवरण (Work)", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth().testTag("transaction_work_input"),
                            colors = outlinedFieldColors()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = leneInput,
                                onValueChange = { leneInput = it },
                                label = { Text("लेने (Debit ₹)", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).testTag("transaction_lene_input"),
                                colors = outlinedFieldColors()
                            )
                            OutlinedTextField(
                                value = aayeInput,
                                onValueChange = { aayeInput = it },
                                label = { Text("आये (Credit ₹)", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).testTag("transaction_aaye_input"),
                                colors = outlinedFieldColors()
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = discountInput,
                                onValueChange = { discountInput = it },
                                label = { Text("Discount (₹)", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                colors = outlinedFieldColors()
                            )

                            var pmMenuExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = pmMenuExpanded,
                                onExpandedChange = { pmMenuExpanded = !pmMenuExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = paymentMode,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Payment Mode", fontSize = 11.sp) },
                                    modifier = Modifier.menuAnchor(),
                                    colors = outlinedFieldColors()
                                )
                                ExposedDropdownMenu(expanded = pmMenuExpanded, onDismissRequest = { pmMenuExpanded = false }) {
                                    listOf("Cash", "UPI", "Bank", "Cheque").forEach { mode ->
                                        DropdownMenuItem(
                                            text = { Text(mode) },
                                            onClick = {
                                                paymentMode = mode
                                                pmMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val l = leneInput.toDoubleOrNull() ?: 0.0
                                val a = aayeInput.toDoubleOrNull() ?: 0.0
                                val d = discountInput.toDoubleOrNull() ?: 0.0
                                if (workDesc.isNotBlank() || l > 0 || a > 0 || d > 0) {
                                    viewModel.addTransaction(
                                        customerId = customer.id,
                                        work = workDesc,
                                        lene = l,
                                        aaye = a,
                                        discount = d,
                                        paymentMode = paymentMode
                                    )
                                    workDesc = ""
                                    leneInput = ""
                                    aayeInput = ""
                                    discountInput = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("save_transaction_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RemsGold)
                        ) {
                            Text("💾 सेव करें", fontWeight = FontWeight.Bold, color = RemsNavyDark)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "खाता विवरण (${transactions.size})",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = RemsGold
                    )
                )
            }

            if (transactions.isEmpty()) {
                item {
                    Text(
                        text = "इस खाते में कोई लेन-देन दर्ज नहीं है।",
                        color = RemsTextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    )
                }
            } else {
                itemsIndexed(transactions, key = { _, tx -> tx.id }) { index, tx ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = RemsNavyLight),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RemsBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${tx.date}  •  ${tx.paymentMode}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = RemsCyan
                                )
                                Text(
                                    text = if (tx.totalDelta >= 0) "+₹${tx.totalDelta.toInt()}" else "-₹${kotlin.math.abs(tx.totalDelta).toInt()}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = if (tx.totalDelta >= 0) RemsRed else RemsGreen
                                )
                            }

                            if (tx.work.isNotBlank()) {
                                Text(
                                    text = tx.work,
                                    fontSize = 12.5.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "लेने: ₹${tx.lene.toInt()} | आये: ₹${tx.aaye.toInt()} | छूट: ₹${tx.discount.toInt()}",
                                    fontSize = 11.sp,
                                    color = RemsTextSecondary
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = RemsBlue,
                                        modifier = Modifier.clickable { transactionToEdit = tx }
                                    ) {
                                        Text("Edit", color = Color.White, fontSize = 9.5.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = RemsRed,
                                        modifier = Modifier.clickable { transactionToDelete = tx }
                                    ) {
                                        Text("Del", color = Color.White, fontSize = 9.5.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showRenameDialog) {
        var renamedName by remember { mutableStateOf(customer.name) }
        var renamedMobile by remember { mutableStateOf(customer.mobile) }
        var renamedAddress by remember { mutableStateOf(customer.address) }

        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("ग्राहक विवरण बदलें", color = RemsGold, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = renamedName,
                        onValueChange = { renamedName = it },
                        label = { Text("ग्राहक का नाम") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words),
                        colors = outlinedFieldColors()
                    )
                    OutlinedTextField(
                        value = renamedMobile,
                        onValueChange = { renamedMobile = it },
                        label = { Text("मोबाइल नंबर") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = outlinedFieldColors()
                    )
                    OutlinedTextField(
                        value = renamedAddress,
                        onValueChange = { renamedAddress = it },
                        label = { Text("पता") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences),
                        colors = outlinedFieldColors()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateCustomer(customer.copy(name = renamedName.trim(), mobile = renamedMobile.trim(), address = renamedAddress.trim()))
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RemsGold)
                ) {
                    Text("Save", color = RemsNavyDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = RemsTextSecondary)
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("खाता हटाएं?", color = RemsRed, fontWeight = FontWeight.Bold) },
            text = { Text("क्या आप सच में '${customer.name}' का पूरा खाता और सभी लेन-देन हटाना चाहते हैं?", color = Color.White) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(customer)
                        showDeleteConfirmDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RemsRed)
                ) {
                    Text("Delete Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = RemsTextSecondary)
                }
            }
        )
    }

    if (showPaymentTimeDialog) {
        var inputDateStr by remember { mutableStateOf(ShareAndPrintUtils.getCurrentDateIso()) }

        AlertDialog(
            onDismissRequest = { showPaymentTimeDialog = false },
            title = { Text("Payment Time तय करें", color = RemsGold, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("तारीख दर्ज करें (YYYY-MM-DD):", color = RemsTextSecondary, fontSize = 12.sp)
                    OutlinedTextField(
                        value = inputDateStr,
                        onValueChange = { inputDateStr = it },
                        placeholder = { Text("उदा. 2026-09-01") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        colors = outlinedFieldColors()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setCustomerPaymentTime(customer, inputDateStr.trim())
                        showPaymentTimeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RemsGold)
                ) {
                    Text("Set Time", color = RemsNavyDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentTimeDialog = false }) {
                    Text("Cancel", color = RemsTextSecondary)
                }
            }
        )
    }

    if (transactionToEdit != null) {
        val tx = transactionToEdit!!
        var eWork by remember { mutableStateOf(tx.work) }
        var eLene by remember { mutableStateOf(tx.lene.toString()) }
        var eAaye by remember { mutableStateOf(tx.aaye.toString()) }
        var eDisc by remember { mutableStateOf(tx.discount.toString()) }
        var eMode by remember { mutableStateOf(tx.paymentMode) }

        AlertDialog(
            onDismissRequest = { transactionToEdit = null },
            title = { Text("Edit Entry", color = RemsGold, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = eWork,
                        onValueChange = { eWork = it },
                        label = { Text("Work Description") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences),
                        colors = outlinedFieldColors()
                    )
                    OutlinedTextField(
                        value = eLene,
                        onValueChange = { eLene = it },
                        label = { Text("लेने (Debit ₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = outlinedFieldColors()
                    )
                    OutlinedTextField(
                        value = eAaye,
                        onValueChange = { eAaye = it },
                        label = { Text("आये (Credit ₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = outlinedFieldColors()
                    )
                    OutlinedTextField(
                        value = eDisc,
                        onValueChange = { eDisc = it },
                        label = { Text("Discount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = outlinedFieldColors()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val l = eLene.toDoubleOrNull() ?: 0.0
                        val a = eAaye.toDoubleOrNull() ?: 0.0
                        val d = eDisc.toDoubleOrNull() ?: 0.0
                        val delta = l - a - d
                        viewModel.updateTransaction(tx.copy(work = eWork.trim(), lene = l, aaye = a, discount = d, paymentMode = eMode, totalDelta = delta))
                        transactionToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RemsGold)
                ) {
                    Text("Save", color = RemsNavyDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToEdit = null }) {
                    Text("Cancel", color = RemsTextSecondary)
                }
            }
        )
    }

    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("एंट्री हटाएं?", color = RemsRed, fontWeight = FontWeight.Bold) },
            text = { Text("क्या आप इस लेन-देन को हटाना चाहते हैं?", color = Color.White) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(transactionToDelete!!)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RemsRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel", color = RemsTextSecondary)
                }
            }
        )
    }
}

private fun buildCustomerLedgerText(
    customer: CustomerEntity,
    transactions: List<TransactionEntity>,
    balance: Double
): String {
    val sb = StringBuilder()
    sb.append("📒 *REMS KHATA BOOK PRO*\n")
    sb.append("Rajat Electro-Mech Solutions • Uchana\n")
    sb.append("Prop.: Sushil Sheokand • 8685058336\n")
    sb.append("----------------------------\n")
    sb.append("ग्राहक: *${customer.name}*\n")
    if (customer.mobile.isNotBlank()) sb.append("मोबाइल: ${customer.mobile}\n")
    sb.append("----------------------------\n")

    transactions.forEach { tx ->
        sb.append("${tx.date} | ${tx.work.ifBlank { "कार्य" }} | लेने ₹${tx.lene.toInt()} | आये ₹${tx.aaye.toInt()} | कुल ₹${tx.totalDelta.toInt()}\n")
    }

    sb.append("----------------------------\n")
    val balWord = if (balance > 0) "बाकी राशि" else "एडवांस राशि"
    sb.append("*कुल $balWord : ₹${kotlin.math.abs(balance).toInt()}*\n")
    sb.append("कृपया भुगतान समय पर करें। धन्यवाद!\n")
    return sb.toString()
}
