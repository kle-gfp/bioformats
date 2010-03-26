//
// XMLTools.java
//

/*
LOCI Common package: utilities for I/O, reflection and miscellaneous tasks.
Copyright (C) 2005-@year@ Melissa Linkert, Curtis Rueden and Chris Allan.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package loci.common.xml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import loci.common.RandomAccessInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A utility class for working with XML.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/components/common/src/loci/common/xml/XMLTools.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/components/common/src/loci/common/xml/XMLTools.java">SVN</a></dd></dl>
 *
 * @author Curtis Rueden ctrueden at wisc.edu
 * @author Chris Allan callan at blackcat.ca
 * @author Melissa Linkert linkert at wisc.edu
 */
public final class XMLTools {

  // -- Constants --

  static final Logger LOGGER = LoggerFactory.getLogger(XMLTools.class);

  // -- Constructor --

  private XMLTools() { }

  // -- XML to/from DOM --

  /** Parses a DOM from the given XML file on disk. */
  public static Document parseDOM(File file)
    throws ParserConfigurationException, SAXException, IOException
  {
    InputStream is = new FileInputStream(file);
    try {
      Document doc = parseDOM(is);
      return doc;
    } finally {
      is.close();
    }
  }

  /** Parses a DOM from the given XML string. */
  public static Document parseDOM(String xml)
    throws ParserConfigurationException, SAXException, IOException
  {
    byte[] bytes = xml.getBytes();
    InputStream is = new ByteArrayInputStream(bytes);
    try {
      Document doc = parseDOM(is);
      return doc;
    } finally {
      is.close();
    }
  }

  /** Parses a DOM from the given XML input stream. */
  public static Document parseDOM(InputStream is)
    throws ParserConfigurationException, SAXException, IOException
  {
    // Java XML factories are not declared to be thread safe
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); 
    DocumentBuilder db = factory.newDocumentBuilder();
    return db.parse(is);
  }

  /** Converts the given DOM back to a string. */
  public static String getXML(Document doc)
    throws TransformerConfigurationException, TransformerException
  {
    Source source = new DOMSource(doc);
    StringWriter stringWriter = new StringWriter();
    Result result = new StreamResult(stringWriter);
    // Java XML factories are not declared to be thread safe
    TransformerFactory factory = TransformerFactory.newInstance();
    Transformer transformer = factory.newTransformer();
    transformer.transform(source, result);
    return stringWriter.getBuffer().toString();
  }

  // -- Filtering --

  /** Remove invalid characters from an XML string. */
  public static String sanitizeXML(String s) {
    for (int i=0; i<s.length(); i++) {
      char c = s.charAt(i);
      if (Character.isISOControl(c) || !Character.isDefined(c) || c > '~') {
        s = s.replace(c, ' ');
      }
    }
    return s;
  }

  /** Indents XML to be more readable. */
  public static String indentXML(String xml) {
    return indentXML(xml, 3, false);
  }

  /** Indents XML by the given spacing to be more readable. */
  public static String indentXML(String xml, int spacing) {
    return indentXML(xml, spacing, false);
  }

  /**
   * Indents XML to be more readable, avoiding any whitespace
   * injection into CDATA if the preserveCData flag is set.
   */
  public static String indentXML(String xml, boolean preserveCData) {
    return indentXML(xml, 3, preserveCData);
  }

  /**
   * Indents XML by the given spacing to be more readable, avoiding any
   * whitespace injection into CDATA if the preserveCData flag is set.
   */
  public static String indentXML(String xml, int spacing,
    boolean preserveCData)
  {
    if (xml == null) return null; // garbage in, garbage out
    StringBuffer sb = new StringBuffer();
    StringTokenizer st = new StringTokenizer(xml, "<>", true);
    int indent = 0, noSpace = 0;
    boolean first = true, element = false;
    while (st.hasMoreTokens()) {
      String token = st.nextToken().trim();
      if (token.equals("")) continue;
      if (token.equals("<")) {
        element = true;
        continue;
      }
      if (element && token.equals(">")) {
        element = false;
        continue;
      }

      if (!element && preserveCData) noSpace = 2;

      if (noSpace == 0) {
        // advance to next line
        if (first) first = false;
        else sb.append("\n");
      }

      // adjust indent backwards
      if (element && token.startsWith("/")) indent -= spacing;

      if (noSpace == 0) {
        // apply indent
        for (int j=0; j<indent; j++) sb.append(" ");
      }

      // output element contents
      if (element) sb.append("<");
      sb.append(token);
      if (element) sb.append(">");

      if (noSpace == 0) {
        // adjust indent forwards
        if (element &&
          !token.startsWith("?") && // ?xml tag, probably
          !token.startsWith("/") && // end element
          !token.endsWith("/") && // standalone element
          !token.startsWith("!")) // comment
        {
          indent += spacing;
        }
      }

      if (noSpace > 0) noSpace--;
    }
    sb.append("\n");
    return sb.toString();
  }

  // -- Parsing --

  /** Parses the given XML string into a list of key/value pairs. */
  public static Hashtable<String, String> parseXML(String xml)
    throws IOException
  {
    MetadataHandler handler = new MetadataHandler();
    parseXML(xml, handler);
    return handler.getMetadata();
  }

  /**
   * Parses the given XML string into a list of key/value pairs
   * using the specified XML handler.
   */
  public static void parseXML(String xml, DefaultHandler handler)
    throws IOException
  {
    parseXML(xml.getBytes(), handler);
  }

  /**
   * Parses the XML string from the given input stream into
   * a list of key/value pairs using the specified XML handler.
   */
  public static void parseXML(RandomAccessInputStream stream,
    DefaultHandler handler) throws IOException
  {
    byte[] b = new byte[(int) (stream.length() - stream.getFilePointer())];
    stream.readFully(b);
    parseXML(b, handler);
    b = null;
  }

  /**
   * Parses the XML string from the given byte array into
   * a list of key/value pairs using the specified XML handler.
   */
  public static void parseXML(byte[] xml, DefaultHandler handler)
    throws IOException
  {
    try {
      // Java XML factories are not declared to be thread safe
      SAXParserFactory factory = SAXParserFactory.newInstance(); 
      SAXParser parser = factory.newSAXParser();
      parser.parse(new ByteArrayInputStream(xml), handler);
    }
    catch (ParserConfigurationException exc) {
      IOException e = new IOException();
      e.initCause(exc);
      throw e;
    }
    catch (SAXException exc) {
      IOException e = new IOException();
      e.initCause(exc);
      throw e;
    }
  }

  // -- XSLT --

  /** Gets an XSLT template from the given resource location. */
  public static Templates getStylesheet(String resourcePath,
    Class<?> sourceClass)
  {
    InputStream xsltStream;
    if (sourceClass == null) {
      try {
        xsltStream = new FileInputStream(resourcePath);
      }
      catch (IOException exc) {
        LOGGER.debug("Could not open file", exc);
        return null;
      }
    }
    else {
      xsltStream = sourceClass.getResourceAsStream(resourcePath);
    }

    try {
      StreamSource xsltSource = new StreamSource(xsltStream);
      // Java XML factories are not declared to be thread safe
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      return transformerFactory.newTemplates(xsltSource);
    }
    catch (TransformerConfigurationException exc) {
      LOGGER.debug("Could not construct template", exc);
    }
    finally {
      try {
        if (xsltStream != null) xsltStream.close();
      }
      catch (IOException e) {
        LOGGER.debug("Could not close file", e);
      }
    }
    return null;
  }

  /** Transforms the given XML string using the specified XSLT stylesheet. */
  public static String transformXML(String xml, Templates xslt)
    throws IOException
  {
    return transformXML(new StreamSource(new StringReader(xml)), xslt);
  }

  /** Transforms the given XML data using the specified XSLT stylesheet. */
  public static String transformXML(Source xmlSource, Templates xslt)
    throws IOException
  {
    Transformer trans;
    try {
      trans = xslt.newTransformer();
    }
    catch (TransformerConfigurationException exc) {
      IOException e = new IOException();
      e.initCause(exc);
      throw e;
    }
    StringWriter xmlWriter = new StringWriter();
    StreamResult xmlResult = new StreamResult(xmlWriter);
    try {
      trans.transform(xmlSource, xmlResult);
    }
    catch (TransformerException exc) {
      IOException e = new IOException();
      e.initCause(exc);
      throw e;
    }
    return xmlWriter.toString();
  }

  // -- Validation --

  /**
   * Attempts to validate the given XML string using
   * Java's XML validation facility. Requires Java 1.5+.
   * @param xml The XML string to validate.
   * @return whether or not validation was successful.
   */
  public static boolean validateXML(String xml) {
    return validateXML(xml, null);
  }

  /**
   * Attempts to validate the given XML string using
   * Java's XML validation facility. Requires Java 1.5+.
   * @param xml The XML string to validate.
   * @param label String describing the type of XML being validated.
   * @return whether or not validation was successful.
   */
  public static boolean validateXML(String xml, String label) {
    if (label == null) label = "XML";
    Exception exception = null;

    // get path to schema from root element using SAX
    LOGGER.info("Parsing schema path");
    ValidationSAXHandler saxHandler = new ValidationSAXHandler();
    try {
      // Java XML factories are not declared to be thread safe
      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser saxParser = factory.newSAXParser();
      InputStream is = new ByteArrayInputStream(xml.getBytes());
      saxParser.parse(is, saxHandler);
    }
    catch (ParserConfigurationException exc) { exception = exc; }
    catch (SAXException exc) { exception = exc; }
    catch (IOException exc) { exception = exc; }
    if (exception != null) {
      LOGGER.warn("Error parsing schema path from {}", label, exception);
      return false;
    }
    String schemaPath = saxHandler.getSchemaPath();
    if (schemaPath == null) {
      LOGGER.error("No schema path found. Validation cannot continue.");
      return false;
    }
    else LOGGER.info(schemaPath);

    LOGGER.info("Validating {}", label);

    // look up a factory for the W3C XML Schema language
    String xmlSchemaPath = "http://www.w3.org/2001/XMLSchema";
    // Java XML factories are not declared to be thread safe
    SchemaFactory factory = SchemaFactory.newInstance(xmlSchemaPath);

    // compile the schema
    URL schemaLocation = null;
    try {
      schemaLocation = new URL(schemaPath);
    }
    catch (MalformedURLException exc) {
      LOGGER.info("Error accessing schema at {}", schemaPath, exc);
      return false;
    }
    Schema schema = null;
    try {
      schema = factory.newSchema(schemaLocation);
    }
    catch (SAXException exc) {
      LOGGER.info("Error parsing schema at {}", schemaPath, exc);
      return false;
    }

    // get a validator from the schema
    Validator validator = schema.newValidator();

    // prepare the XML source
    StringReader reader = new StringReader(xml);
    InputSource is = new InputSource(reader);
    SAXSource source = new SAXSource(is);

    // validate the XML
    ValidationErrorHandler errorHandler = new ValidationErrorHandler();
    validator.setErrorHandler(errorHandler);
    try {
      validator.validate(source);
    }
    catch (IOException exc) { exception = exc; }
    catch (SAXException exc) { exception = exc; }
    if (exception != null) {
      LOGGER.info("Error validating document", exception);
      return false;
    }
    if (errorHandler.ok()) LOGGER.info("No validation errors found.");
    return errorHandler.ok();
  }

}