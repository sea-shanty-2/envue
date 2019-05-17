package dk.cs.aau.envue

import android.app.Activity
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.google.gson.GsonBuilder
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.*
import com.mapbox.mapboxsdk.style.expressions.Expression.*
import com.mapbox.mapboxsdk.style.layers.HeatmapLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import dk.cs.aau.envue.shared.GatewayClient
import dk.cs.aau.envue.utility.EmojiIcon
import dk.cs.aau.envue.utility.Event
import dk.cs.aau.envue.utility.textToBitmap
import kotlinx.android.synthetic.main.activity_broadcast.view.*
import kotlinx.android.synthetic.main.activity_map.*


class MapFragment : Fragment(), OnMapReadyCallback, MapboxMap.OnMarkerClickListener, Style.OnStyleLoaded{
    companion object {
        internal const val SET_FILTERS_REQUEST = 57
    }

    private val STREAM_SOURCE_ID = "stream"
    private val HEATMAP_LAYER_ID = "stream-heat"
    private val HEATMAP_LAYER_SOURCE = "streams"
    private val TAG = "MapFragment"
    private var geoJsonSource: GeoJsonSource = GeoJsonSource(STREAM_SOURCE_ID) //, URL("https://www.mapbox.com/mapbox-gl-js/assets/earthquakes.geojson")) // Used as mock data. Remember to outcomment loadBroadcastsToMap in updateStreamSource when using.
    private var limitedEmojis = ArrayList<String>()
    private var filters: DoubleArray? = null
    private var eventClickedAt: Long = 0
    private var map: MapboxMap? = null
    private lateinit var updater: AsyncTask<Style, Unit, Unit>
    private lateinit var mapStyle: Style

    private inner class StreamUpdateTask : AsyncTask<Style, Unit, Unit>() {
        override fun doInBackground(vararg params: Style) {
            while (!isCancelled) {
                activity?.runOnUiThread { updateStreamSource(params[0]) }
                Thread.sleep(   10000)
            }
        }
    }

    override fun onStyleLoaded(style: Style) {
        mapStyle = style

        map?.style?.apply {
            addSource(geoJsonSource)
        }

        addHeatmapLayer(mapStyle)

        // Launch background task for updating the event markers
        updater = StreamUpdateTask().apply {
            executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mapStyle)
        }
    }

    override fun onResume() {
        if (::updater.isInitialized) {
            if (updater.isCancelled) {
                updater = StreamUpdateTask().apply {
                    executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mapStyle)
                }
            }
        }

        super.onResume()
    }

    override fun onPause() {
        if (::updater.isInitialized) {
            updater.cancel(true)
        }
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        map_view?.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        // Load all emojis into local storage
        loadEmojis()

        // Set mapbox instance, with access token
        Mapbox.getInstance(context!!, "pk.eyJ1IjoidGo0NTc5NCIsImEiOiJjanRrMXpjeWcwejhyNDNscTR5NzYydXk0In0.LWi-WdfCtpvgEiOkHC7MMw")

        // Create fragment view
        val fragment = inflater.inflate(R.layout.activity_map, container, false)

        // Create mapview with options
        val mapView = fragment.findViewById<MapView>(R.id.map_view)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // Bind update button
        fragment.findViewById<FloatingActionButton>(R.id.update_map_button)?.setOnClickListener {
            this.updateMap()
        }

        // Bind filter button
        fragment.findViewById<FloatingActionButton>(R.id.filter_categories_button)?.setOnClickListener {
            val intent = Intent(this.activity, FilterActivity::class.java)
            startActivityForResult(intent, MapFragment.SET_FILTERS_REQUEST)
        }

        return fragment
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            MapFragment.SET_FILTERS_REQUEST ->
                if (resultCode == Activity.RESULT_OK) {
                    var categories = data?.getDoubleArrayExtra(resources.getString(R.string.filter_response_key))
                    if (categories != null && !categories.contains(1.0)) categories = null
                    this.updateFilters(categories)
                }
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        map = mapboxMap

        // Disable rotation
        map?.uiSettings?.isRotateGesturesEnabled = false

        // Set style of map. Use style loader in this context.
        map?.setStyle(Style.MAPBOX_STREETS, this)
        map?.setOnMarkerClickListener(this)
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        if (System.currentTimeMillis() - eventClickedAt < 1000) {
            return false
        }

        val id = marker.title

        // Get the ids of all broadcasts in the same event and start a PlayerActivity with this information.
        getEventIds(id) { broadcastId, eventIds ->
            val intent = Intent(activity, PlayerActivity::class.java).apply {
                putExtra("broadcastId", broadcastId)
                putExtra("eventIds", eventIds)
            }

            startActivity(intent)
        }

        eventClickedAt = System.currentTimeMillis()

        return false
    }

    fun addMarker(position: LatLng, text: String, size: Int, broadcastId: String) {
        val bitmap = textToBitmap(text, size, context!!)
        val descriptor = IconFactory.getInstance(context!!).fromBitmap(bitmap)
        map?.addMarker(MarkerOptions().position(position).icon(descriptor).setTitle(broadcastId))  // Title = broadcastId
        return
    }

    fun setHeatmap(list : List<ActiveBroadcastLocationQuery.Item>?){
        val streamCoordinates = ArrayList<Point>()

        list?.forEach {
            val location = it.location()
            if (location != null) streamCoordinates.add(Point.fromLngLat(location.longitude(), location.latitude()))
        }

        // Do not update, probably fetch error.
        if (streamCoordinates.isEmpty()) return

        val lineString = LineString.fromLngLats(streamCoordinates)

        val features = lineString.coordinates().map {
            val feature = Feature.fromGeometry(it)
            feature.addNumberProperty("mag", 1)
            feature
        }
        
        val featureCollection = FeatureCollection.fromFeatures(features)

        geoJsonSource.setGeoJson(featureCollection)
    }

    private fun updateStreamSource(loadedMapStyle: Style?) {
        // Create queries for
        // - Events (so we can show their emojis)
        // - Broadcasts (so we can show the heat map)
        val activeEventsQuery = EventsQuery.builder().build()
        val activeBroadcastsQuery = ActiveBroadcastLocationQuery.builder().build()

        loadEventsToMap(activeEventsQuery)
        loadBroadcastsToMap(activeBroadcastsQuery)
    }

    // Loads all active broadcasts to the map in the form of heat map points
    private fun loadBroadcastsToMap(activeBroadcastsQuery: ActiveBroadcastLocationQuery) {
        GatewayClient.query(activeBroadcastsQuery).enqueue(object: ApolloCall.Callback<ActiveBroadcastLocationQuery.Data>() {
            override fun onResponse(response: Response<ActiveBroadcastLocationQuery.Data>) {

                activity?.runOnUiThread {
                    setHeatmap(response.data()?.broadcasts()?.active()?.items())
                }
            }

            override fun onFailure(e: ApolloException) {
                Log.d("BROADCASTS", "Something went wrong: ${e.message}")
            }
        })
    }

    // Loads all active events to the map in the form of most frequent emoji in each event
    private fun loadEventsToMap(activeEventsQuery: EventsQuery) {
        GatewayClient.query(activeEventsQuery).enqueue(object: ApolloCall.Callback<EventsQuery.Data>() {
            override fun onResponse(response: Response<EventsQuery.Data>) {
                Log.d("EVENTS", "Received event data.")
                val qEvents = response.data()?.events()?.all()

                // Convert into event objects
                val events = ArrayList<Event>()
                for (qEvent in qEvents!!) {
                    // For all broadcasts, find the emojis used for categorization.
                    // Then, determine the most frequently used one. Use this as the emoji
                    // for the event.
                    val emojiIndexCounts = Array(limitedEmojis.size) {i -> 0}
                    for (broadcast in qEvent.broadcasts()!!) {
                        val emojis = broadcast.categories()
                        for (i in 0 until emojis.size) {
                            val indicator = emojis[i]
                            emojiIndexCounts[i] += if (indicator > 0) 1 else 0
                        }
                    }

                    val mostFrequentIndex = emojiIndexCounts.indexOf(emojiIndexCounts.max())
                    val mostFrequentEmoji = limitedEmojis[mostFrequentIndex]

                    if (filters != null && filters!![mostFrequentIndex] != 1.0) {
                        continue
                    } else {
                        val event = Event(qEvent.broadcasts()?.toTypedArray(), mostFrequentEmoji)
                        events.add(event)
                        Log.d("EVENTS", "Added event with $mostFrequentEmoji as the emoji and ${event.center} as the center.")
                    }
                }

                activity?.runOnUiThread {
                    for (event in events) {
                        Log.d(TAG, "Adding marker ${event.emojiCode}")
                        addMarker(LatLng(event.center.lat, event.center.lon),
                            event.emojiCode,
                            35,
                            event.broadcasts?.first()?.id()!!)
                    }
                }

            }

            override fun onFailure(e: ApolloException) {
                Log.d(TAG, "Something went wrong: ${e.message}")
            }
        })
    }

    private fun addHeatmapLayer(loadedMapStyle: Style) {
        val layer = HeatmapLayer(HEATMAP_LAYER_ID, STREAM_SOURCE_ID)
        layer.maxZoom = 20f
        layer.minZoom = 0f
        layer.sourceLayer = HEATMAP_LAYER_SOURCE
        layer.setProperties(

            // Color ramp for heatmap.  Domain is 0 (low) to 1 (high).
            // Begin color ramp at 0-stop with a 0-transparency color
            // to create a blur-like effect.
            heatmapColor(
                interpolate(
                    linear(), heatmapDensity(),
                    literal(0), rgba(0, 0, 0, 0),
                    literal(0.1), rgb(50, 241, 231),
                    literal(0.5), rgb(0, 255, 0),
                    literal(0.9), rgb(255, 255, 0),
                    literal(0.99), rgb(255, 165,0),
                    literal(1), rgb(255, 0, 0)
                )
            ),

            // Increase the heatmap weight based on frequency and property magnitude
            heatmapWeight(
                interpolate(
                    linear(), get("mag"),
                    stop(0, 0.7),
                    stop(6, 1)
                )
            ),

            // Increase the heatmap color weight weight by zoom level
            // heatmap-intensity is a multiplier on top of heatmap-weight
            heatmapIntensity(
                interpolate(
                    linear(), zoom(),
                    stop(0, 0.01),
                    stop(20, 1)
                )
            ),

            // Adjust the heatmap radius by zoom level
            heatmapRadius(
                interpolate(
                    linear(), zoom(),
                    stop(0, 40),
                    stop(20, 40)
                )
            ),

            // Transition from heatmap to circle layer by zoom level
            heatmapOpacity(
                interpolate(
                    linear(), zoom(),
                    stop(0, 0.5),
                    stop(20, 0.2)
                )
            )
        )

        loadedMapStyle.addLayerAbove(layer, "waterway-label")
    }

    private fun loadEmojis() {
        // Load all emojis into local storage
        limitedEmojis.addAll(GsonBuilder().create().fromJson(
                resources.openRawResource(R.raw.limited_emojis).bufferedReader(),
                Array<EmojiIcon>::class.java).map { e -> e.char })
    }

    fun updateMap() {
        activity?.runOnUiThread {
            map?.markers?.forEach { map?.removeMarker(it) }
            updateStreamSource(null)
        }
    }

    private fun getEventIds(id: String, callback: (id:String, ids:ArrayList<String>) -> Unit) {
        val eventQuery = EventWithIdQuery.builder().id(id).build()
        GatewayClient.query(eventQuery).enqueue(object: ApolloCall.Callback<EventWithIdQuery.Data>() {
            override fun onResponse(response: Response<EventWithIdQuery.Data>) {
                val ids = response.data()?.events()?.containing()?.broadcasts()?.map { it.id() }
                var recommended : String? = response.data()?.events()?.containing()?.recommended()?.id()

                if (recommended == null) recommended = id

                if (ids != null) {
                    callback(recommended, ids as ArrayList<String>)
                }
            }

            override fun onFailure(e: ApolloException) {
                Log.d("GETEVENTS", "Something went wrong while getting events for broadcast id $id")
            }
        })
    }

    fun updateFilters(filterArray: DoubleArray?){
        filters = filterArray
        var emojiString = ""
        if (filterArray != null){
            for (i in 0 until filterArray.size){
                if (filterArray[i] == 1.0){
                    emojiString += limitedEmojis[i]
                }
            }
        }
        updateMap()
    }
}