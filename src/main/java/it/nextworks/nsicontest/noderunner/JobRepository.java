package it.nextworks.nsicontest.noderunner;

import it.nextworks.nsicontest.noderunner.beans.JobRecord;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.CrudRepository;

@Repository
public interface JobRepository extends CrudRepository<JobRecord, String> {
}
