/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package memory;

import actionSelection.SynchronizationMethods;
import main.MemoriesNames;
import actionSelection.Statistic;
import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.AuxiliarMethods;
import perception.Percept;
import br.unicamp.cst.representation.owrl.Property;
import br.unicamp.cst.representation.owrl.QualityDimension;
import perception.Relation;


/**
 *
 * @author rgpolizeli
 */
public class MemoryCodelet extends Codelet{
    
    private MemoryObject perceptionMO;
    private MemoryObject shortMO;
    private MemoryObject longMO;
    private MemoryObject reasonerMO;
    private MemoryObject createdPerceptsMO;
    private MemoryObject toModifyPerceptionMO;
    private MemoryObject synchronizerMO;
    
    private Map<String, Map<Percept, Double>> shortPercepts;
    private Map<String, Map<Percept, Double>> longPercepts;
    private Map<String, Map<Percept, Double>> reasonerPercepts;
    
    private double maxActivation = 1.0;
    private double minActivation = 0.0;
    
    private int shortMemoryCapacity = 20;
    private double shortMemoryDeleteThreshold = this.minActivation;
    private double shortMemoryReplaceThreshold = (this.maxActivation/5); //the objective is to remove percepts that in proximity to deleteThreshold
    private double shortMemoryIncrementPerCycle = 0.1;
    private double shortMemoryDecrementPerCycle = 0.1;
    
    private int longMemoryCapacity = 80;
    private double memorizerThreshold = 0.8; //when percept pass this limiar, increment your activation in longMO
    private double longMemoryDeleteThreshold = this.minActivation;
    private double longMemoryReplaceThreshold = (this.maxActivation/5); //the objective is to remove percepts that in proximity to deleteThreshold
    private double longMemoryIncrementPerCycle = 0.1;
    private double longMemoryDecrementPerCycle = 0.1;
    
    private int reasonerMemoryCapacity = 20;
    private double reasonerMemoryDeleteThreshold = this.minActivation;
    private double reasonerMemoryReplaceThreshold = (this.maxActivation/5); //the objective is to remove percepts that in proximity to deleteThreshold
    private double reasonerMemoryIncrementPerCycle = 0.1;
    private double reasonerMemoryDecrementPerCycle = 0.1;
    
    private final Random rn;
    private final Logger LOGGER = Logger.getLogger(MemoryCodelet.class.getName());
    
    public MemoryCodelet(double maxActivation, double minActivation, 
            int shortMOCapacity, double shortMemoryDeleteThreshold, double shortMemoryReplaceThreshold, 
            double shortMemoryIncrementPerCycle, double shortMemoryDecrementPerCycle,
            int longMemoryCapacity, double memorizerThreshold, double longMemoryDeleteThreshold, double longMemoryReplaceThreshold, 
            double longMemoryIncrementPerCycle, double longMemoryDecrementPerCycle,
            int reasonerMemoryCapacity, double reasonerMemoryDeleteThreshold, double reasonerMemoryReplaceThreshold, 
            double reasonerMemoryIncrementPerCycle, double reasonerMemoryDecrementPerCycle
    ) {
        
        this.maxActivation = maxActivation;
        this.minActivation = minActivation;
        
        this.shortMemoryCapacity = shortMOCapacity;
        this.shortMemoryDeleteThreshold = shortMemoryDeleteThreshold;
        this.shortMemoryReplaceThreshold = shortMemoryReplaceThreshold;
        this.shortMemoryIncrementPerCycle = shortMemoryIncrementPerCycle;
        this.shortMemoryDecrementPerCycle = shortMemoryDecrementPerCycle;
        
        this.longMemoryCapacity = longMemoryCapacity;
        this.memorizerThreshold = memorizerThreshold;
        this.longMemoryDeleteThreshold = longMemoryDeleteThreshold;
        this.longMemoryReplaceThreshold = longMemoryReplaceThreshold;
        this.longMemoryIncrementPerCycle = longMemoryIncrementPerCycle;
        this.longMemoryDecrementPerCycle = longMemoryDecrementPerCycle;
        
        this.reasonerMemoryCapacity = reasonerMemoryCapacity;
        this.reasonerMemoryDeleteThreshold = reasonerMemoryDeleteThreshold;
        this.reasonerMemoryReplaceThreshold = reasonerMemoryReplaceThreshold;
        this.reasonerMemoryIncrementPerCycle = reasonerMemoryIncrementPerCycle;
        this.reasonerMemoryDecrementPerCycle = reasonerMemoryDecrementPerCycle;
        
        this.rn = new Random();
    }
    
    //////////////////////
    // AUXILIARY METHODS //
    //////////////////////
    
    /**
     * Count the total number of percepts in a memory.
     * @param perceptsInMemory
     * @return 
     */
    private int countTotalOfPerceptsInMemory(Map<String, Map<Percept, Double>> perceptsInMemory){
        
        int total = 0;
        
        if (perceptsInMemory != null) {
            for(Map<Percept,Double> map : perceptsInMemory.values()){
                total+= map.keySet().size();
            }
        }
        
        return total;
    }
    
    
    /**
     * Insert percept p in memory object mo with initial activation of incrementPerCycle.
     * @param p
     * @param mo
     * @param incrementPerCycle 
     */
    private void insertPerceptInMemory(Percept p, MemoryObject mo, double incrementPerCycle){
        
        synchronized(mo){
            
            Map<String, Map<Percept, Double>> memoryPercepts = (Map<String, Map<Percept, Double>>) mo.getI();
            Map<Percept, Double> memoryPerceptsOfCategory;
            
            if(memoryPercepts.containsKey(p.getCategory())){
                memoryPerceptsOfCategory = memoryPercepts.get(p.getCategory());
                
            } else{
                memoryPerceptsOfCategory = new HashMap<>();
                memoryPercepts.put(p.getCategory(), memoryPerceptsOfCategory);
            }
          
            memoryPerceptsOfCategory.put(p, incrementPerCycle);
            LOGGER.log(Level.INFO, "Inserted percept {0} in {1}", new Object[]{p, mo.getName()});
        }
    }
    
    /**
     * Remove percept p from memory object mo.
     * @param p
     * @param mo 
     */
    private void removePerceptFromMemory(Percept p, MemoryObject mo){
        
        synchronized(mo){
            
            Map<String, Map<Percept, Double>> memoryPercepts = (Map<String, Map<Percept, Double>>) mo.getI();
            Map<Percept, Double> memoryPerceptsOfCategory;
            
            if(memoryPercepts.containsKey(p.getCategory())){
                memoryPerceptsOfCategory = memoryPercepts.get(p.getCategory());
                if (memoryPerceptsOfCategory.containsKey(p)) {
                    memoryPerceptsOfCategory.remove(p);
                    LOGGER.log(Level.INFO, "Removed percept {0} in {1}", new Object[]{p, mo.getName()});
                    if (memoryPerceptsOfCategory.isEmpty()) {
                        memoryPercepts.remove(p.getCategory());
                    }
                }
            } 
        }
 
    }
    
    /**
     * Get a relation with the type relationType in the list of relations relations. 
     * @param relationType
     * @param relations
     * @return Relation if there is relation with the type, null otherwise.
     */
    private Relation getRelationByTypeInList(String relationType, List<Relation> relations){
        for(Relation r : relations){
            if(r.getType().equals(relationType)){
                return r;
            }
        }
        return null;
    }
    
    
    /**
     * Get relations that must will be deleted, i.e., the relations contained in currentRelations whose type is not contained in newRelations.
     * @param currentRelations
     * @param newRelations
     * @return List<Relation> a list with the to delete relations.
     */
    private List<Relation> getDeletedRelations(List<Relation> currentRelations, List<Relation> newRelations){
        
        List<Relation> deletedRelations = new ArrayList<>();
        
        for (Relation currentRelation : currentRelations) {
            String currentRelationType = currentRelation.getType();
            Relation r = this.getRelationByTypeInList(currentRelationType, newRelations);
            if(r == null){
                deletedRelations.add(currentRelation);
            }
        }
        
        return deletedRelations;
    }
    
    /**
     * Update relations of percept currentPercept with the updated version of the same percept.
     * @param newPercept
     * @param currentPercept 
     */
    private void refreshRelations(Percept newPercept, Percept currentPercept){
        List<Relation> newRelations = newPercept.getRelations();
        List<Relation> currentRelations = currentPercept.getRelations();
        
        List<Relation> deletedRelations = this.getDeletedRelations(currentRelations,newRelations);
        
        for (Relation deletedRelation : deletedRelations) {
            
            if (currentPercept.removeRelation(deletedRelation) != 1) {
                LOGGER.log(Level.SEVERE, "AusentRelationDeletionException");
            }

        }
        
        for (Relation newRelation : newRelations) {
            currentPercept.replaceRelation(newRelation.getType(), newRelation);
        }
    }
    
    /**
     * Increment the activation value of the percept in memory object mo with the value incrementPerCycle.
     * @param percept
     * @param mo
     * @param incrementPerCycle
     * @param maxActivation 
     */
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
    
    /**
     * Refresh the properties and relations of the percept with the new version of the same percept.It is assumed that properties and their quality dimensions are not removed, only their values changed, and that there are no quality dimensions with the same name within the same property.
     * @param newPercept
     * @param currentPercept
     * @param mo 
     */
    private void refreshPerceptInMemory(Percept newPercept, Percept currentPercept, MemoryObject mo){
        
        Map<String,Map<Percept, Double>> memoryMap = (Map<String,Map<Percept, Double>>) mo.getI();
        Map<Percept, Double> memoryPerceptsOfCategory = memoryMap.get(currentPercept.getCategory());
        
        if(memoryPerceptsOfCategory != null){
            for (Property newProperty : newPercept.getProperties()) {
                Property currentProperty = currentPercept.getPropertyByType(newProperty.getName());
                for(QualityDimension newQd : newProperty.getQualityDimensions()){
                    QualityDimension currentQd = (QualityDimension)currentProperty.search(newQd.getName()).get(0);
                    currentQd.setValue(newQd.getValue());
                }
            }

            this.refreshRelations(newPercept, currentPercept);

            double perceptActivation = this.getPerceptActivation(currentPercept, memoryPerceptsOfCategory);

            synchronized(mo){
                memoryPerceptsOfCategory.replace(currentPercept, perceptActivation);
            }
        } else{
            LOGGER.log(Level.SEVERE, "AusentPerceptCategoryInMemoryException");
        }
    }
    
    private double getPerceptActivation(Percept p, Map<Percept, Double> memoryPerceptsOfCategory){
        return memoryPerceptsOfCategory.get(p);
    }
    
    /**
     * Decrement percepts activations, initiates the removal of percepts and creates the toReplacePercepts list.
     * @param mo 
     */
    private void decrementPerceptsActivations(MemoryObject mo, double decrementPerCyle, double deleteThreshold, double replaceThreshold, List<Percept> toReplacePercepts){
        synchronized(mo){
            Map<String,Map<Percept, Double>> memoryMap = (Map<String,Map<Percept, Double>>) mo.getI();

            for (Map<Percept, Double> memoryPerceptsOfCategory : new HashMap<>(memoryMap).values()) {

                for (Map.Entry<Percept, Double> entry : AuxiliarMethods.deepCopyMap(memoryPerceptsOfCategory).entrySet()) {

                    Percept p = entry.getKey();
                    double activation = entry.getValue();

                    activation -= decrementPerCyle;

                    if (activation < this.minActivation) {
                        activation = this.minActivation;
                    }

                    synchronized(mo){
                        memoryPerceptsOfCategory.put(p, activation);
                    }

                    if (activation <= deleteThreshold) {
                        this.removePerceptFromMemory(p, mo);
                    } else{
                        if (activation <= replaceThreshold) {
                            toReplacePercepts.add(p);
                        }
                    }
                }

            }
        }
    }
    
    /**
     * Selects a random percept of the toReplacePercepts to be replaced.
     * @param toReplacePercepts
     * @return a percept
     */
    private Percept selectPerceptToReplace(List<Percept> toReplacePercepts){
        if (toReplacePercepts.isEmpty()) {
            return null;
        } else{
            return toReplacePercepts.get(this.rn.nextInt(toReplacePercepts.size()));
        }
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
    
    /**
     * Try to put a percept in long memory.
     * @param percept
     * @param perceptActivation
     * @param toReplacePercepts 
     */
    private void memorizePercept(Percept percept, double perceptActivation, List<Percept> toReplacePercepts){
        
        Percept pInMemory = AuxiliarMethods.getPerceptFromMemoryMap(percept.getCategory(), percept, this.longPercepts);
        
        if (pInMemory == null) {
            if (perceptActivation >= this.memorizerThreshold) {
                if (countTotalOfPerceptsInMemory(this.longPercepts) < this.longMemoryCapacity) {
                    this.insertPerceptInMemory(percept, this.longMO, this.longMemoryIncrementPerCycle);
                } else{ //full memory 
                    Percept replacedPercept = this.selectPerceptToReplace(toReplacePercepts);
                    if (replacedPercept != null) { //has percepts to replace
                        this.removePerceptFromMemory(replacedPercept, this.longMO);
                        this.insertPerceptInMemory(percept, this.longMO, this.longMemoryIncrementPerCycle);
                    }
                }
            }
        } else{
            this.refreshPerceptInMemory(percept, pInMemory, this.longMO);
            this.incrementPerceptInMemory(percept, this.longMO, this.longMemoryIncrementPerCycle, this.maxActivation);
        }
    }
    
    private void processShortMO(){
        
        this.shortPercepts = (Map<String,Map<Percept,Double>>) this.shortMO.getI();
        List<Percept> toModifyPercepts = (List<Percept>) this.toModifyPerceptionMO.getI();
        List<Percept> toReplacePercepts = new ArrayList<>();
        List<Percept> modifiedPercepts = new ArrayList<>();
        
        this.decrementPerceptsActivations(this.shortMO, this.shortMemoryDecrementPerCycle, this.shortMemoryDeleteThreshold, this.shortMemoryReplaceThreshold, toReplacePercepts);
        
        Map<String,Map<Percept,Double>> longPerceptsCopy = getCopyOfMemoryContent(this.longMO);
        
        synchronized(this.perceptionMO){ 
            Map<String, List<Percept>> perceptsMap = (Map<String, List<Percept>>) this.perceptionMO.getI();

            for (Map.Entry<String,List<Percept>> entry : perceptsMap.entrySet()) {

                String category = entry.getKey();
                List<Percept> percepts = entry.getValue();
                
                for (Percept p : percepts) {
                    
                    Percept pInMemory = AuxiliarMethods.getPerceptFromMemoryMap(category,p, this.shortPercepts);
                    if (pInMemory != null) { //if already in short memory
                        if (toModifyPercepts.contains(pInMemory)) {
                            modifiedPercepts.add(pInMemory);
                        } else{
                            this.refreshPerceptInMemory(p,pInMemory,this.shortMO);
                            this.incrementPerceptInMemory(pInMemory, this.shortMO, this.shortMemoryIncrementPerCycle, this.maxActivation);
                        }
                    } else{ //if not in short memory
                        
                        pInMemory = AuxiliarMethods.getPerceptFromMemoryMap(category,p,longPerceptsCopy);
                        
                        if (pInMemory != null) { //if in long memory
                            p = pInMemory; //the instance in long memory will be added in shortMO and not the new instance of same percept.
                        } 
                            
                        if (countTotalOfPerceptsInMemory(this.shortPercepts) < this.shortMemoryCapacity) { //check shortMO capacity
                            this.insertPerceptInMemory(p, this.shortMO, this.shortMemoryIncrementPerCycle);
                        } else{ //full memory 
                            Percept replacedPercept = this.selectPerceptToReplace(toReplacePercepts);
                            if (replacedPercept != null) { //has percepts to replace
                                this.removePerceptFromMemory(replacedPercept, this.shortMO);
                                this.insertPerceptInMemory(p,this.shortMO,this.shortMemoryIncrementPerCycle);
                            }
                        }
                    }
                    
                }
            }
        }
        
        //COLLECT METHODS
        Statistic.updateShortMO(countTotalOfPerceptsInMemory(this.shortPercepts));
        //
        
        synchronized(this.toModifyPerceptionMO){
            toModifyPercepts.retainAll(modifiedPercepts);
        }
    }
    
    private void processLongMO(){
        this.longPercepts = (Map<String, Map<Percept, Double>>) this.longMO.getI();
        List<Percept> toReplacePercepts = new ArrayList<>();
        
        this.decrementPerceptsActivations(this.longMO, this.longMemoryDecrementPerCycle, this.longMemoryDeleteThreshold, this.longMemoryReplaceThreshold, toReplacePercepts);
        
        Map<String,Map<Percept,Double>> shortPerceptsCopy = getCopyOfMemoryContent(this.shortMO);
        Map<String,Map<Percept,Double>> reasonerPerceptsCopy = getCopyOfMemoryContent(this.reasonerMO);
        
        for (Map<Percept, Double> perceptsOfCategory : shortPerceptsCopy.values()) {
            for (Map.Entry<Percept, Double> entry : perceptsOfCategory.entrySet()) {
                this.memorizePercept(entry.getKey(), entry.getValue(), toReplacePercepts);
            }
        }
        
        for (Map<Percept, Double> perceptsOfCategory : reasonerPerceptsCopy.values()) {
            for (Map.Entry<Percept, Double> entry : perceptsOfCategory.entrySet()) {
                this.memorizePercept(entry.getKey(), entry.getValue(), toReplacePercepts);
            }
        }
        
        //COLLECT METHODS
        Statistic.updateLongMO(countTotalOfPerceptsInMemory(this.longPercepts));
        //
    }
    
    private void processReasonerMO(){
        this.reasonerPercepts = (Map<String, Map<Percept, Double>>) this.reasonerMO.getI();
        List<Percept> toReplacePercepts = new ArrayList<>();
        
        this.decrementPerceptsActivations(this.reasonerMO, this.reasonerMemoryDecrementPerCycle, this.reasonerMemoryDeleteThreshold, this.reasonerMemoryReplaceThreshold, toReplacePercepts);
        
        Map<String,Map<Percept,Double>> longPerceptsCopy = getCopyOfMemoryContent(this.longMO);
        
        synchronized(this.createdPerceptsMO){
            Map<String, List<Percept>> createdPerceptsMap = (Map<String, List<Percept>>) this.createdPerceptsMO.getI();

            for (Map.Entry<String,List<Percept>> entry : createdPerceptsMap.entrySet()) {

                String category = entry.getKey();
                List<Percept> percepts = entry.getValue();
                
                for (Percept p : percepts) {
                    
                    Percept pInMemory = AuxiliarMethods.getPerceptFromMemoryMap(category,p, this.reasonerPercepts);
                    if (pInMemory != null) { //if already in reasoner memory
                        this.refreshPerceptInMemory(p,pInMemory,this.reasonerMO);
                        this.incrementPerceptInMemory(pInMemory, this.reasonerMO, this.reasonerMemoryIncrementPerCycle, this.maxActivation);
                    } else{ //if not in reasoner memory
                        
                        pInMemory = AuxiliarMethods.getPerceptFromMemoryMap(category,p,longPerceptsCopy);
                        
                        if (pInMemory != null) { //if in long memory
                            p = pInMemory; //the instance in long memory will be added in reasonerMO and not the new instance of same percept.
                        } 
                            
                        if (countTotalOfPerceptsInMemory(this.reasonerPercepts) < this.reasonerMemoryCapacity) { //check reasonerMO capacity
                            this.insertPerceptInMemory(p, this.reasonerMO, this.reasonerMemoryIncrementPerCycle);
                        } else{ //full memory 
                            Percept replacedPercept = this.selectPerceptToReplace(toReplacePercepts);
                            if (replacedPercept != null) { //has percepts to replace
                                this.removePerceptFromMemory(replacedPercept, this.reasonerMO);
                                this.insertPerceptInMemory(p,this.reasonerMO,this.reasonerMemoryIncrementPerCycle);
                            }
                        }
                    }   
                }
            }
            createdPerceptsMap.clear(); //delete the percepts already inserted in createdPerceptsMO
        }
    }
    

    //////////////////////
    // OVERRIDE METHODS //
    //////////////////////
    
    @Override
    public void accessMemoryObjects() {
        this.perceptionMO = (MemoryObject) this.getInput(MemoriesNames.PERCEPTION_MO);
        this.shortMO = (MemoryObject) this.getInput(MemoriesNames.SHORT_MO);
        this.longMO = (MemoryObject) this.getInput(MemoriesNames.LONG_MO);
        this.reasonerMO = (MemoryObject) this.getInput(MemoriesNames.REASONER_MO);
        this.createdPerceptsMO = (MemoryObject) this.getInput(MemoriesNames.CREATED_PERCEPTS_MO);
        this.toModifyPerceptionMO = (MemoryObject) this.getInput(MemoriesNames.TO_MODIFY_PERCEPTION_MO);
        this.synchronizerMO = (MemoryObject) this.getInput(MemoriesNames.SYNCHRONIZER_MO);
    }

    @Override
    public void calculateActivation() {
    }

    @Override
    public void proc() {
        
        processShortMO();
        processLongMO();
        processReasonerMO();
        
        SynchronizationMethods.synchronize(super.getName(),synchronizerMO);
    }
    
}
