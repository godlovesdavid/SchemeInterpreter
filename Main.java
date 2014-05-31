import intermediate.Node;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import backend.Executor;
import backend.Printer;
import frontend.Parser;

public class Main
{
	/**
	 * Run program, calling parser to parse all code as lists and then executor
	 * to execute the lists.
	 * 
	 * @param args first argument is the name of file to read in.
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException
	{
		//parse code.
		Parser parser = new Parser();

		List<Node> headnodes = parser.parse(readFileAsString(args[0])); //get result

				for (Node node: headnodes)
					System.out.println(Printer.stringify(node, true));

		//execute lists.
		Executor executor = new Executor();
		for (Node headnode : headnodes)
			System.out.println(Printer.stringify(executor.evaluate(headnode, 0),
				false));

		//continually read from console.
		java.util.Scanner consolescanner = new java.util.Scanner(System.in);
		while (true)
		{
			System.out.print("> ");
			headnodes = parser.parse(consolescanner.nextLine());
			System.out.println();
			for (Node headnode : headnodes)
				System.out.println(Printer.stringify(
					executor.evaluate(headnode, 0), false));
		}
	}

	/**
	 * Read file and return its contents as string.
	 * 
	 * @param filepath  path of file
	 * @return contents of file as string
	 * @throws IOException
	 */
	static String readFileAsString(String filepath) throws IOException
	{
		StringBuffer data = new StringBuffer();
		BufferedReader reader = new BufferedReader(new FileReader(filepath));
		char[] buffer = new char[1024];
		int numread = 0;
		while ((numread = reader.read(buffer)) != -1)
			data.append(String.valueOf(buffer, 0, numread));
		reader.close();
		return data.toString();
	}
}
