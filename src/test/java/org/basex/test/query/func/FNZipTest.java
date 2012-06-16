package org.basex.test.query.func;

import static org.basex.query.func.Function.*;
import static org.basex.util.Token.*;
import static org.junit.Assert.*;

import java.io.*;
import java.util.zip.*;

import org.basex.core.*;
import org.basex.io.*;
import org.basex.query.util.*;
import org.basex.test.query.*;
import org.junit.*;

/**
 * This class tests the functions of the zip module.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Christian Gruen
 */
public final class FNZipTest extends AdvancedQueryTest {
  /** Test ZIP file. */
  private static final String ZIP = "src/test/resources/xml.zip";
  /** Temporary ZIP file. */
  private static final String TMPZIP = Prop.TMP + NAME + ".zip";
  /** Temporary file. */
  private static final String TMPFILE = Prop.TMP + NAME + ".tmp";
  /** Test ZIP entry. */
  private static final String ENTRY1 = "infos/stopWords";
  /** Test ZIP entry. */
  private static final String ENTRY2 = "test/input.xml";

  /**
   * Initializes the test.
   * @throws IOException I/O exception.
   */
  @BeforeClass
  public static void init() throws IOException {
    // create temporary file
    new IOFile(TMPFILE).write(token("!"));
  }

  /** Finishes the test. */
  @AfterClass
  public static void finish() {
    new File(TMPZIP).delete();
    new File(TMPFILE).delete();
  }

  /**
   * Test method for the zip:binary-entry() function.
   */
  @Test
  public void zipBinaryEntry() {
    check(_ZIP_BINARY_ENTRY);
    query(_ZIP_BINARY_ENTRY.args(ZIP, ENTRY1));
    contains("xs:hexBinary(" + _ZIP_BINARY_ENTRY.args(ZIP, ENTRY1) + ")", "610A61626F");

    error(_ZIP_BINARY_ENTRY.args("abc", "xyz"), Err.ZIP_NOTFOUND);
    error(_ZIP_BINARY_ENTRY.args(ZIP, ""), Err.ZIP_NOTFOUND);
  }

  /**
   * Test method for the zip:text-entry() function.
   */
  @Test
  public void zipTextEntry() {
    check(_ZIP_TEXT_ENTRY);
    query(_ZIP_TEXT_ENTRY.args(ZIP, ENTRY1));
    query(_ZIP_TEXT_ENTRY.args(ZIP, ENTRY1, "US-ASCII"));
    error(_ZIP_TEXT_ENTRY.args(ZIP, ENTRY1, "xyz"), Err.ZIP_FAIL);
    // newlines are removed from the result..
    contains(_ZIP_TEXT_ENTRY.args(ZIP, ENTRY1), "aaboutab");
  }

  /**
   * Test method for the zip:xml-entry() function.
   */
  @Test
  public void zipXmlEntry() {
    check(_ZIP_XML_ENTRY);
    query(_ZIP_XML_ENTRY.args(ZIP, ENTRY2));
    query(_ZIP_XML_ENTRY.args(ZIP, ENTRY2) + "//title/text()", "XML");
  }

  /**
   * Test method for the zip:entries() function.
   */
  @Test
  public void zipEntries() {
    check(_ZIP_ENTRIES);
    query(_ZIP_ENTRIES.args(ZIP));
  }

  /**
   * Test method for the zip:zip-file() function.
   * @throws IOException I/O exception
   */
  @Test
  public void zipZipFile() throws IOException {
    check(_ZIP_ZIP_FILE);
    // check first file
    query(_ZIP_ZIP_FILE.args(zipParams("<entry name='one'/>")));
    checkZipEntry("one", new byte[0]);
    // check second file
    query(_ZIP_ZIP_FILE.args(zipParams("<entry name='two'>!</entry>")));
    checkZipEntry("two", new byte[] { '!' });
    // check third file
    query(_ZIP_ZIP_FILE.args(
        zipParams("<entry name='three' encoding='UTF-16'>!</entry>")));
    checkZipEntry("three", new byte[] { '\0', '!' });
    // check fourth file
    query(_ZIP_ZIP_FILE.args(zipParams("<entry name='four' src='" + TMPFILE + "'/>")));
    checkZipEntry("four", new byte[] { '!' });
    // check fifth file
    query(_ZIP_ZIP_FILE.args(zipParams("<entry src='" + TMPFILE + "'/>")));
    checkZipEntry(NAME + ".tmp", new byte[] { '!' });
    // check sixth file
    query(_ZIP_ZIP_FILE.args(zipParams("<dir name='a'><entry name='b' src='" +
        TMPFILE + "'/></dir>")));
    checkZipEntry("a/b", new byte[] { '!' });

    // error: duplicate entry specified
    error(_ZIP_ZIP_FILE.args(zipParams("<entry src='" + TMPFILE + "'/>" +
        "<entry src='" + TMPFILE + "'/>")), Err.ZIP_FAIL);
  }

  /**
   * Test method for the zip:zip-file() function and namespaces.
   * @throws IOException I/O exception
   */
  @Test
  public void zipZipFileNS() throws IOException {
    // ZIP namespace must be removed from zipped node
    query(_ZIP_ZIP_FILE.args(zipParams("<entry name='1'><a/></entry>")));
    checkZipEntry("1", token("<a/>"));
    // ZIP namespace must be removed from zipped node
    query(_ZIP_ZIP_FILE.args(zipParams("<entry name='2'><a b='c'/></entry>")));
    checkZipEntry("2", token("<a b=\"c\"/>"));
    // ZIP namespace must be removed from zipped node and its descendants
    query(_ZIP_ZIP_FILE.args(zipParams("<entry name='3'><a><b/></a></entry>")));
    checkZipEntry("3", token("<a>" + Prop.NL + "  <b/>" + Prop.NL + "</a>"));
    // ZIP namespace must be removed from zipped entry
    query(_ZIP_ZIP_FILE.args(zipParams("<entry name='4'><a xmlns=''/></entry>")));
    checkZipEntry("4", token("<a/>"));

    // ZIP namespace must be removed from zipped entry
    query(_ZIP_ZIP_FILE.args(zipParamsPrefix("5", "<a/>")));
    checkZipEntry("5", token("<a/>"));
    query(_ZIP_ZIP_FILE.args(zipParamsPrefix("6", "<a><b/></a>")));
    checkZipEntry("6", token("<a>" + Prop.NL + "  <b/>" + Prop.NL + "</a>"));
    query(_ZIP_ZIP_FILE.args(zipParamsPrefix("7", "<z:a xmlns:z='z'/>")));
    checkZipEntry("7", token("<z:a xmlns:z=\"z\"/>"));
    query(_ZIP_ZIP_FILE.args(zipParamsPrefix("8", "<zip:a xmlns:zip='z'/>")));
    checkZipEntry("8", token("<zip:a xmlns:zip=\"z\"/>"));
    query(_ZIP_ZIP_FILE.args(zipParamsPrefix("9", "<a xmlns='z'/>")));
    checkZipEntry("9", token("<a xmlns=\"z\"/>"));
  }

  /**
   * Returns a zip archive description with ZIP prefix.
   * @param name file name
   * @param entry entry
   * @return parameter string
   * @throws IOException I/O Exception
   */
  private static String zipParamsPrefix(final String name, final String entry)
      throws IOException {
    return "<zip:file xmlns:zip='http://expath.org/ns/zip' href='" +
        new File(TMPZIP).getCanonicalPath() + "'>" +
        "<zip:entry name='" + name + "'>" + entry + "</zip:entry></zip:file>";
  }

  /**
   * Test method for the zip:zip-file() function.
   * @throws IOException I/O exception
   */
  @Test
  public void zipZipFile2() throws IOException {
    check(_ZIP_ZIP_FILE);
    // check fourth file
    query(_ZIP_ZIP_FILE.args(
        zipParams("<entry name='four' src='" + TMPFILE + "'/>")));
  }

  /**
   * Test method for the zip:update-entries() function.
   */
  @Test
  public void zipUpdateEntries() {
    check(_ZIP_UPDATE_ENTRIES);
    String list = query(_ZIP_ENTRIES.args(ZIP));

    // create and compare identical zip file
    query(_ZIP_UPDATE_ENTRIES.args(list, TMPZIP));
    final String list2 = query(_ZIP_ENTRIES.args(TMPZIP));
    assertEquals(list.replaceAll(" href=\".*?\"", ""),
        list2.replaceAll(" href=\".*?\"", ""));

    // remove one directory
    list = list.replaceAll("<zip:dir name=.test.>.*</zip:dir>", "");
    query(_ZIP_UPDATE_ENTRIES.args(list, TMPZIP));
  }

  /**
   * Returns a zip archive description.
   * @param arg zip arguments
   * @return parameter string
   * @throws IOException I/O Exception
   */
  private static String zipParams(final String arg) throws IOException {
    return "<file xmlns='http://expath.org/ns/zip' href='" +
    new File(TMPZIP).getCanonicalPath() + "'>" + arg + "</file>";
  }

  /**
   * Checks the contents of the specified zip entry.
   * @param file file to be checked
   * @param data expected file contents
   * @throws IOException I/O exception
   */
  private static void checkZipEntry(final String file, final byte[] data)
      throws IOException {

    ZipFile zf = null;
    try {
      zf = new ZipFile(TMPZIP);
      final ZipEntry ze = zf.getEntry(file);
      assertNotNull("File not found: " + file, ze);
      final DataInputStream is = new DataInputStream(zf.getInputStream(ze));
      final byte[] dt = new byte[(int) ze.getSize()];
      is.readFully(dt);
      assertTrue("Wrong contents in file \"" + file + "\":" + Prop.NL +
          "Expected: " + string(data) + Prop.NL + "Found: " + string(dt),
          eq(data, dt));
    } finally {
      if(zf != null) zf.close();
    }
  }
}
