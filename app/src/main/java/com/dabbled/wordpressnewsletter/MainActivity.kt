package com.dabbled.wordpressnewsletter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

// Data class for Locations - simplified to just store what we need
data class Location(
    val id: Int,
    val name: String
)

class MainActivity : AppCompatActivity() {

    private lateinit var locationSpinner: Spinner
    private val locations = mutableListOf<Location>()

    // WordPress site URL - Updated to custom API
    private val WORDPRESS_URL = "https://dandysite.com/yycd/wp-json/ds/v1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("MainActivity", "onCreate: App started")
        setupLocationSpinner()
        fetchLocations()
    }

    private fun setupLocationSpinner() {
        locationSpinner = findViewById(R.id.location_spinner)

        locationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    // "Select a location" - do nothing
                    Log.d("MainActivity", "Prompt selected - no action")
                } else {
                    // Navigate to ArticleActivity with selected location
                    val location = locations[position - 1]
                    Log.d("MainActivity", "Location selected: ${location.name} (ID: ${location.id})")

                    val intent = Intent(this@MainActivity, ArticleActivity::class.java)
                    intent.putExtra("location_id", location.id)
                    startActivity(intent)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
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
                    locations.clear()
                    locations.addAll(locationsData)

                    // Setup spinner adapter with "Select a location" as first item
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
            val apiUrl = "$WORDPRESS_URL/locations"
            Log.d("MainActivity", "getLocationsFromWordPress: Making request to: $apiUrl")

            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")

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
                    Log.e("MainActivity", "getLocationsFromWordPress: HTTP Error: $responseCode")
                    throw Exception("HTTP Error: $responseCode")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun parseLocationsJson(jsonString: String): List<Location> {
        Log.d("MainActivity", "parseLocationsJson: Starting to parse JSON")
        Log.d("MainActivity", "parseLocationsJson: Raw JSON: $jsonString")
        val locations = mutableListOf<Location>()

        try {
            // The API returns an object with a "locations" array, not a direct array
            val jsonObject = JSONObject(jsonString)
            val jsonArray = jsonObject.getJSONArray("locations")
            Log.d("MainActivity", "parseLocationsJson: JSON array has ${jsonArray.length()} items")

            for (i in 0 until jsonArray.length()) {
                val locationObject = jsonArray.getJSONObject(i)
                Log.d("MainActivity", "parseLocationsJson: Location $i JSON: $locationObject")

                val id = locationObject.getInt("id")
                // Use "name" field for the location name
                val name = locationObject.getString("name")

                val location = Location(id, name)
                locations.add(location)

                Log.d("MainActivity", "parseLocationsJson: Parsed location $i - ID: $id, Name: $name")
            }

            Log.d("MainActivity", "parseLocationsJson: Successfully parsed ${locations.size} locations")
        } catch (e: Exception) {
            Log.e("MainActivity", "parseLocationsJson: Error parsing JSON", e)
            Log.e("MainActivity", "parseLocationsJson: JSON string was: $jsonString")
            throw e
        }

        return locations
    }
}