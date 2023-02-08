package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewModelScope
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.androidx.viewmodel.ext.android.viewModel

class AuthenticationActivity : AppCompatActivity() {

    private lateinit var binding : ActivityAuthenticationBinding

    companion object {
        const val TAG = "AuthenticationActivity"
        const val SIGN_IN_RESULT_CODE = 1001
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    private val viewModel by this.viewModels<LoginViewModel>()

    val _viewModel: RemindersListViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isLogout = intent.getBooleanExtra("isLogout",false)
        if (isLogout) _viewModel.deleteReminders()

        this.binding = ActivityAuthenticationBinding.inflate(this.layoutInflater)

        this.binding.loginButton.setOnClickListener { this.launchSignInFlow() }

        this.viewModel.authenticationState.observe(this) { authenticateState ->
            when (authenticateState) {
                LoginViewModel.AuthenticationState.AUTHENTICATED -> {
                    Log.e(
                        AuthenticationActivity.TAG,
                        "\"Authenticate state that doesn't require any UI change $authenticateState"
                    )
                }

                else -> Log.e(
                    AuthenticationActivity.TAG,
                    "\"Authenticate failed : $authenticateState"
                )
            }

        }

        this.setContentView(this.binding.root)

    }

    private fun launchSignInFlow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build()
        )

        this.startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(
                        providers
                ).build(), AuthenticationActivity.SIGN_IN_RESULT_CODE
        )

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AuthenticationActivity.SIGN_IN_RESULT_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode != Activity.RESULT_OK) {
                if (response!!.isNewUser) _viewModel.deleteReminders()
                Log.i(
                    AuthenticationActivity.TAG,
                    "Successfully signed in user " +
                            "${FirebaseAuth.getInstance().currentUser?.displayName}!"
)
                val intent = Intent(this, RemindersActivity::class.java).apply {
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                this.startActivity(intent)
            } else {
                Log.i(AuthenticationActivity.TAG, "Sign in unsuccessful ${response?.error?.errorCode}")

            }
        }
    }

}
