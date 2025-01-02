package com.isar.kkrakshakavach.sos

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.isar.kkrakshakavach.utils.CommonMethods

class SOSViewmodel(private val repository: SOSRepository) : ViewModel() {

    private val _locationLiveData = MutableLiveData<LocationUpdate>()
    val locationLiveData: LiveData<LocationUpdate> get() = _locationLiveData

    var isSendingSos = MutableLiveData(Pair(false,""))
    private var messageString: String = ""
    val message: String get() = messageString
     fun appendMessage(newMessage: String) {
        messageString += if (messageString.isEmpty()) {
            newMessage
        } else {
            "\n$newMessage"
        }
    }

    private val _allContacts = MutableLiveData<List<String>>()
    val allContacts: LiveData<List<String>> get() = _allContacts

    init {
        getAllContacts()
    }

    fun fetchLocation(context: Context) {
        repository.getLocation(context) { location ->
            location.let {
                _locationLiveData.postValue(it)
            }
        }
    }



    private fun getAllContacts() {
        _allContacts.postValue(repository.getAllContacts())
    }

    fun sendSms(contact: String,context: Context) {
        repository.sendSMS(contact, message, context,this)
    }

    fun sendCamerasSms(context: Context, newMessage: String) {
        // Log the state of allContacts
        CommonMethods.showLogs("SOS", "allContacts.value: ${allContacts.value}")

        if (allContacts.value.isNullOrEmpty()) {
            CommonMethods.showLogs("SOS", "allContacts is empty or null")

            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, "Please Add Contact First", Toast.LENGTH_LONG).show()
            }
            isSendingSos.postValue(Pair(false, ""))
            return
        } else {
            CommonMethods.showLogs("SOS", "allContacts is not empty, sending messages")

            isSendingSos.postValue(Pair(true, "Sending Message"))

            allContacts.value!!.forEach {
                CommonMethods.showLogs("SOS", "Sending SMS to: $it")
                repository.sendSMS(it, newMessage, context, this)
            }
        }
    }

}

class SOSViewModelFactory(private val repository: SOSRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SOSViewmodel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SOSViewmodel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
