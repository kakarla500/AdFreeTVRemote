package dev.adfree.tvremote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.adfree.tvremote.model.RemoteKey
import dev.adfree.tvremote.ui.theme.BorderDark
import dev.adfree.tvremote.ui.theme.PrimaryBlue
import dev.adfree.tvremote.ui.theme.SurfaceLightDark
import dev.adfree.tvremote.ui.theme.TextPrimary

@Composable
fun DpadControl(
    onKeyPress: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(260.dp)
            .clip(CircleShape)
            .background(SurfaceLightDark),
        contentAlignment = Alignment.Center
    ) {
        // UP BUTTON
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(80.dp)
                .clickable { onKeyPress(RemoteKey.DPAD_UP) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Up",
                tint = TextPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        // DOWN BUTTON
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(80.dp)
                .clickable { onKeyPress(RemoteKey.DPAD_DOWN) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Down",
                tint = TextPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        // LEFT BUTTON
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(80.dp)
                .clickable { onKeyPress(RemoteKey.DPAD_LEFT) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Left",
                tint = TextPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        // RIGHT BUTTON
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(80.dp)
                .clickable { onKeyPress(RemoteKey.DPAD_RIGHT) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Right",
                tint = TextPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        // CENTER OK BUTTON
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(PrimaryBlue)
                .clickable { onKeyPress(RemoteKey.DPAD_CENTER) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "OK",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

