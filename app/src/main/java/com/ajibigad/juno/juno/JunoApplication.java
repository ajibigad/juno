package com.ajibigad.juno.juno;

import android.app.Application;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by Julius on 27/02/2017.
 */
public class JunoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Realm
        Realm.init(getApplicationContext());

        // for debugging
        RealmConfiguration realmConfig = new RealmConfiguration.Builder().build();
        Realm.deleteRealm(realmConfig); // Delete Realm between app restarts.
        Realm.setDefaultConfiguration(realmConfig);
    }
}
