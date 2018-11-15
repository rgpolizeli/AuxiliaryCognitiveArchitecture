/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package motivation;

import actionSelection.AuxiliarMethods;
import main.MemoriesNames;
import actionSelection.Statistic;
import perception.Percept;
import perception.Property;
import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author ricardo
 */
public class DriveHandleCodelet extends Codelet{
    
    private MemoryObject drivesMO;
    private MemoryObject shortMO;
    private MemoryObject synchronizerMO;
    
    private List<Drive> drives;
    private final String selfPerceptCategory;
    
    private Map<String,Map<Percept, Double>> shortPerceptsMap;
    private Percept selfPercept;
    
    private double relevantPerceptsIncrement = 0.2;

    public DriveHandleCodelet(String selfPerceptCategory, double relevantPerceptsIncrement) {
        this.selfPerceptCategory = selfPerceptCategory;
        this.relevantPerceptsIncrement = relevantPerceptsIncrement;
    }
    
    //////////////////////
    // AUXILIARY METHODS //
    //////////////////////
    
    public Percept getSelfPerceptFromPerceptionMemory(){ 
        
        if (this.shortPerceptsMap.containsKey(this.selfPerceptCategory)) {
            return this.shortPerceptsMap.get(this.selfPerceptCategory).keySet().iterator().next();
        } else{
            return null;
        }

    }
    
    private List<Property> getDriveRelevantPropertiesFromSelfPercept(Drive d){
        List<Property> relevantProperties = new ArrayList<>();
        
        for (String propertyName : d.getRelevantPropertiesNames()) {
            Property prop = this.selfPercept.getPropertyByType(propertyName);
            if (prop == null) {
                //err
                System.out.println("ERRO - DRIVE SEM PROPRIEDADE");
            } else{
                relevantProperties.add(prop);
            }
        }
        return relevantProperties;
    }
    
    
    public Map<String, Percept> searchRelevantPercepts(Drive d){ //find relevant concepts with properties to affordance. 
        Map<String,Percept> relevantPercepts = new HashMap();
        Iterator<String> relevantCategoriesIterator = d.getRelevantPerceptsCategories().iterator();
        
        while (relevantPercepts.size() < d.getRelevantPerceptsCategories().size() && relevantCategoriesIterator.hasNext()) {
            
            String relevantPerceptCategory = relevantCategoriesIterator.next();
            Map<Percept, Double> perceptsOfCategory = this.shortPerceptsMap.get(relevantPerceptCategory);
            if (perceptsOfCategory!=null) {
                Iterator<Percept> perceptsIterator = perceptsOfCategory.keySet().iterator();
            
                while (!relevantPercepts.containsKey(relevantPerceptCategory) && perceptsIterator.hasNext()) {   
                    Percept percept = perceptsIterator.next();
                    if (d.isRelevantPercept(percept)) { //if percept contains all relevant properties for drive
                        relevantPercepts.put(percept.getName(), percept); 
                    }
                }
            }
            
            
        }
        
        return relevantPercepts;
    }
    
    private double computeIncrements(Drive d){
        double increments;
       
        Map<String,Percept> relevantPercepts = this.searchRelevantPercepts(d);
        
        increments = ((double)relevantPercepts.size()/(double)d.getRelevantPerceptsCategories().size()) * this.relevantPerceptsIncrement;
        
        return increments;
    }
    
    
    
    ////////////////////
    //OVERRIDE METHODS//
    ////////////////////
    
    @Override
    public void accessMemoryObjects() {
        drivesMO = (MemoryObject) this.getInput(MemoriesNames.DRIVE_MO);
        shortMO = (MemoryObject) this.getInput(MemoriesNames.SHORT_MO);
        synchronizerMO = (MemoryObject) this.getInput(MemoriesNames.SYNCHRONIZER_MO);
    }

    @Override
    public void calculateActivation() {
    }

    @Override
    public void proc() {
        
        this.drives = new CopyOnWriteArrayList( (List<Drive>) this.drivesMO.getI() );
        
        synchronized(this.shortMO){
            this.shortPerceptsMap = (Map<String,Map<Percept, Double>>) this.shortMO.getI();
            this.shortPerceptsMap = AuxiliarMethods.deepCopyMemoryMap(this.shortPerceptsMap);
        }
        
        this.selfPercept = this.getSelfPerceptFromPerceptionMemory(); 
        
        double activation;
        for (Drive d: drives) {
            if (this.selfPercept == null) { //null quando o ShortMemoryCodelet executa antes do PerceptionCodelet
                activation = d.getMinActivation();
            } else{
                List<Property> relevantProperties = this.getDriveRelevantPropertiesFromSelfPercept(d);
                activation = d.computeActivation(relevantProperties);
                activation = AuxiliarMethods.normalize(activation, d.getMaxActivation(), d.getMinActivation());
                activation += computeIncrements(d);
            }
            d.setValue(activation);
        }
        
        this.drivesMO.setI(this.drives);
        
        //collect methods
        Statistic.putDrivesActivationInData(this.drives);
        //
        
        AuxiliarMethods.synchronize(super.getName());
    }
    
}
