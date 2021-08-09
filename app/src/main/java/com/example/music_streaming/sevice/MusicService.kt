package com.example.music_streaming.sevice

import retrofit2.Call
import retrofit2.http.GET

interface MusicService {
    @GET("/v3/5fbeac3d-a158-4cc8-8428-f783238181d8")
    fun getMusicList(): Call<MusicDto>
}