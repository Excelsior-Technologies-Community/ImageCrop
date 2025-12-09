package com.ext.imagecrop

import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class StartScreenActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var croppedImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_screen_actiivity)

        startButton = findViewById(R.id.startButton)
        croppedImageView = findViewById(R.id.croppedImageView)

        // Load cropped image if passed from MainActivity
        val imagePath = intent.getStringExtra("cropped_image_path")
        if (!imagePath.isNullOrEmpty()) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            croppedImageView.setImageBitmap(bitmap)
            croppedImageView.visibility = View.VISIBLE
        }

        // Button to open MainActivity for new crop
        startButton.setOnClickListener {
            val intent = android.content.Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Status bar customization
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
//
//
//import android.content.Intent
//import android.graphics.Color
//import android.os.Build
//import android.os.Bundle
//import android.view.WindowInsetsController
//import androidx.appcompat.app.AppCompatActivity
//import android.widget.Button
//
//class StartScreenActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_start_screen_actiivity)
//
//        val startButton: Button = findViewById(R.id.startButton)
//        startButton.setOnClickListener {
//            val intent = Intent(this, MainActivity::class.java)
//            startActivity(intent)
//            finish()
//        }
//        window.statusBarColor = Color.parseColor("#000000")
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            window.insetsController?.setSystemBarsAppearance(
//                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
//                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
//            )
//        }
//    }
//}
