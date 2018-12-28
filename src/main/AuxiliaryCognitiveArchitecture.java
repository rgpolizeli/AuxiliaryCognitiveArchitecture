/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import actionSelection.ActionSelectionMechanism;
import actionSelection.ConsummatoryAffordanceType;
import attention.AttentionCodelet;
import executor.ExecutorHandleCodelet;
import memory.Remember;
import memory.RememberCodelet;
import perception.Percept;
import br.unicamp.cst.representation.owrl.Property;
import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.Memory;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.core.entities.Mind;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import memory.MemoryCodelet;
import br.unicamp.cst.motivational.Drive;
import motivation.DriveHandle;
import motivation.DrivesHandlerCodelet;

/**
 *
 * @author rgpolizeli
 */
public class AuxiliaryCognitiveArchitecture {
    
    private Mind m;
    private ActionSelectionMechanism asbac;
    
    private MemoryObject perceptionMO;
    private MemoryObject shortMO;
    private MemoryObject longMO;
    private MemoryObject reasonerMO;
    private MemoryObject createdPerceptsMO;
    private MemoryObject rememberMO;
    private MemoryObject drivesHandlesMO;
    private MemoryObject toModifyPerceptionMO;
    private MemoryObject executorHandleMO;
    private MemoryObject executorsMO;
    private MemoryObject executorParametersMO;
    private MemoryObject actuatorsMO;

    private int attentionMemoryCapacity = -1;
    private int shortMemoryCapacity = -1;
    private int reasonerMemoryCapacity = -1;
    private int longMemoryCapacity = -1;
    private int rememberCapacity = -1;
    
    private List<Property> salienceProperties;
    private double salienceBias = -1.0;
    
    private Map<String, Codelet> executors;
    private Map<String, String> actuators;
    
    private double memorizerThreshold = -1.0; 
    private double longMemoryMaxActivation = -1.0;
    private double longMemoryMinActivation = -1.0;
    private double longMemoryDeleteThreshold = -1.0;
    private double longMemoryReplaceThreshold = -1.0;
    private double longMemoryIncrementPerCycle = -1.0;
    private double longMemoryDecrementPerCycle = -1.0;
    
    private double reasonerMaxActivation = -1.0;
    private double reasonerMinActivation = -1.0;
    private double reasonerDeleteThreshold = -1.0;
    private double reasonerReplaceThreshold = -1.0; 
    private double reasonerIncrementPerCycle = -1.0;
    private double reasonerDecrementPerCycle = -1.0;
    
    private int rememberForgetThreshold = -1;
    private int rememberDuration = -1;
    private int rememberDecrement = -1;
    
    private double shortMemoryMaxActivation = -1.0;
    private double shortMemoryMinActivation = -1.0;
    private double shortMemoryDeleteThreshold = -1.0;
    private double shortMemoryReplaceThreshold = -1.0; 
    private double shortMemoryIncrementPerCycle = -1.0;
    private double shortMemoryDecrementPerCycle = -1.0;
    
    private String selfPerceptCategory;
    private double relevantPerceptsIncrement = -1.0;
    
    private List<DriveHandle> drivesHandles;
    
    public AuxiliaryCognitiveArchitecture(Mind m) {
        this.m = m;
        this.asbac = new ActionSelectionMechanism(m);
        this.createCognitiveArchitectureMOs();
    }
    
    public void setSelfPerceptCategory(String selfPcptCategory){
        this.selfPerceptCategory = selfPcptCategory;
    }
    
    public void setDriveHandleParameters(List<DriveHandle> drivesHandles, double relevantPerceptsIncrement){
        this.drivesHandles = drivesHandles;
        this.relevantPerceptsIncrement = relevantPerceptsIncrement;
    }
    
    public void setMemoriesSize(int attentionMemoryCapacity, int shortMemoryCapacity, int reasonerMemoryCapacity, int longMemoryCapacity, int rememberCapacity){
        this.attentionMemoryCapacity = attentionMemoryCapacity;
        this.shortMemoryCapacity = shortMemoryCapacity;
        this.reasonerMemoryCapacity = reasonerMemoryCapacity;
        this.longMemoryCapacity = longMemoryCapacity;
        this.rememberCapacity = rememberCapacity;
    }
    
    public void setAttentionParameters(List<Property> salienceProperties, double salienceBias){
        this.salienceProperties = salienceProperties;
        this.salienceBias = salienceBias;
    }
    
    public void setActionSelectionParameters(List<ConsummatoryAffordanceType> consummatoryAffordanceTypes, double maxAffordanceActivation, double minAffordanceActivation, double activationThreshold, double decrementPerCount){
        this.asbac.setCountParameters(maxAffordanceActivation, minAffordanceActivation, activationThreshold, decrementPerCount);
        this.asbac.setConsummatoryAffordances(consummatoryAffordanceTypes);   
    }
    
    public void setExecutors(Map<String, Codelet> executors){
        this.executors = executors;
        this.executorsMO.setI(this.executors);
    }
    
    public void setActuators(Map<String, String> actuators){
        this.actuators = actuators;
        this.actuatorsMO.setI(this.actuators);
    }
    
    public void setLongMemoryParameters(double memorizerThreshold, double maxActivation, double minActivation, double deleteThreshold, double replaceThreshold, double incrementPerCycle, double decrementPerCycle){
        this.memorizerThreshold = memorizerThreshold;
        this.longMemoryMaxActivation = maxActivation;
        this.longMemoryMinActivation = minActivation;
        this.longMemoryDeleteThreshold = deleteThreshold;
        this.longMemoryReplaceThreshold = replaceThreshold;
        this.longMemoryIncrementPerCycle = incrementPerCycle;
        this.longMemoryDecrementPerCycle = decrementPerCycle;
    }
    
    public void setReasonerParameters(double maxActivation, double minActivation, double deleteThreshold, double replaceThreshold, double incrementPerCycle, double decrementPerCycle){
        this.reasonerMaxActivation = maxActivation;
        this.reasonerMinActivation = minActivation;
        this.reasonerDeleteThreshold = deleteThreshold;
        this.reasonerReplaceThreshold = replaceThreshold;
        this.reasonerIncrementPerCycle = incrementPerCycle;
        this.reasonerDecrementPerCycle = decrementPerCycle;
    }
    
    public void setRememberParameters(int rememberDuration, int rememberDecrement, int rememberForgetThreshold){
        this.rememberDuration = rememberDuration;
        this.rememberDecrement = rememberDecrement;
        this.rememberForgetThreshold = rememberForgetThreshold; 
    }
    
    public void setShortMemoryParameters(double maxActivation, double minActivation, double deleteThreshold, double replaceThreshold, double incrementPerCycle, double decrementPerCycle){
        this.shortMemoryMaxActivation = maxActivation;
        this.shortMemoryMinActivation = minActivation;
        this.shortMemoryDeleteThreshold = deleteThreshold;
        this.shortMemoryReplaceThreshold = replaceThreshold;
        this.shortMemoryIncrementPerCycle = incrementPerCycle;
        this.shortMemoryDecrementPerCycle = decrementPerCycle;
    }
    
    public void createCognitiveArchitectureMOs(){
        Map<String,List<Percept>> perceptionPercepts = new HashMap<>();
        this.perceptionMO = m.createMemoryObject(MemoriesNames.PERCEPTION_MO, perceptionPercepts);
        
        Map<String, Map<Percept, Double>> shortMemoryMap = new HashMap();
        this.shortMO = m.createMemoryObject(MemoriesNames.SHORT_MO, shortMemoryMap);
        
        Map<String, Map<Percept, Double>> longMemoryMap = new HashMap();
        this.longMO = m.createMemoryObject(MemoriesNames.LONG_MO, longMemoryMap);
        
        Map<String, List<Percept>> createdPercepts = new HashMap<>();
        this.createdPerceptsMO = m.createMemoryObject(MemoriesNames.CREATED_PERCEPTS_MO, createdPercepts);
        
        Map<String,Map<Percept,Double>> reasonerPercepts = new HashMap<>();
        this.reasonerMO = m.createMemoryObject(MemoriesNames.REASONER_MO, reasonerPercepts);
        
        Map<Drive, List<Remember>> remembers = new HashMap<>();
        this.rememberMO = m.createMemoryObject(MemoriesNames.REMEMBER_MO, remembers);
        
        List<DriveHandle> drivesHandles = new ArrayList<>();
        this.drivesHandlesMO = m.createMemoryObject(MemoriesNames.DRIVES_HANDLE_MO, drivesHandles);
        
        List<Percept> toModifyPerception = new ArrayList<>();
        this.toModifyPerceptionMO = m.createMemoryObject(MemoriesNames.TO_MODIFY_PERCEPTION_MO, toModifyPerception);
        
        this.actuatorsMO = m.createMemoryObject(MemoriesNames.ACTUATORS_MO);
        this.executorHandleMO = m.createMemoryObject(MemoriesNames.EXECUTOR_HANDLE_MO, Boolean.FALSE);
        this.executorParametersMO = m.createMemoryObject(MemoriesNames.EXECUTOR_PARAMETERS_MO);
        this.executorsMO = m.createMemoryObject(MemoriesNames.EXECUTORS_MO);
    }
    
    public void createProxyMO(Class proxyClass, Object proxy){
        m.createMemoryObject(MemoriesNames.PROXY_MO, proxyClass.cast(proxy));
    }
    
    private void validParameters(){
     
        if (this.attentionMemoryCapacity == -1) {
            throw new IllegalArgumentException();
        }
        
        if (this.shortMemoryCapacity == -1) {
            throw new IllegalArgumentException();
        }
        
        if (this.reasonerMemoryCapacity == -1) {
            throw new IllegalArgumentException();
        }
        
        if (this.longMemoryCapacity == -1) {
            throw new IllegalArgumentException();
        }
        
        if (this.rememberCapacity == -1) {
            throw new IllegalArgumentException();
        }
        
        if (this.salienceProperties == null) {
            throw new IllegalArgumentException();
        }
        
        if (this.salienceBias == -1.0) {
            throw new IllegalArgumentException();
        }
    
        if (this.executors == null) {
            throw new IllegalArgumentException();
        }
    
        if (this.memorizerThreshold == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.longMemoryMaxActivation == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.longMemoryMinActivation == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.longMemoryDeleteThreshold == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.longMemoryReplaceThreshold == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.longMemoryIncrementPerCycle == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.longMemoryDecrementPerCycle == -1.0) {
            throw new IllegalArgumentException();
        }
        
        
        if (this.reasonerMaxActivation == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.reasonerMinActivation == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.reasonerDeleteThreshold == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.reasonerReplaceThreshold == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.reasonerIncrementPerCycle == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.reasonerDecrementPerCycle == -1.0) {
            throw new IllegalArgumentException();
        }
    
        if (this.rememberForgetThreshold == -1) {
            throw new IllegalArgumentException();
        }
        
        if (this.rememberDuration == -1) {
            throw new IllegalArgumentException();
        }
        
        if (this.rememberDecrement == -1) {
            throw new IllegalArgumentException();
        }
        
        if (this.shortMemoryMaxActivation == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.shortMemoryMinActivation == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.shortMemoryDeleteThreshold == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.shortMemoryReplaceThreshold == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.shortMemoryIncrementPerCycle == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.shortMemoryDecrementPerCycle == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.selfPerceptCategory == null) {
            throw new IllegalArgumentException();
        }
        
        if (this.relevantPerceptsIncrement == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.drivesHandles == null) {
            throw new IllegalArgumentException();
        }
    }
    
    public void createCodelets(){
        
        validParameters();
        
        AttentionCodelet attentionCodelet = new AttentionCodelet(this.salienceProperties, this.salienceBias, this.attentionMemoryCapacity);
        attentionCodelet.addInput(this.shortMO);
        attentionCodelet.addInput(getMemoryByName(MemoriesNames.WORKING_MO));
        attentionCodelet.addInput(this.drivesHandlesMO);
        attentionCodelet.addInput(getMemoryByName(MemoriesNames.SYNCHRONIZER_MO));
        attentionCodelet.setName("AttentionCodelet");
        attentionCodelet.setTimeStep(0);
        this.m.insertCodelet(attentionCodelet);
        
        ExecutorHandleCodelet executorHandleCodelet = new ExecutorHandleCodelet();
        executorHandleCodelet.addInput(this.executorsMO);
        executorHandleCodelet.addInput(this.executorHandleMO);
        executorHandleCodelet.addInput(this.reasonerMO);
        executorHandleCodelet.addInput(getMemoryByName(MemoriesNames.WORKING_MO));
        executorHandleCodelet.addInput(getMemoryByName(MemoriesNames.ACTIVATED_AFFORDANCE_MO));
        executorHandleCodelet.addInput(getMemoryByName(MemoriesNames.SYNCHRONIZER_MO));
        
        executorHandleCodelet.addOutput(this.executorParametersMO);
        executorHandleCodelet.setName("ExecutorHandleCodelet");
        executorHandleCodelet.setTimeStep(0);
        this.m.insertCodelet(executorHandleCodelet);
        Logger.getLogger(ExecutorHandleCodelet.class.getName()).setLevel(Level.SEVERE);
        
        MemoryCodelet memoryCodelet = new MemoryCodelet(longMemoryMaxActivation,longMemoryMinActivation,
                shortMemoryCapacity, shortMemoryDeleteThreshold, shortMemoryReplaceThreshold, shortMemoryIncrementPerCycle, shortMemoryDecrementPerCycle, 
                longMemoryCapacity, memorizerThreshold, longMemoryDeleteThreshold, longMemoryReplaceThreshold, longMemoryIncrementPerCycle, longMemoryDecrementPerCycle, 
                reasonerMemoryCapacity, reasonerDeleteThreshold, reasonerReplaceThreshold, reasonerIncrementPerCycle, reasonerDecrementPerCycle
        );
        memoryCodelet.addInput(this.perceptionMO);
        memoryCodelet.addInput(this.shortMO);
        memoryCodelet.addInput(this.longMO);
        memoryCodelet.addInput(this.reasonerMO);
        memoryCodelet.addInput(this.createdPerceptsMO);
        memoryCodelet.addInput(this.toModifyPerceptionMO);
        memoryCodelet.addInput(getMemoryByName(MemoriesNames.SYNCHRONIZER_MO));
        memoryCodelet.setName("MemoryCodelet");
        memoryCodelet.setTimeStep(0);
        this.m.insertCodelet(memoryCodelet);
        Logger.getLogger(MemoryCodelet.class.getName()).setLevel(Level.SEVERE);

        RememberCodelet rememberCodelet = new RememberCodelet(this.rememberCapacity, 15, this.rememberDuration, this.rememberDecrement, this.rememberForgetThreshold);
        rememberCodelet.addInput(getMemoryByName(MemoriesNames.WORKING_MO));
        rememberCodelet.addInput(this.longMO);
        rememberCodelet.addInput(this.drivesHandlesMO);
        rememberCodelet.addInput(this.reasonerMO);
        rememberCodelet.addInput(this.rememberMO);
        rememberCodelet.addInput(getMemoryByName(MemoriesNames.EXTRACTED_AFFORDANCES_MO));
        rememberCodelet.addInput(getMemoryByName(MemoriesNames.SYNCHRONIZER_MO));
        rememberCodelet.setName("RememberCodelet");
        rememberCodelet.setTimeStep(0);
        this.m.insertCodelet(rememberCodelet);
       
        DrivesHandlerCodelet drivesHandlerCodelet = new DrivesHandlerCodelet(this.selfPerceptCategory, this.relevantPerceptsIncrement);
        drivesHandlerCodelet.addInput(this.shortMO);
        drivesHandlerCodelet.addInput(getMemoryByName(MemoriesNames.SYNCHRONIZER_MO));
        drivesHandlerCodelet.addInput(this.drivesHandlesMO);
        drivesHandlerCodelet.setName("DrivesHandlerCodelet");
        drivesHandlerCodelet.setTimeStep(0);
        this.m.insertCodelet(drivesHandlerCodelet);
        Logger.getLogger(DrivesHandlerCodelet.class.getName()).setLevel(Level.SEVERE);
        
        this.drivesHandlesMO.setI(this.drivesHandles);
        
        Logger.getLogger(SynchronizationCognitiveArchitecture.class.getName()).setLevel(Level.SEVERE);
        
        this.asbac.createCodelets();
    }
    
    private Memory getMemoryByName(String memoryName){
       return this.m.getRawMemory().getAllOfType(memoryName).get(0);
    }
    
    public void createSynchronizationMechanism(){
        this.asbac.createSynchronizationMechanism();
    }
    
    public ActionSelectionMechanism getAsbac(){
        return this.asbac;
    }
}
