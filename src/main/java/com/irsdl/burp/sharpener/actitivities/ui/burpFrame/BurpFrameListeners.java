// Burp Suite Sharpener
// Released as open source by MDSec - https://www.mdsec.co.uk
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener
// Released under AGPL see LICENSE for more information

package com.irsdl.burp.sharpener.actitivities.ui.burpFrame;

import com.irsdl.burp.generic.BurpUITools;
import com.irsdl.burp.sharpener.SharpenerSharedParameters;
import com.irsdl.burp.sharpener.actitivities.ui.subTabs.SubTabsActions;
import com.irsdl.generic.UIHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.HashMap;

public class BurpFrameListeners implements ComponentListener {
    private final SharpenerSharedParameters sharedParameters;
    private boolean _isRecenterInProgress = false;
    private HashMap<String, String> burpFrameShortcutMappings = new HashMap<>() {{
        put("control alt C", "MoveToCenter");
    }};
    private boolean isResizedFrameCheckInProgress = false;
    private boolean isMovedFrameCheckInProgress = false;
    public BurpFrameListeners(SharpenerSharedParameters sharedParameters){
        this.sharedParameters = sharedParameters;
        addBurpFrameListener(sharedParameters.get_mainFrame());
        boolean detectOffScreenPosition = sharedParameters.preferences.safeGetBooleanSetting("detectOffScreenPosition");
        if(detectOffScreenPosition && !_isRecenterInProgress){
            checkAndCenterOffScreen(sharedParameters.get_mainFrame() , 0.1, true);
        }
    }

    public void addBurpFrameListener(JFrame jframe) {
        sharedParameters.printDebugMessage("addBurpFrameListener");
        jframe.addComponentListener(this);
        clearInputMap(jframe.getRootPane());

        burpFrameShortcutMappings.forEach((k, v) -> jframe.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(k), v));

        jframe.getRootPane().getActionMap().put("MoveToCenter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UIHelper.moveFrameToCenter(jframe);
            }
        });
    }

    public void removeBurpFrameListener(JFrame jframe) {
        sharedParameters.printDebugMessage("removeBurpFrameListener");
        jframe.removeComponentListener(this);
        clearInputMap(jframe.getRootPane());
    }

    @Override
    public synchronized void componentResized(ComponentEvent e) {
        if(!isResizedFrameCheckInProgress){
            isResizedFrameCheckInProgress = true;
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            Dimension newSize = e.getComponent().getBounds().getSize();
                            Point newLocation = e.getComponent().getBounds().getLocation();
                            sharedParameters.preferences.safeSetSetting("lastApplicationSize", newSize);
                            sharedParameters.preferences.safeSetSetting("lastApplicationPosition", newLocation);
                            boolean detectOffScreenPosition = sharedParameters.preferences.safeGetBooleanSetting("detectOffScreenPosition");
                            if(detectOffScreenPosition && !_isRecenterInProgress){
                                checkAndCenterOffScreen(sharedParameters.get_mainFrame(), 0.8, false);
                            }
                            isResizedFrameCheckInProgress = false;
                        }
                    },
                    2000 // 2 seconds delay to decrease the amount of checking process
            );
        }
    }

    @Override
    public synchronized void componentMoved(ComponentEvent e) {
        if(!isMovedFrameCheckInProgress) {
            isMovedFrameCheckInProgress = true;
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            Dimension newSize = e.getComponent().getBounds().getSize();
                            Point newLocation = e.getComponent().getBounds().getLocation();
                            sharedParameters.preferences.safeSetSetting("lastApplicationSize", newSize);
                            sharedParameters.preferences.safeSetSetting("lastApplicationPosition", newLocation);
                            boolean detectOffScreenPosition = sharedParameters.preferences.safeGetBooleanSetting("detectOffScreenPosition");
                            if (detectOffScreenPosition && !_isRecenterInProgress) {
                                checkAndCenterOffScreen(sharedParameters.get_mainFrame(), 0.8, false);
                            }
                            isMovedFrameCheckInProgress = false;
                        }
                    },
                    1000 // 1 second delay to decrease the amount of checking process
            );
        }
    }

    @Override
    public void componentShown(ComponentEvent e) {

    }

    @Override
    public void componentHidden(ComponentEvent e) {

    }

    public synchronized void checkAndCenterOffScreen(JFrame jframe, double offScreenMargin, boolean isChoice){
        _isRecenterInProgress = true;
        if (jframe != null && UIHelper.isFrameOutOffScreen(jframe, offScreenMargin)) {
            if(isChoice){
                int response = UIHelper.askConfirmMessage(sharedParameters.extensionName + ": Off Screen Window", "Burp Suite is %"+(int) (offScreenMargin*100) +" outside the screen, do you want to bring it to the center?", new String[]{"Yes", "No"}, null);
                if (response == 0) {
                    UIHelper.moveFrameToCenter(jframe);
                }
            }else{
                UIHelper.moveFrameToCenter(jframe);
                UIHelper.showWarningMessage(sharedParameters.extensionName + ": Burp Suite was at least %"+(int) (offScreenMargin*100) +" outside the screen, therefore, it's been moved to the center!", null);
            }

        }
        _isRecenterInProgress = false;
    }

    private void clearInputMap(JComponent jc) {
        burpFrameShortcutMappings.forEach((k, v) -> jc.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(k), "none"));
    }
}
