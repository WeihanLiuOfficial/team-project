package app;

import data_access.N2yoPassDataAccessObject;
import entity.SatellitePass;
import use_case.predict_passes.PredictPassesDataAccessInterface;
import use_case.predict_passes.PredictPassesInputBoundary;
import use_case.predict_passes.PredictPassesInputData;
import use_case.predict_passes.PredictPassesInteractor;
import use_case.predict_passes.PredictPassesOutputBoundary;
import use_case.predict_passes.PredictPassesOutputData;

/**
 * Manually runs the prediction use case with the real N2YO data source.
 */
public final class N2yoConnectionCheck {

    private N2yoConnectionCheck() {
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("N2YO_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            System.out.println(
                    "Missing N2YO_API_KEY. Set it in the run configuration.");
            return;
        }

        PredictPassesDataAccessInterface dataAccess =
                new N2yoPassDataAccessObject(apiKey);

        PredictPassesOutputBoundary presenter =
                new PredictPassesOutputBoundary() {
                    @Override
                    public void prepareSuccessView(
                            PredictPassesOutputData outputData) {
                        System.out.println("Prediction use case succeeded.");
                        System.out.println(
                                "Matching visual passes: "
                                        + outputData.getPasses().size());

                        for (SatellitePass pass : outputData.getPasses()) {
                            System.out.println(pass);
                        }
                    }

                    @Override
                    public void prepareFailView(String errorMessage) {
                        System.out.println(
                                "Prediction use case failed: " + errorMessage);
                    }
                };

        PredictPassesInputBoundary useCase =
                new PredictPassesInteractor(dataAccess, presenter);

        PredictPassesInputData input = new PredictPassesInputData(
                43.65,
                -79.38,
                100.0,
                3,
                20.0,
                true);

        useCase.execute(input);
    }
}
