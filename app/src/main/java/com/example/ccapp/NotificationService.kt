package com.example.ccapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random


class NotificationService : Service() {

    private var notificationManager: NotificationManager? = null
    private lateinit var userID : String

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(
            "com.ccapp.news",
            "CCApp News",
            "CCApp Changes"
        )

        // get Firebase reference
        var notificationRef = FirebaseDatabase.getInstance("https://ccapp-22f27-default-rtdb.europe-west1.firebasedatabase.app/").getReference("notification")


        notificationRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userID = FirebaseAuth.getInstance().currentUser?.uid!!.toString()
                for (notification in snapshot.children){
                    var not = notification.getValue(com.example.ccapp.dbClasses.Notification::class.java)
                    if(not != null){
                        if (not.userId == userID) {
                            // send notifications relevant for the current user
                            sendNotification(not.message)
                            // delete notification in the database
                            notificationRef.child(notification.key!!).removeValue()
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
        Log.d("notification", "onCreate")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("notification", "onDestroy")
    }

    // setup of notification channel
    private fun createNotificationChannel(id: String, name: String, description: String) {
        val importance = NotificationManager.IMPORTANCE_LOW
        var channel = NotificationChannel(id, name, importance)
        channel.description = description
        channel.enableLights(true)
        channel.lightColor = Color.RED
        notificationManager?.createNotificationChannel(channel)
    }

    fun sendNotification(text: String) {
        val notificationID = Random.nextInt(2000)
        val channelID = "com.ccapp.news"
        val notification = Notification.Builder(this@NotificationService, channelID)
            .setContentTitle("CCApp")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setChannelId(channelID)
            .build()
        notificationManager?.notify(notificationID, notification)
    }
}