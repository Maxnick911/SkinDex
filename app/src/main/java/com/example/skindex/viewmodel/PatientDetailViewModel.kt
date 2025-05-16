package com.example.skindex.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skindex.data.api.model.DiagnosisListResponse
import com.example.skindex.data.api.model.DiagnosisResponse
import com.example.skindex.data.api.model.ImageResponse
import com.example.skindex.data.api.model.Patient
import com.example.skindex.data.api.service.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PatientDetailViewModel @Inject constructor(private val apiService: ApiService) : ViewModel() {

    private val _patient = MutableLiveData<Patient>()
    val patient: LiveData<Patient> = _patient

    private val _imagesWithDiagnoses = MutableLiveData<List<ImageWithDiagnosis>>()
    val imagesWithDiagnoses: LiveData<List<ImageWithDiagnosis>> = _imagesWithDiagnoses

    data class ImageWithDiagnosis(
        val image: ImageResponse,
        val diagnosis: DiagnosisResponse?
    )

    fun loadPatient(id: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.getUserById(id)
                val d = response.data
                _patient.postValue(Patient(d.id, d.name, d.email))
                loadImagesAndDiagnoses(id)
            } catch (e: Exception) {
                Log.d("PatientDetailViewModel", "Patient data upload error: ${e.message}")
            }
        }
    }

    fun deletePatient(id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                apiService.deleteUser(id)
                onSuccess()
            } catch (e: Exception) {
                Log.e("PatientDetailViewModel", "Error deleting patient: ${e.message}")
                onError("Delete error: ${e.message}")
            }
        }
    }

    private suspend fun loadImagesAndDiagnoses(patientId: Int) {
        try {
            val imagesResponse = apiService.getImagesByPatient(patientId)
            val imagesWithDiagnoses = mutableListOf<ImageWithDiagnosis>()
            for (image in imagesResponse.data) {
                val diagnosesResponse = try {
                    apiService.getDiagnosesByImage(image.id.toInt())
                } catch (e: Exception) {
                    Log.w("PatientDetailViewModel", "Error loading diagnoses for image ${image.id}: ${e.message}")
                    null
                }
                val diagnosis = diagnosesResponse?.data?.firstOrNull()
                imagesWithDiagnoses.add(ImageWithDiagnosis(image, diagnosis))
            }
            _imagesWithDiagnoses.postValue(imagesWithDiagnoses)
        } catch (e: Exception) {
            Log.d("PatientDetailViewModel", "Images and Diagnoses upload error: ${e.message}")
            _imagesWithDiagnoses.postValue(emptyList())
        }
    }
}