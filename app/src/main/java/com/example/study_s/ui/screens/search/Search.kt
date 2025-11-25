package com.example.study_s.ui.screens.search
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.study_s.R
import com.example.study_s.data.model.PostModel
import com.example.study_s.data.model.UserModel
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.ui.screens.components.TopBar // Đảm bảo bạn đã import TopBar
import com.example.study_s.viewmodel.SearchViewModel
import com.example.study_s.data.model.Group
import com.example.study_s.data.model.LibraryFile

import com.example.study_s.viewmodel.SearchState // Đảm bảo import SearchState từ ViewModel

@Composable
fun SearchScreen(navController: NavController, viewModel: SearchViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopBar(
                onNotificationClick = { navController.navigate(Routes.Notification) },
                onSearchClick = { /* Không cần hành động */ },
                onChatClick = { navController.navigate(Routes.Message) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onQueryChange(it) },
                placeholder = { Text("Tìm kiếm người dùng, bài viết ...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    viewModel.performSearch()
                    keyboardController?.hide()
                })
            )
            Spacer(modifier = Modifier.height(16.dp))

            CategoryChips(
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.onCategoryChange(it) }
            )
            Spacer(modifier = Modifier.height(16.dp))

            when {
                searchState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                searchState.error != null -> {
                    Text("Đã xảy ra lỗi: ${searchState.error}")
                }
                else -> {
                    SearchResultContent(
                        state = searchState,
                        selectedCategory = selectedCategory,
                        navController = navController
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChips(selectedCategory: String, onCategorySelected: (String) -> Unit) {
    val categories = listOf("Người dùng", "Nhóm", "Tài liệu", "Bài viết")

    // 1. Định nghĩa MÀU SẮC cho chip (Giữ nguyên)
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = Color.Black,
        selectedLabelColor = Color.White,
        containerColor = Color(0xFFF0F0F0),
        labelColor = Color.Black
    )

    // =================================================================
    // SỬA LỖI: XÓA BỎ HOÀN TOÀN BIẾN "chipBorderColors" KHÔNG SỬ DỤNG
    // val chipBorderColors = FilterChipDefaults.filterChipBorder(...) // <--- XÓA DÒNG NÀY
    // =================================================================

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                selected = (selectedCategory == category),
                onClick = { onCategorySelected(category) },
                label = { Text(category) },
                colors = chipColors,
                // Cách dùng BorderStroke là hoàn toàn chính xác và giữ nguyên
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selectedCategory == category) {
                        Color.Transparent
                    } else {
                        Color.Gray
                    }
                )
            )
        }
    }
}

// ----- CÁC COMPOSABLE HIỂN THỊ KẾT QUẢ (KHÔNG THAY ĐỔI) -----
@Composable
fun SearchResultContent(state: SearchState, selectedCategory: String, navController: NavController) {
    when (selectedCategory) {
        "Người dùng" -> UserResultList(state.userModels, navController)
        "Bài viết" -> PostResultList(state.posts, navController)
        "Nhóm" -> GroupResultList(state.groups, navController) // Sửa ở đây
        "Tài liệu" -> FileResultList(state.files, navController) // Sửa ở đây
    }
}

// ----- CÁC COMPOSABLE CŨ (KHÔNG ĐỔI)-----

@Composable
fun UserResultList(userModels: List<UserModel>, navController: NavController) {
    if (userModels.isEmpty()) {
        Text("Không tìm thấy người dùng nào.")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(userModels) { user -> UserResultItem(user, onClick = { navController.navigate("other_profile/${user.userId}") }) }
        }
    }
}

@Composable
fun PostResultList(posts: List<PostModel>, navController: NavController) {
    if (posts.isEmpty()) {
        Text("Không tìm thấy bài viết nào.")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(posts) { post -> PostResultItem(post, onClick = { navController.navigate("${Routes.PostDetail}/${post.postId}") }) }
        }
    }
}

// ✅ PHẦN SỬA LỖI 1: KÍCH HOẠT onCLICK CHO GROUP
// =========================================================================
@Composable
fun GroupResultList(groups: List<Group>, navController: NavController) {
    if (groups.isEmpty()) {
        Text("Không tìm thấy nhóm nào.")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(groups) { group ->
                GroupResultItem(
                    group = group,
                    onClick = {
                        Log.d("SearchScreen_Click", "Đã nhấn vào Group: ${group.groupName}, ID: ${group.groupId}")
                        // Điều hướng đến màn hình chat của nhóm
                        navController.navigate("${Routes.GroupChat}/${group.groupId}")
                    }
                )
            }
        }
    }
}

// =========================================================================
// ✅ PHẦN SỬA LỖI 2: KÍCH HOẠT onCLICK CHO FILE
// =========================================================================
@Composable
fun FileResultList(files: List<LibraryFile>, navController: NavController) {
    if (files.isEmpty()) {
        Text("Không tìm thấy tài liệu nào.")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(files) { file ->
                FileResultItem(
                    file = file,
                    onClick = {
                        Log.d("SearchScreen_Click", "Đã nhấn vào File: ${file.fileName}, URL: ${file.fileUrl}")
                        // Mã hóa URL để đảm bảo an toàn khi truyền đi
                        val encodedUrl = java.net.URLEncoder.encode(file.fileUrl, "UTF-8")
                        // Điều hướng đến màn hình xem trước file
                        navController.navigate("${Routes.FilePreview}?fileUrl=$encodedUrl&fileName=${file.fileName}")
                    }
                )
            }
        }
    }
}

@Composable
fun UserResultItem(userModel: UserModel, onClick: () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = userModel.avatarUrl ?: R.drawable.ic_profile, contentDescription = "Avatar", contentScale = ContentScale.Crop, placeholder = painterResource(R.drawable.ic_profile), modifier = Modifier
                .size(48.dp)
                .clip(CircleShape))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(userModel.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!userModel.bio.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(userModel.bio, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// Dán đoạn code này vào cuối file SearchScreen.kt

@Composable
fun GroupResultItem(group: Group, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = group.groupName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // =========================================================
            // SỬA LỖI: Dùng 'group.description' thay vì 'group.groupDescription'
            // =========================================================
            if (group.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = group.description, // <-- SỬA Ở ĐÂY
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}
// Dán tiếp đoạn code này vào cuối file SearchScreen.kt

@Composable
fun FileResultItem(file: LibraryFile, onClick: () -> Unit) {
    Card(
        modifier = Modifier            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sử dụng một icon tài liệu mặc định
            Icon(
                painter = painterResource(id = R.drawable.ic_document), // Hãy chắc chắn bạn có icon này trong thư mục res/drawable
                contentDescription = "File Icon",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.width(16.dp))

            Text(
                text = file.fileName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PostResultItem(post: PostModel, onClick: () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Text(post.content, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Text("bởi ${post.authorName}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}