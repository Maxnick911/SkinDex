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
import com.example.skindex.databinding.FragmentAuthLoginBinding
import com.example.skindex.viewmodel.AuthViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentAuthLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    private val tag = "LoginFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthLoginBinding.inflate(inflater, container, false)
        Log.d(tag, "LoginFragment view created")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginButton.setOnClickListener {
            Log.d(tag, "Login button clicked")
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            Log.d(tag, "Input data: email=$email")
            if (validateLoginInput(email, password)) {
                Log.d(tag, "Input validation passed, sending login request")
                viewModel.login(email, password)
            } else {
                Log.w(tag, "Input validation failed")
            }
        }

        binding.registerLinkTextView.setOnClickListener {
            Log.d(tag, "Register link clicked, navigating to register")
            findNavController().navigate(R.id.action_login_to_register)
        }

        viewModel.loginResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Log.d(tag, "Login successful")
                navigateToHome()
            }.onFailure { error ->
                Log.e(tag, "Login failed: ${error.message}", error)
                Snackbar.make(binding.root, error.message ?: "Entry error", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun validateLoginInput(email: String, password: String): Boolean {
        return when {
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.emailEditText.error = "Invalid email format"
                Log.w(tag, "Validation failed: Invalid email format")
                false
            }
            password.isBlank() -> {
                binding.passwordEditText.error = "Password cannot be empty"
                Log.w(tag, "Validation failed: Password is empty")
                false
            }
            else -> {
                Log.d(tag, "Validation passed")
                true
            }
        }
    }

    private fun navigateToHome() {
        val action = R.id.action_login_to_doctorProfile
        Log.d(tag, "Navigating to home screen")
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(tag, "LoginFragment destroyed")
        _binding = null
    }
}