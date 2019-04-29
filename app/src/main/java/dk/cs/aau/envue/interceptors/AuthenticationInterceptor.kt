package dk.cs.aau.envue.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody

class AuthenticationInterceptor : Interceptor {
    companion object {
        var token = ""
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
        return chain.proceed(request)
    }
}