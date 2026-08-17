package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.AppThemeColors
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.util.AppLanguage
import com.example.ui.util.AppStrings
import com.example.ui.util.AppThemeMode
import com.example.ui.util.LocalAppLanguage
import com.example.ui.util.LocalAppStrings
import com.example.ui.viewmodel.SalesViewModel
import kotlinx.coroutines.launch

enum class NavigationItem(
    val getTitle: (AppStrings) -> String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isBottomNavItem: Boolean = false // Only true for daily essential field operations
) {
    DASHBOARD({ it.navDashboard }, Icons.Filled.Dashboard, Icons.Outlined.Dashboard, isBottomNavItem = true),
    ROUTES({ it.navRoutes }, Icons.Filled.AltRoute, Icons.Outlined.AltRoute, isBottomNavItem = true),
    STORES({ it.navStores }, Icons.Filled.Storefront, Icons.Outlined.Storefront, isBottomNavItem = true),
    INVENTORY({ it.navInventory }, Icons.Filled.Inventory2, Icons.Outlined.Inventory2, isBottomNavItem = true),
    HISTORY({ it.navHistory }, Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, isBottomNavItem = true),
    ANALYTICS({ it.navAnalytics }, Icons.Filled.Insights, Icons.Outlined.Insights, isBottomNavItem = false),
    SETTINGS({ it.navSettings }, Icons.Filled.Settings, Icons.Outlined.Settings, isBottomNavItem = false),
    MASTER_DATA({ it.navMasterData }, Icons.Filled.Business, Icons.Outlined.Business, isBottomNavItem = false)
}

class MainActivity : ComponentActivity() {

    private val viewModel: SalesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val currentLanguage by viewModel.appLanguage.collectAsState()
            val strings by viewModel.appStrings.collectAsState()

            val isSystemDark = isSystemInDarkTheme()
            val effectiveDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            CompositionLocalProvider(
                LocalAppStrings provides strings,
                LocalAppLanguage provides currentLanguage
            ) {
                MyApplicationTheme(darkTheme = effectiveDarkTheme) {
                    MainAppScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: SalesViewModel) {
    val strings = LocalAppStrings.current
    var currentItem by remember { mutableStateOf(NavigationItem.DASHBOARD) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val allStores by viewModel.allStores.collectAsState()
    val allRoutes by viewModel.allRoutes.collectAsState()
    val totalStores = allStores.size
    val totalDebt = allStores.sumOf { it.outstandingDebt }
    val visitedTodayCount = allStores.count { it.isVisitedToday }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight(),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerTonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Drawer Header Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            )
                            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 18.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TwoWheeler,
                                        contentDescription = "Stock Sales Logo",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(10.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Stock Sales",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = strings.appTagline,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Quick Stats Card in Header
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                tonalElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${strings.totalStoresStat}: $totalStores",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${strings.visitedStoresStat}: $visitedTodayCount",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AppThemeColors.successColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    if (totalDebt > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = AppThemeColors.debtColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Bon: Rp %,d".format(totalDebt.toLong()),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = AppThemeColors.debtColor,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // SECTION 1: Operasional Harian
                    Text(
                        text = strings.drawerSectionOperations,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )

                    val operationsItems = listOf(
                        NavigationItem.DASHBOARD,
                        NavigationItem.ROUTES,
                        NavigationItem.STORES,
                        NavigationItem.INVENTORY,
                        NavigationItem.HISTORY,
                        NavigationItem.MASTER_DATA
                    )

                    operationsItems.forEach { item ->
                        val isSelected = currentItem == item
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = null
                                )
                            },
                            label = {
                                Text(
                                    text = item.getTitle(strings),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            badge = {
                                if (item == NavigationItem.STORES && totalStores > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ) {
                                        Text("$totalStores")
                                    }
                                }
                            },
                            selected = isSelected,
                            onClick = {
                                currentItem = item
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // SECTION 2: Laporan & Analitik (Moved from Bottom Nav)
                    Text(
                        text = strings.drawerSectionReports,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )

                    val isAnalyticsSelected = currentItem == NavigationItem.ANALYTICS
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = if (isAnalyticsSelected) NavigationItem.ANALYTICS.selectedIcon else NavigationItem.ANALYTICS.unselectedIcon,
                                contentDescription = null
                            )
                        },
                        label = {
                            Text(
                                text = NavigationItem.ANALYTICS.getTitle(strings),
                                fontWeight = if (isAnalyticsSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = isAnalyticsSelected,
                        onClick = {
                            currentItem = NavigationItem.ANALYTICS
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // SECTION 3: Sistem & Preferensi (Moved from Bottom Nav)
                    Text(
                        text = strings.drawerSectionSystem,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )

                    val isSettingsSelected = currentItem == NavigationItem.SETTINGS
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = if (isSettingsSelected) NavigationItem.SETTINGS.selectedIcon else NavigationItem.SETTINGS.unselectedIcon,
                                contentDescription = null
                            )
                        },
                        label = {
                            Text(
                                text = NavigationItem.SETTINGS.getTitle(strings),
                                fontWeight = if (isSettingsSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = isSettingsSelected,
                        onClick = {
                            currentItem = NavigationItem.SETTINGS
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Drawer Footer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AppThemeColors.successColor)
                            )
                            Text(
                                text = "Offline Database Ready • SQLite Room",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    ) {
        val openDrawerAction: () -> Unit = {
            scope.launch { drawerState.open() }
        }

        // Bottom Navigation Bar - Only displays primary daily field operation menus
        val bottomNavItems = remember {
            NavigationItem.values().filter { it.isBottomNavItem }
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentItem == item
                        val title = item.getTitle(strings)
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentItem = item },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = title
                                )
                            },
                            label = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            val screenModifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())

            when (currentItem) {
                NavigationItem.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onOpenDrawer = openDrawerAction,
                    onNavigate = { currentItem = it },
                    modifier = screenModifier
                )
                NavigationItem.ROUTES -> RoutesScreen(
                    viewModel = viewModel,
                    onOpenDrawer = openDrawerAction,
                    modifier = screenModifier
                )
                NavigationItem.STORES -> StoresDirectoryScreen(
                    viewModel = viewModel,
                    onOpenDrawer = openDrawerAction,
                    modifier = screenModifier
                )
                NavigationItem.INVENTORY -> InventoryCargoScreen(
                    viewModel = viewModel,
                    onOpenDrawer = openDrawerAction,
                    modifier = screenModifier
                )
                NavigationItem.HISTORY -> HistoryScreen(
                    viewModel = viewModel,
                    onOpenDrawer = openDrawerAction,
                    modifier = screenModifier
                )
                NavigationItem.ANALYTICS -> AnalyticsScreen(
                    viewModel = viewModel,
                    onOpenDrawer = openDrawerAction,
                    modifier = screenModifier
                )
                NavigationItem.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    onOpenDrawer = openDrawerAction,
                    modifier = screenModifier
                )
                NavigationItem.MASTER_DATA -> MasterDataScreen(
                    viewModel = viewModel,
                    onOpenDrawer = openDrawerAction,
                    modifier = screenModifier
                )
            }
        }
    }
}
