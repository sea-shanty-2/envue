package dk.cs.aau.envue.communication

data class SystemMessage(val message: String) : Message(message, "Envue", -1)