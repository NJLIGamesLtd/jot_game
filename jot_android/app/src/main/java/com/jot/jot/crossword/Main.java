/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Stores an arrangement of a crossword com.jot.game board.
 */

package com.jot.jot.crossword;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.FirebaseAuth;
import com.jot.decomp.Words;
import com.jot.jot.android.MainActivity;
import com.jot.jot.db.DbGame;
import com.jot.jot.db.DbUser;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.facebook.AccessToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class Main
{
	public enum State
	{
		OFF,
		ABOUT,
		GAME_CREATE,
		GAME_CREATE_EMAIL,
		GAME_CREATE_WAITING,
		GAME_INVITE,
		GAME_INVITE_WAITING,
		GAME_LOAD,
		GAME_LOAD_GAME,
		GAME_LOAD_GAME_WAITING,
		GAME_LOAD_ROBOT,
		GAME_LOAD_ROBOT_WAITING,
		GAME_LOAD_PLAYER1,
		GAME_LOAD_PLAYER1_WAITING,
		GAME_LOAD_PLAYER2,
		GAME_LOAD_PLAYER2_WAITING,
		GAME_OVER,
		GAME_OVER_ALERT,
		GAME_OVER_MENU,
		GAME_OVER_QUIT,
		GAME_OVER_REPLAY,
		GAME_QUIT,
		GAME_QUIT_ROBOT,
		GAME_RELOAD,
		GAME_RELOAD_ROBOT,
		GAME_RELOAD_ROBOT_WAITING,
		GAME_RELOAD_PLAYER1,
		GAME_RELOAD_PLAYER1_WAITING,
		GAME_RELOAD_PLAYER2,
		GAME_RELOAD_PLAYER2_WAITING,
		GAME_RESUME,
		GAME_SELECT,
		GAME_SELECT_WAITING,
		GAME_SELECTED,
		GAME_START,
		TURN_BESTWORD,
		TURN_DRAW,
		TURN_DRAW_FIRST,
		TURN_FIRST,
		TURN_INVALID,
		TURN_PLACE,
		TURN_PLAY,
		TURN_READY,
		TURN_ROBOT,
		TURN_SKIP,
		TURN_START,
		TURN_VERIFY,
		TURN_WAITING,
		USER_BLOCK,
		USER_DELETE,
		USER_DELETE_WAITING,
		USER_DELETED,
		USER_EMAIL,
		USER_LOGIN,
		USER_REGISTER_WAITING,
		USER_MESSAGE,
		USER_SETUP,
		USER_WAITING,
		USER_WELCOME,
		USER_WELCOME_WAITING;

		static private final Map<State, State> s_mNext = new HashMap();
		static
		{
			s_mNext.put(GAME_CREATE, GAME_CREATE_WAITING);
			s_mNext.put(GAME_CREATE_WAITING, GAME_SELECTED);
			s_mNext.put(GAME_INVITE, GAME_INVITE_WAITING);
			s_mNext.put(GAME_INVITE_WAITING, GAME_INVITE_WAITING);
			s_mNext.put(GAME_LOAD, GAME_LOAD_ROBOT);
			s_mNext.put(GAME_LOAD_GAME, GAME_LOAD_GAME_WAITING);
			s_mNext.put(GAME_LOAD_GAME_WAITING, GAME_SELECTED);
			s_mNext.put(GAME_LOAD_ROBOT, GAME_LOAD_ROBOT_WAITING);
			s_mNext.put(GAME_LOAD_ROBOT_WAITING, GAME_LOAD_PLAYER1);
			s_mNext.put(GAME_LOAD_PLAYER1, GAME_LOAD_PLAYER1_WAITING);
			s_mNext.put(GAME_LOAD_PLAYER2, GAME_LOAD_PLAYER2_WAITING);
			s_mNext.put(GAME_LOAD_PLAYER1_WAITING, GAME_LOAD_PLAYER2);
			s_mNext.put(GAME_LOAD_PLAYER2_WAITING, USER_WELCOME_WAITING);
			s_mNext.put(GAME_RELOAD, GAME_RELOAD_PLAYER1);
			s_mNext.put(GAME_RELOAD_ROBOT, GAME_RELOAD_ROBOT_WAITING);
			s_mNext.put(GAME_RELOAD_ROBOT_WAITING, GAME_RELOAD_PLAYER1);
			s_mNext.put(GAME_RELOAD_PLAYER1, GAME_RELOAD_PLAYER1_WAITING);
			s_mNext.put(GAME_RELOAD_PLAYER2, GAME_RELOAD_PLAYER2_WAITING);
			s_mNext.put(GAME_RELOAD_PLAYER1_WAITING, GAME_RELOAD_PLAYER2);
			s_mNext.put(GAME_RELOAD_PLAYER2_WAITING, GAME_SELECT);
			s_mNext.put(GAME_SELECT, GAME_SELECT_WAITING);
			s_mNext.put(GAME_RESUME, TURN_START);
			s_mNext.put(GAME_START, TURN_START);
			s_mNext.put(TURN_ROBOT, TURN_WAITING);
			s_mNext.put(TURN_VERIFY, TURN_START);
			s_mNext.put(USER_DELETE, USER_DELETE_WAITING);
			s_mNext.put(USER_DELETE_WAITING, USER_DELETED);
		}

		public State
		getNext()
		{
			State eState = s_mNext.get(this);
			if (eState != null)
			{
				return eState;
			}
			return OFF;
		}

		public int
		getPlayer()
		{
			switch (this)
			{
				case GAME_LOAD_PLAYER1:
				case GAME_LOAD_PLAYER1_WAITING:
				case GAME_RELOAD_PLAYER1:
				case GAME_RELOAD_PLAYER1_WAITING:
				default:
					return 0;

				case GAME_LOAD_PLAYER2:
				case GAME_LOAD_PLAYER2_WAITING:
				case GAME_RELOAD_PLAYER2:
				case GAME_RELOAD_PLAYER2_WAITING:
					return 1;
			}
		}

		public String
		toString()
		{
			return name();
		}
	}

	public enum Setting
	{
		BOARD_ROTATED,
		FOCUS_BLOCKED,
		GAME_TIMED;

		public int
		getBitMask()
		{
			return 1 << this.ordinal();
		}

		public String
		toString()
		{
			return name();
		}
	}

	public enum Action
	{
		ABOUT,
		CANCEL,
		CONFIRM,
		DISPLAY_BEST,
		DONE,
		DRAW_BLACK_TILE,
		DRAW_BLACK_VOWEL,
		DRAW_WHITE_TILE,
		DRAW_WHITE_VOWEL,
		JUMBLE_RACK,
		PLAY_AGAIN,
		PLAY_GAME,
		PLAY_WORD,
		PRIVACY,
		QUIT_GAME,
		RACK_TILES,
		ROTATE_BOARD,
		SELECT,
		SKIP_TURN,
		SWITCH_GAME,
		TERMS,
		UNZOOM_BOARD,
		USER_BLOCK,
		USER_DELETE,
		USER_EMAIL,
		USER_LOGIN,
		USER_PRIVACY,
		USER_REGISTER,
		USER_RESET,
		USER_SETUP;

		public String
		toString()
		{
			return name();
		}
	}

	private final MainActivity m_mainActivity;

	private final Words m_words;

	private int m_iGame;
	private Game m_game;
	private final List<List<Game>> m_llGames;

	private int m_iPlayer;

	private State m_eState;
	private State m_eStateParent;
	private int m_bmSettings;

	private DbGame m_dbGame;
	private final DbUser m_dbUser;
	private String m_strEmail;
	private String m_strNewOpponentId;

	private String m_strStatus;

	private boolean m_bFlingMessage = false;

	//**********************************************************************************************
	public
	Main(
		Words words,
		MainActivity mainActivity)
	{
		m_words = words;
		m_mainActivity = mainActivity;

		// Create a default game with the robot.
		m_iPlayer = 0;
		m_game = new Jot(words, mainActivity, 2, 1);
		m_llGames = new ArrayList<>();
		m_llGames.add(new ArrayList<Game>());
		m_llGames.get(0).add(m_game);
		m_iGame = -1;

		// Have to wait for a game to be loaded before having reference to it.
		m_dbGame = null;

		// By default, put us in the welcome state.
		setState(State.USER_WELCOME);

		// No special settings by default.
		m_bmSettings = 0;

		// Initialize our DbUser connection to work with our activity.
		DbUser.Initialize(this, m_mainActivity);

		// Create a default user based on the database login.
		m_dbUser = new DbUser();

		// Initialize our DbGame connection to work with our activity.
		DbGame.Initialize(this, m_mainActivity, m_dbUser);
	}

	//**********************************************************************************************
	public boolean
	addDbGame(DbGame dbGame)
	{
		String strOpponentId = dbGame.getOpponentId();
		String strGameId = dbGame.getGameId();
		List<Game> lGames;
		int iAdd = -1;
		int iNext = 0;
		int iList;
		Game game;

		// If we already have this game, treat it as an update.
		for (iList = 0; iList < m_llGames.size(); ++iList)
		{
			boolean bMyTurn = false;
			lGames = m_llGames.get(iList);
			for (int iGame = 0; iGame < lGames.size(); ++iGame)
			{
				game = lGames.get(iGame);
				DbGame dbGame2 = game.getDbGame();
				if (dbGame2 != null)
				{
					// If this is the same game, use the new DbGame object.
					if (strGameId.equals(dbGame2.getGameId()))
					{
						game.setDbGame(dbGame);

						// If it's our turn, try to push it up on the list.
						if (dbGame.isMyTurn() && iNext > 0)
						{
							m_llGames.set(iList, m_llGames.get(iNext));
							m_llGames.set(iNext, lGames);
						}

						return dbGame != dbGame2;
					}

					// If we already have a game list for this opponent, add the new game to it.
					if (dbGame2.getOpponentId().equals(strOpponentId))
					{
						iAdd = iList;
					}

					if (dbGame2.isMyTurn())
					{
						bMyTurn = true;
					}
				}
			}

			// If its the opponent's turn, allow another game list in front of this.
			if (iList > 0 && iNext == 0 && !bMyTurn)
			{
				iNext = iList;
			}
		}

		// If we have the posted DbGame with the robot, assign it to our previously created game.
		boolean bAddGame = false;
		if (dbGame.isRobot())
		{
			lGames = m_llGames.get(0);
			game = lGames.get(0);
		}
		else
		{
			// Create a new two-player game.
			game = new Jot(m_words, m_mainActivity, 2, -1);
			bAddGame = true;

			// If we need a new game list for this game, create it.
			if (iAdd < 0)
			{
				iAdd = m_llGames.size();
				lGames = new ArrayList<Game>();
				m_llGames.add(lGames);
			}
			else
			{
				lGames = m_llGames.get(iAdd);
			}

			// If it's our turn, try to move this up in the list.
			if (iNext > 0 && iNext < iAdd && dbGame.isMyTurn())
			{
				// If we just created this new game, have it selected.
				if (m_iGame == iAdd)
				{
					m_iGame = iNext;
				}

				// Push the next game up front.
				m_llGames.set(iAdd, m_llGames.get(iNext));
				m_llGames.set(iNext, lGames);

				if (m_eState == State.GAME_LOAD_GAME_WAITING)
				{
					m_iGame = iNext;
				}
			}
		}
		game.setDbGame(dbGame);
		game.setSettings();

		// If needed, add this game to our list.
		if (bAddGame)
		{
			// If this game is over, store it first so the user can see the outcome before
			// it is deleted.
			int iGame = lGames.size();
			if (dbGame.isMyTurn())
			{
				iGame = 0;
				if (lGames.size() > 0 && lGames.get(0).getDbGame().getPlayerTurn() < 0)
				{
					++iGame;
				}
			}
			lGames.add(iGame, game);
		}
		return true;
	}

	//**********************************************************************************************
	public void
	blockFocus()
	{
		m_bmSettings |= Setting.FOCUS_BLOCKED.getBitMask();
	}

	//**********************************************************************************************
	private void
	checkUserInvite()
	{
		switch (m_dbUser.getInviteState())
		{
			case USER_SEARCHING:
				break;

			case USER_EMPTY:
				setStatus("Provide a friend's email");
				setState(State.GAME_INVITE);
				break;

			case USER_SAME:
				setStatus("** That's your email **");
				setState(State.GAME_INVITE);
				break;

			case USER_BLOCKED:
				setStatus("Person has blocked you");
				setState(State.GAME_SELECT);
				break;

			case USER_ACTIVE:
				for (int i = 1; i < m_llGames.size(); ++i)
				{
					DbGame dbGame = m_llGames.get(i).get(0).getDbGame();
					if (dbGame != null && dbGame.isOpponent(m_strNewOpponentId))
					{
						setGame(i);
						m_iGame = i;
						setState(State.GAME_SELECTED);
						return;
					}
				}
				setStatus("Already have a game with them");
				setState(State.GAME_SELECT);
				break;

			case USER_INVITE:
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setData(Uri.parse("mailto:"));
				intent.setType("text/plain");
				
				intent.putExtra(Intent.EXTRA_EMAIL, new String[]{ m_strEmail });
				intent.putExtra(
					Intent.EXTRA_SUBJECT,
					m_dbUser.getUserName() + " would like to play you in Jot.");
				intent.putExtra(
					Intent.EXTRA_TEXT,
					"Jot is the Chess of Crosswords, with your own bag of letter tiles that have " +
					"a different shape and selection of vowels and consonants.\n" +
					"Available now for Android via https://play.google.com/store/apps/details?id=com.jot.jot\n" +
					"Coming soon for iPhone.\n" +
					"Check out https://JotRussell.com for more info.\n");
				try
				{
					m_mainActivity.startActivity(Intent.createChooser(intent, "Send email..."));
					m_mainActivity.finish();
				}
				catch (ActivityNotFoundException e)
				{
					m_mainActivity.failMessage("Email failed: " + e);
				}
				setStatus("Invite has been sent");
				setState(State.GAME_SELECT);
				break;

			case USER_FOUND:
				setStatus("Creating game...");
				setState(State.GAME_LOAD_GAME);
				break;
				
			case USER_START:
			case USER_WAITING:
				break;
		}
	}

	//**********************************************************************************************
	public void
	deleteGame()
	{
		if (m_game != null)
		{
			// Always kill the robot game object.  Just reset it.
			if (m_game.isRobot())
			{
				m_game.reset();
			}
			else
			{
				// Delete this game.
				m_game.delete();

				// Delete the game from our list of lists.
				for (int iList = 0; iList < m_llGames.size(); ++iList)
				{
					if (m_llGames.get(iList).remove(m_game))
					{
						if (m_llGames.get(iList).size() == 0)
						{
							m_llGames.remove(iList);
						}
						break;
					}
				}
				m_dbGame = null;
				m_game = null;
			}
		}
	}

	//**********************************************************************************************
	public String
	getStatus()
	{
		return m_strStatus;
	}

	//**********************************************************************************************
	public int
	getBoardHeight()
	{
		return m_game.getBoardHeight();
	}

	//**********************************************************************************************
	public int
	getBoardWidth()
	{
		return m_game.getBoardWidth();
	}

	//**********************************************************************************************
	public Action
	getDrawTileAction()
	{
		if (m_iPlayer == 0)
		{
			return Action.DRAW_WHITE_TILE;
		}
		return Action.DRAW_BLACK_TILE;
	}

	//**********************************************************************************************
	public Action
	getDrawVowelAction()
	{
		if (m_iPlayer == 0)
		{
			return Action.DRAW_WHITE_VOWEL;
		}
		return Action.DRAW_BLACK_VOWEL;
	}

	//**********************************************************************************************
	public Game
	getGame()
	{
		return m_game;
	}

	//**********************************************************************************************
	public int
	getGameCount()
	{
		return m_llGames.size();
	}

	//**********************************************************************************************
	public String
	getGameOpponentName(int iGame)
	{
		if (iGame < m_llGames.size())
		{
			return m_llGames.get(iGame).get(0).getOpponentName();
		}

		// If the user is logged in, allow a random opponent to be picked.
		if (haveUser())
		{
			if (iGame == m_llGames.size())
			{
				return "Invite Friend";
			}
			else if (iGame == m_llGames.size() + 1)
			{
				return "Random Opponent";
			}
		}
		return null;
	}

	//**********************************************************************************************
	public boolean
	getGameTurnStatus(int iGame)
	{
		if (iGame < m_llGames.size())
		{
			return m_llGames.get(iGame).get(0).isMyTurn();
		}
		return false;
	}

	//**********************************************************************************************
	public int
	getHighScore(int iPlayer)
	{
		if (m_game != null)
		{
			return m_game.getHighScore(iPlayer);
		}
		return 0;
	}

	//**********************************************************************************************
	public String
	getOutcome()
	{
		int nScoreMe = getScore(m_iPlayer);
		int nScoreThem = getScore(m_iPlayer ^ 1);
		if (nScoreMe > nScoreThem)
		{
			return "Congrats, you won!";
		}
		if (nScoreMe < nScoreThem)
		{
			return "Sorry, you lost.";
		}
		return "Wow, tie game.";
	}

	//**********************************************************************************************
	public int
	getPlayer()
	{
		return m_iPlayer;
	}

	//**********************************************************************************************
	public int
	getPlayerCount()
	{
		return m_game.getPlayerCount();
	}

	//**********************************************************************************************
	public int
	getRackSize()
	{
		return m_game.getRackSize();
	}

	//**********************************************************************************************
	public int
	getRackTileCount()
	{
		return m_game.getRackTileCount(m_iPlayer);
	}

	//**********************************************************************************************
	public int
	getRackVowelCount()
	{
		return m_game.getRackVowelCount(m_iPlayer);
	}

	//**********************************************************************************************
	public int
	getRanking(int iPlayer)
	{
		if (m_game != null)
		{
			return m_game.getRanking(iPlayer);
		}
		return 0;
	}

	//**********************************************************************************************
	public int
	getScore(int iPlayer)
	{
		return m_game.getScore(iPlayer);
	}

	//**********************************************************************************************
	public State
	getState()
	{
		return m_eState;
	}

	//**********************************************************************************************
	public String
	getUserEmail()
	{
		return m_dbUser.getUserEmail();
	}

	//**********************************************************************************************
	public String
	getUserName()
	{
		return m_dbUser.getUserName();
	}

	//**********************************************************************************************
	public Words
	getWords()
	{
		return m_words;
	}

	//**********************************************************************************************
	public boolean
	haveBestWord()
	{
		Board boardBest = m_game.getBoardBest();
		return boardBest != null;
	}

	//**********************************************************************************************
	public boolean
	haveTiles()
	{
		return m_game.getTileCount(m_iPlayer) > 0;
	}

	//**********************************************************************************************
	public boolean
	haveTurn()
	{
		boolean bHaveTurn = false;

		// Parse the list of games and push those that are our turn to the front.
		int iOpen = 0;
		for (int iList = 1; iList < m_llGames.size(); ++iList)
		{
			// Sync the game with the database.
			List<Game> lGames = m_llGames.get(iList);
			Game game = lGames.get(0);
			game.updateGame(false);

			// If it's our turn, move this game up in the list.
			if (game.isMyTurn())
			{
				if (iOpen > 0)
				{
					m_llGames.set(iList, m_llGames.get(iOpen));
					m_llGames.set(iOpen, lGames);
					++iOpen;
				}
				bHaveTurn = true;
			}
			else if (iOpen == 0)
			{
				iOpen = iList;
			}
		}
		return bHaveTurn;
	}

	//**********************************************************************************************
	public boolean
	haveUser()
	{
		return m_dbUser != null && m_dbUser.hasDocument() && !m_dbUser.hasFailure();
	}

	//**********************************************************************************************
	public boolean
	haveVowels()
	{
		return m_game.getVowelCount(m_iPlayer) > 0;
	}

	//**********************************************************************************************
	public boolean
	isBoardEmpty()
	{
		return m_game.isBoardEmpty();
	}

	//**********************************************************************************************
	public boolean
	isBoardRotated()
	{
		return (m_bmSettings & Setting.BOARD_ROTATED.getBitMask()) != 0;
	}

	//**********************************************************************************************
	public boolean
	isFocusBlocked()
	{
		boolean bBlockFocus = (m_bmSettings & Setting.FOCUS_BLOCKED.getBitMask()) != 0;
		m_bmSettings &= ~Setting.FOCUS_BLOCKED.getBitMask();
		return bBlockFocus;
	}

	//**********************************************************************************************
	public boolean
	isGameSelected()
	{
		return m_iGame >= 0 && m_iGame < m_llGames.size();
	}

	//**********************************************************************************************
	public boolean
	isGameTimed()
	{
		return (m_bmSettings & Setting.GAME_TIMED.getBitMask()) != 0;
	}

	//**********************************************************************************************
	public boolean
	isRobotWorking()
	{
		if (m_game != null)
		{
			return m_game.isRobotWorking();
		}
		return false;
	}

	//**********************************************************************************************
	public boolean
	isTilesLow()
	{
		if (m_game != null)
		{
			return m_game.isTilesLow(m_iPlayer);
		}
		return false;
	}

	//**********************************************************************************************
	public boolean
	isWorking()
	{
		if (m_game != null)
		{
			return m_game.isWorking();
		}
		return false;
	}

	//**********************************************************************************************
	public boolean
	isTilePlaced()
	{
		return m_game.isTilePlaced();
	}

	//**********************************************************************************************
	public void
	moveRobot()
	{
		new Thread(new Runnable()
		{
			public void
			run()
			{
				try
				{
					// Play the next word.
					m_mainActivity.setInThread(true);
					m_game.moveRobot();
					m_game.postMove(m_iPlayer ^ 1);
					m_game.confirmWord();
				}
				catch (Exception e)
				{
					m_mainActivity.failMessage("ROBOT Error: " + e);
				}
				m_mainActivity.setInThread(false);
			}
		}).start();
	}

	//**********************************************************************************************
	public void
	rotateBoard()
	{
		m_bmSettings ^= Setting.BOARD_ROTATED.getBitMask();
		blockFocus();
	}

	//**********************************************************************************************
	public void
	setAction(
		Action eAction)
	{
		setAction(eAction, null);
	}

	//**********************************************************************************************
	public void
	setAction(
		Action eAction,
		Map<String, Object> mData)
	{
		setStatus(null);

		// Handle user access.
		switch (eAction)
		{
			case ABOUT:
				setState(State.ABOUT);
				return;

			case CANCEL:
				setCancel();
				return;

			case CONFIRM:
				setConfirm(mData);
				return;

			case DONE:		// If we're done displaying something, return the parent.
				switch (m_eState)
				{
					case ABOUT:
						setState(m_eStateParent);
						break;

					case GAME_OVER_ALERT:
						setState(State.GAME_OVER_MENU);
						return;

					case GAME_OVER:
						setState(State.GAME_OVER_REPLAY);
						return;

					case TURN_BESTWORD:
						setState(State.TURN_START);
						return;
				}
				break;

			case PLAY_AGAIN:
				setConfirm(null);
				return;

			case PLAY_GAME:
				switch (m_eState)
				{
					case USER_LOGIN:
					case USER_WELCOME:
						if (!haveUser())
						{
							m_iGame = 0;
							setState(State.GAME_START);
							return;
						}
						// fallthru;
						
					case USER_WELCOME_WAITING:
						setState(State.GAME_SELECT);
						return;
				}
				break;

			case USER_DELETE:
				setState(State.USER_DELETE);
				return;

			case USER_EMAIL:
				setState(State.USER_EMAIL);
				return;

			case USER_LOGIN:
				switch (m_eState)
				{
					case USER_EMAIL:
					case USER_LOGIN:
						setUserLogin(mData);
						return;
				}
				setState(State.USER_LOGIN);
				return;

			case USER_REGISTER:
				setUserRegister(mData);
				return;

			case USER_RESET:
				setUserReset(mData);
				return;
		}

		// Need an active game for the remaining actions.
		if (m_game == null)
		{
			setStatus("Please select a game");
			return;
		}

		// Allow our user to exit out of any game.
		switch (eAction)
		{
			case QUIT_GAME:
				if (m_eState == State.USER_DELETED)
				{
					m_mainActivity.exit();
					return;
				}
				if (m_game.isRobot())
				{
					setState(State.GAME_QUIT_ROBOT);
				}
				else
				{
					setState(State.GAME_QUIT);
				}
				return;

			case SWITCH_GAME:
				if (haveUser())
				{
					switch (m_eState)
					{
						case GAME_OVER_ALERT:
						case GAME_OVER_MENU:
							deleteGame();
							// fallthru

						case GAME_OVER:
						case GAME_OVER_QUIT:
							setState(State.GAME_RELOAD);
							break;

						default:
							setState(State.GAME_SELECT);
							break;
					}
				}
				else
				{
					setState(State.USER_WELCOME);
				}
				return;
		}

		// If our game is over, allow one action to restart.
		if (m_game.isOver())
		{
			setStatus("The game is over");
			return;
		}

		// These actions do not require a filled rack.
		switch (eAction)
		{
			case DISPLAY_BEST:
				blockFocus();
				setState(State.TURN_BESTWORD);
				return;

			case DRAW_BLACK_TILE:
			case DRAW_WHITE_TILE:
				// Don't let the user initially draw all consonants.
				if (getScore(m_iPlayer) == 0 &&
					getRackTileCount() >= 5 &&
					getRackVowelCount() == 0)
				{
					setStatus("** Pick vowels, too **");
					return;
				}
					
				if (!m_game.grabTile(m_iPlayer))
				{
					setStatus("Can't draw a tile");
					return;
				}
				setState(State.TURN_DRAW);
				return;

			case DRAW_BLACK_VOWEL:
			case DRAW_WHITE_VOWEL:
				// Don't let the user initially draw all vowels.
				if (getScore(m_iPlayer) == 0 &&
					getRackTileCount() >= 5 &&
					getRackTileCount() == getRackVowelCount())
				{
					setStatus("** Pick consonants, too **");
					return;
				}
				if (!m_game.grabVowel(m_iPlayer))
				{
					setStatus("Can't draw a vowel");
					return;
				}
				setState(State.TURN_DRAW);
				return;

			case PLAY_GAME:
				switch (m_eState)
				{
					case TURN_START:
						setState(State.TURN_DRAW);
						break;

					case TURN_DRAW:
					case TURN_DRAW_FIRST:
					case TURN_FIRST:
						setState(State.TURN_PLACE);
						break;
				}
				break;

			case ROTATE_BOARD:
				rotateBoard();
				return;

			case USER_SETUP:
				setState(State.USER_SETUP);
				return;
		}

		// Make sure our rack is filled.
		if (!m_game.isRackFilled(m_iPlayer))
		{
			return;
		}

		switch (eAction)
		{
			case JUMBLE_RACK:
				m_game.jumbleRack(m_iPlayer);
				setState(State.TURN_PLACE);
				break;

			case RACK_TILES:
				m_game.withdrawAllTiles();
				setState(State.TURN_PLACE);
				break;

			case PLAY_WORD:
				switch (m_eState)
				{
					case TURN_READY:
						// Force the board to unzoom before scoring the word.
						blockFocus();

						// Make sure this is a valid play.
						PlayStatus eStatus = m_game.playWord(m_iPlayer);
						if (!eStatus.isValid())
						{
							setStatus(eStatus.toString());
							setState(State.TURN_INVALID);
							break;
						}
						setState(State.TURN_VERIFY);
						break;
				}
				break;

			case SKIP_TURN:
				setState(State.TURN_SKIP);
				break;

			case UNZOOM_BOARD:
				blockFocus();
				break;

			case USER_BLOCK:
				setState(State.USER_BLOCK);
				break;
		}
	}

	//**********************************************************************************************
	public void
	setCancel()
	{
		switch (m_eState)
		{
			case ABOUT:
			case TURN_SKIP:
			case GAME_QUIT:
			case GAME_QUIT_ROBOT:
			case USER_BLOCK:
			case USER_DELETE:
				setState(m_eStateParent);
				break;

			case GAME_INVITE:
				setState(State.GAME_SELECT);
				break;

			case USER_WAITING:
			case USER_REGISTER_WAITING:
				setState(State.GAME_LOAD);
				break;

			case USER_SETUP:
				setState(State.GAME_SELECT);
				break;

			default:
				if (m_game.isOver())
				{
					deleteGame();
					if (haveUser())
					{
						setState(State.GAME_RELOAD);
					}
					else
					{
						setState(State.USER_WELCOME);
					}
				}
				break;
		}
	}

	//**********************************************************************************************
	public void
	setConfirm(Map<String, Object> mData)
	{
		switch (m_eState)
		{
			case GAME_INVITE:
				setUserInvite(mData);
				setStateNext();
				break;

			case USER_WAITING:
			case USER_REGISTER_WAITING:
				setState(State.GAME_LOAD);
				return;

			case USER_SETUP:
				setUserDetails(mData);
				setState(State.GAME_SELECT);
				break;

			case TURN_SKIP:
				m_game.skipTurn();
				setState(State.TURN_START);
				break;

			case USER_BLOCK:
				if (m_dbGame != null)
				{
					m_dbGame.blockOpponent();
				}
				// fallthru;

			case GAME_OVER_QUIT:
			case GAME_QUIT_ROBOT:
				m_iGame = -1;
				// TODO Delete game object to avoid null access.
				// fallthru;

			case GAME_QUIT:
				// If the user is flat out just quitting the game, have it impact his ranking.
				if (m_eState == State.GAME_OVER &&
					!m_game.isRobot() &&
					getScore(m_iPlayer) > 200)
				{
					m_game.setScore(m_iPlayer, 0);
					m_game.setGameEndScore(m_iPlayer);
				}
				m_game.clearOpponentScore();
			    deleteGame();
				if (haveUser())
				{
					setState(State.GAME_RELOAD);
				}
				else
				{
					setState(State.USER_WELCOME);
				}
				break;

			case USER_DELETE:
				setUserDelete(mData);
				setStateNext();
				break;

			default:
				if (m_game.isOver())
				{
					m_game.reset();
					m_iPlayer = 0;
					m_dbGame = m_game.getDbGame();
					setState(State.GAME_START);
				}
				break;
		}
	}

	//**********************************************************************************************
	public void
	setGame(int iGame)
	{
		m_iGame = iGame;
		setState(State.GAME_SELECTED);
	}

	//**********************************************************************************************
	public void
	setState(State eState)
	{
		State eStateLast = m_eState;
		if (eState != m_eState)
		{
			m_eState = eState;
		}

		switch (eState)
		{
			case GAME_CREATE:
				DbGame.CreateRandomGame();
				setStateNext();
				break;

			case GAME_CREATE_WAITING:
				if (DbGame.LoadRandomComplete())
				{
					// If we have no new game, default to the robot game.
					if (m_iGame == m_llGames.size())
					{
						m_mainActivity.setAlert("Invitation Sent");
						setState(State.GAME_SELECT);
						return;
					}
					if (m_iGame > m_llGames.size())
					{
						m_iGame = 0;
					}
					setStateNext();
				}
				break;

			case GAME_INVITE_WAITING:
				checkUserInvite();
				return;

			case GAME_LOAD:
			case GAME_RELOAD:
				// We need the user logged on to load his games.
				if (!haveUser())
				{
					setState(State.USER_WELCOME);
					break;
				}
				setStateNext();
				break;

			case GAME_LOAD_GAME:
				m_iGame = -1;
				DbGame.LoadGame(0, m_strNewOpponentId);
				setStateNext();
				break;

			case GAME_LOAD_GAME_WAITING:
				if (DbGame.LoadComplete(0))
				{
					setStateNext();
				}
				break;

			case GAME_LOAD_ROBOT:
			case GAME_RELOAD_ROBOT:
				DbGame.LoadRobotGame();
				setStateNext();
				break;

			case GAME_LOAD_ROBOT_WAITING:
			case GAME_RELOAD_ROBOT_WAITING:
				if (DbGame.LoadRobotComplete())
				{
					setStateNext();
				}
				break;

			case GAME_LOAD_PLAYER1:
			case GAME_LOAD_PLAYER2:
			case GAME_RELOAD_PLAYER1:
			case GAME_RELOAD_PLAYER2:
				DbGame.LoadGames(eState.getPlayer());
				setStateNext();
				break;

			case GAME_LOAD_PLAYER1_WAITING:
			case GAME_LOAD_PLAYER2_WAITING:
			case GAME_RELOAD_PLAYER1_WAITING:
			case GAME_RELOAD_PLAYER2_WAITING:
				if (DbGame.LoadComplete(eState.getPlayer()))
				{
					setStateNext();
				}
				break;

			case GAME_OVER:
				// If the other player went out first, they make the decission to play again.
				if (m_dbGame != null &&
					m_dbGame.getPlayerTurn() < 0 &&
					m_dbGame.getPlayerTurn() != -1 - m_dbGame.getPlayer())
				{
					setState(State.GAME_OVER_ALERT);
					break;
				}
				// fallthru

			case GAME_OVER_ALERT:
			case GAME_OVER_MENU:
				setStatus(" ");
				m_eStateParent = eStateLast;
				break;

			case GAME_OVER_QUIT:
				setStatus("Opponent quit the game");
				break;

			case GAME_OVER_REPLAY:
				setStatus("Play again?");
				m_eStateParent = eStateLast;
				break;

			case GAME_QUIT:
				setStatus("Confirm to quit the game?");
				m_eStateParent = eStateLast;
				break;

			case GAME_QUIT_ROBOT:
				setStatus("Confirm to quit robot game?");
				m_eStateParent = eStateLast;
				break;

			case GAME_SELECT:
				// Make sure each game is fully loaded, including the opponent name.
				for (int iGame = 0; iGame < m_llGames.size(); ++iGame)
				{
					// Wait until all the opponents are known.
					if (getGameOpponentName(iGame) == null)
					{
						return;
					}
				}
				setStateNext();
				break;

			case GAME_SELECT_WAITING:
				setStatus(null);
				m_iGame = -1;
				break;

			case GAME_SELECTED:
				// Make sure a game has been selected.
				if (m_iGame < 0)
				{
					setState(State.GAME_SELECT_WAITING);
					break;
				}

				// If we have this game loaded, resume playing.
				if (m_iGame < m_llGames.size())
				{
					m_game = m_llGames.get(m_iGame).get(0);
					m_dbGame = m_game.getDbGame();
					if (m_dbGame != null)
					{
						m_iPlayer = m_dbGame.getPlayer();
					}
					setState(State.GAME_RESUME);
					break;
				}

				if (m_iGame == m_llGames.size())
				{
					setState(State.GAME_INVITE);
					break;
				}

				// Trigger the load of a random opponent game and check for it to complete.
				setState(State.GAME_CREATE);
				break;

			case GAME_RESUME:
				// If the game has been quit by the other player, wait for aknowledgement and delete.
				if (m_game.isQuit())
				{
					setState(State.GAME_OVER_QUIT);
					break;
				}
				// If the resumed game is over, delete this and start a new one.
				if (m_game.isOver())
				{
					setState(State.GAME_OVER);
					break;
				}
				//fallthru;

			case GAME_START:
				// If the game has been quit by the other player, wait for aknowledgement and delete.
				if (m_game.isQuit())
				{
					setState(State.GAME_OVER_QUIT);
					break;
				}
				// If the resumed game is over, delete this and start a new one.
				if (m_game.isOver())
				{
					m_game.reset();
				}
				// Make sure we are assigned to the correct player.
				if (m_dbGame != null)
				{
					m_iPlayer = m_dbGame.getPlayer();
				}
				m_game.updateState();
				setStateNext();
				break;

			case TURN_BESTWORD:
				m_eStateParent = eStateLast;
				break;

			case TURN_DRAW:
				if (getScore(m_iPlayer) == 0 &&
					m_game.isRackEmpty(m_iPlayer))
				{
					setState(State.TURN_DRAW_FIRST);
					break;
				}
				if (m_game.isRackFilled(m_iPlayer) ||
					(!haveTiles() && !haveVowels()))
				{
					setStatus(null);
					m_game.findBestBoard(m_iPlayer);
					setState(State.TURN_PLACE);
					break;
				}
				m_eStateParent = eStateLast;
				setStatus("Draw more tiles");
				break;

			case TURN_DRAW_FIRST:
				setStatus("Draw vowels and consonants");
				m_eStateParent = eStateLast;
				break;

			case TURN_FIRST:
				// If the idiot picked all consonates, check for an auto-pass.
				if (m_game.isNoPlay())
				{
					m_mainActivity.setAlert("No place to go. Passing...");
					m_game.skipTurn();
					setState(State.TURN_START);
					break;
				}
				setStatus("Place word to touch center");
				m_eStateParent = eStateLast;
				break;

			case TURN_INVALID:
				setStatus("Not all words valid");
				m_eStateParent = eStateLast;
				break;

			case TURN_PLACE:
				// Wait for our turn to be able to place tiles.
				if (m_iPlayer != m_game.getPlayer())
				{
					setState(State.TURN_WAITING);
					break;
				}

				// If there are no valid words to play, pass the player's turn.
				if (m_game.isNoPlay())
				{
					m_mainActivity.setAlert("No place to go. Passing...");
					m_game.skipTurn();
					setState(State.TURN_START);
					break;
				}

				// Ignore if we are already in this state.
				if (eState == eStateLast)
				{
					break;
				}

				// If our board is empty, switch states to show the user where to start.
				if (m_game.isEmpty())
				{
					setState(State.TURN_FIRST);
					break;
				}

				// If not told yet, let the user know to fling up connecting tiles.
				if (!m_bFlingMessage && isTilePlaced())
				{
					m_bFlingMessage = true;
					setStatus("Fling up connecting tiles");
				}
				else
				{
					setStatus("Build a crossword");
				}
				// fallthru;
			case TURN_PLAY:
				m_eStateParent = eStateLast;
				// If the tiles have been placed into a potential word, switch states.
				if (m_game.getPlayStatus() == PlayStatus.VALID)
				{
					setState(State.TURN_READY);
				}
				break;

			case TURN_READY:
				m_eStateParent = eStateLast;
				setStatus("Press play when ready");
				break;

			case TURN_ROBOT:
				moveRobot();
				setStateNext();
				break;

			case TURN_SKIP:
				setStatus("Confirm to pass your turn?");
				m_eStateParent = eStateLast;
				break;

			case TURN_START:
				if (m_game.isOver())
				{
					setState(State.GAME_OVER);
					break;
				}
				if (m_game.isPlayerRobot())
				{
					setState(State.TURN_ROBOT);
					break;
				}
				if (m_iPlayer != m_game.getPlayer())
				{
					setState(State.TURN_WAITING);
					break;
				}
				if (!isGameTimed())
				{
					setState(State.TURN_DRAW);
					break;
				}
				m_eStateParent = eStateLast;
				setStatus("Press play when ready");
				break;

			case TURN_VERIFY:
				if (m_dbGame == null ||
					m_dbGame.isPushed() ||
					m_game.isRobot())
				{
					m_game.confirmWord();
					setStateNext();
					break;
				}
				if (m_dbGame.isFailure())
				{
					m_game.postMove(m_iPlayer);
				}
				// fallthru;

			case TURN_WAITING:
				if (m_game.isRackFilled(m_iPlayer) ||
					(!haveTiles() && !haveVowels()))
				{
					setStatus("Waiting for opponent...");
				}
				else
				{
					setStatus("Draw more tiles");
				}
				m_eStateParent = eStateLast;
				break;

			case USER_BLOCK:
				setStatus("Block games from this user?");
				m_eStateParent = eStateLast;
				break;

			case USER_DELETE_WAITING:
				if (m_dbUser.isDeleted())
				{
					setStateNext();
				}
				break;

			case USER_EMAIL:
			case USER_LOGIN:
			case USER_MESSAGE:
			case USER_REGISTER_WAITING:
			case USER_SETUP:
			case USER_WAITING:
				break;

			case USER_WELCOME:
			case USER_WELCOME_WAITING:
				m_eStateParent = eStateLast;
				setStatus(null);
				break;
		}
	}

	//**********************************************************************************************
	public void
	setStateNext()
	{
		setState(m_eState.getNext());
	}

	//**********************************************************************************************
	public void
	setStatus(String strStatus)
	{
		m_strStatus = strStatus;
	}

	//**********************************************************************************************
	private void
	setUserDelete(Map<String, Object> mData)
	{
		// If we are already logged in, try delete the account.
		if (mData == null)
		{
			m_dbUser.userDelete();
		}
		else
		{
			// Get the login credentials.
			String strEmail = (String)mData.get("strEmail");
			String strPassword = (String)mData.get("strPassword");

			// Make sure these are not empty.
			if (TextUtils.isEmpty(strEmail) || TextUtils.isEmpty(strPassword))
			{
				setStatus("Provide email and Jot password");
				return;
			}
			m_dbUser.userDelete(strEmail, strPassword);
		}
	}

	//**********************************************************************************************
	public void
	setUserDeleted()
	{
		m_game.delete();
		setState(State.USER_DELETED);
	}

	//**********************************************************************************************
	public void
	setUserDetails(Map<String, Object> mData)
	{
		if (haveUser())
		{
			String strNickname = (String)mData.get("strNickname");
			if (!m_dbUser.setUserName(strNickname))
			{
				setStatus("There's only one, JotRussell");
			}
		}
	}

	//**********************************************************************************************
	public void
	setUserFailed()
	{
		setState(State.USER_WELCOME);
	}

	//**********************************************************************************************
	private void
	setUserInvite(Map<String, Object> mData)
	{
		// Get the friend's email address.
	    m_strEmail = (String)mData.get("strEmail");
		m_strNewOpponentId = m_dbUser.userInvite(m_strEmail);
	}

	//**********************************************************************************************
	public void
	setUserLoaded()
	{
		// Don't leave the welcome state until the user presses play.
		switch (m_eState)
		{
			case USER_WELCOME:
				// Trigger the initial search for our games list.
				setState(State.GAME_LOAD);
				break;

			case GAME_INVITE:
			case GAME_INVITE_WAITING:
				setState(m_eState);
				break;
		}
	}

	//**********************************************************************************************
	public void
	setUserLogin()
	{
		switch (m_eState)
		{
			case USER_LOGIN:
			case USER_REGISTER_WAITING:
			case USER_WAITING:
			case USER_WELCOME:
				setState(State.GAME_LOAD);
				break;
		}
	}

	//**********************************************************************************************
	public void
	setUserLogin(Map<String, Object> mData)
	{
		// Get the login credentials.
		String strEmail = (String)mData.get("strEmail");
		String strPassword = (String)mData.get("strPassword");
		String strNickname = (String)mData.get("strNickname");

		// Make sure these are not empty.
		if (TextUtils.isEmpty(strEmail) || TextUtils.isEmpty(strPassword))
		{
			setStatus("Provide user email and Jot password");
			setState(State.USER_EMAIL);
			return;
		}

		m_dbUser.userLogin(strEmail, strPassword, strNickname);
		setState(State.USER_WAITING);
	}

	//**********************************************************************************************
	public void
	setUserLogin(AccessToken accessToken)
	{
		m_dbUser.userLogin(accessToken);
		setState(State.USER_WAITING);
	}

	//**********************************************************************************************
	public void
	setUserLogin(GoogleSignInAccount account)
	{
		m_dbUser.userLogin(account);
		setState(State.USER_WAITING);
	}

	//**********************************************************************************************
	public void
	setUserLogin(FirebaseAuth fbAuth)
	{
		m_dbUser.userLogin(fbAuth);
		setState(State.USER_WAITING);
	}

	//**********************************************************************************************
	public void
	setUserRegister()
	{
		setState(State.USER_WELCOME);
	}

	//**********************************************************************************************
	public void
	setUserRegister(Map<String, Object> mData)
	{
		// Get the login credentials.
		String strEmail = (String)mData.get("strEmail");
		String strPassword = (String)mData.get("strPassword");
		String strNickname = (String)mData.get("strNickname");

		// Make sure these are not empty.
		if (TextUtils.isEmpty(strEmail) || TextUtils.isEmpty(strPassword))
		{
			setStatus("Provide email and Jot password");
			setState(State.USER_EMAIL);
			return;
		}

		m_dbUser.userRegister(strEmail, strPassword, strNickname);
		setState(State.USER_REGISTER_WAITING);
	}

	//**********************************************************************************************
	public void
	setUserReset(Map<String, Object> mData)
	{
		// Get the login credentials.
		String strEmail = (String)mData.get("strEmail");

		// Make sure these are not empty.
		if (TextUtils.isEmpty(strEmail))
		{
			setStatus("Provide your user email");
			setState(State.USER_EMAIL);
			return;
		}

		m_dbUser.userReset(strEmail);
		setState(State.USER_WELCOME);
	}

	//**********************************************************************************************
	public void
	updateState()
	{
		// Avoid updates while the robot is working.
		if (m_mainActivity.isInThread())
		{
			return;
		}

		switch (m_eState)
		{
			case GAME_CREATE:
			case GAME_CREATE_WAITING:
			case GAME_INVITE:
			case GAME_INVITE_WAITING:
			case GAME_LOAD:
			case GAME_LOAD_GAME:
			case GAME_LOAD_GAME_WAITING:
			case GAME_LOAD_ROBOT:
			case GAME_LOAD_ROBOT_WAITING:
			case GAME_LOAD_PLAYER1:
			case GAME_LOAD_PLAYER1_WAITING:
			case GAME_LOAD_PLAYER2:
			case GAME_LOAD_PLAYER2_WAITING:
			case GAME_RELOAD:
			case GAME_RELOAD_ROBOT:
			case GAME_RELOAD_ROBOT_WAITING:
			case GAME_RELOAD_PLAYER1:
			case GAME_RELOAD_PLAYER1_WAITING:
			case GAME_RELOAD_PLAYER2:
			case GAME_RELOAD_PLAYER2_WAITING:
			case GAME_SELECT:
			case GAME_SELECT_WAITING:
			case GAME_SELECTED:
			case TURN_VERIFY:
				// Add a short delay to allow other processes to check for load completion.
				try
				{
					Thread.sleep(10);
				}
				catch (Exception e)
				{
				}
				setState(m_eState);
				return;
		}

		if (m_game != null && m_iGame >= 0)
		{
			// Update the game state.  If the user just went, update our best board.
			int iPlayer = m_game.getPlayer();
			m_game.updateState();
			if (iPlayer != m_game.getPlayer() &&
				m_game.isRackFilled(m_iPlayer))
			{
				m_game.findBestBoard(m_iPlayer);
			}

			// If the opponent is low on tiles, pop up an alert.
			String strAlert = m_game.getTilesLeftAlert(m_iPlayer ^ 1);
			if (strAlert != null)
			{
				m_mainActivity.setAlert(strAlert);
			}

			// Switch the main state based on the game state.
			switch (m_game.getState())
			{
				case PLACE_TILES:
					if (m_eState == State.TURN_READY)
					{
						setState(State.TURN_PLACE);
					}
					break;

				case DRAW_TILES:
				case FIRST_WORD:
				case PLAY_TILES:
				case MOVE_ROBOT:
					break;

				case PLAY_READY:
					setState(State.TURN_READY);
					break;

				case OVER:
					switch (m_eState)
					{
						case USER_WELCOME:
						case USER_WELCOME_WAITING:
							if (m_game.isRobot())
							{
								m_game.reset();
							}
							break;

						case GAME_OVER:
						case GAME_OVER_ALERT:
						case GAME_OVER_MENU:
						case GAME_OVER_REPLAY:
							break;

						default:
							setState(State.GAME_OVER);
					}
			}

			switch (m_eState)
			{
				case TURN_WAITING:
					if (m_iPlayer == m_game.getPlayer() ||
						m_game.isOver())
					{
						setState(State.TURN_START);
					}
					break;
			}
		}
	}
}

//**************************************************************************************************
