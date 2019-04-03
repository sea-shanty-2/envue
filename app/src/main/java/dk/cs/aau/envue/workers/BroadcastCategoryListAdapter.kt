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
import dk.cs.aau.envue.R
import dk.cs.aau.envue.utility.EmojiIcon

public class BroadcastCategoryListAdapter(private val context: Context,
                                          private val dataSource: ArrayList<ArrayList<EmojiIcon>>) : BaseAdapter() {

    private val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

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
        val rowView: View = inflater.inflate(R.layout.broadcast_category_row, parent, false)

        for (emojiIcon in dataSource[position]) {
            rowView.findViewById<LinearLayout>(R.id.linearLayout).addView(makeEmojiButton(emojiIcon.char))
        }

        return rowView
    }

    private fun makeEmojiButton(unicode: String): EmojiTextView {
        // Layout parameters for parent element (LinearLayout)
        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT)
        buttonParams.weight = 1f

        // Create the TextView
        val emoji = EmojiTextView(context).apply {
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