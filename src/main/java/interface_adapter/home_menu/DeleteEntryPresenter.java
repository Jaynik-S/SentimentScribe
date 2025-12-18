package interface_adapter.home_menu;

import use_case.delete_entry.DeleteEntryOutputBoundary;
import use_case.delete_entry.DeleteEntryOutputData;

import java.util.ArrayList;
import java.util.List;

public class DeleteEntryPresenter implements DeleteEntryOutputBoundary {

    private final HomeMenuViewModel viewModel;

    public DeleteEntryPresenter(HomeMenuViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(DeleteEntryOutputData outputData) {
        if (outputData == null) {
            return;
        }
        String deletedPath = outputData.getEntryPath();
        if (deletedPath == null || deletedPath.isBlank()) {
            return;
        }

        HomeMenuState state = viewModel.getState();
        List<String> paths = state.getStoragePaths();
        int index = paths.indexOf(deletedPath);
        if (index < 0) {
            return;
        }

        state.setTitles(removeAt(state.getTitles(), index));
        state.setCreatedDates(removeAt(state.getCreatedDates(), index));
        state.setUpdatedDates(removeAt(state.getUpdatedDates(), index));
        state.setKeywords(removeAt(state.getKeywords(), index));
        state.setStoragePaths(removeAt(state.getStoragePaths(), index));
        state.setErrorMessage("");

        viewModel.setState(state);
        viewModel.firePropertyChanged();
    }

    @Override
    public void prepareFailView(String errorMessage) {
        HomeMenuState state = viewModel.getState();
        state.setErrorMessage(errorMessage == null ? "Failed to delete entry." : errorMessage);
        viewModel.setState(state);
        viewModel.firePropertyChanged();
    }

    private static List<String> removeAt(List<String> list, int index) {
        if (list == null || index < 0 || index >= list.size()) {
            return list == null ? List.of() : list;
        }
        List<String> copy = new ArrayList<>(list);
        copy.remove(index);
        return copy;
    }
}

