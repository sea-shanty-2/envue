package dk.cs.aau.envue.nearby

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.squareup.picasso.Picasso
import dk.cs.aau.envue.EventBroadcastsWithStatsQuery
import dk.cs.aau.envue.R

class NearbyBroadcastHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var thumbnail: ImageView? = itemView.findViewById(R.id.broadcast_thumbnail)
    private var outerLayout: RelativeLayout? = itemView.findViewById(R.id.broadcast_outer_layout)
    private var recommendedView: ImageView? = itemView.findViewById(R.id.recommended_star)
    private var viewerCount: TextView? = itemView.findViewById(R.id.viewer_count_nearby)
    private var likeRatio: TextView? = itemView.findViewById(R.id.like_ratio_nearby)

    internal fun bind(broadcast: EventBroadcastsWithStatsQuery.Broadcast, recommended: Boolean, selected: Boolean) {
        outerLayout?.setBackgroundResource(if (selected) R.drawable.selected else 0)
        recommendedView?.visibility = if (recommended) View.VISIBLE else View.GONE
        viewerCount?.text = broadcast.viewer_count().toString()

        val ratingCount = broadcast.positiveRatings() + broadcast.negativeRatings()
        if (ratingCount > 0) {
            likeRatio?.apply {
                visibility = View.VISIBLE
                text = context.getString(R.string.percentage, broadcast.positiveRatings() / ratingCount * 100.0)
            }
        }

        thumbnail?.let {
            Picasso
                .get()
                .load("https://envue.me/relay/${broadcast.id()}/thumbnail")
                .placeholder(R.drawable.ic_live_tv_48dp)
                .error(R.drawable.ic_live_tv_48dp)
                .into(it)
        }
    }
}