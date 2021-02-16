package com.example.ESP32CAR

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText

import kotlinx.android.synthetic.main.activity_settings.*
import org.w3c.dom.Text

class Settings : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        this.editEnterip.setText(mySettings.remoteHost)
        this.editEnterport.setText(mySettings.remotePort.toString())

        btnSetip.setOnClickListener {
            val finalIP =findViewById<EditText>(R.id.editEnterip)
            val textFromEditText = finalIP.text.toString() // access text this way
            mySettings.remoteHost = textFromEditText
            val finalPort =findViewById<EditText>(R.id.editEnterport)
            val textfromEnterport = finalPort.text.toString().toInt()
            //finalPort = "1238"
            mySettings.remotePort = textfromEnterport

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        btnCancelip.setOnClickListener {
            val editEnterip =  "null"
            val editEnterport = "null"
            myTargetIP = editEnterip
            myTargetPort = editEnterport
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("myTargetIP", myTargetIP)
            intent.putExtra("myTargetPort", myTargetPort)
            startActivity(intent)
        }
    }
}