package com.luis.itera

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.itera.presentation.navigation.IteraNavHost
import com.luis.itera.presentation.theme.IteraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Ruta de navegación pedida por un deep link del widget (p. ej. el anillo de
    // hidratación). Es estado observable para reaccionar también a onNewIntent
    // cuando la app ya estaba abierta.
    private val deepLinkRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLinkRoute.value = intent?.getStringExtra(EXTRA_DESTINATION)
        setContent {
            IteraTheme {
                val viewModel: MainViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                val onboardingState by viewModel.onboardingCompleted.collectAsStateWithLifecycle()

                onboardingState?.let { completed ->
                    if (completed) {
                        LaunchedEffect(Unit) {
                            viewModel.maybeAutoPinWidget()
                        }
                    }
                    IteraNavHost(
                        onboardingCompleted = completed,
                        onOnboardingDone = {},
                        deepLinkRoute = deepLinkRoute.value,
                        onDeepLinkHandled = { deepLinkRoute.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkRoute.value = intent.getStringExtra(EXTRA_DESTINATION)
    }

    companion object {
        const val EXTRA_DESTINATION = "com.luis.itera.EXTRA_DESTINATION"
        const val DEST_HYDRATION = "hydration"
    }
}
