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
package org.roiderh.gcodefunctions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
import javax.swing.text.JTextComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.io.Reader;
import org.openide.awt.ActionReferences;
import org.roiderh.functionparser.FunctionConf;
import org.roiderh.functionparser.DialogNewFunction;
import org.roiderh.functionparser.DialogBackTranslationFunction;

@ActionID(
        category = "File",
        id = "org.roiderh.gcodefunctions.GCodeFunctionsActionListener"
)
@ActionRegistration(
        iconBase = "org/roiderh/gcodefunctions/hi22-wizard.png",
        displayName = "#CTL_GCodeFunctionsActionListener"
)
@ActionReferences({
    @ActionReference(path = "Toolbars/File", position = 0),
    @ActionReference(path = "Editors/text/plain/Popup"),
    @ActionReference(path = "Editors/text/x-nc/Popup")
})

public final class GCodeFunctionsActionListener implements ActionListener {

    //private LineCookie context;
    //private JTextComponent editor;
    // private StyledDocument document;
    private String selectedText;
    private String stringToBeInserted;

    @Override
    public void actionPerformed(ActionEvent e) {
        JTextComponent ed = org.netbeans.api.editor.EditorRegistry.lastFocusedComponent();
        if (ed == null) {
            JOptionPane.showMessageDialog(null, "Error: no open editor");
            return;
        }

        this.selectedText = ed.getSelectedText();
        if (selectedText == null) {
            selectedText = "";
        }
        stringToBeInserted = selectedText;

        Gson gson = new Gson();

        try {

            Reader reader = new InputStreamReader(GCodeFunctionsActionListener.class.getResourceAsStream("/resources/cycles.json"), "UTF-8");
            FunctionConf[] fc = gson.fromJson(reader, FunctionConf[].class);

            if (this.selectedText.trim().length() == 0) {
                DialogNewFunction nf = new DialogNewFunction(fc, org.openide.windows.WindowManager.getDefault().getMainWindow(), true);
                nf.setLocationRelativeTo(org.openide.windows.WindowManager.getDefault().getMainWindow());
                nf.setVisible(true);
                stringToBeInserted = nf.g_code;

            }
            if (stringToBeInserted.trim().length() <= 0) {
                //JOptionPane.showMessageDialog(null, "No code created.");
                return;
            }
            DialogBackTranslationFunction btf = new DialogBackTranslationFunction(stringToBeInserted, fc, org.openide.windows.WindowManager.getDefault().getMainWindow(), true);
            btf.setLocationRelativeTo(org.openide.windows.WindowManager.getDefault().getMainWindow());
            btf.setVisible(true);
            if (btf.canceled) {
                return;

            }
            stringToBeInserted = btf.g_code;
            ed.replaceSelection(stringToBeInserted);

            //disp = gr.create_display_points(contour);
        } catch (Exception e1) {
            System.out.println("Error " + e1.toString());
            JOptionPane.showMessageDialog(null, "Error: " + e1.getMessage());
            return;

        }

    }
}
