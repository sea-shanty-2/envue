package dk.cs.aau.envue.utility

import com.google.gson.GsonBuilder
import dk.cs.aau.envue.EventsQuery

class Event(val broadcasts: Array<EventsQuery.Broadcast>?, val emojiCode: String) {


    class Location(var lat: Double, var lon: Double) {
        override fun toString(): String {
            return "($lat, $lon)"
        }
    }

    val size get() = broadcasts?.size


    val center: Location get() {
        if (broadcasts == null) {
            throw IllegalArgumentException("Cannot calculate the center for an event with NULL broadcasts.")
        }
        if (broadcasts.isEmpty()) {
            throw IllegalArgumentException("Cannot get the center of an event with zero broadcasts.")
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
        loc.lat /= broadcasts.size
        loc.lon /= broadcasts.size
        return loc
    }
}