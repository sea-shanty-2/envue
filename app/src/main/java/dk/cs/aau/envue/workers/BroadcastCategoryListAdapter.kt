package dk.cs.aau.envue.workers

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.support.text.emoji.widget.EmojiTextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import dk.cs.aau.envue.CircularTextView
import dk.cs.aau.envue.R
import dk.cs.aau.envue.utility.EmojiIcon


class BroadcastCategoryListAdapter(private val context: Context,
                                   private val dataSource: ArrayList<ArrayList<EmojiIcon>>) : BaseAdapter() {

    private val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val emojiRows = ArrayList<View>()  // Persistent storage of Emoji views (so we don't lose selection status when scrolling)

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        if (emojiRows.isEmpty()) {  // Eagerly load all emojis to (per-activity persistent) storage

            for (emojiIconRow in dataSource) {
                val constraintLayout = inflater.inflate(R.layout.broadcast_category_row, parent, false)
                emojiRows.add(
                    constraintLayout.apply {
                        // Make a view for each of the provided unicodes, add them to the linear layout
                        emojiIconRow.forEach {
                            this.findViewById<LinearLayout>(R.id.linearLayout)
                                .addView(makeEmojiButton(it))
                        }
                    }
                )
            }
        }

        return emojiRows[position]
    }

    fun getAllEmojis(): ArrayList<CircularTextView> {
        val all = ArrayList<CircularTextView>()
        for (emojiRow in emojiRows) {
            val emojiRowLayout = emojiRow.findViewById<LinearLayout>(R.id.linearLayout)
            for (i in 0 until emojiRowLayout.childCount) {
                all.add(emojiRowLayout.getChildAt(i) as CircularTextView)
            }
        }

        return all
    }

    private fun makeEmojiButton(emojiIcon: EmojiIcon): EmojiTextView {
        // Layout parameters for parent element (LinearLayout)
        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT)
        buttonParams.weight = 1f

        // Create the TextView
        val emoji = CircularTextView(context, emojiIcon).apply {
            layoutParams = buttonParams
        }

        return emoji
    }
}