package com.williamcallahan.tui4j.compat.bubbles.list;

import static com.williamcallahan.tui4j.compat.bubbletea.Command.batch;

import com.williamcallahan.tui4j.compat.bubbles.key.Binding;
import com.williamcallahan.tui4j.compat.bubbles.spinner.TickMessage;
import com.williamcallahan.tui4j.compat.bubbles.textinput.TextInput;
import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.QuitMessage;
import com.williamcallahan.tui4j.compat.bubbletea.UpdateResult;
import java.util.LinkedList;
import java.util.Objects;

/**
 * Update/message handling for {@link List}.
 * <p>
 * Upstream: bubbles/list/list.go (extracted from the original port).
 */
final class ListUpdateHandler {

    private ListUpdateHandler() {
    }

    static UpdateResult<List> update(List list, Message msg) {
        java.util.List<Command> commands = new LinkedList<>();

        if (msg instanceof KeyPressMessage keyPressMessage) {
            if (Binding.matches(keyPressMessage, list.keys.forceQuit())) {
                return UpdateResult.from(list, QuitMessage::new);
            }
        } else if (
            msg instanceof FetchedCurrentPageItems(FetchedItems fetchedItems, Runnable[] postFetchCallbacks)
        ) {
            list.stopSpinner();
            list.fetchingItems = false;

            list.currentPageItems = fetchedItems.items();
            list.matchedItems = fetchedItems.matchedItems();
            list.totalItems = fetchedItems.totalItems();

            list.updateKeybindings();

            for (Runnable runnable : postFetchCallbacks) {
                runnable.run();
            }

            boolean requiresRefetch = ListPaginationUpdater.updatePagination(
                list
            );
            if (requiresRefetch) {
                return UpdateResult.from(
                    list,
                    ListDataFetcher.fetchCurrentPageItems(list)
                );
            }

            return UpdateResult.from(list, ListDataFetcher.updateFilter(list));
        } else if (msg instanceof TickMessage && list.showSpinner) {
            commands.add(list.spinner.update(msg).command());
        } else if (msg instanceof StatusMessageTimeoutMessage) {
            ListStatusMessageManager.hideStatusMessage(list);
        }

        if (list.filterState == FilterState.Filtering) {
            commands.add(handleFiltering(list, msg));
        } else {
            commands.add(handleBrowsing(list, msg));
        }

        return UpdateResult.from(list, batch(commands));
    }

    static Command cursorUp(List list) {
        if ((list.cursor - 1) > -1) {
            list.cursor--;
            return Command.none();
        }

        if (list.paginator.page() != 0) {
            list.paginator.prevPage();
            return ListDataFetcher.fetchCurrentPageItems(list, () ->
                list.cursor = Math.max(0, list.currentPageItems.size() - 1)
            );
        }

        if (!list.infiniteScrolling) {
            return Command.none();
        }

        list.paginator.setPage(list.paginator.totalPages() - 1);
        return ListDataFetcher.fetchCurrentPageItems(list, () ->
            list.cursor = Math.max(0, list.currentPageItems.size() - 1)
        );
    }

    static Command cursorDown(List list) {
        if ((list.cursor + 1) < list.currentPageItems.size()) {
            list.cursor++;
            return Command.none();
        }

        if (!list.paginator.onLastPage()) {
            list.paginator.nextPage();
            return ListDataFetcher.fetchCurrentPageItems(list, () ->
                list.cursor = 0
            );
        }

        if (list.infiniteScrolling) {
            list.paginator.setPage(0);
            return ListDataFetcher.fetchCurrentPageItems(list, () ->
                list.cursor = 0
            );
        }
        return Command.none();
    }

    private static Command handleBrowsing(List list, Message msg) {
        java.util.List<Command> commands = new LinkedList<>();

        if (msg instanceof KeyPressMessage keyPressMessage) {
            if (Binding.matches(keyPressMessage, list.keys.clearFilter())) {
                commands.add(list.resetFiltering());
            } else if (Binding.matches(keyPressMessage, list.keys.quit())) {
                return QuitMessage::new;
            } else if (Binding.matches(keyPressMessage, list.keys.cursorUp())) {
                commands.add(cursorUp(list));
            } else if (
                Binding.matches(keyPressMessage, list.keys.cursorDown())
            ) {
                commands.add(cursorDown(list));
            } else if (Binding.matches(keyPressMessage, list.keys.prevPage())) {
                commands.add(cursorLeft(list));
            } else if (Binding.matches(keyPressMessage, list.keys.nextPage())) {
                commands.add(cursorRight(list));
            } else if (Binding.matches(keyPressMessage, list.keys.goToStart())) {
                commands.add(gotoStart(list));
            } else if (Binding.matches(keyPressMessage, list.keys.goToEnd())) {
                commands.add(gotoEnd(list));
            } else if (Binding.matches(keyPressMessage, list.keys.filter())) {
                ListStatusMessageManager.hideStatusMessage(list);
                commands.add(TextInput::blink);

                if (!list.paginator.onFirstPage()) {
                    list.paginator.setPage(0);
                }

                list.filterState = FilterState.Filtering;
                list.filterInput.cursorEnd();
                list.filterInput.focus();
                list.updateKeybindings();

                commands.add(
                    ListDataFetcher.fetchCurrentPageItems(list, () ->
                        list.cursor = 0
                    )
                );

                return batch(commands);
            } else if (
                Binding.matches(keyPressMessage, list.keys.showFullHelp()) ||
                    Binding.matches(keyPressMessage, list.keys.closeFullHelp())
            ) {
                list.help.setShowAll(!list.help.showAll());
                ListPaginationUpdater.updatePagination(list);

                commands.add(ListDataFetcher.fetchCurrentPageItems(list));
            }
            commands.add(list.itemDelegate.update(msg, list));
        }
        return batch(commands);
    }

    private static Command gotoStart(List list) {
        if (list.paginator.onFirstPage()) {
            list.cursor = 0;
            return Command.none();
        }

        list.paginator.setPage(0);
        list.cursor = 0;
        return ListDataFetcher.fetchCurrentPageItems(list);
    }

    private static Command gotoEnd(List list) {
        if (list.paginator.onLastPage()) {
            selectLastItemOnPage(list);
            return Command.none();
        }

        list.paginator.setPage(list.paginator.totalPages() - 1);
        return ListDataFetcher.fetchCurrentPageItems(list, () ->
            selectLastItemOnPage(list)
        );
    }

    /** Selects the last item of the currently shown page. */
    private static void selectLastItemOnPage(List list) {
        list.cursor = Math.max(0, list.currentPageItems.size() - 1);
    }

    private static Command cursorLeft(List list) {
        if (list.paginator.onFirstPage()) {
            return Command.none();
        }
        list.paginator.prevPage();
        return ListDataFetcher.fetchCurrentPageItems(list);
    }

    private static Command cursorRight(List list) {
        if (list.paginator.onLastPage()) {
            return Command.none();
        }
        list.paginator.nextPage();
        return ListDataFetcher.fetchCurrentPageItems(list, () ->
            keepCursorInBounds(list)
        );
    }

    static void keepCursorInBounds(List list) {
        if (list.currentPageItems.isEmpty()) {
            list.cursor = 0;
            return;
        }
        list.cursor = Math.clamp(list.cursor, 0, list.currentPageItems.size() - 1);
    }

    private static Command handleFiltering(List list, Message msg) {
        java.util.List<Command> commands = new LinkedList<>();

        if (msg instanceof KeyPressMessage keyPressMessage) {
            if (
                Binding.matches(keyPressMessage, list.keys.cancelWhileFiltering())
            ) {
                list.resetFiltering();

                commands.add(
                    ListDataFetcher.fetchCurrentPageItems(list, () -> {
                        list.keys.filter().setEnabled(true);
                        list.keys.clearFilter().setEnabled(false);
                    })
                );
            } else if (
                Binding.matches(keyPressMessage, list.keys.acceptWhileFiltering())
            ) {
                ListStatusMessageManager.hideStatusMessage(list);

                if (list.totalItems > 0) {
                    if (list.matchedItems > 0) {
                        list.filterInput.blur();
                        list.filterState = FilterState.FilterApplied;
                        list.updateKeybindings();

                        if (list.filterInput.isEmpty()) {
                            commands.add(list.resetFiltering());
                        }
                    } else {
                        commands.add(list.resetFiltering());
                    }
                }
            }
        }

        String beforeChange = list.filterInput.value();
        UpdateResult<TextInput> updateResult = list.filterInput.update(msg);
        boolean filterChanged = !Objects.equals(
            beforeChange,
            updateResult.model().value()
        );
        list.filterInput = updateResult.model();
        commands.add(updateResult.command());

        if (filterChanged && !list.filterOnAcceptOnly) {
            commands.add(
                ListDataFetcher.fetchCurrentPageItems(list, () -> {
                    list.keys
                        .acceptWhileFiltering()
                        .setEnabled(!list.filterInput.isEmpty());
                    ListPaginationUpdater.updatePagination(list);
                })
            );
        }
        return batch(commands);
    }
}
