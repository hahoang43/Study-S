package com.example.study_s.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.LibraryFile
import com.example.study_s.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LibraryViewModel(private val libraryRepository: LibraryRepository = LibraryRepository()) : ViewModel() {

    private val _files = MutableStateFlow<List<LibraryFile>>(emptyList())
    val files: StateFlow<List<LibraryFile>> = _files.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadAllFiles()
    }

    /**
     * Tải tất cả file từ repository.
     */
    fun loadAllFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            libraryRepository.getAllFiles().collect { fileList ->
                _files.value = fileList
                _isLoading.value = false
            }
        }
    }

    /**
     * Xử lý quy trình tải file lên Cloudinary và lưu metadata.
     */
    fun uploadFile(
        context: Context, // Nhận Context từ UI
        fileUri: Uri,
        fileName: String,
        mimeType: String,
        uploaderId: String,
        uploaderName: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Gọi Repository để xử lý cả upload và lưu metadata
                libraryRepository.uploadFile(
                    context = context, // Truyền Context xuống Repository
                    fileUri = fileUri,
                    fileName = fileName,
                    mimeType = mimeType,
                    uploaderId = uploaderId,
                    uploaderName = uploaderName
                )
                // loadAllFiles() sẽ được gọi để cập nhật UI sau khi upload thành công
            } catch (e: Exception) {
                _error.value = "Lỗi upload file: ${e.message}"
            } finally {
                // Đảm bảo loading được tắt ngay cả khi có lỗi
                _isLoading.value = false
            }
        }
    }
}