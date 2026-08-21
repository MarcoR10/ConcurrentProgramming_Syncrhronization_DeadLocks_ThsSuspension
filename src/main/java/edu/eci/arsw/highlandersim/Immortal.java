package edu.eci.arsw.highlandersim;

import java.util.List;
import java.util.Random;

public class Immortal extends Thread {

    private ImmortalUpdateReportCallback updateCallback=null;
    private int health;
    private int defaultDamageValue;
    private final List<Immortal> immortalsPopulation;
    private final String name;
    private final Random r = new Random(System.currentTimeMillis());
    private boolean paused = false;

    // Lock de desempate para colisiones de identityHashCode
    private static final Object tieLock = new Object();

    // Flag para detener el hilo permanentemente
    private volatile boolean stopped = false;


    public Immortal(String name, List<Immortal> immortalsPopulation, int health, int defaultDamageValue, ImmortalUpdateReportCallback ucb) {
        super(name);
        this.updateCallback=ucb;
        this.name = name;
        this.immortalsPopulation = immortalsPopulation;
        this.health = health;
        this.defaultDamageValue=defaultDamageValue;
    }

    public void run() {

        while (true) {

            checkPaused();

            Immortal im;

            int myIndex = immortalsPopulation.indexOf(this);

            int nextFighterIndex = r.nextInt(immortalsPopulation.size());

            //avoid self-fight
            if (nextFighterIndex == myIndex) {
                nextFighterIndex = ((nextFighterIndex + 1) % immortalsPopulation.size());
            }

            im = immortalsPopulation.get(nextFighterIndex);

            this.fight(im);

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }

    /**
     * Pausa el hilo de manera segura utilizando wait()
     */
    private synchronized void checkPaused() {
        while (paused) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Cambia el estado a pausado
     */
    public void pauseThread() {
        this.paused = true;
    }

    /**
     * Reanuda el hilo y notifica a wait()
     */
    public synchronized void resumeThread() {
        this.paused = false;
        notifyAll();
    }

    /**
     * Región Crítica Controlada: Bloques sincronizados anidados con ordenamiento
     * para evitar condiciones de carrera y Deadlocks.
     */
    public void fight(Immortal i2) {

        int myHash = System.identityHashCode(this);
        int otherHash = System.identityHashCode(i2);

        if (myHash < otherHash) {
            synchronized (this) {
                synchronized (i2) {
                    doFight(i2);
                }
            }
        } else if (myHash > otherHash) {
            synchronized (i2) {
                synchronized (this) {
                    doFight(i2);
                }
            }
        } else {
            // Caso de colisión de hash: se usa el tieLock de desempate
            synchronized (tieLock) {
                synchronized (this) {
                    synchronized (i2) {
                        doFight(i2);
                    }
                }
            }
        }

    }

    /**
     * Detiene la ejecución del hilo permanentemente.
     */
    public synchronized void stopThread() {
        this.stopped = true;
        this.paused = false;
        notifyAll(); // Despierta el hilo si está en wait()
        this.interrupt(); // Interrumpe el hilo si está en sleep()
    }

    private void doFight(Immortal i2) {
        if (i2.getHealth() > 0) {
            i2.changeHealth(i2.getHealth() - defaultDamageValue);
            this.health += defaultDamageValue;
            updateCallback.processReport("Fight: " + this + " vs " + i2 + "\n");
        } else {
            updateCallback.processReport(this + " says:" + i2 + " is already dead!\n");
        }
    }

    public void changeHealth(int v) {
        health = v;
    }

    public int getHealth() {
        return health;
    }

    @Override
    public String toString() {
        return name + "[" + health + "]";
    }

}
