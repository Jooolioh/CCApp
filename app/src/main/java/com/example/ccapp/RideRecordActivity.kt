package com.example.ccapp

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ccapp.Adapters.PassengerAdapter
import com.example.ccapp.dbClasses.Notification
import com.example.ccapp.dbClasses.Passenger
import com.example.ccapp.dbClasses.Ride
import com.example.ccapp.dbClasses.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_ride_record.*
import java.io.File
import java.util.Locale
import java.util.Currency
import android.content.Intent as Intent


class RideRecordActivity : AppCompatActivity() {

    private lateinit var mDbRef : DatabaseReference
    private lateinit var ride : Ride
    private lateinit var adapter : PassengerAdapter
    private lateinit var passengerList : MutableList<Passenger>
    private lateinit var btnAction : Button


    private lateinit var userType : String
    private lateinit var id: String

    private lateinit var departure: TextView
    private lateinit var destination: TextView
    private lateinit var drivername: TextView
    private lateinit var dateTime: TextView
    private lateinit var price: TextView
    private lateinit var profileImage: ImageView




    //Activity is used for in the following cases:
    // if passenger:
    //  -join
    //  -unjoin
    // if driver:
    //  -can open the manageActivity
    // if the ride has expired
    //  - everyone can open the review activity
    //
    // so we need to change the clickListener of the btnAction
    // and its text

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ride_record)

        id = intent.getStringExtra("ride_id").toString()
        //need to distinguish if:
        // - user not joined
        // - user is passenger
        // - user is driver
        // - the drive has expired and the user needs to review
        userType = intent.getStringExtra("user_type").toString()
        var user: User


        //referencing the data
        departure = findViewById<TextView>(R.id.txtRideRecordDeparture)
        destination = findViewById<TextView>(R.id.txtRideRecordDestination)
        drivername = findViewById<TextView>(R.id.txtRideRecordDriverName)
        dateTime = findViewById<TextView>(R.id.txtRideRecordDateTime)
        price = findViewById<TextView>(R.id.txtRideRecordPrice)
        profileImage = findViewById(R.id.riderecordImage)


        // Formatting for currency string
        val currency = Currency.getInstance("EUR")
        val currencyFormat = java.text.NumberFormat.getCurrencyInstance(Locale.ITALY)
        currencyFormat.currency = currency

        btnAction = findViewById(R.id.btn_action)

        passengerList = mutableListOf<Passenger>()
        adapter = PassengerAdapter(passengerList, this)
        rvPassengers.adapter = adapter

        // Insert line breaks for long texts
        departure.text = departure.text.toString().replace(',','\n')
        destination.text = destination.text.toString().replace(',','\n')

        // Create LinearLayoutManager without horizontal scrolling
        val layoutManager = object : LinearLayoutManager(this) {
            override fun canScrollHorizontally(): Boolean {
                return false
            }
        }

        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        rvPassengers.layoutManager = layoutManager

        mDbRef = FirebaseDatabase.getInstance("https://ccapp-22f27-default-rtdb.europe-west1.firebasedatabase.app/").getReference("ride/" + id!!)
        mDbRef.addValueEventListener(object: ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                ride = snapshot.getValue(Ride::class.java)!!

                // Link to the Driver Profile
                llRideRecordDriverName.setOnClickListener() {
                    //Toast.makeText(baseContext,"Driver clicked!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@RideRecordActivity, ProfileActivity::class.java)
                    intent.putExtra("userID", ride.driverId )
                    //finish()
                    startActivity(intent)
                }


                var userRefImage =
                    FirebaseDatabase.getInstance("https://ccapp-22f27-default-rtdb.europe-west1.firebasedatabase.app/")
                        .getReference("user")

                userRefImage.child(ride.driverId!!)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            var user = snapshot.getValue(User::class.java)!!
                            if (!user.pictureUrl.isNullOrBlank()){
                                var ref = FirebaseStorage.getInstance().reference.child(user.pictureUrl!!)
                                var localImage = File.createTempFile("profile", "jpg")
                                ref.getFile(localImage).addOnSuccessListener {
                                    profileImage.setImageURI(localImage.toUri())
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            TODO("Not yet implemented")
                        }
                    })


                departure.text = ride.departure
                destination.text = ride.destination
                drivername.text = ride.driverName + " " + ride.driverSurname
                dateTime.text = ride.date + " " + ride.time
                price.text = currencyFormat.format(ride.price)



                btnAction.setOnClickListener {
                    var userRef = FirebaseDatabase.getInstance("https://ccapp-22f27-default-rtdb.europe-west1.firebasedatabase.app/")
                        .getReference("user").child(FirebaseAuth.getInstance().currentUser?.uid!!)

                    if (intent.getBooleanExtra("review", false)){
                        if(userType == null){
                            errorGoHome()
                        } else {
                            val intent = Intent(this@RideRecordActivity, ReviewActivity::class.java)
                            intent.putExtra("user_type", userType)
                            intent.putExtra("ride_id", ride.id)
                            finish()
                            startActivity(intent)
                        }
                    } else {
                        when(userType){
                            null -> {
                                errorGoHome()
                            }
                            "driver" -> {
                                val intent =
                                    Intent(this@RideRecordActivity, PassengerManageActivity::class.java)
                                intent.putExtra("ride_id", ride.id)
                                finish()
                                startActivity(intent)
                            }
                            "passenger" -> {
                                //unjoin
                                if(ride.passengers.contains(FirebaseAuth.getInstance().currentUser?.uid!!)){
                                    val builder: AlertDialog.Builder = AlertDialog.Builder(this@RideRecordActivity)
                                    builder.setTitle("Confirm")
                                    builder.setMessage("Are you sure you want to join this ride?")

                                    builder.setPositiveButton(
                                        "YES",
                                        DialogInterface.OnClickListener { dialog, which -> // Do nothing but close the dialog
                                            userRef.addListenerForSingleValueEvent(object: ValueEventListener{
                                                override fun onDataChange(snapshot: DataSnapshot){
                                                    user = snapshot.getValue(User::class.java)!!
                                                    user.ridesAsPassenger.remove(id)
                                                    userRef.setValue(user)
                                                    ride.passengers.remove(FirebaseAuth.getInstance().currentUser?.uid!!)
                                                    mDbRef.setValue(ride)

                                                    val notRef = FirebaseDatabase.getInstance("https://ccapp-22f27-default-rtdb.europe-west1.firebasedatabase.app/")
                                                        .getReference("notification").push()
                                                    notRef.setValue(Notification(ride.driverId!!, "A new passenger joined the ride from ${ride.departure} to ${ride.destination}!"))
                                                }

                                                override fun onCancelled(error: DatabaseError){}
                                            })
                                            dialog.dismiss()
                                            Toast.makeText(this@RideRecordActivity, "You successfully unjoined the ride :(", Toast.LENGTH_LONG).show()
                                            onBackPressed()
                                        })

                                    builder.setNegativeButton(
                                        "NO",
                                        DialogInterface.OnClickListener { dialog, which -> // Do nothing
                                            dialog.dismiss()
                                        })

                                    val alert: AlertDialog = builder.create()
                                    alert.show()
                                } else { //join
                                    if (ride.passengers.size >= ride.seats){
                                        Toast.makeText(baseContext, "No seats available!", Toast.LENGTH_LONG).show()
                                    } else {
                                        val builder: AlertDialog.Builder = AlertDialog.Builder(this@RideRecordActivity)
                                        builder.setTitle("Confirm")
                                        builder.setMessage("Are you sure you want to unjoin this ride?")
                                        builder.setPositiveButton(
                                            "YES",
                                            DialogInterface.OnClickListener { dialog, which -> // Do nothing but close the dialog
                                                userRef.addListenerForSingleValueEvent(object: ValueEventListener{
                                                    override fun onDataChange(snapshot: DataSnapshot){
                                                        user = snapshot.getValue(User::class.java)!!
                                                        if (!user.ridesAsPassenger.contains(id)){
                                                            user.ridesAsPassenger.add(id)
                                                            userRef.setValue(user)
                                                        }
                                                        if(!ride.passengers.contains((FirebaseAuth.getInstance().currentUser?.uid!!))){
                                                            ride.passengers.add(FirebaseAuth.getInstance().currentUser?.uid!!)
                                                            mDbRef.setValue(ride)
                                                            val notRef = FirebaseDatabase.getInstance("https://ccapp-22f27-default-rtdb.europe-west1.firebasedatabase.app/")
                                                                .getReference("notification").push()
                                                            notRef.setValue(Notification(ride.driverId!!, "A new passenger unjoined the ride from ${ride.departure} to ${ride.destination}!"))
                                                        }
                                                    }

                                                    override fun onCancelled(error: DatabaseError){}
                                                })
                                                dialog.dismiss()
                                                Toast.makeText(this@RideRecordActivity, "Thank you for joining the ride :)", Toast.LENGTH_LONG).show()
                                                var intent = Intent(this@RideRecordActivity, HomeActivity::class.java)
                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                                startActivity(intent)
                                            })

                                        builder.setNegativeButton(
                                            "NO",
                                            DialogInterface.OnClickListener { dialog, which -> // Do nothing
                                                dialog.dismiss()
                                            })

                                        val alert: AlertDialog = builder.create()
                                        alert.show()

                                    }
                                }
                            }
                        }
                    }

                }


                //changes the text of the button
                if (intent.getBooleanExtra("review", false)){
                    btnAction.text = "Review"
                } else {
                    when (userType) {
                        null -> {
                            errorGoHome()
                        }
                        "driver" -> {
                            btnAction.text = "Manage"
                        }
                        "passenger" -> {
                            if (ride.passengers.contains(FirebaseAuth.getInstance().currentUser?.uid!!)) {
                                btnAction.text = "Unjoin"
                            } else {
                                if (ride.passengers.size >= ride.seats){
                                    btnAction.isEnabled = false
                                    Toast.makeText(baseContext, "Hidden", Toast.LENGTH_LONG).show()
                                } else {
                                    btnAction.isEnabled = true
                                }
                                btnAction.text = "Join"
                            }
                        }
                    }
                }


                if (ride.seats < ride.passengers.size) {
                    errorGoHome()
                }

                passengerList.clear()
                adapter.notifyDataSetChanged()

                // Update the RecyclerView in the UI
                // Adds the cirlces of the users at the bottom the view
                var userRef = FirebaseDatabase.getInstance("https://ccapp-22f27-default-rtdb.europe-west1.firebasedatabase.app/").getReference("user")
                for (i in 1 .. (ride.seats - ride.passengers.size)) passengerList.add(Passenger(false))
                for (pas in ride.passengers) {
                    userRef.child(pas).addListenerForSingleValueEvent(object: ValueEventListener{
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            Log.d("FIREBASE1", dataSnapshot.toString())
                            var passenger = dataSnapshot.getValue(User::class.java)!!
                            passengerList.add(0, Passenger(passenger.name + " " + passenger.surname, passenger.userID))
                            adapter.notifyDataSetChanged()
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                        }
                    })
                }
            }


            override fun onCancelled(error: DatabaseError) {
            }


        })




    }

    fun errorGoHome(){
        Toast.makeText(
            baseContext, "An error has occurred",
            Toast.LENGTH_LONG
        ).show()
        val intent = Intent(this@RideRecordActivity, HomeActivity::class.java)
        finish()
        startActivity(intent)
    }

}
