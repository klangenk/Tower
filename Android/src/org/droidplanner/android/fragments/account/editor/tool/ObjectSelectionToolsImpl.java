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
import com.o3dr.services.android.lib.drone.mission.item.command.SetRelay;
import com.o3dr.services.android.lib.drone.mission.item.command.Takeoff;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.util.MathUtils;

import org.droidplanner.android.activities.CustomEditorActivity;
import org.droidplanner.android.activities.EditorActivity;
import org.droidplanner.android.dialogs.OkDialog;
import org.droidplanner.android.dialogs.ScanSettingsDialog;
import org.droidplanner.android.fragments.helpers.GestureMapFragment;
import org.droidplanner.android.utils.Raspberry;
import org.droidplanner.android.utils.Utils;
import org.droidplanner.android.utils.prefs.DroidPlannerPrefs;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kevin Langenkämper
 *
 * This class represents the tool for selection of objects on the map
 */
class ObjectSelectionToolsImpl extends DrawToolsImpl implements AdapterView.OnItemSelectedListener {

    private double currentSpeed = CustomEditorActivity.DEFAULT_SPEED;
    private double currentDistance = 0;

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
        return EditorToolsFragment.EditorTools.OBJECT_SELECT;
    }


    @Override
    public void setup() {
        super.setup();
        EditorToolsFragment.EditorToolListener listener = editorToolsFragment.listener;
        if (listener != null) {
            listener.setGestureMode(GestureMapFragment.GestureMode.RECTANGLE);
        }

    }

    /**
     * This function is called when the finger is lifted after it drew a rectangle
     * @param points list of points that represent the edges of the rectangle
     */
    @Override
    public void onPathFinished(final List<LatLong> points) {
        if (missionProxy != null) {

            //check if all four points are in the list
            if (points.size() == 4) {

                //open dialog with settings for object scan
                ScanSettingsDialog dlg = new ScanSettingsDialog();
                dlg.setListener(new ScanSettingsDialog.Listener() {

                    //this is the callback for the ok button in the dialog
                    @Override
                    public void onSettingsSet(final ScanSettingsDialog.Settings settings) {

                        //region of interest is the middle of the object
                        final MissionItem regionOfInterest = calculateRegionOfInterest(points, settings.heightObject / 2);
                        final List<LatLongAlt> waypoints = calculateWaypointsForObject(points, settings.heightFlight, settings.heightObject, DroidPlannerPrefs.getInstance(editorToolsFragment.getContext()).getCameraAngle());

                        if(settings.shouldTriggerCamera){

                            //calculate the distance of one round
                            final double roundDistance = 2 * MathUtils.getDistance2D(waypoints.get(0), waypoints.get(1)) + 2 *MathUtils.getDistance2D(waypoints.get(1), waypoints.get(2));

                            //calculate the whole distance
                            currentDistance =  settings.roundCount * roundDistance;

                            //calculate the flight speed
                            currentSpeed = calcSpeed(currentDistance,settings.secondsPerPicture, settings.pictureCount);

                            //if the calculated speed is too low, more rounds are added
                            if(currentSpeed < CustomEditorActivity.MIN_SPEED) {
                                OkDialog.newInstance(editorToolsFragment.getActivity(), "Geringe Geschwindigkeit", String.format("Die berechnete Fluggeschwindigkeit um alle Bilder aufzunehmen liegt mit %.2f m/s unter der vorgeschlagenen Mindestgewschindigkeit von %.2f m/s. Diese wird durch die Erhöhung der Rundenzahl ausgeglichen", currentSpeed, CustomEditorActivity.MIN_SPEED), new OkDialog.Listener() {
                                    @Override
                                    public void onOk() {

                                        //add rounds while speed is too low
                                        while(currentSpeed < CustomEditorActivity.MIN_SPEED){
                                            settings.roundCount++;
                                            currentDistance =  settings.roundCount * roundDistance;
                                            currentSpeed = calcSpeed(currentDistance,settings.secondsPerPicture, settings.pictureCount);
                                        }

                                        updateSpeedAndRounds(settings.roundCount);
                                        setWaypoints(settings, regionOfInterest, waypoints, true);

                                    }

                                    @Override
                                    public void onCancel() {

                                    }

                                    @Override
                                    public void onDismiss() {

                                    }
                                }).show(editorToolsFragment.getFragmentManager(), "MIN_SPEED");
                            }else{
                                updateSpeedAndRounds(settings.roundCount);
                                setWaypoints(settings, regionOfInterest, waypoints, true);
                            }

                        }else{
                            updateSpeedAndRounds(settings.roundCount);
                            setWaypoints(settings, regionOfInterest, waypoints, false);
                        }

                    }
                });

                LatLong droneLocation = getDroneLocation();
                if(droneLocation != null){
                    sortWaypoints(droneLocation, points);
                }
                dlg.show(editorToolsFragment.getFragmentManager(), "SCAN_SETTINGS");

            }
        }
        editorToolsFragment.setTool(EditorToolsFragment.EditorTools.NONE);
    }


    /***
     * This function generates waypoints for the rounds and saves the mission items
     * @param settings scan setting that where set in the dialog
     * @param regionOfInterest region of interest (location where camera should look at)
     * @param waypoints list of waypoints
     * @param addCameraTrigger true, if camera should be triggered camera trigger
     */
    private void setWaypoints(ScanSettingsDialog.Settings settings, MissionItem regionOfInterest, List<LatLongAlt> waypoints, boolean addCameraTrigger ) {

        Raspberry.CaptureType captureType =  Raspberry.CaptureType.values()[DroidPlannerPrefs.getInstance(editorToolsFragment.getContext()).getCaptureType()];

        if(addCameraTrigger){
            if(captureType == Raspberry.CaptureType.SingleShot) {
                //if capture type is single shot set camera trigger item with calculated distance
                CameraTrigger cameraItem = (CameraTrigger) MissionItemType.CAMERA_TRIGGER.getNewItem();
                double triggerDistance =  currentDistance / settings.pictureCount;
                cameraItem.setTriggerDistance(triggerDistance);
                missionProxy.addMissionItem(cameraItem);
            }else{
                /*SetRelay relayItem = (SetRelay) MissionItemType.SET_RELAY.getNewItem();
                relayItem.setEnabled(true);
                relayItem.setRelayNumber(0);
                missionProxy.addMissionItem(relayItem);*/

                CameraTrigger cameraItem = (CameraTrigger) MissionItemType.CAMERA_TRIGGER.getNewItem();
                double triggerDistance =  999;
                cameraItem.setTriggerDistance(triggerDistance);
                missionProxy.addMissionItem(cameraItem);

                /*relayItem = (SetRelay) MissionItemType.SET_RELAY.getNewItem();
                relayItem.setEnabled(false);
                relayItem.setRelayNumber(0);
                missionProxy.addMissionItem(relayItem);*/
            }
        }


        int waypointCount = waypoints.size();
        double riseAltitude = 0;
        for(int round = 1; round < settings.roundCount; round++){
            riseAltitude += settings.risePerRound;

            //Add the first waypoint again, that the whole round is made on the same height
            waypoints.add(new LatLongAlt(waypoints.get(0)));

            for(int i = 0; i < waypointCount; i++) {
                LatLongAlt wp = new LatLongAlt(waypoints.get(i));
                wp.setAltitude(wp.getAltitude() + riseAltitude);
                waypoints.add(wp);
            }
        }


        missionProxy.addMissionItem(regionOfInterest);
        missionProxy.addWaypointsWithAltitude(waypoints);
        missionProxy.addWaypoint(waypoints.get(0));

        //Add Cameratrigger Item with distance 0 to stop triggering
        if(addCameraTrigger){
            if(captureType == Raspberry.CaptureType.SingleShot) {
                CameraTrigger cameraItem = (CameraTrigger) MissionItemType.CAMERA_TRIGGER.getNewItem();
                cameraItem.setTriggerDistance(0);
                missionProxy.addMissionItem(cameraItem);
            }else{
                /*SetRelay relayItem = (SetRelay) MissionItemType.SET_RELAY.getNewItem();
                relayItem.setEnabled(false);
                relayItem.setRelayNumber(0);
                missionProxy.addMissionItem(3, relayItem);

                relayItem = (SetRelay) MissionItemType.SET_RELAY.getNewItem();
                relayItem.setEnabled(true);
                relayItem.setRelayNumber(0);
                missionProxy.addMissionItem(relayItem);

                relayItem = (SetRelay) MissionItemType.SET_RELAY.getNewItem();
                relayItem.setEnabled(false);
                relayItem.setRelayNumber(0);
                missionProxy.addMissionItem(relayItem);*/
            }
        }

        addTakeOffAndRTL(settings.heightFlight);
    }


    /***
     * This function calculates the flight speed, that the camera can make
     * the desired amount of pictures over the distance
     * @param flightdistance length of the flight
     * @param secondsPerPicture time the camera needs between two images
     * @param pictureCount desired amount of pictures to take
     * @return
     */
    private double calcSpeed(double flightdistance, double secondsPerPicture, int pictureCount){
        return Math.min(CustomEditorActivity.MAX_SPEED, flightdistance / (secondsPerPicture * pictureCount));
    }

    /***
     * This functions calcs the distance between the camera and the object int meters, that the object fits
     * in the picture on the vertical axis
     * @param height height of the object
     * @param angleVertical vertical angle of the camera
     * @return distance in meters
     */
    private static double calcDistanceForObjectToFitInFrame(double height, double angleVertical){
        double errorFactor = 1.1;
        double minDistance = 5;
        double distance = height / (2*Math.tan(Math.toRadians(angleVertical/2)));
        return Math.max(errorFactor * distance, minDistance);
    }

    /**
     * This function calculates the middle of the object
     * @param points the points of the rectangle
     * @param height the height of the object
     * @return mission item for region of interest
     */
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

    /***
     * This funtion calculates the waypoints to fly around the object
     * @param points points of the rectangle
     * @param flightHeight height of the flight
     * @param objectHeight height of the object
     * @param cameraAngleVertical vertical angle of the camera
     * @return waypoints for flying
     */
    @NonNull
    private List<LatLongAlt> calculateWaypointsForObject(List<LatLong> points, double flightHeight, double objectHeight, double cameraAngleVertical) {
        int i = 0;
        List<LatLongAlt> waypoints = new ArrayList<LatLongAlt>();


        double distance = calcDistanceForObjectToFitInFrame(objectHeight, cameraAngleVertical);

        for(LatLong p : points){
            double heading = MathUtils.getHeadingFromCoordinates(points.get(i), points.get((i + 1) % 4));
            LatLongAlt wp = new LatLongAlt(MathUtils.newCoordFromBearingAndDistance(p, (heading+90)%360, distance),flightHeight);
            wp.set(MathUtils.newCoordFromBearingAndDistance(wp, (heading+180)%360, distance));
            i++;
            waypoints.add(wp);
        }

        return waypoints;
    }

    /***
     * This functions shifts the waypoints until the closest is first
     * @param takeoff coordinates of takeoff
     * @param points waypoints to sort
     */
    private void sortWaypoints(LatLong takeoff, List<LatLong> points){
        int closestWaypoint = findClosestWaypoint(takeoff, points);
        for(int i = 0; i < closestWaypoint; i++){
            points.add(points.get(0));
            points.remove(0);
        }
    }

    /**
     * This function updates speed and rounds in the UI
     */
    private void updateSpeedAndRounds(int rounds){
        ((CustomEditorActivity) editorToolsFragment.getActivity()).setSpeedAndRounds( currentSpeed, rounds);
    }

    /**
     * This function finds the closest waypoint from the takeoff
     * @param takeoff coordinates of takeoff
     * @param points waypoints to search in
     * @return index of closest waypoint
     */
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

    /**
     * This function adds takeoff and return to land mission items
     * @param altitude altitude for takeoff
     */
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

    /**
     * This function gets the location of the drone
     * @return coordinates of location
     */
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
