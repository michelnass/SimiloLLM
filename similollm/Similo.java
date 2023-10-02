package similollm;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Similo
{
	private static String[] LOCATORS =         { "tag", "class", "name", "id", "href", "alt", "xpath", "idxpath", "is_button", "location", "area", "shape", "visible_text", "neighbor_text" };
	private double[] WEIGHTS =                 { 1.5,   0.5,     1.5,    1.5,  0.5,    0.5,   0.5,     0.5,       0.5,         0.5,        0.5,    0.5,     1.5,            1.5 };
	private static int[] SIMILARITY_FUNCTION = { 0,     1,       0,      0,    1,      1,     1,       1,         0,           3,          2,      2,       1,              4 };
	private static boolean[] IS_OVERPAPPING =  { true,  true,    true,   true, true,   true,  true,    true,      true,        false,      false,  false,   true,           false };

	private static int NO_THREADS = 20;

	private enum Comparator {EQUAL, NOT_EQUAL, GREATER, LESS, GREATER_OR_EQUAL, LESS_OR_EQUAL, CONTAINS};
	private final static String[] comparatorStrings={"!=", ">=", "<=", "*=", "=", ">", "<"};
	private final static Comparator[] comparators={Comparator.NOT_EQUAL, Comparator.GREATER_OR_EQUAL, Comparator.LESS_OR_EQUAL, Comparator.CONTAINS, Comparator.EQUAL, Comparator.GREATER, Comparator.LESS};
	private static String[] ckecksumProperties = { "visible_text", "x", "y", "width", "height" };
	private static String[] ckecksumPropertiesDiff = { "xpath" };
	private static String[] ckecksumPropertiesAll = { "tag", "class", "name", "href", "alt", "visible_text", "x", "y", "width", "height", "xpath" };	// src
	private static String[] checksumTags={ "h1", "h2", "h3", "h4", "h5", "h6", "label", "input", "textarea", "button", "select", "a", "span", "div", "li", "th", "tr", "td", "label", "svg" };	// img

	private WebDriver webDriver=null;
	private String propertiesFolder="locators";
	private int timeout=30;
	private int stepDelay=10;
	private int stableDelay=3;
	private WebDriverWait wait;
	private Properties latestProperties=null;
	private String elementsToExtract="input,textarea,button,select,a,h1,h2,h3,h4,h5,h6,li,span,div,p,th,tr,td,label,svg";

	private int minScore=100;
	private boolean logOn=true;
	private String previousChecksum=null;
	private long maxChecksumDiff=10;
	private String javascript=null;
	private String defaultProperty = "visible_text";
	private String prioritizedClickTags = "a || button || input";
	private String prioritizedTypeTags = "input || textarea";

	public Similo()
	{
	}

	/**
	 * Create a MultiLocator associated with Selenium WebDriver and the default folder for locator properties.
	 * @param webDriver
	 */
	public Similo(WebDriver webDriver)
	{
		this.webDriver = webDriver;
		File folder=new File(propertiesFolder);
		folder.mkdirs();
		wait=new WebDriverWait(webDriver, Duration.ofSeconds(timeout));
		javascript = loadTextFile("javascript.js");
	}

	/**
	 * Create a MultiLocator associated with Selenium WebDriver and a folder containing locator properties.
	 * @param webDriver
	 * @param propertiesFolder Path to a folder that contains the property files
	 */
	public Similo(WebDriver webDriver, String propertiesFolder)
	{
		this.webDriver = webDriver;
		this.propertiesFolder = propertiesFolder;
		File folder=new File(propertiesFolder);
		folder.mkdirs();
		wait=new WebDriverWait(webDriver, Duration.ofSeconds(timeout));
		javascript = loadTextFile("javascript.js");
	}

	/**
	 * Create a MultiLocator associated with Selenium WebDriver, a locator folder, and a Javascript file.
	 * @param webDriver
	 * @param propertiesFolder Path to a folder that contains the property files
	 * @javascriptFilename Filepath to the file that contains the javascript
	 */
	public Similo(WebDriver webDriver, String propertiesFolder, String javascriptFilename)
	{
		this.webDriver = webDriver;
		this.propertiesFolder = propertiesFolder;
		File folder=new File(propertiesFolder);
		folder.mkdirs();
		wait=new WebDriverWait(webDriver, Duration.ofSeconds(timeout));
		javascript = loadTextFile(javascriptFilename);
	}

	/**
	 * Find a WebElement using adaptive and self-healing multi-locators.
	 * Waits until the element is located and visible.
	 * Creates a new properties file containing a text property (equal to fileName) if the fileName does not exist.
	 * @param fileName Name of the file that contains the locators (excluding the .properties extension).
	 * @return A WebElement or null if not found
	 */
	public WebElement findElement(String fileName)
	{
		fileName = stripString(fileName);
		File propFile=new File(propertiesFolder, fileName+".properties");
		if(!propFile.exists())
		{
			createPropertiesFile(propFile, fileName, null);
		}
		return findElement(propFile);
	}

	/**
	 * Click a WebElement using adaptive and self-healing multi-locators.
	 * Waits until the element is located and visible.
	 * Creates a new properties file containing a text property (equal to fileName) if the fileName does not exist.
	 * @param fileName Name of the file that contains the locators (excluding the .properties extension).
	 * @return true if clicked or false if click failed
	 */
	public boolean click(String fileName)
	{
		fileName = stripString(fileName);
		File propFile=new File(propertiesFolder, fileName+".properties");
		if(!propFile.exists())
		{
			createPropertiesFile(propFile, fileName, prioritizedClickTags);
		}
		return click(propFile);
	}

	/**
	 * Types a text into a WebElement using adaptive and self-healing multi-locators.
	 * Waits until the element is located and visible.
	 * Creates a new properties file containing a text property (equal to fileName) if the fileName does not exist.
	 * @param fileName Name of the file that contains the locators (excluding the .properties extension).
	 * @param textToType The text to type in the located web element
	 * @return true if types or false if type failed
	 */
	public boolean type(String fileName, String textToType)
	{
		fileName = stripString(fileName);
		File propFile=new File(propertiesFolder, fileName+".properties");
		if(!propFile.exists())
		{
			createPropertiesFile(propFile, fileName, prioritizedTypeTags);
		}
		return type(propFile, textToType);
	}

	/**
	 * Enters a text (adds a return at the end) into a WebElement using adaptive and self-healing multi-locators.
	 * Waits until the element is located and visible.
	 * Creates a new properties file containing a text property (equal to fileName) if the fileName does not exist.
	 * @param fileName Name of the file that contains the locators (excluding the .properties extension).
	 * @param textToType The text to type in the located web element
	 * @return true if types or false if type failed
	 */
	public boolean enter(String fileName, String textToType)
	{
		fileName = stripString(fileName);
		File propFile=new File(propertiesFolder, fileName+".properties");
		if(!propFile.exists())
		{
			createPropertiesFile(propFile, fileName, prioritizedTypeTags);
		}
		return enter(propFile, textToType);
	}

	/**
	 * Find a WebElement using adaptive and self-healing multi-locators.
	 * Waits until the element is located and visible.
	 * @param file Path to a properties file that contains the locators
	 * @return A WebElement or null if not found
	 */
	public WebElement findElement(File file)
	{
		latestProperties=loadProperties(file);
		Properties clone=(Properties)latestProperties.clone();
		WebElement element=findElement(latestProperties);
		if(element!=null)
		{
			if(!latestProperties.equals(clone))
			{
				saveProperties(file, latestProperties);
			}
			return element;
		}
		return null;
	}

	/**
	 * Clicks a WebElement using adaptive and self-healing multi-locators.
	 * Waits until the element is located and visible.
	 * @param file Path to a properties file that contains the locators
	 * @return true if clicked or false if click failed
	 */
	public boolean click(File file)
	{
		latestProperties=loadProperties(file);
		Properties clone=(Properties)latestProperties.clone();
		if(clickElement(latestProperties))
		{
			if(!latestProperties.equals(clone))
			{
				saveProperties(file, latestProperties);
			}
			return true;
		}
		return false;
	}

	/**
	 * Types a text into a WebElement using adaptive and self-healing multi-locators.
	 * Waits until the element is located and visible.
	 * @param fileName Name of the file that contains the locators (excluding the .properties extension).
	 * @param textToType The text to type in the located web element
	 * @return true if types or false if type failed
	 */
	public boolean type(File file, String textToType)
	{
		latestProperties=loadProperties(file);
		Properties clone=(Properties)latestProperties.clone();
		if(typeElement(latestProperties, textToType, false))
		{
			if(!latestProperties.equals(clone))
			{
				saveProperties(file, latestProperties);
			}
			return true;
		}
		return false;
	}

	/**
	 * Enters a text (adds a return at the end) into a WebElement using adaptive and self-healing multi-locators.
	 * Waits until the element is located and visible.
	 * @param fileName Name of the file that contains the locators (excluding the .properties extension).
	 * @param textToType The text to type in the located web element
	 * @return true if types or false if type failed
	 */
	public boolean enter(File file, String textToType)
	{
		latestProperties=loadProperties(file);
		Properties clone=(Properties)latestProperties.clone();
		if(typeElement(latestProperties, textToType, true))
		{
			if(!latestProperties.equals(clone))
			{
				saveProperties(file, latestProperties);
			}
			return true;
		}
		return false;
	}

	/**
	 * Find and checks a WebElement using adaptive and self-healing multi-locators.
	 * Waits until the element is located and visible.
	 * Checks the validExpression to determine if the web element is valid.
	 * Valid comparison operators are: =, >, <, !=, >=, <=, and *= (contains).
	 * Creates a new properties file containing a text property (equal to fileName) if the fileName does not exist.
	 * @param fileName Property value to find (defaultProperty) and name of property file.
	 * @return true if the web element can be found and is valid
	 */
	public boolean check(String fileName)
	{
		fileName = stripString(fileName);
		File propFile=new File(propertiesFolder, fileName+".properties");
		if(!propFile.exists())
		{
			createPropertiesFile(propFile, fileName, null);
		}
		return check(propFile, "{" + defaultProperty + "}="+fileName);
	}

	/**
	 * Find and checks a WebElement using adaptive and self-healing multi-locators.
	 * Waits until the element is located and visible.
	 * Checks the validExpression to determine if the web element is valid.
	 * Valid comparison operators are: =, >, <, !=, >=, <=, and *= (contains).
	 * Creates a new properties file containing a text property (equal to fileName) if the fileName does not exist.
	 * @param fileName Name of the file that contains the locators (excluding the .properties extension).
	 * @param validExpression An expression that determines if the web element is valid (for example: {text}=Name, {width}>100 or {text}*=contains)
	 * @return true if the web element can be found and is valid
	 */
	public boolean check(String fileName, String validExpression)
	{
		fileName = stripString(fileName);
		File propFile=new File(propertiesFolder, fileName+".properties");
		if(!propFile.exists())
		{
			createPropertiesFile(propFile, fileName, null);
		}
		return check(propFile, validExpression);
	}
	
	/**
	 * Find and checks a WebElement using adaptive and self-healing multi-locators.
	 * Waits until the element is located and visible.
	 * Checks the validExpression to determine if the web element is valid.
	 * Valid comparison operators are: =, >, <, !=, >=, <=, and *= (contains).
	 * @param file Path to a properties file that contains the locators
	 * @param validExpression An expression that determines if the web element is valid (for example: {text}=Name, {width}>100 or {text}*=contains)
	 * @return true if the web element can be found and is valid
	 */
	public boolean check(File file, String validExpression)
	{
		WebElement element=findElement(file);
		if(element==null)
		{
			return false;
		}
		return isValid(validExpression, latestProperties);
	}

	/**
	 * Find and checks a WebElement using adaptive and self-healing multi-locators.
	 * Waits until the element is located and visible.
	 * Checks the validExpression to determine if the web element is valid.
	 * Valid comparison operators are: =, >, <, !=, >=, <=, and *= (contains).
	 * @param properties Contains the locators (will be updated if required) 
	 * @param validExpression An expression that determines if the web element is valid (for example: {text}=Name, {width}>100 or {text}*=contains)
	 * @return true if the web element can be found and is valid
	 */
	public boolean check(Properties properties, String validExpression)
	{
		WebElement element=findElement(properties);
		if(element==null)
		{
			return false;
		}
		return isValid(validExpression, properties);
	}

	/**
	 * Find a WebElement using adaptive and self-healing multi-locators.
	 * Waits until the element is located and visible.
	 * @param properties Contains the locators (will be updated if required) 
	 * @return A WebElement or null if not found
	 */
	public WebElement findElement(Properties properties)
	{
		stepDelay(properties);
		
		Locator locator=new Locator(properties);
		long startTime=System.currentTimeMillis();
		for(int i=0; i<1000; i++)
		{
			List<Locator> bestMatchingLocators=findBestMatchingLocators(locator);
			if(bestMatchingLocators!=null && bestMatchingLocators.size()>0)
			{
				Locator bestMatchingLocator = bestMatchingLocators.get(0);

				try
				{
					String xpath=(String)bestMatchingLocator.getMetadata("xpath");
					if(xpath!=null && xpath.trim().length()>0)
					{
						WebElement element=webDriver.findElement(By.xpath(xpath));
						if(element != null)
						{
							WebElement webElement=wait.until(ExpectedConditions.visibilityOf(element));
							
							// Repair if needed
							repairLocator(locator, bestMatchingLocator);

							// Update the GUI state checksum
//							String checksum=""+createGuiStateChecksum();
//							properties.setProperty("gui_state_checksum", checksum);

							return webElement;
						}
					}
				}
				catch(Exception e)
				{								
				}
			}
			delay(1000);
			long duration=(System.currentTimeMillis()-startTime)/1000;
			if(duration>=timeout)
			{
				return null;
			}
		}
		return null;
	}

	public boolean clickElement(Properties properties)
	{
		stepDelay(properties);
		
		Locator locator=new Locator(properties);
		long startTime=System.currentTimeMillis();
		for(int i=0; i<1000; i++)
		{
			List<Locator> bestMatchingLocators=findBestMatchingLocators(locator);
			if(bestMatchingLocators!=null)
			{
				// Try to click on elements in order of similarity
				for(Locator bestMatchingLocator:bestMatchingLocators)
				{
					String xpath=(String)bestMatchingLocator.getMetadata("xpath");
					if(xpath!=null && xpath.trim().length()>0)
					{
						WebElement element=webDriver.findElement(By.xpath(xpath));
						if(element != null)
						{
							try
							{
//								WebElement webElement=wait.until(ExpectedConditions.visibilityOf(element));
								element.click();

								// Repair if needed
								repairLocator(locator, bestMatchingLocator);

								// Update the GUI state checksum
//								String checksum=""+createGuiStateChecksum();
//								properties.setProperty("gui_state_checksum", checksum);

								return true;
							}
							catch(Exception e)
							{
								// Try again
								try
								{
//									WebElement webElement=wait.until(ExpectedConditions.visibilityOf(element));
									element.click();

									// Repair if needed
									repairLocator(locator, bestMatchingLocator);

									// Update the GUI state checksum
//									String checksum=""+createGuiStateChecksum();
//									properties.setProperty("gui_state_checksum", checksum);
	
									return true;
								}
								catch(Exception ex)
								{								
								}
							}
						}
					}
				}
			}
			delay(1000);
			long duration=(System.currentTimeMillis()-startTime)/1000;
			if(duration>=timeout)
			{
				return false;
			}
		}
		return false;
	}

	public boolean typeElement(Properties properties, String textToType, boolean addReturn)
	{
		stepDelay(properties);
		
		Locator locator=new Locator(properties);
		long startTime=System.currentTimeMillis();
		for(int i=0; i<1000; i++)
		{
			List<Locator> bestMatchingLocators=findBestMatchingLocators(locator);
			if(bestMatchingLocators!=null)
			{
				// Try to click on elements in order of similarity
				for(Locator bestMatchingLocator:bestMatchingLocators)
				{
					String xpath=(String)bestMatchingLocator.getMetadata("xpath");
					if(xpath!=null && xpath.trim().length()>0)
					{
						WebElement element=webDriver.findElement(By.xpath(xpath));
						if(element != null)
						{
							try
							{
								element.sendKeys(textToType);
								if(addReturn)
								{
									element.sendKeys(Keys.RETURN);
								}

								// Repair if needed
								repairLocator(locator, bestMatchingLocator);

								// Update the GUI state checksum
//								String checksum=""+createGuiStateChecksum();
//								properties.setProperty("gui_state_checksum", checksum);

								return true;
							}
							catch(Exception e)
							{
								// Try again
								try
								{
									element.sendKeys(textToType);
									if(addReturn)
									{
										element.sendKeys(Keys.RETURN);
									}

									// Repair if needed
									repairLocator(locator, bestMatchingLocator);

									// Update the GUI state checksum
//									String checksum=""+createGuiStateChecksum();
//									properties.setProperty("gui_state_checksum", checksum);

									return true;
								}
								catch(Exception ex)
								{								
								}
							}
						}
					}
				}
			}
			delay(1000);
			long duration=(System.currentTimeMillis()-startTime)/1000;
			if(duration>=timeout)
			{
				return false;
			}
		}
		return false;
	}

	private List<Locator> findBestMatchingLocators(Locator targetLocator)
	{
		if(webDriver==null)
		{
			return null;
		}
		try
		{
			String xStr=(String)targetLocator.getMetadata("x");
			String yStr=(String)targetLocator.getMetadata("y");
			String widthStr=(String)targetLocator.getMetadata("width");
			String heightStr=(String)targetLocator.getMetadata("height");
			if(xStr!=null && yStr!=null && widthStr!=null && heightStr!=null)
			{
				// Set the location area to speed up comparisons
				int x=string2Int(xStr);
				int y=string2Int(yStr);
				int width=string2Int(widthStr);
				int height=string2Int(heightStr);
				targetLocator.setLocationArea(new Rectangle(x, y, width, height));
			}

			String elementsToExtract=getElementsToExtract();
			List<Locator> candidateLocators=getLocators(elementsToExtract);
			List<Locator> bestMatchingLocators=similo(targetLocator, candidateLocators);
			return bestMatchingLocators;
		}
		catch(Exception e)
		{
		}

		return null;
	}

	private String object2String(Object o)
	{
		if(o==null)
		{
			return null;
		}
		if(o instanceof String)
		{
			String s=(String)o;
			return s.trim();
		}
		else if(o instanceof Integer)
		{
			Integer i=(Integer)o;
			return i.toString();
		}
		if(o instanceof Double)
		{
			Double d=(Double)o;
			int i=d.intValue();
			return ""+i;
		}
		else if(o instanceof Long)
		{
			Long l=(Long)o;
			return l.toString();
		}
		return null;
	}

	private int string2Int(String text)
	{
		try
		{
			return Integer.parseInt(text);
		}
		catch(Exception e)
		{
			return 0;
		}
	}

	private long string2Long(String text)
	{
		try
		{
			return Long.parseLong(text);
		}
		catch(Exception e)
		{
			return 0;
		}
	}

	/**
	 * Delay the thread a number of milliseconds
	 * @param milliseconds
	 */
	public void delay(int milliseconds)
	{
		try
		{
			Thread.sleep(milliseconds);
		}
		catch (InterruptedException e)
		{
		}
	}

	private void repairLocator(Locator locator, Locator repairFromWidget)
	{
		List<String> keys=repairFromWidget.getMetadataKeys();

		// Check if metadata should be ignored
		for(String key:keys)
		{
			String value1=locator.getMetadata(key);
			String value2=repairFromWidget.getMetadata(key);
			if(value1==null || !value1.equals(value2))
			{
				// Is missing or has changed
				if(locator.isRepairedMetadata(key))
				{
					// Repaired last time - time to ignore
					locator.addIgnoredMetadata(key);
				}
			}
		}

		// Check if metadata should be repaired
		locator.clearRepairedMetadata();
		for(String key:keys)
		{
			String value1=locator.getMetadata(key);
			String value2=repairFromWidget.getMetadata(key);
			if(value1==null)
			{
				// Is missing - add
				locator.putMetadata(key, value2);
			}
			else if(!value1.equals(value2))
			{
				// Has changed - repair
				locator.putMetadata(key, value2);
				locator.addRepairedMetadata(key);
			}
		}

		// Check if metadata should be removed
		keys=locator.getMetadataKeys();
		for(String key:keys)
		{
			if(!key.startsWith("gui_state_"))
			{
				if(!"repaired".equalsIgnoreCase(key) && !"ignored".equalsIgnoreCase(key))
				{
					if(!locator.isIgnoredMetadata(key) && !locator.isRepairedMetadata(key))
					{
						// Neither ignored or repaired
						String value1=locator.getMetadata(key);
						String value2=repairFromWidget.getMetadata(key);
						if(!isBlank(value1) && isBlank(value2))
						{
							// Exists in current locator but missing in repair from - remove
							locator.removeMetadata(key);
						}
					}
				}
			}
		}
	}
	
	private boolean isBlank(String value)
	{
		if(value==null)
		{
			return true;
		}
		String trimmedValue=value.trim();
		return trimmedValue.length()==0;
	}

	/**
	 * Get all locators that belong to any of the tags in elementsToExtract
	 * @return A list of locators to web elements
	 */
	public List<Locator> getLocators()
	{
		return getLocators(getElementsToExtract());
	}

	/**
	 * Get all locators that belong to any of the tags in elementsToExtract
	 * @return A list of locators to web elements
	 */
	public List<Locator> getLocators(String elementsToExtract)
	{
		List<Locator> locators=new ArrayList<Locator>();
		
		if(webDriver!=null)
		{
			try
			{
				webDriver.manage().timeouts().setScriptTimeout(300, TimeUnit.SECONDS);
				JavascriptExecutor executor = (JavascriptExecutor) webDriver;
				Object object=executor.executeScript(javascript +
					"var result = []; " +
					"var all = document.querySelectorAll('"+elementsToExtract+"'); " +
					"for (var i=0, max=all.length; i < max; i++) { " +
					"    if (elementIsVisible(all[i])) result.push({'tag': all[i].tagName, 'class': all[i].className, 'type': all[i].type, 'name': all[i].name, 'id': all[i].id, 'value': all[i].value, 'href': all[i].href, 'text': all[i].textContent, 'placeholder': all[i].placeholder, 'title': all[i].title, 'alt': all[i].alt, 'x': getXPosition(all[i]), 'y': getYPosition(all[i]), 'width': getMaxWidth(all[i]), 'height': getMaxHeight(all[i]), 'children': all[i].children.length, 'xpath': getXPath(all[i]), 'idxpath': getIdXPath(all[i])}); " +
					"} " +
					" return JSON.stringify(result); ");				
				
				
				String json=object.toString();
				JSONParser parser = new JSONParser();
				JSONArray jsonArray = (JSONArray)parser.parse(json);

				for(int i=0; i<jsonArray.size(); i++)
				{
					JSONObject jsonObject=(JSONObject)jsonArray.get(i);

					String tag=object2String(jsonObject.get("tag"));
					if(tag!=null)
					{
						tag=tag.toLowerCase();
					}
					String className=object2String(jsonObject.get("class"));
					String type=object2String(jsonObject.get("type"));
					String name=object2String(jsonObject.get("name"));
					String id=object2String(jsonObject.get("id"));
					String value=object2String(jsonObject.get("value"));
					String href=object2String(jsonObject.get("href"));
					String text=object2String(jsonObject.get("text"));
					String placeholder=object2String(jsonObject.get("placeholder"));
					String title=object2String(jsonObject.get("title"));
					String alt=object2String(jsonObject.get("alt"));
					String xpath=object2String(jsonObject.get("xpath"));
					String idxpath=object2String(jsonObject.get("idxpath"));
					String xStr=object2String(jsonObject.get("x"));
					String yStr=object2String(jsonObject.get("y"));
					String widthStr=object2String(jsonObject.get("width"));
					String heightStr=object2String(jsonObject.get("height"));

					int x=string2Int(xStr);
					int y=string2Int(yStr);
					int width=string2Int(widthStr);
					int height=string2Int(heightStr);

					if(width>0 && height>0)
					{
						Locator locator=new Locator();

						locator.setLocationArea(new Rectangle(x, y, width, height));
						locator.setX(x);
						locator.setY(y);
						locator.setWidth(width);
						locator.setHeight(height);

						addMetadata(locator, "tag", tag);
						addMetadata(locator, "class", className);
						addMetadata(locator, "type", type);
						addMetadata(locator, "name", name);
						addMetadata(locator, "id", id);
						addMetadata(locator, "value", value);
						addMetadata(locator, "href", href);
//						if(isValidText(text))
						{
							addMetadata(locator, "text", stripString(truncate(text)));
						}
						addMetadata(locator, "placeholder", placeholder);
						addMetadata(locator, "title", title);
						addMetadata(locator, "alt", alt);
						addMetadata(locator, "xpath", xpath);
						addMetadata(locator, "idxpath", idxpath);
						addMetadata(locator, "x", xStr);
						addMetadata(locator, "y", yStr);
						addMetadata(locator, "height", heightStr);
						addMetadata(locator, "width", widthStr);

						int area = width * height;
						int shape = (width * 100) / height;
						addMetadata(locator, "area", ""+area);
 						addMetadata(locator, "shape", ""+shape);

						String visibleText=locator.getVisibleText();
						if(visibleText!=null)
						{
							locator.putMetadata("visible_text", visibleText);
						}
						String isButton=isButton(tag, type, className)?"yes":"no";;
						locator.putMetadata("is_button", isButton);

						locators.add(locator);
					}
				}
				
				for(Locator locator:locators)
				{
					addNeighborText(locator, locators);
					double maxScore = calcMaxSimilarityScore(locator);
					locator.setMaxScore(maxScore);
					addOverlappingLocatorParameters(locator, locators);
				}
				
				// Remove duplicates
//				locators = removeIdenticalLocators(locators);
				
				return locators;
			}
			catch (Exception e)
			{
				return null;
			}
		}

		return null;
	}

	private String truncate(String text)
	{
		if(text==null)
		{
			return null;
		}
		if(text.length()>100)
		{
			return text.substring(0, 99);
		}
		return text;
	}

	private void addMetadata(Locator locator, String key, String value)
	{
		if(value!=null && value.length()>0)
		{
//			String lowercaseValue = value.toLowerCase();
//			locator.putMetadata(key, lowercaseValue);
			locator.putMetadata(key, value);
		}
	}

	private boolean isButton(String tag, String type, String className)
	{
		if(tag==null)
		{
			return false;
		}
		if(tag.equalsIgnoreCase("a") && className!=null && className.indexOf("btn")>=0)
		{
			return true;
		}
		if(tag.equalsIgnoreCase("button"))
		{
			return true;
		}
		if(tag.equalsIgnoreCase("input") && ("button".equalsIgnoreCase(type) || "submit".equalsIgnoreCase(type) || "reset".equalsIgnoreCase(type)))
		{
			return true;
		}
		return false;
	}

	/**
	 * Get the max time to wait for a web element
	 * @param timeout Time in seconds (20 by default)
	 */
	public int getTimeout()
	{
		return timeout;
	}

	/**
	 * Set the max time to wait for a web element
	 * @param timeout Time in seconds (20 by default)
	 */
	public void setTimeout(int timeout)
	{
		this.timeout = timeout;
	}

	/**
	 * Get the max time to wait for the correct GUI state (as last run)
	 * @param timeout Time in seconds (10 by default)
	 */
	public int getStepDelay()
	{
		return stepDelay;
	}

	/**
	 * Set the max time to wait for the correct GUI state (as last run)
	 * @param timeout Time in seconds (10 by default)
	 */
	public void setStepDelay(int delay)
	{
		this.stepDelay = delay;
	}

	/**
	 * Load or save cookies from file
	 * @param fileName - Name of cookie file
	 * @param webDriver
	 */
	public void manageCookies(String fileName, WebDriver webDriver)
	{
		File propFile=new File(propertiesFolder, fileName+".cookies");
		if(!propFile.exists())
		{
			delay(stepDelay*1000);
			saveCookies(propFile.getAbsolutePath(), webDriver);
		}
		else
		{
			loadCookies(propFile.getAbsolutePath(), webDriver);
		}
	}

	/**
	 * Load cookies from file
	 * @param filepath - Path to cookie file
	 * @param webDriver
	 */
	public void loadCookies(String filepath, WebDriver webDriver)
	{
		try
		{
			Object object=loadObject(filepath);
			if(object!=null)
			{
				Set<Cookie> cookies=(Set<Cookie>)object;
				for(Cookie cookie:cookies)
				{
					try
					{
						webDriver.manage().addCookie(cookie);
					}
					catch(Exception e)
					{
					}
				}
			}
			webDriver.navigate().refresh();
			delay(1000);
		}
		catch (Throwable e)
		{
		}
	}

	/**
	 * Save cookies to file
	 * @param filepath - Path to cookie file
	 * @param webDriver
	 * @return true if saved
	 */
	public boolean saveCookies(String filepath, WebDriver webDriver)
	{
		try
		{
			Set<Cookie> cookies=webDriver.manage().getCookies();
			return saveObject(filepath, cookies);
		}
		catch (Throwable e)
		{
			return false;
		}
	}

	private Object loadObject(String filepath)
	{
		try
		{
			FileInputStream fileIn = new FileInputStream(filepath);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			Object object = in.readObject();
			in.close();
			fileIn.close();
			return object;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	private boolean saveObject(String filepath, Object object)
	{
		try
		{
			FileOutputStream fileOut = new FileOutputStream(filepath);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(object);
			out.close();
			fileOut.close();
			fileOut.getFD().sync();
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * @return True if the statement is true (valid)
	 */
	private boolean isValid(String statement, Properties p)
	{
		if(statement==null || "".equals(statement.trim()))
		{
			// Nothing to evaluate
			return true;
		}

		for(int i=0; i<comparatorStrings.length; i++)
		{
			List<String> params=splitString(statement, comparatorStrings[i]);
			if(params.size()==2)
			{
				// Has been split in two - comparator found
				Comparator comparator=comparators[i];
				String leftParam=evaluateParameter(((String)params.get(0)).trim(), p);
				String rightParam=evaluateParameter(((String)params.get(1)).trim(), p);
				return compare(comparator, leftParam, rightParam);
			}
		}

		// No comparator found
		return false;
	}

	private boolean compare(Comparator comparator, String left, String right)
	{
		if(isFloatString(left) && isFloatString(right))
		{
			// Compare as Double values
			double leftDouble=string2Double(left);
			double rightDouble=string2Double(right);
			switch(comparator)
			{
				case EQUAL:
				case CONTAINS:
					return leftDouble==rightDouble;
				case NOT_EQUAL:
					return leftDouble!=rightDouble;
				case GREATER:
					return leftDouble>rightDouble;
				case LESS:
					return leftDouble<rightDouble;
				case GREATER_OR_EQUAL:
					return leftDouble>=rightDouble;
				case LESS_OR_EQUAL:
					return leftDouble<=rightDouble;
				default:
					return false;
			}
		}
		else
		{
			left=stripString(left).toLowerCase();
			right=stripString(right).toLowerCase();
			switch(comparator)
			{
				case EQUAL:
					return left.equals(right);
				case NOT_EQUAL:
					return !left.equals(right);
				case GREATER:
					return left.compareTo(right)>0;
				case LESS:
					return left.compareTo(right)<0;
				case GREATER_OR_EQUAL:
					return left.compareTo(right)>=0;
				case LESS_OR_EQUAL:
					return left.compareTo(right)<=0;
				case CONTAINS:
					return left.indexOf(right)>=0;
				default:
					return false;
			}
		}
	}

	/**
	 * Returns the value of param
	 * @param param
	 * @return Value of param or param if not a parameter
	 */
	private String evaluateParameter(String param, Properties p)
	{
		if(param.startsWith("{") && param.endsWith("}"))
		{
			// Within parentheses
			String within=param.substring(1, param.length()-1);
			String value=(String)p.get(within);
			if(value!=null)
			{
				return value.trim();
			}
			else
			{
				// Not a parameter
				return param;
			}
		}
		else
		{
			// Not a parameter
			return param;
		}
	}

	private boolean isFloatString(String s)
	{
		String text = s.trim();
		int startIndex=0;
		if(text.length()>0 && (text.charAt(0)=='-' || text.charAt(0)=='+'))
		{
			// Begins with a - or plus - ok
			startIndex=1;
		}
		for (int i = startIndex; i < text.length(); i++)
		{
			char c = text.charAt(i);
			if (Character.isDigit(c) || c == '.')
			{
				// Valid
			}
			else
			{
				return false;
			}
		}
		return true;
	}

	private double string2Double(String s)
	{
		return string2Double(s, 0.0);
	}

	private double string2Double(String s, double otherwise)
	{
		if (s == null || "".equals(s))
		{
			return otherwise;
		}

		try
		{
			return Double.parseDouble(s);
		} catch (Exception e)
		{
			// Couldn't convert to double
			return otherwise;
		}
	}

	/**
	 * Split the String s in two pieces with delimiter
	 * Will always return a List of two Strings if delimiter was found
	 * one item if delimiter not found
	 * @param s
	 * @param delimiter
	 * @return A list of two strings
	 */
	private List<String> splitString(String s, String delimiter)
	{
		List<String> v=new ArrayList<String>();
		for(int i=0; i<s.length(); i++)
		{
			int pos=s.indexOf(delimiter);
			if(pos>-1)
			{
				// Delimiter found - split
				v.add(s.substring(0, pos));
				if(pos+delimiter.length()>=s.length())
				{
					v.add("");
				}
				else
				{
					v.add(s.substring(pos+delimiter.length()));
				}
				return v;
			}
		}
		v.add(s);
		return v;
	}

	/**
	 * Clear local browser storage
	 */
	public void clearLocalStorage()
	{
		if(webDriver!=null)
		{
			try
			{
				JavascriptExecutor executor = (JavascriptExecutor) webDriver;
				executor.executeScript("localStorage.clear();");
			}
			catch (Exception e)
			{
			}
		}
	}

	private boolean isValidText(String text)
	{
		if(text==null)
		{
			return false;
		}
		String trimmedText=text.trim();
		if(trimmedText.length()<3 || trimmedText.length()>100)
		{
			// Too short or too long
			return false;
		}
		if(trimmedText.indexOf('\n')>=0)
		{
			// Contains newline
			return false;
		}
		if(trimmedText.indexOf('\t')>=0)
		{
			// Contains tab
			return false;
		}
		return true;
	}

	private Properties loadProperties(File file)
	{
		Properties properties=new Properties();
		try
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
			String str;
			while ((str = in.readLine()) != null)
			{
				String[] split=str.split("=", 2);
				if(split.length==2)
				{
					properties.put(split[0], split[1]);
				}
			}
			in.close();
			return properties;
		}
		catch (Exception e)
		{
			// File not found
			return null;
		}
	}

	private boolean saveProperties(File file, Properties properties)
	{
		try
		{
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
			for(Object keyObject:properties.keySet())
			{
				String key=(String)keyObject;
				String value=(String)properties.getProperty(key);
				out.write(key+"="+value);
				out.newLine();
			}
			out.close();
		}
		catch(Exception e)
		{
			return false;
		}
		return true;
	}

	/**
	 * Get a comma separated list of web elements that should be fetches as candidates
	 * Default: input,textarea,button,select,a,h1,h2,h3,h4,h5,li,span,div,p,th,tr,td,label,svg
	 * @return A comma separated list of web elements
	 */
	public String getElementsToExtract()
	{
		return elementsToExtract;
	}

	/**
	 * Set the web elements that should be fetches as candidates
	 * @param elementsToExtract A comma separated list of web elements
	 */
	public void setElementsToExtract(String elementsToExtract)
	{
		this.elementsToExtract = elementsToExtract;
	}

	/**
	 * Get the minimum score required for a match (100 by default)
	 * @return The min score
	 */
	public int getMinScore()
	{
		return minScore;
	}

	/**
	 * Set the minimum score required for a match
	 * @param minScore
	 */
	public void setMinScore(int minScore)
	{
		this.minScore = minScore;
	}

	public String getPropertiesFolder()
	{
		return propertiesFolder;
	}

	public void setPropertiesFolder(String propertiesFolder)
	{
		this.propertiesFolder = propertiesFolder;
	}

	private void createPropertiesFile(File propFile, String text, String prioritizedTags)
	{
		Properties properties=new Properties();
		properties.put(defaultProperty, text);
		if(prioritizedTags != null)
		{
			properties.put("tag", prioritizedTags);
		}
		saveProperties(propFile, properties);
	}

/*
	private void storeChecksum(Properties properties, List<Locator> availableLocators, String[] checksumTags, String propertyName)
	{
		String checksum=""+createGuiStateChecksum(availableLocators, checksumTags);
		properties.setProperty(propertyName, checksum);
	}

	private void storeChecksums(Properties properties)
	{
		List<Locator> availableLocators=getLocators(elementsToExtract);
		storeChecksum(properties, availableLocators, checksumTags, "gui_state_checksum");
	}
*/

	private boolean stepDelay(Properties properties)
	{
//		String guiStateChecksum=properties.getProperty("gui_state_checksum");
		String guiStateCommon=properties.getProperty("gui_state_common");
		if(guiStateCommon==null)
		{
			// No checksum - create one
			delay(stepDelay*1000);
			Set<Long> set=createStateSet();
			String checksum=setToString(set);
//			String checksum=""+createGuiStateChecksum();
			properties.setProperty("gui_state_common", checksum);
			return false;
		}

		Set<Long> setStateCommon=stringToSet(guiStateCommon);

		long startTime=System.currentTimeMillis();
		for(int i=0; i<stepDelay; i++)
		{
			Set<Long> set=createStateSet();
			if(set.containsAll(setStateCommon))
			{
				// In the next state
				return true;
			}

			long deltaTime=(System.currentTimeMillis()-startTime)/1000;
			if(deltaTime>=stepDelay)
			{
				// Done waiting - update the state
//				Set<Long> set=createStateSet();
				setStateCommon.retainAll(set);
				String checksum=setToString(setStateCommon);
				properties.setProperty("gui_state_common", checksum);
				return false;
			}
			delay(50);
		}

/*
		long startTime=System.currentTimeMillis();
		for(int i=0; i<stepDelay; i++)
		{
			String checksum=""+createGuiStateChecksum();
			long deltaTime=(System.currentTimeMillis()-startTime)/1000;
			long diff=Math.abs(string2Long(guiStateChecksum)-string2Long(checksum));
			if(diff<=maxChecksumDiff)
			{
				// Same GUI state checksum (or close)
//				log("Same checksum: "+checksum);
				return true;
			}
			else
			{
				if(previousChecksum!=null && deltaTime>=stableDelay)
				{
					diff=Math.abs(string2Long(previousChecksum)-string2Long(checksum));
					if(diff<=maxChecksumDiff)
					{
						// Checksum is stable and waited long enough
//						log("Checksum is stable: "+checksum);
						return true;
					}
				}
				previousChecksum=checksum;
//				log("Different checksum: "+guiStateChecksum+" vs "+checksum);
			}
			if(deltaTime>=stepDelay)
			{
				// Done waiting
				return false;
			}
			delay(1000);
		}
*/
		return false;
	}

	private long createGuiStateChecksum()
	{
		List<Locator> availableLocators=getLocators(elementsToExtract);

		List<String> tagList=Arrays.asList(checksumTags);
		long checksum=0;
		for(Locator availableLocator:availableLocators)
		{
			String tag=availableLocator.getMetadata("tag");
			if(tag!=null && tagList.contains(tag))
			{
				for(String ckecksumProperty:ckecksumProperties)
				{
					String value=availableLocator.getMetadata(ckecksumProperty);
					checksum+=hash(value);
				}
			}
		}
		return checksum;
	}

	private String setToString(Set<Long> set)
	{
		StringBuffer buf=new StringBuffer();
		List<Long> list=new ArrayList<>(set);
		for(Long item:list)
		{
			if(buf.length()>0)
			{
				buf.append(",");
			}
			buf.append(""+item);
		}
		return buf.toString();
	}

	private Set<Long> stringToSet(String text)
	{
		Set<Long> set=new HashSet<>();
		String[] splitted = text.split(Pattern.quote(","));
		for(String item:splitted)
		{
			long hashValue=Long.parseLong(item);
			set.add(hashValue);
		}
		return set;
	}

	private Set<Long> createInitialStateSet()
	{
		Set<Long> set=null;
		
		delay(5000);
		for(int i=0; i<5; i++)
		{
			if(set==null)
			{
				set=createStateSet();
			}
			else
			{
				// Create an intersection of the current and next set
				Set<Long> nextSet=createStateSet();
				set.retainAll(nextSet);
			}
			delay(2000);
		}
		
		return set;
	}

	private Set<Long> createStateSet()
	{
		try
		{
			Set<Long> set=new HashSet<>();
			List<Locator> availableLocators=getLocators(elementsToExtract);
			for(Locator locator:availableLocators)
			{
				long hashValue=createGuiStateChecksum(locator, checksumTags, ckecksumPropertiesAll);
				if(hashValue>0)
				{
					set.add(hashValue);
				}
			}
			return set;
		}
		catch(Exception e)
		{
			return null;
		}
	}

	private long createGuiStateChecksum(Locator availableLocator, String[] checksumTags, String[] ckecksumProperties)
	{
		List<String> tagList=Arrays.asList(checksumTags);
		long checksum=0;
		String tagMetaData=availableLocator.getMetadata("tag");
		String[] tags = getParameterValues(tagMetaData);
		for(String tag:tags)
		{
			if(tag!=null && tagList.contains(tag))
			{
				for(String ckecksumProperty:ckecksumProperties)
				{
					String value=availableLocator.getMetadata(ckecksumProperty);
					if(value!=null)
					{
						checksum+=hash(value);
					}
				}
			}
		}
		return checksum;
	}

/*
	private long createGuiStateChecksum()
	{
		List<Locator> availableLocators=getLocators(elementsToExtract);

		long checksum=0;
		for(Locator availableLocator:availableLocators)
		{
			for(String ckecksumProperty:ckecksumProperties)
			{
				String value=availableLocator.getMetadata(ckecksumProperty);
				checksum+=hash(value);
			}
		}
		return checksum;
	}
*/
	
	private long hash(String text)
	{
		if(text==null)
		{
			return 0;
		}
		long hash = 0;
		for (int i = 0; i < text.length(); i++)
		{
		    hash += text.charAt(i);
		}
		return hash;
	}

	private void log(String text)
	{
		if(!logOn)
		{
			return;
		}
		System.out.println(text);
		writeLine("Results.txt", text);
	}

	private void writeLine(String filename, String text)
	{
		String logMessage = text + "\r\n";
		File file = new File(filename);
		try
		{
			FileOutputStream o = new FileOutputStream(file, true);
			o.write(logMessage.getBytes());
			o.close();
		}
		catch (Exception e) {}
	}

	/**
	 * Get the minimum time to wait for a stable GUI state
	 * @return The time, in seconds, to wait for a stable GUI state
	 */
	public int getStableDelay()
	{
		return stableDelay;
	}

	/**
	 * Set the minimum time to wait for a stable GUI state
	 * @param stableDelay The time, in seconds, to wait for a stable GUI state
	 */
	public void setStableDelay(int stableDelay)
	{
		this.stableDelay = stableDelay;
	}

	/**
	 * @return The max diff allowed for matching GUI state checksums
	 */
	public long getMaxChecksumDiff()
	{
		return maxChecksumDiff;
	}

	/**
	 * @param maxChecksumDiff The max diff allowed for matching GUI state checksums
	 */
	public void setMaxChecksumDiff(long maxChecksumDiff)
	{
		this.maxChecksumDiff = maxChecksumDiff;
	}
	
	public void prepareAjaxWait(WebDriver webDriver)
	{
		if(webDriver!=null)
		{
			try
			{
				JavascriptExecutor executor = (JavascriptExecutor) webDriver;
				executor.executeScript("window.new_page = 'true'; function setNewPageValue(e) {window.new_page = 'true';}; " +
						"window.addEventListener(\"beforeunload\", setNewPageValue, false); " +
						"if (window.XMLHttpRequest) {if (!window.origXMLHttpRequest || !window.ajax_obj) { " +
						"window.ajax_obj = []; window.origXMLHttpRequest = window.XMLHttpRequest; " +
						"window.XMLHttpRequest = function() { var xhr = new window.origXMLHttpRequest(); " +
						"window.ajax_obj.push(xhr); return xhr;}}} function setDOMModifiedTime() { " +
						"window.domModifiedTime = Date.now();}var _win = window.document.body; " +
						"_win.addEventListener(\"DOMNodeInserted\", setDOMModifiedTime, false); " +
						"_win.addEventListener(\"DOMNodeInsertedIntoDocument\", setDOMModifiedTime, false); " +
						"_win.addEventListener(\"DOMNodeRemoved\", setDOMModifiedTime, false); " +
						"_win.addEventListener(\"DOMNodeRemovedFromDocument\", setDOMModifiedTime, false); " +
						"_win.addEventListener(\"DOMSubtreeModified\", setDOMModifiedTime, false); ");
			}
			catch (Exception e)
			{
				return;
			}
		}
	}
	
	public boolean waitPageDomAjax(WebDriver webDriver)
	{
		long startTime=System.currentTimeMillis();
		while(true)
		{
			boolean newPageValue=getNewPageValue(webDriver);
			boolean documentReady=getDocumentReady(webDriver);
			boolean ajaxReady=getAjaxReady(webDriver);
			String domModifiedTime=getDomModifiedTime(webDriver);
//			if(newPageValue && documentReady && ajaxReady)
			if(newPageValue)
			{
				return true;
			}
			long deltaTime=(System.currentTimeMillis()-startTime)/1000;
			if(deltaTime>=stepDelay)
			{
				// Done waiting
				return false;
			}
			delay(1000);
		}
	}

	public boolean getNewPageValue(WebDriver webDriver)
	{
		if(webDriver!=null)
		{
			try
			{
				JavascriptExecutor executor = (JavascriptExecutor) webDriver;
				Object object=executor.executeScript("return window.new_page; ");
				String result=object.toString();
				return result.equals("true");
			}
			catch (Exception e)
			{
				return false;
			}
		}
		return false;
	}

	public String getDomModifiedTime(WebDriver webDriver)
	{
		if(webDriver!=null)
		{
			try
			{
				JavascriptExecutor executor = (JavascriptExecutor) webDriver;
				Object object=executor.executeScript("return window.domModifiedTime; ");
				String result=object.toString();
				return result;
			}
			catch (Exception e)
			{
				return null;
			}
		}
		return null;
	}

	public boolean getDocumentReady(WebDriver webDriver)
	{
		if(webDriver!=null)
		{
			try
			{
				JavascriptExecutor executor = (JavascriptExecutor) webDriver;
				Object object=executor.executeScript("if(window.document.readyState==\"complete\"){return 'true';}else{return 'false';} ");
				String result=object.toString();
				return result.equals("true");
			}
			catch (Exception e)
			{
				return false;
			}
		}
		return false;
	}

	public boolean getAjaxReady(WebDriver webDriver)
	{
		if(webDriver!=null)
		{
			try
			{
				JavascriptExecutor executor = (JavascriptExecutor) webDriver;
				Object object=executor.executeScript("if (window.ajax_obj) { if (window.ajax_obj.length == 0) {return 'true';} else { " +
				"for (var index in window.ajax_obj) { " +
				"if (window.ajax_obj[index].readyState !== 4 && " +
				"window.ajax_obj[index].readyState !== undefined && " +
				"window.ajax_obj[index].readyState !== 0) {return 'false';}}return 'true';}} " +
				"else {if (window.origXMLHttpRequest) {window.origXMLHttpRequest = \"\";}return 'true';} ");
				String result=object.toString();
				return result.equals("true");
			}
			catch (Exception e)
			{
				return false;
			}
		}
		return false;
	}

	public static String[] getCkecksumProperties()
	{
		return ckecksumProperties;
	}

	/**
	 * Set the properties (or attributes) to create the checksum from
	 * @param ckecksumProperties
	 */
	public static void setCkecksumProperties(String[] ckecksumProperties)
	{
		Similo.ckecksumProperties = ckecksumProperties;
	}

	public static String[] getChecksumTags()
	{
		return checksumTags;
	}

	/**
	 * Set the tags to create a checksum from
	 * @param checksumTags
	 */
	public static void setChecksumTags(String[] checksumTags)
	{
		Similo.checksumTags = checksumTags;
	}

	public List<Locator> similo(Locator targetWidget, List<Locator> candidateWidgets)
	{
		similoCalculation(targetWidget, candidateWidgets);
		Collections.sort(candidateWidgets);
		return candidateWidgets;
	}

	private class FindWebElementsThread extends Thread
	{
		private Locator targetWidget;
		private List<Locator> candidateWidgets;

		public FindWebElementsThread(Locator targetWidget, List<Locator> candidateWidgets)
		{
			this.targetWidget = targetWidget;
			this.candidateWidgets = candidateWidgets;
		}

		public void run()
		{
			similoCalculation(targetWidget, candidateWidgets);
		}
	}

	private void similoCalculation(Locator targetWidget, List<Locator> candidateWidgets)
	{
		long startTime = System.currentTimeMillis();
		double bestSimilarityScore = 0;
		for (Locator candidateWidget : candidateWidgets)
		{
			double similarityScore = 0;
			similarityScore = calcSimilarityScore(targetWidget, candidateWidget);
			candidateWidget.setScore(similarityScore);
			if(similarityScore > bestSimilarityScore)
			{
				bestSimilarityScore = similarityScore;
			}
			long duration = System.currentTimeMillis() - startTime;
			candidateWidget.setDuration(duration);
		}
	}

	private double calcMaxSimilarityScore(Locator candidateWidget)
	{
		double similarityScore=0;
		int index = 0;
		for (String locator : LOCATORS)
		{
			double weight=WEIGHTS[index];
			String candidateValue = candidateWidget.getMetadata(locator);
			if(candidateValue != null)
			{
				similarityScore += weight;
			}
			index++;
		}
		return similarityScore;
	}

	public double calcSimilarityScore(Locator targetWidget, Locator candidateWidget)
	{
		double similarityScore=0;
		int index = 0;
		String totalSimilarity="";
		for (String locator : LOCATORS)
		{
//			if(!targetWidget.isIgnoredMetadata(locator))
			{
				double weight=WEIGHTS[index];
				double similarity=0;

				String targetValue = targetWidget.getMetadata(locator);
				String candidateValue = candidateWidget.getMetadata(locator);

				if(targetValue != null && candidateValue != null)
				{
					int similarityFunction = SIMILARITY_FUNCTION[index];
					if(similarityFunction == 1)
					{
						String[] targetValues = getParameterValues(targetValue);
						String[] candidateValues = getParameterValues(candidateValue);
						for(String targetVal:targetValues)
						{
							for(String candidateVal:candidateValues)
							{
								double valueSimilarity=((double)stringSimilarity(targetVal, candidateVal, 100))/100;
								if(valueSimilarity > similarity)
								{
									similarity = valueSimilarity;
								}
							}
						}
					}
					else if(similarityFunction == 2)
					{
						similarity=((double)integerSimilarity(targetValue, candidateValue, 1000))/1000;
					}
					else if(similarityFunction == 3)
					{
						// Use 2D distance

						String targetLocation = targetWidget.getMetadata("location");
						String candidateLocation = candidateWidget.getMetadata("location");

						String[] splittedTargetLocation = targetLocation.split(Pattern.quote(","));
						String[] splittedCandidateLocation = candidateLocation.split(Pattern.quote(","));
						
						if(splittedTargetLocation.length == 2 && splittedCandidateLocation.length == 2)
						{
							int x = string2Int(splittedTargetLocation[0]);
							int y = string2Int(splittedTargetLocation[1]);
							int xc = string2Int(splittedCandidateLocation[0]);
							int yc = string2Int(splittedCandidateLocation[1]);

							int dx = x - xc;
							int dy = y - yc;
							int pixelDistance = (int)Math.sqrt(dx*dx + dy*dy);
							similarity = ((double)Math.max(200 - pixelDistance, 0))/200;
						}
					}
					else if(similarityFunction == 4)
					{
						similarity=((double)neighborTextSimilarity(targetValue, candidateValue, 100))/100;
					}
					else
					{
						String[] targetValues = getParameterValues(targetValue);
						String[] candidateValues = getParameterValues(candidateValue);
						for(String targetVal:targetValues)
						{
							for(String candidateVal:candidateValues)
							{
								double valueSimilarity=(double)equalSimilarity(targetVal, candidateVal, 1);
								if(valueSimilarity > similarity)
								{
									similarity = valueSimilarity;
								}
							}
						}
					}
				}
				
				if("visible_text".equals(locator) && similarity == 1)
				{
					weight*=2;
				}
				
				similarityScore += similarity * weight;
				totalSimilarity+=locator+"="+(similarity * weight)+" ";
			}
			index++;
		}
		candidateWidget.putMetadata("total_similaity", totalSimilarity);
		return similarityScore;
	}

	private int equalSimilarity(String t1, String t2, int maxScore)
	{
		if (t1 != null && t2 != null)
		{
			if (t1.equalsIgnoreCase(t2))
			{
				return maxScore;
			}
		}
		return 0;
	}

	private int integerSimilarity(String t1, String t2, int maxScore)
	{
		int value1 = string2Int(t1);
		int value2 = string2Int(t2);
		return integerSimilarity(value1, value2, maxScore);
	}

	private int integerSimilarity(int value1, int value2, int maxScore)
	{
		int distance = Math.abs(value1 - value2);
		int max = Math.max(value1, value2);
		int score = (max - distance) * maxScore / max;
		return score;
	}

	private int stringSimilarity(String s1, String s2, int maxScore)
	{
		if(s1.length()==0 || s2.length()==0)
		{
			return 0;
		}

		if(s1.equalsIgnoreCase(s2))
		{
			return maxScore;
		}

		// Make sure s1 is longer (or equal)
		if(s1.length() < s2.length())
		{
			// Swap
			String swap = s1;
			s1 = s2;
			s2 = swap;
		}

		int distance = computeLevenshteinDistance(s1, s2);
		return (s1.length() - distance) * maxScore / s1.length();
	}

	private int computeLevenshteinDistance(String s1, String s2)
	{
//		s1 = stripString(s1);
//		s2 = stripString(s2);
		s1 = s1.toLowerCase();
		s2 = s2.toLowerCase();

		int[] costs = new int[s2.length() + 1];
		for (int i = 0; i <= s1.length(); i++)
		{
			int lastValue = i;
			for (int j = 0; j <= s2.length(); j++)
			{
				if (i == 0)
				{
					costs[j] = j;
				}
				else
				{
					if (j > 0)
					{
						int newValue = costs[j - 1];
						if (s1.charAt(i - 1) != s2.charAt(j - 1))
						{
							newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
						}
						costs[j - 1] = lastValue;
						lastValue = newValue;
					}
				}
			}
			if (i > 0)
			{
				costs[s2.length()] = lastValue;
			}
		}
		return costs[s2.length()];
	}

	private String stripString(String s)
	{
		StringBuffer stripped = new StringBuffer();
		for(int i=0; i<s.length(); i++)
		{
			char c = s.charAt(i);
			if(Character.isAlphabetic(c) || Character.isDigit(c))
			{
				stripped.append(c);
			}
			else if(Character.isWhitespace(c))
			{
				stripped.append(' ');
			}
		}
		String strippedString = stripped.toString();
		return strippedString.trim();
	}

	private void addNeighborText(Locator locator, List<Locator> availableLocators)
	{
		if(locator.getLocationArea()==null)
		{
			return;
		}
		Rectangle r = locator.getLocationArea();
		if(r.height>100 || r.width > 600)
		{
			return;
		}
		Rectangle largerRectangle = new Rectangle(r.x-50, r.y-50, r.width+100, r.height+100);

		List<Locator> neighbors = new ArrayList<Locator>();
		for(Locator available:availableLocators)
		{
			if(locator!=available && available.getLocationArea()!=null)
			{
				Rectangle rect = available.getLocationArea();
				if(rect.getHeight()<=100 && largerRectangle.intersects(rect))
				{
			  	neighbors.add(available);
				}
			}
		}
		
		List<String> words = new ArrayList<String>();
		Properties wordHash = new Properties();
		for(Locator neighbor:neighbors)
		{
			String visibleText=neighbor.getVisibleText();
			if(visibleText != null)
			{
				String[] visibleWords = visibleText.split("\\s+");
				for(String visibleWord:visibleWords)
				{
					String visibleWordLower = stripString(visibleWord.toLowerCase());
					if(!wordHash.containsKey(visibleWordLower))
					{
						wordHash.put(visibleWordLower, true);
						words.add(visibleWordLower);
					}
				}
			}
		}

		StringBuffer wordString = new StringBuffer();
		for(String word:words)
		{
			if(wordString.length()>0)
			{
				wordString.append(" ");
			}
			wordString.append(word);
		}

		if(wordString.length()>0)
		{
			String text = wordString.toString();
			locator.putMetadata("neighbor_text", text);
		}
	}

	private boolean isOverlapping(String locatorName)
	{
		for(int i=0; i<LOCATORS.length; i++)
		{
			if(LOCATORS[i].equalsIgnoreCase(locatorName))
			{
				return IS_OVERPAPPING[i];
			}
		}
		return false;
	}

	private boolean containsSeveralParameterValues(String text)
	{
		int index = text.indexOf(" || ");
		if(index == -1)
		{
			return false;
		}
		else
		{
			return true;
		}
	}

	private String getInitialValue(String text)
	{
		int index = text.indexOf(" || ");
		if(index == -1)
		{
			return text;
		}
		else
		{
			String value = text.substring(0, index);
			return value;
		}
	}

	private String[] getParameterValues(String value)
	{
		String[] splitted = value.split(Pattern.quote(" || "));
		return splitted;
	}

	private boolean containsParameterValue(String text, String value)
	{
		String[] splitted = value.split(Pattern.quote(" || "));
		for(String split:splitted)
		{
			if(split.equalsIgnoreCase(value))
			{
				return true;
			}
		}
		return false;
	}

	private boolean isContained(Locator locator, Locator available)
	{
		String xpathLocator=locator.getMetadata("xpath");
		String xpathAvailable=available.getMetadata("xpath");
		if(locator.getVisibleText() !=null && available.getVisibleText() != null && xpathLocator.startsWith(xpathAvailable) && locator.getVisibleText().equals(available.getVisibleText()))
		{
			return true;
		}
		return false;
	}
	
	private void addOverlappingLocatorParameters(Locator locator, List<Locator> availableLocators)
	{
		Rectangle r = locator.getLocationArea();
		if(r==null)
		{
			return;
		}

		for(Locator available:availableLocators)
		{
			if(locator!=available)
			{
				Rectangle ar = available.getLocationArea();
				if(r.intersects(ar))
				{
					Rectangle union = new Rectangle();
					Rectangle.union(r, ar, union);
					Rectangle intersection = new Rectangle();
					Rectangle.intersect(r, ar, intersection);
					double ratio = (intersection.getWidth() * intersection.getHeight()) / (union.getWidth() * union.getHeight());
					if(ratio >= 0.85 || isContained(locator, available))
					{
/*
						// Update location and size
						locator.setLocationArea(union);
						addMetadata(locator, "x", ""+(int)union.getX());
						addMetadata(locator, "y", ""+(int)union.getY());
						addMetadata(locator, "height", ""+(int)union.getHeight());
						addMetadata(locator, "width", ""+(int)union.getWidth());
						int area = (int)union.getWidth() * (int)union.getHeight();
						int shape = ((int)union.getWidth() * 100) / (int)union.getHeight();
						addMetadata(locator, "area", ""+area);
 						addMetadata(locator, "shape", ""+shape);
*/
						
						List<String> keys=available.getMetadataKeys();
						for(String key:keys)
						{
							if(isOverlapping(key))
							{
								String value=available.getMetadata(key);
								if(value != null && value.trim().length() > 0)
								{
									String existingValue=locator.getMetadata(key);
									if(existingValue==null || existingValue.trim().length() == 0)
									{
										// Missing or empty
										locator.putMetadata(key, value);
									}
									else
									{
										// Contains existing data - concatenate
										String initialValue = getInitialValue(value);
										if(existingValue.indexOf(initialValue) == -1)
										{
											existingValue += " || ";
											existingValue += initialValue;
											locator.putMetadata(key, existingValue);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Remove locators with identical size and location (keep the first)
	 * @param availableLocators
	 * @return A list of uniquely located locators
	 */
	private List<Locator> removeIdenticalLocators(List<Locator> availableLocators)
	{
		List<Locator> nonIdenticalLocators = new ArrayList<Locator>();
		Set<Long> addedRects = new HashSet<Long>();

		for(Locator available:availableLocators)
		{
			Rectangle rect = available.getLocationArea();
			long hash = (long)rect.getWidth() + (long)rect.getHeight() * 1000 + (long)rect.getX() * 1000000 + (long)rect.getY() * 1000000000;
			if(!addedRects.contains(hash))
			{
				addedRects.add(hash);
				nonIdenticalLocators.add(available);
			}
			else
			{
				continue;
			}
		}
		
		return nonIdenticalLocators;
	}

	private boolean containsWord(String containsWord, String[] words)
	{
		for(String word:words)
		{
			if(containsWord.length() < word.length() && (word.startsWith(containsWord) || word.endsWith(containsWord)))
			{
				return true;
			}
			else if(word.length() < containsWord.length() && (containsWord.startsWith(word) || containsWord.endsWith(word)))
			{
				return true;
			}
			else if(containsWord.equals(word))
			{
				return true;
			}
		}
		return false;
	}

	private int neighborTextSimilarity(String text1, String text2, int maxScore)
	{
		if(text1.length()==0 || text2.length()==0)
		{
			return 0;
		}

		String[] words1 = text1.split("\\s+");
		String[] words2 = text2.split("\\s+");
		
		int existsCount = 0;
		int wordCount = Math.max(text1.length() - words1.length + 1, text2.length() - words2.length + 1);
		for(String word1:words1)
		{
			if(containsWord(word1, words2))
			{
				existsCount += word1.length();
			}
		}
		int score = Math.min((existsCount * maxScore) / wordCount, 100);
		return score;
	}

	private List<String> readLines(File file)
	{
		List<String> lines=new ArrayList<String>();
		try
		{
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine())
			{
				String line=scanner.nextLine().trim();
				if(line.length()>0)
				{
					lines.add(line);
				}
			}
			scanner.close();
		}
		catch (Exception e)
		{
		}
		return lines;
	}

	private String loadTextFile(String filename)
	{
		File file=new File(filename);
		if(file.exists())
		{
			List<String> lines=readLines(file);
			StringBuffer buf=new StringBuffer();
			for(String line:lines)
			{
				buf.append(line);
				buf.append("\n");
			}
			return buf.toString();
		}
		return null;
	}

	/**
	 * Get the default property (visible_text)
	 * @return The default property
	 */
	public String getDefaultProperty()
	{
		return defaultProperty;
	}

	/**
	 * Set the default property used when creating a new property file
	 * @param defaultProperty The name of the default property
	 */
	public void setDefaultProperty(String defaultProperty)
	{
		this.defaultProperty = defaultProperty;
	}
}

