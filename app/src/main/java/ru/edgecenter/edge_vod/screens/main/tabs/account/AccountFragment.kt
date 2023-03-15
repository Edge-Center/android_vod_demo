package ru.edgecenter.edge_vod.screens.main.tabs.account

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.navOptions
import edge_vod.R
import edge_vod.databinding.FragmentAccountBinding
import io.reactivex.disposables.CompositeDisposable
import ru.edgecenter.edge_vod.data.remote.RemoteAccessManager
import ru.edgecenter.edge_vod.utils.extensions.findTopNavController

class AccountFragment : Fragment(R.layout.fragment_account) {

    private var binding: FragmentAccountBinding? = null

    private val compositeDisposable = CompositeDisposable()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAccountBinding.bind(view)

        if (savedInstanceState == null) {
            loadAccountData()
        } else {
            binding?.let {
                it.userName.text = savedInstanceState.getString(USER_NAME_KEY, "")
                it.email.text = savedInstanceState.getString(EMAIL_KEY, "")
            }
        }

        binding?.signOut?.setOnClickListener { signOut() }
    }

    private fun loadAccountData() {
        binding?.progressBar?.visibility = View.VISIBLE
        compositeDisposable.add(
            RemoteAccessManager.getAccountDetails(requireActivity().application)
                .subscribe({ accountData ->
                    binding?.let {
                        it.userName.text = accountData.name
                        it.email.text = accountData.email
                        it.progressBar.visibility = View.GONE
                    }
                }, {
                    it.printStackTrace()
                })
        )
    }

    private fun signOut() {
        RemoteAccessManager.signOut(requireActivity().application)
        findTopNavController().navigate(
            R.id.loginFragment,
            null,
            navOptions {
                popUpTo(R.id.main_graph) {
                    inclusive = true
                }
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        binding?.let {
            outState.putString(USER_NAME_KEY, it.userName.text.toString())
            outState.putString(EMAIL_KEY, it.email.text.toString())
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
    }

    companion object {
        private const val USER_NAME_KEY = "userNameKey"
        private const val EMAIL_KEY = "emailKey"
    }
}