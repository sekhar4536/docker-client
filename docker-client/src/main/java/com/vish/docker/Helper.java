package com.vish.docker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

public class Helper {
	
	private static String DOCKER_PROPS_FILE = "docker-java.properties";
	private DockerClient docker;
	private DockerClientConfig config;
	private Logger logger;
	public Helper() {
		logger = org.apache.log4j.LogManager.getLogger(this.getClass());	
	}

	/**
	 * Create a Docker Client using the docker-java.properties file.
	 * @param useRegistryUrlFromFile set to true if registry URL is to be read from file. If set to false, registry
	 * url is set to index.docker.io/v1.
	 * @throws IOException
	 */
	
	protected void createClient(boolean useRegistryUrlFromFile) throws IOException {
		InputStream is = getClass().getClassLoader().getResourceAsStream(DOCKER_PROPS_FILE);
		Properties props = new Properties();
		props.load(is);
		
		String dockerConfigDir = props.getProperty("DOCKER_CONFIG");
		String dockerHostUrl = props.getProperty("DOCKER_HOST");
		Boolean tls = Boolean.valueOf(props.getProperty("DOCKER_TLS_VERIFY"));
		String apiVer = props.getProperty("api.version");
		
		String registryUrl = "https://index.docker.io/v1/";
		if (useRegistryUrlFromFile)
			registryUrl = props.getProperty("registry.url");
		
		logger.info(dockerConfigDir);
		
		logger.info("host:" + dockerHostUrl);
		
		config = DefaultDockerClientConfig.createDefaultConfigBuilder()
				.withDockerHost(dockerHostUrl)
				.withDockerTlsVerify(tls)
				.withDockerConfig(dockerConfigDir)
				.withApiVersion(apiVer)
				.withRegistryUrl(registryUrl)
				.build();
		docker = DockerClientBuilder.getInstance(config).build();

		Info info = docker.infoCmd().exec();
		System.out.print(info);

	}
	
	protected boolean isContainerRunning(String ancestorImg) {
		List<Container> cList = docker.listContainersCmd().exec();
		for (Container c : cList) {
			logger.debug("container id: " + c.getId() + ", image: " + c.getImage() + ", status: " + c.getStatus());
			if (ancestorImg.equalsIgnoreCase(c.getImage())) return true;
		}
		return false;
	}
	
	protected void createAndStartContainer(String imageName, String... runCommand) {
		CreateContainerResponse container = docker.createContainerCmd(imageName)
				   .withCmd(runCommand)
				   .exec();
		docker.startContainerCmd(container.getId()).exec();
	}
	
	protected void stopAndDeleteContainer(CreateContainerResponse container) {
		
		docker.stopContainerCmd(container.getId()).exec();
		docker.waitContainerCmd(container.getId()).exec(null);		
	}
	
}
