package com.isar.kkrakshakavach.sos

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SOSViewmodel(private val repository: SOSRepository) : ViewModel() {

    private val _locationLiveData = MutableLiveData<LocationUpdate>()
    val locationLiveData: LiveData<LocationUpdate> get() = _locationLiveData

    var isSendingSos = MutableLiveData(false)
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

    fun sendCamerasSms(context: Context,newMessage: String) {
        if(allContacts.value.isNullOrEmpty()){
            Toast.makeText(context,"Please Add Contact First", Toast.LENGTH_LONG).show()
            isSendingSos.postValue(false)
            return
        }
        else{
            allContacts.value!!.forEach {
                repository.sendSMS(it,newMessage,context, this)
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
