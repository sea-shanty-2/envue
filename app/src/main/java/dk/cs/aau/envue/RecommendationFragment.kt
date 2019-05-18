package dk.cs.aau.envue

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.squareup.picasso.Picasso

private const val ARG_BROADCAST = "broadcast"

class RecommendationFragment : Fragment() {
    public var broadcast: String? = null
    private var listener: OnRecommendationFragmentListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            broadcast = it.getString(ARG_BROADCAST)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val layout = inflater.inflate(R.layout.fragment_recommendation, container, false)

        // Load thumbnail
        broadcast?.let { broadcast ->
            layout.findViewById<ImageView>(R.id.recommendation_image)?.let {
                Picasso
                    .get()
                    .load("https://envue.me/relay/$broadcast/thumbnail")
                    .placeholder(R.drawable.ic_live_tv_48dp)
                    .error(R.drawable.ic_live_tv_48dp)
                    .into(it)
            }
        }

        // Attach click listener to thumbnail
        layout.findViewById<ImageView>(R.id.recommendation_image)?.setOnClickListener {
            broadcast?.let { listener?.onRecommendationAccepted(it) }
        }

        return layout
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnRecommendationFragmentListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnRecommendationFragmentListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnRecommendationFragmentListener {
        fun onRecommendationAccepted(broadcastId: String)
        fun onRecommendationDismissed(broadcastId: String)
    }

    companion object {
        @JvmStatic
        fun newInstance(broadcast: String): RecommendationFragment =
            RecommendationFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_BROADCAST, broadcast)
                }
            }
    }
}
