package hello;

import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
public class HelloController {
	private final String USER_AGENT = "Mozilla/5.0";

    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }

    @RequestMapping("/entry")
    public String entry() {
    	StringBuilder url = new StringBuilder("http://localhost:8080/secondary");
    	HttpClient client = Application.getInstance().getCoreContainer().getUpdateShardHandler().getHttpClient();
    	//request.addHeader("User-Agent", USER_AGENT);
    	HttpGet request = new HttpGet(url.toString());
    	HttpResponse response;
		try {
			response = client.execute(request);
	    	BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

	    	StringBuffer result = new StringBuffer();
	    	String line = "";
	    	while ((line = rd.readLine()) != null) {
	    		result.append(line);
	    	}
	    	return (result.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
        return "error";
    }
    
    @RequestMapping("/secondary")
    public String secondary() {
        return "secondary\n";
    }
}