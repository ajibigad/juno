package com.ajibigad.juno.juno;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TimePicker;

import com.ajibigad.juno.juno.database.AlertRepository;
import com.ajibigad.juno.juno.model.Alert;
import com.ajibigad.juno.juno.service.AlertManager;

import java.util.Calendar;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback{

    private RecyclerView alarmRecyclerView;
    private RecyclerView.Adapter alertAdapter;
    private RecyclerView.LayoutManager alertLayoutManager;
    private View mLayout;

    /**
     * Id to identify a wake lock permission request.
     */
    private static final int REQUEST_WAKE_LOCK = 0;

    /**
     * Id to identify a fingerprint permission request.
     */
    private static final int REQUEST_FINGERPRINT = 1;

    public Realm realm;

    private static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLayout = findViewById(R.id.activity_main_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        alarmRecyclerView = (RecyclerView) findViewById(R.id.alarm_list);
        alarmRecyclerView.setHasFixedSize(false);
        
        alertLayoutManager = new LinearLayoutManager(this);
        alarmRecyclerView.setLayoutManager(alertLayoutManager);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(alarmRecyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        alarmRecyclerView.addItemDecoration(dividerItemDecoration);

        realm = Realm.getDefaultInstance();
        final RealmResults<Alert> alerts = realm.where(Alert.class).findAllAsync();
        alertAdapter = new AlertAdapter(alerts);
        alarmRecyclerView.setAdapter(alertAdapter);

        // Listeners will be notified when data changes
        alerts.addChangeListener(new RealmChangeListener<RealmResults<Alert>>() {
            @Override
            public void onChange(RealmResults<Alert> results) {
                // Query results are updated in real time
                new AlertManager(MainActivity.this).createAlarmTrigger();
                alertAdapter.notifyDataSetChanged();
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //display time picker to get alarm trigger time
                //display snackbar to show undo creation
                final Calendar now = Calendar.getInstance();
                final View fView = view;
                TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePickerView, int hourOfDay, int minute) {
                        now.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        now.set(Calendar.MINUTE, minute);
                        String snackMessage = String.format("Time selected: %d:%d", hourOfDay, minute);
                        Snackbar.make(fView, snackMessage, Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        final Alert rAlert = new Alert();
                        rAlert.setMessage("Let's get the day started");
                        rAlert.setTriggerTime(now.getTime());
                        realm.executeTransactionAsync(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                AlertRepository repository = new AlertRepository(realm);
                                repository.saveAlert(rAlert);
                            }
                        }, new Realm.Transaction.OnSuccess() {
                            @Override
                            public void onSuccess() {
//                                Snackbar.make(fView, "Alert saved successfully " + alerts.size(), Snackbar.LENGTH_LONG)
//                                        .setAction("Action", null).show();
                            }
                        }, new Realm.Transaction.OnError() {
                            @Override
                            public void onError(Throwable error) {
                                Snackbar.make(fView, "Error saving alert" , Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                                error.printStackTrace();
                            }
                        });
                    }
                };
                TimePickerDialog dialog = new TimePickerDialog(MainActivity.this, listener,
                        now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false);
                dialog.show();
            }
        });

        if (Build.VERSION.SDK_INT >= 23) {
            requestWakeLockPermission();
        }
    }

    private void requestWakeLockPermission(){
        Log.i(TAG, "Show Wake lock button pressed. Checking permission.");
        // Check if the Wake lock permission is already available.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK)
                != PackageManager.PERMISSION_GRANTED) {
            // Wake lock permission has not been granted.

            Log.i(TAG, "Wake lock permission has NOT been granted. Requesting permission.");

            // BEGIN_INCLUDE(Wake lock_permission_request)
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WAKE_LOCK)) {
                // Provide an additional rationale to the user if the permission was not granted
                // and the user would benefit from additional context for the use of the permission.
                // For example if the user has previously denied the permission.
                Log.i(TAG,
                        "Displaying Wake lock permission rationale to provide additional context.");
                Snackbar.make(mLayout, R.string.permission_wake_lock_rationale,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.WAKE_LOCK},
                                        REQUEST_WAKE_LOCK);
                            }
                        })
                        .show();
            } else {

                // Wake lock permission has not been granted yet. Request it directly.
                Log.i(TAG,
                        "Wake lock permission has not been granted yet. Request it directly");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WAKE_LOCK},
                        REQUEST_WAKE_LOCK);
            }

        } else {

            // Wake lock permissions is already available,
            Log.i(TAG,
                    "WAKE LOCK permission has already been granted.");
        }
    }


    private void requestFingerPrintPermission(){
        Log.i(TAG, "Show Fingerprint button pressed. Checking permission.");
        // Check if the Fingerprint permission is already available.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT)
                != PackageManager.PERMISSION_GRANTED) {
            // Fingerprint permission has not been granted.

            Log.i(TAG, "Fingerprint permission has NOT been granted. Requesting permission.");

            // BEGIN_INCLUDE(Fingerprint_permission_request)
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.USE_FINGERPRINT)) {
                // Provide an additional rationale to the user if the permission was not granted
                // and the user would benefit from additional context for the use of the permission.
                // For example if the user has previously denied the permission.
                Log.i(TAG,
                        "Displaying Fingerprint permission rationale to provide additional context.");
                Snackbar.make(mLayout, R.string.permission_fingerprint_rationale,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.USE_FINGERPRINT},
                                        REQUEST_FINGERPRINT);
                            }
                        })
                        .show();
            } else {

                // Fingerprint permission has not been granted yet. Request it directly.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.USE_FINGERPRINT},
                        REQUEST_FINGERPRINT);
            }

        } else {

            // Fingerprint permissions is already available,
            Log.i(TAG,
                    "FINGERPRINT permission has already been granted.");
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == REQUEST_WAKE_LOCK) {
            // BEGIN_INCLUDE(permission_result)
            // Received permission result for wake lock permission.
            Log.i(TAG, "Received response for Wake lock permission request.");

            // Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Wake lock permission has been granted, preview can be displayed
                Log.i(TAG, "Wake lock permission has now been granted. Showing preview.");
                Snackbar.make(mLayout, R.string.permision_available_wake_lock,
                        Snackbar.LENGTH_SHORT).show();
            } else {
                Log.i(TAG, "Wake lock permission was NOT granted.");
                Snackbar.make(mLayout, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT).show();

            }
            requestFingerPrintPermission();
            // END_INCLUDE(permission_result)

        } else if (requestCode == REQUEST_FINGERPRINT) {
            Log.i(TAG, "Received response for Fingerprint permissions request.");

            // We have requested multiple permissions for Fingerprints, so all of them need to be
            // checked.
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // All required permissions have been granted, display Fingerprints fragment.
                Snackbar.make(mLayout, R.string.permision_available_fingeprint,
                        Snackbar.LENGTH_SHORT)
                        .show();
            } else {
                Log.i(TAG, "Fingerprints permissions were NOT granted.");
                Snackbar.make(mLayout, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT)
                        .show();
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        alarmRecyclerView.setAdapter(null);
        realm.close();
    }

}
