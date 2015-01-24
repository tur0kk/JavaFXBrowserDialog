package com.tur0kk.thingiverse.fxgui;

/*
 * BrowserLoginDialog.java
 *
 * Created on 23.12.2014, 22:55:29
 */

// We use JavaFX for its browser control.
// This requires a recent Java version, e.g. Java 8.
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.net.URL;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import static javafx.concurrent.Worker.State.FAILED;

/**
 * @author Patrick Schmidt
 */
public class BrowserLoginDialog extends javax.swing.JDialog
{
  // JavaFX
  private final JFXPanel jfxPanel = new JFXPanel();
  private WebEngine webEngine;

  // Swing
  private final JPanel swingPanel = new JPanel(new BorderLayout());
  private final JLabel lblStatus = new JLabel();
  private final JProgressBar progressBar = new JProgressBar();
  
  private String browserCode = null;
  
  public String getBrowserCode()
  {
    return browserCode;
  }
  
  public BrowserLoginDialog(java.awt.Frame parent, boolean modal, String title, String url, String redirectUrlPrefix)
  {
    super(parent, modal);
    initComponents(title, redirectUrlPrefix);

    loadURL(url);
  }

  private void initComponents(String title, String redirectUrlPrefix)
  {
    // For JavaFX/Swing interop see:
    // http://docs.oracle.com/javafx/2/swing/SimpleSwingBrowser.java.htm
    createScene(redirectUrlPrefix);

    progressBar.setPreferredSize(new Dimension(1000, 18));
    progressBar.setStringPainted(true);

    JPanel statusBar = new JPanel(new BorderLayout(5, 0));
    statusBar.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
    statusBar.add(lblStatus, BorderLayout.CENTER);
    statusBar.add(progressBar, BorderLayout.CENTER);

    swingPanel.add(jfxPanel, BorderLayout.CENTER);
    swingPanel.add(statusBar, BorderLayout.SOUTH);

    getContentPane().add(swingPanel);
    pack();

    setTitle(title);
    setSize(1024, 600);
    setLocationRelativeTo(null);
  }

  private void createScene(String redirectUrlPrefix)
  {
    Platform.runLater(new Runnable()
    {
      String redirectUrlPrefix_;
      
      public Runnable init(String redirectUrlPrefix)
      {
          this.redirectUrlPrefix_ = redirectUrlPrefix;
          return this;
      }
        
      @Override
      public void run()
      {
        WebView view = new WebView();
        
        // Delete all cookies!
        CookieManager manager = new CookieManager();
        CookieHandler.setDefault(manager);
        manager.getCookieStore().removeAll();
        
        webEngine = view.getEngine();

        // Update status label
        webEngine.setOnStatusChanged(new EventHandler<WebEvent<String>>()
        {
          @Override
          public void handle(final WebEvent<String> event)
          {
            SwingUtilities.invokeLater(new Runnable()
            {
              @Override
              public void run()
              {
                lblStatus.setText(event.getData());
              }
            });
          }
        });

        // Update progress bar
        webEngine.getLoadWorker().workDoneProperty().addListener(new ChangeListener<Number>()
        {
          @Override
          public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, final Number newValue)
          {
            SwingUtilities.invokeLater(new Runnable()
            {
              @Override
              public void run()
              {
                progressBar.setValue(newValue.intValue());
              }
            });
          }
        });
        
        // Close dialog on success
        webEngine.locationProperty().addListener(new ChangeListener<String>()
        {
          String redirectUrlPrefix__;
          
          public ChangeListener init(String redirectUrlPrefix)
          {
              this.redirectUrlPrefix__ = redirectUrlPrefix;
              return this;
          }
            
          @Override
          public void changed(ObservableValue<? extends String> ov, String oldValue, final String newValue)
          {
            SwingUtilities.invokeLater(new Runnable()
            {
              @Override
              public void run()
              {
                if (newValue.startsWith(redirectUrlPrefix__))
                {
                  browserCode = newValue.substring(redirectUrlPrefix__.length());
                  
                  // Close dialog
                  BrowserLoginDialog.this.dispose();
                }
              }
            });
          }
        }.init(this.redirectUrlPrefix_));

        // Handle exceptions
        webEngine.getLoadWorker()
          .exceptionProperty()
          .addListener(new ChangeListener<Throwable>()
            {
              public void changed(ObservableValue<? extends Throwable> o, Throwable old, final Throwable value)
              {
                if (webEngine.getLoadWorker().getState() == FAILED)
                {
                  SwingUtilities.invokeLater(new Runnable()
                    {
                      @Override
                      public void run()
                      {
                        JOptionPane.showMessageDialog(
                          swingPanel,
                          (value != null)
                            ? webEngine.getLocation() + "\n" + value.getMessage()
                            : webEngine.getLocation() + "\nUnexpected error.",
                          "Loading error...",
                          JOptionPane.ERROR_MESSAGE);
                      }
                  });
                }
              }
          });

        jfxPanel.setScene(new Scene(view));
      }
    }.init(redirectUrlPrefix));
  }

  private void loadURL(final String url)
  {
    Platform.runLater(new Runnable()
    {
      @Override
      public void run()
      {
        String tmp = toURL(url);

        if (tmp == null)
        {
          tmp = toURL("http://" + url);
        }

        webEngine.load(tmp);
      }
    });
  }

  private static String toURL(String str)
  {
    try
    {
      return new URL(str).toExternalForm();
    }
    catch (MalformedURLException exception)
    {
      return null;
    }
  }
}