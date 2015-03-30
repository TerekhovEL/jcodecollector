/*
 * Copyright 2006-2013 Alessandro Cocco.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jcodecollector;

import jcodecollector.exceptions.SystemExitException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import jcodecollector.exceptions.ConnectionException;

import jcodecollector.data.DBMS;
import jcodecollector.exceptions.DirectoryCreationException;
import jcodecollector.data.settings.ApplicationSettings;
import jcodecollector.data.settings.ApplicationSettingsManager;
import jcodecollector.gui.MainFrame;
import jcodecollector.util.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Loader {
    private static final Logger logger = LoggerFactory.getLogger(Loader.class);
    public static final DBMS DBMS_INSTANCE;
    static {
        try {
            DBMS_INSTANCE = DBMS.getInstance();
        } catch (ClassNotFoundException ex) {
            String message = "An error occurred while loading dbms driver.";
            logger.error(message, ex);
            displayErrorMessageDialog(message);
            throw new SystemExitException(1);
        }
    }

    public static void main(String[] args) {
        try {
            if (OS.isMacOSX()) {
                System.setProperty("apple.laf.useScreenMenuBar", "true");
            } else if (OS.isWindows()) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } else if (OS.isLinux()) {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            }
        } catch (ClassNotFoundException ex) {
            logger.warn("error loading look and feel - using default.", ex);
        } catch (IllegalAccessException ex) {
            logger.warn("error loading look and feel - using default.", ex);
        } catch (InstantiationException ex) {
            logger.warn("error loading look and feel - using default.", ex);
        } catch (UnsupportedLookAndFeelException ex) {
            logger.warn("error loading look and feel - using default.", ex);
        }

        // carica i settaggi dell'applicazione
        ApplicationSettingsManager.readApplicationSettings();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    MainFrame mainFrame = new MainFrame();
                    mainFrame.setVisible(true);

                    if (OS.isMacOSX()) {
                        // forzo il ridisegno dell'interfaccia: e' un piccolo fix
                        // per il problema della bottom bar che appare del colore
                        // sbagliato
                        mainFrame.repaint();
                    }

                    // carico il source list con gli snippet
                    mainFrame.reloadSourceList();
                    mainFrame.restoreSelectedSnippet();
                } catch(ConnectionException ex) {
                    String message = "Cannot start jCodeCollector because an error occurred.";
                    String text = String.format("<html><b>%s (%s)</b><br><br><font size=-1>", message, ex.getMessage());
                    logger.debug(message, ex);
                    if (message.contains("not found")) {
                        text += "JCODECOLLECTOR_DB folder cannot be found in <i>"
                                + ApplicationSettings.getInstance().getDatabasePath()
                                + "jCodeCollector</i>";
                    } else {
                        text += "Only one client at time can access to the database.";
                    }
                    text += "</font></html>";

                    JOptionPane.showMessageDialog(null, text, "", JOptionPane.ERROR_MESSAGE);
                    System.exit(3);
                } catch(DirectoryCreationException ex) {
                    displayErrorMessageDialog(ex.getMessage());
                    System.exit(2);
                }
            }
        });
    }

    private static void displayErrorMessageDialog(String message) {
        String text = String.format("<html><b>%s</b><br><br><font size=-1>", message);
        text += "Click OK and try again.";
        text += "</font></html>";
        JOptionPane.showMessageDialog(null, text, "", JOptionPane.ERROR_MESSAGE);
    }

}
