package com.isar.kkrakshakavach

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.isar.kkrakshakavach.databinding.ActivityLoginBinding
import com.isar.kkrakshakavach.viewmodels.LoginViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]



    }


    private fun validateFields(): Boolean {
        var isValid = true
        if (binding.emailField.text.isNullOrEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(binding.emailField.text).matches()) {
            binding.emailField.error = ("Invalid email address")
            isValid = false
        }
        if (binding.passwordField.text.isNullOrEmpty() || binding.passwordField.text!!.length < 6) {
            binding.passwordField.error = ("Password must be at least 6 characters")
            isValid = false
        }
        return isValid
    }
}