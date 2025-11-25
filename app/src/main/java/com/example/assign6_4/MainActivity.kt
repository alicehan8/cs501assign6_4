package com.example.assign6_4

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.assign6_4.ui.theme.Assign6_4Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Assign6_4Theme {
                TiltBallGame()
            }
        }
    }
}

@Composable
fun TiltBallGame() {
    val context = LocalContext.current
    var ballX by remember { mutableFloatStateOf(200f) }
    var ballY by remember { mutableFloatStateOf(200f) }
    var velocityX by remember { mutableFloatStateOf(0f) }
    var velocityY by remember { mutableFloatStateOf(0f) }
    var accelX by remember { mutableFloatStateOf(0f) }
    var accelY by remember { mutableFloatStateOf(0f) }
    val ballRadius = 30f

    // Maze walls - simple visible layout
    val walls = remember {
        listOf(
            Wall(100f, 300f, 40f, 700f),   // Vertical wall on left
            Wall(600f, 300f, 40f, 700f),   // Vertical wall on right
            Wall(100f, 800f, 600f, 40f)    // Horizontal wall
        )
    }

    // Set up sensor listener
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        // Apply low-pass filter to smooth sensor data
                        val alpha = 0.8f
                        accelX = alpha * accelX + (1 - alpha) * it.values[0]
                        accelY = alpha * accelY + (1 - alpha) * it.values[1]
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Game loop
    LaunchedEffect(Unit) {
        var lastTime = withFrameMillis { it }
        while (true) {
            withFrameMillis { currentTime ->
                val deltaTime = (currentTime - lastTime) / 1000f // seconds
                lastTime = currentTime

                // Physics constants
                val acceleration = 300f
                val friction = 0.95f
                val maxVelocity = 500f

                // Update velocity based on tilt
                velocityX += -accelX * acceleration * deltaTime
                velocityY += accelY * acceleration * deltaTime

                // Apply friction
                velocityX *= friction
                velocityY *= friction

                // Clamp velocity
                velocityX = velocityX.coerceIn(-maxVelocity, maxVelocity)
                velocityY = velocityY.coerceIn(-maxVelocity, maxVelocity)

                // Calculate next position
                var nextX = ballX + velocityX * deltaTime
                var nextY = ballY + velocityY * deltaTime

                // Check wall collisions
                for (wall in walls) {
                    if (nextX + ballRadius > wall.x && nextX - ballRadius < wall.x + wall.width &&
                        nextY + ballRadius > wall.y && nextY - ballRadius < wall.y + wall.height) {

                        // Check which side we hit
                        if (ballX + ballRadius <= wall.x || ballX - ballRadius >= wall.x + wall.width) {
                            velocityX = -velocityX * 0.5f // bounce with energy loss
                            nextX = ballX
                        }
                        if (ballY + ballRadius <= wall.y || ballY - ballRadius >= wall.y + wall.height) {
                            velocityY = -velocityY * 0.5f
                            nextY = ballY
                        }
                    }
                }

                // Boundary collision with screen edges
                if (nextX - ballRadius < 0f) {
                    nextX = ballRadius
                    velocityX = -velocityX * 0.5f
                } else if (nextX + ballRadius > 1080f) { // adjust to screen width if needed
                    nextX = 1080f - ballRadius
                    velocityX = -velocityX * 0.5f
                }

                if (nextY - ballRadius < 0f) {
                    nextY = ballRadius
                    velocityY = -velocityY * 0.5f
                } else if (nextY + ballRadius > 2400f) { // adjust to screen height if needed
                    nextY = 2400f - ballRadius
                    velocityY = -velocityY * 0.5f
                }

                ballX = nextX
                ballY = nextY
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDEDED))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw walls
            for (wall in walls) {
                drawRect(
                    Color.DarkGray,
                    topLeft = androidx.compose.ui.geometry.Offset(wall.x, wall.y),
                    size = androidx.compose.ui.geometry.Size(wall.width, wall.height)
                )
            }

            // Draw ball
            drawCircle(
                Color.Red,
                radius = ballRadius,
                center = androidx.compose.ui.geometry.Offset(ballX, ballY)
            )
        }
    }
}

data class Wall(val x: Float, val y: Float, val width: Float, val height: Float)