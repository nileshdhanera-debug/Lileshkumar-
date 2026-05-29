package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BuildResult
import com.example.data.FurnitureRecipe
import com.example.data.InventoryItem
import com.example.data.RecipeIngredient
import com.example.data.TransactionLog
import com.example.data.SiteMeasurement
import com.example.data.CustomerKhata
import com.example.data.KhataEntry
import com.example.data.Supplier
import com.example.data.SupplierMaterial
import com.example.data.SupplierReceipt
import com.example.data.ReceiptItem
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import android.content.Intent
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryAppScreen(viewModel: InventoryViewModel) {
    val items by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val recipes by viewModel.recipes.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    val selectedRecipeForBuild by viewModel.selectedRecipe.collectAsStateWithLifecycle()
    val adjustingIngredients by viewModel.adjustingIngredients.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Stock, 1: Build Furniture, 2: Logs
    var searchQuery by remember { mutableStateOf("") }
    var selectedStockFilter by remember { mutableStateOf("ALL") } // ALL, SHEET, HARDWARE

    // Dialog sheets states
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showRestockDialog by remember { mutableStateOf<InventoryItem?>(null) }
    var showEditItemDialog by remember { mutableStateOf<InventoryItem?>(null) }
    var showAddRecipeDialog by remember { mutableStateOf(false) }
    var showCustomBuildDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    // Alert status results
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var buildStatusMessage by remember { mutableStateOf<String?>(null) }
    var buildStatusErrors by remember { mutableStateOf<List<String>>(emptyList()) }
    var showBuildResultDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.buildResultEvent) {
        viewModel.buildResultEvent.collectLatest { result ->
            when (result) {
                is BuildResult.Success -> {
                    buildStatusMessage = "સફળતાપૂર્વક પણે બની ગયું! સ્ટોકમાંથી આપોઆપ કાપી લીધું છે. ✅\n(Furniture built successfully! Inventory deducted automatically.)"
                    buildStatusErrors = emptyList()
                    showBuildResultDialog = true
                }
                is BuildResult.Error -> {
                    buildStatusMessage = "સ્ટોક ઓછો છે! કાપી શકાય તેમ નથી. ❌\n(Insufficient inventory in stock!)"
                    buildStatusErrors = result.deficientItems
                    showBuildResultDialog = true
                }
            }
        }
    }

    // Calculations for the Top Banner
    val totalSheets = items.filter { it.type == "SHEET" }.sumOf { it.quantity }
    val totalHardware = items.filter { it.type == "HARDWARE" }.sumOf { it.quantity }
    val lowStockCount = items.count { it.quantity <= it.minThreshold }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = GeoBackground,
                modifier = Modifier.statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "INVENTORY SYSTEM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = GeoTextMuted
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Chamunda uPVC Furniture",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = GeoTextDark,
                                fontSize = 18.sp
                            )
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Theme Palette / Settings Trigger Button
                        IconButton(
                            onClick = { showThemeDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GeoPrimaryAction.copy(alpha = 0.12f)),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "થીમ સેટિંગ્સ (Theme Settings)",
                                tint = GeoPrimaryAction,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Quick low-stock alert pill in header
                        if (lowStockCount > 0) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFFECE0), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$lowStockCount Low ⚠️",
                                    color = DangerRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Gmail Backup
                        val backupStocks by viewModel.inventoryItems.collectAsStateWithLifecycle()
                        val backupMeasurements by viewModel.siteMeasurements.collectAsStateWithLifecycle()
                        val backupCustomers by viewModel.customers.collectAsStateWithLifecycle()
                        val context = androidx.compose.ui.platform.LocalContext.current

                        IconButton(
                            onClick = {
                                val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                                val sb = StringBuilder()
                                sb.append("=========================================\n")
                                sb.append("⚡ uPVC CUPBOARD SYSTEM - FULL DATABASE BACKUP ⚡\n")
                                sb.append("તારીખ / Date: $dateStr\n")
                                sb.append("=========================================\n\n")
                                
                                sb.append("📦 1. ચાલુ સ્ટોક વિગત (CURRENT STOCK INVENTORY)\n")
                                sb.append("-------------------------------------------------\n")
                                if (backupStocks.isEmpty()) {
                                    sb.append("(સ્ટોકમાં કોઈ માલ નથી / Empty Inventory)\n")
                                } else {
                                    backupStocks.forEachIndexed { i, item ->
                                        sb.append("${i + 1}. ${item.name} (${item.specification})\n")
                                        sb.append("   - જથ્થો (Qty): ${item.quantity} ${item.unit} [લઘુત્તમ લિમિટ: ${item.minThreshold}]\n")
                                    }
                                }
                                sb.append("\n")
                                
                                sb.append("📋 2. સાઇટ માપણી લિસ્ટ (SITE AREA MEASUREMENTS)\n")
                                sb.append("-------------------------------------------------\n")
                                if (backupMeasurements.isEmpty()) {
                                    sb.append("(કોઈ સાઇટ માપ સેવ કરેલા નથી / No Measurements)\n")
                                } else {
                                    backupMeasurements.forEachIndexed { i, m ->
                                        val unitStr = if (m.isFeet) "Feet" else "Inches"
                                        val doubleStr = if (m.isDouble) " [ડબલ માપ / Double x2]" else ""
                                        sb.append("${i + 1}. ${m.title}\n")
                                        sb.append("   - માપ: ${m.height} x ${m.width} $unitStr$doubleStr\n")
                                        sb.append("   - કુલ એરિયા: ${"%.2f".format(m.sqFt)} SqFt\n")
                                    }
                                }
                                sb.append("\n")
                                
                                sb.append("👥 3. ગ્રાહક રજીસ્ટર (REGISTERED CLIENT LIST)\n")
                                sb.append("-------------------------------------------------\n")
                                if (backupCustomers.isEmpty()) {
                                    sb.append("(કોઈ ગ્રાહક નોંધાયેલ નથી / No Customers)\n")
                                } else {
                                    backupCustomers.forEachIndexed { i, c ->
                                        sb.append("${i + 1}. ${c.name} [📞 ${c.phone.ifBlank { "No Phone" }}]\n")
                                    }
                                }
                                sb.append("\n=========================================\n")
                                sb.append("બેકઅપ બાય: uPVC Stock Tracker & Accounts Manager App 🚀\n")
                                sb.append("=========================================\n")

                                val backupText = sb.toString()
                                val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:")
                                    putExtra(Intent.EXTRA_SUBJECT, "uPVC Stock and Khata Backup - $dateStr")
                                    putExtra(Intent.EXTRA_TEXT, backupText)
                                }
                                try {
                                    context.startActivity(Intent.createChooser(mailIntent, "Gmail / Email મોકલો"))
                                } catch (e: Exception) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, backupText)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "બેકઅપ શેર કરો"))
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GeoPrimaryAction.copy(alpha = 0.1f)),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Gmail Backup",
                                tint = GeoPrimaryAction,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = GeoCardBg,
                border = BorderStroke(1.dp, GeoNavBorder),
                modifier = Modifier.navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val selectedTab0 = activeTab == 0
                    Column(
                        modifier = Modifier
                            .testTag("nav_tab_stock")
                            .clickable { activeTab = 0 }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Stock",
                            tint = if (selectedTab0) GeoPrimaryAction else GeoTextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "સ્ટોક (Stock)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab0) GeoPrimaryAction else GeoTextMuted
                        )
                    }

                    val selectedTab1 = activeTab == 1
                    Column(
                        modifier = Modifier
                            .testTag("nav_tab_build")
                            .clickable { activeTab = 1 }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Build",
                            tint = if (selectedTab1) GeoPrimaryAction else GeoTextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "બનાવો (Build)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab1) GeoPrimaryAction else GeoTextMuted
                        )
                    }

                    val selectedTab2 = activeTab == 2
                    Column(
                        modifier = Modifier
                            .testTag("nav_tab_measurements")
                            .clickable { activeTab = 2 }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Measurements",
                            tint = if (selectedTab2) GeoPrimaryAction else GeoTextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "માપણી & SqFt",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab2) GeoPrimaryAction else GeoTextMuted
                        )
                    }

                    val selectedTab3 = activeTab == 3
                    Column(
                        modifier = Modifier
                            .testTag("nav_tab_khata")
                            .clickable { activeTab = 3 }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Khata",
                            tint = if (selectedTab3) GeoPrimaryAction else GeoTextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "ગ્રાહક ખાતા",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab3) GeoPrimaryAction else GeoTextMuted
                        )
                    }

                    val selectedTab4 = activeTab == 4
                    Column(
                        modifier = Modifier
                            .testTag("nav_tab_logs")
                            .clickable { activeTab = 4 }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "History",
                            tint = if (selectedTab4) GeoPrimaryAction else GeoTextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "ઇતિહાસ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab4) GeoPrimaryAction else GeoTextMuted
                        )
                    }

                    val selectedTab5 = activeTab == 5
                    Column(
                        modifier = Modifier
                            .testTag("nav_tab_supplier")
                            .clickable { activeTab = 5 }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Supplier",
                            tint = if (selectedTab5) GeoPrimaryAction else GeoTextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "સપ્લાયર (Supplier)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab5) GeoPrimaryAction else GeoTextMuted
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GeoBackground)
                .padding(innerPadding)
        ) {
            // Summary Dashboard Area
            DashboardSummaryCard(
                totalSheets = totalSheets,
                totalHardware = totalHardware,
                lowStockCount = lowStockCount
            )

            // Dynamic Tab Views
            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    0 -> StockTabContent(
                        items = items,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        selectedFilter = selectedStockFilter,
                        onFilterChange = { selectedStockFilter = it },
                        onAddItemClick = { showAddItemDialog = true },
                        onRestockClick = { showRestockDialog = it },
                        onEditClick = { showEditItemDialog = it },
                        onDeleteClick = { viewModel.removeItem(it.id) }
                    )
                    1 -> BuildTabContent(
                        recipes = recipes,
                        items = items,
                        selectedRecipe = selectedRecipeForBuild,
                        adjustingIngredients = adjustingIngredients,
                        onSelectRecipe = { viewModel.selectRecipeForBuild(it) },
                        onIngredientQtyChange = { itemId, qty -> viewModel.updateIngredientQuantity(itemId, qty) },
                        onBuildClick = { name, deduct -> viewModel.buildFurnitureAndDeduct(name, deduct) },
                        onCancelBuild = { viewModel.clearBuildSelection() },
                        onAddRecipeTemplateClick = { showAddRecipeDialog = true },
                        onAddCustomBuildClick = { showCustomBuildDialog = true },
                        onDeleteRecipe = { viewModel.removeRecipe(it) }
                    )
                    2 -> MeasurementsTabContent(viewModel)
                    3 -> CustomerKhataTabContent(viewModel)
                    4 -> LogsTabContent(
                        logs = logs,
                        items = items,
                        recipes = recipes,
                        onClearClick = { viewModel.clearHistoryLogs() }
                    )
                    5 -> SupplierTabContent(viewModel)
                }
            }
        }
    }

    // DIALOGS & OVERLAYS

    // 1. Add Stock Item Dialog
    if (showAddItemDialog) {
        AddItemDialog(
            onDismiss = { showAddItemDialog = false },
            onConfirm = { name, type, spec, qty, unit, minT ->
                viewModel.addNewItem(name, type, spec, qty, unit, minT)
                showAddItemDialog = false
            }
        )
    }

    // 2. Quick Restock Dialog
    if (showRestockDialog != null) {
        RestockDialog(
            item = showRestockDialog!!,
            onDismiss = { showRestockDialog = null },
            onConfirm = { itemId, addQty ->
                viewModel.addStockQuantity(itemId, addQty)
                showRestockDialog = null
            }
        )
    }

    // 3. Edit Item Dialog
    if (showEditItemDialog != null) {
        EditItemDialog(
            item = showEditItemDialog!!,
            onDismiss = { showEditItemDialog = null },
            onConfirm = { updatedItem ->
                viewModel.updateItemDirectly(updatedItem)
                showEditItemDialog = null
            }
        )
    }

    // 4. Create Custom Recipe Template Dialog
    if (showAddRecipeDialog) {
        AddRecipeDialog(
            availableItems = items,
            onDismiss = { showAddRecipeDialog = false },
            onConfirm = { name, desc, ingredients ->
                viewModel.createRecipeTemplate(name, desc, ingredients)
                showAddRecipeDialog = false
            }
        )
    }

    // 5. Custom on-the-spot build dialog
    if (showCustomBuildDialog) {
        CustomBuildDialog(
            availableItems = items,
            onDismiss = { showCustomBuildDialog = false },
            onConfirm = { title, list, deduct ->
                viewModel.buildCustomFurnitureDirect(title, list, deduct)
                showCustomBuildDialog = false
            }
        )
    }

    // 6. Build Result Success/Fail Dialog
    if (showBuildResultDialog) {
        AlertDialog(
            onDismissRequest = { showBuildResultDialog = false },
            confirmButton = {
                Button(
                    onClick = { showBuildResultDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryAction)
                ) {
                    Text("OK (સૂચના બંધ કરો)", color = Color.White)
                }
            },
            title = {
                Text(
                    text = if (buildStatusErrors.isEmpty()) "યશસ્વી બન્યું! (Success!)" else "ચેતવણી: અટકાવેલ છે (Stock Insufficient!)",
                    fontWeight = FontWeight.Bold,
                    color = if (buildStatusErrors.isEmpty()) SuccessGreen else DangerRed
                )
            },
            text = {
                Column {
                    Text(text = buildStatusMessage ?: "", fontSize = 15.sp, color = GeoTextPrimary)
                    if (buildStatusErrors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "આ વસ્તુઓ સ્ટોકમાં ઓછી છે (Low Materials):", 
                            fontWeight = FontWeight.SemiBold, 
                            color = DangerRed,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        buildStatusErrors.forEach { err ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = DangerRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = err,
                                    fontSize = 12.sp,
                                    color = GeoTextPrimary
                                )
                            }
                        }
                    }
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showThemeDialog) {
        ThemeSettingsDialog(
            onDismiss = { showThemeDialog = false }
        )
    }
}

@Composable
fun ThemeSettingsDialog(onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedColor by remember { mutableStateOf(AppThemeManager.currentThemeCode) }
    var isDark by remember { mutableStateOf(AppThemeManager.isDarkTheme) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    com.example.ui.theme.persistThemeSettings(
                        context,
                        selectedColor,
                        if (isDark) "dark" else "light"
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryAction)
            ) {
                Text("સેવ કરો (Save)", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("બંધ કરો (Cancel)", color = GeoTextMuted)
            }
        },
        title = {
            Text(
                text = "થીમ અને લોગો સેટિંગ્સ",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = GeoTextDark
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Brand Logo Card (Put the logo inside app theme option!)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GeoPrimaryAction.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GeoPrimaryAction.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Drawing the Stylized Logo representation (Custom Vector Cabinet)
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GeoPrimaryAction),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "CF",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "UPVC",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Column {
                            Text(
                                text = "CHAMUNDA",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = GeoPrimaryAction,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "uPVC FURNITURE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GeoTextDark
                            )
                            Text(
                                text = "Mo: 7600484742",
                                fontSize = 9.sp,
                                color = GeoTextMuted
                            )
                        }
                    }
                }

                // 1. Theme Color Picker Container
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "થીમનો મુખ્ય કલર (Theme Color):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GeoTextDark
                    )
                    
                    val colorsList = listOf(
                        Triple("clay", Color(0xFF6F5B40), "મૂળ કલર"),
                        Triple("blue", Color(0xFF1565C0), "રોયલ બ્લુ"),
                        Triple("green", Color(0xFF2E7D32), "ગ્રીન"),
                        Triple("teal", Color(0xFF00796B), "ટીલ"),
                        Triple("amber", Color(0xFFE65100), "નારંગી")
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        colorsList.forEach { (code, col, label) ->
                            val isSelected = selectedColor == code
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) GeoTextDark else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedColor = code
                                        AppThemeManager.currentThemeCode = code
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Light vs Dark Mode Toggle Switch
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ડાર્ક / લાઇટ મોડ (Appearance):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GeoTextDark
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Light Mode Button Chip
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isDark) GeoPrimaryAction else GeoPrimaryAction.copy(alpha = 0.12f))
                                .border(1.dp, GeoBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    isDark = false
                                    AppThemeManager.isDarkTheme = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "લાઇટ મોડ (Light)",
                                color = if (!isDark) Color.White else GeoPrimaryAction,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        // Dark Mode Button Chip
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) GeoPrimaryAction else GeoPrimaryAction.copy(alpha = 0.12f))
                                .border(1.dp, GeoBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    isDark = true
                                    AppThemeManager.isDarkTheme = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ડાર્ક મોડ (Dark)",
                                color = if (isDark) Color.White else GeoPrimaryAction,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = GeoCardBg
    )
}

// ==========================================
// COMPOSABLE COMPONENTS
// ==========================================

@Composable
fun DashboardSummaryCard(
    totalSheets: Double,
    totalHardware: Double,
    lowStockCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Sheets
        Card(
            modifier = Modifier.weight(1f).height(128.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7EEE3)),
            border = BorderStroke(1.dp, GeoBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon Header
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(GeoBadgeBg, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = GeoBadgeText,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                // Content
                Column {
                    Text(
                        text = "%.1f".format(totalSheets),
                        color = GeoTextDark,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "uPVC Sheets",
                        color = GeoTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Card 2: Hardware
        Card(
            modifier = Modifier.weight(1f).height(128.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7EEE3)),
            border = BorderStroke(1.dp, GeoBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon Header
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(GeoBadgeBg, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = GeoBadgeText,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Content
                Column {
                    Text(
                        text = "%.0f".format(totalHardware),
                        color = GeoTextDark,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Hardware Pcs",
                        color = GeoTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Card 3: Status / Low Stock
        val hasLowStock = lowStockCount > 0
        Card(
            modifier = Modifier.weight(1f).height(128.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (hasLowStock) Color(0xFFFFECE0) else Color(0xFFE8F5E9)
            ),
            border = BorderStroke(1.dp, if (hasLowStock) Color(0xFFFFCC80) else Color(0xFFA5D6A7))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon Header
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (hasLowStock) Color(0xFFFFB74D) else Color(0xFF81C784),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hasLowStock) Icons.Default.Warning else Icons.Default.Check,
                        contentDescription = null,
                        tint = if (hasLowStock) GeoTextDark else Color(0xFF1B5E20),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Content
                Column {
                    Text(
                        text = "$lowStockCount",
                        color = if (hasLowStock) DangerRed else SuccessGreen,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (hasLowStock) "Low Stock Alerts" else "All In Stock",
                        color = if (hasLowStock) DangerRed else SuccessGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ==========================================
// 1. STOCK TAB CONTENT
// ==========================================

@Composable
fun StockTabContent(
    items: List<InventoryItem>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    onAddItemClick: () -> Unit,
    onRestockClick: (InventoryItem) -> Unit,
    onEditClick: (InventoryItem) -> Unit,
    onDeleteClick: (InventoryItem) -> Unit
) {
    val filteredItems = items.filter { item ->
        val matchesSearch = item.name.contains(searchQuery, ignoreCase = true) ||
                item.specification.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "SHEET" -> item.type == "SHEET"
            "HARDWARE" -> item.type == "HARDWARE"
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddItemClick,
                containerColor = GeoPrimaryAction,
                contentColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.testTag("add_item_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Stock")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("નવો માલ (+ Add)", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 14.dp)
        ) {
            // Search Bar & Filters
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_stock_input"),
                placeholder = { Text("માલ શોધો (Search stock item...)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GeoPrimaryAction,
                    unfocusedBorderColor = GeoBorder,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Pills Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("ALL" to "દિખાવો (All)", "SHEET" to "uPVC Sheets 🪵", "HARDWARE" to "હાર્ડવેર (Hardware) ⚙️")
                filters.forEach { (type, label) ->
                    val isSelected = selectedFilter == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (isSelected) GeoPrimaryAction else Color.White)
                            .border(1.dp, if (isSelected) GeoPrimaryAction else GeoBorder, RoundedCornerShape(24.dp))
                            .clickable { onFilterChange(type) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else GeoTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "આવો કોઈ હજી સ્ટોક નથી ભરેલો!\n(No stock items found)",
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp) // Leave safety padding for FAB
                ) {
                    items(filteredItems) { item ->
                        StockItemCard(
                            item = item,
                            onRestockClick = { onRestockClick(item) },
                            onEditClick = { onEditClick(item) },
                            onDeleteClick = { onDeleteClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StockItemCard(
    item: InventoryItem,
    onRestockClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isLowStock = item.quantity <= item.minThreshold

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isLowStock) DangerRed.copy(alpha = 0.5f) else GeoBorder,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GeoCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Type Badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (item.type == "SHEET") GeoBadgeBg else Color(0xFFFFECE0),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (item.type == "SHEET") "uPVC SHEET" else "HARDWARE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.type == "SHEET") GeoBadgeText else Color(0xFFD84315)
                            )
                        }
                        if (isLowStock) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(color = DangerRed.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "સ્ટોક ખૂટે છે (Low Stock)",
                                    fontSize = 9.sp,
                                    color = DangerRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        color = GeoTextDark,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.specification,
                        color = GeoTextMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Quantity Circle Container
                Column(
                    modifier = Modifier.padding(start = 8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else "%.1f".format(item.quantity),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isLowStock) DangerRed else GeoPrimaryAction
                    )
                    Text(
                        text = item.unit,
                        fontSize = 11.sp,
                        color = GeoTextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = GeoBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Warning threshold detail
                Text(
                    text = "Alert threshold: < ${item.minThreshold.toInt()} ${item.unit}",
                    color = GeoTextMuted,
                    fontSize = 11.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick restock button
                    OutlinedButton(
                        onClick = onRestockClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = GeoPrimaryAction
                        ),
                        border = BorderStroke(1.dp, GeoBorder),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("સ્ટોક વધારો (+ qty)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Edit Icon
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(32.dp)
                            .background(GeoNavBorder, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Item",
                            tint = GeoTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Delete Icon
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(32.dp)
                            .background(DangerRed.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Item",
                            tint = DangerRed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. BUILD TAB CONTENT (AUTO-DEDUCT)
// ==========================================

@Composable
fun BuildTabContent(
    recipes: List<FurnitureRecipe>,
    items: List<InventoryItem>,
    selectedRecipe: FurnitureRecipe?,
    adjustingIngredients: List<RecipeIngredient>,
    onSelectRecipe: (FurnitureRecipe) -> Unit,
    onIngredientQtyChange: (itemId: Int, qty: Double) -> Unit,
    onBuildClick: (String, Boolean) -> Unit,
    onCancelBuild: () -> Unit,
    onAddRecipeTemplateClick: () -> Unit,
    onAddCustomBuildClick: () -> Unit,
    onDeleteRecipe: (Int) -> Unit
) {
    if (selectedRecipe != null) {
        // Auto-Deduct Configuration View
        ActiveBuildDeductView(
            recipe = selectedRecipe,
            adjustingIngredients = adjustingIngredients,
            items = items,
            onQtyChange = onIngredientQtyChange,
            onBuildClick = { deduct -> onBuildClick(selectedRecipe.name, deduct) },
            onCancel = onCancelBuild
        )
    } else {
        // Main list of recipes View
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
        ) {
            Text(
                text = "ફર્નિચર બનાવો અને સ્ટોક ઓટો કાપો \n(Furniture Recipes & Auto Deduction)",
                fontWeight = FontWeight.Bold,
                color = GeoTextDark,
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = "કોઈપણ ફર્નિચર નો નમૂનો સિલેક્ટ કરો જેથી જરૂરી uPVC શીટ્સ અને હાર્ડવેર સ્ટોક માંથી આપોઆપ ઓલરેડી માઈનસ થઈ જાય.",
                color = GeoTextMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Option Actions Group
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAddCustomBuildClick,
                    modifier = Modifier.weight(1.1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryAction),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ઝડપી બિલ્ડ / Direct Build", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onAddRecipeTemplateClick,
                    modifier = Modifier.weight(0.9f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GeoPrimaryAction),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, GeoBorder)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("નવો નમૂનો (Recipe+)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (recipes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "કોઈ ફર્નિચર રેસિપી બનાવેલ નથી.\nઉપર 'નવો નમૂનો' ક્લિક કરી ઉમેરો.\n(No recipes found. Add template now!)",
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(recipes) { recipe ->
                        RecipeItemCard(
                            recipe = recipe,
                            onSelect = { onSelectRecipe(recipe) },
                            onDelete = { onDeleteRecipe(recipe.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeItemCard(
    recipe: FurnitureRecipe,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val converters = com.example.data.Converters()
    val ingredients = converters.stringToIngredients(recipe.ingredientsJson) ?: emptyList()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .border(1.dp, GeoBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GeoCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recipe.name,
                    fontWeight = FontWeight.Bold,
                    color = GeoTextDark,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Template",
                        tint = DangerRed.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = recipe.description,
                color = GeoTextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            HorizontalDivider(color = GeoBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Quick list of ingredients inside template
            Text(
                text = "જરૂરી વસ્તુઓ (Materials required):",
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = GeoPrimaryAction
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ingredients.take(3).forEach { ing ->
                    Box(
                        modifier = Modifier
                            .background(GeoNavBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${ing.name}: ${if (ing.neededQty % 1.0 == 0.0) ing.neededQty.toInt() else ing.neededQty}",
                            fontSize = 10.sp,
                            color = GeoTextPrimary
                        )
                    }
                }
                if (ingredients.size > 3) {
                    Box(
                        modifier = Modifier
                            .background(GeoNavBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "+${ingredients.size - 3} more",
                            fontSize = 10.sp,
                            color = GeoTextMuted
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "પસંદ ક્રમો અને બનાવો (Select & Build) ➔",
                    color = GeoPrimaryAction,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun ActiveBuildDeductView(
    recipe: FurnitureRecipe,
    adjustingIngredients: List<RecipeIngredient>,
    items: List<InventoryItem>,
    onQtyChange: (itemId: Int, qty: Double) -> Unit,
    onBuildClick: (Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var deductStock by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 30.dp)
    ) {
        item {
            // Header back button row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "◀ નમૂના પર પાછા (Templates List)",
                    color = GeoPrimaryAction,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable { onCancel() }
                        .padding(vertical = 4.dp)
                )
            }

            Text(
                text = recipe.name,
                fontWeight = FontWeight.Bold,
                color = GeoTextDark,
                fontSize = 18.sp
            )
            Text(
                text = "${recipe.description} - જો તમે માપ બદલ્યા હોય, તો નીચે જથ્થો પ્લસ-માઇનસ કરી શકો છો, પછી 'બિલ્ડ કન્ફર્મ' કરો.",
                color = GeoTextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = GeoBorder)
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Adjustable items
        items(adjustingIngredients) { ing ->
            val actualStockItem = items.find { it.id == ing.itemId }
            val currentStock = actualStockItem?.quantity ?: 0.0
            val isSufficient = currentStock >= ing.neededQty

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isSufficient) GeoBorder else DangerRed.copy(alpha = 0.5f),
                        RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GeoCardBg)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ing.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = GeoTextDark
                        )
                        Text(
                            text = ing.specification,
                            fontSize = 11.sp,
                            color = GeoTextMuted
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        // Real-time Stock levels comparison
                        Row {
                            Text(
                                text = "હાલ સ્ટોક (Stock): $currentStock",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSufficient) SuccessGreen else DangerRed
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isSufficient) "પર્યાપ્ત છે ✔" else "સ્ટોક ખૂટે છે ❌",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSufficient) SuccessGreen else DangerRed
                            )
                        }
                    }

                    // Increments / Decrements counter
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { onQtyChange(ing.itemId, ing.neededQty - (if (actualStockItem?.type == "SHEET") 0.1 else 1.0)) },
                            modifier = Modifier
                                .size(28.dp)
                                .background(GeoNavBorder, CircleShape)
                        ) {
                            Text("-", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }

                        // Text showing value (formatted)
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .background(GeoNavBorder, RoundedCornerShape(8.dp))
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (ing.neededQty % 1.0 == 0.0) ing.neededQty.toInt().toString() else "%.1f".format(ing.neededQty),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GeoTextDark
                            )
                        }

                        IconButton(
                            onClick = { onQtyChange(ing.itemId, ing.neededQty + (if (actualStockItem?.type == "SHEET") 0.1 else 1.0)) },
                            modifier = Modifier
                                .size(28.dp)
                                .background(GeoNavBorder, CircleShape)
                        ) {
                            Text("+", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Toggle switch for Stock Deduction Choice
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GeoBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GeoCardBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { deductStock = !deductStock }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = deductStock,
                        onCheckedChange = { deductStock = it },
                        colors = CheckboxDefaults.colors(checkedColor = GeoPrimaryAction)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "સ્ટોક માઈનસ (કાપવો) કરવો છે?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = GeoTextDark
                        )
                        Text(
                            text = if (deductStock) "ચાલુ: મટિરિયલ સ્ટોક માંથી ઓછું થશે." else "બંધ: માત્ર ઇતિહાસ ડાયરીમાં બિલ્ડ રેકોર્ડ થશે, સ્ટોક નહીં કપાય.",
                            fontSize = 11.sp,
                            color = GeoTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Check if ALL are sufficient to help colour coding button
            val hasDeficientItem = adjustingIngredients.any { ing ->
                val matching = items.find { it.id == ing.itemId }
                val current = matching?.quantity ?: 0.0
                current < ing.neededQty
            }

            Button(
                onClick = { onBuildClick(deductStock) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("build_confirm_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (deductStock && hasDeficientItem) DangerRed.copy(alpha = 0.5f) else GeoPrimaryAction
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Build, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (deductStock) "બનાવો - સ્ટોક આપોઆપ કાપો (Build & Deduct)" else "બનાવો - માત્ર ડાયરીમાં રેકોર્ડ સેવ કરો (History Record Only)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, GeoBorder)
            ) {
                Text("રદ કરો (Cancel)", color = GeoTextMuted)
            }
        }
    }
}

// ==========================================
// 3. LOGS / HISTORY TAB CONTENT
// ==========================================

@Composable
fun LogsTabContent(
    logs: List<TransactionLog>,
    items: List<InventoryItem>,
    recipes: List<FurnitureRecipe>,
    onClearClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "સ્ટોક અને કપાયેલ હિસાબ ડાયરી (History Logs)",
                    fontWeight = FontWeight.Bold,
                    color = GeoTextDark,
                    fontSize = 16.sp
                )
                Text(
                    text = "સ્ટોક આવ-જાવ અને લીધેલ બાદની તમામ હિસ્ટ્રી ડેટા.",
                    color = GeoTextMuted,
                    fontSize = 11.sp
                )
            }

            if (logs.isNotEmpty()) {
                IconButton(
                    onClick = onClearClick
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Logs",
                        tint = DangerRed
                    )
                }
            }
        }

        // ----------------------------------------
        // Gmail Backup Section Card
        // ----------------------------------------
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = GeoCardBg),
            border = BorderStroke(1.dp, GeoBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(GeoAccentLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Gmail Backup",
                            tint = GeoAccentDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "જીમેલ બેકઅપ (Gmail Backup File)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = GeoTextDark
                        )
                        Text(
                            text = "તમામ સ્ટોક ડેટા અને ડાયરી હિસાબ સુરક્ષિત રીતે તમારા Gmail પર મોકલો.",
                            fontSize = 11.sp,
                            color = GeoTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val backupText = buildBackupText(items, recipes, logs)
                        val dateFormatted = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_SUBJECT, "uPVC Stock App Backup - $dateFormatted")
                            putExtra(Intent.EXTRA_TEXT, backupText)
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, "જીમેલ પસંદ કરો (Send via Gmail)"))
                        } catch (e: Exception) {
                            val fallback = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "uPVC Stock App Backup - $dateFormatted")
                                putExtra(Intent.EXTRA_TEXT, backupText)
                            }
                            context.startActivity(Intent.createChooser(fallback, "Share Backup"))
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryAction),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ડેટા જીમેલ પર સેવ કરો (Backup Now)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "હજુ સુધી કોઈ હિસ્ટ્રી ડેટા નથી (No transaction logs yet)",
                    fontSize = 13.sp,
                    color = GeoTextMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(logs) { log ->
                    LogItemCard(log)
                }
            }
        }
    }
}

private fun buildBackupText(
    items: List<InventoryItem>,
    recipes: List<FurnitureRecipe>,
    logs: List<TransactionLog>
): String {
    val dateStr = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
    val sb = java.lang.StringBuilder()
    sb.append("=========================================\n")
    sb.append("      uPVC Stock App - DATA BACKUP      \n")
    sb.append("=========================================\n")
    sb.append("તારીખ / Backup Date: $dateStr\n")
    sb.append("કુલ આઇટમ્સ / Total Items: ${items.size}\n")
    sb.append("કુલ ફર્નિચર રેસિપિ / Total Recipes: ${recipes.size}\n")
    sb.append("કુલ ટ્રાન્ઝેકશન લોગ્ઝ / Total History Logs: ${logs.size}\n")
    sb.append("\n")

    sb.append("-----------------------------------------\n")
    sb.append("1. STOCK ITEMS LIST (સ્ટોક આઇટમ્સની યાદી)\n")
    sb.append("-----------------------------------------\n")
    if (items.isEmpty()) {
        sb.append("(કોઈ સ્ટોક આઇટમ ઉપલબ્ધ નથી / No items)\n")
    } else {
        items.forEachIndexed { index, item ->
            sb.append("${index + 1}. [${item.type}] ${item.name}\n")
            sb.append("   - Size/Specification: ${item.specification}\n")
            sb.append("   - Quantity: ${item.quantity} ${item.unit}\n")
            sb.append("   - Min Alert Limit: ${item.minThreshold} ${item.unit}\n\n")
        }
    }

    sb.append("-----------------------------------------\n")
    sb.append("2. FURNITURE RECIPES (ફર્નિચર નમૂનાઓ - રેસિપી)\n")
    sb.append("-----------------------------------------\n")
    if (recipes.isEmpty()) {
        sb.append("(કોઈ રેસિપી ઉપલબ્ધ નથી / No recipes)\n")
    } else {
        recipes.forEachIndexed { index, rec ->
            sb.append("${index + 1}. Name: ${rec.name}\n")
            sb.append("   - Detail: ${rec.description}\n")
            sb.append("   - Ingredients JSON: ${rec.ingredientsJson}\n\n")
        }
    }

    sb.append("-----------------------------------------\n")
    sb.append("3. RECENT ACTIVITY LOGS (સ્ટોક હિસાબ ડાયરી)\n")
    sb.append("-----------------------------------------\n")
    if (logs.isEmpty()) {
        sb.append("(લોગ યાદી ખાલી છે / No logs)\n")
    } else {
        val logFormatter = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
        logs.forEachIndexed { index, log ->
            val d = logFormatter.format(Date(log.timestamp))
            sb.append("${index + 1}. [$d] TYPE: ${log.type}\n")
            sb.append("   - Title: ${log.title}\n")
            sb.append("   - Detail: ${log.description}\n\n")
        }
    }

    sb.append("=========================================\n")
    sb.append("  RAW JSON DATABASE (FOR DATA RESTORE)\n")
    sb.append("=========================================\n")
    sb.append("{\n")
    sb.append("  \"backup_timestamp\": ${System.currentTimeMillis()},\n")
    sb.append("  \"items\": [\n")
    items.forEachIndexed { idx, it ->
        sb.append("    {\"id\": ${it.id}, \"name\":\"${it.name.replace("\"", "\\\"")}\", \"type\":\"${it.type}\", \"specification\":\"${it.specification.replace("\"", "\\\"")}\", \"quantity\":${it.quantity}, \"unit\":\"${it.unit}\", \"minThreshold\":${it.minThreshold}}")
        if (idx < items.size - 1) sb.append(",")
        sb.append("\n")
    }
    sb.append("  ],\n")

    sb.append("  \"recipes\": [\n")
    recipes.forEachIndexed { idx, rec ->
        sb.append("    {\"id\": ${rec.id}, \"name\":\"${rec.name.replace("\"", "\\\"")}\", \"description\":\"${rec.description.replace("\"", "\\\"")}\", \"ingredientsJson\":\"${rec.ingredientsJson.replace("\"", "\\\"")}\"}")
        if (idx < recipes.size - 1) sb.append(",")
        sb.append("\n")
    }
    sb.append("  ],\n")

    sb.append("  \"logs\": [\n")
    logs.forEachIndexed { idx, log ->
        sb.append("    {\"id\": ${log.id}, \"timestamp\":${log.timestamp}, \"type\":\"${log.type}\", \"title\":\"${log.title.replace("\"", "\\\"")}\", \"description\":\"${log.description.replace("\"", "\\\"")}\"}")
        if (idx < logs.size - 1) sb.append(",")
        sb.append("\n")
    }
    sb.append("  ]\n")
    sb.append("}\n")

    return sb.toString()
}

@Composable
fun LogItemCard(log: TransactionLog) {
    val formatter = remember { SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault()) }
    val formattedDate = formatter.format(Date(log.timestamp))

    val colorAccent = when (log.type) {
        "ADD_STOCK" -> SuccessGreen
        "REMOVE_STOCK" -> DangerRed
        "BUILD_FURNITURE" -> GeoPrimaryAction
        else -> GeoPrimaryAction
    }

    val icon = when (log.type) {
        "ADD_STOCK" -> Icons.Default.Add
        "REMOVE_STOCK" -> Icons.Default.Delete
        "BUILD_FURNITURE" -> Icons.Default.Build
        else -> Icons.Default.Edit
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GeoBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GeoCardBg)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Pill status icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(colorAccent.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colorAccent,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.title,
                        fontWeight = FontWeight.Bold,
                        color = GeoTextDark,
                        fontSize = 13.sp
                    )
                    Text(
                        text = formattedDate,
                        color = GeoTextMuted,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = log.description,
                    color = GeoTextPrimary.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

// ==========================================
// FORM DIALOGUES BLOCK
// ==========================================

// 1. New Material Form
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, specification: String, quantity: Double, unit: String, minThreshold: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("SHEET") } // SHEET or HARDWARE
    var specification by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("Sheets") }
    var minThresholdStr by remember { mutableStateOf("5") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .background(Color.White)
            ) {
                Text(
                    text = "નવો સ્ટોક ઉમેરો (Add New Stock)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GeoTextDark
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("વસ્તુનું નામ (Item Name)") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeoPrimaryAction,
                        unfocusedBorderColor = GeoBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Type selector
                Text(
                    text = "પ્રકાર (Type):",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = GeoTextDark
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val types = listOf("SHEET" to "uPVC Sheet", "HARDWARE" to "હાર્ડ વેર (Hardware)")
                    types.forEach { (typeKey, typeLabel) ->
                        val isSelected = selectedType == typeKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) GeoPrimaryAction else GeoNavBorder)
                                .clickable {
                                    selectedType = typeKey
                                    unit = if (typeKey == "SHEET") "Sheets" else "Pieces"
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = typeLabel,
                                color = if (isSelected) Color.White else GeoTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Specifications
                OutlinedTextField(
                    value = specification,
                    onValueChange = { specification = it },
                    label = { Text("વિગતો/કદ (Specification e.g: 8x4 White)") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeoPrimaryAction,
                        unfocusedBorderColor = GeoBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quantity & Unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("જથ્થો (Qty)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoPrimaryAction,
                            unfocusedBorderColor = GeoBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("એકમ (Unit e.g: Pcs)") },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoPrimaryAction,
                            unfocusedBorderColor = GeoBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Alert Threshold
                OutlinedTextField(
                    value = minThresholdStr,
                    onValueChange = { minThresholdStr = it },
                    label = { Text("ચેતવણી લિમિટ (Low Stock Limit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeoPrimaryAction,
                        unfocusedBorderColor = GeoBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("બંધ કરો (Cancel)", color = GeoTextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val qty = quantityStr.toDoubleOrNull() ?: 0.0
                                val limit = minThresholdStr.toDoubleOrNull() ?: 5.0
                                onConfirm(name, selectedType, specification, qty, unit, limit)
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryAction)
                    ) {
                        Text("સાચવો (Add Stock)")
                    }
                }
            }
        }
    }
}

// 2. Quick Stock Adding Form
@Composable
fun RestockDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onConfirm: (itemId: Int, addQty: Double) -> Unit
) {
    var addQtyStr by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "સ્ટોક જથ્થો વધારો (Add Stock Quantity)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GeoTextDark
                )
                Text(
                    text = "${item.name} (${item.specification})",
                    fontSize = 12.sp,
                    color = GeoTextMuted,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick buttons helper row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val recommendations = if (item.type == "SHEET") listOf(5.0, 10.0, 20.0) else listOf(50.0, 100.0, 200.0)
                    recommendations.forEach { value ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(GeoNavBorder)
                                .clickable { addQtyStr = value.toInt().toString() }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+${value.toInt()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = GeoPrimaryAction
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = addQtyStr,
                    onValueChange = { addQtyStr = it },
                    label = { Text("કેટલો જથ્થો ઉમેરવો? (Quantity to add)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeoPrimaryAction,
                        unfocusedBorderColor = GeoBorder
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    suffix = { Text(item.unit) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("બંધ કરી (Cancel)", color = GeoTextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = addQtyStr.toDoubleOrNull() ?: 0.0
                            if (amount > 0.0) {
                                onConfirm(item.id, amount)
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryAction)
                    ) {
                        Text("સ્ટોક વધારો (Add)")
                    }
                }
            }
        }
    }
}

// 3. Edit Stock Form
@Composable
fun EditItemDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onConfirm: (InventoryItem) -> Unit
) {
    var name by remember { mutableStateOf(item.name) }
    var specification by remember { mutableStateOf(item.specification) }
    var quantityStr by remember { mutableStateOf(item.quantity.toString()) }
    var unit by remember { mutableStateOf(item.unit) }
    var minThresholdStr by remember { mutableStateOf(item.minThreshold.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "સ્ટોક સુધારો (Edit Stock Item)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GeoTextDark
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("માલનું નામ (Name)") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeoPrimaryAction,
                        unfocusedBorderColor = GeoBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = specification,
                    onValueChange = { specification = it },
                    label = { Text("વિગતો/કદ (Specification)") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeoPrimaryAction,
                        unfocusedBorderColor = GeoBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("જથ્થો (Qty)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoPrimaryAction,
                            unfocusedBorderColor = GeoBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("એકમ (Unit)") },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoPrimaryAction,
                            unfocusedBorderColor = GeoBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = minThresholdStr,
                    onValueChange = { minThresholdStr = it },
                    label = { Text("ઓછા સ્ટોકની લિમિટ (Low stock limit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeoPrimaryAction,
                        unfocusedBorderColor = GeoBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("બંધ કરો (Cancel)", color = GeoTextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val updated = item.copy(
                                    name = name,
                                    specification = specification,
                                    quantity = quantityStr.toDoubleOrNull() ?: item.quantity,
                                    unit = unit,
                                    minThreshold = minThresholdStr.toDoubleOrNull() ?: item.minThreshold
                                )
                                onConfirm(updated)
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryAction)
                    ) {
                        Text("કન્ફર્મ કરો (Save)")
                    }
                }
            }
        }
    }
}

// 4. Create Custom Recipe Dialog
@Composable
fun AddRecipeDialog(
    availableItems: List<InventoryItem>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, desc: String, ingredients: List<RecipeIngredient>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    val selectedIngredients = remember { mutableStateListOf<Pair<InventoryItem, String>>() } // Item & its input qty text

    var selectedItemForAdd by remember { mutableStateOf<InventoryItem?>(null) }
    var qtyForAddStr by remember { mutableStateOf("") }

    // Init first item selector helper
    LaunchedEffect(availableItems) {
        if (availableItems.isNotEmpty()) {
            selectedItemForAdd = availableItems.first()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "નવો ફર્નિચર નમૂનો બનાવો (New Template)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GeoTextDark
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ફર્નિચર નામ (e.g., UPVC Glass Window)") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeoPrimaryAction,
                        unfocusedBorderColor = GeoBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("નમૂના અંગે વિગત (e.g., 3 hinges, 1 screw)") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeoPrimaryAction,
                        unfocusedBorderColor = GeoBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = GeoBorder)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "ઉમેરો કઈ કઈ કડી વપરાશે (Assign Required Items):",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = GeoPrimaryAction
                )

                // Select materials helper row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Dropbox style using click selector
                    var expandedMenu by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1.5f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GeoBorder, RoundedCornerShape(20.dp))
                                .clickable { expandedMenu = true }
                                .padding(10.dp)
                        ) {
                            Text(
                                text = selectedItemForAdd?.name ?: "પસંદ કરો (Select Item)",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false }
                        ) {
                            availableItems.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text("${item.name} (${item.specification})", fontSize = 11.sp) },
                                    onClick = {
                                        selectedItemForAdd = item
                                        expandedMenu = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = qtyForAddStr,
                        onValueChange = { qtyForAddStr = it },
                        modifier = Modifier
                            .weight(0.8f)
                            .height(52.dp),
                        label = { Text("Needed qty", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoPrimaryAction,
                            unfocusedBorderColor = GeoBorder
                        )
                    )

                    IconButton(
                        onClick = {
                            val selItem = selectedItemForAdd
                            val qty = qtyForAddStr.toDoubleOrNull() ?: 1.0
                            if (selItem != null && qty > 0.0) {
                                // Add or update existings inside list
                                val idx = selectedIngredients.indexOfFirst { it.first.id == selItem.id }
                                if (idx != -1) {
                                    selectedIngredients[idx] = Pair(selItem, qty.toString())
                                } else {
                                    selectedIngredients.add(Pair(selItem, qty.toString()))
                                }
                                qtyForAddStr = ""
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(GeoPrimaryAction, RoundedCornerShape(20.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Added items list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .background(GeoNavBorder, RoundedCornerShape(20.dp))
                        .padding(4.dp)
                ) {
                    items(selectedIngredients) { itemPair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .padding(8.dp)
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(itemPair.first.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GeoTextDark)
                                Text("Spec: ${itemPair.first.specification}", fontSize = 10.sp, color = GeoTextMuted)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${itemPair.second} ${itemPair.first.unit}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GeoPrimaryAction
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { selectedIngredients.remove(itemPair) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("બંધ કરો", color = GeoTextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && selectedIngredients.isNotEmpty()) {
                                val ings = selectedIngredients.map { (item, qtyText) ->
                                    RecipeIngredient(
                                        itemId = item.id,
                                        name = item.name,
                                        specification = item.specification,
                                        neededQty = qtyText.toDoubleOrNull() ?: 1.0
                                    )
                                }
                                onConfirm(name, desc, ings)
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryAction)
                    ) {
                        Text("બનાવો (Save Recipe)")
                    }
                }
            }
        }
    }
}

// 5. Custom on-the-spot build builder dialog
@Composable
fun CustomBuildDialog(
    availableItems: List<InventoryItem>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, ingredients: List<RecipeIngredient>, deductStock: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("ઓન-ધ-સ્પોટ ફર્નિચર (Quick Furniture Custom)") }
    val selectedIngredients = remember { mutableStateListOf<Pair<InventoryItem, String>>() }
    var deductStock by remember { mutableStateOf(true) }

    var selectedItemForAdd by remember { mutableStateOf<InventoryItem?>(null) }
    var qtyForAddStr by remember { mutableStateOf("") }

    // Init first item helper
    LaunchedEffect(availableItems) {
        if (availableItems.isNotEmpty()) {
            selectedItemForAdd = availableItems.first()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "ઝડપી બિલ્ડ: ઇન્સ્ટન્ટ કાપો સ્ટોક",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GeoTextDark
                )
                Text(
                    text = "ડાયરેક્ટ uPVC શીટ્સ અને મટીરીયલ સિલેક્ટ કરીને સ્ટોકમાંથી કાપો, કોઈ રેસિપી સેવ કરવાની જરૂર નથી.",
                    fontSize = 11.sp,
                    color = GeoTextMuted,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("ફર્નિચર કે કામની ટિપ્પણી (Description of Work)") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeoPrimaryAction,
                        unfocusedBorderColor = GeoBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = GeoBorder)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "ઉમેરો કઈ કઈ કડી કાપવાની છે (Select Items to Deduct):",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = GeoPrimaryAction
                )

                // Selector row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var expandedMenu by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1.5f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GeoBorder, RoundedCornerShape(20.dp))
                                .clickable { expandedMenu = true }
                                .padding(10.dp)
                        ) {
                            Text(
                                text = selectedItemForAdd?.name ?: "પસંદ કરો (Select Item)",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false }
                        ) {
                            availableItems.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text("${item.name} (${item.specification}) - Stocks: ${item.quantity}", fontSize = 11.sp) },
                                    onClick = {
                                        selectedItemForAdd = item
                                        expandedMenu = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = qtyForAddStr,
                        onValueChange = { qtyForAddStr = it },
                        modifier = Modifier
                            .weight(0.8f)
                            .height(52.dp),
                        label = { Text("Qty", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoPrimaryAction,
                            unfocusedBorderColor = GeoBorder
                        )
                    )

                    IconButton(
                        onClick = {
                            val selItem = selectedItemForAdd
                            val qty = qtyForAddStr.toDoubleOrNull() ?: 1.0
                            if (selItem != null && qty > 0.0) {
                                val idx = selectedIngredients.indexOfFirst { it.first.id == selItem.id }
                                if (idx != -1) {
                                    selectedIngredients[idx] = Pair(selItem, qty.toString())
                                } else {
                                    selectedIngredients.add(Pair(selItem, qty.toString()))
                                }
                                qtyForAddStr = ""
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(GeoPrimaryAction, RoundedCornerShape(20.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Added deduction list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .background(GeoNavBorder, RoundedCornerShape(20.dp))
                        .padding(4.dp)
                ) {
                    items(selectedIngredients) { itemPair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .padding(8.dp)
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(itemPair.first.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GeoTextDark)
                                Text("Available Stocks: ${itemPair.first.quantity}", fontSize = 10.sp, color = GeoTextMuted)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "-${itemPair.second} ${itemPair.first.unit}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DangerRed
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { selectedIngredients.remove(itemPair) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Deduct stock option checkbox row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { deductStock = !deductStock }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = deductStock,
                        onCheckedChange = { deductStock = it },
                        colors = CheckboxDefaults.colors(checkedColor = GeoPrimaryAction)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "સ્ટોક આપોઆપ કાપવો?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = GeoTextDark
                        )
                        Text(
                            text = if (deductStock) "ચાલુ: ઈન્વેન્ટરીમાંથી માઈનસ થશે." else "બંધ: માત્ર ઇતિહાસમાં એન્ટ્રી સેવ થશે.",
                            fontSize = 9.sp,
                            color = GeoTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("બંધ કરો", color = GeoTextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && selectedIngredients.isNotEmpty()) {
                                val ings = selectedIngredients.map { (item, qtyText) ->
                                    RecipeIngredient(
                                        itemId = item.id,
                                        name = item.name,
                                        specification = item.specification,
                                        neededQty = qtyText.toDoubleOrNull() ?: 1.0
                                    )
                                }
                                onConfirm(title, ings, deductStock)
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryAction)
                    ) {
                        Text(if (deductStock) "બનાવો (Deduct Stock Now)" else "હિસાબ સાચવો (Diary Only)")
                    }
                }
            }
        }
    }
}

// ==============================================================
// 6. SITE MEASUREMENTS & SQFT CALCULATOR TAB
// ==============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementsTabContent(viewModel: InventoryViewModel) {
    val measurements by viewModel.siteMeasurements.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    var title by remember { mutableStateOf("") }
    var heightStr by remember { mutableStateOf("") }
    var widthStr by remember { mutableStateOf("") }
    var isFeet by remember { mutableStateOf(false) } // false = Inches, true = Feet
    var isDouble by remember { mutableStateOf(false) }

    // Live calculation for visual reference
    val h = heightStr.toDoubleOrNull() ?: 0.0
    val w = widthStr.toDoubleOrNull() ?: 0.0
    val calculatedSqFt = remember(h, w, isFeet, isDouble) {
        val base = if (isFeet) (h * w) else ((h * w) / 144.0)
        if (isDouble) (base * 2.0) else base
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 30.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GeoBorder, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = GeoCardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "નવી માપણી ગણતરી (Site Area Calculator)",
                        fontWeight = FontWeight.Bold,
                        color = GeoTextDark,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "furniture નું લંબાઈ x પહોળાઈ ભરો, ચોરસ ફૂટ (SqFt) આપોઆપ આવી જશે. માપ ડબલ પણ કરી શકાય છે.",
                        color = GeoTextMuted,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("માપણી વિગત / જગ્યા (e.g. રસોડા ની બારી, Almari Back Panel)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoPrimaryAction,
                            unfocusedBorderColor = GeoBorder,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = heightStr,
                            onValueChange = { heightStr = it },
                            label = { Text("ઊંચાઈ (Height)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GeoPrimaryAction,
                                unfocusedBorderColor = GeoBorder,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = widthStr,
                            onValueChange = { widthStr = it },
                            label = { Text("પહોળાઈ (Width)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GeoPrimaryAction,
                                unfocusedBorderColor = GeoBorder,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Unit Select (Inches vs Feet)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "માપ કઈ યુનિટમાં છે?:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = GeoTextDark
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = !isFeet,
                                onClick = { isFeet = false },
                                colors = RadioButtonDefaults.colors(selectedColor = GeoPrimaryAction)
                            )
                            Text("ઇંચ (Inches)", fontSize = 11.sp, color = GeoTextPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            RadioButton(
                                selected = isFeet,
                                onClick = { isFeet = true },
                                colors = RadioButtonDefaults.colors(selectedColor = GeoPrimaryAction)
                            )
                            Text("ફૂટ (Feet)", fontSize = 11.sp, color = GeoTextPrimary)
                        }
                    }

                    HorizontalDivider(color = GeoBorder, modifier = Modifier.padding(vertical = 10.dp))

                    // Double Measurement Option Row (As requested by user!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDouble = !isDouble }
                            .background(
                                if (isDouble) GeoAccentLight.copy(alpha = 0.5f) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isDouble,
                            onCheckedChange = { isDouble = it },
                            colors = CheckboxDefaults.colors(checkedColor = GeoPrimaryAction)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "માપ ને બે ગણું (ડબલ) કરવું છે? (Double Size / Alternate Charge)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isDouble) GeoAccentDark else GeoTextDark
                            )
                            Text(
                                text = "આ એક્ટિવ કરવાથી સ્ક્વેર ફૂટ ઓટોમેટિક ડબલ (x2) ગણાઈ જશે.",
                                fontSize = 10.sp,
                                color = GeoTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Visual Live Square Feet Result Box
                    if (h > 0.0 && w > 0.0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GeoPrimaryAction.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "લાઈવ ગણતરી (Calculated SqFt):",
                                    fontSize = 11.sp,
                                    color = GeoPrimaryAction,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "%.2f ચોરસ ફૂટ (SqFt)".format(calculatedSqFt),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GeoPrimaryAction
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Save Measurement Action Button
                    Button(
                        onClick = {
                            if (title.isNotBlank() && h > 0.0 && w > 0.0) {
                                viewModel.addSiteMeasurement(
                                    title = title,
                                    height = h,
                                    width = w,
                                    isFeet = isFeet,
                                    isDouble = isDouble,
                                    sqFt = calculatedSqFt
                                )
                                // Clear inputs
                                title = ""
                                heightStr = ""
                                widthStr = ""
                                isDouble = false
                            }
                        },
                        enabled = title.isNotBlank() && h > 0.0 && w > 0.0,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryAction),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("લિસ્ટમાં સાચવો (Save to Measurements)")
                    }
                }
            }
        }

        // Section header and aggregate status of saved measurements
        if (measurements.isNotEmpty()) {
            val totalSavedSqFt = measurements.sumOf { it.sqFt }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ચાલુ સાઇટ માપણી લિસ્ટ (${measurements.size})",
                        fontWeight = FontWeight.Bold,
                        color = GeoTextDark,
                        fontSize = 15.sp
                    )

                    Button(
                        onClick = {
                            // Formatting beautiful share message
                            val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                            val sb = StringBuilder()
                            sb.append("📋 *uPVC FURNITURE SITE MEASUREMENTS*\n")
                            sb.append("તારીખ (Date): $dateStr\n")
                            sb.append("==================================\n\n")

                            measurements.forEachIndexed { i, m ->
                                val unitStr = if (m.isFeet) "ફૂટ" else "ઇંચ"
                                val doubleIndicator = if (m.isDouble) " (ડબલ માપ / x2)" else ""
                                sb.append("${i + 1}. *${m.title}*\n")
                                sb.append("   - સાઈઝ: ${m.height} x ${m.width} $unitStr$doubleIndicator\n")
                                sb.append("   - એરિયા: *${"%.2f".format(m.sqFt)} SqFt*\n\n")
                            }

                            sb.append("==================================\n")
                            sb.append("🔥 *કુલ ચોરસ ફૂટ (Total Area): ${"%.2f".format(totalSavedSqFt)} SqFt*\n")
                            sb.append("==================================\n")
                            sb.append("કોમ્પ્યુટર બાય: uPVC Stock Tracker 🚀")

                            // Native Trigger
                            val textToShare = sb.toString()
                            try {
                                val launchIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, textToShare)
                                    setPackage("com.whatsapp")
                                }
                                context.startActivity(launchIntent)
                            } catch (ex: Exception) {
                                val genericIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, textToShare)
                                }
                                context.startActivity(Intent.createChooser(genericIntent, "મોકલવા માટે એપ પસંદ કરો"))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("વ્હોટ્સએપ શેર કરો (Send)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(measurements) { m ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GeoBorder, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = m.title,
                                    fontWeight = FontWeight.Bold,
                                    color = GeoTextDark,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val unitStr = if (m.isFeet) "ફૂટ" else "ઇંચ"
                                val doubleIndicator = if (m.isDouble) " (ડબલ માપ / x2)" else ""
                                Text(
                                    text = "કદ: ${m.height} x ${m.width} $unitStr$doubleIndicator",
                                    fontSize = 11.sp,
                                    color = GeoTextMuted
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${"%.2f".format(m.sqFt)} SqFt",
                                    fontWeight = FontWeight.Black,
                                    color = GeoPrimaryAction,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(end = 6.dp)
                                )

                                IconButton(
                                    onClick = { viewModel.deleteSiteMeasurement(m.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Measurement",
                                        tint = DangerRed.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = GeoPrimaryAction),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "કુલ સ્ક્વેર ફૂટ સરવાળો (GRAND TOTAL SQFT)",
                                color = Color.White.copy(alpha = 0.82f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "%.2f SqFt".format(totalSavedSqFt),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            TextButton(
                                onClick = { viewModel.clearMeasurements() },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.8f))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("બધી ગણતરીઓ સાફ કરો (Clear All)", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
// ==============================================================
// 7. CUSTOMER KHATA LEDGER / ACCOUNTS TAB
// ==============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerKhataTabContent(viewModel: InventoryViewModel) {
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    var selectedCustomer by remember { mutableStateOf<CustomerKhata?>(null) }

    var newCustomerName by remember { mutableStateOf("") }
    var newCustomerPhone by remember { mutableStateOf("") }

    if (selectedCustomer != null) {
        // Individual Customer Ledger Details View
        CustomerLedgerDetailsScreen(
            customer = selectedCustomer!!,
            viewModel = viewModel,
            onBack = { selectedCustomer = null }
        )
    } else {
        // Main Customers Accounts Directory View
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 30.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GeoBorder, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = GeoCardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "નવું ખાતું ઉમેરો (Create Customer Khata Ledger)",
                            fontWeight = FontWeight.Bold,
                            color = GeoTextDark,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "નવા ગ્રાહકનું ખાતું ચાલુ કરવા નામ અને ફોન નંબર દાખલ કરો.",
                            color = GeoTextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        OutlinedTextField(
                            value = newCustomerName,
                            onValueChange = { newCustomerName = it },
                            label = { Text("ગ્રાહકનું નામ (Customer Full Name)") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GeoPrimaryAction,
                                unfocusedBorderColor = GeoBorder,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newCustomerPhone,
                            onValueChange = { newCustomerPhone = it },
                            label = { Text("મોબાઈલ નંબર (Mobile Number / For WhatsApp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GeoPrimaryAction,
                                unfocusedBorderColor = GeoBorder,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (newCustomerName.isNotBlank()) {
                                    viewModel.addCustomer(newCustomerName, newCustomerPhone)
                                    newCustomerName = ""
                                    newCustomerPhone = ""
                                }
                            },
                            enabled = newCustomerName.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryAction),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ખાતું બનાવો (Create Account Ledger)")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "ગ્રાહક ખાતાવહી (Registered Customer Accounts)",
                    fontWeight = FontWeight.Bold,
                    color = GeoTextDark,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (customers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "કોઈ ખાતા બનાવેલ નથી.\nઉપર નામ ભરી નવું ખાતું એડ કરો.\n(No registered customers yet!)",
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(customers) { customer ->
                    // Gather customer stats dynamically
                    val entries by remember(customer.id) {
                        viewModel.getEntriesForCustomer(customer.id)
                    }.collectAsStateWithLifecycle(initialValue = emptyList())

                    val totalDebit = entries.filter { it.type == "DEBIT" }.sumOf { it.amount }
                    val totalCredit = entries.filter { it.type == "CREDIT" }.sumOf { it.amount }
                    val netBalance = totalDebit - totalCredit

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCustomer = customer }
                            .border(1.dp, GeoBorder, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = customer.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = GeoTextDark
                                )

                                Text(
                                    text = if (customer.phone.isBlank()) "No Contact Info" else "📞 ${customer.phone}",
                                    fontSize = 12.sp,
                                    color = GeoTextMuted,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (netBalance > 0) "Pending: ₹${"%.0f".format(netBalance)}"
                                           else if (netBalance < 0) "Advance: ₹${"%.0f".format(-netBalance)}"
                                           else "Cleared",
                                    fontWeight = FontWeight.Bold,
                                    color = if (netBalance > 0) DangerRed else if (netBalance < 0) SuccessGreen else GeoTextMuted,
                                    fontSize = 13.sp
                                )

                                Text(
                                    text = "હિસાબ જુઓ ➔",
                                    fontSize = 10.sp,
                                    color = GeoPrimaryAction,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Sub ledger view for a specific Customer
data class DraftBillItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val description: String,
    val calculationType: String, // "SQFT" or "QTY"
    val height: Double = 0.0,
    val width: Double = 0.0,
    val isFeet: Boolean = true,
    val qty: Double = 1.0,
    val rate: Double = 0.0,
    val isDouble: Boolean = false,
    val calculatedSqFt: Double = 0.0,
    val totalAmount: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLedgerDetailsScreen(
    customer: CustomerKhata,
    viewModel: InventoryViewModel,
    onBack: () -> Unit
) {
    val entries by remember(customer.id) {
        viewModel.getEntriesForCustomer(customer.id)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val context = androidx.compose.ui.platform.LocalContext.current

    val totalDebit = entries.filter { it.type == "DEBIT" }.sumOf { it.amount }
    val totalCredit = entries.filter { it.type == "CREDIT" }.sumOf { it.amount }
    val netBalance = totalDebit - totalCredit

    var amountStr by remember { mutableStateOf("") }
    var entryNote by remember { mutableStateOf("") }
    var entryType by remember { mutableStateOf("DEBIT") } // DEBIT ("ઉધાર") or CREDIT ("જમા")

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showBillCreator by remember { mutableStateOf(false) }

    if (showBillCreator) {
        BillCreatorView(
            customer = customer,
            viewModel = viewModel,
            onDismiss = { showBillCreator = false }
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 30.dp)
        ) {
            item {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "◀ ગ્રાહક લિસ્ટ (Back)",
                        color = GeoPrimaryAction,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable { onBack() }
                            .padding(vertical = 4.dp)
                    )

                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Customer",
                            tint = DangerRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Main Details
                Text(
                    text = "${customer.name} નો હિસાબ",
                    fontWeight = FontWeight.Black,
                    color = GeoTextDark,
                    fontSize = 18.sp
                )
                if (customer.phone.isNotBlank()) {
                    Text(text = "📞 મોબાઈલ: ${customer.phone}", color = GeoTextMuted, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = GeoBorder)
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Summary Net Balance Dashboard for Customer
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (netBalance > 0) Color(0xFFFFF2F0)
                                       else if (netBalance < 0) Color(0xFFF0FFF4)
                                       else GeoCardBg
                    ),
                    border = BorderStroke(1.dp, if (netBalance > 0) DangerRed.copy(0.2f) else if (netBalance < 0) SuccessGreen.copy(0.2f) else GeoBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (netBalance > 0) "ગ્રાહક પાસે કુલ લેણા બાકી (PENDING DUE)"
                                   else if (netBalance < 0) "ગ્રાહક નું જમા બેલેન્સ (ADVANCE DEPOSIT)"
                                   else "ખાતું ચોખ્ખું છે (ACCOUNT RECONCILED)",
                            fontWeight = FontWeight.Bold,
                            color = GeoTextMuted,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${"%,.0f".format(if (netBalance >= 0) netBalance else -netBalance)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 26.sp,
                            color = if (netBalance > 0) DangerRed else if (netBalance < 0) SuccessGreen else GeoTextDark
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    // Formatting beautiful WhatsApp ledger print
                                    val dateStr = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()).format(Date())
                                    val sb = StringBuilder()
                                    sb.append("📝 *uPVC Stock & Accounts - ખાતા રિપોર્ટ*\n")
                                    sb.append("ગ્રાહક (Customer): *${customer.name}*\n")
                                    if (customer.phone.isNotBlank()) sb.append("મોબાઈલ (Phone): ${customer.phone}\n")
                                    sb.append("તારીખ: $dateStr\n")
                                    sb.append("--------------------------------------------------\n\n")

                                    entries.reversed().forEachIndexed { i, entry ->
                                        val prefix = if (entry.type == "CREDIT") "✅ [જમા / Received]" else "❌ [ઉધાર / Due]"
                                        sb.append("${i + 1}. $prefix\n")
                                        sb.append("   - રકમ: ₹${"%,.0f".format(entry.amount)}\n")
                                        sb.append("   - વિગત: ${entry.note}\n\n")
                                    }

                                    sb.append("--------------------------------------------------\n")
                                    if (netBalance > 0) {
                                        sb.append("🔥 *કુલ બાકી લેણા (Pending Bill): ₹${"%,.0f".format(netBalance)}*\n")
                                    } else if (netBalance < 0) {
                                        sb.append("🎉 *એડવાન્સ જમા રકમ (Advance): ₹${"%,.0f".format(-netBalance)}*\n")
                                    } else {
                                        sb.append("⚖ *કુલ હિસાબ: ચુકતે છે (Cleared / ₹0)*\n")
                                    }
                                    sb.append("--------------------------------------------------\n")
                                    sb.append("મોગલેલ બાય: uPVC Stock Tracker & Invoice Maker App")

                                    val textToShare = sb.toString()
                                    val phoneNumClean = customer.phone.filter { it.isDigit() }
                                    val uriString = if (phoneNumClean.isNotBlank()) {
                                        // Append India prefix if it doesn't exist
                                        val formattedPhone = if (phoneNumClean.length == 10) "91$phoneNumClean" else phoneNumClean
                                        "https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(textToShare)}"
                                    } else {
                                        "https://api.whatsapp.com/send?text=${Uri.encode(textToShare)}"
                                    }

                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val genericIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, textToShare)
                                        }
                                        context.startActivity(Intent.createChooser(genericIntent, "મોકલવા માટે એપ પસંદ કરો"))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ગ્રાહકને વ્હોટ્સએપ હિસાબ મોકલો", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { showBillCreator = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryAction),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("કસ્ટમર નું બિલ બનાવો (Generate detailed Bill / Invoice)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // New Ledger Entry Add Card Form
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GeoBorder, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "નવી ટ્રાન્ઝેક્શન એન્ટ્રી (Add Ledger Entry)",
                            fontWeight = FontWeight.Bold,
                            color = GeoTextDark,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = amountStr,
                                onValueChange = { amountStr = it },
                                label = { Text("જેટલી રકમ (Amount ₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GeoPrimaryAction,
                                    unfocusedBorderColor = GeoBorder
                                )
                            )

                            // Debit (Credit Sale) vs Credit (Payment Received) State Selector Selector
                            Column(
                                modifier = Modifier.weight(0.9f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .border(1.dp, GeoBorder, RoundedCornerShape(12.dp))
                                        .clip(RoundedCornerShape(12.dp)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(if (entryType == "DEBIT") DangerRed.copy(0.12f) else Color.Transparent)
                                            .clickable { entryType = "DEBIT" },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "ઉધાર (Due)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (entryType == "DEBIT") DangerRed else GeoTextMuted
                                        )
                                    }
                                    VerticalDivider(color = GeoBorder)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(if (entryType == "CREDIT") SuccessGreen.copy(0.12f) else Color.Transparent)
                                            .clickable { entryType = "CREDIT" },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "જમા (Paid)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (entryType == "CREDIT") SuccessGreen else GeoTextMuted
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = entryNote,
                            onValueChange = { entryNote = it },
                            label = { Text("એન્ટ્રી ની વિગત (e.g. નવી કબાટ, એડવાન્સ રોકડ, ફિટિંગ કામ)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GeoPrimaryAction,
                                unfocusedBorderColor = GeoBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val amt = amountStr.toDoubleOrNull() ?: 0.0
                                if (amt > 0.0) {
                                    viewModel.addKhataEntry(
                                        customerId = customer.id,
                                        amount = amt,
                                        type = entryType,
                                        note = entryNote.ifBlank { if (entryType == "DEBIT") "furniture વેચાણ" else "પેમેન્ટ ચૂકવણું" }
                                    )
                                    amountStr = ""
                                    entryNote = ""
                                }
                            },
                            enabled = amountStr.toDoubleOrNull() != null && (amountStr.toDoubleOrNull() ?: 0.0) > 0.0,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (entryType == "DEBIT") DangerRed else SuccessGreen
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (entryType == "DEBIT") "ઉધાર એન્ટ્રી ઉમેરો (- Debit)"
                                       else "જમા એન્ટ્રી ઉમેરો (+ Credit)"
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "ખાતાવાર રોજમેળ વિગત (Ledger Transactions)",
                    fontWeight = FontWeight.Bold,
                    color = GeoTextDark,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
            }

            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "કોઈ એન્ટ્રી નથી. ઉપર રકમ ભરી ઉધાર કે જમા કરો.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                items(entries) { entry ->
                    val formatter = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
                    val entryDateStr = formatter.format(Date(entry.timestamp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GeoBorder, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (entry.type == "CREDIT") SuccessGreen.copy(0.12f) else DangerRed.copy(0.12f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (entry.type == "CREDIT") "જમા (Paid)" else "ઉધાર (Due)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (entry.type == "CREDIT") SuccessGreen else DangerRed
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = entryDateStr,
                                        fontSize = 10.sp,
                                        color = GeoTextMuted
                                    )
                                }
                                Text(
                                    text = entry.note,
                                    fontWeight = FontWeight.Bold,
                                    color = GeoTextDark,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${if (entry.type == "CREDIT") "+" else "-"} ₹${"%,.0f".format(entry.amount)}",
                                    fontWeight = FontWeight.Black,
                                    color = if (entry.type == "CREDIT") SuccessGreen else DangerRed,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(end = 6.dp)
                                )

                                IconButton(
                                    onClick = { viewModel.deleteKhataEntry(entry.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Entry",
                                        tint = DangerRed.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Customer Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(customer.id)
                        showDeleteConfirmDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("હા, ખાતું ડિલીટ કરો", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("ના (Cancel)", color = GeoTextMuted)
                }
            },
            title = { Text("ખાતું ડિલીટ કરવું છે?", fontWeight = FontWeight.Bold, color = GeoTextDark) },
            text = { Text("આ કરવાથી ગ્રાહક '${customer.name}' ના બધા જ હિસાબો અને એન્ટ્રીઓ કાયમ માટે કમ્પ્યુટર માંથી ડિલીટ થઇ જશે.") },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillCreatorView(
    customer: CustomerKhata,
    viewModel: InventoryViewModel,
    onDismiss: () -> Unit
) {
    val siteMeasurements by viewModel.siteMeasurements.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Local state for draft list of items
    var billItems by remember { mutableStateOf(listOf<DraftBillItem>()) }

    // Input state for NEW item being edited
    var itemName by remember { mutableStateOf("") }
    var calcType by remember { mutableStateOf("SQFT") } // SQFT or QTY
    
    // Measurement inputs
    var heightStr by remember { mutableStateOf("") }
    var widthStr by remember { mutableStateOf("") }
    var isFeet by remember { mutableStateOf(true) }
    var isDouble by remember { mutableStateOf(false) }
    
    // Qty and Rate inputs
    var qtyStr by remember { mutableStateOf("1") }
    var rateStr by remember { mutableStateOf("") }

    // Dropdown/Selection state for Saved Site Measurements
    var showMeasurementDropdown by remember { mutableStateOf(false) }

    // Subtotal, Discount, Extra charges, GST
    var discountStr by remember { mutableStateOf("") }
    var extraStr by remember { mutableStateOf("") }
    var gstPerStr by remember { mutableStateOf("") } // percent

    val calculatedSqFt = remember(heightStr, widthStr, isFeet, isDouble) {
        val h = heightStr.toDoubleOrNull() ?: 0.0
        val w = widthStr.toDoubleOrNull() ?: 0.0
        val base = if (isFeet) (h * w) else ((h * w) / 144.0)
        if (isDouble) (base * 2.0) else base
    }

    val itemTotal = remember(calcType, qtyStr, rateStr, calculatedSqFt) {
        val r = rateStr.toDoubleOrNull() ?: 0.0
        if (calcType == "SQFT") {
            calculatedSqFt * r
        } else {
            val q = qtyStr.toDoubleOrNull() ?: 1.0
            q * r
        }
    }

    // Bill Math
    val subtotal = billItems.sumOf { it.totalAmount }
    val discount = discountStr.toDoubleOrNull() ?: 0.0
    val extra = extraStr.toDoubleOrNull() ?: 0.0
    val gstPercent = gstPerStr.toDoubleOrNull() ?: 0.0
    val gstAmount = (subtotal - discount + extra) * (gstPercent / 100.0)
    val grandTotal = (subtotal - discount + extra) + gstAmount

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "◀ પાછા જાઓ (Back)",
                    color = GeoPrimaryAction,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(vertical = 10.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "નવું બિલ જનરેટર",
                    fontWeight = FontWeight.Black,
                    color = GeoTextDark,
                    fontSize = 18.sp
                )
            }
            Text(
                text = "${customer.name} માટે વિગતવાર બિલ (uPVC Invoice Form)",
                color = GeoTextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            HorizontalDivider(color = GeoBorder)
        }

        // Section: Add New Line Item Form
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GeoBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GeoCardBg)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "બિલમાં આઈટમ ઉમેરો (Add New Cost Item)",
                        fontWeight = FontWeight.Bold,
                        color = GeoTextDark,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Item Description input
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("આઈટમ નામ / કામ ની વિગત (e.g. uPVC Kitchen Drawer)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoPrimaryAction,
                            unfocusedBorderColor = GeoBorder,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Calculation type toggler: Qty or SqFt
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .border(1.dp, GeoBorder, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (calcType == "SQFT") GeoPrimaryAction.copy(0.12f) else Color.Transparent)
                                .clickable { calcType = "SQFT" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "એરિયા મુજબ (SqFt-based)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (calcType == "SQFT") GeoPrimaryAction else GeoTextMuted
                            )
                        }
                        VerticalDivider(color = GeoBorder)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (calcType == "QTY") GeoPrimaryAction.copy(0.12f) else Color.Transparent)
                                .clickable { calcType = "QTY" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "નંગ મુજબ (Quantity-based)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (calcType == "QTY") GeoPrimaryAction else GeoTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (calcType == "SQFT") {
                        // Button to pull from saved Measurements
                        if (siteMeasurements.isNotEmpty()) {
                            Button(
                                onClick = { showMeasurementDropdown = !showMeasurementDropdown },
                                colors = ButtonDefaults.buttonColors(containerColor = GeoAccentLight),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.List, contentDescription = null, tint = GeoAccentDark, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("સાઈટ માપણી માંથી પસંદ કરો (${siteMeasurements.size})", fontSize = 11.sp, color = GeoAccentDark, fontWeight = FontWeight.Bold)
                            }

                            if (showMeasurementDropdown) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .border(1.dp, GeoBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(4.dp)) {
                                        siteMeasurements.forEach { m ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        itemName = m.title
                                                        heightStr = m.height.toString()
                                                        widthStr = m.width.toString()
                                                        isFeet = m.isFeet
                                                        isDouble = m.isDouble
                                                        showMeasurementDropdown = false
                                                    }
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(m.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GeoTextDark)
                                                    Text("માપ: ${m.height} x ${m.width} ${if (m.isFeet) "ફૂટ" else "ઇંચ"}", fontSize = 10.sp, color = GeoTextMuted)
                                                }
                                                Text("%.2f SqFt".format(m.sqFt), fontWeight = FontWeight.Black, color = GeoPrimaryAction, fontSize = 12.sp)
                                            }
                                            HorizontalDivider(color = GeoBorder.copy(alpha = 0.3f))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Manual height / width fields
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = heightStr,
                                onValueChange = { heightStr = it },
                                label = { Text("ઊંચાઈ (Height)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GeoPrimaryAction,
                                    unfocusedBorderColor = GeoBorder,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = widthStr,
                                onValueChange = { widthStr = it },
                                label = { Text("પહોળાઈ (Width)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GeoPrimaryAction,
                                    unfocusedBorderColor = GeoBorder,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Inches vs Feet toggle & Double measurements check
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = !isFeet,
                                    onClick = { isFeet = false },
                                    colors = RadioButtonDefaults.colors(selectedColor = GeoPrimaryAction),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text("ઇંચ", fontSize = 10.sp, color = GeoTextDark, modifier = Modifier.padding(start = 2.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                RadioButton(
                                    selected = isFeet,
                                    onClick = { isFeet = true },
                                    colors = RadioButtonDefaults.colors(selectedColor = GeoPrimaryAction),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text("ફૂટ", fontSize = 10.sp, color = GeoTextDark, modifier = Modifier.padding(start = 2.dp))
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { isDouble = !isDouble }
                            ) {
                                Checkbox(
                                    checked = isDouble,
                                    onCheckedChange = { isDouble = it },
                                    colors = CheckboxDefaults.colors(checkedColor = GeoPrimaryAction),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text("ડબલ માપ (x2)", fontSize = 10.sp, color = GeoTextDark, modifier = Modifier.padding(start = 2.dp))
                            }
                        }

                        if (heightStr.toDoubleOrNull() != null && widthStr.toDoubleOrNull() != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ગણેલો એરિયા: %.2f SqFt".format(calculatedSqFt),
                                color = GeoPrimaryAction,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    } else {
                        // QTY-based fields
                        OutlinedTextField(
                            value = qtyStr,
                            onValueChange = { qtyStr = it },
                            label = { Text("જથ્થો / નંગ (Quantity)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GeoPrimaryAction,
                                unfocusedBorderColor = GeoBorder,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Rate input
                    OutlinedTextField(
                        value = rateStr,
                        onValueChange = { rateStr = it },
                        label = { Text(if (calcType == "SQFT") "ભાવ પ્રતિ ચો.ફૂટ (Rate per SqFt ₹)" else "ભાવ પ્રતિ નંગ (Rate per piece ₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoPrimaryAction,
                            unfocusedBorderColor = GeoBorder,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live calculation box for new item
                    if (itemTotal > 0.0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SuccessGreen.copy(0.08f), RoundedCornerShape(10.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "આઈટમ સરવાળો: ₹${"%,.2f".format(itemTotal)}",
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Button to ADD
                    Button(
                        onClick = {
                            if (itemName.isNotBlank() && itemTotal > 0.0) {
                                val item = DraftBillItem(
                                    description = itemName,
                                    calculationType = calcType,
                                    height = heightStr.toDoubleOrNull() ?: 0.0,
                                    width = widthStr.toDoubleOrNull() ?: 0.0,
                                    isFeet = isFeet,
                                    qty = qtyStr.toDoubleOrNull() ?: 1.0,
                                    rate = rateStr.toDoubleOrNull() ?: 0.0,
                                    isDouble = isDouble,
                                    calculatedSqFt = if (calcType == "SQFT") calculatedSqFt else 0.0,
                                    totalAmount = itemTotal
                                )
                                billItems = billItems + item
                                
                                // Reset fields
                                itemName = ""
                                heightStr = ""
                                widthStr = ""
                                qtyStr = "1"
                                rateStr = ""
                                isDouble = false
                            }
                        },
                        enabled = itemName.isNotBlank() && rateStr.toDoubleOrNull() != null && rateStr.toDoubleOrNull()!! > 0.0,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryAction),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("બિલમાં ઉમેરો (Add Item to List)")
                    }
                }
            }
        }

        // Section: List of Added Items
        if (billItems.isNotEmpty()) {
            item {
                Text(
                    text = "ઉમેરેલી આઈટમ લિસ્ટ (${billItems.size})",
                    fontWeight = FontWeight.Bold,
                    color = GeoTextDark,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(billItems) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GeoBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.description,
                                fontWeight = FontWeight.Bold,
                                color = GeoTextDark,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            
                            val detailsText = if (item.calculationType == "SQFT") {
                                val doubleInd = if (item.isDouble) " (ડબલ)" else ""
                                "એરિયા: %.2f SqFt @ ₹%.0f/SqFt$doubleInd".format(item.calculatedSqFt, item.rate)
                            } else {
                                "જથ્થો: %.0f નંગ @ ₹%.0f/નંગ".format(item.qty, item.rate)
                            }
                            Text(detailsText, fontSize = 11.sp, color = GeoTextMuted)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "₹${"%,.0f".format(item.totalAmount)}",
                                fontWeight = FontWeight.Bold,
                                color = GeoPrimaryAction,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            IconButton(
                                onClick = { billItems = billItems.filter { it.id != item.id } },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = DangerRed.copy(0.6f), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // Section: Extra charges, Discount and GST fields
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GeoBorder, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("કટોતી અને વધારાના ચાર્જીસ (Discount & Taxes)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GeoTextDark)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = discountStr,
                                onValueChange = { discountStr = it },
                                label = { Text("ડિસ્કાઉન્ટ (Discount ₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GeoPrimaryAction,
                                    unfocusedBorderColor = GeoBorder
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = extraStr,
                                onValueChange = { extraStr = it },
                                label = { Text("મજૂરી/ભાડું (Extra ₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GeoPrimaryAction,
                                    unfocusedBorderColor = GeoBorder
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = gstPerStr,
                            onValueChange = { gstPerStr = it },
                            label = { Text("જીએસટી ટકા (GST Percent % e.g. 18)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GeoPrimaryAction,
                                    unfocusedBorderColor = GeoBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Grand Summary Banner Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GeoPrimaryAction)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "કુલ બિલ સરવાળો (SUBTOTAL: ₹${"%,.0f".format(subtotal)})",
                            color = Color.White.copy(alpha = 0.82f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        
                        if (discount > 0.0 || extra > 0.0 || gstPercent > 0.0) {
                            Text(
                                text = "ડિસ્કાઉન્ટ: -₹%.0f | ચાર્જીસ: +₹%.0f | GST (%.0f%%): +₹%.0f".format(discount, extra, gstPercent, gstAmount),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "₹${"%,.0f".format(grandTotal)}",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Button 1: Save to Ledger
                            Button(
                                onClick = {
                                    // Generate a detailed digest notes text
                                    val sbNotes = java.lang.StringBuilder()
                                    sbNotes.append("🧾 બિલ જનરેટ કર્યું: ")
                                    billItems.forEachIndexed { idx, itm ->
                                        if (idx > 0) sbNotes.append(", ")
                                        val shortDetails = if (itm.calculationType == "SQFT") "(${itm.calculatedSqFt.toInt()}SqFt)" else "(${itm.qty.toInt()}નંગ)"
                                        sbNotes.append("${itm.description}$shortDetails")
                                    }
                                    if (discount > 0) sbNotes.append(" | કટોતી: -₹${discount.toInt()}")
                                    if (extra > 0) sbNotes.append(" | એક્સ્ટ્રા: +₹${extra.toInt()}")
                                    if (gstAmount > 0) sbNotes.append(" | GST: +₹${gstAmount.toInt()}")

                                    viewModel.addKhataEntry(
                                        customerId = customer.id,
                                        amount = grandTotal,
                                        type = "DEBIT",
                                        note = sbNotes.toString()
                                    )
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = GeoPrimaryAction),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = GeoPrimaryAction)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("સેવ (Save to Ledger)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Button 2: WhatsApp invoice
                            Button(
                                onClick = {
                                    val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                                    val sb = java.lang.StringBuilder()
                                    sb.append("=========================================\n")
                                    sb.append("🧾 *uPVC CUPBOARD SYSTEM - INVOICE/બિલ*\n")
                                    sb.append("ગ્રાહક (Customer): *${customer.name}*\n")
                                    if (customer.phone.isNotBlank()) sb.append("મોબાઈલ: ${customer.phone}\n")
                                    sb.append("બિલ તારીખ (Date): $dateStr\n")
                                    sb.append("=========================================\n\n")

                                    sb.append("*આઈટમ વિગતવાર લિસ્ટ (Line Items):*\n")
                                    billItems.forEachIndexed { i, itm ->
                                        val calcText = if (itm.calculationType == "SQFT") {
                                            "  - માપ: ${"%.2f".format(itm.calculatedSqFt)} SqFt @ ₹${itm.rate.toInt()}/SqFt"
                                        } else {
                                            "  - માપ: ${itm.qty.toInt()} નંગ @ ₹${itm.rate.toInt()}/નંગ"
                                        }
                                        sb.append("${i + 1}. *${itm.description}*\n")
                                        sb.append("$calcText\n")
                                        sb.append("    👉 સરવાળો: *₹${"%,.0f".format(itm.totalAmount)}*\n\n")
                                    }

                                    sb.append("-----------------------------------------\n")
                                    sb.append("પેટા સરવાળો (Subtotal): ₹${"%,.0f".format(subtotal)}\n")
                                    if (discount > 0) sb.append("કટોતી (Discount): -₹${"%,.0f".format(discount)}\n")
                                    if (extra > 0) sb.append("વધારાના બીજા ચાર્જ: +₹${"%,.0f".format(extra)}\n")
                                    if (gstPercent > 0) sb.append("જીએસટી (GST ${gstPercent.toInt()}%): +₹${"%,.0f".format(gstAmount)}\n")
                                    sb.append("-----------------------------------------\n")
                                    sb.append("🔥 *કુલ ચૂકવવાપાત્ર રકમ (GRAND TOTAL): ₹${"%,.0f".format(grandTotal)}*\n")
                                    sb.append("=========================================\n")
                                    sb.append("બિલ ક્રિએટ બાય: uPVC Stock Tracker & Invoice Generator 🚀\n")
                                    sb.append("તમારા ઓર્ડર બદલ આપનો ખુબ ખુબ આભાર! 🙏")

                                    val textToShare = sb.toString()
                                    val phoneNumClean = customer.phone.filter { it.isDigit() }
                                    val uriString = if (phoneNumClean.isNotBlank()) {
                                        val formattedPhone = if (phoneNumClean.length == 10) "91$phoneNumClean" else phoneNumClean
                                        "https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(textToShare)}"
                                    } else {
                                        "https://api.whatsapp.com/send?text=${Uri.encode(textToShare)}"
                                    }

                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val genericIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, textToShare)
                                        }
                                        context.startActivity(Intent.createChooser(genericIntent, "મોકલવા માટે એપ પસંદ કરો"))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(0.9f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("વ્હોટ્સએપ કરો (WhatsApp)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        } else {
            // Empty state for bill items
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "કોઈ આઈટમ ઉમેરેલી નથી.\nઉપર વિગત અને કદ/નંગ ભરી બિલમાં એડ કરો.",
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// ==============================================================
// 8. SUPPLIER MANAGEMENT TAB & SCREENS
// ==============================================================

@Composable
fun SupplierTabContent(viewModel: InventoryViewModel) {
    var selectedSupplier by remember { mutableStateOf<Supplier?>(null) }
    
    if (selectedSupplier != null) {
        SupplierDetailScreen(
            viewModel = viewModel,
            supplier = selectedSupplier!!,
            onBack = { selectedSupplier = null }
        )
    } else {
        SupplierListScreen(
            viewModel = viewModel,
            onSupplierClick = { selectedSupplier = it }
        )
    }
}

@Composable
fun SupplierListScreen(
    viewModel: InventoryViewModel,
    onSupplierClick: (Supplier) -> Unit
) {
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredSuppliers = remember(suppliers, searchQuery) {
        suppliers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery, ignoreCase = true) ||
            it.address.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Heading
            Text(
                text = "સપ્લાયર લિસ્ટની યાદી (Suppliers List)",
                fontWeight = FontWeight.Black,
                color = GeoTextDark,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("સપ્લાયર નું નામ અથવા ફોન નંબર શોધો...", color = GeoTextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GeoTextMuted) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Search", tint = GeoTextMuted)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GeoPrimaryAction,
                    unfocusedBorderColor = GeoBorder,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )

            if (filteredSuppliers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(50.dp),
                            tint = GeoTextMuted.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "કોઈ સપ્લાયર મળ્યા નથી." else "હજુ સુધી કોઈ સપ્લાયર ઉમેરેલ નથી.",
                            fontSize = 14.sp,
                            color = GeoTextMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryAction)
                        ) {
                            Text("+ સપ્લાયર ઉમેરો (Add Supplier)", color = Color.White)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredSuppliers) { supplier ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GeoBorder, RoundedCornerShape(16.dp))
                                .clickable { onSupplierClick(supplier) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = supplier.name,
                                        fontWeight = FontWeight.Bold,
                                        color = GeoTextDark,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = null,
                                            tint = GeoTextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = supplier.phone,
                                            fontSize = 13.sp,
                                            color = GeoTextMuted
                                        )
                                    }
                                    if (supplier.address.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = GeoTextMuted,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = supplier.address,
                                                fontSize = 12.sp,
                                                color = GeoTextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(GeoBackground, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = "Details",
                                        tint = GeoPrimaryAction,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = GeoPrimaryAction,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .testTag("add_supplier_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Supplier")
        }
    }

    if (showAddDialog) {
        AddSupplierDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, address ->
                viewModel.addSupplier(name, phone, address)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddSupplierDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, address: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "નવો સપ્લાયર ઉમેરો (Add Supplier)",
                fontWeight = FontWeight.Bold,
                color = GeoTextDark,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("સપ્લાયર નું નામ (Name)*") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeoPrimaryAction,
                        unfocusedBorderColor = GeoBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("મોબાઈલ નંબર (Phone)*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeoPrimaryAction,
                        unfocusedBorderColor = GeoBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("સરનામું (Address)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeoPrimaryAction,
                        unfocusedBorderColor = GeoBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && phone.isNotBlank()) onConfirm(name, phone, address) },
                enabled = name.isNotBlank() && phone.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryAction)
            ) {
                Text("સેવ કરો (Save)", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("રદ કરો (Cancel)", color = GeoTextMuted)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

@Composable
fun SupplierDetailScreen(
    viewModel: InventoryViewModel,
    supplier: Supplier,
    onBack: () -> Unit
) {
    val materialsState = remember(supplier.id) { viewModel.getMaterialsForSupplier(supplier.id) }
    val receiptsState = remember(supplier.id) { viewModel.getReceiptsForSupplier(supplier.id) }

    val materials by materialsState.collectAsStateWithLifecycle(initialValue = emptyList())
    val receipts by receiptsState.collectAsStateWithLifecycle(initialValue = emptyList())

    var activeSubTab by remember { mutableStateOf(0) } // 0: Materials, 1: Receipts
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddMaterialDialog by remember { mutableStateOf(false) }
    var showCreateReceiptDialog by remember { mutableStateOf(false) }
    var selectedReceiptForView by remember { mutableStateOf<SupplierReceipt?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Back Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.White, CircleShape)
                    .border(1.dp, GeoBorder, CircleShape)
                    .size(36.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GeoPrimaryAction, modifier = Modifier.size(18.dp))
            }

            Text(
                text = "સપ્લાયર વિગતો (Supplier Details)",
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = GeoTextDark
            )

            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier
                    .background(Color.White, CircleShape)
                    .border(1.dp, GeoBorder, CircleShape)
                    .size(36.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Supplier", tint = DangerRed, modifier = Modifier.size(18.dp))
            }
        }

        // Supplier Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, GeoBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = supplier.name,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = GeoPrimaryAction
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = GeoTextMuted, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = supplier.phone, fontSize = 13.sp, color = GeoTextDark)
                }
                if (supplier.address.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = GeoTextMuted, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = supplier.address, fontSize = 13.sp, color = GeoTextDark)
                    }
                }
            }
        }

        // Tab Row Switcher (Materials vs Receipts)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, GeoBorder, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Materials Tab Indicator
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activeSubTab == 0) GeoPrimaryAction else Color.Transparent)
                    .clickable { activeSubTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "મટિરિયલ (${materials.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (activeSubTab == 0) Color.White else GeoTextMuted
                )
            }

            // Receipts Tab Indicator
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activeSubTab == 1) GeoPrimaryAction else Color.Transparent)
                    .clickable { activeSubTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ખરીદી રસીદો (${receipts.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (activeSubTab == 1) Color.White else GeoTextMuted
                )
            }
        }

        // Sub-Tab Content Views
        Box(modifier = Modifier.weight(1f)) {
            if (activeSubTab == 0) {
                // MATERIAL SUB TAB
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "મંજૂર કરેલ સામગ્રીઓ (Supplied Materials)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = GeoTextDark
                        )

                        TextButton(
                            onClick = { showAddMaterialDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = GeoPrimaryAction)
                        ) {
                            Text("+ નવું ઉમેરો (Add)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (materials.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "આ સપ્લાયર પાસે હજુ કોઈ મટિરિયલ રજીસ્ટર નથી.\nનવું મટિરિયલ એડ કરવા ઉપર ક્લિક કરો.",
                                fontSize = 12.sp,
                                color = GeoTextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 40.dp)
                        ) {
                            items(materials) { mat ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, GeoBorder, RoundedCornerShape(14.dp)),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = mat.name,
                                                fontWeight = FontWeight.Bold,
                                                color = GeoTextDark,
                                                fontSize = 14.sp
                                            )
                                            if (mat.sku.isNotEmpty()) {
                                                Text(
                                                    text = "Specification/Color: ${mat.sku}",
                                                    fontSize = 11.sp,
                                                    color = GeoTextMuted
                                                )
                                            }
                                            Text(
                                                text = "ભાવ (Supply Price): ₹${"%,.2f".format(mat.unitPrice)} / ${mat.unit}",
                                                fontSize = 12.sp,
                                                color = GeoPrimaryAction,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (mat.linkedInventoryItemId != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(top = 4.dp)
                                                        .background(GeoAccentLight, RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GeoAccentDark, modifier = Modifier.size(10.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("મુખ્ય સ્ટોક સાથે લિંક છે (Connected)", fontSize = 9.sp, color = GeoAccentDark, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteSupplierMaterial(mat.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = DangerRed.copy(0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // RECEIPTS SUB TAB
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ખરીદ કરેલ બિલોની હિસ્ટ્રી (Purchase Receipts)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = GeoTextDark
                        )

                        Button(
                            onClick = { showCreateReceiptDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryAction),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("+ નવી રસીદ (Create)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    if (receipts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "હજુ સુધી ખરીદી ની કોઈ રસીદ એડ કરેલ નથી.\nઉપર બટન દબાવી બિલ એન્ટ્રી કરો.",
                                fontSize = 12.sp,
                                color = GeoTextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 40.dp)
                        ) {
                            items(receipts) { receipt ->
                                val format = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
                                val dStr = format.format(Date(receipt.timestamp))

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, GeoBorder, RoundedCornerShape(14.dp))
                                        .clickable { selectedReceiptForView = receipt },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "બિલ #: " + receipt.receiptNumber,
                                                fontWeight = FontWeight.Bold,
                                                color = GeoTextDark,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = dStr,
                                                color = GeoTextMuted,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("કુલ (Total): ₹${"%,.0f".format(receipt.totalAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GeoTextDark)
                                                Text("ચૂકવેલ: ₹${"%,.0f".format(receipt.amountPaid)}", fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                                                if (receipt.amountDue > 0) {
                                                    Text("બાકી (Due): ₹${"%,.0f".format(receipt.amountDue)}", fontSize = 11.sp, color = DangerRed, fontWeight = FontWeight.Black)
                                                } else {
                                                    Text("ચૂકતે (Paid Fully) ✅", fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .background(GeoAccentLight, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                            ) {
                                                Text("વિગતો જુઓ (View)", fontSize = 10.sp, color = GeoAccentDark, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Suppliers deletion trigger Alert Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("સપ્લાયર ડિલીટ કરવો છે?", fontWeight = FontWeight.Bold, color = GeoTextDark) },
            text = { Text("આ કરવાથી સપ્લાયર '${supplier.name}', તેમના મટિરિયલ અને તમામ ખરીદી બિલો હંમેશા માટે લિસ્ટમાંથી ડિલીટ થઇ જશે.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSupplier(supplier.id)
                        showDeleteConfirm = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("હા, ડિલીટ કરો", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("ના", color = GeoTextMuted)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }

    if (showAddMaterialDialog) {
        AddSupplierMaterialDialog(
            viewModel = viewModel,
            supplierId = supplier.id,
            onDismiss = { showAddMaterialDialog = false }
        )
    }

    if (showCreateReceiptDialog) {
        CreateSupplierReceiptDialog(
            viewModel = viewModel,
            supplierId = supplier.id,
            materials = materials,
            onDismiss = { showCreateReceiptDialog = false }
        )
    }

    if (selectedReceiptForView != null) {
        ViewReceiptDetailsDialog(
            receipt = selectedReceiptForView!!,
            onDismiss = { selectedReceiptForView = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSupplierMaterialDialog(
    viewModel: InventoryViewModel,
    supplierId: Int,
    onDismiss: () -> Unit
) {
    val localItems by viewModel.inventoryItems.collectAsStateWithLifecycle()
    
    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    
    // Linking state
    var isLinked by remember { mutableStateOf(false) }
    var selectedItemIndex by remember { mutableStateOf(-1) }
    var showDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("નવું મટિરિયલ ઉમેરો (Add Material)", fontWeight = FontWeight.Bold, color = GeoTextDark, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("મટિરિયલ નામ (Name)*") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GeoPrimaryAction, unfocusedBorderColor = GeoBorder),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("વિગત / કલર / કોડ (Specification/SKU)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GeoPrimaryAction, unfocusedBorderColor = GeoBorder),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("ભાવ (Supply Price)*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GeoPrimaryAction, unfocusedBorderColor = GeoBorder),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("એકમ (Unit)*") },
                        placeholder = { Text("e.g. Sheets, Pieces") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GeoPrimaryAction, unfocusedBorderColor = GeoBorder),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Checkbox to Link main inventory stock
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isLinked = !isLinked }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = isLinked,
                        onCheckedChange = { isLinked = it },
                        colors = CheckboxDefaults.colors(checkedColor = GeoPrimaryAction)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("મુખ્ય સ્ટોક સાથે લિંક કરો", fontSize = 12.sp, color = GeoTextDark, fontWeight = FontWeight.Bold)
                }

                if (isLinked) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { showDropdown = true },
                            border = BorderStroke(1.dp, GeoBorder),
                            colors = CardDefaults.cardColors(containerColor = GeoBackground),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (selectedItemIndex >= 0 && selectedItemIndex < localItems.size) {
                                    val match = localItems[selectedItemIndex]
                                    "${match.name} (${match.specification})"
                                } else {
                                    "લિંક કરવા આઇટમ પસંદ કરો ⬇️"
                                },
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp),
                                color = GeoTextDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        DropdownMenu(
                            expanded = showDropdown,
                            onDismissRequest = { showDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            localItems.forEachIndexed { idx, it ->
                                DropdownMenuItem(
                                    text = { Text("${it.name} (${it.specification}) [Stock: ${it.quantity} ${it.unit}]", fontSize = 12.sp) },
                                    onClick = {
                                        selectedItemIndex = idx
                                        showDropdown = false
                                        // Auto-populate unit from main item
                                        if (unit.isBlank()) {
                                            unit = it.unit
                                        }
                                        if (name.isBlank()) {
                                            name = it.name
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val price = priceStr.toDoubleOrNull() ?: 0.0
            Button(
                onClick = {
                    val linkedId = if (isLinked && selectedItemIndex >= 0) localItems[selectedItemIndex].id else null
                    viewModel.addSupplierMaterial(supplierId, name, sku, unit, price, linkedId)
                    onDismiss()
                },
                enabled = name.isNotBlank() && unit.isNotBlank() && price > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryAction)
            ) {
                Text("ઉમેરો (Add)", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("રદ કરો (Cancel)", color = GeoTextMuted)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSupplierReceiptDialog(
    viewModel: InventoryViewModel,
    supplierId: Int,
    materials: List<SupplierMaterial>,
    onDismiss: () -> Unit
) {
    var receiptNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var amountPaidStr by remember { mutableStateOf("") }
    var restockStock by remember { mutableStateOf(true) }

    // Draft items being purchased
    var draftItems by remember { mutableStateOf(listOf<ReceiptItem>()) }

    // Inputs for adding a material to list
    var selectedMatIndex by remember { mutableStateOf(-1) }
    var quantityStr by remember { mutableStateOf("1") }
    var priceOverrideStr by remember { mutableStateOf("") }
    var showMatSelector by remember { mutableStateOf(false) }

    val calculatedSubtotal = remember(draftItems) {
        draftItems.sumOf { it.total }
    }

    val calculatedDue = remember(calculatedSubtotal, amountPaidStr) {
        val paid = amountPaidStr.toDoubleOrNull() ?: 0.0
        val diff = calculatedSubtotal - paid
        if (diff < 0.0) 0.0 else diff
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("નવી ખરીદી રસીદ બનાવો (Create Receipt)", fontWeight = FontWeight.Bold, color = GeoTextDark, fontSize = 16.sp) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // Invoice Number
                    OutlinedTextField(
                        value = receiptNumber,
                        onValueChange = { receiptNumber = it },
                        label = { Text("ખરીદી બિલ / ઇનવોઇસ નંબર (#)*") },
                        placeholder = { Text("e.g. IN-2983") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GeoPrimaryAction, unfocusedBorderColor = GeoBorder),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    // Add Material Section heading
                    Text("રસીદમાં આઈટમ ઉમેરો (Select Materials)", fontWeight = FontWeight.Bold, color = GeoPrimaryAction, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Selector Card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GeoBackground, RoundedCornerShape(12.dp))
                            .border(1.dp, GeoBorder, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                onClick = { showMatSelector = true },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, GeoBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (selectedMatIndex >= 0 && selectedMatIndex < materials.size) {
                                        val mat = materials[selectedMatIndex]
                                        "${mat.name} (₹${mat.unitPrice} / ${mat.unit})"
                                    } else {
                                        "ખરીદ મટિરિયલ પસંદ કરો ⬇️"
                                    },
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(10.dp),
                                    color = GeoTextDark
                                )
                            }

                            DropdownMenu(
                                expanded = showMatSelector,
                                onDismissRequest = { showMatSelector = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                materials.forEachIndexed { index, mat ->
                                    DropdownMenuItem(
                                        text = { Text("${mat.name} (₹${mat.unitPrice} / ${mat.unit})", fontSize = 11.sp) },
                                        onClick = {
                                            selectedMatIndex = index
                                            priceOverrideStr = mat.unitPrice.toString()
                                            showMatSelector = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = quantityStr,
                                onValueChange = { quantityStr = it },
                                label = { Text("નંગ/જથ્થો (Qty)*") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GeoPrimaryAction, unfocusedBorderColor = GeoBorder),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = priceOverrideStr,
                                onValueChange = { priceOverrideStr = it },
                                label = { Text("ખરીદ કિંમત (Rate)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GeoPrimaryAction, unfocusedBorderColor = GeoBorder),
                                modifier = Modifier.weight(1.2f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Add to list Button
                        Button(
                            onClick = {
                                if (selectedMatIndex >= 0 && selectedMatIndex < materials.size) {
                                    val mat = materials[selectedMatIndex]
                                    val qty = quantityStr.toDoubleOrNull() ?: 1.0
                                    val pr = priceOverrideStr.toDoubleOrNull() ?: mat.unitPrice
                                    val item = ReceiptItem(
                                        materialId = mat.id,
                                        materialName = mat.name,
                                        quantity = qty,
                                        unitPrice = pr,
                                        total = qty * pr
                                    )
                                    draftItems = draftItems + item
                                    // Reset input values
                                    selectedMatIndex = -1
                                    quantityStr = "1"
                                    priceOverrideStr = ""
                                }
                            },
                            enabled = selectedMatIndex >= 0 && (quantityStr.toDoubleOrNull() ?: 0.0) > 0.0,
                            colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryActive),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("બિલમાં ઉમેરો (Add to Draft List)", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                // Table Items List
                if (draftItems.isNotEmpty()) {
                    item {
                        Text("બિલની આઈટમો (Draft Items Table):", fontWeight = FontWeight.Bold, color = GeoTextDark, fontSize = 11.sp)
                    }

                    items(draftItems) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GeoBorder, RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.materialName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GeoTextDark)
                                    Text("${item.quantity} x ₹${"%,.2f".format(item.unitPrice)}", fontSize = 10.sp, color = GeoTextMuted)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "₹${"%,.0f".format(item.total)}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = GeoPrimaryAction,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    IconButton(
                                        onClick = { draftItems = draftItems.filter { it != item } },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Totals
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = GeoPrimaryAction),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                 ) {
                                    Text("કુલ બિલ (GRAND TOTAL):", color = Color.White.copy(0.85f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("₹${"%,.2f".format(calculatedSubtotal)}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }

                // Financial Inputs
                item {
                    OutlinedTextField(
                        value = amountPaidStr,
                        onValueChange = { amountPaidStr = it },
                        label = { Text("ચૂકવેલ રકમ (Amount Paid - ₹)") },
                        placeholder = { Text("e.g. 5000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GeoPrimaryAction, unfocusedBorderColor = GeoBorder),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GeoBackground, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("બાકી બોલતી રકમ (Due):", color = GeoTextDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("₹${"%,.2f".format(calculatedDue)}", color = if (calculatedDue > 0) DangerRed else SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }

                item {
                    // Restock option checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { restockStock = !restockStock }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = restockStock,
                            onCheckedChange = { restockStock = it },
                            colors = CheckboxDefaults.colors(checkedColor = GeoPrimaryAction)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ખરીદેલ સામગ્રીઓ ઓટો-સ્ટોકમાં જમા કરો ✅", fontSize = 11.sp, color = GeoTextDark, fontWeight = FontWeight.Bold)
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("બિલ નોંધ / શેરો (Notes)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GeoPrimaryAction, unfocusedBorderColor = GeoBorder),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val converters = com.example.data.Converters()
                    val js = converters.receiptItemsToString(draftItems)
                    val paid = amountPaidStr.toDoubleOrNull() ?: 0.0
                    viewModel.addSupplierReceipt(
                        supplierId = supplierId,
                        receiptNumber = receiptNumber,
                        itemsJson = js,
                        totalAmount = calculatedSubtotal,
                        amountPaid = paid,
                        amountDue = calculatedDue,
                        notes = notes,
                        restockStock = restockStock
                    )
                    onDismiss()
                },
                enabled = receiptNumber.isNotBlank() && draftItems.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryAction)
            ) {
                Text("રસીદ સેવ કરો (Save Receipt)", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("બહાર નીકળો (Cancel)", color = GeoTextMuted)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

@Composable
fun ViewReceiptDetailsDialog(
    receipt: SupplierReceipt,
    onDismiss: () -> Unit
) {
    val converters = remember { com.example.data.Converters() }
    val items = remember(receipt.itemsJson) {
        converters.stringToReceiptItems(receipt.itemsJson) ?: emptyList()
    }
    val formatter = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
    val dateStr = formatter.format(Date(receipt.timestamp))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("બિલ વિગત (Receipt Info)", fontWeight = FontWeight.Black, color = GeoTextDark, fontSize = 16.sp)
                Text("બિલ #: ${receipt.receiptNumber}", fontSize = 12.sp, color = GeoPrimaryAction, fontWeight = FontWeight.Bold)
                Text("તારીખ: $dateStr", fontSize = 10.sp, color = GeoTextMuted)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Divider(color = GeoBorder)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("ખરીદેલ સામાન યાદી (Items Purchased):", fontWeight = FontWeight.Bold, color = GeoPrimaryAction, fontSize = 11.sp)
                }

                items(items) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GeoBackground),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.materialName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GeoTextDark)
                                Text("${item.quantity} units x ₹${"%,.2f".format(item.unitPrice)}", fontSize = 10.sp, color = GeoTextMuted)
                            }
                            Text(
                                "₹${"%,.0f".format(item.total)}",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = GeoPrimaryAction
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Divider(color = GeoBorder)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
                            .border(1.dp, GeoBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("કુલ બિલ સરવાળો (Total Amount):", fontSize = 11.sp, color = GeoTextMuted)
                            Text("₹${"%,.2f".format(receipt.totalAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GeoTextDark)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ચૂકવેલ રકમ (Amount Paid):", fontSize = 11.sp, color = GeoTextMuted)
                            Text("₹${"%,.2f".format(receipt.amountPaid)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("બાકી રહેતી રકમ (Due):", fontSize = 11.sp, color = GeoTextMuted)
                            Text("₹${"%,.2f".format(receipt.amountDue)}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (receipt.amountDue > 0) DangerRed else SuccessGreen)
                        }
                    }
                }

                if (receipt.notes.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, GeoBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("બિલ ટિપ્પણી (Notes/Notes):", fontSize = 10.sp, color = GeoTextMuted, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(receipt.notes, fontSize = 11.sp, color = GeoTextDark)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GeoPrimaryAction)
            ) {
                Text("બંધ કરો (Close)", color = Color.White)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}
