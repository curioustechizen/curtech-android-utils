
package in.curtech.android.olbtest;

import in.curtech.android.common.LocalBroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OLBReceiverHighest extends LocalBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Constants.ACTION_OLB_EVENT_OCCURRED.equals(intent.getAction())) {
            Log.d(Constants.LOG_TAG, "In onReceive of "
                    + this.getClass().getName());
        }
    }

}
