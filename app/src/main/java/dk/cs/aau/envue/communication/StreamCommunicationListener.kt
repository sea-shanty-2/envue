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
    override fun onOpen(webSocket: WebSocket, response: Response) {
        communicationListener.onConnected()

        val profileQuery = ProfileQuery.builder().build()
        var displayName = ""
         GatewayClient.query(profileQuery).enqueue(object : ApolloCall.Callback<ProfileQuery.Data>() {
            override fun onResponse(response2: com.apollographql.apollo.api.Response<ProfileQuery.Data>) {
                val profile = response2.data()?.accounts()?.me()

                if (profile != null) {
                    Log.e("DisplayName","$displayName + Profile not null")
                    displayName = profile.displayName()

                    webSocket.send(Gson().toJson(
                        HandshakePacket(
                            displayName,
                            Profile.getCurrentProfile().getProfilePictureUri(256, 256).toString(),
                            channelId
                        )
                    ))

                }
                else {
                    webSocket.send(Gson().toJson(
                        HandshakePacket(
                            "Anon",//Profile.getCurrentProfile().name,
                            Profile.getCurrentProfile().getProfilePictureUri(256, 256).toString(),
                            channelId
                        )
                    ))
                }
            }

            override fun onFailure(e: ApolloException){}

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
                .url("wss://envue.me:4040")
                .build()

            return client.newWebSocket(request, StreamCommunicationListener(communicationListener, channelId))
        }
    }

    private fun output(txt: String) {
        Log.i("CHAT", txt)
    }
}