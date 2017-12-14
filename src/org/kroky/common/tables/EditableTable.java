/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.kroky.common.tables;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.table.NumberEditorExt;
import org.kroky.common.utils.Utils;

/**
 *
 * @author Kroky
 */
public class EditableTable extends JXTable {

    private List<SortKey> defaultSortKeys;

    public EditableTable(TableModel model) {
        this(model, -1, null);
    }

    public EditableTable(TableModel model, int defaultSortColumn, SortOrder defaultSortOrder) {
        super(model);
        setTerminateEditOnFocusLost(true);
        setAutoStartEditOnKeyStroke(false);
        setDefaultRenderer(Timestamp.class, new DateTimeRenderer());
        if (defaultSortColumn >= 0 && defaultSortColumn < model.getColumnCount()) {
            List<RowSorter.SortKey> sortKeys = new ArrayList<>();
            sortKeys.add(new RowSorter.SortKey(defaultSortColumn, defaultSortOrder == null ? SortOrder.ASCENDING : defaultSortOrder));
            defaultSortKeys = sortKeys;
        }
        BasicTableCellEditor stringEditor = new BasicTableCellEditor();
        stringEditor.getTextField().getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "moveDown");
        stringEditor.getTextField().getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "moveUp");
        stringEditor.getTextField().getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "moveRight");
        stringEditor.getTextField().getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK), "moveLeft");
        stringEditor.getTextField().getActionMap().put("moveDown", new MoveDownAction());
        stringEditor.getTextField().getActionMap().put("moveUp", new MoveUpAction());
        stringEditor.getTextField().getActionMap().put("moveRight", new MoveRightAction());
        stringEditor.getTextField().getActionMap().put("moveLeft", new MoveLeftAction());
        setDefaultEditor(String.class, stringEditor);

        DateTimeAsStringCellEditor dateEditor = new DateTimeAsStringCellEditor();
        dateEditor.getTextField().getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "moveDown");
        dateEditor.getTextField().getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "moveUp");
        dateEditor.getTextField().getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "moveRight");
        dateEditor.getTextField().getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK), "moveLeft");
        dateEditor.getTextField().getActionMap().put("moveDown", new MoveDownAction());
        dateEditor.getTextField().getActionMap().put("moveUp", new MoveUpAction());
        dateEditor.getTextField().getActionMap().put("moveRight", new MoveRightAction());
        dateEditor.getTextField().getActionMap().put("moveLeft", new MoveLeftAction());
        setDefaultEditor(Timestamp.class, dateEditor);

        setDefaultEditor(Double.class, new NumberEditorExt(Utils.TWO_DECIMAL_FORMAT));

        setDefaultRenderer(Timestamp.class, new DateTimeRenderer());
        setDefaultRenderer(Double.class, new DoubleRenderer(Utils.TWO_DECIMAL_FORMAT));

        //It's a bug in the synth-installed renderer, quick hack is to force the rendering checkbox opacity to true
        ((DefaultTableRenderer) getDefaultRenderer(Boolean.class)).getComponentProvider().getRendererComponent(null).setOpaque(true);

        //default sorting
        resetSortingToDefault();
    }

    public final void resetSortingToDefault() {
        getRowSorter().setSortKeys(defaultSortKeys);
    }

    public int[] convertRowIndexesToModel(int[] indexes) {
        int[] converted = new int[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
            converted[i] = convertRowIndexToModel(indexes[i]);
        }
        return converted;
    }

    //<editor-fold defaultstate="collapsed" desc="Keyboard actions...">
    private abstract class NextCellAction extends AbstractAction {

        protected void editCell(int rowIndex, int columnIndex) {
            int lastRowIndex = getRowCount() - 1;
            int lastColumnIndex = getColumnCount() - 1;
            if (rowIndex > lastRowIndex) {
                rowIndex = 0;
            }
            if (rowIndex < 0) {
                rowIndex = lastRowIndex;
            }
            if (columnIndex > lastColumnIndex) {
                columnIndex = 0;
            }
            if (columnIndex < 0) {
                columnIndex = lastColumnIndex;
            }
            getSelectionModel().setSelectionInterval(rowIndex, rowIndex);
            editCellAt(rowIndex, columnIndex);
            scrollCellToVisible(rowIndex, columnIndex);
            ((BasicTableCellEditor) getCellEditor(rowIndex, columnIndex)).getComponent().requestFocusInWindow();
        }
    }

    private class MoveDownAction extends NextCellAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (getRowCount() == 1) {
                return;
            }
            int row = getEditingRow() + 1;
            int column = getEditingColumn();
            editCell(row, column);
        }
    }

    private class MoveUpAction extends NextCellAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (getRowCount() == 1) {
                return;
            }
            int row = getEditingRow() - 1;
            int column = getEditingColumn();
            editCell(row, column);
        }
    }

    private class MoveRightAction extends NextCellAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (getColumnCount() == 1) {
                return;
            }
            int row = getEditingRow();
            int column = getEditingColumn() + 1;
            editCell(row, column);
        }
    }

    private class MoveLeftAction extends NextCellAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (getColumnCount() == 1) {
                return;
            }
            int row = getEditingRow();
            int column = getEditingColumn() - 1;
            editCell(row, column);
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="BasicTableCellEditor">
    protected class BasicTableCellEditor extends DefaultCellEditor {

        protected JTextField component;
        protected boolean selectaAll = true;

        public BasicTableCellEditor(final JTextField component) {
            super(component);
            component.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 1));
            component.addFocusListener(new FocusAdapter() {

                @Override
                public void focusGained(FocusEvent e) {
                    if (selectaAll) {
                        component.selectAll();
                    }
                }

            });
            this.component = component;
        }

        public BasicTableCellEditor() {
            this(new JTextField());
        }

        public JTextField getTextField() {
            return (JTextField) component;
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="DateTimeAsStringCellEditor">
    class DateTimeAsStringCellEditor extends BasicTableCellEditor {

        public DateTimeAsStringCellEditor() {
            delegate = new EditorDelegate() {
                @Override
                public void setValue(Object value) {
                    component.setText(Utils.formatDateTime(value));
                }
            };
            component.addActionListener(delegate);
        }

        @Override
        public Object getCellEditorValue() {
            return Utils.stringToTimestamp(component.getText());
        }

        // This method is called when a cell value is edited by the user.
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int rowIndex, int vColIndex) {

            String strValue = Utils.formatDateTime(value);
            if (strValue != null) {
                component.setText(strValue);
            }

            return super.getTableCellEditorComponent(table, value, isSelected, rowIndex, vColIndex);
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="DateTimeRenderer">
    class DateTimeRenderer extends DefaultTableCellRenderer {

        private final DateFormat DF;

        public DateTimeRenderer() {
            this(null);
        }

        public DateTimeRenderer(DateFormat df) {
            if (df == null) {
                DF = Utils.DATE_TIME_FORMAT;
            } else {
                DF = df;
            }
            setHorizontalAlignment(SwingConstants.LEFT);
        }

        @Override
        public void setValue(Object value) {
            try {
                if (value != null) {
                    value = DF.format(value);
                }
            } catch (IllegalArgumentException e) {
            }

            super.setValue(value);
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="DoubleRenderer">
    class DoubleRenderer extends DefaultTableCellRenderer {

        private NumberFormat nf;

        public DoubleRenderer(NumberFormat nf) {
            this.nf = nf;
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        public void setValue(Object value) {
            try {
                if (value != null) {
                    value = nf.format(value);
                }
            } catch (IllegalArgumentException e) {
            }

            super.setValue(value);
        }
    }
    //</editor-fold>
}
