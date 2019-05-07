package dk.cs.aau.envue

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.widget.*
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player.EventListener
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.SimpleExoPlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.gson.Gson
import com.mapbox.mapboxsdk.maps.Style
import dk.cs.aau.envue.communication.*
import dk.cs.aau.envue.communication.packets.MessagePacket
import dk.cs.aau.envue.communication.packets.ReactionPacket
import dk.cs.aau.envue.shared.GatewayClient
import okhttp3.WebSocket


class PlayerActivity : AppCompatActivity(), EventListener, CommunicationListener {

    inner class UpdateEventIdsTask(c: Context): AsyncTask<Void, Void, Void>() {

        override fun doInBackground(vararg params: Void): Void {
            while (true) {
                updateEventIds()
                Log.d("EVENTUPDATE", "Updated event ids.")
                Thread.sleep(10000)  // 10 seconds}
            }
        }
    }

    fun setConnected(state: Boolean) {
        runOnUiThread {
            findViewById<Button>(R.id.button_chatbox_send)?.isEnabled = state
        }
    }

    override fun onClosed(code: Int) {
        setConnected(false)

        if (code != StreamCommunicationListener.NORMAL_CLOSURE_STATUS) {
            Thread.sleep(500)

            startCommunicationSocket()
        }
    }

    override fun onConnected() {
        setConnected(true)
    }

    override fun onMessage(message: Message) {
        runOnUiThread {
            addMessage(message)
            this.chatAdapter?.notifyDataSetChanged()
            scrollToBottom()
        }
    }

    override fun onReaction(reaction: String) {
        runOnUiThread {
            emojiFragment?.begin(reaction,this@PlayerActivity)
        }
    }

    private fun scrollToBottom() {
        this.chatAdapter?.itemCount?.let { this.chatList?.smoothScrollToPosition(it) }
    }

    private lateinit var broadcastId: String
    private lateinit var eventIds: ArrayList<String>
    private var broadcastIndex = 0

    private var fingerX1 = 0.0f
    private var fingerX2 = 0.0f
    private val MIN_DISTANCE = 150  // Minimum distance for a swipe to be registered

    private var playerView: SimpleExoPlayerView? = null
    private var player: SimpleExoPlayer? = null
    private var editMessageView: EditText? = null
    private var chatList: RecyclerView? = null
    private var reactionList: RecyclerView? = null
    private var recommendationView: View? = null
    private var recommendationTimeout: ProgressBar? = null
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var loading: ProgressBar? = null
    private var chatAdapter: MessageListAdapter? = null
    private var reactionAdapter: ReactionListAdapter? = null
    private var socket: WebSocket? = null
    private var messages: ArrayList<Message> = ArrayList()
    private var emojiFragment: EmojiFragment? = null
    private var lastReactionAt: Long = 0
    private var recommendationExpirationThread: Thread? = null
    private var recommendedBroadcastId: String? = null

    private fun startCommunicationSocket() {
        socket = StreamCommunicationListener.buildSocket(this, this.broadcastId)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the broadcastId as sent from the MapFragment (determined by which event was pressed)
        val intentKeys = intent?.extras?.keySet()
        broadcastId = intent.getStringExtra("broadcastId") ?: "main"
        eventIds = intent.getStringArrayListExtra("eventIds") ?: ArrayList<String>().apply { add("main") }



        setContentView(R.layout.activity_player)

        // Initially disable the ability to send messages
        setConnected(false)

        // Prevent dimming
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Create reaction adapter
        reactionAdapter = ReactionListAdapter(::addReaction, resources.getStringArray(R.array.allowed_reactions))

        // Create chat adapter
        chatAdapter = MessageListAdapter(this, messages)

        // Initialize communication socket
        startCommunicationSocket()

        // Initialize player
        val adaptiveTrackSelection = AdaptiveTrackSelection.Factory(DefaultBandwidthMeter())
        player = ExoPlayerFactory.newSimpleInstance(
            DefaultRenderersFactory(this),
            DefaultTrackSelector(adaptiveTrackSelection)
        )

        val defaultBandwidthMeter = DefaultBandwidthMeter()
        // Produces DataSource instances through which media data is loaded.
        val dataSourceFactory = DefaultDataSourceFactory(
            this,
            Util.getUserAgent(this, "Exo2"), defaultBandwidthMeter
        )

        // Create media source
        val hlsUrl = "https://envue.me/relay/$broadcastId"
        val uri = Uri.parse(hlsUrl)
        val mainHandler = Handler()
        val mediaSource = HlsMediaSource(uri, dataSourceFactory, mainHandler, null)

        val listener = this
        player?.apply {
            seekTo(currentWindow, playbackPosition)
            prepare(mediaSource, true, false)
            addListener(listener)
            playWhenReady = true
        }

        joinBroadcast(broadcastId)  // Update viewer counts

        bindContentView()

        // Launch background task for updating event ids
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
            UpdateEventIdsTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        } else {
            UpdateEventIdsTask(this).execute()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindContentView() {
        setContentView(R.layout.activity_player)
        playerView = findViewById(R.id.video_view)
        loading = findViewById(R.id.loading)
        editMessageView = findViewById(R.id.editText)
        chatList = findViewById(R.id.chat_view)
        reactionList = findViewById(R.id.reaction_view)
        recommendationView = findViewById(R.id.recommendation_view)
        recommendationTimeout = findViewById(R.id.recommendation_timer)

        // Assign reaction adapter and layout manager
        val reactionLayoutManager = LinearLayoutManager(this).apply { orientation = 0}
        reactionList?.apply {
            adapter = reactionAdapter
            layoutManager = reactionLayoutManager
        }

        // Assign chat adapter and layout manager
        val chatLayoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        chatList?.apply {
            adapter = chatAdapter
            layoutManager = chatLayoutManager
        }

        // When in horizontal we want to be able to click through the recycler
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            exoPlayerViewOnTouch()
        }

        // Make sure we can detect swipes in portrait mode as well
        (playerView as SimpleExoPlayerView).setOnTouchListener { view, event ->
            changeBroadcastOnSwipe(event)
            false
        }

        // Assign send button
        findViewById<Button>(R.id.button_chatbox_send)?.setOnClickListener {
            addLocalMessage()
        }

        // Creates fragments for EmojiReactionsFragment
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        emojiFragment = EmojiFragment()
        emojiFragment?.let {
            fragmentTransaction.replace(R.id.fragment_container, it)
            fragmentTransaction.commit()
        }

        // Assign player view
        player?.let { playerView?.player = it }

        // Update player state
        player?.let { onPlayerStateChanged(it.playWhenReady, it.playbackState) }

        // Ensure chat is scrolled to bottom
        this.scrollToBottom()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun exoPlayerViewOnTouch() {
        var isPressed = true
        var startX = 0F
        var startY = 0F
        val exoPlayer = playerView
        val chatView = chatList
        exoPlayer?.setOnClickListener { }
        chatView?.setOnTouchListener { _, event ->
            changeBroadcastOnSwipe(event)  // Detect swipes
            if (event?.action == MotionEvent.ACTION_DOWN) {
                startX = event.x
                startY = event.y

            } else if (event?.action == MotionEvent.ACTION_UP) {
                val endX = event.x
                val endY = event.y

                if (Math.abs(startX - endX) < 5 || Math.abs(startY- endY) < 5) {

                    if (isPressed) {
                        exoPlayer?.controllerHideOnTouch = false
                        player?.playWhenReady = false
                        player?.playbackState
                        isPressed = false
                    } else {
                        exoPlayer?.controllerHideOnTouch = true
                        player?.playWhenReady = true
                        player?.playbackState
                        isPressed = true
                    }
                }
            }
            false
        }
    }

    private fun addReaction(reaction: String) {
        val timeSinceReaction = System.currentTimeMillis() - lastReactionAt

        if (timeSinceReaction >= 250) {
            onReaction(reaction)
            socket?.send(Gson().toJson(ReactionPacket(reaction)))
            lastReactionAt = System.currentTimeMillis()
        }
    }

    private fun hideRecommendation() {
        recommendationView?.let { runOnUiThread { transitionView(it, 1f, 0f, View.GONE) }}
    }

    private fun showRecommendation(broadcastId: String) {
        recommendedBroadcastId = broadcastId
        recommendationView?.let { transitionView(it, 0f, 1f, View.VISIBLE) }

        recommendationExpirationThread = Thread {
            recommendationTimeout?.let { it.progress = it.max }

            while (recommendationTimeout?.let { it.progress > 0 } == true) {
                runOnUiThread { recommendationTimeout?.let { it.progress -= 1 } }
                try {
                    Thread.sleep(5)
                } catch (interruptedException: InterruptedException) {
                    return@Thread
                }
            }

            hideRecommendation()
        }
        recommendationExpirationThread?.start()
    }

    private fun cancelRecommendation() {
        recommendedBroadcastId = null
        recommendationExpirationThread?.interrupt()
        hideRecommendation()
    }

    private fun addLocalMessage() {
        val messageView = findViewById<EditText>(R.id.editText)
        val text = messageView?.text.toString()
        showRecommendation("test")
        if (!text.isEmpty()) {
            socket?.send(Gson().toJson(MessagePacket(text)))
            onMessage(Message(text))
            messageView.text.clear()
        }
    }

    private fun addMessage(message: Message) {
        this.messages.add(message)
    }

    override fun onPause() {
        super.onPause()
        player?.playWhenReady = false
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
        leaveBroadcast(broadcastId)
    }

    private fun transitionView(view: View, initialAlpha: Float, finalAlpha: Float, finalState: Int) {
        view.apply {
            visibility = View.VISIBLE
            alpha = initialAlpha
            animate()
                .alpha(finalAlpha)
                .setDuration(1000)
                .setListener((object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        view.visibility = finalState
                    }
                }))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        this.socket?.close(StreamCommunicationListener.NORMAL_CLOSURE_STATUS, "Activity stopped")
        leaveBroadcast(broadcastId)
    }

    private fun releasePlayer() {
        player?.let {
            playbackPosition = it.currentPosition
            currentWindow = it.currentWindowIndex
            playWhenReady = it.playWhenReady
            it.release()
        }

        player = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Since we are using custom layouts for different configurations, we need to manually code state persistence
        val recommendationVisibility = recommendationView?.visibility
        val recommendationExpirationProgress = recommendationTimeout?.progress

        bindContentView()

        // Restore state
        recommendationVisibility?.let { recommendationView?.visibility = it }
        recommendationExpirationProgress?.let { recommendationTimeout?.progress = it }
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            ExoPlayer.STATE_READY -> loading?.visibility = View.GONE
            ExoPlayer.STATE_BUFFERING -> loading?.visibility = View.VISIBLE
        }
    }

    private fun updateEventIds() {
        val eventQuery = EventWithIdQuery.builder().id(broadcastId).build()
        GatewayClient.query(eventQuery).enqueue(object: ApolloCall.Callback<EventWithIdQuery.Data>() {
            override fun onResponse(response: Response<EventWithIdQuery.Data>) {
                val ids = response.data()?.events()?.containing()?.broadcasts()?.map { it.id() }
                if (ids != null) {
                    eventIds = ids as ArrayList<String>
                } else {
                    Log.d("EVENTUPDATE", "No broadcasts in this event (broadcast id was $broadcastId).")
                }
            }

            override fun onFailure(e: ApolloException) {
                Log.d("EVENTUPDATE", "Something went wrong while fetching broadcasts in the event: ${e.message}")
            }
        })
    }

    private fun changeBroadcastOnSwipe(event: MotionEvent) {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                fingerX1 = event.x  // Maybe the start of a swipe
            }

            MotionEvent.ACTION_UP -> {
                fingerX2 = event.x  // Maybe the end of a swipe

                // Measure horizontal distance between x1 and x2 - if its big enough, change broadcast
                val deltaX = Math.abs(fingerX2 - fingerX1)
                if (deltaX > MIN_DISTANCE) {
                    // This is a swipe, change broadcast
                    broadcastIndex++
                    Toast.makeText(this,
                        "Changing from $broadcastId to ${eventIds[broadcastIndex % eventIds.size]}",
                        Toast.LENGTH_LONG)
                        .show()
                    changeBroadcast(eventIds[broadcastIndex % eventIds.size])  // Loop around if necessary

                } else {
                    // Do nothing, maybe display helper message
                    Toast.makeText(this, "Swipe horizontally to see the rest of the event!", Toast.LENGTH_LONG).show()
                }
            }
            else -> return
        }
    }

    private fun changeBroadcast(id: String) {
        val defaultBandwidthMeter = DefaultBandwidthMeter()
        val dataSourceFactory = DefaultDataSourceFactory(
            this,
            Util.getUserAgent(this, "Exo2"), defaultBandwidthMeter
        )
        // Leave current broadcast
        leaveBroadcast(broadcastId)

        // Modify broadcast id
        broadcastId = id

        // Create media source
        val hlsUrl = "https://envue.me/relay/$broadcastId"  // Loop around if necessary
        val uri = Uri.parse(hlsUrl)
        val mainHandler = Handler()
        val mediaSource = HlsMediaSource(uri, dataSourceFactory, mainHandler, null)

        val listener = this
        player?.apply {
            seekTo(currentWindow, playbackPosition)
            prepare(mediaSource, true, false)
            addListener(listener)
            playWhenReady = true
        }

        // Join this broadcast
        joinBroadcast(broadcastId)
    }

    private fun leaveBroadcast(id: String) {
        val leaveMutation = BroadcastLeaveMutation.builder().id(id).build()
        GatewayClient.mutate(leaveMutation).enqueue(object: ApolloCall.Callback<BroadcastLeaveMutation.Data>() {
            override fun onResponse(response: Response<BroadcastLeaveMutation.Data>) {
                // No action required
            }

            override fun onFailure(e: ApolloException) {
                Log.d("LEAVE", "Something went wrong while leaving $id: $e")
            }
        })
    }

    private fun joinBroadcast(id: String) {
        val joinMutation = BroadcastJoinMutation.builder().id(id).build()
        GatewayClient.mutate(joinMutation).enqueue(object: ApolloCall.Callback<BroadcastJoinMutation.Data>() {
            override fun onResponse(response: Response<BroadcastJoinMutation.Data>) {
                // No action required
            }

            override fun onFailure(e: ApolloException) {
                Log.d("JOIN", "Something went wrong while joining $id: $e")
            }
        })
    }
}
