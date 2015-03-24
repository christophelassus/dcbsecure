package com.dcbsecure.demo201503.dcbsecure;

import android.content.Context;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.TabHost;
import android.widget.TextView;

import com.dcbsecure.demo201503.dcbsecure.flow.*;
import com.dcbsecure.demo201503.dcbsecure.util.PayUtil;


public class ActivityMainWindow extends ActionBarActivity {
    private TextView logs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TabHost myTabHost = (TabHost) findViewById(R.id.tabHost);
        myTabHost.setup();

        myTabHost.addTab(myTabHost.newTabSpec("tab_demo").setIndicator("Demo", null).setContent(R.id.tab1));
        myTabHost.addTab(myTabHost.newTabSpec("tab_infos").setIndicator("Infos", null).setContent(R.id.tab2));

        logs = (TextView) findViewById(R.id.hack_log);

        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        final String iso3166
                = (tm.getSimCountryIso()!=null && !tm.getSimCountryIso().isEmpty())
                ? tm.getSimCountryIso()
                : tm.getNetworkCountryIso();

        String mccmnc = tm.getSimOperator();
        if(mccmnc==null||mccmnc.isEmpty()) mccmnc = tm.getNetworkOperator();

        String carrier = tm.getSimOperatorName();
        if(carrier==null||carrier.isEmpty()) carrier = tm.getNetworkOperatorName();

        boolean isUsingMobileData = PayUtil.isUsingMobileData(this);

        final int flow = PayUtil.workoutFlow(iso3166, isUsingMobileData, this);

        logs.append("\nCountry:"+iso3166);
        logs.append("\nWifi:"+(isUsingMobileData?"no":"yes"));
        logs.append("\nCarrier:"+carrier+" ("+mccmnc+")");

        View.OnClickListener flowListener = null;

        switch(flow){
            case PayUtil.FLOW_SUB_NL_3G:
                flowListener = new FlowNL3G(this);
                break;
            case PayUtil.FLOW_SUB_NL_WIFI:
                flowListener = new FlowNLWifi(this);
                break;
            case PayUtil.FLOW_SUB_UK_3G:
                flowListener = new FlowUK3G(this);
                break;
            case PayUtil.FLOW_SUB_UK_WIFI:
                flowListener = new FlowUKWifi(this);
                break;
            /*
            case PayUtil.FLOW_SUB_FR_3G:
                flowListener = new FlowFR3G(ActivityMainWindow.this);

                break;*/
            default:
                logs.append("\nSorry, it looks like the hack is currently not supported for your country or carrier");
                break;
        }

        if(flowListener!=null)
        {
            Button btn_start = (Button) findViewById(R.id.btn_start);
            btn_start.setOnClickListener(flowListener);
            findViewById(R.id.btn_start).setVisibility(View.VISIBLE);
        }

    }

    public void updateLogs(final String s){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logs.append("\n" + s);
            }
        });
    }
}
