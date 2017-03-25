package com.ajibigad.juno.juno.database;

import com.ajibigad.juno.juno.model.Alert;
import com.ajibigad.juno.juno.model.AlertContract;

import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by Julius on 28/02/2017.
 */
public class AlertRepository {

    private Realm realm;

    public AlertRepository(){
        this.realm = Realm.getDefaultInstance();
    }

    public AlertRepository(Realm realm){
        this.realm = realm;
    }

    public Alert findAlert(){
        return null;
    }

    public void saveAlert(Alert alert){
        if(isTriggerExist(alert)){
            return;
        }
        if(!isExist(alert)){
            Number currentMaxID = realm.where(Alert.class).max(AlertContract.ID);
            int nextID = currentMaxID == null ? 1 : currentMaxID.intValue() + 1;
            alert.setId(nextID);
        }
        realm.copyToRealmOrUpdate(alert);
    }

    public boolean isTriggerExist(Alert alert){
        return realm.where(Alert.class).equalTo("triggerTime", alert.getTriggerTime()).findFirst() != null;
    }

    public boolean isExist(Alert alert){
        return realm.where(Alert.class).equalTo(AlertContract.ID, alert.getId()).findFirst() != null;
    }

    public RealmResults<Alert> findAll(){
        return realm.where(Alert.class).findAll();
    }

    public List<Alert> findAllAfterTriggerTime(Date triggerTime){
        return realm.where(Alert.class).greaterThanOrEqualTo(AlertContract.TRIGGER_TIME, triggerTime)
                .or().greaterThanOrEqualTo(AlertContract.NEXT_TRIGGER_TIME, triggerTime).findAllSorted(AlertContract.TRIGGER_TIME);
    }

    public Alert findNextAfterTriggerTime(Date triggerTime){
        return findAllAfterTriggerTime(triggerTime).isEmpty() ? null : findAllAfterTriggerTime(triggerTime).get(0);
    }

    public void delete(Alert alert){
        alert.deleteFromRealm();
    }
}
