/**
 * Tom Chiapete
 * November 26, 2007
 * Implements a round-robin scheduling algorithm by generating jobs 
 * of either 5 or 50 times units, running at most 8 time units at a time 
 * otherwise thrown back on the end of the queue to be finished later.
 * If AFFINITY is set for processor affinity, when the original processor
 * is not the current processor during a pass other than the first, 
 * there is a one time unit penalty.
 */

import java.io.*;
import java.lang.Thread;
import java.util.LinkedList;
import java.lang.System;
import java.util.Arrays;

public class RRScheduler implements Runnable

{

   // For the single-processor version, use NUM_CONSUMERS = 1.
    // For the multi-processor version, use NUM_CONSUMERS = 2.

    private static final int NUM_PRODUCERS = 2;

    private static final int NUM_CONSUMERS = 2;
    
    private static final boolean AFFINITY = false;

    // BUFFER_SIZE controls the max number of jobs in the buffer.   10
    private static final int BUFFER_SIZE = 10;

    private int jobIDCounter = 0;

    // Set debug to true to print job production and consumption times.
    private boolean debug = false;

    // Multiplying factor to JOB_FREQ and SERVICE_TIME variables for debug.
    public static final int DEBUG_FACTOR = 1;

    // Controls the length of the simulation.   50
    private static final int SIMULATION_LENGTH = 50;
    
    // The timeslice required by the round-robin algorithm
    private static final int TIMESLICE = 8;

    // A rough counter used to control the length of the simulation.
    // Producer and consumer threads halt when the counter reaches zero.
    private int simulationCounter = SIMULATION_LENGTH;

    

    // Each thread has a local threadType which controls whether it is a

    // producer or a consumer.

    private enum threadType {Producer, Consumer};

    private threadType tType;

    // Each producer thread has a local producerType which controls
    // whether it creates short jobs or long jobs.
    private enum producerType {ShortJobs, LongJobs};
    private producerType pType;

    // Processor ID
    private int consumerNum;

    // Arrays of producers and consumers.
    private Thread[] producers;
    private Thread[] consumers;

    // The job buffer.
    private LinkedList<RRJob> buffer = new LinkedList<RRJob>();

    // Semaphores control the number of jobs in the buffer (0 - BUFFER_SIZE).
    Semaphore bufferEmpty = new Semaphore(BUFFER_SIZE);
    Semaphore bufferFull = new Semaphore(0);

    // Simulation statistics variables.
    
    // Number of jobs served
    private int numJobsServed = 0;

    // Notes a timestamp of the start time of the simulation
    private long simulationStartTime;
    
    // If there is a processor difference for a particular job, 
    // there is a 1 time unit penalty
    private static final int PROC_SWITCH_PENALTY = 1;

    // Wait and service time arrays contain the min, max, total, and average
    // for each job type and are updated continuously during the simulation.
    private static final int STAT_MIN = 0;
    private static final int STAT_MAX = 1;
    private static final int STAT_TOTAL = 2;
    private static final int STAT_AVG = 3;

    // Arrays to collect wait time and service time statistics
    private double[] shortWaitTime = new double[4];
    private double[] longWaitTime = new double[4];
    private double[] shortServiceTime = new double[4];
    private double[] longServiceTime = new double[4];
    private double numShortJobsServed = 0;
    private double numLongJobsServed = 0;

    // Throughput = Total # jobs served / Total simulation time.
    // Only a running total until calculated at the end of the simulation.
    private double shortThroughput, longThroughput;

    // Processor utilization = total job service time / # jobs served.
    // Only a running total until calculated at the end of the simulation.
    private double[] processorUtilization = new double[NUM_CONSUMERS];

    

    public RRScheduler()
    {

        // Set simulation start time.
        simulationStartTime = System.currentTimeMillis();

        // Initialize statistics arrays.
        shortWaitTime[STAT_MIN] = Long.MAX_VALUE;
        longWaitTime[STAT_MIN] = Long.MAX_VALUE;
        shortServiceTime[STAT_MIN] = Long.MAX_VALUE;
        longServiceTime[STAT_MIN] = Long.MAX_VALUE;
        
        for(int i = 0; i < NUM_CONSUMERS; i++)
            processorUtilization[i] = 0;

        // Create producers.
        producers = new Thread[NUM_PRODUCERS];
        tType = threadType.Producer;

        // One producer creates short jobs.
        pType = producerType.ShortJobs;

        // Start producer 0 thread (short jobs)
        producers[0] = new Thread(this);
        producers[0].start();


        // Give first thread a chance to grab its type = ShortJobs.
        try { Thread.sleep(5); } catch(InterruptedException e) {}

        // One producer creates long jobs.  Start producer 1 threads (Long jobs)
        pType = producerType.LongJobs;
        producers[1] = new Thread(this);
        producers[1].start();

        // Give next thread a chance to grab its type = LongJobs.
        try { Thread.sleep(5); } catch(InterruptedException e) {}

        // Initiate counter.
        simulationCounter = SIMULATION_LENGTH;

        // Create consumers.
        consumers = new Thread[NUM_CONSUMERS];
        tType = threadType.Consumer;

        // Initialize and start all consumer threads
        for(int j = 0; j < NUM_CONSUMERS; j++)
        {
            consumerNum = j;
            consumers[j] = new Thread(this);
            consumers[j].start();
            try { Thread.sleep(5); } catch(InterruptedException e) {}
        }
        
        // Wait for simulation to end, then calculate and print
        // simulation statistics.

        while(simulationCounter > 0)
            try { Thread.sleep(100); } catch(InterruptedException e) {}

        System.out.println("---SIMULATION STATISTICS---");

        // Mark the end of the simulation-- grab the timestamp
        long simulationEndTime = System.currentTimeMillis();
        
        // Calculate total simulation time
        long totalSimulationTime = simulationEndTime - simulationStartTime;

        // Calculate and print processor utilization for each consumer.
        for(int k = 0; k < NUM_CONSUMERS; k++)
        {
                processorUtilization[k] =
                        processorUtilization[k] / totalSimulationTime * 100;

                System.out.println("Processor " + k + " utilization % = "
                        + processorUtilization[k]);
        }

        // Calculate and print throughput for each job type.
        double totalThroughput = shortThroughput + longThroughput;
        totalThroughput = totalThroughput / (totalSimulationTime / DEBUG_FACTOR) * 100;
        shortThroughput = shortThroughput / (totalSimulationTime / DEBUG_FACTOR) * 100;
        longThroughput = longThroughput / (totalSimulationTime / DEBUG_FACTOR) * 100;
        
        // Output throughputs
        System.out.println("Short job throughput = " + shortThroughput);
        System.out.println("Long job throughput = " + longThroughput);
        System.out.println("Total throughput = " + totalThroughput);

        // Print statistics for short jobs.
        System.out.println("Short jobs statistics:" +
            "\n  Min wait time = " + shortWaitTime[STAT_MIN]/ DEBUG_FACTOR +
            "\n  Max wait time = " + shortWaitTime[STAT_MAX]/ DEBUG_FACTOR +
            "\n  Avg wait time = " + shortWaitTime[STAT_AVG]/ DEBUG_FACTOR +
            "\n  Min service time = " + shortServiceTime[STAT_MIN]/ DEBUG_FACTOR +
            "\n  Max service time = " + shortServiceTime[STAT_MAX]/ DEBUG_FACTOR +
            "\n  Avg service time = " + shortServiceTime[STAT_AVG]/ DEBUG_FACTOR);

        // Print statistics for long jobs.
        System.out.println("Long jobs statistics:" +
            "\n  Min wait time = " + longWaitTime[STAT_MIN]/ DEBUG_FACTOR +
            "\n  Max wait time = " + longWaitTime[STAT_MAX]/ DEBUG_FACTOR +
            "\n  Avg wait time = " + longWaitTime[STAT_AVG]/ DEBUG_FACTOR +
            "\n  Min service time = " + longServiceTime[STAT_MIN]/ DEBUG_FACTOR +
            "\n  Max service time = " + longServiceTime[STAT_MAX]/ DEBUG_FACTOR +
            "\n  Avg service time = " + longServiceTime[STAT_AVG]/ DEBUG_FACTOR);

        System.out.println("---SIMULATION COMPLETE---");
    }

    /**
     * run() method
     * Run upon each thread running its start()
     */
    public void run()
    {

        // Get types and save as local (unshared) variables.
        threadType t_type = tType;
        producerType p_type = pType;

        // Producer logic.
        if(t_type == threadType.Producer)
        {
            // Short job producer logic.  //////////////////////////////////////////
            if(p_type == producerType.ShortJobs)
            {
                // While the simulation is running
                while(simulationCounter > 0)
                {
                    simulationCounter--;  // decrement the counter
                    
                    // Acquire the bufferEmpty semaphore
                    try { bufferEmpty.Pacquire(); }
                        catch(InterruptedException e) {}

                    RRJob j = new RRJob(RRJob.jobType.ShortJob, jobIDCounter++);

                    if(debug)
                        System.out.println("AS" + j.jobID + ':' +
                                System.currentTimeMillis());

                    // Set the job service length to the short service time
                    j.setServiceLength(RRJob.SHORT_SERVICE_TIME); 
                    
                    // Add the job to the list.
                    addJob(j);

                    // Release the semaphore
                    bufferFull.Vrelease();

                }
            }

            // Long job producer logic. ////////////////////////////////////////
            else
            {
                while(simulationCounter > 0)
                {
                    simulationCounter--; // decrement the simulation counter
                    
                    // Acquire the bufferEmpty semaphore
                    try { bufferEmpty.Pacquire(); }
                        catch(InterruptedException e) {}

                    RRJob j = new RRJob(RRJob.jobType.LongJob, jobIDCounter++);
                    
                    if(debug)
                        System.out.println("AL" + j.jobID + ':' +
                                System.currentTimeMillis());

                    // set the service length to the long service time
                    j.setServiceLength(RRJob.LONG_SERVICE_TIME);          
                                
                    // Add the job to the list.
                    addJob(j);

                    // Release the bufferFull semaphore
                    bufferFull.Vrelease();
                }
            }
        }  // End producer logic.


        // CONSUMER LOGIC ////////////////////////////////////////////////////

        else
        {

            int processorID = consumerNum;  // Get the processor ID

            while(simulationCounter > 0)  // while we still have an active counter
            {
                // Remove a job from the queue.
                
                // acquire the bufferFull semaphore
                try {bufferFull.Pacquire(); }
                    catch(InterruptedException e) {}
                
                // Take a look at the item at the head of the list, save in j
                RRJob j = buffer.peek();
                
                // Set the current processor
                j.setProcessorID(processorID);

                if(debug)
                    System.out.println("C" + j.jobID + ':' +
                            System.currentTimeMillis());

                // Release the bufferEmpty semaphore
                bufferEmpty.Vrelease();

                // Mark the serviced time
                j.setServicedTime(System.currentTimeMillis());

                // If processor affinity is set to true
                // Check to see if the current processor ID is different than the 
                // last processor set.  If so, penalty of 1 time unit
                if (AFFINITY == true && processorID != j.getProcessorID())
                {
                    try { Thread.sleep(PROC_SWITCH_PENALTY); }
                        catch(InterruptedException e) {}
                }

                // If the job service length is greater than the timeslice ( >8 )
                // Subtract the timeslice off the service length and have the 
                // amount of the timeslice sleep (8)
                if (j.getServiceLength() > TIMESLICE)
                {
                    
                    try { Thread.sleep(TIMESLICE); }
                        catch(InterruptedException e) {}
                        
                    j.setServiceLength(j.getServiceLength() - TIMESLICE);
                       
                    // temporarily remove and place at the tail of list.
                    RRJob temp = removeJob();
                    addJob(temp);
                }
                
                // Otherwise (if the service time length is less than time timeslice (<= 8),
                // sleep for the remainder of the job service length.  Set the service length to zero then.
                else 
                {
                    try { Thread.sleep(j.getServiceLength()); }
                        catch(InterruptedException e) {}
                        
                    j.setServiceLength(0);
                       
                    // If the job type is a short job, increment the shortThroughput counter
                    if(j.getType() == RRJob.jobType.ShortJob)
                    {
                        shortThroughput++;
                    }
                    
                    // Otherwise, if the job type is a long job, increment the longThroughput counter
                    else if(j.getType() == RRJob.jobType.LongJob)
                    {
                        longThroughput++;
                    }
                       
                    // Mark completion time and update statistics
                    j.setCompletionTime(System.currentTimeMillis());
                    updateStats(j);
                    
                    // Completely remove job.  It is done.
                    removeJob();
                }

                

                // Mark the service time in the processor utilization array at index processorID
                processorUtilization[processorID] += j.getServiceTime();
            }  
        }  // End consumer logic.
    }

    // Add a job to the buffer.
    private synchronized void addJob(RRJob j)
    {
        buffer.add(j);
    }

    // Remove a job from the buffer (service a job).
    // Also updates simulation statistics with the statistics of this job.
    private synchronized RRJob removeJob()
    {
        numJobsServed++;
        RRJob j = buffer.remove();
        j.setCompletionTime(System.currentTimeMillis());
        return j;
    }

    // Update the simulation statistics with the information from a job.
    // This method should be called from the consumer logic after sleeping
    // the appropriate service time for the job.
    private void updateStats(RRJob j)
    {
        // Get current simulation statistics.
        long jServiceTime = j.getServiceTime();
        long jEndTime = j.getCompletionTime();
        long jWaitTime = jEndTime - j.getGenerationTime() - jServiceTime;
        int numJobs = ++numJobsServed;

        // Short job logic.
        if(j.getType() == RRJob.jobType.ShortJob)
        {
            numShortJobsServed++;
            if(jWaitTime < shortWaitTime[STAT_MIN])
                shortWaitTime[STAT_MIN] = jWaitTime;

            if(jWaitTime > shortWaitTime[STAT_MAX])
                shortWaitTime[STAT_MAX] = jWaitTime;

            shortWaitTime[STAT_TOTAL] += jWaitTime;
            shortWaitTime[STAT_AVG] = (shortWaitTime[STAT_AVG] *
                    (numShortJobsServed-1) + jWaitTime) / numShortJobsServed;

            if(jServiceTime < shortServiceTime[STAT_MIN])
                shortServiceTime[STAT_MIN] = jServiceTime;

            if(jServiceTime > shortServiceTime[STAT_MAX])
                shortServiceTime[STAT_MAX] = jServiceTime;

            shortServiceTime[STAT_TOTAL] += jServiceTime;
            shortServiceTime[STAT_AVG] = (shortServiceTime[STAT_AVG] *
                    (numShortJobsServed-1) + jServiceTime) / numShortJobsServed;
        }

        // Long job logic.
        else
        {
            numLongJobsServed++;
            
            if(jWaitTime < longWaitTime[STAT_MIN])
                longWaitTime[STAT_MIN] = jWaitTime;

            if(jWaitTime > longWaitTime[STAT_MAX])
                longWaitTime[STAT_MAX] = jWaitTime;

            longWaitTime[STAT_TOTAL] += jWaitTime;
            longWaitTime[STAT_AVG] = (longWaitTime[STAT_AVG] *
                    (numLongJobsServed-1) + jWaitTime) / numLongJobsServed;

            if(jServiceTime < longServiceTime[STAT_MIN])
                longServiceTime[STAT_MIN] = jServiceTime;

            if(jServiceTime > longServiceTime[STAT_MAX])
                longServiceTime[STAT_MAX] = jServiceTime;

            longServiceTime[STAT_TOTAL] += jServiceTime;
            longServiceTime[STAT_AVG] = (longServiceTime[STAT_AVG] *
                    (numLongJobsServed-1) + jServiceTime) / numLongJobsServed;
        }

    }

    /**
     * main() method.  Initializes the round robin scheduler
     */
    public static void main(String[] args)
    {
        RRScheduler rr = new RRScheduler();
    }
    
    /**
     * exponentialStatDistribution() method
     * Generates an exponential statistical distribution using random stats
     */
    public double exponentialStatDistribution(double avgTime)
    {
        return avgTime * -Math.log(Math.random());   
    }

}

