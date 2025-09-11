package ru.plumsoftware.alarm.ui.screen

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.plumsoftware.alarm.ui.theme.alarmCardColor
import ru.plumsoftware.alarm.ui.theme.alarmGrayTextColor
import ru.plumsoftware.alarm.ui.theme.primaryColor

@Composable
fun AlarmRepeatSheet(
    item: Pair<String, Int>,
    onSelected: (Pair<String, Int>) -> Unit,
    onBack: () -> Unit,
    context: Context = LocalContext.current
) {

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
                text = "Повтор",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(
                    RoundedCornerShape(
                        topEnd = 10.dp,
                        topStart = 10.dp,
                        bottomEnd = 0.dp,
                        bottomStart = 0.dp
                    )
                )
                .background(
                    alarmCardColor, RoundedCornerShape(
                        topEnd = 10.dp,
                        topStart = 10.dp,
                        bottomEnd = 0.dp,
                        bottomStart = 0.dp
                    )
                )
                .clickable(enabled = true) {

                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        )
        {
            Text(
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 12.dp
                ),
                text = "Никогда",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White
                )
            )
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .wrapContentSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    space = 4.dp,
                    alignment = Alignment.CenterHorizontally
                )
            ) {

            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    alarmCardColor, RoundedCornerShape(
                        topEnd = 10.dp,
                        topStart = 10.dp,
                        bottomEnd = 0.dp,
                        bottomStart = 0.dp
                    )
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                thickness = 1.dp
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    alarmCardColor, RectangleShape
                )
                .clickable(enabled = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        )
        {
            Text(
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 12.dp
                ),
                text = "Каждый понедельник",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White
                )
            )

            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .wrapContentSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    alarmCardColor, RoundedCornerShape(
                        topEnd = 10.dp,
                        topStart = 10.dp,
                        bottomEnd = 0.dp,
                        bottomStart = 0.dp
                    )
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                thickness = 1.dp
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    alarmCardColor, RectangleShape
                )
                .clickable(enabled = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        )
        {
            Text(
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 12.dp
                ),
                text = "Каждый вторник",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White
                )
            )

            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .wrapContentSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

            }
        }


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    alarmCardColor, RoundedCornerShape(
                        topEnd = 10.dp,
                        topStart = 10.dp,
                        bottomEnd = 0.dp,
                        bottomStart = 0.dp
                    )
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                thickness = 1.dp
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    alarmCardColor, RectangleShape
                )
                .clickable(enabled = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        )
        {
            Text(
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 12.dp
                ),
                text = "Каждую среду",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White
                )
            )

            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .wrapContentSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    alarmCardColor, RoundedCornerShape(
                        topEnd = 10.dp,
                        topStart = 10.dp,
                        bottomEnd = 0.dp,
                        bottomStart = 0.dp
                    )
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                thickness = 1.dp
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    alarmCardColor, RectangleShape
                )
                .clickable(enabled = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        )
        {
            Text(
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 12.dp
                ),
                text = "Каждый четверг",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White
                )
            )

            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .wrapContentSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    alarmCardColor, RoundedCornerShape(
                        topEnd = 10.dp,
                        topStart = 10.dp,
                        bottomEnd = 0.dp,
                        bottomStart = 0.dp
                    )
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                thickness = 1.dp
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    alarmCardColor, RectangleShape
                )
                .clickable(enabled = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        )
        {
            Text(
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 12.dp
                ),
                text = "Каждую пятницу",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White
                )
            )

            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .wrapContentSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    alarmCardColor, RoundedCornerShape(
                        topEnd = 10.dp,
                        topStart = 10.dp,
                        bottomEnd = 0.dp,
                        bottomStart = 0.dp
                    )
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                thickness = 1.dp
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    alarmCardColor, RectangleShape
                )
                .clickable(enabled = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        )
        {
            Text(
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 12.dp
                ),
                text = "Каждую субботу",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White
                )
            )

            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .wrapContentSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

            }
        }


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(
                    RoundedCornerShape(
                        topEnd = 0.dp,
                        topStart = 0.dp,
                        bottomEnd = 10.dp,
                        bottomStart = 10.dp
                    )
                )
                .background(
                    alarmCardColor, RoundedCornerShape(
                        topEnd = 0.dp,
                        topStart = 0.dp,
                        bottomEnd = 10.dp,
                        bottomStart = 10.dp
                    )
                )
                .clickable(enabled = true) {

                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        )
        {
            Text(
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 12.dp
                ),
                text = "Каждое воскресенье",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White
                )
            )
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .wrapContentSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    space = 4.dp,
                    alignment = Alignment.CenterHorizontally
                )
            ) {

            }
        }
    }
}