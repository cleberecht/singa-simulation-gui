package de.bioforscher.singa.simulation.gui.components;

import de.bioforscher.singa.core.events.UpdateEventListener;
import de.bioforscher.singa.features.parameters.EnvironmentalParameters;
import de.bioforscher.singa.simulation.events.GraphUpdatedEvent;
import javafx.scene.text.Text;

/**
 * @author cl
 */
public class SimulationIndicator extends Text implements UpdateEventListener<GraphUpdatedEvent> {

    @Override
    public void onEventReceived(GraphUpdatedEvent event) {
        textProperty().setValue(EnvironmentalParameters.getTimeStep().toString());
    }

}
