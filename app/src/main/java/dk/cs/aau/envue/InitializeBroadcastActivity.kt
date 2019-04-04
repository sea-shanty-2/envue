package dk.cs.aau.envue

import android.animation.ValueAnimator
import android.content.Intent
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
            val selectedEmojis = getSelectedCategories()  // TODO: Do something with this
            startActivity(Intent(this, BroadcastActivity::class.java))
        }

        loadEmojis(R.raw.emojis)
    }

    private fun loadEmojis(resourceId: Int) {
        val allEmojis = GsonBuilder().create().fromJson(
            resources.openRawResource(resourceId).bufferedReader(),
            Array<EmojiIcon>::class.java
        )

        // Wrap all unicodes in EmojiIcon objects in rows of 5
        var emojiRows = ArrayList<ArrayList<EmojiIcon>>()
        for (i in 0 until allEmojis.size) {
            if (i % 5 == 0) {
                emojiRows.add(ArrayList())
            }
            emojiRows.last().add(allEmojis[i])
        }

        // Provide an item adapter to the ListView
        findViewById<ListView>(R.id.broadcastCategoryListView).apply {
            this.adapter = BroadcastCategoryListAdapter(this.context, emojiRows)
        }
    }

    private fun getSelectedCategories(): List<String> {
        val emojiIcons = (findViewById<ListView>(R.id.broadcastCategoryListView).adapter as BroadcastCategoryListAdapter)
            .getAllEmojis()
            .filter {it.isSelected}
            .map {it.getEmoji()}

        return emojiIcons.map {
            it.char
        }
    }
}
