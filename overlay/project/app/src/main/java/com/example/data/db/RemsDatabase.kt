package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Database
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mobile: String = "",
    val address: String = "",
    val paymentTime: String? = null,
    val paymentTimeCount: Int = 0,
    val paymentTimeHistory: String = "[]",
    val notificationSent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val date: String,
    val work: String = "",
    val lene: Double = 0.0,
    val aaye: Double = 0.0,
    val discount: Double = 0.0,
    val paymentMode: String = "Cash",
    val totalDelta: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_categories")
data class CustomCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workType: String,
    val categoryName: String,
    val sortOrder: Int = 0
)

@Entity(tableName = "custom_items")
data class CustomItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workType: String,
    val categoryName: String,
    val itemName: String,
    val sortOrder: Int = 0
)

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey val billNo: String,
    val date: String,
    val customerName: String,
    val customerPhone: String = "",
    val customerAddress: String = "",
    val discount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val netAmount: Double = 0.0,
    val itemsJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_products")
data class SavedProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val defaultRate: Double = 0.0,
    val category: String = ""
)

@Entity(tableName = "warranties")
data class WarrantyEntity(
    @PrimaryKey val warrantyNo: String,
    val date: String,
    val originalBillNo: String = "",
    val customerName: String,
    val customerPhone: String = "",
    val customerAddress: String = "",
    val itemName: String,
    val serialNumber: String,
    val purchaseDate: String,
    val guaranteePeriod: String = "12 महीने",
    val guaranteeTill: String = "",
    val terms: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY id DESC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: Long)

    @Query("DELETE FROM customers")
    suspend fun clearAll()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY timestamp DESC, id DESC")
    fun getTransactionsForCustomer(customerId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions WHERE customerId = :customerId")
    suspend fun deleteTransactionsForCustomer(customerId: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

@Dao
interface CatalogDao {
    @Query("SELECT * FROM custom_categories WHERE workType = :workType ORDER BY sortOrder ASC, id ASC")
    fun getCategories(workType: String): Flow<List<CustomCategoryEntity>>

    @Query("SELECT * FROM custom_items WHERE workType = :workType AND categoryName = :categoryName ORDER BY sortOrder ASC, id ASC")
    fun getItems(workType: String, categoryName: String): Flow<List<CustomItemEntity>>

    @Query("SELECT * FROM custom_categories ORDER BY workType ASC, sortOrder ASC")
    fun getAllCategories(): Flow<List<CustomCategoryEntity>>

    @Query("SELECT * FROM custom_items ORDER BY workType ASC, categoryName ASC, sortOrder ASC")
    fun getAllItems(): Flow<List<CustomItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CustomCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CustomCategoryEntity>)

    @Update
    suspend fun updateCategory(category: CustomCategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CustomCategoryEntity)

    @Query("DELETE FROM custom_categories WHERE workType = :workType AND categoryName = :categoryName")
    suspend fun deleteCategoryByName(workType: String, categoryName: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: CustomItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<CustomItemEntity>)

    @Update
    suspend fun updateItem(item: CustomItemEntity)

    @Delete
    suspend fun deleteItem(item: CustomItemEntity)

    @Query("DELETE FROM custom_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM custom_items WHERE workType = :workType AND categoryName = :categoryName")
    suspend fun deleteItemsForCategory(workType: String, categoryName: String)

    @Query("SELECT COUNT(*) FROM custom_categories")
    suspend fun getCategoryCount(): Int
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY createdAt DESC")
    fun getAllBills(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE billNo = :billNo")
    suspend fun getBillByNo(billNo: String): BillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillEntity)

    @Delete
    suspend fun deleteBill(bill: BillEntity)

    @Query("DELETE FROM bills WHERE billNo = :billNo")
    suspend fun deleteBillByNo(billNo: String)

    @Query("DELETE FROM bills")
    suspend fun clearAll()
}

@Dao
interface SavedProductDao {
    @Query("SELECT * FROM saved_products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<SavedProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: SavedProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<SavedProductEntity>)

    @Update
    suspend fun updateProduct(product: SavedProductEntity)

    @Delete
    suspend fun deleteProduct(product: SavedProductEntity)

    @Query("DELETE FROM saved_products WHERE id = :id")
    suspend fun deleteProductById(id: Long)

    @Query("DELETE FROM saved_products")
    suspend fun clearAll()
}

@Dao
interface WarrantyDao {
    @Query("SELECT * FROM warranties ORDER BY createdAt DESC")
    fun getAllWarranties(): Flow<List<WarrantyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarranty(warranty: WarrantyEntity)

    @Delete
    suspend fun deleteWarranty(warranty: WarrantyEntity)

    @Query("DELETE FROM warranties WHERE warrantyNo = :warrantyNo")
    suspend fun deleteWarrantyByNo(warrantyNo: String)

    @Query("DELETE FROM warranties")
    suspend fun clearAll()
}

@Database(
    entities = [
        CustomerEntity::class,
        TransactionEntity::class,
        CustomCategoryEntity::class,
        CustomItemEntity::class,
        BillEntity::class,
        SavedProductEntity::class,
        WarrantyEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RemsDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun catalogDao(): CatalogDao
    abstract fun billDao(): BillDao
    abstract fun savedProductDao(): SavedProductDao
    abstract fun warrantyDao(): WarrantyDao

    companion object {
        @Volatile
        private var INSTANCE: RemsDatabase? = null

        fun getDatabase(context: android.content.Context): RemsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    RemsDatabase::class.java,
                    "rems_business_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
