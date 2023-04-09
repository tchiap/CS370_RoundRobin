/* OSScheduler.java

 * Emulates an operating system scheduler.

 */



import java.io.*;

import java.lang.Thread;

import java.util.LinkedList;

import java.lang.System;

import java.util.Arrays;



public class OSScheduler implements Runnable

{

    private int jobIDCounter = 0;

    

    // Set debug to true to print job production and consumption times.

    private boolean debug = false;

    

    // Static variables.

    // For the single-processor version, use NUM_CONSUMERS = 1.

    // For the multi-processor version, use NUM_CONSUMERS = 2.

    private static final int NUM_PRODUCERS = 2;

    private static final int NUM_CONSUMERS = 2;

    // BUFFER_SIZE controls the max number of jobs in the buffer.

    private static final int BUFFER_SIZE = 10;

    // Multiplying factor to JOB_FREQ and SERVICE_TIME variables for debug.

    public static final int DEBUG_FACTOR = 100;

    // SHORT_JOB_FREQ and LONG_JOB_FREQ control the frequency of job creation.

    private static final int SHORT_JOB_FREQ = 2 * DEBUG_FACTOR;

    private static final int LONG_JOB_FREQ = 5 * DEBUG_FACTOR;

    // Controls the length of the simulation.

    private static final int SIMULATION_LENGTH = 50;

    

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

    private int consumerNum;

    

    // Arrays of producers and consumers.

    private Thread[] producers;

    private Thread[] consumers;

    // The job buffer.

    private LinkedList<Job> buffer = new LinkedList<Job>();

    

    // Semaphores control the number of jobs in the buffer (0 - BUFFER_SIZE).

    Semaphore bufferEmpty = new Semaphore(BUFFER_SIZE);

    Semaphore bufferFull = new Semaphore(0);

	

    // Simulation statistics variables.

    private int numJobsServed = 0;

    private long simulationStartTime;

    // Wait and service time arrays contain the min, max, total, and average

    // for each job type and are updated continuously during the simulation.

    private static final int STAT_MIN = 0;

    private static final int STAT_MAX = 1;

    private static final int STAT_TOTAL = 2;

    private static final int STAT_AVG = 3;

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

    

    public OSScheduler()

    {

        // Set simulation start time.

        simulationStartTime = System.currentTimeMillis();

	

        // Initialize some stuff.

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

        producers[0] = new Thread(this);

        producers[0].start();

        

        // Give first thread a chance to grab its type = ShortJobs.

        try { Thread.sleep(5); } catch(InterruptedException e) {}

        // One producer creates long jobs.

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

		

        for(int j = 0; j < NUM_CONSUMERS; j++)

        {

			consumerNum = j;

            consumers[j] = new Thread(this);

            consumers[j].start();

            try { Thread.sleep(5); } catch(InterruptedException e) {}

        }  // for

		

        // Wait for simulation to end, then calculate and print

        // simulation statistics.

        while(simulationCounter > 0)

            try { Thread.sleep(100); } catch(InterruptedException e) {}

        

        System.out.println("---SIMULATION STATISTICS---");

        

        long simulationEndTime = System.currentTimeMillis();

        long totalSimulationTime = simulationEndTime - simulationStartTime;

        

        // Calculate and print processor utilization for each consumer.

        for(int k = 0; k < NUM_CONSUMERS; k++)

        {

                processorUtilization[k] =

                        processorUtilization[k] / totalSimulationTime * 100;

                System.out.println("Processor " + k + " utilization % = "

                        + processorUtilization[k]);

        }  // for

        

        // Calculate and print throughput for each job type.

        double totalThroughput = shortThroughput + longThroughput;

        totalThroughput = totalThroughput / (totalSimulationTime / DEBUG_FACTOR) * 100;

        shortThroughput = shortThroughput / (totalSimulationTime / DEBUG_FACTOR) * 100;

        longThroughput = longThroughput / (totalSimulationTime / DEBUG_FACTOR) * 100;

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

    }  // OSScheduler()

    

    public void run()

    {

        // Get types and save as local (unshared) variables.

        threadType t_type = tType;

        producerType p_type = pType;

        

        // Producer logic.

        if(t_type == threadType.Producer)

        {

            // Short job producer logic.

            if(p_type == producerType.ShortJobs)

            {

                while(simulationCounter > 0)

                {

                    simulationCounter--;

                    // Add a short job to the queue.

                    try { bufferEmpty.Pacquire(); }

                        catch(InterruptedException e) {}

                    Job j = new Job(Job.jobType.ShortJob, jobIDCounter++);

                    if(debug)

                        System.out.println("AS" + j.jobID + ':' +

                                System.currentTimeMillis());

                    addJob(j);

                    bufferFull.Vrelease();

                    try { Thread.sleep(SHORT_JOB_FREQ); }

                        catch(InterruptedException e) {}

                }  // while

            }  // if

            

            // Long job producer logic.

            else

            {

                while(simulationCounter > 0)

                {

                    simulationCounter--;

                    // Add a long job to the queue.

                    try { bufferEmpty.Pacquire(); }

                        catch(InterruptedException e) {}

                    Job j = new Job(Job.jobType.LongJob, jobIDCounter++);

                    if(debug)

                        System.out.println("AL" + j.jobID + ':' +

                                System.currentTimeMillis());

                    addJob(j);

                    bufferFull.Vrelease();

                    try { Thread.sleep(LONG_JOB_FREQ); }

                        catch(InterruptedException e) {}

                }  // while

            }  // else

        }  // End producer logic.

        

        // Consumer logic.

        else

        {

            int processorID = consumerNum;

            while(simulationCounter > 0)

            {

                // Remove a job from the queue.

                try {bufferFull.Pacquire(); }

                    catch(InterruptedException e) {}

                Job j = removeJob();

                if(debug)

                    System.out.println("C" + j.jobID + ':' +

                            System.currentTimeMillis());

                bufferEmpty.Vrelease();

                j.setServicedTime(System.currentTimeMillis());

                // Sleep for the amount of time specified by the job type,

                // simulating servicing the job.

                if(j.getType() == Job.jobType.ShortJob)

                {

                    shortThroughput++;

                    try { Thread.sleep(Job.SHORT_SERVICE_TIME); }

                        catch(InterruptedException e) {}

                }  // if

                else

                {

                    longThroughput++;

                    try { Thread.sleep(Job.LONG_SERVICE_TIME); }

                        catch(InterruptedException e) {}

                }  // else

		j.setCompletionTime(System.currentTimeMillis());

		updateStats(j);

		processorUtilization[processorID] += j.getServiceTime();

            }  // while

        }  // End consumer logic.

    }  // run()

    

    // Add a job to the buffer.

    private synchronized void addJob(Job j)

    {

        buffer.add(j);

    }  // adJob()

    

    // Remove a job from the buffer (service a job).

	// Also updates simulation statistics with the statistics of this job.

    private synchronized Job removeJob()

    {

        numJobsServed++;

        Job j = buffer.remove();

        j.setCompletionTime(System.currentTimeMillis());

        return j;

    }  // removeJob()

	

    // Update the simulation statistics with the information from a job.

    // This method should be called from the consumer logic after sleeping

    // the appropriate service time for the job.

    private void updateStats(Job j)

    {

        // Get current simulation statistics.

        long jServiceTime = j.getServiceTime();

        long jEndTime = j.getCompletionTime();

        long jWaitTime = jEndTime - j.getGenerationTime() - jServiceTime;

        int numJobs = ++numJobsServed;



        // Short job logic.

        if(j.getType() == Job.jobType.ShortJob)

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

        }  // if

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

        }  // else

    }  // updateStats()

    

    public static void main(String[] args)

    {

        OSScheduler os = new OSScheduler();

    }  // main()

}  // OSScheduler class

