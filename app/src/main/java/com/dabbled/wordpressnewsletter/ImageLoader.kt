package com.dabbled.wordpressnewsletter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

object ImageLoader {
    private val imageCache = mutableMapOf<String, Bitmap>()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun loadImage(imageUrl: String, imageView: ImageView) {
        imageCache[imageUrl]?.let {
            imageView.setImageBitmap(it)
            imageView.visibility = android.view.View.VISIBLE
            return
        }

        // Tag the view with the URL it's currently loading, so if the view gets
        // recycled to a different URL before this finishes, we skip the stale set.
        imageView.tag = imageUrl

        scope.launch {
            try {
                val bitmap = URL(imageUrl).openConnection().run {
                    doInput = true
                    connectTimeout = 5000
                    readTimeout = 5000
                    connect()
                    getInputStream().use { BitmapFactory.decodeStream(it) }
                }

                if (bitmap != null) {
                    imageCache[imageUrl] = bitmap
                    withContext(Dispatchers.Main) {
                        if (imageView.tag == imageUrl) {
                            imageView.setImageBitmap(bitmap)
                            imageView.visibility = android.view.View.VISIBLE
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        if (imageView.tag == imageUrl) {
                            imageView.visibility = android.view.View.GONE
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ImageLoader", "Error loading image: $imageUrl", e)
                withContext(Dispatchers.Main) {
                    if (imageView.tag == imageUrl) {
                        imageView.visibility = android.view.View.GONE
                    }
                }
            }
        }
    }

    fun clearCache() {
        imageCache.clear()
    }
}