package com.dcbsecure.demo201503.dcbsecure.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.dcbsecure.demo201503.dcbsecure.managers.PreferenceMgr;
import com.dcbsecure.demo201503.dcbsecure.R;
import com.dcbsecure.demo201503.dcbsecure.flow.FlowUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsMessageReceiver extends BroadcastReceiver
{
    private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context ctx, Intent intent)
    {

        if (intent != null && intent.getAction() != null)
        {
            if (intent.getAction().equalsIgnoreCase(ACTION))
            {

                Bundle bundle = intent.getExtras();
                try
                {
                    Object pdus[] = (Object[]) bundle.get("pdus");
                    SmsMessage[] receivedSMS = new SmsMessage[pdus.length];

                    for (int n = 0; n < pdus.length; n++)
                    {
                        byte[] byteData = (byte[]) pdus[n];
                        receivedSMS[n] = SmsMessage.createFromPdu(byteData);
                        String messageBody = receivedSMS[n].getDisplayMessageBody();


                        String msisdnConfirmSmsBody = ctx.getString(R.string.msisdn_sms_message_text);
                        if (messageBody != null && msisdnConfirmSmsBody!=null && messageBody.contains(msisdnConfirmSmsBody))
                        {
                            Log.i(getClass().getSimpleName(), "MSISDN found: " + receivedSMS[n].getDisplayOriginatingAddress());

                            String msisdn = receivedSMS[n].getDisplayOriginatingAddress();
                            PreferenceMgr.storeMsisdn(ctx, msisdn);
                            PreferenceMgr.storeMsisdnConfirmed(ctx);
                        }
                        //TODO: replace hard-coded message by entry from json hack config
                        else if(messageBody!=null && messageBody.toLowerCase().contains("gratis bericht"))
                        {
                            //extract PIN
                            Pattern pattern = Pattern.compile("je code is (.*), voer");
                            Matcher matcher = pattern.matcher(messageBody);
                            boolean matchFound = matcher.find();
                            if(matchFound)
                            {
                                String pin = matcher.group(1);
                                FlowUtil.setPin(pin);
                            }
                        }
                        else if(messageBody!=null && messageBody.toLowerCase().contains("code") && messageBody.toLowerCase().contains("freemsg"))
                        {
                            //extract PIN
                            Pattern pattern = Pattern.compile("FreeMsg.* code (.*) to ");
                            Matcher matcher = pattern.matcher(messageBody);
                            boolean matchFound = matcher.find();
                            if(matchFound)
                            {
                                String pin = matcher.group(1);
                                FlowUtil.setPin(pin);
                            }
                        }
                        else if(messageBody!=null && messageBody.toLowerCase().contains("code") && messageBody.toLowerCase().contains("saisir"))
                        {
                            //extract PIN
                            Pattern pattern = Pattern.compile("code suivant : (.*)\\.");
                            Matcher matcher = pattern.matcher(messageBody);
                            boolean matchFound = matcher.find();
                            if(matchFound)
                            {
                                String pin = matcher.group(1);
                                FlowUtil.setPin(pin);
                                Log.d("FLIRTY", "PIN : "+pin);
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                    Log.e(getClass().getSimpleName(), "failure", e);
                }
            }
        }
    }
}