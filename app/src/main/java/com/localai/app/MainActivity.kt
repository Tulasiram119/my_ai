package com.localai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.localai.app.data.engine.MediaPipeLlmInferenceEngine
import com.localai.app.repository.ModelManager
import com.localai.app.ui.chat.ChatScreen
import com.localai.app.ui.chat.ChatViewModel
import com.localai.app.ui.theme.LocalAiTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val engine = MediaPipeLlmInferenceEngine(applicationContext)
        val modelManager = ModelManager(applicationContext)

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(engine, modelManager) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[ChatViewModel::class.java]

        setContent {
            LocalAiTheme {
                ChatScreen(viewModel = viewModel)
            }
        }
    }
}
