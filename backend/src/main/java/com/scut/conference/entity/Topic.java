package com.scut.conference.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** 征文专题 */
@Data
public class Topic {
    private Long id;
    private String name;
    private String description;
    private Integer reportLimit;
    private Long keynoteExpertId;
    private String keynoteExpertName;
    private String reportName;
    @JsonIgnore
    private String pptPath;
    private String reportTimeNote;

    @JsonProperty("hasPpt")
    public boolean hasPpt() { return pptPath != null && !pptPath.isBlank(); }

    @JsonProperty("pptExt")
    public String getPptExt() {
        if (!hasPpt()) return null;
        int dot = pptPath.lastIndexOf('.');
        return dot < 0 ? null : pptPath.substring(dot + 1);
    }
    private Integer sortNo;
}
