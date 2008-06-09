package gov.nih.mipav.model.file;


import gov.nih.mipav.view.MipavUtil;
import gov.nih.mipav.view.Preferences;
import gov.nih.mipav.view.ViewUserInterface;
import gov.nih.mipav.view.dialogs.JDialogAnalyzeNIFTIChoice;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import ncsa.hdf.object.FileFormat;


/**
 * Constants and static methods which relate to file input, output or processing.
 */
public class FileUtility {

    /** New File Types should be added to the bottom and not inserted somewhere in the middle. */

    /** Ill defined file type. */
    public static final int ERROR = -1;

    /** Undefined file type. */
    public static final int UNDEFINED = 0;

    /** AFNI file type. extension: .head, .brik */
    public static final int AFNI = 1;

    /** Analyze format (Mayo). extension: .img, .hdr */
    public static final int ANALYZE = 2;

    /** Multiple files of type analyze. */
    public static final int ANALYZE_MULTIFILE = 3;

    /** AVI file type. Windows Media. extension: .avi */
    public static final int AVI = 4;

    /** Used by the Bio-Rad Pic format. extension: .pic && fileID(54L)==12345 */
    public static final int BIORAD = 5;

    /** extension: .bmp. */
    public static final int BMP = 6;

    /**
     * Bruker file format. Reads a BRUKER file by first reading in the d3proc header file, second the reco header file,
     * third the acqp file int the same directory or up one or two two parent directories, and finally the 2dseq binary
     * file.
     */
    public static final int BRUKER = 7;

    /** Cheshire file type (a kind of Analyze). extension: .imc */
    public static final int CHESHIRE = 8;

    /** Cheshire overlay file type. Contains VOIs. extension: .oly */
    public static final int CHESHIRE_OVERLAY = 9;

    /**
     * Used by FreeSurfer software. extension: -.info or -.info~ for header file -.nnn for slice data file where nnn is
     * the slice number
     */
    public static final int COR = 10;

    /** extension: .cur. */
    public static final int CUR = 11;

    /** extension: .dib. */
    public static final int DIB = 12;

    /** Digital Imaging and COmmunications in Medicine file type. Fully implemented versions 2 & 3. extension: .dcm */
    public static final int DICOM = 13;

    /** Gatan's Digital Micrograph version 3 file format. extension: .dm3 */
    public static final int DM3 = 14;

    /** FITS file type. extension: .fits */
    public static final int FITS = 15;

    /** GE Genesis 5X and LX. extension: .sig */
    public static final int GE_GENESIS = 16;

    /** GE Signa 4.x. */
    public static final int GE_SIGNA4X = 17;

    /** extension: .gif. */
    public static final int GIF = 18;

    /** extension: .ico. */
    public static final int ICO = 19;

    /** Image Cytometry Standard. extension: .ics, .ids */
    public static final int ICS = 20;

    /** Interfile file format used in Nuclear Medicine. extension: .hdr */
    public static final int INTERFILE = 21;

    /** Java Image Manangement Interface file type. */
    public static final int JIMI = 22;

    /** extension: .jpeg, .jpg. */
    public static final int JPEG = 23;

    /** Used by the Zeiss LSM 510 Dataserver. extension: .lsm */
    public static final int LSM = 24;

    /** Used by the Zeiss LSM 510 Dataserver. */
    public static final int LSM_MULTIFILE = 25;

    /** Siemens MAGNETOM VISION. extension: .ima */
    public static final int MAGNETOM_VISION = 26;

    /** Benes Trus special file type. extension: .map */
    public static final int MAP = 27;

    /** extension: .bin. */
    public static final int MEDIVISION = 28;

    /** MGH/MGZ volume format. */
    public static final int MGH = 29;

    /** Micro CT format for small animal imaging. extension: .log, .ct */
    public static final int MICRO_CAT = 30;

    /**
     * MINC file type. MINC is a medical imaging oriented extension of the NetCDF file format. NetCDF stands for
     * 'Network Common Data Form'. extension: .mnc
     */
    public static final int MINC = 31;

    /** Not presently implemented. */
    public static final int MIPAV = 32;

    /** extension: .mrc. */
    public static final int MRC = 33;

    /** NIFTI format. extension: .img, .hdr, .nii */
    public static final int NIFTI = 34;

    /** NIFTI multi-file format. */
    public static final int NIFTI_MULTIFILE = 35;

    /** Nearly raw raster data. */
    public static final int NRRD = 36;

    /** Washington University OSM dataset structure. extension: .wu */
    public static final int OSM = 37;

    /** extension: .pcx. */
    public static final int PCX = 38;

    /** extension: .pic. */
    public static final int PIC = 39;

    /** extension: .pict. */
    public static final int PICT = 40;

    /** extension: .png. */
    public static final int PNG = 41;

    /** MIPAV project format. project file format (.xml) */
    public static final int PROJECT = 42;

    /** extension: .psd. */
    public static final int PSD = 43;

    /** Quicktime file type. extension: .mov, .qt */
    public static final int QT = 44;

    /** RAW image data, no header. extension: .raw */
    public static final int RAW = 45;

    /** RAW MULTIFLE image data, no header. */
    public static final int RAW_MULTIFILE = 46;

    /** SPM file format. extension: .spm */
    public static final int SPM = 47;

    /** MetaMorph Stack (STK) file type. extension: .stk */
    public static final int STK = 48;

    /** MIPAV Surface XML file format. extension: .xml */
    public static final int SURFACE_XML = 49;

    /** extension: .tga. */
    public static final int TGA = 50;

    /** TIFF file; tagged header. extension: .tif, .tiff */
    public static final int TIFF = 51;

    /** Multiple files of TIFF images. */
    public static final int TIFF_MULTIFILE = 52;

    /** Optical coherence tomography. extension: .tmg */
    public static final int TMG = 53;

    /** VOI file, used to read VOIs. extension: .voi */
    public static final int VOI_FILE = 54;

    /** extension: .xbm. */
    public static final int XBM = 55;

    /** MIPAV XML file format. mipav xml image format. extension: .xml */
    public static final int XML = 56;

    /** MIPAV XML file format. */
    public static final int XML_MULTIFILE = 57;

    /** extension: .xpm. */
    public static final int XPM = 58;

    /** extension: "par","parv2","rec","frec". */
    public static final int PARREC = 59;

    /** MIPAV Surface XML file format. extension: .xml */
    public static final int SURFACEREF_XML = 60;

    /** MINC 2.0 (HDF5) */
    public static final int MINC_HDF = 61;

    /** Improvision OpenLab LIFF .liff */
    /** Do not confuse with Leica image file format .lif */
    public static final int LIFF = 62;

    /** Extension: .hdr for header, .bfloat for data */
    public static final int BFLOAT = 63;

    /**
     * Only for FreeSurfer COR volume files Looks in the image directory and returns all images with the same root up to
     * the hyphen, sorted in lexicographical order. Will set the number of images (<code>nImages</code>) for the
     * calling program.
     * 
     * @param fileDir Directory to look for images.
     * @param fileName File name of the image.
     * @param quiet Whether to avoid displaying errors using the GUI.
     * 
     * @return An array of the image names to be read in or saved as.
     * 
     * @throws OutOfMemoryError If there is a problem allocating required memory.
     */
    public static final String[] getCORFileList(String fileDir, String fileName, boolean quiet) throws OutOfMemoryError {
        int i;
        int j = 0;
        int k;
        int result = 0;
        String[] fileList;
        String[] fileList2;
        String[] fileListBuffer;
        String fileTemp;
        File imageDir;
        String fileName2;
        String suffix2;
        boolean okNumber;
        int nImages;

        imageDir = new File(fileDir);

        // Read directory and find no. of images
        fileListBuffer = imageDir.list();
        fileList = new String[fileListBuffer.length];

        String subName = FileUtility.trimCOR(fileName); // subName = name without indexing numbers at end

        for (i = 0; i < fileListBuffer.length; i++) {
            fileName2 = fileListBuffer[i].trim();
            suffix2 = FileUtility.getCORSuffixFrom(fileName2);
            okNumber = true;

            for (k = 1; k < suffix2.length(); k++) {

                if ( !Character.isDigit(suffix2.charAt(k))) {

                    // modified to use Java.lang version 20 July 2004/parsonsd
                    // if ( suffix2.charAt( k ) < '0' || suffix2.charAt( k ) > '9' ) {
                    okNumber = false;
                }
            } // for (k = 0; k < suffix2.length(); k++)

            if (okNumber) {

                if (FileUtility.trimCOR(fileName2).equals(subName)) {
                    fileList[j] = fileListBuffer[i];
                    j++;
                }
            } // if (okNumber)
        } // for (i = 0; i < fileListBuffer.length; i++)

        // Number of images is index of last image read into fileList
        nImages = j;

        if (nImages == 0) {

            if ( !quiet) {
                MipavUtil.displayError("FileIO: No COR images with that base name: " + subName);
            }

            Preferences.debug("FileIO: No COR images with that base name: " + subName + "\n", Preferences.DEBUG_FILEIO);

            return null;
        }

        fileList2 = new String[nImages];

        for (i = 0; i < nImages; i++) {
            fileList2[i] = fileList[i];
        }

        // sort to ensure that files are in correct (lexicographical) order
        for (i = 0; i < nImages; i++) { // (bubble sort? ... )

            for (j = i + 1; j < nImages; j++) {
                result = fileList2[i].compareTo(fileList2[j]);

                if (result > 0) {
                    fileTemp = fileList2[i];
                    fileList2[i] = fileList2[j];
                    fileList2[j] = fileTemp;
                } // if (result > 0)
            } // for (j = i+1; j < nImages; j++)
        } // for (i = 0; i < nImages; i++)

        return fileList2;
    }

    /**
     * Only used for COR volume files with hyphen in name Breaks the filename into basename and suffix, then returns the
     * suffix.
     * 
     * @param fn The filename.
     * 
     * @return The suffix or file-extension. For example,
     *         <q>-info</q>. Note that suffix includes the separator '-'
     */
    public static final String getCORSuffixFrom(String fn) {
        int s;
        String sfx = "";

        if (fn != null) {
            s = fn.lastIndexOf("-");

            if (s != -1) {
                sfx = fn.substring(s);
            }
        }

        return sfx.toLowerCase();
    }

    /**
     * Returns the extension of the file name, if file name does not have extension, then return empty string.
     * 
     * @param absolutePath the file name.
     * 
     * @return The file's extension.
     */
    public static final String getExtension(String absolutePath) {

        if ( (absolutePath == null) || (absolutePath.length() == 0)) {
            return "";
        }

        int index = absolutePath.lastIndexOf(".");

        if (index >= 0) {
            return absolutePath.substring(index);
        }

        return "";
    }

    /**
     * Returns the path information from the file name with the path information.
     * 
     * @param fileName the file name wiht the path information.
     * 
     * @return The path information.
     */
    public static final String getFileDirectory(String fileName) {

        if ( (fileName == null) || (fileName.length() == 0)) {
            return null;
        }

        int index = fileName.lastIndexOf(File.separator);

        if (index >= 0) {
            return fileName.substring(0, index + 1);
        }

        return null;
    }

    /**
     * Trims off the file extension and file name, but leaves the file index. An index might be 0001, or 140, for
     * example.
     * 
     * @param fName String file name to get index
     * 
     * @return String (index string)
     */
    public static final int getFileIndex(String fName) {
        int i;

        // char ch;
        int length = fName.lastIndexOf("."); // Start before suffix.

        for (i = length - 1; i > -1; i--) {

            if ( !Character.isDigit(fName.charAt(i))) {
                break;
            }
        }

        if (i <= -1) {
            return -1;
        }

        return (new Integer(fName.substring( (i + 1), length)).intValue());
    }

    /**
     * Looks in the image directory and returns all images with the same suffix as <code>fileName</code>, sorted in
     * lexicographical order.
     * 
     * @param fileDir Directory to look for images.
     * @param fileName File name of the image.
     * @param quiet Whether to avoid displaying errors using the GUI.
     * 
     * @return An array of the image names to be read in or saved as.
     * 
     * @throws OutOfMemoryError If there is a problem allocating required memory.
     */
    public static final String[] getFileList(String fileDir, String fileName, boolean quiet) throws OutOfMemoryError {
        int i;
        int j = 0;
        int k;
        int result = 0;
        File[] files;
        String[] fileList;
        String[] fileList2;
        String[] fileListBuffer;
        String fileTemp;
        File imageDir;
        boolean numberSuffix = true;
        String fileName2;
        String suffix2;
        boolean okNumber;
        int nImages;

        imageDir = new File(fileDir);

        // Read directory and find no. of images
        files = imageDir.listFiles();
        fileListBuffer = new String[files.length];

        for (i = 0; i < files.length; i++) {
            fileListBuffer[i] = files[i].getName();
        }

        fileList = new String[fileListBuffer.length];

        String subName = FileUtility.trimNumbersAndSpecial(fileName); // subName = name without indexing numbers at
        // end
        String suffix = FileUtility.getExtension(fileName); // suffix = ie. .ima or .img ...

        // System.out.println( "Suffix = _" + suffix + "_" + " subName = " + subName);

        for (i = 1; i < suffix.length(); i++) {

            if ( !Character.isDigit(suffix.charAt(i))) {

                // modified to use Java.lang check 20 July 2004/parsonsd
                // if ( suffix.charAt( i ) < '0' || suffix.charAt( i ) > '9' ) {
                numberSuffix = false;
            }
        }

        // added this check in to set number suffix to false (for no suffix'd DICOMs)
        if (suffix.equals("")) {
            numberSuffix = false;
        }

        if (numberSuffix) { // .12 is an example of a number suffix

            for (i = 0; i < fileListBuffer.length; i++) {

                if ( !files[i].isDirectory()) {
                    fileName2 = fileListBuffer[i].trim();
                    suffix2 = FileUtility.getExtension(fileName2);

                    // System.out.println( "Suffix2 = _" + suffix2 + "_" );
                    okNumber = true;

                    for (k = 1; k < suffix2.length(); k++) {

                        if ( !Character.isDigit(suffix2.charAt(k))) {

                            // modified to use Java.lang check 20 July 2004/parsonsd
                            // if ( suffix2.charAt( k ) < '0' || suffix2.charAt( k ) > '9' ) {
                            okNumber = false;
                        }
                    } // for (k = 0; k < suffix2.length(); k++)

                    if (okNumber && (suffix2.length() > 0)) {

                        if (FileUtility.trimNumbersAndSpecial(fileName2).equals(subName)) {
                            fileList[j] = fileListBuffer[i];
                            j++;
                        }
                    } // if (okNumber)
                } // if (!files[i].isDirectory())
            } // for (i = 0; i <fileListBuffer.length; i++)
        } // if (numberSuffix)
        else if (suffix.equals("")) {

            for (i = 0; i < fileListBuffer.length; i++) {

                if ( !files[i].isDirectory()) {
                    String fileSubName = FileUtility.trimNumbersAndSpecial(fileListBuffer[i].trim());
                    String fileExtension = FileUtility.getExtension(fileListBuffer[i]);

                    if (fileSubName.trim().equals(subName) && fileExtension.equalsIgnoreCase(suffix)) {
                        fileList[j] = fileListBuffer[i];
                        j++;
                    }

                }
            }

        } else { // numberSuffix == false. I.e. ".img".

            // check to see that they end in suffix. If so, store, count.

            for (i = 0; i < fileListBuffer.length; i++) {

                if ( !files[i].isDirectory()) {

                    if (fileListBuffer[i].trim().toLowerCase().endsWith(suffix.toLowerCase())) { // note: not case
                        // sensitive!

                        if (FileUtility.trimNumbersAndSpecial(fileListBuffer[i].trim()).equals(subName)) {
                            fileList[j] = fileListBuffer[i];
                            j++;
                        }
                    }
                }
            }
        } // numberSuffix == false

        // Number of images is index of last image read into fileList
        nImages = j;

        if (nImages == 0) {

            if ( !quiet) {
                MipavUtil.displayError("FileIO: No images with that suffix: " + suffix);
            }

            Preferences.debug("FileIO: No images with that suffix: " + suffix + "\n", Preferences.DEBUG_FILEIO);

            return null;
        }

        fileList2 = new String[nImages];

        for (i = 0; i < nImages; i++) {
            fileList2[i] = fileList[i];
        }

        // sort to ensure that files are in correct (lexicographical) order
        for (i = 0; i < nImages; i++) { // (bubble sort? ... )

            for (j = i + 1; j < nImages; j++) {
                result = FilenameSorter.compareToLastNumericalSequence(fileList2[i], fileList2[j]); // compare based on
                // last numerical
                // sequence
                // result =
                // fileList2[i].compareTo(
                // fileList2[j] );

                if (result > 0) {
                    fileTemp = fileList2[i];
                    fileList2[i] = fileList2[j];
                    fileList2[j] = fileTemp;
                } // end of if (result > 0)
            } // end of for (j = i+1; j < nImages; j++)
        } // end of for (i = 0; i < nImages; i++)

        return fileList2;
    }

    /**
     * Returns the file name without path information from file name with the path information.
     * 
     * @param absolutePath the file name with the path information.
     * 
     * @return The file name without path information.
     */
    public static final String getFileName(String absolutePath) {

        if ( (absolutePath == null) || (absolutePath.length() == 0)) {
            return null;
        }

        int index = absolutePath.lastIndexOf(File.separator);

        if (index >= 0) {

            if (index == (absolutePath.length() - 1)) {
                return null;
            }

            return absolutePath.substring(index + 1);
        }

        return absolutePath;
    }

    /**
     * Sets the FileBase.(filetype) based on the file extension of the given filename. Also sets file "suffix", if
     * required.
     * 
     * @param fileName Filename of the image to read in. Must include the file extension.
     * @param fileDir Directory where fileName exists.
     * @param quiet Whether to avoid any user interaction (ie, from error popups).
     * 
     * @return Filetype from FileBase.
     * 
     * @see FileBase
     */
    public static final int getFileType(String fileName, String fileDir, boolean quiet) {
        return getFileType(fileName, fileDir, false, quiet);
    }

    /**
     * Sets the FileBase.(filetype) based on the file extension of the given filename. Also sets file "suffix", if
     * required.
     * 
     * @param fileName Filename of the image to read in. Must include the file extension.
     * @param fileDir Directory where fileName exists.
     * @param doWrite If true about to write a file
     * @param quiet Whether to avoid any user interaction (ie, from error popups).
     * 
     * @return Filetype from FileBase.
     * 
     * @see FileBase
     */
    public static final int getFileType(String fileName, String fileDir, boolean doWrite, boolean quiet) {
        int fileType;
        int i;

        String beginString = FileUtility.stripExtension(fileName);

        if ( (beginString.equalsIgnoreCase("d3proc")) || (beginString.equalsIgnoreCase("reco"))
                || (beginString.equalsIgnoreCase("2dseq"))) {
            fileType = FileUtility.BRUKER;

            return fileType;
        }

        fileName.trim();

        String suffix = FileUtility.getExtension(fileName);

        fileType = FileTypeTable.getFileTypeFromSuffix(suffix);

        // check to see if we're being asked to look at an empty file
        if (new File(fileDir + File.separator + fileName).length() == 0) {
            return fileType;
        }

        // handle when the .mnc extension but MINC_HDF file
        if (fileType == FileUtility.MINC) {
            try {
                // inspect the file to see if it is really a MINC1 (suppressing any error dialogs).
                // if not, set the file type using isMincHDF()
                if (isMinc(fileName, fileDir, true) != FileUtility.MINC) {
                    fileType = isMincHDF(fileName, fileDir, quiet);
                }
            } catch (IOException ioe) {
                if (ioe instanceof FileNotFoundException) {
                    MipavUtil.displayError("File does not exist '" + fileDir + fileName + "'.");
                    ioe.printStackTrace();

                    return FileUtility.ERROR;
                }

                if ( !quiet) {
                    MipavUtil.displayError("FileIO: " + ioe);
                    Preferences.debug("FileIO: " + ioe + "\n", Preferences.DEBUG_FILEIO);
                    ioe.printStackTrace();
                } else {
                    Preferences.debug("FileIO: " + ioe + "\n", Preferences.DEBUG_FILEIO);
                    ioe.printStackTrace();
                }

                fileType = FileUtility.UNDEFINED;
            }
        }

        if (fileType == FileUtility.UNDEFINED) {

            if (suffix.equalsIgnoreCase(".pic")) {

                // Both Biorad and JIMI use the pic suffix
                try {
                    File file = new File(fileDir + fileName);
                    RandomAccessFile raFile = new RandomAccessFile(file, "r");

                    raFile.seek(54L);

                    // little endian unsigned short
                    int b1 = raFile.readUnsignedByte();
                    int b2 = raFile.readUnsignedByte();
                    int fileID = ( (b2 << 8) | b1); // Little Endian

                    raFile.close();

                    if (fileID == 12345) {
                        fileType = FileUtility.BIORAD;
                    } else {
                        fileType = FileUtility.JIMI;
                    }
                } catch (OutOfMemoryError error) {
                    System.gc();
                } catch (FileNotFoundException e) {
                    System.gc();
                } catch (IOException e) {
                    System.gc();
                }
            } else if (suffix.equalsIgnoreCase(".img")) {

                // ANALYZE, Interfile, and NIFTI use .img and .hdr
                if (doWrite) {
                    JDialogAnalyzeNIFTIChoice choice = new JDialogAnalyzeNIFTIChoice(ViewUserInterface.getReference()
                            .getMainFrame());

                    if ( !choice.okayPressed()) {
                        fileType = FileUtility.ERROR;
                    } else {
                        fileType = choice.fileType();
                    }
                } else { // read

                    int p = fileName.lastIndexOf(".");
                    String fileHeaderName = fileName.substring(0, p + 1) + "hdr";
                    String headerFile = FileInterfile.isInterfile(fileHeaderName, fileDir);
                    if (headerFile != null) {
                        fileType = FileUtility.INTERFILE;
                    } else {
                        fileType = FileUtility.ANALYZE;

                        try {
                            File file = new File(fileDir + fileHeaderName);
                            RandomAccessFile raFile = new RandomAccessFile(file, "r");

                            raFile.seek(344L);

                            char[] niftiName = new char[4];

                            for (i = 0; i < 4; i++) {
                                niftiName[i] = (char) raFile.readUnsignedByte();
                            }

                            raFile.close();

                            if ( (niftiName[0] == 'n') && ( (niftiName[1] == 'i') || (niftiName[1] == '+'))
                                    && (niftiName[2] == '1') && (niftiName[3] == '\0')) {
                                fileType = FileUtility.NIFTI;
                            }
                        } catch (OutOfMemoryError error) {
                            System.gc();
                        } catch (FileNotFoundException e) {
                            System.gc();
                        } catch (IOException e) {
                            System.gc();
                        }
                    }
                }
            } else if (suffix.equalsIgnoreCase(".hdr")) {
                if (doWrite) {
                    // ANALYZE, Interfile, and NIFTI use .img and .hdr
                    JDialogAnalyzeNIFTIChoice choice = new JDialogAnalyzeNIFTIChoice(ViewUserInterface.getReference()
                            .getMainFrame());

                    if ( !choice.okayPressed()) {
                        fileType = FileUtility.ERROR;
                    } else {
                        fileType = choice.fileType();
                    }
                } else { // read
                    int p = fileName.lastIndexOf(".");
                    String bfloatDataName = fileName.substring(0, p + 1) + "bfloat";
                    File bfloatFile = new File(fileDir + bfloatDataName);
                    if (bfloatFile.exists()) {
                        fileType = FileUtility.BFLOAT;
                    } else {
                        String headerFile = FileInterfile.isInterfile(fileName, fileDir);
                        if (headerFile != null) {
                            fileType = FileUtility.INTERFILE;
                        } else {
                            fileType = FileUtility.ANALYZE;

                            try {
                                File file = new File(fileDir + fileName);
                                RandomAccessFile raFile = new RandomAccessFile(file, "r");

                                raFile.seek(344L);

                                char[] niftiName = new char[4];

                                for (i = 0; i < 4; i++) {
                                    niftiName[i] = (char) raFile.readUnsignedByte();
                                }

                                raFile.close();

                                if ( (niftiName[0] == 'n') && ( (niftiName[1] == 'i') || (niftiName[1] == '+'))
                                        && (niftiName[2] == '1') && (niftiName[3] == '\0')) {
                                    fileType = FileUtility.NIFTI;
                                }
                            } catch (OutOfMemoryError error) {
                                System.gc();
                            } catch (FileNotFoundException e) {
                                System.gc();
                            } catch (IOException e) {
                                System.gc();
                            }
                        }
                    }
                }
            } else if (suffix.equalsIgnoreCase(".ima")) {

                // Both Dicom and Siemens Magnetom Vision file type have the ima suffix
                try {
                    File file = new File(fileDir + fileName);
                    RandomAccessFile raFile = new RandomAccessFile(file, "r");

                    raFile.seek(281L);

                    char[] ModelName = new char[15];

                    for (i = 0; i < 15; i++) {
                        ModelName[i] = (char) raFile.readUnsignedByte();
                    }

                    raFile.close();

                    String ModelNameString = new String(ModelName);

                    if (ModelNameString.equals("MAGNETOM VISION")) {
                        fileType = FileUtility.MAGNETOM_VISION;
                    } else {
                        fileType = FileUtility.DICOM;
                    }
                } catch (OutOfMemoryError error) {
                    System.gc();
                } catch (FileNotFoundException e) {
                    System.gc();
                } catch (IOException e) {
                    System.gc();
                }
            } else if (suffix.equalsIgnoreCase("") && doWrite) {
                Preferences.debug("FileIO: Cannot save a file without an extension: " + fileName + "\n",
                        Preferences.DEBUG_FILEIO);

                return FileUtility.UNDEFINED;
            }
        }

        String strFileType = Preferences.getProperty(Preferences.PREF_USER_FILETYPE_ASSOC);
        String[] assoc = new String[0];

        if (strFileType != null) {
            assoc = strFileType.split(Preferences.ITEM_SEPARATOR);
        }

        // check to see if there are any user defined associations
        for (int k = 0; k < assoc.length; k++) {

            if (suffix.equals(assoc[k].split(Preferences.DEFINITION_SEPARATOR)[0])) {
                fileType = new Integer(assoc[k].split(Preferences.DEFINITION_SEPARATOR)[1]).intValue();
            }
        }

        try {

            if (fileType == FileUtility.UNDEFINED) {
                fileType = FileUtility.isDicom(fileName, fileDir, quiet);
            }

            if (fileType == FileUtility.UNDEFINED) {
                fileType = FileUtility.isGESigna4X(fileName, fileDir, quiet);
            }

            if (fileType == FileUtility.UNDEFINED) {
                fileType = FileUtility.isGESigna5X(fileName, fileDir, quiet);
            }

            if (fileType == FileUtility.UNDEFINED) {
                fileType = FileUtility.isMagnetomVision(fileName, fileDir, quiet);
            }

            if (fileType == FileUtility.UNDEFINED) {
                fileType = FileUtility.isMinc(fileName, fileDir, quiet);
            }

            if (fileType == FileUtility.UNDEFINED) {
                fileType = isMincHDF(fileName, fileDir, quiet);
            }

            if (fileType == FileUtility.UNDEFINED) {

                fileType = FileUtility.isAnalyze(fileName, fileDir, quiet);
            }

            if (fileType == FileUtility.UNDEFINED) {
                fileType = FileUtility.isInterfile(fileName, fileDir, quiet);
            }

            if (fileType == FileUtility.UNDEFINED) {
                fileType = FileUtility.isNIFTI(fileName, fileDir, quiet);
            }

            if (fileType == FileUtility.UNDEFINED) {
                fileType = FileUtility.isSPM(fileName, fileDir, quiet);
            }
        } catch (IOException ioe) {

            if (ioe instanceof FileNotFoundException) {
                MipavUtil.displayError("File does not exist '" + fileDir + fileName + "'.");
                ioe.printStackTrace();

                return FileUtility.ERROR;
            }

            if ( !quiet) {
                MipavUtil.displayError("FileIO: " + ioe);
                Preferences.debug("FileIO: " + ioe + "\n", Preferences.DEBUG_FILEIO);
                ioe.printStackTrace();
            } else {
                Preferences.debug("FileIO: " + ioe + "\n", Preferences.DEBUG_FILEIO);
                ioe.printStackTrace();
            }

            fileType = FileUtility.UNDEFINED;
        }

        return fileType;
    }

    /**
     * Tests if the unknown file is of type Analyze.
     * 
     * @param fileName Name of the image file to read.
     * @param fileDir Directory of the image file to read.
     * @param quiet Whether to avoid any user interaction (ie, from error popups).
     * 
     * @return <code>FileUtility.ANALYZE</code> if the file is a ANALYZE type, and <code>FileUtility.UNDEFINED</code>
     *         otherwise
     * 
     * @throws IOException If there is a problem determining the type of the given file.
     */
    public static final int isAnalyze(String fileName, String fileDir, boolean quiet) throws IOException {

        try {

            boolean isAnalyze = FileAnalyze.isAnalyze(fileDir + fileName);

            if (isAnalyze) {
                return FileUtility.ANALYZE;
            }

            return FileUtility.UNDEFINED;
        } catch (OutOfMemoryError error) {

            if ( !quiet) {
                MipavUtil.displayError("FileIO: " + error);
                Preferences.debug("FileIO: " + error + "\n", Preferences.DEBUG_FILEIO);
            } else {
                Preferences.debug("FileIO: " + error + "\n", Preferences.DEBUG_FILEIO);
            }

            return FileUtility.UNDEFINED;
        }
    }

    /**
     * Tests if the unknown file is of type Dicom.
     * 
     * @param fileName Name of the image file to read.
     * @param fileDir Directory of the image file to read.
     * @param quiet Whether to avoid any user interaction (ie, from error popups).
     * 
     * @return <code>FileUtility.DICOM</code> if the file is a DICOM file, and <code>FileUtility.UNDEFINED</code>
     *         otherwise
     * 
     * @throws IOException If there is a problem determining the type of the given file.
     */
    public static final int isDicom(String fileName, String fileDir, boolean quiet) throws IOException {

        try {
            FileDicom imageFile = new FileDicom(fileName, fileDir);

            if (imageFile != null) {
                boolean isDicom = imageFile.isDICOM();

                imageFile.close();
                imageFile = null;

                if (isDicom) {
                    return FileUtility.DICOM;
                }
            }

            return FileUtility.UNDEFINED;
        } catch (OutOfMemoryError error) {

            if ( !quiet) {
                MipavUtil.displayError("FileIO: " + error);
                Preferences.debug("FileIO: " + error + "\n", Preferences.DEBUG_FILEIO);
            } else {
                Preferences.debug("FileIO: " + error + "\n", Preferences.DEBUG_FILEIO);
            }

            return FileUtility.UNDEFINED;
        }

    }

    /**
     * Tests if the unknown file is of type GE Signa 4X type.
     * 
     * @param fileName Name of the image file to read.
     * @param fileDir Directory of the image file to read.
     * @param quiet Whether to avoid any user interaction (ie, from error popups).
     * 
     * @return <code>FileUtility.GE_SIGNA4X</code> if the file is a GE MR Signa 4.x file, and <code>
     *          FileUtility.UNDEFINED</code>
     *         otherwise
     * 
     * @throws IOException If there is a problem determining the type of the given file.
     */
    public static final int isGESigna4X(String fileName, String fileDir, boolean quiet) throws IOException {

        try {
            FileGESigna4X imageFile = new FileGESigna4X(fileName, fileDir);

            if (imageFile != null) {
                boolean isGESigna4X = imageFile.isGESigna4X();

                if (isGESigna4X) {
                    return FileUtility.GE_SIGNA4X;
                }
            }

            return FileUtility.UNDEFINED;
        } catch (OutOfMemoryError error) {

            if ( !quiet) {
                MipavUtil.displayError("FileIO: " + error);
                Preferences.debug("FileIO: " + error + "\n", Preferences.DEBUG_FILEIO);
            } else {
                Preferences.debug("FileIO: " + error + "\n", Preferences.DEBUG_FILEIO);
            }

            return FileUtility.UNDEFINED;
        }
    }

    /**
     * Tests if the unknown file is of type GE Signa 5X type.
     * 
     * @param fileName Name of the image file to read.
     * @param fileDir Directory of the image file to read.
     * @param quiet Whether to avoid any user interaction (ie, from error popups).
     * 
     * @return <code>FileUtility.GE_GENESIS</code> if the file is a GE MR Signa 5.x file, and <code>
     *          FileUtility.UNDEFINED</code>
     *         otherwise
     * 
     * @throws IOException If there is a problem determining the type of the given file.
     */
    public static final int isGESigna5X(String fileName, String fileDir, boolean quiet) throws IOException {

        try {
            FileGESigna5X imageFile = new FileGESigna5X(fileName, fileDir);

            if (imageFile != null) {
                boolean isGESigna5X = imageFile.isGESigna5X();

                if (isGESigna5X) {
                    return FileUtility.GE_GENESIS;
                }
            }

            return FileUtility.UNDEFINED;
        } catch (OutOfMemoryError error) {

            if ( !quiet) {
                MipavUtil.displayError("FileIO: " + error);
                Preferences.debug("FileIO: " + error + "\n", Preferences.DEBUG_FILEIO);
            } else {
                Preferences.debug("FileIO: " + error + "\n", Preferences.DEBUG_FILEIO);
            }

            return FileUtility.UNDEFINED;
        }
    }

    /**
     * Tests if the unknown file is of type Interfile.
     * 
     * @param fileName Name of the image file to read.
     * @param fileDir Directory of the image file to read.
     * @param quiet Whether to avoid any user interaction (ie, from error popups).
     * 
     * @return <code>FileUtility.Interfile</code> if the file is a Interfile type, and
     *         <code>FileUtility.UNDEFINED</code> otherwise
     * 
     * @throws IOException If there is a problem determining the type of the given file.
     */
    public static final int isInterfile(String fileName, String fileDir, boolean quiet) throws IOException {

        try {

            String fileHeaderName = FileInterfile.isInterfile(fileName, fileDir);

            if (fileHeaderName != null) {
                return FileUtility.INTERFILE;
            }

            return FileUtility.UNDEFINED;
        } catch (OutOfMemoryError error) {

            if ( !quiet) {
                MipavUtil.displayError("FileIO: " + error);
                Preferences.debug("FileIO: " + error + "\n", Preferences.DEBUG_FILEIO);
            } else {
                Preferences.debug("FileIO: " + error + "\n", Preferences.DEBUG_FILEIO);
            }

            return FileUtility.UNDEFINED;
        }
    }

    /**
     * Tests if the unknown file is of type Siemens Magnetom Vision.
     * 
     * @param fileName Name of the image file to read.
     * @param fileDir Directory of the image file to read.
     * @param quiet Whether to avoid any user interaction (ie, from error popups).
     * 
     * @return <code>FileUtility.MAGNETOM_VISION</code> if the file is a Siemens Magnetom Vision type, and <code>
     *          FileUtility.UNDEFINED</code>
     *         otherwise
     * 
     * @throws IOException If there is a problem determining the type of the given file.
     */
    public static final int isMagnetomVision(String fileName, String fileDir, boolean quiet) throws IOException {

        try {
            FileMagnetomVision imageFile = new FileMagnetomVision(fileName, fileDir);

            if (imageFile != null) {
                boolean isMagnetomVision = imageFile.isMagnetomVision();

                if (isMagnetomVision) {
                    return FileUtility.MAGNETOM_VISION;
                }
            }

            return FileUtility.UNDEFINED;
        } catch (OutOfMemoryError error) {

            if ( !quiet) {
                MipavUtil.displayError("FileIO: " + error);
                Preferences.debug("FileIO: " + error + "\n", Preferences.DEBUG_FILEIO);
            } else {
                Preferences.debug("FileIO: " + error + "\n", Preferences.DEBUG_FILEIO);
            }

            return FileUtility.UNDEFINED;
        }
    }

    /**
     * Tests if the unknown file is of type Minc.
     * 
     * @param fileName Name of the image file to read.
     * @param fileDir Directory of the image file to read.
     * @param quiet Whether to avoid any user interaction (ie, from error popups).
     * 
     * @return <code>FileUtility.MINC</code> if the file is a MINC type, and <code>FileUtility.UNDEFINED</code>
     *         otherwise
     * 
     * @throws IOException If there is a problem determining the type of the given file.
     */
    public static final int isMinc(String fileName, String fileDir, boolean quiet) throws IOException {

        try {
            FileMinc imageFile = new FileMinc(fileName, fileDir);

            if (imageFile != null) {
                boolean isMinc = imageFile.isMinc();

                if (isMinc) {
                    return FileUtility.MINC;
                }
            }

            return FileUtility.UNDEFINED;
        } catch (OutOfMemoryError error) {

            if ( !quiet) {
                MipavUtil.displayError("FileIO: " + error);
                Preferences.debug("FileIO: " + error + "\n", Preferences.DEBUG_FILEIO);
            } else {
                Preferences.debug("FileIO: " + error + "\n", Preferences.DEBUG_FILEIO);
            }

            return FileUtility.UNDEFINED;
        }
    }

    /**
     * Determines whether the file on disk is of type MINC 2.0
     * 
     * @param fileName name of the file
     * @param fileDir directory
     * @param quiet
     * @return whether the file is HDF5 type
     */
    public static final int isMincHDF(String fileName, String fileDir, boolean quiet) {
        try {
            FileFormat h5F = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);

            // if the FileFormat object is null, there was probably a problem loading the hdf5 libraries
            if (h5F == null) {
                if ( !quiet) {
                    MipavUtil.displayError("Unable to load HDF5 libraries required for MINC-2.0 HDF files.");
                }

                return FileUtility.ERROR;
            }

            boolean isMincHDF = h5F.isThisType(fileDir + File.separator + fileName);
            if (isMincHDF) {
                return FileUtility.MINC_HDF;
            } else {
                return FileUtility.UNDEFINED;
            }
        } catch (NoClassDefFoundError e) {
            if ( !quiet) {
                MipavUtil.displayError("Unable to load HDF libraries: " + e.getMessage());
            }

            e.printStackTrace();

            return FileUtility.ERROR;
        } catch (SecurityException e) {
            if ( !quiet) {
                MipavUtil.displayError("Unable to load HDF libraries: " + e.getMessage());
            }

            e.printStackTrace();

            return FileUtility.ERROR;
        } catch (UnsatisfiedLinkError e) {
            if ( !quiet) {
                MipavUtil.displayError("Unable to load HDF libraries: " + e.getMessage());
            }

            e.printStackTrace();

            return FileUtility.ERROR;
        }
    }

    /**
     * Tests if the unknown file is of type Nifti.
     * 
     * @param fileName Name of the image file to read.
     * @param fileDir Directory of the image file to read.
     * @param quiet Whether to avoid any user interaction (ie, from error popups).
     * 
     * @return <code>FileUtility.NIFTI</code> if the file is a NIFTI type, and <code>FileUtility.UNDEFINED</code>
     *         otherwise
     * 
     * @throws IOException If there is a problem determining the type of the given file.
     */
    public static final int isNIFTI(String fileName, String fileDir, boolean quiet) throws IOException {

        try {
            boolean isNIFTI = FileNIFTI.isNIFTI(fileName, fileDir);

            if (isNIFTI) {
                return FileUtility.NIFTI;
            }

            return FileUtility.UNDEFINED;
        } catch (OutOfMemoryError error) {

            if ( !quiet) {
                MipavUtil.displayError("FileIO: " + error);
                Preferences.debug("FileIO: " + error + "\n", Preferences.DEBUG_FILEIO);
            } else {
                Preferences.debug("FileIO: " + error + "\n", Preferences.DEBUG_FILEIO);
            }

            return FileUtility.UNDEFINED;
        }
    }

    /**
     * Tests if the unknown file is of type SPM.
     * 
     * @param fileName Name of the image file to read.
     * @param fileDir Directory of the image file to read.
     * @param quiet Whether to avoid any user interaction (ie, from error popups).
     * 
     * @return <code>FileUtility.SPM</code> if the file is a SPM type, and <code>FileUtility.UNDEFINED</code>
     *         otherwise
     * 
     * @throws IOException If there is a problem determining the type of the given file.
     */
    public static final int isSPM(String fileName, String fileDir, boolean quiet) throws IOException {

        try {
            boolean isSPM = FileSPM.isSPM(fileDir + fileName);

            if (isSPM) {
                return FileUtility.SPM;
            }

            return FileUtility.UNDEFINED;
        } catch (OutOfMemoryError error) {

            if ( !quiet) {
                MipavUtil.displayError("FileIO: " + error);
                Preferences.debug("FileIO: " + error + "\n", Preferences.DEBUG_FILEIO);
            } else {
                Preferences.debug("FileIO: " + error + "\n", Preferences.DEBUG_FILEIO);
            }

            return FileUtility.UNDEFINED;
        }
    }

    /**
     * Helper method to strip the image name of the extension, so when we save we don't have double extensions (like
     * genormcor.img.tif).
     * 
     * @param fileName Original name.
     * 
     * @return Name without extension, or original name if there was no extension.
     */
    public static final String stripExtension(String fileName) {
        int index = fileName.lastIndexOf(".");

        if (index != -1) {
            return fileName.substring(0, index);
        } else {
            return fileName;
        }
    }

    /**
     * Trims the numbers or file extension from COR file names. Any numbers or
     * <q>.info</q>
     * or
     * <q>.info~</q>
     * will be removed from after a hyphen in the given fname.
     * 
     * @param fName File name where the last characters are alpha-numerics indicating the image number or .info or
     *            .info~
     * 
     * @return File name without numbers on the end.
     */
    public static final String trimCOR(String fName) {
        int length = fName.lastIndexOf("-");

        if (length >= 0) {
            return (new String(fName.substring(0, length + 1)));
        } else {
            return null;
        }
    }

    /**
     * Trims the numbers and special character from the file name. Numerics and some special characters <code>[ - _
     * .</code>
     * are removed from the end of the file.
     * 
     * @param fName File name where the last characters are alpha-numerics indicating the image number.
     * 
     * @return File name without numbers on the end.
     */
    public static final String trimNumbersAndSpecial(String fName) {
        int i;
        char ch;
        int length = fName.lastIndexOf("."); // Start before suffix.

        for (i = length - 1; i > -1; i--) {
            ch = fName.charAt(i);

            if ( !Character.isDigit(ch) && (ch != '-') && (ch != '.') && (ch != '_')) {
                break;
            }
        }

        String tmpStr;

        if (length == -1) {
            tmpStr = fName;
        } else {
            tmpStr = fName.substring(0, i + 1);
        }

        boolean aCharIsPresent = false;

        // Determine if at least one letter is present
        for (i = 0; i < tmpStr.length(); i++) {
            ch = tmpStr.charAt(i);

            if (Character.isLetter(ch)) {
                aCharIsPresent = true;

                break;
            }
        }

        char fillChar = 'a';

        // If yes, then remove remaining numbers
        if (aCharIsPresent) {

            for (i = 0; i < tmpStr.length(); i++) {
                ch = tmpStr.charAt(i);

                if (Character.isDigit(ch)) {
                    tmpStr = tmpStr.replace(ch, fillChar);
                }
            }
        }

        return (tmpStr);
    }
}
