package dk.cs.aau.envue.chat

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import dk.cs.aau.envue.R

class SentMessageHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var messageText: TextView = itemView.findViewById(R.id.message_body)

    internal fun bind(message: Message) {
        messageText.text = message.text
    }
}