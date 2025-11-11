package com.example.study_s.ui.screens.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.study_s.R
import com.example.study_s.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.tooling.preview.Preview
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StragerScreen(
    navController: NavController,
    userId: String
) {
    val db = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var userProfile by remember { mutableStateOf<User?>(null) }
    var userPosts by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isFollowing by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        loading = true
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc -> userProfile = doc.toObject(User::class.java) }

        db.collection("posts").whereEqualTo("authorId", userId).get()
            .addOnSuccessListener { snap -> userPosts = snap.documents.mapNotNull { it.data } }

        if (currentUserId != null && currentUserId != userId) {
            db.collection("users").document(userId)
                .collection("followers").document(currentUserId)
                .get()
                .addOnSuccessListener { doc -> isFollowing = doc.exists() }
        }
        loading = false
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (userProfile != null) {
        StragerContent(
            navController = navController,
            user = userProfile!!,
            posts = userPosts,
            isFollowing = isFollowing,
            onToggleFollow = {
                val fRef = db.collection("users").document(userId)
                    .collection("followers").document(currentUserId!!)
                if (isFollowing) {
                    fRef.delete()
                    isFollowing = false
                } else {
                    fRef.set(mapOf("since" to System.currentTimeMillis()))
                    isFollowing = true
                }
            }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Không tìm thấy người dùng.")
        }
    }
}

@Composable
private fun StragerContent(
    navController: NavController,
    user: User,
    posts: List<Map<String, Any>> ,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ================= HEADER =================
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFFB3E5FC))
            ) {
                // Nút quay lại
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .padding(16.dp)
                        .offset(y = 24.dp)
                        .align(Alignment.TopStart)
                        .background(Color.White.copy(alpha = 0.8f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = Color.Black
                    )
                }

                // Avatar + tên + nút
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .offset(y = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = user.avatarUrl ?: R.drawable.profile_placeholder,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color.White, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        user.name ?: "Người dùng",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onToggleFollow) {
                            Text(if (isFollowing) "Đang theo dõi" else "Theo dõi")
                        }
                        OutlinedButton(onClick = { /* TODO: Nhắn tin */ }) {
                            Text("Nhắn tin")
                        }
                    }
                }
            }
            Spacer(Modifier.height(60.dp))
        }

        // ================= BÀI VIẾT =================
        item {
            Text(
                "Bài viết",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 16.dp)
            )
        }

        items(posts) { post ->
            val date = (post["timestamp"] as? com.google.firebase.Timestamp)?.toDate()
            val formattedDate = if (date != null) {
                java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date)
            } else "Vừa xong"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // ==== Header: Avatar + Tên + Thời gian
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = user.avatarUrl ?: R.drawable.profile_placeholder,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                user.name ?: "Người dùng",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            Text(formattedDate, fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // ==== Nội dung bài viết
                    Text(
                        text = post["content"] as? String ?: "",
                        fontSize = 16.sp,
                        color = Color(0xFF333333)
                    )

                    Spacer(Modifier.height(10.dp))
                    Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)

                    // ==== Hàng icon Like - Comment - Share
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ❤️ Like
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = "Thích", tint = Color.Gray)
                            Spacer(Modifier.width(4.dp))
                            Text(((post["likesCount"] as? Long) ?: 0).toString(), color = Color.Black)
                        }

                        // 💬 Comment
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Bình luận", tint = Color.Gray)
                            Spacer(Modifier.width(4.dp))
                            Text(((post["commentsCount"] as? Long) ?: 0).toString(), color = Color.Black)
                        }

                        // 🔗 Share
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = "Chia sẻ", tint = Color.Gray)
                            Spacer(Modifier.width(4.dp))
                            Text(((post["sharesCount"] as? Long) ?: 0).toString(), color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun StragerScreenPreview() {
    val fakeUser = User(
        userId = "fakeUserId",
        name = "Đoàn Thị Yến",
        email = "yen@example.com",
        avatarUrl = null,
        bio = "Bio mẫu preview",
        createdAt = Date()
    )
    val navController = rememberNavController()
    MaterialTheme {
        StragerContent(
            navController = navController,
            user = fakeUser,
            posts = emptyList(),
            isFollowing = false,
            onToggleFollow = {}
        )
    }
}
