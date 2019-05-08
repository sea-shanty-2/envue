package dk.cs.aau.envue

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
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
import com.squareup.picasso.Picasso
import dk.cs.aau.envue.communication.*
import dk.cs.aau.envue.communication.packets.MessagePacket
import dk.cs.aau.envue.communication.packets.ReactionPacket
import dk.cs.aau.envue.nearby.NearbyBroadcastsAdapter
import dk.cs.aau.envue.shared.GatewayClient
import okhttp3.WebSocket


class PlayerActivity : AppCompatActivity(), EventListener, CommunicationListener {
    private var broadcastId: String = "main"
        set(value) {
            field = value

            this.nearbyBroadcastsList?.apply {
                runOnUiThread { scrollToCurrentBroadcast() }
            }

            this.nearbyBroadcastsAdapter?.apply {
                currentBroadcastId = value
                runOnUiThread { notifyDataSetChanged() }
            }
        }

    private var eventIds: ArrayList<String> = ArrayList()
        set(value) {
            field = value
            this.nearbyBroadcastsAdapter?.apply {
                broadcastList = eventIds
                runOnUiThread { notifyDataSetChanged() }
            }
        }

    private var broadcastIndex = 0

    private var fingerX1 = 0.0f
    private var fingerX2 = 0.0f
    private val minScrollDistance = 150  // Minimum distance for a swipe to be registered

    private var playerView: SimpleExoPlayerView? = null
    private var player: SimpleExoPlayer? = null
    private var editMessageView: EditText? = null
    private var chatList: RecyclerView? = null
    private var reactionList: RecyclerView? = null
    private var nearbyBroadcastsList: RecyclerView? = null
    private var recommendationView: View? = null
    private var recommendationTimeout: ProgressBar? = null
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var loading: ProgressBar? = null
    private var chatAdapter: MessageListAdapter? = null
    private var reactionAdapter: ReactionListAdapter? = null
    private var nearbyBroadcastsAdapter: NearbyBroadcastsAdapter? = null
    private var recommendationImageView: ImageView? = null
    private var socket: WebSocket? = null
    private var messages: ArrayList<Message> = ArrayList()
    private var emojiFragment: EmojiFragment? = null
    private var lastReactionAt: Long = 0
    private var recommendationExpirationThread: Thread? = null
    private var recommendedBroadcastId: String? = null
        set(value) {
            field = value
            this.nearbyBroadcastsAdapter?.apply {
                this.recommendedBroadcastId = value
                runOnUiThread { notifyDataSetChanged() }
            }
        }

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

    private fun startCommunicationSocket() {
        socket = StreamCommunicationListener.buildSocket(this, this.broadcastId)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the broadcastId as sent from the MapActivity (determined by which event was pressed)
        broadcastId = intent.getStringExtra("broadcastId") ?: "main"
        eventIds = intent.getStringArrayListExtra("eventIds") ?: ArrayList<String>().apply { add("main") }

        setContentView(R.layout.activity_player)

        // Initially disable the ability to send messages
        setConnected(false)

        // Create nearby broadcasts adapter
        nearbyBroadcastsAdapter = NearbyBroadcastsAdapter(eventIds, broadcastId, null, this::changeBroadcast)

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

        // Produces DataSource instances through which media data is loaded
        val defaultBandwidthMeter = DefaultBandwidthMeter()
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

        // Update viewer counts
        joinBroadcast(broadcastId)

        // Bind content
        bindContentView()

        // Launch background task for updating event ids
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
            UpdateEventIdsTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        } else {
            UpdateEventIdsTask(this).execute()
        }
    }

    private fun updateRecommendedBroadcast(broadcastId: String) {
        this.recommendedBroadcastId = broadcastId
        this.nearbyBroadcastsAdapter?.apply {
            recommendedBroadcastId = this@PlayerActivity.recommendedBroadcastId
            notifyDataSetChanged()
        }
    }


    private fun scrollToCurrentBroadcast() {
        nearbyBroadcastsAdapter?.let {
            nearbyBroadcastsList?.apply {
                this.layoutManager?.smoothScrollToPosition(this, null, it.getSelectedPosition())
            }
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
        nearbyBroadcastsList = findViewById(R.id.nearby_broadcasts_list)
        recommendationView = findViewById(R.id.recommendation_view)
        recommendationTimeout = findViewById(R.id.recommendation_timer)
        recommendationImageView = findViewById(R.id.recommendation_image)

        // Add click listener to add reaction button
        findViewById<ImageView>(R.id.reaction_add)?.setOnClickListener {
            val inflater = getSystemService(LAYOUT_INFLATER_SERVICE)
            val view = LayoutInflater.from(this).inflate(R.layout.fragment_reaction_list, null);
            val reactionList = view.findViewById<RecyclerView>(R.id.reaction_view).apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = reactionAdapter
            }

            // Create popup window
            PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true).apply {
                elevation = 20f
                showAtLocation(playerView, Gravity.CENTER, 0, playerView?.height?.plus(this.height)?.times(-1) ?: 0)
            }
        }

        // Scroll to selected
        scrollToCurrentBroadcast()

        // Prevent dimming
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Listen for clicks on recommendations
        recommendationImageView?.setOnClickListener {
            acceptRecommendation()
        }

        // Assign nearby broadcasts adapter and layout manager
        nearbyBroadcastsList?.apply {
            adapter = nearbyBroadcastsAdapter
            layoutManager = LinearLayoutManager(this@PlayerActivity).apply { orientation = 0}
        }

        // Assign reaction adapter and layout manager
        reactionList?.apply {
            adapter = reactionAdapter
            layoutManager = LinearLayoutManager(this@PlayerActivity).apply { orientation = 0}
        }

        // Update chat adapter
        chatAdapter?.apply {
            setLandscapeMode(this@PlayerActivity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        }

        // Assign chat adapter and layout manager
        chatList?.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@PlayerActivity).apply { stackFromEnd = true }
        }

        // When in horizontal we want to be able to click through the recycler
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            exoPlayerViewOnTouch()
        }

        // Make sure we can detect swipes in portrait mode as well
        (playerView as SimpleExoPlayerView).setOnTouchListener { _, event ->
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

    private fun updateRecommendationThumbnail() {
        recommendedBroadcastId?.let { broadcast ->
            recommendationImageView?.let {
                Picasso
                    .get()
                    .load("https://envue.me/relay/$broadcast/thumbnail")
                    .placeholder(R.drawable.ic_live_tv_48dp)
                    .error(R.drawable.ic_live_tv_48dp)
                    .into(recommendationImageView)
            }
        }
    }
    private fun showRecommendation(broadcastId: String) {
        recommendedBroadcastId = broadcastId
        recommendationView?.let { transitionView(it, 0f, 1f, View.VISIBLE) }
        updateRecommendationThumbnail()

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
        leaveBroadcast(broadcastId) { /* Do nothing */ }
        super.onDestroy()
        this.socket?.close(StreamCommunicationListener.NORMAL_CLOSURE_STATUS, "Activity stopped")
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

        // Bind new content view
        bindContentView()

        // Restore state
        recommendationVisibility?.let { recommendationView?.visibility = it }
        recommendationExpirationProgress?.let { recommendationTimeout?.progress = it }
        updateRecommendationThumbnail()
    }

    private fun acceptRecommendation() {
        cancelRecommendation()
        recommendedBroadcastId?.let { changeBroadcast(it) }
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
                if (deltaX > minScrollDistance) {
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

    // The param "id" is the Id of the broadcast to change to.
    // First leave the current broadcast, then update broadcastId to id
    // and join that.
    private fun changeBroadcast(id: String) {
        this.broadcastId = id
        val defaultBandwidthMeter = DefaultBandwidthMeter()
        val dataSourceFactory = DefaultDataSourceFactory(
            this,
            Util.getUserAgent(this, "Exo2"), defaultBandwidthMeter
        )

        // Leave current broadcast, join the new one
        leaveBroadcast(broadcastId, continueWith = {
            broadcastId = id; joinBroadcast(id)
        })

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
    }

    private fun leaveBroadcast(id: String, continueWith: () -> Unit) {
        val leaveMutation = BroadcastLeaveMutation.builder().id(id).build()
        GatewayClient.mutate(leaveMutation).enqueue(object: ApolloCall.Callback<BroadcastLeaveMutation.Data>() {
            override fun onResponse(response: Response<BroadcastLeaveMutation.Data>) {
                continueWith()  // Callback
            }

            override fun onFailure(e: ApolloException) {
                Log.d("LEAVE", "Something went wrong while leaving $id: $e")
                // We don't need to show a toast here
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
                // Do we need to show a toast here? As long as the player starts, it does not matter
                runOnUiThread {
                    Toast.makeText(this@PlayerActivity, "Something went wrong while joining $id \uD83D\uDE22",
                        Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}
