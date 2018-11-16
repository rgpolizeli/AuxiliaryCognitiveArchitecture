/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package executor;

import actionSelection.AuxiliarMethods;
import actionSelection.ExtractedAffordance;
import main.MemoriesNames;
import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryObject;
import java.util.Map;

/**
 *
 * @author ricardo
 */
public class ExecutorHandleCodelet extends Codelet{
    
    private MemoryObject executorsMO;
    private MemoryObject executorParametersMO;
    private MemoryObject executorHandleMO;
    private MemoryObject activatedAffordanceMO;
    private MemoryObject synchronizerMO;
    
    private ExtractedAffordance currentAffordance;
    
    private ExtractedAffordance activatedAffordance;
    private Map<String, Codelet> executors;
    
    public ExecutorHandleCodelet() {
        this.currentAffordance = null;
    }
    
    
   //////////////////////
   // OVERRIDE METHODS //
   //////////////////////
    
    @Override
    public void accessMemoryObjects() {
        this.executorsMO = (MemoryObject) this.getInput(MemoriesNames.EXECUTORS_MO);
        this.activatedAffordanceMO = (MemoryObject) this.getInput(MemoriesNames.ACTIVATED_AFFORDANCE_MO);
        this.executorHandleMO = (MemoryObject) this.getInput(MemoriesNames.EXECUTOR_HANDLE_MO);
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
                        AuxiliarMethods.createLock(executor.getName(), this.synchronizerMO);
                        executor.setLoop(Boolean.TRUE);
                        executor.start();

                        this.currentAffordance = this.activatedAffordance;
                    } else{ //clean variables
                        this.activatedAffordance = null;
                        this.activatedAffordanceMO.setI(this.activatedAffordance);
                        this.currentAffordance = null;
                    }
                    
                }
            }
        }
       
        AuxiliarMethods.synchronize(super.getName(), this.synchronizerMO);
    }
    
}
