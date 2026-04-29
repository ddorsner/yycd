package com.dabbled.wordpressnewsletter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dabbled.yycd.model.Location
import com.dabbled.yycd.repository.YYCDRepository
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var locationSpinner: Spinner
    private lateinit var logoImage: ImageView
    private lateinit var splashImage: ImageView
    private lateinit var titleText: TextView
    private val locations = mutableListOf<Location>()
    private val repository = YYCDRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("MainActivity", "onCreate: App started")

        logoImage = findViewById(R.id.logo_image)
        splashImage = findViewById(R.id.location_title_sp)
        titleText = findViewById(R.id.title_text)

        setupLocationSpinner()
        fetchSplashData()
        fetchLocations()
    }

    override fun onDestroy() {
        super.onDestroy()
        repository.close()
    }

    private fun setupLocationSpinner() {
        locationSpinner = findViewById(R.id.location_spinner)

        locationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    Log.d("MainActivity", "Prompt selected - no action")
                } else {
                    val location = locations[position - 1]
                    Log.d("MainActivity", "Location selected: ${location.name} (ID: ${location.id})")

                    val intent = Intent(this@MainActivity, ArticleActivity::class.java)
                    intent.putExtra("location_id", location.id)
                    startActivity(intent)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun fetchSplashData() {
        Log.d("MainActivity", "fetchSplashData: Starting")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val splashData = repository.getSplashData()

                withContext(Dispatchers.Main) {
                    titleText.text = splashData.splashText

                    if (splashData.titleUrl.isNotEmpty()) {
                        ImageLoader.loadImage(splashData.titleUrl, logoImage)
                    }

                    if (splashData.splashUrl.isNotEmpty()) {
                        ImageLoader.loadImage(splashData.splashUrl, splashImage)
                    } else {
                        splashImage.visibility = View.GONE
                    }
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "fetchSplashData: Error", e)
                withContext(Dispatchers.Main) {
                    titleText.text = getString(R.string.yes_you_can_dance_newsletter)
                    Toast.makeText(this@MainActivity, "Error loading splash data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchLocations() {
        Log.d("MainActivity", "fetchLocations: Starting")
        Toast.makeText(this, "Loading locations...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val locationsData = repository.getLocations()

                withContext(Dispatchers.Main) {
                    locations.clear()
                    locations.addAll(locationsData)

                    val spinnerItems = mutableListOf("Select a location")
                    spinnerItems.addAll(locations.map { it.name })

                    val spinnerAdapter = ArrayAdapter(
                        this@MainActivity,
                        android.R.layout.simple_spinner_item,
                        spinnerItems
                    )
                    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    locationSpinner.adapter = spinnerAdapter

                    Log.d("MainActivity", "fetchLocations: Spinner populated with ${locations.size} locations")
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "fetchLocations: Error", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error loading locations: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}