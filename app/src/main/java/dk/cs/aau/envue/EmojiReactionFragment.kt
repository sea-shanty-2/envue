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


val random = Random()

class EmojiReactionFragment : Fragment() {
    private var animation = DynamicAnimation()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_emoji_reaction,container,false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val testus = requireActivity()
        val button = view?.findViewById(R.id.TestButton) as Button
        button.setOnClickListener { emoji1(testus.findViewById(R.id.animation_holder),testus) }
    }

    fun startEmojiAnimation(emojiBitMap: Bitmap, fragmentActivity: FragmentActivity) {
        val container = fragmentActivity.findViewById<ViewGroup>(R.id.animation_holder)
        animation.play(fragmentActivity, container, emojiBitMap)
    }

    ///Just for testing purposes, the actual functionality will be to used StartEmojiAnimation
    fun emoji1(view: View?,activity: FragmentActivity?) {
        startEmojiAnimation(textToBitmap("\uD83D\uDE09", 64, view!!.context),activity!!)
    }
}