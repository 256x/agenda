package fumi.day.literalagenda.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import fumi.day.literalagenda.data.Event
import fumi.day.literalagenda.data.RepeatType
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class NumpadTarget { DATE, TIME }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    event: Event?,
    viewModel: EditViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isSaving by viewModel.isSaving.collectAsState()
    val controlsOnLeft by viewModel.controlsOnLeft.collectAsState(initial = false)
    val keyboardController = LocalSoftwareKeyboardController.current

    var dateInput by remember { mutableStateOf(event?.date?.format(DateTimeFormatter.ofPattern("yyyyMMdd")) ?: "") }
    var timeInput by remember { mutableStateOf(event?.time?.format(DateTimeFormatter.ofPattern("HHmm")) ?: "") }
    var title by remember { mutableStateOf(event?.title ?: "") }
    var note by remember { mutableStateOf(event?.note ?: "") }
    var repeat by remember { mutableStateOf(event?.repeat ?: RepeatType.NONE) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showNumpad by remember { mutableStateOf<NumpadTarget?>(null) }

    val parsedDate = parseSmartDate(dateInput)
    val parsedTime = parseSmartTime(timeInput)

    val datePreview = parsedDate?.format(DateTimeFormatter.ofPattern("yyyy/MM/dd (EEE)", Locale.ENGLISH)) ?: ""
    val timePreview = parsedTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: ""

    val isValid = parsedDate != null && title.isNotBlank()

    Scaffold(
        floatingActionButtonPosition = if (controlsOnLeft) FabPosition.Start else FabPosition.End,
        topBar = {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = {
                        Text(
                            if (event == null) "New Event" else "Edit Event",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack, enabled = !isSaving) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {
                        if (event != null) {
                            IconButton(
                                onClick = { showDeleteDialog = true },
                                enabled = !isSaving
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                )
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isValid && !isSaving) {
                        keyboardController?.hide()
                        scope.launch {
                            val success = viewModel.saveEvent(
                                existingFilename = event?.filename,
                                date = parsedDate ?: return@launch,
                                time = parsedTime,
                                title = title,
                                note = note,
                                repeat = repeat
                            )
                            if (success) onNavigateBack()
                        }
                    }
                },
                containerColor = if (isValid && !isSaving)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save")
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
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = if (datePreview.isNotEmpty()) datePreview else dateInput,
                    onValueChange = {},
                    label = { Text("Date (dd, mmdd, yyyymmdd)") },
                    singleLine = true,
                    readOnly = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                Box(modifier = Modifier.matchParentSize().clickable(enabled = !isSaving) {
                    keyboardController?.hide()
                    showNumpad = NumpadTarget.DATE
                })
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = if (timePreview.isNotEmpty()) timePreview else timeInput,
                    onValueChange = {},
                    label = { Text("Time (h, hh, hmm, hhmm)") },
                    singleLine = true,
                    readOnly = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                Box(modifier = Modifier.matchParentSize().clickable(enabled = !isSaving) {
                    keyboardController?.hide()
                    showNumpad = NumpadTarget.TIME
                })
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note") },
                minLines = 3,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Repeat", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RepeatType.entries.forEach { type ->
                    FilterChip(
                        selected = repeat == type,
                        onClick = { repeat = type },
                        enabled = !isSaving,
                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    showNumpad?.let { target ->
        val currentValue = if (target == NumpadTarget.DATE) dateInput else timeInput
        val maxLen = if (target == NumpadTarget.DATE) 8 else 4
        val label = if (target == NumpadTarget.DATE) "Date" else "Time"
        val hint = if (target == NumpadTarget.DATE) "dd / mmdd / yyyymmdd" else "h / hh / hmm / hhmm"

        var tempInput by remember(target) { mutableStateOf(currentValue) }
        val tempParsed = if (target == NumpadTarget.DATE) parseSmartDate(tempInput) else parseSmartTime(tempInput)
        val preview = when {
            target == NumpadTarget.DATE && tempParsed != null ->
                (tempParsed as LocalDate).format(DateTimeFormatter.ofPattern("yyyy/MM/dd (EEE)", Locale.ENGLISH))
            target == NumpadTarget.TIME && tempParsed != null ->
                (tempParsed as LocalTime).format(DateTimeFormatter.ofPattern("HH:mm"))
            else -> ""
        }

        AlertDialog(
            onDismissRequest = { showNumpad = null },
            title = { Text(label) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (tempInput.isEmpty()) hint else tempInput,
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace),
                        color = if (tempInput.isEmpty())
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = preview.ifEmpty { " " },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val keys = listOf("7","8","9","4","5","6","1","2","3","CLR","0","⌫")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        keys.chunked(3).forEach { row ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                row.forEach { key ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1.8f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                                            .clickable {
                                                when (key) {
                                                    "CLR" -> tempInput = ""
                                                    "⌫" -> if (tempInput.isNotEmpty()) tempInput = tempInput.dropLast(1)
                                                    else -> if (tempInput.length < maxLen) tempInput += key
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (target == NumpadTarget.DATE) dateInput = tempInput
                    else timeInput = tempInput
                    showNumpad = null
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNumpad = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteDialog && event != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Event") },
            text = { Text("Delete \"${event.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val success = viewModel.deleteEvent(event)
                            if (success) {
                                showDeleteDialog = false
                                onNavigateBack()
                            }
                        }
                    },
                    enabled = !isSaving
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, enabled = !isSaving) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun parseSmartDate(input: String): LocalDate? {
    if (input.isBlank()) return null
    val now = LocalDate.now()
    return try {
        when (input.length) {
            1, 2 -> { val day = input.toInt(); if (day in 1..31) now.withDayOfMonth(day) else null }
            3, 4 -> {
                val month = input.dropLast(2).toInt()
                val day = input.takeLast(2).toInt()
                if (month in 1..12 && day in 1..31) LocalDate.of(now.year, month, day) else null
            }
            6 -> {
                val year = 2000 + input.take(2).toInt()
                val month = input.substring(2, 4).toInt()
                val day = input.takeLast(2).toInt()
                LocalDate.of(year, month, day)
            }
            8 -> {
                val year = input.take(4).toInt()
                val month = input.substring(4, 6).toInt()
                val day = input.takeLast(2).toInt()
                LocalDate.of(year, month, day)
            }
            else -> null
        }
    } catch (e: Exception) { null }
}

private fun parseSmartTime(input: String): LocalTime? {
    if (input.isBlank()) return null
    return try {
        when (input.length) {
            1 -> LocalTime.of(input.toInt(), 0)
            2 -> LocalTime.of(input.toInt(), 0)
            3 -> LocalTime.of(input.take(1).toInt(), input.takeLast(2).toInt())
            4 -> LocalTime.of(input.take(2).toInt(), input.takeLast(2).toInt())
            else -> null
        }
    } catch (e: Exception) { null }
}
