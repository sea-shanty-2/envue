package dk.cs.aau.envue

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import android.util.Log
import android.widget.ListView
import android.widget.Toast
import com.facebook.Profile
import com.facebook.login.LoginManager
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.GsonBuilder
import com.squareup.picasso.Picasso
import dk.cs.aau.envue.shared.GatewayClient
import kotlinx.android.synthetic.main.activity_profile.*
import dk.cs.aau.envue.transformers.CircleTransform

class ProfileActivity : AppCompatActivity() {
    companion object {
        internal val SET_INTERESTS_REQUEST = 0
        internal val TAG = ProfileActivity::class.java.simpleName ?: "ProfileActivity"
        internal val SENDER_ID = 3317625636
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // register button listeners
        logOutButton.setOnClickListener { this.logOut() }
        interestsButton.setOnClickListener { this.onChangeInterests() }
    }

    override fun onStart() {
        super.onStart()
        container.visibility = View.INVISIBLE
        fetchProfile()
    }

    private fun fetchProfile() {
        val profileQuery = ProfileQuery.builder().build()

        GatewayClient.query(profileQuery).enqueue(object : ApolloCall.Callback<ProfileQuery.Data>() {
            override fun onResponse(response: Response<ProfileQuery.Data>) {
                val profile = response.data()?.accounts()?.me()

                if (profile != null) {
                    onProfileFetch(profile!!)
                }
                else {
                    TODO("Handle null response")
                }
            }

            override fun onFailure(e: ApolloException) = onProfileFetchFailure(e)

        })
    }

    private fun onProfileFetch(profile: ProfileQuery.Me) {
        runOnUiThread {
            profileNameView.text = profile.displayName()
            container.visibility = View.VISIBLE
        }
    }

    private fun onProfileFetchFailure(e: ApolloException) {
        runOnUiThread {

            AlertDialog
                .Builder(this)
                .setTitle(e.message)
                .setMessage(
                    "There was an issue with fetching your profile data." +
                    "To resolve the issue, you can try relogging.")
                .setNegativeButton("log out") { _, _ ->  logOut() }
                .setPositiveButton("return") { _, _ -> finish() }
                .create()
                .show()
        }
    }

    private fun logOut() {
        LoginManager.getInstance().logOut()
        startActivity(Intent(this, LoginActivity::class.java))
    }

    private fun onChangeInterests() {
        val curint: CharSequence = currentInterestsView.text
        val intent = Intent(this, InterestsActivity::class.java)
        intent.putExtra(resources.getString(R.string.current_interests_key), curint)
        startActivityForResult(intent, SET_INTERESTS_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            SET_INTERESTS_REQUEST ->
                if (resultCode == Activity.RESULT_OK) {
                    currentInterestsView.text = data?.getStringExtra(resources.getString(R.string.interests_response_key))
                }
        }
    }

}