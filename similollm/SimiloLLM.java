package similollm;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

public class SimiloLLM
{
	public List<Properties> readCSV(String csvFilename)
	{
		List<Properties> rows=new ArrayList<Properties>();
		CSVReader csvReader;
		try
		{
			csvReader = new CSVReader(csvFilename);
			int id=0;
			while (csvReader.hasNext())
			{
				Properties parameters = csvReader.next();
				parameters.put("widget_id", ""+id);
				rows.add(parameters);
				id++;
			}
			return rows;
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	private boolean containsParameterValue(String text, String value)
	{
		String[] splitted = text.split(Pattern.quote(" || "));
		for(String split:splitted)
		{
			if(split.equalsIgnoreCase(value))
			{
				return true;
			}
		}
		return false;
	}

	public Properties getByXpath(String app, String xpath, List<Properties> data)
	{
		for(Properties p:data)
		{
			String dataapp=(String)p.get("app");
			String dataxpath=(String)p.get("xpath");
			if(dataapp.equals(app) && containsParameterValue(dataxpath, xpath))
			{
				return p;
			}
		}
		return null;
	}
	
	public List<Properties> getByApp(String app, List<Properties> data)
	{
		List<Properties> rows=new ArrayList<Properties>();
		for(Properties p:data)
		{
			String dataapp=(String)p.get("app");
			if(dataapp.equals(app))
			{
				rows.add(p);
			}
		}
		return rows;
	}
	
	public boolean oracleExist(String app, String fromxpath, String toxpath, List<Properties> data)
	{
		for(Properties p:data)
		{
			String appData=(String)p.get("app");
			String fromxpathData=(String)p.get("fromxpath");
			String toxpathData=(String)p.get("toxpath");
			if(appData.equals(app) && fromxpathData.equalsIgnoreCase(fromxpath) && toxpathData.equalsIgnoreCase(toxpath))
			{
				return true;
			}
		}
		return false;
	}

	private String pathToIndexPath(String path)
	{
		StringBuffer buf = new StringBuffer();
		char previousChar = ']';
		for(int i=0; i<path.length(); i++)
		{
			char c = path.charAt(i);
			if(c == '/' && previousChar != ']')
			{
				buf.append("[1]");
			}
			buf.append(c);
			previousChar = c;
		}
		if(previousChar != ']')
		{
			buf.append("[1]");
		}
		return buf.toString();
	}

	public List<String> getOracleXpaths(String app, String fromxpath, List<Properties> data)
	{
		List<String> rows=new ArrayList<String>();
		for(Properties p:data)
		{
			String appData=(String)p.get("app");
			String fromxpathData=pathToIndexPath((String)p.get("fromxpath"));
			String toxpathData=pathToIndexPath((String)p.get("toxpath"));
			if(appData.equals(app) && fromxpathData.equalsIgnoreCase(fromxpath))
			{
				rows.add(toxpathData);
			}
		}
		return rows;
	}
	
	private String cleanText(String text)
	{
		if(text==null)
		{
			return "";
		}
		int index=text.indexOf("(Opens Overlay)");
		if(index>0)
		{
			text=text.substring(0, index);
		}
		return text.trim();
	}

	private void addMetadata(Locator locator, String key, Properties data)
	{
		String value = (String)data.get(key);
		if(value!=null && !value.equalsIgnoreCase("null") && value.length()>0)
		{
			locator.putMetadata(key, value);
		}
	}

	private void addMetadataRemoveComma(Locator locator, String key, String keyData, Properties data)
	{
		String value = (String)data.get(keyData);
		if(value!=null && !value.equalsIgnoreCase("null") && value.length()>0)
		{
			value=value.replace(',', ' ');
			locator.putMetadata(key, value);
		}
	}

	public Locator toLocator(Properties p)
	{
		Locator locator=new Locator();

		addMetadata(locator, "widget_id", p);
		addMetadata(locator, "tag", p);
		addMetadata(locator, "class", p);
		addMetadata(locator, "name", p);
		addMetadata(locator, "id", p);
		addMetadata(locator, "href", p);
		addMetadata(locator, "alt", p);
		addMetadata(locator, "xpath", p);
		addMetadata(locator, "idxpath", p);
		addMetadata(locator, "is_button", p);
		addMetadata(locator, "location", p);
		addMetadata(locator, "area", p);
		addMetadata(locator, "shape", p);
		addMetadata(locator, "visible_text", p);
		addMetadataRemoveComma(locator, "neighbor_text", "neighbor_text", p);

		return locator;
	}

	private String toJsonParameter(String key, Properties data, boolean addComma)
	{
		return toJsonParameter(key, key, data, addComma, false);
	}

	private String getShortestParameterValue(String text)
	{
		String shortest=null;
		int len=100000;
		String[] splitted = text.split(Pattern.quote(" || "));
		for(String split:splitted)
		{
			if(split.length()<len)
			{
				shortest=split;
				len=0;
			}
		}
		String replaced=shortest.replaceAll(Pattern.quote("[1]"), "");
		return replaced;
	}

	private String toJsonParameter(String key, String visibleKey, Properties data, boolean addComma, boolean getShortest)
	{
		String json="";
		String value = (String)data.get(key);
		if(value!=null && !value.equalsIgnoreCase("null") && value.length()>0)
		{
			if(addComma)
			{
				json+=",";
			}
			if(getShortest)
			{
				value=getShortestParameterValue(value);
			}
//			value=value.replace(',', ' ');
			json+=visibleKey+":\""+value+"\"";
		}
		return json;
	}

	public String toJson(Properties p)
	{
		return toJson(p, true);
	}
	
	public String toJson(Properties p, boolean includeWidgetId)
	{
		String json="{";
		if(includeWidgetId)
		{
			json+=toJsonParameter("widget_id", p, false);
			json+=toJsonParameter("tag", p, true);
		}
		else
		{
			json+=toJsonParameter("tag", p, false);
		}
		json+=toJsonParameter("visible_text", "text", p, true, false);
		json+=toJsonParameter("class", p, true);
		json+=toJsonParameter("id", p, true);
		json+=toJsonParameter("name", p, true);
		json+=toJsonParameter("href", p, true);
		json+=toJsonParameter("location", p, true);
		json+=toJsonParameter("area", p, true);
		json+=toJsonParameter("shape", p, true);
		json+=toJsonParameter("alt", p, true);
		json+=toJsonParameter("is_button", p, true);
		json+=toJsonParameter("xpath", "xpath", p, true, true);
		json+=toJsonParameter("neighbor_text", p, true);
		json+="}";
		return json;
	}

	public List<Locator> toLocators(List<Properties> list)
	{
		List<Locator> locators=new ArrayList<Locator>();
		for(Properties item:list)
		{
			locators.add(toLocator(item));
		}
		return locators;
	}
}
