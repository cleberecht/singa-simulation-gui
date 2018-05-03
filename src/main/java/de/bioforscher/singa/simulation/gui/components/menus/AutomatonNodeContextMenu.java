package de.bioforscher.singa.simulation.gui.components.menus;

import de.bioforscher.singa.simulation.gui.CellularGraphAutomatonSimulation;
import de.bioforscher.singa.simulation.gui.components.cards.PlotCard;
import de.bioforscher.singa.simulation.gui.components.plots.ConcentrationPlot;
import de.bioforscher.singa.simulation.model.compartments.CellSectionState;
import de.bioforscher.singa.simulation.model.graphs.AutomatonNode;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutomatonNodeContextMenu extends ContextMenu {

    private static final Logger logger = LoggerFactory.getLogger(AutomatonNodeContextMenu.class);

    private final CellularGraphAutomatonSimulation owner;
    private AutomatonNode node;

    private CustomMenuItem header = new CustomMenuItem();
    private MenuItem delete = new MenuItem();
    private MenuItem observe = new MenuItem();
    private Menu stateMenu;
    private ToggleGroup stateGroup;

    public AutomatonNodeContextMenu(AutomatonNode node, CellularGraphAutomatonSimulation owner) {
        this.node = node;
        this.owner = owner;
        configureHeader();
        configureStatesMenu();
        configureDeleteItem();
        configureObserve();
        addItemsToMenu();
    }

    private void configureHeader() {
        Text headerText = new Text(this.node.toString());
        headerText.setStyle("-fx-font-weight: bold;");
        this.header.setContent(headerText);
    }

    private void configureDeleteItem() {
        this.delete.setText("Delete");
        this.delete.setOnAction(this::deleteNode);
    }

    private void configureObserve() {
        this.observe.setText("Observe");
        this.observe.setOnAction(this::observeNode);
    }

    private void configureStatesMenu() {
        this.stateMenu = new Menu("Set State...");
        this.stateGroup = new ToggleGroup();
        // add menuItem for every state
        for (CellSectionState state: CellSectionState.values()) {
            RadioMenuItem stateItem = setupStateMenuItem(state);
            this.stateMenu.getItems().add(stateItem);
        }
    }

    private RadioMenuItem setupStateMenuItem(final CellSectionState state) {
        RadioMenuItem itemCompound = new RadioMenuItem();
        itemCompound.setText(state.name());
        itemCompound.setUserData(state);
        itemCompound.setToggleGroup(this.stateGroup);
        itemCompound.setOnAction(this::setState);
        return itemCompound;
    }

    private void addItemsToMenu() {
        this.getItems().addAll(this.header, this.delete, this.observe, this.stateMenu);
    }

    private void deleteNode(ActionEvent event) {
        logger.debug("Removing node {} from currently displayed graph ...",this.node.getIdentifier());
        this.owner.getGraph().removeNode(this.node.getIdentifier());
        this.owner.redrawGraph();
    }

    private void observeNode(ActionEvent event) {
        owner.getSimulation().observeNode(node);
        ConcentrationPlot plot = new ConcentrationPlot(this.owner.getSimulationManager().getSimulation().getChemicalEntities(), this.node);
        this.owner.getSimulationManager().addNodeUpdateListener(plot);
        this.owner.getPlotControlPanel().getPlotCards().add(new PlotCard(this.owner.getSimulationManager(), plot));
        this.owner.redrawGraph();
    }

    private void setState(ActionEvent event) {
        this.node.setState(((CellSectionState)((RadioMenuItem)event.getSource()).getUserData()));
        this.owner.redrawGraph();
    }

    public AutomatonNode getNode() {
        return this.node;
    }

    public void setNode(AutomatonNode node) {
        this.node = node;
    }

}
