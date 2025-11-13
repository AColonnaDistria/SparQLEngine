package qengine.storage;

import fr.boreal.model.logicalElements.api.Substitution;
import org.apache.commons.lang3.NotImplementedException;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;

import java.util.Collection;
import java.util.Iterator;

/**
 * Implémentation Giant-Table pour stocker des RDFTriple.
 */
public class GiantTable implements RDFStorage {
    @Override
    public boolean add(RDFTriple triple) {
        throw new NotImplementedException();
    }

    @Override
    public long size() {
        throw new NotImplementedException();
    }

    @Override
    public Iterator<Substitution> match(RDFTriple triple) {
        throw new NotImplementedException();
    }

    @Override
    public Iterator<Substitution> match(StarQuery q) {
        throw new NotImplementedException();
    }

    @Override
    public long howMany(RDFTriple triple) {
        throw new NotImplementedException();
    }

    @Override
    public Collection<RDFTriple> getAtoms() {
        throw new NotImplementedException();
    }
}
