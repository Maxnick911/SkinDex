package com.example.skindex.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.canhub.cropper.CropImageView
import com.example.skindex.R
import com.example.skindex.data.api.model.QualityStatusUpdate
import com.example.skindex.data.api.model.DiagnosisRequest
import com.example.skindex.data.api.service.ApiService
import com.example.skindex.databinding.FragmentClassificationBinding
import com.example.skindex.viewmodel.ClassificationViewModel
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class ClassificationFragment : Fragment(), CropImageView.OnCropImageCompleteListener {

    private var _binding: FragmentClassificationBinding? = null
    private val b get() = _binding!!
    private val vm: ClassificationViewModel by viewModels()
    private val args: ClassificationFragmentArgs by navArgs()

    @Inject lateinit var api: ApiService

    private var imageUri: Uri? = null
    private var tmpFile: File? = null
    private var uploadedId = -1
    private var patientId = -1
    private var dialog: AlertDialog? = null

    private val pickLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { startCrop(it) }
    }
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && imageUri != null) startCrop(imageUri!!)
    }
    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) dispatchCamera() else
            Toast.makeText(requireContext(), R.string.camera_denied, Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentClassificationBinding.inflate(inflater, c, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        patientId = args.patientId
        resetUI()
        configureUploadButton()

        b.cropImageView.setOnCropImageCompleteListener(this)
        b.cancelButton.setOnClickListener { onCancelCrop() }
        b.cropButton.setOnClickListener { b.cropImageView.croppedImageAsync() }
        b.btnApprove.setOnClickListener { onApprove() }
        b.btnReject.setOnClickListener { onReject() }

        vm.classificationResult.observe(viewLifecycleOwner) { showClassificationResult(it) }
    }

    private fun resetUI() {
        b.mainContent.visibility        = View.VISIBLE
        b.cropContainer.visibility      = View.GONE
        b.uploadButton.visibility       = View.VISIBLE
        b.pieChart.visibility           = View.GONE
        b.textViewDiagnosis.visibility  = View.GONE
        b.textViewConfidence.visibility = View.GONE
        b.buttonsContainer.visibility   = View.GONE
        b.imageView.setImageDrawable(null)
        uploadedId = -1
    }


    private fun configureUploadButton() {
        if (patientId == -1) {
            b.uploadButton.text = getString(R.string.select_patient)
            b.uploadButton.setOnClickListener { showPatientSelectionDialog() }
        } else {
            b.uploadButton.text = getString(R.string.upload_image)
            b.uploadButton.setOnClickListener { showSourceDialog() }
        }
    }

    private fun showPatientSelectionDialog() {
        lifecycleScope.launch {
            try {
                val patients = api.getPatients()
                if (patients.isEmpty()) {
                    AlertDialog.Builder(requireContext())
                        .setTitle(R.string.no_patients)
                        .setPositiveButton(R.string.go_to_profile) { _, _ ->
                            findNavController().navigate(R.id.action_classificationFragment_to_doctorProfileFragment)
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                    return@launch
                }
                val names = patients.map { "${it.name} (${it.email})" }.toTypedArray()
                dialog = AlertDialog.Builder(requireContext())
                    .setTitle(R.string.select_patient)
                    .setItems(names) { _, which ->
                        patientId = patients[which].id
                        configureUploadButton()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            } catch (e: Exception) {
                Snackbar.make(b.root, R.string.error_loading_patients, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun showClassificationResult(result: String) {
        val raw = result.split("\n").mapNotNull {
            val p = it.split(":")
            val lbl = p.getOrNull(0)?.trim() ?: return@mapNotNull null
            val pct = p.getOrNull(1)?.replace("%","")?.toFloatOrNull() ?: return@mapNotNull null
            lbl to pct
        }

        val threshold = 5f
        val main = raw.filter { it.second >= threshold }
        val others = raw.filter { it.second < threshold }
        val entries = main.map { PieEntry(it.second, it.first) }.toMutableList()
        val othersSum = others.sumOf { it.second.toDouble() }.toFloat()
        if (othersSum > 0f) {
            entries.add(PieEntry(othersSum, "Other"))
        }

        if (entries.isEmpty()) return

        val ds = PieDataSet(entries, "")
        ds.colors = ColorTemplate.COLORFUL_COLORS.toList()
        ds.valueTextSize = 12f
        ds.valueFormatter = PercentFormatter(b.pieChart)

        b.pieChart.apply {
            data = PieData(ds)
            setUsePercentValues(true)
            description.isEnabled = false
            setDrawEntryLabels(false)
            legend.isWordWrapEnabled = true
            invalidate()
            visibility = View.VISIBLE
        }

        val top = entries.maxByOrNull { it.value }!!
        b.textViewDiagnosis.text  = top.label
        b.textViewConfidence.text = String.format("%.2f%%", top.value)
        b.textViewDiagnosis.visibility  = View.VISIBLE
        b.textViewConfidence.visibility = View.VISIBLE

        b.buttonsContainer.visibility = View.VISIBLE
    }

    private fun showSourceDialog() = AlertDialog.Builder(requireContext())
        .setTitle(R.string.select_source)
        .setItems(arrayOf(getString(R.string.take_photo), getString(R.string.choose_gallery))) { _, which ->
            if (which == 0) checkCameraPerm()
            else pickLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        .show()

    private fun checkCameraPerm() {
        if (requireContext().checkSelfPermission(Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) dispatchCamera()
        else permLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun dispatchCamera() {
        val file = File.createTempFile("photo", ".jpg", requireContext().cacheDir)
        imageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider", file
        )
        cameraLauncher.launch(imageUri)
    }

    private fun startCrop(uri: Uri) {
        resetUI()
        b.mainContent.visibility   = View.GONE
        b.cropContainer.visibility = View.VISIBLE
        b.cropImageView.setImageUriAsync(uri)
    }

    private fun onCancelCrop() {
        b.cropContainer.visibility = View.GONE
        b.mainContent.visibility = View.VISIBLE
    }

    override fun onCropImageComplete(view: CropImageView, result: CropImageView.CropResult) {
        resetUI()
        if (!result.isSuccessful || result.uriContent == null) {
            Snackbar.make(b.root, R.string.crop_failed, Snackbar.LENGTH_SHORT).show()
            return
        }
        val uri = result.uriContent!!
        b.imageView.setImageURI(uri)
        tmpFile = File(requireContext().cacheDir, "crop_${System.currentTimeMillis()}.jpg").apply {
            requireContext().contentResolver.openInputStream(uri)?.use { writeBytes(it.readBytes()) }
        }
        vm.classifyImage(uri)
    }

    private fun onApprove() {
        if (tmpFile == null || patientId < 0) return
        lifecycleScope.launch {
            b.progressBar.visibility = View.VISIBLE

            val id = uploadImage(tmpFile!!, patientId)
            if (id == -1) {
                b.progressBar.visibility = View.GONE
                Snackbar.make(b.root, R.string.upload_failed, Snackbar.LENGTH_LONG).show()
                return@launch
            }
            uploadedId = id

            val statusUpdated = updateImageStatus("accepted")
            if (!statusUpdated) {
                b.progressBar.visibility = View.GONE
                Snackbar.make(b.root, R.string.update_failed, Snackbar.LENGTH_SHORT).show()
                return@launch
            }

            val diagSaved = postDiagnosis()
            b.progressBar.visibility = View.GONE
            if (diagSaved) {
                Toast.makeText(requireContext(), R.string.diagnosis_saved, Toast.LENGTH_SHORT).show()
                resetUI()
                configureUploadButton()
            } else {
                Snackbar.make(b.root, R.string.diagnosis_failed, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun uploadImage(file: File, patientId: Int): Int {
        val part = MultipartBody.Part.createFormData(
            name = "image",
            filename = file.name,
            body = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        )
        val pidBody = patientId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        return try {
            val resp = api.uploadImage(part, pidBody)
            if (!resp.isSuccessful) return -1
            resp.body()?.message?.substringAfterLast(": ")?.toIntOrNull() ?: -1
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    private suspend fun updateImageStatus(status: String): Boolean {
        if (uploadedId < 0) return false
        return try {
            val resp = api.updateImage(uploadedId, QualityStatusUpdate(status))
            resp.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun postDiagnosis(): Boolean {
        val top = vm.classificationResult.value
            ?.split("\n")?.firstOrNull()?.split(":") ?: return false

        val diagnosis = top[0].trim()
        val prob = top.getOrNull(1)
            ?.replace("%", "")
            ?.toFloatOrNull()
            ?.div(100f) ?: 0f

        return try {
            val resp = api.postDiagnosis(DiagnosisRequest(uploadedId, diagnosis, prob))
            resp.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    private fun onReject() {
        Toast.makeText(requireContext(), R.string.image_rejected, Toast.LENGTH_SHORT).show()
        resetUI()
        configureUploadButton()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        b.cropImageView.setOnCropImageCompleteListener(null)
        _binding = null
    }
}
