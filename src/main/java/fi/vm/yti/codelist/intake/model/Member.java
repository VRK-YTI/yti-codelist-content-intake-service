package fi.vm.yti.codelist.intake.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;

import fi.vm.yti.codelist.common.dto.Views;
import io.swagger.annotations.ApiModelProperty;
import static fi.vm.yti.codelist.common.constants.ApiConstants.LANGUAGE_CODE_EN;

@Entity
@Table(name = "member")
public class Member extends AbstractIdentifyableTimestampedCode implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer order;
    private Code code;
    private Extension extension;
    private Member relatedMember;
    private Map<String, String> prefLabel;
    private Date startDate;
    private Date endDate;
    private String memberValue_1;
    private String memberValue_2;
    private String memberValue_3;

    @Column(name = "memberorder")
    @JsonView(Views.Normal.class)
    public Integer getOrder() {
        return order;
    }

    public void setOrder(final Integer order) {
        this.order = order;
    }

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JsonView(Views.ExtendedMember.class)
    @JoinColumn(name = "code_id", nullable = false)
    public Code getCode() {
        return code;
    }

    public void setCode(final Code code) {
        this.code = code;
    }

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinColumn(name = "extension_id", updatable = false)
    public Extension getExtension() {
        return extension;
    }

    public void setExtension(final Extension extension) {
        this.extension = extension;
    }

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinColumn(name = "relatedmember_id")
    public Member getRelatedMember() {
        return relatedMember;
    }

    public void setRelatedMember(final Member relatedMember) {
        this.relatedMember = relatedMember;
    }

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "member_preflabel", joinColumns = @JoinColumn(name = "member_id", referencedColumnName = "id"))
    @MapKeyColumn(name = "language")
    @Column(name = "preflabel")
    @OrderColumn
    public Map<String, String> getPrefLabel() {
        if (prefLabel == null) {
            prefLabel = new HashMap<>();
        }
        return prefLabel;
    }

    public void setPrefLabel(final Map<String, String> prefLabel) {
        this.prefLabel = prefLabel;
    }

    public String getPrefLabel(final String language) {
        String prefLabelValue = this.prefLabel.get(language);
        if (prefLabelValue == null) {
            prefLabelValue = this.prefLabel.get(LANGUAGE_CODE_EN);
        }
        return prefLabelValue;
    }

    public void setPrefLabel(final String language,
                             final String value) {
        if (prefLabel == null) {
            prefLabel = new HashMap<>();
        }
        if (language != null && value != null && !value.isEmpty()) {
            prefLabel.put(language, value);
        } else if (language != null) {
            prefLabel.remove(language);
        }
        setPrefLabel(prefLabel);
    }

    @ApiModelProperty(dataType = "dateTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Temporal(TemporalType.DATE)
    @Column(name = "startdate")
    public Date getStartDate() {
        if (startDate != null) {
            return new Date(startDate.getTime());
        }
        return null;
    }

    public void setStartDate(final Date startDate) {
        if (startDate != null) {
            this.startDate = new Date(startDate.getTime());
        } else {
            this.startDate = null;
        }
    }

    @ApiModelProperty(dataType = "dateTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Temporal(TemporalType.DATE)
    @Column(name = "enddate")
    public Date getEndDate() {
        if (endDate != null) {
            return new Date(endDate.getTime());
        }
        return null;
    }

    public void setEndDate(final Date endDate) {
        if (endDate != null) {
            this.endDate = new Date(endDate.getTime());
        } else {
            this.endDate = null;
        }
    }

    @Column(name = "membervalue_1")
    public String getMemberValue_1() {
        return memberValue_1;
    }

    public void setMemberValue_1(final String memberValue_1) {
        this.memberValue_1 = memberValue_1;
    }

    @Column(name = "membervalue_2")
    public String getMemberValue_2() {
        return memberValue_2;
    }

    public void setMemberValue_2(final String memberValue_2) {
        this.memberValue_2 = memberValue_2;
    }

    @Column(name = "membervalue_3")
    public String getMemberValue_3() {
        return memberValue_3;
    }

    public void setMemberValue_3(final String memberValue_3) {
        this.memberValue_3 = memberValue_3;
    }
}