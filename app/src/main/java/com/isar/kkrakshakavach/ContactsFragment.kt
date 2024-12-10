package com.isar.kkrakshakavach

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.isar.kkrakshakavach.databinding.FragmentContactsBinding
import com.isar.kkrakshakavach.utils.CommonMethods


class ContactsFragment : Fragment() {



    private lateinit var binding : FragmentContactsBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding= FragmentContactsBinding.inflate(layoutInflater,container,false)
        // Inflate the layout for this fragment
        return binding.root
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

    import android.content.ContentResolver;
    import android.database.Cursor;
    import android.provider.ContactsContract;
    import android.provider.ContactsContract.CommonDataKinds.Phone;

    public fun getContacts() {
        // Define a list to store contacts
        val contactsList =  ArrayList<String>();

        // Get the content resolver to query contacts
        val contentResolver : ContentResolver = requireContext().contentResolver;

        // Define the URI for contacts
        val uri = ContactsContract.Contacts.CONTENT_URI;

        // Query the contacts
        val cursor = contentResolver.query(uri, null, null, null, null);

        if (cursor != null && cursor.count > 0) {
            while (cursor.moveToNext()) {
                // v contact name
                val contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                // Get contact ID
                val contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));

                // Retrieve phone numbers for this contact
                val phoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    arrayOf(contactId)
                null
                );

                if (phoneCursor != null && phoneCursor.getCount() > 0) {
                    while (phoneCursor.moveToNext()) {
                        val phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.NUMBER));
                        contactsList.add("Name: " + contactName + ", Phone: " + phoneNumber);
                    }
                    phoneCursor.close();
                }
            }
            cursor.close();
        }
    }

}