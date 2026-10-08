//**************************************************************************************************
//
//  MainView.swift
//  jot
//
//  Created by Stephen Kennedy on 4/18/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//  Main view controlling all the games.
//
//**************************************************************************************************

import Foundation
import UIKit

//**************************************************************************************************

class MainView: UIView
{
	let s_strAbout =
		"Jot (ios.0.01) uses Google Firebase to\n" +
		"store and manage user account data.\n" +
		"Email is used for identification, but\n" +
		"only nickname, high score and ranking\n" +
		"is visible to other players.\n" +
		"Jot does not share user data.\n"

	let s_mActionButtonResources: [Main.Action: UIImage] =
	[
		Main.Action.ABOUT: UIImage(named: "button_about.png")!,
		Main.Action.CANCEL: UIImage(named: "button_cancel.png")!,
		Main.Action.CONFIRM: UIImage(named: "button_confirm.png")!,
		Main.Action.DISPLAY_BEST: UIImage(named: "button_best.png")!,
		Main.Action.DRAW_BLACK_TILE: UIImage(named: "button_black_consonents.png")!,
		Main.Action.DRAW_BLACK_VOWEL: UIImage(named: "button_black_vowels.png")!,
		Main.Action.DRAW_WHITE_TILE: UIImage(named: "button_white_consonents.png")!,
		Main.Action.DRAW_WHITE_VOWEL: UIImage(named: "button_white_vowels.png")!,
		Main.Action.JUMBLE_RACK: UIImage(named: "button_jumble.png")!,
		Main.Action.PLAY_GAME: UIImage(named: "button_play.png")!,
		Main.Action.PLAY_WORD: UIImage(named: "button_play.png")!,
		Main.Action.PRIVACY: UIImage(named: "button_privacy.png")!,
		Main.Action.QUIT_GAME: UIImage(named: "button_quit.png")!,
		Main.Action.RACK_TILES: UIImage(named: "button_rack.png")!,
		Main.Action.ROTATE_BOARD: UIImage(named: "button_rotate.png")!,
		Main.Action.SELECT: UIImage(named: "button_play.png")!,
		Main.Action.SKIP_TURN: UIImage(named: "button_skip.png")!,
		Main.Action.SWITCH_GAME: UIImage(named: "button_switch.png")!,
		Main.Action.TERMS: UIImage(named: "button_terms.png")!,
		Main.Action.UNZOOM_BOARD: UIImage(named: "button_unzoom.png")!,
		Main.Action.USER_BLOCK: UIImage(named: "button_block.png")!,
		Main.Action.USER_DELETE: UIImage(named: "button_delete_account.png")!,
		Main.Action.USER_LOGIN: UIImage(named: "button_login.png")!,
		Main.Action.USER_REGISTER: UIImage(named: "button_register.png")!,
		Main.Action.USER_RESET: UIImage(named: "button_reset_password.png")!
	]

	let s_mStateHelpResources: [Main.State: UIImage] =
	[
		Main.State.GAME_LOAD: UIImage(named: "welcome.png")!,
		Main.State.GAME_LOAD_ROBOT: UIImage(named: "welcome.png")!,
		Main.State.GAME_LOAD_ROBOT_WAITING: UIImage(named: "welcome.png")!,
		Main.State.GAME_LOAD_PLAYER1: UIImage(named: "welcome.png")!,
		Main.State.GAME_LOAD_PLAYER1_WAITING: UIImage(named: "welcome.png")!,
		Main.State.GAME_LOAD_PLAYER2: UIImage(named: "welcome.png")!,
		Main.State.GAME_LOAD_PLAYER2_WAITING: UIImage(named: "welcome.png")!,
		Main.State.TURN_DRAW_FIRST: UIImage(named: "help_pick_tiles.png")!,
		Main.State.TURN_START: UIImage(named: "help_turn_start.png")!,
		Main.State.USER_DELETE: UIImage(named: "user_delete.png")!,
		Main.State.USER_MESSAGE: UIImage(named: "welcome.png")!,
		Main.State.USER_WELCOME: UIImage(named: "welcome.png")!,
		Main.State.USER_WELCOME_WAITING: UIImage(named: "welcome.png")!
	]

	var m_main: Main? = nil
	var m_deleteView: DeleteView? = nil
	var m_emailView: emailView? = nil
	var m_gameView: GameView? = nil
	var m_inviteView: InviteView? = nil
	var m_loginView: LoginView? = nil
	var m_setupView: SetupView? = nil
//TODO:    var m_adView: AdView? = nil

    var m_eState: Main.State = .OFF

	var m_butInfo: UIButton? = nil
	var m_tvStatus: UILabel? = nil

	var m_alertView: AlertView? = nil

	var m_bPopup = false
	var m_bQwerty = false
	var m_bUpdating = false
	
	var m_nInfoWidth = 0
	var m_xInfo = 0
	var m_yInfo = 0

	var m_ySelection = 0

	var m_mButtons: [Main.Action: UIButton] = [:]
	var m_mHelpViews: [Main.State: UIView] = [:]

	var m_nButtons = 0			// This is the number of active buttons.
	var m_nWidthButton = 0
	var m_nHeightButton = 0
	var m_nHeightScreen = 0

	var m_progressDialog: UIActivityIndicatorView? = nil

	var m_nDelay = 0
	var m_dFontMultiplier = 0.0

	var m_fbLogin: LoginButton? = nil
	var m_gLogin: SignInButton? = nil
	
	var m_shareLink: ShareLinkContent? = nil
	var m_fbShare: ShareButton? = nil

	//**********************************************************************************************
	init(main: Main) throws
	{
		m_main = main

		// Calculate the board size based on the screen size and board layout.
		let width = Int(UIScreen.main.bounds.width)
		let height = Int(UIScreen.main.bounds.height)
        main.setWidth(width: width)
        main.setHeight(height: height)

		m_yInfo = 23 * height / 31
		m_yAd = 28 * height / 31
        
        super.init(frame: CGRect(x: 0, y: 0, width: width, height: height))

        backgroundColor = UIColor(patternImage: UIImage(named: "jot_background_board.png")!)

		// Show progress while connecting or transfering.
		m_progressDialog = UIActivityIndicatorView()

		// Create a view of what will be the active game.  Need default sizes for all
		// games to match to.
		try m_gameView = GameView(
			main: main,
			nPlayers: main.getPlayerCount(),
			nBoardWidth: main.getBoardWidth(),
			nBoardHeight: main.getBoardHeight(),
			nRackSize: main.getRackSize())

		// Set up the initial game.
		try m_gameView!.setGame(game: m_main!.getGame(), iPlayer: m_main!.getPlayer())

		// Get the font multiplier
		m_dFontMultiplier = main.getFontMultiplier()

		// Create the game-over screen.
		m_butInfo = UIButton(type: .system)
		m_butInfo!.frame = CGRect(
			x: m_xInfo,
			y: 3 * m_xInfo,
			width: m_xInfo + m_nInfoWidth,
			height: 3 * m_xInfo + m_nInfoWidth)
        m_butInfo!.backgroundColor = UIColor(red: 241, green: 181, blue: 21, alpha: 100)
        m_butInfo!.addTarget(self, action: #selector(gameOverAction), for: .touchUpInside)

		// Create the status message region.
		m_tvStatus = UILabel()

/* TODO:
		// Create a banner ad for the bottom of the screen.
		m_adView = new AdView(mainActivity);
		m_adView.setAdSize(AdSize.SMART_BANNER);
		m_adView.setAdUnitId("ca-app-pub-1246465467159410/6901525474");
		AdRequest adRequest = new AdRequest
			.Builder()
			.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
			.build();
		m_adView.loadAd(adRequest);

		m_adView.setAdListener(
			new AdListener()
			{
				@Override
				public void
				onAdLoaded()
				{
					// Code to be executed when an ad finishes loading.
				}

				@Override
				public void
				onAdOpened()
				{
					// Code to be executed when an ad opens an overlay that
					// covers the screen.
				}

				@Override
				public void
				onAdClicked()
				{
					// Code to be executed when the user clicks on an ad.
				}

				@Override
				public void
				onAdLeftApplication()
				{
					// Code to be executed when the user has left the app.
				}

				@Override
				public void
				onAdClosed()
				{
					// Code to be executed when the user is about to return
					// to the app after tapping on an ad.
				}
			}
		);
		addView(m_adView);
*/

		// Create the alert message region.
		m_alertView = AlertView(main: main)

		// Create the list of button images.
		createButtons()

		m_fbLogin = LoginButton(Main: main)
		m_fbLogin.setReadPermissions("email")

		m_gLogin = SignInButton(Main: main)
		m_gLogin.setReadPermissions("email")
		
		// Create a sharable link for people to tell their friends.
		m_shareLink = new ShareLinkContent
			.Builder()
			.setContentUrl(Uri.parse("https://play.google.com/store/apps/details?id=com.jot.jot"))
			.setShareHashtag(
				new ShareHashtag.Builder()
				.setHashtag("#TheChessOfCrosswords")
				.build())
			.build();
		m_fbShare = new ShareButton(mainActivity);
		m_fbShare.setShareContent(m_shareLink);

		// Create the help views.
		createHelpViews()

		// Need a screen to delete the user from the database.
		m_deleteView = DeleteView(main: main)

		// Need a login screen to connect user to the database.
		m_loginView = LoginView(main: main)
		m_emailView = EmailView(main: main)
		m_inviteView = InviteView(main: main)

		// Need a user setup screen to update the database.
		m_setupView = SetupView(main: main)
		m_bPopup = false

		// Remember the changing state of the qwerty keyboard for blank tiles.
		m_bQwerty = false

		// Introduce the welcome state.
		m_eState = Main.State.OFF
		m_nDelay = 0
		try _ = updateView()

		addSubview(m_gameView!)
		addSubview(m_tvStatus!)
	}

	//**********************************************************************************************
	required
	init?(coder aDecoder: NSCoder)
	{
		super.init(coder: aDecoder)
	}
    
	//**********************************************************************************************
	func
	animateUpdate()
	{
        Thread.detachNewThread
		{
			while (true)
			{
                do
                {
                    // Update our score while the robot thinks
                    if (try !self.updateView())
                    {
                        // Wait for the robot to complete.
                        if (!self.m_main!.isWorking())
                        {
                            // Update the score for both players.
                            if (try !self.updateView())
                            {
								cancel()
                                self.resetUpdate()
                                return
                            }
                        }
                    }
                }
                catch
                {
                }
				Thread.sleep(forTimeInterval: 50)
			}
		}
	}

	//**********************************************************************************************
	func
	buttonAction(sender: UIButton!)
	{
		do
		{
/* TODO:
			Intent intent;
			switch (eAction1)
			{
				case PRIVACY:
					intent = new Intent(
						"android.intent.action.VIEW",
						Uri.parse("https://jot.flycricket.io/privacy.html"));
					m_mainActivity.startActivity(intent);
					return;
					
				case TERMS:
					intent = new Intent(
						"android.intent.action.VIEW",
						Uri.parse("https://jot.flycricket.io/terms.html"));
					m_mainActivity.startActivity(intent);
					return;
					
				default:
					break;
			}
*/
            let eAction = Main.Action(rawValue: sender.tag)
			m_main!.setAction(
				eAction: eAction!,
                mData: getActionData(eAction: eAction!))

			// Force a full update the first time through.
			_ = prepareUpdate()

			// Update the our view of the game to be in sync with the game.
			animateUpdate()
		}
		catch
		{
            displayMessage(strMessage: "createButton() error: \(error)")
		}
	}

	//**********************************************************************************************
	func
	createButton(eAction: Main.Action) -> UIButton
	{
		let button = UIButton(type: .system)
		button.frame = CGRect(x: 0, y: 0, width: m_nWidthButton, height: m_nHeightButton)
        button.backgroundColor = UIColor(patternImage: s_mActionButtonResources[eAction]!)
        button.tag = eAction.rawValue

		return button
	}

	//**********************************************************************************************
	func
	createButtons()
	{
		for entry in s_mActionButtonResources
		{
            m_mButtons[entry.key] = createButton(eAction: entry.key)
		}
	}

	//**********************************************************************************************
	func
	createHelpViews()
	{
		for entry in s_mStateHelpResources
		{
			let view = UIView()
    		view.backgroundColor = UIColor(patternImage: entry.value)
			m_mHelpViews[entry.key] = view
		}
	}

	//**********************************************************************************************
	func
	displayMessage(strMessage: String)
	{
        m_main!.failMessage(strMessage: strMessage)
	}

	//**********************************************************************************************
	@objc func
	gameOverAction(sender: UIButton!)
	{
		m_butInfo!.removeFromSuperview()
		m_main!.setAction(eAction: .DONE)
		animateUpdate()
	}
    
	//**********************************************************************************************
	func
	getActionData(eAction: Main.Action) -> [String: Any]?
	{
		var mData: [String: Any]? = nil

		switch (eAction)
		{
            case .USER_DELETE,
				 .USER_LOGIN,
                 .USER_REGISTER,
				 .USER_RESET:
				if (m_eState == Main.State.USER_LOGIN)
				{
					mData = m_emailView!.getUserLogin()
				}
				break

			case .CONFIRM:
				switch (m_eState)
				{
					case .GAME_INVITE:
						mData = m_inviteView.getInviteEmail()
						break

					case .USER_DELETE:
						mData = m_deleteView.getUserLogin()
						break

					case .USER_SETUP:
						mData = m_setupView!.getUserSetup()
						break

					default:
						break
				}
				break
            
            default:
                break
		}

		return mData
	}

	//**********************************************************************************************
    func
	getPlayerName() -> String
	{
		return m_main!.getUserName()
	}

	//**********************************************************************************************
	func
	initDisplayImages(
		nPixelWidth: Int,
		nPixelHeight: Int)
	{
		m_nWidthButton = nPixelWidth / 6
		m_nHeightButton = nPixelHeight / 14

		m_nInfoWidth = 5 * nPixelWidth / 6
		m_xInfo = (nPixelWidth - m_nInfoWidth) >> 1
	}

	//**********************************************************************************************
	func
	prepareUpdate() -> Bool
	{
		if (m_bUpdating)
		{
			return false
		}
		m_bUpdating = true
		return true
	}

	//**********************************************************************************************
	func
	removeButtons()
	{
		// Remove all buttons.
		var iChild = subviews.count
		while (iChild > 0)
		{
			iChild -= 1
			if let child = subviews[iChild] as? UIButton
			{
				if (child != m_butInfo)
				{
					child.removeFromSuperview()
				}
			}
		}
	}

	//**********************************************************************************************
	func
	removeViews()
	{
		// Remove all buttons.
		var iChild = subviews.count
		while (iChild > 0)
		{
			iChild -= 1
			let child = subviews[iChild]
			if (child === m_butInfo)
			{
				child.removeFromSuperview();
				continue;
			}

			if let label = child as? UILabel
			{
				if (label != m_tvStatus)
				{
					label.removeFromSuperview();
				}
				continue;
			}

			if let login = child as? LoginView
			{
				login.removeFromSuperview();
				continue;
			}

			if let message = child as? MessageView
			{
				message.removeFromSuperview();
                continue
			}
		}
	}

	//**********************************************************************************************
	func
	resetUpdate()
	{
		m_bUpdating = false
	}

	//**********************************************************************************************
    func
	setAlert(strAlert: String)
	{
        m_alertView!.setAlert(strAlert: strAlert)
	}

	// *********************************************************************************************
    func
	setMainView()
	{
        m_main!.setMainView(mainView: self)
	}
	
	//**********************************************************************************************
    func
	setPlayerName(strName: String)
	{
		m_gameView.setPlayerName(iPlayer: m_main!.getPlayer(), strPlayerName: strName)
	}

	//**********************************************************************************************
	// Assign a label based on the tile availability.
	func
	setTileButtonLabel()
	{
        m_gameView!.setTileButtonLabel(button: m_mButtons[m_main!.getDrawTileAction()]!)
	}

	//**********************************************************************************************
	// Assign a label based on the tile availability.
	func
	setVowelButtonLabel()
	{
		m_gameView!.setVowelButtonLabel(button: m_mButtons[m_main!.getDrawVowelAction()]!)
	}

	//**********************************************************************************************
	func
	toString() -> String
	{
		return m_gameView!.toString()
	}

	//**********************************************************************************************
	func
	updateBackground()
	{
		switch (m_eState)
		{
			case .GAME_LOAD,
				 .GAME_LOAD_ROBOT,
				 .GAME_LOAD_ROBOT_WAITING,
				 .GAME_LOAD_PLAYER1,
				 .GAME_LOAD_PLAYER1_WAITING,
				 .GAME_LOAD_PLAYER2,
				 .GAME_LOAD_PLAYER2_WAITING,
				 .USER_WELCOME,
				 .USER_WELCOME_WAITING:
    			backgroundColor =
					UIColor(patternImage: UIImage(named: "jot_background_empty.png")!)
				break
            case .GAME_SELECT_WAITING:
    			backgroundColor =
					UIColor(patternImage: UIImage(named: "jot_background_select.png")!)
				break
            case .TURN_FIRST:
    			backgroundColor =
					UIColor(patternImage: UIImage(named: "jot_background_board_start.png")!)
				break
			default:
    			backgroundColor =
					UIColor(patternImage: UIImage(named: "jot_background_board.png")!)
				break
		}
	}

	//**********************************************************************************************
	func
	updateButtons(bLast: Bool)
	{
		// Remove the prior state's buttons.
		removeButtons()

        let nViews = subviews.count
		var nInvisible = 0

		// If the QWERTY keyboard is up, don't display buttons.
		if (!m_gameView!.isQwertyActive())
		{
			switch (m_eState)
			{
				case .GAME_INVITE:
					addSubview(m_fbShare)
					addSubview(m_mButtons[.CONFIRM]!)
					addSubview(m_mButtons[.CANCEL]!)
					break

				case .GAME_OVER,
					 .GAME_OVER_REPLAY:
					addSubview(m_mButtons[.SWITCH_GAME]!)
					addSubview(m_mButtons[.PLAY_AGAIN]!)
					addSubview(m_fbShare)
					break
					
				case GAME_OVER_ALERT:
				case GAME_OVER_MENU:
					addSubview(m_mButtons[.SWITCH_GAME]!)
					addSubview(m_fbShare)
					break
					
                case .GAME_OVER_QUIT:
					addSubview(m_mButtons[.CONFIRM]!)
					break
				
				case .GAME_QUIT,
					 .GAME_QUIT_ROBOT:
					addSubview(m_mButtons[.CANCEL]!)
					addSubview(m_mButtons[.CONFIRM]!)
					if (m_eState == Main.State.GAME_QUIT)
					{
						addSubview(m_mButtons[.USER_BLOCK]!)
					}
					break
					
                case .GAME_RESUME,
					 .GAME_START:
					addSubview(m_mButtons[.PLAY_GAME]!)
					break

				case .GAME_SELECT_WAITING:
					// Clear lock-screen notifications.
					m_main!.clearNotifications()
					
					var iGame = -1
					var strOpponent: String?
                    let width = m_main!.getWidth()
                    let height = m_main!.getHeight()
                    iGame += 1
                    while (m_main!.getGameOpponentName(iGame: iGame) != nil)
					{
                        strOpponent = m_main!.getGameOpponentName(iGame: iGame)
						let button = UIButton()
						nInvisible += 1

						button.frame = CGRect(
							x: (width - m_nInfoWidth) / 2,
							y: (height - m_nInfoWidth) / 2,
							width: m_nInfoWidth,
							height: m_nInfoWidth)

						if (iGame == 0 || m_main!.getGameTurnStatus(iGame))
						{
							button.backgroundColor = UIColor(red: 241, green: 181, blue: 21, alpha: 100)
						}
						else
						{
							button.backgroundColor = UIColor(red: 241, green: 21, blue: 181, alpha: 100)
						}
						
        				button.addTarget(self, action: #selector(gameSelectAction), for: .touchUpInside)
						button.tag = iGame
                        button.setTitle(strOpponent, for: .normal)
						addSubview(button)
                        iGame += 1
					}
					break

				case .TURN_DRAW,
					 .TURN_DRAW_FIRST:
					// Allow user to login during a robot game.
					if (!m_main!.haveUser())
					{
						addSubview(m_mButtons[.USER_LOGIN]!)
					}
					// fallthru

				case .TURN_WAITING:
					// Allow the user to return to the game selection screen.
					if (m_main!.haveUser())
					{
						addSubview(m_mButtons[.SWITCH_GAME]!)
					}

					// Disable most keys while the robot is working on a play.
					if (!m_main!.isRobotWorking())
					{
						// If we have a best word, have a key to display this.
						if (bLast && m_main!.haveBestWord())
						{
							addSubview(m_mButtons[.DISPLAY_BEST]!)
						}

						if (!m_gameView!.isRackFilled())
						{
							if (m_main!.haveTiles())
							{
								setTileButtonLabel()
								addSubview(m_mButtons[m_main!.getDrawTileAction()]!)
							}
							if (m_main!.haveVowels())
							{
								setVowelButtonLabel()
								addSubview(m_mButtons[m_main!.getDrawVowelAction()]!)
							}
						}
						else
						{
							addSubview(m_mButtons[.QUIT_GAME]!)
							addSubview(m_mButtons[.JUMBLE_RACK]!)
						}
					}
					break
				
				case .TURN_FIRST,
					 .TURN_INVALID,
					 .TURN_PLACE:
					// Disable most keys while the robot is working on a play.
					if (m_main!.isRobotWorking())
					{
						addSubview(m_mButtons[.JUMBLE_RACK]!)
						break
					}

					// Near the end of the game, the user might not be able to play a word.
					if (m_main!.isTilesLow())
					{
						addSubview(m_mButtons[.SKIP_TURN]!)
					}
					else if (m_main!.haveUser())
					{
						addSubview(m_mButtons[.SWITCH_GAME]!)
					}

					// Allow the user to quit the game.
					addSubview(m_mButtons[.QUIT_GAME]!)

					// Allow the board to be rotated and zoomed.
					if (!m_main!.isBoardEmpty())
					{
						if (m_gameView!.isBoardFocused())
						{
							addSubview(m_mButtons[.UNZOOM_BOARD]!)
						}
						else
						{
							addSubview(m_mButtons[.ROTATE_BOARD]!)
						}
					}

					// Allow the user to rack or jumble the tiles.
					if (m_main!.isTilePlaced())
					{
						addSubview(m_mButtons[.RACK_TILES]!)
					}
					else
					{
						addSubview(m_mButtons[.JUMBLE_RACK]!)
					}
					break
				
				case .TURN_PLAY:
					if (!m_main!.isBoardEmpty())
					{
						if (m_gameView!.isBoardFocused())
						{
							addSubview(m_mButtons[.UNZOOM_BOARD]!)
						}
						else
						{
							addSubview(m_mButtons[.ROTATE_BOARD]!)
						}
					}
					addSubview(m_mButtons[.RACK_TILES]!)
					if (m_main!.getRackTileCount() > 2)
					{
						addSubview(m_mButtons[.JUMBLE_RACK]!)
					}
					break
				
                case .TURN_READY:
					if (!m_main!.isBoardEmpty())
					{
						if (m_gameView!.isBoardFocused())
						{
							addSubview(m_mButtons[.UNZOOM_BOARD]!)
						}
						else
						{
							addSubview(m_mButtons[.ROTATE_BOARD]!)
						}
					}
					addSubview(m_mButtons[.RACK_TILES]!)
					if (m_main!.getRackTileCount() > 2)
					{
						addSubview(m_mButtons[.JUMBLE_RACK]!)
					}
					addSubview(m_mButtons[.PLAY_WORD]!)
					break
				
				case .TURN_ROBOT:
					break
				
				case .TURN_SKIP,
					 .USER_BLOCK,
					 .USER_MESSAGE:
					addSubview(m_mButtons[.CANCEL]!)
					addSubview(m_mButtons[.CONFIRM]!)
					break
				
				case .TURN_START:
					addSubview(m_mButtons[.PLAY_GAME]!)
					break

				case .TURN_VERIFY:
					addSubview(m_mButtons[.SWITCH_GAME]!)
					break

				case .USER_DELETE:
					addSubview(m_mButtons[.CANCEL]!)
					addSubview(m_mButtons[.CONFIRM]!)
					break

				case .USER_EMAIL:
					addSubview(m_mButtons[.USER_LOGIN]!)
					addSubview(m_mButtons[.USER_REGISTER]!)
					addSubview(m_mButtons[.USER_RESET]!)
					break

				case .USER_LOGIN:
					addSubview(m_mButtons[.PLAY_GAME]!)
					addSubview(m_fbLogin)
					addSubview(m_gLogin)
					if (!m_gLogin.isClickable())
					{
						nInvisible += 1
					}
					addSubview(m_mButtons[.USER_EMAIL]!)
					break

				case .USER_SETUP:
					if (m_main!.haveUser())
					{
						addSubview(m_mButtons[.CONFIRM]!)
						addSubview(m_mButtons[.CANCEL]!)
						addSubview(m_mButtons[.USER_DELETE]!)
					}
					addSubview(m_mButtons[.ABOUT]!)
					break

				case .USER_WELCOME,
					 .USER_WELCOME_WAITING:
					if (!m_main!.haveUser())
					{
						addSubview(m_mButtons[.USER_LOGIN]!)
					}
					addSubview(m_mButtons[.PLAY_GAME]!)
					addSubview(m_mButtons[.TERMS]!)
					addSubview(m_mButtons[.PRIVACY]!)
					break
                
                default:
                    break
			}
		}
		m_nButtons = subviews.count - nViews
	}

	//**********************************************************************************************
	@objc func
	gameSelectAction(sender: UIButton!)
	{
		do
		{
            m_main!.setGame(iGame: sender.tag)

			// Update the game view to be in sync with the game.
			animateUpdate()
		}
		catch
		{
            displayMessage(strMessage: "createButton() error: \(error)")
		}
	}

	//**********************************************************************************************
	func
	updateViews()
	{
		removeViews()
		m_bPopup = false

		switch (m_eState)
		{
			case ABOUT:
				m_butInfo!.setTitle(s_strAbout)
				addSubview(m_butInfo!)
				break;
				
			case .GAME_OVER
				 .GAME_OVER_ALERT:
				var strOver = m_main!.getOutcome()

				// Include the high scores.
				let nScoreHigh0 = m_main!.getHighScore(iPlayer: 0)
				let nScoreHigh1 = m_main!.getHighScore(iPlayer: 1)
				strOver.append(
					"\n\nHigh scores: " + String(nScoreHigh0) +
					" vs " + String(nScoreHigh1))

				// Include the averages.
				var nScoreAve0 = m_main!.getRanking(iPlayer: 0)
				var nScoreAve1 = m_main!.getRanking(iPlayer: 1)
				if (nScoreAve0 == 0)
				{
					nScoreAve0 = nScoreHigh0
				}
				if (nScoreAve1 == 0)
				{
					nScoreAve1 = nScoreHigh1
				}

				strOver.append(
					"\nAverage: " +  String(nScoreAve0) +
					" vs " + String(nScoreAve1))
					
                m_butInfo!.setTitle(strOver, for: .normal)
				addSubview(m_butInfo!)
				break

			case USER_DELETE:
				m_bPopup = true
				addSubview(m_deleteView!)
				break

			case USER_EMAIL:
				m_bPopup = true
				addSubview(m_emailView!)
				break

			case GAME_INVITE:
				m_bPopup = true
				addView(m_inviteView!)
				break

			case .USER_LOGIN:
				m_bPopup = true
				addSubview(m_loginView!)
				break

			case USER_SETUP:
				if (m_main!.haveUser())
				{
					m_bPopup = true
					m_setupView.setDetails(m_main)
					addSubview(m_setupView!)
				}
				break

			case .USER_REGISTER_WAITING:
                m_progressDialog!.accessibilityLabel = "Registering..."
				m_progressDialog?.startAnimating()
				break
            
			case .USER_WAITING:
                m_progressDialog?.accessibilityLabel = "Logging in..."
                m_progressDialog?.startAnimating()
				break

            default:
                break
		}
		
		if let helpView = m_mHelpViews[m_eState]
		{
			addSubview(helpView)
		}
		addSubview(m_alertView!)
	}

	//**********************************************************************************************
	func
	updateView() throws -> Bool
	{
		// Update the main state before updating its view.
		m_main!.updateState()

		// Make sure the player's turn is updated.
		if (m_main!.isWorking())
		{
			m_gameView!.updatePlayers()
		}

		// Make sure we are viewing the active game.
		if (m_main!.isGameSelected())
		{
			try m_gameView!.setGame(game: m_main!.getGame(), iPlayer: m_main!.getPlayer())
		}

		// Avoid updating our view if we're in the same state.
		let eState = m_main!.getState()

		if (eState == m_eState)
		{
			repeat
			{
				switch (m_eState)
				{
					case .GAME_SELECT_WAITING:  // SPK: do we need this?
						return false

					case .GAME_START:
						m_gameView!.reset()
						break
                    
                    default:
                        break
				}

				// If requested, make sure the board is not zoomed in.
				if (m_main!.isFocusBlocked())
				{
					if (m_gameView!.blockFocus())
					{
						break
					}
				}

				// If requested, rotate the view of the board.
                m_gameView!.setBoardRotation(bRotated: m_main!.isBoardRotated())

				// Set a delay after the best word board is displayed.
				if (m_eState == .TURN_BESTWORD)
				{
					if (m_gameView!.updateBest())
					{
						return true
					}
					if (m_nDelay == 0)
					{
						m_nDelay = 40
					}

					// If we have an animation, keep updating until our delay is zero.
                    m_nDelay -= 1
					if (m_nDelay == 0)
					{
						// Tell the main code that our animation delay is complete.
						m_main!.setAction(eAction: .DONE)
					}
					return true
				}

				// If the QWERTY keyboard state has changed, update buttons.
				if (m_bQwerty != m_gameView!.isQwertyActive())
				{
					m_bQwerty = m_gameView!.isQwertyActive()
					break
				}
				
				// Update the focus only when the robot is done.
				if (!m_main!.isWorking())
				{
					// If the board focus has changed, update the buttons.
					if (m_gameView!.updateFocus())
					{
						break
					}
				}

				// If our game view still requires sychronization, continue to update it.
				var bUpdate = false
				if (m_main!.isGameSelected())
				{
					bUpdate = m_gameView!.updateView()
				}

				// On the last update, update the buttons.
				if (!bUpdate)
				{
					updateButtons(bLast: true)
				}
				return bUpdate
			} while (false)
		}
		m_eState = eState

		// Cancel the progress indicator, if active.
        m_progressDialog?.stopAnimating()

		// Update the background image.
		updateBackground()

		// Update our buttons for this state.
		updateButtons(bLast: false)

		// Update our views for this new state.
		updateViews()

		// Return true to force another update.
		return true
	}
}

//**************************************************************************************************
