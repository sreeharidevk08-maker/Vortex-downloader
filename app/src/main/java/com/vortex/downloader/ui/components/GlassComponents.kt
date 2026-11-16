package com.vortex.downloader.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vortex.downloader.ui.theme.LocalVortexGlass

@Composable
fun VortexMeshBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalVortexGlass.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(glass.backgroundBase)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glass.accentBlue.copy(alpha = if (glass.isDark) 0.22f else 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.15f, size.height * 0.12f),
                    radius = size.width * 0.75f
                ),
                radius = size.width * 0.75f,
                center = Offset(size.width * 0.15f, size.height * 0.12f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glass.accentViolet.copy(alpha = if (glass.isDark) 0.20f else 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.35f),
                    radius = size.width * 0.70f
                ),
                radius = size.width * 0.70f,
                center = Offset(size.width * 0.88f, size.height * 0.35f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glass.accentCyan.copy(alpha = if (glass.isDark) 0.12f else 0.09f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.35f, size.height * 0.88f),
                    radius = size.width * 0.65f
                ),
                radius = size.width * 0.65f,
                center = Offset(size.width * 0.35f, size.height * 0.88f)
            )
        }
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val glass = LocalVortexGlass.current
    val shape = RoundedCornerShape(cornerRadius)
    val baseModifier = modifier
        .shadow(
            elevation = if (glass.isDark) 14.dp else 10.dp,
            shape = shape,
            ambientColor = glass.accentBlue.copy(alpha = 0.14f),
            spotColor = glass.accentViolet.copy(alpha = 0.14f)
        )
        .clip(shape)
        .background(
            brush = Brush.linearGradient(
                colors = listOf(glass.cardFillTop, glass.cardFillBottom)
            )
        )
        .border(BorderStroke(1.dp, glass.cardBorder), shape)
        .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }

    Box(modifier = baseModifier) {
        content()
    }
}

@Composable
fun VortexLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val glass = LocalVortexGlass.current
    Canvas(modifier = modifier.size(size)) {
        val stroke = size.toPx() * 0.11f
        val pad = stroke
        drawArc(
            brush = Brush.linearGradient(listOf(glass.accentBlue, glass.accentViolet, glass.accentCyan)),
            startAngle = -85f,
            sweepAngle = 285f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
            topLeft = Offset(pad, pad),
            size = androidx.compose.ui.geometry.Size(this.size.width - pad * 2, this.size.height - pad * 2)
        )
        val cx = this.size.width / 2f
        val topY = this.size.height * 0.28f
        val botY = this.size.height * 0.68f
        drawLine(
            brush = Brush.verticalGradient(listOf(glass.accentCyan, glass.accentBlue)),
            start = Offset(cx, topY),
            end = Offset(cx, botY),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = glass.accentBlue,
            start = Offset(cx - this.size.width * 0.16f, this.size.height * 0.54f),
            end = Offset(cx, botY),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = glass.accentViolet,
            start = Offset(cx + this.size.width * 0.16f, this.size.height * 0.54f),
            end = Offset(cx, botY),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

data class VortexNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun FloatingGlassNavBar(
    items: List<VortexNavItem>,
    currentRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalVortexGlass.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = glass.navBarFill,
            border = BorderStroke(1.dp, glass.navBarBorder),
            shadowElevation = 18.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    val indicatorWidth by animateDpAsState(
                        targetValue = if (selected) 24.dp else 0.dp,
                        animationSpec = tween(220),
                        label = "nav_indicator"
                    )
                    val tint by animateColorAsState(
                        targetValue = if (selected) glass.accentCyan else glass.textSecondary,
                        animationSpec = tween(220),
                        label = "nav_tint"
                    )
                    val interaction = remember { MutableInteractionSource() }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (selected) glass.accentBlue.copy(alpha = 0.14f) else Color.Transparent
                            )
                            .clickable(
                                interactionSource = interaction,
                                indication = null
                            ) { onSelect(item.route) }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = tint,
                                modifier = Modifier.size(22.dp)
                            )
                            if (item.badgeCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(glass.accentViolet)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = item.label,
                            color = tint,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .width(indicatorWidth)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(listOf(glass.accentBlue, glass.accentCyan))
                                )
                        )
                    }
                }
            }
        }
    }
}
