package com.example.videoblendingpoc

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.TextureView
import android.view.View
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.MediaController
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.videoblendingpoc.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    val TAG = "===rode==="
    val widthRatio = 12
    val heightRatio = 9
    val overlappingRatio = 3

    private lateinit var binding: ActivityMainBinding
    private var mediaPlayer: MediaPlayer? = null
    private var mediaController: MediaController? = null
    private var textureView: TextureView? = null
    private var frameView: FrameLayout? = null
    private var selection = "None"

    //private val videoResId = R.raw.apink // Replace with your video resource ID
    //private val videoResId = R.raw.microscope
    //private val videoResId = R.raw.lion
    private val videoResId = R.raw.cat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hideSystemUi()

        //calculate the physical screen area for left and right video
        val items = listOf("None", "Left Half", "Right Half")

        val adapter = ArrayAdapter(this, R.layout.spinner_item, items)
        binding.spinner.adapter = adapter
        binding.spinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    val selectedItem = parent?.getItemAtPosition(position).toString()
                    Log.d(TAG, "Spinner selected item: $selectedItem")
                    selection = parent?.getItemAtPosition(position).toString()
                    val rect = getScreenRectAreaFromAspectRatio(widthRatio, heightRatio, selection)
                    if (rect == null) {
                        Log.d(TAG, "getScreenRectAreaFromAspectRatio return null")
                        return
                    } else {
                        Log.d(
                            TAG,
                            "getScreenRectAreaFromAspectRatio return rect: ${rect.left}, ${rect.top}, ${rect.right}, ${rect.bottom}"
                        )
                        Log.d(
                            TAG,
                            "Projector's physical projecting area width=${rect.width()}, height=${rect.height()}"
                        )
                    }
                    when (selectedItem) {
                        "None" -> {
                            Log.d(TAG, "None selected")
                        }

                        "Left Half" -> {
                            if (rect != null) {
                                removePlayer()
                                addPlayer(rect)
                            }
                        }

                        "Right Half" -> {
                            if (rect != null) {
                                removePlayer()
                                addPlayer(rect)
                            }

                        }
                    }
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                    // Do nothing
                }
            }
    }

    private fun hideSystemUi() {
        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    private fun getScreenResolution(): Pair<Int, Int> {
        val display = windowManager.defaultDisplay
        val metrics = android.util.DisplayMetrics()
        var screen: Pair<Int, Int>
        try {
            val method = Display::class.java.getMethod("getRealMetrics", android.util.DisplayMetrics::class.java)
            method.invoke(display, metrics)
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            Log.d(TAG, "screen width=$width, height=$height")
            screen = Pair(width, height)
        } catch (e: Exception) {
            // Fallback
            display.getMetrics(metrics)
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            screen = Pair(width, height)
            Log.d(TAG, "screen width=$width, height=$height (fallback, may exclude nav bar)")
        }
        return screen
    }

    private fun getScreenRectAreaFromAspectRatio(
        width: Int,
        height: Int,
        whichHalf: String,
    ): Rect? {
        val screenResolution =  getScreenResolution()
        val screenWidth = screenResolution.first
        val screenHeight = screenResolution.second
        val screenAspectRatio = screenWidth.toFloat() / screenHeight
        val targetAspectRatio = width.toFloat() / height

        if (screenAspectRatio > targetAspectRatio) {
            // Screen is wider than target aspect ratio (landscape)
            val scaledWidth = (screenHeight * targetAspectRatio).toInt()
            when (whichHalf) {
                "Left Half" -> {
                    val left = (screenWidth - scaledWidth)
                    return Rect(left, 0, left + scaledWidth, screenHeight)
                }

                "Right Half" -> {
                    val left = 0
                    return Rect(left, 0, left + scaledWidth, screenHeight)
                }
            }
            return null
        } else {
            // Screen is taller than target aspect ratio (portrait)
            //todo: portrait screen orientation not tested yet
            val scaledHeight = (screenWidth / targetAspectRatio).toInt()
            val top = (screenHeight - scaledHeight) / 2
            return Rect(0, top, screenWidth, top + scaledHeight)
        }
    }

    private fun removePlayer() {
        runOnUiThread {
            textureView?.apply {
                binding.main.removeView(this)
            }.also {
                textureView = null
            }
            frameView?.apply {
                binding.main.removeView(this)
            }.also {
                frameView = null
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addPlayer(rect: Rect) {

        mediaPlayer = MediaPlayer.create(this, videoResId)
        mediaPlayer?.isLooping = true
        textureView = TextureView(this)
        textureView?.isClickable = true
        textureView?.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int,
            ) {
                Log.d(TAG, "onSurfaceTextureAvailable+++")
                mediaPlayer?.setSurface(android.view.Surface(surface))
                mediaPlayer?.setOnPreparedListener {
                    Log.d(TAG, "MediaPlayer onPreparedListener")
                    mediaController = MediaController(this@MainActivity)
                    mediaController?.setMediaPlayer(object : MediaController.MediaPlayerControl {
                        override fun canPause(): Boolean = true
                        override fun canSeekBackward(): Boolean = true
                        override fun canSeekForward(): Boolean = true
                        override fun getAudioSessionId(): Int = mediaPlayer?.audioSessionId ?: 0
                        override fun getBufferPercentage(): Int {
                            return 0
                        }

                        override fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0
                        override fun getDuration(): Int = mediaPlayer?.duration ?: 0
                        override fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false
                        override fun pause() {
                            mediaPlayer?.pause()
                        }

                        override fun seekTo(pos: Int) {
                            mediaPlayer?.seekTo(pos)
                        }

                        override fun start() {
                            mediaPlayer?.start()
                        }
                    })
                    mediaController?.setAnchorView(textureView)
                    mediaController?.isEnabled = true
                    mediaPlayer?.start()
                }
                testMatrix()
                Log.d(TAG, "onSurfaceTextureAvailable---")
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                Log.d(TAG, "onSurfaceTextureDestroyed")
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        it.stop()
                    }
                    it.release()
                    mediaPlayer = null
                    mediaController = null
                }
                return true
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int,
            ) {
                Log.d(TAG, "onSurfaceTextureSizeChanged")
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                //Log.d(TAG, "onSurfaceTextureUpdated")
            }
        }

        runOnUiThread {
            Log.d(
                TAG,
                "addPlayer at (${rect.left}, ${rect.top}), size = ${rect.width()} x ${rect.height()}"
            )
            frameView = FrameLayout(this)
            frameView?.let { fv ->
                binding.main.addView(fv)
                fv.layoutParams.width = rect.width()
                fv.layoutParams.height = rect.height()
                fv.x = rect.left.toFloat()
                fv.y = rect.top.toFloat()
                fv.background = getDrawable(R.drawable.rectangular_frame)
            }
            textureView?.let { tv ->
                binding.main.addView(tv)
                tv.layoutParams.width = rect.width()
                tv.layoutParams.height = rect.height()
                tv.x = rect.left.toFloat()
                tv.y = rect.top.toFloat()
                textureView?.setOnTouchListener { view, motionEvent ->
                    //Log.d(TAG, "leftHand onTouch: " + motionEvent.action)
                    if (motionEvent.action == android.view.MotionEvent.ACTION_UP) {
                        if (mediaController?.isShowing == true) {
                            mediaController?.hide()
                        } else {
                            mediaController?.show()
                        }
                    }
                    false
                }
                /*tv.viewTreeObserver.addOnGlobalLayoutListener(object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        Log.d(TAG, "onGlobalLayout+++")
                        Log.d(TAG, "onGlobalLayout---")
                    }
                })*/
            }
        }
    }

    private fun testMatrix() {

        Log.d(TAG, "testMatrix+++")

        val matrix = Matrix()
        Log.d(TAG,"video size: ${mediaPlayer?.videoWidth} x ${mediaPlayer?.videoHeight}")
        val videoWidth = mediaPlayer?.videoWidth ?: 1
        val videoHeight = mediaPlayer?.videoHeight ?: 1
        val singleProjectorWidth = textureView?.width ?: 1
        val singleProjectorHeight = textureView?.height ?: 1
        Log.d(TAG,"projector size: ${singleProjectorWidth} x ${singleProjectorHeight}")
        val totalEffectiveProjectorWidth =
            singleProjectorWidth * 2 - singleProjectorWidth * overlappingRatio / widthRatio
        val totoalEffectiveProjecotrHeight = singleProjectorHeight
        val overlappingEffectiveWidth = singleProjectorWidth * overlappingRatio / widthRatio
        Log.d(
            TAG,
            "totalEffectiveProjectorWidth=$totalEffectiveProjectorWidth, totoalEffectiveProjecotrHeight=$totoalEffectiveProjecotrHeight, overlappingEffectiveWidth=$overlappingEffectiveWidth"
        )
        //calculate scale to fit two projectors area
        val scaleX = totalEffectiveProjectorWidth.toFloat() / videoWidth
        val scaleY = totoalEffectiveProjecotrHeight.toFloat() / videoHeight
        val scale = Math.min(scaleX, scaleY)
        Log.d(TAG, "scaleX=$scaleX, scaleY=$scaleY, chosen scale=$scale")

        //move video to align 0, 0 in view
        matrix?.preTranslate(0f, 0f)
        //scale back to original size (ratio), otherwise it is fitXY initially
        matrix?.preScale(videoWidth / singleProjectorWidth.toFloat(), videoHeight / singleProjectorHeight.toFloat(), 0f, 0f)
        //keep aspect ratio and fit either width or height of projector whoever first reached
        matrix?.postScale(scale, scale, 0f, 0f)
        val scaledVieoWidth = videoWidth * scale
        val scaledVideoHeight = videoHeight * scale
        Log.d(TAG,"scaled video size: ${scaledVieoWidth} x ${scaledVideoHeight}")
        //move video left to right for left projector and right to left for right projector
        //to make two projectors overlaping for overlappingEffectiveWidth
        var translateX = 0f
        var translateY = 0f
        if (selection == "Left Half") {
            //left projector
            translateX = singleProjectorWidth - overlappingEffectiveWidth / 2f - scaledVieoWidth / 2f
            translateY = (singleProjectorHeight - scaledVideoHeight) / 2f
        } else if (selection == "Right Half") {
            //right projector
            translateX = -scaledVieoWidth / 2f + overlappingEffectiveWidth / 2f
            translateY = (singleProjectorHeight - scaledVideoHeight) / 2f
        }
        Log.d(TAG, "translateX=$translateX, translateY=$translateY")
        matrix?.postTranslate(translateX, translateY)
        textureView?.setTransform(matrix)
        textureView?.postInvalidate()

        Log.d(TAG, "testMatrix---")
    }
}
