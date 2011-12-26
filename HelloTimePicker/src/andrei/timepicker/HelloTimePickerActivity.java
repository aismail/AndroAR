package andrei.timepicker;

import java.util.Calendar;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

public class HelloTimePickerActivity extends Activity {
	private TextView mTimeDisplay;
	private Button mPickTime;
	private int mHour, mMinute;
	
	private TimePickerDialog.OnTimeSetListener mTimeSetCallback =
		new TimePickerDialog.OnTimeSetListener() {
			@Override
			public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
				mHour = hourOfDay;
				mMinute = minute;
				updateDisplay();
			}
		};
	
	static final int TIME_DIALOG_ID = 1;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mTimeDisplay = (TextView) findViewById(R.id.timeDisplay);
        mPickTime = (Button) findViewById(R.id.pickTime);
        
        mPickTime.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(TIME_DIALOG_ID);
			}
		});
        
        final Calendar c = Calendar.getInstance();
        mHour = c.get(Calendar.HOUR_OF_DAY);
        mMinute = c.get(Calendar.MINUTE);
        updateDisplay();
    }
    
    public void updateDisplay() {
    	mTimeDisplay.setText(mHour + ":" + mMinute);
    }
    
    protected Dialog onCreateDialog(int id) {
    	switch(id) {
    	case TIME_DIALOG_ID:
    		return new TimePickerDialog(this, mTimeSetCallback, mHour, mMinute, true);
    	}
    	return null;
    }
}