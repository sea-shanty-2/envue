package dk.cs.aau.envue.communication

interface CommunicationListener {
    fun onMessage(message: Message)
    fun onConnected()
    fun onClosed(code: Int)
    fun onReaction(reaction: String)
}
