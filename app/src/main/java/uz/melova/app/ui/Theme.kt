package uz.melova.app.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Night=Color(0xFF101121)
val CardNight=Color(0xFF1C1D33)
val LineNight=Color(0xFF32334B)
val Violet=Color(0xFFAC91FF)
val Aqua=Color(0xFF5CE1CB)
val Muted=Color(0xFFADAEC8)
val Rose=Color(0xFFFF86B1)

private val DarkColors=darkColorScheme(
    primary=Violet,onPrimary=Night,primaryContainer=Color(0xFF373055),
    secondary=Aqua,onSecondary=Night,
    background=Night,onBackground=Color(0xFFF7F4FF),
    surface=Night,onSurface=Color(0xFFF7F4FF),
    surfaceVariant=CardNight,onSurfaceVariant=Muted,
    outline=LineNight,error=Color(0xFFFF928F)
)
@Composable fun MelovaTheme(content:@Composable ()->Unit) {
    MaterialTheme(colorScheme=DarkColors,typography=Typography(),content=content)
}
