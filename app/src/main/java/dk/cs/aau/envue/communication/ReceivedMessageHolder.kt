package dk.cs.aau.envue.communication

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.facebook.Profile
import com.squareup.picasso.Picasso
import dk.cs.aau.envue.R
import dk.cs.aau.envue.transformers.CircleTransform

class ReceivedMessageHolder internal constructor(itemView: View): RecyclerView.ViewHolder(itemView) {
    private var messageText: TextView = itemView.findViewById(R.id.message_body)
    private var nameText: TextView = itemView.findViewById(R.id.name)

    internal fun bind(message: Message) {
        messageText.text = message.text
        nameText.text = message.author
    }
}