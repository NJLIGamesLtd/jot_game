//**************************************************************************************************
//
//  DbAccess.swift
//  
//
//  Created by Stephen Kennedy on 5/7/20.
//
//**************************************************************************************************

import Foundation
import Firebase

//**************************************************************************************************

class DbAccess
{
	static var s_fbStore: Firestore? = nil
	static var s_main: Main? = nil
	static var s_nSuccess = 0

	//**********************************************************************************************
	static func
	Initialize(main: Main?)
	{
		if (main != nil)
		{
			s_main = main
		}
		
		// Initialize Firebase
		if (s_fbStore == nil)
		{
            s_fbStore = Firestore.firestore()
		}
	}

	//**********************************************************************************************
	static func
	GetCollectionReference(strNode: String) -> CollectionReference
	{
		Initialize(nil)
        return s_fbStore!.collection(strNode)
	}

	//**********************************************************************************************
	static func
	GetDocumentReference(strNode: String) -> DocumentReference
	{
		Initialize(nil)
        return s_fbStore!.document(strNode)
	}

	//**********************************************************************************************
	static func
	AddData(
		strTable: String,
		mData: [String: Any]
	) -> String
	{
		s_nSuccess = 0
        let doc: DocumentReference = s_fbStore!.collection(strTable).document()
        doc
			.setData(mData)
			{
				err in
				if let err = err
				{
					print("Error writing document: \(err)")
					s_nSuccess -= 1
					s_main!.updateState(false)
				}
				else
				{
					print("Document written")
					s_nSuccess += 1
					s_main!.updateState(false)
				}
			}

		return doc.documentID
	}

	//**********************************************************************************************
	static func
	IsFailure() -> Bool
	{
		return s_nSuccess < 0
	}

	//**********************************************************************************************
	static func
	IsSuccess() -> Bool
	{
		return s_nSuccess > 0
	}

	//**********************************************************************************************
	static func
	MergeData(
		strTable: String,
		strTitle: String,
		mData: [String: Any])
	{
		s_nSuccess = 0
        s_fbStore!
			.collection(strTable)
			.document(strTitle)
			.set(mData, SetOptions.merge())
			{
				err in
				if let err = err
				{
					print("Error writing document: \(err)")
					s_nSuccess -= 1
					s_main!.updateState(false)
				}
				else
				{
					print("Document written")
					s_nSuccess += 1
					s_main!.updateState(false)
				}
			}
	}

	//**********************************************************************************************
	static func
	SetData(
		strTable: String,
		strTitle: String,
		mData: [String: Any])
	{
		s_nSuccess = 0
        s_fbStore!
			.collection(strTable)
			.document(strTitle)
			.setData(mData)
			{
				err in
				if let err = err
				{
					print("Error writing document: \(err)")
					s_nSuccess -= 1
					s_main!.updateState(false)
				}
				else
				{
					print("Document written")
					s_nSuccess += 1
					s_main!.updateState(false)
				}
			}
	}
}

//**************************************************************************************************
