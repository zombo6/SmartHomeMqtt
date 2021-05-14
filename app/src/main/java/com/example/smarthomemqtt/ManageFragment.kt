package com.example.smarthomemqtt

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.eclipse.paho.client.mqttv3.*

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ManageFragment : Fragment() {
    private lateinit var MqttClient: MQTTClient
    private lateinit var connectToken: IMqttToken

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (MqttClient.isConnected()) {
                    // Disconnect from MQTT Broker
                    MqttClient.disconnect(object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.d(this.javaClass.name, "Disconnected")

                            Toast.makeText(
                                context,
                                "MQTT Disconnection success",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Disconnection success, come back to Connect Fragment
                            findNavController().navigate(R.id.action_ManageFragment_to_ConnectFragment)
                        }

                        override fun onFailure(
                            asyncActionToken: IMqttToken?,
                            exception: Throwable?
                        ) {
                            Log.d(this.javaClass.name, "Failed to disconnect")
                        }
                    })
                } else {
                    Log.d(this.javaClass.name, "Impossible to disconnect, no server connected")
                }
            }
        })
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val serverUrl = arguments?.getString(MQTT_SERVER_URI_KEY)
        val clientId = arguments?.getString(MQTT_CLIENT_ID_KEY)
        val username = ""
        val pwd = ""


        // Check if passed arguments are valid
        if (serverUrl != null &&
            clientId != null &&
            username != null &&
            pwd != null
        ) {
            // Open MQTT Broker communication
            MqttClient = MQTTClient(context, serverUrl, clientId)


            // Connect and login to MQTT Broker
            MqttClient.connect(username,
                pwd,
                object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d(this.javaClass.name, "Connection success")

                        Toast.makeText(
                            context,
                            "MQTT Connection success",
                            Toast.LENGTH_SHORT
                        ).show()
                        getNodesMQTTGateway()
                    }

                    override fun onFailure(
                        asyncActionToken: IMqttToken?,
                        exception: Throwable?
                    ) {
                        Log.d(
                            this.javaClass.name,
                            "Connection failure: ${exception.toString()}"
                        )

                        Toast.makeText(
                            context,
                            "MQTT Connection fails: ${exception.toString()}",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Come back to Connect Fragment
                        findNavController().navigate(R.id.action_ManageFragment_to_ConnectFragment)
                    }
                },
                object : MqttCallback {
                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        val msg =
                            "Receive message: ${message.toString()} from topic: $topic"
                        Log.d(this.javaClass.name, msg)

                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }

                    override fun connectionLost(cause: Throwable?) {
                        Log.d(this.javaClass.name, "Connection lost ${cause.toString()}")
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        Log.d(this.javaClass.name, "Delivery complete")
                    }
//                            override fun connectComplete(reconnect: Boolean?, serverURI: String?) {
//                                getNodesMQTTGateway()
//                            }
                })


        } else {
            // Arguments are not valid, come back to Connect Fragment
            findNavController().navigate(R.id.action_ManageFragment_to_ConnectFragment)
        }



        return inflater.inflate(R.layout.fragment_manage, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Pobranie wartości przekazanych z connect fragment


        connectToken = MqttClient.getConnectToken()

        // connectToken.waitForCompletion(5000)

        // getNodesMQTTGateway()

        //Wywołanie wysłania rządania otrzymania topologii mesh.


        view.findViewById<Button>(R.id.button_prefill_client).setOnClickListener {
            // Set default values in edit texts
            view.findViewById<EditText>(R.id.edittext_pubtopic).setText(MQTT_TEST_TOPIC)
            view.findViewById<EditText>(R.id.edittext_pubmsg).setText(MQTT_TEST_MSG)
            view.findViewById<EditText>(R.id.edittext_subtopic).setText(MQTT_TEST_TOPIC)
        }

        view.findViewById<Button>(R.id.button_clean_client).setOnClickListener {
            // Clean values in edit texts
            view.findViewById<EditText>(R.id.edittext_pubtopic).setText("")
            view.findViewById<EditText>(R.id.edittext_pubmsg).setText("")
            view.findViewById<EditText>(R.id.edittext_subtopic).setText("")
        }

        view.findViewById<Button>(R.id.button_disconnect).setOnClickListener {
            if (MqttClient.isConnected()) {
                // Disconnect from MQTT Broker
                MqttClient.disconnect(object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d(this.javaClass.name, "Disconnected")

                        Toast.makeText(context, "MQTT Disconnection success", Toast.LENGTH_SHORT)
                            .show()

                        // Disconnection success, come back to Connect Fragment
                        findNavController().navigate(R.id.action_ManageFragment_to_ConnectFragment)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.d(this.javaClass.name, "Failed to disconnect")
                    }
                })
            } else {
                Log.d(this.javaClass.name, "Impossible to disconnect, no server connected")
            }
        }

        view.findViewById<Button>(R.id.button_publish).setOnClickListener {
            val topic = view.findViewById<EditText>(R.id.edittext_pubtopic).text.toString()
            val message = view.findViewById<EditText>(R.id.edittext_pubmsg).text.toString()

            if (MqttClient.isConnected()) {
                MqttClient.publish(topic,
                    message,
                    1,
                    false,
                    object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            val msg = "Publish message: $message to topic: $topic"
                            Log.d(this.javaClass.name, msg)

                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }

                        override fun onFailure(
                            asyncActionToken: IMqttToken?,
                            exception: Throwable?
                        ) {
                            Log.d(this.javaClass.name, "Failed to publish message to topic")
                        }
                    })
            } else {
                Log.d(this.javaClass.name, "Impossible to publish, no server connected")
            }
        }

        view.findViewById<Button>(R.id.button_subscribe).setOnClickListener {
            val topic = view.findViewById<EditText>(R.id.edittext_subtopic).text.toString()

            if (MqttClient.isConnected()) {
                MqttClient.subscribe(topic,
                    1,
                    object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            val msg = "Subscribed to: $topic"
                            Log.d(this.javaClass.name, msg)

                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }

                        override fun onFailure(
                            asyncActionToken: IMqttToken?,
                            exception: Throwable?
                        ) {
                            Log.d(this.javaClass.name, "Failed to subscribe: $topic")
                        }
                    })
            } else {
                Log.d(this.javaClass.name, "Impossible to subscribe, no server connected")
            }
        }


        view.findViewById<Button>(R.id.button_unsubscribe).setOnClickListener {
            val topic = view.findViewById<EditText>(R.id.edittext_subtopic).text.toString()

            if (MqttClient.isConnected()) {
                MqttClient.unsubscribe(topic,
                    object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            val msg = "Unsubscribed to: $topic"
                            Log.d(this.javaClass.name, msg)

                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }

                        override fun onFailure(
                            asyncActionToken: IMqttToken?,
                            exception: Throwable?
                        ) {
                            Log.d(this.javaClass.name, "Failed to unsubscribe: $topic")
                        }
                    })
            } else {
                Log.d(this.javaClass.name, "Impossible to unsubscribe, no server connected")
            }
        }


    }

    fun getNodesMQTTGateway() {
        //tutaj subscribe
        val topic_gateway_from = "painlessMesh/from/gateway";
        val topic_gateway_to = "painlessMesh/to/gateway";
        if (MqttClient.isConnected()) {
            MqttClient.subscribe(topic_gateway_from,
                1,
                object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        val msg = "Subscribed to gateway, $topic_gateway_from trying to get nodes: "
                        Log.d(this.javaClass.name, msg)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.d(this.javaClass.name, "Failed to subscribe: $topic_gateway_from")
                    }
                })

            val _messgageGetNodes = "getNodes";
            MqttClient.publish(topic_gateway_to,
                _messgageGetNodes,
                1,
                false,
                object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        val msg =
                            "Publish message: $_messgageGetNodes to topic: $topic_gateway_to"
                        Log.d(this.javaClass.name, msg)

                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(
                        asyncActionToken: IMqttToken?,
                        exception: Throwable?
                    ) {
                        Log.d(this.javaClass.name, "Failed to publish message to topic")
                    }
                })
        } else {
            Log.d(this.javaClass.name, "Impossible to subscribe and publish, no server connected")
            Toast.makeText(
                context,
                "Impossible to subscribe and publish, no server connected",
                Toast.LENGTH_SHORT
            ).show()
        }


    }

}


