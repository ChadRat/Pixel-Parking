package com.example.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ParkingSpot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.JsonReader
import android.util.JsonToken
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

internal enum class VectorRoadClass {
    MOTORWAY_PRIMARY,
    SECONDARY,
    TERTIARY,
    RESIDENTIAL,
    SERVICE,
    PEDESTRIAN,
    FOOTPATH,
    CYCLEWAY
}

internal enum class VectorPoiCategory {
    PHARMACY,
    MEDICAL,
    SPORTS_SWIMMING,
    HOTEL_LODGING,
    WORSHIP,
    LANDMARK_TOURISM,
    CULTURE_CINEMA,
    SHOP,
    FOOD_CAFE,
    PARKING,
    FINANCE,
    OFFICE_COMPANY,
    BENCH,
    WASTE_BASKET,
    TREE,
    GENERIC
}

internal data class VectorBuildingPoly(
    val lats: DoubleArray,
    val lngs: DoubleArray,
    val centerLat: Double,
    val centerLng: Double,
    val houseNumber: String?,
    val name: String?,
    val poiCategory: VectorPoiCategory?
)

internal data class VectorAreaPoly(
    val lats: DoubleArray,
    val lngs: DoubleArray,
    val centerLat: Double,
    val centerLng: Double,
    val isForestOrWood: Boolean,
    val isWaterOrPool: Boolean,
    val name: String?,
    val poiCategory: VectorPoiCategory?
)

internal data class VectorRoadWay(
    val lats: DoubleArray,
    val lngs: DoubleArray,
    val roadClass: VectorRoadClass,
    val name: String?,
    val nameEl: String?,
    val nameEn: String?,
    val onewayDir: Int // 0 = two-way, 1 = forward, -1 = reverse
)

internal data class VectorPoiNode(
    val lat: Double,
    val lng: Double,
    val name: String?,
    val houseNumber: String?,
    val category: VectorPoiCategory
)

internal data class VectorMapCell(
    val key: String,
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
    val areas: List<VectorAreaPoly>,
    val buildings: List<VectorBuildingPoly>,
    val roads: List<VectorRoadWay>,
    val pois: List<VectorPoiNode>
)

private data class VectorCellRequest(
    val cacheDir: File,
    val latIdx: Int,
    val lngIdx: Int,
    val key: String,
    val diskOnly: Boolean,
    val onCellLoaded: () -> Unit
)

/**
 * Infinite-Resolution Vector HD Map Engine for high and ultra-high zoom levels (zoom = 16.2 .. 24.0).
 * Uses a streaming JsonReader parser and compact binary disk cache so Overpass responses never allocate
 * large DOM trees in RAM.
 */
internal object HighZoomVectorEngine {
    private const val CELL_STEP_DEG = 0.008 // ~880m x 880m neighborhood cell
    private const val VECTOR_BIN_CACHE_FOLDER = "osm_vector_hd_bin_v9"
    private const val BIN_MAGIC = 0x564D4339 // "VMC9"
    private const val MAX_CACHED_CELLS = 36
    private const val MAX_BUILDINGS_PER_CELL = 1100
    private const val MAX_ROADS_PER_CELL = 450
    private const val MAX_AREAS_PER_CELL = 140
    private const val MAX_POIS_PER_CELL = 900
    private const val MAX_MICRO_OR_UNNAMED_POIS_PER_CELL = 180
    private const val MAX_VERTICES_PER_GEOMETRY = 384

    internal data class PriorityGreekPoi(
        val lat: Double,
        val lng: Double,
        val nameEl: String,
        val nameEn: String,
        val category: VectorPoiCategory,
        val priorityRank: Int // -3 = top priority (Αρχοντικό Πανόραμα, La Gantem), -2 = Melivoia & Thessaloniki POIs
    )

    // Curated priority POIs for Melivoia (Μελιβοία Λάρισας), Larissa (Λάρισα), Thessaloniki (Central, West & North),
    // and Athens (Αθήνα) so they always render immediately and with highest priority.
    private val PRIORITY_GREEK_POIS: List<PriorityGreekPoi> = listOf(
        // --- Melivoia (Μελιβοία Λάρισας) Priority POIs ---
        PriorityGreekPoi(
            lat = 39.750671,
            lng = 22.792917,
            nameEl = "Αρχοντικό Πανόραμα",
            nameEn = "Αρχοντικό Πανόραμα (Mansion Panorama)",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -3
        ),
        PriorityGreekPoi(
            lat = 39.749186,
            lng = 22.795712,
            nameEl = "La Gantem",
            nameEn = "La Gantem",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -3
        ),
        PriorityGreekPoi(
            lat = 39.749040,
            lng = 22.795420,
            nameEl = "Η Πλατεία",
            nameEn = "Η Πλατεία (Taverna & Cafe)",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.748081,
            lng = 22.797403,
            nameEl = "Αρχοντικό Φιλοκτήτης",
            nameEn = "Αρχοντικό Φιλοκτήτης (Archontiko Philoktitis)",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.751450,
            lng = 22.794850,
            nameEl = "Melivista",
            nameEn = "Melivista",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.749520,
            lng = 22.795180,
            nameEl = "Melivoia Apartment",
            nameEn = "Melivoia Apartment",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.748750,
            lng = 22.794650,
            nameEl = "Airbnb Παραδοσιακό Σπίτι Μελιβοίας",
            nameEn = "Airbnb Melivoia Traditional House",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.750120,
            lng = 22.796380,
            nameEl = "Ξενώνας Κίσσαβος Μελιβοία",
            nameEn = "Kissavos Guesthouse Melivoia",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.748890,
            lng = 22.796050,
            nameEl = "Ταβέρνα Ο Κίσσαβος",
            nameEn = "Taverna O Kissavos",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.749310,
            lng = 22.795920,
            nameEl = "Καφενείο Η Πλάκα",
            nameEn = "Kafenio I Plaka",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.749420,
            lng = 22.795580,
            nameEl = "Φαρμακείο Μαγειρίας Ραφαήλ & Σία Ο.Ε.",
            nameEn = "Mageirias Pharmacy Melivoia",
            category = VectorPoiCategory.PHARMACY,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.749120,
            lng = 22.795610,
            nameEl = "Πλατεία Δημάρχου Γιάννη Γάλλου (Πλάκα)",
            nameEn = "Plaka Central Square Melivoia",
            category = VectorPoiCategory.LANDMARK_TOURISM,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.749850,
            lng = 22.794950,
            nameEl = "Ιερός Ναός Αγίου Νικολάου Μελιβοίας",
            nameEn = "St. Nicholas Church Melivoia",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.748550,
            lng = 22.795250,
            nameEl = "Ιερός Ναός Κοιμήσεως της Θεοτόκου",
            nameEn = "Dormition of the Theotokos Church",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.723500,
            lng = 22.825800,
            nameEl = "Κρινανθός",
            nameEn = "Krinanthos Restaurant",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.743800,
            lng = 22.863200,
            nameEl = "Paradies",
            nameEn = "Paradies Restaurant",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        // --- Larissa (Λάρισα) Priority Restaurants, Airbnbs, Hotels, Churches, Supermarkets & Companies ---
        PriorityGreekPoi(
            lat = 39.644910,
            lng = 22.428210,
            nameEl = "La Gantem",
            nameEn = "La Gantem",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -3
        ),
        PriorityGreekPoi(
            lat = 39.641250,
            lng = 22.416120,
            nameEl = "Ακαμάτρα",
            nameEn = "Akamatra Restaurant Larissa",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.640850,
            lng = 22.415650,
            nameEl = "Βόσπορος",
            nameEn = "Vosporos Larissa",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.641680,
            lng = 22.414850,
            nameEl = "Ο Μπακαλόγατος",
            nameEn = "O Bakalogatos Taverna Larissa",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.641120,
            lng = 22.415220,
            nameEl = "Να με θυμάσαι",
            nameEn = "Na Me Thymasai Larissa",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.641950,
            lng = 22.415850,
            nameEl = "Φρούριο Καφέ Εστιατόριο",
            nameEn = "Frourio Cafe Restaurant Larissa",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.637850,
            lng = 22.416950,
            nameEl = "Las Ramblas",
            nameEn = "Las Ramblas Coffee Bar Larissa",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.637420,
            lng = 22.417350,
            nameEl = "Kubrick Pure Social Club",
            nameEn = "Kubrick Pure Social Club",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.638150,
            lng = 22.416450,
            nameEl = "Sherlock All Day Bar",
            nameEn = "Sherlock All Day Bar Larissa",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.640450,
            lng = 22.414450,
            nameEl = "Εν Λαρίση",
            nameEn = "En Larisi Restaurant",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.638650,
            lng = 22.419150,
            nameEl = "Το Παραδοσιακό",
            nameEn = "To Paradosiako Larissa",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.637550,
            lng = 22.416150,
            nameEl = "Divani Palace Larissa",
            nameEn = "Divani Palace Larissa",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.615850,
            lng = 22.433250,
            nameEl = "Grecotel Larissa Imperial",
            nameEn = "Grecotel Larissa Imperial",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.638850,
            lng = 22.418120,
            nameEl = "Hotel Metropol Larissa",
            nameEn = "Hotel Metropol Larissa",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.641150,
            lng = 22.413650,
            nameEl = "Park Hotel Larissa",
            nameEn = "Park Hotel Larissa",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.639250,
            lng = 22.417250,
            nameEl = "Grand Hotel Larissa",
            nameEn = "Grand Hotel Larissa",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.636450,
            lng = 22.419850,
            nameEl = "Dionissos Hotel Larissa",
            nameEn = "Dionissos Hotel Larissa",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.639150,
            lng = 22.419350,
            nameEl = "Airbnb Κεντρική Πλατεία Λάρισας",
            nameEn = "Airbnb Larissa Central Square Suite",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.641550,
            lng = 22.415450,
            nameEl = "Airbnb Φρούριο Luxury Loft",
            nameEn = "Airbnb Frourio Luxury Loft Larissa",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.637950,
            lng = 22.416750,
            nameEl = "Airbnb Πλατεία Ταχυδρομείου Suite",
            nameEn = "Airbnb Plateia Tachydromeiou Suite",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.643850,
            lng = 22.411250,
            nameEl = "Airbnb Αλκαζάρ Park View Apartment",
            nameEn = "Airbnb Alcazar Park View Apartment",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.629550,
            lng = 22.408550,
            nameEl = "Airbnb Νεάπολη Λάρισας Studio",
            nameEn = "Airbnb Neapoli Larissa Studio",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.631850,
            lng = 22.429550,
            nameEl = "Airbnb Φιλιππούπολη Apartment",
            nameEn = "Airbnb Philippoupoli Larissa Apartment",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.642150,
            lng = 22.415150,
            nameEl = "Ιερός Μητροπολιτικός Ναός Αγίου Αχιλλίου",
            nameEn = "Metropolitan Church of St. Achillios",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.636850,
            lng = 22.422650,
            nameEl = "Ιερός Ναός Αγίου Νικολάου Λάρισας",
            nameEn = "Church of St. Nicholas Larissa",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.640650,
            lng = 22.415550,
            nameEl = "Αρχαίο Θέατρο Λάρισας",
            nameEn = "Ancient Theatre of Larissa",
            category = VectorPoiCategory.LANDMARK_TOURISM,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.646250,
            lng = 22.419850,
            nameEl = "Μύλος του Παππά",
            nameEn = "Mill of Pappas Larissa",
            category = VectorPoiCategory.CULTURE_CINEMA,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.644850,
            lng = 22.409550,
            nameEl = "Πάρκο Αλκαζάρ",
            nameEn = "Alcazar Park Larissa",
            category = VectorPoiCategory.SPORTS_SWIMMING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.638950,
            lng = 22.418950,
            nameEl = "Κεντρική Πλατεία Λάρισας",
            nameEn = "Central Square of Larissa",
            category = VectorPoiCategory.LANDMARK_TOURISM,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.637750,
            lng = 22.416650,
            nameEl = "Πλατεία Ταχυδρομείου",
            nameEn = "Tachydromeiou Square Larissa",
            category = VectorPoiCategory.LANDMARK_TOURISM,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.635250,
            lng = 22.423450,
            nameEl = "Σκλαβενίτης Λάρισα",
            nameEn = "Sklavenitis Supermarket Larissa",
            category = VectorPoiCategory.SHOP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.633850,
            lng = 22.417850,
            nameEl = "ΑΒ Βασιλόπουλος Λάρισα",
            nameEn = "AB Vassilopoulos Larissa",
            category = VectorPoiCategory.SHOP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.640150,
            lng = 22.421450,
            nameEl = "Μασούτης Λάρισα",
            nameEn = "Masoutis Supermarket Larissa",
            category = VectorPoiCategory.SHOP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.638150,
            lng = 22.411450,
            nameEl = "Γενικό Νοσοκομείο Λάρισας",
            nameEn = "General Hospital of Larissa",
            category = VectorPoiCategory.MEDICAL,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.613850,
            lng = 22.407250,
            nameEl = "Πανεπιστημιακό Γενικό Νοσοκομείο Λάρισας",
            nameEn = "University General Hospital of Larissa",
            category = VectorPoiCategory.MEDICAL,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.622450,
            lng = 22.441250,
            nameEl = "Bruno Coffee Stores HQ",
            nameEn = "Bruno Coffee Stores HQ",
            category = VectorPoiCategory.OFFICE_COMPANY,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 39.668500,
            lng = 22.446500,
            nameEl = "Exalco Α.Ε.",
            nameEn = "Exalco Aluminum Systems S.A.",
            category = VectorPoiCategory.OFFICE_COMPANY,
            priorityRank = -2
        ),
        // --- Central & East Thessaloniki (Θεσσαλονίκη) Priority POIs ---
        PriorityGreekPoi(
            lat = 40.635820,
            lng = 22.939540,
            nameEl = "Ούζου Μέλαθρον",
            nameEn = "Ouzou Melathron",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.634780,
            lng = 22.936950,
            nameEl = "Χαρούπι",
            nameEn = "Charoupi Restaurant",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.634920,
            lng = 22.936410,
            nameEl = "Φουλ του Μεζέ",
            nameEn = "Full tou Meze",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.630510,
            lng = 22.947820,
            nameEl = "Δέκα Τραπέζια",
            nameEn = "Deka Trapezia",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.632980,
            lng = 22.939650,
            nameEl = "7 Θάλασσες",
            nameEn = "7 Thalasses",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.632410,
            lng = 22.940120,
            nameEl = "Clochard",
            nameEn = "Clochard Restaurant",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.629840,
            lng = 22.946120,
            nameEl = "Διαγώνιος",
            nameEn = "Diagonios",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.633150,
            lng = 22.942280,
            nameEl = "Τερκενλής",
            nameEn = "Terkenlis",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.629950,
            lng = 22.945350,
            nameEl = "Μπουγάτσα Γιάννης",
            nameEn = "Bougatsa Giannis",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.637850,
            lng = 22.935120,
            nameEl = "Σέμπρικο",
            nameEn = "Sembriko",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.632250,
            lng = 22.934850,
            nameEl = "Kitchen Bar",
            nameEn = "Kitchen Bar Thessaloniki",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.642650,
            lng = 22.955420,
            nameEl = "Τοίχο Τοίχο",
            nameEn = "Toicho Toicho Ano Poli",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.643880,
            lng = 22.951450,
            nameEl = "Ταβέρνα Τσινάρι",
            nameEn = "Tsinari Taverna",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.643520,
            lng = 22.954120,
            nameEl = "Ιγγλέζος",
            nameEn = "Inglezos Ano Poli",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.583420,
            lng = 22.948150,
            nameEl = "Μαύρη Θάλασσα",
            nameEn = "Mavri Thalassa Kalamaria",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.584650,
            lng = 22.947620,
            nameEl = "Νέα Διαγώνιος",
            nameEn = "Nea Diagonios Kalamaria",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.588120,
            lng = 22.953950,
            nameEl = "Το Μανιτάρι",
            nameEn = "To Manitari Sofouli",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.614850,
            lng = 22.971250,
            nameEl = "Παλιά Αθήνα",
            nameEn = "Palia Athina Toumba",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.632850,
            lng = 22.941150,
            nameEl = "Electra Palace Thessaloniki",
            nameEn = "Electra Palace Thessaloniki",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.618420,
            lng = 22.952480,
            nameEl = "Makedonia Palace",
            nameEn = "Makedonia Palace",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.631920,
            lng = 22.940350,
            nameEl = "ON Residence",
            nameEn = "ON Residence",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.629580,
            lng = 22.944720,
            nameEl = "Daios Luxury Living",
            nameEn = "Daios Luxury Living",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.639650,
            lng = 22.922850,
            nameEl = "The Met Hotel",
            nameEn = "The Met Hotel",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.634120,
            lng = 22.938450,
            nameEl = "S Hotel Boutique Thessaloniki",
            nameEn = "S Hotel Boutique Thessaloniki",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.634650,
            lng = 22.939120,
            nameEl = "Colors Urban Hotel",
            nameEn = "Colors Urban Hotel",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.634280,
            lng = 22.942850,
            nameEl = "MonAsty Hotel",
            nameEn = "MonAsty Hotel",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.638850,
            lng = 22.937250,
            nameEl = "Vanoro Hotel",
            nameEn = "Vanoro Hotel",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.635420,
            lng = 22.937650,
            nameEl = "Bahar Boutique Hotel",
            nameEn = "Bahar Boutique Hotel",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.632510,
            lng = 22.941620,
            nameEl = "Airbnb Aristotelous Suite",
            nameEn = "Airbnb Aristotelous Suite",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.635180,
            lng = 22.936780,
            nameEl = "Airbnb Λαδάδικα Loft",
            nameEn = "Airbnb Ladadika Loft",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.627120,
            lng = 22.948950,
            nameEl = "Airbnb Λευκός Πύργος Studio",
            nameEn = "Airbnb White Tower Studio",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.630850,
            lng = 22.949120,
            nameEl = "Airbnb Ναυαρίνου Stay",
            nameEn = "Airbnb Navarinou Stay",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.643120,
            lng = 22.953250,
            nameEl = "Airbnb Αρχοντικό Άνω Πόλης",
            nameEn = "Airbnb Ano Poli Traditional House",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.612450,
            lng = 22.953850,
            nameEl = "Airbnb Νέα Παραλία Luxury Apartment",
            nameEn = "Airbnb Nea Paralia Luxury Apartment",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.583950,
            lng = 22.949120,
            nameEl = "Airbnb Καλαμαριά Riviera Suite",
            nameEn = "Airbnb Kalamaria Riviera Suite",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.615420,
            lng = 22.973450,
            nameEl = "Airbnb Τούμπα Apartment",
            nameEn = "Airbnb Toumba Apartment",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.587850,
            lng = 23.031250,
            nameEl = "Panorama Hotel Thessaloniki",
            nameEn = "Panorama Hotel Thessaloniki",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.626440,
            lng = 22.948420,
            nameEl = "Λευκός Πύργος",
            nameEn = "White Tower of Thessaloniki",
            category = VectorPoiCategory.LANDMARK_TOURISM,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.632680,
            lng = 22.941450,
            nameEl = "Πλατεία Αριστοτέλους",
            nameEn = "Aristotelous Square",
            category = VectorPoiCategory.LANDMARK_TOURISM,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.632250,
            lng = 22.951750,
            nameEl = "Αψίδα Γαλερίου (Καμάρα)",
            nameEn = "Arch of Galerius (Kamara)",
            category = VectorPoiCategory.LANDMARK_TOURISM,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.633350,
            lng = 22.952750,
            nameEl = "Ροτόντα",
            nameEn = "Rotunda Thessaloniki",
            category = VectorPoiCategory.LANDMARK_TOURISM,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.632850,
            lng = 22.946950,
            nameEl = "Ιερός Ναός Αγίας Σοφίας",
            nameEn = "Holy Church of Hagia Sophia",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.638820,
            lng = 22.947550,
            nameEl = "Ιερός Ναός Αγίου Δημητρίου",
            nameEn = "Church of Saint Demetrius",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.630550,
            lng = 22.944250,
            nameEl = "Ιερός Μητροπολιτικός Ναός Αγίου Γρηγορίου Παλαμά",
            nameEn = "Metropolitan Church of St. Gregory Palamas",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.634850,
            lng = 22.941850,
            nameEl = "Αγορά Μοδιάνο",
            nameEn = "Modiano Market",
            category = VectorPoiCategory.SHOP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.633950,
            lng = 22.945120,
            nameEl = "Μασούτης Σούπερ Μάρκετ",
            nameEn = "Masoutis Supermarket",
            category = VectorPoiCategory.SHOP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.621450,
            lng = 22.955250,
            nameEl = "Σκλαβενίτης Σούπερ Μάρκετ",
            nameEn = "Sklavenitis Supermarket",
            category = VectorPoiCategory.SHOP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.628150,
            lng = 22.951150,
            nameEl = "ΑΒ Βασιλόπουλος",
            nameEn = "AB Vassilopoulos Supermarket",
            category = VectorPoiCategory.SHOP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.627850,
            lng = 22.954850,
            nameEl = "ΔΕΘ Helexpo",
            nameEn = "TIF Helexpo",
            category = VectorPoiCategory.OFFICE_COMPANY,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.556250,
            lng = 22.976850,
            nameEl = "Pfizer Digital Innovation Hub",
            nameEn = "Pfizer Digital Innovation Hub",
            category = VectorPoiCategory.OFFICE_COMPANY,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.562450,
            lng = 22.972150,
            nameEl = "Deloitte Competence Center",
            nameEn = "Deloitte Competence Center",
            category = VectorPoiCategory.OFFICE_COMPANY,
            priorityRank = -2
        ),
        // --- West Thessaloniki (Δυτική Θεσσαλονίκη: Εύοσμος, Κορδελιό, Σταυρούπολη, Πολίχνη, Αμπελόκηποι, Μενεμένη, Νεάπολη, Συκιές, Σίνδος) ---
        PriorityGreekPoi(
            lat = 40.668850,
            lng = 22.908550,
            nameEl = "Πλατεία Ευόσμου",
            nameEn = "Evosmos Central Square",
            category = VectorPoiCategory.LANDMARK_TOURISM,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.669150,
            lng = 22.908950,
            nameEl = "Ιερός Ναός Ευαγγελισμού της Θεοτόκου Ευόσμου",
            nameEn = "Church of the Annunciation Evosmos",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.666450,
            lng = 22.906250,
            nameEl = "Ιερός Ναός Αγίου Αθανασίου Ευόσμου",
            nameEn = "Church of St. Athanasius Evosmos",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.668250,
            lng = 22.907850,
            nameEl = "Ταβέρνα Το Στέκι του Ευόσμου",
            nameEn = "To Steki tou Evosmou Taverna",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.669450,
            lng = 22.909650,
            nameEl = "Ψησταριά Ο Γύρος του Ευόσμου",
            nameEn = "Evosmos Grill House",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.668650,
            lng = 22.908150,
            nameEl = "Καφέ Μπαρ Πλατεία Ευόσμου",
            nameEn = "Evosmos Square Cafe Bar",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.669850,
            lng = 22.907450,
            nameEl = "Airbnb Evosmos Central Suite",
            nameEn = "Airbnb Evosmos Central Suite",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.674250,
            lng = 22.912850,
            nameEl = "Airbnb Νέα Πολιτεία Ευόσμου Apartment",
            nameEn = "Airbnb Nea Politeia Evosmos Apartment",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.671250,
            lng = 22.905450,
            nameEl = "Σκλαβενίτης Εύοσμος",
            nameEn = "Sklavenitis Supermarket Evosmos",
            category = VectorPoiCategory.SHOP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.667550,
            lng = 22.910250,
            nameEl = "Μασούτης Εύοσμος",
            nameEn = "Masoutis Supermarket Evosmos",
            category = VectorPoiCategory.SHOP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.672850,
            lng = 22.914250,
            nameEl = "ΑΒ Βασιλόπουλος Εύοσμος",
            nameEn = "AB Vassilopoulos Evosmos",
            category = VectorPoiCategory.SHOP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.665250,
            lng = 22.893850,
            nameEl = "Πλατεία Σκρα Κορδελιού",
            nameEn = "Skra Square Kordelio",
            category = VectorPoiCategory.LANDMARK_TOURISM,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.664850,
            lng = 22.894450,
            nameEl = "Ιερός Ναός Μεταμορφώσεως του Σωτήρος Κορδελιού",
            nameEn = "Transfiguration Church Kordelio",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.665650,
            lng = 22.893250,
            nameEl = "Μεζεδοπωλείο Κορδελιό",
            nameEn = "Kordelio Meze Restaurant",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.663950,
            lng = 22.895150,
            nameEl = "Airbnb Kordelio Comfort Stay",
            nameEn = "Airbnb Kordelio Comfort Stay",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.666150,
            lng = 22.892450,
            nameEl = "Μασούτης Κορδελιό",
            nameEn = "Masoutis Supermarket Kordelio",
            category = VectorPoiCategory.SHOP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.659450,
            lng = 22.931250,
            nameEl = "Μονή Λαζαριστών",
            nameEn = "Moni Lazariston Stavroupoli",
            category = VectorPoiCategory.CULTURE_CINEMA,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.668450,
            lng = 22.930450,
            nameEl = "Ιερός Ναός Αγίου Ελευθερίου Σταυρούπολης",
            nameEn = "St. Eleftherios Church Stavroupoli",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.663850,
            lng = 22.929650,
            nameEl = "Ταβέρνα Η Σταυρούπολη",
            nameEn = "Taverna Stavroupoli",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.658850,
            lng = 22.932150,
            nameEl = "Airbnb Lazaristes Studio Σταυρούπολη",
            nameEn = "Airbnb Lazaristes Studio Stavroupoli",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.674850,
            lng = 22.961450,
            nameEl = "Γενικό Νοσοκομείο Παπαγεωργίου",
            nameEn = "Papageorgiou General Hospital",
            category = VectorPoiCategory.MEDICAL,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.672250,
            lng = 22.952850,
            nameEl = "424 Γενικό Στρατιωτικό Νοσοκομείο",
            nameEn = "424 General Military Hospital",
            category = VectorPoiCategory.MEDICAL,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.662450,
            lng = 22.928850,
            nameEl = "Σκλαβενίτης Σταυρούπολη",
            nameEn = "Sklavenitis Stavroupoli",
            category = VectorPoiCategory.SHOP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.661250,
            lng = 22.948850,
            nameEl = "Ιερός Ναός Αγίου Παντελεήμονος Πολίχνης",
            nameEn = "St. Panteleimon Church Polichni",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.660450,
            lng = 22.947450,
            nameEl = "Μεζεδοπωλείο Πολίχνη",
            nameEn = "Polichni Meze Restaurant",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.662150,
            lng = 22.949650,
            nameEl = "Airbnb Polichni Urban Apartment",
            nameEn = "Airbnb Polichni Urban Apartment",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.659850,
            lng = 22.946250,
            nameEl = "Μασούτης Πολίχνη",
            nameEn = "Masoutis Supermarket Polichni",
            category = VectorPoiCategory.SHOP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.653450,
            lng = 22.921850,
            nameEl = "Πλατεία Επταλόφου Αμπελοκήπων",
            nameEn = "Eptalofos Square Ampelokipoi",
            category = VectorPoiCategory.LANDMARK_TOURISM,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.652850,
            lng = 22.922650,
            nameEl = "Ιερός Ναός Αγίου Παντελεήμονος Αμπελοκήπων",
            nameEn = "St. Panteleimon Church Ampelokipoi",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.651850,
            lng = 22.909450,
            nameEl = "Ιερός Ναός Αγίας Παρασκευής Μενεμένης",
            nameEn = "St. Paraskevi Church Menemeni",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.653850,
            lng = 22.921250,
            nameEl = "Ταβέρνα Επτάλοφος",
            nameEn = "Eptalofos Taverna Ampelokipoi",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.652250,
            lng = 22.923450,
            nameEl = "Airbnb Αμπελόκηποι Apartment",
            nameEn = "Airbnb Ampelokipoi Apartment",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.654250,
            lng = 22.903850,
            nameEl = "Grand Hotel Palace",
            nameEn = "Grand Hotel Palace Thessaloniki",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.653250,
            lng = 22.940850,
            nameEl = "Ιερός Ναός Αγίου Γεωργίου Νεαπόλεως",
            nameEn = "St. George Church Neapoli",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.649450,
            lng = 22.955850,
            nameEl = "Ιερός Ναός Αγίου Ιωάννου Προδρόμου Συκεών",
            nameEn = "St. John Church Sykies",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.652650,
            lng = 22.941650,
            nameEl = "Μεζεδοπωλείο Νεάπολη",
            nameEn = "Neapoli Meze Taverna",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.647850,
            lng = 22.958450,
            nameEl = "Ταβέρνα Το Κάστρο Συκιές",
            nameEn = "To Kastro Taverna Sykies",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.648650,
            lng = 22.957250,
            nameEl = "Airbnb Συκιές Panoramic View",
            nameEn = "Airbnb Sykies Panoramic View",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.653850,
            lng = 22.939850,
            nameEl = "Airbnb Νεάπολη Central Studio",
            nameEn = "Airbnb Neapoli Central Studio",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.657850,
            lng = 22.803850,
            nameEl = "Διεθνές Πανεπιστήμιο της Ελλάδος (Σίνδος)",
            nameEn = "International Hellenic University Sindos",
            category = VectorPoiCategory.LANDMARK_TOURISM,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.669450,
            lng = 22.796250,
            nameEl = "ΒΙ.ΠΕ. Σίνδου Logistics Hub",
            nameEn = "Sindos Industrial Park Hub",
            category = VectorPoiCategory.OFFICE_COMPANY,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.670850,
            lng = 22.805450,
            nameEl = "Ταβέρνα Η Σίνδος",
            nameEn = "Taverna Sindos",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.668450,
            lng = 22.804250,
            nameEl = "Airbnb Σίνδος Campus Studio",
            nameEn = "Airbnb Sindos Campus Studio",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        // --- North Thessaloniki (Βόρεια Θεσσαλονίκη: Ωραιόκαστρο, Πεύκα/Ρετζίκι, Ευκαρπία, Φίλυρο, Ασβεστοχώρι, Λαγκαδάς) ---
        PriorityGreekPoi(
            lat = 40.727850,
            lng = 22.917850,
            nameEl = "Ιερός Ναός Κοιμήσεως της Θεοτόκου Ωραιοκάστρου",
            nameEn = "Dormition of the Theotokos Church Oraiokastro",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.719450,
            lng = 22.922850,
            nameEl = "Ιερός Ναός Τιμίου Σταυρού Γαλήνης Ωραιοκάστρου",
            nameEn = "Holy Cross Church Galini Oraiokastro",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.728450,
            lng = 22.918650,
            nameEl = "Ταβέρνα Το Ωραιόκαστρο",
            nameEn = "Taverna Oraiokastro",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.731250,
            lng = 22.916450,
            nameEl = "Εστιατόριο Άλσος Ωραιοκάστρου",
            nameEn = "Alsos Oraiokastro Restaurant",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.726850,
            lng = 22.919250,
            nameEl = "Καφέ Μπαρ Λεωφόρος Δημοκρατίας Ωραιόκαστρο",
            nameEn = "Oraiokastro Central Cafe Bar",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.729850,
            lng = 22.915250,
            nameEl = "Airbnb Ωραιόκαστρο Forest Villa",
            nameEn = "Airbnb Oraiokastro Forest Villa",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.720450,
            lng = 22.921850,
            nameEl = "Airbnb Γαλήνη Ωραιοκάστρου Suite",
            nameEn = "Airbnb Galini Oraiokastro Suite",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.724250,
            lng = 22.920650,
            nameEl = "Hotel Hills Ωραιόκαστρο",
            nameEn = "Hotel Hills Oraiokastro",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.722850,
            lng = 22.921450,
            nameEl = "Σκλαβενίτης Ωραιόκαστρο",
            nameEn = "Sklavenitis Supermarket Oraiokastro",
            category = VectorPoiCategory.SHOP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.726250,
            lng = 22.918450,
            nameEl = "Μασούτης Ωραιόκαστρο",
            nameEn = "Masoutis Supermarket Oraiokastro",
            category = VectorPoiCategory.SHOP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.724850,
            lng = 22.919850,
            nameEl = "ΑΒ Βασιλόπουλος Ωραιόκαστρο",
            nameEn = "AB Vassilopoulos Oraiokastro",
            category = VectorPoiCategory.SHOP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.658450,
            lng = 22.993850,
            nameEl = "Ιερός Ναός Αγίων Αποστόλων Πεύκων",
            nameEn = "Holy Apostles Church Pefka",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.657850,
            lng = 22.994650,
            nameEl = "Ταβέρνα Τα Πεύκα",
            nameEn = "Taverna Ta Pefka",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.656950,
            lng = 22.992450,
            nameEl = "Καφέ Εστιατόριο Ρετζίκι",
            nameEn = "Retziki Cafe Restaurant",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.659250,
            lng = 22.995450,
            nameEl = "Airbnb Πεύκα Pine View Apartment",
            nameEn = "Airbnb Pefka Pine View Apartment",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.657250,
            lng = 22.991850,
            nameEl = "Μασούτης Πεύκα",
            nameEn = "Masoutis Supermarket Pefka",
            category = VectorPoiCategory.SHOP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.656250,
            lng = 22.989850,
            nameEl = "Σκλαβενίτης Πεύκα",
            nameEn = "Sklavenitis Supermarket Pefka",
            category = VectorPoiCategory.SHOP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.688450,
            lng = 22.953850,
            nameEl = "Ιερός Ναός Αγίου Γεωργίου Ευκαρπίας",
            nameEn = "St. George Church Efkarpia",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.687850,
            lng = 22.954650,
            nameEl = "Ταβέρνα Η Ευκαρπία",
            nameEn = "Taverna Efkarpia",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.689250,
            lng = 22.952850,
            nameEl = "Airbnb Ευκαρπία Modern Stay",
            nameEn = "Airbnb Efkarpia Modern Stay",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.686950,
            lng = 22.953250,
            nameEl = "Μασούτης Ευκαρπία",
            nameEn = "Masoutis Supermarket Efkarpia",
            category = VectorPoiCategory.SHOP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.682450,
            lng = 22.946850,
            nameEl = "TITAN Εργοστάσιο Ευκαρπίας",
            nameEn = "TITAN Cement Efkarpia Plant",
            category = VectorPoiCategory.OFFICE_COMPANY,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.692450,
            lng = 23.003850,
            nameEl = "Ιερός Ναός Αγίου Γεωργίου Φιλύρου",
            nameEn = "St. George Church Filiro",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.691850,
            lng = 23.004650,
            nameEl = "Ταβέρνα Πλατάνια Φίλυρο",
            nameEn = "Platania Taverna Filiro",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.693250,
            lng = 23.002850,
            nameEl = "Airbnb Φίλυρο Panoramic Villa",
            nameEn = "Airbnb Filiro Panoramic Villa",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.646850,
            lng = 23.023850,
            nameEl = "Ιερός Ναός Αγίου Γεωργίου Ασβεστοχωρίου",
            nameEn = "St. George Church Asvestochori",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.646250,
            lng = 23.024650,
            nameEl = "Ταβέρνα Το Ασβεστοχώρι",
            nameEn = "Taverna Asvestochori",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.750450,
            lng = 23.066850,
            nameEl = "Ιερός Μητροπολιτικός Ναός Αγίας Παρασκευής Λαγκαδά",
            nameEn = "Metropolitan Church of St. Paraskevi Lagkadas",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.742850,
            lng = 23.078450,
            nameEl = "Λουτρά Λαγκαδά",
            nameEn = "Lagkadas Thermal Baths",
            category = VectorPoiCategory.SPORTS_SWIMMING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.749850,
            lng = 23.065850,
            nameEl = "Ταβέρνα Ο Λαγκαδάς",
            nameEn = "Taverna Lagkadas",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 40.748850,
            lng = 23.067450,
            nameEl = "Airbnb Λαγκαδάς Thermal Stay",
            nameEn = "Airbnb Lagkadas Thermal Stay",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        // --- Athens (Αθήνα: Σύνταγμα, Πλάκα, Μοναστηράκι, Ψυρρή, Κολωνάκι, Κουκάκι, Εξάρχεια, Παγκράτι, Θησείο, Μαρούσι, Χαλάνδρι, Κηφισιά, Γλυφάδα, Καλλιθέα, Περιστέρι, Πειραιάς) ---
        PriorityGreekPoi(
            lat = 37.975540,
            lng = 23.734850,
            nameEl = "Πλατεία Συντάγματος",
            nameEn = "Syntagma Square",
            category = VectorPoiCategory.LANDMARK_TOURISM,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.971530,
            lng = 23.725750,
            nameEl = "Ακρόπολη Αθηνών",
            nameEn = "Acropolis of Athens",
            category = VectorPoiCategory.LANDMARK_TOURISM,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.968450,
            lng = 23.728520,
            nameEl = "Μουσείο Ακρόπολης",
            nameEn = "Acropolis Museum",
            category = VectorPoiCategory.LANDMARK_TOURISM,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.976350,
            lng = 23.725850,
            nameEl = "Πλατεία Μοναστηρακίου",
            nameEn = "Monastiraki Square",
            category = VectorPoiCategory.LANDMARK_TOURISM,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.975250,
            lng = 23.730150,
            nameEl = "Μητροπολιτικός Ναός Αθηνών (Ευαγγελισμός της Θεοτόκου)",
            nameEn = "Metropolitan Cathedral of Athens",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.976550,
            lng = 23.728650,
            nameEl = "Ιερός Ναός Παναγίας Καπνικαρέας",
            nameEn = "Church of Panagia Kapnikarea",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.979450,
            lng = 23.740850,
            nameEl = "Ιερός Ναός Αγίου Διονυσίου Αρεοπαγίτου",
            nameEn = "Church of St. Dionysius the Areopagite",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.976250,
            lng = 23.735450,
            nameEl = "Hotel Grande Bretagne",
            nameEn = "Hotel Grande Bretagne Athens",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.975950,
            lng = 23.735150,
            nameEl = "King George Hotel Athens",
            nameEn = "King George Hotel Athens",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.974850,
            lng = 23.731850,
            nameEl = "Electra Metropolis Athens",
            nameEn = "Electra Metropolis Athens",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.973150,
            lng = 23.732450,
            nameEl = "Electra Palace Athens",
            nameEn = "Electra Palace Athens Plaka",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.975150,
            lng = 23.731250,
            nameEl = "Ergon House Athens",
            nameEn = "Ergon House Athens",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.975650,
            lng = 23.728950,
            nameEl = "The Dolli at Acropolis",
            nameEn = "The Dolli at Acropolis",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.979850,
            lng = 23.725650,
            nameEl = "Τα Καραμανλίδικα του Φάνη",
            nameEn = "Ta Karamanlidika tou Fani",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.975450,
            lng = 23.722850,
            nameEl = "Kuzina",
            nameEn = "Kuzina Restaurant Thiseio",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.976450,
            lng = 23.726450,
            nameEl = "Μπαϊρακτάρης",
            nameEn = "Bairaktaris Taverna Monastiraki",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.976250,
            lng = 23.726150,
            nameEl = "Ο Θανάσης",
            nameEn = "O Thanasis Monastiraki",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.972650,
            lng = 23.727850,
            nameEl = "Γιασεμί",
            nameEn = "Yiasemi Cafe Bistro Plaka",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.974250,
            lng = 23.732850,
            nameEl = "Nolan",
            nameEn = "Nolan Restaurant Athens",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.974650,
            lng = 23.732550,
            nameEl = "Birdman",
            nameEn = "Birdman Athens",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.977650,
            lng = 23.724450,
            nameEl = "Little Kook",
            nameEn = "Little Kook Psyrri",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.976850,
            lng = 23.731650,
            nameEl = "Baba Au Rum",
            nameEn = "Baba Au Rum Athens",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.978850,
            lng = 23.729450,
            nameEl = "The Clumsies",
            nameEn = "The Clumsies Athens",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.972850,
            lng = 23.729650,
            nameEl = "Airbnb Πλάκα Acropolis View Suite",
            nameEn = "Airbnb Plaka Acropolis View Suite",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.976850,
            lng = 23.725450,
            nameEl = "Airbnb Μοναστηράκι Rooftop Loft",
            nameEn = "Airbnb Monastiraki Rooftop Loft",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.978450,
            lng = 23.724250,
            nameEl = "Airbnb Ψυρρή Industrial Studio",
            nameEn = "Airbnb Psyrri Industrial Studio",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.974450,
            lng = 23.734250,
            nameEl = "Airbnb Σύνταγμα Luxury Apartment",
            nameEn = "Airbnb Syntagma Luxury Apartment",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.973850,
            lng = 23.719850,
            nameEl = "Airbnb Θησείο Acropolis Terrace",
            nameEn = "Airbnb Thiseio Acropolis Terrace",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.977450,
            lng = 23.741250,
            nameEl = "Πλατεία Κολωνακίου",
            nameEn = "Kolonaki Square",
            category = VectorPoiCategory.LANDMARK_TOURISM,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.967250,
            lng = 23.744850,
            nameEl = "Σπονδή (Spondi)",
            nameEn = "Spondi Restaurant Pangrati",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.986450,
            lng = 23.731850,
            nameEl = "Pharaoh",
            nameEn = "Pharaoh Restaurant Exarcheia",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.964850,
            lng = 23.725450,
            nameEl = "Λόλα Κουκάκι",
            nameEn = "Lola Koukaki Restaurant",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.969850,
            lng = 23.745650,
            nameEl = "Μαύρος Γάτος Παγκράτι",
            nameEn = "Mavros Gatos Pangrati",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.980850,
            lng = 23.743850,
            nameEl = "St. George Lycabettus Hotel",
            nameEn = "St. George Lycabettus Hotel",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.965850,
            lng = 23.726850,
            nameEl = "Airbnb Κουκάκι Acropolis Apartment",
            nameEn = "Airbnb Koukaki Acropolis Apartment",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.978250,
            lng = 23.741850,
            nameEl = "Airbnb Κολωνάκι Designer Suite",
            nameEn = "Airbnb Kolonaki Designer Suite",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.968450,
            lng = 23.744250,
            nameEl = "Airbnb Παγκράτι Varnava Loft",
            nameEn = "Airbnb Pangrati Varnava Loft",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.985850,
            lng = 23.735850,
            nameEl = "Airbnb Εξάρχεια Kallidromiou Stay",
            nameEn = "Airbnb Exarcheia Kallidromiou Stay",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.977250,
            lng = 23.748450,
            nameEl = "Γενικό Νοσοκομείο Αθηνών Ευαγγελισμός",
            nameEn = "Evangelismos General Hospital",
            category = VectorPoiCategory.MEDICAL,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.978650,
            lng = 23.739850,
            nameEl = "Σκλαβενίτης Κολωνάκι",
            nameEn = "Sklavenitis Supermarket Kolonaki",
            category = VectorPoiCategory.SHOP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.964250,
            lng = 23.724850,
            nameEl = "ΑΒ Βασιλόπουλος Κουκάκι",
            nameEn = "AB Vassilopoulos Koukaki",
            category = VectorPoiCategory.SHOP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 38.044850,
            lng = 23.792850,
            nameEl = "The Mall Athens Μαρούσι",
            nameEn = "The Mall Athens Marousi",
            category = VectorPoiCategory.SHOP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 38.031850,
            lng = 23.791450,
            nameEl = "Golden Hall Μαρούσι",
            nameEn = "Golden Hall Marousi",
            category = VectorPoiCategory.SHOP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 38.046250,
            lng = 23.803850,
            nameEl = "Μέγαρο ΟΤΕ Μαρούσι",
            nameEn = "OTE Group Headquarters Marousi",
            category = VectorPoiCategory.OFFICE_COMPANY,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 38.040250,
            lng = 23.805450,
            nameEl = "Ιατρικό Κέντρο Αθηνών",
            nameEn = "Athens Medical Center Marousi",
            category = VectorPoiCategory.MEDICAL,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 38.021850,
            lng = 23.798850,
            nameEl = "Πλατεία Χαλανδρίου",
            nameEn = "Chalandri Central Square",
            category = VectorPoiCategory.LANDMARK_TOURISM,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 38.022450,
            lng = 23.799650,
            nameEl = "Airbnb Χαλάνδρι Center Suite",
            nameEn = "Airbnb Chalandri Center Suite",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 38.073450,
            lng = 23.812850,
            nameEl = "Artisanal Κηφισιά",
            nameEn = "Artisanal Restaurant Kifisia",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 38.076250,
            lng = 23.814850,
            nameEl = "Semiramis Hotel Κηφισιά",
            nameEn = "Semiramis Hotel Kifisia",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.939650,
            lng = 23.691850,
            nameEl = "Κέντρο Πολιτισμού Ίδρυμα Σταύρος Νιάρχος",
            nameEn = "Stavros Niarchos Foundation Cultural Center",
            category = VectorPoiCategory.CULTURE_CINEMA,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.946850,
            lng = 23.698850,
            nameEl = "Airbnb Καλλιθέα SNFCC Apartment",
            nameEn = "Airbnb Kallithea SNFCC Apartment",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.862850,
            lng = 23.753850,
            nameEl = "Αρκαδία Γλυφάδα",
            nameEn = "Arkadia Restaurant Glyfada",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.868450,
            lng = 23.742850,
            nameEl = "One&Only Aesthesis Γλυφάδα",
            nameEn = "One&Only Aesthesis Glyfada",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.864250,
            lng = 23.752450,
            nameEl = "Airbnb Γλυφάδα Riviera Suite",
            nameEn = "Airbnb Glyfada Riviera Suite",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 38.012850,
            lng = 23.688850,
            nameEl = "Πλατεία Μπουρναζίου Περιστέρι",
            nameEn = "Bournazi Square Peristeri",
            category = VectorPoiCategory.LANDMARK_TOURISM,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 38.011450,
            lng = 23.691850,
            nameEl = "Airbnb Περιστέρι Metro Studio",
            nameEn = "Airbnb Peristeri Metro Studio",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.937850,
            lng = 23.658450,
            nameEl = "Βαρούλκο Seaside Μικρολίμανο",
            nameEn = "Varoulko Seaside Mikrolimano Piraeus",
            category = VectorPoiCategory.FOOD_CAFE,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.943850,
            lng = 23.646850,
            nameEl = "Ιερός Ναός Αγίας Τριάδος Πειραιά",
            nameEn = "Holy Trinity Cathedral Piraeus",
            category = VectorPoiCategory.WORSHIP,
            priorityRank = -2
        ),
        PriorityGreekPoi(
            lat = 37.936850,
            lng = 23.649850,
            nameEl = "Airbnb Πειραιάς Marina Zeas Suite",
            nameEn = "Airbnb Piraeus Marina Zeas Suite",
            category = VectorPoiCategory.HOTEL_LODGING,
            priorityRank = -2
        )
    )

    private val isMapOpen = AtomicBoolean(false)
    val cellCache = ConcurrentHashMap<String, VectorMapCell>(16)
    private val inFlightOrQueued = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>(16))
    private val diskCheckedMissing = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>(64))
    private val requestQueue = ConcurrentLinkedDeque<VectorCellRequest>()
    private val workerRunning = AtomicBoolean(false)
    private var debounceJob: Job? = null
    @Volatile
    private var lastCenterLatIdx: Int = 0
    @Volatile
    private var lastCenterLngIdx: Int = 0

    // Reusable paths for main-thread Canvas drawing so 120 FPS rendering allocates zero Path objects
    internal val reusableMainPath = Path()
    internal val reusableShadowPath = Path()
    internal val reusableTeardropPath = Path()
    internal val reusableBinPath = Path()
    internal val reusableConiferPath = Path()

    private val vectorDispatcher = java.util.concurrent.ThreadPoolExecutor(
        1,
        1,
        2L,
        java.util.concurrent.TimeUnit.SECONDS,
        java.util.concurrent.LinkedBlockingQueue()
    ) { r ->
        Thread(r, "VectorMapWorker").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY - 1
        }
    }.apply {
        allowCoreThreadTimeOut(true)
    }.asCoroutineDispatcher()

    private val scope = CoroutineScope(SupervisorJob() + vectorDispatcher)

    private val sharedBoldTypeface: Typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

    private val sharedStreetHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = sharedBoldTypeface
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val sharedStreetFillPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = sharedBoldTypeface
        style = Paint.Style.FILL
    }
    private val sharedHnHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = sharedBoldTypeface
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val sharedHnFillPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = sharedBoldTypeface
        style = Paint.Style.FILL
    }
    private val sharedMediumTypeface: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    private val sharedPoiHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = sharedMediumTypeface
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val sharedPoiFillPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = sharedMediumTypeface
        style = Paint.Style.FILL
    }

    private fun cellIndex(coord: Double): Int = floor(coord / CELL_STEP_DEG).toInt()
    private fun cellKey(latIdx: Int, lngIdx: Int): String = "${latIdx}_${lngIdx}"

    fun onMapOpened() {
        isMapOpen.set(true)
    }

    fun onMapClosed() {
        isMapOpen.set(false)
        debounceJob?.cancel()
        debounceJob = null
        requestQueue.clear()
        inFlightOrQueued.clear()
        diskCheckedMissing.clear()
        cellCache.clear()
        reusableMainPath.reset()
        reusableShadowPath.reset()
        reusableTeardropPath.reset()
        reusableBinPath.reset()
        reusableConiferPath.reset()
    }

    fun trimMemory(keepAll: Boolean = false) {
        if (!keepAll) {
            onMapClosed()
        } else {
            debounceJob?.cancel()
            requestQueue.clear()
            inFlightOrQueued.clear()
            evictFarthestCellsIfNeeded(maxAllowed = 4)
        }
    }

    private fun evictFarthestCellsIfNeeded(maxAllowed: Int = MAX_CACHED_CELLS) {
        if (cellCache.size <= maxAllowed) return
        val cLat = lastCenterLatIdx
        val cLng = lastCenterLngIdx
        val entries = cellCache.keys.toList()
        val sortedByDistDesc = entries.sortedByDescending { k ->
            val sep = k.indexOf('_')
            if (sep <= 0) 999999 else {
                val la = k.substring(0, sep).toIntOrNull() ?: 0
                val ln = k.substring(sep + 1).toIntOrNull() ?: 0
                abs(la - cLat) + abs(ln - cLng)
            }
        }
        val toRemove = (cellCache.size - maxAllowed).coerceAtLeast(0)
        for (i in 0 until min(toRemove, sortedByDistDesc.size)) {
            cellCache.remove(sortedByDistDesc[i])
        }
    }

    fun prewarmSpots(
        cacheDir: File,
        spots: List<ParkingSpot>,
        userLat: Double?,
        userLng: Double?,
        onCellLoaded: () -> Unit
    ) {
        if (!isMapOpen.get()) return
        // Disk-only fast binary prewarm for at most 2 cells (never triggers network or JSON parsing on map open)
        val targetCoords = ArrayList<Pair<Double, Double>>(2)
        if (userLat != null && userLng != null) {
            targetCoords.add(userLat to userLng)
        }
        spots.maxByOrNull { it.timestamp }?.let { s ->
            targetCoords.add(s.latitude to s.longitude)
        }
        for ((lat, lng) in targetCoords) {
            val latIdx = cellIndex(lat)
            val lngIdx = cellIndex(lng)
            val binFile = File(cacheDir, "$VECTOR_BIN_CACHE_FOLDER/${cellKey(latIdx, lngIdx)}.bin")
            if (binFile.exists()) {
                enqueueCell(
                    cacheDir = cacheDir,
                    latIdx = latIdx,
                    lngIdx = lngIdx,
                    urgent = false,
                    diskOnly = true,
                    onCellLoaded = onCellLoaded
                )
            }
        }
    }

    fun scheduleViewport(
        cacheDir: File,
        centerLat: Double,
        centerLng: Double,
        zoom: Float,
        onCellLoaded: () -> Unit
    ) {
        if (!isMapOpen.get() || zoom < 13.2f) return
        val cLatIdx = cellIndex(centerLat)
        val cLngIdx = cellIndex(centerLng)
        lastCenterLatIdx = cLatIdx
        lastCenterLngIdx = cLngIdx

        // Immediately check binary disk cache or queue the exact center cell at the front
        enqueueCell(cacheDir, cLatIdx, cLngIdx, urgent = true, diskOnly = false, onCellLoaded = onCellLoaded)

        // Debounce neighboring cell prefetch by 180ms so rapid panning/zooming never floods Overpass
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(180)
            if (!isMapOpen.get()) return@launch
            val fracLat = (centerLat / CELL_STEP_DEG) - cLatIdx
            val fracLng = (centerLng / CELL_STEP_DEG) - cLngIdx
            val neighborLat = if (fracLat > 0.5) cLatIdx + 1 else cLatIdx - 1
            val neighborLng = if (fracLng > 0.5) cLngIdx + 1 else cLngIdx - 1
            enqueueCell(cacheDir, neighborLat, cLngIdx, urgent = false, diskOnly = false, onCellLoaded = onCellLoaded)
            enqueueCell(cacheDir, cLatIdx, neighborLng, urgent = false, diskOnly = false, onCellLoaded = onCellLoaded)
        }
    }

    private fun enqueueCell(
        cacheDir: File,
        latIdx: Int,
        lngIdx: Int,
        urgent: Boolean,
        diskOnly: Boolean,
        onCellLoaded: () -> Unit
    ) {
        if (!isMapOpen.get()) return
        val key = cellKey(latIdx, lngIdx)
        if (cellCache.containsKey(key) || !inFlightOrQueued.add(key)) return

        while (requestQueue.size > 8) {
            val dropped = requestQueue.pollLast() ?: break
            inFlightOrQueued.remove(dropped.key)
        }

        val req = VectorCellRequest(cacheDir, latIdx, lngIdx, key, diskOnly, onCellLoaded)
        if (urgent) {
            requestQueue.addFirst(req)
        } else {
            requestQueue.addLast(req)
        }
        ensureWorker()
    }

    private fun ensureWorker() {
        if (!isMapOpen.get()) {
            requestQueue.clear()
            inFlightOrQueued.clear()
            return
        }
        if (workerRunning.compareAndSet(false, true)) {
            scope.launch {
                try {
                    while (isMapOpen.get()) {
                        val req = requestQueue.pollFirst() ?: break
                        try {
                            processCellRequest(req)
                        } finally {
                            inFlightOrQueued.remove(req.key)
                        }
                    }
                } finally {
                    workerRunning.set(false)
                    if (isMapOpen.get() && requestQueue.isNotEmpty()) {
                        ensureWorker()
                    }
                }
            }
        }
    }

    private suspend fun processCellRequest(req: VectorCellRequest) {
        if (!isMapOpen.get()) return
        val key = req.key
        if (cellCache.containsKey(key)) return

        val binFile = File(req.cacheDir, "$VECTOR_BIN_CACHE_FOLDER/$key.bin")

        // 1. Check compact binary disk cache first (instant <0.5ms load with zero JSON allocations)
        if (!diskCheckedMissing.contains(key)) {
            try {
                if (binFile.exists() && binFile.length() > 16) {
                    val loaded = readCellBinary(key, binFile)
                    if (loaded != null) {
                        if (!isMapOpen.get()) return
                        evictFarthestCellsIfNeeded(MAX_CACHED_CELLS - 1)
                        cellCache[key] = loaded
                        req.onCellLoaded()
                        return
                    }
                }
            } catch (_: Exception) {
            }
            diskCheckedMissing.add(key)
        }

        if (req.diskOnly || !isMapOpen.get()) return

        // 2. Fetch and stream-parse directly from Overpass HTTP InputStream (zero full-string or JSONObject in RAM)
        val parsed = fetchAndStreamParseOverpassCell(key, req.latIdx, req.lngIdx)
        if (parsed != null && isMapOpen.get()) {
            evictFarthestCellsIfNeeded(MAX_CACHED_CELLS - 1)
            cellCache[key] = parsed
            req.onCellLoaded()
            try {
                binFile.parentFile?.mkdirs()
                writeCellBinary(parsed, binFile)
                diskCheckedMissing.remove(key)
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun fetchAndStreamParseOverpassCell(
        key: String,
        latIdx: Int,
        lngIdx: Int
    ): VectorMapCell? {
        val pad = 0.0005
        val south = latIdx * CELL_STEP_DEG - pad
        val north = (latIdx + 1) * CELL_STEP_DEG + pad
        val west = lngIdx * CELL_STEP_DEG - pad
        val east = (lngIdx + 1) * CELL_STEP_DEG + pad
        val bbox = String.format(java.util.Locale.US, "%.5f,%.5f,%.5f,%.5f", south, west, north, east)

        val query = "[out:json][timeout:10];(" +
            "way($bbox)[building];" +
            "way($bbox)[highway];" +
            "way($bbox)[leisure];" +
            "way($bbox)[landuse];" +
            "way($bbox)[natural];" +
            "way($bbox)[amenity];" +
            "way($bbox)[cuisine];" +
            "way($bbox)[tourism];" +
            "way($bbox)[accommodation];" +
            "way($bbox)[shop];" +
            "way($bbox)[office];" +
            "way($bbox)[craft];" +
            "way($bbox)[commercial];" +
            "way($bbox)[company];" +
            "way($bbox)[industrial];" +
            "way($bbox)[healthcare];" +
            "way($bbox)[name];" +
            "way($bbox)[\"addr:housename\"];" +
            "node($bbox)[name];" +
            "node($bbox)[\"addr:housename\"];" +
            "node($bbox)[amenity];" +
            "node($bbox)[cuisine];" +
            "node($bbox)[tourism];" +
            "node($bbox)[accommodation];" +
            "node($bbox)[shop];" +
            "node($bbox)[office];" +
            "node($bbox)[craft];" +
            "node($bbox)[commercial];" +
            "node($bbox)[company];" +
            "node($bbox)[industrial];" +
            "node($bbox)[healthcare];" +
            "node($bbox)[leisure];" +
            "node($bbox)[natural];" +
            "node($bbox)[\"addr:housenumber\"];" +
            "relation($bbox)[tourism];" +
            "relation($bbox)[accommodation];" +
            "relation($bbox)[amenity];" +
            "relation($bbox)[cuisine];" +
            "relation($bbox)[office];" +
            "relation($bbox)[company];" +
            "relation($bbox)[commercial];" +
            "relation($bbox)[craft];" +
            "relation($bbox)[shop];" +
            "relation($bbox)[building][name];" +
            ");out body geom qt;"

        val postBody = "data=" + URLEncoder.encode(query, "UTF-8")
        val endpoints = arrayOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.private.coffee/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter"
        )

        for (endpoint in endpoints) {
            val parsed = httpPostAndStreamParseOverpass(endpoint, postBody, key, latIdx, lngIdx, timeoutMs = 6200)
            if (parsed != null) return parsed
            delay(60)
        }
        return null
    }

    private fun httpPostAndStreamParseOverpass(
        urlStr: String,
        body: String,
        key: String,
        latIdx: Int,
        lngIdx: Int,
        timeoutMs: Int
    ): VectorMapCell? {
        var conn: HttpURLConnection? = null
        try {
            val bytes = body.toByteArray(Charsets.UTF_8)
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                setRequestProperty("User-Agent", "PixelParking/0.75 (Android; HighDef Vector Map)")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                setFixedLengthStreamingMode(bytes.size)
            }
            conn.outputStream.use { out ->
                out.write(bytes)
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.use { rawIn ->
                    JsonReader(InputStreamReader(BufferedInputStream(rawIn, 16384), Charsets.UTF_8)).use { reader ->
                        return parseOverpassStream(reader, key, latIdx, lngIdx)
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            conn?.disconnect()
        }
        return null
    }

    private fun parseOverpassStream(
        reader: JsonReader,
        key: String,
        latIdx: Int,
        lngIdx: Int
    ): VectorMapCell? {
        return try {
            var foundElements = false
            val areas = ArrayList<VectorAreaPoly>(48)
            val buildings = ArrayList<VectorBuildingPoly>(260)
            val roads = ArrayList<VectorRoadWay>(140)
            val pois = ArrayList<VectorPoiNode>(240)
            var microOrUnnamedCount = 0

            // Reusable coordinate scratch buffer so geometry parsing allocates zero intermediate lists
            var scratchLats = DoubleArray(256)
            var scratchLngs = DoubleArray(256)

            reader.beginObject()
            while (reader.hasNext()) {
                val rootProp = reader.nextName()
                if (rootProp == "elements" && reader.peek() == JsonToken.BEGIN_ARRAY) {
                    foundElements = true
                    reader.beginArray()
                    while (reader.hasNext()) {
                        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                            reader.skipValue()
                            continue
                        }
                        var type = ""
                        var nodeLat = Double.NaN
                        var nodeLng = Double.NaN
                        var boundsLat = Double.NaN
                        var boundsLng = Double.NaN
                        var geomCount = 0
                        var sumLat = 0.0
                        var sumLng = 0.0

                        var buildingTag = ""
                        var highwayTag = ""
                        var leisureTag = ""
                        var landuseTag = ""
                        var naturalTag = ""
                        var amenityTag = ""
                        var cuisineTag = ""
                        var foodTag = ""
                        var drinkTag = ""
                        var shopTag = ""
                        var healthcareTag = ""
                        var sportTag = ""
                        var tourismTag = ""
                        var historicTag = ""
                        var officeTag = ""
                        var craftTag = ""
                        var commercialTag = ""
                        var industrialTag = ""
                        var accommodationTag = ""
                        var airbnbTag = ""
                        var lodgingTag = ""
                        var rentalTag = ""
                        var roomsTag = ""
                        var bedsTag = ""
                        var starsTag = ""
                        var telecomTag = ""
                        var advertisingTag = ""
                        var tradeTag = ""
                        var wholesaleTag = ""
                        var diplomaticTag = ""
                        var clubTag = ""
                        var brandTag = ""
                        var operatorTag = ""
                        var hostTag = ""
                        var companyTag = ""
                        var ownerTag = ""
                        var branchTag = ""
                        var websiteTag = ""
                        var socialTag = ""
                        var descriptionTag = ""
                        var streetTag: String? = null
                        var nameTag: String? = null
                        var nameElTag: String? = null
                        var nameEnTag: String? = null
                        var houseNameTag: String? = null
                        var officialNameTag: String? = null
                        var shortNameTag: String? = null
                        var altNameTag: String? = null
                        var locNameTag: String? = null
                        var intNameTag: String? = null
                        var contactNameTag: String? = null
                        var houseNumTag: String? = null
                        var onewayTag = ""
                        var hasTags = false

                        reader.beginObject()
                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "type" -> if (reader.peek() == JsonToken.STRING) type = reader.nextString() else reader.skipValue()
                                "lat" -> if (reader.peek() == JsonToken.NUMBER) nodeLat = reader.nextDouble() else reader.skipValue()
                                "lon" -> if (reader.peek() == JsonToken.NUMBER) nodeLng = reader.nextDouble() else reader.skipValue()
                                "center" -> {
                                    if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                                        reader.beginObject()
                                        while (reader.hasNext()) {
                                            when (reader.nextName()) {
                                                "lat" -> if (reader.peek() == JsonToken.NUMBER) boundsLat = reader.nextDouble() else reader.skipValue()
                                                "lon" -> if (reader.peek() == JsonToken.NUMBER) boundsLng = reader.nextDouble() else reader.skipValue()
                                                else -> reader.skipValue()
                                            }
                                        }
                                        reader.endObject()
                                    } else {
                                        reader.skipValue()
                                    }
                                }
                                "bounds" -> {
                                    if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                                        var minLat = Double.NaN
                                        var minLon = Double.NaN
                                        var maxLat = Double.NaN
                                        var maxLon = Double.NaN
                                        reader.beginObject()
                                        while (reader.hasNext()) {
                                            when (reader.nextName()) {
                                                "minlat" -> if (reader.peek() == JsonToken.NUMBER) minLat = reader.nextDouble() else reader.skipValue()
                                                "minlon" -> if (reader.peek() == JsonToken.NUMBER) minLon = reader.nextDouble() else reader.skipValue()
                                                "maxlat" -> if (reader.peek() == JsonToken.NUMBER) maxLat = reader.nextDouble() else reader.skipValue()
                                                "maxlon" -> if (reader.peek() == JsonToken.NUMBER) maxLon = reader.nextDouble() else reader.skipValue()
                                                else -> reader.skipValue()
                                            }
                                        }
                                        reader.endObject()
                                        if (!minLat.isNaN() && !maxLat.isNaN() && !minLon.isNaN() && !maxLon.isNaN()) {
                                            boundsLat = (minLat + maxLat) * 0.5
                                            boundsLng = (minLon + maxLon) * 0.5
                                        }
                                    } else {
                                        reader.skipValue()
                                    }
                                }
                                "tags" -> {
                                    if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                                        hasTags = true
                                        reader.beginObject()
                                        while (reader.hasNext()) {
                                            val tk = reader.nextName()
                                            if (reader.peek() != JsonToken.STRING) {
                                                reader.skipValue()
                                                continue
                                            }
                                            val tv = reader.nextString().trim()
                                            if (tv.isEmpty()) continue
                                            when (tk) {
                                                "building" -> buildingTag = tv
                                                "highway" -> highwayTag = tv
                                                "leisure" -> leisureTag = tv
                                                "landuse" -> landuseTag = tv
                                                "natural" -> naturalTag = tv
                                                "amenity" -> amenityTag = tv
                                                "cuisine" -> cuisineTag = tv
                                                "food", "eat" -> if (foodTag.isEmpty()) foodTag = tv
                                                "drink", "bar", "wine", "coffee" -> if (drinkTag.isEmpty()) drinkTag = tv
                                                "shop" -> shopTag = tv
                                                "healthcare" -> healthcareTag = tv
                                                "sport" -> sportTag = tv
                                                "tourism" -> tourismTag = tv
                                                "historic" -> historicTag = tv
                                                "office" -> officeTag = tv
                                                "craft" -> craftTag = tv
                                                "commercial" -> commercialTag = tv
                                                "industrial" -> industrialTag = tv
                                                "accommodation" -> accommodationTag = tv
                                                "airbnb" -> airbnbTag = tv
                                                "lodging" -> lodgingTag = tv
                                                "rental" -> rentalTag = tv
                                                "rooms" -> roomsTag = tv
                                                "beds", "capacity:persons" -> if (bedsTag.isEmpty()) bedsTag = tv
                                                "stars" -> starsTag = tv
                                                "telecommunication" -> telecomTag = tv
                                                "advertising" -> advertisingTag = tv
                                                "trade" -> tradeTag = tv
                                                "wholesale" -> wholesaleTag = tv
                                                "diplomatic" -> diplomaticTag = tv
                                                "club" -> clubTag = tv
                                                "brand", "brand:el", "brand:en" -> if (brandTag.isEmpty()) brandTag = tv
                                                "operator", "operator:el", "operator:en" -> if (operatorTag.isEmpty()) operatorTag = tv
                                                "host" -> hostTag = tv
                                                "company" -> companyTag = tv
                                                "owner" -> ownerTag = tv
                                                "branch" -> branchTag = tv
                                                "website", "contact:website", "url" -> if (websiteTag.isEmpty()) websiteTag = tv
                                                "contact:facebook", "facebook", "contact:instagram", "instagram" -> if (socialTag.isEmpty()) socialTag = tv
                                                "description", "note" -> if (descriptionTag.isEmpty()) descriptionTag = tv
                                                "addr:street" -> streetTag = tv
                                                "name" -> nameTag = tv
                                                "name:el" -> nameElTag = tv
                                                "name:en" -> nameEnTag = tv
                                                "addr:housename" -> houseNameTag = tv
                                                "official_name", "official_name:el", "official_name:en" -> if (officialNameTag == null) officialNameTag = tv
                                                "short_name", "short_name:el", "short_name:en" -> if (shortNameTag == null) shortNameTag = tv
                                                "alt_name", "alt_name:el", "alt_name:en" -> if (altNameTag == null) altNameTag = tv
                                                "loc_name", "reg_name", "old_name" -> if (locNameTag == null) locNameTag = tv
                                                "int_name" -> intNameTag = tv
                                                "contact:name" -> contactNameTag = tv
                                                "addr:housenumber" -> houseNumTag = tv
                                                "oneway" -> onewayTag = tv
                                            }
                                        }
                                        reader.endObject()
                                    } else {
                                        reader.skipValue()
                                    }
                                }
                                "geometry" -> {
                                    if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                                        reader.beginArray()
                                        while (reader.hasNext()) {
                                            if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                                                var ptLat = Double.NaN
                                                var ptLon = Double.NaN
                                                reader.beginObject()
                                                while (reader.hasNext()) {
                                                    when (reader.nextName()) {
                                                        "lat" -> if (reader.peek() == JsonToken.NUMBER) ptLat = reader.nextDouble() else reader.skipValue()
                                                        "lon" -> if (reader.peek() == JsonToken.NUMBER) ptLon = reader.nextDouble() else reader.skipValue()
                                                        else -> reader.skipValue()
                                                    }
                                                }
                                                reader.endObject()
                                                if (!ptLat.isNaN() && !ptLon.isNaN() && geomCount < MAX_VERTICES_PER_GEOMETRY) {
                                                    if (geomCount >= scratchLats.size) {
                                                        val newCap = (scratchLats.size * 2).coerceAtMost(MAX_VERTICES_PER_GEOMETRY)
                                                        scratchLats = scratchLats.copyOf(newCap)
                                                        scratchLngs = scratchLngs.copyOf(newCap)
                                                    }
                                                    scratchLats[geomCount] = ptLat
                                                    scratchLngs[geomCount] = ptLon
                                                    sumLat += ptLat
                                                    sumLng += ptLon
                                                    geomCount++
                                                }
                                            } else {
                                                reader.skipValue()
                                            }
                                        }
                                        reader.endArray()
                                    } else {
                                        reader.skipValue()
                                    }
                                }
                                "members" -> {
                                    if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                                        reader.beginArray()
                                        while (reader.hasNext()) {
                                            if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                                                reader.beginObject()
                                                while (reader.hasNext()) {
                                                    when (reader.nextName()) {
                                                        "lat" -> if (reader.peek() == JsonToken.NUMBER) {
                                                            val mLat = reader.nextDouble()
                                                            if (boundsLat.isNaN()) boundsLat = mLat
                                                        } else reader.skipValue()
                                                        "lon" -> if (reader.peek() == JsonToken.NUMBER) {
                                                            val mLon = reader.nextDouble()
                                                            if (boundsLng.isNaN()) boundsLng = mLon
                                                        } else reader.skipValue()
                                                        "geometry" -> {
                                                            if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                                                                reader.beginArray()
                                                                while (reader.hasNext()) {
                                                                    if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                                                                        var mPtLat = Double.NaN
                                                                        var mPtLon = Double.NaN
                                                                        reader.beginObject()
                                                                        while (reader.hasNext()) {
                                                                            when (reader.nextName()) {
                                                                                "lat" -> if (reader.peek() == JsonToken.NUMBER) mPtLat = reader.nextDouble() else reader.skipValue()
                                                                                "lon" -> if (reader.peek() == JsonToken.NUMBER) mPtLon = reader.nextDouble() else reader.skipValue()
                                                                                else -> reader.skipValue()
                                                                            }
                                                                        }
                                                                        reader.endObject()
                                                                        if (!mPtLat.isNaN() && !mPtLon.isNaN()) {
                                                                            sumLat += mPtLat
                                                                            sumLng += mPtLon
                                                                            geomCount++
                                                                        }
                                                                    } else {
                                                                        reader.skipValue()
                                                                    }
                                                                }
                                                                reader.endArray()
                                                            } else {
                                                                reader.skipValue()
                                                            }
                                                        }
                                                        else -> reader.skipValue()
                                                    }
                                                }
                                                reader.endObject()
                                            } else {
                                                reader.skipValue()
                                            }
                                        }
                                        reader.endArray()
                                    } else {
                                        reader.skipValue()
                                    }
                                }
                                else -> reader.skipValue()
                            }
                        }
                        reader.endObject()

                        val explicitName = nameTag ?: nameElTag ?: nameEnTag
                            ?: houseNameTag
                            ?: officialNameTag
                            ?: shortNameTag
                            ?: altNameTag
                            ?: intNameTag
                            ?: locNameTag
                            ?: contactNameTag
                            ?: brandTag.takeIf { it.isNotBlank() }
                            ?: operatorTag.takeIf { it.isNotBlank() }
                            ?: companyTag.takeIf { it.isNotBlank() }
                            ?: hostTag.takeIf { it.isNotBlank() }
                            ?: ownerTag.takeIf { it.isNotBlank() }
                            ?: branchTag.takeIf { it.isNotBlank() }

                        val cat = if (hasTags) {
                            classifyPoiCategoryTags(
                                amenity = amenityTag,
                                cuisine = cuisineTag,
                                food = foodTag,
                                drink = drinkTag,
                                shop = shopTag,
                                healthcare = healthcareTag,
                                leisure = leisureTag,
                                sport = sportTag,
                                natural = naturalTag,
                                tourism = tourismTag,
                                historic = historicTag,
                                building = buildingTag,
                                office = officeTag,
                                craft = craftTag,
                                commercial = commercialTag,
                                industrial = industrialTag,
                                accommodation = accommodationTag,
                                airbnb = airbnbTag,
                                lodging = lodgingTag,
                                rental = rentalTag,
                                rooms = roomsTag,
                                beds = bedsTag,
                                stars = starsTag,
                                telecom = telecomTag,
                                advertising = advertisingTag,
                                trade = tradeTag,
                                wholesale = wholesaleTag,
                                diplomatic = diplomaticTag,
                                club = clubTag,
                                website = websiteTag,
                                social = socialTag,
                                description = descriptionTag,
                                name = explicitName ?: ""
                            )
                        } else null

                        val resolvedName = explicitName ?: synthesizeFallbackPoiName(
                            category = cat,
                            amenity = amenityTag,
                            cuisine = cuisineTag,
                            tourism = tourismTag,
                            accommodation = accommodationTag,
                            airbnb = airbnbTag,
                            lodging = lodgingTag,
                            office = officeTag,
                            craft = craftTag,
                            commercial = commercialTag,
                            industrial = industrialTag,
                            telecom = telecomTag,
                            advertising = advertisingTag,
                            trade = tradeTag,
                            wholesale = wholesaleTag,
                            diplomatic = diplomaticTag,
                            website = websiteTag,
                            social = socialTag,
                            street = streetTag,
                            houseNumber = houseNumTag
                        )

                        if (type == "way" && geomCount >= 2) {
                            val cLat = sumLat / geomCount
                            val cLng = sumLng / geomCount

                            if (buildingTag.isNotEmpty() && geomCount >= 3) {
                                if (buildings.size < MAX_BUILDINGS_PER_CELL) {
                                    buildings.add(
                                        VectorBuildingPoly(
                                            lats = scratchLats.copyOf(geomCount),
                                            lngs = scratchLngs.copyOf(geomCount),
                                            centerLat = cLat,
                                            centerLng = cLng,
                                            houseNumber = houseNumTag,
                                            name = resolvedName,
                                            poiCategory = if (resolvedName != null) (cat ?: VectorPoiCategory.GENERIC) else cat
                                        )
                                    )
                                } else if (resolvedName != null && pois.size < MAX_POIS_PER_CELL) {
                                    pois.add(
                                        VectorPoiNode(
                                            lat = cLat,
                                            lng = cLng,
                                            name = resolvedName,
                                            houseNumber = houseNumTag,
                                            category = cat ?: VectorPoiCategory.GENERIC
                                        )
                                    )
                                }
                            } else if (highwayTag.isNotEmpty()) {
                                if (roads.size < MAX_ROADS_PER_CELL) {
                                    val roadClass = when (highwayTag) {
                                        "motorway", "motorway_link", "trunk", "trunk_link", "primary", "primary_link" ->
                                            VectorRoadClass.MOTORWAY_PRIMARY
                                        "secondary", "secondary_link" ->
                                            VectorRoadClass.SECONDARY
                                        "tertiary", "tertiary_link" ->
                                            VectorRoadClass.TERTIARY
                                        "residential", "unclassified", "living_street", "road" ->
                                            VectorRoadClass.RESIDENTIAL
                                        "pedestrian" ->
                                            VectorRoadClass.PEDESTRIAN
                                        "service", "track" ->
                                            VectorRoadClass.SERVICE
                                        "cycleway" ->
                                            VectorRoadClass.CYCLEWAY
                                        else ->
                                            VectorRoadClass.FOOTPATH
                                    }
                                    val onewayDir = when (onewayTag) {
                                        "yes", "1", "true" -> 1
                                        "-1", "reverse" -> -1
                                        else -> 0
                                    }
                                    roads.add(
                                        VectorRoadWay(
                                            lats = scratchLats.copyOf(geomCount),
                                            lngs = scratchLngs.copyOf(geomCount),
                                            roadClass = roadClass,
                                            name = nameTag,
                                            nameEl = nameElTag,
                                            nameEn = nameEnTag,
                                            onewayDir = onewayDir
                                        )
                                    )
                                }
                            } else if (geomCount >= 3 && areas.size < MAX_AREAS_PER_CELL &&
                                (leisureTag.isNotEmpty() || landuseTag.isNotEmpty() || naturalTag.isNotEmpty() ||
                                 amenityTag.isNotEmpty() || cuisineTag.isNotEmpty() || shopTag.isNotEmpty() ||
                                 officeTag.isNotEmpty() || craftTag.isNotEmpty() || tourismTag.isNotEmpty() ||
                                 accommodationTag.isNotEmpty() || commercialTag.isNotEmpty() ||
                                 industrialTag.isNotEmpty() || companyTag.isNotEmpty() || healthcareTag.isNotEmpty() ||
                                 resolvedName != null)
                            ) {
                                val isForest = landuseTag == "forest" || naturalTag == "wood"
                                val isWater = naturalTag == "water" || leisureTag == "swimming_pool"
                                areas.add(
                                    VectorAreaPoly(
                                        lats = scratchLats.copyOf(geomCount),
                                        lngs = scratchLngs.copyOf(geomCount),
                                        centerLat = cLat,
                                        centerLng = cLng,
                                        isForestOrWood = isForest,
                                        isWaterOrPool = isWater,
                                        name = resolvedName,
                                        poiCategory = if (resolvedName != null) (cat ?: VectorPoiCategory.GENERIC) else cat
                                    )
                                )
                            } else if (resolvedName != null && pois.size < MAX_POIS_PER_CELL) {
                                pois.add(
                                    VectorPoiNode(
                                        lat = cLat,
                                        lng = cLng,
                                        name = resolvedName,
                                        houseNumber = houseNumTag,
                                        category = cat ?: VectorPoiCategory.GENERIC
                                    )
                                )
                            }
                        } else if (type == "node" && hasTags && !nodeLat.isNaN() && !nodeLng.isNaN()) {
                            val isMicroOrUnnamed = resolvedName == null ||
                                cat == VectorPoiCategory.BENCH ||
                                cat == VectorPoiCategory.WASTE_BASKET ||
                                cat == VectorPoiCategory.TREE
                            val canAddNode = if (isMicroOrUnnamed) {
                                microOrUnnamedCount < MAX_MICRO_OR_UNNAMED_POIS_PER_CELL && pois.size < MAX_POIS_PER_CELL
                            } else {
                                pois.size < MAX_POIS_PER_CELL
                            }
                            if ((cat != null || resolvedName != null || houseNumTag != null) && canAddNode) {
                                if (isMicroOrUnnamed) microOrUnnamedCount++
                                pois.add(
                                    VectorPoiNode(
                                        lat = nodeLat,
                                        lng = nodeLng,
                                        name = resolvedName,
                                        houseNumber = houseNumTag,
                                        category = cat ?: VectorPoiCategory.GENERIC
                                    )
                                )
                            }
                        } else if (type == "relation" && hasTags) {
                            val relLat = if (!boundsLat.isNaN()) boundsLat else if (geomCount > 0) sumLat / geomCount else Double.NaN
                            val relLng = if (!boundsLng.isNaN()) boundsLng else if (geomCount > 0) sumLng / geomCount else Double.NaN
                            if (!relLat.isNaN() && !relLng.isNaN() &&
                                (resolvedName != null || cat != null) &&
                                pois.size < MAX_POIS_PER_CELL
                            ) {
                                pois.add(
                                    VectorPoiNode(
                                        lat = relLat,
                                        lng = relLng,
                                        name = resolvedName,
                                        houseNumber = houseNumTag,
                                        category = cat ?: VectorPoiCategory.GENERIC
                                    )
                                )
                            }
                        }
                    }
                    reader.endArray()
                } else {
                    reader.skipValue()
                }
            }
            reader.endObject()

            if (!foundElements) return null

            roads.sortBy { road ->
                when (road.roadClass) {
                    VectorRoadClass.FOOTPATH -> 0
                    VectorRoadClass.CYCLEWAY -> 1
                    VectorRoadClass.SERVICE -> 2
                    VectorRoadClass.PEDESTRIAN -> 3
                    VectorRoadClass.RESIDENTIAL -> 4
                    VectorRoadClass.TERTIARY -> 5
                    VectorRoadClass.SECONDARY -> 6
                    VectorRoadClass.MOTORWAY_PRIMARY -> 7
                }
            }

            VectorMapCell(
                key = key,
                south = latIdx * CELL_STEP_DEG,
                west = lngIdx * CELL_STEP_DEG,
                north = (latIdx + 1) * CELL_STEP_DEG,
                east = (lngIdx + 1) * CELL_STEP_DEG,
                areas = areas,
                buildings = buildings,
                roads = roads,
                pois = pois
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun extractCleanWebsiteBrand(website: String): String? {
        if (website.isBlank()) return null
        val lower = website.lowercase(java.util.Locale.ROOT)
        if (lower.contains("airbnb.")) return "Airbnb"
        val stripped = website
            .replace(Regex("^https?://", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^www\\.", RegexOption.IGNORE_CASE), "")
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore(':')
        val hostPart = stripped.substringBefore('.')
        if (hostPart.length !in 2..24) return null
        if (hostPart.lowercase(java.util.Locale.ROOT) in listOf("facebook", "instagram", "booking", "google", "linktr", "sites", "wixsite", "wordpress")) {
            return null
        }
        return hostPart.split('-', '_')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }
            }
            .takeIf { it.isNotBlank() }
    }

    private fun extractSocialHandleBrand(social: String): String? {
        if (social.isBlank()) return null
        val cleaned = social
            .replace(Regex("^https?://", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^(www\\.|m\\.)?(facebook|instagram)\\.com/", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^@", RegexOption.IGNORE_CASE), "")
            .substringBefore('/')
            .substringBefore('?')
            .trim()
        if (cleaned.length !in 3..28 || cleaned.equals("pages", ignoreCase = true) || cleaned.equals("profile.php", ignoreCase = true)) {
            return null
        }
        return cleaned.split('.', '-', '_')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }
            }
            .takeIf { it.isNotBlank() }
    }

    private fun humanizeOsmTypeTag(rawTag: String): String? {
        val cleaned = rawTag.trim()
        if (cleaned.isEmpty() || cleaned == "yes" || cleaned == "true" || cleaned == "1") return null
        return cleaned.split('_', ';', ',')
            .firstOrNull { it.isNotBlank() }
            ?.replace('-', ' ')
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }
    }

    private fun synthesizeFallbackPoiName(
        category: VectorPoiCategory?,
        amenity: String = "",
        cuisine: String = "",
        tourism: String,
        accommodation: String,
        airbnb: String,
        lodging: String,
        office: String,
        craft: String,
        commercial: String,
        industrial: String,
        telecom: String,
        advertising: String,
        trade: String,
        wholesale: String,
        diplomatic: String,
        website: String,
        social: String = "",
        street: String?,
        houseNumber: String?
    ): String? {
        if (category != VectorPoiCategory.HOTEL_LODGING &&
            category != VectorPoiCategory.FOOD_CAFE &&
            category != VectorPoiCategory.OFFICE_COMPANY
        ) {
            return null
        }
        extractCleanWebsiteBrand(website)?.let { return it }
        extractSocialHandleBrand(social)?.let { return it }

        val addrSuffix = when {
            !street.isNullOrBlank() && !houseNumber.isNullOrBlank() -> " (${street.trim()} ${houseNumber.trim()})"
            !houseNumber.isNullOrBlank() -> " #${houseNumber.trim()}"
            !street.isNullOrBlank() -> " (${street.trim()})"
            else -> ""
        }

        if (category == VectorPoiCategory.HOTEL_LODGING) {
            val baseLabel = when {
                airbnb.isNotEmpty() || website.contains("airbnb", ignoreCase = true) -> "Airbnb"
                tourism == "apartment" || tourism == "apartments" || tourism == "serviced_apartment" ||
                    accommodation == "apartment" || accommodation == "flat" -> "Airbnb / Apartment"
                tourism == "vacation_rental" || accommodation == "vacation_rental" -> "Airbnb Rental"
                tourism == "guest_house" || tourism == "bed_and_breakfast" || accommodation == "bed_and_breakfast" -> "Ξενώνας / Guest House"
                tourism == "chalet" || accommodation == "chalet" || tourism == "villa" || accommodation == "villa" -> "Villa / Αρχοντικό"
                else -> humanizeOsmTypeTag(tourism) ?: humanizeOsmTypeTag(accommodation) ?: humanizeOsmTypeTag(lodging) ?: "Airbnb"
            }
            return "$baseLabel$addrSuffix"
        }

        if (category == VectorPoiCategory.FOOD_CAFE) {
            val cuisineLabel = humanizeOsmTypeTag(cuisine)
            val baseFoodLabel = when {
                cuisine.contains("greek", ignoreCase = true) || cuisine.contains("taverna", ignoreCase = true) ||
                    amenity == "taverna" || amenity == "tavern" -> "Ταβέρνα / Εστιατόριο"
                cuisine.contains("grill", ignoreCase = true) || cuisine.contains("souvlaki", ignoreCase = true) ||
                    cuisine.contains("gyros", ignoreCase = true) -> "Ψησταριά / Grill"
                cuisine.contains("pizza", ignoreCase = true) || cuisine.contains("italian", ignoreCase = true) -> "Πιτσαρία / Restaurant"
                cuisine.contains("seafood", ignoreCase = true) || cuisine.contains("fish", ignoreCase = true) -> "Ψαροταβέρνα / Seafood"
                amenity == "cafe" || amenity == "coffee_shop" || cuisine.contains("coffee", ignoreCase = true) -> "Καφέ / Cafe"
                amenity == "bar" || amenity == "pub" || amenity == "wine_bar" || amenity == "biergarten" -> "Bar & Wine"
                amenity == "bakery" || amenity == "pastry" -> "Φούρνος / Αρτοποιείο"
                cuisineLabel != null -> "$cuisineLabel Restaurant"
                else -> humanizeOsmTypeTag(amenity) ?: "Εστιατόριο / Restaurant"
            }
            return "$baseFoodLabel$addrSuffix"
        }

        val officeType = humanizeOsmTypeTag(office)
            ?: humanizeOsmTypeTag(craft)
            ?: humanizeOsmTypeTag(commercial)
            ?: humanizeOsmTypeTag(industrial)
            ?: humanizeOsmTypeTag(telecom)
            ?: humanizeOsmTypeTag(advertising)
            ?: humanizeOsmTypeTag(trade)
            ?: humanizeOsmTypeTag(wholesale)
            ?: humanizeOsmTypeTag(diplomatic)

        val companyBase = when {
            officeType == null -> "Company"
            officeType.equals("Company", ignoreCase = true) || officeType.equals("Office", ignoreCase = true) -> officeType
            else -> "$officeType Office"
        }
        return "$companyBase$addrSuffix"
    }

    private fun classifyPoiCategoryTags(
        amenity: String,
        cuisine: String = "",
        food: String = "",
        drink: String = "",
        shop: String,
        healthcare: String,
        leisure: String,
        sport: String,
        natural: String,
        tourism: String,
        historic: String,
        building: String,
        office: String = "",
        craft: String = "",
        commercial: String = "",
        industrial: String = "",
        accommodation: String = "",
        airbnb: String = "",
        lodging: String = "",
        rental: String = "",
        rooms: String = "",
        beds: String = "",
        stars: String = "",
        telecom: String = "",
        advertising: String = "",
        trade: String = "",
        wholesale: String = "",
        diplomatic: String = "",
        club: String = "",
        website: String = "",
        social: String = "",
        description: String = "",
        name: String = ""
    ): VectorPoiCategory? {
        val lowerName = name.lowercase(java.util.Locale.ROOT)
        val lowerDesc = description.lowercase(java.util.Locale.ROOT)
        val lowerWeb = website.lowercase(java.util.Locale.ROOT)
        val lowerSocial = social.lowercase(java.util.Locale.ROOT)

        val isAirbnbOrLodging = airbnb.isNotEmpty() ||
            lodging.isNotEmpty() ||
            rental in listOf("apartment", "vacation", "short_term", "holiday", "house", "villa", "airbnb", "yes") ||
            rooms.isNotEmpty() ||
            beds.isNotEmpty() ||
            stars.isNotEmpty() ||
            tourism in listOf(
                "hotel", "guest_house", "hostel", "motel", "resort", "apartment", "apartments",
                "chalet", "bed_and_breakfast", "vacation_rental", "room", "rooms", "serviced_apartment",
                "alpine_hut", "wilderness_hut", "homestay", "villa", "suites", "studio", "studios", "lodging",
                "country_house", "holiday_flat", "aparthotel", "cottage", "bungalow"
            ) ||
            accommodation in listOf(
                "apartment", "apartments", "flat", "bed_and_breakfast", "chalet", "hotel", "room", "rooms",
                "vacation_rental", "guest_house", "homestay", "villa", "studio", "suites", "yes",
                "holiday_home", "archontiko", "mansion", "cottage", "bungalow"
            ) ||
            building in listOf("hotel", "apartments") && (lowerName.isNotEmpty() || tourism.isNotEmpty() || accommodation.isNotEmpty()) ||
            building == "hotel" ||
            lowerWeb.contains("airbnb.") ||
            lowerWeb.contains("booking.com") ||
            lowerWeb.contains("vrbo.com") ||
            lowerSocial.contains("airbnb") ||
            lowerDesc.contains("airbnb") ||
            lowerDesc.contains("αρχοντικό") ||
            lowerDesc.contains("ξενώνας") ||
            lowerName.contains("airbnb") ||
            lowerName.contains("αρχοντικό") ||
            lowerName.contains("αρχοντικο") ||
            lowerName.contains("archontiko") ||
            lowerName.contains("arxontiko") ||
            lowerName.contains("mansion") ||
            lowerName.contains("melivista") ||
            lowerName.contains("apartment") ||
            lowerName.contains("apartments") ||
            lowerName.contains("studio") ||
            lowerName.contains("studios") ||
            lowerName.contains("villa") ||
            lowerName.contains("villas") ||
            lowerName.contains("suite") ||
            lowerName.contains("suites") ||
            lowerName.contains("rooms") ||
            lowerName.contains("guest house") ||
            lowerName.contains("guesthouse") ||
            lowerName.contains("homestay") ||
            lowerName.contains("holiday home") ||
            lowerName.contains("vacation") ||
            lowerName.contains("b&b") ||
            lowerName.contains("bnb") ||
            lowerName.contains("hostel") ||
            lowerName.contains("hotel") ||
            lowerName.contains("resort") ||
            lowerName.contains("maisonette") ||
            lowerName.contains("penthouse") ||
            lowerName.contains("cottage") ||
            lowerName.contains("chalet") ||
            lowerName.contains("luxury stay") ||
            lowerName.contains("short stay") ||
            lowerName.contains("διαμέρισμα") ||
            lowerName.contains("διαμερίσματα") ||
            lowerName.contains("στούντιο") ||
            lowerName.contains("βίλα") ||
            lowerName.contains("βίλες") ||
            lowerName.contains("σουίτα") ||
            lowerName.contains("σουίτες") ||
            lowerName.contains("δωμάτια") ||
            lowerName.contains("ενοικιαζόμενα") ||
            lowerName.contains("ξενώνας") ||
            lowerName.contains("ξενωνας") ||
            lowerName.contains("ξενώνες") ||
            lowerName.contains("κατάλυμα") ||
            lowerName.contains("καταλύματα") ||
            lowerName.contains("εξοχικό") ||
            lowerName.contains("ξενοδοχείο")

        val isFoodOrRestaurant = amenity in listOf(
                "cafe", "restaurant", "fast_food", "bar", "pub", "bakery", "ice_cream",
                "biergarten", "food_court", "bistro", "taverna", "tavern", "wine_bar",
                "coffee_shop", "hookah_lounge", "juice_bar", "pastry", "confectionery", "canteen"
            ) ||
            cuisine.isNotEmpty() ||
            food.isNotEmpty() ||
            drink.isNotEmpty() ||
            shop in listOf(
                "bakery", "pastry", "confectionery", "coffee", "tea", "wine",
                "deli", "cheese", "ice_cream", "chocolate", "pasta"
            ) ||
            craft in listOf("winery", "brewery", "distillery", "bakery", "confectionery", "caterer") ||
            lowerName.contains("la gantem") ||
            lowerName.contains("lagantem") ||
            lowerName.contains("ταβέρνα") ||
            lowerName.contains("ταβερνα") ||
            lowerName.contains("εστιατόριο") ||
            lowerName.contains("εστιατοριο") ||
            lowerName.contains("μεζεδοπωλείο") ||
            lowerName.contains("μεζεδοπωλειο") ||
            lowerName.contains("τσιπουράδικο") ||
            lowerName.contains("τσιπουραδικο") ||
            lowerName.contains("ουζερί") ||
            lowerName.contains("ουζερι") ||
            lowerName.contains("ψησταριά") ||
            lowerName.contains("ψησταρια") ||
            lowerName.contains("σουβλατζίδικο") ||
            lowerName.contains("οβελιστήριο") ||
            lowerName.contains("μαγειρείο") ||
            lowerName.contains("πιτσαρία") ||
            lowerName.contains("καφέ") ||
            lowerName.contains("καφετέρια") ||
            lowerName.contains("καφενείο") ||
            lowerName.contains("καφενειο") ||
            lowerName.contains("μπαρ") ||
            lowerName.contains("ζαχαροπλαστείο") ||
            lowerName.contains("φούρνος") ||
            lowerName.contains("αρτοποιείο") ||
            lowerName.contains("κρεπερί") ||
            lowerName.contains("μπουγάτσα") ||
            lowerName.contains("αναψυκτήριο") ||
            lowerName.contains("taverna") ||
            lowerName.contains("tavern") ||
            lowerName.contains("restaurant") ||
            lowerName.contains("resto") ||
            lowerName.contains("bistro") ||
            lowerName.contains("grill") ||
            lowerName.contains("steakhouse") ||
            lowerName.contains("pizzeria") ||
            lowerName.contains("pizza") ||
            lowerName.contains("burger") ||
            lowerName.contains("souvlaki") ||
            lowerName.contains("gyros") ||
            lowerName.contains("meze") ||
            lowerName.contains("ouzeri") ||
            lowerName.contains("tsipouradiko") ||
            lowerName.contains("cafe") ||
            lowerName.contains("coffee") ||
            lowerName.contains("espresso") ||
            lowerName.contains("wine bar") ||
            lowerName.contains("cocktail") ||
            lowerName.contains("lounge bar") ||
            lowerName.contains("gastro") ||
            lowerName.contains("bakery") ||
            lowerName.contains("patisserie") ||
            lowerName.contains("gelato")

        val isCompanyOrOffice = office.isNotEmpty() ||
            craft.isNotEmpty() ||
            commercial.isNotEmpty() ||
            industrial.isNotEmpty() ||
            telecom.isNotEmpty() ||
            advertising.isNotEmpty() ||
            trade.isNotEmpty() ||
            wholesale.isNotEmpty() ||
            diplomatic.isNotEmpty() ||
            club == "company" ||
            amenity in listOf("company", "coworking_space", "office", "business_centre", "research_institute", "conference_centre") ||
            building in listOf("office", "commercial", "industrial", "warehouse", "manufacture") ||
            lowerName.contains("α.ε.") ||
            lowerName.contains("ε.π.ε.") ||
            lowerName.contains("ι.κ.ε.") ||
            lowerName.contains("ο.ε.") ||
            lowerName.contains("εταιρεία") ||
            lowerName.contains("γραφείο") ||
            lowerName.contains("λογιστικό") ||
            lowerName.contains("δικηγορικό") ||
            lowerName.contains("τεχνικό") ||
            lowerName.contains("κατασκευαστική") ||
            lowerName.contains("συμβουλευτική") ||
            lowerName.contains("πληροφορική") ||
            lowerName.contains("software") ||
            lowerName.contains("technologies") ||
            lowerName.contains("technology") ||
            lowerName.contains("solutions") ||
            lowerName.contains("systems") ||
            lowerName.contains("consulting") ||
            lowerName.contains("agency") ||
            lowerName.contains("group") ||
            lowerName.contains("corp") ||
            lowerName.contains("inc.") ||
            lowerName.contains("ltd") ||
            lowerName.contains("llc") ||
            lowerName.contains("gmbh") ||
            lowerName.contains("company") ||
            lowerName.contains("enterprises") ||
            lowerName.contains("logistics") ||
            lowerName.contains("engineering") ||
            lowerName.contains("architects") ||
            lowerName.contains("accounting") ||
            lowerName.contains("insurance") ||
            lowerName.contains("real estate")

        return when {
            amenity == "pharmacy" || healthcare == "pharmacy" -> VectorPoiCategory.PHARMACY
            amenity == "hospital" || amenity == "clinic" || amenity == "doctors" || amenity == "dentist" || healthcare.isNotEmpty() ->
                VectorPoiCategory.MEDICAL
            amenity == "place_of_worship" || building == "church" || building == "chapel" || building == "cathedral" || building == "monastery" ->
                VectorPoiCategory.WORSHIP
            isAirbnbOrLodging ->
                VectorPoiCategory.HOTEL_LODGING
            isFoodOrRestaurant ->
                VectorPoiCategory.FOOD_CAFE
            isCompanyOrOffice ->
                VectorPoiCategory.OFFICE_COMPANY
            amenity == "cinema" || amenity == "theatre" || amenity == "arts_centre" || amenity == "nightclub" ->
                VectorPoiCategory.CULTURE_CINEMA
            tourism == "attraction" || tourism == "viewpoint" || tourism == "museum" || tourism == "artwork" || tourism == "gallery" || tourism == "theme_park" ||
                historic.isNotEmpty() || leisure == "Square" || leisure == "square" ->
                VectorPoiCategory.LANDMARK_TOURISM
            leisure == "sports_centre" || leisure == "swimming_pool" || leisure == "Water_park" || leisure == "water_park" || leisure == "stadium" || leisure == "pitch" ||
                leisure == "fitness_centre" || leisure == "park" || leisure == "nature_reserve" || leisure == "garden" || sport.isNotEmpty() ->
                VectorPoiCategory.SPORTS_SWIMMING
            amenity == "bench" -> VectorPoiCategory.BENCH
            amenity == "waste_basket" || amenity == "recycling" -> VectorPoiCategory.WASTE_BASKET
            natural == "tree" -> VectorPoiCategory.TREE
            amenity == "parking" || amenity == "parking_entrance" || amenity == "fuel" || amenity == "charging_station" ->
                VectorPoiCategory.PARKING
            amenity == "bank" || amenity == "atm" -> VectorPoiCategory.FINANCE
            shop.isNotEmpty() -> VectorPoiCategory.SHOP
            tourism.isNotEmpty() || amenity.isNotEmpty() || leisure.isNotEmpty() -> VectorPoiCategory.GENERIC
            else -> null
        }
    }

    private fun DataOutputStream.writeNullableUTF(str: String?) {
        if (str == null) {
            writeBoolean(false)
        } else {
            writeBoolean(true)
            writeUTF(str)
        }
    }

    private fun DataInputStream.readNullableUTF(): String? {
        return if (readBoolean()) readUTF() else null
    }

    private fun writeCellBinary(cell: VectorMapCell, file: File) {
        DataOutputStream(BufferedOutputStream(FileOutputStream(file), 8192)).use { out ->
            out.writeInt(BIN_MAGIC)
            out.writeDouble(cell.south)
            out.writeDouble(cell.west)
            out.writeDouble(cell.north)
            out.writeDouble(cell.east)

            out.writeInt(cell.areas.size)
            for (a in cell.areas) {
                out.writeInt(a.lats.size)
                for (i in a.lats.indices) {
                    out.writeFloat(a.lats[i].toFloat())
                    out.writeFloat(a.lngs[i].toFloat())
                }
                out.writeDouble(a.centerLat)
                out.writeDouble(a.centerLng)
                out.writeBoolean(a.isForestOrWood)
                out.writeBoolean(a.isWaterOrPool)
                out.writeNullableUTF(a.name)
                out.writeInt(a.poiCategory?.ordinal ?: -1)
            }

            out.writeInt(cell.buildings.size)
            for (b in cell.buildings) {
                out.writeInt(b.lats.size)
                for (i in b.lats.indices) {
                    out.writeFloat(b.lats[i].toFloat())
                    out.writeFloat(b.lngs[i].toFloat())
                }
                out.writeDouble(b.centerLat)
                out.writeDouble(b.centerLng)
                out.writeNullableUTF(b.houseNumber)
                out.writeNullableUTF(b.name)
                out.writeInt(b.poiCategory?.ordinal ?: -1)
            }

            out.writeInt(cell.roads.size)
            for (r in cell.roads) {
                out.writeInt(r.lats.size)
                for (i in r.lats.indices) {
                    out.writeFloat(r.lats[i].toFloat())
                    out.writeFloat(r.lngs[i].toFloat())
                }
                out.writeInt(r.roadClass.ordinal)
                out.writeNullableUTF(r.name)
                out.writeNullableUTF(r.nameEl)
                out.writeNullableUTF(r.nameEn)
                out.writeInt(r.onewayDir)
            }

            out.writeInt(cell.pois.size)
            for (p in cell.pois) {
                out.writeDouble(p.lat)
                out.writeDouble(p.lng)
                out.writeNullableUTF(p.name)
                out.writeNullableUTF(p.houseNumber)
                out.writeInt(p.category.ordinal)
            }
        }
    }

    private fun readCellBinary(key: String, file: File): VectorMapCell? {
        val poiCategories = VectorPoiCategory.entries
        val roadClasses = VectorRoadClass.entries
        DataInputStream(BufferedInputStream(FileInputStream(file), 8192)).use { inp ->
            if (inp.readInt() != BIN_MAGIC) return null
            val south = inp.readDouble()
            val west = inp.readDouble()
            val north = inp.readDouble()
            val east = inp.readDouble()

            val areaCount = inp.readInt().coerceIn(0, MAX_AREAS_PER_CELL * 2)
            val areas = ArrayList<VectorAreaPoly>(areaCount)
            for (idx in 0 until areaCount) {
                val n = inp.readInt().coerceIn(0, 4096)
                val lats = DoubleArray(n)
                val lngs = DoubleArray(n)
                for (i in 0 until n) {
                    lats[i] = inp.readFloat().toDouble()
                    lngs[i] = inp.readFloat().toDouble()
                }
                val cLat = inp.readDouble()
                val cLng = inp.readDouble()
                val isForest = inp.readBoolean()
                val isWater = inp.readBoolean()
                val name = inp.readNullableUTF()
                val catOrd = inp.readInt()
                val cat = poiCategories.getOrNull(catOrd)
                areas.add(VectorAreaPoly(lats, lngs, cLat, cLng, isForest, isWater, name, cat))
            }

            val buildingCount = inp.readInt().coerceIn(0, MAX_BUILDINGS_PER_CELL * 2)
            val buildings = ArrayList<VectorBuildingPoly>(buildingCount)
            for (idx in 0 until buildingCount) {
                val n = inp.readInt().coerceIn(0, 4096)
                val lats = DoubleArray(n)
                val lngs = DoubleArray(n)
                for (i in 0 until n) {
                    lats[i] = inp.readFloat().toDouble()
                    lngs[i] = inp.readFloat().toDouble()
                }
                val cLat = inp.readDouble()
                val cLng = inp.readDouble()
                val hn = inp.readNullableUTF()
                val name = inp.readNullableUTF()
                val catOrd = inp.readInt()
                val cat = poiCategories.getOrNull(catOrd)
                buildings.add(VectorBuildingPoly(lats, lngs, cLat, cLng, hn, name, cat))
            }

            val roadCount = inp.readInt().coerceIn(0, MAX_ROADS_PER_CELL * 2)
            val roads = ArrayList<VectorRoadWay>(roadCount)
            for (idx in 0 until roadCount) {
                val n = inp.readInt().coerceIn(0, 4096)
                val lats = DoubleArray(n)
                val lngs = DoubleArray(n)
                for (i in 0 until n) {
                    lats[i] = inp.readFloat().toDouble()
                    lngs[i] = inp.readFloat().toDouble()
                }
                val rcOrd = inp.readInt()
                val rc = roadClasses.getOrElse(rcOrd) { VectorRoadClass.RESIDENTIAL }
                val name = inp.readNullableUTF()
                val nameEl = inp.readNullableUTF()
                val nameEn = inp.readNullableUTF()
                val oneway = inp.readInt()
                roads.add(VectorRoadWay(lats, lngs, rc, name, nameEl, nameEn, oneway))
            }

            val poiCount = inp.readInt().coerceIn(0, MAX_POIS_PER_CELL * 2)
            val pois = ArrayList<VectorPoiNode>(poiCount)
            for (idx in 0 until poiCount) {
                val lat = inp.readDouble()
                val lng = inp.readDouble()
                val name = inp.readNullableUTF()
                val hn = inp.readNullableUTF()
                val catOrd = inp.readInt()
                val cat = poiCategories.getOrElse(catOrd) { VectorPoiCategory.GENERIC }
                pois.add(VectorPoiNode(lat, lng, name, hn, cat))
            }

            return VectorMapCell(key, south, west, north, east, areas, buildings, roads, pois)
        }
    }

    private fun computeCustomPoiPriorityRank(name: String, lat: Double, lng: Double, category: VectorPoiCategory): Int {
        val lower = name.lowercase(java.util.Locale.ROOT)
        if (lower.contains("αρχοντικό πανόραμα") ||
            lower.contains("αρχοντικο πανοραμα") ||
            lower.contains("archontiko panorama") ||
            lower.contains("mansion panorama") ||
            lower.contains("la gantem") ||
            lower.contains("lagantem")
        ) {
            return -3
        }
        // Any restaurant, Airbnb, guesthouse, pharmacy, or landmark located in Melivoia near Larissa
        if (lat in 39.738..39.762 && lng in 22.778..22.812) {
            return -2
        }
        // Prioritize restaurants, Airbnbs, hotels, companies, churches, and supermarkets in:
        // - Larissa (39.58..39.69, 22.36..22.49)
        // - Greater Thessaloniki including Central, East, West & North Thessaloniki (40.48..40.80, 22.75..23.12)
        // - Athens & Attica metropolitan area (37.82..38.14, 23.58..23.88)
        val isPriorityGreekCity = (lat in 39.58..39.69 && lng in 22.36..22.49) ||
            (lat in 40.48..40.80 && lng in 22.75..23.12) ||
            (lat in 37.82..38.14 && lng in 23.58..23.88)
        if (isPriorityGreekCity) {
            return when (category) {
                VectorPoiCategory.HOTEL_LODGING,
                VectorPoiCategory.FOOD_CAFE -> -1
                VectorPoiCategory.OFFICE_COMPANY,
                VectorPoiCategory.SHOP,
                VectorPoiCategory.WORSHIP,
                VectorPoiCategory.PHARMACY,
                VectorPoiCategory.MEDICAL,
                VectorPoiCategory.LANDMARK_TOURISM,
                VectorPoiCategory.CULTURE_CINEMA -> 0
                else -> 2
            }
        }
        return when (category) {
            VectorPoiCategory.HOTEL_LODGING,
            VectorPoiCategory.FOOD_CAFE -> 0
            VectorPoiCategory.OFFICE_COMPANY -> 1
            VectorPoiCategory.PHARMACY, VectorPoiCategory.MEDICAL -> 2
            VectorPoiCategory.SHOP -> 3
            VectorPoiCategory.LANDMARK_TOURISM, VectorPoiCategory.CULTURE_CINEMA -> 4
            VectorPoiCategory.FINANCE, VectorPoiCategory.PARKING -> 5
            VectorPoiCategory.WORSHIP -> 6
            VectorPoiCategory.SPORTS_SWIMMING -> 7
            else -> 8
        }
    }

    /**
     * Renders ultra-crisp, infinite-DPI vector buildings, roads, forest tree icons, sports/swimming
     * badges, one-way arrows, street names, house numbers, micro-furniture (benches, bins, trees),
     * and fixed-size flat POI icons/labels (like churches, supermarkets, restaurants, and Airbnbs).
     */
    fun DrawScope.drawVectorHdOverlay(
        centerLat: Double,
        centerLng: Double,
        zoom: Float,
        viewportCenterX: Float,
        viewportCenterY: Float,
        isGreek: Boolean
    ): Boolean {
        if (!zoom.isFinite() || !centerLat.isFinite() || !centerLng.isFinite() || zoom < 11.8f) return false

        val metersPerPx = (156543.03392 * cos(centerLat * PI / 180.0) / 2.0.pow(zoom.toDouble()))
            .coerceAtLeast(0.004)
        val maxBarPx = 110.dp.toPx().coerceAtLeast(100f)
        val maxMeters = metersPerPx * maxBarPx

        // Make all vector elements, POI icons, and names invisible when the zoom level exceeds 500 meters
        // (i.e., when the bottom-left scale bar steps past "500 m" to "1 km" at maxMeters >= 1000.0).
        if (maxMeters >= 1000.0) return false

        val visibleCells = ArrayList<VectorMapCell>(24)

        if (cellCache.isNotEmpty()) {
            for (cell in cellCache.values) {
                if (centerLat in (cell.south - 0.055)..(cell.north + 0.055) &&
                    centerLng in (cell.west - 0.055)..(cell.east + 0.055)
                ) {
                    visibleCells.add(cell)
                }
            }
        }

        val overlayAlpha = 1.0f

        fun metersToPx(meters: Float): Float = (meters / metersPerPx).toFloat()

        // Maintain a 100% stationary screen size regardless of the user's zoom level.
        val metersPerPxAt20m = 42.0 / maxBarPx
        val rawScaleTo20m = (metersPerPxAt20m / metersPerPx).toFloat()
        val poiScale = 1.0f

        fun project(lat: Double, lng: Double): Offset = latLngToScreenPx(
            lat = lat,
            lng = lng,
            centerLat = centerLat,
            centerLng = centerLng,
            zoom = zoom,
            viewportCenterX = viewportCenterX,
            viewportCenterY = viewportCenterY
        )

        val reusablePath = reusableMainPath
        val nativeCanvas = drawContext.canvas.nativeCanvas
        val placedLabelRects = ArrayList<Rect>(120)

        fun tryClaimRect(rect: Rect): Boolean {
            for (existing in placedLabelRects) {
                if (rect.overlaps(existing)) return false
            }
            placedLabelRects.add(rect)
            return true
        }

        if (zoom >= 16.2f && visibleCells.isNotEmpty()) {
        // 0. Mask out underlying raster POI icons/labels with the tile land color (#2C3442) BEFORE drawing
        //    vector parks, roads, and buildings so baked-in raster icons never scale or show through.
        run {
            val landMaskColor = Color(0xFF2C3442).copy(alpha = overlayAlpha)
            val maskHalfW = max(metersToPx(7.8f), 18.dp.toPx())
            val maskTopH = max(metersToPx(2.4f), 8.dp.toPx())
            val maskBotH = max(metersToPx(7.2f), 18.dp.toPx())
            val maskCorner = CornerRadius(4.dp.toPx(), 4.dp.toPx())

            for (cell in visibleCells) {
                for (p in cell.pois) {
                    if (p.category == VectorPoiCategory.BENCH ||
                        p.category == VectorPoiCategory.WASTE_BASKET ||
                        p.category == VectorPoiCategory.TREE
                    ) continue
                    val pt = project(p.lat, p.lng)
                    if (pt.x !in -120f..(size.width + 120f) || pt.y !in -120f..(size.height + 120f)) continue
                    drawRoundRect(
                        color = landMaskColor,
                        topLeft = Offset(pt.x - maskHalfW, pt.y - maskTopH),
                        size = Size(maskHalfW * 2f, maskTopH + maskBotH),
                        cornerRadius = maskCorner
                    )
                }
                for (b in cell.buildings) {
                    if (b.name == null && b.poiCategory == null) continue
                    val pt = project(b.centerLat, b.centerLng)
                    if (pt.x !in -120f..(size.width + 120f) || pt.y !in -120f..(size.height + 120f)) continue
                    drawRoundRect(
                        color = landMaskColor,
                        topLeft = Offset(pt.x - maskHalfW, pt.y - maskTopH),
                        size = Size(maskHalfW * 2f, maskTopH + maskBotH),
                        cornerRadius = maskCorner
                    )
                }
            }
        }

        // 1. Vector Forests, Parks, Sports Grounds & Water Pools (plus crisp vector tree symbols inside forests!)
        val parkFillColor = Color(0xFF1D3530).copy(alpha = overlayAlpha * 0.75f)
        val parkStrokeColor = Color(0xFF3E6E62).copy(alpha = overlayAlpha)
        val poolFillColor = Color(0xFF1B3B5C).copy(alpha = overlayAlpha * 0.85f)
        val poolStrokeColor = Color(0xFF5690C8).copy(alpha = overlayAlpha)

        for (cell in visibleCells) {
            for (area in cell.areas) {
                reusablePath.reset()
                var minX = Float.MAX_VALUE
                var maxX = -Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxY = -Float.MAX_VALUE
                for (i in area.lats.indices) {
                    val pt = project(area.lats[i], area.lngs[i])
                    minX = min(minX, pt.x)
                    maxX = max(maxX, pt.x)
                    minY = min(minY, pt.y)
                    maxY = max(maxY, pt.y)
                    if (i == 0) reusablePath.moveTo(pt.x, pt.y) else reusablePath.lineTo(pt.x, pt.y)
                }
                if (maxX >= -80f && minX <= size.width + 80f && maxY >= -80f && minY <= size.height + 80f) {
                    reusablePath.close()
                    if (area.isWaterOrPool) {
                        drawPath(reusablePath, color = poolFillColor, style = Fill)
                        drawPath(reusablePath, color = poolStrokeColor, style = Stroke(width = 1.4.dp.toPx()))
                    } else {
                        drawPath(reusablePath, color = parkFillColor, style = Fill)
                        drawPath(reusablePath, color = parkStrokeColor, style = Stroke(width = 1.2.dp.toPx()))
                    }

                    if (area.isForestOrWood && zoom >= 16.8f && area.lats.size <= 160) {
                        drawForestTreeSymbolsForArea(
                            area = area,
                            centerLat = centerLat,
                            centerLng = centerLng,
                            zoom = zoom,
                            viewportCenterX = viewportCenterX,
                            viewportCenterY = viewportCenterY,
                            overlayAlpha = overlayAlpha
                        )
                    }
                }
            }
        }

        // 2. Vector Road Casings (Pass 1) & Vector Road Surfaces (Pass 2) with true real-world meter scaling
        val roadCasingColor = Color(0xFF161C27).copy(alpha = overlayAlpha)
        val footpathDash = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()), 0f)

        for (cell in visibleCells) {
            for (road in cell.roads) {
                if (road.roadClass == VectorRoadClass.FOOTPATH || road.roadClass == VectorRoadClass.CYCLEWAY) continue
                val widthMeters = when (road.roadClass) {
                    VectorRoadClass.MOTORWAY_PRIMARY -> 11.5f
                    VectorRoadClass.SECONDARY -> 9.2f
                    VectorRoadClass.TERTIARY -> 7.8f
                    VectorRoadClass.RESIDENTIAL -> 6.4f
                    VectorRoadClass.PEDESTRIAN -> 5.6f
                    VectorRoadClass.SERVICE -> 4.0f
                    else -> 2.0f
                }
                val roadPx = metersToPx(widthMeters).coerceIn(4.dp.toPx(), 110.dp.toPx())
                val casingPx = roadPx + 2.8.dp.toPx()

                reusablePath.reset()
                var anyVisible = false
                for (i in road.lats.indices) {
                    val pt = project(road.lats[i], road.lngs[i])
                    if (pt.x in -160f..(size.width + 160f) && pt.y in -160f..(size.height + 160f)) {
                        anyVisible = true
                    }
                    if (i == 0) reusablePath.moveTo(pt.x, pt.y) else reusablePath.lineTo(pt.x, pt.y)
                }
                if (anyVisible) {
                    drawPath(
                        path = reusablePath,
                        color = roadCasingColor,
                        style = Stroke(width = casingPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }

        for (cell in visibleCells) {
            for (road in cell.roads) {
                val widthMeters = when (road.roadClass) {
                    VectorRoadClass.MOTORWAY_PRIMARY -> 11.5f
                    VectorRoadClass.SECONDARY -> 9.2f
                    VectorRoadClass.TERTIARY -> 7.8f
                    VectorRoadClass.RESIDENTIAL -> 6.4f
                    VectorRoadClass.PEDESTRIAN -> 5.6f
                    VectorRoadClass.SERVICE -> 4.0f
                    VectorRoadClass.CYCLEWAY -> 2.2f
                    VectorRoadClass.FOOTPATH -> 1.8f
                }
                val fillColor = when (road.roadClass) {
                    VectorRoadClass.MOTORWAY_PRIMARY -> Color(0xFF9C744A).copy(alpha = overlayAlpha)
                    VectorRoadClass.SECONDARY -> Color(0xFF866C4E).copy(alpha = overlayAlpha)
                    VectorRoadClass.TERTIARY -> Color(0xFF52627C).copy(alpha = overlayAlpha)
                    VectorRoadClass.RESIDENTIAL -> Color(0xFF495872).copy(alpha = overlayAlpha)
                    VectorRoadClass.PEDESTRIAN -> Color(0xFF404E64).copy(alpha = overlayAlpha)
                    VectorRoadClass.SERVICE -> Color(0xFF3D4A60).copy(alpha = overlayAlpha)
                    VectorRoadClass.CYCLEWAY -> Color(0xFF5482B4).copy(alpha = overlayAlpha)
                    VectorRoadClass.FOOTPATH -> Color(0xFF8E7066).copy(alpha = overlayAlpha)
                }

                reusablePath.reset()
                var anyVisible = false
                for (i in road.lats.indices) {
                    val pt = project(road.lats[i], road.lngs[i])
                    if (pt.x in -160f..(size.width + 160f) && pt.y in -160f..(size.height + 160f)) {
                        anyVisible = true
                    }
                    if (i == 0) reusablePath.moveTo(pt.x, pt.y) else reusablePath.lineTo(pt.x, pt.y)
                }
                if (anyVisible) {
                    val isDashed = road.roadClass == VectorRoadClass.FOOTPATH || road.roadClass == VectorRoadClass.CYCLEWAY
                    val strokeW = if (isDashed) {
                        metersToPx(widthMeters).coerceIn(2.0.dp.toPx(), 7.dp.toPx())
                    } else {
                        metersToPx(widthMeters).coerceIn(4.dp.toPx(), 110.dp.toPx())
                    }
                    drawPath(
                        path = reusablePath,
                        color = fillColor,
                        style = Stroke(
                            width = strokeW,
                            cap = if (isDashed) StrokeCap.Butt else StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = if (isDashed) footpathDash else null
                        )
                    )
                }
            }
        }

        // 3. Vector Buildings with 3D Extruded Shadow & Crisp Warm Architectural Outlines
        val buildingShadowColor = Color(0xFF151A24).copy(alpha = overlayAlpha * 0.85f)
        val buildingFillAlpha = if (rawScaleTo20m >= 1.0f) overlayAlpha else overlayAlpha * 0.94f
        val buildingFillColor = Color(0xFF2E3747).copy(alpha = buildingFillAlpha)
        val buildingOutlineColor = Color(0xFF887E6A).copy(alpha = overlayAlpha)
        val buildingStrokePx = 1.4.dp.toPx()
        val shadowOffsetPx = 1.6.dp.toPx()
        val shadowPath = reusableShadowPath

        val seenBuildings = HashSet<Long>(256)
        for (cell in visibleCells) {
            for (b in cell.buildings) {
                val bHash = (b.centerLat * 1000000).toLong() xor ((b.centerLng * 1000000).toLong() shl 20)
                if (!seenBuildings.add(bHash)) continue

                val cPt = project(b.centerLat, b.centerLng)
                if (cPt.x !in -240f..(size.width + 240f) || cPt.y !in -240f..(size.height + 240f)) continue

                reusablePath.reset()
                shadowPath.reset()
                for (i in b.lats.indices) {
                    val pt = project(b.lats[i], b.lngs[i])
                    if (i == 0) {
                        reusablePath.moveTo(pt.x, pt.y)
                        shadowPath.moveTo(pt.x + shadowOffsetPx, pt.y + shadowOffsetPx)
                    } else {
                        reusablePath.lineTo(pt.x, pt.y)
                        shadowPath.lineTo(pt.x + shadowOffsetPx, pt.y + shadowOffsetPx)
                    }
                }
                reusablePath.close()
                shadowPath.close()

                drawPath(path = shadowPath, color = buildingShadowColor, style = Fill)
                drawPath(path = reusablePath, color = buildingFillColor, style = Fill)
                drawPath(
                    path = reusablePath,
                    color = buildingOutlineColor,
                    style = Stroke(width = buildingStrokePx, join = StrokeJoin.Round)
                )
            }
        }

        // 4. Vector One-Way Street Arrows (capped at 20m-height size)
        if (zoom >= 17.0f) {
            val arrowFill = Color(0xFFC4D4EA).copy(alpha = overlayAlpha)
            val arrowHalo = Color(0xFF151B26).copy(alpha = overlayAlpha)
            val arrowLen = 6.0.dp.toPx() * poiScale
            val arrowWing = arrowLen * 0.6f

            for (cell in visibleCells) {
                for (road in cell.roads) {
                    if (road.onewayDir == 0) continue
                    for (i in 0 until road.lats.size - 1) {
                        val p1 = project(road.lats[i], road.lngs[i])
                        val p2 = project(road.lats[i + 1], road.lngs[i + 1])
                        val segLen = hypot(p2.x - p1.x, p2.y - p1.y)
                        if (segLen < 68.dp.toPx()) continue

                        val midX = (p1.x + p2.x) * 0.5f
                        val midY = (p1.y + p2.y) * 0.5f
                        if (midX !in 16f..(size.width - 16f) || midY !in 16f..(size.height - 16f)) continue

                        val dirX = if (road.onewayDir > 0) (p2.x - p1.x) / segLen else (p1.x - p2.x) / segLen
                        val dirY = if (road.onewayDir > 0) (p2.y - p1.y) / segLen else (p1.y - p2.y) / segLen
                        val perpX = -dirY
                        val perpY = dirX

                        val tail = Offset(midX - dirX * arrowLen, midY - dirY * arrowLen)
                        val tip = Offset(midX + dirX * arrowLen, midY + dirY * arrowLen)
                        val leftWing = Offset(
                            tip.x - dirX * arrowWing + perpX * (arrowWing * 0.72f),
                            tip.y - dirY * arrowWing + perpY * (arrowWing * 0.72f)
                        )
                        val rightWing = Offset(
                            tip.x - dirX * arrowWing - perpX * (arrowWing * 0.72f),
                            tip.y - dirY * arrowWing - perpY * (arrowWing * 0.72f)
                        )

                        drawLine(arrowHalo, tail, tip, strokeWidth = 3.2.dp.toPx() * poiScale, cap = StrokeCap.Round)
                        drawLine(arrowFill, tail, tip, strokeWidth = 1.7.dp.toPx() * poiScale, cap = StrokeCap.Round)

                        reusablePath.reset()
                        reusablePath.moveTo(leftWing.x, leftWing.y)
                        reusablePath.lineTo(tip.x, tip.y)
                        reusablePath.lineTo(rightWing.x, rightWing.y)
                        drawPath(reusablePath, arrowHalo, style = Stroke(width = 3.2.dp.toPx() * poiScale, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        drawPath(reusablePath, arrowFill, style = Stroke(width = 1.7.dp.toPx() * poiScale, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }
        }

        // 5. Vector Street Name Typography Aligned Along Road Segments (capped at 20m-height size)
        val streetTextSize = 12.2.sp.toPx() * poiScale.coerceIn(0.85f, 1.0f)
        val streetHaloPaint = sharedStreetHaloPaint.apply {
            textSize = streetTextSize
            strokeWidth = 3.8.dp.toPx() * poiScale.coerceIn(0.85f, 1.0f)
            color = android.graphics.Color.argb((overlayAlpha * 250).toInt(), 16, 21, 30)
        }
        val streetFillPaint = sharedStreetFillPaint.apply {
            textSize = streetTextSize
            color = android.graphics.Color.argb((overlayAlpha * 255).toInt(), 242, 246, 254)
        }

        for (cell in visibleCells) {
            for (road in cell.roads) {
                val label = if (isGreek) {
                    road.nameEl ?: road.name ?: road.nameEn
                } else {
                    road.name ?: road.nameEn ?: road.nameEl
                } ?: continue

                val textW = streetFillPaint.measureText(label)
                for (i in 0 until road.lats.size - 1) {
                    val p1 = project(road.lats[i], road.lngs[i])
                    val p2 = project(road.lats[i + 1], road.lngs[i + 1])
                    val segLen = hypot(p2.x - p1.x, p2.y - p1.y)
                    if (segLen < textW * 0.85f) continue

                    val midX = (p1.x + p2.x) * 0.5f
                    val midY = (p1.y + p2.y) * 0.5f
                    if (midX !in 32f..(size.width - 32f) || midY !in 32f..(size.height - 32f)) continue

                    val pad = 10.dp.toPx()
                    val candidateRect = Rect(
                        midX - textW * 0.42f - pad,
                        midY - streetTextSize - pad,
                        midX + textW * 0.42f + pad,
                        midY + streetTextSize + pad
                    )
                    if (!tryClaimRect(candidateRect)) continue

                    var angleDeg = (atan2(p2.y - p1.y, p2.x - p1.x) * 180.0 / PI).toFloat()
                    if (angleDeg > 90f) angleDeg -= 180f
                    if (angleDeg < -90f) angleDeg += 180f

                    nativeCanvas.save()
                    nativeCanvas.rotate(angleDeg, midX, midY)
                    val baselineY = midY + streetTextSize * 0.35f
                    nativeCanvas.drawText(label, midX, baselineY, streetHaloPaint)
                    nativeCanvas.drawText(label, midX, baselineY, streetFillPaint)
                    nativeCanvas.restore()
                }
            }
        }
        } // end if (zoom >= 16.2f && visibleCells.isNotEmpty())

        // 6. Flat Standalone POI Icons & Centered Multi-Line Labels matching Screenshot 1 (Faliraki / Εν πλω / Imabari)
        //    Fixed stationary screen size matching 50-meter height (never scales up or down with zoom)
        //    with calm, subtle, non-eye-catching tones that blend harmoniously with the dark map.
        val poiTextSize = 8.5.sp.toPx()
        val poiHaloPaint = sharedPoiHaloPaint.apply {
            textSize = poiTextSize
            strokeWidth = 2.2.dp.toPx()
            color = android.graphics.Color.argb((overlayAlpha * 240).toInt(), 16, 20, 28)
            textAlign = Paint.Align.CENTER
        }
        val poiFillPaint = sharedPoiFillPaint.apply {
            textSize = poiTextSize
            textAlign = Paint.Align.CENTER
        }

        data class RenderablePoi(
            val lat: Double,
            val lng: Double,
            val name: String,
            val category: VectorPoiCategory,
            val priorityRank: Int
        )

        val allNamedPois = ArrayList<RenderablePoi>(240)
        val seenPoiNames = HashSet<String>(240)

        // First inject curated priority POIs for Melivoia, Larissa, Thessaloniki, and Athens
        for (p in PRIORITY_GREEK_POIS) {
            if (abs(p.lat - centerLat) <= 0.14 && abs(p.lng - centerLng) <= 0.14) {
                val displayName = if (isGreek) p.nameEl else p.nameEn
                val dedupKey = "${p.nameEl.lowercase(java.util.Locale.ROOT)}_${(p.lat * 1800).toInt()}_${(p.lng * 1800).toInt()}"
                if (seenPoiNames.add(dedupKey)) {
                    allNamedPois.add(RenderablePoi(p.lat, p.lng, displayName, p.category, p.priorityRank))
                }
            }
        }

        for (cell in visibleCells) {
            for (p in cell.pois) {
                val n = p.name ?: continue
                if (p.category == VectorPoiCategory.BENCH ||
                    p.category == VectorPoiCategory.WASTE_BASKET ||
                    p.category == VectorPoiCategory.TREE
                ) continue
                val dedupKey = "${n.lowercase(java.util.Locale.ROOT)}_${(p.lat * 1800).toInt()}_${(p.lng * 1800).toInt()}"
                if (seenPoiNames.add(dedupKey)) {
                    val rank = computeCustomPoiPriorityRank(n, p.lat, p.lng, p.category)
                    allNamedPois.add(RenderablePoi(p.lat, p.lng, n, p.category, rank))
                }
            }
            for (b in cell.buildings) {
                val n = b.name ?: continue
                val cat = b.poiCategory ?: VectorPoiCategory.GENERIC
                val dedupKey = "${n.lowercase(java.util.Locale.ROOT)}_${(b.centerLat * 1800).toInt()}_${(b.centerLng * 1800).toInt()}"
                if (seenPoiNames.add(dedupKey)) {
                    val rank = computeCustomPoiPriorityRank(n, b.centerLat, b.centerLng, cat)
                    allNamedPois.add(RenderablePoi(b.centerLat, b.centerLng, n, cat, rank))
                }
            }
            for (a in cell.areas) {
                val n = a.name ?: continue
                val cat = a.poiCategory ?: VectorPoiCategory.SPORTS_SWIMMING
                val dedupKey = "${n.lowercase(java.util.Locale.ROOT)}_${(a.centerLat * 1800).toInt()}_${(a.centerLng * 1800).toInt()}"
                if (seenPoiNames.add(dedupKey)) {
                    val rank = computeCustomPoiPriorityRank(n, a.centerLat, a.centerLng, cat)
                    allNamedPois.add(RenderablePoi(a.centerLat, a.centerLng, n, cat, rank))
                }
            }
        }

        if (allNamedPois.isEmpty() && visibleCells.isEmpty()) return false

        // Prioritize Αρχοντικό Πανόραμα, La Gantem, Melivoia, Larissa, Thessaloniki, Athens, and all Airbnbs & Restaurants first!
        allNamedPois.sortBy { it.priorityRank }

        val maxPoisToRender = 140
        val placedPoiRects = ArrayList<Rect>(maxPoisToRender * 2)

        fun hasOverlap(rect: Rect): Boolean {
            for (existing in placedPoiRects) {
                if (rect.overlaps(existing)) return true
            }
            return false
        }

        // Fixed stationary 50m-height screen-space dimensions (never scaled up or down by zoom!)
        val iconHalfR = 4.0.dp.toPx()
        val fixedGlyphScale = 0.72f
        val bottomGap = iconHalfR + 1.8.dp.toPx()
        val lineStep = poiTextSize + 1.3.dp.toPx()
        val padX = 1.2.dp.toPx()
        val padY = 0.8.dp.toPx()

        var drawnPoisCount = 0
        for (poi in allNamedPois) {
            if (drawnPoisCount >= maxPoisToRender && poi.priorityRank >= 0) break
            val pt = project(poi.lat, poi.lng)
            if (pt.x !in 18f..(size.width - 18f) || pt.y !in 18f..(size.height - 18f)) continue

            val lines = splitPoiLabelLines(poi.name)
            val maxLineW = lines.maxOfOrNull { poiFillPaint.measureText(it) } ?: 28f
            val totalTextH = lines.size * lineStep

            val iconRect = Rect(
                pt.x - iconHalfR - 1.2.dp.toPx(),
                pt.y - iconHalfR - 1.2.dp.toPx(),
                pt.x + iconHalfR + 1.2.dp.toPx(),
                pt.y + iconHalfR + 1.2.dp.toPx()
            )
            val bottomLabelRect = Rect(
                pt.x - maxLineW * 0.5f - padX,
                pt.y + bottomGap,
                pt.x + maxLineW * 0.5f + padX,
                pt.y + bottomGap + totalTextH + padY
            )

            // Priority POIs (-3 and -2) always render; standard POIs avoid colliding with already-placed icons/labels
            if (poi.priorityRank >= -1 && (hasOverlap(iconRect) || hasOverlap(bottomLabelRect))) {
                continue
            }

            placedPoiRects.add(iconRect)
            placedPoiRects.add(bottomLabelRect)
            placedLabelRects.add(iconRect)
            placedLabelRects.add(bottomLabelRect)

            drawFixedSizeFlatPoiIcon(
                center = pt,
                category = poi.category,
                name = poi.name,
                alpha = overlayAlpha,
                iconScale = fixedGlyphScale
            )
            drawnPoisCount++

            // Subtle, elegant, non-eye-catching label colors that blend naturally with the dark basemap
            val labelRgb = when (poi.category) {
                VectorPoiCategory.FOOD_CAFE -> Triple(188, 168, 142)         // Soft warm stone beige (#BCA88E)
                VectorPoiCategory.HOTEL_LODGING -> Triple(186, 160, 172)     // Muted dusty mauve (#BAA0AC)
                VectorPoiCategory.WORSHIP -> Triple(164, 180, 194)           // Subdued slate grey-blue (#A4B4C2)
                VectorPoiCategory.LANDMARK_TOURISM -> Triple(176, 168, 188)  // Muted heather slate (#B0A8BC)
                VectorPoiCategory.CULTURE_CINEMA -> Triple(176, 168, 188)    // Muted heather slate (#B0A8BC)
                VectorPoiCategory.SPORTS_SWIMMING -> Triple(152, 178, 162)   // Muted sage green (#98B2A2)
                VectorPoiCategory.PHARMACY,
                VectorPoiCategory.MEDICAL -> Triple(192, 156, 156)           // Muted dusty terracotta (#C09C9C)
                VectorPoiCategory.SHOP,
                VectorPoiCategory.PARKING,
                VectorPoiCategory.FINANCE,
                VectorPoiCategory.OFFICE_COMPANY -> Triple(158, 176, 194)    // Calm cool slate (#9EB0C2)
                else -> Triple(168, 176, 186)
            }
            poiFillPaint.color = android.graphics.Color.argb(
                (overlayAlpha * 240).toInt(),
                labelRgb.first,
                labelRgb.second,
                labelRgb.third
            )

            var lineY = pt.y + bottomGap + poiTextSize * 0.86f
            for (line in lines) {
                nativeCanvas.drawText(line, pt.x, lineY, poiHaloPaint)
                nativeCanvas.drawText(line, pt.x, lineY, poiFillPaint)
                lineY += lineStep
            }
        }

        if (zoom >= 16.2f) {
            // 7. Vector House Numbers (e.g., "35") inside buildings and on address nodes (capped at 20m-height size)
            if (zoom >= 17.2f) {
                val hnSize = 9.6.sp.toPx() * poiScale
                val hnHaloPaint = sharedHnHaloPaint.apply {
                    textSize = hnSize
                    strokeWidth = 2.6.dp.toPx() * poiScale
                    color = android.graphics.Color.argb((overlayAlpha * 245).toInt(), 18, 23, 32)
                }
                val hnFillPaint = sharedHnFillPaint.apply {
                    textSize = hnSize
                    color = android.graphics.Color.argb((overlayAlpha * 248).toInt(), 196, 208, 228)
                }

                for (cell in visibleCells) {
                    for (b in cell.buildings) {
                        val hn = b.houseNumber ?: continue
                        if (b.name != null) continue
                        val pt = project(b.centerLat, b.centerLng)
                        if (pt.x !in 20f..(size.width - 20f) || pt.y !in 20f..(size.height - 20f)) continue
                        val rect = Rect(pt.x - 11.dp.toPx(), pt.y - 8.dp.toPx(), pt.x + 11.dp.toPx(), pt.y + 8.dp.toPx())
                        if (tryClaimRect(rect)) {
                            nativeCanvas.drawText(hn, pt.x, pt.y + hnSize * 0.35f, hnHaloPaint)
                            nativeCanvas.drawText(hn, pt.x, pt.y + hnSize * 0.35f, hnFillPaint)
                        }
                    }
                    for (p in cell.pois) {
                        val hn = p.houseNumber ?: continue
                        if (p.name != null) continue
                        val pt = project(p.lat, p.lng)
                        if (pt.x !in 20f..(size.width - 20f) || pt.y !in 20f..(size.height - 20f)) continue
                        val rect = Rect(pt.x - 11.dp.toPx(), pt.y - 8.dp.toPx(), pt.x + 11.dp.toPx(), pt.y + 8.dp.toPx())
                        if (tryClaimRect(rect)) {
                            nativeCanvas.drawText(hn, pt.x, pt.y + hnSize * 0.35f, hnHaloPaint)
                            nativeCanvas.drawText(hn, pt.x, pt.y + hnSize * 0.35f, hnFillPaint)
                        }
                    }
                }
            }

            // 8. Vector Micro-Street Furniture (Benches, Waste Baskets, Street Trees)
            if (zoom >= 17.8f) {
                for (cell in visibleCells) {
                    for (poi in cell.pois) {
                        if (poi.category != VectorPoiCategory.BENCH &&
                            poi.category != VectorPoiCategory.WASTE_BASKET &&
                            poi.category != VectorPoiCategory.TREE
                        ) continue

                        val pt = project(poi.lat, poi.lng)
                        if (pt.x !in 12f..(size.width - 12f) || pt.y !in 12f..(size.height - 12f)) continue
                        val iconRect = Rect(pt.x - 7.dp.toPx(), pt.y - 7.dp.toPx(), pt.x + 7.dp.toPx(), pt.y + 7.dp.toPx())
                        if (!tryClaimRect(iconRect)) continue

                        when (poi.category) {
                            VectorPoiCategory.BENCH -> drawVectorBenchIcon(pt, overlayAlpha)
                            VectorPoiCategory.WASTE_BASKET -> drawVectorWasteBasketIcon(pt, overlayAlpha)
                            VectorPoiCategory.TREE -> drawVectorForestTreeGlyph(pt, overlayAlpha)
                            else -> {}
                        }
                    }
                }
            }
        }

        return zoom >= 16.2f
    }

    private fun pointInPolygon(lat: Double, lng: Double, lats: DoubleArray, lngs: DoubleArray): Boolean {
        var inside = false
        var j = lats.size - 1
        for (i in lats.indices) {
            val yi = lats[i]
            val xi = lngs[i]
            val yj = lats[j]
            val xj = lngs[j]
            val intersect = ((yi > lat) != (yj > lat)) &&
                (lng < (xj - xi) * (lat - yi) / ((yj - yi).takeIf { it != 0.0 } ?: 1e-9) + xi)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    private fun splitPoiLabelLines(rawName: String): List<String> {
        val trimmed = rawName.trim()
        if (trimmed.length <= 13 || !trimmed.contains(' ')) return listOf(trimmed)
        val words = trimmed.split(Regex("\\s+"))
        val lines = ArrayList<String>(3)
        var currentLine = words[0]
        for (i in 1 until words.size) {
            val w = words[i]
            if (currentLine.length + 1 + w.length <= 15 || lines.size >= 2) {
                currentLine = "$currentLine $w"
            } else {
                lines.add(currentLine)
                currentLine = w
            }
        }
        lines.add(currentLine)
        return lines
    }

    /**
     * Renders a flat standalone OSM-style POI icon centered directly at `center` (no circular badge disc,
     * no teardrop pin, no pointer stem), with a tight dark contour outline and subtle non-eye-catching tones
     * that blend seamlessly into the map.
     */
    private fun DrawScope.drawFixedSizeFlatPoiIcon(
        center: Offset,
        category: VectorPoiCategory,
        name: String,
        alpha: Float,
        iconScale: Float
    ) {
        fun Float.s(): Float = this * iconScale

        // Subtle, refined, non-eye-catching pastel/earthy tones
        val iconColor = when (category) {
            VectorPoiCategory.FOOD_CAFE -> Color(0xFFBCAB94)        // Warm muted stone sand (#BCAB94)
            VectorPoiCategory.HOTEL_LODGING -> Color(0xFFBAA4AE)    // Subdued dusty mauve (#BAA4AE)
            VectorPoiCategory.WORSHIP -> Color(0xFFA6B6C4)          // Soft slate grey-blue (#A6B6C4)
            VectorPoiCategory.LANDMARK_TOURISM -> Color(0xFFB0A6BA) // Muted heather (#B0A6BA)
            VectorPoiCategory.CULTURE_CINEMA -> Color(0xFFB0A6BA)   // Muted heather (#B0A6BA)
            VectorPoiCategory.SPORTS_SWIMMING -> Color(0xFFA0B4A6)  // Calm sage (#A0B4A6)
            VectorPoiCategory.PHARMACY,
            VectorPoiCategory.MEDICAL -> Color(0xFFC0A4A4)          // Subdued dusty terracotta (#C0A4A4)
            VectorPoiCategory.SHOP,
            VectorPoiCategory.PARKING,
            VectorPoiCategory.FINANCE,
            VectorPoiCategory.OFFICE_COMPANY -> Color(0xFFA4B4C4)   // Muted calm slate-blue (#A4B4C4)
            else -> Color(0xFFA0ACB8)
        }.copy(alpha = alpha)

        val haloColor = Color(0xFF101620).copy(alpha = alpha * 0.92f)
        val haloExtra = 1.6.dp.toPx().s()
        val cx = center.x
        val cy = center.y

        when (category) {
            VectorPoiCategory.FOOD_CAFE -> {
                val lower = name.lowercase(java.util.Locale.ROOT)
                val isCocktailOrBar = lower.contains("bar") || lower.contains("μπαρ") ||
                    lower.contains("cocktail") || lower.contains("pub") || lower.contains("club") ||
                    lower.contains("lounge") || lower.contains("imabari") || lower.contains("wine")
                val isCafeOrBakery = !lower.contains("restaurant") && !lower.contains("εστιατόριο") &&
                    !lower.contains("ταβέρνα") && !lower.contains("taverna") && (
                        lower.contains("καφέ") || lower.contains("καφενείο") ||
                        lower.contains("καφενειο") || lower.contains("cafe") || lower.contains("coffee") ||
                        lower.contains("espresso") || lower.contains("μπουγάτσα") || lower.contains("τερκενλής") ||
                        lower.contains("terkenlis") || lower.contains("bougatsa") || lower.contains("bakery") ||
                        lower.contains("φούρνος") || lower.contains("ζαχαροπλαστείο")
                    )

                if (isCocktailOrBar) {
                    // OSM Martini / Cocktail Glass icon matching "Imabari" in Screenshot 1
                    val topY = cy - 4.8.dp.toPx().s()
                    val bowlBotY = cy + 0.2.dp.toPx().s()
                    val baseY = cy + 4.8.dp.toPx().s()
                    val halfBowlW = 4.5.dp.toPx().s()
                    val halfBaseW = 3.0.dp.toPx().s()

                    reusableTeardropPath.reset()
                    reusableTeardropPath.moveTo(cx - halfBowlW, topY)
                    reusableTeardropPath.lineTo(cx + halfBowlW, topY)
                    reusableTeardropPath.lineTo(cx, bowlBotY)
                    reusableTeardropPath.close()

                    // Tight contour halo
                    drawPath(
                        path = reusableTeardropPath,
                        color = haloColor,
                        style = Stroke(width = haloExtra, join = StrokeJoin.Round)
                    )
                    drawLine(haloColor, Offset(cx, bowlBotY - 0.5.dp.toPx().s()), Offset(cx, baseY), strokeWidth = 1.5.dp.toPx().s() + haloExtra, cap = StrokeCap.Round)
                    drawLine(haloColor, Offset(cx - halfBaseW, baseY), Offset(cx + halfBaseW, baseY), strokeWidth = 1.5.dp.toPx().s() + haloExtra, cap = StrokeCap.Round)

                    // Warm golden-sand fill
                    drawPath(path = reusableTeardropPath, color = iconColor, style = Fill)
                    drawLine(iconColor, Offset(cx, bowlBotY - 0.5.dp.toPx().s()), Offset(cx, baseY), strokeWidth = 1.5.dp.toPx().s(), cap = StrokeCap.Round)
                    drawLine(iconColor, Offset(cx - halfBaseW, baseY), Offset(cx + halfBaseW, baseY), strokeWidth = 1.5.dp.toPx().s(), cap = StrokeCap.Round)
                } else if (isCafeOrBakery) {
                    // OSM Coffee Mug icon with right handle matching Screenshot 1
                    val cupW = 6.6.dp.toPx().s()
                    val cupH = 7.4.dp.toPx().s()
                    val cupLeft = cx - cupW * 0.58f
                    val cupTop = cy - cupH * 0.48f
                    val handleCenter = Offset(cupLeft + cupW + 0.4.dp.toPx().s(), cupTop + cupH * 0.46f)
                    val handleR = 2.0.dp.toPx().s()

                    // Tight contour halo
                    drawRoundRect(
                        color = haloColor,
                        topLeft = Offset(cupLeft - haloExtra * 0.5f, cupTop - haloExtra * 0.5f),
                        size = Size(cupW + haloExtra, cupH + haloExtra),
                        cornerRadius = CornerRadius(1.8.dp.toPx().s(), 1.8.dp.toPx().s())
                    )
                    drawCircle(
                        color = haloColor,
                        radius = handleR,
                        center = handleCenter,
                        style = Stroke(width = 1.4.dp.toPx().s() + haloExtra)
                    )

                    // Warm golden-sand fill
                    drawRoundRect(
                        color = iconColor,
                        topLeft = Offset(cupLeft, cupTop),
                        size = Size(cupW, cupH),
                        cornerRadius = CornerRadius(1.4.dp.toPx().s(), 1.4.dp.toPx().s())
                    )
                    drawCircle(
                        color = iconColor,
                        radius = handleR,
                        center = handleCenter,
                        style = Stroke(width = 1.4.dp.toPx().s())
                    )
                } else {
                    // OSM Restaurant Fork (left) & Spoon (right) icon matching "Faliraki Cafe Restaurant Corfu" and "Εν πλω" in Screenshot 1
                    val fkX = cx - 2.6.dp.toPx().s()
                    val spX = cx + 2.6.dp.toPx().s()
                    val topY = cy - 5.2.dp.toPx().s()
                    val forkNeckY = cy - 0.8.dp.toPx().s()
                    val botY = cy + 5.2.dp.toPx().s()
                    val tineSpread = 1.65.dp.toPx().s()

                    val spoonW = 3.8.dp.toPx().s()
                    val spoonBowlH = 5.0.dp.toPx().s()

                    // Tight contour halo behind fork & spoon
                    drawLine(haloColor, Offset(fkX, forkNeckY), Offset(fkX, botY), strokeWidth = 1.6.dp.toPx().s() + haloExtra, cap = StrokeCap.Round)
                    drawRoundRect(
                        color = haloColor,
                        topLeft = Offset(fkX - tineSpread - haloExtra * 0.6f, topY - haloExtra * 0.5f),
                        size = Size(tineSpread * 2f + haloExtra * 1.2f, (forkNeckY - topY) + haloExtra),
                        cornerRadius = CornerRadius(1.5.dp.toPx().s(), 1.5.dp.toPx().s())
                    )
                    drawLine(haloColor, Offset(spX, topY + spoonBowlH * 0.7f), Offset(spX, botY), strokeWidth = 1.6.dp.toPx().s() + haloExtra, cap = StrokeCap.Round)
                    drawOval(
                        color = haloColor,
                        topLeft = Offset(spX - spoonW * 0.5f - haloExtra * 0.5f, topY - haloExtra * 0.5f),
                        size = Size(spoonW + haloExtra, spoonBowlH + haloExtra)
                    )

                    // Left: 3-tine Fork in warm golden-sand (#CCA876)
                    drawLine(iconColor, Offset(fkX, forkNeckY), Offset(fkX, botY), strokeWidth = 1.55.dp.toPx().s(), cap = StrokeCap.Round)
                    drawLine(iconColor, Offset(fkX - tineSpread, topY), Offset(fkX - tineSpread, forkNeckY), strokeWidth = 1.05.dp.toPx().s(), cap = StrokeCap.Round)
                    drawLine(iconColor, Offset(fkX, topY), Offset(fkX, forkNeckY), strokeWidth = 1.05.dp.toPx().s(), cap = StrokeCap.Round)
                    drawLine(iconColor, Offset(fkX + tineSpread, topY), Offset(fkX + tineSpread, forkNeckY), strokeWidth = 1.05.dp.toPx().s(), cap = StrokeCap.Round)
                    drawLine(iconColor, Offset(fkX - tineSpread, forkNeckY), Offset(fkX + tineSpread, forkNeckY), strokeWidth = 1.35.dp.toPx().s(), cap = StrokeCap.Round)

                    // Right: Oval Spoon in warm golden-sand (#CCA876)
                    drawOval(
                        color = iconColor,
                        topLeft = Offset(spX - spoonW * 0.5f, topY),
                        size = Size(spoonW, spoonBowlH)
                    )
                    drawLine(iconColor, Offset(spX, topY + spoonBowlH * 0.75f), Offset(spX, botY), strokeWidth = 1.55.dp.toPx().s(), cap = StrokeCap.Round)
                }
            }
            VectorPoiCategory.HOTEL_LODGING -> {
                // Flat OSM lodging bed icon with tight contour halo
                val w = 9.4.dp.toPx().s()
                drawLine(haloColor, Offset(cx - w * 0.5f, cy - 3.0.dp.toPx().s()), Offset(cx - w * 0.5f, cy + 3.2.dp.toPx().s()), strokeWidth = 1.6.dp.toPx().s() + haloExtra, cap = StrokeCap.Round)
                drawLine(haloColor, Offset(cx - w * 0.5f, cy + 1.1.dp.toPx().s()), Offset(cx + w * 0.5f, cy + 1.1.dp.toPx().s()), strokeWidth = 1.7.dp.toPx().s() + haloExtra, cap = StrokeCap.Round)
                drawLine(haloColor, Offset(cx + w * 0.5f, cy - 0.4.dp.toPx().s()), Offset(cx + w * 0.5f, cy + 3.2.dp.toPx().s()), strokeWidth = 1.6.dp.toPx().s() + haloExtra, cap = StrokeCap.Round)
                drawCircle(haloColor, radius = 1.4.dp.toPx().s() + haloExtra * 0.5f, center = Offset(cx - w * 0.22f, cy - 1.1.dp.toPx().s()))
                drawRoundRect(
                    color = haloColor,
                    topLeft = Offset(cx - 0.2.dp.toPx().s() - haloExtra * 0.5f, cy - 2.0.dp.toPx().s() - haloExtra * 0.5f),
                    size = Size(w * 0.48f + haloExtra, 2.6.dp.toPx().s() + haloExtra),
                    cornerRadius = CornerRadius(1.2.dp.toPx().s(), 1.2.dp.toPx().s())
                )

                drawLine(iconColor, Offset(cx - w * 0.5f, cy - 3.0.dp.toPx().s()), Offset(cx - w * 0.5f, cy + 3.2.dp.toPx().s()), strokeWidth = 1.6.dp.toPx().s(), cap = StrokeCap.Round)
                drawLine(iconColor, Offset(cx - w * 0.5f, cy + 1.1.dp.toPx().s()), Offset(cx + w * 0.5f, cy + 1.1.dp.toPx().s()), strokeWidth = 1.7.dp.toPx().s(), cap = StrokeCap.Round)
                drawLine(iconColor, Offset(cx + w * 0.5f, cy - 0.4.dp.toPx().s()), Offset(cx + w * 0.5f, cy + 3.2.dp.toPx().s()), strokeWidth = 1.6.dp.toPx().s(), cap = StrokeCap.Round)
                drawCircle(iconColor, radius = 1.4.dp.toPx().s(), center = Offset(cx - w * 0.22f, cy - 1.1.dp.toPx().s()))
                drawRoundRect(
                    color = iconColor,
                    topLeft = Offset(cx - 0.2.dp.toPx().s(), cy - 2.0.dp.toPx().s()),
                    size = Size(w * 0.48f, 2.6.dp.toPx().s()),
                    cornerRadius = CornerRadius(1.1.dp.toPx().s(), 1.1.dp.toPx().s())
                )
            }
            VectorPoiCategory.WORSHIP -> {
                // Flat OSM church icon (chapel base + top cross) with tight contour halo
                val baseW = 6.8.dp.toPx().s()
                val baseH = 4.2.dp.toPx().s()
                drawRoundRect(
                    color = haloColor,
                    topLeft = Offset(cx - baseW * 0.5f - haloExtra * 0.5f, cy + 0.2.dp.toPx().s() - haloExtra * 0.5f),
                    size = Size(baseW + haloExtra, baseH + haloExtra),
                    cornerRadius = CornerRadius(1.0.dp.toPx().s(), 1.0.dp.toPx().s())
                )
                drawLine(haloColor, Offset(cx, cy - 4.8.dp.toPx().s()), Offset(cx, cy + 1.2.dp.toPx().s()), strokeWidth = 1.7.dp.toPx().s() + haloExtra, cap = StrokeCap.Round)
                drawLine(haloColor, Offset(cx - 2.6.dp.toPx().s(), cy - 2.4.dp.toPx().s()), Offset(cx + 2.6.dp.toPx().s(), cy - 2.4.dp.toPx().s()), strokeWidth = 1.7.dp.toPx().s() + haloExtra, cap = StrokeCap.Round)

                drawRoundRect(
                    color = iconColor,
                    topLeft = Offset(cx - baseW * 0.5f, cy + 0.2.dp.toPx().s()),
                    size = Size(baseW, baseH),
                    cornerRadius = CornerRadius(0.8.dp.toPx().s(), 0.8.dp.toPx().s())
                )
                drawLine(iconColor, Offset(cx, cy - 4.8.dp.toPx().s()), Offset(cx, cy + 1.2.dp.toPx().s()), strokeWidth = 1.7.dp.toPx().s(), cap = StrokeCap.Round)
                drawLine(iconColor, Offset(cx - 2.6.dp.toPx().s(), cy - 2.4.dp.toPx().s()), Offset(cx + 2.6.dp.toPx().s(), cy - 2.4.dp.toPx().s()), strokeWidth = 1.7.dp.toPx().s(), cap = StrokeCap.Round)
            }
            VectorPoiCategory.SHOP -> {
                // Flat OSM supermarket shopping cart icon with tight contour halo
                val basketW = 7.4.dp.toPx().s()
                val basketH = 4.4.dp.toPx().s()
                drawRoundRect(
                    color = haloColor,
                    topLeft = Offset(cx - basketW * 0.42f, cy - 2.4.dp.toPx().s()),
                    size = Size(basketW, basketH),
                    cornerRadius = CornerRadius(1.0.dp.toPx().s(), 1.0.dp.toPx().s()),
                    style = Stroke(width = 1.45.dp.toPx().s() + haloExtra)
                )
                drawLine(haloColor, Offset(cx - basketW * 0.65f, cy - 3.6.dp.toPx().s()), Offset(cx - basketW * 0.42f, cy - 2.4.dp.toPx().s()), strokeWidth = 1.5.dp.toPx().s() + haloExtra, cap = StrokeCap.Round)
                drawCircle(haloColor, radius = 1.25.dp.toPx().s() + haloExtra * 0.5f, center = Offset(cx - 1.8.dp.toPx().s(), cy + 3.6.dp.toPx().s()))
                drawCircle(haloColor, radius = 1.25.dp.toPx().s() + haloExtra * 0.5f, center = Offset(cx + 2.4.dp.toPx().s(), cy + 3.6.dp.toPx().s()))

                drawRoundRect(
                    color = iconColor,
                    topLeft = Offset(cx - basketW * 0.42f, cy - 2.4.dp.toPx().s()),
                    size = Size(basketW, basketH),
                    cornerRadius = CornerRadius(1.0.dp.toPx().s(), 1.0.dp.toPx().s()),
                    style = Stroke(width = 1.45.dp.toPx().s())
                )
                drawLine(iconColor, Offset(cx - basketW * 0.65f, cy - 3.6.dp.toPx().s()), Offset(cx - basketW * 0.42f, cy - 2.4.dp.toPx().s()), strokeWidth = 1.5.dp.toPx().s(), cap = StrokeCap.Round)
                drawCircle(iconColor, radius = 1.25.dp.toPx().s(), center = Offset(cx - 1.8.dp.toPx().s(), cy + 3.6.dp.toPx().s()))
                drawCircle(iconColor, radius = 1.25.dp.toPx().s(), center = Offset(cx + 2.4.dp.toPx().s(), cy + 3.6.dp.toPx().s()))
            }
            VectorPoiCategory.PHARMACY, VectorPoiCategory.MEDICAL -> {
                val arm = 4.2.dp.toPx().s()
                drawLine(haloColor, Offset(cx, cy - arm), Offset(cx, cy + arm), strokeWidth = 2.2.dp.toPx().s() + haloExtra, cap = StrokeCap.Round)
                drawLine(haloColor, Offset(cx - arm, cy), Offset(cx + arm, cy), strokeWidth = 2.2.dp.toPx().s() + haloExtra, cap = StrokeCap.Round)
                drawLine(iconColor, Offset(cx, cy - arm), Offset(cx, cy + arm), strokeWidth = 2.2.dp.toPx().s(), cap = StrokeCap.Round)
                drawLine(iconColor, Offset(cx - arm, cy), Offset(cx + arm, cy), strokeWidth = 2.2.dp.toPx().s(), cap = StrokeCap.Round)
            }
            VectorPoiCategory.OFFICE_COMPANY -> {
                val bw = 7.8.dp.toPx().s()
                val bh = 5.4.dp.toPx().s()
                val hw = 3.2.dp.toPx().s()
                val hh = 1.7.dp.toPx().s()
                drawRoundRect(
                    color = haloColor,
                    topLeft = Offset(cx - bw * 0.5f - haloExtra * 0.5f, cy - bh * 0.35f - haloExtra * 0.5f),
                    size = Size(bw + haloExtra, bh + haloExtra),
                    cornerRadius = CornerRadius(1.4.dp.toPx().s(), 1.4.dp.toPx().s())
                )
                drawRoundRect(
                    color = haloColor,
                    topLeft = Offset(cx - hw * 0.5f, cy - bh * 0.35f - hh),
                    size = Size(hw, hh + 0.5.dp.toPx().s()),
                    cornerRadius = CornerRadius(0.7.dp.toPx().s(), 0.7.dp.toPx().s()),
                    style = Stroke(width = 1.2.dp.toPx().s() + haloExtra)
                )
                drawRoundRect(
                    color = iconColor,
                    topLeft = Offset(cx - bw * 0.5f, cy - bh * 0.35f),
                    size = Size(bw, bh),
                    cornerRadius = CornerRadius(1.2.dp.toPx().s(), 1.2.dp.toPx().s())
                )
                drawRoundRect(
                    color = iconColor,
                    topLeft = Offset(cx - hw * 0.5f, cy - bh * 0.35f - hh),
                    size = Size(hw, hh + 0.5.dp.toPx().s()),
                    cornerRadius = CornerRadius(0.7.dp.toPx().s(), 0.7.dp.toPx().s()),
                    style = Stroke(width = 1.2.dp.toPx().s())
                )
                drawLine(
                    color = haloColor,
                    start = Offset(cx - bw * 0.5f, cy + 0.3.dp.toPx().s()),
                    end = Offset(cx + bw * 0.5f, cy + 0.3.dp.toPx().s()),
                    strokeWidth = 1.0.dp.toPx().s()
                )
            }
            VectorPoiCategory.LANDMARK_TOURISM -> {
                val camW = 8.8.dp.toPx().s()
                val camH = 6.0.dp.toPx().s()
                drawRoundRect(
                    color = haloColor,
                    topLeft = Offset(cx - camW * 0.5f - haloExtra * 0.5f, cy - camH * 0.42f - haloExtra * 0.5f),
                    size = Size(camW + haloExtra, camH + haloExtra),
                    cornerRadius = CornerRadius(1.6.dp.toPx().s(), 1.6.dp.toPx().s())
                )
                drawRoundRect(
                    color = iconColor,
                    topLeft = Offset(cx - camW * 0.5f, cy - camH * 0.42f),
                    size = Size(camW, camH),
                    cornerRadius = CornerRadius(1.4.dp.toPx().s(), 1.4.dp.toPx().s())
                )
                drawRoundRect(
                    color = iconColor,
                    topLeft = Offset(cx - 2.2.dp.toPx().s(), cy - camH * 0.42f - 1.4.dp.toPx().s()),
                    size = Size(4.4.dp.toPx().s(), 2.0.dp.toPx().s()),
                    cornerRadius = CornerRadius(0.7.dp.toPx().s(), 0.7.dp.toPx().s())
                )
                drawCircle(
                    color = haloColor,
                    radius = 1.9.dp.toPx().s(),
                    center = Offset(cx, cy + 0.3.dp.toPx().s())
                )
            }
            VectorPoiCategory.CULTURE_CINEMA -> {
                val w = 8.2.dp.toPx().s()
                val h = 6.6.dp.toPx().s()
                drawRoundRect(
                    color = haloColor,
                    topLeft = Offset(cx - w * 0.5f, cy - h * 0.5f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(1.2.dp.toPx().s(), 1.2.dp.toPx().s()),
                    style = Stroke(width = 1.5.dp.toPx().s() + haloExtra)
                )
                drawRoundRect(
                    color = iconColor,
                    topLeft = Offset(cx - w * 0.5f, cy - h * 0.5f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(1.2.dp.toPx().s(), 1.2.dp.toPx().s()),
                    style = Stroke(width = 1.5.dp.toPx().s())
                )
                drawLine(iconColor, Offset(cx - w * 0.5f, cy), Offset(cx + w * 0.5f, cy), strokeWidth = 1.3.dp.toPx().s())
            }
            VectorPoiCategory.SPORTS_SWIMMING -> {
                drawCircle(haloColor, radius = 1.5.dp.toPx().s() + haloExtra * 0.5f, center = Offset(cx - 0.5.dp.toPx().s(), cy - 3.5.dp.toPx().s()))
                drawLine(haloColor, Offset(cx - 0.8.dp.toPx().s(), cy - 1.6.dp.toPx().s()), Offset(cx - 1.1.dp.toPx().s(), cy + 1.3.dp.toPx().s()), strokeWidth = 1.6.dp.toPx().s() + haloExtra, cap = StrokeCap.Round)
                drawCircle(iconColor, radius = 1.5.dp.toPx().s(), center = Offset(cx - 0.5.dp.toPx().s(), cy - 3.5.dp.toPx().s()))
                drawLine(iconColor, Offset(cx - 0.8.dp.toPx().s(), cy - 1.6.dp.toPx().s()), Offset(cx - 1.1.dp.toPx().s(), cy + 1.3.dp.toPx().s()), strokeWidth = 1.6.dp.toPx().s(), cap = StrokeCap.Round)
                drawLine(iconColor, Offset(cx - 1.1.dp.toPx().s(), cy + 1.3.dp.toPx().s()), Offset(cx - 2.8.dp.toPx().s(), cy + 4.2.dp.toPx().s()), strokeWidth = 1.4.dp.toPx().s(), cap = StrokeCap.Round)
                drawLine(iconColor, Offset(cx - 1.1.dp.toPx().s(), cy + 1.3.dp.toPx().s()), Offset(cx + 1.1.dp.toPx().s(), cy + 4.2.dp.toPx().s()), strokeWidth = 1.4.dp.toPx().s(), cap = StrokeCap.Round)
            }
            else -> {
                drawCircle(haloColor, radius = 3.2.dp.toPx().s() + haloExtra * 0.5f, center = center)
                drawCircle(iconColor, radius = 3.2.dp.toPx().s(), center = center)
            }
        }
    }

    private fun DrawScope.drawVectorBenchIcon(center: Offset, alpha: Float) {
        val halo = Color(0xFF141A24).copy(alpha = alpha)
        val seat = Color(0xFFD0DCEB).copy(alpha = alpha)
        val w = 13.5.dp.toPx()
        val legH = 4.8.dp.toPx()

        drawLine(halo, Offset(center.x - w / 2f, center.y), Offset(center.x + w / 2f, center.y), strokeWidth = 4.0.dp.toPx(), cap = StrokeCap.Round)
        drawLine(seat, Offset(center.x - w / 2f, center.y), Offset(center.x + w / 2f, center.y), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(seat, Offset(center.x - w * 0.32f, center.y), Offset(center.x - w * 0.32f, center.y + legH), strokeWidth = 2.0.dp.toPx(), cap = StrokeCap.Round)
        drawLine(seat, Offset(center.x + w * 0.32f, center.y), Offset(center.x + w * 0.32f, center.y + legH), strokeWidth = 2.0.dp.toPx(), cap = StrokeCap.Round)
    }

    private fun DrawScope.drawVectorWasteBasketIcon(center: Offset, alpha: Float) {
        val halo = Color(0xFF141A24).copy(alpha = alpha)
        val binColor = Color(0xFFD0DCEB).copy(alpha = alpha)
        val topW = 9.0.dp.toPx()
        val botW = 6.5.dp.toPx()
        val h = 9.5.dp.toPx()

        val binPath = reusableBinPath.apply {
            reset()
            moveTo(center.x - topW / 2f, center.y - h / 2f + 1.5.dp.toPx())
            lineTo(center.x + topW / 2f, center.y - h / 2f + 1.5.dp.toPx())
            lineTo(center.x + botW / 2f, center.y + h / 2f)
            lineTo(center.x - botW / 2f, center.y + h / 2f)
            close()
        }
        drawPath(binPath, color = halo, style = Stroke(width = 2.8.dp.toPx(), join = StrokeJoin.Round))
        drawPath(binPath, color = binColor, style = Fill)
        drawLine(
            color = binColor,
            start = Offset(center.x - topW / 2f - 1.dp.toPx(), center.y - h / 2f),
            end = Offset(center.x + topW / 2f + 1.dp.toPx(), center.y - h / 2f),
            strokeWidth = 2.0.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    private fun DrawScope.drawForestTreeSymbolsForArea(
        area: VectorAreaPoly,
        centerLat: Double,
        centerLng: Double,
        zoom: Float,
        viewportCenterX: Float,
        viewportCenterY: Float,
        overlayAlpha: Float
    ) {
        val safeCenterLat = centerLat.coerceIn(-85.0511, 85.0511)
        val cosCenterLat = cos(safeCenterLat * PI / 180.0).coerceAtLeast(0.01)
        val worldSizePx = 256.0 * 2.0.pow(zoom.toDouble())
        val pxPerDegLng = worldSizePx / 360.0
        val pxPerDegLat = (worldSizePx / 360.0) / cosCenterLat

        val viewHalfDegLng = ((size.width * 0.55) / pxPerDegLng).coerceAtLeast(0.0002)
        val viewHalfDegLat = ((size.height * 0.55) / pxPerDegLat).coerceAtLeast(0.0002)
        val viewSouth = safeCenterLat - viewHalfDegLat
        val viewNorth = safeCenterLat + viewHalfDegLat
        val viewWest = centerLng - viewHalfDegLng
        val viewEast = centerLng + viewHalfDegLng

        val stepDeg = 0.00045
        val rawMinLat = (area.lats.minOrNull() ?: return).coerceAtLeast(viewSouth)
        val rawMaxLat = (area.lats.maxOrNull() ?: return).coerceAtMost(viewNorth)
        val rawMinLng = (area.lngs.minOrNull() ?: return).coerceAtLeast(viewWest)
        val rawMaxLng = (area.lngs.maxOrNull() ?: return).coerceAtMost(viewEast)
        if (rawMinLat > rawMaxLat || rawMinLng > rawMaxLng) return

        var la = floor(rawMinLat / stepDeg) * stepDeg
        var row = 0
        var checks = 0
        while (la <= rawMaxLat && row < 12 && checks < 64) {
            var ln = floor(rawMinLng / stepDeg) * stepDeg + (if (row % 2 == 1) stepDeg * 0.5 else 0.0)
            var col = 0
            while (ln <= rawMaxLng && col < 12 && checks < 64) {
                val tPt = latLngToScreenPx(
                    lat = la,
                    lng = ln,
                    centerLat = centerLat,
                    centerLng = centerLng,
                    zoom = zoom,
                    viewportCenterX = viewportCenterX,
                    viewportCenterY = viewportCenterY
                )
                if (tPt.x in 16f..(size.width - 16f) && tPt.y in 16f..(size.height - 16f)) {
                    checks++
                    if (pointInPolygon(la, ln, area.lats, area.lngs)) {
                        drawVectorForestTreeGlyph(tPt, overlayAlpha)
                    }
                }
                ln += stepDeg
                col++
            }
            la += stepDeg
            row++
        }
    }

    /**
     * Crisp vector dual-tree forest glyph matching the OpenStreetMap forest icon in Screenshot 2.
     */
    private fun DrawScope.drawVectorForestTreeGlyph(center: Offset, alpha: Float) {
        val halo = Color(0xFF12221E).copy(alpha = alpha * 0.9f)
        val treeStroke = Color(0xFF5AB896).copy(alpha = alpha * 0.92f)
        val s = 1.6.dp.toPx()

        // Left deciduous round-canopy tree + trunk
        val leftX = center.x - 4.2.dp.toPx()
        drawCircle(color = halo, radius = 4.6.dp.toPx(), center = Offset(leftX, center.y - 2.dp.toPx()))
        drawCircle(color = treeStroke, radius = 3.6.dp.toPx(), center = Offset(leftX, center.y - 2.dp.toPx()), style = Stroke(width = s))
        drawLine(treeStroke, Offset(leftX, center.y + 1.6.dp.toPx()), Offset(leftX, center.y + 6.dp.toPx()), strokeWidth = s, cap = StrokeCap.Round)

        // Right conifer triangle tree + trunk
        val rightX = center.x + 4.2.dp.toPx()
        val conifer = reusableConiferPath.apply {
            reset()
            moveTo(rightX, center.y - 6.8.dp.toPx())
            lineTo(rightX - 3.8.dp.toPx(), center.y + 1.5.dp.toPx())
            lineTo(rightX + 3.8.dp.toPx(), center.y + 1.5.dp.toPx())
            close()
        }
        drawPath(conifer, color = halo, style = Stroke(width = s + 1.4.dp.toPx(), join = StrokeJoin.Round))
        drawPath(conifer, color = treeStroke, style = Stroke(width = s, join = StrokeJoin.Round))
        drawLine(treeStroke, Offset(rightX, center.y + 1.5.dp.toPx()), Offset(rightX, center.y + 6.dp.toPx()), strokeWidth = s, cap = StrokeCap.Round)
    }
}
