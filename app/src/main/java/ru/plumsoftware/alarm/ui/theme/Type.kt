package ru.plumsoftware.alarm.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        color = Color.White,
        fontSize = 30.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        color = Color.White,
        fontSize = 48.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Normal,
        color = Color.White,
        fontSize = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        color = primaryColor,
        fontSize = 16.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Light,
        color = primaryColor,
        fontSize = 12.sp
    ),
)