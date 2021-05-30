package com.example.smarthomemqtt

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [NodeSelection.newInstance] factory method to
 * create an instance of this fragment.
 */
data class RootData(
    //@JsonProperty("Node")
    val nodeData: nodeData,
    //@JsonProperty("Switch")
    val switchData: MutableList<switchData>
)

data class nodeData(
    val nodeId: Long,
    val time: Int
)
data class switchData(
    val key: String,
    var value: Int
)




class NodeSelection : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var viewInt: View
    private var messageKeep: String? = null

    fun setView(_view: View)
    {
        viewInt = _view
    }
     fun  getView2() :View
    {
        return viewInt
    }
    fun setMsg(_msg: String?)
    {
        messageKeep = _msg
    }

    fun getMsg(): String?
    {
        return messageKeep
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
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
                            findNavController().navigate(R.id.action_NodeSelection_to_ConnectFragment)
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

        val myView  = inflater.inflate(R.layout.fragment_node_selection, container, false) as ConstraintLayout

        return myView

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val _nodeslist: ArrayList<String?> =
            arguments?.getStringArrayList(MQTT_NODES_KEY) as ArrayList<String?>

        _nodeslist.removeAt(0) //Znikanie tego węzła rootwego z widoku przelaczników. Ale moznaby zrobić żeby zwracał topologie np.
        val linear_layoutButton = view.findViewById<LinearLayout>(R.id.linear_layoutButton)
        setView(view)
        //val linear_layoutButton = requireView().findViewById<LinearLayout>(R.id.linear_layoutButton)

        for (element in (_nodeslist.indices))//iteracja po switchach i odczytanie ich wartości
        {
            println(element)
            println(_nodeslist[element])
            if(_nodeslist[element] != "")
            {
                val buttonText: String? = _nodeslist[element]
                val button = Button(context)//
                button.setId(element + 1)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(50, 30, 50, 20)
                button.setText("Pobierz status przełączników: $buttonText")
                button.setPadding(50, 20, 50, 20)

                button.setOnClickListener {
                    //Toast.makeText(context, "Nacisnieto przycisk $buttonText", Toast.LENGTH_SHORT).show()
                    getNodeInfo(buttonText)
                    createSwitchControl()
                }
                button.setLayoutParams(params)

                if (linear_layoutButton != null) {
                    linear_layoutButton.addView(button)
                }
            }
        }
    }

    fun createSwitchControl(){
        val mqttClient = MqttClient.getmqttClient()
        mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                //val msg = "Dupa1234"
                //Log.d(this.javaClass.name, msg)
                //oast.makeText(context, msg, Toast.LENGTH_SHORT).show()

                deserializeJson(message.toString())

                //tutaj dodajemy obsługe JSONA otrzymanego-----------------------------------//
                //handleMessages(message.toString(),topic.toString())
                //convert()
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
    }


    fun getNodeInfo(nodeId: String?,_message: String = "getSwitchStatus" ) {
        //tutaj subscribe
        val topic_from = "painlessMesh/from/" + nodeId;
        val topic_to = "painlessMesh/to/" + nodeId;
        if (MqttClient.isConnected()) {
            MqttClient.subscribe(topic_from,
                1,
                object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        val msg = "Subscribed to node, $topic_from trying node info: "
                        Log.d(this.javaClass.name, msg)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.d(this.javaClass.name, "Failed to subscribe: $topic_from")
                    }

                })

            MqttClient.publish(topic_to,
                _message,
                1,
                false,
                object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        val msg = "Publish message: $_message to topic: $topic_to"
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

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment NodeSelection.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            NodeSelection().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
    fun deserializeJson(_message: String)
    {
        val linear_SwitchList = view?.findViewById<LinearLayout>(R.id.linear_SwitchList)

        var iter: Int = 0
        val view: (NodeSelection) -> View? = NodeSelection::getView
        //val json3  = """{"NodeData":{"nodeId":3186720073,"time":12},"SwitchData":[{"key":"Switch0","value":1},{"key":"Switch1","value":1},{"key":"Switch2","value":1},{"key":"Switch3","value":1}]}"""
        val mapper = jacksonObjectMapper()
        mapper.configure( DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true )

        val RootData: RootData = mapper.readValue<RootData>("""$_message""")

        println(RootData.nodeData)//Odczytanie wartości Node'a

        setMsg(_message)//Tu trzeba jakis warunek na zapamietywanie tylko jeśli odpowiedź jest git

        val switchlist : List<switchData> = RootData.switchData // Odczytanie obiektu Switchy

        if(switchlist.isNotEmpty())//Sprawdzenie czy lista nie pusta bo jak nie pusta to nie chcemy generować za dużo przycisków
        {
            if (linear_SwitchList != null)
            {
                linear_SwitchList.removeAllViews()
            }

        }
        for(element in switchlist){//iteracja po switchach i odczytanie ich wartości

            println(element)

            val buttonText: String? = element.key
            val button = Button(context)

            button.setId(iter + 1)
            val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
             )
            params.setMargins(10, 10, 10, 10)
            button.setText("Sw: $buttonText")
            button.setPadding(20, 20, 20, 20)

            if(element.value == 1)
            {
                button.setBackgroundColor(Color.RED)
            }
            else
            {
                button.setBackgroundColor(Color.GREEN)
            }



            button.setOnClickListener {
                //Toast.makeText(context, "Nacisnieto przycisk $buttonText", Toast.LENGTH_SHORT).show()
                serializeJson(element.key)
                //button.setBackgroundColor(Color.GREEN)//Jakies statusowanie w zależoności od statusu przycisku : https://stackoverflow.com/questions/6615723/getting-child-elements-from-linearlayout
                //getNodeInfo(buttonText)
                //createSwitchControl()
            }
            button.setLayoutParams(params)


            if (linear_SwitchList != null)
            {
                linear_SwitchList.addView(button)
            }

            iter+=1
        }

    }


    fun serializeJson(_pressId: String = "Switch0")
    {
        val _message = getMsg()
        val mapper = jacksonObjectMapper()
        mapper.configure( DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true )

        val RootData: RootData = mapper.readValue<RootData>("""$_message""")

        val switchlistResponse : List<switchData> = RootData.switchData // Odczytanie obiektu Switchy


        for(element in switchlistResponse) {//iteracja po switchach i edycja wartości
            if(_pressId == element.key ) element.value = element.value xor 1
        }

        val NodeResponse = nodeData(RootData.nodeData.nodeId, 9999)

        val WholeJsonResponse = RootData(NodeResponse,switchlistResponse as MutableList<switchData>)// Tworzę całą odpowiedź
        println(WholeJsonResponse)

        println(mapper.writeValueAsString(WholeJsonResponse))


        val topic_to = "painlessMesh/to/" + RootData.nodeData.nodeId
        val _messageSending = mapper.writeValueAsString(WholeJsonResponse)
        MqttClient.publish(topic_to,
            _messageSending,
            2,
            false,
            object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {

                    setMsg(_messageSending)//Zapamietanie co sie zmienilo
                    val msg = "Publish message: $_message to topic: $topic_to"
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



    }

}