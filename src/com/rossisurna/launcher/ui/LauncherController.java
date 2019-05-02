package com.rossisurna.launcher.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Network Programming Spring 2019 Final Project
 * <p>
 * Launcher's UI Controller
 *
 * @author Stephen R
 * @author Tomas S
 * @version 1.0
 * @since Mar 18 2019
 */

public class LauncherController {

    @FXML
    private Button launchServerButton; //Launch server button
    @FXML
    private Button launchClientButton; //Launch client button
    @FXML
    private Label threadCountLabel; //thread count label

    /**
     * Constructor
     */
    public LauncherController() {
    }

    //GETTERS

    /**
     * Get launch server button
     *
     * @return launch server Button
     */
    public Button getLaunchServerButton() {
        return launchServerButton;
    }

    /**
     * Get launch client button
     *
     * @return launch client button
     */
    public Button getLaunchClientButton() {
        return launchClientButton;
    }

    /**
     * Get thread count label
     *
     * @return thread count labels
     */
    public Label getThreadCountLabel() {
        return threadCountLabel;
    }
}