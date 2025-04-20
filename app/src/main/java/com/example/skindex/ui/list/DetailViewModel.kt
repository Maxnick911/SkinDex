package com.example.skindex.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skindex.data.Post
import com.example.skindex.data.PreferencesManager
import com.example.skindex.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> get() = _post

    fun fetchPost(id: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.getPostById(id)
                _post.value = response
                preferencesManager.saveLastPostId(id)
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }
}