package com.example.skindex.ui.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skindex.databinding.DialogAddPatientBinding
import com.example.skindex.databinding.FragmentDoctorProfileBinding
import com.example.skindex.ui.adapters.PatientsAdapter
import com.example.skindex.viewmodel.DoctorProfileViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DoctorProfileFragment : Fragment() {

    private var _binding: FragmentDoctorProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DoctorProfileViewModel by viewModels()
    private lateinit var patientsAdapter: PatientsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDoctorProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupListeners()

        viewModel.fetchDoctorInfo()
        viewModel.fetchPatients()
    }

    private fun setupRecyclerView() {
        patientsAdapter = PatientsAdapter { patient ->
            val action =
                DoctorProfileFragmentDirections.actionDoctorProfileToPatientDetail(patient.id)
            findNavController().navigate(action)
        }
        binding.patientsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = patientsAdapter
        }
    }

    private fun setupObservers() {
        viewModel.doctorInfo.observe(viewLifecycleOwner) { doctor ->
            binding.doctorNameTextView.text = doctor.name
            binding.doctorEmailTextView.text = doctor.email
        }

        viewModel.patients.observe(viewLifecycleOwner) { patients ->
            patientsAdapter.submitList(patients)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
        }

        viewModel.patientAdded.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Patient added", Toast.LENGTH_SHORT).show()
                viewModel.fetchPatients()
            }
        }
    }

    private fun setupListeners() {
        binding.addPatientButton.setOnClickListener {
            showAddPatientDialog()
        }
    }

    private fun showAddPatientDialog() {
        val dialog = Dialog(requireContext())
        val dialogBinding = DialogAddPatientBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.addButton.setOnClickListener {
            val name = dialogBinding.nameEditText.text.toString().trim()
            val email = dialogBinding.emailEditText.text.toString().trim()
            if (name.isNotEmpty() && email.isNotEmpty()) {
                viewModel.addPatient(name, email)
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}