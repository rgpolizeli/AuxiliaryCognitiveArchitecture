/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package memory;

import motivation.Drive;
import actionSelection.AffordanceType;
import actionSelection.AuxiliarMethods;
import actionSelection.ConsummatoryPathInfo;
import actionSelection.ExtractedAffordance;
import actionSelection.IntermediateAffordanceType;
import actionSelection.MemoryObjectsNames;
import perception.Percept;
import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import motivation.BiasDecisionFactor;
import motivation.DecisionFactor;

/**
 *
 * @author ricardo
 */
public class RememberCodelet extends Codelet{

    private MemoryObject longMO;
    private MemoryObject driveMO;
    private MemoryObject biasDecisionFactorsMO;
    private MemoryObject reasonerMO;
    private MemoryObject rememberMO;
    private MemoryObject workingMO;
    private MemoryObject extractedAffordancesMO;
    private MemoryObject toDeleteLongMO;
    private List<ExtractedAffordance> extractedAffordances;
    
    private MemoryObject synchronizerMO;
    
    private Map<String,List<Percept>> attentionPercepts;
    private Map<String,Map<Percept,Double>> memoryPercepts;
    private Map<String,Map<Percept,Double>> reasonerPercepts;
    private List<Drive> drives;
    private List<BiasDecisionFactor> biasFactors;
    private Map<DecisionFactor, List<Remember>> remembers;
    private Map<String,List<Percept>> situation;
    
    private final Map<String, Integer> notRemembers; //(String = drive name + affordance parent name + affordance name)
   
    private Map<Drive, Double> drivesActivations;
    private Map<BiasDecisionFactor, Double> biasFactorsActivations;
    
    private final int totalMemoryCapacity;
    private final int totalPerceptsPerCategory;
    
    private final String rememberCategoryInWMO= "REMEMBER";
    
    private int rememberDuration = 3;
    private int rememberDecrement = 1;
    private int rememberForgetThreshold = 0;
    
    private Random rdm = new Random();
    
    private Long AffordanceExtractorTimeStamp = -Long.MIN_VALUE;
    
    public RememberCodelet(int totalMemoryCapacity, int totalPerceptsPerCategory, int rememberDuration, int rememberDecrement, int rememberForgetThreshold) {
        this.totalMemoryCapacity = totalMemoryCapacity;
        this.totalPerceptsPerCategory = totalPerceptsPerCategory;
        this.rememberDuration = rememberDuration;
        this.rememberDecrement = rememberDecrement;
        this.rememberForgetThreshold = rememberForgetThreshold;
        
        this.notRemembers = new HashMap<>();
    }
    
    //////////////////////
    // AUXILIARY METHODS //
    //////////////////////
    class AffordanceAndParent{
        public AffordanceType affordance;
        public AffordanceType parentAffordance;

        public AffordanceAndParent(AffordanceType affordance, AffordanceType parentAffordance) {
            this.affordance = affordance;
            this.parentAffordance = parentAffordance;
        }
        
    }
    
    
    /**
     * Verifies if there is at least one of the percepts extracted from the memory of each type of percept necessary for the execution of affordance extracted from memory. If any type is missing, check if there is at least one percept of this type in the perception, making affordance executable.
     * @param aff
     * @param relevantPercepts
     * @return 
     */
    private boolean isFeasible(AffordanceType aff, Map<String,List<Percept>> relevantPercepts){
        
        //Check for at least one percept for each of the required types
        Set<String> relevantPerceptsCategories = aff.getRelevantPerceptsCategories().keySet();
        if (relevantPercepts.keySet().containsAll(relevantPerceptsCategories)) {
            return true;
        } else{
            
            //Check if there is at least one percept for each of the necessary types missing in the attention and not in the memory
            Set<String> missingRelevantPerceptsCategories = AuxiliarMethods.setDifference(new HashSet<>(relevantPerceptsCategories), relevantPercepts.keySet());
            
            if (missingRelevantPerceptsCategories.size() == relevantPerceptsCategories.size()) { //if all types of percepts are missing, nothing to recover from memory 
                return false;
            }
            
            for (String missingRelevantCategory : missingRelevantPerceptsCategories) {
                List<String> perceptsCategories = aff.getRelevantPerceptsCategories().get(missingRelevantCategory);
                if (perceptsCategories!=null) {
                    List<Percept> relevantMissingPercepts = new ArrayList<>();
                    for (String perceptCategory : perceptsCategories) {
                        relevantMissingPercepts.addAll(this.getRelevantPerceptsByCategory(aff, perceptCategory, this.attentionPercepts.get(perceptCategory)));
                    }
                    if (relevantMissingPercepts.isEmpty()) {
                        return false;
                    }
                }   
            }
            
            return true; //if there is at least one relevant perception percept for each of the missing percepts types 
        }
    }
    
    private Map<String,List<Percept>> convertRelevantCategoryToPerceptCategory(Map<String,List<Percept>> relevantPercepts){
        Map<String,List<Percept>> converted = new HashMap<>();
        for (Map.Entry<String, List<Percept>> entry : relevantPercepts.entrySet()) {
            String category = entry.getKey();
            List<Percept> perceptsOfCategory = entry.getValue();
            Percept firstPercept = perceptsOfCategory.get(0);
            
            if (!category.equals(firstPercept.getCategory())) {
                converted.put(firstPercept.getCategory(), perceptsOfCategory);
            } else{
                converted.put(category, perceptsOfCategory);
            }
            
        }
        return converted;
    }
    
    private boolean isInNotRemember(Drive drive, AffordanceAndParent affAndParent){
        if (affAndParent.parentAffordance == null) //consummatory affordance
            return this.notRemembers.containsKey(drive.getName()+affAndParent.affordance.getAffordanceName());
        else
            return this.notRemembers.containsKey(drive.getName()+affAndParent.parentAffordance.getAffordanceName()+affAndParent.affordance.getAffordanceName());
    }
    
    private boolean isInExtractedAffordances(Drive drive,AffordanceAndParent affAndParent){
        for (ExtractedAffordance extAff : this.extractedAffordances) {
            if (extAff.getAffordanceType().equals(affAndParent.affordance)) {
                for (ConsummatoryPathInfo consummatoryPath : extAff.getConsummatoryPaths()) {
                    List<IntermediateAffordanceType> intermediateAffordances = consummatoryPath.getIntermediateAffordancesToDrive(drive);
                    if (intermediateAffordances != null) {
                        for (IntermediateAffordanceType intermediateAffordance : intermediateAffordances) {
                            if (intermediateAffordance.getAffordance().equals(affAndParent.affordance) && intermediateAffordance.getParentAffordance().equals(affAndParent.parentAffordance)) {
                                return true;
                            }
                        }
                    }   
                }
            }
        }
        
        return false;
    }
    
    private boolean hasRememberToAffordance(List<Remember> remembersToDrive,AffordanceAndParent affAndParent){
        
        if (remembersToDrive== null) {
            return false;
        }
        
        for(Remember r: remembersToDrive){
            if (r.getCurrentAff().equals(affAndParent.affordance)) {
                if (affAndParent.parentAffordance == null) {
                    return true;
                } else{
                    if (r.getParentAff().equals(affAndParent.parentAffordance)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    private void decrementNotRemembers(){
        if (!this.notRemembers.isEmpty()) {
            for (Map.Entry<String, Integer> entry : new HashMap<>(this.notRemembers).entrySet()) {
                Integer cycles = entry.getValue();
                if (cycles == 0) {
                    this.notRemembers.remove(entry.getKey());
                } else{
                    cycles--;
                    this.notRemembers.replace(entry.getKey(), cycles);
                }
            }
        }
        
    }
    
    private void decrementRemembers(){
        synchronized(this.rememberMO){
            Map<DecisionFactor, List<Remember>> remembersBkp = this.deepCopyRememberMap(this.remembers);
            
            for (Map.Entry<DecisionFactor, List<Remember>> entry : remembersBkp.entrySet()) {
                
                DecisionFactor factor = entry.getKey();
                List<Remember> remembersOfFactorBkp = entry.getValue();
                
                List<Remember> remembersOfFactor = this.remembers.get(factor);
                
                for (Remember rb : remembersOfFactorBkp) {
                    if (rb.getDuration() == this.rememberForgetThreshold) {
                        remembersOfFactor.remove(rb);
                        if (remembersOfFactor.isEmpty()) {
                            this.remembers.remove(factor);
                        }
                    } else{
                        rb.setDuration( (rb.getDuration() - this.rememberDecrement) );
                    }
                }
            }
            this.rememberMO.setI(this.remembers);
        }
    }
    
    private void updateRemembersToDrive(Drive drive){
        List<Remember> remembersToDrive = this.remembers.get(drive);
        
        if (remembersToDrive!=null) {
            
            for (Remember r : new ArrayList<>(remembersToDrive)) { //se eu removo da lista, como posso pegar pelo indice? Sendo que o indice não mais será o mesmo do da cópia.
                
                AffordanceAndParent currentAff = new AffordanceAndParent(r.getCurrentAff(), r.getParentAff());
                if (!isInExtractedAffordances(drive,currentAff)) {
                    
                    remembersToDrive.remove(r);
                    if (remembersToDrive.isEmpty()) {
                        this.remembers.remove(drive);
                    }

                    int cycles;
                    
                    if (currentAff.parentAffordance == null) { //consummatory affordance
                        cycles = drive.getConsummatoryAffordances().size();
                        this.notRemembers.put(drive.getName()+currentAff.affordance.getAffordanceName(), cycles);
                        
                        //System.out.println("DELETED: " + drive.getName()+currentAff.affordance.getAffordanceName());
                        
                    } else{
                        cycles = currentAff.parentAffordance.getIntermediateAffordances().size()-1; //quantity of other alternatives affordances
                        this.notRemembers.put(drive.getName()+currentAff.parentAffordance.getAffordanceName()+currentAff.affordance.getAffordanceName(), cycles);
                        //System.out.println("DELETED: " + drive.getName()+currentAff.parentAffordance.getAffordanceName()+currentAff.affordance.getAffordanceName());
                    }
                }
            }
        } 
    }
    
    private List<Remember> createRemembersToDrive(Drive drive){
        
        List<Remember> newRemembersToDrive = new ArrayList<>();

        Map<Integer, List<AffordanceAndParent>> openMap = new HashMap<>();
        Map<Integer, List<AffordanceAndParent>> openMapBkp = new HashMap<>();

        List<AffordanceAndParent> affordances = new ArrayList<>();
        List<AffordanceAndParent> affordancesBkp = new ArrayList<>();

        for (AffordanceType aff : drive.getConsummatoryAffordances()) {
            affordances.add(new AffordanceAndParent(aff, null));
            affordancesBkp.add(new AffordanceAndParent(aff, null));
        }

        int level = 1;
        //boolean findedInLevel = false;

        AffordanceAndParent currentAff;
        Map<String,List<Percept>> relevantPercepts;

        openMap.put(level, affordances);
        openMapBkp.put(level, affordancesBkp);

        while (!openMap.get(level).isEmpty()) {

            currentAff = openMap.get(level).remove(0); //get and remove first
            if (!isInNotRemember(drive,currentAff) && !hasRememberToAffordance(this.remembers.get(drive), currentAff)) {
                relevantPercepts = this.getRelevantPercepts(currentAff.affordance, this.memoryPercepts); 
                if (this.isFeasible(currentAff.affordance, relevantPercepts)) {
                    
                    for (Map.Entry<String, List<Percept>> entry : relevantPercepts.entrySet()) {
                        List<Percept> relevantPerceptsOfCategory = entry.getValue();
                        if (relevantPerceptsOfCategory.size() > this.totalPerceptsPerCategory ) {
                            List<Percept> relevantPerceptsOfCategoryBkp = relevantPerceptsOfCategory.stream().unordered().limit(this.totalPerceptsPerCategory).collect(Collectors.toList());
                            relevantPerceptsOfCategory.clear();
                            relevantPerceptsOfCategory.addAll(relevantPerceptsOfCategoryBkp);
                        }
                    }
                    
                    Remember rb = new Remember(currentAff.affordance,currentAff.parentAffordance,level,convertRelevantCategoryToPerceptCategory(relevantPercepts),this.rememberDuration);
                    newRemembersToDrive.add(rb);
                    //findedInLevel = true;
                }
            } 

            //put next level's affordances in map
            if (openMap.containsKey(level+1)) { //if any affordancetype of this level already put your intermediates in map.
                affordances = openMap.get(level+1);
                affordancesBkp = openMapBkp.get(level+1);
            } else{
                affordances = new ArrayList<>();
                affordancesBkp = new ArrayList<>();

                openMap.put(level+1, affordances);
                openMapBkp.put(level+1, affordancesBkp);
            }

            for (AffordanceType aff : currentAff.affordance.getIntermediateAffordancesAsAffordancesList()) {
                affordances.add(new AffordanceAndParent(aff, currentAff.affordance));
                affordancesBkp.add(new AffordanceAndParent(aff, currentAff.affordance));
            }

            if(openMap.get(level).isEmpty()){ //if affordance to level is empty, go to next level
                //if (!findedInLevel) {
                    level++;
                //}
            }
        }
            
        return newRemembersToDrive;
        
        /*
        if (rb != null) {
                synchronized(this.rememberMO){
                    this.remembers.put(drive, rb);
                    this.rememberMO.setI(this.remembers);
                }
            }
        */
    }
    
    /*
    private void createRememberToBiasDecisionFactors(BiasDecisionFactor factor){
        Remember rb = null;
        Map<String,List<Percept>> relevantPercepts = this.getRelevantPercepts(factor, this.memoryPercepts);
        
        Iterator<List<Percept>> it = relevantPercepts.values().iterator();
        
        while (it.hasNext() && rb == null) {
            List<Percept> relevantPerceptsList = it.next();
            if (!relevantPerceptsList.isEmpty()) {
                rb = new Remember(null,null,1,relevantPercepts);
            }
        }
        
        if (rb != null) {
            if (this.remembers.containsKey(factor)) {
                synchronized(this.rememberMO){
                        this.remembers.replace(factor, rb);
                        this.rememberMO.setI(this.remembers);
                }
            } else{
                synchronized(this.rememberMO){
                    this.remembers.put(factor, rb);
                    this.rememberMO.setI(this.remembers);
                }
            }
        }
       
    }
    */
    
    
    
    
    private void computeDrivesActivations(){
        this.drivesActivations = new HashMap<>();
        
        for (Drive factor : this.drives) {
            drivesActivations.put(factor, factor.getValue());
        }
    }
    
    private void computeBiasDecisionFactorsActivations(){
        this.biasFactorsActivations = new HashMap<>();
        
        for (BiasDecisionFactor factor : this.biasFactors) {
            this.biasFactorsActivations.put(factor, factor.getValue());
        }
    }
    
    /**
     * Get relevant percepts from memory by percept type
     * @param aff
     * @param perceptsMap
     * @return 
     */
    public Map<String, List<Percept>> getRelevantPercepts(AffordanceType aff, Map<String, Map<Percept,Double>> perceptsMap){ //find relevant concepts with properties to affordance. 
        
        Map<String, List<Percept>> relevantPercepts = new HashMap();
        Map<String,List<String>> relevantPerceptCategoriesMap = aff.getRelevantPerceptsCategories();
        
        for (Map.Entry<String,List<String>> entry : relevantPerceptCategoriesMap.entrySet()) {
            String relevantPerceptCategory = entry.getKey();
            List<String> perceptsCategories = entry.getValue();
            
            Map<Percept, Double> perceptsOfRelevantCategory = new HashMap<>();
            
            for (String perceptCategory : perceptsCategories) {
                Map<Percept,Double> perceptsOfCategory = perceptsMap.get(perceptCategory);
                if (perceptsOfCategory!=null) {
                    perceptsOfRelevantCategory.putAll(perceptsOfCategory);
                }
            }
            
            for (Percept percept : perceptsOfRelevantCategory.keySet()) {

                if (aff.isRelevantPercept(percept)) { //if percept is relevant to aff. type
                    List<Percept> percepts;
                    if (relevantPercepts.containsKey(relevantPerceptCategory)) {
                        percepts = relevantPercepts.get(relevantPerceptCategory);
                    } else{
                        percepts = new ArrayList<>();
                    }
                    percepts.add(percept);
                    relevantPercepts.put(relevantPerceptCategory, percepts);
                }

            }
            
        }
        
        return relevantPercepts;
    }
    
    public Map<String, List<Percept>> getRelevantPercepts(BiasDecisionFactor factor, Map<String, Map<Percept,Double>> perceptsMap){ //find relevant concepts with properties to affordance. 
        
        Map<String, List<Percept>> relevantPercepts = new HashMap();
        List<String> relevantPerceptCategories = factor.getRelevantPerceptsCategories();
        
        for (String perceptCategory : relevantPerceptCategories) {
        
            Map<Percept, Double> perceptsOfCategory = perceptsMap.get(perceptCategory);
            if (perceptsOfCategory!=null) {
                for (Percept percept : perceptsOfCategory.keySet()) {
               
                    if (factor.isRelevantPercept(percept, this.situation)) { //if percept is relevant to biasFactor
                        List<Percept> percepts;
                        if (relevantPercepts.containsKey(perceptCategory)) {
                            percepts = relevantPercepts.get(perceptCategory);
                        } else{
                            percepts = new ArrayList<>();
                        }
                        percepts.add(percept);
                        relevantPercepts.put(perceptCategory, percepts);
                    }
                
                }
            }
            
        }
        
        return relevantPercepts;
    }
    
    public List<Percept> getRelevantPerceptsByCategory(AffordanceType aff, String perceptCategory, List<Percept> perceptsInput){ //find relevant concepts with properties to affordance. 
        
        List<Percept> relevantPercepts = new ArrayList<>();
        
        if (perceptsInput!=null) {
            for (Percept percept : perceptsInput) {
                if (aff.isRelevantPercept(percept)) { //if percept is relevant to aff. type
                    relevantPercepts.add(percept);
                }
            }
        }
        
        return relevantPercepts;
    }
    
    public double getRememberDuration(){
        return this.rememberDuration;
    }
    
    
    
    public double getRememberDecrement(){
        return this.rememberDecrement;
    }
    
    
    
    public int getMemoryCapacity() {
        return totalMemoryCapacity;
    }
    
    private Map<String,List<Percept>> mountCurrentSituation(){
        Map<String,List<Percept>> currentSituation = new HashMap<>();
        
        // ATTENTION //
        currentSituation.putAll(this.attentionPercepts);
        
        
        //REASONER //
        for (Map.Entry<String, Map<Percept,Double>> entry : this.reasonerPercepts.entrySet()) {
            String category = entry.getKey();
            Map<Percept, Double> perceptsOfCategoryMap = entry.getValue();
            
            if (currentSituation.containsKey(category)) {
                List<Percept> perceptsOfCategory = currentSituation.get(category);
                for (Percept p : perceptsOfCategoryMap.keySet()) {
                    if (!perceptsOfCategory.contains(p)) {
                        perceptsOfCategory.add(p);
                    }
                }
            } else{
                List<Percept> perceptsOfCategory = new ArrayList<>();
                perceptsOfCategory.addAll(perceptsOfCategoryMap.keySet());
                currentSituation.put(category, perceptsOfCategory);
            }
            
        }
        
        // REMEMBERED //
        
        for (List<Remember> rbList : this.remembers.values()) {
            for (Remember rb : rbList) {
                for (Map.Entry<String, List<Percept>> entry : rb.getRelevantPercepts().entrySet()) {
                    String category = entry.getKey();
                    List<Percept> rememberedPerceptsList = entry.getValue();

                    if (currentSituation.containsKey(category)) {
                        List<Percept> perceptsOfCategory = currentSituation.get(category);
                        for (Percept rememberedPercept : rememberedPerceptsList) {
                            if (!perceptsOfCategory.contains(rememberedPercept)) {
                                perceptsOfCategory.add(rememberedPercept);
                            }
                        }
                    } else{
                        List<Percept> perceptsOfCategory = new ArrayList<>();
                        perceptsOfCategory.addAll(rememberedPerceptsList);
                        currentSituation.put(category, perceptsOfCategory);
                    }
                }
            }
            
        }
        
        return currentSituation;
    }
    
    private class LastFactorEntry{
        public DecisionFactor factor;
        public double value;
        public String type;

        public LastFactorEntry(DecisionFactor factor, double value, String type) {
            this.factor = factor;
            this.value = value;
            this.type = type;
        }
    }
    
    private LastFactorEntry getLastFactor(List<Entry<Drive,Double>> drivesValuesOrdered,List<Entry<BiasDecisionFactor,Double>> biasFactorsValuesOrdered){
        LastFactorEntry lastDriveEntry;
        LastFactorEntry lastBiasDecisionFactorEntry;
        
        if (drivesValuesOrdered.isEmpty()) { //if drives empty, biasDecisionFactor isn't empty necessary.
            lastBiasDecisionFactorEntry = new LastFactorEntry(biasFactorsValuesOrdered.get(biasFactorsValuesOrdered.size()-1).getKey(), biasFactorsValuesOrdered.get(biasFactorsValuesOrdered.size()-1).getValue(), BiasDecisionFactor.class.getName());
            return lastBiasDecisionFactorEntry;
        }
        
        if (biasFactorsValuesOrdered.isEmpty()) {
            lastDriveEntry = new LastFactorEntry(drivesValuesOrdered.get(drivesValuesOrdered.size()-1).getKey(), drivesValuesOrdered.get(drivesValuesOrdered.size()-1).getValue(),Drive.class.getName());
            return lastDriveEntry;
        }
        
        lastDriveEntry = new LastFactorEntry(drivesValuesOrdered.get(drivesValuesOrdered.size()-1).getKey(), drivesValuesOrdered.get(drivesValuesOrdered.size()-1).getValue(),Drive.class.getName());
        lastBiasDecisionFactorEntry = new LastFactorEntry(biasFactorsValuesOrdered.get(biasFactorsValuesOrdered.size()-1).getKey(), biasFactorsValuesOrdered.get(biasFactorsValuesOrdered.size()-1).getValue(),BiasDecisionFactor.class.getName());
        
        if (lastDriveEntry.value < lastBiasDecisionFactorEntry.value) {
            return lastDriveEntry;
        }else{
            return lastBiasDecisionFactorEntry;
        }
    }
    
    private void removeLastFactor(LastFactorEntry lastFactor,List<Entry<Drive,Double>> drivesValuesOrdered,List<Entry<BiasDecisionFactor,Double>> biasFactorsValuesOrdered){
        
        synchronized(this.rememberMO){
            this.remembers.remove(lastFactor.factor);
            this.rememberMO.setI(this.remembers);
        }
        
        if (lastFactor.type.equals(BiasDecisionFactor.class.getName())) {
            biasFactorsValuesOrdered.remove(biasFactorsValuesOrdered.size()-1);
        } else{ //it is Drive
            drivesValuesOrdered.remove(drivesValuesOrdered.size()-1);
        }
    }
    
    private int countTotalOfPerceptsInRememberMO(){
        
        int total = 0;
        
        if (this.remembers != null) {
            for (List<Remember> rememberList : this.remembers.values()) {
                if (rememberList != null) {
                    for (Remember r : rememberList) {
                        total+= r.getTotalOfPerceptsInRemember();
                    }
                }   
            }
        }
        return total;
    }
    
    private void insertInRememberMO(Drive drive, List<Entry<Drive,Double>> drivesValuesOrdered, List<Entry<BiasDecisionFactor,Double>> biasFactorsValuesOrdered){
        
        List<Remember> newRemembers = null;
        
        if (countTotalOfPerceptsInRememberMO() == this.getMemoryCapacity()) { //full memory, try remove the last factor
            LastFactorEntry lastFactorEntry = this.getLastFactor(drivesValuesOrdered, biasFactorsValuesOrdered);
            if (!drive.equals(lastFactorEntry.factor)) { 
                this.removeLastFactor(lastFactorEntry, drivesValuesOrdered, biasFactorsValuesOrdered);
            }
            
            if (countTotalOfPerceptsInRememberMO() < this.getMemoryCapacity()) { //if the last factor is removed.
                newRemembers = createRemembersToDrive(drive);
            }
            
        } else{
            newRemembers = createRemembersToDrive(drive);
        }
        
        if (newRemembers!=null) {
            for (Remember r : newRemembers) {
                if (countTotalOfPerceptsInRememberMO() == this.getMemoryCapacity()){ //full memory, try remove the last factor
                    LastFactorEntry lastFactorEntry = this.getLastFactor(drivesValuesOrdered, biasFactorsValuesOrdered);
                    if (!drive.equals(lastFactorEntry.factor)) { 
                        this.removeLastFactor(lastFactorEntry, drivesValuesOrdered, biasFactorsValuesOrdered);
                    }
                } 

                if (countTotalOfPerceptsInRememberMO() < this.getMemoryCapacity()){ //if the last factor is removed.
                    List<Remember> remembersToDrive = remembers.get(drive);
                    if (remembersToDrive == null) {
                        remembersToDrive =  new ArrayList<>();
                        remembers.put(drive, remembersToDrive);
                    }
                    remembersToDrive.add(r);
                }
            }
        }
        
        
    }
    
    private void removeDeletedPerceptsFromRemembers(){
        
        List<Percept> toDeletePerceptsFromMemory = new CopyOnWriteArrayList( (List<Percept>) toDeleteLongMO.getI() );
        
        synchronized(this.rememberMO){
            this.remembers = (Map<DecisionFactor, List<Remember>>) this.rememberMO.getI();
            Map<DecisionFactor, List<Remember>> remembersBkp = this.deepCopyRememberMap(this.remembers);
            
            for (Map.Entry<DecisionFactor, List<Remember>> entry : remembersBkp.entrySet()) {
                
                DecisionFactor factor = entry.getKey();
                List<Remember> remembersOfFactorBkp = entry.getValue();
                
                for (int i = 0; i < remembersOfFactorBkp.size(); i++) {
                    List<Remember> remembersOfFactor = this.remembers.get(factor);
                    Remember rb = remembersOfFactor.get(i);
                    
                    Map<String, List<Percept>> rememberedPercepts = rb.getRelevantPercepts();
                
                    for (Percept p: toDeletePerceptsFromMemory) {
                        Map<String,List<String>> relevantPerceptCategoriesMap = rb.getCurrentAff().getRelevantPerceptsCategories();
                       
                        Iterator<String> it = relevantPerceptCategoriesMap.keySet().iterator();
                        boolean finded = false;
                        String category = null;
                        
                        while (it.hasNext() && !finded) {
                            category = it.next();
                            List<String> relevantPerceptsCategories = relevantPerceptCategoriesMap.get(category);
                            if (relevantPerceptsCategories!= null && relevantPerceptsCategories.contains(p.getCategory())) {
                                finded = true;
                            }
                        }
                        
                        if (finded) {
                            List<Percept> rememberedPerceptsOfCategory = rememberedPercepts.get(category); //the category of percept is in rememberedPercepts? 

                            if (rememberedPerceptsOfCategory != null && rememberedPerceptsOfCategory.contains(p)) {
                                rememberedPerceptsOfCategory.remove(p);
                                //System.out.println("Percept " + p.getName() + "removed from RememberMO");
                                if (rememberedPerceptsOfCategory.isEmpty()) { //if list of this percept type was empty, delete it
                                    rememberedPercepts.remove(p.getCategory());
                                    if (rememberedPercepts.isEmpty()) { //if map was empty, delete it
                                        this.remembers.remove(entry.getKey());
                                    }
                                }
                            }  
                        }
                        
                    }
                }
                
                
            }
            
            this.rememberMO.setI(this.remembers);
            
        }
        
        this.toDeleteLongMO.setI(new ArrayList<>());
    
    }
    
    private void addRememberPerceptsToWorkingMO(){
        
        if(!this.remembers.isEmpty()){
            
            synchronized(this.workingMO){
                Map<String,Map<String,List<Percept>>> workingMemory = (Map<String,Map<String,List<Percept>>>) this.workingMO.getI();
                Map<String,List<Percept>> rememberedPerceptsInWMO = workingMemory.get(this.rememberCategoryInWMO);

                if (rememberedPerceptsInWMO == null) {
                    rememberedPerceptsInWMO = new HashMap<>();
                    workingMemory.put(this.rememberCategoryInWMO, rememberedPerceptsInWMO);
                } else{
                    rememberedPerceptsInWMO.clear(); // ou voce apaga toda esta memoria ou e necessario uma function para apagar os percepts deletados.
                }
                
                for (List<Remember> rbList : this.remembers.values()) {
                    for (Remember rb : rbList) {
                        for (Map.Entry<String, List<Percept>> entry : rb.getRelevantPercepts().entrySet()) {
                            String category = entry.getKey();
                            List<Percept> rememberedPerceptsList = entry.getValue();

                            if (rememberedPerceptsInWMO.containsKey(category)) {
                                List<Percept> perceptsOfCategory = rememberedPerceptsInWMO.get(category);
                                for (Percept rememberedPercept : new ArrayList<>(rememberedPerceptsList)) {
                                    if (!perceptsOfCategory.contains(rememberedPercept)) {
                                        perceptsOfCategory.add(rememberedPercept);
                                    }
                                }
                            } else{
                                List<Percept> perceptsOfCategory = new ArrayList<>();
                                perceptsOfCategory.addAll(rememberedPerceptsList);
                                rememberedPerceptsInWMO.put(category, perceptsOfCategory);
                            }
                        }
                    }
                }  
            }
        }
    }
    
    public Map<DecisionFactor, List<Remember>> deepCopyRememberMap(Map<DecisionFactor, List<Remember>> remembers){
        synchronized(remembers){
            Map<DecisionFactor, List<Remember>> remembersBkp = new HashMap<>();
            for(Map.Entry<DecisionFactor, List<Remember>> entry : remembers.entrySet()){
                remembersBkp.put( entry.getKey(),new ArrayList<>(entry.getValue()) );
            }
            return remembersBkp;
        }
    }
    
    //////////////////////
    // OVERRIDE METHODS //
    //////////////////////
    
    @Override
    public void accessMemoryObjects() {
        this.longMO = (MemoryObject) this.getInput(MemoryObjectsNames.LONG_MO);
        this.driveMO = (MemoryObject) this.getInput(MemoryObjectsNames.DRIVE_MO);
        this.rememberMO = (MemoryObject) this.getInput(MemoryObjectsNames.REMEMBER_MO);
        this.biasDecisionFactorsMO = (MemoryObject) this.getInput(MemoryObjectsNames.BIAS_DECISION_FACTORS_MO);
        this.reasonerMO = (MemoryObject) this.getInput(MemoryObjectsNames.REASONER_MO);
        this.workingMO = (MemoryObject) this.getInput(MemoryObjectsNames.WORKING_MO);
        this.extractedAffordancesMO = (MemoryObject) this.getInput(MemoryObjectsNames.EXTRACTED_AFFORDANCES_MO);
        this.toDeleteLongMO = (MemoryObject) this.getInput(MemoryObjectsNames.TO_DELETE_LONG_MO);
        this.synchronizerMO = (MemoryObject) this.getInput(MemoryObjectsNames.SYNCHRONIZER_MO);
    }

    @Override
    public void calculateActivation() {
        
    }

    @Override
    public void proc() {
        
        synchronized(this.longMO){
            this.memoryPercepts = (Map<String,Map<Percept,Double>>) this.longMO.getI();
            this.memoryPercepts = AuxiliarMethods.deepCopyMemoryMap(this.memoryPercepts);
        }
        
        if (memoryPercepts.size() > 0) {
            
            synchronized(this.workingMO){
                Map<String, Map<String,List<Percept>>> attentionPerceptsFromWorkingMemory =  (Map<String, Map<String,List<Percept>> >) this.workingMO.getI();
                this.attentionPercepts = AuxiliarMethods.deepCopyAttentionMap(attentionPerceptsFromWorkingMemory.get("ATTENTION"));
            }
        
            this.drives = new CopyOnWriteArrayList( (List<Drive>) this.driveMO.getI() );

            this.biasFactors = new CopyOnWriteArrayList((List<BiasDecisionFactor>) this.biasDecisionFactorsMO.getI() );
            
            synchronized(this.reasonerMO){
                this.reasonerPercepts = (Map<String,Map<Percept,Double>>) this.reasonerMO.getI();
                this.reasonerPercepts = AuxiliarMethods.deepCopyMemoryMap(this.reasonerPercepts);
            }

            this.remembers = (Map<DecisionFactor, List<Remember>>) this.rememberMO.getI(); //don´t necessary save a actual version of this MO;

            this.situation = this.mountCurrentSituation();
            
            if (this.remembers.isEmpty() || (!this.remembers.isEmpty() && (this.AffordanceExtractorTimeStamp == -Long.MIN_VALUE || this.extractedAffordancesMO.getTimestamp() > this.AffordanceExtractorTimeStamp)) ){ //all remembers were read.
               
                this.computeDrivesActivations(); // mount a map for drives to order;
                this.computeBiasDecisionFactorsActivations(); // mount a map for biasDecisionFactors to order;
                List<Entry<Drive,Double>> drivesValuesOrdered = AuxiliarMethods.descSortEntriesByValues(this.drivesActivations);
                List<Entry<BiasDecisionFactor,Double>> biasFactorsValuesOrdered = AuxiliarMethods.descSortEntriesByValues(this.biasFactorsActivations);
               
                //synchronized(this.extractedAffordancesMO){
                    //this.extractedAffordances = (List<ExtractedAffordance>) this.extractedAffordancesMO.getI();
                    
                    List<ExtractedAffordance> newExtractedAffordances = new CopyOnWriteArrayList((List<ExtractedAffordance>) this.extractedAffordancesMO.getI() );
                    
                    //if (newExtractedAffordances.size()>0 && !newExtractedAffordances.equals(this.extractedAffordances)) { //only when there are extracted affordances and when extracted aff is different of old extracted affordances.
                        this.extractedAffordances = newExtractedAffordances;
                        decrementNotRemembers();
                        decrementRemembers();
                        
                        synchronized(this.rememberMO){ //necessary to synchronize with affordanceExtractorCodelet
                            for (Entry<Drive,Double> entry : drivesValuesOrdered) {

                                Drive drive = entry.getKey();
                                updateRemembersToDrive(drive);
                                insertInRememberMO(drive,drivesValuesOrdered,biasFactorsValuesOrdered);
                            }
                            
                            /*
                            System.out.println("#########NEW REMEMBER CYCLE###########");
                            
                            for (List<Remember> rbList : this.remembers.values()) {
                                for (Remember rb : rbList) {
                                    for (List<Percept> percepts : rb.getRelevantPercepts().values()) {
                                        for (Percept p: percepts) {
                                            if (p.getCategory().contains("FOOD") || p.getCategory().equals("JEWEL")) {
                                                System.out.println(p.getPropertyByType("NAMEID=").getValue());
                                            }else{
                                                System.out.println(p.getCategory());
                                            }
                                        }
                                    }
                                }
                            }
                            
                            System.out.println("");
                            */
                            this.AffordanceExtractorTimeStamp = this.extractedAffordancesMO.getTimestamp();
                        }
                    //}
                    
                //}

                /*
                for (Entry<BiasDecisionFactor,Double> entry : biasFactorsValuesOrdered) {

                    BiasDecisionFactor factor = entry.getKey();

                    if (remembers.containsKey(factor)) { //if factor already in rememberMO
                        this.createRememberToBiasDecisionFactors(factor);
                    } else{
                        if (remembers.size() < this.getMemoryCapacity()) { //if rememberMO has capacity to new remembers;
                            this.createRememberToBiasDecisionFactors(factor);
                        } else{
                            LastFactorEntry lastFactorEntry = this.getLastFactor(drivesValuesOrdered, biasFactorsValuesOrdered);
                            if (!factor.equals(lastFactorEntry.factor)) {
                                this.removeLastFactor(lastFactorEntry, drivesValuesOrdered, biasFactorsValuesOrdered);
                                this.createRememberToBiasDecisionFactors(factor);
                            }
                        }
                    }
                }
                */
            }
        }
        
        removeDeletedPerceptsFromRemembers();
        addRememberPerceptsToWorkingMO();
            
        AuxiliarMethods.synchronize(super.getName());
    }

    
    
}
