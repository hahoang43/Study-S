package com.example.study_s.ui.screens.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.study_s.R

@Composable
fun ChatGroupScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        ChatTopBar()
        Divider(color = Color(0xFFECECEC))
        MessageList(messages = sampleMessages, modifier = Modifier.weight(1f))
        MessageInput(onSend = { })
    }
}

@Composable
fun ChatTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color.Black,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text("Nhóm CNTT", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("11 thành viên", fontSize = 13.sp, color = Color.Gray)
        }
    }
}

data class Message(
    val sender: String,
    val text: String? = null,
    val time: String,
    val isMine: Boolean,
    val avatarRes: Int? = null,
    val imageRes: Int? = null
)

val sampleMessages = listOf(
    Message("Bạn", "Mọi người ơi, tối nay chúng mình học nhóm về SQL nhé", "5:41 AM", true),
    Message("Hà Võ", "oke nhé", "5:42 AM", false, R.drawable.avatar1),
    Message("Hà Võ", "8g nha", "5:42 AM", false, R.drawable.avatar1),
    Message("Hà Võ", "mình ôn phần JOIN và Subquery nha", "5:42 AM", false, R.drawable.avatar1),
    Message("Phú Nguyễn", "Mình tham gia được", "5:43 AM", false, R.drawable.avatar2),
    Message("Duyên Trần", "Ok, tui tham gia nha", "5:43 AM", false, R.drawable.avatar3),
    Message("Bạn", "Tuyệt vời, chúng ta sẽ hiểu bài hơn", "5:44 AM", true, imageRes = R.drawable.happy_gif)
)

@Composable
fun MessageList(messages: List<Message>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        reverseLayout = false
    ) {
        item {
            Text(
                "Oct 10, 2025, 5:41 AM",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
        }

        items(messages) { message ->
            MessageBubble(message)
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    val bubbleColor = if (message.isMine) Color(0xFF000000) else Color(0xFFF1F1F1)
    val textColor = if (message.isMine) Color.White else Color.Black
    val alignment = if (message.isMine) Arrangement.End else Arrangement.Start

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = alignment
    ) {
        if (!message.isMine) {
            message.avatarRes?.let {
                Image(
                    painter = painterResource(id = it),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .align(Alignment.Bottom)
                )
                Spacer(Modifier.width(6.dp))
            }
        }

        Column(horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start) {
            if (!message.isMine) {
                Text(
                    message.sender,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            Column(
                modifier = Modifier
                    .background(bubbleColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                message.text?.let {
                    Text(it, color = textColor)
                }
                message.imageRes?.let {
                    Spacer(Modifier.height(8.dp))
                    Image(
                        painter = painterResource(id = it),
                        contentDescription = "image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInput(onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { }) {
            Icon(Icons.Default.EmojiEmotions, contentDescription = "Emoji", tint = Color.Gray)
        }
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Message...") },
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF2F2F2),
                unfocusedContainerColor = Color(0xFFF2F2F2),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(20.dp),
            maxLines = 3
        )
        IconButton(onClick = { }) {
            Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = Color.Gray)
        }
        IconButton(onClick = {
            if (text.isNotBlank()) {
                onSend(text)
                text = ""
            }
        }) {
            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFF007AFF))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewChatGroupScreen() {
    ChatGroupScreen()
}
