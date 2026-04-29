package com.dabbled.yycd.repository

import com.dabbled.yycd.ApiConfig
import com.dabbled.yycd.model.Location
import com.dabbled.yycd.model.LocationDetail
import com.dabbled.yycd.model.LocationsResponse
import com.dabbled.yycd.model.PostsResponse
import com.dabbled.yycd.model.SplashData
import com.dabbled.yycd.model.WordPressPost
import com.dabbled.yycd.network.createHttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class YYCDRepository {

    private val client = createHttpClient()

    suspend fun getSplashData(): SplashData {
        return client.get("${ApiConfig.BASE_URL}/splash").body()
    }

    suspend fun getLocations(): List<Location> {
        val response: LocationsResponse = client.get("${ApiConfig.BASE_URL}/locations").body()
        return response.locations.map { Location(it.id, it.name) }
    }

    suspend fun getLocationDetail(locationId: Int): LocationDetail {
        val response: LocationsResponse = client.get("${ApiConfig.BASE_URL}/locations").body()
        return response.locations.first { it.id == locationId }
    }

    suspend fun getPostsForLocation(locationId: Int, page: Int = 1): List<WordPressPost> {
        val response: PostsResponse = client
            .get("${ApiConfig.BASE_URL}/locations/$locationId/posts") {
                parameter("per_page", ApiConfig.POSTS_PER_PAGE)
                parameter("page", page)
            }
            .body()
        return response.posts.items
    }

    fun close() {
        client.close()
    }
}
