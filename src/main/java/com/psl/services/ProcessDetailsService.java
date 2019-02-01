package com.psl.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.sql.Clob;
import javax.sql.DataSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.eclipse.stardust.common.StringUtils;
import org.eclipse.stardust.common.log.LogManager;
import org.eclipse.stardust.common.log.Logger;
import org.eclipse.stardust.engine.api.query.ProcessInstances;
import org.eclipse.stardust.engine.api.runtime.ActivityInstance;
import org.eclipse.stardust.engine.api.runtime.ProcessInstance;
import org.eclipse.stardust.engine.api.runtime.UserGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.psl.applications.IppService;
import com.psl.beans.ApplicationConstants;

@Component
@Path("/processDetailsServices")
public class ProcessDetailsService {

	private static final Logger LOG = LogManager.getLogger(ProcessDetailsService.class);
	// static Map<String, String> processDataMap = new HashMap<String,
	// String>();

	private static final String DELETE_JSON_DATA = "Delete from process_json_data where processoid = ?";
	private static final String SELECT_JSON_DATA = "Select json_data from process_json_data where processoid = ?";
	private static final String INSERT_JSON_DATA = "INSERT into process_json_data(processOid,json_data) values(?,?)";
	private static final String UPDATE_JSON_DATA = "UPDATE process_json_data SET json_data = ? WHERE processOid = ?";
	private static final String INSERT_WORKTYPE = "INSERT into WORK_TYPES (TEXT,START_PROCESS,QC,AUTHORIZATION_REQUIRED,ACTIVE,INDEXING_PRIORITY,CONDITIONAL_PERFORMER,ADMINISTRATOR,AUTHORIZER,QC_OPERATOR) values (?,'{DynamicModel}DynamicProcess','N','N','Y','Normal','{Liberty}IndexOperator','{Liberty}IndexOperator',null,null)";

	@Autowired
	IppService ippService;
	private JdbcTemplate jdbcTemplate;

	public IppService getIppService() {
		return ippService;
	}

	public void setIppService(IppService ippService) {
		this.ippService = ippService;
	}

	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public JdbcTemplate getJdbcTemplate() {
		return this.jdbcTemplate;
	}

	/**
	 * Fetches the audit trail of the process oid passed. Can be filtered to
	 * fetch only interactive activities details
	 * 
	 * @param String
	 *            input Stringified JSON which consists of Process Oid and
	 *            onlyInteractive boolean flag
	 * @return
	 */
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("getProcessHistory")
	public Response getProcessHistory(String input) {

		LOG.info("inside getActivityDetailsList  REST API : Getting Activity Details for a process OID !");
		Map<String, String> processDetails = null;
		List<Map<String, String>> activitiesDetails = null;

		try {
			JsonObject jsonObject = ippService.parseJSONObject(input);
			long processOid = jsonObject.get("processOid").getAsLong();
			boolean onlyInteractive = jsonObject.get("onlyInteractive").getAsBoolean();

			LOG.info("inside getActivityDetailsList  REST API : Process OID :" + processOid + " Only Interactive :"
					+ onlyInteractive);

			try {

				processDetails = ippService.getProcessDetails(processOid);

			} catch (Exception e) {
				LOG.info("Exception in fetching process details : Getting Activity Details for a process OID ! "
						+ e.getStackTrace());
				return Response.ok("No details Found!").build();
			}
			try {
				activitiesDetails = ippService.getActivitiesDetails(processOid, onlyInteractive);
			} catch (Exception e) {
				LOG.info("Exception in fetching activity details : Getting Activity Details for a process OID !"
						+ e.getStackTrace());
				return Response.ok("No details Found!").build();
			}
			JsonObject response = new JsonObject();
			JsonObject map = new JsonObject();
			for (String key : processDetails.keySet()) {
				map.addProperty(key, processDetails.get(key).toString());
			}

			JsonArray activityList = new JsonArray();
			for (Map<String, String> activityDetails : activitiesDetails) {
				JsonObject activityMap = new JsonObject();
				for (String activityKey : activityDetails.keySet()) {
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
			LOG.info("Exception inside getProcessHistory  REST API : Get  Details for a process OID !"
					+ e.getStackTrace());
			return Response.ok("Some Exceptions!").build();
		}
	}

	/**
	 * Service is used to get the temporary saved data from a map
	 * 
	 * @param processOid
	 * @return
	 */

	@GET
	@Path("getProcessData")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProcessData(@QueryParam("processOid") String processOid) {
		String output = "{\"response\":\"NotFound\"}";
		Clob outData = null;
		try {
			// List<Map<String, Object>> rows =
			// jdbcTemplate.queryForList(SELECT_JSON_DATA, new Object[] {
			// processOid });
			SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet(SELECT_JSON_DATA, new Object[] { processOid });
			while (sqlRowSet.next()) {
				try {
					outData = (Clob) sqlRowSet.getObject("json_data");
					if (outData == null || outData.length() <= 0) {
						output = "{}";
						return Response.status(200).entity(output).build();
					}
					Reader reader = outData.getCharacterStream();
					BufferedReader buffReader = new BufferedReader(reader);
					output = IOUtils.toString(buffReader);
				} catch (Exception e) {
					LOG.info("getProcessData NEW REST API : Exception 1 -- " + e);
					LOG.info("getProcessData NEW REST API : Exception 2 -- " + e.getStackTrace());
					LOG.info("getProcessData NEW REST API : Exception 3 -- " + e.getCause());
					e.printStackTrace();
					return Response.ok("JSON Error!").build();
				}
			}

			return Response.status(200).entity(output).build();

		} catch (Exception e) {
			LOG.warn("Unable to lookup services for: " + processOid);
			e.printStackTrace();
			return Response.ok("JSON Error!").build();
		}

		/*
		 * if (processDataMap.containsKey(processOid)) { output =
		 * processDataMap.get(processOid); return
		 * Response.status(200).entity(output).build(); } else { return
		 * Response.status(200).entity(output).build(); }
		 */
	}

	/**
	 * Service is used to save temporary data in a map
	 * 
	 * @param processOid
	 * @param data
	 * @return
	 */

	@POST
	@Path("postProcessData")
	@Produces("text/html")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response postProcessData(@QueryParam("processOid") String processOid, String data) {
		LOG.info("Inside postProcessData");
		String output = "success";
		int row = 0;
		try {

			row = jdbcTemplate.update(UPDATE_JSON_DATA, data, processOid);

			if (row <= 0)
				row = jdbcTemplate.update(INSERT_JSON_DATA, processOid, data);

			return Response.status(200).entity(output).build();

		} catch (Exception e) {
			LOG.error(e.getMessage());
			e.printStackTrace();
			LOG.warn("Unable to postdata for: " + processOid);
			return Response.ok("JSON Error!").build();
		}
		// processDataMap.put(processOid, data);
		// String output = "success";
		// return Response.status(200).entity(output).build();
	}

	/**
	 * Service is used to delete the temporary saved data from a map
	 * 
	 * @param processOid
	 * @return
	 */

	@GET
	@Path("resetProcessData")
	@Produces(MediaType.APPLICATION_JSON)
	public Response resetProcessData(@QueryParam("processOid") String processOid) {
		String output = "{\"response\":\"NotFound\"}";
		try {
			int row = jdbcTemplate.update(DELETE_JSON_DATA, processOid);

			if (row > 0) {
				output = "{\"response\":\"Success\"}";
			}
			LOG.info("Inside resetProcessData");
			return Response.status(200).entity(output).build();
		} catch (Exception e) {
			LOG.warn("Unable to reset object data for: " + processOid);
			return Response.ok("JSON Error!").build();
		}
		/*
		 * if (processDataMap.containsKey(processOid)) {
		 * processDataMap.remove(processOid); output =
		 * "{\"response\":\"Success\"}"; return
		 * Response.status(200).entity(output).build(); } else { return
		 * Response.status(200).entity(output).build(); }
		 */
	}

	/**
	 * Fetches the process details of the processes based on member
	 * number/scheme number/case number for Scanning and Indexing process
	 * 
	 * @param input
	 *            contains the input json
	 * @return
	 */
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("getProcessDataForIndexing")
	public Response getProcessDataForIndexing(String input,
			@QueryParam("showCompleted") @DefaultValue(value = "false") boolean showCompleted) {
		LOG.info("inside getProcessDataForIndexing  REST API : Getting Process Data from IPP for input fields !");
		JsonObject jo = null;
		JsonObject mainObj = new JsonObject();
		long processInstanceOID = 0L;
		String memberNo, memberName, schemeNo = "";
		List<ProcessInstance> processInstances = null;
		List<ActivityInstance> activitiesDetails = null;

		try {
			JsonObject jsonObject = ippService.parseJSONObject(input);
			jsonObject.addProperty("showCompleted", showCompleted);

			LOG.info("inside getProcessDataForIndexing  REST API :  " + input);

			try {
				processInstances = ippService.fetchProcessByProcessOID(jsonObject);

				if (processInstances == null || processInstances.size() == 0) {
					mainObj = new JsonObject();
					JsonArray ja = new JsonArray();
					mainObj.add("processInstances", ja);
					return Response.ok(mainObj.toString(), MediaType.APPLICATION_JSON).build();
				}

				JsonArray ja = new JsonArray();

				for (Iterator iterator = processInstances.iterator(); iterator.hasNext();) {
					ProcessInstance pi = (ProcessInstance) iterator.next();
					processInstanceOID = pi.getOID();

					memberNo = (String) ippService.getProcessData(processInstanceOID,
							ApplicationConstants.META_DATA_MEMBER_NO.getValue());
					memberName = (String) ippService.getProcessData(processInstanceOID,
							ApplicationConstants.MEMBER_NAME_XPATH.getValue());
					schemeNo = (String) ippService.getProcessData(processInstanceOID,
							ApplicationConstants.META_DATA_SCHEME_NO.getValue());

					jo = new JsonObject();
					ja.add(jo);
					jo.addProperty("oid", processInstanceOID);
					jo.addProperty("rootProcessOid", pi.getRootProcessInstanceOID());

					JsonObject processJson = new JsonObject();
					jo.add("processDefinition", processJson);

					if (pi.getProcessID()
							.equals(ApplicationConstants.ROUTE_BY_WORKTYPE_PROCESS_DEFINITION.getValue())) {
						processJson.addProperty("name", (String) ippService.getProcessData(processInstanceOID,
								ApplicationConstants.WORK_TYPE_ID.getValue()));
					}else{
					processJson.addProperty("name", pi.getProcessName());
					}
					processJson.addProperty("id", processInstanceOID);

					JsonArray descriptorsJson = new JsonArray();

					JsonObject descriptorJson = new JsonObject();
					descriptorJson.addProperty("id", "memberNo");
					descriptorJson.addProperty("name", "Member No");
					descriptorJson.addProperty("value", memberNo);
					descriptorsJson.add(descriptorJson);

					descriptorJson = new JsonObject();
					descriptorJson.addProperty("id", "memberName");
					descriptorJson.addProperty("name", "Member Name");
					descriptorJson.addProperty("value", memberName);
					descriptorsJson.add(descriptorJson);

					descriptorJson = new JsonObject();
					descriptorJson.addProperty("id", "schemeNo");
					descriptorJson.addProperty("name", "Scheme No");
					descriptorJson.addProperty("value", schemeNo);
					descriptorsJson.add(descriptorJson);

					jo.add("descriptors", descriptorsJson);

					activitiesDetails = ippService.getActivitiesDetailsForIndexing(processInstanceOID);
					JsonArray activityList = new JsonArray();

					for (ActivityInstance ai : activitiesDetails) {

						JsonObject activityObject = new JsonObject();
						activityObject.addProperty("oid", ai.getOID());
						activityObject.addProperty("start", ai.getStartTime().getTime());

						JsonObject activityJson = new JsonObject();
						activityObject.add("activity", activityJson);
						activityJson.addProperty("name", ai.getActivity().getName());
						activityJson.add("requiredDocuments", new JsonArray());

						activityList.add(activityObject);
					}

					LOG.info(activityList);
					jo.add("pendingActivityInstances", activityList);

				}

				mainObj.add("processInstances", ja);

			} catch (Exception e) {
				mainObj = new JsonObject();
				JsonArray ja = new JsonArray();
				mainObj.add("processInstances", ja);
				LOG.info(
						"Exception inside getProcessDataForIndexing while fetching process details 1" + e.getMessage());
				LOG.info("Exception inside getProcessDataForIndexing while fetching process details 2" + e.getCause());
				LOG.info("Exception inside getProcessDataForIndexing while fetching process details 3"
						+ e.fillInStackTrace());
				return Response.serverError().entity(mainObj.toString()).build();
			}

			return Response.ok(mainObj.toString(), MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {
			LOG.info(
					"Exception inside getProcessDataForIndexing  REST API :  Getting Process Data Paths for a Process ID !"
							+ e.getStackTrace());
			mainObj = new JsonObject();
			JsonArray ja = new JsonArray();
			mainObj.add("processInstances", ja);
			return Response.serverError().build();
		}
	}

	/**
	 * Fetches the data paths of the process based on the process ID and Data
	 * IDs passed. Can also be used to set data paths based on the flag passed
	 * 
	 * @param input
	 * @return
	 */
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
				} else {
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

	/**
	 * Aborts the process instance based on the process ID and the data IDs. Has
	 * a flag to specify the process hierarchy for abortion
	 * 
	 * @param input
	 * @return
	 */
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

			if (jsonObject != null && jsonObject.get("processHeirarchy") != null) {
				heirarchy = jsonObject.get("processHeirarchy").getAsString();
				jsonObject.remove("processHeirarchy");
			}

			LOG.info("inside abortProcessInstances  REST API :  " + input);
			processInstances = ippService.fetchProcessInstances(jsonObject);
			if (processInstances.size() > 0) {
				processDetails = ippService.abortProcessInstances(processInstances, heirarchy);
			} else {
				processDetails = new JsonObject();
				JsonArray ja = new JsonArray();
				processDetails.add("ProcessDetails", ja);
				return Response.ok(processDetails.toString(), MediaType.APPLICATION_JSON).build();

			}
			LOG.info(processDetails);
			return Response.ok(processDetails.toString(), MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {
			LOG.info("Exception inside abortProcessInstances  REST API :  Can't Abort Process !" + e.getCause());
			LOG.info("Exception inside abortProcessInstances  REST API :  Can't Abort Process !" + e.getMessage());
			LOG.info("Exception inside abortProcessInstances  REST API :  Can't Abort Process !" + e.getStackTrace());
			processDetails = new JsonObject();
			JsonArray ja = new JsonArray();
			processDetails.add("ProcessDetails", ja);
			return Response.serverError().entity(processDetails.toString()).build();
		}
	}

	// experimental- it is an alternative to start VettingInstallation Process
	// as first handshake is taking too much time to response back to quotes.
	/*
	 * @POST
	 * 
	 * @Produces(MediaType.APPLICATION_JSON)
	 * 
	 * @Consumes(MediaType.APPLICATION_JSON)
	 * 
	 * @Path("startProcessById") public Response startProcess(String input) {
	 * LOG.info("inside startProcess  REST API"); JsonObject processDetails =
	 * null;
	 * 
	 * 
	 * JsonObject jsonObject = ippService.parseJSONObject(input);
	 * 
	 * 
	 * 
	 * String QuoteNumber = jsonObject.get("QuoteNumber").getAsString(); String
	 * SchemeName_ = jsonObject.get("SchemeName_").getAsString(); long
	 * processOid = jsonObject.get("ProcessInstanceOID").getAsLong();
	 * 
	 * Map<String, Object> quoteControl = new HashMap<String, Object>();
	 * quoteControl.put("QuoteNumber", QuoteNumber);
	 * quoteControl.put("SchemeName_", SchemeName_);
	 * quoteControl.put("ProcessInstanceOID", processOid);
	 * 
	 * Map<String, Object> dataMap = new HashMap<String, Object>();
	 * dataMap.put("QuoteControl", quoteControl);
	 * 
	 * ippService.startProcessById("VettingAndInstallationProcess", dataMap);
	 * 
	 * LOG.info(processDetails); return Response.ok(input.toString(),
	 * MediaType.APPLICATION_JSON).build();
	 * 
	 * 
	 * }
	 */

	/**
	 * Fetches the interrupted process/activity details in an excel format
	 * 
	 * @return
	 * @throws IOException
	 * @throws InvalidFormatException
	 */
	@GET
	@Path("getInterruptedActivityCount")
	@Produces("application/vnd.ms-excel")
	public Response getInterruptedActivityCount() throws IOException, InvalidFormatException {

		File f = ippService.getProcessCount();

		ResponseBuilder response = Response.ok((Object) f);
		response.header("Content-Disposition", "attachment; filename=new-excel-file.xls");
		return response.build();

	}

	@POST
	@Path("createUserGroup")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createUserGroup(String input) {

		String id = "";
		String name = "";
		String desc = "";
		JsonObject jsonObject = ippService.parseJSONObject(input);

		if (jsonObject.get("id") != null) {
			id = jsonObject.get("id").getAsString();
		} else {
			return Response.status(500).entity("{\"response\":\"'id' parameter missing in request body.\"}").build();
		}

		if (jsonObject.get("name") != null) {
			name = jsonObject.get("name").getAsString();
		} else {
			return Response.status(500).entity("{\"response\":\"'name' parameter missing in request body.\"}").build();
		}

		if (jsonObject.get("description") != null) {
			desc = jsonObject.get("description").getAsString();
		} else {
			return Response.status(500).entity("{\"response\":\"'description' parameter missing in request body.\"}")
					.build();
		}
		jsonObject = null;
		UserGroup userGroup = ippService.createUserGroup(name, id, desc);

		if (userGroup != null) {
			return Response.status(200).entity("{\"response\":\"UserGroup created successfully\"}").build();
		} else {
			return Response.status(500)
					.entity("{\"response\":\"UserGroup could not be created. Please try again later.\"}").build();
		}

	}

	@POST
	@Path("completeMultipleActivities")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response completeMultipleActivities(String input) {
		List<Long> activityOidList = new ArrayList<Long>();
		Map<Long, String> statusMap = new HashMap<Long, String>();
		JsonArray activityArray = new JsonArray();
		Long currentActivityOid;
		JsonObject outerObject = new JsonObject();
		JsonObject innerObject;
		JsonArray jsonArray = new JsonArray();

		try {
			JsonObject jsonObject = ippService.parseJSONObject(input);
			if (jsonObject.get("activityOids") != null) {
				activityArray = jsonObject.get("activityOids").getAsJsonArray();
			} else {
				return Response.status(400)
						.entity("{\"response\":\"'activityOids' parameter missing in request body.\"}").build();
			}
			for (int i = 0; i < activityArray.size(); i++) {
				currentActivityOid = activityArray.get(i).getAsLong();
				activityOidList.add(currentActivityOid);
			}
			statusMap = ippService.completeSuspendedActivities(activityOidList);

			for (Map.Entry<Long, String> mapEntry : statusMap.entrySet()) {
				innerObject = new JsonObject();
				innerObject.addProperty("ActivityOid", mapEntry.getKey());
				innerObject.addProperty("Status", mapEntry.getValue());
				jsonArray.add(innerObject);
			}
			outerObject.add("response", jsonArray);

		} catch (Exception e) {
			LOG.info("Exception inside completeMultipleActivities  REST API :  Can't complete Activity !"
					+ e.getCause());
			LOG.info("Exception inside completeMultipleActivities  REST API :  Can't complete Activity !"
					+ e.getMessage());
			LOG.info("Exception inside completeMultipleActivities  REST API :  Can't complete Activity !"
					+ e.getStackTrace());
			return Response.serverError().build();
		}
		return Response.ok(outerObject.toString(), MediaType.APPLICATION_JSON_TYPE)
				.header("Access-Control-Allow-Origin", "*").build();
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("addWorktypeToDb/{workType}")
	public Response addWorktypeToDb(@PathParam("workType") String workType) {
		LOG.info("Adding Worktype to the worktype table :  Started");
		try {
			int row = jdbcTemplate.update(INSERT_WORKTYPE, workType);

			LOG.info("Inside addWorktypeToDb ");

			JsonObject jo = null;
			JsonObject mainObj = new JsonObject();
			try {
				jo = new JsonObject();
				jo.addProperty("rows", Integer.toString(row));
				mainObj.add("AddWorktype", jo);
			} catch (Exception e) {
				LOG.info("addWorktypeToDb NEW REST API : Exception  -- " + e);
				LOG.info("addWorktypeToDb NEW REST API : Exception  -- " + e.getStackTrace());
				LOG.info("addWorktypeToDb NEW REST API : Exception  -- " + e.getCause());
				return Response.ok("JSON Error!").build();
			}
			return Response.ok(mainObj.toString(), MediaType.APPLICATION_JSON_TYPE).build();
		} catch (Exception e) {
			LOG.info("addWorktypeToDb NEW REST API : Exception  -- " + e);
			LOG.info("addWorktypeToDb NEW REST API : Exception  -- " + e.getStackTrace());
			LOG.info("addWorktypeToDb NEW REST API : Exception  -- " + e.getCause());
			LOG.warn("Unable to add services for: " + workType);
			return Response.ok("JSON Error!").build();
		}

	}
}