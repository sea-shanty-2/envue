package dk.cs.aau.envue.communication

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import dk.cs.aau.envue.R
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter



class MessageHolder internal constructor(itemView: View): RecyclerView.ViewHolder(itemView) {
    private var messageText: TextView? = itemView.findViewById(R.id.message_body)
    private var nameText: TextView? = itemView.findViewById(R.id.name)
    private var sequenceText: TextView? = itemView.findViewById(R.id.sequence_id)

    internal fun bind(message: Message) {
        messageText?.text = message.text
        nameText?.text = message.author

        sequenceText?.apply {
            val drawable = itemView.resources.getDrawable(R.drawable.rounded_circle)
            val selectedColor = if (message is SystemMessage) itemView.resources.getColor(R.color.black) else {
                val colors = itemView.resources.getIntArray(R.array.sequence_colors)
                colors[message.sequenceId % colors.size]
            }

            text = if (message is SystemMessage) "E" else message.sequenceId.toString()
            drawable.colorFilter = PorterDuffColorFilter(selectedColor, PorterDuff.Mode.SRC_IN)
            background = drawable
        }
    }
}