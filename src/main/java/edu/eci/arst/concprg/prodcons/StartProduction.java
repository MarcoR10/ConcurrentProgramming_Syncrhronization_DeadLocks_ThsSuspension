/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.eci.arst.concprg.prodcons;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StartProduction {

    // Límite de stock (Parte I, punto 3). Con producción rápida y consumo
    // lento, el productor debe suspenderse al llegar a este tamaño en vez
    // de seguir agregando indefinidamente.
    private static final long STOCK_LIMIT = 10;

    public static void main(String[] args) {

        Queue<Integer> queue=new LinkedBlockingQueue<>();


        new Producer(queue,STOCK_LIMIT).start();

        //let the producer create products for 5 seconds (stock).
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            Logger.getLogger(StartProduction.class.getName()).log(Level.SEVERE, null, ex);
        }


        new Consumer(queue).start();
    }


}