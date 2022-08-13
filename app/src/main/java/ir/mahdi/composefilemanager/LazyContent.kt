package ir.mahdi.composefilemanager

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun BackAndTitle(isHome: Boolean, title: String, onBackClick: () -> Unit) {
    Row(
        Modifier
            .padding(16.dp)
    ) {
        if (isHome)
            Icon(imageVector = Icons.Outlined.Home, contentDescription = stringResource(R.string.home))
        else
            Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back),
                Modifier.clickable { onBackClick() })

        Spacer(Modifier.padding(horizontal = 8.dp))
        Text(text = title)
    }
}

@Composable
fun Lazy(list: Array<File>, onMenuClick: (String,File) -> Unit, onItemClick: (File) -> Unit) {

    val state= rememberLazyListState()
    LazyColumn(state = state) {
        itemsIndexed(list) { _, item ->
            LazyItem(item,
                onMenuClick = { action,item ->
                    onMenuClick(action,item)
                }) {
                onItemClick(it)
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItem(item: File, onMenuClick: (String,File) -> Unit, onItemClick: (File) -> Unit) {

    var showDropdownMenu by remember {
        mutableStateOf(false)
    }
    Row(
        Modifier
            .combinedClickable(onClick = {
                onItemClick(item)
            },
                onLongClick = {
                    showDropdownMenu = true
                })
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween

    ) {
        Row {
            if (item.isDirectory)
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_folder_24),
                    contentDescription = stringResource(R.string.folder)
                )
            else
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_insert_drive_file_24),
                    contentDescription = stringResource(R.string.file)
                )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = item.name)
        }
        Box() {
            Icon(
                painterResource(id = R.drawable.ic_outline_more_vert_24),
                stringResource(R.string.more),
                modifier = Modifier.clickable { showDropdownMenu = true }
            )
            OpenMenu(showDropdownMenu) { action ->
                if (action != "")
                    onMenuClick(action,item)
                    showDropdownMenu = false
            }
        }
    }
    Divider(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 16.dp)
    )

}

@Composable
fun OpenMenu(expanded: Boolean, onItemClick: (String) -> Unit) {

    DropdownMenu(expanded = expanded, onDismissRequest = { onItemClick("") }) {
        DropdownMenuItem(onClick = { onItemClick("RENAME") }) {
            Text(text = "Rename")
        }
        DropdownMenuItem(onClick = { onItemClick("DELETE") }) {
            Text(text = "Delete")
        }
    }

}

