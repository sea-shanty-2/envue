package dk.cs.aau.envue.utility
import java.util.*

fun calculateScoreFromViewTime(joinedTimeStamp: MutableList<Pair<String, Int>>, leftTimeStamp: MutableList<Pair<String, Int>>, date: Date): Int {
    var score = 0
    val lastActivity = date.time / 1000

    joinedTimeStamp.forEach {
        val pair = leftTimeStamp.find { x -> x.first == it.first}
        val time = pair?.second?.toLong() ?: lastActivity
        val difference = (time - it.second.toLong())

        if (difference >= 30) score += difference.toInt()  // Only add viewing for minimum of 30 seconds

        leftTimeStamp.remove(pair)
    }

    return score
}