



package com.ext.imagecrop

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import com.ext.image_crop.ImageCropperView
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var cropperView: ImageCropperView
    private lateinit var btnBack: ImageButton
    private lateinit var btnSave: ImageButton
    private lateinit var btnReset: ImageButton
    private lateinit var btnShapeSquare: ImageButton
    private lateinit var btnShapeRectangle: ImageButton
    private lateinit var btnShapeCircle: ImageButton

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            uri?.let { loadImageFromUri(it) }
        }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge layout
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        setContentView(R.layout.activity_main)

        // Apply padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(android.view.WindowInsets.Type.systemBars())
            v.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }

        initViews()
        setupClickListeners()
        applyDynamicStatusBar()

        // Open image picker automatically on first launch
        openImagePicker()
    }

    private fun initViews() {
        cropperView = findViewById(R.id.cropperView)
        btnBack = findViewById(R.id.btnBack)
        btnSave = findViewById(R.id.btnSave)
        btnReset = findViewById(R.id.btnReset)

        btnShapeSquare = findViewById(R.id.btnShapeSquare)
        btnShapeRectangle = findViewById(R.id.btnShapeRectangle)
        btnShapeCircle = findViewById(R.id.btnShapeCircle)

        updateShapeSelection(btnShapeSquare) // default shape

        // Enable dragging the crop box
        cropperView.setDraggable(true)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val croppedBitmap = cropperView.getCroppedBitmap()
            if (croppedBitmap != null) {
                // Save to cache and pass to StartScreenActivity
                val file = saveToCache(croppedBitmap)
                if (file != null) {
                    val intent = android.content.Intent(this, StartScreenActivity::class.java)
                    intent.putExtra("cropped_image_path", file.absolutePath)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK


                            startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to save image!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Nothing to save!", Toast.LENGTH_SHORT).show()
            }
        }

        btnReset.setOnClickListener {

            // 1. Clear image completely
            cropperView.clearImage()

            // 2. Reset crop shape to default (Square)
            cropperView.cropShape = ImageCropperView.CropShape.SQUARE

            // 3. Reset crop box position & size
            cropperView.resetCrop()

//            // 4. Disable editing controls
//            setEditingControlsEnabled(false)

            // 5. Reset UI selection
            updateShapeSelection(btnShapeSquare)
        }


        btnShapeSquare.setOnClickListener {
            cropperView.cropShape = ImageCropperView.CropShape.SQUARE
            updateShapeSelection(btnShapeSquare)
        }
        btnShapeRectangle.setOnClickListener {
            cropperView.cropShape = ImageCropperView.CropShape.RECTANGLE
            updateShapeSelection(btnShapeRectangle)
        }
        btnShapeCircle.setOnClickListener {
            cropperView.cropShape = ImageCropperView.CropShape.CIRCLE
            updateShapeSelection(btnShapeCircle)
        }
    }

    private fun updateShapeSelection(selectedButton: ImageButton) {
        btnShapeSquare.isSelected = selectedButton == btnShapeSquare
        btnShapeRectangle.isSelected = selectedButton == btnShapeRectangle
        btnShapeCircle.isSelected = selectedButton == btnShapeCircle
    }

    private fun loadImageFromUri(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    cropperView.setImage(bitmap)
                    setEditingControlsEnabled(true)
                } else {
//                    Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
//            Toast.makeText(this, "Error loading image.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setEditingControlsEnabled(enabled: Boolean) {
        val alpha = if (enabled) 1f else 0.4f
        btnShapeSquare.isEnabled = enabled
        btnShapeRectangle.isEnabled = enabled
        btnShapeCircle.isEnabled = enabled
        btnShapeSquare.alpha = alpha
        btnShapeRectangle.alpha = alpha
        btnShapeCircle.alpha = alpha
    }

    private fun openImagePicker() {
        pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun saveToCache(bitmap: Bitmap): File? {
        return try {
            val fileName = "Crop_${System.currentTimeMillis()}.jpg"
            val dir = cacheDir
            val file = File(dir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun applyDynamicStatusBar() {
        window.statusBarColor = android.graphics.Color.parseColor("#000000") // black
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                0, // no light icons
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        }
    }

}
//package com.ext.imagecrop
//
//import android.annotation.SuppressLint
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.graphics.Color
//import android.net.Uri
//import android.os.Build
//import android.os.Bundle
//import android.os.Environment
//import android.provider.MediaStore
//import android.view.WindowInsetsController
//import android.widget.Button
//import android.widget.ImageButton
//import android.widget.TextView
//import android.widget.Toast
//import androidx.activity.result.PickVisualMediaRequest
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.annotation.RequiresApi
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.view.ViewCompat
//import androidx.core.view.updatePadding
//import com.ext.image_crop.ImageCropperView
//import java.io.File
//import java.io.FileOutputStream
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var cropperView: ImageCropperView
//    private lateinit var btnBack: ImageButton
//    private lateinit var btnSave: ImageButton
//    private lateinit var btnReset: ImageButton
//    private lateinit var btnSelectImage: Button
//    private lateinit var btnShapeSquare: ImageButton
//    private lateinit var btnShapeRectangle: ImageButton
//    private lateinit var btnShapeCircle: ImageButton
//
//    private val pickImageLauncher =
//        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
//            uri?.let { loadImageFromUri(it) }
//        }
//
//    @RequiresApi(Build.VERSION_CODES.R)
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        initViews()
//        setupClickListeners()
//        applyDynamicStatusBar()
//        applyWindowInsets()
//
//        val incomingUri = intent.getParcelableExtra<Uri>("selected_image_uri")
//        if (incomingUri != null) {
//            loadImageFromUri(incomingUri)
//        } else {
//            // Open gallery automatically on first launch
//            openImagePicker()
//        }
//    }
//
//    private fun initViews() {
//        cropperView = findViewById(R.id.cropperView)
//        btnBack = findViewById(R.id.btnBack)
//        btnSave = findViewById(R.id.btnSave)
//        btnReset = findViewById(R.id.btnReset)
//
//        btnShapeSquare = findViewById(R.id.btnShapeSquare)
//        btnShapeRectangle = findViewById(R.id.btnShapeRectangle)
//        btnShapeCircle = findViewById(R.id.btnShapeCircle)
//
//        updateShapeSelection(btnShapeSquare) // default shape
//
//        // Enable dragging the crop box anywhere on the image
//        cropperView.setDraggable(true)
//    }
//
//    private fun setupClickListeners() {
//        btnBack.setOnClickListener { finish() }
//
//
//        btnSave.setOnClickListener {
//            val croppedBitmap = cropperView.getCroppedBitmap()
//            if (croppedBitmap != null && saveToGallery(croppedBitmap)) {
//                Toast.makeText(this, "Image saved successfully!", Toast.LENGTH_SHORT).show()
//            } else {
//                Toast.makeText(this, "Nothing to save!", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//
//        btnReset.setOnClickListener {
//            cropperView.clearImage() // clears current image
//            setEditingControlsEnabled(false) // optionally disable shape buttons until new image loaded
//            Toast.makeText(this, "Reset done! Pick a new image.", Toast.LENGTH_SHORT).show()
//
//            // Open gallery automatically
//            openImagePicker()
//        }
//
//
//        btnShapeSquare.setOnClickListener {
//            cropperView.cropShape = ImageCropperView.CropShape.SQUARE
//            updateShapeSelection(btnShapeSquare)
//        }
//
//        btnShapeRectangle.setOnClickListener {
//            cropperView.cropShape = ImageCropperView.CropShape.RECTANGLE
//            updateShapeSelection(btnShapeRectangle)
//        }
//
//        btnShapeCircle.setOnClickListener {
//            cropperView.cropShape = ImageCropperView.CropShape.CIRCLE
//            updateShapeSelection(btnShapeCircle)
//        }
//    }
//
//    private fun updateShapeSelection(selectedButton: ImageButton) {
//        btnShapeSquare.isSelected = selectedButton == btnShapeSquare
//        btnShapeRectangle.isSelected = selectedButton == btnShapeRectangle
//        btnShapeCircle.isSelected = selectedButton == btnShapeCircle
//    }
//
//    private fun loadImageFromUri(uri: Uri) {
//        try {
//            contentResolver.openInputStream(uri)?.use { inputStream ->
//                val bitmap = BitmapFactory.decodeStream(inputStream)
//                if (bitmap != null) {
//                    cropperView.setImage(bitmap)
//                    setEditingControlsEnabled(true)
//                    Toast.makeText(this, "Image loaded! Drag the crop box anywhere.", Toast.LENGTH_SHORT).show()
//                } else {
//                    Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show()
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Toast.makeText(this, "Error loading image.", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun setEditingControlsEnabled(enabled: Boolean) {
//        val alpha = if (enabled) 1f else 0.4f
//        btnShapeSquare.isEnabled = enabled
//        btnShapeRectangle.isEnabled = enabled
//        btnShapeCircle.isEnabled = enabled
//
//        btnShapeSquare.alpha = alpha
//        btnShapeRectangle.alpha = alpha
//        btnShapeCircle.alpha = alpha
//    }
//
//    private fun openImagePicker() {
//        pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
//    }
//
//    private fun saveToGallery(bitmap: Bitmap): Boolean {
//        return try {
//            val shape = when (cropperView.cropShape) {
//                ImageCropperView.CropShape.SQUARE -> "square"
//                ImageCropperView.CropShape.RECTANGLE -> "rect"
//                ImageCropperView.CropShape.CIRCLE -> "circle"
//            }
//            val fileName = "Crop_${shape}_${System.currentTimeMillis()}.jpg"
//            val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//            val file = File(dir, fileName)
//
//            FileOutputStream(file).use { out ->
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
//            }
//
//            MediaStore.Images.Media.insertImage(contentResolver, file.absolutePath, fileName, null)
//            true
//        } catch (e: Exception) {
//            e.printStackTrace()
//            false
//        }
//    }
//
//    private fun applyDynamicStatusBar() {
//        window.statusBarColor = Color.parseColor("#000000")
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            window.insetsController?.setSystemBarsAppearance(
//                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
//                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
//            )
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.R)
//    @SuppressLint("WrongConstant")
//    private fun applyWindowInsets() {
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
//            val systemBars = insets.getInsets(android.view.WindowInsets.Type.systemBars())
//            view.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
//            insets
//        }
//    }
//}
//
//
