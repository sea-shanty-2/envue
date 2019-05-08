package dk.cs.aau.envue.communication

import android.util.Log
import com.facebook.Profile
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dk.cs.aau.envue.communication.packets.HandshakePacket
import okhttp3.*
import okio.ByteString

class StreamCommunicationListener(private val communicationListener: CommunicationListener,
                                  private val channelId: String) : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        communicationListener.onConnected()
        webSocket.send(Gson().toJson(
            HandshakePacket(
                Profile.getCurrentProfile().name,
                Profile.getCurrentProfile().getProfilePictureUri(256, 256).toString(),
                channelId
            )
        ))
    }

    override fun onMessage(webSocket: WebSocket?, text: String?) {
        output("Receiving $text")
        text?.let {
            val jsonObj = JsonParser().parse(it) as JsonObject

            when (jsonObj.get("Type").asString) {
                "Message" -> this.communicationListener.onMessage(
                    Message(
                        jsonObj.get("Message").asString,
                        jsonObj.get("Author").asString,
                        jsonObj.get("Avatar").asString
                    )
                )
                "Reaction" -> this.communicationListener.onReaction(jsonObj.get("Reaction").asString)
            }
        }
    }

    override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) {
        output("Received message")
    }

    override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
        output("Closed")

        communicationListener.onClosed(code)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        output("Failure")

        t.printStackTrace()

        communicationListener.onClosed(-1)
    }

    companion object {
        const val NORMAL_CLOSURE_STATUS = 1000

        fun buildSocket(communicationListener: CommunicationListener, channelId: String): WebSocket {
            val client = OkHttpClient.Builder()
                .build()
            val request = Request.Builder()
                .url("wss://envue.me:443/comms")
                .build()

            return client.newWebSocket(request, StreamCommunicationListener(communicationListener, channelId))
        }
    }

    private fun output(txt: String) {
        Log.i("CHAT", txt)
    }
}