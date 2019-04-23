package dk.cs.aau.envue.communication

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import dk.cs.aau.envue.R
import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import kotlinx.android.synthetic.main.fragment_reaction.view.*


class ReactionListAdapter(private val onClick: (String) -> Unit, private val emojiList: List<String>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            onClick(emojiList[position])

            animateRotation((holder as EmojiImageHolder).itemView.reaction_image)
        }

        return (holder as EmojiImageHolder).bind(emojiList[position])
    }

    override fun getItemCount(): Int {
        return emojiList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return EmojiImageHolder(LayoutInflater.from(parent.context).inflate(R.layout.fragment_reaction, parent,false))
    }

    private fun animateRotation(image: ImageView) {
        val rotationAnimator = ValueAnimator.ofFloat(0f, 360f)

        rotationAnimator.duration = 500
        rotationAnimator.interpolator = AccelerateDecelerateInterpolator()

        rotationAnimator.addUpdateListener { valueAnimator ->
            val animatedValue = valueAnimator.animatedValue as Float
            image.rotation = animatedValue
        }

        rotationAnimator.start()
    }
}