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
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

//**************************************************************************************************

public class MessageView extends ViewGroup
{
	private final MainActivity m_mainActivity;
	private final InputMethodManager m_inputManager;
	
	private final EditText m_etCompose;
	private boolean m_bCompose;
	private String m_strCompose;

	private final Button m_butReceive;
	private boolean m_bReceive;

	private int m_nWidth;
	private int m_nHeight;

	//**********************************************************************************************
	public
	MessageView(MainActivity mainActivity)
	{
		super(mainActivity);
		m_mainActivity = mainActivity;
		m_inputManager =
			(InputMethodManager)mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
	
		// Set the layout parameters.
		WindowManager.LayoutParams layout = new WindowManager.LayoutParams();
		setLayoutParams(layout);

		// Get the font multiplier
		float dFontMultiplier = mainActivity.getFontMultiplier();

		// Create a edit text window for sending messages.
		m_etCompose = new EditText(mainActivity);
		m_etCompose.setTextSize(20 * dFontMultiplier);
		m_etCompose.setBackgroundResource(R.drawable.message);
		m_etCompose.setLayoutParams(layout);
		m_etCompose.setOnKeyListener((v, keyCode, event) ->
		{
			// If the event is a key-down event on the "enter" button
			if (event.getAction() == KeyEvent.ACTION_DOWN &&
				keyCode == KeyEvent.KEYCODE_ENTER)
			{
				// Perform action on key press.
				m_strCompose = m_etCompose.getText().toString().trim();
				m_mainActivity.updateState(false);
				return true;
			}
			return false;
		});
		m_etCompose.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void
			beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
			{
			}

			@Override
			public void
			onTextChanged(CharSequence charSequence, int i, int i1, int i2)
			{
			}

			@Override
			public void
			afterTextChanged(Editable editable)
			{
				int nLength = editable.length();
				if (nLength > 0)
				{
					if (editable.charAt(nLength - 1) == '\n')
					{
						editable.delete(nLength - 1, nLength);
						m_strCompose = m_etCompose.getText().toString().trim();
						m_mainActivity.updateState(false);
					}
				}
			}
		});
		m_etCompose.setOnClickListener
		(
			view ->
			{
				hideKeyboard();
				m_etCompose.setText("");
				removeView(m_etCompose);
				m_bCompose = false;
				m_mainActivity.updateState(false);
			}
		);

		// Create simple button for the receive message.
		m_butReceive = new Button(mainActivity);
		m_butReceive.setBackgroundResource(R.drawable.message);
		m_butReceive.setLayoutParams(layout);
		m_butReceive.setAutoSizeTextTypeUniformWithConfiguration(
			16, 20, 2, TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
		m_butReceive.setGravity(Gravity.FILL);
		m_butReceive.setAllCaps(false);
		m_butReceive.setOnClickListener
		(
			view ->
			{
				removeView(m_butReceive);
				m_bReceive = false;
				m_mainActivity.updateState(false);
			}
		);
	}

	//**********************************************************************************************
	public String
	getComposeMessage()
	{
		if (m_bCompose)
		{
			if (m_strCompose != null)
			{
				hideKeyboard();
				removeView(m_etCompose);
				m_bCompose = false;
				return m_strCompose;
			}
		}
		return null;
	}

	//**********************************************************************************************
	public int
	getMessageHeight()
	{
		return m_nHeight;
	}

	//**********************************************************************************************
	public int
	getMessageWidth()
	{
		return m_nWidth;
	}

	//**********************************************************************************************
	public void
	hideKeyboard()
	{
		m_etCompose.clearFocus();
		m_inputManager.hideSoftInputFromWindow(m_etCompose.getWindowToken(), 0);
	}

	//**********************************************************************************************
	public boolean
	isDone()
	{
		return !(m_bCompose || m_bReceive);
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
		int nChildren = getChildCount();
		for (int i = 0; i < nChildren; ++i)
		{
			View child = getChildAt(i);
			child.measure(m_nWidth, m_nHeight);
			child.layout(0, 0, m_nWidth, m_nHeight);

			if (child == m_etCompose)
			{
				if (m_etCompose.requestFocus())
				{
					m_inputManager.showSoftInput(m_etCompose, InputMethodManager.SHOW_IMPLICIT);
				}
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
		params.height = nHeight;
		setLayoutParams(params);
		setMeasuredDimension(params.width, params.height);
	}

	//**********************************************************************************************
	public void
	setCompose()
	{
		setWidthHeight();
		m_etCompose.measure(m_nWidth, m_nHeight);
		m_etCompose.setText("");
		m_etCompose.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
		addView(m_etCompose);
		m_strCompose = null;
		m_bCompose = true;
	}

	//**********************************************************************************************
	public void
	setReceiveMessage(String strMessage)
	{
		setWidthHeight();
		m_butReceive.measure(m_nWidth, m_nHeight);
		m_butReceive.setText("\n" + strMessage);
		addView(m_butReceive);
		m_bReceive = true;
	}

	//**********************************************************************************************
	private void
	setWidthHeight()
	{
		if (m_nWidth == 0)
		{
			m_nWidth = (int)(0.9 * m_mainActivity.getWidth());
			m_nHeight = m_mainActivity.getHeight() / 4;
		}
	}
}

//**************************************************************************************************
