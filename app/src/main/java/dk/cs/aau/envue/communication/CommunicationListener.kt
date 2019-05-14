package dk.cs.aau.envue.communication

interface CommunicationListener {
    fun onMessage(message: Message)
    fun onCommunicationIdentified(sequenceId: Int, name: String)
    fun onCommunicationClosed(code: Int)
    fun onReaction(reaction: String)
    fun onChatStateChanged(enabled: Boolean)
}
