/*
* Copyright (c) 2026 Dabbled Studios
* Created by Dameion Dorsner
*/


import SwiftUI
import WebKit
import Shared

struct ArticleDetailView: View {
    let post: WordPressPost

    var body: some View {
        WebView(htmlContent: post.content)
            .navigationTitle(post.title)
            .navigationBarTitleDisplayMode(.inline)
    }
}

struct WebView: UIViewRepresentable {
    let htmlContent: String

    func makeUIView(context: Context) -> WKWebView {
        return WKWebView()
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {
        uiView.loadHTMLString(styledHTML, baseURL: nil)
    }

    private var styledHTML: String {
        """
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    font-family: -apple-system, sans-serif;
                    font-size: 16px;
                    line-height: 1.6;
                    padding: 16px;
                    color: #333;
                }
                img {
                    max-width: 100%;
                    height: auto;
                }
                a {
                    color: #007AFF;
                }
            </style>
        </head>
        <body>
            \(htmlContent)
        </body>
        </html>
        """
    }
}
