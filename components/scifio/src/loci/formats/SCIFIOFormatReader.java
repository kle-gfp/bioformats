package loci.formats;

import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

import loci.common.DataTools;
import loci.common.Location;
import loci.common.RandomAccessInputStream;
import loci.common.services.DependencyException;
import loci.common.services.ServiceFactory;
import loci.formats.in.DefaultMetadataOptions;
import loci.formats.in.MetadataLevel;
import loci.formats.in.MetadataOptions;
import loci.formats.meta.FilterMetadata;
import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataStore;
import loci.formats.ome.OMEXMLMetadata;
import loci.formats.services.OMEXMLService;
import ome.scifio.Checker;
import ome.scifio.Parser;
import ome.scifio.Reader;
import ome.scifio.Translator;
import ome.xml.model.enums.AcquisitionMode;
import ome.xml.model.enums.ArcType;
import ome.xml.model.enums.Binning;
import ome.xml.model.enums.Compression;
import ome.xml.model.enums.ContrastMethod;
import ome.xml.model.enums.Correction;
import ome.xml.model.enums.DetectorType;
import ome.xml.model.enums.DimensionOrder;
import ome.xml.model.enums.EnumerationException;
import ome.xml.model.enums.ExperimentType;
import ome.xml.model.enums.FilamentType;
import ome.xml.model.enums.FillRule;
import ome.xml.model.enums.FilterType;
import ome.xml.model.enums.FontFamily;
import ome.xml.model.enums.FontStyle;
import ome.xml.model.enums.IlluminationType;
import ome.xml.model.enums.Immersion;
import ome.xml.model.enums.LaserMedium;
import ome.xml.model.enums.LaserType;
import ome.xml.model.enums.LineCap;
import ome.xml.model.enums.Marker;
import ome.xml.model.enums.Medium;
import ome.xml.model.enums.MicrobeamManipulationType;
import ome.xml.model.enums.MicroscopeType;
import ome.xml.model.enums.NamingConvention;
import ome.xml.model.enums.PixelType;
import ome.xml.model.enums.Pulse;
import ome.xml.model.enums.handlers.AcquisitionModeEnumHandler;
import ome.xml.model.enums.handlers.ArcTypeEnumHandler;
import ome.xml.model.enums.handlers.BinningEnumHandler;
import ome.xml.model.enums.handlers.CompressionEnumHandler;
import ome.xml.model.enums.handlers.ContrastMethodEnumHandler;
import ome.xml.model.enums.handlers.CorrectionEnumHandler;
import ome.xml.model.enums.handlers.DetectorTypeEnumHandler;
import ome.xml.model.enums.handlers.DimensionOrderEnumHandler;
import ome.xml.model.enums.handlers.ExperimentTypeEnumHandler;
import ome.xml.model.enums.handlers.FilamentTypeEnumHandler;
import ome.xml.model.enums.handlers.FillRuleEnumHandler;
import ome.xml.model.enums.handlers.FilterTypeEnumHandler;
import ome.xml.model.enums.handlers.FontFamilyEnumHandler;
import ome.xml.model.enums.handlers.FontStyleEnumHandler;
import ome.xml.model.enums.handlers.IlluminationTypeEnumHandler;
import ome.xml.model.enums.handlers.ImmersionEnumHandler;
import ome.xml.model.enums.handlers.LaserMediumEnumHandler;
import ome.xml.model.enums.handlers.LaserTypeEnumHandler;
import ome.xml.model.enums.handlers.LineCapEnumHandler;
import ome.xml.model.enums.handlers.MarkerEnumHandler;
import ome.xml.model.enums.handlers.MediumEnumHandler;
import ome.xml.model.enums.handlers.MicrobeamManipulationTypeEnumHandler;
import ome.xml.model.enums.handlers.MicroscopeTypeEnumHandler;
import ome.xml.model.enums.handlers.NamingConventionEnumHandler;
import ome.xml.model.enums.handlers.PixelTypeEnumHandler;
import ome.xml.model.enums.handlers.PulseEnumHandler;

/**
 * Abstract superclass of all biological file format writers.
 * Defers to ome.scifio.Reader
 *
 */
public abstract class SCIFIOFormatReader extends FormatReader {

  // -- Fields --
  
  /** Scifio Checker for deference */
  protected Checker checker;
  
  /** Scifio Parser for deference */
  protected Parser parser;
  
  /** Scifio Reader for deference */
  protected Reader reader;

  /** Scifio Translator for deference */
  protected Translator translator;
  
  // -- Constructors --

  public SCIFIOFormatReader(String format, String suffix) {
    super(format, suffix);
  }
  
  public SCIFIOFormatReader(String format, String[] suffixes) {
    super(format, suffixes);
  }
  
  // -- Internal FormatReader API methods --

  /**
   * Initializes the given file (parsing header information, etc.).
   * Most subclasses should override this method to perform
   * initialization operations such as parsing metadata.
   *
   * @throws FormatException if a parsing error occurs processing the file.
   * @throws IOException if an I/O error occurs processing the file
   */
  protected void initFile(String id) throws FormatException, IOException {
    LOGGER.debug("{}.initFile({})", this.getClass().getName(), id);
    if (currentId != null) {
      String[] s = getUsedFiles();
      for (int i=0; i<s.length; i++) {
        if (id.equals(s[i])) return;
      }
    }

    series = 0;
    close();
    currentId = id;
    // reinitialize the MetadataStore
    // NB: critical for metadata conversion to work properly!
    getMetadataStore().createRoot();
  }

  /** Returns true if the given file name is in the used files list. */
  protected boolean isUsedFile(String file) {
    String[] usedFiles = getUsedFiles();
    for (String used : usedFiles) {
      if (used.equals(file)) return true;
      String path = new Location(file).getAbsolutePath();
      if (used.equals(path)) return true;
    }
    return false;
  }

  /** Adds an entry to the specified Hashtable. */
  protected void addMeta(String key, Object value,
    Hashtable<String, Object> meta)
  {
    if (key == null || value == null || !isMetadataCollected()) {
      return;
    }

    key = key.trim();

    boolean string = value instanceof String || value instanceof Character;
    boolean simple = string ||
      value instanceof Number ||
      value instanceof Boolean;

    // string value, if passed in value is a string
    String val = string ? String.valueOf(value) : null;

    if (filterMetadata ||
      (saveOriginalMetadata && (getMetadataStore() instanceof OMEXMLMetadata)))
    {
      // filter out complex data types
      if (!simple) return;

      // verify key & value are reasonable length
      int maxLen = 8192;
      if (key.length() > maxLen) return;
      if (string && val.length() > maxLen) return;

      // remove all non-printable characters
      key = DataTools.sanitize(key);
      if (string) val = DataTools.sanitize(val);

      // verify key contains at least one alphabetic character
      if (!key.matches(".*[a-zA-Z].*")) return;

      // remove &lt;, &gt; and &amp; to prevent XML parsing errors
      String[] invalidSequences = new String[] {
        "&lt;", "&gt;", "&amp;", "<", ">", "&"
      };
      for (int i=0; i<invalidSequences.length; i++) {
        key = key.replaceAll(invalidSequences[i], "");
        if (string) val = val.replaceAll(invalidSequences[i], "");
      }

      // verify key & value are not empty
      if (key.length() == 0) return;
      if (string && val.trim().length() == 0) return;

      if (string) value = val;
    }

    meta.put(key, val == null ? value : val);
  }

  /** Adds an entry to the global metadata table. */
  protected void addGlobalMeta(String key, Object value) {
    addMeta(key, value, getGlobalMetadata());
  }

  /** Adds an entry to the global metadata table. */
  protected void addGlobalMeta(String key, boolean value) {
    addGlobalMeta(key, new Boolean(value));
  }

  /** Adds an entry to the global metadata table. */
  protected void addGlobalMeta(String key, byte value) {
    addGlobalMeta(key, new Byte(value));
  }

  /** Adds an entry to the global metadata table. */
  protected void addGlobalMeta(String key, short value) {
    addGlobalMeta(key, new Short(value));
  }

  /** Adds an entry to the global metadata table. */
  protected void addGlobalMeta(String key, int value) {
    addGlobalMeta(key, new Integer(value));
  }

  /** Adds an entry to the global metadata table. */
  protected void addGlobalMeta(String key, long value) {
    addGlobalMeta(key, new Long(value));
  }

  /** Adds an entry to the global metadata table. */
  protected void addGlobalMeta(String key, float value) {
    addGlobalMeta(key, new Float(value));
  }

  /** Adds an entry to the global metadata table. */
  protected void addGlobalMeta(String key, double value) {
    addGlobalMeta(key, new Double(value));
  }

  /** Adds an entry to the global metadata table. */
  protected void addGlobalMeta(String key, char value) {
    addGlobalMeta(key, new Character(value));
  }

  /** Gets a value from the global metadata table. */
  protected Object getGlobalMeta(String key) {
    return metadata.get(key);
  }

  /** Adds an entry to the metadata table for the current series. */
  protected void addSeriesMeta(String key, Object value) {
    addMeta(key, value, core[series].seriesMetadata);
  }

  /** Adds an entry to the metadata table for the current series. */
  protected void addSeriesMeta(String key, boolean value) {
    addSeriesMeta(key, new Boolean(value));
  }

  /** Adds an entry to the metadata table for the current series. */
  protected void addSeriesMeta(String key, byte value) {
    addSeriesMeta(key, new Byte(value));
  }

  /** Adds an entry to the metadata table for the current series. */
  protected void addSeriesMeta(String key, short value) {
    addSeriesMeta(key, new Short(value));
  }

  /** Adds an entry to the metadata table for the current series. */
  protected void addSeriesMeta(String key, int value) {
    addSeriesMeta(key, new Integer(value));
  }

  /** Adds an entry to the metadata table for the current series. */
  protected void addSeriesMeta(String key, long value) {
    addSeriesMeta(key, new Long(value));
  }

  /** Adds an entry to the metadata table for the current series. */
  protected void addSeriesMeta(String key, float value) {
    addSeriesMeta(key, new Float(value));
  }

  /** Adds an entry to the metadata table for the current series. */
  protected void addSeriesMeta(String key, double value) {
    addSeriesMeta(key, new Double(value));
  }

  /** Adds an entry to the metadata table for the current series. */
  protected void addSeriesMeta(String key, char value) {
    addSeriesMeta(key, new Character(value));
  }

  /** Gets an entry from the metadata table for the current series. */
  protected Object getSeriesMeta(String key) {
    return core[series].seriesMetadata.get(key);
  }

  /** Reads a raw plane from disk. */
  protected byte[] readPlane(RandomAccessInputStream s, int x, int y,
    int w, int h, byte[] buf) throws IOException
  {
    return readPlane(s, x, y, w, h, 0, buf);
  }

  /** Reads a raw plane from disk. */
  protected byte[] readPlane(RandomAccessInputStream s, int x, int y,
    int w, int h, int scanlinePad, byte[] buf) throws IOException
  {
    int c = getRGBChannelCount();
    int bpp = FormatTools.getBytesPerPixel(getPixelType());
    if (x == 0 && y == 0 && w == getSizeX() && h == getSizeY() &&
      scanlinePad == 0)
    {
      s.read(buf);
    }
    else if (x == 0 && w == getSizeX() && scanlinePad == 0) {
      if (isInterleaved()) {
        s.skipBytes(y * w * bpp * c);
        s.read(buf, 0, h * w * bpp * c);
      }
      else {
        int rowLen = w * bpp;
        for (int channel=0; channel<c; channel++) {
          s.skipBytes(y * rowLen);
          s.read(buf, channel * h * rowLen, h * rowLen);
          if (channel < c - 1) {
            // no need to skip bytes after reading final channel
            s.skipBytes((getSizeY() - y - h) * rowLen);
          }
        }
      }
    }
    else {
      int scanlineWidth = getSizeX() + scanlinePad;
      if (isInterleaved()) {
        s.skipBytes(y * scanlineWidth * bpp * c);
        for (int row=0; row<h; row++) {
          s.skipBytes(x * bpp * c);
          s.read(buf, row * w * bpp * c, w * bpp * c);
          if (row < h - 1) {
            // no need to skip bytes after reading final row
            s.skipBytes(bpp * c * (scanlineWidth - w - x));
          }
        }
      }
      else {
        for (int channel=0; channel<c; channel++) {
          s.skipBytes(y * scanlineWidth * bpp);
          for (int row=0; row<h; row++) {
            s.skipBytes(x * bpp);
            s.read(buf, channel * w * h * bpp + row * w * bpp, w * bpp);
            if (row < h - 1 || channel < c - 1) {
              // no need to skip bytes after reading final row of final channel
              s.skipBytes(bpp * (scanlineWidth - w - x));
            }
          }
          if (channel < c - 1) {
            // no need to skip bytes after reading final channel
            s.skipBytes(scanlineWidth * bpp * (getSizeY() - y - h));
          }
        }
      }
    }
    return buf;
  }

  /** Return a properly configured loci.formats.meta.FilterMetadata. */
  protected MetadataStore makeFilterMetadata() {
    return new FilterMetadata(getMetadataStore(), isMetadataFiltered());
  }

  // -- IMetadataConfigurable API methods --

  /* (non-Javadoc)
   * @see loci.formats.IMetadataConfigurable#getSupportedMetadataLevels()
   */
  public Set<MetadataLevel> getSupportedMetadataLevels() {
    Set<MetadataLevel> supportedLevels = new HashSet<MetadataLevel>();
    supportedLevels.add(MetadataLevel.ALL);
    supportedLevels.add(MetadataLevel.NO_OVERLAYS);
    supportedLevels.add(MetadataLevel.MINIMUM);
    return supportedLevels;
  }

  /* (non-Javadoc)
   * @see loci.formats.IMetadataConfigurable#getMetadataOptions()
   */
  public MetadataOptions getMetadataOptions() {
    return metadataOptions;
  }

  // -- IFormatReader API methods --

  /**
   * Checks if a file matches the type of this format reader.
   * Checks filename suffixes against those known for this format.
   * If the suffix check is inconclusive and the open parameter is true,
   * the file is opened and tested with
   * {@link #isThisType(RandomAccessInputStream)}.
   *
   * @param open If true, and the file extension is insufficient to determine
   *   the file type, the (existing) file is opened for further analysis.
   */
  @Deprecated
  @Override
  public boolean isThisType(String name, boolean open) {
	  return checker.isFormat(name, open);
  }

  /* @see IFormatReader#isThisType(byte[]) */
  @Deprecated
  @Override
  public boolean isThisType(byte[] block) {
    return checker.isFormat(block);
  }

  /* @see IFormatReader#isThisType(RandomAccessInputStream) */
  @Deprecated
  @Override
  public boolean isThisType(RandomAccessInputStream stream) throws IOException {
    return checker.isFormat(new ome.scifio.io.RandomAccessInputStream(stream.getFile()));
  }

  /* @see IFormatReader#getImageCount() */
  @Deprecated
  @Override
  public int getImageCount() {
    return reader.getMetadata().getImageCount();
  }

  /* @see IFormatReader#isRGB() */
  @Deprecated
  @Override
  public boolean isRGB() {
    return reader.getMetadata().isRGB(getSeries());
  }

  /* @see IFormatReader#getSizeX() */
  @Deprecated
  @Override
  public int getSizeX() {
    return reader.getMetadata().getSizeX(getSeries());
  }

  /* @see IFormatReader#getSizeY() */
  @Deprecated
  @Override
  public int getSizeY() {
    return reader.getMetadata().getSizeY(getSeries());
  }

  /* @see IFormatReader#getSizeZ() */
  @Deprecated
  @Override
  public int getSizeZ() {
    return reader.getMetadata().getSizeZ(getSeries());
  }

  /* @see IFormatReader#getSizeC() */
  @Deprecated
  @Override
  public int getSizeC() {
    return reader.getMetadata().getSizeC(getSeries());
  }

  /* @see IFormatReader#getSizeT() */
  @Deprecated
  @Override
  public int getSizeT() {
    return reader.getMetadata().getSizeT(getSeries());
  }

  /* @see IFormatReader#getPixelType() */
  @Deprecated
  @Override
  public int getPixelType() {
    return reader.getMetadata().getPixelType(getSeries());
  }

  /* @see IFormatReader#getBitsPerPixel() */
  @Deprecated
  @Override
  public int getBitsPerPixel() {
    return reader.getMetadata().getBitsPerPixel(getSeries());
  }

  /* @see IFormatReader#getEffectiveSizeC() */
  @Deprecated
  @Override
  public int getEffectiveSizeC() {
    // NB: by definition, imageCount == effectiveSizeC * sizeZ * sizeT
    int sizeZT = getSizeZ() * getSizeT();
    if (sizeZT == 0) return 0;
    return getImageCount() / sizeZT;
  }

  /* @see IFormatReader#getRGBChannelCount() */
  @Deprecated
  @Override
  public int getRGBChannelCount() {
    int effSizeC = getEffectiveSizeC();
    if (effSizeC == 0) return 0;
    return getSizeC() / effSizeC;
  }

  /* @see IFormatReader#isIndexed() */
  @Deprecated
  @Override
  public boolean isIndexed() {
    return reader.getMetadata().isIndexed(getSeries());
  }

  /* @see IFormatReader#isFalseColor() */
  public boolean isFalseColor() {
    FormatTools.assertId(currentId, true, 1);
    return core[series].falseColor;
  }

  /* @see IFormatReader#get8BitLookupTable() */
  public byte[][] get8BitLookupTable() throws FormatException, IOException {
    return null;
  }

  /* @see IFormatReader#get16BitLookupTable() */
  public short[][] get16BitLookupTable() throws FormatException, IOException {
    return null;
  }

  /* @see IFormatReader#getChannelDimLengths() */
  public int[] getChannelDimLengths() {
    FormatTools.assertId(currentId, true, 1);
    if (core[series].cLengths == null) return new int[] {core[series].sizeC};
    return core[series].cLengths;
  }

  /* @see IFormatReader#getChannelDimTypes() */
  public String[] getChannelDimTypes() {
    FormatTools.assertId(currentId, true, 1);
    if (core[series].cTypes == null) return new String[] {FormatTools.CHANNEL};
    return core[series].cTypes;
  }

  /* @see IFormatReader#getThumbSizeX() */
  public int getThumbSizeX() {
    FormatTools.assertId(currentId, true, 1);
    if (core[series].thumbSizeX == 0) {
      int sx = getSizeX();
      int sy = getSizeY();
      int thumbSizeX = 0;
      if (sx > sy) thumbSizeX = THUMBNAIL_DIMENSION;
      else if (sy > 0) thumbSizeX = sx * THUMBNAIL_DIMENSION / sy;
      if (thumbSizeX == 0) thumbSizeX = 1;
      return thumbSizeX;
    }
    return core[series].thumbSizeX;
  }

  /* @see IFormatReader#getThumbSizeY() */
  public int getThumbSizeY() {
    FormatTools.assertId(currentId, true, 1);
    if (core[series].thumbSizeY == 0) {
      int sx = getSizeX();
      int sy = getSizeY();
      int thumbSizeY = 1;
      if (sy > sx) thumbSizeY = THUMBNAIL_DIMENSION;
      else if (sx > 0) thumbSizeY = sy * THUMBNAIL_DIMENSION / sx;
      if (thumbSizeY == 0) thumbSizeY = 1;
      return thumbSizeY;
    }
    return core[series].thumbSizeY;
  }

  /* @see IFormatReader.isLittleEndian() */
  public boolean isLittleEndian() {
    FormatTools.assertId(currentId, true, 1);
    return core[series].littleEndian;
  }

  /* @see IFormatReader#getDimensionOrder() */
  public String getDimensionOrder() {
    FormatTools.assertId(currentId, true, 1);
    return core[series].dimensionOrder;
  }

  /* @see IFormatReader#isOrderCertain() */
  public boolean isOrderCertain() {
    FormatTools.assertId(currentId, true, 1);
    return core[series].orderCertain;
  }

  /* @see IFormatReader#isThumbnailSeries() */
  public boolean isThumbnailSeries() {
    FormatTools.assertId(currentId, true, 1);
    return core[series].thumbnail;
  }

  /* @see IFormatReader#isInterleaved() */
  public boolean isInterleaved() {
    return isInterleaved(0);
  }

  /* @see IFormatReader#isInterleaved(int) */
  public boolean isInterleaved(int subC) {
    FormatTools.assertId(currentId, true, 1);
    return core[series].interleaved;
  }

  /* @see IFormatReader#openBytes(int) */
  public byte[] openBytes(int no) throws FormatException, IOException {
    return openBytes(no, 0, 0, getSizeX(), getSizeY());
  }

  /* @see IFormatReader#openBytes(int, byte[]) */
  public byte[] openBytes(int no, byte[] buf)
    throws FormatException, IOException
  {
    return openBytes(no, buf, 0, 0, getSizeX(), getSizeY());
  }

  /* @see IFormatReader#openBytes(int, int, int, int, int) */
  public byte[] openBytes(int no, int x, int y, int w, int h)
    throws FormatException, IOException
  {
    int bpp = FormatTools.getBytesPerPixel(getPixelType());
    int ch = getRGBChannelCount();
    byte[] newBuffer = new byte[w * h * ch * bpp];
    return openBytes(no, newBuffer, x, y, w, h);
  }

  /* @see IFormatReader#openBytes(int, byte[], int, int, int, int) */
  public abstract byte[] openBytes(int no, byte[] buf, int x, int y,
    int w, int h) throws FormatException, IOException;

  /* @see IFormatReader#openPlane(int, int, int, int, int int) */
  public Object openPlane(int no, int x, int y, int w, int h)
    throws FormatException, IOException
  {
    // NB: Readers use byte arrays by default as the native type.
    return openBytes(no, x, y, w, h);
  }

  /* @see IFormatReader#openThumbBytes(int) */
  public byte[] openThumbBytes(int no) throws FormatException, IOException {
    FormatTools.assertId(currentId, true, 1);
    return FormatTools.openThumbBytes(this, no);
  }

  /* @see IFormatReader#close(boolean) */
  public void close(boolean fileOnly) throws IOException {
    if (in != null) in.close();
    if (!fileOnly) {
      in = null;
      currentId = null;
    }
  }

  /* @see IFormatReader#getSeriesCount() */
  public int getSeriesCount() {
    FormatTools.assertId(currentId, true, 1);
    return core.length;
  }

  /* @see IFormatReader#setSeries(int) */
  public void setSeries(int no) {
    if (no < 0 || no >= getSeriesCount()) {
      throw new IllegalArgumentException("Invalid series: " + no);
    }
    series = no;
  }

  /* @see IFormatReader#getSeries() */
  public int getSeries() {
    return series;
  }

  /* @see IFormatReader#setGroupFiles(boolean) */
  public void setGroupFiles(boolean groupFiles) {
    FormatTools.assertId(currentId, false, 1);
    group = groupFiles;
  }

  /* @see IFormatReader#isGroupFiles() */
  public boolean isGroupFiles() {
    return group;
  }

  /* @see IFormatReader#fileGroupOption(String) */
  public int fileGroupOption(String id)
    throws FormatException, IOException
  {
    return FormatTools.CANNOT_GROUP;
  }

  /* @see IFormatReader#isMetadataComplete() */
  public boolean isMetadataComplete() {
    FormatTools.assertId(currentId, true, 1);
    return core[series].metadataComplete;
  }

  /* @see IFormatReader#setNormalized(boolean) */
  public void setNormalized(boolean normalize) {
    FormatTools.assertId(currentId, false, 1);
    normalizeData = normalize;
  }

  /* @see IFormatReader#isNormalized() */
  public boolean isNormalized() {
    return normalizeData;
  }

  /**
   * @deprecated
   * @see IFormatReader#setMetadataCollected(boolean)
   */
  public void setMetadataCollected(boolean collect) {
    FormatTools.assertId(currentId, false, 1);
    MetadataLevel level = collect ? MetadataLevel.ALL : MetadataLevel.MINIMUM;
    setMetadataOptions(new DefaultMetadataOptions(level));
  }

  /**
   * @deprecated
   * @see IFormatReader#isMetadataCollected()
   */
  public boolean isMetadataCollected() {
    return getMetadataOptions().getMetadataLevel() == MetadataLevel.ALL;
  }

  /* @see IFormatReader#setOriginalMetadataPopulated(boolean) */
  public void setOriginalMetadataPopulated(boolean populate) {
    FormatTools.assertId(currentId, false, 1);
    saveOriginalMetadata = populate;
  }

  /* @see IFormatReader#isOriginalMetadataPopulated() */
  public boolean isOriginalMetadataPopulated() {
    return saveOriginalMetadata;
  }

  /* @see IFormatReader#getUsedFiles() */
  public String[] getUsedFiles() {
    return getUsedFiles(false);
  }

  /* @see IFormatReader#getUsedFiles() */
  public String[] getUsedFiles(boolean noPixels) {
    int oldSeries = getSeries();
    Vector<String> files = new Vector<String>();
    for (int i=0; i<getSeriesCount(); i++) {
      setSeries(i);
      String[] s = getSeriesUsedFiles(noPixels);
      if (s != null) {
        for (String file : s) {
          if (!files.contains(file)) {
            files.add(file);
          }
        }
      }
    }
    setSeries(oldSeries);
    return files.toArray(new String[files.size()]);
  }

  /* @see IFormatReader#getSeriesUsedFiles() */
  public String[] getSeriesUsedFiles() {
    return getSeriesUsedFiles(false);
  }

  /* @see IFormatReader#getSeriesUsedFiles(boolean) */
  public String[] getSeriesUsedFiles(boolean noPixels) {
    return noPixels ? null : new String[] {currentId};
  }

  /* @see IFormatReader#getAdvancedUsedFiles(boolean) */
  public FileInfo[] getAdvancedUsedFiles(boolean noPixels) {
    String[] files = getUsedFiles(noPixels);
    if (files == null) return null;
    FileInfo[] infos = new FileInfo[files.length];
    for (int i=0; i<infos.length; i++) {
      infos[i] = new FileInfo();
      infos[i].filename = files[i];
      infos[i].reader = this.getClass();
      infos[i].usedToInitialize = files[i].endsWith(getCurrentFile());
    }
    return infos;
  }

  /* @see IFormatReader#getAdvancedSeriesUsedFiles(boolean) */
  public FileInfo[] getAdvancedSeriesUsedFiles(boolean noPixels) {
    String[] files = getSeriesUsedFiles(noPixels);
    if (files == null) return null;
    FileInfo[] infos = new FileInfo[files.length];
    for (int i=0; i<infos.length; i++) {
      infos[i] = new FileInfo();
      infos[i].filename = files[i];
      infos[i].reader = this.getClass();
      infos[i].usedToInitialize = files[i].endsWith(getCurrentFile());
    }
    return infos;
  }

  /* @see IFormatReader#getCurrentFile() */
  public String getCurrentFile() {
    return currentId;
  }

  /* @see IFormatReader#getIndex(int, int, int) */
  public int getIndex(int z, int c, int t) {
    FormatTools.assertId(currentId, true, 1);
    return FormatTools.getIndex(this, z, c, t);
  }

  /* @see IFormatReader#getZCTCoords(int) */
  public int[] getZCTCoords(int index) {
    FormatTools.assertId(currentId, true, 1);
    return FormatTools.getZCTCoords(this, index);
  }

  /* @see IFormatReader#getMetadataValue(String) */
  public Object getMetadataValue(String field) {
    FormatTools.assertId(currentId, true, 1);
    return getGlobalMeta(field);
  }

  /* @see IFormatReader#getSeriesMetadataValue(String) */
  public Object getSeriesMetadataValue(String field) {
    FormatTools.assertId(currentId, true, 1);
    return getSeriesMeta(field);
  }

  /* @see IFormatReader#getGlobalMetadata() */
  public Hashtable<String, Object> getGlobalMetadata() {
    FormatTools.assertId(currentId, true, 1);
    return metadata;
  }

  /* @see IFormatReader#getSeriesMetadata() */
  public Hashtable<String, Object> getSeriesMetadata() {
    FormatTools.assertId(currentId, true, 1);
    return core[series].seriesMetadata;
  }

  /** @deprecated */
  public Hashtable<String, Object> getMetadata() {
    FormatTools.assertId(currentId, true, 1);
    Hashtable<String, Object> h =
      new Hashtable<String, Object>(getGlobalMetadata());
    int oldSeries = getSeries();

    IMetadata meta = getMetadataStore() instanceof IMetadata ?
      (IMetadata) getMetadataStore() : null;

    for (int series=0; series<getSeriesCount(); series++) {
      String name = "Series " + series;
      if (meta != null) {
        String realName = meta.getImageName(series);
        if (realName != null && realName.trim().length() != 0) {
          name = realName;
        }
      }
      setSeries(series);
      MetadataTools.merge(getSeriesMetadata(), h, name + " ");
    }
    setSeries(oldSeries);
    return h;
  }

  /* @see IFormatReader#getCoreMetadata() */
  public CoreMetadata[] getCoreMetadata() {
    FormatTools.assertId(currentId, true, 1);
    return core;
  }

  /* @see IFormatReader#setMetadataFiltered(boolean) */
  public void setMetadataFiltered(boolean filter) {
    FormatTools.assertId(currentId, false, 1);
    filterMetadata = filter;
  }

  /* @see IFormatReader#isMetadataFiltered() */
  public boolean isMetadataFiltered() {
    return filterMetadata;
  }

  /* @see IFormatReader#setMetadataStore(MetadataStore) */
  public void setMetadataStore(MetadataStore store) {
    FormatTools.assertId(currentId, false, 1);
    if (store == null) {
      throw new IllegalArgumentException("Metadata object cannot be null; " +
        "use loci.formats.meta.DummyMetadata instead");
    }
    metadataStore = store;
  }

  /* @see IFormatReader#getMetadataStore() */
  public MetadataStore getMetadataStore() {
    return metadataStore;
  }

  /* @see IFormatReader#getMetadataStoreRoot() */
  public Object getMetadataStoreRoot() {
    FormatTools.assertId(currentId, true, 1);
    return getMetadataStore().getRoot();
  }

  /* @see IFormatReader#getUnderlyingReaders() */
  public IFormatReader[] getUnderlyingReaders() {
    return null;
  }

  /* @see IFormatReader#isSingleFile(String) */
  public boolean isSingleFile(String id) throws FormatException, IOException {
    return true;
  }

  /* @see IFormatReader#hasCompanionFiles() */
  public boolean hasCompanionFiles() {
    return hasCompanionFiles;
  }

  /* @see IFormatReader#getPossibleDomains(String) */
  public String[] getPossibleDomains(String id)
    throws FormatException, IOException
  {
    return domains;
  }

  /* @see IFormatReader#getDomains() */
  public String[] getDomains() {
    FormatTools.assertId(currentId, true, 1);
    return domains;
  }

  /* @see IFormatReader#getOptimalTileWidth() */
  public int getOptimalTileWidth() {
    FormatTools.assertId(currentId, true, 1);
    return getSizeX();
  }

  /* @see IFormatReader#getOptimalTileHeight() */
  public int getOptimalTileHeight() {
    FormatTools.assertId(currentId, true, 1);
     int bpp = FormatTools.getBytesPerPixel(getPixelType());
     int maxHeight = (1024 * 1024) / (getSizeX() * getRGBChannelCount() * bpp);
     return (int) Math.min(maxHeight, getSizeY());
  }

  // -- IFormatHandler API methods --

  /* @see IFormatHandler#isThisType(String) */
  @Override
  public boolean isThisType(String name) {
    // if necessary, open the file for further analysis
    return isThisType(name, true);
  }

  /* @see IFormatHandler#setId(String) */
  public void setId(String id) throws FormatException, IOException {
    if (!id.equals(currentId)) {
    }
  }

  /* @see IFormatHandler#close() */
  public void close() throws IOException {
    close(false);
  }

  // -- Metadata enumeration convenience methods --

  /**
   * Retrieves an {@link ome.xml.model.enums.AcquisitionMode} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected AcquisitionMode getAcquisitionMode(String value)
    throws FormatException
  {
    AcquisitionModeEnumHandler handler = new AcquisitionModeEnumHandler();
    try {
      return (AcquisitionMode) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("AcquisitionMode creation failed", e);
    }
  }

  /**
   * Retrieves an {@link ome.xml.model.enums.ArcType} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected ArcType getArcType(String value) throws FormatException {
    ArcTypeEnumHandler handler = new ArcTypeEnumHandler();
    try {
      return (ArcType) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("ArcType creation failed", e);
    }
  }

  /**
   * Retrieves an {@link ome.xml.model.enums.Binning} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected Binning getBinning(String value) throws FormatException {
    BinningEnumHandler handler = new BinningEnumHandler();
    try {
      return (Binning) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("Binning creation failed", e);
    }
  }
  /**
   * Retrieves an {@link ome.xml.model.enums.Compression} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected Compression getCompression(String value) throws FormatException {
    CompressionEnumHandler handler = new CompressionEnumHandler();
    try {
      return (Compression) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("Compression creation failed", e);
    }
  }
  /**
   * Retrieves an {@link ome.xml.model.enums.ContrastMethod} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected ContrastMethod getContrastMethod(String value)
    throws FormatException
  {
    ContrastMethodEnumHandler handler = new ContrastMethodEnumHandler();
    try {
      return (ContrastMethod) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("ContrastMethod creation failed", e);
    }
  }
  /**
   * Retrieves an {@link ome.xml.model.enums.Correction} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected Correction getCorrection(String value) throws FormatException {
    CorrectionEnumHandler handler = new CorrectionEnumHandler();
    try {
      return (Correction) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("Correction creation failed", e);
    }
  }
  /**
   * Retrieves an {@link ome.xml.model.enums.DetectorType} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected DetectorType getDetectorType(String value) throws FormatException {
    DetectorTypeEnumHandler handler = new DetectorTypeEnumHandler();
    try {
      return (DetectorType) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("DetectorType creation failed", e);
    }
  }
  /**
   * Retrieves an {@link ome.xml.model.enums.DimensionOrder} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected DimensionOrder getDimensionOrder(String value)
    throws FormatException
  {
    DimensionOrderEnumHandler handler = new DimensionOrderEnumHandler();
    try {
      return (DimensionOrder) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("DimensionOrder creation failed", e);
    }
  }
  /**
   * Retrieves an {@link ome.xml.model.enums.ExperimentType} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected ExperimentType getExperimentType(String value)
    throws FormatException
  {
    ExperimentTypeEnumHandler handler = new ExperimentTypeEnumHandler();
    try {
      return (ExperimentType) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("ExperimentType creation failed", e);
    }
  }
  /**
   * Retrieves an {@link ome.xml.model.enums.FilamentType} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected FilamentType getFilamentType(String value) throws FormatException {
    FilamentTypeEnumHandler handler = new FilamentTypeEnumHandler();
    try {
      return (FilamentType) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("FilamentType creation failed", e);
    }
  }
  /**
   * Retrieves an {@link ome.xml.model.enums.FillRule} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected FillRule getFillRule(String value) throws FormatException {
    FillRuleEnumHandler handler = new FillRuleEnumHandler();
    try {
      return (FillRule) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("FillRule creation failed", e);
    }
  }
  /**
   * Retrieves an {@link ome.xml.model.enums.FilterType} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected FilterType getFilterType(String value) throws FormatException {
    FilterTypeEnumHandler handler = new FilterTypeEnumHandler();
    try {
      return (FilterType) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("FilterType creation failed", e);
    }
  }
  /**
   * Retrieves an {@link ome.xml.model.enums.FontFamily} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected FontFamily getFontFamily(String value) throws FormatException {
    FontFamilyEnumHandler handler = new FontFamilyEnumHandler();
    try {
      return (FontFamily) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("FontFamily creation failed", e);
    }
  }
  /**
   * Retrieves an {@link ome.xml.model.enums.FontStyle} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected FontStyle getFontStyle(String value) throws FormatException {
    FontStyleEnumHandler handler = new FontStyleEnumHandler();
    try {
      return (FontStyle) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("FontStyle creation failed", e);
    }
  }
  /**
   * Retrieves an {@link ome.xml.model.enums.IlluminationType} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected IlluminationType getIlluminationType(String value)
    throws FormatException
  {
    IlluminationTypeEnumHandler handler = new IlluminationTypeEnumHandler();
    try {
      return (IlluminationType) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("IlluminationType creation failed", e);
    }
  }
  /**
   * Retrieves an {@link ome.xml.model.enums.Immersion} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected Immersion getImmersion(String value) throws FormatException {
    ImmersionEnumHandler handler = new ImmersionEnumHandler();
    try {
      return (Immersion) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("Immersion creation failed", e);
    }
  }
  /**
   * Retrieves an {@link ome.xml.model.enums.LaserMedium} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected LaserMedium getLaserMedium(String value) throws FormatException {
    LaserMediumEnumHandler handler = new LaserMediumEnumHandler();
    try {
      return (LaserMedium) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("LaserMedium creation failed", e);
    }
  }
  /**
   * Retrieves an {@link ome.xml.model.enums.LaserType} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected LaserType getLaserType(String value) throws FormatException {
    LaserTypeEnumHandler handler = new LaserTypeEnumHandler();
    try {
      return (LaserType) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("LaserType creation failed", e);
    }
  }
  /**
   * Retrieves an {@link ome.xml.model.enums.LineCap} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected LineCap getLineCap(String value) throws FormatException {
    LineCapEnumHandler handler = new LineCapEnumHandler();
    try {
      return (LineCap) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("LineCap creation failed", e);
    }
  }
  /**
   * Retrieves an {@link ome.xml.model.enums.Marker} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected Marker getMarker(String value) throws FormatException {
    MarkerEnumHandler handler = new MarkerEnumHandler();
    try {
      return (Marker) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("Marker creation failed", e);
    }
  }
  /**
   * Retrieves an {@link ome.xml.model.enums.Medium} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected Medium getMedium(String value) throws FormatException {
    MediumEnumHandler handler = new MediumEnumHandler();
    try {
      return (Medium) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("Medium creation failed", e);
    }
  }
  /**
   * Retrieves an {@link ome.xml.model.enums.MicrobeamManipulationType}
   * enumeration value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected MicrobeamManipulationType getMicrobeamManipulationType(String value)
    throws FormatException
  {
    MicrobeamManipulationTypeEnumHandler handler =
      new MicrobeamManipulationTypeEnumHandler();
    try {
      return (MicrobeamManipulationType) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("MicrobeamManipulationType creation failed", e);
    }
  }
  /**
   * Retrieves an {@link ome.xml.model.enums.MicroscopeType} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected MicroscopeType getMicroscopeType(String value)
    throws FormatException
  {
    MicroscopeTypeEnumHandler handler = new MicroscopeTypeEnumHandler();
    try {
      return (MicroscopeType) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("MicroscopeType creation failed", e);
    }
  }
  /**
   * Retrieves an {@link ome.xml.model.enums.NamingConvention} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected NamingConvention getNamingConvention(String value)
    throws FormatException
  {
    NamingConventionEnumHandler handler = new NamingConventionEnumHandler();
    try {
      return (NamingConvention) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("NamingConvention creation failed", e);
    }
  }
  /**
   * Retrieves an {@link ome.xml.model.enums.PixelType} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected PixelType getPixelType(String value) throws FormatException {
    PixelTypeEnumHandler handler = new PixelTypeEnumHandler();
    try {
      return (PixelType) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("PixelType creation failed", e);
    }
  }
  /**
   * Retrieves an {@link ome.xml.model.enums.Pulse} enumeration
   * value for the given String.
   *
   * @throws ome.xml.model.enums.EnumerationException if an appropriate
   *  enumeration value is not found.
   */
  protected Pulse getPulse(String value) throws FormatException {
    PulseEnumHandler handler = new PulseEnumHandler();
    try {
      return (Pulse) handler.getEnumeration(value);
    }
    catch (EnumerationException e) {
      throw new FormatException("Pulse creation failed", e);
    }
  }

}