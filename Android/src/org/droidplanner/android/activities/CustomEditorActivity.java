package org.droidplanner.android.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.services.android.lib.drone.property.Parameter;
import com.o3dr.services.android.lib.drone.property.Parameters;

import org.beyene.sius.unit.length.LengthUnit;
import org.droidplanner.android.R;
import org.droidplanner.android.dialogs.CamSettingsDialog;
import org.droidplanner.android.dialogs.ScanSettingsDialog;
import org.droidplanner.android.dialogs.SupportYesNoWithPrefsDialog;
import org.droidplanner.android.proxy.mission.MissionProxy;
import org.droidplanner.android.utils.Raspberry;
import org.droidplanner.android.utils.prefs.DroidPlannerPrefs;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kevin Langenkämper
 *
 * This class is a modified subclass of the EditoActivity
 */
public class CustomEditorActivity extends EditorActivity {


    public static final double MAX_SPEED = 5; //meters per second.
    public static final double MIN_SPEED = 0.5; //meters per second.

    private double currentSpeed = DEFAULT_SPEED;
    private int currentRounds = 0;
    private TextView speedRoundinfoView;
    private FloatingActionButton camSettings;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        speedRoundinfoView = (TextView) findViewById(R.id.editorSpeedRoundInfoWindow);
        camSettings = (FloatingActionButton) findViewById(R.id.camera_settings);
        camSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CamSettingsDialog dlg = new CamSettingsDialog();
                dlg.setListener(new CamSettingsDialog.Listener() {

                    //this is the callback for the ok button in the dialog
                    @Override
                    public void onSettingsSet(final CamSettingsDialog.Settings settings) {
                        Raspberry.setCaptureType(settings.captureType);

                    }
                });
                dlg.show(editorToolsFragment.getFragmentManager(), "CAM_SETTINGS");
            }
        });
    }

    public void setSpeedAndRounds(double speed, int rounds){
        currentSpeed = speed;
        currentRounds = rounds;
        updateMissionLength();
        speedRoundinfoView.setText(String.format("Runden: %d  Geschwindigkeit: %.2f m/s", currentRounds, currentSpeed));
    }





    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_upload_mission: {
                final Drone dpApi = dpApp.getDrone();
                final MissionProxy missionProxy = dpApp.getMissionProxy();
                List<Parameter> parameters = sendSpeedToAPM();
                VehicleApi.getApi(editorToolsFragment.getDrone()).writeParameters(new Parameters(parameters));
                if (missionProxy.getItems().isEmpty() || missionProxy.hasTakeoffAndLandOrRTL()) {
                    missionProxy.sendMissionToAPM(dpApi);
                } else {
                    SupportYesNoWithPrefsDialog dialog = SupportYesNoWithPrefsDialog.newInstance(
                            getApplicationContext(), MISSION_UPLOAD_CHECK_DIALOG_TAG,
                            getString(R.string.mission_upload_title),
                            getString(R.string.mission_upload_message),
                            getString(android.R.string.ok),
                            getString(R.string.label_skip),
                            DroidPlannerPrefs.PREF_AUTO_INSERT_MISSION_TAKEOFF_RTL_LAND, this);

                    if (dialog != null) {
                        dialog.show(getSupportFragmentManager(), MISSION_UPLOAD_CHECK_DIALOG_TAG);
                    }

                }
                return true;
            }

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @NonNull
    private List<Parameter> sendSpeedToAPM() {
        Parameter p = new Parameter("WPNAV_SPEED", (int)(currentSpeed * 100), 9);
        List<Parameter> parameters = new ArrayList<Parameter>();
        parameters.add(p);
        return parameters;
    }

    private void updateMissionLength() {
        if (missionProxy != null) {

            double missionLength = missionProxy.getMissionLength();
            LengthUnit convertedMissionLength = unitSystem.getLengthUnitProvider().boxBaseValueToTarget(missionLength);
            double speedParameter = dpApp.getDrone().getSpeedParameter() / 100; //cm/s to m/s conversion.
            if (speedParameter == 0)
                speedParameter = currentSpeed;

            int time = (int) (missionLength / speedParameter);

            String infoString = getString(R.string.editor_info_window_distance, convertedMissionLength.toString())
                    + ", " + getString(R.string.editor_info_window_flight_time, time / 60, time % 60);

            infoView.setText(infoString);

            // Remove detail window if item is removed
            if (missionProxy.selection.getSelected().isEmpty() && itemDetailFragment != null) {
                removeItemDetail();
            }
        }
    }


}
