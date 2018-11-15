/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package memory;

import actionSelection.AffordanceType;

/**
 *
 * @author ricardo
 */
public class NotRemember {

    private final AffordanceType affordance;
    private int cycles;
    
    
    public NotRemember(AffordanceType affordance, int cycles) {
        this.affordance = affordance;
        this.cycles = cycles;
    }
    
    
    //////////////////////
    // AUXILIARY METHODS //
    //////////////////////

    public AffordanceType getAffordance() {
        return affordance;
    }

    public int getCycles() {
        return cycles;
    }

    public void setCycles(int cycles) {
        this.cycles = cycles;
    }

    
    
}
