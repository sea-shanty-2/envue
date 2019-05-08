package dk.cs.aau.envue.nearby

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import dk.cs.aau.envue.R

class NearbyBroadcastsAdapter(var broadcastList: List<String> = ArrayList(),
                              var selectedBroadcast: String? = null,
                              var recommendedBroadcastId: String? = null,
                              private val onClick: (String) -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val broadcastId = broadcastList[position]
        return (holder as NearbyBroadcastHolder).bind(broadcastId, recommendedBroadcastId?.equals(broadcastId) ?: false,
            selectedBroadcast?.equals(broadcastId) ?: false)
    }

    override fun getItemCount(): Int {
        return broadcastList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return NearbyBroadcastHolder(LayoutInflater.from(parent.context).inflate(R.layout.fragment_nearby_broadcast, parent,false))
    }
}