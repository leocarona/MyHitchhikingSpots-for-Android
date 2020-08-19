package com.myhitchhikingspots.db;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.myhitchhikingspots.Constants;
import com.myhitchhikingspots.model.Usuario;

import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

public class UsuariosRepository {

    private static UsuariosRepository sInstance;
    private MutableLiveData<Usuario> mUsuario;

    public static UsuariosRepository getInstance() {
        if (sInstance == null) {
            synchronized (UsuariosRepository.class) {
                if (sInstance == null) {
                    sInstance = new UsuariosRepository();
                }
            }
        }
        return sInstance;
    }

    private UsuariosRepository() {
        mUsuario = new MutableLiveData<>();
    }

    private final ValueEventListener eventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            Usuario usuario = dataSnapshot.getValue(Usuario.class);
            mUsuario.setValue(usuario);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };

    public void subscribeTo(String usuarioId) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(Constants.FIREBASE_DATABASE_USUARIOS_PATH).child(usuarioId);

        //Make sure event listener isn't subscribed more than once
        myRef.addValueEventListener(eventListener);
    }

    public void unsubscribeFrom(String usuarioId) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(Constants.FIREBASE_DATABASE_USUARIOS_PATH).child(usuarioId);

        myRef.removeEventListener(eventListener);
    }

    public LiveData<Usuario> getUsuario() {
        return mUsuario;
    }

    public String addNewUsuario() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(Constants.FIREBASE_DATABASE_USUARIOS_PATH);

        DatabaseReference usuarioRef = myRef.push();
        String usuarioId = usuarioRef.getKey();

        if (usuarioId != null) {
            myRef.child(usuarioId).child(Constants.FIREBASE_DATABASE_USUARIO_ID_PATH).setValue(usuarioId);
            myRef.child(usuarioId).child(Constants.FIREBASE_DATABASE_USUARIO_REGISTERED_SINCE_PATH).setValue(DateTime.now().toString());
        }

        return usuarioId;
    }

    public void replaceUserKey(@NonNull String usuarioId, @NonNull String newUsuarioId) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(Constants.FIREBASE_DATABASE_USUARIOS_PATH);

        myRef.child(usuarioId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Usuario usuario = dataSnapshot.getValue(Usuario.class);
                myRef.child(newUsuarioId).setValue(usuario);
                myRef.child(usuarioId).removeValue();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void updateLastAccessAt(@NonNull String usuarioId, @NonNull String firebaseLoginId) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(Constants.FIREBASE_DATABASE_USUARIOS_PATH);

        myRef.child(usuarioId).child(Constants.FIREBASE_DATABASE_USUARIO_LAST_FB_ACCESS_AT_PATH).setValue(DateTime.now().toString());
        myRef.child(usuarioId).child(Constants.FIREBASE_DATABASE_USUARIO_LAST_FB_LOGIN_ID_PATH).setValue(firebaseLoginId);
    }

    public void updateLastHWLoginAt(@NonNull String usuarioId) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(Constants.FIREBASE_DATABASE_USUARIOS_PATH);

        myRef.child(usuarioId).child(Constants.FIREBASE_DATABASE_USUARIO_HW_LOGIN_AT_PATH).setValue(DateTime.now().toString());
    }

    public Task<Void> updateHwUsername(@NonNull String usuarioId, @NonNull String username) {
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.FIREBASE_DATABASE_USUARIO_HW_USERNAME_PATH, username);
        map.put(Constants.FIREBASE_DATABASE_USUARIO_HW_LOGIN_AT_PATH, DateTime.now().toString());

        return updateChildren(usuarioId, map);
    }

    private Task<Void> updateChildren(@NonNull String usuarioId, @NonNull Map<String, Object> map) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(Constants.FIREBASE_DATABASE_USUARIOS_PATH);

        return myRef.child(usuarioId).updateChildren(map);
    }

    public void updateToken(@NonNull String usuarioId, @NonNull String newToken) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(Constants.FIREBASE_DATABASE_USUARIOS_PATH);

        myRef.child(usuarioId).child(Constants.FIREBASE_DATABASE_USUARIO_LAST_FB_TOKEN_PATH).setValue(newToken);
    }

    public void updateLastKnownLocation(@NonNull String usuarioId, Location loc) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(Constants.FIREBASE_DATABASE_USUARIOS_LAST_KNOWN_LOCATION_PATH).child(usuarioId);

        myRef.child(Constants.FIREBASE_DATABASE_USUARIO_LAST_LOCATION_POSITION_PATH).setValue(loc.getLatitude() + "," + loc.getLongitude());
        myRef.child(Constants.FIREBASE_DATABASE_USUARIO_LAST_LOCATION_DATETIME_PATH).setValue(DateTime.now().toString());
    }
}
