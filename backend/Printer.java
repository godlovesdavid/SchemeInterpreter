package backend;

import intermediate.Node;
import intermediate.Symbol;

/**
 * Representer of data.
 */
public class Printer
{
	/**
	 * Give a user-friendly representation of objects representing Scheme objects.
	 * @param object
	 * @param newlines
	 * @return
	 */
	public static String stringify(Object object, boolean newlines)
	{
		if (object == null)
			return "";
		if (object instanceof Node)
			return "(" + stringify((Node) object, 1, newlines);
		if (object instanceof Character)
			return "#\\" + object;
		if (object instanceof Boolean)
			return ((Boolean) object ? "#t" : "#f");
		if (object instanceof String)
			return "\"" + object + "\"";
		if (object instanceof Symbol)
			return object.toString();

		return object.toString();
	}

	private static String stringify(Node node, int level, boolean newlines)
	{
		String string = "";

		/*
		 * element
		 */
		//no element?
		if (node.element == null)
		{
			//skip printing.
		}
		// element is another node?
		else if (node.element instanceof Node)
		{
			if (newlines)
			{
				// print newline.
				string += "\n";

				// indent.
				for (int i = 0; i < level; i++)
					string += "\t";
			}

			// traverse node.
			string += "(" + stringify((Node) node.element, level + 1, newlines);
		}
		//element is sth else?
		else
		{
			string +=
				stringify(node.element, newlines) + (node.next == null ? "" : " ");
		}

		/*
		 * link to next
		 */
		//no more next? append ).
		if (node.next == null)
		{
			string += ")";
		}
		// there's a next node? traverse it.
		else if (node.next instanceof Node)
		{
			string += stringify(node.next, level, newlines);
		}
		// link to next is not node? use dotted pair notation.
		else if (!(node.next instanceof Node))
		{
			string += " . " + stringify(node.next, newlines);
		}
		return string;
	}
}
