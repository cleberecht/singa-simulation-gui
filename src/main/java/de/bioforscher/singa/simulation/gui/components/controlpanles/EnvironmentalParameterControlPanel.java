package de.bioforscher.singa.simulation.gui.components.controlpanles;

import de.bioforscher.singa.features.parameters.EnvironmentalParameters;
import de.bioforscher.singa.features.quantities.DynamicViscosity;
import de.bioforscher.singa.features.units.UnitName;
import de.bioforscher.singa.features.units.UnitPrefix;
import de.bioforscher.singa.features.units.UnitPrefixes;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import tec.uom.se.quantity.Quantities;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.Time;
import java.util.Observable;
import java.util.Observer;

import static de.bioforscher.singa.features.units.UnitProvider.PASCAL_SECOND;
import static tec.uom.se.unit.MetricPrefix.MILLI;
import static tec.uom.se.unit.Units.*;

public class EnvironmentalParameterControlPanel extends GridPane implements Observer {

    private Spinner<Number> nodeDistanceValue;
    private Spinner<Number> timeStepValue;
    private Spinner<Number> temperatureValue;
    private Spinner<Number> viscosityValue;

    private ComboBox<Unit<Length>> nodeDistanceUnit = new ComboBox<>();
    private ComboBox<Unit<Time>> timeStepUnit = new ComboBox<>();
    private Label maximalDegree = new Label();
    private Label maximalDiffusivity = new Label();
    private Label maximalDifference = new Label();

    private StringProperty dirtyableText;

    public EnvironmentalParameterControlPanel() {
        configureGrid();
        configureAndAddLabels();
        configureAndAddButtons();
        configureNodeDistanceValue();
        configureNodeDistanceUnit();
        configureTimeStepValue();
        configureTimeStepUnit();
        configureTemperatureValue();
        configureViscosityValue();
        addRemainingComponentsToGrid();
        EnvironmentalParameters.attachObserver(this);
    }

    private void configureGrid() {
        this.setHgap(10);
        this.setVgap(10);
        this.setPadding(new Insets(10, 10, 10, 10));
    }

    private void configureAndAddLabels() {
        // node distance
        Label labNodeDistance = new Label("Node distance:");
        this.add(labNodeDistance, 0, 0);
        // time step
        Label labTimeStep = new Label("Time step size:");
        this.add(labTimeStep, 0, 1);
        // temperature
        Label labTemperature = new Label("Temperature:");
        this.add(labTemperature, 0, 2);
        Label labTemperatureUnit = new Label(UnitName.CELSIUS.getSymbol());
        this.add(labTemperatureUnit, 2, 2);
        // viscosity
        Label labViscosity = new Label("Viscosity:");
        this.add(labViscosity, 0, 3);
        Label labViscosityUnit = new Label(UnitName.PASCAL.getSymbol() + UnitName.SECOND.getSymbol());
        this.add(labViscosityUnit, 2, 3);
        // maximal degree
        Label labMaximalDegree = new Label("Maximal Degree");
        this.add(labMaximalDegree, 0, 4);
        // maximal diffusivity
        Label labMaximalDiffusivity = new Label("Maximal Diffusivity");
        this.add(labMaximalDiffusivity, 0, 5);
        // maximal diffusivity
        Label labMaximalConcentration = new Label("Steepest concentration drop");
        this.add(labMaximalConcentration, 0, 6);
    }

    private void configureAndAddButtons() {
        // restore defaults
        Button btnDefaults = new Button("Restore Defaults");
        btnDefaults.setMaxWidth(Double.MAX_VALUE);
        btnDefaults.setOnAction(this::restoreDefault);
        this.add(btnDefaults, 0, 7, 1, 1);
        Button btnApply = new Button("Apply");
        // apply changes
        btnApply.setMaxWidth(Double.MAX_VALUE);
        btnApply.setOnAction(this::applyChanges);
        this.add(btnApply, 1, 7, 1, 1);
    }

    private void configureNodeDistanceValue() {
        this.nodeDistanceValue = new Spinner<>(1.0, 1000.0, EnvironmentalParameters.getNodeDistance()
                .getValue().doubleValue());
        this.nodeDistanceValue.setEditable(true);
        this.nodeDistanceValue.setPrefWidth(150);
        this.nodeDistanceValue.valueProperty()
                .addListener((observable, oldValue, newValue) -> this.markChangesAsUnApplied());
    }

    private void configureNodeDistanceUnit() {
        this.nodeDistanceUnit.getItems().addAll(
                UnitPrefixes.generateUnitsForPrefixes(UnitPrefix.getDefaultSpacePrefixes(), METRE));
        this.nodeDistanceUnit.setValue(EnvironmentalParameters.getNodeDistance().getUnit());
        this.nodeDistanceUnit.setPrefWidth(100);
        this.nodeDistanceUnit.valueProperty()
                .addListener((observable, oldValue, newValue) -> this.markChangesAsUnApplied());
    }

    private void configureTimeStepValue() {
        this.timeStepValue = new Spinner<>(1, 1000, EnvironmentalParameters.getTimeStep().getValue().intValue());
        this.timeStepValue.setEditable(true);
        this.timeStepValue.setPrefWidth(150);
        this.timeStepValue.valueProperty()
                .addListener((observable, oldValue, newValue) -> this.markChangesAsUnApplied());
    }

    private void configureTimeStepUnit() {
        this.timeStepUnit.getItems().addAll(
                UnitPrefixes.generateUnitsForPrefixes(UnitPrefix.getDefaultTimePrefixes(), SECOND));
        this.timeStepUnit.setValue(EnvironmentalParameters.getTimeStep().getUnit());
        this.timeStepUnit.setPrefWidth(100);
        this.timeStepUnit.valueProperty()
                .addListener((observable, oldValue, newValue) -> this.markChangesAsUnApplied());
    }

    private void configureTemperatureValue() {
        this.temperatureValue = new Spinner<>(0, 100, EnvironmentalParameters.getTemperature()
                .getValue().doubleValue(), 0.1);
        this.temperatureValue.setEditable(true);
        this.temperatureValue.setPrefWidth(150);
        this.temperatureValue.valueProperty()
                .addListener((observable, oldValue, newValue) -> this.markChangesAsUnApplied());
    }

    private void configureViscosityValue() {
        this.viscosityValue = new Spinner<>(0, 100, EnvironmentalParameters.getViscosity()
                .getValue().doubleValue(), 0.1);
        this.viscosityValue.setEditable(true);
        this.viscosityValue.setPrefWidth(150);
        this.viscosityValue.valueProperty()
                .addListener((observable, oldValue, newValue) -> this.markChangesAsUnApplied());
    }

    private void addRemainingComponentsToGrid() {
        this.add(this.nodeDistanceValue, 1, 0);
        this.add(this.nodeDistanceUnit, 2, 0);
        this.add(this.timeStepValue, 1, 1);
        this.add(this.timeStepUnit, 2, 1);
        this.add(this.temperatureValue, 1, 2);
        this.add(this.viscosityValue, 1, 3);
        this.add(this.maximalDegree, 1, 4);
        this.add(this.maximalDiffusivity, 1, 5);
        this.add(this.maximalDifference, 1, 6);
    }

    private void applyChanges(ActionEvent event) {
        Quantity<Length> nodeDistance = Quantities.getQuantity(this.nodeDistanceValue.getValue(), this.nodeDistanceUnit.getValue());
        Quantity<Time> timeStep = Quantities.getQuantity(this.timeStepValue.getValue(), this.timeStepUnit.getValue());
        Quantity<Temperature> systemTemperature = Quantities.getQuantity(this.temperatureValue.getValue(), CELSIUS);
        Quantity<DynamicViscosity> systemViscosity = Quantities.getQuantity(this.viscosityValue.getValue(), MILLI(PASCAL_SECOND));

        EnvironmentalParameters.setNodeDistance(nodeDistance);
        EnvironmentalParameters.setTimeStep(timeStep);
        EnvironmentalParameters.setTemperature(systemTemperature);
        EnvironmentalParameters.setSystemViscosity(systemViscosity);

        markChangesAsApplied();
    }

    private void restoreDefault(ActionEvent event) {
        EnvironmentalParameters.setNodeDistance(EnvironmentalParameters.DEFAULT_NODE_DISTANCE);
        EnvironmentalParameters.setTimeStep(EnvironmentalParameters.DEFAULT_TIME_STEP);
        EnvironmentalParameters.setTemperature(EnvironmentalParameters.DEFAULT_TEMPERATURE);
        EnvironmentalParameters.setSystemViscosity(EnvironmentalParameters.DEFAULT_VISCOSITY);
        markChangesAsApplied();
    }

    @Override
    public void update(Observable o, Object arg) {
        update();
    }

    public void update() {
        Quantity<Length> nodeDistance = EnvironmentalParameters.getNodeDistance();
        this.nodeDistanceUnit.setValue(nodeDistance.getUnit());
        this.nodeDistanceValue.getValueFactory().setValue(nodeDistance.getValue().doubleValue());

        Quantity<Time> timeStep = EnvironmentalParameters.getTimeStep();
        this.timeStepUnit.setValue(timeStep.getUnit());
        this.timeStepValue.getValueFactory().setValue(timeStep.getValue().intValue());

        Quantity<Temperature> temperature = EnvironmentalParameters.getTemperature().to(CELSIUS);
        this.temperatureValue.getValueFactory().setValue(temperature.getValue().doubleValue());

        Quantity<DynamicViscosity> viscosity = EnvironmentalParameters.getViscosity();
        this.viscosityValue.getValueFactory().setValue(viscosity.getValue().doubleValue());

        markChangesAsApplied();
    }

    private void markChangesAsUnApplied() {
        if (this.dirtyableText != null) {
            if (!this.dirtyableText.getValue().endsWith("*")) {
                this.dirtyableText.setValue(this.dirtyableText.getValue() + " *");
            }
        }
    }

    private void markChangesAsApplied() {
        if (this.dirtyableText != null) {
            this.dirtyableText.setValue(this.dirtyableText.getValue().replace(" *", ""));
        }
    }

    public void setDirtyableText(StringProperty dirtyableText) {
        if (this.dirtyableText != null) {
            this.dirtyableText.bind(dirtyableText);
        }
    }

    public ObjectProperty<Number> getNodeDistanceProperty() {
        return this.nodeDistanceValue.getValueFactory().valueProperty();
    }

    public ReadOnlyObjectProperty<Unit<Length>> getNodeDistanceUnitProperty() {
        return this.nodeDistanceUnit.getSelectionModel().selectedItemProperty();
    }

    public ReadOnlyObjectProperty<Unit<Time>> getTimeStepSizeUnitProperty() {
        return this.timeStepUnit.getSelectionModel().selectedItemProperty();
    }

    public ObjectProperty<Number> getTimeStepSizeProperty() {
        return this.timeStepValue.getValueFactory().valueProperty();
    }

    public StringProperty getMaximalDegreeProperty() {
        return this.maximalDegree.textProperty();
    }

    public StringProperty getMaximalDiffusivityProperty() {
        return this.maximalDiffusivity.textProperty();
    }

    public StringProperty getMaximalConcentrationDiffenceProperty() {
        return this.maximalDifference.textProperty();
    }


}
