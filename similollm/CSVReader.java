package similollm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Vector;

/**
 * Parses a CSV file.
 */
public class CSVReader implements Iterator<Properties>
{
	private BufferedReader bufReader = null;

	private List<String> headerLine = null;

	private List<String> nextLine = null;

	private char columnSeparator=',';

	/**
	 * Parse a CSV file.
	 * 
	 * @param filename
	 *          Filename to parse (absolute or relative to working folder)
	 * @throws IOException
	 *           Exception
	 */
	public CSVReader(String filename) throws IOException
	{
		this(new File(filename));
	}

	/**
	 * Parse a CSV file.
	 * 
	 * @param file
	 *          File to parse
	 * @throws IOException
	 *           Exception
	 */
	public CSVReader(File file) throws IOException
	{
		this(new InputStreamReader(new FileInputStream(file), "Cp1252"));
	}

	/**
	 * Parse a CSV file.
	 * 
	 * @param in
	 *          File to parse
	 * @throws IOException
	 *           Exception
	 */
	public CSVReader(Reader in) throws IOException
	{
		bufReader = new BufferedReader(in);
		headerLine = readNextLine(true);
		if (headerLine == null)
		{
			throw new IOException("File does not contain a header line");
		}
		nextLine = readNextLine();
	}

	private char findColumnSeparator(String header)
	{
		if(header.indexOf(',')>=0)
		{
			return ',';
		}
		if(header.indexOf(';')>=0)
		{
			return ';';
		}
		if(header.indexOf('\t')>=0)
		{
			return '\t';
		}
		if(header.indexOf('|')>=0)
		{
			return '|';
		}
		if(header.indexOf(':')>=0)
		{
			return ':';
		}
		return ',';
	}
	
	/**
	 * Check if there is a next line.
	 * 
	 * @return true if has a new line or false if not
	 */
	public boolean hasNext()
	{
		return (nextLine != null);
	}

	/**
	 * Read next line.
	 * 
	 * @return A list of columns from the current line
	 * @throws NoSuchElementException
	 *           Exception
	 */
	public Properties next() throws NoSuchElementException
	{
		if (nextLine == null)
		{
			throw new NoSuchElementException("Read past end of file");
		}
		if (nextLine.size() != headerLine.size())
		{
			// Not the same number of columns as header
			throw new NoSuchElementException("Not the same no of columns as the header");
		}
		// Add values to a Properties
		Properties p = new Properties();
		for (int i = 0; i < nextLine.size(); i++)
		{
			p.put(headerLine.get(i), nextLine.get(i));
		}

		try
		{
			// Read next line
			nextLine = readNextLine();
		}
		catch (IOException e)
		{
			nextLine = null;
		}
		return p;
	}

	/**
	 * Not supported.
	 * 
	 * @throws UnsupportedOperationException
	 *           Exception
	 */
	public void remove() throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException("Not supported");
	}

	/**
	 * Read next line.
	 * 
	 * @return A list of columns from the current line or null if end of file
	 * @throws IOException
	 *           Exception
	 */
	private List<String> readNextLine() throws IOException
	{
		return readNextLine(false);
	}
	
	private List<String> readNextLine(boolean first) throws IOException
	{
		String nextCsvLine = readOneCsvLine(bufReader);
		if (nextCsvLine == null)
		{
			// Nothing more to read or blank line
			bufReader.close();
			return null;
		}
		if(first)
		{
			// Header - get column separator
			this.columnSeparator = findColumnSeparator(nextCsvLine);
		}
		Vector<String> valuesToImport = splitString(nextCsvLine, columnSeparator, '\"', true, false);
		return valuesToImport;
	}

	/**
	 * Read next non blank line.
	 * 
	 * @return The next non blank line or null if end of file
	 * @throws IOException
	 *           Exception
	 */
	private String readOneCsvLine(BufferedReader bufReader) throws IOException
	{
		String nextCsvLine = readCsvLine(bufReader);
		if (nextCsvLine == null)
		{
			return null;
		}
		if ("".equals(nextCsvLine.trim()))
		{
			// Blank - read next
			return readOneCsvLine(bufReader);
		}
		return nextCsvLine;
	}

	/**
	 * Reads a line from a csv file, the item may be enclosed with "" that may
	 * require several rows to be concatenated.
	 * 
	 * @param bufReader
	 * @return Next line or null if no next line
	 * @throws IOException
	 */
	private String readCsvLine(BufferedReader bufReader) throws IOException
	{
		StringBuffer buf = new StringBuffer();
		while (true)
		{
			String thisLine = bufReader.readLine();
			if (thisLine == null)
			{
				if (buf.length() == 0)
				{
					return null;
				}
				else
				{
					return buf.toString();
				}
			}
			else
			{
				// Add to nextLine
				if (buf.length() > 0)
				{
					buf.append("\n");
				}
				buf.append(thisLine);
				if (countChars(buf.toString(), '\"') % 2 == 0)
				{
					// An even number of " - done
					return buf.toString();
				}
			}
		}
	}

	/**
	 * @param s
	 *          String to split
	 * @param delimiter
	 *          Character to split with
	 * @param protectDelimiter
	 *          Character to protect delimiter with
	 * @param addEmpty
	 *          true if empty parts should be added
	 * @param addProtection
	 *          true if protector characters should be in Vector
	 * @return A vector of stings split with a delimiter that can be protected by
	 *         a protectDelimiter character (for example ") Example: command
	 *         "hello you" insert => command + hello you + insert
	 */
	private Vector<String> splitString(String s, char delimiter, char protectDelimiter, boolean addEmpty, boolean addProtection)
	{
		Vector<String> v = new Vector<String>();
		StringBuffer b = new StringBuffer();
		int isProtected = 0;
		int isSubProtected = 0;
		char lastChar = ' ';

		for (int i = 0; i < s.length(); i++)
		{
			char nextChar = s.charAt(i);
			if (isProtected == 0 && isSubProtected == 0 && nextChar == delimiter)
			{
				// Unprotected and delimiter
				if (b.length() == 0)
				{
					// Empty - only add if addEmpty is true
					if (addEmpty)
					{
						// Add this to the vector
						v.add(b.toString().trim());
						b = new StringBuffer();
					}
				}
				else
				{
					// Add this to the vector
					v.add(b.toString().trim());
					b = new StringBuffer();
				}
			}
			else if (nextChar == protectDelimiter && isSubProtected == 0)
			{
				// Toggle protection
				if (isProtected == 0)
				{
					isProtected = 1;
				}
				else
				{
					isProtected = 0;
				}
				if (addProtection)
				{
					b.append(nextChar);
				}
				else if (lastChar == nextChar)
				{
					// Add double protection as one protection char
					b.append(nextChar);
					// Clear nextChar to avoid adding protection again
					nextChar = ' ';
				}
			}
			else
			{
				b.append(nextChar);
			}
			// Remember last char
			lastChar = nextChar;
		}

		// Add the last piece to the vector
		if (b.length() == 0)
		{
			// Empty - only add if addEmpty is true
			if (addEmpty)
			{
				// Add this to the vector
				v.add(b.toString().trim());
			}
		}
		else
		{
			// Add this to the vector
			v.add(b.toString().trim());
		}

		return v;
	}

	/**
	 * Returns the number of times c appears in str.
	 * 
	 * @param str
	 *          Source string.
	 * @param c
	 *          Char to search for.
	 * @return Number of times c appears in str.
	 */
	private int countChars(String str, char c)
	{
		int count = 0;
		for (int i = 0; i < str.length(); i++)
		{
			if (str.charAt(i) == c)
			{
				count++;
			}
		}
		return count;
	}

	/**
	 * The column separator used (comma by default).
	 * 
	 * @return The current column separator.
	 */
	public char getColumnSeparator()
	{
		return columnSeparator;
	}

	public List<String> getHeaderLine()
	{
		return headerLine;
	}
}
