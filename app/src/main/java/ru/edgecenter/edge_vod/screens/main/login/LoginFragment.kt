package ru.edgecenter.edge_vod.screens.main.login

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import edge_vod.R
import edge_vod.databinding.FragmentLoginBinding
import io.reactivex.disposables.CompositeDisposable
import ru.edgecenter.edge_vod.data.remote.RemoteAccessManager
import ru.edgecenter.edge_vod.data.remote.account.auth.AuthRequestBody
import ru.edgecenter.edge_vod.data.remote.account.auth.AuthResponse
import ru.edgecenter.edge_vod.utils.connectivity_state.ConnectivityProvider
import ru.edgecenter.edge_vod.utils.extensions.findTopNavController
import java.util.regex.Pattern

class LoginFragment : Fragment(R.layout.fragment_login),
    ConnectivityProvider.ConnectivityStateListener {

    private val provider: ConnectivityProvider by lazy {
        ConnectivityProvider.createProvider(
            requireContext()
        )
    }
    private var hasInternet = false

    private lateinit var binding: FragmentLoginBinding
    private val compositeDisposable = CompositeDisposable()

    private var eMailIsCorrect = false
    private var passwordIsCorrect = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        provider.addListener(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentLoginBinding.bind(view)

        binding.etEmail.doAfterTextChanged {
            if (it.isNullOrEmpty()) {
                binding.loginButton.isEnabled = false
            } else {
                eMailIsCorrect = isValidEmail(it)
                binding.loginButton.isEnabled = eMailIsCorrect && passwordIsCorrect
            }
        }
        binding.etPassword.doAfterTextChanged {
            if (it.isNullOrEmpty()) {
                binding.loginButton.isEnabled = false
            } else{
                passwordIsCorrect = isValidPassword(it)
                binding.loginButton.isEnabled = eMailIsCorrect && passwordIsCorrect
            }
        }

        binding.loginButton.setOnClickListener {
            binding.loginButton.isEnabled = false
            hideKeyboard()

            if (!hasInternet) {
                binding.loginButton.isEnabled = true
                showToast(R.string.check_internet)
            } else {
                auth()
            }
        }
        binding.seeDemoBtn.setOnClickListener {
            findTopNavController().navigate(R.id.action_loginFragment_to_viewingFragment2)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        provider.removeListener(this)
        compositeDisposable.dispose()
    }

    private fun isValidEmail(email: CharSequence) = Patterns.EMAIL_ADDRESS.matcher(email).matches()
        .also {
            if (it) {
                binding.tilEmail.error = ""
            } else {
                binding.tilEmail.error = getString(R.string.etEmail_is_incorrect)
            }
        }

    private fun isValidPassword(password: CharSequence) =
        Pattern.compile(passwordValidateRegex).matcher(password).matches()
            .also {
                if (it) {
                    binding.tilPassword.error = ""
                } else {
                    binding.tilPassword.error = getString(R.string.etPassword_is_incorrect)
                }
            }

    private fun auth() {
        val requestBody = AuthRequestBody(
            eMail = binding.etEmail.text.toString(),
            password = binding.etPassword.text.toString()
        )
        try {
            compositeDisposable.add(
                RemoteAccessManager
                    .auth(requireActivity().application, requestBody)
                    .subscribe({ authResponse ->

                        showToast(R.string.logged_success)
                        saveAuthData(requestBody, authResponse)
                        findNavController().navigate(R.id.action_loginFragment_to_tabsFragment)
                    }, { throwable ->

                        showToast(R.string.logged_fail)
                        binding.etPassword.text?.clear()
                        binding.loginButton.isEnabled = false

                        Log.e(TAG, "Result ${throwable.localizedMessage}")
                    })
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
        }
    }

    private fun saveAuthData(requestBody: AuthRequestBody, authResponse: AuthResponse) {
        requireContext().getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE)
            .edit()
            .putString(RemoteAccessManager.EMAIL_KEY, requestBody.eMail)
            .putString(RemoteAccessManager.PASSWORD_KEY, requestBody.password)
            .putString(RemoteAccessManager.REFRESH_TOKEN_KEY, authResponse.refreshAccessToken)
            .putString(RemoteAccessManager.ACCESS_TOKEN_KEY, authResponse.accessToken)
            .apply()
    }

    private fun hideKeyboard() {
        requireActivity().currentFocus.let { view ->
            if (view != null) {
                (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        hasInternet =
            (state as? ConnectivityProvider.NetworkState.ConnectedState)?.hasInternet == true
    }

    private fun showToast(resId: Int) {
        Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show()
    }

    companion object {
        /*
         * (?=.*[0-9]) - string contains at least one number;
         * (?=.*[!@#$%^&*]) - string contains at least one special character;
         * (?=.*[a-z]) - string contains at least one lowercase Latin letter;
         * (?=.*[A-Z]) - string contains at least one uppercase latin letter;
         * [0-9a-zA-Z!@#$%^&*]{8,} - the string consists of at least 8 of the above characters.
         */
        private const val passwordValidateRegex =
            """(?=.*[0-9])(?=.*[!@#${'$'}%^&*])(?=.*[a-z])(?=.*[A-Z])[0-9a-zA-Z!@#${'$'}%^&*]{8,}"""

        const val TAG = "LoginFragment"
    }
}