package com.example.ccapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

// displayed after the RideDialogueActivity, i.e. when a new ride is created by the driver
class ConfirmationActivity : AppCompatActivity() {

    private lateinit var btnHome: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmation)

        btnHome = findViewById(R.id.btnHome)

        btnHome.setOnClickListener {
            val intent = Intent(this@ConfirmationActivity, HomeActivity::class.java)
            finish()
            startActivity(intent)
        }
    }


}