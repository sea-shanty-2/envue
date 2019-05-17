package dk.cs.aau.envue

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.View
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.facebook.login.LoginManager
import com.google.gson.GsonBuilder
import dk.cs.aau.envue.shared.GatewayClient
import dk.cs.aau.envue.utility.EmojiIcon
import kotlinx.android.synthetic.main.activity_profile.*
import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import dk.cs.aau.envue.type.AccountUpdateInputType


class ProfileFragment : Fragment() {
    companion object {
        internal val SET_INTERESTS_REQUEST = 0
        internal val TAG = ProfileFragment::class.java.simpleName ?: "ProfileFragment"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)

        val view = inflater.inflate(R.layout.activity_profile, container, false)

        // Register button listeners
        view.findViewById<Button>(R.id.logOutButton)?.setOnClickListener { this.logOut() }
        view.findViewById<Button>(R.id.changeDisplayName)?.setOnClickListener { this.openDialog() }
        view.findViewById<Button>(R.id.interestsButton)?.setOnClickListener { this.onChangeInterests() }

        return view
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
                response.data()?.accounts()?.me()?.let { onProfileFetch(it) }
            }

            override fun onFailure(e: ApolloException) = onProfileFetchFailure(e)

        })
    }

    private fun onProfileFetch(profile: ProfileQuery.Me) {
        activity?.runOnUiThread {
            profile.categories()?.let { oneHotVectorToEmoji(it) }

            profileNameView.text = profile.displayName()
            container.visibility = View.VISIBLE
        }
    }

    private fun onProfileFetchFailure(e: ApolloException) {
        activity?.run {
            runOnUiThread {
                AlertDialog
                    .Builder(this)
                    .setTitle(e.message)
                    .setMessage(
                        "There was an issue fetching your profile data." +
                                "Check your internet connection or you can try relogging.")
                    .setNegativeButton("log out") { _, _ ->  logOut() }
                    .setPositiveButton("return") { _, _ -> activity?.finish() }
                    .create()
                    .show()
            }
        }
    }

    private fun logOut() {
        LoginManager.getInstance().logOut()
        startActivity(Intent(this.activity, LoginActivity::class.java))
    }

    private fun onChangeInterests() {
        val curInt: CharSequence = currentInterestsView.text
        val intent = Intent(this.activity, InterestsActivity::class.java)
        intent.putExtra(resources.getString(R.string.current_interests_key), curInt)
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

    private fun oneHotVectorToEmoji(categories: List<Double>) {
        var allEmojis = ArrayList<EmojiIcon>()
        allEmojis = allEmojis.plus(
            GsonBuilder().create().fromJson(
                resources.openRawResource(R.raw.limited_emojis).bufferedReader(),
                Array<EmojiIcon>::class.java
            )
        ) as ArrayList


        var temp = ""
        for (i in categories.indices) {
            if (categories[i] == 1.0) {
                temp += allEmojis[i].char
            }
        }

        currentInterestsView.text = temp
    }

    private fun openDialog() {
        val input = EditText(this.activity).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(profileNameView?.text?.toString())
        }

        context?.let {
            AlertDialog.Builder(it).apply {
                setTitle("Change display name")
                setView(input)
                setPositiveButton("OK") { _, _ -> acceptDialog(input)}
                setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

                show()
            }
        }
    }

    private fun acceptDialog(input : EditText) {
        val temp = AccountUpdateInputType.builder().displayName(input.text.toString()).build()
        val changeDisplayName = ProfileUpdateMutation.builder().account(temp).build()

        GatewayClient.mutate(changeDisplayName).enqueue(object: ApolloCall.Callback<ProfileUpdateMutation.Data>(){
            override fun onFailure(e: ApolloException) {
            }

            override fun onResponse(response: Response<ProfileUpdateMutation.Data>) {
                activity?.runOnUiThread{
                    profileNameView.text = input.text.toString()
                }
            }
        })
    }

}