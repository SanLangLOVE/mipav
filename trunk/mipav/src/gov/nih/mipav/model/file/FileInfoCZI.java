package gov.nih.mipav.model.file;


import gov.nih.mipav.model.structures.*;
import gov.nih.mipav.view.dialogs.*;


/**
 * File info storage container.
 */
public class FileInfoCZI extends FileInfoBase {
	
	//~ Static fields/initializers -------------------------------------------------------------------------------------
	private String focusPosition = null;
	private String acquisitionTime = null;
	private String stageXPosition = null;
	private String stageYPosition = null;
	private String validBitsPerPixel = null;
	private double timeStamps[] = null;
	private String imageName = null;
	private String author = null;
	private String userName = null;
	private String subType = null;
	private String title = null;
	private String creationDate = null;
	private String description = null;
	private String thumbnail = null;
	private String comment = null;
	private String rating = null;
	private String keywords = null;
	private String ID = null;
	private String displayName = null;
	private String firstName = null;
	private String middleName = null;
	private String lastName = null;
	private String email = null;
	private String institution = null;
	private String experimenterName = null;
	private String phone = null;
	private String fax = null;
	private String address = null;
	private String city = null;
	private String country = null;
	private String state = null;
	// Number of phases
	private int dimH = -1;
	// Number of rotation angles (indices
	private int dimR = -1;
	// Number of scenes
	private int dimS = -1;
	// Number of illumination direction indices
	private int dimI = -1;
	// Number of mosaic tiles (regular mosaics only)
	private int dimM = -1;
	// Number of acquisition / recording / blocks
	private int dimB = -1;
	// Number of views in a multi-view image
	private int dimV = -1;
	private String originalScanData = null;
	private int channelsFound = -1;
	private String channelID[] = null;
	private String channelName[] = null;
	private String acquisitionMode[] = null;
	private String illuminationType[] = null;
	private String contrastMethod[] = null;
	private String illuminationWavelength[] = null;
	private String detectionWavelength[] = null;
	private String excitationWavelength[] = null;
	private String emissionWavelength[] = null;
	private String dyeID[] = null;
	private String dyeDatabaseID[] = null;
	private String pinholeSize[] = null;
	private String pinholeSizeAiry[] = null;
	private String pinholeGeometry[] = null;
		
	//~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * File info storage constructor.
     *
     * @param  name       file name
     * @param  directory  directory
     * @param  format     file format
     */
    public FileInfoCZI(String name, String directory, int format) {
        super(name, directory, format);
    }
    
  //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Displays the file information.
     *
     * @param  dlog    dialog box that is written to
     * @param  matrix  transformation matrix
     */
    public void displayAboutInfo(JDialogBase dlog, TransMatrix matrix) {
    	int i;
        JDialogText dialog = (JDialogText) dlog;
        displayPrimaryInfo(dialog, matrix);
        dialog.append("\n\n                Other information\n\n");
        
        if (imageName != null) {
        	dialog.append("Image name = " + imageName + "\n");
        }
        
        if (author != null) {
        	dialog.append("Author = " + author + "\n");
        }
        
        if (userName != null) {
        	dialog.append("User name = " + userName + "\n");
        }
        
        if (subType != null) {
        	dialog.append("SubType = " + subType + "\n");
        }
        
        if (title != null) {
        	dialog.append("Title = " + title + "\n");
        }
        
        if (creationDate != null) {
        	dialog.append("Creation date = " + creationDate + "\n");
        }
        
        if (description != null) {
        	dialog.append("Description = " + description + "\n");
        }
        
        if (thumbnail != null) {
        	dialog.append("Thumbnail = " + thumbnail + "\n");
        }
        
        if (comment != null) {
        	dialog.append("Comment = " + comment + "\n");
        }
        
        if (rating != null) {
        	dialog.append("Rating = " + rating + "\n");
        }
        
        if (keywords != null) {
        	dialog.append("Keywords = " + keywords + "\n");
        }
        
        if (ID != null) {
        	dialog.append("ID = " + ID + "\n");
        }
        
        if (displayName != null) {
        	dialog.append("Display name = " + displayName + "\n");
        }
        
        if (firstName != null) {
        	dialog.append("First name = " + firstName + "\n");
        }
        
        if (middleName != null) {
        	dialog.append("Middle name = " + middleName + "\n");
        }
        
        if (lastName != null) {
        	dialog.append("Last name = " + lastName + "\n");
        }
        
        if (email != null) {
        	dialog.append("Email = " + email + "\n");
        }
        
        if (institution != null) {
        	dialog.append("Institution = " + institution + "\n");
        }
        
        if (experimenterName != null) {
        	dialog.append("Experimenter name = " + experimenterName + "\n");
        }
        
        if (phone != null) {
        	dialog.append("Phone = " + phone + "\n");
        }
        
        if (fax != null) {
        	dialog.append("Fax = " + fax + "\n");
        }
        
        if (address != null) {
        	dialog.append("Address = " + address + "\n");
        }
        
        if (city != null) {
        	dialog.append("City = " + city + "\n");
        }
        
        if (state != null) {
        	dialog.append("State = " + state + "\n");
        }
        
        if (country != null) {
        	dialog.append("Country = " + country + "\n");
        }
        
        if (focusPosition != null) {
        	dialog.append("Focus position in micrometers = " + focusPosition + "\n");
        }
        
        if (acquisitionTime != null) {
        	dialog.append("Acquisition time = " + acquisitionTime + "\n");
        }
        
        if (stageXPosition != null) {
        	dialog.append("Stage axis X position in micrometers = " + stageXPosition + "\n");
        }
        
        if (stageYPosition != null) {
        	dialog.append("Stage axis Y position in micrometers = " + stageYPosition + "\n");
        }
        
        if (validBitsPerPixel != null) {
        	dialog.append("Valid bits per pixel = " + validBitsPerPixel + "\n");
        }
        
        if (timeStamps != null) {
        	dialog.append("Time stamps in seconds relative to the start time of acquisition:\n");
        	for (i = 0; i < timeStamps.length; i++) {
        	    dialog.append("Time stamp  " + i + ":     " + timeStamps[i] + "\n");	
        	}
        }
        
        if (dimH > 0) {
        	dialog.append("Number of phases = " + dimH + "\n");
        }
        
        if (dimR > 0) {
        	dialog.append("Number of rotation angles (indices) = " + dimR + "\n");
        }
        
        if (dimS > 0) {
        	dialog.append("Number of scenes = " + dimS + "\n");
        }
        
        if (dimI > 0) {
        	dialog.append("Number of illumination direction indices = " + dimI + "\n");
        }
        
        if (dimM > 0) {
        	dialog.append("Number of mosaic tiles (regular mosaics only) = " + dimM + "\n");
        }
        
        if (dimB > 0) {
        	dialog.append("Number of acquisition / recording / blocks = " + dimB + "\n");
        }
        
        if (dimV > 0) {
        	dialog.append("Number of views in a mult-view image = " + dimV + "\n");
        }
        
        if (originalScanData != null) {
        	boolean originalScan = Boolean.valueOf(originalScanData).booleanValue();
        	if (originalScan) {
        	    dialog.append("The image is the output of a scanning process and has not been modified\n");	
        	}
        	else {
        	    dialog.append("The image is not the original data of a scanning process\n")	;
        	}
        }
        
        for (i = 0; i < channelsFound; i++) {
     
            if ((channelID != null) && (channelID[i] != null)) {
            	dialog.append("Channel ID = " + channelID[i] + "\n") ;
            }
            
            if ((channelName != null) && (channelName[i] != null)) {
            	dialog.append("Channel name = " + channelName[i] + "\n");
            }
            
            if ((acquisitionMode != null) && (acquisitionMode[i] != null)) {
            	dialog.append("Acquisition mode = " + acquisitionMode[i] + "\n");
            }
            
            if ((illuminationType != null) && (illuminationType[i] != null)) {
            	dialog.append("Illumination type = " + illuminationType[i] + "\n");
            }
            
            if ((contrastMethod != null) && (contrastMethod[i] != null)) {
            	dialog.append("Contrast method = " + contrastMethod[i] + "\n");
            }
            
            if ((illuminationWavelength != null) && (illuminationWavelength[i] != null)) {
            	dialog.append("Illumination wavelength = " + illuminationWavelength[i] + "\n");
            }
            
            if ((detectionWavelength != null) && (detectionWavelength[i] != null)) {
            	dialog.append("Detection wavelength = " + detectionWavelength[i] + "\n");
            }
            
            if ((excitationWavelength != null) && (excitationWavelength[i] != null)) {
            	dialog.append("Excitation wavelength in nanometers = " + excitationWavelength[i] + "\n");
            }
            
            if ((emissionWavelength != null) && (emissionWavelength[i] != null)) {
            	dialog.append("Emission wavelength in nanometers = " + emissionWavelength[i] + "\n");
            }
            
            if ((dyeID != null) && (dyeID[i] != null)) {
            	dialog.append("Dye ID = " + dyeID[i] + "\n");
            }
            
            if ((dyeDatabaseID != null) && (dyeDatabaseID[i] != null)) {
            	dialog.append("Dye database ID = " + dyeDatabaseID[i] + "\n");
            }
            
            if ((pinholeSize != null) && (pinholeSize[i] != null)) {
                dialog.append("Pinhole size in micrometers = " + pinholeSize[i] + "\n");	
            }
            
            if ((pinholeSizeAiry != null) && (pinholeSizeAiry[i] != null)) {
                dialog.append("Pinhole size in airy disc units = " + pinholeSizeAiry[i] + "\n");	
            }
            
            if ((pinholeGeometry != null) && (pinholeGeometry[i] != null)) {
            	dialog.append("Pinhole geometry = " + pinholeGeometry[i] + "\n");
            }
            dialog.append("\n");
        } // for (i = 0; i < channelsFound; i++)
    }
    
    /**
     * 
     * @param focusPosition
     */
    public void setFocusPosition(String focusPosition) {
    	this.focusPosition = focusPosition;
    }
    
    /**
     * 
     * @param acquisitionTime
     */
    public void setAcquisitionTime(String acquisitionTime) {
    	this.acquisitionTime = acquisitionTime;
    }
    
    /**
     * 
     * @param stageXPosition
     */
    public void setStageXPosition(String stageXPosition) {
    	this.stageXPosition = stageXPosition;
    }
    
    /**
     * 
     * @param stageYPosition
     */
    public void setStageYPosition(String stageYPosition) {
    	this.stageYPosition = stageYPosition;
    }
    
    /**
     * 
     * @param validBitsPerPixel
     */
    public void setValidBitsPerPixel(String validBitsPerPixel) {
    	this.validBitsPerPixel = validBitsPerPixel;
    }
    
    /**
     * 
     * @param timeStamps
     */
    public void setTimeStamps(double timeStamps[]) {
    	this.timeStamps = timeStamps;
    }
    
    /**
     * 
     * @param imageName
     */
    public void setImageName(String imageName) {
    	this.imageName = imageName;
    }
    
    /**
     * 
     * @param author
     */
    public void setAuthor(String author) {
    	this.author = author;
    }
    
    /**
     * 
     * @param userName
     */
    public void setUserName(String userName) {
    	this.userName = userName;
    }
    
    /**
     * 
     * @param subType
     */
    public void setSubType(String subType) {
    	this.subType = subType;
    }
    
    /**
     * 
     * @param title
     */
    public void setTitle(String title) {
    	this.title = title;
    }
    
    /**
     * 
     * @param creationDate
     */
    public void setCreationDate(String creationDate) {
    	this.creationDate = creationDate;
    }
    
    /**
     * 
     * @param description
     */
    public void setDescription(String description) {
    	this.description = description;
    }
    
    /**
     * 
     * @param thumbnail
     */
    public void setThumbnail(String thumbnail) {
    	this.thumbnail = thumbnail;
    }
    
    /**
     * 
     * @param comment
     */
    public void setComment(String comment) {
    	this.comment = comment;
    }
    
    /**
     * 
     * @param rating
     */
    public void setRating(String rating) {
    	this.rating = rating;
    }
    
    /**
     * 
     * @param keywords
     */
    public void setKeywords(String keywords) {
    	this.keywords = keywords;
    }
    
    /**
     * 
     * @param ID
     */
    public void setID(String ID) {
    	this.ID = ID;
    }
    
    /**
     * 
     * @param displayName
     */
    public void setDisplayName(String displayName) {
    	this.displayName = displayName;
    }
    
    /**
     * 
     * @param firstName
     */
    public void setFirstName(String firstName) {
    	this.firstName = firstName;
    }
    
    /**
     * 
     * @param middleName
     */
    public void setMiddleName(String middleName) {
    	this.middleName = middleName;
    }
    
    /**
     * 
     * @param lastName
     */
    public void setLastName(String lastName) {
    	this.lastName = lastName;
    }
    
    /**
     * 
     * @param email
     */
    public void setEmail(String email) {
    	this.email = email;
    }
    
    /**
     * 
     * @param institution
     */
    public void setInstitution(String institution) {
    	this.institution = institution;
    }
    
    /**
     * 
     * @param experimenterName
     */
    public void setExperimenterName(String experimenterName) {
    	this.experimenterName = experimenterName;
    }
    
    /**
     * 
     * @param phone
     */
    public void setPhone(String phone) {
    	this.phone = phone;
    }
    
    /**
     * 
     * @param fax
     */
    public void setFax(String fax) {
    	this.fax = fax;
    }
    
    /**
     * 
     * @param address
     */
    public void setAddress(String address) {
    	this.address = address;
    }
    
    /**
     * 
     * @param city
     */
    public void setCity(String city) {
    	this.city = city;
    }
    
    /**
     * 
     * @param state
     */
    public void setState(String state) {
    	this.state = state;
    }
    
    /**
     * 
     * @param country
     */
    public void setCountry(String country) {
    	this.country = country;
    }
    
    /**
     * 
     * @param dimH
     */
    public void setDimH(int dimH) {
    	this.dimH = dimH;
    }
    
    /**
     * 
     * @param dimR
     */
    public void setDimR(int dimR) {
    	this.dimR = dimR;
    }
    
    /**
     * 
     * @param dimS
     */
    public void setDimS(int dimS) {
    	this.dimS = dimS;
    }
    
    /**
     * 
     * @param dimI
     */
    public void setDimI(int dimI) {
    	this.dimI = dimI;
    }
    
    /**
     * 
     * @param dimM
     */
    public void setDimM(int dimM) {
    	this.dimM = dimM;
    }
    
    /**
     * 
     * @param dimB
     */
    public void setDimB(int dimB) {
    	this.dimB = dimB;
    }
    
    /**
     * 
     * @param dimV
     */
    public void setDimV(int dimV) {
    	this.dimV = dimV;
    }
    
    /**
     * 
     * @param originalScanData
     */
    public void setOriginalScanData(String originalScanData) {
    	this.originalScanData = originalScanData;
    }
    
    /**
     * 
     * @param channelsFound
     */
    public void setChannelsFound(int channelsFound) {
    	this.channelsFound = channelsFound;
    }
    
    /**
     * 
     * @param channelID
     */
    public void setChannelID(String channelID[]) {
    	this.channelID = channelID;
    }
    
    /**
     * 
     * @param channelName
     */
    public void setChannelName(String channelName[]) {
    	this.channelName = channelName;
    }
    
    /**
     * 
     * @param acquisitionMode
     */
    public void setAcquisitionMode(String acquisitionMode[]) {
    	this.acquisitionMode = acquisitionMode;
    }
    
    /**
     * 
     * @param illuminationType
     */
    public void setIlluminationType(String illuminationType[]) {
    	this.illuminationType = illuminationType;
    }
    
    /**
     * 
     * @param contrastMethod
     */
    public void setContrastMethod(String contrastMethod[]) {
    	this.contrastMethod = contrastMethod;
    }
    
    /**
     * 
     * @param illuminationWavelength
     */
    public void setIlluminationWavelength(String illuminationWavelength[]) {
    	this.illuminationWavelength = illuminationWavelength;
    }
    
    /**
     * 
     * @param detectionWavelength
     */
    public void setDetectionWavelength(String detectionWavelength[]) {
    	this.detectionWavelength = detectionWavelength;
    }
    
    /**
     * 
     * @param excitationWavelength
     */
    public void setExcitationWavelength(String excitationWavelength[]) {
    	this.excitationWavelength = excitationWavelength;
    }
    
    /**
     * 
     * @param emissionWavelength
     */
    public void setEmissionWavelength(String emissionWavelength[]) {
    	this.emissionWavelength = emissionWavelength;
    }
    
    /**
     * 
     * @param dyeID
     */
    public void setDyeID(String dyeID[]) {
    	this.dyeID = dyeID;
    }
    
    /**
     * 
     * @param dyeDatabaseID
     */
    public void setDyeDatabaseID(String dyeDatabaseID[]) {
    	this.dyeDatabaseID = dyeDatabaseID;
    }
    
    /**
     * 
     * @param pinholeSize
     */
    public void setPinholeSize(String pinholeSize[]) {
    	this.pinholeSize = pinholeSize;
    }
    
    /**
     * 
     * @param pinholeSizeAiry
     */
    public void setPinholeSizeAiry(String pinholeSizeAiry[]) {
    	this.pinholeSizeAiry = pinholeSizeAiry;
    }
    
    /**
     * 
     * @param pinholeGeometry
     */
    public void setPinholeGeometry(String pinholeGeometry[]) {
    	this.pinholeGeometry = pinholeGeometry;
    }
}