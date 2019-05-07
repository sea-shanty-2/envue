package dk.cs.aau.envue.workers

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.facebook.AccessToken
import com.facebook.FacebookException
import com.google.common.util.concurrent.ListenableFuture
import androidx.work.*
import androidx.work.impl.utils.futures.SettableFuture
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloCallback
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import dk.cs.aau.envue.GatewayAuthenticationQuery
import dk.cs.aau.envue.interceptors.AuthenticationInterceptor
import dk.cs.aau.envue.shared.GatewayClient

@SuppressLint("RestrictedApi")
class RefreshTokenWorker(context: Context, workerParams: WorkerParameters) : ListenableWorker(context, workerParams) {

    override fun startWork(): ListenableFuture<Result> {
        Log.d("TOKEN_REFRESH_WORKER", "STARTED")
        var future = SettableFuture.create<Result>()
        AccessToken.refreshCurrentAccessTokenAsync(facebookAccessTokenRefreshCallback(future))
        return future
    }

    private fun facebookAccessTokenRefreshCallback(future: SettableFuture<Result>):
            AccessToken.AccessTokenRefreshCallback {

        return object : AccessToken.AccessTokenRefreshCallback {

            override fun OnTokenRefreshed(accessToken: AccessToken?) {
                Log.d("FACEBOOK_TOKEN_REFRESH", "SUCCEEDED")
                future.set(Result.success())
            }

            override fun OnTokenRefreshFailed(exception: FacebookException?) {
                Log.d("FACEBOOK_TOKEN_REFRESH", "FAILED")
                future.set(Result.failure())
            }

        }
    }
}