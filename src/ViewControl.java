import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

public class ViewControl {

	private List<Person> searchresult = new ArrayList<Person>();
	private List<PartnerRelation> partnerRelationList = new ArrayList<PartnerRelation>();

	public List<Person> quickSearchPerson(Person query) {
		int first = 0;
		String[] sqlExpression = { " WHERE ", " OR" };
		String sql = "Select * from MASTERTABLE ";
		Enumeration<?> pEnum = query.propertyNames();

		while (pEnum.hasMoreElements()) {
			String key = (String) pEnum.nextElement();
			sql += sqlExpression[first] + " (" + key + " LIKE '%"
					+ query.getProperty(key) + "%') ";
			first = 1;
		}

		System.out.println(sql);

		return addParents(addPartnerRelation(queryDatabase(sql)));
	}

	public List<Person> searchPerson(Person query) {
		int first = 0;
		String[] sqlExpression = { "", " AND " };
		String sql = "Select * from MASTERTABLE where ";

		Enumeration<?> pEnum = query.propertyNames();

		while (pEnum.hasMoreElements()) {
			String key = (String) pEnum.nextElement();
			sql += sqlExpression[first] + key + " LIKE '%" + query.getProperty(key)
					+ "%'";
			first = 1;
		}
		System.out.println(sql);
		return addParents(addPartnerRelation(queryDatabase(sql)));
	}

	public int createPerson(Person myPerson) {
		// TODO Auto-generated method stub

		String sql = "INSERT INTO MASTERTABLE (";
		String sql2 = " ";
		Enumeration<?> pEnum = myPerson.propertyNames();

		while (pEnum.hasMoreElements()) {
			String key = (String) pEnum.nextElement();
			sql += key + ",";
			sql2 += "'" + myPerson.getProperty(key) + "',";
		}
		sql = sql.substring(0, sql.length() - 1);
		sql2 = sql2.substring(0, sql2.length() - 1);
		sql += ") VALUES (" + sql2 + ")";

		System.out.println(sql);
		return callDatabase(sql);

	}

	public int updatePerson(Person myPerson) {
		// TODO Auto-generated method stub

		String sql = "UPDATE MASTERTABLE SET ";

		Enumeration<?> pEnum = myPerson.propertyNames();

		while (pEnum.hasMoreElements()) {
			String key = (String) pEnum.nextElement();
			if (key != "ID") // ID muss in den where Zweig
				sql += key + "=" + "'" + myPerson.getProperty(key) + "',";
		}
		sql = sql.substring(0, sql.length() - 1);

		sql += " WHERE ID=" + myPerson.getProperty("ID");

		System.out.println(sql);

		return callDatabase(sql);

	}

	private List<Person> addPartnerRelation(List<Person> pList) {
		// TODO Auto-generated method stub

		for (Person key : pList) {
			String myId = key.getProperty("ID");
			key.setPartnerRelations(queryPartnerRelation(myId));
		}
		;

		return pList;
	}

	private List<PartnerRelation> queryPartnerRelation(String myId) {
		// TODO Auto-generated method stub
		String sql = "SELECT ID from MASTERTABLE where ID in (SELECT ID1 from PARTNERTABLE WHERE ID2="
				+ myId
				+ ") or ID in (Select ID2 from PARTNERTABLE where ID1="
				+ myId + ")";

		List<String> partnerIds = new ArrayList<String>();

		Connection con;
		Statement stmt;
		try {
			Context initContext = new InitialContext();
			DataSource ds = (DataSource) initContext
					.lookup("java:comp/env/jdbc/fmtreedb");
			// Get a database connection
			con = ds.getConnection();

			// Prepare a statement object used to execute query
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.isBeforeFirst() == true) {
				// Es gibt �berhaupt PartnerIDs
				while (rs.next()) {
					partnerIds.add(rs.getString("ID"));
				}
				for (String key : partnerIds) {

					sql = "SELECT STATUS, SINCE FROM partnertable WHERE (ID1="
							+ myId + " OR ID1 =" + key + ")" + " and ( ID2 = "
							+ myId + "  or ID2 = " + key + ")";
					rs = stmt.executeQuery(sql);

					if (rs.isBeforeFirst() == true) {
							PartnerRelation myRelation = new PartnerRelation(getPersonFromMastertable(key),rs);
							partnerRelationList.add(myRelation);
					}
				}
			}
			// Das Resultset ist prozessiert , es kann geschlossen werden
			rs.close();
			stmt.close();
			con.close();
		} catch (java.lang.Exception e) {
			e.printStackTrace();
			System.err.print(e.getClass().getName());
			System.err.println(e.getMessage());

		}
		return partnerRelationList;
	}

	private List<Person> queryDatabase(String sql) {

		Connection con;
		Statement stmt;
		try {
			Context initContext = new InitialContext();
			DataSource ds = (DataSource) initContext
					.lookup("java:comp/env/jdbc/fmtreedb");
			// Get a database connection
			con = ds.getConnection();

			// Prepare a statement object used to execute query
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				// Person myPerson = new Person(rs);
				Person myPerson = new Person(rs);
				searchresult.add(myPerson);
			}

			
			// Das Resultset ist prozessiert , es kann geschlossen werden
			rs.close();
			stmt.close();
			con.close();
		} catch (java.lang.Exception e) {
			e.printStackTrace();
			System.err.print(e.getClass().getName());
			System.err.println(e.getMessage());

		}
		return searchresult;
	}
	
	// internal aux function to get personData from MAstertable
	private Person getPersonFromMastertable(String id) {
		String sql = "SELECT * from MASTERTABLE where ID='"+id+"'";
		
		Person myPerson = null;
		Connection con;
		Statement stmt;
		try {
			Context initContext = new InitialContext();
			DataSource ds = (DataSource) initContext
					.lookup("java:comp/env/jdbc/fmtreedb");
			// Get a database connection
			con = ds.getConnection();

			// Prepare a statement object used to execute query
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if(rs.isBeforeFirst()){
				rs.next();
				myPerson = new Person(rs);
			}
						// Das Resultset ist prozessiert , es kann geschlossen werden
			rs.close();
			stmt.close();
			con.close();
		} catch (java.lang.Exception e) {
			e.printStackTrace();
			System.err.print(e.getClass().getName());
			System.err.println(e.getMessage());

		}
		return myPerson;
	}

	private List<Person> addParents(List<Person> pList) {
			for (Person key : pList) {
				key.setFather(getPersonFromMastertable(key.getProperty("FATHER_ID")));
				key.setMother(getPersonFromMastertable(key.getProperty("MOTHER_ID")));	
			}
			return pList;
	}

	// Das sind die Methoden f�r den Baum der Vorfahren
	// ---------------------------------------------------
	public Person getAncestorTreeFromDB(Person query) {
		String id = query.getProperty("ID");
		return getAncestor(id);
	};

	private Person getAncestor(String id) {
		
		Person person = getPersonFromMastertable(id);
		
		String ancestorId = person.getProperty("FATHER_ID");
		if (ancestorId != null) {
			person.setFather(getAncestor(ancestorId));
		}
		;
		ancestorId = person.getProperty("MOTHER_ID");
		if (ancestorId != null) {
			person.setMother(getAncestor(ancestorId));

		}
		return person;
	}

	/*
	 * Hier kommt die Methode f�r den Baum der Nachfahren Die Liste von children
	 * wird ebenfalls rekursiv bef�llt. Die Funktion Person getChild(String Id)
	 * macht: 1. Get Person Details from Database. �Select *from MASTERTABLE
	 * where ID=Id 2. Get all IDs, where Father_ID or Mother_ID is set to this
	 * Person�s ID . In SQL select ID from MasterTable where (FATHER_ID = Id or
	 * MOTHER_ID = Id) 3. F�r jede ID von 2. F�ge das Resultat des Rekursiven
	 * Aufrufs von getChild(String Id) an das ListenObjekt children.
	 */
	public Person getDescendantTreeFromDB(Person query) {
		String id = query.getProperty("ID");
		return getChild(id);
	};

	/*
	 * ==========================================================================
	 * ======================== Old code: Das benutzt noch die Vorgehensweise
	 * �ber die Partnerliste
	 * 
	 * ==========================================================================
	 * ======================== private Person getChild(String id){
	 * 
	 * //hier bereite ich die SQL f�r Schritt eins vor. Die Details der
	 * Hauptperson in dieser // Iteration werden von der datenbank geholt.
	 * String sql = "SELECT *from MASTERTABLE where ID="+id; Connection con;
	 * Statement stmt; Person person = null; List <Person> partner = new
	 * ArrayList<Person>(); // Das ist die Liste der Partner der aktuellen
	 * (Haupt)-Person List <String> childIds = null; // Das ist die Liste der
	 * childIds pro Partner . Ich speichere diese zwischen List <Person>
	 * children = new ArrayList<Person>(); // Das ist die Liste der Kinder als
	 * Personen, sie werden rekursiv generiert. HashMap <Person,List<String>>
	 * myHash = new HashMap<Person, List<String>>(); // Das ist eine Hashmap um
	 * die Liste der childIds pro Partner zu speichern
	 * 
	 * // start der datenbank Operationen mit try catch umgeben. try { Context
	 * initContext = new InitialContext(); DataSource ds = (DataSource)
	 * initContext .lookup("java:comp/env/jdbc/fmtreedb"); // Get a database
	 * connection con = ds.getConnection();
	 * 
	 * // Prepare a statement object used to execute query stmt =
	 * con.createStatement(); ResultSet rs = stmt.executeQuery(sql); // 1. das
	 * sind die Details der aktuellen Person while (rs.next()) { person = new
	 * Person(rs); } // 2. Bestimme Partner der aktuellen (Haupt-)person sql=
	 * "SELECT * from MASTERTABLE WHERE ID in (SELECT  DISTINCT MOTHER_ID FROM MASTERTABLE"
	 * + " WHERE FATHER_ID="+id+
	 * ") OR ID in  (SELECT  DISTINCT FATHER_ID FROM MASTERTABLE " +
	 * " WHERE MOTHER_ID="+id+")"; System.out.println("Partner for Id:" + id
	 * +":"+ sql); rs = stmt.executeQuery(sql); // wenn Partner vorhanden sind,
	 * dann Partner zur Liste hinzuf�gen if (rs.isBeforeFirst() ) { while
	 * (rs.next()) { partner.add(new Person(rs)); } person.setPartner(partner);
	 * 
	 * // So f�r jeden Eintrag in der Partnerliste werden jetzt die Child IDs
	 * bei der Datenbank abgefragt // Die SQL muss noch verfeinert werden, weil
	 * die Hauptperson auch noch in die SQL hinein gefaltet werden muss for
	 * (Person key: partner){ sql =
	 * "SELECT ID from MASTERTABLE where FATHER_ID ="
	 * +key.getProperty("ID")+" OR MOTHER_ID = " + key.getProperty("ID"); rs =
	 * stmt.executeQuery(sql); if (rs.isBeforeFirst() ) { childIds = new
	 * ArrayList<String>(); while (rs.next()) {
	 * childIds.add(rs.getString("ID")); } // Das Ergebnis wird jetzt in einer
	 * Hashmap zwischen gespeichert // F�r jeden Partner mit dem dieHauptperson
	 * Kinder hat, wird ein Entry in der Hashmap erzeugt myHash.put(key,
	 * childIds); } }
	 * 
	 * } rs.close(); stmt.close(); con.close(); } catch (java.lang.Exception e)
	 * { e.printStackTrace(); System.err.print(e.getClass().getName());
	 * System.err.println(e.getMessage());
	 * 
	 * } // so jetzt muss hier noch der rekursive Aufruf rein f�r die Liste der
	 * Child Ids. // F�r jeden Partner werden jetzt die Kinder rekursiv
	 * bestimmt. for (Person key: myHash.keySet()){ List <String> childIdList =
	 * myHash.get(key); children = new ArrayList<Person>(); for (String myId:
	 * childIdList){ children.add(getChild(myId)); } key.setChildren(children);
	 * }
	 * 
	 * return person;
	 * 
	 * 
	 * }
	 * ========================================================================
	 * ==========================
	 */
	private Person getChild(String id) {

		// hier bereite ich die SQL f�r Schritt eins vor. Die Details der
		// Hauptperson in dieser
		// Iteration werden von der datenbank geholt.
		String sql = "SELECT *from MASTERTABLE where ID=" + id;
		Connection con;
		Statement stmt;
		Person person = null;

		List<String> childIds = new ArrayList<String>(); // Das ist die Liste
															// der childIds pro
															// Partner . Ich
															// speichere diese
															// zwischen
		List<Person> children = new ArrayList<Person>(); // Das ist die Liste
															// der Kinder als
															// Personen, sie
															// werden rekursiv
															// generiert.

		// start der datenbank Operationen mit try catch umgeben.
		try {
			Context initContext = new InitialContext();
			DataSource ds = (DataSource) initContext
					.lookup("java:comp/env/jdbc/fmtreedb");
			// Get a database connection
			con = ds.getConnection();

			// Prepare a statement object used to execute query
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			// 1. das sind die Details der aktuellen Person
			while (rs.next()) {
				person = new Person(rs);
			}
			// F�r die Person werden jetzt alle child ids generiert
			sql = "SELECT ID from MASTERTABLE WHERE FATHER_ID=" + id
					+ " OR MOTHER_ID=" + id;
			rs = stmt.executeQuery(sql);
			if (rs.isBeforeFirst()) {

				while (rs.next()) {
					childIds.add(rs.getString("ID"));
				}
			}
			rs.close();
			stmt.close();
			con.close();
		} catch (java.lang.Exception e) {
			e.printStackTrace();
			System.err.print(e.getClass().getName());
			System.err.println(e.getMessage());
		}
		if (!childIds.isEmpty()) {
			for (String myId : childIds) {
				children.add(getChild(myId));
			}
			person.setChildren(children);
		}
		return person;
	}

	private int callDatabase(String sql) {
		Connection con;
		Statement stmt;
		int rc = 0;
		try {
			Context initContext = new InitialContext();
			DataSource ds = (DataSource) initContext
					.lookup("java:comp/env/jdbc/fmtreedb");
			// Get a database connection
			con = ds.getConnection();

			// Prepare a statement object used to execute query
			stmt = con.createStatement();
			rc = stmt.executeUpdate(sql);
			stmt.close();
			con.close();

		} catch (java.lang.Exception e) {
			e.printStackTrace();
			System.err.print(e.getClass().getName());
			System.err.println(e.getMessage());

		}
		return rc;

	}

}
