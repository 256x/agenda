package fumi.day.literalagenda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import fumi.day.literalagenda.data.GitHubRepository
import fumi.day.literalagenda.data.SettingsRepository
import fumi.day.literalagenda.ui.AgendaNavigation
import fumi.day.literalagenda.ui.theme.LiteralAgendaTheme
import javax.inject.Inject
import androidx.core.view.WindowCompat

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var gitHubRepository: GitHubRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val bgColor by settingsRepository.bgColor.collectAsState(initial = "")
            val textColor by settingsRepository.textColor.collectAsState(initial = "")
            val accentColor by settingsRepository.accentColor.collectAsState(initial = "")
            val fontChoice by settingsRepository.fontChoice.collectAsState(initial = "system")
            val fontSize by settingsRepository.fontSize.collectAsState(initial = 16f)

            LiteralAgendaTheme(
                bgColor = bgColor,
                textColor = textColor,
                accentColor = accentColor,
                fontChoice = fontChoice,
                fontSize = fontSize
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AgendaNavigation()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        gitHubRepository.launchSync()
    }
}
