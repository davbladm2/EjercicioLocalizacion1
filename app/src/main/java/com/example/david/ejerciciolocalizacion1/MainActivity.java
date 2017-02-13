package com.example.david.ejerciciolocalizacion1;

import android.*;
import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,LocationListener,OnMapReadyCallback{

    TextView tvLatitud,tvLonguitud;
    ToggleButton btnActualizar;
    GoogleApiClient apiClient;
    private static final int PETICION_PERMISO_LOCALIZACION = 100;
    private static final int PETICION_CONFIG_UBICACION = 201;
    private LocationRequest locRequest;
    private GoogleMap mapa;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLatitud=(TextView) findViewById(R.id.lblLatitud);
        tvLonguitud=(TextView) findViewById(R.id.lblLonguitud);
        btnActualizar=(ToggleButton) findViewById(R.id.btnActualizar);
        btnActualizar.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                toggleLocationUpdates(btnActualizar.isChecked());
            }
        });
        apiClient=new GoogleApiClient.Builder(this).enableAutoManage(this,this).addConnectionCallbacks(this).addApi(LocationServices.API).build();

        //PARA EL MAPA
        MapFragment mapFragment=(MapFragment)getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    //Evento button
    private void toggleLocationUpdates(boolean checked){
        if (checked){
            enableLocationUpdates();
        }else{
            disableLocationUpdates();
        }
    }

    private void enableLocationUpdates(){
        locRequest=new LocationRequest();
        locRequest.setInterval(2000);
        locRequest.setFastestInterval(1000);
        locRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest locSettingsRequest=new LocationSettingsRequest.Builder().addLocationRequest(locRequest).build();

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(
                        apiClient, locSettingsRequest);

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:

                        Log.i("OK", "Configuración correcta");
                        startLocationUpdates();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            Log.i("--", "Se requiere actuación del usuario");
                            status.startResolutionForResult(MainActivity.this, PETICION_CONFIG_UBICACION);
                        } catch (IntentSender.SendIntentException e) {
                            btnActualizar.setChecked(false);
                            Log.i("ERROR", "Error al intentar solucionar configuración de ubicación");
                        }
                        break;

                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.i("ERROR", "No se puede cumplir la configuración de ubicación necesaria");
                        btnActualizar.setChecked(false);
                        break;
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode){
            case PETICION_CONFIG_UBICACION:
                switch (resultCode){
                    case MainActivity.RESULT_OK:
                        startLocationUpdates();
                        break;
                    case MainActivity.RESULT_CANCELED:
                        Log.i("--","El usuario no ha realizado los cambios en la configuración necesarios");
                        btnActualizar.setChecked(false);
                        break;
                }
        }
    }

    private void startLocationUpdates(){
        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
            Log.i("--","Inicio de recepción de ubicaciones");
            LocationServices.FusedLocationApi.requestLocationUpdates(apiClient,locRequest,MainActivity.this);
        }
    }

    private void disableLocationUpdates(){
        LocationServices.FusedLocationApi.removeLocationUpdates(apiClient,this);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i("---","Recibida nueva ubicación!");
        updateUI(location);
    }

    //PARTE 1

    private void updateUI(Location loc){
        if(loc!=null){
            tvLatitud.setText("Latitud: "+String.valueOf(loc.getLatitude()));
            tvLonguitud.setText("Longuitud: "+String.valueOf(loc.getLongitude()));
            CameraUpdate camp= CameraUpdateFactory.newLatLngZoom(new LatLng(Double.valueOf(String.valueOf(loc.getLatitude())),Double.valueOf(String.valueOf(loc.getLongitude()))),20);
            mapa.moveCamera(camp);
            mapa.addMarker(new MarkerOptions().position(new LatLng(Double.valueOf(String.valueOf(loc.getLatitude())),Double.valueOf(String.valueOf(loc.getLongitude())))));
        }else{
            tvLatitud.setText("Latitud: (desconocida)");
            tvLonguitud.setText("Longuitud: (desconocida)");
        }
    }

    public void onConnectionFailed(ConnectionResult result){
        Log.e("Error","Error grave al conectar con Google Play Services");
    }
    public void onConnected(Bundle bundle){
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},PETICION_PERMISO_LOCALIZACION);
        }else{
            Location lastLocation=LocationServices.FusedLocationApi.getLastLocation(apiClient);
            updateUI(lastLocation);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==PETICION_PERMISO_LOCALIZACION){
            if(grantResults.length==1 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                //Permiso concedido
                @SuppressWarnings("MissingPermission")
                Location lastLocation=LocationServices.FusedLocationApi.getLastLocation(apiClient);
                updateUI(lastLocation);
            }else{
                Log.e("ERROR","Permiso Denegado");
            }
        }
    }
    public void onConnectionSuspended(int i){
        Log.e("Error","Se ha interrumpido la conexión con Google Play Services");
    }

    //MAPAS

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapa=googleMap;
        mapa.getUiSettings().setZoomControlsEnabled(true);
    }
}
