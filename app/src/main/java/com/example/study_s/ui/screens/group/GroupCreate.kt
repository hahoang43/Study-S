package com.example.study_s.ui.screens.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.study_s.ui.navigation.Routes
import com.example.study_s.viewmodel.GroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCreateScreen(
    navController: NavHostController,
    // userId is needed here. For now, we'll use a placeholder.
    userId: String = "temp_user_id",
    groupViewModel: GroupViewModel = viewModel()
) {
    val isCreating by groupViewModel.isCreating.collectAsState()
    val createSuccess by groupViewModel.createSuccess.collectAsState()

    var groupName by remember { mutableStateOf("") }

    // When group creation is successful, navigate to the group chat screen
    LaunchedEffect(createSuccess) {
        if (createSuccess != null) {
            navController.navigate("${Routes.GroupChat}/$createSuccess") {
                // Pop up to the group list to avoid going back to the create screen
                popUpTo(Routes.GroupList) { inclusive = false }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Tạo Nhóm Học Tập") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Tên nhóm") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        groupViewModel.createGroup(groupName, userId)
                    }
                },
                enabled = !isCreating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Đang tạo...")
                } else {
                    Text("Tạo nhóm")
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewGroupCreateScreen() {
    GroupCreateScreen(navController = rememberNavController())
}
