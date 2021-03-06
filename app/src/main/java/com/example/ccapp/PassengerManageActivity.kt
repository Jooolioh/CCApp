package com.example.ccapp

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ccapp.Adapters.ManageAdapter
import com.example.ccapp.dbClasses.Notification
import com.example.ccapp.dbClasses.Ride
import com.example.ccapp.dbClasses.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*


class PassengerManageActivity : AppCompatActivity() {

    private lateinit var rideId : String
    private lateinit var rvManage: RecyclerView
    private lateinit var ride: Ride

    private lateinit var rideReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passenger_manage)
        rideId = intent.getStringExtra("ride_id").toString()

        var userList = mutableListOf<User>()
        val adapter = ManageAdapter(userList, rideId, this)
        rvManage = findViewById(R.id.rvManage)
        rvManage.layoutManager = LinearLayoutManager(this)
        rvManage.adapter = adapter

        rideReference =
            FirebaseDatabase.getInstance("https://ccapp-22f27-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("ride/" + rideId!!)
        val userRef =
            FirebaseDatabase.getInstance("https://ccapp-22f27-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("user")

        //populate the list of passenger
        //get the ride reference
        //clean the adapter
        //fill it again with the passengers
        rideReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                adapter.notifyDataSetChanged()
                //get ride from database
                ride = snapshot.getValue(Ride::class.java)!!

                //get passengers and add the to the userList
                for (pas in ride.passengers) {
                    userRef.child(pas)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                var passenger = dataSnapshot.getValue(User::class.java)!!
                                userList.add(passenger)
                                adapter.notifyDataSetChanged()
                            }
                            override fun onCancelled(databaseError: DatabaseError) {
                            }
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.ride_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }


    //menu to delete the ride
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.remove -> {
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)

                builder.setTitle("Confirm")
                builder.setMessage("Are you sure you want to delete this ride?")

                builder.setPositiveButton(
                    "YES",
                    DialogInterface.OnClickListener { dialog, which -> // Do nothing but close the dialog
                        var userRef =
                            FirebaseDatabase.getInstance("https://ccapp-22f27-default-rtdb.europe-west1.firebasedatabase.app/")
                                .getReference("user")

                        // get all of the passenger
                        rideReference.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                ride = snapshot.getValue(Ride::class.java)!!
                                for (pas in ride.passengers) { //remove link from passenger
                                    userRef.child(pas)
                                        .addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                                var passenger = dataSnapshot.getValue(User::class.java)!!
                                                passenger.ridesAsPassenger.remove(rideId)
                                                userRef.child(pas).setValue(passenger)
                                            }
                                            override fun onCancelled(databaseError: DatabaseError) {
                                            }
                                        })
                                    //notification for each deleted passenger
                                    val notRef = FirebaseDatabase.getInstance("https://ccapp-22f27-default-rtdb.europe-west1.firebasedatabase.app/")
                                        .getReference("notification").push()
                                    notRef.setValue(Notification(pas, "The ride from ${ride.departure} to ${ride.destination} has been deleted by the driver!"))
                                }
                                //remove link from driver
                                userRef.child(FirebaseAuth.getInstance().currentUser?.uid!!).addListenerForSingleValueEvent(
                                    object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            var tmp = snapshot.getValue(User::class.java)!!
                                            tmp.ridesAsDriver.remove(intent.getStringExtra("ride_id").toString())
                                            userRef.child(FirebaseAuth.getInstance().currentUser?.uid!!).setValue(tmp)
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            TODO("Not yet implemented")
                                        }
                                    })

                                //delete the whole ride
                                rideReference.removeValue()
                            }

                            override fun onCancelled(error: DatabaseError) {
                            }

                        })

                        dialog.dismiss()
                        val intent = Intent(this@PassengerManageActivity, HomeActivity::class.java)
                        startActivity(intent)
                        finish()
                    })

                builder.setNegativeButton(
                    "NO",
                    DialogInterface.OnClickListener { dialog, which -> // Do nothing
                        dialog.dismiss()

                    })

                //show the dialog
                val alert: AlertDialog = builder.create()
                alert.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}