package dk.cs.aau.envue.shared

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import dk.cs.aau.envue.BroadcastJoinMutation
import dk.cs.aau.envue.BroadcastLeaveMutation

class Broadcast {
    companion object {
        private var id : String? = null

        fun join(id: String, callback: ApolloCall.Callback<BroadcastJoinMutation.Data?>? = null) {
            this.leave()

            val mutation = BroadcastJoinMutation.builder().id(id).build()
            GatewayClient.mutate(mutation).enqueue(object: ApolloCall.Callback<BroadcastJoinMutation.Data?>() {
                override fun onResponse(response: Response<BroadcastJoinMutation.Data?>) {
                    this@Companion.id = id
                    callback?.onResponse(response)
                }

                override fun onFailure(e: ApolloException) {
                    callback?.onFailure(e)
                }
            })
        }

        fun leave(callback: ApolloCall.Callback<BroadcastLeaveMutation.Data?>? = null) {
            val id = this@Companion.id

            if (id.isNullOrBlank()) {
                return
            }

            val mutation = BroadcastLeaveMutation.builder().id(id).build()
            GatewayClient.mutate(mutation).enqueue(object: ApolloCall.Callback<BroadcastLeaveMutation.Data?>() {

                override fun onResponse(response: Response<BroadcastLeaveMutation.Data?>) {
                    this@Companion.id = null
                    callback?.onResponse(response)
                }

                override fun onFailure(e: ApolloException) {
                    callback?.onFailure(e)
                }
            })
        }
    }
}