//**************************************************************************************************
//
//  Main.swift
//  jot
//
//  Created by Stephen Kennedy on 5/2/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//**************************************************************************************************

import Foundation

//**************************************************************************************************
//
// Order of game states
//
// OFF
// USER_WELCOME - Waits for PLAY, LOGIN or AUTO
//   USER_LOGIN
//     USER_LOGIN_WAITING
//       GAME_LOAD[_PLAYER]
//         GAME_LOAD[_PLAYER]_WAITING
//           USER_WELCOME_WAITING
//             GAME_SELECT
//   USER_REGISTER
//     USER_REGISTER_WAITING
//       GAME_LOAD[_PLAYER]
//         GAME_LOAD[_PLAYER]_WAITING
//           USER_WELCOME_WAITING
//             GAME_SELECT
//  GAME_LOAD[_PLAYER]
//    GAME_LOAD[_PLAYER]_WAITING
//      USER_WELCOME_WAITING
//        GAME_SELECT
//   PLAY no user
//     GAME_START
//
// GAME_SELECT
//   GAME_SELECT_WAITING
//     GAME_SELECTED
//       GAME_CREATE
//         GAME_SELECTED
//       GAME_RESUME
//
//**************************************************************************************************

class Main
{
	enum State
	{
		case OFF
		case ABOUT
		case GAME_CREATE
		case GAME_CREATE_EMAIL
		case GAME_CREATE_WAITING
		case GAME_INVITE
		case GAME_INVITE_WAITING
		case GAME_LOAD
		case GAME_LOAD_GAME
		case GAME_LOAD_GAME_WAITING
		case GAME_LOAD_ROBOT
		case GAME_LOAD_ROBOT_WAITING
		case GAME_LOAD_PLAYER1
		case GAME_LOAD_PLAYER1_WAITING
		case GAME_LOAD_PLAYER2
		case GAME_LOAD_PLAYER2_WAITING
		case GAME_OVER
		case GAME_OVER_ALERT
		case GAME_OVER_MENU
		case GAME_OVER_QUIT
		case GAME_OVER_REPLAY
		case GAME_QUIT
		case GAME_QUIT_ROBOT
		case GAME_RELOAD
		case GAME_RELOAD_ROBOT
		case GAME_RELOAD_ROBOT_WAITING
		case GAME_RELOAD_PLAYER1
		case GAME_RELOAD_PLAYER1_WAITING
		case GAME_RELOAD_PLAYER2
		case GAME_RELOAD_PLAYER2_WAITING
		case GAME_RESUME
		case GAME_SELECT
		case GAME_SELECT_WAITING
		case GAME_SELECTED
		case GAME_START
		case TURN_BESTWORD
		case TURN_DRAW
		case TURN_DRAW_FIRST
		case TURN_FIRST
		case TURN_INVALID
		case TURN_PLACE
		case TURN_PLAY
		case TURN_READY
		case TURN_ROBOT
		case TURN_SKIP
		case TURN_START
		case TURN_VERIFY
		case TURN_WAITING
		case USER_BLOCK
		case USER_DELETE
		case USER_EMAIL
		case USER_LOGIN
		case USER_LOGIN_WAITING
		case USER_REGISTER_WAITING
		case USER_MESSAGE
		case USER_SETUP
		case USER_WAITING
		case USER_WELCOME
		case USER_WELCOME_WAITING
	}

	enum Setting: Int
	{
		case BOARD_ROTATED = 0
		case FOCUS_BLOCKED
		case GAME_TIMED
	}

	enum Action: Int
	{
		case ABOUT = 0
		case CANCEL
		case CONFIRM
		case DISPLAY_BEST
		case DONE
		case DRAW_BLACK_TILE
		case DRAW_BLACK_VOWEL
		case DRAW_WHITE_TILE
		case DRAW_WHITE_VOWEL
		case JUMBLE_RACK
		case PLAY_AGAIN
		case PLAY_GAME
		case PLAY_WORD
		case PRIVACY
		case QUIT_GAME
		case RACK_TILES
		case ROTATE_BOARD
		case SELECT
		case SKIP_TURN
		case SWITCH_GAME
		case TERMS
		case UNZOOM_BOARD
		case USER_BLOCK
		case USER_DELETE
		case USER_EMAIL
		case USER_LOGIN
		case USER_PRIVACY
		case USER_REGISTER
		case USER_RESET
		case USER_SETUP
	}

	static let s_mNext: [State: State] =
	[
		.GAME_CREATE: .GAME_CREATE_WAITING,
		.GAME_CREATE_WAITING: .GAME_SELECTED,
		.GAME_INVITE: .GAME_INVITE_WAITING,
		.GAME_INVITE_WAITING: .GAME_INVITE_WAITING,
		.GAME_LOAD: .GAME_LOAD_ROBOT,
		.GAME_LOAD_GAME: .GAME_LOAD_GAME_WAITING,
		.GAME_LOAD_GAME_WAITING: .GAME_SELECETED,
		.GAME_LOAD_ROBOT: .GAME_LOAD_ROBOT_WAITING,
		.GAME_LOAD_ROBOT_WAITING: .GAME_LOAD_PLAYER1,
		.GAME_LOAD_PLAYER1: .GAME_LOAD_PLAYER1_WAITING,
		.GAME_LOAD_PLAYER2: .GAME_LOAD_PLAYER2_WAITING,
		.GAME_LOAD_PLAYER1_WAITING: .GAME_LOAD_PLAYER2,
		.GAME_LOAD_PLAYER2_WAITING: .USER_WELCOME_WAITING,
		.GAME_RELOAD: .GAME_RELOAD_PLAYER1,
		.GAME_RELOAD_ROBOT: .GAME_RELOAD_ROBOT_WAITING,
		.GAME_RELOAD_ROBOT_WAITING: .GAME_RELOAD_PLAYER1,
		.GAME_RELOAD_PLAYER1: .GAME_RELOAD_PLAYER1_WAITING,
		.GAME_RELOAD_PLAYER2: .GAME_RELOAD_PLAYER2_WAITING,
		.GAME_RELOAD_PLAYER1_WAITING: .GAME_RELOAD_PLAYER2,
		.GAME_RELOAD_PLAYER2_WAITING: .GAME_SELECT,
		.GAME_SELECT: .GAME_SELECT_WAITING,
		.GAME_RESUME: .TURN_START,
		.GAME_START: .TURN_START,
		.TURN_ROBOT: .TURN_WAITING,
		.TURN_VERIFY: .TURN_START
	]

	var m_mainView: MainView? = nil

	var m_words: Words

	var m_iGame = 0
	var m_game: Game? = nil
	var m_llGames: [[Game]] = [[]]

	var m_iPlayer = 0

	var m_eState: State = .OFF
	var m_eStateParent: State = .OFF
	var m_bmSettings = 0

	var m_dbGame: DbGame? = nil
	var m_dbUser: DbUser? = nil

	var m_strEmail: String? = nil
	var m_strOpponentId: String? = nil
	var m_strStatus: String? = nil

	var m_bFlingMessage = false

	var m_width = 0
	var m_height = 0

	var m_strAlert = ""

	var m_bInThread = false

	var m_dFontMultiplier = 0.0
	
	//**********************************************************************************************
	init(words: Words)
	{
		m_words = words

		// Create a default game with the robot.
		m_iPlayer = 0

		// Have to wait for a game to be loaded before having reference to it.
		m_dbGame = nil

		// No special settings by default.
		m_bmSettings = 0
	}

	//**********************************************************************************************
	func
	addDbGame(dbGame: DbGame) -> Bool
	{
		let strOpponentId = dbGame.getOpponentId()
		let strGameId = dbGame.getGameId()
		var lGames: [Game]
		var iAdd = -1
		var iNext = 0

		// If we already have this game, treat it as an update.
		for iList in 0..<m_llGames.count
		{
			var bMyTurn = false
			lGames = m_llGames[iList]
			for iGame in 0..<lGames.count
			{
				let game = lGames[iGame]
				if let dbGame2 = game.getDbGame()
				{
					// If this is the same game, use the new DbGame object.
					if (strGameId == dbGame2.getGameId())
					{
						game.setDbGame(dbGame: dbGame)

						// If it's our turn, try to push it up on the list.
						if (dbGame.isMyTurn() && iNext > 0)
						{
							m_llGames[iList] = m_llGames[iNext]
							m_llGames[iNext] = lGames
						}
						
						return dbGame !== dbGame2
					}
					
					// If we already have a game list for this opponent, add the new game to it.
					if (dbGame2.getOpponentId() == strOpponentId)
					{
						iAdd = iList
					}
					
					if (dbGame2.isMyTurn())
					{
						bMyTurn = true
					}
				}
			}

			// If its the opponent's turn, allow another game list in front of this.
			if (iList > 0 && iNext == 0 && !bMyTurn)
			{
				iNext = iList
			}
		}

		// If we have the posted DbGame with the robot, assign it to our previously created game.
        var game: Game? = nil
		var bAddGame = false
		if (dbGame.isRobot())
		{
			lGames = m_llGames[0]
			game = lGames[0]
		}
		else
		{
			// Create a new two-player game.
			game = Jot(words: m_words, main: self, nPlayers: 2, iPlayerRobot: -1)
			bAddGame = true

			// If we need a new game list for this game, create it.
			if (iAdd < 0)
			{
				iAdd = m_llGames.count
				lGames = [Game]()
				m_llGames[iAdd] = lGames
			}
			else
			{
				lGames = m_llGames[iAdd]
			}

			// If it's our turn, try to move this up in the list.
			if (iNext > 0 && iNext < iAdd && dbGame.isMyTurn())
			{
				// If we just created this new game, have it selected.
				if (m_iGame == iAdd)
				{
					m_iGame = iNext
				}

				// Push the next game up front.
				m_llGames[iAdd] = m_llGames[iNext]
				m_llGames[iNext] = lGames
			}

			if (m_eState == .GAME_LOAD_GAME_WAITING)
			{
				m_iGame = iNext
			}
		}
        game!.setDbGame(dbGame: dbGame)
		game!.setSettings()

		// If needed, add this game to our list.
		if (bAddGame)
		{
			// If this game is over, store it first so the user can see the outcome before
			// it is deleted.
			var iGame = lGames.count
			if (dbGame.isMyTurn())
			{
				iGame = 0
                if (lGames.count > 0 && lGames[0].getDbGame()!.getPlayerTurn() < 0)
				{
					iGame += 1
				}
			}
			lGames[iGame] = game!
		}
		return true
	}

	//**********************************************************************************************
	func
	blockFocus()
	{
		m_bmSettings |= getBitMask(eSetting: .FOCUS_BLOCKED)
	}

	//**********************************************************************************************
	func
	checkUserInvite()
	{
		switch (m_dbUser!.getInviteState())
		{
			case .USER_SEARCHING:
				break

			case .USER_EMPTY:
				setStatus(strStatus: "Provide a friend's email")
				setState(eState: .GAME_INVITE)
				break

			case .USER_SAME:
				setStatus(strStatus: "** That's your email **")
				setState(eState: .GAME_INVITE)
				break

			case .USER_BLOCKED:
				setStatus(strStatus: "Person has blocked you")
				setState(eState: .GAME_SELECT)
				break

			case .USER_ACTIVE:
				for i = 1..<m_llGames.count
				{
					if let dbGame = m_llGames[i][0].getDbGame()
					{
						if (dbGame.isOpponent(strOpponentId: m_strNewOpponentId))
						{
							setGame(iGame: i)
							m_iGame = i
							setState(eState: .GAME_SELECTED)
							return
						}
					}
				}
				setStatus(strStatus: "Already have a game with them")
				setState(eState: .GAME_SELECT)
				break

			case .USER_INVITE:
				let intent = new Intent(Intent.ACTION_SEND)
				intent.setData(Uri.parse("mailto:"))
				intent.setType("text/plain")
				
				intent.putExtra(Intent.EXTRA_EMAIL, new String[]{ m_strEmail })
				intent.putExtra(
					Intent.EXTRA_SUBJECT,
					m_dbUser.getUserName() + " would like to play you in Jot.")
				intent.putExtra(
					Intent.EXTRA_TEXT,
					"Jot is the Chess of Crosswords, with your own bag of letter tiles that have " +
					"a different shape and selection of vowels and consonants.\n" +
					"Available now for Android via https://play.google.com/store/apps/details?id=com.jot.jot\n" +
					"Coming soon for iPhone.\n" +
					"Check out https://JotRussell.com for more info.\n")
				do
				{
					m_main.startActivity(Intent.createChooser(intent, "Send email..."))
					m_main.finish()
				}
				catch (ActivityNotFoundException e)
				{
					m_main.failMessage("Email failed: " + e)
				}
				setStatus(strStatus: "Invite has been sent")
				setState(eState: .GAME_SELECT)
				break

			case USER_FOUND:
				setStatus(strStatus: "Creating game...")
				setState(eState: .GAME_LOAD_GAME)
				break

			default:
				break
		}
	}

	//**********************************************************************************************
	func
	deleteGame()
	{
		if let game = m_game
		{
			// Always kill the robot game object.  Just reset it.
			if (game.isRobot())
			{
				game.reset()
			}
			else
			{
				// Delete this game.
				game.delete()

				// Delete the game from our list of lists.
				var bFound = false
				for iList in 0..<m_llGames.count
				{
					for iGame in 0..<m_llGames[iList].count
					{
						if (game === m_llGames[iList][iGame])
						{
							m_llGames[iList].remove(at: iGame)
							if (m_llGames[iList].count == 0)
							{
								m_llGames.remove(at: iList)
							}
							bFound = true
							break
						}
					}
					if (bFound)
					{
						break
					}
				}
				m_dbGame = nil
				m_game = nil
			}
		}
	}

	//**********************************************************************************************
	func
	getBitMask(eSetting: Setting) -> Int
	{
		return 1 << Int(eSetting.rawValue)
	}

	//**********************************************************************************************
	func
	getBoardHeight() -> Int
	{
		if let game = m_game
		{
			return game.getBoardHeight()
		}
		return 0
	}

	//**********************************************************************************************
	func
	getBoardWidth() -> Int
	{
		if let game = m_game
		{
			return game.getBoardWidth()
		}
		return 0
	}

	//**********************************************************************************************
	func
	getDrawTileAction() -> Action
	{
		if (m_iPlayer == 0)
		{
			return Action.DRAW_WHITE_TILE
		}
		return Action.DRAW_BLACK_TILE
	}

	//**********************************************************************************************
	func
	getDrawVowelAction() -> Action
	{
		if (m_iPlayer == 0)
		{
			return Action.DRAW_WHITE_VOWEL
		}
		return Action.DRAW_BLACK_VOWEL
	}

	//**********************************************************************************************
	func
	getFontMultiplier() -> Double
	{
		return m_dFontMultiplier
	}

	//**********************************************************************************************
	func
	getGame() -> Game?
	{
		return m_game
	}

	//**********************************************************************************************
	func
	getGameCount() -> Int
	{
		return m_llGames.count
	}
	
	//**********************************************************************************************
	func
	getGameOpponentName(iGame: Int) -> String?
	{
		if (iGame < m_llGames.count)
		{
			return m_llGames[iGame][0].getOpponentName()
		}

		// If the user is logged in, allow a random opponent to be picked.
		if (haveUser())
		{
			if (iGame == m_llGames.count)
			{
				return "Invite Friend"
			}
			else if (iGame == m_llGames.count + 1)
			{
				return "Random Opponent"
			}
		}
		return nil
	}

	//**********************************************************************************************
	func
	getGameTurnStatus(iGame: Int) -> Bool
	{
		if (iGame < m_llGames.count)
		{
			return m_llGames[iGame][0].isMyTurn()
		}
		return false
	}

	//**********************************************************************************************
	func
	getHighScore(iPlayer: Int) -> Int
	{
		if let game = m_game
		{
			return game.getHighScore(iPlayer: iPlayer)
		}
		return 0
	}

	//**********************************************************************************************
	func
	getNextState() -> State
	{
		if let eState = Main.s_mNext[m_eState]
		{
			return eState
		}
		return .OFF
	}

	//**********************************************************************************************
	func
	getOutcome() -> String
	{
		let nScoreMe = getScore(iPlayer: m_iPlayer)
		let nScoreThem = getScore(iPlayer: m_iPlayer ^ 1)
		if (nScoreMe > nScoreThem)
		{
			return "Congrats, you won!"
		}
		if (nScoreMe < nScoreThem)
		{
			return "Sorry, you lost."
		}
		return "Wow, tie game."
	}

	//**********************************************************************************************
	func
	getPlayer() -> Int
	{
		return m_iPlayer
	}

	//**********************************************************************************************
	func
	getPlayer(eState: State) -> Int
	{
		switch (eState)
		{
			case .GAME_LOAD_PLAYER1,
				 .GAME_LOAD_PLAYER1_WAITING,
				 .GAME_RELOAD_PLAYER1,
				 .GAME_RELOAD_PLAYER1_WAITING:
				return 0

			case .GAME_LOAD_PLAYER2,
				 .GAME_LOAD_PLAYER2_WAITING,
				 .GAME_RELOAD_PLAYER2,
				 .GAME_RELOAD_PLAYER2_WAITING:
				return 1

			default:
				return 0
		}
	}

	//**********************************************************************************************
	func
	getPlayerCount() -> Int
	{
		if let game = m_game
		{
			return game.getPlayerCount()
		}
		return 0
	}

	//**********************************************************************************************
	func
	getRackSize() -> Int
	{
		if let game = m_game
		{
			return game.getRackSize()
		}
		return 0
	}

	//**********************************************************************************************
	func
	getRackTileCount() -> Int
	{
		return m_game!.getRackTileCount(iPlayer: m_iPlayer)
	}

	//**********************************************************************************************
	func
	getRackVowelCount() -> Int
	{
		return m_game!.getRackVowelCount(iPlayer: m_iPlayer)
	}

	//**********************************************************************************************
	func
	getRanking(iPlayer: Int) -> Int
	{
		if let game = m_game
		{
			return game.getRanking(iPlayer: iPlayer)
		}
		return 0
	}

	//**********************************************************************************************
	func
	getScore(iPlayer: Int) -> Int
	{
		if let game = m_game
		{
			return game.getScore(iPlayer: iPlayer)
		}
		return 0
	}

	//**********************************************************************************************
	func
	getState() -> State
	{
		return m_eState
	}

	//**********************************************************************************************
	func
	getStatus() -> String?
	{
		return m_strStatus
	}

	//**********************************************************************************************
	func
	getUserEmail() -> String?
	{
		return m_dbUser!.getUserEmail()
	}

	//**********************************************************************************************
	func
	getUserName() -> String
	{
		return m_dbUser!.getUserName()
	}

	//**********************************************************************************************
	func
	getWords() -> Words
	{
		return m_words
	}

	//**********************************************************************************************
	func
	haveBestWord() -> Bool
	{
		if let game = m_game
		{
			if let _ = game.getBoardBest()
			{
				return true
			}
		}
		return false
	}

	//**********************************************************************************************
	func
	haveTiles() -> Bool
	{
		if let game = m_game
		{
			return game.getTileCount(iPlayer: m_iPlayer) > 0
		}
		return false
	}

	//**********************************************************************************************
	func
	haveTurn() -> Bool
	{
		var bHaveTurn = false
		
		// Parse the list of games and push those that are our turn to the front.
		var iOpen = 0
		for iList in 1..<m_llGames.count
		{
			// Sync the game with the database.
			let lGames = m_llGames[iList]
			let game = lGames[0]
			game.updateGame(bAlert: false)

			// If it's our turn, move this game up in the list.
			if (game.isMyTurn())
			{
				if (iOpen > 0)
				{
					m_llGames[iList] = m_llGames[iOpen]
					m_llGames[iOpen] = lGames
					iOpen += 1
				}
				bHaveTurn = true
			}
			else if (iOpen == 0)
			{
				iOpen = iList
			}
		}
		return bHaveTurn
	}

	//**********************************************************************************************
	func
	haveUser() -> Bool
	{
		if let dbUser = m_dbUser
		{
			return dbUser.hasDocument() && !dbUser.hasFailure()
		}
		return false
	}

	//**********************************************************************************************
	func
	haveVowels() -> Bool
	{
		if let game = m_game
		{
			return game.getVowelCount(iPlayer: m_iPlayer) > 0
		}
		return false
	}

	//**********************************************************************************************
	func
	isBoardEmpty() -> Bool
	{
		if let game = m_game
		{
			return game.isBoardEmpty()
		}
        return true
	}

	//**********************************************************************************************
	func
	isBoardRotated() -> Bool
	{
		return (m_bmSettings & getBitMask(eSetting: .BOARD_ROTATED)) != 0
	}

	//**********************************************************************************************
	func
	isFocusBlocked() -> Bool
	{
		let bBlockFocus = (m_bmSettings & getBitMask(eSetting: .FOCUS_BLOCKED)) != 0
		m_bmSettings &= ~getBitMask(eSetting: .FOCUS_BLOCKED);
		return bBlockFocus
	}

	//**********************************************************************************************
	func
	isGameSelected() -> Bool
	{
		return m_iGame >= 0 && m_iGame < m_llGames.count
	}

	//**********************************************************************************************
	func
	isGameTimed() -> Bool
	{
		return (m_bmSettings & getBitMask(eSetting: .GAME_TIMED)) != 0
	}

	//**********************************************************************************************
	func
	isInThread() -> Bool
	{
		return m_bInThread
	}

	//**********************************************************************************************
	func
	isRobotWorking() -> Bool
	{
		if let game = m_game
		{
			return game.isRobotWorking()
		}
		return false
	}

	//**********************************************************************************************
	func
	isTilesLow() -> Bool
	{
		if let game = m_game
		{
			return game.isTilesLow(iPlayer: m_iPlayer)
		}
		return false
	}

	//**********************************************************************************************
	func
	isWorking() -> Bool
	{
		if let game = m_game
		{
			return game.isWorking()
		}
		return false
	}

	//**********************************************************************************************
	func
	isTilePlaced() -> Bool
	{
		if let game = m_game
		{
			return game.isTilePlaced()
		}
		return false
	}

	//**********************************************************************************************
	func
	moveRobot()
	{
		let queue = DispatchQueue(label: "Moving robot...");
		queue.async
		{
			do
			{
				if let game = self.m_game
				{
					// Play the next word.
					self.setInThread(bInThread: true)
					do
					{
						try game.moveRobot()
						game.postMove(iPlayer: self.m_iPlayer ^ 1)
						game.confirmWord()
					}
					catch
					{
					}
					self.setInThread(bInThread: false)
				}
			}
		}
 	}

	//**********************************************************************************************
	func
	rotateBoard()
	{
		m_bmSettings ^= getBitMask(eSetting: .BOARD_ROTATED)
		blockFocus()
	}

	//**********************************************************************************************
	func
	setAction(eAction: Action)
	{
		setAction(eAction: eAction, mData: nil)
	}

	//**********************************************************************************************
	func
	setAction(
		eAction: Action,
		mData: [String: Any]?)
	{
		setStatus(strStatus: nil)

		// Handle user access.
		switch (eAction)
		{
			case .ABOUT:
				setState(eState: .ABOUT)
				return

			case .CANCEL:
				setCancel()
				return
				
			case .CONFIRM:
				setConfirm(mData: mData)
				return

			case .DONE:		// If we're done displaying something, return the parent.
				switch (m_eState)
				{
					case .ABOUT:
						setState(eState: m_eStateParent)
						break

					case .GAME_OVER_ALERT:
						setState(eState: .GAME_OVER_MENU)
						return

					case .GAME_OVER:
						setState(eState: .GAME_OVER_REPLAY)
						return

					case .TURN_BESTWORD:
						setState(eState: .TURN_START)
						return

					default:
						break
				}
				break

			case .PLAY_AGAIN:
				setConfirm(mData: nil)
				return

			case .PLAY_GAME:
				switch (m_eState)
				{
					case .USER_LOGIN,
						 .USER_WELCOME:
						if (!haveUser())
						{
							m_iGame = 0
							setState(eState: .GAME_START)
							return
						}
						// fallthru;
						
					case .USER_WELCOME_WAITING:
						setState(eState: .GAME_SELECT)
						return

					default:
						break
				}
				break

			case .USER_DELETE:
				setState(eState: .USER_DELETE)
				return
				
			case .USER_EMAIL:
				setState(eState: .USER_EMAIL)
				return
				
			case .USER_LOGIN:
				switch (m_eState)
				{
					case .USER_EMAIL,
						 .USER_LOGIN:
						if let mData = mData
						{
							setUserLogin(mData: mData)
						}
						return

					default:
						break
				}
				setState(eState: .USER_LOGIN)
				return
				
			case .USER_REGISTER:
				if let mData = mData
				{
					setUserRegister(mData: mData)
				}
				return

			case .USER_RESET:
				if let mData = mData
				{
					setUserReset(mData: mData)
				}
				return
				
			default:
				break
		}

		// Need an active game for the remaining actions.
		if let game = m_game
		{
			// Allow our user to exit out of any game.
			switch (eAction)
			{
				case .QUIT_GAME:
					if (game.isRobot())
					{
						setState(eState: .GAME_QUIT_ROBOT)
					}
					else
					{
						setState(eState: .GAME_QUIT)
					}
					return

				case .SWITCH_GAME:
					if (haveUser())
					{
						switch (m_eState)
						{
							case .GAME_OVER_ALERT,
								 .GAME_OVER_MENU:
								deleteGame()
								// fallthru

							case .GAME_OVER,
								 .GAME_OVER_QUIT:
								setState(eState: .GAME_RELOAD)
								break

							default:
								setState(eState: .GAME_SELECT)
								break
						}
					}
					else
					{
						setState(eState: .USER_WELCOME)
					}
					return

				default:
					break
			}

			// If our game is over, allow one action to restart.
			if (game.isOver())
			{
				setStatus(strStatus: "The game is over")
				return
			}

			// These actions do not require a filled rack.
			switch (eAction)
			{
				case .DISPLAY_BEST:
					blockFocus()
					setState(eState: .TURN_BESTWORD)
					return
				
				case .DRAW_BLACK_TILE,
					 .DRAW_WHITE_TILE:
					// Don't let the user initially draw all consonants.
					if (getScore(iPlayer: m_iPlayer) == 0 &&
						getRackTileCount() >= 5 &&
						getRackVowelCount() == 0)
					{
						setStatus(strStatus: "** Pick vowels, too **")
						return
					}
					
					if (!game.grabTile(iPlayer: m_iPlayer))
					{
						setStatus(strStatus: "Can't draw a tile")
						return
					}
					setState(eState: .TURN_DRAW)
					return
				
				case .DRAW_BLACK_VOWEL,
					 .DRAW_WHITE_VOWEL:
					// Don't let the user initially draw all consonants.
					if (getScore(iPlayer: m_iPlayer) == 0 &&
						getRackTileCount() >= 5 &&
						getRackVowelCount() == 0)
					{
						setStatus(strStatus: "** Pick consonants, too **")
						return
					}
					
					if (!game.grabVowel(iPlayer: m_iPlayer))
					{
						setStatus(strStatus: "Can't draw a vowel")
						return
					}
					setState(eState: .TURN_DRAW)
					return

				case .PLAY_GAME:
					switch (m_eState)
					{
						case .TURN_START:
							setState(eState: .TURN_DRAW)
							break

						case .TURN_DRAW,
							 .TURN_DRAW_FIRST,
							 .TURN_FIRST:
							setState(eState: .TURN_PLACE)
							break

						default:
							break
					}
					break

				case .ROTATE_BOARD:
					rotateBoard()
					return

				case .USER_SETUP:
					setState(eState: .USER_SETUP)
					return

				default:
					break
			}

			// Make sure our rack is filled.
			if (!game.isRackFilled(iPlayer: m_iPlayer))
			{
				return
			}

			switch (eAction)
			{
				case .JUMBLE_RACK,
					 .RACK_TILES:
					if (!game.withdrawAllTiles())
					{
						game.jumbleRack(iPlayer: m_iPlayer)
					}
					setState(eState: .TURN_PLACE)
					break
				
				case .PLAY_WORD:
					switch (m_eState)
					{
						case .TURN_READY:
							// Force the board to unzoom before scoring the word.
							blockFocus()

							// Make sure this is a valid play.
							let eStatus = game.playWord(iUser: m_iPlayer)
							if (eStatus != .VALID)
							{
								setStatus(strStatus: PlayStatus(eState: eStatus).toString())
								setState(eState: .TURN_INVALID)
								break
							}
							setState(eState: .TURN_VERIFY)
							break

						default:
							break
					}
					break

				case .SKIP_TURN:
					setState(eState: .TURN_SKIP)
					break
				
				case .UNZOOM_BOARD:
					blockFocus()
					break

				case .USER_BLOCK:
					setState(eState: .USER_BLOCK)
					break

				default:
					break
			}
		}
		else
		{
			setStatus(strStatus: "Please select a game")
		}
	}

	//**********************************************************************************************
	func
	setCancel()
	{
		switch (m_eState)
		{
			case .ABOUT,
				 .TURN_SKIP,
				 .GAME_QUIT,
				 .GAME_QUIT_ROBOT,
				 .USER_BLOCK,
				 .USER_DELETE:
				setState(eState: m_eStateParent)
				break

			case GAME_INVITE:
				setState(eState: .GAME_SELECT)
				break

			case .USER_LOGIN_WAITING,
				 .USER_REGISTER_WAITING:
				setState(eState: .GAME_LOAD)
				break
				
			case .USER_SETUP:
				setState(eState: .GAME_SELECT)
				break

			default:
				if (m_game!.isOver())
				{
					deleteGame()
					if (haveUser())
					{
						setState(eState: .GAME_RELOAD)
					}
					else
					{
						setState(eState: .USER_WELCOME)
					}
				}
				break
		}
	}

	//**********************************************************************************************
	func
	setConfirm(mData: [String: Any]?)
	{
		if let game = m_game
		{
			switch (m_eState)
			{
				case GAME_INVITE:
					setUserInvite(mData: mData)
					setStateNext()
					break

				case .USER_WAITING,
					 .USER_REGISTER_WAITING:
					setState(eState: .GAME_LOAD)
					return

				case .USER_SETUP:
					setUserDetails(mData: mData)
					setState(eState: .GAME_SELECT)
					break
					
				case .TURN_SKIP:
					game.skipTurn()
					setState(eState: .TURN_START)
					break

				case .USER_BLOCK:
					if let dbGame = m_dbGame
					{
						dbGame.blockOpponent()
					}
					// fallthru;

				case .GAME_OVER_QUIT,
					 .GAME_QUIT_ROBOT:
					m_iGame = -1;
					// TODO Delete game object to avoid null access.
					// fallthru;
				
				case .GAME_QUIT:
					// If the user is flat out just quitting the game, have it impact his ranking.
					if (m_eState == .GAME_OVER &&
						!game.isRobot() &&
						getScore(iPlayer: m_iPlayer) > 200)
					{
						game.setScore(iPlayer: m_iPlayer, nScore: 0);
						game.setGameEndScore(iPlayer: m_iPlayer);
					}
					game.clearOpponentScore()
				    deleteGame();
					if (haveUser())
					{
						setState(eState: .GAME_RELOAD)
					}
					else
					{
						setState(eState: .USER_WELCOME)
					}
					break

				case .USER_DELETE:
					setUserDelete()
					setState(eState: .USER_WELCOME)
					break

				default:
					if (game.isOver())
					{
						game.reset()
						m_iPlayer = 0
						m_dbGame = game.getDbGame()
						setState(eState: .GAME_START)
					}
					break
			}
		}
	}

	//**********************************************************************************************
	func
	setGame(iGame: Int)
	{
		m_iGame = iGame
		setState(eState: .GAME_SELECTED)
	}

	//**********************************************************************************************
	func
	setInThread(bInThread: Bool)
	{
		m_bInThread = bInThread
	}

	// *********************************************************************************************
    func
	setNotification(strNote: String)
	{
		//m_noteBuilder.setContentText(strNote)
        //m_idNotification += 1
		//m_noteManager.notify(m_idNotification, m_noteBuilder.build())
	}

	//**********************************************************************************************
	func
	setState(eState: State)
	{
		let eStateLast = m_eState
		if (eState != m_eState)
		{
			m_eState = eState
		}

		switch (eState)
		{
			case .GAME_CREATE:
				DbGame.CreateRandomGame()
				setStateNext()
				break

			case .GAME_CREATE_WAITING:
				if (DbGame.LoadRandomComplete())
				{
					// If we have no new game, default to the robot game.
					if (m_iGame == m_llGames.count)
					{
						m_main!.setAlert(strAlert: "Invitation Sent")
						setState(eState: .GAME_SELECT)
						return
					}
					// If we have no new game, default to the robot game.
					if (m_iGame > m_llGames.count)
					{
						m_iGame = 0
					}
					setStateNext()
				}
				break

			case .GAME_LOAD,
				 .GAME_RELOAD:
				// We need the user logged on to load his games.
				if (!haveUser())
				{
					setState(eState: .USER_WELCOME)
					break
				}
				setStateNext()
				break

			case .GAME_LOAD_GAME:
				m_iGame = -1
				DbGame.LoadGame(iPlayer: 0, strOpponentId: m_strNewOpponentId)
				setStateNext()
				break

			case .GAME_LOAD_GAME_WAITING:
				if (DbGame.LoadComplete(0))
				{
					setStateNext()
				}
				break

			case .GAME_LOAD_ROBOT,
				 .GAME_RELOAD_ROBOT:
				DbGame.LoadRobotGame()
				setStateNext()
				break

			case .GAME_LOAD_ROBOT_WAITING,
				 .GAME_RELOAD_ROBOT_WAITING:
				if (DbGame.LoadRobotComplete())
				{
					setStateNext()
				}
				break

			case .GAME_LOAD_PLAYER1,
				 .GAME_LOAD_PLAYER2,
				 .GAME_RELOAD_PLAYER1,
				 .GAME_RELOAD_PLAYER2:
				DbGame.LoadGames(iPlayer: getPlayer(eState: eState))
				setStateNext()
				break

			case .GAME_LOAD_PLAYER1_WAITING,
				 .GAME_LOAD_PLAYER2_WAITING,
				 .GAME_RELOAD_PLAYER1_WAITING,
				 .GAME_RELOAD_PLAYER2_WAITING:
				if (DbGame.LoadComplete(iPlayer: getPlayer(eState: eState)))
				{
					setStateNext()
				}
				break

			case .GAME_OVER:
				// If the other player went out first, they make the decission to play again.
				if let dbGame = m_dbGame
				{
					if (dbGame.getPlayerTurn() < 0 &&
						dbGame.getPlayerTurn() != -1 - dbGame.getPlayer())
					{
						setState(eState: .GAME_OVER_ALERT)
						break
					}
				}
				// fallthru

			case GAME_OVER_ALERT:
			case GAME_OVER_MENU:
				setStatus(strStatus: " ")
				m_eStateParent = eStateLast
				break

			case .GAME_OVER_QUIT:
				setStatus(strStatus: "Opponent quit the game")
				break

			case .GAME_OVER_REPLAY:
				setStatus(strStatus: "Play again?")
				m_eStateParent = eStateLast
				break

			case .GAME_QUIT:
				setStatus(strStatus: "Confirm to quit the game?")
				m_eStateParent = eStateLast
				break

			case .GAME_QUIT_ROBOT:
				setStatus(strStatus: "Confirm to quit robot game?")
				m_eStateParent = eStateLast
				break

			case .GAME_SELECT:
				// Make sure each game is fully loaded, including the opponent name.
				for iGame in 0..<m_llGames.count
				{
					// Wait until all the opponents are known.
					if (getGameOpponentName(iGame: iGame) == nil)
					{
						return
					}
				}
				setStateNext()
				break

			case .GAME_SELECT_WAITING:
				setStatus(strStatus: nil)
				m_iGame = -1
				break

			case .GAME_SELECTED:
				// Make sure a game has been selected.
				if (m_iGame < 0)
				{
					setState(eState: .GAME_SELECT_WAITING)
					break
				}

				// If we have this game loaded, resume playing.
				if (m_iGame < m_llGames.count)
				{
					m_game = m_llGames[m_iGame][0]
					if let game = m_game
					{
						m_dbGame = game.getDbGame()
						if let dbGame = m_dbGame
						{
							m_iPlayer = dbGame.getPlayer()
						}
					}
					setState(eState: .GAME_RESUME)
					break
				}

				if (m_iGame == m_llGames.count)
				{
					setState(eState: .GAME_INVITE)
					break
				}

				// Trigger the load of a random opponent game and check for it to complete.
				setState(eState: .GAME_CREATE)
				break

			case .GAME_RESUME:
				if let game = m_game
				{
					// If the game has been quit by the other player, wait for aknowledgement and delete.
					if (game.isQuit())
					{
						setState(eState: .GAME_OVER_QUIT)
						break
					}
					// If the resumed game is over, delete this and start a new one.
					if (game.isOver())
					{
						setState(eState: .GAME_OVER)
						break
					}
					// Make sure we are assigned to the correct player.
				}
				// fallthru

			case .GAME_START:
				if let game = m_game
				{
					// If the game has been quit by the other player, wait for aknowledgement and delete.
					if (game.isQuit())
					{
						setState(eState: .GAME_OVER_QUIT)
						break
					}
					// If the resumed game is over, delete this and start a new one.
					if (game.isOver())
					{
						game.reset()
					}
					// Make sure we are assigned to the correct player.
					if let dbGame = m_dbGame
					{
						m_iPlayer = dbGame.getPlayer()
					}
					game.updateState()
				}
				setStateNext()
				break

			case .TURN_BESTWORD:
				m_eStateParent = eStateLast
				break

			case .TURN_DRAW:
				if let game = m_game
				{
					if (getScore(iPlayer: m_iPlayer) == 0 &&
						game.isRackEmpty(iPlayer: m_iPlayer))
					{
						setState(eState: .TURN_DRAW_FIRST)
						break
					}
					if (game.isRackFilled(iPlayer: m_iPlayer) ||
						(!haveTiles() && !haveVowels()))
					{
						setStatus(strStatus: nil)
						game.findBestBoard(iPlayer: m_iPlayer)
						setState(eState: .TURN_PLACE)
						break
					}
				}
				m_eStateParent = eStateLast
				setStatus(strStatus: "Draw more tiles")
				break

			case .TURN_DRAW_FIRST:
				setStatus(strStatus: "Draw vowels and consonants")
				m_eStateParent = eStateLast
				break

			case .TURN_FIRST:
				// If the idiot picked all consonates, check for an auto-pass.
				if (m_game!.isNoPlay())
				{
					setStatus(strStatus: "No place to go. Passing...")
					m_game!.skipTurn()
					setState(eState: .TURN_START)
					break
				}
				setStatus(strStatus: "Place word to touch center")
				m_eStateParent = eStateLast
				break

			case .TURN_INVALID:
				setStatus(strStatus: "Not all words valid")
				m_eStateParent = eStateLast
				break
				
			case .TURN_PLACE:
				if let game = m_game
				{
					// Wait for our turn to be able to place tiles.
					if (m_iPlayer != game.getPlayer())
					{
						setState(eState: .TURN_WAITING)
						break
					}

					// If there are no valid words to play, pass the player's turn.
					if (game.isNoPlay())
					{
						setStatus(strStatus: "No place to go. Passing...")
						game.skipTurn()
						setState(eState: .TURN_START)
						break
					}

					// Ignore if we are already in this state.
					if (eState == eStateLast)
					{
						break
					}

					// If our board is empty, switch states to show the user where to start.
					if (game.isEmpty())
					{
						setState(eState: .TURN_FIRST)
						break
					}
				}
				// If not told yet, let the user know to fling up connecting tiles.
				if (!m_bFlingMessage && isTilePlaced())
				{
					m_bFlingMessage = true
					setStatus(strStatus: "Fling up connecting tiles")
				}
				else
				{
					setStatus(strStatus: "Build a crossword")
				}
				// fallthru
			case .TURN_PLAY:
				m_eStateParent = eStateLast
				if let game = m_game
				{
					// If the tiles have been placed into a potential word, switch states.
					if (game.getPlayStatus() == .VALID)
					{
						setState(eState: .TURN_READY)
					}
				}
				break

			case .TURN_READY:
				m_eStateParent = eStateLast
				setStatus(strStatus: "Press play when ready")
				break

			case .TURN_ROBOT:
				moveRobot()
				setStateNext()
				break
				
			case .TURN_SKIP:
				setStatus(strStatus: "Confirm to pass your turn?")
				m_eStateParent = eStateLast
				break
				
			case .TURN_START:
				if let game = m_game
				{
					if (game.isOver())
					{
						setState(eState: .GAME_OVER)
						break
					}
					if (game.isPlayerRobot())
					{
						setState(eState: .TURN_ROBOT)
						break
					}
					if (m_iPlayer != game.getPlayer())
					{
						setState(eState: .TURN_WAITING)
						break
					}
					if (!isGameTimed())
					{
						setState(eState: .TURN_DRAW)
						break
					}
				}
				m_eStateParent = eStateLast
				setStatus(strStatus: "Press play when ready")
				break

			case .TURN_VERIFY:
				if let dbGame = m_dbGame
				{
					if (dbGame.isPushed() ||
						m_game!.isRobot())
					{
						m_game!.confirmWord()
						setStateNext()
						break
					}
					if (dbGame.isFailure())
					{
						m_game!.postMove(iPlayer: m_iPlayer)
					}
				}
				else
				{
					m_game!.confirmWord()
					setStateNext()
					break
				}
				// fallthru
				
			case .TURN_WAITING:
				if (m_game!.isRackFilled(iPlayer: m_iPlayer) ||
					(!haveTiles() && !haveVowels()))
				{
					setStatus(strStatus: "Waiting for opponent...")
				}
				else
				{
					setStatus(strStatus: "Draw more tiles")
				}
				m_eStateParent = eStateLast
				break
				
			case .USER_BLOCK:
				setStatus(strStatus: "Block games from this user?")
				m_eStateParent = eStateLast
				break

			case .USER_EMAIL,
				 .USER_LOGIN,
				 .USER_MESSAGE,
				 .USER_REGISTER_WAITING,
				 .USER_SETUP,
				 .USER_WAITING:
				break

			case .USER_WELCOME,
				 .USER_WELCOME_WAITING:
				m_eStateParent = eStateLast
				setStatus(strStatus: nil)
				break

			default:
				break
		}
	}

	//**********************************************************************************************
	func
	setStateNext()
	{
		setState(eState: getNextState())
	}

	//**********************************************************************************************
	func
	setStatus(strStatus: String?)
	{
		m_strStatus = strStatus
	}

	//**********************************************************************************************
	func
	setUserDelete()
	{
		m_dbUser!.userDelete()
		setState(eState: .USER_WELCOME)
	}

	//**********************************************************************************************
	func
	setUserDelete(mData: [String: Any]?)
	{
		// Get the login credentials.
        let strEmail = mData!["strEmail"] as! String
		let strPassword = mData!["strPassword"] as! String

		// Make sure these are not empty.
		if (strEmail.isEmpty || strPassword.isEmpty)
		{
			setStatus(strStatus: "Provide email and Jot password")
			return
		}
		m_dbUser!.userDelete(strEmail: strEmail, strPassword: strPassword)
	}

	//**********************************************************************************************
	func
	setUserDetails(mData: [String: Any]?)
	{
		if (haveUser())
		{
			if let strNickname = mData!["strNickname"] as? String
			{
				if (!m_dbUser!.setUserName(strDisplayName: strNickname))
				{
					setStatus(strStatus: "There's only one, JotRussell")
				}
			}
		}
	}

	//**********************************************************************************************
	func
	setUserInvite(mData: [String: Any]?)
	{
		// Get the friend's email address.
		m_strEmail = mData!["strEmail"] as? String
	    m_strEmail = (String)mData.get("strEmail");
		m_strNewOpponentId = m_dbUser!.userInvite(m_strEmail)
	}

	//**********************************************************************************************
	func
	setUserLoaded()
	{
		// Don't leave the welcome state until the user presses play.
		switch (m_eState)
		{
			case .USER_WELCOME:
				// Trigger the initial search for our games list.
				setState(eState: .GAME_LOAD)
				break

			case .GAME_INVITE,
				 .GAME_INVITE_WAITING:
				setState(eState: m_eState)
				break
				
			default:
				break
		}
	}

	//**********************************************************************************************
	func
	setUserLogin()
	{
		switch (m_eState)
		{
			case .USER_LOGIN,
				 .USER_REGISTER_WAITING,
				 .USER_WAITING,
				 .USER_WELCOME:
				setState(eState: .GAME_LOAD)
				break
			default:
				break
		}
	}

	//**********************************************************************************************
	func
	setUserLogin(mData: [String: Any])
	{
		// Get the login credentials.
		let strEmail = mData["strEmail"] as! String
		let strPassword = mData["strPassword"] as! String
		let strNickname = mData["strNickname"] as! String

		// Make sure these are not empty.
		if (strEmail.isEmpty || strPassword.isEmpty)
		{
			setStatus(strStatus: "Provide user email and Jot password")
			setState(eState: .USER_LOGIN)
			return
		}

		m_dbUser!.userLogin(strEmail: strEmail, strPassword: strPassword, strDisplayName: strNickname)
		setState(eState: .USER_LOGIN_WAITING)
	}

	//**********************************************************************************************
	func
	setUserLogin(accessToken: AccessToken)
	{
		m_dbUser.userLogin(accessToken: accessToken)
		setState(eState: .USER_LOGIN_WAITING)
	}

	//**********************************************************************************************
	func
	setUserLogin(account: GoogleSignInAccount)
	{
		m_dbUser!.userLogin(account: account)
		setState(eState: .USER_WAITING)
	}

	//**********************************************************************************************
	func
	setUserLogin(fbAuth: FirebaseAuth)
	{
		m_dbUser!.userLogin(fbAuth: fbAuth)
		setState(eState: .USER_WAITING)
	}

	//**********************************************************************************************
	func
	setUserRegister()
	{
		setState(eState: .USER_WAITING)
	}

	//**********************************************************************************************
	func
	setUserRegister(mData: [String: Any])
	{
		// Get the login credentials.
		let strEmail: String = mData["strEmail"] as! String
		let strPassword: String = mData["strPassword"] as! String
		let strNickname: String = mData["strNickname"] as! String

		// Make sure these are not empty.
		if (strEmail.isEmpty || strPassword.isEmpty)
		{
			setStatus(strStatus: "Provide email and Jot password")
			setState(eState: .USER_EMAIL)
			return
		}

		m_dbUser!.userRegister(strEmail: strEmail, strPassword: strPassword, strNickname: strNickname)
		setState(eState: .USER_REGISTER_WAITING)
	}

	//**********************************************************************************************
	func
	setUserReset(mData: [String: Any])
	{
		// Get the login credentials.
		let strEmail: String = mData["strEmail"] as! String

		// Make sure these are not empty.
		if (strEmail == "")
		{
			setStatus(strStatus: "Provide your user email")
			setState(eState: .USER_EMAIL)
			return
		}

		m_dbUser!.userReset(strEmail: strEmail)
		setState(eState: .USER_WELCOME)
	}

	// *********************************************************************************************
	func
	getWidth() -> Int
	{
        return m_width
	}

	// *********************************************************************************************
	func
	getHeight() -> Int
	{
        return m_height
	}

	// *********************************************************************************************
    func
	setAlert(strAlert: String)
	{
        m_strAlert = strAlert
	}

	// *********************************************************************************************
    func
	failMessage(strMessage: String)
	{
		print(strMessage)
	}

	// *********************************************************************************************
    func
	playTone()
	{
		// Output a tone for each update.
		//m_toneGen.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
	}

	// *********************************************************************************************
	func
	setWidth(width: Int)
	{
        m_width = width
	}

	// *********************************************************************************************
	func
	setHeight(height: Int)
	{
		m_height = height
	}

	// *********************************************************************************************
    func
	setMainView(mainView: MainView)
	{
		m_mainView = mainView

		// Initialize our DbUser connection to work with our activity.
		DbUser.Initialize(main: self)

		// Create a default user based on the database login.
		m_dbUser = DbUser()

		// Initialize our DbGame connection to work with our activity.
		DbGame.Initialize(main: self, dbUser: m_dbUser!)
		
		let game = Jot(words: m_words, main: self, nPlayers: 2, iPlayerRobot: 1)
		m_game = game
		m_llGames[0][0] = game
		m_iGame = -1

		// By default, put us in the welcome state.
		setState(eState: .USER_WELCOME)
	}
	
	// *********************************************************************************************
    func
	updateState(bAnimate: Bool)
	{
		// Only allow one update to trigger at any given time.
		if (!m_mainView!.prepareUpdate())
		{
			return
		}

		// Update the underlying code.
		updateState()
		do
		{
			// If needed, run an animation of the played turn.
			if (bAnimate)
			{
				// Update the our view of the game to be in sync with the game.
				m_mainView!.animateUpdate()
			}
			else
			{
				// Run a quick update.
				while (try m_mainView!.updateView())
				{
					Thread.sleep(forTimeInterval: 1)
				}
				m_mainView!.resetUpdate()
			}
		}
		catch
		{
			failMessage(strMessage: "updateState() failed: \(error)")
		}
	}

	//**********************************************************************************************
	func
	updateState()
	{
		// Avoid updates while the robot is working.
		if (isInThread())
		{
			return;
		}
		
		switch (m_eState)
		{
			case .GAME_CREATE,
				 .GAME_CREATE_WAITING,
				 .GAME_INVITE,
				 .GAME_INVITE_WAITING,
				 .GAME_LOAD,
				 .GAME_LOAD_GAME,
				 .GAME_LOAD_GAME_WAITING,
				 .GAME_LOAD_ROBOT,
				 .GAME_LOAD_ROBOT_WAITING,
				 .GAME_LOAD_PLAYER1,
				 .GAME_LOAD_PLAYER1_WAITING,
				 .GAME_LOAD_PLAYER2,
				 .GAME_LOAD_PLAYER2_WAITING,
				 .GAME_RELOAD,
				 .GAME_RELOAD_ROBOT,
				 .GAME_RELOAD_ROBOT_WAITING,
				 .GAME_RELOAD_PLAYER1,
				 .GAME_RELOAD_PLAYER1_WAITING,
				 .GAME_RELOAD_PLAYER2,
				 .GAME_RELOAD_PLAYER2_WAITING,
				 .GAME_SELECT,
				 .GAME_SELECT_WAITING,
				 .GAME_SELECTED,
				 .TURN_VERIFY:
				// Add a short delay to allow other processes to check for load completion.
				Thread.sleep(forTimeInterval: 10)
				setState(eState: m_eState)
				return

			default:
				break
		}

		if let game = m_game
		{
			if (m_iGame >= 0)
			{
				// Update the game state.  If the user just went, update our best board.
				let iPlayer = game.getPlayer()
				game.updateState()
				if (iPlayer != game.getPlayer() &&
					game.isRackFilled(iPlayer: m_iPlayer))
				{
					game.findBestBoard(iPlayer: m_iPlayer)
				}

				// If the opponent is low on tiles, pop up an alert.
				if let strAlert = m_game.getTilesLeftAlert(iPlayer: m_iPlayer ^ 1)
				{
					m_main!.setAlert(strAlert)
				}

				// Switch the main state based on the game state.
				switch (game.getState())
				{
					case .PLACE_TILES:
						if (m_eState == .TURN_READY)
						{
							setState(eState: .TURN_PLACE)
						}
						break
					
					case .DRAW_TILES,
						 .FIRST_WORD,
						 .PLAY_TILES,
						 .MOVE_ROBOT:
						break

					case .PLAY_READY:
						setState(eState: .TURN_READY)
						break
					
					case .OVER:
						switch (m_eState)
						{
							case .USER_WELCOME,
								 .USER_WELCOME_WAITING:
								game.reset()
								break

							case .GAME_OVER,
								 .GAME_OVER_ALERT,
								 .GAME_OVER_MENU,
								 .GAME_OVER_REPLAY:
								break

							default:
								setState(eState: .GAME_OVER)
						}

					default:
						break
				}

				switch (m_eState)
				{
					case .TURN_WAITING:
						if (m_iPlayer == game.getPlayer() ||
							game.isOver())
						{
							setState(eState: .TURN_START)
						}
						break

					default:
						break
				}
			}
		}
	}
}

//**************************************************************************************************
