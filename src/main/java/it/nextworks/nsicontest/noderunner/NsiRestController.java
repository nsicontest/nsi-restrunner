package it.nextworks.nsicontest.noderunner;

import it.nextworks.nsicontest.noderunner.beans.JobRecord;
import it.nextworks.nsicontest.noderunner.beans.JobStatusResponse;
import it.nextworks.nsicontest.noderunner.beans.StartRequest;
import it.nextworks.nsicontest.noderunner.beans.StartResponse;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
@EnableAutoConfiguration
@EnableJpaRepositories
@ComponentScan
public class NsiRestController {
    @Autowired
    JobRepository db;
    
    @Autowired
    NsiRiInstanceRunner runner;
    
    @RequestMapping(value="/job", method=RequestMethod.POST)
    @ResponseBody
    StartResponse home(@RequestBody StartRequest req) {
        String newId = UUID.randomUUID().toString();
        StartResponse result = new StartResponse()
                .setId(newId);
        JobRecord rec = new JobRecord()
                .setId(newId)
                .setStatus(JobRecord.Status.QUEUED);
        db.save(rec);
        try {
            rec
                .setRequesterId(req.getRequesterId())
                .setXmlData(req.getXmlData().getBytes("utf-8"));
        } catch (Throwable ex) {
            Logger.getLogger(NsiRestController.class.getName()).log(Level.SEVERE, null, ex);
            rec.setStatus(JobRecord.Status.ABORTED_ERROR);
            rec.setErrorMsg(ex.getMessage());
            result.setError(ex.getMessage());
        } finally {
            result.setStatus(rec.getStatus().name());
        }
        
        if (rec.getStatus().equals(JobRecord.Status.QUEUED)) {
            runner.run(rec);
        }
        
        return result;
    }
    
    @RequestMapping(value="/job/{jobId}", method=RequestMethod.GET)
    @ResponseBody
    JobStatusResponse home(@PathVariable String jobId) {
        String id = UUID.fromString(jobId).toString();
        JobRecord rec = db.findOne(id);
        try {
            JobStatusResponse result = new JobStatusResponse()
                    .setId(rec.getId())
                    .setStatus(rec.getStatus().name())
                    .setErrorMsg(rec.getErrorMsg());
            byte[] reportXmlData = rec.getReportXmlData();
            if (reportXmlData != null && reportXmlData.length > 0) {
                result.setTestReport(new String(reportXmlData));
            }
            return result;
        } catch (NullPointerException ex) {
            return new JobStatusResponse()
                    .setId(id)
                    .setStatus("NOT_FOUND");
        }
    }
    
    public static void main(String[] args) throws Exception {
        SpringApplication.run(NsiRestController.class, args);
    }

}
