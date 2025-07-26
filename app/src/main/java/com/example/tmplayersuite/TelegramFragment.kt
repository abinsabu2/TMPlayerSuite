package com.example.tmplayersuite

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tmplayersuite.telegram.TelegramManager
import kotlinx.coroutines.launch

class TelegramFragment : Fragment() {
    
    private lateinit var telegramManager: TelegramManager
    private lateinit var statusText: TextView
    private lateinit var loginButton: Button
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_telegram, container, false)
        
        statusText = view.findViewById(R.id.telegram_status)
        loginButton = view.findViewById(R.id.login_button)
        
        telegramManager = TelegramManager(requireContext())
        
        loginButton.setOnClickListener {
            val intent = Intent(requireContext(), TelegramLoginActivity::class.java)
            startActivity(intent)
        }
        
        statusText.text = "Click Login to connect to Telegram"
        
        return view
    }
}