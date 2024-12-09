package com.isar.kkrakshakavach.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.isar.kkrakshakavach.utils.Results

class LoginViewModel : ViewModel() {

    private val firebaseAuth = FirebaseAuth.getInstance()

    // LiveData for Login
    private val _loginLive = MutableLiveData<Results<FirebaseUser>>()
    val loginLive: LiveData<Results<FirebaseUser>> get() = _loginLive

    // LiveData for Registration
    private val _registerLive = MutableLiveData<Results<FirebaseUser>>()
    val registerLive: LiveData<Results<FirebaseUser>> get() = _registerLive

    /**
     * Login with Email and Password
     */
    fun loginWithEmail(email: String, password: String) {
        _loginLive.postValue(Results.Loading()) // Emit loading state

        try {
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = firebaseAuth.currentUser
                        _loginLive.postValue(Results.Success(user!!))
                    } else {
                        val errorMessage = task.exception?.message ?: "Login failed"
                        _loginLive.postValue(Results.Error(errorMessage))
                    }
                }
        } catch (e: Exception) {
            _loginLive.postValue(Results.Error(e.message ?: "Unexpected error occurred"))
        }
    }

    /**
     * Register with Email and Password
     */
    fun registerWithEmail(email: String, password: String) {
        _registerLive.postValue(Results.Loading()) // Emit loading state

        try {
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = firebaseAuth.currentUser
                        _registerLive.postValue(Results.Success(user!!))
                    } else {
                        val errorMessage = task.exception?.message ?: "Registration failed"
                        _registerLive.postValue(Results.Error(errorMessage))
                    }
                }
        } catch (e: Exception) {
            _registerLive.postValue(Results.Error(e.message ?: "Unexpected error occurred"))
        }
    }
}
