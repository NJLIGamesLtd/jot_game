/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Invite friend via email view screen.
 */

package com.jot.jot.android;

import com.jot.jot.R;
import com.jot.jot.crossword.Main;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

//**************************************************************************************************

public class InviteView extends ViewGroup
{
	private final EditText m_etEmail;

	//**********************************************************************************************
	public
	InviteView(MainActivity mainActivity)
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
		prompt.setText("Social media invites coming\nsoon.\nFriend's email:\n");
		prompt.setTextSize(16 * dFontMultiplier);
		addView(prompt);

		m_etEmail = new EditText(mainActivity);
		m_etEmail.setTextSize(16 * dFontMultiplier);
		m_etEmail.setPaddingRelative(10, 0, 10, 0);
		m_etEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		m_etEmail.setOnKeyListener((v, keyCode, event) ->
		{
			// If the event is a key-down event on the "enter" button
			if (event.getAction() == KeyEvent.ACTION_DOWN &&
				keyCode == KeyEvent.KEYCODE_ENTER)
			{
				// Perform action on key press.
				InputMethodManager imm =
					(InputMethodManager)mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(m_etEmail.getWindowToken(), 0);
				mainActivity.setAction(Main.Action.CONFIRM);
				return true;
			}
			return false;
		});
		addView(m_etEmail);		
	}
	
	//**********************************************************************************************
	public Map<String, Object>
	getInviteEmail()
	{
		Map<String, Object> mLogin = new HashMap<>();
		mLogin.put("strEmail", m_etEmail.getText().toString().toLowerCase().trim());

		return mLogin;
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
				child.layout(xStart, yStart, xStart + width, yStart + 3 * height);
				yStart += 3 * height;
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
