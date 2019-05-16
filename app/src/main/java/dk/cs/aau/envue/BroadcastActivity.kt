package dk.cs.aau.envue

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.hardware.*
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.github.faucamp.simplertmp.RtmpHandler
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import dk.cs.aau.envue.communication.CommunicationListener
import dk.cs.aau.envue.communication.Message
import dk.cs.aau.envue.communication.MessageListAdapter
import dk.cs.aau.envue.communication.StreamCommunicationListener
import dk.cs.aau.envue.communication.packets.ChatStatePacket
import dk.cs.aau.envue.shared.GatewayClient
import dk.cs.aau.envue.type.BroadcastUpdateInputType
import dk.cs.aau.envue.type.LocationInputType
import dk.cs.aau.envue.utility.haversine
import net.ossrs.yasea.SrsEncodeHandler
import net.ossrs.yasea.SrsPublisher
import okhttp3.WebSocket
import java.io.IOException
import java.net.SocketException
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class BroadcastActivity : AppCompatActivity(), RtmpHandler.RtmpListener, SrsEncodeHandler.SrsEncodeListener,
    CommunicationListener, SensorEventListener {
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
    private var counterThread: Thread? = null
    private var running = true
    private var currentBitrate: Int = 0
    private lateinit var broadcastId: String
    private lateinit var updater: AsyncTask<Unit, Unit, Unit>

    private var chatEnabled: Boolean = true
        set(value) {
            field = value
            chatList?.visibility = if (value) View.VISIBLE else View.GONE
            socket?.send(Gson().toJson(ChatStatePacket(value)))
        }

    private inner class BroadcastInformationUpdater(id: String, val activity: BroadcastActivity) :
        AsyncTask<Unit, Unit, Unit>() {
        val queryBuilder: BroadcastUpdateMutation.Builder = BroadcastUpdateMutation.builder().id(id)
        val typeBuilder: BroadcastUpdateInputType.Builder = BroadcastUpdateInputType.builder()
        private var fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(activity)
        private lateinit var currentLocation: LocationInputType
        private lateinit var lastLocation: LocationInputType
        private var lastStability = 0.0
        private var lastBitrate = 0
        private val MAX_LOCATION_RETRIEVAL_ATTEMPTS = 50

        override fun doInBackground(vararg params: Unit) {
            var stability: Double
            var bitrate = 0

            ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)

            var count = 0

            while (!isCancelled) {
                fusedLocationClient.lastLocation.apply {
                    addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            currentLocation = LocationInputType.builder()
                                .latitude(location.latitude)
                                .longitude(location.longitude)
                                .build()
                        }
                    }

                    addOnFailureListener { Log.d(TAG, it.message) }
                    addOnCanceledListener { Log.d(TAG, "Cancelled") }
                }

                if (!::currentLocation.isInitialized) {
                    if (count++ > MAX_LOCATION_RETRIEVAL_ATTEMPTS) {
                        Log.e(TAG, "Too many location tries.")
                        runOnUiThread {
                            Toast.makeText(this@BroadcastActivity, getString(R.string.unable_to_retrieve_location), Toast.LENGTH_LONG).show()
                            finish()
                        }

                        return
                    }

                    Thread.sleep(1000)
                    continue
                }

                if (!::lastLocation.isInitialized) lastLocation = currentLocation

                stability = calculateDirectionChanges()
                lock.withLock { bitrate = currentBitrate }

                var update = typeBuilder

                // Update if above ten percent difference
                if (stability != lastStability && abs(stability - lastStability) / ((stability + lastStability) / 2) > 0.1) {
                    update = update.stability(stability)
                    lastStability = stability
                    Log.d(TAG, "Update stability")
                }

                // Update if above ten percent difference
                if (bitrate != lastBitrate && abs(bitrate - lastBitrate) / ((bitrate + lastBitrate) / 2) > 0.1) {
                    update = update.bitrate(bitrate)
                    lastBitrate = bitrate
                    Log.d(TAG, "Update bitrate")
                }

                // Update if stream has moved 10 meters
                if (haversine(lastLocation, currentLocation) > 10) {
                    update = update.location(currentLocation)
                    lastLocation = currentLocation
                    Log.d(TAG, "Update Location")
                }

                val query = queryBuilder.broadcast(update.build()).build()

                GatewayClient.mutate(query).enqueue(object : ApolloCall.Callback<BroadcastUpdateMutation.Data>() {
                    override fun onResponse(response: Response<BroadcastUpdateMutation.Data>) {
                        Log.d(TAG, response.data()?.broadcasts()?.update()?.id())
                    }

                    override fun onFailure(e: ApolloException) {
                        Log.d(TAG, e.message)
                    }
                })

                updateViewerCount()

                count = 0

                Thread.sleep(15000)
            }
        }
    }

    override fun onChatStateChanged(enabled: Boolean) {
        // Confirmation of changed chat state
    }

    override fun onCommunicationClosed(code: Int) {
        if (code != StreamCommunicationListener.NORMAL_CLOSURE_STATUS) {
            Thread.sleep(500)

            startCommunicationSocket()
        }
    }

    override fun onCommunicationIdentified(sequenceId: Int, name: String) {
    }

    private fun startCommunicationSocket() {
        socket = StreamCommunicationListener.buildSocket(this, this.broadcastId)
    }

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
        var directionChange = 0

        if (sampleArray[lastIndex][x] != sampleArray[lastIndex / 2][y]
            || sampleArray[lastIndex][y] != sampleArray[lastIndex / 2][y]
            || sampleArray[lastIndex][z] != sampleArray[lastIndex / 2][z]
        ) {
            for (i in 0..(lastIndex - 3)) {
                val sgn1 = calculateSign(sampleArray[i], sampleArray[i + 1])
                val sgn2 = calculateSign(sampleArray[i + 1], sampleArray[i + 2])
                if (!(sgn1 contentEquals sgn2)) {
                    directionChange++
                }
            }
        }

        // Smooth curve
        return 1 - Math.tanh(directionChange.toDouble() / curveSmoothingConstant)
    }

    private fun calculateSign(arrayP: FloatArray, arrayQ: FloatArray): FloatArray {
        val xDiff = arrayP[x] - arrayQ[x]
        val yDiff = arrayP[y] - arrayQ[y]
        val zDiff = arrayP[z] - arrayQ[z]

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
            emojiFragment?.begin(reaction, this@BroadcastActivity)
        }
    }

    private fun scrollToBottom() {
        this.chatAdapter?.itemCount?.let { this.chatList?.smoothScrollToPosition(it) }
    }

    override fun onRtmpConnecting(msg: String?) {
    }

    private fun startCounter() {
        val startedAt = System.currentTimeMillis()
        counterThread = Thread {
            while (true) {
                try {
                    Thread.sleep(1000)
                } catch (ex: InterruptedException) {
                    break
                }

                val difference = System.currentTimeMillis() - startedAt
                val seconds = difference / 1000 % 60
                val minutes = difference / 1000 / 60

                runOnUiThread { setLiveText("${getString(R.string.live)} ${minutes.format(2)}:${seconds.format(2)}") }
            }
        }
        counterThread?.start()
    }

    private fun stopCounter() {
        this.counterThread?.interrupt()
    }

    override fun onRtmpConnected(msg: String?) {
        setLiveStatus(true)
        startCounter()
    }

    private fun setLiveText(newText: String) {
        this.findViewById<TextView>(R.id.live_status)?.apply {
            text = newText
        }
    }

    private fun setLiveStatus(live: Boolean) {
        this.findViewById<TextView>(R.id.live_status)?.apply {
            visibility = if (live) View.VISIBLE else View.GONE
        }
    }

    override fun onRtmpVideoStreaming() {
    }

    override fun onRtmpAudioStreaming() {
    }

    override fun onRtmpStopped() {
        Toast.makeText(applicationContext, "RTMP stopped", Toast.LENGTH_SHORT).show()
        stopCounter()
    }

    override fun onRtmpDisconnected() {
        setLiveStatus(false)
    }

    override fun onRtmpVideoFpsChanged(fps: Double) {
        Log.i(TAG, "FPS: $fps")
    }

    override fun onRtmpVideoBitrateChanged(bitrate: Double) {
        // Toast.makeText(applicationContext, "Bitrate: $bitrate", Toast.LENGTH_SHORT).show()
        lock.withLock { currentBitrate = bitrate.toInt() }
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

        val id = intent.getStringExtra("ID")
        val rtmp = intent.getStringExtra("RTMP")

        broadcastId = id
        Log.d("BROADIDTIMES", id)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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
            startPublish(rtmp)
            startCamera()
        }

        // Create chat adapter
        chatAdapter = MessageListAdapter(this, messages, isStreamerView = true)

        // Initialize communication socket
        startCommunicationSocket()
        chatList = findViewById(R.id.chat_view)

        // Creates fragments for EmojiReactionsFragment
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        emojiFragment = EmojiFragment()
        emojiFragment?.let {
            fragmentTransaction.replace(R.id.fragment_container, it)
            fragmentTransaction.commit()
        }

        // Create popup menu when settings clicked
        findViewById<ImageView>(R.id.settings)?.setOnClickListener {
            val popup = PopupMenu(this@BroadcastActivity, it)
            popup.menuInflater.inflate(R.menu.broadcast_settings, popup.menu)

            popup.menu.findItem(R.id.enable_chat)?.apply {
                isChecked = chatEnabled
                setOnMenuItemClickListener {
                    chatEnabled = !chatEnabled
                    true
                }

                popup.show()
            }
        }

        // Assign chat adapter and layout manager
        val chatLayoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        chatList?.apply {
            adapter = chatAdapter
            layoutManager = chatLayoutManager
        }

        // Enable acceleration sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        updater = BroadcastInformationUpdater(id, this).apply {
            execute()
        }

        Log.d(TAG, "Sensor enabled: ${sensor?.maxDelay}")

        // Set stop button listener
        findViewById<ImageView>(R.id.stop_broadcast_button)?.setOnClickListener {
            this.onBackPressed()
        }
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
                removeFromActiveEvents()
                updater.cancel(true)
            }
            .setNegativeButton(android.R.string.no, null).show()
    }

    override fun onDestroy() {
        removeFromActiveEvents()
        super.onDestroy()
        lock.withLock { running = false }
        this.publisher?.stopPublish()
        this.socket?.close(StreamCommunicationListener.NORMAL_CLOSURE_STATUS, "Activity stopped")
    }

    private fun removeFromActiveEvents() {
        val mutation = BroadcastStopMutation.builder().id(broadcastId).build()

        GatewayClient.mutate(mutation).enqueue(object : ApolloCall.Callback<BroadcastStopMutation.Data>() {
            override fun onResponse(response: Response<BroadcastStopMutation.Data>) {
                val joinedTimeStamps = response.data()?.broadcasts()?.stop()?.joinedTimeStamps()
                val leftTimeStamps = response.data()?.broadcasts()?.stop()?.leftTimeStamps()

                // Start statistics activity with viewer count stats
                startStatisticsActivity(joinedTimeStamps?.toTypedArray(), leftTimeStamps?.toTypedArray())
            }

            override fun onFailure(e: ApolloException) {
                Log.d("STOPBROADCAST", "Stop broadcast mutation failed, broadcast has not been removed from events!")
            }
        })
    }

    private fun updateViewerCount() {

        val viewerQuery = BroadcastStatsQuery.builder().id(broadcastId).build()
        GatewayClient.query(viewerQuery).enqueue(object : ApolloCall.Callback<BroadcastStatsQuery.Data>() {
            override fun onResponse(response: Response<BroadcastStatsQuery.Data>) {

                // Fetch and validate the query result data
                val data = response.data()?.broadcasts()?.single()
                if (data == null) {

                    Log.d(
                        "VIEWERCOUNT",
                        "The viewer count query result was null"
                    )
                    return
                }
                else {

                    Log.d(
                        "VIEWERCOUNT",
                        "Fetched viewer count for broadcast ($broadcastId):\n" +
                            "current viewer count: ${data.current_viewer_count()}\n" +
                            "total viewer count: ${data.total_viewer_count()}"
                    )
                }

                runOnUiThread {
                    findViewById<TextView>(R.id.viewer_count)?.apply {
                        text = data.current_viewer_count().toString()
                        visibility = View.VISIBLE
                    }
                    // Update like ratio
                    findViewById<TextView>(R.id.like_ratio)?.apply {
                        val ratingCount = data.positiveRatings() + data.negativeRatings()
                        if (ratingCount > 0) {
                            visibility = View.VISIBLE
                            text = context.getString(R.string.likes, (1f * data.positiveRatings() / ratingCount * 100).roundToInt(), ratingCount)
                        }
                    }
                }}

            override fun onFailure(e: ApolloException) {

            }
        })
    }

    private fun startStatisticsActivity(joinedTimestamps: Array<BroadcastStopMutation.JoinedTimeStamp?>?,
                                        leftTimestamps: Array<BroadcastStopMutation.LeftTimeStamp?>?) {
        val joined =
            joinedTimestamps?.filter { i -> i != null }?.map { i -> i?.time() as Int }?.toTypedArray() as Array<Int>
        val left =
            leftTimestamps?.filter { i -> i != null }?.map { i -> i?.time() as Int }?.toTypedArray() as Array<Int>

        if (joined.isEmpty() && left.isEmpty()) { return }  // No reason to show the statistics page if there were no viewers

        val intent = Intent(this, StatisticsActivity::class.java).apply {
            putExtra("joinedTimestamps", joined)
            putExtra("leftTimestamps", left)
        }

        startActivity(intent)
    }
}

fun Number.format(digits: Int) = "%0${digits}d".format(this)
