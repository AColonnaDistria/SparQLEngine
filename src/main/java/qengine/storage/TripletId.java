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
	
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		
		if (other == null) {
			return false;
		}
		
		if (!(other instanceof TripletId)) {
			return false;
		}
		
		TripletId otherTripletId = (TripletId) other;
		if (otherTripletId.subjectId == this.subjectId 
		 && otherTripletId.predicateId == this.predicateId 
		 && otherTripletId.objectId == this.objectId) {
			return true;
		}
		else {
			return false;
		}
	}
}
