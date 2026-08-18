package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ConsignmentProductDetail
import com.example.data.model.ProductEntity
import com.example.data.model.RouteEntity
import com.example.data.model.StoreEntity
import com.example.data.model.VisitTransactionEntity
import com.example.data.repository.ReconciliationItemInput
import com.example.ui.components.EditableNumberStepper
import com.example.ui.components.NumberStepper
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AppThemeColors
import com.example.ui.theme.DebtBadge
import com.example.ui.theme.ProfitBadge
import com.example.ui.theme.SuccessGreen
import com.example.ui.util.LocalAppStrings
import com.example.ui.util.LocationHelper
import com.example.ui.viewmodel.SalesViewModel

data class ItemReconciliationState(
    val productId: Long,
    val productName: String,
    val unitName: String,
    val packSize: Int,
    val costPrice: Double,       // Harga modal per Pack
    val sellPrice: Double,       // Harga jual per Pack
    var previousStockPcs: Int,   // Total Pcs titipan sebelumnya
    var remainingPcs: Int = 0,   // Sisa fisik di warung dalam Pcs (default 0)
    var newDroppedPacks: Int = 0,// Tambahan titipan baru (dalam satuan Pack - default 0)
    var newDroppedPcs: Int = 0,  // Tambahan titipan baru (eceran Pcs - default 0)
    val sourceBucket: String = "FRESH_FACTORY"
) {
    val safePackSize: Int get() = if (packSize > 0) packSize else 1
    val pricePerPc: Double get() = sellPrice / safePackSize
    val costPerPc: Double get() = costPrice / safePackSize

    val soldPcs: Int get() = (previousStockPcs - remainingPcs).coerceAtLeast(0)
    val subtotalDue: Double get() = soldPcs * pricePerPc
    val subtotalProfit: Double get() = soldPcs * (pricePerPc - costPerPc)

    val totalNewDroppedPcs: Int get() = (newDroppedPacks * safePackSize) + newDroppedPcs
    val finalStockPcs: Int get() = remainingPcs + totalNewDroppedPcs

    fun formatPackAndPcs(totalPcs: Int): String {
        val packs = totalPcs / safePackSize
        val pcs = totalPcs % safePackSize
        return if (packs > 0 && pcs > 0) {
            "$packs $unitName + $pcs pcs"
        } else if (packs > 0) {
            "$packs $unitName ($totalPcs pcs)"
        } else {
            "$totalPcs pcs"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitReconciliationSheet(
    store: StoreEntity,
    route: RouteEntity,
    initialConsignments: List<ConsignmentProductDetail>,
    allProducts: List<ProductEntity>,
    viewModel: SalesViewModel,
    onDismiss: () -> Unit,
    onSuccessTransaction: (VisitTransactionEntity) -> Unit
) {
    val strings = LocalAppStrings.current
    val focusManager = LocalFocusManager.current
    val profitClr = AppThemeColors.profitColor
    val debtClr = AppThemeColors.debtColor
    val context = androidx.compose.ui.platform.LocalContext.current

    // Reconciliation rows state (Dual Pack & Pcs aware)
    val itemsState = remember(initialConsignments) {
        val list = if (initialConsignments.isNotEmpty()) {
            initialConsignments.map {
                val packSize = if (it.packSize > 0) it.packSize else 10
                ItemReconciliationState(
                    productId = it.productId,
                    productName = it.productName,
                    unitName = it.unitName,
                    packSize = packSize,
                    costPrice = it.costPrice,
                    sellPrice = it.sellPrice,
                    previousStockPcs = it.currentDroppedQuantity,
                    remainingPcs = 0,
                    newDroppedPacks = 0,
                    newDroppedPcs = 0
                )
            }.toMutableStateList()
        } else {
            allProducts.take(1).map {
                val packSize = if (it.packSize > 0) it.packSize else 10
                ItemReconciliationState(
                    productId = it.id,
                    productName = it.name,
                    unitName = it.unitName,
                    packSize = packSize,
                    costPrice = it.costPrice,
                    sellPrice = it.sellPrice,
                    previousStockPcs = 0,
                    remainingPcs = 0,
                    newDroppedPacks = 0,
                    newDroppedPcs = 0
                )
            }.toMutableStateList()
        }
        list
    }

    var showAddProductDialog by remember { mutableStateOf(false) }
    var notesText by remember { mutableStateOf("") }
    var previousDebtPaid by remember { mutableStateOf(0.0) }
    var customAmountPaidText by remember { mutableStateOf("") }
    var isCustomPayment by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var gpsError by remember { mutableStateOf<String?>(null) }
    var shouldSubmitAfterPermission by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) shouldSubmitAfterPermission = true
        else gpsError = "Izin lokasi wajib untuk menyelesaikan kunjungan"
    }

    // Computations
    val totalSoldDue = itemsState.sumOf { it.subtotalDue }
    val totalProfit = itemsState.sumOf { it.subtotalProfit }
    val totalItemsSoldPcs = itemsState.sumOf { it.soldPcs }
    val totalStoreMustPay = totalSoldDue + previousDebtPaid

    val effectiveAmountPaid = if (isCustomPayment) {
        customAmountPaidText.toDoubleOrNull() ?: 0.0
    } else {
        totalStoreMustPay
    }

    val paymentStatus = when {
        effectiveAmountPaid >= totalStoreMustPay -> "PAID"
        effectiveAmountPaid > 0 -> "PARTIAL"
        else -> "UNPAID"
    }

    val isInitialDrop = itemsState.all { it.previousStockPcs == 0 } && store.outstandingDebt == 0.0

    LaunchedEffect(shouldSubmitAfterPermission) {
        if (!shouldSubmitAfterPermission) return@LaunchedEffect
        shouldSubmitAfterPermission = false
        isSubmitting = true
        gpsError = null
        try {
            val location = LocationHelper.getCurrentLocation(context)
            if (location == null) {
                gpsError = "Lokasi GPS belum tersedia. Pastikan GPS aktif."
                return@LaunchedEffect
            }
            val itemsInput = itemsState.map {
                ReconciliationItemInput(
                    productId = it.productId,
                    productName = it.productName,
                    unitName = it.unitName,
                    packSize = it.safePackSize,
                    previousStock = it.previousStockPcs,
                    remainingStock = it.remainingPcs,
                    soldQty = it.soldPcs,
                    newDroppedQty = it.totalNewDroppedPcs,
                    costPrice = it.costPerPc,
                    sellPrice = it.pricePerPc,
                    sourceBucket = it.sourceBucket
                )
            }
            viewModel.executeReconciliation(
                store = store,
                route = route,
                items = itemsInput,
                amountPaid = effectiveAmountPaid,
                previousDebtPaid = previousDebtPaid,
                paymentStatus = paymentStatus,
                notes = notesText,
                visitLocation = location,
                onSuccess = onSuccessTransaction,
                onError = { message ->
                    gpsError = message
                    isSubmitting = false
                }
            )
        } catch (error: Exception) {
            gpsError = error.message ?: "Validasi GPS gagal"
        } finally {
            isSubmitting = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header: Store Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = store.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${strings.ownerPrefix}${store.ownerName.ifEmpty { "-" }} • ${route.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (store.outstandingDebt > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = debtClr.copy(alpha = 0.15f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = strings.uncollectedDebt,
                                style = MaterialTheme.typography.labelSmall,
                                color = debtClr
                            )
                            Text(
                                text = SalesViewModel.formatRupiah(store.outstandingDebt),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = debtClr
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Titip Dasar (First Drop / New Store) Banner
            if (isInitialDrop) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "🌟 ${strings.initialDropBadge}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = strings.initialDropDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Debt Payment checkbox/stepper if store has previous debt
            if (store.outstandingDebt > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bayar Hutang/Bon Lalu",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Sisa bon toko: ${SalesViewModel.formatRupiah(store.outstandingDebt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = previousDebtPaid == store.outstandingDebt,
                                onClick = {
                                    previousDebtPaid = if (previousDebtPaid == store.outstandingDebt) 0.0 else store.outstandingDebt
                                },
                                label = {
                                    Text(if (previousDebtPaid > 0) "Lunas Bon" else "Tagih Bon")
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Reconciliation List Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.consignReconcileTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Ketik/atur sisa fisik (Pcs) → Otomatis kalkulasi laku & tagihan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(onClick = { showAddProductDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.btnAddAnotherProduct)
                }
            }

            // List of Reconciled Products
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(itemsState) { index, item ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Product Title & Unit Conversion Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.productName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "1 ${item.unitName} = ${item.packSize} pcs | ${SalesViewModel.formatRupiah(item.sellPrice)}/${item.unitName} (${SalesViewModel.formatRupiah(item.pricePerPc)}/pc)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (itemsState.size > 1) {
                                    IconButton(
                                        onClick = { itemsState.removeAt(index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = strings.btnDelete,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Sumber stok titip baru", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = item.sourceBucket == "FRESH_FACTORY",
                                    onClick = { itemsState[index] = item.copy(sourceBucket = "FRESH_FACTORY") },
                                    label = { Text("Fresh pabrik") }
                                )
                                FilterChip(
                                    selected = item.sourceBucket == "PRIVATE_READY",
                                    onClick = { itemsState[index] = item.copy(sourceBucket = "PRIVATE_READY") },
                                    label = { Text("Pribadi repack") }
                                )
                            }

                            // 1. RECONCILIATION SUMMARY: STOK LALU & LAKU TERJUAL (2-Column Cards)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // A. Previous Stock Card
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = strings.previousStock,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${item.previousStockPcs} Pcs",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "(${item.previousStockPcs / item.safePackSize} ${item.unitName})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // B. Sold Quantity & Subtotal Card
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "${strings.soldPcsLabel.uppercase()} (LAKU)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${item.soldPcs} Pcs",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = SalesViewModel.formatRupiah(item.subtotalDue),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 2. DEDICATED SISA FISIK INPUT BLOCK (Manual Text Input + Stepper + Shortcut Chips)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Inventory2,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "${strings.remainingPcsLabel} (Sisa Fisik di Warung)",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Text(
                                            text = "Maks: ${item.previousStockPcs} pcs",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Manual Input & Stepper Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledTonalIconButton(
                                            onClick = {
                                                if (item.remainingPcs > 0) {
                                                    itemsState[index] = item.copy(remainingPcs = item.remainingPcs - 1)
                                                }
                                            },
                                            enabled = item.remainingPcs > 0,
                                            modifier = Modifier.size(40.dp),
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Kurang 1 pc")
                                        }

                                        // Center Direct Editable Text Field
                                        OutlinedTextField(
                                            value = if (item.remainingPcs == 0) "0" else item.remainingPcs.toString(),
                                            onValueChange = { inputStr ->
                                                val digits = inputStr.filter { it.isDigit() }
                                                val num = digits.toIntOrNull() ?: 0
                                                val clamped = num.coerceIn(0, item.previousStockPcs)
                                                itemsState[index] = item.copy(remainingPcs = clamped)
                                            },
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Done
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onDone = { focusManager.clearFocus() }
                                            ),
                                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                                textAlign = TextAlign.Center,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            suffix = {
                                                Text(
                                                    "pcs",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(54.dp)
                                        )

                                        FilledTonalIconButton(
                                            onClick = {
                                                if (item.remainingPcs < item.previousStockPcs) {
                                                    itemsState[index] = item.copy(remainingPcs = item.remainingPcs + 1)
                                                }
                                            },
                                            enabled = item.remainingPcs < item.previousStockPcs,
                                            modifier = Modifier.size(40.dp),
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Tambah 1 pc")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Quick Remaining Shortcuts (Habis / Sisa 2 / 1/2 Pack / Utuh)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        SuggestionChip(
                                            onClick = { itemsState[index] = item.copy(remainingPcs = 0) },
                                            label = { Text("Habis (0)", fontSize = 11.sp) }
                                        )
                                        if (item.previousStockPcs >= 2) {
                                            SuggestionChip(
                                                onClick = { itemsState[index] = item.copy(remainingPcs = 2.coerceAtMost(item.previousStockPcs)) },
                                                label = { Text("Sisa 2", fontSize = 11.sp) }
                                            )
                                        }
                                        if (item.safePackSize <= item.previousStockPcs && item.safePackSize > 2) {
                                            SuggestionChip(
                                                onClick = {
                                                    val half = (item.safePackSize / 2).coerceIn(0, item.previousStockPcs)
                                                    itemsState[index] = item.copy(remainingPcs = half)
                                                },
                                                label = { Text("½ Pack", fontSize = 11.sp) }
                                            )
                                        }
                                        SuggestionChip(
                                            onClick = { itemsState[index] = item.copy(remainingPcs = item.previousStockPcs) },
                                            label = { Text("Utuh (${item.previousStockPcs})", fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Subtotal Note & Calculation Result
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${strings.subtotalLabel}: ${SalesViewModel.formatRupiah(item.subtotalDue)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${item.soldPcs} pcs × ${SalesViewModel.formatRupiah(item.pricePerPc)}/pc | ${strings.profitPrefix}: +${SalesViewModel.formatRupiah(item.subtotalProfit)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = profitClr,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 3. RESTOCK / TAMBAH TITIPAN BARU UNTUK PERIODE BERIKUTNYA
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Tambah Titipan Baru (Restock)",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Drop barang baru ke warung",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        EditableNumberStepper(
                                            value = item.newDroppedPacks,
                                            onValueChange = { newVal ->
                                                itemsState[index] = item.copy(newDroppedPacks = newVal)
                                            },
                                            minValue = 0,
                                            maxValue = 100,
                                            unit = item.unitName
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Ending Consignment Preview: Sisa + Baru = Total Ditinggal
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "📦 Titipan baru: ${item.totalNewDroppedPcs} Pcs | Sisa ${item.remainingPcs} pcs → kembali ke muatan",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    // Notes field
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text(strings.visitNotesLabel) },
                        placeholder = { Text(strings.visitNotesPlaceholder) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sticky Bottom Summary & Payment
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${strings.soldLabel} ($totalItemsSoldPcs pcs):",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = SalesViewModel.formatRupiah(totalSoldDue),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${strings.totalProfitStat}:",
                            style = MaterialTheme.typography.bodySmall,
                            color = profitClr
                        )
                        Text(
                            text = "+${SalesViewModel.formatRupiah(totalProfit)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = profitClr
                        )
                    }

                    if (previousDebtPaid > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Bayar Hutang/Bon Lama:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "+${SalesViewModel.formatRupiah(previousDebtPaid)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${strings.totalAmountDueLabel.uppercase()}:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = SalesViewModel.formatRupiah(totalStoreMustPay),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Payment Quick Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isCustomPayment,
                            onClick = { isCustomPayment = false },
                            label = { Text("${strings.paymentStatusPaid} (${SalesViewModel.formatRupiah(totalStoreMustPay)})") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = isCustomPayment,
                            onClick = {
                                isCustomPayment = true
                                customAmountPaidText = "0"
                            },
                            label = { Text(strings.paymentStatusUnpaid) },
                            modifier = Modifier.weight(0.7f)
                        )
                    }

                    if (isCustomPayment) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = customAmountPaidText,
                            onValueChange = { customAmountPaidText = it },
                            label = { Text(strings.customPaymentLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

            Spacer(modifier = Modifier.height(12.dp))

            gpsError?.let { message ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.LocationOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(message, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

                    // Action Button: Selesai & Buat Struk
                    Button(
                        onClick = {
                            gpsError = null
                            if (LocationHelper.hasLocationPermission(context)) {
                                shouldSubmitAfterPermission = true
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        val statusLabel = when (paymentStatus) {
                            "PAID" -> strings.paymentStatusPaid
                            "PARTIAL" -> strings.paymentStatusPartial
                            else -> strings.paymentStatusUnpaid
                        }
                        val buttonText = if (isInitialDrop && totalSoldDue == 0.0) {
                            strings.btnSaveInitialDrop
                        } else {
                            "${strings.btnSaveAndReceipt} ($statusLabel)"
                        }
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Dialog to add another product
    if (showAddProductDialog) {
        val existingProductIds = itemsState.map { it.productId }.toSet()
        val availableProducts = allProducts.filter { it.id !in existingProductIds }

        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            modifier = Modifier.fillMaxWidth(0.92f),
            title = { Text(strings.selectProductTitle) },
            text = {
                if (availableProducts.isEmpty()) {
                    Text(strings.allProductsAdded)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableProducts.size) { idx ->
                            val prod = availableProducts[idx]
                            val packSize = if (prod.packSize > 0) prod.packSize else 10
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        itemsState.add(
                                            ItemReconciliationState(
                                                productId = prod.id,
                                                productName = prod.name,
                                                unitName = prod.unitName,
                                                packSize = packSize,
                                                costPrice = prod.costPrice,
                                                sellPrice = prod.sellPrice,
                                                previousStockPcs = 0,
                                                remainingPcs = 0,
                                                newDroppedPacks = 0,
                                                newDroppedPcs = 0
                                            )
                                        )
                                        showAddProductDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(prod.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${strings.sellPriceLabel} ${SalesViewModel.formatRupiah(prod.sellPrice)}/${prod.unitName} ($packSize pcs)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(Icons.Default.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddProductDialog = false }) {
                    Text(strings.btnCancel)
                }
            }
        )
    }
}
