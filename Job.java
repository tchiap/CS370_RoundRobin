/* Job.java
 * Represents a job in an operating system, for use by the OSScheduler class.
 */
 
import java.lang.System;

public class Job
{
    public enum jobType {ShortJob, LongJob};
    public static final int SHORT_SERVICE_TIME = 1 * OSScheduler.DEBUG_FACTOR;
    public static final int LONG_SERVICE_TIME = 10 * OSScheduler.DEBUG_FACTOR;
    
    private jobType type;
    private long serviceLength;
    private long generationTime;
    private long completionTime;
    private long servicedTime;
    public int jobID;
    
    public Job(jobType t, int id)
    {
	generationTime = System.currentTimeMillis();
        type = t;
        jobID = id;
    }  // Job()
    
    public jobType getType()
    {
        return type;
    }  // getType()
	
    public long getServiceTime()
    {
        return completionTime - servicedTime;
    }  // getServiceTime()
	
    public long getGenerationTime()
    {
            return generationTime;
    }  // getGenerationTime()

    public void setCompletionTime(long t)
    {
            completionTime = t;
    }  // setCompletionTime()

    public long getCompletionTime()
    {
            return completionTime;
    }  // getCompletionTime()
    
    public void setServicedTime(long s)
    {
        servicedTime = s;
    }  // setServicedTime()
    
    public long getWaitTime()
    {
        return completionTime - generationTime - servicedTime;
    }  // getWaitTime()
}  // Job class
