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
                emojiRows.add(inflater.inflate(R.layout.broadcast_category_row, parent, false).apply{
                    emojiIconRow.forEach {
                        this.findViewById<LinearLayout>(R.id.linearLayout)
                            .addView(makeEmojiButton(it.char))  // Make a view for each of the provided unicodes (each 'it')
                    }
                })
            }
        }

        return emojiRows[position]
    }

    private fun makeEmojiButton(unicode: String): EmojiTextView {
        // Layout parameters for parent element (LinearLayout)
        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT)
        buttonParams.weight = 1f

        // Create the TextView
        val emoji = CircularTextView(context).apply {
            layoutParams = buttonParams
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            textSize = 36f
            alpha = 1f
            text = unicode
            setTextColor(Color.BLACK)  // Makes bitmap (the emoji) non-transparent
            setSelectionMarkerColor(R.color.white)
        }

        // Enlarge\shrink when pressed
        emoji.setOnClickListener {

            val startSize = if (emoji.isSelected) 42f else 36f
            val endSize = if (emoji.isSelected) 36f else 42f

            // Start the font-size animation
            ValueAnimator.ofFloat(startSize, endSize).apply {
                addUpdateListener { valueAnimator ->
                    emoji.textSize = valueAnimator.animatedValue as Float }
                duration = 300
                start()
            }

            emoji.apply {
                isSelected = !isSelected
            }
        }

        return emoji
    }
}