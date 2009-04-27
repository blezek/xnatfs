/**
 * 
 */
package org.xnat.xnatfs.test;
import junit.framework.TestCase;
import org.xnat.xnatfs.Node;


/**
 * @author blezek
 *
 */
public class FilenameManipulationTest extends TestCase {
	public void testExtention () {
		assertEquals ( ".txt", Node.extention( "/foo/bar/test.txt"));
		assertEquals ( "", Node.extention("/foo/bar"));
		assertEquals ( "", Node.extention("/"));
	}
	public void testRoot() {
		assertEquals ( "/foo/bar", Node.root("/foo/bar.txt"));
		assertEquals ( "/foo/bar", Node.root("/foo/bar"));
		assertEquals ( "/", Node.root("/"));
	}
	public void testTail() {
		assertEquals ( "bar.txt", Node.tail("/foo/bar.txt"));
		assertEquals ( "bar.txt", Node.tail ("/bar.txt") );
		assertEquals ( "bar.txt", Node.tail ("bar.txt"));
		assertEquals ( "foo", Node.tail("/foo/"));
		assertEquals ( "foo2", Node.tail("foo2/"));
		assertEquals ( "._.", Node.tail("/._."));
	}
	public void testDirname() {
		assertEquals ( "/", Node.dirname("/foo"));
		assertEquals ("/bar", Node.dirname("/bar/foo"));
		assertEquals ( "/bar2", Node.dirname("/bar2/foo/"));
		assertEquals ( "/", Node.dirname("/._."));
	}
}
