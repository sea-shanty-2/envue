package dk.cs.aau.envue.shared

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.ApolloMutationCall
import com.apollographql.apollo.ApolloQueryCall
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.facebook.AccessToken
import com.facebook.Profile
import dk.cs.aau.envue.interceptors.AuthenticationInterceptor
import dk.cs.aau.envue.interceptors.LoggingInterceptor
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import javax.xml.datatype.DatatypeConstants.SECONDS
import okhttp3.Route
import java.util.concurrent.TimeUnit


class GatewayClient {

    private constructor() {}

    companion object {

        private fun okHttpClient() : OkHttpClient.Builder {

            return OkHttpClient.Builder()
                .addInterceptor(AuthenticationInterceptor())
                .addInterceptor(LoggingInterceptor())
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
        }

        private fun apolloClient() : ApolloClient.Builder {
            return ApolloClient.builder()
                .serverUrl("http://172.25.11.190")  // Either "https://envue.me/api" or "http://172.25.11.190" (staging)
                .okHttpClient(okHttpClient().build())
        }

        fun setAuthenticationToken(token: String) {
            AuthenticationInterceptor.token = token
        }


        fun <D: Operation.Data, T: Any, V: Operation.Variables>query(query: Query<D, T, V>): ApolloQueryCall<T> {
            return apolloClient().build().query(query)
        }

        fun <D: Operation.Data, T: Any, V: Operation.Variables>mutate(mutation: Mutation<D, T, V>): ApolloMutationCall<T> {
            return apolloClient().build().mutate(mutation)
        }
    }
}