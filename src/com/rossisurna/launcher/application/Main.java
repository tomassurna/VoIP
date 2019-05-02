package com.rossisurna.launcher.application;

import com.rossisurna.client.application.Client;
import com.rossisurna.launcher.ui.LauncherController;
import com.rossisurna.server.application.Server;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * Network Programming Spring 2019 Final Project
 * <p>
 * Client Method for entire application and Launcher Object
 *
 * @author Stephen R
 * @author Tomas S
 * @version 1.0
 * @since Mar 18 2019
 */


// This class is known as "Server Home" To Devs
public class Main extends Application {

    //Util
    private LauncherController launcherController; // controller for the launcher UI
    private boolean isServerRunning = true; //boolean if the server is running
    private Server server; //Server object

    /**
     * Runs start method to launch UI for Launcher
     *
     * @param args command line args
     */
    public static void main(String[] args) {
        //Launches UI
        launch(args);
    }

    /**
     * Launches Launcher's UI
     *
     * @param stage javaFX stage
     * @throws Exception javaFX error
     */
    @Override
    public void start(Stage stage) throws Exception {
        // Initializes loader and assigns to the main UI
        FXMLLoader loader = new FXMLLoader(this.getClass().getResource("../ui/LauncherUI.fxml"));

        // JavaFX Controller, assigns mainController for the program
        launcherController = new LauncherController();
        loader.setController(launcherController);
        Parent root = loader.load();

        // sets up scene
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Application Launcher");
        stage.show();

        Label threadCountLabel = launcherController.getThreadCountLabel();

        //Launches new thread that updates the number of thrwad count in the UI
        new Thread(() -> {
            int threadCount = 0;
            while (true) {
                if (Thread.activeCount() != threadCount) {
                    threadCount = Thread.activeCount();
                    Platform.runLater(() -> {
                        threadCountLabel.setText(String.valueOf(Thread.activeCount()));
                        Thread.currentThread().interrupt();
                    });
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        //Event handler for when stage is closing, shut down everything
        stage.setOnCloseRequest(event -> {
            System.out.println("Stage is closing");
            System.exit(0);
        });

        //UI Buttons
        Button launchClientButton = launcherController.getLaunchClientButton();
        Button launchServerButton = launcherController.getLaunchServerButton();

        //Launches client on button press
        launchClientButton.setOnAction(e -> {
            Client client = new Client();
            new Thread(client).start();
        });

        //Launches or closes server on button press
        Server serverHome = new Server();
        new Thread(serverHome).start();
        server = serverHome;

        //launch server button event handler
        launchServerButton.setOnAction(e -> {
            //if server is running then close and update text in button
            if (isServerRunning) {
                server.close();
                launchServerButton.setText("Start Server");
                isServerRunning = false;
            } else {
                //else launch server and update text in button
                server = new Server();
                new Thread(server).start();
                launchServerButton.setText("Stop Server");
                isServerRunning = true;
            }
        });
    }


}
