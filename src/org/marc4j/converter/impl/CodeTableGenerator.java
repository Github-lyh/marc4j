/**
 * Copyright (C) 2002 Bas Peters
 *
 * This file is part of MARC4J
 *
 * MARC4J is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * MARC4J is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with MARC4J; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.marc4j.converter.impl;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

/**
 * Invoked at build time to generate a java source file (named CodeTableGenerated.java) which when compiled will
 * implement the CodeTableInterface (primarily through switch statements) and which can be used be the AnselToUnicode
 * converter instead of this class, and which will produce the same results as the object CodeTable. <br>
 * The following routines are only used in the code generation process, and are not available to be called from within
 * an application that uses Marc4j. <br>
 * The routines generated for converting MARC8 multibyte characters to unicode are split into several routines to
 * workaround a limitation in java that a method can only contain 64k of code when it is compiled.
 *
 * @author Robert Haschart
 *  
 */
public class CodeTableGenerator extends CodeTable {

    /**
     * Creates a CodeTableGenerator from the supplied {@link InputStream}.
     *
     * @param byteStream
     */
    public CodeTableGenerator(final InputStream byteStream) {
        super(byteStream);
    }

    /**
     * The main function called when generating a codetable.
     *
     * @param args
     * @throws FileNotFoundException
     */
    public static void main(final String args[]) {
        final InputStream in = CodeTable.class.getResourceAsStream("resources/codetables.xml");
        final CodeTableGenerator ct = new CodeTableGenerator(in);

        ct.dumpTableAsSwitchStatement(System.out);
    }

    private void dumpTableAsSwitchStatement(final PrintStream out) {
        out.println("package org.marc4j.converter.impl;");
        out.println("");
        out.println("/** ");
        out.println(" *  An implementation of CodeTableInterface that is used in converting MARC8");
        out.println(" *  data to UTF8 data, that doesn't rely on any data files or resources or data structures");
        out.println(" *  ");
        out.println(" *  Warning: This file is generated by running the main routine in the file CodeTableGenerator.java ");
        out.println(" *  Warning: Do not edit this file, or all edits will be lost at the next build. ");
        out.println(" */");
        out.println("public class CodeTableGenerated implements CodeTableInterface {");
        out.println("\tpublic boolean isCombining(int i, int g0, int g1) {");
        out.println("\t\tswitch (i <= 0x7E ? g0 : g1) {");
        final Object combiningKeys[] = combining.keySet().toArray();
        Arrays.sort(combiningKeys);
        for (int combiningSel = 0; combiningSel < combiningKeys.length; combiningSel++) {
            final Integer nextKey = (Integer) combiningKeys[combiningSel];
            out.println("\t\t\tcase 0x" + Integer.toHexString(nextKey) + ":");
            final Vector<Integer> v = combining.get(nextKey);
            if (v.size() > 0) {
                out.println("\t\t\t\tswitch(i) {");
                for (final Integer vVal : v) {
                    out.println("\t\t\t\t\tcase 0x" + Integer.toHexString(vVal) + ":");
                }
                out.println("\t\t\t\t\t\treturn(true);");
                out.println("\t\t\t\t\tdefault:");
                out.println("\t\t\t\t\t\treturn(false);");
                out.println("\t\t\t\t}");
            } else {
                out.println("\t\t\t\treturn(false);");
            }
        }
        out.println("\t\t\tdefault:");
        out.println("\t\t\t\treturn(false);");
        out.println("\t\t\t}");
        out.println("\t}");
        out.println("");
        out.println("\tpublic char getChar(int c, int mode) {");
        out.println("\t\tint code = getCharCode(c, mode);");
        out.println("\t\tif (code == -1) return((char)0);");
        out.println("\t\tif (code != 0) return((char)code);");
        out.println("\t\tcode = getCharCode(c < 0x80 ? c + 0x80 : c - 0x80 , mode);");
        out.println("\t\treturn((char)code);");
        out.println("\t}");
        out.println("");
        out.println("\tprivate int getCharCode(int c, int mode) {");
        out.println("\t\tif (c == 0x20) return  c;");
        out.println("\t\tswitch (mode) {");
        final Integer charsetsKeys[] = charsets.keySet().toArray(new Integer[0]);
        Arrays.sort(charsetsKeys);
        for (int charsetSel = 0; charsetSel < charsetsKeys.length; charsetSel++) {
            final Integer nextKey = charsetsKeys[charsetSel];
            out.println("\t\t\tcase 0x" + Integer.toHexString(nextKey) + ":");
            if (nextKey.intValue() == 0x31) {
                out.println("\t\t\t\treturn(getMultiByteChar(c));");
            } else {
                final HashMap<Integer, Character> map = charsets.get(nextKey);
                final Integer keyArray[] = map.keySet().toArray(new Integer[0]);
                Arrays.sort(keyArray);
                out.println("\t\t\t\tswitch(c) {");
                for (int sel = 0; sel < keyArray.length; sel++) {
                    final Integer mKey = keyArray[sel];
                    final Character c = map.get(mKey);
                    if (c != null) {
                        out.println("\t\t\t\t\tcase 0x" + Integer.toHexString(mKey) + ":  return(0x" + Integer
                                .toHexString(c.charValue()) + "); ");
                    } else {
                        out.println("\t\t\t\t\tcase 0x" + Integer.toHexString(mKey) + ":  return(0); ");
                    }
                }
                out.println("\t\t\t\t\tdefault:  return(0);");
                out.println("\t\t\t\t}");
            }
        }
        out.println("\t\t\tdefault: return(-1);  // unknown charset specified ");
        out.println("\t\t}");
        out.println("\t}");
        out.println("");
        final StringBuffer getMultiByteFunc = new StringBuffer();
        getMultiByteFunc.append("\tprivate int getMultiByteChar(int c) {\n");

        final HashMap<Integer, Character> map = charsets.get(new Integer(0x31));
        final Integer keyArray[] = map.keySet().toArray(new Integer[0]);
        Arrays.sort(keyArray);

        // Note the switch statements generated for converting multibyte
        // characters must be
        // divided up like this so that the 64K code size per method limitation
        // is not exceeded.

        dumpPartialMultiByteTable(out, getMultiByteFunc, keyArray, map, 0x210000, 0x214fff);
        dumpPartialMultiByteTable(out, getMultiByteFunc, keyArray, map, 0x215000, 0x21ffff);
        dumpPartialMultiByteTable(out, getMultiByteFunc, keyArray, map, 0x220000, 0x22ffff);
        dumpPartialMultiByteTable(out, getMultiByteFunc, keyArray, map, 0x230000, 0x27ffff);
        dumpPartialMultiByteTable(out, getMultiByteFunc, keyArray, map, 0x280000, 0x7f7fff);

        getMultiByteFunc.append("\t\treturn(0);\n");
        getMultiByteFunc.append("\t}");
        out.println(getMultiByteFunc.toString());

        out.println("}");
    }

    private void dumpPartialMultiByteTable(final PrintStream out, final StringBuffer buffer,
            final Integer keyArray[], final HashMap<Integer, Character> map, final int startByte, final int endByte) {
        final String startByteStr = "0x" + Integer.toHexString(startByte);
        final String endByteStr = "0x" + Integer.toHexString(endByte);
        buffer.append("\t\tif (c >= " + startByteStr + " && c <= " + endByteStr + ")  return (getMultiByteChar_" + startByteStr + "_" + endByteStr + "(c));\n");

        out.println("\tprivate char getMultiByteChar_" + startByteStr + "_" + endByteStr + "(int c) {");
        out.println("\t\tswitch(c) {");
        for (int sel = 0; sel < keyArray.length; sel++) {
            final Integer mKey = keyArray[sel];
            final Character c = map.get(mKey);
            if (mKey >= startByte && mKey <= endByte) {
                if (c != null) {
                    out.println("\t\t\tcase 0x" + Integer.toHexString(mKey) + ":  return((char)0x" + Integer
                            .toHexString(c.charValue()) + "); ");
                } else {
                    out.println("\t\t\tcase 0x" + Integer.toHexString(mKey) + ":  return((char)0); ");
                }
            }
        }
        out.println("\t\t\tdefault: return((char)0);");
        out.println("\t\t}");
        out.println("\t}");
        out.println("");
    }

}
