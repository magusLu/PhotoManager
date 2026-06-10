package com.photomaster.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.photomaster.app.ui.navigation.PhotoMasterNavGraph
import com.photomaster.app.ui.theme.PhotoMasterTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhotoMasterTheme {
                PhotoMasterNavGraph()
            }
        }
    }
}
