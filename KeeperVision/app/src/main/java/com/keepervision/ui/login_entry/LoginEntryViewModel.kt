package com.keepervision.ui.login_entry

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LoginEntryViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Login Fragment"
    }
    val text: LiveData<String> = _text
}