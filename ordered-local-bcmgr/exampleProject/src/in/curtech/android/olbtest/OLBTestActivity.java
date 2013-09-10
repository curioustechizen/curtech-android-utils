package in.curtech.android.olbtest;

import in.curtech.android.common.OrderEnabledLocalBroadcastManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class OLBTestActivity extends Activity {
	OrderEnabledLocalBroadcastManager localBroadcastMgr;
	OLBReceiverHighest rcvHighest;
	OLBReceiverMedium rcvMedium;
	OLBReceiverLeast rcvLeast;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final Button btnSend = (Button) findViewById(R.id.btnSend);
		final Button btnSendConsume = (Button) findViewById(R.id.btnSendWithConsume);
		localBroadcastMgr = OrderEnabledLocalBroadcastManager
				.getInstance(getApplicationContext());

		final IntentFilter ifLeast = new IntentFilter(
				Constants.ACTION_OLB_EVENT_OCCURRED);
		final IntentFilter ifMedium = new IntentFilter(ifLeast);
		final IntentFilter ifHighest = new IntentFilter(ifLeast);

		ifLeast.setPriority(100);
		ifMedium.setPriority(200);
		ifHighest.setPriority(300);

		rcvLeast = new OLBReceiverLeast();
		rcvMedium = new OLBReceiverMedium();
		rcvHighest = new OLBReceiverHighest();
		
		localBroadcastMgr.registerReceiver(rcvLeast, ifLeast);
		localBroadcastMgr.registerReceiver(rcvHighest, ifHighest);
		
		/*
		 * On button click, we want to know if a receiver has been registered
		 * before unregistering it. However there is no way of knowing this.
		 * 
		 * As a work-around; we explicitly make sure we register it first. Then, on
		 * every click, unregister and re-register with the appropriate filters.
		 */
		localBroadcastMgr.registerReceiver(rcvMedium, ifMedium);
		

		final Intent eventOccuredIntent = new Intent(
				Constants.ACTION_OLB_EVENT_OCCURRED);

		// final Context ctx = getApplicationContext();
		View.OnClickListener clickListener = new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				localBroadcastMgr.unregisterReceiver(rcvMedium);
				if (v == btnSend) {
					rcvMedium = new OLBReceiverMedium(false);
				} else if (v == btnSendConsume) {
					rcvMedium = new OLBReceiverMedium(true);
				}
				
				localBroadcastMgr.registerReceiver(rcvMedium,
						ifMedium);
				localBroadcastMgr.sendOrderedBroadcast(eventOccuredIntent);
			}
		};

		btnSend.setOnClickListener(clickListener);
		btnSendConsume.setOnClickListener(clickListener);
	}

	
	@Override
	protected void onDestroy() {
		localBroadcastMgr.unregisterReceiver(rcvLeast);
		localBroadcastMgr.unregisterReceiver(rcvMedium);
		localBroadcastMgr.unregisterReceiver(rcvHighest);
		super.onDestroy();
	}
}
