package com.psl.services;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.eclipse.stardust.common.log.LogManager;
import org.eclipse.stardust.common.log.Logger;
import org.eclipse.stardust.engine.api.runtime.DeploymentInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.psl.applications.IppService;

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
		String pathError = "{\"response\":\"'path' parameter missing in request body.\"}";

		JsonObject jsonObject = ippService.parseJSONObject(input);
		if (jsonObject.get("path") != null) {
			path = jsonObject.get("path").getAsString();
			jsonObject.remove("path");
		} else {
			return Response.status(400).entity(pathError).build();
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
			LOG.info("Exception in downloading model : Downloading model for the modelOID! " + e.getStackTrace());
		} finally {
			try {
				if (opStream != null)
					opStream.close();
			} catch (Exception ex) {
				LOG.info("Exception in downloading model : Closing the FileOutputStram! " + ex.getStackTrace());
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
		List<DeploymentInfo> info = new ArrayList<DeploymentInfo>();
		File dir = null;

		String output = "";
		String pathError = "{\"response\":\"'path' parameter missing in request body.\"}";
		String invalidPathError = "{\"response\":\"Invalid path or inaccessible location.\"}";
		ZipInputStream zipInputStream = null;

		String path = "";
		String fileNames = "";
		String fileNameArr[] = null;
		List<String> completeData = new ArrayList<String>();
		boolean filenamesExists = false;
		boolean gitRepo = true;

		JsonObject jsonObject = this.ippService.parseJSONObject(input);
		java.lang.System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");

		if (jsonObject.get("gitRepository") != null) {
			gitRepo = jsonObject.get("gitRepository").getAsBoolean();
			jsonObject.remove("gitRepository");

		}

		if (jsonObject.get("path") != null) {
			if (gitRepo) {
				path = jsonObject.get("path").getAsString();
			} else {
				path = jsonObject.get("path").getAsString();
				dir = new File(path);
				if (!dir.isDirectory()) {
					return Response.status(500).entity(invalidPathError).build();
				}
			}
			jsonObject.remove("path");

		} else {
			return Response.status(400).entity(pathError).build();
		}

		if (jsonObject.get("fileNames") != null) {
			fileNames = jsonObject.get("fileNames").getAsString();
			fileNameArr = fileNames.split(",");
			filenamesExists = true;
			jsonObject.remove("fileNames");
		}

		if (gitRepo) {
			try {
				zipInputStream = new ZipInputStream(new URL(path).openStream());
				ZipEntry zipEntry;

				while ((zipEntry = zipInputStream.getNextEntry()) != null) {
					if (!zipEntry.isDirectory() && zipEntry.getName().endsWith("xpdl")) {
						StringWriter stringWriter = new StringWriter();
						IOUtils.copy(zipInputStream, stringWriter);
						String fileContent = stringWriter.toString().trim();
						if (filenamesExists) {
							for (String fileName : fileNameArr) {
								if (zipEntry.getName().contains(fileName)) {
									completeData.add(fileContent);
								}
							}
						} else {
							completeData.add(fileContent);
						}

					}
				}
			} catch (Exception e) {
				LOG.info("deployModel Exception -- " + e.getStackTrace());
			} finally {
				try {
					zipInputStream.close();
				} catch (IOException e) {
					LOG.info("deployModel Exception -- " + e.getStackTrace());
				}
			}
		} else {

			if (!filenamesExists) {
				for (File file : dir.listFiles()) {
					fileNames = fileNames + file.getName() + ",";
				}
				fileNames = fileNames.substring(0, fileNames.length() - 1);

			}
			fileNameArr = fileNames.split(",");

			for (String fileName : fileNameArr) {
				completeData.add(path + fileName.trim());
			}
		}

		info = this.ippService.changeConfigVariablesandDeployModel(completeData, jsonObject, gitRepo);

		boolean isFailure = false;
		JsonObject outer = new JsonObject();
		JsonObject response = new JsonObject();
		Gson gson = new Gson();
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
						String modelId = deploymentInfo.getId();
						JsonObject error_warnings = new JsonObject();
						error_warnings.addProperty("Errors", (String) deploymentInfo.getErrors().toString());
						error_warnings.addProperty("Warnings", (String) deploymentInfo.getWarnings().toString());
						response.add(modelId, error_warnings);

					}
				}
			} else {
				response.addProperty("result", "Successfully Deployed Models");
				for (DeploymentInfo deploymentInfo : info) {
					if (deploymentInfo.hasWarnings()) {
						String modelId = deploymentInfo.getId();
						JsonObject warnings = new JsonObject();
						warnings.addProperty("Warnings", (String) deploymentInfo.getWarnings().toString());
						response.add(modelId, warnings);

					}
				}
			}

			outer.add("response", response);
			if (isFailure) {
				output = gson.toJson(outer).replaceAll("\\\\", "");
				return Response.status(500).entity(output).build();
			} else {
				output = gson.toJson(outer).replaceAll("\\\\", "");
				return Response.status(200).entity(output).build();
			}
		} else {
			String failedMessage = "{\"response\":\"Something went wrong. Please try again!.\"}";
			return Response.status(500).entity(failedMessage).build();
		}
	}

}
