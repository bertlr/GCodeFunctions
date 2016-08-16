/*
 * Copyright (C) 2016 by Herbert Roider <herbert@roider.at>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.roiderh.functionparser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.swing.text.*;
import javax.swing.JLabel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

/**
 *
 * @author Herbert Roider <herbert@roider.at>
 */
public class DialogBackTranslationFunction extends javax.swing.JDialog implements ActionListener, FocusListener {


    private FunctionConf fc = null;
    /**
     * Field with the generated g-Code:
     */
    public String g_code;
    public boolean canceled = true;
    private java.util.ArrayList<JTextField> jFormattedFields;
    int machine = 0;

    /**
     * Creates new form DialogBackTranslationFunction
     */
    public DialogBackTranslationFunction(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();

    }

    public DialogBackTranslationFunction(String _g_code, FunctionConf[] _fc, java.awt.Frame parent, boolean modal) throws Exception {
        super(parent, modal);
        initComponents();
        InputStream is = new ByteArrayInputStream(_g_code.getBytes());
        gcodereader gr = new gcodereader();

        java.util.ArrayList<String> values = new java.util.ArrayList<>();

        jButtonCancel.setActionCommand("cancel");
        jButtonCancel.addActionListener(this);

        jButtonOk.setActionCommand("ok");
        jButtonOk.addActionListener(this);

        descriptionArea.setContentType("text/html");
        descriptionArea.setEditable(false);
        HTMLEditorKit kit = new HTMLEditorKit();
        descriptionArea.setEditorKit(kit);
        StyleSheet styleSheet = kit.getStyleSheet();
        styleSheet.addRule("body {color:#0000ff; font-family:times; margin: 0px; font: 10px; }");
        styleSheet.addRule("pre {font-family: monospace; color : black; background-color : #f0f0f0; }");
        styleSheet.addRule("td, th {padding: 1px; border: 1px solid #ddd; }");
        Document doc = kit.createDefaultDocument();
        descriptionArea.setDocument(doc);

        jFormattedFields = new java.util.ArrayList<>();

        gr.read(is);

        // find the config:
        for (int i = 0; i < _fc.length; i++) {
            if (_fc[i].name.compareTo(gr.cycle) == 0) {
                fc = _fc[i];
                break;
            }

        }
        if (this.fc == null) {
            throw new Exception("no matching Cycle Config found");

        }

        this.setTitle(fc.title + " " + fc.name);

        machine = gr.machine;
        if (gr.machine == 0) {
            // spinner (840D)

            // Fill missing fields with default value:
            for (int i = 0; i < fc.arg.size(); i++) {
                String v = "";
                if (i < gr.arguments.size()) {
                    v = gr.arguments.get(i);
                } else {
                    v = fc.arg.get(i).defaultval;
                }
                values.add(v);

            }

        } else {
            // emco 810
            // Fill missing fields with default value:
            for (int i = 0; i < fc.arg.size(); i++) {
                String v = "";
                String name = fc.arg.get(i).name;
                String key_s = name.substring(1, name.length());
                int key = Integer.parseInt(key_s);

                if (gr.R.containsKey(key)) {
                    v = String.valueOf(gr.R.get(key));
                } else {
                    v = fc.arg.get(i).defaultval;
                }

                values.add(v);

            }

        }
        for (int i = 0; i < fc.arg.size(); i++) {

            JTextField f = new JTextField();
            f.setText(values.get(i));
            jFormattedFields.add(f);
        }

        for (int i = 0; i < fc.arg.size(); i++) {
            jFormattedFields.get(i).addFocusListener(this);
            jFormattedFields.get(i).addActionListener(this);
            jFormattedFields.get(i).setPreferredSize(new Dimension(80, 16));
            jFormattedFields.get(i).setMinimumSize(new Dimension(60, 16));
        }

        for (int i = 0; i < fc.arg.size(); i++) {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = i;

            listPane.add(new JLabel(fc.arg.get(i).name), c);
            c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.gridy = i;
            c.insets = new Insets(0, 5, 0, 5);  // padding
            listPane.add(jFormattedFields.get(i), c);

            String desc = fc.arg.get(i).desc;
            if (desc.length() > 35) {
                desc = desc.substring(0, 35);
            }
            int breakpos = desc.indexOf('\n');
            if (breakpos > 0) {
                desc = desc.substring(0, breakpos);
            }

            if (fc.arg.get(i).desc.length() > desc.length()) {
                desc += "...";
            }
            c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 2;
            c.gridy = i;
            listPane.add(new JLabel(desc), c);
        }
 
        pack();
    }
    //Handle clicks on the Ok and Cancel buttons.

    public void actionPerformed(ActionEvent e) {
        if ("ok".equals(e.getActionCommand())) {
            java.util.ArrayList<String> args = new java.util.ArrayList<>();

            for (int i = 0; i < fc.arg.size(); i++) {
                if (fc.arg.get(i).type.compareTo("string") == 0) {
                    args.add("\"" + jFormattedFields.get(i).getText().trim() + "\"");
                } else {
                    args.add(jFormattedFields.get(i).getText().trim());
                }

            }

            if (machine == 0) {
                // spinner
                g_code = fc.name + "(";
                g_code += String.join(",", args);
                g_code += ")\n";
            } else if (machine == 1) {
                // emco:
                g_code = "";
                for (int i = 0; i < fc.arg.size(); i++) {
                    if (i % 8 == 0 && i > 0) {
                        g_code += "\n";
                    }
                    g_code += fc.arg.get(i).name + "=";
                    g_code += args.get(i);
                    g_code += " ";

                }
                g_code += "\nL" + fc.name + " P1\n";
            }

            canceled = false;
            this.setVisible(false);
        } else if ("cancel".equals(e.getActionCommand())) {
            this.setVisible(false);
        } else {

        }

    }

    public void focusGained(FocusEvent e) {
        System.out.println("focusGained");
        JTextField source = (JTextField) e.getSource();
        for (int i = 0; i < fc.arg.size(); i++) {
            if (source == jFormattedFields.get(i)) {
                System.out.println("found: " + i);
                descriptionArea.setText(fc.arg.get(i).desc);
                return;
            }

        }

    }

    public void focusLost(FocusEvent e) {
        //System.out.println("focusLost");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButtonOk = new javax.swing.JButton();
        jButtonCancel = new javax.swing.JButton();
        listPane = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        descriptionArea = new javax.swing.JEditorPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        org.openide.awt.Mnemonics.setLocalizedText(jButtonOk, org.openide.util.NbBundle.getMessage(DialogBackTranslationFunction.class, "DialogBackTranslationFunction.jButtonOk.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jButtonCancel, org.openide.util.NbBundle.getMessage(DialogBackTranslationFunction.class, "DialogBackTranslationFunction.jButtonCancel.text")); // NOI18N

        listPane.setLayout(new java.awt.GridBagLayout());

        jScrollPane1.setViewportView(descriptionArea);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButtonOk)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonCancel))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(listPane, javax.swing.GroupLayout.DEFAULT_SIZE, 367, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 550, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(listPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 473, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonCancel)
                    .addComponent(jButtonOk))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(DialogBackTranslationFunction.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(DialogBackTranslationFunction.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(DialogBackTranslationFunction.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(DialogBackTranslationFunction.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                DialogBackTranslationFunction dialog = new DialogBackTranslationFunction(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JEditorPane descriptionArea;
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonOk;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel listPane;
    // End of variables declaration//GEN-END:variables
}
