package com.cis436.p3

import android.R
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.lifecycle.ViewModelProvider
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.cis436.p3.databinding.FragmentCatListBinding
import org.json.JSONArray
import org.json.JSONObject

class CatListFragment : Fragment() {

    data class Cat(
        val name: String,
        val temperament: String,
        val origin: String,
        val referenceImageId: String,
        var imageUrl: String
    )

    private var _binding: FragmentCatListBinding? = null
    private val binding get() = _binding!!

    private lateinit var spinner: Spinner
    private lateinit var viewModel: CatViewModel
    private val cats: MutableList<Cat> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCatListBinding.inflate(inflater, container, false)
        spinner = binding.catSpinner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(CatViewModel::class.java)
        populateSpinner()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // method to get cat list through API
    private fun populateSpinner() {
        var catUrl = "https://api.thecatapi.com/v1/breeds" + "?api_key="

        val queue = Volley.newRequestQueue(requireContext())

        val stringRequest = StringRequest(
            Request.Method.GET, catUrl,
            { response ->
                val catsArray: JSONArray = JSONArray(response)
                for (i in 0 until catsArray.length()) {
                    val theCat: JSONObject = catsArray.getJSONObject(i)

                    val cat = Cat(
                        theCat.getString("name"),
                        theCat.getString("temperament"),
                        theCat.getString("origin"),
                        if (theCat.has("reference_image_id")) theCat.getString("reference_image_id") else "",
                        ""
                    )
                    cats.add(cat)
                }
                val adapter = ArrayAdapter(
                    requireContext(),
                    R.layout.simple_spinner_item,
                    cats.map { it.name }
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter
            },
            {
                Log.e("CatListFragment", "Failed to fetch cat names")
            })
        queue.add(stringRequest)

        // call the function to handle spinner item selection
        setupSpinnerListener()
    }

    // method to handle the cat selection
    private fun setupSpinnerListener() {
        viewModel = ViewModelProvider(requireActivity())[CatViewModel::class.java]
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedCat = cats[position]

                if (selectedCat.referenceImageId.isNotEmpty()) {
                    val catImageUrl =
                        "https://api.thecatapi.com/v1/images/${selectedCat.referenceImageId}" +
                                "?api_key="

                    val queue = Volley.newRequestQueue(requireContext())

                    val stringRequest = StringRequest(
                        Request.Method.GET, catImageUrl,
                        { response ->
                            val catImage: JSONObject = JSONObject(response)
                            val imageUrl = catImage.getString("url")
                            selectedCat.imageUrl = imageUrl

                            // Set the selected cat info in the ViewModel after setting the image URL
                            viewModel.setSelectedCatInfo(
                                selectedCat.name,
                                selectedCat.temperament,
                                selectedCat.origin,
                                selectedCat.referenceImageId,
                                selectedCat.imageUrl
                            )
                        },
                        {
                            Log.e("CatListFragment", "Failed to fetch cat image")
                        })
                    queue.add(stringRequest)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }
}