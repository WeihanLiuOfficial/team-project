package app;

import data_access.N2yoPassDataAccessObject;
import interface_adapter.predict_passes.PredictPassesController;
import interface_adapter.predict_passes.PredictPassesPresenter;
import interface_adapter.predict_passes.PredictPassesViewModel;
import use_case.predict_passes.PredictPassesDataAccessInterface;
import use_case.predict_passes.PredictPassesInputBoundary;
import use_case.predict_passes.PredictPassesInteractor;
import view.SatelliteTrackerView;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.ZoneId;

/**
 * Standalone launcher for the satellite tracker screen.
 */
public final class SatelliteTrackerApplication {

    private SatelliteTrackerApplication() {
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("N2YO_API_KEY");

        SwingUtilities.invokeLater(() -> {
            if (apiKey == null || apiKey.isBlank()) {
                JOptionPane.showMessageDialog(
                        null,
                        "Set N2YO_API_KEY in this application's run configuration.",
                        "Missing API key",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            createWindow(apiKey);
        });
    }

    private static void createWindow(String apiKey) {
        ZoneId displayZone = ZoneId.of("America/Toronto");

        PredictPassesViewModel viewModel =
                new PredictPassesViewModel();

        PredictPassesPresenter presenter =
                new PredictPassesPresenter(viewModel, displayZone);

        PredictPassesDataAccessInterface dataAccess =
                new N2yoPassDataAccessObject(apiKey);

        PredictPassesInputBoundary useCase =
                new PredictPassesInteractor(dataAccess, presenter);

        PredictPassesController controller =
                new PredictPassesController(useCase, presenter);

        SatelliteTrackerView view =
                new SatelliteTrackerView(
                        controller, viewModel, displayZone);

        JFrame frame = new JFrame("ISS Satellite Tracker");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(view);
        frame.setSize(1200, 600);
        frame.setMinimumSize(new Dimension(900, 450));
        frame.setLocationRelativeTo(null);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                view.disposeView();
            }
        });

        frame.setVisible(true);
    }
}
