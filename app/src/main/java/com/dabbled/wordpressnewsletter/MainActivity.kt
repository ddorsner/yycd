package com.dabbled.wordpressnewsletter

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
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
    val url: String,
    val excerpt: String,
    val content: String,
    val date: String,
    val featured_image: String,
    val sticky: Boolean
)

// Data class for Locations
data class Location(
    val id: Int,
    val title: String,
    val name: String,
    val city: String,
    val address: String,
    val phone: String,
    val email: String,
    val contact: String,
    val description: String,
    val yycd_description: String,
    val logo_id: Int?,
    val logo_url: String,
    val latitude: String,
    val longitude: String
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
    private lateinit var locationSpinner: Spinner
    private lateinit var welcomeView: View
    private lateinit var adapter: PostAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val posts = mutableListOf<WordPressPost>()
    private val locations = mutableListOf<Location>()
    private var selectedLocationId: Int? = null

    // WordPress site URL base (includes wp-json)
    private val BASE_URL = "https://www.dandysite.com/yycd"
    private val WORDPRESS_URL = "$BASE_URL/yycd/wp-json"

    // SharedPreferences constants
    private val PREFS_NAME = "YYCDPrefs"
    private val PREF_SELECTED_LOCATION_ID = "selected_location_id"

    // Pagination variables
    private var currentPage = 1
    private val postsPerPage = 10
    private var isLoading = false
    private var hasMorePages = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("MainActivity", "onCreate: App started")

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setupViews()
        setupRecyclerView()
        setupLocationSpinner()
        fetchLocations()
    }

    private fun setupViews() {
        welcomeView = findViewById(R.id.welcome_view)
        recyclerView = findViewById(R.id.recycler_view)
        locationSpinner = findViewById(R.id.location_spinner)

        // Show welcome view initially
        showWelcomeView()
    }

    private fun showWelcomeView() {
        welcomeView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun showPostsView() {
        welcomeView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    private fun setupLocationSpinner() {
        Log.d("MainActivity", "setupLocationSpinner: Setting up location spinner")

        locationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0 && locations.isNotEmpty()) {
                    // position 0 is "Select a location", actual locations start at position 1
                    val location = locations[position - 1]
                    selectedLocationId = location.id

                    // Save the selected location ID to SharedPreferences
                    sharedPreferences.edit().putInt(PREF_SELECTED_LOCATION_ID, location.id).apply()

                    Log.d("MainActivity", "Location selected: ${location.title} (ID: ${location.id})")
                    showPostsView()
                    fetchPosts(page = 1, locationId = location.id)
                } else {
                    selectedLocationId = null

                    // Clear the saved location
                    sharedPreferences.edit().remove(PREF_SELECTED_LOCATION_ID).apply()

                    posts.clear()
                    adapter.notifyDataSetChanged()
                    showWelcomeView()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d("MainActivity", "No location selected")
                showWelcomeView()
            }
        }
    }

    private fun fetchLocations() {
        Log.d("MainActivity", "fetchLocations: Starting to fetch locations")
        Toast.makeText(this, "Loading locations...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("MainActivity", "fetchLocations: Making API call")
                val locationsData = getLocationsFromWordPress()
                Log.d("MainActivity", "fetchLocations: API call successful, received ${locationsData.size} locations")

                withContext(Dispatchers.Main) {
                    if (locationsData.isEmpty()) {
                        Log.w("MainActivity", "fetchLocations: No locations returned from API")
                        Toast.makeText(
                            this@MainActivity,
                            "No locations available",
                            Toast.LENGTH_LONG
                        ).show()
                        return@withContext
                    }

                    locations.clear()
                    locations.addAll(locationsData)

                    // Create spinner items with "Select a location" as first item
                    val spinnerItems = mutableListOf("Select a location")
                    spinnerItems.addAll(locations.map { it.title })

                    Log.d("MainActivity", "fetchLocations: Spinner items: $spinnerItems")

                    val spinnerAdapter = ArrayAdapter(
                        this@MainActivity,
                        android.R.layout.simple_spinner_item,
                        spinnerItems
                    )
                    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    locationSpinner.adapter = spinnerAdapter

                    Log.d("MainActivity", "fetchLocations: Spinner populated with ${locations.size} locations")

                    // Restore previously selected location if it exists
                    restoreSelectedLocation()

                    Toast.makeText(
                        this@MainActivity,
                        "Loaded ${locations.size} locations",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "fetchLocations: Error fetching locations", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error loading locations: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private suspend fun getLocationsFromWordPress(): List<Location> {
        return withContext(Dispatchers.IO) {
            val apiUrl = "$WORDPRESS_URL/ds/v1/locations"
            Log.d("MainActivity", "getLocationsFromWordPress: Making request to: $apiUrl")

            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                Log.d("MainActivity", "getLocationsFromWordPress: Response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    Log.d("MainActivity", "getLocationsFromWordPress: Response received, length: ${response.length}")
                    Log.v("MainActivity", "getLocationsFromWordPress: Response content: $response")

                    val parsedLocations = parseLocationsJson(response)
                    Log.d("MainActivity", "getLocationsFromWordPress: Parsed ${parsedLocations.size} locations")
                    parsedLocations
                } else {
                    val errorStream = connection.errorStream
                    val errorMessage = if (errorStream != null) {
                        BufferedReader(InputStreamReader(errorStream)).readText()
                    } else {
                        "No error details"
                    }
                    Log.e("MainActivity", "getLocationsFromWordPress: HTTP Error $responseCode: $errorMessage")
                    throw Exception("HTTP Error: $responseCode - $errorMessage")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "getLocationsFromWordPress: Exception occurred", e)
                throw e
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun restoreSelectedLocation() {
        val savedLocationId = sharedPreferences.getInt(PREF_SELECTED_LOCATION_ID, -1)

        if (savedLocationId != -1) {
            Log.d("MainActivity", "restoreSelectedLocation: Restoring location ID: $savedLocationId")

            // Find the location in the list
            val locationIndex = locations.indexOfFirst { it.id == savedLocationId }

            if (locationIndex != -1) {
                // Set spinner selection (add 1 because position 0 is "Select a location")
                locationSpinner.setSelection(locationIndex + 1)
                Log.d("MainActivity", "restoreSelectedLocation: Restored location at position ${locationIndex + 1}")
            } else {
                Log.w("MainActivity", "restoreSelectedLocation: Saved location ID not found in current locations")
            }
        } else {
            Log.d("MainActivity", "restoreSelectedLocation: No saved location found")
        }
    }

    private fun parseLocationsJson(jsonString: String): List<Location> {
        Log.d("MainActivity", "parseLocationsJson: Starting to parse JSON")
        val locations = mutableListOf<Location>()

        try {
            val jsonObject = JSONObject(jsonString)
            val locationsArray = jsonObject.getJSONArray("locations")
            Log.d("MainActivity", "parseLocationsJson: JSON array has ${locationsArray.length()} items")

            for (i in 0 until locationsArray.length()) {
                val locationObj = locationsArray.getJSONObject(i)

                val id = locationObj.getInt("id")
                val title = locationObj.getString("title")
                val name = locationObj.optString("name", "")
                val city = locationObj.optString("city", "")
                val address = locationObj.optString("address", "")
                val phone = locationObj.optString("phone", "")
                val email = locationObj.optString("email", "")
                val contact = locationObj.optString("contact", "")
                val description = locationObj.optString("description", "")
                val yycd_description = locationObj.optString("yycd_description", "")
                val logo_id = if (locationObj.isNull("logo_id")) null else locationObj.getInt("logo_id")
                val logo_url = locationObj.optString("logo_url", "")
                val latitude = locationObj.optString("latitude", "")
                val longitude = locationObj.optString("longitude", "")

                val location = Location(
                    id, title, name, city, address, phone, email, contact,
                    description, yycd_description, logo_id, logo_url, latitude, longitude
                )
                locations.add(location)

                Log.d("MainActivity", "parseLocationsJson: Parsed location $i - ID: $id, Title: $title, Name: $name")
            }

            Log.d("MainActivity", "parseLocationsJson: Successfully parsed ${locations.size} locations")
        } catch (e: Exception) {
            Log.e("MainActivity", "parseLocationsJson: Error parsing JSON", e)
            throw e
        }

        return locations
    }

    private fun setupRecyclerView() {
        Log.d("MainActivity", "setupRecyclerView: Setting up RecyclerView")
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
                if (!isLoading && hasMorePages && dy > 0) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2) {
                        selectedLocationId?.let { locationId ->
                            Log.d("MainActivity", "Triggering pagination load for page: ${currentPage + 1}")
                            fetchPosts(page = currentPage + 1, locationId = locationId)
                        }
                    }
                }
            }
        })
    }

    private fun openArticle(post: WordPressPost) {
        Log.d("MainActivity", "Opening article: ${post.title}")
        Toast.makeText(this, "Opening: ${post.title}", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, ArticleActivity::class.java)
        intent.putExtra("article_title", post.title)
        intent.putExtra("article_content", post.content)
        intent.putExtra("article_featured_image", post.featured_image)
        startActivity(intent)
    }

    private fun fetchPosts(page: Int = 1, locationId: Int) {
        if (isLoading) return

        Log.d("MainActivity", "fetchPosts: Starting to fetch page $page for location $locationId")
        isLoading = true

        if (page == 1) {
            Toast.makeText(this, "Loading posts...", Toast.LENGTH_SHORT).show()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("MainActivity", "fetchPosts: Making API call for page $page, location $locationId")
                val postsData = getPostsFromWordPress(page, locationId)
                Log.d("MainActivity", "fetchPosts: API call successful, received ${postsData.size} posts")

                withContext(Dispatchers.Main) {
                    if (page == 1) {
                        Log.d("MainActivity", "fetchPosts: Clearing posts and recreating adapter")
                        posts.clear()
                        posts.addAll(postsData)

                        adapter = PostAdapter(posts) { post ->
                            Log.d("MainActivity", "Post clicked from recreated adapter: ${post.title}")
                            openArticle(post)
                        }
                        recyclerView.adapter = adapter
                        Log.d("MainActivity", "fetchPosts: Adapter recreated with ${posts.size} posts")
                    } else {
                        val oldSize = posts.size
                        posts.addAll(postsData)
                        adapter.notifyItemRangeInserted(oldSize, postsData.size)
                        Log.d("MainActivity", "fetchPosts: Added ${postsData.size} posts, total now: ${posts.size}")

                        if (postsData.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "Loaded ${postsData.size} more posts",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    currentPage = page
                    hasMorePages = postsData.size == postsPerPage
                    isLoading = false

                    Log.d("MainActivity", "fetchPosts: Updated state - currentPage: $currentPage, hasMorePages: $hasMorePages")

                    if (posts.isEmpty()) {
                        Log.w("MainActivity", "fetchPosts: No posts found")
                        Toast.makeText(
                            this@MainActivity,
                            "No posts found for this location",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (!hasMorePages && page > 1) {
                        Log.d("MainActivity", "fetchPosts: Reached end of posts")
                        Toast.makeText(
                            this@MainActivity,
                            "No more posts to load",
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
                        "Error loading posts: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private suspend fun getPostsFromWordPress(page: Int, locationId: Int): List<WordPressPost> {
        return withContext(Dispatchers.IO) {
            val apiUrl = "$WORDPRESS_URL/ds/v1/locations/$locationId/posts?per_page=$postsPerPage&page=$page"
            Log.d("MainActivity", "getPostsFromWordPress: Making request to: $apiUrl")

            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

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
                    val errorStream = connection.errorStream
                    val errorMessage = if (errorStream != null) {
                        BufferedReader(InputStreamReader(errorStream)).readText()
                    } else {
                        "No error details"
                    }
                    Log.e("MainActivity", "getPostsFromWordPress: HTTP Error $responseCode: $errorMessage")
                    throw Exception("HTTP Error: $responseCode - $errorMessage")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "getPostsFromWordPress: Exception occurred", e)
                throw e
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun parsePostsJson(jsonString: String): List<WordPressPost> {
        Log.d("MainActivity", "parsePostsJson: Starting to parse JSON")
        val posts = mutableListOf<WordPressPost>()

        try {
            val jsonObject = JSONObject(jsonString)
            val postsObject = jsonObject.getJSONObject("posts")
            val itemsArray = postsObject.getJSONArray("items")

            Log.d("MainActivity", "parsePostsJson: JSON array has ${itemsArray.length()} items")

            for (i in 0 until itemsArray.length()) {
                val postObject = itemsArray.getJSONObject(i)

                val id = postObject.getInt("id")
                val title = postObject.getString("title")
                val url = postObject.getString("url")
                val excerpt = postObject.getString("excerpt")
                val content = postObject.getString("content")
                val date = postObject.getString("date")
                val featured_image = postObject.optString("featured_image", "")
                val sticky = postObject.optBoolean("sticky", false)

                val post = WordPressPost(id, title, url, excerpt, content, date, featured_image, sticky)
                posts.add(post)

                Log.d("MainActivity", "parsePostsJson: Parsed post $i - ID: $id, Title: $title, URL: $url")
            }

            Log.d("MainActivity", "parsePostsJson: Successfully parsed ${posts.size} posts")
        } catch (e: Exception) {
            Log.e("MainActivity", "parsePostsJson: Error parsing JSON", e)
            throw e
        }

        return posts
    }
}