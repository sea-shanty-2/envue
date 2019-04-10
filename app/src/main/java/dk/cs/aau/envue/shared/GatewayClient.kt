package dk.cs.aau.envue.shared

import com.apollographql.apollo.ApolloClient
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response


class GatewayClient {

    private constructor() {}

    companion object {
        private var okHttpClient = OkHttpClient.Builder().build()

        fun setAuthenticator(token: String) {

            val authenticator = Authenticator { _, response ->
                response.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            }

            okHttpClient = okHttpClient.newBuilder().authenticator(authenticator).build()
        }

        fun addHeader(name: String, value: String) {

            val interceptor = Interceptor { chain ->
                chain.proceed(chain.request().newBuilder().addHeader(name, value).build())
            }

            okHttpClient = okHttpClient.newBuilder().addInterceptor(interceptor).build()
        }

        val apolloClient = ApolloClient.builder()
            .serverUrl("https://envue.me/api")
            .okHttpClient(okHttpClient)
            .build()!!

    }
}