package dk.cs.aau.ensight

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import com.facebook.Profile
import com.github.faucamp.simplertmp.RtmpHandler
import dk.cs.aau.ensight.chat.ChatListener
import dk.cs.aau.ensight.chat.Message
import dk.cs.aau.ensight.chat.MessageListAdapter
import dk.cs.aau.ensight.chat.MessageListener
import net.ossrs.yasea.SrsEncodeHandler
import net.ossrs.yasea.SrsPublisher
import okhttp3.WebSocket
import java.io.IOException
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.net.SocketException



/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class BroadcastActivity : AppCompatActivity(), RtmpHandler.RtmpListener, SrsEncodeHandler.SrsEncodeListener,
    MessageListener {
    private var publisher: SrsPublisher? = null
    private val tag = "ENVUE-BROADCAST"
    private var chatList: RecyclerView? = null
    private var chatAdapter: MessageListAdapter? = null
    private var socket: WebSocket? = null
    private var messages: ArrayList<Message> = ArrayList()

    override fun onMessage(message: Message) {
        runOnUiThread {
            messages.add(message)
            this.chatAdapter?.notifyDataSetChanged()
            scrollToBottom()
        }
    }

    private fun scrollToBottom() {
        this.chatAdapter?.itemCount?.let { this.chatList?.smoothScrollToPosition(it )}
    }

    override fun onRtmpConnecting(msg: String?) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onRtmpConnected(msg: String?) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onRtmpVideoStreaming() {
    }

    override fun onRtmpAudioStreaming() {
    }

    override fun onRtmpStopped() {
        Toast.makeText(applicationContext, "RTMP stopped", Toast.LENGTH_SHORT).show()
    }

    override fun onRtmpDisconnected() {
        Toast.makeText(applicationContext, "RTMP disconnected", Toast.LENGTH_SHORT).show()
    }

    override fun onRtmpVideoFpsChanged(fps: Double) {
        Log.i(tag, "FPS: $fps")
    }

    override fun onRtmpVideoBitrateChanged(bitrate: Double) {
        Toast.makeText(applicationContext, "Bitrate: $bitrate", Toast.LENGTH_SHORT).show()
    }

    override fun onRtmpAudioBitrateChanged(bitrate: Double) {
    }

    override fun onRtmpSocketException(e: SocketException?) {
        Toast.makeText(applicationContext, "Socket exception", Toast.LENGTH_SHORT).show()
    }

    override fun onRtmpIOException(e: IOException?) {
        Toast.makeText(applicationContext, "IO exception", Toast.LENGTH_SHORT).show()
    }

    override fun onRtmpIllegalArgumentException(e: IllegalArgumentException?) {
        Toast.makeText(applicationContext, "Illegal argument exception (RTMP)", Toast.LENGTH_SHORT).show()

    }

    override fun onRtmpIllegalStateException(e: IllegalStateException?) {
        Toast.makeText(applicationContext, "Illegal state exception", Toast.LENGTH_SHORT).show()
    }

    override fun onNetworkWeak() {
        Toast.makeText(applicationContext, "Network weak", Toast.LENGTH_SHORT).show()
    }

    override fun onNetworkResume() {
        Toast.makeText(applicationContext, "Network resumed", Toast.LENGTH_SHORT).show()
    }

    override fun onEncodeIllegalArgumentException(e: IllegalArgumentException?) {
        Toast.makeText(applicationContext, "Illegal argument exception (ENC)", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_broadcast)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // Get profile
        val profile = Profile.getCurrentProfile()

        // Initialize publisher
        this.publisher = SrsPublisher(this.findViewById(R.id.camera_view))
        val rtmpHandler = RtmpHandler(this)
        val srsEncodeHandler = SrsEncodeHandler(this)
        this.publisher?.apply {
            setEncodeHandler(srsEncodeHandler)
            setRtmpHandler(rtmpHandler)
            setRecordHandler(null)
            setPreviewResolution(1280, 720)
            setOutputResolution(1280, 720)
            setVideoHDMode()
            setScreenOrientation(Configuration.ORIENTATION_LANDSCAPE)
            startPublish("rtmp://envue.me:1935/stream/${profile.firstName}${profile.lastName}")
            startCamera()
        }

        // Create chat adapter
        chatAdapter = MessageListAdapter(this, messages, streamerView = true)

        // Initialize chat listener
        socket = ChatListener.buildSocket(this)

        chatList = findViewById(R.id.chat_view)

        // Assign chat adapter and layout manager
        val chatLayoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        chatList?.apply {
            adapter = chatAdapter
            layoutManager = chatLayoutManager
        }

        messages.add(Message("this is a test to see how well the chat works", "test"))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // TODO: Implement orientation change
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        this.publisher?.stopPublish()
        this.socket?.close(ChatListener.NORMAL_CLOSURE_STATUS, "Activity stopped")
    }
}
