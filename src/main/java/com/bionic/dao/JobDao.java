/**
 * 
 */
package com.bionic.dao;

import java.util.List;

import com.bionic.model.Job;
import org.springframework.data.repository.CrudRepository;

/**
 * @author vitalii.levash
 *
 */
public interface JobDao extends CrudRepository<Job, Integer>  {

}
