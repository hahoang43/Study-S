package com.example.study_s.ui.screens.group

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.study_s.data.model.Group
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.ui.screens.components.BottomNavBar
import com.example.study_s.ui.screens.components.TopBar
import com.example.study_s.viewmodel.GroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    navController: NavHostController,
    groupViewModel: GroupViewModel = viewModel(),
    currentUserId: String = "temp_user_id"
) {
    val allGroups by groupViewModel.groups.collectAsState()
    val isRefreshing by groupViewModel.isRefreshing.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRouteFromNav = navBackStackEntry?.destination?.route
    val currentRoute = if (LocalInspectionMode.current) "group_screen" else currentRouteFromNav

    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Đã tham gia", "Khám phá")

    val (joinedGroups, discoverGroups) = allGroups.partition { it.members.contains(currentUserId) }

    val filteredJoined = joinedGroups.filter { it.groupName.contains(searchQuery, ignoreCase = true) }
    val filteredDiscover = discoverGroups.filter { it.groupName.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopBar(
                onNavIconClick = { /* TODO: Open drawer */ },
                onNotificationClick = { /* TODO: Handle notification click */ },
                onSearchClick = { navController.navigate(Routes.Search) }
            )
        },
        bottomBar = {
            if (currentRoute != null) {
                BottomNavBar(navController = navController, currentRoute = currentRoute)
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routes.GroupCreate) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Group")
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { groupViewModel.loadAllGroups() },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF1A73E8), RoundedCornerShape(8.dp))
                            .background(Color(0xFFEAF1FD), RoundedCornerShape(8.dp))
                            .clickable { groupViewModel.loadAllGroups() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NHÓM HỌC TẬP",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1A73E8)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Tìm kiếm trong các nhóm...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }

                val groupsToShow = if (selectedTabIndex == 0) filteredJoined else filteredDiscover

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp)
                ) {
                    items(groupsToShow) { group ->
                        GroupItem(
                            group = group,
                            currentUserId = currentUserId,
                            navController = navController,
                            groupViewModel = groupViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GroupItem(
    group: Group,
    currentUserId: String,
    navController: NavHostController,
    groupViewModel: GroupViewModel
) {
    val isMember = group.members.contains(currentUserId)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(1.dp, Color(0xFFB8C7E0), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = group.groupName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = group.description, fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "${group.members.size} thành viên", fontSize = 14.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = {
                    if (isMember) {
                        navController.navigate("${Routes.GroupChat}/${group.groupId}")
                    } else {
                        groupViewModel.joinGroup(group.groupId, currentUserId)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMember) Color(0xFF4CAF50) else Color(0xFF1A73E8),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(if (isMember) "Chat" else "Tham gia")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewGroupScreen() {
    GroupScreen(navController = rememberNavController())
}
