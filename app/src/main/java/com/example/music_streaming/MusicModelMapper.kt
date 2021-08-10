package com.example.music_streaming

import com.example.music_streaming.sevice.MusicDto
import com.example.music_streaming.sevice.MusicEntity

fun MusicEntity.intoMusicModel(id: Long): MusicModel {

    return MusicModel(
        id = id,
        track = track,
        artist = artist,
        streamUrl = streamUrl,
        coverUrl = coverUrl
    )
    //this.track this(MusicEntity)를 생략 해되 된다.
}

fun MusicDto.intoPlayerModel(): PlayerModel {

    return PlayerModel(
        playMusicList = this.musicList.mapIndexed { index, musicEntity ->
            musicEntity.intoMusicModel(index.toLong())
        }
    )
}