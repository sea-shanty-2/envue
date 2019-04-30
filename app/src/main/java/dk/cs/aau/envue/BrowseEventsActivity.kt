package dk.cs.aau.envue

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.widget.Button
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import dk.cs.aau.envue.shared.GatewayClient
import dk.cs.aau.envue.utility.Event
import dk.cs.aau.envue.workers.BrowseEventsListAdapter

class BrowseEventsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_broadcast)
        loadEvents()

        findViewById<Button>(R.id.watch_broadcast_btn).setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java))
        }
    }

    private fun loadEvents() {

        val eventsQuery = EventsQuery.builder().build()
        GatewayClient.query(eventsQuery).enqueue(object: ApolloCall.Callback<EventsQuery.Data>() {

            override fun onResponse(response: Response<EventsQuery.Data>) {
                val data = response.data()
                if (data == null) {
                    Log.e("LoadEvents", "Could not load events.")
                }

                val events = data?.events()?.all()
                if (events != null) {
                    initializeRecyclerView(events)
                }
            }

            override fun onFailure(e: ApolloException) {
                Log.e("LoadEvents", e.message)
            }
        })
    }



    /**
     * Populates the RecyclerView with the broadcasts. **/
    private fun initializeRecyclerView(items: Iterable<EventsQuery.All>) {

        runOnUiThread {
            val events = items.map { e -> Event(e.broadcasts()?.toTypedArray()) }
            viewManager = LinearLayoutManager(this)
            viewAdapter = BrowseEventsListAdapter(events.toTypedArray())
            recyclerView = findViewById<RecyclerView>(R.id.choose_broadcast_list_view).apply {
                setHasFixedSize(true)
                layoutManager = viewManager
                adapter = viewAdapter
            }
        }
    }
}
