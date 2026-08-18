package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.NavigationItem
import com.example.ui.theme.AppThemeColors
import com.example.ui.viewmodel.SalesViewModel
import com.example.ui.util.LocalAppStrings
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: SalesViewModel,
    onOpenDrawer: () -> Unit,
    onNavigate: (NavigationItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val stores by viewModel.allStores.collectAsState()
    val routes by viewModel.allRoutes.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    val loads by viewModel.todayLoads.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val fieldStock by viewModel.fieldStockSummaries.collectAsState()

    val todayKey = viewModel.todayDateString.replace("-", "")
    val todayTransactions = transactions.filter {
        val key = java.text.SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            .format(java.util.Date(it.transaction.visitTimestamp))
        key == todayKey
    }
    val visited = stores.count { it.isVisitedToday }
    val revenue = todayTransactions.sumOf { it.transaction.totalAmountDue }
    val privateProfit = todayTransactions.flatMap { it.items }
        .filter { it.sourceBucket == "PRIVATE_READY" }
        .sumOf { it.subtotalProfit }
    val factoryProfit = todayTransactions.flatMap { it.items }
        .filter { it.sourceBucket != "PRIVATE_READY" }
        .sumOf { it.subtotalProfit }
    val debt = stores.sumOf { it.outstandingDebt }
    val overdueStores = stores.count { store ->
        store.outstandingDebt > 0 && (store.debtSince ?: store.lastVisitedDate)?.let {
            (System.currentTimeMillis() - it) >= 21L * 24 * 60 * 60 * 1000
        } == true
    }
    val fieldPcs = fieldStock.sumOf { it.totalFieldQuantity }
    val loadedPcs = loads.sumOf { load ->
        val packSize = products.find { it.id == load.productId }?.packSize ?: 1
        load.initialLoadedQty * packSize
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = strings.navDashboard,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = viewModel.todayDateString,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                DashboardHero(
                    visited = visited,
                    totalStores = stores.size,
                    onVisitRoutes = { onNavigate(NavigationItem.ROUTES) }
                )
            }

            item {
                DashboardSectionTitle("Ringkasan hari ini", "Operasional dan kas berjalan")
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Storefront,
                        label = "Kunjungan",
                        value = "$visited/${stores.size}",
                        accent = MaterialTheme.colorScheme.primary
                    )
                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.TrendingUp,
                        label = "Penjualan",
                        value = formatRupiah(revenue),
                        accent = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            item {
                DashboardSectionTitle("Sumber laba", "Pisahkan performa aset pabrik dan pribadi")
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LocalShipping,
                        label = "Laba pabrik",
                        value = formatRupiah(factoryProfit),
                        accent = MaterialTheme.colorScheme.primary
                    )
                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.TrendingUp,
                        label = "Laba pribadi",
                        value = formatRupiah(privateProfit),
                        accent = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            item {
                DebtAgingCard(overdueStores, debt)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AccountBalanceWallet,
                        label = "Piutang",
                        value = formatRupiah(debt),
                        accent = if (debt > 0) AppThemeColors.debtColor else MaterialTheme.colorScheme.primary
                    )
                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Inventory2,
                        label = "Stok lapangan",
                        value = "$fieldPcs pcs",
                        accent = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            item {
                DashboardSectionTitle("Aksi cepat", "Satu tap untuk pekerjaan utama")
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionChip("Kunjungan", Icons.Default.Storefront) {
                        onNavigate(NavigationItem.STORES)
                    }
                    QuickActionChip("Muatan", Icons.Default.LocalShipping) {
                        onNavigate(NavigationItem.INVENTORY)
                    }
                    QuickActionChip("Rute", Icons.Default.Route) {
                        onNavigate(NavigationItem.ROUTES)
                    }
                }
            }

            item {
                DashboardSectionTitle("Kontrol stok", "Muatan fresh dan aset konsinyasi")
            }

            item {
                ElevatedCard(
                    onClick = { onNavigate(NavigationItem.INVENTORY) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Muatan fresh hari ini", fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${loads.size} produk • $loadedPcs pcs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "$fieldPcs pcs sedang dititipkan di warung",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                DashboardSectionTitle("Rute aktif", "${routes.size} rute terdaftar")
            }
        }
    }
}

@Composable
private fun DashboardHero(visited: Int, totalStores: Int, onVisitRoutes: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Siap keliling hari ini?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$visited dari $totalStores warung sudah dikunjungi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            FilterChip(
                selected = false,
                onClick = onVisitRoutes,
                label = { Text("Mulai") }
            )
        }
    }
}

@Composable
private fun DashboardMetricCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accent: Color
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = MaterialTheme.shapes.small,
                color = accent.copy(alpha = 0.13f)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun QuickActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        label = { Text(label) }
    )
}

@Composable
private fun DashboardSectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DebtAgingCard(overdueStores: Int, totalDebt: Double) {
    val warning = overdueStores > 0
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (warning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Column {
                Text("Aging piutang", fontWeight = FontWeight.ExtraBold)
                Text(
                    if (warning) "$overdueStores warung perlu ditagih (>21 hari)"
                    else "Belum ada bon yang melewati 21 hari",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Total bon ${formatRupiah(totalDebt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatRupiah(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }.format(value)
