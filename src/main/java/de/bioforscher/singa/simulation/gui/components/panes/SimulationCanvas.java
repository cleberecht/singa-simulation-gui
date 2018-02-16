package de.bioforscher.singa.simulation.gui.components.panes;

import de.bioforscher.singa.chemistry.descriptive.entities.ChemicalEntity;
import de.bioforscher.singa.javafx.renderer.graphs.GraphCanvas;
import de.bioforscher.singa.javafx.renderer.graphs.GraphRenderer;
import de.bioforscher.singa.mathematics.geometry.faces.Rectangle;
import de.bioforscher.singa.mathematics.graphs.model.Node;
import de.bioforscher.singa.mathematics.vectors.Vector2D;
import de.bioforscher.singa.simulation.gui.CellularGraphAutomatonSimulation;
import de.bioforscher.singa.simulation.gui.components.menus.AutomatonContextMenu;
import de.bioforscher.singa.simulation.gui.components.menus.AutomatonNodeContextMenu;
import de.bioforscher.singa.simulation.gui.renderer.AutomatonGraphRenderer;
import de.bioforscher.singa.simulation.model.compartments.CellSection;
import de.bioforscher.singa.simulation.model.compartments.EnclosedCompartment;
import de.bioforscher.singa.simulation.model.graphs.AutomatonGraph;
import de.bioforscher.singa.simulation.model.graphs.AutomatonNode;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class SimulationCanvas extends BorderPane {

    private CellularGraphAutomatonSimulation owner;
    private GraphCanvas graphCanvas;
    private AutomatonGraph graph;

    private AutomatonGraphRenderer renderer;
    private AutomatonContextMenu graphContextMenu;

    private Vector2D dragStart;

    public SimulationCanvas(CellularGraphAutomatonSimulation owner) {
        this.owner = owner;
        graph = owner.getGraph();
        renderer = new AutomatonGraphRenderer(graph);
        graphContextMenu = new AutomatonContextMenu(this.owner.getSimulation(), this);

        // top part
        VBox topContainer = new VBox();
        final MenuBar menuBar = prepareMenus();
        final ToolBar toolBar = prepareViewingToolBar();
        topContainer.getChildren().addAll(menuBar, toolBar);
        setTop(topContainer);

        // center part
        graphCanvas = new GraphCanvas();
        renderer.renderVoronoi(true);
        setCenter(graphCanvas);

        graphCanvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleClick);
        graphCanvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleDrag);
        graphCanvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleDrag);
        graphCanvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleDrag);

        renderer.drawingWidthProperty().bind(graphCanvas.widthProperty());
        renderer.drawingHeightProperty().bind(graphCanvas.heightProperty());
        renderer.setGraphicsContext(graphCanvas.getGraphicsContext2D());

        graphCanvas.widthProperty().bind(widthProperty());
        graphCanvas.heightProperty().bind(heightProperty().subtract(topContainer.heightProperty()));

        graphCanvas.widthProperty().addListener(observable -> renderer.render(graph));
        graphCanvas.heightProperty().addListener(observable -> renderer.render(graph));

    }

    private void handleDrag(MouseEvent event) {
        if (event.getButton().equals(MouseButton.SECONDARY)) {
            if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
                this.dragStart = new Vector2D(event.getX(), event.getY());
            } else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
                this.draw();
                this.renderer.getGraphicsContext().setFill(Color.DARKOLIVEGREEN.deriveColor(1, 1, 1, 0.5));
                this.renderer.drawDraggedRectangle(this.dragStart, new Vector2D(event.getX(), event.getY()));
            } else if (event.getEventType() == MouseEvent.MOUSE_RELEASED) {
                this.renderer.getGraphicsContext().setFill(Color.DARKOLIVEGREEN.deriveColor(1, 1, 1, 0.5));
                Rectangle rectangle = this.renderer.drawDraggedRectangle(this.dragStart, new Vector2D(event.getX(), event.getY()));
                CellSection cellSection = this.owner.getCompartmentControlPanel().getSelectedCellSection();
                if (cellSection != null && cellSection instanceof EnclosedCompartment) {
                    this.owner.getGraph().addNodesToCompartment((EnclosedCompartment) cellSection, rectangle);
                    this.owner.getCompartmentControlPanel().updateData(this.owner.getGraph().getCellSections());
                }
                this.draw();
            }
        } else {
            if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
                dragStart = new Vector2D(event.getX(), event.getY());
                for (AutomatonNode node : graph.getNodes()) {
                    if (isClickedOnNode(event, node)) {
                        graphCanvas.draggedNode = node;
                        break;
                    }
                }
            } else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
                if (graphCanvas.draggedNode != null) {
                    graphCanvas.draggedNode.setPosition(new Vector2D(event.getX(), event.getY()));
                    handleArrangement();
                }
            } else if (event.getEventType() == MouseEvent.MOUSE_RELEASED) {
                graphCanvas.draggedNode = null;
                renderer.render(graph);
            }
        }
    }

    private void handleClick(MouseEvent event) {
        if (event.getButton().equals(MouseButton.SECONDARY)) {
            handleRightClick(event);
        } else if (event.getButton().equals(MouseButton.PRIMARY)) {
            handleLeftClick(event);
        }
    }

    private void handleRightClick(MouseEvent event) {
        boolean isNode = false;
        for (AutomatonNode node : this.owner.getGraph().getNodes()) {
            if (isClickedOnNode(event, node)) {
                AutomatonNodeContextMenu bioNodeContextMenu = new AutomatonNodeContextMenu(node, this.owner);
                bioNodeContextMenu.show(this.owner.getPlotControlPanel(), event.getScreenX(), event.getScreenY());
                isNode = true;
                break;
            }
        }
        if (!isNode) {
            this.graphContextMenu.show(this.owner.getPlotControlPanel(), event.getScreenX(), event.getScreenY());
        }
    }

    private void handleLeftClick(MouseEvent event) {
        for (AutomatonNode node : this.owner.getGraph().getNodes()) {
            if (isClickedOnNode(event, node)) {
                ChemicalEntity species = this.renderer.getBioRenderingOptions().getNodeHighlightEntity();
                node.setConcentration(species, this.owner.getConcentrationSlider().getValue());
                draw();
                break;
            }
        }
    }

    private boolean isClickedOnNode(MouseEvent event, Node<?, Vector2D, ?> node) {
        return node.getPosition().isNearVector(new Vector2D(event.getX() + this.renderer.getRenderingOptions().getNodeDiameter() / 2,
                        event.getY() + this.renderer.getRenderingOptions().getNodeDiameter() / 2),
                this.renderer.getRenderingOptions().getNodeDiameter() / 2);
    }

    public void setRenderer(AutomatonGraphRenderer renderer) {
        this.renderer = renderer;
    }

    public void draw() {
        this.renderer.render(this.owner.getGraph());
    }

    public void handleArrangement() {
        if (renderer.getRenderingMode().equals(GraphRenderer.RenderingMode.LLOYDS_RELAXATION.name())) {
            renderer.relaxOnce(graph);
        } else {
            renderer.arrangeOnce(graph);
        }
    }

    public void resetGraphContextMenu() {
        this.graphContextMenu = new AutomatonContextMenu(this.owner.getSimulation(), this);
    }

    private ToolBar prepareViewingToolBar() {
        ToolBar toolBar = new ToolBar();
        // arrange button
        Button forceDirectedLayout = new Button("Arrange");
        forceDirectedLayout.setOnAction(action -> renderer.arrangeGraph(graph));
        // relax button
        Button relaxLayout = new Button("Relax");
        relaxLayout.setOnAction(action -> renderer.relaxGraph(graph));
        // add items to toolbar
        toolBar.getItems().addAll(forceDirectedLayout, relaxLayout);
        return toolBar;
    }

    private MenuBar prepareMenus() {
        MenuBar menuBar = new MenuBar();

        // file menu
        Menu menuFile = new Menu("File");
        // new graph
        MenuItem mINewGraph = new MenuItem("New graph ...");
        mINewGraph.setAccelerator(new KeyCodeCombination(KeyCode.G, KeyCombination.CONTROL_DOWN));
        // mINewGraph.setOnAction(this::startGraphWizard);
        // open Graph
        MenuItem mILoadBioGraph = new MenuItem("Open graph ...");
        mILoadBioGraph.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        // mILoadBioGraph.setOnAction(this::loadBioGraph);
        // save Graph
        MenuItem mISaveGraph = new MenuItem("Save graph ...");
        mISaveGraph.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        // mISaveGraph.setOnAction(this::saveBioGraph);

        // rendering menu
        Menu menuRendering = new Menu("Rendering");
        // rendering mode
        Menu menuRenderingMode = new Menu("Rendering mode");
        final ToggleGroup groupRenderingMode = new ToggleGroup();
        for (GraphRenderer.RenderingMode renderingMode : GraphRenderer.RenderingMode.values()) {
            RadioMenuItem itemMode = new RadioMenuItem(renderingMode.getDispayText());
            itemMode.setUserData(renderingMode);
            itemMode.setToggleGroup(groupRenderingMode);
            menuRenderingMode.getItems().add(itemMode);
            if (renderingMode.name().equals(renderer.getRenderingMode())) {
                itemMode.setSelected(true);
            }
        }
        groupRenderingMode.selectedToggleProperty().addListener((ov, old_toggle, new_toggle) -> {
            if (groupRenderingMode.getSelectedToggle() != null) {
                renderer.setRenderingMode(((GraphRenderer.RenderingMode) groupRenderingMode.getSelectedToggle().getUserData()).name());
            }
        });
        // voronoi drawing
        CheckMenuItem voronoiItem = new CheckMenuItem("Render Voronoi");
        voronoiItem.setSelected(true);
        voronoiItem.selectedProperty().addListener((ov, old_val, new_val) -> {
            renderer.renderVoronoi(new_val);
            renderer.render(graph);
        });
        // add rendering items
        menuRendering.getItems().addAll(menuRenderingMode, voronoiItem);

        // add items to file menu
        menuFile.getItems().addAll(mINewGraph, mILoadBioGraph, mISaveGraph);
        // add menus to menu bar
        menuBar.getMenus().addAll(menuFile, menuRendering);
        return menuBar;
    }

    public AutomatonGraphRenderer getRenderer() {
        return renderer;
    }

    public AutomatonGraph getGraph() {
        return graph;
    }


}
