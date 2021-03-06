package com.example.smarthomemqtt

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
    val nodeData: nodeData,
    val switchData: MutableList<switchData>
)

data class nodeData(
    val nodeId: Long,
    val time: Long
)
data class switchData(
    val key: String,
    var value: Int
)

val DeviceAcronyms: MutableMap<String, String> = mutableMapOf("3187194449" to "Akwarium", "2830009614" to "Wentylator", "1613741825" to "Pokoj")
//Przepisanie tego w przyszłosci
lateinit var MqttClient: MQTTClient
var DEBUG_EN: Boolean = false
class NodeSelection : Fragment()  {
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

        setHasOptionsMenu(true)

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val myView  = inflater.inflate(R.layout.fragment_node_selection, container, false) as ConstraintLayout
        createConnection()//Przeniesione z ManageFragment
        //createSwitchControl()// Callback i obsługa otrzymywanych wiadomości
        return myView

    }

    fun createConnection()
    {
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
                        findNavController().navigate(R.id.action_NodeSelection_to_ConnectFragment)
                    }
                },
                object : MqttCallback {
                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        val msg =
                            "Receive message: ${message.toString()} from topic: $topic"
                        Log.d(this.javaClass.name, msg)

                        //Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        //tutaj dodajemy obsługe JSONA otrzymanego-----------------------------------//
                        handleMessages(message.toString(),topic.toString())
                        //convert()

                    }

                    override fun connectionLost(cause: Throwable?) {
                        Log.d(this.javaClass.name, "Connection lost ${cause.toString()}")
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        Log.d(this.javaClass.name, "Delivery complete")
                    }
                })


        } else {
            // Arguments are not valid, come back to Connect Fragment
            findNavController().navigate(R.id.action_NodeSelection_to_ConnectFragment)
        }


    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean { //https://stackoverflow.com/questions/8308695/how-to-add-options-menu-to-fragment-in-android
        return when (item.itemId) {
            R.id.action_settings -> true

            R.id.action_refresh-> {
                getNodesMQTTGateway()
                Toast.makeText(context, "Refreshing nodelist", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }



    fun CreateNodeControl(_nodeslist: ArrayList<String?>)
    {
        val _RootNode: String
        val view: View = getView2()
        val linear_layoutButton = view.findViewById<LinearLayout>(R.id.linear_layoutButton)
        //Koniecznośc wyczyszczenia layoutu urzadzen po odświeżeniu
        if(_nodeslist.isNotEmpty())//Sprawdzenie czy lista nie pusta bo jak nie pusta to nie chcemy generować za dużo przycisków
        {
            if (linear_layoutButton != null)
            {
                linear_layoutButton.removeAllViews()
            }
        }
        _RootNode = _nodeslist.get(0).toString()
        _nodeslist.removeAt(0) //Znikanie tego węzła rootwego z widoku przelaczników. Ale moznaby zrobić żeby zwracał topologie np.
        setView(view)

        for (element in (_nodeslist.indices))//iteracja po switchach i odczytanie ich wartości
        {
            println(element)
            println(_nodeslist[element])
            if(_nodeslist[element] != "")
            {
                val buttonText: String?
                val buttonId = _nodeslist[element];

                if(DeviceAcronyms.contains(_nodeslist[element]))
                {
                    buttonText = DeviceAcronyms.get(_nodeslist[element])
                }
                else
                {
                    buttonText = _nodeslist[element]
                }

//                if(_nodeslist[element] == "3187194449") {
//
//                    buttonText = "Akwarium";
//                }
//                else if(_nodeslist[element] == "2830009614") {
//
//                    buttonText = "Wentylator";
//                }
//                else if(_nodeslist[element] == "1613741825") {
//
//                    buttonText = "Pokoj";
//                }

                val button = Button(context)//
                button.setId(element + 1)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                //params.setMargins(50, 30, 50, 20)
                params.gravity = 1
                button.setText("Pobierz status przełączników: $buttonText")
                button.setPadding(50, 20, 50, 20)

                button.setOnClickListener {
                    //Toast.makeText(context, "Nacisnieto przycisk $buttonText", Toast.LENGTH_SHORT).show()
                    getNodeInfo(buttonId)
                    //createSwitchControl()
                }
                button.setLayoutParams(params)

                if (linear_layoutButton != null) {
                    linear_layoutButton.addView(button)
                }
            }
        }
//        //Pobierz topologie
//        if(_RootNode != "")
//        {
//            val buttonText: String? = _RootNode
//            val button = Button(context)//
//            button.setId(_nodeslist.size + 1)
//            val params = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.WRAP_CONTENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT
//            )
//            // params.setMargins(150, 50, 50, 20)
//            params.gravity = 1
//            button.setText("Pobierz Topologie")
//            button.setPadding(20, 20, 50, 20)
//
//            button.setOnClickListener {
//                //Toast.makeText(context, "Nacisnieto przycisk $buttonText", Toast.LENGTH_SHORT).show()
//                getNodeInfo("gateway","getTopology")
//            }
//            button.setLayoutParams(params)
//
//            if (linear_layoutButton != null) {
//                linear_layoutButton.addView(button)
//            }
//        }


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setView(view)
    }

    fun createSwitchControl(){
        val mqttClient = MqttClient.getmqttClient()
        mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                handleMessages(message.toString(),topic.toString())
            }
            override fun connectionLost(cause: Throwable?) {
                Log.d(this.javaClass.name, "Connection lost ${cause.toString()}")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d(this.javaClass.name, "Delivery complete")
            }
        })
    }

    fun handleMessages(_message: String, _topic: String)// Obsługa zdarzeń na podstawie otrzymanej wiadomości
    {
        if(_topic == "painlessMesh/from/gateway")
        {//tutaj przejście do next ekranu z parametrami nodeów przekazywanymi, przygotowanie w pętli do przekzania SETA

            val _nodeslist: List<String> = _message.split(" ")
            val nodesData =  ArrayList(_nodeslist)
            if(_message != "Client not connected!" && !_message.contains("subs"))
            {
                CreateNodeControl(nodesData)
            }
            else
            {
                Toast.makeText(context, "Please refresh. Mesh not initialized", Toast.LENGTH_SHORT).show()
            }
        }
        else
        {
            deserializeJson(_message.toString())
        }
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
                        if(DEBUG_EN)
                        {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
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
                        if(DEBUG_EN)
                        {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
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
            button.setText("$buttonText")
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
                    if(DEBUG_EN)
                    {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                    }

                override fun onFailure(
                    asyncActionToken: IMqttToken?,
                    exception: Throwable?
                ) {
                    Log.d(this.javaClass.name, "Failed to publish message to topic")
                }
            })



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
                        if(DEBUG_EN)
                        {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
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
                        if(DEBUG_EN)
                        {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
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