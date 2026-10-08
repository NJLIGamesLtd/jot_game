//**************************************************************************************************
//
//  TreeWords.swift
//  
//
//  Created by Stephen Kennedy on 5/7/20.
//
//**************************************************************************************************

import Foundation

//**************************************************************************************************

public class Taboo
{
	//**********************************************************************************************

	static let s_sTaboos: Set =
	[
		"dick",
		"cock",
		"cunt",
		"fuck",
		"nigger",
		"pussy",
		"suck",
		"vagina",
	]

	//**********************************************************************************************
	static func
	IsTaboo(strWord: String) -> Bool
	{
		return s_sTaboos.contains(strWord.lowercased())
	}

	//**********************************************************************************************
	static func
	ContainsTaboo(strMessage: String) -> Bool
	{
		let strMessage = strMessage.lowercased()

		for strFragment in Taboo.s_sTaboos
		{
			if (strMessage.contains(strFragment))
			{
				return true
			}
		}
		return false
	}
}

//**************************************************************************************************
