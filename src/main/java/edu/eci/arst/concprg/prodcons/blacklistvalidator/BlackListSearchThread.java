/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.eci.arst.concprg.prodcons.blacklistvalidator;

import edu.eci.arsw.spamkeywordsdatasource.HostBlacklistsDataSourceFacade;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hilo encargado de revisar un segmento (sub-rango) del conjunto total de
 * servidores de listas negras, en busca de una IP determinada.
 *
 * Varios hilos de este tipo comparten (a través del constructor) el mismo
 * estado de resultados: la lista de listas negras donde se encontró la IP,
 * el conteo total de ocurrencias, y una bandera de "alarma alcanzada". Esto
 * permite que, apenas entre TODOS los hilos se llegue al número de
 * ocurrencias requerido (BLACK_LIST_ALARM_COUNT), los demás dejen de seguir
 * buscando en el resto de su segmento.
 *
 * @author hcadavid
 */
public class BlackListSearchThread extends Thread {

    private final HostBlacklistsDataSourceFacade skds;
    private final String ipaddress;
    private final int startIndex;
    private final int endIndex; // exclusivo
    private final int blackListAlarmCount;

    // ---- Estado COMPARTIDO entre todos los hilos de una misma búsqueda ----
    private final Object lock;
    private final List<Integer> sharedOccurrences;
    private final AtomicInteger sharedOccurrencesCount;
    private final AtomicBoolean alarmReached;

    // ---- Estado propio de este hilo ----
    private int ownOccurrencesFound = 0;
    private int checkedServersCount = 0;

    public BlackListSearchThread(HostBlacklistsDataSourceFacade skds, String ipaddress,
            int startIndex, int endIndex, int blackListAlarmCount,
            Object lock, List<Integer> sharedOccurrences,
            AtomicInteger sharedOccurrencesCount, AtomicBoolean alarmReached) {
        this.skds = skds;
        this.ipaddress = ipaddress;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.blackListAlarmCount = blackListAlarmCount;
        this.lock = lock;
        this.sharedOccurrences = sharedOccurrences;
        this.sharedOccurrencesCount = sharedOccurrencesCount;
        this.alarmReached = alarmReached;
    }

    @Override
    public void run() {

        for (int i = startIndex; i < endIndex; i++) {

            // Parada anticipada: si el conjunto de hilos ya encontró las
            // ocurrencias necesarias, no seguir revisando el resto del
            // segmento propio. AtomicBoolean garantiza que este cambio,
            // hecho por cualquier otro hilo, sea visible aquí de inmediato.
            if (alarmReached.get()) {
                break;
            }

            checkedServersCount++;

            if (skds.isInBlackListServer(i, ipaddress)) {

                // Sección crítica: agregar a la lista compartida e
                // incrementar el conteo compartido deben hacerse como una
                // sola operación atómica, para que ningún par de hilos
                // agregue elementos de más una vez alcanzado el límite
                // (condición de carrera clásica de "check-then-act").
                synchronized (lock) {
                    if (sharedOccurrencesCount.get() < blackListAlarmCount) {
                        sharedOccurrences.add(i);
                        ownOccurrencesFound++;
                        int newCount = sharedOccurrencesCount.incrementAndGet();
                        if (newCount >= blackListAlarmCount) {
                            alarmReached.set(true);
                        }
                    } else {
                        alarmReached.set(true);
                    }
                }
            }
        }
    }

    /**
     * Permite 'preguntarle' a este hilo cuántas ocurrencias de servidores
     * maliciosos ha encontrado (o encontró) él mismo, en su propio
     * segmento de búsqueda.
     *
     * @return número de ocurrencias encontradas por este hilo.
     */
    public int getOccurrencesFound() {
        return ownOccurrencesFound;
    }

    /**
     * @return número de servidores de listas negras que este hilo
     * efectivamente alcanzó a revisar (puede ser menor al tamaño de su
     * segmento si la búsqueda se detuvo antes).
     */
    public int getCheckedServersCount() {
        return checkedServersCount;
    }
}
