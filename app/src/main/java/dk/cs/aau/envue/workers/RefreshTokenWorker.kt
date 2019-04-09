package dk.cs.aau.envue.workers

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.support.v7.app.AlertDialog
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloCallback
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.facebook.AccessToken
import dk.cs.aau.envue.GatewayAuthenticationQuery
import dk.cs.aau.envue.LoginActivity
import dk.cs.aau.envue.R
import dk.cs.aau.envue.shared.GatewayClient


class RefreshTokenWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {

        // validate current access token
        val isValid = AccessToken.isCurrentAccessTokenActive()

        return if (isValid) {
            AccessToken.refreshCurrentAccessTokenAsync()

            var query = GatewayAuthenticationQuery
                .builder()
                .token(AccessToken.getCurrentAccessToken().token)
                .build()

            GatewayClient.apolloClient.query(query).enqueue(object: ApolloCall.Callback<GatewayAuthenticationQuery.Data>() {
                override fun onResponse(response: Response<GatewayAuthenticationQuery.Data>) {
                    val token = response.data()?.authenticate()?.facebook()

                    if (!token.isNullOrEmpty()) {
                        GatewayClient.setAuthenticator(token)
                    }
                }

                override fun onFailure(e: ApolloException) {
                    Result.failure()
                }
            })

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