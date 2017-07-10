package com.dadafactory.hellp;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabase;
    private StateModel mStateModel;
    private ImageButton mFoodWater;
    private ImageView mPet;

    HellpReceiver hellpReceiver = new HellpReceiver();

    static final int MSG_INIT_STATE = 1;
    static final int MSG_DRAW_STATE = 2;
    static final int MSG_SET_DATABASE = 3;

    private Handler mHandler;

    class StateHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SET_DATABASE:
                    setDatabase();
                    break;
                case MSG_INIT_STATE:
                    initState();
                    updateData();
                    break;
                case MSG_DRAW_STATE:
                    drawState();
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    private String getUid() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private void initState() {
        Log.d(TAG, "init state");
        mStateModel = new StateModel();
        mStateModel.setPetName(getString(R.string.pipi));
        mStateModel.setStartDate(System.currentTimeMillis());
        mStateModel.setFoodDate(System.currentTimeMillis());
    }

    private void drawState() {
        Log.d(TAG, "draw state");

        if (mStateModel.isFoodWater()) {
            mFoodWater.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.food_water, null));
        }
        else {
            mFoodWater.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.no_food_water, null));
        }

        if (mStateModel.getPetState() == 0) {
            mPet.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.chrt_n, null));
        }
        else if (mStateModel.getPetState() == 1) {
            mPet.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.chrt_frt_hgr_a, null));
        }
        else if (mStateModel.getPetState() == 2) {
            mPet.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.chrt_frt_hgr_b, null));
        }
        else if (mStateModel.getPetState() == 3) {
            mPet.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.chrt_frt_hgr_c, null));
        }
    }

    private void updateData() {
        Log.d(TAG, "update data");
        mDatabase.setValue(mStateModel);
    }

    /**
     *
     */
    Button.OnClickListener mFoodClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mStateModel.isFoodWater() == false) {
                mStateModel.setFoodWater(true);
            }
            mStateModel.setFoodDate(System.currentTimeMillis());
            updateData();
            hellpReceiver.setAlarm(MainActivity.this, Constants.PET_EATING_TIME);
        }
    };

    private void setDatabase () {
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        String uid = getUid();
        if (uid == null) {
            Log.e(TAG, "Fail to get user id");
            mHandler.sendMessage(Message.obtain(null, MSG_INIT_STATE));
            return;
        }

        mDatabase = FirebaseDatabase.getInstance().getReference().child("state").child(uid);

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "Read value");
                mStateModel = dataSnapshot.getValue(StateModel.class);

                int what;
                if (mStateModel == null) {
                    what = MSG_INIT_STATE;
                }
                else {
                    what = MSG_DRAW_STATE;
                }

                mHandler.sendMessage(Message.obtain(null, what));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        });
    }

    private void signIn() {
        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    mHandler.sendMessage(Message.obtain(null, MSG_SET_DATABASE));
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };

        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInAnonymously:success");
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInAnonymously:failure", task.getException());
                        }
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        mHandler = new StateHandler();
        signIn();

        setContentView(R.layout.activity_main);
        mPet = (ImageView)findViewById(R.id.img_character);
        mFoodWater = (ImageButton)findViewById(R.id.btn_food_water);
        mFoodWater.setOnClickListener(mFoodClickListener);
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }
}
