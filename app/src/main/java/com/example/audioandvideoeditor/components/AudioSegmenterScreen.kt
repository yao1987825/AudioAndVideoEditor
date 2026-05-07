package com.example.audioandvideoeditor.components

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import com.example.audioandvideoeditor.MainActivity
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.entity.TaskInfo
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.utils.TextsUtils
import com.example.audioandvideoeditor.viewmodel.AudioSegmenterViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TAG = "AudioSegmenterScreen"

@Composable
fun AudioSegmenterScreen(
    activity: MainActivity,
    file: File,
    nextDestination: () -> Unit,
    viewModel: AudioSegmenterViewModel = viewModel()
) {
    AudioSegmenter(activity, file, nextDestination, viewModel)
}

@Composable
fun AudioSegmenter(
    activity: MainActivity,
    file: File,
    nextDestination: () -> Unit,
    viewModel: AudioSegmenterViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var playerView: PlayerView? = null
    var audioReady by remember { mutableStateOf(false) }

    val sampleAudioUri = Uri.parse(file.path)
    if (viewModel.currentAudioUri == null) {
        viewModel.setAudioUri(sampleAudioUri)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                viewModel.initializeSource(context)
            } else if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.releasePlayer()
                audioReady = false
                viewModel.initialize_source_flag = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            viewModel.releasePlayer()
            audioReady = false
            playerView = null
        }
    }

    LaunchedEffect(viewModel.getExoPlayer()) {
        viewModel.getExoPlayer()?.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_READY) {
                    audioReady = true
                }
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (viewModel.initialize_source_flag && audioReady) {
            val duration = viewModel.getDuration().toFloat()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
            ) {
                if (viewModel.currentAudioUri != null && audioReady) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            playerView = PlayerView(ctx).apply {
                                player = viewModel.getExoPlayer()
                                useController = true
                            }
                            playerView!!
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${stringResource(id = R.string.start_time)}: ${TextsUtils.millisecondsToString((viewModel.startTime * duration).toLong())}",
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = viewModel.startTime,
                onValueChange = { newStartTime ->
                    viewModel.updateStartTimeAndSeek(newStartTime)
                },
                valueRange = 0f..1f,
                steps = 100,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "${stringResource(id = R.string.end_time)}: ${TextsUtils.millisecondsToString((viewModel.endTime * duration).toLong())}",
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = viewModel.endTime,
                onValueChange = { newEndTime ->
                    viewModel.updateEndTimeAndSeek(newEndTime)
                },
                valueRange = 0f..1f,
                steps = 100,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { viewModel.addSegment() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加片段")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = { viewModel.showSegmentDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("导出所有片段")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "已添加 ${viewModel.segments.value.size} 个片段",
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(viewModel.segments.value) { segment ->
                    SegmentItem(
                        segment = segment,
                        duration = duration,
                        onUpdate = { start, end, name ->
                            viewModel.updateSegment(segment.id, start, end)
                            if (name.isNotEmpty()) {
                                viewModel.updateSegmentName(segment.id, name)
                            }
                        },
                        onDelete = { viewModel.removeSegment(segment.id) },
                        onPreview = { start, end ->
                            viewModel.seekToFraction(start)
                            viewModel.play()
                        }
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.wait), textAlign = TextAlign.Center)
            }
        }
    }

    if (viewModel.showSegmentDialog) {
        ExportDialog(
            viewModel = viewModel,
            file = file,
            activity = activity,
            onDismiss = { viewModel.showSegmentDialog = false },
            onConfirm = {
                startBatchExport(activity, file, nextDestination, viewModel)
                viewModel.showSegmentDialog = false
            }
        )
    }
}

@Composable
private fun SegmentItem(
    segment: AudioSegmenterViewModel.Segment,
    duration: Float,
    onUpdate: (Float, Float, String) -> Unit,
    onDelete: () -> Unit,
    onPreview: (Float, Float) -> Unit
) {
    var editName by remember { mutableStateOf(segment.name) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("名称") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = Color.Red
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "开始: ${TextsUtils.millisecondsToString((segment.startTime * duration).toLong())}",
                fontSize = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = segment.startTime,
                onValueChange = { onUpdate(it, segment.endTime, editName) },
                valueRange = 0f..segment.endTime,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "结束: ${TextsUtils.millisecondsToString((segment.endTime * duration).toLong())}",
                fontSize = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = segment.endTime,
                onValueChange = { onUpdate(segment.startTime, it, editName) },
                valueRange = segment.startTime..1f,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { onPreview(segment.startTime, segment.endTime) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("预览此片段")
            }
        }
    }
}

@Composable
private fun ExportDialog(
    viewModel: AudioSegmenterViewModel,
    file: File,
    activity: MainActivity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var baseFileName by remember { mutableStateOf("audio_segment") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出设置") },
        text = {
            Column {
                Text("将导出 ${viewModel.segments.value.size} 个音频片段为MP3格式")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = baseFileName,
                    onValueChange = { baseFileName = it },
                    label = { Text("文件名基础") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.segments.value.forEachIndexed { index, segment ->
                    segment.name = if (segment.name.isEmpty()) "${baseFileName}_${index + 1}" else segment.name
                }
                onConfirm()
            }) {
                Text(stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun startBatchExport(
    activity: MainActivity,
    file: File,
    nextDestination: () -> Unit,
    viewModel: AudioSegmenterViewModel
) {
    val duration = viewModel.getDuration().toFloat()
    val basePath = ConfigsUtils.target_dir

    viewModel.segments.value.forEachIndexed { index, segment ->
        val startTime = (segment.startTime * duration / 1000).toLong()
        val endTime = (segment.endTime * duration / 1000).toLong()
        val segmentDuration = endTime - startTime
        val targetName = if (segment.name.isEmpty()) "segment_${index + 1}" else segment.name
        val targetPath = "$basePath/${targetName}.mp3"

        val cmd_str = "ffmpeg -ss ${TextsUtils.millisecondsToString(startTime)} -t ${TextsUtils.millisecondsToString(segmentDuration)} -i input_file -vn -acodec libmp3lame -q:a 2 output_file"

        val command_arg_list = cmd_str.trim().split("[\\s\\n]+".toRegex())
            .map {
                when (it) {
                    "input_file" -> file.path
                    "output_file" -> targetPath
                    else -> it
                }
            }

        val int_arr = ArrayList<Int>()
        int_arr.add(3)
        int_arr.add(command_arg_list.size)
        val long_arr = ArrayList<Long>()
        long_arr.add(segmentDuration)
        val str_arr = ArrayList<String>()
        str_arr.add(targetPath)
        val date = Date(System.currentTimeMillis())
        val formatter = SimpleDateFormat("yyyyMMddHHmmss", activity.resources.configuration.locales[0])
        val task_log_path = activity.filesDir.absolutePath + "/ffmpeg" + formatter.format(date) + "_${index}.log"
        str_arr.add(task_log_path)
        str_arr.addAll(command_arg_list)
        val float_arr = ArrayList<Float>()
        val info = TaskInfo(
            int_arr,
            long_arr,
            str_arr,
            float_arr
        )
        activity.tasksBinder.startTask(info)
    }

    viewModel.setAudioUri(null)
    viewModel.segments.value = emptyList()
    nextDestination()
}