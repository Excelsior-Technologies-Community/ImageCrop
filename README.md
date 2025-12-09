# **ImageCropperView**
---

A simple Android library and sample app to **pick, crop, and save images** using a custom cropper view.  
Users can select an image, crop it using **Square, Rectangle, or Circle** shapes, and preview the cropped result.  
This project includes a custom `ImageCropperView` and a sample app demonstrating its usage.

---

## **✨ Features**
- Crop images with **Square, Rectangle, and Circle** shapes  
- Draggable & resizable crop box  
- Grid overlay with corner handles  
- Reset crop box to default state  
- Save cropped image to cache storage  
- Preview cropped image in a separate screen  
- Clean black UI with sharp vector icons  
- Proper activity back-stack handling  
- Edge-to-edge & status bar support  

---

# **Preview**
---
<p align="center">
  <img src="https://github.com/user-attachments/assets/274f2f0f-ec50-40d6-8ef1-f2b3f80a9cd4"
       alt="Demo GIF"
       width="200">

</p>


## **⚡ Installation**

**Step 1:** Add JitPack repository to your root `build.gradle`:

```gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

**Step 2:** Add dependency to your app module `build.gradle`:
```
dependencies {
implementation("com.github.Excelsior-Technologies-Community:ImageCrop:1.0.0")
 }
```
## **📦 Usage**

**Add the custom cropper view in XML:**

```xml
<com.ext.image_crop.ImageCropperView
    android:id="@+id/cropperView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:cropShape="square"
    app:showGrid="true"
    app:borderColor="#FFFFFF"
    app:borderWidth="3dp"
    app:overlayColor="#AA000000"
    app:minCropSize="100dp" />
```

## **In your MainActivity.kt, initialize the cropper**

```kotlin
class MainActivity : AppCompatActivity() {

    private lateinit var cropperView: ImageCropperView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cropperView = findViewById(R.id.cropperView)
        // Set default shape (optional)
        cropperView.cropShape = ImageCropperView.CropShape.SQUARE
        // Enable draggable crop box
        cropperView.setDraggable(true)
    }

    // Example: Save cropped image
    private fun saveCroppedImage() {
        val bitmap = cropperView.getCroppedBitmap()
        if (bitmap != null) {
            val file = saveToCache(bitmap)
            // handle file
        }
    }
}
```

## **📄 License**

**MIT License**  
```
Copyright (c) 2025 Excelsior Technologies

Permission is hereby granted, free of charge, to any person obtaining a copy  
of this software and associated documentation files (the "Software"), to deal  
in the Software without restriction, including without limitation the rights  
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell  
copies of the Software, and to permit persons to whom the Software is  
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all  
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED **"AS IS"**, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR  
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,  
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
```

