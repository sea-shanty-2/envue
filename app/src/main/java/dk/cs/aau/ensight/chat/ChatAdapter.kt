package dk.cs.aau.ensight.chat

import android.app.Activity
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import dk.cs.aau.ensight.R
import dk.cs.aau.ensight.Transformers.CircleTransform
import kotlinx.android.synthetic.main.activity_profile.*

class ChatAdapter(internal var context: Context) : BaseAdapter() {
    override fun getItem(position: Int): Any {
        return messages[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    internal var messages: ArrayList<Message> = ArrayList()

    fun add(message: Message) {
        this.messages.add(message)
        super.notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return messages.count()
    }


    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
        val v: View?
        val holder = MessageViewHolder()
        val messageInflater = context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val message = messages[i]

        if (message.author == null) {
            v = messageInflater.inflate(R.layout.own_message, null)
            holder.messageBody = v.findViewById(R.id.message_body)
            v.tag = holder
            holder.messageBody?.text = message.text
        } else {
            v = messageInflater.inflate(R.layout.other_message, null)
            holder.avatar = v.findViewById(R.id.avatarPicture)
            holder.name = v.findViewById(R.id.name)
            holder.messageBody = v.findViewById(R.id.message_body)
            v.tag = holder

            // Set avatar
            message.avatar?.let {
                Picasso
                    .get()
                    .load(message.avatar)
                    .placeholder(R.drawable.ic_profile_picture_placeholder)
                    .error(R.drawable.ic_profile_picture_placeholder)
                    .resize(256, 256)
                    .transform(CircleTransform())
                    .into(holder.avatar)
            }

            holder.name?.text = message.author
            holder.messageBody?.text = message.text
        }

        return v
    }

}

internal class MessageViewHolder {
    var avatar: ImageView? = null
    var name: TextView? = null
    var messageBody: TextView? = null
}