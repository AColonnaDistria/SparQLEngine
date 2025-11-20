package qengine.storage;

import java.util.List;

import fr.boreal.model.logicalElements.api.Term;

@FunctionalInterface
public interface TriplePermutationTerm {
    List<Term> permuteTerm(Term subject, Term predicate, Term object);
}