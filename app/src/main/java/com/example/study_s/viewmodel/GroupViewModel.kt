package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.Group
import com.example.study_s.data.repository.GroupRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupViewModel(private val groupRepository: GroupRepository = GroupRepository()) : ViewModel() {

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _createSuccess = MutableStateFlow<String?>(null)
    val createSuccess: StateFlow<String?> = _createSuccess.asStateFlow()


    init {
        loadAllGroups()
    }

    fun loadAllGroups() {
        viewModelScope.launch {
            _groups.value = groupRepository.getAllGroups()
        }
    }

    fun getGroupById(groupId: String) {
        viewModelScope.launch {
            _group.value = _groups.value.find { it.groupId == groupId }
        }
    }

    fun createGroup(groupName: String, creatorId: String) {
        viewModelScope.launch {
            _isCreating.value = true
            // Generate a new ID for the group
            val newGroupId = FirebaseFirestore.getInstance().collection("groups").document().id
            val newGroup = Group(
                groupId = newGroupId,
                groupName = groupName,
                description = "", // Assuming description is not needed for now
                members = listOf(creatorId)
            )
            groupRepository.createGroup(newGroup)
            _createSuccess.value = newGroupId
            _isCreating.value = false
            loadAllGroups() // Refresh the group list
        }
    }

    fun joinGroup(groupId: String, userId: String) {
        viewModelScope.launch {
            groupRepository.joinGroup(groupId, userId)
            loadAllGroups() // Refresh the group list
        }
    }
}