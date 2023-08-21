package net.lemontree.push

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import net.lemontree.push.model.PCClient
import net.lemontree.push.model.PCClientViewModel
import net.lemontree.push.model.PCClientViewModelFactory
import net.lemontree.push.ui.theme.MsglistenerTheme

private lateinit var sharedPref: SharedPreferences
val DEFAULT_PORT = "14756"


class PCConfigActivity : ComponentActivity() {
    private lateinit var pcClientViewModel: PCClientViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPref = getSharedPreferences("PCList", Context.MODE_PRIVATE)
        pcClientViewModel = ViewModelProvider(
            this,
            PCClientViewModelFactory(sharedPref)
        ).get(PCClientViewModel::class.java)

        setContent {
            MsglistenerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    MyScreen(pcClientViewModel, this)
                }
            }
        }
    }
}

@Composable
fun MyScreen(pcClientViewModel: PCClientViewModel, activity: PCConfigActivity) {
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar {
            TopAppBar(title = {
                Text(text = "电脑管理", color = Color.White)
            }, elevation = 0.dp, navigationIcon = {
                IconButton(
                    content = {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null,
                            tint = Color.White
                        )
                    },
                    onClick = {
                        activity.finish()
                    })
            }, actions = {
                IconButton(
                    content = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White
                        )
                    },
                    onClick = {
                        showDialog = true
                    }
                )
            }
            )
        }
    }) {
        Greeting(pcClientViewModel)
    }
    if (showDialog) {
        TextInputDialog(
            showDialog = showDialog,
            onConfirm = { it, _ ->
                showDialog = false
                if (it.port == "") {
                    it.port = DEFAULT_PORT
                }
                pcClientViewModel.addPCItem(it)
            },
            onDismiss = { showDialog = false },
            pcClient = null,
            idx = null
        )
    }

}

@Composable
fun TextInputDialog(
    showDialog: Boolean,
    onConfirm: (PCClient, Int?) -> Unit,
    onDismiss: () -> Unit,
    pcClient: PCClient? = null, // 添加可选的 PCClient 参数
    idx: Int?
) {
    if (showDialog) {
        var ipValue by remember { mutableStateOf(pcClient?.ip ?: "") }
        var portValue by remember { mutableStateOf(pcClient?.port ?: "") }
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text(if (pcClient == null) "新增电脑" else "修改电脑") },
            text = {
                Column(
                    modifier = Modifier.padding(6.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    TextField(
                        value = ipValue,
                        onValueChange = {
                            ipValue = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(text = "请输入电脑IP地址，通常以192.168开头") }
                    )
                    TextField(
                        value = portValue,
                        onValueChange = {
                            portValue = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(text = "请输入端口，不输入则默认14756") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm(PCClient(ipValue, ipValue, portValue), idx)
                    },
                ) {
                    Text(if (pcClient == null) "新增" else "保存")
                }
            }
        )
    }
}

@Composable
fun Greeting(pcClientViewModel: PCClientViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    var pcClient by remember { mutableStateOf(PCClient("", "", "")) }
    var pcIndex by remember { mutableStateOf(0) }
    val pcList = pcClientViewModel.pcItems
    if (pcList.size > 0) {
        LazyColumn {
            itemsIndexed(pcList) { idx, pc ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = pc.ip + ":" + pc.port,
                        style = TextStyle(
                            fontSize = 18.sp,
                        ),
                        modifier = Modifier.padding(10.dp)
                    )
                    IconButton(
                        content = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.Black
                            )
                        },
                        onClick = {
                            showDialog = true
                            pcClient = pc
                            pcIndex = idx
                        })
                    IconButton(
                        content = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color.Red
                            )
                        },
                        onClick = {
                            pcClientViewModel.delItem(idx)
                        })
                }
            }
        }
        if (showDialog) {
            TextInputDialog(
                showDialog = showDialog,
                onConfirm = { pcClient, i ->
                    if (i != null) {
                        pcClientViewModel.updatePCItem(i, pcClient)
                    } else {
                        pcClientViewModel.addPCItem(pcClient)
                    }
                    showDialog = false
                },
                onDismiss = { showDialog = false },
                pcClient,
                idx = pcIndex
            )
        }
    }
}

