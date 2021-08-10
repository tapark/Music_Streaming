package com.example.music_streaming

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.music_streaming.databinding.FragmentPlayerBinding
import com.example.music_streaming.sevice.MusicDto
import com.example.music_streaming.sevice.MusicService
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PlayerFragment: Fragment(R.layout.fragment_player) {

    private var model: PlayerModel = PlayerModel()

    private var binding: FragmentPlayerBinding? = null
    //private var isWatchingPlayListView = true
    private var player: SimpleExoPlayer? = null

    private lateinit var playListAdapter: PlayListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentPlayerBinding = FragmentPlayerBinding.bind(view)
        binding = fragmentPlayerBinding

        initPlayView(fragmentPlayerBinding)
        initRecyclerView(fragmentPlayerBinding)
        initPlayControlButtons(fragmentPlayerBinding)
        initPlayListButton(fragmentPlayerBinding)

        getVideoListFromServer()
    }

    private fun initPlayControlButtons(fragmentPlayerBinding: FragmentPlayerBinding) {

        fragmentPlayerBinding.playControlImageView.setOnClickListener {
            val player = this.player ?: return@setOnClickListener
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }

        fragmentPlayerBinding.skipNextImageView.setOnClickListener {
            val nextMusic = model.nextMusic() ?: return@setOnClickListener
            playMusic(nextMusic)
        }

        fragmentPlayerBinding.skipPrevImageView.setOnClickListener {
            val prevMusic = model.prevMusic() ?: return@setOnClickListener
            playMusic(prevMusic)
        }
    }

    private fun initPlayView(fragmentPlayerBinding: FragmentPlayerBinding) {
        context?.let {
            player = SimpleExoPlayer.Builder(it).build()
        }
        fragmentPlayerBinding.playerView.player = player

        player?.addListener(object: Player.Listener {
            // 플레이 상태에 따른 재생-일시정지 버튼 변환
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    fragmentPlayerBinding.playControlImageView.setImageResource(R.drawable.ic_baseline_pause_48)
                } else {
                    fragmentPlayerBinding.playControlImageView.setImageResource(R.drawable.ic_baseline_play_arrow_48)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)

                val newIndex = mediaItem?.mediaId ?: return
                model.currentPosition = newIndex.toInt()
                playListAdapter.submitList(model.getAdapterModels())
            }


        })
    }

    private fun initRecyclerView(fragmentPlayerBinding: FragmentPlayerBinding) {
        playListAdapter = PlayListAdapter {
            //음악을 재생
            playMusic(it)
        }

        fragmentPlayerBinding.playListRecyclerView.layoutManager = LinearLayoutManager(context)
        fragmentPlayerBinding.playListRecyclerView.adapter = playListAdapter
    }

    private fun playMusic(musicModel: MusicModel) {
        model.updateCurrentPosition(musicModel)
        player?.seekTo(model.currentPosition, 0)
        player?.play()
    }

    private fun initPlayListButton(fragmentPlayerBinding: FragmentPlayerBinding) {
        fragmentPlayerBinding.playListImageView.setOnClickListener {
            // 선택된 음악이 없는겅우
            if (model.currentPosition == -1) return@setOnClickListener

            fragmentPlayerBinding.playerViewGroup.isVisible = model.isWatchingPlayListView
            fragmentPlayerBinding.playListViewGroup.isVisible = !model.isWatchingPlayListView
            model.isWatchingPlayListView = !model.isWatchingPlayListView
        }
    }


    private fun getVideoListFromServer() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://run.mocky.io/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(MusicService::class.java).also {
            it.getMusicList().enqueue(object: Callback<MusicDto> {
                override fun onResponse(call: Call<MusicDto>, response: Response<MusicDto>) {
                    if (response.isSuccessful.not()) {
                        Toast.makeText(context, "서버응답 실패", Toast.LENGTH_SHORT).show()
                        return
                    }
                    response.body()?.let { musicDto ->

                        model = musicDto.intoPlayerModel()

                        setMusicList(model.getAdapterModels())
                        playListAdapter.submitList(model.getAdapterModels())
                    }
                }

                override fun onFailure(call: Call<MusicDto>, t: Throwable) {
                    Toast.makeText(context, "서버응답 실패", Toast.LENGTH_SHORT).show()
                }

            })
        }
    }

    private fun setMusicList(modelList: List<MusicModel>) {
        val mediaItemList = modelList.map {
            MediaItem.Builder()
                .setUri(it.streamUrl)
                .setMediaId(it.id.toString())
                .build()
        }
        player?.addMediaItems(mediaItemList)
        player?.prepare()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    companion object {
        fun newInstance(): PlayerFragment {
            return PlayerFragment()
        }
    }
}