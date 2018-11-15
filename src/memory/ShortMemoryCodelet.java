/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package memory;

import actionSelection.AuxiliarMethods;
import actionSelection.MemoryObjectsNames;
import actionSelection.Statistic;
import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import perception.Percept;
import perception.Property;
import perception.Relation;


/**
 *
 * @author ricardo
 */
public class ShortMemoryCodelet extends Codelet{
    
    private MemoryObject perceptionMO;
    private MemoryObject shortMO;
    private MemoryObject longMO;
    private MemoryObject toModifyPerceptionMO;
    private MemoryObject synchronizerMO;
    
    private Map<String, Map<Percept, Double>> shortPercepts;
    private Map<String, Map<Percept, Double>> longPercepts;
    private List<Percept> toModifyPercepts;
    private List<Percept> toReplacePercepts;
    
    int perceptionCapacity = 20;
    private double maxActivation = 1.0;
    private double minActivation = 0.0;
    private double deleteThreshold = this.minActivation;
    private double replaceThreshold = (this.maxActivation/5); //the objective is to remove percepts that in proximity to deleteThreshold
    private double incrementPerCycle = 0.1;
    private double decrementPerCycle = 0.1;
    
    private Random rn;
    
    public ShortMemoryCodelet(int perceptionCapacity, double maxActivation, double minActivation, double deleteThreshold, double replaceThreshold, double incrementPerCycle, double decrementPerCycle) {
        this.perceptionCapacity = perceptionCapacity;
        this.maxActivation = maxActivation;
        this.minActivation = minActivation;
        this.deleteThreshold = deleteThreshold;
        this.replaceThreshold = replaceThreshold;
        this.incrementPerCycle = incrementPerCycle;
        this.decrementPerCycle = decrementPerCycle;
        
        this.rn = new Random();
    }
    
    //////////////////////
    // AUXILIARY METHODS //
    //////////////////////
    
    private int countTotalOfPerceptsInMO(){
        
        int total = 0;
        
        if (this.shortPercepts != null) {
            for(Map<Percept,Double> map : this.shortPercepts.values()){
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
    
    
    private Percept selectPerceptToReplace(){
        if (this.toReplacePercepts.isEmpty()) {
            return null;
        } else{
            return this.toReplacePercepts.get(this.rn.nextInt(this.toReplacePercepts.size()));
        }
    }

    //////////////////////
    // OVERRIDE METHODS //
    //////////////////////
    
    @Override
    public void accessMemoryObjects() {
        
        this.perceptionMO = (MemoryObject) this.getInput(MemoryObjectsNames.PERCEPTION_MO);
        this.shortMO = (MemoryObject) this.getInput(MemoryObjectsNames.SHORT_MO);
        this.longMO = (MemoryObject) this.getInput(MemoryObjectsNames.LONG_MO);
        this.toModifyPerceptionMO = (MemoryObject) this.getInput(MemoryObjectsNames.TO_MODIFY_PERCEPTION_MO);
        
        this.synchronizerMO = (MemoryObject) this.getInput(MemoryObjectsNames.SYNCHRONIZER_MO);
    }

    @Override
    public void calculateActivation() {
        
    }

    @Override
    public void proc() {
        
        this.shortPercepts = (Map<String,Map<Percept,Double>>) this.shortMO.getI();
        
        synchronized(this.longMO){
            this.longPercepts =  (Map<String,Map<Percept,Double>>) this.longMO.getI();
            this.longPercepts = AuxiliarMethods.deepCopyMemoryMap(this.longPercepts);
        }
        
        this.toModifyPercepts = (List<Percept>) this.toModifyPerceptionMO.getI();
        this.toReplacePercepts = new ArrayList<>();
        
        this.decrementPerceptsActivations(this.shortMO);//decrement and create the todeleteList and toReplaceList
        
        List<Percept> modifiedPercepts = new ArrayList<>();
        
        synchronized(this.perceptionMO){ //This requires that every perception has a synchronized
            Map<String, List<Percept>> perceptsMap = (Map<String, List<Percept>>) this.perceptionMO.getI();

            for (Map.Entry<String,List<Percept>> entry : perceptsMap.entrySet()) {

                String category = entry.getKey();
                List<Percept> percepts = entry.getValue();
                
                for (Percept p : percepts) {
                    
                    Percept pInMemory = AuxiliarMethods.getPerceptFromMemoryMap(category,p, this.shortPercepts);
                    if (pInMemory != null) { //if already in short memory
                        if (this.toModifyPercepts.contains(pInMemory)) {
                            modifiedPercepts.add(pInMemory);
                        } else{
                            this.refreshPerceptInMemory(p,pInMemory,this.shortMO);
                            this.incrementPerceptInMemory(pInMemory, this.shortMO, this.incrementPerCycle, this.maxActivation);
                        }
                    } else{ //if not in short memory
                        
                        pInMemory = AuxiliarMethods.getPerceptFromMemoryMap(category,p, this.longPercepts);
                        
                        if (pInMemory != null) { //if in long memory
                            p = pInMemory; //the instance in long memory will be added in shortMO and not the new instance of same percept.
                        } 
                            
                        if (countTotalOfPerceptsInMO() < this.perceptionCapacity) { //check shortMO capacity
                            this.insertPerceptInMemory(p, this.shortMO);
                        } else{ //full memory 
                            Percept replacedPercept = this.selectPerceptToReplace();
                            if (replacedPercept != null) { //has percepts to replace
                                this.removePerceptFromMemory(replacedPercept, this.shortMO);
                                this.insertPerceptInMemory(p,this.shortMO);
                            }
                        }
                    }
                    
                }
            }
        }
        
        //COLLECT METHODS
        Statistic.updateShortMO(countTotalOfPerceptsInMO());
        //
        
        synchronized(this.toModifyPerceptionMO){
            this.toModifyPercepts.retainAll(modifiedPercepts);
        }
        
        AuxiliarMethods.synchronize(super.getName());
    }
    
}
