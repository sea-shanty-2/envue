package dk.cs.aau.envue.interceptors

import dk.cs.aau.envue.shared.GatewayClient
import okhttp3.Interceptor
import okhttp3.Response

class AuthenticationInterceptor : Interceptor {
    companion object {
        var token: String? = null
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain
            .request()
            .newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()

        var response = chain.proceed(request)

        /**
         * If request failed, fetch a new authentication token and retry the request
         */
        if (!response.isSuccessful) {

            token = GatewayClient.fetchToken()

            request = chain
                .request()
                .newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()

            response = chain.proceed(request)

            return response
        }


        return response
    }
}