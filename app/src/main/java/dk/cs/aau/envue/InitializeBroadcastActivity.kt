package dk.cs.aau.envue

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.text.emoji.EmojiCompat
import android.support.text.emoji.bundled.BundledEmojiCompatConfig
import android.support.text.emoji.widget.EmojiTextView
import android.support.v7.app.AppCompatActivity
import android.widget.LinearLayout
import android.widget.TextView
import com.google.gson.GsonBuilder
import dk.cs.aau.envue.utility.EmojiIcon
import kotlinx.android.synthetic.main.activity_initialize_broadcast.*


class InitializeBroadcastActivity : AppCompatActivity() {

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
        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT)
        buttonParams.weight = 1f

        // Create the TextView
        val emoji = EmojiTextView(this).apply {
            layoutParams = buttonParams
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            textSize = 36f
            alpha = 1f
            text = unicode
            setTextColor(Color.BLACK)
        }

        // Enlarge\shrink when pressed
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
