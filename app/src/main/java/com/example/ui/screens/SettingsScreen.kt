package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ui.theme.AppThemeColors
import com.example.ui.theme.SuccessGreen
import com.example.ui.util.AppLanguage
import com.example.ui.util.AppThemeMode
import com.example.ui.util.LocalAppLanguage
import com.example.ui.util.LocalAppStrings
import com.example.ui.viewmodel.SalesViewModel
import com.example.util.BackupRestoreUtil
import com.example.util.PdfReportGenerator
import com.example.util.RestoreResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SalesViewModel,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val currentLang = LocalAppLanguage.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val themeMode by viewModel.themeMode.collectAsState()
    val pinEnabled by viewModel.pinEnabled.collectAsState()
    val gpsThreshold by viewModel.gpsAccuracyThreshold.collectAsState()
    val printerAddress by viewModel.printerAddress.collectAsState()
    val auditEvents by viewModel.recentAuditEvents.collectAsState(initial = emptyList())

    var showResetVisitsDialog by remember { mutableStateOf(false) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var selectedImportUri by remember { mutableStateOf<Uri?>(null) }
    var restoreResultMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }
    var gpsText by remember(gpsThreshold) { mutableStateOf(gpsThreshold.toString()) }
    var printerText by remember(printerAddress) { mutableStateOf(printerAddress) }

    // Launcher for selecting a JSON file to import
    val importFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImportUri = uri
            showImportConfirmDialog = true
        }
    }

    // Launcher for saving JSON backup to user-selected destination
    val exportFileSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            viewModel.exportBackupToUri(context, uri) { success ->
                isProcessing = false
                if (success) {
                    Toast.makeText(context, strings.backupExportSuccess, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Gagal menyimpan file backup", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Launcher for saving PDF report to user-selected destination
    val pdfFileSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            viewModel.writePdfReportToUri(context, uri, "Semua Periode Transaksi") { success ->
                isProcessing = false
                if (success) {
                    Toast.makeText(context, strings.pdfExportSuccess, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Gagal menyimpan file PDF", Toast.LENGTH_SHORT).show()
                }
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
                    Text(
                        text = strings.settingsTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
            // Offline Mode Badge Card
            item {
                Surface(
                    color = SuccessGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = SuccessGreen
                            )
                        }
                        Column {
                            Text(
                                text = strings.offlineBadgeTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                            Text(
                                text = strings.offlineBadgeDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Backup & Restore Card (Transfer to New Phone)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.SyncAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = strings.backupSectionTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = strings.backupSectionSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        // Action 1: Share / Export Backup (JSON)
                        Button(
                            onClick = {
                                isProcessing = true
                                viewModel.exportBackupFile(context) { file ->
                                    isProcessing = false
                                    if (file != null) {
                                        BackupRestoreUtil.shareBackupFile(context, file)
                                    } else {
                                        Toast.makeText(context, "Gagal membuat file cadangan", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.btnExportBackup)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Action 2: Save Backup File to Storage
                        OutlinedButton(
                            onClick = {
                                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                val fileName = "salestrack_backup_${sdf.format(Date())}.json"
                                exportFileSaveLauncher.launch(fileName)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simpan File Backup ke HP")
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Action 3: Restore / Import Data
                        OutlinedButton(
                            onClick = {
                                importFilePickerLauncher.launch("*/*")
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.tertiary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.btnImportBackup)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.importBackupDesc,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // PDF Report Export Card
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = strings.btnExportPdfReport,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = strings.exportPdfReportDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        // Share PDF via WA / Email
                        Button(
                            onClick = {
                                isProcessing = true
                                viewModel.generatePdfReport(context, "Semua Riwayat Penjualan") { pdfFile ->
                                    isProcessing = false
                                    if (pdfFile != null) {
                                        PdfReportGenerator.sharePdfReport(context, pdfFile, strings.pdfShareSubject)
                                    } else {
                                        Toast.makeText(context, "Gagal membuat dokumen PDF", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bagikan PDF via WhatsApp / Drive")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Save PDF to Downloads
                        OutlinedButton(
                            onClick = {
                                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                val fileName = "Laporan_SalesTrack_${sdf.format(Date())}.pdf"
                                pdfFileSaveLauncher.launch(fileName)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simpan File PDF ke Memori")
                        }
                    }
                }
            }

            // Language Selector Card (English vs Bahasa Indonesia)
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = strings.languageSettingTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = strings.languageSettingSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        AppLanguage.values().forEach { lang ->
                            val isSelected = currentLang == lang
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,
                                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { viewModel.setLanguage(lang) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(text = lang.flag, fontSize = 20.sp)
                                        Column {
                                            Text(
                                                text = lang.displayName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                            Text(
                                                text = if (lang == AppLanguage.ENGLISH) "Default language" else "Bahasa Indonesia",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.setLanguage(lang) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Theme Mode Selector Card (System Default, Light, Dark)
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = strings.themeSettingTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = strings.themeSettingSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        val themeOptions = listOf(
                            Triple(AppThemeMode.SYSTEM, strings.themeSystem, strings.themeSystemDesc),
                            Triple(AppThemeMode.LIGHT, strings.themeLight, strings.themeLightDesc),
                            Triple(AppThemeMode.DARK, strings.themeDark, strings.themeDarkDesc)
                        )

                        themeOptions.forEach { (mode, title, desc) ->
                            val isSelected = themeMode == mode
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,
                                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { viewModel.setThemeMode(mode) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = when (mode) {
                                                AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                                AppThemeMode.LIGHT -> Icons.Default.LightMode
                                                AppThemeMode.DARK -> Icons.Default.DarkMode
                                            },
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Column {
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                            Text(
                                                text = desc,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.setThemeMode(mode) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Route Daily Operations
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
                            text = strings.dailyOpsTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showResetVisitsDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.btnResetDailyVisits)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.resetDailyVisitsDesc,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Formula Info Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.formulaTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${strings.formula1}\n${strings.formula2}\n${strings.formula3}\n${strings.formula4}",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Version info
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Keamanan & Perangkat", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(if (pinEnabled) "PIN aplikasi aktif" else "PIN aplikasi belum aktif", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = pinText,
                            onValueChange = { pinText = it.filter(Char::isDigit).take(6) },
                            label = { Text("PIN baru (4-6 digit)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.setSecurityPin(pinText); pinText = "" }, enabled = pinText.length >= 4) { Text(if (pinEnabled) "Ubah PIN" else "Aktifkan PIN") }
                            if (pinEnabled) OutlinedButton(onClick = { viewModel.setSecurityPin("") }) { Text("Nonaktifkan") }
                        }
                        OutlinedTextField(
                            value = gpsText,
                            onValueChange = { gpsText = it.filter(Char::isDigit).take(3) },
                            label = { Text("Batas akurasi GPS (meter)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(onClick = { viewModel.setGpsAccuracyThreshold(gpsText.toIntOrNull() ?: gpsThreshold) }) { Text("Simpan batas GPS") }
                        OutlinedTextField(
                            value = printerText,
                            onValueChange = { printerText = it },
                            label = { Text("Alamat MAC printer Bluetooth") },
                            placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(onClick = { viewModel.setPrinterAddress(printerText) }) { Text("Simpan printer") }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Audit Trail Terbaru", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (auditEvents.isEmpty()) {
                            Text("Belum ada aktivitas tercatat", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            auditEvents.take(8).forEach { event ->
                                Text("${event.eventType}: ${event.description}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // Version info
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strings.appVersionTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = strings.appVersionDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Reset Visits Dialog
    if (showResetVisitsDialog) {
        AlertDialog(
            onDismissRequest = { showResetVisitsDialog = false },
            modifier = Modifier.fillMaxWidth(0.92f),
            title = { Text(strings.resetVisitsDialogTitle) },
            text = {
                Text(strings.resetVisitsDialogDesc)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetDailyVisits()
                        showResetVisitsDialog = false
                    }
                ) {
                    Text(strings.btnConfirmReset)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetVisitsDialog = false }) {
                    Text(strings.btnCancel)
                }
            }
        )
    }

    // Confirm Import Dialog
    if (showImportConfirmDialog && selectedImportUri != null) {
        AlertDialog(
            onDismissRequest = {
                showImportConfirmDialog = false
                selectedImportUri = null
            },
            modifier = Modifier.fillMaxWidth(0.92f),
            title = { Text(strings.importBackupDialogTitle) },
            text = {
                Text(strings.importBackupDialogDesc)
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = selectedImportUri
                        showImportConfirmDialog = false
                        selectedImportUri = null
                        if (uri != null) {
                            isProcessing = true
                            viewModel.importBackupFromUri(context, uri) { result ->
                                isProcessing = false
                                if (result.success) {
                                    restoreResultMessage = strings.backupImportSuccess(
                                        result.routesCount,
                                        result.storesCount,
                                        result.productsCount,
                                        result.transactionsCount
                                    )
                                } else {
                                    restoreResultMessage = result.message
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Text(strings.btnConfirmImport)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportConfirmDialog = false
                    selectedImportUri = null
                }) {
                    Text(strings.btnCancel)
                }
            }
        )
    }

    // Restore Result Dialog
    if (restoreResultMessage != null) {
        AlertDialog(
            onDismissRequest = { restoreResultMessage = null },
            modifier = Modifier.fillMaxWidth(0.92f),
            title = { Text("Informasi Pemulihan Data") },
            text = {
                Text(restoreResultMessage ?: "")
            },
            confirmButton = {
                Button(onClick = { restoreResultMessage = null }) {
                    Text(strings.btnClose)
                }
            }
        )
    }
}
