/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.forgerock.kotlinapp

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import net.openid.appauth.AuthorizationRequest
import org.forgerock.android.auth.*
import org.forgerock.android.auth.exception.AuthenticationRequiredException


interface ActivityListener {
    fun logout()
}

class MainActivity: AppCompatActivity(), NodeListener<FRUser>, ActivityListener {

    private val status: TextView by lazy { findViewById(R.id.status) }
    private val loginButton: Button by lazy { findViewById(R.id.login) }
    private val logoutButton: Button by lazy { findViewById(R.id.logout) }
    private val classNameTag = MainActivity::class.java.name
    private var userInfoFragment: UserInfoFragment? = null
    private var nodeDialog: NodeDialogFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        updateStatus()
        loginButton.setOnClickListener {
            FRUser.login(applicationContext, this)
            if(BuildConfig.embeddedLogin) {
                FRUser.login(applicationContext, this)
            }
            else {
                centralizedLogin()
            }
        }
        logoutButton.setOnClickListener {
            logout()
        }
    }

    override fun onStart() {
        super.onStart()
        userInfoFragment?.let {
            supportFragmentManager.beginTransaction().remove(it).commit()
        }
        
        if(FRUser.getCurrentUser() == null) {
            updateStatus(true)
        } else {
            try {
                val currentUser = FRUser.getCurrentUser()
                updateStatus(false)
                launchUserInfoFragment(currentUser.accessToken, currentUser)
            }
            catch(e: AuthenticationRequiredException) {
                updateStatus(true)
            }

        }
    }

    private fun centralizedLogin() {
        FRUser.browser().appAuthConfigurer()
            .authorizationRequest { r: AuthorizationRequest.Builder ->
                // Add a login hint parameter about the user:
                r.setLoginHint("demo@example.com")
                // Request that the user re-authenticates:
                r.setPrompt("login")
            }
            .customTabsIntent { t: CustomTabsIntent.Builder ->
                // Customize the browser:
                t.setShowTitle(true)
                t.setToolbarColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            }.done()
            .login(this, object : FRListener<FRUser?> {
                override fun onSuccess(result: FRUser?) {
                    Logger.debug(classNameTag, result?.accessToken?.value)
                    getUserInfo(result)
                }
                override fun onException(e: java.lang.Exception) {
                    Logger.error(classNameTag, e.message)
                }
            })
    }


    private fun updateStatus(showLogin: Boolean = false) {
        runOnUiThread {
            (if(showLogin) View.VISIBLE else View.GONE).also {
                loginButton.visibility = it
                logoutButton.visibility = it
                status.visibility = it
            }
            loginButton.apply { this.isEnabled = showLogin == true }
            logoutButton.apply { this.isEnabled = showLogin == false }
            status.text = if(showLogin) "User is not authenticated" else "User is authenticated"
        }
    }


    private fun getUserInfo(result: FRUser?) {
        result?.getAccessToken(object : FRListener<AccessToken> {
            override fun onSuccess(token: AccessToken) {
                runOnUiThread {
                    loginButton.visibility = View.GONE
                    logoutButton.visibility = View.GONE
                    status.visibility = View.GONE
                    launchUserInfoFragment(token, result)
                }
            }
            override fun onException(e: java.lang.Exception) {

            }
        })
    }


    private fun launchUserInfoFragment(token: AccessToken, result: FRUser?) {
        userInfoFragment = UserInfoFragment.newInstance(
            result?.accessToken?.value,
            token.refreshToken,
            token.idToken,
            this@MainActivity
        )
        userInfoFragment?.let {
            supportFragmentManager.beginTransaction()
                .add(R.id.container, it).commit()
        }
    }

    override fun onSuccess(result: FRUser?) {
        getUserInfo(result)
    }

    override fun onException(e: Exception?) {
        Logger.error(classNameTag, e?.message, e)
        runOnUiThread {
            val dialogBuilder = AlertDialog.Builder(this)
            // set message of alert dialog
            dialogBuilder.setMessage("Login Failed. Retry Again")
                // if the dialog is cancelable
                .setCancelable(false)
                // positive button text and action
                .setPositiveButton("OK", DialogInterface.OnClickListener { _, _ -> })
            // create dialog box
            val alert = dialogBuilder.create()
            // set title for alert dialog box
            alert.setTitle("Unauthorized")
            // show alert dialog
            alert.show()
        }
    }

    override fun onCallbackReceived(node: Node?) {
        nodeDialog?.dismiss()
        nodeDialog = NodeDialogFragment.newInstance(node)
        nodeDialog?.show(supportFragmentManager, NodeDialogFragment::class.java.name)

    }

    override fun logout() {
        FRUser.getCurrentUser().logout()
        userInfoFragment?.let {
            supportFragmentManager.beginTransaction().remove(it).commit()
        }
        updateStatus(true)
    }

}