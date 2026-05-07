package com.example.audioandvideoeditor.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class AudioSegmenterViewModel : ViewModel() {
    var currentAudioUri by mutableStateOf<Uri?>(null)
        private set

    private var exoPlayer: ExoPlayer? = null
    var initialize_source_flag by mutableStateOf(false)

    fun setAudioUri(uri: Uri?) {
        currentAudioUri = uri
    }

    fun initializeSource(context: android.content.Context) {
        if (exoPlayer == null && currentAudioUri != null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(currentAudioUri!!)
                setMediaItem(mediaItem)
                prepare()
            }
        }
        initialize_source_flag = true
    }

    fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    fun getExoPlayer(): ExoPlayer? {
        return exoPlayer
    }

    fun getDuration(): Long {
        return exoPlayer?.duration ?: 0L
    }

    fun getCurrentPosition(): Float {
        return exoPlayer?.currentPosition?.toFloat() ?: 0f
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying ?: false
    }

    fun seekTo(position: Float) {
        exoPlayer?.seekTo(position.toLong())
    }

    fun seekToFraction(fraction: Float) {
        val duration = exoPlayer?.duration?.toFloat() ?: 0f
        val position = duration * fraction
        exoPlayer?.seekTo(position.toLong())
    }

    var startTime by mutableStateOf(0f)
    var endTime by mutableStateOf(1f)

    fun updateEndTimeAndSeek(newEndTime: Float) {
        endTime = if (newEndTime <= startTime) {
            startTime
        } else {
            newEndTime
        }
        seekToFraction(endTime)
    }

    fun updateStartTimeAndSeek(newStartTime: Float) {
        startTime = if (newStartTime >= endTime) {
            endTime
        } else {
            newStartTime
        }
        seekToFraction(startTime)
    }

    var showTimeErrorDialog by mutableStateOf(false)
    var showFileNameDialog by mutableStateOf(false)
    var outputFileName by mutableStateOf("")
    var showSegmentDialog by mutableStateOf(false)

    data class Segment(
        val id: Int,
        var startTime: Float,
        var endTime: Float,
        var name: String = ""
    )

    var segments = mutableStateOf<List<Segment>>(emptyList())
    var nextSegmentId = 0

    fun addSegment() {
        val duration = getDuration().toFloat()
        val currentSegments = segments.value
        val newSegment = Segment(
            id = nextSegmentId++,
            startTime = if (currentSegments.isEmpty()) 0f else currentSegments.last().endTime,
            endTime = if (currentSegments.isEmpty()) duration / 2 else minOf(currentSegments.last().endTime + duration / 4, duration),
            name = "segment_${nextSegmentId}"
        )
        segments.value = currentSegments + newSegment
    }

    fun removeSegment(id: Int) {
        segments.value = segments.value.filter { it.id != id }
    }

    fun updateSegment(id: Int, startTime: Float, endTime: Float) {
        segments.value = segments.value.map {
            if (it.id == id) it.copy(startTime = startTime, endTime = endTime) else it
        }
    }

    fun updateSegmentName(id: Int, name: String) {
        segments.value = segments.value.map {
            if (it.id == id) it.copy(name = name) else it
        }
    }
}