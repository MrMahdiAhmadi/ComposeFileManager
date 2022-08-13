package ir.mahdi.composefilemanager

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import ir.mahdi.composefilemanager.ui.theme.ComposeFileManagerTheme
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ComposeFileManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val permissionsState = rememberMultiplePermissionsState(
                        permissions = listOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(key1 = lifecycleOwner, effect = {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_START) {
                                permissionsState.launchMultiplePermissionRequest()
                            }

                        }
                        lifecycleOwner.lifecycle.addObserver(observer)

                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    })

                    permissionsState.permissions.forEach { perm ->
                        when (perm.permission) {
                            Manifest.permission.READ_EXTERNAL_STORAGE -> {
                                if (perm.status.isGranted) {
                                    Log.i("TAG", "permissions: ReadExternalStorage Granted")
                                    PermissionGranted()
                                } else {
                                    Log.i("TAG", "permissions: ReadExternalStorage rejected")
                                }

                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun PermissionGranted() {
        val context = LocalContext.current
        val activity = context as Activity
        val snackBarState = remember {
            SnackbarHostState()
        }
        val scaffoldState = rememberScaffoldState(snackbarHostState = snackBarState)
        val scope = rememberCoroutineScope()

        val home = Environment.getExternalStorageDirectory().path
        val path = remember {
            mutableStateOf(Environment.getExternalStorageDirectory().path)
        }
        var isHome by remember {
            mutableStateOf(true)
        }
        isHome = path.value == home

        var directory by remember {
            mutableStateOf("")
        }
        directory = File(path.value).name

        var rvItems by remember {
            mutableStateOf(File(path.value).listFiles())
        }
        rvItems = File(path.value).listFiles()

        var pathList by remember {
            mutableStateOf(ArrayList<String>())
        }
        var showDialog by remember {
            mutableStateOf(false)
        }
        var actionItem by remember {
            mutableStateOf(File(path.value).listFiles().last())
        }
        actionItem = null
        var showDeleteDialog by remember {
            mutableStateOf(false)
        }

        Scaffold(scaffoldState = scaffoldState, topBar = {
            TopAppBar(title = {
                Text(text = stringResource(id = R.string.app_name))
            },
                actions = {
                    IconButton(onClick = {
                        showDialog = true
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_outline_create_new_folder_24),
                            contentDescription = stringResource(R.string.create_new_folder)
                        )
                    }
                })
        }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(it)
            ) {

                BackAndTitle(isHome = isHome, title = directory) {
                    path.value = pathList.last()
                    pathList.removeAt(pathList.lastIndexOf(pathList.last()))
                }
                Lazy(rvItems,
                    onMenuClick = { action, item ->
                        when (action) {
                            "DELETE" -> {
                                actionItem = item
                                showDeleteDialog = true
                            }
                            "RENAME" -> {
                                actionItem = item
                                showDialog = true
                            }
                        }

                    }) { item ->
                    if (item.isDirectory) {
                        pathList.add(path.value)
                        path.value = item.absolutePath
                    } else {
                        //TODO
                    }
                }
                BackHandler() {
                    if (home == path.value) { //if in root folder
                        activity.finish()
                    } else {
                        path.value = pathList.last()
                        pathList.removeAt(pathList.lastIndexOf(pathList.last()))
                    }

                }

                // show create or edit folder name
                if (showDialog) {
                    // if dialog open for rename, set file name in name
                    // else set "" for create new folder
                    var name = if (actionItem != null) actionItem.name else ""
                    FolderDialog(name) { folderName, isCreated ->
                        if (folderName != "") {
                            val a = path.value //for refresh list after changes
                            if (isCreated) { //if dialog opened for create file
                                val newFolder = File(path.value + File.separator + folderName)
                                if (!newFolder.exists()) {
                                    if (newFolder.mkdir()) {
                                        scope.launch {
                                            scaffoldState.snackbarHostState.showSnackbar(
                                                "$folderName created"
                                            )
                                        }
                                        // for refresh list
                                        path.value = ""
                                        path.value = a
                                    }
                                } else {
                                    scope.launch {
                                        scaffoldState.snackbarHostState.showSnackbar(
                                            "$folderName is already exists"
                                        )
                                    }
                                }
                            } else { //if dialog opened for rename file
                                val from = File(actionItem.parent, actionItem.name)
                                val to = File(actionItem.parent, folderName)
                                if (from.exists())
                                    if (!to.exists())
                                        from.renameTo(to)

                                // for refresh list
                                path.value = ""
                                path.value = a
                            }
                        }
                        actionItem = null
                        showDialog = false
                    }
                }

                //show delete dialog
                if (showDeleteDialog) {
                    DeleteDialog(itemName = actionItem.name) { isConfirmDelete ->
                        if (isConfirmDelete) {
                            val name = actionItem.name
                            if (actionItem.delete()) {
                                scope.launch {
                                    scaffoldState.snackbarHostState.showSnackbar(
                                        "$name has been deleted"
                                    )
                                }
                            } else {
                                scope.launch {
                                    scaffoldState.snackbarHostState.showSnackbar(
                                        "Unknown error"
                                    )
                                }
                            }
                        }
                        actionItem = null
                        showDeleteDialog = false
                    }
                }
            }
        }
    }
}

@Composable
fun FolderDialog(name: String, onFinish: (String, Boolean) -> Unit) {
    val input = remember { mutableStateOf(name) }
    AlertDialog(onDismissRequest = {
        onFinish("", name == "")
    },
        title = {
        },
        text = {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                value = input.value,
                label = { Text(text = stringResource(R.string.folder_name)) },
                onValueChange = { input.value = it },
                singleLine = true

            )
        }, confirmButton = {
            TextButton(
                onClick = {
                    if (input.value.isNotEmpty())
                        onFinish(input.value, name == "")
                }
            ) {
                Text(text = if(name=="") {
                    stringResource(R.string.create)
                } else {
                    stringResource(R.string.rename)
                }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onFinish("", name == "") }
            ) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun DeleteDialog(itemName: String, onFinish: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = { onFinish(false) },
        title = { Text(text = stringResource(R.string.delete)) },
        text = {
            Text(text = "Are you sure you want to delete $itemName?")
        },
        confirmButton = {
            TextButton(onClick = { onFinish(true) }) {
                Text(
                    text = stringResource(R.string.delete),
                    color = Color.Red
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { onFinish(false) }) {
                Text(text = stringResource(id = R.string.cancel))
            }
        })
}



