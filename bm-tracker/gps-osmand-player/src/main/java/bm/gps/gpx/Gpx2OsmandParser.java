package bm.gps.gpx;

import bm.gps.GeoTools;
import bm.gps.MessageOsmand;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.camel.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

@Component
public class Gpx2OsmandParser {

  private static final Logger logger = LoggerFactory.getLogger(Gpx2OsmandParser.class);

  public List<MessageOsmand> parse(
      File gpxFile, @Header(GpxPlayerHeaders.DEVICE_ID) String configuredDeviceId) {

    Document document = parseDocument(gpxFile);
    String deviceId = resolveDeviceId(document, gpxFile, configuredDeviceId);
    List<GeoTools.TrackPoint> trackPoints = extractTrackPoints(document, gpxFile);
    if (trackPoints.isEmpty()) {
      throw new IllegalArgumentException(
          "No GPX track points found in " + gpxFile.getAbsolutePath());
    }

    // potential neutral output mid/end-point for time & space: trackPoints
    // before getting specific, i.e. MessageOsmand
    // separate method to convert TrackPoint to MessageOsmand ?
    //    return trackPoints.stream()
    //            .map(trackPoint -> createMessageOsmand(trackPoint, deviceId))
    //            .collect(Collectors.toList());

    List<MessageOsmand> messages = new ArrayList<>(trackPoints.size());
    GeoTools.TrackPoint previous = null;
    for (GeoTools.TrackPoint point : trackPoints) {
      Double speed = null;
      Double bearing = null;
      if (previous != null) {
        speed = GeoTools.speedMetersPerSecond(previous, point);
        bearing = GeoTools.normalizedBearingDegrees(previous, point);
      }

      messages.add(
          new MessageOsmand(
              deviceId,
              point.latitude(),
              point.longitude(),
              point.timestamp(),
              speed,
              bearing,
              point.altitude(),
              null,
              null));
      previous = point;
    }
    logger.info(
        "Parsed {} track points from GPX file {} into {} Osmand messages for device '{}'",
        trackPoints.size(),
        gpxFile.getName(),
        messages.size(),
        deviceId);
    return messages;
  }

  private Document parseDocument(File gpxFile) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.setErrorHandler(new GpxParseErrorHandler(gpxFile));
      return builder.parse(gpxFile);
    } catch (SAXParseException e) {
      throw new IllegalArgumentException(buildParseErrorMessage(gpxFile, e), e);
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new IllegalArgumentException(
          "Failed to parse GPX file " + gpxFile.getAbsolutePath(), e);
    }
  }

  private String buildParseErrorMessage(File gpxFile, SAXParseException e) {
    return "Failed to parse GPX file "
        + gpxFile.getAbsolutePath()
        + " at line "
        + e.getLineNumber()
        + ", column "
        + e.getColumnNumber()
        + ": "
        + e.getMessage();
  }

  private final class GpxParseErrorHandler implements ErrorHandler {

    private final File gpxFile;

    private GpxParseErrorHandler(File gpxFile) {
      this.gpxFile = gpxFile;
    }

    @Override
    public void warning(SAXParseException exception) throws SAXException {
      logger.warn("{}", buildSaxLogMessage("XML parser warning", exception));
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
      logger.warn("{}", buildSaxLogMessage("Malformed GPX XML", exception));
      throw exception;
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
      logger.warn("{}", buildSaxLogMessage("Malformed GPX XML", exception));
      throw exception;
    }

    private String buildSaxLogMessage(String prefix, SAXParseException exception) {
      return prefix
          + " in "
          + gpxFile.getAbsolutePath()
          + " at line "
          + exception.getLineNumber()
          + ", column "
          + exception.getColumnNumber()
          + ": "
          + exception.getMessage();
    }
  }

  private List<GeoTools.TrackPoint> extractTrackPoints(Document document, File gpxFile) {
    List<GeoTools.TrackPoint> trackPoints = new ArrayList<>();
    NodeList nodes = document.getElementsByTagNameNS("*", "trkpt");
    long fallbackTimestamp =
        gpxFile.lastModified() > 0
            ? gpxFile.lastModified() / 1000L
            : Instant.now().getEpochSecond();

    for (int index = 0; index < nodes.getLength(); index++) {
      Element trackPointElement = (Element) nodes.item(index);
      double latitude = parseRequiredDouble(trackPointElement, "lat", gpxFile);
      double longitude = parseRequiredDouble(trackPointElement, "lon", gpxFile);
      Double altitude = parseOptionalDouble(childText(trackPointElement, "ele"));

      Long parsedTimestamp = parseTimestamp(childText(trackPointElement, "time"));
      long timestamp = parsedTimestamp != null ? parsedTimestamp.longValue() : fallbackTimestamp;
      fallbackTimestamp = Math.max(fallbackTimestamp + 1L, timestamp);

      trackPoints.add(new GeoTools.TrackPoint(latitude, longitude, timestamp, altitude));
    }
    return trackPoints;
  }

  private String resolveDeviceId(Document document, File gpxFile, String configuredDeviceId) {
    if (hasText(configuredDeviceId)) {
      return configuredDeviceId.trim();
    }

    NodeList tracks = document.getElementsByTagNameNS("*", "trk");
    if (tracks.getLength() > 0) {
      String trackName = childText((Element) tracks.item(0), "name");
      if (hasText(trackName)) {
        return trackName.trim();
      }
    }

    String fileName = gpxFile.getName();
    int extensionIndex = fileName.lastIndexOf('.');
    return extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
  }

  private double parseRequiredDouble(Element element, String attributeName, File gpxFile) {
    String value = element.getAttribute(attributeName);
    if (!hasText(value)) {
      throw new IllegalArgumentException(
          "Missing GPX attribute '" + attributeName + "' in " + gpxFile.getAbsolutePath());
    }
    try {
      return Double.parseDouble(value.trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Invalid GPX attribute '"
              + attributeName
              + "' value '"
              + value
              + "' in "
              + gpxFile.getAbsolutePath(),
          e);
    }
  }

  private Double parseOptionalDouble(String value) {
    if (!hasText(value)) {
      return null;
    }
    try {
      return Double.valueOf(Double.parseDouble(value.trim()));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Long parseTimestamp(String value) {
    if (!hasText(value)) {
      return null;
    }
    try {
      return Long.valueOf(Instant.parse(value.trim()).getEpochSecond());
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  private String childText(Element parent, String localName) {
    NodeList children = parent.getChildNodes();
    for (int index = 0; index < children.getLength(); index++) {
      Node child = children.item(index);
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        String childLocalName = child.getLocalName();
        String childNodeName = child.getNodeName();
        if (localName.equals(childLocalName) || localName.equals(childNodeName)) {
          String text = child.getTextContent();
          if (text != null) {
            return text.trim();
          }
        }
      }
    }
    return null;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
