package intermediate;

/**
 * symbol with name and attributes
 */
public class Symbol implements Cloneable
{
	public String name;

	public Symbol(String name)
	{
		this.name = name;
	}

	public String toString()
	{
		return name;
	}

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
	 * because we want no two symbols with same name, 
	 * declare two symbols equal if their names are the same.
	 */
	public boolean equals(Object obj2)
	{
		return (!(obj2 instanceof Symbol)) ? false : name
			.equals(((Symbol) obj2).name);
	}
}