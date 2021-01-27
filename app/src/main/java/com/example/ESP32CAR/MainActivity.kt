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
import android.os.StrictMode
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress



import android.os.CountDownTimer
import java.net.SocketAddress


var mySpeed: Int = 0
var myTimeMinutes: Int = 0
var myTimeHours: Int = 0
var myTargetIP = "not set"
var myTargetPort = "not set"
var myUDP:String = ""
var message:String=""



/*
SoftOptions: Class with the IP and port variables to send the message through UDP protocol.
 */
public class SoftOptions {
    var RemoteHost: String = "192.168.1.255"
    var RemotePort: Int = 4445

    constructor()
    init{}
}

//var RemoteHostMine: String = "192.168.1.117"
// Global variable
val mySettings = SoftOptions()

open class MainActivity : AppCompatActivity() {


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
            val sendPacket = DatagramPacket(
                sendData, sendData.size, InetAddress.getByName(
                    mySettings.RemoteHost
                ), mySettings.RemotePort
            )
            socket.send(sendPacket)
            println("Message SEND = " + sendPacket)


            val duration = Toast.LENGTH_SHORT
            val toast = Toast.makeText(applicationContext, messageStr, duration)
            toast.show()

            socket.close()

        } catch (e: IOException) {
            //Log.e(FragmentActivity.TAG, "IOException: " + e.message)
        }

    }

    open fun receiveUDP() {
        Log.i("Text: -----> ", "Im in Receive method")
        val buffer = ByteArray(256)

        var socket: DatagramSocket? = null
        //val socket = DatagramSocket()


        try {
            //Keep a socket open to listen to all the UDP trafic that is destined for this port
            Log.i("Text: -----> ", "Im in try ")
            socket = DatagramSocket(mySettings.RemotePort, InetAddress.getByName(mySettings.RemoteHost))
            socket.broadcast = true
            socket.reuseAddress = true


            val packet = DatagramPacket(buffer, buffer.size)
            socket.receive(packet)
            message = packet.data.toString()
            println("open fun receiveUDP packet received = " + message)
            println("open fun receiveUDP packet received = " + buffer)
            Log.i("Text: -----> ", packet.data.toString())
            //messageudp.text = message
            socket.close()

        } catch (e: Exception) {
            println("open fun receiveUDP catch exception." + e.toString())
            e.printStackTrace()
        } finally {
            socket?.close()
        }
    }

    var counter = 0

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // General variables
        val OnOFFButton = this.findViewById<Button>(R.id.btnOnOff)
        val txtFanSpeed = this.findViewById<TextView>(R.id.txtFanSpeed)
        val SpeedBarShow = this.findViewById<ProgressBar>(R.id.barSpeed)
        val txtRunTime = this.findViewById<TextView>(R.id.txtRunTime)
        val TimeShowHours = this.findViewById<TextView>(R.id.txtTimeHours)
        val TimeShowMinutes = this.findViewById<TextView>(R.id.txtTimeMinutes)
        val txt2Points = this.findViewById<TextView>(R.id.txtTimeHourCero)




        startTimeCounter.setOnClickListener {

            myUDP = "STATUS\n"
            sendUDP(myUDP)

            var messageudp = this.findViewById<TextView>(R.id.messageudp)

                val countTimeShows = this.findViewById<TextView>(R.id.countTime)
                object  : CountDownTimer(3000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        countTimeShows.text = counter.toString()
                        counter++
                        Thread({
                            //Do some Network Request
                            Log.i("Text: -----> ", "Im in thread... launch receiveUDP ")
                            Log.i("Counter: -----> ", counter.toString())
                            receiveUDP()
                            runOnUiThread({
                                //Update UI
                                messageudp.text = message
                            })
                        }).start()

                    }
                    override fun onFinish() {
                        countTimeShows.text = "Finished"
                    }
                }.start()

        }

        status.setOnClickListener {

            myUDP = "STATUS"
            sendUDP(myUDP)
        }

        /*
        *btnOnOff: Button On/Off behavior
        **/
        btnOnOff.setOnClickListener{
            // Button Off status
            if (OnOFFButton.text.toString() == "ON"){
                OnOFFButton.setBackgroundColor(Color.RED)
                OnOFFButton.setText("OFF")
                txtFanSpeed.text = "FAN"
                txtFanSpeed.textSize = 30f
                SpeedBarShow.visibility = View.GONE
                txtRunTime.text = "OFF"
                txtRunTime.textSize = 30f
                TimeShowHours.visibility = View.GONE
                TimeShowMinutes.visibility = View.GONE
                txt2Points.visibility = View.GONE

                myTimeHours = 0
                myTimeMinutes = 0
                mySpeed = 0
                TimeShowHours.text = "0" + myTimeHours.toString()
                TimeShowMinutes.text = "0" + myTimeMinutes.toString()
                SpeedBarShow.progress = mySpeed

                val FanNumber =  "1"
                val FanStatus = "0"
                val Time = "0" + ":" +  "0"
                myUDP = FanNumber + "," + FanStatus + "," + Time
                sendUDP(myUDP)

            // Button On status
            }else{
                OnOFFButton.setBackgroundColor(Color.GREEN)
                OnOFFButton.setText("ON")

                txtFanSpeed.text = "FAN SPEED"
                txtFanSpeed.textSize = 18f
                SpeedBarShow.visibility = View.VISIBLE
                txtRunTime.text = "RUN TIME"
                txtRunTime.textSize = 18f
                TimeShowHours.visibility = View.VISIBLE
                TimeShowMinutes.visibility = View.VISIBLE
                txt2Points.visibility = View.VISIBLE

            }
        }

        /*
        *btnUpTime: Button to increase the time in 15 minutes. The maximum value is 24:00 (24 hours)
        **/
        btnUpTime.setOnClickListener{

            if(OnOFFButton.text.toString() == "ON"){
                myTimeMinutes += 15

                if (myTimeMinutes >= 60){
                    myTimeMinutes = 0
                    myTimeHours += 1

                    if (myTimeHours > 24 ) myTimeHours = 24
                }

                if(myTimeHours == 24 && myTimeMinutes >= 0) myTimeMinutes = 0

                if(myTimeMinutes == 0 || myTimeMinutes == 60){
                    TimeShowMinutes.text = "0" + myTimeMinutes.toString()
                }else{
                    TimeShowMinutes.text = myTimeMinutes.toString()
                }
                if(myTimeHours <10){
                    TimeShowHours.text = "0" + myTimeHours.toString()
                } else{
                    TimeShowHours.text = myTimeHours.toString()
                }
            }
        }
        /*
        *btnDownTime: Button to decrease the time in 15 minutes. The minimum value is 00:00
        **/
        btnDownTime.setOnClickListener {
            if(OnOFFButton.text.toString() == "ON"){
                if((myTimeMinutes != 0) || (myTimeHours != 0)) {
                    myTimeMinutes -= 15

                    if (myTimeMinutes < 0) {
                        myTimeMinutes = 45

                        if (myTimeHours > 0){
                            myTimeHours -= 1
                        }
                    }

                    if(myTimeMinutes == 0 || myTimeMinutes == 60){
                        TimeShowMinutes.text = "0" + myTimeMinutes.toString()
                    }else{
                        TimeShowMinutes.text = myTimeMinutes.toString()
                    }

                    if (myTimeHours <= 9){
                        TimeShowHours.text = "0" + myTimeHours.toString()
                    }else{
                        TimeShowHours.text = myTimeHours.toString()
                    }
                }
            }
        }

        /*
        *btnIncreaseVel: Button to increase the velocity in 1 step. The maximum value is 15.
        **/
        btnIncreaseVel.setOnClickListener {
            if(OnOFFButton.text.toString() == "ON"){
                mySpeed += 1
                if (mySpeed > 15) mySpeed = 15
                val SpeedBarShow = this.findViewById<ProgressBar>(R.id.barSpeed)
                SpeedBarShow.progress = mySpeed
                myUDP = mySpeed.toString()
                sendUDP(myUDP)
            }
        }

        /*
        *btnDecreaseVel: Button to decrease the velocity in 1 step. The minimum value is 0.
        **/
        btnDecreaseVel.setOnClickListener {
            if(OnOFFButton.text.toString() == "ON"){
                mySpeed -= 1
                if (mySpeed < 0) mySpeed = 0
                val SpeedBarShow = this.findViewById<ProgressBar>(R.id.barSpeed)
                SpeedBarShow.progress = mySpeed
                myUDP = mySpeed.toString()
                sendUDP(myUDP)
            }
        }

        /*
        *btnSend: Button to send the time displayed on the screen. It sends a string (in UDP protocol) by broadcast.
        **/
        btnSend.setOnClickListener {
            if(OnOFFButton.text.toString() == "ON"){
                val FanNumber =  "1"
                val FanStatus = "1"
                val Time = myTimeHours.toString() + ":" +  myTimeMinutes.toString()
                myUDP = FanNumber + "," + FanStatus + "," + Time
                sendUDP(myUDP)
            }
        }

        /*
        *btnSettings: Hidden button for testing and setting other IP address and other port.
        **/
        btnSettings.setOnClickListener {
            val intent = Intent(this, Settings::class.java);
            startActivity(intent)
        }

    }
}
