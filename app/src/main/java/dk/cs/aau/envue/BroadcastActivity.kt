package dk.cs.aau.envue

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap
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
import dk.cs.aau.envue.chat.*
import net.ossrs.yasea.SrsEncodeHandler
import net.ossrs.yasea.SrsPublisher
import okhttp3.WebSocket
import java.io.IOException
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.net.SocketException
import android.support.v7.app.AlertDialog
import kotlinx.android.synthetic.main.activity_broadcast.*
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.sign


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class BroadcastActivity : AppCompatActivity(), RtmpHandler.RtmpListener, SrsEncodeHandler.SrsEncodeListener,
    MessageListener, SensorEventListener, ReactionListener {
    private var publisher: SrsPublisher? = null
    private val TAG = "ENVUE-BROADCAST"
    private var chatList: RecyclerView? = null
    private var chatAdapter: MessageListAdapter? = null
    private var socket: WebSocket? = null
    private var messages: ArrayList<Message> = ArrayList()
    private var rtmpHandler: RtmpHandler? = null
    private var encodeHandler: SrsEncodeHandler? = null
    private var emojiFragment: EmojiFragment? = null
    private var sensorManager: SensorManager? = null
    private var sensor: Sensor? = null
    private var accelerationArray: MutableList<Pair<FloatArray, Long>> = mutableListOf()
    private val lock = ReentrantLock()
    private val x = 0
    private val y = 1
    private val z = 2
    private val threshold = 1f
    private val curveSmoothingConstant = 20
    private var thread: Thread? = null
    private var running = true

    fun calculateDirectionChanges(): Double {
        var arrayCopy: List<FloatArray> = listOf()

        lock.withLock {
            arrayCopy = accelerationArray.map { (values, _) -> values.clone() }
            accelerationArray.removeAll { true }
        }

        if (arrayCopy.isEmpty()) return 0.0 // No data. Assume the worst.

        // Has to assign accelerationArray copy to another variable to ensure smart cast is available.
        val sampleArray = arrayCopy.takeLast(100)

        val lastIndex = sampleArray.lastIndex
        var cd = 0

        if (sampleArray[lastIndex][x] != sampleArray[lastIndex/2][y]
            || sampleArray[lastIndex][y] != sampleArray[lastIndex/2][y]
            || sampleArray[lastIndex][z] != sampleArray[lastIndex/2][z]) {
            for (i in 0..(lastIndex - 3)) {
                val sgn1 = sgn(sampleArray[i], sampleArray[i+1])
                val sgn2 = sgn(sampleArray[i+1], sampleArray[i+2])
                if (!(sgn1 contentEquals sgn2)) {
                    cd++
                }
            }
        }

        // Divides with ten to smooth curve.
        return 1 - Math.tanh(cd.toDouble() / curveSmoothingConstant)
    }

    private fun sgn(arrayp: FloatArray, arrayq: FloatArray): FloatArray {
        val xDiff = arrayp[x] - arrayq[x]
        val yDiff = arrayp[y] - arrayq[y]
        val zDiff = arrayp[z] - arrayq[z]

        // Ensure difference is above threshold to ensure small shakes aren't registered.
        val xSign = if (Math.abs(xDiff) > threshold) sign(xDiff) else 0f
        val ySign = if (Math.abs(yDiff) > threshold) sign(yDiff) else 0f
        val zSign = if (Math.abs(zDiff) > threshold) sign(zDiff) else 0f

        return floatArrayOf(xSign, ySign, zSign)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return //TODO do something? Maybe no care
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            var values: FloatArray = event.values.clone()
            val date = event.timestamp

            // Round to third decimal place.
            values = values.map { Math.round(it * 100) / 100f }.toFloatArray()

            lock.withLock {
                accelerationArray.add(Pair(values, date))
            }
        }
    }

    override fun onMessage(message: Message) {
        runOnUiThread {
            messages.add(message)
            this.chatAdapter?.notifyDataSetChanged()
            scrollToBottom()
        }
    }

    override fun onReaction(reaction: String) {
        runOnUiThread {
            emojiFragment?.begin(reaction,this@BroadcastActivity)
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
        Log.i(TAG, "FPS: $fps")
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

        // Get supported resolutions
        val previewSize = Camera.open().parameters.previewSize
        val outputSize = previewSize  // TODO: This is not optimal, but works fine.

        // Initialize publisher
        this.publisher = SrsPublisher(this.findViewById(R.id.camera_view))
        rtmpHandler = RtmpHandler(this)
        encodeHandler = SrsEncodeHandler(this)
        this.publisher?.apply {
            setEncodeHandler(encodeHandler)
            setRtmpHandler(rtmpHandler)
            setRecordHandler(null)
            setPreviewResolution(previewSize.width, previewSize.height)
            setOutputResolution(outputSize.width, outputSize.height)
            setVideoHDMode()
            setScreenOrientation(Configuration.ORIENTATION_LANDSCAPE)
            startPublish("rtmp://envue.me:1935/stream/${profile.firstName}${profile.lastName}")
            startCamera()
        }

        // Create chat adapter
        chatAdapter = MessageListAdapter(this, messages, streamerView = true)

        // Initialize chat listener
        socket = StreamCommunicationListener.buildSocket(this, this)

        chatList = findViewById(R.id.chat_view)

        // Creates fragments for EmojiReactionsFragment
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        emojiFragment = EmojiFragment()
        emojiFragment?.let {
            fragmentTransaction.replace(R.id.fragment_container, it)
            fragmentTransaction.commit()
        }

        // Assign chat adapter and layout manager
        val chatLayoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        chatList?.apply {
            adapter = chatAdapter
            layoutManager = chatLayoutManager
        }
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor =  sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        setThread()
        Log.d(TAG, "Sensor enabled: ${sensor?.maxDelay}")
    }
    
    fun setThread() {
        thread = Thread(Runnable {
            while(true){
                lock.withLock { if (!running) return@Runnable }
                Thread.sleep(5000)
                val tmp = calculateDirectionChanges()
                // TODO: Send value to server.
                Log.d(TAG, "Stability: $tmp")
            }
        })
        thread?.start()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // TODO: Implement orientation change
    }

    override fun onResume() {
        super.onResume()
        lock.withLock {
            running = true
            if (thread?.isAlive == false) thread?.start()
        }
        sensor?.also { accelerometer ->
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        }
        this.publisher?.resumeRecord()
    }

    override fun onPause() {
        super.onPause()
        lock.withLock { running = false }
        sensorManager?.unregisterListener(this)
        this.publisher?.pauseRecord()
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("Confirmation")
            .setMessage("Are you sure you want to stop the stream?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(
                android.R.string.yes
            ) { _, _ ->
                finish()

                super.onBackPressed()
            }
            .setNegativeButton(android.R.string.no, null).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        lock.withLock { running = false }
        this.publisher?.stopPublish()
        this.socket?.close(StreamCommunicationListener.NORMAL_CLOSURE_STATUS, "Activity stopped")
    }
}
