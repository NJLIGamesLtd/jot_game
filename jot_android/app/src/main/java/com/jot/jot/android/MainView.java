/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Stores an arrangement of a crossword com.jot.game board.
 */

package com.jot.jot.android;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.facebook.share.model.ShareHashtag;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareButton;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.common.SignInButton;
import com.jot.jot.R;
import com.jot.jot.crossword.*;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.text.LineBreaker;
import android.net.Uri;
import android.text.Layout;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//**************************************************************************************************

public class MainView extends ViewGroup
{
	private static final String s_strAbout = new StringBuilder()
		.append("Jot (ver 1.24) uses Google Firebase to\n")
		.append("store and manage user account data.\n")
		.append("Email is used for identification, but\n")
		.append("only nickname, high score and ranking\n")
		.append("is visible to other players.\n")
		.append("Jot does not share user data.\n")
		.toString();

	private static final int RC_SIGN_IN = 9001;

	static private final Map<Main.Action, Integer> s_mActionButtonResources = new HashMap();
	static
	{		
		s_mActionButtonResources.put(Main.Action.ABOUT, R.drawable.button_about);
		s_mActionButtonResources.put(Main.Action.CANCEL, R.drawable.button_cancel);
		s_mActionButtonResources.put(Main.Action.CONFIRM, R.drawable.button_confirm);
		s_mActionButtonResources.put(Main.Action.DISPLAY_BEST, R.drawable.button_best);
		s_mActionButtonResources.put(
			Main.Action.DRAW_BLACK_TILE, R.drawable.button_black_consonents);
		s_mActionButtonResources.put(Main.Action.DRAW_BLACK_VOWEL, R.drawable.button_black_vowels);
		s_mActionButtonResources.put(
			Main.Action.DRAW_WHITE_TILE, R.drawable.button_white_consonents);
		s_mActionButtonResources.put(Main.Action.DRAW_WHITE_VOWEL, R.drawable.button_white_vowels);
		s_mActionButtonResources.put(Main.Action.JUMBLE_RACK, R.drawable.button_jumble);
		s_mActionButtonResources.put(Main.Action.PLAY_AGAIN, R.drawable.button_play_again);
		s_mActionButtonResources.put(Main.Action.PLAY_GAME, R.drawable.button_play);
		s_mActionButtonResources.put(Main.Action.PLAY_WORD, R.drawable.button_play);
		s_mActionButtonResources.put(Main.Action.PRIVACY, R.drawable.button_privacy);
		s_mActionButtonResources.put(Main.Action.QUIT_GAME, R.drawable.button_quit);
		s_mActionButtonResources.put(Main.Action.RACK_TILES, R.drawable.button_rack);
		s_mActionButtonResources.put(Main.Action.ROTATE_BOARD, R.drawable.button_rotate);
		s_mActionButtonResources.put(Main.Action.SELECT, R.drawable.button_play);
		s_mActionButtonResources.put(Main.Action.SKIP_TURN, R.drawable.button_skip);
		s_mActionButtonResources.put(Main.Action.SWITCH_GAME, R.drawable.button_switch);
		s_mActionButtonResources.put(Main.Action.TERMS, R.drawable.button_terms);
		s_mActionButtonResources.put(Main.Action.UNZOOM_BOARD, R.drawable.button_unzoom);
		s_mActionButtonResources.put(Main.Action.USER_BLOCK, R.drawable.button_block);
		s_mActionButtonResources.put(Main.Action.USER_DELETE, R.drawable.button_delete_account);
		s_mActionButtonResources.put(Main.Action.USER_EMAIL, R.drawable.button_email);
		s_mActionButtonResources.put(Main.Action.USER_LOGIN, R.drawable.button_login);
		s_mActionButtonResources.put(Main.Action.USER_REGISTER, R.drawable.button_register);
		s_mActionButtonResources.put(Main.Action.USER_RESET, R.drawable.button_reset_password);
	}

	static private final Map<Main.State, Integer> s_mStateHelpResources = new HashMap();
	static
	{
		s_mStateHelpResources.put(Main.State.GAME_LOAD, R.drawable.welcome);
		s_mStateHelpResources.put(Main.State.GAME_LOAD_ROBOT, R.drawable.welcome);
		s_mStateHelpResources.put(Main.State.GAME_LOAD_ROBOT_WAITING, R.drawable.welcome);
		s_mStateHelpResources.put(Main.State.GAME_LOAD_PLAYER1, R.drawable.welcome);
		s_mStateHelpResources.put(Main.State.GAME_LOAD_PLAYER1_WAITING, R.drawable.welcome);
		s_mStateHelpResources.put(Main.State.GAME_LOAD_PLAYER2, R.drawable.welcome);
		s_mStateHelpResources.put(Main.State.GAME_LOAD_PLAYER2_WAITING, R.drawable.welcome);
		s_mStateHelpResources.put(Main.State.TURN_DRAW_FIRST, R.drawable.help_pick_tiles);
		s_mStateHelpResources.put(Main.State.TURN_START, R.drawable.help_turn_start);
		s_mStateHelpResources.put(Main.State.USER_DELETE, R.drawable.user_delete);
		s_mStateHelpResources.put(Main.State.USER_DELETE_WAITING, R.drawable.user_delete);
		s_mStateHelpResources.put(Main.State.USER_DELETED, R.drawable.user_deleted);
		s_mStateHelpResources.put(Main.State.USER_MESSAGE, R.drawable.welcome);
		s_mStateHelpResources.put(Main.State.USER_WELCOME, R.drawable.welcome);
		s_mStateHelpResources.put(Main.State.USER_WELCOME_WAITING, R.drawable.welcome);
	}

	private final Main m_main;
	private final MainActivity m_mainActivity;
	private final DeleteView m_deleteView;
	private final DeletedView m_deletedView;
	private final EmailView m_emailView;
	private final GameView m_gameView;
	private final InviteView m_inviteView;
	private final LoginView m_loginView;
	private final SetupView m_setupView;
    private final AdView m_adView;

	private final LayoutParams m_layout;

	private Main.State m_eState;

	private final Button m_butInfo;
	private final TextView m_tvStatus;

	private final AlertView m_alertView;

	private boolean m_bPopup;
	private boolean m_bQwerty;
	private boolean m_bUpdating;
	
	private int m_nInfoWidth;
	private int m_xInfo;
	private int m_yInfo;
	private int m_yPopup;
	private int m_yAd;

	private Map<Main.Action, Button> m_mButtons;
	private Map<Main.State, ImageView> m_mHelpViews;

	private int m_nButtons;			// This is the number of active buttons.
	private int m_yButtons;
	private int m_nWidthButton;
	private int m_nHeightButton;
	private int m_nHeightScreen;

	private int m_nDelay;
	private final float m_dFontMultiplier;

	private final LoginButton m_fbLogin;
	private final ShareLinkContent m_shareLink;
	private final ShareButton m_fbShare;

	private final SignInButton m_gLogin;
	
	//**********************************************************************************************
	public
	MainView(
		Main main,
		MainActivity mainActivity,
		CallbackManager callbackManager
	) throws Exception
	{
		super(mainActivity);
		m_main = main;
		m_mainActivity = mainActivity;

		// Don't know the height of the screen, excluding tool-bars and such until onLayout()
		// is called.
		m_nHeightScreen = 0;

		// Set the layout parameters.
		m_layout = new LayoutParams(
			LinearLayout.LayoutParams.WRAP_CONTENT,
			LinearLayout.LayoutParams.WRAP_CONTENT);
		setBackgroundResource(R.drawable.jot_background_board);
		setLayoutParams(m_layout);

		// Create a view of what will be the active game.  Need default sizes for all
		// games to match to.
		m_gameView = new GameView(
			mainActivity,
			main.getPlayerCount(),
			main.getBoardWidth(),
			main.getBoardHeight(),
			main.getRackSize());
		addView(m_gameView);

		// Set up the initial game.
		m_gameView.setGame(m_main.getGame(), m_main.getPlayer());

		// Get the font multiplier
		m_dFontMultiplier = mainActivity.getFontMultiplier();

		// Create the game-over screen.
		m_butInfo = new Button(mainActivity);
		m_butInfo.setBackgroundResource(R.drawable.alert);
		m_butInfo.setLayoutParams(m_layout);
		m_butInfo.setTextSize(14 * m_dFontMultiplier);
		m_butInfo.setAllCaps(false);
		m_butInfo.setOnClickListener
		(
			view ->
			{
				removeView(m_butInfo);
				m_main.setAction(Main.Action.DONE);
				animateUpdate();
			}
		);

		// Create the status message region.
		m_tvStatus = new TextView(mainActivity);
		m_tvStatus.setGravity(Gravity.LEFT);
		m_tvStatus.setTextColor(Color.BLACK);
		addView(m_tvStatus);

		/* Use for testing:
		// Create a banner ad for the bottom of the screen.
		List<String> testDevices = new ArrayList<>();
		testDevices.add(AdRequest.DEVICE_ID_EMULATOR);

		RequestConfiguration requestConfiguration
			= new RequestConfiguration.Builder()
			.setTestDeviceIds(testDevices)
			.build();
		MobileAds.setRequestConfiguration(requestConfiguration);
		*/
		
		m_adView = new AdView(mainActivity);
		// m_adView.setAdSize(AdSize.SMART_BANNER);
		int adWidth = (int)(mainActivity.getWidth() / mainActivity.getDensity());
		m_adView.setAdSize(
			AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(mainActivity, adWidth));
		m_adView.setAdUnitId("ca-app-pub-1246465467159410/6901525474");
		// FOR TESTING: m_adView.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
		AdRequest adRequest = new AdRequest
			.Builder()
			.build();
		m_adView.loadAd(adRequest);

		m_adView.setAdListener(
			new AdListener()
			{
				@Override
				public void
				onAdLoaded()
				{
					// Code to be executed when an ad finishes loading.
				}

				@Override
				public void
				onAdOpened()
				{
					// Code to be executed when an ad opens an overlay that
					// covers the screen.
				}

				@Override
				public void
				onAdClicked()
				{
					// Code to be executed when the user clicks on an ad.
				}

				@Override
				public void
				onAdClosed()
				{
					// Code to be executed when the user is about to return
					// to the app after tapping on an ad.
				}
			}
		);

		addView(m_adView);

		// Create the alert message region.
		m_alertView = new AlertView(mainActivity);
		m_alertView.setLayoutParams(m_layout);

		// Create the list of button images.
		createButtons();

		// TODO: Check uses this instead
		//m_fbLogin = (LoginButton)findViewById(R.id.login_button);
		m_fbLogin = new LoginButton(mainActivity);
		m_fbLogin.setPermissions(Arrays.asList(new String[]{"email"}));
		//m_fbLogin.setReadPermissions("email");

		m_fbLogin.registerCallback
		(
			callbackManager,
			new FacebookCallback<LoginResult>()
			{
				@Override
				public void
				onSuccess(LoginResult loginResult)
				{
					AccessToken token = loginResult.getAccessToken();
					m_main.setUserLogin(token);
				}

				@Override
				public void
				onCancel()
				{
					setAlert("Facebook login cancel.");
				}

				@Override
				public void
				onError(FacebookException exception)
				{
					setAlert("Facebook login error: " + exception.getMessage());
				}
			}
		);
/*
		LoginManager loginManager = LoginManager.getInstance();
		loginManager.logInWithReadPermissions(mainActivity, Arrays.asList(new String[]{"email", "user_friends"}));
		loginManager.registerCallback(
			mainActivity.getCallbackManager(),
			new FacebookCallback<LoginResult>()
			{
				@Override
				public void onSuccess(LoginResult loginResult)
				{
					AccessToken token = loginResult.getAccessToken();
					//setUserLogin(token);

					// Gather friends details.
					GraphRequestBatch batch = new GraphRequestBatch(
						GraphRequest.newMyFriendsRequest(
							token,
							new GraphRequest.GraphJSONArrayCallback()
							{
								@Override
								public void
								onCompleted(
									JSONArray jsonArray,
									GraphResponse response)
								{
									System.out.print("JSON Array: " + jsonArray);
									System.out.print("Response: " + response);
								}
							}
						)
					);
					batch.addCallback(
						new GraphRequestBatch.Callback()
						{
							@Override
							public void
							onBatchCompleted(GraphRequestBatch graphRequests)
							{
								System.out.print("GraphRequests: " + graphRequests);
							}
						}
					);
					batch.executeAsync();
				}

				@Override
				public void onCancel()
				{
					System.out.print("onCancel()");
				}

				@Override
				public void onError(FacebookException exception)
				{
					System.out.print("onError(): " + exception);
				}
			}
		);
*/
		m_gLogin = new SignInButton(mainActivity);
		m_gLogin.setSize(SignInButton.SIZE_ICON_ONLY);
		m_gLogin.setEnabled(true);

		// Create a sharable link for people to tell their friends.
		m_shareLink = new ShareLinkContent
			.Builder()
			.setContentUrl(Uri.parse("https://play.google.com/store/apps/details?id=com.jot.jot"))
			.setShareHashtag(
				new ShareHashtag.Builder()
				.setHashtag("#TheChessOfCrosswords")
				.build())
			.build();
		m_fbShare = new ShareButton(mainActivity);
		m_fbShare.setShareContent(m_shareLink);

		// Create the help views.
		createHelpViews();

		// Need a screen to delete the user from the database.
		m_deleteView = new DeleteView(mainActivity);
		m_deletedView = new DeletedView(mainActivity);

		// Need a login screen to connect user to the database.
		m_loginView = new LoginView(mainActivity);
		m_emailView = new EmailView(mainActivity);
		m_inviteView = new InviteView(mainActivity);

		// Need a user setup screen to update the database.
		m_setupView = new SetupView(mainActivity);
		m_bPopup = false;

		// Remember the changing state of the qwerty keyboard for blank tiles.
		m_bQwerty = false;

		// Introduce the welcome state.
		m_eState = Main.State.OFF;
		m_nDelay = 0;
		updateView();
	}

	//**********************************************************************************************
	public void
	animateUpdate()
	{
		Animation anim = animateUpdateStep();
		startAnimation(anim);
	}

	//**********************************************************************************************
	private Animation
	animateUpdateStep()
	{
		Animation anim = new Animation()
		{
			@Override
			protected void
			applyTransformation(
				float fTime,
				Transformation trans)
			{
				super.applyTransformation(fTime, trans);

				try
				{
					// Update our score while the robot thinks
					if (!updateView())
					{
						// Wait for the robot to complete.
						if (!m_main.isWorking())
						{
							// Update the score for both players.
							if (!updateView())
							{
								cancel();
								resetUpdate();
							}
						}
					}
					Thread.sleep(50);
				}
				catch (Exception e)
				{
					displayMessage("animateUpdateStep() error:" + e.getMessage());
					resetUpdate();
				}
			}
		};

		anim.setRepeatCount(-1);

		return anim;
	}

	//**********************************************************************************************
	private Button
	createButton(Main.Action eAction)
	{
		Button button = new Button(m_mainActivity);
		button.setBackgroundResource(s_mActionButtonResources.get(eAction));
		button.setBreakStrategy(LineBreaker.BREAK_STRATEGY_SIMPLE);
		button.setId(eAction.ordinal());
		button.layout(0, -m_nHeightButton, (int)(1.42 * m_nWidthButton), 0);

		button.setOnClickListener
		(
			view ->
			{
				try
				{
					Main.Action[] values = Main.Action.values();
					if (values == null || values.length == 0)
					{
						displayMessage("Main.Action.values() is empty!");
						return;
					}
					int id = view.getId();
					if (id >= values.length)
					{
						displayMessage("view.getId() is beyond Action list.");
						return;
					}
					setAction(values[id]);
				}
				catch (Exception e)
				{
					displayMessage("createButton() error:" + e.getMessage());
					return;
				}
			}
		);

		return button;
	}

	//**********************************************************************************************
	public void
	createButtons()
	{
		m_mButtons = new HashMap();
		for (Map.Entry<Main.Action, Integer> enResource : s_mActionButtonResources.entrySet())
		{
			Button button = createButton(enResource.getKey());
			button.setGravity(Gravity.CENTER);
			button.setTextSize(10 * m_dFontMultiplier);
			m_mButtons.put(enResource.getKey(), button);
		}
	}

	//**********************************************************************************************
	public void
	createHelpViews()
	{
		m_mHelpViews = new HashMap();
		for (Map.Entry<Main.State, Integer> enResource : s_mStateHelpResources.entrySet())
		{
			ImageView helpView = new ImageView(m_mainActivity);
			helpView.setImageResource(enResource.getValue());
			helpView.setLayoutParams(m_layout);
			m_mHelpViews.put(enResource.getKey(), helpView);
		}
	}

	//**********************************************************************************************
	public void
	displayMessage(String strMessage)
	{
		m_mainActivity.failMessage(strMessage);
	}

	//**********************************************************************************************
	public Map<String, Object>
	getActionData(Main.Action eAction)
	{
		Map<String, Object> mData = null;

		switch (eAction)
		{
			case USER_DELETE:
			case USER_LOGIN:
			case USER_REGISTER:
			case USER_RESET:
				if (m_eState == Main.State.USER_LOGIN ||
					m_eState == Main.State.USER_EMAIL)
				{
					mData = m_emailView.getUserLogin();
				}
				break;
				
			case CONFIRM:
				switch (m_eState)
				{
					case GAME_INVITE:
						mData = m_inviteView.getInviteEmail();
						break;

					case USER_SETUP:
						mData = m_setupView.getUserSetup();
						break;
				}
				break;
		}

		return mData;
	}

	//**********************************************************************************************
    public String
	getPlayerName()
	{
		return m_main.getUserName();
	}

	//**********************************************************************************************
	@Override
	protected void
	onLayout(
		boolean changed,
		int xStart,
		int yStart,
		int xEnd,
		int yEnd)
	{
		if (m_nHeightScreen == 0)
		{
			int nWidth = xEnd - xStart;
			m_nHeightScreen = yEnd - yStart;
			m_mainActivity.setHeight(m_nHeightScreen);
			
			// Initialize the images sizes based on the screen size.
			m_nWidthButton = nWidth / 6;
			m_nHeightButton = m_nHeightScreen / 14;
			m_yButtons = 3 * m_nHeightScreen / 4;

			m_nInfoWidth = 5 * nWidth / 6;
			m_xInfo = (nWidth - m_nInfoWidth) >> 1;

			m_yPopup = 2 * m_nHeightScreen / 31;
			m_yInfo = 23 * m_nHeightScreen / 31;
			m_yAd = 28 * m_nHeightScreen / 31;
			m_yAd = m_nHeightScreen - AdSize.SMART_BANNER.getHeightInPixels(m_mainActivity);
		}

		int yInfo = m_yInfo;
		int hInfo = 5 * m_nHeightScreen / 31;
		int yButtons = m_yButtons;
		switch (m_eState)
		{
			case GAME_OVER:
			case GAME_OVER_ALERT:
				yInfo -= 5 * m_nHeightScreen / (2 * 31);
				hInfo = 9 * m_nHeightScreen / (2 * 31);
				yButtons += m_yPopup;
				break;

			case GAME_INVITE:
			case USER_EMAIL:
			case USER_LOGIN:
			case USER_SETUP:
				yButtons = m_nHeightScreen / 2;
				break;
		}

		// Save the next location for a view.
		int yNext = yStart;
		int yBlock = yStart;

		// Need to know which button we are one.
		int iButton = 0;

		// If we have a popup box, display this after the main screen has been displayed.
		View viewAd = null;
		View viewAlert = null;
		View viewPopup = null;

		// Calculate the gap width between the buttons.
		int nGapWidth = 0;
		if (m_nButtons > 0)
		{
			nGapWidth = ((xEnd - xStart) - (m_nButtons * m_nWidthButton)) / (m_nButtons + 1);
			if (nGapWidth < 0)
			{
				nGapWidth = 0;
			}
		}
		
		int nChildren = getChildCount();
		for (int i = 0; i < nChildren; ++i)
		{
			View child = getChildAt(i);

			switch (m_eState)
			{
				case GAME_SELECT:
				case GAME_SELECT_WAITING:
					if (child == m_fbShare)
					{
						break;
					}
					else if (child instanceof Button)
					{
						yNext += m_nHeightButton + 20;
						child.layout(
							m_xInfo, yNext,
							m_xInfo + m_nInfoWidth, yNext + m_nHeightButton);
					}
					else
					{
						child.layout(0, 0, 0, 0);
					}
					continue;

				case GAME_LOAD:
				case GAME_LOAD_ROBOT:
				case GAME_LOAD_ROBOT_WAITING:
				case GAME_LOAD_PLAYER1:
				case GAME_LOAD_PLAYER1_WAITING:
				case GAME_LOAD_PLAYER2:
				case GAME_LOAD_PLAYER2_WAITING:
				case USER_EMAIL:
				case USER_LOGIN:
				case USER_REGISTER_WAITING:
				case USER_SETUP:
				case USER_WAITING:
				case USER_WELCOME:
				case USER_WELCOME_WAITING:
					if (child instanceof GameView)
					{
						child.layout(0, 0, 0, 0);
						yNext = 9 * yEnd / 12;
						continue;
					}
					break;

				default:
					if (child instanceof AdView)
					{
						viewAd = (AdView)child;
					}
					break;
			}

			if (child instanceof AdView)
			{
				continue;
			}

			if (child == m_alertView)
			{
				viewAlert = child;
				continue;
			}

			// If we have our game over button, treat it like a popup box.
			if (child == m_butInfo)
			{
				child.layout(0, 0, 0, 0);
				viewPopup = child;
				continue;
			}
			
			// Display the game view.
			if (child instanceof GameView)
			{
				yNext = yStart;
				child.measure(xEnd - xStart, yEnd - yStart);
				child.layout(xStart, yStart, xEnd, yEnd - m_nHeightButton);
				yNext += ((GameView)child).getDisplayHeight();
				continue;
			}

			// If we have a login screen, display this ontop of the main screen
			if (child instanceof DeleteView ||
				child instanceof DeletedView ||
				child instanceof EmailView ||
				child instanceof InviteView ||
				child instanceof LoginView ||
				child instanceof SetupView)
			{
				if (m_bPopup)
				{
					viewPopup = child;
				}
				else
				{
					child.layout(0, 0, 0, 0);
				}
				continue;
			}

			// Display buttons from the right.
			if (child instanceof Button || child instanceof SignInButton)
			{
				if (child.isClickable())
				{
					int width = m_nWidthButton;
					int x = xStart + ((iButton + 1) * nGapWidth) + (iButton * width);
					if (child == m_fbShare)
					{
						x -= m_nWidthButton >> 2;
						width += m_nWidthButton >> 1;
						child.measure(width, m_nHeightButton);
					}
					child.layout(x, yButtons, x + width, yButtons + m_nHeightButton);
					yBlock = yButtons + m_nHeightButton;
					++iButton;
				}
			}
			// During the WELCOME state, display our welcome screen.
			else if (
				child instanceof ImageView ||
				child instanceof TextView)
			{
				if (child == m_tvStatus)
				{
					continue;
				}
				
				if (!m_bPopup)
				{
					viewPopup = child;
				}
				else
				{
					child.layout(0, 0, 0, 0);
				}
			}
			else
			{
				child.measure(xEnd, yEnd);
				int width = child.getMeasuredWidth();
				int height = child.getMeasuredHeight();

				child.layout(xStart, yNext, xStart + width, yNext + height);
				yBlock = yNext += height + 10;
			}
		}

		// No status during game over state.
		switch (m_eState)
		{
			case GAME_OVER:
			case GAME_OVER_ALERT:
				m_tvStatus.setText("");
				m_tvStatus.layout(0, 0, 0, 0);
				break;

			default:
				// If the QWERTY keyboard is up, don't block it with an alert message.
				String strStatus = null;
				if (!m_gameView.isQwertyActive())
				{
					strStatus = m_main.getStatus();
				}
			
				// Pop up an alert message, if needed.
				if (strStatus != null && !strStatus.isEmpty())
				{
					if (iButton == 0)
					{
						yBlock = yNext + m_nHeightButton;
					}
					yBlock += 10;
					int height = yEnd - yBlock;
					if (height > m_nHeightButton)
					{
						height = m_nHeightButton;
					}

					m_tvStatus.setTextSize(22 * m_dFontMultiplier);
					m_tvStatus.setText(strStatus);
					m_tvStatus.measure(xEnd - xStart, height);
					m_tvStatus.layout(xStart, yBlock, xEnd, yBlock + height);
					yBlock += 10 + height;
				}
				else
				{
					m_tvStatus.layout(0, 0, 0, 0);
					if (yBlock > 0)
					{
						yBlock += 10 + m_nHeightButton;
					}
				}
				break;
		}

		// If we have a popup, display this now.
		if (viewPopup != null)
		{
			int width = 7 * (xEnd - xStart) >> 3;
			int height = (int)((yEnd - yStart) * .61);
			int x = xStart + (xEnd - xStart - width) >> 1;
			int y = yStart;
			if (viewPopup == m_butInfo)
			{
				y = yInfo;
				height = hInfo;
			}
			else if (m_bPopup)
			{
				height = 3 * (yEnd - y) / 7;
				y = m_yPopup;
			}
			else
			{
				y = m_yPopup;
			}
			viewPopup.measure(width, height);
			viewPopup.layout(x, y, x + width, y + height);
		}

		// Lastly, display any alerts that need to be presented to the user.
		if (viewAlert != null)
		{
			int width = m_alertView.getAlertWidth();
			int height = m_alertView.getAlertHeight();
			int x = ((xEnd - xStart) - width) >> 1;
			int y = ((yEnd - yStart) - height) >> 1;
			viewAlert.measure(width, height);
			viewAlert.layout(x, y, x + width, y + height);
		}

		// If we have an ad view, display it on the bottom.
		if (viewAd != null)
		{
			viewAd.measure(xEnd, yEnd - m_yAd);
			viewAd.layout(0, m_yAd, xEnd, yEnd);
		}
	}

	//**********************************************************************************************
	public boolean
	prepareUpdate()
	{
		if (m_bUpdating)
		{
			return false;
		}
		m_bUpdating = true;
		return true;
	}

	//**********************************************************************************************
	public void
	removeButtons()
	{
		// Remove all buttons.
		int iChild = getChildCount();
		while (iChild-- > 0)
		{
			View child = getChildAt(iChild);
			if ((child instanceof Button ||
				 child instanceof SignInButton) &&
				child != m_butInfo)
			{
				removeView(child);
			}
		}
	}

	//**********************************************************************************************
	public void
	removeViews()
	{
		// Remove all buttons.
		int iChild = getChildCount();
		while (iChild-- > 0)
		{
			View child = getChildAt(iChild);

			if (child instanceof Button)
			{
				if (child == m_butInfo)
				{
					removeView(child);
				}
			}
			else if (
				(child instanceof TextView && child != m_tvStatus) ||
				(child instanceof ImageView ||
				 child instanceof AlertView ||
				 child instanceof DeleteView ||
				 child instanceof DeletedView ||
				 child instanceof EmailView ||
				 child instanceof InviteView ||
				 child instanceof LoginView ||
				 child instanceof SetupView ||
				 child instanceof MessageView))
			{
				removeView(child);
			}
		}
	}

	//**********************************************************************************************
	public void
	resetUpdate()
	{
		m_bUpdating = false;
	}

	//**********************************************************************************************
	public void
	setAction(Main.Action eAction)
	{
 		try
		{
			Intent intent;
			switch (eAction)
			{
				case PRIVACY:
					intent = new Intent(
						"android.intent.action.VIEW",
						Uri.parse("https://jot.flycricket.io/privacy.html"));
					m_mainActivity.startActivity(intent);
					return;
					
				case TERMS:
					intent = new Intent(
						"android.intent.action.VIEW",
						Uri.parse("https://jot.flycricket.io/terms.html"));
					m_mainActivity.startActivity(intent);
					return;
					
				default:
					break;
			}
			
			m_main.setAction(
				eAction,
				getActionData(eAction));

			// Force a full update the first time through.
			prepareUpdate();

			// Update the our view of the game to be in sync with the game.
			animateUpdate();
		}
		catch (Exception e)
		{
			displayMessage("setAction() error:" + e.getMessage());
			return;
		}
	}

	//**********************************************************************************************
    public void
	setAlert(String strAlert)
	{
		m_alertView.setAlert(strAlert);
	}

	//**********************************************************************************************
    public void
	setPlayerName(String strName)
	{
		m_gameView.setPlayerName(m_main.getPlayer(), strName);
	}

	//**********************************************************************************************
	// Assign a label based on the tile availability.
	public void
	setTileButtonLabel()
	{
		m_gameView.setTileButtonLabel(
			m_mButtons.get(m_main.getDrawTileAction()),
			m_dFontMultiplier);
	}

	//**********************************************************************************************
	// Assign a label based on the tile availability.
	public void
	setVowelButtonLabel()
	{
		m_gameView.setVowelButtonLabel(
			m_mButtons.get(m_main.getDrawVowelAction()),
			m_dFontMultiplier);
	}

	//**********************************************************************************************
	public String
	toString()
	{
		return m_gameView.toString();
	}

	//**********************************************************************************************
	public void
	updateBackground()
	{
		switch (m_eState)
		{
			case GAME_LOAD:
			case GAME_LOAD_ROBOT:
			case GAME_LOAD_ROBOT_WAITING:
			case GAME_LOAD_PLAYER1:
			case GAME_LOAD_PLAYER1_WAITING:
			case GAME_LOAD_PLAYER2:
			case GAME_LOAD_PLAYER2_WAITING:
			case USER_WELCOME:
			case USER_WELCOME_WAITING:
				setBackgroundResource(R.drawable.jot_background_empty);
				break;
			case GAME_SELECT_WAITING:
				setBackgroundResource(R.drawable.jot_background_select);
				break;
			case TURN_FIRST:
				setBackgroundResource(R.drawable.jot_background_board_start);
				break;
			default:
				setBackgroundResource(R.drawable.jot_background_board);
				break;
		}
	}

	//**********************************************************************************************
	public void
	updateButtons(boolean bLast)
	{
		// Remove the prior state's buttons.
		removeButtons();

		int nViews = getChildCount();
		int nInvisible = 0;

		// If the QWERTY keyboard is up, don't display buttons.
		if (!m_gameView.isQwertyActive())
		{
			switch (m_eState)
			{
				case GAME_INVITE:
					addView(m_fbShare);
					addView(m_mButtons.get(Main.Action.CONFIRM));
					addView(m_mButtons.get(Main.Action.CANCEL));
					break;

				case GAME_OVER:
				case GAME_OVER_REPLAY:
					addView(m_mButtons.get(Main.Action.SWITCH_GAME));
					addView(m_mButtons.get(Main.Action.PLAY_AGAIN));
					addView(m_fbShare);
					break;
					
				case GAME_OVER_ALERT:
				case GAME_OVER_MENU:
					addView(m_mButtons.get(Main.Action.SWITCH_GAME));
					addView(m_fbShare);
					break;
					
				case GAME_OVER_QUIT:
					addView(m_mButtons.get(Main.Action.CONFIRM));
					break;

				case GAME_QUIT:
				case GAME_QUIT_ROBOT:
					addView(m_mButtons.get(Main.Action.CANCEL));
					addView(m_mButtons.get(Main.Action.CONFIRM));
					if (m_eState == Main.State.GAME_QUIT)
					{
						addView(m_mButtons.get(Main.Action.USER_BLOCK));
					}
					break;
				
				case GAME_RESUME:
				case GAME_START:
					addView(m_mButtons.get(Main.Action.PLAY_GAME));
					break;

				case GAME_SELECT_WAITING:
					// Clear lock-screen notifications.
					m_mainActivity.clearNotifications();
					
					int iGame = -1;
					String strOpponent;
					int yNext = 0;
					int blue = Color.rgb(127, 255, 212);
					int green = Color.rgb(0, 255, 0);
					while ((strOpponent = m_main.getGameOpponentName(++iGame)) != null)
					{
						Button button = new Button(m_mainActivity);
						++nInvisible;

						yNext += m_nHeightButton + 20;
						button.layout(
							m_xInfo,
							yNext,
							m_xInfo + m_nInfoWidth,
							yNext + m_nHeightButton);

						if (iGame == 0 || m_main.getGameTurnStatus(iGame))
						{
							button.setBackgroundColor(green);
						}
						else
						{
							button.setBackgroundColor(blue);
						}

						button.setGravity(Gravity.CENTER);
						button.setWidth(m_nInfoWidth);
						button.setTextColor(Color.BLACK);
						button.setAllCaps(false);
						button.setText(strOpponent);
						button.setTextSize(16 * m_dFontMultiplier);
						button.setId(iGame);
						addView(button);

						button.setOnClickListener
						(
							view ->
							{
								try
								{
									m_main.setGame(view.getId());

									// Update the game view to be in sync with the game.
									animateUpdate();
								}
								catch (Exception e)
								{
									displayMessage("createButton() error:" + e.getMessage());
								}
							}
						);
					}
					break;

				case TURN_DRAW:
				case TURN_DRAW_FIRST:
					// Allow user to login during a robot game.
					if (!m_main.haveUser())
					{
						addView(m_mButtons.get(Main.Action.USER_LOGIN));
					}
					// fallthru;

				case TURN_WAITING:
					// Allow the user to return to the game selection screen.
					if (m_main.haveUser())
					{
						addView(m_mButtons.get(Main.Action.SWITCH_GAME));
					}

					// Disable most keys while the robot is working on a play.
					if (!m_main.isRobotWorking())
					{
						// If we have a best word, have a key to display this.
						if (bLast && m_main.haveBestWord())
						{
							addView(m_mButtons.get(Main.Action.DISPLAY_BEST));
						}

						if (!m_gameView.isRackFilled())
						{
							if (m_main.haveTiles())
							{
								setTileButtonLabel();
								addView(m_mButtons.get(m_main.getDrawTileAction()));
							}
							if (m_main.haveVowels())
							{
								setVowelButtonLabel();
								addView(m_mButtons.get(m_main.getDrawVowelAction()));
							}
						}
						else
						{
							addView(m_mButtons.get(Main.Action.QUIT_GAME));
							addView(m_mButtons.get(Main.Action.JUMBLE_RACK));
						}
					}
					break;
				
				case TURN_FIRST:
				case TURN_INVALID:				
				case TURN_PLACE:
					// Disable most keys while the robot is working on a play.
					if (m_main.isRobotWorking())
					{
						addView(m_mButtons.get(Main.Action.JUMBLE_RACK));
						break;
					}

					// Near the end of the game, the user might not be able to play a word.
					if (m_main.isTilesLow())
					{
						addView(m_mButtons.get(Main.Action.SKIP_TURN));
					}
					else if (m_main.haveUser())
					{
						addView(m_mButtons.get(Main.Action.SWITCH_GAME));
					}

					// Allow the user to quit the game.
					addView(m_mButtons.get(Main.Action.QUIT_GAME));

					// Allow the board to be rotated and zoomed.
					if (!m_main.isBoardEmpty())
					{
						if (m_gameView.isBoardFocused())
						{
							addView(m_mButtons.get(Main.Action.UNZOOM_BOARD));
						}
						else
						{
							addView(m_mButtons.get(Main.Action.ROTATE_BOARD));
						}
					}

					// Allow the user to rack or jumble the tiles.
					if (m_main.isTilePlaced())
					{
						addView(m_mButtons.get(Main.Action.RACK_TILES));
					}
					if (m_main.getRackTileCount() > 2)
					{
						addView(m_mButtons.get(Main.Action.JUMBLE_RACK));
					}
					break;
				
				case TURN_PLAY:
					if (!m_main.isBoardEmpty())
					{
						if (m_gameView.isBoardFocused())
						{
							addView(m_mButtons.get(Main.Action.UNZOOM_BOARD));
						}
						else
						{
							addView(m_mButtons.get(Main.Action.ROTATE_BOARD));
						}
					}
					addView(m_mButtons.get(Main.Action.RACK_TILES));
					if (m_main.getRackTileCount() > 2)
					{
						addView(m_mButtons.get(Main.Action.JUMBLE_RACK));
					}
					break;
				
				case TURN_READY:
					if (!m_main.isBoardEmpty())
					{
						if (m_gameView.isBoardFocused())
						{
							addView(m_mButtons.get(Main.Action.UNZOOM_BOARD));
						}
						else
						{
							addView(m_mButtons.get(Main.Action.ROTATE_BOARD));
						}
					}
					addView(m_mButtons.get(Main.Action.RACK_TILES));
					if (m_main.getRackTileCount() > 2)
					{
						addView(m_mButtons.get(Main.Action.JUMBLE_RACK));
					}
					addView(m_mButtons.get(Main.Action.PLAY_WORD));
					break;
				
				case TURN_ROBOT:
					break;
				
				case TURN_SKIP:
				case USER_BLOCK:
				case USER_MESSAGE:
					addView(m_mButtons.get(Main.Action.CONFIRM));
					addView(m_mButtons.get(Main.Action.CANCEL));
					break;

				case TURN_START:
					addView(m_mButtons.get(Main.Action.PLAY_GAME));
					break;

				case TURN_VERIFY:
					addView(m_mButtons.get(Main.Action.SWITCH_GAME));
					break;

				case USER_DELETE:
					addView(m_mButtons.get(Main.Action.CONFIRM));
					addView(m_mButtons.get(Main.Action.CANCEL));
					break;

				case USER_DELETED:
					addView(m_mButtons.get(Main.Action.QUIT_GAME));
					break;

				case USER_EMAIL:
					addView(m_mButtons.get(Main.Action.USER_LOGIN));
					addView(m_mButtons.get(Main.Action.USER_REGISTER));
					addView(m_mButtons.get(Main.Action.USER_RESET));
					break;
					
				case USER_LOGIN:
					addView(m_mButtons.get(Main.Action.PLAY_GAME));
					addView(m_fbLogin);
					addView(m_gLogin);
					if (!m_gLogin.isClickable())
					{
						++nInvisible;
					}
					addView(m_mButtons.get(Main.Action.USER_EMAIL));
					break;
					
				case USER_SETUP:
					if (m_main.haveUser())
					{
						addView(m_mButtons.get(Main.Action.CONFIRM));
						addView(m_mButtons.get(Main.Action.CANCEL));
						addView(m_mButtons.get(Main.Action.USER_DELETE));
					}
					addView(m_mButtons.get(Main.Action.ABOUT));
					break;

				case USER_WELCOME:
				case USER_WELCOME_WAITING:
					if (!m_main.haveUser())
					{
						addView(m_mButtons.get(Main.Action.USER_LOGIN));
					}
					addView(m_mButtons.get(Main.Action.PLAY_GAME));
					addView(m_mButtons.get(Main.Action.TERMS));
					addView(m_mButtons.get(Main.Action.PRIVACY));
					break;
			}
		}
		m_nButtons = getChildCount() - nViews - nInvisible;
	}

	//**********************************************************************************************
	public void
	updateViews()
	{
		removeViews();
		m_bPopup = false;

		switch (m_eState)
		{
			case ABOUT:
				m_butInfo.setWidth(m_nInfoWidth);
				m_butInfo.layout(
					m_xInfo,
					3 * m_xInfo,
					m_xInfo + m_nInfoWidth,
					3 * m_xInfo + m_nInfoWidth);
				m_butInfo.setGravity(Gravity.CENTER);
				m_butInfo.setText(s_strAbout);
				addView(m_butInfo);
				break;
				
			case GAME_OVER:
			case GAME_OVER_ALERT:
				m_butInfo.setWidth(m_nInfoWidth);
				m_butInfo.layout(
					m_xInfo,
					3 * m_xInfo,
					m_xInfo + m_nInfoWidth,
					3 * m_xInfo + m_nInfoWidth);
				m_butInfo.setGravity(Gravity.CENTER);

				// Include the game's outcome.
				StringBuilder sbOver = new StringBuilder();
				sbOver.append(m_main.getOutcome());

				// Include the high scores.
				int nScoreHigh0 = m_main.getHighScore(0);
				int nScoreHigh1 = m_main.getHighScore(1);
				sbOver.append(
					"\n\nHigh scores: " + Integer.toString(nScoreHigh0) +
					" vs " + Integer.toString(nScoreHigh1));

				// Include the averages.
				int nScoreAve0 = m_main.getRanking(0);
				int nScoreAve1 = m_main.getRanking(1);
				if (nScoreAve0 == 0)
				{
					nScoreAve0 = nScoreHigh0;
				}
				if (nScoreAve1 == 0)
				{
					nScoreAve1 = nScoreHigh1;
				}
				sbOver.append(
					"\nAverage: " +  Integer.toString(nScoreAve0) +
					" vs " + Integer.toString(nScoreAve1));

				m_butInfo.setText(sbOver.toString());
				addView(m_butInfo);
				break;

			case USER_DELETE:
				m_bPopup = true;
				addView(m_deleteView);
				break;

			case USER_DELETED:
				m_bPopup = true;
				addView(m_deletedView);
				break;

			case USER_EMAIL:
				m_bPopup = true;
				addView(m_emailView);
				break;

			case GAME_INVITE:
				m_bPopup = true;
				addView(m_inviteView);
				break;

			case USER_LOGIN:
				m_bPopup = true;
				addView(m_loginView);
				break;

			case USER_SETUP:
				if (m_main.haveUser())
				{
					m_bPopup = true;
					m_setupView.setDetails(m_main);
					addView(m_setupView);
				}
				break;

			case USER_REGISTER_WAITING:
				// m_progressDialog.setTitle("Registering...");
				// m_progressDialog.show();
				break;

			case USER_WAITING:
				// m_progressDialog.setTitle("Logging in...");
				// m_progressDialog.show();
				break;
		}
		
		ImageView helpView = m_mHelpViews.get(m_eState);
		if (helpView != null)
		{
			addView(helpView);
		}
		addView(m_alertView);
	}

	//**********************************************************************************************
	public boolean
	updateView() throws Exception
	{
		// Update the main state before updating its view.
		m_main.updateState();

		// Make sure the player's turn is updated.
		if (m_main.isWorking())
		{
			m_gameView.updatePlayers();
		}

		// Make sure we are viewing the active game.
		if (m_main.isGameSelected())
		{
			m_gameView.setGame(m_main.getGame(), m_main.getPlayer());
		}

		// If requested, popup the player setup.
		if (m_gameView.isPlayerSetup())
		{
			m_main.setAction(Main.Action.USER_SETUP);
		}

		// Avoid updating our view if we're in the same state.
		Main.State eState = m_main.getState();

		if (eState == m_eState)
		{
			do
			{
				switch (m_eState)
				{
					case GAME_SELECT_WAITING:  // SPK: do we need this?
						return false;

					case GAME_START:
						m_gameView.reset();
						break;
				}

				// If requested, make sure the board is not zoomed in.
				if (m_main.isFocusBlocked())
				{
					if (m_gameView.blockFocus())
					{
						break;
					}
				}

				// If requested, rotate the view of the board.
				m_gameView.setBoardRotation(m_main.isBoardRotated());

				// Set a delay after the best word board is displayed.
				if (m_eState == Main.State.TURN_BESTWORD)
				{
					if (m_gameView.updateBest())
					{
						return true;
					}
					if (m_nDelay == 0)
					{
						m_nDelay = 40;
					}

					// If we have an animation, keep updating until our delay is zero.
					if (--m_nDelay == 0)
					{
						// Tell the main code that our animation delay is complete.
						m_main.setAction(Main.Action.DONE);
					}
					return true;
				}

				// If the QWERTY keyboard state has changed, update buttons.
				if (m_bQwerty != m_gameView.isQwertyActive())
				{
					m_bQwerty = m_gameView.isQwertyActive();
					break;
				}
				
				// Update the focus only when the robot is done.
				if (!m_main.isWorking())
				{
					// If the board focus has changed, update the buttons.
					if (m_gameView.updateFocus())
					{
						break;
					}
				}

				// If our game view still requires sychronization, continue to update it.
				boolean bUpdate = false;
				if (m_main.isGameSelected())
				{
					bUpdate = m_gameView.updateView();
				}

				// On the last update, update the buttons.
				if (!bUpdate)
				{
					updateButtons(true);
					requestLayout();
				}
				return bUpdate;
			} while (false);
		}
		m_eState = eState;

		// Cancel the progress indicator, if active.
		// m_progressDialog.cancel();

		// Update the background image.
		updateBackground();

		// Update our buttons for this new state.
		updateButtons(false);

		// Update our views for this new state.
		updateViews();

		// Return true to force another update.
		return true;
	}
}

//**************************************************************************************************
