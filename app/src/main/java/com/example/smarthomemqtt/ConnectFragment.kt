package com.example.smarthomemqtt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class ConnectFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_connect, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.button_defaultvalues).setOnClickListener { // Wypełnienie pól domyslnymi wartościami, brokera MQTT oraz id clienta
            view.findViewById<EditText>(R.id.editText_serverAddr).setText(MQTT_SERVER_URI)
            view.findViewById<EditText>(R.id.editText_clientId).setText(MQTT_CLIENT_ID)
        }

        view.findViewById<Button>(R.id.button_connect).setOnClickListener{ // Odczytanie wartości z text editów w momencie nacisniećia przycisku connect i przejście do kolejnego ekranu

            val serverURLTextEdit   = view.findViewById<EditText>(R.id.editText_serverAddr).text.toString()
            val clientIDTextEdit    = view.findViewById<EditText>(R.id.editText_clientId).text.toString()

            val mqttCredentials = bundleOf(MQTT_SERVER_URI_KEY to serverURLTextEdit,
                                                  MQTT_CLIENT_ID_KEY  to clientIDTextEdit)

            findNavController().navigate(R.id.action_ConnectFragment_to_ManageFragment,mqttCredentials )
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}