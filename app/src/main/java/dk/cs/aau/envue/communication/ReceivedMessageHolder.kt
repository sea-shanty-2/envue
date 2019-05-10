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
    private var avatar: ImageView = itemView.findViewById(R.id.avatarPicture)

    internal fun bind(message: Message) {
        messageText.text = message.text
        nameText.text = message.author

        // Set avatar
        avatar?.let {
            Picasso
                .get()
                .load(Profile.getCurrentProfile().getProfilePictureUri(256, 256))
                .placeholder(R.drawable.ic_profile_picture_placeholder)
                .error(R.drawable.ic_profile_picture_placeholder)
                .resize(256, 256)
                .transform(CircleTransform())
                .into(it)
        }
    }
}