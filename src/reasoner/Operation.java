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

    private List<Drive> drives;
    private ExtractedAffordance aff;
    private Codelet cdt;
    private int status;
    
    public Operation(List<Drive> drives, ExtractedAffordance aff, Codelet cdt, int status) {
        this.drives = drives;
        this.aff = aff;
        this.cdt = cdt;
        this.status = status;
    }

    //////////////////////
    // AUXILIARY METHODS //
    //////////////////////
    
    public List<Drive> getDrives(){
        return this.drives;
    }
    
    public ExtractedAffordance getAff() {
        return aff;
    }

    public Codelet getCdt() {
        return cdt;
    }

    public int getStatus() {
        return status;
    }

    public void setDrives(List<Drive> factors){
        this.drives = factors;
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
