package dk.cs.aau.ensight.chat

import android.util.Log
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

private class ChatListener : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        output("Connected")
    }

    override fun onMessage(webSocket: WebSocket?, text: String?) {
        output("Receiving $text")
    }

    override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) {
    }

    override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
        output("Closing")
    }

    companion object {
        private val NORMAL_CLOSURE_STATUS = 1000

        fun buildSocket(): WebSocket {
            val client = OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .url("wss://envue.me/")
                .build()

            return client.newWebSocket(request, ChatListener())
        }
    }

    private fun output(txt: String) {
        Log.i("CHAT", txt)
    }
}