#!/usr/bin/env python3

import re
import string
from typing import List

from PyQt5.QtCore import Qt, QMetaObject, pyqtSignal
from PyQt5.QtGui import QCloseEvent
from PyQt5.QtWidgets import QDialog, QWidget, QVBoxLayout, QLabel, QHBoxLayout, QComboBox, QTableWidget, QMessageBox, \
    QDialogButtonBox, QTableWidgetItem, QHeaderView

from ..util import match_all
from ...util import DataType, RegisteredTask, ActiveTask, Angle, Position, ChunkPosition, Priority, Dimension


class NewTaskDialog(QDialog):

    INSTANCE = None

    task_emitter = pyqtSignal(str, list)

    boolean = re.compile("(True|False)")
    integer = re.compile("(-?)[0-9]+")
    floating = re.compile(r"((-?)[0-9]*\.[0-9]+)|(%s)" % integer.pattern)
    position = re.compile(r"\(?(?P<x>%s),( *)(?P<y>%s),( *)(?P<z>%s)\)?" % (floating.pattern, floating.pattern,
                                                                            floating.pattern))
    angle = re.compile(r"\(?(?P<yaw>%s),( *)(?P<pitch>%s)\)?" % (floating.pattern, floating.pattern))
    chunk_position = re.compile(r"\(?(?P<x>%s),( *)(?P<z>%s)\)?" % (integer.pattern, integer.pattern))
    string_ = re.compile("[\"']?(?P<value>([A-Za-z0-9]|[%s])*)[\"']?" %
                         string.punctuation.replace("\"", "").replace("'", ""))
    dimension = re.compile("(OVERWORLD|NETHER|END)")
    priority = re.compile("(USER|HIGH|MEDIUM|LOW)")

    def __init__(self, parent: QWidget, main_window) -> None:
        if NewTaskDialog.INSTANCE is not None:
            NewTaskDialog.INSTANCE.setWindowState(NewTaskDialog.INSTANCE.windowState() & ~Qt.WindowMinimized |
                                                  Qt.WindowActive)
            NewTaskDialog.INSTANCE.activateWindow()
            return

        super().__init__(parent)
        NewTaskDialog.INSTANCE = self

        self.main_window = main_window

        self.setObjectName("new_task_dialog")

        self.resize(1, 1)

        self.main_layout = QVBoxLayout(self)
        self.main_layout.setSpacing(6)
        self.main_layout.setContentsMargins(6, 6, 6, 6)

        self.task_select_layout = QHBoxLayout()
        self.task_select_label = QLabel(self)

        self.task_select_layout.addWidget(self.task_select_label)

        self.task_select_combo_box = QComboBox(self)
        self.task_select_layout.addWidget(self.task_select_combo_box)

        self.task_select_layout.setStretch(1, 1)
        self.main_layout.addLayout(self.task_select_layout)

        self.note_label = QLabel(self)
        self.note_label.setAlignment(Qt.AlignCenter)
        self.main_layout.addWidget(self.note_label)

        self.parameters_table_widget = QTableWidget(self)
        self.parameters_table_widget.setColumnCount(2)
        self.parameters_table_widget.horizontalHeader().setSectionResizeMode(0, QHeaderView.ResizeToContents)
        self.parameters_table_widget.horizontalHeader().setSectionResizeMode(1, QHeaderView.Stretch)
        self.main_layout.addWidget(self.parameters_table_widget)

        self.dialog_button_box = QDialogButtonBox(self)
        self.dialog_button_box.setStandardButtons(QDialogButtonBox.Ok | QDialogButtonBox.Cancel)
        self.main_layout.addWidget(self.dialog_button_box)

        self.setWindowTitle("New Task")
        self.task_select_label.setText("Task to start:")
        self.note_label.setText("Note: raw parameter values may be complex.")
        self.parameters_table_widget.setHorizontalHeaderLabels(["Name", "Value"])

        for registered_task in self.main_window.viewer.current_reporter.get_registered_tasks():
            self.task_select_combo_box.addItem(registered_task.name)

        self._populate_parameters()

        self.main_window.disconnect_emitter.connect(self._on_disconnect)
        self.main_window.reporter_unselected_emitter.connect(self._on_deselect_reporter)

        self.task_select_combo_box.currentTextChanged.connect(lambda text: self._populate_parameters())
        self.dialog_button_box.accepted.connect(self._on_accepted)
        self.dialog_button_box.rejected.connect(self.close)

        QMetaObject.connectSlotsByName(self)

        self.show()

    def closeEvent(self, close_event: QCloseEvent) -> None:
        NewTaskDialog.INSTANCE = None

        self.main_window.disconnect_emitter.disconnect(self._on_disconnect)
        self.main_window.reporter_unselected_emitter.disconnect(self._on_deselect_reporter)

        super().closeEvent(close_event)

    def _populate_parameters(self) -> None:
        try:
            registered_task = self.main_window.viewer.current_reporter.get_registered_task(self.task_select_combo_box.currentText())
        except LookupError:
            QMessageBox.critical(self.main_window, "Error", "Couldn't find task (prolly a bug).")
            self.close()
            return

        param_descriptions = registered_task.get_param_descriptions()

        self.parameters_table_widget.clearSpans()
        self.parameters_table_widget.setRowCount(len(param_descriptions))

        for index, param_description in enumerate(param_descriptions):
            item_widget = QTableWidgetItem(param_description.name)
            item_widget.setToolTip(param_description.description)
            item_widget.setFlags(item_widget.flags() & ~Qt.ItemIsEditable)

            self.parameters_table_widget.setItem(index, 0, item_widget)

            item_widget = QTableWidgetItem("")

            array_input = param_description.input_type == RegisteredTask.ParamDescription.InputType.ARRAY
            tool_tip = "%%s type, array: %s" % array_input

            if param_description.data_type == DataType.POSITION:
                item_widget.setText("%s0.0, 0.0, 0.0%s" % (("(", ")") if array_input else ("", "")))
                tool_tip = tool_tip % "Position (not chunk position)"

            elif param_description.data_type == DataType.ANGLE:
                item_widget.setText("%s0.0, 0.0%s" % (("(", ")") if array_input else ("", "")))
                tool_tip = tool_tip % "Angle"

            elif param_description.data_type == DataType.CHUNK_POSITION:
                item_widget.setText("%s0, 0%s" % (("(", ")") if array_input else ("", "")))
                tool_tip = tool_tip % "Chunk position"

            elif param_description.data_type == DataType.DIMENSION:
                item_widget.setText("nether")
                tool_tip = tool_tip % "Dimension (values: OVERWORLD, NETHER, END)"

            elif param_description.data_type == DataType.PRIORITY:
                item_widget.setText("low")
                tool_tip = tool_tip % "Priority (values: LOW, MEDIUM, HIGH, USER)"

            elif param_description.data_type == DataType.STRING:
                item_widget.setText("\"\"" if array_input else "")
                tool_tip = tool_tip % "String"

            elif param_description.data_type == DataType.INTEGER:
                item_widget.setText("0")
                tool_tip = tool_tip % "Integer"

            elif param_description.data_type == DataType.FLOAT:
                item_widget.setText("0.0")
                tool_tip = tool_tip % "Float"

            elif param_description.data_type == DataType.BOOLEAN:
                item_widget.setText("false")
                tool_tip = tool_tip % "Boolean"

            item_widget.setToolTip(tool_tip)

            self.parameters_table_widget.setItem(index, 1, item_widget)

    @property
    def _construct_parameters(self) -> List[ActiveTask.Parameter]:
        registered_task = self.main_window.viewer.current_reporter.get_registered_task(self.task_select_combo_box.currentText())
        parameters = []

        for index in range(self.parameters_table_widget.rowCount()):
            param_name = self.parameters_table_widget.item(index, 0).text()
            param_value = self.parameters_table_widget.item(index, 1).text()

            param_description = registered_task.get_param_description(param_name)
            array_input = param_description.input_type == RegisteredTask.ParamDescription.InputType.ARRAY

            values = []

            if param_description.data_type == DataType.POSITION:
                matches = match_all(re.compile("%s,?( *)" % self.position.pattern), param_value)
                if not matches:
                    raise ValueError("Expected position for param %r, this should be (<x>, <y>, <z>) where x, y and z are floats." % param_name)
                for match in matches:
                    values.append(Position(float(match.group("x")), float(match.group("y")), float(match.group("z"))))

            elif param_description.data_type == DataType.ANGLE:
                matches = match_all(re.compile("%s,?( *)" % self.angle.pattern), param_value)
                if not matches:
                    raise ValueError("Expected angle for param %r, this should be (<yaw>, <pitch>) where yaw and pitch are floats." % param_name)
                for match in matches:
                    values.append(Angle(float(match.group("yaw")), float(match.group("pitch"))))

            elif param_description.data_type == DataType.CHUNK_POSITION:
                matches = match_all(re.compile("%s,?( *)" % self.chunk_position.pattern), param_value)
                if not matches:
                    raise ValueError("Expected chunk position for param %r, this should be (<x>, <z>) where x and z are ints." % param_name)
                for match in matches:
                    values.append(ChunkPosition(int(match.group("x")), int(match.group("z"))))

            elif param_description.data_type == DataType.DIMENSION:
                matches = match_all(re.compile("(?P<value>%s),?( *)" % self.dimension.pattern), param_value.upper())
                if not matches:
                    raise ValueError("Expected dimension for param %r, this should be OVERWORLD, NETHER or END." % param_name)
                for match in matches:
                    values.append(Dimension.__dict__[match.group("value")] - 1)  # TODO: Proper conversion between enum and mc types

            elif param_description.data_type == DataType.PRIORITY:
                matches = match_all(re.compile("(?P<value>%s),?( *)" % self.priority.pattern), param_value.upper())
                if not matches:
                    raise ValueError("Expected priority for param %r, this should be USER, HIGH, MEDIUM or LOW." % param_name)
                for match in matches:
                    values.append(Priority.__dict__[match.group("value")])

            elif param_description.data_type == DataType.STRING:
                matches = match_all(re.compile("%s,?( *)" % self.string_.pattern), param_value)
                if not matches:
                    raise ValueError("Expected string for param %r, this should be \"<value>\"." % param_name)
                for match in matches:
                    values.append(match.group("value"))

            elif param_description.data_type == DataType.INTEGER:
                matches = match_all(re.compile("(?P<value>%s),?( *)" % self.integer.pattern), param_value)
                if not matches:
                    raise ValueError("Expected int for param %r." % param_name)
                for match in matches:
                    values.append(int(match.group("value")))

            elif param_description.data_type == DataType.FLOAT:
                matches = match_all(re.compile("(?P<value>%s),?( *)" % self.floating.pattern), param_value)
                if not matches:
                    raise ValueError("Expected float for param %r." % param_name)
                for match in matches:
                    values.append(float(match.group("value")))

            elif param_description.data_type == DataType.BOOLEAN:
                matches = match_all(re.compile("(?P<value>%s),?( *)" % self.boolean.pattern), param_value.capitalize())
                if not matches:
                    raise ValueError("Expected boolean for param %r, this should be True or False." % param_name)
                for match in matches:
                    values.append(bool(match.group("value")))

            if len(values) > 1 and param_description.input_type == RegisteredTask.ParamDescription.InputType.SINGULAR:
                raise ValueError("Expected singular value for param %r, not array." % param_name)

            parameters.append(ActiveTask.Parameter(param_description, *values))

        return parameters

    def _on_disconnect(self) -> None:
        self.close()
        QMessageBox.critical(self.main_window, "Error", "Disconnected from the server.")

    def _on_deselect_reporter(self) -> None:
        self.close()
        QMessageBox.critical(self.main_window, "Error", "Lost current reporter.")

    def _on_accepted(self) -> None:
        try:
            parameters = self._construct_parameters
        except Exception as error:
            if isinstance(error, ValueError):
                QMessageBox.warning(self, "Error", str(error))
            else:
                QMessageBox.warning(self, "Error", repr(error))
            return

        self.task_emitter.emit(self.task_select_combo_box.currentText(), parameters)
        self.close()
