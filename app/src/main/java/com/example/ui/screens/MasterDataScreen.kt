package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessPartnerEntity
import com.example.data.model.ProductEntity
import com.example.data.model.StoreEntity
import com.example.data.model.StorePriceOverrideEntity
import com.example.ui.util.LocalAppStrings
import com.example.ui.viewmodel.SalesViewModel
import java.text.SimpleDateFormat
import java.util.*

private enum class MasterTab(val kind: String?) {
    SUPPLIER("SUPPLIER"), SALESMAN("SALESMAN"), BANK("BANK"), PRICE(null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterDataScreen(
    viewModel: SalesViewModel,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val partners by viewModel.businessPartners.collectAsState(initial = emptyList())
    val overrides by viewModel.priceOverrides.collectAsState(initial = emptyList())
    val stores by viewModel.allStores.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    var tab by remember { mutableStateOf(MasterTab.SUPPLIER) }
    var showPartnerDialog by remember { mutableStateOf(false) }
    var showOverrideDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, null) } },
                title = { Text(strings.navMasterData, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { if (tab == MasterTab.PRICE) showOverrideDialog = true else showPartnerDialog = true }) {
                        Icon(Icons.Default.Add, "Tambah")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    MasterTab.SUPPLIER to "Supplier",
                    MasterTab.SALESMAN to "Salesman",
                    MasterTab.BANK to "Bank/Rekening",
                    MasterTab.PRICE to "Harga Khusus"
                ).forEach { (value, label) ->
                    FilterChip(selected = tab == value, onClick = { tab = value }, label = { Text(label) })
                }
            }
            if (tab == MasterTab.PRICE) {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(overrides, key = { "${it.storeId}-${it.productId}" }) { item ->
                        val store = stores.find { it.id == item.storeId }
                        val product = products.find { it.id == item.productId }
                        ListItem(
                            headlineContent = { Text("${store?.name ?: "Warung"} • ${product?.name ?: "Produk"}", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Rp ${item.pricePerPc.toLong()} / pcs • berlaku ${item.validFrom}") },
                            trailingContent = { IconButton(onClick = { viewModel.deletePriceOverride(item.storeId, item.productId) }) { Icon(Icons.Default.DeleteOutline, "Hapus") } }
                        )
                        HorizontalDivider()
                    }
                }
            } else {
                val list = partners.filter { it.kind == tab.kind }
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(list, key = { it.id }) { partner ->
                        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            ListItem(
                                headlineContent = { Text(partner.name, fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text(listOf(partner.contactName, partner.phone, partner.address, partner.bankAccount).filter { it.isNotBlank() }.joinToString(" • ")) },
                                trailingContent = { IconButton(onClick = { viewModel.deleteBusinessPartner(partner) }) { Icon(Icons.Default.DeleteOutline, "Hapus") } }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPartnerDialog) {
        PartnerDialog(kind = tab.kind!!, onDismiss = { showPartnerDialog = false }) { entity ->
            viewModel.saveBusinessPartner(entity) { showPartnerDialog = false }
        }
    }
    if (showOverrideDialog) {
        OverrideDialog(stores, products, onDismiss = { showOverrideDialog = false }) { entity ->
            viewModel.savePriceOverride(entity) { showOverrideDialog = false }
        }
    }
}

@Composable
private fun PartnerDialog(kind: String, onDismiss: () -> Unit, onSave: (BusinessPartnerEntity) -> Unit) {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (kind == "SUPPLIER") "Tambah Supplier" else if (kind == "SALESMAN") "Tambah Salesman" else "Tambah Rekening") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nama") }, singleLine = true)
            OutlinedTextField(contact, { contact = it }, label = { Text(if (kind == "BANK") "Atas Nama / CP" else "Kontak") }, singleLine = true)
            OutlinedTextField(phone, { phone = it }, label = { Text("No. HP") }, singleLine = true)
            OutlinedTextField(address, { address = it }, label = { Text("Alamat") }, singleLine = true)
            if (kind == "BANK") OutlinedTextField(bank, { bank = it }, label = { Text("Nomor Rekening") }, singleLine = true)
        } },
        confirmButton = { Button(onClick = { if (name.isNotBlank()) onSave(BusinessPartnerEntity(kind = kind, name = name.trim(), contactName = contact.trim(), phone = phone.trim(), address = address.trim(), bankAccount = bank.trim())) }, enabled = name.isNotBlank()) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverrideDialog(stores: List<StoreEntity>, products: List<ProductEntity>, onDismiss: () -> Unit, onSave: (StorePriceOverrideEntity) -> Unit) {
    var storeId by remember { mutableStateOf(stores.firstOrNull()?.id ?: 0L) }
    var productId by remember { mutableStateOf(products.firstOrNull()?.id ?: 0L) }
    var price by remember { mutableStateOf("") }
    var expandedStore by remember { mutableStateOf(false) }
    var expandedProduct by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Harga Khusus Warung") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(expandedStore, { expandedStore = !expandedStore }) {
                OutlinedTextField(value = stores.find { it.id == storeId }?.name ?: "Pilih warung", onValueChange = {}, readOnly = true, label = { Text("Warung") }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expandedStore, { expandedStore = false }) { stores.forEach { DropdownMenuItem(text = { Text(it.name) }, onClick = { storeId = it.id; expandedStore = false }) } }
            }
            ExposedDropdownMenuBox(expandedProduct, { expandedProduct = !expandedProduct }) {
                OutlinedTextField(value = products.find { it.id == productId }?.name ?: "Pilih produk", onValueChange = {}, readOnly = true, label = { Text("Produk") }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expandedProduct, { expandedProduct = false }) { products.forEach { DropdownMenuItem(text = { Text(it.name) }, onClick = { productId = it.id; expandedProduct = false }) } }
            }
            OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("Harga per pcs") }, singleLine = true)
        }
    }, confirmButton = { Button(onClick = { onSave(StorePriceOverrideEntity(storeId, productId, price.toDoubleOrNull() ?: 0.0, SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))) }, enabled = storeId > 0 && productId > 0 && (price.toDoubleOrNull() ?: 0.0) > 0) { Text("Simpan") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })
}
