package de.bioforscher.singa.simulation.gui.components.plots;

import de.bioforscher.singa.chemistry.descriptive.entities.ChemicalEntity;
import de.bioforscher.singa.core.events.UpdateEventListener;
import de.bioforscher.singa.features.parameters.EnvironmentalParameters;
import de.bioforscher.singa.simulation.events.NodeUpdatedEvent;
import de.bioforscher.singa.simulation.gui.SingaPreferences;
import de.bioforscher.singa.simulation.gui.renderer.ColorManager;
import de.bioforscher.singa.simulation.model.graphs.AutomatonNode;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Set;

import static de.bioforscher.singa.structure.features.molarmass.MolarMass.GRAM_PER_MOLE;


/**
 * The chart is used for visualization of AutomatonNode concentrations changes over
 * the course of a simulation.
 *
 * @author cl
 */
public class ConcentrationPlot extends LineChart<Number, Number> implements UpdateEventListener<NodeUpdatedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ConcentrationPlot.class);

    private ObservableList<ChemicalEntity<?>> observedEntities = FXCollections.observableArrayList();
    private AutomatonNode referencedNode;

    private int maximalDataPoints;
    private int tickSpacing;
    private boolean scaleXAxis = false;

    public ConcentrationPlot(Set<ChemicalEntity<?>> observedEntities, AutomatonNode referencedNode) {
        super(new NumberAxis(), new NumberAxis());
        logger.debug("Initializing {} for node {} ...", this.getClass().getSimpleName(), referencedNode.getIdentifier());
        this.referencedNode = referencedNode;
        setObservedSpecies(observedEntities);
        initializeData();
        initializePreferences();
        configureChart();
        configureXAxis();
        configureYAxis();
    }

    private void initializePreferences() {
        SingaPreferences preferences = new SingaPreferences();
        this.maximalDataPoints = preferences.preferences
                .getInt(SingaPreferences.Plot.MAXIMAL_DATA_POINTS, SingaPreferences.Plot.MAXIMAL_DATA_POINTS_VALUE);
        this.tickSpacing = preferences.preferences
                .getInt(SingaPreferences.Plot.TICK_SPACING, SingaPreferences.Plot.TICK_SPACING_VALUE);
    }

    private void initializeData() {
        for (ChemicalEntity entity : this.observedEntities) {
            Series<Number, Number> series = new Series<>();
            series.setName(entity.getIdentifier().toString());
            this.getData().add(series);
            ColorManager.getInstance().initializeEntity(entity, ColorManager.generateRandomColor());
            series.getNode().setStyle("-fx-stroke: " +
                    ColorManager.getHexColor(ColorManager.getInstance().getColor(entity)) + " ");
        }
    }

    public void updateColor(ChemicalEntity entity) {
        Series<Number, Number> series = this.getData().stream()
                .filter(s -> s.getName().equals(entity.getIdentifier().toString()))
                .findFirst().get();
        series.getNode().setStyle("-fx-stroke: " +
                ColorManager.getHexColor(ColorManager.getInstance().getColor(entity)) + " ");
    }

    private void configureChart() {
        this.setAnimated(false);
    }

    private void configureXAxis() {
        this.getXAxis().setAutoRanging(false);
        // TODO false for swiping style
        ((NumberAxis) this.getXAxis()).setForceZeroInRange(true);
        ((NumberAxis) this.getXAxis()).setLowerBound(0);
        ((NumberAxis) this.getXAxis()).setUpperBound(this.maximalDataPoints);
        ((NumberAxis) this.getXAxis()).setTickUnit(this.tickSpacing);
        this.getXAxis().setLabel("Time in " + EnvironmentalParameters.getInstance().getTimeStep().getUnit().toString());
        ((NumberAxis) this.getXAxis()).setTickLabelFormatter(new StringConverter<Number>() {

            private NumberFormat formatter = new DecimalFormat("0.000E0");

            @Override
            public String toString(Number object) {
                return this.formatter.format(object.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });
    }

    private void configureYAxis() {
        this.getYAxis().setLabel("Molar concentration in " + GRAM_PER_MOLE.toString());
        ((NumberAxis) this.getYAxis()).setForceZeroInRange(true);
        ((NumberAxis) this.getYAxis()).setLowerBound(0.0);
        ((NumberAxis) this.getYAxis()).setUpperBound(1.0);
        ((NumberAxis) this.getYAxis()).setTickLabelFormatter(new StringConverter<Number>() {

            private NumberFormat formatter = new DecimalFormat("0.000E0");

            @Override
            public String toString(Number object) {
                return this.formatter.format(object.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });
    }

    public void setObservedSpecies(Set<ChemicalEntity<?>> observedSpecies) {
        this.observedEntities.addAll(observedSpecies);
    }

    public void addSpecies(ChemicalEntity entity) {
        this.observedEntities.add(entity);
        Series<Number, Number> series = new Series<>();
        series.setName(entity.getIdentifier().toString());
        this.getData().add(series);
    }

    public void removeSpecies(ChemicalEntity entity) {
        this.observedEntities.remove(entity);
        this.getData().stream()
                .filter(series -> series.getName().equals(entity.getIdentifier().toString()))
                .forEach(this.getData()::remove);
    }

    public void hideSeries(ChemicalEntity entity) {
        this.getData().stream()
                .filter(series -> series.getName().equals(entity.getIdentifier().toString()))
                .forEach(series -> series.getNode().setVisible(false));
    }

    public void showSeries(ChemicalEntity entity) {
        this.getData().stream()
                .filter(series -> series.getName().equals(entity.getIdentifier().toString()))
                .forEach(series -> series.getNode().setVisible(true));
    }

    @Override
    protected void dataItemAdded(Series<Number, Number> series, int itemIndex, Data<Number, Number> item) {
        // suppress printing of points
    }

    @Override
    public void onEventReceived(NodeUpdatedEvent event) {
        if (event.getNode().equals(this.referencedNode)) {
            for (ChemicalEntity entity : this.observedEntities) {
                // get associated value
                Series<Number, Number> series = this.getData().stream()
                        .filter(s -> s.getName().equals(entity.getIdentifier().toString()))
                        .findFirst().get();
                // get concentration of entity
                double concentration = event.getNode().getConcentration(entity).getValue().doubleValue();
                // add to plot
                Platform.runLater(() -> {
                    series.getData().add(new Data<>(event.getTime().getValue().doubleValue(), concentration));
                    if (this.scaleXAxis) {
                        if (series.getData().size() > this.maximalDataPoints) {
                            series.getData().remove(series.getData().size() - this.maximalDataPoints);
                        }
                    }
                });

            }
            // FIXME axis scaling does probably not work
            if (this.scaleXAxis) {
                ((NumberAxis) this.getXAxis()).setLowerBound(event.getTime().getValue().doubleValue() - this.maximalDataPoints);
                ((NumberAxis) this.getXAxis()).setUpperBound(event.getTime().getValue().doubleValue() - 1);
            } else {
                ((NumberAxis) this.getXAxis()).setUpperBound(event.getTime().getValue().doubleValue());
//                if (event.getTime().getValue().doubleValue() % 6 == 0) {
//                    ((NumberAxis) this.getXAxis()).setTickUnit(event.getTime().getValue().doubleValue() / 6);
//                }
            }
        }

    }

    public ObservableList<ChemicalEntity<?>> getObservedEntities() {
        return this.observedEntities;
    }

    public AutomatonNode getReferencedNode() {
        return this.referencedNode;
    }


}
