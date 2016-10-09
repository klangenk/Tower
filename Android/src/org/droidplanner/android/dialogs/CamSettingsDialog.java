package org.droidplanner.android.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import org.droidplanner.android.R;
import org.droidplanner.android.utils.Raspberry;
import org.droidplanner.android.utils.prefs.DroidPlannerPrefs;

/**
 * Created by Kevin Langenkämper
 *
 * This class represents the Settings-Dialog for the Camera
 */
public class CamSettingsDialog extends DialogFragment {


    public class Settings{
        public double angleOfView;
        public Raspberry.CaptureType captureType;


        public Settings(double angleOfView, Raspberry.CaptureType captureType) {
            this.angleOfView = angleOfView;
            this.captureType = captureType;
        }
    }

    public interface Listener {
        void onSettingsSet(Settings settings);
    }

    private View contentView;
    Spinner captureTypeSpinner;
    EditText cameraAngleEdit;

    protected Listener mListener;

    public void setListener(Listener listener){
        mListener = listener;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        Object parent = getParentFragment();
        if(parent == null)
            parent = activity;

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return buildDialog(savedInstanceState).create();
    }

    protected AlertDialog.Builder buildDialog(Bundle savedInstanceState){
        final Bundle arguments = getArguments();

        LayoutInflater inflater = getActivity().getLayoutInflater();
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity())
                .setTitle("Kamera-Einstellungen")
                .setView(generateContentView(savedInstanceState))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mListener != null) {
                            try {
                                float angle = Float.parseFloat(cameraAngleEdit.getText().toString());
                                DroidPlannerPrefs prefs = DroidPlannerPrefs.getInstance(getContext());
                                prefs.setCaptureType(captureTypeSpinner.getSelectedItemPosition());
                                prefs.setCameraAngle(angle);

                                mListener.onSettingsSet(new Settings(angle, (Raspberry.CaptureType) captureTypeSpinner.getSelectedItem()));
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }

                        }
                    }
                })
                .setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        return b;
    }

    protected View generateContentView(Bundle savedInstanceState){
        final View contentView = getActivity().getLayoutInflater().inflate(R.layout.dialog_cam_settings, null);

        if(contentView == null){
            return contentView;
        }

        captureTypeSpinner = (Spinner) contentView.findViewById(R.id.captureTypeSpinner);
        ArrayAdapter<Raspberry.CaptureType> adapter = new ArrayAdapter<Raspberry.CaptureType>(getContext(), R.layout.spinner_camsettings_item, Raspberry.CaptureType.values());
        captureTypeSpinner.setAdapter(adapter);

        cameraAngleEdit = (EditText) contentView.findViewById(R.id.angleOfViewEdit);

        DroidPlannerPrefs prefs = DroidPlannerPrefs.getInstance(getContext());
        captureTypeSpinner.setSelection(prefs.getCaptureType());
        cameraAngleEdit.setText(String.valueOf(prefs.getCameraAngle()));

        return contentView;
    }


}
