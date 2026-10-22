package com.prempoddar.tune.ui

import org.robolectric.annotation.Config

// The same screens at tablet sizes, for store listings: a 7-inch tablet held upright and a
// 10-inch tablet (or a Chromebook) on its side.

@Config(qualifiers = "w600dp-h960dp-xhdpi")
class Tablet7ScreensTest : ScreensTest() { override val out = "build/screens/tablet7" }

@Config(qualifiers = "w1280dp-h800dp-land-xhdpi")
class Tablet10ScreensTest : ScreensTest() { override val out = "build/screens/tablet10" }
