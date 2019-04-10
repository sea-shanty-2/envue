package dk.cs.aau.envue.chat

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import dk.cs.aau.envue.R

class ReactionListAdapter(private val emojiList: List<String>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        return (holder as EmojiImageHolder).bind(emojiList[position])
    }

    override fun getItemCount(): Int {
        return emojiList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return EmojiImageHolder(LayoutInflater.from(parent.context).inflate(R.layout.fragment_reaction, parent,false))
    }
}