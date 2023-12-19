/*
 * Copyright (c) 2021 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.idp

import android.os.Bundle
import android.os.OperationCanceledException
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.facebook.CallbackManager
import com.facebook.CallbackManager.Factory.create
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import org.forgerock.android.auth.FRListener
import org.forgerock.android.auth.InitProvider
import org.forgerock.android.auth.Listener

/**
 * [IdPHandler] to handle Facebook login
 */
class FacebookSignInHandler : Fragment(), IdPHandler {
    private var listener: FRListener<IdPResult>? = null
    private lateinit var callbackManager: CallbackManager
    private var idPClient: IdPClient? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            idPClient = arguments?.getSerializable(IdPHandler.IDP_CLIENT) as? IdPClient
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        LoginManager.getInstance().logOut()
        callbackManager = create()
        // Callback registration
        LoginManager.getInstance()
            .registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    Listener.onSuccess(listener, IdPResult(result.accessToken.token))
                }

                override fun onCancel() {
                    Listener.onException(listener, OperationCanceledException())
                }

                override fun onError(error: FacebookException) {
                    Listener.onException(listener, error)
                }
            })

        idPClient?.let {
            LoginManager.getInstance()
                .logInWithReadPermissions(this, callbackManager, getPermissions(it))
        }
    }

    override fun getTokenType(): String {
        return IdPHandler.ACCESS_TOKEN
    }

    override fun signIn(idPClient: IdPClient, listener: FRListener<IdPResult>) {
        val fragmentManager =
            InitProvider.getCurrentActivityAsFragmentActivity().supportFragmentManager
        signIn(fragmentManager, idPClient, listener)
    }

    override fun signIn(fragment: Fragment, idPClient: IdPClient, listener: FRListener<IdPResult>) {
        signIn(fragment.getParentFragmentManager(), idPClient, listener)
    }

    private fun signIn(
        fragmentManager: FragmentManager,
        idPClient: IdPClient,
        listener: FRListener<IdPResult>
    ) {
        val existing = fragmentManager.findFragmentByTag(TAG) as? FacebookSignInHandler
        if (existing != null) {
            existing.listener = null
            fragmentManager.beginTransaction().remove(existing).commitNow()
        }
        val args = Bundle()
        args.putSerializable(IdPHandler.IDP_CLIENT, idPClient)
        setArguments(args)
        this.listener = listener
        fragmentManager.beginTransaction().add(this, TAG)
            .commit()
    }

    /**
     * The request permissions
     *
     * @return The Request permissions
     */
    open fun getPermissions(idPClient: IdPClient): List<String> {
        return idPClient.getScopes()
    }

    companion object {
        val TAG = FacebookSignInHandler::class.java.name
    }
}
