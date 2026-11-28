package interface_adapter.predict_passes;

import entity.SatellitePass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import use_case.predict_passes.PredictPassesOutputData;

import java.beans.PropertyChangeEvent;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PredictPassesPresenterTest {

    private final PredictPassesViewModel viewModel =
            new PredictPassesViewModel();

    private final PredictPassesPresenter presenter =
            new PredictPassesPresenter(
                    viewModel, ZoneId.of("America/Toronto"));

    @ParameterizedTest
    @CsvSource({
            "2026-01-15T12:00:00Z,"
                    + "2026-01-15 07:00:00 -05:00,"
                    + "2026-01-15 07:10:00 -05:00",
            "2026-07-15T12:00:00Z,"
                    + "2026-07-15 08:00:00 -04:00,"
                    + "2026-07-15 08:10:00 -04:00"
    })
    void formatsRowsUsingTheConfiguredTimeZone(
            String utcStart,
            String expectedStart,
            String expectedEnd) {

        SatellitePass pass =
                createPass(Instant.parse(utcStart).getEpochSecond());

        presenter.prepareSuccessView(
                new PredictPassesOutputData(List.of(pass)));

        PredictPassesViewModel.State state = viewModel.getState();

        assertFalse(state.isError());
        assertEquals(1, state.getRows().size());

        assertEquals(
                List.of(
                        "ISS",
                        expectedStart,
                        expectedEnd,
                        "7 min 25 s",
                        "88.07°",
                        "301.72°",
                        "122.97°"),
                state.getRows().get(0));
    }

    @Test
    void failureClearsPreviousResultsAndDisplaysError() {
        presenter.prepareSuccessView(
                new PredictPassesOutputData(
                        List.of(createPass(1000L))));

        assertEquals(1, viewModel.getState().getRows().size());

        presenter.prepareFailView("Unable to retrieve satellite passes.");

        PredictPassesViewModel.State state = viewModel.getState();

        assertTrue(state.isError());
        assertTrue(state.getRows().isEmpty());
        assertEquals(
                "Unable to retrieve satellite passes.",
                state.getMessage());
    }

    @Test
    void emptySuccessClearsPreviousError() {
        presenter.prepareFailView("Previous request failed.");
        assertTrue(viewModel.getState().isError());

        presenter.prepareSuccessView(
                new PredictPassesOutputData(List.of()));

        PredictPassesViewModel.State state = viewModel.getState();

        assertFalse(state.isError());
        assertTrue(state.getRows().isEmpty());
        assertEquals(
                "No visible ISS passes matched your search.",
                state.getMessage());
    }

    @Test
    void resultUpdateNotifiesListenersWithTheNewState() {
        List<PropertyChangeEvent> events = new ArrayList<>();
        viewModel.addPropertyChangeListener(events::add);

        PredictPassesViewModel.State previousState =
                viewModel.getState();

        presenter.prepareSuccessView(
                new PredictPassesOutputData(
                        List.of(createPass(1000L))));

        assertEquals(1, events.size());

        PropertyChangeEvent event = events.get(0);

        assertEquals("state", event.getPropertyName());
        assertSame(previousState, event.getOldValue());
        assertSame(viewModel.getState(), event.getNewValue());
        assertEquals(1, viewModel.getState().getRows().size());
    }

    @Test
    void unrepresentableTimestampProducesErrorInsteadOfThrowing() {
        SatellitePass pass = new SatellitePass(
                25544,
                "ISS",
                Long.MAX_VALUE,
                Long.MAX_VALUE,
                0,
                45.0,
                90.0,
                180.0,
                270.0,
                false);

        presenter.prepareSuccessView(
                new PredictPassesOutputData(List.of(pass)));

        assertTrue(viewModel.getState().isError());
        assertTrue(viewModel.getState().getRows().isEmpty());
        assertFalse(viewModel.getState().getMessage().isBlank());
    }

    private static SatellitePass createPass(long startTime) {
        return new SatellitePass(
                25544,
                "ISS",
                startTime,
                startTime + 600,
                445,
                88.07,
                301.72,
                14.21,
                122.97,
                false);
    }
}
