package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.LibraryFile
import com.example.study_s.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
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
                }
                .onCompletion { 
                    _isRefreshing.value = false
                }
                .collect { fileList ->
                    _files.value = fileList
                }
        }
    }

    fun deleteFile(file: LibraryFile) {
        viewModelScope.launch {
            try {
                repository.deleteFile(file)
                // Refresh the file list after deletion
                loadAllFiles()
            } catch (e: Exception) {
                // Handle error
                println("Error deleting file: ${e.message}")
            }
        }
    }

    fun updateFileName(fileId: String, newName: String) {
        viewModelScope.launch {
            try {
                repository.updateFileName(fileId, newName)
                // Refresh the file list after update
                loadAllFiles()
            } catch (e: Exception) {
                // Handle error
                println("Error updating file name: ${e.message}")
            }
        }
    }
}
