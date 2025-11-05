package com.example.study_s.ui.screens.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.study_s.data.model.Message
import com.example.study_s.viewmodel.ChatViewModel
import com.example.study_s.viewmodel.GroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatGroupScreen(
    navController: NavHostController,
    groupId: String,
    groupViewModel: GroupViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
    // Replace with the actual logged-in user's ID
    currentUserId: String = "currentUserId"
) {
    groupViewModel.getGroupById(groupId)
    val group by groupViewModel.group.collectAsState()
    val messages by chatViewModel.messages.collectAsState()

    LaunchedEffect(groupId) {
        chatViewModel.listenToGroupMessages(groupId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = group?.groupName ?: "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Online",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            MessageInput(
                onSendMessage = { message ->
                    chatViewModel.sendMessage(groupId, currentUserId, message)
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                reverseLayout = true,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages.reversed()) { message ->
                    MessageItem(message = message, currentUserId = currentUserId)
                }
            }
        }
    }
}

@Composable
fun MessageItem(message: Message, currentUserId: String) {
    val isCurrentUser = message.senderId == currentUserId
    val alignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .background(backgroundColor, RoundedCornerShape(12.dp))
                .padding(12.dp)
                .widthIn(max = 300.dp) // Max width for a message bubble
        ) {
            if (!isCurrentUser) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        }
    }
}

@Composable
fun MessageInput(onSendMessage: (String) -> Unit) {
    var text by remember { mutableStateOf("" ) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Type a message...") }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = {
            if (text.isNotBlank()) {
                onSendMessage(text)
                text = ""
            }
        }) {
            Text("Send")
        }
    }
}
