package com.kaori.circletosearch.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.kaori.circletosearch.R
import com.kaori.circletosearch.ocr.ChineseEnglishTextRecognizer
import com.kaori.circletosearch.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val CHATGPT_PACKAGE = "com.openai.chatgpt"
private const val MIN_SELECTION_SIZE_PX = 32f
private const val HANDLE_TOUCH_RADIUS_PX = 56f

private data class SelectionBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun contains(point: Offset): Boolean =
        point.x in left..right && point.y in top..bottom
}

private enum class DragMode { NEW, MOVE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
private enum class OcrDestination { CLIPBOARD, CHATGPT }
private data class OcrRequest(val bitmap: Bitmap, val destination: OcrDestination)

@Composable
fun CircleToSearchScreen(
    screenshot: Bitmap?,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var canvasSize by remember(screenshot) { mutableStateOf(IntSize.Zero) }
    var selection by remember(screenshot) { mutableStateOf<SelectionBox?>(null) }
    var dragMode by remember { mutableStateOf<DragMode?>(null) }
    var dragStart by remember { mutableStateOf(Offset.Zero) }
    var initialSelection by remember { mutableStateOf<SelectionBox?>(null) }
    var imageShareRequest by remember(screenshot) { mutableStateOf<Bitmap?>(null) }
    var ocrRequest by remember(screenshot) { mutableStateOf<OcrRequest?>(null) }
    var imageSaveRequest by remember(screenshot) { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember(screenshot) { mutableStateOf(false) }
    var processingText by remember(screenshot) { mutableStateOf("") }

    BackHandler(onBack = onClose)

    LaunchedEffect(imageShareRequest) {
        val bitmap = imageShareRequest ?: return@LaunchedEffect
        isProcessing = true
        processingText = context.getString(R.string.label_sending_to_chatgpt)
        try {
            shareImageWithChatGpt(context, bitmap)
            onClose()
        } catch (_: Exception) {
            Toast.makeText(context, context.getString(R.string.toast_chatgpt_not_available), Toast.LENGTH_LONG).show()
        } finally {
            imageShareRequest = null
            isProcessing = false
        }
    }
    LaunchedEffect(imageSaveRequest) {
        val bitmap = imageSaveRequest ?: return@LaunchedEffect
        isProcessing = true
        processingText = context.getString(R.string.label_saving_image)
        try {
            val saved = withContext(Dispatchers.IO) { ImageUtils.saveToGallery(context, bitmap) }
            val message = if (saved) {
                context.getString(R.string.toast_image_saved)
            } else {
                context.getString(R.string.toast_image_save_failed)
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(context, context.getString(R.string.toast_image_save_failed), Toast.LENGTH_SHORT).show()
        } finally {
            imageSaveRequest = null
            isProcessing = false
        }
    }

    LaunchedEffect(ocrRequest) {
        val request = ocrRequest ?: return@LaunchedEffect
        isProcessing = true
        processingText = context.getString(R.string.label_scanning_text)
        try {
            val recognizedText = ChineseEnglishTextRecognizer.extractText(request.bitmap)
                .joinToString(separator = "\n") { it.fullText.trim() }
                .trim()
            if (recognizedText.isBlank()) {
                Toast.makeText(context, context.getString(R.string.label_no_text_found), Toast.LENGTH_SHORT).show()
            } else if (request.destination == OcrDestination.CLIPBOARD) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("OCR text", recognizedText))
                Toast.makeText(context, context.getString(R.string.toast_text_copied), Toast.LENGTH_SHORT).show()
            } else {
                shareTextWithChatGpt(context, recognizedText)
                onClose()
            }
        } catch (_: Exception) {
            Toast.makeText(context, context.getString(R.string.label_no_text_found), Toast.LENGTH_SHORT).show()
        } finally {
            ocrRequest = null
            isProcessing = false
        }
    }

    fun selectedBitmap(): Bitmap? = screenshot?.let { source ->
        selection?.let { box -> cropSelection(source, box, canvasSize) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        if (screenshot == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            val image = remember(screenshot) { screenshot.asImageBitmap() }
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(screenshot, canvasSize, isProcessing) {
                        detectDragGestures(
                            onDragStart = { point ->
                                if (isProcessing) return@detectDragGestures
                                dragStart = point
                                initialSelection = selection
                                dragMode = selection?.let { existing -> dragModeFor(existing, point) } ?: DragMode.NEW
                                if (dragMode == DragMode.NEW) {
                                    selection = selectionFromPoints(point, point, canvasSize)
                                }
                            },
                            onDrag = { change, _ ->
                                if (isProcessing) return@detectDragGestures
                                val start = dragStart
                                val original = initialSelection
                                selection = when (dragMode) {
                                    DragMode.NEW, null -> selectionFromPoints(start, change.position, canvasSize)
                                    DragMode.MOVE -> original?.moveBy(
                                        change.position.x - start.x,
                                        change.position.y - start.y,
                                        canvasSize
                                    )
                                    DragMode.TOP_LEFT -> original?.copy(
                                        left = change.position.x.coerceIn(0f, original.right - MIN_SELECTION_SIZE_PX),
                                        top = change.position.y.coerceIn(0f, original.bottom - MIN_SELECTION_SIZE_PX)
                                    )
                                    DragMode.TOP_RIGHT -> original?.copy(
                                        right = change.position.x.coerceIn(original.left + MIN_SELECTION_SIZE_PX, canvasSize.width.toFloat()),
                                        top = change.position.y.coerceIn(0f, original.bottom - MIN_SELECTION_SIZE_PX)
                                    )
                                    DragMode.BOTTOM_LEFT -> original?.copy(
                                        left = change.position.x.coerceIn(0f, original.right - MIN_SELECTION_SIZE_PX),
                                        bottom = change.position.y.coerceIn(original.top + MIN_SELECTION_SIZE_PX, canvasSize.height.toFloat())
                                    )
                                    DragMode.BOTTOM_RIGHT -> original?.copy(
                                        right = change.position.x.coerceIn(original.left + MIN_SELECTION_SIZE_PX, canvasSize.width.toFloat()),
                                        bottom = change.position.y.coerceIn(original.top + MIN_SELECTION_SIZE_PX, canvasSize.height.toFloat())
                                    )
                                }
                            },
                            onDragEnd = {
                                if (selection?.isValid() == false) selection = null
                                dragMode = null
                                initialSelection = null
                            },
                            onDragCancel = {
                                dragMode = null
                                initialSelection = null
                            }
                        )
                    }
            ) {
                val scale = max(size.width / screenshot.width, size.height / screenshot.height)
                val drawWidth = screenshot.width * scale
                val drawHeight = screenshot.height * scale
                val imageOffset = Offset(
                    (size.width - drawWidth) / 2f,
                    (size.height - drawHeight) / 2f
                )
                drawImage(
                    image = image,
                    dstOffset = IntOffset(imageOffset.x.roundToInt(), imageOffset.y.roundToInt()),
                    dstSize = IntSize(drawWidth.roundToInt(), drawHeight.roundToInt())
                )
                selection?.let { box ->
                    drawRect(
                        color = Color(0xFF10A37F),
                        topLeft = Offset(box.left, box.top),
                        size = Size(box.width, box.height),
                        style = Stroke(width = 4.dp.toPx())
                    )
                    listOf(
                        Offset(box.left, box.top),
                        Offset(box.right, box.top),
                        Offset(box.left, box.bottom),
                        Offset(box.right, box.bottom)
                    ).forEach { handle ->
                        drawCircle(Color(0xFF10A37F), radius = 10.dp.toPx(), center = handle)
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp, start = 20.dp, end = 20.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_chatgpt),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close))
                }
            }
        }

        if (selection?.isValid() == true && !isProcessing) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 24.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { selectedBitmap()?.let { imageShareRequest = it } }) {
                            Text(stringResource(R.string.action_share_image_chatgpt))
                        }
                        TextButton(onClick = { selectedBitmap()?.let { imageSaveRequest = it } }) {
                            Text(stringResource(R.string.action_save_image))
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { selectedBitmap()?.let { ocrRequest = OcrRequest(it, OcrDestination.CLIPBOARD) } }) {
                            Text(stringResource(R.string.action_ocr_copy))
                        }
                        TextButton(onClick = { selectedBitmap()?.let { ocrRequest = OcrRequest(it, OcrDestination.CHATGPT) } }) {
                            Text(stringResource(R.string.action_ocr_chatgpt))
                        }
                    }
                }
            }
        }

        if (isProcessing) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(processingText)
                }
            }
        }
    }
}

private fun SelectionBox.isValid(): Boolean =
    width >= MIN_SELECTION_SIZE_PX && height >= MIN_SELECTION_SIZE_PX

private fun dragModeFor(box: SelectionBox, point: Offset): DragMode {
    fun near(handle: Offset): Boolean {
        val dx = point.x - handle.x
        val dy = point.y - handle.y
        return dx * dx + dy * dy <= HANDLE_TOUCH_RADIUS_PX * HANDLE_TOUCH_RADIUS_PX
    }
    return when {
        near(Offset(box.left, box.top)) -> DragMode.TOP_LEFT
        near(Offset(box.right, box.top)) -> DragMode.TOP_RIGHT
        near(Offset(box.left, box.bottom)) -> DragMode.BOTTOM_LEFT
        near(Offset(box.right, box.bottom)) -> DragMode.BOTTOM_RIGHT
        box.contains(point) -> DragMode.MOVE
        else -> DragMode.NEW
    }
}

private fun selectionFromPoints(start: Offset, end: Offset, canvasSize: IntSize): SelectionBox {
    val maxX = canvasSize.width.toFloat()
    val maxY = canvasSize.height.toFloat()
    return SelectionBox(
        left = min(start.x, end.x).coerceIn(0f, maxX),
        top = min(start.y, end.y).coerceIn(0f, maxY),
        right = max(start.x, end.x).coerceIn(0f, maxX),
        bottom = max(start.y, end.y).coerceIn(0f, maxY)
    )
}

private fun SelectionBox.moveBy(deltaX: Float, deltaY: Float, canvasSize: IntSize): SelectionBox {
    val maxLeft = canvasSize.width - width
    val maxTop = canvasSize.height - height
    val newLeft = (left + deltaX).coerceIn(0f, maxLeft.toFloat())
    val newTop = (top + deltaY).coerceIn(0f, maxTop.toFloat())
    return SelectionBox(newLeft, newTop, newLeft + width, newTop + height)
}

private fun cropSelection(
    screenshot: Bitmap,
    selection: SelectionBox,
    canvasSize: IntSize
): Bitmap? {
    if (canvasSize.width == 0 || canvasSize.height == 0 || !selection.isValid()) return null
    val scale = max(
        canvasSize.width.toFloat() / screenshot.width,
        canvasSize.height.toFloat() / screenshot.height
    )
    val imageWidth = screenshot.width * scale
    val imageHeight = screenshot.height * scale
    val offsetX = (canvasSize.width - imageWidth) / 2f
    val offsetY = (canvasSize.height - imageHeight) / 2f

    fun toBitmapX(x: Float): Int = ((x - offsetX) / scale).roundToInt().coerceIn(0, screenshot.width)
    fun toBitmapY(y: Float): Int = ((y - offsetY) / scale).roundToInt().coerceIn(0, screenshot.height)

    val left = min(toBitmapX(selection.left), toBitmapX(selection.right))
    val top = min(toBitmapY(selection.top), toBitmapY(selection.bottom))
    val right = max(toBitmapX(selection.left), toBitmapX(selection.right))
    val bottom = max(toBitmapY(selection.top), toBitmapY(selection.bottom))
    if (right <= left || bottom <= top) return null
    return ImageUtils.cropBitmap(screenshot, Rect(left, top, right, bottom))
}

private suspend fun shareImageWithChatGpt(context: Context, bitmap: Bitmap) {
    val imageUri = withContext(Dispatchers.IO) {
        val imagePath = ImageUtils.saveBitmap(context, bitmap, "chatgpt_selection.png")
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(imagePath))
    }
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        clipData = ClipData.newRawUri("circle_selection", imageUri)
        setPackage(CHATGPT_PACKAGE)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(shareIntent)
}

private fun shareTextWithChatGpt(context: Context, text: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        setPackage(CHATGPT_PACKAGE)
    }
    context.startActivity(shareIntent)
}
