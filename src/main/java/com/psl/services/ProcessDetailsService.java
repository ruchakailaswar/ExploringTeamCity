package com.psl.services;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.stardust.common.log.LogManager;
import org.eclipse.stardust.common.log.Logger;
import org.eclipse.stardust.engine.api.query.ProcessInstances;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.psl.applications.IppService;



@Component
@Path("/processDetailsServices")
public class ProcessDetailsService {

	private static final Logger LOG = LogManager.getLogger(ProcessDetailsService.class);
	static Map<String, String> processDataMap = new HashMap<String, String>(); 

	@Autowired
	IppService ippService;


	public IppService getIppService() {
		return ippService;
	}

	public void setIppService(IppService ippService) {
		this.ippService = ippService;
	}






	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("getProcessHistory")
	public Response getProcessHistory(String input) {


		LOG.info("inside getActivityDetailsList  REST API : Getting Activity Details for a process OID !");
		Map<String,String> processDetails = null;
		List<Map<String,String>> activitiesDetails = null;

		try {
			JsonObject jsonObject = ippService.parseJSONObject(input);
			long processOid = jsonObject.get("processOid").getAsLong();
			boolean onlyInteractive = jsonObject.get("onlyInteractive").getAsBoolean();

			LOG.info("inside getActivityDetailsList  REST API : Process OID :"+processOid +" Only Interactive :"+onlyInteractive);


			try {

				processDetails = ippService.getProcessDetails(processOid);

			} catch (Exception e) {
				LOG.info("Exception in fetching process details : Getting Activity Details for a process OID ! "+e.getStackTrace());
				return Response.ok("No details Found!").build();
			}
			try{
				activitiesDetails = ippService.getActivitiesDetails(processOid,onlyInteractive);
			}catch(Exception e){
				LOG.info("Exception in fetching activity details : Getting Activity Details for a process OID !"+e.getStackTrace());
				return Response.ok("No details Found!").build();
			}
			JsonObject response = new JsonObject();
			JsonObject map = new JsonObject();
			for(String key:processDetails.keySet()){
				map.addProperty(key, processDetails.get(key).toString());
			}

			JsonArray activityList = new JsonArray();
			for(Map<String,String> activityDetails:activitiesDetails){
				JsonObject activityMap = new JsonObject();
				for (String activityKey:activityDetails.keySet()){
					activityMap.addProperty(activityKey, activityDetails.get(activityKey));
				}
				activityList.add(activityMap);

			}
			LOG.info(activityList);
			map.add("activitiesDetails", activityList);
			response.add("processDetails", map);
			return Response.ok(response.toString(), MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {
			LOG.info("Exception inside getProcessHistory  REST API : Get  Details for a process OID !");
			LOG.info("Exception inside getProcessHistory  REST API : Get  Details for a process OID !"+ e.getStackTrace());
			return Response.ok("Some Exceptions!").build();
		}
	}

	@GET 
	@Path("getProcessData") 
	@Produces(MediaType.APPLICATION_JSON) 
	public Response getProcessData(@QueryParam("processOid") String processOid) { 
		System.out.println("Inside get process data: processOid= "+processOid); 
		String output = "{\"response\":\"NotFound\"}"; 
		if(processDataMap.containsKey(processOid)){ 
			output = processDataMap.get(processOid); 
			return Response.status(200).entity(output).build(); 
		} 
		else{ 
			return Response.status(200).entity(output).build(); 
		} 
	} 

	@POST 
	@Path("postProcessData") 
	@Produces("text/html") 
	@Consumes(MediaType.APPLICATION_JSON) 
	public Response postProcessData(@QueryParam("processOid") String processOid, String data){ 
		System.out.println("Inside post process data: processOid= "+processOid); 
		System.out.println(data); 
		processDataMap.put(processOid, data); 
		String output = "success"; 
		return Response.status(200).entity(output).build(); 
	} 

	@GET 
	@Path("resetProcessData") 
	@Produces(MediaType.APPLICATION_JSON) 
	public Response resetProcessData(@QueryParam("processOid") String processOid) { 
		System.out.println("Inside reset process data: processOid= "+processOid); 
		String output = "{\"response\":\"NotFound\"}"; 
		if(processDataMap.containsKey(processOid)){ 
			processDataMap.remove(processOid); 
			output = "{\"response\":\"Success\"}"; 
			return Response.status(200).entity(output).build(); 
		} 
		else{ 
			return Response.status(200).entity(output).build(); 
		} 
	} 

	
	
	// Web service by Prerna
	@POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("getProcessDataPath")
    public Response getProcessDataPaths(String input) {
          LOG.info("inside getProcessDataPaths  REST API : Getting Process Data Paths for a Process ID !");
          JsonObject processDetails = null;
          boolean setDataPath = false;
          JsonObject dataPathJson = null;
          ProcessInstances processInstances = null;

          try {
                JsonObject jsonObject = ippService.parseJSONObject(input);

                LOG.info("inside getProcessDataPaths  REST API :  " + input);

                if (jsonObject.get("setDataPath") != null) {
                      setDataPath = Boolean.parseBoolean(jsonObject.get("setDataPath").getAsString());
                      jsonObject.remove("setDataPath");
                }

                if (jsonObject.get("DataPaths") != null) {

                      LOG.info("Data Paths to set : " + jsonObject.get("DataPaths").getAsJsonObject());
                      dataPathJson = jsonObject.get("DataPaths").getAsJsonObject();
                      jsonObject.remove("DataPaths");

                }

                LOG.info("inside getProcessDataPaths  REST API :  " + input);

                try {

                      processInstances = ippService.fetchProcessInstances(jsonObject);
                      if (processInstances.size() > 0) {
                            processDetails = ippService.fetchAndSetProcessDataPaths(processInstances, setDataPath,
                                        dataPathJson);
                      }
                      else {
                            processDetails = new JsonObject();
                            JsonArray ja = new JsonArray();
                            processDetails.add("ProcessDetails", ja);
                            return Response.ok(processDetails.toString(), MediaType.APPLICATION_JSON).build();

                      }
                } catch (Exception e) {

                      processDetails = new JsonObject();
                      JsonArray ja = new JsonArray();
                      processDetails.add("ProcessDetails", ja);
                      LOG.info("Exception in fetching process details : Getting Process Data Paths for a process OID ! "
                                  + e.getStackTrace());
                      return Response.serverError().entity(processDetails.toString()).build();
                      
                      
                }
                LOG.info(processDetails);
                return Response.ok(processDetails.toString(), MediaType.APPLICATION_JSON).build();

          } catch (Exception e) {
                LOG.info("Exception inside getProcessDataPaths  REST API :  Getting Process Data Paths for a Process ID !");
                LOG.info("Exception inside getProcessDataPaths  REST API :  Getting Process Data Paths for a Process ID !"
                            + e.getStackTrace());
                processDetails = new JsonObject();
                JsonArray ja = new JsonArray();
                processDetails.add("ProcessDetails", ja);
                return Response.serverError().build();
          }
    }


	// Web service by Prerna
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("abortProcessInstances")
    public Response abortProcessInstances(String input) {
          LOG.info("inside abortProcessInstances  REST API : Getting Process Data Paths for a Process ID !");
          ProcessInstances processInstances = null;
          JsonObject processDetails = null;
          String heirarchy = null;

          try {
                JsonObject jsonObject = ippService.parseJSONObject(input);

                if (jsonObject.get("processHeirarchy") != null) {
                      heirarchy = jsonObject.get("processHeirarchy").getAsString();
                      jsonObject.remove("processHeirarchy");
                }

                LOG.info("inside abortProcessInstances  REST API :  " + input);

                try {

                      processInstances = ippService.fetchProcessInstances(jsonObject);
                      if (processInstances.size() > 0) {
                            processDetails = ippService.abortProcessInstances(processInstances, heirarchy);
                      } else {
                            processDetails = new JsonObject();
                            JsonArray ja = new JsonArray();
                            processDetails.add("ProcessDetails", ja);
                            return Response.ok(processDetails.toString(), MediaType.APPLICATION_JSON).build();

                      }

                } catch (Exception e) {
                      LOG.info("Exception inside abortProcessInstances  REST API :  Can't Abort Process !");
                      LOG.info("Exception in fetching process details : Can't Abort Process! " + e.getStackTrace());
                      processDetails = new JsonObject();
                      JsonArray ja = new JsonArray();
                      processDetails.add("ProcessDetails", ja);
                      return Response.serverError().entity(processDetails.toString()).build();

                }
                LOG.info(processDetails);
                return Response.ok(processDetails.toString(), MediaType.APPLICATION_JSON).build();

          } catch (Exception e) {
                LOG.info("Exception inside abortProcessInstances  REST API :  Can't Abort Process !");
                LOG.info("Exception inside abortProcessInstances  REST API :  Can't Abort Process !" + e.getStackTrace());
                processDetails = new JsonObject();
                JsonArray ja = new JsonArray();
                processDetails.add("ProcessDetails", ja);
                return Response.serverError().build();
          }
    }

	/*@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("getProcessDataPath")
	public Response getProcessDataPaths(String input) {
		LOG.info("inside getProcessDataPaths  REST API : Getting Process Data Paths for a Process ID !");
		JsonObject processDetails = null;

		try {
			JsonObject jsonObject = ippService.parseJSONObject(input);

			LOG.info("inside getProcessDataPaths  REST API :  " + input);

			try {


				processDetails = ippService.getProcessDataPaths(jsonObject);

			} catch (Exception e) {
				LOG.info("Exception in fetching process details : Getting Process Data Paths for a process OID ! "
						+ e.getStackTrace());
				return Response.serverError().build();
			}
			LOG.info(processDetails);
			return Response.ok(processDetails.toString(), MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {
			LOG.info("Exception inside getProcessDataPaths  REST API :  Getting Process Data Paths for a Process ID !");
			LOG.info("Exception inside getProcessDataPaths  REST API :  Getting Process Data Paths for a Process ID !"
					+ e.getStackTrace());
			return Response.serverError().build();
		}
	}*/

	
	
	//experimental- it is an alternative to start VettingInstallation Process
	// as first handshake is taking too much time to response back to quotes.
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("startProcessById")
	public Response startProcess(String input) {
		LOG.info("inside startProcess  REST API");
		JsonObject processDetails = null;

		
			JsonObject jsonObject = ippService.parseJSONObject(input);
			
			

				String QuoteNumber = jsonObject.get("QuoteNumber").getAsString();
				String SchemeName_ = jsonObject.get("SchemeName_").getAsString();
				long processOid = jsonObject.get("ProcessInstanceOID").getAsLong();
				
				Map<String, Object> quoteControl = new HashMap<String, Object>();
				quoteControl.put("QuoteNumber", QuoteNumber);
				quoteControl.put("SchemeName_", SchemeName_);
				quoteControl.put("ProcessInstanceOID", processOid);
				
				 Map<String, Object> dataMap = new HashMap<String, Object>();
				 dataMap.put("QuoteControl", quoteControl);
				
				ippService.startProcessById("VettingAndInstallationProcess", dataMap);
				
			LOG.info(processDetails);
			return Response.ok(input.toString(), MediaType.APPLICATION_JSON).build();

		
	}





}
