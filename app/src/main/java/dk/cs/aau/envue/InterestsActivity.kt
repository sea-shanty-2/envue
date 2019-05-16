package dk.cs.aau.envue

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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
//import dk.cs.aau.envue.workers.BroadcastCategoryListAdapter
import kotlinx.android.synthetic.main.activity_category_selection.*
//import kotlinx.android.synthetic.main.activity_initialize_broadcast.*


class InterestsActivity : CategorySelectionActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Modify layout
        this.categorySelectionHeader.text = resources.getString(R.string.interests_dialog_title)
        this.categorySelectionButtonText.text = resources.getString(R.string.interests_dialog_possitive)

        categorySelectionButton.setOnClickListener { view ->
            setInterests(view)
        }

        val currentInterests: CharSequence? = intent.getCharSequenceExtra(resources.getString(R.string.current_interests_key))
        if (!currentInterests.isNullOrBlank())
            selectCurrentInterests(currentInterests)
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
            }

            override fun onResponse(response: Response<ProfileUpdateMutation.Data>) {
            }
        })
    }

    /** Pre-selects the users current interests */
    private fun selectCurrentInterests(selected: CharSequence){
        // TODO: Implement this
    }
}
