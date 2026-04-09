package com.crosshyper.gidertakip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.crosshyper.gidertakip.di.AppContainer
import com.crosshyper.gidertakip.presentation.home.FinanceHome
import com.crosshyper.gidertakip.presentation.home.HomeViewModel
import com.crosshyper.gidertakip.ui.theme.GiderTakipTheme

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { AppContainer(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModel: HomeViewModel by viewModels {
            appContainer.provideHomeViewModelFactory(application)
        }

        setContent {
            GiderTakipTheme(darkTheme = false, dynamicColor = false) {
                FinanceHome(viewModel = viewModel)
            }
        }
    }
}
