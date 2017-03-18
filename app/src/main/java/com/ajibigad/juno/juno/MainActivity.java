package com.ajibigad.juno.juno;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity {

    private RecyclerView alarmRecyclerView;
    private RecyclerView.Adapter alertAdapter;
    private RecyclerView.LayoutManager alertLayoutManager;

    private AlertRepository repository;
    private  Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
        repository = new AlertRepository(realm);
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
                        now.set(Calendar.HOUR, hourOfDay);
                        now.set(Calendar.MINUTE, minute);
                        String snackMessage = String.format("Time selected: %d:%d", hourOfDay, minute);
                        Snackbar.make(fView, snackMessage, Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                        final Alert rAlert = new Alert();
                        rAlert.setMessage("Let's get the day started");
                        rAlert.setTriggerTime(now.getTime());
                        realm.executeTransactionAsync(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                repository.saveAlert(rAlert);
                            }
                        }, new Realm.Transaction.OnSuccess() {
                            @Override
                            public void onSuccess() {
                                Snackbar.make(fView, "Alert saved successfully", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                        }, new Realm.Transaction.OnError() {
                            @Override
                            public void onError(Throwable error) {
                                Snackbar.make(fView, "Error saving alert", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                        });
                    }
                };
                TimePickerDialog dialog = new TimePickerDialog(
                        MainActivity.this, listener
                        , now.get(Calendar.HOUR), now.get(Calendar.MINUTE), false);
                dialog.show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        alarmRecyclerView.setAdapter(null);
        realm.close();
    }

}
