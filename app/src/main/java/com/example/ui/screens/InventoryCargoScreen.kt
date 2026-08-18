package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductEntity
import com.example.data.model.VanLoadEntity
import com.example.data.model.VanReturnEntity
import com.example.data.repository.ClosingLoadInput
import com.example.ui.components.EditableNumberStepper
import com.example.ui.components.NumberStepper
import com.example.ui.components.StatCard
import com.example.ui.theme.AppThemeColors
import com.example.ui.util.LocalAppStrings
import com.example.ui.util.LocalAppLanguage
import com.example.ui.util.localizedLabel
import com.example.ui.viewmodel.SalesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryCargoScreen(
    viewModel: SalesViewModel,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(strings.tabVehicleCargo, strings.tabFieldStock, strings.tabHistory, strings.tabCatalog)

    val products by viewModel.allProducts.collectAsState()
    val todayLoads by viewModel.todayLoads.collectAsState()
    val todayDistributedByProduct by viewModel.todayDistributedByProduct.collectAsState()
    val todayReturns by viewModel.todayReturns.collectAsState()
    val allLoads by viewModel.allLoads.collectAsState()
    val fieldStockSummaries by viewModel.fieldStockSummaries.collectAsState()
    val inventoryBucketSummaries by viewModel.inventoryBucketSummaries.collectAsState()
    val bsProductBalances by viewModel.bsProductBalances.collectAsState()
    val todayClosing by viewModel.todayClosing.collectAsState()
    val freshPcs = todayLoads.sumOf { load ->
        val packSize = products.find { it.id == load.productId }?.packSize ?: 1
        load.initialLoadedQty * packSize
    } - todayDistributedByProduct.values.sum()

    LaunchedEffect(Unit) {
        viewModel.normalizeTodayVanLoads()
    }

    var showAddProductDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var showAddLoadDialog by remember { mutableStateOf(false) }
    var showSortBsDialog by remember { mutableStateOf(false) }
    var showClosingDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    title = {
                        Text(
                            text = strings.inventoryTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        if (selectedTab == 3) {
                            IconButton(onClick = { showAddProductDialog = true }) {
                                Icon(Icons.Default.AddCircle, contentDescription = strings.btnAddProduct)
                            }
                        } else if (selectedTab == 0) {
                            Row {
                                IconButton(onClick = { showClosingDialog = true }) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = localizedLabel(LocalAppLanguage.current, "Closing harian", "Daily closing"))
                                }
                                IconButton(onClick = { showAddLoadDialog = true }) {
                                    Icon(Icons.Default.AddShoppingCart, contentDescription = strings.btnAddCargo)
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 3) {
                ExtendedFloatingActionButton(
                    onClick = { showAddProductDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(strings.btnAddProduct) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            } else if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showAddLoadDialog = true },
                    icon = { Icon(Icons.Default.LocalShipping, contentDescription = null) },
                    text = { Text(strings.btnAddCargo) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> CargoLoadTab(
                    loads = todayLoads,
                    products = products,
                    distributedByProduct = todayDistributedByProduct,
                    returns = todayReturns,
                    inventoryBuckets = inventoryBucketSummaries,
                    freshPcs = freshPcs.coerceAtLeast(0),
                    onSortBsClick = { showSortBsDialog = true },
                    closing = todayClosing,
                    viewModel = viewModel,
                    onAddLoadClick = { showAddLoadDialog = true }
                )
                1 -> FieldStockTab(
                    fieldStockSummaries = fieldStockSummaries,
                    viewModel = viewModel
                )
                2 -> LoadHistoryTab(
                    allLoads = allLoads,
                    products = products,
                    viewModel = viewModel
                )
                3 -> ProductCatalogTab(
                    products = products,
                    viewModel = viewModel,
                    onAddProduct = { showAddProductDialog = true },
                    onEditProduct = { productToEdit = it }
                )
            }
        }
    }

    // Add / Edit Product Dialog
    if (showAddProductDialog || productToEdit != null) {
        val isEditing = productToEdit != null
        val editingProd = productToEdit

        var name by remember { mutableStateOf(editingProd?.name ?: "") }
        var unitName by remember { mutableStateOf(editingProd?.unitName ?: "Pack") }
        var packSizeText by remember { mutableStateOf(editingProd?.packSize?.toString() ?: "10") }
        var costPriceText by remember { mutableStateOf(editingProd?.costPrice?.toInt()?.toString() ?: "0") }
        var sellPriceText by remember { mutableStateOf(editingProd?.sellPrice?.toInt()?.toString() ?: "0") }
        var retailPriceText by remember { mutableStateOf(editingProd?.retailPrice?.toInt()?.toString() ?: "0") }
        var skuText by remember { mutableStateOf(editingProd?.sku ?: "") }
        var categoryText by remember { mutableStateOf(editingProd?.category ?: "") }

        val costVal = costPriceText.toDoubleOrNull() ?: 0.0
        val sellVal = sellPriceText.toDoubleOrNull() ?: 0.0
        val profitPerUnit = sellVal - costVal
        val marginPct = if (sellVal > 0) (profitPerUnit / sellVal) * 100 else 0.0

        AlertDialog(
            onDismissRequest = {
                showAddProductDialog = false
                productToEdit = null
            },
            modifier = Modifier.fillMaxWidth(0.92f),
            title = { Text(if (isEditing) strings.editProductDialogTitle else strings.addProductDialogTitle) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("${strings.productNameLabel} *") },
                            placeholder = { Text(strings.productNamePlaceholder) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = unitName,
                                onValueChange = { unitName = it },
                                label = { Text(strings.unitNameLabel) },
                                placeholder = { Text(strings.unitNamePlaceholder) },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = packSizeText,
                                onValueChange = { packSizeText = it },
                                label = { Text(strings.packSizeLabel) },
                                placeholder = { Text("10") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = costPriceText,
                            onValueChange = { costPriceText = it },
                            label = { Text("${strings.costPriceFieldLabel} *") },
                            placeholder = { Text("11000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = sellPriceText,
                            onValueChange = { sellPriceText = it },
                            label = { Text("${strings.sellPriceFieldLabel} *") },
                            placeholder = { Text("15000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        // Real-time Profit Preview Chip
                        Surface(
                            color = AppThemeColors.profitColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.marginPerPack,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppThemeColors.profitColor
                                )
                                Text(
                                    text = "+${SalesViewModel.formatRupiah(profitPerUnit)} / $unitName (${String.format("%.1f", marginPct)}%)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppThemeColors.profitColor
                                )
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = categoryText,
                            onValueChange = { categoryText = it },
                            label = { Text(localizedLabel(LocalAppLanguage.current, "Kategori", "Category")) },
                            placeholder = { Text("Snack / Beverage") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.saveProduct(
                                ProductEntity(
                                    id = editingProd?.id ?: 0L,
                                    name = name.trim(),
                                    unitName = unitName.trim().ifEmpty { "Pack" },
                                    packSize = packSizeText.toIntOrNull() ?: 10,
                                    costPrice = costVal,
                                    sellPrice = sellVal,
                                    retailPrice = retailPriceText.toDoubleOrNull() ?: 0.0,
                                    sku = skuText.trim(),
                                    category = categoryText.trim()
                                )
                            ) {
                                showAddProductDialog = false
                                productToEdit = null
                            }
                        }
                    },
                    enabled = name.isNotBlank() && costVal > 0 && sellVal > 0
                ) {
                    Text(strings.btnSave)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddProductDialog = false
                        productToEdit = null
                    }
                ) {
                    Text(strings.btnCancel)
                }
            }
        )
    }

    // Add Cargo Load Dialog
    if (showAddLoadDialog) {
        var selectedProdId by remember { mutableStateOf(products.firstOrNull()?.id ?: 0L) }
        var loadQtyText by remember { mutableStateOf("0") }
        var costPerPackText by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddLoadDialog = false },
            modifier = Modifier.fillMaxWidth(0.92f),
            title = { Text(strings.addCargoDialogTitle) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.selectProductLabel, style = MaterialTheme.typography.labelMedium)
                    products.forEach { prod ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedProdId == prod.id) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
                                )
                                .padding(8.dp)
                        ) {
                            RadioButton(
                                selected = selectedProdId == prod.id,
                                onClick = { selectedProdId = prod.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(prod.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    OutlinedTextField(
                        value = loadQtyText,
                        onValueChange = { loadQtyText = it },
                        label = { Text(strings.quantityToLoadLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = costPerPackText,
                        onValueChange = { costPerPackText = it },
                        label = { Text(strings.costPerPack) },
                        placeholder = { Text("10000") },
                        leadingIcon = { Text("Rp", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(strings.notesLabel) },
                        placeholder = { Text(strings.notesPlaceholder) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = loadQtyText.toIntOrNull() ?: 0
                        val cost = costPerPackText.toDoubleOrNull() ?: 0.0
                        if (selectedProdId != 0L && qty > 0) {
                            viewModel.saveVanLoad(selectedProdId, qty, cost, notes)
                            showAddLoadDialog = false
                        }
                    }
                ) {
                    Text(strings.btnSave)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLoadDialog = false }) {
                    Text(strings.btnCancel)
                }
            }
        )
    }

    if (showSortBsDialog) {
        SortBsDialog(
            products = products,
            balances = bsProductBalances,
            onDismiss = { showSortBsDialog = false },
            onConfirm = { productId, good, damaged ->
                viewModel.sortBs(productId, good, damaged) { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                showSortBsDialog = false
            }
        )
    }

    if (showClosingDialog) {
        DailyClosingDialog(
            loads = todayLoads,
            products = products,
            existingClosing = todayClosing,
            onDismiss = { showClosingDialog = false },
            onConfirm = { inputs, cash, notes ->
                viewModel.closeToday(inputs, cash, notes) { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                showClosingDialog = false
            }
        )
    }
}

@Composable
fun CargoLoadTab(
    loads: List<VanLoadEntity>,
    products: List<ProductEntity>,
    distributedByProduct: Map<Long, Int>,
    returns: List<VanReturnEntity>,
    inventoryBuckets: List<com.example.data.local.InventoryBucketSummary>,
    freshPcs: Int,
    onSortBsClick: () -> Unit,
    closing: com.example.data.model.DailyClosingEntity?,
    viewModel: SalesViewModel,
    onAddLoadClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    if (loads.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.LocalShipping,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = strings.emptyCargoTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.emptyCargoDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onAddLoadClick) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.btnLoadCargoNow)
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                InventoryBucketsCard(inventoryBuckets, freshPcs, onSortBsClick)
            }
            item {
                ClosingStatusCard(closing)
            }
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column {
                            Text(
                                text = strings.tabVehicleCargo,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${viewModel.todayDateString} • ${loads.size} ${strings.tabCatalog}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(loads, key = { it.id }) { load ->
                val prod = products.find { it.id == load.productId }
                var damagedQty by remember(load) { mutableIntStateOf(load.damagedQty) }
                val packSize = prod?.packSize?.takeIf { it > 0 } ?: 10

                // Semua dikonversi ke PCS untuk kalkulasi konsisten
                val initialLoadedPcs = load.initialLoadedQty * packSize
                val distributedPcs = distributedByProduct[load.productId] ?: 0  // sudah PCS dari reconciliation
                val productReturns = returns.filter { it.productId == load.productId }
                val totalReturnedPcs = productReturns.sumOf { it.returnedQty }  // sudah PCS
                val damagedPcs = damagedQty  // user input = pcs
                val sisaDiMobilPcs = (initialLoadedPcs - distributedPcs + totalReturnedPcs - damagedPcs).coerceAtLeast(0)

                // Format helper: tampilkan X pack + Y pcs kalau ada sisa
                fun formatPcsToPack(totalPcs: Int): String {
                    val packs = totalPcs / packSize
                    val pcs = totalPcs % packSize
                    val unit = prod?.unitName ?: "Pack"
                    return when {
                        packs > 0 && pcs > 0 -> "$packs $unit + $pcs pcs"
                        packs > 0 -> "$packs $unit"
                        else -> "$pcs pcs"
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = prod?.name ?: "Product #${load.productId}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${strings.loadedLabel}: ${load.initialLoadedQty} ${prod?.unitName ?: "Pack"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (load.costPerPack > 0) {
                                    Text(
                                        text = "${strings.costPerPack}: ${SalesViewModel.formatRupiah(load.costPerPack)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.deleteVanLoad(load) }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = strings.btnDelete,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = strings.cargoDistributed,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatPcsToPack(distributedPcs),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Dikembalikan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatPcsToPack(totalReturnedPcs),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = strings.cargoRemaining,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatPcsToPack(sisaDiMobilPcs),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sisaDiMobilPcs > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        if (productReturns.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Dikembalikan dari:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    productReturns.forEach { ret ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = ret.storeName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = formatPcsToPack(ret.returnedQty),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NumberStepper(
                                value = damagedQty,
                                onValueChange = {
                                    damagedQty = it
                                    viewModel.updateVanLoadReturn(load.id, load.returnedQty, damagedQty)
                                },
                                label = strings.cargoDamaged ?: "Damaged"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryBucketsCard(
    summaries: List<com.example.data.local.InventoryBucketSummary>,
    freshPcs: Int,
    onSortBsClick: () -> Unit
) {
    val values = mapOf(
        "FRESH_FACTORY" to ("Fresh pabrik" to MaterialTheme.colorScheme.primary),
        "BS_UNSORTED" to ("BS belum sortir" to MaterialTheme.colorScheme.tertiary),
        "PRIVATE_READY" to ("Pribadi layak jual" to MaterialTheme.colorScheme.secondary),
        "PRIVATE_DAMAGED" to ("Pribadi rusak" to MaterialTheme.colorScheme.error)
    )
    val totals = summaries.associate { it.bucket to it.totalPcs } + ("FRESH_FACTORY" to freshPcs)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(localizedLabel(LocalAppLanguage.current, "Empat laci inventaris", "Four inventory buckets"), fontWeight = FontWeight.ExtraBold)
            Text(
                "Kepemilikan stok terpisah dan mudah diaudit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if ((totals["BS_UNSORTED"] ?: 0) > 0) {
                TextButton(onClick = onSortBsClick) {
                    Text(localizedLabel(LocalAppLanguage.current, "Sortir BS", "Sort damaged stock"))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            values.entries.chunked(2).forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { (bucket, labelAndColor) ->
                        val (label, color) = labelAndColor
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = color.copy(alpha = 0.10f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${totals[bucket] ?: 0} pcs",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = color
                                )
                            }
                        }
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
                if (index < values.entries.chunked(2).lastIndex) Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ClosingStatusCard(closing: com.example.data.model.DailyClosingEntity?) {
    val shortage = closing?.shortage ?: 0.0
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (closing == null) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else if (shortage > 0) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(localizedLabel(LocalAppLanguage.current, "Closing harian", "Daily closing"), fontWeight = FontWeight.ExtraBold)
            if (closing == null) {
                Text(
                    "Belum ditutup. Pastikan sisa fresh dan kas sudah dihitung.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ClosingValue("Tagihan pabrik", SalesViewModel.formatRupiah(closing.factoryDue))
                    ClosingValue("Kas terkumpul", SalesViewModel.formatRupiah(closing.cashCollected))
                    ClosingValue(
                        "Kurang setor",
                        SalesViewModel.formatRupiah(shortage),
                        if (shortage > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ClosingValue(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier = Modifier.widthIn(max = 125.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

@Composable
private fun SortBsDialog(
    products: List<ProductEntity>,
    balances: List<com.example.data.local.InventoryProductBalance>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Int, Int) -> Unit
) {
    var selectedProductId by remember(balances) { mutableStateOf(balances.firstOrNull()?.productId) }
    var goodText by remember { mutableStateOf("") }
    var damagedText by remember { mutableStateOf("") }
    val selectedBalance = balances.firstOrNull { it.productId == selectedProductId }
        ?: balances.firstOrNull()
    val activeProductId = selectedBalance?.productId
    val selectedProduct = products.firstOrNull { it.id == activeProductId }
    val available = selectedBalance?.totalPcs ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.92f),
        title = { Text(localizedLabel(LocalAppLanguage.current, "Sortir BS", "Sort damaged stock")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Pisahkan BS bagus untuk repack dan BS rusak untuk write-off.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (balances.isEmpty()) {
                    Text(localizedLabel(LocalAppLanguage.current, "Belum ada BS yang bisa disortir.", "No damaged stock is ready to sort."))
                } else {
                    Text(localizedLabel(LocalAppLanguage.current, "Produk", "Product"), fontWeight = FontWeight.Bold)
                    balances.forEach { balance ->
                        val product = products.firstOrNull { it.id == balance.productId }
                        FilterChip(
                            selected = balance.productId == activeProductId,
                            onClick = {
                                selectedProductId = balance.productId
                                goodText = ""
                                damagedText = ""
                            },
                            label = {
                                Text("${product?.name ?: "${localizedLabel(LocalAppLanguage.current, "Produk", "Product")} #${balance.productId}"} • ${balance.totalPcs} pcs")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        "Tersedia: ${selectedProduct?.name ?: "Produk"} • $available pcs",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = goodText,
                        onValueChange = { goodText = it.filter(Char::isDigit) },
                        label = { Text(localizedLabel(LocalAppLanguage.current, "BS bagus / repack", "Good stock / repack")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = damagedText,
                        onValueChange = { damagedText = it.filter(Char::isDigit) },
                        label = { Text(localizedLabel(LocalAppLanguage.current, "BS rusak / write-off", "Damaged stock / write-off")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = activeProductId != null,
                onClick = {
                    val productId = activeProductId
                    if (productId != null) {
                        onConfirm(
                            productId,
                            goodText.toIntOrNull() ?: 0,
                            damagedText.toIntOrNull() ?: 0
                        )
                    }
                }
            ) {
                Text(localizedLabel(LocalAppLanguage.current, "Simpan", "Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(localizedLabel(LocalAppLanguage.current, "Batal", "Cancel")) }
        }
    )
}

@Composable
private fun DailyClosingDialog(
    loads: List<VanLoadEntity>,
    products: List<ProductEntity>,
    existingClosing: com.example.data.model.DailyClosingEntity?,
    onDismiss: () -> Unit,
    onConfirm: (List<ClosingLoadInput>, Double, String) -> Unit
) {
    val remainingByLoad = remember(loads) {
        mutableStateMapOf<Long, String>().apply {
            loads.forEach { put(it.id, "0") }
        }
    }
    var cashText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val factoryDue = loads.sumOf { load ->
        val remaining = remainingByLoad[load.id]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        (load.initialLoadedQty - remaining).coerceAtLeast(0) * load.costPerPack
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.92f),
        title = { Text(localizedLabel(LocalAppLanguage.current, "Closing & Setoran Pabrik", "Daily Closing & Factory Settlement")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                existingClosing?.let { closing ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Closing tersimpan: tagihan ${SalesViewModel.formatRupiah(closing.factoryDue)}",
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Text(
                    "Masukkan sisa fresh fisik dalam satuan box. BS tidak mengurangi tagihan pabrik.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (loads.isEmpty()) {
                    Text(localizedLabel(LocalAppLanguage.current, "Belum ada muatan hari ini.", "No cargo load recorded today."))
                } else {
                    loads.forEach { load ->
                        val product = products.firstOrNull { it.id == load.productId }
                        OutlinedTextField(
                            value = remainingByLoad[load.id] ?: "0",
                            onValueChange = { value ->
                                remainingByLoad[load.id] = value.filter(Char::isDigit)
                            },
                            label = {
                                Text("${product?.name ?: "Product #${load.productId}"} • ${localizedLabel(LocalAppLanguage.current, "muat", "loaded")} ${load.initialLoadedQty} box")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Text(
                    "Tagihan pabrik: ${SalesViewModel.formatRupiah(factoryDue)}",
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = cashText,
                    onValueChange = { cashText = it.filter { char -> char.isDigit() } },
                    label = { Text(localizedLabel(LocalAppLanguage.current, "Kas terkumpul dari outlet", "Cash collected from outlets")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(localizedLabel(LocalAppLanguage.current, "Catatan", "Notes")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = loads.isNotEmpty(),
                onClick = {
                    onConfirm(
                        loads.map { load ->
                            ClosingLoadInput(
                                productId = load.productId,
                                loadedBoxes = load.initialLoadedQty,
                                freshRemainingBoxes = remainingByLoad[load.id]?.toIntOrNull() ?: 0,
                                costPerBox = load.costPerPack
                            )
                        },
                        cashText.toDoubleOrNull() ?: 0.0,
                        notes
                    )
                }
            ) { Text(localizedLabel(LocalAppLanguage.current, "Simpan closing", "Save closing")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(localizedLabel(LocalAppLanguage.current, "Batal", "Cancel")) }
        }
    )
}

@Composable
fun FieldStockTab(
    fieldStockSummaries: List<com.example.data.local.ProductFieldStockSummary>,
    viewModel: SalesViewModel
) {
    val strings = LocalAppStrings.current
    val totalFieldPcs = fieldStockSummaries.sumOf { it.totalFieldQuantity }
    val totalPacks = fieldStockSummaries.sumOf {
        val safeSize = if (it.packSize > 0) it.packSize else 1
        it.totalFieldQuantity / safeSize
    }
    val totalValue = fieldStockSummaries.sumOf {
        val safeSize = if (it.packSize > 0) it.packSize else 1
        it.totalFieldQuantity * (it.sellPrice / safeSize)
    }
    val totalCost = fieldStockSummaries.sumOf {
        val safeSize = if (it.packSize > 0) it.packSize else 1
        it.totalFieldQuantity * (it.costPrice / safeSize)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = strings.fieldStockTitle,
                    value = if (totalFieldPcs > 0) "$totalPacks ${strings.packUnit}" else "0 ${strings.packUnit}",
                    subtitle = "$totalFieldPcs pcs • ${strings.storesHoldingStock}",
                    icon = Icons.Default.Storefront,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = strings.fieldStockValuation,
                    value = SalesViewModel.formatRupiah(totalValue),
                    subtitle = "HPP: ${SalesViewModel.formatRupiah(totalCost)}",
                    icon = Icons.Default.MonetizationOn,
                    accentColor = AppThemeColors.successColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                text = strings.fieldStockTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(fieldStockSummaries, key = { it.productId }) { item ->
            val safePackSize = if (item.packSize > 0) item.packSize else 1
            val packs = item.totalFieldQuantity / safePackSize
            val pcs = item.totalFieldQuantity % safePackSize
            val qtyStr = if (packs > 0 && pcs > 0) {
                "$packs ${item.unitName} + $pcs pcs"
            } else if (packs > 0) {
                "$packs ${item.unitName} (${item.totalFieldQuantity} pcs)"
            } else {
                "${item.totalFieldQuantity} pcs"
            }
            val pricePerPc = item.sellPrice / safePackSize
            val itemValuation = item.totalFieldQuantity * pricePerPc

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.productName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${strings.sellPriceLabel} ${SalesViewModel.formatRupiah(item.sellPrice)}/${item.unitName} (${item.packSize} pcs) • ${SalesViewModel.formatRupiah(pricePerPc)}/pc",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = qtyStr,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = SalesViewModel.formatRupiah(itemValuation),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadHistoryTab(
    allLoads: List<VanLoadEntity>,
    products: List<ProductEntity>,
    viewModel: SalesViewModel
) {
    val strings = LocalAppStrings.current

    // Group by date, sorted newest first
    val groupedLoads = remember(allLoads) {
        allLoads.groupBy { it.dateString }
            .toSortedMap(compareByDescending { it })
    }

    if (groupedLoads.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = strings.emptyHistoryTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = strings.emptyHistoryDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            groupedLoads.forEach { (dateStr, loads) ->
                val totalCost = loads.sumOf { it.initialLoadedQty * it.costPerPack }
                val isAllSetored = loads.all { it.isSetored }
                val unsetoredTotal = loads.filter { !it.isSetored }.sumOf { it.initialLoadedQty * it.costPerPack }

                item(key = "header_$dateStr") {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAllSetored) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isAllSetored) MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = dateStr,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${loads.size} ${strings.tabVehicleCargo.lowercase()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = if (isAllSetored) AppThemeColors.successColor.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (isAllSetored) strings.setorDone else strings.setorPending,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAllSetored) AppThemeColors.successColor
                                        else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (totalCost > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(8.dp))

                                loads.forEach { load ->
                                    val prod = products.find { it.id == load.productId }
                                    val packCost = load.initialLoadedQty * load.costPerPack
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${prod?.name ?: "Product #${load.productId}"}: ${load.initialLoadedQty} ${prod?.unitName ?: "Pack"}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = if (load.costPerPack > 0) SalesViewModel.formatRupiah(packCost) else "-",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = strings.totalLoadCost,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = SalesViewModel.formatRupiah(totalCost),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Setor button
                                Spacer(modifier = Modifier.height(10.dp))
                                if (isAllSetored) {
                                    OutlinedButton(
                                        onClick = {
                                            loads.forEach { viewModel.unmarkSetored(it) }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(strings.unsetorLabel)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            loads.filter { !it.isSetored }.forEach { viewModel.markAsSetored(it) }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AppThemeColors.successColor
                                        )
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${strings.setorAll} • ${SalesViewModel.formatRupiah(unsetoredTotal)}")
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "No cost data",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCatalogTab(
    products: List<ProductEntity>,
    viewModel: SalesViewModel,
    onAddProduct: () -> Unit,
    onEditProduct: (ProductEntity) -> Unit
) {
    val strings = LocalAppStrings.current
    if (products.isEmpty()) {
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
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Belum Ada Master Produk",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Daftarkan produk titip jual Anda (nama snack, harga modal pabrik, harga jual ke warung, dan estimasi laba).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAddProduct,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.addProductDialogTitle)
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Text(
                    text = "${strings.catalogTitle} (${products.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(products, key = { it.id }) { product ->
                val profit = product.sellPrice - product.costPrice
                val margin = if (product.sellPrice > 0) (profit / product.sellPrice) * 100 else 0.0

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "1 ${product.unitName} (${product.packSize} pcs) • ${product.category}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row {
                                IconButton(onClick = { onEditProduct(product) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { viewModel.deleteProduct(product) }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = strings.btnDelete,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = strings.costPriceLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = SalesViewModel.formatRupiah(product.costPrice),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Column {
                                Text(
                                    text = strings.sellPriceLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = SalesViewModel.formatRupiah(product.sellPrice),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = strings.marginPerPack,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppThemeColors.profitColor
                                )
                                Text(
                                    text = "+${SalesViewModel.formatRupiah(profit)} (${String.format(java.util.Locale.US, "%.0f", margin)}%)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppThemeColors.profitColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
