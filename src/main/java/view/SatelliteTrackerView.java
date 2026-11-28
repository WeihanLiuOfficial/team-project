package view;

import interface_adapter.predict_passes.PredictPassesController;
import interface_adapter.predict_passes.PredictPassesViewModel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.beans.PropertyChangeListener;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * Displays ISS visual-pass predictions.
 * Construct and manage this panel on the Swing event dispatch thread.
 */
public final class SatelliteTrackerView extends JPanel {
    private static final long serialVersionUID = 1L;

    private final PredictPassesController controller;
    private final PredictPassesViewModel viewModel;

    private final JTextField latitudeField = new JTextField("43.65");
    private final JTextField longitudeField = new JTextField("-79.38");
    private final JTextField altitudeField = new JTextField("100");
    private final JTextField daysField = new JTextField("3");
    private final JTextField elevationField = new JTextField("20");

    private final JTextField[] fields = {
            latitudeField, longitudeField, altitudeField,
            daysField, elevationField
    };

    private final JButton searchButton = new JButton("Search");
    private final JLabel statusLabel = new JLabel();

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{
                    "Satellite", "Start", "End", "Visible duration",
                    "Peak elevation", "Start azimuth", "End azimuth"
            }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final PropertyChangeListener stateListener =
            event -> refreshOnEdt();

    private SwingWorker<Void, Void> worker;
    private boolean busy;
    private boolean disposed;

    public SatelliteTrackerView(
            PredictPassesController controller,
            PredictPassesViewModel viewModel,
            ZoneId displayZone) {
        this.controller = controller;
        this.viewModel = viewModel;

        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("ISS Satellite Tracker");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

        JLabel description = new JLabel(
                "Optically visible passes from N2YO"
                        + " | Display time zone: " + displayZone.getId());

        JPanel heading = new JPanel(new GridLayout(0, 1, 0, 6));
        heading.add(title);
        heading.add(description);

        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.add(heading, BorderLayout.NORTH);
        top.add(createControls(), BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().setReorderingAllowed(false);

        int[] widths = {150, 235, 235, 130, 120, 120, 120};
        for (int index = 0; index < widths.length; index++) {
            table.getColumnModel().getColumn(index)
                    .setPreferredWidth(widths[index]);
        }

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        searchButton.addActionListener(event -> startSearch());

        for (JTextField field : fields) {
            field.addActionListener(event -> startSearch());
        }

        viewModel.addPropertyChangeListener(stateListener);
        renderState();
    }

    private JPanel createControls() {
        String[] labels = {
                "Latitude (-90 to 90)",
                "Longitude (-180 to 180)",
                "Altitude (meters)",
                "Days (1 to 7)",
                "Min. peak elevation (°)"
        };

        JPanel inputs = new JPanel(new GridLayout(1, 5, 10, 0));

        for (int index = 0; index < fields.length; index++) {
            JPanel input = new JPanel(new BorderLayout(0, 5));
            JLabel label = new JLabel(labels[index]);
            label.setLabelFor(fields[index]);
            input.add(label, BorderLayout.NORTH);
            input.add(fields[index], BorderLayout.CENTER);
            inputs.add(input);
        }

        JPanel controls = new JPanel(new BorderLayout(12, 0));
        controls.add(inputs, BorderLayout.CENTER);
        controls.add(searchButton, BorderLayout.EAST);
        return controls;
    }

    private void startSearch() {
        if (busy || disposed) {
            return;
        }

        // Read Swing components before entering the background thread.
        String latitude = latitudeField.getText();
        String longitude = longitudeField.getText();
        String altitude = altitudeField.getText();
        String days = daysField.getText();
        String elevation = elevationField.getText();

        setBusy(true);
        viewModel.update(List.of(), "Searching for visible ISS passes...", false);

        worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                controller.execute(
                        latitude, longitude, altitude, days, elevation);
                return null;
            }

            @Override
            protected void done() {
                if (disposed) {
                    return;
                }

                try {
                    get();
                }
                catch (CancellationException exception) {
                    viewModel.update(List.of(), "Search cancelled.", false);
                }
                catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    viewModel.update(List.of(), "Search interrupted.", true);
                }
                catch (ExecutionException exception) {
                    viewModel.update(
                            List.of(),
                            "An unexpected error occurred while searching.",
                            true);
                }
                finally {
                    setBusy(false);
                    worker = null;
                }
            }
        };

        worker.execute();
    }

    private void setBusy(boolean searching) {
        busy = searching;
        searchButton.setEnabled(!searching);
        searchButton.setText(searching ? "Searching..." : "Search");

        for (JTextField field : fields) {
            field.setEnabled(!searching);
        }
    }

    private void refreshOnEdt() {
        if (SwingUtilities.isEventDispatchThread()) {
            renderState();
        }
        else {
            SwingUtilities.invokeLater(this::renderState);
        }
    }

    private void renderState() {
        if (disposed) {
            return;
        }

        PredictPassesViewModel.State state = viewModel.getState();

        tableModel.setRowCount(0);
        for (List<String> row : state.getRows()) {
            tableModel.addRow(row.toArray(new Object[0]));
        }

        statusLabel.setText(state.getMessage());
        statusLabel.setForeground(
                state.isError() ? new Color(170, 30, 30) : Color.DARK_GRAY);
    }

    /**
     * Releases listeners and cancels a pending request when the view closes.
     */
    public void disposeView() {
        disposed = true;
        viewModel.removePropertyChangeListener(stateListener);

        if (worker != null) {
            worker.cancel(true);
        }
    }
}
