package info.guardianproject.yakreader.adapters;

import info.guardianproject.yakreader.R;
import info.guardianproject.yakreader.App;
import info.guardianproject.yakreader.uiutil.UIHelpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.holoeverywhere.widget.Spinner;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

public class ShareSpinnerAdapter extends BaseAdapter implements SpinnerAdapter
{
	private final String TITLE_TAG = "TITLE";

	private final Spinner mSpinner;
	private final LayoutInflater mInflater;
	private final ArrayList<ResolveInfo> mReceivers;
	private final HashMap<ResolveInfo, Intent> mReceiverIntents;
	private final Context mContext;
	private final int mResIdHeaderString;
	private final int mResIdButtonLayout;

	private final Intent mSecureChatShareIntentPlaceholder = new Intent();
	
	public ShareSpinnerAdapter(Spinner spinner, Context context, int resIdHeaderString, int resIdButtonLayout)
	{
		super();
		mSpinner = spinner;
		mContext = context;
		mResIdHeaderString = resIdHeaderString;
		mResIdButtonLayout = resIdButtonLayout;
		mReceivers = new ArrayList<ResolveInfo>();
		mReceiverIntents = new HashMap<ResolveInfo, Intent>();
		mInflater = LayoutInflater.from(context);
	}

	public void clear()
	{
		mReceivers.clear();
		mReceiverIntents.clear();
		this.notifyDataSetChanged();
	}

	public void addIntentResolvers(Intent shareIntent, Intent intentToAdd, String packageName, int label, int icon)
	{
		if (shareIntent != null)
		{
			PackageManager pm = mContext.getPackageManager();
			final List<ResolveInfo> infos = pm.queryIntentActivities(shareIntent, 0);
			for (int i = 0; i < infos.size(); i++)
			{
				ResolveInfo info = infos.get(i);

				// Looking for a special package?
				if (packageName != null && !packageName.equals(info.activityInfo.packageName))
					continue;

				if (label != 0)
					info.labelRes = label;
				if (icon != 0)
					info.icon = icon;

				Intent handler = intentToAdd;
				if (handler == null)
				{
					// This is the normal case, with intentToAdd set to null
					handler = new Intent(shareIntent);
					handler.setClassName(info.activityInfo.packageName, info.activityInfo.name);
				}
				mReceivers.add(info);
				mReceiverIntents.put(info, handler);
			}
		}
		this.notifyDataSetChanged();
	}

	public void addIntentResolvers(Intent shareIntent)
	{
		addIntentResolvers(shareIntent, null, null, 0, 0);
	}

	public void addSecureBTShareResolver(Intent shareIntent)
	{
		// Add an intent for our secure share
		//
		addIntentResolver(shareIntent, null, mContext.getPackageName(), mContext.getPackageName() + ".installer.SecureBluetoothSenderActivity",
				R.string.share_via_secure_bluetooth, R.drawable.ic_share_sharer);
	}
	
	public void addSecureChatShareResolver(Intent shareIntent) {
		//addIntentResolver(shareIntent, mSecureChatShareIntentPlaceholder, "com.example.securesharertest", "com.example.securesharertest.MainActivity",
		//	R.string.share_via_secure_chat, R.drawable.ic_share_sharer);
		
//		addIntentResolver(shareIntent, mSecureChatShareIntentPlaceholder, "info.guardianproject.otr.app.im", "info.guardianproject.otr.app.im.app.ImUrlActivity",
//				R.string.share_via_secure_chat, R.drawable.ic_share_sharer);
		
		ResolveInfo info = new ResolveInfo();
		ActivityInfo ai = null;
		try
		{
			ai = App.getInstance().getPackageManager().getActivityInfo(new ComponentName("info.guardianproject.otr.app.im", "info.guardianproject.otr.app.im.app.ImUrlActivity"), 0);
			info.activityInfo = ai;
			info.labelRes = ai.applicationInfo.labelRes;
			info.icon = ai.applicationInfo.icon;
		}
		catch (NameNotFoundException e)
		{
			info.labelRes = R.string.share_via_secure_chat;
			info.icon = R.drawable.ic_share_sharer;
		}
		mReceivers.add(info);
		mReceiverIntents.put(info, mSecureChatShareIntentPlaceholder);
		this.notifyDataSetChanged();
		
	}

	public void addIntentResolver(Intent shareIntent, Intent intentToAdd, String packageName, String className, int label, int icon)
	{
		// Add an intent for our secure share
		//
		ResolveInfo info = new ResolveInfo();
		ActivityInfo ai;
		try
		{
			ai = App.getInstance().getPackageManager().getActivityInfo(new ComponentName(packageName, className), 0);
			info.activityInfo = ai;
			info.labelRes = label;
			info.icon = icon;

			Intent handler = intentToAdd;
			if (handler == null)
			{
				// This is the normal case, with intentToAdd set to null
				handler = new Intent(shareIntent);
				handler.setClassName(info.activityInfo.packageName, info.activityInfo.name);
			}
			mReceivers.add(info);
			mReceiverIntents.put(info, handler);
		}
		catch (NameNotFoundException e)
		{
		}
		this.notifyDataSetChanged();
	}

	public boolean isSecureChatIntent(Intent intent)
	{
		return intent == this.mSecureChatShareIntentPlaceholder;
	}
	
	public Intent getIntentAtPosition(int position)
	{
		ResolveInfo info = getItem(position);
		if (info != null)
		{
			return mReceiverIntents.get(info);
		}
		return null;
	}

	@Override
	public int getCount()
	{
		return 1 + mReceivers.size();
	}

	@Override
	public ResolveInfo getItem(int position)
	{
		if (position == 0)
			return null;
		return mReceivers.get(position - 1);
	}

	@Override
	public long getItemId(int position)
	{
		if (position == 0)
			return 0;
		return getItem(position).hashCode();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View view = convertView;
		if (view == null)
		{
			view = mInflater.inflate(mResIdButtonLayout, parent, false);

			if (view instanceof ImageView)
			{
				ImageView iv = (ImageView) view;
				if (iv != null)
					UIHelpers.colorizeDrawable(mContext, iv.getDrawable());
			}

			view.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					mSpinner.performClick();
				}
			});

		}
		return view;
	}

	@Override
	public boolean isEnabled(int position)
	{
		if (position == 0)
			return false;
		return super.isEnabled(position);
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent)
	{
		if (position == 0)
		{
			View view = convertView;
			if (view == null || view.getTag() != TITLE_TAG)
			{
				view = mInflater.inflate(R.layout.share_popup_title, parent, false);
				TextView tv = (TextView) view;
				if (tv != null)
					tv.setText(mResIdHeaderString);
				view.setTag(TITLE_TAG);
			}
			return view;
		}

		View view = (convertView != null && convertView.getTag() != TITLE_TAG) ? convertView : createView(parent);
		TextView tv = (TextView) view.findViewById(R.id.tvItem);
		ImageView ivItem = (ImageView) view.findViewById(R.id.ivItem);

		ResolveInfo info = getItem(position);
		if (info.activityInfo == null)
		{
			tv.setText(info.labelRes);
			ivItem.setImageResource(info.icon);
		}
		else
		{
			PackageManager pm = mContext.getPackageManager();
			tv.setText(info.loadLabel(pm));

			Drawable icon = info.loadIcon(pm);
			ivItem.setImageDrawable(icon);
		}
		return view;
	}

	private View createView(ViewGroup parent)
	{
		View item = mInflater.inflate(R.layout.share_popup_item, parent, false);
		return item;
	}
}
