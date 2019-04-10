package dk.cs.aau.envue.chat

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import dk.cs.aau.envue.R
import dk.cs.aau.envue.utility.textToBitmap

class EmojiImageHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var reaction: ImageView = itemView.findViewById(R.id.reaction_image)

    internal fun bind(reaction: String) {
        this.reaction.setImageBitmap(textToBitmap(reaction, 64, itemView.context))
    }
}