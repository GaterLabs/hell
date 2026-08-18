package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.ConsignmentProductDetail
import com.example.data.model.ProductEntity
import com.example.data.model.RouteEntity
import com.example.data.model.StoreEntity
import com.example.data.model.VisitTransactionEntity
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.RouteMapCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.StoreDistanceBadge
import com.example.ui.components.StoreVisitAgingBadge
import com.example.ui.theme.AppThemeColors
import com.example.ui.util.LocalAppStrings
import com.example.ui.util.LocationHelper
import com.example.ui.viewmodel.SalesViewModel
import com.example.service.LocationTrackingService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(
    viewModel: SalesViewModel,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val routes by viewModel.allRoutes.collectAsState()
    val stores by viewModel.allStores.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    val storeConsignmentsMap by viewModel.storeConsignmentsMap.collectAsState()
    val selectedRouteId by viewModel.selectedRouteId.collectAsState()

    // Default route selection
    LaunchedEffect(routes) {
        if (selectedRouteId == null && routes.isNotEmpty()) {
            viewModel.selectRoute(routes.first().id)
        }
    }

    val activeRoute = routes.find { it.id == selectedRouteId } ?: routes.firstOrNull()
    val filteredStores = remember(stores, selectedRouteId) {
        if (selectedRouteId != null) {
            stores.filter { it.routeId == selectedRouteId }
        } else {
            stores
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSmartGpsSort by remember { mutableStateOf(true) }
    var prioritizePending by remember { mutableStateOf(true) }
    var userLocation by remember { mutableStateOf<Location?>(null) }
    var isRequestingGps by remember { mutableStateOf(false) }

    // Request GPS permission and start live continuous offline tracking
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

    // Auto-start offline location tracking if permission already granted
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

    // Calculate real-time offline distance for every store in the route
    val storeDistanceMap = remember(filteredStores, userLocation) {
        filteredStores.associate { store ->
            store.id to LocationHelper.getDistanceToStoreMeters(userLocation, store)
        }
    }

    // Sort stores dynamically (Smart Proximity Order vs Standard Route Order)
    val sortedStores = remember(filteredStores, isSmartGpsSort, prioritizePending, storeDistanceMap, userLocation) {
        val list = filteredStores
        if (isSmartGpsSort && userLocation != null) {
            list.sortedWith(
                compareBy<StoreEntity> { store ->
                    if (prioritizePending) (if (store.isVisitedToday) 1 else 0) else 0
                }.thenBy { store ->
                    storeDistanceMap[store.id] ?: (10000000.0 + store.orderIndex)
                }.thenBy { store ->
                    store.orderIndex
                }
            )
        } else if (prioritizePending) {
            list.sortedWith(
                compareBy<StoreEntity> { store ->
                    if (store.isVisitedToday) 1 else 0
                }.thenBy { store ->
                    store.orderIndex
                }
            )
        } else {
            list.sortedBy { it.orderIndex }
        }
    }

    // Identify the #1 next closest store that has NOT been visited yet
    val nextClosestStoreId = remember(sortedStores, storeDistanceMap, userLocation) {
        if (userLocation != null && isSmartGpsSort) {
            sortedStores.firstOrNull { !it.isVisitedToday && storeDistanceMap[it.id] != null }?.id
        } else null
    }

    val displayStores = remember(sortedStores, searchQuery) {
        if (searchQuery.isBlank()) {
            sortedStores
        } else {
            sortedStores.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.ownerName.contains(searchQuery, ignoreCase = true) ||
                        it.address.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    var storeToVisit by remember { mutableStateOf<StoreEntity?>(null) }
    var showAddStoreDialog by remember { mutableStateOf(false) }
    var showAddRouteDialog by remember { mutableStateOf(false) }
    var lastCompletedTransaction by remember { mutableStateOf<VisitTransactionEntity?>(null) }

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
                            text = strings.routesTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.routesSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddStoreDialog = true }) {
                        Icon(Icons.Default.AddBusiness, contentDescription = strings.addStoreDialogTitle)
                    }
                    IconButton(onClick = { showAddRouteDialog = true }) {
                        Icon(Icons.Default.AltRoute, contentDescription = strings.addRouteDialogTitle)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddStoreDialog = true },
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
            // Horizontal Route Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                routes.forEach { route ->
                    val isSelected = route.id == selectedRouteId
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectRoute(route.id) },
                        label = {
                            Text(
                                text = route.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
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

                IconButton(
                    onClick = { showAddRouteDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = strings.addRouteDialogTitle,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Route Header Card with Stats
            if (activeRoute != null) {
                val visitedCount = filteredStores.count { it.isVisitedToday }
                val totalCount = filteredStores.size
                val totalOutstandingInRoute = filteredStores.sumOf { it.outstandingDebt }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeRoute.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (activeRoute.areaDescription.isNotEmpty()) {
                                    Text(
                                        text = activeRoute.areaDescription,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                color = if (visitedCount == totalCount && totalCount > 0) AppThemeColors.successColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = strings.routeStoresCount(visitedCount, totalCount),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (visitedCount == totalCount && totalCount > 0) AppThemeColors.successColor else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (totalOutstandingInRoute > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${strings.routeStatsDebt}: ${SalesViewModel.formatRupiah(totalOutstandingInRoute)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = AppThemeColors.debtColor
                            )
                        }
                    }
                }
            }

            // GPS Tracking Map
            if (activeRoute != null) {
                val todayDateStr = viewModel.todayDateString
                val gpsPoints by viewModel.getGpsPoints(activeRoute.id, todayDateStr)
                    .collectAsState(initial = emptyList())
                val gpsSession by viewModel.getGpsSession(activeRoute.id, todayDateStr)
                    .collectAsState(initial = null)
                val trackingState by LocationTrackingService.trackingState.collectAsState()
                val isTrackingActive = trackingState.isRunning
                val currentTrackingRouteId = trackingState.routeId

                val routeStores = remember(stores, activeRoute) {
                    stores.filter { it.routeId == activeRoute.id }
                }

                // Permission launcher for GPS tracking (all at once)
                val bgPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        val intent = Intent(context, LocationTrackingService::class.java).apply {
                            action = LocationTrackingService.ACTION_START
                            putExtra(LocationTrackingService.EXTRA_ROUTE_ID, activeRoute.id)
                        }
                        ContextCompat.startForegroundService(context, intent)
                    }
                }

                val gpsTrackingPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    if (granted) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val hasBgLoc = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!hasBgLoc) {
                                bgPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                return@rememberLauncherForActivityResult
                            }
                        }
                        val intent = Intent(context, LocationTrackingService::class.java).apply {
                            action = LocationTrackingService.ACTION_START
                            putExtra(LocationTrackingService.EXTRA_ROUTE_ID, activeRoute.id)
                        }
                        ContextCompat.startForegroundService(context, intent)
                    }
                }

                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                    RouteMapCard(
                        routeId = activeRoute.id,
                        routeName = activeRoute.name,
                        gpsPoints = gpsPoints,
                        stores = routeStores,
                        isTrackingActive = isTrackingActive,
                        currentTrackingRouteId = currentTrackingRouteId,
                        todayDateString = todayDateStr,
                        onStartTracking = {
                            // Check permissions
                            val hasFineLoc = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasFineLoc) {
                                // On Android 10+, need background location too
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val hasBgLoc = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!hasBgLoc) {
                                        gpsTrackingPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                            )
                                        )
                                        return@RouteMapCard
                                    }
                                }
                                // Already have all permissions, start directly
                                val intent = Intent(context, LocationTrackingService::class.java).apply {
                                    action = LocationTrackingService.ACTION_START
                                    putExtra(LocationTrackingService.EXTRA_ROUTE_ID, activeRoute.id)
                                }
                                ContextCompat.startForegroundService(context, intent)
                            } else {
                                gpsTrackingPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        onStopTracking = {
                            val intent = Intent(context, LocationTrackingService::class.java).apply {
                                action = LocationTrackingService.ACTION_STOP
                            }
                            context.startService(intent)
                        },
                        onDeleteHistory = {
                            viewModel.deleteGpsHistory(activeRoute.id, todayDateStr)
                        }
                    )
                }
            }

            // Smart GPS Controls Bar (100% Offline Proximity Sorting)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Toggle Smart GPS Nearest vs Route Order
                FilterChip(
                    selected = isSmartGpsSort,
                    onClick = {
                        isSmartGpsSort = !isSmartGpsSort
                        if (isSmartGpsSort && userLocation == null) {
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
                            text = if (isSmartGpsSort) strings.sortNearestGps else strings.sortStandardRoute,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSmartGpsSort) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isSmartGpsSort) Icons.Default.NearMe else Icons.Default.FormatListNumbered,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )

                // Prioritize Pending / Unvisited Stores Toggle
                FilterChip(
                    selected = prioritizePending,
                    onClick = { prioritizePending = !prioritizePending },
                    label = {
                        Text(
                            text = strings.unvisitedFirstToggle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (prioritizePending) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (prioritizePending) Icons.Default.CheckCircleOutline else Icons.Default.PlaylistPlay,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )

                // GPS Live Status Badge
                Surface(
                    color = if (userLocation != null) AppThemeColors.successColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (userLocation != null) AppThemeColors.successColor.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.clickable {
                        if (LocationHelper.hasLocationPermission(context)) {
                            LocationHelper.startOfflineLocationTracking(context) { loc ->
                                userLocation = loc
                            }
                            Toast.makeText(context, "Sinyal GPS Offline: Aktif", Toast.LENGTH_SHORT).show()
                        } else {
                            liveGpsPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (userLocation != null) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                            contentDescription = null,
                            tint = if (userLocation != null) AppThemeColors.successColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (userLocation != null) strings.gpsLiveActive else strings.gpsEnablePrompt,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (userLocation != null) AppThemeColors.successColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchStorePlaceholder) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Stores List / Empty State
            if (routes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AltRoute,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Belum Ada Rute Operasional",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Mulai buat rute kunjungan harian (misal: Rute Senin - Pasar Minggu) untuk mengelompokkan warung dan toko langganan.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showAddRouteDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tambah Rute Pertama")
                        }
                    }
                }
            } else if (displayStores.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Storefront,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) strings.noStoresFound else strings.noStoresFound,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.noStoresFoundDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { showAddStoreDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddBusiness, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.addStoreDialogTitle)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(displayStores, key = { it.id }) { store ->
                        val consignments = storeConsignmentsMap[store.id] ?: emptyList()
                        StoreCardItem(
                            store = store,
                            consignments = consignments,
                            distanceMeters = storeDistanceMap[store.id],
                            isNextClosest = (store.id == nextClosestStoreId),
                            onVisitClick = {
                                storeToVisit = store
                            },
                            onCallClick = {
                                if (store.phone.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${store.phone}"))
                                    context.startActivity(intent)
                                }
                            },
                            onDeleteClick = {
                                viewModel.deleteStore(store)
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet for Visit Reconciliation
    if (storeToVisit != null && activeRoute != null) {
        val consignmentsFlow = viewModel.getStoreConsignments(storeToVisit!!.id)
        val consignments by consignmentsFlow.collectAsState(initial = emptyList())

        VisitReconciliationSheet(
            store = storeToVisit!!,
            route = activeRoute,
            initialConsignments = consignments,
            allProducts = products,
            viewModel = viewModel,
            onDismiss = { storeToVisit = null },
            onSuccessTransaction = { tx ->
                storeToVisit = null
                lastCompletedTransaction = tx
            }
        )
    }

    // Receipt Print / WA Share Dialog after transaction
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

    // Add Store Dialog
    if (showAddStoreDialog && activeRoute != null) {
        val scope = rememberCoroutineScope()
        var newStoreName by remember { mutableStateOf("") }
        var newOwnerName by remember { mutableStateOf("") }
        var newPhone by remember { mutableStateOf("") }
        var newAddress by remember { mutableStateOf("") }
        var newNotes by remember { mutableStateOf("") }
        var newLatitude by remember { mutableStateOf<Double?>(null) }
        var newLongitude by remember { mutableStateOf<Double?>(null) }
        var isAddressManuallyEdited by remember { mutableStateOf(false) }
        var isDetectingGps by remember { mutableStateOf(false) }

        val routeArea = activeRoute.areaDescription.ifBlank { activeRoute.name }

        fun generateAutoAddress(storeName: String): String {
            return if (routeArea.isNotBlank()) {
                routeArea
            } else {
                storeName
            }
        }

        val runGpsDetection: () -> Unit = {
            scope.launch {
                isDetectingGps = true
                Toast.makeText(context, strings.gpsDetecting, Toast.LENGTH_SHORT).show()
                try {
                    val loc = com.example.ui.util.LocationHelper.getCurrentLocation(context)
                    if (loc != null) {
                        newLatitude = loc.latitude
                        newLongitude = loc.longitude
                        val addr = com.example.ui.util.LocationHelper.getAddressFromLocation(context, loc, routeArea)
                        newAddress = addr
                        isAddressManuallyEdited = true
                        Toast.makeText(context, strings.gpsSuccess, Toast.LENGTH_SHORT).show()
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
            onDismissRequest = { showAddStoreDialog = false },
            modifier = Modifier.fillMaxWidth(0.92f),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        strings.addStoreDialogTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tip banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = strings.autofillAddressTip,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Store Name (Required)
                    OutlinedTextField(
                        value = newStoreName,
                        onValueChange = { input ->
                            newStoreName = input
                            // If user hasn't manually typed an address, automatically populate it
                            if (!isAddressManuallyEdited) {
                                newAddress = generateAutoAddress(input)
                            }
                        },
                        label = { Text("${strings.storeNameLabel} *") },
                        placeholder = { Text(strings.storeNamePlaceholder) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // GPS Quick Autofill Button
                    FilledTonalButton(
                        onClick = {
                            if (com.example.ui.util.LocationHelper.hasLocationPermission(context)) {
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

                    // Address (Auto-filled & Editable)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = newAddress,
                            onValueChange = {
                                newAddress = it
                                isAddressManuallyEdited = true
                            },
                            label = {
                                Text(
                                    if (!isAddressManuallyEdited && newAddress.isNotBlank())
                                        "${strings.addressLabel} (⚡ Auto)"
                                    else
                                        "${strings.addressLabel} (${strings.optionalTag})"
                                )
                            },
                            placeholder = { Text(strings.addressPlaceholder) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            },
                            trailingIcon = {
                                IconButton(onClick = {
                                    newAddress = generateAutoAddress(newStoreName)
                                    isAddressManuallyEdited = false
                                }) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = "Autofill Address",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Quick suggestion chips for address
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SuggestionChip(
                                onClick = {
                                    if (com.example.ui.util.LocationHelper.hasLocationPermission(context)) {
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
                                label = { Text("📍 GPS Posisi", style = MaterialTheme.typography.labelSmall) }
                            )
                            if (routeArea.isNotBlank()) {
                                SuggestionChip(
                                    onClick = {
                                        newAddress = routeArea
                                        isAddressManuallyEdited = false
                                    },
                                    label = { Text("📍 $routeArea", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            if (newStoreName.isNotBlank() && routeArea.isNotBlank()) {
                                SuggestionChip(
                                    onClick = {
                                        newAddress = "${newStoreName.trim()}, $routeArea"
                                        isAddressManuallyEdited = true
                                    },
                                    label = { Text("🏪 ${newStoreName.trim()}, $routeArea", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            SuggestionChip(
                                onClick = {
                                    newAddress = "Jl. "
                                    isAddressManuallyEdited = true
                                },
                                label = { Text("🛣️ Jl. ...", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    // Owner Name (Optional)
                    OutlinedTextField(
                        value = newOwnerName,
                        onValueChange = { newOwnerName = it },
                        label = { Text("${strings.ownerNameLabel} (${strings.optionalTag})") },
                        placeholder = { Text(strings.ownerNamePlaceholder) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.PersonOutline, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Phone Number (Optional)
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("${strings.phoneLabel} (${strings.optionalTag})") },
                        placeholder = { Text(strings.phonePlaceholder) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Notes (Optional)
                    OutlinedTextField(
                        value = newNotes,
                        onValueChange = { newNotes = it },
                        label = { Text("${strings.notesLabel} (${strings.optionalTag})") },
                        placeholder = { Text(strings.notesPlaceholder) },
                        leadingIcon = {
                            Icon(Icons.Default.Notes, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newStoreName.isNotBlank()) {
                            val finalAddress = newAddress.trim().ifBlank {
                                generateAutoAddress(newStoreName.trim())
                            }
                            viewModel.saveStore(
                                StoreEntity(
                                    routeId = activeRoute.id,
                                    name = newStoreName.trim(),
                                    ownerName = newOwnerName.trim(),
                                    phone = newPhone.trim(),
                                    address = finalAddress,
                                    latitude = newLatitude,
                                    longitude = newLongitude,
                                    notes = newNotes.trim()
                                )
                            ) {
                                showAddStoreDialog = false
                            }
                        }
                    },
                    enabled = newStoreName.isNotBlank()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.btnSave)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStoreDialog = false }) {
                    Text(strings.btnCancel)
                }
            }
        )
    }

    // Add Route Dialog
    if (showAddRouteDialog) {
        var routeNameInput by remember { mutableStateOf("") }
        var dayOfWeekInput by remember { mutableStateOf("Monday") }
        var areaInput by remember { mutableStateOf("") }
        val daysList = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        AlertDialog(
            onDismissRequest = { showAddRouteDialog = false },
            modifier = Modifier.fillMaxWidth(0.92f),
            title = { Text(strings.addRouteDialogTitle) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = routeNameInput,
                        onValueChange = { routeNameInput = it },
                        label = { Text("${strings.routeNameLabel} *") },
                        placeholder = { Text(strings.routeNamePlaceholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(strings.routeDescriptionLabel, style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        daysList.forEach { day ->
                            FilterChip(
                                selected = dayOfWeekInput == day,
                                onClick = { dayOfWeekInput = day },
                                label = { Text(day) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = areaInput,
                        onValueChange = { areaInput = it },
                        label = { Text(strings.routeDescriptionLabel) },
                        placeholder = { Text(strings.routeDescriptionPlaceholder) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (routeNameInput.isNotBlank()) {
                            viewModel.saveRoute(
                                RouteEntity(
                                    name = routeNameInput.trim(),
                                    dayOfWeek = dayOfWeekInput,
                                    areaDescription = areaInput.trim(),
                                    colorHex = listOf("#0D9488", "#2563EB", "#7C3AED", "#D97706", "#059669", "#DC2626").random()
                                )
                            ) {
                                showAddRouteDialog = false
                            }
                        }
                    },
                    enabled = routeNameInput.isNotBlank()
                ) {
                    Text(strings.btnSave)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRouteDialog = false }) {
                    Text(strings.btnCancel)
                }
            }
        )
    }
}

@Composable
fun StoreCardItem(
    store: StoreEntity,
    consignments: List<ConsignmentProductDetail>,
    distanceMeters: Double? = null,
    isNextClosest: Boolean = false,
    onVisitClick: () -> Unit,
    onCallClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val strings = LocalAppStrings.current

    val successClr = AppThemeColors.successColor
    val debtClr = AppThemeColors.debtColor

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNextClosest) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isNextClosest) 3.dp else 1.dp),
        border = BorderStroke(
            if (isNextClosest) 2.dp else 1.dp,
            if (isNextClosest) MaterialTheme.colorScheme.primary
            else if (store.isVisitedToday) successClr.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Next Closest Store Banner
            if (isNextClosest && !store.isVisitedToday) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = strings.nextClosestBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (store.isVisitedToday) successClr.copy(alpha = 0.15f)
                                else if (isNextClosest) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (store.isVisitedToday) Icons.Default.CheckCircle
                            else if (isNextClosest) Icons.Default.Navigation
                            else Icons.Default.Store,
                            contentDescription = null,
                            tint = if (store.isVisitedToday) successClr else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = store.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${strings.ownerPrefix}${store.ownerName.ifEmpty { "-" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StatusBadge(
                        status = if (store.isVisitedToday) strings.storeStatusVisited else strings.storeStatusPending
                    )
                    StoreVisitAgingBadge(lastVisitedDate = store.lastVisitedDate)
                }
            }

            // Store Location & Real-time Offline Distance
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
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
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = store.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (distanceMeters != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    StoreDistanceBadge(
                        distanceMeters = distanceMeters,
                        isNextClosest = isNextClosest && !store.isVisitedToday
                    )
                }
            }

            // Consignments Preview
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = if (consignments.isNotEmpty()) {
                            "📦 ${strings.currentConsignment}: " + consignments.joinToString(", ") { item ->
                                val safePackSize = if (item.packSize > 0) item.packSize else 1
                                val packs = item.currentDroppedQuantity / safePackSize
                                val pcs = item.currentDroppedQuantity % safePackSize
                                val qtyStr = if (packs > 0 && pcs > 0) {
                                    "$packs ${item.unitName} + $pcs pcs"
                                } else if (packs > 0) {
                                    "$packs ${item.unitName}"
                                } else {
                                    "${item.currentDroppedQuantity} pcs"
                                }
                                "${item.productName} ($qtyStr)"
                            }
                        } else {
                            "📦 ${strings.noActiveConsignment}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Debt Warning
            if (store.outstandingDebt > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "⚠️ ${strings.uncollectedDebt}: ${SalesViewModel.formatRupiah(store.outstandingDebt)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = debtClr
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (store.phone.isNotBlank()) {
                    FilledTonalIconButton(
                        onClick = onCallClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = strings.btnCallStore, modifier = Modifier.size(18.dp))
                    }
                }

                Button(
                    onClick = onVisitClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (store.isVisitedToday) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        contentColor = if (store.isVisitedToday) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        Icons.Default.Calculate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.btnReconciliation,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
