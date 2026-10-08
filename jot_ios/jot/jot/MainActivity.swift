// *************************************************************************************************
//
//  MainActivity.swift
//  jot
//
//  Created by Stephen Kennedy on 4/18/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
// *************************************************************************************************

import Foundation
import Firebase
import UIKit

// *************************************************************************************************

class MainActivity: UIViewController //, UIDropInteractionDelegate, UIDragInteractionDelegate
{
	let TAG = "MainActivity"

	var m_mainView: MainView? = nil
	var m_main: Main? = nil
	//    var m_adView: AdView
	//	  var m_toneGen: ToneGenerator
	//	  var m_noteChannel: NotificationChannel? = nil
	//	  var m_noteManager: NotificationManager? = nil
	//	  var m_noteBuilder: NotificationCompat.Builder
	//	  var m_coder: NSCoder
	var m_idNotification = 0
	var m_appWidgetId = 0

	// *********************************************************************************************
	init()
	{
		super.init(nibName: nil, bundle: nil)
		
        do
        {
            // Initialize Firebase
            //var app: FirebaseApp = FirebaseApp.app()

            // Initialize access to the database.
            DbAccess.Initialize(main: nil)

            // Create the word list and game objects.
            if let filePath = Bundle.main.url(forResource: "crosswords", withExtension: "jot")
            {
                let fHandle = try FileHandle(forReadingFrom: filePath)
                let words = try TreeWords(fHandle: fHandle, iOffset: 0)
                m_main = Main(words: words)

                // Create the main view and have this control the display.
                m_mainView = try MainView(main: m_main!)
				m_mainView!.setMainView()
            }
		
            // Need the full display.  No generic title.
            //requestWindowFeature(Window.FEATURE_NO_TITLE)

            // Open a channel for notifications.
            //createNotificationChannel()
        }
        catch
        {
        }
	}

	// *********************************************************************************************
	required
	init?(coder: NSCoder)
	{
		fatalError("init(coder:) has not been implemented")
	}
	
	// *********************************************************************************************
	//func
	//getCoder() -> NSCoder
	//{
	//	return m_coder
	//}

	// *********************************************************************************************
	override func
	viewDidLoad()
	{
        super.viewDidLoad()
	}

	// *********************************************************************************************
	override func
	loadView()
	{
		do
		{
			// Needed to play tones during updates.
			//m_toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

			// Initialize access to advertisements.
			//MobileAds.initialize(
			//	self,
			//	OnInitializationCompleteListener()
			//	{
			//		func
			//		onInitializationComplete(initializationStatus: InitializationStatus)
			//		{
			//		}
			//	}
			//)
			//m_adView = findViewById(R.id.adView)
			//var adRequest: AdRequest = AdRequest.Builder().build()
			//m_adView.loadAd(adRequest)

			//m_adView.setAdListener(
			//	AdListener()
			//	{
			//		func
			//		onAdLoaded()
			//		{
			//			// Code to be executed when an ad finishes loading.
			//		}

			//		func
			//		onAdFailedToLoad(errorCode: Int)
			//		{
			//			// Code to be executed when an ad request fails.
			//		}

			//		func
			//		onAdOpened()
			//		{
			//			// Code to be executed when an ad opens an overlay that
			//			// covers the screen.
			//		}

			//		func
			//		onAdClicked()
			//		{
			//			// Code to be executed when the user clicks on an ad.
			//		}

			//		func
			//		onAdLeftApplication()
			//		{
			//			// Code to be executed when the user has left the app.
			//		}

			//		func
			//		onAdClosed()
			//		{
			//			// Code to be executed when the user is about to return
			//			// to the app after tapping on an ad.
			//		}
			//	}
			//)

			// Wait a second to provide time for login.
            Thread.sleep(forTimeInterval: 1000)

			// Update the display.
			//setContentView(m_mainView)
		}
/*
		catch
		{
            //failMessage(strMessage: "MainActivity: Unable to create objects: \(error)")
		}
*/
	}

	// *********************************************************************************************
    func
	setNotification(strNote: String)
	{
		//m_noteBuilder.setContentText(strNote)
        //m_idNotification += 1
		//m_noteManager.notify(m_idNotification, m_noteBuilder.build())
	}

	// *********************************************************************************************
	func
	createNotificationChannel()
	{
		//var intent: Intent = Intent(self, MainActivity.class)
		//intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
		//var pi: PendingIntent = PendingIntent.getActivity(self, 0, intent, 0)

		//m_noteBuilder = NotificationCompat
		//	.Builder(self, getString(R.string.default_notification_channel_id))
		//		.setSmallIcon(R.drawable.app_icon) // notification icon
		//		.setContentTitle("Jot") // title for notification
		//		.setPriority(NotificationCompat.PRIORITY_DEFAULT)
		//		.setContentIntent(pi)
		//		.setAutoCancel(true)

		// Create the NotificationChannel, but only on API 26+ because
		// the NotificationChannel class is and not in the support library
		//if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		//{
		//	m_noteChannel = NotificationChannel(
		//		getString(R.string.default_notification_channel_id),
		//		getString(R.string.default_notification_channel_name),
		//		NotificationManager.IMPORTANCE_DEFAULT)
		//	//m_noteChannel.setDescription(getString(R.string.channel_description))

		//	// Register the channel with the system you can't change the importance
		//	// or other notification behaviors after this
		//	m_noteManager = getSystemService(NotificationManager.class)
		//	m_noteManager.createNotificationChannel(m_noteChannel)
		//}
		//else
		//{
		//	//m_noteManager = (NotificationManager) NotificationManagerCompat.from(self)
		//}
	}
}

// *************************************************************************************************
