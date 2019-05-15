package dk.cs.aau.envue.communication

import android.util.Log
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.exception.ApolloException
import com.facebook.Profile
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dk.cs.aau.envue.ProfileQuery
import dk.cs.aau.envue.communication.packets.HandshakePacket
import dk.cs.aau.envue.shared.GatewayClient
import okhttp3.*
import okio.ByteString

class StreamCommunicationListener(private val communicationListener: CommunicationListener,
                                  private val channelId: String) : WebSocketListener() {
    private fun identifyWithName(webSocket: WebSocket, name: String, uniqueId: String) {
        webSocket.send(Gson().toJson(
            HandshakePacket(
                name,
                channelId,
                uniqueId
            )
        ))
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        val profileQuery = ProfileQuery.builder().build()
        GatewayClient.query(profileQuery).enqueue(object : ApolloCall.Callback<ProfileQuery.Data>() {
            override fun onResponse(response2: com.apollographql.apollo.api.Response<ProfileQuery.Data>) {
                val profile = response2.data()?.accounts()?.me()

                profile?.let {
                    identifyWithName(webSocket, it.displayName(), it.id())
                }
            }

            override fun onFailure(e: ApolloException) {}
        })
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
                        jsonObj.get("SequenceId").asInt
                    )
                )
                "Reaction" -> this.communicationListener.onReaction(jsonObj.get("Reaction").asString)
                "Identity" -> this.communicationListener.onCommunicationIdentified(jsonObj.get("SequenceId").asInt,
                    jsonObj.get("Name").asString)
                "ChatState" -> this.communicationListener.onChatStateChanged(jsonObj.get("Enabled").asBoolean)
            }
        }
    }

    override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) {
        output("Received message")
    }

    override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
        output("Closed")

        communicationListener.onCommunicationClosed(code)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        output("Failure")

        t.printStackTrace()

        communicationListener.onCommunicationClosed(-1)
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
        Log.i("COMMUNICATION", txt)
    }
}