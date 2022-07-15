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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

val paddingModifier: Modifier
    get() = Modifier.padding(5.dp)

@Composable
@Preview
fun App() {
    val keysState = remember { mutableStateOf(GpgLauncher.getKeys().unwrap()) }

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


@Composable
fun KeyManagementView(keysState: MutableState<List<KeyInfo>>) {
    var keys by keysState


}

@Composable
fun MessageEncryptionView(keysState: MutableState<List<KeyInfo>>) {
    val keyInfos by keysState
    var encryptedText by remember { mutableStateOf("") }
    var decryptedText by remember { mutableStateOf("") }

    var sender by remember { mutableStateOf(keyInfos[0]) }
    var recipient by remember { mutableStateOf(keyInfos[0]) }

    Row {
        MyColumn {
            MyMenu(keyInfos, recipient, "Recipient") {
                recipient = it
            }

            Button(
                modifier = paddingModifier,
                onClick = { decryptedText = GpgLauncher.decrypt(encryptedText).stringValue }
            ) {
                Text("Decrypt")
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

            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Button(
                    modifier = paddingModifier,
                    onClick = { encryptedText = GpgLauncher.encrypt(sender, recipient, decryptedText).stringValue }
                ) {
                    Text("Encrypt")
                }
                Button(
                    modifier = paddingModifier,
                    onClick = { encryptedText = GpgLauncher.sign(decryptedText).stringValue }
                ) {
                    Text("Sign")
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
