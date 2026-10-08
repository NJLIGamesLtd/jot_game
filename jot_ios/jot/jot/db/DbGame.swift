//**************************************************************************************************
//
//  DbGame.swift
//  
//
//  Created by Stephen Kennedy on 5/7/20.
//
//**************************************************************************************************

import Foundation
import Firebase

//**************************************************************************************************

class DbGame
{
	static let s_strDbTableName: String = "games"

	static var s_dbRefGames: CollectionReference? = nil
    static var s_mDbGames: [String: [DbGame]] = [:]
    static var s_mDbOpponents: [String: DbUser] = [:]
    static var s_sGamesLoaded: Set<String> = []
	static var s_nGamesLoaded = 0
	static var s_nGamesLoading = 0
	static var s_nGamesWaiting = 0
	static var s_dbUser: DbUser? = nil
	static var s_dbRobot: DbUser? = nil
	static var s_bLoading = false
	static var s_bOpponentSearching = false
	static var s_main: Main? = nil
	static var s_iPlayer = 0
	static var s_nQueued = 0

	//**********************************************************************************************
	enum DbGameField: String
	{
		case bmSettings = "bmSettings"
		case iLastSquare1 = "iLastSquare1"
		case iLastSquare2 = "iLastSquare2"
		case iTurn = "iTurn"
		case nLastScore1 = "nLastScore1"
		case nLastScore2 = "nLastScore2"
		case nScore1 = "nScore1"
		case nScore2 = "nScore2"
		case strInvalids = "strInvalids"
		case strMove = "strMove"
		case strRack1 = "strRack1"
		case strRack2 = "strRack2"
		case strUserId1 = "strUserId1"
		case strUserId2 = "strUserId2"
		case strUserMsg1 = "strUserMsg1"
		case strUserMsg2 = "strUserMsg2"
		case timeStamp = "timeStamp"
	}

	//**********************************************************************************************

	var m_dbGame: DbGame? = nil
	var m_dbOpponent: DbUser? = nil
	var m_dbUser: DbUser? = nil
	var m_aDbUsers: [DbUser] = []
	var m_docRef: DocumentReference? = nil
	var m_strInvalids: String? = nil
	var m_sInvalids: Set<String> = []
	var m_strGameId: String = ""
	var m_strMove: String = ""
	var m_aRacks: [String] = []
	var m_aUserIds: [String] = []
	var m_aUserMsgs: [String] = []
	var m_bPlayed = false
	var m_bRobotGame = false
	var m_bUpdated = false
	var m_bmSettings = 0
	var m_iPlayer = 0
	var m_iTurn = 0
	var m_date = Date()
	var m_aLastScores: [Int] = []
	var m_aLastSquares: [Int] = []
	var m_aScores: [Int] = []

	//**********************************************************************************************
	static func
	Delete(docRef: DocumentReference)
	{
		// Delete the document.
		docRef.delete()
	}

	//**********************************************************************************************
	static func
	Initialize(
		main: Main,
		dbUser: DbUser)
	{
		s_main = main
		s_dbUser = dbUser
		s_dbRobot = DbUser.GetDbUser(strUserId: DbUser.GetRobotUserId(), mFields: null)
        s_dbRefGames = DbAccess.GetCollectionReference(strNode: s_strDbTableName)
	}

	//**********************************************************************************************
	static func
	CreateRandomGame()
	{
		// Trigger a search for a random opponent.
		s_bOpponentSearching = true
		s_nGamesLoading = s_nGamesLoaded
        s_dbUser!.findRandomOpponent(mDbOpponents: s_mDbOpponents)
	}

	//**********************************************************************************************
	static func
	CreateRobotGame()
	{
        let strGameId: String = PostGame(dbUser1: s_dbUser!, dbUser2: s_dbRobot!)
        let dbGame: DbGame = DbGame(
            strGameId: strGameId, dbUser: s_dbUser!, dbOpponent: s_dbRobot!, mFields: nil)
		var lDbGames: [DbGame] = []
		lDbGames[0] = dbGame
        s_mDbGames[s_dbRobot!.getUserId()!] = lDbGames
	}

	//**********************************************************************************************
	static func
	HaveGameWith(strUserId: String) -> Bool
	{
		if let mDbOpponents = s_mDbOpponents
		{
			return mDbOpponents.containsKey(strUserId)
		}
		return false
	}

	//**********************************************************************************************
	static func
	LoadComplete(iPlayer: Int) -> Bool
	{
		// Add a short delay to allow other processes to check for load completion.
		Thread.sleep(forTimeInterval: 10)

		// If we're still loading the games, return false.
		if (s_bLoading)
		{
			return false
		}
		
		// Continue to wait for all the games are completely loaded.
		if (s_nGamesLoaded < DbGame.s_nGamesLoading)
		{
			return false
		}

		// This group of games has been loaded.
		return true
	}

	//**********************************************************************************************
	static func
	LoadGame(
		iPlayer: Int,
		strOpponentId: String)
	{
		s_bLoading = true
		s_iPlayer = iPlayer

		// Create a listener for our opponent.
		CollectionReference games = DbAccess.GetCollectionReference(s_strDbTableName)
		games
			.whereEqualTo(DbGameField.GetUserIdEnum(iPlayer).toString(), s_dbUser.getUserId())
			.whereEqualTo(DbGameField.GetUserIdEnum(iPlayer ^ 1).toString(), strOpponentId)
			.get()
			.addOnSuccessListener(
				querySnapshot ->
				{
					LoadGames(
						querySnapshot: querySnapshot,
						iPlayer: s_iPlayer,
						bBlockRobot: true)
					s_bLoading = false
					s_main!.updateState(false)
				}
			)
			.addOnFailureListener(
				e ->
				{
					s_main!.failMessage(
						strMessage: "LoadGame() onFailure(): \(error!)")
					s_bLoading = false
				}
			);
	}

	//**********************************************************************************************
	static func
	LoadGames(iPlayer: Int)
	{
		s_bLoading = true
		s_iPlayer = iPlayer
		var eUserId = DbGameField.strUserId1;
		if (s_iPlayer > 0)
		{
			eUserId = DbGameField.strUserId2;
		}

		if let strUserId = s_dbUser!.getUserId()
		{
			// Create a listener for our opponent.
        	s_dbRefGames!
				.whereField(eUserId.rawValue, isEqualTo: strUserId)
				.addSnapshotListener
				{
					querySnapshot,
            		error in
                    guard querySnapshot != nil
            		else
            		{
                        s_main!.failMessage(
                            strMessage: "LoadGames() onFailure(): \(error!)")
						s_bLoading = false
                		return
            		}

					if let querySnapshot = querySnapshot
					{
						LoadGames(
							querySnapshot: querySnapshot,
							iPlayer: s_iPlayer,
							bBlockRobot: true)
						s_bLoading = false
						s_main!.updateState(bAnimate: false)

						print(
							"************************ LoadGames.onSuccess() complete " +
							String(s_nGamesLoaded) + " out of " + String(s_mDbGames.count))
					}
				}
		}
	}

	//**********************************************************************************************
	static func
	LoadRandomComplete() -> Bool
	{
		// Add a short delay to allow other processes to check for load completion.
		Thread.sleep(forTimeInterval: 10)

		// If we haven't posted the game yet, see if we first found our opponent.
		if (s_bOpponentSearching)
		{
            if let dbOpponent = s_dbUser!.getOpponent()
			{
                if (dbOpponent !== DbGame.s_dbRobot)
				{
                	_ = PostGame(dbUser1: s_dbUser!, dbUser2: dbOpponent)
				}
                LoadGames(iPlayer: 0)
                s_bOpponentSearching = false
			}
			return false
		}

		// If we're still loading the game, return false.
		if (s_bLoading)
		{
			return false
		}

		// Continue to wait until the game is completely loaded.
		if (s_nGamesLoaded < s_nGamesLoading)
		{
			return false
		}

		// We should be done loading the new random game.
		return true
	}

	//**********************************************************************************************
	static func
	LoadRobotComplete() -> Bool
	{
		// Add a short delay to allow other processes to check for load completion.
		Thread.sleep(forTimeInterval: 10)

		// If we're still loading the games, return false.
		if (s_bLoading)
		{
			return false
		}
		
		// Continue to wait for all the games are completely loaded.
		if (s_nGamesLoaded < s_nGamesLoading)
		{
			return false
		}

		// If we don't have it, load our default robot game.
        if (s_mDbGames[s_dbRobot!.getUserId()!] == nil)
		{
			CreateRobotGame()
			return false
		}

		// We have our robot game.
		return true
	}

	//**********************************************************************************************
	static func
	LoadRobotGame()
	{
		s_bLoading = true
		s_iPlayer = 0

		// If we are starting the load from first robot game, clear previous loads.
        s_mDbGames.removeAll(keepingCapacity: true)
		s_mDbOpponents.removeAll(keepingCapacity: true)
		s_sGamesLoaded.removeAll(keepingCapacity: true)

		var idUser = ""
		if let strUserId = s_dbUser!.getUserId()
		{
			idUser = strUserId
		}
		var idRobot = ""
		if let strRobotId = s_dbRobot!.getUserId()
		{
			idRobot = strRobotId
		}

		// Create a listener for our opponent.

		s_dbRefGames!
			.whereField(DbGameField.strUserId1.rawValue, isEqualTo: idUser)
			.whereField(DbGameField.strUserId2.rawValue, isEqualTo: idRobot)
			.addSnapshotListener
			{
				querySnapshot,
            	error in
                guard querySnapshot != nil
            	else
            	{
                    s_main!.failMessage(
                        strMessage: "LoadRobotGame() onFailure(): \(error!)")
					s_bLoading = false
            		return
            	}

				if let querySnapshot = querySnapshot
				{
					LoadGames(
						querySnapshot: querySnapshot,
						iPlayer: s_iPlayer,
						bBlockRobot: false)
					s_bLoading = false
					s_main!.updateState(bAnimate: false)

					print(
						"************************ LoadRobotGame.onSuccess() complete " +
						String(s_nGamesLoaded) + " out of " + String(s_mDbGames.count))
				}
			}
	}

	//**********************************************************************************************
	static func
	LoadGames(
		querySnapshot: QuerySnapshot,
		iPlayer: Int,
		bBlockRobot: Bool)
	{
        let strOpponentField: String = GetUserIdEnum(iPlayer: iPlayer ^ 1).rawValue

		// For now, run a brute force search for our id.
        let lSnapshots: [DocumentSnapshot] = querySnapshot.documents
		let nGames = lSnapshots.count

		for iGame in 0..<nGames
		{
			let snapshot: DocumentSnapshot = lSnapshots[iGame]

			// If we have already loaded this game, skip it.
			let strGameId: String = snapshot.documentID
			if (s_sGamesLoaded.contains(strGameId))
			{
				continue
			}

			// Ignore if our fields are null.
			let mFields: [String: Any]? = snapshot.data()
			if (mFields == nil)
			{
				continue
			}

			// Get the opponent id.
            let strOpponentId = mFields![strOpponentField] as! String

			// If we're blocking robot games to be loaded, skip over them.
			if (bBlockRobot)
			{
                if (s_dbRobot!.getUserId() == strOpponentId)
				{
					continue
				}
			}

			// Get the document reference in case we need to delete the game.
            let docRef: DocumentReference = s_dbRefGames!.document(strGameId)

			// Delete erroneous games we have already blocked.
            if (s_dbUser!.isBlocked(strOpponentId: strOpponentId))
			{
                Delete(docRef: docRef)
				continue
			}

			// Delete games that are more than 28 days old.
			var dateLast = Date()
            if let timeLast = mFields![DbGameField.timeStamp.rawValue] as? Timestamp
			{
                dateLast = timeLast.dateValue()
			}
            if let dateOld = Calendar.current.date(byAdding: .day, value: -28, to: Date())
            {
                if (dateLast < dateOld)
                {
                    DbGame.Delete(docRef: docRef)
                    continue
                }
			}

			// Ignore games that are over which we have already seen its outcome.
            let iTurn = mFields![DbGameField.iTurn.rawValue] as! Int
			if (iTurn == -1 - iPlayer)
			{
				continue
			}
				
			// Map each list of games by the opponent id.
			var lDbGames: [DbGame]? = s_mDbGames[strOpponentId]
			if (lDbGames == nil)
			{
				// Create the array list for this opponent.
				lDbGames = []
                s_mDbGames[strOpponentId] = lDbGames
			}
			// Make sure we haven't already loaded this game.
			else
			{
				var bFound = false
				for dbTest in lDbGames!
				{
					if (strGameId == dbTest.getGameId())
					{
						bFound = true
						break
					}
				}
				if (bFound)
				{
					continue
				}
			}

			// Try to get the DbUser for our opponent.
			var dbOpponent: DbUser?
            if (s_dbRobot!.getUserId() == strOpponentId)
			{
				dbOpponent = s_dbRobot
			}
			else
			{
				dbOpponent = s_mDbOpponents[strOpponentId]
			}
			if (dbOpponent == nil)
			{
				// Create a new DbUser for our opponent and add it to our map.
                dbOpponent = DbUser.GetDbUser(strUserId: strOpponentId, mFields: nil)
				s_mDbOpponents[strOpponentId] = dbOpponent
			}

			// Create a new database connection for this game.
            let dbGame: DbGame = DbGame(
                strGameId: strGameId, dbUser: s_dbUser!, dbOpponent: dbOpponent!, mFields: mFields)

			// Add this game to our active set.
			s_sGamesLoaded.insert(strGameId)

			// If this game is marked for deletion, make sure the user gets to see
			// the outcome by listing it first.
			var iIndex = lDbGames!.count
			if (iTurn < 0)
			{
				iIndex = 0
			}
			// Else, if there are multiple games with the same player, push the most recent first.
			else if (iIndex > 0)
			{
				iIndex = 0
                for dbTest in lDbGames!
				{
					if (dbTest.getPlayerTurn() >= 0 && dateLast < dbTest.getDate())
					{
						break
					}
					iIndex += 1
				}
			}

			// Add the game to the list.
			lDbGames![iIndex] = dbGame
		}
	}

	//**********************************************************************************************
	static func
	PostGame(dbGame: DbGame) -> String
	{
		return PostGame(
            strUserId1: dbGame.getPlayerId(),
            strUserId2: dbGame.getOpponentId(),
            bmSettings: dbGame.getSettings())
	}

	//**********************************************************************************************
	static func
	PostGame(dbUser1: DbUser, dbUser2: DbUser) -> String
	{
		return PostGame(
            strUserId1: dbUser1.getUserId()!,
            strUserId2: dbUser2.getUserId()!,
            bmSettings: dbUser1.getSettings() & dbUser2.getSettings())
	}

	//**********************************************************************************************
	static func
	PostGame(
		strUserId1: String,
		strUserId2: String,
		bmSettings: Int
	) -> String
	{
		// Indicate we have another game we are waiting to load.
		s_nGamesWaiting += 1
		if (s_nGamesLoading == 0)
		{
			s_nGamesLoading = s_nGamesLoaded
		}
		s_nGamesLoading += 1

		// Create a new document to be added to the games table.
        var mGame: [String: Any] = [:]
		mGame[DbGameField.bmSettings.rawValue] = bmSettings
		mGame[DbGameField.iTurn.rawValue] = 0
		mGame[DbGameField.iLastSquare1.rawValue] = 0
		mGame[DbGameField.iLastSquare2.rawValue] = 0
		mGame[DbGameField.nLastScore1.rawValue] = 0
		mGame[DbGameField.nLastScore2.rawValue] = 0
		mGame[DbGameField.nScore1.rawValue] = 0
		mGame[DbGameField.nScore2.rawValue] = 0
		mGame[DbGameField.strInvalids.rawValue] = ""
		mGame[DbGameField.strMove.rawValue] = ""
		mGame[DbGameField.strRack1.rawValue] = ""
		mGame[DbGameField.strRack2.rawValue] = ""
		mGame[DbGameField.strUserId1.rawValue] = strUserId1
		mGame[DbGameField.strUserId2.rawValue] = strUserId2
		mGame[DbGameField.strUserMsg1.rawValue] = ""
		mGame[DbGameField.strUserMsg2.rawValue] = ""
		mGame[DbGameField.timeStamp.rawValue] = Timestamp()

		// Asynchronously add our new game.
        return DbAccess.AddData(strTable: s_strDbTableName, mData: mGame)
	}

	//**********************************************************************************************
	static func
	GetRackEnum(iPlayer: Int) -> DbGameField
	{
		if (iPlayer == 0)
		{
			return .strRack1
		}
		return .strRack2
	}

	//**********************************************************************************************
	static func
	GetScoreEnum(iPlayer: Int) -> DbGameField
	{
		if (iPlayer == 0)
		{
			return .nScore1
		}
		return .nScore2
	}

	//**********************************************************************************************
	static func
	GetUserIdEnum(iPlayer: Int) -> DbGameField
	{
		if (iPlayer == 0)
		{
			return .strUserId1
		}
		return .strUserId2
	}

	//**********************************************************************************************
	static func
	GetUserMsgEnum(iPlayer: Int) -> DbGameField
	{
		if (iPlayer == 0)
		{
			return .strUserMsg1
		}
		return .strUserMsg2
	}

	//**********************************************************************************************
	init(
		strGameId: String,
		dbUser: DbUser,
		dbOpponent: DbUser,
		mFields: [String: Any]?)
	{
		// Store our game id and use this to get a reference to the database entry.
		m_strGameId = strGameId
        m_docRef = DbGame.s_dbRefGames!.document(strGameId)

		// Assign our document details based on the data returned.
		if (mFields != nil)
		{
            initialize(mFields: mFields)
			m_iPlayer = dbUser.getUserId() == m_aUserIds[0] ? 0 : 1
		}
		else
		{
			m_iPlayer = 0
		}

		// Determine if we're the first or second player.
		m_dbUser = dbUser
		m_dbOpponent = dbOpponent
		m_aDbUsers[m_iPlayer] = dbUser
		m_aDbUsers[m_iPlayer ^ 1] = dbOpponent

		// The second player is the robot, mark it as such.
        if (dbOpponent === DbGame.s_dbRobot)
		{
			m_bRobotGame = true
		}
		m_bPlayed = false

		// Try to load our opponent from the database.
		_ = getOpponent()

		// Once this game is fully loaded, send it over to Main.
		m_dbGame = self

		// If the game document is present, set up a listener.
		m_docRef?.addSnapshotListener
        {
            docSnapshot,
            error in
            guard let doc = docSnapshot
            else
            {
                DbGame.s_main!.failMessage(
                    strMessage: "DbGame onComplete() failed: \(error!)")
                return
            }
            guard let data = doc.data()
            else
            {
                DbGame.s_main!.failMessage(
                    strMessage: "DbGame onComplete() document empty")
                return
            }
            self.initialize(mFields: data)
			
			if (!self.m_bRobotGame &&
				self.m_iTurn == self.m_iPlayer)
			{
				if (self.m_bPlayed)
				{
					self.m_bPlayed = false
					let strNote = self.getOpponentName() + " played you."
                    DbGame.s_main!.setNotification(strNote: strNote)
				}
			}
			
			while (true)
			{
				DbGame.s_nQueued += 1
				if (DbGame.s_nQueued < 2)
				{
					break
				}
				Thread.sleep(forTimeInterval: 1)
				DbGame.s_nQueued -= 1
			}
			let bAdded = DbGame.s_main!.addDbGame(dbGame: self.m_dbGame!)
			DbGame.s_nQueued -= 1
            if (bAdded)
			{
	            if (self.m_iTurn != self.m_iPlayer)
				{
	                self.m_bPlayed = true
				}
			}
			
			// Trigger the first update.
            DbGame.s_main!.updateState(bAnimate: false)

			// Add our game.
			if (bAdded)
			{
				DbGame.s_mDbOpponents[self.m_dbOpponent!.getUserId()!] = self.m_dbOpponent
				DbGame.s_nGamesLoaded += 1
				if (DbGame.s_nGamesWaiting > 0)
				{
					DbGame.s_nGamesWaiting -= 1
				}

				// Trigger another update.
                DbGame.s_main!.updateState(bAnimate: false)
			}
		}
/*
		// If the game document is present, set up a listener.
		m_docRef?.addSnapshotListener
        {
            docSnapshot,
            error in
            guard let doc = docSnapshot
            else
            {
                DbGame.s_main!.failMessage(
                    strMessage: "DbGame onSnapshot() failed: \(error!)")
                return
            }
            guard let data = doc.data()
            else
            {
                DbGame.s_main!.failMessage(
                    strMessage: "DbGame onSnapshot() document empty")
                return
            }
            self.initialize(mFields: data)

			if (!self.m_bRobotGame &&
				self.m_iTurn == self.m_iPlayer)
			{
				if (self.m_bPlayed)
				{
					self.m_bPlayed = false
					let strNote = getOpponentName() + " played you."
					DbGame.s_main!.setNotification(strNote)
				}
			}
            DbGame.s_main!.updateState(bAnimate: true)
		}
*/
	}

	//**********************************************************************************************
	func
	blockOpponent()
	{
		m_dbUser!.blockOpponent(dbUser: m_dbOpponent!)
	}

	//**********************************************************************************************
	func
	clearInvalids()
	{
		m_sInvalids.removeAll(keepingCapacity: true)
	}

	//**********************************************************************************************
	func
	delete()
	{
		// No need to delete robot games.  It'll just be reset.
		if (isRobot())
		{
			return
		}

		// Delete this from our static list of open games.
		let strOpponentId: String = getOpponentId()
        if var lDbGames = DbGame.s_mDbGames[strOpponentId]
		{
			for index in 0..<lDbGames.count
			{
				if (lDbGames[index] === self)
				{
					lDbGames.remove(at: index)
					break
				}
			}
			if (lDbGames.count == 0)
			{
				DbGame.s_mDbGames[strOpponentId] = nil
				DbGame.s_mDbOpponents[strOpponentId] = nil
			}
		}

		// Have both players notified of the results before the game is deleted.
		if (m_iTurn >= 0)
		{
			m_iTurn = -1 - m_iPlayer
            var mFields: [String: Any] = [:]
			mFields[DbGameField.iTurn.rawValue] = m_iTurn
            DbAccess.MergeData(strTable: DbGame.s_strDbTableName, strTitle: m_strGameId, mData: mFields)
			return
		}

		// If we already marked this game for deletion, wait for the other user to delete it.
		if (m_iTurn == -1 - m_iPlayer)
		{
			return
		}

		// Else, delete the document.
        DbGame.Delete(docRef: m_docRef!)
	}

	//**********************************************************************************************
	func
	getDate() -> Date
	{
		return m_date
	}

	//**********************************************************************************************
	func
	getGameId() -> String
	{
		return m_strGameId
	}

	//**********************************************************************************************
	func
	getHighScore(iPlayer: Int) -> Int
	{
		return m_aDbUsers[iPlayer].getHighScore()
	}

	//**********************************************************************************************
	func
	getLastScore(iPlayer: Int) -> Int
	{
		return m_aLastScores[iPlayer]
	}

	//**********************************************************************************************
	func
	getLastSquare(iPlayer: Int) -> Int
	{
		return m_aLastSquares[iPlayer]
	}

	//**********************************************************************************************
	func
	getInvalids() -> String?
	{
		// If we have new invalids from the other player, wipe and return them.
		if let strInvalids = m_strInvalids
		{
			if (!strInvalids.isEmpty &&
                strInvalids.first != Character(UnicodeScalar(0x30 + m_iPlayer)!))
			{
				// Save a copy for return before clearing our local copy.
                let start = strInvalids.index(strInvalids.startIndex, offsetBy: 1)
                let strInvalids = String(strInvalids[start...])
				m_strInvalids = nil
				return strInvalids
			}
		}
		return nil
	}

	//**********************************************************************************************
	func
	getMove() -> String
	{
		return m_strMove
	}

	//**********************************************************************************************
	func
	getOpponent() -> DbUser
	{
		if (m_dbOpponent == nil)
		{
            m_dbOpponent = m_dbUser!.getOpponent(strOpponentId: m_aUserIds[m_iPlayer ^ 1])
		}
        return m_dbOpponent!
	}

	//**********************************************************************************************
	func
	getOpponentId() -> String
	{
		return m_aUserIds[m_iPlayer ^ 1]
	}

	//**********************************************************************************************
	func
	getOpponentMessage() -> String?
	{
		let strMessage = m_aUserMsgs[m_iPlayer ^ 1]
        if (!strMessage.isEmpty)
        {
            pushMessage(iPlayer: m_iPlayer ^ 1, strMsg: "");
            return strMessage
		}
		return nil
	}

	//**********************************************************************************************
	func
	getOpponentName() -> String
	{
		return m_aDbUsers[m_iPlayer ^ 1].getUserName()
	}

	//**********************************************************************************************
	func
	getPlayer() -> Int
	{
		return m_iPlayer
	}

	//**********************************************************************************************
	func
	getPlayerId() -> String
	{
		return m_aUserIds[m_iPlayer]
	}

	//**********************************************************************************************
	func
	getPlayerName() -> String
	{
		return m_aDbUsers[m_iPlayer].getUserName()
	}

	//**********************************************************************************************
	func
	getPlayerTurn() -> Int
	{
		return m_iTurn
	}

	//**********************************************************************************************
	func
	getRack() -> String
	{
		return m_aRacks[m_iPlayer]
	}

	//**********************************************************************************************
	func
	getRack(iPlayer: Int) -> String
	{
		return m_aRacks[iPlayer]
	}

	//**********************************************************************************************
	func
	getRanking(iPlayer: Int) -> Int
	{
		return m_aDbUsers[iPlayer].getRanking()
	}

	//**********************************************************************************************
	func
	getScore(iPlayer: Int) -> Int
	{
		return m_aScores[iPlayer]
	}

	//**********************************************************************************************
	func
	getScoreUser() -> Int
	{
		return m_aScores[m_iPlayer]
	}

	//********************************************************************!*************************
	func
	getScoreOpponent() -> Int
	{
		return m_aScores[m_iPlayer ^ 1]
	}

	//**********************************************************************************************
	func
	getSettings() -> Int
	{
		return m_bmSettings
	}

	//**********************************************************************************************
	func
	getUser() -> DbUser
	{
        return m_dbUser!
	}

	//**********************************************************************************************
	func
	getUserId(iPlayer: Int) -> String
	{
		return m_aUserIds[iPlayer]
	}

	//**********************************************************************************************
	func
	getUserMessage() -> String?
	{
		let strMessage = m_aUserMsgs[m_iPlayer]
		if (!strMessage.isEmpty)
		{
            pushMessage(strMsg: "")
            return strMessage
		}
		return nil
	}

	//**********************************************************************************************
	func
	getUserName(iPlayer: Int) -> String
	{
		return m_aDbUsers[iPlayer].getUserName()
	}

	//**********************************************************************************************
	func
	initialize(mFields: [String: Any]?)
	{
		if let mFields = mFields
		{
			m_aScores[0] = mFields[DbGameField.nScore1.rawValue] as! Int
			m_aScores[1] = mFields[DbGameField.nScore2.rawValue] as! Int
			m_aUserIds[0] = mFields[DbGameField.strUserId1.rawValue] as! String
			m_aUserIds[1] = mFields[DbGameField.strUserId2.rawValue] as! String
			m_aUserMsgs[0] = mFields[DbGameField.strUserMsg1.rawValue] as! String
			m_aUserMsgs[1] = mFields[DbGameField.strUserMsg2.rawValue] as! String
			m_bmSettings = mFields[DbGameField.bmSettings.rawValue] as! Int
			m_iTurn = mFields[DbGameField.iTurn.rawValue] as! Int
			m_aLastSquares[0] = mFields[DbGameField.iLastSquare1.rawValue] as! Int
			m_aLastSquares[1] = mFields[DbGameField.iLastSquare2.rawValue] as! Int
			m_aLastScores[0] = mFields[DbGameField.nLastScore1.rawValue] as! Int
			m_aLastScores[1] = mFields[DbGameField.nLastScore2.rawValue] as! Int
			m_strMove = mFields[DbGameField.strMove.rawValue] as! String
			m_aRacks[0] = mFields[DbGameField.strRack1.rawValue] as! String
			m_aRacks[1] = mFields[DbGameField.strRack2.rawValue] as! String
            m_strInvalids = mFields[DbGameField.strInvalids.rawValue] as? String

			// Get the date of the last move.
            if let timeStamp = mFields[DbGameField.timeStamp.rawValue] as? Timestamp
			{
				m_date = timeStamp.dateValue()
			}
			else
			{
				m_date = Date()
			}

			m_bUpdated = true
		}
	}

	//**********************************************************************************************
	func
	isFailure() -> Bool
	{
		return DbAccess.IsFailure()
	}

	//**********************************************************************************************
	func
	isMyTurn() -> Bool
	{
		return m_iTurn == m_iPlayer || m_iTurn == -1 - (m_iPlayer ^ 1)
	}

	//**********************************************************************************************
	func
	isOpponent(strUserId: String) -> Bool
	{
		if let dbOpponent = m_dbOpponent
		{
			if (dbOpponent.getUserId() == strUserId)
			{
				return true
			}
		}
		return false
	}

	//**********************************************************************************************
	func
	isPushed() -> Bool
	{
		return DbAccess.IsSuccess()
	}

	//**********************************************************************************************
	func
	isRobot() -> Bool
	{
        return DbGame.s_dbRobot!.getUserId() == m_aUserIds[1]
	}

	//**********************************************************************************************
	func
	isUpdated() -> Bool
	{
		if (m_bUpdated)
		{
			m_bUpdated = false
			return true
		}
		return false
	}

	//**********************************************************************************************
	func
	pushData()
	{
        var mFields: [String: Any] = [:]

		mFields[DbGameField.bmSettings.rawValue] = m_bmSettings
		mFields[DbGameField.iTurn.rawValue] = m_iTurn
		mFields[DbGameField.iLastSquare1.rawValue] = m_aLastSquares[0]
		mFields[DbGameField.iLastSquare2.rawValue] = m_aLastSquares[1]
		mFields[DbGameField.nLastScore1.rawValue] = m_aLastScores[0]
		mFields[DbGameField.nLastScore2.rawValue] = m_aLastScores[1]
		mFields[DbGameField.nScore1.rawValue] = m_aScores[0]
		mFields[DbGameField.nScore2.rawValue] = m_aScores[1]
		if (m_sInvalids.isEmpty)
		{
			mFields[DbGameField.strInvalids.rawValue] = ""
		}
		else
		{
            let cPlayer = Character(UnicodeScalar(0x30 + m_iPlayer)!)
			var strInvalids = String(cPlayer)
			var bFirst = true
			for strInvalid in m_sInvalids
			{
				if (!bFirst)
				{
					strInvalids.append(", ")
				}
				bFirst = false
				strInvalids.append(strInvalid)
			}
			mFields[DbGameField.strInvalids.rawValue] = strInvalids
			clearInvalids()
		}
		mFields[DbGameField.strMove.rawValue] = m_strMove
		mFields[DbGameField.strRack1.rawValue] = m_aRacks[0]
		mFields[DbGameField.strRack2.rawValue] = m_aRacks[1]
		mFields[DbGameField.strUserId1.rawValue] = m_aUserIds[0]
		mFields[DbGameField.strUserId2.rawValue] = m_aUserIds[1]
		mFields[DbGameField.strUserMsg1.rawValue] = m_aUserMsgs[0]
		mFields[DbGameField.strUserMsg2.rawValue] = m_aUserMsgs[1]
		mFields[DbGameField.timeStamp.rawValue] = Timestamp()

        DbAccess.SetData(strTable: DbGame.s_strDbTableName, strTitle: m_strGameId, mData: mFields)
	}

	//**********************************************************************************************
	func
	pushMessage(strMsg: String)
	{
		pushMessage(iPlayer: m_iPlayer, strMsg: strMsg)
	}

	//**********************************************************************************************
	func
	pushMessage(
		iPlayer: Int,
		strMsg: String)
	{
        if (!Taboo.ContainsTaboo(strMessage: strMsg))
		{
			m_aUserMsgs[iPlayer] = strMsg

        	var mFields: [String: Any] = [:]
            mFields[DbGame.GetUserMsgEnum(iPlayer: iPlayer).rawValue] = strMsg
            DbAccess.MergeData(strTable: DbGame.s_strDbTableName, strTitle: m_strGameId, mData: mFields)
		}
	}

	//**********************************************************************************************
	func
	pushRack(
		iPlayer: Int,
		strRack: String)
	{
		m_aRacks[iPlayer] = strRack;

        var mFields: [String: Any] = [:]
        mFields[DbGame.GetRackEnum(iPlayer: iPlayer).rawValue] = strRack
        DbAccess.MergeData(strTable: DbGame.s_strDbTableName, strTitle: m_strGameId, mData: mFields)
	}

	//**********************************************************************************************
	func
	setLastScore(
		iPlayer: Int,
		nLastScore: Int)
	{
		m_aLastScores[iPlayer] = nLastScore
	}

	//**********************************************************************************************
	func
	setLastSquare(
		iPlayer: Int,
		iLastSquare: Int)
	{
		m_aLastSquares[iPlayer] = iLastSquare
	}

	//**********************************************************************************************
	func
	setInvalids(sInvalids: Set<String>)
	{
		for strInvalid in sInvalids
		{
			m_sInvalids.insert(strInvalid)
		}
	}

	//**********************************************************************************************
	func
	setMessage(
		strMsg: String,
		iPlayer: Int)
	{
        if (!Taboo.ContainsTaboo(strMessage: strMsg))
		{
			m_aUserMsgs[iPlayer] = strMsg
		}
	}

	//**********************************************************************************************
	func
	setMove(strMove: String)
	{
		m_strMove = strMove
	}

	//**********************************************************************************************
	func
	setPlayerTurn(iTurn: Int)
	{
		m_iTurn = iTurn
		if (m_iTurn != m_iPlayer)
		{
			m_bPlayed = true
		}
	}

	//**********************************************************************************************
	func
	setRack(strRack: String)
	{
		m_aRacks[m_iPlayer] = strRack
	}

	//**********************************************************************************************
	func
	setRack(
		iPlayer: Int,
		strRack: String)
	{
		m_aRacks[iPlayer] = strRack
	}

	//**********************************************************************************************
	func
	setScore(
		iPlayer: Int,
		nScore: Int)
	{
		m_aScores[iPlayer] = nScore
	}
}

//**************************************************************************************************
