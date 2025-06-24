package com.example.sup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatMessageDiffCallback()) {

    private val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        if (holder is SentMessageViewHolder) {
            holder.bind(message)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(message)
        }
    }

    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.textViewMessageText)
        private val messageTimestamp: TextView = itemView.findViewById(R.id.textViewMessageTimestamp)

        fun bind(chatMessage: ChatMessage) {
            messageText.text = chatMessage.text
            messageTimestamp.text = chatMessage.timestamp?.toDate()?.let {
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(it)
            } ?: ""
        }
    }

    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val senderNameText: TextView = itemView.findViewById(R.id.textViewMessageSender)
        private val messageText: TextView = itemView.findViewById(R.id.textViewMessageText)
        private val messageTimestamp: TextView = itemView.findViewById(R.id.textViewMessageTimestamp)

        fun bind(chatMessage: ChatMessage) {
            senderNameText.text = chatMessage.senderName
            messageText.text = chatMessage.text
            messageTimestamp.text = chatMessage.timestamp?.toDate()?.let {
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(it)
            } ?: ""
        }
    }
}

class ChatMessageDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
    override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
        return oldItem == newItem
    }
}
