/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Email/password login view screen.
 */

package com.jot.jot.android;

import com.jot.jot.R;

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

public class EmailView extends ViewGroup
{
	private final EditText m_etEmail;
	private final EditText m_etPassword;
	private final EditText m_etNickname;

	private final InputMethodManager m_inputManager;

	//**********************************************************************************************
	public
	EmailView(MainActivity mainActivity)
	{
		super(mainActivity);
		m_inputManager =
			(InputMethodManager)mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);

		// Set the background screen.
		setBackgroundResource(R.drawable.jot_login);
		
		// Set the layout parameters.
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		setLayoutParams(lp);

		// Get the font multiplier
		float dFontMultiplier = mainActivity.getFontMultiplier();

		TextView prompt = new TextView(mainActivity);
		prompt.setLayoutParams(lp);
		prompt.setText("Email address:");
		prompt.setTextSize(16 * dFontMultiplier);
		addView(prompt);

		m_etEmail = new EditText(mainActivity);
		m_etEmail.setTextSize(16 * dFontMultiplier);
		m_etEmail.setPaddingRelative(10, 0, 10, 0);
		m_etEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		m_etEmail.setImeOptions(EditorInfo.IME_ACTION_NEXT);
		m_etEmail.setImeActionLabel("ENTER", KeyEvent.KEYCODE_ENTER);
		addView(m_etEmail);

		prompt = new TextView(mainActivity);
		prompt.setLayoutParams(lp);
		prompt.setText("Jot password:");
		prompt.setTextSize(16 * dFontMultiplier);
		addView(prompt);
		
		m_etPassword = new EditText(mainActivity);
		m_etPassword.setTextSize(16 * dFontMultiplier);
		m_etPassword.setPaddingRelative(10, 0, 10, 0);
		m_etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
		m_etPassword.setImeOptions(EditorInfo.IME_ACTION_NEXT);
		addView(m_etPassword);

		prompt = new TextView(mainActivity);
		prompt.setLayoutParams(lp);
		prompt.setText("Nickname (for new users):");
		prompt.setTextSize(16 * dFontMultiplier);
		addView(prompt);
		
		m_etNickname = new EditText(mainActivity);
		m_etNickname.setTextSize(16 * dFontMultiplier);
		m_etNickname.setPaddingRelative(10, 0, 10, 0);
		m_etNickname.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
		m_etNickname.setImeOptions(EditorInfo.IME_ACTION_NEXT);
		addView(m_etNickname);
	}

	//**********************************************************************************************
	public Map<String, Object>
	getUserLogin()
	{
		Map<String, Object> mLogin = new HashMap<>();
		mLogin.put("strEmail", m_etEmail.getText().toString().toLowerCase().trim());
		mLogin.put("strPassword", m_etPassword.getText().toString().trim());
		mLogin.put("strNickname", m_etNickname.getText().toString().trim());

		// Hide the keyboard.
		m_etEmail.clearFocus();
		m_etPassword.clearFocus();
		m_etNickname.clearFocus();
		m_inputManager.hideSoftInputFromWindow(m_etEmail.getWindowToken(), 0);
		m_inputManager.hideSoftInputFromWindow(m_etPassword.getWindowToken(), 0);
		m_inputManager.hideSoftInputFromWindow(m_etNickname.getWindowToken(), 0);

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
