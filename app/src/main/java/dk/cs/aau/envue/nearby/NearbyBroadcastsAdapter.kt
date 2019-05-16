package dk.cs.aau.envue.nearby

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import dk.cs.aau.envue.EventBroadcastsWithStatsQuery
import dk.cs.aau.envue.R

class NearbyBroadcastsAdapter(var broadcastList: List<EventBroadcastsWithStatsQuery.Broadcast> = ArrayList(),
                              var currentBroadcastId: String? = null, var recommendedBroadcastId: String? = null,
                              private val onClick: (String) -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val broadcast = broadcastList[position]
        holder.itemView.setOnClickListener { onClick(broadcast.id()) }

        return (holder as NearbyBroadcastHolder).bind(broadcast, recommendedBroadcastId?.equals(broadcast.id()) ?: false,
            currentBroadcastId?.equals(broadcast.id()) ?: false)
    }

    override fun getItemCount(): Int {
        return broadcastList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return NearbyBroadcastHolder(LayoutInflater.from(parent.context).inflate(R.layout.fragment_nearby_broadcast, parent,false))
    }

    fun getSelectedPosition(): Int {
        broadcastList.forEachIndexed { index, broadcast ->
            if (broadcast.id() == currentBroadcastId) return index
        }

        for (i in 0..broadcastList.size) {
            if (broadcastList[i].id() == currentBroadcastId) {
                return i
            }
        }

        return 0
    }
}