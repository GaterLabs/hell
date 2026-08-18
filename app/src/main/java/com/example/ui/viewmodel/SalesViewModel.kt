package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ConsignmentProductDetail
import com.example.data.local.InventoryBucketSummary
import com.example.data.local.InventoryProductBalance
import com.example.data.local.ProductFieldStockSummary
import com.example.data.local.TransactionWithItems
import com.example.data.model.*
import com.example.data.repository.ReconciliationItemInput
import com.example.data.repository.ClosingLoadInput
import com.example.data.repository.SalesRepository
import com.example.ui.util.AppLanguage
import com.example.ui.util.AppStrings
import com.example.ui.util.AppThemeMode
import com.example.ui.util.getAppStrings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class DateFilterType(val label: String) {
    TODAY("Hari Ini"),
    LAST_7_DAYS("7 Hari"),
    THIS_WEEK("Minggu Ini"),
    THIS_MONTH("Bulan Ini"),
    ALL_TIME("Semua"),
    CUSTOM_DATE("Pilih Tanggal 📅")
}

data class DailyAnalyticsItem(
    val dateKey: String,
    val displayDate: String,
    val revenue: Double,
    val profit: Double,
    val cost: Double,
    val itemsSold: Int,
    val transactionCount: Int
)

data class TopSellingProductStat(
    val productName: String,
    val unitName: String,
    val packSize: Int,
    val totalSoldPcs: Int,
    val totalRevenue: Double
)

data class AnalyticsSummary(
    val totalRevenue: Double = 0.0,
    val totalProfit: Double = 0.0,
    val totalCost: Double = 0.0,
    val totalItemsSold: Int = 0,
    val totalTransactions: Int = 0,
    val totalOutstandingDebt: Double = 0.0,
    val totalFieldConsignmentStock: Int = 0,
    val totalCapitalDeployed: Double = 0.0,
    val profitMarginPercent: Double = 0.0,
    val topSellingProducts: List<TopSellingProductStat> = emptyList(),
    val topStoresByRevenue: List<Pair<String, Double>> = emptyList(),
    val routePerformances: List<RoutePerf> = emptyList(),
    val dailyBreakdown: List<DailyAnalyticsItem> = emptyList()
)

data class RoutePerf(
    val routeName: String,
    val totalRevenue: Double,
    val totalProfit: Double,
    val storesVisited: Int,
    val totalStores: Int
)

class SalesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SalesRepository
    private val vanLoadUpdateJobs = mutableMapOf<Long, kotlinx.coroutines.Job>()
    val database: AppDatabase
    init {
        database = AppDatabase.getDatabase(application, viewModelScope)
        repository = SalesRepository(database)
        viewModelScope.launch {
            repository.normalizeVanLoadsForDate(
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            )
        }
    }

    private val prefs = application.getSharedPreferences("salestrack_prefs", Context.MODE_PRIVATE)

    val pinEnabled = MutableStateFlow(prefs.getBoolean("security_pin_enabled", false))
    val gpsAccuracyThreshold = MutableStateFlow(prefs.getInt("gps_accuracy_threshold", 20))
    val printerAddress = MutableStateFlow(prefs.getString("printer_address", "") ?: "")

    // Language Preference (Default: English)
    private val savedLangCode = prefs.getString("pref_language", AppLanguage.ENGLISH.code) ?: AppLanguage.ENGLISH.code
    val appLanguage = MutableStateFlow(
        AppLanguage.values().find { it.code == savedLangCode } ?: AppLanguage.ENGLISH
    )
    val appStrings: StateFlow<AppStrings> = appLanguage
        .map { getAppStrings(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, getAppStrings(appLanguage.value))

    fun setLanguage(language: AppLanguage) {
        appLanguage.value = language
        prefs.edit().putString("pref_language", language.code).apply()
    }

    // Theme Mode Preference (Default: SYSTEM)
    private val savedThemeKey = prefs.getString("pref_theme_mode", AppThemeMode.SYSTEM.key) ?: AppThemeMode.SYSTEM.key
    val themeMode = MutableStateFlow(
        AppThemeMode.values().find { it.key == savedThemeKey } ?: AppThemeMode.SYSTEM
    )

    fun setThemeMode(mode: AppThemeMode) {
        themeMode.value = mode
        prefs.edit().putString("pref_theme_mode", mode.key).apply()
    }

    fun setSecurityPin(pin: String) {
        val clean = pin.filter(Char::isDigit).take(6)
        prefs.edit().putString("security_pin", clean).putBoolean("security_pin_enabled", clean.length >= 4).apply()
        pinEnabled.value = clean.length >= 4
        viewModelScope.launch { repository.logAudit("SECURITY_PIN", if (clean.isEmpty()) "PIN dinonaktifkan" else "PIN aplikasi diperbarui") }
    }

    fun verifySecurityPin(pin: String): Boolean =
        !pinEnabled.value || prefs.getString("security_pin", "") == pin.filter(Char::isDigit)

    fun setGpsAccuracyThreshold(value: Int) {
        val safe = value.coerceIn(5, 200)
        prefs.edit().putInt("gps_accuracy_threshold", safe).apply()
        gpsAccuracyThreshold.value = safe
    }

    fun setPrinterAddress(value: String) {
        prefs.edit().putString("printer_address", value.trim()).apply()
        printerAddress.value = value.trim()
    }

    val recentAuditEvents = repository.recentAuditEvents

    // Master Data Flows
    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRoutes: StateFlow<List<RouteEntity>> = repository.allRoutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStores: StateFlow<List<StoreEntity>> = repository.allStores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionWithItems>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fieldStockSummaries: StateFlow<List<ProductFieldStockSummary>> = repository.fieldStockSummary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryBucketSummaries: StateFlow<List<InventoryBucketSummary>> =
        repository.inventoryBucketSummaries
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bsProductBalances: StateFlow<List<InventoryProductBalance>> =
        repository.bsProductBalances
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storeConsignmentsMap: StateFlow<Map<Long, List<ConsignmentProductDetail>>> = repository.allConsignmentDetails
        .map { list -> list.groupBy { it.storeId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Active Route Selection
    val selectedRouteId = MutableStateFlow<Long?>(null)

    val currentRouteStores: StateFlow<List<StoreEntity>> = combine(
        allStores,
        selectedRouteId
    ) { stores, routeId ->
        if (routeId == null) stores else stores.filter { it.routeId == routeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Keep date-based flows aligned when the app remains open across midnight.
    private val activeDate: StateFlow<String> = flow {
        while (true) {
            emit(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
            delay(60_000L)
        }
    }.distinctUntilChanged().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    )

    val todayClosing: StateFlow<DailyClosingEntity?> = activeDate.flatMapLatest { date ->
        repository.observeDailyClosing(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayDateString: String get() = activeDate.value
    val todayLoads: StateFlow<List<VanLoadEntity>> = activeDate.flatMapLatest { date ->
        repository.getVanLoadsForDate(date)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayDistributedByProduct: StateFlow<Map<Long, Int>> = activeDate.flatMapLatest { date ->
        repository.getTransactionsForDate(date)
    }
        .map { txs ->
            txs.flatMap { it.items }
                .filter { it.newDroppedQuantity > 0 }
                .groupBy { it.productId }
                .mapValues { (_, items) -> items.sumOf { it.newDroppedQuantity } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val todayReturns: StateFlow<List<VanReturnEntity>> = activeDate.flatMapLatest { date ->
        repository.getVanReturnsForDate(date)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Van Load History
    val allLoadDates: StateFlow<List<String>> = repository.allLoadDates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLoads: StateFlow<List<VanLoadEntity>> = repository.allLoads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markAsSetored(load: VanLoadEntity) {
        viewModelScope.launch {
            repository.markLoadAsSetored(load)
        }
    }

    fun unmarkSetored(load: VanLoadEntity) {
        viewModelScope.launch {
            repository.unmarkLoadSetored(load)
        }
    }

    // GPS Tracking
    fun getGpsPoints(routeId: Long, dateString: String = todayDateString) =
        repository.getGpsPointsForRouteAndDate(routeId, dateString)

    fun getGpsPointCount(routeId: Long, dateString: String = todayDateString) =
        repository.getGpsPointCountForRouteAndDate(routeId, dateString)

    fun getGpsSession(routeId: Long, dateString: String = todayDateString) =
        repository.observeGpsSessionForRouteAndDate(routeId, dateString)

    fun deleteGpsHistory(routeId: Long, dateString: String = todayDateString) {
        viewModelScope.launch {
            repository.deleteGpsPointsForRouteAndDate(routeId, dateString)
        }
    }

    // Date Filter for Analytics
    val analyticsDateFilter = MutableStateFlow(DateFilterType.THIS_WEEK)
    val customAnalyticsDate = MutableStateFlow<String?>(null) // "yyyy-MM-dd"

    fun setDateFilter(filter: DateFilterType) {
        analyticsDateFilter.value = filter
    }

    fun setCustomAnalyticsDate(dateKey: String) {
        customAnalyticsDate.value = dateKey
        analyticsDateFilter.value = DateFilterType.CUSTOM_DATE
    }

    // Real-Time Analytics State
    val analyticsSummary: StateFlow<AnalyticsSummary> = combine(
        allTransactions,
        allStores,
        allRoutes,
        analyticsDateFilter,
        customAnalyticsDate
    ) { txList, storeList, routeList, filter, customDate ->
        val now = Calendar.getInstance()
        val dayKeyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayDateFormatter = SimpleDateFormat("EEEE, d MMM yyyy", Locale("in", "ID"))
        val todayKey = dayKeyFormatter.format(Date())

        val filteredTx = txList.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.transaction.visitTimestamp }
            val txDateKey = dayKeyFormatter.format(txCal.time)

            when (filter) {
                DateFilterType.TODAY -> {
                    txDateKey == todayKey
                }
                DateFilterType.LAST_7_DAYS -> {
                    val sevenDaysAgo = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -7)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }
                    tx.transaction.visitTimestamp >= sevenDaysAgo.timeInMillis
                }
                DateFilterType.THIS_WEEK -> {
                    txCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                            txCal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)
                }
                DateFilterType.THIS_MONTH -> {
                    txCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                            txCal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                }
                DateFilterType.ALL_TIME -> true
                DateFilterType.CUSTOM_DATE -> {
                    if (customDate != null) {
                        txDateKey == customDate
                    } else {
                        true
                    }
                }
            }
        }

        val totalRev = filteredTx.sumOf { it.transaction.totalAmountDue }
        val totalProf = filteredTx.sumOf { it.transaction.totalProfit }
        val totalCost = totalRev - totalProf
        val totalItems = filteredTx.sumOf { it.transaction.totalItemsSold }
        val totalDebt = storeList.sumOf { it.outstandingDebt }
        val totalCapitalDeployed = filteredTx.flatMap { it.items }.sumOf {
            (it.soldQuantity + it.remainingStock) * it.costPrice
        }
        val marginPct = if (totalCapitalDeployed > 0) (totalProf / totalCapitalDeployed) * 100 else 0.0

        // Product sales aggregation
        val productSalesMap = mutableMapOf<String, TopSellingProductStat>()
        filteredTx.flatMap { it.items }.forEach { item ->
            val existing = productSalesMap[item.productName]
            if (existing != null) {
                productSalesMap[item.productName] = existing.copy(
                    totalSoldPcs = existing.totalSoldPcs + item.soldQuantity,
                    totalRevenue = existing.totalRevenue + item.subtotalDue
                )
            } else {
                val pSize = if (item.packSize > 0) item.packSize else 1
                productSalesMap[item.productName] = TopSellingProductStat(
                    productName = item.productName,
                    unitName = item.unitName,
                    packSize = pSize,
                    totalSoldPcs = item.soldQuantity,
                    totalRevenue = item.subtotalDue
                )
            }
        }
        val topProducts = productSalesMap.values.toList().sortedByDescending { it.totalSoldPcs }.take(5)

        // Store revenue aggregation
        val storeRevMap = mutableMapOf<String, Double>()
        filteredTx.forEach { tx ->
            storeRevMap[tx.transaction.storeName] = (storeRevMap[tx.transaction.storeName] ?: 0.0) + tx.transaction.totalAmountDue
        }
        val topStores = storeRevMap.toList().sortedByDescending { it.second }.take(5)

        // Route performance
        val routePerfs = routeList.map { route ->
            val routeTxs = filteredTx.filter { it.transaction.routeId == route.id }
            val storesInRoute = storeList.filter { it.routeId == route.id }
            val visitedCount = storesInRoute.count { it.isVisitedToday }
            RoutePerf(
                routeName = route.name,
                totalRevenue = routeTxs.sumOf { it.transaction.totalAmountDue },
                totalProfit = routeTxs.sumOf { it.transaction.totalProfit },
                storesVisited = visitedCount,
                totalStores = storesInRoute.size
            )
        }

        // Daily breakdown aggregation
        val dailyGroups = filteredTx.groupBy { dayKeyFormatter.format(Date(it.transaction.visitTimestamp)) }
        val dailyBreakdown = dailyGroups.map { (dateKey, txs) ->
            val firstTimestamp = txs.first().transaction.visitTimestamp
            val displayDate = displayDateFormatter.format(Date(firstTimestamp))
            val dRev = txs.sumOf { it.transaction.totalAmountDue }
            val dProf = txs.sumOf { it.transaction.totalProfit }
            val dCost = dRev - dProf
            val dItems = txs.sumOf { it.transaction.totalItemsSold }
            DailyAnalyticsItem(
                dateKey = dateKey,
                displayDate = displayDate,
                revenue = dRev,
                profit = dProf,
                cost = dCost,
                itemsSold = dItems,
                transactionCount = txs.size
            )
        }.sortedByDescending { it.dateKey }

        AnalyticsSummary(
            totalRevenue = totalRev,
            totalProfit = totalProf,
            totalCost = totalCost,
            totalItemsSold = totalItems,
            totalTransactions = filteredTx.size,
            totalOutstandingDebt = totalDebt,
            totalCapitalDeployed = totalCapitalDeployed,
            profitMarginPercent = marginPct,
            topSellingProducts = topProducts,
            topStoresByRevenue = topStores,
            routePerformances = routePerfs,
            dailyBreakdown = dailyBreakdown
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsSummary())

    // Store Active Consignments Helper
    fun getStoreConsignments(storeId: Long): Flow<List<ConsignmentProductDetail>> =
        repository.getConsignmentsForStore(storeId)

    // Actions: Products
    fun saveProduct(product: ProductEntity, onDone: () -> Unit = {}, onError: (String) -> Unit = { message ->
        android.widget.Toast.makeText(getApplication(), message, android.widget.Toast.LENGTH_LONG).show()
    }) {
        viewModelScope.launch {
            try {
                repository.saveProduct(product)
                onDone()
            } catch (error: Exception) {
                onError(error.message ?: "Gagal menyimpan produk")
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    // Actions: Routes
    fun saveRoute(route: RouteEntity, onDone: () -> Unit = {}, onError: (String) -> Unit = { message ->
        android.widget.Toast.makeText(getApplication(), message, android.widget.Toast.LENGTH_LONG).show()
    }) {
        viewModelScope.launch {
            try {
                val id = repository.saveRoute(route)
                if (selectedRouteId.value == null) {
                    selectedRouteId.value = id
                }
                onDone()
            } catch (error: Exception) {
                onError(error.message ?: "Gagal menyimpan rute")
            }
        }
    }

    fun deleteRoute(route: RouteEntity) {
        viewModelScope.launch {
            repository.deleteRoute(route)
            if (selectedRouteId.value == route.id) {
                selectedRouteId.value = null
            }
        }
    }

    fun selectRoute(routeId: Long?) {
        selectedRouteId.value = routeId
    }

    // Actions: Stores
    fun saveStore(store: StoreEntity, onDone: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.saveStore(store)
                onDone()
            } catch (error: Exception) {
                onError(error.message ?: "Gagal menyimpan warung")
            }
        }
    }

    fun deleteStore(store: StoreEntity) {
        viewModelScope.launch {
            repository.deleteStore(store)
        }
    }

    val debtWriteOffs = repository.debtWriteOffs

    fun writeOffStoreDebt(store: StoreEntity, reason: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.writeOffStoreDebt(store, reason)
            onDone()
        }
    }

    val businessPartners = repository.businessPartners
    val priceOverrides = repository.priceOverrides

    fun saveBusinessPartner(entity: BusinessPartnerEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch { repository.saveBusinessPartner(entity); onDone() }
    }

    fun deleteBusinessPartner(entity: BusinessPartnerEntity) {
        viewModelScope.launch { repository.deleteBusinessPartner(entity) }
    }

    fun savePriceOverride(entity: StorePriceOverrideEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch { repository.savePriceOverride(entity); onDone() }
    }

    fun deletePriceOverride(storeId: Long, productId: Long) {
        viewModelScope.launch { repository.deletePriceOverride(storeId, productId) }
    }

    fun addConsignmentToStore(storeId: Long, productId: Long, qty: Int) {
        viewModelScope.launch {
            repository.addOrUpdateConsignment(storeId, productId, qty)
        }
    }

    fun resetDailyVisits() {
        viewModelScope.launch {
            repository.resetDailyVisitStatus()
        }
    }

    // Actions: Van Loads
    fun saveVanLoad(productId: Long, initialQty: Int, costPerPack: Double = 0.0, notes: String = "") {
        viewModelScope.launch {
            repository.saveVanLoad(
                VanLoadEntity(
                    dateString = todayDateString,
                    productId = productId,
                    initialLoadedQty = initialQty,
                    costPerPack = costPerPack,
                    notes = notes
                )
            )
        }
    }

    fun updateVanLoadReturn(id: Long, returned: Int, damaged: Int) {
        vanLoadUpdateJobs[id]?.cancel()
        vanLoadUpdateJobs[id] = viewModelScope.launch {
            delay(150L)
            repository.updateVanLoadReturn(id, returned, damaged)
            vanLoadUpdateJobs.remove(id)
        }
    }

    fun normalizeTodayVanLoads() {
        viewModelScope.launch {
            repository.normalizeVanLoadsForDate(todayDateString)
        }
    }

    fun sortBs(productId: Long, goodPcs: Int, damagedPcs: Int, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.sortBs(productId, goodPcs, damagedPcs)
            } catch (error: IllegalArgumentException) {
                onError(error.message ?: "Gagal menyimpan sortir BS")
            }
        }
    }

    fun closeToday(
        loads: List<ClosingLoadInput>,
        cashCollected: Double,
        notes: String,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.closeDaily(todayDateString, loads, cashCollected, notes)
            } catch (error: IllegalArgumentException) {
                onError(error.message ?: "Gagal menyimpan closing")
            }
        }
    }

    fun deleteVanLoad(load: VanLoadEntity) {
        viewModelScope.launch {
            repository.deleteVanLoad(load)
        }
    }

    // Action: Complete Store Visit Consignment Reconciliation
    fun executeReconciliation(
        store: StoreEntity,
        route: RouteEntity,
        items: List<ReconciliationItemInput>,
        amountPaid: Double,
        previousDebtPaid: Double,
        paymentStatus: String,
        notes: String,
        visitLocation: Location,
        onSuccess: (VisitTransactionEntity) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val result = repository.executeVisitReconciliation(
                    store = store,
                    route = route,
                    reconciledItems = items,
                    amountPaid = amountPaid,
                    previousDebtPaid = previousDebtPaid,
                    paymentStatus = paymentStatus,
                    notes = notes,
                    visitLocation = visitLocation,
                    gpsAccuracyThreshold = gpsAccuracyThreshold.value
                )
                onSuccess(result)
            } catch (error: IllegalArgumentException) {
                onError(error.message ?: "Validasi kunjungan gagal")
            }
        }
    }

    // Actions: Backup & Restore for Phone Migration
    fun exportBackupFile(context: Context, onResult: (java.io.File?) -> Unit) {
        viewModelScope.launch {
            try {
                val file = com.example.util.BackupRestoreUtil.saveBackupToCacheFile(context, database)
                onResult(file)
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }

    fun exportBackupToUri(context: Context, uri: android.net.Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                com.example.util.BackupRestoreUtil.writeBackupToUri(context, uri, database)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun importBackupFromUri(context: Context, uri: android.net.Uri, onResult: (com.example.util.RestoreResult) -> Unit) {
        viewModelScope.launch {
            try {
                val json = com.example.util.BackupRestoreUtil.readJsonFromUri(context, uri)
                val result = com.example.util.BackupRestoreUtil.restoreDatabaseFromJson(json, database)
                onResult(result)
            } catch (e: Exception) {
                onResult(
                    com.example.util.RestoreResult(
                        success = false,
                        message = e.localizedMessage ?: "Gagal membaca file backup"
                    )
                )
            }
        }
    }

    // Actions: Generate PDF Report
    fun generatePdfReport(context: Context, periodTitle: String, onResult: (java.io.File?) -> Unit) {
        viewModelScope.launch {
            try {
                val transactions = allTransactions.value
                val pdfFile = com.example.util.PdfReportGenerator.generateSalesPdfReport(
                    context = context,
                    transactions = transactions,
                    periodTitle = periodTitle
                )
                onResult(pdfFile)
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }

    fun writePdfReportToUri(context: Context, uri: android.net.Uri, periodTitle: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val transactions = allTransactions.value
                com.example.util.PdfReportGenerator.writePdfToUri(
                    context = context,
                    uri = uri,
                    transactions = transactions,
                    periodTitle = periodTitle
                )
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    companion object {
        fun formatRupiah(amount: Double): String {
            val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            format.maximumFractionDigits = 0
            return format.format(amount).replace("Rp", "Rp ")
        }

        fun formatNumber(number: Number): String {
            val format = NumberFormat.getNumberInstance(Locale("in", "ID"))
            return format.format(number)
        }
    }
}
