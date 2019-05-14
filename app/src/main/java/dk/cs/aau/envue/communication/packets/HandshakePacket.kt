package dk.cs.aau.envue.communication.packets

class HandshakePacket(val Name: String, val Channel: String, val UniqueId: String) {
    val Type = "Identity"
}