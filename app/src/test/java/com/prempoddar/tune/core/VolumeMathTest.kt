package com.prempoddar.tune.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VolumeMathTest {
    @Test fun indexRoundsAndKeepsAudibleAboveZero() {
        assertThat(VolumeMath.pctToIndex(0, 15)).isEqualTo(0)
        assertThat(VolumeMath.pctToIndex(1, 15)).isEqualTo(1)
        assertThat(VolumeMath.pctToIndex(50, 15)).isEqualTo(8)
        assertThat(VolumeMath.pctToIndex(100, 15)).isEqualTo(15)
    }

    @Test fun indexToPctIsInverseAtSteps() {
        assertThat(VolumeMath.indexToPct(15, 15)).isEqualTo(100)
        assertThat(VolumeMath.indexToPct(0, 15)).isEqualTo(0)
        assertThat(VolumeMath.indexToPct(8, 15)).isEqualTo(53)
    }

    @Test fun dbCurve() {
        assertThat(VolumeMath.pctToDb(100)).isEqualTo(0f)
        assertThat(VolumeMath.pctToDb(50)).isEqualTo(-30f)
        assertThat(VolumeMath.pctToDb(0)).isEqualTo(VolumeMath.MUTE_DB)
    }

    @Test fun gainIsRelativeToMasterAndNeverBoosts() {
        assertThat(VolumeMath.gainDb(30, 80)).isWithin(0.01f).of(-30f)
        assertThat(VolumeMath.gainDb(80, 30)).isEqualTo(0f)
        assertThat(VolumeMath.gainDb(0, 80)).isEqualTo(VolumeMath.MUTE_DB)
        assertThat(VolumeMath.gainDb(40, 0)).isEqualTo(0f)
    }

    @Test fun levelsWithClamps() {
        assertThat(Levels.FALLBACK.with(StreamKind.RING, 140).ring).isEqualTo(100)
        assertThat(Levels.FALLBACK.with(StreamKind.MEDIA, -3)[StreamKind.MEDIA]).isEqualTo(0)
    }
}
