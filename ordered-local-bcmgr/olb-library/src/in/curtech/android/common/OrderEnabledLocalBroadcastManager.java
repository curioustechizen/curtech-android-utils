
package in.curtech.android.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Implementation of {@link LocalBroadcastManager} that adds the ability to send
 * ordered broadcasts locally. <b>Note:</b> This class does <em>not</em> extend
 * from {@code LocalBroadcastManager} - hence the two classes are <em>not</em>
 * compatible. <br/>
 * You will need to replace references to {@code LocalBroadcastManager} with
 * references to this class. <br/>
 * <br/>
 * Furthermore, in order to receive ordered local broadcasts, you will need to
 * register {@link LocalBroadcastReceiver} objects instead of the regular
 * {@code BroadcastReceiver}s.
 * 
 * @author Kiran Rao
 * @see LocalBroadcastManager
 * @see LocalBroadcastReceiver
 */
public class OrderEnabledLocalBroadcastManager {
    private static class ReceiverRecord {
        final IntentFilter filter;
        final LocalBroadcastReceiver receiver;
        boolean broadcasting;

        ReceiverRecord(IntentFilter _filter, LocalBroadcastReceiver _receiver) {
            filter = _filter;
            receiver = _receiver;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(128);
            builder.append("Receiver{");
            builder.append(receiver);
            builder.append(" filter=");
            builder.append(filter);
            builder.append("}");
            return builder.toString();
        }
    }

    // Change by Kiran Rao
    /**
     * Comparator introduced for purpose of ordering the receivers according to
     * the priority set in their intent filters.
     * 
     * @author Kiran Rao
     */
    private static class ReceiverRecordComparator implements
            Comparator<ReceiverRecord> {

        @Override
        public int compare(ReceiverRecord o1, ReceiverRecord o2) {
            return o1.filter.getPriority() - o2.filter.getPriority();
        }

    }

    // End of change.

    private static class BroadcastRecord {
        final Intent intent;

        // Change by Kiran Rao
        /**
         * Flag indicating that the broadcast is meant to be ordered.
         */
        final boolean isOrdered;
        // End of change.

        final ArrayList<ReceiverRecord> receivers;

        BroadcastRecord(Intent _intent, ArrayList<ReceiverRecord> _receivers,
                boolean _isOrdered) {
            intent = _intent;
            receivers = _receivers;
            isOrdered = _isOrdered;

        }
    }

    private static final String TAG = "OrderEnabledLocalBroadcastManager";
    private static final boolean DEBUG = false;

    private final Context mAppContext;

    private final HashMap<LocalBroadcastReceiver, ArrayList<IntentFilter>> mReceivers = new HashMap<LocalBroadcastReceiver, ArrayList<IntentFilter>>();
    private final HashMap<String, ArrayList<ReceiverRecord>> mActions = new HashMap<String, ArrayList<ReceiverRecord>>();

    private final ArrayList<BroadcastRecord> mPendingBroadcasts = new ArrayList<BroadcastRecord>();

    static final int MSG_EXEC_PENDING_BROADCASTS = 1;

    private final Handler mHandler;

    private static final Object mLock = new Object();
    private static OrderEnabledLocalBroadcastManager mInstance;

    public static OrderEnabledLocalBroadcastManager getInstance(Context context) {
        synchronized (mLock) {
            if (mInstance == null) {
                mInstance = new OrderEnabledLocalBroadcastManager(
                        context.getApplicationContext());
            }
            return mInstance;
        }
    }

    private OrderEnabledLocalBroadcastManager(Context context) {
        mAppContext = context;
        mHandler = new Handler(context.getMainLooper()) {

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_EXEC_PENDING_BROADCASTS:
                        executePendingBroadcasts();
                        break;
                    default:
                        super.handleMessage(msg);
                }
            }
        };
    }

    /**
     * Register a receive for any local broadcasts that match the given
     * IntentFilter.
     * 
     * @param receiver The LocalBroadcastReceiver to handle the broadcast.
     * @param filter Selects the Intent broadcasts to be received.
     * @see #unregisterReceiver
     */
    public void registerReceiver(LocalBroadcastReceiver receiver,
            IntentFilter filter) {
        synchronized (mReceivers) {
            ReceiverRecord entry = new ReceiverRecord(filter, receiver);
            ArrayList<IntentFilter> filters = mReceivers.get(receiver);
            if (filters == null) {
                filters = new ArrayList<IntentFilter>(1);
                mReceivers.put(receiver, filters);
            }
            filters.add(filter);
            for (int i = 0; i < filter.countActions(); i++) {
                String action = filter.getAction(i);
                ArrayList<ReceiverRecord> entries = mActions.get(action);
                if (entries == null) {
                    entries = new ArrayList<ReceiverRecord>(1);
                    mActions.put(action, entries);
                }
                entries.add(entry);
            }
        }
    }

    /**
     * Unregister a previously registered LocalBroadcastReceiver. <em>All</em>
     * filters that have been registered for this BroadcastReceiver will be
     * removed.
     * 
     * @param receiver The BroadcastReceiver to unregister.
     * @see #registerReceiver
     */
    public void unregisterReceiver(LocalBroadcastReceiver receiver) {
        synchronized (mReceivers) {
            ArrayList<IntentFilter> filters = mReceivers.remove(receiver);
            if (filters == null) {
                return;
            }
            for (int i = 0; i < filters.size(); i++) {
                IntentFilter filter = filters.get(i);
                for (int j = 0; j < filter.countActions(); j++) {
                    String action = filter.getAction(j);
                    ArrayList<ReceiverRecord> receivers = mActions.get(action);
                    if (receivers != null) {
                        for (int k = 0; k < receivers.size(); k++) {
                            if (receivers.get(k).receiver == receiver) {
                                receivers.remove(k);
                                k--;
                            }
                        }
                        if (receivers.size() <= 0) {
                            mActions.remove(action);
                        }
                    }
                }
            }
        }
    }

    private boolean sendBroadcast(Intent intent, boolean isOrdered) {
        synchronized (mReceivers) {
            final String action = intent.getAction();
            final String type = intent.resolveTypeIfNeeded(
                    mAppContext.getContentResolver());
            final Uri data = intent.getData();
            final String scheme = intent.getScheme();
            final Set<String> categories = intent.getCategories();

            final boolean debug = DEBUG
                    ||
                    ((intent.getFlags() & Intent.FLAG_DEBUG_LOG_RESOLUTION) != 0);
            if (debug)
                Log.v(
                        TAG, "Resolving type " + type + " scheme " + scheme
                                + " of intent " + intent);

            ArrayList<ReceiverRecord> entries = mActions
                    .get(intent.getAction());
            if (entries != null) {
                if (debug)
                    Log.v(TAG, "Action list: " + entries);

                ArrayList<ReceiverRecord> receivers = null;
                for (int i = 0; i < entries.size(); i++) {
                    ReceiverRecord receiver = entries.get(i);
                    if (debug)
                        Log.v(TAG, "Matching against filter " + receiver.filter);

                    if (receiver.broadcasting) {
                        if (debug) {
                            Log.v(TAG, "  Filter's target already added");
                        }
                        continue;
                    }

                    int match = receiver.filter.match(action, type, scheme,
                            data,
                            categories, "LocalBroadcastManager");
                    if (match >= 0) {
                        if (debug)
                            Log.v(TAG, "  Filter matched!  match=0x" +
                                    Integer.toHexString(match));
                        if (receivers == null) {
                            receivers = new ArrayList<ReceiverRecord>();
                        }
                        receivers.add(receiver);
                        receiver.broadcasting = true;

                    } else {
                        if (debug) {
                            String reason;
                            switch (match) {
                                case IntentFilter.NO_MATCH_ACTION:
                                    reason = "action";
                                    break;
                                case IntentFilter.NO_MATCH_CATEGORY:
                                    reason = "category";
                                    break;
                                case IntentFilter.NO_MATCH_DATA:
                                    reason = "data";
                                    break;
                                case IntentFilter.NO_MATCH_TYPE:
                                    reason = "type";
                                    break;
                                default:
                                    reason = "unknown reason";
                                    break;
                            }
                            Log.v(TAG, "  Filter did not match: " + reason);
                        }
                    }
                }

                if (receivers != null) {
                    for (int i = 0; i < receivers.size(); i++) {
                        receivers.get(i).broadcasting = false;
                    }
                    //Change by Kiran Rao.
                    //Add the isOrdered boolean while constructing the BroadcastRecord.
                    mPendingBroadcasts.add(new BroadcastRecord(intent,
                            receivers, isOrdered));
                    //End of change.
                    if (!mHandler.hasMessages(MSG_EXEC_PENDING_BROADCASTS)) {
                        mHandler.sendEmptyMessage(MSG_EXEC_PENDING_BROADCASTS);
                    }
                    return true;
                }
            }
        }
        return false;

    }

    /**
     * Broadcast the given intent to all interested LocalBroadcastReceivers.
     * This call is asynchronous; it returns immediately, and you will continue
     * executing while the receivers are run.
     * 
     * @param intent The Intent to broadcast; all receivers matching this Intent
     *            will receive the broadcast.
     * @see #registerReceiver
     */
    public boolean sendBroadcast(Intent intent) {
        return sendBroadcast(intent, false);
    }

    public boolean sendOrderedBroadcast(Intent intent) {
        return sendBroadcast(intent, true);
    }

    /**
     * Like {@link #sendBroadcast(Intent)}, but if there are any receivers for
     * the Intent this function will block and immediately dispatch them before
     * returning.
     */
    public void sendBroadcastSync(Intent intent) {
        if (sendBroadcast(intent)) {
            executePendingBroadcasts();
        }
    }

    private void executePendingBroadcasts() {
        while (true) {
            BroadcastRecord[] brs = null;
            synchronized (mReceivers) {
                final int N = mPendingBroadcasts.size();
                if (N <= 0) {
                    return;
                }
                brs = new BroadcastRecord[N];
                mPendingBroadcasts.toArray(brs);
                mPendingBroadcasts.clear();
            }
            for (int i = 0; i < brs.length; i++) {

                BroadcastRecord br = brs[i];

                // Change by Kiran Rao
                if (br.isOrdered) {
                    // Do the sorting based on priority only if it is an ordered
                    // broadcast.
                    ReceiverRecord[] receiversAsArray = new ReceiverRecord[br.receivers
                            .size()];
                    br.receivers.toArray(receiversAsArray);

                    Arrays.sort(receiversAsArray,
                            new ReceiverRecordComparator());

                    // Examine each receiver in turn and execute its onReceive
                    // method;
                    // provided the broadcast has not been consumed already.
                    for (int j = receiversAsArray.length; j > 0; j--) {
                        LocalBroadcastReceiver receiver = receiversAsArray[j - 1].receiver;
                        receiver.onReceive(mAppContext, br.intent);
                        if (receiver.isBroadcastConsumed()) {
                            break;
                        }
                    }
                } else {
                    for (int j = 0; j < br.receivers.size(); j++) {
                        br.receivers.get(j).receiver.onReceive(mAppContext,
                                br.intent);
                    }

                }
                // End of change
            }
        }
    }
}
