package com.luis.itera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.itera.presentation.navigation.IteraNavHost
import com.luis.itera.presentation.theme.IteraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IteraTheme {
                val viewModel: MainViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                val onboardingState by viewModel.onboardingCompleted.collectAsStateWithLifecycle()

                onboardingState?.let { completed ->
                    IteraNavHost(
                        onboardingCompleted = completed,
                        onOnboardingDone = {}
                    )
                }
            }
        }
    }
}