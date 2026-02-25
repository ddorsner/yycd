package com.dabbled.wordpressnewsletter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
    val url: String,
    val excerpt: String,
    val content: String,
    val date: String,
    val featuredImage: Boolean = false,
    val sticky: Boolean = false
)

// Data class for Location Details
data class LocationDetail(
    val id: Int,
    val title: String,
    val phone: String,
    val email: String,
    val latitude: Double,
    val longitude: Double
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

class ArticleActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var locationTitleText: TextView
    private lateinit var contactButton: Button
    private lateinit var adapter: PostAdapter
    private val posts = mutableListOf<WordPressPost>()

    private var locationId: Int = 0
    private var locationDetail: LocationDetail? = null

    private val WORDPRESS_URL = "https://dandysite.com/yycd/wp-json/ds/v1"

    private var currentPage = 1
    private val postsPerPage = 10
    private var isLoading = false
    private var hasMorePages = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "" // Clear default title

        // Get location ID from intent
        locationId = intent.getIntExtra("location_id", 0)

        if (locationId == 0) {
            Toast.makeText(this, "Error: No location selected", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupViews()
        fetchLocationDetails()
        fetchPosts(page = 1)
    }

    private fun setupViews() {
        locationTitleText = findViewById(R.id.location_title)
        contactButton = findViewById(R.id.contact_button)
        recyclerView = findViewById(R.id.recycler_view)

        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        adapter = PostAdapter(posts) { post ->
            openArticleDetail(post)
        }
        recyclerView.adapter = adapter

        contactButton.setOnClickListener {
            showContactDialog()
        }

        // Add scroll listener for pagination
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && hasMorePages && dy > 0) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2) {
                        fetchPosts(page = currentPage + 1)
                    }
                }
            }
        })
    }

    private fun openArticleDetail(post: WordPressPost) {
        Log.d("ArticleActivity", "Opening article detail: ${post.url}")

        val intent = Intent(this, ArticleDetailActivity::class.java)
        intent.putExtra("article_url", post.url)
        intent.putExtra("article_title", post.title)
        startActivity(intent)
    }

    private fun fetchLocationDetails() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val location = getLocationDetails(locationId)

                withContext(Dispatchers.Main) {
                    locationDetail = location
                    locationTitleText.text = location.title
                    Log.d("ArticleActivity", "Location details loaded: ${location.title}")
                }
            } catch (e: Exception) {
                Log.e("ArticleActivity", "Error fetching location details", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ArticleActivity,
                        "Error loading location details: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun fetchPosts(page: Int = 1) {
        if (isLoading) return

        Log.d("ArticleActivity", "Fetching posts for location $locationId, page $page")
        isLoading = true

        if (page == 1) {
            Toast.makeText(this, "Loading newsletters...", Toast.LENGTH_SHORT).show()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val postsData = getPostsForLocation(locationId, page)

                withContext(Dispatchers.Main) {
                    if (page == 1) {
                        posts.clear()
                        posts.addAll(postsData)

                        adapter = PostAdapter(posts) { post ->
                            openArticleDetail(post)
                        }
                        recyclerView.adapter = adapter
                    } else {
                        val oldSize = posts.size
                        posts.addAll(postsData)
                        adapter.notifyItemRangeInserted(oldSize, postsData.size)

                        if (postsData.isNotEmpty()) {
                            Toast.makeText(
                                this@ArticleActivity,
                                "Loaded ${postsData.size} more newsletters",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    currentPage = page
                    hasMorePages = postsData.size == postsPerPage
                    isLoading = false

                    if (posts.isEmpty()) {
                        Toast.makeText(
                            this@ArticleActivity,
                            "No newsletters found for this location",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ArticleActivity", "Error fetching posts", e)
                withContext(Dispatchers.Main) {
                    isLoading = false
                    Toast.makeText(
                        this@ArticleActivity,
                        "Error loading newsletters: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private suspend fun getLocationDetails(locationId: Int): LocationDetail {
        return withContext(Dispatchers.IO) {
            val apiUrl = "$WORDPRESS_URL/locations"
            Log.d("ArticleActivity", "Fetching location details from: $apiUrl")

            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    // The API returns an object with a "locations" array
                    val jsonObject = JSONObject(response)
                    val jsonArray = jsonObject.getJSONArray("locations")

                    for (i in 0 until jsonArray.length()) {
                        val locationObject = jsonArray.getJSONObject(i)
                        if (locationObject.getInt("id") == locationId) {
                            // Use "name" field for the location name
                            val name = locationObject.getString("name")
                            val phone = locationObject.optString("phone", "")
                            val email = locationObject.optString("email", "")
                            val latitude = locationObject.optString("latitude", "0.0").toDoubleOrNull() ?: 0.0
                            val longitude = locationObject.optString("longitude", "0.0").toDoubleOrNull() ?: 0.0

                            return@withContext LocationDetail(locationId, name, phone, email, latitude, longitude)
                        }
                    }
                    throw Exception("Location not found")
                } else {
                    throw Exception("HTTP Error: $responseCode")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    private suspend fun getPostsForLocation(locationId: Int, page: Int): List<WordPressPost> {
        return withContext(Dispatchers.IO) {
            val apiUrl = "$WORDPRESS_URL/locations/$locationId/posts?per_page=$postsPerPage&page=$page"
            Log.d("ArticleActivity", "Fetching posts from: $apiUrl")

            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    return@withContext parsePostsJson(response)
                } else {
                    Log.e("ArticleActivity", "HTTP Error: $responseCode")
                    throw Exception("HTTP Error: $responseCode")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun parsePostsJson(jsonString: String): List<WordPressPost> {
        val posts = mutableListOf<WordPressPost>()

        try {
            Log.d("ArticleActivity", "parsePostsJson: Raw response: $jsonString")

            // The response is an object with "location" and "posts" fields
            val jsonObject = JSONObject(jsonString)

            // Get the "posts" object, then get the "items" array from it
            val postsObject = jsonObject.getJSONObject("posts")
            val itemsArray = postsObject.getJSONArray("items")
            Log.d("ArticleActivity", "parsePostsJson: Found ${itemsArray.length()} posts")

            for (i in 0 until itemsArray.length()) {
                val postObject = itemsArray.getJSONObject(i)

                val id = postObject.getInt("id")
                val title = postObject.getString("title")
                val url = postObject.getString("url")
                val excerpt = postObject.getString("excerpt")
                val content = postObject.getString("content")
                val date = postObject.getString("date")
                val featuredImage = postObject.optBoolean("featured_image", false)
                val sticky = postObject.optBoolean("sticky", false)

                posts.add(WordPressPost(id, title, url, excerpt, content, date, featuredImage, sticky))
                Log.d("ArticleActivity", "parsePostsJson: Parsed post $i - $title")
            }
        } catch (e: Exception) {
            Log.e("ArticleActivity", "Error parsing posts JSON", e)
            Log.e("ArticleActivity", "JSON was: $jsonString")
            // Don't throw - just return empty list so app doesn't crash
        }

        return posts
    }

    private fun showContactDialog() {
        val location = locationDetail
        if (location == null) {
            Toast.makeText(this, "Location details not loaded yet", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_contact, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Contact ${location.title}")
            .setView(dialogView)
            .setNegativeButton("Close", null)
            .create()

        // Setup contact buttons
        val callButton: Button = dialogView.findViewById(R.id.btn_call)
        val textButton: Button = dialogView.findViewById(R.id.btn_text)
        val emailButton: Button = dialogView.findViewById(R.id.btn_email)
        val directionsButton: Button = dialogView.findViewById(R.id.btn_directions)

        // Call button
        if (location.phone.isNotEmpty()) {
            callButton.text = "Call ${location.phone}"
            callButton.setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${location.phone}"))
                startActivity(intent)
                dialog.dismiss()
            }
        } else {
            callButton.visibility = View.GONE
        }

        // Text button
        if (location.phone.isNotEmpty()) {
            textButton.text = "Text ${location.phone}"
            textButton.setOnClickListener {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${location.phone}"))
                startActivity(intent)
                dialog.dismiss()
            }
        } else {
            textButton.visibility = View.GONE
        }

        // Email button
        if (location.email.isNotEmpty()) {
            emailButton.text = "Email ${location.email}"
            emailButton.setOnClickListener {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${location.email}"))
                startActivity(intent)
                dialog.dismiss()
            }
        } else {
            emailButton.visibility = View.GONE
        }

        // Directions button
        if (location.latitude != 0.0 && location.longitude != 0.0) {
            directionsButton.setOnClickListener {
                val gmmIntentUri = Uri.parse("geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}(${location.title})")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")

                if (mapIntent.resolveActivity(packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    // Fallback to browser if Google Maps not installed
                    val browserIntent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"))
                    startActivity(browserIntent)
                }
                dialog.dismiss()
            }
        } else {
            directionsButton.visibility = View.GONE
        }

        dialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish() // Go back to MainActivity
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
