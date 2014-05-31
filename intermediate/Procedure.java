package intermediate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import backend.Executor;
import frontend.Scanner;

/**
 * Scheme procedure.
 */
public abstract class Procedure implements Cloneable
{
	/*
	 * global procedures.
	 */
	public static Map<String, Procedure> PROCEDURES;
	public static Procedure mainproc = new Procedure("", false, -1, false)
	{
		/**
		 * take lists as args and runs the first elements as procedures.
		 */
		public Object run(List args, Executor executor, int level)
		{
			String result = "";
			for (Object arg : args)
				result += executor.evaluate((Node) arg, level + 1) + "\n";
			return result;
		}
	};
	static
	{
		mainproc.level = 1;
		PROCEDURES = new HashMap<String, Procedure>();
		PROCEDURES.put("map", new Procedure("map", false, 2, true)
		{
			/**
			 * map a procedure over a list of elements and return a list with results.
			 * arg 0 is procedure (looked up)
			 * arg 1 is list of elements to map the procedure over
			 */
			public Object run(List args, Executor executor, int level)
			{
				Node result = new Node();
				Node currentnode = (Node) args.get(1);
				while (currentnode != null && currentnode.element != null)
				{
					//prepend quote to argument, prepend procedure to quote, evaluate, and add to result list.
					Node procnode = new Node(args.get(0));
					Node quotenode = new Node(PROCEDURES.get("quote"));
					Node.append(currentnode.element, quotenode);
					Node.append(quotenode, procnode);
					Node.append(executor.evaluate(procnode, level + 1), result);

					currentnode = currentnode.next;
				}
				return result;
			}
		});
		PROCEDURES.put("display", new Procedure("display", false, 1, true)
		{
			/**
			 * simply return preevaluated args for the console to display.
			 */
			public Object run(List args, Executor executor, int level)
			{
				return args.get(0);
			}
		});
		PROCEDURES.put("eval", new Procedure("eval", false, 1, true)
		{
			/**
			 * evaluate a list
			 */
			public Object run(List args, Executor executor, int level)
			{
				return executor.evaluate(args.get(0), level + 1);
			}
		});
		PROCEDURES.put("quote", new Procedure("quote", false, 1, false)
		{
			/**
			 * return just the argument.
			 */
			public Object run(List args, Executor executor, int level)
			{
				return args.get(0);
			}
		});
		PROCEDURES.put("lambda", new Procedure("lambda", true, -1, false)
		{
			Node torun;
			List paramslist;

			/**
			 * procedure that makes a procedure. 
			 * one passes arg 0--list of params into arg 1--list to eval
			 */
			public Object run(List args, Executor executor, int level)
			{
				//2nd arg to run
				torun = (Node) args.get(1);

				//count number of params, check for duplicates, and add them to list.
				Node paramscounter = (Node) args.get(0);
				int numparams = 0;
				paramslist = new ArrayList();
				while (paramscounter != null)
				{
					if (paramscounter.element == null)
					{
						System.out.println("ERROR: BLANK PARAMETER");
						return null;
					}
					if (paramslist.contains(paramscounter.element))
					{
						System.out.println("ERROR: MULTIPLY DEFINED SYMBOL \""
							+ paramscounter.element + "\"");
						return null;
					}
					paramslist.add(paramscounter.element);
					numparams++;
					paramscounter = paramscounter.next;
				}

				/* Return new procedure with counted number of params. 
				*  It will bind its args to its params that are given by the lambda.
				*/
				Procedure procedure =
					new Procedure("{procedure}", true, numparams, true)
					{
						public Object run(List args, Executor executor, int level)
						{
							//prepend quote to args.
							List quotedargs = new ArrayList();
							for (Object arg : args)
							{
								Node quotenode = new Node(PROCEDURES.get("quote"));
								Node.append(arg, quotenode);

								quotedargs.add(quotenode);
							}

							//Assign params to args.
							for (int i = 0; i < paramslist.size(); i++)
								executor.bind((Symbol) paramslist.get(i), quotedargs
									.get(i), level + 1);

							//execute second argument from lambda.
							return executor.evaluate(torun, level + 1);
						}
					};
				procedure.level = this.level;
				return procedure;
			}
		});
		PROCEDURES.put("let", new Procedure("let", true, 2, false)
		{
			/**
			 * Run values one by one, bind them to given symbols, then run body.
			 * arg 0 is lists of bindings
			 * arg 1 is a list to be evaluated
			 */
			public Object run(List args, Executor executor, int level)
			{
				Node paramnode = (Node) args.get(0);

				Map<Symbol, Object> preevaluated = new HashMap();

				//for each binding list in arg 0
				while (paramnode != null)
				{
					//get symbol.
					Symbol symbol = (Symbol) ((Node) paramnode.element).element;

					//evaluate the value and attach quote in front of it.
					Node quotenode = new Node(PROCEDURES.get("quote"));
					Node.append(executor.evaluate(
						((Node) paramnode.element).next.element, level + 1),
						quotenode);

					//temporarily store the symbol and value in a map (don't bind yet).
					preevaluated.put(symbol, quotenode);

					paramnode = paramnode.next;
				}

				//do the bind.
				for (Symbol arg : preevaluated.keySet())
					executor.bind(arg, preevaluated.get(arg), level + 1);

				//execute second argument list.
				return executor.evaluate(args.get(1), level + 1);
			}
		});
		PROCEDURES.put("let*", new Procedure("let*", true, 2, false)
		{
			/**
			 * let, but binds its symbols as soon as each value expression is evaluated.
			 * Good for when a value expression wants to use a symbol in the bind list.
			 * arg 0 is lists of bindings
			 * arg 1 is a list to be evaluated
			 */
			public Object run(List args, Executor executor, int level)
			{
				Node paramnode = (Node) args.get(0);

				//for each binding list in arg 0
				while (paramnode != null)
				{
					//evaluate the value expressions and prepend quote.
					Node quotenode = new Node(PROCEDURES.get("quote"));
					quotenode.next =
						new Node(executor.evaluate(
							((Node) paramnode.element).next.element, level + 1));

					//bind symbol to result.
					executor.bind((Symbol) ((Node) paramnode.element).element,
						quotenode, level + 1);

					paramnode = paramnode.next;
				}

				//execute second argument list.
				return executor.evaluate(args.get(1), level + 1);
			}
		});
		PROCEDURES.put("if", new Procedure("if", false, 3, false)
		{
			/**
			 * Takes arg 0 as list to evaluate as true or false. 
			 * return arg 1 if result is true, arg 2 if false.
			 */
			public Object run(List args, Executor executor, int level)
			{
				return (Boolean) executor.evaluate(args.get(0), level + 1)
					? executor.evaluate(args.get(1), level + 1) : executor.evaluate(
						args.get(2), level + 1);
			}
		});
		PROCEDURES.put("cond", new Procedure("cond", false, -1, false)
		{
			/**
			 * Run a conditional branch.
			 * arg 0 is an if statement, each successive one is an else-if statement,
			 * and last arg is an else statement.
			 */
			public Object run(List args, Executor executor, int level)
			{
				for (Object arg : args)
				{
					Node node = (Node) arg;

					if (node.element.toString().equals("else")
						|| (Boolean) executor.evaluate(node.element, level + 1))
						return executor.evaluate(node.next.element, level + 1);
				}

				return null;
			}
		});
		PROCEDURES.put("else", new Procedure("else", false, 1, true)
		{
			/**
			 *	return the evaluation of its argument. 
			 **/
			public Object run(List args, Executor executor, int level)
			{
				return executor.evaluate(args.get(0), level + 1);
			}
		});
		PROCEDURES.put("real?", new Procedure("real?", false, 1, true)
		{
			/**
			 * see if argument is Double precision value
			 */
			public Object run(List args, Executor executor, int level)
			{
				return args.get(0) instanceof Double
					|| args.get(0) instanceof Integer;
			}
		});
		PROCEDURES.put("number?", new Procedure("number?", false, 1, true)
		{
			/**
			 * see if argument is Double precision value
			 */
			public Object run(List args, Executor executor, int level)
			{
				return args.get(0) instanceof Double
					|| args.get(0) instanceof Integer;
			}
		});
		PROCEDURES.put("integer?", new Procedure("integer?", false, 1, true)
		{
			/**
			 * see if argument is Integer
			 */
			public Object run(List args, Executor executor, int level)
			{
				return args.get(0) instanceof Integer;
			}
		});
		PROCEDURES.put("equal?", new Procedure("equal?", false, 2, true)
		{
			/**
			 * see if two things are equal
			 */
			public Object run(List args, Executor executor, int level)
			{
				//lists case
				if (args.get(0) instanceof Node)
				{
					if (!(args.get(1) instanceof Node))
						return false;

					Node pointer = (Node) args.get(0);
					Node pointer2 = (Node) args.get(1);
					while (pointer != null)
					{
						if (pointer2 == null)
							return false;

						if (pointer.element == null && pointer2.element != null
							|| pointer.element != null && pointer2.element == null)
							return false;

						else if (!pointer.element.equals(pointer2.element))
							return false;

						pointer = pointer.next;
						pointer2 = pointer2.next;
					}
					if (pointer2 != null)
						return false;

					return true;
				}
				//primitives case
				return args.get(0).equals(args.get(1));
			}
		});
		PROCEDURES.put("pair?", new Procedure("pair?", false, 1, true)
		{
			/**
			 * see if something is a pair (non-blank list node).
			 */
			public Object run(List args, Executor executor, int level)
			{
				return (args.get(0) instanceof Node
					&& ((Node) args.get(0)).element != null ? true : false);
			}
		});
		PROCEDURES.put("and", new Procedure("and", false, -1, false)
		{
			/**
			 * boolean op and
			 */
			public Object run(List args, Executor executor, int level)
			{
				//need to break as soon as result is false
				boolean result =
					(boolean) executor.evaluate(args.get(0), level + 1);
				for (int i = 1; result == true && i < args.size(); i++)
					result =
						result && (boolean) executor.evaluate(args.get(i), level + 1);
				return result;
			}
		});

		PROCEDURES.put("or", new Procedure("or", false, -1, false)
		{
			/**
			 * boolean op or
			 */
			public Object run(List args, Executor executor, int level)
			{
				//need to break as soon as result is true
				boolean result =
					(boolean) executor.evaluate(args.get(0), level + 1);
				for (int i = 1; result == false && i < args.size(); i++)
					result =
						result || (boolean) executor.evaluate(args.get(i), level + 1);
				return result;
			}
		});

		PROCEDURES.put("not", new Procedure("not", false, 1, false)
		{
			/**
			 * boolean op not
			 */
			public Object run(List args, Executor executor, int level)
			{
				return !(boolean) executor.evaluate(args.get(0), level + 1);
			}
		});

		PROCEDURES.put("null?", new Procedure("null?", false, 1, true)
		{
			/**
			 * see if list is empty
			 */
			public Object run(List args, Executor executor, int level)
			{
				return args.get(0) instanceof Node
					? ((Node) args.get(0)).element == null : false;
			}
		});
		PROCEDURES.put("symbol?", new Procedure("symbol?", false, 1, true)
		{
			/**
			 * return whether object is of type symbol
			 * 
			 * @param args
			 *            arg0 for testing
			 */
			public Object run(List args, Executor executor, int level)
			{
				return args.get(0) instanceof Symbol;
			}
		});
		PROCEDURES.put("boolean?", new Procedure("boolean?", false, 1, true)
		{
			/**
			 * return whether object is of type boolean
			 * 
			 * @param args
			 *            arg0 for testing
			 */
			public Object run(List args, Executor executor, int level)
			{
				return args.get(0) instanceof Boolean;
			}
		});
		PROCEDURES.put("char?", new Procedure("char?", false, 1, true)
		{
			/**
			 * return whether object is of type char
			 * 
			 * @param args
			 *            arg0 for testing
			 */
			public Object run(List args, Executor executor, int level)
			{
				return args.get(0) instanceof Character;
			}
		});
		PROCEDURES.put("string?", new Procedure("string?", false, 1, true)
		{
			/**
			 * return whether object is of type string
			 * 
			 * @param args
			 *            arg0 for testing
			 */
			public Object run(List args, Executor executor, int level)
			{
				return args.get(0) instanceof String;
			}
		});
		PROCEDURES.put("*", new Procedure("*", false, -1, true)
		{
			/**
			 * take any number of args and multiply them.
			 */
			public Object run(List args, Executor executor, int level)
			{
				Double total = Double.parseDouble(args.get(0).toString());
				for (int i = 1; i < args.size(); i++)
					total *= Double.parseDouble(args.get(i).toString());

				if (total.toString().matches(Scanner.INTEGER))
					return (int) (double) total;
				else
					return total;
			}
		});
		PROCEDURES.put("+", new Procedure("+", false, -1, true)
		{
			/**
			 * take any number of args and sum them up
			 */
			public Object run(List args, Executor executor, int level)
			{
				Double total = Double.parseDouble(args.get(0).toString());
				for (int i = 1; i < args.size(); i++)
					total += Double.parseDouble(args.get(i).toString());

				if (total.toString().matches(Scanner.INTEGER))
					return (int) (double) total;
				else
					return total;
			}
		});
		PROCEDURES.put("-", new Procedure("-", false, -1, true)
		{
			/**
			 * take any number of args and subtract the args from first arg
			 */
			public Object run(List args, Executor executor, int level)
			{
				Double total = Double.parseDouble(args.get(0).toString());
				for (int i = 1; i < args.size(); i++)
					total -= Double.parseDouble(args.get(i).toString());

				if (total.toString().matches(Scanner.INTEGER))
					return (int) (double) total;
				else
					return total;
			}
		});
		PROCEDURES.put("/", new Procedure("/", false, -1, true)
		{
			/**
			 * take any number of args and divide them up
			 */
			public Object run(List args, Executor executor, int level)
			{
				Double total = Double.parseDouble(args.get(0).toString());
				for (int i = 1; i < args.size(); i++)
					total /= Double.parseDouble(args.get(i).toString());

				if (total.toString().matches(Scanner.INTEGER))
					return (int) (double) total;
				else
					return total;
			}
		});
		PROCEDURES.put("sub1", new Procedure("sub1", false, 1, true)
		{
			/**
			 * subtract 1 from argument.
			 */
			public Object run(List args, Executor executor, int level)
			{
				Double total = Double.parseDouble(args.get(0).toString());
				total = total - 1.;

				if (total.toString().matches(Scanner.INTEGER))
					return (int) (double) total;
				else
					return total;
			}
		});
		PROCEDURES.put("expt", new Procedure("expt", false, -1, true)
		{
			/**
			 * raise the first arg to the second
			 */
			public Object run(List args, Executor executor, int level)
			{
				Double total =
					Math.pow(Double.parseDouble(args.get(0).toString()), Double
						.parseDouble(args.get(1).toString()));

				if (total.toString().matches(Scanner.INTEGER))
					return (int) (double) total;
				else
					return total;
			}
		});
		PROCEDURES.put("<", new Procedure("<", false, 2, true)
		{
			/**
			 * see if 1st arg < 2nd
			 */
			public Object run(List args, Executor executor, int level)
			{
				return Double.parseDouble(args.get(0).toString()) < Double
					.parseDouble(args.get(1).toString());
			}
		});

		PROCEDURES.put("<=", new Procedure("<=", false, 2, true)
		{
			/**
			 * see if 1st arg <= 2nd
			 */
			public Object run(List args, Executor executor, int level)
			{
				return Double.parseDouble(args.get(0).toString()) <= Double
					.parseDouble(args.get(1).toString());
			}
		});
		PROCEDURES.put("=", new Procedure("=", false, 2, true)
		{
			/**
			 * see if 1st arg == 2nd
			 */
			public Object run(List args, Executor executor, int level)
			{
				return Double.parseDouble(args.get(0).toString()) == Double
					.parseDouble(args.get(1).toString());
			}
		});
		PROCEDURES.put(">", new Procedure(">", false, 2, true)
		{
			/**
			 * see if 1st arg > 2nd
			 */
			public Object run(List args, Executor executor, int level)
			{
				return Double.parseDouble(args.get(0).toString()) > Double
					.parseDouble(args.get(1).toString());
			}
		});
		PROCEDURES.put(">=", new Procedure(">=", false, 2, true)
		{
			/**
			 * see if 1st arg < 2nd
			 */
			public Object run(List args, Executor executor, int level)
			{
				return Double.parseDouble(args.get(0).toString()) >= Double
					.parseDouble(args.get(1).toString());
			}
		});
		PROCEDURES.put("append", new Procedure("append", false, -1, true)
		{
			/**
			 * append the contents of lists into one
			 * 
			 * @param args
			 *            headnodes
			 */
			public Object run(List args, Executor executor, int level)
			{
				Node result = new Node();

				for (Object arg : args)
				{
					Node currentnode = (Node) arg;
					while (currentnode != null)
					{
						Node.append(currentnode.element, result);
						currentnode = currentnode.next;
					}
				}
				return result;
			}
		});
		PROCEDURES.put("list", new Procedure("list", false, -1, true)
		{
			/**
			 * add things together into a list
			 * 
			 * @param args
			 */
			public Object run(List args, Executor executor, int level)
			{
				Node result = new Node();

				for (Object arg : args)
					Node.append(arg, result);

				return result;
			}
		});

		PROCEDURES.put("cons", new Procedure("cons", false, 2, true)
		{
			/**
			 * append a list element with another list's elements
			 * 
			 * @param args 1st arg is list element, 2nd arg is list
			 */
			public Object run(List args, Executor executor, int level)
			{
				Node result = new Node(args.get(0));

				if (args.get(1) instanceof Node)
				{
					Node currentnode = (Node) args.get(1);
					while (currentnode != null)
					{
						Node.append(currentnode.element, result);

						currentnode = currentnode.next;
					}
				}
				else
				//2nd arg is not list? we have a dotted pair
				{
					//appender.next = args.get(1); //need to implement dotted pairs (change next type to Object)
				}
				return result;
			}
		});
		PROCEDURES.put("car", new Procedure("car", false, 1, true)
		{
			/**
			 * give first element of list
			 * arg 0 is list head node
			 */
			public Object run(List args, Executor executor, int level)
			{
				return ((Node) args.get(0)).element;
			}
		});
		PROCEDURES.put("cdr", new Procedure("cdr", false, 1, true)
		{
			/**
			 * give rest of list
			 * arg 0 is list head node
			 */
			public Object run(List args, Executor executor, int level)
			{
				Node nextnode = ((Node) args.get(0)).next;
				return nextnode == null ? new Node() : nextnode;
			}
		});
		PROCEDURES.put("reverse", new Procedure("reverse", false, 1, true)
		{
			/**
			 * reverse the order of the elements of a list
			 */
			public Object run(List args, Executor executor, int level)
			{
				Node result = new Node();

				//push node contents onto stack as read.
				Stack stack = new Stack();
				Node node = (Node) args.get(0);
				while (node != null)
				{
					stack.push(node.element);
					node = node.next;
				}

				while (!stack.isEmpty())
					Node.append(stack.pop(), result);

				return result;
			}
		});
		PROCEDURES.put("define", new Procedure("define", false, 2, false)
		{
			/**
			 * map symbol to object in memorymap
			 * 
			 * @param args symbol, object bind pair
			 * @return
			 */
			public Object run(List args, Executor executor, int level)
			{
				//evaluate the second argument and prepend quote.
				Node quotenode = new Node(Procedure.PROCEDURES.get("quote"));
				Node.append(executor.evaluate(args.get(1), level + 1), quotenode);

				//bind the result to the first argument, a symbol.
				executor.bind((Symbol) args.get(0), quotenode, level + 1);

				// don't want to return anything when you define.
				return null;
			}
		});
	}
	public String name;
	public boolean isscopemaking, evalargs;
	public int level, numargs; //can be -1 for infinite

	public Procedure(String name, boolean isscopemaking, int numargs,
		boolean evalargs)
	{
		this.name = name;
		this.isscopemaking = isscopemaking;
		this.evalargs = evalargs;
		this.numargs = numargs;
	}

	public String toString()
	{
		return name;
	}

	/**
	 * Take in arguments for this procedure to run and runs procedure.
	 * 
	 * @param args the arguments for this procedure to run.
	 * @return result of the run.
	 */
	public abstract Object run(List args, Executor executor, int level);

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

	public boolean equals(Object obj2)
	{
		return (!(obj2 instanceof Procedure)) ? false : name
			.equals(((Procedure) obj2).name);
	}
}
