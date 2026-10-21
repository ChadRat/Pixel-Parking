package com.example.util

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.regex.Pattern

data class ParsedCoordinates(
    val latitude: Double,
    val longitude: Double,
    val label: String? = null
)

object CoordinateParser {

    private val DMS_REGEX = Pattern.compile(
        """(\d{1,2})[°\s]+(\d{1,2})['\s]+(\d{1,2}(?:\.\d+)?)["\s]*([NSns])\s*[,;\s]\s*(\d{1,3})[°\s]+(\d{1,2})['\s]+(\d{1,2}(?:\.\d+)?)["\s]*([EWew])"""
    )

    // Standard decimal coordinates pattern: matches lat, lng optionally preceded/followed by other text
    private val DECIMAL_REGEX = Pattern.compile(
        """([-+]?\d{1,2}(?:\.\d+)?)\s*[,;\s]\s*([-+]?\d{1,3}(?:\.\d+)?)"""
    )

    // Exact Google Maps place pin coordinates in data protobuf parameter (!3d<lat>!4d<lng>)
    private val GOOGLE_DATA_COORDS_REGEX = Pattern.compile(
        """(?:!3d|%213d)([-+]?\d{1,2}(?:\.\d+)?)(?:!4d|%214d)([-+]?\d{1,3}(?:\.\d+)?)""",
        Pattern.CASE_INSENSITIVE
    )

    // Google Maps viewport/camera anchor (@lat,lng)
    private val AT_COORDS_REGEX = Pattern.compile(
        """@([-+]?\d{1,2}(?:\.\d+)?),([-+]?\d{1,3}(?:\.\d+)?)"""
    )

    // Google Maps place path coordinates (e.g. /place/lat,lng or /place/Name/lat,lng)
    private val PLACE_COORDS_REGEX = Pattern.compile(
        """place/(?:[^/@?#]+/)?([-+]?\d{1,2}(?:\.\d+)?)[,+]([-+]?\d{1,3}(?:\.\d+)?)"""
    )

    // Query parameters: q, query, destination, daddr, saddr, ll, center
    private val PARAM_COORDS_REGEX = Pattern.compile(
        """(?:[?&](?:q|query|destination|daddr|saddr|ll|center)=)(?:loc:)?([-+]?\d{1,2}(?:\.\d+)?)[,+]([-+]?\d{1,3}(?:\.\d+)?)""",
        Pattern.CASE_INSENSITIVE
    )

    // Direction destination path (e.g. /dir//lat,lng or /dir/Origin/lat,lng)
    private val DIR_PATH_COORDS_REGEX = Pattern.compile(
        """dir/[^/]*/([-+]?\d{1,2}(?:\.\d+)?)[,+]([-+]?\d{1,3}(?:\.\d+)?)"""
    )

    // Google Maps Place Name in path (e.g. /place/Place+Name/@...)
    private val PLACE_NAME_REGEX = Pattern.compile(
        """place/([^/@?#]+)"""
    )

    /**
     * Synchronous coordinate parser.
     * Parses standard decimal, DMS, geo: URIs, and full URLs that already contain coordinates.
     */
    fun parse(input: String?): ParsedCoordinates? {
        if (input.isNullOrBlank()) return null
        val trimmed = input.trim()
        val decodedInput = try {
            URLDecoder.decode(trimmed, "UTF-8")
        } catch (_: Exception) {
            trimmed
        }

        // 1. Try geo: URI
        if (decodedInput.contains("geo:", ignoreCase = true)) {
            val parsedGeo = parseGeoUri(decodedInput)
            if (parsedGeo != null) return parsedGeo
        }

        // 2. Try Google Maps / URL patterns
        if (decodedInput.contains("http://", ignoreCase = true) || decodedInput.contains("https://", ignoreCase = true)) {
            val parsedUrl = parseMapUrl(decodedInput)
            if (parsedUrl != null) return parsedUrl
        }

        // 3. Try DMS format (e.g. 37°46'29.6"N 122°25'09.8"W)
        val dmsMatch = DMS_REGEX.matcher(decodedInput)
        if (dmsMatch.find()) {
            try {
                val latDeg = dmsMatch.group(1)!!.toDouble()
                val latMin = dmsMatch.group(2)!!.toDouble()
                val latSec = dmsMatch.group(3)!!.toDouble()
                val latHem = dmsMatch.group(4)!!
                var lat = latDeg + (latMin / 60.0) + (latSec / 3600.0)
                if (latHem.equals("S", ignoreCase = true)) lat = -lat

                val lngDeg = dmsMatch.group(5)!!.toDouble()
                val lngMin = dmsMatch.group(6)!!.toDouble()
                val lngSec = dmsMatch.group(7)!!.toDouble()
                val lngHem = dmsMatch.group(8)!!
                var lng = lngDeg + (lngMin / 60.0) + (lngSec / 3600.0)
                if (lngHem.equals("W", ignoreCase = true)) lng = -lng

                if (isValidLatLng(lat, lng)) {
                    val label = extractLabel(decodedInput)
                    return ParsedCoordinates(lat, lng, label)
                }
            } catch (_: Exception) {}
        }

        // 4. Try standard decimal coordinates anywhere in string (avoid matching dates or small zoom numbers)
        val decimalMatch = DECIMAL_REGEX.matcher(decodedInput)
        while (decimalMatch.find()) {
            val latStr = decimalMatch.group(1)
            val lngStr = decimalMatch.group(2)
            try {
                val lat = latStr!!.toDouble()
                val lng = lngStr!!.toDouble()
                if (isValidLatLng(lat, lng)) {
                    val label = extractLabel(decodedInput)
                    return ParsedCoordinates(lat, lng, label)
                }
            } catch (_: Exception) {}
        }

        return null
    }

    /**
     * Asynchronous coordinate parser.
     * Resolves HTTP redirects for short URLs (such as maps.app.goo.gl and goo.gl/maps)
     * and extracts coordinates from the resolved URL or text payload.
     */
    suspend fun parseSuspend(input: String?): ParsedCoordinates? = withContext(Dispatchers.IO) {
        if (input.isNullOrBlank()) return@withContext null

        // 1. Fast path: try synchronous parsing first
        val immediate = parse(input)
        if (immediate != null) return@withContext immediate

        // 2. Extract potential URL from the input text
        val urlCandidate = extractUrl(input) ?: return@withContext null

        // Extract any leading or accompanying place label from the shared text
        val textLabel = extractTextLabel(input, urlCandidate)

        // 3. Follow HTTP redirects to unwrap shortened URLs (e.g. maps.app.goo.gl -> google.com/maps/place/...)
        val resolvedUrl = resolveRedirects(urlCandidate)
        val parsedFromResolved = parseMapUrl(resolvedUrl)
        if (parsedFromResolved != null) {
            return@withContext if (parsedFromResolved.label.isNullOrBlank() && !textLabel.isNullOrBlank()) {
                parsedFromResolved.copy(label = textLabel)
            } else {
                parsedFromResolved
            }
        }

        // 4. Fall back to parsing the resolved URL with full parser
        val fallback = parse(resolvedUrl)
        if (fallback != null) {
            return@withContext if (fallback.label.isNullOrBlank() && !textLabel.isNullOrBlank()) {
                fallback.copy(label = textLabel)
            } else {
                fallback
            }
        }

        null
    }

    private fun extractUrl(input: String): String? {
        val pattern = Pattern.compile("""https?://[^\s<>'"()]+""")
        val matcher = pattern.matcher(input)
        if (matcher.find()) {
            var url = matcher.group(0)!!
            // Clean trailing punctuation
            while (url.isNotEmpty() && (url.endsWith(".") || url.endsWith(",") || url.endsWith(")") || url.endsWith(";"))) {
                url = url.substring(0, url.length - 1)
            }
            return url.takeIf { it.isNotBlank() }
        }
        return null
    }

    private fun extractTextLabel(fullText: String, url: String): String? {
        try {
            val textWithoutUrl = fullText.replace(url, "").trim()
            val lines = textWithoutUrl.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (lines.isNotEmpty()) {
                val candidate = lines.first()
                if (!candidate.contains("http", ignoreCase = true) && candidate.length in 2..60) {
                    // Filter out generic phrases like "Dropped pin" if better line available
                    if (candidate.equals("Dropped pin", ignoreCase = true) && lines.size > 1) {
                        val second = lines[1]
                        if (second.length in 2..60) return second
                    }
                    return candidate
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun resolveRedirects(initialUrl: String): String {
        var currentUrl = initialUrl
        var hops = 0
        val maxHops = 6

        while (hops < maxHops) {
            hops++
            var conn: HttpURLConnection? = null
            try {
                val u = URL(currentUrl)
                conn = u.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.requestMethod = "GET"
                conn.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                )
                conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")

                val code = conn.responseCode
                if (code in 300..399) {
                    val location = conn.getHeaderField("Location")
                    if (!location.isNullOrBlank()) {
                        val nextUrl = if (location.startsWith("http://") || location.startsWith("https://")) {
                            location
                        } else {
                            URL(u, location).toString()
                        }
                        currentUrl = nextUrl

                        // If the redirect URL already contains coordinates, return early!
                        if (currentUrl.contains("@") || currentUrl.contains("!3d") || currentUrl.contains("q=")) {
                            return currentUrl
                        }
                        continue
                    }
                } else if (code == 200) {
                    // Check if connected URL redirected automatically
                    val finalUrl = conn.url.toString()
                    if (finalUrl != currentUrl && (finalUrl.contains("@") || finalUrl.contains("!3d") || finalUrl.contains("q="))) {
                        return finalUrl
                    }

                    // Read first 2KB of response to check for meta refresh or canonical tag
                    val reader = conn.inputStream.bufferedReader()
                    val sb = StringBuilder()
                    val buf = CharArray(1024)
                    var read = reader.read(buf, 0, 1024)
                    if (read > 0) sb.append(buf, 0, read)
                    read = reader.read(buf, 0, 1024)
                    if (read > 0) sb.append(buf, 0, read)
                    val body = sb.toString()

                    val metaMatch = Pattern.compile("""url=([^\s"'>]+)""", Pattern.CASE_INSENSITIVE).matcher(body)
                    if (metaMatch.find()) {
                        val metaUrl = metaMatch.group(1)!!
                        return if (metaUrl.startsWith("http")) metaUrl else URL(u, metaUrl).toString()
                    }

                    return finalUrl
                }
                break
            } catch (_: Exception) {
                break
            } finally {
                conn?.disconnect()
            }
        }
        return currentUrl
    }

    private fun parseGeoUri(input: String): ParsedCoordinates? {
        try {
            val geoIndex = input.indexOf("geo:", ignoreCase = true)
            val uriStr = if (geoIndex >= 0) input.substring(geoIndex).split(Regex("\\s+"))[0] else input
            val uri = Uri.parse(uriStr)

            // Check query param 'q' (e.g. geo:0,0?q=37.7749,-122.4194(Label) or geo:0,0?q=loc:37.7749,-122.4194)
            val q = uri.getQueryParameter("q")
            if (!q.isNullOrBlank()) {
                val label = if (q.contains("(") && q.contains(")")) {
                    q.substringAfter("(").substringBefore(")")
                } else null
                var coordsPart = if (q.contains("(")) q.substringBefore("(") else q
                if (coordsPart.startsWith("loc:", ignoreCase = true)) {
                    coordsPart = coordsPart.substring(4)
                }
                val parts = coordsPart.split(",")
                if (parts.size >= 2) {
                    val lat = parts[0].trim().toDoubleOrNull()
                    val lng = parts[1].trim().toDoubleOrNull()
                    if (lat != null && lng != null && isValidLatLng(lat, lng)) {
                        return ParsedCoordinates(lat, lng, label?.takeIf { it.isNotBlank() })
                    }
                }
            }

            // Check scheme specific part before '?' (e.g. geo:37.7749,-122.4194)
            val ssp = uri.schemeSpecificPart.substringBefore("?")
            val parts = ssp.split(",")
            if (parts.size >= 2) {
                val lat = parts[0].trim().toDoubleOrNull()
                val lng = parts[1].trim().toDoubleOrNull()
                if (lat != null && lng != null && isValidLatLng(lat, lng) && (lat != 0.0 || lng != 0.0)) {
                    return ParsedCoordinates(lat, lng)
                }
            }
        } catch (_: Exception) {}
        return null
    }

    fun parseMapUrl(input: String): ParsedCoordinates? {
        try {
            val urlStart = input.indexOf("http", ignoreCase = true)
            if (urlStart < 0) return null
            val urlStr = input.substring(urlStart).split(Regex("\\s+"))[0]

            // 1. Prioritize precise Google pin coordinates (!3d<lat>!4d<lng>)
            val dataMatch = GOOGLE_DATA_COORDS_REGEX.matcher(urlStr)
            if (dataMatch.find()) {
                val lat = dataMatch.group(1)!!.toDoubleOrNull()
                val lng = dataMatch.group(2)!!.toDoubleOrNull()
                if (lat != null && lng != null && isValidLatLng(lat, lng)) {
                    val label = extractPlaceNameFromUrl(urlStr)
                    return ParsedCoordinates(lat, lng, label)
                }
            }

            // 2. Check query parameter matches (e.g. ?q=37.7749,-122.4194 or destination=37.7749,-122.4194)
            val paramMatch = PARAM_COORDS_REGEX.matcher(urlStr)
            if (paramMatch.find()) {
                val lat = paramMatch.group(1)!!.toDoubleOrNull()
                val lng = paramMatch.group(2)!!.toDoubleOrNull()
                if (lat != null && lng != null && isValidLatLng(lat, lng)) {
                    val label = extractPlaceNameFromUrl(urlStr)
                    return ParsedCoordinates(lat, lng, label)
                }
            }

            // 3. Check /@lat,lng pattern (camera anchor)
            val atMatch = AT_COORDS_REGEX.matcher(urlStr)
            if (atMatch.find()) {
                val lat = atMatch.group(1)!!.toDoubleOrNull()
                val lng = atMatch.group(2)!!.toDoubleOrNull()
                if (lat != null && lng != null && isValidLatLng(lat, lng)) {
                    val label = extractPlaceNameFromUrl(urlStr)
                    return ParsedCoordinates(lat, lng, label)
                }
            }

            // 4. Check place/lat,lng path
            val placeMatch = PLACE_COORDS_REGEX.matcher(urlStr)
            if (placeMatch.find()) {
                val lat = placeMatch.group(1)!!.toDoubleOrNull()
                val lng = placeMatch.group(2)!!.toDoubleOrNull()
                if (lat != null && lng != null && isValidLatLng(lat, lng)) {
                    val label = extractPlaceNameFromUrl(urlStr)
                    return ParsedCoordinates(lat, lng, label)
                }
            }

            // 5. Check dir//lat,lng path
            val dirMatch = DIR_PATH_COORDS_REGEX.matcher(urlStr)
            if (dirMatch.find()) {
                val lat = dirMatch.group(1)!!.toDoubleOrNull()
                val lng = dirMatch.group(2)!!.toDoubleOrNull()
                if (lat != null && lng != null && isValidLatLng(lat, lng)) {
                    val label = extractPlaceNameFromUrl(urlStr)
                    return ParsedCoordinates(lat, lng, label)
                }
            }

            // 6. Generic Uri parsing fallback for query
            val uri = Uri.parse(urlStr)
            val q = uri.getQueryParameter("q") ?: uri.getQueryParameter("query")
            if (!q.isNullOrBlank()) {
                val match = DECIMAL_REGEX.matcher(q)
                if (match.find()) {
                    val lat = match.group(1)!!.toDoubleOrNull()
                    val lng = match.group(2)!!.toDoubleOrNull()
                    if (lat != null && lng != null && isValidLatLng(lat, lng)) {
                        val label = extractPlaceNameFromUrl(urlStr)
                        return ParsedCoordinates(lat, lng, label)
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun extractPlaceNameFromUrl(urlStr: String): String? {
        try {
            val match = PLACE_NAME_REGEX.matcher(urlStr)
            if (match.find()) {
                val raw = match.group(1)!!
                // Check that it's not raw coordinates
                if (!raw.matches(Regex("""^[-+]?\d{1,2}(?:\.\d+)?[,+](?:[-+]?\d{1,3}(?:\.\d+)?)$"""))) {
                    val decoded = URLDecoder.decode(raw.replace("+", " "), "UTF-8").trim()
                    if (decoded.isNotBlank() && decoded.length in 2..60) {
                        return decoded
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun extractLabel(input: String): String? {
        if (input.contains("(") && input.contains(")")) {
            val label = input.substringAfter("(").substringBefore(")").trim()
            if (label.isNotBlank() && label.length <= 60) return label
        }
        return null
    }

    fun isValidLatLng(lat: Double, lng: Double): Boolean {
        return lat in -90.0..90.0 && lng in -180.0..180.0 && !(lat == 0.0 && lng == 0.0)
    }
}
