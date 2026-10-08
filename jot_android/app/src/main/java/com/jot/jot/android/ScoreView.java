/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * A ScoreView is an Android View of a player score.
 */

package com.jot.jot.android;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

//**************************************************************************************************

public class ScoreView extends ViewGroup
{
	private static int s_nDigitWidth;

	private final MainActivity m_mainActivity;
	private int m_nScore;
	private final int m_iPlayer;

	//**********************************************************************************************
	public
	ScoreView(
		Context context,
		int nScore,
		int iPlayer)
	{
		super(context);
		m_mainActivity = (MainActivity)context;
		m_iPlayer = iPlayer;

		// Set this view object's layout parameters.
		setLayoutParams(new WindowManager.LayoutParams());

		// Force the displayed score to be updated when we set it.
		m_nScore = -1;
		setScore(nScore);
	}

	//**********************************************************************************************
	public void
	bumpScore(int nScore)
	{
		for (int nDigit = 1000; nDigit > 0; nDigit /= 10)
		{
			if (nScore >= m_nScore + nDigit)
			{
				m_nScore += nDigit;
				break;
			}
		}
		updateScore();
	}

	//**********************************************************************************************
	public int
	getScore()
	{
		return m_nScore;
	}

	//**********************************************************************************************
	public void
	decrementScore()
	{
		// Decrement the score.
		--m_nScore;
		updateScore();
	}

	//**********************************************************************************************
	public void
	eraseScore()
	{
		// Erase the score.
		removeAllViews();
		m_nScore = -1;
		setScore(0);
	}

	//**********************************************************************************************
	public void
	incrementScore()
	{
		// Increment the score.
		++m_nScore;
		updateScore();
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
		// If needed, get the size of each digit.
		if (s_nDigitWidth == 0)
		{
			s_nDigitWidth = yEnd - yStart;
		}
		
		// Set our layout params.
		WindowManager.LayoutParams params = (WindowManager.LayoutParams)getLayoutParams();
		params.x = xStart;
		params.y = yStart;

		// If we're the alternate player, have the score right justified.
		int nChildren = getChildCount();
		if (nChildren > 0)
		{
			params.width = (nChildren * s_nDigitWidth);
			xStart = xEnd - params.width;

			// Display each digit in our score.
			for (int i = 0; i < nChildren; ++i)
			{
				View child = getChildAt(i);
				child.measure(s_nDigitWidth, s_nDigitWidth);
				child.layout(
					xStart,
					0,
					xStart + s_nDigitWidth,
					s_nDigitWidth);
			
				xStart += s_nDigitWidth;
			}
		}
		else
		{
			params.width = s_nDigitWidth;
		}
		params.height = s_nDigitWidth;
		setLayoutParams(params);

		measure(params.width, params.height);
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
	setScore(int nScore)
	{
		if (m_nScore != nScore)
		{
			m_nScore = nScore;
			updateScore();
		}
	}

	//**********************************************************************************************
	public void
	updateScore()
	{
		// Don't display the initial pre-game score of -1.
		if (m_nScore < 0)
		{
			return;
		}

		// Get the score digits.
		String strDigits = Integer.toString(m_nScore);
		int nDigits = strDigits.length();

		// Update the digit views to include each within the score.
		int i = 0;
		int nChildren = getChildCount();
		while (i < nChildren || i < nDigits)
		{
			if (i < nChildren)
			{
				DigitView digit = (DigitView)getChildAt(i);

				if (i < nDigits)
				{
					digit.setDigit(strDigits.charAt(i), m_iPlayer);
					++i;
				}
				else
				{
					removeView(digit);
					--nChildren;
				}
			}
			else
			{
				DigitView digit = new DigitView(m_mainActivity, strDigits.charAt(i), m_iPlayer);
				addView(digit);
				++i;

				WindowManager.LayoutParams params = (WindowManager.LayoutParams)getLayoutParams();
				params.width += s_nDigitWidth;
				if (m_iPlayer > 0 && params.x > 0)
				{
					params.x -= s_nDigitWidth;
				}
				setLayoutParams(params);

				if (params.height > 0)
				{
					measure(params.width, params.height);
				}
			}
		}
	}
}

//**************************************************************************************************
