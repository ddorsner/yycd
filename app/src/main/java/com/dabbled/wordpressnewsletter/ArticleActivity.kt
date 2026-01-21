package com.dabbled.wordpressnewsletter

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.*
import java.net.URL

class ArticleActivity : AppCompatActivity() {

    private lateinit var titleView: TextView
    private lateinit var featuredImageView: ImageView
    private lateinit var contentView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get article data from intent
        val articleTitle = intent.getStringExtra("article_title") ?: "Article"
        val articleContent = intent.getStringExtra("article_content") ?: ""
        val articleFeaturedImage = intent.getStringExtra("article_featured_image") ?: ""

        // Set title in action bar
        supportActionBar?.title = articleTitle

        // Setup views
        titleView = findViewById(R.id.article_title)
        featuredImageView = findViewById(R.id.article_featured_image)
        contentView = findViewById(R.id.article_content)

        // Set content
        titleView.text = articleTitle

        // Load featured image if available
        if (articleFeaturedImage.isNotEmpty() && articleFeaturedImage != "false" && articleFeaturedImage != "null") {
            Log.d("ArticleActivity", "Loading featured image: $articleFeaturedImage")
            loadImage(articleFeaturedImage)
        } else {
            Log.d("ArticleActivity", "No featured image available")
            featuredImageView.visibility = android.view.View.GONE
        }

        // Convert HTML content to styled text
        val htmlContent = HtmlCompat.fromHtml(articleContent, HtmlCompat.FROM_HTML_MODE_COMPACT)
        contentView.text = htmlContent
    }

    private fun loadImage(imageUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection()
                connection.doInput = true
                connection.connect()
                val input = connection.getInputStream()
                val bitmap = BitmapFactory.decodeStream(input)

                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        featuredImageView.setImageBitmap(bitmap)
                        featuredImageView.visibility = android.view.View.VISIBLE
                        Log.d("ArticleActivity", "Featured image loaded successfully")
                    } else {
                        featuredImageView.visibility = android.view.View.GONE
                        Log.e("ArticleActivity", "Failed to decode bitmap")
                    }
                }
            } catch (e: Exception) {
                Log.e("ArticleActivity", "Error loading image", e)
                withContext(Dispatchers.Main) {
                    featuredImageView.visibility = android.view.View.GONE
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}