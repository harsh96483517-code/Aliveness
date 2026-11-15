package com.aliveness.offline.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aliveness.offline.MainViewModel
import com.aliveness.offline.data.VideoItem
import com.aliveness.offline.data.formatDate
import com.aliveness.offline.ui.components.EmptyState
import com.aliveness.offline.ui.components.ScreenTitle
import com.aliveness.offline.ui.components.VideoMenuHost
import com.aliveness.offline.ui.components.VideoRow
import com.aliveness.offline.ui.theme.Dimens

private val TypeFilters = listOf("All", "Reels", "Long", "Favorites")
private val DateFilters = listOf("Any date", "Today", "This week", "This month")
private val FormatFilters = listOf("Any format", "MP4", "MKV", "WEBM", "3GP")

private fun matches(v: VideoItem, tokens: List<String>): Boolean {
    if (tokens.isEmpty()) return true
    val hay = "${v.title} ${v.fileName} ${v.folderName} ${v.folderPath} ${formatDate(v.dateAddedSec)} ${v.extension}".lowercase()
    return tokens.all { it in hay }
}

@Composable
fun SearchScreen(vm: MainViewModel, onPlay: (VideoItem, String) -> Unit) {
    val items by vm.items.collectAsState()
    val recents by vm.recentSearches.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(TypeFilters[0]) }
    var date by rememberSaveable { mutableStateOf(DateFilters[0]) }
    var format by rememberSaveable { mutableStateOf(FormatFilters[0]) }
    var menu by remember { mutableStateOf<VideoItem?>(null) }
    val focus = LocalFocusManager.current

    val tokens = remember(query) { query.lowercase().split(' ').filter { it.isNotBlank() } }
    val filtersOn = type != TypeFilters[0] || date != DateFilters[0] || format != FormatFilters[0]
    val searching = tokens.isNotEmpty() || filtersOn

    val results = remember(items, tokens, type, date, format) {
        val now = System.currentTimeMillis() / 1000
        val cutoff = when (date) {
            "Today" -> now - 86_400
            "This week" -> now - 7 * 86_400
            "This month" -> now - 30 * 86_400
            else -> 0L
        }
        items.filter { v ->
            matches(v, tokens) &&
                when (type) { "Reels" -> v.isReel; "Long" -> v.isLong; "Favorites" -> v.favorite; else -> true } &&
                v.dateAddedSec >= cutoff &&
                (format == FormatFilters[0] || v.extension == format)
        }
    }
    // Suggestions: titles and folders that contain what has been typed so far.
    val suggestions = remember(items, tokens) {
        if (tokens.isEmpty()) emptyList()
        else (items.map { it.title } + items.map { it.folderName }).distinct()
            .filter { s -> s.lowercase().contains(tokens.last()) && !s.equals(query, ignoreCase = true) }
            .take(5)
    }

    fun commit() { vm.rememberSearch(query); focus.clearFocus() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
            bottom = Dimens.NavBarSpace,
        ),
    ) {
        item {
            Column {
                ScreenTitle("Search")
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.PageGutter),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("Name, folder, date or format") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Clear search") }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { commit() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                )
                Spacer(Modifier.height(10.dp))
                ChipRow(TypeFilters, type) { type = it }
                ChipRow(DateFilters, date) { date = it }
                ChipRow(FormatFilters, format) { format = it }
                if (filtersOn || query.isNotEmpty()) {
                    TextButton(
                        onClick = { query = ""; type = TypeFilters[0]; date = DateFilters[0]; format = FormatFilters[0] },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    ) { Text("Clear search and filters") }
                }
            }
        }

        if (!searching) {
            if (recents.isNotEmpty()) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(start = Dimens.PageGutter, end = 8.dp, top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Recent searches", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.clearSearches() }) { Text("Clear") }
                    }
                }
                items(recents, key = { it.text }) { r ->
                    Row(
                        Modifier.fillMaxWidth().clickable { query = r.text }
                            .padding(horizontal = Dimens.PageGutter, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(r.text, modifier = Modifier.weight(1f).padding(horizontal = 14.dp))
                        IconButton(onClick = { vm.forgetSearch(r) }) { Icon(Icons.Rounded.Close, "Remove") }
                    }
                }
            } else {
                item {
                    EmptyState(Icons.Rounded.Search, "Search your videos",
                        "Find a clip by name, folder, the date it was added or its format. Everything stays on this phone.")
                }
            }
        } else {
            if (suggestions.isNotEmpty()) {
                items(suggestions, key = { "s$it" }) { s ->
                    Row(
                        Modifier.fillMaxWidth().clickable { query = s }
                            .padding(horizontal = Dimens.PageGutter, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(s, modifier = Modifier.padding(start = 14.dp), maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }
            if (results.isEmpty()) {
                item { EmptyState(Icons.Rounded.Search, "No results", "Nothing matches. Try fewer words or clear a filter.") }
            } else {
                item {
                    Text(
                        "${results.size} result${if (results.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Dimens.PageGutter, vertical = 8.dp),
                    )
                }
                items(results, key = { it.id }) { v ->
                    VideoRow(v, onClick = {
                        vm.rememberSearch(query)
                        onPlay(v, com.aliveness.offline.data.Source.ALL)
                    }, onLongClick = { menu = v })
                }
            }
        }
    }
    VideoMenuHost(menu, { menu = null }, vm, onPlay = { onPlay(it, com.aliveness.offline.data.Source.ALL) })
}

@Composable
private fun ChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimens.PageGutter, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options) { o -> FilterChip(selected = o == selected, onClick = { onSelect(o) }, label = { Text(o) }) }
    }
}
