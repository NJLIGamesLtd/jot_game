/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Database access to netword games.
 */

package com.jot.jot.db;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.facebook.AccessToken;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.FirebaseMessaging;
import com.jot.jot.android.MainActivity;
import com.jot.jot.crossword.Main;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.security.MessageDigest;
import java.util.*;

//**************************************************************************************************

public class DbUser
{
	public static class Status
	{
		public static int NONE = 0x00;
		public static int HAS_CONNECTION = 0x01;
		public static int HAS_USER = 0x02;
		public static int HAS_ANONYMOUS = 0x04;
		public static int HAS_DOCUMENT = 0x08;
		public static int HAS_FAILURE = 0x10;
		public static int HAS_NAME = 0x20;
	};
	
	public enum State
	{
		INITIAL,
		NO_CONNECTION,
		NO_DOCUMENT,
		USER_ANONYMOUS,
		HAVE_USER,
		READY;
	}

	public enum OpponentState
	{
		USER_NONE,
		USER_START,
		USER_SEARCHING,
		USER_EMPTY,
		USER_SAME,
		USER_BLOCKED,
		USER_ACTIVE,
		USER_INVITE,
		USER_FOUND,
		USER_WAITING;
	}
	
	// Fields must match that in t_user.sql
	private enum DbUserField
	{
		bmSettings("bmSettings"),
		nGames("nGames"),
		nRanking("nRanking"),
		nHighScore("nHighScore"),
		strName("strName"),
		aUsersBlock("aUsersBlock");

		private final String m_strEnum;
		
		DbUserField(final String strEnum)
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

	private static final Map<String, Object> s_mRobotFields = new HashMap<String, Object>()
	{{
		put(DbUserField.bmSettings.toString(), 0L);
		put(DbUserField.nGames.toString(), 1L);
		put(DbUserField.nRanking.toString(), 613L);
		put(DbUserField.nHighScore.toString(), 957L);
		put(DbUserField.strName.toString(), s_strNameRobot);
	}};

	private static final String s_strNameRobot = "JotRussell";
	private static final String s_strNameDefault = "You";
	private static final String s_strNameUnknown = "Anonymous";
	private static final String s_strDbTableName = "users";

	private static Main s_main;
	private static MainActivity s_mainActivity;
	private static FirebaseAuth s_fbAuth;
	private static CollectionReference s_dbRefUsers;
	private static DbUser s_dbRobot;
	private static String s_strOpponentId;
	private static Map<String, DbUser> s_mDbUsers = new HashMap();

	private FirebaseUser m_fbUser;

	private int m_bmStatus;
	private State m_eState;
	private OpponentState m_eOpponentState = OpponentState.USER_NONE;
	
	private DocumentReference m_docRef;
	private String m_strUserId;
	private String m_strName;
	private int m_bmSettings;
	private int m_nRanking;
	private int m_nHighScore;
	private int m_nGames;

	private int m_nRankWindow = 0;
	private boolean m_bSearching = false;
	private boolean m_bSearching2 = false;
	private boolean m_bOpponent = false;
	private DbUser m_dbOpponent = null;
	private String m_strOpponentId = null;
	private Map<String, DbUser> m_mDbOpponents;
	private final Set<String> m_sUsersBlock = new TreeSet();

	private boolean m_bRecurse = false;

	//**********************************************************************************************
	public static DbUser
	GetDbUser(
		String strUserId,
		Map<String, Object> mFields)
	{
		DbUser dbUser;
		if ((dbUser = s_mDbUsers.get(strUserId)) == null)
		{
			// If we already have the field data for our opponent, avoid extra reads by just saving
			// it in the local object.
			if (mFields != null)
			{
				dbUser = new DbUser(strUserId, mFields);
			}
			else if (strUserId.equals(s_strNameRobot))
			{
				s_dbRobot = dbUser = new DbUser(s_strNameRobot, s_mRobotFields);
			}
			else
			{
				dbUser = new DbUser(strUserId);
			}
			s_mDbUsers.put(strUserId, dbUser);
		}
		return dbUser;
	}

	//**********************************************************************************************
	private static int
	GetIntValue(
		DbUserField eField,
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
	public static String
	GetRobotUserId()
	{
		return s_strNameRobot;
	}

	//**********************************************************************************************
	public static String
	GetRobotUserName()
	{
		return s_strNameRobot;
	}

	//**********************************************************************************************
	public static String
	GetDefaultUserName()
	{
		return s_strNameDefault;
	}

	//**********************************************************************************************
	public static String
	GetOpponentId()
	{
		return s_strOpponentId;
	}

	//**********************************************************************************************
	public static String
	GetTaskExceptionMessage(Task task)
	{
		String strMessage = "";
		Exception exc = task.getException();
		if (exc != null)
		{
			strMessage = exc.getMessage();
		}
		return strMessage;
	}
			
	//**********************************************************************************************
	public static void
	Initialize(
		Main main,
		MainActivity mainActivity)
	{
		s_main = main;
		s_mainActivity = mainActivity;
		s_fbAuth = FirebaseAuth.getInstance();
		s_dbRefUsers = DbAccess.GetCollectionReference(s_strDbTableName);
	}

	//**********************************************************************************************
	// Default constructor for the primary user of the application.
	public
	DbUser()
	{
		m_bmStatus = Status.NONE;
		m_eState = State.INITIAL;

		// See if we have an automatic login for this phone.
		if ((m_fbUser = s_fbAuth.getCurrentUser()) != null)
		{
			// Request a user token id.
			requestUserTokenId(false);
		}
		else
		{
			// Check to see if we have a user logged in already.
			AccessToken accessToken = AccessToken.getCurrentAccessToken();
			if (accessToken != null && !accessToken.isExpired())
			{
				userLogin(accessToken);
			}
			else
			{
				LoginManager.getInstance().registerCallback(
					s_mainActivity.getCallbackManager(),
					new FacebookCallback<LoginResult>()
					{
						@Override
						public void
						onSuccess(LoginResult loginResult)
						{
							userLogin(loginResult.getAccessToken());
						}

						@Override
						public void
						onCancel()
						{
							s_mainActivity.setAlert("facebook login cancelled");
						}

						@Override
						public void
						onError(FacebookException exception)
						{
							s_mainActivity.setAlert(
								"facebook login error" + exception.getMessage());
						}
					}
				);
			}
		}
	}

	//**********************************************************************************************
	// Constructor of an opponent DbUser, will full reference to its document data.
	public
	DbUser(String strUserId)
	{
		m_eOpponentState = OpponentState.USER_SEARCHING;
		m_strUserId = strUserId;
		m_docRef = s_dbRefUsers.document(strUserId);
		m_bOpponent = true;

		// Create a listener for our opponent.
		m_docRef
			.get()
			.addOnCompleteListener(
				task ->
				{
					if (task.isSuccessful())
					{
						DocumentSnapshot doc = task.getResult();
						if (doc.exists())
						{
							initialize(doc.getId(), doc.getData());
							m_eOpponentState = OpponentState.USER_FOUND;
						}
						else
						{
							m_eOpponentState = OpponentState.USER_INVITE;
						}
						s_main.setUserLoaded();
					}
					else
					{
						s_mainActivity.failMessage(
							"DbUser() task failed 1: " + GetTaskExceptionMessage(task));
					}
				}
			);
	}

	//**********************************************************************************************
	// Constructor of an opponent DbUser, will pre-read document data.
	public
	DbUser(
		String strUserId,
		Map<String, Object> mFields)
	{
		m_bmStatus = Status.NONE;
		m_bOpponent = true;
		m_docRef = null;
		initialize(strUserId, mFields);
	}

	//**********************************************************************************************
	public void
	blockOpponent(DbUser dbUser)
	{
		if (dbUser != null)
		{
			String strUserId = dbUser.getUserId();
			if (strUserId != null &&
				!strUserId.isEmpty() &&
				!strUserId.equals(s_strNameRobot))
			{
				m_sUsersBlock.add(strUserId);
				pushUsersBlock();
			}
		}
	}

	//**********************************************************************************************
	public void
	findRandomOpponent(Map<String, DbUser> mDbOpponents)
	{
		// Mark our new opponent as null.
		m_dbOpponent = null;

		// Store the map of opponents we are already playing.
		m_mDbOpponents = mDbOpponents;

		// Trigger a search for an opponent.  Start with a small ranking difference.
		m_bSearching2 = true;
		m_nRankWindow = 10;

		// Create a listener for our opponent.
		CollectionReference users = DbAccess.GetCollectionReference(s_strDbTableName);
		users
			// TODO: put in these where clauses.
			//.whereGreaterThanOrEqualTo(DbUserField.nRanking.toString(), m_nRanking - m_nRankWindow)
			//.whereLessThanOrEqualTo(DbUserField.nRanking.toString(), m_nRanking + m_nRankWindow)
			.get()
			.addOnSuccessListener
			(
				querySnapshot ->
				{
					List<DocumentSnapshot> lSnapshots = querySnapshot.getDocuments();
					int nPlayers = lSnapshots.size();
					DbUser dbOpponent = s_dbRobot;
					if (nPlayers > 1)
					{
						int nTries = 0;
						while (++nTries < 1000)
						{
							int iPlayer = (int)(nPlayers * Math.random());
							DocumentSnapshot snapshot = lSnapshots.get(iPlayer);

							// Ignore users we are blocking.
							String strUserId = snapshot.getId();
							if (m_sUsersBlock.contains(strUserId))
							{
								continue;
							}

							// Ignore the robot, this account and anyone we have a game open with.
							if (!strUserId.equals(m_strUserId) &&
								!strUserId.equals(s_strNameRobot) &&
								m_mDbOpponents.get(strUserId) == null)
							{
								// Create a DbUser object for this player.
								dbOpponent = GetDbUser(strUserId, snapshot.getData());

								// Ignore users who are blocking us.
								if (dbOpponent.m_sUsersBlock.contains(m_strUserId))
								{
									dbOpponent = s_dbRobot;
									continue;
								}
								m_strOpponentId = strUserId;
								break;
							}
						}
					}
					m_dbOpponent = dbOpponent;
					s_mainActivity.updateState(false);
					m_bSearching2 = false;
				}
			);
	}

	//**********************************************************************************************
	public String
	getUserId()
	{
		return m_strUserId;
	}

	//**********************************************************************************************
	public String
	getUserEmail()
	{
		return m_fbUser.getEmail();
	}

	//**********************************************************************************************
	public String
	getUserName()
	{
		if (m_strName == null || m_strName.isEmpty())
		{
			if (m_bOpponent)
			{
				return s_strNameUnknown;
			}
			return s_strNameDefault;
		}
		return m_strName;
	}

	//**********************************************************************************************
	public int
	getGameCount()
	{
		return m_nGames;
	}

	//**********************************************************************************************
	public int
	getHighScore()
	{
		return m_nHighScore;
	}

	//**********************************************************************************************
	public DbUser
	getOpponent()
	{
		return m_dbOpponent;
	}

	//**********************************************************************************************
	public DbUser
	getOpponent(String strOpponentId)
	{
		// If requested, search for a different opponent.
		if (m_strOpponentId != null && !m_strOpponentId.equals(strOpponentId))
		{
			m_dbOpponent = null;
			m_bSearching = false;
		}
		m_strOpponentId = strOpponentId;
		
		// If found, return our opponent;
		if (m_dbOpponent != null)
		{
			return m_dbOpponent;
		}

		// If we're already searching for an opponent, don't trigger another yet.
		if (m_bSearching)
		{
			return null;
		}
		
		// Trigger a search for an opponent.  Start with a small ranking difference.
		m_bSearching = true;

		// Create a listener for our opponent.
		DocumentReference docRef = s_dbRefUsers.document(strOpponentId);
		if (docRef != null)
		{
			docRef
				.get()
				.addOnSuccessListener
				(
					querySnapshot ->
					{
						Map<String, Object> mFields = querySnapshot.getData();
						m_dbOpponent = GetDbUser(strOpponentId, mFields);
						m_bSearching = false;
						s_mainActivity.updateState(false);
					}
				);
		}

		return null;
	}

	//**********************************************************************************************
	public int
	getRanking()
	{
		return m_nRanking;
	}

	//**********************************************************************************************
	public int
	getSettings()
	{
		return m_bmSettings;
	}

	//**********************************************************************************************
	public State
	getState()
	{
		return m_eState;
	}

	//**********************************************************************************************
	public boolean
	hasConnection()
	{
		return (m_bmStatus & Status.HAS_CONNECTION) != 0;
	}

	//**********************************************************************************************
	public boolean
	hasUser()
	{
		return (m_bmStatus & Status.HAS_USER) != 0;
	}

	//**********************************************************************************************
	public boolean
	hasAnonymous()
	{
		return (m_bmStatus & Status.HAS_ANONYMOUS) != 0;
	}

	//**********************************************************************************************
	public boolean
	hasDocument()
	{
		return (m_bmStatus & Status.HAS_DOCUMENT) != 0;
	}

	//**********************************************************************************************
	public boolean
	hasFailure()
	{
		return (m_bmStatus & Status.HAS_FAILURE) != 0;
	}

	//**********************************************************************************************
	public boolean
	hasName()
	{
		return (m_bmStatus & Status.HAS_NAME) != 0;
	}
	
	//**********************************************************************************************
	public boolean
	isBlocked(String strOpponentId)
	{
		return m_sUsersBlock.contains(strOpponentId);
	}

	//**********************************************************************************************
	public boolean
	isDeleted()
	{
		return m_fbUser == null;
	}

	//**********************************************************************************************
	public boolean
	isLoggedIn()
	{
		return m_strUserId != null;
	}

	//**********************************************************************************************
	public boolean
	isRobot()
	{
		return m_strUserId.equals(s_strNameRobot);
	}

	//**********************************************************************************************
	public void
	initialize(
		String strUserId,
		Map<String, Object> mFields)
	{
		m_strUserId = strUserId;
		m_bmSettings = GetIntValue(DbUserField.bmSettings, mFields);
		m_nGames = GetIntValue(DbUserField.nGames, mFields);
		m_nRanking = GetIntValue(DbUserField.nRanking, mFields);
		m_nHighScore = GetIntValue(DbUserField.nHighScore, mFields);
		m_strName = (String)mFields.get(DbUserField.strName.toString());

		// If we have a list of users to block, add them locally.
		List<String> aUsersBlock = (List<String>)mFields.get(DbUserField.aUsersBlock.toString());
		if (aUsersBlock != null)
		{
			for (String strBlockId: aUsersBlock)
			{
				m_sUsersBlock.add(strBlockId);
			}
		}

		m_bmStatus |= Status.HAS_DOCUMENT;
		m_eState = State.READY;
	}

	//**********************************************************************************************
	private void
	pullData(AccessToken accessToken)
	{
		pullData(accessToken.getUserId(), null, true);
	}
	
	//**********************************************************************************************
	private void
	pullData(
		String strUserId,
		String strDisplayName,
		boolean bRecurse)
	{
		m_bRecurse = bRecurse;
		if ((m_strName = strDisplayName) == null || m_strName.isEmpty())
		{
			m_eState = State.USER_ANONYMOUS;
		}
		else
		{
			m_bmStatus |= Status.HAS_NAME;
		}
		
		// Get a reference to our user document, even if there isn't one there yet.
		m_docRef = s_dbRefUsers.document(strUserId);
		m_strUserId = strUserId;
		
		// If the user document is there, load its data into our object.
		m_docRef
			.get()
			.addOnCompleteListener(
				task ->
				{
					if (task.isSuccessful())
					{
						DocumentSnapshot doc = task.getResult();
						Map<String, Object> mFields = null;
						if (doc.exists())
						{
							mFields = doc.getData();
						}
						if (mFields != null)
						{
							initialize(doc.getId(), mFields);
							s_main.setUserLoaded();
							m_bRecurse = false;
						}
					}
					if (m_bRecurse)
					{
						String strDisplayName1 = m_fbUser.getDisplayName();
						if (strDisplayName1 == null || strDisplayName1.isEmpty())
						{
							strDisplayName1 = m_strName;
						}
						pushData(m_strUserId, strDisplayName1);
						pullData(m_strUserId, strDisplayName1, false);
						s_main.setUserRegister();
					}
					s_mainActivity.updateState(false);
				}
			);
	}

	//**********************************************************************************************
	public void
	pushData()
	{
		if (!isRobot())
		{
			Map<String, Object> mFields = new HashMap<>();

			mFields.put(DbUserField.bmSettings.toString(), (long)m_bmSettings);
			mFields.put(DbUserField.nGames.toString(), (long)m_nGames);
			mFields.put(DbUserField.nRanking.toString(), (long)m_nRanking);
			mFields.put(DbUserField.nHighScore.toString(), (long)m_nHighScore);
			mFields.put(DbUserField.strName.toString(), m_strName);

			// Asynchronously add our new user.
			DbAccess.MergeData(s_strDbTableName, m_strUserId, mFields);
		}
	}

	//**********************************************************************************************
	public void
	pushData(
		String strUserId,
		String strDisplayName)
	{
		m_strUserId = strUserId;
		m_strName = strDisplayName;
		m_bmSettings = 0;
		m_nRanking = 0;
		m_nHighScore = 0;
		m_nGames = 0;

		// Create a new document to be added to the users table.
		Map<String, Object> mFields = new HashMap<>();
		mFields.put(DbUserField.bmSettings.toString(), (long)m_bmSettings);
		mFields.put(DbUserField.nGames.toString(), (long)m_nGames);
		mFields.put(DbUserField.nRanking.toString(), (long)m_nRanking);
		mFields.put(DbUserField.nHighScore.toString(), (long)m_nHighScore);
		mFields.put(DbUserField.strName.toString(), m_strName);

		// Asynchronously add our new user.
		DbAccess.MergeData(s_strDbTableName, strUserId, mFields);
	}

	//**********************************************************************************************
	public void
	pushUsersBlock()
	{
		if (!m_sUsersBlock.isEmpty())
		{
			List<String> lUsersBlock = new ArrayList();
			lUsersBlock.addAll(m_sUsersBlock);
			Map<String, Object> mFields = new HashMap<>();
			mFields.put(DbUserField.aUsersBlock.toString(), lUsersBlock);
			DbAccess.MergeData(s_strDbTableName, m_strUserId, mFields);
		}
	}

	//**********************************************************************************************
	private String
	createEncoding(String strWord)
	{
		final int[] primes = new int[]
		{
			23, 173, 89, 59, 137, 11, 163, 101, 73, 13,
			199, 181, 7, 109, 113, 29, 179, 47, 127, 5
		};
		
		if (strWord == null || strWord.length() < 8)
		{
			return null;
		}

		try
		{
			// Hash-encrypt the word.
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(strWord.getBytes());
			byte[] mdHash = md.digest();

			// Extend this to a 20 character shrouded hash.
			byte[] bytes = new byte[20];
			int j = 1;
			int k = 3;
			for (int i = 0; i < bytes.length; ++i)
			{
				bytes[i] = (byte)((mdHash[j] + mdHash[k] + primes[i]) & 0xFF);
				if (++j >= mdHash.length)
				{
					j = 0;
				}
				if ((k += 7) >= mdHash.length)
				{
					k -= mdHash.length;
				}
			}

			// Shroud it further.
			j = 0;
			k = 1;
			for (int i = 0; i < 100003; ++i)
			{
				bytes[j] = (byte)((bytes[j] >> 1) ^ bytes[k]);
				if (++j >= bytes.length)
				{
					j = 0;
				}
				if ((k += 3) >= bytes.length)
				{
					k -= bytes.length;
				}
			}

			// Convert the binary digits into printable characters.
			StringBuilder sbHash = new StringBuilder();
			for (int i = 0; i < bytes.length; ++i)
			{
				sbHash.append((char)(0x3D + (bytes[i] & 0x3F)));
			}

			return sbHash.toString();
		}
		catch (Exception ignored)
		{
		}
		return null;
	}

	//**********************************************************************************************
	private void
	requestUserTokenId(boolean bRecurse)
	{
		if (m_fbUser != null)
		{
			// If we have a profile, get the person's name.  TODO: photo?
			Profile profile = Profile.getCurrentProfile();
			if (profile != null)
			{
				m_strName = profile.getName();
			}

			// Use email, if available for the user id token seed, else facebook id.
			m_strUserId = m_fbUser.getEmail();
			if ((m_strUserId == null || m_strUserId.isEmpty()) &&
				profile != null)
			{
				m_strUserId = profile.getId();
			}

			// Reference the database for our user table data.
			FirebaseMessaging
				.getInstance()
				.getToken()
				.addOnCompleteListener
				(
					task ->
					{
						// Create an encoding of our email, if present.
						m_strUserId = createEncoding(m_strUserId);
						
						// If needed, get a Firebase token as a user id.
						// These can vary with each application update.
						if (m_strUserId == null || m_strUserId.isEmpty())
						{
							if (task.isSuccessful())
							{
								//	m_strUserId = task.getResult().getToken();
								m_strUserId = task.getResult();
								
								// Limit the token identifier to 28 digits
								if (m_strUserId != null && m_strUserId.length() > 28)
								{
									m_strUserId = m_strUserId.substring(0, 28);
								}
							}
						}

						// If we have an id, connect or create an account.
						if (m_strUserId != null && !m_strUserId.isEmpty())
						{
							m_bmStatus |= Status.HAS_USER;

							// Try to get the user's display name.
							String strDisplayName = m_fbUser.getDisplayName();
							if (strDisplayName == null || strDisplayName.isEmpty())
							{
								strDisplayName = m_strName;
							}
							else
							{
								m_strName = strDisplayName;
							}

							pullData(m_strUserId, strDisplayName, bRecurse);
							s_main.setUserLogin();
						}
						else
						{
							s_mainActivity.setAlert("Failed to get token");
						}
					}
				);
		}
	}

	//**********************************************************************************************
	public void
	setFinalGameScore(int nScore)
	{
		m_nRanking = (nScore + (m_nRanking * m_nGames)) / (m_nGames + 1);
		if (nScore > m_nHighScore)
		{
			m_nHighScore = nScore;
		}
		++m_nGames;
		pushData();
	}

	//**********************************************************************************************
	public boolean
	setUserName(String strDisplayName)
	{
		if (strDisplayName != null && !strDisplayName.isEmpty())
		{
			// Block the use of JotRussell as a name.
			if (strDisplayName.equals(s_strNameRobot))
			{
				return false;
			}
			
			m_strName = strDisplayName;
			s_mainActivity.setPlayerName(strDisplayName);
			
			Map<String, Object> mFields = new HashMap<>();
			mFields.put(DbUserField.strName.toString(), m_strName);
			DbAccess.MergeData(s_strDbTableName, m_strUserId, mFields);
		}
		return true;
	}

	//**********************************************************************************************
	public void
	userDelete()
	{
		// Delete the user's entry in the users table.
		if (m_docRef != null)
		{
			DbAccess.Delete(m_docRef);
			m_docRef = null;
		}

		// Delete the user's authentication account.
		try
		{
			m_fbUser
				.delete()
				.addOnCompleteListener
				(
					task ->
					{
						m_bmStatus &= ~Status.HAS_USER;
						m_fbUser = null;
						s_main.setUserDeleted();
						s_mainActivity.updateState(false);
					}
				);
		}
		catch (Exception e)
		{
		}
	}
	
	//**********************************************************************************************
	public void
	userDelete(
		String strEmail,
		String strPassword)
	{
		s_fbAuth
			.signInWithEmailAndPassword(strEmail, strPassword)
			.addOnCompleteListener(
				s_mainActivity,
				task ->
				{
					m_bmStatus = Status.HAS_CONNECTION;
					if (task.isSuccessful())
					{
						m_fbUser = s_fbAuth.getCurrentUser();
						userDelete();
					}
					else
					{
						m_eState = State.NO_DOCUMENT;
						m_bmStatus |= Status.HAS_FAILURE;
						// If sign in fails, display a message to the user.
						s_mainActivity.setAlert(
							"Login failed:" + GetTaskExceptionMessage(task));
						s_mainActivity.updateState(false);
					}
				}
			);
	}

	//**********************************************************************************************
	public OpponentState
	getInviteState()
	{
		if (m_eOpponentState == OpponentState.USER_SEARCHING && m_dbOpponent != null)
		{
			OpponentState eState;
			if ((eState = m_dbOpponent.m_eOpponentState) == OpponentState.USER_FOUND)
			{
				m_dbOpponent.m_eOpponentState = OpponentState.USER_WAITING;
				DbGame.PostGame(this, m_dbOpponent);
			}
			
			return eState;
		}
		return m_eOpponentState;
	}
	
	//**********************************************************************************************
	public String
	userInvite(String strEmail)
	{
		m_eOpponentState = OpponentState.USER_START;

		// Make sure the email is not empty.
		if (TextUtils.isEmpty(strEmail))
		{
			m_eOpponentState = OpponentState.USER_EMPTY;
			return null;
		}

		// Make sure the player didn't mistakenly provide their own email.
		if (strEmail.equals(getUserEmail()))
		{
			m_eOpponentState = OpponentState.USER_SAME;
			return null;
		}

		// Create an encoding of the email.
		s_strOpponentId = createEncoding(strEmail);

		// Check to see if we already have a game active.
		if (DbGame.HaveGameWith(s_strOpponentId))
		{
			m_eOpponentState = OpponentState.USER_ACTIVE;
		}
		else
		{
			// Create a DbUser() object which will search for an existing user.
			m_dbOpponent = null;
			m_eOpponentState = OpponentState.USER_SEARCHING;
			m_dbOpponent = GetDbUser(s_strOpponentId, null);
		}
		return s_strOpponentId;
	}

	//**********************************************************************************************
	public void
	userLogin(AccessToken accessToken)
	{
		if (accessToken == null)
		{
			s_mainActivity.setAlert("Login failed: Null access token");
			return;
		}

		// Get a Firebase credential from Facebook.
		AuthCredential authCred = FacebookAuthProvider.getCredential(accessToken.getToken());

		// Attempt login.
		s_fbAuth
			.signInWithCredential(authCred)
			.addOnCompleteListener(
				s_mainActivity,
				task ->
				{
					m_bmStatus = Status.HAS_CONNECTION;
					if (task.isSuccessful())
					{
						m_fbUser = task.getResult().getUser();
					}
					else
					{
						m_fbUser = s_fbAuth.getCurrentUser();
					}
					if (m_fbUser != null)
					{
						requestUserTokenId(true);
					}
					else
					{
						m_eState = State.NO_DOCUMENT;
						m_bmStatus |= Status.HAS_FAILURE;
						// If sign in fails, display a message to the user.
						s_mainActivity.setAlert(
							"Login failed:" + GetTaskExceptionMessage(task));
						s_main.setUserFailed();
					}
					s_mainActivity.updateState(false);
				}
			);
	}

	//**********************************************************************************************
	public void
	userLogin(FirebaseAuth fbAuth)
	{
		FirebaseUser fbUser = fbAuth.getCurrentUser();
		if (m_fbUser == null && fbUser != null)
		{
			s_fbAuth = fbAuth;
			m_fbUser = fbUser;
			requestUserTokenId(false);
			s_mainActivity.updateState(false);
		}
	}

	//**********************************************************************************************
	public void
	userLogin(GoogleSignInAccount account)
	{
		if (account == null)
		{
			s_mainActivity.setAlert("Login failed: Null account");
			return;
		}

		// Get a Firebase credential from Facebook.
		AuthCredential authCred = GoogleAuthProvider.getCredential(account.getIdToken(), null);

		// Attempt login.
		s_fbAuth
			.signInWithCredential(authCred)
			.addOnCompleteListener(
				s_mainActivity,
				task ->
				{
					m_bmStatus = Status.HAS_CONNECTION;
					if (task.isSuccessful())
					{
						m_fbUser = task.getResult().getUser();
					}
					else
					{
						m_fbUser = s_fbAuth.getCurrentUser();
					}
					if (m_fbUser != null)
					{
						requestUserTokenId(true);
					}
					else
					{
						m_eState = State.NO_DOCUMENT;
						m_bmStatus |= Status.HAS_FAILURE;
						// If sign in fails, display a message to the user.
						s_mainActivity.setAlert(
							"Login failed:" + GetTaskExceptionMessage(task));
						s_main.setUserFailed();
					}
					s_mainActivity.updateState(false);
				}
			);
	}

	//**********************************************************************************************
	public void
	userLogin(
		String strEmail,
		String strPassword,
		String strDisplayName)
	{
		s_fbAuth
			.signInWithEmailAndPassword(strEmail, strPassword)
			.addOnCompleteListener(
				s_mainActivity,
				task ->
				{
					// Block the use of JotRussell as a name.
					if (strDisplayName != null &&
						!strDisplayName.isEmpty() &&
						!strDisplayName.equals(s_strNameRobot))
					{
						m_strName = strDisplayName;
					}
					m_bmStatus = Status.HAS_CONNECTION;
					if (task.isSuccessful())
					{
						// Sign in success, update UI with the signed-in user's information
						m_fbUser = s_fbAuth.getCurrentUser();
						requestUserTokenId(true);
					}
					else
					{
						m_eState = State.NO_DOCUMENT;
						m_bmStatus |= Status.HAS_FAILURE;
						// If sign in fails, display a message to the user.
						s_mainActivity.setAlert(
							"Login failed:" + GetTaskExceptionMessage(task));
						s_main.setUserFailed();
					}
					s_mainActivity.updateState(false);
				}
			);
	}

	//**********************************************************************************************
	public void
	userRegister(
		String strEmail,
		String strPassword,
		String strDisplayName)
	{
		s_fbAuth
			.createUserWithEmailAndPassword(strEmail, strPassword)
			.addOnCompleteListener(
				s_mainActivity,
				task ->
				{
					m_bmStatus = Status.HAS_CONNECTION;
					if (task.isSuccessful())
					{
						m_fbUser = s_fbAuth.getCurrentUser();
						setUserName(strDisplayName);
						requestUserTokenId(false);
					}
					else
					{
						m_eState = State.NO_DOCUMENT;
						m_bmStatus |= Status.HAS_FAILURE;
						s_mainActivity.setAlert(
							"Registration failed: " + GetTaskExceptionMessage(task));
						s_main.setUserFailed();
					}
					s_mainActivity.updateState(false);
				}
			);
	}

	//**********************************************************************************************
	public void
	userReset(String strEmail)
	{
		s_fbAuth
			.sendPasswordResetEmail(strEmail)
			.addOnCompleteListener(
				s_mainActivity,
				task ->
				{
					if (task.isSuccessful())
					{
						s_mainActivity.setAlert("Password reset email sent");
					}
					else
					{
						s_mainActivity.setAlert(
							"Password reset error: " + GetTaskExceptionMessage(task));
					}
					s_mainActivity.updateState(false);
				}
			);
	}
}

//**************************************************************************************************
