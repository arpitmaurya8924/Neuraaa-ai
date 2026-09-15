package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeuraCyan
import com.example.ui.theme.NeuraPurple

@Composable
fun NeuraLogoBadge(
    modifier: Modifier = Modifier,
    sizeDp: Int = 36
) {
    val infiniteTransition = rememberInfiniteTransition(label = "neura_glow")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(NeuraCyan.copy(alpha = 0.85f), NeuraPurple.copy(alpha = 0.85f))
                )
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "NEURA Logo",
            tint = Color.White,
            modifier = Modifier.size((sizeDp * 0.55).dp)
        )
    }
}

@Composable
fun NeuraBrandHeader(
    modifier: Modifier = Modifier,
    subtitle: String = "Your Intelligent Companion",
    showTagline: Boolean = true
) {
    Row(
        modifier = modifier.testTag("neura_brand_header"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NeuraLogoBadge(sizeDp = 40)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "NEURA",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (showTagline) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun NeuraStatusChip(
    isOnline: Boolean,
    hasApiKey: Boolean,
    modifier: Modifier = Modifier
) {
    val chipColor = when {
        !isOnline -> Color(0xFFEF4444)
        hasApiKey -> NeuraCyan
        else -> Color(0xFFF59E0B)
    }

    val chipText = when {
        !isOnline -> "Offline"
        hasApiKey -> "Gemini Active"
        else -> "Smart Engine"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(chipColor.copy(alpha = 0.15f))
            .border(1.dp, chipColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(chipColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = chipText,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = chipColor
        )
    }
}
