package dk.cs.aau.envue

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.google.android.gms.location.LocationServices
import dk.cs.aau.envue.shared.GatewayClient
import dk.cs.aau.envue.type.BroadcastInputType
import dk.cs.aau.envue.type.LocationInputType
import kotlinx.android.synthetic.main.activity_category_selection.*


class InitializeBroadcastActivity : CategorySelectionActivity() {
    private val tag = "InitBroadcastActivity"
    private var id: String? = null
    private var rtmp: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Bind broadcast create event to the button click listener
        categorySelectionButton.setOnClickListener { view ->
            // Inform the user of missing permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(view.context, R.string.location_permission_not_granted, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            showProgressBar()
            // Fetch last known location
            LocationServices.getFusedLocationProviderClient(this).lastLocation.apply {
                addOnSuccessListener { location: Location? ->
                    // Inform the user of missing location data
                    when {
                        location == null -> {
                            Toast.makeText(view.context, R.string.did_not_receive_location, Toast.LENGTH_LONG).show()
                            showContainer()
                        }
                        getSelectedCategories().isEmpty() -> {
                            Toast.makeText(view.context, R.string.no_categories_chosen, Toast.LENGTH_LONG).show()
                            showContainer()
                        }
                        else -> createBroadcast(location, getCategoryVector(getSelectedCategories()))
                    }
                }
            }
        }

        findViewById<TextView>(R.id.categorySelectionHeader).text = resources.getString(R.string.initialize_broadcast_header)
    }

    private fun showContainer() {
        pBar.visibility = View.GONE
        container.visibility = View.VISIBLE
    }

    private fun showProgressBar() {
        container.visibility = View.GONE
        pBar.visibility = View.VISIBLE
    }

    private fun createBroadcast(location: Location, categories: Array<Double>) {

        // Construct input parameters for the graph mutation
        val location = LocationInputType.builder().latitude(location.latitude).longitude(location.longitude).build()
        val broadcast = BroadcastInputType.builder().categories(categories.toList()).location(location).build()

        // Construct the graph mutation with the input parameters
        val broadcastCreateMutation = BroadcastCreateMutation.builder().broadcast(broadcast).build()

        // Enqueue the mutation result
        GatewayClient
            .mutate(broadcastCreateMutation)
            .enqueue(object: ApolloCall.Callback<BroadcastCreateMutation.Data>() {

                override fun onResponse(response: Response<BroadcastCreateMutation.Data>) {
                    // Bind the response data
                    val data = response.data()?.broadcasts()?.create()

                    // Check if response data is null
                    if (data == null) {
                        Toast.makeText(this@InitializeBroadcastActivity.currentFocus?.context, "Could not create broadcast.", Toast.LENGTH_LONG).show()
                        showContainer()
                        return
                    }
                    else {
                        val intent = Intent(this@InitializeBroadcastActivity, BroadcastActivity::class.java)

                        // Variables to pass to next activity.
                        intent.apply {
                            putExtra("ID", data.id())
                            putExtra("RTMP", data.rtmp())
                        }

                        startActivity(intent)
                        finish()
                    }
                }

                override fun onFailure(e: ApolloException) {
                    Toast.makeText(this@InitializeBroadcastActivity.currentFocus?.context, "Could not create broadcast.", Toast.LENGTH_LONG).show()
                    Log.d(tag, e.message)
                    finish()
                }
            })

    }



}
