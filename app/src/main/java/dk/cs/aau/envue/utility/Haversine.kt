package dk.cs.aau.envue.utility

import dk.cs.aau.envue.type.LocationInputType

/**
 * Haversine formula. Giving great-circle distances between two points on a sphere from their longitudes and latitudes.
 * It is a special case of a more general formula in spherical trigonometry, the law of haversines, relating the
 * sides and angles of spherical "triangles". Inspired from jferrao, https://gist.github.com/jferrao/cb44d09da234698a7feee68ca895f491
 *
 * https://rosettacode.org/wiki/Haversine_formula#Java
 *
 * @return Distance in meters
 */

fun haversine(first: LocationInputType, second: LocationInputType): Double {
    val earthRadiusm = 6371000
    val dLat = Math.toRadians(first.latitude() - second.latitude())
    val dLon = Math.toRadians(first.longitude() - second.longitude())
    val originLat = Math.toRadians(second.latitude())
    val destinationLat = Math.toRadians(first.latitude())

    val a = Math.pow(Math.sin(dLat / 2), 2.toDouble()) + Math.pow(Math.sin(dLon / 2), 2.toDouble()) * Math.cos(originLat) * Math.cos(destinationLat)
    val c = 2 * Math.asin(Math.sqrt(a))
    return earthRadiusm * c
}