package qengine.storage;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.logicalElements.api.Term;
import qengine.model.RDFTriple;

public class Index3i {
	// key1, key2, key3
	TreeMap<Integer, TreeMap<Integer, HashSet<Integer>>> index3;
	TriplePermutation permutationOrder;
	TriplePermutationTerm permutationOrderTerm;

	TriplePermutation permutationOrder_INVERSE;
	TriplePermutationTerm permutationOrderTerm_INVERSE;
	
	public Index3i() {
		index3 = new TreeMap<Integer, TreeMap<Integer, HashSet<Integer>>>();
	}
	
	public void setPermutationOrder(TriplePermutation permutationOrder, TriplePermutationTerm permutationOrderTerm,
			TriplePermutation permutationOrder_INVERSE, TriplePermutationTerm permutationOrderTerm_INVERSE) {
		this.permutationOrder = permutationOrder;
		this.permutationOrderTerm = permutationOrderTerm;
		
		this.permutationOrder_INVERSE = permutationOrder_INVERSE;
		this.permutationOrderTerm_INVERSE = permutationOrderTerm_INVERSE;
	}

	public List<Integer> applyPermutationOrder(int s, int p, int o) {
		return this.permutationOrder.permute(s, p, o);
	}

	public List<Term> applyPermutationOrder(Term subject, Term predicate, Term object) {
		return this.permutationOrderTerm.permuteTerm(subject, predicate, object);
	}

	public List<Integer> applyInversePermutationOrder(int key1, int key2, int key3) {
		return this.permutationOrder_INVERSE.permute(key1, key2, key3);
	}

	public List<Term> applyInversePermutationOrder(Term term1, Term term2, Term term3) {
		return this.permutationOrderTerm_INVERSE.permuteTerm(term1, term2, term3);
	}
	
	public Set<Integer> keySet() {
		return index3.keySet();
	}
	
	public TreeMap<Integer, HashSet<Integer>> get(int key1) {
		if (!index3.containsKey(key1)) {
			return null;
		}
		
		return index3.get(key1);
	}

	public HashSet<Integer> get(int key1, int key2) {
		TreeMap<Integer, HashSet<Integer>> index_key1 = this.get(key1);

		if (!index_key1.containsKey(key2)) {
			return null;
		}

		HashSet<Integer> index_key2 = index_key1.get(key2);
		return index_key2;
	}

	public boolean contains(int key1, int key2, int key3) {
		HashSet<Integer> index_key2 = this.get(key1, key2);
		
		return index_key2.contains(key3);
	}

	public boolean containsAsSPO(int s, int p, int o) {
		List<Integer> keys = permutationOrder.permute(s, p, o);
		
		return this.contains(keys.get(0), keys.get(1), keys.get(2));
	}
	
	public void put(int key1, int key2, int key3) {
	    TreeMap<Integer, HashSet<Integer>> index_key1 = this.index3.get(key1);
	    
	    if (index_key1 == null) {
	        index_key1 = new TreeMap<>();
	        this.index3.put(key1, index_key1);
	    }
	    
	    HashSet<Integer> index_key2 = index_key1.get(key2);
	    if (index_key2 == null) {
	        index_key2 = new HashSet<>();
	        index_key1.put(key2, index_key2);
	    }
	    
	    index_key2.add(key3);
	}
	
	public void putAsSPO(int s, int p, int o) {
		List<Integer> keys = permutationOrder.permute(s, p, o);
		
		this.put(keys.get(0), keys.get(1), keys.get(2));
	}
	
	public int selectivity() {
		return this.index3.size();
	}

	public int selectivity(int key1) {
		return this.get(key1).size();
	}
	
	public int selectivity(int key1, int key2) {
		return this.get(key1, key2).size();
	}
	
	// size of the index (= number of branches of first level)
	public int size() {
		return index3.size();
	}
	
	public void clear() {
		this.index3.clear();
	}
}
