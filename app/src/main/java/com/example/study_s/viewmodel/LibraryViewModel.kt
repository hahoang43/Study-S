package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.LibraryFile
import com.example.study_s.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class LibraryViewModel(private val repository: LibraryRepository = LibraryRepository()) : ViewModel() {

    private val _files = MutableStateFlow<List<LibraryFile>>(emptyList())
    val files: StateFlow<List<LibraryFile>> = _files.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun loadAllFiles() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.getAllFiles()
                .catch { e ->
                    // Handle error
                    println("Error loading files: ${e.message}")
                    _isRefreshing.value = false
                }
                .collect { fileList ->
                    _files.value = fileList
                    _isRefreshing.value = false
                }
        }
    }
}
