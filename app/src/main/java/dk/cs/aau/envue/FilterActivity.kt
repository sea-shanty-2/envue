package dk.cs.aau.envue

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_category_selection.*

class FilterActivity : CategorySelectionActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Modify layout
        this.categorySelectionHeader.text = resources.getString(R.string.title_activity_filter)
        this.categorySelectionButtonText.text = resources.getString(R.string.accept_filters_button_text)

        // Add button listener
        categorySelectionButtonText.setOnClickListener { view ->
            onSetFilters()
        }

    }

    /** Returns the currently selected filters to the initiating activity */
    private fun onSetFilters() {
        val data = Intent()
            .putExtra(  resources.getString(R.string.filter_response_key),
                        getCategoryVector(getSelectedCategories()))

        setResult(Activity.RESULT_OK, data)
        finish()
    }
}
