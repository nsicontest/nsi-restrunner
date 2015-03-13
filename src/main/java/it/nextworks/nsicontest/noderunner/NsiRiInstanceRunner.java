package it.nextworks.nsicontest.noderunner;

import com.google.common.io.Files;
import it.nextworks.nsicontest.noderunner.beans.JobRecord;
import it.nextworks.nsicontest.subprocess.RuntimeExec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Data;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.IOUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "nsi-ri-runner")
@Data
@Component
public class NsiRiInstanceRunner{
    private static final Logger LOG = Logger.getLogger(NsiRiInstanceRunner.class.getName());
    
    private String zipFilePath;
    
    @Autowired
    JobRepository db;
            
    
    
    void run(final JobRecord rec) {
        new Thread(new Runnable() {
            
            File tempDir;
            File xmlTestScenario;
            File xmlTestScenarioResults;
            
            private void deployInstance() throws ZipException {
                tempDir = Files.createTempDir();
                LOG.info(String.format("unzipping archive %s to %s ...", zipFilePath, tempDir.getAbsolutePath()));
                new ZipFile(zipFilePath).extractAll(tempDir.getAbsolutePath());
            }
            
            private void undeployInstance() throws IOException {
                LOG.info(String.format("removing %s ...", tempDir.getAbsolutePath()));
                FileUtils.deleteDirectory(tempDir);
            }
            
            private void injectXmlTestScenario() throws FileNotFoundException, IOException {
                xmlTestScenario = new File(tempDir, String.format("xml_test_scenario-%s", rec.getId()));
                xmlTestScenarioResults = new File(tempDir, String.format("xml_test_scenario-%s-result", rec.getId()));
                
                LOG.info(String.format("writing %s ...", xmlTestScenario.getAbsolutePath()));
                FileOutputStream out = new FileOutputStream(xmlTestScenario);
                IOUtils.write(rec.getXmlData(), out);
                out.close();
            }
            
            private void injectInstanceConfig() throws FileNotFoundException, IOException {
                File nsiProperties = new File(new File(tempDir, "etc"), "nsi.properties");
                LOG.info(String.format("editing %s ...", nsiProperties.getAbsolutePath()));
                FileInputStream in = new FileInputStream(nsiProperties);
                Properties props = new Properties();
                props.load(in);
                in.close();

                // set/alter relevant nsi.properties keys here
                props.setProperty("xml_test_scenario", xmlTestScenario.getAbsolutePath());
                props.setProperty("xml_test_scenario_result", xmlTestScenarioResults.getAbsolutePath());
                props.setProperty("cli", "false");
                
                FileOutputStream out = new FileOutputStream(nsiProperties);
                props.store(out, null);
                out.close();

            }
            
            private void startupInstance() throws IOException, InterruptedException {
                Runtime rt = Runtime.getRuntime();
                String scriptPath = new File(tempDir.getAbsolutePath(), "startConsole.sh").getAbsolutePath();
                LOG.info(String.format("running %s ...", scriptPath));
                Process proc = rt.exec("sh "+scriptPath);
                StringBuffer buffer = new StringBuffer();
                RuntimeExec.StreamWrapper stdout = RuntimeExec.getStreamWrapper(System.out, proc.getInputStream(), rec.getId(), buffer);
                RuntimeExec.StreamWrapper stderr = RuntimeExec.getStreamWrapper(System.err, proc.getErrorStream(), rec.getId(), buffer);
                stdout.start();
                stderr.start();
                stdout.join();
                stderr.join();
                if (proc.waitFor() != 0)
                    throw new RuntimeExec.ExecutionError(buffer.toString());
            }
            
            @Override
            public void run() {
                try {
                    rec.setStatus(JobRecord.Status.STARTED);
                    LOG.info(String.format("jobid status %s marked as %s", rec.getId(), rec.getStatus().name()));
                    
                    db.save(rec);
                    deployInstance();
                    injectXmlTestScenario();
                    injectInstanceConfig();
                    startupInstance();
                    
                    rec.setStatus(JobRecord.Status.COMPLETED_OK);
                    RandomAccessFile f = new RandomAccessFile(xmlTestScenarioResults.getAbsolutePath(), "r");
                    byte[] reportXmlData = new byte[(int)xmlTestScenarioResults.length()];
                    f.read(reportXmlData);
                    
                    rec.setReportXmlData(reportXmlData);
                    LOG.info(String.format("jobid status %s marked as %s", rec.getId(), rec.getStatus().name()));
                    db.save(rec);
                } catch (Throwable ex) {
                    LOG.log(Level.SEVERE, null, ex);
                    rec.setStatus(JobRecord.Status.COMPLETED_ERROR);
                    rec.setErrorMsg(ex.getMessage());
                    LOG.info(String.format("jobid status %s marked as %s", rec.getId(), rec.getStatus().name()));
                    db.save(rec);
                } finally {
                    try {
                        undeployInstance();
                    } catch (IOException ex) {
                        LOG.log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        ).start();
    }
    
}
