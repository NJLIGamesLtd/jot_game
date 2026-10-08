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
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;

//**************************************************************************************************

public class QwertyView extends ViewGroup
{
	private static final char[] s_aLetters =
	{
		'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P',
		'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L',
		'Z', 'X', 'C', 'V', 'B', 'N', 'M', 'M'
	};

	private static final int[] s_aImages =
	{
		R.drawable.jot_white_qwerty,
		R.drawable.jot_black_qwerty
	};
	
	private final GameView m_gameView;
	private int m_width;
	private int m_height;
	
	//**********************************************************************************************
	public
	QwertyView(
		Context context,
		GameView gameView)
	{
		super(context);

		m_gameView = gameView;

		// Set the layout parameters.
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		setLayoutParams(lp);
	}
	
	//**********************************************************************************************
	@Override
	public boolean
	onInterceptTouchEvent(MotionEvent event)
	{
		int keyWidth = (int)(m_width / 10.2);
		int keyHeight = (int)(m_height / 3.9);

		int x = (int)event.getX() - (int)(keyWidth / 7.5);
		int y = (int)event.getY() - (int)(keyHeight * .75);

		// Determine which, if any, key has been touched.
		if (x >= 0 && y >= 0)
		{
			int iLetter = 0;
			if (y >= keyHeight)
			{
				if (y < (keyHeight << 1))
				{
					x -= keyWidth >> 2;
					iLetter += 10;
				}
				else
				{
					iLetter += 19;
					x -= keyWidth >> 1;
				}
			}

			if (x >= (m_width >> 1))
			{
				iLetter += 5;
				x -= m_width >> 1;
			}
			while (x >= keyWidth)
			{
				x -= keyWidth;
				++iLetter;
			}

			m_gameView.setBlankLetter(s_aLetters[iLetter]);
		}

		return false;	
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
		m_width = xEnd - xStart;
		m_height = yEnd - yStart;
	}

	//**********************************************************************************************
	@Override
	protected void
	onMeasure(
		int nWidth,
		int nHeight)
	{
		LayoutParams params = (LayoutParams)getLayoutParams();
		params.width = nWidth;
		params.height = nWidth;
		setLayoutParams(params);
		setMeasuredDimension(params.width, params.height);
	}

	//**********************************************************************************************
	public void
	setPlayer(int iPlayer)
	{
		setBackgroundResource(s_aImages[iPlayer]);
	}
}

//**************************************************************************************************
