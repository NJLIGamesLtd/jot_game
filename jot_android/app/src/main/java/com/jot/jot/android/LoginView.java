/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Stores an arrangement of a crossword com.jot.game board.
 */

package com.jot.jot.android;

import com.jot.jot.R;

import android.text.Editable;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

//**************************************************************************************************

public class LoginView extends ViewGroup
{
	//**********************************************************************************************
	public
	LoginView(MainActivity mainActivity)
	{
		super(mainActivity);

		// Set the background screen.
		setBackgroundResource(R.drawable.jot_login);
		
		// Set the layout parameters.
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		setLayoutParams(lp);

		// Get the font multiplier
		float dFontMultiplier = mainActivity.getFontMultiplier();

		TextView prompt = new TextView(mainActivity);
		prompt.setLayoutParams(lp);
		prompt.setText(
			"Please pick a method of login\n" +
			"to play against others, or\n" +
			"press play to verse the computer.");
		prompt.setGravity(Gravity.CENTER);
		prompt.setTextSize(16 * dFontMultiplier);
		addView(prompt);
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
		int width = 8 * (xEnd - xStart) / 10;
		int height = (yEnd - yStart) >> 1;
		yStart += height >> 4;

		int nChildren = getChildCount();
		for (int i = 0; i < nChildren; ++i)
		{
			View child = getChildAt(i);
			if (child instanceof TextView)
			{
				child.measure(width, height);
				child.layout(xStart, yStart, xStart + width, yStart + height);
				yStart += height;
			}
		}
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
}

//**************************************************************************************************
