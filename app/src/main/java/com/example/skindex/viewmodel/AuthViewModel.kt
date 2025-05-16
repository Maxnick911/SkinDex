package com.example.skindex.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skindex.data.api.model.auth.PatientRegisterRequest
import com.example.skindex.data.api.model.auth.UserLoginRequest
import com.example.skindex.data.api.model.auth.UserRegisterRequest
import com.example.skindex.data.api.service.ApiService
import com.example.skindex.data.api.service.AuthService
import com.example.skindex.data.storage.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(private val authService: AuthService,
                                        private val apiService: ApiService,
                                        private val secureStorage: SecureStorage)
    : ViewModel() {

    private val _loginResult = MutableLiveData<Result<String>>()
    val loginResult: LiveData<Result<String>> get() = _loginResult

    private val _registerResult = MutableLiveData<Result<String>>()
    val registerResult: LiveData<Result<String>> get() = _registerResult

    private val _addPatientResult = MutableLiveData<Result<String>>()
    val addPatientResult: LiveData<Result<String>> get() = _addPatientResult

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                val response = authService.login(UserLoginRequest(email, password))
                secureStorage.saveToken(response.token)
                _loginResult.value = Result.success("doctor")
            } catch (e: Exception) {
                _loginResult.value = Result.failure(e)
            }
        }
    }

    fun register(email: String, password: String, name: String) {
        viewModelScope.launch {
            try {
                val response = authService.register(UserRegisterRequest(email, password, name))
                _registerResult.value = Result.success(response.message)
            } catch (e: Exception) {
                _registerResult.value = Result.failure(e)
            }
        }
    }

    fun addPatient(email: String, name: String) {
        viewModelScope.launch {
            try {
                val response = apiService.addPatient(PatientRegisterRequest(email, name))
                _addPatientResult.value = Result.success(response.message)
            } catch (e: Exception) {
                _addPatientResult.value = Result.failure(e)
            }
        }
    }
}