package info.guardianproject.yakreader;
		
import info.guardianproject.securereader.Settings;
import info.guardianproject.securereader.Settings.UiLanguage;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereader.SocialReader.SocialReaderLockListener;
import info.guardianproject.securereader.SocialReporter;
import info.guardianproject.yakreader.widgets.CustomFontButton;
import info.guardianproject.yakreader.widgets.CustomFontEditText;
import info.guardianproject.yakreader.widgets.CustomFontRadioButton;
import info.guardianproject.yakreader.widgets.CustomFontTextView;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.tinymission.rss.Feed;

public class App extends Application implements OnSharedPreferenceChangeListener, SocialReaderLockListener
{
	public static final boolean UI_ENABLE_POPULAR_ITEMS = false;
			
	public static final boolean UI_ENABLE_COMMENTS = false;
	public static final boolean UI_ENABLE_TAGS = true;
	public static final boolean UI_ENABLE_POST_LOGIN = false;
	public static final boolean UI_ENABLE_REPORTER = false;
	public static final boolean UI_ENABLE_CHAT = false;
	public static final boolean UI_ENABLE_LANGUAGE_CHOICE = true;
	
	public static final String EXIT_BROADCAST_PERMISSION = "info.guardianproject.yakreader.exit.permission";
	public static final String EXIT_BROADCAST_ACTION = "info.guardianproject.yakreader.exit.action";
	public static final String SET_UI_LANGUAGE_BROADCAST_ACTION = "info.guardianproject.yakreader.setuilanguage.action";
	public static final String WIPE_BROADCAST_ACTION = "info.guardianproject.yakreader.wipe.action";
	public static final String UNLOCKED_BROADCAST_ACTION = "info.guardianproject.yakreader.unlock.action";
	
	private static App m_singleton;

	public Feed m_activeFeed;
	public int m_selectedArticleId;
	public boolean m_unreadOnly = true;
	public boolean m_unreadArticlesOnly = true;
	// public String m_sessionId;
	// public int m_apiLevel;
	public boolean m_canUseProgress;

	public static Context m_context;
	public static Settings m_settings;

	public SocialReader socialReader;
	public SocialReporter socialReporter;
	private String mCurrentLanguage;


	@SuppressLint("NewApi")
	@Override
	public void onCreate()
	{
		super.onCreate();

		m_singleton = this;
		m_context = this;
		m_settings = new Settings(m_context);
		applyUiLanguage();

		socialReader = SocialReader.getInstance(this.getApplicationContext());
		socialReader.setLockListener(this);
		socialReporter = new SocialReporter(socialReader);

		m_settings.registerChangeListener(this);
		
		mCurrentLanguage = getBaseContext().getResources().getConfiguration().locale.getLanguage();
	}

	public static Context getContext()
	{
		return m_context;
	}

	public static App getInstance()
	{
		return m_singleton;
	}

	public static Settings getSettings()
	{
		return m_settings;
	}

	private Bitmap mTransitionBitmap;

	private LockScreenActivity mLockScreen;

	public Bitmap getTransitionBitmap()
	{
		return mTransitionBitmap;
	}

	public void putTransitionBitmap(Bitmap bitmap)
	{
		mTransitionBitmap = bitmap;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if (key.equals(Settings.KEY_UI_LANGUAGE))
		{
			applyUiLanguage();
		}
	}
		
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		applyUiLanguage();
	}

	@SuppressLint("NewApi")
	private void applyUiLanguage()
	{
		UiLanguage lang = m_settings.uiLanguage();

		// Update language!
		//
		Configuration config = new Configuration();
		
		String language = "en";
		if (lang == UiLanguage.Farsi)
			language = "ar";
		else if (lang == UiLanguage.Tibetan)
			language = "bo";
		else if (lang == UiLanguage.Chinese)
			language = "zh";
		
		if (language.equals(mCurrentLanguage))
			return;
		mCurrentLanguage = language;
		
		Locale loc = new Locale(language);
		if (Build.VERSION.SDK_INT >= 17)
			config.setLocale(loc);
		else
			config.locale = loc;
		Locale.setDefault(loc);
		getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
	
		// Notify activities (if any)
		Intent intent = new Intent(App.SET_UI_LANGUAGE_BROADCAST_ACTION);
		this.sendOrderedBroadcast(intent, App.EXIT_BROADCAST_PERMISSION, new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
			}
		}, null, Activity.RESULT_OK, null, null);
	}

	public void wipe(int wipeMethod)
	{
		socialReader.doWipe(wipeMethod);

		// Notify activities (if any)
		Intent intent = new Intent(App.WIPE_BROADCAST_ACTION);
		this.sendOrderedBroadcast(intent, App.EXIT_BROADCAST_PERMISSION, new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
			}
		}, null, Activity.RESULT_OK, null, null);
	}
	
	public static View createView(String name, Context context, AttributeSet attrs)
	{
		if (name.equals("TextView"))
		{
			return new CustomFontTextView(context, attrs);
		}
		else if (name.equals("Button"))
		{
			return new CustomFontButton(context, attrs);
		}
		else if (name.equals("RadioButton"))
		{
			return new CustomFontRadioButton(context, attrs);
		}
		else if (name.equals("EditText"))
		{
			return new CustomFontEditText(context, attrs);
		}
		return null;
	}

	private int mnResumed = 0;
	private Activity mLastResumed;

	public void onActivityPause(Activity activity)
	{
		mnResumed--;
		if (mnResumed == 0)
			socialReader.onPause();
	}

	public void onActivityResume(Activity activity)
	{
		mLastResumed = activity;
		mnResumed++;
		if (mnResumed == 1)
			socialReader.onResume();
	}
	
	@Override
	public void onLocked()
	{
		if (mLastResumed != null && mLockScreen == null)
		{
			Intent intent = new Intent(this, LockScreenActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			intent.putExtra("originalIntent", mLastResumed.getIntent());
			mLastResumed.startActivity(intent);
			mLastResumed.overridePendingTransition(0, 0);
			mLastResumed = null;
		}
	}

	@Override
	public void onUnlocked()
	{
		if (mLockScreen != null)
			mLockScreen.onUnlocked();
		mLockScreen = null;
		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(UNLOCKED_BROADCAST_ACTION));
	}

	public void onLockScreenResumed(LockScreenActivity lockScreenActivity)
	{
		mLockScreen = lockScreenActivity;
	}

	public void onLockScreenPaused(LockScreenActivity lockScreenActivity)
	{
		mLockScreen = null;
	}
}
