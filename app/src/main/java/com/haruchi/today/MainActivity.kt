package com.haruchi.today

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.haruchi.today.ui.FocusApp
import com.haruchi.today.ui.theme.HaruchiTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host. All screens are Compose destinations under [FocusApp].
 * The timer screen requests landscape from within Compose; everything else is portrait.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AppRoot()
        }
    }
}

@Composable
private fun AppRoot() {
    HaruchiTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            FocusApp()
        }
    }
}
