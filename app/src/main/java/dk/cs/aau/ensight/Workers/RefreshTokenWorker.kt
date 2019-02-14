package dk.cs.aau.ensight.Workers

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.app.AlertDialog
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.facebook.AccessToken
import com.facebook.login.LoginManager
import dk.cs.aau.ensight.LoginActivity
import dk.cs.aau.ensight.R


class RefreshTokenWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {


        // validate current access token
        val isValid = AccessToken.isCurrentAccessTokenActive()

        return if (isValid) {
            AccessToken.refreshCurrentAccessTokenAsync()
            Result.success()

        } else {
            AlertDialog.Builder(applicationContext)
                .setMessage(applicationContext.resources.getString(R.string.invalid_access_token))
                .setPositiveButton(R.string.com_facebook_loginview_log_in_button) { _: DialogInterface, _: Int -> run{
                    applicationContext.startActivity(Intent(applicationContext, LoginActivity::class.java))
                }}
                .create()
                .show()
            Result.failure()

        }


    }
}