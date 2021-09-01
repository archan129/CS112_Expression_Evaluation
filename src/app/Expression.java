package app;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import structures.Stack;

public class Expression {
	//final copy
	public static String delims = " \t*+-/()[]";

	/**
	 * Populates the vars list with simple variables, and arrays lists with arrays
	 * in the expression. For every variable (simple or array), a SINGLE instance is
	 * created and stored, even if it appears more than once in the expression. At
	 * this time, values for all variables and all array items are set to zero -
	 * they will be loaded from a file in the loadVariableValues method.
	 * 
	 * @param expr   The expression
	 * @param vars   The variables array list - already created by the caller
	 * @param arrays The arrays array list - already created by the caller
	 */

	public static void makeVariableLists(String expr, ArrayList<Variable> vars, ArrayList<Array> arrays) {
		expr = removeSpaces(expr) + ' ';
		String name = "";
		Array arr;
		Variable var;
		for (int i = 0; i < expr.length(); i++) {
			if (Character.isLetter(expr.charAt(i))) {
				name += expr.charAt(i);
			} else if (expr.charAt(i) == '[') {
				arr = new Array(name);
				if (!arrays.contains(arr)) {
					arr = new Array(name);
					arrays.add(arr);
				}
				name = "";
			} else if ((name.length() != 0 && isOperand(expr.charAt(i)))
					|| (name.length() != 0 && expr.charAt(i) == ' ')) {
				var = new Variable(name);
				if (!vars.contains(var)) {
					var = new Variable(name);
					vars.add(var);
				}
				name = "";
			}
		}
	}

	/**
	 * Loads values for variables and arrays in the expression
	 * 
	 * @param sc Scanner for values input
	 * @throws IOException If there is a problem with the input
	 * @param vars   The variables array list, previously populated by
	 *               makeVariableLists
	 * @param arrays The arrays array list - previously populated by
	 *               makeVariableLists
	 */
	public static void loadVariableValues(Scanner sc, ArrayList<Variable> vars, ArrayList<Array> arrays)
			throws IOException {
		while (sc.hasNextLine()) {
			StringTokenizer st = new StringTokenizer(sc.nextLine().trim());
			int numTokens = st.countTokens();
			String tok = st.nextToken();
			Variable var = new Variable(tok);
			Array arr = new Array(tok);
			int vari = vars.indexOf(var);
			int arri = arrays.indexOf(arr);
			if (vari == -1 && arri == -1) {
				continue;
			}
			int num = Integer.parseInt(st.nextToken());
			if (numTokens == 2) { // scalar symbol
				vars.get(vari).value = num;
			} else { // array symbol
				arr = arrays.get(arri);
				arr.values = new int[num];
				// following are (index,val) pairs
				while (st.hasMoreTokens()) {
					tok = st.nextToken();
					StringTokenizer stt = new StringTokenizer(tok, " (,)");
					int index = Integer.parseInt(stt.nextToken());
					int val = Integer.parseInt(stt.nextToken());
					arr.values[index] = val;
				}
			}
		}
	}

	/**
	 * Evaluates the expression.
	 * 
	 * @param vars   The variables array list, with values for all variables in the
	 *               expression
	 * @param arrays The arrays array list, with values for all array items
	 * @return Result of evaluation
	 */
	public static float evaluate(String expr, ArrayList<Variable> vars, ArrayList<Array> arrays) {
		String se;
		String revised;
		expr+= " ";
		int indexL = -1;
		int indexR = -1;
		int count = 0;
		boolean isArray = false;
		for (int i = 0; i < expr.length(); i++) {
			if (expr.charAt(i) == '(') {
				indexL = i;
				count++;
			} else if (expr.charAt(i) == '[') {
				indexL = i;
				count++;
			} else if (expr.charAt(i) == ')') {
				indexR = i;
				isArray = false;
				i = expr.length();
			} else if (expr.charAt(i) == ']') {
				indexR = i;
				isArray = true;
				i = expr.length();
			}
		}
		
		String unary= "";
		Float value;
		if (count == 0) {
			se = load(expr, vars, arrays);
			return eval(se);
		}
		if (isArray == true) {
			se = expr.substring(indexL + 1, indexR);
			value = eval(load(se, vars, arrays));
			revised = expr.substring(0, indexR) + "{" + value + "}" + expr.substring(indexR + 1);
		} else {
			se = expr.substring(indexL + 1, indexR);
			if(eval(load(se, vars, arrays)) < 0) {
				value = eval(load(se, vars, arrays)) * -1;
				unary = value +"";
				revised = expr.substring(0, indexL) + '_' + unary + expr.substring(indexR + 1);
				
			}else {
				revised = expr.substring(0, indexL) + eval(load(se, vars, arrays)) + expr.substring(indexR + 1);
			}
		}
		
		return evaluate(revised,vars,arrays);
	}

	private static Float eval(String expr) {
		expr = removeSpaces(expr);
		String e = "";
		for (int i = 0; i < expr.length(); i++) {
			if (Character.isDigit(expr.charAt(i))) {
				e += expr.charAt(i);
			} else if (!Character.isLetter(expr.charAt(i))) {
				e += " ";
				e += expr.charAt(i);
				e += " ";
			}
		}
		Stack<Float> values = new Stack<Float>();
		Stack<Character> ops = new Stack<Character>();
		boolean unary = false;
		float value;
		char[] tokens = e.toCharArray();
		for (int i = 0; i < tokens.length; i++) {
			if(tokens[i] == '_') {
				unary= true;
			}
			if (tokens[i] >= '0' && tokens[i] <= '9') {
				StringBuffer sbuf = new StringBuffer();
				while (i < tokens.length && tokens[i] >= '0' && tokens[i] <= '9') {
					sbuf.append(tokens[i++]);
				}
				if(unary) {
					value = (float) (-1.0*Float.parseFloat(sbuf.toString()));
					values.push(value);
				} else {
					values.push(Float.parseFloat(sbuf.toString()));
				}
				unary = false;
				
			} else if (tokens[i] == '(') {
				ops.push(tokens[i]);
			} else if (tokens[i] == ')') {
				while (ops.peek() != '(') {
					values.push(applyOp(ops.pop(), values.pop(), values.pop()));
				}
				ops.pop();
			}
			else if (tokens[i] == '+' || tokens[i] == '-' || tokens[i] == '*' || tokens[i] == '/') {
				while (!(ops.size() == 0) && hasPrecedence(tokens[i], ops.peek())) {
					values.push(applyOp(ops.pop(), values.pop(), values.pop()));
				}
				ops.push(tokens[i]);
			}
		}
		while (!(ops.size() == 0)) {
			values.push(applyOp(ops.pop(), values.pop(), values.pop()));
		}
		return values.pop();
	}

	private static boolean hasPrecedence(char op1, char op2) {
		if (op2 == '(' || op2 == ')')
			return false;
		if ((op1 == '*' || op1 == '/') && (op2 == '+' || op2 == '-'))
			return false;
		else
			return true;
	}

	private static float applyOp(char op, float b, float a) {
		switch (op) {
		case '+':
			return a + b;
		case '-':
			return a - b;
		case '*':
			return a * b;
		case '/':
			return a / b;
		}
		return 0;
	}
	
	private static String load(String expr, ArrayList<Variable> vars, ArrayList<Array> arrays) {
		expr = removeSpaces(expr);
		expr += " ";
		expr = " " + expr;
		String name = "";
		String temp;
		int t;
		for (int i = 0; i < expr.length(); i++) {
			if (Character.isLetter(expr.charAt(i))) {
				name += expr.charAt(i);
			} else if (expr.charAt(i) == '+' || expr.charAt(i) == '-' || expr.charAt(i) == '*' || expr.charAt(i) == '/'
					|| expr.charAt(i) == ' ') {
				for (int j = 0; j < vars.size(); j++) {
					if (name.equals(vars.get(j).name)) {
						expr = expr.substring(0, i - name.length()) + vars.get(j).value + expr.substring(i);
						i = 1;
						break;
					}
				}
				name = "";
			} else if (expr.charAt(i) == '{') {
				for (int j = 0; j < arrays.size(); j++) {
					if (name.equals(arrays.get(j).name)) {
						temp = expr.substring(expr.indexOf("{") + 1, expr.indexOf("}"));
						if(temp.contains(".")) {
							t = (int) Float.parseFloat(temp);
							expr = expr.substring(0, i - name.length())
									+ arrays.get(j).values[t]+ expr.substring(expr.indexOf("}") + 1);
						}
						expr = expr.substring(0, i - name.length())
								+ arrays.get(j).values[Integer.parseInt(expr.substring(expr.indexOf("{") + 1, expr.indexOf("}")))]+ expr.substring(expr.indexOf("}") + 1);
						i = 1;
						break;
					}
				}
				name = "";
			}
		}
		return expr;
	}
	
	private static String removeSpaces(String expr) {
		String noSpaces = "";
		for (int i = 0; i < expr.length(); i++) {
			if (!expr.substring(i, i + 1).equals(" ")) {
				noSpaces += expr.substring(i, i + 1);
			}
		}
		return noSpaces += " ";
	}

	private static boolean isOperand(char a) {
		if (a == '+' || a == '-' || a == '*' || a == '/') {
			return true;
		}
		return false;
	}
}



