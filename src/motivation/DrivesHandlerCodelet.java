/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package motivation;

import actionSelection.SynchronizationMethods;
import main.MemoriesNames;
import actionSelection.Statistic;
import perception.Percept;
import perception.Property;
import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.motivational.Drive;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.AuxiliarMethods;

/**
 *
 * @author rgpolizeli
 */
public class DrivesHandlerCodelet extends Codelet{
    
    private MemoryObject drivesHandlesMO;
    private MemoryObject shortMO;
    private MemoryObject synchronizerMO;
    
    private List<DriveHandle> drivesHandles;
    private final String selfPerceptCategory;
    
    private Map<String,Map<Percept, Double>> shortPerceptsMap;
    private Percept selfPercept;
    
    private double relevantPerceptsIncrement = 0.2;

    private final Logger LOGGER = Logger.getLogger(DrivesHandlerCodelet.class.getName());
    
    public DrivesHandlerCodelet(String selfPerceptCategory, double relevantPerceptsIncrement) {
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
    
    private List<Property> getDriveRelevantPropertiesFromSelfPercept(DriveHandle driveHandle){
        List<Property> relevantProperties = new ArrayList<>();
        
        for (String propertyName : driveHandle.getRelevantPropertiesNames()) {
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
    
    
    public Map<String, Percept> searchRelevantPercepts(DriveHandle driveHandle){ //find relevant concepts with properties to affordance. 
        Map<String,Percept> relevantPercepts = new HashMap();
        Iterator<String> relevantCategoriesIterator = driveHandle.getRelevantPerceptsCategories().iterator();
        
        while (relevantPercepts.size() < driveHandle.getRelevantPerceptsCategories().size() && relevantCategoriesIterator.hasNext()) {
            
            String relevantPerceptCategory = relevantCategoriesIterator.next();
            Map<Percept, Double> perceptsOfCategory = this.shortPerceptsMap.get(relevantPerceptCategory);
            if (perceptsOfCategory!=null) {
                Iterator<Percept> perceptsIterator = perceptsOfCategory.keySet().iterator();
            
                while (!relevantPercepts.containsKey(relevantPerceptCategory) && perceptsIterator.hasNext()) {   
                    Percept percept = perceptsIterator.next();
                    if (driveHandle.isRelevantPercept(percept)) { //if percept contains all relevant properties for drive
                        relevantPercepts.put(percept.getName(), percept); 
                    }
                }
            }
            
            
        }
        
        return relevantPercepts;
    }
    
    private double computeIncrements(DriveHandle driveHandle){
        double increments;
       
        Map<String,Percept> relevantPercepts = this.searchRelevantPercepts(driveHandle);
        
        increments = ((double)relevantPercepts.size()/(double)driveHandle.getRelevantPerceptsCategories().size()) * this.relevantPerceptsIncrement;
        
        return increments;
    }
    
    //collect methods
    private List<Drive> getDrivesFromDrivesHandles(List<DriveHandle> drivesHandles){
        List<Drive> drives = new ArrayList<>();
        for(DriveHandle driveHandle : drivesHandles){
            drives.add(driveHandle.getDrive());
        }
        return drives;
    }
    //
    
    
    ////////////////////
    //OVERRIDE METHODS//
    ////////////////////
    
    @Override
    public void accessMemoryObjects() {
        drivesHandlesMO = (MemoryObject) this.getInput(MemoriesNames.DRIVES_HANDLE_MO);
        shortMO = (MemoryObject) this.getInput(MemoriesNames.SHORT_MO);
        synchronizerMO = (MemoryObject) this.getInput(MemoriesNames.SYNCHRONIZER_MO);
    }

    @Override
    public void calculateActivation() {
    }

    @Override
    public void proc() {
        
        
        
        synchronized(this.shortMO){
            this.shortPerceptsMap = (Map<String,Map<Percept, Double>>) this.shortMO.getI();
            this.shortPerceptsMap = AuxiliarMethods.deepCopyMemoryMap(this.shortPerceptsMap);
        }
        
        this.selfPercept = this.getSelfPerceptFromPerceptionMemory(); 
        
        synchronized(this.drivesHandlesMO){
            this.drivesHandles = (List<DriveHandle>) this.drivesHandlesMO.getI();
            
            double driveActivation;
            Drive drive;
            for (DriveHandle driveHandle: drivesHandles) {
                
                if (this.selfPercept == null) { //null quando o ShortMemoryCodelet executa antes do PerceptionCodelet
                    driveActivation = driveHandle.getMinActivation();
                } else{
                    List<Property> relevantProperties = this.getDriveRelevantPropertiesFromSelfPercept(driveHandle);
                    driveActivation = driveHandle.computeActivation(relevantProperties);
                    driveActivation = AuxiliarMethods.normalize(driveActivation, driveHandle.getMaxActivation(), driveHandle.getMinActivation());
                    driveActivation += computeIncrements(driveHandle);
                }
                drive = driveHandle.getDrive();
                drive.setActivation(driveActivation);
                LOGGER.log(Level.INFO,"Drive: {0} , Activation: {1}", new Object[]{drive.getName(), drive.getActivation()});
            }
            //collect methods
            Statistic.putDrivesActivationInData(this.getDrivesFromDrivesHandles(this.drivesHandles));
            //
        }
        
        SynchronizationMethods.synchronize(super.getName(),this.synchronizerMO);
    }
    
}
