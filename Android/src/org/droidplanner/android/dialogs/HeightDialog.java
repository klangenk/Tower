package org.droidplanner.android.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.beyene.sius.unit.length.LengthUnit;
import org.droidplanner.android.R;
import org.droidplanner.android.utils.Utils;
import org.droidplanner.android.utils.unit.UnitManager;
import org.droidplanner.android.utils.unit.providers.length.LengthUnitProvider;
import org.droidplanner.android.utils.unit.systems.UnitSystem;
import org.droidplanner.android.view.spinnerWheel.CardWheelHorizontalView;
import org.droidplanner.android.view.spinnerWheel.adapters.LengthWheelAdapter;

/**
 * Created by Kevin on 19.05.2016.
 */
public class HeightDialog extends DialogFragment {

    private double MIN_HEIGHT_DISTANCE_TO_OBJECT = 5;

    public interface Listener {
        void onHeightSet(double heightObject, double heightFlight);
    }

    private View contentView;
    private LengthUnit heightObject;
    private LengthUnit heightFlight;

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
                .setTitle("Höhe einstellen")
                .setView(generateContentView(savedInstanceState))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mListener != null) {
                            mListener.onHeightSet(heightObject.getValue(), heightFlight.getValue());
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
        final View contentView = getActivity().getLayoutInflater().inflate(R.layout.dialog_height, null);

        if(contentView == null){
            return contentView;
        }
        final UnitSystem unitSystem = UnitManager.getUnitSystem(getContext());
        final LengthUnitProvider lengthUnitProvider = unitSystem.getLengthUnitProvider();
        final LengthWheelAdapter heightAdapter = new LengthWheelAdapter(getActivity(), R.layout.wheel_text_centered,
                lengthUnitProvider.boxBaseValueToTarget(Utils.MIN_DISTANCE), lengthUnitProvider.boxBaseValueToTarget(Utils.MAX_DISTANCE));


        heightObject = heightFlight = lengthUnitProvider.boxBaseValueToTarget(Utils.MIN_DISTANCE);

        final CardWheelHorizontalView<LengthUnit> altitudeObjectPicker = (CardWheelHorizontalView<LengthUnit>) contentView.findViewById(R.id.altitudeObjectPicker);
        altitudeObjectPicker.setViewAdapter(heightAdapter);


        final CardWheelHorizontalView<LengthUnit> altitudeFlightPicker = (CardWheelHorizontalView<LengthUnit>) contentView.findViewById(R.id.altitudeFlightPicker);
        altitudeFlightPicker.setViewAdapter(heightAdapter);


        CardWheelHorizontalView.OnCardWheelScrollListener<LengthUnit> listener = new CardWheelHorizontalView.OnCardWheelScrollListener<LengthUnit>(){

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
                }else{
                    heightObject = endValue;

                    altitudeFlightPicker.setViewAdapter(new LengthWheelAdapter(getActivity(), R.layout.wheel_text_centered,
                            heightObject, lengthUnitProvider.boxBaseValueToTarget(Utils.MAX_DISTANCE)));
                    if(heightFlight.getValue() < heightObject.getValue() + MIN_HEIGHT_DISTANCE_TO_OBJECT){
                        heightFlight = lengthUnitProvider.boxBaseValue(heightObject.getValue() + MIN_HEIGHT_DISTANCE_TO_OBJECT);

                    }
                    altitudeFlightPicker.setCurrentValue(heightFlight);


                }
            }
        };

        altitudeObjectPicker.addScrollListener(listener);
        altitudeFlightPicker.addScrollListener(listener);
        return contentView;
    }


}
