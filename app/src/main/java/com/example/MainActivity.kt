package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import com.example.data.AppDatabase
import com.example.data.MarginRepository
import com.example.ui.MainScreen
import com.example.ui.MarginViewModel
import com.example.ui.MarginViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import java.io.File

class MainActivity : ComponentActivity() {

    // Initialize Room Database, Repository, and ViewModel using standard Android practices
    private val database by lazy { AppDatabase.getDatabase(applicationContext) }
    private val repository by lazy { MarginRepository(database.marginDao()) }
    
    private val viewModel: MarginViewModel by viewModels {
        MarginViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable Edge-to-Edge full bleed layout support
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = com.example.ui.theme.CharcoalBg
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        onGetTempUri = { getTempFileUri() }
                    )
                }
            }
        }
    }

    /**
     * Generates a secure Content URI for temporary camera image capture files using FileProvider.
     * This avoids storage permissions entirely.
     */
    private fun getTempFileUri(): Uri {
        val tempFile = File.createTempFile("margin_capture_", ".jpg", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        return FileProvider.getUriForFile(
            applicationContext,
            "${packageName}.fileprovider",
            tempFile
        )
    }
}
