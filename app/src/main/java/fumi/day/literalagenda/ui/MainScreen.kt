package fumi.day.literalagenda.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import fumi.day.literalagenda.data.Event
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToEdit: (Event?) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val events by viewModel.events.collectAsState(initial = emptyList())
    val showMiniCalendar by viewModel.showMiniCalendar.collectAsState(initial = false)
    val controlsOnLeft by viewModel.controlsOnLeft.collectAsState(initial = false)
    val dateFormat by viewModel.dateFormat.collectAsState(initial = "wmd")
    val syncState by viewModel.syncState.collectAsState()
    val lastSyncedAt by viewModel.lastSyncedAt.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var searchInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(syncState) {
        if (syncState is SyncState.Error) viewModel.clearSyncState()
    }
    BackHandler(enabled = searchQuery.isNotBlank()) {
        searchQuery = ""
        searchInput = ""
    }

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val eventsOnSelectedDate = selectedDate?.let { date ->
        events.filter { it.date == date }
    } ?: emptyList()

    val displayedEvents = if (searchQuery.isBlank()) {
        events
    } else {
        events.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.note.contains(searchQuery, ignoreCase = true)
        }
    }

    val eventDates = remember(events) {
        events.map { it.date }.toSet()
    }


    Scaffold(
        topBar = {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = {
                        if (syncState is SyncState.Syncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(12.dp).size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        } else {
                            IconButton(onClick = { viewModel.syncAndLoad() }) {
                                Icon(Icons.Default.Sync, contentDescription = "Sync", tint = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = if (searchQuery.isNotBlank()) "${displayedEvents.size} results" else "${events.size} events",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) "searching: $searchQuery" else getSubtitleText(syncState, lastSyncedAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleMiniCalendar() }) {
                            Icon(
                                if (showMiniCalendar) Icons.Default.List else Icons.Default.CalendarMonth,
                                contentDescription = "Toggle calendar",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                )
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
            }
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color.Transparent,
                modifier = Modifier.imePadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            searchQuery = searchInput
                            keyboardController?.hide()
                        }),
                        decorationBox = { innerTextField ->
                            if (searchInput.isEmpty()) {
                                Text(
                                    text = "Search events...",
                                    style = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }
        },
        floatingActionButtonPosition = if (controlsOnLeft) FabPosition.Start else FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEdit(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New event")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (showMiniCalendar) {
                MiniCalendar(
                    eventDates = eventDates,
                    onDateSelected = { date -> selectedDate = date },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            if (displayedEvents.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isBlank()) "No events yet.\nTap + to create one." else "No results",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    itemsIndexed(displayedEvents, key = { _, event -> "${event.filename}_${event.date}" }) { index, event ->
                        EventItem(event = event, dateFormat = dateFormat, isOdd = index % 2 == 0, onClick = { onNavigateToEdit(event) })
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    selectedDate?.let { date ->
        val dateFormatter = DateTimeFormatter.ofPattern("MM/dd (EEE)", Locale.ENGLISH)
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val bodyStyle = MaterialTheme.typography.bodyMedium
        AlertDialog(
            onDismissRequest = { selectedDate = null },
            title = { Text(date.format(dateFormatter), color = MaterialTheme.colorScheme.onBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    eventsOnSelectedDate.forEach { event ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDate = null; onNavigateToEdit(event) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            event.time?.let { time ->
                                Text(
                                    text = time.format(timeFormatter),
                                    style = bodyStyle.copy(fontFamily = FontFamily.Monospace, lineHeight = bodyStyle.lineHeight),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(text = event.title, style = bodyStyle.copy(lineHeight = bodyStyle.lineHeight), color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selectedDate = null }) { Text("Close") } }
        )
    }
}

@Composable
private fun EventItem(event: Event, dateFormat: String, isOdd: Boolean, onClick: () -> Unit) {
    val dateText = formatDate(event.date, dateFormat)
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val bodyStyle = MaterialTheme.typography.bodyMedium
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isOdd) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = dateText, style = bodyStyle.copy(fontFamily = FontFamily.Monospace, lineHeight = bodyStyle.lineHeight), color = MaterialTheme.colorScheme.onBackground)
        event.time?.let { time ->
            Text(text = time.format(timeFormatter), style = bodyStyle.copy(fontFamily = FontFamily.Monospace, lineHeight = bodyStyle.lineHeight), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = event.title, style = bodyStyle.copy(lineHeight = bodyStyle.lineHeight), color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

private fun getSubtitleText(syncState: SyncState, lastSyncedAt: String): String {
    return when {
        syncState is SyncState.Error -> "sync failed"
        lastSyncedAt.isNotEmpty() -> "synced $lastSyncedAt"
        else -> ""
    }
}

private fun formatDate(date: LocalDate, format: String): String {
    val dayOfWeek = date.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH))
    val month = date.format(DateTimeFormatter.ofPattern("MM"))
    val day = date.format(DateTimeFormatter.ofPattern("dd"))
    return when (format) {
        "wmd" -> "$dayOfWeek $month/$day"
        "wdm" -> "$dayOfWeek $day/$month"
        "mdw" -> "$month/$day $dayOfWeek"
        "dmw" -> "$day/$month $dayOfWeek"
        else -> "$dayOfWeek $month/$day"
    }
}
