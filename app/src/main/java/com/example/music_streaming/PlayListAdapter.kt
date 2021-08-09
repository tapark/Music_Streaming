package com.example.music_streaming

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.Resource

class PlayListAdapter(private val onClicked: (MusicModel) -> Unit): ListAdapter<MusicModel, PlayListAdapter.ViewHolder>(diffUtil) {

    inner class ViewHolder(private val view: View): RecyclerView.ViewHolder(view) {

        fun bind(musicModel: MusicModel) {
            val trackTextView = view.findViewById<TextView>(R.id.itemTrackTextView)
            val artistTextView = view.findViewById<TextView>(R.id.itemArtistTextView)
            val coverImageView = view.findViewById<ImageView>(R.id.itemCoverImageView)
            trackTextView.text = musicModel.track
            artistTextView.text = musicModel.artist
            Glide.with(coverImageView)
                .load(musicModel.coverUrl)
                .into(coverImageView)

            if (musicModel.isPlaying) {
                itemView.setBackgroundColor(Color.GRAY)
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT)
            }

            itemView.setOnClickListener {
                onClicked(musicModel)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.item_music, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    companion object {
        val diffUtil = object: DiffUtil.ItemCallback<MusicModel>() {
            override fun areItemsTheSame(oldItem: MusicModel, newItem: MusicModel): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: MusicModel, newItem: MusicModel): Boolean {
                return oldItem == newItem
            }

        }
    }
}