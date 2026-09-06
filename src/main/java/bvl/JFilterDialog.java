package bvl;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class JFilterDialog extends JDialog {

    private static final long serialVersionUID = -2698691621068951038L;
    private JPanel panelEast;
    private JPanel panelCenter;
    private JPanel panelWest;
    private JList lstAvailable;
    private JList lstToUse;
    private JButton btnAdd;
    private JButton btnRemove;

    public JFilterDialog() {
        setTitle("Editar Filtros");
        initComponents();
    }

    private void initComponents() {
        setSize(400, 300);
        panelWest = new JPanel();
        panelCenter = new JPanel();
        panelEast = new JPanel();
        DefaultListModel model = new DefaultListModel();
        for (Object obj : new Object[]{"YAHOO", "GOOGLE", "STRST"}) {
            model.addElement(obj);
        }
        lstAvailable = new JList(model);
        lstToUse = new JList(new DefaultListModel());
        btnAdd = new JButton(">>");
        btnRemove = new JButton("<<");

        Dimension dim = new Dimension(150, 100);

        btnAdd.addActionListener(new AddActionListener());
        btnRemove.addActionListener(new RemoveActionListener());

        Border border = new EmptyBorder(10, 10, 10, 10);
        panelWest.setLayout(new BorderLayout());
        panelWest.setBorder(border);
        panelWest.add(createScrollPane(lstAvailable, dim), BorderLayout.CENTER);

        panelEast.setLayout(new BorderLayout());
        panelEast.setBorder(border);
        panelEast.add(createScrollPane(lstToUse, dim), BorderLayout.CENTER);

        panelCenter.setBorder(border);
        panelCenter.add(btnAdd);
        panelCenter.add(btnRemove);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(panelWest, BorderLayout.WEST);
        getContentPane().add(panelEast, BorderLayout.EAST);
        getContentPane().add(panelCenter, BorderLayout.CENTER);
    }

    private JScrollPane createScrollPane(JComponent comp, Dimension dim) {
        JScrollPane sp = new JScrollPane(comp);
        sp.setPreferredSize(dim);
        return sp;
    }

    class AddActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] selIndexs = lstAvailable.getSelectedIndices();
            for (int index : selIndexs) {
                DefaultListModel model1 = (DefaultListModel) lstToUse.getModel();
                DefaultListModel model2 = (DefaultListModel) lstAvailable.getModel();
                if (!model1.contains(model2.getElementAt(index))) {
                    model1.addElement(model2.getElementAt(index));
                }
            }
        }
    }

    class RemoveActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] selIndexs = lstToUse.getSelectedIndices();
            for (int i = selIndexs.length - 1; i >= 0; i--) {
                ((DefaultListModel) lstToUse.getModel()).remove(selIndexs[i]);
            }
        }
    }

}
