package fi.vm.yti.codelist.intake.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "membervalue")
public class MemberValue extends AbstractIdentifyableTimestampedCode implements Serializable {

    private static final long serialVersionUID = 1L;

    private String value;
    private ValueType valueType;
    private Member member;

    @Column(name = "value")
    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valuetype_id", nullable = false)
    public ValueType getValueType() {
        return valueType;
    }

    public void setValueType(final ValueType valueType) {
        this.valueType = valueType;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    public Member getMember() {
        return member;
    }

    public void setMember(final Member member) {
        this.member = member;
    }
}
