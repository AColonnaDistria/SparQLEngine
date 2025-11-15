package qengine.storage;

import fr.boreal.model.logicalElements.api.*;
import fr.boreal.model.logicalElements.impl.*;

import org.apache.commons.lang3.NotImplementedException;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;

import static org.mockito.Mockito.framework;

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
	
	public RDFHexaStore() {
		this.dictionary = new Dictionary();
		
		this.spo = new Index3i();
		this.sop = new Index3i();
		
		this.pso = new Index3i();
		this.pos = new Index3i();
		
		this.osp = new Index3i();
		this.ops = new Index3i();
	}
	
    @Override
    public boolean add(RDFTriple triple) {
    	int s, p, o;
    	
    	if (!this.dictionary.containsValue(triple.getTripleSubject())) {
    		this.dictionary.put(triple.getTripleSubject());
    	}

    	if (!this.dictionary.containsValue(triple.getTriplePredicate())) {
    		this.dictionary.put(triple.getTriplePredicate());
    	}

    	if (!this.dictionary.containsValue(triple.getTripleObject())) {
    		this.dictionary.put(triple.getTripleObject());
    	}
    	
    	s = this.dictionary.getId(triple.getTripleSubject());
    	p = this.dictionary.getId(triple.getTriplePredicate());
    	o = this.dictionary.getId(triple.getTripleObject());
    	
    	return insertTripleId(s, p, o);
    }
    
    private boolean insertTripleId(int s, int p, int o) {
    	this.spo.put(s, p, o);
    	this.sop.put(s, o, p);
    	
    	this.pso.put(p, s, o);
    	this.pos.put(p, o, s);

    	this.osp.put(o, s, p);
    	this.ops.put(o, p, s);
    	
    	return true;
    }

    @Override
    public long size() {
    	return this.spo.size();
    }

    @Override
    public Iterator<Substitution> match(RDFTriple triple) {
    	boolean variableSubject = triple.getTripleSubject().label().startsWith("?");
    	boolean variablePredicate = triple.getTriplePredicate().label().startsWith("?");
    	boolean variableObject = triple.getTripleObject().label().startsWith("?");
    	
    	Term subject = triple.getTripleSubject();
    	Term predicate = triple.getTriplePredicate();
    	Term object = triple.getTripleObject();

    	int s = variableSubject ? -1 : this.dictionary.getId(subject);
    	int p = variablePredicate ? -1 : this.dictionary.getId(predicate);
    	int o = variableObject ? -1 : this.dictionary.getId(object);

    	// choisit l'index tel que 

    	// (1) les objets variables sont placés le plus à droite
    	// (2) les objets variables égaux sont placés le plus à droite
    	
    	if (!variableSubject) {
    		if (!variablePredicate) {
    			if (!variableObject) {
    				// s p o
    				return match_index(this.spo, subject, predicate, object);
    			}
    			else {
    				// s p ?o
    				return match_index(this.spo, subject, predicate, object);
    			}
    		}
    		else {
    			if (!variableObject) {
    				// s ?p o 
    				return match_index(this.sop, subject, object, predicate);
    			}
    			else {
    				// s ?p ?o
    				return match_index(this.sop, subject, object, predicate);
    			}
    		}
    	}
    	else {
    		if (!variablePredicate) {
    			if (!variableObject) {
    				// ?s p o
    				return match_index(this.pos, predicate, object, subject);
    			}
    			else {
    				// ?s p ?o
    				return match_index(this.pos, predicate, object, subject);
    			}
    		}
    		else {
    			if (!variableObject) {
    				// ?s ?p o
    				return match_index(this.ops, object, predicate, subject);
    			}
    			else {
    				// ?s ?p ?o
    				if (subject.label().equals(object.label())) {
        				return match_index(this.pos, predicate, object, subject);
    				}
    				else if (subject.label().equals(predicate.label())) {
        				return match_index(this.osp, object, subject, predicate);
    				}
    				else {
        				return match_index(this.spo, subject, predicate, object);
    				}
    			}
    		}
    	}
    }
    
    private Iterator<Substitution> match_index(Index3i index, Term term1, Term term2, Term term3) {
    	boolean variableTerm1 = term1.label().startsWith("?");
    	boolean variableTerm2 = term2.label().startsWith("?");
    	boolean variableTerm3 = term3.label().startsWith("?");

    	boolean eq12 = variableTerm1 && variableTerm2 && term1.label().equals(term2.label()); // ?x = ?y = ?z
    	boolean eq23 = variableTerm2 && variableTerm3 && term2.label().equals(term3.label()); // ?y = ?z
    	
    	// ici on suppose
    	
    	// (1) les objets variables sont placés le plus à droite
    	// (2) les objets variables égaux sont placés le plus à droite
    	
		ArrayList<Substitution> subs = new ArrayList<Substitution>();
		
		if (eq12) {
			// cas ?x = ?y = ?z.

			Collection<Integer> L1 = index.keySet();
			
			for (int id1 : L1) {
				if (index.contains(id1, id1, id1)) {
					SubstitutionImpl current_subs = new SubstitutionImpl();
					
					Term current_term1 = this.dictionary.getValue(id1);
					current_subs.add(new VariableImpl(term1.label()), current_term1);
				
					subs.add(current_subs);
				}
			}
			
			return subs.iterator();
		}

		if (eq23) {
			// cas ?y = ?z.
			Collection<Integer> L1 = variableTerm1 ? index.keySet() : List.of(this.dictionary.getId(term1));
			for (int id1 : L1) {
				TreeMap<Integer, HashSet<Integer>> map2 = index.get(id1);
				Collection<Integer> L2 = variableTerm2 ? map2.keySet() : List.of(this.dictionary.getId(term2));
				
				for (int id2 : L2) {
					if (index.contains(id1, id2, id2)) {
						SubstitutionImpl current_subs = new SubstitutionImpl();
						
						Term current_term1 = this.dictionary.getValue(id1);
						Term current_term2 = this.dictionary.getValue(id2);
						
						if (variableTerm1) {
							current_subs.add(new VariableImpl(term1.label()), current_term1);
						}

						current_subs.add(new VariableImpl(term2.label()), current_term2);

						subs.add(current_subs);
					}
				}
			}
			
			return subs.iterator();
		}
		
		// cas tous differents
		Collection<Integer> L1 = variableTerm1 ? index.keySet() : List.of(this.dictionary.getId(term1));
		for (int id1 : L1) {
			TreeMap<Integer, HashSet<Integer>> map2 = index.get(id1);
			Collection<Integer> L2 = variableTerm2 ? map2.keySet() : List.of(this.dictionary.getId(term2));
			
			for (int id2 : L2) {
				HashSet<Integer> map3 = map2.get(id2);
				Collection<Integer> L3 = variableTerm3 ? map3 : List.of(this.dictionary.getId(term3));
				
				for (int id3 : L3) {
					SubstitutionImpl current_subs = new SubstitutionImpl();
					
					Term current_term1 = this.dictionary.getValue(id1);
					Term current_term2 = this.dictionary.getValue(id2);
					Term current_term3 = this.dictionary.getValue(id3);
					
					if (variableTerm1) {
						current_subs.add(new VariableImpl(term1.label()), current_term1);
					}

					if (variableTerm2) {
						current_subs.add(new VariableImpl(term2.label()), current_term2);
					}

					if (variableTerm3) {
						current_subs.add(new VariableImpl(term3.label()), current_term3);
					}
					
					subs.add(current_subs);
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
    	if (this.spo.contains(s, p, o)) {
    		return 1;
    	}
    	else {
    		return 0;
    	}
    }

    @Override
    public Collection<RDFTriple> getAtoms() {
    	ArrayList<RDFTriple> atoms = new ArrayList<>();
    	
    	for (int s : this.spo.keySet()) {
			Term subject = this.dictionary.getValue(s);
    		TreeMap<Integer, HashSet<Integer>> po = this.spo.get(s);
    		
    		for (int p : po.keySet()) {
    			HashSet<Integer> objects = po.get(p);
    			Term predicate = this.dictionary.getValue(p);
    			
        		for (int o : objects) {
        			Term object = this.dictionary.getValue(o);
        			
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
