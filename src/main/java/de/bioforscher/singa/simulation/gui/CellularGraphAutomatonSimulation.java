package de.bioforscher.singa.simulation.gui;

import de.bioforscher.singa.chemistry.descriptive.entities.ComplexedChemicalEntity;
import de.bioforscher.singa.chemistry.descriptive.entities.Protein;
import de.bioforscher.singa.core.events.UpdateEventListener;
import de.bioforscher.singa.features.identifiers.UniProtIdentifier;
import de.bioforscher.singa.features.parameters.EnvironmentalParameters;
import de.bioforscher.singa.simulation.events.NodeUpdatedEvent;
import de.bioforscher.singa.simulation.gui.components.SimulationIndicator;
import de.bioforscher.singa.simulation.gui.components.controlpanles.CompartmentControlPanel;
import de.bioforscher.singa.simulation.gui.components.controlpanles.EnvironmentalParameterControlPanel;
import de.bioforscher.singa.simulation.gui.components.controlpanles.PlotControlPanel;
import de.bioforscher.singa.simulation.gui.components.panes.PlotPreferencesPane;
import de.bioforscher.singa.simulation.gui.components.panes.SimulationCanvas;
import de.bioforscher.singa.simulation.gui.components.panes.SpeciesOverviewPane;
import de.bioforscher.singa.simulation.gui.wizards.AddSpeciesWizard;
import de.bioforscher.singa.simulation.gui.wizards.NewGraphWizard;
import de.bioforscher.singa.simulation.gui.wizards.NewReactionWizard;
import de.bioforscher.singa.simulation.model.compartments.EnclosedCompartment;
import de.bioforscher.singa.simulation.model.compartments.Membrane;
import de.bioforscher.singa.simulation.model.concentrations.MembraneContainer;
import de.bioforscher.singa.simulation.model.graphs.AutomatonGraph;
import de.bioforscher.singa.simulation.model.graphs.AutomatonGraphs;
import de.bioforscher.singa.simulation.model.graphs.AutomatonNode;
import de.bioforscher.singa.simulation.modules.model.Simulation;
import de.bioforscher.singa.simulation.modules.model.SimulationManager;
import de.bioforscher.singa.simulation.modules.transport.FreeDiffusion;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tec.uom.se.quantity.Quantities;

import java.io.File;
import java.util.concurrent.CopyOnWriteArrayList;

import static de.bioforscher.singa.features.units.UnitProvider.MOLE_PER_LITRE;
import static tec.uom.se.unit.Units.SECOND;

public class CellularGraphAutomatonSimulation extends Application {

    private static final Logger logger = LoggerFactory.getLogger(CellularGraphAutomatonSimulation.class);

    private Stage stage;

    private SimulationCanvas simulationCanvas;
    private PlotControlPanel plotControlPanel;

    private CompartmentControlPanel compartmentControlPanel;
    private Slider concentrationSlider;

    public static Simulation simulation;
    private SimulationManager simulationManager;
    private SimulationIndicator timeIndicator;

    public static void main(String[] args) {
        logger.info("Started simulation GUI.");
        launch();
    }

    @Override
    public void start(Stage stage) throws Exception {
        // setup the simulation
        logger.info("Setting up simulation from example ...");
        if (simulation == null) {
            // simulation = SimulationExamples.createDiffusionAndMembraneTransportExample();
            // simulation = SimulationExamples.createSimulationFromSBML();
            // simulation = SimulationExamples.createIodineMultiReactionExample();
            // simulation = SimulationExamples.createDiffusionModuleExample(10, Quantities.getQuantity(500, NANO(SECOND)));


            // simulation
            simulation = new Simulation();
            // g-protein subunits
            Protein gProteinBeta = new Protein.Builder("G(B)")
                    .name("G protein subunit beta")
                    .additionalIdentifier(new UniProtIdentifier("P62873"))
                    .build();
            Protein gProteinGamma = new Protein.Builder("G(G)")
                    .name("G protein subunit gamma")
                    .additionalIdentifier(new UniProtIdentifier("P63211"))
                    .build();
            // complexed entity
            ComplexedChemicalEntity gProteinBetaGamma = ComplexedChemicalEntity.create("G(BG)")
                    .name("G protein beta gamma complex")
                    .addAssociatedPart(gProteinBeta)
                    .addAssociatedPart(gProteinGamma)
                    .setMembraneAnchored(true)
                    .build();
            // create graph
            AutomatonGraph graph = AutomatonGraphs.createRectangularAutomatonGraph(3, 2);
            simulation.setGraph(graph);
            // sections
            EnclosedCompartment outerSection = new EnclosedCompartment("Ext", "Extracellular region");
            EnclosedCompartment innerSection = new EnclosedCompartment("Cyt", "Cytoplasm");
            Membrane membrane = Membrane.forCompartment(innerSection);
            // distribute nodes to sections
            graph.getNodesOfRow(0).forEach(node -> {
                node.setCellSection(membrane);
                node.setConcentrationContainer(new MembraneContainer(outerSection, innerSection, membrane));
            });
            graph.getNodesOfRow(1).forEach(node -> node.setCellSection(innerSection));
            // reference sections in graph
            graph.addCellSection(outerSection);
            graph.addCellSection(innerSection);
            graph.addCellSection(membrane);
            // add some protein in cytoplasm
//            AutomatonNode node1 = graph.getNode(2, 1);
//            node1.setConcentration(gProteinBetaGamma, Quantities.getQuantity(0.1, MOLE_PER_LITRE)
//                    .to(EnvironmentalParameters.getTransformedMolarConcentration()));
            AutomatonNode node2 = graph.getNode(0, 0);
            node2.setAvailableConcentration(gProteinBetaGamma, innerSection, Quantities.getQuantity(0.1, MOLE_PER_LITRE)
                    .to(EnvironmentalParameters.getTransformedMolarConcentration()));
            // add diffusion
            FreeDiffusion.inSimulation(simulation)
                    .onlyFor(gProteinBetaGamma)
                    .build();


        }
        simulationManager = new SimulationManager(simulation);

        logger.info("Initializing simulation GUI.");
        // Stage
        this.stage = stage;
        this.stage.setMaximized(true);
        this.stage.setTitle("GraphAutomaton Simulation");

        // Setup the Root and Top Container
        BorderPane root = new BorderPane();
        VBox topContainer = new VBox();
        logger.debug("Initializing menus and buttons ...");
        // Menu Bar
        MenuBar menuBar = new MenuBar();

        // File Menu
        Menu menuFile = new Menu("File");

        // New Graph
        MenuItem mINewGraph = new MenuItem("New Graph ...");
        mINewGraph.setAccelerator(new KeyCodeCombination(KeyCode.G, KeyCombination.CONTROL_DOWN));
        mINewGraph.setOnAction(this::startGraphWizard);

        // Open Graph
        MenuItem mILoadBioGraph = new MenuItem("Open BioGraph ...");
        mILoadBioGraph.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        mILoadBioGraph.setOnAction(this::loadBioGraph);

        // Save Graph
        MenuItem mISaveGraph = new MenuItem("Save BioGraph ...");
        mISaveGraph.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        mISaveGraph.setOnAction(this::saveBioGraph);

        // Adding Species
        MenuItem mIAddSpecies = new MenuItem("Add Species ...");
        mIAddSpecies.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN));
        mIAddSpecies.setOnAction(this::showSearchSpeciesPanel);

        // Adding Reactions
        MenuItem mIAddReaction = new MenuItem("Add Reaction ...");
        mIAddReaction.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN));
        mIAddReaction.setOnAction(this::startReactionWizard);

        menuFile.getItems().addAll(mINewGraph, mILoadBioGraph, mISaveGraph, new SeparatorMenuItem(), mIAddSpecies,
                mIAddReaction);

        // Edit Menu
        Menu menuEdit = new Menu("Edit");

        // View Menu
        Menu menuView = new Menu("View");

        // Full screen toggle
        MenuItem mIFullScreen = new MenuItem("Toggle full screen");
        mIFullScreen.setAccelerator(new KeyCodeCombination(KeyCode.F11));
        mIFullScreen.setOnAction(this::toggleFullScreen);

        // Species Overview
        MenuItem mISpeciesOverview = new MenuItem("Species", new ImageView(IconProvider.MOLECULE_ICON_IMAGE));
        mISpeciesOverview.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.ALT_DOWN));
        mISpeciesOverview.setOnAction(this::showSpeciesOverview);

        menuView.getItems().addAll(mIFullScreen, mISpeciesOverview);

        Menu menuPreferences = new Menu("Preferences");

        MenuItem mIPlot = new MenuItem("Plot preferences");
        mIPlot.setAccelerator(new KeyCodeCombination(KeyCode.P, KeyCombination.ALT_DOWN));
        mIPlot.setOnAction(this::showPlotPreferencesControlPanel);

        menuPreferences.getItems().add(mIPlot);
        menuBar.getMenus().addAll(menuFile, menuEdit, menuView, menuPreferences);

        logger.debug("Initializing graphs tab ...");
        // Chart Half
        final TabPane rightPane = new TabPane();
        rightPane.setMinWidth(200);

        this.plotControlPanel = new PlotControlPanel();
        Tab chartTab = new Tab();
        chartTab.setText("Plots");
        chartTab.setClosable(false);
        chartTab.setContent(this.plotControlPanel);
        rightPane.getTabs().add(chartTab);

        this.compartmentControlPanel = new CompartmentControlPanel(simulation);
        Tab compartmentTab = new Tab();
        compartmentTab.setText("Compartments");
        compartmentTab.setClosable(false);
        compartmentTab.setContent(this.compartmentControlPanel);
        rightPane.getTabs().add(compartmentTab);

        logger.debug("Initializing environment tab ...");
        Tab environmentTab = new Tab();
        environmentTab.setText("Environment");
        environmentTab.setClosable(false);
        EnvironmentalParameterControlPanel environmentControlPanel = new EnvironmentalParameterControlPanel();
        environmentControlPanel.setDirtyableText(environmentTab.textProperty());
        environmentControlPanel.update();
        environmentTab.setContent(environmentControlPanel);
        rightPane.getTabs().add(environmentTab);

        logger.debug("Initializing simulation canvas ...");
        this.simulationCanvas = new SimulationCanvas(this);
        this.simulationCanvas.getRenderer().getRenderingOptions().setNodeDiameter(8);
        // ResizablePane anchorPane = new ResizablePane(this.simulationCanvas);
        // Simulation Half
//        AnchorPane.setTopAnchor(this.simulationCanvas, 0.0);
//        AnchorPane.setLeftAnchor(this.simulationCanvas, 0.0);
//        AnchorPane.setBottomAnchor(this.simulationCanvas, 0.0);
//        AnchorPane.setRightAnchor(this.simulationCanvas, 0.0);

        // Main Content Pane
        SplitPane splitPane = new SplitPane(this.simulationCanvas, rightPane);
        splitPane.setDividerPosition(0, 0.4);

        // ToolBar
        ToolBar toolBar = new ToolBar();
        // simulate button
        Button btnSimulate = IconProvider.FontAwesome.createIconButton(IconProvider.FontAwesome.ICON_PLAY);
        btnSimulate.setTooltip(new Tooltip("Starts the simulation with the current configuration."));
        btnSimulate.setOnAction(this::startSimulation);
        // stop button
        Button btnStop = IconProvider.FontAwesome.createIconButton(IconProvider.FontAwesome.ICON_PAUSE);
        btnStop.setTooltip(new Tooltip("Pauses the current simulation."));
        btnStop.setOnAction(this::pauseSimulation);
        // step button
        Button btnStep = IconProvider.FontAwesome.createIconButton(IconProvider.FontAwesome.ICON_STEP_FORWARD);
        btnStep.setTooltip(new Tooltip("Pauses the current simulation."));
        btnStep.setOnAction(this::stepSimulation);
        // Concentration slider
        setupConcentrationSlider();

        // Add toolbar components
        toolBar.getItems().addAll(btnSimulate, btnStop, btnStep, this.concentrationSlider);

        // Add toolbar and menu
        topContainer.getChildren().addAll(menuBar, toolBar);
        root.setTop(topContainer);

        // Simulation Frame
        root.setCenter(splitPane);

        timeIndicator = new SimulationIndicator();

        // Anchor to the Bottom
        root.setBottom(new HBox(new Text("Current Time Step: "), timeIndicator));
        root.bottomProperty().get().minHeight(10);
        // Scene
        Scene scene = new Scene(root);
        stage.setScene(scene);

        // Show
        stage.show();
    }

    private void startSimulation(ActionEvent event) {
        logger.debug("Starting simulation ...");
        if (!this.simulationManager.isRunning()) {
            initializeSimulationManager();
            Thread thread = new Thread(this.simulationManager);
            thread.setDaemon(true);
            thread.start();
            this.simulationCanvas.getRenderer().start();
        }
    }

    private void pauseSimulation(ActionEvent event) {
        logger.debug("Pausing simulation ...");
        if (this.simulationManager.isRunning()) {
            this.simulationManager.cancel();
            this.simulationCanvas.getRenderer().stop();
            initializeSimulationManager();
        }
    }

    private void stepSimulation(ActionEvent event) {
        initializeSimulationManager();
        simulation.nextEpoch();
        simulationManager.emitGraphEvent(simulation);
        for (AutomatonNode automatonNode : simulation.getObservedNodes()) {
            simulationManager.emitNodeEvent(simulation, automatonNode);
        }
    }

    private void initializeSimulationManager() {
        CopyOnWriteArrayList<UpdateEventListener<NodeUpdatedEvent>> nodeListeners = this.simulationManager.getNodeListeners();
        simulationManager = new SimulationManager(simulation);
        simulationManager.setSimulationTerminationToTime(Quantities.getQuantity(1.0, SECOND));
        simulationManager.tieUpdateEmissionToFPS(10);
        nodeListeners.forEach(simulationManager::addNodeUpdateListener);
        simulationManager.addGraphUpdateListener(timeIndicator);
        simulationManager.addGraphUpdateListener(simulationCanvas.getRenderer());
    }

    private Stage prepareUtilityWindow(int width, int height, String title) {
        Stage utilityStage = new Stage();
        utilityStage.initModality(Modality.APPLICATION_MODAL);
        utilityStage.initStyle(StageStyle.UTILITY);
        utilityStage.setTitle(title);
        utilityStage.setX(this.stage.getX() + this.stage.getWidth() / 2 - width / 2);
        utilityStage.setY(this.stage.getY() + this.stage.getHeight() / 2 - height / 2);
        return utilityStage;
    }

    private void showSearchSpeciesPanel(ActionEvent event) {
        int width = 600;
        int height = 530;
        Stage speciesStage = prepareUtilityWindow(width, height, "Search Species...");
        AddSpeciesWizard speciesWizard = new AddSpeciesWizard(speciesStage, this);
        speciesStage.setScene(new Scene(speciesWizard, width, height));
        speciesStage.sizeToScene();
        speciesStage.showAndWait();
        if (speciesWizard.getSpeciesToAdd() != null) {
            speciesWizard.getSpeciesToAdd().forEach(species -> {
                simulation.getChemicalEntities().add(species);
                this.simulationCanvas.resetGraphContextMenu();
            });
        }
    }

    private void showSpeciesOverview(ActionEvent event) {
        int width = 800;
        int height = 600;
        Stage speciesStage = prepareUtilityWindow(width, height, "Species Overview");
        SpeciesOverviewPane speciesOverviewPane = new SpeciesOverviewPane(this);
        speciesStage.setScene(new Scene(speciesOverviewPane, width, height));
        speciesStage.sizeToScene();
        speciesStage.showAndWait();
    }

    private void startGraphWizard(ActionEvent event) {
        int width = 600;
        int height = 300;
        Stage graphStage = prepareUtilityWindow(width, height, "New Graph Wizard");
        NewGraphWizard graphWizard = new NewGraphWizard(graphStage);
        graphStage.setScene(new Scene(graphWizard, width, height));
        graphStage.showAndWait();
        if (graphWizard.getGraph() != null) {
            resetGraph(graphWizard.getGraph());
        }
    }

    private void startReactionWizard(ActionEvent event) {
        int width = 800;
        int height = 600;
        Stage reactionStage = prepareUtilityWindow(width, height, "New Reaction Wizard");
        NewReactionWizard reactionWizard = new NewReactionWizard(reactionStage);
        reactionStage.setScene(new Scene(reactionWizard, width, height));
        reactionStage.showAndWait();
        // TODO migrate to new interface and reaction interface
    }

    private void showPlotPreferencesControlPanel(ActionEvent event) {
        int width = 400;
        int height = 300;
        Stage plotPreferencesStage = prepareUtilityWindow(width, height, "Plot preferences");
        PlotPreferencesPane plotPreferencesControlPanel = new PlotPreferencesPane(plotPreferencesStage);
        plotPreferencesStage.setScene(new Scene(plotPreferencesControlPanel));
        plotPreferencesStage.showAndWait();
    }

    private FileChooser prepareFileChooser(String title, String... extensions) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        for (String extension : extensions) {
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                    extension.toUpperCase() + " files (*." + extension + ")", "*." + extension);
            fileChooser.getExtensionFilters().add(extFilter);
        }
        return fileChooser;
    }

    private void loadBioGraph(ActionEvent event) {
//        FileChooser fileChooser = prepareFileChooser("Load GraphML-File", "xml");
//        File file = fileChooser.showOpenDialog(this.stage);
//        if (file != null) {
//            GraphMLParserService parserService = new GraphMLParserService(file.getPath());
//            AutomatonGraph graph = parserService.parse();
//            resetGraph(graph);
//        }
    }

    private void saveBioGraph(ActionEvent event) {
        FileChooser fileChooser = prepareFileChooser("Save graph to file", "xml");
        File file = fileChooser.showSaveDialog(this.stage);
        if (file != null) {
//            GraphMLExportService.exportGraph(simulation.getGraph(), file);
        }
    }

    private void setupConcentrationSlider() {
        this.concentrationSlider = new Slider();
        this.concentrationSlider.setMin(0);
        this.concentrationSlider.setMax(1);
        this.concentrationSlider.setValue(1);
        this.concentrationSlider.setShowTickLabels(true);
        this.concentrationSlider.setShowTickMarks(true);
        this.concentrationSlider.setMajorTickUnit(0.5);
        this.concentrationSlider.setMinorTickCount(4);
    }

    private void resetGraph(AutomatonGraph graph) {
        simulation.setGraph(graph);
        this.simulationCanvas.getRenderer().getBioRenderingOptions().setNodeHighlightEntity(null);
        this.simulationCanvas.getRenderer().getBioRenderingOptions().setEdgeHighlightEntity(null);
        this.simulationCanvas.resetGraphContextMenu();
        this.simulationCanvas.draw();
    }

    private void resetGraphContextMenu() {
        this.simulationCanvas.resetGraphContextMenu();
    }

    public void redrawGraph() {
        this.simulationCanvas.draw();
    }

    private void toggleFullScreen(ActionEvent event) {
        if (this.stage.isFullScreen()) {
            this.stage.setFullScreen(false);
        } else {
            this.stage.setFullScreen(true);
        }
    }

    public SimulationManager getSimulationManager() {
        return simulationManager;
    }

    public PlotControlPanel getPlotControlPanel() {
        return this.plotControlPanel;
    }

    public void setPlotControlPanel(PlotControlPanel plotControlPanel) {
        this.plotControlPanel = plotControlPanel;
    }

    public CompartmentControlPanel getCompartmentControlPanel() {
        return this.compartmentControlPanel;
    }

    public void setCompartmentControlPanel(CompartmentControlPanel compartmentControlPanel) {
        this.compartmentControlPanel = compartmentControlPanel;
    }

    public Slider getConcentrationSlider() {
        return this.concentrationSlider;
    }

    public void setConcentrationSlider(Slider concentrationSlider) {
        this.concentrationSlider = concentrationSlider;
    }

    public AutomatonGraph getGraph() {
        return simulation.getGraph();
    }

    public void setGraph(AutomatonGraph graph) {
        simulation.setGraph(graph);
    }

    public Simulation getSimulation() {
        return simulation;
    }

}
