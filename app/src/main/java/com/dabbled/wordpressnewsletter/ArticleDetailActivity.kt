/*
* Copyright (c) 2026 Dabbled Studios
*/
package com.dabbled.wordpressnewsletter

import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class ArticleDetailActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article_detail)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get article data from intent
        val articleContent = intent.getStringExtra("article_content") ?: ""
        val articleTitle = intent.getStringExtra("article_title") ?: "Article"
        val featuredImageUrl = intent.getStringExtra("featured_image_url") ?: ""

        // Set title
        supportActionBar?.title = articleTitle

        // Setup WebView
        webView = findViewById(R.id.webview)
        setupWebView()

        // Load the article content as HTML
        if (articleContent.isNotEmpty()) {
            // Build featured image HTML if available
            val featuredImageHtml = if (featuredImageUrl.isNotEmpty()) {
                """<img src="$featuredImageUrl" alt="$articleTitle" style="width: 100%; height: auto; margin-bottom: 24px;">"""
            } else {
                ""
            }

            // Wrap content in a basic HTML template for better rendering
            val htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body {
                            font-family: sans-serif;
                            padding: 16px;
                            line-height: 1.6;
                            color: #333;
                            margin: 0;
                        }
                        h1 {
                            margin-top: 16px;
                            margin-bottom: 16px;
                            font-weight: bold;
                            font-size: 28px;
                        }
                        h2, h3, h4, h5, h6 {
                            margin-top: 24px;
                            margin-bottom: 16px;
                            font-weight: bold;
                        }
                        p {
                            margin-bottom: 16px;
                        }
                        img {
                            max-width: 100%;
                            height: auto;
                            display: block;
                            margin: 16px 0;
                        }
                        a {
                            color: #1976D2;
                            text-decoration: none;
                        }
                        ul, ol {
                            margin-bottom: 16px;
                            padding-left: 24px;
                        }
                        li {
                            margin-bottom: 8px;
                        }
                        iframe {
                            max-width: 100%;
                            margin: 16px 0;
                        }
                        .wp-block-embed {
                            margin: 16px 0;
                        }
                        .featured-image {
                            width: 100%;
                            height: auto;
                            margin: 0 0 24px 0;
                            display: block;
                        }
                    </style>
                </head>
                <body>
                    $featuredImageHtml
                    <h1>$articleTitle</h1>
                    $articleContent
                </body>
                </html>
            """.trimIndent()

            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        }
    }

    private fun setupWebView() {
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
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