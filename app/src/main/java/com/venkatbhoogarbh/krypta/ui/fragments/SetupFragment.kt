package com.venkatbhoogarbh.krypta.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.venkatbhoogarbh.krypta.R
import com.venkatbhoogarbh.krypta.ui.viewmodel.AuthViewModel
import com.venkatbhoogarbh.krypta.util.PasswordStrength
import com.venkatbhoogarbh.krypta.util.PasswordUtils
import kotlinx.coroutines.launch

class SetupFragment : Fragment() {

    private lateinit var viewModel: AuthViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_setup_master, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        val passwordInput = view.findViewById<TextInputEditText>(R.id.setup_password)
        val confirmInput = view.findViewById<TextInputEditText>(R.id.setup_confirm_password)
        val confirmLayout = view.findViewById<TextInputLayout>(R.id.setup_confirm_password_layout)
        val strengthIndicator = view.findViewById<LinearProgressIndicator>(R.id.strength_indicator)
        val strengthLabel = view.findViewById<TextView>(R.id.strength_label)
        val setupBtn = view.findViewById<Button>(R.id.setup_btn)
        val loading = view.findViewById<CircularProgressIndicator>(R.id.setup_loading)

        passwordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val pass = s?.toString() ?: ""
                if (pass.isEmpty()) {
                    strengthIndicator.visibility = View.GONE
                    strengthLabel.visibility = View.GONE
                } else {
                    val strength = PasswordUtils.evaluateStrength(pass)
                    strengthIndicator.visibility = View.VISIBLE
                    strengthLabel.visibility = View.VISIBLE
                    strengthIndicator.progress = strength.score * 25
                    strengthIndicator.setIndicatorColor(ContextCompat.getColor(requireContext(), strength.colorResId))
                    strengthLabel.text = getString(R.string.password_strength_label, getString(strength.labelResId))
                    strengthLabel.setTextColor(ContextCompat.getColor(requireContext(), strength.colorResId))
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        setupBtn.setOnClickListener {
            val pass = passwordInput.text.toString()
            val confirm = confirmInput.text.toString()

            if (pass != confirm) {
                confirmLayout.error = "Passwords do not match"
                return@setOnClickListener
            }

            val strength = PasswordUtils.evaluateStrength(pass)
            if (strength == PasswordStrength.WEAK) {
                passwordInput.error = "Master password must be at least Medium strength"
                return@setOnClickListener
            }

            viewModel.setupMasterPassword(pass)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { state ->
                    when (state) {
                        is AuthViewModel.AuthState.Loading -> {
                            loading.visibility = View.VISIBLE
                            setupBtn.isEnabled = false
                        }
                        is AuthViewModel.AuthState.Success -> {
                            findNavController().navigate(R.id.action_setup_to_vault)
                        }
                        is AuthViewModel.AuthState.Error -> {
                            loading.visibility = View.GONE
                            setupBtn.isEnabled = true
                            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                            viewModel.resetState()
                        }
                        else -> {
                            loading.visibility = View.GONE
                            setupBtn.isEnabled = true
                        }
                    }
                }
            }
        }
    }
}
