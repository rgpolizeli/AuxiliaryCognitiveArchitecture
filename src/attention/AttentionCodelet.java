/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package attention;

import actionSelection.AffordanceType;
import actionSelection.SynchronizationMethods;
import actionSelection.ConsummatoryAffordanceType;
import actionSelection.IntermediateAffordanceType;
import main.MemoriesNames;
import perception.Percept;
import perception.Property;
import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import main.AuxiliarMethods;
import br.unicamp.cst.motivational.Drive;
import motivation.DriveHandle;

/**
 *
 * @author ricardo
 */
public class AttentionCodelet extends Codelet{

    class AffodanceTypeToPercept{
        public AffordanceType aff;
        public IntermediateAffordanceType interAff;
        public int hierarchyContribution;
        public String perceptCategory;

        public AffodanceTypeToPercept(AffordanceType aff, IntermediateAffordanceType interAff, int hierarchyContribution, String perceptType) {
            this.aff = aff;
            this.interAff = interAff;
            this.hierarchyContribution = hierarchyContribution;
            this.perceptCategory = perceptType;
        }
    }
    
    
    public AttentionCodelet(List<Property> salientProperties, double salienceBias, int memoryCapacity) {
        setSalientProperties(salientProperties);
        setSalienceBias(salienceBias);
        setMemoryCapacity(memoryCapacity);
    }
    
    private MemoryObject shortMO;
    private MemoryObject workingMO;
    private MemoryObject drivesHandlesMO;
    private MemoryObject synchronizerMO;
    
    private Map<String,Map<Percept, Double>> extractedPerceptsFromShortMO;
    private Map<String,List<Percept>> attentionPercepts;
    private Map<String,Map<Percept, Double>> shortPerceptsMap;
    private List<DriveHandle> drivesHandles;

    private List<Property> salientProperties;
    private double salienceBias = 0.3;
    private double drivesBias = 0.4;

    private int memoryCapacity = 0;
    
    private double maxImportance = 1.0;
    private double minImportance = 0.0;
    
    private final String attentionCategoryInWMO= "ATTENTION";
    
    //////////////////////
    // AUXILIARY METHODS //
    //////////////////////
    
    private void getPerceptsFromShortMO(){
        synchronized(this.shortMO){
            this.shortPerceptsMap = (Map<String,Map<Percept, Double>>) this.shortMO.getI();
            this.shortPerceptsMap = AuxiliarMethods.deepCopyMemoryMap(this.shortPerceptsMap);
        }
    }
    
    private void computePerceptsAttentionValues(){
        this.extractedPerceptsFromShortMO = new HashMap<>();
        this.drivesHandles = new CopyOnWriteArrayList( (List<DriveHandle>) drivesHandlesMO.getI());

        for (Map<Percept, Double> perceptsMap : this.shortPerceptsMap.values()) {
            for (Percept p : perceptsMap.keySet()) {

                double totalValue = 0.0;

                //action decision factors
                totalValue += computeDrivesBenefit(p);

                if (totalValue>0) {
                    //salient properties
                    totalValue += this.getSalienceBias()*this.countSalientProperties(p); 

                    //activation
                    totalValue += this.shortPerceptsMap.get(p.getCategory()).get(p); 
                }

                this.putPerceptInMap(p, totalValue, extractedPerceptsFromShortMO);
            }
        }
    }
    
    private void putPerceptInMap(Percept p, double value, Map<String,Map<Percept, Double>> extractedPercepts){
        
        Map<Percept, Double> perceptsOfCategory = extractedPercepts.get(p.getCategory());

        if(perceptsOfCategory == null){
            perceptsOfCategory = new HashMap<>();
            perceptsOfCategory.put(p, value);
            extractedPercepts.put(p.getCategory(), perceptsOfCategory);
        } else{
            if (perceptsOfCategory.containsKey(p)) {
                double totalValue = value + perceptsOfCategory.get(p);
                perceptsOfCategory.put(p, totalValue);
            } else{
                perceptsOfCategory.put(p, value);
            }
        }

    }
    
    private Percept getPerceptWithMaxValue(Map<Percept, Double> extractedPercepts){
        double maxValue = -1;
        Percept p = null;
        
        if (extractedPercepts != null && !extractedPercepts.isEmpty()) {
            for (Map.Entry<Percept, Double> e : extractedPercepts.entrySet()) {
                if (e.getValue() > maxValue) {
                    p = e.getKey();
                    maxValue = e.getValue();
                }
            }
        } else{
            //ERR
        }
        return p;
    }
    
    public void selectExtractedPercepts(){
        this.attentionPercepts = new HashMap<>();
        int quantityToSelect = this.getMemoryCapacity();
        Map<Percept, Double> extractedPerceptsCpy = new HashMap<>();
        
        for (Map<Percept,Double> perceptsOfCategory : this.extractedPerceptsFromShortMO.values()) {
            extractedPerceptsCpy.putAll(perceptsOfCategory);
        }
        
        while(quantityToSelect > 0 && !extractedPerceptsCpy.isEmpty()){ //while memory not full and there is extracted percepts to select 
            Percept p = this.getPerceptWithMaxValue(extractedPerceptsCpy);
            
            List<Percept> perceptsOfCategory = attentionPercepts.get(p.getCategory());
            if (perceptsOfCategory == null) {
                perceptsOfCategory = new ArrayList<>();
                perceptsOfCategory.add(p);
                attentionPercepts.put(p.getCategory(), perceptsOfCategory);
            } else{
                perceptsOfCategory.add(p);
            }
           
            extractedPerceptsCpy.remove(p);
            quantityToSelect -= 1;
        }
    }
    
    private int countSalientProperties(Percept percept){
        List<Property> salientProperties = this.getSalientProperties();
        int numberOfSalientProperties = 0;
        
        for (Property prop : percept.getProperties()) {
            if (salientProperties.contains(prop)) {
                numberOfSalientProperties+=1;
            }
        }
        
        return numberOfSalientProperties;
    }
    
    public List<Property> getSalientProperties(){
        return this.salientProperties;
    }
    
    public double getSalienceBias(){
        return this.salienceBias;
    }
    
    public int getMemoryCapacity() {
        return memoryCapacity;
    }
    
    public void setMemoryCapacity(int memoryCapacity) {
        this.memoryCapacity = memoryCapacity;
    }
    
    public void setSalienceBias(double salienceBias) {
        this.salienceBias = salienceBias;
    }
    
    public void setSalientProperties(List<Property> newSalientProperties){
        this.salientProperties = newSalientProperties;
    }
   
    //private boolean isPerceptRelevantToAffordanceType(AffordanceType aff, Percept p){
    //    return aff.getRelevantPerceptsCategories().contains(p.getCategory()) && aff.isRelevantPercept(p);
    //}
    
    
    private List<AffodanceTypeToPercept> searchAffordancesTypesToPercept(ConsummatoryAffordanceType consummatoryAffordance, Percept p){
        
        List<AffodanceTypeToPercept> affordancesToPercept = new ArrayList<>();
        int level; //level in tree;
        Map<Integer, List<IntermediateAffordanceType>> openMap; // explored affordances
        
        if ( consummatoryAffordance.isRelevantPercept(p) ) { //if percept p is relevant to current affordance type
            AffodanceTypeToPercept affToPercept = new AffodanceTypeToPercept(consummatoryAffordance,  null, 1, p.getCategory());
            affordancesToPercept.add(affToPercept);
        }

        level = 2;
        openMap = new HashMap<>();
        openMap.put(level, consummatoryAffordance.getIntermediateAffordances());

        boolean endSearch = false;

        while(!endSearch){ //Breadth-first search = busca em largura

            List<IntermediateAffordanceType> intermediateAffordances = openMap.get(level);

            for (IntermediateAffordanceType intermediateAff : intermediateAffordances) {
                AffordanceType aff = intermediateAff.getAffordance();
                
                if (aff.isRelevantPercept(p)) { //if percept p is relevant, add it in List
                    AffodanceTypeToPercept affToPercept = new AffodanceTypeToPercept(aff, intermediateAff, level, p.getCategory());
                    affordancesToPercept.add(affToPercept);
                } 
                if (openMap.containsKey(level+1)) {
                    List<IntermediateAffordanceType> inferiorIntermediateAffordances = openMap.get(level+1);
                    inferiorIntermediateAffordances.addAll(aff.getIntermediateAffordances());
                } else{
                    openMap.put(level+1, new ArrayList<>(aff.getIntermediateAffordances()));
                }

            }

            level++;

            if (openMap.get(level)==null) {
                endSearch = true;
            } else if (openMap.get(level).isEmpty()) {
                endSearch = true;
            }
        }
        
        return affordancesToPercept;
    }
    
    private double computeDrivesBenefit(Percept p){
        double benefit = 0.0;
        List<AffodanceTypeToPercept> affordancesToPercept;
        
        for (DriveHandle driveHandle : this.drivesHandles) {
            
            for (ConsummatoryAffordanceType consummatoryAff : driveHandle.getConsummatoryAffordances()) {
                affordancesToPercept = this.searchAffordancesTypesToPercept(consummatoryAff, p);
                benefit += computeAffordancesBenefit(consummatoryAff, driveHandle.getDrive(), affordancesToPercept, p); //benefit
            }
        }
     
        return (this.drivesBias*benefit);
    }
    
    private double computeAffordancesBenefit(ConsummatoryAffordanceType consummatoryAffordance, Drive drive, List<AffodanceTypeToPercept> affordancesToPercept, Percept p){
        double benefit = 0.0;
        
        if (!affordancesToPercept.isEmpty()) {
            for (AffodanceTypeToPercept affToPercept : affordancesToPercept) {
                if (affToPercept.aff.equals((AffordanceType)consummatoryAffordance))
                    benefit +=  drive.getActivation(); 
                else
                    benefit +=  (1.0/(double)affToPercept.hierarchyContribution) * drive.getActivation();
            }
        }
        
        return benefit;
    }
    
    private int countTotalOfPerceptsInMO(Map<String, List<Percept>> percepts){
        
        int total = 0;
        
        if (percepts != null) {
            for(List<Percept> map : percepts.values()){
                total+= map.size();
            }
        }
        
        return total;
    }
    
    
    
    private void addAttentionPerceptsToWorkingMO(){
        
        synchronized(this.workingMO){
            Map<String,Map<String,List<Percept>>> workingMemory = (Map<String,Map<String,List<Percept>>>) this.workingMO.getI();
            workingMemory.put(this.attentionCategoryInWMO, attentionPercepts);
        }
        
    }
    
    
    
    //////////////////////
    // OVERRIDE METHODS //
    //////////////////////
    
    @Override
    public void accessMemoryObjects() {
        this.shortMO = (MemoryObject) this.getInput(MemoriesNames.SHORT_MO);
        this.workingMO = (MemoryObject) this.getInput(MemoriesNames.WORKING_MO);
        this.drivesHandlesMO = (MemoryObject) this.getInput(MemoriesNames.DRIVES_HANDLE_MO);
        this.synchronizerMO = (MemoryObject) this.getInput(MemoriesNames.SYNCHRONIZER_MO);
    }

    @Override
    public void calculateActivation() {
    }

    @Override
    public void proc() {
        
        getPerceptsFromShortMO();
        
        if (!this.shortPerceptsMap.isEmpty()) {
            
            computePerceptsAttentionValues();
            selectExtractedPercepts();
            //COLLECT METHODS
            //Statistic.updateAttentionMO(countTotalOfPerceptsInMO(newAttentionPercepts));
            //
            addAttentionPerceptsToWorkingMO();
        }
        
        SynchronizationMethods.synchronize(super.getName(),this.synchronizerMO);
    }
    
}
