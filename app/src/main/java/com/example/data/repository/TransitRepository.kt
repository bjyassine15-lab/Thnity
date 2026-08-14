package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.PoiDao
import com.example.data.local.PoiEntity
import com.example.data.local.StreetDao
import com.example.data.local.StreetEntity
import com.example.data.local.StreetJunctionDao
import com.example.data.local.StreetJunctionEntity
import com.example.data.local.TransitEncryptedDatabase
import com.example.data.model.Poi
import com.example.data.model.Street
import com.example.data.model.StreetJunction
import com.example.data.model.TransitDataset
import com.example.data.parser.TransitGsonParser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class TransitRepository(
    private val poiDao: PoiDao,
    private val streetDao: StreetDao,
    private val junctionDao: StreetJunctionDao,
    private val database: TransitEncryptedDatabase,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    val allPois: Flow<List<Poi>> = poiDao.getAllPoisFlow()
        .map { entities -> entities.map { it.toDomain() } }
        .catch { emit(emptyList()) }

    val allStreets: Flow<List<Street>> = streetDao.getAllStreetsFlow()
        .map { entities -> entities.map { it.toDomain() } }
        .catch { emit(emptyList()) }

    val allJunctions: Flow<List<StreetJunction>> = junctionDao.getAllJunctionsFlow()
        .map { entities -> entities.map { it.toDomain() } }
        .catch { emit(emptyList()) }

    val poiCount: Flow<Int> = poiDao.getPoiCountFlow().catch { emit(0) }
    val streetCount: Flow<Int> = streetDao.getStreetCountFlow().catch { emit(0) }
    val junctionCount: Flow<Int> = junctionDao.getJunctionCountFlow().catch { emit(0) }

    suspend fun isLocalDatasetReady(): Boolean = withContext(Dispatchers.IO) {
        poiDao.getPoiCount() > 0 &&
            streetDao.getStreetCount() > 0 &&
            junctionDao.getJunctionCount() > 0
    }

    suspend fun importDataset(dataset: TransitDataset) = withContext(Dispatchers.IO) {
        val poiEntities = dataset.pois.map { poi ->
            PoiEntity(
                name = poi.name,
                alternativeNames = poi.alternativeNames,
                localizedNames = poi.localizedNames,
                longitude = poi.longitude,
                latitude = poi.latitude,
                address = poi.address,
                type = poi.type
            )
        }

        val streetEntities = dataset.streets.map { street ->
            StreetEntity(
                name = street.name,
                alternativeNames = street.alternativeNames,
                coordinates = street.coordinates,
                region = street.region
            )
        }

        val junctionEntities = dataset.streetJunctions.map { junction ->
            StreetJunctionEntity(
                streetRef = junction.streetRef,
                longitude = junction.longitude,
                latitude = junction.latitude
            )
        }

        // Replace the complete dataset atomically. If SQLCipher/Room fails at
        // any point, the previous valid dataset remains intact instead of
        // leaving the application with zero or partially inserted rows.
        database.withTransaction {
            poiDao.clearAll()
            streetDao.clearAll()
            junctionDao.clearAll()
            poiDao.insertPois(poiEntities)
            streetDao.insertStreets(streetEntities)
            junctionDao.insertJunctions(junctionEntities)
        }
    }

    suspend fun importJson(jsonString: String): Result<TransitDataset> = withContext(Dispatchers.IO) {
        try {
            val dataset = TransitGsonParser.parseDataset(jsonString)
            importDataset(dataset)
            Result.success(dataset)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun seedInitialDataIfEmpty(force: Boolean = false) = withContext(Dispatchers.IO) {
        // Do not rewrite the encrypted database on every launch. Besides being
        // expensive, an unnecessary open/write cycle can abort application
        // startup on devices where the SQLCipher provider is still warming up.
        val hasCompleteLocalDataset = poiDao.getPoiCount() > 0 &&
            streetDao.getStreetCount() > 0 &&
            junctionDao.getJunctionCount() > 0
        if (!force && hasCompleteLocalDataset) {
            return@withContext
        }

        val sampleJson = """
        {
          "_version": "3.1",
          "_fields": {
            "pois": ["name", "alternativeNames", "localizedNames", "coordinates", "address", "type"],
            "streets": ["name", "alternativeNames", "coordinates", "region"],
            "streetJunctions": ["streetRef", "coordinates"]
          },
          "pois": [
            [
              "شارع الحبيب بورقيبة - تونس",
              ["Avenue Habib Bourguiba", "Bourguiba Ave"],
              {"ar": "شارع الحبيب بورقيبة", "fr": "Avenue Habib Bourguiba", "en": "Habib Bourguiba Ave"},
              [10.1815316, 36.800185],
              "وسط المدينة، تونس العاصمة",
              "محور رئيسي / Main Avenue"
            ],
            [
              "ساحة برشلونة (محطة النقل المركزية)",
              ["Place Barcelone", "Barcelona Station"],
              {"ar": "ساحة برشلونة", "fr": "Place Barcelone", "en": "Barcelona Square Station"},
              [10.180421, 36.795821],
              "تونس العاصمة",
              "محطة قطارات ومترو / Transit Hub"
            ],
            [
              "ساحة باستور (محطة الحافلات والترام)",
              ["Place Pasteur", "Pasteur Metro"],
              {"ar": "ساحة باستور", "fr": "Place Pasteur", "en": "Pasteur Square Station"},
              [10.178234, 36.817419],
              "تونس",
              "محطة تبادلية / Metro Station"
            ],
            [
              "محطة تونس البحرية (TGM)",
              ["Tunis Marine", "Gare TGM Tunis Marine"],
              {"ar": "تونس البحرية", "fr": "Tunis Marine", "en": "Tunis Marine TGM"},
              [10.191243, 36.800542],
              "مدخل بحيرة تونس",
              "محطة قطار الضاحية الشمالية / Train Station"
            ],
            [
              "مطار تونس قرطاج الدولي",
              ["Tunis Carthage Airport", "Aéroport Tunis Carthage"],
              {"ar": "مطار تونس قرطاج", "fr": "Aéroport Tunis-Carthage", "en": "Tunis-Carthage Airport"},
              [10.227218, 36.851034],
              "طريق المطار، تونس",
              "مطار دولي / International Airport"
            ],
            [
              "المدينة العتيقة - باب سويقة",
              ["Bab Souika", "Medina Bab Souika"],
              {"ar": "باب سويقة", "fr": "Bab Souika", "en": "Bab Souika Historic Gate"},
              [10.166412, 36.808120],
              "المدينة العتيقة، تونس",
              "نقطة جذب تاريخية / Heritage Site"
            ]
          ],
          "streets": [
            [
              "شارع الحبيب بورقيبة",
              ["Habib Bourguiba"],
              [
                [10.17421, 36.79981],
                [10.18012, 36.80020],
                [10.18560, 36.80060],
                [10.19124, 36.80054]
              ],
              "تونس الوسطى"
            ],
            [
              "شارع محمد الخامس",
              ["Avenue Mohamed V"],
              [
                [10.18560, 36.80060],
                [10.18430, 36.80950],
                [10.18120, 36.81740],
                [10.17823, 36.82510]
              ],
              "تونس التجارية"
            ],
            [
              "خط مترو تونس السريع (الخط 1)",
              ["Tunis Light Rail Line 1"],
              [
                [10.18042, 36.79582],
                [10.17950, 36.78500],
                [10.18200, 36.76500]
              ],
              "الضاحية الجنوبية"
            ]
          ],
          "streetJunctions": [
            ["تقاطع شارع بورقيبة مع شارع محمد الخامس", [10.18560, 36.80060]],
            ["تقاطع ساحة باستور مع شارع الحرية", [10.17823, 36.81740]],
            ["تقاطع باب عليوة مع ساحة برشلونة", [10.18042, 36.79582]]
          ]
        }
        """.trimIndent()

        val parsed = TransitGsonParser.parseDataset(sampleJson)
        importDataset(parsed)
    }

    suspend fun restoreDefaultData() = seedInitialDataIfEmpty(force = true)

    suspend fun clearDatabase() = withContext(Dispatchers.IO) {
        database.withTransaction {
            poiDao.clearAll()
            streetDao.clearAll()
            junctionDao.clearAll()
        }
    }

    // Sync dataset from Cloud Firestore if available
    suspend fun syncWithCloudFirestore(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("transit_dataset").document("latest").get().await()
            if (!doc.exists()) {
                return@withContext Result.failure(
                    IllegalStateException("لم توجد مجموعة بيانات سحابية في transit_dataset/latest")
                )
            }
            val jsonString = doc.getString("jsonString")
            require(!jsonString.isNullOrBlank()) {
                "مجموعة البيانات السحابية فارغة"
            }
            val dataset = TransitGsonParser.parseDataset(jsonString)
            validateDataset(dataset)
            importDataset(dataset)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Push new dataset to Cloud Firestore (Admin capability)
    suspend fun uploadDatasetToCloud(jsonString: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Validate JSON completely before touching local or cloud data.
            val dataset = TransitGsonParser.parseDataset(jsonString)
            validateDataset(dataset)

            // Save to Firestore first. A permission/network failure must not
            // leave the device showing a locally changed dataset as if upload
            // had succeeded.

            val cloudPayload = hashMapOf(
                "version" to dataset.version,
                "jsonString" to jsonString,
                "poiCount" to dataset.pois.size,
                "streetCount" to dataset.streets.size,
                "junctionCount" to dataset.streetJunctions.size,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("transit_dataset").document("latest").set(cloudPayload).await()
            // Only after Firestore confirms the write, replace local data in one
            // SQLCipher transaction. Any local failure is returned explicitly.
            importDataset(dataset)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun validateDataset(dataset: TransitDataset) {
        require(dataset.pois.isNotEmpty()) { "مجموعة البيانات لا تحتوي على محطات" }
        require(dataset.streets.isNotEmpty()) { "مجموعة البيانات لا تحتوي على مسارات" }
        require(dataset.streetJunctions.isNotEmpty()) { "مجموعة البيانات لا تحتوي على تقاطعات" }
    }

    private fun PoiEntity.toDomain() = Poi(
        name = name,
        alternativeNames = alternativeNames,
        localizedNames = localizedNames,
        coordinates = listOf(longitude, latitude),
        address = address,
        type = type
    )

    private fun StreetEntity.toDomain() = Street(
        name = name,
        alternativeNames = alternativeNames,
        coordinates = coordinates,
        region = region
    )

    private fun StreetJunctionEntity.toDomain() = StreetJunction(
        streetRef = streetRef,
        coordinates = listOf(longitude, latitude)
    )
}
