package com.example.ccapp.Adapters

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.example.ccapp.R
import com.example.ccapp.dbClasses.Notification
import com.example.ccapp.dbClasses.Ride
import com.example.ccapp.dbClasses.User
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.item_review.view.*
import java.io.File

class ManageAdapter (var userList: List<User>, var rideId: String, var context: Context) : RecyclerView.Adapter<ManageAdapter.ManageViewHolder>(){

    inner class ManageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private lateinit var rideRef: DatabaseReference

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.manage_user_item,parent,false)
        return ManageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ManageViewHolder, position: Int) {
        holder.itemView.apply {
            txPersonName.text = userList[position].name + " " + userList[position].surname
            var imageRef = FirebaseDatabase.getInstance("https://ccapp-22f27-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("user/"+userList[position].userID)
            imageRef.addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    var user = snapshot.getValue(User::class.java)!!
                    if (!user.pictureUrl.isNullOrBlank()){
                        var ref = FirebaseStorage.getInstance().reference.child(user.pictureUrl!!)
                        var localImage = File.createTempFile("profile", "jpg")
                        ref.getFile(localImage).addOnSuccessListener {
                            holder.itemView.findViewById<ImageView>(R.id.ivAvatarManage).setImageURI(localImage.toUri())
                        }
                    }

                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
        holder.itemView.findViewById<Button>(R.id.btnDelete).setOnClickListener {
            var index = holder.position
            var userId = userList[index].userID

            //instanciate AlertDialogBuilder
            val builder: AlertDialog.Builder = AlertDialog.Builder(context)
            builder.setTitle("Confirm")
            builder.setMessage("Are you sure you want to remove this passenger?")

            //set positive and negative
            builder.setPositiveButton(
                "YES",
                DialogInterface.OnClickListener { dialog, which -> // Do nothing but close the dialog
                    rideRef =
                        FirebaseDatabase.getInstance("https://ccapp-22f27-default-rtdb.europe-west1.firebasedatabase.app/")
                            .getReference("ride/" + rideId!!)

                    //opening the ride to remove the user from the passengerList
                    rideRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            var ride = snapshot.getValue(Ride::class.java)!!
                            ride.passengers.remove(userId)
                            //save the modified ride to the database
                            rideRef.setValue(ride)

                            //finding the user to delete from its rides the current ride
                            var userRef =
                                FirebaseDatabase.getInstance("https://ccapp-22f27-default-rtdb.europe-west1.firebasedatabase.app/")
                                    .getReference("user").child(userId!!)
                            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    var pass = dataSnapshot.getValue(User::class.java)!!
                                    pass.ridesAsPassenger.remove(rideId)
                                    userRef.child(userId!!).setValue(pass)
                                }

                                override fun onCancelled(databaseError: DatabaseError) {
                                }
                            })

                            //create a notification on the database
                            val notRef = FirebaseDatabase.getInstance("https://ccapp-22f27-default-rtdb.europe-west1.firebasedatabase.app/")
                                .getReference("notification").push()
                            notRef.setValue(Notification(userId, "You have been deleted by the driver from the ride from ${ride.departure} to ${ride.destination}!"))
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
                    dialog.dismiss()
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

    override fun getItemCount(): Int {
        return userList.size
    }
}
