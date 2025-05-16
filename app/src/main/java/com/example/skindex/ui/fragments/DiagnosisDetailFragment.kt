package com.example.skindex.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.skindex.R
import com.example.skindex.core.constants.AppConstants
import com.example.skindex.databinding.FragmentDiagnosisDetailBinding
import com.example.skindex.viewmodel.DiagnosisDetailViewModel
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DiagnosisDetailFragment : Fragment() {

    private var _b: FragmentDiagnosisDetailBinding? = null
    private val b get() = _b!!
    private val args: DiagnosisDetailFragmentArgs by navArgs()
    private val vm: DiagnosisDetailViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentDiagnosisDetailBinding.inflate(inflater, container, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm.diagnosisDetail.observe(viewLifecycleOwner) { dto ->
            val imageUrl = "${AppConstants.BASE_URL}/${dto.image.filePath}"
            Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.ic_error_placeholder)
                .into(b.imageViewDiagnosis)

            b.textViewDiagnosisName.text = dto.diagnosis.diseaseName
            b.textViewProbability.text =
                getString(R.string.probability_format, dto.diagnosis.confidence * 100f)
            dto.diagnosis.doctorComment?.let { comment ->
                b.textViewDoctorComment.apply {
                    text = comment
                    visibility = View.VISIBLE
                }
            }

            b.deleteDiagnosisButton.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Diagnosis")
                    .setMessage("Are you sure you want to delete this image and diagnosis?")
                    .setPositiveButton("Yes") { _, _ ->
                        vm.deleteDiagnosisAndImage(
                            diagnosisId = dto.diagnosis.id.toInt(),
                            imageId = dto.image.id.toInt(),
                            onSuccess = {
                                findNavController().popBackStack()
                            },
                            onError = { error ->
                                AlertDialog.Builder(requireContext())
                                    .setTitle("Error")
                                    .setMessage(error)
                                    .setPositiveButton("OK", null)
                                    .show()
                            }
                        )
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        }

        vm.load(args.diagnosisId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}