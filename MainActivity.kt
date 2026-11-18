package se.example.photovisit

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContent { MaterialTheme { App(this) } }
    }
}

@Composable
fun App(ctx: Context) {
    var photos by remember { mutableStateOf(listOf<Photo>()) }
    var loading by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }
    var showMap by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun scan() {
        loading = true
        scope.launch {
            photos = withContext(Dispatchers.IO) { Geotagger.scan(ctx) }
            loading = false
        }
    }

    val perms = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        denied = res[Manifest.permission.ACCESS_MEDIA_LOCATION] != true
        if (res[Manifest.permission.READ_MEDIA_IMAGES] == true) scan()
    }
    LaunchedEffect(Unit) {
        perms.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.ACCESS_MEDIA_LOCATION))
    }

    val withGps = photos.count { it.lat != null }
    Column(Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("PhotoVisit", style = MaterialTheme.typography.headlineMedium)
        Text(when {
            loading -> "Läser foton…"
            photos.isEmpty() -> "Inga foton lästa ännu."
            else -> "$withGps av ${photos.size} senaste foton har plats."
        })
        if (denied) Text("Tillåt behörigheten för platsinfo i foton i appens inställningar, annars kan platsen inte läsas.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scan() }) { Text("Läs om") }
            OutlinedButton(onClick = { showMap = !showMap }) { Text(if (showMap) "Visa lista" else "Visa karta") }
        }
        if (showMap) MapScreen(photos, Modifier.fillMaxWidth().weight(1f))
        else {
            val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
            LazyColumn(Modifier.weight(1f)) {
                items(photos) { p ->
                    ListItem(
                        leadingContent = { Thumb(ctx, p.uri) },
                        headlineContent = { Text(p.name) },
                        supportingContent = {
                            Text(fmt.format(Date(p.takenAt)) + if (p.lat != null) "  ·  %.5f, %.5f".format(p.lat, p.lon) else "  ·  saknar plats")
                        },
                        trailingContent = {
                            if (p.lat != null) TextButton(onClick = {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${p.lat},${p.lon}?q=${p.lat},${p.lon}")))
                            }) { Text("Öppna") }
                        })
                }
            }
        }
    }
}

@Composable
fun Thumb(ctx: Context, uri: Uri) {
    val bmp by produceState<ImageBitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            try { ctx.contentResolver.loadThumbnail(uri, Size(112, 112), null).asImageBitmap() } catch (e: Exception) { null }
        }
    }
    bmp?.let { Image(it, null, Modifier.size(56.dp)) } ?: Spacer(Modifier.size(56.dp))
}
