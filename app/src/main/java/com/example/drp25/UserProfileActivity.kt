package com.example.drp25

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import com.example.drp25.databinding.ActivityUserProfileBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.File
import java.io.FileOutputStream

/** Retrieve user information using Stream SDK to personalise. */

class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private lateinit var stampLayout: LinearLayout
    private lateinit var noStampsTextView: TextView
    private lateinit var imageFile: File

    companion object {
        private const val PICK_IMAGE_REQUEST_CODE = 123
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        // indicates this is the person logged in (currently Kevin)
        listenToUser(UNI_ID, USER_ID)

        /* inflate binding */
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        stampLayout = findViewById(R.id.stamp_layout)
        noStampsTextView = findViewById(R.id.no_stamps_prompt)

        imageFile = File(filesDir, "pfp$UNI_ID$USER_ID.png")

        // set up views
        val uniRef = FirebaseDatabase.getInstance().reference.child("universities")
            .child(UNI_ID)
        val userRef = uniRef.child("users").child(USER_ID)

        userRef.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val name: String = snapshot.child("name").value as String
                val course = snapshot.child("course").value
                val year = snapshot.child("year").value
                val pfpPath = snapshot.child("pfp").value
                if (pfpPath != null) {
                    displayImageFromFile(pfpPath as String)
                }

                binding.nameText.text = name

                uniRef.child("name").addValueEventListener(object: ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val uniName = snapshot.value as String
                        binding.personalInfoText.text = "Year $year | $course | $uniName"
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }
                })

                for (interest in snapshot.child("interests").children) {
                    val inflater = LayoutInflater.from(this@UserProfileActivity)
                    val rowView = inflater.inflate(R.layout.profile_interest, binding.interestsTable, false) as TableRow
                    val interestNameView = rowView.findViewById<TextView>(R.id.interest_name)
                    interestNameView.text = interest.key
                    binding.interestsTable.addView(rowView)
                }

                stampLayout.removeAllViews()
                if (snapshot.child("stamps").hasChildren()){
                    noStampsTextView.visibility = View.GONE
                    stampLayout.visibility = View.VISIBLE

                    for (stamp in snapshot.child("stamps").children) {
                        val inflater = LayoutInflater.from(this@UserProfileActivity)
                        val imgView = inflater.inflate(R.layout.stamp_view, stampLayout, false) as ImageView
                        stamp.getValue(Int::class.java)?.let { imgView.setImageResource(it) }
                        stampLayout.addView(imgView)
                    }
                } else {
                    stampLayout.visibility = View.GONE
                    noStampsTextView.visibility = View.VISIBLE
                }

            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })

        /* Functionality of "Back" button -> takes user back to homepage. */
        binding.profileBackButton.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }

        /* Functionality of "Meet Someone New" button -> takes user to match page. */
        binding.profileMatchButton.setOnClickListener {
            val intent = Intent(this, MatchActivity::class.java)
            startActivity(intent)
        }

        binding.updateInterestsButton.setOnClickListener {
            val intent = Intent(this, InterestActivity::class.java)
            startActivity(intent)
        }

        binding.uploadButton.setOnClickListener {
            // Open the file picker or gallery when the button is clicked
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri: Uri? = data.data
            saveImageToFile(selectedImageUri)
        }
    }

    private fun saveImageToFile(imageUri: Uri?) {
        imageUri?.let { uri ->
            val inputStream = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(imageFile)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
        }
        updatePfp(UNI_ID, USER_ID, imageFile.absolutePath)
        displayImageFromFile(imageFile.absolutePath)
    }

    private fun displayImageFromFile(path: String) {
        val imageBitmap = BitmapFactory.decodeFile(path)
        binding.profileImageView.setImageBitmap(imageBitmap)
    }
}