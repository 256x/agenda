package fumi.day.literalagenda.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import fumi.day.literalagenda.data.GitForge
import fumi.day.literalagenda.ui.theme.parseColor
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext

enum class ColorPickerTarget {
    BACKGROUND, TEXT, ACCENT
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val controlsOnLeft by viewModel.controlsOnLeft.collectAsState(initial = false)
    val gitToken by viewModel.gitToken.collectAsState(initial = "")
    val gitRepo by viewModel.gitRepo.collectAsState(initial = "")
    val gitForge by viewModel.gitForge.collectAsState(initial = GitForge.GITHUB)
    val gitHost by viewModel.gitHost.collectAsState(initial = "")
    val bgColor by viewModel.bgColor.collectAsState(initial = "")
    val textColor by viewModel.textColor.collectAsState(initial = "")
    val accentColor by viewModel.accentColor.collectAsState(initial = "")
    val fontChoice by viewModel.fontChoice.collectAsState(initial = "system")
    val fontSize by viewModel.fontSize.collectAsState(initial = 16f)
    val dateFormat by viewModel.dateFormat.collectAsState(initial = "wmd")
    val pastMonths by viewModel.pastMonths.collectAsState(initial = 1)
    val futureMonths by viewModel.futureMonths.collectAsState(initial = 3)
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val importResult by viewModel.importResult.collectAsState()

    var showGitDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf<ColorPickerTarget?>(null) }

    val fontOptions = listOf("system", "serif", "mono", "scopeone")
    val fontLabels = mapOf(
        "system" to "Default",
        "serif" to "Serif",
        "mono" to "Mono",
        "scopeone" to "Scope"
    )
    val dateFormatOptions = listOf("wmd", "wdm", "mdw", "dmw")
    val dateFormatLabels = mapOf(
        "wmd" to "Tue 04/15",
        "wdm" to "Tue 15/04",
        "mdw" to "04/15 Tue",
        "dmw" to "15/04 Tue"
    )

    val isGitConnected = gitToken.isNotBlank() && gitRepo.isNotBlank() &&
        (gitForge == GitForge.GITHUB || gitHost.isNotBlank())

    LaunchedEffect(syncError) {
        syncError?.let { viewModel.clearSyncError() }
    }

    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            val inputStream = context.contentResolver.openInputStream(selectedUri)
            inputStream?.let { viewModel.importICal(it) }
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = { Text("Settings", color = MaterialTheme.colorScheme.onBackground) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack, enabled = !isSyncing) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                )
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance card
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fontOptions.forEach { font ->
                            FilterChip(
                                selected = fontChoice == font,
                                onClick = { scope.launch { viewModel.setFontChoice(font) } },
                                label = { Text(fontLabels[font] ?: font) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Size", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = fontSize,
                            onValueChange = { scope.launch { viewModel.setFontSize(it) } },
                            valueRange = 12f..24f,
                            steps = 5,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        Text("${fontSize.roundToInt()}sp", style = MaterialTheme.typography.bodyMedium)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    // Colors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ColorDot(
                            label = "BG",
                            color = parseColor(bgColor) ?: MaterialTheme.colorScheme.background,
                            onClick = { showColorPicker = ColorPickerTarget.BACKGROUND }
                        )
                        ColorDot(
                            label = "Text",
                            color = parseColor(textColor) ?: MaterialTheme.colorScheme.onBackground,
                            onClick = { showColorPicker = ColorPickerTarget.TEXT }
                        )
                        ColorDot(
                            label = "Accent",
                            color = parseColor(accentColor) ?: MaterialTheme.colorScheme.primary,
                            onClick = { showColorPicker = ColorPickerTarget.ACCENT }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    // Date format
                    Text("Date format", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dateFormatOptions.forEach { format ->
                            FilterChip(
                                selected = dateFormat == format,
                                onClick = { scope.launch { viewModel.setDateFormat(format) } },
                                label = { Text(dateFormatLabels[format] ?: format) }
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Controls on left", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = controlsOnLeft,
                            onCheckedChange = { scope.launch { viewModel.setControlsOnLeft(it) } }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text("Search range", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Past", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0 to "Today", 1 to "1 mo", 3 to "3 mo", 12 to "1 yr", -1 to "All").forEach { (months, label) ->
                            FilterChip(
                                selected = pastMonths == months,
                                onClick = { scope.launch { viewModel.setPastMonths(months) } },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Future", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1 to "1 mo", 3 to "3 mo", 6 to "6 mo", 12 to "1 yr", -1 to "All").forEach { (months, label) ->
                            FilterChip(
                                selected = futureMonths == months,
                                onClick = { scope.launch { viewModel.setFutureMonths(months) } },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            // Git Sync card
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Git Sync", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (isGitConnected) {
                        Text(
                            text = if (gitForge == GitForge.GITEA) "Gitea / Forgejo" else "GitHub",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(gitRepo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { scope.launch { viewModel.syncNow() } }, enabled = !isSyncing) {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync Now")
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            OutlinedButton(onClick = { showGitDialog = true }, enabled = !isSyncing) {
                                Text("Edit")
                            }
                            OutlinedButton(
                                onClick = { scope.launch { viewModel.setGitToken(""); viewModel.setGitRepo("") } },
                                enabled = !isSyncing
                            ) {
                                Text("Disconnect")
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showGitDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSyncing
                        ) {
                            Text("Connect")
                        }
                    }
                }
            }

            // Import card
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Import", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSyncing && !isImporting
                    ) {
                        Text("Import from iCal (.ics)")
                    }
                }
            }

            Text(
                text = "Literal Agenda v${fumi.day.literalagenda.BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    // Syncing dialog
    if (isSyncing) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Syncing...") },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Text("Please wait")
                }
            },
            confirmButton = { }
        )
    }

    // Importing dialog
    if (isImporting) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Importing...") },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Text("Please wait")
                }
            },
            confirmButton = { }
        )
    }

    // Import result dialog
    importResult?.let { count ->
        AlertDialog(
            onDismissRequest = { viewModel.clearImportResult() },
            title = { Text("Import complete") },
            text = { Text("Imported $count events.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearImportResult() }) {
                    Text("OK")
                }
            }
        )
    }

    // Git dialog
    if (showGitDialog) {
        var forgeInput by remember { mutableStateOf(gitForge) }
        var hostInput by remember { mutableStateOf(gitHost) }
        var tokenInput by remember { mutableStateOf(gitToken) }
        var repoInput by remember { mutableStateOf(gitRepo) }
        val repoWillChange = isGitConnected && (
            forgeInput != gitForge || hostInput != gitHost || repoInput != gitRepo
        )
        AlertDialog(
            onDismissRequest = { if (!isSyncing) showGitDialog = false },
            title = { Text("Git Sync") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = forgeInput == GitForge.GITHUB,
                            onClick = { forgeInput = GitForge.GITHUB },
                            label = { Text("GitHub") },
                            enabled = !isSyncing
                        )
                        FilterChip(
                            selected = forgeInput == GitForge.GITEA,
                            onClick = { forgeInput = GitForge.GITEA },
                            label = { Text("Gitea / Forgejo") },
                            enabled = !isSyncing
                        )
                    }
                    if (forgeInput == GitForge.GITEA) {
                        OutlinedTextField(
                            value = hostInput,
                            onValueChange = { hostInput = it },
                            label = { Text("Host (e.g. https://codeberg.org)") },
                            singleLine = true,
                            enabled = !isSyncing,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("Personal Access Token") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val repoValid = repoInput.matches(Regex("^[^/]+/[^/]+$"))
                    OutlinedTextField(
                        value = repoInput,
                        onValueChange = { repoInput = it },
                        label = { Text("Repository (owner/repo)") },
                        singleLine = true,
                        isError = repoInput.isNotBlank() && !repoValid,
                        supportingText = if (repoInput.isNotBlank() && !repoValid) {
                            { Text("Format: owner/repo") }
                        } else null,
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (repoWillChange) {
                        Text(
                            text = "Switching repositories will remove all local data. It will be re-synced from the new repository.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                val repoValid = repoInput.matches(Regex("^[^/]+/[^/]+$"))
                val hostValid = forgeInput == GitForge.GITHUB || hostInput.isNotBlank()
                TextButton(
                    onClick = {
                        scope.launch {
                            val success = viewModel.connectGit(forgeInput, hostInput, tokenInput, repoInput)
                            if (success) showGitDialog = false
                        }
                    },
                    enabled = !isSyncing && tokenInput.isNotBlank() && repoValid && hostValid
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGitDialog = false }, enabled = !isSyncing) {
                    Text("Cancel")
                }
            }
        )
    }

    // Color picker dialog
    showColorPicker?.let { target ->
        val initialColor = when (target) {
            ColorPickerTarget.BACKGROUND -> parseColor(bgColor) ?: MaterialTheme.colorScheme.background
            ColorPickerTarget.TEXT -> parseColor(textColor) ?: MaterialTheme.colorScheme.onBackground
            ColorPickerTarget.ACCENT -> parseColor(accentColor) ?: MaterialTheme.colorScheme.primary
        }
        ColorPickerDialog(
            initialColor = initialColor,
            onColorSelected = { color ->
                val hex = colorToHex(color)
                scope.launch {
                    when (target) {
                        ColorPickerTarget.BACKGROUND -> viewModel.setBgColor(hex)
                        ColorPickerTarget.TEXT -> viewModel.setTextColor(hex)
                        ColorPickerTarget.ACCENT -> viewModel.setAccentColor(hex)
                    }
                }
                showColorPicker = null
            },
            onDismiss = { showColorPicker = null }
        )
    }
}

@Composable
private fun ColorDot(label: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f), CircleShape)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var brightness by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(initialColor) {
        if (initialColor != Color.Unspecified) {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
            hue = hsv[0]
            saturation = hsv[1]
            brightness = hsv[2]
        }
    }

    val currentColor = Color.hsv(hue, saturation, brightness)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Color") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .pointerInput(hue) {
                            detectTapGestures { offset ->
                                saturation = (offset.x / size.width).coerceIn(0f, 1f)
                                brightness = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                            }
                        }
                        .pointerInput(hue) {
                            detectDragGestures { change, _ ->
                                saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                                brightness = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(brush = Brush.horizontalGradient(colors = listOf(Color.White, Color.hsv(hue, 1f, 1f))))
                        drawRect(brush = Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black)))
                        val cursorX = saturation * size.width
                        val cursorY = (1f - brightness) * size.height
                        drawCircle(color = Color.White, radius = 12f, center = Offset(cursorX, cursorY))
                        drawCircle(
                            color = Color.Black, radius = 10f, center = Offset(cursorX, cursorY),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                hue = (offset.x / size.width * 360f).coerceIn(0f, 360f)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                hue = (change.position.x / size.width * 360f).coerceIn(0f, 360f)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val colors = (0..360 step 30).map { Color.hsv(it.toFloat(), 1f, 1f) }
                        drawRect(brush = Brush.horizontalGradient(colors))
                        val cursorX = hue / 360f * size.width
                        drawCircle(color = Color.White, radius = 14f, center = Offset(cursorX, size.height / 2))
                        drawCircle(
                            color = Color.Black, radius = 12f, center = Offset(cursorX, size.height / 2),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f), CircleShape)
                    )
                    Text(text = colorToHex(currentColor), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(currentColor) }) { Text("Select") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    return String.format("#%06X", 0xFFFFFF and argb)
}
