package com.katadojo.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.katadojo.app.ui.theme.*

@Composable
fun DojoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    singleLine: Boolean = true,
    minLines: Int = 1,
    helper: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        isError = error != null,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        supportingText = if (error != null || helper != null) {
            { Text(error ?: helper.orEmpty()) }
        } else null,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Rice,
            unfocusedTextColor = Rice,
            focusedContainerColor = Charcoal,
            unfocusedContainerColor = Charcoal,
            errorContainerColor = Charcoal,
            focusedBorderColor = DojoRed,
            unfocusedBorderColor = CharcoalBorder,
            errorBorderColor = ErrorRed,
            focusedLabelColor = DojoRed,
            unfocusedLabelColor = Stone,
            errorLabelColor = ErrorRed,
            cursorColor = DojoRed,
            errorCursorColor = ErrorRed,
            focusedSupportingTextColor = Stone,
            unfocusedSupportingTextColor = Stone,
            errorSupportingTextColor = ErrorRed
        )
    )
}

/** Horizontally scrolling filter chips. */
@Composable
fun <T> FilterRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options.size) { i ->
            val option = options[i]
            SelectChip(label = label(option), selected = option == selected, onClick = { onSelect(option) })
        }
    }
}

/** Options laid out in rows of [columns] equal-width chips. */
@Composable
fun <T> ChipGrid(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { option ->
                    SelectChip(
                        label = label(option),
                        selected = option == selected,
                        onClick = { onSelect(option) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
