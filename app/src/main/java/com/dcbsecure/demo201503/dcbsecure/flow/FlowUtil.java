package com.dcbsecure.demo201503.dcbsecure.flow;


public class FlowUtil
{
    private static String pin = null;

    public static void setPin(String pin)
    {
        FlowUtil.pin = pin;
    }

    public static String getPin()
    {
        return pin;
    }

}
