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
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import com.google.gson.Gson
import dk.cs.aau.envue.chat.Message
import dk.cs.aau.envue.chat.MessageListener
import dk.cs.aau.envue.chat.packets.MessagePacket
import dk.cs.aau.envue.emojiAnimations.EmojiListener
import okhttp3.WebSocket
import android.system.Os.shutdown
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator.build
import okhttp3.OkHttpClient
import okhttp3.Request


val random = Random()

class EmojiFragment : Fragment() {
    private var messages: ArrayList<Message> = ArrayList()
    private var animation = DynamicAnimation()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_emoji_reaction,container,false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val currentActivity = requireActivity()
        val button = view?.findViewById(R.id.startAnimation) as Button
        button.setOnClickListener {
            localEmoji(currentActivity.findViewById(R.id.animation_holder), currentActivity)
            beginEmojiAnimation("\uD83D\uDE09",currentActivity)

        }
    }

    fun beginEmojiAnimation(emojiUniCode: String, fragmentActivity: FragmentActivity) {
        val emoji = textToBitmap(emojiUniCode,64, fragmentActivity)

        val container = fragmentActivity.findViewById<ViewGroup>(R.id.animation_holder)
        animation.play(fragmentActivity, container, emoji)
    }

    ///Just for testing purposes, the actual functionality will be to use StartEmojiAnimation
    fun localEmoji(view: View,activity: FragmentActivity?) {
        val emojiUniCode = "\uD83D\uDE09"
        //startEmojiAnimation(textToBitmap("\uD83D\uDE09", 64, view.context),activity!!)
    }

}