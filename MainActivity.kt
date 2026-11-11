package com.example.metrica360

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import kotlin.math.atan2
import kotlin.math.roundToInt

class MainActivity : Activity(), SensorEventListener {

    private lateinit var sm: SensorManager
    private lateinit var levelView: LevelView
    private var gx = 0f
    private var gy = 9.8f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        levelView = LevelView(this)
        setContentView(levelView)
        sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun onResume() {
        super.onResume()
        // TYPE_GRAVITY já é fundido com o giroscópio; se não existir, usa o acelerômetro
        val s = sm.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (s != null) {
            sm.registerListener(this, s, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sm.unregisterListener(this)
    }

    override fun onSensorChanged(e: SensorEvent) {
        // filtro passa-baixa pra suavizar
        val a = 0.15f
        gx += a * (e.values[0] - gx)
        gy += a * (e.values[1] - gy)
        // 0° = celular em pé, reto. Se o sinal ficar invertido, troque -gx por gx
        val angle = Math.toDegrees(atan2(-gx.toDouble(), gy.toDouble())).toFloat()
        levelView.angle = angle
        levelView.invalidate()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

class LevelView(context: Context) : View(context) {

    var angle = 0f

    private val white = Paint().apply { color = Color.WHITE }
    private val black = Paint().apply { color = Color.BLACK }
    private val green = Paint().apply { color = Color.rgb(52, 199, 89) }
    private val line = Paint().apply {
        color = Color.WHITE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val text = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2
        val cy = h / 2
        val big = maxOf(w, h) * 2
        // linha do horizonte fica acima do número (número sempre sobre o preto/verde)
        val ly = cy - w * 0.16f
        // escala 0..359: em pé = 90, esquerda = 0, direita = 180, de cabeça pra baixo = 270
        val deg = ((angle + 90f).roundToInt() % 360 + 360) % 360
        val nivelado = deg % 90 == 0

        c.drawColor(Color.BLACK)

        // tudo gira junto: metade branca/preta, tracinhos e número
        c.save()
        c.rotate(-angle, cx, cy)
        c.drawRect(cx - big, cy - big, cx + big, ly, white)
        c.drawRect(cx - big, ly, cx + big, cy + big, if (nivelado) green else black)

        // tracinhos laterais (na altura do centro do número)
        c.drawLine(w * 0.19f, cy, w * 0.25f, cy, line)
        c.drawLine(w * 0.75f, cy, w * 0.81f, cy, line)

        // número no centro
        text.textSize = w * 0.18f
        val y = cy - (text.descent() + text.ascent()) / 2
        c.drawText("$deg°", cx, y, text)
        c.restore()
    }
}
