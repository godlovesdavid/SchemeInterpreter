package backend;

import java.util.Stack;

/**
 * runtime stack that manages the activation records being activated.
 */
public class RuntimeStack extends Stack<ActivationRecord>
{
	public RuntimeDisplay display;

	public RuntimeStack()
	{
		display = new RuntimeDisplay();
	}

	/**
	 * push record onto stack and update runtime display.
	 */
	public ActivationRecord push(ActivationRecord record)
	{
		display.callUpdate(record);
		return super.push(record);
	}

	/**
	 * pop off record and update runtime display.
	 */
	public ActivationRecord pop()
	{
		display.returnUpdate(isEmpty() ? -1 : peek().procedure.level);
		return super.pop();
	}

	public String toString()
	{
		String string = "";
		for (int i = size() - 1; i >= 0; i--)
			string += get(i) + (i != 0 ? "\n" : "");
		return string;
	}
}
