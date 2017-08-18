package com.psl.services;


import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.stardust.common.log.LogManager;
import org.eclipse.stardust.common.log.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.psl.applications.IppService;



@Component
@Path("/processDetailsServices")
public class ProcessDetailsService {
	
	private static final Logger LOG = LogManager.getLogger(ProcessDetailsService.class);

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

}
