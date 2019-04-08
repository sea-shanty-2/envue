package dk.cs.aau.envue.emojiAnimations

import android.graphics.Bitmap
import android.support.v4.app.FragmentActivity
import android.view.Gravity
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ImageView
import java.lang.ref.WeakReference

class CreateEmoji {
    private var weakActivity: WeakReference<FragmentActivity>? = null
    private var weakRootView: WeakReference<ViewGroup>? = null
    private var createdOttLayer: FrameLayout? = null


    fun with(weakReferenceActivity: FragmentActivity): CreateEmoji {
        weakActivity = WeakReference(weakReferenceActivity)
        return this
    }

    fun attachTo(rootView: ViewGroup) {
        weakRootView = WeakReference(rootView)
    }

    fun create(drawLocation: IntArray, bitmap: Bitmap) {
        val activity = weakActivity?.get()
        val attachingView: ViewGroup? = weakRootView?.get()

        val imageView = ImageView(activity)
        imageView.setImageBitmap(bitmap)

        val emoji = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP

        )
        emoji.apply {
            width = bitmap.width
            height = bitmap.height
            leftMargin = drawLocation[0]
            topMargin = drawLocation[1]
        }


        imageView.layoutParams = emoji

        val ottLayer = FrameLayout(activity!!)
        ottLayer.addView(imageView)
        attachingView?.addView(ottLayer)
        createdOttLayer = ottLayer
    }

    fun applyAnimation(animation: Animation) {
        val drawImage = createdOttLayer?.getChildAt(0)
        drawImage?.startAnimation(animation)
    }

    fun destroy() {
        val attachingView: ViewGroup? = weakRootView?.get()
        attachingView?.removeView(createdOttLayer)
        createdOttLayer = null
    }
}