package com.example.tmplayersuite.telegram

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.td.libcore.telegram.TdApi

class TelegramManager(private val context: Context) {
    private var telegramClient: TelegramClient? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.NotInitialized)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _chats = MutableStateFlow<List<TdApi.Chat>>(emptyList())
    val chats: StateFlow<List<TdApi.Chat>> = _chats.asStateFlow()
    
    sealed class AuthState {
        object NotInitialized : AuthState()
        object WaitingForPhoneNumber : AuthState()
        object WaitingForCode : AuthState()
        object WaitingForPassword : AuthState()
        object Ready : AuthState()
        data class Error(val message: String) : AuthState()
    }
    
    fun initialize(apiId: Int, apiHash: String) {
        try {
            telegramClient = TelegramClient(context).apply {
                initialize(apiId, apiHash)
            }
            
            // Listen for updates
            scope.launch {
                telegramClient?.getUpdates()?.collect { update ->
                    handleUpdate(update)
                }
            }
            
            _authState.value = AuthState.WaitingForPhoneNumber
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Failed to initialize: ${e.message}")
        }
    }
    
    private suspend fun handleUpdate(update: TdApi.Update) {
        when (update.constructor) {
            TdApi.UpdateAuthorizationState.CONSTRUCTOR -> {
                val authUpdate = update as TdApi.UpdateAuthorizationState
                when (authUpdate.authorizationState.constructor) {
                    TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> {
                        _authState.value = AuthState.WaitingForPhoneNumber
                    }
                    TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> {
                        _authState.value = AuthState.WaitingForCode
                    }
                    TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR -> {
                        _authState.value = AuthState.WaitingForPassword
                    }
                    TdApi.AuthorizationStateReady.CONSTRUCTOR -> {
                        _authState.value = AuthState.Ready
                        loadChats()
                    }
                }
            }
            TdApi.UpdateNewChat.CONSTRUCTOR -> {
                val chatUpdate = update as TdApi.UpdateNewChat
                val currentChats = _chats.value.toMutableList()
                currentChats.add(chatUpdate.chat)
                _chats.value = currentChats.sortedByDescending { it.lastMessage?.date ?: 0 }
            }
            TdApi.UpdateChatLastMessage.CONSTRUCTOR -> {
                val messageUpdate = update as TdApi.UpdateChatLastMessage
                val currentChats = _chats.value.toMutableList()
                val chatIndex = currentChats.indexOfFirst { it.id == messageUpdate.chatId }
                if (chatIndex != -1) {
                    currentChats[chatIndex] = currentChats[chatIndex].apply {
                        lastMessage = messageUpdate.lastMessage
                    }
                    _chats.value = currentChats.sortedByDescending { it.lastMessage?.date ?: 0 }
                }
            }
        }
    }
    
    suspend fun setPhoneNumber(phoneNumber: String): Result<Unit> {
        return try {
            telegramClient?.setPhoneNumber(phoneNumber)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun checkAuthenticationCode(code: String): Result<Unit> {
        return try {
            telegramClient?.checkAuthenticationCode(code)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun checkAuthenticationPassword(password: String): Result<Unit> {
        return try {
            telegramClient?.checkAuthenticationPassword(password)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun loadChats() {
        try {
            val chatsResult = telegramClient?.getChats(50) as? TdApi.Chats
            chatsResult?.let { result ->
                val chatList = mutableListOf<TdApi.Chat>()
                for (chatId in result.chatIds) {
                    val chat = telegramClient?.getChat(chatId) as? TdApi.Chat
                    chat?.let { chatList.add(it) }
                }
                _chats.value = chatList.sortedByDescending { it.lastMessage?.date ?: 0 }
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Failed to load chats: ${e.message}")
        }
    }
    
    suspend fun sendMessage(chatId: Long, text: String): Result<Unit> {
        return try {
            telegramClient?.sendMessage(chatId, text)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getChatHistory(chatId: Long, limit: Int = 20): Result<TdApi.Messages> {
        return try {
            val messages = telegramClient?.getChatHistory(chatId, limit) as TdApi.Messages
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun destroy() {
        scope.cancel()
        telegramClient?.close()
    }
}