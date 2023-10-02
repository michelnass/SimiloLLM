package similollm;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class SimiloLLMTest
{
	private static Font textBoxFont = new Font("Arial", Font.PLAIN, 32);
	private static BasicStroke auraStroke=new BasicStroke(5);

	private List<Properties> visibleOracles()
	{
		SimiloLLM similoGPT=new SimiloLLM();
		List<Properties> visibleOracles=new ArrayList<Properties>();

		List<Properties> dataOld=similoGPT.readCSV("csv_all_old_witheqs.csv");
		List<Properties> dataNew=similoGPT.readCSV("csv_all_new_witheqs.csv");
		List<Properties> oracles=similoGPT.readCSV("eq_oracles_csv.csv");
		
		for(Properties oracle:oracles)
		{
			String app=(String)oracle.get("app");

			String fromXPath=(String)oracle.get("fromxpath");
			String toXPath=(String)oracle.get("toxpath");
			
			Properties target=similoGPT.getByXpath(app, fromXPath, dataOld);
			Properties candidate=similoGPT.getByXpath(app, toXPath, dataNew);

			String targetVisibleText=(String)target.get("eqvisibletext");
			String candidateVisibleText=(String)candidate.get("eqvisibletext");
			if(targetVisibleText!=null && candidateVisibleText!=null && targetVisibleText.trim().length()>1 && candidateVisibleText.trim().length()>1)
			{
				visibleOracles.add(oracle);
			}
		}

		return visibleOracles;
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

	private BufferedImage loadImage(String filename)
	{
		try
		{
			File file = new File(filename);
			if(file.exists())
			{
				return ImageIO.read(file);
			}
			else
			{
				return null;
			}
		}
		catch (Exception e)
		{
			System.out.println("Failed to load image: " + e.toString());
			return null;
		}
	}

	private boolean saveImage(BufferedImage image, String filepath)
	{
		try
		{
			File file=new File(filepath);
			File parentDir=file.getParentFile();
			if(parentDir!=null && !parentDir.exists())
			{
				// Create parent folder
				parentDir.mkdirs();
			}

			ImageIO.write(image, "png", file);
			return true;
		}
		catch (Exception e)
		{
			System.out.println("Failed to save image: " + e.toString());
			return false;
		}
	}

	private void drawRectangle(Graphics2D g2, Color color, BasicStroke stroke, int x, int y, int width, int height)
	{
		drawRectangle(g2, x, y, width, height, color, stroke, auraStroke);
	}
	
	private void drawRectangle(Graphics2D g2, int x, int y, int width, int height, Color color, BasicStroke stroke, BasicStroke auraStroke)
	{
		g2.setFont(textBoxFont);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		int margin=3;

		// Draw rectangle
		g2.setColor(color);
		g2.setStroke(stroke);
		g2.drawRoundRect(x-margin, y-margin, width+margin*2, height+margin*2, 10, 10);
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

	private Rectangle getRect(Locator locator)
	{
		int area=string2Int(locator.getMetadata("area"));
		int shape=string2Int(locator.getMetadata("shape"));
		int w = (int)Math.sqrt((area * shape) / 100);
		int h = (int)(area / Math.sqrt((area * shape) / 100));
		String location=locator.getMetadata("location");
		String[] splitted = location.split(Pattern.quote(","));
		int x=string2Int(splitted[0]);
		int y=string2Int(splitted[1]);
		return new Rectangle(x, y, w, h);
	}

	private void log(String filename, String text)
	{
		System.out.println(text);
		writeLine(filename, text);
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

	@Test
	void convertOracles()
	{
		SimiloLLM similoGPT=new SimiloLLM();
		List<Properties> oracles=similoGPT.readCSV("oracles.txt");
		System.out.println("No rows: "+oracles.size());

		for(Properties oracle:oracles)
		{
			String app=(String)oracle.get("app");
			String name=(String)oracle.get("name");
			String fromXPath=pathToIndexPath((String)oracle.get("fromxpath"));
			String toXPath=pathToIndexPath((String)oracle.get("toxpath"));

			String fromXPath2=pathToIndexPath(fromXPath);
			String toXPath2=pathToIndexPath(toXPath);

			String converted=app+";"+name+";"+fromXPath2+";"+toXPath2;
			log("converted_oracles.txt", converted);
		}
	}

	private double getMedian(double[] numArray)
	{
		Arrays.sort(numArray);
		double median;
		if (numArray.length % 2 == 0)
		    median = ((double)numArray[numArray.length/2] + (double)numArray[numArray.length/2 - 1])/2;
		else
		    median = (double) numArray[numArray.length/2];
		return median;
	}
	
	private double[] getLower(double[] numArray)
	{
		int noItems=numArray.length/2;
		double[] array=new double[noItems];
		for(int i=0; i<noItems; i++)
		{
			array[i]=numArray[numArray.length-1-i];
		}
		return array;
	}

	private double[] getHigher(double[] numArray)
	{
		int noItems=numArray.length/2;
		double[] array=new double[noItems];
		for(int i=0; i<noItems; i++)
		{
			array[i]=numArray[i];
		}
		return array;
	}
	
	/**
	 * Calculate the boundary value for a higher outlier according to Turkey's Outliers Formula 
	 * @param numArray
	 * @return
	 */
	private double higherOutlier(double[] numArray)
	{
		double[] lower=getLower(numArray);
		double[] higher=getHigher(numArray);
		double q1=getMedian(lower);
		double q3=getMedian(higher);
		double iqr=q3-q1;
		double higherOutlier=q3 + (1.5 * iqr);
		return higherOutlier;
	}
	
	private double higherOutlier(List<Locator> locators)
	{
		double[] array=new double[locators.size()];
		int i=0;
		for(Locator locator:locators)
		{
			array[i]=locator.getScore();
			i++;
		}
		Arrays.sort(array);
		double[] topArray=new double[10];
		int noItems=Math.min(10, array.length);
		for(int j=0; j<noItems; j++)
		{
			topArray[j]=array[array.length-1-j];
		}
		return higherOutlier(topArray);
	}
	
	private List<Locator> getOutliers(List<Locator> locators, double threshold)
	{
		List<Locator> outliers=new ArrayList<Locator>();
		for(Locator locator:locators)
		{
			if(locator.getScore()>=threshold)
			{
				outliers.add(locator);
			}
		}
		return outliers;
	}

	@Test
	void testVONSimilo()
	{
		Similo similo=new Similo();
		SimiloLLM similoGPT=new SimiloLLM();

		List<Properties> dataOld=similoGPT.readCSV("old.txt");
		List<Properties> dataNew=similoGPT.readCSV("new.txt");
		List<Properties> oracles=similoGPT.readCSV("oracles.txt");
		System.out.println("No rows: "+dataOld.size());
		System.out.println("No rows: "+dataNew.size());
		System.out.println("No rows: "+oracles.size());

		int correctCount=0;
		int inCorrectCount=0;

		long startAll=System.currentTimeMillis();

		int oracleNo=0;
		for(Properties oracle:oracles)
		{
			oracleNo++;
			System.out.println("Oracle no: "+oracleNo);
			String app=(String)oracle.get("app");

			String fromXPath=(String)oracle.get("fromxpath");
			String toXPath=(String)oracle.get("toxpath");

			Properties target=similoGPT.getByXpath(app, fromXPath, dataOld);
			if(target!=null)
			{
				List<Properties> candidates=similoGPT.getByApp(app, dataNew);
				Locator targetLocator=similoGPT.toLocator(target);
				List<Locator> candidateLocators=similoGPT.toLocators(candidates);
				List<Locator> bestLocators=similo.similo(targetLocator, candidateLocators);
				Locator bestLocator=bestLocators.get(0);
				String bestXpath=(String)bestLocator.getMetadata("xpath");

				if(containsParameterValue(bestXpath, toXPath))
				{
					log("results_von.txt", "Similo Correct");
					correctCount++;
				}
				else
				{
					log("results_von.txt", "Similo Incorrect");
					inCorrectCount++;
				}
			}
			else
			{
				System.out.println("Target not found for: "+app+" - "+fromXPath);
			}
		}
		long endAll=System.currentTimeMillis();
		long durationAll=endAll-startAll;
		log("performance_von.txt", "TotaL: "+durationAll);
		log("performance_von.txt", "Average: "+durationAll/oracles.size());

		log("results_von.txt", "\nTotal:");
		log("results_von.txt", "Correct: "+correctCount);
		log("results_von.txt", "Incorrect: "+inCorrectCount);
	}

	@Test
	void testVONSimiloOutliers()
	{
		Similo similo=new Similo();
		SimiloLLM similoGPT=new SimiloLLM();

		List<Properties> dataOld=similoGPT.readCSV("old.txt");
		List<Properties> dataNew=similoGPT.readCSV("new.txt");
		List<Properties> oracles=similoGPT.readCSV("oracles.txt");
		System.out.println("No rows: "+dataOld.size());
		System.out.println("No rows: "+dataNew.size());
		System.out.println("No rows: "+oracles.size());

		int foundCount=0;
		int notFoundCount=0;
		int correctCount=0;
		int inCorrectCount=0;
		int inCorrectCountNotAmong=0;
		int correctSimiloCount=0;
		int inCorrectSimiloCount=0;
		int singleOutliers=0;

		long startAll=System.currentTimeMillis();

		int oracleNo=0;
		for(Properties oracle:oracles)
		{
			oracleNo++;
			System.out.println("Oracle no: "+oracleNo);
			String app=(String)oracle.get("app");

			String fromXPath=(String)oracle.get("fromxpath");
			String toXPath=(String)oracle.get("toxpath");

			Properties target=similoGPT.getByXpath(app, fromXPath, dataOld);
			if(target!=null)
			{
				List<Properties> candidates=similoGPT.getByApp(app, dataNew);
				Locator targetLocator=similoGPT.toLocator(target);
				List<Locator> candidateLocators=similoGPT.toLocators(candidates);
				List<Locator> bestLocators=similo.similo(targetLocator, candidateLocators);
				Locator bestLocator=bestLocators.get(0);
				String bestXpath=(String)bestLocator.getMetadata("xpath");
				
				double higherOutlier=higherOutlier(candidateLocators);
				List<Locator> outliers=getOutliers(bestLocators, higherOutlier);

				if(outliers.size()==1)
				{
					System.out.println("Found");
					foundCount++;

					if(containsParameterValue(bestXpath, toXPath))
					{
						correctSimiloCount++;
						log("results.txt", "Similo Correct");
						foundCount++;
					}
					else
					{
						inCorrectSimiloCount++;
						log("results.txt", "Similo Incorrect");
						notFoundCount++;
					}
				}
			}
			else
			{
				System.out.println("Target not found for: "+app+" - "+fromXPath);
			}
		}
		long endAll=System.currentTimeMillis();
		long durationAll=endAll-startAll;
		log("performance.txt", "TotaL: "+durationAll);
		log("performance.txt", "Average: "+durationAll/oracles.size());

		System.out.println("#Found: "+foundCount);
		System.out.println("#NotFound: "+notFoundCount);
		double percentFound=((double)foundCount*100)/((double)foundCount+(double)notFoundCount);
		System.out.println("PercentFound: "+percentFound);
		log("results.txt", "\nTotal:");
		log("results.txt", "Outliers: "+singleOutliers);
		log("results.txt", "Correct: "+correctCount);
		log("results.txt", "Incorrect: "+inCorrectCount);
		log("results.txt", "Incorrect (since oracle not among the candidates): "+inCorrectCountNotAmong);
		log("results.txt", "Correct Similo: "+correctSimiloCount);
		log("results.txt", "Incorrect Similo: "+inCorrectSimiloCount);
	}

	@Test
	void testVONSimiloLLM()
	{
		Similo similo=new Similo();
		SimiloLLM similoGPT=new SimiloLLM();

		List<Properties> dataOld=similoGPT.readCSV("old.txt");
		List<Properties> dataNew=similoGPT.readCSV("new.txt");
		List<Properties> oracles=similoGPT.readCSV("oracles.txt");
		System.out.println("No rows: "+dataOld.size());
		System.out.println("No rows: "+dataNew.size());
		System.out.println("No rows: "+oracles.size());

		int correctCount=0;
		int inCorrectCount=0;
		int inCorrectCountNotAmong=0;

		int oracleNo=0;
		for(Properties oracle:oracles)
		{
			oracleNo++;
			System.out.println("Oracle no: "+oracleNo);
			String app=(String)oracle.get("app");

			String fromXPath=(String)oracle.get("fromxpath");
			String toXPath=(String)oracle.get("toxpath");

			Properties target=similoGPT.getByXpath(app, fromXPath, dataOld);
			if(target!=null)
			{
				List<Properties> candidates=similoGPT.getByApp(app, dataNew);
				Locator targetLocator=similoGPT.toLocator(target);
				List<Locator> candidateLocators=similoGPT.toLocators(candidates);
				List<Locator> bestLocators=similo.similo(targetLocator, candidateLocators);

				List<String> oracleXmls=similoGPT.getOracleXpaths(app, fromXPath, oracles);
				for(String oracleXml:oracleXmls)
				{
					oracleXml=pathToIndexPath(oracleXml);
					Properties targetOracle=similoGPT.getByXpath(app, oracleXml, dataNew);
					if(targetOracle!=null)
					{
						boolean isOracleAmongTheBest=false;
						String oracleWidgetId = (String)targetOracle.get("widget_id");

						String message="";
						message+="Given the following candidate web elements (|| means that an attribute can have multiple values):\n";
						for(int i=0; i<10 && i<bestLocators.size(); i++)
						{
							Locator locator=bestLocators.get(i);
							message+=similoGPT.toJson(locator.getProperties())+"\n";

							String locatorWidgetId = (String)locator.getMetadata("widget_id");
							if(oracleWidgetId.equals(locatorWidgetId))
							{
								isOracleAmongTheBest=true;
							}
						}
						message+="\nfind the one that is most similar to the element:\n";
						message+=similoGPT.toJson(target, false)+"\n";
						message+="Answer with the widget_id number(digits) only, no explanation or text characters.\n";
						
						Conversation conv=new Conversation();
						conv.addSystemMessage("Given an old (target) web element and a list of up to 10 new candidate web elements from an evolved web application, help me identify the most likely new web element that the old element has been changed into. The old and new elements will have multiple attributes, such as tag, text, class, href, location, area, shape, xpath, neighbor_text, and potentially others. Consider that changes can be made manually by human developers or through (automated) software engineering tools. There are no specific weightings for the provided attributes. Just reply with one of the new candidate elements and with a list of (bulleted) motivations for why you think this is the most likely new element (that the old one has been changed to).");
						String exampleUserMessage="Given the following candidate web elements (|| means that an attribute can have multiple values):\n";
						exampleUserMessage+="{widget_id:\"400\",tag:\"li || a\",text:\"Sign In\",class:\"signin || link\",href:\"https://zoom.us/signin\",location:\"1283,40\"}\n";
						exampleUserMessage+="{widget_id:\"410\",tag:\"li || a\",text:\"Plans & Pricing\",class:\"top-pricing\",href:\"https://zoom.us/pricing\",location:\"400,40\"}\n";
						exampleUserMessage+="{widget_id:\"420\",tag:\"li || a\",text:\"Contact Sales\",class:\"top-contactsales top-sales\",href:\"https://explore.zoom.us/contactsales\",location:\"529,40\"}\n\n";
						exampleUserMessage+="find the one that is most similar to the element:\n";
						exampleUserMessage+="{tag:\"li || a\",text:\"PLANS\",class:\"link\",href:\"http://zoom.us/pricing\",location:\"601,0\"}\n";
						exampleUserMessage+="Answer with the widget_id number(digits) only, no explanation or text characters.\n";
						String exampleAssistantMessage="410";
						conv.addUserMessage(exampleUserMessage);
						conv.addAssistantMessage(exampleAssistantMessage);

						sleep(1000);

						long start=System.currentTimeMillis();					
						String response=conv.addMessage(message);

						long end=System.currentTimeMillis();
						long duration=end-start;
						log("performance_llm.txt", ""+duration);
						
						boolean isCorrect=false;
						if(response.indexOf(oracleWidgetId)>=0)
						{
							isCorrect=true;
						}
						if(isCorrect)
						{
							log("results_llm.txt", "Correct");
							correctCount++;
						}
						else
						{
							if(isOracleAmongTheBest)
							{
								log("results_llm.txt", "Incorrect");
								inCorrectCount++;
							}
							else
							{
								log("results_llm.txt", "Incorrect (since oracle not among the candidates)");
								inCorrectCountNotAmong++;
							}
						}
					}
					else
					{
						System.out.println("Target oracle not found for: "+app+" - "+oracleXml);
					}
				}
			}
			else
			{
				System.out.println("Target not found for: "+app+" - "+fromXPath);
			}
		}

		log("results_llm.txt", "\nTotal:");
		log("results_llm.txt", "Correct: "+correctCount);
		log("results_llm.txt", "Incorrect: "+inCorrectCount);
		log("results_llm.txt", "Incorrect (since oracle not among the candidates): "+inCorrectCountNotAmong);
	}

	@Test
	void testOracles()
	{
		Similo similo=new Similo();
		SimiloLLM similoGPT=new SimiloLLM();

		List<Properties> dataOld=similoGPT.readCSV("old.txt");
		List<Properties> dataNew=similoGPT.readCSV("new.txt");
		List<Properties> oracles=similoGPT.readCSV("oracles.txt");
		System.out.println("No rows: "+dataOld.size());
		System.out.println("No rows: "+dataNew.size());
		System.out.println("No rows: "+oracles.size());

		BufferedImage capture=loadImage("empty.png");
		String lastApp = null;

		int foundCount=0;
		int notFoundCount=0;
		int correctCount=0;
		int inCorrectCount=0;
		int inCorrectCountNotAmong=0;
		int correctSimiloCount=0;
		int inCorrectSimiloCount=0;
		int singleOutliers=0;

		long startAll=System.currentTimeMillis();

		int oracleNo=0;
		for(Properties oracle:oracles)
		{
			oracleNo++;
			System.out.println("Oracle no: "+oracleNo);
			String app=(String)oracle.get("app");

			String fromXPath=(String)oracle.get("fromxpath");
			String toXPath=(String)oracle.get("toxpath");

			if(!app.equals(lastApp))
			{
				// Create new image
				capture=loadImage("empty.png");
				lastApp = app;
			}
			
			Properties target=similoGPT.getByXpath(app, fromXPath, dataOld);
			if(target!=null)
			{
				List<Properties> candidates=similoGPT.getByApp(app, dataNew);
				Locator targetLocator=similoGPT.toLocator(target);
				List<Locator> candidateLocators=similoGPT.toLocators(candidates);
				List<Locator> bestLocators=similo.similo(targetLocator, candidateLocators);
				Locator bestLocator=bestLocators.get(0);
				String bestXpath=(String)bestLocator.getMetadata("xpath");
				
				double higherOutlier=higherOutlier(candidateLocators);
				List<Locator> outliers=getOutliers(bestLocators, higherOutlier);

				if(outliers.size()>0)
				{
					log("outliers.txt", ""+outliers.size());
				}

				if(outliers.size()==1)
				{
					singleOutliers++;
					String text=(String)targetLocator.getMetadata("visible_text");
					if(text==null)
					{
						text="unknown";
					}

					System.out.println("Not Found for: "+app+" - "+text);
					notFoundCount++;

					text=(String)bestLocator.getMetadata("visible_text");
					if(text==null)
					{
						text="unknown";
					}
					System.out.println("Best similarity score for: "+text+": "+bestLocator.getScore());

					List<String> oracleXmls=similoGPT.getOracleXpaths(app, fromXPath, oracles);
					for(String oracleXml:oracleXmls)
					{
						oracleXml=pathToIndexPath(oracleXml);
						Properties targetOracle=similoGPT.getByXpath(app, oracleXml, dataNew);
						if(targetOracle!=null)
						{
							Locator oracleLocator=similoGPT.toLocator(targetOracle);
							double similarityScore = similo.calcSimilarityScore(targetLocator, oracleLocator);
							text=(String)oracleLocator.getMetadata("visible_text");
							if(text==null)
							{
								text="unknown";
							}
							System.out.println("Oracle similarity score for: "+text+": "+similarityScore);

							boolean isOracleAmongTheBest=false;
							String oracleWidgetId = (String)targetOracle.get("widget_id");

							String message="";
							log("not_found.txt", "Oracle:");
							log("not_found.txt", similoGPT.toJson(targetOracle));
							log("not_found.txt", "");
							message+="Given the following candidate web elements (|| means that an attribute can have multiple values):\n";
							for(int i=0; i<10 && i<bestLocators.size(); i++)
							{
								Locator locator=bestLocators.get(i);
								message+=similoGPT.toJson(locator.getProperties())+"\n";

								String locatorWidgetId = (String)locator.getMetadata("widget_id");
								if(oracleWidgetId.equals(locatorWidgetId))
								{
									isOracleAmongTheBest=true;
								}
							}
							message+="\nfind the one that is most similar to the element:\n";
							message+=similoGPT.toJson(target, false)+"\n";
							message+="Answer with the widget_id number(digits) only, no explanation or text characters.\n";
							log("not_found.txt", message);
							
//							Conversation conv=new Conversation();
/*
							// Part 2
							// Add system message and example input and output
							conv.addSystemMessage("Given an old (target) web element and a list of up to 10 new candidate web elements from an evolved web application, help me identify the most likely new web element that the old element has been changed into. The old and new elements will have multiple attributes, such as tag, text, class, href, location, area, shape, xpath, neighbor_text, and potentially others. Consider that changes can be made manually by human developers or through (automated) software engineering tools. There are no specific weightings for the provided attributes. Just reply with one of the new candidate elements and with a list of (bulleted) motivations for why you think this is the most likely new element (that the old one has been changed to).");
							String exampleUserMessage="Given the following candidate web elements (|| means that an attribute can have multiple values):\n";
							exampleUserMessage+="{widget_id:\"400\",tag:\"li || a\",text:\"Sign In\",class:\"signin || link\",href:\"https://zoom.us/signin\",location:\"1283,40\"}\n";
							exampleUserMessage+="{widget_id:\"410\",tag:\"li || a\",text:\"Plans & Pricing\",class:\"top-pricing\",href:\"https://zoom.us/pricing\",location:\"400,40\"}\n";
							exampleUserMessage+="{widget_id:\"420\",tag:\"li || a\",text:\"Contact Sales\",class:\"top-contactsales top-sales\",href:\"https://explore.zoom.us/contactsales\",location:\"529,40\"}\n\n";
							exampleUserMessage+="find the one that is most similar (answer with the widget_id of the most similar and motivate why using a list) to the element:\n";
							exampleUserMessage+="{tag:\"li || a\",text:\"PLANS\",class:\"link\",href:\"http://zoom.us/pricing\",location:\"601,0\"}\n";
							String exampleAssistantMessage="The most similar element is the one with widget_id \"410\". The reasons for this choice are:\n\n";
							exampleAssistantMessage+="1. Both elements have \"li\" or \"a\" as their 'tag' attribute.\n";
							exampleAssistantMessage+="2. The text \"Plans & Pricing\" in the element with widget_id \"410\" is closely related to the text \"PLANS\" in the given element.\n";
							exampleAssistantMessage+="3. The 'href' attribute in both elements is almost the same, with a minor difference in the protocol used (https vs. http).\n";
							exampleAssistantMessage+="4. Both elements have a similar 'location' attribute, indicating that they might be close to each other on the layout of the website.\n";
							conv.addUserMessage(exampleUserMessage);
							conv.addAssistantMessage(exampleAssistantMessage);
*/
							// Part 3
/*
							conv.addSystemMessage("Given an old (target) web element and a list of up to 10 new candidate web elements from an evolved web application, help me identify the most likely new web element that the old element has been changed into. The old and new elements will have multiple attributes, such as tag, text, class, href, location, area, shape, xpath, neighbor_text, and potentially others. Consider that changes can be made manually by human developers or through (automated) software engineering tools. There are no specific weightings for the provided attributes. Just reply with one of the new candidate elements and with a list of (bulleted) motivations for why you think this is the most likely new element (that the old one has been changed to).");
							String exampleUserMessage="Given the following candidate web elements (|| means that an attribute can have multiple values):\n";
							exampleUserMessage+="{widget_id:\"400\",tag:\"li || a\",text:\"Sign In\",class:\"signin || link\",href:\"https://zoom.us/signin\",location:\"1283,40\"}\n";
							exampleUserMessage+="{widget_id:\"410\",tag:\"li || a\",text:\"Plans & Pricing\",class:\"top-pricing\",href:\"https://zoom.us/pricing\",location:\"400,40\"}\n";
							exampleUserMessage+="{widget_id:\"420\",tag:\"li || a\",text:\"Contact Sales\",class:\"top-contactsales top-sales\",href:\"https://explore.zoom.us/contactsales\",location:\"529,40\"}\n\n";
							exampleUserMessage+="find the one that is most similar to the element:\n";
							exampleUserMessage+="{tag:\"li || a\",text:\"PLANS\",class:\"link\",href:\"http://zoom.us/pricing\",location:\"601,0\"}\n";
							exampleUserMessage+="Answer with the widget_id number(digits) only, no explanation or text characters.\n";
							String exampleAssistantMessage="410";
							conv.addUserMessage(exampleUserMessage);
							conv.addAssistantMessage(exampleAssistantMessage);

							sleep(1000);

							long start=System.currentTimeMillis();					
							String response=conv.addMessage(message);
*/
							long start=System.currentTimeMillis();
							String response="";

							long end=System.currentTimeMillis();
							long duration=end-start;
							log("performance.txt", ""+duration);

							log("not_found.txt", response);							
							log("not_found.txt", "");
							log("not_found.txt", "");
							
							boolean isCorrect=false;
							if(response.indexOf(oracleWidgetId)>=0)
							{
								isCorrect=true;
							}
							if(isCorrect)
							{
								log("results.txt", "Correct");
								correctCount++;
							}
							else
							{
								if(isOracleAmongTheBest)
								{
									log("results.txt", "Incorrect");
									inCorrectCount++;
								}
								else
								{
									log("results.txt", "Incorrect (since oracle not among the candidates)");
									inCorrectCountNotAmong++;
								}
							}
						}
						else
						{
							System.out.println("Target oracle not found for: "+app+" - "+oracleXml);
						}
					}
				}
				
//				saveImage(capture, "images/app"+app+".png");
			}
			else
			{
				System.out.println("Target not found for: "+app+" - "+fromXPath);
			}
		}
		long endAll=System.currentTimeMillis();
		long durationAll=endAll-startAll;
		log("performance.txt", "TotaL: "+durationAll);
		log("performance.txt", "Average: "+durationAll/oracles.size());

		System.out.println("#Found: "+foundCount);
		System.out.println("#NotFound: "+notFoundCount);
		double percentFound=((double)foundCount*100)/((double)foundCount+(double)notFoundCount);
		System.out.println("PercentFound: "+percentFound);
		log("results.txt", "\nTotal:");
		log("results.txt", "Outliers: "+singleOutliers);
		log("results.txt", "Correct: "+correctCount);
		log("results.txt", "Incorrect: "+inCorrectCount);
		log("results.txt", "Incorrect (since oracle not among the candidates): "+inCorrectCountNotAmong);
		log("results.txt", "Correct Similo: "+correctSimiloCount);
		log("results.txt", "Incorrect Similo: "+inCorrectSimiloCount);
	}

	@Test
	void listVisibleText()
	{
		SimiloLLM similoGPT=new SimiloLLM();

		List<Properties> dataOld=similoGPT.readCSV("old.txt");
		List<Properties> dataNew=similoGPT.readCSV("new.txt");
		List<Properties> oracles=similoGPT.readCSV("oracles.txt");

		for(Properties data:dataNew)
		{
			Locator targetLocator=similoGPT.toLocator(data);
			String text=(String)targetLocator.getMetadata("visible_text");
			if(text==null)
			{
				text="unknown";
			}
			System.out.println(text);
		}
	}

	@Test
	void testCart()
	{
		Similo similo=new Similo();
		SimiloLLM similoGPT=new SimiloLLM();

		List<Properties> dataOld=similoGPT.readCSV("csv_all_old_witheqs.csv");
		List<Properties> dataNew=similoGPT.readCSV("csv_all_new_witheqs.csv");
		List<Properties> oracles=visibleOracles();
		System.out.println("No rows: "+dataOld.size());
		System.out.println("No rows: "+dataNew.size());
		System.out.println("No rows: "+oracles.size());
		
		int foundCount=0;
		int notFoundCount=0;
		for(Properties oracle:oracles)
		{
			String app=(String)oracle.get("app");
			String fromXPath=(String)oracle.get("fromxpath");
			String toXPath=(String)oracle.get("toxpath");
			
			Properties target=similoGPT.getByXpath(app, fromXPath, dataOld);
			List<Properties> candidates=similoGPT.getByApp(app, dataNew);
			Locator targetLocator=similoGPT.toLocator(target);
			List<Locator> candidateLocators=similoGPT.toLocators(candidates);
			List<Locator> bestLocators=similo.similo(targetLocator, candidateLocators);
			Locator bestLocator=bestLocators.get(0);
			String bestXpath=(String)bestLocator.getMetadata("xpath");
			if(similoGPT.oracleExist(app, fromXPath, bestXpath, oracles))
			{
				System.out.println("Found");
				foundCount++;
			}
			else
			{
				String text=(String)targetLocator.getMetadata("visible_text");
				if(text==null)
				{
					text="unknown";
				}

				System.out.println("Not Found for: "+app+" - "+text);
				notFoundCount++;

				text=(String)bestLocator.getMetadata("visible_text");
				if(text==null)
				{
					text="unknown";
				}
				System.out.println("Best similarity score for: "+text+": "+bestLocator.getScore());
				List<String> oracleXmls=similoGPT.getOracleXpaths(app, fromXPath, oracles);
				for(String oracleXml:oracleXmls)
				{
					Properties targetOracle=similoGPT.getByXpath(app, oracleXml, dataNew);
					Locator oracleLocator=similoGPT.toLocator(targetOracle);
					double similarityScore = similo.calcSimilarityScore(targetLocator, oracleLocator);
					text=(String)oracleLocator.getMetadata("visible_text");
					if(text==null)
					{
						text="unknown";
					}
					System.out.println("Oracle similarity score for: "+text+": "+similarityScore);
				}
			}
		}
		System.out.println("#Found: "+foundCount);
		System.out.println("#NotFound: "+notFoundCount);
		double percentFound=((double)foundCount*100)/((double)foundCount+(double)notFoundCount);
		System.out.println("PercentFound: "+percentFound);
	}

	@Test
	void testLogVisibleText()
	{
		SimiloLLM similoGPT=new SimiloLLM();

		List<Properties> dataOld=similoGPT.readCSV("csv_all_old_witheqs.csv");
		List<Properties> dataNew=similoGPT.readCSV("csv_all_new_witheqs.csv");
		List<Properties> oracles=similoGPT.readCSV("eq_oracles_csv.csv");
	
		for(Properties oracle:oracles)
		{
			String app=(String)oracle.get("app");
			String fromXPath=(String)oracle.get("fromxpath");
			Properties target=similoGPT.getByXpath(app, fromXPath, dataOld);
			String visibleText=(String)target.get("eqvisibletext");
			System.out.println(visibleText);
		}
	}

	@Test
	void testLogOracleVisibleText()
	{
		SimiloLLM similoGPT=new SimiloLLM();

		List<Properties> dataOld=similoGPT.readCSV("csv_all_old_witheqs.csv");
		List<Properties> dataNew=similoGPT.readCSV("csv_all_new_witheqs.csv");
		List<Properties> oracles=similoGPT.readCSV("eq_oracles_csv.csv");
		
		for(Properties oracle:oracles)
		{
			String app=(String)oracle.get("app");

			String fromXPath=(String)oracle.get("fromxpath");
			String toXPath=(String)oracle.get("toxpath");
			
			Properties target=similoGPT.getByXpath(app, fromXPath, dataOld);
			Properties candidate=similoGPT.getByXpath(app, toXPath, dataNew);

			String targetVisibleText=(String)target.get("eqvisibletext");
			String candidateVisibleText=(String)candidate.get("eqvisibletext");
			if(targetVisibleText!=null && candidateVisibleText!=null && targetVisibleText.trim().length()>1 && candidateVisibleText.trim().length()>1)
			{
				System.out.println("Target:"+targetVisibleText);
				System.out.println("Candidate: "+candidateVisibleText);
			}
		}
	}

	void sleep()
	{
		sleep(2000);
	}
	
	void sleep(int milliseconds)
	{
		try
		{
			Thread.sleep(milliseconds);
		}
		catch (InterruptedException e)
		{
		}
	}
}
