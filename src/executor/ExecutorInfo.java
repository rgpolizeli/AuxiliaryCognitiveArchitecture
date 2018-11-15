/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package executor;

import br.unicamp.cst.core.entities.MemoryObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ricardo
 */
public class ExecutorInfo{

    private String className;
    private LinkedHashMap<Class, List<Object>> parameters;
    private List<MemoryObject> memoryObjectsInput;
    private List<MemoryObject> memoryObjectsOutput;
    
    /**
     * Creates an object with parameters to initiate a executor codelet.  
     * @param className
     */
    public ExecutorInfo(String className) {
        this.className = className;
        this.memoryObjectsInput = new ArrayList<>();
        this.memoryObjectsOutput = new ArrayList<>();
        
        this.parameters = new LinkedHashMap<>();
    }    
    
    public void addParameter(Class parameterClass, Object parameterObject){
        Map<Class,List<Object>> parametersMap = this.getParametersMap();
        if (parametersMap.containsKey(parameterClass)) {
            List<Object> parametersObjects = parametersMap.get(parameterClass);
            parametersObjects.add(parameterObject);
        } else{
            List<Object> parametersObjects = new ArrayList<>();
            parametersObjects.add(parameterObject);
            parametersMap.put(parameterClass, parametersObjects);
        }
    }
    
    
    public String getClassName() {
        return className;
    }

    public List<Class> getParametersClasses() {
        List<Class> parametersClasses = new ArrayList<>();
        for (Map.Entry<Class, List<Object>> entry : this.getParametersMap().entrySet()) {
            Class cl = entry.getKey();
            List<Object> objects = entry.getValue();
            
            for (int i = 0; i < objects.size(); i++) {
                parametersClasses.add(cl);
            }
        }
        return parametersClasses;
    }

    public Object[] getParametersObjects() {
        List<Object> parametersObjects = new ArrayList<>();
        for (Map.Entry<Class, List<Object>> entry : this.getParametersMap().entrySet()) {
            List<Object> objects = entry.getValue();
            parametersObjects.addAll(objects);
        }
        return parametersObjects.toArray();
    }

    public LinkedHashMap<Class, List<Object>> getParametersMap() {
        return parameters;
    }
    
    public List<MemoryObject> getMemoryObjectsInput() {
        return memoryObjectsInput;
    }

    public List<MemoryObject> getMemoryObjectsOutput() {
        return memoryObjectsOutput;
    }
    
    public void addInputMemoryObject(MemoryObject mo){
        if (!this.memoryObjectsInput.contains(mo)) {
            this.memoryObjectsInput.add(mo);
        }
    }
    
    public void addOutputMemoryObject(MemoryObject mo){
        if (!this.memoryObjectsOutput.contains(mo)) {
            this.memoryObjectsOutput.add(mo);
        }
    }
    
    
    public ExecutorInfo getClone(){
        ExecutorInfo clone = new ExecutorInfo(this.getClassName());
        
        LinkedHashMap<Class, List<Object>> newParameters = new LinkedHashMap<>();
        List<MemoryObject> newMemoryObjectsInput = new ArrayList<>();
        List<MemoryObject> newMemoryObjectsOutput = new ArrayList<>();
        
        newParameters.putAll(this.getParametersMap());
        newMemoryObjectsInput.addAll(this.getMemoryObjectsInput());
        newMemoryObjectsOutput.addAll(this.getMemoryObjectsOutput());
        
        clone.setMemoryObjectsInput(newMemoryObjectsInput);
        clone.setMemoryObjectsOutput(newMemoryObjectsOutput);
        clone.setParameters(newParameters);
        
        return clone;
    }
    
    public void setMemoryObjectsInput(List<MemoryObject> newInputMOs){
        this.memoryObjectsInput = newInputMOs;
    }
    
    public void setMemoryObjectsOutput(List<MemoryObject> newOutputMOs){
        this.memoryObjectsOutput = newOutputMOs;
    }
    
    public void setParameters(LinkedHashMap<Class, List<Object>> newParameters){
        this.parameters = newParameters;
    }
    
    
}
