package com.bookisham.app.ui.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bookisham.app.LocalApp
import com.bookisham.app.data.BookOpen
import com.bookisham.app.data.UnauthorizedException
import com.bookisham.app.ui.components.IconPill
import com.bookisham.app.ui.components.LoadingScreen
import com.bookisham.app.ui.components.Pill
import com.bookisham.app.ui.components.PillStyle
import com.bookisham.app.ui.rememberViewModel
import com.bookisham.app.ui.theme.Ember
import com.bookisham.app.ui.theme.Night
import com.bookisham.app.ui.theme.NightLine
import com.bookisham.app.ui.theme.NightPanel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The reading surface: every page of the book in one column, scrolled.
 *
 * Each page has a slot of the right shape from the start, so the column is
 * the full length of the book before anything has loaded. A slot near the
 * viewport fetches its page as an opaque byte stream, unwraps it with the
 * key the server handed this session, decodes it and shows it. Slots that
 * scroll away are dropped by the lazy column; the repository keeps a bounded
 * cache behind them. Nothing is written to disk.
 *
 * What the reader may not do: the window is flagged secure, so the system
 * blocks screenshots and screen recording and blanks the app in the recent
 * apps switcher. While the app is not in front, the pages are covered. The
 * email of the reader is tiled faintly across every page.
 */
private const val PAGE_MAX_WIDTH_DP = 920

@Composable
fun ReaderScreen(bookId: String, onBack: () -> Unit, onSignedOut: () -> Unit) {
    val app = LocalApp.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    // Tablets and large phones get the sharpest render; the rest a lighter one.
    val scale = remember { if (configuration.screenWidthDp * density.density >= 1600f) 3 else 2 }
    val vm = rememberViewModel(key = "reader:$bookId") { ReaderViewModel(app, bookId, scale) }

    ReaderWindow()
    if (vm.signedOut) LaunchedEffect(Unit) { onSignedOut() }

    Box(Modifier.fillMaxSize().background(Night)) {
        val book = vm.book
        val error = vm.error
        when {
            book != null -> ReaderSurface(vm, book, onBack)
            error != null -> Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(error, color = Color.White, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Pill("Try again", onClick = { vm.open() }, style = PillStyle.Dark)
                    Pill("Library", onClick = onBack, style = PillStyle.Dark)
                }
            }
            else -> LoadingScreen(night = true)
        }
    }
}

/** FLAG_SECURE while the reader is open, and light status bar icons over the night background. */
@Composable
private fun ReaderWindow() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            controller?.isAppearanceLightStatusBars = true
            controller?.isAppearanceLightNavigationBars = true
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun ReaderSurface(vm: ReaderViewModel, book: BookOpen, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = vm.page - 1)
    var zoom by rememberSaveable { mutableStateOf(1f) }
    var covered by remember { mutableStateOf(false) }
    var touchStamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var chromeVisible by remember { mutableStateOf(true) }
    val textMeasurer = rememberTextMeasurer()
    val watermark = remember(book.watermark) {
        textMeasurer.measure(book.watermark, TextStyle(fontSize = 16.sp, color = Color.Black))
    }

    // The bars show on any touch and fade three seconds later.
    LaunchedEffect(touchStamp) {
        chromeVisible = true
        delay(3000)
        chromeVisible = false
    }

    // Cover the pages whenever the app is not in front.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> covered = true
                Lifecycle.Event.ON_RESUME -> covered = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // The current page is the slot crossing the middle of the viewport.
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - middle) }?.index
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { vm.onPage(it + 1) }
    }

    // Keep the current page in place once a zoom has settled.
    var settledZoom by remember { mutableStateOf(zoom) }
    LaunchedEffect(zoom) {
        delay(150)
        if (settledZoom != zoom) {
            settledZoom = zoom
            listState.scrollToItem(vm.page - 1)
        }
    }

    fun goTo(n: Int) {
        val target = n.coerceIn(1, book.pages)
        scope.launch { listState.animateScrollToItem(target - 1) }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Night)
            .pointerInput(Unit) {
                // Seen before the column: a touch shows the bars; two fingers zoom.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    touchStamp = System.currentTimeMillis()
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pressed = event.changes.count { it.pressed }
                        if (pressed >= 2) {
                            val change = event.calculateZoom()
                            if (change != 1f) zoom = (zoom * change).coerceIn(0.5f, 3f)
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        val stageWidth = maxWidth
        val pageWidth: Dp = ((stageWidth - 32.dp).coerceAtMost(PAGE_MAX_WIDTH_DP.dp) * zoom).coerceAtLeast(160.dp)
        val hScroll = rememberScrollState()

        Box(Modifier.fillMaxSize().horizontalScroll(hScroll)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.width(pageWidth + 32.dp).fillMaxHeight(),
                contentPadding = PaddingValues(top = 84.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(count = book.pages, key = { it }) { index ->
                    val n = index + 1
                    PageSlot(n = n, width = pageWidth, ratio = vm.ratioOf(n), watermark = watermark, vm = vm)
                }
            }
        }

        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            TopBar(book = book, zoom = zoom, onBack = onBack, onZoom = { zoom = it })
        }

        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            BottomBar(page = vm.page, pages = book.pages, onGo = { goTo(it) })
        }

        val error = vm.error
        if (error != null) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 128.dp)
                    .shadow(16.dp, RoundedCornerShape(16.dp))
                    .background(NightPanel, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(error, color = Color.White, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(12.dp))
                Pill("Dismiss", onClick = { vm.error = null }, style = PillStyle.Dark, compact = true)
            }
        }

        if (covered) {
            Box(
                Modifier.fillMaxSize().background(Night).pointerInput(Unit) {},
                contentAlignment = Alignment.Center,
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Come back to keep reading", style = MaterialTheme.typography.headlineMedium, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "The pages are hidden while this app is not in front.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun PageSlot(n: Int, width: Dp, ratio: Float, watermark: TextLayoutResult, vm: ReaderViewModel) {
    var bitmap by remember(n) { mutableStateOf(vm.cached(n)) }
    var failed by remember(n) { mutableStateOf(false) }

    LaunchedEffect(n, failed) {
        if (bitmap == null && !failed) {
            try {
                bitmap = vm.load(n)
            } catch (e: UnauthorizedException) {
                vm.signedOut = true
            } catch (e: Exception) {
                failed = true
                vm.error = e.message ?: "A page could not be loaded. Check your connection and try again."
            }
        }
    }

    Column(horizontalAlignment = Alignment.End) {
        Box(
            Modifier
                .width(width)
                .aspectRatio(ratio)
                .shadow(24.dp, spotColor = Color.Black, ambientColor = Color.Black)
                .background(Color.White)
                .drawWithContent {
                    drawContent()
                    drawWatermark(watermark)
                },
            contentAlignment = Alignment.Center,
        ) {
            val bmp = bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.High,
                )
            } else if (failed) {
                Pill("Retry", onClick = { failed = false }, style = PillStyle.Primary, compact = true)
            }
        }
        Text(
            "$n",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** The reader tiles their own email faintly across every page: what leaves, leaves with a name on it. */
private fun DrawScope.drawWatermark(layout: TextLayoutResult) {
    if (layout.layoutInput.text.isBlank()) return
    val tileW = 340.dp.toPx()
    val tileH = 240.dp.toPx()
    var y = -tileH / 2
    while (y < size.height) {
        var x = -tileW / 2
        while (x < size.width) {
            withTransform({ rotate(-24f, pivot = Offset(x + tileW / 2, y + tileH / 2)) }) {
                drawText(layout, topLeft = Offset(x + 20.dp.toPx(), y + 110.dp.toPx()), alpha = 0.16f)
            }
            x += tileW
        }
        y += tileH
    }
}

@Composable
private fun TopBar(book: BookOpen, zoom: Float, onBack: () -> Unit, onZoom: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Night.copy(alpha = 0.92f))) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconPill(Icons.AutoMirrored.Filled.ArrowBack, "Back to library", onClick = onBack)
            Column(Modifier.weight(1f)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (book.author.isNotBlank()) {
                    Text(book.author, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconPill(Icons.Filled.ZoomOut, "Zoom out", onClick = { onZoom((zoom - 0.15f).coerceAtLeast(0.5f)) })
            Text("${(zoom * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
            IconPill(Icons.Filled.ZoomIn, "Zoom in", onClick = { onZoom((zoom + 0.15f).coerceAtMost(3f)) })
        }
        HorizontalDivider(color = NightLine)
    }
}

@Composable
private fun BottomBar(page: Int, pages: Int, onGo: (Int) -> Unit) {
    val focus = LocalFocusManager.current
    var text by remember(page) { mutableStateOf(page.toString()) }

    Column(Modifier.fillMaxWidth().background(Night.copy(alpha = 0.92f))) {
        HorizontalDivider(color = NightLine)
        Box(Modifier.fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.1f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth((page.toFloat() / pages).coerceIn(0f, 1f)).background(Ember))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconPill(Icons.Filled.KeyboardArrowUp, "Previous page", onClick = { onGo(page - 1) }, enabled = page > 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Page", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                BasicTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() }.take(6) },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center),
                    cursorBrush = SolidColor(Ember),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        text.toIntOrNull()?.let(onGo)
                        focus.clearFocus()
                    }),
                    modifier = Modifier
                        .width(60.dp)
                        .background(NightPanel, RoundedCornerShape(8.dp))
                        .border(1.dp, NightLine, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
                Text("of $pages", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            }
            IconPill(Icons.Filled.KeyboardArrowDown, "Next page", onClick = { onGo(page + 1) }, enabled = page < pages)
        }
    }
}
