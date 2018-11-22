/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import perception.Percept;

/**
 *
 * @author ricardo
 */
public class AuxiliarMethods {
    
    public AuxiliarMethods() {
    }
    
    /**
     * Get an instance of the percept. 
     * @param perceptCategory
     * @param targetPercept
     * @param memoryMap it must be a copy of a memory map.
     * @return 
     */
    public static Percept getPerceptFromMemoryMap(String perceptCategory, Percept targetPercept, Map<String,Map<Percept, Double>> memoryMap){ 
        Map<Percept, Double> perceptsOfCategory = memoryMap.get(perceptCategory);
            
        if (perceptsOfCategory!=null) {
            for (Map.Entry<Percept,Double> e : perceptsOfCategory.entrySet()) {
                Percept perceptInMemoryMap = e.getKey();
                if (perceptInMemoryMap.equals(targetPercept)) {
                    return perceptInMemoryMap;
                }
            }
        }

        return null;
    }
    
    
    
    
    ////
    // Auxiliar methods
    ////
    
    public static <K,V> Map<K,V> deepCopyMap(Map<K,V> m){
        synchronized(m){
            Map<K, V> copy = new HashMap<>();
            for(Map.Entry<K,V> entry : m.entrySet()){
                copy.put( entry.getKey(),entry.getValue() );
            }
            return copy;
        }
    }
    
    public static Map<String, Map<Percept,Double>> deepCopyMemoryMap(Map<String, Map<Percept,Double>> m){
        synchronized(m){
            Map<String, Map<Percept,Double>> copy = new HashMap<>();
            for(Map.Entry<String, Map<Percept,Double>> entry : m.entrySet()){
                copy.put( entry.getKey(),new HashMap<>(entry.getValue()) );
            }
            return copy;
        }
    }
    
    public static Map<String, Map<String,List<Percept>>> deepCopyWorkingMemoryMap(Map<String, Map<String,List<Percept>>> m){
        synchronized(m){
            Map<String, Map<String,List<Percept>>> copy = new HashMap<>();
            for(Map.Entry<String, Map<String,List<Percept>>> entry : m.entrySet()){
                String key = entry.getKey();
                Map<String,List<Percept>> secondMap = m.get(key);
                Map<String,List<Percept>> secondMapCopy = new HashMap<>();
                for(Map.Entry<String,List<Percept>> entrySecondMap : secondMap.entrySet()){
                    String keySecondMap = entrySecondMap.getKey();
                    List<Percept> perceptListCopy = new ArrayList<>(entrySecondMap.getValue());
                    secondMapCopy.put(keySecondMap, perceptListCopy);
                }
                copy.put(key,secondMapCopy);
            }
            return copy;
        }
    }
    
    public static Map<String,List<Percept>> deepCopyAttentionMap(Map<String,List<Percept>> m){
        synchronized(m){
            Map<String,List<Percept>> copy = new HashMap<>();
            for(Map.Entry<String,List<Percept>> entry : m.entrySet()){
                copy.put( entry.getKey(),new ArrayList<>(entry.getValue()) );
            }
            return copy;
        }
    }
    
    /**
     * Sort map by values in desc order.
     * @param <K>
     * @param <V>
     * @param map
     * @return 
     */
    public static <K,V extends Comparable<? super V>> List<Map.Entry<K, V>> descSortEntriesByValues(Map<K,V> map) {

        List<Map.Entry<K,V>> sortedEntries = new ArrayList<>(map.entrySet());
        Collections.sort(sortedEntries, (Map.Entry<K,V> e1, Map.Entry<K,V> e2) -> e2.getValue().compareTo(e1.getValue()));
    
        return sortedEntries;
    }
    
    /**
     * Sort map by values in asc order.
     * @param <K>
     * @param <V>
     * @param map
     * @return 
     */
    public static <K,V extends Comparable<? super V>> List<Map.Entry<K, V>> ascSortEntriesByValues(Map<K,V> map) {

        List<Map.Entry<K,V>> sortedEntries = new ArrayList<>(map.entrySet());
        Collections.sort(sortedEntries, (Map.Entry<K,V> e1, Map.Entry<K,V> e2) -> e1.getValue().compareTo(e2.getValue()));
    
        return sortedEntries;
    }
    
    /**
     * Set difference operation.
     * @param <T>
     * @param setA
     * @param setB
     * @return 
     */
    public static <T> Set<T> setDifference(Set<T> setA, Set<T> setB) {
        Set<T> tmp = new TreeSet<>(setA);
        tmp.removeAll(setB);
        return tmp;
    }
    
    public static double normalize(double value, double max, double min){
        return ((value-min)/(max-min));
    }
    
}
