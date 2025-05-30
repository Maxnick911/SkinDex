package com.example.skindex.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skindex.core.util.JwtUtils
import com.example.skindex.data.api.model.Patient
import com.example.skindex.data.api.model.PatientListResponse
import com.example.skindex.data.api.model.auth.PatientRegisterRequest
import com.example.skindex.data.api.service.ApiService
import com.example.skindex.data.storage.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DoctorProfileViewModel @Inject constructor(
    private val apiService: ApiService,
    private val secureStorage: SecureStorage,
    private val jwtUtils: JwtUtils
) : ViewModel() {

    private val _doctorInfo = MutableLiveData<Patient>()
    val doctorInfo: LiveData<Patient> = _doctorInfo

    private val _patients = MutableLiveData<List<Patient>>()
    val patients: LiveData<List<Patient>> = _patients

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _patientAdded = MutableLiveData<Boolean>()
    val patientAdded: LiveData<Boolean> = _patientAdded

    fun fetchDoctorInfo() {
        viewModelScope.launch {
            try {
                val token = secureStorage.getToken()
                if (token.isNullOrEmpty()) {
                    _error.postValue("Token not found")
                    return@launch
                }

                val userId = jwtUtils.getUserIdFromToken(token)
                if (userId == null) {
                    _error.postValue("Invalid token: no userId")
                    return@launch
                }

                val response = apiService.getUserById(userId)
                val data = response.data

                _doctorInfo.postValue(
                    Patient(
                        id = data.id,
                        name = data.name,
                        email = data.email
                    )
                )
            } catch (e: Exception) {
                _error.postValue("Error fetching doctor info: ${e.message}")
                Log.d("DoctorProfileVM", "fetchDoctorInfo error: ${e.message}")
            }
        }
    }

    fun fetchPatients() {
        viewModelScope.launch {
            try {
                val response: PatientListResponse = apiService.getPatients()
                _patients.postValue(response.data)
            } catch (e: Exception) {
                _error.postValue("Network error: ${e.message}")
                Log.d("DoctorProfileVM", "fetchPatients error: ${e.message}")
            }
        }
    }

    fun addPatient(name: String, email: String) {
        viewModelScope.launch {
            try {
                val request = PatientRegisterRequest(email = email, name = name)
                val response = apiService.addPatient(request)
                if (response.message.contains("Patient created")) {
                    _patientAdded.postValue(true)
                } else {
                    _error.postValue("Error: ${response.message}")
                }
            } catch (e: Exception) {
                _error.postValue("Network error: ${e.message}")
                Log.d("DoctorProfileVM", "addPatient error: ${e.message}")
            }
        }
    }
}