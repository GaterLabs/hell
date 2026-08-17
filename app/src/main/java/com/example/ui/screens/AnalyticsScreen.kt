package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.StatCard
import com.example.ui.theme.AppThemeColors
import com.example.ui.util.LocalAppLanguage
import com.example.ui.util.LocalAppStrings
import com.example.ui.viewmodel.DateFilterType
import com.example.ui.viewmodel.SalesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: SalesViewModel,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current
    val context = LocalContext.current
    val profitClr = AppThemeColors.profitColor
    val debtClr = AppThemeColors.debtColor
    val warningClr = AppThemeColors.warningColor
    val dateFilter by viewModel.analyticsDateFilter.collectAsState()
    val customDate by viewModel.customAnalyticsDate.collectAsState()
    val summary by viewModel.analyticsSummary.collectAsState()
    val writeOffs by viewModel.debtWriteOffs.collectAsState(initial = emptyList())
    val inventoryBuckets by viewModel.inventoryBucketSummaries.collectAsState()

    val locale = if (language.code == "id") Locale("in", "ID") else Locale.ENGLISH
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDateFormatter = remember(language) { SimpleDateFormat("d MMMM yyyy", locale) }

    val customDateDisplayText = remember(customDate, language) {
        customDate?.let { dateStr ->
            try {
                val parsed = dateFormatter.parse(dateStr)
                if (parsed != null) displayDateFormatter.format(parsed) else dateStr
            } catch (e: Exception) {
                dateStr
            }
        }
    }

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
                            text = strings.analyticsTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.analyticsSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val activePeriodName = when (dateFilter) {
                                DateFilterType.TODAY -> "Hari Ini"
                                DateFilterType.LAST_7_DAYS -> "7 Hari Terakhir"
                                DateFilterType.THIS_WEEK -> "Minggu Ini"
                                DateFilterType.THIS_MONTH -> "Bulan Ini"
                                DateFilterType.ALL_TIME -> "Semua Riwayat"
                                DateFilterType.CUSTOM_DATE -> customDateDisplayText ?: "Tanggal Khusus"
                            }
                            viewModel.generatePdfReport(context, activePeriodName) { pdfFile ->
                                if (pdfFile != null) {
                                    com.example.util.PdfReportGenerator.sharePdfReport(
                                        context,
                                        pdfFile,
                                        "Laporan Analitik SalesTrack ($activePeriodName)"
                                    )
                                } else {
                                    android.widget.Toast.makeText(context, "Gagal membuat dokumen PDF", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF Report",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
        ) {
            // Horizontal Date Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateFilterType.values().forEach { filter ->
                        val isSelected = dateFilter == filter
                        val filterLabel = when (filter) {
                            DateFilterType.TODAY -> strings.filterToday
                            DateFilterType.LAST_7_DAYS -> strings.filter7Days
                            DateFilterType.THIS_WEEK -> strings.filterThisWeek
                            DateFilterType.THIS_MONTH -> strings.filterThisMonth
                            DateFilterType.ALL_TIME -> strings.filterAllTime
                            DateFilterType.CUSTOM_DATE -> strings.filterPickDate
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (filter == DateFilterType.CUSTOM_DATE) {
                                    val now = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val cal = Calendar.getInstance().apply {
                                                set(year, month, dayOfMonth)
                                            }
                                            val selectedKey = dateFormatter.format(cal.time)
                                            viewModel.setCustomAnalyticsDate(selectedKey)
                                        },
                                        now.get(Calendar.YEAR),
                                        now.get(Calendar.MONTH),
                                        now.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                } else {
                                    viewModel.setDateFilter(filter)
                                }
                            },
                            label = {
                                val chipText = if (filter == DateFilterType.CUSTOM_DATE && customDateDisplayText != null && isSelected) {
                                    "📅 $customDateDisplayText"
                                } else {
                                    filterLabel
                                }
                                Text(chipText, style = MaterialTheme.typography.labelSmall)
                            },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            // Period Indicator Banner
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = strings.periodIndicator,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val currentFilterLabel = when (dateFilter) {
                                DateFilterType.TODAY -> strings.filterToday
                                DateFilterType.LAST_7_DAYS -> strings.filter7Days
                                DateFilterType.THIS_WEEK -> strings.filterThisWeek
                                DateFilterType.THIS_MONTH -> strings.filterThisMonth
                                DateFilterType.ALL_TIME -> strings.filterAllTime
                                DateFilterType.CUSTOM_DATE -> strings.filterPickDate
                            }
                            Text(
                                text = if (dateFilter == DateFilterType.CUSTOM_DATE && customDateDisplayText != null) {
                                    customDateDisplayText
                                } else {
                                    currentFilterLabel
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = "${summary.totalTransactions} ${strings.tabRoutes}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Primary Stat Cards 2x2
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = strings.totalRevenueStat,
                        value = SalesViewModel.formatRupiah(summary.totalRevenue),
                        subtitle = "${summary.totalTransactions} ${strings.txPrefix}",
                        icon = Icons.Default.Payments,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = strings.totalProfitStat,
                        value = SalesViewModel.formatRupiah(summary.totalProfit),
                        subtitle = "Margin: ${String.format("%.1f", summary.profitMarginPercent)}%",
                        icon = Icons.Default.TrendingUp,
                        accentColor = profitClr,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        title = "Aset Pribadi Repack",
                        value = "${inventoryBuckets.firstOrNull { it.bucket == "PRIVATE_READY" }?.totalPcs ?: 0} pcs",
                        subtitle = "Stok siap jual",
                        icon = Icons.Default.Diamond,
                        accentColor = AppThemeColors.profitColor,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Kerugian Write-off",
                        value = SalesViewModel.formatRupiah(writeOffs.sumOf { it.amount }),
                        subtitle = "${writeOffs.size} kejadian",
                        icon = Icons.Default.WarningAmber,
                        accentColor = AppThemeColors.debtColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = strings.totalItemsSoldStat,
                        value = "${summary.totalItemsSold} pcs",
                        subtitle = strings.itemsDeliveredDesc,
                        icon = Icons.Default.ShoppingBag,
                        accentColor = warningClr,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = strings.uncollectedDebtStat,
                        value = SalesViewModel.formatRupiah(summary.totalOutstandingDebt),
                        subtitle = strings.debtRemainingDesc,
                        icon = Icons.Default.Receipt,
                        accentColor = debtClr,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Daily Analytics Breakdown per Date (Rekap Analitik Harian per Tanggal)
            if (summary.dailyBreakdown.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
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
                                        Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = strings.dailyBreakdownTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${summary.dailyBreakdown.size} Days",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val maxDailyRev = summary.dailyBreakdown.maxOfOrNull { it.revenue } ?: 1.0

                            summary.dailyBreakdown.forEach { daily ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = daily.displayDate,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${daily.transactionCount} Stores • ${daily.itemsSold} pcs",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = SalesViewModel.formatRupiah(daily.revenue),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "${strings.profitPrefix}: +${SalesViewModel.formatRupiah(daily.profit)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = profitClr
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Progress comparison bar for this day
                                    LinearProgressIndicator(
                                        progress = { if (maxDailyRev > 0) (daily.revenue / maxDailyRev).toFloat().coerceIn(0f, 1f) else 0f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    HorizontalDivider(
                                        modifier = Modifier.padding(top = 8.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Financial Breakdown Card (Omset vs Modal HPP vs Laba)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.financialSummaryTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Visual Proportional Bar
                        if (summary.totalRevenue > 0) {
                            val costFraction = (summary.totalCost / summary.totalRevenue).toFloat().coerceIn(0f, 1f)
                            val profitFraction = (summary.totalProfit / summary.totalRevenue).toFloat().coerceIn(0f, 1f)

                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(costFraction.coerceAtLeast(0.01f))
                                            .fillMaxHeight()
                                            .background(MaterialTheme.colorScheme.outline)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(profitFraction.coerceAtLeast(0.01f))
                                            .fillMaxHeight()
                                            .background(profitClr)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.outline)
                                        )
                                        Text(
                                            text = "${strings.costHppLabel}: ${SalesViewModel.formatRupiah(summary.totalCost)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(profitClr)
                                        )
                                        Text(
                                            text = "${strings.profitPrefix}: ${SalesViewModel.formatRupiah(summary.totalProfit)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = profitClr
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = strings.noTransactionsFound,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Top Selling Products Breakdown
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🏆 ${strings.topSellingTitle}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (summary.topSellingProducts.isEmpty()) {
                            Text(
                                text = strings.noTransactionsFound,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val maxSold = summary.topSellingProducts.maxOfOrNull { it.totalSoldPcs } ?: 1

                            summary.topSellingProducts.forEachIndexed { idx, topItem ->
                                val safePackSize = if (topItem.packSize > 0) topItem.packSize else 1
                                val packs = topItem.totalSoldPcs / safePackSize
                                val pcs = topItem.totalSoldPcs % safePackSize
                                val qtyStr = if (packs > 0 && pcs > 0) {
                                    "$packs ${topItem.unitName} + $pcs pcs"
                                } else if (packs > 0) {
                                    "$packs ${topItem.unitName} (${topItem.totalSoldPcs} pcs)"
                                } else {
                                    "${topItem.totalSoldPcs} pcs"
                                }

                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${idx + 1}. ${topItem.productName}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = qtyStr,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = SalesViewModel.formatRupiah(topItem.totalRevenue),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { (topItem.totalSoldPcs.toFloat() / maxSold).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Route Performance Comparison
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🗺️ ${strings.routePerformanceTitle}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (summary.routePerformances.isEmpty()) {
                            Text(
                                text = strings.noStoresFound,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            summary.routePerformances.forEach { rPerf ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = rPerf.routeName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = SalesViewModel.formatRupiah(rPerf.totalRevenue),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = strings.routeStoresCount(rPerf.storesVisited, rPerf.totalStores),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${strings.profitPrefix}: +${SalesViewModel.formatRupiah(rPerf.totalProfit)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = profitClr
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(top = 6.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
