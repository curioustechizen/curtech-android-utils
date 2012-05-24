
package in.curtech.android.common;

import android.content.BroadcastReceiver;

/**
 * Extension of {@code BroadcastReceiver} meant to be used in conjunction with
 * {@link OrderEnabledLocalBroadcastManager}
 * 
 * @author Kiran Rao
 */
public abstract class LocalBroadcastReceiver extends BroadcastReceiver {

    /**
     * Flag indicating whether the broadcast has been consumed already.
     * Analogous to mAbortBroadcast.
     */
    protected boolean mConsumeBroadcast;

    
    /**
     * Returns the flag indicating whether or not this receiver should consume  the current broadcast.
     * @return true if the broadcast should be consumed.s
     */
    public final boolean isBroadcastConsumed() {
        return mConsumeBroadcast;
    }

    /**
     * Sets the flag indicating that this receiver should consume the current
     * broadcast; only works with broadcasts sent through
     * {@code OrderEnabledLocalBroadcastManager.sendOrderedBroadcast}.
     * 
     * This will prevent any other broadcast receivers from receiving the broadcast.
     */
    public final void consumeBroadcast() {
        mConsumeBroadcast = true;
    }

    /**
     * Clears the flag indicating that this receiver should consume the current broadcast.
     */
    public final void clearConsumeBroadcast() {
        mConsumeBroadcast = false;
    }

}
