#!/usr/bin/env python3

import datetime
from typing import List

from PyQt5.QtCore import QThread, pyqtSignal
from PyQt5.QtWidgets import QWidget, QVBoxLayout, QHBoxLayout, QPushButton, QTreeWidget, QTreeWidgetItem, QMessageBox

from ..dialogs.new_task_dialog import NewTaskDialog
from ...util import ActiveTask, RegisteredTask


class TasksTab(QWidget):

    def __init__(self, main_window) -> None:
        super().__init__()

        self.main_window = main_window

        self._start_task_thread = None
        self._stop_task_thread = None

        self._active_tasks = []

        self.setObjectName("tasks_tab")

        self.main_layout = QVBoxLayout(self)

        self.tasks_tree_widget = QTreeWidget(self)
        self.main_layout.addWidget(self.tasks_tree_widget)

        self.buttons_layout = QHBoxLayout()

        self.new_task_button = QPushButton(self)
        self.buttons_layout.addWidget(self.new_task_button)

        self.stop_task_button = QPushButton(self)
        self.stop_task_button.setEnabled(False)
        self.buttons_layout.addWidget(self.stop_task_button)

        self.main_layout.addLayout(self.buttons_layout)

        self.tasks_tree_widget.setHeaderLabel(" Current tasks (0):")
        self.new_task_button.setText("New task")
        self.stop_task_button.setText("Stop task")

        self.main_window.task_added_emitter.connect(self._on_task_added)
        self.main_window.task_removed_emitter.connect(self._on_task_removed)
        self.main_window.task_updated_emitter.connect(self._on_task_updated)
        self.main_window.task_result_emitter.connect(self._on_task_result)

        self.tasks_tree_widget.itemSelectionChanged.connect(self._on_item_selection_changed)
        self.new_task_button.clicked.connect(self._on_new_task)
        self.stop_task_button.clicked.connect(self._on_stop_task)

    # ----------------------------- Convenience methods ----------------------------- #

    def _get_item_widget(self, active_task: ActiveTask) -> QTreeWidgetItem:
        for item_widget, active_task2 in self._active_tasks:
            if active_task == active_task2:
                return item_widget

        # noinspection PyTypeChecker
        return None

    def _get_task(self, item_widget: QTreeWidgetItem) -> ActiveTask:
        for item_widget2, active_task in self._active_tasks:
            if item_widget == item_widget2:
                return active_task

        # noinspection PyTypeChecker
        return None

    def _update_tasks_header(self) -> None:
        if self.main_window.viewer is not None and self.main_window.viewer.current_reporter is not None:
            tasks_count = len(self.main_window.viewer.current_reporter.get_active_tasks())
        else:
            tasks_count = 0

        self.tasks_tree_widget.setHeaderLabel(" Current tasks (%i):" % tasks_count)

    def _update_task_data(self, item_widget: QTreeWidgetItem, active_task: ActiveTask) -> QTreeWidgetItem:
        item_widget.setText(0, active_task.registered_task.name)

        if not item_widget.childCount():
            for index in range(8):
                item_widget.addChild(QTreeWidgetItem([]))

        item_widget.child(0).setText(0, "Task ID: %i" % active_task.task_id)

        item_widget.child(1).setText(0, "Progress: %.1f%%" % active_task.progress)

        elapsed = datetime.timedelta(seconds=active_task.time_elapsed // 1000)
        # FIXME: Use more real time data, queryrates can change in short spans of time
        eta = datetime.timedelta(seconds=((active_task.time_elapsed / max(1, active_task.progress)) *
                                          (100 - active_task.progress)) // 1000)
        item_widget.child(2).setText(0, "Elapsed: %s" % elapsed)
        item_widget.child(3).setText(0, "Remaining: %s" % eta)
        item_widget.child(4).setText(0, "Loaded chunk task: %s" % active_task.loaded_chunk_task)
        item_widget.child(5).setText(0, "Current position: %s" % ("N/A" if not active_task.loaded_chunk_task else
                                                                  "%i, %i" % (active_task.current_position.x,
                                                                              active_task.current_position.z)))

        parameters_item_widget = item_widget.child(6)
        parameters_item_widget.setText(0, "Parameters: %i" % len(active_task.parameters))
        if parameters_item_widget.childCount() != len(active_task.parameters):
            parameters_item_widget.takeChildren()
            for parameter in active_task.parameters:
                parameter_item_widget = QTreeWidgetItem([parameter.param_description.name])
                parameter_item_widget.addChild(QTreeWidgetItem(["Description: %s" % parameter.param_description.description]))

                if parameter.param_description.input_type == RegisteredTask.ParamDescription.InputType.SINGULAR:
                    parameter_item_widget.addChild(QTreeWidgetItem(["Value: %r" % parameter.value]))
                else:
                    values_item_widget = QTreeWidgetItem(["Values: %i" % len(parameter.values)])
                    for value in parameter.values:
                        values_item_widget.addChild(QTreeWidgetItem([repr(value)]))

                parameters_item_widget.addChild(parameter_item_widget)

        results_item_widget = item_widget.child(7)
        results_item_widget.setText(0, "Results: %i" % len(active_task.results))
        if results_item_widget.childCount() != len(active_task.results):  # Only update if we have received new results
            results_item_widget.takeChildren()
            for result in active_task.results:
                results_item_widget.addChild(QTreeWidgetItem([result]))

        return item_widget

    # ----------------------------- General emitters ----------------------------- #

    def _on_task_added(self, active_task: ActiveTask) -> None:
        item_widget = self._update_task_data(QTreeWidgetItem([]), active_task)

        self._active_tasks.append((item_widget, active_task))
        self.tasks_tree_widget.addTopLevelItem(item_widget)

        self._update_tasks_header()

    def _on_task_removed(self, active_task: ActiveTask) -> None:
        item_widget = self._get_item_widget(active_task)
        if item_widget is not None:
            self.tasks_tree_widget.takeTopLevelItem(self.tasks_tree_widget.indexOfTopLevelItem(item_widget))
            self._active_tasks.remove((item_widget, active_task))

        self._update_tasks_header()

    def _on_task_updated(self, active_task: ActiveTask) -> None:
        item_widget = self._get_item_widget(active_task)
        if item_widget is not None:
            # item_widget.takeChildren()
            self._update_task_data(item_widget, active_task)

    def _on_task_result(self, active_task: ActiveTask, result: str) -> None:
        item_widget = self._get_item_widget(active_task)
        if item_widget is not None:
            # item_widget.takeChildren()
            self._update_task_data(item_widget, active_task)

    # ----------------------------- Widget emitters ----------------------------- #

    def _on_item_selection_changed(self) -> None:
        task = self._get_task(self.tasks_tree_widget.currentItem())
        if task is not None:
            self.stop_task_button.setEnabled(True)
        else:
            self.stop_task_button.setEnabled(False)

    def _on_new_task(self, checked: bool) -> None:
        if self.main_window.viewer is None or self.main_window.viewer.current_reporter is None:
            QMessageBox.warning(self, "Error", "No reporter is currently selected.")
            return

        current_reporter = self.main_window.viewer.current_reporter
        if not current_reporter.get_registered_tasks():
            QMessageBox.warning(self, "Error", "Reporter has no registered tasks (this is prolly a bug).")
            return

        NewTaskDialog(self, self.main_window).task_emitter.connect(self._on_start_task)

    def _on_start_task(self, task_name: str, task_params: List[ActiveTask.Parameter]) -> None:
        self._start_task_thread = TasksTab.StartTaskThread(self.main_window, task_name, task_params)
        self._start_task_thread.error_emitter.connect(self._on_start_error)
        self._start_task_thread.start()

    def _on_start_error(self, error: str) -> None:
        QMessageBox.critical(self, "Task Error", error)

    def _on_stop_task(self, checked: bool) -> None:
        if self.main_window.viewer is None or self.main_window.viewer.current_reporter is None:
            QMessageBox.warning(self, "Error", "No reporter is currently selected.")
            return

        task = self._get_task(self.tasks_tree_widget.currentItem())
        if task is not None:
            self._stop_task_thread = TasksTab.StopTaskThread(self.main_window, task)
            self._stop_task_thread.error_emitter.connect(self._on_stop_error)
            self._stop_task_thread.start()

    def _on_stop_error(self, error: str) -> None:
        QMessageBox.critical(self, "Task Error", error)

    # ----------------------------- Threads ----------------------------- #

    class StartTaskThread(QThread):

        error_emitter = pyqtSignal(str)

        def __init__(self, main_window, task_name: str, task_params: List[ActiveTask.Parameter]) -> None:
            super().__init__()

            self.main_window = main_window
            self.task_name = task_name
            self.task_parameters = task_params

        def run(self) -> None:
            try:
                self.main_window.viewer.start_task_raw(self.task_name, self.task_parameters)
            except Exception as error:
                self.error_emitter.emit(repr(error))

    class StopTaskThread(QThread):

        error_emitter = pyqtSignal(str)

        def __init__(self, main_window, active_task: ActiveTask) -> None:
            super().__init__()

            self.main_window = main_window
            self.active_task = active_task

        def run(self) -> None:
            try:
                self.main_window.viewer.stop_task(self.active_task.task_id)
            except Exception as error:
                self.error_emitter.emit(repr(error))
