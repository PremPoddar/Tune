package com.prempoddar.tune.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.prempoddar.tune.core.Levels
import com.prempoddar.tune.core.StreamKind
import com.prempoddar.tune.data.db.TuneDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProfileRepositoryTest {
    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(), TuneDatabase::class.java,
    ).allowMainThreadQueries().build()
    private var now = 100L
    private val repo = ProfileRepository(db.profileDao()) { now }
    private val base = Levels(50, 60, 60, 80)

    @After fun close() = db.close()

    @Test fun setLevelCreatesRowFromBaseThenUpdates() = runTest {
        repo.setLevel("spotify", "speaker", StreamKind.MEDIA, 30, base)
        now = 200
        repo.setLevel("spotify", "speaker", StreamKind.RING, 10, base)
        val p = repo.profiles.first().single()
        assertThat(p.levels).isEqualTo(Levels(30, 10, 60, 80))
        assertThat(p.updatedAt).isEqualTo(200)
    }

    @Test fun pinningAppliesToEveryRouteAndNewRowsInheritIt() = runTest {
        repo.setLevel("spotify", "speaker", StreamKind.MEDIA, 30, base)
        repo.setPinned("spotify", "wired", true, base)
        repo.setLevel("spotify", "usb", StreamKind.MEDIA, 70, base)
        val rows = repo.profiles.first()
        assertThat(rows).hasSize(3)
        assertThat(rows.all { it.pinned }).isTrue()
    }

    @Test fun forgetRemovesEveryRoute() = runTest {
        repo.setLevel("spotify", "speaker", StreamKind.MEDIA, 30, base)
        repo.setLevel("spotify", "wired", StreamKind.MEDIA, 40, base)
        repo.setLevel("chrome", "wired", StreamKind.MEDIA, 40, base)
        repo.forget("spotify")
        assertThat(repo.profiles.first().map { it.pkg }).containsExactly("chrome")
    }
}
