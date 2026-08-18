package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ConsignmentProductDetail
import com.example.data.model.ProductEntity
import com.example.data.model.RouteEntity
import com.example.data.model.StoreEntity
import com.example.data.model.VisitTransactionEntity
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.StatusBadge
import com.example.ui.components.StoreDistanceBadge
import com.example.ui.components.StoreVisitAgingBadge
import com.example.ui.theme.DebtBadge
import com.example.ui.theme.ProfitBadge
import com.example.ui.theme.SuccessGreen
import com.example.ui.util.LocalAppStrings
import com.example.ui.util.LocalAppLanguage
import com.example.ui.util.LocationHelper
import com.example.ui.viewmodel.SalesViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

enum class StoreStatusFilter {
    ALL,
    DUE_RESTOCK,
    NEW_STORE,
    HAS_DEBT,
    VISITED_TODAY,
    NOT_VISITED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoresDirectoryScreen(
    viewModel: SalesViewModel,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current
    val context = LocalContext.current

    val allStores by viewModel.allStores.collectAsState()
    val allRoutes by viewModel.allRoutes.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val storeConsignmentsMap by viewModel.storeConsignmentsMap.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedRouteFilterId by remember { mutableStateOf<Long?>(null) }
    var selectedStatusFilter by remember { mutableStateOf(StoreStatusFilter.ALL) }

    var storeToVisit by remember { mutableStateOf<StoreEntity?>(null) }
    var storeToEdit by remember { mutableStateOf<StoreEntity?>(null) }
    var storeToDelete by remember { mutableStateOf<StoreEntity?>(null) }
    var storeToWriteOff by remember { mutableStateOf<StoreEntity?>(null) }
    var showAddStoreDialog by remember { mutableStateOf(false) }
    var storeFormError by remember { mutableStateOf<String?>(null) }
    var lastCompletedTransaction by remember { mutableStateOf<VisitTransactionEntity?>(null) }
    var sortByNearestGps by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf<Location?>(null) }

    // Live Offline GPS Tracking
    val liveGpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            LocationHelper.startOfflineLocationTracking(context) { loc ->
                userLocation = loc
            }
        }
    }

    DisposableEffect(context) {
        var stopTracking: (() -> Unit)? = null
        if (LocationHelper.hasLocationPermission(context)) {
            stopTracking = LocationHelper.startOfflineLocationTracking(context) { loc ->
                userLocation = loc
            }
        }
        onDispose {
            stopTracking?.invoke()
        }
    }

    val storeDistanceMap = remember(allStores, userLocation) {
        allStores.associate { store ->
            store.id to LocationHelper.getDistanceToStoreMeters(userLocation, store)
        }
    }

    // Filter and Sort stores
    val filteredStores = remember(allStores, searchQuery, selectedRouteFilterId, selectedStatusFilter, sortByNearestGps, storeDistanceMap, userLocation) {
        val list = allStores.filter { store ->
            val matchesSearch = searchQuery.isBlank() ||
                    store.name.contains(searchQuery, ignoreCase = true) ||
                    store.ownerName.contains(searchQuery, ignoreCase = true) ||
                    store.phone.contains(searchQuery, ignoreCase = true) ||
                    store.address.contains(searchQuery, ignoreCase = true)

            val matchesRoute = selectedRouteFilterId == null || store.routeId == selectedRouteFilterId

            val matchesStatus = when (selectedStatusFilter) {
                StoreStatusFilter.ALL -> true
                StoreStatusFilter.DUE_RESTOCK -> {
                    if (store.lastVisitedDate == null) false
                    else {
                        val days = ((System.currentTimeMillis() - store.lastVisitedDate) / (1000L * 60 * 60 * 24)).toInt()
                        days >= 6
                    }
                }
                StoreStatusFilter.NEW_STORE -> store.lastVisitedDate == null
                StoreStatusFilter.HAS_DEBT -> store.outstandingDebt > 0
                StoreStatusFilter.VISITED_TODAY -> store.isVisitedToday
                StoreStatusFilter.NOT_VISITED -> !store.isVisitedToday
            }

            matchesSearch && matchesRoute && matchesStatus
        }

        if (sortByNearestGps && userLocation != null) {
            list.sortedWith(
                compareBy<StoreEntity> { store ->
                    storeDistanceMap[store.id] ?: (10000000.0 + store.orderIndex)
                }.thenBy { store ->
                    store.name
                }
            )
        } else {
            list
        }
    }

    val totalStores = allStores.size
    val totalDebt = allStores.sumOf { it.outstandingDebt }
    val visitedTodayCount = allStores.count { it.isVisitedToday }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = strings.storesTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.storesSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                IconButton(onClick = { storeFormError = null; showAddStoreDialog = true }) {
                        Icon(Icons.Default.AddBusiness, contentDescription = strings.addStoreDialogTitle)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { storeFormError = null; showAddStoreDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(strings.addStoreDialogTitle) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Stats Overview Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = strings.totalStoresStat,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$totalStores",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1.2f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (totalDebt > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = strings.totalDebtStat,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (totalDebt > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = SalesViewModel.formatRupiah(totalDebt),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (totalDebt > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = strings.visitedStoresStat,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$visitedTodayCount / $totalStores",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.storesSearchPlaceholder, style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // Route Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedRouteFilterId == null,
                    onClick = { selectedRouteFilterId = null },
                    label = { Text(strings.filterAllRoutes) }
                )

                allRoutes.forEach { route ->
                    val isSelected = selectedRouteFilterId == route.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedRouteFilterId = route.id },
                        label = { Text(route.name) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        try {
                                            Color(android.graphics.Color.parseColor(route.colorHex))
                                        } catch (e: Exception) {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                            )
                        }
                    )
                }
            }

            // Status Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // GPS Nearest Sort Toggle
                FilterChip(
                    selected = sortByNearestGps,
                    onClick = {
                        sortByNearestGps = !sortByNearestGps
                        if (sortByNearestGps && userLocation == null) {
                            if (LocationHelper.hasLocationPermission(context)) {
                                LocationHelper.startOfflineLocationTracking(context) { loc ->
                                    userLocation = loc
                                }
                            } else {
                                liveGpsPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                    },
                    label = {
                        Text(
                            text = strings.sortNearestGps,
                            fontWeight = if (sortByNearestGps) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )

                FilterChip(
                    selected = selectedStatusFilter == StoreStatusFilter.ALL,
                    onClick = { selectedStatusFilter = StoreStatusFilter.ALL },
                    label = { Text(strings.filterAllStatus) }
                )
                FilterChip(
                    selected = selectedStatusFilter == StoreStatusFilter.DUE_RESTOCK,
                    onClick = { selectedStatusFilter = StoreStatusFilter.DUE_RESTOCK },
                    label = { Text("⚠️ ${strings.filterNeedsVisitDue}") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                )
                FilterChip(
                    selected = selectedStatusFilter == StoreStatusFilter.NEW_STORE,
                    onClick = { selectedStatusFilter = StoreStatusFilter.NEW_STORE },
                    label = { Text("🌟 ${strings.filterNewStore}") }
                )
                FilterChip(
                    selected = selectedStatusFilter == StoreStatusFilter.HAS_DEBT,
                    onClick = { selectedStatusFilter = StoreStatusFilter.HAS_DEBT },
                    label = { Text(strings.filterHasDebt) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                    )
                )
                FilterChip(
                    selected = selectedStatusFilter == StoreStatusFilter.VISITED_TODAY,
                    onClick = { selectedStatusFilter = StoreStatusFilter.VISITED_TODAY },
                    label = { Text(strings.filterVisitedToday) }
                )
                FilterChip(
                    selected = selectedStatusFilter == StoreStatusFilter.NOT_VISITED,
                    onClick = { selectedStatusFilter = StoreStatusFilter.NOT_VISITED },
                    label = { Text(strings.filterNotVisited) }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Stores List
            if (filteredStores.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Storefront,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = strings.emptyStoresFilter,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.emptyStoresFilterDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredStores, key = { it.id }) { store ->
                        val parentRoute = allRoutes.find { it.id == store.routeId }
                        val consignments = storeConsignmentsMap[store.id] ?: emptyList()

                        MasterStoreCard(
                            store = store,
                            route = parentRoute,
                            consignments = consignments,
                            distanceMeters = storeDistanceMap[store.id],
                            onVisit = { storeToVisit = store },
                            onEdit = { storeToEdit = store },
                            onDelete = { storeToDelete = store },
                            onWriteOff = { storeToWriteOff = store }
                        )
                    }
                }
            }
        }
    }

    // Active Visit Reconciliation Sheet
    if (storeToVisit != null) {
        val store = storeToVisit!!
        val route = allRoutes.find { it.id == store.routeId }
            ?: RouteEntity(id = store.routeId, name = "Rute", dayOfWeek = "")
        val storeConsignments by viewModel.getStoreConsignments(store.id).collectAsState(initial = emptyList())

        VisitReconciliationSheet(
            store = store,
            route = route,
            initialConsignments = storeConsignments,
            allProducts = allProducts,
            viewModel = viewModel,
            onDismiss = { storeToVisit = null },
            onSuccessTransaction = { transaction ->
                storeToVisit = null
                lastCompletedTransaction = transaction
            }
        )
    }

    // Completed Transaction Receipt Dialog
    if (lastCompletedTransaction != null) {
        val allTx by viewModel.allTransactions.collectAsState()
        val currentTxWithItems = allTx.find { it.transaction.id == lastCompletedTransaction!!.id }

        if (currentTxWithItems != null) {
            ReceiptDialog(
                transaction = currentTxWithItems.transaction,
                items = currentTxWithItems.items,
                onDismiss = { lastCompletedTransaction = null }
            )
        }
    }

    // Add Store Dialog (With Route Selector & GPS Autofill)
    if (showAddStoreDialog) {
        MasterStoreFormDialog(
            title = strings.addStoreDialogTitle,
            initialStore = null,
            routes = allRoutes,
            preselectedRouteId = selectedRouteFilterId,
            gpsAccuracyThreshold = viewModel.gpsAccuracyThreshold.value,
            externalError = storeFormError,
            onDismiss = { showAddStoreDialog = false },
            onSave = { newStore ->
                storeFormError = null
                viewModel.saveStore(newStore, onDone = { showAddStoreDialog = false }, onError = { storeFormError = it })
            }
        )
    }

    // Edit Store Dialog (With Route Selector & GPS Autofill)
    if (storeToEdit != null) {
        MasterStoreFormDialog(
            title = strings.btnEditStore,
            initialStore = storeToEdit,
            routes = allRoutes,
            preselectedRouteId = storeToEdit?.routeId,
            gpsAccuracyThreshold = viewModel.gpsAccuracyThreshold.value,
            externalError = storeFormError,
            onDismiss = { storeToEdit = null },
            onSave = { updatedStore ->
                storeFormError = null
                viewModel.saveStore(updatedStore, onDone = { storeToEdit = null }, onError = { storeFormError = it })
            }
        )
    }

    // Delete Store Confirmation
    if (storeToDelete != null) {
        val store = storeToDelete!!
        AlertDialog(
            onDismissRequest = { storeToDelete = null },
            modifier = Modifier.fillMaxWidth(0.92f),
            title = { Text(strings.deleteStoreDialogTitle) },
            text = {
                Text(strings.deleteStoreConfirmMsg.replace("{name}", store.name))
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStore(store)
                        storeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(strings.btnDelete)
                }
            },
            dismissButton = {
                TextButton(onClick = { storeToDelete = null }) {
                    Text(strings.btnCancel)
                }
            }
        )
    }

    if (storeToWriteOff != null) {
        val store = storeToWriteOff!!
        var reason by remember(store.id) { mutableStateOf("Warung blacklist / bangkrut") }
        AlertDialog(
            onDismissRequest = { storeToWriteOff = null },
            modifier = Modifier.fillMaxWidth(0.92f),
            title = { Text(if (language.code == "id") "Hapus Buku Piutang" else "Debt Write-off") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${if (language.code == "id") "Piutang" else "Debt"} ${store.name}: ${SalesViewModel.formatRupiah(store.outstandingDebt)}")
                    Text(if (language.code == "id") "Saldo dipindahkan ke laporan kerugian dan histori transaksi tetap tersimpan." else "The balance moves to the loss report while transaction history remains intact.")
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text(if (language.code == "id") "Alasan" else "Reason") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.writeOffStoreDebt(store, reason) { storeToWriteOff = null }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(if (language.code == "id") "Hapus Buku" else "Write Off") }
            },
            dismissButton = {
                TextButton(onClick = { storeToWriteOff = null }) { Text(strings.btnCancel) }
            }
        )
    }
}

@Composable
fun MasterStoreCard(
    store: StoreEntity,
    route: RouteEntity?,
    consignments: List<ConsignmentProductDetail>,
    distanceMeters: Double? = null,
    onVisit: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onWriteOff: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (store.isVisitedToday)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (store.isVisitedToday) SuccessGreen.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Store Name + Route Badge + Visited Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        StorePhotoThumbnail(photoUri = store.photoUri, size = 48.dp)
                        Column {
                            Text(
                                text = store.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                    if (route != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        try {
                                            Color(android.graphics.Color.parseColor(route.colorHex))
                                        } catch (e: Exception) {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                            )
                            Text(
                                text = route.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                        }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StoreLifecycleBadge(status = store.status)
                    if (store.isVisitedToday) {
                        StatusBadge(status = "VISITED")
                    } else {
                        StatusBadge(status = "PENDING")
                    }
                    StoreVisitAgingBadge(lastVisitedDate = store.lastVisitedDate)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Details: Owner, Phone, Address + Real-time Offline Distance
            if (store.ownerName.isNotBlank() || store.phone.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (store.ownerName.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                Icons.Outlined.Person,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = store.ownerName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (store.phone.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = store.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (store.address.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = store.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }

                if (distanceMeters != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    StoreDistanceBadge(distanceMeters = distanceMeters)
                }
            }

            // Outstanding Debt / Bon Banner
            if (store.outstandingDebt > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = strings.uncollectedDebt,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            text = SalesViewModel.formatRupiah(store.outstandingDebt),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                val remainingCredit = (store.creditLimit - store.outstandingDebt).coerceAtLeast(0.0)
                Text(
                    text = "${strings.remainingCreditLabel}: ${SalesViewModel.formatRupiah(remainingCredit)} / ${SalesViewModel.formatRupiah(store.creditLimit)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (remainingCredit <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (store.debtSince != null) {
                    val debtDays = ((System.currentTimeMillis() - store.debtSince) / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0)
                    Text(
                        text = "${strings.debtAgeLabel}: $debtDays hari",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (debtDays > 21) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Consignments preview
            if (consignments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val totalPcs = consignments.sumOf { it.currentDroppedQuantity }
                val totalPacks = consignments.sumOf {
                    val s = if (it.packSize > 0) it.packSize else 1
                    it.currentDroppedQuantity / s
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${strings.consignmentItemsBadge}: ${consignments.size} SKU ($totalPacks ${strings.packUnit} • $totalPcs pcs)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Call/WA Button
                if (store.phone.isNotBlank()) {
                    FilledTonalIconButton(
                        onClick = {
                            val cleanPhone = store.phone.replace(Regex("[^0-9]"), "")
                            val formattedPhone = if (cleanPhone.startsWith("0")) "62" + cleanPhone.substring(1) else cleanPhone
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://wa.me/$formattedPhone?text=Halo%20${Uri.encode(store.ownerName.ifBlank { store.name })}%2C%20saya%20sales%20mau%20konfirmasi%20kunjungan%20stok%20titipan.")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Tidak dapat membuka WhatsApp", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = "WhatsApp",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF25D366)
                        )
                    }
                }

                // Quick Maps Button
                if (store.address.isNotBlank()) {
                    FilledTonalIconButton(
                        onClick = {
                            val uri = Uri.parse("geo:0,0?q=${Uri.encode(store.address + " " + store.name)}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                val genericMap = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(store.address)}"))
                                try {
                                    context.startActivity(genericMap)
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Directions,
                            contentDescription = "Maps",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Edit Button
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = strings.btnEditStore,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = strings.btnDeleteStore,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                if (store.outstandingDebt > 0) {
                    IconButton(
                        onClick = onWriteOff,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = "Write-off piutang",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Reconcile / Visit Button
                Button(
                    onClick = onVisit,
                    enabled = store.status == "ACTIVE",
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (store.isVisitedToday)
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.primary,
                        contentColor = if (store.isVisitedToday)
                            MaterialTheme.colorScheme.onSecondary
                        else
                            MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Checklist,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (store.status == "ACTIVE") strings.btnReconcileVisit else strings.storeBlacklisted,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreLifecycleBadge(status: String) {
    val isIndonesian = LocalAppLanguage.current.code == "id"
    val (label, container, content) = when (status) {
        "BLACKLISTED" -> Triple(if (isIndonesian) "BLACKLIST" else "BLACKLISTED", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        "TEMPORARILY_CLOSED" -> Triple(if (isIndonesian) "TUTUP SEMENTARA" else "TEMPORARILY CLOSED", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        else -> Triple(if (isIndonesian) "AKTIF" else "ACTIVE", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = container
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun StorePhotoThumbnail(photoUri: String?, size: androidx.compose.ui.unit.Dp) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = photoUri) {
        value = if (photoUri.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(Uri.parse(photoUri)).use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = LocalAppStrings.current.storePhotoDescription,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Default.Storefront,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(10.dp).fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterStoreFormDialog(
    title: String,
    initialStore: StoreEntity?,
    routes: List<RouteEntity>,
    preselectedRouteId: Long?,
    gpsAccuracyThreshold: Int = 20,
    externalError: String? = null,
    onDismiss: () -> Unit,
    onSave: (StoreEntity) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedRouteId by remember {
        mutableStateOf(
            initialStore?.routeId ?: preselectedRouteId ?: routes.firstOrNull()?.id ?: 0L
        )
    }
    var storeName by remember { mutableStateOf(initialStore?.name ?: "") }
    var ownerName by remember { mutableStateOf(initialStore?.ownerName ?: "") }
    var phone by remember { mutableStateOf(initialStore?.phone ?: "") }
    var address by remember { mutableStateOf(initialStore?.address ?: "") }
    var latitude by remember { mutableStateOf(initialStore?.latitude) }
    var longitude by remember { mutableStateOf(initialStore?.longitude) }
    var photoUri by remember { mutableStateOf(initialStore?.photoUri) }
    var notes by remember { mutableStateOf(initialStore?.notes ?: "") }
    var storeStatus by remember { mutableStateOf(initialStore?.status ?: "ACTIVE") }
    var creditLimitText by remember {
        mutableStateOf(initialStore?.creditLimit?.toLong()?.toString() ?: "500000")
    }
    var isAddressManuallyEdited by remember { mutableStateOf(false) }
    var isDetectingGps by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }
            photoUri = uri.toString()
        }
    }

    val activeRoute = routes.find { it.id == selectedRouteId }
    val routeArea = activeRoute?.areaDescription?.ifBlank { activeRoute.name } ?: ""

    val runGpsDetection: () -> Unit = {
        scope.launch {
            isDetectingGps = true
            Toast.makeText(context, strings.gpsDetecting, Toast.LENGTH_SHORT).show()
            try {
                val loc = LocationHelper.getCurrentLocation(context)
                if (loc != null) {
                    latitude = loc.latitude
                    longitude = loc.longitude
                    val addr = LocationHelper.getAddressFromLocation(context, loc, routeArea)
                    address = addr
                    isAddressManuallyEdited = true
                    val accuracyWarning = if (loc.hasAccuracy() && loc.accuracy > gpsAccuracyThreshold) {
                        " Koordinat tersimpan, tetapi akurasi ${loc.accuracy.toInt()}m di atas batas ${gpsAccuracyThreshold}m."
                    } else ""
                    Toast.makeText(context, strings.gpsSuccess + accuracyWarning, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, strings.gpsFailed, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, strings.gpsFailed, Toast.LENGTH_LONG).show()
            } finally {
                isDetectingGps = false
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            runGpsDetection()
        } else {
            Toast.makeText(context, strings.gpsPermissionNeeded, Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.92f),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Route Selector (Dropdown / Chips)
                Text(
                    text = strings.changeRouteLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    routes.forEach { route ->
                        val isSelected = route.id == selectedRouteId
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedRouteId = route.id },
                            label = { Text(route.name) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            try {
                                                Color(android.graphics.Color.parseColor(route.colorHex))
                                            } catch (e: Exception) {
                                                MaterialTheme.colorScheme.primary
                                            }
                                        )
                                )
                            }
                        )
                    }
                }

                // Store Name
                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text(strings.storeNameLabel) },
                    placeholder = { Text(strings.storeNamePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = strings.storeLifecycleLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "ACTIVE" to strings.storeActive,
                        "TEMPORARILY_CLOSED" to strings.storeTemporarilyClosed,
                        "BLACKLISTED" to strings.storeBlacklisted
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = storeStatus == value,
                            onClick = { storeStatus = value },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = creditLimitText,
                    onValueChange = { value ->
                        creditLimitText = value.filter(Char::isDigit).take(12)
                    },
                    label = { Text(strings.creditLimitLabel) },
                    placeholder = { Text(strings.creditLimitPlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Owner Name
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text(strings.ownerNameLabel) },
                    placeholder = { Text(strings.ownerNamePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Phone
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(strings.phoneLabel) },
                    placeholder = { Text(strings.phonePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // GPS Quick Autofill Button
                FilledTonalButton(
                    onClick = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            runGpsDetection()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    enabled = !isDetectingGps,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    if (isDetectingGps) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.gpsDetecting, style = MaterialTheme.typography.labelMedium)
                    } else {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            strings.btnGetGpsLocation,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                OutlinedButton(
                    onClick = { photoPickerLauncher.launch(arrayOf("image/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (photoUri == null) strings.addStorePhoto else strings.replaceStorePhoto)
                }
                StorePhotoThumbnail(photoUri = photoUri, size = 92.dp)

                if (latitude != null && longitude != null) {
                    Text(
                        text = "${strings.storedCoordinatesLabel}: %.6f, %.6f".format(Locale.US, latitude ?: 0.0, longitude ?: 0.0),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // Address
                externalError?.let { error ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        isAddressManuallyEdited = true
                    },
                    label = { Text(strings.addressLabel) },
                    placeholder = { Text(strings.addressPlaceholder) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.notesLabel) },
                    placeholder = { Text(strings.notesPlaceholder) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (storeName.isNotBlank() && selectedRouteId > 0) {
                        val finalStore = initialStore?.copy(
                            routeId = selectedRouteId,
                            name = storeName.trim(),
                            ownerName = ownerName.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            latitude = latitude,
                            longitude = longitude,
                            photoUri = photoUri,
                            notes = notes.trim(),
                            status = storeStatus,
                            creditLimit = creditLimitText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 500_000.0
                        ) ?: StoreEntity(
                            routeId = selectedRouteId,
                            name = storeName.trim(),
                            ownerName = ownerName.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            latitude = latitude,
                            longitude = longitude,
                            photoUri = photoUri,
                            notes = notes.trim(),
                            status = storeStatus,
                            creditLimit = creditLimitText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 500_000.0
                        )
                        onSave(finalStore)
                    } else if (selectedRouteId <= 0) {
                        Toast.makeText(context, strings.selectRouteRequired, Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = storeName.isNotBlank() && selectedRouteId > 0
            ) {
                Text(strings.btnSave)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.btnCancel)
            }
        }
    )
}
