/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Stores an arrangement of a crossword com.jot.game board.
 */

package com.jot.jot.android;

import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.jot.jot.R;
import com.jot.jot.crossword.*;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

//**************************************************************************************************
/*
public class SignInActivity
	extends MainActivity
	implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener
{
	private static final String TAG = "GoogleActivity";
	private static final int RC_SIGN_IN = 9001;

	private FirebaseAuth m_fbAuth;
	private FirebaseAuth.AuthStateListener m_fbAuthListener;

	private GoogleApiClient m_gApiClient;

	private Main m_main;

	SignInActivity(Main main)
	{
		m_main = main;
	}

	@Override
	protected void
	onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Configure Google Sign In
		GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build();

		m_gApiClient = new GoogleApiClient.Builder(this)
            .enableAutoManage(this, this) // (FragmentActivity, OnConnectionFailedListener)
            .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
            .build();

		m_fbAuth = FirebaseAuth.getInstance();

		m_fbAuthListener = new FirebaseAuth.AuthStateListener()
			{
				@Override
				public void
				onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth)
				{
					FirebaseUser user = firebaseAuth.getCurrentUser();
					if (user != null)
					{
						// User is signed in
						Intent intent = new Intent(SignInActivity.this, MainActivity.class);
						startActivity(intent);
					}
				}
			};
	}

	@Override
	public void
	onStart()
	{
		super.onStart();
		m_fbAuth.addAuthStateListener(m_fbAuthListener);
	}

	@Override
	public void
	onStop()
	{
		super.onStop();
		if (m_fbAuthListener != null)
		{
			m_fbAuth.removeAuthStateListener(m_fbAuthListener);
		}
	}

	@Override
	public void
	onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		super.onActivityResult(requestCode, resultCode, intent);

		// Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
		if (requestCode == RC_SIGN_IN)
		{
			GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(intent);
			if (result.isSuccess())
			{
				// Google Sign In was successful, authenticate with Firebase
				GoogleSignInAccount account = result.getSignInAccount();
				m_main.setUserLogin(account);
			}
        }
    }

	private void
	signIn()
	{
		Intent intent = Auth.GoogleSignInApi.getSignInIntent(m_gApiClient);
		startActivityForResult(intent, RC_SIGN_IN);
	}

	private void
	revokeAccess()
	{
		// Firebase sign out
		m_fbAuth.signOut();

		// Google revoke access
		Auth.GoogleSignInApi.revokeAccess(m_gApiClient).setResultCallback
		(
            new ResultCallback<Status>()
			{
                @Override
                public void
				onResult(@NonNull Status status)
				{
                }
			}
		);
	}

	@Override
	public void
	onConnectionFailed(@NonNull ConnectionResult connectionResult)
	{
		// An unresolvable error has occurred and Google APIs (including Sign-In) will not
		// be available.
		setAlert("onConnectionFailed:" + connectionResult);
	}

	@Override
	public void
	onClick(View v)
	{
		signIn();
	}
}
*/
