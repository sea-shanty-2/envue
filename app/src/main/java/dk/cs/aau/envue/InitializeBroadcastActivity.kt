package dk.cs.aau.envue

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.view.View
import kotlinx.android.synthetic.main.activity_category_selection.*
import android.util.Log
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dk.cs.aau.envue.shared.GatewayClient
import dk.cs.aau.envue.type.BroadcastInputType
import dk.cs.aau.envue.type.LocationInputType


class InitializeBroadcastActivity : CategorySelectionActivity() {
    private val tag = "InitBroadcastActivity"
    private var id: String? = null
    private var rtmp: String? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // modify layout
        this.categorySelectionHeader.text = getString(R.string.initialize_broadcast_header)
        this.categorySelectionButtonText.text = getString(R.string.start_broadcast_button_text)

        // Create a broadcaster and start the stream when "GO" is pressed
        categorySelectionButton.setOnClickListener { view ->
            // Check if we have the required permission
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                // When we receive the last known position, create the broadcast and start the stream
                fusedLocationClient.lastLocation.apply {
                    addOnSuccessListener { location : Location? ->
                        if (location != null) {
                            createBroadcaster(location.latitude, location.longitude, category=getCategoryVector(getSelectedCategories()), view = view)
                            startBroadcast(view)
                        } else {
                            // We were not able to get the location
                            Snackbar.make(view, R.string.did_not_receive_location, Snackbar.LENGTH_LONG)
                        }
                    }

                    addOnFailureListener {
                        Snackbar.make(view, R.string.did_not_receive_location, Snackbar.LENGTH_LONG)
                    }

                    addOnCanceledListener {
                        Snackbar.make(view, R.string.did_not_receive_location, Snackbar.LENGTH_LONG)
                    }
                }
            } else {
                // The location permission has not been granted by the user
                Snackbar.make(view, R.string.location_permission_not_granted, Snackbar.LENGTH_LONG)
            }

        }

        // So we can access the geo location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    /** Creates a broadcaster object and stores it in stable storage
     * on the Envue database. */
    private fun createBroadcaster(latitude: Double, longitude: Double, category: Array<Double>, view: View) {
        val location = LocationInputType.builder().latitude(latitude).longitude(longitude).build()
        val broadcast = BroadcastInputType.builder().categories(category.toList()).location(location).build()

        val broadcastCreateMutation = BroadcastCreateMutation.builder().broadcast(broadcast).build()

        GatewayClient.mutate(broadcastCreateMutation).enqueue(object: ApolloCall.Callback<BroadcastCreateMutation.Data>() {
            override fun onResponse(response: Response<BroadcastCreateMutation.Data>) {
                val create = response.data()?.broadcasts()?.create()
                if (create == null) {
                    Snackbar.make(view, "Could not create broadcast.", Snackbar.LENGTH_LONG)
                }
                else {
                    id = create.id()
                    rtmp = create.rtmp()

                    Log.d(tag, "ID: $id, RTMP:  $rtmp")
                }
            }

            override fun onFailure(e: ApolloException) {
                Log.d(tag, e.message)
            }
        })
    }

    inner class StartBroadcastTask(c: Context): AsyncTask<Void, Void, Boolean>() {
        val progress: ProgressDialog = ProgressDialog(c)
        val context: Context = c

        init {
            progress.apply {
                setMessage("Loading...")
                setTitle("Creating broadcast")
                setCancelable(true)
            }
        }

        override fun doInBackground(vararg params: Void): Boolean {
            val startTime = System.currentTimeMillis()

            while (id == null || rtmp == null) {
                if (System.currentTimeMillis() - startTime > 10000) {
                    return false
                }
                Thread.sleep(500)
            }

            return true
        }

        override fun onPreExecute() {
            super.onPreExecute()
            progress.show()
        }

        override fun onPostExecute(result: Boolean) {
            super.onPostExecute(result)
            progress.dismiss()
            if (!result) {
                this@InitializeBroadcastActivity.finish()
                return
            }

            val intent = Intent(context, BroadcastActivity::class.java)

            // Variables to pass to next activity.
            intent.apply {
                putExtra("ID", id)
                putExtra("RTMP", rtmp)
            }

            startActivity(intent)
        }
    }

    /** Starts the broadcast after processing selected categories
     * if all checks pass. */
    private fun startBroadcast(view: View) {
        val selectedEmojiIcons = getSelectedCategories()  // TODO: Do something with this
        if (selectedEmojiIcons.isEmpty()) {
            Snackbar.make(
                view, resources.getString(R.string.no_categories_chosen), Snackbar.LENGTH_LONG).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
            StartBroadcastTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        } else {
            StartBroadcastTask(this).execute()
        }
    }
}
