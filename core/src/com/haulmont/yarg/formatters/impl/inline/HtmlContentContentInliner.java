/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Artamonov Yuryi
 * Created: 10.03.11 17:11
 *
 * $Id: HtmlContentTagHandler.java 10587 2013-02-19 08:40:16Z degtyarjov $
 */
package com.haulmont.yarg.formatters.impl.inline;

import com.haulmont.yarg.exception.ReportingException;
import com.haulmont.yarg.formatters.impl.doc.OfficeComponent;
import com.sun.star.beans.PropertyValue;
import com.sun.star.document.XDocumentInsertable;
import com.sun.star.text.XText;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextRange;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.contenttype.ContentType;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.SpreadsheetMLPackage;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.PartName;
import org.docx4j.openpackaging.parts.SpreadsheetML.WorksheetPart;
import org.docx4j.openpackaging.parts.WordprocessingML.AlternativeFormatInputPart;
import org.docx4j.relationships.Relationship;
import org.docx4j.wml.CTAltChunk;
import org.docx4j.wml.R;
import org.docx4j.wml.Text;
import org.xlsx4j.sml.Cell;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.haulmont.yarg.formatters.impl.doc.UnoConverter.asXDocumentInsertable;

/**
 * Handle HTML with format string: ${html}
 */
public class HtmlContentContentInliner implements ContentInliner {

    public final static String REGULAR_EXPRESSION = "\\$\\{html\\}";

    private static final String ENCODING_HEADER = "<META HTTP-EQUIV=\"CONTENT-TYPE\" CONTENT=\"text/html; charset=utf-8\">";

    private static final String OPEN_HTML_TAGS = "<html> <head> </head> <body>";
    private static final String CLOSE_HTML_TAGS = "</body> </html>";

    private Pattern tagPattern;

    public HtmlContentContentInliner() {
        tagPattern = Pattern.compile(REGULAR_EXPRESSION, Pattern.CASE_INSENSITIVE);
    }

    public Pattern getTagPattern() {
        return tagPattern;
    }

    public void inlineToDoc(OfficeComponent officeComponent,
                            XTextRange textRange, XText destination,
                            Object paramValue, Matcher matcher) throws Exception {
        try {
            boolean inserted = false;
            if (paramValue != null) {
                String htmlContent = paramValue.toString();
                if (!StringUtils.isEmpty(htmlContent)) {
                    insertHTML(destination, textRange, htmlContent);
                    inserted = true;
                }
            }

            if (!inserted)
                destination.getText().insertString(textRange, "", true);
        } catch (Exception e) {
            throw new ReportingException("An error occurred while inserting html to doc file", e);
        }
    }

    public void inlineToDocx(WordprocessingMLPackage wordPackage, Text text, Object paramValue, Matcher matcher) {
        try {
            AlternativeFormatInputPart afiPart = new AlternativeFormatInputPart(new PartName("/" + UUID.randomUUID().toString() + ".html"));
            afiPart.setBinaryData(paramValue.toString().getBytes());
            afiPart.setContentType(new ContentType("text/html"));
            Relationship altChunkRel = wordPackage.getMainDocumentPart().addTargetPart(afiPart);
            CTAltChunk ac = Context.getWmlObjectFactory().createCTAltChunk();
            ac.setId(altChunkRel.getId());
            R run = (R) text.getParent();
            run.getContent().add(ac);
            text.setValue("");
            wordPackage.getContentTypeManager().addDefaultContentType("html", "text/html");
        } catch (Exception e) {
            throw new ReportingException("An error occurred while inserting html to docx file", e);
        }
    }

    @Override
    public void inlineToXls(HSSFPatriarch patriarch, HSSFCell resultCell, Object paramValue, Matcher matcher) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void inlineToXlsx(SpreadsheetMLPackage pkg, WorksheetPart worksheetPart, Cell newCell, Object paramValue, Matcher matcher) {
        throw new UnsupportedOperationException();
    }

    private void insertHTML(XText destination, XTextRange textRange, String htmlContent)
            throws Exception {
        File tempFile = null;
        try {
            tempFile = File.createTempFile(UUID.randomUUID().toString(), ".htm");

            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append(ENCODING_HEADER);
            contentBuilder.append(OPEN_HTML_TAGS);
            contentBuilder.append(htmlContent);
            contentBuilder.append(CLOSE_HTML_TAGS);

            FileUtils.writeByteArrayToFile(tempFile, contentBuilder.toString().getBytes());
            String fileUrl = "file:///" + tempFile.getCanonicalPath().replace("\\", "/");

            XTextCursor textCursor = destination.createTextCursorByRange(textRange);
            XDocumentInsertable insertable = asXDocumentInsertable(textCursor);

            insertable.insertDocumentFromURL(fileUrl, new PropertyValue[0]);
        } finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }
}