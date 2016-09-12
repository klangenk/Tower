package org.droidplanner.android.fragments.helpers;

import java.util.ArrayList;
import java.util.List;

import org.droidplanner.android.R;
import org.droidplanner.android.fragments.EditorMapFragment;
import org.droidplanner.android.fragments.account.editor.tool.CustomGestureOverlayView;

import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGestureListener;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.util.MathUtils;

public class GestureMapFragment extends Fragment implements OnGestureListener {
	private static final int TOLERANCE = 15;
	private static final int STROKE_WIDTH = 0;
	private PointF gestureStart;
	private PointF gestureEnd;


	private double toleranceInPixels;

	public interface OnPathFinishedListener {

		void onPathFinished(List<LatLong> path);
	}

	private CustomGestureOverlayView overlay;
	private OnPathFinishedListener listener;
    private EditorMapFragment mapFragment;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_gesture_map, container, false);
	}

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
        final FragmentManager fm = getChildFragmentManager();
        mapFragment = ((EditorMapFragment) fm.findFragmentById(R.id.gesture_map_fragment));
        if(mapFragment == null){
            mapFragment = new EditorMapFragment();
            fm.beginTransaction().add(R.id.gesture_map_fragment, mapFragment).commit();
        }

        overlay = (CustomGestureOverlayView) view.findViewById(R.id.overlay1);
        overlay.addOnGestureListener(this);
        overlay.setEnabled(false);

        overlay.setGestureStrokeWidth(scaleDpToPixels(STROKE_WIDTH));
        toleranceInPixels = scaleDpToPixels(TOLERANCE);
    }

	private int scaleDpToPixels(double value) {
		final float scale = getResources().getDisplayMetrics().density;
		return (int) Math.round(value * scale);
	}

    public EditorMapFragment getMapFragment(){
        return mapFragment;
    }

	public void enableGestureDetection() {
		overlay.setEnabled(true);
	}

	public void disableGestureDetection() {
		overlay.setEnabled(false);
	}

	public void setOnPathFinishedListener(OnPathFinishedListener listener) {
		this.listener = listener;
	}

	@Override
	public void onGestureEnded(GestureOverlayView arg0, MotionEvent arg1) {
		overlay.setEnabled(false);
		List<LatLong> path = decodeGesture();
		if(currentMode == GestureMode.FREE) {
			if (path.size() > 1) {
				path = MathUtils.simplify(path, toleranceInPixels);
			}

			//if mode is rectangle
			//get the first and last point of the path
			//and calculate the other two points of the rectangle
		}else if(currentMode == GestureMode.RECTANGLE){
			List<LatLong> rectPath = new ArrayList<LatLong>();
			LatLong point1 = path.get(0);
			LatLong point2 = path.get(path.size() - 1);

			LatLong min = new LatLong(
					Math.min(point1.getLatitude(), point2.getLatitude()),
					Math.min(point1.getLongitude(), point2.getLongitude())
			);

			LatLong max = new LatLong(
					Math.max(point1.getLatitude(), point2.getLatitude()),
					Math.max(point1.getLongitude(), point2.getLongitude())
			);

			rectPath.add(min);
			rectPath.add(new LatLong(min.getLatitude(), max.getLongitude()));
			rectPath.add(max);
			rectPath.add(new LatLong(max.getLatitude(), min.getLongitude()));
			path = rectPath;
		}

		//Canvas canvas = new Canvas();

		//overlay.draw(canvas);

		listener.onPathFinished(path);
		overlay.removeRect();
	}


	public enum GestureMode{
		FREE,
		RECTANGLE
	}

	private GestureMode currentMode = GestureMode.FREE;

	public GestureMode getMode(){
		return currentMode;
	}

	public void setGestureMode(GestureMode mode){
		this.currentMode = mode;
	}

	private List<LatLong> decodeGesture() {
		List<LatLong> path = new ArrayList<LatLong>();
		extractPathFromGesture(path);
		return path;
	}

	private void extractPathFromGesture(List<LatLong> path) {
		float[] points = overlay.getGesture().getStrokes().get(0).points;
		for (int i = 0; i < points.length; i += 2) {
			path.add(new LatLong((int) points[i], (int) points[i + 1]));
		}
	}

	@Override
	public void onGesture(GestureOverlayView arg0, MotionEvent arg1) {
		Log.v("Pos", String.format("%.4f %.4f", arg1.getX(), arg1.getY()));


		if(currentMode == GestureMode.RECTANGLE){
            //draw rectangle
			gestureEnd.set(arg1.getX(), arg1.getY());
			overlay.drawRect(gestureStart, gestureEnd);
		}
	}

	@Override
	public void onGestureCancelled(GestureOverlayView arg0, MotionEvent arg1) {
	}

	@Override
	public void onGestureStarted(GestureOverlayView arg0, MotionEvent arg1) {
		gestureStart = new PointF(arg1.getX(), arg1.getY());
		gestureEnd = new PointF(arg1.getX(), arg1.getY());
	}


}
