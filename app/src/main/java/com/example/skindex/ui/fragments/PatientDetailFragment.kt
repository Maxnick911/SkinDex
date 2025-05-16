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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skindex.databinding.FragmentPatientDetailBinding
import com.example.skindex.ui.adapters.ImageDiagnosisAdapter
import com.example.skindex.viewmodel.PatientDetailViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PatientDetailFragment : Fragment() {

    private var _binding: FragmentPatientDetailBinding? = null
    private val binding get() = _binding!!

    private val args: PatientDetailFragmentArgs by navArgs()
    private val viewModel: PatientDetailViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPatientDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.loadPatient(args.patientId)

        viewModel.patient.observe(viewLifecycleOwner) { patient ->
            binding.nameTextView.text = patient.name
            binding.emailTextView.text = patient.email
        }

        viewModel.imagesWithDiagnoses.observe(viewLifecycleOwner) { images ->
            binding.imagesRecyclerView.layoutManager = LinearLayoutManager(context)
            binding.imagesRecyclerView.adapter = ImageDiagnosisAdapter(images) { diagnosis ->
                val action = PatientDetailFragmentDirections
                    .actionPatientDetailToDiagnosisDetail(diagnosis.id.toInt())
                findNavController().navigate(action)
            }
        }

        binding.classifyButton.setOnClickListener {
            val action = PatientDetailFragmentDirections
                .actionPatientDetailToClassificationFragment(args.patientId)
            findNavController().navigate(action)
        }

        binding.deletePatientButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Patient")
                .setMessage("Are you sure you want to delete this patient? All associated images and diagnoses will also be deleted.")
                .setPositiveButton("Yes") { _, _ ->
                    viewModel.deletePatient(args.patientId,
                        onSuccess = {
                            findNavController().popBackStack()
                        },
                        onError = { error ->
                            AlertDialog.Builder(requireContext())
                                .setTitle("Error")
                                .setMessage(error)
                                .setPositiveButton("OK", null)
                                .show()
                        })
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}