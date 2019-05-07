package dk.cs.aau.envue

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_initialize_broadcast.*

class InterestsActivity : CategorySelectionActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Modify layout
        this.initializeBroadcastHeader.text = resources.getString(R.string.interests_dialog_title)
        this.startBroadcastButtonText.text = resources.getString(R.string.interests_dialog_possitive)

        startBroadcastButton.setOnClickListener { view ->
            setInterests(view)
        }

        val currentInterests: CharSequence? = intent.getCharSequenceExtra(resources.getString(R.string.current_interests_key))
        if (!currentInterests.isNullOrBlank())
            selectCurrentInterests(currentInterests)
    }

    /** Starts the broadcast after processing selected categories
     * if all checks pass. */
    private fun setInterests(view: View) {
        val selectedEmojis = getSelectedCategories()
        val data = Intent().putExtra(resources.getString(R.string.interests_response_key), "".join(selectedEmojis))
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    /** Pre-selects the users current interests */
    private fun selectCurrentInterests(selected: CharSequence){
        // TODO: Implement this
    }
}
