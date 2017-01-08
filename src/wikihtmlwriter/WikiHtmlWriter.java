package wikihtmlwriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class WikiHtmlWriter {
	
	
	public static void main (String[] args) {
		File file;
		Scanner s = new Scanner(System.in);
		System.out.println("Path to txt-File");
		while (true) {
			try {
				String in = s.nextLine();
				if (in.equals("exit")) {
					System.exit(0);
				}
				if (!in.equals("")) {
					file = new File(in);
					break;
				}
			} catch (Exception e) {
				
			}
		}
		if (s != null) {
			s.close();
		}
		
		File out = new File(file.getParent() + "\\" + file.getName() + ".html");
		writeToFile(format(read(file)), out);
	}
	
	public static String read (File file) {
		String in = "";
		FileReader r = null;
		Scanner s = null;
		try {
			r = new FileReader(file);
			s = new Scanner(r);
			s.useDelimiter("\\Z");
			in = s.next();
			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		if (s != null) {
			s.close();
		}
		return in;
	}
	
	public static void writeToFile (String content, File file) {
		FileWriter w = null;
		try {
			w = new FileWriter(file);
			w.write(content, 0, content.length());
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String format (String file) {
		List<String> headings = new ArrayList<>();
		boolean inVerbatim = false;
		
		//split file by \n
		String[] lines = file.split("\r\n");
		
		//replace umlauts
		for (int i = 0; i < lines.length; i++) {
			lines[i] = lines[i].replaceAll("&", "&amp;");
			lines[i] = lines[i].replaceAll("ä", "&auml;");
			lines[i] = lines[i].replaceAll("ü", "&uuml;");
			lines[i] = lines[i].replaceAll("ö", "&ouml;");
			lines[i] = lines[i].replaceAll("Ä", "&Auml;");
			lines[i] = lines[i].replaceAll("Ü", "&Uuml;");
			lines[i] = lines[i].replaceAll("Ö", "&Ouml;");
			lines[i] = lines[i].replaceAll("ß", "&szlig;");
			lines[i] = lines[i].replaceAll("§", "&sect;");
		}
		
		//replace headings
		for (int i = 0; i < lines.length; i++) {
			for (int j = 1; j <= 6; j++) {
				if (lines[i].contains("<h" + j + ">")) {
					int start = lines[i].indexOf("<h" + j + ">") + 4;
					int end = lines[i].indexOf("</h" + j + ">");
					String heading = lines[i].substring(start, end);
					headings.add(j + ":" + heading);
					String headingLink = createID(headings.get(headings.size()-1), headings);
					lines[i] = lines[i].replaceAll("<h" + j + ">", "<h" + j + " class=\"TML\"><a name=\"" + headingLink + "\"></a>");
				}
			}
		}
		
		//replace verbatim
		for (int i = 0; i < lines.length; i++) {
			if (inVerbatim || lines[i].contains("<v>")) {
				lines[i] = lines[i].replaceAll(" ", "&nbsp;");
			}
			if (lines[i].contains("<v>")) {
				inVerbatim = true;
				lines[i] = lines[i].replaceAll("<v>", "<pre class=\"TMLverbatim\">");
			}
			if (lines[i].contains("</v>")) {
				inVerbatim = false;
				lines[i] = lines[i].replaceAll("</v>", "</pre>");
			}
			if (inVerbatim){
				lines[i] += "<br />";
			}
		}
		
		//replace tables
		int level = 0;
		for (int i = 0; i < lines.length; i++) {
			int levelTemp = 0;
			String stringStart = "\t";
			
			if (lines[i].contains("<pre")) {
				inVerbatim = true;
			}
			
			while(lines[i].startsWith(stringStart)) {
				levelTemp++;
				stringStart += "\t";
			}
			if (levelTemp > 0 && !inVerbatim) {
				String temp = "";
				while (level < levelTemp) {
					temp += "<ul>\n";
					level++;
				}
				while (level > levelTemp) {
					temp += "</ul>\n";
					level--;
				}
				lines[i] = lines[i].replaceAll("\t", "");
				lines[i] = temp + "<li>" + lines[i] + "</li>";
			}
			else {
				String temp = "";
				while (level > 0) {
					temp += "</ul>\n";
					level--;
				}
				lines[i] = temp + lines[i];
			}
			
			if (lines[i].contains("</pre>")) {
				inVerbatim = false;
			}
		}
		
		//surround rest with <p> ... </p>
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].contains("<pre")) {
				inVerbatim = true;
			}
			if (!lines[i].startsWith("<") && !inVerbatim) {
				lines[i] = "<p>" + lines[i] + "</p>";
			}
			if (lines[i].contains("</pre>")) {
				inVerbatim = false;
			}
		}
		
		//put all lines together
		String newFile = "";
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].contains("<br />")) {
				newFile += lines[i];
			}
			else {
				newFile += lines[i] + "\n";
			}
		}
		
		//create Index
		level = 0;
		String index = "<h1 class=\"TML\"><a name=\"Inhaltsverzeichnis\"></a>Inhaltsverzeichnis</h1>\n";
		for (String string : headings) {
			String stringLink = createID(string, headings);
			int tempLevel = Integer.parseInt("" + string.charAt(0));
			while (level < tempLevel) {
				index += "<ul>\n";
				level++;
			}
			while (level > tempLevel) {
				index += "</ul>\n";
				level--;
			}
			index+="<li><a href=\"#" + stringLink + "\">" + string.substring(2) + "</a></li>\n";
		}
		while (level > 0) {
			index += "</ul>\n";
			level--;
		}
		newFile = "<!--WYSIWYG content - do not remove this comment, and never use this identical text in your topics-->\n"
				+ index + "<p></p>\n" + newFile;
		
		return newFile;	
	}
	
	/**
	 * 
	 * @param string format n:string
	 * @param allNames
	 * @return
	 */
	private static String createID (String string, List<String> allNames) {
		int n = 0;
		for (String string2 : allNames) {
			if (string2.equals(string)) {
				if (string2 == string) {
					break;
				}
				n++;
			}
		}
		return string.substring(2).replaceAll(" |&|;|\\.|-|\\(|\\)", "").toLowerCase() + (n > 0 ? n : "");
	}
	
	
	
	
	
	
	
	
	
	
	
	
}
