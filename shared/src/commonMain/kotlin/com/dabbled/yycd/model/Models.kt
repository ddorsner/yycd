/*
* Copyright (c) 2026 Dabbled Studios
*/

package com.dabbled.yycd.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.KSerializer

@Serializable
data class Location(
    val id: Int,
    val name: String
)

@Serializable
data class SplashData(
    @SerialName("title_url") val titleUrl: String = "",
    @SerialName("splash_url") val splashUrl: String = "",
    @SerialName("splash_text") val splashText: String = ""
)

@Serializable
data class WordPressPost(
    val id: Int,
    val title: String,
    val url: String,
    val excerpt: String,
    val content: String,
    val date: String,
    @SerialName("featured_image")
    @Serializable(with = FeaturedImageSerializer::class)
    val featuredImage: String = "",
    val sticky: Boolean = false
)

object FeaturedImageSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FeaturedImage", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return decoder.decodeString()
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> when {
                element.booleanOrNull != null -> ""
                else -> element.content
            }
            else -> ""
        }
    }
}

@Serializable
data class LocationDetail(
    val id: Int,
    val name: String,
    val phone: String = "",
    val email: String = "",
    @Serializable(with = LatLngStringSerializer::class)
    val latitude: Double = 0.0,
    @Serializable(with = LatLngStringSerializer::class)
    val longitude: Double = 0.0
)

object LatLngStringSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LatLng", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Double) {
        encoder.encodeDouble(value)
    }

    override fun deserialize(decoder: Decoder): Double {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return decoder.decodeDouble()
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> element.content.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }
}

@Serializable
internal data class LocationsResponse(
    val locations: List<LocationDetail>
)

@Serializable
internal data class PostsResponse(
    val posts: PostsItems
)

@Serializable
internal data class PostsItems(
    val items: List<WordPressPost>
)