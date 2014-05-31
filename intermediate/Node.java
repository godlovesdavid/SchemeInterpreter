package intermediate;

import backend.Printer;

/**
 * node that is used to make lists with.
 */
public class Node implements Cloneable
{
	public Object element; // can be Node or Token
	public Node next;

	/**
	 * blank node
	 */
	public Node()
	{

	}

	/**
	 * node with element
	 * @param element
	 */
	public Node(Object element)
	{
		this.element = element;
	}

	/**
	 * say what node has.
	 */
	public String toString()
	{
		return Printer.stringify(this, false);
	}

	/**
	 * shallow clone.
	 */
	public Object clone()
	{
		try
		{
			return super.clone();
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * append something to a list node.
	 * @param object the thing to append
	 * @param head the list node to be appended
	 */
	public static void append(Object object, Node head)
	{
		if (head.element == null)
			head.element = object;
		else if (head.next == null)
			head.next = new Node(object);
		else
			append(object, head.next);
	}
}