package se.example.photovisit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/** Visar varje foto med plats som en markör (OpenStreetMap, ingen API-nyckel). */
@Composable
fun MapScreen(photos: List<Photo>, modifier: Modifier = Modifier) {
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    AndroidView(
        modifier = modifier,
        factory = { c ->
            Configuration.getInstance().apply {
                userAgentValue = c.packageName
                osmdroidBasePath = File(c.cacheDir, "osmdroid")
                osmdroidTileCache = File(c.cacheDir, "osmdroid/tiles")
            }
            MapView(c).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(5.0)
            }
        },
        update = { map ->
            map.overlays.clear()
            val pts = mutableListOf<GeoPoint>()
            photos.forEach { p ->
                if (p.lat == null || p.lon == null) return@forEach
                val g = GeoPoint(p.lat, p.lon)
                pts += g
                map.overlays.add(Marker(map).apply {
                    position = g
                    title = p.name
                    snippet = fmt.format(Date(p.takenAt))
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                })
            }
            when {
                pts.size > 1 -> map.post { map.zoomToBoundingBox(BoundingBox.fromGeoPoints(pts), true, 96) }
                pts.size == 1 -> { map.controller.setZoom(16.0); map.controller.setCenter(pts[0]) }
            }
            map.invalidate()
        }
    )
}
