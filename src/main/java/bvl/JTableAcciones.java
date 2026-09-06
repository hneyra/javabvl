package bvl;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.util.Comparator;
import java.util.Vector;

public class JTableAcciones extends JTable {

    private static final long serialVersionUID = 5329800686686622736L;
    private JTextField filterText;

    public JTableAcciones(JTextField filterText2) {
        setFilterText(filterText2);
    }

    @Override
    public void setModel(TableModel dataModel) {
        super.setModel(dataModel);
        TableRowSorter<TableModel> sorter = new AccionesTableRowSorter<TableModel>(getModel());
        // sorter.setComparator(arg0, arg1)
        setRowSorter(sorter);
    }

    public JTextField getFilterText() {
        return filterText;
    }

    public void setFilterText(JTextField filterText) {
        this.filterText = filterText;
        filterText.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                ((AccionesTableRowSorter) getRowSorter()).newFilter();
            }

            public void insertUpdate(DocumentEvent e) {
                ((AccionesTableRowSorter) getRowSorter()).newFilter();
            }

            public void removeUpdate(DocumentEvent e) {
                ((AccionesTableRowSorter) getRowSorter()).newFilter();
            }
        });
    }

    class AccionesTableRowSorter<T extends TableModel> extends TableRowSorter<T> {

        public final Comparator<String> strComparator = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                if (o1 == null) {
                    o1 = "";
                }
                return o1.compareTo(o2);
            }
        };
        public final Comparator<Comparable<?>> numComparator = new Comparator<Comparable<?>>() {
            @Override
            public int compare(Comparable<?> o1, Comparable<?> o2) {
                if (o1 == null || o1.toString() == "") {
                    o1 = new Integer(0);
                }
                if (o2 == null || o2.toString() == "") {
                    o2 = new Integer(0);
                }
                Double d1 = new Double(o1.toString());
                Double d2 = new Double(o2.toString());
                return d1.compareTo(d2);
            }
        };

        public AccionesTableRowSorter(T model) {
            super(model);
            RowFilter<TableModel, Integer> rf = null;
            newFilter();
        }

        private void newFilter() {
            RowFilter<TableModel, Object> rf = null;
            // If current expression doesn't parse, don't update.
            try {
                if (getFilterText() != null) {
                    rf = RowFilter.regexFilter(getFilterText().getText(), 0);
                }
            } catch (java.util.regex.PatternSyntaxException e) {
                return;
            }
            setRowFilter(rf);
        }

        @Override
        public Comparator<?> getComparator(int column) {
            int i = column;
            try {
                if (i == 0 || i == 2 || i == 11 || i == 12) {
                    return strComparator;
                }
                // else if(i==2)
                return numComparator;
            } catch (Exception e) {
                System.out.println("index=" + e);
                e.printStackTrace();
            }
            return super.getComparator(column);
        }
    }

}

class AccionesTableModel extends DefaultTableModel {

    private static final long serialVersionUID = 6984478166522122743L;

    public AccionesTableModel(Vector<Vector<Object>> data, Vector<Object> title) {
        super(data, title);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        int i = columnIndex;
        if (i == 0 || i == 2 || i == 11 || i == 12) {
            return String.class;
        }
        return Number.class;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
}
