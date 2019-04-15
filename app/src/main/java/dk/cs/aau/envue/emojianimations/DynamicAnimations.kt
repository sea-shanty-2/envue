package dk.cs.aau.envue.emojianimations

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import dk.cs.aau.envue.random
import android.os.Handler


class DynamicAnimation {

    fun play(activity: Activity, parent: ViewGroup, emoji: Bitmap) {
        val startingPoints = getRandomWidth(parent, emoji.width)
        val parentHeight = parent.height / 6
        val randomYStartCoordinate = (random.nextInt(parentHeight - emoji.height) + emoji.height).toFloat()

        // Says in which direction the animation should go. fromY coordinate toY coordinate
        val animation = TranslateAnimation(0f, 0f, -randomYStartCoordinate, -parent.height.toFloat())
        animation.duration = (random.nextInt(3000 - 1500) + 1500).toLong()

        val drawEmoji = CreateEmoji().apply {
            attachTo(parent)
            with(activity)
            create(startingPoints, emoji)
        }

        val animationGroup = animationFadeOut(animation, drawEmoji)
        drawEmoji.applyAnimation(animationGroup)
    }

    /**
     * @param animation will be grouped to a fadeOut animation that happens 500ms before the animation ends
     * @return AnimationSet
     */
    private fun animationFadeOut(animation: TranslateAnimation, emoji: CreateEmoji): AnimationSet {
        val out = AlphaAnimation(1.0f, 0.0f)
        out.duration = animation.duration - 500

        val animationGroup = AnimationSet(true).apply {
            addAnimation(animation)
            addAnimation(out)
            fillAfter = true
        }

        animationGroup.setAnimationListener(object: Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                val testTimer = Handler()
                testTimer.postDelayed({
                    emoji.destroy()
                }, 100)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })

        return animationGroup
    }

    private fun getRandomWidth(parent: ViewGroup, emojiWidth: Int): IntArray {
        val width = parent.width - emojiWidth
        val moveEmojiToRight = parent.width - (parent.width / 6) - emojiWidth
        val height = parent.height

        val x = random.nextInt(width - moveEmojiToRight) + moveEmojiToRight

        return intArrayOf(x, height)
    }
}
