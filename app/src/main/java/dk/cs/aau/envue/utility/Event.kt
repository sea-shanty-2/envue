package dk.cs.aau.envue.utility

import dk.cs.aau.envue.BroadcastsQuery

class Event(val broadcasts: Array<BroadcastsQuery.Item>) {

    class Location(var lat: Double, var lon: Double)

    val size get() = broadcasts.size
    val center: Location get() {
        var loc = Location(0.0, 0.0)
        for (l in broadcasts.map { b -> b.location() }) {
            val lon = l?.longitude()
            val lat = l?.latitude()
            if (lon != null && lat != null) {
                loc.lat += lat
                loc.lon += lon
            }
        }
        return loc
    }

    override fun toString(): String {
        return "(${center.lat}, ${center.lon})"
    }
}