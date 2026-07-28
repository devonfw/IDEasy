package com.devonfw.ide.gui.tray;

import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.io.InputStream;

/**
 * Utility service to show tray notifications. Extracted to avoid duplicated logic.
 */
public final class TrayNotificationService {

  private static final String TRAY_ICON_RESOURCE = "/com/devonfw/ide/gui/assets/devonfw.png";

  private TrayNotificationService() {
    // utility
  }

  public static void show(String caption, String text, Runnable onClick) {
    try {
      if (GraphicsEnvironment.isHeadless()) {
        return;
      }
      if (!SystemTray.isSupported()) {
        return;
      }

      SystemTray tray = SystemTray.getSystemTray();
      Image image = loadTrayImage();
      TrayIcon trayIcon = new TrayIcon(image, "IDEasy");
      trayIcon.setImageAutoSize(true);

      if (onClick != null) {
        trayIcon.addActionListener(_e -> {
          try {
            onClick.run();
          } catch (Throwable t) {
            // ignore listener exceptions
          }
        });
      }

      tray.add(trayIcon);
      trayIcon.displayMessage(caption, text, TrayIcon.MessageType.INFO);

      // cleanup after delay
      Thread cleanup = new Thread(() -> {
        try {
          Thread.sleep(8000L);
        } catch (InterruptedException ignored) {
          Thread.currentThread().interrupt();
        }
        try {
          tray.remove(trayIcon);
        } catch (Exception ignored) {
        }
      }, "ide-gui-tray-cleanup");
      cleanup.setDaemon(true);
      cleanup.start();
    } catch (Throwable t) {
      // best-effort only; swallow errors
    }
  }

  private static Image loadTrayImage() {
    Image image = null;
    try (InputStream in = TrayNotificationService.class.getResourceAsStream(TRAY_ICON_RESOURCE)) {
      if (in != null) {
        image = Toolkit.getDefaultToolkit().createImage(in.readAllBytes());
      }
    } catch (Exception e) {
      // ignore
    }
    if (image == null) {
      image = Toolkit.getDefaultToolkit().createImage(new byte[0]);
    }
    return image;
  }
}


