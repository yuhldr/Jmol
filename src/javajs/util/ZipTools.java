/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package javajs.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javajs.J2SIgnoreImport;
import javajs.api.GenericZipInputStream;
import javajs.api.GenericZipTools;
import javajs.api.Interface;
import javajs.api.ZInputStream;

import org.apache.tools.bzip2.CBZip2InputStreamFactory;


/**
 * Note the JSmol/HTML5 must use its own version of java.util.zip.ZipOutputStream.
 * 
 */
@J2SIgnoreImport({ java.util.zip.ZipOutputStream.class })
public class ZipTools implements GenericZipTools {

  public ZipTools() {
    // for reflection
  }
  
  @Override
  public ZInputStream newZipInputStream(InputStream is) {
    return (ZInputStream) newZIS(is);
  }

  @SuppressWarnings("resource")
  private static ZipInputStream newZIS(InputStream is) {
    return (is instanceof ZipInputStream ? (ZipInputStream) is
        : is instanceof BufferedInputStream ? new GenericZipInputStream(is)
            : new GenericZipInputStream(new BufferedInputStream(is)));
  }

  @Deprecated
  @Override
  public void getAllZipData(InputStream is, String[] subfileList,
                                          String name0, String binaryFileList, String exclude,
                                          Map<String, String> fileData) {
    // Spartan only - moved to SpartanUtil.java
  }

  /**
   * iteratively drills into zip files of zip files to extract file content or
   * zip file directory. Also works with JAR files.
   * 
   * Does not return "__MACOS" paths
   * 
   * @param bis
   * @param list
   * @param listPtr
   * @param asBufferedInputStream
   *        for Pmesh
   * @return directory listing or subfile contents
   */
  @Override
  public Object getZipFileDirectory(BufferedInputStream bis, String[] list,
                                    int listPtr, boolean asBufferedInputStream) {
    SB ret;
    boolean justDir = (list == null || listPtr >= list.length);
    String fileName = (justDir ? "." : list[listPtr]);
    if (Rdr.isTar(bis))
      return getTarFileDirectory(bis, fileName, asBufferedInputStream);
    if (justDir)
      return getZipDirectoryAsStringAndClose(bis);
    bis = Rdr.getPngZipStream(bis, true);
    ZipInputStream zis = newZIS(bis);
    ZipEntry ze;
    //System.out.println("fname=" + fileName);
    try {
      boolean isAll = (fileName.equals("."));
      if (isAll || fileName.lastIndexOf("/") == fileName.length() - 1) {
        ret = new SB();
        while ((ze = zis.getNextEntry()) != null) {
          String name = ze.getName();
          if (isAll || name.startsWith(fileName))
            ret.append(name).appendC('\n');
        }
        String str = ret.toString();
        return (asBufferedInputStream ? Rdr.getBIS(str.getBytes()) : str);
      }
      int pt = fileName.indexOf(":asBinaryString");
      boolean asBinaryString = (pt > 0);
      if (asBinaryString)
        fileName = fileName.substring(0, pt);
      fileName = fileName.replace('\\', '/');
      while ((ze = zis.getNextEntry()) != null
          && !fileName.equals(ze.getName())) {
      }
      byte[] bytes = (ze == null ? null : Rdr.getLimitedStreamBytes(zis,
          ze.getSize()));
      ze = null;
      zis.close();
      if (bytes == null)
        return "";
      if (Rdr.isZipB(bytes) || Rdr.isPngZipB(bytes))
        return getZipFileDirectory(Rdr.getBIS(bytes), list, ++listPtr,
            asBufferedInputStream);
      if (asBufferedInputStream)
        return Rdr.getBIS(bytes);
      if (asBinaryString) {
        ret = new SB();
        for (int i = 0; i < bytes.length; i++)
          ret.append(Integer.toHexString(bytes[i] & 0xFF)).appendC(' ');
        return ret.toString();
      }
      if (Rdr.isGzipB(bytes))
        bytes = Rdr.getLimitedStreamBytes(getUnGzippedInputStream(bytes), -1);
      return Rdr.fixUTF(bytes);
    } catch (Exception e) {
      return "";
    }
  }

  private static Object getTarFileDirectory(BufferedInputStream bis,
                                            String fileName,
                                            boolean asBufferedInputStream) {
    SB ret;
    try {
      boolean isAll = (fileName.equals("."));
      if (isAll || fileName.lastIndexOf("/") == fileName.length() - 1) {
        ret = new SB();
        getTarContents(bis, fileName, ret);
        String str = ret.toString();
        return (asBufferedInputStream ? Rdr.getBIS(str.getBytes()) : str);
      }
      fileName = fileName.replace('\\', '/');
      byte[] bytes = getTarContents(bis, fileName, null);
      bis.close();
      return (bytes == null ? "" : asBufferedInputStream ? Rdr.getBIS(bytes) : Rdr.fixUTF(bytes));
    } catch (Exception e) {
      return "";
    }
  }

  @Override
  public byte[] getZipFileContentsAsBytes(BufferedInputStream bis,
                                          String[] list, int listPtr) {
    byte[] ret = new byte[0];
    String fileName = list[listPtr];
    if (fileName.lastIndexOf("/") == fileName.length() - 1)
      return ret;
    try {
      if (Rdr.isTar(bis))
        return getTarContents(bis, fileName, null);
      bis = Rdr.getPngZipStream(bis, true);
      ZipInputStream zis = newZIS(bis);
      ZipEntry ze;
      while ((ze = zis.getNextEntry()) != null) {
        if (!fileName.equals(ze.getName()))
          continue;
        byte[] bytes = Rdr.getLimitedStreamBytes(zis, ze.getSize());
        return ((Rdr.isZipB(bytes) || Rdr.isPngZipB(bytes)) && ++listPtr < list.length ? getZipFileContentsAsBytes(
            Rdr.getBIS(bytes), list, listPtr) : bytes);
      }
    } catch (Exception e) {
    }
    return ret;
  }
  
  private static byte[] b512;

  private static byte[] getTarContents(BufferedInputStream bis, String fileName, SB sb)
      throws IOException {
    if (b512 == null)
      b512 = new byte[512];
    int len = fileName.length();
    while (bis.read(b512, 0, 512) > 0) {
      byte[] bytes = getTarFile(bis, fileName, len, sb, null, false);
      if (bytes != null)
        return bytes;
    }
    return null;
  }

  private static byte[] getTarFile(BufferedInputStream bis, String fileName,
                                   int len, SB sb, Map<String, Object> cache,
                                   boolean oneFile)
      throws IOException {
    int j = 124;
    while (b512[j] == 48)
      j++;
    boolean isAll = (sb != null && fileName.equals("."));
    int nbytes = 0;
    while (j < 135)
      nbytes = (nbytes << 3) + (b512[j++] - 48);
    if (nbytes == 0)
      return null;
    String fname = new String(b512, 0, 100).trim();
    String prefix = new String(b512, 345, 155).trim();

    String name = prefix + fname;
    boolean found = false;
    if (sb != null) {
      if (name.length() == 0)
        return null;
      if (isAll || (oneFile ? name.equalsIgnoreCase(fileName)
          : name.startsWith(fileName))) {
        found = (cache != null);
        sb.append(name).appendC('\n');
      }
      len = -1;
    }
    int nul = (512 - (nbytes % 512)) % 512;
    if (!found && (len != name.length() || !fileName.equals(name))) {
      // skip tar entry
      int nBlocks = (nbytes + nul) >> 9;
      for (int i = nBlocks; --i >= 0;)
        bis.read(b512, 0, 512);
      return null;
    }
    byte[] bytes = Rdr.getLimitedStreamBytes(bis, nbytes);
    if (cache != null) {
      cache.put(name, new BArray(bytes));
      bis.read(b512, 0, nul);
    }
    return bytes;
  }

  @Override
  public String getZipDirectoryAsStringAndClose(BufferedInputStream bis) {
    SB sb = new SB();
    String[] s = new String[0];
    try {
      s = getZipDirectoryOrErrorAndClose(bis, null);
      bis.close();
    } catch (Exception e) {
      System.out.println(e.toString());
    }
    for (int i = 0; i < s.length; i++)
      sb.append(s[i]).appendC('\n');
    return sb.toString();
  }

  @Override
  public String[] getZipDirectoryAndClose(BufferedInputStream bis,
                                                 String manifestID) {
    String[] s = new String[0];
    try {
      s = getZipDirectoryOrErrorAndClose(bis, manifestID);
      bis.close();
    } catch (Exception e) {
      System.out.println(e.toString());
    }
    return s;
  }

  private String[] getZipDirectoryOrErrorAndClose(BufferedInputStream bis,
                                                  String manifestID)
      throws IOException {
    bis = Rdr.getPngZipStream(bis, true);
    Lst<String> v = new Lst<String>();
    ZipInputStream zis = newZIS(bis);
    ZipEntry ze;
    String manifest = null;
    while ((ze = zis.getNextEntry()) != null) {
      String fileName = ze.getName();
      if (manifestID != null && fileName.startsWith(manifestID))
        manifest = getStreamAsString(zis);
      else if (!fileName.startsWith("__MACOS")) // resource fork not nec.
        v.addLast(fileName);
    }
    zis.close();
    if (manifestID != null)
      v.add(0, manifest == null ? "" : manifest + "\n############\n");
    return v.toArray(new String[v.size()]);
  }

  public static String getStreamAsString(InputStream is) throws IOException {
    return Rdr.fixUTF(Rdr.getLimitedStreamBytes(is, -1));
  }

  @Override
  public InputStream newGZIPInputStream(InputStream is) throws IOException {
    return new BufferedInputStream(new GZIPInputStream(is, 512));
  }

  @Override
  public InputStream newBZip2InputStream(InputStream is) throws IOException {
    return new BufferedInputStream(((CBZip2InputStreamFactory) Interface.getInterface("org.apache.tools.bzip2.CBZip2InputStreamFactory")).getStream(is));
  }

  @Override
  public BufferedInputStream getUnGzippedInputStream(byte[] bytes) {
    try {
      return Rdr.getUnzippedInputStream(this, Rdr.getBIS(bytes));
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public void addZipEntry(Object zos, String fileName) throws IOException {
    ((ZipOutputStream) zos).putNextEntry(new ZipEntry(fileName));
  }

  @Override
  public void closeZipEntry(Object zos) throws IOException {
    ((ZipOutputStream) zos).closeEntry();
  }

  @Override
  public Object getZipOutputStream(Object bos) {
    /**
     * @j2sNative
     * 
     *            return javajs.api.Interface.getInterface(
     *            "java.util.zip.ZipOutputStream").setZOS(bos);
     * 
     */
    {
      return new ZipOutputStream((OutputStream) bos);
    }
  }

  @Override
  public int getCrcValue(byte[] bytes) {
    CRC32 crc = new CRC32();
    crc.update(bytes, 0, bytes.length);
    return (int) crc.getValue();
  }

  @Override
  public void readFileAsMap(BufferedInputStream bis, Map<String, Object> bdata, String name) {
    readFileAsMapStatic(bis, bdata, name);
  }

  private static void readFileAsMapStatic(BufferedInputStream bis,
      Map<String, Object> bdata, String name) {
    int pt = (name == null ? -1 : name.indexOf("|"));
    name = (pt >= 0 ? name.substring(pt + 1) : null);
    try {
      boolean isZip = false;
      if (Rdr.isPngZipStream(bis)) {
        boolean isImage = "_IMAGE_".equals(name);
        if (name == null || isImage)
          bdata.put((isImage ? "_DATA_" : "_IMAGE_"), new BArray(getPngImageBytes(bis)));
        isZip = !isImage;
      } else if (Rdr.isZipS(bis)) {
        isZip = true;
      } else if (Rdr.isTar(bis)) {
        cacheTarContentsStatic(bis, name, bdata);
      } else if (name == null){
        bdata.put("_DATA_", new BArray(Rdr.getLimitedStreamBytes(bis, -1)));
      } else {
        throw new IOException("ZIP file " + name + " not found");
      }
      if (isZip)
        cacheZipContentsStatic(bis, name, bdata, true);
      bdata.put("$_BINARY_$", Boolean.TRUE);
    } catch (IOException e) {
      bdata.clear();
      bdata.put("_ERROR_", e.getMessage());
    }
  }

  /**
   * 
   * @param bis
   * @param fileName may end with "/" for a prefix or contain "|xxxx.xxx" for a specific file or be null
   * @param cache map to store data
   * @return contents as a String
   */
  private static String cacheTarContentsStatic(BufferedInputStream bis,
      String fileName, Map<String, Object> cache) {
    SB listing = new SB();  
    long n = 0;
    if (fileName != null && fileName.endsWith("/."))
      fileName = fileName.substring(0, fileName.length() - 1);
    boolean isPath = (fileName != null && fileName.endsWith("/"));
    boolean justOne = (fileName != null && !isPath);
    try {
      if (b512 == null)
        b512 = new byte[512];
      while (bis.read(b512, 0, 512) > 0) {
        byte[] bytes = getTarFile(bis, fileName == null ? "." : fileName, -1, listing, cache, justOne);
        if (bytes != null) {
          n += bytes.length;
          if (justOne)
            break;
        }
      }
      bis.close();
    } catch (Exception e) {
      try {
        bis.close();
      } catch (IOException e1) {
      }
      return null;
    }
    if (n == 0 || fileName == null)
      return null;
    System.out.println("ZipTools cached " + n + " bytes from " + fileName);
    return listing.toString();
  }

  @Override
  public String cacheZipContents(BufferedInputStream bis,
                                        String fileName,
                                        Map<String, Object> cache, 
                                        boolean asByteArray) {
    return cacheZipContentsStatic(bis, fileName, cache, asByteArray);
  }

  /**
   * 
   * @param bis
   * @param fileName
   *        may be a case-insensitive file name or end with "/" to add a prefix
   *        or contain "|xxxx.xxx" for a specific file or be null
   * @param cache
   * @param asByteArray
   * @return contents as a String
   */
  private static String cacheZipContentsStatic(BufferedInputStream bis,
                                               String fileName,
                                               Map<String, Object> cache,
                                               boolean asByteArray) {
    ZipInputStream zis = newZIS(bis);
    ZipEntry ze;
    SB listing = new SB();
    long n = 0;
    if (fileName != null && fileName.endsWith("/."))
      fileName = fileName.substring(0, fileName.length() - 1);
    boolean isPath = (fileName != null && fileName.endsWith("/"));
    boolean oneFile = (fileName != null && !isPath && asByteArray);
    int pt = (oneFile ? fileName.indexOf("|") : -1);
    String zipEntryRoot = (pt >= 0 ? fileName : null);
    if (pt >= 0)
      fileName = fileName.substring(0, pt);
    String prefix = (fileName == null || isPath ? "" : fileName + "|");
    try {
      while ((ze = zis.getNextEntry()) != null) {
        if (ze.isDirectory())
          continue;
        String name = ze.getName();
        if (fileName != null) {
          if (oneFile) {
            if (!name.equalsIgnoreCase(fileName))
              continue;
          } else {
            if (isPath && !name.startsWith(fileName))
              continue;
            listing.append(name).appendC('\n');
          }
        }
        long nBytes = ze.getSize();
        byte[] bytes = Rdr.getLimitedStreamBytes(zis, nBytes);
        if (zipEntryRoot != null) {
          readFileAsMapStatic(Rdr.getBIS(bytes), cache, zipEntryRoot);
          return null;
        }
        n += bytes.length;
        Object o = (asByteArray ? new BArray(bytes) : bytes);
        cache.put((oneFile ? "_DATA_" : prefix + name), o);
        if (oneFile)
          break;
      }
      zis.close();
    } catch (Exception e) {
      try {
        zis.close();
      } catch (IOException e1) {
      }
      return null;
    }
    if (n == 0 || fileName == null)
      return null;
    System.out.println("ZipTools cached " + n + " bytes from " + fileName);
    return listing.toString();
  }


  private static byte[] getPngImageBytes(BufferedInputStream bis) {
    try {
      if (Rdr.isPngZipStream(bis)) {
        int pt_count[] = new int[2];
        Rdr.getPngZipPointAndCount(bis, pt_count);
        if (pt_count[1] != 0)
          return deActivatePngZipB(Rdr.getLimitedStreamBytes(bis, pt_count[0]));
      }
      return Rdr.getLimitedStreamBytes(bis, -1);
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Once a PNGJ image has been extracted, we want to red-line its
   * iTXt "Jmol Type PNGJ" tag, since it is no longer associated with
   * ZIP data.
   *  
   * @param bytes
   * @return disfigured bytes
   * 
   */
  private static byte[] deActivatePngZipB(byte[] bytes) {
    // \0PNGJ starting at byte 50 changed to \0 NGJ
    if (Rdr.isPngZipB(bytes))
      bytes[51] = 32;
    return bytes;
  }

  @Override
  public boolean isZipStream(Object br) {
    return br instanceof ZInputStream;
  }



}
