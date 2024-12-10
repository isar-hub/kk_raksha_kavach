package com.isar.kkrakshakavach

import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.isar.kkrakshakavach.databinding.FragmentViewAllContactBinding
import com.isar.kkrakshakavach.db.DbClassHelper
import com.isar.kkrakshakavach.utils.CommonMethods


class ViewAllContact : Fragment() {

    private lateinit var myDb : DbClassHelper
    private lateinit var binding: FragmentViewAllContactBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentViewAllContactBinding.inflate(layoutInflater,container,false)
        return binding.root
    }


    fun getAllContacts(){

        myDb = DbClassHelper(requireContext())
        //populate an ArrayList<String> from the database and then view it
        val theList = ArrayList<String>()
        val data: Cursor = myDb.getListContents()
        if (data.count == 0) {
           CommonMethods.showSnackBar(binding.root,"No Contacts!!")
        } else {
            while (data.moveToNext()) {
                theList.add(data.getString(1))
                val listAdapter: ListAdapter =
                    ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, theList)
                binding.listView.adapter = listAdapter
            }
        }
    }
}