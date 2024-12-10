package com.isar.kkrakshakavach

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.isar.kkrakshakavach.databinding.ActivityRegisterBinding
import com.isar.kkrakshakavach.utils.CommonMethods
import com.isar.kkrakshakavach.utils.Results
import com.isar.kkrakshakavach.viewmodels.LoginViewModel

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var viewModel: LoginViewModel
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]


        firebaseAuth = FirebaseAuth.getInstance()

        observers()
        binding.registerbtn.setOnClickListener {
            register()
        }

        binding.googleSignInButton.setOnClickListener {
            Toast.makeText(this, "Google Sign-In functionality not implemented yet", Toast.LENGTH_SHORT).show()
        }

        binding.goToLogin.setOnClickListener {
            goTOLogin()
        }
    }
    private fun goTOLogin(){
        startActivity(Intent(this, LoginActivity::class.java))

    }

    private fun validateFields(): Boolean {
        var isValid = true

        if (binding.fullNameEdtx.text.isNullOrEmpty()) {
            binding.fullNameEdtx.error = "Full Name cannot be empty"
            isValid = false
        }

        if (binding.emailEdtx.text.isNullOrEmpty()) {
            binding.emailEdtx.error = "Email cannot be empty"
            isValid = false
        }

        if (binding.passEdtx.text.isNullOrEmpty()) {
            binding.passEdtx.error = "Password cannot be empty"
            isValid = false
        }

        if (binding.confirmPassEdtx.text.isNullOrEmpty()) {
            binding.confirmPassEdtx.error = "Confirm Password cannot be empty"
            isValid = false
        } else if (binding.passEdtx.text.toString() != binding.confirmPassEdtx.text.toString()) {
            binding.confirmPassEdtx.error = "Passwords do not match"
            isValid = false
        }

        if (!binding.termsAndConditionCheckbox.isChecked) {
            Toast.makeText(this, "Please accept the Terms and Conditions", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun register() {
        if(validateFields()){
            viewModel.registerWithEmail(email = binding.emailEdtx.text.toString(), password = binding.passEdtx.text.toString() )
        }
    }

    private fun observers(){
        viewModel.registerLive.observe(this){
            when(it){
                is Results.Error -> {
                    CommonMethods.hideLoader()
                    CommonMethods.showSnackBar(binding.root,"Error : ${it.message}", isSuccess = false)
                }
                is Results.Loading -> {
                    CommonMethods.showLoader(this)
                }
                is Results.Success -> {
                    CommonMethods.hideLoader()
                    CommonMethods.showSnackBar(binding.root,"Success : ${it.data?.email}")
                    goTOLogin()
                }
            }
        }
    }

}