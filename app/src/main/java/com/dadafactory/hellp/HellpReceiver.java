package com.dadafactory.hellp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HellpReceiver extends BroadcastReceiver {
    private static final String TAG = "HellpReceiver";

    private DatabaseReference mDatabase;
    private StateModel mStateModel;
    private Context mContext;

    static final int MSG_UPDATE_STATE = 3;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handle message");

            switch (msg.what) {
                case 3:
                    updateState();
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    private String getUid() {
        String uid;
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d(TAG, "uid="+uid);
        return uid;
    }

    private void updateData() {
        Log.d(TAG, "update data");
        mDatabase.setValue(mStateModel);
    }

    private void updateState() {
        Log.d(TAG, "update state");
        boolean needUpdateData = false;

        long currentTime = System.currentTimeMillis();

        if (mStateModel.isFoodWater()) {
            if (currentTime >= mStateModel.getFoodDate() + Constants.PET_EATING_TIME) {
                mStateModel.setFoodWater(false);
                needUpdateData = true;
                this.setAlarm(mContext, Constants.TWO_DAYS);
            }
        }

        int petState;
        if (currentTime >= mStateModel.getFoodDate() + Constants.SEVEN_DAYS) {
            //game over;
            petState = 4;
        }
        else if (currentTime >= mStateModel.getFoodDate() + Constants.FIVE_DAYS) {
            petState = 3;
        }
        else if (currentTime >= mStateModel.getFoodDate() + Constants.THREE_DAYS) {
            petState = 2;
        }
        else if (currentTime >= mStateModel.getFoodDate() + Constants.TWO_DAYS) {
            petState = 1;
        }
        else {
            petState = 0;
        }

        if (mStateModel.getPetState() != petState) {
            Log.d(TAG, "set pet state from " + mStateModel.getPetState() + " to " + petState);
            mStateModel.setPetState(petState);
            switch (petState) {
                case 1:
                    this.setAlarm(mContext, Constants.THREE_DAYS - Constants.TWO_DAYS);
                    break;
                case 2:
                    this.setAlarm(mContext, Constants.FIVE_DAYS - Constants.THREE_DAYS);
                    break;
                case 3:
                    this.setAlarm(mContext, Constants.SEVEN_DAYS - Constants.FIVE_DAYS);
                    break;
            }
            needUpdateData = true;
        }

        if (needUpdateData) {
            updateData();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "on receive");

        mContext = context;
        mDatabase = FirebaseDatabase.getInstance().getReference().child("state").child(getUid());

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "Read value");
                mStateModel = dataSnapshot.getValue(StateModel.class);
                if (mStateModel == null) {
                    Log.e(TAG, "Fail to get state model!!");
                }
                else {
                    mHandler.sendMessage(Message.obtain(null, MSG_UPDATE_STATE));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        });
    }

    public void setAlarm(Context context, long ms)
    {
        Log.d(TAG, "set alarm ms="+ms);

        AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, HellpReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);

        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+ms, pi);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + ms, pi);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + ms, pi);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + ms, pi);
        }
    }

    public void cancelAlarm(Context context)
    {
        Intent intent = new Intent(context, HellpReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}
