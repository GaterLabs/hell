package com.example.ui.util

import com.example.ui.viewmodel.SalesViewModel
import java.util.Locale

fun localizedLabel(language: AppLanguage, indonesian: String, english: String): String =
    if (language.code == "id") indonesian else english

interface AppStrings {
    // Navigation
    val navDashboard: String
    val navRoutes: String
    val navStores: String
    val navInventory: String
    val navAnalytics: String
    val navHistory: String
    val navSettings: String
    val navMasterData: String
    val drawerSectionOperations: String
    val drawerSectionReports: String
    val drawerSectionSystem: String
    val appTagline: String

    // Unit & Consignment Breakdown Strings
    val piecesUnit: String
    val packIsiLabel: String
    val remainingPcsLabel: String
    val soldPcsLabel: String
    val pricePerPcLabel: String
    val restockPackLabel: String
    val restockPcsLabel: String
    val totalEndingStockLabel: String
    val stockPacksAndPcs: (Int, Int, String) -> String
    val stockPcsOnly: (Int) -> String
    val soldCalcDetail: (Int, Double, Double) -> String

    // History Tabs & Stock Reports
    val tabReceipts: String
    val tabRemainingByRoute: String
    val remainingStockReportTitle: String
    val remainingStockReportSubtitle: String
    val filterSelectRoute: String
    val filterSelectStore: String
    val allStoresInRoute: String
    val totalRemainingPcsStat: String
    val totalConsignmentValueStat: String
    val storesHoldingStockStat: String
    val noRemainingStockFound: String
    val noRemainingStockFoundDesc: String
    val lastRecordedConsignment: String

    // Master Stores Directory
    val storesTitle: String
    val storesSubtitle: String
    val storesSearchPlaceholder: String
    val filterAllRoutes: String
    val filterAllStatus: String
    val filterHasDebt: String
    val filterVisitedToday: String
    val filterNotVisited: String
    val totalStoresStat: String
    val totalDebtStat: String
    val visitedStoresStat: String
    val btnReconcileVisit: String
    val btnEditStore: String
    val btnDeleteStore: String
    val deleteStoreDialogTitle: String
    val deleteStoreConfirmMsg: String
    val changeRouteLabel: String
    val consignmentItemsBadge: String
    val emptyStoresFilter: String
    val emptyStoresFilterDesc: String
    val selectRouteRequired: String
    val storeLifecycleLabel: String
    val storeActive: String
    val storeTemporarilyClosed: String
    val storeBlacklisted: String
    val creditLimitLabel: String
    val creditLimitPlaceholder: String
    val remainingCreditLabel: String
    val debtAgeLabel: String
    val addStorePhoto: String
    val replaceStorePhoto: String
    val storedCoordinatesLabel: String
    val storePhotoDescription: String
    val takeStorePhoto: String
    val chooseStorePhoto: String

    // Common / Global
    val btnCancel: String
    val btnSave: String
    val btnDelete: String
    val btnClose: String
    val packUnit: String
    val soldLabel: String
    val previousStock: String
    val remainingStock: String
    val newDroppedQty: String
    val costHppLabel: String
    val costPriceLabel: String
    val sellPriceLabel: String
    val notesLabel: String
    val notesPlaceholder: String
    val profitPrefix: String
    val subtotalLabel: String
    val totalAmountDueLabel: String
    val amountPaidLabel: String
    val remainingDebtLabel: String
    val paymentStatusLabel: String
    val paymentStatusPaid: String
    val paymentStatusUnpaid: String
    val paymentStatusPartial: String
    val ownerPrefix: String
    val phonePrefix: String
    val addressPrefix: String
    val txPrefix: String
    val todayBadge: String
    val yesterdayBadge: String
    val btnSendWhatsapp: String
    val receiptNumberLabel: String
    val storeLabel: String
    val routeLabel: String
    val timeLabel: String

    // Routes Screen
    val routesTitle: String
    val routesSubtitle: String
    val allRoutes: String
    val addRoute: String
    val searchStorePlaceholder: String
    val routeStatsVisited: String
    val routeStatsConsignmentStock: String
    val routeStatsDebt: String
    val noStoresFound: String
    val noStoresFoundDesc: String
    val addFirstStore: String
    val storeStatusVisited: String
    val storeStatusPending: String
    val btnReconciliation: String
    val btnCallStore: String
    val btnOpenMaps: String
    val currentConsignment: String
    val uncollectedDebt: String
    val noActiveConsignment: String
    val routeStoresCount: (Int, Int) -> String

    // Add Route Dialog
    val addRouteDialogTitle: String
    val routeNameLabel: String
    val routeNamePlaceholder: String
    val routeDescriptionLabel: String
    val routeDescriptionPlaceholder: String

    // Add Store Dialog
    val addStoreDialogTitle: String
    val storeNameLabel: String
    val storeNamePlaceholder: String
    val ownerNameLabel: String
    val ownerNamePlaceholder: String
    val phoneLabel: String
    val phonePlaceholder: String
    val addressLabel: String
    val addressPlaceholder: String
    val selectRouteLabel: String
    val initialConsignmentSection: String
    val initialConsignmentHint: String
    val autofillAddressTip: String
    val autofillRouteAreaChip: String
    val autofillStoreRouteChip: String
    val optionalTag: String
    val btnGetGpsLocation: String
    val gpsDetecting: String
    val gpsSuccess: String
    val gpsFailed: String
    val gpsPermissionNeeded: String

    // Smart Offline GPS Route Sorting
    val sortNearestGps: String
    val sortStandardRoute: String
    val gpsLiveActive: String
    val gpsLiveSearching: String
    val gpsEnablePrompt: String
    val smartRouteNearestTitle: String
    val distanceUnitMeters: (Int) -> String
    val distanceUnitKm: (Double) -> String
    val nearestStoreTag: String
    val nextClosestBadge: String
    val unvisitedFirstToggle: String

    // Visit Reconciliation Sheet
    val sheetTitle: String
    val consignReconcileTitle: String
    val btnAddAnotherProduct: String
    val initialDropBadge: String
    val initialDropDesc: String
    val btnSaveInitialDrop: String

    // Consignment Age & Cycle Tracking
    val consignmentAgeLabel: String
    val ageNewStore: String
    val ageToday: String
    val ageYesterday: String
    val ageDaysAgo: (Int) -> String
    val ageDueToday: String
    val ageOverdue: (Int) -> String
    val filterNeedsVisitDue: String
    val filterNewStore: String
    val visitNotesLabel: String
    val visitNotesPlaceholder: String
    val customPaymentLabel: String
    val btnSaveAndReceipt: String
    val selectProductTitle: String
    val allProductsAdded: String

    // Inventory & Cargo Screen
    val inventoryTitle: String
    val tabVehicleCargo: String
    val tabFieldStock: String
    val tabCatalog: String
    val btnAddProduct: String
    val btnAddCargo: String
    val cargoTotalLoaded: String
    val cargoDistributed: String
    val cargoRemaining: String
    val cargoDamaged: String
    val tabHistory: String
    val setorLabel: String
    val setorDone: String
    val setorPending: String
    val setorAll: String
    val unsetorLabel: String
    val totalLoadCost: String
    val totalSetor: String
    val totalUnsetored: String
    val costPerPack: String
    val emptyHistoryTitle: String
    val emptyHistoryDesc: String
    val loadFromFactory: String
    val returnedGoods: String
    val emptyCargoTitle: String
    val emptyCargoDesc: String
    val btnLoadCargoNow: String
    val loadedLabel: String
    val distributedLabel: String
    val remainingLabel: String
    val fieldStockTitle: String
    val fieldStockValuation: String
    val storesHoldingStock: String
    val emptyFieldStock: String
    val emptyFieldStockDesc: String
    val catalogTitle: String
    val marginPerPack: String
    val addProductDialogTitle: String
    val editProductDialogTitle: String
    val productNameLabel: String
    val productNamePlaceholder: String
    val unitNameLabel: String
    val unitNamePlaceholder: String
    val packSizeLabel: String
    val costPriceFieldLabel: String
    val sellPriceFieldLabel: String
    val addCargoDialogTitle: String
    val selectProductLabel: String
    val quantityToLoadLabel: String
    val deleteProductConfirmTitle: String
    val deleteProductConfirmDesc: (String) -> String

    // Analytics Screen
    val analyticsTitle: String
    val analyticsSubtitle: String
    val filterToday: String
    val filter7Days: String
    val filterThisWeek: String
    val filterThisMonth: String
    val filterAllTime: String
    val filterPickDate: String
    val periodIndicator: String
    val tabRoutes: String
    val totalRevenueStat: String
    val totalProfitStat: String
    val totalItemsSoldStat: String
    val itemsDeliveredDesc: String
    val uncollectedDebtStat: String
    val debtRemainingDesc: String
    val dailyBreakdownTitle: String
    val financialSummaryTitle: String
    val topSellingTitle: String
    val routePerformanceTitle: String
    val noTransactionsFound: String
    val noTransactionsFoundDesc: String

    // History Screen
    val historyTitle: String
    val searchHistoryPlaceholder: String

    // Receipt Dialog
    val receiptDialogTitle: String

    // Settings Screen
    val settingsTitle: String
    val languageSettingTitle: String
    val languageSettingSubtitle: String
    val themeSettingTitle: String
    val themeSettingSubtitle: String
    val themeSystem: String
    val themeSystemDesc: String
    val themeLight: String
    val themeLightDesc: String
    val themeDark: String
    val themeDarkDesc: String
    val offlineBadgeTitle: String
    val offlineBadgeDesc: String
    val dailyOpsTitle: String
    val btnResetDailyVisits: String
    val resetDailyVisitsDesc: String
    val resetVisitsDialogTitle: String
    val resetVisitsDialogDesc: String
    val btnConfirmReset: String
    val formulaTitle: String
    val formula1: String
    val formula2: String
    val formula3: String
    val formula4: String
    val appVersionTitle: String
    val appVersionDesc: String

    // Backup & Restore / Transfer Data
    val backupSectionTitle: String
    val backupSectionSubtitle: String
    val btnExportBackup: String
    val exportBackupDesc: String
    val btnImportBackup: String
    val importBackupDesc: String
    val importBackupDialogTitle: String
    val importBackupDialogDesc: String
    val btnConfirmImport: String
    val backupExportSuccess: String
    val backupImportSuccess: (Int, Int, Int, Int) -> String
    val backupImportFailed: String

    // PDF Export Reports
    val btnExportPdfReport: String
    val exportPdfReportDesc: String
    val generatingPdf: String
    val pdfExportSuccess: String
    val pdfShareSubject: String
}

object AppStringsEn : AppStrings {
    // Navigation
    override val navDashboard = "Dashboard"
    override val navRoutes = "Routes"
    override val navStores = "All Stores"
    override val navInventory = "Cargo & Stock"
    override val navAnalytics = "Analytics"
    override val navHistory = "Receipts"
    override val navSettings = "Settings"
    override val navMasterData = "Master Data"
    override val drawerSectionOperations = "Daily Operations"
    override val drawerSectionReports = "Reports & Analytics"
    override val drawerSectionSystem = "System & Preferences"
    override val appTagline = "Motorcycle Sales & Consignment"

    // Unit & Consignment Breakdown Strings
    override val piecesUnit = "pcs"
    override val packIsiLabel = "Pack Size"
    override val remainingPcsLabel = "Remaining Stock (Pcs)"
    override val soldPcsLabel = "Sold / Laku (Pcs)"
    override val pricePerPcLabel = "Price / Pc"
    override val restockPackLabel = "Restock (Pack)"
    override val restockPcsLabel = "Restock (Pcs)"
    override val totalEndingStockLabel = "Total Stock Left at Store"
    override val stockPacksAndPcs: (Int, Int, String) -> String = { packs, pcs, unit ->
        if (packs > 0 && pcs > 0) "$packs $unit + $pcs pcs"
        else if (packs > 0) "$packs $unit"
        else "$pcs pcs"
    }
    override val stockPcsOnly: (Int) -> String = { pcs -> "$pcs pcs" }
    override val soldCalcDetail: (Int, Double, Double) -> String = { qty, price, total ->
        "$qty pcs × ${SalesViewModel.formatRupiah(price)} = ${SalesViewModel.formatRupiah(total)}"
    }

    // History Tabs & Stock Reports
    override val tabReceipts = "Receipts & Transactions"
    override val tabRemainingByRoute = "Remaining Stock by Route & Store"
    override val remainingStockReportTitle = "Consignment Stock Report"
    override val remainingStockReportSubtitle = "Track physical remaining stock consigned across all stores and routes"
    override val filterSelectRoute = "Select Route"
    override val filterSelectStore = "Select Store"
    override val allStoresInRoute = "All Stores in Route"
    override val totalRemainingPcsStat = "Total Remaining Stock"
    override val totalConsignmentValueStat = "Consigned Stock Value"
    override val storesHoldingStockStat = "Stores Holding Stock"
    override val noRemainingStockFound = "No Consignment Stock Found"
    override val noRemainingStockFoundDesc = "Stores on this route currently have no active consignment stock recorded."
    override val lastRecordedConsignment = "Last Consignment Record"

    // Master Stores Directory
    override val storesTitle = "All Stores Directory"
    override val storesSubtitle = "Comprehensive list of all stores across all routes"
    override val storesSearchPlaceholder = "Search store name, owner, phone, or address..."
    override val filterAllRoutes = "All Routes"
    override val filterAllStatus = "All Status"
    override val filterHasDebt = "Has Unpaid Debt"
    override val filterVisitedToday = "Visited Today"
    override val filterNotVisited = "Not Visited Yet"
    override val totalStoresStat = "Total Stores"
    override val totalDebtStat = "Total Store Debt"
    override val visitedStoresStat = "Visited Today"
    override val btnReconcileVisit = "Check Stock / Visit"
    override val btnEditStore = "Edit Store"
    override val btnDeleteStore = "Delete Store"
    override val deleteStoreDialogTitle = "Delete Store"
    override val deleteStoreConfirmMsg = "Are you sure you want to delete {name}? All active consignment data will be removed."
    override val changeRouteLabel = "Assigned Route"
    override val consignmentItemsBadge = "Consigned Items"
    override val emptyStoresFilter = "No stores found"
    override val emptyStoresFilterDesc = "Try adjusting your search keywords or route filter."
    override val selectRouteRequired = "Please select a route for this store"
    override val storeLifecycleLabel = "Store lifecycle"
    override val storeActive = "Active"
    override val storeTemporarilyClosed = "Temporarily closed"
    override val storeBlacklisted = "Blacklisted"
    override val creditLimitLabel = "Credit limit (Rp)"
    override val creditLimitPlaceholder = "e.g. 500000"
    override val remainingCreditLabel = "Remaining credit"
    override val debtAgeLabel = "Debt age"
    override val addStorePhoto = "Add store photo"
    override val replaceStorePhoto = "Replace store photo"
    override val storedCoordinatesLabel = "Saved coordinates"
    override val storePhotoDescription = "Store photo"
    override val takeStorePhoto = "Take photo"
    override val chooseStorePhoto = "Choose from gallery"

    // Common / Global
    override val btnCancel = "Cancel"
    override val btnSave = "Save"
    override val btnDelete = "Delete"
    override val btnClose = "Close"
    override val packUnit = "packs"
    override val soldLabel = "Sold"
    override val previousStock = "Prev Stock"
    override val remainingStock = "Remaining"
    override val newDroppedQty = "New Dropped"
    override val costHppLabel = "Cost (HPP)"
    override val costPriceLabel = "Cost Price"
    override val sellPriceLabel = "Price"
    override val notesLabel = "Notes"
    override val notesPlaceholder = "Optional notes"
    override val profitPrefix = "Profit"
    override val subtotalLabel = "Subtotal"
    override val totalAmountDueLabel = "Total Due"
    override val amountPaidLabel = "Amount Paid"
    override val remainingDebtLabel = "Remaining Debt"
    override val paymentStatusLabel = "Payment Status"
    override val paymentStatusPaid = "Paid in Full"
    override val paymentStatusUnpaid = "Unpaid / Credit"
    override val paymentStatusPartial = "Partial Paid"
    override val ownerPrefix = "Owner: "
    override val phonePrefix = "Phone: "
    override val addressPrefix = "Address: "
    override val txPrefix = "Transactions"
    override val todayBadge = "TODAY"
    override val yesterdayBadge = "YESTERDAY"
    override val btnSendWhatsapp = "Send via WhatsApp"
    override val receiptNumberLabel = "Receipt No."
    override val storeLabel = "Store"
    override val routeLabel = "Route"
    override val timeLabel = "Time"

    // Routes Screen
    override val routesTitle = "Routes & Stores"
    override val routesSubtitle = "Sales Consignment Tracking System"
    override val allRoutes = "All Routes"
    override val addRoute = "+ Add Route"
    override val searchStorePlaceholder = "Search store, owner, address..."
    override val routeStatsVisited = "Visited"
    override val routeStatsConsignmentStock = "Consignment Stock"
    override val routeStatsDebt = "Unpaid Debt"
    override val noStoresFound = "No stores found"
    override val noStoresFoundDesc = "Add stores to this route to start recording visits."
    override val addFirstStore = "Add First Store"
    override val storeStatusVisited = "Visited"
    override val storeStatusPending = "Pending Visit"
    override val btnReconciliation = "Reconcile & Visit"
    override val btnCallStore = "Call Store"
    override val btnOpenMaps = "Open Maps"
    override val currentConsignment = "Active Consignment"
    override val uncollectedDebt = "Outstanding Debt"
    override val noActiveConsignment = "No consignment stock yet"
    override val routeStoresCount: (Int, Int) -> String = { visited, total -> "$visited of $total stores visited" }

    // Add Route Dialog
    override val addRouteDialogTitle = "Add New Route"
    override val routeNameLabel = "Route Name"
    override val routeNamePlaceholder = "e.g. Route A - North Area"
    override val routeDescriptionLabel = "Description / Schedule"
    override val routeDescriptionPlaceholder = "e.g. Monday & Thursday regular delivery"

    // Add Store Dialog
    override val addStoreDialogTitle = "Add New Store / Warung"
    override val storeNameLabel = "Store Name"
    override val storeNamePlaceholder = "e.g. Warung Bu Sri"
    override val ownerNameLabel = "Owner Name"
    override val ownerNamePlaceholder = "e.g. Bu Sri (Optional)"
    override val phoneLabel = "WhatsApp / Phone Number"
    override val phonePlaceholder = "e.g. 08123456789 (Optional)"
    override val addressLabel = "Address / Location"
    override val addressPlaceholder = "e.g. Jl. Melati No. 12"
    override val selectRouteLabel = "Select Route Area *"
    override val initialConsignmentSection = "Initial Dropped Consignment Stock"
    override val initialConsignmentHint = "Set initial consignment stock for each product left at this store."
    override val autofillAddressTip = "💡 Tip: Just enter Store Name! Address is autofilled from the route, and Owner & Phone can be left blank."
    override val autofillRouteAreaChip = "Route Area"
    override val autofillStoreRouteChip = "Store + Route"
    override val optionalTag = "Optional"
    override val btnGetGpsLocation = "📍 Detect GPS Location (Autofill)"
    override val gpsDetecting = "Detecting current GPS coordinates..."
    override val gpsSuccess = "Address filled from GPS!"
    override val gpsFailed = "Could not get GPS location. Please ensure device GPS is turned on."
    override val gpsPermissionNeeded = "Location permission is required for GPS autofill."

    // Smart Offline GPS Route Sorting
    override val sortNearestGps = "Nearest (Offline GPS)"
    override val sortStandardRoute = "Route Order"
    override val gpsLiveActive = "Offline GPS Active"
    override val gpsLiveSearching = "Searching GPS..."
    override val gpsEnablePrompt = "Enable GPS for Smart Order"
    override val smartRouteNearestTitle = "Smart Route (Live Distance)"
    override val distanceUnitMeters: (Int) -> String = { m -> "$m m" }
    override val distanceUnitKm: (Double) -> String = { km -> String.format(Locale.US, "%.1f km", km) }
    override val nearestStoreTag = "NEXT CLOSEST"
    override val nextClosestBadge = "Next Closest Destination"
    override val unvisitedFirstToggle = "Pending First"

    // Visit Reconciliation Sheet
    override val sheetTitle = "Store Visit Reconciliation"
    override val consignReconcileTitle = "Consignment Stock & Remaining"
    override val btnAddAnotherProduct = "+ Add Product"
    override val initialDropBadge = "Initial Drop (New Store)"
    override val initialDropDesc = "First consignment drop for this store. No previous stock and no payment due (Rp 0)."
    override val btnSaveInitialDrop = "Save Initial Drop"

    // Consignment Age & Cycle Tracking
    override val consignmentAgeLabel = "Consignment Age"
    override val ageNewStore = "New Store (No Drop Yet)"
    override val ageToday = "Dropped Today"
    override val ageYesterday = "1 Day Ago"
    override val ageDaysAgo: (Int) -> String = { days -> "$days Days Ago" }
    override val ageDueToday = "Restock Due (7 Days)"
    override val ageOverdue: (Int) -> String = { days -> "Overdue ($days Days)" }
    override val filterNeedsVisitDue = "Restock Due (≥7 Days)"
    override val filterNewStore = "New Stores"
    override val visitNotesLabel = "Visit Notes (Optional)"
    override val visitNotesPlaceholder = "e.g. Requested extra stock next visit"
    override val customPaymentLabel = "Cash Received (Rp)"
    override val btnSaveAndReceipt = "Save & Print Receipt"
    override val selectProductTitle = "Select Consignment Product"
    override val allProductsAdded = "All catalog products are already added to this store."

    // Inventory & Cargo Screen
    override val inventoryTitle = "Cargo & Inventory"
    override val tabVehicleCargo = "Vehicle Cargo"
    override val tabFieldStock = "Field Consignment"
    override val tabCatalog = "Product Catalog"
    override val btnAddProduct = "+ New Product"
    override val btnAddCargo = "+ Load Cargo"
    override val cargoTotalLoaded = "Total Loaded"
    override val cargoDistributed = "Distributed"
    override val cargoRemaining = "In Vehicle"
    override val cargoDamaged = "Damaged"
    override val tabHistory = "History"
    override val setorLabel = "Setor"
    override val setorDone = "Settled"
    override val setorPending = "Unsettled"
    override val setorAll = "Settle All"
    override val unsetorLabel = "Undo Settle"
    override val totalLoadCost = "Total Load Cost"
    override val totalSetor = "Total Settled"
    override val totalUnsetored = "Total Unsettled"
    override val costPerPack = "Cost / Pack"
    override val emptyHistoryTitle = "No Load History"
    override val emptyHistoryDesc = "Load cargo first to see history here"
    override val loadFromFactory = "From Factory"
    override val returnedGoods = "Returned Goods"
    override val emptyCargoTitle = "Vehicle Cargo Empty"
    override val emptyCargoDesc = "Load inventory into your vehicle before starting daily delivery routes."
    override val btnLoadCargoNow = "Load Cargo Now"
    override val loadedLabel = "Loaded"
    override val distributedLabel = "Distributed"
    override val remainingLabel = "In Vehicle"
    override val fieldStockTitle = "Field Consignment Stock"
    override val fieldStockValuation = "Field Stock Value"
    override val storesHoldingStock = "Stores holding stock"
    override val emptyFieldStock = "No consignment stock in stores"
    override val emptyFieldStockDesc = "When you drop consignment goods at stores, they will be tracked here."
    override val catalogTitle = "Catalog & Pricing"
    override val marginPerPack = "Margin"
    override val addProductDialogTitle = "Add New Product"
    override val editProductDialogTitle = "Edit Product"
    override val productNameLabel = "Product Name *"
    override val productNamePlaceholder = "e.g. Keripik Singkong Balado 200g"
    override val unitNameLabel = "Unit (Pack/Bungkus/Pcs) *"
    override val unitNamePlaceholder = "e.g. bks, pcs, pack"
    override val packSizeLabel = "Pack Size (Pcs per pack) *"
    override val costPriceFieldLabel = "Cost Price / HPP (Rp) *"
    override val sellPriceFieldLabel = "Selling Price to Store (Rp) *"
    override val addCargoDialogTitle = "Load Cargo into Vehicle"
    override val selectProductLabel = "Select Product *"
    override val quantityToLoadLabel = "Quantity to Load (Packs) *"
    override val deleteProductConfirmTitle = "Delete Product"
    override val deleteProductConfirmDesc: (String) -> String = { name -> "Are you sure you want to delete '$name'?" }

    // Analytics Screen
    override val analyticsTitle = "Consignment Analytics"
    override val analyticsSubtitle = "Sales, Profit & Consignment Performance"
    override val filterToday = "Today"
    override val filter7Days = "Last 7 Days"
    override val filterThisWeek = "This Week"
    override val filterThisMonth = "This Month"
    override val filterAllTime = "All Time"
    override val filterPickDate = "Pick Date 📅"
    override val periodIndicator = "PERIOD:"
    override val tabRoutes = "Visits"
    override val totalRevenueStat = "Gross Revenue"
    override val totalProfitStat = "Net Profit"
    override val totalItemsSoldStat = "Total Sold"
    override val itemsDeliveredDesc = "Units delivered & reconciled"
    override val uncollectedDebtStat = "Unpaid Debt"
    override val debtRemainingDesc = "Outstanding credit from stores"
    override val dailyBreakdownTitle = "Daily Analytics Breakdown"
    override val financialSummaryTitle = "Revenue vs Cost (HPP) Breakdown"
    override val topSellingTitle = "Top Selling Consignment Products"
    override val routePerformanceTitle = "Route Sales Performance"
    override val noTransactionsFound = "No transactions recorded for this period"
    override val noTransactionsFoundDesc = "Transactions and receipts will appear here after completing store visits."

    // History Screen
    override val historyTitle = "Receipts & History"
    override val searchHistoryPlaceholder = "Search store, product, route, receipt no..."

    // Receipt Dialog
    override val receiptDialogTitle = "Consignment Sales Receipt"

    // Settings Screen
    override val settingsTitle = "Settings"
    override val languageSettingTitle = "App Language"
    override val languageSettingSubtitle = "Select display language for the application"
    override val themeSettingTitle = "Display Theme"
    override val themeSettingSubtitle = "Select light, dark, or device system theme"
    override val themeSystem = "System Default"
    override val themeSystemDesc = "Automatically follows device settings"
    override val themeLight = "Light Mode"
    override val themeLightDesc = "Clean high-contrast bright theme"
    override val themeDark = "Dark Mode"
    override val themeDarkDesc = "Easy on eyes in dark environments"
    override val offlineBadgeTitle = "100% Offline Capable"
    override val offlineBadgeDesc = "All consignment data is stored locally on device without requiring internet."
    override val dailyOpsTitle = "Daily Operations"
    override val btnResetDailyVisits = "Reset Daily Store Visited Status"
    override val resetDailyVisitsDesc = "Marks all stores as 'Pending Visit' so you can start a fresh delivery day."
    override val resetVisitsDialogTitle = "Reset Daily Visits?"
    override val resetVisitsDialogDesc = "This will mark all stores as 'Pending Visit' for the new day. Past transaction receipts and sales history will remain safe."
    override val btnConfirmReset = "Reset Status"
    override val formulaTitle = "Consignment Calculation Rules"
    override val formula1 = "Sold Quantity = Previous Stock - Remaining Stock"
    override val formula2 = "Store Bill = Sold Quantity × Selling Price"
    override val formula3 = "Salesman Profit = Sold Quantity × (Selling Price - Cost HPP)"
    override val formula4 = "Next Visit Stock = Remaining Stock + New Dropped Quantity"
    override val appVersionTitle = "Stock Sales System"
    override val appVersionDesc = "Version 1.0.0 • Production Ready"

    // Backup & Restore / Transfer Data
    override val backupSectionTitle = "Backup & Restore (Phone Transfer)"
    override val backupSectionSubtitle = "Export or import full database to transfer data to a new phone or save backups"
    override val btnExportBackup = "Export Backup (JSON)"
    override val exportBackupDesc = "Save all routes, stores, products, stock, and history into a shareable backup file."
    override val btnImportBackup = "Restore / Import Data"
    override val importBackupDesc = "Restore database from a previously exported Stock Sales JSON backup file."
    override val importBackupDialogTitle = "Restore Database?"
    override val importBackupDialogDesc = "Importing backup will replace current database with data from the backup file. Make sure you select the correct backup file."
    override val btnConfirmImport = "Confirm & Restore"
    override val backupExportSuccess = "Backup file created successfully! Ready to share or save."
    override val backupImportSuccess: (Int, Int, Int, Int) -> String = { routes, stores, prods, txs -> "Restore successful! Loaded $routes routes, $stores stores, $prods products, and $txs transactions." }
    override val backupImportFailed = "Failed to restore data. Please ensure the file is a valid Stock Sales backup."

    // PDF Export Reports
    override val btnExportPdfReport = "Export PDF Report"
    override val exportPdfReportDesc = "Generate a formal A4 sales and financial report document in PDF format."
    override val generatingPdf = "Generating PDF report..."
    override val pdfExportSuccess = "PDF report generated successfully!"
    override val pdfShareSubject = "Stock Sales Consignment & Financial Report"
}

object AppStringsId : AppStrings {
    // Navigation
    override val navDashboard = "Dashboard"
    override val navRoutes = "Rute"
    override val navStores = "Daftar Warung"
    override val navInventory = "Muatan & Stok"
    override val navAnalytics = "Analitik"
    override val navHistory = "Riwayat Nota"
    override val navSettings = "Pengaturan"
    override val navMasterData = "Master Data"
    override val drawerSectionOperations = "Operasional Harian"
    override val drawerSectionReports = "Laporan & Analitik"
    override val drawerSectionSystem = "Sistem & Preferensi"
    override val appTagline = "Aplikasi Sales Motoris & Konsinyasi"

    // Unit & Consignment Breakdown Strings
    override val piecesUnit = "pcs"
    override val packIsiLabel = "Isi per Pack"
    override val remainingPcsLabel = "Sisa di Warung (Pcs)"
    override val soldPcsLabel = "Laku / Terjual (Pcs)"
    override val pricePerPcLabel = "Harga per Pcs"
    override val restockPackLabel = "Tambah Titip (Pack/Unit)"
    override val restockPcsLabel = "Tambah Titip (Pcs)"
    override val totalEndingStockLabel = "Total Stok Ditinggal di Warung"
    override val stockPacksAndPcs: (Int, Int, String) -> String = { packs, pcs, unit ->
        if (packs > 0 && pcs > 0) "$packs $unit + $pcs pcs"
        else if (packs > 0) "$packs $unit"
        else "$pcs pcs"
    }
    override val stockPcsOnly: (Int) -> String = { pcs -> "$pcs pcs" }
    override val soldCalcDetail: (Int, Double, Double) -> String = { qty, price, total ->
        "$qty pcs × ${SalesViewModel.formatRupiah(price)} = ${SalesViewModel.formatRupiah(total)}"
    }

    // History Tabs & Stock Reports
    override val tabReceipts = "Riwayat Nota Transaksi"
    override val tabRemainingByRoute = "Sisa Stok Rute & Warung"
    override val remainingStockReportTitle = "Laporan Sisa Stok Konsinyasi"
    override val remainingStockReportSubtitle = "Pantau sisa barang fisik yang masih ada di tiap warung dan rute"
    override val filterSelectRoute = "Pilih Rute"
    override val filterSelectStore = "Pilih Warung"
    override val allStoresInRoute = "Semua Warung di Rute"
    override val totalRemainingPcsStat = "Total Sisa Stok"
    override val totalConsignmentValueStat = "Nilai Sisa Titipan"
    override val storesHoldingStockStat = "Warung Menampung Stok"
    override val noRemainingStockFound = "Tidak Ada Stok Konsinyasi"
    override val noRemainingStockFoundDesc = "Warung-warung pada rute ini saat ini belum memiliki catatan sisa barang titipan."
    override val lastRecordedConsignment = "Titipan Terakhir Tercatat"

    // Master Stores Directory
    override val storesTitle = "Daftar Semua Warung"
    override val storesSubtitle = "Master database seluruh warung dari semua rute"
    override val storesSearchPlaceholder = "Cari nama warung, pemilik, no HP, alamat..."
    override val filterAllRoutes = "Semua Rute"
    override val filterAllStatus = "Semua Status"
    override val filterHasDebt = "Ada Piutang/Bon"
    override val filterVisitedToday = "Sudah Dikunjungi"
    override val filterNotVisited = "Belum Dikunjungi"
    override val totalStoresStat = "Total Warung"
    override val totalDebtStat = "Total Bon Warung"
    override val visitedStoresStat = "Selesai Dikunjungi"
    override val btnReconcileVisit = "Cek Stok / Kunjungi"
    override val btnEditStore = "Edit Warung"
    override val btnDeleteStore = "Hapus Warung"
    override val deleteStoreDialogTitle = "Hapus Warung"
    override val deleteStoreConfirmMsg = "Yakin ingin menghapus {name}? Data titipan stok aktif pada warung ini akan terhapus."
    override val changeRouteLabel = "Pilihan Rute"
    override val consignmentItemsBadge = "Barang Dititip"
    override val emptyStoresFilter = "Tidak ada warung ditemukan"
    override val emptyStoresFilterDesc = "Coba ubah kata kunci pencarian atau filter rute."
    override val selectRouteRequired = "Silakan pilih rute untuk warung ini"
    override val storeLifecycleLabel = "Status warung"
    override val storeActive = "Aktif"
    override val storeTemporarilyClosed = "Tutup sementara"
    override val storeBlacklisted = "Blacklist"
    override val creditLimitLabel = "Limit bon (Rp)"
    override val creditLimitPlaceholder = "Contoh: 500000"
    override val remainingCreditLabel = "Sisa limit"
    override val debtAgeLabel = "Umur bon"
    override val addStorePhoto = "Tambah foto warung"
    override val replaceStorePhoto = "Ganti foto warung"
    override val storedCoordinatesLabel = "Koordinat tersimpan"
    override val storePhotoDescription = "Foto warung"
    override val takeStorePhoto = "Ambil foto"
    override val chooseStorePhoto = "Pilih dari galeri"

    // Common / Global
    override val btnCancel = "Batal"
    override val btnSave = "Simpan"
    override val btnDelete = "Hapus"
    override val btnClose = "Tutup"
    override val packUnit = "bks"
    override val soldLabel = "Laku"
    override val previousStock = "Titip Lalu"
    override val remainingStock = "Sisa di Warung"
    override val newDroppedQty = "Drop Baru"
    override val costHppLabel = "Modal HPP"
    override val costPriceLabel = "Harga Modal"
    override val sellPriceLabel = "Jual"
    override val notesLabel = "Catatan"
    override val notesPlaceholder = "Catatan tambahan (opsional)"
    override val profitPrefix = "Laba"
    override val subtotalLabel = "Subtotal"
    override val totalAmountDueLabel = "Total Tagihan"
    override val amountPaidLabel = "Uang Diterima"
    override val remainingDebtLabel = "Sisa Bon/Hutang"
    override val paymentStatusLabel = "Status Pembayaran"
    override val paymentStatusPaid = "Lunas"
    override val paymentStatusUnpaid = "Bon / Tempo"
    override val paymentStatusPartial = "Bayar Sebagian"
    override val ownerPrefix = "Pemilik: "
    override val phonePrefix = "No. HP: "
    override val addressPrefix = "Alamat: "
    override val txPrefix = "Transaksi"
    override val todayBadge = "HARI INI"
    override val yesterdayBadge = "KEMARIN"
    override val btnSendWhatsapp = "Kirim Nota via WA"
    override val receiptNumberLabel = "No. Struk"
    override val storeLabel = "Warung"
    override val routeLabel = "Rute"
    override val timeLabel = "Waktu"

    // Routes Screen
    override val routesTitle = "Rute & Warung Titipan"
    override val routesSubtitle = "Sistem Pencatatan Sales Titip Jual (Konsinyasi)"
    override val allRoutes = "Semua Rute"
    override val addRoute = "+ Tambah Rute"
    override val searchStorePlaceholder = "Cari warung, pemilik, alamat..."
    override val routeStatsVisited = "Terkunjungi"
    override val routeStatsConsignmentStock = "Stok Titipan"
    override val routeStatsDebt = "Bon Belum Lunas"
    override val noStoresFound = "Belum Ada Warung"
    override val noStoresFoundDesc = "Tambahkan warung/toko ke rute ini untuk mulai mencatat kunjungan."
    override val addFirstStore = "Tambah Warung Pertama"
    override val storeStatusVisited = "Sudah Dikunjungi"
    override val storeStatusPending = "Belum Dikunjungi"
    override val btnReconciliation = "Hitung Sisa / Tagih"
    override val btnCallStore = "Hubungi Warung"
    override val btnOpenMaps = "Buka Peta"
    override val currentConsignment = "Titipan Aktif"
    override val uncollectedDebt = "Ada Bon/Hutang Lalu"
    override val noActiveConsignment = "Belum ada titip barang"
    override val routeStoresCount: (Int, Int) -> String = { visited, total -> "$visited dari $total warung terkunjungi" }

    // Add Route Dialog
    override val addRouteDialogTitle = "Tambah Rute Baru"
    override val routeNameLabel = "Nama Rute"
    override val routeNamePlaceholder = "Contoh: Rute A - Pasir Mulya"
    override val routeDescriptionLabel = "Keterangan / Jadwal"
    override val routeDescriptionPlaceholder = "Contoh: Jadwal Senin & Kamis"

    // Add Store Dialog
    override val addStoreDialogTitle = "Tambah Warung / Toko Baru"
    override val storeNameLabel = "Nama Warung"
    override val storeNamePlaceholder = "Contoh: Warung Bu Sri"
    override val ownerNameLabel = "Nama Pemilik"
    override val ownerNamePlaceholder = "Contoh: Ibu Sri (Boleh kosong)"
    override val phoneLabel = "Nomor WhatsApp / HP"
    override val phonePlaceholder = "Contoh: 08123456789 (Boleh kosong)"
    override val addressLabel = "Alamat / Lokasi"
    override val addressPlaceholder = "Contoh: Jl. Melati No. 12"
    override val selectRouteLabel = "Pilih Wilayah Rute *"
    override val initialConsignmentSection = "Stok Awal Titipan Barang"
    override val initialConsignmentHint = "Masukkan jumlah barang yang langsung ditinggal/dititipkan pertama kali di warung ini."
    override val autofillAddressTip = "💡 Info: Cukup ketik Nama Warung! Alamat otomatis terisi dari rute, dan Pemilik & No. HP boleh dikosongkan."
    override val autofillRouteAreaChip = "Area Rute"
    override val autofillStoreRouteChip = "Warung + Rute"
    override val optionalTag = "Boleh Kosong"
    override val btnGetGpsLocation = "📍 Ambil Lokasi GPS (Autofill Alamat)"
    override val gpsDetecting = "Sedang mendeteksi koordinat GPS..."
    override val gpsSuccess = "Alamat terisi otomatis dari GPS!"
    override val gpsFailed = "Gagal mengambil lokasi GPS. Pastikan GPS HP aktif."
    override val gpsPermissionNeeded = "Izin lokasi diperlukan untuk autofill GPS."

    // Smart Offline GPS Route Sorting
    override val sortNearestGps = "Terdekat (GPS Offline)"
    override val sortStandardRoute = "Urutan Rute"
    override val gpsLiveActive = "GPS Offline Aktif"
    override val gpsLiveSearching = "Mencari sinyal GPS..."
    override val gpsEnablePrompt = "Aktifkan GPS untuk Urutan Pintar"
    override val smartRouteNearestTitle = "Rute Pintar (Jarak Realtime)"
    override val distanceUnitMeters: (Int) -> String = { m -> "$m m" }
    override val distanceUnitKm: (Double) -> String = { km -> String.format(Locale.US, "%.1f km", km) }
    override val nearestStoreTag = "TERDEKAT BERIKUTNYA"
    override val nextClosestBadge = "Tujuan Terdekat Berikutnya"
    override val unvisitedFirstToggle = "Dahulukan Belum Kunjung"

    // Visit Reconciliation Sheet
    override val sheetTitle = "Rekonsiliasi Kunjungan Warung"
    override val consignReconcileTitle = "Rincian Titip Barang & Sisa"
    override val btnAddAnotherProduct = "+ Produk Lain"
    override val initialDropBadge = "Titip Dasar (Warung Baru)"
    override val initialDropDesc = "Kunjungan pertama / Titip dasar ke warung baru. Belum ada stok lama & belum ada tagihan (Rp 0)."
    override val btnSaveInitialDrop = "Simpan Titip Dasar"

    // Consignment Age & Cycle Tracking
    override val consignmentAgeLabel = "Umur Titipan"
    override val ageNewStore = "Warung Baru (Belum Dititip)"
    override val ageToday = "Dititip Hari Ini"
    override val ageYesterday = "1 Hari Lalu"
    override val ageDaysAgo: (Int) -> String = { days -> "$days Hari Lalu" }
    override val ageDueToday = "Jadwal Ganti (7 Hari)"
    override val ageOverdue: (Int) -> String = { days -> "Lewat Jadwal ($days Hari)" }
    override val filterNeedsVisitDue = "Waktunya Ganti (≥7 Hari)"
    override val filterNewStore = "Warung Baru"
    override val visitNotesLabel = "Catatan Kunjungan (Opsional)"
    override val visitNotesPlaceholder = "Contoh: Ibu titip kerupuk kaleng lagi minggu depan"
    override val customPaymentLabel = "Jumlah Uang Diterima (Rp)"
    override val btnSaveAndReceipt = "Simpan & Buat Nota"
    override val selectProductTitle = "Pilih Produk Titipan"
    override val allProductsAdded = "Semua produk dari katalog sudah ditambahkan ke warung ini."

    // Inventory & Cargo Screen
    override val inventoryTitle = "Muatan & Inventaris"
    override val tabVehicleCargo = "Muatan Kendaraan"
    override val tabFieldStock = "Titipan Lapangan"
    override val tabCatalog = "Katalog Produk"
    override val btnAddProduct = "+ Tambah Produk"
    override val btnAddCargo = "+ Muat Barang"
    override val cargoTotalLoaded = "Total Dimuat"
    override val cargoDistributed = "Didistribusikan"
    override val cargoRemaining = "Sisa di Mobil"
    override val cargoDamaged = "Rusak"
    override val tabHistory = "Riwayat"
    override val setorLabel = "Setor"
    override val setorDone = "Lunas"
    override val setorPending = "Belum Setor"
    override val setorAll = "Setor Semua"
    override val unsetorLabel = "Batalkan Setor"
    override val totalLoadCost = "Total Biaya Muatan"
    override val totalSetor = "Total Disetor"
    override val totalUnsetored = "Belum Disetor"
    override val costPerPack = "Modal / Pack"
    override val emptyHistoryTitle = "Belum Ada Riwayat Muatan"
    override val emptyHistoryDesc = "Muat barang dulu untuk melihat riwayat"
    override val loadFromFactory = "Dari Pabrik"
    override val returnedGoods = "Barang Sisa"
    override val emptyCargoTitle = "Muatan Kendaraan Kosong"
    override val emptyCargoDesc = "Muat stok barang dari gudang ke kendaraan sebelum berangkat keliling rute."
    override val btnLoadCargoNow = "Muat Barang Sekarang"
    override val loadedLabel = "Dimuat"
    override val distributedLabel = "Didistribusikan"
    override val remainingLabel = "Sisa di Kendaraan"
    override val fieldStockTitle = "Stok Titipan di Warung-Warung"
    override val fieldStockValuation = "Nilai Aset Titipan"
    override val storesHoldingStock = "Warung aktif memegang stok"
    override val emptyFieldStock = "Belum ada barang di lapangan"
    override val emptyFieldStockDesc = "Ketika Anda menitipkan barang ke warung, posisi stok akan tercatat di sini."
    override val catalogTitle = "Katalog & Harga Produk"
    override val marginPerPack = "Laba/bks"
    override val addProductDialogTitle = "Tambah Produk Baru"
    override val editProductDialogTitle = "Edit Produk"
    override val productNameLabel = "Nama Produk *"
    override val productNamePlaceholder = "Contoh: Keripik Singkong Balado 200g"
    override val unitNameLabel = "Satuan (Bungkus/Pack/Pcs) *"
    override val unitNamePlaceholder = "Contoh: bks, pcs, pack"
    override val packSizeLabel = "Isi per pack/ikat *"
    override val costPriceFieldLabel = "Harga Modal Pokok HPP (Rp) *"
    override val sellPriceFieldLabel = "Harga Jual ke Warung (Rp) *"
    override val addCargoDialogTitle = "Muat Barang ke Mobil/Motor"
    override val selectProductLabel = "Pilih Produk *"
    override val quantityToLoadLabel = "Jumlah Bungkus yang Dimuat *"
    override val deleteProductConfirmTitle = "Hapus Produk"
    override val deleteProductConfirmDesc: (String) -> String = { name -> "Apakah Anda yakin ingin menghapus '$name'?" }

    // Analytics Screen
    override val analyticsTitle = "Analitik Penjualan Titip"
    override val analyticsSubtitle = "Omset, Laba Bersih & Kinerja Konsinyasi"
    override val filterToday = "Hari Ini"
    override val filter7Days = "7 Hari Terakhir"
    override val filterThisWeek = "Minggu Ini"
    override val filterThisMonth = "Bulan Ini"
    override val filterAllTime = "Semua Tanggal"
    override val filterPickDate = "Pilih Tanggal 📅"
    override val periodIndicator = "PERIODE:"
    override val tabRoutes = "Kunjungan"
    override val totalRevenueStat = "Total Omset"
    override val totalProfitStat = "Laba Bersih Sales"
    override val totalItemsSoldStat = "Total Terjual"
    override val itemsDeliveredDesc = "Bungkus laku & direkonsiliasi"
    override val uncollectedDebtStat = "Bon / Piutang Belum Bayar"
    override val debtRemainingDesc = "Hutang titipan belum lunas"
    override val dailyBreakdownTitle = "Rekap Analitik Harian per Tanggal"
    override val financialSummaryTitle = "Rincian Omset vs Modal Pokok (HPP)"
    override val topSellingTitle = "Produk Paling Laris di Warung"
    override val routePerformanceTitle = "Kinerja Penjualan per Rute"
    override val noTransactionsFound = "Tidak ada transaksi pada periode ini"
    override val noTransactionsFoundDesc = "Nota dan rekonsiliasi kunjungan akan otomatis dihitung di sini."

    // History Screen
    override val historyTitle = "Riwayat Nota & Transaksi"
    override val searchHistoryPlaceholder = "Cari warung, produk, rute, struk..."

    // Receipt Dialog
    override val receiptDialogTitle = "Nota Penjualan Titip Barang"

    // Settings Screen
    override val settingsTitle = "Pengaturan"
    override val languageSettingTitle = "Bahasa Aplikasi"
    override val languageSettingSubtitle = "Pilih bahasa tampilan untuk aplikasi"
    override val themeSettingTitle = "Tema Tampilan"
    override val themeSettingSubtitle = "Pilih mode terang, gelap, atau ikuti sistem ponsel"
    override val themeSystem = "Ikuti Sistem HP"
    override val themeSystemDesc = "Secara otomatis mengikuti pengaturan mode gelap/terang perangkat"
    override val themeLight = "Mode Terang"
    override val themeLightDesc = "Tampilan cerah dan kontras tinggi"
    override val themeDark = "Mode Gelap"
    override val themeDarkDesc = "Nyaman di mata saat malam dan hemat baterai"
    override val offlineBadgeTitle = "100% Offline Lokal"
    override val offlineBadgeDesc = "Semua data tersimpan aman di HP Anda tanpa butuh koneksi internet."
    override val dailyOpsTitle = "Operasional Harian"
    override val btnResetDailyVisits = "Reset Status Kunjungan Warung"
    override val resetDailyVisitsDesc = "Mengubah status semua warung menjadi 'Belum Dikunjungi' untuk memulai rute hari baru."
    override val resetVisitsDialogTitle = "Reset Kunjungan Hari Ini?"
    override val resetVisitsDialogDesc = "Ini akan mengubah status warung menjadi belum dikunjungi untuk hari baru. Seluruh data riwayat transaksi dan nota masa lalu tetap aman."
    override val btnConfirmReset = "Reset Status Kunjungan"
    override val formulaTitle = "Rumus Perhitungan Titip Jual"
    override val formula1 = "Barang Laku = Titip Lalu - Sisa di Warung"
    override val formula2 = "Tagihan Warung = Barang Laku × Harga Jual"
    override val formula3 = "Laba Salesman = Barang Laku × (Harga Jual - Harga Modal HPP)"
    override val formula4 = "Stok Kunjungan Berikutnya = Sisa di Warung + Drop Baru"
    override val appVersionTitle = "Stock Sales Sistem Konsinyasi"
    override val appVersionDesc = "Versi 1.0.0 • Siap Produksi"

    // Backup & Restore / Transfer Data
    override val backupSectionTitle = "Cadangkan & Pulihkan (Pindah HP)"
    override val backupSectionSubtitle = "Export seluruh database ke file cadangan JSON untuk dipindah ke HP baru atau disimpan aman"
    override val btnExportBackup = "Cadangkan / Export Data (JSON)"
    override val exportBackupDesc = "Simpan semua rute, warung, produk, stok konsinyasi, dan riwayat nota ke dalam satu file cadangan yang bisa dikirim via WA/Drive."
    override val btnImportBackup = "Pulihkan / Import Data"
    override val importBackupDesc = "Pulihkan database lengkap dari file cadangan Stock Sales JSON yang pernah diekspor sebelumnya."
    override val importBackupDialogTitle = "Pulihkan Data dari File?"
    override val importBackupDialogDesc = "Proses ini akan menggantikan data saat ini dengan data dari file cadangan. Pastikan file cadangan yang dipilih sudah benar."
    override val btnConfirmImport = "Pulihkan Data Sekarang"
    override val backupExportSuccess = "File cadangan berhasil dibuat! Siap dibagikan ke HP baru atau disimpan."
    override val backupImportSuccess: (Int, Int, Int, Int) -> String = { routes, stores, prods, txs -> "Data berhasil dipulihkan! Memuat $routes rute, $stores warung, $prods produk, dan $txs transaksi nota." }
    override val backupImportFailed = "Gagal memulihkan data. Pastikan format file cadangan Stock Sales valid."

    // PDF Export Reports
    override val btnExportPdfReport = "Cetak / Export Laporan PDF"
    override val exportPdfReportDesc = "Generate dokumen laporan resmi penjualan & finansial konsinyasi dalam format PDF A4."
    override val generatingPdf = "Sedang membuat dokumen PDF..."
    override val pdfExportSuccess = "Dokumen PDF berhasil dibuat!"
    override val pdfShareSubject = "Laporan Rekapitulasi Penjualan & Finansial Stock Sales"
}

fun getAppStrings(language: AppLanguage): AppStrings {
    return when (language) {
        AppLanguage.ENGLISH -> AppStringsEn
        AppLanguage.INDONESIAN -> AppStringsId
    }
}
