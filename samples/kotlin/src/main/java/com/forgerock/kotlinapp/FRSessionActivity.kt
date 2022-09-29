/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.forgerock.kotlinapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import net.openid.appauth.AuthorizationRequest
import org.forgerock.android.auth.*
import java.lang.Exception


class FRSessionActivity: AppCompatActivity(), NodeListener<FRSession>, ActivityListener {

    private val status: TextView by lazy { findViewById(R.id.status) }
    private val loginButton: Button by lazy { findViewById(R.id.login) }
    private val logoutButton: Button by lazy { findViewById(R.id.logout) }
    private val classNameTag = FRSessionActivity::class.java.name
    private var userInfoFragment: UserInfoFragment? = null
    private var nodeDialog: NodeDialogFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        updateStatus(showLogin = true)
        loginButton.setOnClickListener {
            // FRSession.authenticate() method will take as an input the Tree/Journey name and output the SSO token, to get a Access Token you have to invoke the getAccessToken method.
            val journeyName = "SimpleLogin"
            FRSession.authenticate(this, journeyName, this)
        }
        logoutButton.setOnClickListener {
            logout()
        }
    }

    override fun onSuccess(result: FRSession?) {
        getAccessToken()
    }


    override fun onException(e: Exception?) {
      print("------> $e")
    }

    override fun onCallbackReceived(node: Node?) {
        nodeDialog?.dismiss()
        nodeDialog = NodeDialogFragment.newInstance(node)
        nodeDialog?.show(supportFragmentManager, NodeDialogFragment::class.java.name)
    }


    private fun getAccessToken() {
        FRUser.getCurrentUser()?.getAccessToken(object : FRListener<AccessToken> {
            override fun onSuccess(token: AccessToken?) {
                runOnUiThread {
                    loginButton.visibility = View.GONE
                    logoutButton.visibility = View.GONE
                    status.visibility = View.GONE
                    token?.let {
                        launchUserInfoFragment(token)
                    }
                }
            }

            override fun onException(e: Exception?) {
                Logger.error(classNameTag, e?.message)
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


    private fun launchUserInfoFragment(token: AccessToken) {
        userInfoFragment = UserInfoFragment.newInstance(
            token.value,
            token.refreshToken,
            token.idToken,
            this
        )
        userInfoFragment?.let {
            supportFragmentManager.beginTransaction()
                .add(R.id.container, it).commit()
        }
    }

    override fun logout() {
        FRSession.getCurrentSession().logout()
        userInfoFragment?.let {
            supportFragmentManager.beginTransaction().remove(it).commit()
        }
        updateStatus(true)
    }


}