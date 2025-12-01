package qengine.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import fr.boreal.storage.natives.SimpleInMemoryGraphStore;
import fr.lirmm.boreal.util.stream.CloseableIteratorWithoutException;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;
import fr.boreal.model.logicalElements.api.*;

public class Oracle implements RDFStorage {
	SimpleInMemoryGraphStore data;
	
	public Oracle() {
		data = new SimpleInMemoryGraphStore();
	}
	
	@Override
	public boolean add(RDFTriple t) {
		return data.add(t);
	}

	@Override
	public Iterator<Substitution> match(RDFTriple query) {
		CloseableIteratorWithoutException<Atom> subs_atoms = data.match(query);
		ArrayList<Substitution> substitutions = new ArrayList<>();
		
		while (subs_atoms.hasNext()) {
			Atom atom = subs_atoms.next();
			Substitution sub = new SubstitutionImpl();
			
			if (query.getTripleSubject().isVariable()) {
                sub.add((Variable) query.getTripleSubject(), atom.getTerm(0));
            }
			
            if (query.getTriplePredicate().isVariable()) {
                sub.add((Variable) query.getTriplePredicate(), atom.getTerm(1));
            }
            
            if (query.getTripleObject().isVariable()) {
                sub.add((Variable) query.getTripleObject(), atom.getTerm(2));
            }
            
            substitutions.add(sub);
		}
		
		return substitutions.iterator();
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

	@Override
	public long howMany(RDFTriple a) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long size() {
		return data.size();
	}

	@Override
	public Collection<RDFTriple> getAtoms() {
		return data.getAtoms()
                .map(atom -> new RDFTriple(atom.getTerms()))
                .collect(Collectors.toList());
	}

}
