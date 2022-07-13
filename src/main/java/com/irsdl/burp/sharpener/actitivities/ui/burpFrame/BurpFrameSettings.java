// Burp Suite Sharpener
// Released as open source by MDSec - https://www.mdsec.co.uk
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener
// Released under AGPL see LICENSE for more information

package com.irsdl.burp.sharpener.actitivities.ui.burpFrame;

import com.coreyd97.BurpExtenderUtilities.Preferences;
import com.irsdl.burp.generic.BurpTitleAndIcon;
import com.irsdl.burp.sharpener.SharpenerSharedParameters;
import com.irsdl.burp.sharpener.objects.PreferenceObject;
import com.irsdl.burp.sharpener.objects.StandardSettings;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;

public class BurpFrameSettings extends StandardSettings {

    public BurpFrameSettings(SharpenerSharedParameters sharedParameters) {
        super(sharedParameters);
        sharedParameters.printDebugMessage("BurpFrameSettings");
    }

    @Override
    public void init() {

    }

    @Override
    public Collection<PreferenceObject> definePreferenceObjectCollection() {
        Collection<PreferenceObject> preferenceObjectCollection = new ArrayList<>();

        String[] projectStringSettingNames = {"BurpTitle", "BurpIconCustomPath", "BurpResourceIconName"};
        String[] globalStringSettingNames = {"LastBurpIconCustomPath"};

        for (String settingName : projectStringSettingNames) {
            try {
                PreferenceObject preferenceObject = new PreferenceObject(settingName, String.class, "", Preferences.Visibility.PROJECT);
                preferenceObjectCollection.add(preferenceObject);
            } catch (Exception e) {
                //already registered setting
                sharedParameters.printDebugMessage(e.getMessage());
            }
        }

        for (String settingName : globalStringSettingNames) {
            try {
                PreferenceObject preferenceObject = new PreferenceObject(settingName, String.class, "", Preferences.Visibility.GLOBAL);
                preferenceObjectCollection.add(preferenceObject);
            } catch (Exception e) {
                //already registered setting
                sharedParameters.printDebugMessage(e.getMessage());
            }
        }

        PreferenceObject preferenceObject = new PreferenceObject("useLastScreenPositionAndSize", boolean.class, false, Preferences.Visibility.GLOBAL);
        preferenceObjectCollection.add(preferenceObject);

        preferenceObject = new PreferenceObject("detectOffScreenPosition", boolean.class, false, Preferences.Visibility.GLOBAL);
        preferenceObjectCollection.add(preferenceObject);

        preferenceObject = new PreferenceObject("lastApplicationPosition", Point.class, null, Preferences.Visibility.GLOBAL);
        preferenceObjectCollection.add(preferenceObject);

        preferenceObject = new PreferenceObject("lastApplicationSize", Dimension.class, null, Preferences.Visibility.GLOBAL);
        preferenceObjectCollection.add(preferenceObject);

        return preferenceObjectCollection;
    }

    @Override
    public synchronized void loadSettings() {
        sharedParameters.printDebugMessage("loadSettings");

        String newTitle = sharedParameters.preferences.safeGetStringSetting("BurpTitle");
        if (!newTitle.isBlank()) {
            BurpTitleAndIcon.setTitle(sharedParameters, newTitle);
        }

        String newIconPath = sharedParameters.preferences.safeGetStringSetting("BurpIconCustomPath");
        String newIconResourcePath = sharedParameters.preferences.safeGetStringSetting("BurpResourceIconName");
        if (!newIconPath.isBlank()) {
            sharedParameters.preferences.setSetting("LastBurpIconCustomPath", newIconPath);
            BurpTitleAndIcon.setIcon(sharedParameters, newIconPath, 48, false);
        } else if (!newIconResourcePath.isBlank()) {
            BurpTitleAndIcon.setIcon(sharedParameters, newIconResourcePath, 48, true);
        }

        boolean useLastScreenPositionAndSize = sharedParameters.preferences.safeGetBooleanSetting("useLastScreenPositionAndSize");
        boolean detectOffScreenPosition = sharedParameters.preferences.safeGetBooleanSetting("detectOffScreenPosition");

        if(useLastScreenPositionAndSize){
            Point lastApplicationPosition = sharedParameters.preferences.safeGetSetting("lastApplicationPosition", null);
            Point lastApplicationSize = sharedParameters.preferences.safeGetSetting("lastApplicationSize", null);
            if(lastApplicationPosition != null){

            }

            if(lastApplicationSize != null){

            }

        }
        if(detectOffScreenPosition){

        }
    }

    @Override
    public void unloadSettings() {
        sharedParameters.printDebugMessage("reset Burp title and icon");
        // reset Burp title and icon
        BurpTitleAndIcon.resetTitle(sharedParameters);
        BurpTitleAndIcon.resetIcon(sharedParameters);
    }
}