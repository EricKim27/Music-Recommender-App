package com.example.musicrecommend

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.activity.viewModels
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.TextView
import androidx.core.app.NotificationCompat

/*
You will need to edit all the example.com to your own url.
The url should be the address to the server which contains the api server.
The API server code can be found here: https://github.com/EricKim27/AI-music-recommender-backend
 */

class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private val myViewModel: MyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        runnable = object : Runnable {
            override fun run() {
                myViewModel.viewModelScope.launch {
                    println(myViewModel.Verify())
                    if(myViewModel.Verify() == 0)
                    {
                        val parsed = myViewModel.getJson("http://example.com:3000/api/getdata", "emotion")
                        println(parsed)
                        val url: String = set_url(parsed)
                        sendNotification(this@MainActivity, "Music_channel", "당신을 위한 음악", "현재 감정에 알맞는 플레이리스트", url)
                        withContext(Dispatchers.Main) {
                            val text = findViewById<TextView>(R.id.Status)
                            text.text = "Application is Running"
                            val emotion = findViewById<TextView>(R.id.emotion)
                            emotion.text = parsed ?: "No emotion data"
                        }
                    }
                }
                handler.postDelayed(this, 10000)
            }
        }

        handler.post(runnable)
    }
    override fun onDestroy() {
        super.onDestroy()
        // Stop the loop when the activity is destroyed
        handler.removeCallbacks(runnable)
    }
    fun set_url(emotion: String?) : String{
        var ret : String = "default"

        when(emotion) {
            "Happy" ->
                ret = "https://www.youtube.com/watch?v=vfWwL4N8CM4"
            "Sad" ->
                ret = "https://www.youtube.com/watch?v=uQ47sUutrRU"
            "Angry" ->
                ret = "https://www.youtube.com/watch?v=71hZutqP_cM"
            "Boring" ->
                ret = "https://www.youtube.com/watch?v=ygtlUylfoy8"
            "Scared" ->
                ret = "https://www.youtube.com/watch?v=yB7N3PFJUew"
        }
        return ret
    }
    fun sendNotification(
        context: Context,
        channelId: String,
        title: String,
        message: String,
        webUrl: String
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a notification channel (required for Android Oreo and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel Name",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open the web link
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
        val pendingWebIntent =
            PendingIntent.getActivity(context, 0, webIntent, PendingIntent.FLAG_IMMUTABLE)

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your app's icon
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_view, // Icon for the button
                "Open Link", // Button text
                pendingWebIntent
            )

        // Show the notification
        notificationManager.notify(0, notificationBuilder.build())
    }
}

class MyViewModel : ViewModel() {
    suspend fun getJson(url: String, Id:String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val fetched = URL(url).readText()
                val parsed = JSONObject(fetched)
                parsed.getString(Id)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun Verify(): Int? {
        val ret = getJson("http://example.com:3000/api/status", "status") //modify the url as your own url.
        return ret?.toIntOrNull()
    }
}