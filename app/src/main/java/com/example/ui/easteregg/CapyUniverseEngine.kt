package com.example.ui.easteregg

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Biome definitions for the 2D Universe road trip.
 */
enum class UniverseBiome(
    val displayName: String,
    val skyTop: Color,
    val skyBottom: Color,
    val horizonColor: Color,
    val groundColor: Color,
    val roadColor: Color,
    val laneMarkColor: Color
) {
    NEO_CITY(
        displayName = "Neo Metropolis",
        skyTop = Color(0xFF0F172A),
        skyBottom = Color(0xFF312E81),
        horizonColor = Color(0xFF1E1B4B),
        groundColor = Color(0xFF1E293B),
        roadColor = Color(0xFF0F172A),
        laneMarkColor = Color(0xFFE2E8F0)
    ),
    EMERALD_FOREST(
        displayName = "Emerald Forest",
        skyTop = Color(0xFF064E3B),
        skyBottom = Color(0xFF047857),
        horizonColor = Color(0xFF065F46),
        groundColor = Color(0xFF064E3B),
        roadColor = Color(0xFF1F2937),
        laneMarkColor = Color(0xFFFDE047)
    ),
    SAPPHIRE_LAKE(
        displayName = "Sapphire Lake",
        skyTop = Color(0xFF0284C7),
        skyBottom = Color(0xFF38BDF8),
        horizonColor = Color(0xFF0369A1),
        groundColor = Color(0xFF0284C7),
        roadColor = Color(0xFF78350F), // Wooden pier / coastal boardwalk
        laneMarkColor = Color(0xFFFEF3C7)
    ),
    LUNAR_MOON(
        displayName = "The Moon",
        skyTop = Color(0xFF020617),
        skyBottom = Color(0xFF0F172A),
        horizonColor = Color(0xFF1E293B),
        groundColor = Color(0xFF334155),
        roadColor = Color(0xFF1E293B),
        laneMarkColor = Color(0xFF38BDF8) // Glowing celestial cyan guide
    ),
    MARS_DUNES(
        displayName = "Mars Dunes",
        skyTop = Color(0xFF451A03),
        skyBottom = Color(0xFF9A3412),
        horizonColor = Color(0xFF7C2D12),
        groundColor = Color(0xFF7C2D12),
        roadColor = Color(0xFF431407),
        laneMarkColor = Color(0xFFFB923C) // Glowing Martian solar strip
    )
}

/**
 * Collectible treat along the road.
 */
data class RoadCollectible(
    val id: Int,
    val worldX: Float,
    val yOffset: Float,
    val isYuzu: Boolean,
    var collected: Boolean = false
)

/**
 * 2D Universe engine managing procedural scenery, smooth biome morphing,
 * touch-based speed dynamics, spinning wheel math, and collision detection.
 * All background layers scroll to the LEFT (opposite direction of the right-facing car),
 * creating an ultra-smooth motion parallax illusion that directly corresponds to car speed.
 */
class CapyUniverseEngine {

    var totalDistanceMeters: Float = 0f
    var currentSpeedKmh: Float = 0f
    var targetSpeedKmh: Float = 0f
    var wheelAngle: Float = 0f
    var treatsCollected: Int = 0
    var isTurboActive: Boolean = false
    var currentBiomeName: String = UniverseBiome.NEO_CITY.displayName

    // Collectibles & speed boosts
    private val collectibles = mutableListOf<RoadCollectible>()
    private var nextCollectibleId = 1
    private var lastGeneratedWorldX = 0f

    // Stars for space biomes
    private val starX = FloatArray(60) { (it * 37f) % 1000f }
    private val starY = FloatArray(60) { ((it * 53f) % 300f) + 20f }
    private val starSize = FloatArray(60) { (it % 3) + 1.5f }

    init {
        // Pre-populate initial collectibles along the first 1000 meters
        generateCollectiblesAhead(500f)
    }

    /**
     * Updates physics, speed inertia, wheel spin, and distance.
     * @param dt Delta time in seconds (typically ~0.016s for 60fps)
     * @param touchX Current touch X coordinate on screen, or null if not holding
     * @param screenWidth Total width of screen in pixels
     */
    fun update(dt: Float, touchX: Float?, screenWidth: Float) {
        val clampedDt = dt.coerceIn(0.001f, 0.05f)

        if (touchX != null && screenWidth > 0f) {
            // Holding screen accelerates car:
            // Center slower (~35 km/h)
            // Edges faster dynamically (up to 220 km/h)
            val relX = (touchX / screenWidth).coerceIn(0f, 1f)
            val centerOffset = abs(relX - 0.5f) * 2f // 0.0 at center, 1.0 at edge

            val isRightSide = relX >= 0.5f
            if (isRightSide) {
                targetSpeedKmh = 35f + centerOffset * 185f
            } else {
                targetSpeedKmh = 35f + centerOffset * 165f
            }
        } else {
            // Not holding screen: car stops / coasts to a halt
            targetSpeedKmh = 0f
        }

        // Inertia smoothing for car acceleration / deceleration
        val accelFactor = if (targetSpeedKmh > currentSpeedKmh) 0.08f else 0.10f
        currentSpeedKmh += (targetSpeedKmh - currentSpeedKmh) * accelFactor
        if (currentSpeedKmh < 0.1f) {
            currentSpeedKmh = 0f
        }

        isTurboActive = currentSpeedKmh > 130f

        // Wheel spin: angular speed = (speed in m/s) / wheelRadius
        // Rotate smoothly in degrees
        wheelAngle = (wheelAngle + currentSpeedKmh * clampedDt * 9.5f) % 360f

        // Accumulate distance in meters (km/h converted to m/s * dt)
        val deltaMeters = (currentSpeedKmh / 3.6f) * clampedDt
        totalDistanceMeters += deltaMeters

        // Procedural generation of collectibles ahead
        if (totalDistanceMeters + 300f > lastGeneratedWorldX) {
            generateCollectiblesAhead(totalDistanceMeters + 600f)
        }

        // Check collectible collision with car (car is positioned at virtual X = totalDistanceMeters)
        val carWorldX = totalDistanceMeters
        collectibles.forEach { item ->
            if (!item.collected && abs(item.worldX - carWorldX) < 3.2f) {
                item.collected = true
                treatsCollected++
            }
        }

        // Clean up far behind collectibles
        collectibles.removeAll { it.worldX < totalDistanceMeters - 200f }
    }

    private fun generateCollectiblesAhead(targetWorldX: Float) {
        var x = if (lastGeneratedWorldX == 0f) 50f else lastGeneratedWorldX + 20f
        while (x < targetWorldX) {
            collectibles.add(
                RoadCollectible(
                    id = nextCollectibleId++,
                    worldX = x,
                    yOffset = if (nextCollectibleId % 2 == 0) -28f else -10f,
                    isYuzu = (nextCollectibleId % 3 != 0)
                )
            )
            x += (15f + (nextCollectibleId * 7f % 18f))
        }
        lastGeneratedWorldX = x
    }

    /**
     * Renders the complete multi-biome universe scenery, road, landmarks, and collectibles.
     */
    fun drawUniverse(
        drawScope: DrawScope,
        screenWidth: Float,
        screenHeight: Float,
        carX: Float,
        carY: Float,
        carWidth: Float,
        carHeight: Float,
        selectedCar: CapyCarType
    ) {
        with(drawScope) {
            val biomes = UniverseBiome.entries
            val biomeDistanceLength = 300f // 300 meters per biome
            val totalLoopLength = biomeDistanceLength * biomes.size

            val currentCycleProgress = (totalDistanceMeters % totalLoopLength) / biomeDistanceLength
            val currentBiomeIndex = currentCycleProgress.toInt() % biomes.size
            val nextBiomeIndex = (currentBiomeIndex + 1) % biomes.size
            val rawTransition = currentCycleProgress - currentBiomeIndex // 0.0 to 1.0

            // Smooth transition in the last 30% of each biome
            val blendFactor = if (rawTransition >= 0.70f) {
                ((rawTransition - 0.70f) / 0.30f).coerceIn(0f, 1f)
            } else {
                0f
            }

            val curBiome = biomes[currentBiomeIndex]
            val nxtBiome = biomes[nextBiomeIndex]

            currentBiomeName = if (blendFactor > 0.5f) nxtBiome.displayName else curBiome.displayName

            // Interpolate colors across biomes
            val skyTop = lerp(curBiome.skyTop, nxtBiome.skyTop, blendFactor)
            val skyBottom = lerp(curBiome.skyBottom, nxtBiome.skyBottom, blendFactor)
            val horizonCol = lerp(curBiome.horizonColor, nxtBiome.horizonColor, blendFactor)
            val groundCol = lerp(curBiome.groundColor, nxtBiome.groundColor, blendFactor)
            val roadCol = lerp(curBiome.roadColor, nxtBiome.roadColor, blendFactor)
            val laneMarkCol = lerp(curBiome.laneMarkColor, nxtBiome.laneMarkColor, blendFactor)

            val horizonY = screenHeight * 0.56f
            val roadY = screenHeight * 0.70f
            val roadHeight = screenHeight * 0.28f

            // 1. Sky Gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(skyTop, skyBottom),
                    startY = 0f,
                    endY = horizonY
                ),
                topLeft = Offset.Zero,
                size = Size(screenWidth, horizonY)
            )

            // Space Stars for Lunar / Space / Mars biomes (scrolls left smoothly)
            val spaceAlpha = when {
                curBiome == UniverseBiome.LUNAR_MOON || curBiome == UniverseBiome.MARS_DUNES -> 1f - (blendFactor * (if (nxtBiome == UniverseBiome.NEO_CITY) 1f else 0f))
                nxtBiome == UniverseBiome.LUNAR_MOON -> blendFactor
                else -> 0.15f // subtle background stars in night city
            }
            if (spaceAlpha > 0.05f) {
                drawCosmicStars(screenWidth, horizonY, spaceAlpha)
            }

            // Celestial bodies (Earth in Lunar biome, Phobos in Mars biome) (scrolls left smoothly)
            drawCelestialBodies(curBiome, nxtBiome, blendFactor, screenWidth, horizonY)

            // 2. Far Scenery Silhouette (Mountain / Skyline Ridge - smooth continuous wave moving left)
            drawFarBackgroundLayer(screenWidth, horizonY, horizonCol)

            // 3. Midground Landmark Scenery (Buildings, Trees, Lakes, Moon landers - moves left smoothly)
            drawMidgroundLayer(curBiome, nxtBiome, blendFactor, screenWidth, horizonY, roadY)

            // 4. Ground Layer
            drawRect(
                color = groundCol,
                topLeft = Offset(0f, horizonY),
                size = Size(screenWidth, screenHeight - horizonY)
            )

            // Special Lake Water surface when in Sapphire Lake biome (water ripples move left)
            drawLakeWaterSurface(curBiome, nxtBiome, blendFactor, screenWidth, horizonY, roadY)

            // 5. Road Surface
            drawRect(
                color = roadCol,
                topLeft = Offset(0f, roadY),
                size = Size(screenWidth, roadHeight)
            )

            // Road Edge Curbs
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(0f, roadY),
                end = Offset(screenWidth, roadY),
                strokeWidth = 3f
            )

            // Moving Lane Markings / Boardwalk Planks (scrolls left matching speed)
            drawMovingRoadMarkings(curBiome, roadY, roadHeight, screenWidth, laneMarkCol)

            // 6. Roadside Collectibles (Golden Yuzu Fruits & Space Stars - float into screen from right to left)
            drawCollectibles(screenWidth, roadY)

            // 7. Motion Speed Wind Streaks during Turbo (streaks move left rapidly)
            if (isTurboActive || currentSpeedKmh > 110f) {
                drawSpeedWarpStreaks(screenWidth, screenHeight)
            }

            // 8. Draw the Capybara Car on the road (car faces right, looking forward!)
            val suspensionBounce = sin(totalDistanceMeters * 0.45f) * 2.2f
            CapyCarDrawer.drawCar(
                drawScope = this,
                carType = selectedCar,
                carX = carX,
                carY = carY,
                carWidth = carWidth,
                carHeight = carHeight,
                wheelAngle = wheelAngle,
                suspensionBounce = suspensionBounce,
                speedKmh = currentSpeedKmh,
                isTurbo = isTurboActive
            )
        }
    }

    private fun DrawScope.drawCosmicStars(width: Float, height: Float, alpha: Float) {
        // Moves LEFT (-X direction) opposite to right-facing car motion
        val scrollOffset = (totalDistanceMeters * 1.5f) % width
        for (i in starX.indices) {
            val sx = ((starX[i] - scrollOffset) % width + width) % width
            val sy = (starY[i]) % (height * 0.75f)
            val size = starSize[i]
            drawCircle(
                color = Color.White.copy(alpha = alpha.coerceIn(0f, 1f)),
                radius = size,
                center = Offset(sx, sy)
            )
        }
    }

    private fun DrawScope.drawCelestialBodies(
        cur: UniverseBiome,
        nxt: UniverseBiome,
        blend: Float,
        width: Float,
        horizonY: Float
    ) {
        val isMoonActive = (cur == UniverseBiome.LUNAR_MOON || (nxt == UniverseBiome.LUNAR_MOON && blend > 0.3f))
        val isMarsActive = (cur == UniverseBiome.MARS_DUNES || (nxt == UniverseBiome.MARS_DUNES && blend > 0.3f))

        // Parallax scroll celestial objects smoothly to the LEFT
        val loopW = width + 300f
        val celestialScroll = (totalDistanceMeters * 0.8f) % loopW

        if (isMoonActive) {
            // Glowing Blue Marble Earth in the sky moving left
            val earthX = ((width * 0.85f - celestialScroll) % loopW + loopW) % loopW - 100f
            val earthY = horizonY * 0.38f
            val earthRadius = 42f
            val earthAlpha = if (cur == UniverseBiome.LUNAR_MOON) (1f - blend * if (nxt != UniverseBiome.LUNAR_MOON) 1f else 0f) else blend

            if (earthX in -100f..(width + 100f)) {
                // Atmospheric halo glow
                drawCircle(
                    color = Color(0x3340C4FF),
                    radius = earthRadius * 1.35f,
                    center = Offset(earthX, earthY)
                )
                // Blue Ocean
                drawCircle(
                    color = Color(0xFF0288D1).copy(alpha = earthAlpha.coerceIn(0f, 1f)),
                    radius = earthRadius,
                    center = Offset(earthX, earthY)
                )
                // Green continents & white clouds
                drawCircle(
                    color = Color(0xFF66BB6A).copy(alpha = earthAlpha.coerceIn(0f, 1f)),
                    radius = earthRadius * 0.60f,
                    center = Offset(earthX - 10f, earthY - 6f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.65f * earthAlpha.coerceIn(0f, 1f)),
                    radius = earthRadius * 0.45f,
                    center = Offset(earthX + 12f, earthY + 8f)
                )
            }
        }

        if (isMarsActive) {
            // Mars Moons: Phobos & Deimos moving left
            val marsAlpha = if (cur == UniverseBiome.MARS_DUNES) (1f - blend * if (nxt != UniverseBiome.MARS_DUNES) 1f else 0f) else blend
            val phobosX = ((width * 0.82f - celestialScroll) % loopW + loopW) % loopW - 50f
            val deimosX = ((width * 0.68f - celestialScroll) % loopW + loopW) % loopW - 50f

            if (phobosX in -50f..(width + 50f)) {
                drawCircle(
                    color = Color(0xFFE0E0E0).copy(alpha = marsAlpha.coerceIn(0f, 1f)),
                    radius = 14f,
                    center = Offset(phobosX, horizonY * 0.28f)
                )
            }
            if (deimosX in -50f..(width + 50f)) {
                drawCircle(
                    color = Color(0xFFBDBDBD).copy(alpha = marsAlpha.coerceIn(0f, 1f)),
                    radius = 8f,
                    center = Offset(deimosX, horizonY * 0.38f)
                )
            }
        }
    }

    private fun getMountainPeakHeight(worldX: Float): Float {
        // Continuous organic mountain peaks
        val wave1 = sin(worldX * 0.008f) * 32f
        val wave2 = cos(worldX * 0.022f) * 20f
        val wave3 = sin(worldX * 0.045f) * 12f
        return 45f + wave1 + wave2 + wave3
    }

    private fun DrawScope.drawFarBackgroundLayer(
        width: Float,
        horizonY: Float,
        horizonCol: Color
    ) {
        // Far background mountains move LEFT at parallax speed 3.5x
        val scrollFar = totalDistanceMeters * 3.5f

        val ridgePath = Path().apply {
            moveTo(0f, horizonY)
            val numSteps = 40
            val stepSize = width / numSteps
            for (i in 0..numSteps) {
                val x = i * stepSize
                val worldX = x + scrollFar
                val peakHeight = getMountainPeakHeight(worldX)
                lineTo(x, horizonY - peakHeight)
            }
            lineTo(width, horizonY)
            close()
        }
        drawPath(ridgePath, horizonCol.copy(alpha = 0.70f))
    }

    private fun DrawScope.drawMidgroundLayer(
        cur: UniverseBiome,
        nxt: UniverseBiome,
        blend: Float,
        width: Float,
        horizonY: Float,
        roadY: Float
    ) {
        when (cur) {
            UniverseBiome.NEO_CITY -> drawCityBuildings(width, horizonY, roadY, totalDistanceMeters, 1f - blend)
            UniverseBiome.EMERALD_FOREST -> drawForestTrees(width, horizonY, roadY, totalDistanceMeters, 1f - blend)
            UniverseBiome.SAPPHIRE_LAKE -> drawLakeScenery(width, horizonY, roadY, totalDistanceMeters, 1f - blend)
            UniverseBiome.LUNAR_MOON -> drawLunarScenery(width, horizonY, roadY, totalDistanceMeters, 1f - blend)
            UniverseBiome.MARS_DUNES -> drawMarsScenery(width, horizonY, roadY, totalDistanceMeters, 1f - blend)
        }

        if (blend > 0.05f) {
            when (nxt) {
                UniverseBiome.NEO_CITY -> drawCityBuildings(width, horizonY, roadY, totalDistanceMeters, blend)
                UniverseBiome.EMERALD_FOREST -> drawForestTrees(width, horizonY, roadY, totalDistanceMeters, blend)
                UniverseBiome.SAPPHIRE_LAKE -> drawLakeScenery(width, horizonY, roadY, totalDistanceMeters, blend)
                UniverseBiome.LUNAR_MOON -> drawLunarScenery(width, horizonY, roadY, totalDistanceMeters, blend)
                UniverseBiome.MARS_DUNES -> drawMarsScenery(width, horizonY, roadY, totalDistanceMeters, blend)
            }
        }
    }

    private fun DrawScope.drawCityBuildings(width: Float, horizonY: Float, roadY: Float, totalMeters: Float, alpha: Float) {
        // Midground moves LEFT at parallax speed 9.0x
        val scrollMid = totalMeters * 9.0f
        val buildingWidths = listOf(70f, 95f, 60f, 110f, 80f, 75f)
        val buildingHeights = listOf(140f, 185f, 110f, 210f, 160f, 130f)
        val totalPatternWidth = buildingWidths.sum() + buildingWidths.size * 18f // = 598f

        val scrollOffset = scrollMid % totalPatternWidth
        var curX = -scrollOffset - 100f
        var index = ((scrollMid / totalPatternWidth).toInt() * buildingWidths.size) % buildingWidths.size

        while (curX < width + 150f) {
            val idx = abs(index)
            val bw = buildingWidths[idx % buildingWidths.size]
            val bh = buildingHeights[idx % buildingHeights.size]
            val by = roadY - bh

            if (curX + bw > -50f && curX < width + 50f) {
                // Building body
                drawRect(
                    color = Color(0xFF1E293B).copy(alpha = alpha.coerceIn(0f, 1f)),
                    topLeft = Offset(curX, by),
                    size = Size(bw, bh)
                )
                // Lit window grids (cyberpunk yellow & cyan)
                val windowColor = if (idx % 2 == 0) Color(0xFFFDE047) else Color(0xFF38BDF8)
                for (wy in (by + 16f).toInt() until (roadY - 10f).toInt() step 22) {
                    for (wx in (curX + 10f).toInt() until (curX + bw - 10f).toInt() step 18) {
                        drawRect(
                            color = windowColor.copy(alpha = 0.75f * alpha.coerceIn(0f, 1f)),
                            topLeft = Offset(wx.toFloat(), wy.toFloat()),
                            size = Size(8f, 10f)
                        )
                    }
                }
                // Antenna beacon on roof
                drawLine(
                    color = Color(0xFFEF4444).copy(alpha = alpha.coerceIn(0f, 1f)),
                    start = Offset(curX + bw * 0.5f, by),
                    end = Offset(curX + bw * 0.5f, by - 24f),
                    strokeWidth = 2.5f
                )
                drawCircle(
                    color = Color(0xFFEF4444).copy(alpha = alpha.coerceIn(0f, 1f)),
                    radius = 4f,
                    center = Offset(curX + bw * 0.5f, by - 24f)
                )
            }

            curX += bw + 18f
            index++
        }
    }

    private fun DrawScope.drawForestTrees(width: Float, horizonY: Float, roadY: Float, totalMeters: Float, alpha: Float) {
        val scrollMid = totalMeters * 9.0f
        val treeSpacing = 95f
        val scrollOffset = scrollMid % treeSpacing
        var curX = -scrollOffset - 60f
        var index = (scrollMid / treeSpacing).toInt()

        while (curX < width + 100f) {
            val idx = abs(index)
            val th = 90f + (idx % 4) * 22f
            val trunkW = 14f
            val baseTreeY = roadY - 4f

            if (curX + th * 0.5f > -50f && curX < width + 50f) {
                // Trunk
                drawRect(
                    color = Color(0xFF5D4037).copy(alpha = alpha.coerceIn(0f, 1f)),
                    topLeft = Offset(curX + th * 0.22f - trunkW * 0.5f, baseTreeY - th * 0.35f),
                    size = Size(trunkW, th * 0.35f)
                )

                // 3-tier pine tree foliage
                val tierColors = listOf(Color(0xFF065F46), Color(0xFF047857), Color(0xFF10B981))
                for (t in 0..2) {
                    val tierY = baseTreeY - th * (0.28f + t * 0.24f)
                    val tierW = th * (0.65f - t * 0.14f)
                    val pineTier = Path().apply {
                        moveTo(curX + th * 0.22f - tierW * 0.5f, tierY)
                        lineTo(curX + th * 0.22f + tierW * 0.5f, tierY)
                        lineTo(curX + th * 0.22f, tierY - th * 0.30f)
                        close()
                    }
                    drawPath(pineTier, tierColors[t].copy(alpha = alpha.coerceIn(0f, 1f)))
                }
            }

            curX += treeSpacing
            index++
        }
    }

    private fun DrawScope.drawLakeScenery(width: Float, horizonY: Float, roadY: Float, totalMeters: Float, alpha: Float) {
        val scrollMid = totalMeters * 9.0f
        val loopW = width + 400f

        // Distant lighthouse on a rock moving left
        val lhX = ((width * 0.75f - scrollMid * 0.5f) % loopW + loopW) % loopW - 100f
        val lhY = roadY - 95f
        if (lhX in -100f..(width + 100f)) {
            val lhPath = Path().apply {
                moveTo(lhX - 16f, roadY)
                lineTo(lhX - 8f, lhY)
                lineTo(lhX + 8f, lhY)
                lineTo(lhX + 16f, roadY)
                close()
            }
            drawPath(lhPath, Color.White.copy(alpha = alpha.coerceIn(0f, 1f)))
            drawCircle(Color(0xFFFDE047).copy(alpha = 0.85f * alpha.coerceIn(0f, 1f)), radius = 8f, center = Offset(lhX, lhY - 4f))
        }

        // Sailboat moving left
        val boatX = ((width * 0.35f - scrollMid * 0.7f) % loopW + loopW) % loopW - 100f
        val boatY = roadY - 32f
        if (boatX in -100f..(width + 100f)) {
            val sailPath = Path().apply {
                moveTo(boatX, boatY)
                lineTo(boatX, boatY - 35f)
                lineTo(boatX + 24f, boatY)
                close()
            }
            drawPath(sailPath, Color.White.copy(alpha = alpha.coerceIn(0f, 1f)))
            drawRoundRect(
                color = Color(0xFF78350F).copy(alpha = alpha.coerceIn(0f, 1f)),
                topLeft = Offset(boatX - 14f, boatY),
                size = Size(38f, 10f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }

    private fun DrawScope.drawLakeWaterSurface(
        cur: UniverseBiome,
        nxt: UniverseBiome,
        blend: Float,
        width: Float,
        horizonY: Float,
        roadY: Float
    ) {
        val isLakeActive = (cur == UniverseBiome.SAPPHIRE_LAKE || nxt == UniverseBiome.SAPPHIRE_LAKE)
        if (isLakeActive) {
            val lakeAlpha = if (cur == UniverseBiome.SAPPHIRE_LAKE) (1f - blend * if (nxt != UniverseBiome.SAPPHIRE_LAKE) 1f else 0f) else blend
            drawRect(
                color = Color(0xFF0284C7).copy(alpha = lakeAlpha.coerceIn(0f, 1f)),
                topLeft = Offset(0f, horizonY),
                size = Size(width, roadY - horizonY)
            )
            // Animated wave ripples moving LEFT
            val rippleScroll = (totalDistanceMeters * 12f) % 75f
            for (ry in (horizonY + 12f).toInt() until roadY.toInt() step 18) {
                for (rx in (-rippleScroll).toInt() until (width + 75f).toInt() step 75) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.45f * lakeAlpha.coerceIn(0f, 1f)),
                        start = Offset(rx.toFloat(), ry.toFloat()),
                        end = Offset(rx + 28f, ry.toFloat()),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }

    private fun DrawScope.drawLunarScenery(width: Float, horizonY: Float, roadY: Float, totalMeters: Float, alpha: Float) {
        val scrollMid = totalMeters * 9.0f
        val loopW = width + 500f

        // Apollo Lunar Lander moving left
        val landerX = ((width * 0.70f - scrollMid * 0.6f) % loopW + loopW) % loopW - 150f
        val landerY = roadY - 75f
        if (landerX in -150f..(width + 150f)) {
            val landerPath = Path().apply {
                moveTo(landerX - 25f, roadY)
                lineTo(landerX - 18f, landerY + 30f)
                lineTo(landerX - 30f, landerY + 15f)
                lineTo(landerX + 30f, landerY + 15f)
                lineTo(landerX + 18f, landerY + 30f)
                lineTo(landerX + 25f, roadY)
                close()
            }
            drawPath(landerPath, Color(0xFF94A3B8).copy(alpha = alpha.coerceIn(0f, 1f)))
            drawRect(
                color = Color(0xFFF1F5F9).copy(alpha = alpha.coerceIn(0f, 1f)),
                topLeft = Offset(landerX - 16f, landerY),
                size = Size(32f, 22f)
            )
        }

        // Moon craters moving left
        val craterSpacing = 140f
        val scrollOffset = scrollMid % craterSpacing
        var cx = -scrollOffset - 50f
        while (cx < width + 80f) {
            drawOval(
                color = Color(0xFF1E293B).copy(alpha = 0.60f * alpha.coerceIn(0f, 1f)),
                topLeft = Offset(cx, roadY - 18f),
                size = Size(45f, 14f)
            )
            cx += craterSpacing
        }
    }

    private fun DrawScope.drawMarsScenery(width: Float, horizonY: Float, roadY: Float, totalMeters: Float, alpha: Float) {
        val scrollMid = totalMeters * 9.0f
        val loopW = width + 500f

        // Curiosity Rover moving left
        val roverX = ((width * 0.60f - scrollMid * 0.6f) % loopW + loopW) % loopW - 100f
        val roverY = roadY - 32f
        if (roverX in -100f..(width + 100f)) {
            drawRoundRect(
                color = Color(0xFFE2E8F0).copy(alpha = alpha.coerceIn(0f, 1f)),
                topLeft = Offset(roverX - 18f, roverY - 14f),
                size = Size(36f, 18f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawLine(
                color = Color(0xFFE2E8F0).copy(alpha = alpha.coerceIn(0f, 1f)),
                start = Offset(roverX - 8f, roverY - 14f),
                end = Offset(roverX - 8f, roverY - 30f),
                strokeWidth = 3f
            )
            drawCircle(Color(0xFF38BDF8).copy(alpha = alpha.coerceIn(0f, 1f)), radius = 4f, center = Offset(roverX - 8f, roverY - 30f))
        }

        // Red sand dune curves moving left
        val dunePeriod = 140f
        val scrollOffset = scrollMid % dunePeriod
        val dunePath = Path().apply {
            moveTo(-50f, roadY)
            var dx = -scrollOffset - 50f
            while (dx < width + 150f) {
                cubicTo(dx + 45f, roadY - 35f, dx + 95f, roadY - 35f, dx + dunePeriod, roadY)
                dx += dunePeriod
            }
            lineTo(width + 50f, roadY)
            close()
        }
        drawPath(dunePath, Color(0xFF9A3412).copy(alpha = 0.65f * alpha.coerceIn(0f, 1f)))
    }

    private fun DrawScope.drawMovingRoadMarkings(
        curBiome: UniverseBiome,
        roadY: Float,
        roadHeight: Float,
        screenWidth: Float,
        markColor: Color
    ) {
        val centerY = roadY + roadHeight * 0.42f
        // Foreground road lane markings scroll LEFT matching car wheel speed (25.0x)
        val dashScroll = (totalDistanceMeters * 25f) % 70f

        if (curBiome == UniverseBiome.SAPPHIRE_LAKE) {
            // Boardwalk wooden planks perpendicular to driving, scrolling left
            val plankScroll = (totalDistanceMeters * 25f) % 35f
            var px = -plankScroll
            while (px < screenWidth + 35f) {
                drawLine(
                    color = Color(0xFF451A03).copy(alpha = 0.60f),
                    start = Offset(px, roadY),
                    end = Offset(px, roadY + roadHeight),
                    strokeWidth = 3f
                )
                px += 30f
            }
        } else {
            // High-speed dashed center lane markers scrolling left
            var mx = -dashScroll
            while (mx < screenWidth + 70f) {
                drawRoundRect(
                    color = markColor,
                    topLeft = Offset(mx, centerY - 3f),
                    size = Size(42f, 6f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                mx += 70f
            }
        }
    }

    private fun DrawScope.drawCollectibles(screenWidth: Float, roadY: Float) {
        val carWorldX = totalDistanceMeters
        collectibles.forEach { item ->
            if (!item.collected) {
                // Collectibles slide in from the right edge and move left towards car
                val screenX = (item.worldX - carWorldX) * 25f + screenWidth * 0.34f
                if (screenX in -60f..(screenWidth + 60f)) {
                    val floatBob = sin((item.worldX + totalDistanceMeters * 5f) * 0.08f) * 6f
                    val itemY = roadY + item.yOffset + floatBob

                    if (item.isYuzu) {
                        // Golden Yuzu Fruit
                        drawCircle(Color(0x55FACC15), radius = 18f, center = Offset(screenX, itemY))
                        drawCircle(Color(0xFFFACC15), radius = 12f, center = Offset(screenX, itemY))
                        drawCircle(Color(0xFFFEF08A), radius = 6f, center = Offset(screenX - 3f, itemY - 3f))
                        val leaf = Path().apply {
                            moveTo(screenX, itemY - 11f)
                            cubicTo(screenX + 6f, itemY - 18f, screenX + 12f, itemY - 14f, screenX + 4f, itemY - 8f)
                            close()
                        }
                        drawPath(leaf, Color(0xFF22C55E))
                    } else {
                        // Golden Cosmic Star
                        drawCircle(Color(0x6638BDF8), radius = 18f, center = Offset(screenX, itemY))
                        drawCircle(Color(0xFFFDE047), radius = 11f, center = Offset(screenX, itemY))
                        drawCircle(Color.White, radius = 5f, center = Offset(screenX, itemY))
                    }
                }
            }
        }
    }

    private fun DrawScope.drawSpeedWarpStreaks(screenWidth: Float, screenHeight: Float) {
        val numStreaks = 8
        // Motion speed streaks move LEFT rapidly during turbo/high speed
        val warpScroll = (totalDistanceMeters * 40f) % screenWidth
        for (i in 0 until numStreaks) {
            val sy = (screenHeight * (0.18f + (i * 0.08f)))
            val sx = (screenWidth - warpScroll + (i * 130f)) % screenWidth
            val streakLen = 80f + (currentSpeedKmh / 220f) * 140f
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                    startX = sx,
                    endX = sx + streakLen
                ),
                start = Offset(sx, sy),
                end = Offset(sx + streakLen, sy),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
        }
    }
}
