package com.example.ideality.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.IOException

data class PrivacyPolicyState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val content: PrivacyPolicyContent? = null
)

data class PrivacyPolicyContent(
    val introduction: String,
    val informationWeCollect: List<String>,
    val howWeUseInfo: List<String>,
    val dataSecurity: String
)

class PrivacyPolicyViewModel : ViewModel() {
    private val _state = MutableLiveData(PrivacyPolicyState())
    val state: LiveData<PrivacyPolicyState> = _state

    fun loadPrivacyPolicy() {
        viewModelScope.launch {
            _state.value = _state.value?.copy(isLoading = true)
            try {
                // This could be replaced with actual API call in the future
                val content = PrivacyPolicyContent(
                    introduction = "This privacy policy explains how we collect...",
                    informationWeCollect = listOf(
                        "Account Information: Email, name, and phone number...",
                        "Device Information: Camera access for AR features...",
                        "Usage Information: How you interact with our app...",
                        "Location Information: Approximate location..."
                    ),
                    howWeUseInfo = listOf(
                        "Provide and improve our AR furniture visualization service",
                        "Process and fulfill your orders",
                        "Send important notifications about your orders or account",
                        "Improve our app's performance and user experience"
                    ),
                    dataSecurity = "We implement appropriate security measures..."
                )
                _state.value = _state.value?.copy(
                    isLoading = false,
                    content = content,
                    error = null
                )
            } catch (e: IOException) {
                _state.value = _state.value?.copy(
                    isLoading = false,
                    error = "Failed to load privacy policy. Please try again."
                )
            }
        }
    }
}