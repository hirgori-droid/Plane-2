package com.example.russiantrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.example.russiantrainer.ui.TrainerApp
import com.example.russiantrainer.ui.TrainerViewModel
import com.example.russiantrainer.ui.theme.RussianTrainerTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<TrainerViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appTheme by viewModel.theme.collectAsState()
            RussianTrainerTheme(appTheme = appTheme) {
                TrainerApp(viewModel = viewModel)
            }
        }
    }
}
