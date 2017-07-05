package de.bioforscher.singa.simulation.gui.components.entities;

import de.bioforscher.singa.chemistry.descriptive.entities.ChemicalEntity;
import de.bioforscher.singa.simulation.gui.renderer.ColorManager;
import javafx.scene.paint.Color;

/**
 * @author cl
 */
public class ColorableChemicalEntity {

    private ChemicalEntity entity;
    private Color color;
    private boolean visible;

    public ColorableChemicalEntity(ChemicalEntity entity) {
        this(entity, ColorManager.generateRandomColor());
    }

    public ColorableChemicalEntity(ChemicalEntity entity, Color color) {
        this.entity = entity;
        this.color = color;
        this.visible = true;
    }

    public ChemicalEntity getEntity() {
        return this.entity;
    }

    public void setEntity(ChemicalEntity entity) {
        this.entity = entity;
    }

    public Color getColor() {
        return this.color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

}
