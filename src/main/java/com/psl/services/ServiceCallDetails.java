package com.psl.services;


import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.stardust.common.log.LogManager;
import org.eclipse.stardust.common.log.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.psl.applications.IppService;

@Component
@Path("/servicecalls")
public class ServiceCallDetails {
	
	public IppService getIppService() {
		return ippService;
	}

	public void setIppService(IppService ippService) {
		this.ippService = ippService;
	}

	private static final Logger LOG = LogManager.getLogger(ServiceCallDetails.class);
	/*@Autowired
	ProcessCommunicationComponent processComponent;*/
	
	@Autowired
	IppService ippService;
	
	private static final String SELECT_QUERY = "Select servicename, status from servicecall_events where processoid = ?";
	private static final String INSERT_QUERY = "{ call LCU_IPP_SERVICE_CALL.LCU_IPP_ADDSERVICECALLS ( ?, ? ) }";
	private static final String UPDATE_QUERY = "Update servicecall_events set status = ? where processoid = ? and servicename = ?";
	private static final String DELETE_QUERY = "Delete from servicecall_events where processoid = ?";
	private JdbcTemplate jdbcTemplate;
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("getServiceCallList/{processOId}")
	public Response getServiceCallList(@PathParam("processOId") String processOId) {
		LOG.info("Inside getServiceCallList 1");
		try {
			List<Map<String, Object>> rows = jdbcTemplate.queryForList(SELECT_QUERY, new Object[] {processOId});

			LOG.info("Inside getServiceCallList 2: rows: "+rows);
			
			JsonObject jo = null;
			JsonArray ja = new JsonArray();
			JsonObject mainObj = new JsonObject();
	    	try{
	    		for (Map<String, Object> row : rows) {
	    			jo = new JsonObject();
					jo.addProperty("ServiceName", (String)row.get("servicename")); 
					jo.addProperty("Status", (String)row.get("status"));
					ja.add(jo);
				}
	    		mainObj.add("WebServiceCalls", ja);
			}
			catch(Exception e){
				LOG.info("getServiceCallList NEW REST API : Exception 1 -- " +e);
				LOG.info("getServiceCallList NEW REST API : Exception 2 -- " +e.getStackTrace());
				LOG.info("getServiceCallList NEW REST API : Exception 3 -- " +e.getCause());
				e.printStackTrace();
				return Response.ok("JSON Error!").build();
			}
			return Response.ok(mainObj.toString(), MediaType.APPLICATION_JSON_TYPE).build();
			
		} catch (Exception e) {
			LOG.warn("Unable to lookup services for: " + processOId);
			return Response.ok("JSON Error!").build();
		}
	}
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("insertNewServiceCalls/{processOId}/{processId}")
	public Response addServiceCallList(@PathParam("processOId") String processOId, @PathParam("processId") String processId) {
		LOG.info("Inside addServiceCallList 1");
		try {
			int row = jdbcTemplate.update(INSERT_QUERY, processOId, processId);

			LOG.info("Inside addServiceCallList 2");
			
			JsonObject jo = null;
			JsonObject mainObj = new JsonObject();
	    	try{
    			jo = new JsonObject();
				jo.addProperty("rows", Integer.toString(row)); 
	    		mainObj.add("InsertWebServiceCalls", jo);
			}
			catch(Exception e){
				LOG.info("addServiceCallList NEW REST API : Exception 1 -- " +e);
				LOG.info("addServiceCallList NEW REST API : Exception 2 -- " +e.getStackTrace());
				LOG.info("addServiceCallList NEW REST API : Exception 3 -- " +e.getCause());
				e.printStackTrace();
				return Response.ok("JSON Error!").build();
			}
			return Response.ok(mainObj.toString(), MediaType.APPLICATION_JSON_TYPE).build();
			
		} catch (Exception e) {
			LOG.warn("Unable to add services for: " + processOId);
			return Response.ok("JSON Error!").build();
		}
	}
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("updateServiceCalls/{processOId}/{status}/{servicename}")
	public Response updateServiceCallList(@PathParam("processOId") String processOId, @PathParam("status") String status, @PathParam("servicename") String servicename) {
		LOG.info("Inside updateServiceCallList 1");
		try {
			int row = jdbcTemplate.update(UPDATE_QUERY, status, processOId, servicename);

			LOG.info("Inside updateServiceCallList 2");
			
			JsonObject jo = null;
			JsonObject mainObj = new JsonObject();
	    	try{
    			jo = new JsonObject();
				jo.addProperty("rows", Integer.toString(row)); 
	    		mainObj.add("UpdateWebServiceCalls", jo);
			}
			catch(Exception e){
				LOG.info("updateServiceCallList NEW REST API : Exception 1 -- " +e);
				LOG.info("updateServiceCallList NEW REST API : Exception 2 -- " +e.getStackTrace());
				LOG.info("updateServiceCallList NEW REST API : Exception 3 -- " +e.getCause());
				e.printStackTrace();
				return Response.ok("JSON Error!").build();
			}
			return Response.ok(mainObj.toString(), MediaType.APPLICATION_JSON_TYPE).build();
			
		} catch (Exception e) {
			LOG.warn("Unable to update services for: " + processOId);
			return Response.ok("JSON Error!").build();
		}
	}
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("deleteServiceCalls/{processOId}")
	public Response deleteServiceCallList(@PathParam("processOId") String processOId) {
		LOG.info("Inside deleteServiceCallList 1");
		try {
			int row = jdbcTemplate.update(DELETE_QUERY, processOId);

			LOG.info("Inside deleteServiceCallList 2");
			
			JsonObject jo = null;
			JsonObject mainObj = new JsonObject();
	    	try{
    			jo = new JsonObject();
				jo.addProperty("rows", Integer.toString(row)); 
	    		mainObj.add("DeleteWebServiceCalls", jo);
			}
			catch(Exception e){
				LOG.info("deleteServiceCallList NEW REST API : Exception 1 -- " +e);
				LOG.info("deleteServiceCallList NEW REST API : Exception 2 -- " +e.getStackTrace());
				LOG.info("deleteServiceCallList NEW REST API : Exception 3 -- " +e.getCause());
				e.printStackTrace();
				return Response.ok("JSON Error!").build();
			}
			return Response.ok(mainObj.toString(), MediaType.APPLICATION_JSON_TYPE).build();
			
		} catch (Exception e) {
			LOG.warn("Unable to delete services for: " + processOId);
			return Response.ok("JSON Error!").build();
		}
	}
	
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource); 
	}

	public JdbcTemplate getJdbcTemplate() {
		return this.jdbcTemplate;
	}
	
	
	
}