package com.example.codecompass.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class UniversityRecommendation {

    // All University fields (flat POJO — Gson handles flat JSON better without inheritance)
    private int id;
    private String name;
    private String abbreviation;

    @SerializedName("university_type")
    private String universityType;

    private String region;
    private String city;

    @SerializedName("website_url")
    private String websiteUrl;

    @SerializedName("logo_url")
    private String logoUrl;

    @SerializedName("accreditation_level")
    private int accreditationLevel;

    @SerializedName("ched_coe")
    private boolean chedCoe;

    @SerializedName("ched_cod")
    private boolean chedCod;

    @SerializedName("tuition_range_min")
    private Integer tuitionRangeMin;

    @SerializedName("tuition_range_max")
    private Integer tuitionRangeMax;

    private List<CCSProgram> programs;

    // Recommendation-specific fields
    @SerializedName("match_score")
    private int matchScore;

    @SerializedName("match_reasons")
    private List<String> matchReasons;

    public int getId() { return id; }
    public String getName() { return name; }
    public String getAbbreviation() { return abbreviation; }
    public String getUniversityType() { return universityType; }
    public String getRegion() { return region; }
    public String getCity() { return city; }
    public String getWebsiteUrl() { return websiteUrl; }
    public String getLogoUrl() { return logoUrl; }
    public int getAccreditationLevel() { return accreditationLevel; }
    public boolean isChedCoe() { return chedCoe; }
    public boolean isChedCod() { return chedCod; }
    public Integer getTuitionRangeMin() { return tuitionRangeMin; }
    public Integer getTuitionRangeMax() { return tuitionRangeMax; }
    public List<CCSProgram> getPrograms() { return programs; }
    public int getMatchScore() { return matchScore; }
    public List<String> getMatchReasons() { return matchReasons; }
}
