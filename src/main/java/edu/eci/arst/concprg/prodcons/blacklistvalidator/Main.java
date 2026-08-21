/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.eci.arst.concprg.prodcons.blacklistvalidator;

import java.util.List;

/**
 *
 * @author hcadavid
 */
public class Main {

    public static void main(String a[]){

        // Número de hilos entre los que se reparte la búsqueda.
        // Se puede pasar por línea de comandos (args[0]); si no se indica,
        // se usa el número de núcleos disponibles como valor por defecto
        // (útil para la Parte III - evaluación de desempeño).
        int n = (a.length > 0) ? Integer.parseInt(a[0]) : Runtime.getRuntime().availableProcessors();

        HostBlackListsValidator hblv=new HostBlackListsValidator();

        long start = System.currentTimeMillis();
        List<Integer> blackListOcurrences=hblv.checkHost("200.24.34.55", n);
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("The host was found in the following blacklists:"+blackListOcurrences);
        System.out.println("Threads used: "+n+" | Elapsed time: "+elapsed+" ms");

    }

}
