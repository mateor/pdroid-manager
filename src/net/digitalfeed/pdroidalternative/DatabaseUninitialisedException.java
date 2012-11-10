package net.digitalfeed.pdroidalternative;

/**
 * An exception used to notify that the database is in some way 'uninitialised': 
 * that is, it is lacking the basic data which initialisation should have inserted. This is:
 *   List of settings
 *   List of permissions
 *   Relationships between settings and permissions
 * @author smorgan
 *
 */
public class DatabaseUninitialisedException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public DatabaseUninitialisedException(String s) {
		super(s);
	}
}
