package dk.cs.aau.envue

import android.os.Bundle
import android.support.text.emoji.EmojiCompat
import android.support.text.emoji.bundled.BundledEmojiCompatConfig
import android.support.v7.app.AppCompatActivity
import android.widget.ListView
import com.google.gson.GsonBuilder
import dk.cs.aau.envue.utility.EmojiIcon
import dk.cs.aau.envue.workers.CategoryListAdapter


abstract class CategorySelectionActivity : AppCompatActivity() {
    private val tag = "CategorySelectionActivity"
    internal var _allEmojis = ArrayList<EmojiIcon>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_category_selection)
        EmojiCompat.init(BundledEmojiCompatConfig(this))

        // Load emojis into the grid view
        loadEmojis(R.raw.limited_emojis, R.id.categorySelectionListView)
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

    /** Creates a one-hot vector of selected emojis */
    internal fun getCategoryVector(selectedEmojis: List<EmojiIcon>): Array<Double> {
        val categoryVector = Array(_allEmojis.size) {i -> 0.0}
        for (emojiIcon in selectedEmojis) {
            val i = _allEmojis.indexOf(emojiIcon)
            categoryVector[i] = 1.0
        }

        return categoryVector
    }

    /** Loads emojis from a JSON file provided by the resource id.
     * Emojis are loaded directly into a list adapter used by the
     * category selection list view. */
    internal fun loadEmojis(resourceId: Int, targetResourceId: Int) {
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
            this.adapter = CategoryListAdapter(this.context, emojiRows)
        }
    }


    /** Returns all the emojis selected by the user.
     * Includes emojis chosen in the grid view as well
     * as emojis searched by the user. */
    internal fun getSelectedCategories(): List<EmojiIcon> =
        (findViewById<ListView>(R.id.categorySelectionListView).adapter as CategoryListAdapter)
            .getAllEmojis()
            .filter {it.isSelected}
            .map {it.getEmoji()}
}
