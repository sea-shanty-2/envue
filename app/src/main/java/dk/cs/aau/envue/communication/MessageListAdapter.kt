package dk.cs.aau.envue.communication

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import dk.cs.aau.envue.R


class MessageListAdapter(private val context: Context, private val messageList: List<Message>,
                         private val isStreamerView: Boolean = false,
                         var isLandscape: Boolean = false) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var lastPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val holder =
        print("create view holder")

        // Select layout
        val layout = if (isLandscape || isStreamerView) R.layout.viewer_message else when (viewType) {
            VIEW_TYPE_MESSAGE_RECEIVED -> R.layout.other_message
            else -> R.layout.own_message
        }

        // Select appropriate message holder
        return when (viewType) {
            VIEW_TYPE_MESSAGE_RECEIVED -> ReceivedMessageHolder(
                LayoutInflater.from(parent.context).inflate(
                    layout,
                    parent,
                    false
                )
            )
            else -> SentMessageHolder(LayoutInflater.from(parent.context).inflate(
                layout,
                parent,
                false
            ))
        }
    }

    fun setLandscapeMode(newMode: Boolean) {
        if (this.isLandscape != newMode) {
            this.isLandscape = newMode
            this.notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    override fun getItemViewType(position: Int): Int {
        val message = messageList[position]

        return if (message.author == null) {
            VIEW_TYPE_MESSAGE_SENT
        } else {
            VIEW_TYPE_MESSAGE_RECEIVED
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList[position]

        when (holder.itemViewType) {
            VIEW_TYPE_MESSAGE_SENT -> (holder as SentMessageHolder).bind(message)
            VIEW_TYPE_MESSAGE_RECEIVED -> (holder as ReceivedMessageHolder).bind(message)
        }

        setAnimation(holder.itemView, position)
    }

    private fun setAnimation(itemView: View, position: Int) {
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
            itemView.startAnimation(animation)
            lastPosition = position
        }
    }

    companion object {
        private const val VIEW_TYPE_MESSAGE_SENT = 1
        private const val VIEW_TYPE_MESSAGE_RECEIVED = 2
    }
}