package com.example.tmplayersuite

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.lifecycle.lifecycleScope
import com.example.tmplayersuite.telegram.TelegramManager
import kotlinx.coroutines.launch

class TelegramLoginFragment : GuidedStepSupportFragment() {
    
    private lateinit var telegramManager: TelegramManager
    private var currentStep = LoginStep.PHONE_NUMBER
    
    enum class LoginStep {
        PHONE_NUMBER,
        VERIFICATION_CODE,
        PASSWORD
    }
    
    companion object {
        private const val ACTION_PHONE_INPUT = 1L
        private const val ACTION_CODE_INPUT = 2L
        private const val ACTION_PASSWORD_INPUT = 3L
        private const val ACTION_SUBMIT = 4L
        private const val ACTION_CANCEL = 5L
        
        // Replace with your actual API credentials
        private const val API_ID = 0 // Get from https://my.telegram.org/apps
        private const val API_HASH = "" // Get from https://my.telegram.org/apps
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        telegramManager = TelegramManager(requireContext())
        
        if (API_ID != 0 && API_HASH.isNotEmpty()) {
            telegramManager.initialize(API_ID, API_HASH)
            
            // Observe auth state changes
            lifecycleScope.launch {
                telegramManager.authState.collect { authState ->
                    handleAuthStateChange(authState)
                }
            }
        } else {
            Toast.makeText(context, "Please configure API credentials", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        return when (currentStep) {
            LoginStep.PHONE_NUMBER -> GuidanceStylist.Guidance(
                "Telegram Login",
                "Enter your phone number to sign in to Telegram",
                "TMPlayerSuite",
                requireContext().getDrawable(R.drawable.app_icon_your_company)
            )
            LoginStep.VERIFICATION_CODE -> GuidanceStylist.Guidance(
                "Verification Code",
                "Enter the verification code sent to your phone",
                "TMPlayerSuite",
                requireContext().getDrawable(R.drawable.app_icon_your_company)
            )
            LoginStep.PASSWORD -> GuidanceStylist.Guidance(
                "Two-Factor Authentication",
                "Enter your Telegram password",
                "TMPlayerSuite",
                requireContext().getDrawable(R.drawable.app_icon_your_company)
            )
        }
    }
    
    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        when (currentStep) {
            LoginStep.PHONE_NUMBER -> {
                actions.add(
                    GuidedAction.Builder(requireContext())
                        .id(ACTION_PHONE_INPUT)
                        .title("Phone Number")
                        .description("Enter with country code (e.g., +1234567890)")
                        .editable(true)
                        .build()
                )
            }
            LoginStep.VERIFICATION_CODE -> {
                actions.add(
                    GuidedAction.Builder(requireContext())
                        .id(ACTION_CODE_INPUT)
                        .title("Verification Code")
                        .description("Enter the 5-digit code")
                        .editable(true)
                        .build()
                )
            }
            LoginStep.PASSWORD -> {
                actions.add(
                    GuidedAction.Builder(requireContext())
                        .id(ACTION_PASSWORD_INPUT)
                        .title("Password")
                        .description("Enter your Telegram password")
                        .editable(true)
                        .build()
                )
            }
        }
        
        // Add submit and cancel buttons
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ACTION_SUBMIT)
                .title("Submit")
                .build()
        )
        
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ACTION_CANCEL)
                .title("Cancel")
                .build()
        )
    }
    
    override fun onGuidedActionClicked(action: GuidedAction) {
        when (action.id) {
            ACTION_SUBMIT -> handleSubmit()
            ACTION_CANCEL -> {
                telegramManager.destroy()
                requireActivity().finish()
            }
        }
    }
    
    private fun handleSubmit() {
        lifecycleScope.launch {
            try {
                when (currentStep) {
                    LoginStep.PHONE_NUMBER -> {
                        val phoneAction = findActionById(ACTION_PHONE_INPUT)
                        val phoneNumber = phoneAction?.editTitle?.toString()?.trim()
                        
                        if (phoneNumber.isNullOrEmpty()) {
                            showError("Please enter a phone number")
                            return@launch
                        }
                        
                        val result = telegramManager.setPhoneNumber(phoneNumber)
                        if (result.isFailure) {
                            showError("Failed to send verification code: ${result.exceptionOrNull()?.message}")
                        }
                    }
                    
                    LoginStep.VERIFICATION_CODE -> {
                        val codeAction = findActionById(ACTION_CODE_INPUT)
                        val code = codeAction?.editTitle?.toString()?.trim()
                        
                        if (code.isNullOrEmpty()) {
                            showError("Please enter the verification code")
                            return@launch
                        }
                        
                        val result = telegramManager.checkAuthenticationCode(code)
                        if (result.isFailure) {
                            showError("Invalid verification code: ${result.exceptionOrNull()?.message}")
                        }
                    }
                    
                    LoginStep.PASSWORD -> {
                        val passwordAction = findActionById(ACTION_PASSWORD_INPUT)
                        val password = passwordAction?.editTitle?.toString()?.trim()
                        
                        if (password.isNullOrEmpty()) {
                            showError("Please enter your password")
                            return@launch
                        }
                        
                        val result = telegramManager.checkAuthenticationPassword(password)
                        if (result.isFailure) {
                            showError("Invalid password: ${result.exceptionOrNull()?.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                showError("An error occurred: ${e.message}")
            }
        }
    }
    
    private fun handleAuthStateChange(authState: TelegramManager.AuthState) {
        when (authState) {
            is TelegramManager.AuthState.WaitingForCode -> {
                if (currentStep == LoginStep.PHONE_NUMBER) {
                    currentStep = LoginStep.VERIFICATION_CODE
                    refreshGuidedActions()
                }
            }
            is TelegramManager.AuthState.WaitingForPassword -> {
                currentStep = LoginStep.PASSWORD
                refreshGuidedActions()
            }
            is TelegramManager.AuthState.Ready -> {
                Toast.makeText(context, "Successfully logged in to Telegram!", Toast.LENGTH_SHORT).show()
                
                // Navigate to main Telegram activity
                val intent = Intent(requireContext(), TelegramActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            }
            is TelegramManager.AuthState.Error -> {
                showError(authState.message)
            }
            else -> {
                // Handle other states if needed
            }
        }
    }
    
    private fun refreshGuidedActions() {
        // Clear current actions and recreate them
        actions.clear()
        onCreateActions(actions, null)
        notifyActionChanged(findActionPositionById(ACTION_PHONE_INPUT))
    }
    
    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::telegramManager.isInitialized) {
            telegramManager.destroy()
        }
    }
}