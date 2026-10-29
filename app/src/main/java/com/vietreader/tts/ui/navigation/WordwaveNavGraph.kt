package com.vietreader.tts.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vietreader.tts.data.repository.BookRepositoryImpl
import com.vietreader.tts.domain.model.ReaderMode
import com.vietreader.tts.ui.ambient.AmbientMusicRoute
import com.vietreader.tts.ui.bookdetail.BookDetailRoute
import com.vietreader.tts.ui.components.AppBottomTab
import com.vietreader.tts.ui.components.WelcomeOnboardingDialog
import com.vietreader.tts.ui.library.LibraryRoute
import com.vietreader.tts.ui.reader.ReaderRoute
import com.vietreader.tts.ui.settings.SettingsRoute
import com.vietreader.tts.ui.voices.VoiceManagementRoute

object WordwaveRoutes {
    const val LIBRARY = "library"
    const val AMBIENT_MUSIC = "ambient_music"
    const val BOOK_DETAIL = "book_detail/{bookId}"
    const val READER = "reader?bookId={bookId}&mode={mode}"
    const val VOICES = "voices"
    const val SETTINGS = "settings"

    fun bookDetailRoute(bookId: String): String {
        return "book_detail/${Uri.encode(bookId)}"
    }

    fun readerRoute(
        bookId: String? = null,
        mode: ReaderMode = ReaderMode.READ_ALONG
    ): String {
        val encodedBookId = Uri.encode(bookId.orEmpty())
        return "reader?bookId=$encodedBookId&mode=${mode.name}"
    }
}

private const val NAV_ANIM_DURATION = 280

@Composable
fun WordwaveNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = WordwaveRoutes.LIBRARY,
    openReaderRequestTick: Long = 0L,
    onOpenReaderHandled: () -> Unit = {}
) {
    val repository = BookRepositoryImpl.instance
    val showOnboarding by repository.showOnboarding.collectAsState()

    LaunchedEffect(openReaderRequestTick) {
        if (openReaderRequestTick > 0L) {
            val activeBookId = repository.playbackState.value?.bookId
                ?: repository.selectedBookIdForDetail.value
            if (!activeBookId.isNullOrBlank()) {
                navController.navigate(
                    WordwaveRoutes.readerRoute(
                        bookId = activeBookId,
                        mode = repository.readerMode.value
                    )
                ) {
                    launchSingleTop = true
                }
            }
            onOpenReaderHandled()
        }
    }

    val navigateToBottomTab: (AppBottomTab) -> Unit = { tab ->
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        when (tab) {
            AppBottomTab.LIBRARY -> {
                if (currentRoute != WordwaveRoutes.LIBRARY) {
                    val popped = navController.popBackStack(
                        route = WordwaveRoutes.LIBRARY,
                        inclusive = false
                    )
                    if (!popped) {
                        navController.navigate(WordwaveRoutes.LIBRARY) {
                            launchSingleTop = true
                        }
                    }
                }
            }

            AppBottomTab.AMBIENT_MUSIC -> {
                if (currentRoute != WordwaveRoutes.AMBIENT_MUSIC) {
                    navController.navigate(WordwaveRoutes.AMBIENT_MUSIC) {
                        popUpTo(WordwaveRoutes.LIBRARY) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                }
            }

            AppBottomTab.VOICES -> {
                if (currentRoute != WordwaveRoutes.VOICES) {
                    navController.navigate(WordwaveRoutes.VOICES) {
                        popUpTo(WordwaveRoutes.LIBRARY) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                }
            }

            AppBottomTab.SETTINGS -> {
                if (currentRoute != WordwaveRoutes.SETTINGS) {
                    navController.navigate(WordwaveRoutes.SETTINGS) {
                        popUpTo(WordwaveRoutes.LIBRARY) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        // Default smooth tab transition (gentle fade + subtle scale)
        enterTransition = {
            fadeIn(
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
            ) + scaleIn(
                initialScale = 0.97f,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            fadeOut(
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            ) + scaleOut(
                targetScale = 0.98f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            )
        },
        popEnterTransition = {
            fadeIn(
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
            ) + scaleIn(
                initialScale = 0.97f,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            fadeOut(
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            ) + scaleOut(
                targetScale = 0.98f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            )
        }
    ) {
        // 1. Màn hình Thư viện (LibraryScreen)
        composable(route = WordwaveRoutes.LIBRARY) {
            LibraryRoute(
                onNavigateToBookDetail = { bookId ->
                    repository.selectBookForDetail(bookId)
                    navController.navigate(WordwaveRoutes.bookDetailRoute(bookId)) {
                        launchSingleTop = true
                    }
                },
                onNavigateToNowPlaying = { bookId ->
                    repository.selectBookForPlayback(bookId)
                    navController.navigate(
                        WordwaveRoutes.readerRoute(
                            bookId = bookId,
                            mode = repository.readerMode.value
                        )
                    ) {
                        launchSingleTop = true
                    }
                },
                onNavigateToTab = navigateToBottomTab
            )
        }

        // 1b. Màn hình Quản lý Nhạc nền (AmbientMusicScreen)
        composable(route = WordwaveRoutes.AMBIENT_MUSIC) {
            AmbientMusicRoute(
                onNavigateToNowPlaying = {
                    val activeBookId = repository.playbackState.value?.bookId
                    navController.navigate(
                        WordwaveRoutes.readerRoute(
                            bookId = activeBookId,
                            mode = repository.readerMode.value
                        )
                    ) {
                        launchSingleTop = true
                    }
                },
                onNavigateToTab = navigateToBottomTab
            )
        }

        // 2. Màn hình Chi tiết sách (BookDetailScreen) — Horizontal Push & Pop Slide
        composable(
            route = WordwaveRoutes.BOOK_DETAIL,
            arguments = listOf(
                navArgument("bookId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            ),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(durationMillis = NAV_ANIM_DURATION, easing = FastOutSlowInEasing),
                    initialOffset = { fullWidth -> fullWidth / 4 }
                ) + fadeIn(
                    animationSpec = tween(durationMillis = NAV_ANIM_DURATION, easing = FastOutSlowInEasing)
                )
            },
            exitTransition = {
                fadeOut(
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                ) + scaleOut(
                    targetScale = 0.96f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                )
            },
            popEnterTransition = {
                fadeIn(
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                ) + scaleIn(
                    initialScale = 0.96f,
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(durationMillis = NAV_ANIM_DURATION, easing = FastOutSlowInEasing),
                    targetOffset = { fullWidth -> fullWidth / 4 }
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                )
            }
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId").orEmpty()
            BookDetailRoute(
                bookId = bookId,
                onBackClick = {
                    if (!navController.popBackStack()) {
                        navController.navigate(WordwaveRoutes.LIBRARY) {
                            launchSingleTop = true
                        }
                    }
                },
                onNavigateToReader = { requestedMode ->
                    repository.setReaderMode(requestedMode)
                    navController.navigate(
                        WordwaveRoutes.readerRoute(
                            bookId = bookId,
                            mode = requestedMode
                        )
                    ) {
                        launchSingleTop = true
                    }
                },
                onNavigateToVoiceManagement = {
                    navController.navigate(WordwaveRoutes.VOICES) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(WordwaveRoutes.SETTINGS) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // 3. Màn hình Đang nghe & Đọc đồng bộ (ReaderScreen) — Vertical Modal Slide Up / Down
        composable(
            route = WordwaveRoutes.READER,
            arguments = listOf(
                navArgument("bookId") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = ReaderMode.READ_ALONG.name
                }
            ),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    initialOffset = { fullHeight -> fullHeight / 5 }
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                )
            },
            exitTransition = {
                fadeOut(
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                )
            },
            popEnterTransition = {
                fadeIn(
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                    targetOffset = { fullHeight -> fullHeight / 5 }
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                )
            }
        ) { backStackEntry ->
            val rawBookId = backStackEntry.arguments?.getString("bookId").orEmpty()
            val rawMode = backStackEntry.arguments?.getString("mode")
            val initialMode = runCatching {
                if (rawMode != null) ReaderMode.valueOf(rawMode) else ReaderMode.READ_ALONG
            }.getOrDefault(ReaderMode.READ_ALONG)

            ReaderRoute(
                bookId = rawBookId.ifBlank { null },
                initialMode = initialMode,
                onBackClick = {
                    if (!navController.popBackStack()) {
                        navController.navigate(WordwaveRoutes.LIBRARY) {
                            launchSingleTop = true
                        }
                    }
                },
                onOpenBookDetailToc = { activeBookId ->
                    repository.selectBookForDetail(activeBookId)
                    val poppedToExistingDetail = navController.popBackStack(
                        route = WordwaveRoutes.BOOK_DETAIL,
                        inclusive = false
                    )
                    if (!poppedToExistingDetail) {
                        navController.navigate(WordwaveRoutes.bookDetailRoute(activeBookId)) {
                            launchSingleTop = true
                        }
                    }
                },
                onNavigateToVoiceManagement = {
                    navController.navigate(WordwaveRoutes.VOICES) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(WordwaveRoutes.SETTINGS) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // 4. Màn hình Quản lý giọng đọc (VoiceManagementScreen)
        composable(route = WordwaveRoutes.VOICES) {
            VoiceManagementRoute(
                onNavigateToNowPlaying = {
                    val activeBookId = repository.playbackState.value?.bookId
                    navController.navigate(
                        WordwaveRoutes.readerRoute(
                            bookId = activeBookId,
                            mode = repository.readerMode.value
                        )
                    ) {
                        launchSingleTop = true
                    }
                },
                onNavigateToTab = navigateToBottomTab
            )
        }

        // 5. Màn hình Cài đặt (SettingsScreen)
        composable(route = WordwaveRoutes.SETTINGS) {
            SettingsRoute(
                onNavigateToNowPlaying = {
                    val activeBookId = repository.playbackState.value?.bookId
                    navController.navigate(
                        WordwaveRoutes.readerRoute(
                            bookId = activeBookId,
                            mode = repository.readerMode.value
                        )
                    ) {
                        launchSingleTop = true
                    }
                },
                onNavigateToVoices = {
                    navController.navigate(WordwaveRoutes.VOICES) {
                        popUpTo(WordwaveRoutes.LIBRARY) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },
                onNavigateToTab = navigateToBottomTab
            )
        }
    }

    if (showOnboarding) {
        WelcomeOnboardingDialog(
            onConfirm = { name, role ->
                repository.completeOnboarding(name, role)
            }
        )
    }
}
