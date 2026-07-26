package com.timereci.focus.ui.detail

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timereci.focus.ui.components.IconActionButton
import com.timereci.focus.ui.components.PhotoCard
import com.timereci.focus.ui.components.grainyBackground
import com.timereci.focus.ui.theme.FocusColors
import kotlinx.coroutines.launch

/** A single record, shown large, with a save-to-gallery export. */
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val card by viewModel.card.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    Column(
        Modifier
            .fillMaxSize()
            .grainyBackground(
                base = FocusColors.BaseLight,
                blob = FocusColors.AccentDeep.copy(alpha = 0.10f),
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        // Header — icons only, no title text.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconActionButton(icon = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", onClick = onBack, size = 38.dp)
            Spacer(Modifier.weight(1f))
            IconActionButton(icon = Icons.Outlined.DeleteOutline, contentDescription = "삭제", onClick = { viewModel.delete(onBack) }, size = 38.dp)
        }

        val current = card
        if (current != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Record the card into a graphics layer so it can be exported as a bitmap.
                Box(
                    Modifier
                        .width(280.dp)
                        .height(380.dp)
                        .drawWithContent {
                            graphicsLayer.record { this@drawWithContent.drawContent() }
                            drawLayer(graphicsLayer)
                        },
                ) {
                    PhotoCard(
                        photos = current.photos,
                        stamp = current.stamp,
                        task = current.task,
                        focus = current.focus,
                        cornerRadius = 18.dp,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Column(Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                IconActionButton(
                    icon = Icons.Outlined.Download,
                    contentDescription = "이미지 저장",
                    onClick = {
                        scope.launch {
                            val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                            val ok = Exporter.saveToGallery(
                                context = context,
                                bitmap = bitmap,
                                displayName = "focus_${current.id}",
                            )
                            Toast.makeText(
                                context,
                                if (ok) "갤러리에 저장했어요" else "저장에 실패했어요",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    accent = true,
                    size = 60.dp,
                )
            }
        }
    }
}

