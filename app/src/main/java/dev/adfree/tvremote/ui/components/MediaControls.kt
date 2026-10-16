package dev.adfree.tvremote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.adfree.tvremote.model.RemoteKey
import dev.adfree.tvremote.ui.theme.AccentRed
import dev.adfree.tvremote.ui.theme.SurfaceLightDark
import dev.adfree.tvremote.ui.theme.TextPrimary

@Composable
fun SystemNavRow(
    onKeyPress: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RemoteRoundButton(
            icon = Icons.Default.ArrowBack,
            label = "Back",
            onClick = { onKeyPress(RemoteKey.BACK) }
        )
        RemoteRoundButton(
            icon = Icons.Default.Home,
            label = "Home",
            onClick = { onKeyPress(RemoteKey.HOME) }
        )
        RemoteRoundButton(
            icon = Icons.Default.Settings,
            label = "Settings",
            onClick = { onKeyPress(RemoteKey.SETTINGS) }
        )
    }
}

@Composable
fun VolumeAndMediaControls(
    onKeyPress: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Volume Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceLightDark)
                .padding(vertical = 8.dp, horizontal = 12.dp)
        ) {
            IconButtonBox(icon = Icons.Default.VolumeUp, onClick = { onKeyPress(RemoteKey.VOLUME_UP) })
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "VOL", color = TextPrimary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(8.dp))
            IconButtonBox(icon = Icons.Default.VolumeDown, onClick = { onKeyPress(RemoteKey.VOLUME_DOWN) })
        }

        // Center Media Controls
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RemoteRoundButton(
                icon = Icons.Default.PlayArrow,
                label = "Play",
                onClick = { onKeyPress(RemoteKey.PLAY_PAUSE) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            RemoteRoundButton(
                icon = Icons.Default.VolumeMute,
                label = "Mute",
                onClick = { onKeyPress(RemoteKey.VOLUME_MUTE) }
            )
        }

        // Channel Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceLightDark)
                .padding(vertical = 8.dp, horizontal = 12.dp)
        ) {
            IconButtonBox(icon = Icons.Default.KeyboardArrowUp, onClick = { onKeyPress(RemoteKey.CHANNEL_UP) })
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "CH", color = TextPrimary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(8.dp))
            IconButtonBox(icon = Icons.Default.KeyboardArrowDown, onClick = { onKeyPress(RemoteKey.CHANNEL_DOWN) })
        }
    }
}

@Composable
fun RemoteRoundButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TextPrimary
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(SurfaceLightDark)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun IconButtonBox(
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}

