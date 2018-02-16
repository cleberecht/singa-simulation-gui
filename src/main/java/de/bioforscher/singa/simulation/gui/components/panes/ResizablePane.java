package de.bioforscher.singa.simulation.gui.components.panes;

import javafx.scene.layout.AnchorPane;

/**
 * @author cl
 */
public class ResizablePane extends AnchorPane {

    private SimulationCanvas canvas;

    public ResizablePane(SimulationCanvas canvas) {
        this.canvas = canvas;
        getChildren().add(canvas);
        canvas.setManaged(false);
    }

    @Override
    public void resize(double width,double height) {
        super.resize(width, height);
        this.canvas.setPrefWidth(width);
        this.canvas.setPrefHeight(height);
    }

}
