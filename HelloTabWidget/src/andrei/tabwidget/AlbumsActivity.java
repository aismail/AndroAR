package andrei.tabwidget;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class AlbumsActivity extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		TextView tv = new TextView(this);
		tv.setText("This is the Albums tab");
		setContentView(tv);
	}
}