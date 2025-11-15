package qengine.storage;

import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.logicalElements.api.Term;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import fr.boreal.model.logicalElements.impl.VariableImpl;

import org.apache.commons.lang3.NotImplementedException;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implémentation Giant-Table pour stocker des RDFTriple.
 */
public class GiantTable implements RDFStorage {
	Dictionary dictionary;
	ArrayList<TripletId> giantTable;
	
	public GiantTable() {
		this.dictionary = new Dictionary();
		this.giantTable = new ArrayList<TripletId>();
	}
	
    @Override
    public boolean add(RDFTriple triple) {
    	Integer s = dictionary.getId(triple.getTripleSubject());
    	Integer p = dictionary.getId(triple.getTriplePredicate());
    	Integer o = dictionary.getId(triple.getTripleObject());
    	
    	if (s == null) {
    		s = dictionary.put(triple.getTripleSubject());
    	}

    	if (p == null) {
    		p = dictionary.put(triple.getTriplePredicate());
    	}

    	if (o == null) {
    		o = dictionary.put(triple.getTripleObject());
    	}
    	
    	return this.giantTable.add(new TripletId(s, p, o));
    }

    @Override
    public long size() {
        return this.giantTable.size();
    }

    @Override
    public Iterator<Substitution> match(RDFTriple triple) {
    	Term subject = triple.getTripleSubject();
    	Term predicate = triple.getTriplePredicate();
    	Term object = triple.getTripleObject();
    	
    	Integer s = dictionary.getId(subject);
    	Integer p = dictionary.getId(predicate);
    	Integer o = dictionary.getId(object);
    	
    	Stream<TripletId> st = this.giantTable.stream();
    	
    	if (!subject.label().startsWith("?")) {
    		st = st.filter(tr -> tr.getSubjectId() == s);
    	}

    	if (!predicate.label().startsWith("?")) {
    		st = st.filter(tr -> tr.getPredicateId() == p);
    	}

    	if (!object.label().startsWith("?")) {
    		st = st.filter(tr -> tr.getObjectId() == o);
    	}
    	
    	boolean eq_sp = subject.label().startsWith("?") && predicate.label().startsWith("?") && subject.label().equals(predicate);
    	boolean eq_so = subject.label().startsWith("?") && object.label().startsWith("?") && subject.label().equals(object);
    	boolean eq_po = predicate.label().startsWith("?") && object.label().startsWith("?") && predicate.label().equals(object);
    	
    	if (eq_sp) {
    		st = st.filter(tr -> tr.getSubjectId() == tr.getPredicateId());
    	}

    	if (eq_so) {
    		st = st.filter(tr -> tr.getSubjectId() == tr.getObjectId());
    	}

    	if (eq_po) {
    		st = st.filter(tr -> tr.getPredicateId() == tr.getObjectId());
    	}
    	
    	return st.map(tr -> {
		            Substitution subs = new SubstitutionImpl();
		
		            if (subject.label().startsWith("?")) {
		            	subs.add(new VariableImpl(subject.label()), dictionary.getValue(tr.getSubjectId()));
		            }

		            if (predicate.label().startsWith("?")) {
		            	subs.add(new VariableImpl(predicate.label()), dictionary.getValue(tr.getPredicateId()));
		            }

		            if (object.label().startsWith("?")) {
		            	subs.add(new VariableImpl(object.label()), dictionary.getValue(tr.getObjectId()));
		            }
		
		            return subs;
		        })
    		    .collect(Collectors.toList())
    		    .iterator();
    }

    @Override
    public Iterator<Substitution> match(StarQuery q) {
        throw new NotImplementedException();
    }

    @Override
    public long howMany(RDFTriple triple) {
    	Integer s = dictionary.getId(triple.getTripleSubject());
    	Integer p = dictionary.getId(triple.getTriplePredicate());
    	Integer o = dictionary.getId(triple.getTripleObject());
    	
    	if (s == null || p == null || o == null) {
    		return 0;
    	}
    	
    	return this.giantTable.stream().filter(tr -> 
    		((tr.getSubjectId() == s)
    	  && (tr.getPredicateId() == p)
    	  && (tr.getObjectId() == o))).count();
    }

    @Override
    public Collection<RDFTriple> getAtoms() {
        return this.giantTable.stream()
        .map(tr -> new RDFTriple(
            dictionary.getValue(tr.getSubjectId()),
            dictionary.getValue(tr.getPredicateId()),
            dictionary.getValue(tr.getObjectId())
        ))
        .toList();
    }
}
