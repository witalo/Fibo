package com.example.fibo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.fibo.ui.screens.person.FilterBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onMenuClick: () -> Unit,
    onDateSelected: (String) -> Unit,
    currentDate: String,
    onTitleClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onTitleClick() }
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar cliente",
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menú"
                )
            }
        },
        actions = {
            DateSelector(
                currentDate = currentDate,
                onDateSelected = onDateSelected
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

// Versión que usa LocalMenuClickHandler (para uso con AppScaffold)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBarWithSearch(
    title: String,
    onDateSelected: (String) -> Unit,
    currentDate: String,
    onTitleClick: () -> Unit
) {
    val onMenuClick = LocalMenuClickHandler.current
    
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onTitleClick() }
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar cliente",
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menú"
                )
            }
        },
        actions = {
            DateSelector(
                currentDate = currentDate,
                onDateSelected = onDateSelected
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBarWithFilter(
    title: String,
    onFilterClick: () -> Unit,
    onTitleClick: () -> Unit,
    filterBadgeCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val onMenuClick = LocalMenuClickHandler.current
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onTitleClick() }
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (filterBadgeCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterBadge(count = filterBadgeCount)
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menú"
                )
            }
        },
        actions = {
            IconButton(
                onClick = onFilterClick,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(40.dp)
            ) {
                BadgedBox(
                    badge = {
                        if (filterBadgeCount > 0) {
                            Badge {
                                Text(filterBadgeCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filtros",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        modifier = modifier
    )
}
// Versión simple solo con título (para pantallas como Productos)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleAppTopBar(
    title: String,
    backgroundColor: Color = Color.Black,
    contentColor: Color = Color.White
) {
    val onMenuClick = LocalMenuClickHandler.current
    
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menú"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor,
            titleContentColor = contentColor,
            navigationIconContentColor = contentColor
        )
    )
}

// Versión personalizable para cualquier necesidad
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAppTopBar(
    title: String,
    actions: @Composable () -> Unit = {},
    backgroundColor: Color = Color.Black,
    contentColor: Color = Color.White
) {
    val onMenuClick = LocalMenuClickHandler.current
    
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menú"
                )
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor,
            titleContentColor = contentColor,
            navigationIconContentColor = contentColor,
            actionIconContentColor = contentColor
        )
    )
}