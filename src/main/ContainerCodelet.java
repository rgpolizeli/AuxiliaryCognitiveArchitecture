/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import actionSelection.SynchronizationMethods;
import perception.Percept;
import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.core.entities.Mind;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ricardo
 */
public abstract class ContainerCodelet extends Codelet{
    private Mind agentMind;
    
    public ContainerCodelet(Mind m){
        this.agentMind = m;
    }
    
    /**
     * Create codelet and start it.
     * @param className
     * @param parameters
     * @param memoryObjectsInput
     * @param memoryObjectsOutput
     * @return
     */
    public Codelet createCdt(String className, Map<Integer,Percept> parameters, List<MemoryObject> memoryObjectsInput, List<MemoryObject> memoryObjectsOutput){
        Codelet cdt = null;
        Constructor c;
        
        Class[] parametersClasses = new Class[1];
        parametersClasses[0] = Map.class;
        Object[] parametersObjects = new Object[1];
        parametersObjects[0] = parameters;
        
        try {
            
            c = Class.forName(className).getConstructor(parametersClasses);
            c.setAccessible(true);
            
            cdt = (Codelet) c.newInstance(parametersObjects);
            
            for (MemoryObject mo : memoryObjectsInput) {
                 cdt.addInput(mo);
            }
            
            for (MemoryObject mo : memoryObjectsOutput) {
                 cdt.addOutput(mo);
            }
            
            cdt.setTimeStep(0);
            this.agentMind.insertCodelet(cdt);
            cdt.start();
            
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(ContainerCodelet.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return cdt;
    }
    
    /**
     * Create codelet for an executor and start it.
     * @param className
     * @param parametersClasses
     * @param parametersObjects
     * @param memoryObjectsInput
     * @param memoryObjectsOutput
     * @return
     */
    public Codelet createCdt(long id, String className, List<Class> parametersClasses, Object[] parametersObjects, List<MemoryObject> memoryObjectsInput, List<MemoryObject> memoryObjectsOutput){
        Codelet cdt = null;
        Constructor c;
        
        try {
            Class<?>[] classes = new Class<?>[parametersClasses.size()];
            parametersClasses.toArray(classes);
            c = Class.forName(className).getConstructor(classes);
            c.setAccessible(true);
            
            cdt = (Codelet) c.newInstance(parametersObjects);
            
            for (MemoryObject mo : memoryObjectsInput) {
                 cdt.addInput(mo);
            }
            
            for (MemoryObject mo : memoryObjectsOutput) {
                 cdt.addOutput(mo);
            }
            cdt.setName(className + id);
            cdt.setLoop(Boolean.FALSE);
            cdt.setTimeStep(0);
            this.agentMind.insertCodelet(cdt);
            
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(ContainerCodelet.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return cdt;
    }
    
    public void startCodelet(Codelet codelet, MemoryObject synchronizerMO){
        
        /*
        synchronized(synchronizerMO){
            ConcurrentHashMap<String, MyLock> synchronizers = (ConcurrentHashMap<String, MyLock>) synchronizerMO.getI();
            
            String codeletName = codelet.getName();
            MyLock myLock = new MyLock();
            synchronizers.put(codeletName, myLock);
        }
        */
        SynchronizationMethods.createLock(codelet.getName(),synchronizerMO);
        codelet.start();
    }
    
    
    /**
     * Stop codelet, release your synchronizer and destroy it.
     * @param codelet
     * @param synchronizerMO
     */
    public void deleteCdt(Codelet codelet, MemoryObject synchronizerMO){
        
        //if (SynchronizationMethods.destroyLock(codelet.getName()) == 1) {
            this.agentMind.getCodeRack().destroyCodelet(codelet);
            //return true;
        //} else{
          //  return false;
        //}
        
        /*
        ConcurrentHashMap<String, MyLock> synchronizers = (ConcurrentHashMap<String, MyLock>) synchronizerMO.getI();
        //MyLock myLock = synchronizers.get(codelet.getName());
        
        //if (myLock.isLocked) {
            //setLoop to false
            this.agentMind.getCodeRack().destroyCodelet(codelet);

            //release synchronizer codelet lock


            synchronized(myLock.lock){
                try{

                    //myLock.unlock();
                    System.out.println("Antes do Notify do codelet: " + codelet.getName());
                    myLock.lock.notify();

                } catch(Exception ex){
                    if (ex instanceof java.lang.NullPointerException) {
                        //not problem
                    } else{
                      
                    }
                }
                synchronizers.remove(codelet.getName());
                System.out.println("Lock deletado do codelet: " + codelet.getName());
            }

            synchronizers.remove(codelet.getName());
            
            return true;
        //}else{
          //  return false;
        //}
        */
    }
    
    
}
