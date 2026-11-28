package interface_adapter.predict_passes;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Holds immutable snapshots of the prediction screen's display data.
 */
public final class PredictPassesViewModel {

    private final PropertyChangeSupport support =
            new PropertyChangeSupport(this);

    private volatile State state = new State(
            List.of(),
            "Enter observation details and select Search.",
            false);

    public State getState() {
        return state;
    }

    public void update(
            List<List<String>> rows,
            String message,
            boolean error) {
        State nextState = new State(rows, message, error);
        State previousState = state;
        state = nextState;

        support.firePropertyChange(
                "state", previousState, nextState);
    }

    public void addPropertyChangeListener(
            PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(
            PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    /**
     * One complete, immutable snapshot of the screen's result.
     */
    public static final class State {
        private final List<List<String>> rows;
        private final String message;
        private final boolean error;

        private State(
                List<List<String>> rows,
                String message,
                boolean error) {
            List<List<String>> copiedRows = new ArrayList<>();

            for (List<String> row : rows) {
                copiedRows.add(List.copyOf(row));
            }

            this.rows = List.copyOf(copiedRows);
            this.message = Objects.requireNonNull(message);
            this.error = error;
        }

        public List<List<String>> getRows() {
            return rows;
        }

        public String getMessage() {
            return message;
        }

        public boolean isError() {
            return error;
        }
    }
}
