/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Database access to netword games.
 */

package com.jot.jot.db;

import com.google.firebase.Timestamp;
import com.jot.decomp.Taboo;
import com.jot.jot.android.MainActivity;
import com.jot.jot.crossword.Main;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

//**************************************************************************************************

public class DbGame
{
	private static final String s_strDbTableName = "games";
	private static CollectionReference s_dbRefGames = null;
	private static final Map<String, List<DbGame>> s_mDbGames = new HashMap<>();
	private static final Map<String, DbUser> s_mDbOpponents = new HashMap<>();
	private static final Set<String> s_sGamesLoaded = new HashSet<>();
	private static int s_nGamesLoaded = 0;
	private static int s_nGamesLoading = 0;
	private static int s_nGamesWaiting = 0;
	private static DbUser s_dbUser = null;
	private static DbUser s_dbRobot = null;
	private static boolean s_bLoading = false;
	private static boolean s_bOpponentSearching = false;
	private static Main s_main = null;
	private static MainActivity s_mainActivity;
	private static int s_iPlayer = 0;
	private static int s_nQueued = 0;

	//**********************************************************************************************
	private enum DbGameField
	{
		bmSettings("bmSettings"),
		iLastSquare1("iLastSquare1"),
		iLastSquare2("iLastSquare2"),
		iTurn("iTurn"),
		nLastScore1("nLastScore1"),
		nLastScore2("nLastScore2"),
		nScore1("nScore1"),
		nScore2("nScore2"),
		strInvalids("strInvalids"),
		strMove("strMove"),
		strRack1("strRack1"),
		strRack2("strRack2"),
		strUserId1("strUserId1"),
		strUserId2("strUserId2"),
		strUserMsg1("strUserMsg1"),
		strUserMsg2("strUserMsg2"),
		timeStamp("timeStamp");

		private final String m_strEnum;

		public static DbGameField
		GetRackEnum(int iPlayer)
		{
			if (iPlayer == 0)
			{
				return strRack1;
			}
			return strRack2;
		}

		public static DbGameField
		GetScoreEnum(int iPlayer)
		{
			if (iPlayer == 0)
			{
				return nScore1;
			}
			return nScore2;
		}

		public static DbGameField
		GetUserIdEnum(int iPlayer)
		{
			if (iPlayer == 0)
			{
				return strUserId1;
			}
			return strUserId2;
		}

		public static DbGameField
		GetUserMsgEnum(int iPlayer)
		{
			if (iPlayer == 0)
			{
				return strUserMsg1;
			}
			return strUserMsg2;
		}

		DbGameField(final String strEnum)
		{
			m_strEnum = strEnum;
		}

		@Override
		public String
		toString()
		{
			return m_strEnum;
		}
	}

	//**********************************************************************************************

	private final DbGame m_dbGame;
	private DbUser m_dbOpponent;
	private final DbUser m_dbUser;
	private final DbUser[] m_aDbUsers = new DbUser[2];
	private final DocumentReference m_docRef;
	private String m_strInvalids;
	private final Set<String> m_sInvalids;
	private final String m_strGameId;
	private String m_strMove;
	private final String[] m_aRacks = new String[2];
	private final String[] m_aUserIds = new String[2];
	private final String[] m_aUserMsgs = new String[2];
	private boolean m_bPlayed;
	private boolean m_bRobotGame;
	private boolean m_bUpdated;
	private int m_bmSettings;
	private final int m_iPlayer;
	private int m_iTurn;
	private Date m_date;
	private final int[] m_aLastScores = new int[2];
	private final int[] m_aLastSquares = new int[2];
	private final int[] m_aScores = new int[2];

	//**********************************************************************************************
	private static int
	GetIntValue(
		DbGameField eField,
		Map<String, Object> mFields)
	{
		int iValue = 0;
		Long lValue = (Long)mFields.get(eField.toString());
		if (lValue != null)
		{
			iValue = lValue.intValue();
		}
		return iValue;
	}

	//**********************************************************************************************
	public static void
	Initialize(
		Main main,
		MainActivity mainActivity,
		DbUser dbUser)
	{
		s_main = main;
		s_mainActivity = mainActivity;
		s_dbUser = dbUser;
		s_dbRobot = DbUser.GetDbUser(DbUser.GetRobotUserId(), null);
		s_dbRefGames = DbAccess.GetCollectionReference(s_strDbTableName);
	}

	//**********************************************************************************************
	public static void
	CreateRandomGame()
	{
		// Trigger a search for a random opponent.
		s_bOpponentSearching = true;
		s_nGamesLoading = s_nGamesLoaded;
		s_dbUser.findRandomOpponent(s_mDbOpponents);
	}

	//**********************************************************************************************
	private static void
	CreateRobotGame()
	{
		String strGameId = PostGame(s_dbUser, s_dbRobot);
		DbGame dbGame = new DbGame(strGameId, s_dbUser, s_dbRobot, null);
		List<DbGame> lDbGames = new ArrayList<>();
		lDbGames.add(dbGame);
		s_mDbGames.put(s_dbRobot.getUserId(), lDbGames);
	}

	//**********************************************************************************************
	public static boolean
	HaveGameWith(String strUserId)
	{
		if (s_mDbOpponents != null)
		{
			return s_mDbOpponents.containsKey(strUserId);
		}
		return false;
	}

	//**********************************************************************************************
	public static boolean
	LoadComplete(int iPlayer)
	{
		// Add a short delay to allow other processes to check for load completion.
		try
		{
			Thread.sleep(10);
		}
		catch (Exception e)
		{
		}

		// If we're still loading the games, return false.
		if (s_bLoading)
		{
			return false;
		}
		
		// Continue to wait for all the games are completely loaded.
		if (s_nGamesLoaded < s_nGamesLoading)
		{
			return false;
		}

		// This group of games has been loaded.
		return true;
	}

	//**********************************************************************************************
	public static void
	LoadGame(
		int iPlayer,
		String strOpponentId)
	{
		s_bLoading = true;
		s_iPlayer = iPlayer;

		// Create a listener for our opponent.
		CollectionReference games = DbAccess.GetCollectionReference(s_strDbTableName);
		try
		{
			games
				.whereEqualTo(DbGameField.GetUserIdEnum(iPlayer).toString(), s_dbUser.getUserId())
				.whereEqualTo(DbGameField.GetUserIdEnum(iPlayer ^ 1).toString(), strOpponentId)
				.get()
				.addOnSuccessListener(
					querySnapshot ->
					{
						LoadGames(querySnapshot, s_iPlayer, true);
						s_bLoading = false;
						s_mainActivity.updateState(false);
					}
				)
				.addOnFailureListener(
					e ->
					{
						s_mainActivity.failMessage(
							"LoadGame() onFailure(): " + e.getMessage());
						s_bLoading = false;
					}
				);
		}
		catch (Exception e)
		{
		}
	}

	//**********************************************************************************************
	public static void
	LoadGames(int iPlayer)
	{
		s_bLoading = true;
		s_iPlayer = iPlayer;

		// Create a listener for our opponent.
		CollectionReference games = DbAccess.GetCollectionReference(s_strDbTableName);
		try
		{
			games
				.whereEqualTo(DbGameField.GetUserIdEnum(s_iPlayer).toString(), s_dbUser.getUserId())
				.get()
				.addOnSuccessListener(
					querySnapshot ->
					{
						LoadGames(querySnapshot, s_iPlayer, true);
						s_bLoading = false;
						s_mainActivity.updateState(false);
					
/* SPK: check for DEBUG mode.
					System.out.println(
						"************************ LoadGames.onSuccess() complete " +
						s_nGamesLoaded + " out of " + s_mDbGames.size());
*/
					}
				)
				.addOnFailureListener(
					e ->
					{
						s_mainActivity.failMessage(
							"LoadGames() onFailure(): " + e.getMessage());
						s_bLoading = false;
					}
				);
		}
		catch (Exception e)
		{
		}
	}

	//**********************************************************************************************
	public static boolean
	LoadRandomComplete()
	{
		// Add a short delay to allow other processes to check for load completion.
		try
		{
			Thread.sleep(10);
		}
		catch (Exception e)
		{
		}

		// If we haven't posted the game yet, see if we first found our opponent.
		if (s_bOpponentSearching)
		{
			DbUser dbOpponent = s_dbUser.getOpponent();
			if (dbOpponent != null)
			{
				if (dbOpponent != s_dbRobot)
				{
					PostGame(s_dbUser, dbOpponent);
				}
				LoadGames(0);
				s_bOpponentSearching = false;
			}
			return false;
		}

		// If we're still loading the game, return false.
		if (s_bLoading)
		{
			return false;
		}

		// Continue to wait until the game is completely loaded.
		if (s_nGamesLoaded < s_nGamesLoading)
		{
			return false;
		}

		// We should be done loading the new random game.
		return true;
	}

	//**********************************************************************************************
	public static boolean
	LoadRobotComplete()
	{
		// Add a short delay to allow other processes to check for load completion.
		try
		{
			Thread.sleep(10);
		}
		catch (Exception ignored)
		{
		}

		// If we're still loading the games, return false.
		if (s_bLoading)
		{
			return false;
		}
		
		// Continue to wait for all the games are completely loaded.
		if (s_nGamesLoaded < s_nGamesLoading)
		{
			return false;
		}

		// If we don't have it, load our default robot game.
		if (s_mDbGames.get(s_dbRobot.getUserId()) == null)
		{
			CreateRobotGame();
			return false;
		}

		// We have our robot game.
		return true;
	}

	//**********************************************************************************************
	public static void
	LoadRobotGame()
	{
		s_bLoading = true;
		s_iPlayer = 0;

		// If we are starting the load from first robot game, clear previous loads.
		s_mDbGames.clear();
		s_mDbOpponents.clear();
		s_sGamesLoaded.clear();
		
		// Create a listener for our opponent.
		CollectionReference games = DbAccess.GetCollectionReference(s_strDbTableName);
		try
		{
			games
				.whereEqualTo(DbGameField.strUserId1.toString(), s_dbUser.getUserId())
				.whereEqualTo(DbGameField.strUserId2.toString(), s_dbRobot.getUserId())
				.get()
				.addOnSuccessListener(
					querySnapshot ->
					{
						LoadGames(querySnapshot, s_iPlayer, false);
						s_bLoading = false;
						s_mainActivity.updateState(false);

/* SPK: check for DEBUG mode.
					System.out.println(
						"************************ LoadGames.onSuccess() complete " +
						s_nGamesLoaded + " out of " + s_mDbGames.size());
*/
					}
				)
				.addOnFailureListener(
					e ->
					{
						s_mainActivity.failMessage(
							"LoadRobotGame() onFailure(): " + e.getMessage());
						s_bLoading = false;
					}
				);
		}
		catch (Exception e)
		{
		}
	}

	//**********************************************************************************************
	private static void
	LoadGames(
		QuerySnapshot querySnapshot,
		int iPlayer,
		boolean bBlockRobot)
	{
		String strOpponentField = DbGameField.GetUserIdEnum(iPlayer ^ 1).toString();

		// For now, run a brute force search for our id.
		List<DocumentSnapshot> lSnapshots = querySnapshot.getDocuments();
		int nGames = lSnapshots.size();

		for (int iGame = 0; iGame < nGames; ++iGame)
		{
			DocumentSnapshot snapshot = lSnapshots.get(iGame);

			// If we have already loaded this game, skip it.
			String strGameId = snapshot.getId();
			if (s_sGamesLoaded.contains(strGameId))
			{
				continue;
			}
			
			// Ignore if our fields are null.
			Map<String, Object> mFields = snapshot.getData();
			if (mFields == null)
			{
				continue;
			}

			// Get the opponent id.
			String strOpponentId = (String)mFields.get(strOpponentField);

			// If we're blocking robot games to be loaded, skip over them.
			if (bBlockRobot)
			{
				if (s_dbRobot.getUserId().equals(strOpponentId))
				{
					continue;
				}
			}

			// Get the document reference in case we need to delete the game.
			DocumentReference docRef = s_dbRefGames.document(strGameId);

			// Delete erroneous games we have already blocked.
			if (s_dbUser.isBlocked(strOpponentId))
			{
				DbAccess.Delete(docRef);
				continue;
			}

			// Delete games that are more than 28 days old.
			Date dateLast;
			Timestamp timeLast = (Timestamp)mFields.get(DbGameField.timeStamp.toString());
			if (timeLast == null)
			{
				dateLast = new Date();
			}
			else
			{
				dateLast = timeLast.toDate();
			}
			Date dateOld = new Date(System.currentTimeMillis() - (28L * 24 * 60 * 60 * 1000));
			if (dateLast.before(dateOld))
			{
				DbAccess.Delete(docRef);
				continue;
			}

			// Ignore games that are over which we have already seen its outcome.
			int iTurn = GetIntValue(DbGameField.iTurn, mFields);
			if (iTurn == -1 - iPlayer)
			{
				continue;
			}
				
			// Map each list of games by the opponent id.
			List<DbGame> lDbGames = s_mDbGames.get(strOpponentId);
			if (lDbGames == null)
			{
				// Create the array list for this opponent.
				lDbGames = new ArrayList<>();
				s_mDbGames.put(strOpponentId, lDbGames);
			}
			// Make sure we haven't already loaded this game.
			else
			{
				boolean bFound = false;
				for (DbGame dbTest : lDbGames)
				{
					if (strGameId.equals(dbTest.getGameId()))
					{
						bFound = true;
						break;
					}
				}
				if (bFound)
				{
					continue;
				}
			}

			// Try to get the DbUser for our opponent.
			DbUser dbOpponent;
			if (s_dbRobot.getUserId().equals(strOpponentId))
			{
				dbOpponent = s_dbRobot;
			}
			else
			{
				dbOpponent = s_mDbOpponents.get(strOpponentId);
			}
			if (dbOpponent == null)
			{
				// Create a new DbUser for our opponent and add it to our map.
				dbOpponent = DbUser.GetDbUser(strOpponentId, null);
				s_mDbOpponents.put(strOpponentId, dbOpponent);
			}

			// Create a new database connection for this game.
			DbGame dbGame = new DbGame(strGameId, s_dbUser, dbOpponent, mFields);

			// Add this game to our active set.
			s_sGamesLoaded.add(strGameId);

			// If this game is marked for deletion, make sure the user gets to see
			// the outcome by listing it first.
			int iIndex = lDbGames.size();
			if (iTurn < 0)
			{
				iIndex = 0;
			}
			// Else, if there are multiple games with the same player, push the most recent first.
			else if (iIndex > 0)
			{
				iIndex = 0;
				for (DbGame dbTest : lDbGames)
				{
					if (dbTest.getPlayerTurn() >= 0 && dateLast.before(dbTest.getDate()))
					{
						break;
					}
					++iIndex;
				}
			}

			// Add the game to the list.
			lDbGames.add(iIndex, dbGame);
		}
	}

	//**********************************************************************************************
	public static String
	PostGame(DbGame dbGame)
	{
		return PostGame(
			dbGame.getPlayerId(),
			dbGame.getOpponentId(),
			dbGame.getSettings());
	}

	//**********************************************************************************************
	public static String
	PostGame(DbUser dbUser1, DbUser dbUser2)
	{
		return PostGame(
			dbUser1.getUserId(),
			dbUser2.getUserId(),
			dbUser1.getSettings() & dbUser2.getSettings());
	}

	//**********************************************************************************************
	private static String
	PostGame(
		String strUserId1,
		String strUserId2,
		int bmSettings)
	{
		// Indicate we have another game we are waiting to load.
		++s_nGamesWaiting;
		if (s_nGamesLoading == 0)
		{
			s_nGamesLoading = s_nGamesLoaded;
		}
		++s_nGamesLoading;

		// Create a new document to be added to the games table.
		Map<String, Object> mGame = new HashMap<>();
		mGame.put(DbGameField.bmSettings.toString(), (long)bmSettings);
		mGame.put(DbGameField.iTurn.toString(), (long)0);
		mGame.put(DbGameField.iLastSquare1.toString(), (long)0);
		mGame.put(DbGameField.iLastSquare2.toString(), (long)0);
		mGame.put(DbGameField.nLastScore1.toString(), (long)0);
		mGame.put(DbGameField.nLastScore2.toString(), (long)0);
		mGame.put(DbGameField.nScore1.toString(), (long)0);
		mGame.put(DbGameField.nScore2.toString(), (long)0);
		mGame.put(DbGameField.strInvalids.toString(), "");
		mGame.put(DbGameField.strMove.toString(), "");
		mGame.put(DbGameField.strRack1.toString(), "");
		mGame.put(DbGameField.strRack2.toString(), "");
		mGame.put(DbGameField.strUserId1.toString(), strUserId1);
		mGame.put(DbGameField.strUserId2.toString(), strUserId2);
		mGame.put(DbGameField.strUserMsg1.toString(), "");
		mGame.put(DbGameField.strUserMsg2.toString(), "");
		mGame.put(DbGameField.timeStamp.toString(), Timestamp.now());

		// Asynchronously add our new game.
		return DbAccess.AddData(s_strDbTableName, mGame);
	}

	//**********************************************************************************************
	public
	DbGame(
		String strGameId,
		DbUser dbUser,
		DbUser dbOpponent,
		Map<String, Object> mFields)
	{
		// Once this game is fully loaded, send it over to Main.
		m_dbGame = this;

		// Store our game id and use this to get a reference to the database entry.
		m_strGameId = strGameId;
		m_docRef = s_dbRefGames.document(strGameId);

		// Assign our document details based on the data returned.
		if (mFields != null)
		{
			initialize(mFields);
			m_iPlayer = dbUser.getUserId().equals(m_aUserIds[0]) ? 0 : 1;
		}
		else
		{
			m_iPlayer = 0;
		}

		// Determine if we're the first or second player.
		m_dbUser = dbUser;
		m_dbOpponent = dbOpponent;
		m_aDbUsers[m_iPlayer] = dbUser;
		m_aDbUsers[m_iPlayer ^ 1] = dbOpponent;

		// Create a set for managing invalids.
		m_sInvalids = new TreeSet<>();

		// The second player is the robot, mark it as such.
		if (dbOpponent == s_dbRobot)
		{
			m_bRobotGame = true;
		}
		m_bPlayed = false;

		// Try to load our opponent from the database.
		getOpponent();

		m_docRef
			.get()
			.addOnCompleteListener(
				task ->
				{
					boolean bAdded = false;
					if (task.isSuccessful())
					{
						DocumentSnapshot docSnapshot = task.getResult();
						if (docSnapshot.exists())
						{
							initialize(docSnapshot.getData());
							while (++s_nQueued > 1)
							{
								try
								{
									Thread.sleep(1);
								}
								catch (Exception ignored)
								{
								}
								--s_nQueued;
							}
							bAdded = s_main.addDbGame(m_dbGame);
							--s_nQueued;
							if (bAdded)
							{
								if (m_iTurn != m_iPlayer)
								{
									m_bPlayed = true;
								}
							}
						}
					}
					else
					{
						s_mainActivity.failMessage(
							"DbGame onComplete() failed: " +
							task.getException());
					}

					// Trigger the first update.
					s_mainActivity.updateState(false);

					// Add our game.
					if (bAdded)
					{
						s_mDbOpponents.put(m_dbOpponent.getUserId(), m_dbOpponent);
						++s_nGamesLoaded;
						if (s_nGamesWaiting > 0)
						{
							--s_nGamesWaiting;
						}

						// Trigger another update.
						s_mainActivity.updateState(false);
					}
				}
			);

		m_docRef
			.addSnapshotListener(
				(docSnapshot, e) ->
				{
					if (e != null)
					{
						return;
					}

					if (docSnapshot != null && docSnapshot.exists())
					{
						initialize(docSnapshot.getData());
						if (!m_bRobotGame &&
							m_iTurn == m_iPlayer)
						{
							if (m_bPlayed)
							{
								m_bPlayed = false;
								String strNote =
									getOpponentName() + " played you.";
								s_mainActivity.setNotification(strNote);
							}
						}
					}
					s_mainActivity.updateState(true);
				}
			);
	}

	//**********************************************************************************************
	public void
	blockOpponent()
	{
		m_dbUser.blockOpponent(m_dbOpponent);
	}

	//**********************************************************************************************
	public void
	clearInvalids()
	{
		m_sInvalids.clear();
	}

	//**********************************************************************************************
	public void
	delete()
	{
		// No need to delete robot games.  It'll just be reset.
		if (isRobot())
		{
			return;
		}

		// Delete this from our static list of open games.
		String strOpponentId = getOpponentId();
		List<DbGame> lDbGames = s_mDbGames.get(strOpponentId);
		if (lDbGames != null)
		{
			lDbGames.remove(this);
			if (lDbGames.size() == 0)
			{
				s_mDbGames.remove(strOpponentId);
				s_mDbOpponents.remove(strOpponentId);
			}
		}

		// Have both players notified of the results before the game is deleted.
		if (m_iTurn >= 0)
		{
			m_iTurn = -1 - m_iPlayer;
			Map<String, Object> mFields = new HashMap<>();
			mFields.put(DbGameField.iTurn.toString(), (long)m_iTurn);
			DbAccess.MergeData(s_strDbTableName, m_strGameId, mFields);
			return;
		}

		// If we already marked this game for deletion, wait for the other user to delete it.
		if (m_iTurn == -1 - m_iPlayer)
		{
			return;
		}

		// Else, delete the document.
		DbAccess.Delete(m_docRef);
	}

	//**********************************************************************************************
	public Date
	getDate()
	{
		return m_date;
	}

	//**********************************************************************************************
	public String
	getGameId()
	{
		return m_strGameId;
	}

	//**********************************************************************************************
	public int
	getHighScore(int iPlayer)
	{
		return m_aDbUsers[iPlayer].getHighScore();
	}

	//**********************************************************************************************
	public int
	getLastScore(int iPlayer)
	{
		return m_aLastScores[iPlayer];
	}

	//**********************************************************************************************
	public int
	getLastSquare(int iPlayer)
	{
		return m_aLastSquares[iPlayer];
	}

	//**********************************************************************************************
	public String
	getInvalids()
	{
		// If we have new invalids from the other player, wipe and return them.
		if (m_strInvalids != null &&
			!m_strInvalids.isEmpty() &&
			m_strInvalids.charAt(0) != '0' + m_iPlayer)
		{
			// Save a copy for return before clearing our local copy.
			String strInvalids = m_strInvalids.substring(1);
			m_strInvalids = null;
			return strInvalids;
		}
		return null;
	}

	//**********************************************************************************************
	public String
	getMove()
	{
		return m_strMove;
	}

	//**********************************************************************************************
	public DbUser
	getOpponent()
	{
		if (m_dbOpponent == null)
		{
			m_dbOpponent = m_dbUser.getOpponent(m_aUserIds[m_iPlayer ^ 1]);
		}
		return m_dbOpponent;
	}

	//**********************************************************************************************
	public String
	getOpponentId()
	{
		return m_aUserIds[m_iPlayer ^ 1];
	}

	//**********************************************************************************************
	public String
	getOpponentMessage()
	{
		String strMessage = m_aUserMsgs[m_iPlayer ^ 1];
		if (strMessage != null && !strMessage.isEmpty())
		{
			pushMessage(m_iPlayer ^ 1, "");
		}
		return strMessage;
	}

	//**********************************************************************************************
	public String
	getOpponentName()
	{
		return m_aDbUsers[m_iPlayer ^ 1].getUserName();
	}

	//**********************************************************************************************
	public int
	getPlayer()
	{
		return m_iPlayer;
	}

	//**********************************************************************************************
	public String
	getPlayerId()
	{
		return m_aUserIds[m_iPlayer];
	}

	//**********************************************************************************************
	public String
	getPlayerName()
	{
		return m_aDbUsers[m_iPlayer].getUserName();
	}

	//**********************************************************************************************
	public int
	getPlayerTurn()
	{
		return m_iTurn;
	}

	//**********************************************************************************************
	public String
	getRack()
	{
		return m_aRacks[m_iPlayer];
	}

	//**********************************************************************************************
	public String
	getRack(int iPlayer)
	{
		return m_aRacks[iPlayer];
	}

	//**********************************************************************************************
	public int
	getRanking(int iPlayer)
	{
		return m_aDbUsers[iPlayer].getRanking();
	}

	//**********************************************************************************************
	public int
	getScore(int iPlayer)
	{
		return m_aScores[iPlayer];
	}

	//**********************************************************************************************
	public int
	getScoreUser()
	{
		return m_aScores[m_iPlayer];
	}

	//********************************************************************!*************************
	public int
	getScoreOpponent()
	{
		return m_aScores[m_iPlayer ^ 1];
	}

	//**********************************************************************************************
	public int
	getSettings()
	{
		return m_bmSettings;
	}

	//**********************************************************************************************
	public DbUser
	getUser()
	{
		return m_dbUser;
	}

	//**********************************************************************************************
	public String
	getUserId(int iPlayer)
	{
		return m_aUserIds[iPlayer];
	}

	//**********************************************************************************************
	public String
	getUserMessage()
	{
		String strMessage = m_aUserMsgs[m_iPlayer];
		if (strMessage != null && !strMessage.isEmpty())
		{
			pushMessage("");
		}
		return strMessage;
	}

	//**********************************************************************************************
	public String
	getUserName(int iPlayer)
	{
		return m_aDbUsers[iPlayer].getUserName();
	}

	//**********************************************************************************************
	private void
	initialize(Map<String, Object> mFields)
	{
		if (mFields == null)
		{
			return;
		}
		m_aScores[0] = GetIntValue(DbGameField.nScore1, mFields);
		m_aScores[1] = GetIntValue(DbGameField.nScore2, mFields);
		m_aUserIds[0] = (String)mFields.get(DbGameField.strUserId1.toString());
		m_aUserIds[1] = (String)mFields.get(DbGameField.strUserId2.toString());
		m_aUserMsgs[0] = (String)mFields.get(DbGameField.strUserMsg1.toString());
		m_aUserMsgs[1] = (String)mFields.get(DbGameField.strUserMsg2.toString());
		m_bmSettings = GetIntValue(DbGameField.bmSettings, mFields);
		m_iTurn = GetIntValue(DbGameField.iTurn, mFields);
		m_aLastSquares[0] = GetIntValue(DbGameField.iLastSquare1, mFields);
		m_aLastSquares[1] = GetIntValue(DbGameField.iLastSquare2, mFields);
		m_aLastScores[0] = GetIntValue(DbGameField.nLastScore1, mFields);
		m_aLastScores[1] = GetIntValue(DbGameField.nLastScore2, mFields);
		m_strMove = (String)mFields.get(DbGameField.strMove.toString());
		m_aRacks[0] = (String)mFields.get(DbGameField.strRack1.toString());
		m_aRacks[1] = (String)mFields.get(DbGameField.strRack2.toString());
		m_strInvalids = (String)mFields.get(DbGameField.strInvalids.toString());

		// Get the date of the last move.
		Timestamp timeStamp = (Timestamp)mFields.get(DbGameField.timeStamp.toString());
		if (timeStamp == null)
		{
			m_date = new Date();
		}
		else
		{
			m_date = timeStamp.toDate();
		}

		m_bUpdated = true;
	}

	//**********************************************************************************************
	public boolean
	isFailure()
	{
		return DbAccess.IsFailure();
	}

	//**********************************************************************************************
	public boolean
	isMyTurn()
	{
		return m_iTurn == m_iPlayer || m_iTurn == -1 - (m_iPlayer ^ 1);
	}

	//**********************************************************************************************
	public boolean
	isOpponent(String strUserId)
	{
		if (m_dbOpponent != null && strUserId.equals(m_dbOpponent.getUserId()))
		{
			return true;
		}
		return false;
	}

	//**********************************************************************************************
	public boolean
	isPushed()
	{
		return DbAccess.IsSuccess();
	}

	//**********************************************************************************************
	public boolean
	isRobot()
	{
		return s_dbRobot.getUserId().equals(m_aUserIds[1]);
	}

	//**********************************************************************************************
	public boolean
	isUpdated()
	{
		if (m_bUpdated)
		{
			m_bUpdated = false;
			return true;
		}
		return false;
	}

	//**********************************************************************************************
	public void
	pushData()
	{
		Map<String, Object> mFields = new HashMap<>();

		mFields.put(DbGameField.bmSettings.toString(), (long)m_bmSettings);
		mFields.put(DbGameField.iTurn.toString(), (long)m_iTurn);
		mFields.put(DbGameField.iLastSquare1.toString(), (long)m_aLastSquares[0]);
		mFields.put(DbGameField.iLastSquare2.toString(), (long)m_aLastSquares[1]);
		mFields.put(DbGameField.nLastScore1.toString(), (long)m_aLastScores[0]);
		mFields.put(DbGameField.nLastScore2.toString(), (long)m_aLastScores[1]);
		mFields.put(DbGameField.nScore1.toString(), (long)m_aScores[0]);
		mFields.put(DbGameField.nScore2.toString(), (long)m_aScores[1]);
		if (m_sInvalids.isEmpty())
		{
			mFields.put(DbGameField.strInvalids.toString(), "");
		}
		else
		{
			char[] cPlayer = { '0' };
			cPlayer[0] += m_iPlayer;
			mFields.put(
				DbGameField.strInvalids.toString(),
				new String(cPlayer) + m_sInvalids.toString());
			clearInvalids();
		}
		mFields.put(DbGameField.strMove.toString(), m_strMove);
		mFields.put(DbGameField.strRack1.toString(), m_aRacks[0]);
		mFields.put(DbGameField.strRack2.toString(), m_aRacks[1]);
		mFields.put(DbGameField.strUserId1.toString(), m_aUserIds[0]);
		mFields.put(DbGameField.strUserId2.toString(), m_aUserIds[1]);
		mFields.put(DbGameField.strUserMsg1.toString(), m_aUserMsgs[0]);
		mFields.put(DbGameField.strUserMsg2.toString(), m_aUserMsgs[1]);
		mFields.put(DbGameField.timeStamp.toString(), Timestamp.now());

		DbAccess.SetData(s_strDbTableName, m_strGameId, mFields);
	}

	//**********************************************************************************************
	public void
	pushMessage(String strMsg)
	{
		pushMessage(m_iPlayer, strMsg);
	}

	//**********************************************************************************************
	public void
	pushMessage(
		int iPlayer,
		String strMsg)
	{
		if (!Taboo.ContainsTaboo(strMsg))
		{
			m_aUserMsgs[iPlayer] = strMsg;

			Map<String, Object> mFields = new HashMap<>();
			mFields.put(DbGameField.GetUserMsgEnum(iPlayer).toString(), strMsg);
			DbAccess.MergeData(s_strDbTableName, m_strGameId, mFields);
		}
	}

	//**********************************************************************************************
	public void
	pushRack(
		int iPlayer,
		String strRack)
	{
		m_aRacks[iPlayer] = strRack;

		Map<String, Object> mFields = new HashMap<>();
		mFields.put(DbGameField.GetRackEnum(iPlayer).toString(), strRack);
		DbAccess.MergeData(s_strDbTableName, m_strGameId, mFields);
	}

	//**********************************************************************************************
	public void
	setLastScore(
		int iPlayer,
		int nLastScore)
	{
		m_aLastScores[iPlayer] = nLastScore;
	}

	//**********************************************************************************************
	public void
	setLastSquare(
		int iPlayer,
		int iLastSquare)
	{
		m_aLastSquares[iPlayer] = iLastSquare;
	}

	//**********************************************************************************************
	public void
	setInvalids(Set<String> sInvalids)
	{
		m_sInvalids.addAll(sInvalids);
	}

	//**********************************************************************************************
	public void
	setMessage(
		String strMsg,
		int iPlayer)
	{
		if (!Taboo.ContainsTaboo(strMsg))
		{
			m_aUserMsgs[iPlayer] = strMsg;
		}
	}

	//**********************************************************************************************
	public void
	setMove(String strMove)
	{
		m_strMove = strMove;
	}

	//**********************************************************************************************
	public void
	setPlayerTurn(int iTurn)
	{
		m_iTurn = iTurn;
		if (m_iTurn != m_iPlayer)
		{
			m_bPlayed = true;
		}
	}

	//**********************************************************************************************
	public void
	setRack(String strRack)
	{
		m_aRacks[m_iPlayer] = strRack;
	}

	//**********************************************************************************************
	public void
	setRack(
		int iPlayer,
		String strRack)
	{
		m_aRacks[iPlayer] = strRack;
	}

	//**********************************************************************************************
	public void
	setScore(
		int iPlayer,
		int nScore)
	{
		m_aScores[iPlayer] = nScore;
	}
}

//**************************************************************************************************
