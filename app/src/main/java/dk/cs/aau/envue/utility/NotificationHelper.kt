package dk.cs.aau.envue.utility

import android.content.Context
import android.provider.Settings.System.getString
import android.util.Log
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessaging
import dk.cs.aau.envue.ProfileActivity
import dk.cs.aau.envue.R

class NotificationHelper{

    fun subscribeCategory(topic: String, baseContext: Context){
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                var msg = getString(R.string.msg_subscribed)
                if (!task.isSuccessful) {
                    msg = getString(R.string.msg_subscribe_failed)
                }
                Log.d(ProfileActivity.TAG, msg)
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            }
    }

    fun unsubscribeCategory(topic: String){
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener { task ->
                var msg = getString(R.string.msg_unsubscribed)
                if (!task.isSuccessful) {
                    msg = getString(R.string.msg_unsubscribe_failed)
                }
                Log.d(ProfileActivity.TAG, msg)
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            }
    }
}