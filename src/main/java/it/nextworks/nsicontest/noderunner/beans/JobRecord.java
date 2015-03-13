package it.nextworks.nsicontest.noderunner.beans;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import static javax.persistence.EnumType.STRING;
import javax.persistence.Enumerated;
import static javax.persistence.FetchType.LAZY;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.Transient;
import lombok.Data;
import lombok.experimental.Accessors;

@Accessors(chain=true)
@Data
@Entity
public class JobRecord implements Serializable {
    
    public enum Status {
        QUEUED,
        STARTED,
        ABORTED_ERROR,
        COMPLETED_OK,
        COMPLETED_ERROR
    };

    
    @Id
    @Column(length = 36, nullable = false)
    private String id;
    @Column
    private String requesterId;
    @Lob
    @Basic(fetch=LAZY)
    @Column(length = 1024*1024)
    private byte[] xmlData;
    
    @Enumerated(STRING)
    private Status status;
    @Column
    private Timestamp startTime;
    
    @PrePersist
    void defaults() {
        if (startTime == null)
            startTime = new Timestamp(new Date().getTime());
    }
    
    @Transient
    private String tempDir;
    
    @Lob
    @Basic(fetch=LAZY)
    @Column(length = 1024*1024)
    private byte[] reportXmlData;
    
    @Column
    private String errorMsg;
}

