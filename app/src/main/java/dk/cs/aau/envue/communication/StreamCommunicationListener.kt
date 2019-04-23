package dk.cs.aau.envue.communication

import android.util.Log
import com.facebook.Profile
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dk.cs.aau.envue.communication.packets.HandshakePacket
import okhttp3.*
import okio.ByteString

class StreamCommunicationListener(private val messageListener: MessageListener,
                                  private val reactionListener: ReactionListener) : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        output("Connected")
        webSocket.send(Gson().toJson(
            HandshakePacket(
                Profile.getCurrentProfile().name,
                Profile.getCurrentProfile().getProfilePictureUri(256, 256).toString(),
                "test"
            )
        ))
    }

    override fun onMessage(webSocket: WebSocket?, text: String?) {
        output("Receiving $text")
        text?.let {
            val jsonObj = JsonParser().parse(it) as JsonObject

            when (jsonObj.get("Type").asString) {
                "Message" -> this.messageListener.onMessage(
                    Message(
                        jsonObj.get("Message").asString,
                        jsonObj.get("Author").asString,
                        jsonObj.get("Avatar").asString
                    )
                )
                "Reaction" -> this.reactionListener.onReaction(jsonObj.get("Reaction").asString)
            }
        }
    }

    override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) {
        output("Received message")
    }

    override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
        output("Closing")
    }

    companion object {
        const val NORMAL_CLOSURE_STATUS = 1000

        fun buildSocket(messageListener: MessageListener, reactionListener: ReactionListener): WebSocket {
            val client = OkHttpClient.Builder()
                .build()
            val request = Request.Builder()
                .url("ws://envue.me:4040")
                .build()

            return client.newWebSocket(request, StreamCommunicationListener(messageListener, reactionListener))
        }
    }

    private fun output(txt: String) {
        Log.i("CHAT", txt)
    }
}