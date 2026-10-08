//**************************************************************************************************
//
//  DbUser.swift
//  
//
//  Created by Stephen Kennedy on 5/7/20.
//
//**************************************************************************************************

import Foundation
import Firebase
import FirebaseCore
import UIKit

//**************************************************************************************************

class DbUser
{
	enum Status: Int
	{
		case NONE = 0x00
		case HAS_CONNECTION = 0x01
		case HAS_USER = 0x02
		case HAS_ANONYMOUS = 0x04
		case HAS_DOCUMENT = 0x08
		case HAS_FAILURE = 0x10
		case HAS_NAME = 0x20
	}
	
	enum State
	{
		case INITIAL
		case NO_CONNECTION
		case NO_DOCUMENT
		case USER_ANONYMOUS
		case HAVE_USER
		case READY
	}
	
	enum OpponentState
	{
		case USER_NONE
		case USER_START
		case USER_SEARCHING
		case USER_EMPTY
		case USER_SAME
		case USER_BLOCKED
		case USER_ACTIVE
		case USER_INVITE
		case USER_FOUND
		case USER_WAITING
	}
	
	// Fields must match that in t_user.sql
	enum DbUserField: String
	{
		case bmSettings = "bmSettings"
		case nGames = "nGames"
		case nRanking = "nRanking"
		case nHighScore = "nHighScore"
		case strName = "strName"
		case aUsersBlock = "aUsersBlock"
	}

	let s_mRobotFields =
	[
		DbUserField.bmSettings: 0L,
		DbUserField.nGames: 1L,
		DbUserField.nRanking: 613L,
		DbUserField.nHighScore: 957L,
		DbUserField.strName: s_strNameRobot
	]

	static let s_strNameRobot = "JotRussell"
	static let s_strNameDefault = "You"
	static let s_strNameUnknown = "Anonymous"
	static let s_strDbTableName = "users"

	static var s_main: Main? = nil
    static var s_fbAuth: Auth? = nil
	static var s_dbRefUsers: CollectionReference? = nil
	static var s_dbRobot: DbUser? = nil
	static var s_strOpponentId: String? = nil
    static var s_mDbOpponents: [String: DbUser] = [:]

	var m_fbUser: User? = nil

	var m_bmStatus = 0
    var m_eState: State = .INITIAL
	var m_eOpponentState: OpponentState = .USER_NONE

	var m_docRef: DocumentReference? = nil
	var m_strUserId: String? = nil
	var m_strName: String? = DbUser.s_strNameUnknown
	var m_bmSettings = 0
	var m_nRanking = 0
	var m_nHighScore = 0
	var m_nGames = 0

	var m_nRankWindow = 0
	var m_bSearching = false
	var m_bSearching2 = false
	var m_bOpponent = false
	var m_dbOpponent: DbUser? = nil
	var m_strOpponentId: String? = nil
    var m_mDbOpponents: [String: DbUser] = [:]
	var m_sUsersBlock: Set<String> = []

	var m_bRecurse = false

	//**********************************************************************************************
	static func
	GetDbUser(
		strUserId: String,
		mFields: [String: Any]
	) -> DbUser
	{
		dbUser: DbUser? = DbUser.s_mDbUsers[strUserId]
		if (dbUser == nil)
		{
			// If we already have the field data for our opponent, avoid extra reads by just saving
			// it in the local object.
			if let mFields = mFields
			{
				dbUser = new DbUser(strUserId: strUserId, mFields: mFields)
			}
			else if (strUserId == s_strNameRobot)
			{
				dbUser = new DbUser(strUserId: s_strNameRobot, mFields: s_mRobotFields)
				DbUser.s_dbRobot = dbUser
			}
			else
			{
				dbUser = new DbUser(strUserId: strUserId)
			}
			DbUser.s_mDbUsers[strUserId] = dbUser
		}
		return dbUser
	}

	//**********************************************************************************************
	static func
	GetRobotUserId() -> String
	{
		return DbUser.s_strNameRobot
	}

	//**********************************************************************************************
	static func
	GetRobotUserName() -> String
	{
		return DbUser.s_strNameRobot
	}

	//**********************************************************************************************
	static func
	GetDefaultUserName() -> String
	{
		return DbUser.s_strNameDefault
	}

	//**********************************************************************************************
	static func
	GetOpponentId() -> String
	{
		return DbUser.s_strOpponentId
	}

	//**********************************************************************************************
	static func
	Initialize(main: Main)
	{
		s_main = main
        s_fbAuth = Auth.auth()
        s_dbRefUsers = DbAccess.GetCollectionReference(strNode: s_strDbTableName)
	}

	//**********************************************************************************************
    init()
	{
		m_bmStatus = .NONE
		m_eState = .INITIAL

		// See if we have an automatic login for this phone.
		m_fbUser = s_fbAuth!.getCurrentUser()
		if (m_fbUser != nil)
		{
			// Request a user token id.
			requestUserTokenId()
		}
		else
		{
			// Check to see if we have a user logged in already.
			accessToken: AccessToken = AccessToken.getCurrentAccessToken()
			if (accessToken != nil && !accessToken!.isExpired())
			{
				userLogin(accessToken)
			}
			else
			{
				LoginManager.getInstance().registerCallback(
					s_main!.getCallbackManager(),
					new FacebookCallback<LoginResult>()
					{
						@Override
						public void onSuccess(LoginResult loginResult)
						{
							userLogin(loginResult.getAccessToken());
						}

						@Override
						public void onCancel()
						{
						}

						@Override
						public void onError(FacebookException exception)
						{
						}
					}
				);
			}
		}
	}
	
	//**********************************************************************************************
	// Constructor of an opponent DbUser, will full reference to its document data.
    init(strUserId: String)
	{
		m_eOpponentState = .USER_SEARCHING
		m_strUserId = strUserId
        m_docRef = DbUser.s_dbRefUsers?.document(strUserId)
		m_bOpponent = true

		// Create a listener for our opponent.
		m_docRef?.addSnapshotListener	// addSnapshotListener
        {
            docSnapshot,
            error in
            guard let doc = docSnapshot
            else
            {
                print("DbUser.init() failed: \(error!)\n")
                return
            }
            guard let data = doc.data()
            else
            {
                print("DbUser.init() document data was empty.\n")
				self.m_eOpponentState = .USER_INVITE
				DbUser.s_main!.setUserLoaded();
                return
            }
            self.initialize(strUserId: doc.documentID, mFields: data)
			self.m_eOpponentState = .USER_FOUND
			DbUser.s_main!.setUserLoaded();
        }
	}

	//**********************************************************************************************
	init(
		strUserId: String,
		mFields: [String: Any])
	{
        m_bmStatus = Status.NONE.rawValue
		m_bOpponent = true
		m_docRef = nil
		initialize(strUserId: strUserId, mFields: mFields)
	}

	//**********************************************************************************************
	func
	blockOpponent(dbUser: DbUser)
	{
		if let strUserId = dbUser.getUserId()
		{
			if (!strUserId.isEmpty &&
			   	strUserId != DbUser.s_strNameRobot)
			{
				m_sUsersBlock.insert(strUserId)
				pushUsersBlock()
			}
		}
	}

	//**********************************************************************************************
	func
	findRandomOpponent(mDbOpponents: [String: DbUser])
	{
		// Mark our new opponent as nil.
		m_dbOpponent = nil

		// Store the map of opponents we are already playing.
		m_mDbOpponents = mDbOpponents

		// Trigger a search for an opponent.  Start with a small ranking difference.
		m_bSearching2 = true
		m_nRankWindow = 10

		// Create a listener for our opponent.
		let users = DbAccess.GetCollectionReference(strNode: DbUser.s_strDbTableName)
		// TODO: put in these where clauses.
        users.whereField(DbUserField.nGames.rawValue, isGreaterThan: 0)
            .getDocuments()
            { (querySnapshot, error) in
                if let error = error
                {
                    print("DbUser.findRandomOpponent() failure: \(error)\n")
					DbUser.s_main?.updateState(bAnimate: false)
					return
                }

				if let lSnapshots = querySnapshot?.documents
				{
                    let nPlayers = lSnapshots.count
					var dbOpponent = DbUser.s_dbRobot
					if (nPlayers > 1)
					{
						var nTries = 0
						while (nTries < 1000)
						{
							nTries += 1
							let iPlayer = Int.random(in: 0..<nPlayers)
							let snapshot = lSnapshots[iPlayer]

							// Ignore users we are blocking.
                            let strUserId: String = snapshot.documentID
							if (self.m_sUsersBlock.contains(strUserId))
							{
								continue
							}

							if (strUserId != self.m_strUserId &&
								strUserId != DbUser.s_strNameRobot &&
                                self.m_mDbOpponents[strUserId] == nil)
							{
								dbOpponent = GetDbUser(strUserId: strUserId, mFields: snapshot.data())

								// Ignore users who are blocking us.
                                if (dbOpponent!.m_sUsersBlock.contains(self.m_strUserId!))
								{
									dbOpponent = DbUser.s_dbRobot
									continue
								}
								self.m_strOpponentId = strUserId
								break
							}
						}
					}
					self.m_dbOpponent = dbOpponent
					DbUser.s_main?.updateState(bAnimate: false)
					self.m_bSearching2 = false
                }
            }
	}

	//**********************************************************************************************
	func
	getUserId() -> String?
	{
		return m_strUserId
	}

	//**********************************************************************************************
	func
	getUserEmail() -> String?
	{
        return m_fbUser!.email
	}

	//**********************************************************************************************
	func
	getUserName() -> String
	{
		if let strName = m_strName
		{
			if (!strName.isEmpty)
			{
				return strName
			}
		}
		
		if (m_bOpponent)
		{
			return DbUser.s_strNameUnknown
		}
		return DbUser.s_strNameDefault
	}

	//**********************************************************************************************
	func
	getGameCount() -> Int
	{
		return m_nGames
	}

	//**********************************************************************************************
	func
	getHighScore() -> Int
	{
		return m_nHighScore
	}

	//**********************************************************************************************
	func
	getOpponent() -> DbUser?
	{
		return m_dbOpponent
	}

	//**********************************************************************************************
	func
	getOpponent(strOpponentId: String) -> DbUser?
	{
		// If requested, search for a different opponent.
		if (m_strOpponentId != nil && m_strOpponentId != strOpponentId)
		{
			m_dbOpponent = nil
			m_bSearching = false
		}
		m_strOpponentId = strOpponentId
		
		// If found, return our opponent
		if (m_dbOpponent != nil)
		{
			return m_dbOpponent
		}

		// If we're already searching for an opponent, don't trigger another yet.
		if (m_bSearching)
		{
			return nil
		}
		
		// Trigger a search for an opponent.  Start with a small ranking difference.
		m_bSearching = true

		// Create a listener for our opponent.
		if let docRef = DbUser.s_dbRefUsers?.document(strOpponentId)
		{
			// Create a listener for our opponent.
			docRef
				.addSnapshotListener
            	{
					docSnapshot,
                	error in
                	guard let doc = docSnapshot
                	else
                	{
						print("DbUser.getOpponent() error fetching document: \(error!)\n")
						self.m_bSearching = false
						DbUser.s_main?.updateState(bAnimate: false)
                    	return
					}
                	guard let data = doc.data()
                	else
                	{
						print("DbUser.getOpponent() document data was empty.\n")
						self.m_bSearching = false
						DbUser.s_main?.updateState(bAnimate: false)
                    	return
                	}
					self.m_dbOpponent = GetDbUser(strUserId: strOpponentId, mFields: data);
					self.m_bSearching = false
					DbUser.s_main?.updateState(bAnimate: false)
            	}
		}

		return nil
	}

	//**********************************************************************************************
	func
	getRanking() -> Int
	{
		return m_nRanking
	}

	//**********************************************************************************************
	func
	getSettings() -> Int
	{
		return m_bmSettings
	}

	//**********************************************************************************************
	func
	getState() -> State
	{
		return m_eState
	}

	//**********************************************************************************************
	func
	hasConnection() -> Bool
	{
		return (m_bmStatus & Status.HAS_CONNECTION.rawValue) != 0
	}

	//**********************************************************************************************
	func
	hasUser() -> Bool
	{
		return (m_bmStatus & Status.HAS_USER.rawValue) != 0
	}

	//**********************************************************************************************
	func
	hasAnonymous() -> Bool
	{
		return (m_bmStatus & Status.HAS_ANONYMOUS.rawValue) != 0
	}

	//**********************************************************************************************
	func
	hasDocument() -> Bool
	{
		return (m_bmStatus & Status.HAS_DOCUMENT.rawValue) != 0
	}

	//**********************************************************************************************
	func
	hasFailure() -> Bool
	{
		return (m_bmStatus & Status.HAS_FAILURE.rawValue) != 0
	}

	//**********************************************************************************************
	func
	hasName() -> Bool
	{
		return (m_bmStatus & Status.HAS_NAME.rawValue) != 0
	}

	//**********************************************************************************************
	func
	isBlocked(strOpponentId: String) -> Bool
	{
		return m_sUsersBlock.contains(strOpponentId)
	}

	//**********************************************************************************************
	func
	isLoggedIn() -> Bool
	{
		return m_strUserId != nil
	}

	//**********************************************************************************************
	func
	isRobot() -> Bool
	{
		return m_strUserId == DbUser.s_strNameRobot
	}

	//**********************************************************************************************
	func
	initialize(
		strUserId: String,
		mFields: [String: Any])
	{
		m_strUserId = strUserId
		m_bmSettings = mFields[DbUserField.bmSettings.rawValue] as! Int
		m_nGames = mFields[DbUserField.nGames.rawValue] as! Int
		m_nRanking = mFields[DbUserField.nRanking.rawValue] as! Int
		m_nHighScore = mFields[DbUserField.nHighScore.rawValue] as! Int
		m_strName = mFields[DbUserField.strName.rawValue] as? String
        
        // If we have a list of users to block, add them locally.
        if let aUsersBlock = mFields[DbUserField.aUsersBlock.rawValue] as? Array<String>
        {
			for strBlockId in aUsersBlock
			{
				m_sUsersBlock.insert(strBlockId);
			}
		}

		m_bmStatus |= Status.HAS_DOCUMENT.rawValue
		m_eState = State.READY
	}

	//**********************************************************************************************
	func
	pullData(accessToken: AccessToken)
	{
		pullData(strUserId: accessToken.getUserId(), strDisplayName: nil, bRecurse: true)
	}
	
	//**********************************************************************************************
	func
	pullData(
		strUserId: String,
		strDisplayName: String?,
		bRecurse: Bool)
	{
		m_bRecurse = bRecurse
		if ((strDisplayName ?? "").isEmpty)
		{
			m_eState = .USER_ANONYMOUS
		}
		else
		{
			m_bmStatus |= Status.HAS_NAME.rawValue
		}
		
		// Get a reference to our user document, even if there isn't one there yet.
		m_docRef = DbUser.s_dbRefUsers?.document(strUserId)
		m_strUserId = strUserId

		// If the user document is there, load its data into our object.
		m_docRef?.addSnapshotListener
        {
            docSnapshot,
            error in
            guard let doc = docSnapshot
            else
            {
				print("DbUser.pullData() error: \(error!)\n")
				return
            }
            if let data = doc.data()
			{
				self.initialize(strUserId: doc.documentID, mFields: data)
				DbUser.s_main?.setUserLoaded()
				self.m_bRecurse = false
			}

			if (self.m_bRecurse)
			{
				var strDisplayName1 = self.m_fbUser!.displayName
				if (strDisplayName1 == nil || strDisplayName1!.isEmpty)
				{
					strDisplayName1 = self.m_strName
				}
                self.pushData(strUserId: self.m_strUserId!, strDisplayName: strDisplayName1)
                self.pullData(strUserId: self.m_strUserId!, strDisplayName: strDisplayName1, bRecurse: false)
				DbUser.s_main!.setUserRegister()
			}
			DbUser.s_main!.updateState(bAnimate: false)
        }
	}

	//**********************************************************************************************
	func
	pushData()
	{
		if (!isRobot())
		{
			var mFields: [String: Any] = [:]

			mFields[DbUserField.bmSettings.rawValue] = m_bmSettings
			mFields[DbUserField.nGames.rawValue] = m_nGames
			mFields[DbUserField.nRanking.rawValue] = m_nRanking
			mFields[DbUserField.nHighScore.rawValue] = m_nHighScore
			mFields[DbUserField.strName.rawValue] = m_strName

			// Asynchronously add our new user.
			if let strUserId = m_strUserId
			{
				DbAccess.MergeData(
					strTable: DbUser.s_strDbTableName, strTitle: strUserId, mData: mFields)
			}
		}
	}

	//**********************************************************************************************
	func
	pushData(
		strUserId: String,
		strDisplayName: String?)
	{
		m_strUserId = strUserId
		m_strName = strDisplayName
		m_bmSettings = 0
		m_nRanking = 0
		m_nHighScore = 0
		m_nGames = 0

		// Create a new document to be added to the users table.
        var mFields: [String: Any] = [:]
		mFields[DbUserField.bmSettings.rawValue] = m_bmSettings
		mFields[DbUserField.nGames.rawValue] = m_nGames
		mFields[DbUserField.nRanking.rawValue] = m_nRanking
		mFields[DbUserField.nHighScore.rawValue] = m_nHighScore
		mFields[DbUserField.strName.rawValue] = m_strName

		// Asynchronously add our new user.
		DbAccess.MergeData(strTable: DbUser.s_strDbTableName, strTitle: strUserId, mData: mFields)
	}

	//**********************************************************************************************
	func
	pushUsersBlock()
	{
		if (!m_sUsersBlock.isEmpty)
		{
			var lUsersBlock: [String] = []
            var i = 0
            for strUserId in m_sUsersBlock
            {
                lUsersBlock[i] = strUserId
                i += 1
            }
			var mFields: [String: Any] = [:]
			mFields[DbUserField.aUsersBlock.rawValue] = lUsersBlock
            DbAccess.MergeData(strTable: DbUser.s_strDbTableName, strTitle: m_strUserId!, mData: mFields)
		}
	}

	//**********************************************************************************************
	func
	createEncoding(strWord: String?) -> String?
	{
		let primes =
		[
			23, 173, 89, 59, 137, 11, 163, 101, 73, 13,
			199, 181, 7, 109, 113, 29, 179, 47, 127, 5
		]
		
        if (strWord == nil || strWord!.count < 8)
		{
			return nil
		}

		do
		{
			// Hash-encrypt the word.
			let md = MessageDigest.getInstance("MD5")
			md.update(strWord!.getBytes())
			let mdHash = md.digest()

			// Extend this to a 20 character shrouded hash.
			var bytes: [UInt8] = []
			var j = 1
			var k = 3
			for i in 0..<20
			{
				bytes[i] = (mdHash[j] + mdHash[k] + primes[i]) & 0xFF
				j += 1
				if (j >= mdHash.length)
				{
					j = 0
				}
				k += 7
				if (k >= mdHash.length)
				{
					k -= mdHash.length;
				}
			}

			// Shroud it further.
			j = 0
			k = 1
			for i in 0..<100003
			{
				bytes[j] = (bytes[j] >> 1) ^ bytes[k]
				j += 1
				if (j >= bytes.count)
				{
					j = 0
				}
				k += 3
				if (k >= bytes.count)
				{
					k -= bytes.count;
				}
			}

			// Convert the binary digits into printable characters.
			var strHash = String()

			for i in 0..<bytes.count
			{
                let char = Character(Unicode.Scalar(0x3D + (bytes[i] & 0x3F)))
                strHash.append(char)
			}

			return strHash
		}
		catch
		{
		}
		return nil
	}

	//**********************************************************************************************
	func
	requestUserTokenId()
	{
		if let fbUser = m_fbUser
		{
			m_strUserId = fbUser.email
			
			// If we have a profile, get the person's name.  TODO: photo?
			if let profile = Profile.getCurrentProfile()
			{
				m_strName = profile.getName()

				// Use email, if available for the user id token seed, else facebook id.
				if (m_strUserId == nil || m_strUserId!.isEmpty)
				{
					m_strUserId = profile.getId()
				}
			}

			InstanceId
				.instanceId()
				.instanceId
				{
					(result, error) in
					if let error = error
					{
						DbUser.s_main!.setAlert(strAlert: "Error getting token: \(error!)\n")
						return
					}
					if let result = result
					{
						// Create an encoding of our email, if present.
						self.m_strUserId = createEncoding(self.m_strUserId)
					
						// If needed, get a Firebase token as a user id.
						// These can vary with each application update.
						if (self.m_strUserId == nil || self.m_strUserId!.isEmpty)
						{
							self.m_strUserId = result.data()
						
							// Limit the token identifier to 28 digits
							if (self.m_strUserId != nil && self.m_strUserId!.length > 28)
							{
								let end = self.m_strUserId!.index(self.m_strUserId!.startIndex, offsetBy: 28)
								self.m_strUserId = String(self.m_strUserId![0...end])
							}
						}
					}
					else
					{
						DbUser.s_main!.setAlert(strAlert: "Token is empty")
						return
					}

					// If we have an id, connect or create an account.
					if (self.m_strUserId != nil && !self.m_strUserId!.isEmpty)
					{
						self.m_bmStatus |= Status.HAS_USER.rawValue

						// Try to get the user's display name.
						var strDisplayName = fbUser.displayName
						if (strDisplayName == nil || strDisplayName!.isEmpty)
						{
							strDisplayName = self.m_strName
						}
						else
						{
							self.m_strName = strDisplayName
						}

						pullData(strUserId: self.m_strUserId, strDisplayName: strDisplayName, bRecurse: true)
						DbUser.s_main!.setUserLogin()
					}
                }
		}
	}

	//**********************************************************************************************
	func
	setFinalGameScore(nScore: Int)
	{
		m_nRanking = (nScore + (m_nRanking * m_nGames)) / (m_nGames + 1)
		if (nScore > m_nHighScore)
		{
			m_nHighScore = nScore
		}
		m_nGames += 1
		pushData()
	}

	//**********************************************************************************************
	func
	setUserName(strDisplayName: String?) -> Bool
	{
		if let strDisplayName = strDisplayName
		{
			if (!strDisplayName.isEmpty)
			{
				// Block the use of JotRussell as a name.
                if (strDisplayName == DbUser.s_strNameRobot)
				{
					return false
				}
			
				m_strName = strDisplayName

				var mFields: [String: Any] = [:]
				mFields[DbUserField.strName.rawValue] = m_strName
                DbAccess.MergeData(strTable: DbUser.s_strDbTableName, strTitle: m_strUserId!, mData: mFields)
			}
		}
		return true
	}

	//**********************************************************************************************
	func
	userDelete()
	{
		m_fbUser!
            .delete
            {
                error in
                if let error = error
                {
                    DbUser.s_main!.setAlert(strAlert: "Error deleting account: \(error)\n")
                    return
                }

				DbUser.s_fbAuth = nil
                self.m_bmStatus &= ~Status.HAS_USER.rawValue
				DbUser.s_main!.setAlert(strAlert: "Your account has been deleted")
				DbUser.s_main!.updateState(bAnimate: false)
			}
	}

	//**********************************************************************************************
	func
	userDelete(
		strEmail: String,
		strPassword: String)
	{
		s_fbAuth
			.signInWithEmailAndPassword(strEmail, strPassword)
			.addOnCompleteListener
			{
				docSnapshot,
				error in
				guard let doc = docSnapshot
				else
				{
					self.m_bmStatus = Status.HAS_CONNECTION.rawValue
					self.m_bmStatus |= Status.HAS_FAILURE.rawValue
					self.m_eState = State.NO_DOCUMENT
					DbUser.s_main.setAlert(strAlert: "Login failed: \(error!)\n")
					DbUser.s_main.updateState(bAnimate: false)
					return
				}
				self.m_bmStatus = Status.HAS_CONNECTION.rawValue
				self.m_fbUser = DbUser.s_fbAuth.getCurrentUser()
				userDelete()
			}
	}

	//**********************************************************************************************
	func
	getInviteState() -> OpponentState
	{
		if (m_eOpponentState == .USER_SEARCHING && m_dbOpponent != nil)
		{
			let eState = m_dbOpponent.m_eOpponentState
			if (eState == .USER_FOUND)
			{
				m_dbOpponent.m_eOpponentState = .USER_WAITING
				DbGame.PostGame(dbUser: this, dbOpponent: m_dbOpponent)
			}
			
			return eState
		}
		return m_eOpponentState
	}
	
	//**********************************************************************************************
	func
	userInvite(strEmail: String?) -> String
	{
		m_eOpponentState = .USER_START

		// Make sure the email is not empty.
		if (strEmail == nil || strEmail! == "")
		{
			m_eOpponentState = .USER_EMPTY;
			return nil
		}

		// Make sure the player didn't mistakenly provide their own email.
		if (strEmail == getUserEmail())
		{
			m_eOpponentState = .USER_SAME
			return nil
		}

		// Create an encoding of the email.
		s_strOpponentId = createEncoding(strEmail: strEmail)

		// Check to see if we already have a game active.
		if (DbGame.HaveGameWith(strOpponentId: s_strOpponentId))
		{
			m_eOpponentState = .USER_ACTIVE
		}
		else
		{
			// Create a DbUser() object which will search for an existing user.
			m_dbOpponent = nil
			m_eOpponentState = .USER_SEARCHING
			m_dbOpponent = GetDbUser(strUserId: s_strOpponentId, mFields: null)
		}
		return s_strOpponentId
	}

	//**********************************************************************************************
	func
	userLogin(accessToken: AccessToken?)
	{
		if let accessToken = accessToken
		{
			let authCred = FacebookAuthProvider.getCredential(accessToken.getToken())

			s_fbAuth
				.signInWithCredential(authCred)
				.addOnCompleteListener
				{
					docSnapshot,
					error in
					guard let doc = docSnapshot
					else
					{
						self.m_eState = State.NO_DOCUMENT
						self.m_bmStatus |= Status.HAS_FAILURE.rawValue
						// If sign in fails, display a message to the user.
						DbUser.s_main.setAlert(strAlert: "Login failed: \(error!)\n")
						DbUser.s_main.setState(eState: .USER_LOGIN)
						DbUser.s_main.updateState(bAnimate: false)
						return
					}

					self.m_bmStatus = Status.HAS_CONNECTION.rawValue
					// Sign in success, update UI with the signed-in user's information
					self.m_fbUser = DbUser.s_fbAuth.getCurrentUser()
					requestUserTokenId()
					DbUser.s_main!.updateState(bAnimate: false)
				}
		}
		else
		{
            DbUser.s_main!.setAlert(strAlert: "Login failed: Null access token");
		}
	}

	//**********************************************************************************************
	func
	userLogin(fbAuth: Auth)
	{
        if let fbUser = fbAuth.currentUser
		{
			if (m_fbUser == nil)
			{
                DbUser.s_fbAuth = fbAuth
				m_fbUser = fbUser
				requestUserTokenId()
				DbUser.s_main!.updateState(bAnimate: false)
			}
		}
	}

	//**********************************************************************************************
	func
    userLogin(account: GoogleSignInAccount)
	{
		if let account = account
		{
			// Get a Firebase credential from Facebook.
			let authCred = GoogleAuthProvider.getCredential(account.getIdToken(), nil)

			// Attempt login.
			s_fbAuth
				.signInWithCredential(authCred)
				{
					authResult,
					error in
					guard let user = authResult?.user
					else
					{
						m_eState = State.NO_DOCUMENT
						m_bmStatus |= Status.HAS_FAILURE.rawValue
						// If sign in fails, display a message to the user.
						DbUser.s_main!.setAlert(strAlert: "Login failed: \(error!)\n")
						return
					}

					self.m_bmStatus = Status.HAS_CONNECTION;
					self.m_fbUser = user
					if (self.m_fbUser != nil)
					{
						self.requestUserTokenId()
					}
					DbUser.s_main!.updateState(bAnimate: false)
				}
		}
		else
		{
			DbUser.s_main!.setAlert(strAlert: "Login failed: Null account")
		}
	}

	//**********************************************************************************************
	func
	userLogin(
		strEmail: String,
		strPassword: String,
		strDisplayName: String)
	{
		DbUser.s_fbAuth?
            .signIn(withEmail: strEmail, password: strPassword)
			{
				authResult,
				error in
				guard let user = authResult?.user
				else
				{
					self.m_eState = .NO_DOCUMENT
					self.m_bmStatus |= Status.HAS_FAILURE.rawValue
					// If sign in fails, display a message to the user.
					DbUser.s_main!.setAlert(strAlert: "Login failed: \(error!)\n")
					DbUser.s_main!.setState(eState: .USER_LOGIN)
					DbUser.s_main!.updateState(bAnimate: false)
                	return
				}

				// Block the use of JotRussell as a name.
				if (!strDisplayName.isEmpty &&
                    strDisplayName != DbUser.s_strNameRobot)
				{
					self.m_strName = strDisplayName;
				}
				self.m_bmStatus = Status.HAS_CONNECTION.rawValue

				// Sign in success, update UI with the signed-in user's information
				self.m_fbUser = user
				self.requestUserTokenId()
        	}
	}

	//**********************************************************************************************
	func
	userRegister(
		strEmail: String,
		strPassword: String,
		strDisplayName: String)
	{
		DbUser.s_fbAuth?
			.createUser(withEmail: strEmail, password: strPassword)
			{
				authResult,
				error in
				guard let user = authResult?.user
				else
				{
					print("DbUser.userRegister() error: \(error!)\n")
					self.m_eState = .NO_DOCUMENT
					self.m_bmStatus |= Status.HAS_FAILURE.rawValue
					// If sign in fails, display a message to the user.
					DbUser.s_main?.setAlert(strAlert: "Registration failed")
					DbUser.s_main?.setState(eState: .USER_LOGIN)
					DbUser.s_main?.updateState(bAnimate: false)
                	return
				}
				
				self.m_bmStatus = Status.HAS_CONNECTION.rawValue

				// Sign in success, update UI with the signed-in user's information
				self.m_fbUser = user
				self.requestUserTokenId()
        	}
	}

	//**********************************************************************************************
	func
	userReset(strEmail: String)
	{
		s_fbAuth
			.sendPasswordResetEmail(strEmail)
			.addOnCompleteListener
			{
				authResult,
				error in
				guard let user = authResult?.user
				else
				{
					DbUser.s_main!.setAlert(strAlert: "Password reset error: \(error!)\n")
					return
				}

				DbUser.s_main!.setAlert(strAlert: "Password reset email sent")
				DbUser.s_main!.updateState(bAnimate: false)
			}
	}
}

//**************************************************************************************************
