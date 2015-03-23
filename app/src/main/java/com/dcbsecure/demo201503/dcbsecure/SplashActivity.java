package com.dcbsecure.demo201503.dcbsecure;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dcbsecure.demo201503.dcbsecure.managers.ConfigMgr;
import com.dcbsecure.demo201503.dcbsecure.managers.PreferenceMgr;

import java.util.Timer;
import java.util.TimerTask;


public class SplashActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Log.d("DBCSecure", "Device id : " + ConfigMgr.lookupDeviceId(this));
        /*final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(SplashActivity.this, ActivityMainWindow.class);
                startActivity(i);
                finish();
            }
        }, 3000);*/

        if (PreferenceMgr.isMsisdnConfirmed(getApplicationContext())) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(SplashActivity.this, ActivityMainWindow.class);
                    startActivity(i);
                    finish();
                }
            }, 3000);
        }
        else {
            verifyMsisdn(null, getString(R.string.dialog_msisdn_message));
        }
    }

    private void verifyMsisdn(String msisdn, final String dialogMessage)
    {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(dialogMessage);
        builder.setCancelable(false);

        final EditText editMsisdn = new EditText(this);
        editMsisdn.setInputType(InputType.TYPE_CLASS_PHONE);
        if (msisdn != null && !msisdn.isEmpty()) editMsisdn.setText(msisdn, TextView.BufferType.EDITABLE);
        editMsisdn.setHint(R.string.enter_your_number);
        builder.setView(editMsisdn);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {

                final String msisdnEntered = editMsisdn.getText().toString();

                dialog.dismiss();

                if (msisdnEntered.isEmpty())
                {
                    verifyMsisdn(msisdnEntered, dialogMessage);
                    return;
                }
                else
                {
                    try
                    {
                        //check number entered by user looks like a long
                        Long.parseLong(msisdnEntered);
                    }
                    catch (NumberFormatException e)
                    {
                        verifyMsisdn(msisdnEntered, dialogMessage);
                        return;
                    }
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(SplashActivity.this);
                builder.setMessage(getString(R.string.processing));
                ProgressBar pb = new ProgressBar(SplashActivity.this);
                pb.setIndeterminate(true);
                builder.setView(pb);
                builder.setCancelable(false);
                final AlertDialog sendingSmsDialog = builder.create();
                sendingSmsDialog.show();


                String SENT = "SMS_VERIFICATION_SENT";
                SmsManager smsManager = SmsManager.getDefault();
                PendingIntent sentPI = PendingIntent.getBroadcast(SplashActivity.this, 0, new Intent(SENT), PendingIntent.FLAG_CANCEL_CURRENT);
                registerReceiver(new BroadcastReceiver()
                {
                    @Override
                    public void onReceive(Context context, Intent intent)
                    {
                        if (getResultCode() == Activity.RESULT_OK)
                        {
                            //sms has been sent, we can fire up the progress dialog to wait for the sms to arrive back
                            showProgressDialog();
                        }
                        else if (getResultCode() == SmsManager.RESULT_ERROR_GENERIC_FAILURE)
                        {
                            verifyMsisdn(msisdnEntered, getString(R.string.sms_verification_failed_credits));
                        }
                        else
                        {
                            verifyMsisdn(msisdnEntered,getString(R.string.dialog_msisdn_message));
                        }
                        unregisterReceiver(this);
                        sendingSmsDialog.dismiss();
                    }

                }, new IntentFilter(SENT));

                smsManager.sendTextMessage(msisdnEntered, null, getString(R.string.msisdn_sms_message_text), sentPI, null);

            }
        });

        AlertDialog dialogVerifyMsisdn = builder.create();
        dialogVerifyMsisdn.show();

        Button bPositive = dialogVerifyMsisdn.getButton(DialogInterface.BUTTON_POSITIVE);
        if (bPositive != null)
        {
            bPositive.setTextColor(getResources().getColor(android.R.color.white));
            bPositive.setBackgroundDrawable(getResources().getDrawable(R.drawable.redbutton_nocorner_background));
            bPositive.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        }


    }

    private void showProgressDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.verifying_number));
        // show spinner for 30 seconds. After that we conclude the entered number is wrong.
        ProgressBar pb = new ProgressBar(SplashActivity.this);
        pb.setIndeterminate(true);
        builder.setView(pb);
        builder.setCancelable(false);

        final AlertDialog waitingDialog = builder.create();


        // start the waiting task
        TimerTask task = new TimerTask()
        {
            @Override
            public void run()
            {
                final long WAITING_TIME = 30000;
                final long SLEEP_TIME = 1000;
                final long END_TIME = System.currentTimeMillis() + WAITING_TIME;

                while (System.currentTimeMillis() <= END_TIME)
                {
                    if (PreferenceMgr.isMsisdnConfirmed(getApplicationContext()))
                    {
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                waitingDialog.dismiss();
                                Intent i = new Intent(SplashActivity.this, ActivityMainWindow.class);
                                startActivity(i);
                                finish();
                            }
                        });
                        return;
                    }

                    try
                    {
                        Thread.sleep(SLEEP_TIME);
                    }
                    catch (Exception e)
                    {
                    }

                }

                if (!PreferenceMgr.isMsisdnConfirmed(getApplicationContext()))
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            waitingDialog.dismiss();
                            Toast.makeText(getApplicationContext(), getString(R.string.wait_message_error), Toast.LENGTH_SHORT).show();
                            verifyMsisdn(null, getString(R.string.dialog_msisdn_message));
                        }
                    });
                }
            }
        };

        waitingDialog.show();

        Timer timer = new Timer();
        timer.schedule(task, 0);
    }
}
