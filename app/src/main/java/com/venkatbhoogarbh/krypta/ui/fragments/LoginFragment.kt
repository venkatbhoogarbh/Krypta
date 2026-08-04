package com.venkatbhoogarbh.krypta.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.venkatbhoogarbh.krypta.R
import com.venkatbhoogarbh.krypta.ui.viewmodel.AuthViewModel
import com.venkatbhoogarbh.krypta.util.SessionManager
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private lateinit var viewModel: AuthViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        val passwordInput = view.findViewById<TextInputEditText>(R.id.login_password)
        val loginBtn = view.findViewById<Button>(R.id.login_btn)
        val cooldownText = view.findViewById<TextView>(R.id.cooldown_text)
        val loading = view.findViewById<CircularProgressIndicator>(R.id.login_loading)

        loginBtn.setOnClickListener {
            val pass = passwordInput.text.toString()
            if (pass.isBlank()) {
                passwordInput.error = "Please enter your master password"
                return@setOnClickListener
            }
            viewModel.login(pass)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.authState.collect { state ->
                        when (state) {
                            is AuthViewModel.AuthState.Loading -> {
                                loading.visibility = View.VISIBLE
                                loginBtn.isEnabled = false
                            }
                            is AuthViewModel.AuthState.Success -> {
                                findNavController().navigate(R.id.action_login_to_vault)
                            }
                            is AuthViewModel.AuthState.Error -> {
                                loading.visibility = View.GONE
                                loginBtn.isEnabled = true
                                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                                viewModel.resetState()
                            }
                            else -> {
                                loading.visibility = View.GONE
                                loginBtn.isEnabled = true
                            }
                        }
                    }
                }
                
                launch {
                    SessionManager.cooldownTimeLeft.collect { millis ->
                        if (millis > 0) {
                            cooldownText.visibility = View.VISIBLE
                            cooldownText.text = "Too many attempts. Try again in ${millis / 1000}s"
                            loginBtn.isEnabled = false
                        } else {
                            cooldownText.visibility = View.GONE
                            loginBtn.isEnabled = true
                        }
                    }
                }
            }
        }
    }
}
