package dk.cs.aau.envue.workers

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import dk.cs.aau.envue.BroadcastsQuery
import dk.cs.aau.envue.R

class ChooseBroadcastListAdapter(private val items: Array<BroadcastsQuery.Item>) :
    RecyclerView.Adapter<ChooseBroadcastListAdapter.ChooseBroadcastViewHolder>() {

    class ChooseBroadcastViewHolder(val activeBroadcastView: Button) : RecyclerView.ViewHolder(activeBroadcastView)

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ChooseBroadcastListAdapter.ChooseBroadcastViewHolder {

        val broadcastButton = LayoutInflater.from(parent.context)
            .inflate(R.layout.active_broadcast_view, parent, false) as Button


        return ChooseBroadcastViewHolder(broadcastButton)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ChooseBroadcastViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.activeBroadcastView.apply {
            val broadcast = items[position]

            setOnClickListener {
                Log.d("Click", "You clicked broadcast ${broadcast.id()}!")
            }

            text = "${broadcast.id()}\n Lat(${broadcast.location()?.latitude()}), Lon(${broadcast.location()?.longitude()})"

        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = items.size
}