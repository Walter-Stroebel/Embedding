package nl.infcomtec.ffmpeg;

import com.google.gson.annotations.SerializedName;

/**
 *
 * @author walter
 */
public class F {

    public String filename;
    @SerializedName(value = "nb_streams")
    public int nbStreams;
    @SerializedName(value = "nb_programs")
    public int nbPrograms;
    @SerializedName(value = "format_name")
    public String formatName;
    @SerializedName(value = "format_long_name")
    public String formatLongName;
    @SerializedName(value = "start_time")
    public double startTime;
    public double duration;
    public long size;
    @SerializedName(value = "bit_rate")
    public int bitRate;
    @SerializedName(value = "probe_score")
    public int probeScore;
    public T tags;

}
