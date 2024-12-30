package com.isar.kkrakshakavach

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.isar.kkrakshakavach.databinding.FragmentSettingBinding


class SettingFragment : Fragment() {


    private lateinit var binding : FragmentSettingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSettingBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val firebaseAuth = Firebase.auth

        binding.signout.setOnClickListener {

            val user = firebaseAuth.currentUser
            if (user != null) {
                firebaseAuth.signOut()
                // Redirect to Login Screen
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            }

        }

    }

}