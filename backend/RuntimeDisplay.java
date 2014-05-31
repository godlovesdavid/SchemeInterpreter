package backend;

import java.util.HashMap;

/**
 * Dynamic library for symbol value lookup at runtime.
 * 
 * Runtime display makes accesses from procedures to nonlocal values more
 * efficient. When accessing, it only lets lookups look up what should be
 * viewable (displayable) to the lookup.
 *
 */
public class RuntimeDisplay extends HashMap<Integer, ActivationRecord>
{
	public RuntimeDisplay()
	{
	}

	/**
	 * Update display for given nesting level whenever executor executes a
	 * _call_ to a procedure.
	 */
	public void callUpdate(ActivationRecord record)
	{
		int level = record.procedure.level;

		// record exists for this nesting level?
		if (containsKey(level))
			// make new record point to old record
			record.previous = get(level);

		// make display at this level point to new record
		put(level, record);
	}

	/**
	 * update display for given nesting level whenever executor executes a
	 * _return_ from a procedure.
	 */
	public void returnUpdate(int level)
	{
		// only record at this level?
		if (get(level).previous == null)
			// remove nesting level.
			remove(level);

		// more than one record at this level?
		else
			// make display at this level point to popped record's previous
			// record reference.
			put(level, get(level).previous);
	}

	public String toString()
	{
		String string = "";
		for (ActivationRecord record : values())
			string += record + "\n";
		return string;
	}
}
