package com.ext.image_crop

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ImageCropperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var bitmap: Bitmap? = null
    private val drawMatrix = Matrix()
    private var lastX = 0f
    private var lastY = 0f

    private var draggable = false
    private val cropRect = RectF()
    private val imageRect = RectF()

    // Crop shape: SQUARE, RECTANGLE, CIRCLE
    var cropShape = CropShape.SQUARE
        set(value) {
            field = value
            initializeCropRect()
            invalidate()
        }

    enum class CropShape {
        SQUARE, RECTANGLE, CIRCLE
    }

    private var borderColor: Int = Color.WHITE
    private var borderWidth: Float = 4f
    private var overlayColor: Int = Color.parseColor("#AA000000")
    private var cornerHandleColor: Int = Color.WHITE
    private var cornerHandleRadius: Float = 12f
    private var gridColor: Int = Color.parseColor("#88FFFFFF")
    private var showGrid: Boolean = true
    private var minCropSize: Float = 100f

    private val borderPaint = Paint().apply {
        color = borderColor
        style = Paint.Style.STROKE
        strokeWidth = borderWidth
        isAntiAlias = true
    }

    private val overlayPaint = Paint().apply {
        color = overlayColor
        style = Paint.Style.FILL
    }

    private val cornerPaint = Paint().apply {
        color = cornerHandleColor
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val cornerBorderPaint = Paint().apply {
        color = Color.parseColor("#80000000")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    private val gridPaint = Paint().apply {
        color = gridColor
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
    }

    private var activeCorner: Corner? = null
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private val touchTolerance = 50f

    private var draggingCropBox = false

    enum class Corner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
        LEFT, RIGHT, TOP, BOTTOM
    }

    init {
        scaleType = ScaleType.MATRIX
        attrs?.let {
            context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.ImageCropperView,
                0, 0
            ).apply {
                try {
                    val shapeValue = getInt(R.styleable.ImageCropperView_cropShape, 0)
                    cropShape = when (shapeValue) {
                        0 -> CropShape.SQUARE
                        1 -> CropShape.RECTANGLE
                        2 -> CropShape.CIRCLE
                        else -> CropShape.SQUARE
                    }
                    borderColor = getColor(R.styleable.ImageCropperView_borderColor, Color.WHITE)
                    borderWidth = getDimension(R.styleable.ImageCropperView_borderWidth, 4f)
                    overlayColor = getColor(R.styleable.ImageCropperView_overlayColor, Color.parseColor("#AA000000"))
                    cornerHandleColor = getColor(R.styleable.ImageCropperView_cornerHandleColor, Color.WHITE)
                    cornerHandleRadius = getDimension(R.styleable.ImageCropperView_cornerHandleRadius, 12f)
                    gridColor = getColor(R.styleable.ImageCropperView_gridColor, Color.parseColor("#88FFFFFF"))
                    showGrid = getBoolean(R.styleable.ImageCropperView_showGrid, true)
                    minCropSize = getDimension(R.styleable.ImageCropperView_minCropSize, 100f)
                } finally {
                    recycle()
                }

                borderPaint.color = borderColor
                borderPaint.strokeWidth = borderWidth
                overlayPaint.color = overlayColor
                cornerPaint.color = cornerHandleColor
                gridPaint.color = gridColor
            }
        }
    }

    fun setImage(bitmap: Bitmap) {
        this.bitmap = bitmap
        post {
            fitImageToView()
            initializeCropRect()
            invalidate()
        }
    }

    private fun fitImageToView() {
        bitmap?.let { bmp ->
            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()
            val bmpWidth = bmp.width.toFloat()
            val bmpHeight = bmp.height.toFloat()
            val scale = min(viewWidth / bmpWidth, viewHeight / bmpHeight)
            val scaledWidth = bmpWidth * scale
            val scaledHeight = bmpHeight * scale
            val dx = (viewWidth - scaledWidth) / 2f
            val dy = (viewHeight - scaledHeight) / 2f
            drawMatrix.reset()
            drawMatrix.postScale(scale, scale)
            drawMatrix.postTranslate(dx, dy)
            imageRect.set(dx, dy, dx + scaledWidth, dy + scaledHeight)
        }
    }

    private fun initializeCropRect() {
        if (imageRect.isEmpty) return

        when (cropShape) {
            CropShape.SQUARE, CropShape.CIRCLE -> {
                val size = min(imageRect.width(), imageRect.height()) * 0.7f
                val centerX = imageRect.centerX()
                val centerY = imageRect.centerY()
                cropRect.set(centerX - size / 2, centerY - size / 2, centerX + size / 2, centerY + size / 2)
            }
            CropShape.RECTANGLE -> {
                val width = imageRect.width() * 0.7f
                val height = imageRect.height() * 0.6f
                val centerX = imageRect.centerX()
                val centerY = imageRect.centerY()
                cropRect.set(centerX - width / 2, centerY - height / 2, centerX + width / 2, centerY + height / 2)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        bitmap?.let { bmp -> canvas.drawBitmap(bmp, drawMatrix, null) }
        if (cropShape == CropShape.CIRCLE) drawCircleOverlay(canvas) else drawRectangleOverlay(canvas)
        if (showGrid) drawGrid(canvas)
        if (cropShape == CropShape.CIRCLE) canvas.drawCircle(cropRect.centerX(), cropRect.centerY(), cropRect.width() / 2, borderPaint)
        else canvas.drawRect(cropRect, borderPaint)
        drawCornerHandles(canvas)
    }

    private fun drawRectangleOverlay(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, overlayPaint)
        canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), overlayPaint)
        canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, overlayPaint)
        canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, overlayPaint)
    }

    private fun drawCircleOverlay(canvas: Canvas) {
        val path = Path()
        path.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        path.addCircle(cropRect.centerX(), cropRect.centerY(), cropRect.width() / 2, Path.Direction.CCW)
        canvas.drawPath(path, overlayPaint)
    }

    private fun drawGrid(canvas: Canvas) {
        val third = cropRect.width() / 3
        val thirdHeight = cropRect.height() / 3
        canvas.drawLine(cropRect.left + third, cropRect.top, cropRect.left + third, cropRect.bottom, gridPaint)
        canvas.drawLine(cropRect.left + third * 2, cropRect.top, cropRect.left + third * 2, cropRect.bottom, gridPaint)
        canvas.drawLine(cropRect.left, cropRect.top + thirdHeight, cropRect.right, cropRect.top + thirdHeight, gridPaint)
        canvas.drawLine(cropRect.left, cropRect.top + thirdHeight * 2, cropRect.right, cropRect.top + thirdHeight * 2, gridPaint)
    }

    private fun drawCornerHandles(canvas: Canvas) {
        val points = listOf(
            Pair(cropRect.left, cropRect.top),
            Pair(cropRect.right, cropRect.top),
            Pair(cropRect.left, cropRect.bottom),
            Pair(cropRect.right, cropRect.bottom)
        )
        points.forEach { (x, y) ->
            canvas.drawCircle(x, y, cornerHandleRadius, cornerPaint)
            canvas.drawCircle(x, y, cornerHandleRadius, cornerBorderPaint)
        }
        if (cropShape == CropShape.RECTANGLE) {
            canvas.drawCircle(cropRect.centerX(), cropRect.top, cornerHandleRadius, cornerPaint)
            canvas.drawCircle(cropRect.centerX(), cropRect.top, cornerHandleRadius, cornerBorderPaint)
            canvas.drawCircle(cropRect.centerX(), cropRect.bottom, cornerHandleRadius, cornerPaint)
            canvas.drawCircle(cropRect.centerX(), cropRect.bottom, cornerHandleRadius, cornerBorderPaint)
            canvas.drawCircle(cropRect.left, cropRect.centerY(), cornerHandleRadius, cornerPaint)
            canvas.drawCircle(cropRect.left, cropRect.centerY(), cornerHandleRadius, cornerBorderPaint)
            canvas.drawCircle(cropRect.right, cropRect.centerY(), cornerHandleRadius, cornerPaint)
            canvas.drawCircle(cropRect.right, cropRect.centerY(), cornerHandleRadius, cornerBorderPaint)
        }
    }

    fun setDraggable(enabled: Boolean) {
        draggable = enabled
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!draggable) return super.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                activeCorner = getCornerAtPoint(event.x, event.y)
                draggingCropBox = activeCorner == null && cropRect.contains(event.x, event.y)
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY

                if (activeCorner != null) {
                    updateCropRect(activeCorner!!, dx, dy)
                } else if (draggingCropBox) {
                    moveCropBox(dx, dy)
                }

                lastTouchX = event.x
                lastTouchY = event.y
                invalidate()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeCorner = null
                draggingCropBox = false
            }
        }
        return true
    }

    private fun moveCropBox(dx: Float, dy: Float) {
        cropRect.offset(dx, dy)
        if (cropRect.left < imageRect.left) cropRect.offset(imageRect.left - cropRect.left, 0f)
        if (cropRect.top < imageRect.top) cropRect.offset(0f, imageRect.top - cropRect.top)
        if (cropRect.right > imageRect.right) cropRect.offset(imageRect.right - cropRect.right, 0f)
        if (cropRect.bottom > imageRect.bottom) cropRect.offset(0f, imageRect.bottom - cropRect.bottom)
    }

    private fun getCornerAtPoint(x: Float, y: Float): Corner? {
        if (isNear(x, cropRect.left) && isNear(y, cropRect.top)) return Corner.TOP_LEFT
        if (isNear(x, cropRect.right) && isNear(y, cropRect.top)) return Corner.TOP_RIGHT
        if (isNear(x, cropRect.left) && isNear(y, cropRect.bottom)) return Corner.BOTTOM_LEFT
        if (isNear(x, cropRect.right) && isNear(y, cropRect.bottom)) return Corner.BOTTOM_RIGHT
        if (cropShape == CropShape.RECTANGLE) {
            if (isNear(x, cropRect.centerX()) && isNear(y, cropRect.top)) return Corner.TOP
            if (isNear(x, cropRect.centerX()) && isNear(y, cropRect.bottom)) return Corner.BOTTOM
            if (isNear(x, cropRect.left) && isNear(y, cropRect.centerY())) return Corner.LEFT
            if (isNear(x, cropRect.right) && isNear(y, cropRect.centerY())) return Corner.RIGHT
        }
        return null
    }

    private fun isNear(a: Float, b: Float) = abs(a - b) < touchTolerance

    private fun updateCropRect(corner: Corner, dx: Float, dy: Float) {
        val newRect = RectF(cropRect)
        when (corner) {
            Corner.TOP_LEFT -> { if (cropShape == CropShape.SQUARE || cropShape == CropShape.CIRCLE) { val delta = max(dx, dy); newRect.left += delta; newRect.top += delta } else { newRect.left += dx; newRect.top += dy } }
            Corner.TOP_RIGHT -> { if (cropShape == CropShape.SQUARE || cropShape == CropShape.CIRCLE) { val delta = max(-dx, dy); newRect.right -= delta; newRect.top += delta } else { newRect.right += dx; newRect.top += dy } }
            Corner.BOTTOM_LEFT -> { if (cropShape == CropShape.SQUARE || cropShape == CropShape.CIRCLE) { val delta = max(dx, -dy); newRect.left += delta; newRect.bottom -= delta } else { newRect.left += dx; newRect.bottom += dy } }
            Corner.BOTTOM_RIGHT -> { if (cropShape == CropShape.SQUARE || cropShape == CropShape.CIRCLE) { val delta = max(dx, dy); newRect.right += delta; newRect.bottom += delta } else { newRect.right += dx; newRect.bottom += dy } }
            Corner.TOP -> { newRect.top += dy }
            Corner.BOTTOM -> { newRect.bottom += dy }
            Corner.LEFT -> { newRect.left += dx }
            Corner.RIGHT -> { newRect.right += dx }
        }
        if (cropShape == CropShape.SQUARE || cropShape == CropShape.CIRCLE) {
            val size = min(newRect.width(), newRect.height())
            when (corner) {
                Corner.TOP_LEFT -> { newRect.left = newRect.right - size; newRect.top = newRect.bottom - size }
                Corner.TOP_RIGHT -> { newRect.right = newRect.left + size; newRect.top = newRect.bottom - size }
                Corner.BOTTOM_LEFT -> { newRect.left = newRect.right - size; newRect.bottom = newRect.top + size }
                Corner.BOTTOM_RIGHT -> { newRect.right = newRect.left + size; newRect.bottom = newRect.top + size }
                else -> {}
            }
        }
        if (newRect.width() >= minCropSize && newRect.height() >= minCropSize &&
            newRect.left >= imageRect.left && newRect.right <= imageRect.right &&
            newRect.top >= imageRect.top && newRect.bottom <= imageRect.bottom) {
            cropRect.set(newRect)
        }
    }

    fun getCroppedBitmap(): Bitmap? {
        bitmap?.let { bmp ->
            val values = FloatArray(9)
            drawMatrix.getValues(values)
            val scaleX = values[Matrix.MSCALE_X]
            val scaleY = values[Matrix.MSCALE_Y]
            val transX = values[Matrix.MTRANS_X]
            val transY = values[Matrix.MTRANS_Y]

            val left = ((cropRect.left - transX) / scaleX).toInt().coerceAtLeast(0)
            val top = ((cropRect.top - transY) / scaleY).toInt().coerceAtLeast(0)
            val width = ((cropRect.width()) / scaleX).toInt().coerceAtMost(bmp.width - left)
            val height = ((cropRect.height()) / scaleY).toInt().coerceAtMost(bmp.height - top)

            if (left < 0 || top < 0 || width <= 0 || height <= 0 ||
                left + width > bmp.width || top + height > bmp.height) return null

            val croppedBitmap = Bitmap.createBitmap(bmp, left, top, width, height)
            return if (cropShape == CropShape.CIRCLE) createCircularBitmap(croppedBitmap) else croppedBitmap
        }
        return null
    }



    private fun createCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = min(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply { isAntiAlias = true; color = Color.BLACK }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
        val dstRect = Rect(0, 0, size, size)
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
        return output
    }

    fun resetCrop() { initializeCropRect(); invalidate() }
    fun clearImage() { setImageBitmap(null); invalidate() }
    fun setOverlayColor(color: Int) { overlayColor = color; overlayPaint.color = color; invalidate() }
    fun setCornerHandleColor(color: Int) { cornerHandleColor = color; cornerPaint.color = color; invalidate() }
    fun setGridVisible(visible: Boolean) { showGrid = visible; invalidate() }
    fun setBorderColor(color: Int) { borderColor = color; borderPaint.color = color; invalidate() }
    fun setBorderWidth(width: Float) { borderWidth = width; borderPaint.strokeWidth = width; invalidate() }
}
