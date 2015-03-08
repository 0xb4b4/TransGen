package com.transgen.api;

import com.transgen.api.enums.State;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.pdf417.PDF417Writer;
import com.google.zxing.pdf417.encoder.Dimensions;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class StateGenerator {
    private final String STATE_CODE;
    private final String FILE_TYPE;
    private final String IIN;
    private final int VERSION;
    private final int JURISDICTION;
    public LinkedHashMap<String, String> data = new LinkedHashMap<String, String>();
    private String CI = Character.toString((char) 64);
    private String LF = Character.toString((char) 10);
    private String RS = Character.toString((char) 30);
    private String CR = Character.toString((char) 13);
    private LinkedHashMap<String, LinkedHashMap<String, Integer>> fields;

    public StateGenerator(String[] data, LinkedHashMap<String, LinkedHashMap<String, Integer>> fields, String code, String type, String iin, Integer ver, Integer juris) {
        for (String s : data) {
            String[] fv = s.split("::");
            this.data.put(fv[0], fv.length > 1 ? fv[1] : "");
        }
        this.fields = fields;
        this.STATE_CODE = code;
        this.FILE_TYPE = type;
        this.IIN = iin;
        this.VERSION = ver;
        this.JURISDICTION = juris;
    }

    public StateGenerator(String[] data, LinkedHashMap<String, LinkedHashMap<String, Integer>> fields, String code, String type, String iin, Integer ver, Integer juris, String rs) {
        this(data, fields, code, type, iin, ver, juris);
        this.RS = rs;
    }

    public StateGenerator(String[] data, LinkedHashMap<String, LinkedHashMap<String, Integer>> fields, String code, String type, String iin, Integer ver, Integer juris, String rs, String cr) {
        this(data, fields, code, type, iin, ver, juris, rs);
        this.CR = cr;
    }

    public StateGenerator(String[] data, LinkedHashMap<String, LinkedHashMap<String, Integer>> fields, State state, String type, Integer ver, Integer juris) {
        this(data, fields, state.getAbbreviation(), type, state.getIIN(), ver, juris);
    }

    public StateGenerator(String[] data, LinkedHashMap<String, LinkedHashMap<String, Integer>> fields, State state, String type, Integer ver, Integer juris, String rs) {
        this(data, fields, state.getAbbreviation(), type, state.getIIN(), ver, juris);
        this.RS = rs;
    }

    public StateGenerator(String[] data, LinkedHashMap<String, LinkedHashMap<String, Integer>> fields, State state, String type, Integer ver, Integer juris, String rs, String cr) {
        this(data, fields, state.getAbbreviation(), type, state.getIIN(), ver, juris, rs);
        this.CR = cr;
    }

    protected static LinkedHashMap<String, LinkedHashMap<String, Integer>> getFields() {
        return null;
    }

    public static String padRight(String s, int n) {
        if (n == 0) return s;
        return String.format("%1$-" + n + "s", s);
    }

    public static String zeroLeft(Integer i, int n) {
        if (n == 0) return "" + i;
        return String.format("%0" + n + "d", i);
    }

    public static String zeroLeft(String s, int n) {
        return zeroLeft(Integer.parseInt(s), n);
    }

    public static StateGenerator instantiateStateScript(Class clazz, String[] fields) {
        Object aScript = null;
        try {
            aScript = clazz.getConstructor(new Class[]{String[].class}).newInstance(new Object[]{fields});
        } catch (Exception e) {
            System.out.println("[Error] Couldn't instantiate state script, does it extend StateGenerator?");
            e.printStackTrace();
        }
        return (StateGenerator) aScript;
    }

    public String getStateCode() {
        return STATE_CODE;
    }

    public String getCI() {
        return CI;
    }

    public String getLF() {
        return LF;
    }

    public String getRS() {
        return RS;
    }

    public String getCR() {
        return CR;
    }

    public String getFileType() {
        return FILE_TYPE;
    }

    public String getIIN() {
        return IIN;
    }

    public String getVersion() {
        return zeroLeft(VERSION, 2);
    }

    public String getJurisdiction() {
        return zeroLeft(JURISDICTION, 2);
    }

    public String getEntries() {
        return zeroLeft(fields.keySet().size(), 2);
    }

    public Integer getFieldPadding(String doc, String field) {
        return fields.get(doc).get(field);
    }

    public ArrayList<String> getDocuments() {
        ArrayList<String> l = new ArrayList<String>();
        l.addAll(fields.keySet());
        return l;
    }

    public ArrayList<String> getFields(String doc) {
        ArrayList<String> l = new ArrayList<String>();
        l.addAll(fields.get(doc).keySet());
        return l;
    }

    public Integer getFieldLength(String doc, String field) {
        return fields.get(doc).get(field);
    }

    public String getSpecialField(String doc, String field) {
        return null;
    }

    public abstract void generate2D(int width, int height);

    public abstract void generate1D(int width, int height);

    public void generate(int width2d, int height2d, int width1d, int height1d) {
        this.generate2D(width2d, height2d);
        this.generate1D(width1d, height1d);
    }

    public String getUniqueFilename() {
        return this.data.get("DCT").replaceAll(",", "_") + "_" + this.data.get("DCS").replaceAll(",", "_") + "_" + this.data.get("DAQ");
    }

    public int dataLength(String s) {
        return s.length();
    }

    public abstract String getHeader();

    public abstract String getCommonTerminal();

    public abstract String getEndTerminal();

    public abstract Boolean finalTerminalOnly();

    public abstract Boolean includeDocumentHeaders();

    public abstract Boolean removeFinalTerminal();

    public String additionalFinalTerminal() {
        return "";
    }

    public int headerSizeOffset() {
        return 0;
    }

    public int bodySizeOffset() {
        return 0;
    }

    public String generate1DData() {
        return null;
    }

    public String generate2DData() {
        String body = "";
        Integer len = 0;

        String header = getHeader();
        Integer header_len = dataLength(header) + (10 * this.getDocuments().size());

        int i = 0;
        for (String doc : this.getDocuments()) {
            i++;
            Integer doc_len = includeDocumentHeaders() ? doc.length() : 0;
            if (includeDocumentHeaders()) body += doc;

            for (String field : this.getFields(doc)) {
                String val = getSpecialField(doc, field);
                if (val == null) val = this.data.get(field);

                String term = getCommonTerminal();
                if (field.equals(this.getFields(doc).get(this.getFields(doc).size() - 1))) {
                    if (finalTerminalOnly()) {
                        if (i == this.getDocuments().size()) {
                            if (!removeFinalTerminal()) term = getEndTerminal();
                        }
                    } else {
                        if (i == this.getDocuments().size()) {
                            if (!removeFinalTerminal()) term = getEndTerminal();
                            else term = "";
                        } else {
                            term = getEndTerminal();
                        }
                    }

                    if (doc.equals(getDocuments().get(getDocuments().size() - 1))) {
                        term += additionalFinalTerminal();
                    }
                }

                String f = field.toUpperCase() + padRight(val.toUpperCase(), getFieldLength(doc, field)) + term;
                body += f;
                doc_len += dataLength(f);
            }

            Integer offset = header_len + len + headerSizeOffset();
            header += (doc + zeroLeft(offset.toString(), 4) + zeroLeft(Integer.toString(doc_len + bodySizeOffset()), 4));
            len += doc_len;
        }

        return header + body;
    }

    public void generateCode128(String data, int width, int height) {
        try {
            Code128Writer c = new Code128Writer();
            BitMatrix matrix = c.encode(data, BarcodeFormat.CODE_128, width, height);
            if (Files.notExists(Paths.get(this.getStateCode()))) {
                Files.createDirectory(Paths.get(this.getStateCode()));
            }
            FileOutputStream fos = new FileOutputStream(new File(this.getStateCode() + File.separator + "CODE128_" + this.getUniqueFilename() + ".png"));
            MatrixToImageWriter.writeToStream(matrix, "png", fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void generatePDF417(String data, int row, int col, int ecl, int width, int height) {
        try {
            PDF417Writer pdf = new PDF417Writer();

            Map hints = new HashMap();
            hints.put(EncodeHintType.MARGIN, 0);
            hints.put(EncodeHintType.PDF417_DIMENSIONS, new Dimensions(col, col, row, row));
            hints.put(EncodeHintType.CHARACTER_SET, "ISO8859_2");
            hints.put(EncodeHintType.ERROR_CORRECTION, ecl);

            BitMatrix matrix = pdf.encode(data, BarcodeFormat.PDF_417, width, height, hints);
            if (Files.notExists(Paths.get(this.getStateCode()))) {
                Files.createDirectory(Paths.get(this.getStateCode()));
            }
            FileOutputStream fos = new FileOutputStream(new File(this.getStateCode() + File.separator + "PDF417_" + this.getUniqueFilename() + ".png"));
            MatrixToImageWriter.writeToStream(matrix, "png", fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
