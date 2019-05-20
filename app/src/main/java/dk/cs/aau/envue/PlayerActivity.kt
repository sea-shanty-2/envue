package dk.cs.aau.envue

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.InputType
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.EventListener
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.gson.Gson
import dk.cs.aau.envue.communication.*
import dk.cs.aau.envue.communication.packets.MessagePacket
import dk.cs.aau.envue.communication.packets.ReactionPacket
import dk.cs.aau.envue.nearby.NearbyBroadcastsAdapter
import dk.cs.aau.envue.shared.Broadcast
import dk.cs.aau.envue.shared.GatewayClient
import okhttp3.WebSocket
import java.lang.Math.pow
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.absoluteValue


class PlayerActivity : AppCompatActivity(), EventListener, CommunicationListener, RecommendationFragment.OnRecommendationFragmentListener {
    private var fingerX1 = 0.0f
    private var fingerX2 = 0.0f
    private val minScrollDistance = 150  // Minimum distance for a swipe to be registered

    // Player
    private var playerView: PlayerView? = null
    private var player: ExoPlayer? = null
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var loading: ProgressBar? = null

    // Communication
    private var editMessageView: EditText? = null
    private var chatList: RecyclerView? = null
    private var reactionList: RecyclerView? = null
    private var chatAdapter: MessageListAdapter? = null
    private var reactionAdapter: ReactionListAdapter? = null
    private var messages: ArrayList<Message> = ArrayList()
    private var emojiFragment: EmojiFragment? = null
    private var lastReactionAt: Long = 0
    private var ownDisplayName: String = "You"
    private var ownSequenceId: Int = 0
    private var showChatInLandscape: Boolean = true
    private var showRecommendations: Boolean = true

    // Broadcast selection and recommendation
    private var nearbyBroadcastsList: RecyclerView? = null
    private var nearbyBroadcastsAdapter: NearbyBroadcastsAdapter? = null
    private var recommendationExpirationThread: Thread? = null
    private var recommendationProgress: Int = 0
    private var currentRecommendationFragment: RecommendationFragment? = null
    private val nextRecommendation: ConcurrentHashMap<String, Long> = ConcurrentHashMap()
    private var recommendationAccepted: Boolean = false
    private var numDismisses: Int = 0
    private lateinit var updater: AsyncTask<Unit, Unit, Unit>

    private var broadcastId: String = "main"
        set(value) {
            field = value

            this.nearbyBroadcastsAdapter?.apply {
                currentBroadcastId = value
                runOnUiThread { notifyDataSetChanged() }
            }

            this.nearbyBroadcastsList?.apply {
                runOnUiThread { scrollToCurrentBroadcast() }
            }
        }

    private var nearbyBroadcasts: List<EventBroadcastsWithStatsQuery.Broadcast> = ArrayList()
        set(value) {
            field = value
            this.nearbyBroadcastsAdapter?.apply {
                broadcastList = nearbyBroadcasts
                runOnUiThread { notifyDataSetChanged() }
            }
        }

    private var broadcastIndex
        get() = this.nearbyBroadcastsAdapter?.getSelectedPosition() ?: 0
        set(value) {
            this.nearbyBroadcastsAdapter?.apply {
                if (this.broadcastList.isNotEmpty()) {
                    val newValue = if (value < 0) this.broadcastList.size - 1 else value
                    this@PlayerActivity.changeBroadcast(this.broadcastList[newValue % this.broadcastList.size].id())
                }
            }
        }

    // Network
    private var socket: WebSocket? = null

    private var communicationConnected: Boolean = false
        set(value) {
            field = value
            runOnUiThread {
                findViewById<Button>(R.id.button_chatbox_send)?.isEnabled = value
            }
        }

    private var recommendedBroadcastId: String? = null
        set(value) {
            field = value
            this.nearbyBroadcastsAdapter?.apply {
                this.recommendedBroadcastId = value
                runOnUiThread { notifyDataSetChanged() }
            }
        }

    inner class UpdateEventIdsTask(c: Context) : AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg params: Unit?) {
            while (!isCancelled) {
                updateEventIds()
                Thread.sleep(5000)
            }
        }
    }

    private fun isLandscape(): Boolean = this@PlayerActivity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    override fun onRecommendationDismissed(broadcastId: String) {
        if (recommendationProgress > 0) {
            recommendationExpirationThread?.interrupt()
        }

        numDismisses++
    }

    override fun onRecommendationAccepted(broadcastId: String) {
        recommendationAccepted = true
        changeBroadcast(broadcastId)
    }

    override fun onChatStateChanged(enabled: Boolean) {
        runOnUiThread {
            onMessage(SystemMessage(resources.getString(if (enabled) R.string.chat_enabled else R.string.chat_disabled)))
        }
    }

    override fun onCommunicationClosed(code: Int) {
        communicationConnected = false

        if (code != StreamCommunicationListener.NORMAL_CLOSURE_STATUS) {
            Thread.sleep(1000)

            startCommunicationSocket()
        }
    }

    override fun onCommunicationIdentified(sequenceId: Int, name: String) {
        communicationConnected = true

        ownDisplayName = name
        ownSequenceId = sequenceId

        runOnUiThread { editMessageView?.hint = getString(R.string.write_a_message_as, name) }
    }

    override fun onMessage(message: Message) {
        runOnUiThread {
            addMessage(message)
            this.chatAdapter?.notifyDataSetChanged()
            scrollToBottom()
        }
    }

    override fun onReaction(reaction: String) {
        runOnUiThread { emojiFragment?.begin(reaction, this@PlayerActivity) }
    }

    private fun scrollToBottom() {
        this.chatAdapter?.itemCount?.let { this.chatList?.smoothScrollToPosition(it) }
    }

    private fun startCommunicationSocket() {
        messages.clear()
        runOnUiThread { chatAdapter?.notifyDataSetChanged() }

        socket = StreamCommunicationListener.buildSocket(this, this.broadcastId)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        // Get the broadcastId as sent from the MapFragment (determined by which event was pressed)
        broadcastId = intent.getStringExtra("broadcastId") ?: "main"

        // Initially disable the ability to send messages
        communicationConnected = false

        // Create nearby broadcasts adapter
        nearbyBroadcastsAdapter = NearbyBroadcastsAdapter(nearbyBroadcasts, broadcastId, null, this::changeBroadcast)

        // Use the initial broadcast as the recommended id
        recommendedBroadcastId = broadcastId

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

        window.decorView.findViewById<View>(android.R.id.content).viewTreeObserver.addOnGlobalLayoutListener {
            scrollToBottom()
        }

        // Begin playing the broadcast
        changePlayerSource(broadcastId)

        // Update viewer counts
        Broadcast.join(broadcastId)

        // Bind content
        bindContentView()

        // Launch background task for updating event ids
        updater = UpdateEventIdsTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
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

        // Load views
        playerView = findViewById(R.id.video_view)
        loading = findViewById(R.id.loading)
        editMessageView = findViewById(R.id.editText)
        chatList = findViewById(R.id.chat_view)
        reactionList = findViewById(R.id.reaction_view)
        nearbyBroadcastsList = findViewById(R.id.nearby_broadcasts_list)

        // Add click listener to add reaction button
        findViewById<ImageView>(R.id.reaction_add)?.setOnClickListener {
            val view = LayoutInflater.from(this).inflate(R.layout.fragment_reaction_list, null)
            view.findViewById<RecyclerView>(R.id.reaction_view).apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = reactionAdapter
            }

            // Create popup window
            PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true).apply {
                elevation = 20f
                showAtLocation(playerView, Gravity.CENTER, 0, 0)
            }
        }

        // Scroll to selected
        scrollToCurrentBroadcast()

        // Prevent dimming
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Assign nearby broadcasts adapter and layout manager
        nearbyBroadcastsList?.apply {
            adapter = nearbyBroadcastsAdapter
            layoutManager = LinearLayoutManager(this@PlayerActivity).apply { orientation = 0 }
        }

        // Assign reaction adapter and layout manager
        reactionList?.apply {
            adapter = reactionAdapter
            layoutManager = LinearLayoutManager(this@PlayerActivity).apply { orientation = 0 }
        }

        // Update chat adapter
        chatAdapter?.apply {
            isLandscape = this@PlayerActivity.isLandscape()
        }

        // Assign chat adapter and layout manager
        chatList?.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@PlayerActivity).apply { stackFromEnd = true }
        }

        // Make sure we can detect swipes in portrait mode as well
        (playerView as PlayerView).setOnTouchListener { _, event ->
            changeBroadcastOnSwipe(event)
            false
        }

        // Assign send button
        findViewById<Button>(R.id.button_chatbox_send)?.apply {
            setOnClickListener { addLocalMessage() }
            isEnabled = communicationConnected
        }

        findViewById<EditText>(R.id.editText)?.setOnEditorActionListener { _, actionId, _ ->
            var handle = false
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                addLocalMessage()
                handle = true
            }
            handle
        }

        // Creates fragments for EmojiReactionsFragment
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        emojiFragment = EmojiFragment()
        emojiFragment?.let {
            fragmentTransaction.replace(R.id.fragment_container, it)
            fragmentTransaction.commit()
        }

        // Assign player view
        player?.apply {
            playerView?.let {
                it.player = this
                it.setControllerVisibilityListener { visibility ->
                    if (this@PlayerActivity.isLandscape()) {
                        this@PlayerActivity.findViewById<TextView>(R.id.editText).visibility = visibility
                    }
                }
            }
            onPlayerStateChanged(playWhenReady, playbackState)
        }

        // Hide chat container if chat is disabled
        val chatContainer = findViewById<LinearLayout>(R.id.chat_container)
        chatContainer?.visibility = if (showChatInLandscape) View.VISIBLE else View.GONE

        // Add stream options in isLandscape mode
        findViewById<ImageView>(R.id.stream_settings)?.apply {
            visibility = if (this@PlayerActivity.isLandscape()) View.VISIBLE else View.GONE

            if (this@PlayerActivity.isLandscape()) {
                setOnClickListener {
                    val popup = PopupMenu(this@PlayerActivity, it)
                    popup.menuInflater.inflate(R.menu.stream_settings, popup.menu)

                    popup.menu.findItem(R.id.enable_chat)?.apply {
                        isChecked = showChatInLandscape
                        setOnMenuItemClickListener {
                            showChatInLandscape = !isChecked
                            chatContainer?.visibility = if (showChatInLandscape) View.VISIBLE else View.GONE
                            true
                        }
                    }

                    popup.menu.findItem(R.id.enable_recommendations)?.apply {
                        isChecked = showRecommendations
                        setOnMenuItemClickListener {
                            showRecommendations = !isChecked
                            if (!showRecommendations) {
                                recommendationExpirationThread?.interrupt()
                            }
                            true
                        }
                    }

                    popup.show()
                }
            }
        }

        // Add click listener to report stream button
        findViewById<ImageView>(R.id.report_stream)?.setOnClickListener { reportContentDialog() }

        // Ensure chat is scrolled to bottom
        this.scrollToBottom()
    }

    private fun reportContentDialog() {
        val input = EditText(this).apply { 
            inputType = InputType.TYPE_CLASS_TEXT
            hint = context.getString(R.string.report_reason)
        }

        AlertDialog.Builder(this).apply {
            setTitle(getString(R.string.report_video))
            setView(input)
            setPositiveButton("OK") { _, _ -> sendReport(input) }
            setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            show()
        }
    }

    private fun sendReport(message: EditText) {
        val reportMessage = BroadcastReportMutation.builder().id(broadcastId).message(message.text.toString()).build()
        GatewayClient.mutate(reportMessage).enqueue(object : ApolloCall.Callback<BroadcastReportMutation.Data>() {
            override fun onResponse(response: Response<BroadcastReportMutation.Data>) {
                runOnUiThread {
                    Toast.makeText(
                        this@PlayerActivity,
                        getString(R.string.broadcast_reported),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            override fun onFailure(e: ApolloException) {
                runOnUiThread {
                    Toast.makeText(
                        this@PlayerActivity,
                        getString(R.string.error_occurred),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })
    }

    private fun addReaction(reaction: String) {
        val timeSinceReaction = System.currentTimeMillis() - lastReactionAt

        if (timeSinceReaction >= 250) {
            onReaction(reaction)
            socket?.send(Gson().toJson(ReactionPacket(reaction)))
            lastReactionAt = System.currentTimeMillis()
        }
    }

    private fun showRecommendation(broadcastId: String) {
        val allowedAt = nextRecommendation[broadcastId] ?: 0
        if (!isLandscape() || broadcastId == this.broadcastId || recommendationProgress > 0|| !showRecommendations ||
                System.currentTimeMillis() < allowedAt) {
            return
        }

        // Slide in new recommendation
        currentRecommendationFragment = RecommendationFragment.newInstance(broadcastId).also {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.enter, R.anim.exit)
                .replace(R.id.recommendation_view, it)
                .commit()
        }

        // Start expiration thread
        recommendedBroadcastId = broadcastId
        recommendationExpirationThread = Thread {
            recommendationProgress = 500

            while (recommendationProgress-- > 0) {
                currentRecommendationFragment?.view?.findViewById<ProgressBar>(R.id.recommendation_timer)?.apply {
                    progress = recommendationProgress
                }

                try {
                    Thread.sleep(50)
                } catch (interruptedException: InterruptedException) {
                    break
                }
            }

            // Reset progress
            recommendationProgress = 0

            // Hide recommendation
            runOnUiThread {
                currentRecommendationFragment?.let { fragment ->
                    val animation = AnimationUtils.loadAnimation(this, R.anim.exit).apply {
                        setAnimationListener(object : Animation.AnimationListener {
                            override fun onAnimationStart(animation: Animation?) {}

                            override fun onAnimationRepeat(animation: Animation?) {}

                            override fun onAnimationEnd(animation: Animation?) {
                                supportFragmentManager.beginTransaction()
                                    .remove(fragment)
                                    .commit()
                            }
                        })
                    }

                    findViewById<FrameLayout>(R.id.recommendation_view)?.startAnimation(animation)
                }
            }

            // Check if it was dismissed or accepted
            if (!recommendationAccepted) {
                onRecommendationDismissed(broadcastId)
            } else {
                numDismisses = 0
            }

            // Determine when we can recommend this broadcast again
            nextRecommendation[broadcastId] = (pow(2.0, numDismisses.toDouble()) * 1000 + 30000 + System.currentTimeMillis()).toLong()

            recommendationAccepted = false
        }

        recommendationExpirationThread?.start()
    }

    private fun addLocalMessage() {
        val messageView = findViewById<EditText>(R.id.editText)
        val text = messageView?.text.toString()

        if (!text.isEmpty()) {
            socket?.send(Gson().toJson(MessagePacket(text)))
            onMessage(Message(text, ownDisplayName, ownSequenceId))
            messageView.text.clear()
        }
    }

    private fun addMessage(message: Message) {
        this.messages.add(message)
    }

    override fun onPause() {
        super.onPause()
        player?.apply {
            this@PlayerActivity.playbackPosition = this.currentPosition
            playWhenReady = false
        }
    }

    override fun onResume() {
        super.onResume()
        player?.apply {
            seekTo(playbackPosition)
            playWhenReady = true
        }
    }

    override fun onDestroy() {
        Broadcast.leave()
        updater.cancel(true)
        super.onDestroy()
        player?.release()
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

        // Destroy recommendation thread
        recommendationExpirationThread?.interrupt()

        // Bind new content view
        bindContentView()
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        loading?.visibility = if (playbackState == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
    }

    private fun updateEventIds() {
        val eventQuery = EventBroadcastsWithStatsQuery.builder().id(broadcastId).build()
        GatewayClient.query(eventQuery).enqueue(object : ApolloCall.Callback<EventBroadcastsWithStatsQuery.Data>() {
            override fun onResponse(response: Response<EventBroadcastsWithStatsQuery.Data>) {
                val broadcasts = response.data()?.events()?.containing()?.broadcasts()?.toList()
                val recommendedId = response.data()?.events()?.containing()?.recommended()?.id()

                // Update nearby broadcasts
                broadcasts?.let { nearbyBroadcasts = it }

                // Show new recommendation
                recommendedId?.let { runOnUiThread { showRecommendation(it) } }
            }

            override fun onFailure(e: ApolloException) {
                Log.d("EVENTUPDATE", "Something went wrong while fetching broadcasts in the event: ${e.message}")
            }
        })
    }

    private fun changeBroadcastOnSwipe(event: MotionEvent) {
        if (nearbyBroadcasts.size < 2) {
            return
        }

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                fingerX1 = event.x  // Maybe the start of a swipe
            }

            MotionEvent.ACTION_UP -> {
                fingerX2 = event.x  // Maybe the end of a swipe

                // Measure horizontal distance between x1 and x2 - if its big enough, change broadcast
                val deltaX = fingerX2 - fingerX1
                if (deltaX.absoluteValue > minScrollDistance) {
                    broadcastIndex += if (deltaX < 0) 1 else -1
                } else {
                    // Do nothing, maybe display helper message
                    Toast.makeText(this, "Swipe horizontally to see the rest of the event!", Toast.LENGTH_LONG).show()
                }
            }
            else -> return
        }
    }

    private fun getHlsUri(fromBroadcastId: String) = Uri.parse("https://envue.me/relay/$fromBroadcastId")

    private fun getMediaSource(fromBroadcastId: String) = HlsMediaSource.Factory(getDataSource()).createMediaSource(getHlsUri(fromBroadcastId))

    private fun getDataSource() = DefaultHttpDataSourceFactory(Util.getUserAgent(this, "Envue"))

    private fun changePlayerSource(toBroadcastId: String) {
        // Create media source
        player?.apply {
            seekTo(currentWindow, playbackPosition)
            prepare(getMediaSource(toBroadcastId), true, false)
            addListener(this@PlayerActivity)
            playWhenReady = true
        }
    }

    private fun changeBroadcast(id: String) {
        // If this broadcast is recommended then interrupt the recommendation
        currentRecommendationFragment?.broadcast?.run {
            if (this == id) {
                this@PlayerActivity.recommendationExpirationThread?.interrupt()
            }
        }

        // Register as a viewer
        broadcastId = id
        Broadcast.join(broadcastId)

        // Update player source
        changePlayerSource(broadcastId)

        // Close current comm socket
        this.socket?.close(StreamCommunicationListener.NORMAL_CLOSURE_STATUS, "Changed broadcast")

        // Start communication socket with new broadcastId
        startCommunicationSocket()
    }
}