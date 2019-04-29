package dk.cs.aau.envue.shared

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.ApolloMutationCall
import com.apollographql.apollo.ApolloQueryCall
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response


class GatewayClient {

    private constructor() {}

    companion object {
        private var okHttpClient = OkHttpClient.Builder().build()
        private val apolloClient = ApolloClient.builder()
            .serverUrl("http://172.25.11.190")  // Change to http://172.25.11.190 if using staging
            .okHttpClient(okHttpClient)
            .build()!!

        fun setAuthenticator(token: String) {

            val authenticator = Authenticator { _, response ->
                response.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            }

            okHttpClient = okHttpClient.newBuilder().authenticator(authenticator).build()
        }

        fun <D: Operation.Data, T: Any, V: Operation.Variables>query(query: Query<D, T, V>): ApolloQueryCall<T> {
            return apolloClient.query(query)
        }

        fun <D: Operation.Data, T: Any, V: Operation.Variables>mutate(mutation: Mutation<D, T, V>): ApolloMutationCall<T> {
            return apolloClient.mutate(mutation)
        }
    }
}