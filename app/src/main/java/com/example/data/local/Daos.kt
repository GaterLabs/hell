package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

// Helper classes for relational queries
data class StoreWithConsignments(
    @Embedded val store: StoreEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "storeId"
    )
    val consignments: List<StoreConsignmentEntity>
)

data class TransactionWithItems(
    @Embedded val transaction: VisitTransactionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "transactionId"
    )
    val items: List<TransactionItemEntity>
)

data class ConsignmentProductDetail(
    val storeId: Long,
    val productId: Long,
    val productName: String,
    val unitName: String,
    val packSize: Int,
    val costPrice: Double,
    val sellPrice: Double,
    val currentDroppedQuantity: Int
)

data class InventoryBucketSummary(
    val bucket: String,
    val totalPcs: Int
)

data class InventoryProductBalance(
    val productId: Long,
    val totalPcs: Int
)

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY id ASC")
    suspend fun getAllProductsSnapshot(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int
}

@Dao
interface RouteDao {
    @Query("SELECT * FROM routes ORDER BY sortOrder ASC, id ASC")
    fun getAllRoutes(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllRoutesSnapshot(): List<RouteEntity>

    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun getRouteById(id: Long): RouteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutes(routes: List<RouteEntity>)

    @Update
    suspend fun updateRoute(route: RouteEntity)

    @Delete
    suspend fun deleteRoute(route: RouteEntity)

    @Query("DELETE FROM routes")
    suspend fun deleteAllRoutes()

    @Query("SELECT COUNT(*) FROM routes")
    suspend fun getRouteCount(): Int
}

@Dao
interface StoreDao {
    @Query("SELECT * FROM stores ORDER BY orderIndex ASC, name ASC")
    fun getAllStores(): Flow<List<StoreEntity>>

    @Query("SELECT * FROM stores ORDER BY orderIndex ASC, name ASC")
    suspend fun getAllStoresSnapshot(): List<StoreEntity>

    @Query("SELECT * FROM stores WHERE routeId = :routeId ORDER BY orderIndex ASC, name ASC")
    fun getStoresByRoute(routeId: Long): Flow<List<StoreEntity>>

    @Query("SELECT * FROM stores WHERE id = :id")
    suspend fun getStoreById(id: Long): StoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStore(store: StoreEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStores(stores: List<StoreEntity>)

    @Update
    suspend fun updateStore(store: StoreEntity)

    @Delete
    suspend fun deleteStore(store: StoreEntity)

    @Query("DELETE FROM stores")
    suspend fun deleteAllStores()

    @Query("UPDATE stores SET outstandingDebt = :newDebt, debtSince = :debtSince WHERE id = :storeId")
    suspend fun updateStoreDebt(storeId: Long, newDebt: Double, debtSince: Long?)

    @Query("UPDATE stores SET lastVisitedDate = :timestamp, isVisitedToday = :visited WHERE id = :storeId")
    suspend fun updateStoreVisitStatus(storeId: Long, timestamp: Long, visited: Boolean)

    @Query("UPDATE stores SET isVisitedToday = 0")
    suspend fun resetDailyVisitStatus()

    @Query("SELECT COUNT(*) FROM stores")
    suspend fun getStoreCount(): Int
}

@Dao
interface StoreConsignmentDao {
    @Query("SELECT * FROM store_consignments WHERE storeId = :storeId")
    fun getConsignmentsForStore(storeId: Long): Flow<List<StoreConsignmentEntity>>

    @Query("SELECT * FROM store_consignments")
    suspend fun getAllConsignmentsSnapshot(): List<StoreConsignmentEntity>

    @Query("""
        SELECT 
            sc.storeId,
            sc.productId,
            p.name AS productName,
            p.unitName AS unitName,
            p.packSize AS packSize,
            p.costPrice AS costPrice,
            p.sellPrice AS sellPrice,
            sc.currentDroppedQuantity AS currentDroppedQuantity
        FROM store_consignments sc
        INNER JOIN products p ON sc.productId = p.id
        WHERE sc.storeId = :storeId
        ORDER BY p.name ASC
    """)
    fun getConsignmentDetailsForStore(storeId: Long): Flow<List<ConsignmentProductDetail>>

    @Query("""
        SELECT 
            sc.storeId,
            sc.productId,
            p.name AS productName,
            p.unitName AS unitName,
            p.packSize AS packSize,
            p.costPrice AS costPrice,
            p.sellPrice AS sellPrice,
            sc.currentDroppedQuantity AS currentDroppedQuantity
        FROM store_consignments sc
        INNER JOIN products p ON sc.productId = p.id
        ORDER BY sc.storeId ASC, p.name ASC
    """)
    fun getAllConsignmentDetails(): Flow<List<ConsignmentProductDetail>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConsignment(consignment: StoreConsignmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsignments(consignments: List<StoreConsignmentEntity>)

    @Query("DELETE FROM store_consignments WHERE storeId = :storeId AND productId = :productId")
    suspend fun deleteConsignment(storeId: Long, productId: Long)

    @Query("DELETE FROM store_consignments")
    suspend fun deleteAllConsignments()

    @Query("SELECT SUM(currentDroppedQuantity) FROM store_consignments")
    fun getTotalFieldConsignmentQty(): Flow<Int?>

    @Query("""
        SELECT 
            p.id AS productId,
            p.name AS productName,
            p.unitName AS unitName,
            p.packSize AS packSize,
            p.costPrice AS costPrice,
            p.sellPrice AS sellPrice,
            COALESCE(SUM(sc.currentDroppedQuantity), 0) AS totalFieldQuantity
        FROM products p
        LEFT JOIN store_consignments sc ON p.id = sc.productId
        GROUP BY p.id
    """)
    fun getFieldStockSummaryByProduct(): Flow<List<ProductFieldStockSummary>>
}

data class ProductFieldStockSummary(
    val productId: Long,
    val productName: String,
    val unitName: String,
    val packSize: Int,
    val costPrice: Double,
    val sellPrice: Double,
    val totalFieldQuantity: Int
)

@Dao
interface VanLoadDao {
    @Query("SELECT * FROM van_loads WHERE dateString = :dateString")
    fun getLoadsForDate(dateString: String): Flow<List<VanLoadEntity>>

    @Query("SELECT * FROM van_loads WHERE dateString = :dateString AND productId = :productId")
    suspend fun getLoadForProductOnDate(dateString: String, productId: Long): VanLoadEntity?

    @Query("SELECT * FROM van_loads WHERE dateString = :dateString AND productId = :productId ORDER BY id ASC")
    suspend fun getLoadsForProductOnDate(dateString: String, productId: Long): List<VanLoadEntity>

    @Query("SELECT * FROM van_loads ORDER BY dateString DESC, id ASC")
    suspend fun getAllVanLoadsSnapshot(): List<VanLoadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLoad(load: VanLoadEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoads(loads: List<VanLoadEntity>)

    @Update
    suspend fun updateLoad(load: VanLoadEntity)

    @Query("UPDATE van_loads SET returnedQty = :returned, damagedQty = :damaged, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateVanLoadReturn(id: Long, returned: Int, damaged: Int, updatedAt: Long)

    @Query("UPDATE van_loads SET returnedQty = :returned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateVanLoadReturned(id: Long, returned: Int, updatedAt: Long)

    @Delete
    suspend fun deleteVanLoad(load: VanLoadEntity)

    @Query("DELETE FROM van_loads")
    suspend fun deleteAllVanLoads()

    @Query("SELECT * FROM van_loads WHERE dateString = :dateString")
    suspend fun getLoadsForDateSnapshot(dateString: String): List<VanLoadEntity>

    @Query("SELECT DISTINCT dateString FROM van_loads ORDER BY dateString DESC")
    fun getAllLoadDates(): Flow<List<String>>

    @Query("SELECT * FROM van_loads ORDER BY dateString DESC, id ASC")
    fun getAllLoads(): Flow<List<VanLoadEntity>>

    @Query("UPDATE van_loads SET isSetored = 1, setorAmount = :setorAmount, setorTimestamp = :setorTimestamp, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markAsSetored(id: Long, setorAmount: Double, setorTimestamp: Long, updatedAt: Long)

    @Query("UPDATE van_loads SET isSetored = 0, setorAmount = 0, setorTimestamp = NULL, updatedAt = :updatedAt WHERE id = :id")
    suspend fun unmarkSetored(id: Long, updatedAt: Long)

    @Query("SELECT SUM(initialLoadedQty * costPerPack) FROM van_loads WHERE dateString = :dateString AND isSetored = 0")
    suspend fun getTotalUnsetoredForDate(dateString: String): Double?

    @Query("SELECT SUM(initialLoadedQty * costPerPack) FROM van_loads WHERE dateString = :dateString")
    suspend fun getTotalLoadCostForDate(dateString: String): Double?
}

@Dao
interface InventoryMovementDao {
    @Insert
    suspend fun insertMovement(movement: InventoryMovementEntity): Long

    @Insert
    suspend fun insertMovements(movements: List<InventoryMovementEntity>)

    @Query("SELECT * FROM inventory_movements ORDER BY createdAt ASC, id ASC")
    suspend fun getAllMovementsSnapshot(): List<InventoryMovementEntity>

    @Query("SELECT bucket, COALESCE(SUM(quantityPcs), 0) AS totalPcs FROM inventory_movements GROUP BY bucket")
    fun observeBucketSummaries(): Flow<List<InventoryBucketSummary>>

    @Query("SELECT productId, COALESCE(SUM(quantityPcs), 0) AS totalPcs FROM inventory_movements WHERE bucket = :bucket GROUP BY productId HAVING SUM(quantityPcs) > 0 ORDER BY productId ASC")
    fun observeProductBalances(bucket: String): Flow<List<InventoryProductBalance>>

    @Query("SELECT COALESCE(SUM(quantityPcs), 0) FROM inventory_movements WHERE productId = :productId AND bucket = :bucket")
    suspend fun getBalance(productId: Long, bucket: String): Int

    @Query("DELETE FROM inventory_movements")
    suspend fun deleteAllMovements()
}

@Dao
interface DailyClosingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveClosing(closing: DailyClosingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClosings(closings: List<DailyClosingEntity>)

    @Query("SELECT * FROM daily_closings ORDER BY dateString DESC")
    suspend fun getAllClosingsSnapshot(): List<DailyClosingEntity>

    @Query("SELECT * FROM daily_closings WHERE dateString = :dateString")
    fun observeClosing(dateString: String): Flow<DailyClosingEntity?>

    @Query("DELETE FROM daily_closings")
    suspend fun deleteAllClosings()
}

@Dao
interface DebtWriteOffDao {
    @Insert
    suspend fun insert(writeOff: DebtWriteOffEntity): Long

    @Query("SELECT * FROM debt_write_offs ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DebtWriteOffEntity>>

    @Query("SELECT * FROM debt_write_offs ORDER BY createdAt ASC, id ASC")
    suspend fun getAllSnapshot(): List<DebtWriteOffEntity>

    @Query("DELETE FROM debt_write_offs")
    suspend fun deleteAll()
}

@Dao
interface BusinessPartnerDao {
    @Query("SELECT * FROM business_partners WHERE kind = :kind ORDER BY name ASC")
    fun observeByKind(kind: String): Flow<List<BusinessPartnerEntity>>

    @Query("SELECT * FROM business_partners ORDER BY kind ASC, name ASC")
    fun observeAll(): Flow<List<BusinessPartnerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BusinessPartnerEntity): Long

    @Update
    suspend fun update(entity: BusinessPartnerEntity)

    @Delete
    suspend fun delete(entity: BusinessPartnerEntity)

    @Query("SELECT * FROM business_partners ORDER BY id ASC")
    suspend fun getAllSnapshot(): List<BusinessPartnerEntity>

    @Query("DELETE FROM business_partners")
    suspend fun deleteAll()
}

@Dao
interface StorePriceOverrideDao {
    @Query("SELECT * FROM store_price_overrides ORDER BY storeId ASC, productId ASC")
    fun observeAll(): Flow<List<StorePriceOverrideEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: StorePriceOverrideEntity)

    @Query("DELETE FROM store_price_overrides WHERE storeId = :storeId AND productId = :productId")
    suspend fun delete(storeId: Long, productId: Long)

    @Query("SELECT * FROM store_price_overrides ORDER BY storeId ASC, productId ASC")
    suspend fun getAllSnapshot(): List<StorePriceOverrideEntity>

    @Query("DELETE FROM store_price_overrides")
    suspend fun deleteAll()
}

@Dao
interface AuditEventDao {
    @Insert
    suspend fun insert(event: AuditEventEntity): Long

    @Query("SELECT * FROM audit_events ORDER BY createdAt DESC LIMIT 500")
    fun observeRecent(): Flow<List<AuditEventEntity>>

    @Query("SELECT * FROM audit_events ORDER BY createdAt ASC, id ASC")
    suspend fun getAllSnapshot(): List<AuditEventEntity>

    @Query("DELETE FROM audit_events")
    suspend fun deleteAll()
}

@Dao
interface TransactionDao {
    @Transaction
    @Query("SELECT * FROM visit_transactions ORDER BY visitTimestamp DESC")
    fun getAllTransactionsWithItems(): Flow<List<TransactionWithItems>>

    @Transaction
    @Query("SELECT * FROM visit_transactions ORDER BY visitTimestamp DESC")
    suspend fun getAllTransactionsWithItemsSnapshot(): List<TransactionWithItems>

    @Query("SELECT * FROM visit_transactions ORDER BY visitTimestamp DESC")
    suspend fun getAllTransactionsSnapshot(): List<VisitTransactionEntity>

    @Query("SELECT * FROM transaction_items")
    suspend fun getAllTransactionItemsSnapshot(): List<TransactionItemEntity>

    @Transaction
    @Query("SELECT * FROM visit_transactions WHERE routeId = :routeId ORDER BY visitTimestamp DESC")
    fun getTransactionsByRoute(routeId: Long): Flow<List<TransactionWithItems>>

    @Transaction
    @Query("SELECT * FROM visit_transactions WHERE storeId = :storeId ORDER BY visitTimestamp DESC")
    fun getTransactionsByStore(storeId: Long): Flow<List<TransactionWithItems>>

    @Transaction
    @Query("SELECT * FROM visit_transactions WHERE visitTimestamp >= :startTimestamp AND visitTimestamp < :endTimestamp ORDER BY visitTimestamp DESC")
    fun getTransactionsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<TransactionWithItems>>

    @Transaction
    @Query("SELECT * FROM visit_transactions WHERE visitTimestamp >= :startTimestamp AND visitTimestamp < :endTimestamp ORDER BY visitTimestamp DESC")
    suspend fun getTransactionsByDateRangeSnapshot(startTimestamp: Long, endTimestamp: Long): List<TransactionWithItems>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: VisitTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<VisitTransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItemEntity>)

    @Query("DELETE FROM visit_transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM transaction_items")
    suspend fun deleteAllTransactionItems()

    @Query("SELECT * FROM visit_transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): VisitTransactionEntity?

    @Query("SELECT COUNT(*) FROM visit_transactions")
    fun getTransactionCount(): Flow<Int>

    @Query("SELECT SUM(totalAmountDue) FROM visit_transactions")
    fun getTotalRevenue(): Flow<Double?>

    @Query("SELECT SUM(totalProfit) FROM visit_transactions")
    fun getTotalProfit(): Flow<Double?>

    @Query("SELECT SUM(totalItemsSold) FROM visit_transactions")
    fun getTotalItemsSold(): Flow<Int?>
}

@Dao
interface VanReturnDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturn(returnEntity: VanReturnEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturns(returns: List<VanReturnEntity>)

    @Query("SELECT * FROM van_returns WHERE dateString = :dateString")
    fun getReturnsForDate(dateString: String): Flow<List<VanReturnEntity>>

    @Query("SELECT * FROM van_returns WHERE dateString = :dateString AND productId = :productId")
    suspend fun getReturnsForProductOnDate(dateString: String, productId: Long): List<VanReturnEntity>

    @Query("SELECT * FROM van_returns ORDER BY dateString DESC, timestamp DESC")
    suspend fun getAllReturnsSnapshot(): List<VanReturnEntity>

    @Query("DELETE FROM van_returns")
    suspend fun deleteAllReturns()
}

@Dao
interface GpsTrackDao {
    @Insert
    suspend fun insertPoint(point: GpsTrackEntity): Long

    @Insert
    suspend fun insertPoints(points: List<GpsTrackEntity>)

    @Query("SELECT * FROM gps_tracks WHERE routeId = :routeId AND dateString = :dateString ORDER BY timestamp ASC")
    fun getPointsForRouteAndDate(routeId: Long, dateString: String): Flow<List<GpsTrackEntity>>

    @Query("SELECT * FROM gps_tracks WHERE routeId = :routeId AND dateString = :dateString ORDER BY timestamp ASC")
    suspend fun getPointsForRouteAndDateSnapshot(routeId: Long, dateString: String): List<GpsTrackEntity>

    @Query("SELECT COUNT(*) FROM gps_tracks WHERE routeId = :routeId AND dateString = :dateString")
    fun getPointCountForRouteAndDate(routeId: Long, dateString: String): Flow<Int>

    @Query("DELETE FROM gps_tracks WHERE routeId = :routeId AND dateString = :dateString")
    suspend fun deletePointsForRouteAndDate(routeId: Long, dateString: String)

    @Query("DELETE FROM gps_tracks")
    suspend fun deleteAllPoints()
}

@Dao
interface GpsSessionDao {
    @Insert
    suspend fun insertSession(session: GpsSessionEntity): Long

    @Query("SELECT * FROM gps_sessions WHERE routeId = :routeId AND dateString = :dateString LIMIT 1")
    suspend fun getSessionForRouteAndDate(routeId: Long, dateString: String): GpsSessionEntity?

    @Query("SELECT * FROM gps_sessions WHERE routeId = :routeId AND dateString = :dateString LIMIT 1")
    fun observeSessionForRouteAndDate(routeId: Long, dateString: String): Flow<GpsSessionEntity?>

    @Query("UPDATE gps_sessions SET isActive = 0, endTime = :endTime, totalPoints = :totalPoints, totalDistanceMeters = :totalDistance WHERE id = :id")
    suspend fun stopSession(id: Long, endTime: Long, totalPoints: Int, totalDistance: Float)

    @Query("UPDATE gps_sessions SET totalPoints = :totalPoints, totalDistanceMeters = :totalDistance WHERE id = :id")
    suspend fun updateSessionStats(id: Long, totalPoints: Int, totalDistance: Float)

    @Query("SELECT * FROM gps_sessions ORDER BY dateString DESC, startTime DESC")
    suspend fun getAllSessionsSnapshot(): List<GpsSessionEntity>

    @Query("DELETE FROM gps_sessions")
    suspend fun deleteAllSessions()
}
