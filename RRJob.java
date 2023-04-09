/**
 * Tom Chiapete
 * November 26, 2007
 * Implements the job class for the round-robin scheduling algorithm.
 */

import java.lang.System;

public class RRJob
{
    // Declare short and long job types
    public enum jobType {ShortJob, LongJob};

    // Set short and long service times
    public static final int SHORT_SERVICE_TIME = 5 * RRScheduler.DEBUG_FACTOR;
    public static final int LONG_SERVICE_TIME = 50 * RRScheduler.DEBUG_FACTOR;

    private int processorID;  // set processor ID for this job
    private jobType type; // job type

    private int serviceLength;  // The service or remaining service length
    private long generationTime; // The job generation time
    private long completionTime; // The job completion time
    private long servicedTime; // The job serviced time
    public int jobID; // The job ID

    /**
     * setProcessorID() setter.  Sets processor ID
     */
    public void setProcessorID(int id)
    {
        processorID = id;   
    }
    
    /**
     * getProcessorID() getter.  Get processor ID
     */
    public int getProcessorID()
    {
        return processorID;   
    }
    
    /**
     * setServiceLength() setter.  Sets the service length
     */
    public void setServiceLength(int length)
    {
        serviceLength = length;   
    }
    
    /**
     * getServiceLength() getter.  Gets the service length
     */
    public int getServiceLength()
    {
        return serviceLength;   
    }

    /**
     * RRJob() constructor.
     * Initializes the RRJob object.  Sets the generation time,
     * the job type, processor ID, and job ID.
     */
    public RRJob(jobType t, int id)
    {
        generationTime = System.currentTimeMillis();
        type = t;
        processorID = -1;
        jobID = id;
    } 

    /**
     * getType() method
     * Returns the job type.
     */
    public jobType getType()
    {
        return type;
    }  // getType()

    /** 
     * getSericeTime() getter
     * Returns the service time, found by completionTime minus servicedTime
     */
    public long getServiceTime()
    {
        return completionTime - servicedTime;
    }

    /**
     * getGenerationTime() getter
     * Returns the generation time
     */
    public long getGenerationTime()
    {
            return generationTime;
    }

    /** 
     * setCompletionTime() setter
     * Sets the completion time to the instance variable
     */
    public void setCompletionTime(long t)
    {
            completionTime = t;
    } 

    /**
     * getCompletionTime() getter
     * Returns completion Time
     */
    public long getCompletionTime()
    {
            return completionTime;
    }

    /** 
     * setServicedTime() setter
     * Sets the serviced Time
     */
    public void setServicedTime(long s)
    {
        servicedTime = s;
    }

    /**
     * getWaitTime() getter
     * Returns the wait time.
     * Completion time minus generation time minus serviced time.
     */
    public long getWaitTime()
    {
        return completionTime - generationTime - servicedTime;
    }
}

