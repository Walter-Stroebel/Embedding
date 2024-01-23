package nl.infcomtec.ffmpeg;

import com.google.gson.annotations.SerializedName;

/**
 *
 * @author walter
 */
public class T {

    @SerializedName(value = "major_brand")
    public String majorBrand;
    @SerializedName(value = "minor_version")
    public String minorVersion;
    @SerializedName(value = "compatible_brands")
    public String compatibleBrands;
    public String encoder;
    public String ENCODER;
    public String title;
    public String comment;
    @SerializedName(value = "creation_time")
    public String creationTime;

}
