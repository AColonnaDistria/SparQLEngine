package qengine.storage;

import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.logicalElements.api.Term;
import fr.boreal.model.logicalElements.api.Variable;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import fr.boreal.model.logicalElements.impl.VariableImpl;

import org.apache.commons.lang3.NotImplementedException;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Collection;
import java.util.Iterator;

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
    	Integer s = dictionary.tryGetIdOrCreate(triple.getTripleSubject());
    	Integer p = dictionary.tryGetIdOrCreate(triple.getTriplePredicate());
    	Integer o = dictionary.tryGetIdOrCreate(triple.getTripleObject());
    	
    	return this.giantTable.add(new TripletId(s, p, o));
    }

    @Override
    public long size() {
        return this.giantTable.size();
    }
    
    private boolean isEqual(Term term1, Term term2) {
    	return term1.label().equals(term2.label());
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
    	
    	if (!subject.isVariable()) st = st.filter(tr -> tr.getSubjectId() == s);
    	if (!predicate.isVariable()) st = st.filter(tr -> tr.getPredicateId() == p);
    	if (!object.isVariable()) st = st.filter(tr -> tr.getObjectId() == o);
    	
    	boolean eq_sp = subject.isVariable() && predicate.isVariable() && isEqual(subject, predicate);
    	boolean eq_so = subject.isVariable() && object.isVariable() && isEqual(subject, object);
    	boolean eq_po = predicate.isVariable() && object.isVariable() && isEqual(predicate, object);
    	
    	if (eq_sp)
    		st = st.filter(tr -> tr.getSubjectId() == tr.getPredicateId());

    	if (eq_so)
    		st = st.filter(tr -> tr.getSubjectId() == tr.getObjectId());

    	if (eq_po)
    		st = st.filter(tr -> tr.getPredicateId() == tr.getObjectId());
    	
    	return st.map(tr -> {
            Substitution subs = new SubstitutionImpl();

            if (subject.isVariable())
            	subs.add((Variable)subject, dictionary.getValue(tr.getSubjectId()));

            if (predicate.isVariable())
            	subs.add((Variable)predicate, dictionary.getValue(tr.getPredicateId()));

            if (object.isVariable())
            	subs.add((Variable)object, dictionary.getValue(tr.getObjectId()));

            return subs;
        })
	    .collect(Collectors.toList())
	    .iterator();
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
    public long howMany(RDFTriple triple) {
    	Integer s = dictionary.getId(triple.getTripleSubject());
    	Integer p = dictionary.getId(triple.getTriplePredicate());
    	Integer o = dictionary.getId(triple.getTripleObject());
    	
    	if (s == null || p == null || o == null) return 0; // missing operands
    	
    	return this.giantTable.stream().filter(tr -> 
    		((tr.getSubjectId() == s) && (tr.getPredicateId() == p) && (tr.getObjectId() == o)))
    		.count();
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
