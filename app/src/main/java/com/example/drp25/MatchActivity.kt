package com.example.drp25

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.ScrollView
import android.widget.TextView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MatchActivity : AppCompatActivity() {
    private lateinit var parentLayout: LinearLayout
    private lateinit var context: Context
    private var selectedMatchName: String? = null

    private val observer = object : Observer {
        override fun notify(matchIds: Set<String>) {
            parentLayout.removeAllViews()
            for (matchId in matchIds) {
                val scrollView = ScrollView(context)
                parentLayout.addView(scrollView)
                val linearLayout = LinearLayout(context)
                linearLayout.orientation = LinearLayout.VERTICAL
                linearLayout.setPadding(30, 30, 30, 30)
//                parentLayout.addView(linearLayout)
                scrollView.addView(linearLayout)
                val matchRef = FirebaseDatabase.getInstance().reference.child("universities")
                    .child(UNI_ID).child("users").child(matchId)
                matchRef.addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val name: String = snapshot.child("name").value as String
                        val nationality = snapshot.child("nationality").value
                        val course = snapshot.child("course").value
                        val year = snapshot.child("year").value

                        addText(linearLayout, "Name: $name")
                        addText(linearLayout, "Nationality: $nationality")
                        addText(linearLayout, "Course: $course")
                        addText(linearLayout, "Year: $year")

                        addText(linearLayout, "Interests:")
                        for (interest in snapshot.child("interests").children) {
                            addText(linearLayout, interest.key + " (" + interest.value + " stars)")

                            val ratingBar = RatingBar(context)
                            ratingBar.layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            ratingBar.numStars = 5
                            ratingBar.rating = interest.getValue(Float::class.java)!!
                            ratingBar.setIsIndicator(true)

                            linearLayout.addView(ratingBar)

                        }

                        val button = getButton(linearLayout, "Match with $name")
                        linearLayout.addView(button)
                        button.setOnClickListener {
                            selectedMatchName = name
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }

                })
            }
        }
    }

    private fun addText(linearLayout: LinearLayout, text: String) {
        val entry = TextView(context)
        entry.text = text
        entry.setPadding(16, 16, 16, 16)
        entry.textSize = 20f
        linearLayout.addView(entry)
    }

    private fun getButton(linearLayout: LinearLayout, text: String): Button {
        val btn = Button(context)
        btn.text = text
        btn.setPadding(16, 16, 16, 16)
        btn.textSize = 20f
        return btn
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match)

        context = this
        parentLayout = findViewById(R.id.match_matches)
        addMatchObserver(observer)

        /* Functionality for SEND button -> takes user to chat page. */
        val sendBtn = findViewById<Button>(R.id.match_send_button)
        sendBtn.setOnClickListener { view ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("fromMatch", true)
            intent.putExtra("matchedName", selectedMatchName)
            startActivity(intent)
        }

        // Set up stamps
        val stamp1 = findViewById<ImageView>(R.id.stamp_option_1)
        stamp1.setImageResource(R.drawable.china_flag_stamp)
        val stamp2 = findViewById<ImageView>(R.id.stamp_option_2)
        stamp2.setImageResource(R.drawable.china_tourist_stamp)
        val stamp3 = findViewById<ImageView>(R.id.stamp_option_3)
        stamp3.setImageResource(R.drawable.china_food_stamp)
    }

}