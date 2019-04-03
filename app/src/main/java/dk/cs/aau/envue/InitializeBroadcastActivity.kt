package dk.cs.aau.envue

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.design.widget.Snackbar
import android.support.text.emoji.EmojiCompat
import android.support.text.emoji.EmojiSpan
import android.support.text.emoji.bundled.BundledEmojiCompatConfig
import android.support.text.emoji.widget.EmojiButton
import android.support.text.emoji.widget.EmojiTextView
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import dk.cs.aau.envue.utility.textToBitmap

import kotlinx.android.synthetic.main.activity_initialize_broadcast.*
import android.animation.ValueAnimator
import android.util.JsonReader
import com.google.gson.GsonBuilder
import dk.cs.aau.envue.utility.EmojiIcon
import java.io.FileReader
import java.io.Reader


class InitializeBroadcastActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initialize_broadcast)
        EmojiCompat.init(BundledEmojiCompatConfig(this))

        startBroadcastButton.setOnClickListener { view ->
            Snackbar.make(view, "nibber", 2)
                .setAction("Action", null).show()
        }

        // Load all emojis from local file
        val allEmojis = GsonBuilder().create().fromJson(
            resources.openRawResource(R.raw.emojis).bufferedReader(),
            Array<EmojiIcon>::class.java
        )

        // Create initial LinearLayout
        val layout: LinearLayout = findViewById(R.id.linearLayout)

        // Load five emojis into the horizontal stack
        for (i in 1..5) {
            val emoji = allEmojis[i]
            layout.addView(makeEmojiButton(emoji.char))
        }

    }


    private fun makeEmojiButton(unicode: String): EmojiTextView {
        // Layout parameters for parent element (LinearLayout)
        var buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT)
        buttonParams.weight = 1f

        // Create the TextView
        var emoji = EmojiTextView(this)
        emoji.layoutParams = buttonParams;

        // Set styling
        emoji.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        emoji.textSize = 36f
        emoji.alpha = 1f
        emoji.setTextColor(Color.BLACK)
        emoji.text = unicode

        // Enlarge/shrink when pressed
        emoji.setOnClickListener {

            val startSize = if (emoji.isSelected) 42f else 36f
            val endSize = if (emoji.isSelected) 36f else 42f

            val animationDuration: Long = 300 // Animation duration in ms

            val animator = ValueAnimator.ofFloat(startSize, endSize)
            animator.duration = animationDuration

            animator.addUpdateListener { valueAnimator ->
                emoji.textSize = valueAnimator.animatedValue as Float
            }

            animator.start()
            emoji.isSelected = !emoji.isSelected
        }

        return emoji
    }
}
