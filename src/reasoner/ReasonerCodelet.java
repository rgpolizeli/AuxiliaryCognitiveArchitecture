/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reasoner;

import actionSelection.AuxiliarMethods;
import main.ContainerCodelet;
import actionSelection.ExtractedAffordance;
import main.MemoriesNames;
import executor.ExecutorInfo;
import motivation.Drive;
import perception.Percept;
import perception.Relation;
import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.core.entities.Mind;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import perception.Property;

/**
 *
 * @author ricardo
 */
public class ReasonerCodelet extends ContainerCodelet{

    private final Mind m;
    
    private MemoryObject driveMO;
    private MemoryObject operationsMO;
    private MemoryObject reasonerMO;
    private MemoryObject workingMO;
    private MemoryObject activatedAffordanceMO;
    private MemoryObject synchronizerMO;
    
    private List<Drive> drives;
    private Map<String, ExecutorInfo> operationsMap; //this parameter associate the affordanceType that invoke this operation
    private ExtractedAffordance activatedAffordance;
    private List<Operation> reasonerOperations;
    private List<Long> reasonerOperationsIds;
    private Map<String,Map<Percept,Double>> reasonerPercepts;
    private List<Percept> toReplacePercepts;
    private ConcurrentHashMap<String, Boolean> synchronizers;
   
    private Map<ExtractedAffordance, Date> executedOperations;
    
    private final int NOT_STARTED = 0; //codelet not started.
    private final int STARTED = 1; //codelet already started.
    private final int DONE = 2; //codelet completed, but the percepts produced were not read
    private final int COMMITED = 3; //codelet completed and the percepts produced were read. Operation can be deleted.
    
    private final String reasonerCategoryInWMO= "REASONER";
    
    int reasonerMOCapacity = 20;
    private double maxActivation = 1.0;
    private double minActivation = 0.0;
    private double deleteThreshold = this.minActivation;
    private double replaceThreshold = (this.maxActivation/5); //the objective is to remove percepts that in proximity to deleteThreshold
    private double incrementPerCycle = 0.1;
    private double decrementPerCycle = 0.1;
    
    private Random rn;
    
    
    public ReasonerCodelet(Mind m, Map<String, ExecutorInfo> operationsMap, int reasonerMOCapacity, double maxActivation, double minActivation, double deleteThreshold, double replaceThreshold, double incrementPerCycle, double decrementPerCycle) {
        super(m);
        
        this.m = m;
        this.operationsMap = operationsMap;
        this.reasonerMOCapacity = reasonerMOCapacity;
        this.maxActivation = maxActivation;
        this.minActivation = minActivation;
        this.deleteThreshold = deleteThreshold;
        this.replaceThreshold = replaceThreshold;
        this.incrementPerCycle = incrementPerCycle;
        this.decrementPerCycle = decrementPerCycle;
        
        this.rn = new Random();
        this.executedOperations = new HashMap<>();
        this.reasonerOperationsIds = new ArrayList<>();
    }
    
    
    private int countTotalOfPerceptsInMO(){
        
        int total = 0;
        
        if (this.reasonerPercepts != null) {
            for(Map<Percept,Double> map : this.reasonerPercepts.values()){
                total+= map.keySet().size();
            }
        }
        
        return total;
    }
    
    private void insertPerceptInMemory(Percept p, MemoryObject mo){
        
        synchronized(mo){
            
            Map<String, Map<Percept, Double>> memoryPercepts = (Map<String, Map<Percept, Double>>) mo.getI();
            Map<Percept, Double> memoryPerceptsOfCategory;
            
            if(memoryPercepts.containsKey(p.getCategory())){
                memoryPerceptsOfCategory = memoryPercepts.get(p.getCategory());
                
            } else{
                memoryPerceptsOfCategory = new HashMap<>();
                memoryPercepts.put(p.getCategory(), memoryPerceptsOfCategory);
            }
            
            memoryPerceptsOfCategory.put(p, this.incrementPerCycle);
        }
    }
    
    private void removePerceptFromMemory(Percept p, MemoryObject mo){
        
        synchronized(mo){
            
            Map<String, Map<Percept, Double>> memoryPercepts = (Map<String, Map<Percept, Double>>) mo.getI();
            Map<Percept, Double> memoryPerceptsOfCategory;
            
            if(memoryPercepts.containsKey(p.getCategory())){
                memoryPerceptsOfCategory = memoryPercepts.get(p.getCategory());
                if (memoryPerceptsOfCategory.containsKey(p)) {
                    memoryPerceptsOfCategory.remove(p);
                    if (memoryPerceptsOfCategory.isEmpty()) {
                        memoryPercepts.remove(p.getCategory());
                    }
                }
            } 
        }
        
    }
    
    private void refreshRelations(Percept newPercept, Percept currentPercept){
        List<Relation> newRelations = newPercept.getRelations();
        List<Relation> currentRelations = currentPercept.getRelations();
        
        currentRelations.removeAll(newRelations); //get relations that must will be deleted.
        
        for (Relation currentRelation : currentRelations) {
            if (currentPercept.removeRelation(currentRelation) == 1) {
                
            } else{
                //err
            }

        }
        
        for (Relation newRelation : newRelations) {
            currentPercept.replaceRelation(newRelation.getType(), newRelation);
        }
    }
    
    private void incrementPerceptInMemory(Percept percept, MemoryObject mo, double incrementPerCycle, double maxActivation){
        Map<String,Map<Percept, Double>> memoryMap = (Map<String,Map<Percept, Double>>) mo.getI();
        Map<Percept, Double> memoryPerceptsOfCategory = memoryMap.get(percept.getCategory());
        
        double perceptActivation = this.getPerceptActivation(percept, memoryPerceptsOfCategory);
        perceptActivation += incrementPerCycle;

        if (perceptActivation > maxActivation) {
            perceptActivation = maxActivation;
        }

        synchronized(mo){
            memoryPerceptsOfCategory.replace(percept, perceptActivation);
        }
    }
    
    
    private void refreshPerceptInMemory(Percept newPercept, Percept currentPercept, MemoryObject mo){
        
        Map<String,Map<Percept, Double>> memoryMap = (Map<String,Map<Percept, Double>>) mo.getI();
        Map<Percept, Double> memoryPerceptsOfCategory = memoryMap.get(currentPercept.getCategory());
        
        if(memoryPerceptsOfCategory != null){
            
            for (Property prop : newPercept.getProperties()) {
                currentPercept.setPropertyValue(prop.getType(), prop.getValue());
            }

            this.refreshRelations(newPercept, currentPercept);

            double perceptActivation = this.getPerceptActivation(currentPercept, memoryPerceptsOfCategory);

            synchronized(mo){
                memoryPerceptsOfCategory.replace(currentPercept, perceptActivation);
            }

        } else{
            //Exception
        }
    }
    
    private double getPerceptActivation(Percept p, Map<Percept, Double> memoryPerceptsOfCategory){
        return memoryPerceptsOfCategory.get(p);
    }
    
    private void decrementPerceptsActivations(MemoryObject mo){
        synchronized(mo){
            
            Map<String,Map<Percept, Double>> memoryMap = (Map<String,Map<Percept, Double>>) mo.getI();
            for (Map<Percept, Double> memoryPerceptsOfCategory : new HashMap<>(memoryMap).values()) {

                for (Map.Entry<Percept, Double> entry : AuxiliarMethods.deepCopyMap(memoryPerceptsOfCategory).entrySet()) {

                    Percept p = entry.getKey();
                    double activation = entry.getValue();

                    activation -= this.decrementPerCycle;

                    if (activation < this.minActivation) {
                        activation = this.minActivation;
                    }

                    synchronized(mo){
                        memoryPerceptsOfCategory.put(p, activation);
                    }

                    if (activation <= this.deleteThreshold) {
                        this.removePerceptFromMemory(p, mo);
                    } else{
                        if (activation <= this.replaceThreshold) {
                            this.toReplacePercepts.add(p);
                        }
                    }
                }

            }
        }
    }
    
    
    //////////////////////
    // AUXILIARY METHODS //
    //////////////////////
    
    private long getOperationID(){
        long id;
        do{
            id = this.rn.nextLong();
        } while(this.reasonerOperationsIds.contains(id));
        
        this.reasonerOperationsIds.add(id);
        return id;
    }
    
    private void removeOperationID(long id){
        this.reasonerOperationsIds.remove(id);
    }
    
    
    private void putOperationsPerceptsInReasonerMO(){
        for (Operation op : this.reasonerOperations) {
            
            if (op.getStatus() == this.DONE) { //if operation already had done.
                Codelet cdt = op.getCdt();
                ExecutorInfo operationInfos = this.operationsMap.get(op.getAff().getAffordanceType().getAffordanceName());

                List<Percept> operationPercepts = new CopyOnWriteArrayList((List<Percept>)cdt.getOutput(operationInfos.getClassName()+"_PERCEPTS").getI());

                for (Percept p: operationPercepts) {
                    Percept pInReasoner = AuxiliarMethods.getPerceptFromMemoryMap(p.getCategory(), p, this.reasonerPercepts);
                    if (pInReasoner != null) { //if already in perception
                        this.refreshPerceptInMemory(p,pInReasoner, this.reasonerMO);
                        this.incrementPerceptInMemory(p, this.reasonerMO, this.incrementPerCycle, this.maxActivation);
                    } else{ //if not in perception

                        if (countTotalOfPerceptsInMO()< this.reasonerMOCapacity) { //check perceptionMO capacity
                            this.insertPerceptInMemory(p, this.reasonerMO);
                        } else{ //full memory 
                            Percept replacedPercept = this.selectPerceptToReplace();
                            if (replacedPercept != null) { //has percepts to replace
                                this.removePerceptFromMemory(replacedPercept, this.reasonerMO);
                                this.insertPerceptInMemory(p, this.reasonerMO);
                            }
                        }
                    }
                }
                
                op.setStatus(this.COMMITED);
            }
            
        }
        
    }

    private Percept selectPerceptToReplace(){
        if (this.toReplacePercepts.isEmpty()) {
            return null;
        } else{
            return this.toReplacePercepts.get(this.rn.nextInt(this.toReplacePercepts.size()));
        }
    }
    
    
    private void createReasonerOperation(){
        Operation newOperation = null;
        
        synchronized(this.activatedAffordanceMO){
            if (this.activatedAffordance != null && this.operationsMap.containsKey(this.activatedAffordance.getAffordanceType().getAffordanceName())) { //if there is activated affordance and if this activated affordance trigger a reasoner operation
                //if (!this.hasOperationToActivatedAffordance()) {
                //if (!this.executedOperations.containsKey(this.activatedAffordance)) {
                          
                    ExecutorInfo operationInfos = this.operationsMap.get(this.activatedAffordance.getAffordanceType().getAffordanceName()).getClone();
                    operationInfos.addInputMemoryObject(this.operationsMO);
                    operationInfos.addInputMemoryObject(this.synchronizerMO);
                    operationInfos.addOutputMemoryObject(this.m.createMemoryObject(operationInfos.getClassName()+"_PERCEPTS"));
                    
                    //inputMOs.add(this.extractedAffordancesMO);
                    
                    operationInfos.addParameter(Map.class, this.activatedAffordance.getPerceptsPermutation());

                    Codelet cdt = super.createCdt(this.getOperationID(), operationInfos.getClassName(), operationInfos.getParametersClasses(), operationInfos.getParametersObjects(), operationInfos.getMemoryObjectsInput(), operationInfos.getMemoryObjectsOutput());
                    newOperation = new Operation(this.activatedAffordance.getDrives(),(ExtractedAffordance)this.activatedAffordance.getClone(), cdt, this.STARTED);
                    this.reasonerOperations.add( newOperation );
                    this.operationsMO.setI(this.reasonerOperations);
                //}
                //else{
                    //this.activatedAffordance = null;
                //}
            }
        }
        
        if (newOperation != null) { //if new operation was created.
            super.startCodelet(newOperation.getCdt(), synchronizerMO);
        }
        
        
    }
     
    private void destroyReasonerOperations(){
        
        //synchronized(this.operationsMO){
            List<Operation> ops = new ArrayList<>();
            ops.addAll(this.reasonerOperations);

            for (Operation op: ops){
                List<Drive> factors = op.getDrives();
                int status = op.getStatus();

                if ( (!this.drives.containsAll(factors)) || (status == this.COMMITED) ) { //if factor were deleted or if codelet has terminated and producted percepts had read.

                    //if (super.deleteCdt(op.getCdt(), this.synchronizerMO)) { //if success in delete
                        //synchronized(this.operationsMO){
                            this.removeOperationMO(op.getCdt());
                            super.deleteCdt(op.getCdt(),this.synchronizerMO);
                            if (op.getAff().equals(this.activatedAffordance)) {
                                this.activatedAffordance = null;
                                this.activatedAffordanceMO.setI(this.activatedAffordance);
                            }

                            this.insertExecutedOperation(op.getAff());

                            this.reasonerOperations.remove(op);
                            this.operationsMO.setI(this.reasonerOperations);

                            //removeReasonerPercepts(op);
                        //}
                    //}
                    
                }
            }
        //}
        
    }
    
    private void removeOperationMO(Codelet operationCodelet){
        this.m.getRawMemory().destroyMemoryObject(operationCodelet.getOutput(operationCodelet.getName()+"_PERCEPTS"));
    }
    
    private ExtractedAffordance getOldExecutedOperation(){
        ExtractedAffordance old = null;
        Date oldDate = null;
        for (Map.Entry<ExtractedAffordance, Date> entry : this.executedOperations.entrySet()) {
            if (old == null){
                old = entry.getKey();
                oldDate = entry.getValue();
            } else if (oldDate.after(entry.getValue())) {
                old = entry.getKey();
                oldDate = entry.getValue();
            }
        }
        return old;
    }
    
    private void insertExecutedOperation(ExtractedAffordance extAff){
        if (this.executedOperations.size() < this.reasonerMOCapacity) {
            this.executedOperations.put(extAff, new Date());
        } else{
            ExtractedAffordance oldOperation = this.getOldExecutedOperation();
            if (oldOperation!=null) {
                this.executedOperations.remove(oldOperation);
                this.executedOperations.put(extAff, new Date());
            }
        }
        
    }
    
    private void addReasonerPerceptsToWorkingMO(){
        synchronized(this.workingMO){
            Map<String,Map<String,List<Percept>>> workingMemory = (Map<String,Map<String,List<Percept>>>) this.workingMO.getI();
            Map<String,List<Percept>> reasonerPerceptsInWMO = workingMemory.get(this.reasonerCategoryInWMO);

            if (reasonerPerceptsInWMO == null) {
                reasonerPerceptsInWMO = new HashMap<>();
                workingMemory.put(this.reasonerCategoryInWMO, reasonerPerceptsInWMO);
            } else{
                reasonerPerceptsInWMO.clear(); // ou voce apaga toda esta memoria ou e necessario uma function para apagar os percepts deletados.
            }

            for (Map.Entry<String, Map<Percept,Double>> entry : this.reasonerPercepts.entrySet()) {
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
        this.driveMO = (MemoryObject) this.getInput(MemoriesNames.DRIVE_MO);
        this.operationsMO = (MemoryObject) this.getInput(MemoriesNames.OPERATIONS_MO);
        this.reasonerMO = (MemoryObject) this.getInput(MemoriesNames.REASONER_MO);
        this.workingMO = (MemoryObject) this.getInput(MemoriesNames.WORKING_MO);
        this.activatedAffordanceMO = (MemoryObject) this.getInput(MemoriesNames.ACTIVATED_AFFORDANCE_MO);
        this.synchronizerMO = (MemoryObject) this.getInput(MemoriesNames.SYNCHRONIZER_MO);
    }

    @Override
    public void calculateActivation() {
    }

    @Override
    public void proc() {
        
        this.drives = new CopyOnWriteArrayList( (List<Drive>) this.driveMO.getI() );
        this.activatedAffordance = (ExtractedAffordance) this.activatedAffordanceMO.getI();
        this.reasonerOperations = (List<Operation>) this.operationsMO.getI();
        this.synchronizers = (ConcurrentHashMap<String, Boolean>) this.synchronizerMO.getI();
        this.reasonerPercepts = (Map<String,Map<Percept,Double>>) this.reasonerMO.getI();
        
        this.toReplacePercepts = new ArrayList<>();
        
        this.putOperationsPerceptsInReasonerMO(); //this operation must be executed before destroy Reasoner, because in this manner it is sure that the new reasoner operation have executed before will deleted.
        
        this.decrementPerceptsActivations(this.reasonerMO);//decrement and create the todeleteList and toReplaceList
        
        this.destroyReasonerOperations();
        this.createReasonerOperation();
    
        //System.out.println("ReasonerMO: " + this.reasonerPercepts.size() + " Total: "  + this.reasonerMOCapacity);
        
        addReasonerPerceptsToWorkingMO();
        
        AuxiliarMethods.synchronize(super.getName());
    }
    
}
