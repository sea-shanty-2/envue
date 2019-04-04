package dk.cs.aau.envue

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import com.facebook.Profile
import com.github.faucamp.simplertmp.RtmpHandler
import dk.cs.aau.envue.chat.ChatListener
import dk.cs.aau.envue.chat.Message
import dk.cs.aau.envue.chat.MessageListAdapter
import dk.cs.aau.envue.chat.MessageListener
import net.ossrs.yasea.SrsEncodeHandler
import net.ossrs.yasea.SrsPublisher
import okhttp3.WebSocket
import java.io.IOException
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.net.SocketException
import android.content.DialogInterface
import android.support.v7.app.AlertDialog





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
    private var rtmpHandler: RtmpHandler? = null
    private var encodeHandler: SrsEncodeHandler? = null

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
        rtmpHandler = RtmpHandler(this)
        encodeHandler = SrsEncodeHandler(this)
        this.publisher?.apply {
            setEncodeHandler(encodeHandler)
            setRtmpHandler(rtmpHandler)
            setRecordHandler(null)
            setPreviewResolution(640, 360)
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
    }

    override fun onDestroy() {
        super.onDestroy()

        this.publisher?.apply {
            stopPublish()
            stopRecord()
        }

        this.socket?.close(ChatListener.NORMAL_CLOSURE_STATUS, "Activity stopped")
    }

    override fun onPause() {
        super.onPause()
        this.publisher?.pauseRecord()
    }

    override fun onResume() {
        super.onResume()
        this.publisher?.resumeRecord()
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("Confirmation")
            .setMessage("Are you sure you want to stop the stream?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.yes
            ) { _, _ ->
                finish()
                
                super.onBackPressed()
            }
            .setNegativeButton(android.R.string.no, null).show()
    }
}
