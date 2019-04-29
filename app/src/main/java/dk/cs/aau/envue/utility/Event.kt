package dk.cs.aau.envue.utility

import dk.cs.aau.envue.BroadcastsQuery
import dk.cs.aau.envue.EventsQuery

class Event(val broadcasts: Array<EventsQuery.Broadcast>?) {

    class Location(var lat: Double, var lon: Double)


    val size get() = broadcasts?.size


    val center: Location get() {
        if (broadcasts == null) {
            throw IllegalArgumentException("Cannot calculate the center for an event with NULL broadcasts.")
        }
        val loc = Location(0.0, 0.0)
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