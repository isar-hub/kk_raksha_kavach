package com.isar.kkrakshakavach

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.TextView
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
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentViewAllContactBinding.inflate(layoutInflater,container,false)
        getAllContacts()
        return binding.root
    }


    private fun getAllContacts() {
        myDb = DbClassHelper(requireContext())
        // Populate an ArrayList<Pair<String, String>> from the database
        val contactList = ArrayList<Pair<String, String>>() // List of name-phone pairs
        val data: Cursor = myDb.getAllContacts()

        if (data.count == 0) {
            CommonMethods.showSnackBar(binding.root, "No Contacts!!")
        } else {
            while (data.moveToNext()) {
                val nameIndex = data.getColumnIndex(DbClassHelper.COL_NAME)
                val phoneIndex = data.getColumnIndex(DbClassHelper.COL_PHONE)

                // Check if column indices are valid
                if (nameIndex >= 0 && phoneIndex >= 0) {
                    val name = data.getString(nameIndex)
                    val phone = data.getString(phoneIndex)
                    contactList.add(Pair(name, phone)) // Add name-phone pair to the list
                } else {
                    // Handle the case where indices are not found
                    Log.e("DatabaseError", "Column names not found in cursor.")
                }
            }

            // Set the adapter for the ListView
            val listAdapter = ContactsAdapter(requireContext(), contactList)
            binding.listView.adapter = listAdapter
        }
        data.close() // Don't forget to close the cursor
    }

}


class ContactsAdapter(
    context: Context,
    private val contactList: ArrayList<Pair<String, String>> // List of name-phone pairs
) : ArrayAdapter<Pair<String, String>>(context, 0, contactList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Get the data item for this position
        val contact = getItem(position)

        // Check if an existing view is being reused, otherwise inflate the view
        val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_contact, parent, false)

        // Lookup view for data population
        val contactNameTextView = listItemView.findViewById<TextView>(R.id.contactName)
        val contactPhoneTextView = listItemView.findViewById<TextView>(R.id.contactPhone)

        // Populate the data into the template view using the data object
        if (contact != null) {
            contactNameTextView.text = contact.first // Name
            contactPhoneTextView.text = contact.second // Phone
        }

        // Return the completed view to render on screen
        return listItemView
    }
}
