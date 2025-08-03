package dev.eliaschen.national.composeimagepicker

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import dev.eliaschen.national.composeimagepicker.ui.theme.ComposeimagepickerTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposeimagepickerTheme {
                PhotoChooserScreen()
            }
        }
    }
}

@Composable
fun PhotoChooserScreen() {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val photoFile =
        File(context.getExternalFilesDir("camera_photo"), "photo_${System.currentTimeMillis()}.jpg")
    val photoUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        photoFile
    )

    // Launcher for picking images from gallery
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedImageUri = uri
    }

    // Launcher for taking photos with camera
    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri = photoUri
        }
    }

    // Camera Intent
    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
        putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    // Gallery Intent
    val galleryIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "image/*"
    }

    // Create Chooser
    val chooserIntent = Intent.createChooser(galleryIntent, "Select Image").apply {
        putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
    }
    val chooserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        selectedImageUri = result.data?.data ?: photoUri
        // NOTE: the result.data.data return null when user chose to take photo with camera
        Log.e("image get uri", selectedImageUri.toString())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Text(
            text = "Photo Chooser Demo",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text("Pick Image from Gallery")
        }

        Button(
            onClick = {
                takePhotoLauncher.launch(photoUri)
            },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("Take Photo with Camera")
        }

        Button(onClick = {
            chooserLauncher.launch(chooserIntent)
        }) {
            Text("Select Image")
        }

        selectedImageUri?.let { uri ->
            Text(
                text = "Selected Image:",
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            ContentImage(uri)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "URI: $uri",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
            )
        }
    }
}


@Composable
fun ContentImage(uri: Uri) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            bitmap = BitmapFactory.decodeStream(inputStream)
        }
    }
    bitmap?.let {
        Image(bitmap = it.asImageBitmap(), contentDescription = null)
    }
}
