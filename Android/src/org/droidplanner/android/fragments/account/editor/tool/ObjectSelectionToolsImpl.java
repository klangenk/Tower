package org.droidplanner.android.fragments.account.editor.tool;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.mission.MissionItemType;
import com.o3dr.services.android.lib.drone.mission.item.MissionItem;
import com.o3dr.services.android.lib.util.MathUtils;

import org.droidplanner.android.R;
import org.droidplanner.android.dialogs.HeightDialog;
import org.droidplanner.android.fragments.helpers.GestureMapFragment;

import java.util.ArrayList;
import java.util.List;


class ObjectSelectionToolsImpl extends DrawToolsImpl implements AdapterView.OnItemSelectedListener {

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




                HeightDialog dlg = new HeightDialog();
                dlg.setListener(new HeightDialog.Listener() {
                    @Override
                    public void onHeightSet(double heightObject, double heightFlight) {
                        final MissionItem item = calculateRegionOfInterest(points, heightObject / 2);
                        List<LatLongAlt> waypoints = calculateWaypointsForObject(points,heightFlight, heightObject, 90);

                        missionProxy.addMissionItem(item);
                        missionProxy.addWaypointsWithAltitude(waypoints);
                        missionProxy.addWaypoint(waypoints.get(0));
                    }
                });
                dlg.show(editorToolsFragment.getFragmentManager(), "Höhe");
                /*
                LatLong point1 = points.get(0);
                LatLong point2 = points.get(points.size() - 1);
                LatLongAlt middle = new LatLongAlt((point1.getLatitude()+point2.getLatitude()) / 2 ,
                                            (point1.getLongitude()+point2.getLongitude()) / 2,
                                            10);
                MissionItem item = MissionItemType.REGION_OF_INTEREST.getNewItem();
                ((MissionItem.SpatialItem) item).setCoordinate(middle);
                missionProxy.addMissionItem(item);
                missionProxy.addWaypoint(point1);
                missionProxy.addWaypoint(new LatLong(point1.getLatitude(), point2.getLongitude()));
                missionProxy.addWaypoint(point2);
                missionProxy.addWaypoint(new LatLong(point2.getLatitude(), point1.getLongitude()));
                missionProxy.addWaypoint(point1);*/
            }
        }
        editorToolsFragment.setTool(EditorToolsFragment.EditorTools.NONE);
    }

    private void askHeight(String text, HeightCallback cb){

    }

    private interface HeightCallback{
        void onHeight(double height);
    }

    private static double calcDistanceForObjectToFitInFrame(double width, double height, double angle){
        double distanceWidth = 1.1 * width * Math.pow(Math.sin(Math.toRadians((180 - angle)/2)),2) / Math.sin(Math.toRadians(angle));
        double distanceHeight = 1.1 * height * Math.pow(Math.sin(Math.toRadians((180 - angle)/2)),2) / Math.sin(Math.toRadians(angle));
        double minDistance = 10;
        return Math.max(Math.max(distanceHeight, distanceWidth), minDistance);
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
    private List<LatLongAlt> calculateWaypointsForObject(List<LatLong> points, double flightHeight, double objectHeight, double cameraAngle) {
        int i = 0;
        List<LatLongAlt> waypoints = new ArrayList<LatLongAlt>();
        double width = MathUtils.getDistance2D(points.get(0), points.get(1));
        double length = MathUtils.getDistance2D(points.get(1), points.get(2));
        double distances[] = new double[]{
                calcDistanceForObjectToFitInFrame(width, objectHeight, cameraAngle),
                calcDistanceForObjectToFitInFrame(length, objectHeight, cameraAngle)
        };
        for(LatLong p : points){
            double heading = MathUtils.getHeadingFromCoordinates(points.get(i), points.get((i + 1) % 4));


            LatLongAlt wp = new LatLongAlt(MathUtils.newCoordFromBearingAndDistance(p, (heading+90)%360, distances[i%2]),flightHeight);
            wp.set(MathUtils.newCoordFromBearingAndDistance(wp, (heading+180)%360, distances[(i+1)%2]));
            i++;
            waypoints.add(wp);
        }
        return waypoints;
    }


}
