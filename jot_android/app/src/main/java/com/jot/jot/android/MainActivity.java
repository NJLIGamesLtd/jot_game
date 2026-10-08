/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Stores an arrangement of a crossword com.jot.game board.
 */

package com.jot.jot.android;

import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.snackbar.Snackbar;
import com.jot.jot.R;
import com.jot.decomp.TreeWords;
import com.jot.jot.crossword.Main;
import com.jot.jot.db.*;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.google.firebase.FirebaseApp;

import java.io.InputStream;

//**************************************************************************************************

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "MainActivity";
	private static final int RC_SIGN_IN = 9001;

	private Main m_main;
	private MainView m_mainView;
	private ToneGenerator m_toneGen;
	private NotificationChannel m_noteChannel = null;
	private NotificationManager m_noteManager = null;
	private NotificationCompat.Builder m_noteBuilder;
	private CallbackManager m_callbackManager = null;
	private int m_idNotification = 0;
	private int m_nWidth;
	private int m_nHeight;
	private float m_dDensity;
	private float m_dFontMultiplier;
	private float m_dScale;
	
	private int m_appWidgetId;

	private boolean m_bInThread = false;
	
	//**********************************************************************************************
	public void
	exit()
	{
		finish();
		System.exit(0);
	}

	//**********************************************************************************************
	public CallbackManager
	getCallbackManager()
	{
		return m_callbackManager;
	}

	//**********************************************************************************************
	public float
	getDensity()
	{
		return m_dDensity;
	}

	//**********************************************************************************************
	public int
	getWidth()
	{
		return m_nWidth;
	}

	//**********************************************************************************************
	public int
	getHeight()
	{
		return m_nHeight;
	}

	//**********************************************************************************************
	public float
	getScale()
	{
		return m_dScale;
	}

	//**********************************************************************************************
	public void
	setHeight(int nHeight)
	{
		m_nHeight = nHeight;
	}

	//**********************************************************************************************
	public float
	getFontMultiplier()
	{
		return m_dFontMultiplier;
	}

	//**********************************************************************************************
    public String
	getPlayerName()
	{
		return m_mainView.getPlayerName();
	}

	//**********************************************************************************************
	public boolean
	isInThread()
	{
		return m_bInThread;
	}

	//**********************************************************************************************
	public void
	setInThread(boolean bInThread)
	{
		m_bInThread = bInThread;
	}

	//**********************************************************************************************
	@Override
	public void
	onActivityResult(int requestCode, int resultCode, Intent indent)
	{
		if (m_callbackManager != null)
		{
			super.onActivityResult(requestCode, resultCode, indent);
			m_callbackManager.onActivityResult(requestCode, resultCode, indent);
		}
	}
				
	//**********************************************************************************************
    @Override
    protected void
	onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

		try
		{
			m_callbackManager = CallbackManager.Factory.create();

			// Get the size of the display.
			WindowManager windowManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
			DisplayMetrics displayMetrics = new DisplayMetrics();
			windowManager.getDefaultDisplay().getMetrics(displayMetrics);
			if ((m_dDensity = displayMetrics.density) == 0.0)
			{
				m_dDensity = (float)1.0;
			}
			m_dScale = displayMetrics.scaledDensity;
			m_nWidth = displayMetrics.widthPixels;
			if ((m_nHeight = displayMetrics.heightPixels) < m_nWidth)
			{
				m_nHeight = m_nWidth;
				m_nWidth = displayMetrics.heightPixels;
			}
			m_dFontMultiplier = 1;
			if (displayMetrics.ydpi != 0.0)
			{
				if ((m_dFontMultiplier = (float)(m_nHeight / displayMetrics.ydpi / 4.1)) > 1.5)
				{
					m_dFontMultiplier = (float)1.5;
				}
			}

			// Initialize Firebase
			FirebaseApp.initializeApp(this);

			// Initialize access to the database.
			DbAccess.Initialize(this);

			// Create the word list and game objects.
			InputStream crosswords = getResources().openRawResource(R.raw.crosswords);
			TreeWords words = new TreeWords(crosswords);
			m_main = new Main(words, this);

			// FacebookSdk.sdkInitialize(getApplicationContext());

			// Needed to play tones during updates.
			m_toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
		
			// Need the full display.  No generic title.
			requestWindowFeature(Window.FEATURE_NO_TITLE);

			// Initialize access to advertisements.
			MobileAds.initialize(
				this,
				new OnInitializationCompleteListener()
				{
					@Override
					public void
					onInitializationComplete(InitializationStatus initializationStatus)
					{
					}
				}
			);

			// Create the main view and have this control the display.
			m_mainView = new MainView(m_main, this, m_callbackManager);

			// Open a channel for notifications.
			createNotificationChannel();
			clearNotifications();

			// Wait a second to provide time for login.
			Thread.sleep(1000);

			// Update the display.
			setContentView(m_mainView);
		}
		catch (Exception e)
		{
			failMessage("MainActivity: Unable to create objects: " + e.getMessage());
		}
	}

	//**********************************************************************************************
    @Override
    protected void
	onResume()
	{
        super.onResume();
		clearNotifications();
	}

	//**********************************************************************************************
    public void
	setAction(Main.Action eAction)
	{
		m_mainView.setAction(eAction);
	}

	//**********************************************************************************************
    public void
	setAlert(String strAlert)
	{
		if (m_mainView != null)
		{
			m_mainView.setAlert(strAlert);
		}
	}

	//**********************************************************************************************
    public void
	setPlayerName(String strName)
	{
		m_mainView.setPlayerName(strName);
	}

	//**********************************************************************************************
    public void
	clearNotifications()
	{
		if (m_noteManager != null)
		{
			m_noteManager.cancelAll();
		}
	}

	//**********************************************************************************************
    public void
	setNotification(String strNote)
	{
		if (m_noteManager != null)
		{
			m_noteBuilder.setContentText(strNote);
			m_noteManager.notify(++m_idNotification, m_noteBuilder.build());
			MediaPlayer mp = MediaPlayer.create(this, R.raw.jot_notification);
			mp.start();
		}
	}

	//**********************************************************************************************
	private void
	createNotificationChannel()
	{
		Intent intent = new Intent(this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

		Uri path = Uri.parse("android.resource://com.jot.jot.android/" + R.raw.jot_notification);

		m_noteBuilder = new NotificationCompat
			.Builder(this, getString(R.string.default_notification_channel_id))
			.setSmallIcon(R.drawable.app_icon) // notification icon
			.setContentTitle("Jot") // title for notification
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setContentIntent(pi)
			.setAutoCancel(true)
			.setSound(path);

		// Create the NotificationChannel, but only on API 26+ because
		// the NotificationChannel class is new and not in the support library
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			m_noteChannel = new NotificationChannel(
				getString(R.string.default_notification_channel_id),
				getString(R.string.default_notification_channel_name),
				NotificationManager.IMPORTANCE_DEFAULT);

			AudioAttributes audio = new AudioAttributes
				.Builder()
				.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
				.setUsage(AudioAttributes.USAGE_NOTIFICATION)
				.build();
			m_noteChannel.setSound(path, audio);
			
			//m_noteChannel.setDescription(getString(R.string.channel_description));

			// Register the channel with the system; you can't change the importance
			// or other notification behaviors after this
			m_noteManager = getSystemService(NotificationManager.class);
			m_noteManager.createNotificationChannel(m_noteChannel);
		}
		else
		{
			//m_noteManager = (NotificationManager) NotificationManagerCompat.from(this);
		}
	}

	//**********************************************************************************************
    public void
	failMessage(String strMessage)
	{
		//SPK - check for DEBUG mode: System.err.printf(strMessage);
		Snackbar.make(m_mainView, strMessage, Snackbar.LENGTH_LONG).show();
	}

	//**********************************************************************************************
    public void
	playTone()
	{
		// Output a tone for each update.
		m_toneGen.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
	}

	//**********************************************************************************************
    public void
	updateState(boolean bAnimate)
	{
		// Only allow one update to trigger at any given time.
		if (!m_mainView.prepareUpdate())
		{
			return;
		}

		// Update the underlying code.
		m_main.updateState();
		try
		{
			// If needed, run an animation of the played turn.
			if (bAnimate)
			{
				// Run an animation of the remaining update.
				Handler handler = new Handler(Looper.getMainLooper());
				handler.post
				(
					() ->
					{
						// Update the our view of the game to be in sync with the game.
						m_mainView.animateUpdate();
					}
				);
			}
			else
			{
				// Run a quick update.
				while (m_mainView.updateView())
				{
					Thread.sleep(1);
				}
				m_mainView.resetUpdate();
			}
		}
		catch (Exception e)
		{
			failMessage("MainActivity.updateState() failed: " + e.getMessage());
		}
	}
}

//**************************************************************************************************
