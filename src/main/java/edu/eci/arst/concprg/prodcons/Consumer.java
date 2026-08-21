package edu.eci.arst.concprg.prodcons;

import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Consumer extends Thread{

    private Queue<Integer> queue;


    public Consumer(Queue<Integer> queue){
        this.queue=queue;
    }

    @Override
    public void run() {
        while (true) {

            int elem;

            synchronized (queue) {

                // Mientras no haya elementos, el consumidor se suspende
                while (queue.isEmpty()) {
                    try {
                        queue.wait();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Consumer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                elem = queue.poll();

                // Despierta a cualquier productor que estuviera esperando
                // porque se había alcanzado el límite de stock.
                queue.notifyAll();
            }

            System.out.println("Consumer consumes " + elem);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Consumer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
