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

import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;

/**
 *
 * @author Herbert Roider <herbert.roider@utanet.at>
 */
public class gcodereader {

    public int linenumber_offset = 0;
    /**
     * Holds the Error messages since last function read call.
     *
     */
    public Collection<String> messages = null;

    // R Parameters:
    public Map<Integer, Double> R = new HashMap<>();
    // Holds the last Subprogram call:
    public int L = Integer.MAX_VALUE;
    public String cycle = "";
    public String[] arguments;

    // spinner = 0, emco = 1
    public int machine = 0;

    public boolean read(InputStream is) throws Exception {
        //FileInputStream is;
        //FileOutputStream os;
        this.messages = new HashSet<>();
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
                    arguments = Params.split(",");
                    /*
                     *  remove quote or calculate the expresions
                     */
                    for (int i = 0; i < arguments.length; i++) {
                        arguments[i] = arguments[i].trim();
                        if (arguments[i].contains("\"")) {
                            arguments[i] = arguments[i].substring(1, arguments[i].length() - 1);
                        } else if(arguments[i].length() > 0){
                            org.nfunk.jep.JEP myParser = new org.nfunk.jep.JEP();
                            myParser.addStandardFunctions();
                            myParser.addStandardConstants();

                            for (Map.Entry<Integer, Double> entry : R.entrySet()) {
                                myParser.addVariable("R" + entry.getKey().toString(), entry.getValue());
                            }

                            myParser.parseExpression(arguments[i]);

                            double val = myParser.getValue();
                            arguments[i] = String.valueOf(val);

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
                    org.nfunk.jep.JEP myParser = new org.nfunk.jep.JEP();
                    myParser.addStandardFunctions();
                    myParser.addStandardConstants();

                    for (Map.Entry<Integer, Double> entry : R.entrySet()) {
                        myParser.addVariable("R" + entry.getKey().toString(), entry.getValue());
                    }

                    myParser.parseExpression(para.strval);

                    double val = myParser.getValue();
                    R.put(para.index, val);

                }

                if (para.name.compareTo("L") == 0) {
                    machine = 1; // emco
                    org.nfunk.jep.JEP myParser = new org.nfunk.jep.JEP();
                    myParser.addStandardFunctions();
                    myParser.addStandardConstants();

                    for (Map.Entry<Integer, Double> entry : R.entrySet()) {
                        myParser.addVariable("R" + entry.getKey().toString(), entry.getValue());
                    }
                    myParser.parseExpression(para.strval);
                    double val = myParser.getValue();
                    L = (int) val;
                    cycle = String.valueOf(L);
                    ret = true;

                }

            } while (!(t.kind == GcodereaderConstants.EOF));

        }

        return ret;
    }

}
