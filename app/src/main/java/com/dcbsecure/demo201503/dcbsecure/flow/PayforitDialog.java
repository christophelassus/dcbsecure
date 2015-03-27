package com.dcbsecure.demo201503.dcbsecure.flow;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.dcbsecure.demo201503.dcbsecure.ActivityMainWindow;
import com.dcbsecure.demo201503.dcbsecure.managers.PreferenceMgr;
import com.dcbsecure.demo201503.dcbsecure.R;


public class PayforitDialog extends Dialog
{
    private final ActivityMainWindow activityMainWindow;
    private final Button btnStart;

    public PayforitDialog(ActivityMainWindow activityMainWindow, Button btnStart) {
        super(activityMainWindow, R.style.PayforitDialogTheme);
        this.activityMainWindow = activityMainWindow;
        this.btnStart = btnStart;

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        setContentView(R.layout.dialog_payforit);

        TextView txtCancel = (TextView) findViewById(R.id.txtCancel);
        txtCancel.setPaintFlags(txtCancel.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);

        TextView billToMobile = (TextView) findViewById(R.id.billToMobile);

        String billToMobileTxt = "Bill to mobile";
        String msisdn = PreferenceMgr.getMsisdn(activityMainWindow);
        if(msisdn!=null && msisdn.length()>4)
        {
            billToMobileTxt = "Bill to mobile ending "+msisdn.substring(msisdn.length()-4);
        }
        billToMobile.setText(billToMobileTxt);

        findViewById(R.id.btnSubscribe).setOnClickListener(new FlowUKWifi(activityMainWindow, btnStart));

        findViewById(R.id.txtCancel).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dismiss();
            }
        });


        setOnKeyListener(new Dialog.OnKeyListener()
        {
            @Override
            public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event)
            {
                if (keyCode == KeyEvent.KEYCODE_BACK)
                {
                    PayforitDialog.this.dismiss();
                }
                return true;
            }
        });
    }
}
