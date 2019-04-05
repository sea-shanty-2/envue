package dk.cs.aau.envue.emojiAnimations

import android.util.Log
import com.facebook.Profile
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dk.cs.aau.envue.chat.Message
import dk.cs.aau.envue.chat.MessageListener
import dk.cs.aau.envue.chat.packets.HandshakePacket
import okhttp3.*
import okio.ByteString
import android.system.Os.shutdown
import okhttp3.WebSocket
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator.build





class EmojiListener(private val messageListener: MessageListener): WebSocketListener() {


    override fun onOpen(webSocket: WebSocket, response: Response) {
        output("Emoji Listener")
        //webSocket.send(Gson().toJson(
        //    HandshakePacket(
        //        Profile.getCurrentProfile().name,
        //        Profile.getCurrentProfile().getProfilePictureUri(256, 256).toString()
        //    )
        //))
        webSocket.send("\uD83D\uDE09")
    }


    override fun onMessage(webSocket: WebSocket?, text: String?) {
        output("Receiving $text")
        text?.let {
            val jsonObj = JsonParser().parse(it) as JsonObject

            when (jsonObj.get("type").asString) {
                "message" -> this.messageListener.onMessage(
                    Message(
                        jsonObj.get("message").asString,
                        jsonObj.get("author").asString,
                        jsonObj.get("avatar").asString
                    )
                )
            }
        }
    }

    //requester
    companion object {
        const val NORMAL_CLOSURE_STATUS = 1000
        fun buildSocket(messageListener: MessageListener): WebSocket {
            val client = OkHttpClient.Builder()
                .build()
            val request = Request.Builder()
                .url("ws://envue.me:8765")
                .build()

            return client.newWebSocket(request, EmojiListener(messageListener))
        }
    }

    private fun output(txt: String) {
        Log.i("EMOJI", txt)
    }
}