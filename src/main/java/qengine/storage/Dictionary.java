package qengine.storage;

import java.util.ArrayList;
import java.util.HashMap;

import fr.boreal.model.logicalElements.api.Term;

public class Dictionary {
	HashMap<Term, Integer> term2id;
	HashMap<Integer, Term> id2term;
	int count;
	
	public Dictionary() {
		this.term2id = new HashMap<Term, Integer>();
		this.id2term = new HashMap<Integer, Term>();
		this.count = 0;
	}
	
	public int put(Term term) {
		int id = count;
		++count;
		
		this.term2id.put(term, id);
		this.id2term.put(id, term);
		
		return id;
	}
	
	public boolean containsValue(Term term) {
		return this.term2id.containsKey(term);
	}

	public boolean containsId(Integer id) {
		return this.id2term.containsKey(id);
	}
	
	public Integer getId(Term term) {
		return this.term2id.get(term);
	}

	public Term getValue(int id) {
		return this.id2term.get(id);
	}
	
	public int size() {
		return this.term2id.size();
	}
}
