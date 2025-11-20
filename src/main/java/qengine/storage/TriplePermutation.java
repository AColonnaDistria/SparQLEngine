package qengine.storage;

import java.util.List;

import fr.boreal.model.logicalElements.api.Term;

@FunctionalInterface
public interface TriplePermutation {
    List<Integer> permute(int s, int p, int o);
}