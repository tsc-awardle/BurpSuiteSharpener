// Burp Suite Sharpener
// Released as open source by MDSec - https://www.mdsec.co.uk
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener
// Released under AGPL see LICENSE for more information

package com.irsdl.burp.sharpener.actitivities.ui.mainTabs;

import com.irsdl.burp.sharpener.SharpenerSharedParameters;

import javax.swing.*;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;

public class MainTabsListeners implements ContainerListener {
    private final SharpenerSharedParameters sharedParameters;
    private boolean isResetInProgress = false;

    public MainTabsListeners(SharpenerSharedParameters sharedParameters) {
        this.sharedParameters = sharedParameters;
        addTabListener(sharedParameters.get_rootTabbedPane());
    }

    public void addTabListener(JTabbedPane tabbedPane) {
        sharedParameters.printDebugMessage("addMainTabListener");
        tabbedPane.addContainerListener(this);
    }

    public void removeTabListener(JTabbedPane tabbedPane) {
        sharedParameters.printDebugMessage("removeMainTabListener");
        tabbedPane.removeContainerListener(this);
    }

    @Override
    public void componentAdded(ContainerEvent e) {
        if (e.getSource() instanceof JTabbedPane && !isResetInProgress) {
            setResetInProgress(true);
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            SwingUtilities.invokeLater(() -> {
                                MainTabsStyleHandler.resetMainTabsStylesFromSettings(sharedParameters);
                                setResetInProgress(false);
                            });
                        }
                    },
                    2000 // 2 seconds-delay to ensure all has been settled!
            );
        }

    }

    @Override
    public void componentRemoved(ContainerEvent e) {

    }

    public synchronized void setResetInProgress(boolean resetInProgress) {
        isResetInProgress = resetInProgress;
    }


}
