package dk.cs.aau.envue

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dk.cs.aau.envue.emojianimations.DynamicAnimation
import dk.cs.aau.envue.utility.textToBitmap
import java.util.*


val random = Random()

class EmojiFragment : Fragment() {
    private var animation = DynamicAnimation()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_emoji_reaction,container,false)
    }

    fun begin(emojiUniCode: String, Activity: Activity) {
        val emoji = textToBitmap(emojiUniCode,64, Activity)
        val container = Activity.findViewById<ViewGroup>(R.id.animation_holder)
        animation.play(Activity, container, emoji)
    }
}