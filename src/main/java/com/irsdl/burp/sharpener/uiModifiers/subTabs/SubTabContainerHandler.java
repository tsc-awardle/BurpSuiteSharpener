// Burp Suite Sharpener
// Released as open source by MDSec - https://www.mdsec.co.uk
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener
// Released under AGPL see LICENSE for more information

package com.irsdl.burp.sharpener.uiModifiers.subTabs;

import com.irsdl.burp.generic.BurpUITools;
import com.irsdl.burp.sharpener.SharpenerSharedParameters;
import com.irsdl.burp.sharpener.objects.TabFeaturesObject;
import com.irsdl.burp.sharpener.objects.TabFeaturesObjectStyle;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class SubTabContainerHandler {
    public JTabbedPane parentTabbedPane;
    public Container currentTabContainer;
    public Component currentTabLabel;
    public Component currentTabCloseButton;
    public ArrayList<Integer> tabIndexHistory = new ArrayList<>();
    public BurpUITools.MainTabs currentToolTab;

    private final SubTabContainerHandler instance;
    private final SharpenerSharedParameters sharedParameters;
    private ArrayList<String> cachedTabTitles;
    private boolean titleEditInProgress = false;
    private String beforeManualEditTabTitle = "";
    private Color originalTabColor;
    private PropertyChangeListener subTabPropertyChangeListener;
    private boolean isFromSetColor = false;
    private ArrayList<String> titleHistory = new ArrayList<String>();
    private Boolean _isVisible = true;
    private Boolean _hasChanges = false;
    private boolean _isDotDotDotTab = false;

    public SubTabContainerHandler(SharpenerSharedParameters sharedParameters, JTabbedPane tabbedPane, int tabIndex, boolean forComparison) {
        this.instance = this;
        this.sharedParameters = sharedParameters;
        this.parentTabbedPane = tabbedPane;
        Component currentTabTemp = tabbedPane.getTabComponentAt(tabIndex);
        if (!(currentTabTemp instanceof Container)) return; // this is not a container, so it is not useful for us

        // to find whether this subtab is in repeater or intruder:
        String toolTabName = "";
        Component _parentTabbedPane = tabbedPane.getParent();
        if (_parentTabbedPane instanceof JTabbedPane) {
            JTabbedPane parentTabbedPane = ((JTabbedPane) _parentTabbedPane);
            toolTabName = parentTabbedPane.getTitleAt(parentTabbedPane.indexOfComponent(tabbedPane));

        } else if (_parentTabbedPane instanceof JPanel && _parentTabbedPane.getParent() instanceof JTabbedPane) {
            // this is the ... tab!
            _isDotDotDotTab = true;
            JTabbedPane parentTabbedPane = ((JTabbedPane) _parentTabbedPane.getParent());
            toolTabName = parentTabbedPane.getTitleAt(parentTabbedPane.indexOfComponent(tabbedPane.getParent()));
        }else if (_parentTabbedPane instanceof JPanel){
            // it's being detached! who does that?! :p
            JPanel parentTabbedPane = ((JPanel) _parentTabbedPane);
            toolTabName = ((JFrame) parentTabbedPane.getRootPane().getParent()).getTitle().replace("Burp ", "");
        }

        currentToolTab = BurpUITools.getMainTabsObjFromString(toolTabName);

        if(currentToolTab == BurpUITools.MainTabs.None){
            // this is the new changes introduce by burp 2022.1 so we need this code now
            int currentTabToolIndex = sharedParameters.get_rootTabbedPane().indexOfComponent(tabbedPane.getParent());
            toolTabName = sharedParameters.get_rootTabbedPane().getTitleAt(currentTabToolIndex);
        }

        currentToolTab = BurpUITools.getMainTabsObjFromString(toolTabName);
        this.currentTabContainer = (Container) currentTabTemp;
        this.currentTabLabel = currentTabContainer.getComponent(0);

        if(currentTabLabel == null){
            sharedParameters.printlnError("An error has occurred when reading a specific tab. A restart might be needed.");
            return;
        }
        if (tabIndex != tabbedPane.getTabCount() - 1)
            currentTabCloseButton = currentTabContainer.getComponent(1); // to get the X button

        // to keep history of previous titles
        if (titleHistory.size() == 0)
            addTitleHistory(getTabTitle(), true);

        if (tabIndexHistory.size() == 0)
            tabIndexHistory.add(tabIndex);

        if(!forComparison)
            addSubTabWatcher();

        setHasChanges(false); // init mode
    }

    public boolean addSubTabWatcher() {
        if(!isValid())
            return false;
        // this.currentTabLabel.getPropertyChangeListeners().length is 2 by default in this case ... Burp Suite may change this and break my extension :s
        if (subTabPropertyChangeListener == null && this.currentTabLabel.getPropertyChangeListeners().length < 3) {
            subTabPropertyChangeListener = new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equalsIgnoreCase("editable")) {
                        if (evt.getSource().getClass().equals(currentTabLabel.getClass())) {
                            if (!titleEditInProgress) {
                                if ((boolean) evt.getNewValue() == true) {
                                    titleEditInProgress = true;
                                    beforeManualEditTabTitle = getTabTitle();
                                    originalTabColor = getColor();
                                }
                            } else {
                                if ((boolean) evt.getNewValue() == false) {
                                    titleEditInProgress = false;
                                    new java.util.Timer().schedule(
                                            new java.util.TimerTask() {
                                                @Override
                                                public void run() {
                                                    setColor(originalTabColor, false);
                                                    if (!beforeManualEditTabTitle.equals(getTabTitle())) {
                                                        addTitleHistory(beforeManualEditTabTitle, true);
                                                        // title has changed manually
                                                        sharedParameters.allSettings.subTabSettings.prepareAndSaveSettings(instance);
                                                    }
                                                    sharedParameters.allSettings.subTabSettings.loadSettings();
                                                }
                                            },
                                            500
                                    );
                                }
                            }
                        }

                    }else if (evt.getPropertyName().equalsIgnoreCase("disabledTextColor")) {
                        boolean isFromSetToDefault = false;
                        Color newColor = (Color) evt.getNewValue();

                        if(newColor!=null && isSetToDefaultColour(newColor)){
                            isFromSetToDefault = true;
                        }

                        loadDefaultSetting();

                        if(!isFromSetColor && !isFromSetToDefault){
                            if(newColor!=null && newColor.equals(sharedParameters.defaultSubTabObject.getColor())){
                                // we have a case for auto tab colour change which we want to avoid
                                setColor((Color) evt.getOldValue(), false);
                            }
                        }else if(newColor==null || isFromSetToDefault){
                            setColor(sharedParameters.defaultSubTabObject.getColor(), false);
                        }
                        isFromSetColor = false;
                    }
                }
            };
            this.currentTabLabel.addPropertyChangeListener(subTabPropertyChangeListener);
        } else if (this.currentTabLabel.getPropertyChangeListeners().length == 3) {
            subTabPropertyChangeListener = this.currentTabLabel.getPropertyChangeListeners()[2];
        }
        return true;
    }

    public void removeSubTabWatcher() {
        if (subTabPropertyChangeListener != null) {
            this.currentTabLabel.removePropertyChangeListener(subTabPropertyChangeListener);
        }
    }

    public TabFeaturesObject getTabFeaturesObject() {
        return new TabFeaturesObject(getTabIndex(), getTabTitle(), getTitleHistory(), getFontName(), getFontSize(), isBold(), isItalic(), getVisibleCloseButton(), getColor());
    }

    public TabFeaturesObjectStyle getTabFeaturesObjectStyle() {
        return getTabFeaturesObject().getStyle();
    }

    public void updateByTabFeaturesObject(TabFeaturesObject tabFeaturesObject, boolean keepHistory, boolean ignoreHasChanges) {
        this.setTabTitle(tabFeaturesObject.title, ignoreHasChanges);
        if(keepHistory){
            //this.setTitleHistory(tabFeaturesObject.titleHistory.toArray(String[]::new));
            this.setTitleHistory(tabFeaturesObject.titleHistory);
        }


        this.updateByTabFeaturesObjectStyle(tabFeaturesObject.getStyle(), ignoreHasChanges);
    }

    public void updateByTabFeaturesObjectStyle(TabFeaturesObjectStyle tabFeaturesObjectStyle, boolean ignoreHasChanges) {
        this.setFontName(tabFeaturesObjectStyle.fontName, ignoreHasChanges);
        this.setFontSize(tabFeaturesObjectStyle.fontSize, ignoreHasChanges);
        this.setBold(tabFeaturesObjectStyle.isBold, ignoreHasChanges);
        this.setItalic(tabFeaturesObjectStyle.isItalic, ignoreHasChanges);
        this.setVisibleCloseButton(tabFeaturesObjectStyle.isCloseButtonVisible, ignoreHasChanges);
        this.setColor(tabFeaturesObjectStyle.getColorCode(), ignoreHasChanges);

        /*
        // enabling auto save on property change after 2 seconds!
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        shouldSaveOnPropertyChange = true;
                    }
                },
                2000
        );
        */

    }

    public boolean isValid() {
        boolean result = true;

        if (parentTabbedPane == null || getTabIndex() == -1 || currentTabContainer == null || currentTabLabel == null ||
                currentTabCloseButton == null || currentTabLabel == null ) {
            result = false;
        }
        return result;
    }

    private void loadDefaultSetting() {
        // To set the defaultSubTabObject parameter which keeps default settings of a normal tab
        if (sharedParameters.defaultSubTabObject == null) {
            for (BurpUITools.MainTabs tool : sharedParameters.subTabSupportedTabs) {
                if (sharedParameters.supportedTools_SubTabs.get(tool) != null) {
                    JTabbedPane toolTabbedPane = sharedParameters.get_toolTabbedPane(tool);
                    if (toolTabbedPane != null) {
                        for (Component tabComponent : toolTabbedPane.getComponents()) {
                            int subTabIndex = toolTabbedPane.indexOfComponent(tabComponent);
                            if (subTabIndex == -1)
                                continue;
                            sharedParameters.defaultSubTabObject = new SubTabContainerHandler(sharedParameters, toolTabbedPane, toolTabbedPane.getTabCount() - 1,true);
                            break;
                        }
                    }
                }
                if (sharedParameters.defaultSubTabObject != null)
                    break;
            }
        }
    }

    public Boolean isDefaultColour(Color color){
        return Integer.toHexString(color.getRGB()).substring(2).equals("000000") || Integer.toHexString(color.getRGB()).substring(2).equals("010101")
                || Integer.toHexString(color.getRGB()).substring(2).equals("bbbbbb") || Integer.toHexString(color.getRGB()).substring(2).equals("bcbcbc");
    }

    public Boolean isSetToDefaultColour(Color color){
        return Integer.toHexString(color.getRGB()).substring(2).equals("010101") || Integer.toHexString(color.getRGB()).substring(2).equals("bcbcbc");
    }

    public boolean isDotDotDotTab(){
        return parentTabbedPane.getTabComponentAt(parentTabbedPane.getTabCount() - 1).equals(currentTabContainer);
    }

    public boolean isDefault() {
        boolean result = false;
        if(isValid()){
            if(isDefaultColour(getColor())){
                // this is useful when user has changed dark <-> light mode; so we can still detect a default colour!
                if (getTabIndex() == parentTabbedPane.getTabCount() - 1 || sharedParameters.defaultSubTabObject.getTabFeaturesObjectStyle().equalsIgnoreColor(getTabFeaturesObjectStyle())) {
                    result = true;
                }
            }else{
                if (getTabIndex() == parentTabbedPane.getTabCount() - 1 || sharedParameters.defaultSubTabObject.getTabFeaturesObjectStyle().equals(getTabFeaturesObjectStyle())) {
                    result = true;
                }
            }
        }
        loadDefaultSetting();
        return result;
    }

    public void setToDefault(boolean ignoreHasChanges) {
        if(isValid()){
            loadDefaultSetting();
            // in order to set the right colour when reset to default is used, we need to use a special colour to detect this event
            // this is because Burp does use the default colour when an item is changed - we have a workaround for that but
            // the workaround stops reset to default to change the colour as well so we need another workaround!!!
            TabFeaturesObjectStyle tfosDefault = sharedParameters.defaultSubTabObject.getTabFeaturesObjectStyle();
            if(Integer.toHexString(tfosDefault.getColorCode().getRGB()).substring(2).equals("000000")){
                // light mode workaround
                tfosDefault.setColorCode(Color.decode("#010101"));
            }else{
                // dark mode workaround
                tfosDefault.setColorCode(Color.decode("#bcbcbc"));
            }

            updateByTabFeaturesObjectStyle(tfosDefault, ignoreHasChanges);
        }
    }

    public boolean isCurrentTitleUnique(boolean isCaseSensitive) {
        boolean result = true;
        String currentTabTitle = getTabTitle();
        if (cachedTabTitles == null || !titleHistory.get(titleHistory.size() - 1).equals(currentTabTitle)) {
            refreshLocalTitleCache(isCaseSensitive);
            addTitleHistory(currentTabTitle, true);
        }

        if(!isCaseSensitive) {
            currentTabTitle = currentTabTitle.toLowerCase();
        }
        if (Collections.frequency(cachedTabTitles, currentTabTitle) > 1)
            result = false;

        return result;
    }

    public boolean isNewTitleUnique(String newTitle, boolean isCaseSensitive) {
        boolean result = true;

        if (cachedTabTitles == null || !titleHistory.get(titleHistory.size() - 1).equals(getTabTitle())) {
            cachedTabTitles = new ArrayList<>();
            for (int index = 0; index < parentTabbedPane.getTabCount() - 1; index++) {
                if(isCaseSensitive){
                    cachedTabTitles.add(parentTabbedPane.getTitleAt(index));
                }else{
                    cachedTabTitles.add(parentTabbedPane.getTitleAt(index).toLowerCase());
                }
            }
        }

        if(!isCaseSensitive){
            newTitle = newTitle.toLowerCase();
        }

        if (Collections.frequency(cachedTabTitles, newTitle) > 0)
            result = false;

        return result;
    }

    public int getTabIndex() {
        int subTabIndex = parentTabbedPane.indexOfTabComponent(currentTabContainer);

        if(isDotDotDotTab()){
            subTabIndex = parentTabbedPane.getTabCount() - 1;
        }

        if (tabIndexHistory.size() == 0 || subTabIndex != tabIndexHistory.get(tabIndexHistory.size() - 1)) {
            tabIndexHistory.add(subTabIndex);
        }

        return subTabIndex;
    }

    public String[] getTitleHistory() {
        //return new LinkedHashSet<>(Lists.reverse(titleHistory)).toArray(new String[titleHistory.size()]);
        if(titleHistory==null || titleHistory.size() <= 0)
            titleHistory.add(getTabTitle());

        String[] result = titleHistory.toArray(new String[titleHistory.size()]);
        return result;
    }


    public void setTitleHistory(String[] titles) {
        if(titles==null || titles.length <= 0)
            titles = new String[]{getTabTitle()};

        titleHistory = new ArrayList<String>(Arrays.asList(titles));
    }

    public void addTitleHistory(String title, boolean shouldUpdateSharedParameters){
        if(titleHistory.indexOf(title) >=0)
            titleHistory.remove(title);

        titleHistory.add(title);

        if(shouldUpdateSharedParameters){
            ArrayList<SubTabContainerHandler> subTabContainerHandlers = sharedParameters.allSubTabContainerHandlers.get(currentToolTab);
            int currentIndex = subTabContainerHandlers.indexOf(instance);
            if(currentIndex>=0)
                subTabContainerHandlers.get(currentIndex).addTitleHistory(title, false);
        }
    }

    public String getTabTitle() {
        String title = "";
        if(getTabIndex() != -1)
            title = parentTabbedPane.getTitleAt(getTabIndex());

        return title;
    }

    public void setTabTitle(String title, boolean ignoreHasChanges) {
        if (isValid() && !title.isEmpty() && !getTabTitle().equals(title)) {
            if(!ignoreHasChanges)
                setHasChanges(true);
            title = StringUtils.abbreviate(title, 100);
            addTitleHistory(title, true);
            parentTabbedPane.setTitleAt(getTabIndex(), title);
            refreshLocalTitleCache(false);
        }
    }

    public void refreshLocalTitleCache(boolean isCaseSensitive){
        cachedTabTitles = new ArrayList<>();
        for (int index = 0; index < parentTabbedPane.getTabCount() - 1; index++) {
            if(isCaseSensitive){
                cachedTabTitles.add(parentTabbedPane.getTitleAt(index));
            }else{
                cachedTabTitles.add(parentTabbedPane.getTitleAt(index).toLowerCase());
            }

        }
    }

    public void setFont(Font newFont, boolean ignoreHasChanges) {
        if (isValid() && !getFont().equals(newFont)) {
            if(!ignoreHasChanges)
                setHasChanges(true);
            currentTabLabel.setFont(newFont);
        }
    }

    public Font getFont() {
        return currentTabLabel.getFont();
    }

    public void setFontName(String name, boolean ignoreHasChanges) {
        setFont(new Font(name, getFont().getStyle(), getFont().getSize()),ignoreHasChanges);
    }

    public String getFontName() {
        return getFont().getFamily();
    }

    public void setFontSize(float size, boolean ignoreHasChanges) {
        setFont(getFont().deriveFont(size),ignoreHasChanges);
    }

    public float getFontSize() {
        return getFont().getSize();
    }

    public void toggleBold(boolean ignoreHasChanges) {
        setFont(getFont().deriveFont(getFont().getStyle() ^ Font.BOLD), ignoreHasChanges);
    }

    public void setBold(boolean shouldBeBold, boolean ignoreHasChanges) {
        if (shouldBeBold && !isBold()) {
            toggleBold(ignoreHasChanges);
        } else if (!shouldBeBold && isBold()) {
            toggleBold(ignoreHasChanges);
        }
    }

    public boolean isBold() {
        return getFont().isBold();
    }

    public void toggleItalic(boolean ignoreHasChanges) {
        setFont(getFont().deriveFont(getFont().getStyle() ^ Font.ITALIC),ignoreHasChanges);
    }

    public void setItalic(boolean shouldBeItalic, boolean ignoreHasChanges) {
        if (shouldBeItalic && !isItalic()) {
            toggleItalic(ignoreHasChanges);
        } else if (!shouldBeItalic && isItalic()) {
            toggleItalic(ignoreHasChanges);
        }
    }

    public boolean isItalic() {
        return getFont().isItalic();
    }

    public Color getColor() {
        return currentTabLabel.getForeground();
    }

    public void setColor(Color color, boolean ignoreHasChanges) {
        if (isValid() && !getColor().equals(color)) {
            isFromSetColor = true;
            if(!ignoreHasChanges)
                setHasChanges(true);
            parentTabbedPane.setBackgroundAt(getTabIndex(), color);
        }
    }

    public void showCloseButton(boolean ignoreHasChanges) {
        if (isValid() && !currentTabCloseButton.isVisible()) {
            if(!ignoreHasChanges)
                setHasChanges(true);
            currentTabCloseButton.setVisible(true);
        }
    }

    public void hideCloseButton(boolean ignoreHasChanges) {
        if (isValid() && currentTabCloseButton.isVisible()) {
            if(!ignoreHasChanges)
                setHasChanges(true);
            currentTabCloseButton.setVisible(false);
        }
    }

    public void setVisibleCloseButton(boolean isVisible, boolean ignoreHasChanges) {
        if (isVisible) {
            showCloseButton(ignoreHasChanges);
        } else {
            hideCloseButton(ignoreHasChanges);
        }
    }

    public boolean getVisibleCloseButton() {
        if (!isValid()) {
            return true;
        }
        return currentTabCloseButton.isVisible();
    }

    public Boolean getVisible() {
        return _isVisible;
    }

    public void setVisible(Boolean visible) {
        if(visible != getVisible()){
            if(!visible) {
                originalTabColor = getColor();
                currentTabContainer.setPreferredSize(new Dimension(0,getCurrentDimension().height));
            }else{
                currentTabContainer.setPreferredSize(null);
                setColor(originalTabColor, true);
            }
            parentTabbedPane.setEnabledAt(getTabIndex(),visible);
            currentTabContainer.repaint();
            currentTabContainer.revalidate();
            _isVisible = visible;
            setHasChanges(false);
        }
    }

    public Dimension getCurrentDimension() {
        return currentTabContainer.getPreferredSize();
    }

    public Boolean getHasChanges() {
        if(!getVisible())
            setHasChanges(false);
        return _hasChanges;
    }

    public void setHasChanges(Boolean hasChanges) {
        this._hasChanges = hasChanges;
    }

    @Override
    public boolean equals(Object o) {
        boolean result = false;
        if (isValid()) {
            if (o instanceof SubTabContainerHandler) {
                SubTabContainerHandler temp = (SubTabContainerHandler) o;
                if (temp.currentTabContainer!=null)
                    result = temp.currentTabContainer.equals(this.currentTabContainer);
            } else if (o instanceof Container) {
                Container temp = (Container) o;
                result = temp.equals(this.currentTabContainer);
            }
        } else {
            if (o instanceof SubTabContainerHandler) {
                SubTabContainerHandler temp = (SubTabContainerHandler) o;
                if(temp.tabIndexHistory.size() != 0 && this.tabIndexHistory.size() !=0)
                    result = temp.tabIndexHistory.get(temp.tabIndexHistory.size() - 1).equals(this.tabIndexHistory.get(this.tabIndexHistory.size() - 1));
            }
        }
        return result;
    }

}