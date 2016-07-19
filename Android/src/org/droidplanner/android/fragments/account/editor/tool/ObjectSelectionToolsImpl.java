package org.droidplanner.android.fragments.account.editor.tool;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.AdapterView;

import com.o3dr.android.client.Drone;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.mission.MissionItemType;
import com.o3dr.services.android.lib.drone.mission.item.MissionItem;
import com.o3dr.services.android.lib.drone.mission.item.command.CameraTrigger;
import com.o3dr.services.android.lib.drone.mission.item.command.ReturnToLaunch;
import com.o3dr.services.android.lib.drone.mission.item.command.Takeoff;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.util.MathUtils;

import org.droidplanner.android.activities.EditorActivity;
import org.droidplanner.android.dialogs.OkDialog;
import org.droidplanner.android.dialogs.ScanSettingsDialog;

import java.util.ArrayList;
import java.util.List;


class ObjectSelectionToolsImpl extends DrawToolsImpl implements AdapterView.OnItemSelectedListener {

    private static double cameraAngleVertical = 97;
    private static double secondsPerPicture = 4;
    private double currentSpeed = EditorActivity.DEFAULT_SPEED;
    private double currentDistance = 0;
    private int currentRounds = 0;

    ObjectSelectionToolsImpl(EditorToolsFragment fragment) {
        super(fragment);
    }

    void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    void onRestoreInstanceState(Bundle savedState) {
        super.onRestoreInstanceState(savedState);

    }

    @Override
    public EditorToolsFragment.EditorTools getEditorTools() {
        return EditorToolsFragment.EditorTools.DRAW;
    }


    @Override
    public void setup() {
        super.setup();
        EditorToolsFragment.EditorToolListener listener = editorToolsFragment.listener;
        if (listener != null) {
            listener.setGestureMode(CustomGestureOverlayView.GestureMode.RECTANGLE);
        }

    }

    @Override
    public void onPathFinished(final List<LatLong> points) {
        if (missionProxy != null) {
            if (points.size() == 4) {

                ScanSettingsDialog dlg = new ScanSettingsDialog();
                dlg.setListener(new ScanSettingsDialog.Listener() {
                    @Override
                    public void onSettingsSet(final ScanSettingsDialog.Settings settings) {

                        final MissionItem item = calculateRegionOfInterest(points, settings.heightObject / 2);
                        final List<LatLongAlt> waypoints = calculateWaypointsForObject(points, settings.heightFlight, settings.heightObject, 97);

                        if(settings.shouldTakePictures){

                            final double roundDistance = 2 * MathUtils.getDistance2D(waypoints.get(0), waypoints.get(1)) + 2 *MathUtils.getDistance2D(waypoints.get(1), waypoints.get(2));
                            currentDistance =  settings.roundCount * roundDistance;
                            currentSpeed = calcSpeed(currentDistance,settings.secondsPerPicture, settings.pictureCount);


                            if(currentSpeed < EditorActivity.MIN_SPEED) {
                                OkDialog.newInstance(editorToolsFragment.getActivity(), "Geringe Geschwindigkeit", String.format("Die berechnete Fluggeschwindigkeit um alle Bilder aufzunehmen liegt mit %.2f m/s unter der vorgeschlagenen Mindestgewschindigkeit von %.2f m/s. Diese wird durch die Erhöhung der Rundenzahl ausgeglichen", currentSpeed, EditorActivity.MIN_SPEED), new OkDialog.Listener() {
                                    @Override
                                    public void onOk() {
                                        while(currentSpeed < EditorActivity.MIN_SPEED){
                                            settings.roundCount++;
                                            currentDistance =  settings.roundCount * roundDistance;
                                            currentSpeed = calcSpeed(currentDistance,settings.secondsPerPicture, settings.pictureCount);

                                        }
                                        CameraTrigger cameraItem = (CameraTrigger) MissionItemType.CAMERA_TRIGGER.getNewItem();
                                        double triggerDistance = currentDistance / settings.pictureCount;
                                        cameraItem.setTriggerDistance(triggerDistance);
                                        missionProxy.addMissionItem(cameraItem);
                                        updateSpeedAndRounds();
                                        setWaypoints(settings, item, waypoints);
                                    }

                                    @Override
                                    public void onCancel() {

                                    }

                                    @Override
                                    public void onDismiss() {

                                    }
                                }).show(editorToolsFragment.getFragmentManager(), "MIN_SPEED");
                            }else{
                                CameraTrigger cameraItem = (CameraTrigger) MissionItemType.CAMERA_TRIGGER.getNewItem();
                                double triggerDistance = currentDistance / settings.pictureCount;
                                cameraItem.setTriggerDistance(triggerDistance);
                                missionProxy.addMissionItem(cameraItem);
                                updateSpeedAndRounds();
                                setWaypoints(settings, item, waypoints);
                            }








                        }else{
                            updateSpeedAndRounds();
                            setWaypoints(settings, item, waypoints);
                        }



                    }
                });

                LatLong droneLocation = getDroneLocation();
                if(droneLocation != null){
                    sortWaypoints(droneLocation, points);
                }
                dlg.show(editorToolsFragment.getFragmentManager(), "Höhe");

            }
        }
        editorToolsFragment.setTool(EditorToolsFragment.EditorTools.NONE);
    }

    private void setWaypoints(ScanSettingsDialog.Settings settings, MissionItem item, List<LatLongAlt> waypoints) {
        //Add additional rounds
        int waypointCount = waypoints.size();
        double riseAltitude = 0;
        for(int round = 1; round < settings.roundCount; round++){
            riseAltitude += settings.risePerRound;
            for(int i = 0; i < waypointCount; i++) {
                LatLongAlt wp = new LatLongAlt(waypoints.get(i));
                wp.setAltitude(wp.getAltitude() + riseAltitude);
                waypoints.add(wp);
            }
        }


        missionProxy.addMissionItem(item);
        missionProxy.addWaypointsWithAltitude(waypoints);
        missionProxy.addWaypoint(waypoints.get(0));

        addTakeOffAndRTL(settings.heightFlight);
    }

    private double calcSpeed(double flightdistance, double secondsPerPicturem, int pictureCount){
        return Math.min(EditorActivity.MAX_SPEED, flightdistance / (secondsPerPicture * pictureCount));
    }

    private static double calcDistanceForObjectToFitInFrame(double height, double angleVertical){
        double errorFactor = 1.1;
        double minDistance = 5;
        //double distanceWidth = 1.1 * width * Math.pow(Math.sin(Math.toRadians((180 - angleHorizontal)/2)),2) / Math.sin(Math.toRadians(angleHorizontal));
        //double diagonal = Math.sqrt(length*length + height*height);
        double distance = height / (2*Math.tan(Math.toRadians(angleVertical/2)));

        return Math.max(errorFactor * distance, minDistance);
    }

    @NonNull
    private MissionItem calculateRegionOfInterest(List<LatLong> points, double height) {
        LatLongAlt middle = new LatLongAlt((
                points.get(0).getLatitude()+points.get(2).getLatitude()) / 2 ,
                (points.get(0).getLongitude()+points.get(2).getLongitude()) / 2,
                height
        );
        MissionItem item = MissionItemType.REGION_OF_INTEREST.getNewItem();
        ((MissionItem.SpatialItem) item).setCoordinate(middle);
        return item;
    }

    @NonNull
    private List<LatLongAlt> calculateWaypointsForObject(List<LatLong> points, double flightHeight, double objectHeight, double cameraAngleVertical) {
        int i = 0;
        List<LatLongAlt> waypoints = new ArrayList<LatLongAlt>();


        double distance = calcDistanceForObjectToFitInFrame(objectHeight, cameraAngleVertical);
        //double distances[] = new double[]{
        //        calcDistanceForObjectToFitInFrame(width, objectHeight, cameraAngle),
        //        calcDistanceForObjectToFitInFrame(length, objectHeight, cameraAngle)
       // };
        for(LatLong p : points){
            double heading = MathUtils.getHeadingFromCoordinates(points.get(i), points.get((i + 1) % 4));


            LatLongAlt wp = new LatLongAlt(MathUtils.newCoordFromBearingAndDistance(p, (heading+90)%360, distance),flightHeight);
            wp.set(MathUtils.newCoordFromBearingAndDistance(wp, (heading+180)%360, distance));
            i++;
            waypoints.add(wp);
        }



        return waypoints;
    }

    private void sortWaypoints(LatLong takeoff, List<LatLong> points){
        int closestWaypoint = findClosestWaypoint(takeoff, points);
        for(int i = 0; i < closestWaypoint; i++){
            points.add(points.get(0));
            points.remove(0);
        }
    }

    private void updateSpeedAndRounds(){
        ((EditorActivity) editorToolsFragment.getActivity()).setSpeedAndRounds( currentSpeed, currentRounds);
    }

    private int findClosestWaypoint(LatLong takeoff, List<LatLong> points){
        double min = Double.MAX_VALUE;
        int minIndex = 0;
        int i = 0;
        for(LatLong point : points){
            double dist = MathUtils.getDistance2D(takeoff, point);
            if(dist < min) {
                min = dist;
                minIndex = i;
            }
            i++;
        }
        return minIndex;
    }

    public void addTakeOffAndRTL(double altitude) {
        if (!missionProxy.isFirstItemTakeoff()) {
            Takeoff takeOff = new Takeoff();
            takeOff.setTakeoffAltitude(altitude);
            missionProxy.addMissionItem(0, takeOff);
        }

        if (!missionProxy.isLastItemLandOrRTL()) {
            ReturnToLaunch rtl = new ReturnToLaunch();
            missionProxy.addMissionItem(rtl);
        }
    }

    public LatLong getDroneLocation() {
        Drone dpApi = editorToolsFragment.getDrone();
        if (!dpApi.isConnected())
            return null;

        Gps gps = dpApi.getAttribute(AttributeType.GPS);
        if (!gps.isValid()) {
            return null;
        }

        return gps.getPosition();
    }


}
