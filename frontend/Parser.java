package frontend;

import intermediate.Node;
import intermediate.Procedure;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Parser of Scheme code that builds executable lists.
 */
public class Parser
{
	List<Node> heads; // top level list root nodes

	Node currenthead, currentnode; // list-traversing reference
	int currentscopelevel;
	Stack<Node> uppernodes; // for backtracking

	/**
	 * Constructing a new parser starts a new scheme "session" for the user.
	 * All successive parse calls from the same Parser object builds on same session.
	 */
	public Parser()
	{
	}

	/**
	 * Make lists out of scanned items.
	 * 
	 * @param sourcecode code to parse 
	 */
	public List<Node> parse(String sourcecode)
	{
		uppernodes = new Stack<Node>();
		heads = new ArrayList<Node>();
		Scanner scanner = new Scanner(sourcecode);

		//get tokens from scanner.
		Object token;
		while ((token = scanner.scanForNextToken()) != null)
		{
			if (token == Scanner.Bracket.OPEN)
				startList();
			else if (token == Scanner.Bracket.CLOSE)
				endList();
			else
				addElement(token);
			
			//System.out.println(token + "\t" + token.getClass().getSimpleName());
		}

		//return resulting headnodes.
		return heads;
	}

	/**
	 * Start new list.
	 */
	public void startList()
	{
		// new top level list?
		if (currenthead == null)
		{
			// Make new head.
			currenthead = currentnode = new Node();
			uppernodes.push(currentnode);
			currentscopelevel = 1;
		}
		// sublist?
		else
		{
			// node has no element?
			if (currentnode.element == null)
			{
				// Its element is now new node.
				currentnode.element = new Node();
				uppernodes.push(currentnode);
				currentnode = (Node) currentnode.element;
			}
			// node has element?
			else
			{
				// Its next node is a new node, with element being a new node.
				currentnode.next = new Node(new Node());
				currentnode = currentnode.next;
				uppernodes.push(currentnode);
				currentnode = (Node) currentnode.element;
			}
		}
	}

	/**
	 * add object to list.
	 * 
	 * @param token Scheme primitive.
	 */
	public void addElement(Object object)
	{
		if (object instanceof Procedure)
		{
			Procedure procedure = (Procedure) object;

			// make a new scope if scope-making.
			procedure.level =
				procedure.isscopemaking ? ++currentscopelevel : currentscopelevel;
		}

		//not in a list?
		if (currentnode == null)
		{
			//put it in as argument to a display procedure.
			startList();
			addElement(Procedure.PROCEDURES.get("display"));
			addElement(object);
			endList();
		}
		// current node has no element?
		else if (currentnode.element == null)
		{
			// set element to token.
			currentnode.element = object;
		}
		// node has element?
		else
		{
			// make new node.
			currentnode.next = new Node(object);
			currentnode = currentnode.next;
		}
	}

	/**
	 * End list.
	 */
	public void endList()
	{
		//jump back up a level
		currentnode = uppernodes.pop();

		// done with top level list?
		if (currentnode == currenthead && currenthead.next != null)
		{
			// Add root node to list of roots.
			heads.add(currenthead);
			currenthead = currentnode = null;
		}
	}
}
