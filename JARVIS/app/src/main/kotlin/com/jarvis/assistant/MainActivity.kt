package com.jarvis.assistant

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jarvis.assistant.ai.AiCallback
import com.jarvis.assistant.ai.AiService
import com.jarvis.assistant.ai.AiServiceProvider
import com.jarvis.assistant.data.AppPreferences
import com.jarvis.assistant.data.ChatStorage
import com.jarvis.assistant.model.ChatMessage
import com.jarvis.assistant.model.Role
import com.jarvis.assistant.ui.ChatAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var rootView: View
    private lateinit var recyclerChat: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnNewChat: TextView
    private lateinit var btnMenu: TextView

    private lateinit var adapter: ChatAdapter
    private lateinit var storage: ChatStorage
    private lateinit var aiService: AiService
    private lateinit var appPrefs: AppPreferences

    private var requestCounter = 0
    private var isWaiting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        appPrefs = AppPreferences(this)
        AppCompatDelegate.setDefaultNightMode(appPrefs.getThemeMode())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rootView = findViewById(R.id.rootView)
        recyclerChat = findViewById(R.id.recyclerChat)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnNewChat = findViewById(R.id.btnNewChat)
        btnMenu = findViewById(R.id.btnMenu)

        storage = ChatStorage(this)
        aiService = AiServiceProvider.get(this)

        setupWindow()
        setupList()
        setupInput()

        tvSubtitle.text = "AI Assistant  •  " + aiService.displayName
        btnNewChat.setOnClickListener { confirmNewChat() }
        btnMenu.setOnClickListener { showMenu() }

        adapter.setMessages(storage.load(ChatStorage.DEFAULT_CONVERSATION_ID))
        updateEmptyState()
        scrollToBottom()
        updateSendEnabled()
    }

    private fun setupWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val barColor = ContextCompat.getColor(this, R.color.jarvis_bg)
        window.statusBarColor = barColor
        window.navigationBarColor = barColor

        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val controller = WindowCompat.getInsetsController(window, rootView)
        controller.isAppearanceLightStatusBars = !isNight
        controller.isAppearanceLightNavigationBars = !isNight

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun setupList() {
        adapter = ChatAdapter { text -> copyToClipboard(text) }
        recyclerChat.layoutManager = LinearLayoutManager(this)
        recyclerChat.adapter = adapter
        recyclerChat.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom) {
                scrollToBottom()
            }
        }
    }

    private fun setupInput() {
        btnSend.setOnClickListener { sendMessage() }
        etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                updateSendEnabled()
            }
        })
    }

    private fun sendMessage() {
        if (isWaiting) return
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return

        addMessage(ChatMessage(role = Role.USER, text = text))
        etMessage.setText("")

        val history = adapter.snapshot().filter { !it.isError }
        requestCounter++
        val myRequest = requestCounter
        setWaiting(true)

        try {
            aiService.generateReply(history, object : AiCallback {
                override fun onSuccess(reply: String) {
                    if (isFinishing || isDestroyed || myRequest != requestCounter) return
                    setWaiting(false)
                    if (reply.isBlank()) {
                        showError("JARVIS returned an empty response.")
                    } else {
                        addMessage(ChatMessage(role = Role.ASSISTANT, text = reply.trim()))
                    }
                }

                override fun onError(message: String) {
                    if (isFinishing || isDestroyed || myRequest != requestCounter) return
                    setWaiting(false)
                    showError(message)
                }
            })
        } catch (e: Exception) {
            setWaiting(false)
            showError("Unexpected error. Please try again.")
        }
    }

    private fun addMessage(message: ChatMessage) {
        adapter.addMessage(message)
        persist()
        updateEmptyState()
        scrollToBottom()
    }

    private fun showError(message: String) {
        addMessage(ChatMessage(role = Role.ASSISTANT, text = message, isError = true))
    }

    private fun persist() {
        val toSave = adapter.snapshot().filter { !it.isError }
        storage.save(ChatStorage.DEFAULT_CONVERSATION_ID, toSave)
    }

    private fun setWaiting(value: Boolean) {
        isWaiting = value
        adapter.setTyping(value)
        updateSendEnabled()
        updateEmptyState()
        if (value) {
            scrollToBottom()
        }
    }

    private fun updateSendEnabled() {
        btnSend.isEnabled = !isWaiting && etMessage.text.toString().isNotBlank()
    }

    private fun updateEmptyState() {
        tvEmpty.visibility = if (adapter.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun scrollToBottom() {
        recyclerChat.post {
            val count = adapter.itemCount
            if (count > 0) {
                recyclerChat.scrollToPosition(count - 1)
            }
        }
    }

    private fun confirmNewChat() {
        if (adapter.snapshot().isEmpty() && !isWaiting) {
            Toast.makeText(this, "Chat is already empty", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Start a new chat?")
            .setMessage("The current conversation will be deleted.")
            .setPositiveButton("Delete & start new") { _, _ -> startNewChat() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startNewChat() {
        requestCounter++
        isWaiting = false
        adapter.clear()
        storage.clear(ChatStorage.DEFAULT_CONVERSATION_ID)
        updateEmptyState()
        updateSendEnabled()
    }

    private fun copyToClipboard(text: String) {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("JARVIS", text))
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Copy failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMenu() {
        val popup = PopupMenu(this, btnMenu)
        popup.menu.add(0, MENU_THEME, 0, "Theme")
        popup.menu.add(0, MENU_ABOUT, 1, "About JARVIS")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_THEME -> {
                    showThemeDialog()
                    true
                }
                MENU_ABOUT -> {
                    showAboutDialog()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showThemeDialog() {
        val options = arrayOf("Dark", "Light", "System default")
        val modes = intArrayOf(
            AppCompatDelegate.MODE_NIGHT_YES,
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )
        var checked = modes.indexOf(appPrefs.getThemeMode())
        if (checked < 0) checked = 0

        AlertDialog.Builder(this)
            .setTitle("Theme")
            .setSingleChoiceItems(options, checked) { dialog, which ->
                appPrefs.setThemeMode(modes[which])
                dialog.dismiss()
                AppCompatDelegate.setDefaultNightMode(modes[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAboutDialog() {
        val mode = if (aiService.isMock) {
            "Local demo mode. Replies are not from a real AI yet."
        } else {
            "Connected to your AI backend."
        }
        AlertDialog.Builder(this)
            .setTitle("JARVIS v1")
            .setMessage(mode)
            .setPositiveButton("OK", null)
            .show()
    }

    companion object {
        private const val MENU_THEME = 1
        private const val MENU_ABOUT = 2
    }
}