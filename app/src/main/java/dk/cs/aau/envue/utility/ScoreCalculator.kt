package dk.cs.aau.envue.utility

import dk.cs.aau.envue.BroadcastStopMutation
import dk.cs.aau.envue.LeaderboardQuery

fun calculateScoreFromViewTime(joinedTimeStamp: MutableList<Pair<String, Int>>, leftTimeStamp: MutableList<Pair<String, Int>>): Int {
    var score = 0
    joinedTimeStamp.forEach {
        val pair = leftTimeStamp.find { x -> x.first == it.first}
        if (pair != null) {
            val difference = pair.second - it.second
            if (difference >= 30) score += difference  // Only add viewing for minimum of 30 seconds

            leftTimeStamp.remove(pair)
        }

    }

    return score
}