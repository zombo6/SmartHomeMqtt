package com.example.smarthomemqtt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [NodeSelection.newInstance] factory method to
 * create an instance of this fragment.
 */

class NodeSelection : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

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

        val linear_layoutButton = view.findViewById<LinearLayout>(R.id.linear_layoutButton)
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
                    Toast.makeText(context, "Nacisnieto przycisk $buttonText", Toast.LENGTH_LONG).show()
                }
                button.setLayoutParams(params)

                if (linear_layoutButton != null) {
                    linear_layoutButton.addView(button)
                }
            }
        }

        if (MqttClient.isConnected()) {
            Toast.makeText(context, "Mamy zmienna ", Toast.LENGTH_LONG).show()
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
}