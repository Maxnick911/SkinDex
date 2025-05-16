package com.example.skindex.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ClassificationViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _classificationResult = MutableLiveData<String>()
    val classificationResult: LiveData<String> get() = _classificationResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private var interpreter: Interpreter? = null

    private val classNames = listOf(
        "Eczema",
        "Warts & Viral Infections",
        "Melanoma",
        "Atopic Dermatitis",
        "Basal Cell Carcinoma",
        "Mole",
        "Benign Keratosis",
        "Psoriasis & Lichen",
        "Seborrheic Keratosis",
        "Tinea & Fungal Infections",
        "Normal Skin"
    )

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            Log.d("ClassificationViewModel", "Loading model...")
            val modelFile = context.assets.openFd("model.tflite")
            val fileDescriptor = modelFile.fileDescriptor
            val startOffset = modelFile.startOffset
            val declaredLength = modelFile.declaredLength
            val fileChannel = FileInputStream(fileDescriptor).channel
            val mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            interpreter = Interpreter(mappedByteBuffer)
            fileChannel.close()
            modelFile.close()
            Log.d("ClassificationViewModel", "Model loaded successfully")
        } catch (e: Exception) {
            Log.e("ClassificationViewModel", "Model loading error", e)
            _classificationResult.value = "Model loading error: ${e.message}"
        }
    }

    fun classifyImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.postValue(true)
                Log.d("ClassificationViewModel", "Processing image: $uri")
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap == null) {
                    _classificationResult.postValue("Failed to load image")
                    Log.e("ClassificationViewModel", "Failed to load image")
                    _isLoading.postValue(false)
                    return@launch
                }

                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
                val inputBuffer = preprocessImage(resizedBitmap)
                val output = Array(1) { FloatArray(11) }

                interpreter?.run(inputBuffer, output) ?: run {
                    _classificationResult.postValue("Model not initialized")
                    Log.e("ClassificationViewModel", "Model not initialized")
                    _isLoading.postValue(false)
                    return@launch
                }

                val result = postprocessOutput(output)
                _classificationResult.postValue(result)
                _isLoading.postValue(false)
                Log.d("ClassificationViewModel", "Classification result: $result")
            } catch (e: Exception) {
                Log.e("ClassificationViewModel", "Classification error", e)
                _classificationResult.postValue("Classification error: ${e.message}")
                _isLoading.postValue(false)
            }
        }
    }

    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * 4)
            .order(ByteOrder.nativeOrder())
        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = bitmap.getPixel(x, y)
                val r = ((pixel shr 16 and 0xFF) / 255.0f)
                val g = ((pixel shr 8  and 0xFF) / 255.0f)
                val b = ((pixel       and 0xFF) / 255.0f)
                buffer.putFloat(r)
                buffer.putFloat(g)
                buffer.putFloat(b)
            }
        }
        buffer.rewind()
        return buffer
    }

    private fun postprocessOutput(output: Array<FloatArray>): String {
        val probabilities = output[0].mapIndexed { index, prob -> classNames[index] to prob }
            .sortedByDescending { it.second }
        return probabilities.joinToString("\n") {
            "${it.first}: ${String.format(Locale.US, "%.2f", it.second * 100)}%"
        }
    }

    override fun onCleared() {
        super.onCleared()
        interpreter?.close()
        Log.d("ClassificationViewModel", "Interpreter closed")
    }
}