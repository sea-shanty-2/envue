package dk.cs.aau.envue

import android.os.Bundle
import android.graphics.*
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import dk.cs.aau.envue.emojiAnimations.DynamicAnimation
import dk.cs.aau.envue.utility.textToBitmap
import java.util.*
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.view.LayoutInflater
import com.google.gson.Gson
import dk.cs.aau.envue.chat.Message
import dk.cs.aau.envue.chat.MessageListener
import dk.cs.aau.envue.chat.packets.MessagePacket
import dk.cs.aau.envue.emojiAnimations.EmojiListener
import okhttp3.WebSocket


val random = Random()

class EmojiFragment : Fragment(),MessageListener {
    private var messages: ArrayList<Message> = ArrayList()


    override fun onMessage(message: Message) {
            this.messages.add(message)
    }

    private var animation = DynamicAnimation()
    private var socket: WebSocket? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_emoji_reaction,container,false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val currentActivity = requireActivity()

        val button = view?.findViewById(R.id.startAnimation) as Button
        button.setOnClickListener {
            socket = EmojiListener.buildSocket(this)
            localEmoji(currentActivity.findViewById(R.id.animation_holder), currentActivity)

        }
    }

    fun startEmojiAnimation(emojiBitMap: Bitmap, fragmentActivity: FragmentActivity) {
        val container = fragmentActivity.findViewById<ViewGroup>(R.id.animation_holder)
        animation.play(fragmentActivity, container, emojiBitMap)
    }

    ///Just for testing purposes, the actual functionality will be to use StartEmojiAnimation
    fun localEmoji(view: View,activity: FragmentActivity?) {
        val emojiUniCode = "\uD83D\uDE09"
        if (!emojiUniCode.isEmpty()) {
            socket?.send(Gson().toJson(MessagePacket(emojiUniCode)))
            onMessage(Message(emojiUniCode))
        }
        startEmojiAnimation(textToBitmap("\uD83D\uDE09", 64, view.context),activity!!)
    }



    override fun onDestroy() {
        super.onDestroy()
        this.socket?.close(EmojiListener.NORMAL_CLOSURE_STATUS, "Activity stopped")
    }
}