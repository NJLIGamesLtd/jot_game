/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Screen view to allow user to control personal settings.
 */

package com.jot.jot.android;

import com.jot.jot.R;
import com.jot.jot.crossword.Main;

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

public class SetupView extends ViewGroup
{
	private final TextView m_tvHeader;
	private final EditText m_etNickname;

	//**********************************************************************************************
	public
	SetupView(MainActivity mainActivity)
	{
		super(mainActivity);

		// Set the background screen.
		setBackgroundResource(R.drawable.jot_login);
		
		// Set the layout parameters.
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		setLayoutParams(lp);

		// Get the font multiplier
		float dFontMultiplier = mainActivity.getFontMultiplier();

		m_tvHeader = new TextView(mainActivity);
		m_tvHeader.setLayoutParams(lp);
		m_tvHeader.setTextSize(16 * dFontMultiplier);
		addView(m_tvHeader);

		TextView prompt = new TextView(mainActivity);
		prompt.setLayoutParams(lp);
		prompt.setText("Nickname:");
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
	getUserSetup()
	{
		Map<String, Object> mSetup = new HashMap<>();
		mSetup.put("strNickname", m_etNickname.getText().toString().trim());

		return mSetup;
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

	//**********************************************************************************************
	public void
	setDetails(Main main)
	{
		String strHeader = "Account: " + main.getUserEmail();
		m_tvHeader.setText(strHeader);

		String strName = main.getUserName();
		if (strName != null &&
			!strName.isEmpty() &&
			!strName.equals("Anonymous"))
		{
			m_etNickname.setText(strName);
		}
	}
}

//**************************************************************************************************
