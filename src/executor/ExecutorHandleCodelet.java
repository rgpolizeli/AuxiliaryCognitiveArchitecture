/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package executor;

import actionSelection.SynchronizationMethods;
import actionSelection.ExtractedAffordance;
import main.MemoriesNames;
import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.AuxiliarMethods;
import perception.Percept;

/**
 *
 * @author rgpolizeli
 */
public class ExecutorHandleCodelet extends Codelet{
    
    private MemoryObject executorsMO;
    private MemoryObject executorParametersMO;
    private MemoryObject executorHandleMO;
    private MemoryObject activatedAffordanceMO;
    private MemoryObject workingMO;
    private MemoryObject reasonerMO;
    private MemoryObject synchronizerMO;
    
    private ExtractedAffordance currentAffordance;
    
    private ExtractedAffordance activatedAffordance;
    private Map<String, Codelet> executors;
    
    private final String reasonerCategoryInWMO= "REASONER";
    
    private final Logger LOGGER = Logger.getLogger(ExecutorHandleCodelet.class.getName());
    
    public ExecutorHandleCodelet() {
        this.currentAffordance = null;
    }
    
    
    /**
     * Deep copy of memory object.
     * @param mo
     * @return 
     */
    private Map<String,Map<Percept,Double>> getCopyOfMemoryContent(MemoryObject mo){
        Map<String,Map<Percept,Double>> copy;
        synchronized(mo){
            copy =  (Map<String,Map<Percept,Double>>) mo.getI();
            copy = AuxiliarMethods.deepCopyMemoryMap(copy);
        }
        return copy;
    }
    
    private void addReasonerPerceptsToWorkingMO(){
        
        Map<String,Map<Percept,Double>> reasonerPerceptsCopy = getCopyOfMemoryContent(this.reasonerMO);
        
        synchronized(this.workingMO){
            Map<String,Map<String,List<Percept>>> workingMemory = (Map<String,Map<String,List<Percept>>>) this.workingMO.getI();
            Map<String,List<Percept>> reasonerPerceptsInWMO = workingMemory.get(this.reasonerCategoryInWMO);

            if (reasonerPerceptsInWMO == null) {
                reasonerPerceptsInWMO = new HashMap<>();
                workingMemory.put(this.reasonerCategoryInWMO, reasonerPerceptsInWMO);
            } else{
                reasonerPerceptsInWMO.clear(); // ou voce apaga toda esta memoria ou e necessario uma function para apagar os percepts deletados.
            }

            for (Map.Entry<String, Map<Percept,Double>> entry : reasonerPerceptsCopy.entrySet()) {
                String category = entry.getKey();
                Map<Percept, Double> perceptsOfCategoryMap = entry.getValue();

                if (reasonerPerceptsInWMO.containsKey(category)) {
                    List<Percept> perceptsOfCategory = reasonerPerceptsInWMO.get(category);
                    for (Percept p : perceptsOfCategoryMap.keySet()) {
                        if (!perceptsOfCategory.contains(p)) {
                            perceptsOfCategory.add(p);
                        }
                    }
                } else{
                    List<Percept> perceptsOfCategory = new ArrayList<>();
                    perceptsOfCategory.addAll(perceptsOfCategoryMap.keySet());
                    reasonerPerceptsInWMO.put(category, perceptsOfCategory);
                }
            }

            workingMemory.put(this.reasonerCategoryInWMO, reasonerPerceptsInWMO);
        }
    }
    
   //////////////////////
   // OVERRIDE METHODS //
   //////////////////////
    
    @Override
    public void accessMemoryObjects() {
        this.executorsMO = (MemoryObject) this.getInput(MemoriesNames.EXECUTORS_MO);
        this.activatedAffordanceMO = (MemoryObject) this.getInput(MemoriesNames.ACTIVATED_AFFORDANCE_MO);
        this.executorHandleMO = (MemoryObject) this.getInput(MemoriesNames.EXECUTOR_HANDLE_MO);
        this.reasonerMO = (MemoryObject) this.getInput(MemoriesNames.REASONER_MO);
        this.workingMO = (MemoryObject) this.getInput(MemoriesNames.WORKING_MO);
        this.synchronizerMO = (MemoryObject) this.getInput(MemoriesNames.SYNCHRONIZER_MO);
        
        this.executorParametersMO = (MemoryObject) this.getOutput(MemoriesNames.EXECUTOR_PARAMETERS_MO);
    }

    @Override
    public void calculateActivation() {
        
    }

    @Override
    public void proc() {
        this.executors = (Map<String, Codelet>) this.executorsMO.getI();
        
        synchronized(this.activatedAffordanceMO){
            
            this.activatedAffordance = (ExtractedAffordance) this.activatedAffordanceMO.getI();
            
            if (this.activatedAffordance != null && this.executors.get(this.activatedAffordance.getAffordanceType().getAffordanceName()) != null) { //if there is activatedAffordance and if there is an executor to this activated, i.e., if this activated affordance don't trigger reasoner operations.

                boolean alreadyInExecution = (boolean) this.executorHandleMO.getI();
                Codelet currentExecutor = null;
                if (this.currentAffordance != null) {
                    currentExecutor = this.executors.get(this.currentAffordance.getAffordanceType().getAffordanceName());
                }
                if( !alreadyInExecution && (currentExecutor == null || !currentExecutor.isLoop()) ){ //if any executor isn't in execution and the current executor alreay free system's synchronization lock
                    
                    if (!this.activatedAffordance.equals(this.currentAffordance)) { //start the new executor's codelet
                        
                        this.executorParametersMO.setI(this.activatedAffordance.getPerceptsPermutation());
                        Codelet executor = this.executors.get(this.activatedAffordance.getAffordanceType().getAffordanceName());
                        this.executorHandleMO.setI(Boolean.TRUE); //executor in execution
                        SynchronizationMethods.createLock(executor.getName(), this.synchronizerMO);
                        executor.setLoop(Boolean.FALSE);
                        executor.setTimeStep(0);
                        executor.start();
                        
                        LOGGER.log(Level.INFO, "Executor codelet {0} initiated.", new Object[]{executor.getName()});
                        
                        this.currentAffordance = this.activatedAffordance;
                        
                    } else{ //clean variables
                        
                        this.activatedAffordance = null;
                        this.activatedAffordanceMO.setI(this.activatedAffordance);
                        this.currentAffordance = null;
                        LOGGER.log(Level.INFO, "Cleaned variables.");
                    }
                    
                }
            }
        }
        
        addReasonerPerceptsToWorkingMO();
        
        SynchronizationMethods.synchronize(super.getName(), this.synchronizerMO);
    }
    
}
