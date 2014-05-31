package backend;

import java.util.HashMap;
import java.util.Iterator;

import intermediate.Procedure;
import intermediate.Symbol;

/**
 * The runtime activation record. maintains information about the currently
 * executing procedure, including procedure's local memory.
 */
public class ActivationRecord extends HashMap<Symbol, Object>
{
	public Procedure procedure;
	public ActivationRecord previous; // link to previous record

	/**
	 * Make a new activation record from procedure call.
	 */
	public ActivationRecord(Procedure procedure)
	{
		this.procedure = procedure;
	}

	/**
	 * get value assigned to symbol.
	 */
	public Object get(Object object)
	{
		if (!(object instanceof Symbol))
			return null;

		for (Symbol node : keySet())
			if (node.name.equals(object.toString()))
				return super.get(node);

		return null;
	}

	/**
	 * assign value to symbol.
	 * @return 
	 */
	public Object put(Symbol symbol, Object object)
	{
		Iterator<Symbol> iterator = keySet().iterator();
		while (iterator.hasNext())
		{
			Symbol mapsymbol = iterator.next();
			if (mapsymbol.equals(symbol))
				return super.put(mapsymbol, object);
		}

		return super.put(symbol, object);
	}

	/**
	 * check if contains symbol.
	 */
	public boolean containsKey(Object obj)
	{
		if (!(obj instanceof Symbol))
			return false;

		for (Symbol symbol : keySet())
			if (symbol.name.equals(obj.toString()))
				return true;

		return false;
	}

	/**
	 * print contents.
	 */
	public String toString()
	{
		String memory = "";
		for (Symbol symbol : this.keySet())
			memory += "\"" + symbol + "\" = " + get(symbol) + "\t";

		return procedure.level + "[AR: " + procedure + "\t" + memory + "]";
	}
}
