package com.isar.kkrakshakavach

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.isar.kkrakshakavach.databinding.FragmentHomeBinding


class HomeFragment : Fragment() {

    private  lateinit var binding : FragmentHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentHomeBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        initialize()
        return binding.root
    }

    private fun initialize(){
        binding.gotoAddContact.setOnClickListener {
            goToAddContact()
        }
        binding.addContactBtn.setOnClickListener {
            goToAddContact()
        }
        binding.goToSOS.setOnClickListener {
            goTOSOS()
        }
    }

    private fun goTOSOS() {
        Navigation.findNavController(binding.root).navigate(R.id.action_homeFragment_to_soSFragment)
    }


    private fun goToAddContact() {
        Navigation.findNavController(binding.root).navigate(R.id.action_homeFragment_to_contactsFragment)
    }

}