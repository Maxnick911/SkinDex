package com.example.skindex.ui.list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skindex.data.Post
import com.example.skindex.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UiState {
    object Loading : UiState()
    data class Success(val posts: List<Post>) : UiState()
    data class Error(val message: String) : UiState()
}

@HiltViewModel
class ListViewModel @Inject constructor(private val apiService: ApiService) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Success(emptyList()))
    val uiState: StateFlow<UiState> = _uiState

    fun fetchPosts() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                Log.d("ListViewModel", "Починаємо завантаження постів...")
                val response = apiService.getPosts()
                Log.d("ListViewModel", "Отримано ${response.size} постів")
                _uiState.value = UiState.Success(response)
            } catch (e: Exception) {
                Log.e("ListViewModel", "Помилка: ${e.message}", e)
                _uiState.value = UiState.Error("Не вдалося завантажити дані")
            }
        }
    }
}