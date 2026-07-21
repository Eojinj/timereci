package com.timereci.focus.ui.detail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timereci.focus.ui.components.IconActionButton
import com.timereci.focus.ui.components.PhotoCard
import com.timereci.focus.ui.components.grainyBackground
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.MonoFamily
import kotlinx.coroutines.launch

/** Single-receipt detail + export. Free tier: standard-resolution PNG, no watermark. */
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
                FormatRow()
                Spacer(Modifier.height(18.dp))
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
                Spacer(Modifier.height(12.dp))
                Text(
                    "표준 해상도 단건 내보내기는 무료.\n고해상도·무손실·연말 롤북은 Pro.",
                    color = FocusColors.Muted2,
                    fontFamily = MonoFamily,
                    fontSize = 10.5.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun FormatRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Free option (selected)
        Column(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(11.dp))
                .background(FocusColors.Paper)
                .border(1.5.dp, FocusColors.AccentSky, RoundedCornerShape(11.dp))
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("PNG · 표준", color = FocusColors.Ink, fontFamily = MonoFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text("무료 · 워터마크 없음", color = FocusColors.Muted, fontSize = 10.5.sp)
        }
        // Pro option (locked)
        Column(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(11.dp))
                .background(FocusColors.Mist)
                .border(1.dp, FocusColors.LineSoft, RoundedCornerShape(11.dp))
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("고해상도", color = FocusColors.Muted, fontFamily = MonoFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(FocusColors.AccentInk)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) { Text("PRO", color = FocusColors.NightInk, fontSize = 9.sp, letterSpacing = 1.sp) }
            }
            Text("무손실 · 연말 롤북", color = FocusColors.Muted2, fontSize = 10.5.sp)
        }
    }
}
