package cn.wch.ch34xuartdriver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;
import cn.wch.ch34xuartdemo.R;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.util.Log;

import static cn.wch.ch34xuartdemo.R.array.AT_command;

public class MainActivity extends Activity {

	public static final String TAG = "cn.wch.wchusbdriver";
	private static final String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";

	public readThread handlerThread;
	protected final Object ThreadLock = new Object();
	private EditText readText;
	private EditText writeText;
	private Spinner baudSpinner;
	private Spinner stopSpinner;
	private Spinner dataSpinner;
	private Spinner paritySpinner;
	private Spinner flowSpinner;
	private boolean isOpen;
	private Handler handler;
	private int retval;
	private MainActivity activity;

	private Button writeButton, configButton, openButton, clearButton, readButton;

	private Button connectButton_1, connectButton_2, connectButton_3, connectButton_4;

	private Button connectButton_5, connectButton_6, connectButton_7, connectButton_8;

	private Button connectButton_9, stopButton;

	public byte[] writeBuffer;
	public byte[] readBuffer;
	public int actualNumBytes;

	public int numBytes;
	public byte count;
	public int status;
	public byte writeIndex = 0;
	public byte readIndex = 0;

	public int baudRate;
	public byte baudRate_byte;
	public byte stopBit;
	public byte dataBit;
	public byte parity;
	public byte flowControl;

	public boolean isConfiged = false;
	public boolean READ_ENABLE = false;
	public SharedPreferences sharePrefSettings;
	public String act_string;

	public int i = 0;
//	Resources res = this.getResources();

	private Timer timer;
	private TimerTask timerTask;


	/**
	 * ��ȡ��ӦȨ��
	 **/

	private static final int ACTION_REQUEST_PERMISSIONS = 0x001;

	private static String[] NEEDED_PERMISSIONS = new String[]{
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.READ_EXTERNAL_STORAGE
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
//		ActivityCompat.requestPermissions(MainActivity.this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
		MyApp.driver = new CH34xUARTDriver(
				(UsbManager) getSystemService(Context.USB_SERVICE), this,
				ACTION_USB_PERMISSION);
		initUI();

		final Resources res = this.getResources(); //������onCreate��,��ȡ����

		if (!MyApp.driver.UsbFeatureSupported())// �ж�ϵͳ�Ƿ�֧��USB HOST
		{
			Dialog dialog = new AlertDialog.Builder(MainActivity.this)
					.setTitle("��ʾ")
					.setMessage("�����ֻ���֧��USB HOST������������ֻ����ԣ�")
					.setPositiveButton("ȷ��",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface arg0,
										int arg1) {
									System.exit(0);
								}
							}).create();
			dialog.setCanceledOnTouchOutside(false);
			dialog.show();
		}
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);// ���ֳ�������Ļ��״̬
		writeBuffer = new byte[512];
		readBuffer = new byte[512];
		isOpen = false;
		configButton.setEnabled(false);
		writeButton.setEnabled(false);
		readButton.setEnabled(false);
		connectButton_1.setEnabled(false);
		connectButton_2.setEnabled(false);
		connectButton_3.setEnabled(false);
		connectButton_4.setEnabled(false);
		connectButton_5.setEnabled(false);
		connectButton_6.setEnabled(false);
		connectButton_7.setEnabled(false);
		connectButton_8.setEnabled(false);
		connectButton_9.setEnabled(false);
		stopButton.setEnabled(false);
		activity = this;
		
		//��������Ҫ����ΪResumeUsbList��UartInit
		openButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (!isOpen) {
					int retval = MyApp.driver.ResumeUsbPermission();
					if (retval == 0) {
						//Resume usb device list
						retval = MyApp.driver.ResumeUsbList();
						if (retval == -1)// ResumeUsbList��������ö��CH34X�豸�Լ�������豸
						{
							Toast.makeText(MainActivity.this, "Open failed!",
									Toast.LENGTH_SHORT).show();
							MyApp.driver.CloseDevice();
						} else if (retval == 0){
							if (MyApp.driver.mDeviceConnection != null) {
								if (!MyApp.driver.UartInit()) {//�Դ����豸���г�ʼ������
									Toast.makeText(MainActivity.this, "Initialization failed!",
											Toast.LENGTH_SHORT).show();
									return;
								}
								Toast.makeText(MainActivity.this, "Device opened",
										Toast.LENGTH_SHORT).show();
								MyApp.driver.SetConfig(baudRate, dataBit, stopBit, parity,//���ô��ڲ����ʣ�����˵���ɲ��ձ���ֲ�
										flowControl);
								isOpen = true;
								openButton.setText("Close");
								configButton.setEnabled(true);
								writeButton.setEnabled(true);
								readButton.setEnabled(true);
								connectButton_1.setEnabled(true);
								connectButton_2.setEnabled(true);
								connectButton_3.setEnabled(true);
								connectButton_4.setEnabled(true);
								connectButton_5.setEnabled(true);
								connectButton_6.setEnabled(true);
								connectButton_7.setEnabled(true);
								connectButton_8.setEnabled(true);
								connectButton_9.setEnabled(true);
								stopButton.setEnabled(true);
								new readThread().start();//�������̶߳�ȡ���ڽ��յ�����
							} else {	
								Toast.makeText(MainActivity.this, "Open failed!",
										Toast.LENGTH_SHORT).show();		
							}
						} else {
							
							AlertDialog.Builder builder = new AlertDialog.Builder(activity);
							builder.setIcon(R.drawable.icon);
							builder.setTitle("δ��Ȩ��");
							builder.setMessage("ȷ���˳���");
							builder.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									System.exit(0);
								}
							});
							builder.setNegativeButton("����", new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub
									
								}
							});
							builder.show();
							
						}
					}
				} else {
					openButton.setText("Open");
					configButton.setEnabled(false);
					writeButton.setEnabled(false);
					readButton.setEnabled(false);
					connectButton_1.setEnabled(false);
					connectButton_2.setEnabled(false);
					connectButton_3.setEnabled(false);
					connectButton_4.setEnabled(false);
					connectButton_5.setEnabled(false);
					connectButton_6.setEnabled(false);
					connectButton_7.setEnabled(false);
					connectButton_8.setEnabled(false);
					connectButton_9.setEnabled(false);
					stopButton.setEnabled(false);
					isOpen = false;
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					MyApp.driver.CloseDevice();
				}
			}
		});

		configButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {

				if (MyApp.driver.SetConfig(baudRate, dataBit, stopBit, parity,//���ô��ڲ����ʣ�����˵���ɲ��ձ���ֲ�
						flowControl)) {					
					Toast.makeText(MainActivity.this, "Config successfully",
							Toast.LENGTH_SHORT).show();					
				} else {
					Toast.makeText(MainActivity.this, "Config failed!",
							Toast.LENGTH_SHORT).show();
				}
			}
		});
		writeButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {

				String ts_1 = convertStringToHex( writeText.getText().toString() );

				convert_send( ts_1 );

			}
		});

		readButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {

//				try {
//					ReadDataFromStorage("/sdcard/TH_DATA/","DATA.txt",1 );
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
				registerReceiver(loginreceiver, new IntentFilter("HTX.test"));  // ע��㲥

			}
		});

		stopButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {

//				registerReceiver(loginreceiver, new IntentFilter("HTX.test"));  // ע��㲥
//				unregisterReceiver( loginreceiver );//��ֹ�㲥
				//1.�½�һ��intent��Ȼ������Action
				//2.sendBroadcast(intent)
				Intent intent = new Intent();
				intent.setAction("Stop_HTX_R.test");            //��Ӧ�㲥������ע��Ķ���
//				intent.putExtra("data_r","stop");     //���Դ��ݲ������Ǳ���
				sendBroadcast(intent);

				Log.i( "stop","11111111111111111111111111");

			}
		});

		connectButton_1.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {

//				Handler handler = new Handler();
//
//				Runnable runnable_1=new Runnable() {
//					@Override
//					public void run() {
//						// TODO Auto-generated method stub
//						//Ҫ��������
////						handler.postDelayed(this, 2000);
						String[] str_1 = res.getStringArray( R.array.AT_command );
						send_command( str_1 [0] );
//					}
//				};
//
//				handler.postDelayed(runnable_1, 0);//����ִ��.


//				send_command( str_1 [0] ); //AT+NRB

//				send_command( str_1 [1] ); //AT+NCDP=49.4.85.232,5683

//				send_command( str_1 [2] ); //AT+CMEE=1
//
//				send_command( str_1 [3] ); //AT+NSMI=1
//
//				send_command( str_1 [4] ); //AT+NNMI=1
//
//				send_command( str_1 [5] ); //AT+NUESTATS
//
//				send_command( str_1 [6] ); //AT+CGATT?
//
//				send_command( str_1 [7] ); //AT+NMSTATUS?
//
//				send_command( str_1 [8] ); //AT+NMGS=5,2020373839


			}
		});

		connectButton_2.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {

				String[] str_1 = res.getStringArray( R.array.AT_command );

				send_command( str_1 [1] ); //AT+NCDP=49.4.85.232,5683

			}
		});

		connectButton_3.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {

				String[] str_1 = res.getStringArray( R.array.AT_command );

				send_command( str_1 [2] ); //AT+CMEE=1

			}
		});

		connectButton_4.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {

				String[] str_1 = res.getStringArray( R.array.AT_command );

				send_command( str_1 [3] ); //AT+NSMI=1

			}
		});

		connectButton_5.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {

				String[] str_1 = res.getStringArray( R.array.AT_command );

				send_command( str_1 [4] ); //

			}
		});

		connectButton_6.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {

				String[] str_1 = res.getStringArray( R.array.AT_command );

				send_command( str_1 [5] ); //

			}
		});

		connectButton_7.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {

				String[] str_1 = res.getStringArray( R.array.AT_command );

				send_command( str_1 [6] ); //

			}
		});

		connectButton_8.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {

				String[] str_1 = res.getStringArray( R.array.AT_command );

				send_command( str_1 [7] ); //

			}
		});

		connectButton_9.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {

				String[] str_1 = res.getStringArray( R.array.AT_command );

				send_command( str_1 [8] ); //

			}
		});


		handler = new Handler() {
			public void handleMessage(Message msg) {
				readText.append((String) msg.obj);
			}
		};

	}

	private BroadcastReceiver loginreceiver = new BroadcastReceiver(){// ����THSensor�������Ĺ㲥
		@Override
		public void onReceive(Context context, Intent intent) { //�������涨��Խ��յ������ݽ��д���
//			Toast.makeText(context, "The data is:"+intent.getStringExtra("data"), Toast.LENGTH_SHORT).show();// THSensor���͹���������
			String str = intent.getStringExtra("data");

			int first_zero = Firstzero( str );

			String str_1 = convstr( str, 0, first_zero );

			String str_2 = convstr( str, first_zero + 1, str.length() - 1 );

			Toast.makeText(context, "The data is:"+str_1+getText(R.string.temperature_unit)+","+str_2+getText(R.string.humidity_unit), Toast.LENGTH_SHORT).show();

			String str_f = "";

			if ( str != null )
			{
				if ( str.length() == 8 )
				{
					for ( int i = 0; i < str.length(); i++ )
					{
						if ( str.charAt(i) >= 48 && str.charAt(i) <= 57 ) //����ַ�Ϊ0-9
						{
							String variable = "3" + str.charAt(i);
							str_f += variable;
						}else
						{
							str_f += "2E";
						}
					}
				}else
				{
					if ( str_1.length() < 4 )
					{
						str_f += "20";
					}
					for ( int u = 0; u < str_1.length(); u++ )
					{
						if ( str.charAt(u) >= 48 && str.charAt(u) <= 57 ) //����ַ�Ϊ0-9
						{
							String variable = "3" + str.charAt(u);
							str_f += variable;
						}else
						{
							str_f += "2E";
						}
					}

					if ( str_2.length() < 4 )
					{
						str_f += "20";
					}
					for ( int v = 0; v < str_2.length(); v++ )
					{
						if ( str.charAt(v) >= 48 && str.charAt(v) <= 57 ) //����ַ�Ϊ0-9
						{
							String variable = "3" + str.charAt(v);
							str_f += variable;
						}else
						{
							str_f += "2E";
						}
					}
				}
				str_f = "AT+NMGS=" + str.length() + "," + str_f;
			}

			convert_send( convertStringToHex( str_f ) );

		}
	};


	@Override
	public void onResume() {
		super.onResume();
	}
	
	
	@Override
	public void onDestroy() {
		isOpen = false;
		unregisterReceiver( loginreceiver );
		MyApp.driver.CloseDevice();
		super.onDestroy();

	}

	//�������
	private void initUI() {
		readText = (EditText) findViewById(R.id.ReadValues);
		writeText = (EditText) findViewById(R.id.WriteValues);
		configButton = (Button) findViewById(R.id.configButton);
		writeButton = (Button) findViewById(R.id.WriteButton);
		readButton = (Button) findViewById(R.id.ReadButton);
		connectButton_1 = (Button) findViewById(R.id.ConnectButton_1);
		connectButton_2 = (Button) findViewById(R.id.ConnectButton_2);
		connectButton_3 = (Button) findViewById(R.id.ConnectButton_3);
		connectButton_4 = (Button) findViewById(R.id.ConnectButton_4);
		connectButton_5 = (Button) findViewById(R.id.ConnectButton_5);
		connectButton_6 = (Button) findViewById(R.id.ConnectButton_6);
		connectButton_7 = (Button) findViewById(R.id.ConnectButton_7);
		connectButton_8 = (Button) findViewById(R.id.ConnectButton_8);
		connectButton_9 = (Button) findViewById(R.id.ConnectButton_9);
		stopButton = (Button) findViewById(R.id.StopButton);
		openButton = (Button) findViewById(R.id.open_device);
		clearButton = (Button) findViewById(R.id.clearButton);

		baudSpinner = (Spinner) findViewById(R.id.baudRateValue);
		ArrayAdapter<CharSequence> baudAdapter = ArrayAdapter
				.createFromResource(this, R.array.baud_rate,
						R.layout.my_spinner_textview);
		baudAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
		baudSpinner.setAdapter(baudAdapter);
		baudSpinner.setGravity(0x10);
		baudSpinner.setSelection(5);
		/* by default it is 9600 */
		baudRate = 9600;

		/* stop bits */
		stopSpinner = (Spinner) findViewById(R.id.stopBitValue);
		ArrayAdapter<CharSequence> stopAdapter = ArrayAdapter
				.createFromResource(this, R.array.stop_bits,
						R.layout.my_spinner_textview);
		stopAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
		stopSpinner.setAdapter(stopAdapter);
		stopSpinner.setGravity(0x01);
		/* default is stop bit 1 */
		stopBit = 1;

		/* data bits */
		dataSpinner = (Spinner) findViewById(R.id.dataBitValue);
		ArrayAdapter<CharSequence> dataAdapter = ArrayAdapter
				.createFromResource(this, R.array.data_bits,
						R.layout.my_spinner_textview);
		dataAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
		dataSpinner.setAdapter(dataAdapter);
		dataSpinner.setGravity(0x11);
		dataSpinner.setSelection(3);
		/* default data bit is 8 bit */
		dataBit = 8;

		/* parity */
		paritySpinner = (Spinner) findViewById(R.id.parityValue);
		ArrayAdapter<CharSequence> parityAdapter = ArrayAdapter
				.createFromResource(this, R.array.parity,
						R.layout.my_spinner_textview);
		parityAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
		paritySpinner.setAdapter(parityAdapter);
		paritySpinner.setGravity(0x11);
		/* default is none */
		parity = 0;

		/* flow control */
		flowSpinner = (Spinner) findViewById(R.id.flowControlValue);
		ArrayAdapter<CharSequence> flowAdapter = ArrayAdapter
				.createFromResource(this, R.array.flow_control,
						R.layout.my_spinner_textview);
		flowAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
		flowSpinner.setAdapter(flowAdapter);
		flowSpinner.setGravity(0x11);
		/* default flow control is is none */
		flowControl = 0;

		/* set the adapter listeners for baud */
		baudSpinner.setOnItemSelectedListener(new MyOnBaudSelectedListener());
		/* set the adapter listeners for stop bits */
		stopSpinner.setOnItemSelectedListener(new MyOnStopSelectedListener());
		/* set the adapter listeners for data bits */
		dataSpinner.setOnItemSelectedListener(new MyOnDataSelectedListener());
		/* set the adapter listeners for parity */
		paritySpinner
				.setOnItemSelectedListener(new MyOnParitySelectedListener());
		/* set the adapter listeners for flow control */
		flowSpinner.setOnItemSelectedListener(new MyOnFlowSelectedListener());

		clearButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				readText.setText("");
			}
		});
		return;
	}

	public class MyOnBaudSelectedListener implements OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view,
				int position, long id) {
			baudRate = Integer.parseInt(parent.getItemAtPosition(position)
					.toString());
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}
	}

	public class MyOnStopSelectedListener implements OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view,
				int position, long id) {
			stopBit = (byte) Integer.parseInt(parent
					.getItemAtPosition(position).toString());
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}

	}

	public class MyOnDataSelectedListener implements OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view,
				int position, long id) {
			dataBit = (byte) Integer.parseInt(parent
					.getItemAtPosition(position).toString());
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}

	}

	public class MyOnParitySelectedListener implements OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view,
				int position, long id) {
			String parityString = new String(parent.getItemAtPosition(position)
					.toString());
			if (parityString.compareTo("None") == 0) {
				parity = 0;
			}

			if (parityString.compareTo("Odd") == 0) {
				parity = 1;
			}

			if (parityString.compareTo("Even") == 0) {
				parity = 2;
			}

			if (parityString.compareTo("Mark") == 0) {
				parity = 3;
			}

			if (parityString.compareTo("Space") == 0) {
				parity = 4;
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}

	}

	public class MyOnFlowSelectedListener implements OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view,
				int position, long id) {
			String flowString = new String(parent.getItemAtPosition(position)
					.toString());
			if (flowString.compareTo("None") == 0) {
				flowControl = 0;
			}

			if (flowString.compareTo("CTS/RTS") == 0) {
				flowControl = 1;
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}

	}

	private class readThread extends Thread {


		public void run() {

			byte[] buffer = new byte[4096];

			while (true) {

				Message msg = Message.obtain();
				if (!isOpen) {
					break;
				}
				int length = MyApp.driver.ReadData(buffer, 4096);
				if (length > 0) {
//					String recv_1 = toHexString(buffer, length);		//��16�������
//					String recv = toutf8( recv_1 ); //��utf-8��ʽ���
					String recv_2 = new String(buffer, 0, length);		//���ַ�����ʽ���
					String recv = toutf8_2( recv_2 ); //��utf-8��ʽ���

					msg.obj = recv;
					handler.sendMessage(msg);
				}
			}
		}
	}

	/**
	 * ��byte[]����ת��ΪString����
	 * @param arg
	 *            ��Ҫת����byte[]����
	 * @param length
	 *            ��Ҫת�������鳤��
	 * @return ת�����String����
	 */
	private String toHexString(byte[] arg, int length) {
		String result = new String();
		if (arg != null) {
			for (int i = 0; i < length; i++) {
				result = result
						+ (Integer.toHexString(
								arg[i] < 0 ? arg[i] + 256 : arg[i]).length() == 1 ? "0"
								+ Integer.toHexString(arg[i] < 0 ? arg[i] + 256
										: arg[i])
								: Integer.toHexString(arg[i] < 0 ? arg[i] + 256
										: arg[i])) + " ";
			}
			return result;
		}
		return "";
	}

	/**
	 * ��16���Ƶ�Stringת��Ϊbyte[]����
	 * @param arg
	 *            ��Ҫת����String����
	 * @return ת�����byte[]����
	 */
	private byte[] toByteArray(String arg) {
		if (arg != null) {
			/* 1.��ȥ��String�е�' '��Ȼ��Stringת��Ϊchar���� */
			char[] NewArray = new char[1000];
			char[] array = arg.toCharArray();
			int length = 0;
			for (int i = 0; i < array.length; i++) {
				if (array[i] != ' ') {
					NewArray[length] = array[i];
					length++;
				}
			}
			/* ��char�����е�ֵת��һ��ʵ�ʵ�ʮ�������� */
			int EvenLength = (length % 2 == 0) ? length : length + 1;
			if (EvenLength != 0) {
				int[] data = new int[EvenLength];
				data[EvenLength - 1] = 0;
				for (int i = 0; i < length; i++) {
					if (NewArray[i] >= '0' && NewArray[i] <= '9') {
						data[i] = NewArray[i] - '0';
					} else if (NewArray[i] >= 'a' && NewArray[i] <= 'f') {
						data[i] = NewArray[i] - 'a' + 10;
					} else if (NewArray[i] >= 'A' && NewArray[i] <= 'F') {
						data[i] = NewArray[i] - 'A' + 10;
					}
				}
				/* �� ÿ��char��ֵÿ�������һ��16�������� */
				byte[] byteArray = new byte[EvenLength / 2];
				for (int i = 0; i < EvenLength / 2; i++) {
					byteArray[i] = (byte) (data[i * 2] * 16 + data[i * 2 + 1]);
				}
				return byteArray;
			}
		}
		return new byte[] {};
	}


	/**
	 * ��Stringת��Ϊbyte[]����
	 * @param arg
	 *            ��Ҫת����String����
	 * @return ת�����byte[]����
	 */
	private byte[] toByteArray2(String arg) {
		if (arg != null) {
			/* 1.��ȥ��String�е�' '��Ȼ��Stringת��Ϊchar���� */
			char[] NewArray = new char[1000];
			char[] array = arg.toCharArray();
			int length = 0;
			for (int i = 0; i < array.length; i++) {
				if (array[i] != ' ') {
					NewArray[length] = array[i];
					length++;
				}
			}

			byte[] byteArray = new byte[length];
			for (int i = 0; i < length; i++) {
				byteArray[i] = (byte)NewArray[i];
			}
			return byteArray;
	
		}
		return new byte[] {};
	}

	/**
			* ��16����Stringת��Ϊutf-8��ʽ
	 * @param s
	 *            ��Ҫת����String����
	 * @return ת�����utf-8��ʽ��s
	 */

	private String toutf8 (String s) {
		byte[] baKeyword = new byte[s.length() / 2];
		for (int i = 0; i < baKeyword.length; i++) {
			try {
				baKeyword[i] = (byte) (0xff & Integer.parseInt(s.substring(
						i * 2, i * 2 + 2), 16));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			s = new String(baKeyword, "utf-8");// UTF-16le:Not
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return s;
	}

	/**
	 * ��16����Stringת��Ϊutf-8��ʽ
	 * @param str
	 *            ��Ҫת����String����
	 * @return ת�����utf-8��ʽ��resuly
	 */

	public static String toutf8_2 (String str) {
		String result = null;
		try {
			result = new String(str.getBytes("UTF-8"), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * ��һ��Stringת��Ϊ16���Ƹ�ʽ
	 * @param str
	 *            ��Ҫת����String����
	 * @return ת�����16���Ƹ�ʽ�ַ���
	 */

	public String convertStringToHex(String str){

	//���ַ���ת����char����
		char[] chars = str.toCharArray();

	//�½�һ���ַ���������
		StringBuffer hex = new StringBuffer();

	//ѭ��ÿһ��char
		for(int i = 0; i < chars.length; i++){

	//��ÿһ��char��ת����16���Ƶģ�Ȼ����ӵ��ַ������������
			hex.append(Integer.toHexString((int)chars[i]));
		}

		hex.append( "0A" );
	//��󷵻��ַ�������16���Ƶ��ַ���
		return hex.toString();
	}

	/**
	 * @param fileDirName:�ļ���Ŀ¼
	 * @param fileName:�ļ�����
	 * @param ways:��ȡ�ļ��ķ�ʽ
	 * @Function: �Ӵ洢·���ж�������
	 * @Return:
	 */
	private void ReadDataFromStorage(String fileDirName, String fileName, int ways) throws IOException {
		File file = new File(fileDirName, fileName);
		int Ways = ways;
		if (Ways == 0) {
			Log.i("information", "FileInputStream");
			try {
				FileInputStream fileInputStream = new FileInputStream(file);
				byte[] bytes = new byte[fileInputStream.available()];
				fileInputStream.read(bytes);
				String result = new String(bytes);
				Log.i("information", "The data is:" + result);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (Ways == 1) {   //���ʹ�� Buffer ����������ȫ���� �������ļ�
			Log.i("information", "Bufferreader");
			try {
				BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
				String readline = "";
				StringBuffer stringBuffer = new StringBuffer();
				while ((readline = bufferedReader.readLine()) != null) {
					stringBuffer.append(readline);
				}
				if (bufferedReader != null) {
					bufferedReader.close();
					Log.i("information", "The data is:" + stringBuffer);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (Ways == 2) {
			Log.i("information", "Input+Buffer");
			try {
				String fileContent = null;
				InputStreamReader read = new InputStreamReader(new FileInputStream(file), "UTF-8");
				BufferedReader reader = new BufferedReader(read);
				String line;
				while ((line = reader.readLine()) != null) {
					fileContent += line;
				}
				reader.close();
				read.close();
				Log.i("information", fileContent);
			} catch (Exception e) {
				e.printStackTrace();
				Log.i("error", e.getMessage());
			}
		} else
			Ways = 2;
	}

	/**
	 * @param ms:Ҫ�ӳٵ�ʱ�䣬�Ժ���Ϊ��λ
	 * @Function: ʵ���ӳ�
	 * @Return:
	 */
	private void delay(int ms){
		try {
			Thread.currentThread();
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param str:Ҫ������ַ���
	 * @Function: ����ָ��
	 * @Return:
	 */

	private void send_command(String str){

		String ts_1 = convertStringToHex( str );

		byte[] to_send = toByteArray(ts_1);		//��16���Ʒ���

//		byte[] to_send = toByteArray2(writeText.getText().toString());		//���ַ�����ʽ����

		int retval = MyApp.driver.WriteData(to_send, to_send.length);//д���ݣ���һ������Ϊ��Ҫ���͵��ֽ����飬�ڶ�������Ϊ��Ҫ���͵��ֽڳ��ȣ�����ʵ�ʷ��͵��ֽڳ���

		if (retval < 0)
			Toast.makeText(MainActivity.this, "Write failed!",
					Toast.LENGTH_SHORT).show();

		Toast.makeText(MainActivity.this, str,
				Toast.LENGTH_SHORT).show();
	}

	/**
	 * @param ts_1:Ҫ������ַ���
	 * @Function: ����ȡ��ָ��ת���ɶ����Ƹ������
	 * @Return:
	 */

	private void convert_send ( String ts_1 ) {

//		String ts_1 = writeText.getText().toString();

		byte[] to_send = toByteArray(ts_1);		//��16���Ʒ���

//		byte[] to_send = toByteArray2(writeText.getText().toString());		//���ַ�����ʽ����

		int retval = MyApp.driver.WriteData(to_send, to_send.length);//д���ݣ���һ������Ϊ��Ҫ���͵��ֽ����飬�ڶ�������Ϊ��Ҫ���͵��ֽڳ��ȣ�����ʵ�ʷ��͵��ֽڳ���
		if (retval < 0)
			Toast.makeText(MainActivity.this, "Write failed!",
					Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * @param :
	 * @Function: ȡ����ʱ��
	 * @Return:
	 */

	//To stop timer
	private void stopTimer(){
		if(timer != null){
			timer.cancel();
			timer.purge();
		}
	}

	/**
	 * @param stri:Ҫ������ַ���
	 * @Function: ����0�����ÿ��7000���뷢��һ������
	 * @Return:
	 */

	//To start timer
	private void startTimer( final String stri ) {
		timer = new Timer();
		timerTask = new TimerTask() {
			public void run() {
				handler.post(new Runnable() {
					public void run() {
						//your code is here
						send_command( stri );
					}
				});
			}
		};
		timer.schedule(timerTask, 0, 7000);
	}

	/**
	 * @param str:Ҫ������ַ���
	 * @Function: �ҵ��ַ����е�һ��0��λ��
	 * @Return:first_zero
	 */

	private int Firstzero( String str )
	{
		int first_zero = 0;
		for ( int j = 0; j < str.length(); j++ )
		{
			if ( str.charAt(j) == 48 )
			{
				first_zero = j;
				break;
			}
		}
		return first_zero;
	}

	/**
	 * @param str,start,end:Ҫ������ַ���,��ʼ������
	 * @Function: ��ԭʼ��������ȡ��ָ������
	 * @Return:first_zero
	 */

	private  String convstr ( String str, int start, int end )
	{
		String str_1 = "";
		for ( int k = start; k <= end; k++ )
		{
			str_1 += str.charAt(k);
		}
		return str_1;
	}

	/**
	 * @param :
	 * @Function: ����Ƿ񱻸���Ȩ��
	 * @Return:first_zero
	 */

	private void checkPermission() {
		//���д��Ȩ�ޣ�NEED_PERMISSION���Ƿ���Ȩ PackageManager.PERMISSION_GRANTED��ʾͬ����Ȩ
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {
			//�û��Ѿ��ܾ���һ�Σ��ٴε���Ȩ������Ի�����Ҫ���û�һ������
			if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission
					.WRITE_EXTERNAL_STORAGE)) {
				Toast.makeText(this, "�뿪ͨ���Ȩ�ޣ������޷�����ʹ�ñ�Ӧ�ã�", Toast.LENGTH_SHORT).show();
			}
			//����Ȩ��
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0x001);

		} else {
			Toast.makeText(this, "��Ȩ�ɹ���", Toast.LENGTH_SHORT).show();
			Log.i("information", "checkPermission: �Ѿ���Ȩ��");
		}
	}

}

