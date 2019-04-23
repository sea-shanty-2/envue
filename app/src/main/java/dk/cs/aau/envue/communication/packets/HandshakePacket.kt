package dk.cs.aau.envue.communication.packets

class HandshakePacket(val name: String, val avatar: String, val channel: String) {
    val type = "identity"
}