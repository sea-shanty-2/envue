package dk.cs.aau.envue

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import dk.cs.aau.envue.shared.GatewayClient
import dk.cs.aau.envue.type.BroadcastInputType
import dk.cs.aau.envue.type.LocationInputType
import dk.cs.aau.envue.workers.RefreshTokenWorker
import kotlinx.android.synthetic.main.activity_login.*
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {
    private val callbackManager = CallbackManager.Factory.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult?) = this@LoginActivity.onSuccess(result)
            override fun onCancel() = this@LoginActivity.onCancel()
            override fun onError(error: FacebookException?) = this@LoginActivity.onError(error)
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun onSuccess(loginResult: LoginResult?) {

        // remove login button (to avoid interrupts)
        login_button.visibility = View.GONE

        // set a unique work name for periodic token refresh
        val uniqueWorkName = "periodic_token_refresh"

        // build token refresh worker
        val periodicWorkRequest = PeriodicWorkRequest
            .Builder(RefreshTokenWorker::class.java, 1, TimeUnit.HOURS)
            .build()

        // start periodic token refresh worker
        WorkManager
            .getInstance()
            .enqueueUniquePeriodicWork(
                uniqueWorkName,
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicWorkRequest)


        // return to prev activity
        finish()
    }

    fun onCancel() {
        Snackbar.make(container, "Cancelled", Snackbar.LENGTH_SHORT).show()
    }

    fun onError(exception: FacebookException?) {
        exception?.localizedMessage?.let {
            AlertDialog.Builder(this)
                .setMessage(it)
                .create()
                .show()
        }
    }
}