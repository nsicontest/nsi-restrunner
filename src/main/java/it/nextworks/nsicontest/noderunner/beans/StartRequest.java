package it.nextworks.nsicontest.noderunner.beans;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain=true)
public class StartRequest {
    private String requesterId;
    private String xmlData;
}
