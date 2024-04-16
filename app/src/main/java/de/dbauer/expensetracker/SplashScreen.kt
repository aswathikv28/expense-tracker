package de.dbauer.expensetracker

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler

import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen")
class SplashScreen : AppCompatActivity() {
    private val SPLASH_DELAY: Long = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashscreen)

        val fadeInAnimation = AnimationUtils.loadAnimation(this, androidx.appcompat.R.anim.abc_grow_fade_in_from_bottom)
        findViewById<View>(R.id.splash_layout).startAnimation(fadeInAnimation)


        Handler().postDelayed({
            startActivity(Intent(this, Login::class.java))
            finish()
        }, SPLASH_DELAY)
    }

    override fun onDestroy() {
        super.onDestroy()

        Handler().removeCallbacksAndMessages(null)
    }
}
