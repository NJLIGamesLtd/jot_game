/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Database access to netword games.
 */

package com.jot.jot.db;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.jot.jot.android.MainActivity;

import java.util.*;

//**************************************************************************************************

public class DbAccess
{
	private static FirebaseFirestore s_fbStore;
	private static MainActivity s_mainActivity;
	private static int s_nSuccess;

	//**********************************************************************************************
	public static void
	Initialize(MainActivity mainActivity)
	{
		if (mainActivity != null)
		{
			s_mainActivity = mainActivity;
		}
		
		// Initialize Firebase
		if (s_fbStore == null)
		{
			s_fbStore = FirebaseFirestore.getInstance();
		}
	}

	//**********************************************************************************************
	public static CollectionReference
	GetCollectionReference(String strNode)
	{
		Initialize(null);
		return s_fbStore.collection(strNode);
	}

	//**********************************************************************************************
	public static DocumentReference
	GetDocumentReference(String strNode)
	{
		Initialize(null);
		return s_fbStore.document(strNode);
	}

	//**********************************************************************************************
	public static String
	AddData(
		String strTable,
		Map<String, Object> mData)
	{
		s_nSuccess = 0;
		DocumentReference doc = s_fbStore.collection(strTable).document();
		doc
			.set(mData)
			.addOnSuccessListener(new OnSuccessListener<Void>()
			{
				@Override
				public void
				onSuccess(Void data)
				{
					++s_nSuccess;
					s_mainActivity.updateState(false);
				}
			})
			.addOnFailureListener(new OnFailureListener()
			{
				@Override
				public void
				onFailure(@NonNull Exception e)
				{
					--s_nSuccess;
					s_mainActivity.updateState(false);
				}
			});

		return doc.getId();
	}

	//**********************************************************************************************
	public static void
	Delete(DocumentReference docRef)
	{
		// Delete the document.
		if (docRef != null)
		{
			docRef
				.delete()
				.addOnSuccessListener(
					aVoid ->
					{
					}
				)
				.addOnFailureListener(
					e ->
					{
					}
				);
		}
	}

	//**********************************************************************************************
	public static boolean
	IsFailure()
	{
		return s_nSuccess < 0;
	}

	//**********************************************************************************************
	public static boolean
	IsSuccess()
	{
		return s_nSuccess > 0;
	}

	//**********************************************************************************************
	public static void
	MergeData(
		String strTable,
		String strTitle,
		Map<String, Object> mData)
	{
		s_nSuccess = 0;
		s_fbStore
			.collection(strTable)
			.document(strTitle)
			.set(mData, SetOptions.merge())
			.addOnSuccessListener(new OnSuccessListener<Void>()
			{
				@Override
				public void
				onSuccess(Void data)
				{
					++s_nSuccess;
					s_mainActivity.updateState(false);
				}
			})
			.addOnFailureListener(new OnFailureListener()
			{
				@Override
				public void
				onFailure(@NonNull Exception e)
				{
					--s_nSuccess;
					s_mainActivity.updateState(false);
				}
			});
	}

	//**********************************************************************************************
	public static void
	SetData(
		String strTable,
		String strTitle,
		Map<String, Object> mData)
	{
		s_nSuccess = 0;
		s_fbStore
			.collection(strTable)
			.document(strTitle)
			.set(mData)
			.addOnSuccessListener(new OnSuccessListener<Void>()
			{
				@Override
				public void
				onSuccess(Void data)
				{
					++s_nSuccess;
					s_mainActivity.updateState(false);
				}
			})
			.addOnFailureListener(new OnFailureListener()
			{
				@Override
				public void
				onFailure(@NonNull Exception e)
				{
					--s_nSuccess;
					s_mainActivity.updateState(false);
				}
			});
	}
}

//**************************************************************************************************
