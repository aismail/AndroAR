package andrei.spinner;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class HelloSpinnerActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource
        	(this, R.array.planets_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(2);
        spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());
    }
    
    public class MyOnItemSelectedListener implements OnItemSelectedListener {
    	@Override
    	public void onItemSelected(AdapterView<?> parent, View view, int pos,
    			long id) {
    		Toast.makeText(HelloSpinnerActivity.this, "The planet is " +
    				parent.getItemAtPosition(pos).toString(), Toast.LENGTH_SHORT).show();
    	}
    	@Override
    	public void onNothingSelected(AdapterView<?> parent) {
    		// TODO Auto-generated method stub
    	}
    }
}