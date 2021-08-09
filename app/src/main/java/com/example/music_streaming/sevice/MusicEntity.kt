package com.example.music_streaming.sevice

import com.google.gson.annotations.SerializedName

data class MusicEntity(
    @SerializedName("track")        val track: String,
    @SerializedName("artist")       val artist: String,
    @SerializedName("streamUrl")    val streamUrl: String,
    @SerializedName("coverUrl")     val coverUrl: String
)
