/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Stores an arrangement of a crossword com.jot.game board.
 */

package com.jot.jot.android;

import com.jot.jot.R;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

//**************************************************************************************************

public class AlertView extends ViewGroup
{
	private final MainActivity m_mainActivity;

	private final Button m_butAlert;
	private boolean m_bAlert;

	private int m_nWidth;
	private int m_nHeight;

	//**********************************************************************************************
	public
	AlertView(MainActivity mainActivity)
	{
		super(mainActivity);
		m_mainActivity = mainActivity;

		// Set the layout parameters.
		WindowManager.LayoutParams layout = new WindowManager.LayoutParams();
		setLayoutParams(layout);

		// Get the font multiplier
		float dFontMultiplier = mainActivity.getFontMultiplier();

		// Create simple button for the receive message.
		m_butAlert = new Button(mainActivity);
		m_butAlert.setBackgroundResource(R.drawable.alert);
		m_butAlert.setLayoutParams(layout);
		m_butAlert.setAutoSizeTextTypeUniformWithConfiguration(
			(int)(16 * dFontMultiplier),
			(int)(20 * dFontMultiplier),
			2,
			TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
		m_butAlert.setGravity(Gravity.CENTER);
		m_butAlert.setAllCaps(false);
		m_bAlert = false;
		
		m_butAlert.setOnClickListener
		(
			view ->
			{
				m_bAlert = false;
				removeView(m_butAlert);
				m_mainActivity.updateState(false);
			}
		);
	}

	//**********************************************************************************************
	public int
	getAlertHeight()
	{
		return m_nHeight;
	}

	//**********************************************************************************************
	public int
	getAlertWidth()
	{
		return m_nWidth;
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
	setAlert(String strAlert)
	{
		if (!m_bAlert)
		{
			m_bAlert = true;
			removeAllViews();
			setWidthHeight();
			m_butAlert.measure(m_nWidth, m_nHeight);

			StringBuilder sbAlert = new StringBuilder(strAlert);
			for (int i = 16; i < sbAlert.length(); ++i)
			{
				if (sbAlert.charAt(i) == ' ')
				{
					sbAlert.setCharAt(i, '\n');
					i += 16;
				}
			}
			
			m_butAlert.setText(sbAlert.toString());
			addView(m_butAlert);
		}
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
