package dk.cs.aau.envue.workers

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import dk.cs.aau.envue.R
import dk.cs.aau.envue.utility.Event

class BrowseEventsListAdapter(private val items: Array<Event>) :
    RecyclerView.Adapter<BrowseEventsListAdapter.ChooseEventViewHolder>() {

    class ChooseEventViewHolder(val activeEventView: Button) : RecyclerView.ViewHolder(activeEventView)

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): BrowseEventsListAdapter.ChooseEventViewHolder {

        val eventButton = LayoutInflater.from(parent.context)
            .inflate(R.layout.active_broadcast_view, parent, false) as Button


        return ChooseEventViewHolder(eventButton)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ChooseEventViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.activeEventView.apply {
            val event = items[position]

            setOnClickListener {
                Log.d("Click", "You clicked an with ${event.size} broadcasts!")
            }

            text = "Event at ${event.center} (contains ${event.size} broadcasts)"

        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = items.size
}