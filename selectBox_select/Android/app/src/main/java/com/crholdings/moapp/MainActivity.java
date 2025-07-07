public class MainActivity extends BaseActivity {
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void InitView() {
	
		// name spinner 초기화
		ArrayAdapter nameProtocol = ArrayAdapter.createFromResource(MainActivity.this, R.array.select_name, android.R.layout.simple_spinner_item);
		nameProtocol.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		final Spinner nameSpinner = (Spinner) findViewById(R.id.reg_push_select);
		nameSpinner.setAdapter(nameProtocol);

		int text_position = sharedPreferences.getInt("text_position", 0);
		nameSpinner.setSelection(text_position);
		

		// spinner 초기화
		ArrayAdapter urlProtocol = ArrayAdapter.createFromResource(MainActivity.this, R.array.select_url, android.R.layout.simple_spinner_item);
		
		// EditText 초기화
		final int[] ur_position = new int[1];
		final EditText serverurl = (EditText) findViewById(R.id.reg_push_url);
		nameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				serverurl.setText(urlProtocol.getItem(position).toString() + "/test");
				ur_position[0] = position;
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				MOUtils.WriteLog(TAG, "onNothingSelected");
			}
		});
		
		// 버튼 초기화
		Button button = (Button) findViewById(R.id.reg_btn);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				final String strProtocol = arrProtocol.getItem(1).toString();
				final String strUrl = serverurl.getText().toString();

				//입력한 gw URL을 저장
				sharedPreferences.edit().putString("test_URL", strProtocol + strUrl).commit();
			}
		});

    }

}
