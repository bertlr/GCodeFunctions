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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import org.roiderh.gcodeviewer.lexer.Gcodereader;
import org.roiderh.gcodeviewer.lexer.GcodereaderConstants;
import org.roiderh.gcodeviewer.lexer.Token;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.regex.Matcher;

/**
 *
 * @author Herbert Roider <herbert.roider@utanet.at>
 */
public class gcodereader {

    public int linenumber_offset = 0;

    // R Parameters:
    public Map<Integer, String> R = new HashMap<>();
    // Holds the last Subprogram call:
    public String L ="";
    public String cycle = "";
    public ArrayList<String> arguments;

    // spinner = 0, emco = 1
    public int machine = 0;

    public boolean read(InputStream is) throws Exception {
        boolean ret = false;

        int linenumber = 0;

        //try {
        //os = new FileOutputStream(new File("/home/herbert/NetBeansProjects/gcodeviewer/src/gcodeviewer/punkte.txt"));
        Token t;
        BufferedReader br;
        String line;
        InputStream istream;
        br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        //InputStream line = new ByteArrayInputStream(this.selectedText.getBytes());
        while ((line = br.readLine()) != null) {
            linenumber++;
            // remove comments, from semicolon to the line break.
            int semicolon_pos = line.indexOf(";");
            if (semicolon_pos >= 0) {
                line = line.substring(0, semicolon_pos);
            }
            // if the first character of a line is a brace, 
            // the complete line is interpreted as a comment. For old sinumerik 810
            int first_brace_pos = line.trim().indexOf("(");
            if (first_brace_pos == 0) {
                line = "";
            }
            // The parser needs an line break:
            line += '\n';
            System.out.println("line=" + line);

            istream = new ByteArrayInputStream(line.getBytes());
            Gcodereader gr = new Gcodereader(istream);

            /*
             read one line
             */
            do {
                t = gr.getNextToken();
                if (t.kind == GcodereaderConstants.EOF) {
                    break;
                }
                System.out.println("Token: " + t.kind + ", " + t.image);
                Matcher m;
                parameter para = null;

                if (t.kind == GcodereaderConstants.CYCLE) {
                    //System.out.println("Cyclus: " + para.strval);
                    machine = 0; // spinner
                    int openBracketLoc = t.image.indexOf("(");
                    int closeBracketLoc = t.image.indexOf(")");

                    cycle = t.image.substring(0, openBracketLoc).trim();
                    String Params = t.image.substring(openBracketLoc + 1, closeBracketLoc).trim();
                    System.out.println("Func=" + cycle);
                    System.out.println(Params);
                    arguments = new ArrayList<>(Arrays.asList(Params.split(",", -1)));
                    /*
                     *  remove quote
                     */
                    for (int i = 0; i < arguments.size(); i++) {
                        arguments.set(i, arguments.get(i).trim());
                        if (arguments.get(i).contains("\"")) {
                            arguments.set(i, arguments.get(i).substring(1, arguments.get(i).length() - 1));
                        }
                    }
                    ret = true;
                    continue;

                }

                // All parameters except G-functions
                if (t.kind == GcodereaderConstants.PARAM || t.kind == GcodereaderConstants.SHORT_PARAM) {

                    para = new parameter();
                    para.parse(t.image);

                }
                if (para == null) {
                    continue;
                }

                if (para.name.compareTo("R") == 0) {
                    R.put(para.index, para.strval.trim());

                }

                if (para.name.compareTo("L") == 0) {
                    machine = 1; // emco
                    L = para.strval.trim();
                    cycle = para.strval.trim();
                    ret = true;
                }

            } while (!(t.kind == GcodereaderConstants.EOF));

        }

        return ret;
    }

}
