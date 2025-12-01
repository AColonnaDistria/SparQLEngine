package qengine.storage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentNavigableMap;

import org.mapdb.BTreeKeySerializer;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.Fun;
import org.mapdb.Fun.Tuple2;
import org.mapdb.Serializer;

import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.logicalElements.api.Term;
import qengine.model.RDFTriple;

public class Index3i {
	DB db; // = DBMaker.heapDB().make();
	
	// key1, key2, key3
	BTreeMap<Fun.Tuple2<Integer, Integer>, HashSet<Integer>> index3;
	TriplePermutation permutationOrder;
	TriplePermutationTerm permutationOrderTerm;

	TriplePermutation permutationOrder_INVERSE;
	TriplePermutationTerm permutationOrderTerm_INVERSE;
	
	private HashMap<Integer, Integer> stats1;
	private HashMap<Fun.Tuple2<Integer, Integer>, Integer> stats2;
	
	public Index3i(DB db, String name) {
		this.db = db;
		
		this.stats1 = new HashMap<>();
		this.stats2 = new HashMap<>();
		
		index3 = db.createTreeMap(name)
			.keySerializer(new BTreeKeySerializer.Tuple2KeySerializer(
	            BTreeMap.COMPARABLE_COMPARATOR, // Comparator for K1 (assumes Integer implements Comparable)
	            Serializer.INTEGER,             // Serializer for K1
	            Serializer.INTEGER              // Serializer for K2
		      ))
			.valueSerializer(Serializer.JAVA).make();
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
	
	public Set<Map.Entry<Fun.Tuple2<Integer,Integer>,HashSet<Integer>>> entrySet() {
		return index3.entrySet();
	}
	
	public ConcurrentNavigableMap<Fun.Tuple2<Integer,Integer>,HashSet<Integer>> get(int key1) {
		Fun.Tuple2<Integer, Integer> startKey = Fun.t2(key1, Integer.MIN_VALUE);
		Fun.Tuple2<Integer, Integer> endKey = Fun.t2(key1, Integer.MAX_VALUE);
	
		return this.index3.subMap(startKey, true, endKey, false);
	}

	public HashSet<Integer> get(int key1, int key2) {
		return this.index3.get(new Fun.Tuple2<>(key1, key2));
	}

	public boolean contains(int key1, int key2, int key3) {
		HashSet<Integer> index_key2 = this.get(key1, key2);
		
		if (index_key2 == null) {
			return false;
		}
		
		return index_key2.contains(key3);
	}

	public boolean containsAsSPO(int s, int p, int o) {
		List<Integer> keys = permutationOrder.permute(s, p, o);
		
		return this.contains(keys.get(0), keys.get(1), keys.get(2));
	}
	
	public void put(int key1, int key2, int key3) {
		HashSet<Integer> index_key2 = this.get(key1, key2);
		if (index_key2 == null) {
			index_key2 = new HashSet<>();
			
			this.index3.put(Fun.t2(key1, key2), index_key2);

            // Increment count for Key1
            stats1.merge(key1, 1, Integer::sum);
            // Increment count for Key1 + Key2
            stats2.merge(Fun.t2(key1, key2), 1, Integer::sum);
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
		return this.stats1.getOrDefault(key1, 0);
	}
	
	public int selectivity(int key1, int key2) {
		return this.stats2.getOrDefault(Fun.t2(key1, key2), 0);
	}
	
	// size of the index (= number of branches of first level)
	public int size() {
		return index3.size();
	}
	
	public void clear() {
		this.index3.clear();
	}
}
