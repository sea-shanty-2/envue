package dk.cs.aau.envue

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.*
import com.mapbox.mapboxsdk.style.layers.HeatmapLayer
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.expressions.Expression.*
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import dk.cs.aau.envue.shared.GatewayClient
import dk.cs.aau.envue.utility.textToBitmap
import android.os.AsyncTask
import android.support.v4.app.Fragment
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View


class MapActivity : Fragment(), OnMapReadyCallback, MapboxMap.OnMarkerClickListener, Style.OnStyleLoaded{
    // private val EARTHQUAKE_SOURCE_URL = "https://www.mapbox.com/mapbox-gl-js/assets/earthquakes.geojson"
    private val STREAM_SOURCE_ID = "stream"
    private val HEATMAP_LAYER_ID = "stream-heat"
    private val HEATMAP_LAYER_SOURCE = "streams"
    private val CIRCLE_LAYER_ID = "earthquakes-circle"
    private val TAG = "MapActivity"
    private var geoJsonSource: GeoJsonSource = GeoJsonSource(STREAM_SOURCE_ID)

    private inner class StreamUpdateTask : AsyncTask<Style, Void, Void>() {
        override fun doInBackground(vararg params: Style): Void {
            while (true) {
                activity?.runOnUiThread {
                        updateStreamSource(params[0])
                }
                Thread.sleep(   300000)
            }
        }
    }

    override fun onStyleLoaded(style: Style) {
        mMap?.style?.addSource(geoJsonSource)
        addHeatmapLayer(style)

        StreamUpdateTask().execute(style)
    }

    private var mMap: MapboxMap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Set mapbox instance, with access token

        Mapbox.getInstance(context!!, "pk.eyJ1IjoidGo0NTc5NCIsImEiOiJjanRrMXpjeWcwejhyNDNscTR5NzYydXk0In0.LWi-WdfCtpvgEiOkHC7MMw")

        // Mapbox view options
        var options = MapboxMapOptions().apply {
            rotateGesturesEnabled(false)
        }

        // Create mapview with options
        var mapView = MapView(context!!, options)
        mapView.id = R.id.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this) // Fragment mapper

        return mapView
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        mMap = mapboxMap

        // Set style of map. Use style loader in this context.
        mMap?.setStyle(Style.MAPBOX_STREETS, this)

        // TODO: Load markers from database.
        addMarker(LatLng(50.0, 50.0), "\uD83D\uDE1A", 50)
        mMap?.setOnMarkerClickListener(this)
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun addMarker(position: LatLng, text: String, size: Int) {
        var bitmap = textToBitmap(text, size, context!!)
        var descriptor = IconFactory.getInstance(context!!).fromBitmap(bitmap)
        mMap?.addMarker(MarkerOptions().position(position).icon(descriptor))
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
        val featureCollection = FeatureCollection.fromFeatures(arrayOf(Feature.fromGeometry(lineString)))

        geoJsonSource.setGeoJson(featureCollection)
    }

    private fun updateStreamSource(loadedMapStyle: Style) {

       val activeQuery: ActiveBroadcastLocationQuery = ActiveBroadcastLocationQuery.builder().build()

        val testCall = GatewayClient.query(activeQuery)

        testCall.enqueue(object : ApolloCall.Callback<ActiveBroadcastLocationQuery.Data>() {
            override fun onResponse(response: Response<ActiveBroadcastLocationQuery.Data>){
                Log.d(TAG, "Did shit")

                var d = response.data()?.broadcasts()?.active()?.items()

                activity?.runOnUiThread(object : Runnable {
                    override fun run() {
                        setHeatmap(d)
                    }
                })

            }

            override fun onFailure(e: ApolloException){
                Log.d(TAG, e.message)
            }
        })

    }

    private fun addHeatmapLayer(loadedMapStyle: Style) {
        val layer = HeatmapLayer(HEATMAP_LAYER_ID, STREAM_SOURCE_ID)
        layer.maxZoom = 19f
        layer.setSourceLayer(HEATMAP_LAYER_SOURCE)
        layer.setProperties(

            // Color ramp for heatmap.  Domain is 0 (low) to 1 (high).
            // Begin color ramp at 0-stop with a 0-transparency color
            // to create a blur-like effect.
            heatmapColor(
                interpolate(
                    linear(), heatmapDensity(),
                    literal(0), rgba(33, 102, 172, 0),
                    literal(0.2), rgb(103, 169, 207),
                    literal(0.4), rgb(209, 229, 240),
                    literal(0.6), rgb(253, 219, 199),
                    literal(0.8), rgb(239, 138, 98),
                    literal(1), rgb(178, 24, 43)
                )
            ),

            // Increase the heatmap weight based on frequency and property magnitude
            heatmapWeight(
                interpolate(
                    linear(), get("mag"),
                    stop(0, 0),
                    stop(6, 1)
                )
            ),

            // Increase the heatmap color weight weight by zoom level
            // heatmap-intensity is a multiplier on top of heatmap-weight
            heatmapIntensity(
                interpolate(
                    linear(), zoom(),
                    stop(0, 1),
                    stop(19, 3)
                )
            ),

            // Adjust the heatmap radius by zoom level
            heatmapRadius(
                interpolate(
                    linear(), zoom(),
                    stop(0, 2),
                    stop(19, 20)
                )
            ),

            // Transition from heatmap to circle layer by zoom level
            heatmapOpacity(
                interpolate(
                    linear(), zoom(),
                    stop(13, 1),
                    stop(19, 0)
                )
            )
        )

        loadedMapStyle.addLayerAbove(layer, "waterway-label")
    }

    private fun addCircleLayer(loadedMapStyle: Style) {
        val circleLayer = CircleLayer(CIRCLE_LAYER_ID, STREAM_SOURCE_ID)
        circleLayer.setProperties(

            // Size circle radius by earthquake magnitude and zoom level
            circleRadius(
                interpolate(
                    linear(), zoom(),
                    literal(7), interpolate(
                        linear(), get("mag"),
                        stop(1, 1),
                        stop(6, 4)
                    ),
                    literal(16), interpolate(
                        linear(), get("mag"),
                        stop(1, 5),
                        stop(6, 50)
                    )
                )
            ),

            // Color circle by earthquake magnitude
            circleColor(
                interpolate(
                    linear(), get("mag"),
                    literal(1), rgba(33, 102, 172, 0),
                    literal(2), rgb(103, 169, 207),
                    literal(3), rgb(209, 229, 240),
                    literal(4), rgb(253, 219, 199),
                    literal(5), rgb(239, 138, 98),
                    literal(6), rgb(178, 24, 43)
                )
            ),

            // Transition from heatmap to circle layer by zoom level
            circleOpacity(
                interpolate(
                    linear(), zoom(),
                    stop(7, 0),
                    stop(8, 1)
                )
            ),
            circleStrokeColor("white"),
            circleStrokeWidth(1.0f)
        )

        loadedMapStyle.addLayerBelow(circleLayer, HEATMAP_LAYER_ID)
    }


}