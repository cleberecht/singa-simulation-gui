package de.bioforscher.singa.simulation.gui;

import javafx.scene.image.Image;

/**
 * @author cl
 */
public enum AvailableModules {

    FREE_DIFFUSION(IconProvider.DIFFUSION_ICON_IMAGE, "Free Diffusion"),
    CHEMICAL_REACTION(IconProvider.REACTIONS_ICON_IMAGE, "Chemical Reactions");

    private final Image icon;
    private final String representativeName;

    AvailableModules(Image icon, String representativeName) {
        this.icon = icon;
        this.representativeName = representativeName;
    }

    public Image getIcon() {
        return this.icon;
    }

    public String getRepresentativeName() {
        return this.representativeName;
    }

}
