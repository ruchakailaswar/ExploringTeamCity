package com.psl.services;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.stardust.common.log.LogManager;
import org.eclipse.stardust.common.log.Logger;
import org.eclipse.stardust.engine.api.runtime.DeploymentInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.JsonObject;
import com.psl.applications.IppService;
import com.psl.beans.ApplicationConstants;

@Component
@Path("/deploymentServices")
public class DeploymentServices {
	private static final Logger LOG = LogManager.getLogger(ProcessDetailsService.class);


	@Autowired
	IppService ippService;

	public IppService getIppService() {
		return this.ippService;
	}

	public void setIppService(IppService ippService) {
		this.ippService = ippService;
	}

	/**
	 * Download the model from the server based on the ModelOID passed
	 * 
	 * @param input
	 * @return
	 */

	@POST
	@Path("model-instance/{oid: \\d+}/downloadModel")
	@Consumes({ "application/json" })
	@Produces({ "application/json" })
	public Response downloadModel(@PathParam("oid") String modelOid, String input) {
		//

		String path = null;
		String output = "{\"response\":\"NotFound\"}";

		JsonObject jsonObject = ippService.parseJSONObject(input);
		if (jsonObject.get(ApplicationConstants.PATH_PARAM.getValue()) != null) {
			path = jsonObject.get(ApplicationConstants.PATH_PARAM.getValue()).getAsString();
			jsonObject.remove(ApplicationConstants.PATH_PARAM.getValue());
		} else {
			return Response.status(400).entity(ApplicationConstants.PATH_ERROR.getValue()).build();
		}

		long mOid = Long.parseLong(modelOid);
		String model = ippService.getQueryService().getModelAsXML(mOid);
		byte[] byteArray = model.getBytes();

		File myFile = new File(path);
		FileOutputStream opStream = null;
		try {
			if (!myFile.exists()) {
				myFile.createNewFile();
			}
			opStream = new FileOutputStream(myFile);
			opStream.write(byteArray);
			opStream.flush();
		} catch (IOException e) {
			LOG.info("Exception in downloading model : Downloading model for the modelOID -- " + e);
			LOG.info("Exception in downloading model : Downloading model for the modelOID-- " + e.getStackTrace());
			LOG.info("Exception in downloading model : Downloading model for the modelOID -- " + e.getCause());
		} finally {
			try {
				if (opStream != null)
					opStream.close();
			} catch (Exception ex) {
				LOG.info("Exception in downloading model : Closing the FileOutputStram! " + ex);
				LOG.info("Exception in downloading model : Closing the FileOutputStram! " + ex.getStackTrace());
				LOG.info("Exception in downloading model : Closing the FileOutputStram! " + ex.getCause());
			}
		}

		if (byteArray != null) {
			output = "{\"response\":\"Success\"}";
			return Response.status(200).entity(output).build();
		}
		return Response.status(500).entity(output).build();

	}

	/**
	 * Deploys the model from GIT Repository or Local File/Folder Location based
	 * on the input flag passed
	 * 
	 * @param input
	 * @return
	 */
	@POST
	@Path("deployModel")
	@Consumes({ "application/json" })
	@Produces({ "application/json" })
	public Response deployModel(String input) {
		LOG.info("Automatic Deployment of Models : Started " + input);
		List<DeploymentInfo> info = new ArrayList<DeploymentInfo>();
		File dir = null;

		String path = "";
		String fileNames = "";
		boolean isFailure = false;
		JsonObject outer = new JsonObject();
		JsonObject response = new JsonObject();
		JsonObject error_warnings;
		JsonObject warnings;
		String modelId;
		Response responseObj = null;

		HashMap<String, String> completeData = null;
		boolean filenamesExists = false;
		boolean gitRepo = true;
		try {
			JsonObject jsonObject = ippService.parseJSONObject(input);
			java.lang.System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");

			if (jsonObject.get(ApplicationConstants.GIT_REPO_PARAM.getValue()) != null) {
				gitRepo = jsonObject.get(ApplicationConstants.GIT_REPO_PARAM.getValue()).getAsBoolean();
				jsonObject.remove(ApplicationConstants.GIT_REPO_PARAM.getValue());

			}

			if (jsonObject.get(ApplicationConstants.PATH_PARAM.getValue()) != null) {
				path = jsonObject.get(ApplicationConstants.PATH_PARAM.getValue()).getAsString();

				if (jsonObject.get(ApplicationConstants.FILENAME_PARAM.getValue()) != null) {
					fileNames = jsonObject.get(ApplicationConstants.FILENAME_PARAM.getValue()).getAsString();

					filenamesExists = true;
					jsonObject.remove(ApplicationConstants.FILENAME_PARAM.getValue());
				}

				if (!gitRepo) {
					dir = new File(path);
					if (!dir.isDirectory()) {
						return Response.status(500).entity(ApplicationConstants.INVALID_PATH_ERROR.getValue()).build();
					}
					completeData = ippService.getFilesFromDirectory(path, dir, filenamesExists, fileNames);
				} else {
					completeData = ippService.getFilesFromRepository(path, filenamesExists, fileNames);
				}

				jsonObject.remove(ApplicationConstants.PATH_PARAM.getValue());

			} else {
				return Response.status(400).entity(ApplicationConstants.PATH_ERROR.getValue()).build();
			}

			if (completeData != null && completeData.size() > 0) {
				for (String string : completeData.keySet()) {
					LOG.info("Model Name = " + string);
				}
				info = this.ippService.changeConfigVariablesandDeployModel(completeData, jsonObject, gitRepo);
			} else {
				LOG.info("Inside else");
				return Response.status(400)
						.entity("{\"response\":\"Could not get the model files for deployment.. Please check and try again..!!\"}")
						.build();
			}

			
		} catch (Exception e) {
			LOG.info("Deploy Model REST API : Exception in deployModel -- " + e);
			LOG.info("Deploy Model REST API : Exception in deployModel -- " + e.getStackTrace());
			LOG.info("Deploy Model REST API : Exception in deployModel -- " + e.getCause());
			return Response.status(400)
					.entity("{\"response\":\"Could not get the model files for deployment.. Please check and try again..!!\"}")
					.build();
			
		}

		if (info != null) {
			for (DeploymentInfo deploymentInfo : info) {
				if (deploymentInfo.hasErrors()) {
					isFailure = true;
					break;

				}
			}

			if (isFailure) {
				response.addProperty("result", "Failure");
				for (DeploymentInfo deploymentInfo : info) {
					if (deploymentInfo.hasErrors() || deploymentInfo.hasWarnings()) {
						modelId = deploymentInfo.getId();
						error_warnings = new JsonObject();
						error_warnings.addProperty("Errors", (String) deploymentInfo.getErrors().toString());
						error_warnings.addProperty("Warnings", (String) deploymentInfo.getWarnings().toString());
						response.add(modelId, error_warnings);

					}
				}
			} else {
				response.addProperty("result", "Successfully Deployed Models");
				for (DeploymentInfo deploymentInfo : info) {
					if (deploymentInfo.hasWarnings()) {
						modelId = deploymentInfo.getId();
						warnings = new JsonObject();
						warnings.addProperty("Warnings", (String) deploymentInfo.getWarnings().toString());
						LOG.warn(warnings);
						response.add(modelId, warnings);
						LOG.info(response);

					}
				}
			}

			outer.add("response", response);

			if (isFailure) {
				responseObj = Response.serverError().status(500).entity(outer.toString()).build();
				
			} else {
				responseObj = Response.ok(outer.toString(), MediaType.APPLICATION_JSON).status(200).build();

			}
		} else {
			String failedMessage = "{\"response\":\"Something went wrong. Please try again!.\"}";
			responseObj = Response.status(500).entity(failedMessage).build();
		}
		return responseObj;
	}
	

}
