package de.bioforscher.singa.simulation.gui.components.menus;

import de.bioforscher.singa.chemistry.descriptive.entities.ChemicalEntity;
import de.bioforscher.singa.features.identifiers.SimpleStringIdentifier;
import de.bioforscher.singa.simulation.gui.components.panes.SimulationCanvas;
import de.bioforscher.singa.simulation.gui.renderer.RenderingMode;
import de.bioforscher.singa.simulation.modules.model.Simulation;
import javafx.event.ActionEvent;
import javafx.scene.control.*;

import java.util.Map;
import java.util.Map.Entry;

/**
 * The class represents a context menu that is able to manipulate the graph and
 * its rendering.
 *
 * @author cl
 */
public class AutomatonContextMenu extends ContextMenu {

    private final SimulationCanvas owner;
    private Simulation simulation;

    private MenuItem colorByStateItem = new MenuItem();
    private MenuItem colorByCompartment = new MenuItem();
    private Menu colorByChemicalEntityMenu;
    private ToggleGroup chemicalEntitiesGrouping;

    public AutomatonContextMenu(Simulation simulation, SimulationCanvas canvas) {
        this.simulation = simulation;
        this.owner = canvas;
        configureColorByStateItem();
        configureColorByCompartmentItem();
        configureColorByChemicalEntityMenu();
        addItemsToMenu();
    }

    private void configureColorByCompartmentItem() {
        this.colorByCompartment.setText("Color by compartment");
        this.colorByCompartment.setOnAction(this::colorByCompartment);
    }

    private void configureColorByStateItem() {
        this.colorByStateItem.setText("Color by state");
        this.colorByStateItem.setOnAction(this::colorByState);
    }

    private void configureColorByChemicalEntityMenu() {
        this.colorByChemicalEntityMenu = new Menu("Color by entity ...");
        this.chemicalEntitiesGrouping = new ToggleGroup();
        Map<SimpleStringIdentifier, ChemicalEntity> chemicalEntities = this.simulation.getChemicalEntityMap();
        // add MenuItem for every Species
        if (!chemicalEntities.isEmpty()) {
            fillSpeciesMenu(chemicalEntities);
        } else {
            RadioMenuItem itemCompound = new RadioMenuItem("No entity to highlight.");
            itemCompound.setUserData(null);
            itemCompound.setToggleGroup(this.chemicalEntitiesGrouping);
            this.colorByChemicalEntityMenu.getItems().add(itemCompound);
        }
    }

    private void fillSpeciesMenu(Map<SimpleStringIdentifier, ChemicalEntity> speciesMap) {
        for (Entry<SimpleStringIdentifier, ChemicalEntity> species : speciesMap.entrySet()) {
            RadioMenuItem speciesMenuItem = setupSpeciesMenuItem(species.getValue());
            this.colorByChemicalEntityMenu.getItems().add(speciesMenuItem);
        }
    }

    private RadioMenuItem setupSpeciesMenuItem(final ChemicalEntity species) {
        RadioMenuItem itemCompound = new RadioMenuItem(species.getName());
        itemCompound.setUserData(species);
        itemCompound.setToggleGroup(this.chemicalEntitiesGrouping);
        itemCompound.setOnAction(this::colorBySpecies);
        return itemCompound;
    }

    private void addItemsToMenu() {
        this.getItems().addAll(this.colorByStateItem, this.colorByCompartment, this.colorByChemicalEntityMenu);
    }

    private void colorBySpecies(ActionEvent event) {
        ChemicalEntity chemicalEntity = (ChemicalEntity)((RadioMenuItem)event.getSource()).getUserData();
        this.owner.getRenderer().getBioRenderingOptions().setRenderingMode(RenderingMode.ENTITY_BASED);
        this.owner.getRenderer().getBioRenderingOptions().setNodeHighlightEntity(chemicalEntity);
        this.owner.getRenderer().getBioRenderingOptions().setEdgeHighlightEntity(chemicalEntity);
        this.owner.draw();
    }

    private void colorByState(ActionEvent event) {
        this.owner.getRenderer().getBioRenderingOptions().setRenderingMode(RenderingMode.STATE_BASED);
        this.owner.draw();
    }

    private void colorByCompartment(ActionEvent event) {
        this.owner.getRenderer().getBioRenderingOptions().setRenderingMode(RenderingMode.COMPARTMENT_BASED);
        this.owner.draw();
    }

    public Simulation getSimulation() {
        return this.simulation;
    }

    public void setSimulation(Simulation simulation) {
        this.simulation = simulation;
    }

}
