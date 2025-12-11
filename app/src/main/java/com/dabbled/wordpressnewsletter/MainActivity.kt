package com.dabbled.wordpressnewsletter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

// Data class for WordPress posts
data class WordPressPost(
    val id: Int,
    val title: String,
    val content: String,
    val excerpt: String,
    val date: String,
    val link: String
)

// RecyclerView Adapter
class PostAdapter(
    private val posts: List<WordPressPost>,
    private val onPostClick: (WordPressPost) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.post_title)
        val excerptText: TextView = view.findViewById(R.id.post_excerpt)
        val dateText: TextView = view.findViewById(R.id.post_date)
        val cardView: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        Log.d("PostAdapter", "onCreateViewHolder: Creating new ViewHolder")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        Log.d("PostAdapter", "onBindViewHolder: Binding position $position, title: ${post.title}")

        holder.titleText.text = post.title
        holder.excerptText.text = post.excerpt
        holder.dateText.text = post.date

        // Set click listener for the entire card
        holder.cardView.setOnClickListener {
            Log.d("PostAdapter", "onClick: Card clicked at position: $position, title: ${post.title}")
            onPostClick(post)
        }

        Log.d("PostAdapter", "onBindViewHolder: Click listener set for position $position")
    }

    override fun getItemCount(): Int {
        Log.d("PostAdapter", "getItemCount: Returning ${posts.size}")
        return posts.size
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PostAdapter
    private val posts = mutableListOf<WordPressPost>()

    // WordPress site URL
    private val WORDPRESS_URL = "https://www.yesyoucandance.org"

    // Pagination variables
    private var currentPage = 1
    private val postsPerPage = 10
    private var isLoading = false
    private var hasMorePages = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("MainActivity", "onCreate: App started")
        setupRecyclerView()
        fetchPosts(page = 1)
    }

    private fun setupRecyclerView() {
        Log.d("MainActivity", "setupRecyclerView: Setting up RecyclerView")
        recyclerView = findViewById(R.id.recycler_view)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        // Create adapter with click listener
        adapter = PostAdapter(posts) { post ->
            Log.d("MainActivity", "Post clicked from adapter callback: ${post.title}")
            openArticle(post)
        }
        recyclerView.adapter = adapter
        Log.d("MainActivity", "setupRecyclerView: RecyclerView setup complete, posts count: ${posts.size}")

        // Add scroll listener for pagination
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                // Check if we should load more posts
                if (!isLoading && hasMorePages && dy > 0) { // Only load when scrolling down
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2) {
                        Log.d("MainActivity", "Triggering pagination load for page: ${currentPage + 1}")
                        fetchPosts(page = currentPage + 1)
                    }
                }
            }
        })
    }

    private fun openArticle(post: WordPressPost) {
        Log.d("MainActivity", "Opening article: ${post.link}")
        Toast.makeText(this, "Opening: ${post.title}", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, ArticleActivity::class.java)
        intent.putExtra("article_url", post.link)
        intent.putExtra("article_title", post.title)
        startActivity(intent)
    }

    private fun fetchPosts(page: Int = 1) {
        if (isLoading) return

        Log.d("MainActivity", "fetchPosts: Starting to fetch page $page")
        isLoading = true

        // Show a toast for first page loading
        if (page == 1) {
            Toast.makeText(this, "Loading newsletters...", Toast.LENGTH_SHORT).show()
        }

        // Use coroutines for network operations
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("MainActivity", "fetchPosts: Making API call for page $page")
                val postsData = getPostsFromWordPress(page)
                Log.d("MainActivity", "fetchPosts: API call successful, received ${postsData.size} posts")

                withContext(Dispatchers.Main) {
                    if (page == 1) {
                        // First page - clear existing posts and recreate adapter
                        Log.d("MainActivity", "fetchPosts: Clearing posts and recreating adapter")
                        posts.clear()
                        posts.addAll(postsData)

                        // Recreate adapter to ensure click listeners work
                        adapter = PostAdapter(posts) { post ->
                            Log.d("MainActivity", "Post clicked from recreated adapter: ${post.title}")
                            openArticle(post)
                        }
                        recyclerView.adapter = adapter
                        Log.d("MainActivity", "fetchPosts: Adapter recreated with ${posts.size} posts")
                    } else {
                        // Subsequent pages - append new posts
                        val oldSize = posts.size
                        posts.addAll(postsData)
                        adapter.notifyItemRangeInserted(oldSize, postsData.size)
                        Log.d("MainActivity", "fetchPosts: Added ${postsData.size} posts, total now: ${posts.size}")

                        // Show toast for pagination
                        if (postsData.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "Loaded ${postsData.size} more newsletters",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    // Update pagination state
                    currentPage = page
                    hasMorePages = postsData.size == postsPerPage
                    isLoading = false

                    Log.d("MainActivity", "fetchPosts: Updated state - currentPage: $currentPage, hasMorePages: $hasMorePages")

                    // Show message if no posts loaded
                    if (posts.isEmpty()) {
                        Log.w("MainActivity", "fetchPosts: No posts found")
                        Toast.makeText(
                            this@MainActivity,
                            "No newsletters found",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (!hasMorePages && page > 1) {
                        Log.d("MainActivity", "fetchPosts: Reached end of posts")
                        Toast.makeText(
                            this@MainActivity,
                            "No more newsletters to load",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "fetchPosts: Error fetching posts", e)
                withContext(Dispatchers.Main) {
                    isLoading = false
                    Toast.makeText(
                        this@MainActivity,
                        "Error loading newsletters: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private suspend fun getPostsFromWordPress(page: Int): List<WordPressPost> {
        return withContext(Dispatchers.IO) {
            val apiUrl = "$WORDPRESS_URL/wp-json/wp/v2/posts?per_page=$postsPerPage&page=$page"
            Log.d("MainActivity", "getPostsFromWordPress: Making request to: $apiUrl")

            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")

                val responseCode = connection.responseCode
                Log.d("MainActivity", "getPostsFromWordPress: Response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    Log.d("MainActivity", "getPostsFromWordPress: Response received, length: ${response.length}")
                    Log.v("MainActivity", "getPostsFromWordPress: Response content: $response")

                    val parsedPosts = parsePostsJson(response)
                    Log.d("MainActivity", "getPostsFromWordPress: Parsed ${parsedPosts.size} posts")
                    parsedPosts
                } else {
                    Log.e("MainActivity", "getPostsFromWordPress: HTTP Error: $responseCode")
                    throw Exception("HTTP Error: $responseCode")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun parsePostsJson(jsonString: String): List<WordPressPost> {
        Log.d("MainActivity", "parsePostsJson: Starting to parse JSON")
        val posts = mutableListOf<WordPressPost>()

        try {
            val jsonArray = JSONArray(jsonString)
            Log.d("MainActivity", "parsePostsJson: JSON array has ${jsonArray.length()} items")

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)

                val id = jsonObject.getInt("id")
                val title = jsonObject.getJSONObject("title").getString("rendered")
                val content = jsonObject.getJSONObject("content").getString("rendered")
                val excerpt = jsonObject.getJSONObject("excerpt").getString("rendered")
                val date = jsonObject.getString("date")
                val link = jsonObject.getString("link")

                // Clean HTML tags from title and excerpt
                val cleanTitle = title.replace(Regex("<.*?>"), "")
                val cleanExcerpt = excerpt.replace(Regex("<.*?>"), "")

                val post = WordPressPost(id, cleanTitle, content, cleanExcerpt, date, link)
                posts.add(post)

                Log.d("MainActivity", "parsePostsJson: Parsed post $i - ID: $id, Title: $cleanTitle, Link: $link")
            }

            Log.d("MainActivity", "parsePostsJson: Successfully parsed ${posts.size} posts")
        } catch (e: Exception) {
            Log.e("MainActivity", "parsePostsJson: Error parsing JSON", e)
            throw e
        }

        return posts
    }
}