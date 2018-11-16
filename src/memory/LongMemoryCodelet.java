/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package memory;

import actionSelection.AuxiliarMethods;
import main.MemoriesNames;
import actionSelection.Statistic;
import perception.Percept;
import perception.Relation;
import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import perception.Property;


/**
 *
 * @author ricardo
 */
public class LongMemoryCodelet extends Codelet{
    
    private MemoryObject longMO;
    private MemoryObject shortMO;
    private MemoryObject reasonerMO;
    private MemoryObject toDeleteLongMO;
    private MemoryObject synchronizerMO;
    
    private Map<String, Map<Percept, Double>> longPercepts;
    private Map<String, Map<Percept, Double>> shortPercepts;
    private Map<String, Map<Percept, Double>> reasonerPercepts;
    private List<Percept> toDeleteLongMemory;
    private List<Percept> toDeletePercepts;
    private List<Percept> toReplacePercepts;
    
    private int longMemoryCapacity = 80;
    private double memorizerThreshold = 0.8; //when percept pass this limiar, increment your activation in longMO
    private double maxActivation = 1.0;
    private double minActivation = 0.0;
    private double deleteThreshold = this.minActivation;
    private double replaceThreshold = (this.maxActivation/5); //the objective is to remove percepts that in proximity to deleteThreshold
    private double incrementPerCycle = 0.1;
    private double decrementPerCycle = 0.1;
    
    private Random rn;
    
    public LongMemoryCodelet(int longMemoryCapacity, double incrementPerCycle, double decrementPerCycle, double maxActivation, double minActivation, double memorizerThreshold, double deleteThreshold, double replaceThreshold) {
        this.longMemoryCapacity = longMemoryCapacity;
        this.incrementPerCycle = incrementPerCycle;
        this.decrementPerCycle = decrementPerCycle;
        this.maxActivation = maxActivation;
        this.minActivation = minActivation;
        this.memorizerThreshold = memorizerThreshold;
        this.deleteThreshold = deleteThreshold;
        this.replaceThreshold = replaceThreshold;
        
        this.rn = new Random();
    }
    
    
    
    //////////////////////
    // AUXILIARY METHODS //
    //////////////////////
    
    private int countTotalOfPerceptsInMO(){
        
        int total = 0;
        
        if (this.longPercepts != null) {
            for(Map<Percept,Double> map : this.longPercepts.values()){
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
    
    private double getPerceptActivation(Percept p, Map<Percept, Double> memoryPerceptsOfCategory){
        return memoryPerceptsOfCategory.get(p);
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
    
    
    
    private void removePerceptFromMemory(Percept p, MemoryObject mo){
        
        synchronized(mo){
            
            Map<String, Map<Percept, Double>> memoryPercepts = (Map<String, Map<Percept, Double>>) mo.getI();
            Map<Percept, Double> memoryPerceptsOfCategory;
            
            if(memoryPercepts.containsKey(p.getCategory())){
                memoryPerceptsOfCategory = memoryPercepts.get(p.getCategory());
                if (memoryPerceptsOfCategory.containsKey(p)) {
                    memoryPerceptsOfCategory.remove(p);
                    this.toDeletePercepts.add(p);
                    if (memoryPerceptsOfCategory.isEmpty()) {
                        memoryPercepts.remove(p.getCategory());
                    }
                }
            } 
        }
        
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
                        //System.out.println("Removed percept: " + p.getCategory() + " " + p.getPropertyByType("NAMEID=").getValue());
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
    
    private void memorizePercept(Percept percept, double perceptActivation){
        
        Percept pInMemory = AuxiliarMethods.getPerceptFromMemoryMap(percept.getCategory(), percept, this.longPercepts);
        
        if (pInMemory == null) {
            if (perceptActivation >= this.memorizerThreshold) {
                if (countTotalOfPerceptsInMO() < this.longMemoryCapacity) { //check perceptionMO capacity
                    this.insertPerceptInMemory(percept, this.longMO);
                } else{ //full memory 
                    Percept replacedPercept = this.selectPerceptToReplace();
                    if (replacedPercept != null) { //has percepts to replace
                        this.removePerceptFromMemory(replacedPercept, this.longMO);
                        this.insertPerceptInMemory(percept, this.longMO);
                    }
                }
            }
        } else{
            this.refreshPerceptInMemory(percept, pInMemory, this.longMO);
            this.incrementPerceptInMemory(percept, this.longMO, this.incrementPerCycle, this.maxActivation);
        }

    }
    
    private void addRemovedPerceptsToDeleteLongMO(){
        if (!this.toDeletePercepts.isEmpty()) {
            
            synchronized(this.toDeleteLongMO){
                this.toDeleteLongMemory = (List<Percept>) this.toDeleteLongMO.getI();
                        
                if (this.toDeleteLongMemory == null) {
                    this.toDeleteLongMemory = new ArrayList<>(this.toDeletePercepts);
                    this.toDeleteLongMO.setI(this.toDeleteLongMemory);
                } else{
                    for(Percept p : this.toDeletePercepts){
                        if(!this.toDeleteLongMemory.contains(p)){
                            this.toDeleteLongMemory.add(p);
                        }
                    }
                }
            }
        }
    }
    
    //////////////////////
    // OVERRIDE METHODS //
    //////////////////////
    
    @Override
    public void accessMemoryObjects() {
        this.longMO = (MemoryObject) this.getInput(MemoriesNames.LONG_MO);
        this.shortMO = (MemoryObject) this.getInput(MemoriesNames.SHORT_MO);
        this.reasonerMO = (MemoryObject) this.getInput(MemoriesNames.REASONER_MO);
        this.toDeleteLongMO = (MemoryObject) this.getInput(MemoriesNames.TO_DELETE_LONG_MO);
        this.synchronizerMO = (MemoryObject) this.getInput(MemoriesNames.SYNCHRONIZER_MO);
    }

    @Override
    public void calculateActivation() {
    }

    @Override
    public void proc() {
        
        this.longPercepts = (Map<String, Map<Percept, Double>>) this.longMO.getI();
        
        synchronized(this.shortMO){ 
            this.shortPercepts = (Map<String, Map<Percept, Double>>) this.shortMO.getI();
            this.shortPercepts = AuxiliarMethods.deepCopyMemoryMap(this.shortPercepts);
        }
        
        synchronized(this.reasonerMO){
            this.reasonerPercepts = (Map<String, Map<Percept, Double>>) this.reasonerMO.getI();
            this.reasonerPercepts = AuxiliarMethods.deepCopyMemoryMap(this.reasonerPercepts);
        }
        
        this.toDeletePercepts = new ArrayList<>();
        this.toReplacePercepts = new ArrayList<>();
        
        this.decrementPerceptsActivations(this.longMO);//decrement and create the todeleteList and toReplaceList
        
        for (Map<Percept, Double> perceptsOfCategory : this.shortPercepts.values()) {
            for (Map.Entry<Percept, Double> entry : perceptsOfCategory.entrySet()) {
                this.memorizePercept(entry.getKey(), entry.getValue());
            }
        }
        
        for (Map<Percept, Double> perceptsOfCategory : this.reasonerPercepts.values()) {
            for (Map.Entry<Percept, Double> entry : perceptsOfCategory.entrySet()) {
                this.memorizePercept(entry.getKey(), entry.getValue());
            }
        }
        
        //COLLECT METHODS
        Statistic.updateLongMO(countTotalOfPerceptsInMO());
        //
        
        addRemovedPerceptsToDeleteLongMO();
        
        AuxiliarMethods.synchronize(super.getName(),synchronizerMO);
    }
    
}
