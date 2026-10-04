package com.jarvis.assistant.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jarvis.assistant.R
import com.jarvis.assistant.model.ChatMessage
import com.jarvis.assistant.model.Role

class ChatAdapter(
    private val onCopy: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = ArrayList<ChatMessage>()
    private var typing = false

    fun snapshot(): List<ChatMessage> = ArrayList(items)

    fun isEmpty(): Boolean = items.isEmpty() && !typing

    fun setMessages(list: List<ChatMessage>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun addMessage(message: ChatMessage) {
        items.add(message)
        notifyItemInserted(items.size - 1)
    }

    fun clear() {
        items.clear()
        typing = false
        notifyDataSetChanged()
    }

    fun setTyping(value: Boolean) {
        if (typing == value) return
        if (value) {
            typing = true
            notifyItemInserted(items.size)
        } else {
            typing = false
            notifyItemRemoved(items.size)
        }
    }

    override fun getItemCount(): Int = items.size + (if (typing) 1 else 0)

    override fun getItemViewType(position: Int): Int {
        if (position >= items.size) return TYPE_TYPING
        return if (items[position].role == Role.USER) TYPE_USER else TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> UserHolder(inflater.inflate(R.layout.item_user_message, parent, false))
            TYPE_AI -> AiHolder(inflater.inflate(R.layout.item_ai_message, parent, false))
            else -> TypingHolder(inflater.inflate(R.layout.item_typing, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position >= items.size) return
        val message = items[position]
        when (holder) {
            is UserHolder -> holder.bind(message)
            is AiHolder -> holder.bind(message)
            else -> {
            }
        }
    }

    inner class UserHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)

        fun bind(message: ChatMessage) {
            tvMessage.text = message.text
            tvMessage.setOnLongClickListener {
                onCopy(message.text)
                true
            }
        }
    }

    inner class AiHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        private val tvCopy: TextView = view.findViewById(R.id.tvCopy)

        fun bind(message: ChatMessage) {
            tvMessage.text = message.text
            val colorRes = if (message.isError) R.color.jarvis_error else R.color.jarvis_text
            tvMessage.setTextColor(ContextCompat.getColor(itemView.context, colorRes))
            tvCopy.visibility = if (message.isError) View.GONE else View.VISIBLE
            tvCopy.setOnClickListener { onCopy(message.text) }
            tvMessage.setOnLongClickListener {
                onCopy(message.text)
                true
            }
        }
    }

    inner class TypingHolder(view: View) : RecyclerView.ViewHolder(view)

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_AI = 1
        private const val TYPE_TYPING = 2
    }
}