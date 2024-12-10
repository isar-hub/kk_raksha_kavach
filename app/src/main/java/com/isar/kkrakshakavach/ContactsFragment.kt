package com.isar.kkrakshakavach

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.isar.kkrakshakavach.databinding.FragmentContactsBinding
import com.isar.kkrakshakavach.db.DbClassHelper
import com.isar.kkrakshakavach.utils.CommonMethods


class ContactsFragment : Fragment() {



    private lateinit var binding : FragmentContactsBinding
    private lateinit var myDB : DbClassHelper
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding= FragmentContactsBinding.inflate(layoutInflater,container,false)
        // Inflate the layout for this fragment
        myDB = DbClassHelper(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.discardBtn.setOnClickListener {
            val fragmentB = ViewAllContact()
            childFragmentManager.beginTransaction() // Use childFragmentManager if this is a nested fragment
                .replace(R.id.main, fragmentB)
                .addToBackStack(null)
                .commit()
        }
        binding.saveBtn.setOnClickListener {
            saveContact()
        }
    }
    private fun saveContact(){
        val name = binding.nameField.text.toString()
        val phone = binding.numberField.text.toString()
        if(name.isEmpty() || phone.isEmpty() ){
            CommonMethods.showSnackBar(binding.root,"Please fill all fields")
            return
        }
        myDB.addData(name,phone,email = null)
        CommonMethods.showSnackBar(binding.root,"Contact saved successfully")
        clearFields()



    }

    private fun clearFields() {
        binding.nameField.text.clear()
        binding.numberField.text.clear()
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_menu,menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.allContacts){
            CommonMethods.showSnackBar(binding.root,"CLicked")
            return true;
        }
    return super.onOptionsItemSelected(item)
    }
    companion object{
        private val CONTACT_PICK_REQUEST = 1
    }
    private fun openContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        contactPickerLauncher.launch(intent)
    }


    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == CONTACT_PICK_REQUEST) {
            result.data?.data?.let { contactUri ->
                handleSelectedContact(contactUri)
            }
        }
    }
    private fun handleSelectedContact(contactUri: Uri) {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        requireContext().contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val name = cursor.getString(nameIndex)
                val phoneNumber = cursor.getString(numberIndex)

//                // Save to database
//                saveContactToDatabase(name)
//                saveContactToDatabase(phoneNumber);
            }
        }
    }

    private fun saveContactToDatabase(name: String,phone : String) {
        try {
            myDB.addData(name,phone,null)

        }catch (e : Exception){
            CommonMethods.showSnackBar(binding.root,"Error saving contact to database")
            e.printStackTrace()
        }

    }


//
//    public fun getContacts() {
//
//        val contactsList =  ArrayList<String>();
//        val contentResolver : ContentResolver = requireContext().contentResolver;
//
//        val uri = ContactsContract.Contacts.CONTENT_URI;
//        val cursor = contentResolver.query(uri, null, null, null, null);
//
//        if (cursor != null && cursor.count > 0) {
//            while (cursor.moveToNext()) {
//                // v contact name
//                val contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
//
//                // Get contact ID
//                val contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
//
//                // Retrieve phone numbers for this contact
//                val phoneCursor = contentResolver.query(
//                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
//                null,
//                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
//                    arrayOf(contactId)
//                null
//                );
//
//                if (phoneCursor != null && phoneCursor.getCount() > 0) {
//                    while (phoneCursor.moveToNext()) {
//                        val phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
//                        contactsList.add("Name: " + contactName + ", Phone: " + phoneNumber);
//                    }
//                    phoneCursor.close();
//                }
//            }
//            cursor.close();
//        }
//    }

}