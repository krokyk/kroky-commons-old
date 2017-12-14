/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.kroky.common.tables;

import com.citra.editors.DateEditor;
import com.citra.table.AdvancedJTable;
import java.awt.Component;
import java.awt.Rectangle;
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
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import org.jdesktop.swingx.table.NumberEditorExt;
import org.kroky.common.utils.Utils;

/**
 *
 * @author Kroky
 */
public class GenericTable extends AdvancedJTable {

    private List<SortKey> defaultSortKeys;

    public GenericTable(TableModel model) {
        super(model);
    }

    public GenericTable(TableModel model, int defaultSortColumn, SortOrder defaultSortOrder) {
        super(model);
        init(defaultSortColumn, defaultSortOrder);
    }

    private void init(int defaultSortColumn, SortOrder defaultSortOrder) {
        setAutoCreateRowSorter(true);
        if (defaultSortColumn >= 0 && defaultSortColumn < dataModel.getColumnCount()) {
            List<RowSorter.SortKey> sortKeys = new ArrayList<>();
            sortKeys.add(new RowSorter.SortKey(defaultSortColumn, defaultSortOrder == null ? SortOrder.ASCENDING : defaultSortOrder));
            defaultSortKeys = sortKeys;
        }

        setNonContiguousCellSelection(false);
        setCellSelectionEnabled(false);

        setDefaultEditor(String.class, new BasicTableCellEditor());
        setDefaultEditor(Double.class, new NumberEditorExt(Utils.TWO_DECIMAL_FORMAT));

        setDefaultRenderer(Timestamp.class, new DateTimeRenderer());
        setDefaultRenderer(Double.class, new DoubleRenderer(Utils.TWO_DECIMAL_FORMAT));

        //It's a bug in the synth-installed renderer, quick hack is to force the rendering checkbox opacity to true
        ((JComponent) getDefaultRenderer(Boolean.class)).setOpaque(true);

        //apply the default sorting
        resetSortingToDefault();
    }

    public final void resetSortingToDefault() {
        getRowSorter().setSortKeys(defaultSortKeys);
    }

    public void scrollCellToVisible(int row, int column) {
        Rectangle cellRect = getCellRect(row, column, false);
        scrollRectToVisible(cellRect);
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        styleModel.applyStyles(c, this, row, column);
        return c;
    }

    @Override
    public void setDefaultEditor(Class<?> columnClass, TableCellEditor editor) {
        super.setDefaultEditor(columnClass, editor);
        JComponent editorComponent = null;
        if (editor instanceof BasicTableCellEditor) {
            editorComponent = ((BasicTableCellEditor) editor).getTextField();
        } else if (editor instanceof NumberEditorExt) {
            editorComponent = ((NumberEditorExt) editor).getComponent();
        }
        if (editorComponent != null) {
            editorComponent.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "moveDown");
            editorComponent.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "moveUp");
            editorComponent.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "moveRight");
            editorComponent.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK), "moveLeft");
            editorComponent.getActionMap().put("moveDown", new MoveDownAction(editorComponent));
            editorComponent.getActionMap().put("moveUp", new MoveUpAction(editorComponent));
            editorComponent.getActionMap().put("moveRight", new MoveRightAction(editorComponent));
            editorComponent.getActionMap().put("moveLeft", new MoveLeftAction(editorComponent));
        }
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

        private JComponent editorComponent;

        public NextCellAction(JComponent editorComponent) {
            this.editorComponent = editorComponent;
        }

        protected abstract int[] getNextEditableCell(int row, int column);

        protected void editCell(int row, int column) {
            getSelectionModel().setSelectionInterval(row, row);
            scrollCellToVisible(row, column);
            editCellAt(row, column);
            editorComponent.requestFocusInWindow();
        }
    }

    private class MoveDownAction extends NextCellAction {

        private MoveDownAction(JComponent editorComponent) {
            super(editorComponent);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (getRowCount() == 1) {
                return;
            }
            int[] nextCellCoord = getNextEditableCell(getEditingRow() + 1, getEditingColumn());
            editCell(nextCellCoord[0], nextCellCoord[1]);
        }

        @Override
        protected int[] getNextEditableCell(int row, int column) {
            if (row > getRowCount() - 1) {
                row = 0;
            }
            while (!isCellEditable(row, column)) {
                row++;
                if (row > getRowCount() - 1) {
                    row = 0;
                }
            }
            if (getCellEditor(row, column) instanceof DateEditor) {
                return getNextEditableCell(row + 1, column);
            } else {
                return new int[]{row, column};
            }
        }
    }

    private class MoveUpAction extends NextCellAction {

        private MoveUpAction(JComponent editorComponent) {
            super(editorComponent);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (getRowCount() == 1) {
                return;
            }
            int[] nextCellCoord = getNextEditableCell(getEditingRow() - 1, getEditingColumn());
            editCell(nextCellCoord[0], nextCellCoord[1]);
        }

        @Override
        protected int[] getNextEditableCell(int row, int column) {
            if (row < 0) {
                row = getRowCount() - 1;
            }
            while (!isCellEditable(row, column)) {
                row--;
                if (row < 0) {
                    row = getRowCount() - 1;
                }
            }
            if (getCellEditor(row, column) instanceof DateEditor) {
                return getNextEditableCell(row - 1, column);
            } else {
                return new int[]{row, column};
            }
        }
    }

    private class MoveRightAction extends NextCellAction {

        private MoveRightAction(JComponent editorComponent) {
            super(editorComponent);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (getColumnCount() == 1) {
                return;
            }
            int[] nextCellCoord = getNextEditableCell(getEditingRow(), getEditingColumn() + 1);
            editCell(nextCellCoord[0], nextCellCoord[1]);
        }

        @Override
        protected int[] getNextEditableCell(int row, int column) {
            if (column > getColumnCount() - 1) {
                column = 0;
            }
            while (!isCellEditable(row, column)) {
                column++;
                if (column > getColumnCount() - 1) {
                    column = 0;
                }
            }
            if (getCellEditor(row, column) instanceof DateEditor) {
                return getNextEditableCell(row, column + 1);
            } else {
                return new int[]{row, column};
            }
        }
    }

    private class MoveLeftAction extends NextCellAction {

        private MoveLeftAction(JComponent editorComponent) {
            super(editorComponent);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (getColumnCount() == 1) {
                return;
            }
            int[] nextCellCoord = getNextEditableCell(getEditingRow(), getEditingColumn() - 1);
            editCell(nextCellCoord[0], nextCellCoord[1]);
        }

        @Override
        protected int[] getNextEditableCell(int row, int column) {
            if (column < 0) {
                column = getColumnCount() - 1;
            }
            while (!isCellEditable(row, column)) {
                column--;
                if (column < 0) {
                    column = getColumnCount() - 1;
                }
            }
            if (getCellEditor(row, column) instanceof DateEditor) {
                return getNextEditableCell(row, column - 1);
            } else {
                return new int[]{row, column};
            }
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
            delegate = new DefaultCellEditor.EditorDelegate() {
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

        private final NumberFormat nf;

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
