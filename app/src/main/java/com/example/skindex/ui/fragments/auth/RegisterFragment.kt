package com.example.skindex.ui.fragments.auth

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.skindex.R
import com.example.skindex.databinding.FragmentAuthRegisterBinding
import com.example.skindex.viewmodel.AuthViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentAuthRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    private val tag = "RegisterFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.registerButton.setOnClickListener {
            Log.d(tag, "Register button clicked")
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val name = binding.nameEditText.text.toString()

            Log.d(tag, "Input data: email=$email, name=$name")
            if (validateRegisterInput(email, password, name)) {
                Log.d(tag, "Input validation passed, sending register request")
                viewModel.register(email, password, name)
            } else {
                Log.w(tag, "Input validation failed")
            }
        }

        binding.loginLinkTextView.setOnClickListener {
            Log.d(tag, "Login link clicked, navigating to login")
            navigateToLogin()
        }

        viewModel.registerResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Log.d(tag, "Registration successful, navigating to login")
                Snackbar.make(binding.root, "Registration successful", Snackbar.LENGTH_LONG).show()
                navigateToLogin()
            }.onFailure { error ->
                Log.e(tag, "Registration failed: ${error.message}", error)
                Snackbar.make(binding.root, error.message ?: "Registration Error", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToLogin() {
        findNavController().navigate(R.id.action_register_to_login)
    }

    private fun validateRegisterInput(email: String, password: String, name: String): Boolean {
        return when {
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.emailEditText.error = "Invalid email format"
                Log.w(tag, "Validation failed: Invalid email format")
                false
            }
            password.length < 6 -> {
                binding.passwordEditText.error = "Password must be longer than 6 symbols"
                Log.w(tag, "Validation failed: Password too short")
                false
            }
            name.isBlank() -> {
                binding.nameEditText.error = "Name can not be empty"
                Log.w(tag, "Validation failed: Name is empty")
                false
            }
            else -> {
                Log.d(tag, "Validation passed")
                true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(tag, "RegisterFragment destroyed")
        _binding = null
    }
}