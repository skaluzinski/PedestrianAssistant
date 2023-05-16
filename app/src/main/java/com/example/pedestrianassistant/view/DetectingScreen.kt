package com.example.pedestrianassistant.view

import android.content.Context
import androidx.compose.runtime.Composable
import com.example.pedestrianassistant.R
import java.io.File

@Composable
fun DetectingScreen() {

    fun getOutputDirectory(context: Context): File {
        val appContext = context.applicationContext
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else appContext.filesDir
    }


}