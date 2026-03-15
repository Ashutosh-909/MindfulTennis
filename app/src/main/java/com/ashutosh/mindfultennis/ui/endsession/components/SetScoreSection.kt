package com.ashutosh.mindfultennis.ui.endsession.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ashutosh.mindfultennis.domain.model.MatchType
import com.ashutosh.mindfultennis.domain.model.Opponent
import com.ashutosh.mindfultennis.domain.model.Partner
import com.ashutosh.mindfultennis.ui.theme.Spacing

/**
 * Data class holding set score dialog state.
 */
data class SetScoreInputData(
    val matchType: MatchType = MatchType.SINGLES,
    val opponent2: Opponent? = null,
    val partner: Partner? = null,
    val sets: List<SetScoreRow> = listOf(SetScoreRow()),
)

data class SetScoreRow(
    val userScore: String = "",
    val opponentScore: String = "",
    val opponent: Opponent? = null,
)

/**
 * Section that shows an "+ Add Set Scores" button, or a summary card with edit/remove
 * if set scores have been saved.
 */
@Composable
fun SetScoreSection(
    data: SetScoreInputData?,
    opponents: List<Opponent>,
    partners: List<Partner>,
    onSave: (SetScoreInputData) -> Unit,
    onClear: () -> Unit,
    onCreateOpponent: (String) -> Unit,
    onCreatePartner: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Set Scores",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        if (data != null && data.sets.any { it.userScore.isNotBlank() }) {
            // Summary card
            SetScoreSummaryCard(
                data = data,
                opponents = opponents,
                partners = partners,
                onEdit = { showDialog = true },
                onClear = onClear,
            )
        } else {
            // Add button
            OutlinedButton(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text("Add Set Scores")
            }
        }
    }

    if (showDialog) {
        SetScoreDialog(
            initialData = data ?: SetScoreInputData(),
            opponents = opponents,
            partners = partners,
            onSave = { savedData ->
                onSave(savedData)
                showDialog = false
            },
            onDismiss = { showDialog = false },
            onCreateOpponent = onCreateOpponent,
            onCreatePartner = onCreatePartner,
        )
    }
}

@Composable
private fun SetScoreSummaryCard(
    data: SetScoreInputData,
    opponents: List<Opponent>,
    partners: List<Partner>,
    onEdit: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = data.matchType.name.lowercase()
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                )
                Row {
                    TextButton(onClick = onEdit) { Text("Edit") }
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = "Clear set scores")
                    }
                }
            }

            // Opponent info
            val opp2Name = data.opponent2?.name
            if (data.matchType == MatchType.DOUBLES && opp2Name != null) {
                Text(
                    text = "vs. Opponent 2: $opp2Name",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            // Partner info (doubles)
            if (data.matchType == MatchType.DOUBLES && data.partner != null) {
                Text(
                    text = "w/ Partner: ${data.partner.name}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            // Set scores
            val scoresText = data.sets
                .filter { it.userScore.isNotBlank() && it.opponentScore.isNotBlank() }
                .mapIndexed { index, row ->
                    val oppLabel = row.opponent?.name?.let { " vs $it" } ?: ""
                    "Set ${index + 1}: ${row.userScore}-${row.opponentScore}$oppLabel"
                }
                .joinToString("  ")
            if (scoresText.isNotBlank()) {
                Text(text = scoresText, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SetScoreDialog(
    initialData: SetScoreInputData,
    opponents: List<Opponent>,
    partners: List<Partner>,
    onSave: (SetScoreInputData) -> Unit,
    onDismiss: () -> Unit,
    onCreateOpponent: (String) -> Unit,
    onCreatePartner: (String) -> Unit,
) {
    var matchType by rememberSaveable { mutableStateOf(initialData.matchType) }
    var opponent2 by remember { mutableStateOf(initialData.opponent2) }
    var partner by remember { mutableStateOf(initialData.partner) }
    var sets by remember { mutableStateOf(initialData.sets.ifEmpty { listOf(SetScoreRow()) }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Scores") },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        SetScoreInputData(
                            matchType = matchType,
                            opponent2 = if (matchType == MatchType.DOUBLES) opponent2 else null,
                            partner = if (matchType == MatchType.DOUBLES) partner else null,
                            sets = sets,
                        )
                    )
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = {
            Column(
                modifier = Modifier.semantics {
                    contentDescription = "Set Scores dialog"
                },
            ) {
                // Match Type
                Text("Match Type:", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(Spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = matchType == MatchType.SINGLES,
                        onClick = { matchType = MatchType.SINGLES },
                    )
                    Text("Singles", modifier = Modifier.padding(end = Spacing.md))
                    RadioButton(
                        selected = matchType == MatchType.DOUBLES,
                        onClick = { matchType = MatchType.DOUBLES },
                    )
                    Text("Doubles")
                }

                Spacer(Modifier.height(Spacing.md))

                // Doubles-only fields (Opponent 2 and Partner stay at dialog level)
                if (matchType == MatchType.DOUBLES) {
                    Text("Opponent 2:", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(Spacing.xs))
                    OpponentDropdown(
                        selectedOpponent = opponent2,
                        opponents = opponents,
                        onSelected = { opponent2 = it },
                        onCreateNew = onCreateOpponent,
                        label = "Opponent 2",
                    )

                    Spacer(Modifier.height(Spacing.sm))

                    Text("Partner:", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(Spacing.xs))
                    PartnerDropdown(
                        selectedPartner = partner,
                        partners = partners,
                        onSelected = { partner = it },
                        onCreateNew = onCreatePartner,
                        label = "Partner",
                    )
                }

                Spacer(Modifier.height(Spacing.md))

                // Sets
                Text("Sets:", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(Spacing.sm))

                sets.forEachIndexed { index, setRow ->
                    SetScoreRowInput(
                        setNumber = index + 1,
                        userScore = setRow.userScore,
                        opponentScore = setRow.opponentScore,
                        selectedOpponent = setRow.opponent,
                        opponents = opponents,
                        onUserScoreChanged = { value ->
                            sets = sets.toMutableList().also {
                                it[index] = it[index].copy(userScore = value.filter { c -> c.isDigit() }.take(2))
                            }
                        },
                        onOpponentScoreChanged = { value ->
                            sets = sets.toMutableList().also {
                                it[index] = it[index].copy(opponentScore = value.filter { c -> c.isDigit() }.take(2))
                            }
                        },
                        onOpponentSelected = { opponent ->
                            sets = sets.toMutableList().also {
                                it[index] = it[index].copy(opponent = opponent)
                            }
                        },
                        onCreateOpponent = onCreateOpponent,
                        canRemove = index > 0,
                        onRemove = {
                            sets = sets.toMutableList().also { it.removeAt(index) }
                        },
                    )
                    if (index < sets.lastIndex) {
                        Spacer(Modifier.height(Spacing.sm))
                    }
                }

                Spacer(Modifier.height(Spacing.sm))

                TextButton(
                    onClick = {
                        sets = sets + SetScoreRow()
                    },
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(Spacing.xs))
                    Text("Add Set")
                }
            }
        },
    )
}

@Composable
private fun SetScoreRowInput(
    setNumber: Int,
    userScore: String,
    opponentScore: String,
    selectedOpponent: Opponent?,
    opponents: List<Opponent>,
    onUserScoreChanged: (String) -> Unit,
    onOpponentScoreChanged: (String) -> Unit,
    onOpponentSelected: (Opponent?) -> Unit,
    onCreateOpponent: (String) -> Unit,
    canRemove: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = "Set $setNumber:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(48.dp),
            )
            OutlinedTextField(
                value = userScore,
                onValueChange = onUserScoreChanged,
                modifier = Modifier.width(56.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                label = { Text("You") },
            )
            Text("–", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = opponentScore,
                onValueChange = onOpponentScoreChanged,
                modifier = Modifier.width(56.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                label = { Text("Opp") },
            )
            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove set $setNumber")
                }
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        OpponentDropdown(
            selectedOpponent = selectedOpponent,
            opponents = opponents,
            onSelected = onOpponentSelected,
            onCreateNew = onCreateOpponent,
            label = "Opponent (Set $setNumber)",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpponentDropdown(
    selectedOpponent: Opponent?,
    opponents: List<Opponent>,
    onSelected: (Opponent?) -> Unit,
    onCreateNew: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf(selectedOpponent?.name ?: "") }
    var showAddNew by rememberSaveable { mutableStateOf(false) }
    var newName by rememberSaveable { mutableStateOf("") }

    val filteredOpponents = remember(query, opponents) {
        if (query.isBlank()) opponents
        else opponents.filter { it.name.contains(query, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                expanded = true
                if (selectedOpponent != null && it != selectedOpponent.name) {
                    onSelected(null)
                }
            },
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            singleLine = true,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            filteredOpponents.forEach { opponent ->
                DropdownMenuItem(
                    text = { Text(opponent.name) },
                    onClick = {
                        query = opponent.name
                        onSelected(opponent)
                        expanded = false
                    },
                )
            }
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(Spacing.sm))
                        Text("Add New Opponent")
                    }
                },
                onClick = {
                    expanded = false
                    showAddNew = true
                },
            )
        }
    }

    if (showAddNew) {
        AlertDialog(
            onDismissRequest = { showAddNew = false },
            title = { Text("New Opponent") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onCreateNew(newName.trim())
                            query = newName.trim()
                            newName = ""
                            showAddNew = false
                        }
                    },
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNew = false; newName = "" }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartnerDropdown(
    selectedPartner: Partner?,
    partners: List<Partner>,
    onSelected: (Partner?) -> Unit,
    onCreateNew: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf(selectedPartner?.name ?: "") }
    var showAddNew by rememberSaveable { mutableStateOf(false) }
    var newName by rememberSaveable { mutableStateOf("") }

    val filteredPartners = remember(query, partners) {
        if (query.isBlank()) partners
        else partners.filter { it.name.contains(query, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                expanded = true
                if (selectedPartner != null && it != selectedPartner.name) {
                    onSelected(null)
                }
            },
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            singleLine = true,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            filteredPartners.forEach { partner ->
                DropdownMenuItem(
                    text = { Text(partner.name) },
                    onClick = {
                        query = partner.name
                        onSelected(partner)
                        expanded = false
                    },
                )
            }
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(Spacing.sm))
                        Text("Add New Partner")
                    }
                },
                onClick = {
                    expanded = false
                    showAddNew = true
                },
            )
        }
    }

    if (showAddNew) {
        AlertDialog(
            onDismissRequest = { showAddNew = false },
            title = { Text("New Partner") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onCreateNew(newName.trim())
                            query = newName.trim()
                            newName = ""
                            showAddNew = false
                        }
                    },
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNew = false; newName = "" }) { Text("Cancel") }
            },
        )
    }
}
