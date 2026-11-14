package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.*
import com.example.data.model.*
import com.example.data.repository.RemsRepository
import com.example.utils.HindiNumberToWords
import com.example.utils.JsonBackupManager
import com.example.utils.KhataImportResult
import com.example.utils.BillsImportResult
import com.example.utils.ShareAndPrintUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

enum class AppHub {
    HOME,
    ITEM_LIST,
    KHATA_BOOK,
    BILL_GENERATE
}

enum class CustomerSortOption {
    ALPHABETICAL,
    HIGHEST_DUE,
    DUE_DATE
}

class RemsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = RemsDatabase.getDatabase(application)
    private val repository = RemsRepository(database)

    // Current Active Hub
    private val _currentHub = MutableStateFlow(AppHub.HOME)
    val currentHub: StateFlow<AppHub> = _currentHub.asStateFlow()

    // --- ITEM LIST MODULE STATE ---
    var technicianName by mutableStateOf("")
    var technicianMobile by mutableStateOf("")
    var customerNameForItemList by mutableStateOf("")
    var customerMobileForItemList by mutableStateOf("")
    var customerAddressForItemList by mutableStateOf("")

    private val _selectedWorkType = MutableStateFlow("Electrical")
    val selectedWorkType: StateFlow<String> = _selectedWorkType.asStateFlow()

    private val _selectedCategory = MutableStateFlow("")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedItemName = MutableStateFlow("")
    val selectedItemName: StateFlow<String> = _selectedItemName.asStateFlow()

    var quantityInput by mutableStateOf("")
    val selectedOutputItems = mutableStateListOf<SelectedItem>()

    // Catalog categories and items from Database
    val currentCategories: StateFlow<List<CustomCategoryEntity>> = _selectedWorkType
        .flatMapLatest { workType -> repository.getCategories(workType) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentCategoryItems: StateFlow<List<CustomItemEntity>> = combine(_selectedWorkType, _selectedCategory) { wt, cat ->
        wt to cat
    }.flatMapLatest { (wt, cat) ->
        if (cat.isBlank()) flowOf(emptyList()) else repository.getItems(wt, cat)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- KHATA BOOK PRO STATE ---
    val allCustomers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    val selectedCustomerId: StateFlow<Long?> = _selectedCustomerId.asStateFlow()

    val currentCustomerTransactions: StateFlow<List<TransactionEntity>> = _selectedCustomerId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getTransactionsForCustomer(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var customerSearchQuery by mutableStateOf("")
    var customerSortOption by mutableStateOf(CustomerSortOption.ALPHABETICAL)

    // --- BILL & WARRANTY MODULE STATE ---
    var currentBillNo by mutableStateOf("")
    var currentBillDate by mutableStateOf(ShareAndPrintUtils.getCurrentDateIso())
    var currentBillCustomerName by mutableStateOf("")
    var currentBillCustomerPhone by mutableStateOf("")
    var currentBillCustomerAddress by mutableStateOf("")
    var currentBillDiscount by mutableStateOf("0")
    val billItemRows = mutableStateListOf<BillItemRow>()

    val allBills: StateFlow<List<BillEntity>> = repository.allBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSavedProducts: StateFlow<List<SavedProductEntity>> = repository.allSavedProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWarranties: StateFlow<List<WarrantyEntity>> = repository.allWarranties
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var billSearchQuery by mutableStateOf("")

    // Warranty Form
    var warrantyNo by mutableStateOf("")
    var warrantyDate by mutableStateOf(ShareAndPrintUtils.getCurrentDateIso())
    var warrantyOriginalBillNo by mutableStateOf("")
    var warrantyCustomerName by mutableStateOf("")
    var warrantyCustomerPhone by mutableStateOf("")
    var warrantyCustomerAddress by mutableStateOf("")
    var warrantyItemName by mutableStateOf("")
    var warrantySerialNumber by mutableStateOf("")
    var warrantyPurchaseDate by mutableStateOf(ShareAndPrintUtils.getCurrentDateIso())
    var warrantyPeriod by mutableStateOf("12 महीने")
    var warrantyTillDate by mutableStateOf("")
    var warrantyTerms by mutableStateOf(
        "यह Guarantee नहीं, Warranty है। Warranty अवधि के दौरान कोई भी item खराब होने पर उसे repair करवाया जाएगा, बदला / replace नहीं करवाया जाएगा। Warranty केवल सामान/कारीगरी दोष के लिए मान्य है। जलने, टूट-फूट, गलत वोल्टेज, पानी, शॉर्ट-सर्किट, गलत उपयोग या बाहरी क्षति पर Warranty लागू नहीं होगी।"
    )

    init {
        viewModelScope.launch {
            repository.checkAndPrepopulateCatalog()
            resetNewBill()
        }
    }

    fun navigateToHub(hub: AppHub) {
        _currentHub.value = hub
    }

    fun setWorkType(workType: String) {
        _selectedWorkType.value = workType
        _selectedCategory.value = ""
        _selectedItemName.value = ""
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
        _selectedItemName.value = ""
    }

    fun setItemName(itemName: String) {
        _selectedItemName.value = itemName
    }

    fun addItemToOutput(insertAfterId: String? = null) {
        val item = _selectedItemName.value.trim()
        val qty = quantityInput.trim()
        if (item.isBlank() || qty.isBlank()) return

        val newItem = SelectedItem(
            name = item,
            quantity = qty,
            workType = _selectedWorkType.value,
            category = _selectedCategory.value
        )

        if (insertAfterId != null) {
            val idx = selectedOutputItems.indexOfFirst { it.id == insertAfterId }
            if (idx != -1) {
                selectedOutputItems.add(idx + 1, newItem)
            } else {
                selectedOutputItems.add(newItem)
            }
        } else {
            selectedOutputItems.add(newItem)
        }

        quantityInput = ""
    }

    fun removeOutputItem(id: String) {
        selectedOutputItems.removeAll { it.id == id }
    }

    fun updateOutputItem(id: String, newName: String, newQty: String) {
        val idx = selectedOutputItems.indexOfFirst { it.id == id }
        if (idx != -1) {
            selectedOutputItems[idx] = selectedOutputItems[idx].copy(
                name = newName.trim(),
                quantity = newQty.trim()
            )
        }
    }

    // Catalog Management
    fun addNewCategory(name: String) = viewModelScope.launch(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return@launch
        val currentCount = currentCategories.value.size
        repository.catalogDao.insertCategory(
            CustomCategoryEntity(
                workType = _selectedWorkType.value,
                categoryName = trimmed,
                sortOrder = currentCount
            )
        )
        _selectedCategory.value = trimmed
    }

    fun editCategory(oldName: String, newName: String) = viewModelScope.launch(Dispatchers.IO) {
        val trimmed = newName.trim()
        if (trimmed.isBlank() || oldName == trimmed) return@launch
        val cat = currentCategories.value.firstOrNull { it.categoryName == oldName } ?: return@launch
        repository.catalogDao.updateCategory(cat.copy(categoryName = trimmed))
        // Also update all items with old category
        val items = repository.catalogDao.getItems(_selectedWorkType.value, oldName).firstOrNull() ?: emptyList()
        items.forEach {
            repository.catalogDao.updateItem(it.copy(categoryName = trimmed))
        }
        if (_selectedCategory.value == oldName) {
            _selectedCategory.value = trimmed
        }
    }

    fun deleteCategory(catName: String) = viewModelScope.launch(Dispatchers.IO) {
        repository.catalogDao.deleteCategoryByName(_selectedWorkType.value, catName)
        repository.catalogDao.deleteItemsForCategory(_selectedWorkType.value, catName)
        if (_selectedCategory.value == catName) {
            _selectedCategory.value = ""
        }
    }

    fun addNewCatalogItem(catName: String, itemName: String) = viewModelScope.launch(Dispatchers.IO) {
        val trimmed = itemName.trim()
        if (trimmed.isBlank() || catName.isBlank()) return@launch
        val currentCount = currentCategoryItems.value.size
        repository.catalogDao.insertItem(
            CustomItemEntity(
                workType = _selectedWorkType.value,
                categoryName = catName,
                itemName = trimmed,
                sortOrder = currentCount
            )
        )
    }

    fun editCatalogItem(item: CustomItemEntity, newName: String) = viewModelScope.launch(Dispatchers.IO) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return@launch
        repository.catalogDao.updateItem(item.copy(itemName = trimmed))
    }

    fun deleteCatalogItem(item: CustomItemEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.catalogDao.deleteItem(item)
    }

    // --- KHATA BOOK ACTIONS ---
    fun selectCustomer(id: Long?) {
        _selectedCustomerId.value = id
    }

    fun addCustomer(name: String, mobile: String, address: String = "") = viewModelScope.launch(Dispatchers.IO) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return@launch
        val entity = CustomerEntity(
            name = trimmedName,
            mobile = mobile.trim(),
            address = address.trim()
        )
        val id = repository.saveCustomer(entity)
        selectCustomer(id)
    }

    fun updateCustomer(customer: CustomerEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateCustomer(customer)
    }

    fun deleteCustomer(customer: CustomerEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteCustomer(customer)
        if (_selectedCustomerId.value == customer.id) {
            _selectedCustomerId.value = null
        }
    }

    fun addTransaction(
        customerId: Long,
        work: String,
        lene: Double,
        aaye: Double,
        discount: Double,
        paymentMode: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        val totalDelta = lene - aaye - discount
        val entity = TransactionEntity(
            customerId = customerId,
            date = ShareAndPrintUtils.getCurrentDateIndian(),
            work = work.trim(),
            lene = lene,
            aaye = aaye,
            discount = discount,
            paymentMode = paymentMode,
            totalDelta = totalDelta,
            timestamp = System.currentTimeMillis()
        )
        repository.saveTransaction(entity)
    }

    fun deleteTransaction(transaction: TransactionEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteTransaction(transaction)
    }

    fun updateTransaction(transaction: TransactionEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateTransaction(transaction)
    }

    fun setCustomerPaymentTime(customer: CustomerEntity, dateIso: String) = viewModelScope.launch(Dispatchers.IO) {
        val currentCount = customer.paymentTimeCount + 1
        val historyList = PaymentHistoryItem.fromJson(customer.paymentTimeHistory).toMutableList()
        val isoNow = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        historyList.add(
            PaymentHistoryItem(
                time = dateIso,
                createdAt = isoNow,
                count = currentCount
            )
        )
        val updated = customer.copy(
            paymentTime = dateIso,
            paymentTimeCount = currentCount,
            paymentTimeHistory = PaymentHistoryItem.toJson(historyList),
            notificationSent = false
        )
        repository.updateCustomer(updated)
    }

    fun closeCustomerPaymentTime(customer: CustomerEntity) = viewModelScope.launch(Dispatchers.IO) {
        val updated = customer.copy(
            paymentTime = null,
            notificationSent = false
        )
        repository.updateCustomer(updated)
    }

    fun getCustomerBalance(customerId: Long, transactions: List<TransactionEntity>): Double {
        return transactions.filter { it.customerId == customerId }.sumOf { it.totalDelta }
    }

    // --- BILL GENERATION ACTIONS ---
    fun resetNewBill() {
        val date = ShareAndPrintUtils.getCurrentDateIso()
        currentBillDate = date
        currentBillNo = ShareAndPrintUtils.generateBillNumber(date, (allBills.value.size + 1))
        currentBillCustomerName = ""
        currentBillCustomerPhone = ""
        currentBillCustomerAddress = ""
        currentBillDiscount = "0"
        billItemRows.clear()
        for (i in 0 until 5) {
            billItemRows.add(BillItemRow())
        }
    }

    fun addBillRow(qty: String = "", desc: String = "", rate: String = "") {
        billItemRows.add(BillItemRow(qty = qty, description = desc, rate = rate))
    }

    fun removeBillRow(id: String) {
        billItemRows.removeAll { it.id == id }
    }

    fun saveCurrentBill() = viewModelScope.launch(Dispatchers.IO) {
        val total = billItemRows.sumOf { it.amount }
        val disc = currentBillDiscount.toDoubleOrNull() ?: 0.0
        val net = kotlin.math.max(0.0, total - disc)

        val entity = BillEntity(
            billNo = currentBillNo.ifBlank { "ZB0001" },
            date = currentBillDate,
            customerName = currentBillCustomerName.trim().ifBlank { "Grahak" },
            customerPhone = currentBillCustomerPhone.trim(),
            customerAddress = currentBillCustomerAddress.trim(),
            discount = disc,
            totalAmount = total,
            netAmount = net,
            itemsJson = BillItemRow.toJson(billItemRows.filter { it.description.isNotBlank() || it.qty.isNotBlank() })
        )
        repository.saveBill(entity)

        // Also add customer to customer database if phone or name is present
        if (currentBillCustomerName.isNotBlank()) {
            val exists = allCustomers.value.any { it.name.equals(currentBillCustomerName, ignoreCase = true) }
            if (!exists) {
                repository.saveCustomer(
                    CustomerEntity(
                        name = currentBillCustomerName.trim(),
                        mobile = currentBillCustomerPhone.trim(),
                        address = currentBillCustomerAddress.trim()
                    )
                )
            }
        }
    }

    fun loadBill(bill: BillEntity) {
        currentBillNo = bill.billNo
        currentBillDate = bill.date
        currentBillCustomerName = bill.customerName
        currentBillCustomerPhone = bill.customerPhone
        currentBillCustomerAddress = bill.customerAddress
        currentBillDiscount = bill.discount.toString()
        billItemRows.clear()
        val loaded = BillItemRow.fromJson(bill.itemsJson)
        billItemRows.addAll(loaded)
        if (billItemRows.isEmpty()) {
            for (i in 0 until 5) billItemRows.add(BillItemRow())
        }
    }

    fun deleteBill(billNo: String) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteBill(billNo)
    }

    fun resetWarrantyForm(bill: BillEntity? = null) {
        warrantyNo = "G-" + (allWarranties.value.size + 1).toString().padStart(4, '0')
        warrantyDate = ShareAndPrintUtils.getCurrentDateIso()
        warrantyPurchaseDate = bill?.date ?: ShareAndPrintUtils.getCurrentDateIso()
        warrantyOriginalBillNo = bill?.billNo ?: currentBillNo
        warrantyCustomerName = bill?.customerName ?: currentBillCustomerName
        warrantyCustomerPhone = bill?.customerPhone ?: currentBillCustomerPhone
        warrantyCustomerAddress = bill?.customerAddress ?: currentBillCustomerAddress
        val firstItem = bill?.let { BillItemRow.fromJson(it.itemsJson).firstOrNull()?.description } ?: ""
        warrantyItemName = firstItem
        warrantySerialNumber = ShareAndPrintUtils.generateUniqueSerialCode()
        warrantyPeriod = "12 महीने"

        // Auto calculate 12 months till
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, 12)
        warrantyTillDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    fun saveCurrentWarranty() = viewModelScope.launch(Dispatchers.IO) {
        val entity = WarrantyEntity(
            warrantyNo = warrantyNo.ifBlank { "G-0001" },
            date = warrantyDate,
            originalBillNo = warrantyOriginalBillNo,
            customerName = warrantyCustomerName.trim().ifBlank { "Customer" },
            customerPhone = warrantyCustomerPhone.trim(),
            customerAddress = warrantyCustomerAddress.trim(),
            itemName = warrantyItemName.trim(),
            serialNumber = warrantySerialNumber.trim(),
            purchaseDate = warrantyPurchaseDate,
            guaranteePeriod = warrantyPeriod,
            guaranteeTill = warrantyTillDate,
            terms = warrantyTerms
        )
        repository.saveWarranty(entity)
    }

    fun deleteWarranty(no: String) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteWarranty(no)
    }

    // Saved Products Management
    fun saveProduct(name: String, rate: Double, category: String = "") = viewModelScope.launch(Dispatchers.IO) {
        if (name.isBlank()) return@launch
        repository.saveProduct(
            SavedProductEntity(
                name = name.trim(),
                defaultRate = rate,
                category = category.trim()
            )
        )
    }

    fun deleteProduct(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteProduct(id)
    }

    // --- JSON BACKUP & RESTORE ---
    fun exportKhataBookJson(): String {
        return JsonBackupManager.generateKhataBookJsonString(
            customers = allCustomers.value,
            transactions = allTransactions.value
        )
    }

    fun importKhataBookJson(
        jsonString: String,
        onResult: (Result<KhataImportResult>) -> Unit
    ) = viewModelScope.launch {
        try {
            val result = JsonBackupManager.importKhataBookJson(jsonString, repository)
            onResult(Result.success(result))
        } catch (e: Exception) {
            onResult(Result.failure(e))
        }
    }

    fun exportBillsJson(): String {
        return JsonBackupManager.generateBillsJsonString(
            bills = allBills.value,
            savedProducts = allSavedProducts.value,
            warranties = allWarranties.value
        )
    }

    fun importBillsJson(
        jsonString: String,
        onResult: (Result<BillsImportResult>) -> Unit
    ) = viewModelScope.launch {
        try {
            val result = JsonBackupManager.importBillsJson(jsonString, repository)
            onResult(Result.success(result))
        } catch (e: Exception) {
            onResult(Result.failure(e))
        }
    }

    fun clearAll() = viewModelScope.launch(Dispatchers.IO) {
        repository.clearAllData()
    }
}
