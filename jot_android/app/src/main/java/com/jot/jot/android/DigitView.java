/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Stores an arrangement of a crossword com.jot.game board.
 */

package com.jot.jot.android;

import com.jot.jot.R;

import android.content.Context;
import android.view.ViewGroup;
import android.view.WindowManager;

//**************************************************************************************************

public class DigitView extends ViewGroup
{
	private static final int[][] s_idDigits =
	{
		{
			R.drawable.jot_white_0,
			R.drawable.jot_white_1,
			R.drawable.jot_white_2,
			R.drawable.jot_white_3,
			R.drawable.jot_white_4,
			R.drawable.jot_white_5,
			R.drawable.jot_white_6,
			R.drawable.jot_white_7,
			R.drawable.jot_white_8,
			R.drawable.jot_white_9
		},
		{
			R.drawable.jot_black_0,
			R.drawable.jot_black_1,
			R.drawable.jot_black_2,
			R.drawable.jot_black_3,
			R.drawable.jot_black_4,
			R.drawable.jot_black_5,
			R.drawable.jot_black_6,
			R.drawable.jot_black_7,
			R.drawable.jot_black_8,
			R.drawable.jot_black_9
		}
	};

	//**********************************************************************************************
	public
	DigitView(
		Context context,
		char cDigit,
		int iPlayer)
	{
		super(context);

		// Set this view object's layout parameters.
		setLayoutParams(new WindowManager.LayoutParams());

		setDigit(cDigit, iPlayer);
	}

	//**********************************************************************************************
	@Override
	protected void
	onLayout(
		boolean bChanged,
		int xStart,
		int yStart,
		int xEnd,
		int yEnd)
	{
	}
	
	//**********************************************************************************************
	@Override
    protected void
	onMeasure(
		int nWidth,
		int nHeight)
	{
		setMeasuredDimension(nWidth, nHeight);
	}

	//**********************************************************************************************
    public void
	setDigit(
		char cDigit,
		int iPlayer)
	{
		int iDigit = (int)(cDigit - '0');
		setBackgroundResource(s_idDigits[iPlayer][iDigit]);
	}
}

//**************************************************************************************************
