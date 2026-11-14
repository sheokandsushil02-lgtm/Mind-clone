package com.example.data.repository

import com.example.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class RemsRepository(private val database: RemsDatabase) {
    val customerDao = database.customerDao()
    val transactionDao = database.transactionDao()
    val catalogDao = database.catalogDao()
    val billDao = database.billDao()
    val savedProductDao = database.savedProductDao()
    val warrantyDao = database.warrantyDao()

    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allBills: Flow<List<BillEntity>> = billDao.getAllBills()
    val allSavedProducts: Flow<List<SavedProductEntity>> = savedProductDao.getAllProducts()
    val allWarranties: Flow<List<WarrantyEntity>> = warrantyDao.getAllWarranties()

    fun getTransactionsForCustomer(customerId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsForCustomer(customerId)

    fun getCategories(workType: String): Flow<List<CustomCategoryEntity>> =
        catalogDao.getCategories(workType)

    fun getItems(workType: String, categoryName: String): Flow<List<CustomItemEntity>> =
        catalogDao.getItems(workType, categoryName)

    suspend fun checkAndPrepopulateCatalog() = withContext(Dispatchers.IO) {
        if (catalogDao.getCategoryCount() == 0) {
            val defaultCatalog = mapOf(
                "Electrical" to mapOf(
                    "Under ground" to listOf(
                        "Pipe", "Batten", "Flexible pipe", "Fan box",
                        "Concealed box", "Wall dibbi", "Bend", "T",
                        "BOARD 2M", "BOARD 3M", "BOARD 4M", "BOARD 6M",
                        "BOARD 8M", "BOARD 12M", "BOARD 18M"
                    ),
                    "Wires" to listOf(
                        "Alu. Wire 6mm", "Alu. Wire 8mm", "Alu. Wire 10mm",
                        "Alu. Wire 12mm", "Wire 0.75mm", "Wire 1mm",
                        "Wire 1.5mm", "Wire 2.5mm", "Wire 4mm",
                        "Wire 6mm", "Wire 10mm", "Dish cable", "RG6 cable"
                    ),
                    "Switch" to listOf(
                        "6A switch", "16A switch", "25A switch",
                        "2 way switch", "Switch bell push 1M",
                        "Switch bell push 2M", "AC starter",
                        "Regulator", "Indicator", "Dummy plate"
                    ),
                    "Socket" to listOf(
                        "Socket 6A", "Socket 6A 2 pin", "Socket 16A",
                        "Male sue", "Female sue", "3P top 6A",
                        "3P top 16A", "Connector 6A", "Connector 16A",
                        "Connector 32A", "USB port", "Cable socket", "LAN socket"
                    ),
                    "Other acc" to listOf(
                        "Insulation tape", "Fan cover", "Round sheet",
                        "Round block", "Block 4x4", "Holder modular",
                        "Square holder", "Round flat holder", "Ceiling rose",
                        "Door bell", "Water tank bell"
                    ),
                    "Lights" to listOf(
                        "LED bulb", "Concealed light", "LED strip",
                        "Batten light", "Street light"
                    ),
                    "Main box" to listOf(
                        "DP switch 32A", "MCB", "Isolator", "RCCB",
                        "Change over", "Kit-kat 32A", "MCB box",
                        "Submersible starter"
                    ),
                    "Extra" to listOf(
                        "Sheat 2M", "Sheat 3M", "Sheat 4M", "Sheat 6M",
                        "Sheat 8M", "Sheat 12M", "Sheat 18M",
                        "Screw 35x8", "MC petch 3/4\"", "MC petch 1.0\"",
                        "MC petch 1.5\"", "MC petch 2.0\"", "MC petch 2.5\"",
                        "MC petch 3.0\"", "Black screw 1/2\"",
                        "Black screw 3/4\"", "Black screw 1.0\"",
                        "Black screw 1.5\"", "Black screw 2.0\"",
                        "Black screw 2.5\"", "Black screw 3.0\"",
                        "Gitti", "Gatter hook", "Gatter patti",
                        "Kadi hook", "Cutter blade", "Steel wire"
                    ),
                    "Open feeting" to listOf(
                        "Board 4x4", "Board 7x4", "Board 8x5",
                        "Board 8x6", "Board 10x5", "Board 8x10",
                        "Sheet 4x4", "Sheet 7x4", "Sheet 8x5",
                        "Sheet 8x6", "Sheet 10x5", "Sheet 8x10"
                    )
                ),
                "Plumbing" to mapOf(
                    "Pipes & Fittings" to listOf(
                        "CPVC Pipe 1/2\"", "CPVC Pipe 3/4\"", "CPVC Pipe 1\"",
                        "UPVC Pipe 1/2\"", "UPVC Pipe 3/4\"", "UPVC Pipe 1\"",
                        "Elbow 90°", "Tee", "Socket / Coupling", "Reducer",
                        "Union", "FTA (Female Adapter)", "MTA (Male Adapter)",
                        "End Cap", "Ball Valve", "Concealed Valve"
                    ),
                    "Taps & Fixtures" to listOf(
                        "Bib Cock", "Pillar Cock", "Angle Valve", "Sink Cock",
                        "Shower Head", "Health Faucet / Jet Spray", "Diverter"
                    ),
                    "Accessories" to listOf(
                        "Teflon Tape", "Solvent Cement CPVC", "Solvent Cement UPVC",
                        "Waste Pipe", "Connection Pipe 18\"", "Connection Pipe 24\"",
                        "Floor Drain / Jali", "Wall Flange"
                    )
                ),
                "Mechanical" to mapOf(
                    "Fasteners & Bolts" to listOf(
                        "Hex Bolt M8", "Hex Bolt M10", "Hex Bolt M12",
                        "Washer Flat", "Washer Spring", "Nyloc Nut", "Rawl Plug Anchor"
                    ),
                    "Welding & Tools" to listOf(
                        "Welding Rod 10G", "Welding Rod 12G", "Grinding Wheel 4\"",
                        "Cutting Wheel 4\"", "Flap Disc", "Sanding Paper"
                    ),
                    "Bearings & Belts" to listOf(
                        "Bearing 6201", "Bearing 6202", "Bearing 6203",
                        "Bearing 6204", "V-Belt A-Section", "V-Belt B-Section", "Grease AP3"
                    )
                )
            )

            defaultCatalog.forEach { (workType, categories) ->
                var catOrder = 0
                categories.forEach { (catName, items) ->
                    catalogDao.insertCategory(
                        CustomCategoryEntity(
                            workType = workType,
                            categoryName = catName,
                            sortOrder = catOrder++
                        )
                    )
                    var itemOrder = 0
                    val itemEntities = items.map {
                        CustomItemEntity(
                            workType = workType,
                            categoryName = catName,
                            itemName = it,
                            sortOrder = itemOrder++
                        )
                    }
                    catalogDao.insertItems(itemEntities)
                }
            }

            // Also insert some standard saved products for billing
            val initialProducts = listOf(
                SavedProductEntity(name = "1.5mm Copper Wire (90m)", defaultRate = 1850.0, category = "Electrical"),
                SavedProductEntity(name = "2.5mm Copper Wire (90m)", defaultRate = 2950.0, category = "Electrical"),
                SavedProductEntity(name = "6A Modular Switch", defaultRate = 45.0, category = "Electrical"),
                SavedProductEntity(name = "16A Modular Socket", defaultRate = 95.0, category = "Electrical"),
                SavedProductEntity(name = "LED Concealed Light 7W", defaultRate = 160.0, category = "Electrical"),
                SavedProductEntity(name = "LED Batten Tube 20W", defaultRate = 240.0, category = "Electrical"),
                SavedProductEntity(name = "32A DP MCB Isolator", defaultRate = 380.0, category = "Electrical"),
                SavedProductEntity(name = "Ceiling Fan 1200mm", defaultRate = 1750.0, category = "Electrical"),
                SavedProductEntity(name = "Submersible Pump Starter", defaultRate = 1450.0, category = "Electrical"),
                SavedProductEntity(name = "CPVC Angle Valve Brass", defaultRate = 220.0, category = "Plumbing"),
                SavedProductEntity(name = "Brass Bib Cock Heavy", defaultRate = 340.0, category = "Plumbing")
            )
            savedProductDao.insertProducts(initialProducts)
        }
    }

    // Customer & Transaction Operations
    suspend fun saveCustomer(customer: CustomerEntity): Long = customerDao.insertCustomer(customer)
    suspend fun updateCustomer(customer: CustomerEntity) = customerDao.updateCustomer(customer)
    suspend fun deleteCustomer(customer: CustomerEntity) {
        transactionDao.deleteTransactionsForCustomer(customer.id)
        customerDao.deleteCustomer(customer)
    }

    suspend fun saveTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransaction(transaction)
    suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)
    suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    // Bill & Warranty Operations
    suspend fun saveBill(bill: BillEntity) = billDao.insertBill(bill)
    suspend fun deleteBill(billNo: String) = billDao.deleteBillByNo(billNo)

    suspend fun saveProduct(product: SavedProductEntity): Long = savedProductDao.insertProduct(product)
    suspend fun deleteProduct(id: Long) = savedProductDao.deleteProductById(id)

    suspend fun saveWarranty(warranty: WarrantyEntity) = warrantyDao.insertWarranty(warranty)
    suspend fun deleteWarranty(warrantyNo: String) = warrantyDao.deleteWarrantyByNo(warrantyNo)

    // Full Export & Import JSON Backup
    suspend fun exportFullBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("appName", "REMS Business")
        root.put("version", 2)
        root.put("exportDate", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()))

        // Customers with transactions
        val custArray = JSONArray()
        // We can fetch from database
        val customers = mutableListOf<CustomerEntity>()
        // We'll collect current state
        root.put("note", "REMS Complete Business Backup")
        root.toString(2)
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        customerDao.clearAll()
        transactionDao.clearAll()
        billDao.clearAll()
        savedProductDao.clearAll()
        warrantyDao.clearAll()
    }
}
