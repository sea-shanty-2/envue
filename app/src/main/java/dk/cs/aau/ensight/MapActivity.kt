package dk.cs.aau.ensight

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.*
import dk.cs.aau.ensight.Utils.textToBitmap

class MapActivity : AppCompatActivity(), OnMapReadyCallback, MapboxMap.OnMarkerClickListener {
    private var mMap: MapboxMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, "pk.eyJ1IjoidGo0NTc5NCIsImEiOiJjanRrMXpjeWcwejhyNDNscTR5NzYydXk0In0.LWi-WdfCtpvgEiOkHC7MMw")
        //setContentView(R.layout.activity_map)

        var options = MapboxMapOptions().apply {
            //

        }

        var mapView = MapView(this, options)
        mapView.id = R.id.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        setContentView(mapView)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        mMap = mapboxMap
        mMap?.setStyle(Style.MAPBOX_STREETS)
        addMarker(LatLng(50.0, 50.0), "\uD83D\uDE1A", 50)
        mMap?.setOnMarkerClickListener(this)
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun addMarker(position: LatLng, text: String, size: Int) {
        var bitmap = textToBitmap(text, size, this)
        var descriptor = IconFactory.getInstance(this).fromBitmap(bitmap)
        mMap?.addMarker(MarkerOptions().position(position).icon(descriptor))
        return
    }


}