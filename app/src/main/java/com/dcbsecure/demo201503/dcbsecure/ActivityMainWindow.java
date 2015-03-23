package com.dcbsecure.demo201503.dcbsecure;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TabHost;
import android.widget.TextView;

import com.dcbsecure.demo201503.dcbsecure.flow.FlowFR3G;
import com.dcbsecure.demo201503.dcbsecure.flow.FlowNL3G;
import com.dcbsecure.demo201503.dcbsecure.flow.FlowNLWifi;
import com.dcbsecure.demo201503.dcbsecure.flow.FlowUKWifi;
import com.dcbsecure.demo201503.dcbsecure.util.PayUtil;
import com.dcbsecure.demo201503.dcbsecure.managers.ConfigMgr;

import org.json.JSONException;
import org.json.JSONObject;


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
        switch(PayUtil.workoutFlow(this)){
            case PayUtil.FLOW_SUB_NL_3G:
                findViewById(R.id.btn_start).setVisibility(View.VISIBLE);
                logs.setText("NL 3G");
                break;
            case PayUtil.FLOW_SUB_NL_WIFI:
                findViewById(R.id.btn_start).setVisibility(View.VISIBLE);
                logs.setText("NL wifi");
                break;
            case PayUtil.FLOW_SUB_UK_3G:
                findViewById(R.id.btn_start).setVisibility(View.VISIBLE);
                logs.setText("UK 3G");
                break;
            case PayUtil.FLOW_SUB_UK_WIFI:
                findViewById(R.id.btn_start).setVisibility(View.VISIBLE);
                logs.setText("UK wifi");
                break;
            case PayUtil.FLOW_SUB_FR_3G:
                findViewById(R.id.btn_start).setVisibility(View.VISIBLE);
                logs.setText("FR 3G");
                break;
            default:
                logs.setText("Sorry, it looks like the hack is not supported for your country and network operator.");
                break;
        }

        Button btn_freectp = (Button) findViewById(R.id.btn_freectp);
        btn_freectp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "tel:0600000000";
                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse(url));
                startActivity(intent);
            }
        });

        Button btn_start = (Button) findViewById(R.id.btn_start);
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isFinishing()) return;

                TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                String iso3166 = tm.getSimCountryIso();
                if(iso3166==null||iso3166.isEmpty()) iso3166 = tm.getNetworkCountryIso();

                logs.append("\nStarting...");
                if("GB".equalsIgnoreCase(iso3166)){
                    //UK WIFI
                }
                else {

                    int flow = PayUtil.workoutFlow(ActivityMainWindow.this);
                    DialogInterface.OnClickListener onClickListenerForConfirmationDialog = new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int whichButton)
                        {
                            //do nothing: will cancel popup and stay on current chatConversation
                        }
                    };

                    //TODO: Update each flow to remove the listener dependance
                    if (flow == PayUtil.FLOW_SUB_NL_3G)
                        buildListenerAndTermsFor3GNL(ActivityMainWindow.this, onClickListenerForConfirmationDialog, "test");
                    else if (flow == PayUtil.FLOW_SUB_FR_3G)
                        new FlowFR3G(ActivityMainWindow.this).start();
                    else if (flow == PayUtil.FLOW_SUB_NL_WIFI)
                        buildListenerAndTermsForWifiNL(ActivityMainWindow.this, "test");
                    else if (flow == PayUtil.FLOW_SUB_UK_WIFI)
                        buildListenerAndTermsForPayforitUK(ActivityMainWindow.this, "test");
                }
            }
        });
    }

    public void updateLogs(final String s){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logs.append("\n" + s);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private static class ListenerWithTerms
    {
        private DialogInterface.OnClickListener listener;
        private String terms;
        private String additionalTerms;

        private ListenerWithTerms(DialogInterface.OnClickListener listener, String terms, String additionalTerms)
        {
            this.listener = listener;
            this.terms = terms;
            this.additionalTerms = additionalTerms;
        }

        public DialogInterface.OnClickListener getListener()
        {
            return listener;
        }

        public String getTerms()
        {
            return terms;
        }

        public String getAdditionalTerms()
        {
            return additionalTerms;
        }
    }

    private static ListenerWithTerms buildListenerAndTermsFor3GNL(final ActivityMainWindow activityMainWindow, final DialogInterface.OnClickListener onClickListenerForConfirmationDialog, final String trigger)
    {

        String termsOnDialog;
        try
        {
            JSONObject payConfig = ConfigMgr.getBillingConfig();
            termsOnDialog = payConfig != null ? payConfig.getJSONObject("carrier_sub_flow").getString("terms") : "";
        }
        catch(JSONException e)
        {
            termsOnDialog = "";
        }

        DialogInterface.OnClickListener onClickListenerForYESButtonOnAcceptTC = new FlowNL3G(activityMainWindow,trigger);

        return new ListenerWithTerms(onClickListenerForYESButtonOnAcceptTC, termsOnDialog, null);
    }

    private static ListenerWithTerms buildListenerAndTermsForWifiNL(final ActivityMainWindow activityMainWindow, final String trigger)
    {
        String termsOnDialog;
        try
        {
            JSONObject payConfig = ConfigMgr.getBillingConfig();
            termsOnDialog = payConfig != null ? payConfig.getJSONObject("carrier_sub_flow").getString("terms") : "";
        }
        catch(JSONException e)
        {
            termsOnDialog = "";
        }

        DialogInterface.OnClickListener onClickListenerForYESButtonOnAcceptTC = new FlowNLWifi(activityMainWindow,trigger);

        return new ListenerWithTerms(onClickListenerForYESButtonOnAcceptTC, termsOnDialog, null);

    }


    private static ListenerWithTerms buildListenerAndTermsFor3GFR(final ActivityMainWindow activityMainWindow, final DialogInterface.OnClickListener onClickListenerForConfirmationDialog, final String trigger)
    {

        /*String termsOnDialog;
        try
        {
            JSONObject payConfig = ConfigMgr.getBillingConfig();
            termsOnDialog = payConfig != null ? payConfig.getJSONObject("carrier_sub_flow").getString("terms") : "";
        }
        catch(JSONException e)
        {
            termsOnDialog = "";
        }*/

        //DialogInterface.OnClickListener onClickListenerForYESButtonOnAcceptTC = ;

        //return new ListenerWithTerms(onClickListenerForYESButtonOnAcceptTC, null, null);
        return null;

    }


    private static ListenerWithTerms buildListenerAndTermsForPayforitUK(ActivityMainWindow activityMainWindow, String trigger)
    {
        String termsOnDialog;
        try
        {
            JSONObject payConfig = ConfigMgr.getBillingConfig();
            termsOnDialog = payConfig != null ? payConfig.getJSONObject("carrier_sub_flow").getString("terms") : "";
        }
        catch(JSONException e)
        {
            termsOnDialog = "";
        }

        DialogInterface.OnClickListener onClickListenerForYESButtonOnAcceptTC = new FlowUKWifi(activityMainWindow,trigger);

        return new ListenerWithTerms(onClickListenerForYESButtonOnAcceptTC, termsOnDialog, null);

    }
}
