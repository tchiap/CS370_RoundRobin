/* Semaphore.java
 * Implements a Semaphore for Java threads.
 */

class Semaphore
{
    private int value;

    public Semaphore()
    {
            value = 0;
    }  // Semaphore()

    public Semaphore(int value)
    {
            this.value = value;
    }  // Semaphore()

    public synchronized void Pacquire() throws InterruptedException
    {
            while (value == 0)
                    wait();
            value--;
    }  // Paxquire()

    public synchronized void Vrelease()
    {
            ++value;
            notify();
    }  // Vrelease()
}  // Semaphore class
