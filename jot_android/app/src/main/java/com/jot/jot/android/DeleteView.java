/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * View for gaining login credentials for user account deletion.
 */

package com.jot.jot.android;

import com.jot.jot.R;

import android.text.Editable;
import android.text.InputType;
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

public class DeleteView extends ViewGroup
{
	//**********************************************************************************************
	public
	DeleteView(MainActivity mainActivity)
	{
		super(mainActivity);

		// Set the background screen.
		setBackgroundResource(R.drawable.user_delete);
		
		// Set the layout parameters.
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		setLayoutParams(lp);
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
		int height = (yEnd - yStart) >> 3;
		yStart += height;

		int nChildren = getChildCount();
		for (int i = 0; i < nChildren; ++i)
		{
			View child = getChildAt(i);
			if (child instanceof EditText)
			{
				child.measure(width, height);
				child.layout(xStart, yStart, xStart + width, yStart + height + 5);
				yStart += height + 5;
			}
			else if (child instanceof TextView)
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
