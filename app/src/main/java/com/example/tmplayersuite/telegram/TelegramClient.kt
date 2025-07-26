package com.example.tmplayersuite.telegram

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class TelegramClient(private val context: Context) {
    private var client: Client? = null
    private val requestId = AtomicLong(1)
    private val handlers = ConcurrentHashMap<Long, CompletableDeferred<TdApi.Object>>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var apiId: Int = 0
    private var apiHash: String = ""
    private var phoneNumber: String = ""
    
    private val updateChannel = Channel<TdApi.Update>(Channel.UNLIMITED)
    
    init {
        // Initialize TDLib
        Client.execute(TdApi.SetLogVerbosityLevel(1))
        if (Client.execute(TdApi.SetLogStream(TdApi.LogStreamFile("tdlib.log", 1 shl 27, false))) is TdApi.Error) {
            throw RuntimeException("Write access to the current directory is required")
        }
    }
    
    fun initialize(apiId: Int, apiHash: String) {
        this.apiId = apiId
        this.apiHash = apiHash
        
        client = Client.create({ update ->
            when (update.constructor) {
                TdApi.UpdateAuthorizationState.CONSTRUCTOR -> {
                    handleAuthorizationState((update as TdApi.UpdateAuthorizationState).authorizationState)
                }
                else -> {
                    scope.launch {
                        updateChannel.send(update as TdApi.Update)
                    }
                }
            }
        }, null, null)
    }
    
    suspend fun sendRequest(request: TdApi.Function): TdApi.Object {
        val client = this.client ?: throw IllegalStateException("Client not initialized")
        val id = requestId.getAndIncrement()
        val deferred = CompletableDeferred<TdApi.Object>()
        handlers[id] = deferred
        
        client.send(request) { result ->
            val handler = handlers.remove(id)
            handler?.complete(result)
        }
        
        return deferred.await()
    }
    
    fun getUpdates(): Flow<TdApi.Update> = flow {
        while (true) {
            emit(updateChannel.receive())
        }
    }
    
    private fun handleAuthorizationState(authorizationState: TdApi.AuthorizationState) {
        scope.launch {
            when (authorizationState.constructor) {
                TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
                    val parameters = TdApi.TdlibParameters().apply {
                        databaseDirectory = context.filesDir.absolutePath + "/tdlib"
                        useMessageDatabase = true
                        useSecretChats = true
                        this.apiId = this@TelegramClient.apiId
                        this.apiHash = this@TelegramClient.apiHash
                        systemLanguageCode = "en"
                        deviceModel = "Android TV"
                        applicationVersion = "1.0"
                        enableStorageOptimizer = true
                    }
                    sendRequest(TdApi.SetTdlibParameters(parameters))
                }
                TdApi.AuthorizationStateWaitEncryptionKey.CONSTRUCTOR -> {
                    sendRequest(TdApi.CheckDatabaseEncryptionKey())
                }
                TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> {
                    if (phoneNumber.isNotEmpty()) {
                        sendRequest(TdApi.SetAuthenticationPhoneNumber(phoneNumber, null))
                    }
                }
                TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> {
                    // Handle code input - this would typically be done through UI
                }
                TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR -> {
                    // Handle password input - this would typically be done through UI
                }
                TdApi.AuthorizationStateReady.CONSTRUCTOR -> {
                    // Client is ready to use
                    updateChannel.trySend(TdApi.UpdateAuthorizationState(authorizationState))
                }
                TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR -> {
                    // Client is logging out
                }
                TdApi.AuthorizationStateClosing.CONSTRUCTOR -> {
                    // Client is closing
                }
                TdApi.AuthorizationStateClosed.CONSTRUCTOR -> {
                    // Client is closed
                    client?.close()
                }
            }
        }
    }
    
    suspend fun setPhoneNumber(phoneNumber: String) {
        this.phoneNumber = phoneNumber
        sendRequest(TdApi.SetAuthenticationPhoneNumber(phoneNumber, null))
    }
    
    suspend fun checkAuthenticationCode(code: String) {
        sendRequest(TdApi.CheckAuthenticationCode(code))
    }
    
    suspend fun checkAuthenticationPassword(password: String) {
        sendRequest(TdApi.CheckAuthenticationPassword(password))
    }
    
    suspend fun getChats(limit: Int = 20): TdApi.Chats {
        return sendRequest(TdApi.LoadChats(TdApi.ChatListMain(), limit)) as TdApi.Chats
    }
    
    suspend fun getChat(chatId: Long): TdApi.Chat {
        return sendRequest(TdApi.GetChat(chatId)) as TdApi.Chat
    }
    
    suspend fun getChatHistory(chatId: Long, limit: Int = 20): TdApi.Messages {
        return sendRequest(TdApi.GetChatHistory(chatId, 0, 0, limit, false)) as TdApi.Messages
    }
    
    suspend fun sendMessage(chatId: Long, text: String) {
        val inputMessageContent = TdApi.InputMessageText(
            TdApi.FormattedText(text, emptyArray()), 
            false, 
            false
        )
        sendRequest(TdApi.SendMessage(chatId, 0, null, null, null, inputMessageContent))
    }
    
    fun close() {
        scope.cancel()
        client?.close()
        updateChannel.close()
    }
}