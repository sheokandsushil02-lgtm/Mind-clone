package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.db.*
import com.example.data.repository.RemsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class KhataImportResult(
    val success: Boolean,
    val importedCustomers: Int,
    val importedTransactions: Int,
    val message: String
)

data class BillsImportResult(
    val success: Boolean,
    val importedBills: Int,
    val importedProducts: Int,
    val importedWarranties: Int,
    val message: String
)

object JsonBackupManager {

    private fun getTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }

    private fun getReadableDate(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    // ==========================================
    // KHATA BOOK JSON EXPORT & IMPORT
    // ==========================================

    fun generateKhataBookJsonString(
        customers: List<CustomerEntity>,
        transactions: List<TransactionEntity>
    ): String {
        val root = JSONObject()
        root.put("fileType", "REMS_KHATA_BOOK_BACKUP")
        root.put("version", 1)
        root.put("appName", "REMS Business")
        root.put("exportDate", getReadableDate())
        root.put("totalCustomers", customers.size)
        root.put("totalTransactions", transactions.size)

        val customerNameMap = customers.associate { it.id to it.name }

        val custArray = JSONArray()
        customers.forEach { c ->
            val cObj = JSONObject()
            cObj.put("id", c.id)
            cObj.put("name", c.name)
            cObj.put("mobile", c.mobile)
            cObj.put("address", c.address)
            if (c.paymentTime != null) {
                cObj.put("paymentTime", c.paymentTime)
            } else {
                cObj.put("paymentTime", JSONObject.NULL)
            }
            cObj.put("paymentTimeCount", c.paymentTimeCount)
            cObj.put("paymentTimeHistory", c.paymentTimeHistory)
            cObj.put("notificationSent", c.notificationSent)
            cObj.put("createdAt", c.createdAt)
            custArray.put(cObj)
        }
        root.put("customers", custArray)

        val txArray = JSONArray()
        transactions.forEach { t ->
            val tObj = JSONObject()
            tObj.put("id", t.id)
            tObj.put("customerId", t.customerId)
            tObj.put("customerName", customerNameMap[t.customerId] ?: "")
            tObj.put("date", t.date)
            tObj.put("work", t.work)
            tObj.put("lene", t.lene)
            tObj.put("aaye", t.aaye)
            tObj.put("discount", t.discount)
            tObj.put("paymentMode", t.paymentMode)
            tObj.put("totalDelta", t.totalDelta)
            tObj.put("timestamp", t.timestamp)
            txArray.put(tObj)
        }
        root.put("transactions", txArray)

        return root.toString(2)
    }

    suspend fun importKhataBookJson(
        jsonString: String,
        repository: RemsRepository
    ): KhataImportResult = withContext(Dispatchers.IO) {
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("फ़ाइल खाली है (File is empty). कृपया वैध JSON फ़ाइल चुनें।")
        }

        val root: JSONObject
        try {
            root = JSONObject(trimmed)
        } catch (e: Exception) {
            throw IllegalArgumentException("अमान्य JSON प्रारूप (Invalid JSON syntax): ${e.message}")
        }

        if (!root.has("customers") && !root.has("transactions")) {
            throw IllegalArgumentException("अमान्य खाता बही डेटा: JSON में ग्राहक ('customers') या लेन-देन ('transactions') सूची नहीं मिली।")
        }

        val existingCustomers = repository.allCustomers.first()
        val existingTransactions = repository.allTransactions.first()

        val customerIdMap = mutableMapOf<Long, Long>()
        var importedCustomerCount = 0
        var importedTxCount = 0

        // 1. Process Customers
        val custArray = root.optJSONArray("customers") ?: JSONArray()
        for (i in 0 until custArray.length()) {
            val cObj = custArray.getJSONObject(i)
            val oldId = cObj.optLong("id", -1L)
            val name = cObj.optString("name", "").trim()
            val mobile = cObj.optString("mobile", "").trim()
            val address = cObj.optString("address", "").trim()
            val paymentTime = if (cObj.isNull("paymentTime")) null else cObj.optString("paymentTime")
            val paymentTimeCount = cObj.optInt("paymentTimeCount", 0)
            val paymentTimeHistory = cObj.optString("paymentTimeHistory", "[]")
            val notificationSent = cObj.optBoolean("notificationSent", false)
            val createdAt = cObj.optLong("createdAt", System.currentTimeMillis())

            if (name.isBlank()) continue

            val match = existingCustomers.firstOrNull {
                (mobile.isNotBlank() && it.mobile == mobile) || (it.name.equals(name, ignoreCase = true))
            }

            val finalId: Long
            if (match != null) {
                finalId = match.id
                val updatedCustomer = match.copy(
                    mobile = if (match.mobile.isBlank()) mobile else match.mobile,
                    address = if (match.address.isBlank()) address else match.address,
                    paymentTime = match.paymentTime ?: paymentTime,
                    paymentTimeCount = maxOf(match.paymentTimeCount, paymentTimeCount),
                    paymentTimeHistory = if (match.paymentTimeHistory == "[]" || match.paymentTimeHistory.isBlank()) paymentTimeHistory else match.paymentTimeHistory
                )
                repository.updateCustomer(updatedCustomer)
            } else {
                val newEntity = CustomerEntity(
                    id = 0,
                    name = name,
                    mobile = mobile,
                    address = address,
                    paymentTime = paymentTime,
                    paymentTimeCount = paymentTimeCount,
                    paymentTimeHistory = paymentTimeHistory,
                    notificationSent = notificationSent,
                    createdAt = createdAt
                )
                finalId = repository.saveCustomer(newEntity)
                importedCustomerCount++
            }

            if (oldId > 0) {
                customerIdMap[oldId] = finalId
            }
        }

        // 2. Process Transactions
        val txArray = root.optJSONArray("transactions") ?: JSONArray()
        for (i in 0 until txArray.length()) {
            val tObj = txArray.getJSONObject(i)
            val oldCustId = tObj.optLong("customerId", -1L)
            val custName = tObj.optString("customerName", "").trim()
            val date = tObj.optString("date", ShareAndPrintUtils.getCurrentDateIndian())
            val work = tObj.optString("work", "")
            val lene = tObj.optDouble("lene", 0.0)
            val aaye = tObj.optDouble("aaye", 0.0)
            val discount = tObj.optDouble("discount", 0.0)
            val paymentMode = tObj.optString("paymentMode", "Cash")
            val totalDelta = tObj.optDouble("totalDelta", lene - aaye - discount)
            val timestamp = tObj.optLong("timestamp", System.currentTimeMillis())

            var targetCustId = customerIdMap[oldCustId]
            if (targetCustId == null && custName.isNotBlank()) {
                val matched = repository.allCustomers.first().firstOrNull { it.name.equals(custName, ignoreCase = true) }
                targetCustId = matched?.id
            }

            if (targetCustId == null) {
                if (custName.isNotBlank()) {
                    val newId = repository.saveCustomer(CustomerEntity(name = custName))
                    targetCustId = newId
                    if (oldCustId > 0) customerIdMap[oldCustId] = newId
                    importedCustomerCount++
                } else {
                    continue
                }
            }

            val isDuplicate = existingTransactions.any {
                it.customerId == targetCustId &&
                        it.work == work &&
                        it.lene == lene &&
                        it.aaye == aaye &&
                        it.discount == discount &&
                        (it.timestamp == timestamp || it.date == date)
            }

            if (!isDuplicate) {
                val txEntity = TransactionEntity(
                    id = 0,
                    customerId = targetCustId,
                    date = date,
                    work = work,
                    lene = lene,
                    aaye = aaye,
                    discount = discount,
                    paymentMode = paymentMode,
                    totalDelta = totalDelta,
                    timestamp = timestamp
                )
                repository.saveTransaction(txEntity)
                importedTxCount++
            }
        }

        KhataImportResult(
            success = true,
            importedCustomers = importedCustomerCount,
            importedTransactions = importedTxCount,
            message = "सफलतापूर्वक $importedCustomerCount ग्राहक और $importedTxCount लेन-देन आयात (Import) किए गए।"
        )
    }

    // ==========================================
    // BILLS JSON EXPORT & IMPORT
    // ==========================================

    fun generateBillsJsonString(
        bills: List<BillEntity>,
        savedProducts: List<SavedProductEntity> = emptyList(),
        warranties: List<WarrantyEntity> = emptyList()
    ): String {
        val root = JSONObject()
        root.put("fileType", "REMS_BILLS_BACKUP")
        root.put("version", 1)
        root.put("appName", "REMS Business")
        root.put("exportDate", getReadableDate())
        root.put("totalBills", bills.size)
        root.put("totalProducts", savedProducts.size)
        root.put("totalWarranties", warranties.size)

        val billsArray = JSONArray()
        bills.forEach { b ->
            val bObj = JSONObject()
            bObj.put("billNo", b.billNo)
            bObj.put("date", b.date)
            bObj.put("customerName", b.customerName)
            bObj.put("customerPhone", b.customerPhone)
            bObj.put("customerAddress", b.customerAddress)
            bObj.put("discount", b.discount)
            bObj.put("totalAmount", b.totalAmount)
            bObj.put("netAmount", b.netAmount)
            bObj.put("itemsJson", b.itemsJson)
            bObj.put("createdAt", b.createdAt)
            billsArray.put(bObj)
        }
        root.put("bills", billsArray)

        val prodArray = JSONArray()
        savedProducts.forEach { p ->
            val pObj = JSONObject()
            pObj.put("id", p.id)
            pObj.put("name", p.name)
            pObj.put("defaultRate", p.defaultRate)
            pObj.put("category", p.category)
            prodArray.put(pObj)
        }
        root.put("savedProducts", prodArray)

        val warArray = JSONArray()
        warranties.forEach { w ->
            val wObj = JSONObject()
            wObj.put("warrantyNo", w.warrantyNo)
            wObj.put("date", w.date)
            wObj.put("originalBillNo", w.originalBillNo)
            wObj.put("customerName", w.customerName)
            wObj.put("customerPhone", w.customerPhone)
            wObj.put("customerAddress", w.customerAddress)
            wObj.put("itemName", w.itemName)
            wObj.put("serialNumber", w.serialNumber)
            wObj.put("purchaseDate", w.purchaseDate)
            wObj.put("guaranteePeriod", w.guaranteePeriod)
            wObj.put("guaranteeTill", w.guaranteeTill)
            wObj.put("terms", w.terms)
            wObj.put("createdAt", w.createdAt)
            warArray.put(wObj)
        }
        root.put("warranties", warArray)

        return root.toString(2)
    }

    suspend fun importBillsJson(
        jsonString: String,
        repository: RemsRepository
    ): BillsImportResult = withContext(Dispatchers.IO) {
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("फ़ाइल खाली है (File is empty). कृपया वैध JSON फ़ाइल चुनें।")
        }

        val root: JSONObject
        try {
            root = JSONObject(trimmed)
        } catch (e: Exception) {
            throw IllegalArgumentException("अमान्य JSON प्रारूप (Invalid JSON syntax): ${e.message}")
        }

        if (!root.has("bills") && !root.has("savedProducts") && !root.has("warranties")) {
            throw IllegalArgumentException("अमान्य बिल डेटा: JSON में बिल ('bills') या वारंटी सूची नहीं मिली।")
        }

        var importedBillsCount = 0
        var importedProductsCount = 0
        var importedWarrantiesCount = 0

        // 1. Process Bills
        val billsArray = root.optJSONArray("bills") ?: JSONArray()
        for (i in 0 until billsArray.length()) {
            val bObj = billsArray.getJSONObject(i)
            val billNo = bObj.optString("billNo", "").trim()
            val date = bObj.optString("date", ShareAndPrintUtils.getCurrentDateIso())
            val customerName = bObj.optString("customerName", "Customer").trim()
            val customerPhone = bObj.optString("customerPhone", "").trim()
            val customerAddress = bObj.optString("customerAddress", "").trim()
            val discount = bObj.optDouble("discount", 0.0)
            val totalAmount = bObj.optDouble("totalAmount", 0.0)
            val netAmount = bObj.optDouble("netAmount", totalAmount - discount)
            val itemsJson = bObj.optString("itemsJson", "[]")
            val createdAt = bObj.optLong("createdAt", System.currentTimeMillis())

            if (billNo.isBlank()) continue

            val billEntity = BillEntity(
                billNo = billNo,
                date = date,
                customerName = customerName,
                customerPhone = customerPhone,
                customerAddress = customerAddress,
                discount = discount,
                totalAmount = totalAmount,
                netAmount = netAmount,
                itemsJson = itemsJson,
                createdAt = createdAt
            )
            repository.saveBill(billEntity)
            importedBillsCount++
        }

        // 2. Process Saved Products
        val prodArray = root.optJSONArray("savedProducts") ?: JSONArray()
        val existingProducts = repository.allSavedProducts.first()
        for (i in 0 until prodArray.length()) {
            val pObj = prodArray.getJSONObject(i)
            val name = pObj.optString("name", "").trim()
            val rate = pObj.optDouble("defaultRate", 0.0)
            val category = pObj.optString("category", "").trim()

            if (name.isBlank()) continue

            val exists = existingProducts.any { it.name.equals(name, ignoreCase = true) }
            if (!exists) {
                repository.saveProduct(
                    SavedProductEntity(
                        id = 0,
                        name = name,
                        defaultRate = rate,
                        category = category
                    )
                )
                importedProductsCount++
            }
        }

        // 3. Process Warranties
        val warArray = root.optJSONArray("warranties") ?: JSONArray()
        for (i in 0 until warArray.length()) {
            val wObj = warArray.getJSONObject(i)
            val warrantyNo = wObj.optString("warrantyNo", "").trim()
            val date = wObj.optString("date", ShareAndPrintUtils.getCurrentDateIso())
            val originalBillNo = wObj.optString("originalBillNo", "").trim()
            val customerName = wObj.optString("customerName", "Customer").trim()
            val customerPhone = wObj.optString("customerPhone", "").trim()
            val customerAddress = wObj.optString("customerAddress", "").trim()
            val itemName = wObj.optString("itemName", "").trim()
            val serialNumber = wObj.optString("serialNumber", "").trim()
            val purchaseDate = wObj.optString("purchaseDate", date)
            val guaranteePeriod = wObj.optString("guaranteePeriod", "12 महीने")
            val guaranteeTill = wObj.optString("guaranteeTill", "")
            val terms = wObj.optString("terms", "")
            val createdAt = wObj.optLong("createdAt", System.currentTimeMillis())

            if (warrantyNo.isBlank()) continue

            val warrantyEntity = WarrantyEntity(
                warrantyNo = warrantyNo,
                date = date,
                originalBillNo = originalBillNo,
                customerName = customerName,
                customerPhone = customerPhone,
                customerAddress = customerAddress,
                itemName = itemName,
                serialNumber = serialNumber,
                purchaseDate = purchaseDate,
                guaranteePeriod = guaranteePeriod,
                guaranteeTill = guaranteeTill,
                terms = terms,
                createdAt = createdAt
            )
            repository.saveWarranty(warrantyEntity)
            importedWarrantiesCount++
        }

        BillsImportResult(
            success = true,
            importedBills = importedBillsCount,
            importedProducts = importedProductsCount,
            importedWarranties = importedWarrantiesCount,
            message = "सफलतापूर्वक $importedBillsCount बिल, $importedProductsCount उत्पाद, $importedWarrantiesCount वारंटी आयात (Import) किए गए।"
        )
    }

    // ==========================================
    // EXPORT JSON TO FILE & SHARE
    // ==========================================

    fun exportAndShareJsonFile(
        context: Context,
        fileName: String,
        jsonString: String
    ) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val cleanName = if (fileName.endsWith(".json")) fileName else "$fileName.json"
            val file = File(exportDir, cleanName)

            FileOutputStream(file).use { out ->
                out.write(jsonString.toByteArray(Charsets.UTF_8))
                out.flush()
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, cleanName)
                putExtra(Intent.EXTRA_TEXT, "REMS Business Data Backup: $cleanName")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Save or Share JSON Backup")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "JSON एक्सपोर्ट त्रुटि: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
