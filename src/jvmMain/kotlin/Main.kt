// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:Suppress("FunctionName")

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

val paddingModifier: Modifier
    get() = Modifier.padding(5.dp)

@Composable
@Preview
fun App() {
    val keysState = remember { mutableStateOf(loadKeys()) }

    MaterialTheme {
        var tabIndex by remember { mutableStateOf(0) }
        val tabTitles = listOf("Encrypt/Decrypt message", "KeyManagement")
        Column {
            TabRow(
                modifier = paddingModifier,
                selectedTabIndex = tabIndex
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        modifier = paddingModifier,
                        selected = tabIndex == index,
                        onClick = { tabIndex = index },
                        text = { Text(text = title) }
                    )
                }
            }
            when (tabIndex) {
                0 -> MessageEncryptionView(keysState)
                1 -> KeyManagementView(keysState)
            }
        }
    }
}

private fun loadKeys(): List<KeyInfo> {
    return GpgLauncher.getKeys().getOrNull() ?: emptyList()
}


@Composable
fun KeyManagementView(keysState: MutableState<List<KeyInfo>>) {
    var keys by keysState
    Column {
        Row {
            SimpleButton("Reload keys") { keys = loadKeys() }
            Box {
                var newKeyExpanded by remember { mutableStateOf(false) }
                var newKeyText by remember { mutableStateOf("") }
                if (!newKeyExpanded) {
                    SimpleButton("Add new key") {
                        newKeyExpanded = true
                    }
                } else {
                    Column {
                        Row {
                            SimpleButton("Add") {
                                when (val result = GpgLauncher.addNewKey(newKeyText)) {
                                    is Result.Success -> {
                                        newKeyExpanded = false
                                        newKeyText = ""
                                        keysState.value = loadKeys()
                                    }
                                    is Result.Error -> {
                                        newKeyText = result.stringValue
                                    }
                                }
                            }
                            SimpleButton("Cancel") {
                                newKeyExpanded = false
                                newKeyText = ""
                            }
                        }
                        MyTextField(newKeyText, "Public Key") { newKeyText = it }
                    }
                }
            }
        }
        Column(paddingModifier) {
            for (key in keys) {
                var isExpanded by remember { mutableStateOf(false) }
                Column(paddingModifier.clickable { isExpanded = !isExpanded }) {
                    Text(key.name, fontWeight = FontWeight.Bold)
                    if (isExpanded) {
                        Text(key.toString(), fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }


}

@Composable
fun MessageEncryptionView(keysState: MutableState<List<KeyInfo>>) {
    val keyInfos by keysState

    val encryptedTextState = remember { mutableStateOf("") }
    var encryptedText by encryptedTextState

    val decryptedTextState = remember { mutableStateOf("") }
    var decryptedText by decryptedTextState

    var sender by remember { mutableStateOf(keyInfos[0]) }
    var recipient by remember { mutableStateOf(keyInfos[0]) }

    Row {
        MyColumn {
            MyMenu(keyInfos, recipient, "Recipient") {
                recipient = it
            }

            RowWithClearButton(encryptedTextState) {
                SimpleButton("Decrypt") {
                    decryptedText = GpgLauncher.decrypt(encryptedText).stringValue
                }
            }

            MyTextField(
                value = encryptedText,
                onValueChange = { encryptedText = it },
                label = "encrypted text"
            )
        }

        MyColumn {
            MyMenu(keyInfos, sender, "Sender") {
                sender = it
            }
            RowWithClearButton(decryptedTextState) {
                Row {
                    SimpleButton("Encrypt") {
                        encryptedText = GpgLauncher.encrypt(sender, recipient, decryptedText).stringValue
                    }
                    SimpleButton("Sign") {
                        encryptedText = GpgLauncher.sign(decryptedText).stringValue
                    }
                }
            }

            MyTextField(
                value = decryptedText,
                onValueChange = { decryptedText = it },
                label = "decrypted text"
            )
        }
    }
}

@Composable
fun RowWithClearButton(stateToClear: MutableState<String>, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        content()

        SimpleButton("Clear") { stateToClear.value = "" }
    }
}

@Composable
fun SimpleButton(text: String, onClick: () -> Unit) {
    Button(modifier = paddingModifier, onClick = onClick) {
        Text(text)
    }
}

@Composable
fun MyMenu(keyInfos: List<KeyInfo>, chosenKey: KeyInfo, prefix: String, onClick: (KeyInfo) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Text(
            text = "$prefix: ${chosenKey.nameAndValidity}",
            modifier = paddingModifier.clickable { expanded = !expanded }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            keyInfos.forEach { keyInfo ->
                DropdownMenuItem(
                    onClick = {
                        onClick(keyInfo)
                        expanded = false
                    }
                ) {
                    Text(text = keyInfo.nameAndValidity)
                }
            }
        }
    }
}

val KeyInfo.nameAndValidity: String
    get() = name + if (isValid) "" else " [Invalid]"

@Composable
fun ColumnScope.MyTextField(value: String, label: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = false,
        textStyle = TextStyle(),
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()

    )
}

@Composable
fun RowScope.MyColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        paddingModifier
            .weight(1f)
            .fillMaxWidth(),
        content = content
    )

}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
