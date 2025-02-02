package com.meizu.ruandongchuan.dlnatest.engine;


import com.meizu.ruandongchuan.dlnatest.util.LogUtil;

import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.device.DeviceChangeListener;


/**
 * A thread to search the devices all the time.
 * 
 * @author CharonChui
 */
public class SearchThread extends Thread {
	private boolean flag = true;
	private ControlPoint mControlPoint;
	private boolean mStartComplete;
	private int mSearchTimes;
	private static final int mFastInternalTime = 15000;
	private static final int mNormalInternalTime = 3600000;
	private static final String TAG = "SearchThread";
	private DeviceChangeListener mDeviceChangeListener;

	public SearchThread(ControlPoint mControlPoint) {
		super();
		this.mControlPoint = mControlPoint;
		mDeviceChangeListener = new DeviceChangeListener() {

			@Override
			public void deviceRemoved(Device dev) {
				LogUtil.d(TAG, "control point remove a device");
				ControlPointContainer.getInstance().removeDevice(dev);
			}

			@Override
			public void deviceAdded(Device dev) {
				LogUtil.d(TAG, "control point add a device..." + dev.getDeviceType() + dev.getFriendlyName());
				ControlPointContainer.getInstance().addDevice(dev);
			}
		};
		if (mControlPoint != null) {
			this.mControlPoint.addDeviceChangeListener(mDeviceChangeListener);
		}
	}

	@Override
	public void run() {
		while (flag) {
			if (mControlPoint == null) {
				break;
			}
			searchDevices();
		}
	}

	/**
	 * Search for the DLNA devices.
	 */
	private void searchDevices() {
		try {
			if (mStartComplete) {
				mControlPoint.search();
				LogUtil.d(TAG, "controlpoint search...");
			} else {
				mControlPoint.stop();
				boolean startRet = mControlPoint.start();
				LogUtil.d(TAG, "controlpoint start:" + startRet);
				if (startRet) {
					mStartComplete = true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Search the devices five times fast, and after that we can make it
		// search lowly to save the power.
		synchronized (this) {
			try {
				mSearchTimes++;
				if (mSearchTimes >= 5) {
					wait(mNormalInternalTime);
				} else {
					wait(mFastInternalTime);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Set the search times, set this to 0 to make it search fast.
	 * 
	 * @param searchTimes
	 *            The times we have searched.
	 */
	public synchronized void setSearcTimes(int searchTimes) {
		this.mSearchTimes = searchTimes;
	}

	/**
	 * Notify all the thread.
	 */
	public void awake() {
		synchronized (this) {
			notifyAll();
		}
	}

	/**
	 * Stop the thread, if quit this application we should use this method to
	 * stop the thread.
	 */
	public void stopThread() {
		flag = false;
		awake();
	}


}