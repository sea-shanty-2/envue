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
import android.widget.ListView
import android.widget.TextView
import com.google.gson.GsonBuilder
import dk.cs.aau.envue.utility.EmojiIcon
import dk.cs.aau.envue.workers.BroadcastCategoryListAdapter
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

        var emojiRows = ArrayList<ArrayList<EmojiIcon>>()
        for (i in 0 until allEmojis.size) {
            if (i % 5 == 0) {
                emojiRows.add(ArrayList())
            }
            emojiRows.last().add(allEmojis[i])
        }

        val adapter = BroadcastCategoryListAdapter(this, emojiRows)

        val listView = findViewById<ListView>(R.id.broadcastCategoryListView).apply {
            this.adapter = adapter
        }
    }
}
