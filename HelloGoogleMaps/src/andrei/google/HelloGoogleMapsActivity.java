package andrei.google;

import java.util.List;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class HelloGoogleMapsActivity extends MapActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        final MapView mapView = (MapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);

        // add overlays on map (with ItemizedOverlay that has more overlays in it)
        List<Overlay> mapOverlays = mapView.getOverlays();
        Drawable drawable = this.getResources().getDrawable(R.drawable.androidmarker);
        HelloItemizedOverlay itemizedoverlay = new HelloItemizedOverlay(drawable, this);
        
        // create one point
        GeoPoint point = new GeoPoint(19240000, -99120000);
        OverlayItem overlayitem = new OverlayItem(point, "Hola, Mundo!", "I'm in Mexico City!");
        itemizedoverlay.addOverlay(overlayitem);
        
        // create another point
        GeoPoint point2 = new GeoPoint(35410000, 139460000);
        OverlayItem overlayitem2 = new OverlayItem(point2, "Sekai, konichiwa!", "I'm in Japan!");
        itemizedoverlay.addOverlay(overlayitem2);
        
        // home sweet home
        GeoPoint point3 = new GeoPoint(45653795, 24716550);
        OverlayItem overlayitem3 = new OverlayItem(point3, "Salut!", "Sunt în România!");
        itemizedoverlay.addOverlay(overlayitem3);
        
        mapOverlays.add(itemizedoverlay);
        
        // TODO: show coordinates of clicked point
//        mapView.setOnTouchListener(new OnTouchListener() {
//			
//			@Override
//			public boolean onTouch(View v, MotionEvent event) {
//				GeoPoint g = mapView.getMapCenter();
//				AlertDialog.Builder dialog = new AlertDialog.Builder(HelloGoogleMapsActivity.this);
//				dialog.setTitle("Clicked GeoPoint");
//				dialog.setMessage("(" + g.getLatitudeE6() + ", " + g.getLongitudeE6() + ")");
//				dialog.show();
//				return true;
//			}
//		});
    }

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}