package similollm;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Represents an locator for an element.
 */
public class Locator implements Comparable<Object>
{
	private long index=0;
	private Properties properties=new Properties();
	private Rectangle locationArea=null;
	private int x=0;
	private int y=0;
	private int width=0;
	private int height=0;
	private double maxScore=0;

	private double score=0;
	private long duration=0;

	public Locator()
	{
	}

	public Locator(Properties properties)
	{
		setProperties(properties);
	}

	public Locator clone()
	{
		Locator clone = new Locator();
		clone.index = index;
		clone.locationArea = new Rectangle(locationArea);
		clone.properties = new Properties();
		Set<Object> keySet=properties.keySet();
		for(Object o:keySet)
		{
			String key=(String)o;
			String value=properties.getProperty(key);
			clone.properties.put(key, value);
		}
		return clone;
	}
	
	public Properties getProperties()
	{
		return properties;
	}

	public void setProperties(Properties properties)
	{
		this.properties = properties;
	}

	/**
	 * Get metadata for key
	 * @param key The key to get metadata from
	 * @return Metadata for key or null
	 */
	public String getMetadata(String key)
	{
		return (String)properties.get(key);
	}

	public int getNoProperties()
	{
		return properties.size();
	}

	/**
	 * Get all metadata keys
	 * @return A list of metadata keys
	 */
	public List<String> getMetadataKeys()
	{
		if(properties==null)
		{
			return null;
		}
		Set<Object> keySet=properties.keySet();
		List<String> keyList=new ArrayList<String>();
		for(Object o:keySet)
		{
			String s=(String)o;
			keyList.add(s);
		}
		return keyList;
	}

	/**
	 * Put meta data associated with this state
	 * @param key The metadata key
	 * @param value The metadata value
	 */
	public void putMetadata(String key, String value)
	{
		if(value!=null)
		{
			properties.put(key, value);
		}
	}

	/**
	 * Remove meta data for key
	 * @param key The metadata key
	 */
	public void removeMetadata(String key)
	{
		if(properties!=null)
		{
			properties.remove(key);
		}
	}

	public long getIndex()
	{
		return index;
	}

	public void setIndex(long index)
	{
		this.index = index;
	}

	public void clearRepairedMetadata()
	{
		removeMetadata("repaired");
	}

	public boolean containsRepairedMetadata()
	{
		String repaired=getMetadata("repaired");
		if(repaired==null || repaired.length()==0)
		{
			return false;
		}
		return true;
	}

	public boolean isRepairedMetadata(String key)
	{
		String repaired=getMetadata("repaired");
		if(repaired==null)
		{
			return false;
		}
		String paddedRepaired=" "+repaired+" ";
		if(paddedRepaired.indexOf(" "+key+" ")>=0)
		{
			return true;
		}
		return false;
	}
	
	public void addRepairedMetadata(String key)
	{
		if(isRepairedMetadata(key))
		{
			// Already repaired
			return;
		}
		String repaired=getMetadata("repaired");
		if(repaired==null)
		{
			repaired="";
		}
		if(repaired.length()>0)
		{
			repaired+=" ";
		}
		repaired+=key;
		putMetadata("repaired", repaired);
	}

	public boolean isIgnoredMetadata(String key)
	{
		String ignored=getMetadata("ignored");
		if(ignored==null)
		{
			return false;
		}
		String paddedIgnored=" "+ignored+" ";
		if(paddedIgnored.indexOf(" "+key+" ")>=0)
		{
			return true;
		}
		return false;
	}
	
	public void addIgnoredMetadata(String key)
	{
		if(isIgnoredMetadata(key))
		{
			// Already ignored
			return;
		}
		String ignored=getMetadata("ignored");
		if(ignored==null)
		{
			ignored="";
		}
		if(ignored.length()>0)
		{
			ignored+=" ";
		}
		ignored+=key;
		putMetadata("ignored", ignored);
	}

	public Rectangle getLocationArea()
	{
		return locationArea;
	}

	public void setLocationArea(Rectangle locationArea)
	{
		this.locationArea = locationArea;
	}
	
	public String getVisibleText()
	{
		String text=(String)getMetadata("text");
		String value=(String)getMetadata("value");
		String title=(String)getMetadata("title");
		String placeholder=(String)getMetadata("placeholder");
		String alt=(String)getMetadata("alt");

		if(text!=null && text.trim().length()>0)
		{
			return text.trim();
		}
		else if(value!=null && value.trim().length()>0)
		{
			return value.trim();
		}
		else if(title!=null && title.trim().length()>0)
		{
			return title.trim();
		}
		else if(placeholder!=null && placeholder.trim().length()>0)
		{
			return placeholder.trim();
		}
		else if(alt!=null && alt.trim().length()>0)
		{
			return alt.trim();
		}
		
		return null;
	}

	public int compareTo(Object o)
	{
		Locator compareTo=(Locator)o;
		return (int)(compareTo.getScore()*1000-getScore()*1000);
	}

	public double getScore()
	{
		return score;
	}

	public void setScore(double score)
	{
		this.score = score;
	}

	public void increaseScore(double increaseScore)
	{
		this.score += increaseScore;
	}

	public int getX()
	{
		return x;
	}

	public void setX(int x)
	{
		this.x = x;
	}

	public int getY()
	{
		return y;
	}

	public void setY(int y)
	{
		this.y = y;
	}

	public int getWidth()
	{
		return width;
	}

	public void setWidth(int width)
	{
		this.width = width;
	}

	public int getHeight()
	{
		return height;
	}

	public void setHeight(int height)
	{
		this.height = height;
	}

	public double getMaxScore()
	{
		return maxScore;
	}

	public void setMaxScore(double maxScore)
	{
		this.maxScore = maxScore;
	}

	public long getDuration()
	{
		return duration;
	}

	public void setDuration(long duration)
	{
		this.duration = duration;
	}
}
