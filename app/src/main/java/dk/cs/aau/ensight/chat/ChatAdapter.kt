package dk.cs.aau.ensight.chat

import android.app.Activity
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import dk.cs.aau.ensight.R

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

        if (message.belongsToCurrentUser) { // this message was sent by us so let's create a basic chat bubble on the right
            v = messageInflater.inflate(R.layout.own_message, null)
            holder.messageBody = v?.findViewById<TextView>(R.id.message_body)
            v?.tag = holder
            holder.messageBody!!.text = message.text
        } else { // this message was sent by someone else so let's create an advanced chat bubble on the left
            v = messageInflater.inflate(R.layout.other_message, null)
            holder.avatar = v?.findViewById(R.id.avatar) as View
            holder.name = v.findViewById(R.id.name) as TextView
            holder.messageBody = v.findViewById(R.id.message_body) as TextView
            v.tag = holder

            holder.name!!.text = "John"
            holder.messageBody!!.text = message.text
            val drawable = holder.avatar!!.background as GradientDrawable
        }

        return v
    }

}

internal class MessageViewHolder {
    var avatar: View? = null
    var name: TextView? = null
    var messageBody: TextView? = null
}