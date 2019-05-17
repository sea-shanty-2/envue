package dk.cs.aau.envue.shared

import java.text.SimpleDateFormat
import java.util.*

fun FormatDate(date:String): Date {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    return dateFormat.parse(date)
}