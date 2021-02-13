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
//var udpMessage:String=""
//var udpNewMessage:Boolean= false

var message:String=""
var statusOnOFFReceived:String=""
var speedReceived:String=""
var hour2ShowScreen:String=""
var minutes2ShowScreen:String=""
var setTime:String=""
var myUDPset:String=""
var threadKill:Boolean = false
var sendAbilable:Boolean = true
var myUDPStorage:String = ""

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
var buff = ByteArray(2048)

open class MainActivity : AppCompatActivity() {


    fun udpSendMessage (myTextString: String){
        myUDPMessageBuffer[udpMessageHead++] = myTextString
        if (udpMessageHead >19) udpMessageHead = 0
    }

    fun bufferMaker(messageStr: String): ByteArray{

        val data4Buffer = messageStr.toByte()

        for (i in buff.indices){
            buff[i] = data4Buffer
        }

        return buff

    }

    /*
     *sendUDP: Function to send the message.
     **/
    fun sendUDP(messageStr: String) {
        // Hack Prevent crash (sending should be done using an async task)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        //val buff = ByteArray(2048)

        try {
            //Open a port to send the package
            val socket = DatagramSocket()
            socket.broadcast = true
            val sendData = messageStr.toByteArray()
            //buff[1] = messageStr.toByte()
            val sendPacket = DatagramPacket(sendData, sendData.size, InetAddress.getByName(mySettings.RemoteHost), mySettings.RemotePort
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
                mySettings.RemotePort,
                InetAddress.getByName(mySettings.RemoteHost)
            )
            socket.broadcast = true
            socket.reuseAddress = true


            val packet = DatagramPacket(buffer, buffer.size)

            socket.receive(packet)

            println("----- Received Message ---- ")

            val data = String(packet.data, 0, packet.length)
            message = data
            var datamessage = message.toString()
            var parts = datamessage.split(",")

            statusOnOFFReceived = parts[0]
            speedReceived = parts[1]

            var hour = parts[2].toInt()/3600

            val hour2Show = hour as Int
            val newHourInSecounds = hour2Show * 3600
            val minutesInSecounds = parts[2].toInt() - newHourInSecounds

            var minutes2Show = minutesInSecounds/60
            minutes2Show = minutes2Show as Int

            if(hour2Show <10){
                hour2ShowScreen = "0" + hour2Show.toString()
            } else{
                hour2ShowScreen = hour2Show.toString()
            }

            if(minutes2Show <10){
                minutes2ShowScreen = "0" + minutes2Show.toString()
            } else{
                minutes2ShowScreen = minutes2Show.toString()
            }

            //socket.close()

        } catch (e: Exception) {
            println("open fun receiveUDP catch exception." + e.toString())
            e.printStackTrace()
        } finally {
            socket?.close()
        }
    }

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
        val txt2PointsReceiving = this.findViewById<TextView>(R.id.txtTimeHourPointReceive)
        val TimeShowHoursReceiving= this.findViewById<TextView>(R.id.txtTimeHoursReceiving)
        val TimeShowMinutesReceiving= this.findViewById<TextView>(R.id.txtTimeMinutesReceiving)
        val SlashSeparator= this.findViewById<TextView>(R.id.txtTimeSlash)

        threadKill = false
        //myUDP = "STATUS\n"


        var messageudp = this.findViewById<TextView>(R.id.messageudp)


        /*object  : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {

                // if(sendAbilable){
                sendUDP(myUDP)
                //}

                Thread({
                    //Do some Network Request
                    receiveUDP()
                    runOnUiThread({
                        //Update UI

                    })
                }).start()
            }
            override fun onFinish() {

                if (!threadKill){
                    this.start()
                }else{
                    finish()
                }
            }
        }.start()

*/

        object  : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {

                Thread({
                    if(udpMessageHead == udpMessageTail){
                        sendUDP("STATUS\n")
                        println("----- Sent STATUS ---- " + java.util.Calendar.getInstance())
                    }else{
                        sendUDP(myUDPMessageBuffer[udpMessageTail++].toString())
                        if (udpMessageTail >19) udpMessageTail = 0
                        println("----- Sent Message ----" + java.util.Calendar.getInstance())

                    }
                    runOnUiThread({

                    })
                }).start()
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

                    //val bufferToSend = bufferMaker(myUDP)

                    //sendUDP(myUDP)

                    /*

                    if(udpMessageHead == udpMessageTail){
                        sendUDP("STATUS\n")
                        println("----- Sent STATUS ---- " + java.util.Calendar.getInstance())
                    }else{
                        sendUDP(myUDPMessageBuffer[udpMessageTail++].toString())
                        if (udpMessageTail >19) udpMessageTail = 0
                        println("----- Sent Message ----" + java.util.Calendar.getInstance())

                    }

                     */

                    Thread({
                        //Do some Network Request
                        receiveUDP()

                        println("----- Before update---- ")
                        runOnUiThread({
                            //Update UI
                            //messageudp.text = message



                            when (statusOnOFFReceived) {
                                "OFF" -> {

                                    println("-----updateing OFF---- ")

                                    OnOFFButton.setText("OFF")

                                    txtFanSpeed.text = "FAN"
                                    txtFanSpeed.textSize = 30f
                                    txtFanSpeed.setPadding(0,0,0,0)

                                    SpeedBarShow.visibility = View.GONE
                                    SpeedBarShow.progress = 0

                                    txtRunTime.text = "OFF"
                                    txtRunTime.textSize = 30f
                                    txtRunTime.setPadding(0,0,0,0)

                                    TimeShowHours.visibility = View.GONE
                                    TimeShowMinutes.visibility = View.GONE
                                    txt2Points.visibility = View.GONE
                                    TimeShowHoursReceiving.visibility = View.GONE
                                    TimeShowMinutesReceiving.visibility = View.GONE
                                    txt2PointsReceiving.visibility = View.GONE
                                    SlashSeparator.visibility = View.GONE

                                }
                                "ON" -> {

                                    OnOFFButton.setText("ON")
                                    println("-----updateing ONN---- ")

                                    txtFanSpeed.text = "FAN SPEED"
                                    txtFanSpeed.textSize = 18f
                                    txtFanSpeed.setPadding(0,0,0,150)

                                    SpeedBarShow.visibility = View.VISIBLE
                                    SpeedBarShow.progress = speedReceived.toInt()
                                    mySpeed = speedReceived.toInt()

                                    txtRunTime.text = "RUN TIME"
                                    txtRunTime.textSize = 18f
                                    txtRunTime.setPadding(0,0,0,200)

                                    TimeShowHours.visibility = View.VISIBLE
                                    TimeShowMinutes.visibility = View.VISIBLE
                                    txt2Points.visibility = View.VISIBLE
                                    TimeShowHoursReceiving.visibility = View.VISIBLE
                                    TimeShowHoursReceiving.text = hour2ShowScreen
                                    TimeShowMinutesReceiving.visibility = View.VISIBLE
                                    TimeShowMinutesReceiving.text = minutes2ShowScreen
                                    txt2PointsReceiving.visibility = View.VISIBLE
                                    SlashSeparator.visibility = View.VISIBLE

                                }
                            }
                            println("----- After update ---- ")

                        })
                    }).start()
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

            sendAbilable = false
            // Button Off status
            if (OnOFFButton.text.toString() == "ON"){
/*

                OnOFFButton.setBackgroundColor(Color.RED)
                OnOFFButton.setText("OFF")

                txtFanSpeed.text = "FAN"
                txtFanSpeed.textSize = 30f
                txtFanSpeed.setPadding(0,0,0,0)

                txtRunTime.text = "OFF"
                txtRunTime.textSize = 30f
                txtRunTime.setPadding(0,0,0,0)

                SpeedBarShow.visibility = View.GONE

                TimeShowHours.visibility = View.GONE
                TimeShowMinutes.visibility = View.GONE
                txt2Points.visibility = View.GONE
                TimeShowHoursReceiving.visibility = View.GONE
                TimeShowMinutesReceiving.visibility = View.GONE
                txt2PointsReceiving.visibility = View.GONE
                SlashSeparator.visibility = View.GONE

                myTimeHours = 0
                myTimeMinutes = 0
                mySpeed = 0
                TimeShowHours.text = "0" + myTimeHours.toString()
                TimeShowMinutes.text = "0" + myTimeMinutes.toString()
                SpeedBarShow.progress = mySpeed
*/
                //myUDP = "STATE,OFF\n"
                //buff.set()
                //myUDPStorage = myUDP
                udpSendMessage("STATE,OFF\n")

            // Button On status
            }else{

                /*
                OnOFFButton.setBackgroundColor(Color.GREEN)
                OnOFFButton.setText("ON")

                txtFanSpeed.text = "FAN SPEED"
                txtFanSpeed.setPadding(0,0,0,150)
                txtFanSpeed.textSize = 18f
                SpeedBarShow.visibility = View.VISIBLE

                txtRunTime.text = "RUN TIME"
                txtRunTime.textSize = 18f
                txtRunTime.setPadding(0,0,0,200)

                TimeShowHours.visibility = View.VISIBLE
                TimeShowMinutes.visibility = View.VISIBLE
                txt2Points.visibility = View.VISIBLE
                TimeShowHoursReceiving.visibility = View.VISIBLE
                TimeShowMinutesReceiving.visibility = View.VISIBLE
                txt2PointsReceiving.visibility = View.VISIBLE
                SlashSeparator.visibility = View.VISIBLE

                 */

                //myUDP = "STATE,ON\n"
                //myUDPStorage = myUDP
                udpSendMessage("STATE,ON\n")

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

                    if (myTimeHours > 12 ) myTimeHours = 12
                }

                if(myTimeHours == 12 && myTimeMinutes >= 0) myTimeMinutes = 0

                setTime = (myTimeHours*3600 + myTimeMinutes*60).toString()

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

                    setTime = (myTimeHours*3600 + myTimeMinutes*60).toString()

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
                //SpeedBarShow.progress = mySpeed
                //myUDP = "SPEED," + mySpeed.toString()+"\n"
                udpSendMessage("SPEED," + mySpeed.toString()+"\n")
            }
        }

        /*
        *btnDecreaseVel: Button to decrease the velocity in 1 step. The minimum value is 0.
        **/
        btnDecreaseVel.setOnClickListener {
            if(OnOFFButton.text.toString() == "ON"){
                mySpeed -= 1
                if (mySpeed < 0) mySpeed = 0
                //SpeedBarShow.progress = mySpeed
                //myUDP = "SPEED," + mySpeed.toString()+"\n"
                udpSendMessage("SPEED," + mySpeed.toString()+"\n")
            }
        }

        /*
        *btnSend: Button to send the time displayed on the screen. It sends a string (in UDP protocol) by broadcast.
        **/
        btnSend.setOnClickListener {
            if(OnOFFButton.text.toString() == "ON"){
                //myUDP = "TIME," + setTime + "\n"
                udpSendMessage("TIME," + setTime + "\n")
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

    override fun onDestroy() {
        super.onDestroy()
            threadKill = true
    }

    override fun onPause() {
        super.onPause()
        threadKill = true
    }

}
