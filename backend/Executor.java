package backend;

import intermediate.Node;
import intermediate.Procedure;
import intermediate.Symbol;

import java.util.ArrayList;
import java.util.List;

/**
 * Executor that executes Scheme parsed code.
 */
public class Executor
{
	public RuntimeStack stack;

	boolean DEBUG = false;

	/**
	 * Make executor. Makes new stack and push main record.
	 */
	public Executor()
	{
		(stack = new RuntimeStack())
			.push(new ActivationRecord(Procedure.mainproc));
	}

	/**
	 * Evaluate objects.
	 * 1. for symbols, get the value bound to symbol.
	 * 2. for list nodes, execute it like (procedure arg0 arg1 arg3...)
	 * 3. for any other object, just return that same object.
	 * @param object
	 * @return
	 */
	public Object evaluate(Object object, int level)
	{
		//debug mode
		String indent = "";
		for (int i = 0; i < level; i++)
			indent += "\t";

		if (object instanceof Symbol)
		{
			if (DEBUG)
				System.out.println(indent + "LOOK UP: \"" + object + "\"\n"
					+ indent + "{");

			//fetch the value bound to the symbol from runtime display.
			Object[] records = stack.display.values().toArray();

			for (int i = stack.peek().procedure.level - 1; i >= 0; i--)
			{
				ActivationRecord record = ((ActivationRecord) records[i]);
				if (record.containsKey(object))
				{
					Object result = evaluate(record.get(object), level + 1);

					if (DEBUG)
						System.out.println(indent + "\tLOOKUP RESULT: \"" + object
							+ "\" = " + result + "\n" + indent + "}");

					return result;
				}
			}
			System.out.println("ERROR: SYMBOL \"" + object + "\" NOT BOUND");
			return null;
		}

		//lists with procedures as first element
		if (object instanceof Node)
		{
			Node node = (Node) object;

			if (DEBUG)
				System.out.println(indent + "EVALUATE: "
					+ Printer.stringify(node, false) + "\n" + indent + "{");

			//get procedure from headnode.
			Symbol procsymbol = null;
			if (node.element instanceof Symbol)
				procsymbol = (Symbol) node.element;
			Object firstelem = evaluate(node.element, level + 1);
			Procedure procedure = null;
			if (!(firstelem instanceof Procedure))
			{
				System.out
					.println("ERROR: " + node.element + " IS NOT A PROCEDURE");
				return null;
			}
			else
				procedure = (Procedure) firstelem;

			//get arguments for procedure.
			List args = new ArrayList();
			for (Node argnode = node.next; argnode != null; argnode = argnode.next)
				args.add(procedure.evalargs ? evaluate(argnode.element, level + 1)
					: argnode.element); //pre-evaluate arguments if procedure says to

			//do error checking on arguments.
//						for (Object arg : args)
//							if (arg == null)
//								return null;
			for (int i = 0; i < args.size(); i++)
				if (args.get(i) == null)
					args.remove(args.get(i));
						
			if (procedure.numargs != -1 && args.size() != procedure.numargs)
			{
				System.out.println("ERROR: WRONG NUMBER OF ARGUMENTS. PROCEDURE "
					+ procedure + " TAKES " + procedure.numargs);
				return null;
			}

			if (DEBUG)
				System.out.println(indent + "\tRUN: "
					+ (procsymbol != null ? procsymbol : procedure) + args + "\n"
					+ indent + "\t{");

			//push new activation record if it is a procedure that makes scope.
			if (procedure.isscopemaking)
				stack.push(new ActivationRecord(procedure));

			//get result of procedure.
			Object result = procedure.run(args, this, level + 1);

			if (DEBUG)
				System.out.println(indent + "\t}\n" + indent + "\tRETURN: "
					+ result);

			if (procedure.isscopemaking)
				stack.pop();

			if (DEBUG)
				System.out.println(indent + "}");

			return result;
		}

		//Scheme primitive case.
		return object;
	}

	/**
	 * Assign a symbol to a value.
	 * @param symbol
	 * @param object
	 */
	public void bind(Symbol symbol, Object object, int level)
	{
		stack.peek().put(symbol, object);

		//debug mode
		String indent = "";
		for (int i = 0; i < level; i++)
			indent += "\t";
		if (DEBUG)
		{
			System.out.println(indent + "BIND: \"" + symbol + "\" = " + object);

			//show runtime display contents.
			//			System.out.println(indent + "\t" + "Display contents:");
			//			for (ActivationRecord record : stack.display.values())
			//				System.out.println(indent + "\t" + record);

			//show runtime stack contents.
			System.out.println(indent + "\t" + "Stack contents:");
			for (int i = stack.size() - 1; i >= 0; i--)
				System.out.println(indent + "\t" + stack.get(i));
		}
	}
}