package dk.cs.aau.envue.emojianimations

import android.util.Log
import dk.cs.aau.envue.chat.MessageListener
import okhttp3.*
import okio.ByteString
import okhttp3.WebSocket


class EmojiListener(messageListener: MessageListener): WebSocketListener() {
    private val NORMAL_CLOSURE_STATUS = 1000

    override fun onOpen(webSocket: WebSocket, response: Response) {
        output("CONNECTED")
        //webSocket.close(NORMAL_CLOSURE_STATUS, "Goodbye !")
    }

    override fun onMessage(webSocket: WebSocket?, text: String?) {
        output("Receiving : " + text!!)
    }

    override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) {
        output("Receiving bytes : " + bytes!!.hex())
    }

    override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
        webSocket!!.close(NORMAL_CLOSURE_STATUS, null)
        output("Closing : $code / $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        output("Failure" + t.message)
    }

    private fun output(txt: String) {
        Log.i("EMOJI", txt)
    }

}
