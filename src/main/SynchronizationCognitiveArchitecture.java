/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import actionSelection.SynchronizationMethods;
import br.unicamp.cst.core.entities.Codelet;
import synchronization.MyLock;
import br.unicamp.cst.core.entities.MemoryObject;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rgpolizeli
 */
public class SynchronizationCognitiveArchitecture {
    
    private static final Logger LOGGER = Logger.getLogger(SynchronizationCognitiveArchitecture.class.getName());
    
    public SynchronizationCognitiveArchitecture() {
    }
    
    public static void synchronizeExecutor(String executorName, Codelet codelet, MemoryObject executorHandleMO, MemoryObject synchronizerMO){
        SynchronizationMethods.getSynchronizerLock().lock();
        Map<String, MyLock> myLocks = (Map<String, MyLock>) synchronizerMO.getI();
        try{
            
            executorHandleMO.setI(Boolean.FALSE);
            codelet.setLoop(Boolean.FALSE);
            SynchronizationMethods.destroyLock(executorName, myLocks);
            
            
        } finally{
            SynchronizationMethods.getSynchronizerLock().unlock();
        }
    }
    
}
