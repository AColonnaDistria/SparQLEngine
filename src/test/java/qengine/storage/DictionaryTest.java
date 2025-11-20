package qengine.storage;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import fr.boreal.model.logicalElements.api.Literal;
import fr.boreal.model.logicalElements.api.Term;
import fr.boreal.model.logicalElements.factory.impl.SameObjectTermFactory;

class DictionaryTest {
    private static final Literal<String> OBJ1 = SameObjectTermFactory.instance().createOrGetLiteral("obj1");
    private static final Literal<String> OBJ2 = SameObjectTermFactory.instance().createOrGetLiteral("obj2");
    private static final Literal<String> OBJ3 = SameObjectTermFactory.instance().createOrGetLiteral("obj3");
    private static final Literal<String> OBJ4 = SameObjectTermFactory.instance().createOrGetLiteral("obj4");
    private static final Literal<String> OBJ5 = SameObjectTermFactory.instance().createOrGetLiteral("obj5");
    private static final Literal<String> OBJ6 = SameObjectTermFactory.instance().createOrGetLiteral("obj6");
    private static final Literal<String> OBJ7 = SameObjectTermFactory.instance().createOrGetLiteral("obj7");

	@Test
	public void test() {
		Dictionary dictionary = new Dictionary();
		
		Integer id1 = dictionary.put(OBJ1);
		Integer id2 = dictionary.put(OBJ2);
		Integer id3 = dictionary.put(OBJ3);
		Integer id4 = dictionary.put(OBJ4);
		Integer id5 = dictionary.put(OBJ5);
		Integer id6 = dictionary.put(OBJ6);
		Integer id7 = dictionary.put(OBJ7);

        // Taille = 7
        assertEquals(7, dictionary.size());

        // Chaque element veriication id
        assertEquals(id1, dictionary.getId(OBJ1));
        assertEquals(id2, dictionary.getId(OBJ2));
        assertEquals(id3, dictionary.getId(OBJ3));
        assertEquals(id4, dictionary.getId(OBJ4));
        assertEquals(id5, dictionary.getId(OBJ5));
        assertEquals(id6, dictionary.getId(OBJ6));
        assertEquals(id7, dictionary.getId(OBJ7));

        // Dans l'autre sens
        assertEquals(OBJ1, dictionary.getValue(id1));
        assertEquals(OBJ2, dictionary.getValue(id2));
        assertEquals(OBJ3, dictionary.getValue(id3));
        assertEquals(OBJ4, dictionary.getValue(id4));
        assertEquals(OBJ5, dictionary.getValue(id5));
        assertEquals(OBJ6, dictionary.getValue(id6));
        assertEquals(OBJ7, dictionary.getValue(id7));

        // Test contient
        assertTrue(dictionary.containsValue(OBJ1));
        assertTrue(dictionary.containsValue(OBJ2));
        assertTrue(dictionary.containsValue(OBJ3));
        assertTrue(dictionary.containsValue(OBJ4));
        assertTrue(dictionary.containsValue(OBJ5));
        assertTrue(dictionary.containsValue(OBJ6));
        assertTrue(dictionary.containsValue(OBJ7));

        assertTrue(dictionary.containsId(id1));
        assertTrue(dictionary.containsId(id2));
        assertTrue(dictionary.containsId(id3));
        assertTrue(dictionary.containsId(id4));
        assertTrue(dictionary.containsId(id5));
        assertTrue(dictionary.containsId(id6));
        assertTrue(dictionary.containsId(id7));
	}
}
