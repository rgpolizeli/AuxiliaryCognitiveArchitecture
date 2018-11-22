/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reasoner;

import actionSelection.ExtractedAffordance;
import motivation.Drive;
import br.unicamp.cst.core.entities.Codelet;
import java.util.List;

/**
 *
 * @author ricardo
 */
public class Operation {

    private ExtractedAffordance aff;
    private Codelet cdt;
    private int status;
    
    public Operation(ExtractedAffordance aff, Codelet cdt, int status) {
        this.aff = aff;
        this.cdt = cdt;
        this.status = status;
    }

    //////////////////////
    // AUXILIARY METHODS //
    //////////////////////
    
    public ExtractedAffordance getAff() {
        return aff;
    }

    public Codelet getCdt() {
        return cdt;
    }

    public int getStatus() {
        return status;
    }

    public void setAff(ExtractedAffordance aff) {
        this.aff = aff;
    }

    public void setCdt(Codelet cdt) {
        this.cdt = cdt;
    }

    public void setStatus(int status) {
        this.status = status;
    }
    
}
