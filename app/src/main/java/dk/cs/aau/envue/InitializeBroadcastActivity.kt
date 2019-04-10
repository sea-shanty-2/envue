package dk.cs.aau.envue

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.text.emoji.EmojiCompat
import android.support.text.emoji.bundled.BundledEmojiCompatConfig
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ListView
import com.google.gson.GsonBuilder
import dk.cs.aau.envue.utility.EmojiIcon
import dk.cs.aau.envue.workers.BroadcastCategoryListAdapter
import kotlinx.android.synthetic.main.activity_initialize_broadcast.*
import android.util.Log
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import dk.cs.aau.envue.shared.GatewayClient
import dk.cs.aau.envue.type.BroadcastInputType
import dk.cs.aau.envue.type.LocationInputType


class InitializeBroadcastActivity : AppCompatActivity() {

    var _allEmojis = ArrayList<EmojiIcon>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initialize_broadcast)
        EmojiCompat.init(BundledEmojiCompatConfig(this))

        startBroadcastButton.setOnClickListener { view ->
            // TODO: Fetch the exact geo-position
            createBroadcaster(100.0, 20.0, category=getCategoryVector(getSelectedCategories()))
            startBroadcast(view)
        }

        loadEmojis(R.raw.emojis, R.id.broadcastCategoryListView)
    }

    /** Loads emojis from a JSON file provided by the resource id.
     * Emojis are loaded directly into a list adapter used by the
     * broadcast category list view. */
    fun loadEmojis(resourceId: Int, targetResourceId: Int) {

        // Load all emojis into local storage
        _allEmojis = _allEmojis.plus(GsonBuilder().create().fromJson(
            resources.openRawResource(resourceId).bufferedReader(),
            Array<EmojiIcon>::class.java
        )) as ArrayList


        // Wrap all unicodes in EmojiIcon objects in rows of 5
        var emojiRows = ArrayList<ArrayList<EmojiIcon>>()
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

    /** Returns all the emojis selected by the user.
     * Includes emojis chosen in the grid view as well
     * as emojis searched by the user. */
    private fun getSelectedCategories(): List<EmojiIcon> {
        val gridViewEmojis = (findViewById<ListView>(R.id.broadcastCategoryListView).adapter as BroadcastCategoryListAdapter)
            .getAllEmojis()
            .filter {it.isSelected}
            .map {it.getEmoji()}

        return gridViewEmojis
    }

    /** Starts the broadcast after processing selected categories
     * if all checks pass. */
    private fun startBroadcast(view: View) {

        val selectedEmojiIcons = getSelectedCategories()  // TODO: Do something with this
        if (selectedEmojiIcons.isEmpty()) {
            Snackbar.make(
                view, resources.getString(R.string.no_categories_chosen), Snackbar.LENGTH_LONG).show()
            return
        }

        startActivity(Intent(this, BroadcastActivity::class.java))
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

    /** Creates a broadcaster object and stores it in stable storage
     * on the Envue database. */
    private fun createBroadcaster(latitude: Double, longitude: Double, category: DoubleArray) {

        val location = LocationInputType.builder().latitude(latitude).longitude(longitude).build()
        val broadcast = BroadcastInputType.builder().categories(category.toList()).location(location).build()
        val broadcastCreateMutation = BroadcastCreateMutation.builder().broadcast(broadcast).build()
        GatewayClient.mutate(broadcastCreateMutation).enqueue(object: ApolloCall.Callback<BroadcastCreateMutation.Data>() {

            override fun onResponse(response: Response<BroadcastCreateMutation.Data>) {
                Log.d("Test", response.data()?.broadcasts()?.create())
            }

            override fun onFailure(e: ApolloException) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        })
    }

    private fun getCategoryVector(selectedEmojis: List<EmojiIcon>): DoubleArray {
        val categoryVector = _allEmojis.map { 0.0 }.toDoubleArray()
        for (emojiIcon in selectedEmojis) {
            val i = _allEmojis.indexOf(emojiIcon)
            categoryVector[i] = 1.0
        }

        return categoryVector
    }
}
