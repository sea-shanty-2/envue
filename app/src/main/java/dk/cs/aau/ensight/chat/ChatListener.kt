package dk.cs.aau.ensight.chat

import android.util.Log
import com.facebook.Profile
import okhttp3.*
import okio.ByteString

class ChatListener(private val messageListener: MessageListener) : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        output("Connected")
        webSocket.send(Profile.getCurrentProfile().name)
    }

    override fun onMessage(webSocket: WebSocket?, text: String?) {
        output("Receiving $text")
        text?.let { this.messageListener.onMessage(it) }
    }

    override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) {
        output("Received message")
    }

    override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
        output("Closing")
    }

    companion object {
        private val NORMAL_CLOSURE_STATUS = 1000

        fun buildSocket(messageListener: MessageListener): WebSocket {
            val client = OkHttpClient.Builder()
                .build()
            val request = Request.Builder()
                .url("ws://envue.me:8765")
                .build()

            return client.newWebSocket(request, ChatListener(messageListener))
        }
    }

    private fun output(txt: String) {
        Log.i("CHAT", txt)
    }
}