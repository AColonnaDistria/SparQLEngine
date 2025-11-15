package qengine.storage;

public record TripletId(int subjectId, int predicateId, int objectId) {
	public int getSubjectId() {
		return this.subjectId;
	}
	
	public int getPredicateId() {
		return this.predicateId;
	}

	public int getObjectId() {
		return this.objectId;
	}
}
