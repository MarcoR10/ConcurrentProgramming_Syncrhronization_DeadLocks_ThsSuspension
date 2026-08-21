package edu.eci.arst.concprg.prodcons;

import java.util.Queue;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Producer extends Thread {

    private Queue<Integer> queue = null;

    private int dataSeed = 0;
    private Random rand=null;
    private final long stockLimit;

    public Producer(Queue<Integer> queue,long stockLimit) {
        this.queue = queue;
        rand = new Random(System.currentTimeMillis());
        this.stockLimit=stockLimit;
    }

    @Override
    public void run() {
        while (true) {

            synchronized (queue) {

                // Si ya se alcanzó el límite de stock, el productor espera
                // (se suspende) en vez de seguir intentando producir.
                // Se usa 'while' (no 'if') para protegerse de:
                //  - que otro productor haya vuelto a llenar la cola
                //  - entre el notify y la re-adquisición del lock.
                while (queue.size() >= stockLimit) {
                    try {
                        queue.wait();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Producer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                dataSeed = dataSeed + rand.nextInt(100);
                System.out.println("Producer added " + dataSeed);
                queue.add(dataSeed);

                // Despierta a cualquier consumidor que estuviera esperando
                // porque la cola estaba vacía.
                queue.notifyAll();
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                Logger.getLogger(Producer.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }
}
