package dk.cs.aau.envue.communication

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import dk.cs.aau.envue.R


class MessageListAdapter(private val context: Context, private val messageList: List<Message>,
                         private val isStreamerView: Boolean = false) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var isLandscape: Boolean = false
        set(value) {
            val changed = field == value
            field = value

            if (changed) {
                notifyDataSetChanged()
            }
        }

    private var lastPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // Select layout
        val layout = if (isLandscape || isStreamerView) R.layout.horizontal_chat_message else R.layout.vertical_chat_message

        // Inflate view
        return MessageHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    override fun getItemViewType(position: Int): Int {
        return CHAT_MESSAGE
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList[position]

        when (holder.itemViewType) {
            CHAT_MESSAGE -> (holder as MessageHolder).bind(message)
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
        private const val CHAT_MESSAGE = 1
        private const val SYSTEM_MESSAGE = 2
    }
}