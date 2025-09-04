package com.dabbled.wordpressnewsletter

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
class PostAdapter(private val posts: List<WordPressPost>) :
    RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.post_title)
        val excerptText: TextView = view.findViewById(R.id.post_excerpt)
        val dateText: TextView = view.findViewById(R.id.post_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.titleText.text = post.title
        holder.excerptText.text = post.excerpt
        holder.dateText.text = post.date
    }

    override fun getItemCount() = posts.size
}

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PostAdapter
    private val posts = mutableListOf<WordPressPost>()

    // Replace with your WordPress site URL
    private val WORDPRESS_URL = "https://www.yesyoucandance.org/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupRecyclerView()
        fetchPosts()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PostAdapter(posts)
        recyclerView.adapter = adapter
    }

    private fun fetchPosts() {
        // Use coroutines for network operations
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val postsData = getPostsFromWordPress()

                withContext(Dispatchers.Main) {
                    posts.clear()
                    posts.addAll(postsData)
                    adapter.notifyDataSetChanged()
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching posts", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error loading posts: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private suspend fun getPostsFromWordPress(): List<WordPressPost> {
        return withContext(Dispatchers.IO) {
            val url = URL("$WORDPRESS_URL/wp-json/wp/v2/posts?per_page=20")
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    parsePostsJson(response)
                } else {
                    throw Exception("HTTP Error: $responseCode")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun parsePostsJson(jsonString: String): List<WordPressPost> {
        val posts = mutableListOf<WordPressPost>()
        val jsonArray = JSONArray(jsonString)

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

            posts.add(WordPressPost(id, cleanTitle, content, cleanExcerpt, date, link))
        }

        return posts
    }
}