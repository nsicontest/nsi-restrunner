package it.nextworks.nsicontest.noderunner.beans;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain=true)
public class StartResponse {
    private String id;
    private String status;
    private String error;
    private String testReport;
}
