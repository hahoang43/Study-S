package com.example.study_s.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.Group
import com.example.study_s.data.model.User
import com.example.study_s.data.repository.GroupRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.joinAll

// Giả sử GroupRepository có thể được cung cấp qua DI hoặc mặc định
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

    private val _bannedMembers = MutableStateFlow<List<User>>(emptyList())
    val bannedMembers: StateFlow<List<User>> = _bannedMembers.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _createSuccess = MutableStateFlow<String?>(null)
    val createSuccess: StateFlow<String?> = _createSuccess.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _showRemovedToast = MutableStateFlow(false)
    val showRemovedToast: StateFlow<Boolean> = _showRemovedToast.asStateFlow()

    // Thêm trạng thái lỗi để hiển thị phản hồi cho người dùng (nếu có lỗi tải nhóm)
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadAllGroups()
    }

    /**
     * Tải tất cả các nhóm từ Repository và cập nhật trạng thái làm mới.
     * Logic này đã chính xác trong việc sử dụng khối try/finally.
     */
    fun loadAllGroups() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null // Xóa lỗi cũ
            try {
                _groups.value = groupRepository.getAllGroups()
            } catch (e: Exception) {
                // Xử lý và ghi nhận lỗi nếu có vấn đề khi tải
                _error.value = "Không thể tải nhóm: ${e.message}"
                println("Error loading groups: ${e.message}")
            } finally {
                // Đảm bảo trạng thái làm mới luôn tắt dù thành công hay thất bại
                _isRefreshing.value = false
            }
        }
    }

    fun loadUserGroups(userId: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null // Xóa lỗi cũ
            try {
                _userGroups.value = groupRepository.getUserGroups(userId)
            } catch (e: Exception) {
                _error.value = "Không thể tải các nhóm đã tham gia: ${e.message}"
                println("Error loading user groups: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
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
                val newGroupId = FirebaseFirestore.getInstance().collection("groups").document().id

                // =========================================================================
                // ✅ PHẦN LOGIC TẠO TỪ KHÓA TÌM KIẾM
                // =========================================================================
                // 1. Chuẩn hóa tên nhóm về chữ thường
                val normalizedName = groupName.lowercase()

                // 2. Tách tên nhóm thành các từ khóa, loại bỏ các từ khóa rỗng nếu có
                val keywords = normalizedName.split(" ").filter { it.isNotBlank() }.distinct()
                // .distinct() để đảm bảo không có từ khóa trùng lặp

                // 3. Bạn cũng có thể thêm cả từ khóa từ mô tả hoặc chủ đề
                val subjectKeywords = subject.lowercase().split(" ").filter { it.isNotBlank() }
                val combinedKeywords = (keywords + subjectKeywords).distinct()
                // =========================================================================

                val newGroup = Group(
                    groupId = newGroupId,
                    groupName = groupName,
                    groupNameLowercase = normalizedName, // Giữ lại để sắp xếp
                    description = description,
                    subject = subject,
                    members = listOf(creatorId),
                    createdBy = creatorId,
                    searchKeywords = combinedKeywords, // <-- SỬ DỤNG MẢNG TỪ KHÓA MỚI
                    createdAt = null // Firestore sẽ tự điền giá trị này với @ServerTimestamp
                )
                groupRepository.createGroup(newGroup)
                _createSuccess.value = newGroupId


                val job1 = launch { loadAllGroups() }
                val job2 = launch { loadUserGroups(creatorId) }
                joinAll(job1, job2)

            } catch (e: Exception) {
                // Xử lý lỗi tạo nhóm
                _error.value = "Lỗi tạo nhóm: ${e.message}"
            } finally {
                _isCreating.value = false
            }
        }
    }

    fun joinGroup(groupId: String, userId: String) {
        viewModelScope.launch {
            try {
                groupRepository.joinGroup(groupId, userId)
                val job1 = launch { loadAllGroups() }
                val job2 = launch { loadUserGroups(userId) }
                joinAll(job1, job2)
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
            val job1 = launch { loadAllGroups() }
            val job2 = launch { loadUserGroups(userId) }
            joinAll(job1, job2)
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
            groupChatViewModel.notifyUserRemoved()
            // Refresh group and member details
            getGroupById(groupId)
            _group.value?.let { loadMemberDetails(it.members) }
        }
    }

    fun banUser(groupId: String, userId: String) {
        viewModelScope.launch {
            groupRepository.banUser(groupId, userId)
            // Refresh group and member details
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
            loadAllGroups()
        }
    }

    fun onToastShown() {
        _showRemovedToast.value = false
    }

    /**
     * Hàm mới: Thực hiện tham gia nhóm và ngay lập tức tải lại thông tin nhóm.
     * Điều này sẽ tự động cập nhật UI trong ChatGroupScreen.
     */
    fun joinGroupAndRefresh(groupId: String, userId: String) {
        if (groupId.isBlank() || userId.isBlank()) {
            _error.value = "Không thể tham gia do thiếu thông tin."
            return
        }

        viewModelScope.launch {
            try {
                // 1. Gọi Repository để thêm người dùng vào danh sách thành viên
                groupRepository.joinGroup(groupId, userId)

                // 2. NGAY LẬP TỨC tải lại thông tin của chính nhóm đó bằng cách gọi getGroupById()
                //    Việc này sẽ cập nhật `_group.value` (một StateFlow),
                //    khiến cho biến `isMember` trong ChatGroupScreen tính toán lại và trở thành `true`,
                //    và Compose sẽ tự động vẽ lại giao diện sang màn hình chat.
                getGroupById(groupId)

            } catch (e: Exception) {
                // Ghi lại lỗi nếu có vấn đề xảy ra
                _error.value = "Không thể tham gia nhóm: ${e.message}"
            }
        }
    }
}