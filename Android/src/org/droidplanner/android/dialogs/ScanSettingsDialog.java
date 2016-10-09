package org.droidplanner.android.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import org.beyene.sius.unit.length.LengthUnit;
import org.droidplanner.android.R;
import org.droidplanner.android.utils.Utils;
import org.droidplanner.android.utils.prefs.DroidPlannerPrefs;
import org.droidplanner.android.utils.unit.UnitManager;
import org.droidplanner.android.utils.unit.providers.length.LengthUnitProvider;
import org.droidplanner.android.utils.unit.systems.UnitSystem;
import org.droidplanner.android.view.spinnerWheel.CardWheelHorizontalView;
import org.droidplanner.android.view.spinnerWheel.adapters.LengthWheelAdapter;
import org.droidplanner.android.view.spinnerWheel.adapters.NumericWheelAdapter;

/**
 * Created by Kevin Langenkämper
 *
 * This class represents the Settings-Dialog for Object-Scanning
 */
public class ScanSettingsDialog extends DialogFragment {

    private double MIN_HEIGHT_DISTANCE_TO_OBJECT = 5;

    public class Settings{
        public double heightObject;
        public double heightFlight;
        public double secondsPerPicture;
        public int pictureCount;
        public boolean shouldTriggerCamera;
        public int roundCount;
        public double risePerRound;

        public Settings(double heightObject, double heightFlight, double secondsPerPicture, int pictureCount, boolean shouldTriggerCamera, int roundCount, double risePerRound) {
            this.heightObject = heightObject;
            this.heightFlight = heightFlight;
            this.secondsPerPicture = secondsPerPicture;
            this.pictureCount = pictureCount;
            this.shouldTriggerCamera = shouldTriggerCamera;
            this.roundCount = roundCount;
            this.risePerRound = risePerRound;
        }
    }

    public interface Listener {
        void onSettingsSet(Settings settings);
    }

    private View contentView;
    private LengthUnit heightObject;
    private LengthUnit heightFlight;
    private int secondsPerPicture;
    private int pictureCount;
    private boolean shouldTriggerCamera;
    private int roundCount;
    private LengthUnit risePerRound;

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
                .setTitle("Einstellungen")
                .setView(generateContentView(savedInstanceState))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DroidPlannerPrefs.getInstance(getContext()).setImageCount(pictureCount);
                        DroidPlannerPrefs.getInstance(getContext()).setImageTime(secondsPerPicture);
                        if (mListener != null) {
                            mListener.onSettingsSet(new Settings(heightObject.getValue(), heightFlight.getValue(), secondsPerPicture, pictureCount, shouldTriggerCamera, roundCount, risePerRound.getValue()));
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
        final View contentView = getActivity().getLayoutInflater().inflate(R.layout.dialog_scan_settings, null);

        if(contentView == null){
            return contentView;
        }
        final UnitSystem unitSystem = UnitManager.getUnitSystem(getContext());
        final LengthUnitProvider lengthUnitProvider = unitSystem.getLengthUnitProvider();
        final LengthWheelAdapter heightAdapterObject = new LengthWheelAdapter(getActivity(), R.layout.wheel_text_centered,
                lengthUnitProvider.boxBaseValueToTarget(Utils.MIN_DISTANCE), lengthUnitProvider.boxBaseValueToTarget(Utils.MAX_DISTANCE));
        final LengthWheelAdapter heightAdapterFlight = new LengthWheelAdapter(getActivity(), R.layout.wheel_text_centered,
                lengthUnitProvider.boxBaseValueToTarget(2), lengthUnitProvider.boxBaseValueToTarget(Utils.MAX_DISTANCE));
        final NumericWheelAdapter pictureCountAdapter = new NumericWheelAdapter(getActivity(), R.layout.wheel_text_centered, 1, 200, "%d");
        final NumericWheelAdapter cameraTriggerSpeedAdapter = new NumericWheelAdapter(getActivity(),R.layout.wheel_text_centered, 1, 10, "%d");


        heightObject = heightFlight = lengthUnitProvider.boxBaseValueToTarget(Utils.MIN_DISTANCE);

        final CardWheelHorizontalView<LengthUnit> altitudeObjectPicker = (CardWheelHorizontalView<LengthUnit>) contentView.findViewById(R.id.altitudeObjectPicker);
        altitudeObjectPicker.setViewAdapter(heightAdapterObject);

        final CardWheelHorizontalView<LengthUnit> risePerRoundPicker = (CardWheelHorizontalView<LengthUnit>) contentView.findViewById(R.id.risePerRound);
        risePerRoundPicker.setViewAdapter(heightAdapterObject);


        final CardWheelHorizontalView<LengthUnit> altitudeFlightPicker = (CardWheelHorizontalView<LengthUnit>) contentView.findViewById(R.id.altitudeFlightPicker);
        altitudeFlightPicker.setViewAdapter(heightAdapterFlight);

        final CardWheelHorizontalView<Integer> cameraPictureCountPicker = (CardWheelHorizontalView<Integer>) contentView.findViewById(R.id.cameraPictureCountPicker);
        cameraPictureCountPicker.setViewAdapter(pictureCountAdapter);

        final CardWheelHorizontalView<Integer> cameraTriggerSpeedPicker = (CardWheelHorizontalView<Integer>) contentView.findViewById(R.id.cameraTriggerSpeedPicker);
        cameraTriggerSpeedPicker.setViewAdapter(cameraTriggerSpeedAdapter);

        final CardWheelHorizontalView<Integer> roundCountPicker = (CardWheelHorizontalView<Integer>) contentView.findViewById(R.id.roundCountPicker);
        roundCountPicker.setViewAdapter(cameraTriggerSpeedAdapter);

        final CheckBox cameraTriggerCheckbox = (CheckBox) contentView.findViewById(R.id.cameraTriggerCheckbox);

        heightObject = altitudeObjectPicker.getCurrentValue();
        heightFlight = altitudeObjectPicker.getCurrentValue();
        pictureCount = DroidPlannerPrefs.getInstance(getContext()).getImageCount();
        cameraPictureCountPicker.setCurrentValue(pictureCount);
        secondsPerPicture = DroidPlannerPrefs.getInstance(getContext()).getImageTime();
        cameraPictureCountPicker.setCurrentValue(secondsPerPicture);
        shouldTriggerCamera = cameraTriggerCheckbox.isChecked();
        roundCount = roundCountPicker.getCurrentValue();
        risePerRound = risePerRoundPicker.getCurrentValue();


        CompoundButton.OnCheckedChangeListener checkboxlistener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                shouldTriggerCamera = checked;
                cameraTriggerSpeedPicker.setVisibility(checked ? View.VISIBLE : View.GONE);
                cameraPictureCountPicker.setVisibility(checked ? View.VISIBLE : View.GONE);
            }
        };

        CardWheelHorizontalView.OnCardWheelScrollListener<Integer> numericlistener = new CardWheelHorizontalView.OnCardWheelScrollListener<Integer>() {

            @Override
            public void onScrollingStarted(CardWheelHorizontalView cardWheel, Integer startValue) {

            }

            @Override
            public void onScrollingUpdate(CardWheelHorizontalView cardWheel, Integer oldValue, Integer newValue) {

            }

            @Override
            public void onScrollingEnded(CardWheelHorizontalView cardWheel, Integer startValue, Integer endValue) {
                if(cardWheel.getId() == R.id.cameraPictureCountPicker){
                    pictureCount = endValue;
                }else if(cardWheel.getId() == R.id.cameraTriggerSpeedPicker){
                    secondsPerPicture = endValue;
                }else{
                    roundCount = endValue;
                }
            }
        };


        CardWheelHorizontalView.OnCardWheelScrollListener<LengthUnit> lengthlistener = new CardWheelHorizontalView.OnCardWheelScrollListener<LengthUnit>(){

            @Override
            public void onScrollingStarted(CardWheelHorizontalView cardWheel, LengthUnit startValue) {

            }

            @Override
            public void onScrollingUpdate(CardWheelHorizontalView cardWheel, LengthUnit oldValue, LengthUnit newValue) {

            }

            @Override
            public void onScrollingEnded(CardWheelHorizontalView cardWheel, LengthUnit startValue, LengthUnit endValue) {
                if(cardWheel.getId() == R.id.altitudeFlightPicker){
                    heightFlight = endValue;
                }else if(cardWheel.getId() == R.id.altitudeObjectPicker){
                    heightObject = endValue;
                    if(heightFlight.getValue() < heightObject.getValue() + MIN_HEIGHT_DISTANCE_TO_OBJECT){
                        heightFlight = lengthUnitProvider.boxBaseValue(heightObject.getValue() + MIN_HEIGHT_DISTANCE_TO_OBJECT);
                    }
                    altitudeFlightPicker.setCurrentValue(heightFlight);
                }else{
                    risePerRound = endValue;
                }
            }
        };

        altitudeObjectPicker.addScrollListener(lengthlistener);
        altitudeFlightPicker.addScrollListener(lengthlistener);
        cameraPictureCountPicker.addScrollListener(numericlistener);
        cameraTriggerSpeedPicker.addScrollListener(numericlistener);
        roundCountPicker.addScrollListener(numericlistener);
        cameraTriggerCheckbox.setOnCheckedChangeListener(checkboxlistener);
        risePerRoundPicker.addScrollListener(lengthlistener);
        return contentView;
    }


}
