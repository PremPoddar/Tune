package com.prempoddar.tune.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.prempoddar.tune.R

@OptIn(ExperimentalTextApi::class)
private fun nunito(weight: Int) = Font(
    R.font.nunito,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Nunito = FontFamily(nunito(400), nunito(600), nunito(700), nunito(800))

private const val TABULAR = "tnum"

/** Big level numbers: heavy and tabular so they don't jitter while dragging. */
val LevelNumber = TextStyle(
    fontFamily = Nunito, fontWeight = FontWeight(800), fontSize = 34.sp,
    letterSpacing = (-0.02).em, fontFeatureSettings = TABULAR,
)

val LevelNumberSmall = LevelNumber.copy(fontSize = 17.sp, letterSpacing = 0.em)

val TuneTypography = Typography(
    displaySmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(800), fontSize = 34.sp, letterSpacing = (-0.02).em),
    headlineMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(800), fontSize = 28.sp, letterSpacing = (-0.01).em),
    headlineSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(800), fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(700), fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(700), fontSize = 17.sp),
    titleSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(700), fontSize = 14.sp, letterSpacing = 0.01.em),
    bodyLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(400), fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(400), fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(400), fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(700), fontSize = 15.sp),
    labelMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(700), fontSize = 12.sp, letterSpacing = 0.02.em),
    labelSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(700), fontSize = 11.sp, letterSpacing = 0.04.em),
)
