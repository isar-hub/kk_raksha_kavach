package com.isar.kkrakshakavach

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.isar.kkrakshakavach.databinding.ActivityLoginBinding
import com.isar.kkrakshakavach.utils.CommonMethods
import com.isar.kkrakshakavach.utils.Results
import com.isar.kkrakshakavach.viewmodels.LoginViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel
    private lateinit var googleSignInClient: GoogleSignInAccount


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)



        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        val user = Firebase.auth.currentUser
        if (user != null) {
            gotoHome(user)
        }

        forgotPass()

        observers()

        val signInRequest = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()

        val mGoogleSignInClient = GoogleSignIn.getClient(this, signInRequest)

        binding.googleSignInButton.setOnClickListener {
            val signInIntent = mGoogleSignInClient.signInIntent
            startActivityForResult(signInIntent, 1)
        }
        binding.goToRegister.setOnClickListener {
            goToRegisterPage()
        }
        binding.loginBtn.setOnClickListener {
            login()
        }
    }

    private fun forgotPass() {
        binding.forgotPass.setOnClickListener {
            // Create a dialog
            val dialog = android.app.AlertDialog.Builder(this)
            val emailInput = EditText(this)
            emailInput.hint = "Enter your email"
            emailInput.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

            dialog.setTitle("Forgot Password")
            dialog.setMessage("Enter your email to reset your password")
            dialog.setView(emailInput)
            dialog.setPositiveButton("Submit") { _, _ ->
                val email = emailInput.text.toString().trim()
                if (email.isEmpty()) {
                    Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                } else {
                    // Call forgotPassword method
                    viewModel.forgotPassword(email) { isSuccess ->
                        if (isSuccess) {
                            Toast.makeText(this, "Reset email sent successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Failed to send reset email", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            dialog.setNegativeButton("Cancel") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            dialog.show()
        }
    }

    private fun login() {
        if (validateFields()) {
            viewModel.loginWithEmail(
                binding.emailField.text.toString(), binding.passwordField.text.toString()
            )

        }
    }


    private fun goToRegisterPage() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }

    private fun observers() {
        viewModel.loginLive.observe(this) {
            when (it) {
                is Results.Error -> {
                    CommonMethods.hideLoader()
                    CommonMethods.showSnackBar(
                        binding.root,

                        "Error : ${it.message}", isSuccess = false
                    )
                }

                is Results.Loading -> {
                    CommonMethods.showLoader(this)
                }

                is Results.Success -> {
                    CommonMethods.hideLoader()
                    CommonMethods.showSnackBar(binding.root, "Success : ${it.data?.email}")
                    gotoHome(it.data!!)
                }
            }
        }
    }

    private fun gotoHome(user: FirebaseUser) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("user", user)
        }
        startActivity(
            intent
        )
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { viewModel.firebaseAuthWithGoogle(it) }
            } catch (e: ApiException) {
                CommonMethods.showSnackBar(binding.root, "Error : ${e.message}")
            }
        }
    }

    private fun validateFields(): Boolean {
        var isValid = true

        if (binding.emailField.text.isNullOrEmpty()) {
            binding.emailField.error = "Email cannot be empty"
            isValid = false
        }

        if (binding.passwordField.text.isNullOrEmpty()) {
            binding.passwordField.error = "Password cannot be empty"
            isValid = false
        }

        return isValid
    }

}