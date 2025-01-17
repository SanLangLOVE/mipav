package gov.nih.mipav.model.file;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Serializable;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;


import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gov.nih.mipav.model.file.MetadataExtractor.HuffmanTablesDirectory.HuffmanTable;
import gov.nih.mipav.model.file.MetadataExtractor.HuffmanTablesDirectory.HuffmanTable.HuffmanTableClass;
import gov.nih.mipav.model.file.MetadataExtractorTest.CanonMakernoteDescriptorTest;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Collection;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.GregorianCalendar;



import java.math.RoundingMode;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/*
 * Ported to MIPAV by William Gandler
 * Copyright 2002-2019 Drew Noakes and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 * More information about this project is available at:
 *
 *    https://drewnoakes.com/code/exif/
 *    https://github.com/drewnoakes/metadata-extractor
 */


public class MetadataExtractorTest extends MetadataExtractor {
	
	public MetadataExtractorTest() {
		
	}
	
	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class CanonMakernoteDescriptorTest
	{
		public CanonMakernoteDescriptorTest() {
			
		}
	    //@Test
		//MetadataExtractorTest me = new MetadataExtractorTest();
    	//CanonMakernoteDescriptorTest cm = me.new CanonMakernoteDescriptorTest();
    	//try {
    	//    cm.testGetFlashBiasDescription();
    	//}
    	//catch (Exception e) {
    	//	e.printStackTrace();
    	//}
		//Finished running testGetFlashBiasDescription()
	    public void testGetFlashBiasDescription() throws Exception
	    {
	        CanonMakernoteDirectory directory = new CanonMakernoteDirectory();
	        CanonMakernoteDescriptor descriptor = new CanonMakernoteDescriptor(directory);

	        // set and check values

	        directory.setInt(CanonMakernoteDirectory.FocalLength.TAG_FLASH_BIAS, 0xFFC0);
	        assertEquals("-2.0 EV", descriptor.getDescription(CanonMakernoteDirectory.FocalLength.TAG_FLASH_BIAS));

	        directory.setInt(CanonMakernoteDirectory.FocalLength.TAG_FLASH_BIAS, 0xffd4);
	        assertEquals("-1.375 EV", descriptor.getDescription(CanonMakernoteDirectory.FocalLength.TAG_FLASH_BIAS));

	        directory.setInt(CanonMakernoteDirectory.FocalLength.TAG_FLASH_BIAS, 0x0000);
	        assertEquals("0.0 EV", descriptor.getDescription(CanonMakernoteDirectory.FocalLength.TAG_FLASH_BIAS));

	        directory.setInt(CanonMakernoteDirectory.FocalLength.TAG_FLASH_BIAS, 0x000c);
	        assertEquals("0.375 EV", descriptor.getDescription(CanonMakernoteDirectory.FocalLength.TAG_FLASH_BIAS));

	        directory.setInt(CanonMakernoteDirectory.FocalLength.TAG_FLASH_BIAS, 0x0010);
	        assertEquals("0.5 EV", descriptor.getDescription(CanonMakernoteDirectory.FocalLength.TAG_FLASH_BIAS));

	        directory.setInt(CanonMakernoteDirectory.FocalLength.TAG_FLASH_BIAS, 0x0014);
	        assertEquals("0.625 EV", descriptor.getDescription(CanonMakernoteDirectory.FocalLength.TAG_FLASH_BIAS));

	        directory.setInt(CanonMakernoteDirectory.FocalLength.TAG_FLASH_BIAS, 0x0020);
	        assertEquals("1.0 EV", descriptor.getDescription(CanonMakernoteDirectory.FocalLength.TAG_FLASH_BIAS));

	        directory.setInt(CanonMakernoteDirectory.FocalLength.TAG_FLASH_BIAS, 0x0030);
	        assertEquals("1.5 EV", descriptor.getDescription(CanonMakernoteDirectory.FocalLength.TAG_FLASH_BIAS));

	        directory.setInt(CanonMakernoteDirectory.FocalLength.TAG_FLASH_BIAS, 0x0034);
	        assertEquals("1.625 EV", descriptor.getDescription(CanonMakernoteDirectory.FocalLength.TAG_FLASH_BIAS));

	        directory.setInt(CanonMakernoteDirectory.FocalLength.TAG_FLASH_BIAS, 0x0040);
	        assertEquals("2.0 EV", descriptor.getDescription(CanonMakernoteDirectory.FocalLength.TAG_FLASH_BIAS));
	        System.out.println("Finished running testGetFlashBiasDescription()");
	    }
	}
	
	/**
	 * JUnit test case for class ExifReader.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class ExifReaderTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
    	//ExifReaderTest er = me.new ExifReaderTest();
		public ExifReaderTest() {
			
		}
	    @NotNull
	    public Metadata processBytes(@NotNull String filePath) throws IOException
	    {
	        Metadata metadata = new Metadata();
	        byte[] bytes = FileUtil.readBytes(filePath);
	        new ExifReader().extract(new ByteArrayReader(bytes), metadata, ExifReader.JPEG_SEGMENT_PREAMBLE.length(), null);
	        return metadata;
	    }

	    @NotNull
	    public <T extends Directory> T processBytes(@NotNull String filePath, @NotNull Class<T> directoryClass) throws IOException
	    {
	        T directory = processBytes(filePath).getFirstDirectoryOfType(directoryClass);
	        assertNotNull(directory);
	        return directory;
	    }

	    //@SuppressWarnings("ConstantConditions")
	    //@Test
	    //try {
	    //	er.testExtractWithNullDataThrows();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testExtractWithNullDataThrows()
	    public void testExtractWithNullDataThrows() throws Exception
	    {
	        try{
	            new ExifReader().readJpegSegments(null, new Metadata(), JpegSegmentType.APP1);
	            fail("Exception expected");
	        } catch (NullPointerException npe) {
	            // passed
	        }
	        System.out.println("Finished running testExtractWithNullDataThrows()");
	    }

	    //@Test
	    //try {
	    //	er.testLoadFujifilmJpeg();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testLoadFujifilmJpeg()
	    public void testLoadFujifilmJpeg() throws Exception
	    {
	        ExifSubIFDDirectory directory = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/withExif.jpg.app1", ExifSubIFDDirectory.class);

	        final String description = directory.getDescription(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT);
	        assertNotNull(description);
	        assertEquals("80", description);
	        System.out.println("Finished running testLoadFujifilmJpeg()");
	        // TODO decide if this should still be returned -- it was being calculated upon setting of a related tag
//	      assertEquals("F9", directory.getDescription(ExifSubIFDDirectory.TAG_APERTURE));
	    }

	    //@Test
	    //try {
	    //	er.testReadJpegSegmentWithNoExifData();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testReadJpegSegmentWithNoExifData()
	    public void testReadJpegSegmentWithNoExifData() throws Exception
	    {
	        byte[] badExifData = new byte[]{ 1,2,3,4,5,6,7,8,9,10 };
	        Metadata metadata = new Metadata();
	        ArrayList<byte[]> segments = new ArrayList<byte[]>();
	        segments.add(badExifData);
	        new ExifReader().readJpegSegments(segments, metadata, JpegSegmentType.APP1);
	        assertEquals(0, metadata.getDirectoryCount());
	        assertFalse(metadata.hasErrors());
	        System.out.println("Finished running testReadJpegSegmentWithNoExifData()");
	    }

	    //@Test
	    //try {
	    //	er.testCrashRegressionTest();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running CrashRegressionTest()
	    public void testCrashRegressionTest() throws Exception
	    {
	        // This image was created via a resize in ACDSee.
	        // It seems to have a reference to an IFD starting outside the data segment.
	        // I've noticed that ACDSee reports a Comment for this image, yet ExifReader doesn't report one.
	        ExifSubIFDDirectory directory = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/crash01.jpg.app1", ExifSubIFDDirectory.class);

	        assertTrue(directory.getTagCount() > 0);
	        System.out.println("Finished running CrashRegressionTest()");
	    }

	    //@Test
	    //try {
	    //	er.testDateTime();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testDateTime()
	    public void testDateTime() throws Exception
	    {
	        ExifIFD0Directory directory = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/manuallyAddedThumbnail.jpg.app1", ExifIFD0Directory.class);

	        assertEquals("2002:11:27 18:00:35", directory.getString(ExifIFD0Directory.TAG_DATETIME));
	        System.out.println("Finished running testDateTime()");
	    }

	    //@Test
	    //try {
	    //	er.testThumbnailXResolution();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testThumbnailXResolution()
	    public void testThumbnailXResolution() throws Exception
	    {
	        ExifThumbnailDirectory directory = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/manuallyAddedThumbnail.jpg.app1", ExifThumbnailDirectory.class);

	        Rational rational = directory.getRational(ExifThumbnailDirectory.TAG_X_RESOLUTION);
	        assertNotNull(rational);
	        assertEquals(72, rational.getNumerator());
	        assertEquals(1, rational.getDenominator());
	        System.out.println("Finished running testThumbnailXResolution()");
	    }

	    //@Test
	    //try {
	    //	er.testThumbnailYResolution();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testThumbnailYResolution()
	    public void testThumbnailYResolution() throws Exception
	    {
	        ExifThumbnailDirectory directory = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/manuallyAddedThumbnail.jpg.app1", ExifThumbnailDirectory.class);

	        Rational rational = directory.getRational(ExifThumbnailDirectory.TAG_Y_RESOLUTION);
	        assertNotNull(rational);
	        assertEquals(72, rational.getNumerator());
	        assertEquals(1, rational.getDenominator());
	        System.out.println("Finished running testThumbnailYResolution()");
	    }

	    //@Test
	    //try {
	    //	er.testThumbnailOffset();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testThumbnailOffset()
	    public void testThumbnailOffset() throws Exception
	    {
	        ExifThumbnailDirectory directory = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/manuallyAddedThumbnail.jpg.app1", ExifThumbnailDirectory.class);

	        assertEquals(192, directory.getInt(ExifThumbnailDirectory.TAG_THUMBNAIL_OFFSET));
	        System.out.println("Finished running testThumbnailOffset()");
	    }

	    //@Test
	    //try {
	    //	er.testThumbnailLength();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testThumbnailLength()
	    public void testThumbnailLength() throws Exception
	    {
	        ExifThumbnailDirectory directory = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/manuallyAddedThumbnail.jpg.app1", ExifThumbnailDirectory.class);

	        assertEquals(2970, directory.getInt(ExifThumbnailDirectory.TAG_THUMBNAIL_LENGTH));
	        System.out.println("Finished running testThumbnailLength()");
	    }

	    //@Test
	    //try {
	    //	er.testCompression();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testCompression()
	    public void testCompression() throws Exception
	    {
	        ExifThumbnailDirectory directory = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/manuallyAddedThumbnail.jpg.app1", ExifThumbnailDirectory.class);

	        // 6 means JPEG compression
	        assertEquals(6, directory.getInt(ExifThumbnailDirectory.TAG_COMPRESSION));
	        System.out.println("Finished running testCompression()");
	    }

	    //@Test
	    //try {
	    //	er.testStackOverflowOnRevisitationOfSameDirectory();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testStackOverflowOnRevisitationOfSameDirectory()
	    public void testStackOverflowOnRevisitationOfSameDirectory() throws Exception
	    {
	        // An error has been discovered in Exif data segments where a directory is referenced
	        // repeatedly.  Thanks to Alistair Dickie for providing the sample data used in this
	        // unit test.

	        Metadata metadata = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/recursiveDirectories.jpg.app1");

	        // Mostly we're just happy at this point that we didn't get stuck in an infinite loop.

	        assertEquals(5, metadata.getDirectoryCount());
	        System.out.println("Finished running testStackOverflowOnRevisitationOfSameDirectory()");
	    }

	    //@Test
	    //try {
	    //	er.testDifferenceImageAndThumbnailOrientations();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testDifferenceImageAndThumbnailOrientations()
	    public void testDifferenceImageAndThumbnailOrientations() throws Exception
	    {
	        // This metadata contains different orientations for the thumbnail and the main image.
	        // These values used to be merged into a single directory, causing errors.
	        // This unit test demonstrates correct behaviour.
	        Metadata metadata = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/repeatedOrientationTagWithDifferentValues.jpg.app1");
	        ExifIFD0Directory ifd0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
	        ExifThumbnailDirectory thumbnailDirectory = metadata.getFirstDirectoryOfType(ExifThumbnailDirectory.class);

	        assertNotNull(ifd0Directory);
	        assertNotNull(thumbnailDirectory);

	        assertEquals(1, ifd0Directory.getInt(ExifIFD0Directory.TAG_ORIENTATION));
	        assertEquals(8, thumbnailDirectory.getInt(ExifThumbnailDirectory.TAG_ORIENTATION));
	        System.out.println("Finished running testDifferenceImageAndThumbnailOrientations()");
	    }

	/*
	    public void testUncompressedYCbCrThumbnail() throws Exception
	    {
	        String fileName = "withUncompressedYCbCrThumbnail.jpg";
	        String thumbnailFileName = "withUncompressedYCbCrThumbnail.bmp";
	        Metadata metadata = new ExifReader(new File(fileName)).extract();
	        ExifSubIFDDirectory directory = (ExifSubIFDDirectory)metadata.getOrCreateDirectory(ExifSubIFDDirectory.class);
	        directory.writeThumbnail(thumbnailFileName);

	        fileName = "withUncompressedYCbCrThumbnail2.jpg";
	        thumbnailFileName = "withUncompressedYCbCrThumbnail2.bmp";
	        metadata = new ExifReader(new File(fileName)).extract();
	        directory = (ExifSubIFDDirectory)metadata.getOrCreateDirectory(ExifSubIFDDirectory.class);
	        directory.writeThumbnail(thumbnailFileName);
	        fileName = "withUncompressedYCbCrThumbnail3.jpg";
	        thumbnailFileName = "withUncompressedYCbCrThumbnail3.bmp";
	        metadata = new ExifReader(new File(fileName)).extract();
	        directory = (ExifSubIFDDirectory)metadata.getOrCreateDirectory(ExifSubIFDDirectory.class);
	        directory.writeThumbnail(thumbnailFileName);
	        fileName = "withUncompressedYCbCrThumbnail4.jpg";
	        thumbnailFileName = "withUncompressedYCbCrThumbnail4.bmp";
	        metadata = new ExifReader(new File(fileName)).extract();
	        directory = (ExifSubIFDDirectory)metadata.getOrCreateDirectory(ExifSubIFDDirectory.class);
	        directory.writeThumbnail(thumbnailFileName);
	    }

	    public void testUncompressedRGBThumbnail() throws Exception
	    {
	        String fileName = "withUncompressedRGBThumbnail.jpg";
	        String thumbnailFileName = "withUncompressedRGBThumbnail.bmp";
	        Metadata metadata = new ExifReader(new File(fileName)).extract();
	        ExifSubIFDDirectory directory = (ExifSubIFDDirectory)metadata.getOrCreateDirectory(ExifSubIFDDirectory.class);
	        directory.writeThumbnail(thumbnailFileName);
	    }
	*/
	}
	
	/**
	 * Unit tests for {@link ExifSubIFDDirectory}, {@link ExifIFD0Directory}, {@link ExifThumbnailDirectory} and
	 * {@link GpsDirectory}.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("ConstantConditions")
	public class ExifDirectoryTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
    	//ExifDirectoryTest ed = me.new ExifDirectoryTest();
		public ExifDirectoryTest() {
			
		}
		//@Test
		//try {
	    //	ed.testGetDirectoryName();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running testGetDirectoryName()
	    public void testGetDirectoryName() throws Exception
	    {
	        Directory subIFDDirectory = new ExifSubIFDDirectory();
	        Directory ifd0Directory = new ExifIFD0Directory();
	        Directory thumbDirectory = new ExifThumbnailDirectory();
	        Directory gpsDirectory = new GpsDirectory();

	        assertFalse(subIFDDirectory.hasErrors());
	        assertFalse(ifd0Directory.hasErrors());
	        assertFalse(thumbDirectory.hasErrors());
	        assertFalse(gpsDirectory.hasErrors());

	        assertEquals("Exif IFD0", ifd0Directory.getName());
	        assertEquals("Exif SubIFD", subIFDDirectory.getName());
	        assertEquals("Exif Thumbnail", thumbDirectory.getName());
	        assertEquals("GPS", gpsDirectory.getName());
	        System.out.println("Finished running testGetDirectoryName()");
	    }

	    //@Test
	    //try {
	    //	ed.testDateTime();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testDateTime()
	    public void testDateTime() throws JpegProcessingException, IOException, MetadataException
	    {
	    	ExifReaderTest er = new ExifReaderTest();
	        Metadata metadata = er.processBytes("C:/metadata/metadata-extractor-master/Tests/Data/nikonMakernoteType2a.jpg.app1");

	        ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
	        ExifSubIFDDirectory exifSubIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

	        assertNotNull(exifIFD0Directory);
	        assertNotNull(exifSubIFDDirectory);

	        assertEquals("2003:10:15 10:37:08", exifIFD0Directory.getString(ExifIFD0Directory.TAG_DATETIME));
	        assertEquals("80", exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_SUBSECOND_TIME));
	        assertEquals("2003:10:15 10:37:08", exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL));
	        assertEquals("80", exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_SUBSECOND_TIME_ORIGINAL));
	        assertEquals("2003:10:15 10:37:08", exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED));
	        assertEquals("80", exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_SUBSECOND_TIME_DIGITIZED));

	        assertEquals(1066214228800L, exifIFD0Directory.getDate(
	            ExifIFD0Directory.TAG_DATETIME,
	            exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_SUBSECOND_TIME),
	            null
	        ).getTime());
	        assertEquals(1066210628800L, exifIFD0Directory.getDate(
	            ExifIFD0Directory.TAG_DATETIME,
	            exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_SUBSECOND_TIME),
	            TimeZone.getTimeZone("GMT+0100")
	        ).getTime());

	        assertEquals(1066214228800L, exifSubIFDDirectory.getDateModified().getTime());
	        assertEquals(1066210628800L, exifSubIFDDirectory.getDateModified(TimeZone.getTimeZone("GMT+0100")).getTime());
	        assertEquals(1066214228800L, exifSubIFDDirectory.getDateOriginal().getTime());
	        assertEquals(1066210628800L, exifSubIFDDirectory.getDateOriginal(TimeZone.getTimeZone("GMT+0100")).getTime());
	        assertEquals(1066214228800L, exifSubIFDDirectory.getDateDigitized().getTime());
	        assertEquals(1066210628800L, exifSubIFDDirectory.getDateDigitized(TimeZone.getTimeZone("GMT+0100")).getTime());
	        System.out.println("Finished running testDateTime()");
	    }

	    //@Test
	    //try {
	    //	ed.testResolution();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testResolution()
	    public void testResolution() throws JpegProcessingException, IOException, MetadataException
	    {
	    	ExifReaderTest er = new ExifReaderTest();
	        Metadata metadata = er.processBytes("C:/metadata/metadata-extractor-master/Tests/Data/withUncompressedRGBThumbnail.jpg.app1");

	        ExifThumbnailDirectory thumbnailDirectory = metadata.getFirstDirectoryOfType(ExifThumbnailDirectory.class);
	        assertNotNull(thumbnailDirectory);
	        assertEquals(72, thumbnailDirectory.getInt(ExifThumbnailDirectory.TAG_X_RESOLUTION));

	        ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
	        assertNotNull(exifIFD0Directory);
	        assertEquals(216, exifIFD0Directory.getInt(ExifIFD0Directory.TAG_X_RESOLUTION));
	        System.out.println("Finished running testResolution()");
	    }

	    //@Test
	    //try {
	    //	ed.testGeolocation();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGeoLocation()
	    public void testGeoLocation() throws IOException, MetadataException
	    {
	    	ExifReaderTest er = new ExifReaderTest();
	        Metadata metadata = er.processBytes("C:/metadata/metadata-extractor-master/Tests/Data/withExifAndIptc.jpg.app1.0");

	        GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
	        assertNotNull(gpsDirectory);
	        GeoLocation geoLocation = gpsDirectory.getGeoLocation();
	        assertEquals(54.989666666666665, geoLocation.getLatitude(), 0.001);
	        assertEquals(-1.9141666666666666, geoLocation.getLongitude(), 0.001);
	        System.out.println("Finished running testGeoLocation()");
	    }

	    //@Test
	    //try {
	    //	ed.testGpsDate();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGpsDate()
	    public void testGpsDate() throws IOException, MetadataException
	    {
	    	ExifReaderTest er = new ExifReaderTest();
	        Metadata metadata = er.processBytes("C:/metadata/metadata-extractor-master/Tests/Data/withPanasonicFaces.jpg.app1");

	        GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
	        assertNotNull(gpsDirectory);
	        assertEquals("2010:06:24", gpsDirectory.getString(GpsDirectory.TAG_DATE_STAMP));
	        assertEquals("10/1 17/1 21/1", gpsDirectory.getString(GpsDirectory.TAG_TIME_STAMP));
	        assertEquals(1277374641000L, gpsDirectory.getGpsDate().getTime());
	        System.out.println("Finished running testGpsDate()");
	    }
	}


	
	/**
	 * Unit tests for {@link ExifIFD0Descriptor}.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class ExifIFD0DescriptorTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
    	//ExifIFD0DescriptorTest ed = me.new ExifIFD0DescriptorTest();
		public ExifIFD0DescriptorTest() {
			
		}
	    //@Test
		//try {
	    //	ed.testXResolutionDescription();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running testXResolutionDescription()
	    public void testXResolutionDescription() throws Exception
	    {
	        ExifIFD0Directory directory = new ExifIFD0Directory();
	        directory.setRational(ExifIFD0Directory.TAG_X_RESOLUTION, new Rational(72, 1));
	        // 2 is for 'Inch'
	        directory.setInt(ExifIFD0Directory.TAG_RESOLUTION_UNIT, 2);
	        ExifIFD0Descriptor descriptor = new ExifIFD0Descriptor(directory);
	        assertEquals("72 dots per inch", descriptor.getDescription(ExifIFD0Directory.TAG_X_RESOLUTION));
	        System.out.println("Finished running testXResolutionDescription()");
	    }

	    //@Test
	    //try {
	    //	ed.testYResolutionDescription();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testYResolutionDescription()
	    public void testYResolutionDescription() throws Exception
	    {
	        ExifIFD0Directory directory = new ExifIFD0Directory();
	        directory.setRational(ExifIFD0Directory.TAG_Y_RESOLUTION, new Rational(50, 1));
	        // 3 is for 'cm'
	        directory.setInt(ExifIFD0Directory.TAG_RESOLUTION_UNIT, 3);
	        ExifIFD0Descriptor descriptor = new ExifIFD0Descriptor(directory);
	        assertEquals("50 dots per cm", descriptor.getDescription(ExifIFD0Directory.TAG_Y_RESOLUTION));
	        System.out.println("Finished running testYResolutionDescription()");
	    }

	    //@Test
	    //try {
	    //	ed.testWindowsXpFields();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testWindowsXpFields()
	    public void testWindowsXpFields() throws Exception
	    {
	    	ExifReaderTest er = new ExifReaderTest();
	        ExifIFD0Directory directory = er.processBytes("C:/metadata/metadata-extractor-master/Tests/Data/windowsXpFields.jpg.app1", ExifIFD0Directory.class);

	        assertEquals("Testing artist\0", directory.getString(ExifIFD0Directory.TAG_WIN_AUTHOR, "UTF-16LE"));
	        assertEquals("Testing comments\0", directory.getString(ExifIFD0Directory.TAG_WIN_COMMENT, "UTF-16LE"));
	        assertEquals("Testing keywords\0", directory.getString(ExifIFD0Directory.TAG_WIN_KEYWORDS, "UTF-16LE"));
	        assertEquals("Testing subject\0", directory.getString(ExifIFD0Directory.TAG_WIN_SUBJECT, "UTF-16LE"));
	        assertEquals("Testing title\0", directory.getString(ExifIFD0Directory.TAG_WIN_TITLE, "UTF-16LE"));

	        ExifIFD0Descriptor descriptor = new ExifIFD0Descriptor(directory);
	        assertEquals("Testing artist", descriptor.getDescription(ExifIFD0Directory.TAG_WIN_AUTHOR));
	        assertEquals("Testing comments", descriptor.getDescription(ExifIFD0Directory.TAG_WIN_COMMENT));
	        assertEquals("Testing keywords", descriptor.getDescription(ExifIFD0Directory.TAG_WIN_KEYWORDS));
	        assertEquals("Testing subject", descriptor.getDescription(ExifIFD0Directory.TAG_WIN_SUBJECT));
	        assertEquals("Testing title", descriptor.getDescription(ExifIFD0Directory.TAG_WIN_TITLE));
	        System.out.println("Finished running testWindowsXpFields()");
	    }
	}

	/**
	 * Unit tests for {@link ExifInteropDescriptor}.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class ExifInteropDescriptorTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
    	//ExifInteropDescriptorTest ei = me.new ExifInteropDescriptorTest();
		public ExifInteropDescriptorTest() {
			
		}
	    //@Test
		//try {
	    //	ei.testGetInteropVersionDescription();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running testGetInteropVersionDescription()
	    public void testGetInteropVersionDescription() throws Exception
	    {
	        ExifInteropDirectory directory = new ExifInteropDirectory();
	        directory.setIntArray(ExifInteropDirectory.TAG_INTEROP_VERSION, new int[]{0, 1, 0, 0});
	        ExifInteropDescriptor descriptor = new ExifInteropDescriptor(directory);
	        assertEquals("1.00", descriptor.getDescription(ExifInteropDirectory.TAG_INTEROP_VERSION));
	        assertEquals("1.00", descriptor.getInteropVersionDescription());
	        System.out.println("Finished running testGetInteropVersionDescription()");
	    }

	    //@Test
	    //try {
	    //	ei.testGetInteropIndexDescription();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetIndexVersionDescription()
	    public void testGetInteropIndexDescription() throws Exception
	    {
	        ExifInteropDirectory directory = new ExifInteropDirectory();
	        directory.setString(ExifInteropDirectory.TAG_INTEROP_INDEX, "R98");
	        ExifInteropDescriptor descriptor = new ExifInteropDescriptor(directory);
	        assertEquals("Recommended Exif Interoperability Rules (ExifR98)", descriptor.getDescription(ExifInteropDirectory.TAG_INTEROP_INDEX));
	        assertEquals("Recommended Exif Interoperability Rules (ExifR98)", descriptor.getInteropIndexDescription());
	        System.out.println("Finished running testGetIndexVersionDescription()");
	    }
	}
	
	/**
	 * JUnit test case for class ExifSubIFDDescriptor.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class ExifSubIFDDescriptorTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
		//ExifSubIFDDescriptorTest ei = me.new ExifSubIFDDescriptorTest();
	    //@Test
		//try {
	    //	ei.testUserCommentDescription_EmptyEncoding();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running testUserCommentDescription_EmptyEncoding()
	    public void testUserCommentDescription_EmptyEncoding() throws Exception
	    {
	        byte[] commentBytes = "\0\0\0\0\0\0\0\0This is a comment".getBytes();
	        ExifSubIFDDirectory directory = new ExifSubIFDDirectory();
	        directory.setByteArray(ExifSubIFDDirectory.TAG_USER_COMMENT, commentBytes);
	        ExifSubIFDDescriptor descriptor = new ExifSubIFDDescriptor(directory);
	        assertEquals("This is a comment", descriptor.getDescription(ExifSubIFDDirectory.TAG_USER_COMMENT));
	        System.out.println("Finished running testUserCommentDescription_EmptyEncoding()");
	    }

	    //@Test
	    //try {
	    //	ei.testUserCommentDescription_AsciiHeaderAsciiEncoding();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testUserCommentDescription_AsciiHeaderAsciiEncoding()
	    public void testUserCommentDescription_AsciiHeaderAsciiEncoding() throws Exception
	    {
	        byte[] commentBytes = "ASCII\0\0This is a comment".getBytes();
	        ExifSubIFDDirectory directory = new ExifSubIFDDirectory();
	        directory.setByteArray(ExifSubIFDDirectory.TAG_USER_COMMENT, commentBytes);
	        ExifSubIFDDescriptor descriptor = new ExifSubIFDDescriptor(directory);
	        assertEquals("This is a comment", descriptor.getDescription(ExifSubIFDDirectory.TAG_USER_COMMENT));
	        System.out.println("Finished running testUserCommentDescription_AsciiHeaderAsciiEncoding()");
	    }

	    //@Test
	    //try {
	    //	ei.testUserCommentDescription_BlankAscii();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testUserCommentDescription_BlankAscii()
	    public void testUserCommentDescription_BlankAscii() throws Exception
	    {
	        byte[] commentBytes = "ASCII\0\0\0          ".getBytes();
	        ExifSubIFDDirectory directory = new ExifSubIFDDirectory();
	        directory.setByteArray(ExifSubIFDDirectory.TAG_USER_COMMENT, commentBytes);
	        ExifSubIFDDescriptor descriptor = new ExifSubIFDDescriptor(directory);
	        assertEquals("", descriptor.getDescription(ExifSubIFDDirectory.TAG_USER_COMMENT));
	        System.out.println("Finished running testUserCommentDescription_BlankAscii()");
	    }

	    //@Test
	    //try {
	    //	ei.testUserCommentDescription_ZeroLengthAscii1();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testUserCommentDescription_ZeroLengthAscii1()
	    public void testUserCommentDescription_ZeroLengthAscii1() throws Exception
	    {
	        // the 10-byte encoding region is only partially full
	        byte[] commentBytes = "ASCII\0\0\0".getBytes();
	        ExifSubIFDDirectory directory = new ExifSubIFDDirectory();
	        directory.setByteArray(ExifSubIFDDirectory.TAG_USER_COMMENT, commentBytes);
	        ExifSubIFDDescriptor descriptor = new ExifSubIFDDescriptor(directory);
	        assertEquals("ASCII", descriptor.getDescription(ExifSubIFDDirectory.TAG_USER_COMMENT));
	        System.out.println("Finished running testUserCommentDescription_ZeroLengthAscii1()");
	    }

	    //@Test
	    //try {
	    //	ei.testUserCommentDescription_ZeroLengthAscii2();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testUserCommentDescription_ZeroLengthAscii2()
	    public void testUserCommentDescription_ZeroLengthAscii2() throws Exception
	    {
	        // fill the 10-byte encoding region
	        byte[] commentBytes = "ASCII\0\0\0\0\0".getBytes();
	        ExifSubIFDDirectory directory = new ExifSubIFDDirectory();
	        directory.setByteArray(ExifSubIFDDirectory.TAG_USER_COMMENT, commentBytes);
	        ExifSubIFDDescriptor descriptor = new ExifSubIFDDescriptor(directory);
	        assertEquals("", descriptor.getDescription(ExifSubIFDDirectory.TAG_USER_COMMENT));
	        System.out.println("Finished running testUserCommentDescription_ZeroLengthAscii2()");
	    }

	    //@Test
	    //try {
	    //	ei.testUnicodeComment_ActualBytes();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testUnicodeComment_ActualBytes()
	    public void testUnicodeComment_ActualBytes() throws Exception
	    {
	        byte[] commentBytes = new byte[] { 85, 78, 73, 67, 79, 68, 69, 0, 84, 0, 104, 0, 105, 0, 115, 0, 32, 0, 109, 0, 97, 0, 114, 0, 109, 0, 111, 0, 116, 0, 32, 0, 105, 0, 115, 0, 32, 0, 103, 0, 101, 0, 116, 0, 116, 0, 105, 0, 110, 0, 103, 0, 32, 0, 99, 0, 108, 0, 111, 0, 115, 0, 101, 0, 46, 0, 46, 0, 46, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0, 32, 0 };
	        ExifSubIFDDirectory directory = new ExifSubIFDDirectory();
	        directory.setByteArray(ExifSubIFDDirectory.TAG_USER_COMMENT, commentBytes);
	        ExifSubIFDDescriptor descriptor = new ExifSubIFDDescriptor(directory);
	        assertEquals("This marmot is getting close...", descriptor.getDescription(ExifSubIFDDirectory.TAG_USER_COMMENT));
	        System.out.println("Finished running testUnicodeComment_ActualBytes()");
	    }

	    //@Test
	    //try {
	    //	ei.testUnicodeComment_Ascii();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testUnicodeComment_Ascii()
	    public void testUnicodeComment_Ascii() throws Exception
	    {
	        byte[] commentBytes = new byte[] { 65, 83, 67, 73, 73, 0, 0, 0, 73, 32, 97, 109, 32, 97, 32, 99, 111, 109, 109, 101, 110, 116, 46, 32, 89, 101, 121, 46, 0 };
	        ExifSubIFDDirectory directory = new ExifSubIFDDirectory();
	        directory.setByteArray(ExifSubIFDDirectory.TAG_USER_COMMENT, commentBytes);
	        ExifSubIFDDescriptor descriptor = new ExifSubIFDDescriptor(directory);
	        assertEquals("I am a comment. Yey.", descriptor.getDescription(ExifSubIFDDirectory.TAG_USER_COMMENT));
	        System.out.println("Finished running testUnicodeComment_Ascii()");
	    }
	}

	/**
	 * JUnit test case for class ExifThumbnailDescriptor.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class ExifThumbnailDescriptorTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //ExifThumbnailDescriptorTest ed = me.new ExifThumbnailDescriptorTest();
	    //@Test
		//try {
	    //	ed.testGetYCbCrSubsamplingDescription();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running testGetYCbCrSubsamplingDescription()
	    public void testGetYCbCrSubsamplingDescription() throws Exception
	    {
	        ExifThumbnailDirectory directory = new ExifThumbnailDirectory();
	        directory.setIntArray(ExifThumbnailDirectory.TAG_YCBCR_SUBSAMPLING, new int[]{2, 1});

	        ExifThumbnailDescriptor descriptor = new ExifThumbnailDescriptor(directory);
	        assertEquals("YCbCr4:2:2", descriptor.getDescription(ExifThumbnailDirectory.TAG_YCBCR_SUBSAMPLING));
	        assertEquals("YCbCr4:2:2", descriptor.getYCbCrSubsamplingDescription());

	        directory.setIntArray(ExifThumbnailDirectory.TAG_YCBCR_SUBSAMPLING, new int[]{2, 2});

	        assertEquals("YCbCr4:2:0", descriptor.getDescription(ExifThumbnailDirectory.TAG_YCBCR_SUBSAMPLING));
	        assertEquals("YCbCr4:2:0", descriptor.getYCbCrSubsamplingDescription());
	        System.out.println("Finished running testGetYCbCrSubsamplingDescription()");
	    }
	}
	
	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class NikonType1MakernoteTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
		// NikonType1MakernoteTest nm = me.new NikonType1MakernoteTest();
		// try {
	    //    nm.setUp();
	    // }
		// catch(Exception e) {
		//    e.printStackTrace();
		//}
	    private NikonType1MakernoteDirectory _nikonDirectory;
	    private ExifIFD0Directory _exifIFD0Directory;
	    private ExifSubIFDDirectory _exifSubIFDDirectory;
	    private ExifThumbnailDirectory _thumbDirectory;
	    public NikonType1MakernoteTest() {
	  
	    }

	    /*
	        [Interoperability] Interoperability Index = Recommended Exif Interoperability Rules (ExifR98)
	        [Interoperability] Interoperability Version = 1.00
	        [Jpeg] Data Precision = 8 bits
	        [Jpeg] Image Width = 600 pixels
	        [Jpeg] Image Height = 800 pixels
	        [Jpeg] Number of Components = 3
	        [Jpeg] Component 1 = Y component: Quantization table 0, Sampling factors 1 horiz/1 vert
	        [Jpeg] Component 2 = Cb component: Quantization table 1, Sampling factors 1 horiz/1 vert
	        [Jpeg] Component 3 = Cr component: Quantization table 1, Sampling factors 1 horiz/1 vert
	    */

	    //@Before
	    public void setUp() throws Exception
	    {
	    	ExifReaderTest er = new ExifReaderTest();
	        Metadata metadata = er.processBytes("C:/metadata/metadata-extractor-master/Tests/Data/nikonMakernoteType1.jpg.app1");



	        _nikonDirectory = metadata.getFirstDirectoryOfType(NikonType1MakernoteDirectory.class);
	        _exifSubIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
	        _exifIFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
	        _thumbDirectory = metadata.getFirstDirectoryOfType(ExifThumbnailDirectory.class);

	    }

	    /*
	        [Nikon Makernote] Makernote Unknown 1 = 08.00
	        [Nikon Makernote] Quality = Unknown (12)
	        [Nikon Makernote] Color Mode = Color
	        [Nikon Makernote] Image Adjustment = Contrast +
	        [Nikon Makernote] CCD Sensitivity = ISO80
	        [Nikon Makernote] White Balance = Auto
	        [Nikon Makernote] Focus = 0
	        [Nikon Makernote] Makernote Unknown 2 =
	        [Nikon Makernote] Digital Zoom = No digital zoom
	        [Nikon Makernote] Fisheye Converter = None
	        [Nikon Makernote] Makernote Unknown 3 = 0 0 16777216 0 2685774096 0 34833 6931 16178 4372 4372 3322676767 3373084416 15112 0 0 1151495 252903424 17 0 0 844038208 55184128 218129428 1476410198 370540566 4044363286 16711749 204629079 1729
	    */
	    
	    //@Test
	    //try {
	    //	nm.testNikonMakernote_MatchesKnownValues();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testNikonMakernote_MatchesKnownValues()
	    public void testNikonMakernote_MatchesKnownValues() throws Exception
	    {
	        assertTrue(_nikonDirectory.getTagCount() > 0);
	        assertEquals(8, _nikonDirectory.getDouble(NikonType1MakernoteDirectory.TAG_UNKNOWN_1), 0.0001);
	        assertEquals(12, _nikonDirectory.getInt(NikonType1MakernoteDirectory.TAG_QUALITY));
	        assertEquals(1, _nikonDirectory.getInt(NikonType1MakernoteDirectory.TAG_COLOR_MODE));
	        assertEquals(3, _nikonDirectory.getInt(NikonType1MakernoteDirectory.TAG_IMAGE_ADJUSTMENT));
	        assertEquals(0, _nikonDirectory.getInt(NikonType1MakernoteDirectory.TAG_CCD_SENSITIVITY));
	        assertEquals(0, _nikonDirectory.getInt(NikonType1MakernoteDirectory.TAG_WHITE_BALANCE));
	        assertEquals(0, _nikonDirectory.getInt(NikonType1MakernoteDirectory.TAG_FOCUS));
	        assertEquals("", _nikonDirectory.getString(NikonType1MakernoteDirectory.TAG_UNKNOWN_2));
	        assertEquals(0, _nikonDirectory.getDouble(NikonType1MakernoteDirectory.TAG_DIGITAL_ZOOM), 0.0001);
	        assertEquals(0, _nikonDirectory.getInt(NikonType1MakernoteDirectory.TAG_CONVERTER));
	        long[] unknown3 = (long[])_nikonDirectory.getObject(NikonType1MakernoteDirectory.TAG_UNKNOWN_3);
	        long[] expected = new long[] { 0, 0, 16777216, 0, 2685774096L, 0, 34833, 6931, 16178, 4372, 4372, 3322676767L, 3373084416L, 15112, 0, 0, 1151495, 252903424, 17, 0, 0, 844038208, 55184128, 218129428, 1476410198, 370540566, 4044363286L, 16711749, 204629079, 1729 };
	        assertNotNull(unknown3);
	        assertEquals(expected.length, unknown3.length);
	        for (int i = 0; i<expected.length; i++) {
	            assertEquals(expected[i], unknown3[i]);
	        }
	        System.out.println("Finished running testNikonMakernote_MatchesKnownValues()");
	    }

	    /*
	        [Exif] Image Description =
	        [Exif] Make = NIKON
	        [Exif] Model = E950
	        [Exif] Orientation = top, left side
	        [Exif] X Resolution = 300 dots per inch
	        [Exif] Y Resolution = 300 dots per inch
	        [Exif] Resolution Unit = Inch
	        [Exif] Software = v981-79
	        [Exif] Date/Time = 2001:04:06 11:51:40
	        [Exif] YCbCr Positioning = Datum point
	        [Exif] Exposure Time = 1/77 sec
	        [Exif] F-Number = F5.5
	        [Exif] Exposure Program = Program normal
	        [Exif] ISO Speed Ratings = 80
	        [Exif] Exif Version = 2.10
	        [Exif] Date/Time Original = 2001:04:06 11:51:40
	        [Exif] Date/Time Digitized = 2001:04:06 11:51:40
	        [Exif] Components Configuration = YCbCr
	        [Exif] Compressed Bits Per Pixel = 4 bits/pixel
	        [Exif] Exposure Bias Value = 0
	        [Exif] Max Aperture Value = F2.5
	        [Exif] Metering Mode = Multi-segment
	        [Exif] Light Source = Unknown
	        [Exif] Flash = No flash fired
	        [Exif] Focal Length = 12.8 mm
	        [Exif] User Comment =
	        [Exif] FlashPix Version = 1.00
	        [Exif] Color Space = sRGB
	        [Exif] Exif Image Width = 1600 pixels
	        [Exif] Exif Image Height = 1200 pixels
	        [Exif] File Source = Digital Still Camera (DSC)
	        [Exif] Scene Type = Directly photographed image
	        [Exif] Compression = JPEG compression
	        [Exif] Thumbnail Offset = 2036 bytes
	        [Exif] Thumbnail Length = 4662 bytes
	        [Exif] Thumbnail Data = [4662 bytes of thumbnail data]
	    */
	    //@Test
	    //try {
	    //	nm.testExifDirectory_MatchesKnownValues();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testExifDirectory_MatchesKnownValues()
	    public void testExifDirectory_MatchesKnownValues() throws Exception
	    {
	        assertEquals("          ", _exifIFD0Directory.getString(ExifIFD0Directory.TAG_IMAGE_DESCRIPTION));
	        assertEquals("NIKON", _exifIFD0Directory.getString(ExifIFD0Directory.TAG_MAKE));
	        assertEquals("E950", _exifIFD0Directory.getString(ExifIFD0Directory.TAG_MODEL));
	        assertEquals(1, _exifIFD0Directory.getInt(ExifIFD0Directory.TAG_ORIENTATION));
	        assertEquals(300, _exifIFD0Directory.getDouble(ExifIFD0Directory.TAG_X_RESOLUTION), 0.001);
	        assertEquals(300, _exifIFD0Directory.getDouble(ExifIFD0Directory.TAG_Y_RESOLUTION), 0.001);
	        assertEquals(2, _exifIFD0Directory.getInt(ExifIFD0Directory.TAG_RESOLUTION_UNIT));
	        assertEquals("v981-79", _exifIFD0Directory.getString(ExifIFD0Directory.TAG_SOFTWARE));
	        assertEquals("2001:04:06 11:51:40", _exifIFD0Directory.getString(ExifIFD0Directory.TAG_DATETIME));
	        assertEquals(2, _exifIFD0Directory.getInt(ExifIFD0Directory.TAG_YCBCR_POSITIONING));

	        assertEquals(new Rational(1, 77), _exifSubIFDDirectory.getRational(ExifSubIFDDirectory.TAG_EXPOSURE_TIME));
	        assertEquals(5.5, _exifSubIFDDirectory.getDouble(ExifSubIFDDirectory.TAG_FNUMBER), 0.001);
	        assertEquals(2, _exifSubIFDDirectory.getInt(ExifSubIFDDirectory.TAG_EXPOSURE_PROGRAM));
	        assertEquals(80, _exifSubIFDDirectory.getInt(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT));
	        assertEquals("48 50 49 48", _exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_EXIF_VERSION));
	        assertEquals("2001:04:06 11:51:40", _exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED));
	        assertEquals("2001:04:06 11:51:40", _exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL));
	        assertEquals("1 2 3 0", _exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_COMPONENTS_CONFIGURATION));
	        assertEquals(4, _exifSubIFDDirectory.getInt(ExifSubIFDDirectory.TAG_COMPRESSED_AVERAGE_BITS_PER_PIXEL));
	        assertEquals(0, _exifSubIFDDirectory.getInt(ExifSubIFDDirectory.TAG_EXPOSURE_BIAS));
	        // this 2.6 *apex*, which is F2.5
	        assertEquals(2.6, _exifSubIFDDirectory.getDouble(ExifSubIFDDirectory.TAG_MAX_APERTURE), 0.001);
	        assertEquals(5, _exifSubIFDDirectory.getInt(ExifSubIFDDirectory.TAG_METERING_MODE));
	        assertEquals(0, _exifSubIFDDirectory.getInt(ExifSubIFDDirectory.TAG_WHITE_BALANCE));
	        assertEquals(0, _exifSubIFDDirectory.getInt(ExifSubIFDDirectory.TAG_FLASH));
	        assertEquals(12.8, _exifSubIFDDirectory.getDouble(ExifSubIFDDirectory.TAG_FOCAL_LENGTH), 0.001);
	        assertEquals("0 0 0 0 0 0 0 0 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32", _exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_USER_COMMENT));
	        assertEquals("48 49 48 48", _exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_FLASHPIX_VERSION));
	        assertEquals(1, _exifSubIFDDirectory.getInt(ExifSubIFDDirectory.TAG_COLOR_SPACE));
	        assertEquals(1600, _exifSubIFDDirectory.getInt(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH));
	        assertEquals(1200, _exifSubIFDDirectory.getInt(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT));
	        assertEquals(3, _exifSubIFDDirectory.getInt(ExifSubIFDDirectory.TAG_FILE_SOURCE));
	        assertEquals(1, _exifSubIFDDirectory.getInt(ExifSubIFDDirectory.TAG_SCENE_TYPE));

	        assertEquals(6, _thumbDirectory.getInt(ExifThumbnailDirectory.TAG_COMPRESSION));
	        assertEquals(2036, _thumbDirectory.getInt(ExifThumbnailDirectory.TAG_THUMBNAIL_OFFSET));
	        assertEquals(4662, _thumbDirectory.getInt(ExifThumbnailDirectory.TAG_THUMBNAIL_LENGTH));
	        System.out.println("Finished running testExifDirectory_MatchesKnownValues()");
	    }
	}

	public class NikonType2MakernoteTest1
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
		// NikonType2MakernoteTest1 nm = me.new NikonType2MakernoteTest1();
		// try {
	    //    nm.setUp();
	    // }
		// catch(Exception e) {
		//    e.printStackTrace();
		//}
	    private NikonType2MakernoteDirectory _nikonDirectory;
	    private NikonType2MakernoteDescriptor _descriptor;
	    public NikonType2MakernoteTest1() {
	    	
	    }

	    //@Before
	    public void setUp() throws Exception
	    {
	        Locale.setDefault(new Locale("en", "GB"));

	        ExifReaderTest er = new ExifReaderTest();
	        _nikonDirectory = er.processBytes("C:/metadata/metadata-extractor-master/Tests/Data/nikonMakernoteType2a.jpg.app1", NikonType2MakernoteDirectory.class);

	        assertNotNull(_nikonDirectory);

	        _descriptor = new NikonType2MakernoteDescriptor(_nikonDirectory);
	    }

	    /*
	        [Nikon Makernote] Firmware Version = 0200
	        [Nikon Makernote] ISO = 0 320
	        [Nikon Makernote] File Format = FINE
	        [Nikon Makernote] White Balance = FLASH
	        [Nikon Makernote] Sharpening = AUTO
	        [Nikon Makernote] AF Type = AF-C
	        [Nikon Makernote] Unknown 17 = NORMAL
	        [Nikon Makernote] Unknown 18 =
	        [Nikon Makernote] White Balance Fine = 0
	        [Nikon Makernote] Unknown 01 =
	        [Nikon Makernote] Unknown 02 =
	        [Nikon Makernote] Unknown 03 = 914
	        [Nikon Makernote] Unknown 19 =
	        [Nikon Makernote] ISO = 0 320
	        [Nikon Makernote] Tone Compensation = AUTO
	        [Nikon Makernote] Unknown 04 = 6
	        [Nikon Makernote] Lens Focal/Max-FStop pairs = 240/10 850/10 35/10 45/10
	        [Nikon Makernote] Unknown 05 = 0
	        [Nikon Makernote] Unknown 06 = 
	        [Nikon Makernote] Unknown 07 = 1
	        [Nikon Makernote] Unknown 20 = 0
	        [Nikon Makernote] Unknown 08 = @
	        [Nikon Makernote] Colour Mode = MODE1
	        [Nikon Makernote] Unknown 10 = NATURAL
	        [Nikon Makernote] Unknown 11 = 0100
	        
	

	        


	        
	-
	        [Nikon Makernote] Camera Hue = 0
	        [Nikon Makernote] Noise Reduction = OFF
	        [Nikon Makernote] Unknown 12 = 0100

	        [Nikon Makernote] Unknown 13 = 0100{t@7b,4x,D"Y
	        [Nikon Makernote] Unknown 15 = 78/10 78/10
	    */
	    //@Test
	    //try {
	    //	nm.testNikonMakernote_MatchesKnownValues();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testNikonMakernote_MatchesKnownValues()
	    public void testNikonMakernote_MatchesKnownValues() throws Exception
	    {
	        assertEquals("48 50 48 48", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_FIRMWARE_VERSION));
	        assertEquals("0 320", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_ISO_1));
	        assertEquals("0 320", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_ISO_REQUESTED));
	        assertEquals("FLASH       ", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_CAMERA_WHITE_BALANCE));
	        assertEquals("AUTO  ", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_CAMERA_SHARPENING));
	        assertEquals("AF-C  ", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_AF_TYPE));
	        assertEquals("NORMAL      ", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_FLASH_SYNC_MODE));
	        assertEquals("0", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_CAMERA_WHITE_BALANCE_FINE));
	        assertEquals("914", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_PREVIEW_IFD));
	        assertEquals("AUTO    ", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_CAMERA_TONE_COMPENSATION));
	        assertEquals("6", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_LENS_TYPE));
	        assertEquals("240/10 850/10 35/10 45/10", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_LENS));
	        assertEquals("0", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_FLASH_USED));
	        assertEquals("1", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_SHOOTING_MODE));
	        assertEquals("0", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_UNKNOWN_20));
	        assertEquals("MODE1   ", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_CAMERA_COLOR_MODE));
	        assertEquals("NATURAL    ", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_LIGHT_SOURCE));
	        assertEquals("0", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_CAMERA_HUE_ADJUSTMENT));
	        assertEquals("OFF ", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_NOISE_REDUCTION));
	        assertEquals("78/10 78/10", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_SENSOR_PIXEL_SIZE));
	        System.out.println("Finished running testNikonMakernote_MatchesKnownValues()");
	    }

	    //@Test
	    //try {
	    //	nm.testGetLensDescription();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetLensDescription()
	    public void testGetLensDescription() throws MetadataException
	    {
	        assertEquals("24-85mm f/3.5-4.5", _descriptor.getDescription(NikonType2MakernoteDirectory.TAG_LENS));
	        assertEquals("24-85mm f/3.5-4.5", _descriptor.getLensDescription());
	        System.out.println("Finished running testGetLensDescription()");
	    }

	   // @Test
	    //try {
	    //	nm.testGetHueAdjustmentDescription();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetHueAdjustmentDescription()
	    public void testGetHueAdjustmentDescription() throws MetadataException
	    {
	        assertEquals("0 degrees", _descriptor.getDescription(NikonType2MakernoteDirectory.TAG_CAMERA_HUE_ADJUSTMENT));
	        assertEquals("0 degrees", _descriptor.getHueAdjustmentDescription());
	        System.out.println("Finished running testGetHueAdjustmentDescription()");
	    }

	    //@Test
	    //try {
	    //	nm.testGetColorModeDescription();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetColorModeDescription()
	    public void testGetColorModeDescription() throws Exception
	    {
	        assertEquals("Mode I (sRGB)", _descriptor.getDescription(NikonType2MakernoteDirectory.TAG_CAMERA_COLOR_MODE));
	        assertEquals("Mode I (sRGB)", _descriptor.getColorModeDescription());
	        System.out.println("Finished running testGetColorModeDescription()");
	    }

	    //@Test
	    //try {
	    //	nm.testGetAutoFlashCompensationDescription();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetAutoFlashCompensationDescription()
	    public void testGetAutoFlashCompensationDescription() throws Exception
	    {
	        NikonType2MakernoteDirectory directory = new NikonType2MakernoteDirectory();
	        NikonType2MakernoteDescriptor descriptor = new NikonType2MakernoteDescriptor(directory);

	        // no entry exists
	        assertNull(descriptor.getAutoFlashCompensationDescription());

	        directory.setByteArray(NikonType2MakernoteDirectory.TAG_AUTO_FLASH_COMPENSATION, new byte[] { 0x06, 0x01, 0x06 });
	        assertEquals("1 EV", descriptor.getAutoFlashCompensationDescription());

	        directory.setByteArray(NikonType2MakernoteDirectory.TAG_AUTO_FLASH_COMPENSATION, new byte[] { 0x04, 0x01, 0x06 });
	        assertEquals("0.67 EV", descriptor.getAutoFlashCompensationDescription());

	        directory.setByteArray(NikonType2MakernoteDirectory.TAG_AUTO_FLASH_COMPENSATION, new byte[] { 0x02, 0x01, 0x06 });
	        assertEquals("0.33 EV", descriptor.getAutoFlashCompensationDescription());

	        directory.setByteArray(NikonType2MakernoteDirectory.TAG_AUTO_FLASH_COMPENSATION, new byte[] { (byte)0xFE, 0x01, 0x06 });
	        assertEquals("-0.33 EV", descriptor.getAutoFlashCompensationDescription());
	        System.out.println("Finished running testGetAutoFlashCompensationDescription()");
	    }
	}
	
	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class NikonType2MakernoteTest2
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
		// NikonType2MakernoteTest2 nm = me.new NikonType2MakernoteTest2();
		// try {
	    //    nm.setUp();
	    // }
		// catch(Exception e) {
		//    e.printStackTrace();
		//}
		public NikonType2MakernoteTest2() {
			
		}
	    private Metadata _metadata;
	    private NikonType2MakernoteDirectory _nikonDirectory;
	    private ExifIFD0Directory _exifIFD0Directory;
	    private ExifSubIFDDirectory _exifSubIFDDirectory;
	    private ExifThumbnailDirectory _thumbDirectory;

	    //@Before
	    public void setUp() throws Exception
	    {
	        ExifReaderTest er = new ExifReaderTest();
	    	_metadata = er.processBytes("C:/metadata/metadata-extractor-master/Tests/Data/nikonMakernoteType2b.jpg.app1");

	        _nikonDirectory = _metadata.getFirstDirectoryOfType(NikonType2MakernoteDirectory.class);
	        _exifIFD0Directory = _metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
	        _exifSubIFDDirectory = _metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
	        _thumbDirectory = _metadata.getFirstDirectoryOfType(ExifThumbnailDirectory.class);

	        assertNotNull(_nikonDirectory);
	        assertNotNull(_exifSubIFDDirectory);
	    }

	    /*
	        [Nikon Makernote] Makernote Unknown 1 =
	        [Nikon Makernote] ISO Setting = Unknown (0 0)
	        [Nikon Makernote] Color Mode = COLOR
	        [Nikon Makernote] Quality = NORMAL
	        [Nikon Makernote] White Balance = AUTO
	        [Nikon Makernote] Image Sharpening = AUTO
	        [Nikon Makernote] Focus Mode = AF-C
	        [Nikon Makernote] Flash Setting = NORMAL
	        [Nikon Makernote] Makernote Unknown 2 = 4416/500
	        [Nikon Makernote] ISO Selection = AUTO
	        [Nikon Makernote] Unknown tag (0x0011) = 1300
	        [Nikon Makernote] Image Adjustment = AUTO
	        [Nikon Makernote] Adapter = OFF
	        [Nikon Makernote] Focus Distance = 0
	        [Nikon Makernote] Digital Zoom = No digital zoom
	        [Nikon Makernote] AF Focus Position = Unknown ()
	        [Nikon Makernote] Unknown tag (0x008f) =
	        [Nikon Makernote] Unknown tag (0x0094) = 0
	        [Nikon Makernote] Unknown tag (0x0095) = FPNR
	        [Nikon Makernote] Unknown tag (0x0e00) = PrintIM
	        [Nikon Makernote] Unknown tag (0x0e10) = 1394
	    */
	    //@Test
	    //try {
	    //	nm.testNikonMakernote_MatchesKnownValues();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testNikonMakernote_MatchesKnownValues()
	    public void testNikonMakernote_MatchesKnownValues() throws Exception
	    {
	        assertEquals("0 1 0 0", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_FIRMWARE_VERSION));
	        assertEquals("0 0", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_ISO_1));
	        assertEquals("COLOR", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_COLOR_MODE));
	        assertEquals("NORMAL ", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_QUALITY_AND_FILE_FORMAT));
	        assertEquals("AUTO        ", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_CAMERA_WHITE_BALANCE));
	        assertEquals("AUTO  ", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_CAMERA_SHARPENING));
	        assertEquals("AF-C  ", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_AF_TYPE));
	        assertEquals("NORMAL      ", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_FLASH_SYNC_MODE));
//	        assertEquals(new Rational(4416,500), _nikonDirectory.getRational(NikonType3MakernoteDirectory.TAG_UNKNOWN_2));
	        assertEquals("AUTO  ", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_ISO_MODE));
	        assertEquals(1300, _nikonDirectory.getInt(0x0011));
	        assertEquals("AUTO         ", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_IMAGE_ADJUSTMENT));
	        assertEquals("OFF         ", _nikonDirectory.getString(NikonType2MakernoteDirectory.TAG_ADAPTER));
	        assertEquals(0, _nikonDirectory.getInt(NikonType2MakernoteDirectory.TAG_MANUAL_FOCUS_DISTANCE));
	        assertEquals(1, _nikonDirectory.getInt(NikonType2MakernoteDirectory.TAG_DIGITAL_ZOOM));
	        assertEquals("                ", _nikonDirectory.getString(0x008f));
	        assertEquals(0, _nikonDirectory.getInt(0x0094));
	        assertEquals("FPNR", _nikonDirectory.getString(0x0095));

	        // PrintIM
	        HashMap<Integer, String> _expectedData = new HashMap<Integer, String>();
	        _expectedData.put(0x0000, "0100");
	        _expectedData.put(0x0001, "0x00160016");
	        _expectedData.put(0x0002, "0x00000001");
	        _expectedData.put(0x0003, "0x0000005e");
	        _expectedData.put(0x0007, "0x00000000");
	        _expectedData.put(0x0008, "0x00000000");
	        _expectedData.put(0x0009, "0x00000000");
	        _expectedData.put(0x000A, "0x00000000");
	        _expectedData.put(0x000B, "0x000000a6");
	        _expectedData.put(0x000C, "0x00000000");
	        _expectedData.put(0x000D, "0x00000000");
	        _expectedData.put(0x000E, "0x000000be");
	        _expectedData.put(0x0100, "0x00000005");
	        _expectedData.put(0x0101, "0x00000001");

	        PrintIMDirectory nikonPrintImDirectory = _metadata.getFirstDirectoryOfType(PrintIMDirectory.class);

	        assertNotNull(nikonPrintImDirectory);

	        assertEquals(_expectedData.size(), nikonPrintImDirectory.getTagCount());
	        for (Map.Entry<Integer, String> _expected : _expectedData.entrySet())
	        {
	            assertEquals(_expected.getValue(), nikonPrintImDirectory.getDescription(_expected.getKey()));
	        }

//	        assertEquals("80 114 105 110 116 73 77 0 48 49 48 48 0 0 13 0 1 0 22 0 22 0 2 0 1 0 0 0 3 0 94 0 0 0 7 0 0 0 0 0 8 0 0 0 0 0 9 0 0 0 0 0 10 0 0 0 0 0 11 0 166 0 0 0 12 0 0 0 0 0 13 0 0 0 0 0 14 0 190 0 0 0 0 1 5 0 0 0 1 1 1 0 0 0 9 17 0 0 16 39 0 0 11 15 0 0 16 39 0 0 151 5 0 0 16 39 0 0 176 8 0 0 16 39 0 0 1 28 0 0 16 39 0 0 94 2 0 0 16 39 0 0 139 0 0 0 16 39 0 0 203 3 0 0 16 39 0 0 229 27 0 0 16 39 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0", _nikonDirectory.getString(0x0e00));
//	        assertEquals("PrintIM", _nikonDirectory.getString(0x0e00));
	        assertEquals(1394, _nikonDirectory.getInt(0x0e10));
	        System.out.println("Finished running testNikonMakernote_MatchesKnownValues()");
	    }

	    /*
	        [Exif] Image Description =
	        [Exif] Make = NIKON
	        [Exif] Model = E995
	        [Exif] X Resolution = 300 dots per inch
	        [Exif] Y Resolution = 300 dots per inch
	        [Exif] Resolution Unit = Inch
	        [Exif] Software = E995v1.6
	        [Exif] Date/Time = 2002:08:29 17:31:40
	        [Exif] YCbCr Positioning = Center of pixel array
	        [Exif] Exposure Time = 2439024/100000000 sec
	        [Exif] F-Number = F2.6
	        [Exif] Exposure Program = Program normal
	        [Exif] ISO Speed Ratings = 100
	        [Exif] Exif Version = 2.10
	        [Exif] Date/Time Original = 2002:08:29 17:31:40
	        [Exif] Date/Time Digitized = 2002:08:29 17:31:40
	        [Exif] Components Configuration = YCbCr
	        [Exif] Exposure Bias Value = 0 EV
	        [Exif] Max Aperture Value = F1
	        [Exif] Metering Mode = Multi-segment
	        [Exif] White Balance = Unknown
	        [Exif] Flash = Flash fired
	        [Exif] Focal Length = 8.2 mm
	        [Exif] User Comment =
	        [Exif] FlashPix Version = 1.00
	        [Exif] Color Space = sRGB
	        [Exif] Exif Image Width = 2048 pixels
	        [Exif] Exif Image Height = 1536 pixels
	        [Exif] File Source = Digital Still Camera (DSC)
	        [Exif] Scene Type = Directly photographed image
	    */
	    //@Test
	    //try {
	    //	nm.testExifDirectory_MatchesKnownValues();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testExifDirectory_MatchesKnownValues()
	    public void testExifDirectory_MatchesKnownValues() throws Exception
	    {
	        assertEquals("          ", _exifIFD0Directory.getString(ExifIFD0Directory.TAG_IMAGE_DESCRIPTION));
	        assertEquals("NIKON", _exifIFD0Directory.getString(ExifIFD0Directory.TAG_MAKE));
	        assertEquals("E995", _exifIFD0Directory.getString(ExifIFD0Directory.TAG_MODEL));
	        assertEquals(300, _exifIFD0Directory.getDouble(ExifIFD0Directory.TAG_X_RESOLUTION), 0.001);
	        assertEquals(300, _exifIFD0Directory.getDouble(ExifIFD0Directory.TAG_Y_RESOLUTION), 0.001);
	        assertEquals(2, _exifIFD0Directory.getInt(ExifIFD0Directory.TAG_RESOLUTION_UNIT));
	        assertEquals("E995v1.6", _exifIFD0Directory.getString(ExifIFD0Directory.TAG_SOFTWARE));
	        assertEquals("2002:08:29 17:31:40", _exifIFD0Directory.getString(ExifIFD0Directory.TAG_DATETIME));
	        assertEquals(1, _exifIFD0Directory.getInt(ExifIFD0Directory.TAG_YCBCR_POSITIONING));

	        assertEquals(new Rational(2439024, 100000000), _exifSubIFDDirectory.getRational(ExifSubIFDDirectory.TAG_EXPOSURE_TIME));
	        assertEquals(2.6, _exifSubIFDDirectory.getDouble(ExifSubIFDDirectory.TAG_FNUMBER), 0.001);
	        assertEquals(2, _exifSubIFDDirectory.getInt(ExifSubIFDDirectory.TAG_EXPOSURE_PROGRAM));
	        assertEquals(100, _exifSubIFDDirectory.getInt(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT));
	        assertEquals("48 50 49 48", _exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_EXIF_VERSION));
	        assertEquals("2002:08:29 17:31:40", _exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED));
	        assertEquals("2002:08:29 17:31:40", _exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL));
	        assertEquals("1 2 3 0", _exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_COMPONENTS_CONFIGURATION));
	        assertEquals(0, _exifSubIFDDirectory.getInt(ExifSubIFDDirectory.TAG_EXPOSURE_BIAS));
	        assertEquals("0", _exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_MAX_APERTURE));
	        assertEquals(5, _exifSubIFDDirectory.getInt(ExifSubIFDDirectory.TAG_METERING_MODE));
	        assertEquals(0, _exifSubIFDDirectory.getInt(ExifSubIFDDirectory.TAG_WHITE_BALANCE));
	        assertEquals(1, _exifSubIFDDirectory.getInt(ExifSubIFDDirectory.TAG_FLASH));
	        assertEquals(8.2, _exifSubIFDDirectory.getDouble(ExifSubIFDDirectory.TAG_FOCAL_LENGTH), 0.001);
	        assertEquals("0 0 0 0 0 0 0 0 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32 32", _exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_USER_COMMENT));
	        assertEquals("48 49 48 48", _exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_FLASHPIX_VERSION));
	        assertEquals(1, _exifSubIFDDirectory.getInt(ExifSubIFDDirectory.TAG_COLOR_SPACE));
	        assertEquals(2048, _exifSubIFDDirectory.getInt(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH));
	        assertEquals(1536, _exifSubIFDDirectory.getInt(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT));
	        assertEquals(3, _exifSubIFDDirectory.getInt(ExifSubIFDDirectory.TAG_FILE_SOURCE));
	        assertEquals(1, _exifSubIFDDirectory.getInt(ExifSubIFDDirectory.TAG_SCENE_TYPE));
	        System.out.println("Finished running testExifDirectory_MatchesKnownValues()");
	    }

	    /*
	        [Exif Thumbnail] Thumbnail Compression = JPEG (old-style)
	        [Exif Thumbnail] X Resolution = 72 dots per inch
	        [Exif Thumbnail] Y Resolution = 72 dots per inch
	        [Exif Thumbnail] Resolution Unit = Inch
	        [Exif Thumbnail] Thumbnail Offset = 1494 bytes
	        [Exif Thumbnail] Thumbnail Length = 6077 bytes
	    */
	    //@Test
	    //try {
	    //	nm.testExifThumbnailDirectory_MatchesKnownValues();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testExifThumbnailDirectory_MatchesKnownValues()
	    public void testExifThumbnailDirectory_MatchesKnownValues() throws Exception
	    {
	        assertEquals(6, _thumbDirectory.getInt(ExifThumbnailDirectory.TAG_COMPRESSION));
	        assertEquals(1494, _thumbDirectory.getInt(ExifThumbnailDirectory.TAG_THUMBNAIL_OFFSET));
	        assertEquals(6077, _thumbDirectory.getInt(ExifThumbnailDirectory.TAG_THUMBNAIL_LENGTH));
	        assertEquals(1494, _thumbDirectory.getInt(ExifThumbnailDirectory.TAG_THUMBNAIL_OFFSET));
	        assertEquals(72, _thumbDirectory.getInt(ExifThumbnailDirectory.TAG_X_RESOLUTION));
	        assertEquals(72, _thumbDirectory.getInt(ExifThumbnailDirectory.TAG_Y_RESOLUTION));
	        System.out.println("Finished running testExifThumbnailDirectory_MatchesKnownValues()");
	    }
	}

	/**
	 * @author psandhaus, Drew Noakes
	 */
	public class PanasonicMakernoteDescriptorTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
		// PanasonicMakernoteDescriptorTest pm = me.new PanasonicMakernoteDescriptorTest();
		// try {
	    //    pm.setUp();
	    // }
		// catch(Exception e) {
		//    e.printStackTrace();
		//}
		//Finished running testGetDetectedFaces()
		public PanasonicMakernoteDescriptorTest() {
			
		}
	    private PanasonicMakernoteDirectory _panasonicDirectory;

	    //@Before
	    public void setUp() throws Exception
	    {
	    	ExifReaderTest er = new ExifReaderTest();
	        _panasonicDirectory = er.processBytes("C:/metadata/metadata-extractor-master/Tests/Data/withPanasonicFaces.jpg.app1", PanasonicMakernoteDirectory.class);
	    }

	    //@Test
	    //try {
	    //	pm.testGetDetectedFaces();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetDetectedFaces()
	    public void testGetDetectedFaces() throws Exception
	    {
	        Face expResult = new Face(142, 120, 76, 76, null, null);
	        Face[] result = _panasonicDirectory.getDetectedFaces();
	        assertNotNull(result);
	        assertEquals(expResult, result[0]);
	        System.out.println("Finished running testGetDetectedFaces()");
	    }

	    //@Test
	    //try {
	    //	pm.testGetRecognizedFaces();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetRecognizedFaces()
	    public void testGetRecognizedFaces() throws Exception
	    {
	        Face expResult = new Face(142, 120, 76, 76, "NIELS", new Age(31, 7, 15, 0, 0, 0));
	        Face[] result = _panasonicDirectory.getRecognizedFaces();
	        assertNotNull(result);
	        assertEquals(expResult, result[0]);
	        System.out.println("Finished running testGetRecognizedFaces()");
	    }
	}
	
	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class SonyType1MakernoteTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    // SonyType1MakernoteTest sm = me.new SonyType1MakernoteTest();
	    public SonyType1MakernoteTest() {
	    	
	    }
	    //try {
	    //	sm.testSonyType1Makernote();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testSonyType1Makernote()
		public void testSonyType1Makernote() throws Exception
	    {
	    	ExifReaderTest er = new ExifReaderTest();
	        SonyType1MakernoteDirectory directory = er.processBytes("C:/metadata/metadata-extractor-master/Tests/Data/sonyType1.jpg.app1", SonyType1MakernoteDirectory.class);

	        assertNotNull(directory);
	        assertFalse(directory.hasErrors());

	        SonyType1MakernoteDescriptor descriptor = new SonyType1MakernoteDescriptor(directory);

	        assertNull(directory.getObject(SonyType1MakernoteDirectory.TAG_COLOR_TEMPERATURE));
	        assertNull(descriptor.getColorTemperatureDescription());
	        assertNull(directory.getObject(SonyType1MakernoteDirectory.TAG_SCENE_MODE));
	        assertNull(descriptor.getSceneModeDescription());
	        assertNull(directory.getObject(SonyType1MakernoteDirectory.TAG_ZONE_MATCHING));
	        assertNull(descriptor.getZoneMatchingDescription());
	        assertNull(directory.getObject(SonyType1MakernoteDirectory.TAG_DYNAMIC_RANGE_OPTIMISER));
	        assertNull(descriptor.getDynamicRangeOptimizerDescription());
	        assertNull(directory.getObject(SonyType1MakernoteDirectory.TAG_IMAGE_STABILISATION));
	        assertNull(descriptor.getImageStabilizationDescription());
	        assertNull(directory.getObject(SonyType1MakernoteDirectory.TAG_COLOR_MODE));
	        assertNull(descriptor.getColorModeDescription());

	        assertEquals("On (Shooting)", descriptor.getAntiBlurDescription());
	        assertEquals("Program", descriptor.getExposureModeDescription());
	        assertEquals("Off", descriptor.getLongExposureNoiseReductionDescription());
	        assertEquals("Off", descriptor.getMacroDescription());
	        assertEquals("Normal", descriptor.getJpegQualityDescription());
	        System.out.println("Finished running testSonyType1Makernote()");
	    }
	}

	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class SonyType6MakernoteTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    // SonyType6MakernoteTest sm = me.new SonyType6MakernoteTest();
	    public SonyType6MakernoteTest() {
	    	
	    }
	    //try {
	    //	sm.testSonyType6Makernote();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testSonyType6Makernote()
	    public void testSonyType6Makernote() throws Exception
	    {
	    	ExifReaderTest er = new ExifReaderTest();
	        SonyType6MakernoteDirectory directory = er.processBytes("C:/metadata/metadata-extractor-master/Tests/Data/sonyType6.jpg.app1.0", SonyType6MakernoteDirectory.class);

	        assertNotNull(directory);
	        assertFalse(directory.hasErrors());

	        SonyType6MakernoteDescriptor descriptor = new SonyType6MakernoteDescriptor(directory);

	        assertEquals("2.00", descriptor.getMakernoteThumbVersionDescription());
	        System.out.println("Finished running testSonyType6Makernote()");
	    }
	}
	
	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class JpegReaderTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //JpegReaderTest jr = me.new JpegReaderTest();
		// try {
	    //    jr.setUp();
	    // }
		// catch(Exception e) {
		//    e.printStackTrace();
		//}
	    @NotNull
	    public JpegDirectory processBytes(String filePath) throws IOException
	    {
	        Metadata metadata = new Metadata();
	        new JpegReader().extract(FileUtil.readBytes(filePath), metadata, JpegSegmentType.SOF0);

	        JpegDirectory directory = metadata.getFirstDirectoryOfType(JpegDirectory.class);
	        assertNotNull(directory);
	        return directory;
	    }

	    private JpegDirectory _directory;

	    //@Before
	    public void setUp() throws JpegProcessingException, IOException
	    {
	        _directory = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/simple.jpg.sof0");
	    }

	    //@Test
	    //try {
	    //	jr.testExtract_Width();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testExtract_Width
	    public void testExtract_Width() throws Exception
	    {
	        assertEquals(800, _directory.getInt(JpegDirectory.TAG_IMAGE_WIDTH));
	        System.out.println("Finished running testExtract_Width");
	    }

	    //@Test
	    //try {
	    //	jr.testExtract_Height();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testExtract_Height
	    public void testExtract_Height() throws Exception
	    {
	        assertEquals(600, _directory.getInt(JpegDirectory.TAG_IMAGE_HEIGHT));
	        System.out.println("Finished running testExtract_Height");
	    }

	    //@Test
	    //try {
	    //	jr.testExtract_DataPrecision();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testExtract_DataPrecision()
	    public void testExtract_DataPrecision() throws Exception
	    {
	        assertEquals(8, _directory.getInt(JpegDirectory.TAG_DATA_PRECISION));
	        System.out.println("Finished running testExtract_DataPrecision()");
	    }

	    //@Test
	    //try {
	    //	jr.testExtract_NumberOfComponents();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testExtract_NumberOfComponents()
	    public void testExtract_NumberOfComponents() throws Exception
	    {
	        assertEquals(3, _directory.getInt(JpegDirectory.TAG_NUMBER_OF_COMPONENTS));
	        System.out.println("Finished running testExtract_NumberOfComponents()");
	    }

	    //@Test
	    //try {
	    //	jr.testComponentData1();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testComponentData1()
	    public void testComponentData1() throws Exception
	    {
	        JpegComponent component = (JpegComponent)_directory.getObject(JpegDirectory.TAG_COMPONENT_DATA_1);

	        assertNotNull(component);
	        assertEquals("Y", component.getComponentName());
	        assertEquals(1, component.getComponentId());
	        assertEquals(0, component.getQuantizationTableNumber());
	        assertEquals(2, component.getHorizontalSamplingFactor());
	        assertEquals(2, component.getVerticalSamplingFactor());
	        System.out.println("Finished running testComponentData1()");
	    }

	    //@Test
	    //try {
	    //	jr.testComponentData2();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testComponentData2()
	    public void testComponentData2() throws Exception
	    {
	        JpegComponent component = (JpegComponent)_directory.getObject(JpegDirectory.TAG_COMPONENT_DATA_2);

	        assertNotNull(component);
	        assertEquals("Cb", component.getComponentName());
	        assertEquals(2, component.getComponentId());
	        assertEquals(1, component.getQuantizationTableNumber());
	        assertEquals(1, component.getHorizontalSamplingFactor());
	        assertEquals(1, component.getVerticalSamplingFactor());
	        assertEquals("Cb component: Quantization table 1, Sampling factors 1 horiz/1 vert", _directory.getDescription(JpegDirectory.TAG_COMPONENT_DATA_2));
	        System.out.println("Finished running testComponentData2()");
	    }

	    //@Test
	    //try {
	    //	jr.testComponentData3();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testComponentData3()
	    public void testComponentData3() throws Exception
	    {
	        JpegComponent component = (JpegComponent)_directory.getObject(JpegDirectory.TAG_COMPONENT_DATA_3);

	        assertNotNull(component);
	        assertEquals("Cr", component.getComponentName());
	        assertEquals(3, component.getComponentId());
	        assertEquals(1, component.getQuantizationTableNumber());
	        assertEquals(1, component.getHorizontalSamplingFactor());
	        assertEquals(1, component.getVerticalSamplingFactor());
	        assertEquals("Cr component: Quantization table 1, Sampling factors 1 horiz/1 vert", _directory.getDescription(JpegDirectory.TAG_COMPONENT_DATA_3));
	        System.out.println("Finished running testComponentData3()");
	    }

	/*
	    // this test is part of an incomplete investigation into extracting audio from JPG files
	    public void testJpegWithAudio() throws Exception
	    {
	        // use a known testing image
	        File jpegFile = new File("Tests/com/drew/metadata/jpeg/audioPresent.jpg");

	        JpegSegmentReader jpegSegmentReader = new JpegSegmentReader(jpegFile);
	        byte[] segment1Bytes = jpegSegmentReader.readSegment(JpegSegmentReader.APP2);
	        System.out.println(segment1Bytes.length);

//	        System.out.println(jpegSegmentReader.readSegment(JpegSegmentReader.APP1));
	        System.out.println(jpegSegmentReader.readSegment(JpegSegmentReader.APP2).length);
//	        System.out.println(jpegSegmentReader.readSegment(JpegSegmentReader.APP3));
//	        System.out.println(jpegSegmentReader.readSegment(JpegSegmentReader.APP4));
//	        System.out.println(jpegSegmentReader.readSegment(JpegSegmentReader.APP5));
//	        System.out.println(jpegSegmentReader.readSegment(JpegSegmentReader.APP6));
//	        System.out.println(jpegSegmentReader.readSegment(JpegSegmentReader.APP7));
//	        System.out.println(jpegSegmentReader.readSegment(JpegSegmentReader.APP8));
//	        System.out.println(jpegSegmentReader.readSegment(JpegSegmentReader.APP9));
//	        System.out.println(jpegSegmentReader.readSegment(JpegSegmentReader.APPA));
//	        System.out.println(jpegSegmentReader.readSegment(JpegSegmentReader.APPB));
//	        System.out.println(jpegSegmentReader.readSegment(JpegSegmentReader.APPC));
//	        System.out.println(jpegSegmentReader.readSegment(JpegSegmentReader.APPD));
//	        System.out.println(jpegSegmentReader.readSegment(JpegSegmentReader.APPE));
//	        System.out.println(jpegSegmentReader.readSegment(JpegSegmentReader.APPF));
//	        System.out.println(jpegSegmentReader.readSegment(JpegSegmentReader.COM));
	        System.out.println(jpegSegmentReader.readSegment(JpegSegmentReader.DHT).length);
	        System.out.println(jpegSegmentReader.readSegment(JpegSegmentReader.DQT).length);
	        System.out.println(jpegSegmentReader.readSegment(JpegSegmentReader.SOF0).length);
//	        System.out.println(jpegSegmentReader.readSegment(JpegSegmentReader.SOI));

	        // write the segment's data out to a wav file...
	        File audioFile = new File("Tests/com/drew/metadata/jpeg/audio.wav");
	        FileOutputStream os = null;
	        try
	        {
	            os = new FileOutputStream(audioFile);
	            os.write(segment1Bytes);
	        }
	        finally
	        {
	            if (os!=null)
	                os.close();
	        }
	    }
	*/
	}

	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class JpegComponentTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //JpegComponentTest jc = me.new JpegComponentTest();
	    //@Test
		//try {
	    //	jc.testGetComponentCharacter();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running testGetComponentCharacter()
	    public void testGetComponentCharacter() throws Exception
	    {
	        JpegComponent component;

	        component = new JpegComponent(1,2,3);
	        assertEquals("Y", component.getComponentName());

	        component = new JpegComponent(2,2,3);
	        assertEquals("Cb", component.getComponentName());

	        component = new JpegComponent(3,2,3);
	        assertEquals("Cr", component.getComponentName());

	        component = new JpegComponent(4,2,3);
	        assertEquals("I", component.getComponentName());

	        component = new JpegComponent(5,2,3);
	        assertEquals("Q", component.getComponentName());
	        System.out.println("Finished running testGetComponentCharacter()");
	    }
	}
	
	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class JpegDescriptorTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //JpegDescriptorTest jd = me.new JpegDescriptorTest();
		// try {
	    //    jd.setUp();
	    // }
		// catch(Exception e) {
		//    e.printStackTrace();
		//}
		public JpegDescriptorTest() {
			
		}
	    private JpegDirectory _directory;
	    private JpegDescriptor _descriptor;

	    //@Before
	    public void setUp() throws Exception
	    {
	        _directory = new JpegDirectory();
	        _descriptor = new JpegDescriptor(_directory);
	    }

	    //@Test
	    //try {
	    //	jd.testGetComponentDataDescription_InvalidComponentNumber();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetComponentDataDescription_InvalidComponentNumber()
	    public void testGetComponentDataDescription_InvalidComponentNumber() throws Exception
	    {
	        assertNull(_descriptor.getComponentDataDescription(1));
	        System.out.println("Finished running testGetComponentDataDescription_InvalidComponentNumber()");
	    }

	    //@Test
	    //try {
	    //	jd.testGetImageWidthDescription();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetImageWidthDescription()
	    public void testGetImageWidthDescription() throws Exception
	    {
	        _directory.setInt(JpegDirectory.TAG_IMAGE_WIDTH, 123);
	        assertEquals("123 pixels", _descriptor.getImageWidthDescription());
	        assertEquals("123 pixels", _directory.getDescription(JpegDirectory.TAG_IMAGE_WIDTH));
	        System.out.println("Finished running testGetImageWidthDescription()");
	    }

	    //@Test
	    //try {
	    //	jd.testGetImageHeightDescription();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetImageHeightDescription()
	    public void testGetImageHeightDescription() throws Exception
	    {
	        _directory.setInt(JpegDirectory.TAG_IMAGE_HEIGHT, 123);
	        assertEquals("123 pixels", _descriptor.getImageHeightDescription());
	        assertEquals("123 pixels", _directory.getDescription(JpegDirectory.TAG_IMAGE_HEIGHT));
	        System.out.println("Finished running testGetImageHeightDescription()");
	    }

	    //@Test
	    //try {
	    //	jd.testGetDataPrecisionDescription();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetDataPrecisionDescription()
	    public void testGetDataPrecisionDescription() throws Exception
	    {
	        _directory.setInt(JpegDirectory.TAG_DATA_PRECISION, 8);
	        assertEquals("8 bits", _descriptor.getDataPrecisionDescription());
	        assertEquals("8 bits", _directory.getDescription(JpegDirectory.TAG_DATA_PRECISION));
	        System.out.println("Finished running testGetDataPrecisionDescription()");
	    }

	    //@Test
	    //try {
	    //	jd.testGetComponentDescription();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetComponentDescription()
	    public void testGetComponentDescription() throws MetadataException
	    {
	        JpegComponent component1 = new JpegComponent(1, 0x22, 0);
	        _directory.setObject(JpegDirectory.TAG_COMPONENT_DATA_1, component1);
	        assertEquals("Y component: Quantization table 0, Sampling factors 2 horiz/2 vert", _directory.getDescription(JpegDirectory.TAG_COMPONENT_DATA_1));
	        assertEquals("Y component: Quantization table 0, Sampling factors 2 horiz/2 vert", _descriptor.getComponentDataDescription(0));
	        System.out.println("Finished running testGetComponentDescription()");
	    }
	}

	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class JpegDirectoryTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //JpegDirectoryTest jd = me.new JpegDirectoryTest();
		// try {
	    //    jd.setUp();
	    // }
		// catch(Exception e) {
		//    e.printStackTrace();
		//}
		public JpegDirectoryTest() {
			
		}
	    private JpegDirectory _directory;

	    //@Before
	    public void setUp()
	    {
	        _directory = new JpegDirectory();
	    }

	    //@Test
	    //try {
	    //	jd.testSetAndGetValue();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testSetAndGetValue()
	    public void testSetAndGetValue() throws Exception
	    {
	        _directory.setInt(123, 8);
	        assertEquals(8, _directory.getInt(123));
	        System.out.println("Finished running testSetAndGetValue()");
	    }

	    //@Test
	    //try {
	    //	jd.testGetComponent_NotAdded();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //} 
	    //Finished running testGetComponent_NotAdded()
	    public void testGetComponent_NotAdded()
	    {
	        assertNull(_directory.getComponent(1));
	        System.out.println("Finished running testGetComponent_NotAdded()");
	    }

	    // NOTE tests for individual tag values exist in JpegReaderTest.java

	    //@Test
	    //try {
	    //	jd.testGetImageWidth();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //} 
	    //Finished running testGetImageWidth()
	    public void testGetImageWidth() throws Exception
	    {
	        _directory.setInt(JpegDirectory.TAG_IMAGE_WIDTH, 123);
	        assertEquals(123, _directory.getImageWidth());
	        System.out.println("Finished running testGetImageWidth()");
	    }

	    //@Test
	    //try {
	    //	jd.testGetImageHeight();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //} 
	    //Finished running testGetImageHeight()
	    public void testGetImageHeight() throws Exception
	    {
	        _directory.setInt(JpegDirectory.TAG_IMAGE_HEIGHT, 123);
	        assertEquals(123, _directory.getImageHeight());
	        System.out.println("Finished running testGetImageHeight()");
	    }


	    //@Test
	    //try {
	    //	jd.testGetNumberOfComponents();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //} 
	    //Finished running testGetNumberOfComponents()
	    public void testGetNumberOfComponents() throws Exception
	    {
	        _directory.setInt(JpegDirectory.TAG_NUMBER_OF_COMPONENTS, 3);
	        assertEquals(3, _directory.getNumberOfComponents());
	        assertEquals("3", _directory.getDescription(JpegDirectory.TAG_NUMBER_OF_COMPONENTS));
	        System.out.println("Finished running testGetNumberOfComponents()");
	    }

	    //@Test
	    //try {
	    //	jd.testGetComponent();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //} 
	    //Finished running testGetComponent()
	    public void testGetComponent() throws Exception
	    {
	        JpegComponent component1 = new JpegComponent(1, 2, 3);
	        JpegComponent component2 = new JpegComponent(1, 2, 3);
	        JpegComponent component3 = new JpegComponent(1, 2, 3);
	        JpegComponent component4 = new JpegComponent(1, 2, 3);

	        _directory.setObject(JpegDirectory.TAG_COMPONENT_DATA_1, component1);
	        _directory.setObject(JpegDirectory.TAG_COMPONENT_DATA_2, component2);
	        _directory.setObject(JpegDirectory.TAG_COMPONENT_DATA_3, component3);
	        _directory.setObject(JpegDirectory.TAG_COMPONENT_DATA_4, component4);

	        // component numbers are zero-indexed for this method
	        assertSame(component1, _directory.getComponent(0));
	        assertSame(component2, _directory.getComponent(1));
	        assertSame(component3, _directory.getComponent(2));
	        assertSame(component4, _directory.getComponent(3));
	        System.out.println("Finished running testGetComponent()");
	    }
	}
	
	/**
	 * @author Nadahar
	 */
	public class HuffmanTablesDirectoryTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //HuffmanTablesDirectoryTest htdt = me.new HuffmanTablesDirectoryTest();
		// try {
	    //    htdt.setUp();
	    // }
		// catch(Exception e) {
		//    e.printStackTrace();
		//}
		
		public HuffmanTablesDirectoryTest() {
			
		}
	    private HuffmanTablesDirectory _directory;

	    //@Before
	    public void setUp()
	    {
	        _directory = new HuffmanTablesDirectory();
	    }

	    //@Test
	    //try {
	    //	htdt.testSetAndGetValue();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //} 
	    //Finished running testSetAndGetValue()
	    public void testSetAndGetValue() throws Exception
	    {
	        _directory.setInt(32, 8);
	        assertEquals(8, _directory.getInt(32));
	        System.out.println("Finished running testSetAndGetValue()");
	    }

	    //@Test
	    //try {
	    //	htdt.testGetComponent_NotAdded();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //} 
        //Finished running testComponent_NotAdded()
	    public void testGetComponent_NotAdded()
	    {
	        try {
	            _directory.getTable(1);
	            fail();
	        } catch (IndexOutOfBoundsException e) {
	            // Expected exception
	        }
	        System.out.println("Finished running testComponent_NotAdded()");
	    }

	    //@Test
	    //try {
	    //	htdt.testGetNumberOfTables();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //} 
	    //Finished running testGetNumberOfTables()
	    public void testGetNumberOfTables() throws Exception
	    {
	        _directory.setInt(HuffmanTablesDirectory.TAG_NUMBER_OF_TABLES, 9);
	        assertEquals(9,_directory.getNumberOfTables());
	        assertEquals("9 Huffman tables", _directory.getDescription(HuffmanTablesDirectory.TAG_NUMBER_OF_TABLES));
	        System.out.println("Finished running testGetNumberOfTables()");
	    }

	    //@Test
	    //try {
	    //	htdt.testIsTypical();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //} 
	    //Finished running testIsTypical()
	    public void testIsTypical() throws Exception
	    {
	        _directory.tables.add(_directory.new HuffmanTable(
	            HuffmanTableClass.AC,
	            0,
	            _directory.TYPICAL_CHROMINANCE_AC_LENGTHS,
	            _directory.TYPICAL_CHROMINANCE_AC_VALUES
	        ));
	        _directory.tables.add(_directory.new HuffmanTable(
	            HuffmanTableClass.DC,
	            0,
	            _directory.TYPICAL_LUMINANCE_DC_LENGTHS,
	            _directory.TYPICAL_LUMINANCE_DC_VALUES
	        ));

	        assertTrue(_directory.getTable(0).isTypical());
	        assertFalse(_directory.getTable(0).isOptimized());
	        assertTrue(_directory.getTable(1).isTypical());
	        assertFalse(_directory.getTable(1).isOptimized());

	        assertTrue(_directory.isTypical());
	        assertFalse(_directory.isOptimized());
	        System.out.println("Finished running testIsTypical()");
	    }
	}
	
	/**
	 * @author Nadahar
	 */
	public class HuffmanTablesDescriptorTest
	{
	    private HuffmanTablesDirectory _directory;
	    private HuffmanTablesDescriptor _descriptor;
	    //MetadataExtractorTest me = new MetadataExtractorTest();
	    //HuffmanTablesDescriptorTest htdt = me.new HuffmanTablesDescriptorTest();
		// try {
	    //    htdt.setUp();
	    // }
		// catch(Exception e) {
		//    e.printStackTrace();
		//}
		
		public HuffmanTablesDescriptorTest() {
			
		}

	    //@Before
	    public void setUp() throws Exception
	    {
	        _directory = new HuffmanTablesDirectory();
	        _descriptor = new HuffmanTablesDescriptor(_directory);
	    }

	    //@Test
	    //try {
	    //	htdt.testGetNumberOfTablesDescription();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetNumberOfTablesDescription()
	    public void testGetNumberOfTablesDescription() throws Exception
	    {
	        assertNull(_descriptor.getNumberOfTablesDescription());
	        _directory.setInt(HuffmanTablesDirectory.TAG_NUMBER_OF_TABLES, 0);
	        assertEquals("0 Huffman tables", _descriptor.getNumberOfTablesDescription());
	        assertEquals("0 Huffman tables", _descriptor.getDescription(HuffmanTablesDirectory.TAG_NUMBER_OF_TABLES));
	        _directory.setInt(HuffmanTablesDirectory.TAG_NUMBER_OF_TABLES, 1);
	        assertEquals("1 Huffman table", _descriptor.getNumberOfTablesDescription());
	        assertEquals("1 Huffman table", _descriptor.getDescription(HuffmanTablesDirectory.TAG_NUMBER_OF_TABLES));
	        _directory.setInt(HuffmanTablesDirectory.TAG_NUMBER_OF_TABLES, 3);
	        assertEquals("3 Huffman tables", _descriptor.getNumberOfTablesDescription());
	        assertEquals("3 Huffman tables", _descriptor.getDescription(HuffmanTablesDirectory.TAG_NUMBER_OF_TABLES));
	        System.out.println("Finished running testGetNumberOfTablesDescription()");
	    }
	}
	
	/**
	 * @author Nadahar
	 */
	public class JpegDhtReaderTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //JpegDhtReaderTest jdrt = me.new JpegDhtReaderTest();
		// try {
	    //    jdrt.setUp();
	    // }
		// catch(Exception e) {
		//    e.printStackTrace();
		//}
		
		public JpegDhtReaderTest() {
			
		}
	    @NotNull
	    public HuffmanTablesDirectory processBytes(String filePath) throws Exception
	    {
	        Metadata metadata = new Metadata();
	        JpegSegmentData segmentData = JpegSegmentReader.readSegments(
	            new File(filePath),
	            Collections.singletonList(JpegSegmentType.DHT));

	        Iterable<byte[]> segments = segmentData.getSegments(JpegSegmentType.DHT);
	        for (byte[] segment : segments) {
	            new JpegDhtReader().extract(new SequentialByteArrayReader(segment), metadata);
	        }


	        HuffmanTablesDirectory directory = metadata.getFirstDirectoryOfType(HuffmanTablesDirectory.class);
	        assertNotNull(directory);
	        assertEquals(1, metadata.getDirectoriesOfType(HuffmanTablesDirectory.class).size());
	        return directory;
	    }

	    private HuffmanTablesDirectory _directory;

	    //@Before
	    public void setUp() throws Exception
	    {
	        _directory = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/withExifAndIptc.jpg");
	    }

	    //@Test
	    //try {
	    //	jdrt.testExtract_NumberOfTables();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testExtract_NumberOfTables()
	    public void testExtract_NumberOfTables() throws Exception
	    {
	        assertEquals(4, _directory.getInt(HuffmanTablesDirectory.TAG_NUMBER_OF_TABLES));
	        assertEquals(4, _directory.getNumberOfTables());
	        System.out.println("Finished running testExtract_NumberOfTables()");
	    }

	    //@Test
	    //try {
	    //	jdrt.testExtract_Tables();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testExtract_Tables()
	    public void testExtract_Tables() throws Exception
	    {
	        byte[] l = {0, 1, 4, 1, 2, 3, 3, 8, 5, 9, 6, 4, 6, 2, 3, 0};
	        byte[] v = {0, 1, 3, 2, 4, 5};

	        assertArrayEquals(l, _directory.getTable(1).getLengthBytes());
	        assertArrayEquals(v, _directory.getTable(2).getValueBytes());
	        assertEquals(HuffmanTableClass.DC, _directory.getTable(0).getTableClass());
	        assertEquals(HuffmanTableClass.AC, _directory.getTable(1).getTableClass());
	        assertEquals(HuffmanTableClass.DC, _directory.getTable(2).getTableClass());
	        assertEquals(HuffmanTableClass.AC, _directory.getTable(3).getTableClass());
	        assertEquals(0, _directory.getTable(0).getTableDestinationId());
	        assertEquals(0, _directory.getTable(1).getTableDestinationId());
	        assertEquals(1, _directory.getTable(2).getTableDestinationId());
	        assertEquals(1, _directory.getTable(3).getTableDestinationId());
	        assertEquals(25, _directory.getTable(0).getTableLength());
	        assertEquals(74, _directory.getTable(1).getTableLength());
	        assertEquals(23, _directory.getTable(2).getTableLength());
	        assertEquals(38, _directory.getTable(3).getTableLength());
	        System.out.println("Finished running testExtract_Tables()");
	    }
	}

	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings({ "ConstantConditions" })
	public class JpegSegmentDataTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //JpegSegmentDataTest jsdt = me.new JpegSegmentDataTest();
	    //@Test
		//try {
	    //	jsdt.testAddAndGetSegment();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running testAddAndGetSegment()
	    public void testAddAndGetSegment() throws Exception
	    {
	        JpegSegmentData segmentData = new JpegSegmentData();

	        byte segmentMarker = (byte)12;
	        byte[] segmentBytes = new byte[] { 1,2,3 };

	        segmentData.addSegment(segmentMarker, segmentBytes);
	        assertEquals(1, segmentData.getSegmentCount(segmentMarker));
	        assertArrayEquals(segmentBytes, segmentData.getSegment(segmentMarker));
	        System.out.println("Finished running testAddAndGetSegment()");
	    }

	   // @Test
	    //try {
	    //	jsdt.testContainsSegment();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testContainsSegment()
	    public void testContainsSegment() throws Exception
	    {
	        JpegSegmentData segmentData = new JpegSegmentData();

	        byte segmentMarker = (byte)12;
	        byte[] segmentBytes = new byte[] { 1,2,3 };

	        assertTrue(!segmentData.containsSegment(segmentMarker));

	        segmentData.addSegment(segmentMarker, segmentBytes);

	        assertTrue(segmentData.containsSegment(segmentMarker));
	        System.out.println("Finished running testContainsSegment()");
	    }

	    //@Test
	    //try {
	    //	jsdt.testAddingMultipleSegments();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testAddingMultipleSegments()
	    public void testAddingMultipleSegments() throws Exception
	    {
	        JpegSegmentData segmentData = new JpegSegmentData();

	        byte segmentMarker1 = (byte)12;
	        byte segmentMarker2 = (byte)21;
	        byte[] segmentBytes1 = new byte[] { 1,2,3 };
	        byte[] segmentBytes2 = new byte[] { 3,2,1 };

	        segmentData.addSegment(segmentMarker1, segmentBytes1);
	        segmentData.addSegment(segmentMarker2, segmentBytes2);
	        assertEquals(1, segmentData.getSegmentCount(segmentMarker1));
	        assertEquals(1, segmentData.getSegmentCount(segmentMarker2));
	        assertArrayEquals(segmentBytes1, segmentData.getSegment(segmentMarker1));
	        assertArrayEquals(segmentBytes2, segmentData.getSegment(segmentMarker2));
	        System.out.println("Finished running testAddingMultipleSegments()");
	    }

	    //@Test
	    //try {
	    //	jsdt.testSegmentWithMultipleOccurrences();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testSegmentWithMultipleOccurrences()
	    public void testSegmentWithMultipleOccurrences() throws Exception
	    {
	        JpegSegmentData segmentData = new JpegSegmentData();

	        byte segmentMarker = (byte)12;
	        byte[] segmentBytes1 = new byte[] { 1,2,3 };
	        byte[] segmentBytes2 = new byte[] { 3,2,1 };

	        segmentData.addSegment(segmentMarker, segmentBytes1);
	        segmentData.addSegment(segmentMarker, segmentBytes2);
	        assertEquals(2, segmentData.getSegmentCount(segmentMarker));
	        assertArrayEquals(segmentBytes1, segmentData.getSegment(segmentMarker));
	        assertArrayEquals(segmentBytes1, segmentData.getSegment(segmentMarker, 0));
	        assertArrayEquals(segmentBytes2, segmentData.getSegment(segmentMarker, 1));
	        System.out.println("Finished running testSegmentWithMultipleOccurrences()");
	    }

	    //@Test
	    //try {
	    //	jsdt.testRemoveSegmentOccurrence();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testRemoveSegmentOccurrence()
	    public void testRemoveSegmentOccurrence() throws Exception
	    {
	        JpegSegmentData segmentData = new JpegSegmentData();

	        byte segmentMarker = (byte)12;
	        byte[] segmentBytes1 = new byte[] { 1,2,3 };
	        byte[] segmentBytes2 = new byte[] { 3,2,1 };

	        segmentData.addSegment(segmentMarker, segmentBytes1);
	        segmentData.addSegment(segmentMarker, segmentBytes2);

	        assertEquals(2, segmentData.getSegmentCount(segmentMarker));

	        assertArrayEquals(segmentBytes1, segmentData.getSegment(segmentMarker, 0));

	        segmentData.removeSegmentOccurrence(segmentMarker, 0);

	        assertArrayEquals(segmentBytes2, segmentData.getSegment(segmentMarker, 0));
	        System.out.println("Finished running testRemoveSegmentOccurrence()");
	    }

	    //@Test
	    //try {
	    //	jsdt.testRemoveSegment();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testRemoveSegment()
	    public void testRemoveSegment() throws Exception
	    {
	        JpegSegmentData segmentData = new JpegSegmentData();

	        byte segmentMarker = (byte)12;
	        byte[] segmentBytes1 = new byte[] { 1,2,3 };
	        byte[] segmentBytes2 = new byte[] { 3,2,1 };

	        segmentData.addSegment(segmentMarker, segmentBytes1);
	        segmentData.addSegment(segmentMarker, segmentBytes2);

	        assertEquals(2, segmentData.getSegmentCount(segmentMarker));
	        assertTrue(segmentData.containsSegment(segmentMarker));

	        assertArrayEquals(segmentBytes1, segmentData.getSegment(segmentMarker, 0));

	        segmentData.removeSegment(segmentMarker);

	        assertTrue(!segmentData.containsSegment(segmentMarker));
	        assertEquals(0, segmentData.getSegmentCount(segmentMarker));
	        System.out.println("Finished running testRemoveSegment()");
	    }
	}

	/**
	 * Unit tests for {@link JpegSegmentReader}.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class JpegSegmentReaderTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //JpegSegmentReaderTest jsrt = me.new JpegSegmentReaderTest();
	    //@Test
		//try {
	    //	jsrt.testReadAllSegments();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running testReadAllSegments()
	    public void testReadAllSegments() throws Exception
	    {
	        JpegSegmentData segmentData = JpegSegmentReader.readSegments(new File("C:/metadata/metadata-extractor-master/Tests/Data/withExifAndIptc.jpg"), null);

	        assertEquals(1, segmentData.getSegmentCount(JpegSegmentType.APP0));
	        assertArrayEquals(
	                FileUtil.readBytes("C:/metadata/metadata-extractor-master/Tests/Data/withExifAndIptc.jpg.app0"),
	                segmentData.getSegment(JpegSegmentType.APP0));
	        assertNull(segmentData.getSegment(JpegSegmentType.APP0, 1));

	        assertEquals(2, segmentData.getSegmentCount(JpegSegmentType.APP1));
	        assertArrayEquals(
	                FileUtil.readBytes("C:/metadata/metadata-extractor-master/Tests/Data/withExifAndIptc.jpg.app1.0"),
	                segmentData.getSegment(JpegSegmentType.APP1, 0));
	        assertArrayEquals(
	                FileUtil.readBytes("C:/metadata/metadata-extractor-master/Tests/Data/withExifAndIptc.jpg.app1.1"),
	                segmentData.getSegment(JpegSegmentType.APP1, 1));
	        assertNull(segmentData.getSegment(JpegSegmentType.APP1, 2));

	        assertEquals(1, segmentData.getSegmentCount(JpegSegmentType.APP2));
	        assertArrayEquals(
	                FileUtil.readBytes("C:/metadata/metadata-extractor-master/Tests/Data/withExifAndIptc.jpg.app2"),
	                segmentData.getSegment(JpegSegmentType.APP2));
	        assertNull(segmentData.getSegment(JpegSegmentType.APP2, 1));

	        assertEquals(1, segmentData.getSegmentCount(JpegSegmentType.APPD));
	        assertArrayEquals(
	                FileUtil.readBytes("C:/metadata/metadata-extractor-master/Tests/Data/withExifAndIptc.jpg.appd"),
	                segmentData.getSegment(JpegSegmentType.APPD));
	        assertNull(segmentData.getSegment(JpegSegmentType.APPD, 1));

	        assertEquals(1, segmentData.getSegmentCount(JpegSegmentType.APPE));
	        assertArrayEquals(
	                FileUtil.readBytes("C:/metadata/metadata-extractor-master/Tests/Data/withExifAndIptc.jpg.appe"),
	                segmentData.getSegment(JpegSegmentType.APPE));
	        assertNull(segmentData.getSegment(JpegSegmentType.APPE, 1));

	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP3));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP4));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP5));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP6));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP7));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP8));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP9));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APPA));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APPB));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APPC));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APPF));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.COM));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.DAC));
	        assertEquals(4, segmentData.getSegmentCount(JpegSegmentType.DHT));
	        assertEquals(2, segmentData.getSegmentCount(JpegSegmentType.DQT));
	        assertEquals(1, segmentData.getSegmentCount(JpegSegmentType.SOF0));

	        assertNull(segmentData.getSegment(JpegSegmentType.APP3, 0));
	        System.out.println("Finished running testReadAllSegments()");
	    }

	    //@Test
	    //try {
	    //	jsrt.testReadSpecificSegments();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testReadSpecificSegments()
	    public void testReadSpecificSegments() throws Exception
	    {
	        JpegSegmentData segmentData = JpegSegmentReader.readSegments(
	                new File("C:/metadata/metadata-extractor-master/Tests/Data/withExifAndIptc.jpg"),
	                Arrays.asList(JpegSegmentType.APP0, JpegSegmentType.APP2));

	        assertEquals(1, segmentData.getSegmentCount(JpegSegmentType.APP0));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP1));
	        assertEquals(1, segmentData.getSegmentCount(JpegSegmentType.APP2));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APPD));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APPE));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP3));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP4));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP5));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP6));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP7));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP8));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP9));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APPA));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APPB));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APPC));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APPF));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.COM));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.DHT));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.SOF0));

	        assertArrayEquals(
	                FileUtil.readBytes("C:/metadata/metadata-extractor-master/Tests/Data/withExifAndIptc.jpg.app0"),
	                segmentData.getSegment(JpegSegmentType.APP0));
	        assertArrayEquals(
	                FileUtil.readBytes("C:/metadata/metadata-extractor-master/Tests/Data/withExifAndIptc.jpg.app2"),
	                segmentData.getSegment(JpegSegmentType.APP2));
	        System.out.println("Finished running testReadSpecificSegments()");
	    }

	    //@Test
	    //try {
	    //	jsrt.testReadDhtSegment();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testReadDhtSegment()
	    public void testReadDhtSegment() throws Exception
	    {
	        JpegSegmentData segmentData = JpegSegmentReader.readSegments(
	            new File("C:/metadata/metadata-extractor-master/Tests/Data/withExifAndIptc.jpg"),
	            Collections.singletonList(JpegSegmentType.DHT));

	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP0));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP1));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP2));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APPD));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APPE));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP3));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP4));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP5));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP6));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP7));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP8));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APP9));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APPA));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APPB));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APPC));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.APPF));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.COM));
	        assertEquals(4, segmentData.getSegmentCount(JpegSegmentType.DHT));
	        assertEquals(0, segmentData.getSegmentCount(JpegSegmentType.SOF0));
	        System.out.println("Finished running testReadDhtSegment()");
	    }

	    //@Test
	    //try {
	    //	jsrt.testLoadJpegWithoutExifDataReturnsNull();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testLoadJpegWithoutExifDataReturnsNull()
	    public void testLoadJpegWithoutExifDataReturnsNull() throws Exception
	    {
	        JpegSegmentData segmentData = JpegSegmentReader.readSegments(new File("C:/metadata/metadata-extractor-master/Tests/Data/noExif.jpg"), null);
	        assertNull(segmentData.getSegment(JpegSegmentType.APP1));
	        System.out.println("Finished running testLoadJpegWithoutExifDataReturnsNull()");
	    }

	    //@Test
	    //try {
	    //	jsrt.testWithNonJpegFile();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testWithNonJpegFile()
	    public void testWithNonJpegFile() throws Exception
	    {
	        try {
	            JpegSegmentReader.readSegments(new File("C:/metadata/metadata-extractor-master/Tests/com/drew/imaging/jpeg/JpegSegmentReaderTest.java"), null);
	            fail("shouldn't be able to construct JpegSegmentReader with non-JPEG file");
	        } catch (JpegProcessingException e) {
	            // expect exception
	        }
	        System.out.println("Finished running testWithNonJpegFile()");
	    }
	}

	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class JpegMetadataReaderTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //JpegMetadataReaderTest jdrt = me.new JpegMetadataReaderTest();
	    //@Test
		//try {
	    //	jdrt.testExtractMetadata();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running testExtractMetadata()
	    public void testExtractMetadata() throws Exception
	    {
	    	JpegMetadataReader jr = new JpegMetadataReader();
	        validate(jr.readMetadata(new File("C:/metadata/metadata-extractor-master/Tests/Data/withExif.jpg")));
	        System.out.println("Finished running testExtractMetadata()");
	    }

	    //@Test
	    //try {
	    //	jdrt.testExtractMetadataUsingInputStream();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testExtractMetadataUsingInputStream()
	    public void testExtractMetadataUsingInputStream() throws Exception
	    {
	        FileInputStream stream = new FileInputStream((new File("C:/metadata/metadata-extractor-master/Tests/Data/withExif.jpg")));
	        JpegMetadataReader jr = new JpegMetadataReader();

	        try {
	            validate(jr.readMetadata(stream));
	        } finally {
	            stream.close();
	        }
	        System.out.println("Finished running testExtractMetadataUsingInputStream()");
	    }

	    //@Test
	    /*
	    public void testExtractXmpMetadata() throws Exception
	    {
	        Metadata metadata = JpegMetadataReader.readMetadata(new File("C:/metadata/metadata-extractor-master//Data/withXmp.jpg"));
	        Directory directory = metadata.getFirstDirectoryOfType(XmpDirectory.class);
	        assertNotNull(directory);
	        directory = metadata.getFirstDirectoryOfType(HuffmanTablesDirectory.class);
	        assertNotNull(directory);
	        assertTrue(((HuffmanTablesDirectory) directory).isOptimized());
	    }
	    */

	    //@Test
	    //try {
	    //	jdrt.testTypicalHuffman();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testTypicalHuffman()
	    public void testTypicalHuffman() throws Exception
	    {
	    	JpegMetadataReader jr = new JpegMetadataReader();
	        Metadata metadata = jr.readMetadata(new File("C:/metadata/metadata-extractor-master/Tests/Data/withTypicalHuffman.jpg"));
	        Directory directory = metadata.getFirstHuffmanTablesDirectory();
	        assertNotNull(directory);
	        assertTrue(((HuffmanTablesDirectory) directory).isTypical());
	        assertFalse(((HuffmanTablesDirectory) directory).isOptimized());
	        for (int i = 0; i < ((HuffmanTablesDirectory) directory).getNumberOfTables(); i++) {
	            HuffmanTable table = ((HuffmanTablesDirectory) directory).getTable(i);
	            assertTrue(table.isTypical());
	            assertFalse(table.isOptimized());
	        }
	        System.out.println("Finished running testTypicalHuffman()");
	    }

	    private void validate(Metadata metadata)
	    {
	        Directory directory = metadata.getFirstExifSubIFDDirectory();
	        assertNotNull(directory);
	        assertEquals("80", directory.getString(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT));
	        directory = metadata.getFirstHuffmanTablesDirectory();
	        assertNotNull(directory);
	        assertTrue(((HuffmanTablesDirectory) directory).isOptimized());
	    }
	}
	
	//@SuppressWarnings("ConstantConditions")
	public class IccReaderTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //IccReaderTest irt = me.new IccReaderTest();
	    // TODO add a test with well-formed ICC data and assert output values are correct

	    //@Test
		//try {
	    //	irt.testExtract_InvalidData();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running testExtract_InvalidData()
	    public void testExtract_InvalidData() throws Exception
	    {
	        byte[] app2Bytes = FileUtil.readBytes("C:/metadata/metadata-extractor-master/Tests/Data/iccDataInvalid1.jpg.app2");

	        // When in an APP2 segment, ICC data starts after a 14-byte preamble
	        TestHelper th = new TestHelper();
	        byte[] icc = th.skipBytes(app2Bytes, 14);

	        Metadata metadata = new Metadata();
	        new IccReader().extract(new ByteArrayReader(icc), metadata);

	        IccDirectory directory = metadata.getFirstDirectoryOfType(IccDirectory.class);

	        assertNotNull(directory);
	        assertTrue(directory.hasErrors());
	        System.out.println("Finished running testExtract_InvalidData()");
	    }

	    //@Test
	    //try {
	    //	irt.testReadJpegSegments_InvalidData();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testReadJpegSegments_InvalidData()
	    public void testReadJpegSegments_InvalidData() throws Exception
	    {
	        byte[] app2Bytes = FileUtil.readBytes("C:/metadata/metadata-extractor-master/Tests/Data/iccDataInvalid1.jpg.app2");

	        Metadata metadata = new Metadata();
	        new IccReader().readJpegSegments(Arrays.asList(app2Bytes), metadata, JpegSegmentType.APP2);

	        IccDirectory directory = metadata.getFirstDirectoryOfType(IccDirectory.class);

	        assertNotNull(directory);
	        assertTrue(directory.hasErrors());
	        System.out.println("Finished running testReadJpegSegments_InvalidData()");
	    }

	    //@Test
	    //try {
	    //	irt.testExtract_ProfileDateTime();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testExtract_ProfileDateTime()
	    public void testExtract_ProfileDateTime() throws Exception
	    {
	        byte[] app2Bytes = FileUtil.readBytes("C:/metadata/metadata-extractor-master/Tests/Data/withExifAndIptc.jpg.app2");

	        Metadata metadata = new Metadata();
	        new IccReader().readJpegSegments(Arrays.asList(app2Bytes), metadata, JpegSegmentType.APP2);

	        IccDirectory directory = metadata.getFirstDirectoryOfType(IccDirectory.class);

	        assertNotNull(directory);
	        assertEquals("1998:02:09 06:49:00", directory.getString(IccDirectory.TAG_PROFILE_DATETIME));
	        assertEquals(887006940000L, directory.getDate(IccDirectory.TAG_PROFILE_DATETIME).getTime());
	        System.out.println("Finished running testExtract_ProfileDateTime()");
	    }
	}
	
	public class TestHelper
	{
	    public byte[] skipBytes(byte[] input, int countToSkip)
	    {
	        if (input.length - countToSkip < 0) {
	            throw new IllegalArgumentException("Attempting to skip more bytes than exist in the array.");
	        }

	        byte[] output = new byte[input.length - countToSkip];
	        System.arraycopy(input, countToSkip, output, 0, input.length - countToSkip);
	        return output;
	    }
	}

	//@SuppressWarnings("ConstantConditions")
	public class IptcDirectoryTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //IptcDirectoryTest idt = me.new IptcDirectoryTest();
		//try {
		//	idt.setUp();
		//}
		//catch(Exception e) {
		//    e.printStackTrace();
		//}
	    private IptcDirectory _directory;

	    //@Before
	    public void setUp()
	    {
	        _directory = new IptcDirectory();
	    }

	    //@Test
	    //try {
	    //	idt.testGetDateSent();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetDateSent()
	    public void testGetDateSent()
	    {
	        _directory.setString(IptcDirectory.TAG_DATE_SENT, "20101212");
	        _directory.setString(IptcDirectory.TAG_TIME_SENT, "124135+0100");
	        final Date actual = _directory.getDateSent();

	        Calendar calendar = new GregorianCalendar(2010, 12 - 1, 12, 12, 41, 35);
	        calendar.setTimeZone(TimeZone.getTimeZone("GMT+1"));
	        assertEquals(calendar.getTime(), actual);
	        assertEquals(1292154095000L, actual.getTime());
	        System.out.println("Finished running testGetDateSent()");
	    }

	    //@Test
	    //try {
	    //	idt.testGetReleaseDate();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetReleaseDate()
	    public void testGetReleaseDate()
	    {
	        _directory.setString(IptcDirectory.TAG_RELEASE_DATE, "20101212");
	        _directory.setString(IptcDirectory.TAG_RELEASE_TIME, "124135+0100");
	        final Date actual = _directory.getReleaseDate();

	        Calendar calendar = new GregorianCalendar(2010, 12 - 1, 12, 12, 41, 35);
	        calendar.setTimeZone(TimeZone.getTimeZone("GMT+1"));
	        assertEquals(calendar.getTime(), actual);
	        assertEquals(1292154095000L, actual.getTime());
	        System.out.println("Finished running testGetReleaseDate()");
	    }

	    //@Test
	    //try {
	    //	idt.testGetExpirationDate();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetExpirationDate()
	    public void testGetExpirationDate()
	    {
	        _directory.setString(IptcDirectory.TAG_EXPIRATION_DATE, "20101212");
	        _directory.setString(IptcDirectory.TAG_EXPIRATION_TIME, "124135+0100");
	        final Date actual = _directory.getExpirationDate();

	        Calendar calendar = new GregorianCalendar(2010, 12 - 1, 12, 12, 41, 35);
	        calendar.setTimeZone(TimeZone.getTimeZone("GMT+1"));
	        assertEquals(calendar.getTime(), actual);
	        assertEquals(1292154095000L, actual.getTime());
	        System.out.println("Finished running testGetExpirationDate()");
	    }

	    //@Test
	    //try {
	    //	idt.testGetDateCreated();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetDateCreated()
	    public void testGetDateCreated()
	    {
	        _directory.setString(IptcDirectory.TAG_DATE_CREATED, "20101212");
	        _directory.setString(IptcDirectory.TAG_TIME_CREATED, "124135+0100");
	        final Date actual = _directory.getDateCreated();

	        Calendar calendar = new GregorianCalendar(2010, 12 - 1, 12, 12, 41, 35);
	        calendar.setTimeZone(TimeZone.getTimeZone("GMT+1"));
	        assertEquals(calendar.getTime(), actual);
	        assertEquals(1292154095000L, actual.getTime());
	        System.out.println("Finished running testGetDateCreated()");
	    }

	    //@Test
	    //try {
	    //	idt.testGetDigitalDateCreated();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetDigitalDateCreated()
	    public void testGetDigitalDateCreated()
	    {
	        _directory.setString(IptcDirectory.TAG_DIGITAL_DATE_CREATED, "20101212");
	        _directory.setString(IptcDirectory.TAG_DIGITAL_TIME_CREATED, "124135+0100");
	        final Date actual = _directory.getDigitalDateCreated();

	        Calendar calendar = new GregorianCalendar(2010, 12 - 1, 12, 12, 41, 35);
	        calendar.setTimeZone(TimeZone.getTimeZone("GMT+1"));
	        assertEquals(calendar.getTime(), actual);
	        assertEquals(1292154095000L, actual.getTime());
	        System.out.println("Finished running testGetDigitalDateCreated()");
	    }
	}
	
	/**
	 * Unit tests for {@link IptcReader}.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("ConstantConditions")
	public class IptcReaderTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //IptcReaderTest irt = me.new IptcReaderTest();
	    @NotNull
	    public IptcDirectory processBytes(@NotNull String filePath) throws IOException
	    {
	        Metadata metadata = new Metadata();
	        byte[] bytes = FileUtil.readBytes(filePath);
	        new IptcReader().extract(new SequentialByteArrayReader(bytes), metadata, bytes.length);
	        IptcDirectory directory = metadata.getFirstDirectoryOfType(IptcDirectory.class);
	        assertNotNull(directory);
	        return directory;
	    }

	    //@Test
	    //try {
	    //	irt.testIptc1BytesFromFile();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testIptc1BytesFromFile()
	    public void testIptc1BytesFromFile() throws Exception
	    {
	        IptcDirectory directory = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/iptc1.jpg.appd");

	        assertFalse(directory.getErrors().toString(), directory.hasErrors());

	        Tag[] tags = directory.getTags().toArray(new Tag[directory.getTagCount()]);
	        assertEquals(16, tags.length);

	        assertEquals(IptcDirectory.TAG_CATEGORY, tags[0].getTagType());
	        assertArrayEquals(new String[] { "Supl. Category2", "Supl. Category1", "Cat" }, directory.getStringArray(tags[0].getTagType()));

	        assertEquals(IptcDirectory.TAG_COPYRIGHT_NOTICE, tags[1].getTagType());
	        assertEquals("Copyright", directory.getString(tags[1].getTagType()));

	        assertEquals(IptcDirectory.TAG_SPECIAL_INSTRUCTIONS, tags[2].getTagType());
	        assertEquals("Special Instr.", directory.getString(tags[2].getTagType()));

	        assertEquals(IptcDirectory.TAG_HEADLINE, tags[3].getTagType());
	        assertEquals("Headline", directory.getString(tags[3].getTagType()));

	        assertEquals(IptcDirectory.TAG_CAPTION_WRITER, tags[4].getTagType());
	        assertEquals("CaptionWriter", directory.getString(tags[4].getTagType()));

	        assertEquals(IptcDirectory.TAG_CAPTION, tags[5].getTagType());
	        assertEquals("Caption", directory.getString(tags[5].getTagType()));

	        assertEquals(IptcDirectory.TAG_ORIGINAL_TRANSMISSION_REFERENCE, tags[6].getTagType());
	        assertEquals("Transmission", directory.getString(tags[6].getTagType()));

	        assertEquals(IptcDirectory.TAG_COUNTRY_OR_PRIMARY_LOCATION_NAME, tags[7].getTagType());
	        assertEquals("Country", directory.getString(tags[7].getTagType()));

	        assertEquals(IptcDirectory.TAG_PROVINCE_OR_STATE, tags[8].getTagType());
	        assertEquals("State", directory.getString(tags[8].getTagType()));

	        assertEquals(IptcDirectory.TAG_CITY, tags[9].getTagType());
	        assertEquals("City", directory.getString(tags[9].getTagType()));

	        assertEquals(IptcDirectory.TAG_DATE_CREATED, tags[10].getTagType());
	        assertEquals("20000101", directory.getString(tags[10].getTagType()));

	        assertEquals(IptcDirectory.TAG_OBJECT_NAME, tags[11].getTagType());
	        assertEquals("ObjectName", directory.getString(tags[11].getTagType()));

	        assertEquals(IptcDirectory.TAG_SOURCE, tags[12].getTagType());
	        assertEquals("Source", directory.getString(tags[12].getTagType()));

	        assertEquals(IptcDirectory.TAG_CREDIT, tags[13].getTagType());
	        assertEquals("Credits", directory.getString(tags[13].getTagType()));

	        assertEquals(IptcDirectory.TAG_BY_LINE_TITLE, tags[14].getTagType());
	        assertEquals("BylineTitle", directory.getString(tags[14].getTagType()));

	        assertEquals(IptcDirectory.TAG_BY_LINE, tags[15].getTagType());
	        assertEquals("Byline", directory.getString(tags[15].getTagType()));
	        System.out.println("Finished running testIptc1BytesFromFile()");
	    }

	    //@Test
	    //try {
	    //	irt.testIptc2Photoshop6BytesFromFile() ;
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testIptc2Photoshop6BytesFromFile() 
	    public void testIptc2Photoshop6BytesFromFile() throws Exception
	    {
	        IptcDirectory directory = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/iptc2-photoshop6.jpg.appd");

	        assertFalse(directory.getErrors().toString(), directory.hasErrors());

	        Tag[] tags = directory.getTags().toArray(new Tag[directory.getTagCount()]);
	        assertEquals(17, tags.length);

	        assertEquals(IptcDirectory.TAG_APPLICATION_RECORD_VERSION, tags[0].getTagType());
	        assertEquals(2, directory.getObject(tags[0].getTagType()));

	        assertEquals(IptcDirectory.TAG_CAPTION, tags[1].getTagType());
	        assertEquals("Caption PS6", directory.getString(tags[1].getTagType()));

	        assertEquals(IptcDirectory.TAG_CAPTION_WRITER, tags[2].getTagType());
	        assertEquals("CaptionWriter", directory.getString(tags[2].getTagType()));

	        assertEquals(IptcDirectory.TAG_HEADLINE, tags[3].getTagType());
	        assertEquals("Headline", directory.getString(tags[3].getTagType()));

	        assertEquals(IptcDirectory.TAG_SPECIAL_INSTRUCTIONS, tags[4].getTagType());
	        assertEquals("Special Instr.", directory.getString(tags[4].getTagType()));

	        assertEquals(IptcDirectory.TAG_BY_LINE, tags[5].getTagType());
	        assertEquals("Byline", directory.getString(tags[5].getTagType()));

	        assertEquals(IptcDirectory.TAG_BY_LINE_TITLE, tags[6].getTagType());
	        assertEquals("BylineTitle", directory.getString(tags[6].getTagType()));

	        assertEquals(IptcDirectory.TAG_CREDIT, tags[7].getTagType());
	        assertEquals("Credits", directory.getString(tags[7].getTagType()));

	        assertEquals(IptcDirectory.TAG_SOURCE, tags[8].getTagType());
	        assertEquals("Source", directory.getString(tags[8].getTagType()));

	        assertEquals(IptcDirectory.TAG_OBJECT_NAME, tags[9].getTagType());
	        assertEquals("ObjectName", directory.getString(tags[9].getTagType()));

	        assertEquals(IptcDirectory.TAG_CITY, tags[10].getTagType());
	        assertEquals("City", directory.getString(tags[10].getTagType()));

	        assertEquals(IptcDirectory.TAG_PROVINCE_OR_STATE, tags[11].getTagType());
	        assertEquals("State", directory.getString(tags[11].getTagType()));

	        assertEquals(IptcDirectory.TAG_COUNTRY_OR_PRIMARY_LOCATION_NAME, tags[12].getTagType());
	        assertEquals("Country", directory.getString(tags[12].getTagType()));

	        assertEquals(IptcDirectory.TAG_ORIGINAL_TRANSMISSION_REFERENCE, tags[13].getTagType());
	        assertEquals("Transmission", directory.getString(tags[13].getTagType()));

	        assertEquals(IptcDirectory.TAG_CATEGORY, tags[14].getTagType());
	        assertEquals("Cat", directory.getString(tags[14].getTagType()));

	        assertEquals(IptcDirectory.TAG_SUPPLEMENTAL_CATEGORIES, tags[15].getTagType());
	        assertArrayEquals(new String[] { "Supl. Category1", "Supl. Category2" }, directory.getStringArray(tags[15].getTagType()));

	        assertEquals(IptcDirectory.TAG_COPYRIGHT_NOTICE, tags[16].getTagType());
	        assertEquals("Copyright", directory.getString(tags[16].getTagType()));
	        System.out.println("Finished running testIptc2Photoshop6BytesFromFile() ");
	    }

	    //@Test
	    //try {
	    //	irt.testIptcEncodingUtf8();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //org.junit.ComparisonFailure: expected:<...Umlaute enthalten, n[Ã¤mlich Ã¶fter als Ã¼blich: Ã„Ã–ÃœÃ¤Ã¶Ã¼ÃŸ]
	    //> but was:<...Umlaute enthalten, n[ämlich öfter als üblich: ÄÖÜäöüß]
	    public void testIptcEncodingUtf8() throws Exception
	    {
	        IptcDirectory directory = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/iptc-encoding-defined-utf8.bytes");

	        assertFalse(directory.getErrors().toString(), directory.hasErrors());

	        Tag[] tags = directory.getTags().toArray(new Tag[directory.getTagCount()]);
	        assertEquals(4, tags.length);

	        assertEquals(IptcDirectory.TAG_ENVELOPE_RECORD_VERSION, tags[0].getTagType());
	        assertEquals(2, directory.getObject(tags[0].getTagType()));

	        assertEquals(IptcDirectory.TAG_CODED_CHARACTER_SET, tags[1].getTagType());
	        assertEquals("UTF-8", directory.getObject(tags[1].getTagType()));

	        assertEquals(IptcDirectory.TAG_APPLICATION_RECORD_VERSION, tags[2].getTagType());
	        assertEquals(2, directory.getObject(tags[2].getTagType()));

	        assertEquals(IptcDirectory.TAG_CAPTION, tags[3].getTagType());
	        assertEquals("In diesem Text sind Umlaute enthalten, nÃ¤mlich Ã¶fter als Ã¼blich: Ã„Ã–ÃœÃ¤Ã¶Ã¼ÃŸ\r", directory.getStringValue(tags[3].getTagType()).toString());
	        System.out.println("Finished running testIptcEncodingUtf8()");
	    }

	    //@Test
	    //try {
	    //	irt.testIptcEncodingUndefinedIso();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //org.junit.ComparisonFailure: expected:<...Umlaute enthalten, n[Ã¤mlich Ã¶fter als Ã¼blich: Ã„Ã–ÃœÃ¤Ã¶Ã¼ÃŸ]
	    //> but was:<...Umlaute enthalten, n[ämlich öfter als üblich: ÄÖÜäöüß]
	    public void testIptcEncodingUndefinedIso() throws Exception
	    {
	        IptcDirectory directory = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/iptc-encoding-undefined-iso.bytes");

	        assertFalse(directory.getErrors().toString(), directory.hasErrors());

	        Tag[] tags = directory.getTags().toArray(new Tag[directory.getTagCount()]);
	        assertEquals(3, tags.length);

	        assertEquals(IptcDirectory.TAG_ENVELOPE_RECORD_VERSION, tags[0].getTagType());
	        assertEquals(2, directory.getObject(tags[0].getTagType()));

	        assertEquals(IptcDirectory.TAG_APPLICATION_RECORD_VERSION, tags[1].getTagType());
	        assertEquals(2, directory.getObject(tags[1].getTagType()));

	        assertEquals(IptcDirectory.TAG_CAPTION, tags[2].getTagType());
	        assertEquals("In diesem Text sind Umlaute enthalten, nÃ¤mlich Ã¶fter als Ã¼blich: Ã„Ã–ÃœÃ¤Ã¶Ã¼ÃŸ\r", directory.getStringValue(tags[2].getTagType()).toString());
	        System.out.println("Finished running testIptcEncodingUndefinedIso()");
	    }

	    //@Test
	    //try {
	    //	irt.testIptcEncodingUnknown();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //org.junit.ComparisonFailure: expected:<...cht deklariert und l[Ã¤]sst sich nur schwer ...> but was:<...cht deklariert und l[ä]sst sich nur schwer ...>
	    public void testIptcEncodingUnknown() throws Exception
	    {
	        IptcDirectory directory = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/iptc-encoding-unknown.bytes");

	        assertFalse(directory.getErrors().toString(), directory.hasErrors());

	        Tag[] tags = directory.getTags().toArray(new Tag[directory.getTagCount()]);
	        assertEquals(3, tags.length);

	        assertEquals(IptcDirectory.TAG_APPLICATION_RECORD_VERSION, tags[0].getTagType());
	        assertEquals(2, directory.getObject(tags[0].getTagType()));

	        assertEquals(IptcDirectory.TAG_CAPTION, tags[1].getTagType());
	        assertEquals("Das Encoding dieser Metadaten ist nicht deklariert und lÃ¤sst sich nur schwer erkennen.", directory.getStringValue(tags[1].getTagType()).toString());

	        assertEquals(IptcDirectory.TAG_KEYWORDS, tags[2].getTagType());
	        assertArrayEquals(new String[]{"hÃ¤ufig", "Ã¼blich", "LÃ¶sung", "SpaÃŸ"}, directory.getStringArray(tags[2].getTagType()));
	        System.out.println("Finished running testIptcEncodingUnknown()");
	    }
	    

	    //@Test
	    //try {
	    //	irt.testIptcEncodingUnknown2();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testIptcEncodingUnknown2()
	    public void testIptcEncodingUnknown2() throws Exception
	    {
	        // This metadata has an encoding of three characters [ \ESC '%' '5' ]
	        // It's not clear what to do with this, so it should be ignored.
	        // Version 2.7.0 tripped up on this and threw an exception.
	        IptcDirectory directory = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/iptc-encoding-unknown-2.bytes");

	        assertFalse(directory.getErrors().toString(), directory.hasErrors());

	        Tag[] tags = directory.getTags().toArray(new Tag[directory.getTagCount()]);
	        assertEquals(37, tags.length);

	        assertEquals("MEDWAS,MEDLON,MEDTOR,RONL,ASIA,AONL,APC,USA,CAN,SAM,BIZ", directory.getString(IptcDirectory.TAG_DESTINATION));
	        System.out.println("Finished running testIptcEncodingUnknown2()");
	    }
	}

	public class Iso2022ConverterTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //Iso2022ConverterTest ict = me.new Iso2022ConverterTest();
	    //@Test
		//try {
	    //	ict.testConvertISO2022CharsetToJavaCharset();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running testConvertISO2022CharsetToJavaCharset()
	    public void testConvertISO2022CharsetToJavaCharset() throws Exception
	    {
	    	Iso2022Converter iso = new Iso2022Converter();
	        assertEquals("UTF-8", iso.convertISO2022CharsetToJavaCharset(new byte[]{0x1B, 0x25, 0x47}));
	        assertEquals("ISO-8859-1", iso.convertISO2022CharsetToJavaCharset(new byte[]{0x1B, 0x2E, 0x41}));
	        assertEquals("ISO-8859-1", iso.convertISO2022CharsetToJavaCharset(new byte[]{0x1B, (byte)0xE2, (byte)0x80, (byte)0xA2, 0x41}));
	        System.out.println("Finished running testConvertISO2022CharsetToJavaCharset()");
	    }
	}

	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class JfifReaderTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //JfifReaderTest jrt = me.new JfifReaderTest();
	    //@Test 
		//try {
	    //	jrt.testRead();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running testRead()
	    public void testRead() throws Exception
	    {
	        final byte[] jfifData = new byte[] {
	            74,70,73,70,0,
	            1,2,
	            1,
	            0,108,
	            0,108,
	            0,0
	        };

	        final Metadata metadata = new Metadata();
	        final JfifReader reader = new JfifReader();
	        reader.extract(new ByteArrayReader(jfifData), metadata);

	        assertEquals(1, metadata.getDirectoryCount());
	        JfifDirectory directory = metadata.getFirstDirectoryOfType(JfifDirectory.class);
	        assertNotNull(directory);
	        assertFalse(directory.getErrors().toString(), directory.hasErrors());

	        Tag[] tags = directory.getTags().toArray(new Tag[directory.getTagCount()]);
	        assertEquals(6, tags.length);

	        assertEquals(JfifDirectory.TAG_VERSION, tags[0].getTagType());
	        assertEquals(0x0102, directory.getInt(tags[0].getTagType()));

	        assertEquals(JfifDirectory.TAG_UNITS, tags[1].getTagType());
	        assertEquals(1, directory.getInt(tags[1].getTagType()));

	        assertEquals(JfifDirectory.TAG_RESX, tags[2].getTagType());
	        assertEquals(108, directory.getInt(tags[2].getTagType()));

	        assertEquals(JfifDirectory.TAG_RESY, tags[3].getTagType());
	        assertEquals(108, directory.getInt(tags[3].getTagType()));

	        assertEquals(JfifDirectory.TAG_THUMB_WIDTH, tags[4].getTagType());
	        assertEquals(0, directory.getInt(tags[4].getTagType()));

	        assertEquals(JfifDirectory.TAG_THUMB_HEIGHT, tags[5].getTagType());
	        assertEquals(0, directory.getInt(tags[5].getTagType()));
	        System.out.println("Finished running testRead()");
	    }
	}
	
	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class AdobeJpegReaderTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //AdobeJpegReaderTest jrt = me.new AdobeJpegReaderTest();
	    @NotNull
	    public AdobeJpegDirectory processBytes(@NotNull String filePath) throws IOException
	    {
	        Metadata metadata = new Metadata();
	        new AdobeJpegReader().extract(new SequentialByteArrayReader(FileUtil.readBytes(filePath)), metadata);

	        AdobeJpegDirectory directory = metadata.getFirstDirectoryOfType(AdobeJpegDirectory.class);
	        assertNotNull(directory);
	        return directory;
	    }

	    //@Test
	    //try {
	    //	jrt.testSegmentTypes();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testSegmentTypes()
	    public void testSegmentTypes() throws Exception
	    {
	        AdobeJpegReader reader = new AdobeJpegReader();
	        Iterables it = new Iterables();

	        assertEquals(1, it.toList(reader.getSegmentTypes()).size());
	        assertEquals(JpegSegmentType.APPE, it.toList(reader.getSegmentTypes()).get(0));
	        System.out.println("Finished running testSegmentTypes()");
	    }

	    //@Test
	    //try {
	    //	jrt.testReadAdobeJpegMetadata1();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testReadAdobeJpegMetadata1()

	    public void testReadAdobeJpegMetadata1() throws Exception
	    {
	        AdobeJpegDirectory directory = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/adobeJpeg1.jpg.appe");

	        assertFalse(directory.getErrors().toString(), directory.hasErrors());

	        assertEquals(4, directory.getTagCount());

	        assertEquals(1, directory.getInt(AdobeJpegDirectory.TAG_COLOR_TRANSFORM));
	        assertEquals(25600, directory.getInt(AdobeJpegDirectory.TAG_DCT_ENCODE_VERSION));
	        assertEquals(128, directory.getInt(AdobeJpegDirectory.TAG_APP14_FLAGS0));
	        assertEquals(0, directory.getInt(AdobeJpegDirectory.TAG_APP14_FLAGS1));
	        System.out.println("Finished running testReadAdobeJpegMetadata1()");
	    }
	}

	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class BmpReaderTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //BmpReaderTest br = me.new BmpReaderTest();
	    @NotNull
	    public BmpHeaderDirectory processBytes(@NotNull String file) throws Exception
	    {
	        Metadata metadata = new Metadata();
	        InputStream stream = new FileInputStream(file);
	        new BmpReader().extract(new StreamReader(stream), metadata);
	        stream.close();

	        BmpHeaderDirectory directory = metadata.getFirstDirectoryOfType(BmpHeaderDirectory.class);
	        assertNotNull(directory);
	        return directory;
	    }

	    //@Test
	    //try {
	    //	br.testMsPaint16color();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testMsPaint16color()
	    public void testMsPaint16color() throws Exception
	    {
	        BmpHeaderDirectory directory = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/16color-10x10.bmp");

	        assertFalse(directory.hasErrors());

	        assertEquals(10, directory.getInt(BmpHeaderDirectory.TAG_IMAGE_WIDTH));
	        assertEquals(10, directory.getInt(BmpHeaderDirectory.TAG_IMAGE_HEIGHT));
	        assertEquals(4, directory.getInt(BmpHeaderDirectory.TAG_BITS_PER_PIXEL));
	        assertEquals("None", directory.getDescription(BmpHeaderDirectory.TAG_COMPRESSION));
	        assertEquals(0, directory.getInt(BmpHeaderDirectory.TAG_X_PIXELS_PER_METER));
	        assertEquals(0, directory.getInt(BmpHeaderDirectory.TAG_Y_PIXELS_PER_METER));
	        assertEquals(0, directory.getInt(BmpHeaderDirectory.TAG_PALETTE_COLOUR_COUNT));
	        assertEquals(0, directory.getInt(BmpHeaderDirectory.TAG_IMPORTANT_COLOUR_COUNT));
	        assertEquals(1, directory.getInt(BmpHeaderDirectory.TAG_COLOUR_PLANES));
	        assertEquals(40, directory.getInt(BmpHeaderDirectory.TAG_HEADER_SIZE));
	        System.out.println("Finished running testMsPaint16color()");
	    }

	    //@Test
	    //try {
	    //	br.testMsPaint24bpp();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testMsPaint24bpp()
	    public void testMsPaint24bpp() throws Exception
	    {
	        BmpHeaderDirectory directory = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/24bpp-10x10.bmp");

	        assertFalse(directory.hasErrors());

	        assertEquals(10, directory.getInt(BmpHeaderDirectory.TAG_IMAGE_WIDTH));
	        assertEquals(10, directory.getInt(BmpHeaderDirectory.TAG_IMAGE_HEIGHT));
	        assertEquals(24, directory.getInt(BmpHeaderDirectory.TAG_BITS_PER_PIXEL));
	        assertEquals("None", directory.getDescription(BmpHeaderDirectory.TAG_COMPRESSION));
	        assertEquals(0, directory.getInt(BmpHeaderDirectory.TAG_X_PIXELS_PER_METER));
	        assertEquals(0, directory.getInt(BmpHeaderDirectory.TAG_Y_PIXELS_PER_METER));
	        assertEquals(0, directory.getInt(BmpHeaderDirectory.TAG_PALETTE_COLOUR_COUNT));
	        assertEquals(0, directory.getInt(BmpHeaderDirectory.TAG_IMPORTANT_COLOUR_COUNT));
	        assertEquals(1, directory.getInt(BmpHeaderDirectory.TAG_COLOUR_PLANES));
	        assertEquals(40, directory.getInt(BmpHeaderDirectory.TAG_HEADER_SIZE));
	        System.out.println("Finished running testMsPaint24bpp()");
	    }
	}
	
	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class PngChunkReaderTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //PngChunkReaderTest pr = me.new PngChunkReaderTest();
	    public List<PngChunk> processFile(String filePath) throws PngProcessingException, IOException
	    {
	        FileInputStream inputStream = null;
	        try {
	            inputStream = new FileInputStream(filePath);
	            Iterables it = new Iterables();
	            return it.toList(new PngChunkReader().extract(new StreamReader(inputStream), null));
	        } finally {
	            if (inputStream != null) {
	                inputStream.close();
	            }
	        }
	    }

	    //@Test
	    //try {
	    //	pr.testExtractMspaint();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testExtractMspaint()
	    public void testExtractMspaint() throws Exception
	    {
	        List<PngChunk> chunks = processFile("C:/metadata/metadata-extractor-master/Tests/Data/mspaint-8x10.png");

	        assertEquals(6, chunks.size());

	        PngChunkType ihdr = new PngChunkType("IHDR");
	        assertEquals(ihdr, chunks.get(0).getType());
	        assertEquals(13, chunks.get(0).getBytes().length);

	        PngChunkType srgb = new PngChunkType("sRGB");
	        assertEquals(srgb, chunks.get(1).getType());
	        assertEquals(1, chunks.get(1).getBytes().length);

	        PngChunkType gama = new PngChunkType("gAMA");
	        assertEquals(gama, chunks.get(2).getType());
	        assertEquals(4, chunks.get(2).getBytes().length);

	        PngChunkType phys = new PngChunkType("pHYs");
	        assertEquals(phys, chunks.get(3).getType());
	        assertEquals(9, chunks.get(3).getBytes().length);

	        PngChunkType idat = new PngChunkType("IDAT", true);
	        assertEquals(idat, chunks.get(4).getType());
	        assertEquals(17, chunks.get(4).getBytes().length);

	        PngChunkType iend = new PngChunkType("IEND");
	        assertEquals(iend, chunks.get(5).getType());
	        assertEquals(0, chunks.get(5).getBytes().length);
	        System.out.println("Finished running testExtractMspaint()");
	    }

	    //@Test
	    //try {
	    //	pr.testExtractPhotoshop();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testExtractPhotoshop()
	    public void testExtractPhotoshop() throws Exception
	    {
	        List<PngChunk> chunks = processFile("C:/metadata/metadata-extractor-master/Tests/Data/photoshop-8x12-rgba32.png");

	        assertEquals(5, chunks.size());

	        PngChunkType ihdr = new PngChunkType("IHDR");
	        assertEquals(ihdr, chunks.get(0).getType());
	        assertEquals(13, chunks.get(0).getBytes().length);

	        PngChunkType text = new PngChunkType("tEXt", true);
	        assertEquals(text, chunks.get(1).getType());
	        assertEquals(25, chunks.get(1).getBytes().length);

	        PngChunkType itxt = new PngChunkType("iTXt", true);
	        assertEquals(itxt, chunks.get(2).getType());
	        assertEquals(802, chunks.get(2).getBytes().length);

	        PngChunkType idat = new PngChunkType("IDAT", true);
	        assertEquals(idat, chunks.get(3).getType());
	        assertEquals(130, chunks.get(3).getBytes().length);

	        PngChunkType iend = new PngChunkType("IEND");
	        assertEquals(iend, chunks.get(4).getType());
	        assertEquals(0, chunks.get(4).getBytes().length);
	        System.out.println("Finished running testExtractPhotoshop()");
	    }
	}
	
	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class PngChunkTypeTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //PngChunkTypeTest pt = me.new PngChunkTypeTest();
	    //@Test
	    //try {
	    //	pt.testConstructorTooLong();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running testConstructorTooLong()
	    public void testConstructorTooLong() throws Exception
	    {
	        try {
	            new PngChunkType("TooLong");
	            fail("Expecting exception");
	        } catch (PngProcessingException ex) {
	            assertEquals("PNG chunk type identifier must be four bytes in length", ex.getMessage());
	        }
	        System.out.println("Finished running testConstructorTooLong()");
	    }

	    //@Test
	    //try {
	    //	pt.testConstructorTooShort();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testConstructorTooShort()
	    public void testConstructorTooShort() throws Exception
	    {
	        try {
	            new PngChunkType("foo");
	            fail("Expecting exception");
	        } catch (PngProcessingException ex) {
	            assertEquals("PNG chunk type identifier must be four bytes in length", ex.getMessage());
	        }
	        System.out.println("Finished running testConstructorTooShort()");
	    }

	    //@Test
	    //try {
	    //	pt.testConstructorInvalidBytes();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //org.junit.ComparisonFailure: expected:<...nk type identifier m[ay only contain alphabet characters]> 
	    //but was:<...nk type identifier m[ust be four bytes in length]>
	    public void testConstructorInvalidBytes() throws Exception
	    {
	        String[] invalidStrings = {"ABC1", "1234", "    ", "!Â£$%"};

	        for (String invalidString : invalidStrings) {
	            try {
	                new PngChunkType(invalidString);
	                fail("Expecting exception");
	            } catch (PngProcessingException ex) {
	                assertEquals("PNG chunk type identifier may only contain alphabet characters", ex.getMessage());
	            }
	        }
	        System.out.println("Finished running testConstructorInvalidBytes()");
	    }

	    //@Test
	    //try {
	    //	pt.testConstructorValidBytes();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testConstructorValidBytes()
	    public void testConstructorValidBytes() throws Exception
	    {
	        String[] validStrings = {"ABCD", "abcd", "wxyz", "WXYZ", "lkjh", "LKJH"};

	        for (String validString : validStrings) {
	            new PngChunkType(validString);
	        }
	        System.out.println("Finished running testConstructorValidBytes()");
	    }

	    //@Test
	    //try {
	    //	pt.testIsCritical();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testIsCritical()
	    public void testIsCritical() throws Exception
	    {
	        assertTrue(new PngChunkType("ABCD").isCritical());
	        assertFalse(new PngChunkType("aBCD").isCritical());
	        System.out.println("Finished running testIsCritical()");
	    }

	    //@Test
	    //try {
	    //	pt.testIsAncillary();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testIsAncillary()
	    public void testIsAncillary() throws Exception
	    {
	        assertFalse(new PngChunkType("ABCD").isAncillary());
	        assertTrue(new PngChunkType("aBCD").isAncillary());
	        System.out.println("Finished running testIsAncillary()");
	    }

	    //@Test
	    //try {
	    //	pt.testIsPrivate();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testIsPrivate()
	    public void testIsPrivate() throws Exception
	    {
	        assertTrue(new PngChunkType("ABCD").isPrivate());
	        assertFalse(new PngChunkType("AbCD").isPrivate());
	        System.out.println("Finished running testIsPrivate()");
	    }

	    //@Test
	    //try {
	    //	pt.testIsSafeToCopy();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testIsSafeToCopy()
	    public void testIsSafeToCopy() throws Exception
	    {
	        assertFalse(new PngChunkType("ABCD").isSafeToCopy());
	        assertTrue(new PngChunkType("ABCd").isSafeToCopy());
	        System.out.println("Finished running testIsSafeToCopy()");
	    }

	    //@Test
	    //try {
	    //	pt.testAreMultipleAllowed();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testAreMultipleAllowed()
	    public void testAreMultipleAllowed() throws Exception
	    {
	        assertFalse(new PngChunkType("ABCD").areMultipleAllowed());
	        assertFalse(new PngChunkType("ABCD", false).areMultipleAllowed());
	        assertTrue(new PngChunkType("ABCD", true).areMultipleAllowed());
	        System.out.println("Finished running testAreMultipleAllowed()");
	    }

	    //@Test
	    //try {
	    //	pt.testEquality();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testEquality()
	    public void testEquality() throws Exception
	    {
	        assertEquals(new PngChunkType("ABCD"), new PngChunkType("ABCD"));
	        assertEquals(new PngChunkType("ABCD", true), new PngChunkType("ABCD", true));
	        assertEquals(new PngChunkType("ABCD", false), new PngChunkType("ABCD", false));
	        // NOTE we don't consider the 'allowMultiples' value in the equality test (or hash code)
	        assertEquals(new PngChunkType("ABCD", true), new PngChunkType("ABCD", false));

	        //assertNotEquals(new PngChunkType("ABCD"), new PngChunkType("abcd"));
	        System.out.println("Finished running testEquality()");
	    }
	}

	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class PngMetadataReaderTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //PngMetadataReaderTest pr = me.new PngMetadataReaderTest();
	    @NotNull
	    private Metadata processFile(@NotNull String filePath) throws PngProcessingException, IOException
	    {
	        FileInputStream inputStream = null;
	        try {
	            inputStream = new FileInputStream(filePath);
	            PngMetadataReader pr = new PngMetadataReader();
	            return pr.readMetadata(inputStream);
	        } finally {
	            if (inputStream != null) {
	                inputStream.close();
	            }
	        }
	    }

	    //@Test
	    //try {
	    //	pr.testGimpGreyscaleWithManyChunks();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGimpGreyscaleWithManyChunks()
	    public void testGimpGreyscaleWithManyChunks() throws Exception
	    {
	        TimeZone timeZone = TimeZone.getDefault();

	        try {
	            TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

	            Metadata metadata = processFile("C:/metadata/metadata-extractor-master/Tests/Data/gimp-8x12-greyscale-alpha-time-background.png");
	            Collection<PngDirectory> directories = metadata.getDirectoriesOfType(PngDirectory.class);

	            assertNotNull(directories);
	            assertEquals(6, directories.size());

	            PngDirectory[] dirs = new PngDirectory[directories.size()];
	            directories.toArray(dirs);

	            PngChunkType ihdr = new PngChunkType("IHDR");
	            assertEquals(ihdr, dirs[0].getPngChunkType());
	            assertEquals(8, dirs[0].getInt(PngDirectory.TAG_IMAGE_WIDTH));
	            assertEquals(12, dirs[0].getInt(PngDirectory.TAG_IMAGE_HEIGHT));
	            assertEquals(8, dirs[0].getInt(PngDirectory.TAG_BITS_PER_SAMPLE));
	            assertEquals(4, dirs[0].getInt(PngDirectory.TAG_COLOR_TYPE));
	            assertEquals(0, dirs[0].getInt(PngDirectory.TAG_COMPRESSION_TYPE));
	            assertEquals(0, dirs[0].getInt(PngDirectory.TAG_FILTER_METHOD));
	            assertEquals(0, dirs[0].getInt(PngDirectory.TAG_INTERLACE_METHOD));

	            PngChunkType gama = new PngChunkType("gAMA");
	            assertEquals(gama, dirs[1].getPngChunkType());
	            assertEquals(0.45455, dirs[1].getDouble(PngDirectory.TAG_GAMMA), 0.00001);

	            PngChunkType bkgd = new PngChunkType("bKGD");
	            assertEquals(bkgd, dirs[2].getPngChunkType());
	            assertArrayEquals(new byte[]{0, 52}, dirs[2].getByteArray(PngDirectory.TAG_BACKGROUND_COLOR));

	            //noinspection ConstantConditions
	            PngChunkType phys = new PngChunkType("pHYs");
	            assertEquals(phys, dirs[3].getPngChunkType());
	            assertEquals(1, dirs[3].getInt(PngDirectory.TAG_UNIT_SPECIFIER));
	            assertEquals(2835, dirs[3].getInt(PngDirectory.TAG_PIXELS_PER_UNIT_X));
	            assertEquals(2835, dirs[3].getInt(PngDirectory.TAG_PIXELS_PER_UNIT_Y));
              
	            PngChunkType time = new PngChunkType("tIME");
	            assertEquals(time, dirs[4].getPngChunkType());
	            assertEquals("2013:01:01 04:08:30", dirs[4].getString(PngDirectory.TAG_LAST_MODIFICATION_TIME));

	            java.util.Date modTime = dirs[4].getDate(PngDirectory.TAG_LAST_MODIFICATION_TIME);
	            SimpleDateFormat formatter = new SimpleDateFormat("EE MMM DD HH:mm:ss z yyyy", Locale.US);
	            formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
	            assertEquals("Tue Jan 01 04:08:30 GMT 2013", formatter.format(modTime));
	            assertNotNull(modTime);
	            assertEquals(1357013310000L, modTime.getTime());

	            PngChunkType itxt = new PngChunkType("iTXt", true);
	            assertEquals(itxt, dirs[5].getPngChunkType());
	            //@SuppressWarnings("unchecked")
	            List<KeyValuePair> pairs = (List<KeyValuePair>)dirs[5].getObject(PngDirectory.TAG_TEXTUAL_DATA);
	            assertNotNull(pairs);
	            assertEquals(1, pairs.size());
	            assertEquals("Comment", pairs.get(0).getKey().toString());
	            assertEquals("Created with GIMP", pairs.get(0).getValue().toString());
	        } finally {
	            TimeZone.setDefault(timeZone);
	        }
	        System.out.println("Finished running testGimpGreyscaleWithManyChunks()");
	    }
	}

	/**
	 * @author Akihiko Kusanagi
	 */
	public class PngDescriptorTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //PngDescriptorTest pd = me.new PngDescriptorTest();
	    Charsets ch = new Charsets();
		private Charset _latin1Encoding = ch.ISO_8859_1;

	    //@Test
		//try {
	    //	pd.testGetColorTypeDescription();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running testGetColorTypeDescription()
	    public void testGetColorTypeDescription() throws Exception
	    {
	    	PngChunkType ihdr = new PngChunkType("IHDR");
	        PngDirectory directory = new PngDirectory(ihdr);
	        PngDescriptor descriptor = new PngDescriptor(directory);

	        directory.setInt(PngDirectory.TAG_COLOR_TYPE, 6);
	        assertEquals("True Color with Alpha", descriptor.getColorTypeDescription());
	        assertEquals("True Color with Alpha", directory.getDescription(PngDirectory.TAG_COLOR_TYPE));
	        System.out.println("Finished running testGetColorTypeDescription()");
	    }

	     //@Test
	    //try {
	    //	pd.testGetCompressionTypeDescription();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetCompressionTypeDescription()
	    public void testGetCompressionTypeDescription() throws Exception
	    {
	    	PngChunkType ihdr = new PngChunkType("IHDR");
	        PngDirectory directory = new PngDirectory(ihdr);
	        PngDescriptor descriptor = new PngDescriptor(directory);

	        directory.setInt(PngDirectory.TAG_COMPRESSION_TYPE, 0);
	        assertEquals("Deflate", descriptor.getCompressionTypeDescription());
	        assertEquals("Deflate", directory.getDescription(PngDirectory.TAG_COMPRESSION_TYPE));
	        System.out.println("Finished running testGetCompressionTypeDescription()");
	    }

	    //@Test
	    //try {
	    //	pd.testGetFilterMethodDescription();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetFilterMethodDescription()
	    public void testGetFilterMethodDescription() throws Exception
	    {
	    	PngChunkType ihdr = new PngChunkType("IHDR");
	        PngDirectory directory = new PngDirectory(ihdr);
	        PngDescriptor descriptor = new PngDescriptor(directory);

	        directory.setInt(PngDirectory.TAG_FILTER_METHOD, 0);
	        assertEquals("Adaptive", descriptor.getFilterMethodDescription());
	        assertEquals("Adaptive", directory.getDescription(PngDirectory.TAG_FILTER_METHOD));
	        System.out.println("Finished running testGetFilterMethodDescription()");
	    }

	    //@Test
	    //try {
	    //	pd.testGetInterlaceMethodDescription();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetInterlaceMethodDescription()
	    public void testGetInterlaceMethodDescription() throws Exception
	    {
	    	PngChunkType ihdr = new PngChunkType("IHDR");
	        PngDirectory directory = new PngDirectory(ihdr);
	        PngDescriptor descriptor = new PngDescriptor(directory);

	        directory.setInt(PngDirectory.TAG_INTERLACE_METHOD, 1);
	        assertEquals("Adam7 Interlace", descriptor.getInterlaceMethodDescription());
	        assertEquals("Adam7 Interlace", directory.getDescription(PngDirectory.TAG_INTERLACE_METHOD));
	        System.out.println("Finished running testGetInterlaceMethodDescription()");
	    }

	    //@Test
	    //try {
	    //	pd.testGetPaletteHasTransparencyDescription();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetPaletteHasTransparencyDescription()
	    public void testGetPaletteHasTransparencyDescription() throws Exception
	    {
	    	PngChunkType trns = new PngChunkType("tRNS");
	        PngDirectory directory = new PngDirectory(trns);
	        PngDescriptor descriptor = new PngDescriptor(directory);

	        directory.setInt(PngDirectory.TAG_PALETTE_HAS_TRANSPARENCY, 1);
	        assertEquals("Yes", descriptor.getPaletteHasTransparencyDescription());
	        assertEquals("Yes", directory.getDescription(PngDirectory.TAG_PALETTE_HAS_TRANSPARENCY));
	        System.out.println("Finished running testGetPaletteHasTransparencyDescription()");
	    }

	    //@Test
	    //try {
	    //	pd.testGetIsSrgbColorSpaceDescription();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetIsSrgbColorSpaceDescription()
	    public void testGetIsSrgbColorSpaceDescription() throws Exception
	    {
	    	PngChunkType srgb = new PngChunkType("sRGB");
	        PngDirectory directory = new PngDirectory(srgb);
	        PngDescriptor descriptor = new PngDescriptor(directory);

	        directory.setInt(PngDirectory.TAG_SRGB_RENDERING_INTENT, 0);
	        assertEquals("Perceptual", descriptor.getIsSrgbColorSpaceDescription());
	        assertEquals("Perceptual", directory.getDescription(PngDirectory.TAG_SRGB_RENDERING_INTENT));
	        System.out.println("Finished running testGetIsSrgbColorSpaceDescription()");
	    }

	    //@Test
	    //try {
	    //	pd.testGetUnitSpecifierDescription();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetUnitSpecifierDescription()
	    public void testGetUnitSpecifierDescription() throws Exception
	    {
	    	PngChunkType phys = new PngChunkType("pHYs");
	        PngDirectory directory = new PngDirectory(phys);
	        PngDescriptor descriptor = new PngDescriptor(directory);

	        directory.setInt(PngDirectory.TAG_UNIT_SPECIFIER, 1);
	        assertEquals("Metres", descriptor.getUnitSpecifierDescription());
	        assertEquals("Metres", directory.getDescription(PngDirectory.TAG_UNIT_SPECIFIER));
	        System.out.println("Finished running testGetUnitSpecifierDescription()");
	    }

	    //@Test
	    //try {
	    //	pd.testGetTextualDataDescription();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetTextualDataDescription()
	    public void testGetTextualDataDescription() throws Exception
	    {
	        List<KeyValuePair> textPairs = new ArrayList<KeyValuePair>();
	        StringValue value = new StringValue("value".getBytes(_latin1Encoding), _latin1Encoding);
	        textPairs.add(new KeyValuePair("keyword", value));

	        PngChunkType text = new PngChunkType("tEXt", true);
	        PngDirectory directory = new PngDirectory(text);
	        PngDescriptor descriptor = new PngDescriptor(directory);
	        directory.setObject(PngDirectory.TAG_TEXTUAL_DATA, textPairs);
	        assertEquals("keyword: value", descriptor.getTextualDataDescription());
	        assertEquals("keyword: value", directory.getDescription(PngDirectory.TAG_TEXTUAL_DATA));

	        PngChunkType ztxt = new PngChunkType("zTXt", true);
	        directory = new PngDirectory(ztxt);
	        descriptor = new PngDescriptor(directory);
	        directory.setObject(PngDirectory.TAG_TEXTUAL_DATA, textPairs);
	        assertEquals("keyword: value", descriptor.getTextualDataDescription());
	        assertEquals("keyword: value", directory.getDescription(PngDirectory.TAG_TEXTUAL_DATA));

	        PngChunkType itxt = new PngChunkType("iTXt", true);
	        directory = new PngDirectory(itxt);
	        descriptor = new PngDescriptor(directory);
	        directory.setObject(PngDirectory.TAG_TEXTUAL_DATA, textPairs);
	        assertEquals("keyword: value", descriptor.getTextualDataDescription());
	        assertEquals("keyword: value", directory.getDescription(PngDirectory.TAG_TEXTUAL_DATA));
	        System.out.println("Finished running testGetTextualDataDescription()");
	    }

	    //@Test
	    //try {
	    //	pd.testGetBackgroundColorDescription();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetBackgroundColorDescription()
	    public void testGetBackgroundColorDescription() throws Exception
	    {
	    	PngChunkType bkgd = new PngChunkType("bKGD");
	        PngDirectory directory = new PngDirectory(bkgd);
	        PngDescriptor descriptor = new PngDescriptor(directory);

	        directory.setByteArray(PngDirectory.TAG_BACKGROUND_COLOR, new byte[]{52});
	        assertEquals("Palette Index 52", descriptor.getBackgroundColorDescription());
	        assertEquals("Palette Index 52", directory.getDescription(PngDirectory.TAG_BACKGROUND_COLOR));
	        directory.setByteArray(PngDirectory.TAG_BACKGROUND_COLOR, new byte[]{0, 52});
	        assertEquals("Greyscale Level 52", descriptor.getBackgroundColorDescription());
	        assertEquals("Greyscale Level 52", directory.getDescription(PngDirectory.TAG_BACKGROUND_COLOR));
	        directory.setByteArray(PngDirectory.TAG_BACKGROUND_COLOR, new byte[]{0, 50, 0, 51, 0, 52});
	        assertEquals("R 50, G 51, B 52", descriptor.getBackgroundColorDescription());
	        assertEquals("R 50, G 51, B 52", directory.getDescription(PngDirectory.TAG_BACKGROUND_COLOR));
	        System.out.println("Finished running testGetBackgroundColorDescription()");
	    }
	}
	
	public class FileTypeTest
	{
	    //@Test
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //FileTypeTest ft = me.new FileTypeTest();
		//try {
	    //	ft.testExtensions();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running testExtensions()
	    public void testExtensions()
	    {
	        assertEquals("jpg", FileType.Jpeg.getCommonExtension());
	        assertEquals("bmp", FileType.Bmp.getCommonExtension());

	        assertEquals("JPEG", FileType.Jpeg.getName());
	        assertEquals("BMP", FileType.Bmp.getName());

	        assertArrayEquals(new String[]{"jpg", "jpeg", "jpe"}, FileType.Jpeg.getAllExtensions());

	        assertNull(FileType.Unknown.getCommonExtension());
	        System.out.println("Finished running testExtensions()");
	    }
	}
	
	/**
	 * Base class for testing implementations of {@link RandomAccessReader}.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public abstract class RandomAccessTestBase
	{
	    protected abstract RandomAccessReader createReader(byte[] bytes);

	    //@Test
	    //try {
	    //	bt.testDefaultEndianess();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testDefaultEndianness()
	    {
	        assertEquals(true, createReader(new byte[1]).isMotorolaByteOrder());
	        System.out.println("Finished running testDefaultEndianess()");
	    }

	    //@Test
	    //try {
	    //	bt.testGetInt8();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetInt8() throws Exception
	    {
	        byte[] buffer = new byte[]{0x00, 0x01, (byte)0x7F, (byte)0xFF};
	        RandomAccessReader reader = createReader(buffer);

	        assertEquals((byte)0, reader.getInt8(0));
	        assertEquals((byte)1, reader.getInt8(1));
	        assertEquals((byte)127, reader.getInt8(2));
	        assertEquals((byte)255, reader.getInt8(3));
	        System.out.println("Finished running testGetInt8()");
	    }

	    //@Test
	    //try {
	    //	bt.testGetUInt8();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetUInt8() throws Exception
	    {
	        byte[] buffer = new byte[]{0x00, 0x01, (byte)0x7F, (byte)0xFF};
	        RandomAccessReader reader = createReader(buffer);

	        assertEquals(0, reader.getUInt8(0));
	        assertEquals(1, reader.getUInt8(1));
	        assertEquals(127, reader.getUInt8(2));
	        assertEquals(255, reader.getUInt8(3));
	        System.out.println("Finished running testGetUInt8()");
	    }

	    //@Test
	    //try {
	    //	bt.testGetUInt8_OutOfBounds();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetUInt8_OutOfBounds()
	    {
	        try {
	            RandomAccessReader reader = createReader(new byte[2]);
	            reader.getUInt8(2);
	            fail("Exception expected");
	        } catch (IOException ex) {
	            assertEquals("Attempt to read from beyond end of underlying data source (requested index: 2, requested count: 1, max index: 1)", ex.getMessage());
	        }
	        System.out.println("Finished running testGetUInt8_OutOfBounds()");
	    }

	    //@Test
	    //try {
	    //	bt.testGetInt16();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetInt16() throws Exception
	    {
	        assertEquals(-1, createReader(new byte[]{(byte)0xff, (byte)0xff}).getInt16(0));

	        byte[] buffer = new byte[]{0x00, 0x01, (byte)0x7F, (byte)0xFF};
	        RandomAccessReader reader = createReader(buffer);

	        assertEquals((short)0x0001, reader.getInt16(0));
	        assertEquals((short)0x017F, reader.getInt16(1));
	        assertEquals((short)0x7FFF, reader.getInt16(2));

	        reader.setMotorolaByteOrder(false);

	        assertEquals((short)0x0100, reader.getInt16(0));
	        assertEquals((short)0x7F01, reader.getInt16(1));
	        assertEquals((short)0xFF7F, reader.getInt16(2));
	        System.out.println("Finished running testGetInt16()");
	    }

	    //@Test
	    //try {
	    //	bt.testGetUInt16();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetUInt16() throws Exception
	    {
	        byte[] buffer = new byte[]{0x00, 0x01, (byte)0x7F, (byte)0xFF};
	        RandomAccessReader reader = createReader(buffer);

	        assertEquals(0x0001, reader.getUInt16(0));
	        assertEquals(0x017F, reader.getUInt16(1));
	        assertEquals(0x7FFF, reader.getUInt16(2));

	        reader.setMotorolaByteOrder(false);

	        assertEquals(0x0100, reader.getUInt16(0));
	        assertEquals(0x7F01, reader.getUInt16(1));
	        assertEquals(0xFF7F, reader.getUInt16(2));
	        System.out.println("Finished running testGetUInt16()");
	    }

	    //@Test
	    //try {
	    //	bt.testGetUInt16_OutOfBounds();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetUInt16_OutOfBounds()
	    {
	        try {
	            RandomAccessReader reader = createReader(new byte[2]);
	            reader.getUInt16(1);
	            fail("Exception expected");
	        } catch (IOException ex) {
	            assertEquals("Attempt to read from beyond end of underlying data source (requested index: 1, requested count: 2, max index: 1)", ex.getMessage());
	        }
	        System.out.println("Finished running testGetUInt16_OutOfBounds()");
	    }

	    //@Test
	    //try {
	    //	bt.testGetInt32();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetInt32() throws Exception
	    {
	        assertEquals(-1, createReader(new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff}).getInt32(0));

	        byte[] buffer = new byte[]{0x00, 0x01, (byte)0x7F, (byte)0xFF, 0x02, 0x03, 0x04};
	        RandomAccessReader reader = createReader(buffer);

	        assertEquals(0x00017FFF, reader.getInt32(0));
	        assertEquals(0x017FFF02, reader.getInt32(1));
	        assertEquals(0x7FFF0203, reader.getInt32(2));
	        assertEquals(0xFF020304, reader.getInt32(3));

	        reader.setMotorolaByteOrder(false);

	        assertEquals(0xFF7F0100, reader.getInt32(0));
	        assertEquals(0x02FF7F01, reader.getInt32(1));
	        assertEquals(0x0302FF7F, reader.getInt32(2));
	        assertEquals(0x040302FF, reader.getInt32(3));
	        System.out.println("Finished running testGetInt32()");
	    }

	    //@Test
	    //try {
	    //	bt.testGetUInt32();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetUInt32() throws Exception
	    {
	        assertEquals(4294967295L, createReader(new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff}).getUInt32(0));

	        byte[] buffer = new byte[]{0x00, 0x01, (byte)0x7F, (byte)0xFF, 0x02, 0x03, 0x04};
	        RandomAccessReader reader = createReader(buffer);

	        assertEquals(0x00017FFFL, reader.getUInt32(0));
	        assertEquals(0x017FFF02L, reader.getUInt32(1));
	        assertEquals(0x7FFF0203L, reader.getUInt32(2));
	        assertEquals(0xFF020304L, reader.getUInt32(3));

	        reader.setMotorolaByteOrder(false);

	        assertEquals(4286513408L, reader.getUInt32(0));
	        assertEquals(0x02FF7F01L, reader.getUInt32(1));
	        assertEquals(0x0302FF7FL, reader.getUInt32(2));
	        assertEquals(0x040302FFL, reader.getInt32(3));
	        System.out.println("Finished running testGetUInt32()");
	    } 

	    //@Test
	    //try {
	    //	bt.testGetInt32_OutOfBounds();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetInt32_OutOfBounds()
	    {
	        try {
	            RandomAccessReader reader = createReader(new byte[3]);
	            reader.getInt32(0);
	            fail("Exception expected");
	        } catch (IOException ex) {
	            assertEquals("Attempt to read from beyond end of underlying data source (requested index: 0, requested count: 4, max index: 2)", ex.getMessage());
	        }
	        System.out.println("Finished running testGetInt32_OutOfBounds()");
	    }

	    //@Test
	    //try {
	    //	bt.testGetInt64();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetInt64() throws IOException
	    {
	        byte[] buffer = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, (byte)0xFF};
	        RandomAccessReader reader = createReader(buffer);

	        assertEquals(0x0001020304050607L, reader.getInt64(0));
	        assertEquals(0x01020304050607FFL, reader.getInt64(1));

	        reader.setMotorolaByteOrder(false);

	        assertEquals(0x0706050403020100L, reader.getInt64(0));
	        assertEquals(0xFF07060504030201L, reader.getInt64(1));
	        System.out.println("Finished running testGetInt64()");
	    }

	    //@Test
	    //try {
	    //	bt.testGetInt64_OutOfBounds();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetInt64_OutOfBounds() throws Exception
	    {
	        try {
	            RandomAccessReader reader = createReader(new byte[7]);
	            reader.getInt64(0);
	            fail("Exception expected");
	        } catch (IOException ex) {
	            assertEquals("Attempt to read from beyond end of underlying data source (requested index: 0, requested count: 8, max index: 6)", ex.getMessage());
	        }
	        try {
	            RandomAccessReader reader = createReader(new byte[7]);
	            reader.getInt64(-1);
	            fail("Exception expected");
	        } catch (IOException ex) {
	            assertEquals("Attempt to read from buffer using a negative index (-1)", ex.getMessage());
	        }
	        System.out.println("Finished running testGetInt64_OutOfBounds()");
	    }

	    //@Test
	    //try {
	    //	bt.testGetFloat32();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetFloat32() throws Exception
	    {
	        final int nanBits = 0x7fc00000;
	        assertTrue(Float.isNaN(Float.intBitsToFloat(nanBits)));

	        byte[] buffer = new byte[]{0x7f, (byte)0xc0, 0x00, 0x00};
	        RandomAccessReader reader = createReader(buffer);

	        assertTrue(Float.isNaN(reader.getFloat32(0)));
	        System.out.println("Finished running testGetFloat32()");
	    }

	    //@Test
	    //try {
	    //	bt.testGetFloat64();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetFloat64() throws Exception
	    {
	        final long nanBits = 0xfff0000000000001L;
	        assertTrue(Double.isNaN(Double.longBitsToDouble(nanBits)));

	        byte[] buffer = new byte[]{(byte)0xff, (byte)0xf0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01};
	        RandomAccessReader reader = createReader(buffer);

	        assertTrue(Double.isNaN(reader.getDouble64(0)));
	        System.out.println("Finished running testGetFloat64()");
	    }

	    //@Test
	    //try {
	    //	bt.testGetNullTerminatedString();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetNullTerminatedString() throws Exception
	    {
	    	Charsets ch = new Charsets();
	        byte[] bytes = new byte[]{0x41, 0x42, 0x43, 0x44, 0x00, 0x45, 0x46, 0x47};
	        RandomAccessReader reader = createReader(bytes);

	        assertEquals("", reader.getNullTerminatedString(0, 0, ch.UTF_8));
	        assertEquals("A", reader.getNullTerminatedString(0, 1, ch.UTF_8));
	        assertEquals("AB", reader.getNullTerminatedString(0, 2, ch.UTF_8));
	        assertEquals("ABC", reader.getNullTerminatedString(0, 3, ch.UTF_8));
	        assertEquals("ABCD", reader.getNullTerminatedString(0, 4, ch.UTF_8));
	        assertEquals("ABCD", reader.getNullTerminatedString(0, 5, ch.UTF_8));
	        assertEquals("ABCD", reader.getNullTerminatedString(0, 6, ch.UTF_8));

	        assertEquals("BCD", reader.getNullTerminatedString(1, 3, ch.UTF_8));
	        assertEquals("BCD", reader.getNullTerminatedString(1, 4, ch.UTF_8));
	        assertEquals("BCD", reader.getNullTerminatedString(1, 5, ch.UTF_8));

	        assertEquals("", reader.getNullTerminatedString(4, 3, ch.UTF_8));
	        System.out.println("Finished running testGetNullTerminatedString()");
	    }

	    //@Test
	    //try {
	    //	bt.testGetString();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetString() throws Exception
	    {
	    	Charsets ch = new Charsets();
	        byte[] bytes = new byte[]{0x41, 0x42, 0x43, 0x44, 0x00, 0x45, 0x46, 0x47};
	        RandomAccessReader reader = createReader(bytes);

	        assertEquals("", reader.getString(0, 0, ch.UTF_8));
	        assertEquals("A", reader.getString(0, 1, ch.UTF_8));
	        assertEquals("AB", reader.getString(0, 2, ch.UTF_8));
	        assertEquals("ABC", reader.getString(0, 3, ch.UTF_8));
	        assertEquals("ABCD", reader.getString(0, 4, ch.UTF_8));
	        assertEquals("ABCD\0", reader.getString(0, 5, ch.UTF_8));
	        assertEquals("ABCD\0E", reader.getString(0, 6, ch.UTF_8));

	        assertEquals("BCD", reader.getString(1, 3, ch.UTF_8));
	        assertEquals("BCD\0", reader.getString(1, 4, ch.UTF_8));
	        assertEquals("BCD\0E", reader.getString(1, 5, ch.UTF_8));

	        assertEquals("\0EF", reader.getString(4, 3, ch.UTF_8));
	        System.out.println("Finished running testGetString()");
	    }

	    //@Test
	    //try {
	    //	bt.testIndexPlusCountExceedsIntMaxValue();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testIndexPlusCountExceedsIntMaxValue()
	    {
	        RandomAccessReader reader = createReader(new byte[10]);

	        try {
	            reader.getBytes(0x6FFFFFFF, 0x6FFFFFFF);
	        } catch (IOException e) {
	            assertEquals("Number of requested bytes summed with starting index exceed maximum range of signed 32 bit integers (requested index: 1879048191, requested count: 1879048191)", e.getMessage());
	        }
	        System.out.println("Finished running testIndexPlusCountExceedsIntMaxValue()");
	    }

	    //@Test
	    //try {
	    //	bt.testOverflowBoundsCalculation();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testOverflowBoundsCalculation()
	    {
	        RandomAccessReader reader = createReader(new byte[10]);

	        try {
	            reader.getBytes(5, 10);
	        } catch (IOException e) {
	            assertEquals("Attempt to read from beyond end of underlying data source (requested index: 5, requested count: 10, max index: 9)", e.getMessage());
	        }
	        System.out.println("Finished running testOverflowBoundsCalculation()");
	    }

	    //@Test
	    //try {
	    //	bt.testGetBytesEOF();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetBytesEOF() throws Exception
	    {
	        createReader(new byte[50]).getBytes(0, 50);

	        RandomAccessReader reader = createReader(new byte[50]);
	        reader.getBytes(25, 25);

	        try {
	            createReader(new byte[50]).getBytes(0, 51);
	            fail("Expecting exception");
	        } catch (IOException ignored) {}
	        System.out.println("Finished running testGetBytesEOF()");
	    }

	    //@Test
	    //try {
	    //	bt.testGetInt8EOF();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetInt8EOF() throws Exception
	    {
	        createReader(new byte[1]).getInt8(0);

	        RandomAccessReader reader = createReader(new byte[2]);
	        reader.getInt8(0);
	        reader.getInt8(1);

	        try {
	            reader = createReader(new byte[1]);
	            reader.getInt8(0);
	            reader.getInt8(1);
	            fail("Expecting exception");
	        } catch (IOException ignored) {}
	        System.out.println("Finished running testGetInt8EOF()");
	    }
	}


	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class ByteArrayReaderTest extends RandomAccessTestBase
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //ByteArrayReaderTest bt = me.new ByteArrayReaderTest();
		//Finished running testDefaultEndianess()
		//Finished running testGetInt8()
		//Finished running testGetUInt8()
		//Finished running testGetUInt8_OutOfBounds()
		//Finished running testGetInt16()
		//Finished running testGetUInt16()
		//Finished running testGetUInt16_OutOfBounds()
		//Finished running testGetInt32()
		//Finished running testGetUInt32()
		//Finished running testGetInt32_OutOfBounds()
		//Finished running testGetInt64()
		//Finished running testGetInt64_OutOfBounds()
		//Finished running testGetFloat32()
		//Finished running testGetFloat64()
		//Finished running testGetNullTerminatedString()
		//Finished running testGetString()
		//Finished running testIndexPlusCountExceedsIntMaxValue()
		//Finished running testOverflowBoundsCalculation()
		//Finished running testGetBytesEOF()
		//Finished running testGetInt8EOF()
	    @Override
	    protected RandomAccessReader createReader(byte[] bytes)
	    {
	        return new ByteArrayReader(bytes);
	    }

	    //@SuppressWarnings({ "ConstantConditions" })
	    //@Test(expected = NullPointerException.class)
	    //try {
	    //	bt.testConstructWithNullBufferThrows();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //throws NullPointerException as expected
	    public void testConstructWithNullBufferThrows()
	    {
	        new ByteArrayReader(null);
	    }
	}
	
	/**
	 * @author Drew Noakes http://drewnoakes.com
	 */
	public class ByteConvertTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //ByteConvertTest bt = me.new ByteConvertTest();
	    //@Test
		//try {
	    //	bt.toInt32BigEndian();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running toInt32BigEndian()
	    public void toInt32BigEndian()
	    {
	    	ByteConvert bc = new ByteConvert();
	        assertEquals(0x01020304, bc.toInt32BigEndian(new byte[]{1, 2, 3, 4}));
	        assertEquals(0x01020304, bc.toInt32BigEndian(new byte[]{1, 2, 3, 4, 5}));
	        System.out.println("Finished running toInt32BigEndian()");
	    }

	    //@Test
	    //try {
	    //	bt.toInt32LittleEndian();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running toInt32LittleEndian()
	    public void toInt32LittleEndian()
	    {
	    	ByteConvert bc = new ByteConvert();
	        assertEquals(0x04030201, bc.toInt32LittleEndian(new byte[]{1, 2, 3, 4}));
	        assertEquals(0x04030201, bc.toInt32LittleEndian(new byte[]{1, 2, 3, 4, 5}));
	        System.out.println("Finished running toInt32LittleEndian()");
	    }
	}

	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class ByteTrieTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //ByteTrieTest bt = me.new ByteTrieTest();
	    //@Test
		//try {
	    //	bt.testBasics();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running testBasics()
	    public void testBasics()
	    {
	        ByteTrie<String> trie = new ByteTrie<String>();

	        String[] strings = {"HELLO", "HELLO WORLD", "HERBERT"};

	        for (String s : strings)
	            trie.addPath(s, s.getBytes());

	        for (String s : strings)
	            assertSame(s, trie.find(s.getBytes()));

	        assertNull(trie.find("Not Included".getBytes()));
	        assertNull(trie.find("HELL".getBytes()));
	        assertNull(trie.find("H".getBytes()));
	        assertEquals("HELLO", trie.find("HELLO MUM".getBytes()));

	        assertEquals("HELLO WORLD".length(), trie.getMaxDepth());

	        trie.setDefaultValue("DEFAULT");

	        assertEquals("DEFAULT", trie.find("Also Not Included".getBytes()));
	        assertEquals("DEFAULT", trie.find("H".getBytes()));
	        assertEquals("DEFAULT", trie.find("HELL".getBytes()));
	        System.out.println("Finished running testBasics()");
	    }

	    //@Test
	    //try {
	    //	bt.testDisallowsAddingEmptyPath();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testDisallowsAddingEmptyPath()
	    public void testDisallowsAddingEmptyPath()
	    {
	        try {
	            new ByteTrie<String>().addPath("Foo", new byte[0]);
	            fail();
	        } catch (IllegalArgumentException ignored) {
	        }

	        try {
	            new ByteTrie<String>().addPath("Foo", new byte[0], new byte[0], new byte[0]);
	            fail();
	        } catch (IllegalArgumentException ignored) {
	        }
	        System.out.println("Finished running testDisallowsAddingEmptyPath()");
	    }
	}
	
	/**
	 * @author Nadahar
	 */
	public class ByteUtilTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //ByteUtilTest bt = me.new ByteUtilTest();
	    //@Test
		//try {
	    //	bt.testGetInt16();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running testGetInt16()
	    public void testGetInt16() {
	    	ByteUtil bu = new ByteUtil();
	        byte[] buffer = new byte[] {0x7F, (byte) 0xFF};
	        assertEquals(0x7FFF, bu.getInt16(buffer, 0, true));
	        assertEquals(0xFF7F, bu.getInt16(buffer, 0, false));

	        buffer = new byte[] {(byte) 0xFF, (byte) 0xFF};
	        assertEquals(0xFFFF, bu.getInt16(buffer, 0, true));
	        assertEquals(0xFFFF, bu.getInt16(buffer, 0, false));

	        buffer = new byte[] {0x1, 0x0};
	        assertEquals(0x100, bu.getInt16(buffer, 0, true));
	        assertEquals(0x1, bu.getInt16(buffer, 0, false));

	        buffer = new byte[] {0x7F, (byte) 0xFF, 0x7F, (byte) 0xFF, 0x7F, (byte) 0xFF};
	        assertEquals(0xFF7F, bu.getInt16(buffer, 1, true));
	        assertEquals(0x7FFF, bu.getInt16(buffer, 1, false));
	        assertEquals(0x7FFF, bu.getInt16(buffer, 2, true));
	        assertEquals(0xFF7F, bu.getInt16(buffer, 2, false));
	        System.out.println("Finished running testGetInt16()");
	    }

	    //@Test
	    //try {
	    //	bt.testGetInt32();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetInt32()
	    public void testGetInt32() {
	    	ByteUtil bu = new ByteUtil();
	        byte[] buffer = new byte[] {0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
	        assertEquals(0x7FFFFFFF, bu.getInt32(buffer, 0, true));
	        assertEquals(0xFFFFFF7F, bu.getInt32(buffer, 0, false));

	        buffer = new byte[] {(byte) 0xFF, (byte) 0xFF, 0x0, 0x0};
	        assertEquals(0xFFFF0000, bu.getInt32(buffer, 0, true));
	        assertEquals(0xFFFF, bu.getInt32(buffer, 0, false));

	        buffer = new byte[] {0x1, 0x0, 0x1, 0x0};
	        assertEquals(0x1000100, bu.getInt32(buffer, 0, true));
	        assertEquals(0x10001, bu.getInt32(buffer, 0, false));

	        buffer = new byte[] {0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
	        assertEquals(0xFFFFFF7F, bu.getInt32(buffer, 1, true));
	        assertEquals(0x7FFFFFFF, bu.getInt32(buffer, 1, false));
	        assertEquals(0x7FFFFFFF, bu.getInt32(buffer, 4, true));
	        assertEquals(0xFFFFFF7F, bu.getInt32(buffer, 4, false));
	        System.out.println("Finished running testGetInt32()");
	    }

	    //@Test
	    //try {
	    //	bt.testGetInt64();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
        //Finished running testGetInt64()
	    public void testGetInt64() {
	    	ByteUtil bu = new ByteUtil();
	        byte[] buffer = new byte[] {
	            0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
	            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
	        };
	        assertEquals(0x7FFFFFFFFFFFFFFFL, bu.getLong64(buffer, 0, true));
	        assertEquals(0xFFFFFFFFFFFFFF7FL, bu.getLong64(buffer, 0, false));

	        buffer = new byte[] {(byte) 0xFF, (byte) 0xFF, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0};
	        assertEquals(0xFFFF000000000000L, bu.getLong64(buffer, 0, true));
	        assertEquals(0xFFFFL, bu.getLong64(buffer, 0, false));

	        buffer = new byte[] {0x1, 0x0, 0x1, 0x0, 0x1, 0x0, 0x1, 0x0};
	        assertEquals(0x100010001000100L, bu.getLong64(buffer, 0, true));
	        assertEquals(0x1000100010001L, bu.getLong64(buffer, 0, false));

	        buffer = new byte[] {
	            0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
	            0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
	            0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
	            0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
	        };
	        assertEquals(0xFFFFFF7FFFFFFF7FL, bu.getLong64(buffer, 1, true));
	        assertEquals(0x7FFFFFFF7FFFFFFFL, bu.getLong64(buffer, 1, false));
	        assertEquals(0x7FFFFFFF7FFFFFFFL, bu.getLong64(buffer, 8, true));
	        assertEquals(0xFFFFFF7FFFFFFF7FL, bu.getLong64(buffer, 8, false));
	        System.out.println("Finished running testGetInt64()");
	    }
	}
	
	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class CompoundExceptionTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //CompoundExceptionTest ce = me.new CompoundExceptionTest();
	    //@Test
		//try {
	    //	ce.testGetMessage_NonNested();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running testGetMessage_NonNested()
	    public void testGetMessage_NonNested() throws Exception
	    {
	        try {
	            throw new CompoundException("message");
	        } catch (CompoundException e) {
	            assertEquals("message", e.getMessage());
	        }
	        System.out.println("Finished running testGetMessage_NonNested()");
	    }

	    //@Test
	    //try {
	    //	ce.testGetMessage_Nested();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetMessage_Nested()
	    public void testGetMessage_Nested() throws Exception
	    {
	        try {
	            try {
	                throw new IOException("io");
	            } catch (IOException e) {
	                throw new CompoundException("compound", e);
	            }
	        } catch (CompoundException e) {
	            assertEquals("compound", e.getMessage());
	            final Throwable innerException = e.getInnerException();
	            assertNotNull(innerException);
	            assertEquals("io", innerException.getMessage());
	        }
	        System.out.println("Finished running testGetMessage_Nested()");
	    }

	    //@Test
	    //try {
	    //	ce.testNoInnerException();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testNoInnerException()
	    public void testNoInnerException() throws Exception
	    {
	        try {
	            throw new CompoundException("message", null);
	        } catch (CompoundException e) {
	            try {
	                PrintStream nullStream = new PrintStream(new NullOutputStream());
	                e.printStackTrace(nullStream);
	                e.printStackTrace(new PrintWriter(nullStream));
	            } catch (Exception e1) {
	                fail("Exception during printStackTrace for CompoundException with no inner exception");
	            }
	        }
	        System.out.println("Finished running testNoInnerException()");
	    }
	}

	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class GeoLocationTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //GeoLocationTest gt = me.new GeoLocationTest();
	    //@Test
		//try {
	    //	gt.testDecimalToDegreesMinutesSeconds();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running testDecimalToDegreesMinutesSeconds()
	    public void testDecimalToDegreesMinutesSeconds() throws Exception
	    {
	    	GeoLocation geo = new GeoLocation();
	        double[] dms = geo.decimalToDegreesMinutesSeconds(1);
	        assertEquals(1.0, dms[0], 0.0001);
	        assertEquals(0.0, dms[1], 0.0001);
	        assertEquals(0.0, dms[2], 0.0001);

	        dms = geo.decimalToDegreesMinutesSeconds(-12.3216);
	        assertEquals(-12.0, dms[0], 0.0001);
	        assertEquals(19.0, dms[1], 0.0001);
	        assertEquals(17.76, dms[2], 0.0001);

	        dms = geo.decimalToDegreesMinutesSeconds(32.698);
	        assertEquals(32.0, dms[0], 0.0001);
	        assertEquals(41.0, dms[1], 0.0001);
	        assertEquals(52.8, dms[2], 0.0001);
	        System.out.println("Finished running testDecimalToDegreesMinutesSeconds()");
	    }
	}

	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class NullOutputStreamTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //NullOutputStreamTest nt = me.new NullOutputStreamTest();
	    //@Test
		//try {
	    //	nt.testCreateNullOutputStream();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running testCreateNullOutputStream()
	    public void testCreateNullOutputStream() throws Exception
	    {
	        OutputStream out = new NullOutputStream();
	        try {
	            out.write(1);
	        } finally {
	            out.close();
	        }
	        System.out.println("Finished running testCreateNullOutputStream()");
	    }
	}
	
	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class RandomAccessFileReaderTest extends RandomAccessTestBase
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //RandomAccessFileReaderTest rt = me.new RandomAccessFileReaderTest();
	    private File _tempFile;
	    private RandomAccessFile _randomAccessFile;

	    @Override
	    protected RandomAccessReader createReader(byte[] bytes)
	    {
	        try {
	            // Unit tests can create multiple readers in the same test, as long as they're used one after the other
	            deleteTempFile();

	            _tempFile = File.createTempFile("C:/metadata/metadata-extractor-test-", ".tmp");
	            FileUtil.saveBytes(_tempFile, bytes);
	            _randomAccessFile = new RandomAccessFile(_tempFile, "r");
	            return new RandomAccessFileReader(_randomAccessFile);
	        } catch (IOException e) {
	            fail("Unable to create temp file");
	            return null;
	        }
	    }

	    //@After
	    public void deleteTempFile() throws IOException
	    {
	        if (_randomAccessFile == null)
	            return;

	        _randomAccessFile.close();

	        if (_tempFile == null)
	            return;

	        assertTrue(
	                "Unable to delete temp file used during unit test: " + _tempFile.getAbsolutePath(),
	                _tempFile.delete());

	        _tempFile = null;
	        _randomAccessFile = null;
	    }

	    //@SuppressWarnings({ "ConstantConditions" })
	    //@Test(expected = NullPointerException.class)
	    //try {
	    //	rt.testConstructWithNullBufferThrows();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    // Throws NullPointerException as expected
	    public void testConstructWithNullBufferThrows() throws IOException
	    {
	        new RandomAccessFileReader(null);
	        System.out.println("Finished running testConstructWithNullBufferThrows()");
	    }
	}

	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class RandomAccessStreamReaderTest extends RandomAccessTestBase
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //RandomAccessStreamReaderTest rt = me.new RandomAccessStreamReaderTest();
	    //@SuppressWarnings({ "ConstantConditions" })
	    //@Test(expected = NullPointerException.class)
		//try {
	    //	rt.testConstructWithNullBufferThrows();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		// Throws NullPointerException as expected
	    public void testConstructWithNullBufferThrows()
	    {
	        new RandomAccessStreamReader(null);
	        System.out.println("Finished running testConstructWithNullBufferThrows()");
	    }

	    @Override
	    protected RandomAccessReader createReader(byte[] bytes)
	    {
	        return new RandomAccessStreamReader(new ByteArrayInputStream(bytes));
	    }
	}
	
	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class RationalTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //RationalTest rt = me.new RationalTest();
	    //@Test
		//try {
	    //	rt.testCompare();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running testCompare()
	    public void testCompare() throws Exception
	    {
	        Rational third1 = new Rational(1, 3);
	        Rational third2 = new Rational(2, 6);
	        assertEquals(0, third1.compareTo(third2));

	        Rational half = new Rational(1, 2);
	        assertEquals(-1, third1.compareTo(half));

	        Rational negForth = new Rational(-1, 4);
	        assertEquals(1, third1.compareTo(negForth));
	        System.out.println("Finished running testCompare()");
	    }

	    //@Test
	    //try {
	    //	rt.testCreateRational();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testCreateRational()
	    public void testCreateRational() throws Exception
	    {
	        Rational rational = new Rational(1, 3);
	        assertEquals(1, rational.getNumerator());
	        assertEquals(3, rational.getDenominator());
	        assertEquals(1d / 3d, rational.doubleValue(), 0.0001);
	        System.out.println("Finished running testCreateRational()");
	    }

	    //@Test
	    //try {
	    //	rt.testToString();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testToString()
	    public void testToString() throws Exception
	    {
	        Rational rational = new Rational(1, 3);
	        assertEquals("1/3", rational.toString());
	        System.out.println("Finished running testToString()");
	    }

	    //@Test
	    //try {
	    //	rt.testToSimpleString();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testToSimpleString()
	    public void testToSimpleString() throws Exception
	    {
	        Rational third1 = new Rational(1, 3);
	        Rational third2 = new Rational(2, 6);
	        assertEquals("1/3", third1.toSimpleString(true));
	        assertEquals("1/3", third2.toSimpleString(true));
	        assertEquals(third1, third2);

	        Rational twoThirds = new Rational(10, 15);
	        assertEquals("2/3", twoThirds.toSimpleString(true));

	        Rational twoSixths = new Rational(2, 6);
	        assertEquals("1/3", twoSixths.toSimpleString(true));
	        assertEquals("1/3", twoSixths.toSimpleString(false));

	        Rational two = new Rational(10, 5);
	        assertTrue(two.isInteger());
	        assertEquals("2", two.toSimpleString(true));
	        assertEquals("2", two.toSimpleString(false));

	        Rational twoFifths = new Rational(4, 10);
	        assertEquals("0.4", twoFifths.toSimpleString(true));
	        assertEquals("2/5", twoFifths.toSimpleString(false));

	        Rational threeEighths = new Rational(3, 8);
	        assertEquals("3/8", threeEighths.toSimpleString(true));

	        Rational zero = new Rational(0, 8);
	        assertTrue(zero.isInteger());
	        assertEquals("0", zero.toSimpleString(true));
	        assertEquals("0", zero.toSimpleString(false));

	        zero = new Rational(0, 0);
	        assertTrue(zero.isInteger());
	        assertEquals("0", zero.toSimpleString(true));
	        assertEquals("0", zero.toSimpleString(false));
	        System.out.println("Finished running testToSimpleString()");
	    }

	    //@Test
	    //try {
	    //	rt.testGetReciprocal();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testGetReciprocal()
	    public void testGetReciprocal() throws Exception
	    {
	        Rational rational = new Rational(1, 3);
	        Rational reciprocal = rational.getReciprocal();
	        assertEquals("new rational should be reciprocal", new Rational(3, 1), reciprocal);
	        assertEquals("original reciprocal should remain unchanged", new Rational(1, 3), rational);
	        System.out.println("Finished running testGetReciprocal()");
	    }

	    //@Test
	    //try {
	    //	rt.testZeroOverZero();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testZeroOverZero()
	    public void testZeroOverZero() throws Exception
	    {
	        assertEquals(new Rational(0, 0), new Rational(0, 0).getReciprocal());
	        assertEquals(0.0d, new Rational(0, 0).doubleValue(), 0.000000001);
	        assertEquals(0, new Rational(0, 0).byteValue());
	        assertEquals(0.0f, new Rational(0, 0).floatValue(), 0.000000001f);
	        assertEquals(0, new Rational(0, 0).intValue());
	        assertEquals(0L, new Rational(0, 0).longValue());
	        assertTrue(new Rational(0, 0).isInteger());
	        System.out.println("Finished running testZeroOverZero()");
	    }

	    private final int[] _primes =
	    {
	        2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131,
	        137, 139, 149, 151, 157, 163, 167, 173, 179, 181, 191, 193, 197, 199, 211, 223, 227, 229, 233, 239, 241, 251, 257, 263, 269, 271,
	        277, 281, 283, 293, 307, 311, 313, 317, 331, 337, 347, 349, 353, 359, 367, 373, 379, 383, 389, 397, 401, 409, 419, 421, 431, 433,
	        439, 443, 449, 457, 461, 463, 467, 479, 487, 491, 499, 503, 509, 521, 523, 541, 547, 557, 563, 569, 571, 577, 587, 593, 599, 601,
	        607, 613, 617, 619, 631, 641, 643, 647, 653, 659, 661, 673, 677, 683, 691, 701, 709, 719, 727, 733, 739, 743, 751, 757, 761, 769,
	        773, 787, 797, 809, 811, 821, 823, 827, 829, 839, 853, 857, 859, 863, 877, 881, 883, 887, 907, 911, 919, 929, 937, 941, 947, 953,
	        967, 971, 977, 983, 991, 997, 1009, 1013, 1019, 1021, 1031, 1033, 1039, 1049, 1051, 1061, 1063, 1069, 1087, 1091, 1093, 1097, 1103,
	        1109, 1117, 1123, 1129, 1151, 1153, 1163, 1171, 1181, 1187, 1193, 1201, 1213, 1217, 1223, 1229, 1231, 1237, 1249, 1259, 1277, 1279,
	        1283, 1289, 1291, 1297, 1301, 1303, 1307, 1319, 1321, 1327, 1361, 1367, 1373, 1381, 1399, 1409, 1423, 1427, 1429, 1433, 1439, 1447,
	        1451, 1453, 1459, 1471, 1481, 1483, 1487, 1489, 1493, 1499, 1511, 1523, 1531, 1543, 1549, 1553, 1559, 1567, 1571, 1579, 1583, 1597,
	        1601, 1607, 1609, 1613, 1619, 1621, 1627, 1637, 1657, 1663, 1667, 1669, 1693, 1697, 1699, 1709, 1721, 1723, 1733, 1741, 1747, 1753,
	        1759, 1777, 1783, 1787, 1789, 1801, 1811, 1823, 1831, 1847, 1861, 1867, 1871, 1873, 1877, 1879, 1889, 1901, 1907, 1913, 1931, 1933,
	        1949, 1951, 1973, 1979, 1987, 1993, 1997, 1999, 2003, 2011, 2017, 2027, 2029, 2039, 2053, 2063, 2069, 2081, 2083, 2087, 2089, 2099,
	        2111, 2113, 2129, 2131, 2137, 2141, 2143, 2153, 2161, 2179, 2203, 2207, 2213, 2221, 2237, 2239, 2243, 2251, 2267, 2269, 2273, 2281,
	        2287, 2293, 2297, 2309, 2311, 2333, 2339, 2341, 2347, 2351, 2357, 2371, 2377, 2381, 2383, 2389, 2393, 2399, 2411, 2417, 2423, 2437,
	        2441, 2447, 2459, 2467, 2473, 2477, 2503, 2521, 2531, 2539, 2543, 2549, 2551, 2557, 2579, 2591, 2593, 2609, 2617, 2621, 2633, 2647,
	        2657, 2659, 2663, 2671, 2677, 2683, 2687, 2689, 2693, 2699, 2707, 2711, 2713, 2719, 2729, 2731, 2741, 2749, 2753, 2767, 2777, 2789,
	        2791, 2797, 2801, 2803, 2819, 2833, 2837, 2843, 2851, 2857, 2861, 2879, 2887, 2897, 2903, 2909, 2917, 2927, 2939, 2953, 2957, 2963,
	        2969, 2971, 2999, 3001, 3011, 3019, 3023, 3037, 3041, 3049, 3061, 3067, 3079, 3083, 3089, 3109, 3119, 3121, 3137, 3163, 3167, 3169,
	        3181, 3187, 3191, 3203, 3209, 3217, 3221, 3229, 3251, 3253, 3257, 3259, 3271, 3299, 3301, 3307, 3313, 3319, 3323, 3329, 3331, 3343,
	        3347, 3359, 3361, 3371, 3373, 3389, 3391, 3407, 3413, 3433, 3449, 3457, 3461, 3463, 3467, 3469, 3491, 3499, 3511, 3517, 3527, 3529,
	        3533, 3539, 3541, 3547, 3557, 3559, 3571, 3581, 3583, 3593, 3607, 3613, 3617, 3623, 3631, 3637, 3643, 3659, 3671, 3673, 3677, 3691,
	        3697, 3701, 3709, 3719, 3727, 3733, 3739, 3761, 3767, 3769, 3779, 3793, 3797, 3803, 3821, 3823, 3833, 3847, 3851, 3853, 3863, 3877,
	        3881, 3889, 3907, 3911, 3917, 3919, 3923, 3929, 3931, 3943, 3947, 3967, 3989, 4001, 4003, 4007, 4013, 4019, 4021, 4027, 4049, 4051,
	        4057, 4073, 4079, 4091, 4093, 4099, 4111, 4127, 4129, 4133, 4139, 4153, 4157, 4159, 4177, 4201, 4211, 4217, 4219, 4229, 4231, 4241,
	        4243, 4253, 4259, 4261, 4271, 4273, 4283, 4289, 4297, 4327, 4337, 4339, 4349, 4357, 4363, 4373, 4391, 4397, 4409, 4421, 4423, 4441,
	        4447, 4451, 4457, 4463, 4481, 4483, 4493, 4507, 4513, 4517, 4519, 4523, 4547, 4549, 4561, 4567, 4583, 4591, 4597, 4603, 4621, 4637,
	        4639, 4643, 4649, 4651, 4657, 4663, 4673, 4679, 4691, 4703, 4721, 4723, 4729, 4733, 4751, 4759, 4783, 4787, 4789, 4793, 4799, 4801,
	        4813, 4817, 4831, 4861, 4871, 4877, 4889, 4903, 4909, 4919, 4931, 4933, 4937, 4943, 4951, 4957, 4967, 4969, 4973, 4987, 4993, 4999,
	        5003, 5009, 5011, 5021, 5023, 5039, 5051, 5059, 5077, 5081, 5087, 5099, 5101, 5107, 5113, 5119, 5147, 5153, 5167, 5171, 5179, 5189,
	        5197, 5209, 5227, 5231, 5233, 5237, 5261, 5273, 5279, 5281, 5297, 5303, 5309, 5323, 5333, 5347, 5351, 5381, 5387, 5393, 5399, 5407,
	        5413, 5417, 5419, 5431, 5437, 5441, 5443, 5449, 5471, 5477, 5479, 5483, 5501, 5503, 5507, 5519, 5521, 5527, 5531, 5557, 5563, 5569,
	        5573, 5581, 5591, 5623, 5639, 5641, 5647, 5651, 5653, 5657, 5659, 5669, 5683, 5689, 5693, 5701, 5711, 5717, 5737, 5741, 5743, 5749,
	        5779, 5783, 5791, 5801, 5807, 5813, 5821, 5827, 5839, 5843, 5849, 5851, 5857, 5861, 5867, 5869, 5879, 5881, 5897, 5903, 5923, 5927,
	        5939, 5953, 5981, 5987, 6007, 6011, 6029, 6037, 6043, 6047, 6053, 6067, 6073, 6079, 6089, 6091, 6101, 6113, 6121, 6131, 6133, 6143,
	        6151, 6163, 6173, 6197, 6199, 6203, 6211, 6217, 6221, 6229, 6247, 6257, 6263, 6269, 6271, 6277, 6287, 6299, 6301, 6311, 6317, 6323,
	        6329, 6337, 6343, 6353, 6359, 6361, 6367, 6373, 6379, 6389, 6397, 6421, 6427, 6449, 6451, 6469, 6473, 6481, 6491, 6521, 6529, 6547,
	        6551, 6553, 6563, 6569, 6571, 6577, 6581, 6599, 6607, 6619, 6637, 6653, 6659, 6661, 6673, 6679, 6689, 6691, 6701, 6703, 6709, 6719,
	        6733, 6737, 6761, 6763, 6779, 6781, 6791, 6793, 6803, 6823, 6827, 6829, 6833, 6841, 6857, 6863, 6869, 6871, 6883, 6899, 6907, 6911,
	        6917, 6947, 6949, 6959, 6961, 6967, 6971, 6977, 6983, 6991, 6997, 7001, 7013, 7019, 7027, 7039, 7043, 7057, 7069, 7079, 7103, 7109,
	        7121, 7127, 7129, 7151, 7159, 7177, 7187, 7193, 7207, 7211, 7213, 7219, 7229, 7237, 7243, 7247, 7253, 7283, 7297, 7307, 7309, 7321,
	        7331, 7333, 7349, 7351, 7369, 7393, 7411, 7417, 7433, 7451, 7457, 7459, 7477, 7481, 7487, 7489, 7499, 7507, 7517, 7523, 7529, 7537,
	        7541, 7547, 7549, 7559, 7561, 7573, 7577, 7583, 7589, 7591, 7603, 7607, 7621, 7639, 7643, 7649, 7669, 7673, 7681, 7687, 7691, 7699,
	        7703, 7717, 7723, 7727, 7741, 7753, 7757, 7759, 7789, 7793, 7817, 7823, 7829, 7841, 7853, 7867, 7873, 7877, 7879, 7883, 7901, 7907,
	        7919, 7927, 7933, 7937, 7949, 7951, 7963, 7993, 8009, 8011, 8017, 8039, 8053, 8059, 8069, 8081, 8087, 8089, 8093, 8101, 8111, 8117,
	        8123, 8147, 8161, 8167, 8171, 8179, 8191, 8209, 8219, 8221, 8231, 8233, 8237, 8243, 8263, 8269, 8273, 8287, 8291, 8293, 8297, 8311,
	        8317, 8329, 8353, 8363, 8369, 8377, 8387, 8389, 8419, 8423, 8429, 8431, 8443, 8447, 8461, 8467, 8501, 8513, 8521, 8527, 8537, 8539,
	        8543, 8563, 8573, 8581, 8597, 8599, 8609, 8623, 8627, 8629, 8641, 8647, 8663, 8669, 8677, 8681, 8689, 8693, 8699, 8707, 8713, 8719,
	        8731, 8737, 8741, 8747, 8753, 8761, 8779, 8783, 8803, 8807, 8819, 8821, 8831, 8837, 8839, 8849, 8861, 8863, 8867, 8887, 8893, 8923,
	        8929, 8933, 8941, 8951, 8963, 8969, 8971, 8999, 9001, 9007, 9011, 9013, 9029, 9041, 9043, 9049, 9059, 9067, 9091, 9103, 9109, 9127,
	        9133, 9137, 9151, 9157, 9161, 9173, 9181, 9187, 9199, 9203, 9209, 9221, 9227, 9239, 9241, 9257, 9277, 9281, 9283, 9293, 9311, 9319,
	        9323, 9337, 9341, 9343, 9349, 9371, 9377, 9391, 9397, 9403, 9413, 9419, 9421, 9431, 9433, 9437, 9439, 9461, 9463, 9467, 9473, 9479,
	        9491, 9497, 9511, 9521, 9533, 9539, 9547, 9551, 9587, 9601, 9613, 9619, 9623, 9629, 9631, 9643, 9649, 9661, 9677, 9679, 9689, 9697,
	        9719, 9721, 9733, 9739, 9743, 9749, 9767, 9769, 9781, 9787, 9791, 9803, 9811, 9817, 9829, 9833, 9839, 9851, 9857, 9859, 9871, 9883,
	        9887, 9901, 9907, 9923, 9929, 9931, 9941, 9949, 9967, 9973
	    };

	    //@Test
	    //try {
	    //	rt.simplifiedInstances();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running simplifiedInstances()
	    public void simplifiedInstances()
	    {
	        Rational simple = new Rational(1, 2);

	        for (int prime : _primes)
	        {
	            Rational complex = new Rational(prime, 2 * prime);
	            Rational actualSimple = complex.getSimplifiedInstance();

	            assertTrue(simple.equalsExact(actualSimple));
	            assertEquals(actualSimple.doubleValue(), complex.doubleValue(), 0.0001);
	        }

	        simple = new Rational(2, 1);

	        for (int prime : _primes)
	        {
	            Rational complex = new Rational(2 * prime, prime);
	            Rational actualSimple = complex.getSimplifiedInstance();

	            assertTrue(simple.equalsExact(actualSimple));
	            assertEquals(actualSimple.doubleValue(), complex.doubleValue(), 0.0001);
	        }

	        simple = new Rational(-1, 2);

	        for (int prime : _primes)
	        {
	            Rational complex = new Rational(-prime, 2 * prime);
	            Rational actualSimple = complex.getSimplifiedInstance();

	            assertTrue(simple.equalsExact(actualSimple));
	            assertEquals(actualSimple.doubleValue(), complex.doubleValue(), 0.0001);
	        }

	        simple = new Rational(-1, 2);

	        for (int prime : _primes)
	        {
	            Rational complex = new Rational(prime, -2 * prime);
	            Rational actualSimple = complex.getSimplifiedInstance();

	            assertTrue(simple.equalsExact(actualSimple));
	            assertEquals(actualSimple.doubleValue(), complex.doubleValue(), 0.0001);
	        }

	        simple = new Rational(1, 2);

	        for (int prime : _primes)
	        {
	            Rational complex = new Rational(-prime, -2 * prime);
	            Rational actualSimple = complex.getSimplifiedInstance();

	            assertTrue(simple.equalsExact(actualSimple));
	            assertEquals(actualSimple.doubleValue(), complex.doubleValue(), 0.0001);
	        }

	        assertEquals(new Rational(-32768, 65535), new Rational(-32768, 65535).getSimplifiedInstance());
	        assertEquals(new Rational(-32768, 32767), new Rational(-32768, 32767).getSimplifiedInstance());
	        System.out.println("Finished running simplifiedInstances()");
	    }

	    //@Test
	    //try {
	    //	rt.getSimplifiedInstance_FlipsSignsIfNeeded();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running getSimplifiedInstance_FlipsSignsIfNeeded()
	    public void getSimplifiedInstance_FlipsSignsIfNeeded()
	    {
	        Rational r = new Rational(1, -2);

	        Rational s = r.getSimplifiedInstance();

	        assertEquals(-1, s.getNumerator());
	        assertEquals(2, s.getDenominator());
	        System.out.println("Finished running getSimplifiedInstance_FlipsSignsIfNeeded()");
	    }

	    //@Test
	    //try {
	    //	rt.getSimplifiedInstance_RemovesSignsIfNeeded();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running getSimplifiedInstance_RemovesSignsIfNeeded()
	    public void getSimplifiedInstance_RemovesSignsIfNeeded()
	    {
	        Rational r = new Rational(-1, -2);

	        Rational s = r.getSimplifiedInstance();

	        assertEquals(1, s.getNumerator());
	        assertEquals(2, s.getDenominator());
	        System.out.println("Finished running getSimplifiedInstance_RemovesSignsIfNeeded()");
	    }
	}

	/**
	 * Base class for testing implementations of {@link com.drew.lang.SequentialReader}.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public abstract class SequentialAccessTestBase
	{
	    protected abstract SequentialReader createReader(byte[] bytes);

	    //@Test
	    //try {
	    //	rt.testDefaultEndianness();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testDefaultEndianness()
	    {
	        assertTrue(createReader(new byte[1]).isMotorolaByteOrder());
	        System.out.println("Finished running testDefaultEndianness()");
	    }

	    //@Test
	    //try {
	    //	rt.testGetInt8();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetInt8() throws IOException
	    {
	        byte[] buffer = new byte[]{0x00, 0x01, (byte)0x7F, (byte)0xFF};
	        SequentialReader reader = createReader(buffer);

	        assertEquals((byte)0, reader.getInt8());
	        assertEquals((byte)1, reader.getInt8());
	        assertEquals((byte)127, reader.getInt8());
	        assertEquals((byte)255, reader.getInt8());
	        System.out.println("Finished running testGetInt8()");
	    }

	    //@Test
	    //try {
	    //	rt.testGetUInt8();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetUInt8() throws IOException
	    {
	        byte[] buffer = new byte[]{0x00, 0x01, (byte)0x7F, (byte)0xFF};
	        SequentialReader reader = createReader(buffer);

	        assertEquals(0, reader.getUInt8());
	        assertEquals(1, reader.getUInt8());
	        assertEquals(127, reader.getUInt8());
	        assertEquals(255, reader.getUInt8());
	        System.out.println("Finished running testGetUInt8()");
	    }

	    //@Test
	    //try {
	    //	rt.testGetUInt8_OutOfBounds();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetUInt8_OutOfBounds()
	    {
	        try {
	            SequentialReader reader = createReader(new byte[1]);
	            reader.getUInt8();
	            reader.getUInt8();
	            fail("Exception expected");
	        } catch (IOException ex) {
	            assertEquals("End of data reached.", ex.getMessage());
	        }
	        System.out.println("Finished running testGetUInt8_OutOfBounds()");
	    }

	    //@Test
	    //try {
	    //	rt.testGetInt16();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetInt16() throws IOException
	    {
	        assertEquals(-1, createReader(new byte[]{(byte)0xff, (byte)0xff}).getInt16());

	        byte[] buffer = new byte[]{0x00, 0x01, (byte)0x7F, (byte)0xFF};
	        SequentialReader reader = createReader(buffer);

	        assertEquals((short)0x0001, reader.getInt16());
	        assertEquals((short)0x7FFF, reader.getInt16());

	        reader = createReader(buffer);
	        reader.setMotorolaByteOrder(false);

	        assertEquals((short)0x0100, reader.getInt16());
	        assertEquals((short)0xFF7F, reader.getInt16());
	        System.out.println("Finished running testGetInt16()");
	    }

	    //@Test
	    //try {
	    //	rt.testGetUInt16();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetUInt16() throws IOException
	    {
	        byte[] buffer = new byte[]{0x00, 0x01, (byte)0x7F, (byte)0xFF};
	        SequentialReader reader = createReader(buffer);

	        assertEquals(0x0001, reader.getUInt16());
	        assertEquals(0x7FFF, reader.getUInt16());

	        reader = createReader(buffer);
	        reader.setMotorolaByteOrder(false);

	        assertEquals(0x0100, reader.getUInt16());
	        assertEquals(0xFF7F, reader.getUInt16());
	        System.out.println("Finished running testGetUInt16()");
	    }

	    //@Test
	    //try {
	    //	rt.testGetUInt16_OutOfBounds();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetUInt16_OutOfBounds()
	    {
	        try {
	            SequentialReader reader = createReader(new byte[1]);
	            reader.getUInt16();
	            fail("Exception expected");
	        } catch (IOException ex) {
	            assertEquals("End of data reached.", ex.getMessage());
	        }
	        System.out.println("Finished running testGetUInt16_OutOfBounds()");
	    }

	    //@Test
	    //try {
	    //	rt.testGetInt32();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetInt32() throws IOException
	    {
	        assertEquals(-1, createReader(new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff}).getInt32());

	        byte[] buffer = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};
	        SequentialReader reader = createReader(buffer);

	        assertEquals(0x00010203, reader.getInt32());
	        assertEquals(0x04050607, reader.getInt32());

	        reader = createReader(buffer);
	        reader.setMotorolaByteOrder(false);

	        assertEquals(0x03020100, reader.getInt32());
	        assertEquals(0x07060504, reader.getInt32());
	        System.out.println("Finished running testGetInt32()");
	    }

	    //@Test
	    //try {
	    //	rt.testGetUInt32();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetUInt32() throws IOException
	    {
	        assertEquals(4294967295L, createReader(new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff}).getUInt32());

	        byte[] buffer = new byte[]{(byte)0xFF, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06};
	        SequentialReader reader = createReader(buffer);

	        assertEquals(0xFF000102L, reader.getUInt32());
	        assertEquals(0x03040506L, reader.getUInt32());

	        reader = createReader(buffer);
	        reader.setMotorolaByteOrder(false);

	        assertEquals(0x020100FFL, reader.getUInt32()); // 0x0010200FF
	        assertEquals(0x06050403L, reader.getUInt32());
	        System.out.println("Finished running testGetUInt32()");
	    }

	    //@Test
	    //try {
	    //	rt.testGetInt32_OutOfBounds();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetInt32_OutOfBounds()
	    {
	        try {
	            SequentialReader reader = createReader(new byte[3]);
	            reader.getInt32();
	            fail("Exception expected");
	        } catch (IOException ex) {
	            assertEquals("End of data reached.", ex.getMessage());
	        }
	        System.out.println("Finished running testGetInt32_OutOfBounds()");
	    }

	    //@Test
	    //try {
	    //	rt.testGetInt64();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetInt64() throws IOException
	    {
	        byte[] buffer = new byte[]{(byte)0xFF, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};
	        SequentialReader reader = createReader(buffer);

	        assertEquals(0xFF00010203040506L, reader.getInt64());

	        reader = createReader(buffer);
	        reader.setMotorolaByteOrder(false);

	        assertEquals(0x06050403020100FFL, reader.getInt64());
	        System.out.println("Finished running testGetInt64()");
	    }

	    //@Test
	    //try {
	    //	rt.testGetInt64_OutOfBounds();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetInt64_OutOfBounds()
	    {
	        try {
	            SequentialReader reader = createReader(new byte[7]);
	            reader.getInt64();
	            fail("Exception expected");
	        } catch (IOException ex) {
	            assertEquals("End of data reached.", ex.getMessage());
	        }
	        System.out.println("Finished running testGetInt64_OutOfBounds()");
	    }

	    //@Test
	    //try {
	    //	rt.testGetFloat32();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetFloat32() throws IOException
	    {
	        final int nanBits = 0x7fc00000;
	        assertTrue(Float.isNaN(Float.intBitsToFloat(nanBits)));

	        byte[] buffer = new byte[]{0x7f, (byte)0xc0, 0x00, 0x00};
	        SequentialReader reader = createReader(buffer);

	        assertTrue(Float.isNaN(reader.getFloat32()));
	        System.out.println("Finished running testGetFloat32()");
	    }

	    //@Test
	    //try {
	    //	rt.testGetFloat64();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetFloat64() throws IOException
	    {
	        final long nanBits = 0xfff0000000000001L;
	        assertTrue(Double.isNaN(Double.longBitsToDouble(nanBits)));

	        byte[] buffer = new byte[]{(byte)0xff, (byte)0xf0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01};
	        SequentialReader reader = createReader(buffer);

	        assertTrue(Double.isNaN(reader.getDouble64()));
	        System.out.println("Finished running testGetFloat64()");
	    }

	    //@Test
	    //try {
	    //	rt.testGetNullTerminatedString();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetNullTerminatedString() throws IOException
	    {
	    	Charsets ch = new Charsets();
	        byte[] bytes = new byte[]{0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47};

	        // Test max length
	        for (int i = 0; i < bytes.length; i++) {
	            assertEquals("ABCDEFG".substring(0, i), createReader(bytes).getNullTerminatedString(i, ch.UTF_8));
	        }

	        assertEquals("", createReader(new byte[]{0}).getNullTerminatedString(10, ch.UTF_8));
	        assertEquals("A", createReader(new byte[]{0x41, 0}).getNullTerminatedString(10, ch.UTF_8));
	        assertEquals("AB", createReader(new byte[]{0x41, 0x42, 0}).getNullTerminatedString(10, ch.UTF_8));
	        assertEquals("AB", createReader(new byte[]{0x41, 0x42, 0, 0x43}).getNullTerminatedString(10, ch.UTF_8));
	        System.out.println("Finished running testGetNullTerminatedString()");
	    }

	    //@Test
	    //try {
	    //	rt.testGetString();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetString() throws IOException
	    {
	        byte[] bytes = new byte[]{0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47};
	        String expected = new String(bytes);
	        assertEquals(bytes.length, expected.length());

	        for (int i = 0; i < bytes.length; i++) {
	            assertEquals("ABCDEFG".substring(0, i), createReader(bytes).getString(i));
	        }
	        System.out.println("Finished running testGetString()");
	    }

	    //@Test
	    //try {
	    //	rt.testGetBytes();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetBytes() throws IOException
	    {
	        byte[] bytes = {0, 1, 2, 3, 4, 5};

	        for (int i = 0; i < bytes.length; i++) {
	            SequentialReader reader = createReader(bytes);
	            byte[] readBytes = reader.getBytes(i);
	            for (int j = 0; j < i; j++) {
	                assertEquals(bytes[j], readBytes[j]);
	            }
	        }
	        System.out.println("Finished running testGetBytes()");
	    }

	    //@Test
  	    //try {
	    //	rt.testOverflowBoundsCalculation();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testOverflowBoundsCalculation()
	    {
	        try {
	            SequentialReader reader = createReader(new byte[10]);
	            reader.getBytes(15);
	        } catch (IOException e) {
	            assertEquals("End of data reached.", e.getMessage());
	        }

	        try {
	            SequentialReader reader = createReader(new byte[10]);
	            reader.getBytes(5);
	            reader.getBytes(Integer.MAX_VALUE);
	        } catch (IOException e) {
	            assertEquals("End of data reached.", e.getMessage());
	        }
	        System.out.println("Finished running testOverflowBoundsCalculation()");
	    }

	    //@Test
	    //try {
	    //	rt.testGetBytesEOF();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetBytesEOF() throws Exception
	    {
	        createReader(new byte[50]).getBytes(50);

	        SequentialReader reader = createReader(new byte[50]);
	        reader.getBytes(25);
	        reader.getBytes(25);

	        try {
	            createReader(new byte[50]).getBytes(51);
	            fail("Expecting exception");
	        } catch (EOFException ignored) {}
	        System.out.println("Finished running testGetBytesEOF()");
	    }

	    //@Test
	    //try {
	    //	rt.testGetInt8EOF();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testGetInt8EOF() throws Exception
	    {
	        createReader(new byte[1]).getInt8();

	        SequentialReader reader = createReader(new byte[2]);
	        reader.getInt8();
	        reader.getInt8();

	        try {
	            reader = createReader(new byte[1]);
	            reader.getInt8();
	            reader.getInt8();
	            fail("Expecting exception");
	        } catch (EOFException ignored) {}
	        System.out.println("Finished running testGetInt8EOF()");
	    }

	    //@Test
	    //try {
	    //	rt.testSkipEOF();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testSkipEOF() throws Exception
	    {
	        createReader(new byte[1]).skip(1);

	        SequentialReader reader = createReader(new byte[2]);
	        reader.skip(1);
	        reader.skip(1);

	        try {
	            reader = createReader(new byte[1]);
	            reader.skip(1);
	            reader.skip(1);
	            fail("Expecting exception");
	        } catch (EOFException ignored) {}

	        try {
	            reader = createReader(new byte[100]);
	            reader.skip(50);
	            reader.skip(Integer.MAX_VALUE);
	            fail("Expecting exception");
	        } catch (EOFException ignored) {}
	        System.out.println("Finished running testSkipEOF()");
	    }

	    //@Test
	    //try {
	    //	rt.testTrySkipEOF();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    public void testTrySkipEOF() throws Exception
	    {
	        assertTrue(createReader(new byte[1]).trySkip(1));

	        SequentialReader reader = createReader(new byte[2]);
	        assertTrue(reader.trySkip(1));
	        assertTrue(reader.trySkip(1));
	        assertFalse(reader.trySkip(1));

	        reader = createReader(new byte[100]);
	        reader.getBytes(50);
	        assertFalse(reader.trySkip(Integer.MAX_VALUE));
	        System.out.println("Finished running testTrySkipEOF()");
	    }
	}
	
	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class SequentialByteArrayReaderTest extends SequentialAccessTestBase
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //SequentialByteArrayReaderTest st = me.new SequentialByteArrayReaderTest();
		//Finished running testDefaultEndianness()
		//Finished running testGetInt8()
		//Finished running testGetUInt8()
		//Finished running testGetUInt8_OutOfBounds()
		//Finished running testGetInt16()
		//Finished running testGetUInt16()
		//Finished running testGetUInt16_OutOfBounds()
		//Finished running testGetInt32()
		//Finished running testGetUInt32()
		//Finished running testGetInt32_OutOfBounds()
		//Finished running testGetInt64()
		//Finished running testGetInt64_OutOfBounds()
		//Finished running testGetFloat32()
		//Finished running testGetFloat64()
		//Finished running testGetNullTerminatedString()
		//Finished running testGetString()
		//Finished running testGetBytes()
		//Finished running testOverflowBoundsCalculation()
		//Finished running testGetBytesEOF()
		//Finished running testGetInt8EOF()
		//Finished running testSkipEOF()
		//Finished running testTrySkipEOF()
	    //@SuppressWarnings({"ConstantConditions"})
	    //@Test(expected = NullPointerException.class)
		//try {
	    //	st.testConstructWithNullStreamThrows();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Throws NullPointerException as expected
	    public void testConstructWithNullStreamThrows()
	    {
	        new SequentialByteArrayReader(null);
	    }

	    @Override
	    protected SequentialReader createReader(byte[] bytes)
	    {
	        return new SequentialByteArrayReader(bytes);
	    }
	}

	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class StreamReaderTest extends SequentialAccessTestBase
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //StreamReaderTest st = me.new StreamReaderTest();
	    //@SuppressWarnings({"ConstantConditions"})
	    //@Test(expected = NullPointerException.class)
		//try {
	    //	st.testConstructWithNullStreamThrows();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Throws NullPointerException as expected
	    public void testConstructWithNullStreamThrows()
	    {
	        new StreamReader(null);
	    }

	    @Override
	    protected SequentialReader createReader(byte[] bytes)
	    {
	        return new StreamReader(new ByteArrayInputStream(bytes));
	    }
	}
	
	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class StringUtilTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //StringUtilTest st = me.new StringUtilTest();
	    //@Test
		//try {
	    //	st.testJoinIterable();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running testJoinIterable()
		public void testJoinIterable()
	    {
	        List<String> strings = new ArrayList<String>();
	        strings.add("A");
	        strings.add("B");
	        strings.add("C");

	        assertEquals("A;B;C", StringUtil.join(strings, ";"));

	        assertEquals("", StringUtil.join(new ArrayList<String>(), ";"));
	        
	        System.out.println("Finished running testJoinIterable()");
	    }

	    //@Test 
		//try {
	    //	st.testJoinArray();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
		//Finished running testJoinArray()
		public void testJoinArray()
	    {
	        String[] strings = new String[]{"A", "B", "C"};

	        assertEquals("A;B;C", StringUtil.join(strings, ";"));

	        assertEquals("", StringUtil.join(new ArrayList<String>(), ";"));

	        System.out.println("Finished running testJoinArray()");
	    }
	}

	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class AgeTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //AgeTest at = me.new AgeTest();
	    //@Test
		//try {
	    //	at.testParse();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
        //Finished running testParse()
	    public void testParse()
	    {
	    	Age aa = new Age();
	        Age age = aa.fromPanasonicString("0031:07:15 00:00:00");
	        assertNotNull(age);
	        assertEquals(31, age.getYears());
	        assertEquals(7, age.getMonths());
	        assertEquals(15, age.getDays());
	        assertEquals(0, age.getHours());
	        assertEquals(0, age.getMinutes());
	        assertEquals(0, age.getSeconds());
	        assertEquals("0031:07:15 00:00:00", age.toString());
	        assertEquals("31 years 7 months 15 days", age.toFriendlyString());
	        System.out.println("Finished running testParse()");
	    }

	    //@SuppressWarnings({ "ObjectEqualsNull", "EqualsBetweenInconvertibleTypes", "NullableProblems" })
	    //@Test
	    //try {
	    //	at.testEqualsAndHashCode();
	    //}
	    //catch(Exception e) {
	    //	e.printStackTrace();
	    //}
	    //Finished running testEqualsAndHashCode()
	    public void testEqualsAndHashCode()
	    {
	        Age age1 = new Age(10, 11, 12, 13, 14, 15);
	        Age age2 = new Age(10, 11, 12, 13, 14, 15);
	        Age age3 = new Age(0, 0, 0, 0, 0, 0);

	        assertEquals(age1, age1);
	        assertEquals(age1, age2);
	        assertEquals(age2, age1);

	        assertTrue(age1.equals(age1));
	        assertTrue(age1.equals(age2));
	        assertFalse(age1.equals(age3));
	        assertFalse(age1.equals(null));
	        assertFalse(age1.equals("Hello"));

	        assertEquals(age1.hashCode(), age1.hashCode());
	        assertEquals(age1.hashCode(), age2.hashCode());
	        assertFalse(age1.hashCode() == age3.hashCode());
	        System.out.println("Finished running testEqualsAndHashCode()");
	    }
	}
	
	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class DirectoryTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //DirectoryTest dt = me.new DirectoryTest();
		//try {
		//    dt.setup();
		//}
		//catch (Exception e) {
		//    e.printStackTrace();
		//}
	    // TODO write tests to validate type conversions from all underlying types

	    private Directory _directory;

	    //@Before 
	    public void setup()
	    {
	        _directory = new MockDirectory();
	    }

	    //@Test 
	    //try {
  		//    dt.testSetAndGetMultipleTagsInSingleDirectory();
  		//}
  		//catch (Exception e) {
  		//    e.printStackTrace();
  		//}
	    //Finished running testSetAndGetMultipleTagsInSingleDirectory()
	    public void testSetAndGetMultipleTagsInSingleDirectory() throws Exception
	    {
	        _directory.setString(ExifSubIFDDirectory.TAG_APERTURE, "TAG_APERTURE");
	        _directory.setString(ExifSubIFDDirectory.TAG_BATTERY_LEVEL, "TAG_BATTERY_LEVEL");
	        assertEquals("TAG_APERTURE", _directory.getString(ExifSubIFDDirectory.TAG_APERTURE));
	        assertEquals("TAG_BATTERY_LEVEL", _directory.getString(ExifSubIFDDirectory.TAG_BATTERY_LEVEL));
	        System.out.println("Finished running testSetAndGetMultipleTagsInSingleDirectory()");
	    }

	    //@Test 
	    //try {
  		//    dt.testSetSameTagMultipleTimesOverwritesValue();
  		//}
  		//catch (Exception e) {
  		//    e.printStackTrace();
  		//}
	    //Finished running testSetSameTagMultipleTimesOverwritesValue()
	    public void testSetSameTagMultipleTimesOverwritesValue() throws Exception
	    {
	        _directory.setInt(ExifSubIFDDirectory.TAG_APERTURE, 1);
	        _directory.setInt(ExifSubIFDDirectory.TAG_APERTURE, 2);
	        assertEquals(2, _directory.getInt(ExifSubIFDDirectory.TAG_APERTURE));
	        System.out.println("Finished running testSetSameTagMultipleTimesOverwritesValue()");
	    }

	    //@Test
	    //try {
  		//    dt.testUnderlyingInt();
  		//}
  		//catch (Exception e) {
  		//    e.printStackTrace();
  		//}
	    //Finished running testUnderlyingInt()
	    public void testUnderlyingInt() throws Exception
	    {
	        int value = 123;
	        int tagType = 321;
	        _directory.setInt(tagType, value);

	        assertEquals(value, _directory.getInt(tagType));
	        assertEquals(Integer.valueOf(value), _directory.getInteger(tagType));
	        assertEquals((float)value, _directory.getFloat(tagType), 0.00001);
	        assertEquals((double)value, _directory.getDouble(tagType), 0.00001);
	        assertEquals((long)value, _directory.getLong(tagType));
	        assertEquals(Integer.toString(value), _directory.getString(tagType));
	        assertEquals(new Rational(value, 1), _directory.getRational(tagType));
	        assertArrayEquals(new int[]{value}, _directory.getIntArray(tagType));
	        assertArrayEquals(new byte[]{(byte)value}, _directory.getByteArray(tagType));
	        System.out.println("Finished running testUnderlyingInt()");
	    }

	    //@Test 
	    //try {
  		//    dt.testSetAndGetIntArray();
  		//}
  		//catch (Exception e) {
  		//    e.printStackTrace();
  		//}
	    //Finished running testSetAndGetIntArray()
	    public void testSetAndGetIntArray() throws Exception
	    {
	        int[] inputValues = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
	        int tagType = 123;
	        _directory.setIntArray(tagType, inputValues);
	        int[] outputValues = _directory.getIntArray(tagType);
	        assertNotNull(outputValues);
	        assertEquals(inputValues.length, outputValues.length);
	        for (int i = 0; i < inputValues.length; i++) {
	            int inputValue = inputValues[i];
	            int outputValue = outputValues[i];
	            assertEquals(inputValue, outputValue);
	        }
	        assertArrayEquals(inputValues, _directory.getIntArray(tagType));
	        StringBuilder outputString = new StringBuilder();
	        for (int i = 0; i < inputValues.length; i++) {
	            int inputValue = inputValues[i];
	            if (i > 0) {
	                outputString.append(' ');
	            }
	            outputString.append(inputValue);
	        }
	        assertEquals(outputString.toString(), _directory.getString(tagType));
	        System.out.println("Finished running testSetAndGetIntArray()");
	    }

	    //@Test
	    //try {
  		//    dt.testSetStringAndGetDate();
  		//}
  		//catch (Exception e) {
  		//    e.printStackTrace();
  		//}
	    //Finished running testSetStringAndGetDate()
	    public void testSetStringAndGetDate() throws Exception
	    {
	        String date1 = "2002:01:30 23:59:59";
	        String date2 = "2002:01:30 23:59";
	        String date3 = "2002-01-30 23:59:59";
	        String date4 = "2002-01-30 23:59";
	        String date5 = "2002-01-30T23:59:59.099-08:00";
	        String date6 = "2002-01-30T23:59:59.099";
	        String date7 = "2002-01-30T23:59:59-08:00";
	        String date8 = "2002-01-30T23:59:59";
	        String date9 = "2002-01-30T23:59-08:00";
	        String date10 = "2002-01-30T23:59";
	        String date11 = "2002-01-30";
	        String date12 = "2002-01";
	        String date13 = "2002";
	        _directory.setString(1, date1);
	        _directory.setString(2, date2);
	        _directory.setString(3, date3);
	        _directory.setString(4, date4);
	        _directory.setString(5, date5);
	        _directory.setString(6, date6);
	        _directory.setString(7, date7);
	        _directory.setString(8, date8);
	        _directory.setString(9, date9);
	        _directory.setString(10, date10);
	        _directory.setString(11, date11);
	        _directory.setString(12, date12);
	        _directory.setString(13, date13);
	        assertEquals(date1, _directory.getString(1));

	            // Don't use default timezone
	        TimeZone gmt = TimeZone.getTimeZone("GMT");
	        GregorianCalendar gc = new GregorianCalendar(gmt);
	            // clear millis to 0 or test will fail
	        gc.setTimeInMillis(0);
	        gc.set(2002, GregorianCalendar.JANUARY, 30, 23, 59, 59);
	        assertEquals(gc.getTime(), _directory.getDate(1, null));

	        gc.set(2002, GregorianCalendar.JANUARY, 30, 23, 59, 0);
	        assertEquals(gc.getTime(), _directory.getDate(2, null));

	            // Use specific timezone
	        TimeZone pst = TimeZone.getTimeZone("PST");
	        gc = new GregorianCalendar(pst);
	        gc.setTimeInMillis(0);

	        gc.set(2002, GregorianCalendar.JANUARY, 30, 23, 59, 59);
	        assertEquals(gc.getTime(), _directory.getDate(3, pst));

	        gc.set(2002, GregorianCalendar.JANUARY, 30, 23, 59, 0);
	        assertEquals(gc.getTime(), _directory.getDate(4, pst));

	        gc.set(2002, GregorianCalendar.JANUARY, 30, 23, 59, 59);
	        gc.set(Calendar.MILLISECOND, 99);
	        assertEquals(gc.getTime(), _directory.getDate(5, null));
	        assertEquals(gc.getTime(), _directory.getDate(5, gmt));
	        assertEquals(gc.getTime(), _directory.getDate(6, pst));

	        assertEquals(gc.getTime(), _directory.getDate(5, "011", null));
	        assertEquals(gc.getTime(), _directory.getDate(6, "011", pst));
	        assertEquals(gc.getTime(), _directory.getDate(7, "099", null));
	        assertEquals(gc.getTime(), _directory.getDate(8, "099", pst));

	        gc.set(Calendar.MILLISECOND, 0);
	        assertEquals(gc.getTime(), _directory.getDate(7, null));
	        assertEquals(gc.getTime(), _directory.getDate(7, gmt));
	        assertEquals(gc.getTime(), _directory.getDate(8, pst));

	        gc.set(2002, GregorianCalendar.JANUARY, 30, 23, 59, 0);
	        assertEquals(gc.getTime(), _directory.getDate(9, null));
	        assertEquals(gc.getTime(), _directory.getDate(9, gmt));
	        assertEquals(gc.getTime(), _directory.getDate(10, pst));

	        gc = new GregorianCalendar(gmt);
	        gc.setTimeInMillis(0);

	        gc.set(2002, GregorianCalendar.JANUARY, 30, 0, 0, 0);
	        assertEquals(gc.getTime(), _directory.getDate(11, null));

	        gc.set(2002, GregorianCalendar.JANUARY, 1, 0, 0, 0);
	        assertEquals(gc.getTime(), _directory.getDate(12, null));

	        gc.set(2002, GregorianCalendar.JANUARY, 1, 0, 0, 0);
	        assertEquals(gc.getTime(), _directory.getDate(13, null));
	        System.out.println("Finished running testSetStringAndGetDate()");
	    }

	    //@Test
	    //try {
  		//    dt.testSetIntArrayGetByteArray();
  		//}
  		//catch (Exception e) {
  		//    e.printStackTrace();
  		//}
	    //Finished running testSetIntArrayGetByteArray()
	    public void testSetIntArrayGetByteArray() throws Exception
	    {
	        int[] ints = {1, 2, 3, 4, 5};
	        _directory.setIntArray(1, ints);

	        byte[] bytes = _directory.getByteArray(1);
	        assertNotNull(bytes);
	        assertEquals(ints.length, bytes.length);
	        assertEquals(1, bytes[0]);
	        System.out.println("Finished running testSetIntArrayGetByteArray()");
	    }

	    //@Test
	    //try {
  		//    dt.testSetStringGetInt();
  		//}
  		//catch (Exception e) {
  		//    e.printStackTrace();
  		//}
	    //Finished running testSetStringGetInt()
	    public void testSetStringGetInt() throws Exception
	    {
	        byte[] bytes = { 0x01, 0x02, 0x03 };
	        _directory.setString(1, new String(bytes));
	        assertEquals(0x010203, _directory.getInt(1));
	        System.out.println("Finished running testSetStringGetInt()");
	    }

	    //@Test
	    //try {
  		//    dt.testContainsTag();
  		//}
  		//catch (Exception e) {
  		//    e.printStackTrace();
  		//}
	    //Finished running testContainsTag()
	    public void testContainsTag() throws Exception
	    {
	        assertFalse(_directory.containsTag(ExifSubIFDDirectory.TAG_APERTURE));
	        _directory.setString(ExifSubIFDDirectory.TAG_APERTURE, "Tag Value");
	        assertTrue(_directory.containsTag(ExifSubIFDDirectory.TAG_APERTURE));
	        System.out.println("Finished running testContainsTag()");
	    }

	    //@Test
	    //try {
  		//    dt.testGetNonExistentTagIsNullForAllTypes();
  		//}
  		//catch (Exception e) {
  		//    e.printStackTrace();
  		//}
	    //Finished running testGetNonExistentTagIsNullForAllTypes()
	    public void testGetNonExistentTagIsNullForAllTypes() throws Exception
	    {
	        assertNull(_directory.getString(ExifSubIFDDirectory.TAG_APERTURE));
	        assertNull(_directory.getInteger(ExifSubIFDDirectory.TAG_APERTURE));
	        assertNull(_directory.getDoubleObject(ExifSubIFDDirectory.TAG_APERTURE));
	        assertNull(_directory.getFloatObject(ExifSubIFDDirectory.TAG_APERTURE));
	        assertNull(_directory.getByteArray(ExifSubIFDDirectory.TAG_APERTURE));
	        assertNull(_directory.getDate(ExifSubIFDDirectory.TAG_APERTURE));
	        assertNull(_directory.getIntArray(ExifSubIFDDirectory.TAG_APERTURE));
	        assertNull(_directory.getLongObject(ExifSubIFDDirectory.TAG_APERTURE));
	        assertNull(_directory.getObject(ExifSubIFDDirectory.TAG_APERTURE));
	        assertNull(_directory.getRational(ExifSubIFDDirectory.TAG_APERTURE));
	        assertNull(_directory.getRationalArray(ExifSubIFDDirectory.TAG_APERTURE));
	        assertNull(_directory.getStringArray(ExifSubIFDDirectory.TAG_APERTURE));
	        System.out.println("Finished running testGetNonExistentTagIsNullForAllTypes()");
	    }

	    //@Test
	    //try {
  		//    dt.testToString();
  		//}
  		//catch (Exception e) {
  		//    e.printStackTrace();
  		//}
	    //Finished running testToString()
	    public void testToString()
	    {
	        Directory directory = new ExifIFD0Directory();
	        assertEquals("Exif IFD0 Directory (0 tags)", directory.toString());
	        directory.setString(1, "Tag 1");
	        assertEquals("Exif IFD0 Directory (1 tag)", directory.toString());
	        directory.setString(2, "Tag 2");
	        assertEquals("Exif IFD0 Directory (2 tags)", directory.toString());
	        System.out.println("Finished running testToString()");
	    }
	}

	/**
	 * A mock implementation of Directory used in unit testing.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class MockDirectory extends Directory
	{
	    private final HashMap<Integer, String> _tagNameMap;

	    public MockDirectory()
	    {
	        this._tagNameMap = new HashMap<Integer, String>();
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }
	}
	
	/**
	 * JUnit test case for class Metadata.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class MetadataTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //MetadataTest mt = me.new MetadataTest();
	    //@Test
		//try {
  		//    mt.testGetDirectoryWhenNotExists();
  		//}
  		//catch (Exception e) {
  		//    e.printStackTrace();
  		//}
		//Finished running testGetDirectoryWhenNotExists()
	    public void testGetDirectoryWhenNotExists()
	    {
	        assertNull(new Metadata().getFirstDirectoryOfType(ExifSubIFDDirectory.class));
	        System.out.println("Finished running testGetDirectoryWhenNotExists()");
	    }

	    //@Test
	    //try {
  		//    mt.testHasErrors();
  		//}
  		//catch (Exception e) {
  		//    e.printStackTrace();
  		//}
	    //Finished running testHasErrors()
	    public void testHasErrors() throws Exception
	    {
	        ExifSubIFDDirectory directory = new ExifSubIFDDirectory();
	        directory.addError("Test Error 1");

	        Metadata metadata = new Metadata();
	        assertFalse(metadata.hasErrors());

	        metadata.addDirectory(directory);
	        assertTrue(metadata.hasErrors());
	        System.out.println("Finished running testHasErrors()");
	    }

	    //@Test
	    //try {
  		//    mt.testToString();
  		//}
  		//catch (Exception e) {
  		//    e.printStackTrace();
  		//}
	    //Finished running testToString()
	    public void testToString()
	    {
	        Metadata metadata = new Metadata();
	        assertEquals("Metadata (0 directories)", metadata.toString());

	        metadata.addDirectory(new ExifIFD0Directory());
	        assertEquals("Metadata (1 directory)", metadata.toString());

	        metadata.addDirectory(new ExifSubIFDDirectory());
	        assertEquals("Metadata (2 directories)", metadata.toString());
	        System.out.println("Finished running testToString()");
	    }

	    //@Test
	    //try {
  		//    mt.testOrderOfSameType();
  		//}
  		//catch (Exception e) {
  		//    e.printStackTrace();
  		//}
	    //Finished running testOrderOfSameType()
	    public void testOrderOfSameType()
	    {
	        Metadata metadata = new Metadata();
	        Directory directory2 = new ExifSubIFDDirectory();
	        Directory directory3 = new ExifSubIFDDirectory();
	        Directory directory1 = new ExifSubIFDDirectory();

	        metadata.addDirectory(directory1);
	        metadata.addDirectory(directory2);
	        metadata.addDirectory(directory3);

	        Collection<ExifSubIFDDirectory> directories = metadata.getDirectoriesOfType(ExifSubIFDDirectory.class);

	        assertNotNull(directories);
	        assertEquals(3, directories.size());
	        assertSame(directory1, directories.toArray()[0]);
	        assertSame(directory2, directories.toArray()[1]);
	        assertSame(directory3, directories.toArray()[2]);
	        System.out.println("Finished running testOrderOfSameType()");
	    }

	    //@Test
	    //try {
  		//    mt.testOrderOfDifferentTypes();
  		//}
  		//catch (Exception e) {
  		//    e.printStackTrace();
  		//}
	    //Finished running testOrderOfDifferentTypes()
	    public void testOrderOfDifferentTypes()
	    {
	        Metadata metadata = new Metadata();
	        Directory directory1 = new ExifSubIFDDirectory();
	        Directory directory2 = new ExifThumbnailDirectory();
	        Directory directory3 = new ExifIFD0Directory();

	        metadata.addDirectory(directory1);
	        metadata.addDirectory(directory2);
	        metadata.addDirectory(directory3);

	        List<Directory> directories = new ArrayList<Directory>();
	        for (Directory directory : metadata.getDirectories()) {
	            directories.add(directory);
	        }

	        assertEquals(3, directories.size());
	        assertSame(directory1, directories.toArray()[0]);
	        assertSame(directory2, directories.toArray()[1]);
	        assertSame(directory3, directories.toArray()[2]);
	        System.out.println("Finished running testOrderOfDifferentTypes()");
	    }
	}

	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class PsdReaderTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //PsdReaderTest pt = me.new PsdReaderTest();
	    //@NotNull
	    public PsdHeaderDirectory processBytes(@NotNull String file) throws Exception
	    {
	        Metadata metadata = new Metadata();
	        InputStream stream = new FileInputStream(new File(file));
	        try {
	            new PsdReader().extract(new StreamReader(stream), metadata);
	        } catch (Exception e) {
	            stream.close();
	            throw e;
	        }

	        PsdHeaderDirectory directory = metadata.getFirstDirectoryOfType(PsdHeaderDirectory.class);
	        assertNotNull(directory);
	        return directory;
	    }

	    //@Test
	    //try {
  		//    pt.test8x8x8bitGrayscale();
  		//}
  		//catch (Exception e) {
  		//    e.printStackTrace();
  		//}
	    //Finished running test8x8x8bitGrayscale()
	    public void test8x8x8bitGrayscale() throws Exception
	    {
	        PsdHeaderDirectory directory = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/8x4x8bit-Grayscale.psd");

	        assertEquals(8, directory.getInt(PsdHeaderDirectory.TAG_IMAGE_WIDTH));
	        assertEquals(4, directory.getInt(PsdHeaderDirectory.TAG_IMAGE_HEIGHT));
	        assertEquals(8, directory.getInt(PsdHeaderDirectory.TAG_BITS_PER_CHANNEL));
	        assertEquals(1, directory.getInt(PsdHeaderDirectory.TAG_CHANNEL_COUNT));
	        assertEquals(1, directory.getInt(PsdHeaderDirectory.TAG_COLOR_MODE)); // 1 = grayscale
	        System.out.println("Finished running test8x8x8bitGrayscale()");
	    }

	    //@Test
	    //try {
  		//    pt.test10x12x16bitCMYK();
  		//}
  		//catch (Exception e) {
  		//    e.printStackTrace();
  		//}
	    //Finished running test10x12x16bitCMYK()
	    public void test10x12x16bitCMYK() throws Exception
	    {
	        PsdHeaderDirectory directory = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/10x12x16bit-CMYK.psd");

	        assertEquals(10, directory.getInt(PsdHeaderDirectory.TAG_IMAGE_WIDTH));
	        assertEquals(12, directory.getInt(PsdHeaderDirectory.TAG_IMAGE_HEIGHT));
	        assertEquals(16, directory.getInt(PsdHeaderDirectory.TAG_BITS_PER_CHANNEL));
	        assertEquals(4, directory.getInt(PsdHeaderDirectory.TAG_CHANNEL_COUNT));
	        assertEquals(4, directory.getInt(PsdHeaderDirectory.TAG_COLOR_MODE)); // 4 = CMYK
	        System.out.println("Finished running test10x12x16bitCMYK()");
	    }
	}
	
	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class GifReaderTest
	{
		//MetadataExtractorTest me = new MetadataExtractorTest();
	    //GifReaderTest gt = me.new GifReaderTest();
	    //@NotNull
	    public GifHeaderDirectory processBytes(@NotNull String file) throws Exception
	    {
	        Metadata metadata = new Metadata();
	        InputStream stream = new FileInputStream(file);
	        new GifReader().extract(new StreamReader(stream), metadata);
	        stream.close();

	        GifHeaderDirectory directory = metadata.getFirstDirectoryOfType(GifHeaderDirectory.class);
	        assertNotNull(directory);
	        return directory;
	    }

	    //@Test
	    //try {
  		//    gt.testMsPaintGif();
  		//}
  		//catch (Exception e) {
  		//    e.printStackTrace();
  		//}
	    //Finished running testMsPaintGif()
	    public void testMsPaintGif() throws Exception
	    {
	        GifHeaderDirectory directory = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/mspaint-10x10.gif");

	        assertFalse(directory.hasErrors());

	        assertEquals("89a", directory.getString(GifHeaderDirectory.TAG_GIF_FORMAT_VERSION));
	        assertEquals(10, directory.getInt(GifHeaderDirectory.TAG_IMAGE_WIDTH));
	        assertEquals(10, directory.getInt(GifHeaderDirectory.TAG_IMAGE_HEIGHT));
	        assertEquals(256, directory.getInt(GifHeaderDirectory.TAG_COLOR_TABLE_SIZE));
	        assertFalse(directory.getBoolean(GifHeaderDirectory.TAG_IS_COLOR_TABLE_SORTED));
	        assertEquals(8, directory.getInt(GifHeaderDirectory.TAG_BITS_PER_PIXEL));
	        assertTrue(directory.getBoolean(GifHeaderDirectory.TAG_HAS_GLOBAL_COLOR_TABLE));
	        assertEquals(0, directory.getInt(GifHeaderDirectory.TAG_BACKGROUND_COLOR_INDEX));
	        System.out.println("Finished running testMsPaintGif()");
	    }

	    //@Test
	    //try {
  		//    gt.testPhotoshopGif();
  		//}
  		//catch (Exception e) {
  		//    e.printStackTrace();
  		//}
	    //Finished running testPhotoshopGif()
	    public void testPhotoshopGif() throws Exception
	    {
	        GifHeaderDirectory directory = processBytes("C:/metadata/metadata-extractor-master/Tests/Data/photoshop-8x12-32colors-alpha.gif");

	        assertFalse(directory.hasErrors());

	        assertEquals("89a", directory.getString(GifHeaderDirectory.TAG_GIF_FORMAT_VERSION));
	        assertEquals(8, directory.getInt(GifHeaderDirectory.TAG_IMAGE_WIDTH));
	        assertEquals(12, directory.getInt(GifHeaderDirectory.TAG_IMAGE_HEIGHT));
	        assertEquals(32, directory.getInt(GifHeaderDirectory.TAG_COLOR_TABLE_SIZE));
	        assertFalse(directory.getBoolean(GifHeaderDirectory.TAG_IS_COLOR_TABLE_SORTED));
	        assertEquals(5, directory.getInt(GifHeaderDirectory.TAG_BITS_PER_PIXEL));
	        assertTrue(directory.getBoolean(GifHeaderDirectory.TAG_HAS_GLOBAL_COLOR_TABLE));
	        assertEquals(8, directory.getInt(GifHeaderDirectory.TAG_BACKGROUND_COLOR_INDEX));
	        System.out.println("Finished running testPhotoshopGif()");
	    }
	}


}