package com.example.tmplayersuite

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

/**
 * Activity for Telegram integration features.
 */
class TelegramActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telegram)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.telegram_fragment, TelegramFragment())
                .commitNow()
        }
    }
}