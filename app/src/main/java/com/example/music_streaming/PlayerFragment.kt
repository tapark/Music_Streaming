package com.example.music_streaming

import android.os.Bundle
import android.util.Log
import android.util.TimeUtils
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
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
import java.util.concurrent.TimeUnit

class PlayerFragment: Fragment(R.layout.fragment_player) {

    private var model: PlayerModel = PlayerModel()

    private var binding: FragmentPlayerBinding? = null
    //private var isWatchingPlayListView = true
    private var player: SimpleExoPlayer? = null

    private lateinit var playListAdapter: PlayListAdapter

    private val updateSeekRunnable = Runnable {
        updateSeek()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentPlayerBinding = FragmentPlayerBinding.bind(view)
        binding = fragmentPlayerBinding

        initPlayView(fragmentPlayerBinding)
        initRecyclerView(fragmentPlayerBinding)
        initPlayControlButtons(fragmentPlayerBinding)
        initPlayListButton(fragmentPlayerBinding)
        initSeekBar(fragmentPlayerBinding)

        getVideoListFromServer()
    }

    private fun initSeekBar(fragmentPlayerBinding: FragmentPlayerBinding) {
        fragmentPlayerBinding.playerSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            // seekbar를 조작하고 손에서 뗄때
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                player?.seekTo((seekBar.progress * 1000).toLong())
            }

        })

        fragmentPlayerBinding.playListSeekBar.setOnTouchListener { _, _ ->
            false
        }
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

            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)

                updateSeek()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)

                val newIndex = mediaItem?.mediaId ?: return
                model.currentPosition = newIndex.toInt()
                updatePlayerView(model.currentMusicModel())
                playListAdapter.submitList(model.getAdapterModels())
            }


        })
    }

    private fun updateSeek() {

        val player = this.player ?: return
        val duration = if (player.duration >= 0) player.duration else 0
        val position = player.currentPosition

        // ui update
        updateSeekUi(duration, position)

        val state = player.playbackState
        view?.removeCallbacks(updateSeekRunnable)
        // STATE_IDLE : no media to play, STATE_ENDED : finish media play
        if (state != Player.STATE_IDLE && state != Player.STATE_ENDED) {
            view?.postDelayed(updateSeekRunnable, 1000)
        }
    }

    private fun updateSeekUi(duration: Long, position: Long) {
        binding?.let { binding ->
            binding.playListSeekBar.max = (duration / 1000).toInt()
            binding.playListSeekBar.progress = (position / 1000).toInt()

            binding.playerSeekBar.max = (duration / 1000).toInt()
            binding.playerSeekBar.progress = (position / 1000).toInt()

            binding.playTimeTextView.text = String.format("%02d:%02d",
                TimeUnit.MINUTES.convert(position, TimeUnit.MILLISECONDS),
                position / 1000 % 60
            )
            binding.totalTimeTextView.text = String.format("%02d:%02d",
                TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS),
                duration / 1000 % 60
            )
        }
    }

    private fun updatePlayerView(currentMusicModel: MusicModel?) {
        currentMusicModel ?: return
        binding?.let { binding ->
            binding.trackTextView.text = currentMusicModel.track
            binding.artistTextView.text = currentMusicModel.artist
            Glide.with(binding.coverImageView)
                .load(currentMusicModel.coverUrl)
                .into(binding.coverImageView)
        }
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

    override fun onStop() {
        super.onStop()

        player?.pause()
        view?.removeCallbacks(updateSeekRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null

        player?.release()
        view?.removeCallbacks(updateSeekRunnable)
    }

    companion object {
        fun newInstance(): PlayerFragment {
            return PlayerFragment()
        }
    }
}