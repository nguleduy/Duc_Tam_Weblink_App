/****************************************************************************
 *
 * @file ServicesFragment.java
 * @brief
 *
 * Contains the ServicesFragment class.
 *
 * @author Abalta Technologies, Inc.
 * @date Jan, 2014
 *
 * @cond Copyright
 *
 * COPYRIGHT 2014 ABALTA TECHNOLOGIES ALL RIGHTS RESERVED.<br>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @endcond
 *****************************************************************************/
package com.abaltatech.weblinkclientapp;


import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.abaltatech.weblinkclientapp.services.ServiceClient;
import com.abaltatech.weblinkclientapp.services.Services;
import com.testabalta.R;

/**
 * Optional weblink feature which allows for services to connect through WEBLINK to provide
 * customizable functionality.  This fragment provides various settings and demo functionality.
 *
 */
public class ServicesFragment extends Fragment {

	private static final String TAG = ServicesFragment.class.getSimpleName();

    Button m_btnServiceClientConnect;
    Button m_btnServiceClientTimerCtl;
    Button m_btnServiceClientTimerReset;
    Button m_btnServiceProxyServer;
    Button m_btnServiceProxyClient;
    Button m_btnServiceRegister;
    Button m_btnServiceTimerCtl;
    Button m_btnServiceTimerReset;
    
    
    TextView m_tvServiceTimerStatus;
    TextView m_tvServiceClientTimerStatus;
    
    Spinner m_spnServiceClientPicker;
    int m_pickerItem = 0;
    boolean m_serviceClientToggle = false;

    private Services m_services = App.instance().getWLClient().getServices();
    private ServiceClient m_serviceClient = App.instance().getWLClient().getServiceClient();
    
    private Handler m_handler = new Handler();

    /** Called when the activity is first created. */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.services_fragment,
                container, false);
        
        

        m_btnServiceClientConnect = (Button) view.findViewById(R.id.btnServiceClientConnect);
        m_btnServiceClientConnect.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				m_serviceClient.OnConnectService(m_pickerItem);
				updateUI();				
			}});
        m_btnServiceClientTimerCtl = (Button) view.findViewById(R.id.btnServiceClientTimerCtl);
        m_btnServiceClientTimerCtl.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				m_serviceClient.onStart(m_serviceClientToggle);
				m_serviceClientToggle = !m_serviceClientToggle;
				updateUI();
			}});
        m_btnServiceClientTimerReset = (Button) view.findViewById(R.id.btnServiceClientTimerReset);
        m_btnServiceClientTimerReset.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				m_serviceClient.onReset();
				updateUI();
			}});
        m_btnServiceProxyClient = (Button) view.findViewById(R.id.btnServiceProxyClient);
        m_btnServiceProxyClient.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				m_services.onHTTPProxyClient();
				updateUI();
			}});
        m_btnServiceProxyServer = (Button) view.findViewById(R.id.btnServiceProxyServer);
        m_btnServiceProxyServer.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				m_services.onHTTPProxyServer();
				updateUI();
			}});
        m_btnServiceRegister = (Button) view.findViewById(R.id.btnServiceRegister);
        m_btnServiceRegister.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				m_services.OnRegisterService();
				updateUI();
			}});
        m_btnServiceTimerCtl = (Button) view.findViewById(R.id.btnServiceTimerCtl);
        m_btnServiceTimerCtl.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				m_services.onStart();
				updateUI();
			}});
        m_btnServiceTimerReset = (Button) view.findViewById(R.id.btnServiceTimerReset);
        m_btnServiceTimerReset.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				m_services.onReset();
				updateUI();
			}});
        

        m_tvServiceTimerStatus = (TextView) view.findViewById(R.id.tvServiceTimerStatus);
        m_tvServiceClientTimerStatus = (TextView) view.findViewById(R.id.tvServiceClientTimerStatus);
        
        final String[] items = {"WL App Service","Client Service","Proxy Service"};
        m_spnServiceClientPicker = (Spinner) view.findViewById(R.id.spnServiceClientPicker);
        m_spnServiceClientPicker.setAdapter(new ArrayAdapter<String>(view.getContext(), android.R.layout.simple_spinner_item, items));
        m_spnServiceClientPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3) {
            	m_pickerItem = arg2;
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        
        return view;

    }
    
    private final Runnable m_periodic = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			updateUI();
			m_handler.postDelayed(m_periodic, 1000);
		}
    	
    };
    
    public void updateUI() {
        //services
        boolean isServiceReg = m_services.getServiceRegistered(); 
        boolean isTimerStart = m_services.getTimerStarted();
        boolean isProxyServer = m_services.getProxyServerStarted();
        boolean isProxyClient = m_services.getProxyClientStarted();

        m_btnServiceRegister.setText(isServiceReg ? "Unregister Service": "Register Service");
        m_btnServiceProxyServer.setText(isProxyServer ? "Stop Proxy Server": "Start Proxy Server");
        m_btnServiceProxyClient.setText(isProxyClient ? "Stop Proxy Client": "Start Proxy Client");
        m_btnServiceTimerCtl.setEnabled(isServiceReg);
        m_btnServiceTimerCtl.setText(isTimerStart ? "Stop": "Start");
        m_btnServiceTimerReset.setEnabled(isServiceReg && !isTimerStart);
        m_btnServiceTimerReset.setText("Reset");

        //services client
        boolean sc = m_serviceClient.getIsServiceConnected();
        boolean ts = this.m_serviceClientToggle;
        m_btnServiceClientConnect.setEnabled(true);
        m_btnServiceClientConnect.setText(sc?"Disconnect Service":"Connect Service");
        m_btnServiceClientTimerCtl.setEnabled(sc);
        m_btnServiceClientTimerCtl.setText(sc && ts ?"Stop":"Start");
        m_btnServiceClientTimerReset.setEnabled(sc && !ts);
        m_btnServiceClientTimerReset.setText("Reset");
    }


    @Override
    public void onStop(){
    	m_services.setTimerTextView(null);
    	m_serviceClient.setTimerTextView(null);
        super.onStop();
    }

    @Override
    public void onStart(){
    	m_services.setTimerTextView(m_tvServiceTimerStatus);
    	m_serviceClient.setTimerTextView(m_tvServiceClientTimerStatus);
        super.onStart();

    }
    @Override
    public void onResume(){
        super.onResume();
		updateUI();

		m_handler.postDelayed(m_periodic, 1000);
    }
    @Override
    public void onPause() {
    	super.onPause();

		m_handler.removeCallbacks(m_periodic);
    }
    @Override
    public void onDetach() {
        super.onDetach();
    }

}
