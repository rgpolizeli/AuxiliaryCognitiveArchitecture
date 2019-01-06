/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package motivation;

import actionSelection.AffordanceType;
import perception.Percept;
import br.unicamp.cst.representation.owrl.Property;
import java.util.List;
import br.unicamp.cst.motivational.Drive;

/**
 *
 * @author rgpolizeli
 */
public abstract class DriveHandle{

    private Drive drive;
    private List<String> propertiesNames;
    private List<String> relevantPerceptsCategories;
    private List<AffordanceType> consummatoryAffordances;
    private double minActivation = 0.0;
    private double maxActivation = 1.0;
    
    /**
     *
     * @param drive The drive.
     * @param propertiesNames The properties' names in self percept to get value of the drive.
     * @param relevantPerceptsCategories The categories of target percepts.
     * @param consummatoryAffordances The ConsummatoryAffordance's that realize this drive.
     * @param minActivation The minimum activation value of this drive.
     * @param maxActivation The maximum activation value of this drive.
     */
    public DriveHandle(Drive drive, List<String> propertiesNames, List<String> relevantPerceptsCategories, List<AffordanceType> consummatoryAffordances, double minActivation, double maxActivation) {
        setDrive(drive);
        setRelevantPropertiesNames(propertiesNames);
        setRelevantPerceptsCategories(relevantPerceptsCategories);
        setConsummatoryAffordances(consummatoryAffordances);
        setMinActivation(minActivation);
        setMaxActivation(maxActivation);
    }
    
    /////////////////////
    // AUXILIARY METHODS //
    /////////////////////
    
    public Drive getDrive() {
        return this.drive;
    }
    
    public List<String> getRelevantPropertiesNames() {
        return this.propertiesNames;
    }
    
    public List<String> getRelevantPerceptsCategories() {
        return this.relevantPerceptsCategories;
    }
    
    public List<AffordanceType> getConsummatoryAffordances() {
        return this.consummatoryAffordances;
    }
    
    public double getMinActivation(){
        return this.minActivation;
    }
    
    public double getMaxActivation(){
        return this.maxActivation;
    }
    
    public void setDrive(Drive d){
        this.drive = d;
    }
    
    public void setRelevantPropertiesNames(List<String> propertiesNames) {
        this.propertiesNames = propertiesNames;
    }
    
    public void setRelevantPerceptsCategories(List<String> relevantPerceptsCategories) {
        this.relevantPerceptsCategories = relevantPerceptsCategories;
    }
    
    public void setConsummatoryAffordances(List<AffordanceType> consummatoryAffordances) {
        this.consummatoryAffordances = consummatoryAffordances;
    }
    
    public void setMinActivation(double minActivation){
        this.minActivation = minActivation;
    }
    
    public void setMaxActivation(double maxActivation){
        this.maxActivation = maxActivation;
    }
    
    /////////////////////
    // ABSTRACT METHODS //
    /////////////////////
    
    /**
     * Define if a percept is relevant for the drive. For each types of percepts relevant to drive, it is necessary specify conditions to define if it is or isn't relevant.
     * @param percept
     * @return 
     */
    public abstract boolean isRelevantPercept(Percept percept);
    
    /**
     * Compute the drive's value based on properties of the self percept and decrement or increment per cycle.
     * @param relevantProperties
     * @return 
     */
    public abstract double computeActivation(List<Property> relevantProperties);
    
}
