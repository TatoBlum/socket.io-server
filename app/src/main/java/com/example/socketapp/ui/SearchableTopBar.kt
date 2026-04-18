package com.example.socketapp.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Reusable top bar that morphs between a centered title and an inline search input.
 *
 * The caller owns [isSearchMode] and [searchQuery] so the same state can drive the screen body.
 * Back behavior splits in two callbacks: [onCloseSearch] fires when back is tapped while in
 * search mode (caller should flip [isSearchMode] back and clear [searchQuery]); [onBack] fires
 * only for "real" back navigation from titles mode.
 * Autofocus on the input is handled internally and intentionally deferred to let the morph
 * animation settle before the keyboard slides in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableTopBar(
    title: String,
    isSearchMode: Boolean,
    searchQuery: String,
    onBack: () -> Unit,
    onCloseSearch: () -> Unit,
    onOpenSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    showNavigationIcon: Boolean = true,
    searchPlaceholder: String = "Buscar",
    extraActions: @Composable RowScope.() -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchMode) {
        if (isSearchMode) {
            delay(FOCUS_DEFER_MS)
            focusRequester.requestFocus()
        }
    }

    CenterAlignedTopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
        navigationIcon = {
            if (showNavigationIcon) {
                IconButton(onClick = { if (isSearchMode) onCloseSearch() else onBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "back",
                    )
                }
            }
        },
        title = {
            AnimatedContent(
                targetState = isSearchMode,
                transitionSpec = {
                    val opening = targetState
                    val direction = if (opening) 1 else -1
                    val enter = slideInHorizontally(
                        animationSpec = tween(durationMillis = SLIDE_MS, easing = FastOutSlowInEasing),
                    ) { fullWidth -> (fullWidth / 4) * direction } +
                        fadeIn(tween(durationMillis = SLIDE_MS, easing = FastOutSlowInEasing))
                    val exit = slideOutHorizontally(
                        animationSpec = tween(durationMillis = SLIDE_MS, easing = FastOutSlowInEasing),
                    ) { fullWidth -> (-fullWidth / 4) * direction } +
                        fadeOut(tween(durationMillis = EXIT_FADE_MS, easing = FastOutSlowInEasing))
                    enter.togetherWith(exit).using(
                        SizeTransform(clip = false) { _, _ ->
                            tween(durationMillis = SLIDE_MS, easing = FastOutSlowInEasing)
                        }
                    )
                },
                label = "searchable-topbar-title",
            ) { searchMode ->
                if (searchMode) {
                    SearchInput(
                        value = searchQuery,
                        placeholder = searchPlaceholder,
                        focusRequester = focusRequester,
                        onValueChange = onQueryChange,
                    )
                } else {
                    TitleLabel(text = title)
                }
            }
        },
        actions = {
            Crossfade(
                targetState = isSearchMode,
                animationSpec = tween(durationMillis = ENTER_MS, easing = FastOutSlowInEasing),
                label = "searchable-topbar-search-icon",
            ) { searchMode ->
                if (!searchMode) {
                    IconButton(onClick = onOpenSearch) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "search",
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(ICON_BUTTON_SIZE))
                }
            }
            extraActions()
        },
    )
}

@Composable
private fun TitleLabel(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 22.sp,
            lineHeight = 26.4.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        ),
    )
}

@Composable
private fun SearchInput(
    value: String,
    placeholder: String,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
//        cursorBrush = SolidColor(BomboTheme.colors.iconPrimary),
        textStyle = TextStyle(
            fontSize = 18.sp,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = TextStyle(
                            fontSize = 18.sp,
                        ),
                    )
                }
                innerTextField()
            }
        },
    )
}

private const val ENTER_MS = 280
private const val SLIDE_MS = 320
private const val EXIT_FADE_MS = 200
private const val FOCUS_DEFER_MS = 180L
private val ICON_BUTTON_SIZE = 48.dp
