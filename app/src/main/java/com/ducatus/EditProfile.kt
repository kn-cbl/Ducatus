package com.ducatus

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.android.material.textfield.TextInputEditText

class EditProfile : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        //username
        val username = findViewById<View>(R.id.TextInputEditTextUsername) as TextInputEditText
        //if (username.getText().toString().trim().equalsIgnoreCase("")) {
        //    username.setError("This field can not be blank");
        //}
        //if (username.getText().toString().trim().equalsIgnoreCase("")) {
        //    username.setError("This field can not be blank");
        //}
        username.error = "Invalid Username"


        //email address
        val email_address = findViewById<View>(R.id.TextInputEditTextEmailAddress) as TextInputEditText
        //if (email_address.getText().toString().trim().equalsIgnoreCase("")) {
        //    email_address.setError("This field can not be blank");
        //}
        //if (email_address.getText().toString().trim().equalsIgnoreCase("")) {
        //    email_address.setError("This field can not be blank");
        //}
        email_address.error = "Account does not exist"


        //mobile number
        val mob_num = findViewById<View>(R.id.TextInputEditTextMobNum) as TextInputEditText
        //if (mob_num.getText().toString().trim().equalsIgnoreCase("")) {
        //    mob_num.setError("This field can not be blank");
        //}
        //if (mob_num.getText().toString().trim().equalsIgnoreCase("")) {
        //    mob_num.setError("This field can not be blank");
        //}
        mob_num.error = "Invalid mobile number"
    }
}