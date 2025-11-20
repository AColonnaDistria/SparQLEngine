package qengine.storage;

import fr.boreal.model.logicalElements.api.*;
import fr.boreal.model.logicalElements.impl.*;

import org.apache.commons.lang3.NotImplementedException;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;

import java.util.*;

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
	
    private static List<List<List<List<Index3i>>>> choix_index_array;
	
	public RDFHexaStore() {
		this.dictionary = new Dictionary();
		
		this.spo = new Index3i();
		this.sop = new Index3i();
		
		this.pso = new Index3i();
		this.pos = new Index3i();
		
		this.osp = new Index3i();
		this.ops = new Index3i();
		
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
	
    @Override
    public boolean add(RDFTriple triple) {
    	int s, p, o;
    	
    	s = this.dictionary.put(triple.getTripleSubject());
    	p = this.dictionary.put(triple.getTriplePredicate());
    	o = this.dictionary.put(triple.getTripleObject());
    	
    	insertTripleId(s, p, o);
    	return true;
    }
    
    private void insertTripleId(int s, int p, int o) {
    	this.spo.put(s, p, o);
    	this.sop.put(s, o, p);
    	
    	this.pso.put(p, s, o);
    	this.pos.put(p, o, s);

    	this.osp.put(o, s, p);
    	this.ops.put(o, p, s);
    }

    @Override
    public long size() {
    	return this.spo.size();
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
    
    @Override
    public Iterator<Substitution> match(RDFTriple triple) {
    	ArrayList<Substitution> subs = new ArrayList<Substitution>();
    	
    	boolean variableSubject = triple.getTripleSubject().isVariable();
    	boolean variablePredicate = triple.getTriplePredicate().isVariable();
    	boolean variableObject = triple.getTripleObject().isVariable();
    	
    	Term subject = triple.getTripleSubject();
    	Term predicate = triple.getTriplePredicate();
    	Term object = triple.getTripleObject();

    	int s = variableSubject ? -1 : this.dictionary.getId(subject);
    	int p = variablePredicate ? -1 : this.dictionary.getId(predicate);
    	int o = variableObject ? -1 : this.dictionary.getId(object);

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
    		
    		for (int key3_subs : keySet3) {
    			Term term3_subs = dictionary.getValue(key3_subs);
    			
    			addSubstitution(subs, term3, term3_subs);
    		}
    	}
    	else if (count == 2) {
    		TreeMap<Integer, HashSet<Integer>> L2 = index.get(key1);
    		Set<Integer> keySet2 = L2.keySet();
    		
    		for (int key2_subs : keySet2) {
    			Term term2_subs = dictionary.getValue(key2_subs);
    			Set<Integer> keySet3 = L2.get(key2_subs);
    			
        		for (int key3_subs : keySet3) {
        			Term term3_subs = dictionary.getValue(key3_subs);

        			addSubstitution(subs, term2, term2_subs, term3, term3_subs);
        		}
    		}
    	}
    	else { // count == 3
    		Set<Integer> keySet1 = index.keySet();
    		
    		for (int key1_subs : keySet1) {
        		TreeMap<Integer, HashSet<Integer>> L2 = index.get(key1_subs);
        		Set<Integer> keySet2 = L2.keySet();
    			Term term1_subs = dictionary.getValue(key1_subs);
        		
        		for (int key2_subs : keySet2) {
        			Term term2_subs = dictionary.getValue(key2_subs);
        			Set<Integer> keySet3 = L2.get(key2_subs);
        			
            		for (int key3_subs : keySet3) {
            			Term term3_subs = dictionary.getValue(key3_subs);

            			addSubstitution(subs, term1, term1_subs, term2, term2_subs, term3, term3_subs);
            		}
        		}
    		}
    	}
    	
    	return subs.iterator();
    }
    
    @Override
    public Iterator<Substitution> match(StarQuery q) {
        throw new NotImplementedException();
    }

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
		Set<Integer> keySet1 = index.keySet();
		
		for (int key1 : keySet1) {
    		TreeMap<Integer, HashSet<Integer>> L2 = index.get(key1);
    		Set<Integer> keySet2 = L2.keySet();
			Term term1 = dictionary.getValue(key1);
    		
    		for (int key2 : keySet2) {
    			Term term2 = dictionary.getValue(key2);
    			Set<Integer> keySet3 = L2.get(key2);
    			
        		for (int key3 : keySet3) {
        			Term term3 = dictionary.getValue(key3);

        			List<Term> SPO = index.applyInversePermutationOrder(term1, term2, term3);
        			
        			Term subject = SPO.get(0);
        			Term predicate = SPO.get(1);
        			Term object = SPO.get(2);
        			
        			atoms.add(new RDFTriple(subject, predicate, object));
        		}
    		}
		}
    	
    	return atoms;
    }
    
    public Dictionary getDictionary() {
    	return this.dictionary;
    }
}
