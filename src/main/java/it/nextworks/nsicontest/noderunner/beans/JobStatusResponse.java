package it.nextworks.nsicontest.noderunner.beans;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 *
 * @author a.gronchi
 */
@Data
@Accessors(chain=true)
public class JobStatusResponse {
    private String id;
    private String status;
    private String testReport;
    private String errorMsg;
}
