package similollm;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

public class Conversation
{
	private int maxNoMessages = 10;
	private JSONArray messages = new JSONArray();

	public Conversation()
	{
		super();
	}

	/**
	 * Reset the current conversation (remove all messages including the system message)
	 */
	public void resetConversation()
	{
		messages = new JSONArray();
	}

	private void addInternalMessage(String text, String role)
	{
		JSONObject message = new JSONObject();
		message.put("role", role);
		message.put("content", text);
		messages.put(message);

		// Remove oldest in case too many
		if(messages.length()>maxNoMessages)
		{
			messages.remove(1);
		}
}

	/**
	 * Add a system message
	 * @param text
	 */
	public void addSystemMessage(String text)
	{
		addInternalMessage(text, "system");
	}

	/**
	 * Add a user message
	 * @param text
	 */
	public void addUserMessage(String text)
	{
		addInternalMessage(text, "user");
	}

	/**
	 * Add an assistant message
	 * @param text
	 */
	public void addAssistantMessage(String text)
	{
		addInternalMessage(text, "assistant");
	}

	/**
	 * Add another user message and get an assistant response
	 * @param message
	 * @return An assistant response
	 */
	public String addMessage(String message)
	{
		try
		{
			// Create a connection to the API
			String url = "https://api.openai.com/v1/chat/completions";
			HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
			String apiKey = "";
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("Authorization", "Bearer " + apiKey);

			// Add user message
			addUserMessage(message);

			// Create data object
			JSONObject data = new JSONObject();
			data.put("model", "gpt-4");
			data.put("messages", messages);

			// Send request
			con.setDoOutput(true);
			con.getOutputStream().write(data.toString().getBytes());
			String output = new BufferedReader(new InputStreamReader(con.getInputStream())).lines().reduce((a, b) -> a + b).get();

			// Get response
//			int status = con.getResponseCode();
			String responseMessage=new JSONObject(output).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");

			// Add response from the assistant
			addAssistantMessage(responseMessage);

			return responseMessage;
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
			return null;
		}
	}

	public int getMaxNoMessages()
	{
		return maxNoMessages;
	}

	public void setMaxNoMessages(int maxNoMessages)
	{
		this.maxNoMessages = maxNoMessages;
	}
}
