package com.example

import com.example.backend.contracts.CapabilitySupportStatus
import com.example.backend.contracts.JoyTubeCapabilityMatrix
import com.example.model.AuthStep
import com.example.model.ConfirmActionType
import com.example.model.JoyTubeRoute
import com.example.model.PrimaryTab
import com.example.viewmodel.JoyTubeViewModel
import org.junit.Assert.*
import org.junit.Test

class ExampleRobolectricTest {

    @Test
    fun `verify signed-out state and Google identity does not imply YouTube account linkage`() {
        val viewModel = JoyTubeViewModel()
        val initial = viewModel.uiState.value
        assertTrue(initial.glassDesignEnabled)
        assertEquals(initial.isSignedIn, initial.authSession != null)
        assertFalse("Google identity login must not claim YouTube account authorization", initial.authSession?.isYouTubeReadonlyConnected == true)

        // Verify capability matrix documents all 4 independent keyless providers + Supabase RLS
        val musicEntry = JoyTubeCapabilityMatrix.entries.first {
            it.featureDomain.contains("MusicProvider", ignoreCase = true)
        }
        assertEquals(CapabilitySupportStatus.TECHNICALLY_POSSIBLE_UNOFFICIAL, musicEntry.status)
        assertEquals(5, JoyTubeCapabilityMatrix.entries.size)
    }

    @Test
    fun `verify zero fake templates and require sign in before cloud imports`() {
        val viewModel = JoyTubeViewModel()
        val initial = viewModel.uiState.value

        // 1. Verify NO fake templates or songs exist on startup
        assertTrue("No fake media templates should exist on startup", initial.mediaList.isEmpty())
        assertTrue("No fake category templates should exist on startup", initial.categories.isEmpty())
        assertTrue("No fake downloads should exist on startup", initial.downloads.isEmpty())
        assertTrue("No fake watch history should exist on startup", initial.watchHistory.isEmpty())
        assertTrue("No fake playback queue should exist on startup", initial.globalPlayback.queue.isEmpty())
        assertFalse("Mini-player must be hidden when nothing is playing", initial.globalPlayback.isMiniBarVisible)
        assertEquals("Initial imported items count must be 0", 0, initial.importedItemsCount)

        // Importing without a Supabase user must not create a local pretend cloud record.
        viewModel.updateImportUrl("https://cdn.example.org/videos/my_custom_clip.mp4")
        viewModel.startImportFromUrl()
        viewModel.updatePendingImportTitle("My Custom Clip")
        viewModel.confirmImportNow()

        val afterFirstImport = viewModel.uiState.value
        assertTrue(afterFirstImport.mediaList.isEmpty())
        assertEquals(0, afterFirstImport.importedItemsCount)
        assertTrue(afterFirstImport.feedbackMessage.orEmpty().contains("Sign in"))
    }
}
