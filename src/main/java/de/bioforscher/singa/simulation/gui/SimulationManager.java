package de.bioforscher.singa.simulation.gui;

import de.bioforscher.singa.core.events.UpdateEventEmitter;
import de.bioforscher.singa.core.events.UpdateEventListener;
import de.bioforscher.singa.simulation.events.GraphUpdatedEvent;
import de.bioforscher.singa.simulation.model.graphs.AutomatonNode;
import de.bioforscher.singa.simulation.modules.model.Simulation;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

/**
 * @author cl
 */
public class SimulationManager extends Task<Simulation> implements UpdateEventEmitter<GraphUpdatedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(SimulationManager.class);

    private final Simulation simulation;
    private CopyOnWriteArrayList<UpdateEventListener<GraphUpdatedEvent>> listeners;

    private final int TICKS_PER_SECOND = 25;
    private final int SKIP_TICKS = 1000 / TICKS_PER_SECOND;
    // final int MAX_FRAMESKIP = 5;

    public SimulationManager(Simulation simulation) {
        logger.debug("Initializing simulation manager ...");
        this.simulation = simulation;
        this.listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public void addEventListener(UpdateEventListener<GraphUpdatedEvent> listener) {
        logger.info("Added {} to registered update listeners.", listener.getClass().getSimpleName());
        this.listeners.add(listener);
    }

    @Override
    public CopyOnWriteArrayList<UpdateEventListener<GraphUpdatedEvent>> getListeners() {
        return this.listeners;
    }

    @Override
    protected Simulation call() throws Exception {
        long nextTick = System.currentTimeMillis();
        int skips = 0;
        while (!isCancelled()) {
            long currentMillis = System.currentTimeMillis();
            this.simulation.nextEpoch();
            if (currentMillis > nextTick) {
                nextTick = currentMillis + SKIP_TICKS;
                this.emitEvent(new GraphUpdatedEvent(this.simulation.getGraph()));
                this.simulation.getGraph().getNodes().stream()
                        .filter(AutomatonNode::isObserved)
                        .forEach(simulation::emitNextEpochEvent);
                skips = 0;
            } else {
                skips++;
            }
        }
        return this.simulation;
    }

    @Override
    protected void done() {
        try {
            if (!isCancelled()) {
                get();
            }
        } catch (ExecutionException e) {
            // Exception occurred, deal with it
            logger.error("Encountered an exception during simulation: " + e.getCause());
            e.printStackTrace();
        } catch (InterruptedException e) {
            // Shouldn't happen, we're invoked when computation is finished
            throw new AssertionError(e);
        }
    }


}
