package com.dabbled.wordpressnewsletter

import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.*
import java.net.URL

class ArticleActivity : AppCompatActivity() {

    private lateinit var titleView: TextView
    private lateinit var featuredImageView: ImageView
    private lateinit var contentWebView: WebView

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
        contentWebView = findViewById(R.id.article_content_webview)

        // Set title
        titleView.text = articleTitle

        // Load featured image if available
        if (articleFeaturedImage.isNotEmpty() && articleFeaturedImage != "false" && articleFeaturedImage != "null") {
            Log.d("ArticleActivity", "Loading featured image: $articleFeaturedImage")
            loadImage(articleFeaturedImage)
        } else {
            Log.d("ArticleActivity", "No featured image available")
            featuredImageView.visibility = android.view.View.GONE
        }

        // Setup WebView for content
        setupContentWebView(articleContent)
    }

    private fun setupContentWebView(htmlContent: String) {
        // Configure WebView settings
        contentWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }

        // Set WebViewClient to open links in external browser
        contentWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                // Open links in external browser
                url?.let {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(it))
                    startActivity(intent)
                }
                return true
            }
        }

        // Wrap content in HTML with proper styling
        val styledHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: sans-serif;
                        font-size: 16px;
                        line-height: 1.6;
                        color: #333;
                        padding: 0;
                        margin: 0;
                    }
                    img {
                        max-width: 100%;
                        height: auto;
                        display: block;
                        margin: 10px 0;
                    }
                    video {
                        max-width: 100%;
                        height: auto;
                        display: block;
                        margin: 10px 0;
                    }
                    iframe {
                        max-width: 100%;
                        margin: 10px 0;
                    }
                    p {
                        margin: 10px 0;
                    }
                    a {
                        color: #1976D2;
                        text-decoration: none;
                    }
                    a:hover {
                        text-decoration: underline;
                    }
                    h1, h2, h3, h4, h5, h6 {
                        margin: 15px 0 10px 0;
                        color: #222;
                    }
                    ul, ol {
                        margin: 10px 0;
                        padding-left: 30px;
                    }
                    blockquote {
                        border-left: 4px solid #ddd;
                        padding-left: 15px;
                        margin: 10px 0;
                        color: #666;
                    }
                    .wp-block-embed,
                    .wp-block-video {
                        margin: 10px 0;
                    }
                </style>
            </head>
            <body>
                $htmlContent
            </body>
            </html>
        """.trimIndent()

        // Load the HTML content
        contentWebView.loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null)

        Log.d("ArticleActivity", "Content loaded into WebView")
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