package dk.cs.aau.envue

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.graphics.*
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import dk.cs.aau.envue.emojiAnimations.DynamicAnimation
import dk.cs.aau.envue.utility.textToBitmap
import java.util.*
import android.content.Intent
import android.content.res.Configuration
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.view.LayoutInflater
import android.content.Context.LAYOUT_INFLATER_SERVICE
import com.facebook.FacebookSdk.getApplicationContext




val random = Random()

class EmojiReactionActivity : Fragment() {
    private var animation = DynamicAnimation()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_emoji_reaction,container,false)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val testus = requireActivity()
        val button = view?.findViewById(R.id.TestButton) as Button
        button.setOnClickListener { emoji1(testus.findViewById(R.id.animation_holder),testus) }
    }


    private fun moveEmoji(emojiBitMap: Bitmap,fragmentActivity: FragmentActivity) {
        val container = fragmentActivity.findViewById<ViewGroup>(R.id.animation_holder)
        animation.play(fragmentActivity, container, emojiBitMap)
    }

    fun emoji1(view: View?,activity: FragmentActivity?) {
        //val handler = Handler()
        //val r = Runnable {
        //    moveEmoji(textToBitmap("\uD83D\uDE09", 32, view!!.context),activity!!)
        //}
//
        //for (i in 0..1000) {
        //    handler.postDelayed(r, random.nextInt(30000).toLong())
        //}
        moveEmoji(textToBitmap("\uD83D\uDE09", 64, view!!.context),activity!!)
    }
}