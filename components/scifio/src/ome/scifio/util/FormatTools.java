package ome.scifio.util;

import ome.scifio.FormatException;
import ome.scifio.Reader;
import ome.scifio.io.RandomAccessInputStream;

public class FormatTools {

  // -- Constants - Thumbnail dimensions --

  /** Default height and width for thumbnails. */
  public static final int THUMBNAIL_DIMENSION = 128;

  // -- Constants - pixel types --

  /** Identifies the <i>INT8</i> data type used to store pixel values. */
  public static final int INT8 = 0;

  /** Identifies the <i>UINT8</i> data type used to store pixel values. */
  public static final int UINT8 = 1;

  /** Identifies the <i>INT16</i> data type used to store pixel values. */
  public static final int INT16 = 2;

  /** Identifies the <i>UINT16</i> data type used to store pixel values. */
  public static final int UINT16 = 3;

  /** Identifies the <i>INT32</i> data type used to store pixel values. */
  public static final int INT32 = 4;

  /** Identifies the <i>UINT32</i> data type used to store pixel values. */
  public static final int UINT32 = 5;

  /** Identifies the <i>FLOAT</i> data type used to store pixel values. */
  public static final int FLOAT = 6;

  /** Identifies the <i>DOUBLE</i> data type used to store pixel values. */
  public static final int DOUBLE = 7;

  /** Human readable pixel type. */
  private static final String[] pixelTypes = makePixelTypes();

  static String[] makePixelTypes() {
    String[] pixelTypes = new String[8];
    pixelTypes[INT8] = "int8";
    pixelTypes[UINT8] = "uint8";
    pixelTypes[INT16] = "int16";
    pixelTypes[UINT16] = "uint16";
    pixelTypes[INT32] = "int32";
    pixelTypes[UINT32] = "uint32";
    pixelTypes[FLOAT] = "float";
    pixelTypes[DOUBLE] = "double";
    return pixelTypes;
  }

  // -- Constants - dimensional labels --

  /**
   * Identifies the <i>Channel</i> dimensional type,
   * representing a generic channel dimension.
   */
  public static final String CHANNEL = "Channel";

  /**
   * Identifies the <i>Spectra</i> dimensional type,
   * representing a dimension consisting of spectral channels.
   */
  public static final String SPECTRA = "Spectra";

  /**
   * Identifies the <i>Lifetime</i> dimensional type,
   * representing a dimension consisting of a lifetime histogram.
   */
  public static final String LIFETIME = "Lifetime";

  /**
   * Identifies the <i>Polarization</i> dimensional type,
   * representing a dimension consisting of polarization states.
   */
  public static final String POLARIZATION = "Polarization";

  /**
   * Identifies the <i>Phase</i> dimensional type,
   * representing a dimension consisting of phases.
   */
  public static final String PHASE = "Phase";

  /**
   * Identifies the <i>Frequency</i> dimensional type,
   * representing a dimension consisting of frequencies.
   */
  public static final String FREQUENCY = "Frequency";

  // -- Constants - miscellaneous --

  /** File grouping options. */
  public static final int MUST_GROUP = 0;
  public static final int CAN_GROUP = 1;
  public static final int CANNOT_GROUP = 2;

  /** Patterns to be used when constructing a pattern for output filenames. */
  public static final String SERIES_NUM = "%s";
  public static final String SERIES_NAME = "%n";
  public static final String CHANNEL_NUM = "%c";
  public static final String CHANNEL_NAME = "%w";
  public static final String Z_NUM = "%z";
  public static final String T_NUM = "%t";
  public static final String TIMESTAMP = "%A";

  // -- Constants - versioning --

  /**
   * Current SVN revision.
   * @deprecated After Git move, deprecated in favour of {@link #VCS_REVISION}.
   */
  @Deprecated
  public static final String SVN_REVISION = "@vcs.revision@";

  /** Current VCS revision. */
  public static final String VCS_REVISION = "@vcs.revision@";

  /** Date on which this release was built. */
  public static final String DATE = "@date@";

  /** Version number of this release. */
  public static final String VERSION = "4.3.3-DEV";

  // -- Constants - domains --

  /** Identifies the high content screening domain. */
  public static final String HCS_DOMAIN = "High-Content Screening (HCS)";

  /** Identifies the light microscopy domain. */
  public static final String LM_DOMAIN = "Light Microscopy";

  /** Identifies the electron microscopy domain. */
  public static final String EM_DOMAIN = "Electron Microscopy (EM)";

  /** Identifies the scanning probe microscopy domain. */
  public static final String SPM_DOMAIN = "Scanning Probe Microscopy (SPM)";

  /** Identifies the scanning electron microscopy domain. */
  public static final String SEM_DOMAIN = "Scanning Electron Microscopy (SEM)";

  /** Identifies the fluorescence-lifetime domain. */
  public static final String FLIM_DOMAIN = "Fluorescence-Lifetime Imaging";

  /** Identifies the medical imaging domain. */
  public static final String MEDICAL_DOMAIN = "Medical Imaging";

  /** Identifies the histology domain. */
  public static final String HISTOLOGY_DOMAIN = "Histology";

  /** Identifies the gel and blot imaging domain. */
  public static final String GEL_DOMAIN = "Gel/Blot Imaging";

  /** Identifies the astronomy domain. */
  public static final String ASTRONOMY_DOMAIN = "Astronomy";

  /**
   * Identifies the graphics domain.
   * This includes formats used exclusively by analysis software.
   */
  public static final String GRAPHICS_DOMAIN = "Graphics";

  /** Identifies an unknown domain. */
  public static final String UNKNOWN_DOMAIN = "Unknown";

  /** List of non-graphics domains. */
  public static final String[] NON_GRAPHICS_DOMAINS = new String[] {
      LM_DOMAIN, EM_DOMAIN, SPM_DOMAIN, SEM_DOMAIN, FLIM_DOMAIN,
      MEDICAL_DOMAIN, HISTOLOGY_DOMAIN, GEL_DOMAIN, ASTRONOMY_DOMAIN,
      HCS_DOMAIN, UNKNOWN_DOMAIN};

  /** List of non-HCS domains. */
  public static final String[] NON_HCS_DOMAINS = new String[] {
      LM_DOMAIN, EM_DOMAIN, SPM_DOMAIN, SEM_DOMAIN, FLIM_DOMAIN,
      MEDICAL_DOMAIN, HISTOLOGY_DOMAIN, GEL_DOMAIN, ASTRONOMY_DOMAIN,
      UNKNOWN_DOMAIN};

  /**
   * List of domains that do not require special handling.  Domains that
   * require special handling are {@link #GRAPHICS_DOMAIN} and
   * {@link #HCS_DOMAIN}.
   */
  public static final String[] NON_SPECIAL_DOMAINS = new String[] {
      LM_DOMAIN, EM_DOMAIN, SPM_DOMAIN, SEM_DOMAIN, FLIM_DOMAIN,
      MEDICAL_DOMAIN, HISTOLOGY_DOMAIN, GEL_DOMAIN, ASTRONOMY_DOMAIN,
      UNKNOWN_DOMAIN};

  /** List of all supported domains. */
  public static final String[] ALL_DOMAINS = new String[] {
      HCS_DOMAIN, LM_DOMAIN, EM_DOMAIN, SPM_DOMAIN, SEM_DOMAIN, FLIM_DOMAIN,
      MEDICAL_DOMAIN, HISTOLOGY_DOMAIN, GEL_DOMAIN, ASTRONOMY_DOMAIN,
      GRAPHICS_DOMAIN, UNKNOWN_DOMAIN};

  // -- Constants - web pages --

  /** URL of Bio-Formats web page. */
  public static final String URL_BIO_FORMATS =
    "http://www.loci.wisc.edu/software/bio-formats";

  /** URL of 'Bio-Formats as a Java Library' web page. */
  public static final String URL_BIO_FORMATS_LIBRARIES =
    "http://www.loci.wisc.edu/bio-formats/bio-formats-java-library";

  /** URL of OME-TIFF web page. */
  public static final String URL_OME_TIFF = "http://ome-xml.org/wiki/OmeTiff";

  // -- Constructor --

  private FormatTools() {
  }

  // -- Utility methods - sanity checking

  /**
   * Asserts that the current file is either null, or not, according to the
   * given flag. If the assertion fails, an IllegalStateException is thrown.
   * @param currentId File name to test.
   * @param notNull True iff id should be non-null.
   * @param depth How far back in the stack the calling method is; this name
   *   is reported as part of the exception message, if available. Use zero
   *   to suppress output of the calling method name.
   */
  public static void assertStream(RandomAccessInputStream stream,
    boolean notNull, int depth)
  {
    String msg = null;
    if (stream == null && notNull) {
      msg = "Current file should not be null; call setId(String) first";
    }
    else if (stream != null && !notNull) {
      msg =
        "Current file should be null, but is '" + stream +
          "'; call close() first";
    }
    if (msg == null) return;

    StackTraceElement[] ste = new Exception().getStackTrace();
    String header;
    if (depth > 0 && ste.length > depth) {
      String c = ste[depth].getClassName();
      if (c.startsWith("ome.scifio.")) {
        c = c.substring(c.lastIndexOf(".") + 1);
      }
      header = c + "." + ste[depth].getMethodName() + ": ";
    }
    else header = "";
    throw new IllegalStateException(header + msg);
  }

  /**
   * Convenience method for checking that the plane number, tile size and
   * buffer sizes are all valid for the given reader.
   * If 'bufLength' is less than 0, then the buffer length check is not
   * performed.
   */
  public static void checkPlaneParameters(Reader r, int no, int bufLength,
    int x, int y, int w, int h) throws FormatException
  {
    assertStream(r.getStream(), true, 2);
    checkPlaneNumber(r, no);
    checkTileSize(r, x, y, w, h, no);
    if (bufLength >= 0) checkBufferSize(r, bufLength, w, h, no);
  }

  /** Checks that the given plane number is valid for the given reader. */
  public static void checkPlaneNumber(Reader r, int no) throws FormatException {
    int imageCount = r.getMetadata().getImageCount();
    if (no < 0 || no >= imageCount) {
      throw new FormatException("Invalid image number: " + no + " (" +
      /* TODO series=" +
      r.getMetadata().getSeries() + ", */"imageCount=" + imageCount + ")");
    }
  }

  /** Checks that the given tile size is valid for the given reader. */
  public static void checkTileSize(Reader r, int x, int y, int w, int h, int no)
    throws FormatException
  {
    int width = r.getMetadata().getSizeX(no);
    int height = r.getMetadata().getSizeY(no);
    if (x < 0 || y < 0 || w < 0 || h < 0 || (x + w) > width || (y + h) > height)
    {
      throw new FormatException("Invalid tile size: x=" + x + ", y=" + y +
        ", w=" + w + ", h=" + h);
    }
  }

  public static void checkBufferSize(int no, Reader r, int len)
    throws FormatException
  {
    checkBufferSize(r, len, r.getMetadata().getSizeX(no), r.getMetadata()
      .getSizeY(no), no);
  }

  /**
   * Checks that the given buffer size is large enough to hold a w * h
   * image as returned by the given reader.
   * @throws FormatException if the buffer is too small
   */
  public static void checkBufferSize(Reader r, int len, int w, int h, int no)
    throws FormatException
  {
    int size = getPlaneSize(r, w, h, no);
    if (size > len) {
      throw new FormatException("Buffer too small (got " + len + ", expected " +
        size + ").");
    }
  }

  /** Returns the size in bytes of a single plane. */
  public static int getPlaneSize(Reader r, int no) {
    return getPlaneSize(r, r.getMetadata().getSizeX(no), r.getMetadata()
      .getSizeY(no), no);
  }

  /** Returns the size in bytes of a w * h tile. */
  public static int getPlaneSize(Reader r, int w, int h, int no) {
    return w * h * r.getMetadata().getRGBChannelCount(no) *
      getBytesPerPixel(r.getMetadata().getPixelType(no));
  }

  // -- Utility methods - pixel types --

  /**
   * Takes a string value and maps it to one of the pixel type enumerations.
   * @param pixelTypeAsString the pixel type as a string.
   * @return type enumeration value for use with class constants.
   */
  public static int pixelTypeFromString(String pixelTypeAsString) {
    String lowercaseTypeAsString = pixelTypeAsString.toLowerCase();
    for (int i = 0; i < pixelTypes.length; i++) {
      if (pixelTypes[i].equals(lowercaseTypeAsString)) return i;
    }
    throw new IllegalArgumentException("Unknown type: '" + pixelTypeAsString +
      "'");
  }

  /**
   * Takes a pixel type value and gets a corresponding string representation.
   * @param pixelType the pixel type.
   * @return string value for human-readable output.
   */
  public static String getPixelTypeString(int pixelType) {
    if (pixelType < 0 || pixelType >= pixelTypes.length) {
      throw new IllegalArgumentException("Unknown pixel type: " + pixelType);
    }
    return pixelTypes[pixelType];
  }

  /**
   * Retrieves how many bytes per pixel the current plane or section has.
   * @param pixelType the pixel type as retrieved from
   * @return the number of bytes per pixel.
   * @see ome.scifio.Metadata#getPixelType()
   */
  public static int getBytesPerPixel(int pixelType) {
    switch (pixelType) {
      case INT8:
      case UINT8:
        return 1;
      case INT16:
      case UINT16:
        return 2;
      case INT32:
      case UINT32:
      case FLOAT:
        return 4;
      case DOUBLE:
        return 8;
    }
    throw new IllegalArgumentException("Unknown pixel type: " + pixelType);
  }

  /**
   * Retrieves how many bytes per pixel the current plane or section has.
   * @param pixelType the pixel type as retrieved from
   *   {@link ome.scifio.Metadata#getPixelType()}.
   * @return the number of bytes per pixel.
   * @see ome.scifio.Metadata#getPixelType()
   */
  public static int getBitsPerPixel(int pixelType) {
    return 8 * FormatTools.getBytesPerPixel(pixelType);
  }

  /**
   * Retrieves the number of bytes per pixel in the current plane.
   * @param pixelType the pixel type, as a String.
   * @return the number of bytes per pixel.
   * @see #pixelTypeFromString(String)
   * @see #getBytesPerPixel(int)
   */
  public static int getBytesPerPixel(String pixelType) {
    return getBytesPerPixel(pixelTypeFromString(pixelType));
  }

  /**
   * Determines whether the given pixel type is floating point or integer.
   * @param pixelType the pixel type as retrieved from
   *   {@link ome.scifio.Metadata#getPixelType()}.
   * @return true if the pixel type is floating point.
   * @see ome.scifio.Metadata#getPixelType()
   */
  public static boolean isFloatingPoint(int pixelType) {
    switch (pixelType) {
      case INT8:
      case UINT8:
      case INT16:
      case UINT16:
      case INT32:
      case UINT32:
        return false;
      case FLOAT:
      case DOUBLE:
        return true;
    }
    throw new IllegalArgumentException("Unknown pixel type: " + pixelType);
  }

  /**
   * Determines whether the given pixel type is signed or unsigned.
   * @param pixelType the pixel type as retrieved from
   *   {@link ome.scifio.Metadata#getPixelType()}.
   * @return true if the pixel type is signed.
   * @see ome.scifio.Metadata#getPixelType()
   */
  public static boolean isSigned(int pixelType) {
    switch (pixelType) {
      case INT8:
      case INT16:
      case INT32:
      case FLOAT:
      case DOUBLE:
        return true;
      case UINT8:
      case UINT16:
      case UINT32:
        return false;
    }
    throw new IllegalArgumentException("Unknown pixel type: " + pixelType);
  }

  /**
   * Returns an appropriate pixel type given the number of bytes per pixel.
   *
   * @param bytes number of bytes per pixel.
   * @param signed whether or not the pixel type should be signed.
   * @param fp whether or not these are floating point pixels.
   */
  public static int pixelTypeFromBytes(int bytes, boolean signed, boolean fp)
    throws FormatException
  {
    switch (bytes) {
      case 1:
        return signed ? INT8 : UINT8;
      case 2:
        return signed ? INT16 : UINT16;
      case 4:
        return fp ? FLOAT : signed ? INT32 : UINT32;
      case 8:
        return DOUBLE;
      default:
        throw new FormatException("Unsupported byte depth: " + bytes);
    }
  }

  /**
   * Gets the Z, C and T coordinates corresponding
   * to the given rasterized index value.
   */
  public static int[] getZCTCoords(Reader reader, int index) {
    String order = reader.getMetadata().getDimensionOrder(index);
    int zSize = reader.getMetadata().getSizeZ(index);
    int cSize = reader.getMetadata().getEffectiveSizeC(index);
    int tSize = reader.getMetadata().getSizeT(index);
    int num = reader.getMetadata().getImageCount();
    return getZCTCoords(order, zSize, cSize, tSize, num, index);
  }

  /**
   * Gets the Z, C and T coordinates corresponding to the given rasterized
   * index value.
   *
   * @param order Dimension order.
   * @param zSize Total number of focal planes.
   * @param cSize Total number of channels.
   * @param tSize Total number of time points.
   * @param num Total number of image planes (zSize * cSize * tSize),
   *   specified as a consistency check.
   * @param index 1D (rasterized) index to convert to ZCT coordinate triple.
   */
  public static int[] getZCTCoords(String order, int zSize, int cSize,
    int tSize, int num, int index)
  {
    // check DimensionOrder
    if (order == null) {
      throw new IllegalArgumentException("Dimension order is null");
    }
    if (!order.startsWith("XY") && !order.startsWith("YX")) {
      throw new IllegalArgumentException("Invalid dimension order: " + order);
    }
    int iz = order.indexOf("Z") - 2;
    int ic = order.indexOf("C") - 2;
    int it = order.indexOf("T") - 2;
    if (iz < 0 || iz > 2 || ic < 0 || ic > 2 || it < 0 || it > 2) {
      throw new IllegalArgumentException("Invalid dimension order: " + order);
    }

    // check SizeZ
    if (zSize <= 0) {
      throw new IllegalArgumentException("Invalid Z size: " + zSize);
    }

    // check SizeC
    if (cSize <= 0) {
      throw new IllegalArgumentException("Invalid C size: " + cSize);
    }

    // check SizeT
    if (tSize <= 0) {
      throw new IllegalArgumentException("Invalid T size: " + tSize);
    }

    // check image count
    if (num <= 0) {
      throw new IllegalArgumentException("Invalid image count: " + num);
    }
    if (num != zSize * cSize * tSize) {
      // if this happens, there is probably a bug in metadata population --
      // either one of the ZCT sizes, or the total number of images --
      // or else the input file is invalid
      throw new IllegalArgumentException("ZCT size vs image count mismatch " +
        "(sizeZ=" + zSize + ", sizeC=" + cSize + ", sizeT=" + tSize +
        ", total=" + num + ")");
    }
    if (index < 0 || index >= num) {
      throw new IllegalArgumentException("Invalid image index: " + index + "/" +
        num);
    }

    // assign rasterization order
    int len0 = iz == 0 ? zSize : (ic == 0 ? cSize : tSize);
    int len1 = iz == 1 ? zSize : (ic == 1 ? cSize : tSize);
    //int len2 = iz == 2 ? sizeZ : (ic == 2 ? sizeC : sizeT);
    int v0 = index % len0;
    int v1 = index / len0 % len1;
    int v2 = index / len0 / len1;
    int z = iz == 0 ? v0 : (iz == 1 ? v1 : v2);
    int c = ic == 0 ? v0 : (ic == 1 ? v1 : v2);
    int t = it == 0 ? v0 : (it == 1 ? v1 : v2);

    return new int[] {z, c, t};
  }

  /**
   * Gets the rasterized index corresponding
   * to the given Z, C and T coordinates.
   */
  public static int getIndex(Reader reader, int image, int z, int c, int t) {
    String order = reader.getMetadata().getDimensionOrder(image);
    int zSize = reader.getMetadata().getSizeZ(image);
    int cSize = reader.getMetadata().getEffectiveSizeC(image);
    int tSize = reader.getMetadata().getSizeT(image);
    int num = reader.getMetadata().getImageCount();
    return getIndex(order, zSize, cSize, tSize, num, z, c, t);
  }

  /**
   * Gets the rasterized index corresponding
   * to the given Z, C and T coordinates.
   *
   * @param order Dimension order.
   * @param zSize Total number of focal planes.
   * @param cSize Total number of channels.
   * @param tSize Total number of time points.
   * @param num Total number of image planes (zSize * cSize * tSize),
   *   specified as a consistency check.
   * @param z Z coordinate of ZCT coordinate triple to convert to 1D index.
   * @param c C coordinate of ZCT coordinate triple to convert to 1D index.
   * @param t T coordinate of ZCT coordinate triple to convert to 1D index.
   */
  public static int getIndex(String order, int zSize, int cSize, int tSize,
    int num, int z, int c, int t)
  {
    // check DimensionOrder
    if (order == null) {
      throw new IllegalArgumentException("Dimension order is null");
    }
    if (!order.startsWith("XY") && !order.startsWith("YX")) {
      throw new IllegalArgumentException("Invalid dimension order: " + order);
    }
    int iz = order.indexOf("Z") - 2;
    int ic = order.indexOf("C") - 2;
    int it = order.indexOf("T") - 2;
    if (iz < 0 || iz > 2 || ic < 0 || ic > 2 || it < 0 || it > 2) {
      throw new IllegalArgumentException("Invalid dimension order: " + order);
    }

    // check SizeZ
    if (zSize <= 0) {
      throw new IllegalArgumentException("Invalid Z size: " + zSize);
    }
    if (z < 0 || z >= zSize) {
      throw new IllegalArgumentException("Invalid Z index: " + z + "/" + zSize);
    }

    // check SizeC
    if (cSize <= 0) {
      throw new IllegalArgumentException("Invalid C size: " + cSize);
    }
    if (c < 0 || c >= cSize) {
      throw new IllegalArgumentException("Invalid C index: " + c + "/" + cSize);
    }

    // check SizeT
    if (tSize <= 0) {
      throw new IllegalArgumentException("Invalid T size: " + tSize);
    }
    if (t < 0 || t >= tSize) {
      throw new IllegalArgumentException("Invalid T index: " + t + "/" + tSize);
    }

    // check image count
    if (num <= 0) {
      throw new IllegalArgumentException("Invalid image count: " + num);
    }
    if (num != zSize * cSize * tSize) {
      // if this happens, there is probably a bug in metadata population --
      // either one of the ZCT sizes, or the total number of images --
      // or else the input file is invalid
      throw new IllegalArgumentException("ZCT size vs image count mismatch " +
        "(sizeZ=" + zSize + ", sizeC=" + cSize + ", sizeT=" + tSize +
        ", total=" + num + ")");
    }

    // assign rasterization order
    int v0 = iz == 0 ? z : (ic == 0 ? c : t);
    int v1 = iz == 1 ? z : (ic == 1 ? c : t);
    int v2 = iz == 2 ? z : (ic == 2 ? c : t);
    int len0 = iz == 0 ? zSize : (ic == 0 ? cSize : tSize);
    int len1 = iz == 1 ? zSize : (ic == 1 ? cSize : tSize);

    return v0 + v1 * len0 + v2 * len0 * len1;
  }
}