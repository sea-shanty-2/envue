package dk.cs.aau.envue

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import dk.cs.aau.envue.shared.GatewayClient
import kotlinx.android.synthetic.main.activity_choose_broadcast.*

class ChooseBroadcastActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_broadcast)
        loadBroadcasts()
    }

    private fun loadBroadcasts() {
        val broadcastsQuery = BroadcastsQuery.builder().build()
        GatewayClient.query(broadcastsQuery).enqueue(object: ApolloCall.Callback<BroadcastsQuery.Data>() {
            override fun onResponse(response: Response<BroadcastsQuery.Data>) {
                val data = response.data()
                if (data == null) {

                }

                Log.d("LOAD_SUCCESS", data.toString())
            }

            override fun onFailure(e: ApolloException) {
                Log.d("LOAD_FAILURE", e.toString())
            }
        })
    }
}
