package qengine.storage;

import fr.boreal.model.logicalElements.api.*;
import fr.boreal.model.logicalElements.impl.*;

import org.apache.commons.lang3.NotImplementedException;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;

import qengine.model.RDFTriple;
import qengine.model.StarQuery;

import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;

/**
 * Implémentation d'un HexaStore pour stocker des RDFAtom.
 * Cette classe utilise six index pour optimiser les recherches.
 * Les index sont basés sur les combinaisons (Sujet, Prédicat, Objet), (Sujet, Objet, Prédicat),
 * (Prédicat, Sujet, Objet), (Prédicat, Objet, Sujet), (Objet, Sujet, Prédicat) et (Objet, Prédicat, Sujet).
 */
public class RDFHexaStore implements RDFStorage {
	Dictionary dictionary;

	Index3i spo;
	Index3i sop;

	Index3i pso;	
	Index3i pos;

	Index3i osp;
	Index3i ops;
	
	long size;
	
    private List<List<List<List<Index3i>>>> choix_index_array;
    
    DB db;
	
	public RDFHexaStore() {
		this.dictionary = new Dictionary();
		this.size = 0;
		
		this.db = DBMaker.newHeapDB().make();
		
		this.spo = new Index3i(db, "spo");
		this.sop = new Index3i(db, "sop");
		
		this.pso = new Index3i(db, "pso");
		this.pos = new Index3i(db, "pos");
		
		this.osp = new Index3i(db, "osp");
		this.ops = new Index3i(db, "ops");
		
		TriplePermutation SPO_ORDER = (s, p, o) -> Arrays.asList(s, p, o);
		TriplePermutation SOP_ORDER = (s, p, o) -> Arrays.asList(s, o, p);

		TriplePermutation PSO_ORDER = (s, p, o) -> Arrays.asList(p, s, o);
		TriplePermutation POS_ORDER = (s, p, o) -> Arrays.asList(p, o, s);

		TriplePermutation OSP_ORDER = (s, p, o) -> Arrays.asList(o, s, p);
		TriplePermutation OPS_ORDER = (s, p, o) -> Arrays.asList(o, p, s);

		TriplePermutationTerm SPO_ORDER_TERM = (s, p, o) -> Arrays.asList(s, p, o);
		TriplePermutationTerm SOP_ORDER_TERM = (s, p, o) -> Arrays.asList(s, o, p);

		TriplePermutationTerm PSO_ORDER_TERM = (s, p, o) -> Arrays.asList(p, s, o);
		TriplePermutationTerm POS_ORDER_TERM = (s, p, o) -> Arrays.asList(p, o, s);

		TriplePermutationTerm OSP_ORDER_TERM = (s, p, o) -> Arrays.asList(o, s, p);
		TriplePermutationTerm OPS_ORDER_TERM = (s, p, o) -> Arrays.asList(o, p, s);

		// INVERSE
		// SPO_ORDER_INVERSE = SPO_ORDER
		// SOP_ORDER_INVERSE = SOP_ORDER
		
		// PSO_ORDER_INVERSE = PSO_ORDER
		// POS_ORDER_INVERSE = OSP_ORDER
		
		// OSP_ORDER_INVERSE = POS_ORDER
		// OPS_ORDER_INVERSE = OPS_ORDER
		
		this.spo.setPermutationOrder(SPO_ORDER, SPO_ORDER_TERM, SPO_ORDER, SPO_ORDER_TERM);
		this.sop.setPermutationOrder(SOP_ORDER, SOP_ORDER_TERM, SOP_ORDER, SOP_ORDER_TERM);
		
		this.pso.setPermutationOrder(PSO_ORDER, PSO_ORDER_TERM, PSO_ORDER, PSO_ORDER_TERM);
		this.pos.setPermutationOrder(POS_ORDER, POS_ORDER_TERM, OSP_ORDER, OSP_ORDER_TERM);

		this.osp.setPermutationOrder(OSP_ORDER, OSP_ORDER_TERM, POS_ORDER, POS_ORDER_TERM);
		this.ops.setPermutationOrder(OPS_ORDER, OPS_ORDER_TERM, OPS_ORDER, OPS_ORDER_TERM);
		
		this.spo.clear();
	    this.sop.clear();
	    this.pso.clear();
	    this.pos.clear();
	    this.osp.clear();
	    this.ops.clear();
	    
		this.choix_index_array = List.of(
	    	List.of(
    			// s
    			List.of(
    				// p
    				List.of(sop, osp, spo, pso, ops, pos), // o
    				List.of(spo, pso)  // ?o
    			),
    			List.of(
    				// ?p
    				List.of(sop, osp), // o
    				List.of(spo, sop)  // ?o
    			)
    		),
    		List.of(
    			// ?s
    			List.of(
    				// p
    				List.of(pos, ops), // o
    				List.of(pso, pos)  // ?o
    			),
    			List.of(
    				// ?p
    				List.of(osp, ops), // o
    				List.of(sop, osp, spo, pso, ops, pos)  // ?o
    			)
    		)
		);
	}
	
	public void addFromFile(String path) {
		
	}
	
    @Override
    public boolean add(RDFTriple triple) {
    	int s, p, o;
    	
    	s = this.dictionary.put(triple.getTripleSubject());
    	p = this.dictionary.put(triple.getTriplePredicate());
    	o = this.dictionary.put(triple.getTripleObject());
    	
    	if (!this.spo.contains(s, p, o)) {
        	insertTripleId(s, p, o);
        	return true;
    	}
    	else {
    		return false;
    	}
    }
    
    private void insertTripleId(int s, int p, int o) {
    	this.spo.put(s, p, o);
    	this.sop.put(s, o, p);
    	
    	this.pso.put(p, s, o);
    	this.pos.put(p, o, s);

    	this.osp.put(o, s, p);
    	this.ops.put(o, p, s);

    	this.size++;
    }

    @Override
    public long size() {
    	return this.size;
    }

    private boolean isEqual(Term term1, Term term2) {
    	return term1.label().equals(term2.label());
    }
    
    private void addSubstitution(ArrayList<Substitution> subs, 
    	Term variableTerm, Term substitutionTerm) {
    	
    	SubstitutionImpl sub = new SubstitutionImpl();
    	sub.add((Variable) variableTerm, substitutionTerm);
    	
    	subs.add(sub);
    }
    
    private void addSubstitution(ArrayList<Substitution> subs, 
    	Term variableTerm1, Term substitutionTerm1, 
    	Term variableTerm2, Term substitutionTerm2) {
    	
    	SubstitutionImpl sub = new SubstitutionImpl();
    	sub.add((Variable) variableTerm1, substitutionTerm1);
    	sub.add((Variable) variableTerm2, substitutionTerm2);
    	
    	subs.add(sub);
    }

    private void addSubstitution(ArrayList<Substitution> subs, 
    	Term variableTerm1, Term substitutionTerm1, 
    	Term variableTerm2, Term substitutionTerm2,
    	Term variableTerm3, Term substitutionTerm3) {
    	
    	SubstitutionImpl sub = new SubstitutionImpl();
    	sub.add((Variable) variableTerm1, substitutionTerm1);
    	sub.add((Variable) variableTerm2, substitutionTerm2);
    	sub.add((Variable) variableTerm3, substitutionTerm3);
    	
    	subs.add(sub);
    }

    private void addEmptySubstitution(ArrayList<Substitution> subs) {
    	SubstitutionImpl sub = new SubstitutionImpl();
    	subs.add(sub);
    }
    
    private Index3i choose_index(boolean variableSubject, boolean variablePredicate, boolean variableObject, Integer s, Integer p, Integer o) {
    	int vs = variableSubject ? 1 : 0;
    	int vp = variablePredicate ? 1 : 0;
    	int vo = variableObject ? 1 : 0;
    	
    	return this.choix_index_array.get(vs).get(vp).get(vo)
    			.stream().min((Index3i index1, Index3i index2) -> {
    		int s1 = index1.selectivity();
    		int s2 = index2.selectivity();
    		
    		if (s1 < s2) {
    			return -1;
    		}
    		else if (s1 > s2) {
    			return +1;
    		}
    		
    		List<Integer> keys_index1_perm = index1.applyPermutationOrder(s, p, o);
    		List<Integer> keys_index2_perm = index2.applyPermutationOrder(s, p, o);
    		
    		s1 = index1.selectivity(keys_index1_perm.get(0));
    		s2 = index2.selectivity(keys_index2_perm.get(0));
    		
    		if (s1 < s2) {
    			return -1;
    		}
    		else if (s1 > s2) {
    			return +1;
    		}

    		s1 = index1.selectivity(keys_index1_perm.get(0), keys_index1_perm.get(1));
    		s2 = index2.selectivity(keys_index2_perm.get(0), keys_index2_perm.get(1));
    		
    		if (s1 < s2) {
    			return -1;
    		}
    		else if (s1 > s2) {
    			return +1;
    		}
    		
    		return 0;
    	}).orElse(null);
    }
    
    /*
    private List<Index3i> choose_indexes(boolean variableSubject, boolean variablePredicate, boolean variableObject) {
    	int vs = variableSubject ? 1 : 0;
    	int vp = variablePredicate ? 1 : 0;
    	int vo = variableObject ? 1 : 0;
    	
    	return this.choix_index_array.get(vs).get(vp).get(vo);
    }
    
    private Index3i choose_index(boolean variableSubject, boolean variablePredicate, boolean variableObject, Integer s, Integer p, Integer o) {
    	List<Index3i> indexes = choose_indexes(variableSubject, variablePredicate, variableObject);
    	
    	return indexes.stream().sorted((Index3i index1, Index3i index2) -> {
    		int s1 = index1.selectivity();
    		int s2 = index2.selectivity();
    		
    		if (s1 < s2) {
    			return -1;
    		}
    		else if (s1 > s2) {
    			return +1;
    		}
    		
    		List<Integer> keys_index1_perm = index1.applyPermutationOrder(s, p, o);
    		List<Integer> keys_index2_perm = index2.applyPermutationOrder(s, p, o);
    		
    		s1 = index1.selectivity(keys_index1_perm.get(0));
    		s2 = index2.selectivity(keys_index2_perm.get(0));
    		
    		if (s1 < s2) {
    			return -1;
    		}
    		else if (s1 > s2) {
    			return +1;
    		}

    		s1 = index1.selectivity(keys_index1_perm.get(0), keys_index1_perm.get(1));
    		s2 = index2.selectivity(keys_index2_perm.get(0), keys_index2_perm.get(1));
    		
    		if (s1 < s2) {
    			return -1;
    		}
    		else if (s1 > s2) {
    			return +1;
    		}
    		
    		return 0;
    	}).findFirst().orElse(null);
    }
    */
    
    @Override
    public Iterator<Substitution> match(RDFTriple triple) {
    	ArrayList<Substitution> subs = new ArrayList<Substitution>();
    	
    	boolean variableSubject = triple.getTripleSubject().isVariable();
    	boolean variablePredicate = triple.getTriplePredicate().isVariable();
    	boolean variableObject = triple.getTripleObject().isVariable();
    	
    	Term subject = triple.getTripleSubject();
    	Term predicate = triple.getTriplePredicate();
    	Term object = triple.getTripleObject();

    	Integer s, p, o;
    	
    	if (!variableSubject) {
    		s = this.dictionary.getId(subject);
    	}
    	else {
    		s = -1;
    	}
    	
    	if (!variablePredicate) {
    		p = this.dictionary.getId(predicate);
    	}
    	else {
    		p = -1;
    	}

    	if (!variableObject) {
    		o = this.dictionary.getId(object);
    	}
    	else {
    		o = -1;
    	}
    	
    	if (s == null || p == null || o == null) {
            return subs.iterator(); 
        }
    	
    	// choisit le meilleur index
    	Index3i index = this.choose_index(variableSubject, variablePredicate, variableObject, s, p, o);
    	List<Integer> keys = index.applyPermutationOrder(s, p, o);
    	List<Term> terms = index.applyPermutationOrder(subject, predicate, object);
    	
    	int key1 = keys.get(0);
    	int key2 = keys.get(1);
    	int key3 = keys.get(2);
    	
    	Term term1 = terms.get(0);
    	Term term2 = terms.get(1);
    	Term term3 = terms.get(2);
    	
    	int count = ((key1 == -1) ? 1 : 0) + ((key2 == -1) ? 1 : 0) + ((key3 == -1) ? 1 : 0);
    	
    	if (count == 0) {
    		if (index.contains(key1, key2, key3)) {
    			addEmptySubstitution(subs);
    		}
    	}
    	else if (count == 1) {
    		Set<Integer> keySet3 = index.get(key1, key2);
    		
    		if (keySet3 != null) {
        		for (int key3_subs : keySet3) {
        			Term term3_subs = dictionary.getValue(key3_subs);
        			
        			addSubstitution(subs, term3, term3_subs);
        		}
    		}
    	}
    	else if (count == 2) {
    		ConcurrentNavigableMap<Fun.Tuple2<Integer,Integer>,HashSet<Integer>> L2 = index.get(key1);
    		
    		if (L2 != null) {
        		for (Map.Entry<Fun.Tuple2<Integer, Integer>, HashSet<Integer>> entry : L2.entrySet()) {
        			Fun.Tuple2<Integer, Integer> entryKeys = entry.getKey();
        			Set<Integer> entryValues = entry.getValue();
        			
        			Integer key2_subs = entryKeys.b;
        			Term term2_subs = dictionary.getValue(key2_subs);
        			
            		for (int key3_subs : entryValues) {
            			Term term3_subs = dictionary.getValue(key3_subs);

            			addSubstitution(subs, term2, term2_subs, term3, term3_subs);
            		}
        		}
    		}
    	}
    	else { // count == 3
    		if (index.entrySet() != null) {
        		for (Map.Entry<Fun.Tuple2<Integer, Integer>, HashSet<Integer>> entry : index.entrySet()) {
        			Fun.Tuple2<Integer, Integer> entryKeys = entry.getKey();
        			Set<Integer> entryValues = entry.getValue();

        			Integer key1_subs = entryKeys.a;
        			Term term1_subs = dictionary.getValue(key1_subs);
        			
        			Integer key2_subs = entryKeys.b;
        			Term term2_subs = dictionary.getValue(key2_subs);
        			
            		for (int key3_subs : entryValues) {
            			Term term3_subs = dictionary.getValue(key3_subs);

            			addSubstitution(subs, term1, term1_subs, term2, term2_subs, term3, term3_subs);
            		}
        		}
    		}
    	}
    	
    	// Filtrage par les égalités
    	
    	
    	return subs.iterator();
    }
    
    @Override
    public Iterator<Substitution> match(StarQuery q) {
    	List<RDFTriple> queries = q.getRdfAtoms();
    	Set<Substitution> substitutions = new HashSet<>();
    	this.match(queries.get(0)).forEachRemaining(substitutions::add);
    	
    	for (int index = 1 ; index < queries.size(); ++index) {
    		RDFTriple query = queries.get(index);
    		Set<Substitution> current = new HashSet<>();
    		this.match(query).forEachRemaining(current::add);
    		
    		substitutions.retainAll(current);
    	}
    	
        return substitutions.iterator();
    }

    // variables
    @Override
    public long howMany(RDFTriple triple) {
    	int s, p, o;
    	
    	s = this.dictionary.getId(triple.getTripleSubject());
    	p = this.dictionary.getId(triple.getTriplePredicate());
    	o = this.dictionary.getId(triple.getTripleObject());
    	
    	// modify in order to account for best index
    	Index3i index = this.choose_index(false, false, false, s, p, o);
    	
    	return (index.containsAsSPO(s, p, o)) ? 1 : 0;
    }

    @Override
    public Collection<RDFTriple> getAtoms() {
    	Index3i index = this.choose_index(true, true, true, -1, -1, -1);
    	
    	ArrayList<RDFTriple> atoms = new ArrayList<>();
		
		for (Map.Entry<Fun.Tuple2<Integer, Integer>, HashSet<Integer>> entry : index.entrySet()) {
			Fun.Tuple2<Integer, Integer> entryKeys = entry.getKey();
			Set<Integer> entryValues = entry.getValue();

			Integer key1 = entryKeys.a;
			Term term1 = dictionary.getValue(key1);
			
			Integer key2 = entryKeys.b;
			Term term2 = dictionary.getValue(key2);
			
    		for (int key3 : entryValues) {
    			Term term3 = dictionary.getValue(key3);
    			
    			List<Term> spo = index.applyInversePermutationOrder(term1, term2, term3);
    			
    			Term subject = spo.get(0);
    			Term predicate = spo.get(1);
    			Term object = spo.get(2);

    			atoms.add(new RDFTriple(subject, predicate, object));
    		}
		}
    	
    	return atoms;
    }
    
    public Dictionary getDictionary() {
    	return this.dictionary;
    }
}
