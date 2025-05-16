package com.example.skindex.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skindex.data.api.model.DiagnosisResponse
import com.example.skindex.data.api.model.ImageResponse
import com.example.skindex.data.api.service.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiagnosisDetailViewModel @Inject constructor(private val api: ApiService) : ViewModel() {

    private val _diagnosisDetail = MutableLiveData<DiagnosisDetailDto>()
    val diagnosisDetail: LiveData<DiagnosisDetailDto> = _diagnosisDetail

    fun load(id: Int) {
        viewModelScope.launch {
            try {
                val diagResp = api.getDiagnosisById(id)
                val imageResp = api.getImageById(diagResp.data.imageId.toInt())
                _diagnosisDetail.postValue(DiagnosisDetailDto(imageResp.data, diagResp.data))
            } catch (e: Exception) {
            }
        }
    }

    fun deleteDiagnosisAndImage(diagnosisId: Int, imageId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                api.deleteDiagnosis(diagnosisId)
                api.deleteImage(imageId)
                onSuccess()
            } catch (e: Exception) {
                onError("Deleting error: ${e.message}")
            }
        }
    }
}

data class DiagnosisDetailDto(
    val image: ImageResponse,
    val diagnosis: DiagnosisResponse
)