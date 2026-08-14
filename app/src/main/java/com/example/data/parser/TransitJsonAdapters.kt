package com.example.data.parser

import com.example.data.model.Poi
import com.example.data.model.Street
import com.example.data.model.StreetJunction
import com.example.data.model.TransitDataset
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class PoiArrayTypeAdapter : JsonDeserializer<Poi> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Poi {
        if (!json.isJsonArray) {
            return Poi(name = "")
        }

        val array = json.asJsonArray

        // [0] Name
        val name = if (array.size() > 0 && !array[0].isJsonNull) array[0].asString else ""

        // [1] Alternative Names
        val altNames = mutableListOf<String>()
        if (array.size() > 1 && array[1].isJsonArray) {
            array[1].asJsonArray.forEach { item ->
                if (!item.isJsonNull) altNames.add(item.asString)
            }
        }

        // [2] Localized Names (JSON Object)
        val locNames = mutableMapOf<String, String>()
        if (array.size() > 2 && array[2].isJsonObject) {
            array[2].asJsonObject.entrySet().forEach { (k, v) ->
                if (!v.isJsonNull) locNames[k] = v.asString
            }
        }

        // [3] Coordinates [longitude, latitude]
        val coords = mutableListOf<Double>()
        if (array.size() > 3 && array[3].isJsonArray) {
            array[3].asJsonArray.forEach { item ->
                if (!item.isJsonNull) {
                    try {
                        coords.add(item.asDouble)
                    } catch (_: Exception) {}
                }
            }
        }

        // [4] Address
        val address = if (array.size() > 4 && !array[4].isJsonNull) array[4].asString else null

        // [5] Type / Category
        val type = if (array.size() > 5 && !array[5].isJsonNull) array[5].asString else null

        return Poi(
            name = name,
            alternativeNames = altNames,
            localizedNames = locNames,
            coordinates = coords,
            address = address,
            type = type
        )
    }
}

class StreetArrayTypeAdapter : JsonDeserializer<Street> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Street {
        if (!json.isJsonArray) {
            return Street(name = "")
        }

        val array = json.asJsonArray

        // [0] Name
        val name = if (array.size() > 0 && !array[0].isJsonNull) array[0].asString else ""

        // [1] Alternative Names
        val altNames = mutableListOf<String>()
        if (array.size() > 1 && array[1].isJsonArray) {
            array[1].asJsonArray.forEach { item ->
                if (!item.isJsonNull) altNames.add(item.asString)
            }
        }

        // [2] Coordinates [[lon, lat], [lon, lat], ...]
        val coords = mutableListOf<List<Double>>()
        if (array.size() > 2 && array[2].isJsonArray) {
            array[2].asJsonArray.forEach { pointElement ->
                if (pointElement.isJsonArray) {
                    val point = mutableListOf<Double>()
                    pointElement.asJsonArray.forEach { coord ->
                        if (!coord.isJsonNull) {
                            try {
                                point.add(coord.asDouble)
                            } catch (_: Exception) {}
                        }
                    }
                    if (point.isNotEmpty()) {
                        coords.add(point)
                    }
                }
            }
        }

        // [3] Region
        val region = if (array.size() > 3 && !array[3].isJsonNull) array[3].asString else null

        return Street(
            name = name,
            alternativeNames = altNames,
            coordinates = coords,
            region = region
        )
    }
}

class StreetJunctionArrayTypeAdapter : JsonDeserializer<StreetJunction> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): StreetJunction {
        if (!json.isJsonArray) {
            return StreetJunction(streetRef = "")
        }

        val array = json.asJsonArray

        // [0] Street Reference
        val streetRef = if (array.size() > 0 && !array[0].isJsonNull) array[0].asString else ""

        // [1] Coordinates [lon, lat]
        val coords = mutableListOf<Double>()
        if (array.size() > 1 && array[1].isJsonArray) {
            array[1].asJsonArray.forEach { item ->
                if (!item.isJsonNull) {
                    try {
                        coords.add(item.asDouble)
                    } catch (_: Exception) {}
                }
            }
        }

        return StreetJunction(
            streetRef = streetRef,
            coordinates = coords
        )
    }
}

object TransitGsonParser {
    val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Poi::class.java, PoiArrayTypeAdapter())
            .registerTypeAdapter(Street::class.java, StreetArrayTypeAdapter())
            .registerTypeAdapter(StreetJunction::class.java, StreetJunctionArrayTypeAdapter())
            .create()
    }

    fun parseDataset(jsonString: String): TransitDataset {
        return gson.fromJson(jsonString, TransitDataset::class.java)
    }
}
