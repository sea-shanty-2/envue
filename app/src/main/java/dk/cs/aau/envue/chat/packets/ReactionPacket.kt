package dk.cs.aau.envue.chat.packets

class ReactionPacket(private val reaction: String) {
    val type = "reaction"
}