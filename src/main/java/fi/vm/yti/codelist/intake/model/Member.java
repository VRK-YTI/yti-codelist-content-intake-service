package fi.vm.yti.codelist.intake.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;

import fi.vm.yti.codelist.common.dto.Views;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name = "member")
public class Member extends AbstractIdentifyableTimestampedCode implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer order;
    private Code code;
    private Extension extension;
    private Member relatedMember;
    private Map<String, String> prefLabel;
    @Convert(converter = LocalDateAttributeConverter.class)
    private LocalDate startDate;
    @Convert(converter = LocalDateAttributeConverter.class)
    private LocalDate endDate;
    private Set<MemberValue> memberValues;
    private String uri;
    private Integer sequenceId;

    public Member() {
        memberValues = new HashSet<>();
    }

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
        if (this.prefLabel != null && !this.prefLabel.isEmpty()) {
            return this.prefLabel.get(language);
        }
        return null;
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

    @Schema(name = "date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Column(name = "startdate")
    public LocalDate getStartDate() {
        if (startDate != null) {
            return startDate;
        }
        return null;
    }

    public void setStartDate(final LocalDate startDate) {
        if (startDate != null) {
            this.startDate = startDate;
        } else {
            this.startDate = null;
        }
    }

    @Schema(name = "date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Column(name = "enddate")
    public LocalDate getEndDate() {
        if (endDate != null) {
            return endDate;
        }
        return null;
    }

    public void setEndDate(final LocalDate endDate) {
        if (endDate != null) {
            this.endDate = endDate;
        } else {
            this.endDate = null;
        }
    }

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<MemberValue> getMemberValues() {
        return memberValues;
    }

    public void setMemberValues(final Set<MemberValue> memberValues) {
        this.memberValues = memberValues;
    }

    public MemberValue getMemberValueWithLocalName(final String localName) {
        if (memberValues != null && !memberValues.isEmpty()) {
            for (final MemberValue memberValue : memberValues) {
                if (memberValue.getValueType().getLocalName().equalsIgnoreCase(localName)) {
                    return memberValue;
                }
            }
        }
        return null;
    }

    @Column(name = "uri")
    public String getUri() {
        return uri;
    }

    public void setUri(final String uri) {
        this.uri = uri;
    }

    @Column(name = "sequence_id")
    @JsonView(Views.Normal.class)
    public Integer getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(final Integer sequenceId) {
        this.sequenceId = sequenceId;
    }
}
