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
import com.dabbled.yycd.model.LocationDetail
import com.dabbled.yycd.model.WordPressPost
import com.dabbled.yycd.repository.YYCDRepository
import kotlinx.coroutines.*

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
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.titleText.text = post.title
        holder.excerptText.text = post.excerpt
        holder.dateText.text = post.date
        holder.cardView.setOnClickListener { onPostClick(post) }
    }

    override fun getItemCount() = posts.size
}

class ArticleActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var contactButton: Button
    private lateinit var adapter: PostAdapter
    private val posts = mutableListOf<WordPressPost>()
    private val repository = YYCDRepository()

    private var locationId: Int = 0
    private var locationDetail: LocationDetail? = null

    private var currentPage = 1
    private var isLoading = false
    private var hasMorePages = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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

    override fun onDestroy() {
        super.onDestroy()
        repository.close()
    }

    private fun setupViews() {
        contactButton = findViewById(R.id.contact_button)
        recyclerView = findViewById(R.id.recycler_view)

        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        adapter = PostAdapter(posts) { post -> openArticleDetail(post) }
        recyclerView.adapter = adapter

        contactButton.setOnClickListener { showContactDialog() }

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
        val intent = Intent(this, ArticleDetailActivity::class.java)
        intent.putExtra("article_content", post.content)
        intent.putExtra("article_title", post.title)
        intent.putExtra("featured_image_url", post.featuredImage)
        startActivity(intent)
    }

    private fun fetchLocationDetails() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val location = repository.getLocationDetail(locationId)
                withContext(Dispatchers.Main) {
                    supportActionBar?.title = location.name
                    locationDetail = location
                    Log.d("ArticleActivity", "Location details loaded: ${location.name}")
                }
            } catch (e: Exception) {
                Log.e("ArticleActivity", "Error fetching location details", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ArticleActivity, "Error loading location details: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun fetchPosts(page: Int = 1) {
        if (isLoading) return
        isLoading = true

        if (page == 1) {
            Toast.makeText(this, "Loading newsletters...", Toast.LENGTH_SHORT).show()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val postsData = repository.getPostsForLocation(locationId, page)

                withContext(Dispatchers.Main) {
                    if (page == 1) {
                        posts.clear()
                        posts.addAll(postsData)
                        adapter = PostAdapter(posts) { post -> openArticleDetail(post) }
                        recyclerView.adapter = adapter
                    } else {
                        val oldSize = posts.size
                        posts.addAll(postsData)
                        adapter.notifyItemRangeInserted(oldSize, postsData.size)

                        if (postsData.isNotEmpty()) {
                            Toast.makeText(this@ArticleActivity, "Loaded ${postsData.size} more newsletters", Toast.LENGTH_SHORT).show()
                        }
                    }

                    currentPage = page
                    hasMorePages = postsData.size == 10
                    isLoading = false

                    if (posts.isEmpty()) {
                        Toast.makeText(this@ArticleActivity, "No newsletters found for this location", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ArticleActivity", "Error fetching posts", e)
                withContext(Dispatchers.Main) {
                    isLoading = false
                    Toast.makeText(this@ArticleActivity, "Error loading newsletters: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showContactDialog() {
        val location = locationDetail ?: run {
            Toast.makeText(this, "Location details not loaded yet", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_contact, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Contact ${location.name}")
            .setView(dialogView)
            .setNegativeButton("Close", null)
            .create()

        val callButton: Button = dialogView.findViewById(R.id.btn_call)
        val textButton: Button = dialogView.findViewById(R.id.btn_text)
        val emailButton: Button = dialogView.findViewById(R.id.btn_email)
        val directionsButton: Button = dialogView.findViewById(R.id.btn_directions)

        if (location.phone.isNotEmpty()) {
            callButton.text = "Call ${location.phone}"
            callButton.setOnClickListener {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${location.phone}")))
                dialog.dismiss()
            }
            textButton.text = "Text ${location.phone}"
            textButton.setOnClickListener {
                startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${location.phone}")))
                dialog.dismiss()
            }
        } else {
            callButton.visibility = View.GONE
            textButton.visibility = View.GONE
        }

        if (location.email.isNotEmpty()) {
            emailButton.text = "Email ${location.email}"
            emailButton.setOnClickListener {
                startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${location.email}")))
                dialog.dismiss()
            }
        } else {
            emailButton.visibility = View.GONE
        }

        if (location.latitude != 0.0 && location.longitude != 0.0) {
            directionsButton.setOnClickListener {
                val gmmIntentUri = Uri.parse("geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}(${location.name})")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                if (mapIntent.resolveActivity(packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}")))
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
            android.R.id.home -> { finish(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }
}