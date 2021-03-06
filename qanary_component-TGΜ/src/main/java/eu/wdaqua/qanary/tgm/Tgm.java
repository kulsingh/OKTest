package eu.wdaqua.qanary.tgm;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryMessage;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.UUID;

/**
 * represents a wrapper of the Stanford NER tool used here as a spotter
 *
 * @author Dennis Diefenbach
 */

@Component
public class Tgm extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(Tgm.class);
    //private static final String foxService = "http://fox-demo.aksw.org/api";

    /**
     * default processor of a QanaryMessage
     */
    public static String runCurlPOSTWithParam(String weburl,String data,String contentType) throws Exception
	{
		/*URL url = new URL(weburl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		
		connection.setRequestProperty("Content-Type", contentType);
				
		DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
		wr.writeBytes(data);
		wr.flush();
		wr.close();
		
		
		String resp = "";
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(connection.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		resp = response.toString();
		
		System.out.println("Curl Response: \n"+resp);*/
    	

        String xmlResp = "";
        try {
            URL url = new URL(weburl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // String temp = "text=<?xml version=\"1.0\"
            // encoding=\"UTF-8\"?><annotation
            // text=\""+question+"\"><surfaceForm name=\"published\"
            // offset=\"23\" /><surfaceForm name=\"Heart\" offset=\"63\"
            // /></annotation>";
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type",contentType);

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());

            wr.write(("text=" + data).getBytes("UTF-8"));
            wr.flush();
            wr.close();

            // InputStreamReader ir = new
            // InputStreamReader(connection.getInputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            xmlResp = response.toString();
            logger.info("Response spotlight service {}", xmlResp);
        } catch (Exception e) {
        }
        return (xmlResp);

	}
    
    public QanaryMessage process(QanaryMessage myQanaryMessage) {
    	System.out.println("Kuldeep Singh");
        long startTime = System.currentTimeMillis();
        org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
        logger.info("Qanary Message: {}", myQanaryMessage);

        // STEP1: Retrieve the named graph and the endpoint
        String endpoint = myQanaryMessage.getEndpoint().toASCIIString();
        String namedGraph = myQanaryMessage.getInGraph().toASCIIString();
        logger.info("Endpoint: {}", endpoint);
        logger.info("InGraph: {}", namedGraph);

        // STEP2: Retrieve information that are needed for the computations
        //Retrive the uri where the question is exposed
        String sparql = "PREFIX qa:<http://www.wdaqua.eu/qa#> "
                + "SELECT ?questionuri "
                + "FROM <" + namedGraph + "> "
                + "WHERE {?questionuri a qa:Question}";
        
        String questionlang = "PREFIX qa:<http://www.wdaqua.eu/qa#> "
                + "SELECT ?lang "
                + "FROM <" + namedGraph + "> "
                + "WHERE {?q a qa:Question ?anno has:target ?q .?anno has:body ?lang .?anno a qa:AnnotationOfQuestionLanguage}";
        
        ResultSet result = selectTripleStore(sparql, endpoint);
        String uriQuestion = result.next().getResource("questionuri").toString();
        logger.info("Uri of the question: {}", uriQuestion);
        //Retrive the question itself
        RestTemplate restTemplate = new RestTemplate();
        //TODO: pay attention to "/raw" maybe change that
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(uriQuestion + "/raw", String.class);
        String question = responseEntity.getBody();
        logger.info("Question: {}", question);
        
        //String langQuestion= result.getResource("lang")
       // retrieve language of the question from Named Graph
        ResultSet result1 = selectTripleStore(questionlang, endpoint);
        String langQuestion = result1.next().getResource("lang").toString();
        logger.info("Language of the question: {}", langQuestion);
        //Retrive the question itself
        RestTemplate restTemplate1 = new RestTemplate();
        //TODO: pay attention to "/raw" maybe change that
        ResponseEntity<String> responseEntity1 = restTemplate1.getForEntity(langQuestion + "/raw", String.class);
        String lang = responseEntity1.getBody();
        logger.info("Language: {}", lang);
        
        System.out.println("lang:"+lang+"Question;"+question);
        // STEP3: Pass the information to the component and execute it
        //curl -d type=text -d task=NER -d output=N-Triples --data-urlencode "input=The foundation of the University of Leipzig in 1409 initiated the city's development into a centre of German law and the publishing industry, and towards being a location of the Reichsgericht (High Court), and the German National Library (founded in 1912). The philosopher and mathematician Gottfried Leibniz was born in Leipzig in 1646, and attended the university from 1661-1666." -H "Content-Type: application/x-www-form-urlencoded" http://fox-demo.aksw.org/api
        //Create body
       
        
        String url = "";
		String data = "";
		String contentType = "application/json";
		 
		//http://repository.okbqa.org/components/21
		//Sample input	
		/* 
		 * {
		  	"string": "Which river flows through Seoul?",
		  	"language": "en"
		   } http://ws.okbqa.org:1515/templategeneration/rocknrole
		*/
		url = "http://ws.okbqa.org:1515/templategeneration/rocknrole";
		data = "{  \"string\":"+question+",\"language\":"+lang+"}";//"{  \"string\": \"Which river flows through Seoul?\",  \"language\": \"en\"}";
		System.out.println("\ndata :" +data);
		System.out.println("\nComponent : 21");
		String output1="";
		try
		{
		output1= Tgm.runCurlPOSTWithParam(url, data, contentType);
		}catch(Exception e){}
		
        
		//return output1;

       /* // STEP4: Vocabulary alignment
        logger.info("Apply vocabulary alignment on outgraph");
        //Retrieve the triples from FOX
        JSONObject obj = new JSONObject(response);
        String triples = URLDecoder.decode(obj.getString("output"));

        //Create a new temporary named graph
        final UUID runID = UUID.randomUUID();
        String namedGraphTemp = "urn:graph:" + runID.toString();

        //Insert data into temporary graph
        sparql = "INSERT DATA { GRAPH <" + namedGraphTemp + "> {" + triples + "}}";
        logger.info(sparql);
        loadTripleStore(sparql, endpoint);

        //Align to QANARY vocabulary
        sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
                + "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
                + "prefix xsd: <http://www.w3.org/2001/XMLSchema#> "
                + "INSERT { "
                + "GRAPH <" + namedGraph + "> { "
                + "  ?a a qa:AnnotationOfSpotInstance . "
                + "  ?a oa:hasTarget [ "
                + "           a    oa:SpecificResource; "
                + "           oa:hasSource    <" + uriQuestion + ">; "
                + "           oa:hasSelector  [ "
                + "                    a oa:TextPositionSelector ; "
                + "                    oa:start ?begin ; "
                + "                    oa:end  ?end "
                + "           ] "
                + "  ] ; "
                + "     oa:annotatedBy <http://fox-demo.aksw.org> ; "
                + "	    oa:AnnotatedAt ?time  "
                + "}} "
                + "WHERE { "
                + "	SELECT ?a ?s ?begin ?end ?time "
                + "	WHERE { "
                + "		graph <" + namedGraphTemp + "> { "
                + "			?s	<http://ns.aksw.org/scms/beginIndex> ?begin . "
                + "			?s  <http://ns.aksw.org/scms/endIndex> ?end . "
                + "			BIND (IRI(str(RAND())) AS ?a) ."
                + "			BIND (now() as ?time) "
                + "		} "
                + "	} "
                + "}";
        loadTripleStore(sparql, endpoint);

        //Drop the temporary graph
        sparql = "DROP SILENT GRAPH <" + namedGraphTemp + ">";
        loadTripleStore(sparql, endpoint);
*/
        long estimatedTime = System.currentTimeMillis() - startTime;
        logger.info("Time: {}", estimatedTime);

        return myQanaryMessage;
    }

    private void loadTripleStore(String sparqlQuery, String endpoint) {
        UpdateRequest request = UpdateFactory.create(sparqlQuery);
        UpdateProcessor proc = UpdateExecutionFactory.createRemote(request, endpoint);
        proc.execute();
    }

    private ResultSet selectTripleStore(String sparqlQuery, String endpoint) {
        Query query = QueryFactory.create(sparqlQuery);
        QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, query);
        return qExe.execSelect();
    }

    private class Selection {
        public int begin;
        public int end;
    }

}


