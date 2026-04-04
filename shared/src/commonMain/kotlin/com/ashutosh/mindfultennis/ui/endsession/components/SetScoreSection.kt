package com.ashutosh.mindfultennis.ui.endsession.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
    val sets: List<SetScoreRow> = listOf(SetScoreRow()),
) {
    /** Convenience: match type of the first set (for backward compat). */
    val matchType: MatchType get() = sets.firstOrNull()?.matchType ?: MatchType.SINGLES
    val opponent2: Opponent? get() = sets.firstOrNull()?.opponent2
    val partner: Partner? get() = sets.firstOrNull()?.partner
}

data class SetScoreRow(
    val matchType: MatchType = MatchType.SINGLES,
    val opponent1: Opponent? = null,
    val opponent2: Opponent? = null,
    val partner: Partner? = null,
    val userScore: String = "",
    val opponentScore: String = "",
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
                    text = "Set Scores",
                    style = MaterialTheme.typography.titleSmall,
                )
                Row {
                    TextButton(onClick = onEdit) { Text("Edit") }
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = "Clear set scores")
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            // Set scores summary
            data.sets
                .filter { it.userScore.isNotBlank() && it.opponentScore.isNotBlank() }
                .forEachIndexed { index, row ->
                    val typeLabel = row.matchType.name.lowercase().replaceFirstChar { it.uppercase() }
                    val oppLabel = row.opponent1?.name?.let { " vs $it" } ?: ""
                    Text(
                        text = "Set ${index + 1} ($typeLabel): ${row.userScore}-${row.opponentScore}$oppLabel",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var sets by remember { mutableStateOf(initialData.sets.ifEmpty { listOf(SetScoreRow()) }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Scores") },
        confirmButton = {
            Button(
                onClick = {
                    onSave(SetScoreInputData(sets = sets))
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
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .semantics {
                        contentDescription = "Set Scores dialog"
                    },
            ) {
                sets.forEachIndexed { index, setRow ->
                    if (index > 0) {
                        Spacer(Modifier.height(Spacing.md))
                    }

                    SetScoreBlockInput(
                        setNumber = index + 1,
                        setRow = setRow,
                        opponents = opponents,
                        partners = partners,
                        onSetRowChanged = { updated ->
                            sets = sets.toMutableList().also { it[index] = updated }
                        },
                        onCreateOpponent = onCreateOpponent,
                        onCreatePartner = onCreatePartner,
                        canRemove = index > 0,
                        onRemove = {
                            sets = sets.toMutableList().also { it.removeAt(index) }
                        },
                    )
                }

                Spacer(Modifier.height(Spacing.sm))

                TextButton(
                    onClick = {
                        // Copy match type & players from the last set as defaults
                        val lastSet = sets.lastOrNull() ?: SetScoreRow()
                        sets = sets + SetScoreRow(
                            matchType = lastSet.matchType,
                            opponent1 = lastSet.opponent1,
                            opponent2 = lastSet.opponent2,
                            partner = lastSet.partner,
                        )
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

/**
 * A single set block: match type selector, player dropdowns, and score input.
 */
@Composable
private fun SetScoreBlockInput(
    setNumber: Int,
    setRow: SetScoreRow,
    opponents: List<Opponent>,
    partners: List<Partner>,
    onSetRowChanged: (SetScoreRow) -> Unit,
    onCreateOpponent: (String) -> Unit,
    onCreatePartner: (String) -> Unit,
    canRemove: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Set $setNumber",
                style = MaterialTheme.typography.titleSmall,
            )
            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove set $setNumber")
                }
            }
        }

        // Match Type
        Text("Match Type:", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(Spacing.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = setRow.matchType == MatchType.SINGLES,
                onClick = {
                    onSetRowChanged(
                        setRow.copy(
                            matchType = MatchType.SINGLES,
                            opponent2 = null,
                            partner = null,
                        ),
                    )
                },
            )
            Text("Singles", modifier = Modifier.padding(end = Spacing.md))
            RadioButton(
                selected = setRow.matchType == MatchType.DOUBLES,
                onClick = { onSetRowChanged(setRow.copy(matchType = MatchType.DOUBLES)) },
            )
            Text("Doubles")
        }

        Spacer(Modifier.height(Spacing.sm))

        // Doubles-only: Partner
        if (setRow.matchType == MatchType.DOUBLES) {
            Text("Partner:", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(Spacing.xs))
            PartnerDropdown(
                selectedPartner = setRow.partner,
                partners = partners,
                onSelected = { onSetRowChanged(setRow.copy(partner = it)) },
                onCreateNew = onCreatePartner,
                label = "Partner",
            )
            Spacer(Modifier.height(Spacing.sm))
        }

        // Opponent 1 (always)
        Text("Opponent 1:", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(Spacing.xs))
        OpponentDropdown(
            selectedOpponent = setRow.opponent1,
            opponents = opponents,
            onSelected = { onSetRowChanged(setRow.copy(opponent1 = it)) },
            onCreateNew = onCreateOpponent,
            label = "Opponent 1",
        )

        // Doubles-only: Opponent 2
        if (setRow.matchType == MatchType.DOUBLES) {
            Spacer(Modifier.height(Spacing.sm))
            Text("Opponent 2:", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(Spacing.xs))
            OpponentDropdown(
                selectedOpponent = setRow.opponent2,
                opponents = opponents,
                onSelected = { onSetRowChanged(setRow.copy(opponent2 = it)) },
                onCreateNew = onCreateOpponent,
                label = "Opponent 2",
            )
        }

        Spacer(Modifier.height(Spacing.sm))

        // Score row
        Text("Score:", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(Spacing.xs))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            OutlinedTextField(
                value = setRow.userScore,
                onValueChange = {
                    onSetRowChanged(setRow.copy(userScore = it.filter { c -> c.isDigit() }.take(2)))
                },
                modifier = Modifier.width(64.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                label = { Text("You", maxLines = 1) },
            )
            Text("\u2013", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = setRow.opponentScore,
                onValueChange = {
                    onSetRowChanged(setRow.copy(opponentScore = it.filter { c -> c.isDigit() }.take(2)))
                },
                modifier = Modifier.width(64.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                label = { Text("Opp", maxLines = 1) },
            )
        }
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
