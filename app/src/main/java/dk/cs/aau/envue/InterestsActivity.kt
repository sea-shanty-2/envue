package dk.cs.aau.envue

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.text.emoji.EmojiCompat
import android.support.text.emoji.bundled.BundledEmojiCompatConfig
import android.support.v7.app.AppCompatActivity;
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.GsonBuilder
import dk.cs.aau.envue.shared.GatewayClient
import dk.cs.aau.envue.type.AccountUpdateInputType
import dk.cs.aau.envue.utility.EmojiIcon
import dk.cs.aau.envue.workers.BroadcastCategoryListAdapter
import kotlinx.android.synthetic.main.activity_initialize_broadcast.*


import kotlinx.android.synthetic.main.activity_interests.*
import okhttp3.OkHttpClient

class InterestsActivity : AppCompatActivity() {

    private var _allEmojis = ArrayList<EmojiIcon>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initialize_broadcast)
        // Modify layout
        this.initializeBroadcastHeader.text = resources.getString(R.string.interests_dialog_title)
        this.startBroadcastButtonText.text = resources.getString(R.string.interests_dialog_possitive)

        EmojiCompat.init(BundledEmojiCompatConfig(this))

        startBroadcastButton.setOnClickListener { view ->
            setInterests(view)
        }

        val currentInterests: CharSequence? = intent.getCharSequenceExtra(resources.getString(R.string.current_interests_key))
        if (!currentInterests.isNullOrBlank())
            selectCurrentInterests(currentInterests)

        loadEmojis(R.raw.limited_emojis, R.id.broadcastCategoryListView)

    }

    /** Starts the broadcast after processing selected categories
     * if all checks pass. */
    private fun setInterests(view: View) {
        val selectedEmojis = getSelectedCategories()  // TODO: Do something with this

        //Snackbar.make(view, " and ".join(selectedEmojis), Snackbar.LENGTH_LONG).show()  // For testing
        val getCategories = getCategoryVector(selectedEmojis)
        // Subscribe to new interests and unsubscribe from old.
        updateInterests(getCategories)
        updateSubscriptions(getCategories)

        val data = Intent().putExtra(resources.getString(R.string.interests_response_key), "".join(selectedEmojis.map {it.char}))
        //startActivity(Intent(this, BroadcastActivity::class.java))
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    private fun updateSubscriptions(topics: Array<Double>){
        for (i in 0 until topics.size)
        {
            val topic = "Category$i"
            if (topics[i] == 1.0)
                FirebaseMessaging.getInstance().subscribeToTopic(topic)
                    .addOnCompleteListener { task ->
                        var msg = getString(R.string.msg_subscribed)
                        if (!task.isSuccessful) {
                            msg = getString(R.string.msg_subscribe_failed)
                        }
                        Log.d(ProfileActivity.TAG, msg)
                    }
            else
                FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                    .addOnCompleteListener { task ->
                        var msg = getString(R.string.msg_unsubscribed)
                        if (!task.isSuccessful) {
                            msg = getString(R.string.msg_unsubscribe_failed)
                        }
                        Log.d(ProfileActivity.TAG, msg)
                    }
        }
    }

    private fun updateInterests (categories : Array<Double>) {
        val temp = AccountUpdateInputType.builder().categories(categories.asList()).build()
        val updateCategories = ProfileUpdateMutation.builder().account(temp).build()

        GatewayClient.mutate(updateCategories).enqueue(object: ApolloCall.Callback<ProfileUpdateMutation.Data>(){
            override fun onFailure(e: ApolloException) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onResponse(response: Response<ProfileUpdateMutation.Data>) {

            }

        })
    }
    /** Creates a one-hot vector of selected emojis */
    private fun getCategoryVector(selectedEmojis: List<EmojiIcon>): Array<Double> {
        val categoryVector = Array(_allEmojis.size) {i -> 0.0}
        for (emojiIcon in selectedEmojis) {
            val i = _allEmojis.indexOf(emojiIcon)
            categoryVector[i] = 1.0
        }

        return categoryVector
    }


    /** Returns all the emojis selected by the user.
     * Includes emojis chosen in the grid view as well
     * as emojis searched by the user. */
    private fun getSelectedCategories(): List<EmojiIcon> =
        (findViewById<ListView>(R.id.broadcastCategoryListView).adapter as BroadcastCategoryListAdapter)
            .getAllEmojis()
            .filter {it.isSelected}
            .map {it.getEmoji()}

    /** Pre-selects the users current interests */
    private fun selectCurrentInterests(selected: CharSequence){
        // TODO: Implement this
    }

    /** Loads emojis from a JSON file provided by the resource id.
     * Emojis are loaded directly into a list adapter used by the
     * broadcast category list view. */
    private fun loadEmojis(resourceId: Int, targetResourceId: Int) {

        // Load all emojis into local storage
        _allEmojis = _allEmojis.plus(GsonBuilder().create().fromJson(
            resources.openRawResource(resourceId).bufferedReader(),
            Array<EmojiIcon>::class.java
        )) as ArrayList


        // Wrap all unicodes in EmojiIcon objects in rows of 5
        val emojiRows = ArrayList<ArrayList<EmojiIcon>>()
        for (i in 0 until _allEmojis.size) {
            if (i % 5 == 0) {
                emojiRows.add(ArrayList())
            }
            emojiRows.last().add(_allEmojis[i])
        }

        // Provide an item adapter to the ListView
        findViewById<ListView>(targetResourceId).apply {
            this.adapter = BroadcastCategoryListAdapter(this.context, emojiRows)
        }
    }


    /** Returns a string of elements in the provided list joined by the origin string. */
    fun <T> String.join(other: Iterable<T>): String {
        var joined = ""
        other as Collection<T>
        if (other.size < 2) {
            return if (other.isNotEmpty()) other.first().toString() else joined
        }
        for (element in other) {
            joined += "${element.toString()}${this}"
        }

        return joined.removeSuffix(this)
    }

}
