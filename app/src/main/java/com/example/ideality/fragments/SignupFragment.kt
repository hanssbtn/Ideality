package com.example.ideality.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.Toast
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.Fragment
import com.example.ideality.R
import com.google.android.material.textfield.TextInputEditText
import com.hbb20.CountryCodePicker
import java.util.Date
import java.util.Locale

class SignupFragment: Fragment() {
    private val tag = "SignupFragment"
    private lateinit var motionLayout: MotionLayout
    private lateinit var businessSignupButton: RadioButton
    private lateinit var customerSignupButton: RadioButton

    private lateinit var customerNameField: EditText
    private lateinit var customerEmailField: EditText
    private lateinit var customerAddressField: EditText
    private lateinit var customerPhone: CountryCodePicker
    private lateinit var customerPhoneEditText: TextInputEditText
    private lateinit var customerDOBField: TextInputEditText
    private lateinit var customerPasswordField: TextInputEditText

    private lateinit var businessOwnerField: EditText
    private lateinit var businessNameField: EditText
    private lateinit var businessEmailField: EditText
    private lateinit var businessAddressField: EditText
    private lateinit var businessPhone: CountryCodePicker
    private lateinit var businessPhoneEditText: TextInputEditText
    private lateinit var businessPasswordField: TextInputEditText

    private var signupOption = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_signup, container, false)
        Log.d(tag, "OnCreateView: business signup tag is ${view.findViewById<ScrollView>(R.id.business_signup)?.tag ?: "null"}")
        Log.d(tag, "OnCreateView: customer signup tag is ${view.findViewById<ScrollView>(R.id.customer_signup)?.tag ?: "null"}")
        return view
    }

    private fun getIDs(view: View) {
        motionLayout = view.findViewById(R.id.signup_layout)

        customerSignupButton = view.findViewById(R.id.customer_signup_button)
        customerNameField = view.findViewById(R.id.name_field_customer)
        customerEmailField = view.findViewById(R.id.email_field)
        customerPhone = view.findViewById(R.id.country_code_customer)
        customerPhoneEditText = view.findViewById(R.id.phone_number_field_customer)
        customerDOBField = view.findViewById(R.id.date_of_birth_field_customer)
        customerAddressField = view.findViewById(R.id.address_field_customer)
        customerPasswordField = view.findViewById(R.id.password_field_customer)

        businessSignupButton = view.findViewById(R.id.business_signup_button)
        businessOwnerField = view.findViewById(R.id.name_field_business)
        businessNameField = view.findViewById(R.id.business_name_field)
        businessEmailField = view.findViewById(R.id.business_email_field)
        businessAddressField = view.findViewById(R.id.address_field_business)
        businessPhone = view.findViewById(R.id.country_code_business)
        businessPhoneEditText = view.findViewById(R.id.phone_number_field_business)
        businessPasswordField = view.findViewById(R.id.password_field_business)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getIDs(view)

        try {
            businessSignupButton.setOnClickListener { v ->
                Log.d(tag, "onViewCreated.businessSignupButton.OnClickListener: business button clicked.")
                try {
                    customerSignupButton.isChecked = false
                    businessSignupButton.isChecked = true
                    signupOption = 1
                    motionLayout.transitionToEnd()
                } catch (npe: NullPointerException) {
                    Toast.makeText(requireContext(), "Cannot set checked state: ${npe.printStackTrace()}", Toast.LENGTH_SHORT).show()
                }
            }

            customerSignupButton?.setOnClickListener { v ->
                Log.d(tag, "onViewCreated.customerSignupButton.OnClickListener: customer button clicked.")
                try {
                    businessSignupButton.isChecked = false
                    customerSignupButton.isChecked = true
                    signupOption = 0
                    motionLayout.transitionToStart()
                } catch (npe: NullPointerException) {
                    Toast.makeText(requireContext(), "Cannot set checked state: ${npe.printStackTrace()}", Toast.LENGTH_SHORT).show()
                }
            }

            customerNameField.filters = arrayOf(InputFilter.LengthFilter(30))
            customerEmailField.filters = arrayOf(InputFilter.LengthFilter(256))
            businessPhoneEditText.filters = arrayOf(InputFilter.LengthFilter(15))
            businessAddressField.filters = arrayOf(InputFilter.LengthFilter(500))
            businessPasswordField.filters = arrayOf(InputFilter.LengthFilter(30))

            customerNameField.filters = arrayOf(InputFilter.LengthFilter(30))
            customerEmailField.filters = arrayOf(InputFilter.LengthFilter(256))
            customerPhoneEditText.filters = arrayOf(InputFilter.LengthFilter(15))
            customerAddressField.filters = arrayOf(InputFilter.LengthFilter(500))
            customerPasswordField.filters = arrayOf(InputFilter.LengthFilter(30))
            customerDOBField.keyListener = null

            view.findViewById<Button>(R.id.create_account_button).apply {
                setOnClickListener { view ->
                    try {
                        when (signupOption) {
                            0 -> {
                                if (customerNameField.text == null || customerNameField.text.isBlank() /**/) {
                                    Toast.makeText(requireContext(), "Name field is empty.", Toast.LENGTH_SHORT).show()
                                    customerNameField.error = "Name field is empty."
                                    return@setOnClickListener
                                }
                                if (customerEmailField.text.isNullOrBlank() /**/) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Email field is empty.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    customerEmailField.error = "Name field is empty."
                                    return@setOnClickListener
                                }
                                if (customerPhone.fullNumberWithPlus.isNullOrBlank() /**/) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Phone number field is empty.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    customerPhoneEditText.error = "Phone number field is empty."
                                    return@setOnClickListener
                                }
                                if (customerAddressField.text.isNullOrBlank() /**/) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Address field is empty.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@setOnClickListener
                                }
                                if (customerDOBField.text.isNullOrBlank()) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Date of birth is empty.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@setOnClickListener
                                }
                                if (customerPasswordField.text == null || customerPasswordField.text!!.length <= 3/**/) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Password cannot be less than 4 characters.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@setOnClickListener
                                }
                            }

                            1 -> {
                                if (businessOwnerField.text.isNullOrBlank()) {
                                    Toast.makeText(requireContext(), "Name field is empty.", Toast.LENGTH_SHORT).show()
                                    return@setOnClickListener
                                }
                                if (businessNameField.text.isNullOrBlank()) {
                                    Toast.makeText(requireContext(), "Business name field is empty.", Toast.LENGTH_SHORT).show()
                                    return@setOnClickListener
                                }
                                if (businessAddressField.text.isNullOrBlank()) {
                                    Toast.makeText(requireContext(), "Address field is empty.", Toast.LENGTH_SHORT).show()
                                    return@setOnClickListener
                                }
                                if (businessPhone.fullNumberWithPlus.isNullOrBlank()) {
                                    Toast.makeText(requireContext(), "Phone number field is empty.", Toast.LENGTH_SHORT).show()
                                    return@setOnClickListener
                                }
                                if (businessPasswordField.text == null || businessPasswordField.text!!.length <= 3) {
                                    Toast.makeText(requireContext(), "Password cannot be less than 4 characters.", Toast.LENGTH_SHORT).show()
                                    return@setOnClickListener
                                }
                            }
                        }
                        Toast.makeText(
                            requireContext(),
                            "TODO: validate and write account to DB.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (npe: NullPointerException) {
                        Log.e("SignupFragment", "OnViewCreated.create_account_button.OnClickListener: element is null. (${npe.printStackTrace()})")
                    } catch (ise: IllegalStateException) {
                        Log.e("SignupFragment", "OnViewCreated.create_account_button.OnClickListener: cannot get context. (${ise.printStackTrace()})")
                    }
                }
            }
            customerDOBField.setOnClickListener { v ->
                DatePickerDialog(requireContext()).apply {
                    setOnDateSetListener { datePicker: DatePicker, y: Int, m: Int, d: Int ->
                        customerDOBField.setText(
                            String.format(
                                Locale.getDefault(),
                                "%4d-%2d-%2d",
                                y,
                                m,
                                d
                            )
                        )
                        Log.d(
                            tag,
                            "CustomerDOBField.OnClickListener.DatePickerDialog.OnDateSetListener: date set to ${customerDOBField.text}"
                        )
                    }
                    show()
                }
            }
        } catch (npe: NullPointerException) {
            Log.e(tag, "OnViewCreated: got NPE:\n${npe.printStackTrace()}")
        } catch (ise: IllegalStateException) {
            Log.e(tag, "OnViewCreated: cannot get context. (${ise.printStackTrace()})")
        }


    }
}