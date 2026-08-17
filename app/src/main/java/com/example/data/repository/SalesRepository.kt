package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import android.location.Location

data class ClosingLoadInput(
    val productId: Long,
    val loadedBoxes: Int,
    val freshRemainingBoxes: Int,
    val costPerBox: Double
)

class SalesRepository(private val database: AppDatabase) {

    private val productDao = database.productDao()
    private val routeDao = database.routeDao()
    private val storeDao = database.storeDao()
    private val consignmentDao = database.consignmentDao()
    private val vanLoadDao = database.vanLoadDao()
    private val inventoryMovementDao = database.inventoryMovementDao()
    private val dailyClosingDao = database.dailyClosingDao()
    private val debtWriteOffDao = database.debtWriteOffDao()
    private val businessPartnerDao = database.businessPartnerDao()
    private val storePriceOverrideDao = database.storePriceOverrideDao()
    private val auditEventDao = database.auditEventDao()
    private val vanReturnDao = database.vanReturnDao()
    private val transactionDao = database.transactionDao()
    private val gpsTrackDao = database.gpsTrackDao()
    private val gpsSessionDao = database.gpsSessionDao()

    // Products
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val activeProducts: Flow<List<ProductEntity>> = productDao.getAllActiveProducts()

    suspend fun saveProduct(product: ProductEntity): Long = withContext(Dispatchers.IO) {
        if (product.id == 0L) {
            productDao.insertProduct(product)
        } else {
            productDao.updateProduct(product)
            product.id
        }
    }

    suspend fun deleteProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        productDao.deleteProduct(product)
    }

    // Routes
    val allRoutes: Flow<List<RouteEntity>> = routeDao.getAllRoutes()

    suspend fun saveRoute(route: RouteEntity): Long = withContext(Dispatchers.IO) {
        if (route.id == 0L) {
            routeDao.insertRoute(route)
        } else {
            routeDao.updateRoute(route)
            route.id
        }
    }

    suspend fun deleteRoute(route: RouteEntity) = withContext(Dispatchers.IO) {
        routeDao.deleteRoute(route)
    }

    // Stores
    val allStores: Flow<List<StoreEntity>> = storeDao.getAllStores()

    fun getStoresByRoute(routeId: Long): Flow<List<StoreEntity>> =
        storeDao.getStoresByRoute(routeId)

    suspend fun saveStore(store: StoreEntity): Long = withContext(Dispatchers.IO) {
        if (store.id == 0L) {
            storeDao.insertStore(store).also { auditEventDao.insert(AuditEventEntity(eventType = "STORE_CREATED", referenceId = it, description = "Warung dibuat: ${store.name}")) }
        } else {
            storeDao.updateStore(store)
            auditEventDao.insert(AuditEventEntity(eventType = "STORE_UPDATED", referenceId = store.id, description = "Warung diperbarui: ${store.name}"))
            store.id
        }
    }

    suspend fun deleteStore(store: StoreEntity) = withContext(Dispatchers.IO) {
        storeDao.deleteStore(store)
        auditEventDao.insert(AuditEventEntity(eventType = "STORE_DELETED", referenceId = store.id, description = "Warung dihapus: ${store.name}"))
    }

    suspend fun resetDailyVisitStatus() = withContext(Dispatchers.IO) {
        storeDao.resetDailyVisitStatus()
    }

    val debtWriteOffs: Flow<List<DebtWriteOffEntity>> = debtWriteOffDao.observeAll()

    suspend fun writeOffStoreDebt(store: StoreEntity, reason: String) = withContext(Dispatchers.IO) {
        require(store.outstandingDebt > 0) { "Warung tidak memiliki piutang untuk dihapus buku" }
        database.withTransaction {
            debtWriteOffDao.insert(
                DebtWriteOffEntity(
                    storeId = store.id,
                    amount = store.outstandingDebt,
                    reason = reason.trim().ifBlank { "Write-off piutang" }
                )
            )
            storeDao.updateStoreDebt(store.id, 0.0, null)
            auditEventDao.insert(
                AuditEventEntity(
                    eventType = "DEBT_WRITE_OFF",
                    referenceId = store.id,
                    description = "Piutang ${store.name} dihapus buku: ${store.outstandingDebt}"
                )
            )
        }
    }

    val businessPartners: Flow<List<BusinessPartnerEntity>> = businessPartnerDao.observeAll()
    val priceOverrides: Flow<List<StorePriceOverrideEntity>> = storePriceOverrideDao.observeAll()

    suspend fun saveBusinessPartner(entity: BusinessPartnerEntity) = withContext(Dispatchers.IO) {
        if (entity.id == 0L) businessPartnerDao.insert(entity) else businessPartnerDao.update(entity).let { entity.id }
    }

    suspend fun deleteBusinessPartner(entity: BusinessPartnerEntity) = withContext(Dispatchers.IO) {
        businessPartnerDao.delete(entity)
    }

    suspend fun savePriceOverride(entity: StorePriceOverrideEntity) = withContext(Dispatchers.IO) {
        storePriceOverrideDao.save(entity)
    }

    suspend fun deletePriceOverride(storeId: Long, productId: Long) = withContext(Dispatchers.IO) {
        storePriceOverrideDao.delete(storeId, productId)
    }

    val recentAuditEvents: Flow<List<AuditEventEntity>> = auditEventDao.observeRecent()

    suspend fun logAudit(eventType: String, description: String, referenceId: Long? = null) = withContext(Dispatchers.IO) {
        auditEventDao.insert(AuditEventEntity(eventType = eventType, description = description, referenceId = referenceId))
    }

    // Consignments
    val allConsignmentDetails: Flow<List<ConsignmentProductDetail>> = consignmentDao.getAllConsignmentDetails()

    fun getConsignmentsForStore(storeId: Long): Flow<List<ConsignmentProductDetail>> =
        consignmentDao.getConsignmentDetailsForStore(storeId)

    val totalFieldConsignmentQty: Flow<Int?> = consignmentDao.getTotalFieldConsignmentQty()
    val fieldStockSummary: Flow<List<ProductFieldStockSummary>> = consignmentDao.getFieldStockSummaryByProduct()

    suspend fun addOrUpdateConsignment(storeId: Long, productId: Long, qty: Int) = withContext(Dispatchers.IO) {
        consignmentDao.insertOrUpdateConsignment(
            StoreConsignmentEntity(
                storeId = storeId,
                productId = productId,
                currentDroppedQuantity = qty
            )
        )
    }

    suspend fun removeConsignment(storeId: Long, productId: Long) = withContext(Dispatchers.IO) {
        consignmentDao.deleteConsignment(storeId, productId)
    }

    // Van Cargo Loads
    fun getVanLoadsForDate(dateStr: String): Flow<List<VanLoadEntity>> =
        vanLoadDao.getLoadsForDate(dateStr)

    suspend fun saveVanLoad(load: VanLoadEntity): Long = withContext(Dispatchers.IO) {
        database.withTransaction {
            val existingLoads = vanLoadDao.getLoadsForProductOnDate(load.dateString, load.productId)
            if (existingLoads.isEmpty()) {
                vanLoadDao.insertOrUpdateLoad(load)
            } else {
                val allLoads = existingLoads + load
                val totalQty = allLoads.sumOf { it.initialLoadedQty }
                val weightedCost = allLoads.sumOf { it.initialLoadedQty * it.costPerPack } /
                        totalQty.coerceAtLeast(1)
                val primary = existingLoads.first()
                val merged = primary.copy(
                    initialLoadedQty = totalQty,
                    returnedQty = allLoads.sumOf { it.returnedQty },
                    damagedQty = allLoads.sumOf { it.damagedQty },
                    costPerPack = weightedCost,
                    isSetored = false,
                    setorAmount = 0.0,
                    setorTimestamp = null,
                    notes = allLoads.map { it.notes }.filter { it.isNotBlank() }.distinct().joinToString("; "),
                    updatedAt = System.currentTimeMillis()
                )
                vanLoadDao.updateLoad(merged)
                existingLoads.drop(1).forEach { vanLoadDao.deleteVanLoad(it) }
                merged.id
            }
        }
    }

    suspend fun normalizeVanLoadsForDate(dateString: String) = withContext(Dispatchers.IO) {
        database.withTransaction {
            vanLoadDao.getLoadsForDateSnapshot(dateString)
                .groupBy { it.productId }
                .values
                .filter { it.size > 1 }
                .forEach { duplicateLoads ->
                    val primary = duplicateLoads.first()
                    val totalQty = duplicateLoads.sumOf { it.initialLoadedQty }
                    val weightedCost = duplicateLoads.sumOf { it.initialLoadedQty * it.costPerPack } /
                            totalQty.coerceAtLeast(1)
                    vanLoadDao.updateLoad(
                        primary.copy(
                            initialLoadedQty = totalQty,
                            returnedQty = duplicateLoads.sumOf { it.returnedQty },
                            damagedQty = duplicateLoads.sumOf { it.damagedQty },
                            costPerPack = weightedCost,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    duplicateLoads.drop(1).forEach { vanLoadDao.deleteVanLoad(it) }
                }
        }
    }

    suspend fun updateVanLoadReturn(id: Long, returned: Int, damaged: Int) = withContext(Dispatchers.IO) {
        vanLoadDao.updateVanLoadReturn(id, returned, damaged, System.currentTimeMillis())
    }

    suspend fun deleteVanLoad(load: VanLoadEntity) = withContext(Dispatchers.IO) {
        vanLoadDao.deleteVanLoad(load)
    }

    val allLoadDates: Flow<List<String>> = vanLoadDao.getAllLoadDates()

    val allLoads: Flow<List<VanLoadEntity>> = vanLoadDao.getAllLoads()

    val inventoryBucketSummaries: Flow<List<InventoryBucketSummary>> =
        inventoryMovementDao.observeBucketSummaries()

    val bsProductBalances: Flow<List<InventoryProductBalance>> =
        inventoryMovementDao.observeProductBalances("BS_UNSORTED")

    fun observeDailyClosing(dateString: String): Flow<DailyClosingEntity?> =
        dailyClosingDao.observeClosing(dateString)

    suspend fun closeDaily(
        dateString: String,
        loads: List<ClosingLoadInput>,
        cashCollected: Double,
        notes: String = ""
    ) = withContext(Dispatchers.IO) {
        require(loads.isNotEmpty()) { "Belum ada muatan untuk ditutup" }
        loads.forEach {
            require(it.freshRemainingBoxes in 0..it.loadedBoxes) {
                "Sisa fresh tidak boleh melebihi muatan"
            }
        }
        val totalLoaded = loads.sumOf { it.loadedBoxes }
        val freshRemaining = loads.sumOf { it.freshRemainingBoxes }
        val factoryDue = loads.sumOf {
            (it.loadedBoxes - it.freshRemainingBoxes) * it.costPerBox
        }
        val cash = cashCollected.coerceAtLeast(0.0)
        dailyClosingDao.saveClosing(
            DailyClosingEntity(
                dateString = dateString,
                totalLoadedBoxes = totalLoaded,
                freshRemainingBoxes = freshRemaining,
                factoryDue = factoryDue,
                cashCollected = cash,
                shortage = (factoryDue - cash).coerceAtLeast(0.0),
                notes = notes
            )
        )
    }

    suspend fun recordInventoryMovement(movement: InventoryMovementEntity) =
        withContext(Dispatchers.IO) {
            inventoryMovementDao.insertMovement(movement)
        }

    suspend fun sortBs(productId: Long, goodPcs: Int, damagedPcs: Int) =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val good = goodPcs.coerceAtLeast(0)
                val damaged = damagedPcs.coerceAtLeast(0)
                val total = good + damaged
                val available = inventoryMovementDao.getBalance(productId, "BS_UNSORTED")
                require(total <= available) { "Jumlah sortir melebihi stok BS tersedia" }
                require(total > 0) { "Jumlah sortir harus lebih dari 0" }

                inventoryMovementDao.insertMovement(
                    InventoryMovementEntity(
                        productId = productId,
                        bucket = "BS_UNSORTED",
                        quantityPcs = -total,
                        movementType = "BS_SORTED"
                    )
                )
                if (good > 0) {
                    inventoryMovementDao.insertMovement(
                        InventoryMovementEntity(
                            productId = productId,
                            bucket = "PRIVATE_READY",
                            quantityPcs = good,
                            movementType = "BS_REPACK"
                        )
                    )
                }
                if (damaged > 0) {
                    inventoryMovementDao.insertMovement(
                        InventoryMovementEntity(
                            productId = productId,
                            bucket = "PRIVATE_DAMAGED",
                            quantityPcs = damaged,
                            movementType = "BS_WRITE_OFF"
                        )
                    )
                }
            }
        }

    suspend fun getLoadsForDateSnapshot(dateString: String): List<VanLoadEntity> = withContext(Dispatchers.IO) {
        vanLoadDao.getLoadsForDateSnapshot(dateString)
    }

    suspend fun markLoadAsSetored(load: VanLoadEntity) = withContext(Dispatchers.IO) {
        val setorAmount = load.initialLoadedQty * load.costPerPack
        vanLoadDao.markAsSetored(
            id = load.id,
            setorAmount = setorAmount,
            setorTimestamp = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun unmarkLoadSetored(load: VanLoadEntity) = withContext(Dispatchers.IO) {
        vanLoadDao.unmarkSetored(load.id, System.currentTimeMillis())
    }

    suspend fun getTotalUnsetoredForDate(dateString: String): Double = withContext(Dispatchers.IO) {
        vanLoadDao.getTotalUnsetoredForDate(dateString) ?: 0.0
    }

    suspend fun getTotalLoadCostForDate(dateString: String): Double = withContext(Dispatchers.IO) {
        vanLoadDao.getTotalLoadCostForDate(dateString) ?: 0.0
    }

    suspend fun syncVanLoadAfterReconciliation(dateString: String, reconciledItems: List<ReconciliationItemInput>) {
        for (item in reconciledItems) {
            val existingLoad = vanLoadDao.getLoadForProductOnDate(dateString, item.productId)
            if (existingLoad != null) {
                // Sum all remaining stock returned from ALL stores for this product today
                val returnsToday = vanReturnDao.getReturnsForProductOnDate(dateString, item.productId)
                val totalReturned = returnsToday.sumOf { it.returnedQty }
                vanLoadDao.updateVanLoadReturned(existingLoad.id, totalReturned, System.currentTimeMillis())
            }
        }
    }

    // Returns (barang sisa kembali ke muatan)
    fun getVanReturnsForDate(dateString: String): Flow<List<VanReturnEntity>> = vanReturnDao.getReturnsForDate(dateString)

    // Transactions & Analytics
    val allTransactions: Flow<List<TransactionWithItems>> = transactionDao.getAllTransactionsWithItems()
    val totalRevenue: Flow<Double?> = transactionDao.getTotalRevenue()
    val totalProfit: Flow<Double?> = transactionDao.getTotalProfit()
    val totalItemsSold: Flow<Int?> = transactionDao.getTotalItemsSold()

    fun getTransactionsForDate(dateString: String): Flow<List<TransactionWithItems>> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateString) ?: return flowOf(emptyList())
        val cal = java.util.Calendar.getInstance().apply {
            time = date
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startTs = cal.timeInMillis
        cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        val endTs = cal.timeInMillis
        return transactionDao.getTransactionsByDateRange(startTs, endTs)
    }

    fun getTransactionsByRoute(routeId: Long): Flow<List<TransactionWithItems>> =
        transactionDao.getTransactionsByRoute(routeId)

    fun getTransactionsByStore(storeId: Long): Flow<List<TransactionWithItems>> =
        transactionDao.getTransactionsByStore(storeId)

    fun getTransactionsByDateRange(start: Long, end: Long): Flow<List<TransactionWithItems>> =
        transactionDao.getTransactionsByDateRange(start, end)

    /**
     * Executes the automatic consignment reconciliation transaction:
     * 1. Records the visit transaction & individual item breakdowns (Previous, Remaining, Sold, Subtotal, Profit, New Dropped)
     * 2. Updates active store consignment quantities for the next visit
     * 3. Updates store outstanding debt and marked visit status
     */
    suspend fun executeVisitReconciliation(
        store: StoreEntity,
        route: RouteEntity,
        reconciledItems: List<ReconciliationItemInput>,
        amountPaid: Double,
        previousDebtPaid: Double,
        paymentStatus: String,
        notes: String,
        visitLocation: Location,
        gpsAccuracyThreshold: Int
    ): VisitTransactionEntity = withContext(Dispatchers.IO) {
        database.withTransaction {
        require(visitLocation.hasAccuracy() && visitLocation.accuracy <= gpsAccuracyThreshold) {
            "Akurasi GPS tidak memenuhi batas ${gpsAccuracyThreshold}m"
        }
        val storeLatitude = store.latitude ?: throw IllegalArgumentException("Koordinat GPS warung belum tersimpan")
        val storeLongitude = store.longitude ?: throw IllegalArgumentException("Koordinat GPS warung belum tersimpan")
        val distanceResult = FloatArray(1)
        Location.distanceBetween(visitLocation.latitude, visitLocation.longitude, storeLatitude, storeLongitude, distanceResult)
        val distanceMeters = distanceResult[0]
        require(distanceMeters <= 100.0f) {
            "Posisi terlalu jauh dari warung (${distanceMeters.toInt()}m), maksimal 100m"
        }
        val dateCode = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val randomDigits = (100..999).random()
        val receiptNumber = "STR-$dateCode-$randomDigits"

        val totalAmountDue = reconciledItems.sumOf { it.soldQty * it.sellPrice }
        val totalProfit = reconciledItems.sumOf { it.soldQty * (it.sellPrice - it.costPrice) }
        val totalItemsSold = reconciledItems.sumOf { it.soldQty }

        val totalOwedBeforePayment = store.outstandingDebt + totalAmountDue
        val totalPaid = amountPaid.coerceAtLeast(0.0)
        val updatedDebt = (totalOwedBeforePayment - totalPaid).coerceAtLeast(0.0)
        require(store.status == "ACTIVE") { "Warung tidak aktif untuk transaksi" }
        require(updatedDebt <= store.creditLimit) { "Transaksi melewati limit bon warung" }

        // Break down payment between past debt & current sales for receipt reporting
        val actualOldDebtPaid = if (previousDebtPaid > 0) minOf(store.outstandingDebt, previousDebtPaid) else minOf(store.outstandingDebt, totalPaid)
        val paymentForNewSales = (totalPaid - actualOldDebtPaid).coerceAtLeast(0.0)
        val netNewDebt = (totalAmountDue - paymentForNewSales).coerceAtLeast(0.0)

        val transaction = VisitTransactionEntity(
            receiptNumber = receiptNumber,
            storeId = store.id,
            storeName = store.name,
            routeId = route.id,
            routeName = route.name,
            visitTimestamp = System.currentTimeMillis(),
            totalAmountDue = totalAmountDue,
            amountPaid = totalPaid,
            previousDebtPaid = actualOldDebtPaid,
            newDebtAdded = netNewDebt,
            totalProfit = totalProfit,
            totalItemsSold = totalItemsSold,
            paymentStatus = paymentStatus,
            notes = notes,
            visitLatitude = visitLocation.latitude,
            visitLongitude = visitLocation.longitude,
            gpsAccuracyMeters = if (visitLocation.hasAccuracy()) visitLocation.accuracy else null,
            gpsDistanceMeters = distanceMeters
        )

        val transactionId = transactionDao.insertTransaction(transaction)

        val itemEntities = reconciledItems.map { item ->
            TransactionItemEntity(
                transactionId = transactionId,
                productId = item.productId,
                productName = item.productName,
                unitName = item.unitName,
                packSize = item.packSize,
                previousStock = item.previousStock,
                remainingStock = item.remainingStock,
                soldQuantity = item.soldQty,
                newDroppedQuantity = item.newDroppedQty,
                sourceBucket = item.sourceBucket,
                costPrice = item.costPrice,
                sellPrice = item.sellPrice,
                subtotalDue = item.soldQty * item.sellPrice,
                subtotalProfit = item.soldQty * (item.sellPrice - item.costPrice)
            )
        }
        transactionDao.insertTransactionItems(itemEntities)

        // Private repack stock is consumed when it is selected as the source
        // of a new drop. Fresh factory stock remains governed by today's load.
        for (item in reconciledItems) {
            if (item.sourceBucket == "PRIVATE_READY" && item.newDroppedQty > 0) {
                val availablePrivate = inventoryMovementDao.getBalance(item.productId, "PRIVATE_READY")
                require(item.newDroppedQty <= availablePrivate) {
                    "Stok pribadi ${item.productName} tidak mencukupi"
                }
                inventoryMovementDao.insertMovement(
                    InventoryMovementEntity(
                        productId = item.productId,
                        bucket = "PRIVATE_READY",
                        quantityPcs = -item.newDroppedQty,
                        movementType = "DROP_TO_STORE",
                        referenceId = store.id,
                        unitCostPerPc = item.costPrice
                    )
                )
            }
        }

        // Update Store Consignments for next visit:
        // Only new drops become the store's consignment — remaining goes back to van
        for (item in reconciledItems) {
            if (item.newDroppedQty > 0) {
                consignmentDao.insertOrUpdateConsignment(
                    StoreConsignmentEntity(
                        storeId = store.id,
                        productId = item.productId,
                        currentDroppedQuantity = item.newDroppedQty,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            } else {
                // No new drop and remaining returned → clear consignment
                if (item.remainingStock <= 0) {
                    consignmentDao.deleteConsignment(store.id, item.productId)
                }
            }
        }

        // Record returned stock per-store (barang sisa kembali ke muatan)
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        for (item in reconciledItems) {
            if (item.remainingStock > 0) {
                vanReturnDao.insertReturn(
                    VanReturnEntity(
                        dateString = dateString,
                        storeId = store.id,
                        storeName = store.name,
                        productId = item.productId,
                        returnedQty = item.remainingStock
                    )
                )
                inventoryMovementDao.insertMovement(
                    InventoryMovementEntity(
                        productId = item.productId,
                        bucket = "BS_UNSORTED",
                        quantityPcs = item.remainingStock,
                        movementType = "RETURN_FROM_STORE",
                        referenceId = store.id,
                        unitCostPerPc = item.costPrice
                    )
                )
            }
        }

        // Update Store Debt & Status
        val debtSince = if (updatedDebt > 0) store.debtSince ?: System.currentTimeMillis() else null
        storeDao.updateStoreDebt(store.id, updatedDebt, debtSince)
        storeDao.updateStoreVisitStatus(store.id, System.currentTimeMillis(), true)

        // Auto-sync Van Load using the same date format stored by VanLoadEntity.
        // The receipt uses yyyyMMdd, while van loads use yyyy-MM-dd.
        syncVanLoadAfterReconciliation(dateString, reconciledItems)

        transaction.copy(id = transactionId)
        }
    }

    // GPS Tracking
    fun getGpsPointsForRouteAndDate(routeId: Long, dateString: String): Flow<List<GpsTrackEntity>> =
        gpsTrackDao.getPointsForRouteAndDate(routeId, dateString)

    fun getGpsPointCountForRouteAndDate(routeId: Long, dateString: String): Flow<Int> =
        gpsTrackDao.getPointCountForRouteAndDate(routeId, dateString)

    suspend fun deleteGpsPointsForRouteAndDate(routeId: Long, dateString: String) = withContext(Dispatchers.IO) {
        gpsTrackDao.deletePointsForRouteAndDate(routeId, dateString)
    }

    fun observeGpsSessionForRouteAndDate(routeId: Long, dateString: String): Flow<GpsSessionEntity?> =
        gpsSessionDao.observeSessionForRouteAndDate(routeId, dateString)

    suspend fun getGpsSessionForRouteAndDate(routeId: Long, dateString: String): GpsSessionEntity? = withContext(Dispatchers.IO) {
        gpsSessionDao.getSessionForRouteAndDate(routeId, dateString)
    }
}

data class ReconciliationItemInput(
    val productId: Long,
    val productName: String,
    val unitName: String,
    val packSize: Int,
    val previousStock: Int,
    val remainingStock: Int,
    val soldQty: Int,
    val newDroppedQty: Int,
    val costPrice: Double,
    val sellPrice: Double,
    val sourceBucket: String = "FRESH_FACTORY"
)
