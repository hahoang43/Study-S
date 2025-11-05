package com.example.study_s.ui.screens.group

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.ui.screens.components.BottomNavBar
import com.example.study_s.ui.screens.components.TopBar
import com.example.study_s.viewmodel.GroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    navController: NavHostController,
    groupViewModel: GroupViewModel = viewModel(),
    // Replace with the actual logged-in user's ID
    currentUserId: String = "temp_user_id" 
) {
    val groups by groupViewModel.groups.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRouteFromNav = navBackStackEntry?.destination?.route
    val currentRoute = if (LocalInspectionMode.current) "group_screen" else currentRouteFromNav

    LaunchedEffect(Unit) {
        groupViewModel.loadAllGroups()
    }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1A73E8), RoundedCornerShape(8.dp))
                    .background(Color(0xFFEAF1FD), RoundedCornerShape(8.dp))
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

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(groups) { group ->
                    val isMember = group.members.contains(currentUserId)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .border(1.dp, Color(0xFFB8C7E0), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = group.groupName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.Black
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = group.description,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        if (isMember) {
                                            navController.navigate("${Routes.GroupChat}/${group.groupId}")
                                        } else {
                                            groupViewModel.joinGroup(group.groupId, currentUserId)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1A73E8),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(if (isMember) "Vào nhóm" else "Tham gia", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewGroupScreen() {
    GroupScreen(navController = rememberNavController())
}
