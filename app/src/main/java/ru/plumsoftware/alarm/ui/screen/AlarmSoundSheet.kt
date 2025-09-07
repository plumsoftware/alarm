package ru.plumsoftware.alarm.ui.screen

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.plumsoftware.alarm.data.alarmSounds
import ru.plumsoftware.alarm.ui.theme.alarmCardColor
import ru.plumsoftware.alarm.ui.theme.primaryColor

@SuppressLint("LocalContextResourcesRead")
@Composable
fun AlarmSoundSheet(
    item: Pair<String, Int>,
    onSelected: (Pair<String, Int>) -> Unit,
    onBack: () -> Unit,
    context: Context = LocalContext.current
) {
    var firstOpen by remember { mutableStateOf(true) }
    var selectedItem by remember { mutableStateOf(item) }
    val mediaPlayer = remember { MediaPlayer() }
    var currentVolume by remember { mutableFloatStateOf(1.0f) }

    LaunchedEffect(selectedItem) {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.reset()

        try {
            val resourceId = selectedItem.second
            val assetFd = context.resources.openRawResourceFd(resourceId)
            mediaPlayer.setDataSource(assetFd.fileDescriptor, assetFd.startOffset, assetFd.length)
            assetFd.close()
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            )
            mediaPlayer.prepare()
            mediaPlayer.setVolume(currentVolume, currentVolume)
            if (!firstOpen)
                mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val duration = 1000L
            val steps = 50
            val delayMillis = duration / steps

            if (mediaPlayer.isPlaying) {
                CoroutineScope(Dispatchers.Main).launch {
                    for (i in 0 until steps) {
                        val volume = currentVolume * (1 - i.toFloat() / steps)
                        mediaPlayer.setVolume(volume, volume)
                        delay(delayMillis)
                    }
                    mediaPlayer.stop()
                    mediaPlayer.release()
                }
            } else {
                mediaPlayer.release()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(
            space = 24.dp,
            alignment = Alignment.Top
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        )
        {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
//                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .wrapContentSize()
                    .clickable(true) {
                        onBack()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    space = 4.dp,
                    alignment = Alignment.CenterHorizontally
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = primaryColor
                )
                Text(
                    text = "Назад",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = primaryColor
                    )
                )
            }
            Text(
                modifier = Modifier
                    .align(Alignment.Center),
                text = "Мелодия",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            item {
                Spacer(modifier = Modifier.height(36.dp))
            }
            itemsIndexed(alarmSounds) { index, mapItem ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(
                            when (index) {
                                0 -> RoundedCornerShape(
                                    topEnd = 12.dp,
                                    topStart = 12.dp
                                )

                                alarmSounds.size - 1 -> RoundedCornerShape(
                                    bottomStart = 12.dp,
                                    bottomEnd = 12.dp
                                )

                                else -> RectangleShape
                            }
                        )
                        .background(
                            alarmCardColor,
                            when (index) {
                                0 -> RoundedCornerShape(
                                    topEnd = 12.dp,
                                    topStart = 12.dp
                                )

                                alarmSounds.size - 1 -> RoundedCornerShape(
                                    bottomStart = 12.dp,
                                    bottomEnd = 12.dp
                                )

                                else -> RectangleShape
                            }
                        )
                        .clickable {
                            selectedItem = mapItem
                            onSelected(selectedItem)
                            firstOpen = false
                        }
                        .padding(start = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = if (mapItem == selectedItem) primaryColor else Color.Transparent
                    )
                    Text(
                        text = mapItem.first,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White
                        )
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}