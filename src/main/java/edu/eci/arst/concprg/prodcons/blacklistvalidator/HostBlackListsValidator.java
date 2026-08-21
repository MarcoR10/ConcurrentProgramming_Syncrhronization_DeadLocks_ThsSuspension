/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.eci.arst.concprg.prodcons.blacklistvalidator;

import edu.eci.arsw.spamkeywordsdatasource.HostBlacklistsDataSourceFacade;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hcadavid
 */
public class HostBlackListsValidator {

    private static final int BLACK_LIST_ALARM_COUNT=5;

    /**
     * Check the given host's IP address in all the available black lists,
     * and report it as NOT Trustworthy when such IP was reported in at least
     * BLACK_LIST_ALARM_COUNT lists, or as Trustworthy in any other case.
     * The search is not exhaustive: When the number of occurrences is equal to
     * BLACK_LIST_ALARM_COUNT, the search is finished, the host reported as
     * NOT Trustworthy, and the list of the five blacklists returned.
     *
     * La búsqueda se reparte entre 'n' hilos, cada uno revisando un
     * sub-segmento del total de servidores disponibles. La búsqueda se
     * detiene apenas, entre TODOS los hilos, se alcanza el número de
     * ocurrencias requerido (BLACK_LIST_ALARM_COUNT), sin condiciones de
     * carrera sobre el resultado compartido.
     *
     * @param ipaddress suspicious host's IP address.
     * @param n número de hilos entre los que se reparte la búsqueda.
     * @return  Blacklists numbers where the given host's IP address was found.
     */
    public List<Integer> checkHost(String ipaddress, int n){

        HostBlacklistsDataSourceFacade skds=HostBlacklistsDataSourceFacade.getInstance();

        int totalServers=skds.getRegisteredServersCount();

        // Reparto del espacio de búsqueda entre los n hilos. Se usa
        // baseSize + resto para que funcione tanto si n es par como impar,
        // y aunque totalServers no sea múltiplo exacto de n (los primeros
        // 'remainder' hilos reciben un servidor adicional).
        int baseSize=totalServers/n;
        int remainder=totalServers%n;

        // ---- Estado compartido entre los n hilos ----
        Object lock=new Object();
        List<Integer> sharedOccurrences=Collections.synchronizedList(new LinkedList<Integer>());
        AtomicInteger sharedOccurrencesCount=new AtomicInteger(0);
        AtomicBoolean alarmReached=new AtomicBoolean(false);

        BlackListSearchThread[] threads=new BlackListSearchThread[n];

        int start=0;
        for (int t=0; t<n; t++){
            int size=baseSize + (t<remainder?1:0);
            int end=start+size;

            threads[t]=new BlackListSearchThread(skds, ipaddress, start, end,
                    BLACK_LIST_ALARM_COUNT, lock, sharedOccurrences,
                    sharedOccurrencesCount, alarmReached);
            threads[t].start();

            start=end;
        }

        // Esperar a que TODOS los hilos terminen antes de calcular el
        // resultado final (ver API de concurrencia: Thread.join()).
        int checkedListsCount=0;
        for (BlackListSearchThread thread: threads){
            try {
                thread.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(HostBlackListsValidator.class.getName()).log(Level.SEVERE, null, ex);
            }
            checkedListsCount+=thread.getCheckedServersCount();
        }

        LinkedList<Integer> blackListOcurrences=new LinkedList<>(sharedOccurrences);

        if (sharedOccurrencesCount.get()>=BLACK_LIST_ALARM_COUNT){
            skds.reportAsNotTrustworthy(ipaddress);
        }
        else{
            skds.reportAsTrustworthy(ipaddress);
        }

        LOG.log(Level.INFO, "Checked Black Lists:{0} of {1}", new Object[]{checkedListsCount, totalServers});

        return blackListOcurrences;
    }


    private static final Logger LOG = Logger.getLogger(HostBlackListsValidator.class.getName());



}
