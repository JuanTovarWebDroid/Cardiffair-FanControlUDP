/*
Author: Juan David Tovar Vera
Date: 05/01/2021

Description:
This app is an interface to communicate an android smartphone with a wireless module through UDP protocol by broadcast.

IP: 192.168.1.255
Port: 1112

In the interface, customers are going to find a fan control with 6 buttons:

1) On/Off: turn the fan on or off
2) Up fan speed: increase the fan speed in 1 step, starts in 0, the minimum speed is 0 (stopped) and the maximum speed is 15.
3) Down fan speed: decrease the fan in 1 step, the minimum speed is 0 (stopped) and the maximum speed is 15.
4) Up time (15 minutes step): Increase the time in 15 minutes, maximum value is when the time is in 24 hours,
    and the minimum time value is 0.
5) Down time (15 minutes step): Decrease the time in 15 minutes, maximum value is when the time is in 24 hours,
    and the minimum time value is 0.
6) Set clock: Sends the time displayed in the screen.

The app sends a string to the module with the speed and the time the user wants keep turn the fan on. In addition, the
fan can be turn off from the app.
 */

package com.example.ESP32CAR

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.StrictMode
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress


var mySpeed: Int = 0
var myTimeMinutes: Int = 0
var myTimeHours: Int = 0
var myTargetIP = "not set"
var myTargetPort = "not set"
var myUDP:String = ""

val myUDPMessageBuffer = arrayOfNulls<String>(20)
var udpMessageHead: Int = 0
var udpMessageTail: Int = 0

var statusOnOFFReceived:String=""
var speedReceived:String=""
var hour2ShowScreen:String=""
var minutes2ShowScreen:String=""
var setTime:String=""
var threadKill:Boolean = false

/*
SoftOptions: Class with the IP and port variables to send the message through UDP protocol.
 */
class SoftOptions() {
    var remoteHost: String = "192.168.1.255"
    var remotePort: Int = 4445

    init{}
}

// Global variable
val mySettings = SoftOptions()

open class MainActivity : AppCompatActivity() {


    private fun udpSendMessage (myTextString: String){
        myUDPMessageBuffer[udpMessageHead++] = myTextString
        if (udpMessageHead >19) udpMessageHead = 0
    }

    /*
     *sendUDP: Function to send the message.
     **/
    fun sendUDP(messageStr: String) {
        // Hack Prevent crash (sending should be done using an async task)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        try {
            //Open a port to send the package
            val socket = DatagramSocket()
            socket.broadcast = true
            val sendData = messageStr.toByteArray()
            val sendPacket = DatagramPacket(sendData, sendData.size, InetAddress.getByName(mySettings.remoteHost), mySettings.remotePort
            )
            socket.send(sendPacket)

            //socket.close()


        } catch (e: IOException) {
            //Log.e(FragmentActivity.TAG, "IOException: " + e.message)
        }

        myUDP="STATUS\n"

    }

    open fun receiveUDP() {
        val buffer = ByteArray(256)
        var socket: DatagramSocket? = null

        try {
            //Keep a socket open to listen to all the UDP trafic that is destined for this port
            socket = DatagramSocket(
                mySettings.remotePort,
                InetAddress.getByName(mySettings.remoteHost)
            )
            socket.broadcast = true
            socket.reuseAddress = true


            val packet = DatagramPacket(buffer, buffer.size)

            socket.receive(packet)

            println("----- Received Message ---- ")

            val data = String(packet.data, 0, packet.length)

            val parts:List<String> = data.split(",")

            statusOnOFFReceived = parts[0]
            speedReceived = parts[1]

            val hour2Show = parts[2].toInt()/3600
            val newHourInSeconds = hour2Show * 3600
            val minutesInSeconds = parts[2].toInt() - newHourInSeconds

            val minutes2Show = minutesInSeconds/60

            hour2ShowScreen = if(hour2Show <10){"0$hour2Show"}
            else hour2Show.toString()

            minutes2ShowScreen = if(minutes2Show <10){"0$minutes2Show"}
            else minutes2Show.toString()

            //socket.close()

        } catch (e: Exception) {
            println("open fun receiveUDP catch exception.$e")
            e.printStackTrace()
        } finally {
            socket?.close()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // General variables
        val onOFFButton = this.findViewById<Button>(R.id.btnOnOff)
        val txtFanSpeed = this.findViewById<TextView>(R.id.txtFanSpeed)
        val speedBarShow = this.findViewById<ProgressBar>(R.id.barSpeed)
        val txtRunTime = this.findViewById<TextView>(R.id.txtRunTime)
        val timeShowHours = this.findViewById<TextView>(R.id.txtTimeHours)
        val timeShowMinutes = this.findViewById<TextView>(R.id.txtTimeMinutes)
        val txt2Points = this.findViewById<TextView>(R.id.txtTimeHourCero)
        val txt2PointsReceiving = this.findViewById<TextView>(R.id.txtTimeHourPointReceive)
        val timeShowHoursReceiving= this.findViewById<TextView>(R.id.txtTimeHoursReceiving)
        val timeShowMinutesReceiving= this.findViewById<TextView>(R.id.txtTimeMinutesReceiving)
        val slashSeparator= this.findViewById<TextView>(R.id.txtTimeSlash)

        threadKill = false

        object  : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {

                Thread(){
                    if(udpMessageHead == udpMessageTail){
                        sendUDP("STATUS\n")
                        println("----- Sent STATUS ---- ")
                    }else{
                        sendUDP(myUDPMessageBuffer[udpMessageTail++].toString())
                        if (udpMessageTail >19) udpMessageTail = 0
                        println("----- Sent Message ----")
                    }
                    runOnUiThread(){
                    }
                }.start()
            }
            override fun onFinish() {
                if (!threadKill){
                    this.start()
                }else{
                    finish()
                }
            }
        }.start()


        object  : CountDownTimer(5000, 100) {
                override fun onTick(millisUntilFinished: Long) {
                    Thread(){
                        //Do some Network Request
                        receiveUDP()

                        println("----- Before update---- ")
                        runOnUiThread(){
                            //Update UI

                            when (statusOnOFFReceived) {
                                "OFF" -> {
                                    println("-----updateing OFF---- ")

                                    onOFFButton.setText(R.string.OFF)

                                    txtFanSpeed.text = R.string.FAN.toString()
                                    txtFanSpeed.textSize = 30f
                                    txtFanSpeed.setPadding(0,0,0,0)

                                    speedBarShow.visibility = View.GONE
                                    speedBarShow.progress = 0

                                    txtRunTime.text = R.string.OFF.toString()
                                    txtRunTime.textSize = 30f
                                    txtRunTime.setPadding(0,0,0,0)

                                    timeShowHours.visibility = View.GONE
                                    timeShowMinutes.visibility = View.GONE
                                    txt2Points.visibility = View.GONE
                                    timeShowHoursReceiving.visibility = View.GONE
                                    timeShowMinutesReceiving.visibility = View.GONE
                                    txt2PointsReceiving.visibility = View.GONE
                                    slashSeparator.visibility = View.GONE
                                }
                                "ON" -> {

                                    onOFFButton.setText(R.string.ON)
                                    println("-----updateing ONN---- ")

                                    txtFanSpeed.text = R.string.FAN_SPEED.toString()
                                    txtFanSpeed.textSize = 18f
                                    txtFanSpeed.setPadding(0,0,0,230)

                                    speedBarShow.visibility = View.VISIBLE
                                    speedBarShow.progress = speedReceived.toInt()
                                    mySpeed = speedReceived.toInt()

                                    txtRunTime.text = R.string.RUN_TIME.toString()
                                    txtRunTime.textSize = 18f
                                    txtRunTime.setPadding(0,0,0,200)

                                    timeShowHours.visibility = View.VISIBLE
                                    timeShowMinutes.visibility = View.VISIBLE
                                    txt2Points.visibility = View.VISIBLE
                                    timeShowHoursReceiving.visibility = View.VISIBLE
                                    timeShowHoursReceiving.text = hour2ShowScreen
                                    timeShowMinutesReceiving.visibility = View.VISIBLE
                                    timeShowMinutesReceiving.text = minutes2ShowScreen
                                    txt2PointsReceiving.visibility = View.VISIBLE
                                    slashSeparator.visibility = View.VISIBLE
                                }
                            }
                            println("----- After update ---- ")
                        }
                    }.start()
                }
                override fun onFinish() {
                    if (!threadKill){
                        this.start()
                    }else{
                        finish()
                    }
                }
            }.start()

        /*
        *btnOnOff: Button On/Off behavior
        **/
        btnOnOff.setOnClickListener{

            // Button Off status
            if (onOFFButton.text.toString() == "ON"){
                udpSendMessage("STATE,OFF\n")
            // Button On status
            }else{
                udpSendMessage("STATE,ON\n")
            }
        }

        /*
        *btnUpTime: Button to increase the time in 15 minutes. The maximum value is 24:00 (24 hours)
        **/
        btnUpTime.setOnClickListener{
            if(onOFFButton.text.toString() == "ON"){
                myTimeMinutes += 15

                if (myTimeMinutes >= 60){
                    myTimeMinutes = 0
                    myTimeHours += 1
                    if (myTimeHours > 12 ) myTimeHours = 12
                }

                if(myTimeHours == 12 && myTimeMinutes >= 0) myTimeMinutes = 0

                setTime = (myTimeHours*3600 + myTimeMinutes*60).toString()

                if(myTimeMinutes == 0 || myTimeMinutes == 60){
                    timeShowMinutes.text = getString(0, myTimeMinutes)
                }else{
                    timeShowMinutes.text = myTimeMinutes.toString()
                }
                if(myTimeHours <10){
                    timeShowHours.text = getString(0, myTimeHours)
                } else{
                    timeShowHours.text = myTimeHours.toString()
                }
            }
        }
        /*
        *btnDownTime: Button to decrease the time in 15 minutes. The minimum value is 00:00
        **/
        btnDownTime.setOnClickListener {
            if(onOFFButton.text.toString() == "ON"){
                if((myTimeMinutes != 0) || (myTimeHours != 0)) {
                    myTimeMinutes -= 15

                    if (myTimeMinutes < 0) {
                        myTimeMinutes = 45

                        if (myTimeHours > 0){
                            myTimeHours -= 1
                        }
                    }

                    setTime = (myTimeHours*3600 + myTimeMinutes*60).toString()

                    if(myTimeMinutes == 0 || myTimeMinutes == 60){
                        timeShowMinutes.text = getString(0, myTimeMinutes)
                    }else{
                        timeShowMinutes.text = myTimeMinutes.toString()
                    }

                    if (myTimeHours <= 9){
                        timeShowHours.text = getString(0, myTimeHours)
                    }else{
                        timeShowHours.text = myTimeHours.toString()
                    }
                }
            }
        }

        /*
        *btnIncreaseVel: Button to increase the velocity in 1 step. The maximum value is 15.
        **/
        btnIncreaseVel.setOnClickListener {
            if(onOFFButton.text.toString() == "ON"){
                mySpeed += 1
                if (mySpeed > 15) mySpeed = 15
                udpSendMessage("SPEED,$mySpeed\n")
            }
        }

        /*
        *btnDecreaseVel: Button to decrease the velocity in 1 step. The minimum value is 0.
        **/
        btnDecreaseVel.setOnClickListener {
            if(onOFFButton.text.toString() == "ON"){
                mySpeed -= 1
                if (mySpeed < 0) mySpeed = 0
                udpSendMessage("SPEED,$mySpeed.$\n")
            }
        }

        /*
        *btnSend: Button to send the time displayed on the screen. It sends a string (in UDP protocol) by broadcast.
        **/
        btnSend.setOnClickListener {
            if(onOFFButton.text.toString() == "ON"){
                udpSendMessage("TIME,$setTime\n")
            }
        }

        /*
        *btnSettings: Hidden button for testing and setting other IP address and other port.
        **/
        btnSettings.setOnClickListener {
            val intent = Intent(this, Settings::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
            threadKill = true
    }

    override fun onPause() {
        super.onPause()
        threadKill = true
    }
}
