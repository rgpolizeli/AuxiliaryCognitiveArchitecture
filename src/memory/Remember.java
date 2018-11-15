/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package memory;

import actionSelection.AffordanceType;
import perception.Percept;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ricardo
 */
public class Remember {

    private AffordanceType currentAff;
    private AffordanceType parentAff;
    private Integer hierarchyContribution;
    private Map<String, List<Percept>> relevantPercepts;
    private int duration;
    private int totalNumberOfPercepts;
    
    
    public Remember(AffordanceType currentAff, AffordanceType parentAff, Integer hierarchyContribution, Map<String, List<Percept>> relevantPercepts, int duration) {
        
        this.currentAff = currentAff;
        this.parentAff = parentAff;
        this.hierarchyContribution = hierarchyContribution;
        this.relevantPercepts = relevantPercepts;
        this.duration = duration;
        this.totalNumberOfPercepts = countTotalNumberOfPercepts();
    }
    
    
    //////////////////////
    // AUXILIARY METHODS //
    //////////////////////

    public AffordanceType getCurrentAff() {
        return currentAff;
    }
    
    public AffordanceType getParentAff() {
        return this.parentAff;
    }

    public void setCurrentAff(AffordanceType currentAff) {
        this.currentAff = currentAff;
    }

    public int getHierarchyContribution() {
        return hierarchyContribution;
    }
    
    public int getTotalOfPerceptsInRemember(){
        return totalNumberOfPercepts;
    }
    
    public int countTotalNumberOfPercepts(){
        if (this.relevantPercepts == null) {
            return 0;
        }
        if (this.relevantPercepts.isEmpty()) {
            return 0;
        }
        
        int total = 0;
        for (List<Percept> percepts : this.relevantPercepts.values()) {
            if (percepts!=null) {
                total += percepts.size();
            }
        }
        return total;
    }
    
    public void setHierarchyContribution(int hierarchyContribution) {
        this.hierarchyContribution = hierarchyContribution;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int newDuration) {
        this.duration = newDuration;
    }

    public Map<String, List<Percept>> getRelevantPercepts() {
        return relevantPercepts;
    }

    public void setRelevantPercepts(Map<String, List<Percept>> relevantPercepts) {
        this.relevantPercepts = relevantPercepts;
        this.totalNumberOfPercepts = countTotalNumberOfPercepts();
    }
    
}
