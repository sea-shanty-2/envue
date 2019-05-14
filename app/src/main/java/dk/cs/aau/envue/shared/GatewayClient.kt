package dk.cs.aau.envue.shared

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.ApolloMutationCall
import com.apollographql.apollo.ApolloQueryCall
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.exception.ApolloHttpException
import com.facebook.AccessToken
import dk.cs.aau.envue.GatewayAuthenticationQuery
import dk.cs.aau.envue.interceptors.AuthenticationInterceptor
import dk.cs.aau.envue.interceptors.LoggingInterceptor
import okhttp3.OkHttpClient


class GatewayClient private constructor() {
    companion object {
        private const val endpoint = "https://envue.me/api"

        private fun okHttpClient() : OkHttpClient.Builder {
            return OkHttpClient.Builder()
                .addInterceptor(AuthenticationInterceptor())
                .addInterceptor(LoggingInterceptor())
        }

        private fun apolloClient() : ApolloClient.Builder {
            return ApolloClient.builder()
                .serverUrl(endpoint)
                .okHttpClient(okHttpClient().build())
        }

        fun fetchToken(): String? {
            // Build new client instances without interceptors
            val httpClient = OkHttpClient.Builder().build()
            val apolloClient = ApolloClient.builder().serverUrl(endpoint).okHttpClient(httpClient).build()

            if (AccessToken.isCurrentAccessTokenActive()) {
                val query = GatewayAuthenticationQuery
                    .builder()
                    .token(AccessToken.getCurrentAccessToken().token)
                    .build()

                var result : String?
                // Perform a blocking authentication request
                try {
                    result = apolloClient.query(query).execute().data()?.authenticate()?.facebook()
                }
                catch(e: ApolloHttpException) {
                    result = null
                }

                return result


            }

            return null
        }

        fun <D: Operation.Data, T: Any, V: Operation.Variables>query(query: Query<D, T, V>): ApolloQueryCall<T> {
            return apolloClient().build().query(query)
        }

        fun <D: Operation.Data, T: Any, V: Operation.Variables>mutate(mutation: Mutation<D, T, V>): ApolloMutationCall<T> {
            return apolloClient().build().mutate(mutation)
        }
    }
}