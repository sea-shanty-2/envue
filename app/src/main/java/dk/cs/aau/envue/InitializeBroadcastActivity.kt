package dk.cs.aau.envue

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.text.emoji.EmojiCompat
import android.support.text.emoji.bundled.BundledEmojiCompatConfig
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.EditText
import android.widget.ListView
import com.apollographql.apollo.ApolloClient
import com.google.gson.GsonBuilder
import dk.cs.aau.envue.utility.EmojiIcon
import dk.cs.aau.envue.workers.BroadcastCategoryListAdapter
import kotlinx.android.synthetic.main.activity_initialize_broadcast.*
import okhttp3.OkHttpClient
import android.util.Log
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException


class InitializeBroadcastActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initialize_broadcast)
        EmojiCompat.init(BundledEmojiCompatConfig(this))

        startBroadcastButton.setOnClickListener { view ->
            startBroadcast(view)
        }

        emojiSearchField.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                emojiSearchField.hint = ""
            } else {
                emojiSearchField.hint = resources.getString(R.string.search_for_emoji_text)
            }
        }

        loadEmojis(R.raw.emojis, R.id.broadcastCategoryListView)
        theFunThing()
    }

    /** Loads emojis from a JSON file provided by the resource id.
     * Emojis are loaded directly into a list adapter used by the
     * broadcast category list view. */
    fun loadEmojis(resourceId: Int, targetResourceId: Int) {
        val allEmojis = GsonBuilder().create().fromJson(
            resources.openRawResource(resourceId).bufferedReader(),
            Array<EmojiIcon>::class.java
        )

        // Wrap all unicodes in EmojiIcon objects in rows of 5
        var emojiRows = ArrayList<ArrayList<EmojiIcon>>()
        for (i in 0 until allEmojis.size) {
            if (i % 5 == 0) {
                emojiRows.add(ArrayList())
            }
            emojiRows.last().add(allEmojis[i])
        }

        // Provide an item adapter to the ListView
        findViewById<ListView>(targetResourceId).apply {
            this.adapter = BroadcastCategoryListAdapter(this.context, emojiRows)
        }
    }

    /** Returns all the emojis selected by the user.
     * Includes emojis chosen in the grid view as well
     * as emojis searched by the user. */
    private fun getSelectedCategories(): List<String> {
        val gridViewEmojis = (findViewById<ListView>(R.id.broadcastCategoryListView).adapter as BroadcastCategoryListAdapter)
            .getAllEmojis()
            .filter {it.isSelected}
            .map {it.getEmoji()}

        val searchedEmojis = (findViewById<EditText>(R.id.emojiSearchField)).text.toString().run {
            emojiStringToArray(this)
        }
        return if (searchedEmojis.isEmpty()) {
            gridViewEmojis.map {it.char}
        } else {
            gridViewEmojis.map {it.char}.plus(searchedEmojis)
        }
    }

    /** Starts the broadcast after processing selected categories
     * if all checks pass. */
    private fun startBroadcast(view: View) {
        if (!isOnlyEmojis(emojiSearchField.text.toString())) {
            Snackbar.make(
                view, resources.getString(R.string.illegal_char_in_search_for_emoji_field), Snackbar.LENGTH_LONG).show()
            return
        }

        val selectedEmojis = getSelectedCategories()  // TODO: Do something with this
        if (selectedEmojis.isEmpty()) {
            Snackbar.make(
                view, resources.getString(R.string.no_categories_chosen), Snackbar.LENGTH_LONG).show()
            return
        }

        Snackbar.make(view, " and ".join(selectedEmojis), Snackbar.LENGTH_LONG).show()  // For testing
        //startActivity(Intent(this, BroadcastActivity::class.java))
    }

    /** Returns true if the string consists of only emojis or is empty, otherwise false. */
    private fun isOnlyEmojis(str: String): Boolean {  // LMFAO REGEX
        return str.matches(Regex("(?:[\uD83C\uDF00-\uD83D\uDDFF]|[\uD83E\uDD00-\uD83E\uDDFF]|" +
                "[\uD83D\uDE00-\uD83D\uDE4F]|[\uD83D\uDE80-\uD83D\uDEFF]|" +
                "[\u2600-\u26FF]\uFE0F?|[\u2700-\u27BF]\uFE0F?|\u24C2\uFE0F?|" +
                "[\uD83C\uDDE6-\uD83C\uDDFF]{1,2}|" +
                "[\uD83C\uDD70\uD83C\uDD71\uD83C\uDD7E\uD83C\uDD7F\uD83C\uDD8E\uD83C\uDD91-\uD83C\uDD9A]\uFE0F?|" +
                "[\u0023\u002A\u0030-\u0039]\uFE0F?\u20E3|[\u2194-\u2199\u21A9-\u21AA]\uFE0F?|[\u2B05-\u2B07\u2B1B\u2B1C\u2B50\u2B55]\uFE0F?|" +
                "[\u2934\u2935]\uFE0F?|[\u3030\u303D]\uFE0F?|[\u3297\u3299]\uFE0F?|" +
                "[\uD83C\uDE01\uD83C\uDE02\uD83C\uDE1A\uD83C\uDE2F\uD83C\uDE32-\uD83C\uDE3A\uD83C\uDE50\uD83C\uDE51]\uFE0F?|" +
                "[\u203C\u2049]\uFE0F?|[\u25AA\u25AB\u25B6\u25C0\u25FB-\u25FE]\uFE0F?|" +
                "[\u00A9\u00AE]\uFE0F?|[\u2122\u2139]\uFE0F?|\uD83C\uDC04\uFE0F?|\uD83C\uDCCF\uFE0F?|" +
                "[\u231A\u231B\u2328\u23CF\u23E9-\u23F3\u23F8-\u23FA]\uFE0F?)+")) || str.isEmpty()
    }

    /** Separates a string of (only!) emojis into an array list of individual emojis. */
    private fun emojiStringToArray(emojiString: String): ArrayList<String> {
        val arr = ArrayList<String>()
        var accumulator = ""
        for (i in 0 until emojiString.length) {
            if (accumulator.isNotEmpty()) {
                accumulator += emojiString[i]
                arr.add(accumulator.toString())
                accumulator = ""
            } else {
                accumulator = emojiString[i].toString()
                if (isOnlyEmojis(accumulator)) {
                    arr.add(accumulator.toString())
                    accumulator = ""
                }
            }
        }

        return arr
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

    fun theFunThing() {
        val baseUrl = resources.getString(R.string.graphql_base_url)
        val okHttpClient = OkHttpClient.Builder().build()
        val apolloClient = ApolloClient.builder()
            .serverUrl(baseUrl)
            .okHttpClient(okHttpClient)
            .build()

        val activeQuery = AccountsQuery.builder().first(5).build()
        val TAG = "InitializeBroadcast"
        val testCall: ApolloCall<AccountsQuery.Data> = apolloClient.query(activeQuery)
        var data: AccountsQuery.Data? = null
        testCall.enqueue(object : ApolloCall.Callback<AccountsQuery.Data>() {
            override fun onResponse(response: Response<AccountsQuery.Data>){
                Log.d(TAG, "Did shit")
                var d = response.data()
                this@InitializeBroadcastActivity.runOnUiThread {
                    findViewById<EditText>(R.id.emojiSearchField).hint = d.toString()
                }
            }
            override fun onFailure(e: ApolloException){
                Log.d(TAG, e.message)
            }
        })
    }
}
