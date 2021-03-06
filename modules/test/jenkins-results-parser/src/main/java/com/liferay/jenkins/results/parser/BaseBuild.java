/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.jenkins.results.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author Kevin Yen
 */
public abstract class BaseBuild implements Build {

	@Override
	public void addDownstreamBuilds(String... urls) {
		try {
			for (String url : urls) {
				url = JenkinsResultsParserUtil.getLocalURL(
					JenkinsResultsParserUtil.decode(url));

				if (!hasBuildURL(url)) {
					downstreamBuilds.add(BuildFactory.newBuild(url, this));
				}
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<String> getBadBuildURLs() {
		List<String> badBuildURLs = new ArrayList<>();

		String jobURL = getJobURL();

		for (Integer badBuildNumber : badBuildNumbers) {
			StringBuilder sb = new StringBuilder();

			sb.append(jobURL);
			sb.append("/");
			sb.append(badBuildNumber);
			sb.append("/");

			badBuildURLs.add(sb.toString());
		}

		return badBuildURLs;
	}

	@Override
	public int getBuildNumber() {
		return _buildNumber;
	}

	@Override
	public String getBuildURL() {
		try {
			String jobURL = getJobURL();

			if ((jobURL == null) || (_buildNumber == -1)) {
				return null;
			}

			jobURL = JenkinsResultsParserUtil.decode(jobURL);

			return JenkinsResultsParserUtil.encode(
				jobURL + "/" + _buildNumber + "/");
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getConsoleText() {
		try {
			return JenkinsResultsParserUtil.toString(
				JenkinsResultsParserUtil.getLocalURL(
					getBuildURL() + "/consoleText"),
				false);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int getDownstreamBuildCount(String status) {
		List<Build> downstreamBuilds = getDownstreamBuilds(status);

		return downstreamBuilds.size();
	}

	@Override
	public List<Build> getDownstreamBuilds(String status) {
		if (status == null) {
			return downstreamBuilds;
		}

		List<Build> filteredDownstreamBuilds = new ArrayList<>();

		for (Build downstreamBuild : downstreamBuilds) {
			if (status.equals(downstreamBuild.getStatus())) {
				filteredDownstreamBuilds.add(downstreamBuild);
			}
		}

		return filteredDownstreamBuilds;
	}

	@Override
	public String getInvocationURL() {
		String jobURL = getJobURL();

		if (jobURL == null) {
			return null;
		}

		StringBuffer sb = new StringBuffer(jobURL);

		sb.append("/buildWithParameters?");

		Map<String, String> parameters = getParameters();

		parameters.put("token", "raen3Aib");

		for (Map.Entry<String, String> parameter : parameters.entrySet()) {
			String value = parameter.getValue();

			if ((value != null) && !value.isEmpty()) {
				sb.append(parameter.getKey());
				sb.append("=");
				sb.append(parameter.getValue());
				sb.append("&");
			}
		}

		sb.deleteCharAt(sb.length() - 1);

		try {
			return JenkinsResultsParserUtil.encode(sb.toString());
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getJobName() {
		return jobName;
	}

	@Override
	public String getJobURL() {
		if ((master == null) || (jobName == null)) {
			return null;
		}

		try {
			return JenkinsResultsParserUtil.encode(
				"http://" + master + "/job/" + jobName);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getMaster() {
		return master;
	}

	@Override
	public Map<String, String> getParameters() {
		return new HashMap<>(_parameters);
	}

	@Override
	public String getParameterValue(String name) {
		return _parameters.get(name);
	}

	@Override
	public Build getParentBuild() {
		return _parentBuild;
	}

	@Override
	public String getResult() {
		String buildURL = getBuildURL();

		if ((result == null) && (buildURL != null)) {
			try {
				JSONObject resultJSONObject = getBuildJSONObject("result");

				result = resultJSONObject.optString("result");

				if (result.equals("")) {
					result = null;
				}
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		return result;
	}

	@Override
	public Map<String, String> getStartPropertiesMap() {
		return getTempMap("start.properties");
	}

	@Override
	public String getStatus() {
		return _status;
	}

	@Override
	public long getStatusAge() {
		return System.currentTimeMillis() - statusModifiedTime;
	}

	@Override
	public String getStatusReport() {
		return getStatusReport(0);
	}

	@Override
	public String getStatusReport(int indentSize) {
		StringBuffer indentStringBuffer = new StringBuffer();

		for (int i = 0; i < indentSize; i++) {
			indentStringBuffer.append(" ");
		}

		StringBuilder sb = new StringBuilder();

		sb.append(indentStringBuffer);
		sb.append("Build \"");
		sb.append(jobName);
		sb.append("\"");

		String status = getStatus();

		if (status.equals("completed")) {
			sb.append(" completed at ");
			sb.append(getBuildURL());
			sb.append(". ");
			sb.append(getResult());

			return sb.toString();
		}

		if (status.equals("missing")) {
			sb.append(" is missing ");
			sb.append(getJobURL());
			sb.append(".");

			return sb.toString();
		}

		if (status.equals("queued")) {
			sb.append(" is queued at ");
			sb.append(getJobURL());
			sb.append(".");

			return sb.toString();
		}

		if (status.equals("running")) {
			sb.append(" running at ");
			sb.append(getBuildURL());
			sb.append(".\n");

			if (getDownstreamBuildCount(null) > 0) {
				sb.append("\n");

				for (Build downstreamBuild : getDownstreamBuilds("running")) {
					sb.append(downstreamBuild.getStatusReport(indentSize + 4));
				}

				sb.append("\n");
				sb.append(indentStringBuffer);
				sb.append(getStatusSummary());
				sb.append("\n");
			}

			return sb.toString();
		}

		if (status.equals("starting")) {
			sb.append(" invoked at ");
			sb.append(getJobURL());
			sb.append(".");

			return sb.toString();
		}

		throw new RuntimeException("Unknown status: " + status + ".");
	}

	@Override
	public String getStatusSummary() {
		StringBuilder sb = new StringBuilder();

		sb.append(getDownstreamBuildCount("starting"));
		sb.append(" Starting  ");
		sb.append("/ ");

		sb.append(getDownstreamBuildCount("missing"));
		sb.append(" Missing  ");
		sb.append("/ ");

		sb.append(getDownstreamBuildCount("queued"));
		sb.append(" Queued  ");
		sb.append("/ ");

		sb.append(getDownstreamBuildCount("running"));
		sb.append(" Running  ");
		sb.append("/ ");

		sb.append(getDownstreamBuildCount("completed"));
		sb.append(" Completed  ");
		sb.append("/ ");

		sb.append(getDownstreamBuildCount(null));
		sb.append(" Total ");

		return sb.toString();
	}

	@Override
	public Map<String, String> getStopPropertiesMap() {
		return getTempMap("stop.properties");
	}

	@Override
	public boolean hasBuildURL(String buildURL) {
		try {
			buildURL = JenkinsResultsParserUtil.decode(buildURL);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

		buildURL = JenkinsResultsParserUtil.getLocalURL(buildURL);

		String thisBuildURL = getBuildURL();

		if ((thisBuildURL != null) && thisBuildURL.equals(buildURL)) {
			return true;
		}

		for (Build downstreamBuild : downstreamBuilds) {
			if (downstreamBuild.hasBuildURL(buildURL)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void reinvoke() {
		String hostName = JenkinsResultsParserUtil.getHostName("");

		if (!hostName.startsWith("cloud-10-0")) {
			System.out.println("A build may not be reinvoked by " + hostName);

			return;
		}

		String invocationURL = getInvocationURL();

		try {
			JenkinsResultsParserUtil.toString(
				JenkinsResultsParserUtil.getLocalURL(invocationURL));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

		System.out.println(getReinvokedMessage());

		reset();
	}

	@Override
	public void update() {
		String status = getStatus();

		if (!status.equals("completed")) {
			try {
				if (status.equals("missing") || status.equals("queued") ||
					status.equals("starting")) {

					JSONObject runningBuildJSONObject =
						getRunningBuildJSONObject();

					if (runningBuildJSONObject != null) {
						setBuildNumber(runningBuildJSONObject.getInt("number"));
					}
					else {
						JSONObject queueItemJSONObject =
							getQueueItemJSONObject();

						if (status.equals("starting") &&
							(queueItemJSONObject != null)) {

							setStatus("queued");
						}
						else if (status.equals("queued") &&
								 (queueItemJSONObject == null)) {

							setStatus("missing");
						}
					}
				}

				status = getStatus();

				if (downstreamBuilds != null) {
					ExecutorService executorService = getExecutorService();

					for (final Build downstreamBuild : downstreamBuilds) {
						if (executorService != null) {
							Runnable runnable = new Runnable() {

								@Override
								public void run() {
									downstreamBuild.update();
								}

							};

							executorService.execute(runnable);
						}
						else {
							downstreamBuild.update();
						}
					}

					if (executorService != null) {
						executorService.shutdown();

						while (!executorService.isTerminated()) {
							JenkinsResultsParserUtil.sleep(100);
						}
					}

					String result = getResult();

					if ((downstreamBuilds.size() ==
							getDownstreamBuildCount("completed")) &&
						(result != null)) {

						setStatus("completed");
					}
				}
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}

			findDownstreamBuilds();
		}
	}

	protected BaseBuild(String url) throws Exception {
		this(url, null);
	}

	protected BaseBuild(String url, Build parentBuild) throws Exception {
		_parentBuild = parentBuild;

		if (url.contains("buildWithParameters")) {
			setInvocationURL(url);
		}
		else {
			setBuildURL(url);
		}

		update();
	}

	protected void checkForReinvocation() {
		Build topLevelBuild = getTopLevelBuild();

		if (topLevelBuild == null) {
			return;
		}

		String consoleText = topLevelBuild.getConsoleText();

		if (consoleText.contains(getReinvokedMessage())) {
			reset();

			update();
		}
	}

	protected void findDownstreamBuilds() {
		List<String> foundDownstreamBuildURLs = new ArrayList<>(
			findDownstreamBuildsInConsoleText());

		JSONObject buildJSONObject;

		try {
			buildJSONObject = getBuildJSONObject("runs[number,url]");
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

		if ((buildJSONObject != null) && buildJSONObject.has("runs")) {
			JSONArray runsJSONArray = buildJSONObject.getJSONArray("runs");

			if (runsJSONArray != null) {
				for (int i = 0; i < runsJSONArray.length(); i++) {
					JSONObject runJSONObject = runsJSONArray.getJSONObject(i);

					if (runJSONObject.getInt("number") == _buildNumber) {
						String url = runJSONObject.getString("url");

						if (!hasBuildURL(url) &&
							!foundDownstreamBuildURLs.contains(url)) {

							foundDownstreamBuildURLs.add(url);
						}
					}
				}
			}
		}

		addDownstreamBuilds(
			foundDownstreamBuildURLs.toArray(
				new String[foundDownstreamBuildURLs.size()]));
	}

	protected List<String> findDownstreamBuildsInConsoleText() {
		List<String> foundDownstreamBuildURLs = new ArrayList<>();

		if (getBuildURL() != null) {
			String consoleText = getConsoleText();

			Matcher downstreamBuildURLMatcher =
				downstreamBuildURLPattern.matcher(
					consoleText.substring(_consoleReadCursor));

			_consoleReadCursor = consoleText.length();

			while (downstreamBuildURLMatcher.find()) {
				String url = downstreamBuildURLMatcher.group("url");

				if (!foundDownstreamBuildURLs.contains(url)) {
					foundDownstreamBuildURLs.add(url);
				}
			}
		}

		return foundDownstreamBuildURLs;
	}

	protected JSONObject getBuildJSONObject(String tree) throws Exception {
		if (getBuildURL() == null) {
			return null;
		}

		StringBuffer sb = new StringBuffer();

		sb.append(JenkinsResultsParserUtil.getLocalURL(getBuildURL()));
		sb.append("/api/json?pretty");

		if (tree != null) {
			sb.append("&tree=");
			sb.append(tree);
		}

		return JenkinsResultsParserUtil.toJSONObject(sb.toString(), false);
	}

	protected String getBuildMessage() {
		if (jobName != null) {
			String status = getStatus();

			StringBuilder sb = new StringBuilder();

			sb.append("Build \"");
			sb.append(jobName);
			sb.append("\"");

			if (status.equals("completed")) {
				sb.append(" completed at ");
				sb.append(getBuildURL());
				sb.append(". ");
				sb.append(getResult());

				return sb.toString();
			}

			if (status.equals("missing")) {
				sb.append(" is missing ");
				sb.append(getJobURL());
				sb.append(".");

				return sb.toString();
			}

			if (status.equals("queued")) {
				sb.append(" is queued at ");
				sb.append(getJobURL());
				sb.append(".");

				return sb.toString();
			}

			if (status.equals("running")) {
				if (badBuildNumbers.size() > 0) {
					sb.append(" restarted at ");
				}
				else {
					sb.append(" started at ");
				}

				sb.append(getBuildURL());
				sb.append(".");

				return sb.toString();
			}

			if (status.equals("starting")) {
				sb.append(" invoked at ");
				sb.append(getJobURL());
				sb.append(".");

				return sb.toString();
			}

			throw new RuntimeException("Unknown status: " + status);
		}

		return "";
	}

	protected JSONArray getBuildsJSONArray() throws Exception {
		JSONObject jsonObject = JenkinsResultsParserUtil.toJSONObject(
			getJobURL() + "/api/json?tree=builds[actions[parameters" +
				"[name,type,value]],building,duration,number,result,url]",
			false);

		return jsonObject.getJSONArray("builds");
	}

	protected ExecutorService getExecutorService() {
		return null;
	}

	protected Set<String> getJobParameterNames() throws Exception {
		JSONObject jsonObject = JenkinsResultsParserUtil.toJSONObject(
			getJobURL() + "/api/json?tree=actions[parameterDefinitions" +
				"[name,type,value]]");

		JSONArray actionsJSONArray = jsonObject.getJSONArray("actions");

		JSONObject firstActionJSONObject = actionsJSONArray.getJSONObject(0);

		JSONArray parameterDefinitionsJSONArray =
			firstActionJSONObject.getJSONArray("parameterDefinitions");

		Set<String> parameterNames = new HashSet<>(
			parameterDefinitionsJSONArray.length());

		for (int i = 0; i < parameterDefinitionsJSONArray.length(); i++) {
			JSONObject parameterDefinitionJSONObject =
				parameterDefinitionsJSONArray.getJSONObject(i);

			String type = parameterDefinitionJSONObject.getString("type");

			if (type.equals("StringParameterDefinition")) {
				parameterNames.add(
					parameterDefinitionJSONObject.getString("name"));
			}
		}

		return parameterNames;
	}

	protected String getJSONMapURL(TopLevelBuild topLevelBuild) {
		StringBuilder sb = new StringBuilder();

		sb.append(topLevelBuild.getMaster());
		sb.append("/");
		sb.append(topLevelBuild.getJobName());
		sb.append("/");
		sb.append(topLevelBuild.getBuildNumber());
		sb.append("/");
		sb.append(getJobName());
		sb.append("/");

		String jobVariant = getParameterValue("JOB_VARIANT");

		if ((jobVariant != null) && !jobVariant.isEmpty()) {
			sb.append(jobVariant);
			sb.append("/");
		}

		return sb.toString();
	}

	protected Map<String, String> getParameters(JSONArray jsonArray)
		throws Exception {

		Map<String, String> parameters = new HashMap<>(jsonArray.length());

		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);

			if (jsonObject.opt("value") instanceof String) {
				String name = jsonObject.getString("name");
				String value = jsonObject.getString("value");

				if (!value.isEmpty()) {
					parameters.put(name, value);
				}
			}
		}

		return parameters;
	}

	protected Map<String, String> getParameters(JSONObject buildJSONObject)
		throws Exception {

		JSONArray actionsJSONArray = buildJSONObject.getJSONArray("actions");

		if (actionsJSONArray.length() == 0) {
			return new HashMap<>();
		}

		JSONObject jsonObject = actionsJSONArray.getJSONObject(0);

		if (jsonObject.has("parameters")) {
			JSONArray parametersJSONArray = jsonObject.getJSONArray(
				"parameters");

			return getParameters(parametersJSONArray);
		}

		return new HashMap<>();
	}

	protected JSONObject getQueueItemJSONObject() throws Exception {
		JSONArray queueItemsJSONArray = getQueueItemsJSONArray();

		for (int i = 0; i < queueItemsJSONArray.length(); i++) {
			JSONObject queueItemJSONObject = queueItemsJSONArray.getJSONObject(
				i);

			JSONObject taskJSONObject = queueItemJSONObject.getJSONObject(
				"task");

			String queueItemName = taskJSONObject.getString("name");

			if (!queueItemName.equals(jobName)) {
				continue;
			}

			if (_parameters.equals(getParameters(queueItemJSONObject))) {
				return queueItemJSONObject;
			}
		}

		return null;
	}

	protected JSONArray getQueueItemsJSONArray() throws Exception {
		JSONObject jsonObject = JenkinsResultsParserUtil.toJSONObject(
			"http://" + master +
				"/queue/api/json?tree=items[actions[parameters" +
					"[name,value]],task[name,url]]",
			false);

		return jsonObject.getJSONArray("items");
	}

	protected String getReinvokedMessage() {
		StringBuffer sb = new StringBuffer();

		sb.append("Reinvoked: ");
		sb.append(getBuildURL());
		sb.append(" at ");
		sb.append(getInvocationURL());

		return sb.toString();
	}

	protected JSONObject getRunningBuildJSONObject() throws Exception {
		JSONArray buildsJSONArray = getBuildsJSONArray();

		for (int i = 0; i < buildsJSONArray.length(); i++) {
			JSONObject buildJSONObject = buildsJSONArray.getJSONObject(i);

			Map<String, String> parameters = getParameters();

			if (parameters.equals(getParameters(buildJSONObject)) &&
				!badBuildNumbers.contains(buildJSONObject.getInt("number"))) {

				return buildJSONObject;
			}
		}

		return null;
	}

	protected Map<String, String> getStartProperties(Build targetBuild) {
		BaseBuild parentBuild = (BaseBuild)_parentBuild;

		if (parentBuild != null) {
			return parentBuild.getStartProperties(targetBuild);
		}

		return Collections.emptyMap();
	}

	protected Map<String, String> getStopProperties(Build targetBuild) {
		BaseBuild parentBuild = (BaseBuild)_parentBuild;

		if (parentBuild != null) {
			return parentBuild.getStopProperties(targetBuild);
		}

		return Collections.emptyMap();
	}

	protected Map<String, String> getTempMap(String mapName) {
		Build buildCur = this;

		while (!(buildCur instanceof TopLevelBuild)) {
			buildCur = buildCur.getParentBuild();

			if (buildCur == null) {
				throw new RuntimeException("Incomplete Build tree");
			}
		}

		StringBuilder sb = new StringBuilder();

		sb.append(
			"http://cloud-10-0-0-31.lax.liferay.com/osb-jenkins-web/map/");
		sb.append(getJSONMapURL((TopLevelBuild)buildCur));
		sb.append(mapName);

		try {
			JSONObject tempMapJSONObject =
				JenkinsResultsParserUtil.toJSONObject(sb.toString(), false);

			if (!tempMapJSONObject.has("properties")) {
				return Collections.emptyMap();
			}

			JSONArray propertiesJSONArray = tempMapJSONObject.getJSONArray(
				"properties");

			Map<String, String> tempMap = new HashMap<>(
				propertiesJSONArray.length());

			for (int i = 0; i < propertiesJSONArray.length(); i++) {
				JSONObject propertyJSONObject =
					propertiesJSONArray.getJSONObject(i);

				String key = propertyJSONObject.getString("name");
				String value = propertyJSONObject.optString("value");

				if ((value != null) && !value.isEmpty()) {
					tempMap.put(key, value);
				}
			}

			return tempMap;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected Build getTopLevelBuild() {
		Build topLevelBuild = _parentBuild;

		while ((topLevelBuild != null) &&
		 !(topLevelBuild instanceof TopLevelBuild)) {

			topLevelBuild = topLevelBuild.getParentBuild();
		}

		return topLevelBuild;
	}

	protected boolean isParentBuildRoot() {
		if (_parentBuild == null) {
			return false;
		}

		if ((_parentBuild.getParentBuild() == null) &&
			(_parentBuild instanceof TopLevelBuild)) {

			return true;
		}

		return false;
	}

	protected void loadParametersFromBuildJSONObject() throws Exception {
		if (getBuildURL() == null) {
			return;
		}

		JSONObject buildJSONObject = getBuildJSONObject(
			"actions[parameters[*]]");

		JSONArray actionsJSONArray = buildJSONObject.getJSONArray("actions");

		if (actionsJSONArray.length() == 0) {
			_parameters = new HashMap<>(0);

			return;
		}

		JSONObject actionJSONObject = actionsJSONArray.getJSONObject(0);

		if (actionJSONObject.has("parameters")) {
			JSONArray parametersJSONArray = actionJSONObject.getJSONArray(
				"parameters");

			_parameters = new HashMap<>(parametersJSONArray.length());

			for (int i = 0; i < parametersJSONArray.length(); i++) {
				JSONObject parameterJSONObject =
					parametersJSONArray.getJSONObject(i);

				Object value = parameterJSONObject.opt("value");

				if (value instanceof String) {
					if (!value.toString().isEmpty()) {
						_parameters.put(
							parameterJSONObject.getString("name"),
							value.toString());
					}
				}
			}

			return;
		}

		_parameters = Collections.emptyMap();
	}

	protected void loadParametersFromQueryString(String queryString)
		throws Exception {

		Set<String> jobParameterNames = getJobParameterNames();

		for (String parameter : queryString.split("&")) {
			String[] nameValueArray = parameter.split("=");

			if ((nameValueArray.length == 2) &&
				jobParameterNames.contains(nameValueArray[0])) {

				_parameters.put(nameValueArray[0], nameValueArray[1]);
			}
		}
	}

	protected void reset() {
		result = null;

		badBuildNumbers.add(getBuildNumber());

		setBuildNumber(-1);

		downstreamBuilds.clear();

		_consoleReadCursor = 0;

		setStatus("starting");
	}

	protected void setBuildNumber(int buildNumber) {
		_buildNumber = buildNumber;

		setStatus("running");

		if (_buildNumber != -1) {
			checkForReinvocation();
		}
	}

	protected void setBuildURL(String buildURL) throws Exception {
		buildURL = JenkinsResultsParserUtil.decode(buildURL);

		Matcher matcher = buildURLPattern.matcher(buildURL);

		if (!matcher.find()) {
			throw new IllegalArgumentException("Invalid build URL " + buildURL);
		}

		_buildNumber = Integer.parseInt(matcher.group("buildNumber"));
		jobName = matcher.group("jobName");
		master = matcher.group("master");

		loadParametersFromBuildJSONObject();

		_consoleReadCursor = 0;

		setStatus("running");

		checkForReinvocation();
	}

	protected void setInvocationURL(String invocationURL) throws Exception {
		if (getBuildURL() == null) {
			invocationURL = JenkinsResultsParserUtil.decode(invocationURL);

			Matcher invocationURLMatcher = invocationURLPattern.matcher(
				invocationURL);

			if (!invocationURLMatcher.find()) {
				throw new IllegalArgumentException("Invalid invocation URL");
			}

			jobName = invocationURLMatcher.group("jobName");
			master = invocationURLMatcher.group("master");

			loadParametersFromQueryString(invocationURL);

			setStatus("starting");
		}
	}

	protected void setStatus(String status) {
		if (((status == null) && (_status != null)) ||
			!status.equals(_status)) {

			_status = status;

			statusModifiedTime = System.currentTimeMillis();

			if (isParentBuildRoot()) {
				System.out.println(getBuildMessage());
			}
		}
	}

	protected static final Pattern buildURLPattern = Pattern.compile(
		"\\w+://(?<master>[^/]+)/+job/+(?<jobName>[^/]+).*/(?<buildNumber>" +
			"\\d+)/?");
	protected static final Pattern downstreamBuildURLPattern = Pattern.compile(
		"[\\'\\\"].*[\\'\\\"] started at (?<url>.+)\\.");
	protected static final Pattern invocationURLPattern = Pattern.compile(
		"\\w+://(?<master>[^/]+)/+job/+(?<jobName>[^/]+).*/" +
			"buildWithParameters\\?(?<queryString>.*)");

	protected List<Integer> badBuildNumbers = new ArrayList<>();
	protected List<Build> downstreamBuilds = new ArrayList<>();
	protected String jobName;
	protected String master;
	protected String result;
	protected long statusModifiedTime;

	private int _buildNumber = -1;
	private int _consoleReadCursor;
	private Map<String, String> _parameters = new HashMap<>();
	private final Build _parentBuild;
	private String _status;

}