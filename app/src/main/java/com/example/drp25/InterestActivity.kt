package com.example.drp25

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.RatingBar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class InterestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interest)

        // Get a reference to the AutoCompleteTextView in the layout.
        val interestSelectTextView = findViewById<AutoCompleteTextView>(R.id.interestTextView)
        // Get the string array for list of autocomplete suggestions.
        val interests: Array<out String> = resources.getStringArray(R.array.interest_array)
        // Create the adapter and set it to the AutoCompleteTextView.
        ArrayAdapter(this, android.R.layout.simple_list_item_1, interests).also { adapter ->
            interestSelectTextView.setAdapter(adapter)
        }

        // Display list of selected interests
        val interestsChipGroup = findViewById<ChipGroup>(R.id.interests_group)
        displayExistingInterests(interestsChipGroup)

        // Select rating
        val interestRatingBar = findViewById<RatingBar>(R.id.interest_rating_bar)
        // Add new interest to list of selected interests
        val addInterestButton = findViewById<Button>(R.id.add_new_interest_button)
        var selectedInterest = ""
        interestSelectTextView.setOnItemClickListener { adapterView, view, pos, id ->
            selectedInterest = adapterView.getItemAtPosition(pos).toString()
        }
        addInterestButton.setOnClickListener {
            if (selectedInterest.isNotEmpty()) {
                val rating = interestRatingBar.rating.toInt()
                addChipIfNotExist(selectedInterest, rating, interestsChipGroup)
                selectedInterest = ""
            }
        }

        val selectInterestButton = findViewById<Button>(R.id.selectInterestButton)
        selectInterestButton.setOnClickListener {
            // Update database
            addSelectedInterestsToDatabase(interestsChipGroup)
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

    }

    private fun addSelectedInterestsToDatabase(pChipGroup: ChipGroup) {
        // Reset all interests
        clearInterests(UNI_ID, USER_ID)
        // Add all interests
        for (i in 0 until pChipGroup.childCount) {
            val chip = pChipGroup.getChildAt(i)
            if (chip is Chip) {
                val displayString = chip.text.toString()
                val regex = Regex("""^(.*?) \((\d+)""")
                val matchResult = regex.find(displayString)
                val (interest, stars) = matchResult?.destructured ?: throw IllegalArgumentException("Invalid input format")

                val interestValue: String = interest.trim()
                val starsValue: Int = stars.toInt()
                putInterestRating(UNI_ID, USER_ID, interestValue, starsValue)
            }
        }
    }

    private fun displayExistingInterests(pChipGroup: ChipGroup) {
        val matchRef = FirebaseDatabase.getInstance().reference.child("universities")
            .child(UNI_ID).child("users").child(USER_ID)
        matchRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (interest in snapshot.child("interests").children) {
                    val interestName = interest.key
                    val interestRating: Long = interest.value as Long
                    if (interestName != null) {
                        addChipIfNotExist(interestName, interestRating.toInt(), pChipGroup)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun addChipIfNotExist(pItem: String, rating: Int, pChipGroup: ChipGroup) {
        var chipAlreadyExists = false

        for (i in 0 until pChipGroup.childCount) {
            val chip = pChipGroup.getChildAt(i)
            if (chip is Chip && chip.text.toString() == pItem) {
                chipAlreadyExists = true
                break
            }
        }

        if (!chipAlreadyExists) {
            addChip(pItem, rating, pChipGroup)
        }
    }

    private fun addChip(pItem: String, rating: Int, pChipGroup: ChipGroup) {
        val lChip = Chip(this)
        val displayString = "$pItem ($rating stars) ❌"
        lChip.text = displayString
        // Remove chip from group if it is clicked
        lChip.setOnClickListener{
            val anim = AlphaAnimation(1f,0f)
            anim.duration = 250
            anim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    pChipGroup.removeView(it)
                }
                override fun onAnimationStart(animation: Animation?) {}
            })
            it.startAnimation(anim)
        }
        pChipGroup.addView(lChip, pChipGroup.childCount - 1)
    }

}