package com.example.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.MarginCalculation
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MarginViewModel,
    modifier: Modifier = Modifier,
    onGetTempUri: () -> Uri
) {
    val context = LocalContext.current
    val calculations by viewModel.allCalculations.collectAsStateWithLifecycle()
    val totalCount = calculations.size

    // Pick visual media launcher (screenshot upload)
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.parseImage(context, uri)
        }
    }

    // Camera picture capture launcher
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri?.let { uri ->
                viewModel.parseImage(context, uri)
            }
        }
    }

    // Reset History confirmation dialog state
    var showResetDialog by remember { mutableStateOf(false) }

    // Educational / Info Dialog state
    var showInfoDialog by remember { mutableStateOf(false) }
    var showVersionDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!viewModel.isInputPanelOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                ) {
                    // Custom sleek navigation bar container matching the mockup HTML
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(CharcoalSurface, RoundedCornerShape(32.dp))
                            .border(1.dp, Color(0xFF27272A), RoundedCornerShape(32.dp))
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // Home Tab click area
                        val homeActive = !viewModel.isHistoryViewActive
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { viewModel.isHistoryViewActive = false }
                                .testTag("toggle_history_button")
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home",
                                tint = if (homeActive) NeonGreen else MutedText,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "HOME",
                                color = if (homeActive) NeonGreen else MutedText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // History Tab click area
                        val historyActive = viewModel.isHistoryViewActive
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { viewModel.isHistoryViewActive = true }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History Logs",
                                tint = if (historyActive) NeonGreen else MutedText,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "HISTORY",
                                color = if (historyActive) NeonGreen else MutedText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // FAB positioned on the right overlapping beautifully
                    FloatingActionButton(
                        onClick = {
                            viewModel.resetInputs()
                            viewModel.isInputPanelOpen = true
                        },
                        containerColor = NeonGreen,
                        contentColor = Color.Black,
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = (-8).dp, y = (-20).dp)
                            .size(64.dp)
                            .testTag("add_calculation_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add New Calculation",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CharcoalBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // 1. Custom Sleek Header (Matches HTML "Sleek Interface" Header)
                val dateFormat = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
                val currentDateStr = remember { dateFormat.format(Date()) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Margin Level",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Text(
                            text = currentDateStr,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MutedText,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    // Row of Action Buttons (Settings Menu and Info Button)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(CharcoalSurface, CircleShape)
                                    .border(1.dp, Color(0xFF27272A), CircleShape)
                                    .clip(CircleShape)
                                    .clickable { showMenu = true }
                                    .testTag("menu_icon_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Show Menu",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(CharcoalSurface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("App Version", color = Color.White, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        showMenu = false
                                        showVersionDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Reset History", color = Color.White, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        showMenu = false
                                        showResetDialog = true
                                    }
                                )
                            }
                        }

                        // Circular Info Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(CharcoalSurface, CircleShape)
                                .border(1.dp, Color(0xFF27272A), CircleShape)
                                .clip(CircleShape)
                                .clickable { showInfoDialog = true }
                                .testTag("info_icon_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Show Education Info",
                                tint = NeonGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // 2. Beautiful Sleek Counter Card with Thick Left Border (Matches HTML mockup)
                HeaderMetricPanel(totalCalculations = totalCount)

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Dynamic lists based on active view state
                if (viewModel.isHistoryViewActive) {
                    HistoryView(
                        calculations = calculations,
                        onItemClick = { item ->
                            viewModel.selectHistoricItem(item)
                        },
                        onDeleteClick = { item ->
                            viewModel.deleteItem(item)
                        },
                        showResetDialog = { showResetDialog = true }
                    )
                } else {
                    DashboardView(
                        calculations = calculations,
                        onItemClick = { item ->
                            viewModel.selectHistoricItem(item)
                        },
                        onNewCaptureClick = {
                            viewModel.resetInputs()
                            viewModel.isInputPanelOpen = true
                        }
                    )
                }
            }

            // Input / Upload Overlay Panel (when open)
            AnimatedVisibility(
                visible = viewModel.isInputPanelOpen,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring()) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring()) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                InputPanelScreen(
                    viewModel = viewModel,
                    onUploadScreenshot = {
                        pickMediaLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    onTakeCameraPhoto = {
                        val uri = onGetTempUri()
                        cameraUri = uri
                        takePictureLauncher.launch(uri)
                    },
                    onClose = {
                        viewModel.isInputPanelOpen = false
                        viewModel.resetInputs()
                    }
                )
            }

            // Educational info dialog about margin levels
            if (showInfoDialog) {
                AlertDialog(
                    onDismissRequest = { showInfoDialog = false },
                    title = {
                        Text(
                            "What is Margin Level?",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Margin Level is a key metric representing the health of your trading account. It is computed as:",
                                color = MutedText,
                                fontSize = 13.sp
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CharcoalBg, RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "(Equity / Used Margin) × 100%",
                                    color = NeonGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                "⚠️ 100% Margin Call: Broker places account on alert. New trades cannot be opened.",
                                color = WarningYellow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "🚨 30% Stop Out: Automatic liquidation of active positions begins to protect your capital from dropping below zero.",
                                color = DangerRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showInfoDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                        ) {
                            Text("Got it", fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = CharcoalSurface,
                    textContentColor = WhiteText
                )
            }

            // Historical Detail Dialog
            viewModel.selectedHistoricItem?.let { item ->
                DetailDialog(
                    item = item,
                    onDismiss = { viewModel.selectHistoricItem(null) }
                )
            }

            // Reset Confirmation Dialog
            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = {
                        Text(
                            "Reset Calculation History?",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    text = {
                        Text(
                            "This will permanently wipe all your past CFD trade calculations. This action cannot be undone.",
                            color = MutedText
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.resetHistory()
                                showResetDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                        ) {
                            Text("Reset", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) {
                            Text("Cancel", color = NeonGreen)
                        }
                    },
                    containerColor = CharcoalSurface,
                    textContentColor = WhiteText
                )
            }

            // Version Information Dialog
            if (showVersionDialog) {
                val uriHandler = LocalUriHandler.current
                AlertDialog(
                    onDismissRequest = { showVersionDialog = false },
                    title = {
                        Text(
                            "App Information",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // ExaGuard Company Logo Image (Link to website on tap)
                            Image(
                                painter = painterResource(id = R.drawable.exaguard_logo),
                                contentDescription = "ExaGuard Logo",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .padding(4.dp)
                                    .clickable { uriHandler.openUri("https://exaguard.prasanti.com") },
                                contentScale = ContentScale.Fit
                            )
                            
                            Text(
                                "CFD Margin Level Calculator",
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            
                            Text(
                                "Version: v0.20.1",
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            
                            Text(
                                "A product of ExaGuard©",
                                color = NeonGreen,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )

                            // Media Links Row
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Web Icon/Link
                                IconButton(
                                    onClick = { uriHandler.openUri("https://exaguard.prasanti.com") },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = "Website",
                                        tint = NeonGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                // YouTube Icon/Link
                                IconButton(
                                    onClick = { uriHandler.openUri("https://youtube.com/@exaguardai") },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = "YouTube",
                                        tint = Color(0xFFFF0000), // YouTube Red
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Text(
                                "Calculates real-time margin break points (100% Margin Call and 30% Stop Out) with advanced OCR scanning for CFD and Forex trading.",
                                color = MutedText,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            
                            Divider(color = Color(0xFF27272A), thickness = 1.dp)

                            Text(
                                "Disclaimer: This application is for informational and simulation purposes only. Always refer to your broker's official trading platform for accurate and official Margin Level and margin requirements.",
                                color = MutedText,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 14.sp
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showVersionDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                        ) {
                            Text("OK", fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = CharcoalSurface,
                    textContentColor = WhiteText
                )
            }
        }
    }
}

@Composable
fun HeaderMetricPanel(
    totalCalculations: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF27272A))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thick left border segment styled in Electric Neon Green
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(84.dp)
                    .background(NeonGreen)
            )
            
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LIFETIME REPORTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedText,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = "$totalCalculations",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonGreen,
                        modifier = Modifier.testTag("calculations_counter")
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Box(
                        modifier = Modifier
                            .background(NeonGreen.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            color = NeonGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Last synced 2m ago",
                        fontSize = 10.sp,
                        color = MutedText.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardView(
    calculations: List<MarginCalculation>,
    onItemClick: (MarginCalculation) -> Unit,
    onNewCaptureClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT CALCULATIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MutedText,
                letterSpacing = 1.sp
            )
        }

        if (calculations.isEmpty()) {
            EmptyStateView(onNewCaptureClick)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 120.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(calculations.take(5)) { item ->
                    CalculationItemRow(item = item, onClick = { onItemClick(item) }, onDelete = null)
                }
            }
        }
    }
}

@Composable
fun HistoryView(
    calculations: List<MarginCalculation>,
    onItemClick: (MarginCalculation) -> Unit,
    onDeleteClick: (MarginCalculation) -> Unit,
    showResetDialog: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "COMPLETE CALCULATION LOGS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MutedText,
                letterSpacing = 1.sp
            )
            if (calculations.isNotEmpty()) {
                Text(
                    text = "Reset History",
                    color = NeonGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(onClick = showResetDialog)
                        .testTag("reset_history_button")
                )
            }
        }

        if (calculations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No calculations recorded yet.",
                    color = MutedText,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 120.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(calculations) { item ->
                    CalculationItemRow(
                        item = item,
                        onClick = { onItemClick(item) },
                        onDelete = { onDeleteClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun CalculationItemRow(
    item: MarginCalculation,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val isBuy = item.positionType == "BUY"
    val dateString = remember(item.timestamp) {
        val formatter = SimpleDateFormat("HH:mm a", Locale.getDefault())
        formatter.format(Date(item.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("calculation_item_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF27272A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Symbol & Timestamp / Delete action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Screenshot Thumbnail if present
                    if (item.imageUri != null) {
                        AsyncImage(
                            model = item.imageUri,
                            contentDescription = "Trade Screenshot",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(CharcoalCard, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = "Calculated metrics",
                                tint = NeonGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = item.symbol,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )

                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isBuy) DarkGreen.copy(alpha = 0.3f) else DangerRed.copy(alpha = 0.2f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = item.positionType,
                                    color = if (isBuy) NeonGreen else DangerRed,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "Margin: ${formatMoney(item.margin)} | Open: ${formatPrice(item.openPrice)}",
                            color = MutedText,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }
 
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = dateString,
                        color = MutedText.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
 
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete item",
                                tint = DangerRed.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
 
            Spacer(modifier = Modifier.height(12.dp))
 
            // Dual Columns matching HTML list grid cols 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 100% Margin Call Breakpoint
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(CharcoalBg, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "100% Margin Level",
                        fontSize = 9.sp,
                        color = MutedText,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = formatPrice(item.price100),
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen,
                        fontSize = 13.sp
                    )
                }
 
                // 30% Stop Out Breakpoint
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(CharcoalBg, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "30% Margin Level",
                        fontSize = 9.sp,
                        color = MutedText,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = formatPrice(item.price30),
                        fontWeight = FontWeight.Bold,
                        color = WarningYellow,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    onNewCaptureClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(CharcoalSurface, CircleShape)
                .border(2.dp, NeonGreen.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = "Add Icon",
                tint = NeonGreen,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SCAN CFD SCREENSHOT",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Upload or take a screenshot of your trading panel to instantly extract metrics and compute margin stop-out prices.",
            fontSize = 13.sp,
            color = MutedText,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onNewCaptureClick,
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Analyze screenshot", fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputPanelScreen(
    viewModel: MarginViewModel,
    onUploadScreenshot: () -> Unit,
    onTakeCameraPhoto: () -> Unit,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "ANALYZE TRADE MARGIN",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                TextButton(onClick = { viewModel.resetInputs() }) {
                    Text("Clear", color = NeonGreen)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Row: Upload / Camera Buttons
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CAPTURE METHOD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedText,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onUploadScreenshot,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("upload_screenshot_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CharcoalCard,
                                contentColor = NeonGreen
                            ),
                            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Screenshot", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onTakeCameraPhoto,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("camera_photo_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CharcoalCard,
                                contentColor = NeonGreen
                            ),
                            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Camera", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Camera Photo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Error Message
            viewModel.errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, DangerRed)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = "Error", tint = DangerRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(error, color = Color.White, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Image Preview (if available)
            viewModel.selectedImageBitmap?.let { bitmap ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = bitmap,
                            contentDescription = "Selected screenshot preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("LOADED SCREENSHOT", color = NeonGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Live Calculated Breakpoints
            InteractiveBreakpointCard(
                price100 = viewModel.price100,
                price30 = viewModel.price30
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Active Positions Section (Accommodates multiple CFD trade positions)
            var showAddPositionDialog by remember { mutableStateOf(false) }
            if (showAddPositionDialog) {
                var symbol by remember { mutableStateOf("") }
                var type by remember { mutableStateOf("BUY") }
                var openPrice by remember { mutableStateOf("") }
                var currentPrice by remember { mutableStateOf("") }
                var pnl by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showAddPositionDialog = false },
                    title = { Text("Add Custom Position", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = symbol,
                                onValueChange = { symbol = it },
                                label = { Text("Symbol (e.g. EURUSD)") },
                                textStyle = TextStyle(color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonGreen,
                                    unfocusedBorderColor = Color(0xFF2C2C2C),
                                    focusedLabelColor = NeonGreen
                                )
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { type = "BUY" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (type == "BUY") NeonGreen else CharcoalCard,
                                        contentColor = if (type == "BUY") Color.Black else Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("BUY")
                                }
                                Button(
                                    onClick = { type = "SELL" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (type == "SELL") DangerRed else CharcoalCard,
                                        contentColor = if (type == "SELL") Color.Black else Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("SELL")
                                }
                            }
                            OutlinedTextField(
                                value = openPrice,
                                onValueChange = { openPrice = it },
                                label = { Text("Open Price") },
                                textStyle = TextStyle(color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonGreen,
                                    unfocusedBorderColor = Color(0xFF2C2C2C),
                                    focusedLabelColor = NeonGreen
                                )
                            )
                            OutlinedTextField(
                                value = currentPrice,
                                onValueChange = { currentPrice = it },
                                label = { Text("Current Price") },
                                textStyle = TextStyle(color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonGreen,
                                    unfocusedBorderColor = Color(0xFF2C2C2C),
                                    focusedLabelColor = NeonGreen
                                )
                            )
                            OutlinedTextField(
                                value = pnl,
                                onValueChange = { pnl = it },
                                label = { Text("Floating Position PnL") },
                                textStyle = TextStyle(color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonGreen,
                                    unfocusedBorderColor = Color(0xFF2C2C2C),
                                    focusedLabelColor = NeonGreen
                                )
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (symbol.isNotEmpty()) {
                                    viewModel.addManualPosition(
                                        com.example.api.ParsedPosition(
                                            symbol = symbol.uppercase(),
                                            positionType = type,
                                            openPrice = openPrice.toDoubleOrNull() ?: 0.0,
                                            currentPrice = currentPrice.toDoubleOrNull() ?: 0.0,
                                            positionPnl = pnl.toDoubleOrNull() ?: 0.0
                                        )
                                    )
                                    showAddPositionDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                        ) {
                            Text("Add", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddPositionDialog = false }) {
                            Text("Cancel", color = MutedText)
                        }
                    },
                    containerColor = CharcoalSurface
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFF27272A)), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVE TRADING POSITIONS (${viewModel.currentPositions.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "+ Add Position",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { showAddPositionDialog = true }
                                .testTag("add_manual_position_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (viewModel.currentPositions.isEmpty()) {
                        Text(
                            text = "No active positions. Upload a trading screenshot or tap '+ Add Position' to simulate manually.",
                            color = MutedText,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            viewModel.currentPositions.forEachIndexed { index, pos ->
                                val isSelected = viewModel.selectedPositionIndex == index
                                val isBuy = pos.positionType == "BUY"
                                val pnlVal = pos.positionPnl ?: 0.0

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectPosition(index) }
                                        .border(
                                            BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) NeonGreen else Color(0xFF2C2C2C)
                                            ),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) CharcoalCard else CharcoalSurface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = pos.symbol ?: "Asset",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 14.sp,
                                                    color = Color.White
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            if (isBuy) DarkGreen.copy(alpha = 0.3f) else DangerRed.copy(alpha = 0.2f),
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = pos.positionType ?: "BUY",
                                                        color = if (isBuy) NeonGreen else DangerRed,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Open: ${formatPrice(pos.openPrice)}  |  Live: ${formatPrice(pos.currentPrice)}",
                                                color = MutedText,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Text(
                                                text = formatMoney(pnlVal),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (pnlVal >= 0.0) NeonGreen else DangerRed,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )

                                            IconButton(
                                                onClick = { viewModel.removePosition(index) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Remove",
                                                    tint = DangerRed.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(16.dp)
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

            Spacer(modifier = Modifier.height(12.dp))

            // Validation Card
            val computedLevel = remember(viewModel.currentEquity, viewModel.currentMargin) {
                val eq = viewModel.currentEquity.toDoubleOrNull() ?: 0.0
                val marg = viewModel.currentMargin.toDoubleOrNull() ?: 0.0
                if (marg > 0.0) (eq / marg) * 100.0 else 0.0
            }
            ValidationCard(
                extractedLevel = viewModel.currentExtractedMarginLevel,
                computedLevel = computedLevel
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Price Simulation Card
            PriceSimulationCard(viewModel = viewModel)

            Spacer(modifier = Modifier.height(16.dp))

            // Editable Input Fields Header
            Text(
                text = "VERIFY & EDIT DETECTED VALUES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MutedText,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Selector for Position Type (BUY/SELL)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.currentPositionType = "BUY" },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("buy_selector_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (viewModel.currentPositionType == "BUY") DarkGreen else CharcoalSurface,
                        contentColor = if (viewModel.currentPositionType == "BUY") Color.White else MutedText
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (viewModel.currentPositionType == "BUY") NeonGreen else Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("BUY / LONG", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.currentPositionType = "SELL" },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("sell_selector_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (viewModel.currentPositionType == "SELL") DangerRed.copy(alpha = 0.3f) else CharcoalSurface,
                        contentColor = if (viewModel.currentPositionType == "SELL") Color.White else MutedText
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (viewModel.currentPositionType == "SELL") DangerRed else Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.TrendingDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SELL / SHORT", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Text Input Fields
            OutlinedTextField(
                value = viewModel.currentSymbol,
                onValueChange = { viewModel.currentSymbol = it },
                label = { Text("Asset / Symbol (e.g. EURUSD, XAUUSD)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonGreen,
                    unfocusedBorderColor = BorderColor,
                    focusedLabelColor = NeonGreen,
                    unfocusedLabelColor = MutedText,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("symbol_input"),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = viewModel.currentOpenPrice,
                    onValueChange = { viewModel.currentOpenPrice = it },
                    label = { Text("Open Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = BorderColor,
                        focusedLabelColor = NeonGreen,
                        unfocusedLabelColor = MutedText,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_price_input"),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = viewModel.currentCurrentPrice,
                    onValueChange = { viewModel.currentCurrentPrice = it },
                    label = { Text("Current Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = BorderColor,
                        focusedLabelColor = NeonGreen,
                        unfocusedLabelColor = MutedText,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("current_price_input"),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = viewModel.currentPositionPnl,
                onValueChange = { viewModel.currentPositionPnl = it },
                label = { Text("Position Profit / Loss ($)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonGreen,
                    unfocusedBorderColor = BorderColor,
                    focusedLabelColor = NeonGreen,
                    unfocusedLabelColor = MutedText,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pnl_input"),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = viewModel.currentEquity,
                    onValueChange = { viewModel.currentEquity = it },
                    label = { Text("Current Equity ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = BorderColor,
                        focusedLabelColor = NeonGreen,
                        unfocusedLabelColor = MutedText,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("equity_input"),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = viewModel.currentMargin,
                    onValueChange = { viewModel.currentMargin = it },
                    label = { Text("Used Margin ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = BorderColor,
                        focusedLabelColor = NeonGreen,
                        unfocusedLabelColor = MutedText,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("margin_input"),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Buttons
            Button(
                onClick = { viewModel.saveCalculation() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_calculation_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save to Log History", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(80.dp)) // space for scrolling
        }

        // Processing Loading HUD overlay
        if (viewModel.isLoading) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                    border = BorderStroke(1.dp, NeonGreen),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = NeonGreen,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "ANALYZING TRADE SCREENSHOT",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Gemini-3.5-Flash is processing OCR and extracting trade metrics...",
                            color = MutedText,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveBreakpointCard(
    price100: Double,
    price30: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, NeonGreen.copy(alpha = 0.3f)), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "CRITICAL MARGIN BREAKPOINTS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NeonGreen,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "100% Margin Level",
                        fontSize = 12.sp,
                        color = MutedText
                    )
                    Text(
                        text = "Margin Call (Alert)",
                        fontSize = 10.sp,
                        color = WarningYellow
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (price100 > 0) formatPrice(price100) else "N/A",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonGreen,
                        modifier = Modifier.testTag("breakpoint_100_price")
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(60.dp)
                        .background(Color(0xFF2C2C2C))
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = "30% Margin Level",
                        fontSize = 12.sp,
                        color = MutedText
                    )
                    Text(
                        text = "Stop Out (Liquidation)",
                        fontSize = 10.sp,
                        color = DangerRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (price30 > 0) formatPrice(price30) else "N/A",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = WarningYellow,
                        modifier = Modifier.testTag("breakpoint_30_price")
                    )
                }
            }
        }
    }
}

@Composable
fun DetailDialog(
    item: MarginCalculation,
    onDismiss: () -> Unit
) {
    val isBuy = item.positionType == "BUY"
    val dateString = remember(item.timestamp) {
        val formatter = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' HH:mm", Locale.getDefault())
        formatter.format(Date(item.timestamp))
    }

    val parsedPositionsList = remember(item.positionsJson) {
        if (!item.positionsJson.isNullOrEmpty()) {
            try {
                val moshi = com.squareup.moshi.Moshi.Builder()
                    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                    .build()
                val adapter = moshi.adapter<List<com.example.api.ParsedPosition>>(
                    com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.api.ParsedPosition::class.java)
                )
                adapter.fromJson(item.positionsJson) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.symbol,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )

                    Box(
                        modifier = Modifier
                            .background(
                                if (isBuy) DarkGreen.copy(alpha = 0.3f) else DangerRed.copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.positionType,
                            color = if (isBuy) NeonGreen else DangerRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = dateString,
                    color = MutedText,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Divider(color = Color(0xFF2C2C2C), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                // Image if available
                if (item.imageUri != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        AsyncImage(
                            model = item.imageUri,
                            contentDescription = "Saved Screenshot",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Calculation Results Row
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(BorderStroke(1.dp, NeonGreen.copy(alpha = 0.2f)), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = CharcoalCard)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "MARGIN BREAKPOINT RESULTS",
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("100% (Margin Call)", color = MutedText, fontSize = 11.sp)
                                Text(
                                    formatPrice(item.price100),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("30% (Stop Out)", color = MutedText, fontSize = 11.sp)
                                Text(
                                    formatPrice(item.price30),
                                    color = WarningYellow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                // Underlying Data Metrics Table
                Text("UNDERLYING METRICS", color = MutedText, fontWeight = FontWeight.Bold, fontSize = 10.sp)

                Spacer(modifier = Modifier.height(4.dp))

                MetricDetailRow("Open / Entry Price", formatPrice(item.openPrice))
                MetricDetailRow("Current Asset Price", formatPrice(item.currentPrice))
                MetricDetailRow("Floating Position PnL", formatMoney(item.positionPnl), color = if (item.positionPnl >= 0) NeonGreen else DangerRed)
                MetricDetailRow("Account Equity", formatMoney(item.equity))
                MetricDetailRow("Used Margin", formatMoney(item.margin))

                val computedLevel = if (item.margin > 0) (item.equity / item.margin) * 100.0 else 0.0
                MetricDetailRow("Current Margin Level", formatPercent(computedLevel), color = if (computedLevel >= 100.0) NeonGreen else WarningYellow)

                item.extractedMarginLevel?.let { ocrLevel ->
                    MetricDetailRow("Screenshot Margin Level", formatPercent(ocrLevel), color = Color.White)
                }

                // Deserialized Positions Breakdown
                if (parsedPositionsList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("ACTIVE POSITIONS BREAKDOWN (${parsedPositionsList.size})", color = MutedText, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        parsedPositionsList.forEach { pos ->
                            val posBuy = pos.positionType == "BUY"
                            val posPnl = pos.positionPnl ?: 0.0
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(BorderStroke(1.dp, Color(0xFF2C2C2C)), RoundedCornerShape(6.dp)),
                                colors = CardDefaults.cardColors(containerColor = CharcoalCard)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = pos.symbol ?: "Asset",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (posBuy) DarkGreen.copy(alpha = 0.3f) else DangerRed.copy(alpha = 0.2f),
                                                        RoundedCornerShape(3.dp)
                                                    )
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = pos.positionType ?: "BUY",
                                                    color = if (posBuy) NeonGreen else DangerRed,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Open: ${formatPrice(pos.openPrice)} | Live: ${formatPrice(pos.currentPrice)}",
                                            color = MutedText,
                                            fontSize = 10.sp
                                        )
                                    }

                                    Text(
                                        text = formatMoney(posPnl),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (posPnl >= 0.0) NeonGreen else DangerRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = CharcoalSurface,
        titleContentColor = Color.White,
        textContentColor = WhiteText
    )
}

@Composable
fun MetricDetailRow(
    label: String,
    value: String,
    color: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MutedText, fontSize = 13.sp)
        Text(text = value, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun ValidationCard(
    extractedLevel: String,
    computedLevel: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, NeonGreen.copy(alpha = 0.2f)), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "MARGIN LEVEL VALIDATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NeonGreen,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Extracted Margin Level Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Screenshot OCR",
                        fontSize = 12.sp,
                        color = MutedText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (extractedLevel.isNotEmpty()) {
                            val cleanVal = extractedLevel.toDoubleOrNull()
                            if (cleanVal != null) String.format(Locale.US, "%.2f%%", cleanVal) else "$extractedLevel%"
                        } else "N/A",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(45.dp)
                        .background(Color(0xFF2C2C2C))
                )

                // Computed Margin Level Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Formula-Calculated",
                        fontSize = 12.sp,
                        color = MutedText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (computedLevel > 0.0) String.format(Locale.US, "%.2f%%", computedLevel) else "N/A",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (computedLevel >= 100.0) NeonGreen else WarningYellow
                    )
                }
            }

            if (extractedLevel.isNotEmpty() && computedLevel > 0.0) {
                val extractedDbl = extractedLevel.toDoubleOrNull()
                if (extractedDbl != null) {
                    val diff = kotlin.math.abs(extractedDbl - computedLevel)
                    val tolerance = 5.0 // allow small rounding diffs
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (diff <= tolerance) DarkGreen.copy(alpha = 0.15f) else WarningYellow.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (diff <= tolerance) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (diff <= tolerance) NeonGreen else WarningYellow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (diff <= tolerance) {
                                    "Validation Passed: OCR and formula match (diff is tiny)."
                                } else {
                                    "Minor variance detected. Checking broker margin terms is advised."
                                },
                                fontSize = 11.sp,
                                color = if (diff <= tolerance) NeonGreen else WarningYellow,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PriceSimulationCard(
    viewModel: MarginViewModel
) {
    val currentPrice = viewModel.currentCurrentPrice.toDoubleOrNull() ?: 0.0
    val openPrice = viewModel.currentOpenPrice.toDoubleOrNull() ?: 0.0
    val positionPnl = viewModel.currentPositionPnl.toDoubleOrNull() ?: 0.0
    val equity = viewModel.currentEquity.toDoubleOrNull() ?: 0.0
    val margin = viewModel.currentMargin.toDoubleOrNull() ?: 0.0
    val positionType = viewModel.currentPositionType

    if (currentPrice <= 0.0 || margin <= 0.0) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFF27272A)), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Fill or scan open & current price to enable ML% simulation",
                    color = MutedText,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // Range: 80% of currentPrice to 120% of currentPrice
    val minPrice = currentPrice * 0.8
    val maxPrice = currentPrice * 1.2
    
    val simulatedPrice = viewModel.simulatedPriceValue ?: currentPrice

    // Range: 50% of equity to 150% of equity
    val originalEquity = equity
    val minEquity = originalEquity * 0.5
    val maxEquity = originalEquity * 1.5
    val simulatedEquity = viewModel.simulatedEquityValue ?: originalEquity
    
    // Compute simulated margin level
    val priceDiff = currentPrice - openPrice
    val kSigned = if (priceDiff != 0.0) {
        positionPnl / priceDiff
    } else {
        0.0
    }
    
    // Total simulated profit change from price slider
    val simulatedPnlChange = kSigned * (simulatedPrice - currentPrice)
    // Equity is modified by the price simulation + the equity simulation delta
    val simulatedEquityWithPriceAndEquityChange = simulatedEquity + simulatedPnlChange
    val simulatedML = if (margin > 0) (simulatedEquityWithPriceAndEquityChange / margin) * 100.0 else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, NeonGreen.copy(alpha = 0.2f)), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "INTERACTIVE RISK SIMULATOR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NeonGreen,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Simulated Price", color = MutedText, fontSize = 11.sp)
                    Text(
                        text = formatPrice(simulatedPrice),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Simulated Margin Level (%)", color = MutedText, fontSize = 11.sp)
                    Text(
                        text = formatPercent(simulatedML),
                        color = when {
                            simulatedML <= 30.0 -> DangerRed
                            simulatedML <= 100.0 -> WarningYellow
                            else -> NeonGreen
                        },
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = simulatedPrice.toFloat(),
                onValueChange = { newValue ->
                    viewModel.simulatedPriceValue = newValue.toDouble()
                },
                valueRange = if (minPrice < maxPrice) minPrice.toFloat()..maxPrice.toFloat() else 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = NeonGreen,
                    activeTrackColor = NeonGreen,
                    inactiveTrackColor = Color(0xFF2C2C2C)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Min: ${formatPrice(minPrice)}",
                    color = MutedText,
                    fontSize = 10.sp
                )

                Text(
                    text = "Reset Price",
                    color = NeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        viewModel.simulatedPriceValue = currentPrice
                    }
                )

                Text(
                    text = "Max: ${formatPrice(maxPrice)}",
                    color = MutedText,
                    fontSize = 10.sp
                )
            }

            Divider(color = Color(0xFF2C2C2C), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Simulated Account Equity", color = MutedText, fontSize = 11.sp)
                    Text(
                        text = formatMoney(simulatedEquity),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }

                Text(
                    text = "Reset Equity",
                    color = NeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        viewModel.simulatedEquityValue = originalEquity
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = simulatedEquity.toFloat(),
                onValueChange = { newValue ->
                    viewModel.simulatedEquityValue = newValue.toDouble()
                },
                valueRange = if (minEquity < maxEquity) minEquity.toFloat()..maxEquity.toFloat() else 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = NeonGreen,
                    activeTrackColor = NeonGreen,
                    inactiveTrackColor = Color(0xFF2C2C2C)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Min: ${formatMoney(minEquity)}",
                    color = MutedText,
                    fontSize = 10.sp
                )

                Text(
                    text = "Max: ${formatMoney(maxEquity)}",
                    color = MutedText,
                    fontSize = 10.sp
                )
            }

            if (simulatedML <= 100.0) {
                Spacer(modifier = Modifier.height(12.dp))
                val isStopOut = simulatedML <= 30.0
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isStopOut) DangerRed.copy(alpha = 0.15f) else WarningYellow.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isStopOut) Icons.Default.Cancel else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isStopOut) DangerRed else WarningYellow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isStopOut) {
                                "STOP OUT REACHED! Position will be liquidated."
                            } else {
                                "MARGIN CALL WARNING! Additional funds needed soon."
                            },
                            fontSize = 11.sp,
                            color = if (isStopOut) DangerRed else WarningYellow,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun formatMoney(value: Double?, prefix: String = "$", decimalPlaces: Int = 2): String {
    if (value == null) return "${prefix}0.00"
    val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.US)
    formatter.minimumFractionDigits = decimalPlaces
    formatter.maximumFractionDigits = decimalPlaces
    return "$prefix${formatter.format(value)}"
}

fun formatPrice(value: Double?, prefix: String = "$", decimalPlaces: Int = 4): String {
    if (value == null) return "${prefix}0.0000"
    val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.US)
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = decimalPlaces
    return "$prefix${formatter.format(value)}"
}

fun formatPercent(value: Double?, suffix: String = "%", decimalPlaces: Int = 2): String {
    if (value == null) return "0.00$suffix"
    val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.US)
    formatter.minimumFractionDigits = decimalPlaces
    formatter.maximumFractionDigits = decimalPlaces
    return "${formatter.format(value)}$suffix"
}

fun formatRawNumber(value: Double?, decimalPlaces: Int = 2): String {
    if (value == null) return "0.00"
    val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.US)
    formatter.minimumFractionDigits = decimalPlaces
    formatter.maximumFractionDigits = decimalPlaces
    return formatter.format(value)
}
