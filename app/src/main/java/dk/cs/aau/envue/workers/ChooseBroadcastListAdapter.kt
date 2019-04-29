package dk.cs.aau.envue.workers

import android.support.design.widget.Snackbar
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import dk.cs.aau.envue.BroadcastsQuery
import dk.cs.aau.envue.R

class ChooseBroadcastListAdapter(private val items: Array<BroadcastsQuery.Item>) :
    RecyclerView.Adapter<ChooseBroadcastListAdapter.ChooseBroadcastViewHolder>() {

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    class ChooseBroadcastViewHolder(val activeBroadcastView: TextView) : RecyclerView.ViewHolder(activeBroadcastView)

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ChooseBroadcastListAdapter.ChooseBroadcastViewHolder {
        // create a new view
        val textView = LayoutInflater.from(parent.context)
            .inflate(R.layout.active_broadcast_view, parent, false) as TextView
        // set the view's size, margins, paddings and layout parameters
        // ...
        return ChooseBroadcastViewHolder(textView)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ChooseBroadcastViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.activeBroadcastView.apply {
            text = items[position].toString()
            setOnClickListener {view ->
                Log.d("Click", "You clicked broadcast ${items[position].id()}!")
            }
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = items.size
}