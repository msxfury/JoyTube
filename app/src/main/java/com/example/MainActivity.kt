package com.example

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import com.example.model.ActiveOverlay
import com.example.backend.auth.SupabaseAuthRuntime
import com.example.model.JoyTubeRoute
import com.example.model.PrimaryTab
import com.example.ui.components.BulkMediaSelectionToolbar
import com.example.ui.components.FloatingFeedbackToast
import com.example.ui.components.JoyTubeFloatingNavigationDock
import com.example.ui.screens.*
import com.example.ui.theme.JoyTubeColors
import com.example.ui.theme.LocalGlassDesignEnabled
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.JoyTubeViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupabaseAuthRuntime.manager.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                JoyTubeAppRoot()
            }
        }
    }
}

@Composable
fun JoyTubeAppRoot(
    viewModel: JoyTubeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isDockScrollCompacted by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val updatePreferences = remember(context) {
        context.getSharedPreferences(APP_UPDATE_PREFERENCES, android.content.Context.MODE_PRIVATE)
    }
    var dismissedUpdateVersion by remember(updatePreferences) {
        mutableStateOf(updatePreferences.getString(DISMISSED_UPDATE_VERSION_KEY, null))
    }
    val dismissUpdate: (String) -> Unit = { version ->
        dismissedUpdateVersion = version
        updatePreferences.edit().putString(DISMISSED_UPDATE_VERSION_KEY, version).apply()
    }

    LaunchedEffect(uiState.isSignedIn) {
        if (!uiState.isSignedIn) {
            viewModel.clearAvailableAppUpdate()
        } else {
            while (true) {
                viewModel.checkForAppUpdate()
                delay(APP_UPDATE_CHECK_INTERVAL_MS)
            }
        }
    }

    // Reset dock to full floating height whenever route changes
    LaunchedEffect(uiState.currentRoute) {
        isDockScrollCompacted = false
    }

    // Zero-overhead NestedScrollConnection with scroll delta accumulation so the floating glass dock
    // smoothly slides shut into a sphere on downward scroll and slides open on upward scroll
    val dockScrollConnection = remember {
        object : NestedScrollConnection {
            private var accumulatedDeltaY = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                if ((dy < 0f && accumulatedDeltaY > 0f) || (dy > 0f && accumulatedDeltaY < 0f)) {
                    accumulatedDeltaY = 0f
                }
                accumulatedDeltaY += dy

                if (accumulatedDeltaY < -28f && !isDockScrollCompacted) {
                    isDockScrollCompacted = true
                    accumulatedDeltaY = 0f
                } else if (accumulatedDeltaY > 22f && isDockScrollCompacted) {
                    isDockScrollCompacted = false
                    accumulatedDeltaY = 0f
                }
                return Offset.Zero
            }
        }
    }

    val canHandleBack = !uiState.isSignedIn ||
        uiState.activeOverlay != null ||
        uiState.isMediaSelectionMode ||
        uiState.isCategorySelectionMode ||
        uiState.backStack.isNotEmpty() ||
        (uiState.currentRoute !is JoyTubeRoute.Main || (uiState.currentRoute as JoyTubeRoute.Main).tab != PrimaryTab.Home)

    BackHandler(enabled = canHandleBack) {
        if (uiState.isSignedIn) {
            viewModel.navigateBack()
        }
    }

    LaunchedEffect(uiState.feedbackMessage) {
        if (uiState.feedbackMessage != null) {
            delay(2400L)
            viewModel.dismissFeedback()
        }
    }

    // Advance player timeline only when actively on the VideoPlayer screen
    val isPlayerActive = uiState.isSignedIn && uiState.currentRoute is JoyTubeRoute.VideoPlayer && uiState.isPlaying
    LaunchedEffect(isPlayerActive) {
        while (isPlayerActive) {
            delay(1000L)
            viewModel.seekTo(viewModel.uiState.value.currentPositionSec + 1)
        }
    }

    val showBottomDock = uiState.isSignedIn && uiState.currentRoute is JoyTubeRoute.Main && !uiState.isMediaSelectionMode

    CompositionLocalProvider(LocalGlassDesignEnabled provides uiState.glassDesignEnabled) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(JoyTubeColors.Background)
                .statusBarsPadding()
                .nestedScroll(dockScrollConnection)
        ) {
            if (!uiState.isSignedIn) {
                AuthFlowScreen(
                    initialStep = com.example.model.AuthStep.SignIn,
                    uiState = uiState,
                    viewModel = viewModel
                )
            } else when (val route = uiState.currentRoute) {
                is JoyTubeRoute.Main -> {
                    when (route.tab) {
                        PrimaryTab.Home -> HomeScreen(uiState = uiState, viewModel = viewModel)
                        PrimaryTab.Shorts -> ShortsScreen(uiState = uiState, viewModel = viewModel, isFullscreen = false)
                        PrimaryTab.Search -> SearchScreen(uiState = uiState, viewModel = viewModel)
                        PrimaryTab.Account -> AccountScreen(uiState = uiState, viewModel = viewModel)
                    }
                }

                is JoyTubeRoute.VideoPlayer -> {
                    VideoPlayerScreen(
                        mediaId = route.mediaId,
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }

                is JoyTubeRoute.FullscreenShorts -> {
                    ShortsScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        isFullscreen = true
                    )
                }

                JoyTubeRoute.MyCategories, JoyTubeRoute.CreateCategory -> {
                    MyCategoriesScreen(uiState = uiState, viewModel = viewModel)
                }

                is JoyTubeRoute.CategoryDetail -> {
                    CategoryDetailScreen(
                        categoryId = route.categoryId,
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }

                JoyTubeRoute.ImportMedia -> {
                    ImportMediaScreen(uiState = uiState, viewModel = viewModel)
                }

                JoyTubeRoute.ImportPreview -> {
                    ImportPreviewScreen(uiState = uiState, viewModel = viewModel)
                }

                JoyTubeRoute.ImportCompletion -> {
                    ImportCompletionScreen(uiState = uiState, viewModel = viewModel)
                }

                JoyTubeRoute.ImportedVideos -> {
                    ImportedVideosScreen(uiState = uiState, viewModel = viewModel)
                }

                JoyTubeRoute.Favourites -> {
                    FavouritesScreen(uiState = uiState, viewModel = viewModel)
                }

                JoyTubeRoute.Downloads -> {
                    DownloadsScreen(uiState = uiState, viewModel = viewModel)
                }

                JoyTubeRoute.History -> {
                    HistoryScreen(uiState = uiState, viewModel = viewModel)
                }

                JoyTubeRoute.BrowserHome -> {
                    BrowserHomeScreen(uiState = uiState, viewModel = viewModel)
                }

                JoyTubeRoute.BrowserTabs -> {
                    BrowserTabsScreen(uiState = uiState, viewModel = viewModel)
                }

                JoyTubeRoute.AdBlockerSettings -> {
                    AdBlockerSettingsScreen(uiState = uiState, viewModel = viewModel)
                }

                JoyTubeRoute.Settings -> {
                    SettingsScreen(uiState = uiState, viewModel = viewModel)
                }

                JoyTubeRoute.AboutJoyTube -> {
                    AboutJoyTubeScreen(viewModel = viewModel)
                }

                JoyTubeRoute.TermsAndPrivacy -> {
                    TermsAndPrivacyScreen(viewModel = viewModel)
                }

                JoyTubeRoute.Profile -> {
                    ProfileScreen(uiState = uiState, viewModel = viewModel)
                }

                is JoyTubeRoute.AuthFlow -> {
                    AuthFlowScreen(
                        initialStep = route.step,
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }

                JoyTubeRoute.DesignSystemSpecs -> {
                    DesignSystemScreen(uiState = uiState, viewModel = viewModel)
                }
            }

            // Floating Bottom Navigation Dock with Scroll-Down/Up, Synchronized Mini-Player & Liquid Glass Click Physics
            if (showBottomDock) {
                val activeTab = (uiState.currentRoute as JoyTubeRoute.Main).tab
                JoyTubeFloatingNavigationDock(
                    selectedTab = activeTab,
                    onSelectTab = { tab ->
                        isDockScrollCompacted = false
                        viewModel.selectPrimaryTab(tab)
                    },
                    isScrollCompacted = isDockScrollCompacted,
                    playbackSnapshot = uiState.globalPlayback,
                    onMiniPlayerClick = { viewModel.openActiveMiniPlayer() },
                    onMiniPlayerPlayPause = { viewModel.togglePlayPause() },
                    onMiniPlayerNext = { viewModel.skipToNextPlaybackItem() },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } else if (uiState.isMediaSelectionMode && uiState.currentRoute is JoyTubeRoute.Main) {
                BulkMediaSelectionToolbar(
                    onMoveClick = {
                        viewModel.showOverlay(
                            ActiveOverlay.AddToCategory(uiState.selectedMediaIds.toList())
                        )
                    },
                    onFavouriteClick = { viewModel.bulkFavouriteSelectedMedia() },
                    onDownloadClick = {
                        val first = uiState.mediaList.find { it.id in uiState.selectedMediaIds }
                        if (first != null) {
                            viewModel.showOverlay(ActiveOverlay.DownloadQuality(first))
                        }
                    },
                    onDeleteClick = {
                        viewModel.showOverlay(
                            ActiveOverlay.DeleteSelectedMediaConfirm(uiState.selectedMediaIds)
                        )
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // Contextual Pop-ups, Bottom Sheets, and Confirmation Dialogs
            JoyTubeOverlayHost(
                uiState = uiState,
                viewModel = viewModel
            )

            // Floating contextual feedback banner
            FloatingFeedbackToast(
                message = uiState.feedbackMessage,
                onDismiss = { viewModel.dismissFeedback() },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            val releaseUpdate = uiState.updateAvailable
            if (uiState.isSignedIn && releaseUpdate != null && dismissedUpdateVersion != releaseUpdate.version) {
                AlertDialog(
                    onDismissRequest = { dismissUpdate(releaseUpdate.version) },
                    title = { Text("New Update Available!") },
                    text = { Text("JoyTube v${releaseUpdate.version}") },
                    confirmButton = {
                        Button(
                            onClick = {
                                dismissUpdate(releaseUpdate.version)
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(releaseUpdate.apkUrl)))
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF111318)
                            )
                        ) {
                            Text("Update")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = { dismissUpdate(releaseUpdate.version) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JoyTubeColors.SurfaceElevated,
                                contentColor = JoyTubeColors.TextPrimary
                            )
                        ) {
                            Text("Later")
                        }
                    },
                    containerColor = JoyTubeColors.Surface
                )
            }
        }
    }
}

private const val APP_UPDATE_CHECK_INTERVAL_MS = 30 * 60 * 1000L
private const val APP_UPDATE_PREFERENCES = "joytube_app_updates"
private const val DISMISSED_UPDATE_VERSION_KEY = "dismissed_version"
