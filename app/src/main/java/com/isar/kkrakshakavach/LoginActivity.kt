package com.isar.kkrakshakavach

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
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

        FirebaseApp.initializeApp(this)

        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        val user = Firebase.auth.currentUser
        if ( user != null){
            gotoHome(user)
        }

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