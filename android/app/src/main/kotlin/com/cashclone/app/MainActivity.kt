package com.cashclone.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cashclone.app.di.AppContainer
import com.cashclone.app.ui.nav.AppNav
import com.cashclone.app.ui.theme.CashCloneTheme

class MainActivity : ComponentActivity() {
    private lateinit var appContainer: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContainer = AppContainer(applicationContext)

        setContent {
            CashCloneTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav(repository = appContainer.repository)
                }
            }
        }
    }
}
