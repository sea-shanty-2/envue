package dk.cs.aau.envue.shared

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

fun FormatDate(date:String): Date {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    return dateFormat.parse(date)
}