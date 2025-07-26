package com.example.tmplayersuite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tmplayersuite.telegram.TelegramManager
import kotlinx.coroutines.launch

class TelegramFragment : Fragment() {
    
    private lateinit var telegramManager: TelegramManager
    private lateinit var statusText: TextView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_telegram, container, false)
        
        statusText = view.findViewById(R.id.telegram_status)
        
        telegramManager = TelegramManager(requireContext())
        
        // Initialize with your API credentials
        // You need to get these from https://my.telegram.org/apps
        val apiId = 0 // Replace with your API ID
        val apiHash = "" // Replace with your API Hash
        
        if (apiId != 0 && apiHash.isNotEmpty()) {
            telegramManager.initialize(apiId, apiHash)
            
            // Observe auth state
            lifecycleScope.launch {
                telegramManager.authState.collect { authState ->
                    updateStatus(authState)
                }
            }
            
            // Observe chats
            lifecycleScope.launch {
                telegramManager.chats.collect { chats ->
                    // Update UI with chats
                    statusText.text = "Loaded ${chats.size} chats"
                }
            }
        } else {
            statusText.text = "Please configure API credentials in TelegramFragment"
        }
        
        return view
    }
    
    private fun updateStatus(authState: TelegramManager.AuthState) {
        val status = when (authState) {
            is TelegramManager.AuthState.NotInitialized -> "Not initialized"
            is TelegramManager.AuthState.WaitingForPhoneNumber -> "Waiting for phone number"
            is TelegramManager.AuthState.WaitingForCode -> "Waiting for verification code"
            is TelegramManager.AuthState.WaitingForPassword -> "Waiting for password"
            is TelegramManager.AuthState.Ready -> "Ready - Connected to Telegram"
            is TelegramManager.AuthState.Error -> "Error: ${authState.message}"
        }
        statusText.text = status
    }
    
    override fun onDestroy() {
        super.onDestroy()
        telegramManager.destroy()
    }
}