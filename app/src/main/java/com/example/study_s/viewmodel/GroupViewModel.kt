package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.Group
import com.example.study_s.data.model.User
import com.example.study_s.data.repository.GroupRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class GroupViewModel(
    private val groupRepository: GroupRepository = GroupRepository(),
    private val groupChatViewModel: GroupChatViewModel = GroupChatViewModel()
) : ViewModel() {

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _userGroups = MutableStateFlow<List<Group>>(emptyList())
    val userGroups: StateFlow<List<Group>> = _userGroups.asStateFlow()

    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group.asStateFlow()

    private val _members = MutableStateFlow<List<User>>(emptyList())
    val members: StateFlow<List<User>> = _members.asStateFlow()

    private val _pendingMembers = MutableStateFlow<List<User>>(emptyList())
    val pendingMembers: StateFlow<List<User>> = _pendingMembers.asStateFlow()

    private val _bannedMembers = MutableStateFlow<List<User>>(emptyList())
    val bannedMembers: StateFlow<List<User>> = _bannedMembers.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _createSuccess = MutableStateFlow<String?>(null)
    val createSuccess: StateFlow<String?> = _createSuccess.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadAllGroups()
    }

    fun loadAllGroups(): Job = viewModelScope.launch {
        _isRefreshing.value = true
        _error.value = null
        try {
            _groups.value = groupRepository.getAllGroups()
        } catch (e: Exception) {
            _error.value = "Không thể tải nhóm: ${e.message}"
            println("Error loading groups: ${e.message}")
        } finally {
            _isRefreshing.value = false
        }
    }

    fun loadUserGroups(userId: String): Job = viewModelScope.launch {
        _isRefreshing.value = true
        _error.value = null
        try {
            _userGroups.value = groupRepository.getUserGroups(userId)
        } catch (e: Exception) {
            _error.value = "Không thể tải các nhóm đã tham gia: ${e.message}"
            println("Error loading user groups: ${e.message}")
        } finally {
            _isRefreshing.value = false
        }
    }

    fun getGroupById(groupId: String) {
        viewModelScope.launch {
            _group.value = groupRepository.getGroupById(groupId)
        }
    }

    fun loadMemberDetails(memberIds: List<String>) {
        viewModelScope.launch {
            try {
                _members.value = groupRepository.getMemberDetails(memberIds)
            } catch (e: Exception) {
                _error.value = "Không thể tải chi tiết thành viên: ${e.message}"
            }
        }
    }

    fun loadPendingMemberDetails(memberIds: List<String>) {
        viewModelScope.launch {
            try {
                _pendingMembers.value = groupRepository.getMemberDetails(memberIds)
            } catch (e: Exception) {
                _error.value = "Không thể tải chi tiết thành viên: ${e.message}"
            }
        }
    }

    fun loadBannedMemberDetails(memberIds: List<String>) {
        viewModelScope.launch {
            try {
                _bannedMembers.value = groupRepository.getMemberDetails(memberIds)
            } catch (e: Exception) {
                _error.value = "Không thể tải chi tiết thành viên bị cấm: ${e.message}"
            }
        }
    }

    fun createGroup(
        groupName: String,
        description: String,
        subject: String,
        creatorId: String
    ) {
        viewModelScope.launch {
            _isCreating.value = true
            try {
                val normalizedName = groupName.lowercase()
                val keywords = normalizedName.split(" ").filter { it.isNotBlank() }.distinct()
                val subjectKeywords = subject.lowercase().split(" ").filter { it.isNotBlank() }
                val combinedKeywords = (keywords + subjectKeywords).distinct()

                val newGroup = Group(
                    groupName = groupName,
                    groupNameLowercase = normalizedName,
                    description = description,
                    subject = subject,
                    members = listOf(creatorId),
                    createdBy = creatorId,
                    searchKeywords = combinedKeywords,
                )
                val newGroupId = groupRepository.createGroup(newGroup)

                joinAll(loadAllGroups(), loadUserGroups(creatorId))

                _createSuccess.value = newGroupId

            } catch (e: Exception) {
                _error.value = "Lỗi tạo nhóm: ${e.message}"
            } finally {
                _isCreating.value = false
            }
        }
    }

    fun onGroupCreationHandled() {
        _createSuccess.value = null
    }

    fun sendJoinRequest(groupId: String, userId: String) {
        viewModelScope.launch {
            try {
                groupRepository.addPendingMember(groupId, userId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun approveJoinRequest(groupId: String, userId: String, groupName: String, userName: String) {
        viewModelScope.launch {
            try {
                groupRepository.approveJoinRequest(groupId, userId)
                groupChatViewModel.sendMessage(groupId, "system", "$userName đã được thêm vào nhóm.", "System")
                getGroupById(groupId)
                _group.value?.let { loadPendingMemberDetails(it.pendingMembers) }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun rejectJoinRequest(groupId: String, userId: String) {
        viewModelScope.launch {
            try {
                groupRepository.removePendingMember(groupId, userId)
                getGroupById(groupId)
                _group.value?.let { loadPendingMemberDetails(it.pendingMembers) }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun joinGroup(groupId: String, userId: String) {
        viewModelScope.launch {
            try {
                sendJoinRequest(groupId, userId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun leaveGroup(groupId: String, userId: String) {
        viewModelScope.launch {
            groupRepository.leaveGroup(groupId, userId)
            joinAll(loadAllGroups(), loadUserGroups(userId))
        }
    }

    fun removeMember(groupId: String, userId: String, adminName: String, memberName: String) {
        viewModelScope.launch {
            groupRepository.removeMemberFromGroup(groupId, userId)
            groupChatViewModel.sendMessage(
                groupId,
                "system",
                "$adminName đã xóa $memberName khỏi nhóm.",
                "System"
            )
            groupChatViewModel.notifyUserRemoved(userId)
            getGroupById(groupId)
            _group.value?.let { loadMemberDetails(it.members) }
        }
    }

    fun banUser(groupId: String, userId: String) {
        viewModelScope.launch {
            groupRepository.banUser(groupId, userId)
            getGroupById(groupId)
            _group.value?.let { loadMemberDetails(it.members) }
        }
    }

    fun unbanUser(groupId: String, userId: String) {
        viewModelScope.launch {
            groupRepository.unbanUser(groupId, userId)
            getGroupById(groupId)
            _group.value?.let { loadBannedMemberDetails(it.bannedUsers) }
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            groupRepository.deleteGroup(groupId)
            loadAllGroups().join()
        }
    }

    fun joinGroupAndRefresh(groupId: String, userId: String) {
        if (groupId.isBlank() || userId.isBlank()) {
            _error.value = "Không thể gửi yêu cầu do thiếu thông tin."
            return
        }

        viewModelScope.launch {
            try {
                sendJoinRequest(groupId, userId)
                getGroupById(groupId)
            } catch (e: Exception) {
                _error.value = "Không thể gửi yêu cầu tham gia nhóm: ${e.message}"
            }
        }
    }
}
