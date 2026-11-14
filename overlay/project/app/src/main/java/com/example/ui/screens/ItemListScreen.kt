package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.CustomCategoryEntity
import com.example.data.db.CustomItemEntity
import com.example.data.model.SelectedItem
import com.example.ui.components.RemsHeaderBanner
import com.example.ui.theme.*
import com.example.utils.ShareAndPrintUtils
import com.example.viewmodel.RemsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemListScreen(
    viewModel: RemsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val workType by viewModel.selectedWorkType.collectAsState()
    val category by viewModel.selectedCategory.collectAsState()
    val itemName by viewModel.selectedItemName.collectAsState()
    val categories by viewModel.currentCategories.collectAsState()
    val itemsForCategory by viewModel.currentCategoryItems.collectAsState()

    var customerSectionExpanded by remember { mutableStateOf(false) }
    var selectorSectionExpanded by remember { mutableStateOf(true) }

    var showEditListsDialog by remember { mutableStateOf(false) }
    var showPosPreviewDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<SelectedItem?>(null) }
    var insertBelowTargetId by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RemsNavyDark)
            .padding(horizontal = 14.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        RemsHeaderBanner()

        Spacer(modifier = Modifier.height(12.dp))

        // --- 1. CUSTOMER / TECHNICIAN DETAILS COLLAPSIBLE ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = RemsNavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, RemsBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { customerSectionExpanded = !customerSectionExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "👤", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CUSTOMER / TECHNICIAN DETAILS",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = RemsGold,
                                fontSize = 13.sp
                            )
                        )
                    }
                    Icon(
                        imageVector = if (customerSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle",
                        tint = RemsGold
                    )
                }

                AnimatedVisibility(visible = customerSectionExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = viewModel.technicianName,
                                onValueChange = { viewModel.technicianName = it },
                                label = { Text("Technician Name", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f).testTag("tech_name_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words),
                                colors = outlinedFieldColors()
                            )
                            OutlinedTextField(
                                value = viewModel.technicianMobile,
                                onValueChange = { viewModel.technicianMobile = it },
                                label = { Text("Technician Mobile", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f).testTag("tech_mobile_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = outlinedFieldColors()
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = viewModel.customerNameForItemList,
                                onValueChange = { viewModel.customerNameForItemList = it },
                                label = { Text("Customer Name", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f).testTag("cust_name_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words),
                                colors = outlinedFieldColors()
                            )
                            OutlinedTextField(
                                value = viewModel.customerMobileForItemList,
                                onValueChange = { viewModel.customerMobileForItemList = it },
                                label = { Text("Customer Mobile", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f).testTag("cust_mobile_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = outlinedFieldColors()
                            )
                        }

                        OutlinedTextField(
                            value = viewModel.customerAddressForItemList,
                            onValueChange = { viewModel.customerAddressForItemList = it },
                            label = { Text("Customer Address", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("cust_address_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences),
                            colors = outlinedFieldColors()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- 2. ITEM SELECTOR (3 LEVEL HIERARCHY) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = RemsNavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, RemsBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectorSectionExpanded = !selectorSectionExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📦", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SELECT ITEM & QUANTITY",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = RemsGold,
                                fontSize = 13.sp
                            )
                        )
                    }
                    Icon(
                        imageVector = if (selectorSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle",
                        tint = RemsGold
                    )
                }

                AnimatedVisibility(visible = selectorSectionExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Level 1: Work Type Selector
                        var workTypeMenuExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = workTypeMenuExpanded,
                            onExpandedChange = { workTypeMenuExpanded = !workTypeMenuExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = workType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("1. Work Type", fontSize = 11.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = workTypeMenuExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                colors = outlinedFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = workTypeMenuExpanded,
                                onDismissRequest = { workTypeMenuExpanded = false }
                            ) {
                                listOf("Electrical", "Plumbing", "Mechanical").forEach { wt ->
                                    DropdownMenuItem(
                                        text = { Text(wt, fontWeight = if (wt == workType) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            viewModel.setWorkType(wt)
                                            workTypeMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Level 2: Category Selector
                        var categoryMenuExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = categoryMenuExpanded,
                            onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = category.ifBlank { "Select Category" },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("2. Category", fontSize = 11.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                colors = outlinedFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = categoryMenuExpanded,
                                onDismissRequest = { categoryMenuExpanded = false }
                            ) {
                                if (categories.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No categories. Use Edit Lists to add.") },
                                        onClick = { categoryMenuExpanded = false }
                                    )
                                } else {
                                    categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat.categoryName, fontWeight = if (cat.categoryName == category) FontWeight.Bold else FontWeight.Normal) },
                                            onClick = {
                                                viewModel.setCategory(cat.categoryName)
                                                categoryMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Level 3: Item Selector
                        var itemMenuExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = itemMenuExpanded,
                            onExpandedChange = { itemMenuExpanded = !itemMenuExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = itemName.ifBlank { "Select Item" },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("3. Item", fontSize = 11.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemMenuExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                colors = outlinedFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = itemMenuExpanded,
                                onDismissRequest = { itemMenuExpanded = false }
                            ) {
                                if (itemsForCategory.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text(if (category.isBlank()) "Select Category First" else "No items in category. Add via Edit Lists.") },
                                        onClick = { itemMenuExpanded = false }
                                    )
                                } else {
                                    itemsForCategory.forEach { itm ->
                                        DropdownMenuItem(
                                            text = { Text(itm.itemName, fontWeight = if (itm.itemName == itemName) FontWeight.Bold else FontWeight.Normal) },
                                            onClick = {
                                                viewModel.setItemName(itm.itemName)
                                                itemMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Quantity & Add Item Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = viewModel.quantityInput,
                                onValueChange = { viewModel.quantityInput = it },
                                label = { Text("Qty", fontSize = 11.sp) },
                                placeholder = { Text("उदा. 2, 5m, 10pc") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                modifier = Modifier.width(110.dp).testTag("item_qty_input"),
                                colors = outlinedFieldColors()
                            )

                            Button(
                                onClick = {
                                    viewModel.addItemToOutput(insertBelowTargetId)
                                    insertBelowTargetId = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("add_item_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (insertBelowTargetId != null) RemsAmber else RemsGold
                                )
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = RemsNavyDark,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (insertBelowTargetId != null) "INSERT ITEM" else "ADD ITEM",
                                    fontWeight = FontWeight.Black,
                                    color = RemsNavyDark,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        if (insertBelowTargetId != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = RemsAmber.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RemsAmber),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "📍 चयनित स्थान के नीचे जोड़ा जाएगा",
                                        color = RemsGoldLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    TextButton(
                                        onClick = { insertBelowTargetId = null },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("रद्द करें", color = RemsRed, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 3. OUTPUT ITEM LIST ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ORDER LIST (${viewModel.selectedOutputItems.size})",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = RemsGold
                )
            )

            if (viewModel.selectedOutputItems.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.selectedOutputItems.clear() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = RemsRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear List", color = RemsRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (viewModel.selectedOutputItems.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = RemsNavyCard.copy(alpha = 0.6f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, RemsBorder.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = "Empty", tint = RemsTextMuted, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "सूची खाली है। ऊपर से आइटम और मात्रा चुनकर 'ADD ITEM' दबाएं।",
                        color = RemsTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                viewModel.selectedOutputItems.forEachIndexed { index, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = RemsNavyLight),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RemsBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}.",
                                fontWeight = FontWeight.Bold,
                                color = RemsGold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Qty: ${item.quantity}  •  ${item.category}",
                                    color = RemsGoldLight,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = RemsBlue,
                                    modifier = Modifier.clickable { itemToEdit = item }
                                ) {
                                    Text(
                                        text = "Edit",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = RemsAmber,
                                    modifier = Modifier.clickable {
                                        insertBelowTargetId = item.id
                                        selectorSectionExpanded = true
                                    }
                                ) {
                                    Text(
                                        text = "+ Below",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = RemsRed,
                                    modifier = Modifier.clickable { viewModel.removeOutputItem(item.id) }
                                ) {
                                    Text(
                                        text = "✕",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Important Note
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF101D2D),
            border = androidx.compose.foundation.BorderStroke(1.dp, RemsBorder)
        ) {
            Row(modifier = Modifier.padding(10.dp)) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(RemsGold)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Important Note: If any item needs to be changed from the listed items, please contact the electrician on the provided number (8685058336).",
                    color = RemsTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 4. ACTION BUTTONS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showEditListsDialog = true },
                modifier = Modifier.weight(1f).height(44.dp).testTag("edit_lists_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RemsGold)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Edit Lists", tint = RemsNavyDark, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("EDIT LISTS", fontWeight = FontWeight.Black, color = RemsNavyDark, fontSize = 11.5.sp)
            }

            Button(
                onClick = {
                    if (viewModel.selectedOutputItems.isEmpty()) {
                        ShareAndPrintUtils.sharePlainText(context, "REMS Item List", "REMS: Item List is empty.")
                    } else {
                        val text = buildItemListShareText(viewModel)
                        ShareAndPrintUtils.shareToWhatsApp(
                            context = context,
                            phoneNumber = viewModel.customerMobileForItemList,
                            message = text,
                            customerName = viewModel.customerNameForItemList,
                            documentType = "Service Report"
                        )
                    }
                },
                modifier = Modifier.weight(1f).height(44.dp).testTag("share_list_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RemsBlue)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("SHARE", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.5.sp)
            }

            Button(
                onClick = { showPosPreviewDialog = true },
                modifier = Modifier.weight(1f).height(44.dp).testTag("pos_print_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RemsGreen)
            ) {
                Icon(Icons.Default.Print, contentDescription = "Print", tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("POS PRINT", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.5.sp)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    // Edit Item Dialog
    if (itemToEdit != null) {
        var editName by remember { mutableStateOf(itemToEdit!!.name) }
        var editQty by remember { mutableStateOf(itemToEdit!!.quantity) }

        AlertDialog(
            onDismissRequest = { itemToEdit = null },
            title = { Text("Edit Item", fontWeight = FontWeight.Bold, color = RemsGold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Item Name") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words),
                        colors = outlinedFieldColors()
                    )
                    OutlinedTextField(
                        value = editQty,
                        onValueChange = { editQty = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        colors = outlinedFieldColors()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateOutputItem(itemToEdit!!.id, editName, editQty)
                        itemToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RemsGold)
                ) {
                    Text("Save", color = RemsNavyDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToEdit = null }) {
                    Text("Cancel", color = RemsTextSecondary)
                }
            }
        )
    }

    // POS / Thermal Preview Dialog
    if (showPosPreviewDialog) {
        Dialog(onDismissRequest = { showPosPreviewDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("RAJAT ELECTRO-MECH SOLUTIONS", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.Black, textAlign = TextAlign.Center)
                    Text("REMS • Uchana", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.DarkGray)
                    Text("Prop: Sushil Sheokand • 8685058336", fontSize = 11.sp, color = Color.DarkGray)
                    Text("Date: ${ShareAndPrintUtils.getCurrentDateIndian()}", fontSize = 11.sp, color = Color.Gray)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color.LightGray)

                    if (viewModel.customerNameForItemList.isNotBlank()) {
                        Text(
                            text = "Customer: ${viewModel.customerNameForItemList} (${viewModel.customerMobileForItemList})",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            modifier = Modifier.fillMaxWidth()
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.LightGray)
                    }

                    viewModel.selectedOutputItems.forEachIndexed { i, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${i + 1}. ${item.name}", fontSize = 11.5.sp, color = Color.Black, modifier = Modifier.weight(1f))
                            Text(item.quantity, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color.LightGray)
                    Text(
                        text = "Important Note: If any item needs to be changed, please contact electrician at 8685058336.",
                        fontSize = 9.5.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                val text = buildItemListShareText(viewModel)
                                ShareAndPrintUtils.printCustomerDocument(
                                    context = context,
                                    customerName = viewModel.customerNameForItemList,
                                    documentType = "Service Report",
                                    contentText = text
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RemsGreen)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Print / PDF", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val text = buildItemListShareText(viewModel)
                                ShareAndPrintUtils.createAndSharePdf(
                                    context = context,
                                    customerName = viewModel.customerNameForItemList,
                                    documentType = "Service Report",
                                    contentText = text
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RemsNavyLight)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Share PDF", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showPosPreviewDialog = false },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RemsGold)
                        ) {
                            Text("Close", color = RemsNavyDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // Edit Lists Catalog Manager Dialog
    if (showEditListsDialog) {
        EditListsCatalogDialog(
            viewModel = viewModel,
            onDismiss = { showEditListsDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditListsCatalogDialog(
    viewModel: RemsViewModel,
    onDismiss: () -> Unit
) {
    val workType by viewModel.selectedWorkType.collectAsState()
    val category by viewModel.selectedCategory.collectAsState()
    val categories by viewModel.currentCategories.collectAsState()
    val items by viewModel.currentCategoryItems.collectAsState()

    var showAddCategoryPrompt by remember { mutableStateOf(false) }
    var showEditCategoryPrompt by remember { mutableStateOf(false) }
    var showAddItemPrompt by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<CustomItemEntity?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(4.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = RemsNavyCard),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, RemsGold)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIST MANAGEMENT",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = RemsGold
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Work Type & Category Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var wtExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = wtExpanded,
                        onExpandedChange = { wtExpanded = !wtExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = workType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Work Type", fontSize = 10.sp) },
                            modifier = Modifier.menuAnchor(),
                            colors = outlinedFieldColors()
                        )
                        ExposedDropdownMenu(expanded = wtExpanded, onDismissRequest = { wtExpanded = false }) {
                            listOf("Electrical", "Plumbing", "Mechanical").forEach { wt ->
                                DropdownMenuItem(
                                    text = { Text(wt) },
                                    onClick = {
                                        viewModel.setWorkType(wt)
                                        wtExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    var catExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = catExpanded,
                        onExpandedChange = { catExpanded = !catExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = category.ifBlank { "Select Category" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category", fontSize = 10.sp) },
                            modifier = Modifier.menuAnchor(),
                            colors = outlinedFieldColors()
                        )
                        ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                            categories.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.categoryName) },
                                    onClick = {
                                        viewModel.setCategory(c.categoryName)
                                        catExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Category Actions Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { showAddCategoryPrompt = true },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RemsGreen),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text("+ Category", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            if (category.isNotBlank()) showEditCategoryPrompt = true
                        },
                        enabled = category.isNotBlank(),
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RemsBlue),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text("✎ Category", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            if (category.isNotBlank()) viewModel.deleteCategory(category)
                        },
                        enabled = category.isNotBlank(),
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RemsRed),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text("🗑 Category", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Items in: ${category.ifBlank { "No Category Selected" }} (${items.size})",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = RemsGoldLight
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Items list inside category
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(RemsNavyDark, RoundedCornerShape(8.dp))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(items) { idx, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(RemsNavyLight, RoundedCornerShape(6.dp))
                                .border(1.dp, RemsBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${idx + 1}. ${item.itemName}", color = Color.White, fontSize = 11.5.sp, modifier = Modifier.weight(1f))

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = RemsBlue,
                                    modifier = Modifier.clickable { itemToEdit = item }
                                ) {
                                    Text("Edit", color = Color.White, fontSize = 9.5.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = RemsRed,
                                    modifier = Modifier.clickable { viewModel.deleteCatalogItem(item) }
                                ) {
                                    Text("Del", color = Color.White, fontSize = 9.5.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { showAddItemPrompt = true },
                    enabled = category.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RemsGreen)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ ADD NEW ITEM TO CATEGORY", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.5.sp)
                }
            }
        }
    }

    if (showAddCategoryPrompt) {
        var newCatName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryPrompt = false },
            title = { Text("Add Category", color = RemsGold, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    label = { Text("Category Name") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words),
                    colors = outlinedFieldColors()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addNewCategory(newCatName)
                        showAddCategoryPrompt = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RemsGold)
                ) {
                    Text("Add", color = RemsNavyDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryPrompt = false }) {
                    Text("Cancel", color = RemsTextSecondary)
                }
            }
        )
    }

    if (showEditCategoryPrompt) {
        var editCatName by remember { mutableStateOf(category) }
        AlertDialog(
            onDismissRequest = { showEditCategoryPrompt = false },
            title = { Text("Edit Category", color = RemsGold, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editCatName,
                    onValueChange = { editCatName = it },
                    label = { Text("New Category Name") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words),
                    colors = outlinedFieldColors()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.editCategory(category, editCatName)
                        showEditCategoryPrompt = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RemsGold)
                ) {
                    Text("Save", color = RemsNavyDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditCategoryPrompt = false }) {
                    Text("Cancel", color = RemsTextSecondary)
                }
            }
        )
    }

    if (showAddItemPrompt) {
        var newItmName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddItemPrompt = false },
            title = { Text("Add Item to $category", color = RemsGold, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newItmName,
                    onValueChange = { newItmName = it },
                    label = { Text("Item Name") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words),
                    colors = outlinedFieldColors()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addNewCatalogItem(category, newItmName)
                        showAddItemPrompt = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RemsGold)
                ) {
                    Text("Add Item", color = RemsNavyDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemPrompt = false }) {
                    Text("Cancel", color = RemsTextSecondary)
                }
            }
        )
    }

    if (itemToEdit != null) {
        var editItmName by remember { mutableStateOf(itemToEdit!!.itemName) }
        AlertDialog(
            onDismissRequest = { itemToEdit = null },
            title = { Text("Edit Item", color = RemsGold, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editItmName,
                    onValueChange = { editItmName = it },
                    label = { Text("Item Name") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words),
                    colors = outlinedFieldColors()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.editCatalogItem(itemToEdit!!, editItmName)
                        itemToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RemsGold)
                ) {
                    Text("Save", color = RemsNavyDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToEdit = null }) {
                    Text("Cancel", color = RemsTextSecondary)
                }
            }
        )
    }
}

@Composable
fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = EditorialPrimary,
    unfocusedBorderColor = EditorialBorder,
    focusedLabelColor = EditorialPrimary,
    unfocusedLabelColor = EditorialTextSecondary,
    focusedTextColor = EditorialTextPrimary,
    unfocusedTextColor = EditorialTextPrimary,
    cursorColor = EditorialPrimary,
    focusedContainerColor = EditorialSurface,
    unfocusedContainerColor = EditorialSurface
)

private fun buildItemListShareText(viewModel: RemsViewModel): String {
    val sb = StringBuilder()
    sb.append("📋 *RAJAT ELECTRO-MECH SOLUTIONS (REMS)*\n")
    sb.append("Prop.: Sushil Sheokand • 8685058336\n")
    sb.append("Saffa Kheri Road, Kharak Bhura, Uchana\n")
    sb.append("Date: ${ShareAndPrintUtils.getCurrentDateIndian()}\n")
    sb.append("----------------------------\n")

    if (viewModel.technicianName.isNotBlank() || viewModel.customerNameForItemList.isNotBlank()) {
        if (viewModel.technicianName.isNotBlank()) sb.append("Technician: ${viewModel.technicianName} (${viewModel.technicianMobile})\n")
        if (viewModel.customerNameForItemList.isNotBlank()) sb.append("Customer: ${viewModel.customerNameForItemList} (${viewModel.customerMobileForItemList})\n")
        if (viewModel.customerAddressForItemList.isNotBlank()) sb.append("Address: ${viewModel.customerAddressForItemList}\n")
        sb.append("----------------------------\n")
    }

    sb.append("*ITEMS LIST:*\n")
    viewModel.selectedOutputItems.forEachIndexed { i, itm ->
        sb.append("${i + 1}. ${itm.name} (Qty: ${itm.quantity})\n")
    }

    sb.append("----------------------------\n")
    sb.append("Note: If any item needs to be changed, please contact electrician at 8685058336.\n")
    sb.append("REMS • Quality Service Guaranteed")
    return sb.toString()
}
