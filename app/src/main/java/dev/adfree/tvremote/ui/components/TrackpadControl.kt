package dev.adfree.tvremote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.adfree.tvremote.model.RemoteKey
import dev.adfree.tvremote.ui.theme.BorderDark
import dev.adfree.tvremote.ui.theme.SurfaceLightDark
import dev.adfree.tvremote.ui.theme.TextSecondary
import kotlin.math.abs

@Composable
fun TrackpadControl(
    onKeyPress: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier
) {
    var accumulatedDrag by remember { mutableStateOf(Offset.Zero) }
    val threshold = 70f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceLightDark)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onKeyPress(RemoteKey.DPAD_CENTER)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { accumulatedDrag = Offset.Zero },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDrag += dragAmount

                        if (abs(accumulatedDrag.x) > threshold) {
                            if (accumulatedDrag.x > 0) {
                                onKeyPress(RemoteKey.DPAD_RIGHT)
                            } else {
                                onKeyPress(RemoteKey.DPAD_LEFT)
                            }
                            accumulatedDrag = Offset.Zero
                        } else if (abs(accumulatedDrag.y) > threshold) {
                            if (accumulatedDrag.y > 0) {
                                onKeyPress(RemoteKey.DPAD_DOWN)
                            } else {
                                onKeyPress(RemoteKey.DPAD_UP)
                            }
                            accumulatedDrag = Offset.Zero
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Trackpad",
                color = TextSecondary,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Swipe to Navigate • Tap to Select",
                color = TextSecondary.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}

