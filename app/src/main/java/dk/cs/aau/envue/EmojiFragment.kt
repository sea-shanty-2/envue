package dk.cs.aau.envue

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import dk.cs.aau.envue.emojianimations.DynamicAnimation
import dk.cs.aau.envue.utility.textToBitmap
import java.util.*
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.view.LayoutInflater


val random = Random()

class EmojiFragment : Fragment() {
    private var animation = DynamicAnimation()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_emoji_reaction,container,false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //val currentActivity = requireActivity()
        //val button = view?.findViewById(R.id.startAnimation) as Button
        //button.setOnClickListener {
        //    begin("\uD83D\uDE09",currentActivity)
//
        //}
    }

    fun begin(emojiUniCode: String, Activity: Activity) {
        val emoji = textToBitmap(emojiUniCode,64, Activity)

        val container = Activity.findViewById<ViewGroup>(R.id.animation_holder)
        animation.play(Activity, container, emoji)
    }
}