package com.example.study_s.ui.screens.components

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import com.example.study_s.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onNavIconClick: () -> Unit = {},
    onNotificationClick: () -> Unit,
    onSearchClick: () -> Unit,
    onChatClick: () -> Unit,
    notificationCount: Int = 0,
    messageCount: Int = 0 // ✅ ADD NEW PARAMETER
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "STUDY-S",
                color = MaterialTheme.colorScheme.onSurface, // Dùng màu của theme
                fontWeight = FontWeight.Bold,
                fontSize = 23.sp,
                fontFamily = FontFamily.Serif
            )
        },
        navigationIcon = {
            // ✅ WRAP CHAT ICON IN A BADGEDBOX
            IconButton(onClick = onChatClick) {
                BadgedBox(
                    badge = {
                        if (messageCount > 0) {
                            Badge(modifier = Modifier.offset(x = (-4).dp, y = 6.dp)) {
                                Text(
                                    text = if (messageCount > 99) "99+" else messageCount.toString(),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.message),
                        contentDescription = "Chat",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Tìm kiếm",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onNotificationClick) {
                BadgedBox(
                    badge = {
                        if (notificationCount > 0) {
                            Badge(
                                modifier = Modifier.offset(x = (-4).dp, y = 6.dp)
                            ) {
                                Text(
                                    text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Thông báo",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Preview(showBackground = true)
@Composable
fun TopBarPreview() {
    TopBar(
        onNavIconClick = {},
        onNotificationClick = {},
        onSearchClick = {},
        onChatClick = {},
        notificationCount = 3,
        messageCount = 5
    )
}
