package com.classlog.app.ui.setup.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EditProfileDialog(
    currentName: String,
    currentInstitution: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, institution: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var institution by remember { mutableStateOf(currentInstitution) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Teacher Profile") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Teacher / Tutor Name") },
                    placeholder = { Text("e.g. Prof. Sharma") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = institution,
                    onValueChange = { institution = it },
                    label = { Text("Coaching / Institution (Optional)") },
                    placeholder = { Text("e.g. Apex Academy") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), institution.trim()) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
