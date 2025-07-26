package com.example.tmplayersuite

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

/**
 * Activity for Telegram login flow.
 */
class TelegramLoginActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telegram_login)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.telegram_login_fragment, TelegramLoginFragment())
                .commitNow()
        }
    }
}