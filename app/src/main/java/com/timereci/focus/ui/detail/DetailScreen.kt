package com.timereci.focus.ui.detail

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.timereci.focus.data.PhotoStorage
import com.timereci.focus.ui.components.IconActionButton
import com.timereci.focus.ui.i18n.LocalStrings
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.PhotoTones
import com.timereci.focus.ui.theme.patternPlaceholder
import kotlinx.coroutines.launch

/**
 * Session Detail (Merci v5 screen 9): opened from a History tile. Share replaces the old
 * save-to-gallery-only button (the system sheet already offers Save Image, Messages, AirDrop).
 */
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val card by viewModel.card.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    var editingNote by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val brush = Brush.radialGradient(
                    0f to Color(0xFF6FA8DE),
                    0.5f to Color(0xFFBEE0F5),
                    1f to Color(0xFFF7F9FB),
                    center = Offset(size.width / 2f, size.height * 0.05f),
                    radius = size.minDimension * 0.9f,
                )
                onDrawBehind { drawRect(brush) }
            }
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconActionButton(icon = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = strings.back, onClick = onBack, size = 38.dp)
                IconActionButton(
                    icon = Icons.Outlined.Share,
                    contentDescription = strings.share,
                    size = 38.dp,
                    onClick = {
                        scope.launch {
                            val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                            val uri = Exporter.saveToGallery(context, bitmap, "merci_${card?.id ?: 0}")
                            if (uri != null) {
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(send, null))
                            } else {
                                Toast.makeText(context, strings.shareFailed, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                )
            }

            val current = card
            if (current != null) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    val photo = current.photos.first()
                    Box(
                        Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(18.dp))
                            .then(
                                if (photo.fileName == null) Modifier.patternPlaceholder(photo.toneIndex)
                                else Modifier.background(PhotoTones.brush(photo.toneIndex)),
                            )
                            .drawWithContent {
                                graphicsLayer.record { this@drawWithContent.drawContent() }
                                drawLayer(graphicsLayer)
                            },
                    ) {
                        photo.fileName?.let { name ->
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(PhotoStorage.fileIn(context, name)).crossfade(true).build(),
                                contentDescription = current.task,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Text(current.task.ifBlank { strings.focusFallback }, color = FocusColors.Ink, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                        Text(current.stamp, color = FocusColors.Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                        current.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                            Text(comment, color = FocusColors.Ink, fontSize = 16.5.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 12.dp))
                        }
                    }

                    SectionLabel(strings.detailsSection)
                    Column(
                        Modifier
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xEBFFFFFF)),
                    ) {
                        DetailRow(strings.focused, strings.minutes((current.focusMs / 60_000L).coerceAtLeast(1).toInt()))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(FocusColors.LineSoft))
                        DetailRow(strings.planned, current.focus)
                    }

                    Spacer(Modifier.height(16.dp))
                    Column(
                        Modifier
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xEBFFFFFF)),
                    ) {
                        Text(
                            strings.editNote,
                            color = FocusColors.AccentBlue,
                            fontSize = 16.5.sp,
                            modifier = Modifier.fillMaxWidth().clickable { editingNote = true }.padding(horizontal = 16.dp, vertical = 14.dp),
                        )
                        Box(Modifier.fillMaxWidth().height(1.dp).background(FocusColors.LineSoft))
                        Text(
                            strings.deleteSession,
                            color = FocusColors.Danger,
                            fontSize = 16.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().clickable { confirmDelete = true }.padding(horizontal = 16.dp, vertical = 14.dp),
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (editingNote) {
        val current = card
        var text by remember(current?.id) { mutableStateOf(current?.comment.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editingNote = false },
            title = { Text(strings.editNote, fontWeight = FontWeight.Bold) },
            text = {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(FocusColors.Mist)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        textStyle = TextStyle(color = FocusColors.Ink, fontSize = 15.sp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                IconActionButton(icon = Icons.Outlined.Check, contentDescription = strings.save, accent = true, size = 40.dp, onClick = {
                    viewModel.updateNote(text)
                    editingNote = false
                })
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(strings.deleteSessionTitle, fontWeight = FontWeight.Bold) },
            text = { Text(strings.deleteSessionBody, color = FocusColors.Muted) },
            confirmButton = {
                IconActionButton(icon = Icons.Outlined.DeleteOutline, contentDescription = strings.delete, size = 40.dp, onClick = {
                    viewModel.delete(onBack)
                })
            },
            dismissButton = {
                IconActionButton(icon = Icons.Outlined.Close, contentDescription = strings.cancel, size = 40.dp, onClick = { confirmDelete = false })
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = FocusColors.Muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 6.dp, bottom = 8.dp, start = 20.dp),
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = FocusColors.Ink, fontSize = 16.sp)
        Text(value, color = FocusColors.Muted, fontSize = 15.5.sp)
    }
}
