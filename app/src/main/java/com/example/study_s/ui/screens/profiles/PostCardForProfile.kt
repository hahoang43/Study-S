package com.example.study_s.ui.screens.profiles

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.study_s.data.model.PostModel
import com.example.study_s.ui.navigation.Routes
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PostCardForProfile(
    post: PostModel,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clickable {
                navController.navigate("${Routes.PostDetail}/${post.postId}")
            }
    ) {

        // ===== HEADER: Avatar + Name + Time =====
        Row(verticalAlignment = Alignment.CenterVertically) {

            Image(
                painter = rememberAsyncImagePainter(
                    model = post.authorAvatarUrl ?: ""
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    post.authorName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                val timeStr = post.timestamp?.toDate()?.let {
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                } ?: ""

                Text(
                    timeStr,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ===== TEXT CONTENT (trắng tự nhiên, không nền) =====
        if (post.content.isNotEmpty()) {
            Text(
                post.content,
                fontSize = 15.sp,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        // ===== IMAGE (nếu có thì hiển thị, auto-fit) =====
        if (!post.imageUrl.isNullOrEmpty()) {
            Image(
                painter = rememberAsyncImagePainter(post.imageUrl),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .heightIn(min = 160.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // ===== FOOTER: LIKE & COMMENTS =====
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // like
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = Color.Black
                )
                Spacer(Modifier.width(6.dp))
                Text("${post.likesCount}")
            }

            Spacer(modifier = Modifier.width(20.dp))

            // comment
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = Color.Black
                )
                Spacer(Modifier.width(6.dp))
                Text("${post.commentsCount}")
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        // ===== LINE SEPARATOR (đường kẻ ngăn giữa bài viết) =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFDDDDDD))
        )
    }
}
