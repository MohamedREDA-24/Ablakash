package com.xperiencelabs.arapp

import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_TEXT_SENT = 1
        private const val VIEW_TYPE_TEXT_REPLY = 2
        private const val VIEW_TYPE_IMAGE_SENT = 3
        private const val VIEW_TYPE_IMAGE_REPLY = 4
        private const val BASE_IMAGE_URL = "http://13.92.86.232/static/"
    }

    inner class TextSentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.message_text)
    }

    inner class TextReplyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.message_text)
    }

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chatImage: ImageView = itemView.findViewById(R.id.chat_image)
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            messages[position].imageUrl != null && messages[position].isSent -> VIEW_TYPE_IMAGE_SENT
            messages[position].imageUrl != null -> VIEW_TYPE_IMAGE_REPLY
            messages[position].isSent -> VIEW_TYPE_TEXT_SENT
            else -> VIEW_TYPE_TEXT_REPLY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_TEXT_SENT -> TextSentViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_sent, parent, false)
            )
            VIEW_TYPE_TEXT_REPLY -> TextReplyViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_received, parent, false)
            )
            VIEW_TYPE_IMAGE_SENT -> ImageViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_image_sent, parent, false)
            )
            VIEW_TYPE_IMAGE_REPLY -> ImageViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_image, parent, false)
            )
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chatMessage = messages[position]
        when (holder) {
            is TextSentViewHolder -> {
                holder.messageText.text = chatMessage.message
                holder.messageText.movementMethod = LinkMovementMethod.getInstance()
            }
            is TextReplyViewHolder -> {
                // Handle HTML-formatted responses with clickable links
                chatMessage.message?.let {
                    holder.messageText.text = Html.fromHtml(it.toString(), Html.FROM_HTML_MODE_LEGACY)
                    holder.messageText.movementMethod = LinkMovementMethod.getInstance()
                }
            }
            is ImageViewHolder -> {
                val imageUrl = when {
                    chatMessage.isSent && chatMessage.imageUrl?.startsWith("content://") == true ->
                        chatMessage.imageUrl
                    else ->
//                        BASE_IMAGE_URL + chatMessage.imageUrl
                        chatMessage.imageUrl

                }

                Glide.with(holder.itemView.context)
                    .load(imageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
//                    .placeholder(R.drawable.placeholder_image)
//                    .error(R.drawable.error_image)
                    .into(holder.chatImage)
            }
        }
    }

    override fun getItemCount(): Int = messages.size
}