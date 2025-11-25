package com.example.assign6_4

import android.content.pm.ActivityInfo
import android.hardware.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.assign6_4.ui.theme.Assign6_4Theme
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null
    private var _gx by mutableFloatStateOf(0f)
    private var _gy by mutableFloatStateOf(0f)
    private var _gz by mutableFloatStateOf(0f)
    private var _accuracy by mutableStateOf("Unknown")

    private var lastTimestamp = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        setContent {
            Assign6_4Theme {
                Maze(_gx, _gy)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                val dt = if (lastTimestamp == 0L) {
                    0f
                } else {
                    (event.timestamp - lastTimestamp) / 1_000_000_000f
                }
                lastTimestamp = event.timestamp
                _gx += it.values[0] * dt
                _gy += it.values[1] * dt
                _gz += it.values[2] * dt

                // clamp
                _gx *= 0.9f
                _gy *= 0.9f
                _gz *= 0.9f
            }

        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

@Composable
fun Maze(gx : Float, gy : Float) {
    var x by rememberSaveable { mutableStateOf(900f) }
    var y by rememberSaveable { mutableStateOf(200f) }
    var ballRad = 40f
    //i asked to do this part but its really bad at it
    val walls = listOf(
        Rect(950f, 300f, 1000f, 1000f),
        Rect(90f, 300f, 790f, 350f),
        Rect(790f, 500f, 1000f, 550f),
        Rect(590f, 300f, 640f, 600f),
        Rect(400f, 600f, 550f, 650f),
        Rect(290f, 300f, 340f, 450f),
        Rect(290f, 450f, 440f, 500f),
        Rect(90f, 1000f, 1000f, 1050f),
        Rect(90f, 300f, 140f, 850f),
        Rect(400f, 800f, 750f, 850f),
        Rect(90f, 700f, 290f, 750f),
        Rect(290f, 650f, 340f, 850f),
    )


    Canvas(
        modifier = Modifier.fillMaxSize()
    )
    {
        val sens = 50f
        val dx = gy * sens
        val dy = (-1 * gx) * sens

        x = (x + dx).coerceIn(ballRad, size.width - ballRad)
        y = (y + dy).coerceIn(ballRad, size.height - ballRad)

        for (wall in walls) {
            if (detectCollision(x, y, ballRad, wall)) {
                when {
                    x < wall.left -> x = wall.left - ballRad
                    x > wall.right -> x = wall.right + ballRad
                    y < wall.top -> y = wall.top - ballRad
                    y > wall.bottom -> y = wall.bottom + ballRad
                }
            }
        }

        for (wall in walls) {
            drawRect(
                color = Color.Gray,
                topLeft = Offset(wall.left, wall.top),
                size = androidx.compose.ui.geometry.Size(wall.width, wall.height)
            )
        }
        drawCircle(
            color = Color.Cyan,
            radius = ballRad,
            center = Offset(x, y)
        )
    }

}


private fun detectCollision(cx: Float, cy: Float, r: Float, rect: Rect) : Boolean {
    val px = maxOf(rect.left, minOf(cx, rect.right))
    val py = maxOf(rect.top, minOf(cy, rect.bottom))
    val dx = cx - px
    val dy = cy - py
    val dist = sqrt(dx * dx + dy * dy)
    return (dist < r)
}


