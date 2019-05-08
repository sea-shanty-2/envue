package dk.cs.aau.envue.nearby

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import com.squareup.picasso.Picasso
import dk.cs.aau.envue.R

class NearbyBroadcastHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var thumbnail: ImageView? = itemView.findViewById(R.id.broadcast_thumbnail)
    private var outerLayout: RelativeLayout? = itemView.findViewById(R.id.broadcast_outer_layout)

    internal fun bind(broadcastId: String, recommended: Boolean, selected: Boolean) {
        outerLayout?.apply {
            background = if (selected) R.drawable.border else null
        }

        thumbnail?.let {
            Picasso
                .get()
                .load("https://envue.me/relay/$broadcastId/thumbnail")
                .placeholder(R.drawable.ic_live_tv_48dp)
                .error(R.drawable.ic_live_tv_48dp)
                .into(it)
        }
    }
}