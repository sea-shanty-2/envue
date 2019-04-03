package dk.cs.aau.envue.emojiAnimations

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.app.FragmentActivity
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import dk.cs.aau.envue.R
import dk.cs.aau.envue.random


class DynamicAnimation {

    fun play(activity: FragmentActivity, parent: ViewGroup, emoji: Bitmap) {

        var emojiCopy = emoji
        val startingPoints = getRandomWidth(parent, emoji.width)
        val randomYStartCoordinate = (random.nextInt(400 - 100) + 100).toFloat()

        val animation = TranslateAnimation(0f, 0f, -randomYStartCoordinate, -parent.height.toFloat())
        animation.duration = (random.nextInt(3000 - 1500) + 1500).toLong()
        if (random.nextInt(100) == 3) {
            emojiCopy = createimages(activity, R.drawable.yes)
        }

        val drawEmojionLayout = CreateEmoji().apply {
            attachTo(parent)
            with(activity)
            create(startingPoints, emojiCopy)
        }

        val animationGroup = animationFadeOut(animation, drawEmojionLayout)

        drawEmojionLayout.applyAnimation(animationGroup)

    }

    private fun animationFadeOut(animation: TranslateAnimation, emoji: CreateEmoji): AnimationSet {
        val out = AlphaAnimation(1.0f, 0.0f)
        out.duration = animation.duration - 500

        val animationGroup = AnimationSet(true).apply {
            addAnimation(animation)
            addAnimation(out)
            fillAfter = true
        }

        animationGroup.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                emoji.destroy()
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        return animationGroup
    }

    private fun getRandomWidth(parent: ViewGroup, emojiWidth: Int): IntArray {
        val width = parent.width - emojiWidth
        val height = parent.height
        val x = random.nextInt(width - emojiWidth) + emojiWidth

        return intArrayOf(x, height)
    }

    private fun createimages(activity: Activity, imageResId: Int): Bitmap {
        return BitmapFactory.decodeResource(activity.resources, imageResId)
    }
}
