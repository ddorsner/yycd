package com.dabbled.wordpressnewsletter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import kotlinx.coroutines.*
import java.net.URL

object ImageLoader {
    private val imageCache = mutableMapOf<String, Bitmap>()

    fun loadImage(imageUrl: String, imageView: ImageView) {
        // Check cache first
        imageCache[imageUrl]?.let {
            imageView.setImageBitmap(it)
            return
        }

        // Load from network
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection()
                connection.doInput = true
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()

                val input = connection.getInputStream()
                val bitmap = BitmapFactory.decodeStream(input)
                input.close()

                if (bitmap != null) {
                    // Cache the bitmap
                    imageCache[imageUrl] = bitmap

                    withContext(Dispatchers.Main) {
                        imageView.setImageBitmap(bitmap)
                        imageView.visibility = android.view.View.VISIBLE
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        imageView.visibility = android.view.View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.e("ImageLoader", "Error loading image: $imageUrl", e)
                withContext(Dispatchers.Main) {
                    imageView.visibility = android.view.View.GONE
                }
            }
        }
    }

    fun clearCache() {
        imageCache.clear()
    }
}