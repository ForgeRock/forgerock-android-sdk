/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.auth;

import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import org.forgerock.android.auth.WebAuthnDataRepository;
import org.forgerock.android.auth.webauthn.FRWebAuthn;
import org.forgerock.android.auth.webauthn.PublicKeyCredentialSource;

import java.util.ArrayList;
import java.util.List;

public class WebAuthnKeysListActivity extends ListActivity {
    private TextView mainText;
    private String rpId;
    private List<String> listValues;
    private FRWebAuthn frWebAuthn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rpId = getIntent().getStringExtra("RPID");

        setContentView(R.layout.activity_web_authn_keys_list);
        mainText = (TextView) findViewById(R.id.mainText);
        mainText.setText("WebAuthn Credentials for RpID \"" + rpId + "\"");

        WebAuthnDataRepository repository = new WebAuthnDataRepository(this, null);
        frWebAuthn = new FRWebAuthn(this, repository);
        reloadCredentials();
    }

    private void reloadCredentials() {
        List<PublicKeyCredentialSource> credentialSourceList = frWebAuthn.loadAllCredentials(rpId);
        ArrayList<PublicKeyCredentialSource> credentialSourceListArrayList = new ArrayList<>();
        credentialSourceListArrayList.addAll(credentialSourceList);
        // Create a new adapter and display the keys.
        WebAuthnArrayAdapter arrayAdapter = new WebAuthnArrayAdapter(this, credentialSourceListArrayList);
        setListAdapter(arrayAdapter);
    }

    // when an item of the list is clicked
    @Override
    protected void onListItemClick(ListView list, View view, int position, long id) {
        super.onListItemClick(list, view, position, id);

        PublicKeyCredentialSource selectedItem = (PublicKeyCredentialSource) getListView().getItemAtPosition(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete WebAuthn Credentials");
        builder.setMessage("Do you want to delete WebAuthn Credentials \"" + selectedItem.getOtherUI() + "\"?");

        // Set up the buttons
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                frWebAuthn.deleteCredentials(selectedItem);
                reloadCredentials();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener( new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.GRAY);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.DKGRAY);
            }
        });

        dialog.show();
    }

    public void onDeleteAll(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete ALL WebAuthn Credentials?");
        builder.setMessage("Are you sure you want to delete all WebAuthn Credentials with RpID \"" + rpId + "\"?");

        // Set up the buttons
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                frWebAuthn.deleteCredentials(rpId);
                reloadCredentials();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener( new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.GRAY);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.DKGRAY);
            }
        });

        dialog.show();
    }

    public void onClickCancel(View view) {
        finish();
    }
}

class WebAuthnArrayAdapter extends ArrayAdapter<PublicKeyCredentialSource> {
    public WebAuthnArrayAdapter(Context context, ArrayList<PublicKeyCredentialSource> credentials) {
        super(context, 0, credentials);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        PublicKeyCredentialSource credential = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.web_authn_key_row, parent, false);
        }
        // Lookup view for data population
        TextView listText = (TextView) convertView.findViewById(R.id.listText);

        // Populate the data into the template view using the data object
        listText.setText(credential.getOtherUI());

        // Return the completed view to render on screen
        return convertView;
    }
}