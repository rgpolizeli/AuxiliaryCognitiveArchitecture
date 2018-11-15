/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import actionSelection.ActionSelectionMechanism;
import attention.AttentionCodelet;
import reasoner.ReasonerCodelet;
import reasoner.Operation;
import executor.ExecutorInfo;
import executor.ExecutorHandleCodelet;
import memory.LongMemoryCodelet;
import memory.Remember;
import memory.RememberCodelet;
import perception.Percept;
import perception.Property;
import memory.ShortMemoryCodelet;
import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.Memory;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.core.entities.Mind;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import motivation.BiasDecisionFactor;
import motivation.DecisionFactor;
import motivation.Drive;
import motivation.DriveHandleCodelet;

/**
 *
 * @author ricardo
 */
public class AuxiliaryCognitiveArchitecture {
    
    private Mind m;
    private ActionSelectionMechanism asbac;
    
    private MemoryObject perceptionMO;
    private MemoryObject shortMO;
    private MemoryObject longMO;
    private MemoryObject workingMO;
    private MemoryObject driveMO;
    private MemoryObject extractedAffordanceMO;
    private MemoryObject biasDecisionFactorsMO;
    private MemoryObject activatedAffordanceMO;
    private MemoryObject reasonerMO;
    private MemoryObject operationsMO;
    private MemoryObject rememberMO;
    private MemoryObject toDeleteMO;
    private MemoryObject toDeleteLongMO;
    private MemoryObject toModifyPerceptionMO;
    private MemoryObject executorHandleMO;
    private MemoryObject executorsMO;
    private MemoryObject executorParametersMO;
    private MemoryObject actuatorsMO;
    private MemoryObject synchronizerMO;

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
    private double memorizerMaxActivation = -1.0;
    private double memorizerMinActivation = -1.0;
    private double memorizerDeleteThreshold = -1.0;
    private double memorizerReplaceThreshold = -1.0;
    private double memorizerIncrementPerCycle = -1.0;
    private double memorizerDecrementPerCycle = -1.0;
    
    private double reasonerMaxActivation = -1.0;
    private double reasonerMinActivation = -1.0;
    private double reasonerDeleteThreshold = -1.0;
    private double reasonerReplaceThreshold = -1.0; 
    private double reasonerIncrementPerCycle = -1.0;
    private double reasonerDecrementPerCycle = -1.0;
    private Map<String, ExecutorInfo> operationsMap;
    
    private int rememberForgetThreshold = -1;
    private int rememberDuration = -1;
    private int rememberDecrement = -1;
    
    private double perceptionMaxActivation = -1.0;
    private double perceptionMinActivation = -1.0;
    private double perceptionDeleteThreshold = -1.0;
    private double perceptionReplaceThreshold = -1.0; 
    private double perceptionIncrementPerCycle = -1.0;
    private double perceptionDecrementPerCycle = -1.0;
    
    private String selfPerceptCategory;
    private double relevantPerceptsIncrement = -1.0;
    
    private List<Drive> drives;
    private List<BiasDecisionFactor> bias;
    
    public AuxiliaryCognitiveArchitecture(Mind m) {
        this.m = m;
        this.asbac = new ActionSelectionMechanism(m);
        this.createCognitiveArchitectureMOs();
    }
    
    public void setSelfPerceptCategory(String selfPcptCategory){
        this.selfPerceptCategory = selfPcptCategory;
    }
    
    public void setBiasDecisionFactors(List<BiasDecisionFactor> bias){
        this.bias = bias;
        this.biasDecisionFactorsMO.setI(this.bias);
    }
    
    public void setDriveHandleParameters(double relevantPerceptsIncrement){
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
    
    public void setActionSelectionParameters(List<Drive> drives, double maxAffordanceActivation, double minAffordanceActivation, double activationThreshold, double decrementPerCount){
        this.drives = drives;
        this.asbac.setCountParameters(maxAffordanceActivation, minAffordanceActivation, activationThreshold, decrementPerCount);
        this.asbac.setDrives(drives);
        
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
        this.memorizerMaxActivation = maxActivation;
        this.memorizerMinActivation = minActivation;
        this.memorizerDeleteThreshold = deleteThreshold;
        this.memorizerReplaceThreshold = replaceThreshold;
        this.memorizerIncrementPerCycle = incrementPerCycle;
        this.memorizerDecrementPerCycle = decrementPerCycle;
    }
    
    public void setReasonerParameters(Map<String, ExecutorInfo> operationsMap, double maxActivation, double minActivation, double deleteThreshold, double replaceThreshold, double incrementPerCycle, double decrementPerCycle){
        this.operationsMap = operationsMap;
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
        this.perceptionMaxActivation = maxActivation;
        this.perceptionMinActivation = minActivation;
        this.perceptionDeleteThreshold = deleteThreshold;
        this.perceptionReplaceThreshold = replaceThreshold;
        this.perceptionIncrementPerCycle = incrementPerCycle;
        this.perceptionDecrementPerCycle = decrementPerCycle;
    }
    
    public void createCognitiveArchitectureMOs(){
        Map<String,List<Percept>> perceptionPercepts = new HashMap<>();
        this.perceptionMO = m.createMemoryObject(MemoryObjectsNames.PERCEPTION_MO, perceptionPercepts);
        
        Map<String, Map<Percept, Double>> shortMemoryMap = new HashMap();
        this.shortMO = m.createMemoryObject(MemoryObjectsNames.SHORT_MO, shortMemoryMap);
        
        Map<String, Map<Percept, Double>> longMemoryMap = new HashMap();
        this.longMO = m.createMemoryObject(MemoryObjectsNames.LONG_MO, longMemoryMap);
        
        List<BiasDecisionFactor> bias = new ArrayList<>();
        this.biasDecisionFactorsMO = m.createMemoryObject(MemoryObjectsNames.BIAS_DECISION_FACTORS_MO, bias);
        
        Map<Operation, List<Percept>> reasonerPercepts = new HashMap<>();
        this.reasonerMO = m.createMemoryObject(MemoryObjectsNames.REASONER_MO, reasonerPercepts);
        
        List<Operation> reasonerOperations = new ArrayList<>();
        this.operationsMO = m.createMemoryObject(MemoryObjectsNames.OPERATIONS_MO, reasonerOperations);
        
        Map<DecisionFactor, List<Remember>> remembers = new HashMap<>();
        this.rememberMO = m.createMemoryObject(MemoryObjectsNames.REMEMBER_MO, remembers);
        
        Map<String,Map<String,List<Percept>>> toDeleteMemory = new HashMap<>();
        this.toDeleteMO = m.createMemoryObject(MemoryObjectsNames.TO_DELETE_MO, toDeleteMemory);
        
        List<Percept> toDeleteLongMemory = new ArrayList<>();
        this.toDeleteLongMO = m.createMemoryObject(MemoryObjectsNames.TO_DELETE_LONG_MO, toDeleteLongMemory);
        
        List<Percept> toModifyPerception = new ArrayList<>();
        this.toModifyPerceptionMO = m.createMemoryObject(MemoryObjectsNames.TO_MODIFY_PERCEPTION_MO, toModifyPerception);
        
        this.actuatorsMO = m.createMemoryObject(MemoryObjectsNames.ACTUATORS_MO);
        this.executorHandleMO = m.createMemoryObject(MemoryObjectsNames.EXECUTOR_HANDLE_MO, Boolean.FALSE);
        this.executorParametersMO = m.createMemoryObject(MemoryObjectsNames.EXECUTOR_PARAMETERS_MO);
        this.executorsMO = m.createMemoryObject(MemoryObjectsNames.EXECUTORS_MO);
    }
    
    public void createProxyMO(Class proxyClass, Object proxy){
        m.createMemoryObject(MemoryObjectsNames.PROXY_MO, proxyClass.cast(proxy));
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
        
        if (this.memorizerMaxActivation == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.memorizerMinActivation == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.memorizerDeleteThreshold == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.memorizerReplaceThreshold == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.memorizerIncrementPerCycle == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.memorizerDecrementPerCycle == -1.0) {
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
    
        if (this.operationsMap == null) {
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
        
        if (this.perceptionMaxActivation == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.perceptionMinActivation == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.perceptionDeleteThreshold == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.perceptionReplaceThreshold == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.perceptionIncrementPerCycle == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.perceptionDecrementPerCycle == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.selfPerceptCategory == null) {
            throw new IllegalArgumentException();
        }
        
        if (this.relevantPerceptsIncrement == -1.0) {
            throw new IllegalArgumentException();
        }
        
        if (this.drives == null) {
            throw new IllegalArgumentException();
        }
    }
    
    public void createCodelets(){
        
        validParameters();
        
        AttentionCodelet attentionCodelet = new AttentionCodelet(this.salienceProperties, this.salienceBias, this.attentionMemoryCapacity);
        attentionCodelet.addInput(this.shortMO);
        attentionCodelet.addInput(getMemoryByName(MemoryObjectsNames.WORKING_MO));
        attentionCodelet.addInput(getMemoryByName(MemoryObjectsNames.DRIVE_MO));
        attentionCodelet.addInput(getMemoryByName(MemoryObjectsNames.SYNCHRONIZER_MO));
        attentionCodelet.setName("AttentionCodelet");
        this.m.insertCodelet(attentionCodelet);
        
        ExecutorHandleCodelet executorHandleCodelet = new ExecutorHandleCodelet();
        executorHandleCodelet.addInput(this.executorsMO);
        executorHandleCodelet.addInput(this.executorHandleMO);
        executorHandleCodelet.addInput(getMemoryByName(MemoryObjectsNames.ACTIVATED_AFFORDANCE_MO));
        executorHandleCodelet.addInput(getMemoryByName(MemoryObjectsNames.SYNCHRONIZER_MO));
        
        executorHandleCodelet.addOutput(this.executorParametersMO);
        executorHandleCodelet.setName("ExecutorHandleCodelet");
        this.m.insertCodelet(executorHandleCodelet);
        
        LongMemoryCodelet memorizerCodelet = new LongMemoryCodelet(this.longMemoryCapacity, this.memorizerIncrementPerCycle, this.memorizerDecrementPerCycle, this.memorizerMaxActivation, this.memorizerMinActivation, this.memorizerThreshold, this.memorizerDeleteThreshold, this.memorizerReplaceThreshold);
        memorizerCodelet.addInput(this.longMO);
        memorizerCodelet.addInput(this.shortMO);
        memorizerCodelet.addInput(this.reasonerMO);
        memorizerCodelet.addInput(this.toDeleteMO);
        memorizerCodelet.addInput(this.toDeleteLongMO);
        memorizerCodelet.addInput(getMemoryByName(MemoryObjectsNames.SYNCHRONIZER_MO));
        memorizerCodelet.setName("MemorizerCodelet");
        this.m.insertCodelet(memorizerCodelet);
        
        ReasonerCodelet reasonerCodelet = new ReasonerCodelet(this.m, this.operationsMap, this.reasonerMemoryCapacity, this.reasonerMaxActivation, this.reasonerMinActivation, this.reasonerDeleteThreshold, this.reasonerReplaceThreshold, this.reasonerIncrementPerCycle, this.reasonerDecrementPerCycle);
        reasonerCodelet.addInput(getMemoryByName(MemoryObjectsNames.DRIVE_MO));
        reasonerCodelet.addInput(this.operationsMO);
        reasonerCodelet.addInput(this.reasonerMO);
        reasonerCodelet.addInput(getMemoryByName(MemoryObjectsNames.WORKING_MO));
        reasonerCodelet.addInput(this.toDeleteMO);
        reasonerCodelet.addInput(getMemoryByName(MemoryObjectsNames.ACTIVATED_AFFORDANCE_MO));
        reasonerCodelet.addInput(getMemoryByName(MemoryObjectsNames.SYNCHRONIZER_MO));
        reasonerCodelet.setName("ReasonerCodelet");
        this.m.insertCodelet(reasonerCodelet);
        
        RememberCodelet rememberCodelet = new RememberCodelet(this.rememberCapacity, 15, this.rememberDuration, this.rememberDecrement, this.rememberForgetThreshold);
        rememberCodelet.addInput(getMemoryByName(MemoryObjectsNames.WORKING_MO));
        rememberCodelet.addInput(this.longMO);
        rememberCodelet.addInput(getMemoryByName(MemoryObjectsNames.DRIVE_MO));
        rememberCodelet.addInput(this.biasDecisionFactorsMO);
        rememberCodelet.addInput(this.reasonerMO);
        rememberCodelet.addInput(this.rememberMO);
        rememberCodelet.addInput(getMemoryByName(MemoryObjectsNames.EXTRACTED_AFFORDANCES_MO));
        rememberCodelet.addInput(this.toDeleteLongMO);
        rememberCodelet.addInput(getMemoryByName(MemoryObjectsNames.SYNCHRONIZER_MO));
        rememberCodelet.setName("RememberCodelet");
        this.m.insertCodelet(rememberCodelet);
        
        ShortMemoryCodelet shortMemoryCodelet = new ShortMemoryCodelet(this.shortMemoryCapacity, this.perceptionMaxActivation, this.perceptionMinActivation, this.perceptionDeleteThreshold, this.perceptionReplaceThreshold, this.perceptionIncrementPerCycle, this.perceptionDecrementPerCycle);
        shortMemoryCodelet.addInput(this.perceptionMO);
        shortMemoryCodelet.addInput(this.shortMO);
        shortMemoryCodelet.addInput(this.longMO);
        shortMemoryCodelet.addInput(this.toDeleteMO);
        shortMemoryCodelet.addInput(this.toModifyPerceptionMO);
        shortMemoryCodelet.addInput(getMemoryByName(MemoryObjectsNames.SYNCHRONIZER_MO));
        shortMemoryCodelet.setName("ShortMemoryCodelet");
        this.m.insertCodelet(shortMemoryCodelet);
        
        DriveHandleCodelet driveHandleCdt = new DriveHandleCodelet(this.selfPerceptCategory, this.relevantPerceptsIncrement);
        driveHandleCdt.addInput(this.shortMO);
        driveHandleCdt.addInput(getMemoryByName(MemoryObjectsNames.SYNCHRONIZER_MO));
        driveHandleCdt.addInput(getMemoryByName(MemoryObjectsNames.DRIVE_MO));
        driveHandleCdt.setName("DriveHandleCodelet");
        this.m.insertCodelet(driveHandleCdt);
        
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
